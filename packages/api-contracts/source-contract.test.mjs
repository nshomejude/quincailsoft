import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const api=JSON.parse(fs.readFileSync(new URL('./openapi.json',import.meta.url),'utf8'));
const inventory=JSON.parse(fs.readFileSync(new URL('./endpoint-inventory.json',import.meta.url),'utf8'));
test('qualified ResponseEntity workforce exports retain declared media and string payload',()=>{
 for(const [path,media] of [['/api/v1/workforce/payroll/{id}/print','text/html'],['/api/v1/workforce/payroll/export.csv','text/csv']]){
  const content=api.paths[path].get.responses['200'].content;
  assert.deepEqual(Object.keys(content),[media]);assert.equal(content[media].schema.type,'string');
 }
 assert.equal(inventory.unresolvedJavaTypes.some(t=>t.includes('ResponseEntity')),false);
});
test('billing printable records retain HTML content type',()=>{
 for(const path of ['/admin/billing/invoices/{id}/print','/api/v1/billing/invoices/{id}/print','/admin/subscriptions/payments/{id}/print','/api/v1/billing/payments/{id}/print'])assert.equal(api.paths[path].get.responses['200'].content['text/html'].schema.type,'string');
});
test('detailed payroll exposes nested payroll and explicit deduction line shapes',()=>{
 const operation=api.paths['/api/v1/workforce/payroll/detailed'].post;
 assert.equal(operation.requestBody.content['application/json'].schema.$ref,'#/components/schemas/WorkforceService_DetailedPayroll');
 const detailed=api.components.schemas.WorkforceService_DetailedPayroll.properties;
 assert.equal(detailed.payroll.$ref,'#/components/schemas/WorkforceService_PayrollDraft');
 assert.equal(detailed.deductions.items.$ref,'#/components/schemas/WorkforceService_Deduction');
 assert.equal(api.components.schemas.WorkforceService_Deduction.properties.amountXaf.type,'string');
 assert(operation.parameters.some(p=>p.in==='header'&&p.name==='Idempotency-Key'&&p.required));
});
test('report route enum includes actual expanded source catalog',()=>{
 const source=fs.readFileSync(new URL('../../backend/business-modules/reporting/src/main/java/com/quincaisoft/reporting/application/DefaultReportingService.java',import.meta.url),'utf8');
 const codes=[...source.matchAll(/Map\.entry\("([^"]+)",new Spec\(/g)].map(m=>m[1]).sort();
 assert(codes.includes('customer-custody'));assert(codes.includes('recalls'));
 for(const [path,item] of Object.entries(api.paths))if(path.startsWith('/api/v1/reports/'))for(const operation of Object.values(item))for(const parameter of operation.parameters??[])if(parameter.name==='code')assert.deepEqual(parameter.schema.enum,codes);
});
