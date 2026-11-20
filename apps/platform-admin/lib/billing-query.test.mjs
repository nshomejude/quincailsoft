import test from 'node:test';
import assert from 'node:assert/strict';
import {billingQuery} from './billing-query.ts';
const tenantId='11111111-1111-1111-1111-111111111111';
test('paging preserves opaque cursor and tenant in encoded links',()=>{const q=billingQuery({tenantId,cursor:'opaque+/_=token',limit:'25'});assert.equal(q.limit,25);assert.equal(new URLSearchParams(q.params.toString()).get('cursor'),'opaque+/_=token');assert.equal(q.params.get('tenantId'),tenantId);});
test('paging rejects oversized limits, duplicate filters and invalid cursors',()=>{for(const limit of ['0','101','1.5','01','1e2'])assert.throws(()=>billingQuery({tenantId,limit}));assert.throws(()=>billingQuery({tenantId:[tenantId,tenantId]}));assert.throws(()=>billingQuery({tenantId,cursor:'line\nbreak'}));assert.throws(()=>billingQuery({tenantId,cursor:'a'.repeat(4097)}));});
