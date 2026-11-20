# CampusOA Privacy Notice

> CampusOA is an independent student project and is not affiliated with or endorsed by CBIT.

This page explains what CampusOA stores, why, who can see it, and how to delete it. The same text
appears in the app at **/privacy**.

## What we store and why

| Data | Where it comes from | Why we need it | Who can see it |
|---|---|---|---|
| Name, email, profile photo URL, Google account id (`sub`) | Google sign-in | To identify your account and show your name on posts | Name/photo: everyone. Email: you and admins. |
| Branch, graduation year | You (profile) | Eligibility checks, cohort leaderboard, context on posts | Everyone (shown next to your posts) |
| **CGPA, active backlogs** (optional) | You (profile) | Only to compute "eligible / not eligible" on drives | **Only you.** Not other students, moderators or admins. They live in a separate table (`private_profile`) that no public API reads. |
| Leaderboard opt-in (off by default) | You | To include you in rankings | If on: your name, branch, year and problem count are shown on leaderboards |
| Interview experiences, questions, comments | You | That's the point of the site | Everyone. Anonymous posts hide your name from everyone except moderators. |
| Upvotes, reports | You | Ranking content and moderation | Votes are counted, not shown per person. Moderators see who filed a report. |
| Roadmap ticks, problem log | You | Your prep dashboard and streaks | Only you (plus the count on leaderboards, if you opted in) |
| Session cookie (`SESSION`) and CSRF cookie (`XSRF-TOKEN`) | The server | To keep you signed in for up to 14 days, and to block cross-site request forgery | Nobody else. No advertising or analytics cookies. |
| Server logs | The server | Debugging and security | The maintainers. Logs never contain tokens, CGPA or post contents. |

## What we never do

- **No student data from placement documents.** We never import names, roll numbers or results from
  CBIT's placement PDFs. The scraper reads only the public list of circular titles and links.
- No selling or sharing of data, and no third-party trackers.

## Where it's stored

PostgreSQL on Neon (cloud database, encrypted at rest), accessed only by the CampusOA backend on
Render over TLS. The site is served over HTTPS.

## Deleting your account

Profile → **Delete my account**. This immediately and permanently deletes:

- your account, profile and private details (CGPA/backlogs)
- roadmap progress, problem log and votes
- every active session (you're signed out everywhere)

Posts, questions and comments you wrote **stay up as "Deleted user"**, so the knowledge remains
useful to juniors. If you want them gone too, delete them individually *before* deleting your
account, or ask a moderator.

Database backups (Neon's point-in-time restore) may keep deleted data for up to 7 days before it
expires.

## Contact

Open an issue on the GitHub repository, or ask a moderator in the app.
