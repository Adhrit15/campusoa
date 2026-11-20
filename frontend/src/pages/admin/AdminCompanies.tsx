import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../../api/client';
import { useCompanies, useCompany } from '../../api/hooks';
import type { CompanyDetail } from '../../api/types';
import { Markdown } from '../../components/Markdown';
import { Field, QueryState } from '../../components/ui';

export function AdminCompanies() {
  const [q, setQ] = useState('');
  const companies = useCompanies(q, 'name', 0, 50);
  const [slug, setSlug] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  return (
    <div className="grid gap-4 lg:grid-cols-3">
      <div className="space-y-2">
        <div className="flex gap-2">
          <input className="input" placeholder="Search…" value={q} onChange={(e) => setQ(e.target.value)} />
          <button className="btn-primary" onClick={() => { setCreating(true); setSlug(null); }}>New</button>
        </div>
        <ul className="card max-h-[70vh] divide-y divide-slate-100 overflow-y-auto p-0 dark:divide-slate-800">
          {companies.data?.content.map((c) => (
            <li key={c.id}>
              <button className={`w-full px-3 py-2 text-left text-sm ${slug === c.slug ? 'bg-brand-50 dark:bg-slate-800' : ''}`}
                onClick={() => { setSlug(c.slug); setCreating(false); }}>{c.name}</button>
            </li>
          ))}
        </ul>
      </div>
      <div className="lg:col-span-2">
        {creating && <CompanyForm onSaved={(c) => { setCreating(false); setSlug(c.slug); }} />}
        {slug && <CompanyEditor slug={slug} onSlugChange={setSlug} />}
        {!creating && !slug && <p className="muted">Select a company.</p>}
      </div>
    </div>
  );
}

function CompanyEditor({ slug, onSlugChange }: { slug: string; onSlugChange: (s: string) => void }) {
  const company = useCompany(slug);
  return (
    <QueryState query={company}>
      {(c) => (
        <div key={c.id} className="space-y-4">
          <CompanyForm company={c} onSaved={(u) => onSlugChange(u.slug)} />
          <PlacementForm company={c} />
        </div>
      )}
    </QueryState>
  );
}

function CompanyForm({ company, onSaved }: { company?: CompanyDetail; onSaved: (c: CompanyDetail) => void }) {
  const qc = useQueryClient();
  const [f, setF] = useState({
    name: company?.name ?? '',
    website: company?.website ?? '',
    aliases: company?.aliases.join(', ') ?? '',
    prepMarkdown: company?.prepMarkdown ?? '',
  });
  const [preview, setPreview] = useState(false);
  const save = useMutation({
    mutationFn: () => {
      const body = {
        name: f.name,
        website: f.website || null,
        prepMarkdown: f.prepMarkdown,
        aliases: f.aliases.split(',').map((a) => a.trim()).filter(Boolean),
      };
      return company ? api.put<CompanyDetail>(`/api/admin/companies/${company.id}`, body) : api.post<CompanyDetail>('/api/admin/companies', body);
    },
    onSuccess: (c) => {
      qc.invalidateQueries({ queryKey: ['companies'] });
      qc.setQueryData(['company', c.slug], c);
      onSaved(c);
    },
  });
  return (
    <form className="card space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <h2 className="font-semibold">{company ? company.name : 'New company'}</h2>
      <div className="grid gap-2 sm:grid-cols-2">
        <Field label="Name"><input className="input" required value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} /></Field>
        <Field label="Website"><input className="input" type="url" value={f.website} onChange={(e) => setF({ ...f, website: e.target.value })} /></Field>
      </div>
      <Field label="Aliases (comma-separated)" hint="Names used in circular titles, e.g. JPMC, LTM. Helps the scraper match drives.">
        <input className="input" value={f.aliases} onChange={(e) => setF({ ...f, aliases: e.target.value })} />
      </Field>
      <div>
        <div className="mb-1 flex items-center justify-between">
          <span className="label">How to prepare (markdown)</span>
          <button type="button" className="text-xs underline" onClick={() => setPreview(!preview)}>{preview ? 'Edit' : 'Preview'}</button>
        </div>
        {preview ? <div className="rounded-xl border p-3 dark:border-slate-700"><Markdown>{f.prepMarkdown || '_Nothing yet_'}</Markdown></div> : (
          <textarea className="input font-mono" rows={8} value={f.prepMarkdown} onChange={(e) => setF({ ...f, prepMarkdown: e.target.value })} />
        )}
      </div>
      {save.error && <p className="text-sm text-rose-600">{(save.error as ApiError).message}</p>}
      {save.isSuccess && <p className="text-sm text-emerald-600">Saved.</p>}
      <button className="btn-primary" disabled={save.isPending}>Save company</button>
    </form>
  );
}

function PlacementForm({ company }: { company: CompanyDetail }) {
  const qc = useQueryClient();
  const [f, setF] = useState({ academicYear: '2025-26', roleTitle: '', ctcMinLpa: '', ctcMaxLpa: '', offers: '' });
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['company', company.slug] });
    qc.invalidateQueries({ queryKey: ['companies'] });
  };
  const add = useMutation({
    mutationFn: () => api.post(`/api/admin/companies/${company.id}/placements`, {
      academicYear: f.academicYear,
      roleTitle: f.roleTitle || null,
      ctcMinLpa: f.ctcMinLpa ? Number(f.ctcMinLpa) : null,
      ctcMaxLpa: Number(f.ctcMaxLpa),
      offers: f.offers ? Number(f.offers) : null,
      sourceNote: 'Admin-entered',
    }),
    onSuccess: refresh,
  });
  const remove = useMutation({ mutationFn: (id: number) => api.del(`/api/admin/companies/${company.id}/placements/${id}`), onSuccess: refresh });

  return (
    <div className="card space-y-3">
      <h3 className="font-semibold">Placement stats</h3>
      <ul className="text-sm">
        {company.placements.map((p) => (
          <li key={p.id} className="flex items-center gap-2">
            {p.academicYear} · {p.roleTitle} · {p.ctcLabel} · {p.offers ?? '?'} offers
            <button className="text-xs text-rose-600" onClick={() => confirm('Delete this stat?') && remove.mutate(p.id)}>✕</button>
          </li>
        ))}
      </ul>
      <form className="grid grid-cols-2 gap-2 sm:grid-cols-6" onSubmit={(e) => { e.preventDefault(); add.mutate(); }}>
        <input className="input" placeholder="2025-26" value={f.academicYear} onChange={(e) => setF({ ...f, academicYear: e.target.value })} />
        <input className="input sm:col-span-2" placeholder="Role" value={f.roleTitle} onChange={(e) => setF({ ...f, roleTitle: e.target.value })} />
        <input className="input" placeholder="Min LPA" type="number" step="0.1" value={f.ctcMinLpa} onChange={(e) => setF({ ...f, ctcMinLpa: e.target.value })} />
        <input className="input" placeholder="Max LPA" required type="number" step="0.1" value={f.ctcMaxLpa} onChange={(e) => setF({ ...f, ctcMaxLpa: e.target.value })} />
        <input className="input" placeholder="Offers" type="number" value={f.offers} onChange={(e) => setF({ ...f, offers: e.target.value })} />
        <button className="btn-ghost col-span-2 sm:col-span-6" disabled={add.isPending}>Add stat</button>
      </form>
      {add.error && <p className="text-sm text-rose-600">{(add.error as ApiError).message}</p>}
    </div>
  );
}
