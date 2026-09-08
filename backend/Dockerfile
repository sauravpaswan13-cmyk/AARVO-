FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm install --omit=dev --ignore-scripts
COPY src ./src
COPY schema.sql ./schema.sql
COPY migrations ./migrations
# Phone-first auth is part of the committed server.js; do not rewrite production source during the Docker build.
# Fix legacy escaped SQL interval and the launcher's generated OTP SQL, then verify production JavaScript sources.
RUN sed -i "s/interval \\\'1 minute\\\'/make_interval(mins => 1)/g" src/server.js \
    && sed -i "s/interval \\\\\\\'10 minutes\\\\\\\'/make_interval(mins => 10)/g" src/launcher.js \
    && node --check src/server.js \
    && node --check src/marketplace-completion.js \
    && node --check src/launcher.js
# The launcher generates a compatibility wrapper beside server.js; make the app tree writable by the runtime user.
RUN chown -R node:node /app
EXPOSE 8080
USER node
# Start the HTTP server immediately so Render can detect the port while migrations/bootstrap run.
# If migrations fail, terminate the server and fail the deploy; otherwise keep the server process attached.
CMD ["sh", "-c", "node src/launcher.js & SERVER_PID=$!; node src/migrate.js && if [ -n \"$ADMIN_EMAIL\" ] && [ -n \"$ADMIN_PASSWORD\" ]; then node src/bootstrap-admin.js; else echo 'Admin bootstrap skipped: ADMIN_EMAIL/ADMIN_PASSWORD not available'; fi; MIGRATE_STATUS=$?; if [ $MIGRATE_STATUS -ne 0 ]; then kill $SERVER_PID 2>/dev/null || true; wait $SERVER_PID 2>/dev/null || true; exit $MIGRATE_STATUS; fi; wait $SERVER_PID"]

# AARVO backend port-binding + runtime syntax fix
