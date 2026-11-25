import {api, ApiError, Me, permitted, Product, xaf} from '@/lib/api';
import {ListingView, StoreView} from '@/lib/domains';
import {locale, messages} from '@/lib/locale';
import {Access} from '@/components/Access';
import {CommandForm} from '@/components/CommandForm';
import {PageTitle} from '@/components/Records';

export const dynamic = 'force-dynamic';

export default async function Commerce() {
  try {
    const t = messages[await locale()];
    const me = await api<Me>('me');
    if (!permitted(me, 'commerce:manage')) throw new ApiError(403);

    const [store, listings, products] = await Promise.all([
      api<StoreView>('commerce/store'),
      api<ListingView[]>('commerce/listings'),
      permitted(me, 'catalog:read') ? api<Product[]>('products') : Promise.resolve([] as Product[]),
    ]);

    // The service returns an empty store with version 0 until the storefront is first configured.
    const configured = Boolean(store.id);
    const listingByProduct = new Map(listings.map(listing => [listing.productId, listing]));

    return (
      <>
        <PageTitle
          title={t.commerce}
          description="Publish your storefront and choose which products customers can see online."
        />

        <section className="panel">
          <h2>Storefront</h2>
          <dl>
            <div><dt>Status</dt><dd>{configured ? (store.enabled ? 'Published' : 'Not published') : 'Not configured'}</dd></div>
            <div><dt>Address name</dt><dd>{store.slug || '—'}</dd></div>
            <div><dt>Display name</dt><dd>{store.displayName || '—'}</dd></div>
            <div><dt>Listed products</dt><dd>{listings.filter(listing => listing.published).length}</dd></div>
          </dl>
          <h3>{configured ? 'Update storefront' : 'Set up storefront'}</h3>
          <CommandForm
            path="commerce/store"
            title={configured ? 'Save storefront' : 'Create storefront'}
            fixed={{version: Number(store.version)}}
            fields={[
              {name: 'slug', label: 'Address name (3–63 lowercase letters, digits or hyphens)', value: store.slug},
              {name: 'displayName', label: 'Store display name', value: store.displayName},
              {name: 'enabled', label: 'Publish storefront to customers', type: 'checkbox', value: store.enabled},
              {name: 'reason', label: 'Reason for this change', type: 'textarea'},
            ]}
          />
          <p className="muted">
            Publishing requires the online-store entitlement on your plan; the service rejects the
            request otherwise.
          </p>
        </section>

        <section className="panel">
          <h2>Product listings</h2>
          {!configured ? (
            <p className="notice">Set up the storefront above before listing products.</p>
          ) : products.length === 0 ? (
            <p>No products are available to list. Add products to the catalogue first.</p>
          ) : (
            <>
              <p className="muted">
                Only active products with no tax rate can be listed online until a gross-price
                policy is configured.
              </p>
              {products.map(product => {
                const listing = listingByProduct.get(product.id);
                return (
                  <details key={product.id}>
                    <summary>
                      {product.sku} · {product.name} · {xaf(product.sellingPriceXaf)}{' '}
                      <span className="badge">{listing?.published ? 'Listed' : 'Not listed'}</span>
                    </summary>
                    <CommandForm
                      path="commerce/listings"
                      title={listing?.published ? 'Update or unlist' : 'Save listing'}
                      fixed={{productId: product.id, version: Number(listing?.version ?? 0)}}
                      fields={[
                        {name: 'title', label: 'Online title', value: listing?.title ?? product.name},
                        {name: 'description', label: 'Online description', type: 'textarea', required: false, value: listing?.description ?? ''},
                        {name: 'published', label: 'Show this product in the online store', type: 'checkbox', value: listing?.published ?? false},
                        {name: 'reason', label: 'Reason for this change', type: 'textarea'},
                      ]}
                    />
                  </details>
                );
              })}
            </>
          )}
        </section>
      </>
    );
  } catch (error) {
    return <Access error={error} />;
  }
}
