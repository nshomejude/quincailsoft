import test from 'node:test';
import assert from 'node:assert/strict';
import {compareRows} from './reconcile-exports.mjs';
const row = {branchId:'branch-a',warehouseId:'warehouse-a',variantId:'variant-a',quantityMilli:'1000',valueXaf:'9007199254740993'};
const stock = () => [structuredClone(row), {...row,variantId:'variant-b',valueXaf:'7'}];
test('exact values above Number precision and row ordering', () => {
  assert.equal(compareRows('stockBalances',stock(),stock().reverse()),2);
});
for (const [name,change] of [
  ['missing row',r=>r.pop()],
  ['duplicate key',r=>r[1]={...r[0]}],
  ['extra row',r=>r.push({...r[0],variantId:'variant-c'})],
  ['branch substitution',r=>r[0].branchId='branch-b'],
  ['warehouse substitution',r=>r[0].warehouseId='warehouse-b'],
  ['variant collapse',r=>r[0].variantId='family-a'],
  ['one XAF drift above safe precision',r=>r[0].valueXaf='9007199254740992'],
  ['offsetting errors preserving grand total',r=>{r[0].valueXaf='9007199254740992';r[1].valueXaf='8';}],
  ['quantity drift',r=>r[0].quantityMilli='999'],
  ['numeric money',r=>r[0].valueXaf=9007199254740992],
  ['decimal money',r=>r[0].valueXaf='1.0'],
  ['exponent money',r=>r[0].valueXaf='1e3'],
  ['missing field',r=>delete r[0].valueXaf],
  ['unexpected field',r=>r[0].status='PASSED'],
]) test('rejects '+name,()=>{const actual=stock();change(actual);assert.throws(()=>compareRows('stockBalances',stock(),actual));});
test('rejects empty exports',()=>assert.throws(()=>compareRows('stockBalances',[],[])));
for (const [kind,record,field] of [
 ['customerOpenItems',{branchId:'b',saleId:'sale',customerId:'customer',balanceXaf:'0'},'customerId'],
 ['supplierOpenItems',{branchId:'b',invoiceId:'invoice',supplierId:'supplier',balanceXaf:'0'},'supplierId'],
 ['trialBalance',{branchId:'b',account:'AP',signedDebitBalanceXaf:'-10'},'account'],
 ['monthlyAnalytics',{branchId:'b',month:'2026-11',operatingProfitXaf:'-10'},'month'],
]) test(kind+' retains zero/negative balances and rejects identity drift',()=>{
 assert.equal(compareRows(kind,[record],[{...record}]),1);
 assert.throws(()=>compareRows(kind,[record],[{...record,[field]:'other'}]));
});

// Optional full-size verifier regression using copied synthetic fixtures, never Java evidence.
import {mkdtemp,cp,writeFile,readFile,rm} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {reconcile,keys} from './reconcile-exports.mjs';
test('full fixture export/replay verifier', {skip:!process.env.QUINCAI_REFERENCE_DIR}, async t => {
 const reference=process.env.QUINCAI_REFERENCE_DIR, temp=await mkdtemp(join(tmpdir(),'quincai-reconciliation-test-'));
 const first=join(temp,'initial'), second=join(temp,'replay');
 try {
  const {mkdir}=await import('node:fs/promises');
  await mkdir(first);await mkdir(second);
  for(const kind of Object.keys(keys))for(const directory of [first,second])await cp(join(reference,kind+'.ndjson'),join(directory,kind+'.ndjson'));
  const initial={schemaVersion:1,phase:'initial',datasetId:'quincaisoft-synthetic-history-v1',mappingVersion:'TEST_ONLY',runId:'fixture-initial',tenantId:'fixture-tenant',companyId:'fixture-company',command:'TEST ONLY copied fixtures; no Java execution',sourceCommit:'e993fd6054a3b73fa034be6bf4f5a0e406582fee'};
  const replay={...initial,phase:'replay',runId:'fixture-replay'};
  const save=async(p,v)=>writeFile(join(p,'export-provenance.json'),JSON.stringify(v));
  await save(first,initial);await save(second,replay);
  await t.test('all 70016 reference rows match supplied fixture copies',async()=>{
   const r=await reconcile(reference,first,second);
   assert.equal(r.results.reduce((n,x)=>n+x.rows,0),70016);
   assert.equal(r.javaExecutionVerified,false);assert.equal(r.releaseCertified,false);
  });
  await t.test('refuses reference directory as application export',async()=>assert.rejects(reconcile(reference,reference,second)));
  for(const [name,patch] of [['different mapping',{mappingVersion:'changed'}],['different tenant',{tenantId:'other'}],['different commit',{sourceCommit:'a'.repeat(40)}],['same run ID',{runId:initial.runId}]]){
   await t.test('rejects '+name,async()=>{await save(second,{...replay,...patch});await assert.rejects(reconcile(reference,first,second));await save(second,replay);});
  }
  await t.test('rejects replay value drift',async()=>{
   const path=join(second,'trialBalance.ndjson'), original=await readFile(path,'utf8');
   const rows=original.trim().split('\n').map(JSON.parse);rows[0].signedDebitBalanceXaf=(BigInt(rows[0].signedDebitBalanceXaf)+1n).toString();
   await writeFile(path,rows.map(r=>JSON.stringify(r)).join('\n')+'\n');
   await assert.rejects(reconcile(reference,first,second));await writeFile(path,original);
  });
  await t.test('rejects missing export file',async()=>{
   await rm(join(second,'stockBalances.ndjson'));await assert.rejects(reconcile(reference,first,second));
  });
 } finally {await rm(temp,{recursive:true,force:true});}
});
