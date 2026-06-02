# Play4Change — Web

React portal for Play4Change: public landing page and restricted admin portal.

## Prerequisites

- Node 20+
- Play4Change REST server running on `http://localhost:8080` (see [root README](../README.md))

## Getting Started

```bash
npm install
npm run dev     # http://localhost:5173
```

## Scripts

| Script | Description |
|---|---|
| `npm run dev` | Start Vite dev server with hot-reload |
| `npm run build` | Type-check and emit production build to `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run ESLint |

## Environment Variables

Create a `.env.local` file in this directory:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Pages

**`/`** — Public landing page. No authentication. Describes the platform and its alignment with UN SDGs 4, 11, and 13.

**`/admin`** — Admin portal. Requires an `ADMIN`-role account authenticated via magic link.

| Feature | Description |
|---|---|
| Topic management | Create topics from a URL or PDF; monitor async AI generation status |
| Content review | Approve or reject Mistral-generated tasks before they reach citizens |
| Metrics | Live view of domain metrics — badge distribution, task submission rates, struggle trigger rate, AI generation latency |

## First Admin User

Promote an existing user from the repo root:

```bash
./scripts/promote-admin.sh your@email.com
```

The change takes effect on the user's next login.

## Production

In production, Nginx serves `dist/` on port 80 via Docker Compose. The `dist/` directory is committed to the repository so the container can be built without a Node build step.

```bash
npm run build   # outputs to dist/
```
