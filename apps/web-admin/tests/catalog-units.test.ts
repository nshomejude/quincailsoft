import test from 'node:test';
import assert from 'node:assert/strict';
import {fixedUnitScale, unitFields, type Unit} from '../lib/catalog-fields.ts';
import {commandValues} from '../lib/command-input.ts';

const metre: Unit = {
 id:'metre-id',code:'m',name:'Metre',baseUnit:'m',factorNumerator:'1',
 factorDenominator:'1',precision:3,active:true,version:'2'
};
const centimetre: Unit = {
 ...metre,id:'centimetre-id',code:'cm',name:'Centimetre',factorDenominator:'100'
};

test('a blank base selection sends the new code as the base unit', () => {
 const fields = unitFields([]);
 const payload = commandValues(fields, {
  code:'piece',name:'Piece',baseUnit:'',factorNumerator:'1',
  factorDenominator:'1',precision:'0',active:true,reason:'New inventory scale'
 });
 assert.equal(payload.baseUnit,'piece');
 assert.equal(payload.precision,0);
});

test('a converted unit can select only active base units', () => {
 const fields = unitFields([metre,centimetre,{...metre,code:'kg',baseUnit:'kg',active:false}]);
 const base = fields.find(field => field.name==='baseUnit');
 assert.deepEqual(base?.options?.map(option => option.value),['m']);
 const payload = commandValues(fields, {
  code:'cm',name:'Centimetre',baseUnit:'m',factorNumerator:'1',
  factorDenominator:'100',precision:'3',active:true,reason:'Metric inventory'
 });
 assert.equal(payload.baseUnit,'m');
 assert.equal(payload.factorDenominator,'100');
});

test('editing a unit preserves its identity, scale and version', () => {
 const fields = unitFields([],centimetre);
 assert.deepEqual(commandValues(fields,{
  name:'Centimeter',active:false,reason:'Retire spelling',baseUnit:'piece',precision:'0'
 },fixedUnitScale(centimetre)),{
  code:'cm',baseUnit:'m',factorNumerator:'1',factorDenominator:'100',precision:3,version:'2',
  name:'Centimeter',active:false,reason:'Retire spelling'
 });
});
