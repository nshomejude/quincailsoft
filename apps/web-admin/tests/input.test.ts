import test from 'node:test';import assert from 'node:assert/strict';import {quantityMilli} from '../lib/input.ts';
test('quantity wire format preserves exact precision',()=>{assert.equal(quantityMilli('1.001'),'1001');assert.equal(quantityMilli('9007199254740.993'),'9007199254740993');assert.equal(quantityMilli('9223372036854775.807'),'9223372036854775807')});
test('invalid fractional, exponential, negative and overflow inputs fail',()=>{for(const value of ['0','-1','1e3','1.0001','NaN','Infinity','9223372036854775.808'])assert.throws(()=>quantityMilli(value))});
