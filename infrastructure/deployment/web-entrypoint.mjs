import {readFileSync} from 'node:fs';
import {spawn} from 'node:child_process';
import {pathToFileURL} from 'node:url';
import {resolve} from 'node:path';
export function runtimeEnvironment(input,read=path=>readFileSync(path,'utf8')) {
 const env={...input};
 if(!['web-admin','platform-admin'].includes(env.WEB_APP))throw Error('Unsupported web application');
 for(const key of ['REDIS_URL',env.WEB_APP==='web-admin'?'OIDC_CLIENT_SECRET':'PLATFORM_OIDC_CLIENT_SECRET']) {
  if(env[key])throw Error(`${key} must come from its secret file`);
  const file=env[key+'_FILE'];if(!file)throw Error(`${key}_FILE is required`);
  const value=read(file).trim();if(!value||value.includes('\n'))throw Error(`Invalid ${key} secret`);env[key]=value;delete env[key+'_FILE'];
 }
 return env;
}
if(process.argv[1]&&import.meta.url===pathToFileURL(resolve(process.argv[1])).href){
 try {
  const env=runtimeEnvironment(process.env);
  const child=spawn(process.execPath,[`/opt/quincaisoft/apps/${env.WEB_APP}/server.js`],{env,stdio:'inherit'});
  for(const signal of ['SIGTERM','SIGINT'])process.on(signal,()=>child.kill(signal));
  child.on('error',()=>{console.error('Web runtime could not start');process.exitCode=1});
  child.on('exit',(code)=>{process.exitCode=code??1});
 }catch(error){console.error(error.message);process.exitCode=1}
}
