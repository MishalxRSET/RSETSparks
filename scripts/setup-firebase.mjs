#!/usr/bin/env node
// One-command Firebase setup for RSETSparks. Works on Windows, macOS and Linux.
//
//   node scripts/setup-firebase.mjs [project-id]
//
// Needs Node 20+ and Java 11+ (for keytool). It is safe to re-run: every step checks what already
// exists. Steps the Firebase CLI can't do (billing, sign-in providers, Storage bucket) open the
// right console page and wait for you.

import { execFileSync, spawnSync } from 'node:child_process';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { join, resolve } from 'node:path';
import { createInterface } from 'node:readline/promises';

const ROOT = resolve(import.meta.dirname, '..');
const PACKAGE = 'com.rsetsparks.app';
const REGION = 'asia-south1'; // Mumbai
const isWindows = process.platform === 'win32';
// rsetsparks.blaze in gradle.properties: false = free Spark plan (no Storage bucket, no Cloud Functions).
const BLAZE = /^rsetsparks\.blaze\s*=\s*true\s*$/m.test(readFileSync(join(ROOT, 'gradle.properties'), 'utf8'));
const rl = createInterface({ input: process.stdin, output: process.stdout });

const step = (n, msg) => console.log(`\n\x1b[35m[${n}] ${msg}\x1b[0m`);
const ok = (msg) => console.log(`  \x1b[32m✔\x1b[0m ${msg}`);
const warn = (msg) => console.log(`  \x1b[33m!\x1b[0m ${msg}`);

/** Runs the Firebase CLI (pinned major version, no global install needed). */
function firebase(args, { json = false, interactive = false } = {}) {
  const full = ['--yes', 'firebase-tools@14', ...args, ...(json ? ['--json'] : [])];
  if (interactive) {
    const r = spawnSync('npx', full, { stdio: 'inherit', cwd: ROOT, shell: isWindows });
    if (r.status !== 0) throw new Error(`firebase ${args.join(' ')} failed`);
    return null;
  }
  const out = spawnSync('npx', full, { cwd: ROOT, shell: isWindows, encoding: 'utf8' });
  if (json) {
    try {
      const parsed = JSON.parse(out.stdout);
      if (parsed.status === 'error') throw new Error(parsed.error);
      return parsed.result;
    } catch (e) {
      throw new Error(`firebase ${args.join(' ')} failed: ${e.message}\n${out.stderr || out.stdout}`);
    }
  }
  if (out.status !== 0) throw new Error(`firebase ${args.join(' ')} failed:\n${out.stderr || out.stdout}`);
  return out.stdout;
}

function openUrl(url) {
  const cmd = isWindows ? 'cmd' : process.platform === 'darwin' ? 'open' : 'xdg-open';
  const args = isWindows ? ['/c', 'start', '', url] : [url];
  spawnSync(cmd, args, { stdio: 'ignore' });
}

async function manual(title, url, instructions) {
  console.log(`  → ${title}`);
  for (const line of instructions) console.log(`     • ${line}`);
  console.log(`     ${url}`);
  openUrl(url);
  await rl.question('  Press Enter when done... ');
}

function sha(keystore, alias, storepass, keypass) {
  try {
    const out = execFileSync('keytool', ['-list', '-v', '-keystore', keystore, '-alias', alias,
      '-storepass', storepass, ...(keypass ? ['-keypass', keypass] : [])], { encoding: 'utf8' });
    const grab = (label) => out.match(new RegExp(`${label}:\\s*([0-9A-F:]+)`))?.[1];
    return [grab('SHA1'), grab('SHA256')].filter(Boolean);
  } catch {
    return [];
  }
}

async function main() {
  console.log('\x1b[1mRSETSparks — Firebase setup\x1b[0m');

  step(1, 'Sign in to Firebase (a browser window opens)');
  firebase(['login'], { interactive: true });

  step(2, 'Project');
  const suggested = `rsetsparks-${Math.random().toString(36).slice(2, 7)}`;
  const projectId = process.argv[2]
    || (await rl.question(`  Project ID [${suggested}] (lowercase, must be globally unique): `)).trim()
    || suggested;
  const projects = firebase(['projects:list'], { json: true }) || [];
  if (projects.some((p) => p.projectId === projectId)) {
    ok(`Using existing project ${projectId}`);
  } else {
    firebase(['projects:create', projectId, '--display-name', 'RSETSparks'], { interactive: true });
    ok(`Created ${projectId}`);
  }
  writeFileSync(join(ROOT, '.firebaserc'), JSON.stringify({ projects: { default: projectId } }, null, 2) + '\n');
  ok('Wrote .firebaserc');
  const console_ = `https://console.firebase.google.com/project/${projectId}`;

  step(3, 'Blaze plan (needed for photo storage and push-notification functions)');
  if (BLAZE) {
    await manual('Upgrade to the Blaze (pay-as-you-go) plan', `${console_}/usage/details`, [
      'Click "Modify plan" → Blaze, link or create a billing account.',
      'Then set a budget alert (for example ₹500/month) under Google Cloud Billing → Budgets.',
    ]);
  } else {
    warn('Skipped: rsetsparks.blaze=false in gradle.properties, so the app runs on the free Spark plan without photos or push.');
  }

  step(4, 'Android app');
  const apps = firebase(['apps:list', 'ANDROID', '--project', projectId], { json: true }) || [];
  let app = apps.find((a) => a.packageName === PACKAGE);
  if (app) {
    ok(`Android app already registered (${app.appId})`);
  } else {
    app = firebase(['apps:create', 'ANDROID', 'RSETSparks', '--package-name', PACKAGE, '--project', projectId], { json: true });
    ok(`Registered ${PACKAGE} (${app.appId})`);
  }

  step(5, 'Signing fingerprints (Google Sign-In needs them)');
  const fingerprints = [];
  const debugKeystore = join(homedir(), '.android', 'debug.keystore');
  if (existsSync(debugKeystore)) fingerprints.push(...sha(debugKeystore, 'androiddebugkey', 'android'));
  else warn('No debug keystore yet. Build the app once in Android Studio, then re-run this script.');
  const keyProps = join(ROOT, 'keystore.properties');
  if (existsSync(keyProps)) {
    const props = Object.fromEntries(readFileSync(keyProps, 'utf8').split(/\r?\n/)
      .filter((l) => l.includes('=')).map((l) => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()]));
    fingerprints.push(...sha(join(ROOT, props.storeFile), props.keyAlias || 'upload', props.storePassword, props.keyPassword));
  } else {
    warn('No keystore.properties yet (upload key). Re-run after creating it (guides/RELEASE_ANDROID.md §1).');
  }
  const existing = (firebase(['apps:android:sha:list', app.appId, '--project', projectId], { json: true }) || [])
    .map((c) => c.shaHash?.toUpperCase().replace(/:/g, ''));
  for (const fp of fingerprints) {
    if (existing.includes(fp.replace(/:/g, ''))) continue;
    firebase(['apps:android:sha:create', app.appId, fp, '--project', projectId]);
    ok(`Added ${fp.slice(0, 20)}…`);
  }
  if (fingerprints.length > 0) ok('Fingerprints are registered');
  warn('After your first Play upload, add the Play App Signing SHA-1 and SHA-256 too (Play Console → App integrity).');

  step(6, 'Sign-in providers');
  await manual('Enable Email/Password and Google sign-in', `${console_}/authentication/providers`, [
    'Click "Get started" if asked.',
    'Email/Password → Enable → Save. Leave "Email link" off.',
    'Google → Enable → set the support email → Save.',
    'Templates tab (optional): change the sender name to "RSETSparks".',
  ]);

  step(7, 'Cloud Firestore');
  const dbs = firebase(['firestore:databases:list', '--project', projectId], { json: true }) || [];
  if (dbs.some((d) => (d.name || '').endsWith('/databases/(default)'))) {
    ok('Firestore database already exists');
  } else {
    firebase(['firestore:databases:create', '(default)', '--location', REGION, '--project', projectId], { interactive: true });
    ok(`Created Firestore in ${REGION}`);
  }

  step(8, 'Cloud Storage');
  if (BLAZE) {
    await manual('Create the default Storage bucket', `${console_}/storage`, [
      'Click "Get started" → Production mode → location asia-south1 → Done.',
      'Skip this if you already see a bucket there.',
    ]);
  } else {
    warn('Skipped (Spark plan).');
  }

  step(9, 'App config (google-services.json)');
  firebase(['apps:sdkconfig', 'ANDROID', app.appId, '--project', projectId, '--out', join(ROOT, 'app', 'google-services.json')]);
  const config = readFileSync(join(ROOT, 'app', 'google-services.json'), 'utf8');
  if (config.includes('"client_type": 3')) ok('Wrote app/google-services.json (includes the Google Sign-In client)');
  else warn('google-services.json has no web client: make sure Google sign-in is enabled (step 6), then re-run.');

  step(10, 'Deploy security rules, indexes, push-notification functions and legal pages');
  if (BLAZE) {
    const npm = spawnSync('npm', ['ci'], { cwd: join(ROOT, 'functions'), stdio: 'inherit', shell: isWindows });
    if (npm.status !== 0) throw new Error('npm ci in functions/ failed');
  }
  const targets = BLAZE ? 'firestore:rules,firestore:indexes,storage,functions,hosting' : 'firestore:rules,firestore:indexes,hosting';
  firebase(['deploy', '--only', targets, '--project', projectId, '--force'], { interactive: true });
  ok('Deployed');

  // The privacy policy, terms and account-deletion pages (docs/) are served by Firebase Hosting,
  // which works with a private repo. Point the app's links at them.
  const site = `https://${projectId}.web.app`;
  const stringsPath = join(ROOT, 'app', 'src', 'main', 'res', 'values', 'strings.xml');
  const strings = readFileSync(stringsPath, 'utf8')
    .replace(/https:\/\/[^<"]+?\/(privacy-policy|terms|delete-account)\.html/g, `${site}/$1.html`);
  writeFileSync(stringsPath, strings);
  ok(`Legal pages live at ${site}/privacy-policy.html (app links updated in strings.xml)`);

  step(11, 'Almost done: things only you can do');
  console.log(`  1. Build and run the app (Android Studio ▶), sign in once with your Rajagiri account.
  2. Make yourself a moderator: ${console_}/firestore/databases/-default-/data
     Start collection "admins" → Document ID = your UID (from ${console_}/authentication/users)
     → field "role" = "moderator". Restart the app.
  3. Before launch: App Check → Play Integrity (guides/FIREBASE_SETUP.md §6).
  4. Commit app/google-services.json, .firebaserc and app/src/main/res/values/strings.xml.
  5. Play Console privacy policy URL: https://${projectId}.web.app/privacy-policy.html
     Account deletion URL:      https://${projectId}.web.app/delete-account.html`);
  rl.close();
}

main().catch((e) => {
  console.error(`\n\x1b[31m✖ ${e.message}\x1b[0m\nFix the problem above and re-run; finished steps are skipped.`);
  rl.close();
  process.exit(1);
});
