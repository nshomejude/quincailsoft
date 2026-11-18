import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {root,files} from './inventory-python-parity.mjs';
function fixture(){const tmp=fs.mkdtempSync(path.join(os.tmpdir(),'quincai-gate-'));const selected=new Set(['AGENTS.md','ARCHITECTURE.md','package.json','v3/contracts.json','docs/v5-gap-register.json','docs/migration/python-java-parity.json']);for(const dir of ['scripts','server','tests','backend','apps','infrastructure'])for(const file of files(dir))if(!file.includes('/.expo/'))selected.add(file);for(const file of files('desktop-integration').filter(f=>f.endsWith('.csproj')))selected.add(file);for(const file of selected){const dest=path.join(tmp,file);fs.mkdirSync(path.dirname(dest),{recursive:true});fs.copyFileSync(path.join(root,file),dest);}return tmp;}
function run(tmp,name,...args){const r=spawnSync(process.execPath,[path.join(tmp,'scripts',name),...args],{cwd:tmp,encoding:'utf8'});return {code:r.status,text:r.stdout+r.stderr};}
test('rule gate rejects protected-rule edits and production Python bridges without touching the workspace',()=>{const tmp=fixture();try{assert.equal(run(tmp,'check-project-rules.mjs').code,0);fs.appendFileSync(path.join(tmp,'AGENTS.md'),'\nUnauthorized fixture edit\n');let r=run(tmp,'check-project-rules.mjs');assert.equal(r.code,1);assert.match(r.text,/Protected source changed/);fs.copyFileSync(path.join(root,'AGENTS.md'),path.join(tmp,'AGENTS.md'));const target=path.join(tmp,'backend/business-modules/sales/src/main/java/BridgeFixture.java');fs.writeFileSync(target,'class BridgeFixture { String runtime="server/server.py"; }');r=run(tmp,'check-project-rules.mjs');assert.equal(r.code,1);assert.match(r.text,/Forbidden production Python bridge/);}finally{fs.rmSync(tmp,{recursive:true,force:true});}});
test('parity gate stays red for unresolved behavior and refuses fabricated source-only acceptance',()=>{const tmp=fixture();try{assert.equal(run(tmp,'inventory-python-parity.mjs').code,0);let r=run(tmp,'check-python-java-parity.mjs');assert.equal(r.code,1);assert.match(r.text,/FULL PARITY BLOCKED/);const inventory=JSON.parse(fs.readFileSync(path.join(tmp,'docs/migration/python-java-parity.json')));const id=inventory.requirements[0].id;fs.writeFileSync(path.join(tmp,'scripts/parity-acceptance.json'),JSON.stringify({[id]:{status:'ACCEPTED',reviewedBy:'synthetic-fixture',reviewNotes:'File exists only',sourceEvidence:[],testEvidence:[]}}));assert.equal(run(tmp,'inventory-python-parity.mjs').code,0);r=run(tmp,'check-python-java-parity.mjs');assert.equal(r.code,1);assert.match(r.text,/source evidence missing/);assert.match(r.text,/executed assertion evidence missing/);fs.appendFileSync(path.join(tmp,'server/policy.py'),'\n# changed fixture\n');r=run(tmp,'inventory-python-parity.mjs','--check');assert.equal(r.code,1);assert.match(r.text,/inventory is stale/);}finally{fs.rmSync(tmp,{recursive:true,force:true});}});

test('rule gate tolerates CRLF checkouts and still rejects a Python executable in production scripts', () => {
  const tmp = fixture();
  try {
    for (const name of ['AGENTS.md', 'ARCHITECTURE.md']) {
      const target = path.join(tmp, name);
      const source = fs.readFileSync(target, 'utf8');
      fs.writeFileSync(target, source.replace(/\r?\n/g, '\r\n'));
    }
    assert.equal(run(tmp, 'check-project-rules.mjs').code, 0);
    const manifest = path.join(tmp, 'package.json');
    const pkg = JSON.parse(fs.readFileSync(manifest, 'utf8'));
    pkg.scripts['start:fixture'] = 'python3.13.exe -m bridge';
    fs.writeFileSync(manifest, JSON.stringify(pkg));
    const result = run(tmp, 'check-project-rules.mjs');
    assert.equal(result.code, 1);
    assert.match(result.text, /Production package script invokes Python: start:fixture/);
  } finally {
    fs.rmSync(tmp, {recursive: true, force: true});
  }
});
