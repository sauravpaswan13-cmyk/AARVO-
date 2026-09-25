import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const gradle = fs.readFileSync(new URL('../../app/build.gradle.kts', import.meta.url), 'utf8');
const workflow = fs.readFileSync(new URL('../../.github/workflows/android.yml', import.meta.url), 'utf8');
const example = fs.readFileSync(new URL('../../keystore.properties.example', import.meta.url), 'utf8');

test('release build signing is externally configured and release mode is explicit', () => {
  assert.match(gradle, /getByName\("release"\)/);
  assert.match(gradle, /isMinifyEnabled\s*=\s*(true|false)/);
  assert.match(gradle, /isShrinkResources\s*=\s*(true|false)/);
  assert.match(gradle, /aarvoReleaseStoreFile/);
  assert.match(gradle, /aarvoReleaseStorePassword/);
  assert.match(gradle, /aarvoReleaseKeyAlias/);
  assert.match(gradle, /aarvoReleaseKeyPassword/);
  assert.match(example, /REPLACE_ME/);
  assert.doesNotMatch(example, /rzp_live_[A-Za-z0-9]{12,}/);
});

test('CI produces both debug APK and release AAB artifacts', () => {
  assert.match(workflow, /assembleDebug/);
  assert.match(workflow, /bundleRelease/);
  assert.match(workflow, /aarvo-debug-apk/);
  assert.match(workflow, /aarvo-release-aab/);
});
