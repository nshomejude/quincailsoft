import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const api=JSON.parse(fs.readFileSync(new URL('./openapi.json',import.meta.url),'utf8'));
test('retention approval and purge preserve command identity and evidence schema',()=>{
 const root='/api/v1/operations/retention/exports';
 for(const path of [root,root+'/{id}/approve',root+'/{id}/execute']){
  const op=api.paths[path].post;
  for(const name of ['Idempotency-Key','X-Request-ID'])assert(op.parameters.some(p=>p.name===name&&p.in==='header'&&p.required));
  assert.equal(op.responses['200'].content['application/json'].schema.$ref,'#/components/schemas/ExportRetentionService_Decision');
 }
 const fields=api.components.schemas.ExportRetentionService_Decision.properties;
 for(const name of ['exportId','digest','expiresAt','reason','requestedBy','reviewedBy','version','purgedAt'])assert(fields[name],name);
 assert.equal(fields.version.type,'string');
});
