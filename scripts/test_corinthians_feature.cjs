const fs = require('fs');
const html = fs.readFileSync('app/src/main/assets/index.html', 'utf8');
const kt = fs.readFileSync('app/src/main/java/br/com/glicozinho/app/MainActivity.kt', 'utf8');
const gradle = fs.readFileSync('app/build.gradle.kts', 'utf8');
const assert = (x, m) => { if (!x) throw new Error(m); };
assert(html.includes('id="corinthiansMusicBtn"'), 'Corinthians shortcut missing');
assert(html.includes('id="corinthiansPlayer"'), 'Inline compact player missing');
assert(html.includes('src="corinthians-aguia.png"'), 'User eagle artwork missing');
assert(!html.includes('corinthiansMusicModal'), 'Old music list modal must be removed');
assert(!html.includes('youtube.com/results'), 'Player must not open YouTube');
for (const id of ['hino-oficial.mp3','bando-de-loucos.mp3','nunca-vou-te-abandonar.mp3','louco-por-ti.mp3','minha-vida-minha-historia.mp3']) {
  assert(html.includes(id), `Track asset path ${id} missing`);
}
for (const id of ['corinthiansPlay','corinthiansPrev','corinthiansNext','corinthiansSeek','corinthiansShuffle','corinthiansRepeat','corinthiansVolume']) {
  assert(html.includes(`id="${id}"`), `Player control ${id} missing`);
}
assert(kt.includes('hasBundledTrack'), 'Native bundled-track check missing');
assert(kt.includes('mediaPlaybackRequiresUserGesture = false'), 'WebView autoplay setting missing');
assert(gradle.includes('versionCode = 14') && gradle.includes('versionName = "1.1.3"'), 'Version 1.1.3 not configured');
assert(!fs.readFileSync('app/src/main/AndroidManifest.xml','utf8').includes('android.permission.INTERNET'), 'App must remain offline');
console.log('CORINTHIANS PLAYER TESTS PASSED: supplied eagle artwork, inline compact player, no playlist modal, no YouTube, five local MP3 slots, playback/seek/volume/shuffle/repeat controls, Android autoplay check, offline manifest and v1.1.3.');
