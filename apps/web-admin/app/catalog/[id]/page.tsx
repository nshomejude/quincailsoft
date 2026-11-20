import Link from 'next/link';
import {api, Product, Me, permitted, xaf} from '@/lib/api';
import {Access} from '@/components/Access';
import {CommandForm} from '@/components/CommandForm';
import {PageTitle} from '@/components/Records';
import type {Category} from '@/lib/catalog-fields';
import {productFields} from '@/lib/product-fields';

export const dynamic = 'force-dynamic';

export default async function ProductDetail({params, searchParams}: {
    params: Promise<{id: string}>;
    searchParams: Promise<{tab?: string}>;
}) {
    try {
        const {id} = await params;
        const [product, me, categories, query] = await Promise.all([
            api<Product>('products/' + encodeURIComponent(id)), api<Me>('me'),
            api<Category[]>('catalog/categories'), searchParams,
        ]);
        const category = categories.find(item => item.id === product.categoryId);
        const path = '/catalog/' + encodeURIComponent(id);
        const tabs = ['Overview', 'Stock', 'Prices', 'History'];
        const current = tabs.find(tab => tab.toLowerCase() === query.tab) ?? 'Overview';
        const editable = permitted(me, 'catalog:write');
        const details = [
            ['SKU', product.sku], ['Category', category?.name], ['Brand', product.brand],
            ['Barcode', product.barcode], ['Base unit', product.baseUnit],
            ['Status', product.active ? 'Active' : 'Inactive'],
        ];
        return <>
            <p className="breadcrumb"><Link href="/catalog">Products</Link> / Product detail</p>
            <PageTitle title={product.name} description={product.sku}>
                {editable && <a className="button" href={path + '#edit-product'}>Edit product</a>}
                {permitted(me, 'inventory:read') && <Link className="secondary" href="/inventory">Open stock</Link>}
            </PageTitle>
            <div className="record-tabs" aria-label="Product sections">{tabs.map(tab => <Link key={tab} className="secondary" href={path + '?tab=' + tab.toLowerCase()} aria-current={tab===current?'page':undefined}>{tab}</Link>)}</div>
            <section className="panel product-summary">
                <h2>{current === 'Overview' ? 'Product information' : current}</h2>
                {current === 'Overview' && <>
                    <dl>{details.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value || '—'}</dd></div>)}</dl>
                    {product.description && <p>{product.description}</p>}
                </>}
                {current === 'Stock' && <dl>
                    <div><dt>Pack size (milli units)</dt><dd>{product.packSizeMilli}</dd></div>
                    <div><dt>Reorder level (milli units)</dt><dd>{product.reorderLevelMilli}</dd></div>
                    <div><dt>Tracking</dt><dd>{[product.trackStock && 'Stock',product.trackSerial && 'Serial numbers',product.trackExpiry && 'Expiry'].filter(Boolean).join(', ') || 'Disabled'}</dd></div>
                </dl>}
                {current === 'Prices' && <dl><div><dt>Selling price</dt><dd>{xaf(product.sellingPriceXaf)}</dd></div><div><dt>Tax (basis points)</dt><dd>{product.taxBasisPoints}</dd></div></dl>}
                {current === 'History' && <p className="muted">Product history is pending Java integration.</p>}
            </section>
            {editable && <details className="panel" id="edit-product" open>
                <summary>Edit product information</summary>
                <CommandForm path={'products/' + id} method="PUT" title="Save product"
                    fixed={{version:product.version,baseUnit:product.baseUnit}} fields={productFields(categories,product)}/>
            </details>}
        </>;
    } catch (error) {
        return <Access error={error}/>;
    }
}
