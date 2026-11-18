import assert from 'node:assert/strict';
import {createReadStream} from 'node:fs';
import {readFile, writeFile} from 'node:fs/promises';
import {resolve} from 'node:path';
import {createInterface} from 'node:readline';

const directory = resolve(process.argv[2] ?? 'datasets/business-history/generated');
const readJson = async (path) => JSON.parse(await readFile(path, 'utf8'));
const published = await readJson(new URL('./evidence/manifest.json', import.meta.url));
const generated = await readJson(resolve(directory, 'manifest.json'));
assert.deepEqual(generated, published, 'Generated manifest differs from the reviewed fixture');

const blockers = {
    companies: 'Isolated PostgreSQL 18 and synthetic tenant provisioning',
    branches: 'Isolated company, six scoped branches and warehouses',
    customers: 'Authenticated scoped Java import and source-to-target IDs',
    suppliers: 'Authenticated scoped Java import and source-to-target IDs',
    categories: 'Authenticated scoped Java catalog import and source-to-target IDs',
    units: 'Authenticated scoped Java catalog import and source-to-target IDs',
    manufacturers: 'Typed scoped manufacturer identity and catalog API',
    products: 'Typed manufacturer/model/family identity and source-to-target IDs',
    variants: 'Typed family/variant mapping retaining colour, size, and independent manufacturer',
    supplierCatalog: 'Typed variant-to-supplier offer workflow',
    employees: 'Typed personnel workflow with a scoped employee identity',
    employmentContracts: 'Typed personnel and contract workflow',
    attendance: 'Typed attendance workflow',
    payslips: 'Typed nonstatutory payroll workflow',
    priceLists: 'Approved historical price policy workflow',
    financialPeriods: 'Scoped period opening and independent close approval',
    taxPolicies: 'Scoped synthetic tax policy and checked posting setup',
    events: 'Chronological typed command replay with scoped actors and durable source identity',
    purchaseOrders: 'Scoped purchase workflow linked to original receipt commands',
    quotations: 'Scoped quotation workflow and stable source mapping',
};
const results = [];
for (const {file, rows} of generated.files) {
    const kind = file.replace(/\.ndjson$/, '');
    const blocker = blockers[kind];
    if (!blocker) {
        results.push({file, rows, status:'REFERENCE_OR_ACCEPTANCE_ONLY'});
        continue;
    }
    const entries = [];
    for await (const line of createInterface({input:createReadStream(resolve(directory,file)), crlfDelay:Infinity})) {
        if (!line) continue;
        const row = JSON.parse(line);
        assert.equal(typeof row.id, 'string', `Missing source ID in ${file}`);
        entries.push({id:row.id, ...(kind==='events'?{type:row.type, sequence:row.sequence}:{})});
    }
    assert.equal(entries.length, rows, `Unexpected row count in ${file}`);
    results.push({file, rows, imported:0, rejected:0, status:'BLOCKED_PRE_IMPORT', blocker, entries});
}
const report = {dataset:generated.datasetId, databaseTouched:false, results};
await writeFile(resolve(directory,'java-import-preflight.json'), JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(results.map(({entries:_, ...summary})=>summary),null,2));
