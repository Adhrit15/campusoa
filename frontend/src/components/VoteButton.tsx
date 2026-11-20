import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { api } from '../api/client';
import { useMe } from '../api/hooks';

/**
 * Upvote toggle with an optimistic update: the count changes immediately and is rolled back if
 * the request fails. {@code path} is e.g. "/api/experiences/12/vote".
 */
export function VoteButton({ path, votes, votedByMe, disabled }: { path: string; votes: number; votedByMe: boolean; disabled?: boolean }) {
  const { data: me } = useMe();
  const [state, setState] = useState({ votes, voted: votedByMe });
  const m = useMutation({
    mutationFn: (up: boolean) => (up ? api.post(path) : api.del(path)),
    onMutate: (up) => {
      const prev = state;
      setState({ votes: state.votes + (up ? 1 : -1), voted: up });
      return prev;
    },
    onError: (_e, _up, prev) => prev && setState(prev),
  });

  return (
    <button
      type="button"
      className={`inline-flex items-center gap-1 rounded-full border px-3 py-1 text-sm ${
        state.voted ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-slate-800 dark:text-white' : 'border-slate-300 dark:border-slate-700'
      }`}
      disabled={!me || disabled || m.isPending}
      title={!me ? 'Sign in to upvote' : disabled ? "You can't upvote your own post" : 'Upvote'}
      aria-pressed={state.voted}
      onClick={() => m.mutate(!state.voted)}
    >
      ▲ <span>{state.votes}</span>
    </button>
  );
}
