import {readFile, writeFile} from 'node:fs/promises';
import {randomUUID} from 'node:crypto';
import {pathToFileURL} from 'node:url';
import {performance} from 'node:perf_hooks';
export function percentile(values,p) {if(!values.length)return null;const s=[...values].sort((a,b)=>a-b);return s[Math.max(0,Math.ceil(p*s.length)-1)];}
export function validate(config,env) {
 if(env.QUINCAI_PERF_SYNTHETIC!=='true')throw Error('Synthetic environment acknowledgment required');
 const base=new URL(env.QUINCAI_PERF_BASE_URL);
 if(base.username||base.password||base.search||base.hash||base.pathname!=='/'||!(base.protocol==='https:'||(base.protocol==='http:'&&['localhost','127.0.0.1','[::1]'].includes(base.hostname))))throw Error('Explicit HTTPS or loopback base URL required');
 for(const [key,max] of [['durationSeconds',600],['ratePerSecond',100],['concurrency',100],['timeoutMs',30000]])if(!Number.isInteger(config[key])||config[key]<1||config[key]>max)throw Error(`Invalid ${key}`);
 if(config.durationSeconds*config.ratePerSecond>10000)throw Error('Maximum 10000 scheduled requests');
 if(!Array.isArray(config.operations)||!config.operations.length)throw Error('Operations required');
 if(!Array.isArray(config.tokenEnvs)||!config.tokenEnvs.length||config.tokenEnvs.some(n=>!/^QUINCAI_PERF_TOKEN_[A-Z0-9_]+$/.test(n)||!env[n]))throw Error('Explicit token environment variables required');
 const names=new Set();
 for(const op of config.operations){
  if(!/^[a-z][a-z0-9_-]{0,63}$/.test(op.name)||names.has(op.name))throw Error('Unique non-identifying operation name required');names.add(op.name);
  if(typeof op.path!=='string'||!op.path.startsWith('/api/v1/')||op.path.includes('\\')||op.path.includes('#')||new URL(op.path,base).origin!==base.origin)throw Error('Relative API path required');
  if(!['GET','POST','PUT','PATCH','DELETE'].includes(op.method))throw Error('Invalid method');
  if(op.method!=='GET'&&env.QUINCAI_PERF_ALLOW_WRITES!=='true')throw Error('Explicit synthetic writes acknowledgment required');
  if(!Array.isArray(op.expectedStatuses)||!op.expectedStatuses.length||op.expectedStatuses.some(s=>!Number.isInteger(s)||s<200||s>299))throw Error('Successful expected statuses required');
 }
 if(config.thresholds)for(const key of ['p95Ms','maxErrorFraction','maxDroppedFraction'])if(!Number.isFinite(config.thresholds[key])||config.thresholds[key]<0)throw Error('Complete thresholds required');
 return base;
}
export async function run(config,env=process.env) {
 const base=validate(config,env), results=new Map(config.operations.map(o=>[o.name,{latencies:[],errors:0,count:0}]));
 let active=0,dropped=0,maxSchedulingDelayMs=0;const pending=new Set(),started=performance.now();
 const total=config.durationSeconds*config.ratePerSecond;
 async function request(op,index){
  const result=results.get(op.name);result.count++;active++;const begin=performance.now();
  try{
   const id=randomUUID(); const substitute=x=>x.replaceAll('{{operationId}}',id).replaceAll('{{requestId}}',id);
   const response=await fetch(new URL(substitute(op.path),base),{method:op.method,redirect:'manual',signal:AbortSignal.timeout(config.timeoutMs),headers:{Authorization:`Bearer ${env[config.tokenEnvs[index%config.tokenEnvs.length]]}`,'Content-Type':'application/json',...(op.method==='GET'?{}:{'Idempotency-Key':id})},...(op.body===undefined?{}:{body:substitute(JSON.stringify(op.body))})});
   // Drain bounded bytes: do not retain or emit customer data or response bodies.
   let size=0; if(response.body)for await(const bytes of response.body){size+=bytes.length;if(size>1048576)throw Error('ResponseTooLarge');}
   if(!op.expectedStatuses.includes(response.status))result.errors++;
  }catch{result.errors++;}finally{result.latencies.push(performance.now()-begin);active--;}
 }
 for(let index=0;index<total;index++){
  const due=started+index*1000/config.ratePerSecond,wait=due-performance.now();if(wait>0)await new Promise(r=>setTimeout(r,wait));
  const delay=performance.now()-due;maxSchedulingDelayMs=Math.max(maxSchedulingDelayMs,delay);
  if(active>=config.concurrency||delay>1000/config.ratePerSecond){dropped++;continue;}
  const job=request(config.operations[index%config.operations.length],index);pending.add(job);job.finally(()=>pending.delete(job));
 }
 await Promise.all(pending);
 const elapsedMs=performance.now()-started;const operations=Object.fromEntries([...results].map(([name,r])=>[name,{count:r.count,errors:r.errors,p50Ms:percentile(r.latencies,.5),p95Ms:percentile(r.latencies,.95),p99Ms:percentile(r.latencies,.99)}]));
 const errors=[...results.values()].reduce((a,r)=>a+r.errors,0),count=total-dropped;
 const thresholds=config.thresholds;
 const accepted=thresholds&&count>0&&errors/count<=thresholds.maxErrorFraction&&dropped/total<=thresholds.maxDroppedFraction&&Object.values(operations).every(r=>r.count>0&&r.p95Ms<=thresholds.p95Ms);
 // Write correctness requires an independent business reconciliation; this harness cannot certify it.
 const writes=config.operations.some(o=>o.method!=='GET');
 const status=!thresholds||writes?'MEASURED_NOT_ACCEPTED':accepted?'PASS_SCENARIO':'FAIL_SCENARIO';
 return {status,scope:'Configured synthetic HTTP scenario only; no production capacity certification',scheduled:total,completed:count,errors,dropped,elapsedMs,requestsPerSecond:count/(elapsedMs/1000),maxSchedulingDelayMs,operations,writeReconciliationRequired:writes};
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){
 try{const config=JSON.parse(await readFile(process.argv[2],'utf8'));const result=await run(config);await writeFile(process.argv[3]??'performance-result.json',JSON.stringify(result,null,2)+'\n',{flag:'wx',mode:0o600});console.log(result.status);process.exitCode=result.status==='PASS_SCENARIO'?0:result.status==='FAIL_SCENARIO'?1:2;}catch{console.error('Performance execution failed; check configuration, credentials, connectivity and output path.');process.exitCode=1;}
}
