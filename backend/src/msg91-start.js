import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const serverPath = path.join(here, 'server.js');
let source = await fs.readFile(serverPath, 'utf8');

const imports = [
  "import { registerMsg91WidgetAuth } from './msg91-widget-auth.js';",
  "import { registerMarketplaceCompletion } from './marketplace-completion.js';",
  "import { registerCartCompletion } from './cart-completion.js';",
  "import { registerSettlementCompletion } from './settlement-completion.js';",
  "import { registerSellerOnboarding } from './seller-onboarding.js';",
  "import { enforceOrderActionReasons } from './order-action-reasons.js';"
];
const cryptoImport = "import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';";
for (const statement of imports) {
  if (!source.includes(statement)) {
    source = source.replace(cryptoImport, `${cryptoImport}\n${statement}`);
  }
}

if (!source.includes("POST /v1/auth/verify-msg91-token DIRECT")) {
  source = source.replace(
    "app.listen(PORT, '0.0.0.0', () => {",
    "await registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone }); // POST /v1/auth/verify-msg91-token DIRECT\n" +
    "await registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit });\n" +
    "await registerCartCompletion({ app, pool, requireRole, audit });\n" +
    "await registerSettlementCompletion({ app, pool, requireRole, audit, razorpay });\n" +
    "await registerSellerOnboarding({ app, pool, requireRole, audit }); // SELLER ONBOARDING API\n" +
    "app.listen(PORT, '0.0.0.0', () => {"
  );
}

if (!source.includes("order-action-reasons.js")) {
  source = enforceOrderActionReasons(source);
}

await fs.writeFile(serverPath, source, 'utf8');
await import('./launcher.js');
