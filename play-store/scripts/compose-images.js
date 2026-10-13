/**
 * Generates the Google Play images of AgriPedia:
 *  - images/icon-512.png (512 x 512)
 *  - images/feature-graphic-<lang>.png (1024 x 500) for fr, en, ar
 *  - images/screenshots-<lang>/*.png framed from build/raw/<lang>/*.png when present (1080 x 1920)
 *
 * Fonts (once): see play-store/README.md, section "Refaire les images".
 * Run from the repository root: node play-store/scripts/compose-images.js
 */
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const ROOT = path.join(__dirname, '..');
const FONTS = path.join(ROOT, 'build', 'fonts');
const OUT = path.join(ROOT, 'images');

function font(pkg, file) {
  const p = path.join(FONTS, pkg, 'package', 'files', file);
  return fs.existsSync(p) ? `url(data:font/woff2;base64,${fs.readFileSync(p).toString('base64')})` : null;
}
const fontCss = [
  ['Figtree', 'fontsource-figtree-5.2.8', 'figtree-latin-700-normal.woff2', 700],
  ['Figtree', 'fontsource-figtree-5.2.8', 'figtree-latin-800-normal.woff2', 800],
  ['Figtree', 'fontsource-figtree-5.2.8', 'figtree-latin-400-normal.woff2', 400],
  ['Kufi', 'fontsource-noto-kufi-arabic-5.2.8', 'noto-kufi-arabic-arabic-700-normal.woff2', 700],
  ['Kufi', 'fontsource-noto-kufi-arabic-5.2.8', 'noto-kufi-arabic-arabic-400-normal.woff2', 400],
].map(([family, pkg, file, weight]) => {
  const src = font(pkg, file);
  if (!src) throw new Error(`Missing font ${file}: see play-store/README.md`);
  return `@font-face{font-family:${family};src:${src};font-weight:${weight};}`;
}).join('\n');

// Same sprout as the launcher icon (app/src/main/res/drawable/ic_launcher_foreground.xml).
const sprout = (color) => `
<svg viewBox="30 24 50 58" xmlns="http://www.w3.org/2000/svg">
  <path fill="${color}" d="M53,74 L53,52 C53,44 47,38 37,37 C37,47 43,53 53,53 Z"/>
  <path fill="${color}" d="M55,74 L55,46 C55,36 62,30 73,29 C73,40 66,46 55,47 Z"/>
  <path fill="${color}" d="M52.5,75 L55.5,75 L55.5,47 L52.5,47 Z"/>
  <path fill="${color}" d="M32,74 L76,74 L76,78 L32,78 Z"/>
</svg>`;

const GREEN = '#2E6B3A', DARK = '#0E3A18', BEIGE = '#F3EBD7';

const TEXT = {
  fr: { title: 'AgriPedia', tagline: 'Que planter, quand et comment,\nselon votre région', chips: ['Calendrier', 'Guides', 'Hors ligne'], dir: 'ltr' },
  en: { title: 'AgriPedia', tagline: 'What to plant, when and how,\nfor your region', chips: ['Calendar', 'Guides', 'Offline'], dir: 'ltr' },
  ar: { title: 'أغريبيديا', tagline: 'ماذا تزرع ومتى وكيف\nحسب منطقتك', chips: ['التقويم', 'الأدلة', 'دون اتصال'], dir: 'rtl' },
};

const CAPTIONS = {
  fr: ['Cultures conseillées pour votre région', 'Recherche en 3 langues, même avec des fautes', 'Fiches détaillées : climat, guide, santé', 'Calendrier adapté à votre climat', 'Vos tâches et rappels', 'Mode sombre'],
  en: ['Crops suggested for your region', 'Search in 3 languages, even with typos', 'Detailed sheets: climate, guide, health', 'Calendar adapted to your climate', 'Your tasks and reminders', 'Dark mode'],
  ar: ['محاصيل مقترحة لمنطقتك', 'بحث بثلاث لغات حتى مع الأخطاء', 'بطاقات مفصلة: المناخ والدليل والصحة', 'تقويم ملائم لمناخك', 'مهامك وتذكيراتك', 'الوضع الداكن'],
};

const page = (body, w, h) => `<!doctype html><html><head><meta charset="utf-8"><style>
${fontCss}
*{margin:0;padding:0;box-sizing:border-box}
body{width:${w}px;height:${h}px;overflow:hidden;font-family:Figtree,Kufi,sans-serif}
.ar{font-family:Kufi,sans-serif}
</style></head><body>${body}</body></html>`;

async function shot(browser, html, w, h, file) {
  const p = await browser.newPage({ viewport: { width: w, height: h } });
  await p.setContent(html, { waitUntil: 'load' });
  await p.evaluate(() => document.fonts.ready);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  await p.screenshot({ path: file });
  await p.close();
  console.log('wrote', path.relative(ROOT, file));
}

(async () => {
  const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });

  await shot(browser, page(`
    <div style="width:512px;height:512px;background:${GREEN};display:flex;align-items:center;justify-content:center">
      <div style="width:330px;height:330px">${sprout(BEIGE)}</div>
    </div>`, 512, 512), 512, 512, path.join(OUT, 'icon-512.png'));

  for (const [lang, t] of Object.entries(TEXT)) {
    const cls = lang === 'ar' ? 'ar' : '';
    const chips = t.chips.map((c) => `<span style="background:${BEIGE};color:${DARK};border-radius:999px;padding:10px 22px;font-size:26px;font-weight:700">${c}</span>`).join('');
    await shot(browser, page(`
      <div dir="${t.dir}" class="${cls}" style="width:1024px;height:500px;background:linear-gradient(135deg,${GREEN},${DARK});display:flex;align-items:center;gap:48px;padding:0 64px;color:${BEIGE}">
        <div style="flex:0 0 220px;height:220px;background:rgba(243,235,215,.12);border-radius:48px;display:flex;align-items:center;justify-content:center">
          <div style="width:160px;height:160px">${sprout(BEIGE)}</div>
        </div>
        <div style="flex:1">
          <div style="font-size:${lang === 'ar' ? 64 : 76}px;font-weight:800;line-height:1.1">${t.title}</div>
          <div style="font-size:${lang === 'ar' ? 32 : 36}px;font-weight:700;line-height:1.35;margin-top:16px;white-space:pre-line">${t.tagline}</div>
          <div style="display:flex;gap:12px;margin-top:28px;flex-wrap:wrap">${chips}</div>
        </div>
      </div>`, 1024, 500), 1024, 500, path.join(OUT, `feature-graphic-${lang}.png`));
  }

  // Framed phone screenshots from raw captures of the web version (play-store/build/raw/<lang>/NN-name.png, see capture-screens.js).
  for (const lang of Object.keys(TEXT)) {
    const rawDir = path.join(ROOT, 'build', 'raw', lang);
    if (!fs.existsSync(rawDir)) continue;
    const files = fs.readdirSync(rawDir).filter((f) => f.endsWith('.png')).sort();
    for (const [i, f] of files.entries()) {
      const img = `data:image/png;base64,${fs.readFileSync(path.join(rawDir, f)).toString('base64')}`;
      const caption = CAPTIONS[lang][i] || '';
      await shot(browser, page(`
        <div dir="${TEXT[lang].dir}" class="${lang === 'ar' ? 'ar' : ''}" style="width:1080px;height:1920px;background:linear-gradient(160deg,${GREEN},${DARK});display:flex;flex-direction:column;align-items:center;padding-top:80px">
          <div style="color:${BEIGE};font-size:58px;font-weight:800;text-align:center;padding:0 60px;line-height:1.25;height:220px;display:flex;align-items:center">${caption}</div>
          <img src="${img}" style="width:800px;border-radius:40px;border:12px solid #111;box-shadow:0 24px 60px rgba(0,0,0,.35);margin-top:40px">
        </div>`, 1080, 1920), 1080, 1920, path.join(OUT, `screenshots-${lang}`, f));
    }
  }
  await browser.close();
})().catch((e) => { console.error(e); process.exit(1); });
