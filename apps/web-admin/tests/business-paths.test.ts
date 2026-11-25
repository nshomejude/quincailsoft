import test from 'node:test';import assert from 'node:assert/strict';import {allowedPath} from '../lib/business-paths.ts';
const id='98aa4d01-6873-4bbe-8073-803e8985b6c8';
test('BFF permits implemented routes and commands',()=>{for(const path of ['products','customers','warehouses','identity/members','documents/settings','purchases/invoices','expenses','periods','period-requests'])assert.equal(allowedPath(path,'GET'),true,path);for(const path of ['sales',`sales/${id}/post`,`purchases/${id}/receipts`,`mobile-money/intents/${id}/reconcile`,`inventory/jobs/${id}/capture`,`expenses/${id}/pay`,`periods/${id}/close`,`period-requests/${id}/approve`,`purchases/receipts/${id}/returns`])assert.equal(allowedPath(path,'POST'),true,path)});
test('BFF rejects traversal, arbitrary services and destructive methods',()=>{for(const path of ['../admin','https://evil.example/','//evil.example','products/../../identity','operations/database/reset','users','server/server.py'])for(const method of ['GET','POST'])assert.equal(allowedPath(path,method),false,path);assert.equal(allowedPath('products','DELETE'),false);assert.equal(allowedPath('me','POST'),false)});

test('BFF permits online store publishing and nothing beside it',()=>{
  for(const path of ['commerce/store','commerce/listings']){
    assert.equal(allowedPath(path,'GET'),true,path);
    assert.equal(allowedPath(path,'POST'),true,path);
    assert.equal(allowedPath(path,'PUT'),false,path);
    assert.equal(allowedPath(path,'DELETE'),false,path);
  }
  for(const path of ['commerce','commerce/','commerce/store/extra',`commerce/listings/${id}`,'commerce/../me','commerce/orders','api/public/stores/demo'])
    for(const method of ['GET','POST'])assert.equal(allowedPath(path,method),false,path);
});

test('BFF separates updates from commands',()=>{assert.equal(allowedPath('documents/settings','PUT'),true);assert.equal(allowedPath('documents/settings','POST'),false);assert.equal(allowedPath(`products/${id}`,'PUT'),true);assert.equal(allowedPath(`sales/${id}/post`,'PUT'),false)});
