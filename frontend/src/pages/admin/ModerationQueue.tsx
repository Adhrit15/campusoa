import { useState } from 'react';
import { Link } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../../api/client';
import { useModActions, useModQueue } from '../../api/hooks';
import { Empty, Pagination, QueryState } from '../../components/ui';
import { timeAgo } from '../../lib/format';

type Decision = 'DISMISS' | 'HIDE' | 'REMOVE' | 'RESTORE';

export function ModerationQueue() {
  const [status, setStatus] = useState('OPEN');
  const [page, setPage] = useState(0);
  const queue = useModQueue(status, page);
  const qc = useQueryClient();
  const [notes, setNotes] = useState<Record<number, string>>({});
  const resolve = useMutation({
    mutationFn: ({ id, decision }: { id: number; decision: Decision }) =>
      api.post(`/api/mod/reports/${id}/resolve`, { decision, note: notes[id] || undefined }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['mod-queue'] });
      qc.invalidateQueries({ queryKey: ['mod-actions'] });
    },
  });

  return (
    <div className="space-y-3">
      <select className="input w-auto" aria-label="Report status" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
        <option value="OPEN">Open</option><option value="RESOLVED">Resolved</option><option value="DISMISSED">Dismissed</option>
      </select>
      <QueryState query={queue}>
        {(p) =>
          p.content.length === 0 ? <Empty title="Queue is empty 🎉" /> : (
            <>
              <ul className="space-y-3">
                {p.content.map((r) => (
                  <li key={r.reportId} className="card space-y-2">
                    <div className="flex flex-wrap items-center gap-2 text-xs">
                      <span className="chip">{r.targetType.toLowerCase()} #{r.targetId}</span>
                      <span className="chip bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300">{r.reason.replace('_', ' ').toLowerCase()}</span>
                      <span className="chip">{r.targetStatus?.toLowerCase()}</span>
                      <span className="muted">{r.openReportsForTarget} open report(s) · by {r.reporterName ?? 'deleted user'} · {timeAgo(r.createdAt)}</span>
                    </div>
                    <p className="whitespace-pre-line text-sm">{r.preview}</p>
                    {r.details && <p className="text-sm italic">&ldquo;{r.details}&rdquo;</p>}
                    {r.experienceId && <Link className="text-xs underline" to={`/experiences/${r.experienceId}`} target="_blank">Open in context</Link>}
                    {status === 'OPEN' && (
                      <div className="flex flex-wrap items-center gap-2">
                        <input className="input flex-1" placeholder="Note (optional)" value={notes[r.reportId] ?? ''}
                          onChange={(e) => setNotes({ ...notes, [r.reportId]: e.target.value })} />
                        <button className="btn-ghost" onClick={() => resolve.mutate({ id: r.reportId, decision: 'DISMISS' })}>Dismiss</button>
                        <button className="btn-ghost" onClick={() => resolve.mutate({ id: r.reportId, decision: 'HIDE' })}>Hide</button>
                        <button className="btn-danger" onClick={() => resolve.mutate({ id: r.reportId, decision: 'REMOVE' })}>Remove</button>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
              <Pagination page={p.page} totalPages={p.totalPages} onPage={setPage} />
            </>
          )
        }
      </QueryState>
    </div>
  );
}

export function AuditLog() {
  const [page, setPage] = useState(0);
  const actions = useModActions(page);
  return (
    <QueryState query={actions}>
      {(p) => (
        <div className="card">
          <ul className="divide-y divide-slate-100 text-sm dark:divide-slate-800">
            {p.content.length === 0 && <li className="muted py-2">No actions yet.</li>}
            {p.content.map((a) => (
              <li key={a.id} className="py-2">
                <strong>{a.action}</strong> {a.targetType?.toLowerCase()} #{a.targetId} by {a.moderatorName} · {timeAgo(a.createdAt)}
                {a.note && <span className="muted"> · {a.note}</span>}
              </li>
            ))}
          </ul>
          <Pagination page={p.page} totalPages={p.totalPages} onPage={setPage} />
        </div>
      )}
    </QueryState>
  );
}
