import test from 'node:test';import assert from 'node:assert/strict';
import {reportQuery,reportExportTarget,type ReportDefinition} from '../lib/report-contract.ts';
const journal:ReportDefinition={code:'journal-lines',title:'Journals',basis:'Business date',columns:[],requiresDateRange:true,permission:'finance:read'};
test('new dated reports retain date filters and exact paging instead of five hardcoded report names',()=>{
 assert.deepEqual(Object.fromEntries(reportQuery(journal,{from:'2026-01-01',to:'2026-01-31',offset:'200'},'2026-02-01')),{from:'2026-01-01',to:'2026-01-31',offset:'200',limit:'100'});
 assert.equal(reportQuery(journal,{},'2026-02-01').get('from'),'2026-02-01');
 assert.equal(reportQuery({...journal,requiresDateRange:false},{from:'2026-01-01',offset:'-1'},'2026-02-01').has('from'),false);
});
test('export resolves any actual catalog entry while preserving only supported filters',()=>{
 const result=reportExportTarget('journal-lines',[journal],new URLSearchParams('from=2026-01-01&to=2026-01-31&offset=200&limit=100&tenant=foreign&url=https://evil.test'),'https://java.example');
 assert.equal(result?.href,'https://java.example/api/v1/reports/journal-lines/export?from=2026-01-01&to=2026-01-31&offset=200&limit=100');
});
test('unknown reports and arbitrary targets stay denied even if malicious catalog data is supplied',()=>{
 assert.equal(reportExportTarget('unknown',[journal],new URLSearchParams(),'https://java.example'),null);
 for(const code of ['../admin','//evil.test','https://evil.test','journal-lines/export','%2e%2e','x\r\nInjected: yes'])assert.equal(reportExportTarget(code,[{...journal,code}],new URLSearchParams(),'https://java.example'),null);
});
