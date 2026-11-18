/** Source shape: ReportingService.Definition; backend remains the authorization authority. */
export type ReportDefinition={code:string;title:string;basis:string;columns:string[];requiresDateRange:boolean;permission:string};
export function reportQuery(definition:ReportDefinition,input:{from?:string;to?:string;offset?:string},today:string){
 const query=new URLSearchParams({offset:/^\d+$/.test(input.offset??'')?input.offset!:'0',limit:'100'});
 if(definition.requiresDateRange){query.set('from',input.from??today);query.set('to',input.to??today)}
 return query;
}
export function reportExportTarget(code:string,catalog:ReportDefinition[],query:URLSearchParams,base:string):URL|null{
 if(!/^[a-z][a-z0-9-]{0,63}$/.test(code)||!catalog.some(report=>report.code===code))return null;
 const target=new URL('/api/v1/reports/'+code+'/export',base);
 for(const key of ['from','to','offset','limit']){const value=query.get(key);if(value)target.searchParams.set(key,value)}
 return target;
}
