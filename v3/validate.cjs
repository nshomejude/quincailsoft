const fs=require('fs'),vm=require('vm'),assert=require('assert'),crypto=require('crypto').webcrypto,path=require('path');
const fake=()=>({innerHTML:'',value:'',textContent:'',hidden:false,addEventListener(){},classList:{remove(){}},setAttribute(){},focus(){}});
const el={};
const ctx={window:{addEventListener(){}},document:{querySelector(s){return el[s]??=fake()},querySelectorAll(){return []},addEventListener(){},body:{classList:{remove(){}}}},location:{hash:''},localStorage:{getItem(){return null},setItem(){}},crypto,structuredClone,Intl,URLSearchParams,TextEncoder,setTimeout,Blob,URL,console};
vm.createContext(ctx);
for(const f of ['contracts.js','model.js','ui.js','reports.js','documents.js','actions.js','action-overrides.js'])vm.runInContext(fs.readFileSync(path.join(__dirname,f),'utf8'),ctx,{filename:f});
const results=vm.runInContext(`(()=>{
const checks=[],coverage=[];
for(const s of C.screens){
 current={s,params:{},entity:s.entity,mode:s.kind};
 let html;
 if(s.kind==='dashboard')html=dashboard();
 else if(s.kind==='reports')html=reportView();
 else if(s.kind==='documents')html=documentCards(C.documents);
 else if(s.kind==='document')html=documentView();
 else if(s.kind==='pos'||s.kind==='commerce')html=posView();
 else if(s.kind==='form'||s.kind==='identity')html=formView(s,s.entity,null);
 else if(s.kind==='detail')html=detailView(s,s.entity,rows(s.entity)[0]?.id);
 else html=listView(s,s.entity);
 if(!html||html.includes('undefined'))throw Error('Screen render invalid '+s.id);
 const schema=C.schemas[s.entity];
 if(schema){const rec=rows(s.entity)[0];current.record=rec?.id;const detail=detailView(s,s.entity,rec?.id);if(!detail.includes('record-heading'))throw Error('Missing detail '+s.id);const form=formView(s,s.entity,null);for(const f of schema.fields)if(!form.includes('name="'+f.key+'"'))throw Error('Missing field '+s.id+' '+f.key)}
 for(const [index,label] of s.actions.entries()){const d=actionDescriptor(s,label);if(d.kind==='navigate'&&!screenById(d.target))throw Error('Broken target');coverage.push({screen:s.id,index,label,...d})}
 checks.push({screen:s.id,kind:s.kind,fields:s.form_fields.length,render:'PASS',detail:'PASS'});
}
const reportChecks=C.reports.map(d=>{current={params:{report:d.id}};const data=reportData(d);if(!data.headers||!data.items||!data.formula)throw Error('Invalid report '+d.id);return {id:d.id,name:d.name,rows:data.items.length,formula:data.formula,result:'PASS'}});
const docChecks=C.documents.map(d=>{current={params:{doc:d.id,record:rows(d.entity)[0]?.id}};const html=documentView();if(!html.includes('Source details'))throw Error('Document missing fields '+d.id);return {id:d.id,title:d.title,layout:d.layout,result:'PASS'}});
return {checks,coverage,reportChecks,docChecks};
})()`,ctx);
fs.writeFileSync(path.join(__dirname,'coverage.json'),JSON.stringify(results,null,2));
const kinds={};results.coverage.forEach(a=>kinds[a.kind]=(kinds[a.kind]||0)+1);
const summary={screens:results.checks.length,field_placements:results.checks.reduce((n,s)=>n+s.fields,0),actions:results.coverage.length,action_kinds:kinds,reports:results.reportChecks.length,documents:results.docChecks.length,render:'PASS',browser_tests:'PENDING',production_backend:'NOT_IMPLEMENTED'};
fs.writeFileSync(path.join(__dirname,'validation.json'),JSON.stringify(summary,null,2));
vm.runInContext(`(()=>{
if(mul('0.1','0.2')!=='0.02')throw Error('Decimal arithmetic');
if(sum(['0.1','0.2'])!=='0.3')throw Error('Decimal addition');
let x=saveRecord('product',{sku:'TEST-SKU',name:'Test',base_unit:'piece',selling_price:'100'},null,0);
if(x.errors)throw Error('Valid product rejected');
let dup=saveRecord('product',{sku:'TEST-SKU',name:'Test',base_unit:'piece',selling_price:'100'},null,0);if(!dup.errors?.sku)throw Error('Duplicate SKU allowed');
let neg=saveRecord('product',{sku:'NEG',name:'Test',base_unit:'piece',selling_price:'-1'},null,0);if(!neg.errors?.selling_price)throw Error('Negative price allowed');
let rec=get('sale','sale-1');let threw=false;try{saveRecord('sale',{customer:'customer-1',business_date:'2026-11-17'},rec.id,rec.version)}catch{threw=true}if(!threw)throw Error('Posted edit allowed');
let draft=get('sale','sale-2');transition('sale',draft.id,'submit','Test',draft.version);let stale=false;try{transition('sale',draft.id,'approve','Test',1)}catch{stale=true}if(!stale)throw Error('Stale operation allowed');
})()`,ctx);
console.log(JSON.stringify(summary,null,2));
