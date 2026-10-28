#!/usr/bin/env node
// Checks this machine and repo for everything needed to build, test and release RSETSparks.
//
//   node scripts/doctor.mjs          human-readable report
//   node scripts/doctor.mjs --json   machine-readable (for Claude Code / CI)
//
// Exit code 0 = ready to build and run locally, 1 = something required is missing.
// It only reads; it never changes anything.

import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join, resolve } from 'node:path';

const ROOT = resolve(import.meta.dirname, '..');
const isWindows = process.platform === 'win32';
const checks = [];

function run(cmd, args) {
  const r = spawnSync(cmd, args, { encoding: 'utf8', shell: isWindows, cwd: ROOT, timeout: 60000 });
  return { ok: r.status === 0, out: `${r.stdout || ''}${r.stderr || ''}`.trim() };
}

/** level: 'required' blocks a local build; 'release' is needed before publishing; 'optional' is nice to have. */
function check(id, level, label, fn) {
  try {
    const { ok, detail, fix } = fn();
    checks.push({ id, level, label, ok, detail, fix: ok ? undefined : fix });
  } catch (e) {
    checks.push({ id, level, label, ok: false, detail: e.message });
  }
}

check('node', 'required', 'Node.js 20+', () => {
  const major = Number(process.versions.node.split('.')[0]);
  return { ok: major >= 20, detail: process.version, fix: 'Install Node.js 22 LTS from https://nodejs.org' };
});

check('java', 'required', 'Java 17+ (JDK)', () => {
  const r = run('java', ['-version']);
  const v = r.out.match(/version "(\d+)/)?.[1];
  return {
    ok: r.ok && Number(v) >= 17,
    detail: r.ok ? (r.out.split('\n').find((l) => /version/.test(l)) ?? 'found') : 'java not on PATH',
    fix: isWindows
      ? 'Set JAVA_HOME to Android Studio\'s bundled JDK (e.g. C:\\Program Files\\Android\\Android Studio\\jbr) and add %JAVA_HOME%\\bin to PATH'
      : 'Install a JDK 17+ or point JAVA_HOME at Android Studio\'s jbr folder',
  };
});

check('keytool', 'required', 'keytool (signing fingerprints)', () => {
  const r = run('keytool', ['-help']);
  return { ok: r.ok || /Key and Certificate/i.test(r.out), detail: r.ok ? 'found' : 'not on PATH', fix: 'Add %JAVA_HOME%\\bin (or $JAVA_HOME/bin) to PATH' };
});

check('android-sdk', 'required', 'Android SDK', () => {
  const props = join(ROOT, 'local.properties');
  const fromProps = existsSync(props) ? readFileSync(props, 'utf8').match(/^sdk\.dir=(.*)$/m)?.[1]?.replace(/\\\\/g, '\\').replace(/\\:/g, ':') : null;
  const sdk = fromProps || process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
  return {
    ok: !!sdk && existsSync(sdk),
    detail: sdk || 'not found',
    fix: 'Open the project once in Android Studio (it writes local.properties), or set ANDROID_HOME',
  };
});

check('adb-device', 'optional', 'Phone or emulator connected (adb)', () => {
  const r = run('adb', ['devices']);
  const devices = r.out.split('\n').slice(1).filter((l) => /\tdevice$/.test(l));
  return {
    ok: devices.length > 0,
    detail: r.ok ? `${devices.length} device(s)` : 'adb not on PATH',
    fix: 'Start an emulator in Android Studio (Device Manager) or plug in a phone with USB debugging on; add <sdk>/platform-tools to PATH',
  };
});

check('debug-keystore', 'required', 'Debug keystore (~/.android/debug.keystore)', () => {
  const p = join(homedir(), '.android', 'debug.keystore');
  return { ok: existsSync(p), detail: p, fix: 'Build once: ./gradlew :app:assembleDebug (or press Run in Android Studio)' };
});

check('firebase-config', 'required', 'app/google-services.json', () => {
  const p = join(ROOT, 'app', 'google-services.json');
  if (!existsSync(p)) return { ok: false, detail: 'missing', fix: 'Run: node scripts/setup-firebase.mjs' };
  const cfg = JSON.parse(readFileSync(p, 'utf8'));
  const client = cfg.client?.find((c) => c.client_info?.android_client_info?.package_name === 'com.rsetsparks.app');
  const hasWebClient = JSON.stringify(cfg).includes('"client_type":3') || JSON.stringify(cfg).includes('"client_type": 3');
  return {
    ok: !!client && hasWebClient,
    detail: `project ${cfg.project_info?.project_id}${client ? '' : ', no com.rsetsparks.app client'}${hasWebClient ? '' : ', no Google Sign-In web client'}`,
    fix: 'Enable Google sign-in in Firebase Authentication, then re-run node scripts/setup-firebase.mjs',
  };
});

check('firebaserc', 'required', '.firebaserc (default project)', () => {
  const p = join(ROOT, '.firebaserc');
  const id = existsSync(p) ? JSON.parse(readFileSync(p, 'utf8')).projects?.default : null;
  return { ok: !!id, detail: id || 'missing', fix: 'Run: node scripts/setup-firebase.mjs' };
});

check('legal-urls', 'release', 'Privacy/terms/deletion URLs point at Firebase Hosting', () => {
  const s = readFileSync(join(ROOT, 'app', 'src', 'main', 'res', 'values', 'strings.xml'), 'utf8');
  const urls = [...s.matchAll(/https:\/\/[^<]+\.html/g)].map((m) => m[0]);
  return {
    ok: urls.length === 3 && urls.every((u) => u.includes('.web.app/') || u.includes('.firebaseapp.com/')),
    detail: urls.join(' '),
    fix: 'Run node scripts/setup-firebase.mjs (it deploys docs/ to Firebase Hosting and rewrites these URLs)',
  };
});

check('upload-key', 'release', 'Upload key (keystore.properties + .jks)', () => {
  const p = join(ROOT, 'keystore.properties');
  if (!existsSync(p)) return { ok: false, detail: 'keystore.properties missing', fix: 'See guides/RELEASE_ANDROID.md §1' };
  const store = readFileSync(p, 'utf8').match(/^storeFile=(.*)$/m)?.[1]?.trim();
  return { ok: !!store && existsSync(join(ROOT, store)), detail: store || 'storeFile not set', fix: 'storeFile in keystore.properties must point at your .jks' };
});

check('firebase-cli-login', 'optional', 'Firebase CLI signed in', () => {
  const r = run('npx', ['--yes', 'firebase-tools@14', 'login:list', '--json']);
  let accounts = [];
  try { accounts = JSON.parse(r.out.slice(r.out.indexOf('{'))).result || []; } catch { /* not signed in */ }
  return { ok: accounts.length > 0, detail: accounts.map((a) => a.user?.email).join(', ') || 'not signed in', fix: 'npx firebase-tools@14 login' };
});

const args = process.argv.slice(2);
const requiredOk = checks.filter((c) => c.level === 'required').every((c) => c.ok);
if (args.includes('--json')) {
  console.log(JSON.stringify({ readyToBuildAndRun: requiredOk, checks }, null, 2));
} else {
  const icon = (c) => (c.ok ? '\x1b[32m✔\x1b[0m' : c.level === 'required' ? '\x1b[31m✖\x1b[0m' : '\x1b[33m!\x1b[0m');
  for (const c of checks) {
    console.log(`${icon(c)} ${c.label.padEnd(52)} ${c.detail ?? ''}`);
    if (!c.ok && c.fix) console.log(`    → ${c.fix}`);
  }
  console.log(requiredOk ? '\nReady to build and run locally.' : '\nFix the ✖ items above, then re-run.');
}
process.exit(requiredOk ? 0 : 1);
