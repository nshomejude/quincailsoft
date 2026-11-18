import test from 'node:test';
import assert from 'node:assert/strict';
import {commandValues} from '../lib/command-input.ts';
import {productFields} from '../lib/product-fields.ts';
import type {Product} from '../lib/api.ts';
import type {Category} from '../lib/catalog-fields.ts';

const category: Category = {
 id:'category',code:'TOOLS',name:'Tools',parentId:null,brand:'',description:'',active:true,version:'1'
};
const product: Product = {
 id:'product',sku:'CEM-001',name:'Cement',baseUnit:'piece',sellingPriceXaf:'4850',taxBasisPoints:0,
 categoryId:'inactive',brand:'Cement supplier',barcode:'00042',packSizeMilli:'1000',
 reorderLevelMilli:'40000',trackStock:true,trackSerial:false,trackExpiry:true,description:'50 kg',
 active:true,version:'3'
};

test('creation forwards product metadata with exact pack and reorder values', () => {
 const fields=productFields([category,{...category,id:'inactive',active:false}]);
 assert.deepEqual(fields.find(field=>field.name==='categoryId')?.options?.map(option=>option.value),['category']);
 assert.deepEqual(commandValues(fields,{
  categoryId:'category',brand:'Tools brand',barcode:'00042',packSizeMilli:'1000',
  reorderLevelMilli:'9007199254740992',trackStock:true,trackSerial:true,trackExpiry:false,description:'New item'
 }),{
  categoryId:'category',brand:'Tools brand',barcode:'00042',packSizeMilli:'1000',
  reorderLevelMilli:'9007199254740992',trackStock:true,trackSerial:true,trackExpiry:false,description:'New item'
 });
});

test('editing can retain a retired category and preserves stock tracking and scale', () => {
 const fields=productFields([category],product);
 assert.equal(fields.find(field=>field.name==='categoryId')?.value,'');
 assert.deepEqual(commandValues(fields,{
  categoryId:'',baseUnit:'box',trackStock:false,name:'Cement 50 kg'
 },{baseUnit:product.baseUnit,version:product.version}),{
  baseUnit:'piece',version:'3',categoryId:null,name:'Cement 50 kg'
 });
});
