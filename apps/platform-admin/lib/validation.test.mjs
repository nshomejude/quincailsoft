import test from 'node:test';
import assert from 'node:assert/strict';
import {uuid,version,money,billingTotal,date,xaf} from './validation.ts';
test('preserves integer amounts beyond JavaScript safe number range',()=>{assert.equal(money('9007199254740993'),'9007199254740993');assert.equal(billingTotal('9007199254740993','1','2'),'9007199254740994');assert.match(xaf('9007199254740993'),/XAF$/)});
for(const input of ['-1','1.2','1e4','01','NaN','9223372036854775808'])test('rejects noncanonical or overflowing amount '+input,()=>assert.throws(()=>money(input)));
test('total overflow and excessive discount fail',()=>{assert.throws(()=>billingTotal('9223372036854775807','0','1'));assert.throws(()=>billingTotal('10','10','0'));});
test('invalid and missing IDs rejected',()=>{assert.throws(()=>uuid('../../admin'));assert.throws(()=>uuid('00000000-0000-0000-0000-00000000000x'));assert.equal(uuid('00000000-0000-0000-0000-000000000001'),'00000000-0000-0000-0000-000000000001')});
test('version cannot silently lose precision',()=>{assert.equal(version('12'),12);assert.throws(()=>version('9007199254740993'));assert.throws(()=>version('-1'));});
test('calendar dates cannot silently roll over',()=>{assert.equal(date('2024-02-29'),'2024-02-29');assert.throws(()=>date('2025-02-29'));assert.throws(()=>date('2024-13-01'));});

test('bank evidence digest is normalized and malformed evidence rejected',async()=>{const {sha256}=await import('./validation.ts');assert.equal(sha256('AB'.repeat(32)),'ab'.repeat(32));assert.throws(()=>sha256('x'.repeat(64)));assert.throws(()=>sha256('ab'));});
