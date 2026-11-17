import {products} from '@/lib/api';import {Access} from '@/components/Access';import {SaleForm} from '@/components/SaleForms';
export const dynamic='force-dynamic';
export default async function NewSale(){try{const rows=await products();return <><p className="eyebrow">NEW SALE</p><h1>Create sale draft</h1><section className="panel"><SaleForm products={rows}/></section></>}catch(error){return <Access error={error}/>}}
