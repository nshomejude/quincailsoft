"""Transactional physical-inventory commands for the local integration service.

Caller owns SQLite transaction, authentication and idempotency. This module
rechecks scope/version so direct invocation cannot bypass those guards.
"""
from decimal import Decimal, ROUND_HALF_EVEN
import secrets

FINAL={'Completed','Received','Collected','Cancelled','Disposed'}

def _s():
 import server
 return server

def _positive(value):
 s=_s();q=s.number(value)
 if q<=0:raise s.APIError(422,'Quantity must be positive.')
 return q

def availability(c,p):
 s=_s();held=Decimal(0)
 for e,a,b in [('custody','owned_quantity','collected_quantity'),('quota','granted_quantity','consumed_quantity')]:
  for x in s.all_records(c,e):
   if x.get('product')==p['id'] and (x.get('status') in {'Active','Frozen'} if e=='quota' else x.get('status') not in FINAL|{'Revoked','Inactive','Expired'}):
    held+=max(Decimal(0),s.number(x.get(a))-s.number(x.get(b)))
 for r in s.all_records(c,'return'):
  if r.get('status')=='Completed' and not r.get('credit_posting_id'):
   held+=sum((s.number(l.get('quantity')) for l in r.get('lines',[]) if l.get('product')==p['id']),Decimal(0))
 return s.number(p.get('stock'))-held-s.number(p.get('quarantined_stock'))

def _positions(c,p):
 s=_s();positions=p.get('stock_positions')
 if positions is None:
  ws=[w for w in s.all_records(c,'warehouse') if w.get('branch_id')==p.get('branch_id')]
  if len(ws)!=1:raise s.APIError(409,'Establish explicit warehouse opening balances before posting multi-warehouse inventory.')
  positions={ws[0]['id']:s.ds(p.get('stock',0))}
 if sum((s.number(v) for v in positions.values()),Decimal(0))!=s.number(p.get('stock')):raise s.APIError(409,'Warehouse balances differ from aggregate stock; reconcile before posting.')
 return dict(positions)

def change_stock(c,user,product_id,delta,warehouse_id=None,source=None,allow_custody=False):
 s=_s();p=s.get(c,'product',product_id);s.check(user,'product','read',p);delta=s.number(delta)
 positions=_positions(c,p)
 warehouse_id=warehouse_id or (next(iter(positions)) if len(positions)==1 else None)
 if not warehouse_id:raise s.APIError(422,'Warehouse is required for this product.')
 w=s.get(c,'warehouse',warehouse_id)
 if not s.scoped(user,w,'warehouse'):raise s.APIError(403,'Warehouse outside permitted scope.')
 if w.get('company_id')!=p.get('company_id'):raise s.APIError(422,'Cross-company stock changes require intercompany trade.')
 if delta<0 and not allow_custody and availability(c,p)<-delta:raise s.APIError(422,'Insufficient merchant-owned available stock.')
 if s.number(positions.get(warehouse_id,0))+delta<0:raise s.APIError(422,'Insufficient stock in the selected warehouse.')
 positions[warehouse_id]=s.ds(s.number(positions.get(warehouse_id,0))+delta)
 p['stock_positions']=positions;p['stock']=s.ds(s.number(p.get('stock'))+delta);p['version']+=1;s.store(c,'product',p)
 mid=secrets.token_hex(10);movement={'id':mid,'reference':'MOVE-'+mid[:8],'version':1,'status':'Completed','tenant_id':p.get('tenant_id','demo'),'company_id':p.get('company_id'),'branch_id':w.get('branch_id'),'product':product_id,'warehouse':warehouse_id,'quantity':s.ds(abs(delta)),'signed_quantity':s.ds(delta),'direction':'Receipt' if delta>=0 else 'Issue','source_reference':source or '', 'business_date':__import__('datetime').date.today().isoformat(),'reason':'Domain transaction','lines':[]}
 if not any(x.get('reference')==source for x in s.all_records(c,'movement')):s.store(c,'movement',movement)
 return p

def _lines(r):
 s=_s();out={}
 for line in r.get('lines',[]):out[line['product']]=out.get(line['product'],Decimal(0))+_positive(line.get('quantity'))
 if not out:raise s.APIError(422,'At least one quantity line is required.')
 return out

def _state(r,states):
 if r['status'] not in states:raise _s().APIError(409,'Command is not valid from '+r['status']+'.')

def _related(c,user,e,id):
 s=_s();r=s.get(c,e,id)
 if not s.scoped(user,r,e):raise s.APIError(403,'Related record is outside your scope.')
 return r

def handle(c,user,body):
 s=_s();e=body.get('entity');verb=body.get('command');payload=body.get('payload') or {}
 supported={'receipt':{'complete'},'purchase':{'complete'},'movement':{'complete'},'count':{'start','capture','complete'},'transfer':{'reserve','dispatch','receive','complete'},'custody':{'collect','release','complete'},'fulfillment':{'dispatch','complete'},'return':{'complete'},'supplier_return':{'complete'},'repack':{'complete','assemble','disassemble'},'cut':{'confirm-cut','complete'},'recall':{'block','recover','dispose'}}
 if verb not in supported.get(e,set()):return None
 r=s.get(c,e,body['id']);s.check(user,e,'write',r)
 if r['version']!=body.get('version'):raise s.APIError(409,'Record changed. Refresh before retrying.')
 if not str(body.get('reason','')).strip():raise s.APIError(422,'A reason is required.')
 if (e in ('movement','repack','cut','recall','return','supplier_return') or e=='count' and verb=='complete') and not s.ROLES[user['role']]['approve']:raise s.APIError(403,'Manager approval authority is required for inventory adjustments.')
 if verb not in ('start','capture'):
  from finance_workflows import open_period
  open_period(c,r)
 if e=='receipt':
  _state(r,{'Draft','Approved'});po=_related(c,user,'purchase',r['purchase_order'])
  _state(po,{'Approved','Partially received'})
  if po['supplier']!=r['supplier'] or po['warehouse']!=r['warehouse']:raise s.APIError(422,'Receipt supplier and warehouse must match the purchase order.')
  ordered=_lines(po);incoming=_lines(r);prior={}
  for prev in s.all_records(c,'receipt'):
   if prev.get('purchase_order')==po['id'] and prev['status']=='Completed':
    for pid,q in _lines(prev).items():prior[pid]=prior.get(pid,Decimal(0))+q
  for pid,q in incoming.items():
   if q+prior.get(pid,0)>ordered.get(pid,0):raise s.APIError(422,'Receipt exceeds remaining purchase quantity.')
   before=_related(c,user,'product',pid);oldvalue=s.number(before.get('stock'))*s.number(before.get('purchase_cost'))
   incoming_value=sum((s.number(l['quantity'])*s.number(l.get('unit_price')) for l in r['lines'] if l['product']==pid),Decimal(0))
   updated=change_stock(c,user,pid,q,r['warehouse'],r['reference'])
   updated['purchase_cost']=s.ds(((oldvalue+incoming_value)/s.number(updated['stock'])).quantize(Decimal('0.000001'),rounding=ROUND_HALF_EVEN));s.store(c,'product',updated)
  po['status']='Completed' if all(prior.get(pid,0)+incoming.get(pid,0)==q for pid,q in ordered.items()) else 'Partially received';po['version']+=1;s.store(c,'purchase',po);r['status']='Completed'
 elif e=='purchase':
  raise s.APIError(409,'Purchase orders complete only through fully posted goods receipts.')
 elif e=='movement':
  _state(r,{'Approved'});q=_positive(r['quantity']);direction=r.get('direction')
  if direction not in ('Receipt','Issue'):raise s.APIError(422,'Adjustments require an approved count snapshot; choose Receipt or Issue for explicit quantity changes.')
  change_stock(c,user,r['product'],q if direction=='Receipt' else -q,r['warehouse'],r['reference']);r['status']='Completed'
 elif e=='count':
  if verb=='start':
   _state(r,{'Draft'});r['stock_snapshot']={};r['count_captured']=False
   for pid in _lines(r):
    p=_related(c,user,'product',pid);pos=_positions(c,p);r['stock_snapshot'][pid]={'version':p['version'],'quantity':pos.get(r['warehouse'],'0')}
   r['status']='Counting'
  elif verb=='capture':
   _state(r,{'Counting'});values=payload.get('quantities')
   if not isinstance(values,dict) or set(values)!=set(r.get('stock_snapshot',{})):raise s.APIError(422,'Capture a counted quantity for every snapshot product.')
   for line in r['lines']:
    value=s.number(values[line['product']])
    if value<0:raise s.APIError(422,'Count cannot be negative.')
    line['counted_quantity']=s.ds(value)
   r['count_captured']=True
  else:
   _state(r,{'Approved'})
   if not r.get('stock_snapshot') or not r.get('count_captured'):raise s.APIError(409,'Start and approve a count snapshot before posting.')
   seen=set()
   for l in r['lines']:
    pid=l['product'];p=_related(c,user,'product',pid);snap=r['stock_snapshot'].get(pid)
    if pid in seen or not snap or snap['version']!=p['version']:raise s.APIError(409,'Count snapshot is stale or contains duplicate products.')
    seen.add(pid)
    if 'counted_quantity' not in l:raise s.APIError(422,'Capture actual counted quantity for every line.')
    counted=s.number(l['counted_quantity'])
    if counted<0:raise s.APIError(422,'Counted quantity cannot be negative.')
    change_stock(c,user,pid,counted-s.number(snap['quantity']),r['warehouse'],r['reference'])
   r['status']='Completed'
 elif e=='transfer':
  origin=_related(c,user,'warehouse',r['origin_warehouse']);dest=_related(c,user,'warehouse',r['destination_warehouse'])
  if origin['id']==dest['id'] or origin.get('company_id')!=dest.get('company_id'):raise s.APIError(422,'Transfers require distinct warehouses in the same company.')
  if origin.get('branch_id')!=dest.get('branch_id'):raise s.APIError(501,'Cross-branch product identity and destination acceptance require the production inventory authority.')
  quantities=_lines(r)
  if verb=='reserve':raise s.APIError(501,'Exclusive transfer reservation is not implemented; use approved immediate dispatch.')
  elif verb=='dispatch':
   _state(r,{'Approved'});
   for pid,q in quantities.items():change_stock(c,user,pid,-q,origin['id'],r['reference'])
   r['received_quantities']={};r['status']='Dispatched'
  else:
   _state(r,{'Dispatched','Partially received'});received=r.setdefault('received_quantities',{});requested=payload.get('quantities')
   if not isinstance(requested,dict) or not requested:raise s.APIError(422,'Supply received product quantities explicitly.')
   for pid,value in requested.items():
    q=_positive(value);remaining=quantities.get(pid,0)-s.number(received.get(pid,0))
    if q>remaining:raise s.APIError(422,'Received quantity exceeds outstanding transit quantity.')
    change_stock(c,user,pid,q,dest['id'],r['reference']);received[pid]=s.ds(s.number(received.get(pid,0))+q)
   r['status']='Received' if all(s.number(received.get(pid,0))==q for pid,q in quantities.items()) else 'Partially received'
 elif e=='custody':
  _state(r,{'Pending','Partially collected'});sale=_related(c,user,'sale',r['sale'])
  if sale['status']!='Completed' or sale.get('customer')!=r['customer']:raise s.APIError(422,'Custody requires a completed sale for the same customer.')
  if not payload.get('proof_reference'):raise s.APIError(422,'Collection proof reference is required.')
  q=_positive(payload.get('quantity'));remaining=s.number(r['owned_quantity'])-s.number(r.get('collected_quantity'))
  if q>remaining:raise s.APIError(422,'Collection exceeds remaining customer custody.')
  change_stock(c,user,r['product'],-q,r['warehouse'],r['reference'],allow_custody=True)
  r['collected_quantity']=s.ds(s.number(r.get('collected_quantity'))+q);r['status']='Collected' if q==remaining else 'Partially collected';r.setdefault('collections',[]).append({'quantity':s.ds(q),'proof_reference':str(payload['proof_reference']),'actor':user['id']})
 elif e=='fulfillment':
  sale=_related(c,user,'sale',r['sale'])
  if sale['status']!='Completed' or sale.get('customer')!=r.get('customer'):raise s.APIError(422,'Fulfillment requires a completed sale for the same customer.')
  if any(x.get('sale')==sale['id'] and x['status'] not in FINAL for x in s.all_records(c,'custody')):raise s.APIError(409,'Release customer custody with collection proof before completing fulfillment.')
  if verb=='dispatch':_state(r,{'Pending','Approved'});r['status']='Dispatched'
  else:
   _state(r,{'Pending','Approved','Dispatched'})
   if not (payload.get('proof_reference') or r.get('proof_reference')):raise s.APIError(422,'Delivery or pickup proof is required.')
   r['proof_reference']=payload.get('proof_reference') or r['proof_reference'];r['status']='Completed'
 elif e in ('return','supplier_return'):
  _state(r,{'Approved'});source_e='sale' if e=='return' else 'receipt';key='original_sale' if e=='return' else 'goods_receipt';source=_related(c,user,source_e,r[key]);_state(source,{'Completed'})
  if e=='return' and r.get('customer')!=source.get('customer'):raise s.APIError(422,'Return customer must match source sale.')
  if e=='supplier_return' and r.get('supplier')!=source.get('supplier'):raise s.APIError(422,'Return supplier must match source receipt.')
  if e=='return' and any(x.get('sale')==source['id'] and x['status'] not in FINAL for x in s.all_records(c,'custody')):raise s.APIError(409,'Uncollected custody must be resolved before accepting a physical return.')
  eligible=_lines(source);prior={}
  for prev in s.all_records(c,e):
   if prev.get(key)==source['id'] and prev['status']=='Completed':
    for pid,q in _lines(prev).items():prior[pid]=prior.get(pid,0)+q
  for pid,q in _lines(r).items():
   if q+prior.get(pid,0)>eligible.get(pid,0):raise s.APIError(422,'Return exceeds eligible source quantity.')
   if e=='return':
    if r.get('condition')!='Sellable':raise s.APIError(501,'Damaged/quarantine returns require lot-level inspection workflow.')
    before=_related(c,user,'product',pid);updated=change_stock(c,user,pid,q,payload.get('warehouse'),r['reference'])
    r.setdefault('physical_return_evidence',{})[pid]={'quantity':s.ds(q),'unit_cost':s.ds(before.get('purchase_cost')),'stock_before':s.ds(before.get('stock')),'stock_after':s.ds(updated.get('stock')),'warehouse':payload.get('warehouse') or next(iter(updated['stock_positions'])),'product_version':updated['version']}
   else:
    if r.get('disposition')!='Return to supplier':raise s.APIError(501,'Supplier quarantine requires lot-level inspection workflow.')
    change_stock(c,user,pid,-q,source['warehouse'],r['reference'])
  r['status']='Completed';r['financial_settlement_status']='Pending separate credit/refund approval'
 elif e=='repack':
  _state(r,{'Approved'});component=r['component_product'];output=r['output_product']
  if component==output:raise s.APIError(422,'Component and output must be distinct products.')
  if s.number(r.get('loss_quantity'))!=0:raise s.APIError(501,'Loss-bearing transformations need an approved conversion and costing recipe.')
  q=_positive(r['component_quantity']);out=_positive(r['output_quantity']);warehouse=payload.get('warehouse')
  if verb=='disassemble':component,output,q,out=output,component,out,q
  src=_related(c,user,'product',component);dst=_related(c,user,'product',output)
  cost=s.number(src.get('purchase_cost'))*q;prior_cost=s.number(dst.get('purchase_cost'))*s.number(dst.get('stock'))
  change_stock(c,user,component,-q,warehouse,r['reference']);updated=change_stock(c,user,output,out,warehouse,r['reference'])
  updated['purchase_cost']=s.ds(((prior_cost+cost)/s.number(updated['stock'])).quantize(Decimal('0.000001'),rounding=ROUND_HALF_EVEN));s.store(c,'product',updated)
  r['status']='Completed';r['transferred_cost']=format(cost,'f');r['cost_method']='Weighted average; six-decimal output unit cost'
 elif e=='cut':
  _state(r,{'Approved'})
  if not payload.get('output_product') or not payload.get('warehouse') or not str(payload.get('evidence_reference','')).strip():raise s.APIError(422,'Output product, warehouse and measurement evidence are required.')
  if r.get('unit') not in ('metre','millimetre'):raise s.APIError(422,'Use a single supported length unit for the complete cut job.')
  parent_length=_positive(r.get('parent_length'));output_length=_positive(r.get('output_length'));count=_positive(r.get('output_count'))
  if count!=count.to_integral_value():raise s.APIError(422,'Output count must be a whole number of pieces.')
  if s.number(r.get('kerf'))!=0 or s.number(r.get('waste'))!=0:raise s.APIError(501,'Loss-bearing cuts require an approved waste and costing recipe.')
  if output_length*count!=parent_length:raise s.APIError(422,'Zero-loss cut outputs must exactly conserve parent length; remainder requires its own recipe.')
  src=_related(c,user,'product',r['parent_product']);dst=_related(c,user,'product',payload['output_product']);warehouse=_related(c,user,'warehouse',payload['warehouse'])
  if src['id']==dst['id']:raise s.APIError(422,'Cut output must use a distinct product identity.')
  if any(x.get('company_id')!=r.get('company_id') or x.get('branch_id')!=r.get('branch_id') for x in (src,dst,warehouse)):raise s.APIError(422,'Cut job, products and warehouse must belong to the same branch and company.')
  if any(x.get('status')!='Active' or x.get('base_unit')!='piece' or x.get('track_stock') is not True for x in (src,dst)):raise s.APIError(422,'Zero-loss cutting requires active, stock-tracked products measured in pieces.')
  if payload.get('parent_version')!=src['version'] or payload.get('output_version')!=dst['version']:raise s.APIError(409,'Product balances changed. Refresh before confirming the cut.')
  cost=_positive(src.get('purchase_cost'));old_stock=s.number(dst.get('stock'));old_cost=s.number(dst.get('purchase_cost'))
  if old_stock<0 or old_cost<0:raise s.APIError(422,'Reconcile output stock and valuation before cutting.')
  new_cost=((old_stock*old_cost+cost)/(old_stock+count)).quantize(Decimal('0.000001'),rounding=ROUND_HALF_EVEN)
  # Preserve valuation exactly in this bounded implementation; do not conceal rounding loss.
  if new_cost*(old_stock+count)!=old_stock*old_cost+cost:raise s.APIError(422,'Output valuation cannot conserve cost at six-decimal precision; use a supported costing recipe.')
  change_stock(c,user,src['id'],-1,warehouse['id'],r['reference']);updated=change_stock(c,user,dst['id'],count,warehouse['id'],r['reference'])
  updated['purchase_cost']=s.ds(new_cost);s.store(c,'product',updated)
  r.update(status='Completed',output_product=dst['id'],warehouse=warehouse['id'],transferred_cost=s.ds(cost),evidence_reference=str(payload['evidence_reference']).strip(),cost_method='Exact zero-loss piece transformation')
  r['product_lineage']={'input':{'product':src['id'],'quantity':'1','length':s.ds(parent_length),'unit_cost':s.ds(cost),'version':src['version']},'output':{'product':dst['id'],'quantity':s.ds(count),'length':s.ds(output_length),'version':dst['version']},'length_unit':r['unit'],'warehouse':warehouse['id'],'actor':user['id']}

 elif e=='recall':
  lot=_related(c,user,'lot',r['lot']);p=_related(c,user,'product',lot['product']);q=_positive(r['affected_quantity'])
  if q>s.number(lot['quantity']):raise s.APIError(422,'Recall exceeds identified lot quantity.')
  if verb=='block':
   _state(r,{'Draft','Approved'})
   if lot.get('condition')!='Available' or s.number(p.get('stock'))-s.number(p.get('quarantined_stock'))<q:raise s.APIError(409,'Lot is already blocked or quantity is unavailable.')
   p['quarantined_stock']=s.ds(s.number(p.get('quarantined_stock'))+q);lot['condition']='Quarantine';r['status']='Quarantine'
  else:
   _state(r,{'Quarantine'})
   if not payload.get('evidence_reference'):raise s.APIError(422,'Disposition evidence reference is required.')
   if verb=='dispose':
    change_stock(c,user,p['id'],-q,lot['warehouse'],r['reference'],allow_custody=True);p=s.get(c,'product',p['id']);lot['quantity']=s.ds(s.number(lot['quantity'])-q)
   p['quarantined_stock']=s.ds(max(Decimal(0),s.number(p.get('quarantined_stock'))-q));lot['condition']='Available' if verb=='recover' else 'Disposed';r['status']='Recovered' if verb=='recover' else 'Disposed';r['evidence_reference']=str(payload['evidence_reference'])
  p['version']+=1;lot['version']+=1;s.store(c,'product',p);s.store(c,'lot',lot)
 r['version']+=1;s.store(c,e,r);s.event(c,user,e,r['id'],verb,body['reason']);return {'record':s.safe_record(user,e,r)}
