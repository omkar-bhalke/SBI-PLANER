FROM node:20-slim

# Install system dependencies if required for canvas/caching
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY . .

# Set volume mount directory for session persistence
ENV AUTH_DIR=/data
VOLUME /data

EXPOSE 3000

CMD ["node", "server.js"]
