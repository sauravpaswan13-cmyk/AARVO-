FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY package*.json ./
RUN npm install --omit=dev --ignore-scripts
COPY src ./src
COPY schema.sql ./schema.sql
COPY migrations ./migrations
EXPOSE 8080
USER node
# TEMPORARY: bootstrap the first admin during deployment. Revert after success.
CMD ["sh", "-c", "node src/migrate.js && node src/bootstrap-admin.js && node src/server.js"]
