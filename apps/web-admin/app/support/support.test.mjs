import test from 'node:test';
import assert from 'node:assert/strict';
import {supportInput} from './contracts.ts';
import {supportText} from './copy.ts';
const id='11111111-1111-1111-1111-111111111111',now=Date.parse('2026-11-18T12:00:00Z');
function form(extra={}){const fd=new FormData();for(const [key,value] of Object.entries({operationId:id,action:'consent',operatorId:id,ticketReference:'SUP-10',reason:'Diagnose scoped counts',expiresAt:'2026-11-18T13:00:00Z',...extra}))fd.set(key,value);return fd;}
test('owner consent preserves the named operator, purpose and bounded expiration',()=>{const result=supportInput(form(),now);assert.equal(result.path,'support/grants');assert.equal(result.body.operatorId,id);assert.equal(result.body.expiresAt,'2026-11-18T13:00:00.000Z');});
test('consent rejects expired, overly long and timezone-free grants',()=>{for(const expiresAt of ['2026-11-18T11:00:00Z','2026-11-19T12:00:01Z','2026-11-18T13:00:00'])assert.throws(()=>supportInput(form({expiresAt}),now));});
test('revocation is version checked and cannot name another API action',()=>{const result=supportInput(form({action:'revoke',id,expectedVersion:'2'}),now);assert.equal(result.body.expectedVersion,2);assert.equal(result.path,'support/grants/'+id+'/revoke');assert.throws(()=>supportInput(form({action:'impersonate'}),now));assert.throws(()=>supportInput(form({action:'revoke',id,expectedVersion:'9007199254740993'}),now));});
test('French consent and error copy is available',()=>{assert.equal(supportText('fr','Revoke consent'),'Révoquer le consentement');assert.match(supportText('fr','Consent must expire within the next 24 hours. Use an ISO timestamp with timezone.'),/24 heures/);});
