import { Link, useSearchParams } from 'react-router';
import { useExperiences } from '../api/hooks';
import type { ExperienceSummary, Outcome } from '../api/types';
import { Highlight } from '../components/Markdown';
import { Avatar, Empty, Pagination, QueryState } from '../components/ui';
import { OUTCOME_LABEL, stripMarkdown, timeAgo } from '../lib/format';

const OUTCOME_STYLE: Record<Outcome, string> = {
  SELECTED: 'text-emerald-700 dark:text-emerald-300',
  REJECTED: 'text-rose-700 dark:text-rose-300',
  WAITING: 'text-amber-700 dark:text-amber-300',
  WITHDREW: 'text-slate-500',
};

export function Difficulty({ value }: { value: number }) {
  return (
    <span className="text-xs text-slate-500" title={`Difficulty ${value} of 5`} aria-label={`Difficulty ${value} of 5`}>
      {'●'.repeat(value)}
      <span className="opacity-30">{'●'.repeat(5 - value)}</span>
    </span>
  );
}

export function ExperienceCard({ e }: { e: ExperienceSummary }) {
  return (
    <Link to={`/experiences/${e.id}`} className="card block hover:border-brand-500">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="font-semibold">
          {e.company.name} <span className="font-normal text-slate-500">· {e.roleTitle} · {e.year}</span>
        </p>
        <span className={`text-xs font-semibold ${OUTCOME_STYLE[e.outcome]}`}>{OUTCOME_LABEL[e.outcome]}</span>
      </div>
      <p className="mt-2 line-clamp-3 text-sm text-slate-700 dark:text-slate-300"><Highlight text={stripMarkdown(e.excerpt)} /></p>
      <div className="mt-3 flex items-center gap-3 text-xs text-slate-500">
        {e.author ? (
          <span className="flex items-center gap-1"><Avatar name={e.author.displayName} url={e.author.avatarUrl} size={18} />{e.author.displayName}</span>
        ) : (
          <span>{e.anonymous ? 'Anonymous' : 'Deleted user'}</span>
        )}
        <Difficulty value={e.difficulty} />
        <span>▲ {e.votes}</span>
        <span>💬 {e.comments}</span>
        <span className="ml-auto">{timeAgo(e.createdAt)}</span>
      </div>
    </Link>
  );
}

export function ExperiencesPage() {
  const [params, setParams] = useSearchParams();
  const f = {
    q: params.get('q') ?? '',
    company: params.get('company') ?? undefined,
    outcome: params.get('outcome') ?? undefined,
    sort: params.get('sort') ?? undefined,
    page: Number(params.get('page') ?? 0),
  };
  const set = (patch: Record<string, string | number | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => (v === undefined || v === '' ? next.delete(k) : next.set(k, String(v))));
    if (!('page' in patch)) next.delete('page');
    setParams(next, { replace: true });
  };
  const list = useExperiences(f);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="page-title">Interview experiences</h1>
          <p className="muted">Written by students, in their own words.</p>
        </div>
        <Link to="/experiences/new" className="btn-primary">Share yours</Link>
      </div>
      <div className="grid gap-2 sm:grid-cols-4">
        <input className="input sm:col-span-2" type="search" aria-label="Search experiences"
          placeholder='Search, e.g. "dynamic programming" -hr' value={f.q} onChange={(e) => set({ q: e.target.value })} />
        <select className="input" aria-label="Outcome" value={f.outcome ?? ''} onChange={(e) => set({ outcome: e.target.value })}>
          <option value="">Any outcome</option>
          {Object.entries(OUTCOME_LABEL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
        <select className="input" aria-label="Sort" value={f.sort ?? ''} onChange={(e) => set({ sort: e.target.value })}>
          <option value="">{f.q ? 'Best match' : 'Newest'}</option>
          <option value="new">Newest</option>
          <option value="top">Most upvoted</option>
        </select>
      </div>
      {f.company && (
        <p className="text-sm">Company: <span className="chip">{f.company}</span> <button className="underline" onClick={() => set({ company: undefined })}>clear</button></p>
      )}
      <QueryState query={list}>
        {(p) =>
          p.content.length === 0 ? (
            <Empty title="No experiences found">Try different words, or share the first one.</Empty>
          ) : (
            <>
              <div className="space-y-3">{p.content.map((e) => <ExperienceCard key={e.id} e={e} />)}</div>
              <Pagination page={p.page} totalPages={p.totalPages} onPage={(pg) => set({ page: pg })} />
            </>
          )
        }
      </QueryState>
    </div>
  );
}
