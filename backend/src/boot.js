import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const launcherPath = path.join(here, 'launcher.js');
let launcher = await fs.readFile(launcherPath, 'utf8');

// phone-first-start.js pre-inserts the MSG91 import into server.js. The old
// launcher checked that import to decide whether to register the route, so it
// could skip registration entirely. Always key this check off the actual
// registration call instead.
launcher = launcher.replace(
  "if (!source.includes('msg91-widget-auth.js')) {",
  "if (!source.includes('await registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone });')) {"
);
await fs.writeFile(launcherPath, launcher, 'utf8');
await import('./phone-first-start.js');
