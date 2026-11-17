/* Browser-local demonstration store. No API or production credential access. */
const C=window.APP_CONTRACTS;
const SCALE=1000000n;
function dec(s){s=String(s??'0').trim();if(!/^-?\d+(\.\d{1,6})?$/.test(s))throw Error('Use a number with no more than six decimal places.');const neg=s.startsWith('-');s=s.replace('-','');const [a,b='']=s.split('.');return (neg?-1n:1n)*(BigInt(a)*SCALE+BigInt(b.padEnd(6,'0')))}
function decimal(n){const neg=n<0n;if(neg)n=-n;return (neg?'-':'')+(n/SCALE)+((n%SCALE)?'.'+String(n%SCALE).padStart(6,'0').replace(/0+$/,''):'')}
function sum(xs){return decimal(xs.reduce((n,x)=>n+dec(x||'0'),0n))}
function mul(a,b){return decimal(dec(a)*dec(b)/SCALE)}
function sub(a,b){return decimal(dec(a)-dec(b))}
function uid(prefix='LOCAL'){return prefix+'-'+crypto.randomUUID().slice(0,8).toUpperCase()}
function emptyStore(){return {version:3,entities:Object.fromEntries(Object.keys(C.schemas).map(k=>[k,[]])),audit:[],snapshots:[],basket:[],preferences:{branch:'branch-1',locale:'en'}}}
function seed(){
 let db=emptyStore();
 const add=(e,v)=>{const obj={id:e+'-'+(db.entities[e].length+1),reference:e.toUpperCase().slice(0,4)+'-00'+(db.entities[e].length+1),status:'Draft',version:1,company_id:'company-1',branch_id:'branch-1',business_date:'2026-11-17',created_at:'2026-11-17T08:00:00Z',lines:[],...v};db.entities[e].push(obj);return obj};
 add('company',{legal_name:'QuincaiSoft Demo Merchant',trading_name:'Demo Store',address:'Douala, Cameroon',currency:'XAF',timezone:'Africa/Douala',status:'Active'});
 add('branch',{name:'Main branch',company:'company-1',address:'Douala',status:'Active'});add('branch',{name:'Bonabéri',company:'company-1',status:'Active'});
 add('warehouse',{name:'Main warehouse',branch:'branch-1',company:'company-1',status:'Active'});
 add('role',{name:'Administrator',read_records:true,create_drafts:true,approve:true,export:true,status:'Active'});
 add('staff',{name:'Jude Nshome',email:'demo@example.invalid',role:'role-1',branch:'branch-1',status:'Active'});
 ['Building materials','Steel & roofing','Paint & finishes','Plumbing','Tools'].forEach(name=>add('category',{name,status:'Active'}));
 [['CEM-001','Cement CPJ 35 · 50 kg','4850','4000','240'],['ROD-012','Steel bar · 12 mm','6200','5100','86'],['PNT-020','White paint · 20 L','28500','23000','32'],['PVC-050','PVC pipe · 50 mm','3200','2600','64'],['SCR-100','Wood screws · box','2500','1800','120']].forEach(([sku,name,selling_price,purchase_cost,stock],i)=>add('product',{sku,name,selling_price,purchase_cost,stock,opening_stock:stock,base_unit:'piece',category:'category-'+(i+1),reorder_level:'40',tax_rate:'0',track_stock:true,status:'Active'}));
 add('customer',{name:'Mbella Construction',type:'Company',phone:'+237 600000001',credit_limit:'500000',payment_terms_days:'30',status:'Active'});add('customer',{name:'Amina Trading',type:'Contractor',credit_limit:'200000',status:'Active'});
 add('supplier',{name:'Central Supplies',payment_terms_days:'30',status:'Active'});
 const lines=(n)=>[{id:uid('LINE'),product:'product-1',description:'Cement CPJ 35 · 50 kg',quantity:String(n),unit_price:'4850',unit_cost:'4000',tax_rate:'0',received_quantity:'0',counted_quantity:'0'}];
 add('sale',{customer:'customer-1',channel:'Counter',fulfillment:'Customer custody',discount:'0',status:'Completed',lines:lines(10),total:'48500',paid:'20000',cashier:'staff-1',price_tier:'Retail',due_date:'2026-12-17'});
 add('sale',{customer:'customer-2',channel:'Counter',fulfillment:'Immediate',discount:'0',status:'Draft',lines:lines(5),total:'24250',paid:'0',cashier:'staff-1',price_tier:'Retail'});
 add('quote',{customer:'customer-1',valid_until:'2026-12-01',lines:lines(15),discount:'0',status:'Draft'});
 add('purchase',{supplier:'supplier-1',warehouse:'warehouse-1',lines:lines(30).map(l=>({...l,unit_price:'4000'})),status:'Approved'});
 add('receipt',{purchase_order:'purchase-1',supplier:'supplier-1',warehouse:'warehouse-1',lines:lines(10).map(l=>({...l,unit_price:'4000'})),status:'Draft'});
 add('payment',{customer:'customer-1',sale:'sale-1',amount:'20000',allocation_amount:'20000',tender:'Cash',status:'Completed'});
 add('deposit',{customer:'customer-2',amount:'10000',allocated_amount:'0',tender:'Cash',status:'Completed'});
 add('fulfillment',{sale:'sale-1',customer:'customer-1',warehouse:'warehouse-1',method:'Pickup',lines:lines(10),status:'Pending'});
 add('custody',{sale:'sale-1',customer:'customer-1',product:'product-1',owned_quantity:'10',collected_quantity:'0',warehouse:'warehouse-1',status:'Pending'});
 add('cash',{register:'Till 1',operator:'staff-1',opening_amount:'50000',expected_amount:'70000',counted_amount:'69500',status:'Open'});
 add('expense',{category:'Delivery',payee:'Demo transporter',amount:'5000',description:'Local delivery',status:'Draft'});
 add('period',{name:'November 2026',company:'company-1',start_date:'2026-11-01',end_date:'2026-11-30',status:'Open'});
 add('device',{name:'Counter Windows terminal',branch:'branch-1',platform:'Windows',epoch:'1',status:'Active'});
 add('quota',{device:'device-1',product:'product-1',granted_quantity:'20',consumed_quantity:'5',status:'Active'});
 add('count',{warehouse:'warehouse-1',assigned_operator:'staff-1',lines:lines(240).map(l=>({...l,counted_quantity:'239'})),status:'Draft'});
 for(const [e,sch] of Object.entries(C.schemas)){
  if(db.entities[e].length)continue;
  const v={};
  for(const f of sch.fields){if(f.type==='relation')v[f.key]=db.entities[f.entity]?.[0]?.id||'';else if(f.type==='select')v[f.key]=f.options[0];else if(['money','decimal','number'].includes(f.type))v[f.key]='0';else if(f.type==='date')v[f.key]='2026-11-17';else if(f.type==='checkbox')v[f.key]=false;else if(f.type==='password'||f.type==='file')v[f.key]='';else v[f.key]=f.required?'Demo '+f.label:''}
  add(e,v);
 }
 db.audit.push({id:uid('AUD'),date:'2026-11-17T08:00:00Z',entity:'company',record_id:'company-1',action:'Demo dataset initialized',actor:'Demo operator',reason:'Synthetic data',operation_id:uid('OP')});
 return db;
}
let DB;try{DB=JSON.parse(localStorage.getItem('quincai-v3'));if(DB?.version!==3)DB=null}catch{}if(!DB)DB=seed();
let storageError='';
function persist(){try{localStorage.setItem('quincai-v3',JSON.stringify(DB));storageError='';return true}catch{storageError='Browser storage is full or unavailable. Changes remain in this tab only.';return false}}
function rows(e){return DB.entities[e]||[]}
function get(e,id){return rows(e).find(x=>x.id===id)}
function nameOf(e,id){let r=get(e,id);return r?.name||r?.legal_name||r?.title||r?.reference||id||'—'}
function total(r){if(r.lines?.length)return sub(sum(r.lines.map(l=>mul(l.quantity,l.unit_price))),r.discount||'0');return r.amount||r.total||'0'}
function audit(e,id,action,reason){DB.audit.unshift({id:uid('AUD'),date:new Date().toISOString(),entity:e,record_id:id,action,reason,actor:'Demo operator',operation_id:uid('OP')});persist()}
function saveRecord(e,input,id,version){
 let sch=C.schemas[e],old=get(e,id),errors={};
 if(old&&['Completed','Posted','Closed'].includes(old.status))throw Error('Finalized local records are read-only. Create a linked correction instead.');
 if(old&&old.version!==version)throw Error('This record changed. Reload it before saving.');
 for(const f of sch.fields){let v=input[f.key];if(f.required&&(v===undefined||v===null||v===''))errors[f.key]='Required field';
  if(v!==''&&v!=null&&['money','decimal','number'].includes(f.type)){try{if(dec(v)<0n)errors[f.key]='Must be zero or positive'}catch(err){errors[f.key]=err.message}}
  if(f.type==='relation'&&v&&!get(f.entity,v))errors[f.key]='Choose an existing record';
  if(f.type==='email'&&v&&!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v))errors[f.key]='Enter a valid email';
 }
 if(e==='product'&&rows(e).some(r=>r.id!==id&&r.sku===input.sku))errors.sku='SKU already exists';
 if(e==='transfer'&&input.origin_warehouse===input.destination_warehouse)errors.destination_warehouse='Choose a different destination';
 if(e==='intercompany'&&input.seller_company===input.buyer_company)errors.buyer_company='Buyer and seller must differ';
 if(input.start_date&&input.end_date&&input.end_date<input.start_date)errors.end_date='End must follow start';
 if(Object.keys(errors).length)return {errors};
 const safe={...input};for(const f of sch.fields)if(f.type==='password')delete safe[f.key];
 let rec={...old,...safe,id:old?.id||uid(e),reference:old?.reference||uid(e.slice(0,4).toUpperCase()),status:old?.status||'Draft',version:(old?.version||0)+1,branch_id:DB.preferences.branch,company_id:'company-1',business_date:input.business_date||'2026-11-17',created_at:old?.created_at||new Date().toISOString(),lines:old?.lines||[]};
 old?Object.assign(old,rec):DB.entities[e].push(rec);audit(e,rec.id,old?'Draft edited':'Draft created','Local UI demonstration');return {record:rec};
}
const TRANSITIONS={submit:{from:['Draft','Rejected'],to:'In review'},approve:{from:['In review','Pending'],to:'Approved'},reject:{from:['In review','Pending'],to:'Rejected'},complete:{from:['Draft','Approved','Open','Pending'],to:'Completed'},cancel:{from:['Draft','In review','Pending'],to:'Cancelled'},archive:{from:['Draft','Active','Inactive'],to:'Archived'},activate:{from:['Draft','Inactive'],to:'Active'},hold:{from:['Draft'],to:'Held'},resume:{from:['Held'],to:'Draft'}};
function transition(e,id,verb,reason,expected){
 const rec=get(e,id),rule=TRANSITIONS[verb];if(!rec||!rule)throw Error('Unknown local operation.');
 if(rec.version!==expected)throw Error('Record version changed. Reload and retry.');
 if(!rule.from.includes(rec.status))throw Error('Cannot '+verb+' a '+rec.status+' record. Allowed: '+rule.from.join(', ')+'.');
 if(!reason?.trim())throw Error('Enter a reason.');
 rec.status=rule.to;rec.version++;audit(e,id,'Local '+verb,reason);return rec;
}
function snapshot(doc,r){const snap={id:uid('SNAP'),doc_id:doc.id,source_id:r.id,source_entity:doc.entity,source_version:r.version,created_at:new Date().toISOString(),template_version:'3.0',record:structuredClone(r),company:structuredClone(rows('company')[0]),demo:true};DB.snapshots.push(snap);audit(doc.entity,r.id,'Local document snapshot created',snap.id);return snap}
