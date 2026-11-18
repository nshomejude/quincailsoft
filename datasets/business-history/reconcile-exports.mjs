import assert from 'node:assert/strict';
import {createHash} from 'node:crypto';
import {readFile, realpath} from 'node:fs/promises';
import {resolve} from 'node:path';
import {pathToFileURL} from 'node:url';

// Offline evidence tooling only. Never connects to or modifies a database.
export const keys = Object.freeze({
  stockBalances: ['branchId','warehouseId','variantId'],
  customerOpenItems: ['branchId','saleId'],
  supplierOpenItems: ['branchId','invoiceId'],
  trialBalance: ['branchId','account'],
  monthlyAnalytics: ['branchId','month'],
});
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const integer = /^(?:0|-[1-9]\d*|[1-9]\d*)$/;
function index(kind, rows) {
  assert.ok(Object.hasOwn(keys, kind), 'Unknown reconciliation kind');
  assert.ok(Array.isArray(rows) && rows.length > 0, `${kind}: empty or invalid export`);
  const result = new Map();
  for (const row of rows) {
    assert.ok(row && typeof row === 'object' && !Array.isArray(row), `${kind}: invalid row`);
    for (const key of keys[kind]) assert.ok(typeof row[key] === 'string' && row[key].trim(), `${kind}: missing key ${key}`);
    for (const [field,value] of Object.entries(row)) if (/(?:Xaf|Milli)$/.test(field)) {
      assert.ok(typeof value === 'string' && integer.test(value), `${kind}: ${field} must be an exact canonical integer string`);
    }
    const id = JSON.stringify(keys[kind].map(k => row[k]));
    assert.ok(!result.has(id), `${kind}: duplicate source key ${id}`);
    result.set(id, row);
  }
  return result;
}
export function compareRows(kind, reference, actual) {
  const expected = index(kind, reference), observed = index(kind, actual);
  assert.equal(observed.size, expected.size, `${kind}: row count mismatch`);
  for (const [id,row] of expected) {
    assert.ok(observed.has(id), `${kind}: missing source key ${id}`);
    // Compare every field, including parties, dates, zero balances and negative profits.
    assert.deepEqual(observed.get(id), row, `${kind}: value or field mismatch for ${id}`);
  }
  return expected.size;
}
async function readRows(directory, kind) {
  const bytes = await readFile(resolve(directory, kind + '.ndjson'));
  const lines = bytes.toString('utf8').split(/\r?\n/).filter(line => line.trim());
  return {rows:lines.map(line => JSON.parse(line)), sha256:hash(bytes), bytes:bytes.length};
}
async function provenance(directory, phase) {
  const p = JSON.parse(await readFile(resolve(directory, 'export-provenance.json'), 'utf8'));
  assert.equal(p.schemaVersion, 1, 'Export provenance schemaVersion must be 1');
  assert.equal(p.phase, phase, 'Export phase mismatch');
  assert.match(p.sourceCommit ?? '', /^[a-f0-9]{40}$/, 'Full source commit required');
  for (const field of ['datasetId','mappingVersion','runId','tenantId','companyId','command']) {
    assert.ok(typeof p[field] === 'string' && p[field].trim(), `Missing provenance ${field}`);
  }
  return p;
}
export async function reconcile(referenceDirectory, initialDirectory, replayDirectory) {
  const dirs = await Promise.all([referenceDirectory,initialDirectory,replayDirectory].map(p => realpath(p)));
  assert.equal(new Set(dirs).size, 3, 'Reference, initial and replay directories must be distinct');
  const published = JSON.parse(await readFile(new URL('./evidence/manifest.json', import.meta.url), 'utf8'));
  const generated = JSON.parse(await readFile(resolve(dirs[0], 'manifest.json'), 'utf8'));
  assert.deepEqual(generated, published, 'Reference manifest differs from reviewed dataset');
  const initial = await provenance(dirs[1], 'initial'), replay = await provenance(dirs[2], 'replay');
  assert.equal(initial.datasetId, published.datasetId, 'Wrong initial dataset');
  for (const field of ['datasetId','mappingVersion','tenantId','companyId','sourceCommit']) {
    assert.equal(replay[field], initial[field], `Replay changed ${field}`);
  }
  assert.notEqual(initial.runId, replay.runId, 'Separate import and replay run IDs required');
  const results = [];
  for (const kind of Object.keys(keys)) {
    const reference = await readRows(dirs[0], kind);
    const manifest = published.files.find(f => f.file === kind + '.ndjson');
    assert.ok(manifest, `Missing manifest entry for ${kind}`);
    assert.equal(reference.sha256, manifest.sha256, `Reference checksum mismatch: ${kind}`);
    assert.equal(reference.rows.length, manifest.rows, `Reference count mismatch: ${kind}`);
    const first = await readRows(dirs[1], kind), second = await readRows(dirs[2], kind);
    const rows = compareRows(kind, reference.rows, first.rows);
    compareRows(kind, reference.rows, second.rows);
    results.push({kind, rows, referenceSha256:reference.sha256, initialSha256:first.sha256, replaySha256:second.sha256});
  }
  return {status:'MATCHED_SUPPLIED_EXPORTS_NOT_INGESTION_CERTIFICATION', initial, replay, results,
    javaExecutionVerified:false, releaseCertified:false};
}
if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  try {
    assert.equal(process.argv.length, 5, 'Usage: node reconcile-exports.mjs REFERENCE INITIAL_EXPORT REPLAY_EXPORT');
    console.log(JSON.stringify(await reconcile(...process.argv.slice(2)), null, 2));
  } catch (error) {
    console.error(JSON.stringify({status:'FAIL_EXPORT_RECONCILIATION', error:error.message}));
    process.exitCode = 1;
  }
}
