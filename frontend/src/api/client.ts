import type { ApiErrorBody, FieldError } from './types';

/**
 * The one place that talks HTTP. Every request:
 *  - goes to a relative URL (/api/...), so it hits the same origin (Vite proxy locally,
 *    Vercel rewrite in production) and the session cookie is sent automatically;
 *  - on writes, copies the XSRF-TOKEN cookie into the X-XSRF-TOKEN header (CSRF protection);
 *  - turns non-2xx responses into an ApiError carrying the backend's error JSON.
 */

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldError[];

  constructor(status: number, body: Partial<ApiErrorBody>) {
    super(body.message ?? `Request failed (${status})`);
    this.status = status;
    this.code = body.code ?? 'UNKNOWN';
    this.fieldErrors = body.fieldErrors ?? [];
  }

  /** Map of field -> message, handy for showing errors next to form inputs. */
  fieldMap(): Record<string, string> {
    return Object.fromEntries(this.fieldErrors.map((f) => [f.field, f.message]));
  }
}

export function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(name + '='))
    ?.split('=')[1];
}

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export async function request<T>(method: Method, url: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') {
    const token = readCookie('XSRF-TOKEN');
    if (token) headers['X-XSRF-TOKEN'] = decodeURIComponent(token);
  }

  const res = await fetch(url, {
    method,
    headers,
    credentials: 'same-origin',
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const data = text ? safeJson(text) : undefined;
  if (!res.ok) {
    throw new ApiError(res.status, (data as Partial<ApiErrorBody>) ?? { message: res.statusText });
  }
  return data as T;
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return { message: text.slice(0, 200) };
  }
}

/** Build "?a=1&b=x" from an object, skipping empty values. */
export function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const p = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') p.set(k, String(v));
  }
  const s = p.toString();
  return s ? `?${s}` : '';
}

export const api = {
  get: <T>(url: string) => request<T>('GET', url),
  post: <T>(url: string, body?: unknown) => request<T>('POST', url, body ?? {}),
  put: <T>(url: string, body?: unknown) => request<T>('PUT', url, body ?? {}),
  del: <T = void>(url: string) => request<T>('DELETE', url),
};

/** Where the "Sign in with Google" button points. Handled by Spring Security, not React. */
export const GOOGLE_LOGIN_URL = '/oauth2/authorization/google';
