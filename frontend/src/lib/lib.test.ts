import { afterEach, describe, expect, it, vi } from 'vitest';
import { formatDate, inr, stripMarkdown, timeAgo } from './format';
import { api, ApiError, qs } from '../api/client';
import { mockFetch } from '../test/utils';

describe('format helpers', () => {
  it('formats rupees with Indian digit grouping', () => {
    expect(inr(125000)).toBe('₹1,25,000');
  });

  it('formats calendar dates without timezone drift', () => {
    expect(formatDate('2026-11-20')).toBe('20 Nov 2026');
  });

  it('describes relative time', () => {
    const now = new Date('2026-11-20T10:00:00Z');
    expect(timeAgo('2026-11-20T09:59:30Z', now)).toBe('just now');
    expect(timeAgo('2026-11-20T07:00:00Z', now)).toBe('3 h ago');
  });

  it('strips markdown but keeps highlight markers', () => {
    expect(stripMarkdown('## Prep\n**graphs** and [link](https://x.y) [[dp]]')).toBe('Prep graphs and link [[dp]]');
  });
});

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT';
  });

  it('builds query strings and skips empty values', () => {
    expect(qs({ q: 'dp', page: 0, company: undefined, stage: '' })).toBe('?q=dp&page=0');
    expect(qs({})).toBe('');
  });

  it('sends the CSRF cookie back as X-XSRF-TOKEN on writes only', async () => {
    document.cookie = 'XSRF-TOKEN=abc%3D123';
    const fetchMock = mockFetch({ '/api/x': { ok: true } });
    await api.get('/api/x');
    await api.post('/api/x', { a: 1 });
    const getHeaders = fetchMock.mock.calls[0][1]?.headers as Record<string, string>;
    const postHeaders = fetchMock.mock.calls[1][1]?.headers as Record<string, string>;
    expect(getHeaders['X-XSRF-TOKEN']).toBeUndefined();
    expect(postHeaders['X-XSRF-TOKEN']).toBe('abc=123');
  });

  it('turns error JSON into an ApiError with field errors', async () => {
    mockFetch({
      '/api/bad': () => ({
        status: 400,
        body: { status: 400, code: 'VALIDATION_FAILED', message: 'Request has invalid fields', fieldErrors: [{ field: 'year', message: 'too small' }] },
      }),
    });
    const err = (await api.post('/api/bad', {}).catch((e: unknown) => e)) as ApiError;
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(400);
    expect(err.code).toBe('VALIDATION_FAILED');
    expect(err.fieldMap()).toEqual({ year: 'too small' });
  });

  it('returns undefined for 204 No Content', async () => {
    mockFetch({ '/api/empty': () => ({ status: 204 }) });
    await expect(api.del('/api/empty')).resolves.toBeUndefined();
  });
});
