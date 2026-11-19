import test from 'node:test';import assert from 'node:assert/strict';import {allowedPath} from '../lib/business-paths.ts';
const id='98aa4d01-6873-4bbe-8073-803e8985b6c8';
test('quotation customer decisions expose only the implemented commands and read history',()=>{
 for(const action of ['accept','reject']){
  assert.equal(allowedPath(`quotations/${id}/${action}`,'POST'),true);
  for(const method of ['GET','PUT','DELETE'])assert.equal(allowedPath(`quotations/${id}/${action}`,method),false);
 }
 assert.equal(allowedPath(`quotations/${id}/decisions`,'GET'),true);
 assert.equal(allowedPath(`quotations/${id}/decisions`,'POST'),false);
 assert.equal(allowedPath(`quotations/${id}/clone`,'POST'),false);
});
test('document commands are explicit and cannot be called through read or destructive methods',()=>{
 for(const path of ['documents/captures',`documents/captures/${id}/print-attempts`,`documents/print-attempts/${id}/outcomes`]){
  assert.equal(allowedPath(path,'POST'),true,path);
  for(const method of ['GET','PUT','DELETE'])assert.equal(allowedPath(path,method),false,path);
 }
 for(const path of ['documents/captures/../../admin',`documents/captures/${id}/delete`,`documents/print-attempts/${id}/approve`])
  assert.equal(allowedPath(path,'POST'),false,path);
});
test('BFF permits implemented routes and commands',()=>{for(const path of ['products','customers','warehouses','identity/members','documents/settings','purchases/invoices','expenses','periods','period-requests'])assert.equal(allowedPath(path,'GET'),true,path);for(const path of ['sales',`sales/${id}/post`,`purchases/${id}/receipts`,`mobile-money/intents/${id}/reconcile`,`inventory/jobs/${id}/capture`,`expenses/${id}/pay`,`periods/${id}/close`,`period-requests/${id}/approve`,`purchases/receipts/${id}/returns`])assert.equal(allowedPath(path,'POST'),true,path)});
test('BFF rejects traversal, arbitrary services and destructive methods',()=>{for(const path of ['../admin','https://evil.example/','//evil.example','products/../../identity','operations/database/reset','users','server/server.py'])for(const method of ['GET','POST'])assert.equal(allowedPath(path,method),false,path);assert.equal(allowedPath('products','DELETE'),false);assert.equal(allowedPath('me','POST'),false)});

test('BFF separates updates from commands',()=>{assert.equal(allowedPath('documents/settings','PUT'),true);assert.equal(allowedPath('documents/settings','POST'),false);assert.equal(allowedPath(`products/${id}`,'PUT'),true);assert.equal(allowedPath(`sales/${id}/post`,'PUT'),false)});

test('fulfillment proxy permits only implemented read and command routes',()=>{
 for(const path of ['fulfillments','fulfillments/preparation-summary',
     'fulfillments/'+id,'fulfillments/'+id+'/picklist','fulfillments/'+id+'/dispatches'])
  assert.equal(allowedPath(path,'GET'),true,path);
 for(const path of ['fulfillments','fulfillments/'+id+'/dispatch',
     'fulfillments/'+id+'/dispatch-lines','fulfillments/'+id+'/complete'])
  assert.equal(allowedPath(path,'POST'),true,path);
 for(const path of ['fulfillments/'+id+'/dispatch-lines','fulfillments/'+id+'/complete'])
  assert.equal(allowedPath(path,'GET'),false,path);
 for(const path of ['fulfillments/'+id+'/picklist','fulfillments/'+id+'/dispatches',
     'fulfillments/preparation-summary','fulfillments/'+id+'/cancel',
     'fulfillments/../../admin/dispatch'])
  assert.equal(allowedPath(path,'POST'),false,path);
 assert.equal(allowedPath('fulfillments/'+id,'PUT'),false);
 assert.equal(allowedPath('fulfillments/'+id,'DELETE'),false);
});
