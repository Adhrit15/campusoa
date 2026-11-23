// Builds REPORT.pdf from REPORT.md:
//   1. markdown -> HTML (marked), with ```mermaid blocks turned into <pre class="mermaid">
//   2. a cover page and a table of contents built from the # and ## headings
//   3. headless Chromium (Playwright) renders the Mermaid diagrams, then prints an A4 PDF with
//      page numbers in the footer.
// Usage:  cd tools/report && npm install && npm run build
import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath, pathToFileURL } from 'node:url';
import path from 'node:path';
import { Marked } from 'marked';
import { chromium } from 'playwright';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '../..');
let md = await readFile(path.join(root, 'REPORT.md'), 'utf8');

// --- front matter (title/subtitle/date) ---
const meta = {};
md = md.replace(/^---\n([\s\S]*?)\n---\n/, (_, body) => {
  for (const line of body.split('\n')) {
    const m = line.match(/^(\w+):\s*"?(.*?)"?$/);
    if (m) meta[m[1]] = m[2];
  }
  return '';
});

// --- markdown -> HTML with heading ids and mermaid blocks ---
const toc = [];
const slug = (t) => t.toLowerCase().replace(/<[^>]+>/g, '').replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
const marked = new Marked({
  renderer: {
    heading({ tokens, depth }) {
      const text = this.parser.parseInline(tokens);
      const id = slug(text);
      if (depth <= 2) toc.push({ depth, text, id });
      return `<h${depth} id="${id}">${text}</h${depth}>`;
    },
    code({ text, lang }) {
      if (lang === 'mermaid') return `<pre class="mermaid">${text.replace(/</g, '&lt;')}</pre>`;
      const esc = text.replace(/&/g, '&amp;').replace(/</g, '&lt;');
      return `<pre><code class="lang-${lang ?? ''}">${esc}</code></pre>`;
    },
  },
});
const body = marked.parse(md);

const tocHtml = toc
  .map((t) => `<li class="toc-${t.depth}"><a href="#${t.id}">${t.text}</a></li>`)
  .join('\n');

const mermaidJs = pathToFileURL(path.join(here, 'node_modules/mermaid/dist/mermaid.min.js')).href;

const html = `<!doctype html><html><head><meta charset="utf-8"><title>${meta.title ?? 'Report'}</title>
<style>
  @page { size: A4; margin: 22mm 18mm 20mm 18mm; }
  body { font-family: "Segoe UI", "Inter", system-ui, sans-serif; font-size: 10.5pt; line-height: 1.55; color: #1e293b; }
  h1 { font-size: 20pt; color: #1e3a8a; border-bottom: 2px solid #c7d2fe; padding-bottom: 4px; page-break-before: always; margin-top: 0; }
  h2 { font-size: 14pt; color: #1e40af; margin-top: 18px; }
  h3 { font-size: 12pt; }
  a { color: #1d4ed8; text-decoration: none; }
  code { font-family: "Cascadia Code", Consolas, monospace; font-size: 9pt; background: #f1f5f9; padding: 1px 3px; border-radius: 3px; }
  pre { background: #0f172a; color: #e2e8f0; padding: 10px 12px; border-radius: 6px; font-size: 8.5pt; overflow: hidden; white-space: pre-wrap; word-break: break-word; page-break-inside: avoid; }
  pre code { background: none; color: inherit; padding: 0; }
  pre.mermaid { background: #fff; color: #000; text-align: center; border: 1px solid #e2e8f0; }
  table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 9pt; page-break-inside: auto; }
  th, td { border: 1px solid #cbd5e1; padding: 4px 6px; vertical-align: top; text-align: left; }
  th { background: #eef2ff; }
  tr { page-break-inside: avoid; }
  blockquote { border-left: 4px solid #f59e0b; background: #fffbeb; margin: 8px 0; padding: 6px 10px; }
  .cover { height: 245mm; display: flex; flex-direction: column; justify-content: center; text-align: center; }
  .cover h1 { page-break-before: avoid; border: none; font-size: 32pt; }
  .cover .sub { font-size: 14pt; color: #475569; margin-top: 8px; }
  .cover .meta { margin-top: 40px; color: #64748b; }
  .cover .disclaimer { margin-top: 60px; font-size: 9.5pt; color: #92400e; background: #fffbeb; padding: 8px; border-radius: 6px; }
  .toc { page-break-before: always; }
  .toc h1 { page-break-before: avoid; }
  .toc ul { list-style: none; padding: 0; }
  .toc .toc-1 { font-weight: 600; margin-top: 6px; }
  .toc .toc-2 { margin-left: 18px; font-size: 9.5pt; }
</style></head><body>
<section class="cover">
  <h1>${meta.title ?? 'CampusOA'}</h1>
  <div class="sub">${meta.subtitle ?? ''}</div>
  <div class="meta">${meta.date ?? ''}<br>github.com/Adhrit15/campusoa · campusoa.vercel.app</div>
  <div class="disclaimer">CampusOA is an independent student project and is not affiliated with or endorsed by CBIT.</div>
</section>
<section class="toc"><h1 id="toc">Contents</h1><ul>${tocHtml}</ul></section>
${body}
<script src="${mermaidJs}"></script>
</body></html>`;

const htmlPath = path.join(here, 'report.html');
await writeFile(htmlPath, html);

const browser = await chromium.launch();
const page = await browser.newPage();
await page.goto(pathToFileURL(htmlPath).href, { waitUntil: 'load' });
await page.evaluate(async () => {
  // eslint-disable-next-line no-undef
  mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'strict' });
  // eslint-disable-next-line no-undef
  await mermaid.run({ querySelector: 'pre.mermaid' });
});
const failed = await page.locator('pre.mermaid:not(:has(svg)), pre.mermaid:has-text("Syntax error")').count();
if (failed) { console.error(`${failed} mermaid diagram(s) failed to render`); process.exit(1); }

await page.pdf({
  path: path.join(root, 'REPORT.pdf'),
  format: 'A4',
  printBackground: true,
  displayHeaderFooter: true,
  headerTemplate: '<div></div>',
  footerTemplate:
    '<div style="width:100%;font-size:8px;color:#64748b;padding:0 18mm;display:flex;justify-content:space-between">' +
    '<span>CampusOA: Build &amp; Learning Report</span><span><span class="pageNumber"></span> / <span class="totalPages"></span></span></div>',
  margin: { top: '22mm', bottom: '20mm', left: '18mm', right: '18mm' },
});
await browser.close();
console.log(`REPORT.pdf written (${toc.length} TOC entries, ${await Promise.resolve(failed)} diagram failures)`);
