const {chromium}=await import(new URL('../../../.local-tools/browser-verification/node_modules/playwright/index.mjs',import.meta.url));
import fs from 'node:fs/promises';
const directory='docs/migration-evidence/local-browser-2026-11-19';
await fs.mkdir(directory,{recursive:true});
const browser=await chromium.launch({channel:'chrome',headless:true});
try {
 const page=await browser.newPage({viewport:{width:1440,height:1000}});
 await page.goto('http://localhost:3100/auth/login',{waitUntil:'networkidle'});
 await page.screenshot({path:directory+'/login.png',fullPage:true});
 await page.getByRole('button',{name:'Continue as owner',exact:true}).click();
 await page.waitForURL(url=>url.port==='3100' && url.pathname==='/',{timeout:60000});
 await page.getByRole('heading',{name:'Your business, in focus.'}).waitFor({timeout:60000});
 await page.screenshot({path:directory+'/dashboard.png',fullPage:true});
 console.log(JSON.stringify({url:page.url(),dashboard:await page.locator('main').innerText()}));
 await page.getByRole('link',{name:'Products',exact:true}).click();
 await page.waitForURL('**/catalog');
 console.log(JSON.stringify({catalog:await page.locator('main').innerText()}));
 const detail=page.locator('main table tbody a').first();
 await detail.click();
 await page.waitForURL(url=>url.pathname.startsWith('/catalog/') && url.pathname!=='/catalog/new');
 await page.getByRole('heading',{name:'Electrical Cable',exact:true}).waitFor();
 await page.waitForLoadState('networkidle');
 await page.screenshot({path:directory+'/product-detail.png',fullPage:true});
 console.log(JSON.stringify({url:page.url(),detail:await page.locator('main').innerText()}));
} catch(error) {
 console.error(error.message);
 for(const page of browser.contexts().flatMap(c=>c.pages()))
  console.error(JSON.stringify({url:page.url(),text:(await page.locator('body').innerText()).slice(0,3500)}));
 process.exitCode=1;
} finally {await browser.close();}
