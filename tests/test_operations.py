import sys, tempfile, unittest, json, uuid, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'server'))
import server
import operations as ops

class OperationsTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.oldpath = server.DBPATH
        self.olddemo = server.DEMO
        server.DBPATH = Path(self.tmp.name) / 'db.sqlite3'
        server.DEMO = True
        server.initialize()
        with server.connect() as c:
            self.owner = dict(c.execute("SELECT * FROM users WHERE id='owner'").fetchone())
            self.cashier = dict(c.execute("SELECT * FROM users WHERE id='cashier'").fetchone())
            self.manager = dict(c.execute("SELECT * FROM users WHERE id='manager'").fetchone())
    def tearDown(self):
        server.DBPATH = self.oldpath
        server.DEMO = self.olddemo
        self.tmp.cleanup()
    def call(self, path, body=None, user=None):
        with server.connect() as c:
            return ops.handle_api(c, user or self.owner, path, body or {})
    def oid(self): return str(uuid.uuid4())
    def backup(self): return self.call('/api/backup/create')
    def test_backup_verify_and_secret_exclusion(self):
        b = self.backup()
        self.assertNotIn('users', b['entities'])
        self.assertNotIn('sessions', b['entities'])
        result = self.call('/api/backup/verify', {'backup_id': b['id']})
        self.assertTrue(result['valid'])
    def test_restore_safety_backup_version_and_sessions(self):
        b = self.backup()
        with server.connect() as c:
            r = server.get(c, 'product', 'product-1'); r['name'] = 'Changed'; r['version'] += 1
            server.store(c, 'product', r)
            c.execute('INSERT INTO sessions VALUES(?,?,?,?,?)', ('test-session', 'cashier', 'test-csrf', time.time()+600, 1))
            digest = ops.state_digest(c)
        body = {'backup_id': b['id'], 'expected_state_digest': digest, 'reason': 'Test recovery', 'operation_id': self.oid()}
        result = self.call('/api/backup/restore', body)
        self.assertTrue(result['restored'])
        self.assertNotEqual(result['safety_backup_id'], b['id'])
        with server.connect() as c:
            restored = server.get(c, 'product', 'product-1')
            self.assertEqual(restored['name'], b['entities']['product'][0]['name'])
            self.assertGreater(restored['version'], r['version'])
            self.assertEqual(c.execute('SELECT COUNT(*) FROM sessions').fetchone()[0], 0)
            self.assertGreater(c.execute('SELECT COUNT(*) FROM audit').fetchone()[0], 0)
        self.assertEqual(self.call('/api/backup/restore', body), result)
    def test_stale_restore_rejected(self):
        b = self.backup()
        with self.assertRaises(server.APIError) as cm:
            self.call('/api/backup/restore', {'backup_id':b['id'], 'expected_state_digest':'old', 'reason':'test', 'operation_id':self.oid()})
        self.assertEqual(cm.exception.status, 409)
    def test_tampered_backup_rejected(self):
        b = self.backup()
        with server.connect() as c:
            p = b['entities']; p['product'][0]['name'] = 'tamper'
            c.execute('UPDATE backups SET payload=? WHERE id=?', (json.dumps(p), b['id']))
        with self.assertRaises(server.APIError): self.call('/api/backup/verify', {'backup_id':b['id']})
    def test_restore_with_provider_evidence_blocked(self):
        b = self.backup()
        with server.connect() as c:
            c.execute('CREATE TABLE payment_intents(id TEXT)'); c.execute("INSERT INTO payment_intents VALUES('external-1')")
        with self.assertRaises(server.APIError) as cm:
            self.call('/api/backup/restore', {'backup_id':b['id'],'expected_state_digest':b['digest'],'reason':'test','operation_id':self.oid()})
        self.assertEqual(cm.exception.status,409)
    def test_restore_with_finance_ledger_blocked(self):
        b = self.backup()
        with server.connect() as c:
            c.execute('CREATE TABLE finance_postings(id TEXT)'); c.execute("INSERT INTO finance_postings VALUES('posting-1')")
        with self.assertRaises(server.APIError) as cm:
            self.call('/api/backup/restore', {'backup_id':b['id'],'expected_state_digest':b['digest'],'reason':'test','operation_id':self.oid()})
        self.assertEqual(cm.exception.status,409)
    def test_backup_permission(self):
        with self.assertRaises(server.APIError) as cm: self.call('/api/backup/create', user=self.cashier)
        self.assertEqual(cm.exception.status,403)
    def test_csv_import_commit_and_replay(self):
        p = self.call('/api/import/preview', {'entity':'product','csv':'sku,name,base_unit,selling_price,track_stock\nNEW-01,New item,piece,12.50,true\n'})
        self.assertTrue(p['can_commit'])
        body={'preview_id':p['preview_id'],'digest':p['digest'],'reason':'Import approved','operation_id':self.oid()}
        r=self.call('/api/import/commit',body)
        self.assertEqual(r['count'],1)
        self.assertEqual(self.call('/api/import/commit',body),r)
        with server.connect() as c:
            item=server.get(c,'product',r['records'][0]);self.assertEqual(item['stock'],'0');self.assertEqual(item['status'],'Draft');self.assertTrue(item['track_stock'])
    def test_csv_validation_no_preview_for_errors(self):
        p=self.call('/api/import/preview',{'entity':'product','csv':'sku,name,base_unit,selling_price\nNEW,Name,piece,2\nNEW,Other,piece,3\n'})
        self.assertFalse(p['can_commit']);self.assertIsNone(p['preview_id']);self.assertEqual(p['errors'][0]['row'],3)
    def test_import_atomic_on_concurrent_duplicate(self):
        p=self.call('/api/import/preview',{'entity':'product','csv':'sku,name,base_unit,selling_price\nUNIQUE-A,One,piece,1\nUNIQUE-B,Two,piece,2\n'})
        with server.connect() as c:
            product=server.get(c,'product','product-1');product['id']='new-existing';product['sku']='UNIQUE-B';server.store(c,'product',product)
        with self.assertRaises(server.APIError):
            self.call('/api/import/commit',{'preview_id':p['preview_id'],'digest':p['digest'],'reason':'Test','operation_id':self.oid()})
        with server.connect() as c:self.assertFalse(any(r.get('sku')=='UNIQUE-A' for r in server.all_records(c,'product')))
    def test_import_cannot_write_stock_or_financial_posting(self):
        p=self.call('/api/import/preview',{'entity':'product','csv':'sku,name,base_unit,selling_price,stock\nNEW,Name,piece,2,999\n'})
        self.assertFalse(p['can_commit'])
        with self.assertRaises(server.APIError):self.call('/api/import/preview',{'entity':'payment','csv':'amount\n100\n'})
    def test_cross_account_import_denied(self):
        p=self.call('/api/import/preview',{'entity':'customer','csv':'name,email\nTest,test@example.com\n'})
        with self.assertRaises(server.APIError) as cm:
            self.call('/api/import/commit',{'preview_id':p['preview_id'],'digest':p['digest'],'reason':'Test','operation_id':self.oid()},self.manager)
        self.assertEqual(cm.exception.status,404)
    def test_session_revocation_scope(self):
        with self.assertRaises(server.APIError) as cm:self.call('/api/sessions/revoke',{'user_id':'owner','reason':'Test'},self.cashier)
        self.assertEqual(cm.exception.status,403)
        r=self.call('/api/sessions/revoke',{'user_id':'cashier','reason':'Lost device'},self.cashier)
        self.assertTrue(r['reauthenticate'])
    def test_integrity_report(self):
        r=self.call('/api/system/integrity');self.assertTrue(r['sqlite_ok']);self.assertTrue(r['business_relations_ok'],r)

if __name__=='__main__':unittest.main()
