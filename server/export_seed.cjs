const vm=require('vm'),fs=require('fs'),crypto=require('crypto').webcrypto;
const ctx={window:{},crypto,structuredClone,localStorage:{getItem(){return null},setItem(){}},console};
vm.createContext(ctx);
for(const f of ['contracts.js','model.js'])vm.runInContext(fs.readFileSync(__dirname+'/../v3/'+f,'utf8'),ctx);
const seed=JSON.parse(vm.runInContext('JSON.stringify(seed())',ctx));
for(const rows of Object.values(seed.entities))for(const record of rows)if(!Number.isInteger(record.version))record.version=1;
fs.writeFileSync(__dirname+'/seed.json',JSON.stringify(seed,null,2));
