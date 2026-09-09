FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm install --omit=dev --ignore-scripts
COPY src ./src
COPY schema.sql ./schema.sql
COPY migrations ./migrations
RUN node --check src/server.js \
    && node --check src/marketplace-completion.js \
    && node --check src/launcher.js \
    && node --check src/phone-first-start.js
RUN chown -R node:node /app
EXPOSE 10000
USER node
# Start HTTP immediately so Render can detect the port while migrations/bootstrap run.
CMD ["sh", "-c", "node src/phone-first-start.js & SERVER_PID=$!; node src/migrate.js && if [ -n \"$ADMIN_EMAIL\" ] && [ -n \"$ADMIN_PASSWORD\" ]; then node src/bootstrap-admin.js; else echo 'Admin bootstrap skipped: ADMIN_EMAIL/ADMIN_PASSWORD not available'; fi; MIGRATE_STATUS=$?; if [ $MIGRATE_STATUS -ne 0 ]; then kill $SERVER_PID 2>/dev/null || true; wait $SERVER_PID 2>/dev/null || true; exit $MIGRATE_STATUS; fi; wait $SERVER_PID"]
