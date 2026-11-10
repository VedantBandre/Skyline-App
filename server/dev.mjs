import { createServer } from 'node:http';
import { weatherApi } from './weather.mjs';

export function listen(api = weatherApi, port = Number(process.env.PORT ?? 8765)) {
  const routes = { '/api/v1/search': api.search, '/api/v1/forecast': api.forecast };
  return createServer(async (request, response) => {
    try {
      const url = new URL(request.url, 'http://localhost');
      const handler = Object.hasOwn(routes, url.pathname) ? routes[url.pathname] : null;
      const result = handler
        ? await handler(new Request(url, { method: request.method }))
        : Response.json({ error: 'not_found' }, { status: 404 });
      response.writeHead(result.status, Object.fromEntries(result.headers));
      response.end(await result.text());
    } catch {
      response.writeHead(500, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify({ error: 'internal_error' }));
    }
  }).listen(port, '127.0.0.1', () => console.log(`Skyline API at http://127.0.0.1:${port}`));
}

if (import.meta.main) listen();
