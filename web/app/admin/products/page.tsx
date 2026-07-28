import { PortalShell } from '@/components/PortalShell';
import { EmptyState } from '@/components/EmptyState';
import { requireAdmin } from '@/lib/portal/authz';
import { listProducts, listCapabilities, type CapabilityRow, type ProductRow } from '@/features/products/product-service';
import { Package, Layers, Sparkles } from 'lucide-react';

type ProductEdition = ProductRow['editions'][number];
type ProductEditionCapability = ProductEdition['capabilities'][number];

const nav = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/products', label: 'Products' },
  { href: '/admin/licenses', label: 'Licenses' },
];

export default async function AdminProductsPage() {
  await requireAdmin();
  const hasDb = !!process.env.DATABASE_URL;
  const [products, capabilities]: [ProductRow[], CapabilityRow[]] = hasDb ? await Promise.all([listProducts(), listCapabilities()]) : [[], []];

  return (
    <PortalShell title="Products, Editions & Capabilities" description="Manage the product catalog. Capabilities use product names only — runtime names (LITERT_LM_PRO, LLAMA_CPP_PRO, GGUF_ACCESS) are forbidden." nav={nav}>
      {/* Capabilities */}
      <div className="space-y-3">
        <h3 className="text-sm font-semibold text-white flex items-center gap-2"><Sparkles className="h-4 w-4 text-loomora-secondary" /> Capabilities</h3>
        {capabilities.length === 0 ? (
          <div className="rounded-lg border border-slate-800 bg-slate-900 p-4 text-xs text-slate-400">
            {hasDb ? 'No capabilities defined. Create capabilities via POST /api/admin/products.' : 'Database not connected.'}
          </div>
        ) : (
          <div className="flex flex-wrap gap-2">
            {capabilities.map((cap) => (
              <div key={cap.id} className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs">
                <span className="font-mono text-loomora-secondary">{cap.key}</span>
                <span className="text-slate-400 ml-2">{cap.name}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Products & Editions */}
      <div className="space-y-3">
        <h3 className="text-sm font-semibold text-white flex items-center gap-2"><Package className="h-4 w-4 text-loomora-secondary" /> Products & Editions</h3>
        {products.length === 0 ? (
          <EmptyState icon={Layers} title="No products" description={hasDb ? 'Create products, editions, and capabilities via the API.' : 'Database not connected.'} />
        ) : (
          <div className="space-y-4">
            {products.map((product) => (
              <div key={product.id} className="rounded-xl border border-slate-800 bg-slate-900/70 p-4 space-y-3">
                <div>
                  <h4 className="text-sm font-semibold text-white">{product.name}</h4>
                  <p className="text-xs text-slate-400">{product.description}</p>
                  <p className="text-xs text-slate-500 font-mono mt-0.5">slug: {product.slug}</p>
                </div>
                {product.editions.length > 0 && (
                  <div className="overflow-x-auto rounded-lg border border-slate-800">
                    <table className="w-full text-left text-sm">
                      <thead className="bg-slate-900 text-slate-300">
                        <tr>
                          <th className="px-3 py-2 text-xs font-medium">Edition</th>
                          <th className="px-3 py-2 text-xs font-medium">Price</th>
                          <th className="px-3 py-2 text-xs font-medium">Capabilities</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800">
                        {product.editions.map((edition: ProductEdition) => (
                          <tr key={edition.id} className="bg-slate-950/60">
                            <td className="px-3 py-2 text-xs text-slate-200">{edition.name} <span className="text-slate-500 font-mono">({edition.slug})</span></td>
                            <td className="px-3 py-2 text-xs text-slate-300">${(edition.priceCents / 100).toFixed(2)} {edition.currency}</td>
                            <td className="px-3 py-2 text-xs text-slate-400">{edition.capabilities.map((c: ProductEditionCapability) => c.capability.key).join(', ') || '—'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </PortalShell>
  );
}

