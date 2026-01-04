#tape 1 : Builder Angular
FROM node:20-slim AS builder
WORKDIR /app
# Installer les dépendances système nécessaires
RUN apt-get update && apt-get install -y python3 make g++ && rm -rf /var/lib/apt/lists/*
# Copier package.json et installer les dépendances
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps
# Copier le reste du code
COPY . .
# Build Angular avec npx
RUN npx ng build --configuration production --no-prerender
# Étape 2 : Servir avec nginx
FROM nginx:1.25-alpine
RUN rm -rf /usr/share/nginx/html/*
# Copier les fichiers Angular générés
COPY --from=builder /app/dist/authentification-front/browser /usr/share/nginx/html
RUN mv /usr/share/nginx/html/index.csr.html /usr/share/nginx/html/index.html
# Copier la config NGINX
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Permissions
RUN chown -R nginx:nginx /usr/share/nginx/html
