import { useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../api/client';
import { useDashboard, useProblems, useRoadmap, useTickRoadmap, useTopics } from '../api/hooks';
import type { Difficulty, RoadmapSection } from '../api/types';
import { Field, Pagination, QueryState } from '../components/ui';
import { formatDate, todayIst } from '../lib/format';

const TABS = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'roadmap', label: 'Roadmap' },
  { id: 'problems', label: 'Problem log' },
] as const;

export function PrepPage() {
  const [params, setParams] = useSearchParams();
  const tab = (params.get('tab') ?? 'dashboard') as (typeof TABS)[number]['id'];
  return (
    <div className="space-y-4">
      <h1 className="page-title">My prep</h1>
      <div className="flex gap-1 overflow-x-auto" role="tablist">
        {TABS.map((t) => (
          <button key={t.id} role="tab" aria-selected={tab === t.id}
            className={`rounded-xl px-4 py-2 text-sm font-medium ${tab === t.id ? 'bg-brand-600 text-white' : 'bg-slate-100 dark:bg-slate-800'}`}
            onClick={() => setParams({ tab: t.id }, { replace: true })}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'dashboard' && <DashboardTab />}
      {tab === 'roadmap' && <RoadmapTab />}
      {tab === 'problems' && <ProblemsTab />}
    </div>
  );
}

function Stat({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="card">
      <p className="muted">{label}</p>
      <p className="text-2xl font-bold">{value}</p>
      {sub && <p className="text-xs text-slate-500">{sub}</p>}
    </div>
  );
}

function DashboardTab() {
  const dash = useDashboard();
  return (
    <QueryState query={dash}>
      {(d) => {
        const maxWeek = Math.max(1, ...d.perWeek.map((w) => w.solved));
        const maxTopic = Math.max(1, ...d.topicCoverage.map((t) => t.solved));
        return (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <Stat label="Current streak" value={`🔥 ${d.currentStreak}`} sub={`Longest: ${d.longestStreak} days`} />
              <Stat label="This week" value={d.solvedThisWeek} sub={`${d.solvedThisMonth} this month`} />
              <Stat label="Total solved" value={d.totalSolved} sub={`${d.easy} easy · ${d.medium} med · ${d.hard} hard`} />
              <Stat label="Roadmap" value={`${d.roadmapTotal ? Math.round((100 * d.roadmapDone) / d.roadmapTotal) : 0}%`} sub={`${d.roadmapDone} of ${d.roadmapTotal} topics`} />
            </div>
            <div className="card">
              <h2 className="mb-3 font-semibold">Problems per week (last 12 weeks)</h2>
              <div className="flex h-36 items-end gap-1" role="img" aria-label="Bar chart of problems solved per week">
                {d.perWeek.map((w) => (
                  <div key={w.weekStart} className="flex flex-1 flex-col items-center gap-1">
                    <span className="text-[10px] text-slate-500">{w.solved || ''}</span>
                    <div className="w-full rounded-t bg-brand-500" style={{ height: `${(w.solved / maxWeek) * 100}%`, minHeight: w.solved ? 4 : 1 }} title={`Week of ${formatDate(w.weekStart)}: ${w.solved}`} />
                  </div>
                ))}
              </div>
              <div className="mt-1 flex justify-between text-[10px] text-slate-500">
                <span>{formatDate(d.perWeek[0].weekStart)}</span><span>this week</span>
              </div>
            </div>
            <div className="card">
              <h2 className="mb-3 font-semibold">DSA topic coverage</h2>
              <ul className="space-y-1">
                {d.topicCoverage.map((t) => (
                  <li key={t.code} className="flex items-center gap-2 text-sm">
                    <span className="w-40 shrink-0 truncate">{t.name}</span>
                    <span className="h-2 flex-1 rounded bg-slate-100 dark:bg-slate-800">
                      <span className={`block h-2 rounded ${t.solved ? 'bg-emerald-500' : ''}`} style={{ width: `${(t.solved / maxTopic) * 100}%` }} />
                    </span>
                    <span className={`w-8 text-right ${t.solved ? '' : 'text-rose-500'}`}>{t.solved}</span>
                  </li>
                ))}
              </ul>
              <p className="mt-2 text-xs text-slate-500">Red zeros are your gaps: pick one this week.</p>
            </div>
          </div>
        );
      }}
    </QueryState>
  );
}

export function RoadmapChecklist({ sections, onToggle }: { sections: RoadmapSection[]; onToggle: (itemId: number, done: boolean) => void }) {
  return (
    <ol className="space-y-3">
      {sections.map((s, idx) => (
        <li key={s.code} className="card">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold">{idx + 1}. {s.name}</h2>
            <span className="chip">{s.done}/{s.total}</span>
          </div>
          <ul className="space-y-1">
            {s.items.map((i) => (
              <li key={i.id}>
                <label className="flex cursor-pointer items-start gap-2 rounded-lg p-1 hover:bg-slate-50 dark:hover:bg-slate-800">
                  <input type="checkbox" className="mt-1 h-4 w-4" checked={i.done} onChange={(e) => onToggle(i.id, e.target.checked)} />
                  <span>
                    <span className={i.done ? 'line-through opacity-60' : ''}>{i.title}</span>
                    {i.description && <span className="block text-xs text-slate-500">{i.description}</span>}
                  </span>
                </label>
              </li>
            ))}
          </ul>
        </li>
      ))}
    </ol>
  );
}

function RoadmapTab() {
  const roadmap = useRoadmap();
  const tick = useTickRoadmap();
  return (
    <QueryState query={roadmap}>
      {(sections) => <RoadmapChecklist sections={sections} onToggle={(itemId, done) => tick.mutate({ itemId, done })} />}
    </QueryState>
  );
}

function ProblemsTab() {
  const [page, setPage] = useState(0);
  const problems = useProblems(page);
  const topics = useTopics();
  const qc = useQueryClient();
  const blank = { title: '', url: '', topicCode: '', difficulty: 'MEDIUM' as Difficulty, solvedOn: todayIst() };
  const [form, setForm] = useState(blank);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['problems'] });
    qc.invalidateQueries({ queryKey: ['dashboard'] });
    qc.invalidateQueries({ queryKey: ['leaderboard'] });
  };
  const add = useMutation({
    mutationFn: () => api.post('/api/prep/problems', form),
    onSuccess: () => { setForm({ ...blank, topicCode: form.topicCode }); setErrors({}); refresh(); },
    onError: (e) => e instanceof ApiError && setErrors(e.fieldErrors.length ? e.fieldMap() : { form: e.message }),
  });
  const remove = useMutation({ mutationFn: (id: number) => api.del(`/api/prep/problems/${id}`), onSuccess: refresh });

  const submit = (ev: FormEvent) => {
    ev.preventDefault();
    add.mutate();
  };

  return (
    <div className="space-y-4">
      <form onSubmit={submit} className="card grid gap-3 sm:grid-cols-2">
        <Field label="Problem" error={errors.title}>
          <input className="input" required placeholder="Two Sum" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
        </Field>
        <Field label="Link" error={errors.url}>
          <input className="input" required type="url" placeholder="https://leetcode.com/problems/…" value={form.url} onChange={(e) => setForm({ ...form, url: e.target.value })} />
        </Field>
        <Field label="Topic" error={errors.topicCode}>
          <select className="input" required value={form.topicCode} onChange={(e) => setForm({ ...form, topicCode: e.target.value })}>
            <option value="">Select…</option>
            {topics.data?.filter((t) => t.parentCode === 'dsa').map((t) => <option key={t.code} value={t.code}>{t.name}</option>)}
          </select>
        </Field>
        <div className="grid grid-cols-2 gap-2">
          <Field label="Difficulty">
            <select className="input" value={form.difficulty} onChange={(e) => setForm({ ...form, difficulty: e.target.value as Difficulty })}>
              <option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
            </select>
          </Field>
          <Field label="Solved on" error={errors.solvedOn}>
            <input className="input" type="date" max={todayIst()} value={form.solvedOn} onChange={(e) => setForm({ ...form, solvedOn: e.target.value })} />
          </Field>
        </div>
        {errors.form && <p className="text-sm text-rose-600 sm:col-span-2">{errors.form}</p>}
        <button className="btn-primary sm:col-span-2" disabled={add.isPending}>Log problem</button>
      </form>

      <QueryState query={problems}>
        {(p) => (
          <div className="card">
            {p.content.length === 0 ? (
              <p className="muted">Nothing logged yet. Solve one and add it above!</p>
            ) : (
              <ul className="divide-y divide-slate-100 dark:divide-slate-800">
                {p.content.map((pr) => (
                  <li key={pr.id} className="flex items-center gap-2 py-2 text-sm">
                    <a href={pr.url} target="_blank" rel="noopener noreferrer" className="min-w-0 flex-1 truncate font-medium hover:underline">{pr.title}</a>
                    <span className="chip hidden sm:inline-flex">{pr.topicName}</span>
                    <span className="text-xs text-slate-500">{formatDate(pr.solvedOn)}</span>
                    <button className="text-xs text-rose-600" aria-label={`Delete ${pr.title}`} onClick={() => remove.mutate(pr.id)}>✕</button>
                  </li>
                ))}
              </ul>
            )}
            <Pagination page={p.page} totalPages={p.totalPages} onPage={setPage} />
          </div>
        )}
      </QueryState>
    </div>
  );
}
