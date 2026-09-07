import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const serverPath = path.join(here, 'server.js');
// The compatibility wrapper lives beside server.js so Node resolves package imports
// (fastify, pg, etc.) and relative imports from the correct /app/src module scope.
const runtimePath = path.join(here, '.aarvo-runtime-server.mjs');
let source = await fs.readFile(serverPath, 'utf8');

if (!source.includes('registerMarketplaceCompletion')) {
  source = source.replace(
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';",
    "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\nimport { registerMarketplaceCompletion } from './marketplace-completion.js';\nimport { registerCartCompletion } from './cart-completion.js';"
  );
  source = source.replace(
    "const port=Number(process.env.PORT||8080);",
    "await registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit });\nawait registerCartCompletion({ app, pool, requireRole, audit });\nconst port=Number(process.env.PORT||8080);"
  );
}

await fs.writeFile(runtimePath, source, 'utf8');
await import(`${pathToFileURL(runtimePath).href}?v=${Date.now()}`);
