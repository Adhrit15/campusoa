import { Link, useSearchParams } from 'react-router';
import { useQuestions, useTopics } from '../api/hooks';
import type { Question } from '../api/types';
import { Markdown } from '../components/Markdown';
import { ReportButton } from '../components/ReportButton';
import { VoteButton } from '../components/VoteButton';
import { Empty, Pagination, QueryState } from '../components/ui';

const DIFF_STYLE = {
  EASY: 'text-emerald-700 dark:text-emerald-300',
  MEDIUM: 'text-amber-700 dark:text-amber-300',
  HARD: 'text-rose-700 dark:text-rose-300',
};

export function QuestionCard({ q }: { q: Question }) {
  return (
    <div className="card">
      <div className="mb-2 flex flex-wrap items-center gap-2 text-xs">
        <span className={`font-semibold ${DIFF_STYLE[q.difficulty]}`}>{q.difficulty.toLowerCase()}</span>
        {q.topics.map((t) => <span key={t.code} className="chip">{t.name}</span>)}
        {q.company && <Link to={`/companies/${q.company.slug}`} className="chip hover:underline">{q.company.name}</Link>}
      </div>
      <Markdown>{q.bodyMd}</Markdown>
      <div className="mt-2 flex items-center gap-3 text-xs text-slate-500">
        <VoteButton path={`/api/questions/${q.id}/vote`} votes={q.votes} votedByMe={q.votedByMe} />
        {q.experienceId && <Link className="underline" to={`/experiences/${q.experienceId}`}>From an experience</Link>}
        <span className="ml-auto"><ReportButton targetType="QUESTION" targetId={q.id} /></span>
      </div>
    </div>
  );
}

export function QuestionsPage() {
  const [params, setParams] = useSearchParams();
  const topics = useTopics();
  const f = {
    q: params.get('q') ?? '',
    topic: params.get('topic') ?? undefined,
    difficulty: params.get('difficulty') ?? undefined,
    sort: params.get('sort') ?? undefined,
    page: Number(params.get('page') ?? 0),
  };
  const set = (patch: Record<string, string | number | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(patch).forEach(([k, v]) => (v === undefined || v === '' ? next.delete(k) : next.set(k, String(v))));
    if (!('page' in patch)) next.delete('page');
    setParams(next, { replace: true });
  };
  const list = useQuestions(f);
  const topLevel = topics.data?.filter((t) => !t.parentCode) ?? [];

  return (
    <div className="space-y-4">
      <div>
        <h1 className="page-title">Question bank</h1>
        <p className="muted">Questions students were asked, tagged by topic. Add yours when you post an experience.</p>
      </div>
      <div className="flex flex-wrap gap-1" role="group" aria-label="Topic">
        <button className={`chip ${!f.topic ? 'bg-brand-600 text-white dark:bg-brand-600' : ''}`} onClick={() => set({ topic: undefined })}>All</button>
        {topLevel.map((t) => (
          <button key={t.code} className={`chip ${f.topic === t.code ? 'bg-brand-600 text-white dark:bg-brand-600' : ''}`} onClick={() => set({ topic: t.code })}>
            {t.name}
          </button>
        ))}
      </div>
      {f.topic?.startsWith('dsa') && (
        <div className="flex flex-wrap gap-1">
          {topics.data?.filter((t) => t.parentCode === 'dsa').map((t) => (
            <button key={t.code} className={`chip ${f.topic === t.code ? 'ring-2 ring-brand-500' : ''}`} onClick={() => set({ topic: t.code })}>{t.name}</button>
          ))}
        </div>
      )}
      <div className="grid gap-2 sm:grid-cols-4">
        <input className="input sm:col-span-2" type="search" aria-label="Search questions" placeholder="Search questions…" value={f.q} onChange={(e) => set({ q: e.target.value })} />
        <select className="input" aria-label="Difficulty" value={f.difficulty ?? ''} onChange={(e) => set({ difficulty: e.target.value })}>
          <option value="">Any difficulty</option><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
        </select>
        <select className="input" aria-label="Sort" value={f.sort ?? ''} onChange={(e) => set({ sort: e.target.value })}>
          <option value="">{f.q ? 'Best match' : 'Newest'}</option><option value="top">Most upvoted</option>
        </select>
      </div>
      <QueryState query={list}>
        {(p) =>
          p.content.length === 0 ? <Empty title="No questions found" /> : (
            <>
              <div className="space-y-3">{p.content.map((q) => <QuestionCard key={q.id} q={q} />)}</div>
              <Pagination page={p.page} totalPages={p.totalPages} onPage={(pg) => set({ page: pg })} />
            </>
          )
        }
      </QueryState>
    </div>
  );
}
