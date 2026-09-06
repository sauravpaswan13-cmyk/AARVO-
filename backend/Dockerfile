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
CMD ["sh", "-c", "node src/migrate.js && node src/server.js"]
