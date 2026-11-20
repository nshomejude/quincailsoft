import assert from 'node:assert/strict';
import {existsSync, readFileSync, writeFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';

const root = new URL('../', import.meta.url);
const file = path => new URL(path, root);
const exists = path => existsSync(fileURLToPath(file(path)));
const read = path => readFileSync(file(path), 'utf8');
const load = path => JSON.parse(read(path));
const coveragePath = 'docs/migration/canonical-migration-coverage.json';
const parityPath = 'docs/migration/python-java-parity.json';
const {records} = load(coveragePath);
const {requirements} = load(parityPath);
const kind = name => records.filter(row => row.kind===name);
const byScreen = (kindName,id) => kind(kindName).filter(row=>row.canonical.screenId===id).map(row=>row.id);
const backendOwner = path => path.match(/^backend\/(?:business-modules\/)?([^/]+)\/src\//)?.[1];
const write = (path, data) => writeFileSync(file(path),JSON.stringify(data,null,2)+'\n');
const screenshotDirectory = 'docs/migration-evidence/issue-3-preview/';
const responsiveEvidenceCommit = '09f093493506e3008b5b8884f1043324776e68c2';
const salesEvidenceCommit = 'f35d258dfc9a6a7d1618f6c59e1954d9bc866c35';
const narrowAcceptance = {
    M03: [{test:'IdentityWorkerPostgresTest', commit:salesEvidenceCommit, status:'PASS', scope:'Separate approval, epoch revocation and fenced replay on PostgreSQL 18.7'}],
    M17: [{test:'PostgresSaleIntegrationTest', commit:salesEvidenceCommit, status:'PASS', scope:'Scoped exact posted amount, drafts excluded and custody/rollback on PostgreSQL 18.7'},
          {test:'FullApplicationPostgresTest', commit:salesEvidenceCommit, status:'PASS', scope:'Signed actor can read a date total; anonymous request denied'}],
    M26: [{test:'ReportingPostgresTest', commit:'831d2dfbeeaffa0df5d39e18433f0e6f7a1a10ae', status:'PASS', scope:'Scoped stock report, existing capability denials and exact CSV totals on PostgreSQL 18.7'}],
};
const reviewed = {
    S01: {route:'/', component:'apps/web-admin/app/page.tsx', endpoints:['GET /api/v1/session','GET /api/v1/sales','GET /api/v1/sales/summary?from=&to='], screenshot:'live-dashboard.jpg', gaps:['Day, week and month posted gross sales are wired to the scoped Java summary. Low-stock aggregate and order/collection queues remain unaccepted.','Open POS and priority drilldowns need canonical workflow comparison.']},
    S11: {route:'/catalog/[id]', component:'apps/web-admin/app/catalog/[id]/page.tsx', endpoints:['GET /api/v1/products/{id}','PUT /api/v1/products/{id}','GET /api/v1/catalog/categories'], screenshot:'live-product-detail.jpg', gaps:['History, product-scoped stock card, and label printing are pending.','Manufacturer, model, family and variant identity are pending.']},
};
const endpoints = sourcePaths => sourcePaths.filter(path=>path.endsWith('Controller.java')&&exists(path)).map(path=>({
    source:path, controllerPath:read(path).match(/@RequestMapping\(\s*"([^"]+)"/)?.[1]??null,
}));
const screens = kind('screen').map(row=>{
    const source = row.canonical;
    const sample = reviewed[row.id];
    const candidate = sample?.route??row.implementedRouteCandidate??null;
    const webComponent = sample?.component??(candidate?'apps/web-admin/app'+(candidate==='/'?'':candidate)+'/page.tsx':null);
    const originalPage = 'pages/'+row.id+'.html';
    return {
        id:row.id, title:row.title, original:{canonicalRoute:source.route, source:exists(originalPage)?originalPage:'ARCHITECTURE.md', contract:source.contracts, prototypeRenderers:['app.js','v3/ui.js','v4/shell.js']},
        next:{routeCandidate:candidate, component:webComponent&&exists(webComponent)?webComponent:null, mappingReviewed:!!sample},
        required:{tabs:source.tabs??[], actions:source.actions??[], fieldIds:byScreen('screen_field',row.id), dialogAcceptance:'NOT_RUN', details:source.kind==='detail'},
        roleBranchScope:sample?'Signed-in merchant with source capability and current branch membership. Complete denial matrix pending.':'NOT_RUN per-screen capability and branch review',
        java:{reviewedEndpoints:sample?.endpoints??[], controllerCandidates:endpoints(row.evidence)},
        gaps:sample?.gaps??[row.reason,'Compare actionable fields, dialogs and permissions against original workflow.'],
        captures:{
            original:row.id==='S01'?['previews/QuincaiSoft_Dashboard_v4.jpg','app.js']:[],
            desktop:sample?[{path:screenshotDirectory+sample.screenshot,commit:'831d2df',note:'Initial seeded preview capture'}, ...(row.id==='S01'?[{path:screenshotDirectory+'live-dashboard-sales-summary.jpg',commit:salesEvidenceCommit,note:'Posted sales amount verified as owner on the small preview seed'}, {path:screenshotDirectory+'live-dashboard-month.jpg',commit:'0dd97db07cafd5c14050fefb2de36469e80f01ee',note:'Owner verified month range and the Java result'}]:[])]:[],
            narrow:sample?[{path:screenshotDirectory+(row.id==='S01'?'narrow-dashboard.jpg':'narrow-product-detail.jpg'),commit:responsiveEvidenceCommit,viewportWidth:390}]:[], comparableBeforeAfter:false,
        },
        status:sample?'FAIL':'NOT_RUN',
    };
});

assert.equal(screens.length,152);
assert.equal(new Set(screens.map(screen=>screen.id)).size,screens.length);
write('docs/migration/screen-fidelity-register.json',{
    schemaVersion:1, source:coveragePath, evidenceCommit:'831d2dfbeeaffa0df5d39e18433f0e6f7a1a10ae',
    method:'Canonical coverage supplies candidate mappings. Candidates and observed preview captures do not certify visual, behavioral or role parity.',
    loginComparison:{original:'previews/QuincaiSoft_Login_v4.jpg', capture:screenshotDirectory+'live-login.jpg', narrow:{path:screenshotDirectory+'narrow-login.jpg',commit:responsiveEvidenceCommit,viewportWidth:390}, status:'NOT_RUN', remaining:'Matched original comparison and browser role-switch matrix.'},
    screens,
});

const modules=kind('module').map(row=>{
    const owners=new Set(row.evidence.map(backendOwner).filter(Boolean));
    const owned = paths => paths.some(path=>owners.has(backendOwner(path)));
    const screenRows=kind('screen').filter(screen=>owned(screen.evidence)
        || (row.id==='M06'&&screen.id==='S11') || (row.id==='M26'&&screen.id==='S01'));
    const screenIds=new Set(screenRows.map(screen=>screen.id));
    const isReport=row.id==='M26';
    const isDocument=row.id==='M24';
    const reports=kind('report').filter(report=>isReport||screenIds.has(report.canonical.target)).map(report=>report.id);
    const templates=kind('document').filter(template=>isDocument||owned(template.evidence)).map(template=>template.id);
    const candidates=requirements.filter(r=>owned(r.candidateJavaSources??[]));
    const controllers=endpoints(row.evidence);
    const permissions=[...new Set(controllers.flatMap(controller=>[...read(controller.source).matchAll(/'(?:SCOPE_)?([a-z]+(?::[a-z-]+)+)'/g)].map(match=>match[1])))].sort();
    return {
        id:row.id, title:row.title, canonicalWorkflowsAndInvariants:row.canonical.contracts,
        candidateScreens:[...screenIds], candidateDetailPages:screenRows.filter(screen=>screen.canonical.kind==='detail').map(screen=>screen.id),
        fieldIds:kind('screen_field').filter(field=>screenIds.has(field.canonical.screenId)).map(field=>field.id),
        candidateReports:reports, candidateTemplates:templates,
        apiEvidence:controllers, permissionCandidates:permissions,
        legacyParityCandidates:candidates.map(candidate=>candidate.id),
        integrationTestCandidates:[...new Set(candidates.flatMap(candidate=>candidate.candidateJavaTests??[]))].sort(),
        executedAcceptance:narrowAcceptance[row.id]??[], remaining:[row.reason,'Verify canonical workflows and fields including scoped denials, rollback, replay, invariants and actual browser behavior.'],
        status:row.id==='M29'||row.id==='M30'?'BLOCKED':'NOT_RUN',
    };
});
assert.equal(modules.length,40);
assert.equal(new Set(modules.map(module=>module.id)).size,modules.length);
write('docs/migration/module-acceptance-register.json',{
    schemaVersion:1, coverage:coveragePath, parity:parityPath,
    method:'Module, field, workflow and test candidates are traceability only. Missing relationships require review; no automated acceptance.',
    modules,
});
console.log(`Recorded ${screens.length} canonical screens and ${modules.length} canonical modules without accepting untested rows.`);
