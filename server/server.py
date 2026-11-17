#!/usr/bin/env python3
"""Local integration backend; not the production Java/Spring service."""
import os,json,sqlite3,secrets,hashlib,hmac,time,argparse,threading
from pathlib import Path
from http.server import ThreadingHTTPServer,SimpleHTTPRequestHandler
from http.cookies import SimpleCookie
from urllib.parse import urlparse
from decimal import Decimal,InvalidOperation
from policy import ROLES,allow,scoped,public_policy
import sys
# Keep one module instance when launched as a script; workflow plugins import server.
sys.modules.setdefault("server",sys.modules[__name__])
ROOT=Path(__file__).resolve().parents[1]
CONTRACT=json.loads((ROOT/'v3/contracts.json').read_text())
SCHEMAS=CONTRACT['schemas']
DBPATH=Path(os.environ.get('QUINCAI_DATA',ROOT/'runtime'))/'quincai.sqlite3'
LOCK=threading.RLock()
DEMO=False
class APIError(Exception):
 def __init__(self,status,message):self.status=status;self.message=message
def connect():
 c=sqlite3.connect(DBPATH,timeout=20);c.row_factory=sqlite3.Row;return c
def password_hash(password,salt):return hashlib.scrypt(password.encode(),salt=bytes.fromhex(salt),n=16384,r=8,p=1).hex()
def event(c,user,e,r,action,reason=''):
 c.execute('INSERT INTO audit(data) VALUES (?)',(json.dumps({'id':secrets.token_hex(10),'date':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),'entity':e,'record_id':r,'action':action,'reason':reason,'actor':user['email'],'branch_id':user['branch_id'],'tenant_id':'demo'}),))
def initialize():
 DBPATH.parent.mkdir(parents=True,exist_ok=True)
 with connect() as c:
  c.executescript('''
 CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY,email TEXT UNIQUE,role TEXT,branch_id TEXT,customer_id TEXT,salt TEXT,hash TEXT,active INTEGER DEFAULT 1,auth_version INTEGER DEFAULT 1);
 CREATE TABLE IF NOT EXISTS sessions(token_hash TEXT PRIMARY KEY,user_id TEXT,csrf TEXT,expires REAL,auth_version INTEGER);
 CREATE TABLE IF NOT EXISTS records(entity TEXT,id TEXT,data TEXT,PRIMARY KEY(entity,id));
 CREATE TABLE IF NOT EXISTS audit(seq INTEGER PRIMARY KEY AUTOINCREMENT,data TEXT);
 CREATE TABLE IF NOT EXISTS commands(user_id TEXT,operation_id TEXT,request_hash TEXT,result TEXT,PRIMARY KEY(user_id,operation_id));
 CREATE TABLE IF NOT EXISTS attempts(key TEXT PRIMARY KEY,count INTEGER,until REAL);
 CREATE TABLE IF NOT EXISTS snapshots(id TEXT PRIMARY KEY,entity TEXT,record_id TEXT,branch_id TEXT,data TEXT);
 CREATE TABLE IF NOT EXISTS backups(id TEXT PRIMARY KEY,created REAL,payload TEXT,digest TEXT);
 ''')
  if DEMO and not c.execute('SELECT 1 FROM users LIMIT 1').fetchone():
   for role in ROLES:
    salt=secrets.token_hex(16);c.execute('INSERT INTO users(id,email,role,branch_id,customer_id,salt,hash) VALUES(?,?,?,?,?,?,?)',(role,role+'@demo.quincai.local',role,'branch-1','customer-1' if role=='customer' else '',salt,password_hash('Demo-Quincai-2026!',salt)))
   seed=json.loads((ROOT/'server/seed.json').read_text())
   for e,rs in seed['entities'].items():
    for r in rs:r['tenant_id']='demo';c.execute('INSERT INTO records VALUES(?,?,?)',(e,r['id'],json.dumps(r)))
def number(v):
 try:
  d=Decimal(str(v or 0))
  if not d.is_finite() or d.as_tuple().exponent < -6 or abs(d)>Decimal('1000000000000'):raise ValueError()
  return d
 except (ValueError,InvalidOperation):raise APIError(422,'Invalid decimal amount or quantity.')
def ds(v):return format(number(v),'f')
def get(c,e,id):
 row=c.execute('SELECT data FROM records WHERE entity=? AND id=?',(e,id)).fetchone()
 if not row:raise APIError(404,'Record not found.')
 return json.loads(row[0])
def all_records(c,e):return [json.loads(x[0]) for x in c.execute('SELECT data FROM records WHERE entity=?',(e,))]
def store(c,e,r):c.execute('INSERT INTO records VALUES(?,?,?) ON CONFLICT(entity,id) DO UPDATE SET data=excluded.data',(e,r['id'],json.dumps(r)))
def check(user,e,op='read',r=None):
 if not allow(user,e,op) or (r is not None and not scoped(user,r,e)):raise APIError(403,'Your role or branch scope does not permit this action.')
def total(r):return sum((number(l['quantity'])*number(l['unit_price']) for l in r.get('lines',[])),Decimal(0))-number(r.get('discount',0)) if r.get('lines') else number(r.get('amount',r.get('total',0)))
def safe_record(user,e,r):
 r=dict(r)
 if user['role'] in ('cashier','customer'):
  for k in ['purchase_cost','unit_cost','original_cost','revised_cost']:r.pop(k,None)
  r['lines']=[{k:v for k,v in l.items() if k!='unit_cost'} for l in r.get('lines',[])]
 return r
def validate(c,user,e,data,old):
 if not isinstance(data,dict):raise APIError(422,'Record data must be an object.')
 from finance_workflows import validate_record
 validate_record(c,user,e,data,old)
 for key in ({'quota':['consumed_quantity'],'cash':['expected_amount']}.get(e,[])):
  if key in data and number(data[key])!=number((old or {}).get(key,0)):raise APIError(422,key+' is derived and cannot be edited.')
 if e not in SCHEMAS:raise APIError(404,'Unknown entity.')
 out={}
 for f in SCHEMAS[e]['fields']:
  key=f['key'];v=data.get(key,old.get(key,'') if old else '')
  if f['type']=='password':continue
  if f['type']=='checkbox' and not isinstance(v,bool):
   if v=='':v=False
   else:raise APIError(422,f['label']+' must be true or false.')
  if f['type']!='checkbox' and not isinstance(v,(str,int,float)):raise APIError(422,'Invalid field value: '+f['label'])
  if f['required'] and (v is None or v==''):raise APIError(422,f['label']+' is required.')
  if f['type'] in ('money','decimal','number') and v!='':
   if number(v)<0:raise APIError(422,f['label']+' cannot be negative.')
   v=ds(v)
  if f['type']=='relation' and v:
   other=get(c,f['entity'],v)
   if not scoped(user,other,f['entity']):raise APIError(403,'Related record is outside your scope.')
  if f['type']=='select' and v and v not in f['options']:raise APIError(422,'Invalid '+f['label'])
  if isinstance(v,str) and len(v)>10000:raise APIError(422,'Field too long.')
  if user['role']=='cashier' and e=='sale' and key=='discount' and number(v)>0:raise APIError(403,'Discount override requires a manager.')
  out[key]=v
 if e=='product' and any(r.get('sku')==out.get('sku') and r['id']!=(old or {}).get('id') for r in all_records(c,e)):raise APIError(409,'SKU already exists.')
 if e=='transfer' and out.get('origin_warehouse')==out.get('destination_warehouse'):raise APIError(422,'Transfer warehouses must differ.')
 return out
def command(c,user,body):
 e=body.get('entity');id=body.get('id');verb=body.get('command');op='approve' if verb=='approve' else 'write'
 check(user,e,op);r=get(c,e,id);check(user,e,op,r)
 if body.get('version')!=r.get('version'):raise APIError(409,'Record changed. Refresh before retrying.')
 reason=str(body.get('reason','')).strip()
 if not reason:raise APIError(422,'A reason is required.')
 payload=body.get('payload') or {}
 if not isinstance(payload,dict):raise APIError(422,'Command payload must be an object.')
 if user['role']=='customer' and verb not in ('submit','cancel'):raise APIError(403,'Customers can only submit or cancel their own draft request.')
 if verb=='approve' and r.get('submitted_by')==user['id']:raise APIError(403,'A different person must approve this request.')
 from inventory_workflows import handle as inventory_command
 from finance_workflows import handle as finance_command
 for handler in (inventory_command,finance_command):
  handled=handler(c,user,body)
  if handled is not None:return handled
 original=r['status']
 if verb in ('grant','refund','confirm-cut','reopen','dispose') and not ROLES[user['role']]['approve']:raise APIError(403,'Approval authority is required for this command.')
 final={'Completed','Posted','Closed','Cancelled','Refunded'}
 transitions={'submit':(['Draft','Rejected'],'In review'),'approve':(['In review','Pending'],'Approved'),'reject':(['In review','Pending'],'Rejected'),'hold':(['Draft'],'Held'),'resume':(['Held'],'Draft'),'cancel':(['Draft','In review','Pending'],'Cancelled'),'archive':(['Draft','Active','Inactive'],'Archived'),'activate':(['Draft','Inactive'],'Active'),'issue':(['Draft','Approved'],'Issued'),'acknowledge':(['Draft','Active','Pending'],'Acknowledged'),'snooze':(['Draft','Active','Pending','Acknowledged'],'Snoozed'),'silence':(['Draft','Active'],'Silenced'),'return':(['In review'],'Draft'),'freeze':(['Draft','Active','In review','Pending'],'Frozen'),'reopen':(['Frozen','Closed'],'Draft'),'retire':(['Draft','Active','Frozen'],'Retired'),'revoke':(['Draft','Active','Invited','Approved'],'Revoked'),'disable':(['Draft','Active','Published'],'Inactive'),'reserve':(['Draft','Approved'],'Reserved'),'dispatch':(['Approved','Reserved'],'Dispatched'),'release':(['Reserved','Held','Frozen'],'Draft'),'publish':(['Approved','Issued'],'Published'),'block':(['Draft','Active'],'Quarantine'),'recover':(['Quarantine','Recalled'],'Recovered'),'dispose':(['Quarantine','Recovered'],'Disposed'),'complete':(['Draft','Approved','Open','Pending'],'Completed')}
 if verb=='add-line':
  if r['status'] not in ('Draft','Rejected'):raise APIError(409,'Only draft or rejected lines can be edited.')
  p=get(c,'product',payload.get('product'));check(user,'product','read',p);q=number(payload.get('quantity'));price=number(payload.get('unit_price') or p['selling_price'])
  if q<=0 or price<0:raise APIError(422,'Invalid line quantity or price.')
  if user['role']=='cashier' and price!=number(p['selling_price']):raise APIError(403,'Price override requires a manager.')
  r.setdefault('lines',[]).append({'id':secrets.token_hex(8),'product':p['id'],'description':p['name'],'quantity':ds(q),'unit_price':ds(price),'unit_cost':p.get('purchase_cost','0'),'tax_rate':p.get('tax_rate','0')})
 elif verb=='remove-line':
  if r['status'] not in ('Draft','Rejected'):raise APIError(409,'Only draft or rejected lines can be edited.')
  r['lines']=[l for l in r.get('lines',[]) if l['id']!=payload.get('line')]
 elif verb=='refund' and e=='deposit':
  available=number(r['amount'])-number(r.get('allocated_amount',0))-number(r.get('refunded_amount',0));amount=number(payload.get('amount'))
  if amount<=0 or amount>available:raise APIError(422,'Refund exceeds remaining deposit.')
  r['refunded_amount']=ds(number(r.get('refunded_amount',0))+amount)
  if amount==available:r['status']='Refunded'
 elif verb=='grant' and e=='quota':
  if r['status'] not in ('Draft','Approved'):raise APIError(409,'Only a draft or approved quota can be granted.')
  if number(r.get('consumed_quantity',0))!=0:raise APIError(422,'Consumed quota must start at zero.')
  qty=number(r.get('granted_quantity'));p=get(c,'product',r['product']);check(user,'product','read',p)
  others=sum((number(x.get('granted_quantity',0))-number(x.get('consumed_quantity',0)) for x in all_records(c,'quota') if x['id']!=r['id'] and x.get('product')==p['id'] and x['status']=='Active'),Decimal(0))
  from inventory_workflows import availability
  if qty<=0 or qty>availability(c,p):raise APIError(422,'Allocation exceeds unallocated stock.')
  r['status']='Active'
 elif verb=='confirm-cut' and e=='cut':
  length=number(r['parent_length']);used=(number(r['output_length'])+number(r['kerf']))*number(r['output_count'])
  if used>length:raise APIError(422,'Outputs and kerf exceed parent length.')
  if r['status'] not in ('Draft','Approved'):raise APIError(409,'Cut job already processed.')
  r['waste']=ds(length-used);r['status']='Completed'
 elif verb in transitions:
  safe_review={'submit','approve','reject','return'}
  safe_drafts={'hold','resume','cancel'}
  config_entities={'category','unit','price','template','printer','industry','content'}
  safe_config=verb in ('archive','activate','disable') and e in config_entities
  safe_alert=verb in ('acknowledge','snooze','silence') and e in ('policy','incident')
  if verb not in safe_review|safe_drafts|{'complete'} and not safe_config and not safe_alert:raise APIError(501,'A dedicated domain handler is required for this operation; nothing changed.')
  allowed,new=transitions[verb]
  if verb=='submit' and e=='count' and r.get('count_captured'):allowed=[*allowed,'Counting']
  if original not in allowed:raise APIError(409,'Cannot '+verb+' from '+original+'.')
  if verb=='complete':
   if e not in ('sale','payment','quote','journal'):raise APIError(501,'This domain posting adapter is not implemented. The record has not been posted.')
   if e in ('sale','purchase','receipt','quote') and not r.get('lines'):raise APIError(422,'At least one line is required.')
   if e=='sale':
    from finance_workflows import open_period,post_sale
    open_period(c,r)
    post_sale(c,user,r)
    for l in r['lines']:
     p=get(c,'product',l['product']);q=number(l['quantity'])
     quota=sum((number(x.get('granted_quantity',0))-number(x.get('consumed_quantity',0)) for x in all_records(c,'quota') if x.get('product')==p['id'] and x['status']=='Active'),Decimal(0))
     custody=sum((number(x.get('owned_quantity',0))-number(x.get('collected_quantity',0)) for x in all_records(c,'custody') if x.get('product')==p['id'] and x['status'] not in ('Cancelled','Collected','Completed')),Decimal(0))
     if number(p.get('stock',0))-quota-custody<q:raise APIError(422,'Insufficient unallocated stock.')
     if r.get('fulfillment')=='Customer custody':
      cid=secrets.token_hex(8);store(c,'custody',{'id':cid,'reference':'CUST-'+cid,'version':1,'status':'Pending','tenant_id':'demo','branch_id':r['branch_id'],'sale':r['id'],'customer':r.get('customer'),'product':p['id'],'owned_quantity':ds(q),'collected_quantity':'0','warehouse':'warehouse-1','business_date':r['business_date']})
     else:
      from inventory_workflows import change_stock
      change_stock(c,user,p['id'],-q,warehouse_id=r.get('warehouse'),source=r['reference'])
    r['total']=ds(total(r))
   if e=='payment':
    amount=number(r.get('amount'));allocated=number(r.get('allocation_amount') or amount)
    if amount<=0 or allocated>amount:raise APIError(422,'Invalid payment allocation.')
    if r.get('sale'):
     sale=get(c,'sale',r['sale']);check(user,'sale','read',sale)
     paid=sum((number(x.get('allocation_amount') or x.get('amount')) for x in all_records(c,'payment') if x.get('sale')==sale['id'] and x['status']=='Completed'),Decimal(0))
     if allocated>total(sale)-paid:raise APIError(422,'Payment allocation exceeds outstanding sale.')
   if e=='journal' and number(r.get('debit',0))!=number(r.get('credit',0)):raise APIError(422,'Journal batch must balance.')
  r['status']=new
  if verb=='submit':r['submitted_by']=user['id']
 else:raise APIError(422,'Command is not available for this entity.')
 r['version']+=1;store(c,e,r);event(c,user,e,id,verb,reason);return {'record':safe_record(user,e,r)}
class Handler(SimpleHTTPRequestHandler):
 def __init__(self,*a,**kw):super().__init__(*a,directory=str(ROOT),**kw)
 def log_message(self,fmt,*args):pass
 def send_json(self,status,data,cookie=None):
  raw=json.dumps(data).encode();self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(raw)));self.send_header('Cache-Control','no-store');self.send_header('X-Content-Type-Options','nosniff')
  if cookie:self.send_header('Set-Cookie',cookie)
  self.end_headers();self.wfile.write(raw)
 def body(self):
  try:n=int(self.headers.get('Content-Length','0'))
  except ValueError:raise APIError(400,'Invalid content length.')
  if n<0:raise APIError(400,'Invalid content length.')
  if n>2000000:raise APIError(413,'Request too large.')
  try:return json.loads(self.rfile.read(n) or b'{}')
  except ValueError:raise APIError(400,'Invalid JSON.')
 def auth(self,c,write=False):
  try:jar=SimpleCookie(self.headers.get('Cookie',''));token=jar['quincai_session'].value
  except (KeyError,ValueError):raise APIError(401,'Sign in to continue.')
  row=c.execute('SELECT s.*,u.* FROM sessions s JOIN users u ON u.id=s.user_id WHERE token_hash=?',(hashlib.sha256(token.encode()).hexdigest(),)).fetchone()
  if not row or row['expires']<time.time() or not row['active']:raise APIError(401,'Session expired.')
  user=dict(c.execute('SELECT * FROM users WHERE id=?',(row['user_id'],)).fetchone())
  if row['auth_version']!=user['auth_version']:raise APIError(401,'Session revoked.')
  if write:
   if not hmac.compare_digest(self.headers.get('X-CSRF-Token',''),row['csrf']):raise APIError(403,'CSRF token missing or invalid.')
   origin=self.headers.get('Origin')
   if origin and urlparse(origin).netloc!=self.headers.get('Host'):raise APIError(403,'Cross-origin request rejected.')
  return user,dict(row)
 def do_GET(self):
  path=urlparse(self.path).path
  if path.startswith('/api/'):return self.api('GET',path)
  # Never serve runtime databases, backend source, fixtures or test files.
  if path.startswith(('/server/','/runtime/','/tests/','/.')):return self.send_error(404)
  allowed=path=='/' or path.endswith(('.html','.js','.css','.png','.jpg','.svg','.ico'))
  if not allowed:return self.send_error(404)
  super().do_GET()
 def do_POST(self):self.api('POST',urlparse(self.path).path)
 def do_PUT(self):self.api('PUT',urlparse(self.path).path)
 def api(self,method,path):
  try:
   with LOCK,connect() as c:
    b=self.body() if method in ('POST','PUT') else {}
    if not isinstance(b,dict):raise APIError(400,'Request body must be a JSON object.')
    if path.startswith('/api/mobile-money/callback/') and method in ('POST','PUT'):
     parts=path.split('/')
     if len(parts)!=6:raise APIError(404,'Invalid callback path.')
     from mobile_money import callback
     return self.send_json(202,callback(c,parts[4],parts[5],b))
    if method=='PUT':raise APIError(405,'PUT is supported only for provider callbacks.')
    if method=='GET' and path not in ('/api/meta','/api/session','/api/state','/api/users','/api/mobile-money/config','/api/mobile-money/intents','/api/backup/list','/api/system/integrity'):raise APIError(405,'Use POST for this operation.')
    if path=='/api/meta':return self.send_json(200,{'demo':DEMO,'roles':{k:v['label'] for k,v in ROLES.items()}})
    if path=='/api/login' and method=='POST':
     email=str(b.get('email','')).strip().lower();key=self.client_address[0]+':'+email
     attempt=c.execute('SELECT * FROM attempts WHERE key=?',(key,)).fetchone()
     if attempt and attempt['count']>=5 and attempt['until']>time.time():raise APIError(429,'Too many attempts. Try again later.')
     u=c.execute('SELECT * FROM users WHERE email=?',(email,)).fetchone();salt=u['salt'] if u else '00'*16
     valid=hmac.compare_digest(password_hash(str(b.get('password','')),salt),u['hash'] if u else '0'*128)
     if not u or not valid or not u['active']:
      count=attempt['count']+1 if attempt and attempt['until']>time.time() else 1
      c.execute('INSERT OR REPLACE INTO attempts VALUES(?,?,?)',(key,count,time.time()+300));c.commit();raise APIError(401,'Invalid sign-in details.')
     c.execute('DELETE FROM attempts WHERE key=?',(key,))
     token=secrets.token_urlsafe(32);csrf=secrets.token_urlsafe(32);c.execute('INSERT INTO sessions VALUES(?,?,?,?,?)',(hashlib.sha256(token.encode()).hexdigest(),u['id'],csrf,time.time()+28800,u['auth_version']))
     event(c,dict(u),'identity',u['id'],'Sign in');c.commit()
     secure='; Secure' if os.environ.get('QUINCAI_HTTPS')=='1' else ''
     return self.send_json(200,{'user':{k:u[k] for k in ['id','email','role','branch_id','customer_id']},'policy':public_policy(u['role']),'csrf':csrf},'quincai_session='+token+'; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800'+secure)
    user,session=self.auth(c,method=='POST')
    from operations import handle_api as operations_api
    from mobile_money import handle_api as money_api
    for endpoint in (operations_api,money_api):
     handled=endpoint(c,user,path,b)
     if handled is not None:
      c.commit();return self.send_json(200,handled)
    if path=='/api/session':return self.send_json(200,{'user':{k:user[k] for k in ['id','email','role','branch_id','customer_id']},'policy':public_policy(user['role']),'csrf':session['csrf']})
    if path=='/api/state':
     entities={e:[safe_record(user,e,r) for r in all_records(c,e) if scoped(user,r,e)] if allow(user,e) else [] for e in SCHEMAS}
     audits=[json.loads(r[0]) for r in c.execute('SELECT data FROM audit ORDER BY seq DESC LIMIT 200') if scoped(user,json.loads(r[0]),'audit')] if allow(user,'audit') else []
     if allow(user,'audit'):entities['audit']=[{**a,'reference':'AUD-'+a['id'][:8],'status':'Recorded','version':1,'business_date':a['date'][:10]} for a in audits]
     snaps=[json.loads(r[0]) for r in c.execute('SELECT data FROM snapshots')] 
     snaps=[s for s in snaps if allow(user,s['source_entity']) and scoped(user,s['record'],s['source_entity'])]
     for snap in snaps:
      snap['record']=safe_record(user,snap['source_entity'],snap['record'])
      authorized_ids={e+':'+r['id'] for e,rs in entities.items() for r in rs}
      snap['labels']={k:v for k,v in snap.get('labels',{}).items() if k in authorized_ids}
      snap['product_units']={k:v for k,v in snap.get('product_units',{}).items() if 'product:'+k in authorized_ids}
      snap['related_records']=[{**x,'record':safe_record(user,x['entity'],x['record'])} for x in snap['related_records'] if allow(user,x['entity']) and scoped(user,x['record'],x['entity'])]
     return self.send_json(200,{'version':3,'entities':entities,'audit':audits,'snapshots':snaps,'basket':[],'preferences':{'branch':user['branch_id'],'locale':'en'}})
    if path=='/api/users':
     if user['role']!='owner':raise APIError(403,'Owner permission required.')
     return self.send_json(200,{'users':[dict(r) for r in c.execute('SELECT id,email,role,branch_id,active FROM users WHERE role != ? ORDER BY email',('platform_admin',))]})
    if path=='/api/users/update':
     if user['role']!='owner':raise APIError(403,'Owner permission required.')
     target=c.execute('SELECT * FROM users WHERE id=?',(b.get('id'),)).fetchone()
     if not target or target['role']=='platform_admin':raise APIError(404,'User not found.')
     if target['id']==user['id']:raise APIError(403,'You cannot change your own access.')
     role=b.get('role');branch=b.get('branch_id');active=b.get('active')
     if role not in ROLES or role in ('platform_admin','customer') or target['role']=='customer':raise APIError(422,'Customer and platform identities cannot be converted to staff roles.')
     get(c,'branch',branch)
     if not isinstance(active,bool):raise APIError(422,'Active must be true or false.')
     if not str(b.get('reason','')).strip():raise APIError(422,'Access change reason required.')
     c.execute('UPDATE users SET role=?,branch_id=?,active=?,auth_version=auth_version+1 WHERE id=?',(role,branch,int(active),target['id']))
     c.execute('DELETE FROM sessions WHERE user_id=?',(target['id'],));event(c,user,'identity',target['id'],'Access changed',str(b['reason']));c.commit();return self.send_json(200,{'ok':True,'sessions_revoked':True})
    if path=='/api/logout':
     c.execute('DELETE FROM sessions WHERE token_hash=?',(session['token_hash'],));c.commit();return self.send_json(200,{'ok':True},'quincai_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0')
    if path=='/api/password':
     if not hmac.compare_digest(password_hash(str(b.get('current','')),user['salt']),user['hash']):raise APIError(401,'Current password is incorrect.')
     new=str(b.get('password',''))
     if len(new)<12:raise APIError(422,'Use at least 12 characters.')
     salt=secrets.token_hex(16);c.execute('UPDATE users SET salt=?,hash=?,auth_version=auth_version+1 WHERE id=?',(salt,password_hash(new,salt),user['id']));c.execute('DELETE FROM sessions WHERE user_id=?',(user['id'],));event(c,user,'identity',user['id'],'Password changed');c.commit();return self.send_json(200,{'ok':True,'reauthenticate':True})
    if path=='/api/record':
     e=b.get('entity');check(user,e,'write');
     if e in ('audit','role','staff','identity','custody'):raise APIError(403,'Use the dedicated identity or audit service; these records are not editable drafts.')
     id=b.get('id');old=get(c,e,id) if id else None
     if old:
      check(user,e,'write',old)
      if old['version']!=b.get('version'):raise APIError(409,'Record version changed.')
      if old['status'] not in ('Draft','Rejected') and e not in ('product','customer','supplier','company','branch','category','unit','price','template','printer','industry','content'):raise APIError(409,'Finalized record is read-only.')
     fields=validate(c,user,e,b.get('data',{}),old);id=id or secrets.token_hex(10)
     r={**(old or {}),**fields,'id':id,'reference':old['reference'] if old else e[:4].upper()+'-'+id[:8],'version':old['version']+1 if old else 1,'status':old['status'] if old else 'Draft','branch_id':old['branch_id'] if old else user['branch_id'],'company_id':'company-1','tenant_id':'demo','business_date':fields.get('business_date') or (old or {}).get('business_date') or '2026-11-17','lines':(old or {}).get('lines',[]),'created_by':(old or {}).get('created_by',user['id'])}
     if user['role']=='customer' and r.get('customer')!=user['customer_id']:raise APIError(403,'You can only create requests for your own customer account.')
     store(c,e,r);event(c,user,e,id,'Draft saved');c.commit();return self.send_json(200,{'record':safe_record(user,e,r)})
    if path=='/api/sales/draft':
     check(user,'sale','write')
     operation=b.get('operation_id')
     if not isinstance(operation,str) or not operation or len(operation)>100:raise APIError(422,'Operation ID required.')
     digest=hashlib.sha256(json.dumps({'path':path,'body':b},sort_keys=True).encode()).hexdigest()
     prior=c.execute('SELECT * FROM commands WHERE user_id=? AND operation_id=?',(user['id'],operation)).fetchone()
     if prior:
      if prior['request_hash']!=digest:raise APIError(409,'Operation ID reused with different data.')
      return self.send_json(200,json.loads(prior['result']))
     lines=b.get('lines')
     if not isinstance(lines,list) or not 1<=len(lines)<=500:raise APIError(422,'Provide 1 to 500 sale lines.')
     fields=validate(c,user,'sale',b.get('data',{}),None);id=secrets.token_hex(10)
     r={**fields,'id':id,'reference':'SALE-'+id[:8],'version':1,'status':'Draft','branch_id':user['branch_id'],'company_id':'company-1','tenant_id':'demo','lines':[],'created_by':user['id']}
     store(c,'sale',r)
     for line in lines:
      if not isinstance(line,dict):raise APIError(422,'Invalid sale line.')
      result=command(c,user,{'entity':'sale','id':id,'version':r['version'],'command':'add-line','payload':line,'reason':'POS basket save'})
      r=get(c,'sale',id)
     result={'record':safe_record(user,'sale',r)}
     c.execute('INSERT INTO commands VALUES(?,?,?,?)',(user['id'],operation,digest,json.dumps(result)));event(c,user,'sale',id,'POS draft saved');c.commit();return self.send_json(200,result)
    if path=='/api/command':
     operation=b.get('operation_id')
     if not operation or len(operation)>100:raise APIError(422,'Operation ID required.')
     check(user,b.get('entity'),'approve' if b.get('command')=='approve' else 'write',get(c,b.get('entity'),b.get('id')))
     digest=hashlib.sha256(json.dumps(b,sort_keys=True).encode()).hexdigest()
     previous=c.execute('SELECT * FROM commands WHERE user_id=? AND operation_id=?',(user['id'],operation)).fetchone()
     if previous:
      if previous['request_hash']!=digest:raise APIError(409,'Operation ID reused with different data.')
      return self.send_json(200,json.loads(previous['result']))
     result=command(c,user,b);c.execute('INSERT INTO commands VALUES(?,?,?,?)',(user['id'],operation,digest,json.dumps(result)));c.commit();return self.send_json(200,result)
    if path=='/api/backup':
     if user['role']!='owner':raise APIError(403,'Owner permission required.')
     payload={e:all_records(c,e) for e in SCHEMAS};raw=json.dumps(payload,sort_keys=True);id=secrets.token_hex(10);digest=hashlib.sha256(raw.encode()).hexdigest();c.execute('INSERT INTO backups VALUES(?,?,?,?)',(id,time.time(),raw,digest));event(c,user,'backup',id,'Backup created');c.commit();return self.send_json(200,{'id':id,'digest':digest,'entities':payload})
    if path=='/api/backup/verify':
     if user['role']!='owner':raise APIError(403,'Owner permission required.')
     raw=json.dumps(b.get('entities',{}),sort_keys=True);valid=hmac.compare_digest(hashlib.sha256(raw.encode()).hexdigest(),str(b.get('digest','')))
     return self.send_json(200,{'valid':valid,'records':sum(len(x) for x in b.get('entities',{}).values())})
    if path=='/api/snapshot':
     d=next((x for x in CONTRACT['documents'] if x['id']==b.get('doc')),None)
     if not d:raise APIError(404,'Unknown template.')
     e=d['entity'];check(user,e);r=get(c,e,b.get('record'));check(user,e,'read',r)
     snap={'id':'SNAP-'+secrets.token_hex(8),'doc_id':d['id'],'source_entity':e,'source_id':r['id'],'record':safe_record(user,e,r),'company':all_records(c,'company')[0],'created_at':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),'template_version':'4.0','labels':{},'product_units':{},'related_records':[]}
     for rel,sch in SCHEMAS.items():
      if not allow(user,rel):continue
      for x in all_records(c,rel):
       if not scoped(user,x,rel):continue
       snap['labels'][rel+':'+x['id']]=x.get('name') or x.get('legal_name') or x['reference']
       if rel=='product':snap['product_units'][x['id']]=x.get('base_unit','piece')
       for f in sch['fields']:
        if f.get('entity')==e and x.get(f['key'])==r['id']:snap['related_records'].append({'entity':rel,'record':safe_record(user,rel,x),'relation':f['label']})
     snap['digest']=hashlib.sha256(json.dumps(snap,sort_keys=True).encode()).hexdigest()
     c.execute('INSERT INTO snapshots VALUES(?,?,?,?,?)',(snap['id'],e,r['id'],r['branch_id'],json.dumps(snap)));event(c,user,e,r['id'],'Snapshot created');c.commit();return self.send_json(200,{'snapshot':snap})
    if path=='/api/integration':
     check(user,b.get('entity'),'write')
     raise APIError(503,'External adapter is not configured. No external operation was performed.')
    raise APIError(404,'Unknown API route.')
  except APIError as err:self.send_json(err.status,{'error':err.message})
  except Exception as err:
   print(type(err).__name__,str(err),flush=True);self.send_json(500,{'error':'Request failed; no transaction committed.'})
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--host',default='127.0.0.1');parser.add_argument('--data-dir');parser.add_argument('--strictPort',action='store_true');parser.add_argument('--port',type=int,default=int(os.getenv('PORT','5173')));parser.add_argument('--demo',action='store_true');args=parser.parse_args();DEMO=args.demo
 if args.data_dir:DBPATH=Path(args.data_dir)/'quincai.sqlite3'
 initialize()
 print('QuincaiSoft local integration server on port',args.port,flush=True)
 ThreadingHTTPServer((args.host,args.port),Handler).serve_forever()
