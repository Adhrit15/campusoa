import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router';
import { vi } from 'vitest';

/** Render with the same providers as the real app, but a fresh cache and an in-memory router. */
export function renderWithProviders(ui: ReactElement, { route = '/' } = {}) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return {
    client,
    ...render(
      <QueryClientProvider client={client}>
        <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
      </QueryClientProvider>,
    ),
  };
}

/**
 * Replace window.fetch with a fake that answers by URL prefix. Tests stay fast and
 * deterministic: no backend needed.
 */
export function mockFetch(routes: Record<string, unknown | ((init?: RequestInit) => { status: number; body?: unknown })>) {
  const fn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input.toString();
    const key = Object.keys(routes).sort((a, b) => b.length - a.length).find((k) => url.startsWith(k));
    if (!key) return new Response(JSON.stringify({ code: 'NOT_FOUND', message: `no mock for ${url}` }), { status: 404 });
    const r = routes[key];
    const { status, body } = typeof r === 'function' ? (r as (i?: RequestInit) => { status: number; body?: unknown })(init) : { status: 200, body: r };
    return new Response(status === 204 ? null : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
  });
  vi.stubGlobal('fetch', fn);
  return fn;
}
