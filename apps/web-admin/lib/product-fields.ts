import type {Field} from '@/components/CommandForm';
import type {Product} from './api';
import type {Category} from './catalog-fields';

export function productFields(categories: Category[], product?: Product): Field[] {
 const activeCategories = categories.filter(category => category.active);
 return [
  {name:'sku',label:'SKU',maxLength:80,value:product?.sku},
  {name:'name',label:'Product name',maxLength:240,value:product?.name},
  ...(product ? [] : [{name:'baseUnit',label:'Base unit',maxLength:32,value:'piece'}]),
  {name:'sellingPriceXaf',label:'Selling price (FCFA)',type:'money',value:product?.sellingPriceXaf},
  {name:'taxBasisPoints',label:'Tax (basis points)',type:'number',maximum:'10000',value:String(product?.taxBasisPoints??0)},
  {
   name:'categoryId',label:'Category',type:'select',required:false,emptyAsNull:true,
   value:activeCategories.find(category=>category.id===product?.categoryId)?.id??'',
   hint:product?.categoryId?'Leave blank to retain the current category.':undefined,
   options:activeCategories.map(category=>({value:category.id,label:category.code+' · '+category.name}))
  },
  {name:'brand',label:'Brand',maxLength:120,required:false,value:product?.brand},
  {name:'barcode',label:'Barcode',maxLength:120,required:false,value:product?.barcode},
  {name:'packSizeMilli',label:'Pack size (milli units)',type:'integer',minimum:'1',value:product?.packSizeMilli??'1000'},
  {name:'reorderLevelMilli',label:'Reorder level (milli units)',type:'integer',value:product?.reorderLevelMilli??'0'},
  {name:'description',label:'Description',type:'textarea',maxLength:4000,required:false,value:product?.description},
  ...(product ? [] : [
   {name:'trackStock',label:'Track stock',type:'checkbox' as const,value:true},
   {name:'trackSerial',label:'Track serial numbers',type:'checkbox' as const,value:false},
   {name:'trackExpiry',label:'Track expiry',type:'checkbox' as const,value:false}
  ]),
  {name:'active',label:'Active',type:'checkbox',value:product?.active??true},
  {name:'reason',label:'Reason',type:'textarea'}
 ];
}
