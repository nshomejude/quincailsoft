const {chromium}=await import(new URL('../../../.local-tools/browser-verification/node_modules/playwright/index.mjs',import.meta.url));
import assert from 'node:assert/strict';import fs from 'node:fs/promises';
const browser=await chromium.launch({channel:'chrome',headless:true});
const results=[];const root='http://localhost:3100';
try{
 for(const role of ['owner','cashier','auditor']){
  const context=await browser.newContext();const page=await context.newPage();
  await page.goto(root+'/auth/login');
  await page.getByRole('button',{name:'Continue as '+role,exact:true}).click({noWaitAfter:true});
  await page.waitForURL(url=>url.port==='3100'&&url.pathname==='/',{timeout:120000});
  const meResponse=await context.request.get(root+'/api/business/me');assert.equal(meResponse.status(),200);
  const me=await meResponse.json();const permitted=permission=>me.authorities.some(a=>a===permission||a==='SCOPE_'+permission);
  if(role==='owner')for(const permission of ['documents:read','documents:write','documents:print','sales:read'])
   assert(permitted(permission),'Owner preview permission '+permission);
  const missing=crypto.randomUUID();const outcomes=[];
  for(const [path,payload,capability] of [
   ['documents/captures',{templateId:'PICK_TICKET',sourceId:missing},'documents:write'],
   ['documents/captures/'+missing+'/print-attempts',{paper:'A4',reason:'Synthetic permission acceptance'},'documents:print'],
   ['documents/print-attempts/'+missing+'/outcomes',{status:'FAILED',reason:'Synthetic permission acceptance'},'documents:print']]){
   const data={...payload,operationId:crypto.randomUUID()};
   const denied=await context.request.post(root+'/api/business/'+path,{headers:{Origin:'http://invalid.example'},data});
   assert.equal(denied.status(),403,'Origin check '+path);
   const response=await context.request.post(root+'/api/business/'+path,{headers:{Origin:root},data});
   const expected=permitted(capability)?404:403;
   assert.equal(response.status(),expected,role+' '+path+': '+await response.text());
   outcomes.push({path,status:response.status(),originDenied:denied.status()});
  }
  results.push({role,outcomes});await context.close();
 }
 await fs.writeFile('docs/migration-evidence/fulfillment-web-2026-11-19/document-bff-checks.json',JSON.stringify(results,null,2)+'\n');
 console.log(JSON.stringify(results));
}finally{await browser.close();}
