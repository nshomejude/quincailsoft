import fs from 'node:fs';import os from 'node:os';import path from 'node:path';import {spawnSync} from 'node:child_process';import {createHash} from 'node:crypto';import assert from 'node:assert/strict';import {fileURLToPath} from 'node:url';
const source=path.resolve(process.argv[2]??'datasets/business-history/generated');
const validator=fileURLToPath(new URL('./validate.mjs',import.meta.url));
const results=[];
for(let [file,change,expected] of [
 ['journals.ndjson',row=>{row.lines[0].debitXaf=(BigInt(row.lines[0].debitXaf)+1n).toString();},'journal balanced'],
 ['stockBalances.ndjson',row=>{row.quantityMilli=(BigInt(row.quantityMilli)+1000n).toString();},'stock replay']
]){let dir=fs.mkdtempSync(path.join(os.tmpdir(),'quincai-fixture-negative-'));try{let manifest=JSON.parse(fs.readFileSync(path.join(source,'manifest.json')));for(let f of manifest.files)fs.linkSync(path.join(source,f.file),path.join(dir,f.file));let rows=fs.readFileSync(path.join(dir,file),'utf8').trim().split('\n'),first=JSON.parse(rows[0]);change(first);rows[0]=JSON.stringify(first);let bytes=rows.join('\n')+'\n';fs.unlinkSync(path.join(dir,file));fs.writeFileSync(path.join(dir,file),bytes);let entry=manifest.files.find(f=>f.file===file);entry.sha256=createHash('sha256').update(bytes).digest('hex');entry.bytes=Buffer.byteLength(bytes);fs.writeFileSync(path.join(dir,'manifest.json'),JSON.stringify(manifest));let run=spawnSync(process.execPath,[validator,dir],{encoding:'utf8',maxBuffer:1024*1024});assert.notEqual(run.status,0);assert.ok(run.stderr.includes(expected),run.stderr);results.push({file,expected,rejected:true,checksumUpdatedToEnsureBehavioralDetection:true});}finally{fs.rmSync(dir,{recursive:true,force:true});}}
console.log(JSON.stringify({negativeCases:results},null,2));
