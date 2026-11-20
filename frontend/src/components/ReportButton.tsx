import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { api, ApiError } from '../api/client';
import type { ContentTarget, ReportReason } from '../api/types';
import { useMe } from '../api/hooks';

const REASONS: { value: ReportReason; label: string }[] = [
  { value: 'CONFIDENTIAL', label: 'Copies confidential test material' },
  { value: 'PERSONAL_INFO', label: "Shares someone's personal information" },
  { value: 'ABUSIVE', label: 'Abusive or hateful' },
  { value: 'SPAM', label: 'Spam or advertising' },
  { value: 'OFF_TOPIC', label: 'Off-topic' },
  { value: 'OTHER', label: 'Something else' },
];

export function ReportButton({ targetType, targetId }: { targetType: ContentTarget; targetId: number }) {
  const { data: me } = useMe();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState<ReportReason>('CONFIDENTIAL');
  const [details, setDetails] = useState('');
  const m = useMutation({
    mutationFn: () => api.post('/api/reports', { targetType, targetId, reason, details: details || undefined }),
  });

  if (!me) return null;
  if (m.isSuccess) return <span className="text-xs text-emerald-600">Reported. Thanks, a moderator will review it.</span>;

  return (
    <span className="relative inline-block">
      <button type="button" className="text-xs text-slate-500 underline hover:text-rose-600" onClick={() => setOpen(!open)}>
        Report
      </button>
      {open && (
        <div className="card absolute right-0 z-30 mt-2 w-72 space-y-2 text-left">
          <p className="text-sm font-medium">Why are you reporting this?</p>
          {REASONS.map((r) => (
            <label key={r.value} className="flex items-center gap-2 text-sm">
              <input type="radio" name={`reason-${targetType}-${targetId}`} checked={reason === r.value} onChange={() => setReason(r.value)} />
              {r.label}
            </label>
          ))}
          <textarea className="input" rows={2} maxLength={500} placeholder="Details (optional)" value={details} onChange={(e) => setDetails(e.target.value)} />
          {m.error && <p className="text-xs text-rose-600">{m.error instanceof ApiError ? m.error.message : 'Failed'}</p>}
          <div className="flex justify-end gap-2">
            <button type="button" className="btn-ghost py-1" onClick={() => setOpen(false)}>Cancel</button>
            <button type="button" className="btn-danger py-1" disabled={m.isPending} onClick={() => m.mutate()}>Send report</button>
          </div>
        </div>
      )}
    </span>
  );
}
