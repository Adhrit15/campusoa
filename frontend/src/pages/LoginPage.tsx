import { useSearchParams } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import { api, GOOGLE_LOGIN_URL } from '../api/client';

const ERRORS: Record<string, string> = {
  domain_not_allowed: 'Only college email addresses can sign in. Use your college Google account.',
  email_unverified: 'Your Google account email is not verified.',
  account_banned: 'This account has been suspended. Contact the moderators if you think this is a mistake.',
};

interface DemoUser {
  id: number;
  displayName: string;
  role: string;
}

/** Spring Security redirects here (?error=CODE) when Google sign-in is rejected. */
export function LoginPage() {
  const [params] = useSearchParams();
  const error = params.get('error');
  // Only answers in the backend's "dev" profile; in production this 404s and nothing is shown.
  const demo = useQuery({ queryKey: ['dev-users'], queryFn: () => api.get<DemoUser[]>('/api/dev/users'), retry: false });

  const devLogin = async (id: number) => {
    await api.post(`/api/dev/login/${id}`);
    window.location.href = '/';
  };

  return (
    <div className="card mx-auto max-w-md space-y-4 text-center">
      <h1 className="text-xl font-semibold">Sign in to CampusOA</h1>
      {error && (
        <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-800 dark:bg-rose-950 dark:text-rose-200">
          {ERRORS[error] ?? 'Sign-in failed. Please try again.'}
        </p>
      )}
      <a href={GOOGLE_LOGIN_URL} className="btn-primary w-full">Sign in with Google</a>
      <p className="muted">We only receive your name, email and profile photo from Google.</p>

      {demo.data && demo.data.length > 0 && (
        <div className="rounded-xl border border-dashed border-amber-400 p-3 text-left">
          <p className="text-sm font-semibold text-amber-700 dark:text-amber-300">Local dev only: sign in as a demo user</p>
          <div className="mt-2 flex flex-wrap gap-2">
            {demo.data.map((u) => (
              <button key={u.id} className="btn-ghost py-1 text-xs" onClick={() => devLogin(u.id)}>
                {u.displayName} ({u.role.toLowerCase()})
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
