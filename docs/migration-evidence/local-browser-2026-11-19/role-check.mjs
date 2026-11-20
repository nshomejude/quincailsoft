const {chromium}=await import(new URL('../../../.local-tools/browser-verification/node_modules/playwright/index.mjs',import.meta.url));
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
const browser=await chromium.launch({channel:'chrome',headless:true});
const results=[];
try {
 for(const role of ['cashier','auditor']){
  const context=await browser.newContext({viewport:{width:1440,height:1000}});
  const page=await context.newPage();
  await page.goto('http://localhost:3100/auth/login',{waitUntil:'domcontentloaded'});
  await page.getByRole('button',{name:'Continue as '+role,exact:true}).click();
  await page.waitForURL(url=>url.port==='3100' && url.pathname==='/',{timeout:60000});
  await page.getByRole('heading',{name:'Your business, in focus.'}).waitFor();
  const products=await context.request.get('http://localhost:3100/api/business/products');
  assert.equal(products.status(),200,role+' catalog read');
  const identity=await context.request.get('http://localhost:3100/api/business/identity/members');
  assert.equal(identity.status(),403,role+' identity access denied');
  const listed=await products.json();
  const {id:productId,...product}=listed[0];
  const attempt=await context.request.put('http://localhost:3100/api/business/products/856daf06-41dd-483c-aecb-1ad48703e4ff',{
   headers:{Origin:'http://localhost:3100'},
   data:{...product,operationId:crypto.randomUUID(),reason:'Synthetic unauthorized request'}
  });
  if(attempt.status()!==403)console.log(JSON.stringify({role,status:attempt.status(),error:await attempt.text()}));
  assert.equal(attempt.status(),403,role+' product update denied');
  const result={role,catalogRead:products.status(),identityRead:identity.status(),productUpdate:attempt.status()};
  results.push(result);
  console.log(JSON.stringify(result));
  await page.screenshot({path:'docs/migration-evidence/local-browser-2026-11-19/'+role+'-dashboard.png',fullPage:true});
  await context.close();
 }
 await fs.writeFile('docs/migration-evidence/local-browser-2026-11-19/role-checks.json',JSON.stringify(results,null,2));
}finally{await browser.close();}
