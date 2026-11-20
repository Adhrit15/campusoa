import type { ReactNode } from 'react';
import { useMe } from '../api/hooks';
import { GOOGLE_LOGIN_URL } from '../api/client';
import type { Role } from '../api/types';
import { Spinner } from './ui';

/**
 * Hides a page from signed-out users (or users without the role). This is only for UX: the
 * REAL protection is on the server (SecurityConfig + @PreAuthorize). Never rely on the client.
 */
export function RequireAuth({ children, role }: { children: ReactNode; role?: Role }) {
  const { data: me, isPending } = useMe();
  if (isPending) return <Spinner />;
  if (!me) {
    return (
      <div className="card mx-auto max-w-md text-center">
        <h1 className="text-lg font-semibold">Sign in to continue</h1>
        <p className="muted mt-1">Use your college Google account.</p>
        <a href={GOOGLE_LOGIN_URL} className="btn-primary mt-4">Sign in with Google</a>
      </div>
    );
  }
  const allowed =
    !role || me.role === 'ADMIN' || (role === 'MODERATOR' && me.role === 'MODERATOR') || role === 'STUDENT';
  if (!allowed) return <div className="card">You don&apos;t have access to this page.</div>;
  return <>{children}</>;
}
