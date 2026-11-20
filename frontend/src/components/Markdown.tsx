import ReactMarkdown from 'react-markdown';
import rehypeSanitize from 'rehype-sanitize';

/**
 * Renders user-written markdown SAFELY. react-markdown never uses dangerouslySetInnerHTML, and
 * rehype-sanitize strips anything outside a safe allow-list (no <script>, no onclick=, no
 * javascript: links). That's our defence against stored XSS in posts.
 */
export function Markdown({ children }: { children: string }) {
  return (
    <div className="prose-md break-words">
      <ReactMarkdown
        rehypePlugins={[rehypeSanitize]}
        components={{
          a: ({ href, children: c }) => (
            <a href={href} target="_blank" rel="noopener noreferrer nofollow">
              {c}
            </a>
          ),
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}

/** Search snippets come back as "...the [[graph]] round..."; highlight the [[...]] parts. */
export function Highlight({ text }: { text: string }) {
  const parts = text.split(/(\[\[.*?\]\])/g);
  return (
    <>
      {parts.map((p, i) =>
        p.startsWith('[[') && p.endsWith(']]') ? (
          <mark key={i} className="rounded bg-yellow-200 px-0.5 text-inherit dark:bg-yellow-700/60">
            {p.slice(2, -2)}
          </mark>
        ) : (
          <span key={i}>{p}</span>
        ),
      )}
    </>
  );
}
