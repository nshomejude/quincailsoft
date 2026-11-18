import {api,Me,permitted,ApiError} from '@/lib/api';
import {Access} from '@/components/Access';
import {CommandForm} from '@/components/CommandForm';
import {PageTitle} from '@/components/Records';
import type {Category} from '@/lib/catalog-fields';
import {productFields} from '@/lib/product-fields';

export const dynamic='force-dynamic';
export default async function NewProduct() {
 try {
  const me=await api<Me>('me');
  if(!permitted(me,'catalog:write'))throw new ApiError(403);
  const categories=await api<Category[]>('catalog/categories');
  return <>
   <PageTitle title="New product"/>
   <section className="panel">
    <CommandForm path="products" redirectBase="/catalog" fields={productFields(categories)}/>
   </section>
  </>;
 } catch(error) {
  return <Access error={error}/>;
 }
}
