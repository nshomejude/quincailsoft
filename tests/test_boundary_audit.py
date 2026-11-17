"""Adversarial API boundaries; these tests do not certify production readiness."""
import http.client
import json
import sys
import tempfile
import threading
import unittest
import uuid
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'server'))
import server

class BoundaryAudit(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.temp=tempfile.TemporaryDirectory();cls.previous_path=server.DBPATH
  server.DBPATH=Path(cls.temp.name)/'audit.sqlite3';server.DEMO=True;server.initialize()
  cls.http=server.ThreadingHTTPServer(('127.0.0.1',0),server.Handler);cls.port=cls.http.server_port
  cls.thread=threading.Thread(target=cls.http.serve_forever,daemon=True);cls.thread.start()
 @classmethod
 def tearDownClass(cls):
  cls.http.shutdown();cls.http.server_close();cls.thread.join();server.DBPATH=cls.previous_path;cls.temp.cleanup()
 def request(self,path,body=None,auth=None):
  conn=http.client.HTTPConnection('127.0.0.1',self.port);headers={'Content-Type':'application/json'}
  if auth:headers.update({'Cookie':auth[0],'X-CSRF-Token':auth[1]})
  conn.request('GET' if body is None else 'POST',path,None if body is None else json.dumps(body),headers)
  response=conn.getresponse();raw=response.read();cookie=response.getheader('Set-Cookie');conn.close()
  return response.status,json.loads(raw),cookie
 def login(self,role):
  status,data,cookie=self.request('/api/login',{'email':role+'@demo.quincai.local','password':'Demo-Quincai-2026!'})
  self.assertEqual(status,200,data);return cookie.split(';')[0],data['csrf']
 def test_owner_snapshot_labels_do_not_leak_to_cashier(self):
  owner=self.login('owner');status,data,_=self.request('/api/snapshot',{'doc':'DOC01','record':'sale-1'},owner);self.assertEqual(status,200,data)
  snapid=data['snapshot']['id'];status,state,_=self.request('/api/state',auth=self.login('cashier'));self.assertEqual(status,200,state)
  snap=next(s for s in state['snapshots'] if s['id']==snapid)
  self.assertFalse(any(k.startswith(('supplier:','staff:','role:','integration:')) for k in snap.get('labels',{})), 'Snapshot labels must obey reader permissions, not creator permissions')
 def test_approved_record_payload_cannot_be_edited(self):
  owner=self.login('owner')
  with server.connect() as c:
   r=server.get(c,'sale','sale-2');r['status']='Approved';r['id']='approved-edit-test';server.store(c,'sale',r)
  status,data,_=self.request('/api/record',{'entity':'sale','id':r['id'],'version':r['version'],'data':{'notes':'Changed after independent approval'}},owner)
  self.assertIn(status,(403,409),data)
 def test_approved_record_lines_cannot_be_edited(self):
  owner=self.login('owner')
  with server.connect() as c:
   r=server.get(c,'sale','sale-2');r['status']='Approved';r['id']='approved-line-test';server.store(c,'sale',r)
  status,data,_=self.request('/api/command',{'entity':'sale','id':r['id'],'version':r['version'],'command':'add-line','reason':'Attempt approval bypass','payload':{'product':'product-1','quantity':'1','unit_price':'4850'},'operation_id':str(uuid.uuid4())},owner)
  self.assertIn(status,(403,409),data)
 def test_role_cannot_be_published_by_generic_status_change(self):
  owner=self.login('owner')
  with server.connect() as c:
   r=server.all_records(c,'role')[0];r['status']='Approved';r['id']='generic-publish-test';server.store(c,'role',r)
  status,data,_=self.request('/api/command',{'entity':'role','id':r['id'],'version':r['version'],'command':'publish','reason':'No role publishing workflow exists','operation_id':str(uuid.uuid4())},owner)
  self.assertIn(status,(403,409,422,501),data)

 def test_static_get_and_head_share_resolved_path_guards(self):
  for method in ('GET','HEAD'):
   for path in ('/server/server.py','/server/export_seed.cjs','/%73erver/export_seed.cjs','/runtime/','/v3/','/v3/contracts.json'):
    conn=http.client.HTTPConnection('127.0.0.1',self.port)
    conn.request(method,path);response=conn.getresponse();response.read();conn.close()
    self.assertEqual(response.status,404,(method,path))
   conn=http.client.HTTPConnection('127.0.0.1',self.port)
   conn.request(method,'/v4/workflows.js');response=conn.getresponse();response.read();conn.close()
   self.assertEqual(response.status,200)
 def test_static_symlink_cannot_escape_app_root(self):
  target=Path(self.temp.name)/'private.js';target.write_text('private fixture')
  link=server.ROOT/'boundary-fixture.js'
  try:
   link.symlink_to(target)
   conn=http.client.HTTPConnection('127.0.0.1',self.port)
   conn.request('GET','/boundary-fixture.js');response=conn.getresponse();response.read();conn.close()
   self.assertEqual(response.status,404)
  finally:link.unlink(missing_ok=True)

 def test_financial_cost_snapshots_are_redacted_for_retail_roles(self):
  record={'id':'synthetic','return_valuation':{'p':{'cost':'100'}},'credit_cost':'100','recognized_cost':'100','credit_amount':'120','lines':[{'unit_cost':'100','quantity':'1'}]}
  for role in ('cashier','customer'):
   safe=server.safe_record({'role':role},'return',record)
   self.assertNotIn('return_valuation',safe);self.assertNotIn('credit_cost',safe);self.assertNotIn('recognized_cost',safe)
   self.assertNotIn('unit_cost',safe['lines'][0]);self.assertEqual(safe['credit_amount'],'120')
  self.assertIn('return_valuation',record)

 def test_cached_command_replay_rechecks_costs_and_scope(self):
  with server.connect() as c:
   r=server.get(c,'sale','sale-1');r['lines']=[{'unit_cost':'100','quantity':'1'}]
   raw=json.dumps({'record':r})
   cashier={'role':'cashier','branch_id':r['branch_id']}
   result=server.replay_result(c,cashier,'sale',raw)
   self.assertNotIn('unit_cost',result['record']['lines'][0])
   cashier['branch_id']='different-branch'
   with self.assertRaises(server.APIError):server.replay_result(c,cashier,'sale',raw)

 def test_directory_index_symlink_is_not_served(self):
  previous=server.ROOT;public=Path(self.temp.name)/'public';public.mkdir(exist_ok=True)
  secret=Path(self.temp.name)/'secret.html';secret.write_text('private fixture')
  (public/'index.html').symlink_to(secret)
  try:
   server.ROOT=public
   for method in ('GET','HEAD'):
    conn=http.client.HTTPConnection('127.0.0.1',self.port);conn.request(method,'/')
    response=conn.getresponse();response.read();conn.close();self.assertEqual(response.status,404)
  finally:server.ROOT=previous
