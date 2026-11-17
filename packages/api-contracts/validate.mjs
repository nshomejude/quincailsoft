import fs from 'node:fs';
import assert from 'node:assert/strict';
const api=JSON.parse(fs.readFileSync(new URL('./openapi.json',import.meta.url),'utf8'));
assert.equal(api.openapi,'3.1.0');
function visit(value){
 if(!value||typeof value!=='object')return;
 if(value.$ref){assert(value.$ref.startsWith('#/'));let target=api;for(const key of value.$ref.slice(2).split('/'))target=target?.[key];assert(target,`Unresolved ${value.$ref}`);}
 for(const child of Object.values(value))visit(child);
}
visit(api);
for(const [path,item] of Object.entries(api.paths))for(const [method,op] of Object.entries(item)){
 if(!['get','post','put','delete','patch'].includes(method))continue;
 assert(op.responses,`${method} ${path} missing responses`);
 if(['post','put','patch'].includes(method))for(const name of ['Idempotency-Key','X-Request-ID'])assert(op.parameters.some(p=>p.name===name&&p.required),`${path} lacks ${name}`);
}
for(const name of ['Product','Sale','SaleLine'])for(const [field,schema] of Object.entries(api.components.schemas[name].properties))if(field.endsWith('Xaf')||field==='quantityMilli')assert.equal(schema.type,'string',`${field} must use exact wire strings`);
console.log(`PASS structural contract: ${Object.keys(api.paths).length} paths, local references, mutation headers and exact amount/quantity types. Full OpenAPI lint and consumer contract execution remain separate gates.`);
