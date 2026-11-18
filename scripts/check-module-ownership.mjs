import fs from 'node:fs';import assert from 'node:assert/strict';
const doc=JSON.parse(fs.readFileSync('docs/ownership/module-ownership.json','utf8'));
const arch=fs.readFileSync('ARCHITECTURE.md','utf8').split('## 4. Complete module inventory and boundaries')[1].split('### 4.1')[0];
const canonical=[...arch.matchAll(/^\| (M\d{2}) \| ([^|]+) \|/gm)].map(m=>[m[1],m[2].trim()]);
assert.equal(canonical.length,40);assert.equal(doc.modules.length,40);
const ids=new Set(doc.modules.map(m=>m.id));assert.equal(ids.size,40);
for(let [id,name] of canonical){let m=doc.modules.find(m=>m.id===id);assert.ok(m,`Missing ${id}`);assert.equal(m.name,name);assert.ok(doc.owners[m.owner]);assert.ok(doc.owners[m.reviewer]);assert.notEqual(m.owner,m.reviewer);assert.equal(m.integrationOwner,'DESKTOP');assert.ok(m.acceptanceRequirements.length>=8);assert.ok(m.sourceAreas.length);for(let dep of m.dependsOn)assert.ok(ids.has(dep),`${id}: invalid ${dep}`);if(m.releaseStatus==='ACCEPTED')assert.ok(m.acceptedEvidence.length,`${id} lacks release evidence`);}
for(let w of doc.workstreams){assert.ok(doc.owners[w.owner]);assert.notEqual(w.owner,w.reviewer);for(let dep of w.dependsOn)assert.ok(ids.has(dep));}
assert.equal(new Set(doc.workstreams.map(x=>x.id)).size,doc.workstreams.length);
for(let a of doc.desktopAssignments){assert.equal(a.owner,'DESKTOP');assert.ok(a.completionCriteria);if(a.verificationStatus==='VERIFIED')assert.ok(a.acknowledged&&a.evidence.length,'Assignment cannot be verified without acknowledgment/evidence');}
for(let id of ['M29','M30'])assert.equal(doc.modules.find(m=>m.id===id).deliveryStatus,'PAUSED_BY_USER');
assert.equal(doc.stack.productionPython,'PROHIBITED');
console.log(JSON.stringify({status:'PASS_REGISTER_STRUCTURE_NOT_MODULE_ACCEPTANCE',canonicalModules:40,cloudModules:doc.modules.filter(m=>m.owner==='CLOUD').length,desktopModules:doc.modules.filter(m=>m.owner==='DESKTOP').length,workstreams:doc.workstreams.length,desktopAssignments:doc.desktopAssignments.length,unowned:0,releaseCertified:false},null,2));
