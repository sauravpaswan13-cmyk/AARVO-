FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm install --omit=dev --ignore-scripts
COPY src ./src
COPY schema.sql ./schema.sql
COPY migrations ./migrations
# Make the runtime patcher independent of the old 8080 string and register all runtime modules before server listen.
RUN node --input-type=module -e "import fs from 'node:fs'; const p='src/launcher.js'; let s=fs.readFileSync(p,'utf8'); const marker=\"await fs.writeFile(runtimePath, source, 'utf8');\"; const patch=\`\nif (!source.includes(\"registerMarketplaceCompletion\")) {\n  source = source.replace(\"import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\", \"import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\\nimport { registerMarketplaceCompletion } from './marketplace-completion.js';\\nimport { registerCartCompletion } from './cart-completion.js';\\nimport { registerSettlementCompletion } from './settlement-completion.js';\");\n}\nif (!source.includes(\"msg91-widget-auth.js\")) {\n  source = source.replace(\"import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\", \"import { createHmac, randomBytes, randomUUID, scryptSync, timingSafeEqual } from 'node:crypto';\\nimport { registerMsg91WidgetAuth } from './msg91-widget-auth.js';\");\n}\nconst listenMarker=\"app.listen(PORT, '0.0.0.0', () => {\";\nconst registrations=\"await registerMarketplaceCompletion({ app, pool, requireAuth, requireRole, audit });\\nawait registerCartCompletion({ app, pool, requireRole, audit });\\nawait registerSettlementCompletion({ app, pool, requireRole, audit, razorpay });\\nawait registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone });\\n\";\nif (source.includes(listenMarker) && !source.includes(\"await registerMsg91WidgetAuth({ app, pool, issueToken, normalizePhone });\")) source = source.replace(listenMarker, registrations + listenMarker);\n\`; if (!s.includes(marker)) throw new Error('launcher write marker missing'); s=s.replace(marker, patch+marker); fs.writeFileSync(p,s);"
RUN sed -i "s/interval \\\'1 minute\\'/make_interval(mins => 1)/g" src/server.js \
    && node --check src/server.js \
    && node --check src/marketplace-completion.js \
    && node --check src/launcher.js
RUN chown -R node:node /app
EXPOSE 10000
USER node
# Start HTTP immediately so Render can detect the port while migrations/bootstrap run.
CMD ["sh", "-c", "node src/launcher.js & SERVER_PID=$!; node src/migrate.js && if [ -n \"$ADMIN_EMAIL\" ] && [ -n \"$ADMIN_PASSWORD\" ]; then node src/bootstrap-admin.js; else echo 'Admin bootstrap skipped: ADMIN_EMAIL/ADMIN_PASSWORD not available'; fi; MIGRATE_STATUS=$?; if [ $MIGRATE_STATUS -ne 0 ]; then kill $SERVER_PID 2>/dev/null || true; wait $SERVER_PID 2>/dev/null || true; exit $MIGRATE_STATUS; fi; wait $SERVER_PID"]
