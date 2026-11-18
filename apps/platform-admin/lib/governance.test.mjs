import test from 'node:test';
import assert from 'node:assert/strict';
import {governanceInput} from './governance.ts';
const id='11111111-1111-1111-1111-111111111111';
function form(extra={}){const f=new FormData();for(const [k,v]of Object.entries({operationId:id,action:'request',tenantId:id,kind:'SUSPEND',expectedVersion:'7',reason:'Security incident',reference:'INC-10',...extra}))f.set(k,v);return f;}
test('governance uses displayed target version and independent command identity',()=>{const input=governanceInput(form());assert.equal(input.body.expectedVersion,7);assert.equal(input.body.operationId,id);assert.equal(input.body.kind,'SUSPEND');assert.equal(input.path,'/admin/governance/tenants/'+id+'/requests');});
test('hold release requires a valid target and unsafe versions fail',()=>{assert.throws(()=>governanceInput(form({kind:'RELEASE_LEGAL_HOLD'})));assert.equal(governanceInput(form({kind:'RELEASE_LEGAL_HOLD',holdId:id})).body.holdId,id);assert.throws(()=>governanceInput(form({expectedVersion:'9007199254740993'})));assert.throws(()=>governanceInput(form({kind:'DELETE_TENANT'})));});
