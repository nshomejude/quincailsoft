import unittest,tempfile,sys,copy
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'server'))
import server as s
from inventory_workflows import handle,change_stock,availability
class InventoryCommands(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.old=s.DBPATH;s.DBPATH=Path(self.tmp.name)/'test.db';s.DEMO=True;s.initialize()
  with s.connect() as c:
   self.user=dict(c.execute("select * from users where id='owner'").fetchone());period=s.all_records(c,'period')[0];period.update(start_date='2026-01-01',end_date='2026-12-31',status='Open',company='company-1');s.store(c,'period',period)
 def tearDown(self):s.DBPATH=self.old;self.tmp.cleanup()
 def cmd(self,e,id,verb,payload=None):
  with s.connect() as c:
   r=s.get(c,e,id);return handle(c,self.user,{'entity':e,'id':id,'version':r['version'],'command':verb,'payload':payload or {},'reason':'Acceptance test'})
 def record(self,e,id):
  with s.connect() as c:return s.get(c,e,id)
 def update(self,e,id,**data):
  with s.connect() as c:r=s.get(c,e,id);r.update(data);s.store(c,e,r)
 def test_receipt_stock_and_order_partial(self):
  self.cmd('receipt','receipt-1','complete');self.assertEqual(self.record('product','product-1')['stock'],'250');self.assertEqual(self.record('purchase','purchase-1')['status'],'Partially received')
  with self.assertRaises(s.APIError):self.cmd('receipt','receipt-1','complete')
 def test_overreceipt_rolls_back(self):
  r=self.record('receipt','receipt-1');r['lines'][0]['quantity']='31';self.update('receipt',r['id'],lines=r['lines'])
  with self.assertRaises(s.APIError):self.cmd('receipt',r['id'],'complete')
  self.assertEqual(self.record('product','product-1')['stock'],'240')
 def test_count_snapshot_stale_rejected(self):
  self.cmd('count','count-1','start');self.cmd('count','count-1','capture',{'quantities':{'product-1':'239'}});self.update('count','count-1',status='Approved')
  with s.connect() as c:change_stock(c,self.user,'product-1','1','warehouse-1','test')
  with self.assertRaises(s.APIError):self.cmd('count','count-1','complete')
  self.assertEqual(self.record('product','product-1')['stock'],'241')
 def test_count_variance(self):
  self.cmd('count','count-1','start');self.cmd('count','count-1','capture',{'quantities':{'product-1':'239'}});self.update('count','count-1',status='Approved');self.cmd('count','count-1','complete');self.assertEqual(self.record('product','product-1')['stock'],'239')
 def test_custody_partial_and_excess(self):
  self.cmd('custody','custody-1','collect',{'quantity':'3','proof_reference':'TEST-PROOF'});self.assertEqual(self.record('custody','custody-1')['status'],'Partially collected');self.assertEqual(self.record('product','product-1')['stock'],'237')
  with self.assertRaises(s.APIError):self.cmd('custody','custody-1','collect',{'quantity':'8','proof_reference':'TEST'})
 def test_transfer_in_transit_partial_receipt(self):
  # Establish explicit opening positions before adding a second warehouse.
  with s.connect() as c:
   p=s.get(c,'product','product-1');p['stock_positions']={'warehouse-1':'240','warehouse-2':'0'};s.store(c,'product',p)
   w=s.get(c,'warehouse','warehouse-1');w.update(id='warehouse-2',reference='WARE-002');s.store(c,'warehouse',w)
  self.update('transfer','transfer-1',status='Approved',destination_warehouse='warehouse-2',lines=[{'product':'product-1','quantity':'12'}])
  self.cmd('transfer','transfer-1','dispatch');self.assertEqual(self.record('product','product-1')['stock'],'228')
  self.cmd('transfer','transfer-1','receive',{'quantities':{'product-1':'5'}});self.assertEqual(self.record('transfer','transfer-1')['status'],'Partially received');self.assertEqual(self.record('product','product-1')['stock_positions']['warehouse-2'],'5')
  with self.assertRaises(s.APIError):self.cmd('transfer','transfer-1','receive',{'quantities':{'product-1':'8'}})
  self.cmd('transfer','transfer-1','receive',{'quantities':{'product-1':'7'}});self.assertEqual(self.record('product','product-1')['stock'],'240')
 def test_stock_protects_customer_owned_quantity(self):
  with self.assertRaises(s.APIError):
   with s.connect() as c:change_stock(c,self.user,'product-1','-235','warehouse-1','test')
  self.assertEqual(self.record('product','product-1')['stock'],'240')
 def test_returns_quantity_and_finality(self):
  self.update('custody','custody-1',status='Collected',collected_quantity='10')
  self.update('return','return-1',status='Approved',lines=[{'product':'product-1','quantity':'3'}]);self.cmd('return','return-1','complete',{'warehouse':'warehouse-1'});self.assertEqual(self.record('product','product-1')['stock'],'243')
  evidence=self.record('return','return-1')['physical_return_evidence']['product-1'];self.assertEqual(evidence['unit_cost'],'4000');self.assertEqual(evidence['quantity'],'3')
  with s.connect() as c:self.assertLessEqual(availability(c,s.get(c,'product','product-1')),240)
  with self.assertRaises(s.APIError):self.cmd('return','return-1','complete',{'warehouse':'warehouse-1'})
 def test_recall_removes_available_then_disposes(self):
  self.update('lot','lot-1',quantity='10');self.update('recall','recall-1',affected_quantity='5')
  self.cmd('recall','recall-1','block');self.assertEqual(self.record('product','product-1')['quarantined_stock'],'5')
  self.cmd('recall','recall-1','dispose',{'evidence_reference':'DISPOSAL-1'});p=self.record('product','product-1');self.assertEqual(p['stock'],'235');self.assertEqual(p['quarantined_stock'],'0')
 def test_repack_preserves_transferred_cost(self):
  self.update('repack','repack-1',status='Approved',component_product='product-1',component_quantity='2',output_product='product-2',output_quantity='4',loss_quantity='0')
  self.cmd('repack','repack-1','complete',{'warehouse':'warehouse-1'})
  self.assertEqual(self.record('product','product-1')['stock'],'238');self.assertEqual(self.record('product','product-2')['stock'],'90');self.assertEqual(self.record('repack','repack-1')['transferred_cost'],'8000')
 def test_scope_and_stale_version(self):
  with s.connect() as c:
   user=dict(c.execute("select * from users where id='cashier'").fetchone());r=s.get(c,'receipt','receipt-1');body={'entity':'receipt','id':r['id'],'version':r['version'],'command':'complete','reason':'test'}
   with self.assertRaises(s.APIError) as err:handle(c,user,body)
   self.assertEqual(err.exception.status,403);body['version']=999
   with self.assertRaises(s.APIError) as err:handle(c,self.user,body)
   self.assertEqual(err.exception.status,409)
 def test_cut_fails_without_output_lineage(self):
  with self.assertRaises(s.APIError) as x:self.cmd('cut','cut-1','confirm-cut')
  self.assertEqual(x.exception.status,409)
 def cut_setup(self):
  self.update('cut','cut-1',status='Approved',parent_product='product-2',parent_length='6',output_length='2',output_count='3',kerf='0',waste='0')
  self.update('product','product-3',stock='0',stock_positions={'warehouse-1':'0'},purchase_cost='0',base_unit='piece',track_stock=True)
  return {'output_product':'product-3','warehouse':'warehouse-1','parent_version':self.record('product','product-2')['version'],'output_version':self.record('product','product-3')['version'],'evidence_reference':'MEASUREMENT-1'}
 def test_cut_conserves_stock_length_cost_and_lineage(self):
  payload=self.cut_setup();self.cmd('cut','cut-1','confirm-cut',payload)
  self.assertEqual(self.record('product','product-2')['stock'],'85');out=self.record('product','product-3');self.assertEqual(out['stock'],'3');self.assertEqual(out['purchase_cost'],'1700.000000')
  job=self.record('cut','cut-1');self.assertEqual(job['product_lineage']['input']['quantity'],'1');self.assertEqual(job['product_lineage']['output']['product'],'product-3');self.assertEqual(job['transferred_cost'],'5100')
  with self.assertRaises(s.APIError):self.cmd('cut','cut-1','complete',payload)
  self.assertEqual(self.record('product','product-3')['stock'],'3')
 def test_cut_rejects_nonconservation_fractional_count_and_loss(self):
  payload=self.cut_setup()
  for fields in ({'output_length':'1'},{'output_length':'2','output_count':'2.5'},{'output_count':'3','kerf':'0.1'},{'kerf':'0','waste':'1'}):
   self.update('cut','cut-1',**fields)
   with self.assertRaises(s.APIError):self.cmd('cut','cut-1','confirm-cut',payload)
  self.assertEqual(self.record('product','product-2')['stock'],'86');self.assertEqual(self.record('product','product-3')['stock'],'0')
 def test_cut_rejects_stale_balance_and_wrong_identity(self):
  payload=self.cut_setup()
  for bad in ({'parent_version':999},{'output_product':'product-2'},{'evidence_reference':' '},{'output_version':999}):
   with self.assertRaises(s.APIError):self.cmd('cut','cut-1','complete',{**payload,**bad})
  self.update('product','product-3',branch_id='branch-2')
  with self.assertRaises(s.APIError):self.cmd('cut','cut-1','complete',payload)
  self.assertEqual(self.record('product','product-2')['stock'],'86')
 def test_cut_requires_approval_authority_and_open_period(self):
  payload=self.cut_setup()
  with s.connect() as c:
   user=dict(c.execute("select * from users where id='inventory'").fetchone());r=s.get(c,'cut','cut-1')
   with self.assertRaises(s.APIError) as err:handle(c,user,{'entity':'cut','id':'cut-1','version':r['version'],'command':'complete','payload':payload,'reason':'Test'})
   self.assertEqual(err.exception.status,403)
   for period in s.all_records(c,'period'):period['status']='Closed';s.store(c,'period',period)
  with self.assertRaises(s.APIError):self.cmd('cut','cut-1','complete',payload)
  self.assertEqual(self.record('product','product-2')['stock'],'86')
 def test_cut_rejects_valuation_rounding_and_unavailable_stock(self):
  payload=self.cut_setup();self.update('product','product-3',stock='4',stock_positions={'warehouse-1':'4'},purchase_cost='1')
  with self.assertRaises(s.APIError):self.cmd('cut','cut-1','complete',payload)
  self.update('product','product-3',stock='0',stock_positions={'warehouse-1':'0'});self.update('product','product-2',stock='0',stock_positions={'warehouse-1':'0'})
  with self.assertRaises(s.APIError):self.cmd('cut','cut-1','complete',payload)
  self.assertEqual(self.record('product','product-3')['stock'],'0')
if __name__=='__main__':unittest.main()
