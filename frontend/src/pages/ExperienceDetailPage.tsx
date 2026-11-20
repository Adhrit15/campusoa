import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, ApiError } from '../api/client';
import { useComments, useExperience, useMe } from '../api/hooks';
import { Markdown } from '../components/Markdown';
import { ReportButton } from '../components/ReportButton';
import { VoteButton } from '../components/VoteButton';
import { Avatar, QueryState } from '../components/ui';
import { OUTCOME_LABEL, timeAgo } from '../lib/format';
import { Difficulty } from './ExperiencesPage';
import { QuestionCard } from './QuestionsPage';

export function ExperienceDetailPage() {
  const id = Number(useParams().id);
  const exp = useExperience(id);
  const navigate = useNavigate();
  const qc = useQueryClient();
  const del = useMutation({
    mutationFn: () => api.del(`/api/experiences/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['experiences'] });
      navigate('/experiences');
    },
  });

  return (
    <QueryState query={exp}>
      {(e) => (
        <article className="mx-auto max-w-3xl space-y-4">
          <Link to="/experiences" className="text-sm underline">← Experiences</Link>
          {e.status !== 'PUBLISHED' && (
            <div className="rounded-xl bg-amber-100 p-3 text-sm text-amber-900 dark:bg-amber-950 dark:text-amber-200">
              This post is {e.status.toLowerCase()} pending moderator review. Only you and moderators can see it.
            </div>
          )}
          <header className="card">
            <h1 className="page-title">
              <Link to={`/companies/${e.company.slug}`} className="hover:underline">{e.company.name}</Link>
            </h1>
            <p className="muted">{e.roleTitle} · {e.year} · {OUTCOME_LABEL[e.outcome]}</p>
            <div className="mt-3 flex flex-wrap items-center gap-3 text-sm">
              {e.author ? (
                <span className="flex items-center gap-2"><Avatar name={e.author.displayName} url={e.author.avatarUrl} />{e.author.displayName}{e.anonymous && <span className="chip">posted anonymously</span>}</span>
              ) : (
                <span className="muted">{e.anonymous ? 'Anonymous' : 'Deleted user'}</span>
              )}
              <Difficulty value={e.difficulty} />
              <span className="muted">{timeAgo(e.createdAt)}</span>
              <span className="ml-auto flex items-center gap-3">
                <VoteButton path={`/api/experiences/${e.id}/vote`} votes={e.votes} votedByMe={e.votedByMe} disabled={e.mine} />
                {!e.mine && <ReportButton targetType="EXPERIENCE" targetId={e.id} />}
                {e.mine && (
                  <button className="text-xs text-rose-600 underline" onClick={() => confirm('Delete this post permanently?') && del.mutate()}>
                    Delete
                  </button>
                )}
              </span>
            </div>
          </header>

          {e.rounds.length > 0 && (
            <section className="card">
              <h2 className="mb-2 font-semibold">Rounds</h2>
              <ol className="space-y-2">
                {e.rounds.map((r) => (
                  <li key={r.position} className="text-sm">
                    <span className="font-medium">{r.position}. {r.name}</span>
                    {r.description && <span className="muted block whitespace-pre-line">{r.description}</span>}
                  </li>
                ))}
              </ol>
            </section>
          )}

          <section className="card"><Markdown>{e.bodyMd}</Markdown></section>

          {e.questions.length > 0 && (
            <section>
              <h2 className="mb-2 text-lg font-semibold">Questions</h2>
              <div className="space-y-3">{e.questions.map((q) => <QuestionCard key={q.id} q={q} />)}</div>
            </section>
          )}

          <Comments experienceId={e.id} />
        </article>
      )}
    </QueryState>
  );
}

function Comments({ experienceId }: { experienceId: number }) {
  const { data: me } = useMe();
  const comments = useComments(experienceId);
  const qc = useQueryClient();
  const [body, setBody] = useState('');
  const add = useMutation({
    mutationFn: () => api.post(`/api/experiences/${experienceId}/comments`, { body }),
    onSuccess: () => {
      setBody('');
      qc.invalidateQueries({ queryKey: ['comments', experienceId] });
    },
  });
  const remove = useMutation({
    mutationFn: (id: number) => api.del(`/api/comments/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', experienceId] }),
  });

  return (
    <section className="card">
      <h2 className="mb-3 font-semibold">Comments</h2>
      <QueryState query={comments}>
        {(p) => (
          <ul className="space-y-3">
            {p.content.length === 0 && <li className="muted">No comments yet.</li>}
            {p.content.map((c) => (
              <li key={c.id} className="text-sm">
                <div className="flex items-center gap-2 text-xs text-slate-500">
                  <Avatar name={c.author?.displayName} url={c.author?.avatarUrl} size={18} />
                  {c.author?.displayName ?? 'Deleted user'} · {timeAgo(c.createdAt)}
                  <span className="ml-auto">
                    {c.mine ? (
                      <button className="underline" onClick={() => remove.mutate(c.id)}>Delete</button>
                    ) : (
                      <ReportButton targetType="COMMENT" targetId={c.id} />
                    )}
                  </span>
                </div>
                <p className="mt-1 whitespace-pre-line">{c.body}</p>
              </li>
            ))}
          </ul>
        )}
      </QueryState>
      {me && (
        <form className="mt-4 space-y-2" onSubmit={(ev) => { ev.preventDefault(); if (body.trim()) add.mutate(); }}>
          <textarea className="input" rows={2} maxLength={2000} placeholder="Add a comment…" aria-label="Comment" value={body} onChange={(ev) => setBody(ev.target.value)} />
          {add.error && <p className="text-xs text-rose-600">{add.error instanceof ApiError ? add.error.message : 'Failed'}</p>}
          <button className="btn-primary" disabled={add.isPending || !body.trim()}>Post comment</button>
        </form>
      )}
    </section>
  );
}
