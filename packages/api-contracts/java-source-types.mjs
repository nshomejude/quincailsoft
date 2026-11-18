/** Conservative declaration lookup: lexical owner first, then declared package/imports.
 * Unknown or ambiguous names are never inferred from a global simple-name match.
 */
export function sourceContext(source,owner){return {owner,packageName:source.match(/\bpackage\s+([\w.]+)\s*;/)?.[1]??'',imports:[...source.matchAll(/\bimport\s+(?!static\b)([\w.*]+)\s*;/g)].map(m=>m[1])};}
export function declarationIdentity(context,name){const topLevel=context.owner===name,key=topLevel?name:context.owner+'.'+name;return {key,qualifiedName:context.packageName+'.'+key};}
export function resolveDeclaration(type,owner,declarations,contexts){
 const local=owner+'.'+type;if(declarations.has(local))return local;
 // A qualified nested declaration already names its owner; never select a bare enum globally.
 if(type.includes('.')&&declarations.has(type))return type;
 const context=contexts.get(owner);if(!context)return null;
 const [head,...tail]=type.split('.'),suffix=tail.length?'.'+tail.join('.'):'';
 const candidates=new Set([type,context.packageName+'.'+type]);
 for(const imported of context.imports){if(imported.endsWith('.*'))candidates.add(imported.slice(0,-1)+type);else if(imported.split('.').at(-1)===head)candidates.add(imported+suffix);}
 const matches=[...declarations].filter(([,value])=>candidates.has(value.qualifiedName)).map(([key])=>key);
 return matches.length===1?matches[0]:null;
}
export function plainEnums(source,context,sourcePath){
 // Only ordinary name-serialized enums are supported. Constructors, bodies,
 // Jackson overrides or annotated constants remain unresolved for explicit review.
 const clean=source.replace(/"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\/\*[\s\S]*?\*\/|\/\/[^\n]*/g,token=>token.startsWith('"')?'""':token.startsWith("'")?"''":'');
 if(/@(JsonValue|JsonFormat|JsonSerialize|JsonProperty)\b/.test(clean))return [];
 const result=[];
 for(const match of clean.matchAll(/\benum\s+(\w+)\s*\{([^{}]*)\}/g)){
  const body=match[2].trim().replace(/;\s*$/,'');
  if(!/^[A-Za-z_$][\w$]*(?:\s*,\s*[A-Za-z_$][\w$]*)*\s*,?$/.test(body))continue;
  const values=body.split(',').map(v=>v.trim()).filter(Boolean),identity=declarationIdentity(context,match[1]);
  result.push([identity.key,{...identity,owner:context.owner,name:match[1],values,source:sourcePath}]);
 }
 return result;
}
