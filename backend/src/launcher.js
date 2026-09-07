import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const serverPath = path.join(here, 'server.js');
// The image runs as the unprivileged `node` user, so /app/src is not writable.
// Keep the generated compatibility wrapper in the writable runtime temp area.
const runtimePath = path.join('/tmp', '.aarvo-runtime-server.mjs');
let source = await fs.readFile(serverPath, 'utf8');

if (!source.includes('registerMarketplaceCompletion')) {
  source = source.replace(
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';",
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\nimport { registerMarketplaceCompletion } from './marketplace-completion.js';"
  );
  source = source.replace(
    "const port=Number(process.env.PORT||8080);",
    "await registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit });\nconst port=Number(process.env.PORT||8080);"
  );
}

await fs.writeFile(runtimePath, source, 'utf8');
await import(`${pathToFileURL(runtimePath).href}?v=${Date.now()}`);
