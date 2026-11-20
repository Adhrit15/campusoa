import { Link } from 'react-router';
import { DISCLAIMER } from '../components/Layout';

export function GuidelinesPage() {
  return (
    <article className="prose-md card mx-auto max-w-3xl">
      <h1 className="page-title">Posting & moderation guidelines</h1>
      <p>CampusOA works because seniors share honestly and juniors learn from it. Please keep it that way.</p>
      <h2>Do</h2>
      <ul>
        <li><strong>Describe questions in your own words.</strong> &ldquo;Given a weighted graph, find the cheapest route with at most K stops&rdquo; is perfect.</li>
        <li>Share the format: number of rounds, duration, topics, what the interviewers cared about.</li>
        <li>Share how you prepared and what you&apos;d do differently.</li>
        <li>Be kind in comments. Everyone was a beginner once.</li>
      </ul>
      <h2>Don&apos;t</h2>
      <ul>
        <li><strong>Don&apos;t paste, screenshot or copy confidential test material</strong> (OA question text, test PDFs, answer keys). Companies ask candidates not to, and it can get the college&apos;s drives cancelled.</li>
        <li>Don&apos;t post anyone else&apos;s personal information: names, roll numbers, phone numbers, results.</li>
        <li>No spam, referral-selling, abuse, or anything hateful.</li>
        <li>Don&apos;t present guesses as official information. Link the circular instead.</li>
      </ul>
      <h2>Anonymous posts</h2>
      <p>If you post anonymously, other students won&apos;t see your name, but moderators can, so they can act on abuse.</p>
      <h2>How moderation works</h2>
      <ul>
        <li>Posts go live immediately. Anyone signed in can <strong>Report</strong> a post, question or comment.</li>
        <li>When 3 different people report something, it&apos;s automatically hidden until a moderator reviews it.</li>
        <li>Moderators can dismiss the report, hide, remove or restore content. Every decision is logged.</li>
        <li>Repeated or serious violations can lead to a ban.</li>
      </ul>
      <p className="text-sm">{DISCLAIMER}</p>
    </article>
  );
}

export function PrivacyPage() {
  return (
    <article className="prose-md card mx-auto max-w-3xl">
      <h1 className="page-title">Privacy</h1>
      <p>Short version: we store the minimum needed to run the site, keep your academic details private, and let you delete everything.</p>
      <h2>What we store</h2>
      <ul>
        <li><strong>From Google sign-in:</strong> your name, email address, profile photo URL and Google account id.</li>
        <li><strong>Profile:</strong> branch and graduation year (shown next to your posts), and your leaderboard choice (off by default).</li>
        <li><strong>Private details (optional):</strong> CGPA and active backlogs. Used only to compute your eligibility, and shown only to you. Not to other students, moderators or admins.</li>
        <li><strong>Your activity:</strong> posts, questions, comments, votes, reports, roadmap ticks and problem log.</li>
        <li><strong>A session cookie</strong> that keeps you signed in (up to 14 days), plus a CSRF cookie for security. No advertising or tracking cookies.</li>
      </ul>
      <h2>What we don&apos;t do</h2>
      <ul>
        <li>We never import student names or roll numbers from placement documents. Only company-level aggregates.</li>
        <li>We don&apos;t sell or share your data.</li>
      </ul>
      <h2>Deleting your account</h2>
      <p>
        Go to <Link to="/me">your profile</Link> → <em>Delete my account</em>. This permanently removes your profile,
        private details, prep data and votes, and signs you out everywhere. Posts you wrote remain as &ldquo;Deleted
        user&rdquo;. Delete them individually first if you want them gone.
      </p>
      <p className="text-sm">{DISCLAIMER}</p>
    </article>
  );
}
