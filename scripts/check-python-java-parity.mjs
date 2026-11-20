import fs from 'node:fs';
import path from 'node:path';
import {root,read,sha,generate} from './inventory-python-parity.mjs';
const failures=[],inventory=JSON.parse(read('docs/migration/python-java-parity.json'));
if(JSON.stringify(inventory)!==JSON.stringify(generate()))failures.push('Inventory differs from current source. Regenerate it and review new/changed behavior before acceptance.');
const knownStatuses=new Set(['OPEN','IMPLEMENTED_UNVERIFIED','EXTERNAL_BLOCKED','ACCEPTED','REPLACED_ACCEPTED']);
const accepted=new Set(['ACCEPTED','REPLACED_ACCEPTED']);
function checkedFile(item,label){if(!item?.path||path.isAbsolute(item.path)||item.path.split(/[\\/]/).includes('..')){failures.push(label+': evidence must use a repository-relative path');return null;}if(!fs.existsSync(path.join(root,item.path))){failures.push(label+': evidence missing '+item.path);return null;}const source=read(item.path);if(item.sha256!==sha(source)){failures.push(label+': evidence digest differs '+item.path);return null;}return source;}
const xmlValue=(tag,name)=>tag.match(new RegExp('\\b'+name+'="([^"]*)"'))?.[1]?.replaceAll('&lt;','<').replaceAll('&gt;','>').replaceAll('&amp;','&');
for(const row of inventory.requirements){if(!knownStatuses.has(row.status))failures.push(row.id+': unsupported status');if(!accepted.has(row.status))continue;
 const e=row.acceptance;if(!e||e.status!==row.status||!e.reviewedBy||!e.reviewNotes?.trim()){failures.push(row.id+': explicit behavioral review missing');continue;}
 if(!Array.isArray(e.sourceEvidence)||!e.sourceEvidence.length)failures.push(row.id+': source evidence missing');
 for(const item of e.sourceEvidence??[]){const source=checkedFile(item,row.id);if(source&&(!item.symbol||!source.includes(item.symbol)))failures.push(row.id+': actual source symbol missing');if(!item.path?.startsWith('backend/')&&!item.path?.startsWith('apps/')&&!item.path?.startsWith('desktop-integration/'))failures.push(row.id+': legacy source is not migrated implementation evidence');}
 if(!Array.isArray(e.testEvidence)||!e.testEvidence.length)failures.push(row.id+': executed assertion evidence missing');
 for(const item of e.testEvidence??[]){const source=checkedFile(item,row.id);if(source&&(!item.testName||!source.includes(item.testName)))failures.push(row.id+': mapped test method missing');if(!item.assertionSnippets?.length||source&&item.assertionSnippets.some(s=>!source.includes(s)||!/(?:assert|expect|throw)/.test(s)))failures.push(row.id+': concrete test assertion mapping missing');
  const report=checkedFile(item.executionReport,row.id);if(report){const cases=[...report.matchAll(/<testcase\b[^>]*(?:\/>|>[\s\S]*?<\/testcase>)/g)].map(m=>m[0]);const test=cases.find(t=>xmlValue(t,'name')===item.testName&&xmlValue(t,'classname')===item.className);if(!test||/<(?:failure|error|skipped)\b/.test(test))failures.push(row.id+': named test has no executed passing JUnit case');}
 }
 if(row.status==='REPLACED_ACCEPTED'&&(!e.replacement?.section||!e.replacement?.reason||!read('ARCHITECTURE.md').includes(e.replacement.section)))failures.push(row.id+': normative replacement requires an exact architecture section heading and reviewed behavior explanation');
}
const unresolved=inventory.requirements.filter(r=>!accepted.has(r.status));
if(unresolved.length){const counts=Object.groupBy(unresolved,r=>r.owner);console.error(`FULL PARITY BLOCKED: ${unresolved.length}/${inventory.requirements.length} required behaviors unresolved.`);for(const [owner,rows]of Object.entries(counts))console.error(` - ${owner}: ${rows.length}`);console.error('Exact IDs, legacy guards/assertions and candidate mappings: docs/migration/python-java-parity.json');}
if(failures.length)console.error('INVALID PARITY EVIDENCE\n'+failures.map(f=>' - '+f).join('\n'));
if(unresolved.length||failures.length)process.exitCode=1;else console.log('PASS explicit Python-to-Java behavioral parity evidence. Canonical full-screen, report/template, provider, native-device and release gates remain separate requirements.');
