# syntax=docker/dockerfile:1
# Build context MUST be the repository root:
#   docker build -f apps/platform-admin/Dockerfile -t quincaisoft/platform-admin .
ARG NODE_IMAGE=node:22-alpine

FROM ${NODE_IMAGE} AS build
ENV NEXT_TELEMETRY_DISABLED=1
RUN corepack enable
WORKDIR /repo
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps ./apps
COPY packages ./packages
# Drop any host node_modules/.next that came in with the build context.
RUN find . \( -name node_modules -o -name .next \) -prune -exec rm -rf {} +
RUN pnpm install --frozen-lockfile --filter @quincaisoft/platform-admin...
RUN pnpm --filter @quincaisoft/platform-admin build

FROM ${NODE_IMAGE} AS runtime
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 HOSTNAME=0.0.0.0 PORT=3200
WORKDIR /srv
# Standalone output keeps the monorepo layout: server.js lives in apps/platform-admin/.
COPY --from=build --chown=node:node /repo/apps/platform-admin/.next/standalone ./
COPY --from=build --chown=node:node /repo/apps/platform-admin/.next/static ./apps/platform-admin/.next/static
USER node
EXPOSE 3200
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:3200/').then(r=>process.exit(r.status<500?0:1)).catch(()=>process.exit(1))"
CMD ["node","apps/platform-admin/server.js"]
