import test from 'node:test';
import assert from 'node:assert/strict';
import {dispatchLines} from '../lib/fulfillment-input.ts';

test('dispatch preserves exact quantities and leaves zero items for a later shipment', () => {
    assert.deepEqual(dispatchLines([
        {productId: 'cable', quantityMilli: '2500'}, {productId: 'pipe', quantityMilli: '4000'},
    ], {cable: '1.125', pipe: '0'}), [{productId: 'cable', quantityMilli: '1125'}]);
    assert.deepEqual(dispatchLines([{productId: 'bulk', quantityMilli: '9223372036854775807'}],
        {bulk: '9223372036854775.806'}), [{productId: 'bulk', quantityMilli: '9223372036854775806'}]);
});

test('dispatch rejects overshipment, unsupported precision and empty shipment', () => {
    const remaining = [{productId: 'cable', quantityMilli: '2500'}];
    for (const cable of ['2.501', '1.0001', '-1', '1e3', 'NaN'])
        assert.throws(() => dispatchLines(remaining, {cable}));
    assert.throws(() => dispatchLines(remaining, {cable: '0.000'}));
    assert.throws(() => dispatchLines(remaining, {}));
});

test('dispatch forwards only products from the current remaining picklist', () => {
    assert.deepEqual(dispatchLines([{productId: 'cable', quantityMilli: '2500'}],
        {cable: '1', staleProduct: '3'}), [{productId: 'cable', quantityMilli: '1000'}]);
});
