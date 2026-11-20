import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router';
import { useBranches, useCalendar, useDrives, type DriveFilters } from '../api/hooks';
import type { CalendarEvent, DriveSummary, StageType } from '../api/types';
import { EligibilityBadge } from '../components/EligibilityBadge';
import { Empty, Pagination, QueryState } from '../components/ui';
import { formatDate, STAGE_LABEL } from '../lib/format';

const STAGES: StageType[] = ['PPT', 'OA', 'GD', 'INTERVIEW', 'HR'];

export function StageChips({ stages }: { stages: StageType[] }) {
  return (
    <div className="flex flex-wrap gap-1">
      {stages.map((s) => (
        <span key={s} className="chip" title={STAGE_LABEL[s]}>{s === 'INTERVIEW' ? 'Interview' : s}</span>
      ))}
    </div>
  );
}

export function DriveCard({ drive }: { drive: DriveSummary }) {
  const company = drive.company?.name ?? drive.companyGuess ?? 'Unknown company';
  return (
    <Link to={`/drives/${drive.id}`} className="card block hover:border-brand-500">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="truncate font-semibold">{company}</p>
          <p className="muted line-clamp-2">{drive.title}</p>
        </div>
        <time className="shrink-0 text-xs text-slate-500" dateTime={drive.noticeDate}>{formatDate(drive.noticeDate)}</time>
      </div>
      <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
        <StageChips stages={drive.stageTypes} />
        <EligibilityBadge eligibility={drive.eligibility} />
      </div>
    </Link>
  );
}

/**
 * Filters live in the URL (?company=...&stage=OA), so a filtered view can be bookmarked or
 * shared on WhatsApp, and the back button works.
 */
export function DriveFiltersBar({ filters, onChange }: { filters: DriveFilters; onChange: (f: DriveFilters) => void }) {
  const branches = useBranches();
  return (
    <div className="grid gap-2 sm:grid-cols-4">
      <input
        className="input sm:col-span-2"
        type="search"
        placeholder="Search company or title…"
        aria-label="Search drives"
        value={filters.q ?? ''}
        onChange={(e) => onChange({ ...filters, q: e.target.value, page: 0 })}
      />
      <select className="input" aria-label="Branch" value={filters.branch ?? ''} onChange={(e) => onChange({ ...filters, branch: e.target.value, page: 0 })}>
        <option value="">All branches</option>
        {branches.data?.map((b) => <option key={b.code} value={b.code}>{b.name}</option>)}
      </select>
      <select className="input" aria-label="Stage" value={filters.stage ?? ''} onChange={(e) => onChange({ ...filters, stage: e.target.value, page: 0 })}>
        <option value="">Any stage</option>
        {STAGES.map((s) => <option key={s} value={s}>{STAGE_LABEL[s]}</option>)}
      </select>
    </div>
  );
}

export function DrivesPage() {
  const [params, setParams] = useSearchParams();
  const view = params.get('view') === 'calendar' ? 'calendar' : 'list';
  const filters: DriveFilters = {
    q: params.get('q') ?? undefined,
    branch: params.get('branch') ?? undefined,
    stage: params.get('stage') ?? undefined,
    company: params.get('company') ?? undefined,
    page: Number(params.get('page') ?? 0),
  };
  const setFilters = (f: DriveFilters) => {
    const next = new URLSearchParams();
    Object.entries({ ...f, view }).forEach(([k, v]) => v !== undefined && v !== '' && v !== 0 && next.set(k, String(v)));
    setParams(next, { replace: true });
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h1 className="page-title">Drive calendar</h1>
          <p className="muted">From the public CDC notice board, refreshed every 6 hours. Always confirm on the official circular.</p>
        </div>
        <div className="flex rounded-xl border border-slate-300 p-1 dark:border-slate-700" role="tablist">
          {(['list', 'calendar'] as const).map((v) => (
            <button
              key={v}
              role="tab"
              aria-selected={view === v}
              className={`rounded-lg px-3 py-1 text-sm capitalize ${view === v ? 'bg-brand-600 text-white' : ''}`}
              onClick={() => {
                const next = new URLSearchParams(params);
                next.set('view', v);
                setParams(next, { replace: true });
              }}
            >
              {v}
            </button>
          ))}
        </div>
      </div>

      {view === 'list' ? <DriveList filters={filters} setFilters={setFilters} /> : <CalendarView />}
    </div>
  );
}

function DriveList({ filters, setFilters }: { filters: DriveFilters; setFilters: (f: DriveFilters) => void }) {
  const drives = useDrives(filters);
  return (
    <>
      <DriveFiltersBar filters={filters} onChange={setFilters} />
      {filters.company && (
        <p className="text-sm">
          Company: <span className="chip">{filters.company}</span>{' '}
          <button className="underline" onClick={() => setFilters({ ...filters, company: undefined })}>clear</button>
        </p>
      )}
      <QueryState query={drives}>
        {(page) =>
          page.content.length === 0 ? (
            <Empty title="No drives match these filters" />
          ) : (
            <>
              <p className="muted">{page.totalElements} drives</p>
              <div className="grid gap-3 sm:grid-cols-2">
                {page.content.map((d) => <DriveCard key={d.id} drive={d} />)}
              </div>
              <Pagination page={page.page} totalPages={page.totalPages} onPage={(p) => setFilters({ ...filters, page: p })} />
            </>
          )
        }
      </QueryState>
    </>
  );
}

// ---------- calendar ----------

function monthRange(year: number, month: number) {
  const pad = (n: number) => String(n).padStart(2, '0');
  const last = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  return { from: `${year}-${pad(month + 1)}-01`, to: `${year}-${pad(month + 1)}-${pad(last)}`, days: last };
}

const DOT: Record<string, string> = {
  ELIGIBLE: 'bg-emerald-500',
  NOT_ELIGIBLE: 'bg-rose-400',
  UNKNOWN: 'bg-amber-400',
};

function CalendarView() {
  const now = new Date();
  const [ym, setYm] = useState({ y: now.getFullYear(), m: now.getMonth() });
  const { from, to, days } = monthRange(ym.y, ym.m);
  const events = useCalendar(from, to);
  const [selected, setSelected] = useState<string | null>(null);

  const byDay = useMemo(() => {
    const map: Record<string, CalendarEvent[]> = {};
    events.data?.forEach((e) => (map[e.date] ??= []).push(e));
    return map;
  }, [events.data]);

  const firstWeekday = (new Date(Date.UTC(ym.y, ym.m, 1)).getUTCDay() + 6) % 7; // Monday = 0
  const label = new Date(Date.UTC(ym.y, ym.m, 1)).toLocaleDateString('en-IN', { month: 'long', year: 'numeric', timeZone: 'UTC' });
  const move = (delta: number) => {
    const d = new Date(Date.UTC(ym.y, ym.m + delta, 1));
    setYm({ y: d.getUTCFullYear(), m: d.getUTCMonth() });
    setSelected(null);
  };
  const dayKey = (d: number) => `${from.slice(0, 8)}${String(d).padStart(2, '0')}`;

  return (
    <div className="card">
      <div className="mb-3 flex items-center justify-between">
        <button className="btn-ghost py-1" onClick={() => move(-1)} aria-label="Previous month">←</button>
        <h2 className="font-semibold">{label}</h2>
        <button className="btn-ghost py-1" onClick={() => move(1)} aria-label="Next month">→</button>
      </div>
      <div className="grid grid-cols-7 gap-1 text-center text-xs text-slate-500">
        {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((d) => <div key={d}>{d}</div>)}
      </div>
      <div className="mt-1 grid grid-cols-7 gap-1">
        {Array.from({ length: firstWeekday }).map((_, i) => <div key={`pad${i}`} />)}
        {Array.from({ length: days }, (_, i) => i + 1).map((d) => {
          const key = dayKey(d);
          const list = byDay[key] ?? [];
          return (
            <button
              key={d}
              onClick={() => setSelected(key)}
              className={`flex aspect-square flex-col items-center justify-start rounded-lg p-1 text-sm ${
                selected === key ? 'bg-brand-600 text-white' : list.length ? 'bg-slate-100 dark:bg-slate-800' : ''
              }`}
              aria-label={`${d}: ${list.length} events`}
            >
              {d}
              <span className="mt-auto flex flex-wrap justify-center gap-0.5">
                {list.slice(0, 4).map((e, i) => (
                  <span key={i} className={`h-1.5 w-1.5 rounded-full ${e.eligibility ? DOT[e.eligibility] : 'bg-brand-500'}`} />
                ))}
              </span>
            </button>
          );
        })}
      </div>
      <p className="mt-2 text-xs text-slate-500">Dots: green = eligible, red = not eligible, amber = criteria unknown (when signed in).</p>
      {selected && (
        <ul className="mt-4 space-y-2">
          {(byDay[selected] ?? []).length === 0 && <li className="muted">Nothing on {formatDate(selected)}.</li>}
          {(byDay[selected] ?? []).map((e, i) => (
            <li key={i}>
              <Link to={`/drives/${e.driveId}`} className="block rounded-lg border border-slate-200 p-2 hover:border-brand-500 dark:border-slate-800">
                <span className="chip mr-2">{STAGE_LABEL[e.kind]}</span>
                <span className="font-medium">{e.companyName ?? 'Unknown company'}</span>
                <span className="muted block truncate">{e.title}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
      {events.error && <p className="mt-2 text-sm text-rose-600">Couldn&apos;t load events.</p>}
    </div>
  );
}
