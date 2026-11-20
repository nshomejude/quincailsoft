import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.argv[2] || 'backend');
let suites = 0;
let tests = 0;
let failures = 0;
let errors = 0;
let skipped = 0;

function scan(directory) {
  for (const item of fs.readdirSync(directory, { withFileTypes: true })) {
    if (item.name === '.git' || item.name === 'node_modules') continue;
    const file = path.join(directory, item.name);
    if (item.isDirectory()) {
      scan(file);
    } else if (item.name.startsWith('TEST-') && item.name.endsWith('.xml')
      && file.includes(`${path.sep}target${path.sep}surefire-reports${path.sep}`)) {
      const source = fs.readFileSync(file, 'utf8');
      const header = source.match(/<testsuite\b[^>]*>/);
      if (!header) throw new Error(`Missing JUnit testsuite element: ${file}`);
      const metric = (key) => {
        const found = header[0].match(new RegExp(`\\b${key}="(\\d+)"`));
        if (!found) throw new Error(`Missing testsuite ${key} metric in ${file}`);
        return Number(found[1]);
      };
      suites++;
      tests += metric('tests');
      failures += metric('failures');
      errors += metric('errors');
      skipped += metric('skipped');
    }
  }
}

if (!fs.existsSync(root)) throw new Error(`Backend root not found: ${root}`);
scan(root);
console.log(JSON.stringify({ suites, tests, failures, errors, skipped }));
if (suites === 0 || tests === 0 || failures || errors || skipped) {
  throw new Error('Java backend test evidence is missing, failing or skipped; release gate stays closed.');
}
