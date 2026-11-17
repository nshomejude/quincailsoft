"""Transactional operational finance for the local integration harness.

Amounts use Decimal; account identifiers below are operational control names,
not a claim of SYSCOHADA mapping or fiscal certification.
"""
import json,secrets
from datetime import date
from decimal import Decimal

def api():
 import server
 return server

def setup(c):
 c.execute('CREATE TABLE IF NOT EXISTS finance_cash_entries(id TEXT PRIMARY KEY, session_id TEXT, source_entity TEXT, source_id TEXT, direction TEXT, amount TEXT, data TEXT)')
 c.execute('CREATE TABLE IF NOT EXISTS finance_postings(id TEXT PRIMARY KEY, source_entity TEXT, source_id TEXT, operation TEXT, data TEXT, UNIQUE(source_entity,source_id,operation))')

def fail(msg,code=422):raise api().APIError(code,msg)
def positive(v):
 n=api().number(v)
 if n<=0:fail('Amount must be greater than zero.')
 return n

def require_state(r,states):
 if r['status'] not in states:fail('This command is not allowed from '+r['status']+'.',409)

def approval(user):
 if not api().ROLES[user['role']]['approve']:fail('Approval authority is required.',403)

def scoped_get(c,user,e,id,source=None):
 r=api().get(c,e,id);api().check(user,e,'read',r)
 if source and (r.get('branch_id')!=source.get('branch_id') or r.get('company_id','company-1')!=source.get('company_id','company-1')):fail('Related records must belong to the same branch and company.',403)
 return r

def open_period(c,r):
 d=r.get('business_date') or r.get('invoice_date')
 try:date.fromisoformat(d)
 except (ValueError,TypeError):fail('A valid business date is required.')
 matches=[x for x in api().all_records(c,'period') if x.get('company',x.get('company_id'))==r.get('company_id','company-1') and x.get('start_date','')<=d<=x.get('end_date','')]
 if not matches:fail('Open a financial period covering this business date.',409)
 if any(x['status']!='Open' for x in matches):fail('The financial period is closed or frozen.',409)
 return matches[0]

def posting(c,user,e,r,operation,lines):
 """One immutable, balanced posting per source operation, mirrored in UI journals."""
 setup(c);period=open_period(c,r)
 if c.execute('SELECT 1 FROM finance_postings WHERE source_entity=? AND source_id=? AND operation=?',(e,r['id'],operation)).fetchone():fail('This source operation has already been posted.',409)
 debits=sum((api().number(l.get('debit',0)) for l in lines),Decimal(0));credits=sum((api().number(l.get('credit',0)) for l in lines),Decimal(0))
 if debits<=0 or debits!=credits or len(lines)<2:fail('A posting requires balanced positive debit and credit lines.')
 for l in lines:
  d=api().number(l.get('debit',0));cr=api().number(l.get('credit',0))
  if not l.get('account') or d<0 or cr<0 or (d>0 and cr>0):fail('Each journal line must have one nonnegative debit or credit.')
 pid='FIN-'+secrets.token_hex(10);normalized=[{'account':l['account'],'debit':api().ds(l.get('debit',0)),'credit':api().ds(l.get('credit',0))} for l in lines]
 doc={'id':pid,'source_entity':e,'source_id':r['id'],'operation':operation,'period':period['id'],'lines':normalized,'business_date':r.get('business_date',r.get('invoice_date')),'branch_id':r['branch_id'],'company_id':r.get('company_id','company-1')}
 c.execute('INSERT INTO finance_postings VALUES(?,?,?,?,?)',(pid,e,r['id'],operation,json.dumps(doc)))
 # Operational journal schema is line-oriented, so persist each accounting line separately.
 for idx,l in enumerate(normalized):
  j={**l,'id':pid+'-'+str(idx),'reference':pid+'-'+str(idx),'batch_reference':pid,'source_reference':r['reference'],'source_entity':e,'source_id':r['id'],'period':period['id'],'business_date':doc['business_date'],'branch_id':r['branch_id'],'company_id':r.get('company_id','company-1'),'tenant_id':r.get('tenant_id','demo'),'status':'Posted','version':1,'lines':[],'system_posting':True}
  api().store(c,'journal',j)
 return pid

def two_sides(debit,credit,amount):return [{'account':debit,'debit':amount,'credit':'0'},{'account':credit,'debit':'0','credit':amount}]

def cash_effect(c,user,e,r,payload,amount,direction):
 setup(c);sid=payload.get('cash_session') or r.get('cash_session')
 if not sid:fail('Select an open cash session before recording a cash movement.')
 till=scoped_get(c,user,'cash',sid,r);require_state(till,{'Open'})
 if till.get('business_date')!=r.get('business_date'):fail('Cash movement date must match its session.')
 if api().number(till.get('opening_amount',0))+cash_balance(c,till)<(amount if direction=='out' else 0):fail('Cash outflow exceeds available till cash.')
 id='CASHMOVE-'+secrets.token_hex(10);entry={'id':id,'session_id':sid,'entity':e,'record_id':r['id'],'direction':direction,'amount':api().ds(amount),'actor':user['id']}
 c.execute('INSERT INTO finance_cash_entries VALUES(?,?,?,?,?,?,?)',(id,sid,e,r['id'],direction,api().ds(amount),json.dumps(entry)));r['cash_session']=sid
 return id

def cash_balance(c,r):
 return sum((api().number(x['amount'])*(1 if x['direction']=='in' else -1) for x in c.execute('SELECT amount,direction FROM finance_cash_entries WHERE session_id=?',(r['id'],))),Decimal(0))

def settlement_account(c,user,e,r,payload,amount,direction):
 tender=r.get('tender')
 if tender=='Cash':cash_effect(c,user,e,r,payload,amount,direction);return 'CASH_CONTROL'
 if tender=='Bank':
  if not r.get('external_reference',r.get('payment_reference')):fail('A bank transaction reference is required.')
  approval(user);return 'BANK_CONTROL'
 if tender=='Mobile money':fail('Use a verified provider payment; manual mobile-money completion is blocked.',409)
 fail('Select a supported settlement tender.')

def outstanding(c,sale):
 paid=sum((api().number(x.get('allocation_amount') or x.get('amount')) for x in api().all_records(c,'payment') if x.get('sale')==sale['id'] and x['status']=='Completed' and x.get('direction','in')=='in'),Decimal(0))
 credits=sum((api().number(x.get('credit_amount',0)) for x in api().all_records(c,'return') if x.get('original_sale')==sale['id'] and x['status'] in ('Completed','Posted')),Decimal(0))
 return api().total(sale)-paid-credits

def invoice_outstanding(c,invoice):
 paid=sum((api().number(x.get('allocation_amount') or x.get('amount')) for x in api().all_records(c,'payment') if x.get('supplier_invoice')==invoice['id'] and x['status']=='Completed'),Decimal(0))
 return api().number(invoice['amount'])-paid

def finish(c,user,e,r,verb,reason,status=None):
 if status:r['status']=status
 r['version']+=1;api().store(c,e,r);api().event(c,user,e,r['id'],verb,reason)
 return {'record':api().safe_record(user,e,r)}

def validate_record(c,user,e,data,old):
 """Called before draft save; derived balances must not be user editable."""
 if e in ('deposit','advance'):
  for k in ('allocated_amount','refunded_amount'):
   if k in data and api().number(data[k])!=api().number((old or {}).get(k,0)):fail(k+' is derived from confirmed allocations, not an editable balance.')
 if old and (old.get('system_posting') or old.get('status') in ('Posted','Closed','Completed','Refunded','Converted')):fail('Finalized financial records are immutable.',409)

def handle(c,user,body):
 e=body.get('entity');verb=body.get('command');p=body.get('payload') or {};reason=body.get('reason','')
 supported={'deposit':{'complete','allocate','refund'},'payment':{'complete'},'advance':{'complete','allocate'},'supplier_invoice':{'complete','match'},'cash':{'open','complete','close'},'journal':{'complete','post','reverse'},'period':{'complete','close','reopen'},'quote':{'issue','convert'},'expense':{'complete'},'bank':{'complete','match'}}
 if verb not in supported.get(e,set()):return None
 setup(c);r=api().get(c,e,body['id']);api().check(user,e,'write',r)
 if body.get('version')!=r['version']:fail('Record changed. Refresh before retrying.',409)
 if not str(reason).strip():fail('A reason is required.')
 if e=='quote':
  if verb=='issue':
   require_state(r,{'Draft','Approved'})
   if not r.get('lines'):fail('A quotation needs at least one item.')
   if r.get('valid_until','')<r.get('business_date',''):fail('Quotation expiry precedes its issue date.')
   r['issued_total']=api().ds(api().total(r)+api().number(r.get('delivery_charge',0)))
   return finish(c,user,e,r,verb,reason,'Issued')
  require_state(r,{'Issued','Approved'})
  if r.get('converted_sale'):fail('This quotation was already converted.',409)
  target_date=p.get('business_date') or date.today().isoformat()
  if target_date>r.get('valid_until',''):fail('Quotation has expired. Issue a revised quotation.')
  if api().number(r.get('delivery_charge',0))!=0:fail('Delivery-charge invoicing needs a configured service line; conversion is blocked.')
  api().check(user,'sale','write');sid=secrets.token_hex(10);sale={**r,'id':sid,'reference':'SALE-'+sid[:8],'status':'Draft','version':1,'business_date':target_date,'channel':'Counter','fulfillment':'Immediate','quote':r['id'],'created_by':user['id']}
  for k in ('valid_until','issued_total','converted_sale'):sale.pop(k,None)
  api().store(c,'sale',sale);r['converted_sale']=sid;result=finish(c,user,e,r,verb,reason,'Converted');result['created_record']={'entity':'sale','id':sid};return result
 if e=='deposit':
  amount=positive(r['amount'])
  if verb=='complete':
   require_state(r,{'Draft','Approved'});r['allocated_amount']='0';r['refunded_amount']='0'
   acct=settlement_account(c,user,e,r,p,amount,'in');r['posting_id']=posting(c,user,e,r,'confirm',two_sides(acct,'CUSTOMER_ADVANCES',amount));return finish(c,user,e,r,verb,reason,'Completed')
  require_state(r,{'Completed'})
  amount=positive(p.get('amount'));available=api().number(r['amount'])-api().number(r.get('allocated_amount',0))-api().number(r.get('refunded_amount',0))
  if amount>available:fail('Amount exceeds the remaining customer deposit.')
  operation=verb+'-'+str(r['version'])
  if verb=='allocate':
   sale=scoped_get(c,user,'sale',p.get('sale'),r);require_state(sale,{'Completed','Posted'})
   if sale.get('customer')!=r['customer']:fail('The deposit and sale must belong to the same customer.')
   if amount>outstanding(c,sale):fail('Allocation exceeds the outstanding sale.')
   pid=secrets.token_hex(10);payment={**r,'id':pid,'reference':'PAY-'+pid[:8],'status':'Completed','version':1,'amount':api().ds(amount),'allocation_amount':api().ds(amount),'sale':sale['id'],'deposit':r['id'],'tender':'Deposit','direction':'in','lines':[]}
   payment['posting_id']=posting(c,user,e,r,operation,two_sides('CUSTOMER_ADVANCES','ACCOUNTS_RECEIVABLE',amount));api().store(c,'payment',payment);r['allocated_amount']=api().ds(api().number(r.get('allocated_amount',0))+amount)
  else:
   approval(user)
   if r.get('tender')!='Cash':fail('Bank/provider deposit refunds require their verified refund adapter.',409)
   cash_effect(c,user,e,r,p,amount,'out');posting(c,user,e,r,operation,two_sides('CUSTOMER_ADVANCES','CASH_CONTROL',amount));r['refunded_amount']=api().ds(api().number(r.get('refunded_amount',0))+amount)
  return finish(c,user,e,r,verb,reason)
 if e=='payment':
  require_state(r,{'Draft','Approved','Pending'});amount=positive(r['amount']);allocated=positive(r.get('allocation_amount') or amount)
  if allocated!=amount:fail('Split excess money into an explicit customer deposit or supplier advance.')
  if r.get('tender')=='Deposit':fail('Use the deposit allocation command; payment completion cannot spend a deposit.',409)
  if bool(r.get('customer'))==bool(r.get('supplier')):fail('Choose exactly one customer or supplier.')
  if r.get('customer'):
   sale=scoped_get(c,user,'sale',r.get('sale'),r);require_state(sale,{'Completed','Posted'})
   if sale.get('customer')!=r['customer']:fail('The payment customer does not match the sale.')
   if allocated>outstanding(c,sale):fail('Payment exceeds the outstanding sale.')
   acct=settlement_account(c,user,e,r,p,amount,'in');lines=two_sides(acct,'ACCOUNTS_RECEIVABLE',amount);r['direction']='in'
  else:
   invoice=scoped_get(c,user,'supplier_invoice',p.get('supplier_invoice') or r.get('supplier_invoice'),r);require_state(invoice,{'Posted'})
   if invoice.get('supplier')!=r['supplier']:fail('The payment supplier does not match the invoice.')
   if allocated>invoice_outstanding(c,invoice):fail('Payment exceeds the outstanding supplier invoice.')
   acct=settlement_account(c,user,e,r,p,amount,'out');lines=two_sides('ACCOUNTS_PAYABLE',acct,amount);r['direction']='out';r['supplier_invoice']=invoice['id']
  r['allocation_amount']=api().ds(allocated);r['posting_id']=posting(c,user,e,r,'confirm',lines);return finish(c,user,e,r,verb,reason,'Completed')
 if e=='advance':
  approval(user)
  if verb=='complete':
   require_state(r,{'Draft','Approved'});amount=positive(r['amount']);r['tender']=p.get('tender','Bank');r['allocated_amount']='0';acct=settlement_account(c,user,e,r,p,amount,'out');r['posting_id']=posting(c,user,e,r,'confirm',two_sides('SUPPLIER_ADVANCES',acct,amount));return finish(c,user,e,r,verb,reason,'Completed')
  require_state(r,{'Completed'});amount=positive(p.get('amount'));invoice=scoped_get(c,user,'supplier_invoice',p.get('supplier_invoice'),r);require_state(invoice,{'Posted'})
  if invoice['supplier']!=r['supplier']:fail('Supplier mismatch.')
  if amount>api().number(r['amount'])-api().number(r.get('allocated_amount',0)) or amount>invoice_outstanding(c,invoice):fail('Allocation exceeds available advance or payable.')
  pid=secrets.token_hex(10);payment={**r,'id':pid,'reference':'PAY-'+pid[:8],'status':'Completed','version':1,'amount':api().ds(amount),'allocation_amount':api().ds(amount),'supplier_invoice':invoice['id'],'advance':r['id'],'tender':'Advance','direction':'out','lines':[]};payment['posting_id']=posting(c,user,e,r,'allocate-'+str(r['version']),two_sides('ACCOUNTS_PAYABLE','SUPPLIER_ADVANCES',amount));api().store(c,'payment',payment);r['allocated_amount']=api().ds(api().number(r.get('allocated_amount',0))+amount);return finish(c,user,e,r,verb,reason)
 if e=='supplier_invoice':
  approval(user);require_state(r,{'Draft','Approved'});amount=positive(r['amount']);po=scoped_get(c,user,'purchase',r['purchase_order'],r);receipt=scoped_get(c,user,'receipt',r['goods_receipt'],r);require_state(receipt,{'Completed','Posted'})
  if po.get('supplier')!=r['supplier'] or receipt.get('purchase_order')!=po['id']:fail('Supplier, purchase order and receipt must match.')
  if any(x['id']!=r['id'] and x.get('supplier')==r['supplier'] and x.get('supplier_reference')==r['supplier_reference'] and x['status']=='Posted' for x in api().all_records(c,e)):fail('This supplier invoice reference is already posted.',409)
  expected=api().total(receipt)+api().number(r.get('tax_amount',0));difference=amount-expected
  if difference!=0:fail('Invoice differs from the receipt total. Resolve the matching exception before posting.')
  r['discrepancy_amount']='0';r['posting_id']=posting(c,user,e,r,'confirm',two_sides('GOODS_RECEIVED_NOT_INVOICED','ACCOUNTS_PAYABLE',amount));return finish(c,user,e,r,verb,reason,'Posted')
 if e=='expense':
  approval(user);require_state(r,{'Approved'});amount=positive(r['amount']);acct=settlement_account(c,user,e,r,p,amount,'out');r['posting_id']=posting(c,user,e,r,'confirm',two_sides('OPERATING_EXPENSE',acct,amount));return finish(c,user,e,r,verb,reason,'Posted')
 if e=='cash':
  if verb=='open':
   require_state(r,{'Draft'})
   if any(x['id']!=r['id'] and x.get('register')==r['register'] and x.get('branch_id')==r['branch_id'] and x['status']=='Open' for x in api().all_records(c,e)):fail('This register already has an open session.',409)
   if api().number(r['opening_amount'])<0:fail('Opening cash cannot be negative.')
   r['opened_by']=user['id'];r['expected_amount']=r['opening_amount'];return finish(c,user,e,r,verb,reason,'Open')
  require_state(r,{'Open'})
  for typ in ('payment','deposit','expense','advance'):
   if any(x.get('tender')=='Cash' and x.get('branch_id')==r['branch_id'] and x.get('business_date')==r['business_date'] and x['status'] in ('Completed','Posted') and not x.get('cash_session') for x in api().all_records(c,typ)):fail('Unassigned historical cash movements require reconciliation before cash close.',409)
  expected=api().number(r['opening_amount'])+cash_balance(c,r);counted=api().number(p.get('counted_amount',r.get('counted_amount')))
  if counted<0:fail('Counted cash cannot be negative.')
  diff=counted-expected
  if diff:
   approval(user)
   if not str(p.get('variance_reason',r.get('variance_reason',''))).strip():fail('Explain the cash variance before closing.')
   posting(c,user,e,r,'close-variance',two_sides('CASH_CONTROL' if diff>0 else 'CASH_VARIANCE','CASH_VARIANCE' if diff>0 else 'CASH_CONTROL',abs(diff)))
  r.update(expected_amount=api().ds(expected),counted_amount=api().ds(counted),variance=api().ds(diff),variance_reason=p.get('variance_reason',r.get('variance_reason','')),closed_by=user['id']);return finish(c,user,e,r,verb,reason,'Closed')
 if e=='journal':
  approval(user)
  if verb=='reverse':
   require_state(r,{'Posted'})
   if r.get('system_posting'):fail('Reverse the source business operation, not an individual system journal line.')
   if r.get('reversal_id'):fail('This journal was already reversed.',409)
   lines=r.get('posting_lines')
   if not lines:fail('Journal has no immutable posting lines to reverse.')
   reverse={**r,'business_date':p.get('business_date') or date.today().isoformat()};r['reversal_id']=posting(c,user,e,reverse,'reverse',[{'account':l['account'],'debit':l['credit'],'credit':l['debit']} for l in lines]);return finish(c,user,e,r,verb,reason)
  require_state(r,{'Draft','Approved'});lines=p.get('entries')
  if not isinstance(lines,list):fail('Supply balanced journal entries with account, debit and credit.')
  r['posting_id']=posting(c,user,e,r,'confirm',lines);r['posting_lines']=lines;return finish(c,user,e,r,verb,reason,'Posted')
 if e=='period':
  approval(user)
  if verb=='reopen':
   require_state(r,{'Closed'});r['reopened_by']=user['id'];return finish(c,user,e,r,verb,reason,'Open')
  require_state(r,{'Open'})
  for typ in ('journal','sale','payment','deposit','expense','supplier_invoice'):
   if any(x.get('company_id')==r.get('company',r.get('company_id')) and r['start_date']<=x.get('business_date','')<=r['end_date'] and x['status'] in ('Draft','In review','Pending','Approved') for x in api().all_records(c,typ)):fail('Resolve pending '+typ+' records before closing this period.',409)
  if any(x.get('company_id')==r.get('company',r.get('company_id')) and r['start_date']<=x.get('business_date','')<=r['end_date'] and x['status']=='Open' for x in api().all_records(c,'cash')):fail('Close all cash sessions before closing this period.',409)
  r['close_reason']=reason;return finish(c,user,e,r,verb,reason,'Closed')
 if e=='bank':
  approval(user);require_state(r,{'Draft','Approved'});payment=scoped_get(c,user,'payment',r.get('payment'),r);require_state(payment,{'Completed'})
  if payment.get('tender')!='Bank':fail('Only confirmed bank payments can be matched to this statement.')
  if any(x['id']!=r['id'] and x.get('payment')==payment['id'] and x['status']=='Completed' for x in api().all_records(c,'bank')):fail('Payment is already matched.',409)
  gross=positive(payment['amount']);fee=api().number(r.get('fee_amount',0));net=api().number(r['bank_amount'])
  expected=gross-fee if payment.get('direction','in')=='in' else gross+fee
  if fee<0 or net!=expected:fail('Statement amount must reconcile exactly to payment and fee.')
  if fee:posting(c,user,e,r,'fee',two_sides('BANK_FEES','BANK_CONTROL',fee))
  r['matched_amount']=api().ds(gross);r['variance']='0';return finish(c,user,e,r,verb,reason,'Completed')
 return None

def post_sale(c,user,r):
 """Post only proved immediate-handover, zero-tax sale semantics.

Nonzero tax and bill-and-hold policies require production rule engines; fail
rather than producing an apparently balanced but economically wrong journal.
"""
 if r.get('fulfillment')!='Immediate':fail('Sale recognition requires immediate handover; delivery/custody recognition policy is not implemented.',409)
 if any(api().number(l.get('tax_rate',0))!=0 for l in r.get('lines',[])):fail('Nonzero-tax recognition requires a configured fiscal calculation policy.',409)
 revenue=positive(api().total(r));cost=sum((api().number(l.get('quantity',0))*api().number(l.get('unit_cost',0)) for l in r.get('lines',[])),Decimal(0))
 if any(api().number(l.get('unit_cost',0))<=0 for l in r.get('lines',[])):fail('Every stock sale line requires a positive immutable cost snapshot.')
 lines=two_sides('ACCOUNTS_RECEIVABLE','SALES_REVENUE',revenue)
 if cost>0:lines+=two_sides('COST_OF_GOODS_SOLD','MERCHANT_INVENTORY',cost)
 r['posting_id']=posting(c,user,'sale',r,'recognize',lines);r['recognized_revenue']=api().ds(revenue);r['recognized_cost']=api().ds(cost)
 return r['posting_id']

def post_verified_provider_payment(c,user,r):
 """Called only after an adapter authenticated and verified live final status."""
 if not r.get('provider_verified') or r.get('status')!='Completed' or r.get('tender')!='Mobile money':fail('Verified live provider completion is required.',409)
 if not r.get('external_reference'):fail('Verified provider reference is required.')
 amount=positive(r['amount'])
 r['posting_id']=posting(c,user,'payment',r,'provider-confirm',two_sides('PROVIDER_CLEARING','ACCOUNTS_RECEIVABLE',amount))
 return r['posting_id']
