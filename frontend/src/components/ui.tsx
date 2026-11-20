import type { ReactNode } from 'react';
import { ApiError } from '../api/client';

/** Small presentational building blocks shared by every page. */

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center gap-2 py-8 text-sm text-slate-500" aria-live="polite">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
      {label}
    </div>
  );
}

export function ErrorBox({ error }: { error: unknown }) {
  const msg =
    error instanceof ApiError
      ? error.message
      : 'Could not reach the server. If the site was idle it may be waking up: try again in ~30 seconds.';
  return (
    <div role="alert" className="rounded-xl border border-rose-300 bg-rose-50 p-3 text-sm text-rose-800 dark:border-rose-900 dark:bg-rose-950 dark:text-rose-200">
      {msg}
    </div>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="card text-center">
      <p className="font-medium">{title}</p>
      {children && <div className="muted mt-1">{children}</div>}
    </div>
  );
}

/** Render loading / error / content for a query in one line. */
export function QueryState<T>({
  query,
  children,
}: {
  query: { isPending: boolean; error: unknown; data: T | undefined };
  children: (data: T) => ReactNode;
}) {
  if (query.isPending) return <Spinner />;
  if (query.error) return <ErrorBox error={query.error} />;
  return <>{children(query.data as T)}</>;
}

export function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="mt-4 flex items-center justify-between" aria-label="Pagination">
      <button className="btn-ghost" disabled={page === 0} onClick={() => onPage(page - 1)}>
        ← Previous
      </button>
      <span className="muted">
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn-ghost" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
        Next →
      </button>
    </nav>
  );
}

export function SourceNote({ note, url }: { note?: string; url?: string }) {
  if (!note) return null;
  return (
    <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
      {url ? (
        <a className="underline" href={url} target="_blank" rel="noopener noreferrer">
          {note}
        </a>
      ) : (
        note
      )}
    </p>
  );
}

export function Avatar({ name, url, size = 28 }: { name?: string; url?: string; size?: number }) {
  if (url) {
    return <img src={url} alt="" width={size} height={size} referrerPolicy="no-referrer" className="rounded-full" />;
  }
  const initials = (name ?? '?').split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase();
  return (
    <span
      aria-hidden
      style={{ width: size, height: size }}
      className="inline-flex items-center justify-center rounded-full bg-slate-200 text-[10px] font-semibold text-slate-600 dark:bg-slate-700 dark:text-slate-200"
    >
      {initials}
    </span>
  );
}

export function Field({ label, error, children, hint }: { label: string; error?: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="label">{label}</span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-slate-500">{hint}</span>}
      {error && <span className="mt-1 block text-xs text-rose-600">{error}</span>}
    </label>
  );
}
