import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { EligibilityBadge } from './EligibilityBadge';
import { Highlight, Markdown } from './Markdown';

describe('EligibilityBadge', () => {
  it('renders nothing when signed out (no eligibility)', () => {
    const { container } = render(<EligibilityBadge eligibility={undefined} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('shows the status label for each state', () => {
    const { rerender } = render(<EligibilityBadge eligibility={{ status: 'ELIGIBLE', reasons: [] }} />);
    expect(screen.getByText('Eligible')).toBeInTheDocument();
    rerender(<EligibilityBadge eligibility={{ status: 'UNKNOWN', reasons: [] }} />);
    expect(screen.getByText('Criteria unknown')).toBeInTheDocument();
  });

  it('lists reasons when asked', () => {
    render(<EligibilityBadge showReasons eligibility={{ status: 'NOT_ELIGIBLE', reasons: ['Minimum CGPA is 7.5', 'No active backlogs allowed'] }} />);
    expect(screen.getByText('Not eligible')).toBeInTheDocument();
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
  });
});

describe('Highlight', () => {
  it('wraps [[...]] search matches in <mark>', () => {
    render(<p>Highlight: <Highlight text="the [[graph]] round and [[DP]]" /></p>);
    const marks = document.querySelectorAll('mark');
    expect([...marks].map((m) => m.textContent)).toEqual(['graph', 'DP']);
  });
});

describe('Markdown', () => {
  it('renders formatting but strips dangerous HTML (XSS defence)', () => {
    render(<Markdown>{'**bold** <script>alert(1)</script> <img src=x onerror="alert(1)"> [x](javascript:alert(1))'}</Markdown>);
    expect(screen.getByText('bold').tagName).toBe('STRONG');
    expect(document.querySelector('script')).toBeNull();
    expect(document.querySelector('img[onerror]')).toBeNull();
    const link = screen.getByText('x').closest('a');
    expect(link?.getAttribute('href') ?? '').not.toMatch(/^javascript:/);
  });
});
