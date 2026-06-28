# Browsers are pre-installed in the Microsoft Playwright base image.
# --ignore-scripts prevents playwright's postinstall from re-downloading them.
FROM mcr.microsoft.com/playwright:v1.48.2-noble

WORKDIR /app

COPY package.json .
RUN npm install --omit=dev --ignore-scripts

COPY scraper.mjs .

EXPOSE 3000

CMD ["node", "scraper.mjs"]
