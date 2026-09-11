import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { enforceOrderActionReasons } from './order-action-reasons.js';

const here = path.dirname(fileURLToPath(import.meta.url));
const serverPath = path.join(here, 'server.js');
let source = await fs.readFile(serverPath, 'utf8');

const imports = [
  "import { registerMsg91WidgetAuth } from './msg91-widget-auth.js';",
  "import { registerMarketplaceCompletion } from './marketplace-completion.js';",
  "import { registerCartCompletion } from './cart-completion.js';",
  "import { registerSettlementCompletion } from './settlement-completion.js';",
  "import { registerSellerOnboarding } from './seller-onboarding.js';"
];
const cryptoImport = "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';";
for (const statement of imports) {
  if (!source.includes(statement)) {
    source = source.replace(cryptoImport, `${cryptoImport}\n${statement}`);
  }
}

// Check the executable registration call, not a comment marker. The server's
// startup code currently uses Fastify's object-form listen call.
const msg91Registration = "await registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone });";
if (!source.includes(msg91Registration)) {
  const listenMarker = "const port=Number(process.env.PORT||8080);await app.listen({port,host:'0.0.0.0'});";
  const registrationBlock =
    msg91Registration + "\n" +
    "await registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit });\n" +
    "await registerCartCompletion({ app, pool, requireRole, audit });\n" +
    "await registerSettlementCompletion({ app, pool, requireRole, audit, razorpay });\n" +
    "await registerSellerOnboarding({ app, pool, requireRole, audit }); // SELLER ONBOARDING API\n" +
    listenMarker;
  if (!source.includes(listenMarker)) throw new Error('AARVO server listen marker not found');
  source = source.replace(listenMarker, registrationBlock);
}

source = enforceOrderActionReasons(source);

await fs.writeFile(serverPath, source, 'utf8');
await import('./launcher.js');