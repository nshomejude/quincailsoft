const $=s=>document.querySelector(s);
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const cash=x=>new Intl.NumberFormat('en-CM',{maximumFractionDigits:6}).format(Number(x||0))+' FCFA';
const human=s=>s.replaceAll('_',' ').replace(/^./,c=>c.toUpperCase());
let current={},filters={q:'',status:'',from:'',to:''},fieldErrors={},dialogState=null;
const screenById=id=>C.screens.find(s=>s.id===id);
function route(id,params={}){const s=screenById(id);const base=s?s.path:id;return '#'+base+(Object.keys(params).length?'?'+new URLSearchParams(params):'')}
function entityLink(e,id,mode='detail'){const s=C.schemas[e];return route(s.detail||C.screens.find(x=>x.entity===e)?.id||'S01',{entity:e,record:id,mode})}
function link(id,label,params={}){return '<a href="'+esc(route(id,params))+'">'+esc(label)+'</a>'}
function button(label,act,attrs='',primary=false){return '<button type="button" data-act="'+act+'" '+attrs+' class="'+(primary?'primary':'')+'">'+esc(label)+'</button>'}
function pill(v){return '<span class="pill status-'+esc(String(v).toLowerCase().replaceAll(' ','-'))+'">'+esc(v)+'</span>'}
function title(k,t,sub=''){return '<div class="page-top"><div><div class="eyebrow">'+esc(k)+'</div><h1>'+esc(t)+'</h1><p class="sub">'+esc(sub)+'</p></div></div>'}
function table(headers,data){return '<div class="table-wrap"><table><thead><tr>'+headers.map(h=>'<th scope="col">'+esc(h)+'</th>').join('')+'</tr></thead><tbody>'+data.map(row=>'<tr>'+row.map(c=>'<td>'+c+'</td>').join('')+'</tr>').join('')+'</tbody></table></div>'}
function toast(text){$('#toast').textContent=text;$('#toast').hidden=false}
function crumbs(s){return '<div class="breadcrumbs">'+link('S01','Home')+' / '+link(s.id,s.title)+' / '+esc(current.mode||s.kind)+'</div>'}
function navigation(){
 const q=$('#search').value.toLowerCase();const groups=[['WORKSPACE',[['Overview','S01'],['Point of sale','S04'],['Sales','S05'],['Quotations','S37'],['Customers','S32']]],['OPERATIONS',[['Products','S10'],['Inventory','S17'],['Purchasing','S24'],['Suppliers','S29'],['Pickup & delivery','S74']]],['MANAGEMENT',[['Cash & expenses','S40'],['Reports','S45'],['Documents','S53'],['Settings','S81']]]];
 $('#nav').innerHTML=q?C.screens.filter(s=>(s.id+' '+s.title).toLowerCase().includes(q)).map(s=>'<a href="'+route(s.id)+'">'+esc(s.id+' · '+s.title)+'</a>').join(''):groups.map(([g,items])=>'<h3>'+g+'</h3>'+items.map(([n,id])=>'<a class="'+(current.s?.id===id?'active':'')+'" href="'+route(id)+'">'+esc(n)+'</a>').join('')).join('')+'<details class="all-pages"><summary>All 152 screens</summary>'+C.screens.map(s=>'<a href="'+route(s.id)+'">'+esc(s.id+' · '+s.title)+'</a>').join('')+'</details>';
}
function readRoute(){const old=location.hash.slice(1);const raw=({home:'S01',documents:'S53',reports:'S45',design:'S81'})[old]||old||screenById('S01').path;const [path,q='']=raw.split('?');const params=Object.fromEntries(new URLSearchParams(q));let s=C.screens.find(s=>s.path===path)||screenById(path);
 if(path.startsWith('/entity/')){const parts=path.split('/');params.entity=parts[2];params.record=parts[3];params.mode='detail';s=screenById(C.schemas[params.entity]?.detail)||C.screens.find(x=>x.entity===params.entity)}
 return {s,params,entity:params.entity||s?.entity,record:params.record,mode:params.mode||s?.kind,tab:params.tab||'Overview'};
}
function field(f,v='',prefix='form'){const id=prefix+'-'+f.key,req=f.required?' required':'',common=' id="'+id+'" name="'+f.key+'"'+req;let control;const val=String(v??'');
 if(f.type==='relation'){control='<select'+common+'><option value="">Choose '+esc(f.label)+'</option>'+rows(f.entity).map(r=>'<option value="'+esc(r.id)+'" '+(r.id===v?'selected':'')+'>'+esc(nameOf(f.entity,r.id))+'</option>').join('')+'</select><a class="field-link" href="'+route(C.schemas[f.entity].editor||C.schemas[f.entity].detail,{entity:f.entity,mode:'create'})+'">Create '+esc(C.schemas[f.entity].title.toLowerCase())+'</a>'}
 else if(f.type==='select')control='<select'+common+'>'+f.options.map(o=>'<option '+(o===v?'selected':'')+'>'+esc(o)+'</option>').join('')+'</select>';
 else if(f.type==='textarea')control='<textarea'+common+' rows="3">'+esc(val)+'</textarea>';
 else if(f.type==='checkbox')control='<input type="checkbox"'+common+' '+(v?'checked':'')+'>';
 else if(f.type==='file')control='<input type="file"'+common+'><small>Local metadata only; attachment upload and malware scanning are not connected.</small>'+(val?'<small>'+esc(val)+'</small>':'');
 else control='<input'+common+' type="'+(['money','decimal','number'].includes(f.type)?'text':f.type)+'" '+(['money','decimal','number'].includes(f.type)?'inputmode="decimal" pattern="-?[0-9]+([.][0-9]{1,6})?"':'')+' value="'+esc(f.type==='password'?'':val)+'" autocomplete="'+(f.type==='password'?'new-password':'off')+'">';
 return '<div class="field '+(f.type==='textarea'?'wide':'')+'"><label for="'+id+'">'+esc(f.label)+(f.required?' <span aria-label="required">*</span>':'')+'</label>'+control+(fieldErrors[f.key]?'<p class="error" role="alert">'+esc(fieldErrors[f.key])+'</p>':'')+'</div>';
}
function formView(s,e,id){const sch=C.schemas[e],r=get(e,id)||{},isLocked=['Completed','Posted','Closed'].includes(r.status);
 if(!sch)return '<div class="card">No editable fields apply to this workspace.</div>';
 return '<form id="entity-form" data-entity="'+e+'" data-id="'+esc(id||'')+'" data-version="'+(r.version||0)+'" class="card"><div class="section-head"><h2>'+esc(id?'Edit '+r.reference:'New '+sch.title.toLowerCase())+'</h2>'+pill(r.status||'Draft')+'</div>'+(isLocked?'<div class="notice">Finalized fields are read-only. Use a linked correction or return.</div>':'')+'<fieldset '+(isLocked?'disabled':'')+'><div class="fields">'+sch.fields.map(f=>field(f,r[f.key]??(f.type==='date'?'2026-11-17':f.type==='relation'?'':f.type==='select'?f.options[0]:''))).join('')+'</div></fieldset><div class="actions">'+(!isLocked?'<button class="primary" type="submit">Save draft</button>':'')+link(s.id,'Back to register',{entity:e,mode:'list'})+'</div><p class="muted">Saved to the local integration database. Synthetic data only.</p></form>';
}
function display(f,v){if(f.type==='relation')return v?'<a href="'+entityLink(f.entity,v)+'">'+esc(nameOf(f.entity,v))+'</a>':'—';if(f.type==='money')return esc(cash(v));if(f.type==='checkbox')return v?'Yes':'No';if(f.type==='password')return 'Never stored';return esc(v||'—')}
function related(e,id){const found=[];for(const [name,sch] of Object.entries(C.schemas)){for(const f of sch.fields.filter(f=>f.type==='relation'&&f.entity===e))for(const r of rows(name).filter(r=>r[f.key]===id))found.push({entity:name,record:r,relation:f.label})}return found}
function history(e,id){const list=DB.audit.filter(a=>(!e||a.entity===e)&&(!id||a.record_id===id));return list.length?table(['Time','Action','Actor','Reason','Operation'],list.map(a=>[esc(a.date),esc(a.action),esc(a.actor),esc(a.reason),esc(a.operation_id)])):'<div class="empty-state">No recorded history for this record.</div>'}
function detailView(s,e,id){
 const r=get(e,id);if(!r)return '<div class="card"><h2>Record not found</h2><p>The link refers to a missing or unavailable record.</p>'+link(s.id,'Back to register',{mode:'list'})+'</div>';
 const sch=C.schemas[e],relations=related(e,id),docs=C.documents.filter(d=>d.entity===e);
 const tabs=['Overview',...(r.lines?.length||s.has_lines?['Items']:[]),'Related records','Documents','History',...s.tabs.map(t=>t.charAt(0).toUpperCase()+t.slice(1))].filter((v,i,a)=>a.findIndex(x=>x.toLowerCase()===v.toLowerCase())===i);
 let chosen=current.params.tab||'Overview';
 let content='';
 if(chosen==='Items')content=lineView(e,r);
 else if(chosen==='History'||/history|audit/i.test(chosen))content=history(e,id);
 else if(chosen==='Documents'||/receipt|preview|A4/i.test(chosen))content=documentCards(docs,e,id);
 else if(chosen==='Related records'||/payment|purchase|credit|reservation|dispatch|allocation|receipt/i.test(chosen)){
 const filtered=chosen==='Related records'?relations:relations.filter(x=>(x.entity+' '+x.relation).toLowerCase().includes(chosen.split(' ')[0].toLowerCase().replace(/s$/,'')));
 content=filtered.length?table(['Type','Reference','Relationship','Status','Open'],filtered.map(x=>[esc(C.schemas[x.entity].title),esc(x.record.reference),esc(x.relation),pill(x.record.status),'<a href="'+entityLink(x.entity,x.record.id)+'">Open detail</a>'])):'<div class="empty-state">No '+esc(chosen.toLowerCase())+' linked yet.</div>';
 }else content='<div class="detail-fields">'+sch.fields.map(f=>'<div><dt>'+esc(f.label)+'</dt><dd>'+display(f,r[f.key])+'</dd></div>').join('')+'</div>'+(r.lines?.length?lineView(e,r):'');
 return '<div class="record-heading"><div><h2>'+esc(r.reference)+'</h2><span class="muted">Version '+r.version+' · '+esc(r.business_date)+'</span></div>'+pill(r.status)+'</div><div class="actions">'+link(s.id,'Edit draft',{entity:e,record:id,mode:'edit'})+button('Duplicate draft','duplicate','data-entity="'+e+'" data-id="'+id+'"')+button('Submit for review','transition','data-command="submit"')+button('Approve request','transition','data-command="approve"')+button('Complete record','transition','data-command="complete"')+'</div><div class="tabs" role="tablist">'+tabs.map(t=>'<a role="tab" aria-selected="'+(t===chosen)+'" class="'+(t===chosen?'selected':'')+'" href="'+route(s.id,{entity:e,record:id,mode:'detail',tab:t})+'">'+esc(t)+'</a>').join('')+'</div><section class="card">'+content+'</section>';
}
function listView(s,e){const sch=C.schemas[e];if(!sch)return '<div class="card">Use the dedicated workspace tools.</div>';const available=rows(e),r=available.filter(r=>(r.branch_id===DB.preferences.branch||e==='branch'||e==='company')&&(!filters.q||JSON.stringify(r).toLowerCase().includes(filters.q.toLowerCase()))&&(!filters.status||r.status===filters.status));
 const selected=sch.fields.filter(f=>!['textarea','password','file','checkbox'].includes(f.type)).slice(0,4);
 return '<div class="card"><div class="section-head"><h2>'+esc(sch.title)+' '+pill(r.length)+'</h2><div class="actions">'+link(sch.editor||s.id,'New record',{entity:e,mode:'create'})+button('Export filtered CSV','export-list')+'</div></div><div class="filter-row"><label>Search<input id="list-search" value="'+esc(filters.q)+'" placeholder="Reference, name or value"></label><label>Status<select id="list-status"><option value="">All statuses</option>'+[...new Set(available.map(r=>r.status))].map(st=>'<option '+(filters.status===st?'selected':'')+'>'+esc(st)+'</option>').join('')+'</select></label></div><div id="list-results">'+(r.length?table(['Reference',...selected.map(f=>f.label),'Status','Actions'],r.map(r=>['<a href="'+entityLink(e,r.id)+'">'+esc(r.reference)+'</a>',...selected.map(f=>display(f,r[f.key])),pill(r.status),'<a href="'+entityLink(e,r.id)+'">Open detail</a>'])):'<div class="empty-state"><h3>No matching records</h3><p>Change filters or create a local draft.</p></div>')+'</div></div>';
}
function lineView(e,r){const locked=['Completed','Posted','Closed'].includes(r.status);const productOptions=rows('product').map(p=>'<option value="'+p.id+'">'+esc(p.sku+' · '+p.name)+'</option>').join('');
 return '<h2>Line items</h2>'+table(['Product','Quantity','Unit price','Amount','Actions'],(r.lines||[]).map(l=>['<a href="'+entityLink('product',l.product)+'">'+esc(l.description||nameOf('product',l.product))+'</a>',esc(l.quantity),esc(cash(l.unit_price)),esc(cash(mul(l.quantity,l.unit_price))),locked?'Read-only':button('Remove','remove-line','data-line="'+l.id+'"')]))+'<p class="total">Total '+cash(total(r))+'</p>'+(!locked?'<form id="line-form" class="line-form"><label>Product<select name="product" required>'+productOptions+'</select></label><label>Quantity<input name="quantity" value="1" inputmode="decimal" required></label><label>Unit price<input name="unit_price" inputmode="decimal" placeholder="Product price"></label><button type="submit" class="primary">Add line</button></form>':'');
}
function actionBar(s){return '<details class="workflow-actions"><summary>All '+s.actions.length+' workflow actions</summary><div class="actions">'+s.actions.map((a,i)=>button(a,'workflow','data-index="'+i+'"')).join('')+'</div></details>'}
function dashboard(){const completed=rows('sale').filter(r=>r.status==='Completed'),sales=sum(completed.map(total)),paid=sum(rows('payment').filter(r=>r.status==='Completed').map(r=>r.amount)),outstanding=sub(sales,paid),low=rows('product').filter(p=>dec(p.stock||0)<dec(p.reorder_level||0));
 return title('BUSINESS OVERVIEW','Your business, at a glance.','Linked local records · Demo company · Douala')+'<div class="actions quick">'+link('S04','New sale')+link('S12','Add product',{mode:'create'})+link('S27','Receive stock',{mode:'create'})+link('S35','Receive payment',{mode:'create'})+'</div><div class="kpi-grid">'+[['Completed sales',cash(sales),'S05'],['Collected payments',cash(paid),'S09'],['Outstanding',cash(outstanding),'S34'],['Low-stock products',low.length,'S23']].map(([n,v,id])=>'<a class="kpi" href="'+route(id)+'"><div class="kpi-label">'+n+'</div><div class="kpi-value">'+v+'</div><small>Open underlying records →</small></a>').join('')+'</div><div class="dashboard-mid"><section class="card"><div class="section-head"><h2>Recent sales</h2>'+link('S05','View all')+'</div>'+table(['Reference','Customer','Total','Status'],rows('sale').map(r=>['<a href="'+entityLink('sale',r.id)+'">'+esc(r.reference)+'</a>','<a href="'+entityLink('customer',r.customer)+'">'+esc(nameOf('customer',r.customer))+'</a>',cash(total(r)),pill(r.status)]))+'</section><section class="card"><h2>Operational priorities</h2><p>'+link('S23',low.length+' products below reorder')+'</p><p>'+link('S74',rows('fulfillment').filter(r=>r.status!=='Completed').length+' delivery records to review')+'</p><p>'+link('X12','Offline exception investigations')+'</p><p>'+link('S45','76 report views')+'</p><p>'+link('S53','26 document families')+'</p></section></div><section class="card"><h2>Recent local activity</h2>'+history()+'</section>';
}
function configHub(){return '<div class="grid">'+['S57','S58','S59','S60','S61','S63','S55','S56','S82','S83','S87','S88','N04','N05','N09','X23','X24'].map(id=>{let s=screenById(id);return '<div class="card"><h2>'+esc(s.title)+'</h2><p>'+esc(C.schemas[s.entity]?.title||s.area)+'</p>'+link(id,'Open settings')+'</div>'}).join('')+'</div>'}
function posView(){return '<div class="split"><section class="card"><label>Search products<input id="pos-search" placeholder="SKU or product"></label><div class="product-grid">'+rows('product').filter(p=>p.status==='Active').map(p=>'<button class="product-tile" data-act="add-basket" data-id="'+p.id+'"><strong>'+esc(p.name)+'</strong><small>'+esc(p.sku)+' · '+esc(p.stock)+' available</small><b>'+cash(p.selling_price)+'</b></button>').join('')+'</div></section><section class="card"><h2>Current basket</h2><label>Customer<select id="pos-customer">'+rows('customer').map(c=>'<option value="'+c.id+'">'+esc(c.name)+'</option>').join('')+'</select></label><div id="basket"></div></section></div>'}
function renderBasket(){if(!$('#basket'))return;$('#basket').innerHTML=DB.basket.length?DB.basket.map((l,i)=>'<div class="basket-line"><div><a href="'+entityLink('product',l.product)+'">'+esc(l.description)+'</a><small>'+cash(l.unit_price)+'</small></div>'+button('−','basket-minus','data-index="'+i+'"')+'<span>'+l.quantity+'</span>'+button('+','basket-plus','data-index="'+i+'"')+'</div>').join('')+'<h2>Total '+cash(total({lines:DB.basket}))+'</h2><div class="actions">'+button('Save sale draft','save-basket','',true)+button('Clear basket','clear-basket')+'</div>':'<div class="empty-state">Select products to start a sale.</div>'}
function documentCards(docs,e,id){return docs.length?'<div class="grid">'+docs.map(d=>'<div class="card"><span class="pill">'+d.id+'</span><h2>'+esc(d.title)+'</h2>'+link('S54','Preview document',{doc:d.id,entity:e||d.entity,record:id||rows(d.entity)[0]?.id||''})+'</div>').join('')+'</div>':'<p>No document family is assigned to this record type.</p>'}
function render(){current=readRoute();fieldErrors={};let s=current.s,html;
 if(!s){html=title('404','Workspace not found','Choose a screen from navigation.')}
 else {let e=current.entity,mode=current.mode;
 html=crumbs(s)+title(s.area+' / '+s.id,s.title,'Shared workspace · '+nameOf('branch',DB.preferences.branch))+actionBar(s);
 if(mode==='detail')html+=detailView(s,e,current.record||rows(e)[0]?.id);
 else if(mode==='create'||mode==='edit')html+=formView(s,e,mode==='edit'?current.record:null);
 else if(s.id==='S81')html+=configHub();
 else if(mode==='dashboard')html=dashboard();
 else if(mode==='reports')html+=reportView();
 else if(mode==='documents')html+=documentCards(C.documents);
 else if(mode==='document')html+=documentView();
 else if(mode==='pos')html+=posView();
 else if(mode==='commerce')html+='<div class="notice">Merchant storefront preview · Draft orders only</div>'+posView();
 else if(mode==='identity')html+='<div class="notice">Identity UI only. This demonstration does not authenticate users or persist passwords.</div>'+formView(s,e,current.record);
 else if(mode==='form')html+=formView(s,e,current.record);
 else html+=listView(s,e);
 }
 $('#main').innerHTML=html;navigation();renderBasket();document.title=(s?.title||'Not found')+' · QuincaiSoft';document.body.classList.remove('nav-open');if(storageError)toast(storageError);
}
