const fs=require('fs'),vm=require('vm'),path=require('path'),crypto=require('crypto').webcrypto;
const fake=()=>({innerHTML:'',value:'',textContent:'',hidden:false,addEventListener(){},classList:{remove(){}},setAttribute(){},focus(){}}),el={};
const ctx={window:{addEventListener(){}},document:{querySelector(s){return el[s]??=fake()},querySelectorAll(){return []},addEventListener(){},body:{classList:{remove(){}}}},location:{hash:''},localStorage:{getItem(){return null},setItem(){}},crypto,structuredClone,Intl,URLSearchParams,TextEncoder,setTimeout,Blob,URL,console};
vm.createContext(ctx);const root=path.resolve(__dirname,'..');
for(const f of ['v3/contracts.js','v3/model.js','v3/ui.js','v3/reports.js','v3/documents.js','v3/actions.js','v3/action-overrides.js','v4/workflows.js'])vm.runInContext(fs.readFileSync(path.join(root,f),'utf8'),ctx,{filename:f});
const result=vm.runInContext(`C.screens.flatMap(s=>s.actions.map((label,index)=>({screen:s.id,index,label,...actionDescriptor(s,label)})))`,ctx);
const counts={};for(const a of result){counts[a.kind]=(counts[a.kind]||0)+1;if(a.kind==='navigate'&&!vm.runInContext(`!!screenById(${JSON.stringify(a.target)})`,ctx))throw Error('Unknown navigation target');}
fs.writeFileSync(path.join(root,'docs/v5-action-bindings.json'),JSON.stringify({note:'Binding classification only. A server_workflow may explicitly reject unsupported cases. This does not certify workflow completion.',counts,actions:result},null,2));console.log(JSON.stringify(counts));
