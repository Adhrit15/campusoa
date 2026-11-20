import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../../api/client';
import { useScrapeRuns } from '../../api/hooks';
import { QueryState } from '../../components/ui';
import { timeAgo } from '../../lib/format';

export function AdminScraper() {
  const runs = useScrapeRuns();
  const qc = useQueryClient();
  const run = useMutation({
    mutationFn: () => api.post('/api/admin/scraper/run'),
    onSettled: () => {
      qc.invalidateQueries({ queryKey: ['scrape-runs'] });
      qc.invalidateQueries({ queryKey: ['drives'] });
    },
  });
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-3">
        <button className="btn-primary" disabled={run.isPending} onClick={() => run.mutate()}>{run.isPending ? 'Running…' : 'Run scraper now'}</button>
        <span className="muted">Runs automatically every 6 h. Manual runs are limited to one per 10 min to stay polite.</span>
      </div>
      {run.error && <p className="text-sm text-rose-600">{(run.error as ApiError).message}</p>}
      <QueryState query={runs}>
        {(p) => (
          <ul className="card divide-y divide-slate-100 p-0 text-sm dark:divide-slate-800">
            {p.content.length === 0 && <li className="muted p-3">No runs yet.</li>}
            {p.content.map((r) => (
              <li key={r.id} className="flex flex-wrap items-center gap-2 px-3 py-2">
                <span className={`chip ${r.status === 'FAILED' ? 'bg-rose-100 text-rose-800' : r.status === 'SUCCESS' ? 'bg-emerald-100 text-emerald-800' : ''}`}>{r.status}</span>
                <span>{timeAgo(r.startedAt)}</span>
                <span className="muted">HTTP {r.httpStatus ?? '–'} · {r.itemsSeen} seen · {r.itemsNew} new</span>
                {r.error && <span className="w-full text-xs text-rose-600">{r.error}</span>}
              </li>
            ))}
          </ul>
        )}
      </QueryState>
    </div>
  );
}
