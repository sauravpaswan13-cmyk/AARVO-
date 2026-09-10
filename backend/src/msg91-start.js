import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const serverPath = path.join(here, 'server.js');
let source = await fs.readFile(serverPath, 'utf8');

if (!source.includes("import { registerMsg91WidgetAuth } from './msg91-widget-auth.js';")) {
  source = source.replace(
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';",
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\nimport { registerMsg91WidgetAuth } from './msg91-widget-auth.js';"
  );
}

if (!source.includes("POST /v1/auth/verify-msg91-token DIRECT")) {
  const route = "\nawait registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone }); // POST /v1/auth/verify-msg91-token DIRECT\n";
  const listenIndex = source.indexOf('app.listen(');
  if (listenIndex >= 0) source = source.slice(0, listenIndex) + route + source.slice(listenIndex);
}

await fs.writeFile(serverPath, source, 'utf8');
await import('./launcher.js');
