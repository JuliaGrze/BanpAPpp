# Etap 1: Budowanie Angulara
FROM node:20 AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install

COPY . .

# Workaround dla błędów DOM: użyj opcji Node.js + ustawienie produkcyjne ręcznie
ENV NODE_OPTIONS=--openssl-legacy-provider

RUN npx ng build --configuration production

# Etap 2: serwowanie przez nginx
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
