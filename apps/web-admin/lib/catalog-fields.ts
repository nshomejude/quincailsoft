import type {Field} from '@/components/CommandForm';export type Category={id:string;code:string;name:string;parentId:string|null;brand:string;description:string;active:boolean;version:string};export type Unit={id:string;code:string;name:string;baseUnit:string;factorNumerator:string;factorDenominator:string;precision:number;active:boolean;version:string};
export function categoryFields(categories:Category[],c?:Category):Field[]{return [{name:'code',label:'Code',value:c?.code},{name:'name',label:'Name',value:c?.name},{name:'parentId',label:'Parent category',type:'select',required:false,emptyAsNull:true,value:c?.parentId??'',options:categories.filter(x=>x.id!==c?.id).map(x=>({value:x.id,label:x.code+' · '+x.name}))},{name:'brand',label:'Brand',required:false,value:c?.brand},{name:'description',label:'Description',type:'textarea',required:false,value:c?.description},{name:'active',label:'Active',type:'checkbox',value:c?.active??true},{name:'reason',label:'Reason',type:'textarea'}]}
export function unitFields(units: Unit[], unit?: Unit): Field[] {
 const identity: Field[] = unit ? [] : [
  {name:'code',label:'Code'},
  {
   name:'baseUnit',label:'Base unit',type:'select',required:false,emptyFallbackField:'code',
   hint:'Leave blank to create a base unit with a 1:1 conversion.',
   options:units.filter(u=>u.active&&u.code===u.baseUnit&&u.factorNumerator==='1'&&u.factorDenominator==='1')
    .map(u=>({value:u.code,label:u.code+' · '+u.name}))
  },
  {name:'factorNumerator',label:'Conversion numerator',type:'integer',minimum:'1',value:'1'},
  {name:'factorDenominator',label:'Conversion denominator',type:'integer',minimum:'1',value:'1'},
  {name:'precision',label:'Decimal precision',type:'number',maximum:'6',value:'0'}
 ];
 return [...identity,{name:'name',label:'Name',value:unit?.name},
  {name:'active',label:'Active',type:'checkbox',value:unit?.active??true},
  {name:'reason',label:'Reason',type:'textarea'}];
}
export function fixedUnitScale({code,baseUnit,factorNumerator,factorDenominator,precision,version}: Unit) {
 return {code,baseUnit,factorNumerator,factorDenominator,precision,version};
}
