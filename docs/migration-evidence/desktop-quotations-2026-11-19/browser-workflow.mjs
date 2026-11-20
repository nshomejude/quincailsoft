const {chromium}=await import(new URL('../../../.local-tools/browser-verification/node_modules/playwright/index.mjs',import.meta.url));
import assert from 'node:assert/strict';import fs from 'node:fs/promises';
const root='http://localhost:3100';const directory='docs/migration-evidence/desktop-quotations-2026-11-19';
const progress={status:'RUNNING',synthetic:true,steps:[]};
const save=()=>fs.writeFile(directory+'/browser-workflow.json',JSON.stringify(progress,null,2)+'\n');
const browser=await chromium.launch({channel:'chrome',headless:true});
try{
 const context=await browser.newContext({viewport:{width:1440,height:1000}});const page=await context.newPage();
 await page.goto(root+'/auth/login');await page.getByRole('button',{name:'Continue as owner',exact:true}).click({noWaitAfter:true});
 await page.waitForURL(url=>url.port==='3100'&&url.pathname==='/',{timeout:120000});
 const get=async path=>{const r=await context.request.get(root+'/api/business/'+path);assert.equal(r.status(),200,path);return r.json();};
 const settings=await get('documents/settings');
 if(!settings.issuerName?.trim()){
  const response=await context.request.put(root+'/api/business/documents/settings',{headers:{Origin:root},data:{...settings,
   issuerName:'QUINCAISOFT SYNTHETIC DEMO',issuerAddress:'Local development only',registrationNumber:'DEMO-NOT-FISCAL',fiscalSeries:'DEMO',locale:'en',operationId:crypto.randomUUID()}});
  assert.equal(response.status(),200,'Synthetic preview issuer setup');progress.steps.push('Configured previously blank local issuer as SYNTHETIC DEMO');
 }
 const [customers,warehouses,products]=await Promise.all(['customers','warehouses','products'].map(get));
 const customer=customers.find(row=>row.active!==false);const warehouse=warehouses.find(row=>row.active!==false);
 const product=products.find(row=>row.active&&row.taxBasisPoints===0);assert(customer&&warehouse&&product,'Scoped synthetic preview reference records required');
 await page.goto(root+'/quotations/new');await page.getByRole('heading',{name:'New quotation',exact:true}).waitFor({timeout:60000});
 await page.locator('select[name="customerId"]').selectOption(customer.id);await page.locator('select[name="warehouseId"]').selectOption(warehouse.id);
 await page.locator('select[name="lines.0.productId"]').selectOption(product.id);await page.getByLabel('Quantity',{exact:true}).fill('2');
 const until=new Date();until.setUTCDate(until.getUTCDate()+7);await page.getByLabel('Valid until',{exact:true}).fill(until.toISOString().slice(0,10));
 await page.getByRole('button',{name:'Save quotation draft',exact:true}).click({noWaitAfter:true});
 await page.waitForURL(url=>/^\/quotations\/[0-9a-f-]{36}$/.test(url.pathname),{timeout:120000});
 progress.quotationId=new URL(page.url()).pathname.split('/').at(-1);progress.steps.push('Created actual Java quotation through the existing web form');await save();
 const action=async(name,path)=>{
  const responsePromise=page.waitForResponse(response=>response.url()===root+'/api/business/'+path&&response.request().method()==='POST',{timeout:120000});
  await page.getByRole('button',{name,exact:true}).click({noWaitAfter:true});const response=await responsePromise;
  assert.equal(response.status(),200,path+': '+await response.text());return response.json();
 };
 await page.getByLabel('Issue reason',{exact:true}).fill('SYNTHETIC BROWSER ACCEPTANCE: issue demonstration quotation');
 const path='quotations/'+progress.quotationId;
 await action('Issue quotation',path+'/issue');await page.getByRole('heading',{name:'Customer response',exact:true}).waitFor({timeout:120000});
 assert.equal(await page.getByRole('button',{name:'Create sale draft',exact:true}).count(),0,'Issued quotation cannot stage a sale before acceptance');
 progress.steps.push('Issued quotation; conversion action absent before customer acceptance');await save();
 await page.getByLabel('Acceptance evidence reference',{exact:true}).fill('SYNTHETIC-BROWSER-CUSTOMER-ACCEPTANCE');
 await page.getByLabel('Acceptance reason',{exact:true}).fill('SYNTHETIC demonstration customer response; not a real customer commitment');
 await action('Record customer acceptance',path+'/accept');await page.getByRole('button',{name:'Create sale draft',exact:true}).waitFor({timeout:120000});
 await page.screenshot({path:directory+'/accepted-quotation.png',fullPage:true});
 progress.steps.push('Recorded explicit customer acceptance through Java with evidence');await save();
 await page.getByLabel('Sale staging reason',{exact:true}).fill('SYNTHETIC BROWSER ACCEPTANCE: stage draft only');
 const converted=await action('Create sale draft',path+'/convert');progress.saleId=converted.convertedSaleId;await save();
 await page.getByRole('link',{name:'Open converted sale',exact:true}).waitFor({timeout:120000});
 const decisions=await get(path+'/decisions');assert.equal(decisions.length,1);assert.equal(decisions[0].evidenceReference,'SYNTHETIC-BROWSER-CUSTOMER-ACCEPTANCE');
 const sale=await get('sales/'+progress.saleId);assert.equal(sale.status,'DRAFT');assert.equal(sale.totalXaf,converted.totalXaf);
 await page.screenshot({path:directory+'/converted-quotation.png',fullPage:true});
 progress.steps.push('Converted accepted quote to actual Java DRAFT sale; exact total and one decision verified');progress.status='PASS';await save();
 console.log(JSON.stringify(progress));await context.close();
}catch(error){progress.status='FAIL';progress.error=error.message;await save();throw error;}finally{await browser.close();}
