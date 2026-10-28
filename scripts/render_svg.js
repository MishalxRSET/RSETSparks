const { chromium } = require('playwright');
const fs = require('fs');
(async () => {
  const [,, svgPath, outPath, size] = process.argv;
  const s = parseInt(size || '1024');
  const svg = fs.readFileSync(svgPath, 'utf8');
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: s, height: s } });
  const b64 = Buffer.from(svg).toString('base64');
  await page.setContent(`<html><body style="margin:0;background:transparent"><img src="data:image/svg+xml;base64,${b64}" style="width:${s}px;height:${s}px;display:block"></body></html>`);
  await page.waitForTimeout(500);
  await page.screenshot({ path: outPath, omitBackground: true });
  await browser.close();
})();
