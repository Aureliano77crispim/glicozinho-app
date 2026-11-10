const fs = require('fs');
const html = fs.readFileSync('app/src/main/assets/index.html', 'utf8');
const kt = fs.readFileSync('app/src/main/java/br/com/glicozinho/app/MainActivity.kt', 'utf8');
const assert = (x, m) => { if (!x) throw new Error(m); };
assert(html.includes('id="corinthiansMusicBtn"'), 'Corinthians shortcut missing');
assert(html.includes('id="corinthiansMusicModal"'), 'Music modal missing');
assert(html.includes('src="corinthians-crest.svg"'), 'Local crest missing');
for (const id of ['hino','bando','nunca','louco','minhavida']) {
  assert(html.includes(`openCorinthiansSong('${id}')`), `Song ${id} missing in UI`);
  assert(kt.includes(`"${id}"`), `Song ${id} missing in native allowlist`);
}
assert(kt.includes('Intent.ACTION_VIEW'), 'External YouTube open intent missing');
assert(!fs.readFileSync('app/src/main/AndroidManifest.xml','utf8').includes('android.permission.INTERNET'), 'App must remain offline');
console.log('CORINTHIANS FEATURE TESTS PASSED: local crest, 1910 shortcut, five allowlisted YouTube searches, external intent, offline manifest.');
