import unittest,sys,tempfile,threading,json,http.client,uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'server'))
import server
class Security(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.temp=tempfile.TemporaryDirectory();server.DBPATH=Path(cls.temp.name)/'test.sqlite3';server.DEMO=True;server.initialize();cls.http=server.ThreadingHTTPServer(('127.0.0.1',0),server.Handler);cls.port=cls.http.server_port;threading.Thread(target=cls.http.serve_forever,daemon=True).start()
 @classmethod
 def tearDownClass(cls):cls.http.shutdown();cls.http.server_close();cls.temp.cleanup()
 def request(self,path,data=None,auth=None,method=None,csrf=True):
  c=http.client.HTTPConnection('127.0.0.1',self.port);headers={'Content-Type':'application/json'}
  if auth:headers['Cookie']=auth[0];headers.update({'X-CSRF-Token':auth[1]} if csrf else {})
  c.request(method or ('POST' if data is not None else 'GET'),path,json.dumps(data) if data is not None else None,headers);r=c.getresponse();status=r.status;cookie=r.getheader('Set-Cookie');raw=r.read();c.close()
  try:body=json.loads(raw)
  except:body={}
  return status,body,cookie
 def login(self,role):
  status,b,cookie=self.request('/api/login',{'email':role+'@demo.quincai.local','password':'Demo-Quincai-2026!'});self.assertEqual(status,200);return cookie.split(';')[0],b['csrf']
 def draft(self,auth):
  status,b,_=self.request('/api/record',{'entity':'sale','data':{'customer':'customer-1','business_date':'2026-11-17','channel':'Counter','fulfillment':'Immediate','discount':'0'}},auth);self.assertEqual(status,200,b);return b['record']
 def cmd(self,auth,r,verb,payload=None):return self.request('/api/command',{'entity':'sale','id':r['id'],'version':r['version'],'command':verb,'reason':'Automated acceptance check','payload':payload or {},'operation_id':str(uuid.uuid4())},auth)
 def test_unauthenticated_and_method_guard(self):
  self.assertEqual(self.request('/api/state')[0],401);self.assertEqual(self.request('/api/logout',auth=self.login('owner'))[0],405)
 def test_all_nine_roles(self):
  for role in server.ROLES:
   a=self.login(role);status,b,_=self.request('/api/state',auth=a);self.assertEqual(status,200);self.assertTrue(all(not rs or server.allow({'role':role},e) for e,rs in b['entities'].items()))
 def test_auditor_cannot_write(self):self.assertEqual(self.request('/api/record',{'entity':'sale','data':{}},self.login('auditor'))[0],403)
 def test_csrf_and_cashier_price_override(self):
  a=self.login('cashier');self.assertEqual(self.request('/api/record',{'entity':'sale','data':{}},a,csrf=False)[0],403);r=self.draft(a);self.assertEqual(self.cmd(a,r,'add-line',{'product':'product-1','quantity':'1','unit_price':'1'})[0],403)
 def test_cost_redaction(self):
  a=self.login('cashier');_,b,_=self.request('/api/state',auth=a);self.assertTrue(all('purchase_cost' not in r for r in b['entities']['product']))
  _,b,_=self.request('/api/snapshot',{'doc':'DOC01','record':'sale-1'},a);self.assertTrue(all('unit_cost' not in x for x in b['snapshot']['record']['lines']))
 def test_customer_scope(self):
  _,b,_=self.request('/api/state',auth=self.login('customer'));self.assertTrue(all(r.get('customer')=='customer-1' for r in b['entities']['sale']));self.assertEqual(b['entities']['supplier'],[])
 def test_branch_scope(self):
  with server.connect() as c:
   r=server.get(c,'sale','sale-1');r.update(id='other-branch',branch_id='branch-2');server.store(c,'sale',r)
  a=self.login('cashier');_,b,_=self.request('/api/state',auth=a);self.assertNotIn('other-branch',[x['id'] for x in b['entities']['sale']]);self.assertEqual(self.cmd(a,r,'cancel')[0],403)
 def test_version_and_idempotency(self):
  a=self.login('owner');r=self.draft(a);data={'entity':'sale','id':r['id'],'version':r['version'],'command':'add-line','reason':'test','payload':{'product':'product-1','quantity':'1','unit_price':'4850'},'operation_id':str(uuid.uuid4())};first=self.request('/api/command',data,a);second=self.request('/api/command',data,a);self.assertEqual(first[:2],second[:2]);self.assertEqual(first[0],200);self.assertEqual(self.cmd(a,r,'submit')[0],409)
 def test_maker_checker(self):
  a=self.login('owner');r=self.draft(a);_,b,_=self.cmd(a,r,'submit');self.assertEqual(self.cmd(a,b['record'],'approve')[0],403);self.assertEqual(self.cmd(self.login('manager'),b['record'],'approve')[0],200)
 def test_access_change_revokes_sessions(self):
  owner=self.login('owner');staff=self.login('purchasing')
  status,b,_=self.request('/api/users/update',{'id':'purchasing','role':'purchasing','branch_id':'branch-1','active':True,'reason':'Verify session revocation'},owner);self.assertEqual(status,200,b);self.assertEqual(self.request('/api/state',auth=staff)[0],401)
  self.assertEqual(self.request('/api/users/update',{'id':'owner'},owner)[0],403)
 def test_password_change_revokes_sessions(self):
  a=self.login('inventory');self.assertEqual(self.request('/api/password',{'current':'Demo-Quincai-2026!','password':'Different-Demo-2026!'},a)[0],200);self.assertEqual(self.request('/api/state',auth=a)[0],401)
  self.request('/api/login',{'email':'inventory@demo.quincai.local','password':'Different-Demo-2026!'})
 def test_pos_save_atomic_and_idempotent(self):
  a=self.login('cashier');body={'data':{'customer':'customer-1','business_date':'2026-11-17','channel':'Counter','fulfillment':'Immediate','discount':'0'},'lines':[{'product':'product-1','quantity':'2','unit_price':'4850'}],'operation_id':str(uuid.uuid4())}
  first=self.request('/api/sales/draft',body,a);self.assertEqual(first[0],200,first);self.assertEqual(self.request('/api/sales/draft',body,a)[:2],first[:2]);self.assertEqual(len(first[1]['record']['lines']),1)
  with server.connect() as c:before=len(server.all_records(c,'sale'))
  body['operation_id']=str(uuid.uuid4());body['lines'].append({'product':'missing','quantity':'1','unit_price':'5'})
  self.assertEqual(self.request('/api/sales/draft',body,a)[0],404)
  with server.connect() as c:self.assertEqual(len(server.all_records(c,'sale')),before)
 def test_mobile_money_provider_configuration_visible(self):
  a=self.login('owner');status,b,_=self.request('/api/mobile-money/config',auth=a);self.assertEqual(status,200,b);self.assertEqual({p['id'] for p in b['providers']},{'mtn','orange'})
 def test_logout_revokes(self):
  a=self.login('owner');self.assertEqual(self.request('/api/logout',{},a)[0],200);self.assertEqual(self.request('/api/state',auth=a)[0],401)
 def test_oversell_rolls_back(self):
  a=self.login('owner');r=self.draft(a);_,b,_=self.cmd(a,r,'add-line',{'product':'product-1','quantity':'999999','unit_price':'4850'});self.assertEqual(self.cmd(a,b['record'],'complete')[0],422)
 def test_unimplemented_post_fails_closed(self):
  a=self.login('owner')
  with server.connect() as c:r=server.all_records(c,'integration')[0]
  status,_,_=self.request('/api/command',{'entity':'integration','id':r['id'],'version':r['version'],'command':'complete','reason':'test','operation_id':str(uuid.uuid4())},a);self.assertIn(status,(409,501))
if __name__=='__main__':unittest.main()
