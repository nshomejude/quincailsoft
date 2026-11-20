import Link from 'next/link';
import {api,Me,permitted,pagedProducts} from '@/lib/api';
import {Access} from '@/components/Access';
import {PageTitle,Records} from '@/components/Records';

export const dynamic='force-dynamic';
type Params={q?:string;p?:string};
export default async function Catalogue({searchParams}:{searchParams:Promise<Params>}) {
 try {
  const params=await searchParams;
  const q=typeof params.q==='string'?params.q.trim().slice(0,120):'';
  const requested=Number(params.p??0);
  const offset=Number.isSafeInteger(requested)&&requested>=0&&requested<=1000000?requested:0;
  const [page,me]=await Promise.all([pagedProducts(q,offset,50),api<Me>('me')]);
  const next=page.offset+page.items.length<page.total;
  const previous=Math.max(0,page.offset-page.limit);
  const url=(p:number)=>'/catalog?q='+encodeURIComponent(q)+'&p='+p;
  return <>
    <PageTitle title="Products">
      <Link className="secondary" href="/catalog/categories">Categories</Link>
      <Link className="secondary" href="/catalog/units">Units of measure</Link>
      {permitted(me,'catalog:write')&&<Link className="button" href="/catalog/new">New product</Link>}
    </PageTitle>
    <section className="panel">
      <form method="get" role="search" className="field-row">
        <label htmlFor="catalog-search">Search by SKU, product, brand or barcode</label>
        <input id="catalog-search" name="q" type="search" defaultValue={q} maxLength={120}
          autoComplete="off" placeholder="Search products"/>
        <button type="submit">Search</button>
      </form>
      <p aria-live="polite">{page.total} matching products</p>
      <Records rows={page.items} href="/catalog" columns={[
        {key:'name',label:'Product'},{key:'sku',label:'SKU'},
        {key:'baseUnit',label:'Base unit'},
        {key:'sellingPriceXaf',label:'Selling price (FCFA)',money:true},
        {key:'active',label:'Active'}]}/>
      <nav aria-label="Product pages" className="field-row">
        {page.offset>0&&<Link className="secondary" href={url(previous)}>Previous</Link>}
        <span>{page.total===0?0:page.offset+1}–{Math.min(page.total,page.offset+page.items.length)} of {page.total}</span>
        {next&&<Link className="secondary" href={url(page.offset+page.limit)}>Next</Link>}
      </nav>
    </section>
  </>;
 } catch(error) {return <Access error={error}/>;}
}
