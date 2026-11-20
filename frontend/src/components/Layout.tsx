import { NavLink, Outlet, Link } from 'react-router';
import { useMe } from '../api/hooks';
import { ThemeToggle } from './ThemeToggle';
import { Avatar } from './ui';

export const DISCLAIMER =
  'CampusOA is an independent student project and is not affiliated with or endorsed by CBIT.';

const NAV = [
  { to: '/drives', label: 'Drives', icon: '📅' },
  { to: '/companies', label: 'Companies', icon: '🏢' },
  { to: '/experiences', label: 'Experiences', icon: '💬' },
  { to: '/questions', label: 'Questions', icon: '❓' },
  { to: '/prep', label: 'Prep', icon: '✅' },
  { to: '/leaderboard', label: 'Leaderboard', icon: '🏆' },
];

// Phones get the five most-used destinations in a bottom bar (thumb-reachable).
const MOBILE_NAV = [NAV[0], NAV[1], NAV[2], NAV[4], { to: '/me', label: 'Me', icon: '👤' }];

function linkClass({ isActive }: { isActive: boolean }) {
  return `rounded-lg px-3 py-2 text-sm font-medium ${
    isActive ? 'bg-brand-50 text-brand-700 dark:bg-slate-800 dark:text-white' : 'text-slate-600 hover:text-slate-900 dark:text-slate-300 dark:hover:text-white'
  }`;
}

export function Layout() {
  const { data: me } = useMe();
  const isMod = me?.role === 'MODERATOR' || me?.role === 'ADMIN';

  return (
    <div className="flex min-h-screen flex-col">
      {/* Always-visible "unofficial" banner (ground rule #1). */}
      <div className="bg-amber-50 px-4 py-1 text-center text-xs text-amber-900 dark:bg-amber-950 dark:text-amber-200">
        Unofficial student project · not affiliated with or endorsed by CBIT
      </div>

      <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/90 backdrop-blur dark:border-slate-800 dark:bg-slate-950/90">
        <div className="mx-auto flex max-w-6xl items-center gap-2 px-4 py-2">
          <Link to="/" className="mr-2 flex items-center gap-2 font-bold">
            <img src="/favicon.svg" alt="" width={28} height={28} />
            <span>CampusOA</span>
          </Link>
          <nav className="hidden flex-1 items-center gap-1 md:flex" aria-label="Main">
            {NAV.map((n) => (
              <NavLink key={n.to} to={n.to} className={linkClass}>
                {n.label}
              </NavLink>
            ))}
            {isMod && (
              <NavLink to="/admin" className={linkClass}>
                Admin
              </NavLink>
            )}
          </nav>
          <div className="ml-auto flex items-center gap-1">
            <ThemeToggle />
            {me ? (
              <Link to="/me" className="flex items-center gap-2 rounded-lg p-1 hover:bg-slate-100 dark:hover:bg-slate-800" aria-label="Your profile">
                <Avatar name={me.displayName} url={me.avatarUrl} />
              </Link>
            ) : (
              <Link to="/login" className="btn-primary py-1.5">
                Sign in
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pb-24 pt-4 md:pb-8">
        {me && !me.profileComplete && (
          <div className="mb-4 rounded-xl border border-brand-100 bg-brand-50 p-3 text-sm dark:border-slate-700 dark:bg-slate-900">
            Add your branch and graduation year in <Link to="/me" className="font-medium underline">your profile</Link> to see eligibility and your cohort leaderboard.
          </div>
        )}
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 px-4 py-6 pb-24 text-center text-xs text-slate-500 dark:border-slate-800 md:pb-6">
        <p className="font-medium">{DISCLAIMER}</p>
        <p className="mt-2 space-x-3">
          <Link className="underline" to="/guidelines">Posting guidelines</Link>
          <Link className="underline" to="/privacy">Privacy</Link>
          <a className="underline" href="/api/docs" target="_blank" rel="noopener noreferrer">API docs</a>
          {isMod && <Link className="underline md:hidden" to="/admin">Admin</Link>}
        </p>
        <p className="mt-2">Placement figures are approximate, from public records. Always check the official circular.</p>
      </footer>

      <nav
        className="fixed inset-x-0 bottom-0 z-20 grid grid-cols-5 border-t border-slate-200 bg-white pb-[env(safe-area-inset-bottom)] dark:border-slate-800 dark:bg-slate-950 md:hidden"
        aria-label="Mobile"
      >
        {MOBILE_NAV.map((n) => (
          <NavLink
            key={n.to}
            to={n.to}
            className={({ isActive }) =>
              `flex flex-col items-center py-2 text-[11px] ${isActive ? 'text-brand-600 dark:text-white' : 'text-slate-500'}`
            }
          >
            <span aria-hidden className="text-lg leading-none">{n.icon}</span>
            {n.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
