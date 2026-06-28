import http from 'node:http';
import { chromium } from 'playwright';

const PORT = process.env.PORT ?? 3000;
const NAVIGATION_TIMEOUT_MS = 25_000;
const SCRAPE_TIMEOUT_MS = 35_000;

let browser;

async function ensureBrowser() {
  if (!browser?.isConnected()) {
    browser = await chromium.launch({
      args: ['--no-sandbox', '--disable-dev-shm-usage', '--disable-gpu'],
    });
  }
  return browser;
}

async function scrape(url) {
  const br = await ensureBrowser();
  const page = await br.newPage();
  try {
    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: NAVIGATION_TIMEOUT_MS });
    // Best-effort wait for SPA data fetches; ignore if the page keeps long-polling.
    await page.waitForLoadState('networkidle', { timeout: 5_000 }).catch(() => {});
    const text = await page.evaluate(() => {
      document.querySelectorAll(
        'nav, footer, header, aside, script, style, noscript, iframe'
      ).forEach(el => el.remove());
      return document.body?.innerText?.trim() ?? '';
    });
    return text.replace(/[ \t]{2,}/g, ' ').replace(/\n{3,}/g, '\n\n');
  } finally {
    await page.close();
  }
}

const server = http.createServer((req, res) => {
  if (req.method === 'GET' && req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ status: 'ok' }));
    return;
  }

  if (req.method !== 'POST' || req.url !== '/scrape') {
    res.writeHead(404);
    res.end();
    return;
  }

  let body = '';
  req.on('data', chunk => { body += chunk; });
  req.on('end', async () => {
    let url;
    try {
      ({ url } = JSON.parse(body));
    } catch {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'invalid json' }));
      return;
    }
    if (!url || typeof url !== 'string') {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'missing url field' }));
      return;
    }
    try {
      const text = await Promise.race([
        scrape(url),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('scrape timeout')), SCRAPE_TIMEOUT_MS)
        ),
      ]);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ text }));
    } catch (err) {
      res.writeHead(502, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: err.message }));
    }
  });
});

server.listen(PORT, () => console.log(`scraper ready on :${PORT}`));

process.on('SIGTERM', async () => {
  server.close();
  await browser?.close();
});
