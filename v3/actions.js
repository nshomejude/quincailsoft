function exportCSV(name,headers,data){
 const cell=v=>{v=String(v??'');if(/^[=+@\-\t\r]/.test(v))v="'"+v;return '"'+v.replaceAll('"','""')+'"'};
 download(name+'.csv','text/csv','\uFEFF'+[headers,...data].map(row=>row.map(cell).join(',')).join('\r\n'));
}
function download(name,type,content){const a=document.createElement('a');const url=URL.createObjectURL(new Blob([content],{type}));a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
function go(id,params={}){location.hash=route(id,params).slice(1)}
function openModal(title,body,state){dialogState=state;$('#dialog-title').textContent=title;$('#dialog-body').innerHTML=body;$('#modal-error').textContent='';$('#dialog').showModal()}
function closeModal(){$('#dialog').close();dialogState=null}
function reviewTransition(cmd){let e=current.entity,id=current.record||rows(e)[0]?.id,r=get(e,id);if(!r)throw Error('Choose a record first.');
 openModal('Review '+cmd,'<p>'+esc(r.reference)+' · '+esc(r.status)+'</p><p>The server checks your permissions, branch scope and record version before applying this change.</p><label for="command-reason">Reason</label><textarea id="command-reason" required></textarea><div class="actions">'+button('Apply transition','confirm-transition','',true)+button('Cancel','close-modal')+'</div>',{type:'transition',entity:e,id,version:r.version,command:cmd})}
function actionDescriptor(s,label){
 const t=label.toLowerCase(),e=s.entity;
 const nav=(target,mode,extra={})=>({kind:'navigate',target,params:{...(mode?{mode}:{}),...extra}});
 if(/open pos|convert to sale|stage replacement/.test(t))return nav('S04','pos');
 if(/new po|create po|convert to rfq\/po/.test(t))return nav('S25','create');
 if(/receive goods/.test(t))return nav('S27','create');
 if(/receive payment|take payment/.test(t))return nav('S35','create');
 if(/record deposit/.test(t))return nav('S76','create');
 if(/view pickup/.test(t))return nav('S75','list');
 if(/stock card/.test(t))return nav('S18','list');
 if(/print statement|view statement/.test(t))return {kind:'document',doc:'DOC08'};
 if(/credit note/.test(t))return {kind:'document',doc:'DOC10'};
 if(/delivery note|pick ticket/.test(t))return {kind:'document',doc:'DOC12'};
 if(/print grn/.test(t))return {kind:'document',doc:'DOC06'};
 if(/print label/.test(t))return nav('S16','list');
 if(/change price/.test(t))return nav('S15','create');
 if(/initiate return|inspect return/.test(t))return nav('S08','create');
 if(/new expense/.test(t))return nav('S84','create');
 if(/pay suppliers|^pay$/.test(t)&&e==='supplier')return nav('S31','create');
 if(/search|filter|set date range|choose items/.test(t))return {kind:'filter'};
 if(/export|download|copy diagnostics/.test(t))return {kind:'export'};
 if(/reprint|print|pdf|preview document/.test(t)){let d=C.documents.find(d=>d.entity===e);return d?{kind:'document',doc:d.id}:{kind:'print'}}
 if(/inspect|open detail|open sale|open profile|view ledger|drill down|open affected|open entity|^open$/.test(t))return {kind:'detail'};
 if(/history|view audit|audit$/.test(t))return {kind:'detail',tab:'History'};
 if(/new|^add|^create|^record|^draft$|^prepare|^capture/.test(t))return {kind:'create'};
 if(/^edit|^update|configure|set |^save|validate|^next$|^back$/.test(t))return {kind:'edit'};
 if(/duplicate|clone/.test(t))return {kind:'duplicate'};
 if(/submit|request approval/.test(t))return {kind:'transition',command:'submit'};
 if(/reject/.test(t))return {kind:'transition',command:'reject'};
 if(/approve|^accept$/.test(t))return {kind:'transition',command:'approve'};
 if(/^hold$/.test(t))return {kind:'transition',command:'hold'};
 if(/^resume$/.test(t))return {kind:'transition',command:'resume'};
 if(/cancel|discard/.test(t))return {kind:'transition',command:'cancel'};
 if(/archive|deactivate/.test(t))return {kind:'transition',command:'archive'};
 if(/^post|^close$|^finish|^confirm$/.test(t))return {kind:'transition',command:'complete'};
 return {kind:'integration_required',reason:'This action requires its domain service or a dedicated workflow implementation. It is not represented as completed by this local UI.'};
}
async function workflow(s,index){
 const label=s.actions[index],a=actionDescriptor(s,label),e=s.entity,id=current.record||rows(e)[0]?.id;
 if(a.kind==='navigate')return go(a.target,a.params);
 if(a.kind==='document'){let d=C.documents.find(d=>d.id===a.doc);return go('S54',{doc:a.doc,record:d.entity===e?id:rows(d.entity)[0]?.id||''})}
 if(a.kind==='create')return go(C.schemas[e]?.editor||s.id,{entity:e,mode:'create'});
 if(a.kind==='edit')return go(C.schemas[e]?.editor||s.id,{entity:e,record:id||'',mode:id?'edit':'create'});
 if(a.kind==='detail')return go(C.schemas[e]?.detail||s.id,{entity:e,record:id||'',mode:'detail',...(a.tab?{tab:a.tab}:{})});
 if(a.kind==='transition')return reviewTransition(a.command);
 if(a.kind==='duplicate')return duplicate(e,id);
 if(a.kind==='export')return exportEntity(e);
 if(a.kind==='print')return window.print();
 if(a.kind==='filter'){const input=$('#list-search')||$('#report-search')||$('#pos-search');if(input){input.focus();return}go(s.id,{mode:'list'});return}
 openModal(label,'<div class="notice">'+esc(a.reason)+'</div><p>Screen '+s.id+' · Entity '+esc(e)+'</p><p>Required fields and source contract are preserved in the coverage register.</p><div class="actions">'+button('Open related workspace','open-current')+button('Close','close-modal')+'</div>',{type:'blocked'});
}
function duplicate(e,id){const source=get(e,id);if(!source)throw Error('Choose a source record.');const r={...structuredClone(source),id:uid(e),reference:uid(e.toUpperCase()),status:'Draft',version:1};DB.entities[e].push(r);audit(e,r.id,'Draft duplicated',source.id);go(C.schemas[e].detail||current.s.id,{entity:e,record:r.id,mode:'detail'})}
function exportEntity(e){const list=rows(e).filter(r=>(!filters.q||JSON.stringify(r).toLowerCase().includes(filters.q.toLowerCase()))&&(!filters.status||r.status===filters.status));const fields=C.schemas[e]?.fields.filter(f=>f.type!=='password')||[];exportCSV('QuincaiSoft-'+e,['Reference',...fields.map(f=>f.label),'Status'],list.map(r=>[r.reference,...fields.map(f=>r[f.key]??''),r.status]));audit(e,'','CSV exported','Filtered local records')}
document.addEventListener('click',async ev=>{
 const b=ev.target.closest('[data-act]');if(!b)return;const a=b.dataset.act;
 try{
 if(a==='workflow')await workflow(current.s,Number(b.dataset.index));
 if(a==='transition')reviewTransition(b.dataset.command);
 if(a==='confirm-transition'){const st=dialogState,reason=$('#command-reason').value;await transition(st.entity,st.id,st.command,reason,st.version);closeModal();render();toast('Record updated and audited.')}
 if(a==='close-modal')closeModal();
 if(a==='open-current'){closeModal();go(current.s.id,{entity:current.entity,mode:'list'})}
 if(a==='duplicate')await duplicate(b.dataset.entity,b.dataset.id);
 if(a==='export-list')exportEntity(current.entity);
 if(a==='print')window.print();
 if(a==='report-export'){const def=C.reports.find(r=>r.id===current.params.report),d=reportData(def);exportCSV(def.id+'-'+def.name,d.headers,d.items.map(x=>x.values));audit('audit','','Report exported',def.id)}
 if(a==='add-basket'){let p=get('product',b.dataset.id),line=DB.basket.find(x=>x.product===p.id);line?line.quantity=sum([line.quantity,'1']):DB.basket.push({id:uid('LINE'),product:p.id,description:p.name,quantity:'1',unit_price:p.selling_price,unit_cost:p.purchase_cost,tax_rate:p.tax_rate||'0'});persist();renderBasket()}
 if(a==='basket-plus'||a==='basket-minus'){let i=+b.dataset.index,l=DB.basket[i];l.quantity=sum([l.quantity,a==='basket-plus'?'1':'-1']);if(dec(l.quantity)<=0n)DB.basket.splice(i,1);persist();renderBasket()}
 if(a==='clear-basket'){DB.basket=[];persist();renderBasket()}
 if(a==='save-basket'){await saveBasket()}
 if(a==='remove-line'){await serverCommand(current.entity,current.record,'remove-line',{line:b.dataset.line});render()}
 if(a==='snapshot'){const result=await api('/api/snapshot',{doc:b.dataset.doc,record:b.dataset.record});await refreshState();go('S54',{doc:b.dataset.doc,record:b.dataset.record,snapshot:result.snapshot.id})}

 }catch(err){if($('#dialog').open)$('#modal-error').textContent=err.message;else toast(err.message)}
});
document.addEventListener('submit',async ev=>{
 ev.preventDefault();const form=ev.target;
 try{
 if(form.id==='entity-form'){
 const e=form.dataset.entity,id=form.dataset.id||null,sch=C.schemas[e],fd=new FormData(form),input={};
 for(const f of sch.fields){const v=fd.get(f.key);input[f.key]=f.type==='checkbox'?fd.has(f.key):f.type==='file'?(v?.name||get(e,id)?.[f.key]||''):String(v??'').trim()}
 const result=await saveRecord(e,input,id,Number(form.dataset.version));
 if(result.errors){fieldErrors=result.errors;document.querySelectorAll('.field .error').forEach(x=>x.remove());for(const [key,msg] of Object.entries(result.errors)){const control=form.elements[key];if(control){control.setAttribute('aria-invalid','true');const p=document.createElement('p');p.className='error';p.textContent=msg;control.closest('.field').append(p)}}toast('Correct the highlighted fields.');return}
 go(sch.detail||current.s.id,{entity:e,record:result.record.id,mode:'detail'});toast('Draft saved to the integration database.');
 }
 if(form.id==='line-form'){
 const r=get(current.entity,current.record),fd=new FormData(form),p=get('product',fd.get('product'));if(!r||['Completed','Posted','Closed'].includes(r.status))throw Error('Record is read-only.');
 const qty=String(fd.get('quantity')),price=String(fd.get('unit_price')||p.selling_price);if(dec(qty)<=0n||dec(price)<0n)throw Error('Quantity must be positive and price zero or positive.');
 await serverCommand(current.entity,r.id,'add-line',{product:p.id,quantity:qty,unit_price:price});render();
 }
 if(form.id==='report-filter'){const fd=new FormData(form),from=String(fd.get('from')),to=String(fd.get('to'));if(from&&to&&to<from)throw Error('End date must follow start date.');go('S45',{report:current.params.report,from,to})}
 }catch(err){toast(err.message)}
});
document.addEventListener('input',ev=>{
 if(ev.target.id==='search')navigation();
 if(ev.target.id==='list-search'){filters.q=ev.target.value;const s=current.s;const temp=document.createElement('div');temp.innerHTML=listView(s,current.entity);$('#list-results').innerHTML=temp.querySelector('#list-results').innerHTML}
 if(ev.target.id==='pos-search')document.querySelectorAll('.product-tile').forEach(x=>x.hidden=!x.textContent.toLowerCase().includes(ev.target.value.toLowerCase()));
 if(ev.target.id==='report-search')document.querySelectorAll('.report-card').forEach(x=>x.hidden=!x.textContent.toLowerCase().includes(ev.target.value.toLowerCase()));
});
document.addEventListener('change',ev=>{
 if(ev.target.id==='list-status'){filters.status=ev.target.value;const temp=document.createElement('div');temp.innerHTML=listView(current.s,current.entity);$('#list-results').innerHTML=temp.querySelector('#list-results').innerHTML}
 if(ev.target.id==='branch-select'){DB.preferences.branch=ev.target.value;persist();render()}
 if(ev.target.id==='paper-profile')go('S54',{...current.params,paper:ev.target.value});
});
window.addEventListener('hashchange',()=>{filters={q:'',status:'',from:'',to:''};render()});
$('#menu').onclick=()=>document.body.classList.toggle('nav-open');
$('#branch-select').innerHTML=rows('branch').map(r=>'<option value="'+r.id+'" '+(r.id===DB.preferences.branch?'selected':'')+'>'+esc(r.name)+'</option>').join('');
