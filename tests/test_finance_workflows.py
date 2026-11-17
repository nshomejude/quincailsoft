import unittest,tempfile,sys,json,uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'server'))
import server
import finance_workflows as f

class FinanceWorkflows(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.old_path=server.DBPATH;self.old_demo=server.DEMO;server.DBPATH=Path(self.temp.name)/'test.sqlite3';server.DEMO=True;server.initialize();self.c=server.connect();self.c.execute('BEGIN');self.owner=dict(self.c.execute("SELECT * FROM users WHERE id='owner'").fetchone());f.setup(self.c)
 def tearDown(self):
  self.c.rollback();self.c.close();server.DBPATH=self.old_path;server.DEMO=self.old_demo;self.temp.cleanup()
 def add(self,e,**fields):
  id=uuid.uuid4().hex;r={'id':id,'reference':e+'-'+id[:8],'version':1,'status':'Draft','business_date':'2026-11-17','branch_id':'branch-1','company_id':'company-1','tenant_id':'demo','lines':[],**fields};server.store(self.c,e,r);return r
 def run_cmd(self,e,r,command,payload=None,user=None):
  return f.handle(self.c,user or self.owner,{'entity':e,'id':r['id'],'version':r['version'],'command':command,'reason':'Acceptance test','payload':payload or {}})['record']
 def assert_error(self,fn,status):
  with self.assertRaises(server.APIError) as cm:fn()
  self.assertEqual(cm.exception.status,status)
 def test_deposit_receive_allocate_refund_and_double_spend(self):
  d=self.add('deposit',customer='customer-1',amount='10000',tender='Cash');d=self.run_cmd('deposit',d,'complete',{'cash_session':'cash-1'});d=self.run_cmd('deposit',d,'allocate',{'sale':'sale-1','amount':'7000'});self.assertEqual(d['allocated_amount'],'7000');d=self.run_cmd('deposit',d,'refund',{'amount':'2000','cash_session':'cash-1'});self.assertEqual(d['refunded_amount'],'2000');self.assert_error(lambda:self.run_cmd('deposit',d,'refund',{'amount':'2000','cash_session':'cash-1'}),422)
  movements=self.c.execute('SELECT direction,amount FROM finance_cash_entries WHERE source_id=?',(d['id'],)).fetchall();self.assertEqual([(x[0],x[1]) for x in movements],[('in','10000'),('out','2000')])
  self.assertEqual(self.c.execute('SELECT count(*) FROM finance_postings').fetchone()[0],3)
 def test_deposit_customer_mismatch_and_derived_guard(self):
  d=server.get(self.c,'deposit','deposit-1');self.assert_error(lambda:self.run_cmd('deposit',d,'allocate',{'sale':'sale-1','amount':'1'}),422);self.assert_error(lambda:f.validate_record(self.c,self.owner,'deposit',{'allocated_amount':'0'},dict(d,allocated_amount='10')),422)
 def test_payment_requires_real_funding(self):
  for tender in ('Mobile money','Deposit'):
   r=self.add('payment',customer='customer-1',sale='sale-1',amount='50',tender=tender);self.assert_error(lambda:self.run_cmd('payment',r,'complete'),409)
  r=self.add('payment',customer='customer-1',sale='sale-1',amount='50',tender='Cash');self.assert_error(lambda:self.run_cmd('payment',r,'complete'),422);r=self.run_cmd('payment',r,'complete',{'cash_session':'cash-1'});self.assertEqual(r['status'],'Completed')
 def test_payment_overallocation_and_branch_escape(self):
  r=self.add('payment',customer='customer-1',sale='sale-1',amount='30000',tender='Cash');self.assert_error(lambda:self.run_cmd('payment',r,'complete',{'cash_session':'cash-1'}),422)
  sale=server.get(self.c,'sale','sale-1');sale['branch_id']='branch-2';server.store(self.c,'sale',sale);r['amount']='1';server.store(self.c,'payment',r);self.assert_error(lambda:self.run_cmd('payment',r,'complete',{'cash_session':'cash-1'}),403)
 def test_supplier_invoice_and_supplier_payment(self):
  po=server.get(self.c,'purchase','purchase-1');gr=self.add('receipt',purchase_order=po['id'],status='Completed',supplier=po['supplier'],lines=[{'quantity':'10','unit_price':'100'}]);invoice=self.add('supplier_invoice',supplier=po['supplier'],supplier_reference='INV-TEST',purchase_order=po['id'],goods_receipt=gr['id'],invoice_date='2026-11-17',amount='1000',tax_amount='0');invoice=self.run_cmd('supplier_invoice',invoice,'match');self.assertEqual(invoice['status'],'Posted')
  pay=self.add('payment',supplier=po['supplier'],purchase_order=po['id'],amount='1000',tender='Cash');pay=self.run_cmd('payment',pay,'complete',{'supplier_invoice':invoice['id'],'cash_session':'cash-1'});self.assertEqual(pay['direction'],'out');self.assertEqual(f.invoice_outstanding(self.c,invoice),0)
 def test_cash_close_derives_balance_and_blocks_late_payment(self):
  p=self.add('payment',customer='customer-1',sale='sale-1',amount='50',tender='Cash');self.run_cmd('payment',p,'complete',{'cash_session':'cash-1'});cash=server.get(self.c,'cash','cash-1');self.assert_error(lambda:self.run_cmd('cash',cash,'close',{'counted_amount':'50050'}),409)
  for typ in ('payment','deposit'):
   for legacy in server.all_records(self.c,typ):
    if not legacy.get('cash_session'):legacy['cash_session']='legacy-reconciled-till';server.store(self.c,typ,legacy)
  cash=self.run_cmd('cash',cash,'close',{'counted_amount':'50050'});self.assertEqual(cash['expected_amount'],'50050');self.assertEqual(cash['variance'],'0');p=self.add('payment',customer='customer-1',sale='sale-1',amount='10',tender='Cash');self.assert_error(lambda:self.run_cmd('payment',p,'complete',{'cash_session':'cash-1'}),409)
 def test_journal_balances_reversal_and_closed_period(self):
  j=self.add('journal',batch_reference='TEST',account='control',source_reference='TEST');self.assert_error(lambda:self.run_cmd('journal',j,'post',{'entries':[{'account':'A','debit':'1','credit':'0'}]}),422);j=self.run_cmd('journal',j,'post',{'entries':[{'account':'A','debit':'10','credit':'0'},{'account':'B','debit':'0','credit':'10'}]});j=self.run_cmd('journal',j,'reverse',{'business_date':'2026-11-17'});self.assertIn('reversal_id',j);self.assert_error(lambda:self.run_cmd('journal',j,'reverse',{'business_date':'2026-11-17'}),409)
  period=server.get(self.c,'period','period-1');period['status']='Closed';server.store(self.c,'period',period);j2=self.add('journal');self.assert_error(lambda:self.run_cmd('journal',j2,'post',{'entries':[{'account':'A','debit':'1','credit':'0'},{'account':'B','debit':'0','credit':'1'}]}),409)
 def test_quote_conversion_immutable_source_and_expiry(self):
  q=server.get(self.c,'quote','quote-1');q=self.run_cmd('quote',q,'issue');before=json.dumps(q['lines']);q=self.run_cmd('quote',q,'convert',{'business_date':'2026-11-17'});sale=server.get(self.c,'sale',q['converted_sale']);self.assertEqual(json.dumps(sale['lines']),before);self.assertEqual(sale['status'],'Draft');self.assert_error(lambda:self.run_cmd('quote',q,'convert',{'business_date':'2026-11-17'}),409)
 def test_finance_role_and_approval_guard(self):
  cashier=dict(self.c.execute("SELECT * FROM users WHERE id='cashier'").fetchone());d=server.get(self.c,'deposit','deposit-1');self.assert_error(lambda:self.run_cmd('deposit',d,'refund',{'amount':'1','cash_session':'cash-1'},cashier),403)
 def test_bank_match_exact_fee_and_unique_payment(self):
  payment=self.add('payment',customer='customer-1',sale='sale-1',amount='100',tender='Bank',external_reference='BANK-TEST');payment=self.run_cmd('payment',payment,'complete');bank=self.add('bank',payment=payment['id'],statement_reference='STAT-TEST',bank_amount='98',fee_amount='2');bank=self.run_cmd('bank',bank,'match');self.assertEqual(bank['matched_amount'],'100');second=self.add('bank',payment=payment['id'],statement_reference='STAT-TEST2',bank_amount='100',fee_amount='0');self.assert_error(lambda:self.run_cmd('bank',second,'match'),409)
 def test_sale_posting_and_unsupported_recognition(self):
  sale=self.add('sale',customer='customer-1',fulfillment='Immediate',discount='0',lines=[{'product':'product-1','quantity':'2','unit_price':'100','unit_cost':'70','tax_rate':'0'}]);f.post_sale(self.c,self.owner,sale);self.assertEqual(sale['recognized_revenue'],'200');self.assertEqual(sale['recognized_cost'],'140');self.assert_error(lambda:f.post_sale(self.c,self.owner,sale),409)
  sale['fulfillment']='Customer custody';self.assert_error(lambda:f.post_sale(self.c,self.owner,sale),409)
 def test_provider_posting_requires_verified_live_payment(self):
  pay=self.add('payment',amount='100',tender='Mobile money',status='Pending',external_reference='PROVIDER-1');self.assert_error(lambda:f.post_verified_provider_payment(self.c,self.owner,pay),409);pay.update(status='Completed',provider_verified=True);f.post_verified_provider_payment(self.c,self.owner,pay);self.assertIn('posting_id',pay)
 def test_rollback_preserves_cash_on_posting_failure(self):
  self.c.commit();period=server.get(self.c,'period','period-1');period['status']='Closed';server.store(self.c,'period',period);self.c.commit();d=self.add('deposit',customer='customer-1',amount='100',tender='Cash');self.c.commit()
  with self.assertRaises(server.APIError):
   with self.c:self.run_cmd('deposit',d,'complete',{'cash_session':'cash-1'})
  self.assertEqual(self.c.execute('SELECT count(*) FROM finance_cash_entries').fetchone()[0],0);self.assertEqual(server.get(self.c,'deposit',d['id'])['status'],'Draft')
if __name__=='__main__':unittest.main()
