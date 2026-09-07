FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm install --omit=dev --ignore-scripts
COPY src ./src
COPY schema.sql ./schema.sql
COPY migrations ./migrations
# Prepare phone-first auth first because the preparation script rewrites server.js.
RUN node src/prepare-phone-first-auth.js
# Fix any legacy escaped SQL interval and verify all production JavaScript sources.
RUN sed -i "s/interval \\\'1 minute\\\'/make_interval(mins => 1)/g" src/server.js \
    && node --check src/server.js \
    && node --check src/marketplace-completion.js \
    && node --check src/launcher.js
# The launcher generates a compatibility wrapper beside server.js; make the app tree writable by the runtime user.
RUN chown -R node:node /app
EXPOSE 8080
USER node
CMD ["sh", "-c", "node src/migrate.js && if [ -n \"$ADMIN_EMAIL\" ] && [ -n \"$ADMIN_PASSWORD\" ]; then node src/bootstrap-admin.js; else echo 'Admin bootstrap skipped: ADMIN_EMAIL/ADMIN_PASSWORD not available'; fi && node src/launcher.js"]

# AARVO backend redeploy marker
