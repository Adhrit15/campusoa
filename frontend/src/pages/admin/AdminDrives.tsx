import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../../api/client';
import { useBranches, useCompanies, useDrive, useDrives } from '../../api/hooks';
import type { DriveDetail, StageType } from '../../api/types';
import { Field, Pagination, QueryState } from '../../components/ui';
import { formatDate, STAGE_LABEL } from '../../lib/format';

/** Admin drive tools: link a company, fix details, enter criteria and stages, merge duplicates. */
export function AdminDrives() {
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<number | null>(null);
  const drives = useDrives({ q, page });
  const qc = useQueryClient();
  const [creating, setCreating] = useState(false);

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <div className="space-y-3">
        <div className="flex gap-2">
          <input className="input" placeholder="Search drives…" value={q} onChange={(e) => { setQ(e.target.value); setPage(0); }} />
          <button className="btn-primary" onClick={() => { setCreating(true); setSelected(null); }}>New</button>
        </div>
        <QueryState query={drives}>
          {(p) => (
            <>
              <ul className="card divide-y divide-slate-100 p-0 dark:divide-slate-800">
                {p.content.map((d) => (
                  <li key={d.id}>
                    <button className={`w-full px-3 py-2 text-left text-sm ${selected === d.id ? 'bg-brand-50 dark:bg-slate-800' : ''}`}
                      onClick={() => { setSelected(d.id); setCreating(false); }}>
                      <span className="font-medium">#{d.id} {d.company?.name ?? <em className="text-amber-600">{d.companyGuess ?? 'no company'}</em>}</span>
                      <span className="muted block truncate">{formatDate(d.noticeDate)} · {d.title}</span>
                      {!d.hasCriteria && <span className="text-xs text-amber-600">criteria missing</span>}
                    </button>
                  </li>
                ))}
              </ul>
              <Pagination page={p.page} totalPages={p.totalPages} onPage={setPage} />
            </>
          )}
        </QueryState>
      </div>
      <div>
        {creating && <DriveForm onSaved={(d) => { setCreating(false); setSelected(d.id); qc.invalidateQueries({ queryKey: ['drives'] }); }} />}
        {selected && <DriveEditor id={selected} />}
        {!creating && !selected && <p className="muted">Select a drive to edit it.</p>}
      </div>
    </div>
  );
}

function CompanySelect({ value, onChange }: { value?: number; onChange: (id?: number) => void }) {
  const companies = useCompanies('', 'name', 0, 50);
  return (
    <select className="input" value={value ?? ''} onChange={(e) => onChange(e.target.value ? Number(e.target.value) : undefined)}>
      <option value="">— none —</option>
      {companies.data?.content.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
    </select>
  );
}

function useInvalidateDrives(id?: number) {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({ queryKey: ['drives'] });
    qc.invalidateQueries({ queryKey: ['calendar'] });
    if (id) qc.invalidateQueries({ queryKey: ['drive', id] });
  };
}

function DriveForm({ drive, onSaved }: { drive?: DriveDetail; onSaved: (d: DriveDetail) => void }) {
  const [f, setF] = useState({
    companyId: drive?.company?.id,
    title: drive?.title ?? '',
    noticeDate: drive?.noticeDate ?? new Date().toISOString().slice(0, 10),
    pdfUrl: drive?.pdfUrl ?? '',
    roleTitle: drive?.roleTitle ?? '',
    ctcText: drive?.ctcText ?? '',
    hidden: drive?.hidden ?? false,
  });
  const save = useMutation({
    mutationFn: () => {
      const body = { ...f, pdfUrl: f.pdfUrl || undefined, roleTitle: f.roleTitle || undefined, ctcText: f.ctcText || undefined };
      return drive ? api.put<DriveDetail>(`/api/admin/drives/${drive.id}`, body) : api.post<DriveDetail>('/api/admin/drives', body);
    },
    onSuccess: onSaved,
  });
  return (
    <form className="card space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <h2 className="font-semibold">{drive ? `Drive #${drive.id}` : 'New drive'}</h2>
      {drive?.companyGuess && !drive.company && <p className="text-sm text-amber-600">Scraper guessed: &ldquo;{drive.companyGuess}&rdquo;</p>}
      <Field label="Company"><CompanySelect value={f.companyId} onChange={(companyId) => setF({ ...f, companyId })} /></Field>
      <Field label="Title"><input className="input" required value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} /></Field>
      <div className="grid grid-cols-2 gap-2">
        <Field label="Date"><input className="input" type="date" required value={f.noticeDate} onChange={(e) => setF({ ...f, noticeDate: e.target.value })} /></Field>
        <Field label="CTC text"><input className="input" placeholder="12 LPA" value={f.ctcText} onChange={(e) => setF({ ...f, ctcText: e.target.value })} /></Field>
      </div>
      <Field label="Role"><input className="input" value={f.roleTitle} onChange={(e) => setF({ ...f, roleTitle: e.target.value })} /></Field>
      <Field label="Circular PDF URL"><input className="input" type="url" value={f.pdfUrl} onChange={(e) => setF({ ...f, pdfUrl: e.target.value })} /></Field>
      <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.hidden} onChange={(e) => setF({ ...f, hidden: e.target.checked })} /> Hidden (not a real drive)</label>
      {save.error && <p className="text-sm text-rose-600">{(save.error as ApiError).message}</p>}
      <button className="btn-primary" disabled={save.isPending}>Save details</button>
    </form>
  );
}

function DriveEditor({ id }: { id: number }) {
  const drive = useDrive(id);
  const refresh = useInvalidateDrives(id);
  return (
    <QueryState query={drive}>
      {(d) => (
        <div key={d.id} className="space-y-4">
          <DriveForm drive={d} onSaved={refresh} />
          <CriteriaForm drive={d} onSaved={refresh} />
          <StagesForm drive={d} onSaved={refresh} />
          <MergeForm drive={d} onSaved={refresh} />
        </div>
      )}
    </QueryState>
  );
}

function CriteriaForm({ drive, onSaved }: { drive: DriveDetail; onSaved: () => void }) {
  const branches = useBranches();
  const c = drive.criteria;
  const [f, setF] = useState({
    minCgpa: c?.minCgpa?.toString() ?? '',
    maxBacklogs: c?.maxBacklogs?.toString() ?? '',
    gradYear: c?.gradYear?.toString() ?? '',
    branches: c?.branches ?? [],
    notes: c?.notes ?? '',
  });
  const n =(s: string) => (s === '' ? null : Number(s));
  const save = useMutation({
    mutationFn: () => api.put(`/api/admin/drives/${drive.id}/criteria`, {
      minCgpa: n(f.minCgpa), maxBacklogs: n(f.maxBacklogs), gradYear: n(f.gradYear), branches: f.branches, notes: f.notes || null,
    }),
    onSuccess: onSaved,
  });
  const clear = useMutation({ mutationFn: () => api.del(`/api/admin/drives/${drive.id}/criteria`), onSuccess: onSaved });
  const toggle = (code: string) =>
    setF({ ...f, branches: f.branches.includes(code) ? f.branches.filter((b) => b !== code) : [...f.branches, code] });

  return (
    <form className="card space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <h3 className="font-semibold">Eligibility criteria {c ? '' : <span className="text-xs text-amber-600">(none yet = &ldquo;criteria unknown&rdquo;)</span>}</h3>
      <div className="grid grid-cols-3 gap-2">
        <Field label="Min CGPA"><input className="input" type="number" step="0.01" value={f.minCgpa} onChange={(e) => setF({ ...f, minCgpa: e.target.value })} /></Field>
        <Field label="Max backlogs"><input className="input" type="number" value={f.maxBacklogs} onChange={(e) => setF({ ...f, maxBacklogs: e.target.value })} /></Field>
        <Field label="Batch"><input className="input" type="number" placeholder="2027" value={f.gradYear} onChange={(e) => setF({ ...f, gradYear: e.target.value })} /></Field>
      </div>
      <fieldset>
        <legend className="label">Branches (none ticked = all branches)</legend>
        <div className="flex flex-wrap gap-2">
          {branches.data?.map((b) => (
            <label key={b.code} className="flex items-center gap-1 text-sm">
              <input type="checkbox" checked={f.branches.includes(b.code)} onChange={() => toggle(b.code)} /> {b.code}
            </label>
          ))}
        </div>
      </fieldset>
      <Field label="Notes"><input className="input" value={f.notes} onChange={(e) => setF({ ...f, notes: e.target.value })} /></Field>
      {save.error && <p className="text-sm text-rose-600">{(save.error as ApiError).message}</p>}
      <div className="flex gap-2">
        <button className="btn-primary" disabled={save.isPending}>Save criteria</button>
        {c && <button type="button" className="btn-ghost" onClick={() => clear.mutate()}>Clear</button>}
      </div>
    </form>
  );
}

const STAGE_TYPES: StageType[] = ['PPT', 'OA', 'GD', 'INTERVIEW', 'HR', 'OTHER'];

/** datetime-local works in local browser time; we convert to/from ISO instants. */
const toLocalInput = (iso?: string) => (iso ? new Date(new Date(iso).getTime() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16) : '');

function StagesForm({ drive, onSaved }: { drive: DriveDetail; onSaved: () => void }) {
  const [stages, setStages] = useState(drive.stages.map((s) => ({ stageType: s.stageType, startsAt: toLocalInput(s.startsAt), mode: s.mode ?? '', notes: s.notes ?? '' })));
  const save = useMutation({
    mutationFn: () => api.put(`/api/admin/drives/${drive.id}/stages`, {
      stages: stages.map((s) => ({ stageType: s.stageType, startsAt: s.startsAt ? new Date(s.startsAt).toISOString() : null, mode: s.mode || null, notes: s.notes || null })),
    }),
    onSuccess: onSaved,
  });
  const update = (i: number, patch: Partial<(typeof stages)[number]>) => setStages(stages.map((s, j) => (j === i ? { ...s, ...patch } : s)));
  return (
    <form className="card space-y-2" onSubmit={(e) => { e.preventDefault(); save.mutate(); }}>
      <h3 className="font-semibold">Stages</h3>
      {stages.map((s, i) => (
        <div key={i} className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          <select className="input" value={s.stageType} onChange={(e) => update(i, { stageType: e.target.value as StageType })}>
            {STAGE_TYPES.map((t) => <option key={t} value={t}>{STAGE_LABEL[t]}</option>)}
          </select>
          <input className="input" type="datetime-local" value={s.startsAt} onChange={(e) => update(i, { startsAt: e.target.value })} />
          <select className="input" value={s.mode} onChange={(e) => update(i, { mode: e.target.value })}>
            <option value="">Mode?</option><option value="ONLINE">Online</option><option value="OFFLINE">Offline</option><option value="HYBRID">Hybrid</option>
          </select>
          <div className="flex gap-1">
            <input className="input" placeholder="Notes" value={s.notes} onChange={(e) => update(i, { notes: e.target.value })} />
            <button type="button" className="px-2 text-rose-600" aria-label="Remove stage" onClick={() => setStages(stages.filter((_, j) => j !== i))}>✕</button>
          </div>
        </div>
      ))}
      <div className="flex gap-2">
        <button type="button" className="btn-ghost" onClick={() => setStages([...stages, { stageType: 'OA', startsAt: '', mode: '', notes: '' }])}>+ Stage</button>
        <button className="btn-primary" disabled={save.isPending}>Save stages</button>
      </div>
    </form>
  );
}

function MergeForm({ drive, onSaved }: { drive: DriveDetail; onSaved: () => void }) {
  const [target, setTarget] = useState('');
  const merge = useMutation({
    mutationFn: () => api.post(`/api/admin/drives/${drive.id}/merge`, { targetDriveId: Number(target) }),
    onSuccess: onSaved,
  });
  return (
    <form className="card space-y-2" onSubmit={(e) => { e.preventDefault(); if (confirm(`Merge #${drive.id} into #${target}?`)) merge.mutate(); }}>
      <h3 className="font-semibold">Merge duplicate</h3>
      <p className="muted">Moves this drive&apos;s stages into another drive (e.g. the PPT and OA circulars of one company) and hides this one.</p>
      <div className="flex gap-2">
        <input className="input" type="number" placeholder="Target drive #" value={target} onChange={(e) => setTarget(e.target.value)} />
        <button className="btn-ghost" disabled={!target || merge.isPending}>Merge</button>
      </div>
      {merge.error && <p className="text-sm text-rose-600">{(merge.error as ApiError).message}</p>}
      {merge.isSuccess && <p className="text-sm text-emerald-600">Merged.</p>}
    </form>
  );
}
