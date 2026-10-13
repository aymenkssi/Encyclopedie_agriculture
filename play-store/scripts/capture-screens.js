// 1) Captures the raw app screens (fr, en, ar) from the web version of the app, with a demo garden.
//    From the project folder:
//      npx expo export --platform web --output-dir play-store/build/web
//      node play-store/scripts/capture-screens.js      (needs Playwright)
//    Output: play-store/build/raw/<lang>/NN-<screen>.png (framed by compose-images.js)
const { chromium } = require('playwright');
const fs = require('fs');
const http = require('http');
const path = require('path');

const WEB = path.join(__dirname, '../build/web');
// Headless Chromium has no proper Arabic system font: the Arabic screens use Noto Kufi Arabic when it was
// downloaded for compose-images.js (see README).
const KUFI = path.join(__dirname, '../build/fonts/fontsource-noto-kufi-arabic-5.2.8/package/files/');
const arabicFontCss = () =>
  [400, 700, 800]
    .map((w) => {
      const file = `${KUFI}noto-kufi-arabic-arabic-${w}-normal.woff2`;
      if (!fs.existsSync(file)) return '';
      const data = fs.readFileSync(file).toString('base64');
      return `@font-face{font-family:'KufiArabic';font-weight:${w};src:url(data:font/woff2;base64,${data}) format('woff2');unicode-range:U+0600-06FF,U+FE70-FEFF;}`;
    })
    .join('') + '*:not([style*="onicons"]){font-family:KufiArabic,system-ui,Roboto,sans-serif !important}' +
    '[role="tablist"] *{overflow:visible !important;line-height:normal !important}';
const OUT = path.join(__dirname, '../build/raw/');

const TYPES = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.png': 'image/png', '.ttf': 'font/ttf', '.ico': 'image/x-icon', '.json': 'application/json' };
const DYNAMIC = [[/^\/plant\//, '/plant/[id].html'], [/^\/crop\//, '/crop/[id].html'], [/^\/garden\//, '/garden/[id].html']];
const server = http.createServer((req, res) => {
  const url = decodeURIComponent(req.url.split('?')[0]);
  const fallback = DYNAMIC.find(([re]) => re.test(url))?.[1];
  for (const candidate of [url, url + '.html', path.join(url, 'index.html'), fallback].filter(Boolean)) {
    const file = path.join(WEB, candidate);
    if (file.startsWith(WEB) && fs.existsSync(file) && fs.statSync(file).isFile()) {
      res.writeHead(200, { 'Content-Type': TYPES[path.extname(file)] || 'application/octet-stream' });
      return fs.createReadStream(file).pipe(res);
    }
  }
  res.writeHead(404);
  res.end();
});

// Demo personal data: one garden with a tomato crop and a few tasks, dated around today.
const iso = (offset) => {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};
const meta = (id) => ({ id, createdAt: 1, updatedAt: 1, deleted: false, deviceId: 'demo' });
const TITLES = {
  fr: { garden: 'Potager familial', water: 'Arroser les tomates', check: 'Surveiller le mildiou · Tomate', harvest: 'Récolter · Pomme de terre' },
  en: { garden: 'Family vegetable garden', water: 'Water the tomatoes', check: 'Check for blight · Tomato', harvest: 'Harvest · Potato' },
  ar: { garden: 'حديقة العائلة', water: 'سقي الطماطم', check: 'مراقبة البياض · طماطم', harvest: 'حصاد · بطاطس' },
};
const userData = (lang) => {
  const T = TITLES[lang];
  const task = (id, title, activity, due, plantId, cropId, extra = {}) => ({
    ...meta(id), title, description: null, activity, dueDate: due, done: false, doneAt: null, plantId, cropId, reminder: true,
    repeatDays: null, origin: 'user', notificationId: null, ...extra,
  });
  return {
    favorites: [{ ...meta('f1'), plantId: 'solanum-lycopersicum' }, { ...meta('f2'), plantId: 'allium-cepa' }],
    gardens: [{ ...meta('g1'), name: T.garden, farmType: 'garden', areaM2: 60, regionId: 'ma-rsk' }],
    plots: [{ ...meta('p1'), gardenId: 'g1', name: 'A', areaM2: 20, soilId: null, methodId: 'open_field' }],
    crops: [
      { ...meta('c1'), gardenId: 'g1', plotId: 'p1', plantId: 'solanum-lycopersicum', varietyId: null, methodId: 'open_field', startActivity: 'transplanting', startDate: iso(-40), stage: 'flowering', quantity: '12', harvestedOn: null },
      { ...meta('c2'), gardenId: 'g1', plotId: null, plantId: 'solanum-tuberosum', varietyId: null, methodId: 'open_field', startActivity: 'planting', startDate: iso(-80), stage: 'growing', quantity: null, harvestedOn: null },
    ],
    tasks: [
      task('t1', T.water, 'watering', iso(0), 'solanum-lycopersicum', 'c1', { repeatDays: 3 }),
      task('t2', T.check, 'health_monitoring', iso(2), 'solanum-lycopersicum', 'c1', { origin: 'planned' }),
      task('t3', T.harvest, 'harvest', iso(5), 'solanum-tuberosum', 'c2', { origin: 'planned' }),
    ],
    notes: [],
  };
};

const SEARCH = { fr: 'tomtae', en: 'tomatoe', ar: 'طماطم' };

(async () => {
  await new Promise((resolve) => server.listen(8098, resolve));
  const browser = await chromium.launch(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {});
  const only = process.env.LANGS ? process.env.LANGS.split(',') : ['fr', 'en', 'ar'];
  for (const lang of only) {
    for (const dark of [false, true]) {
      const ctx = await browser.newContext({ viewport: { width: 390, height: 760 }, deviceScaleFactor: 3, locale: lang, timezoneId: 'Africa/Casablanca' });
      await ctx.addInitScript(([l, theme, data]) => {
        if (window.top !== window) return;
        localStorage.setItem('app_language', l);
        localStorage.setItem('theme_preference', theme);
        localStorage.setItem('settings', JSON.stringify({ onboardingDone: true, regionId: 'ma-rsk', experience: 'beginner', farmType: 'garden', preferredMethod: 'open_field', notificationsEnabled: true }));
        localStorage.setItem('user_data_v1', data);
      }, [lang, dark ? 'dark' : 'light', JSON.stringify(userData(lang))]);
      const page = await ctx.newPage();
      page.on('pageerror', (e) => console.error(`[${lang}] page error:`, e.message));
      if (lang === 'ar') {
        const css = arabicFontCss();
        page.on('load', () => page.addStyleTag({ content: css }).catch(() => undefined));
      }
      fs.mkdirSync(path.join(OUT, lang), { recursive: true });
      const snap = (name) => page.screenshot({ path: path.join(OUT, name.startsWith('x-') ? 'extra' : lang, `${name.startsWith('x-') ? `${lang}-` : ''}${name}.png`) });
      const go = async (url, wait = 1500) => {
        await page.goto(`http://localhost:8098${url}`, { waitUntil: 'networkidle' });
        await page.waitForTimeout(wait);
      };
      if (dark) {
        await go('/plant/solanum-lycopersicum?tab=calendar', 2000);
        await snap('06-dark');
        await ctx.close();
        continue;
      }
      await go('/home', 2500);
      await snap('01-home');
      await go('/encyclopedia');
      await page.getByTestId('search-input').fill(SEARCH[lang]);
      await page.waitForTimeout(800);
      await snap('02-search');
      await go('/plant/solanum-lycopersicum');
      await snap('03-sheet');
      await go('/plant/solanum-lycopersicum?tab=calendar');
      await snap('04-calendar');
      await go('/calendar');
      await snap('05-tasks');
      if (process.env.ALL_SCREENS) {
        for (const [name, url] of [['crops', '/crops'], ['crop', '/crop/c1'], ['garden', '/garden/g1'], ['profile', '/profile'], ['donate', '/donate'],
          ['sources', '/sources'], ['guide', '/plant/solanum-lycopersicum?tab=guide'], ['health', '/plant/solanum-lycopersicum?tab=health'],
          ['conditions', '/plant/solanum-lycopersicum?tab=conditions'], ['onboarding', '/onboarding'], ['history', '/history']]) {
          await go(url);
          fs.mkdirSync(path.join(OUT, 'extra'), { recursive: true });
          await snap(`x-${name}`);
        }
      }
      await ctx.close();
    }
  }
  await browser.close();
  server.close();
})();
