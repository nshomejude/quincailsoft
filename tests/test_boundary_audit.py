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
