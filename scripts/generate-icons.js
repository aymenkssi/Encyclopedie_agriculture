/**
 * Generates the app icons in assets/images from the sprout drawing (Playwright + Chromium).
 * Usage: node scripts/generate-icons.js
 */
const path = require('path');
const { chromium } = require('playwright');

const OUT = path.join(__dirname, '..', 'assets', 'images');
const GREEN = '#2E6B3A';
const BEIGE = '#F3EBD7';

const sprout = (color) => `<svg viewBox="30 24 50 58" xmlns="http://www.w3.org/2000/svg" style="width:100%;height:100%">
  <path fill="${color}" d="M53,74 L53,52 C53,44 47,38 37,37 C37,47 43,53 53,53 Z"/>
  <path fill="${color}" d="M55,74 L55,46 C55,36 62,30 73,29 C73,40 66,46 55,47 Z"/>
  <path fill="${color}" d="M52.5,75 L55.5,75 L55.5,47 L52.5,47 Z"/>
  <path fill="${color}" d="M32,74 L76,74 L76,78 L32,78 Z"/>
</svg>`;

const ICONS = [
  // [file, size, background, sprout color, sprout size ratio]
  ['icon.png', 1024, GREEN, BEIGE, 0.62],
  ['adaptive-icon.png', 1024, 'transparent', BEIGE, 0.5], // Android crops to the central 66%
  ['splash-icon.png', 1024, 'transparent', BEIGE, 0.8],
  ['favicon.png', 48, GREEN, BEIGE, 0.7],
  ['notification-icon.png', 96, 'transparent', '#FFFFFF', 0.8], // Android: white silhouette
];

(async () => {
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
  for (const [file, size, bg, color, ratio] of ICONS) {
    const page = await browser.newPage({ viewport: { width: size, height: size } });
    await page.setContent(`<html><body style="margin:0;width:${size}px;height:${size}px;background:${bg};display:flex;align-items:center;justify-content:center">
      <div style="width:${size * ratio}px;height:${size * ratio}px">${sprout(color)}</div></body></html>`);
    await page.screenshot({ path: path.join(OUT, file), omitBackground: bg === 'transparent' });
    await page.close();
    console.log('wrote', file);
  }
  await browser.close();
})();
