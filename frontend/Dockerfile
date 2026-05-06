FROM node:20-alpine AS base

# Stage 1: Install dependencies
FROM base AS deps
WORKDIR /app

# Install build dependencies for 'sharp'
RUN apk add --no-cache \
    python3 \
    make \
    g++ \
    libc6-compat \
    vips-dev

COPY package.json package-lock.json* ./

# Install dependencies including 'sharp'
RUN npm ci

# Stage 2: Build the Next.js app
FROM base AS builder
WORKDIR /app

COPY . .

COPY --from=deps /app/node_modules ./node_modules

# Disable Next.js telemetry
ENV NEXT_TELEMETRY_DISABLED=1

# Select which env file to use at build time (defaults to production)
ARG APP_ENV=production
COPY .env.${APP_ENV} ./.env.production

RUN npm run build

# Stage 3: Prepare production image
FROM base AS runner
WORKDIR /app

# Disable Next.js telemetry
ENV NEXT_TELEMETRY_DISABLED=1

# Install runtime dependencies for 'sharp'
RUN apk add --no-cache \
    libc6-compat \
    vips

# Copy necessary files from the builder stage
COPY --from=builder /app/src ./src

COPY --from=builder /app/next.config.* ./
COPY --from=builder /app/.next ./.next
# COPY --from=builder /app/public ./public

# Copy package files
COPY --from=builder /app/package.json ./
COPY --from=deps /app/package-lock.json ./

# Install only production dependencies
RUN npm ci --only=production

# Fix root folder permission
RUN mkdir -p /app/.next/cache/ && chown -R 101:101 /app/.next/cache

# Expose port 3000
EXPOSE 3000

# Start the Next.js app
CMD ["npm", "start"]
