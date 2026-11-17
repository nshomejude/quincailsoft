const vm=require('vm'),fs=require('fs'),crypto=require('crypto').webcrypto;
const ctx={window:{},crypto,structuredClone,localStorage:{getItem(){return null},setItem(){}},console};
vm.createContext(ctx);
for(const f of ['contracts.js','model.js'])vm.runInContext(fs.readFileSync(__dirname+'/../v3/'+f,'utf8'),ctx);
fs.writeFileSync(__dirname+'/seed.json',vm.runInContext('JSON.stringify(seed())',ctx));
