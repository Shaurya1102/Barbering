# Barbering frontend

The redesigned Barbering application lives in this Next.js source tree.

## Local development

From this directory, run:

```powershell
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The API defaults to
`http://localhost:8080`; set `NEXT_PUBLIC_API_URL` only when the backend uses a
different URL.

The standard `dev`, `build`, and `start` scripts all use the default `.next`
directory. Use them one at a time because they share that directory. The
optional `BARBERING_NEXT_DIST_DIR` setting is only for an isolated preview
that needs a separate output directory; it is not needed for normal local
development or production.

For lint, type checks, and a production build, run `npm run lint`,
`npm run typecheck`, and `npm run build` from this directory.
