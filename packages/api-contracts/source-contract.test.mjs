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
test('governance and quota enum schemas preserve their declaration owners and exact values',()=>{
 const schemas=api.components.schemas;
 assert.deepEqual(schemas.PlatformGovernanceService_Kind.enum,['SUSPEND','RESUME','PLACE_LEGAL_HOLD','RELEASE_LEGAL_HOLD']);
 assert.equal(schemas.PlatformGovernanceService_Request.properties.kind.$ref,'#/components/schemas/PlatformGovernanceService_Kind');
 assert.equal(schemas.PlatformGovernanceService_Review.properties.kind.$ref,'#/components/schemas/PlatformGovernanceService_Kind');
 assert.deepEqual(schemas.SubscriptionQuotaService_Resource.enum,['STAFF_SEATS','BRANCHES','WAREHOUSES']);
 for(const name of ['Usage','Reserve','Reservation'])assert.equal(schemas['SubscriptionQuotaService_'+name].properties.resource.$ref,'#/components/schemas/SubscriptionQuotaService_Resource');
});
test('both session aliases expose the exact typed scope and authority list',()=>{
 for(const route of ['/api/v1/session','/api/v1/me'])assert.equal(api.paths[route].get.responses['200'].content['application/json'].schema.$ref,'#/components/schemas/SessionController_SessionView');
 const view=api.components.schemas.SessionController_SessionView.properties;
 assert.deepEqual(Object.keys(view).sort(),['authorities','scope']);
 assert.deepEqual(view.authorities,{type:'array',items:{type:'string'}});
 assert.equal(view.scope.$ref,'#/components/schemas/TenantScope');
 const scope=api.components.schemas.TenantScope.properties;
 assert.deepEqual(Object.keys(scope).sort(),['actorId','branchId','companyId','tenantId']);
 for(const value of Object.values(scope))assert.deepEqual(value,{type:'string',format:'uuid'});
});
test('fulfillment canonical record components survive the legacy overload and expose delivery and pick-list detail',()=>{
 const schemas=api.components.schemas;
 assert.deepEqual(Object.keys(schemas.FulfillmentService_Draft.properties),['operationId','saleId','customerId','businessDate','reason','delivery']);
 assert.equal(schemas.FulfillmentService_Draft.properties.delivery.$ref,'#/components/schemas/FulfillmentService_DeliveryDetails');
 assert.equal(schemas.FulfillmentService_Fulfillment.properties.delivery.$ref,'#/components/schemas/FulfillmentService_DeliveryDetails');
 assert.deepEqual(Object.keys(schemas.FulfillmentService_DeliveryDetails.properties),['method','scheduledDate','address','recipientName','recipientPhone','notes']);
 assert.deepEqual(schemas.FulfillmentService_DeliveryDetails.properties.scheduledDate,{type:'string',format:'date'});
 assert.deepEqual(Object.keys(schemas.FulfillmentService_PickList.properties),['fulfillmentId','saleId','warehouseId','status','lines']);
 assert.equal(schemas.FulfillmentService_PickList.properties.lines.items.$ref,'#/components/schemas/FulfillmentService_PickLine');
 assert.deepEqual(schemas.FulfillmentService_PickLine.properties.quantityMilli,{type:'string'});
 assert.equal(api.paths['/api/v1/fulfillments/{id}/picklist'].get.responses['200'].content['application/json'].schema.$ref,'#/components/schemas/FulfillmentService_PickList');
 assert.equal(api.paths['/api/v1/fulfillments'].post.requestBody.content['application/json'].schema.$ref,'#/components/schemas/FulfillmentService_Draft');
});
test('partial fulfillment dispatch and history retain exact quantities and command identity',()=>{
 const schemas=api.components.schemas;
 const command=api.paths['/api/v1/fulfillments/{id}/dispatch-lines'].post;
 assert.equal(command.requestBody.content['application/json'].schema.$ref,'#/components/schemas/FulfillmentService_DispatchLines');
 assert.deepEqual(Object.keys(schemas.FulfillmentService_DispatchLines.properties),['operationId','version','reason','lines']);
 assert.equal(schemas.FulfillmentService_DispatchLines.properties.lines.items.$ref,'#/components/schemas/FulfillmentService_PickLine');
 assert.deepEqual(schemas.FulfillmentService_PickLine.properties.quantityMilli,{type:'string'});
 for(const name of ['Idempotency-Key','X-Request-ID'])assert(command.parameters.some(p=>p.in==='header'&&p.name===name&&p.required));
 assert.equal(command.responses['200'].content['application/json'].schema.$ref,'#/components/schemas/FulfillmentService_Fulfillment');
 const history=api.paths['/api/v1/fulfillments/{id}/dispatches'].get.responses['200'].content['application/json'].schema;
 assert.equal(history.type,'array');assert.equal(history.items.$ref,'#/components/schemas/FulfillmentService_DispatchEntry');
 const entry=schemas.FulfillmentService_DispatchEntry.properties;
 assert.deepEqual(Object.keys(entry),['operationId','productId','quantityMilli','dispatchedAt','dispatchedBy']);
 assert.deepEqual(entry.quantityMilli,{type:'string'});
 assert.deepEqual(entry.dispatchedAt,{type:'string',format:'date-time'});
 assert.deepEqual(entry.dispatchedBy,{type:'string',format:'uuid'});
});

test('fulfillment queue pagination is typed and preserves scope-bound cursor parameters',()=>{
 const operation=api.paths['/api/v1/fulfillments/page'].get;
 assert.equal(operation.responses['200'].content['application/json'].schema.$ref,'#/components/schemas/FulfillmentService_QueuePage');
 assert.deepEqual(operation.parameters.filter(p=>p.in==='query').map(p=>p.name),['status','q','limit','cursor']);
 assert.equal(operation.parameters.find(p=>p.name==='cursor').required,false);
 const page=api.components.schemas.FulfillmentService_QueuePage.properties;
 assert.equal(page.items.items.$ref,'#/components/schemas/FulfillmentService_QueueEntry');
 assert.equal(page.nextCursor.type,'string');
 assert.equal(api.components.schemas.FulfillmentService_QueueEntry.properties.fulfillment.$ref,
  '#/components/schemas/FulfillmentService_Fulfillment');
 assert(inventory.endpoints.length>0);
 assert(inventory.endpoints.every(endpoint=>!endpoint.source.includes(String.fromCharCode(92))));
});
