import assert from 'node:assert/strict';
import {test} from 'node:test';
import {dashboardDate, dashboardPeriod, dashboardRange} from '../lib/dashboard-period.ts';

test('dashboard filters reject invalid dates and unknown periods', () => {
    assert.equal(dashboardDate('2025-02-29','2026-11-18'),'2026-11-18');
    assert.equal(dashboardDate('2024-02-29','2026-11-18'),'2024-02-29');
    assert.equal(dashboardPeriod('bad'),'today');
});

test('dashboard periods include the full calendar week or month', () => {
    assert.deepEqual(dashboardRange('2026-01-01','week'), {from:'2025-12-29',to:'2026-01-04'});
    assert.deepEqual(dashboardRange('2024-02-13','month'), {from:'2024-02-01',to:'2024-02-29'});
    assert.deepEqual(dashboardRange('2026-11-18','today'), {from:'2026-11-18',to:'2026-11-18'});
});
