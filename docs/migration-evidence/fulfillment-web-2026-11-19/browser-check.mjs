const {chromium}=await import(new URL('../../../.local-tools/browser-verification/node_modules/playwright/index.mjs',import.meta.url));
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
const directory='docs/migration-evidence/fulfillment-web-2026-11-19';
await fs.mkdir(directory,{recursive:true});
const browser=await chromium.launch({channel:'chrome',headless:true});
const results=[];
try{
 for(const role of ['owner','auditor']){
  const context=await browser.newContext({viewport:{width:1440,height:1000}});
  const page=await context.newPage();
  await page.goto('http://localhost:3100/auth/login');
  await page.getByRole('button',{name:'Continue as '+role,exact:true}).click();
  await page.waitForURL(url=>url.port==='3100'&&url.pathname==='/',{timeout:60000});
  await page.getByRole('link',{name:'Pickup & delivery',exact:true}).click();
  await page.getByRole('heading',{name:'Pickup & Delivery Queue',exact:true}).waitFor({timeout:60000});

  assert.equal(await page.getByRole('link',{name:'Create dispatch from a sale',exact:true}).count(),role==='owner'?1:0);
  const tabs=[];
  for(const label of ['Partial','Dispatched','Completed','All','Awaiting pickup']){
   const tab=page.getByRole('link',{name:label,exact:true});
   const href=await tab.getAttribute('href');
   await tab.click();
   await page.waitForURL(new URL(href,'http://localhost:3100').toString());
   await page.waitForFunction(label=>document.querySelector('[aria-label="Fulfillment status"] [aria-current="page"]')?.textContent?.trim()===label,label);

   await page.getByRole('heading',{name:'Pickup & Delivery Queue',exact:true}).waitFor();
   assert.equal(await page.getByRole('heading',{name:'The request could not be completed'}).count(),0);
   tabs.push(label);
  }
  await page.screenshot({path:directory+'/'+role+'-queue.png',fullPage:true});
  await page.getByLabel('Customer, recipient or reference').fill('NO-MATCH-SYNTHETIC-FILTER');
  await Promise.all([page.waitForURL(url=>url.searchParams.get('q')==='NO-MATCH-SYNTHETIC-FILTER'),
   page.getByRole('button',{name:'Filter',exact:true}).click()]);

  assert.match(page.url(),/NO-MATCH-SYNTHETIC-FILTER/);
  await page.getByText('No pickup or delivery tasks match these filters.',{exact:true}).waitFor();
  const salesResponse=await context.request.get('http://localhost:3100/api/business/sales');
  const draft=(await salesResponse.json()).find(sale=>sale.status==='DRAFT');
  assert(draft,'Existing synthetic preview sale draft is required for the rejection case');
  await page.goto('http://localhost:3100/fulfillments/new?saleId='+draft.id);
  if(role==='owner')
   await page.getByText('Choose a posted sale before preparing pickup or delivery.',{exact:true}).waitFor();
  else
   await page.getByRole('heading',{name:'This view needs a different permission',exact:true}).waitFor();
  assert.equal(await page.getByRole('button',{name:'Prepare pickup / delivery',exact:true}).count(),0);
  results.push({role,tabs,filter:'PASS',createAction:role==='owner'?'VISIBLE':'HIDDEN',
   draftPreparation:role==='owner'?'UNPOSTED_SALE_REJECTED':'WRITE_PERMISSION_REQUIRED'});
  await context.close();
 }
 await fs.writeFile(directory+'/browser-checks.json',JSON.stringify(results,null,2));
 console.log(JSON.stringify(results));
}catch(error){
 console.error(error.message);
 for(const page of browser.contexts().flatMap(context=>context.pages()))
  console.error(JSON.stringify({url:page.url(),text:(await page.locator('body').innerText()).slice(0,2500)}));
 process.exitCode=1;
}finally{await browser.close();}
