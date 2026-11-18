import {api,Product,Me,permitted} from '@/lib/api';
import {Access} from '@/components/Access';
import {CommandForm} from '@/components/CommandForm';
import {PageTitle,Records} from '@/components/Records';
import type {Category} from '@/lib/catalog-fields';
import {productFields} from '@/lib/product-fields';

export const dynamic='force-dynamic';
export default async function ProductDetail({params}:{params:Promise<{id:string}>}) {
 try {
  const {id}=await params;
  const [product,me,categories]=await Promise.all([
   api<Product>('products/'+encodeURIComponent(id)),api<Me>('me'),api<Category[]>('catalog/categories')
  ]);
  const category=categories.find(item=>item.id===product.categoryId);
  return <>
   <PageTitle title={product.name} description={product.sku}/>
   <section className="panel">
    <Records rows={[{...product,categoryName:category?.name??product.categoryId}]} columns={[
     {key:'baseUnit',label:'Base unit'},
     {key:'categoryName',label:'Category'},
     {key:'brand',label:'Brand'},
     {key:'barcode',label:'Barcode'},
     {key:'sellingPriceXaf',label:'Selling price (FCFA)',money:true},
     {key:'taxBasisPoints',label:'Tax (basis points)'},
     {key:'packSizeMilli',label:'Pack size (milli units)'},
     {key:'reorderLevelMilli',label:'Reorder level (milli units)'},
     {key:'trackStock',label:'Track stock'},
     {key:'trackSerial',label:'Track serial numbers'},
     {key:'trackExpiry',label:'Track expiry'},
     {key:'description',label:'Description'},
     {key:'active',label:'Active'}
    ]}/>
   </section>
   {permitted(me,'catalog:write')&&<section className="panel">
    <CommandForm path={'products/'+id} method="PUT" title="Save product"
     fixed={{version:product.version,baseUnit:product.baseUnit}} fields={productFields(categories,product)}/>
   </section>}
  </>;
 } catch(error) {
  return <Access error={error}/>;
 }
}
