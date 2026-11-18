import test from 'node:test';
import assert from 'node:assert/strict';
import {sourceContext,declarationIdentity,resolveDeclaration,plainEnums} from './java-source-types.mjs';
test('same-named enum fields bind to their lexical declaration owner',()=>{
 const a=sourceContext('package sample; interface Quotas {enum Resource {SEATS, WAREHOUSES}}','Quotas');
 const b=sourceContext('package sample; interface Files {enum Resource {PDF, CSV}}','Files');
 const contexts=new Map([['Quotas',a],['Files',b],['Other',sourceContext('package sample;','Other')]]);
 const declarations=new Map([...plainEnums('enum Resource {SEATS, WAREHOUSES}',a,'quotas.java'),...plainEnums('enum Resource {PDF, CSV}',b,'files.java')]);
 assert.deepEqual(declarations.get(resolveDeclaration('Resource','Quotas',declarations,contexts)).values,['SEATS','WAREHOUSES']);
 assert.deepEqual(declarations.get(resolveDeclaration('Resource','Files',declarations,contexts)).values,['PDF','CSV']);
 assert.equal(resolveDeclaration('Resource','Other',declarations,contexts),null);
 assert.equal(resolveDeclaration('Files.Resource','Quotas',declarations,contexts),'Files.Resource');
});
test('top-level records resolve through actual imports without inventing Object structure',()=>{
 const tenant=sourceContext('package sample.shared; public record TenantScope() {}','TenantScope');
 const identity=declarationIdentity(tenant,'TenantScope'),declarations=new Map([[identity.key,identity]]);
 const contexts=new Map([['Session',sourceContext('package sample.api; import sample.shared.*;','Session')],['Other',sourceContext('package elsewhere;','Other')]]);
 assert.equal(resolveDeclaration('TenantScope','Session',declarations,contexts),'TenantScope');
 assert.equal(resolveDeclaration('sample.shared.TenantScope','Other',declarations,contexts),'TenantScope');
 assert.equal(resolveDeclaration('TenantScope','Other',declarations,contexts),null);
 assert.equal(resolveDeclaration('Object','Session',declarations,contexts),null);
});
test('custom or annotated enum serialization remains explicitly unsupported',()=>{
 const c=sourceContext('package sample;','Owner');
 assert.deepEqual(plainEnums('String example="enum Fake { A, B }";',c,'test.java'),[]);
 for(const source of ['enum Mode { A("wire"); Mode(String x){} }','enum Mode { @JsonProperty("wire") A }','@JsonFormat(shape=JsonFormat.Shape.NUMBER) enum Mode { A, B }'])assert.deepEqual(plainEnums(source,c,'test.java'),[]);
 assert.deepEqual(plainEnums('enum Mode { A, /* comment */ B, }',c,'test.java')[0][1].values,['A','B']);
});
