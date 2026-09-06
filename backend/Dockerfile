FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm install --omit=dev --ignore-scripts
COPY src ./src
COPY schema.sql ./schema.sql
COPY migrations ./migrations
# Replace the unescaped SQL interval literal before Node parses server.js.
RUN sed -i "s/interval '10 minutes'/make_interval(mins => 10)/g" src/server.js
RUN node src/prepare-phone-first-auth.js
EXPOSE 8080
USER node
CMD ["sh", "-c", "node src/migrate.js && if [ -n \"$ADMIN_EMAIL\" ] && [ -n \"$ADMIN_PASSWORD\" ]; then node src/bootstrap-admin.js; else echo 'Admin bootstrap skipped: ADMIN_EMAIL/ADMIN_PASSWORD not available'; fi && node src/server.js"]

# AARVO backend redeploy marker
