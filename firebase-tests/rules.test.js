// Security rules tests. Run with `npm test` (starts the Firestore + Storage emulators).
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  addDoc, collection, deleteDoc, doc, getDoc, getDocs, query, serverTimestamp, setDoc, updateDoc, where, writeBatch,
} from 'firebase/firestore';
import { ref, uploadBytes } from 'firebase/storage';

let env;

const campus = (uid) => ({ email: `${uid}@rajagiri.edu.in`, email_verified: true });

const profile = (overrides = {}) => ({
  name: 'Student',
  age: 20,
  department: 'B.Tech Computer Science',
  year: 2027,
  bio: 'Hi!',
  interests: ['Robotics'],
  photoUrl: '',
  avatarEmoji: '🎓',
  avatarColorIndex: 1,
  verified: true,
  suspended: false,
  warningCount: 0,
  visible: true,
  showBadges: false,
  gender: 'WOMAN',
  showMe: 'EVERYONE',
  orientation: '',
  lookingFor: '',
  pronouns: '',
  ...overrides,
});

const as = (uid, token = campus(uid)) => env.authenticatedContext(uid, token).firestore();
const pair = (a, b) => (a < b ? `${a}_${b}` : `${b}_${a}`);

async function seed(fn) {
  await env.withSecurityRulesDisabled(async (ctx) => fn(ctx.firestore()));
}

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-rsetsparks',
    firestore: { rules: readFileSync('../firestore.rules', 'utf8') },
    storage: { rules: readFileSync('../storage.rules', 'utf8') },
  });
});

after(async () => env?.cleanup());

beforeEach(async () => {
  await env.clearFirestore();
  await seed(async (db) => {
    for (const uid of ['alice', 'bob', 'carol', 'mod']) {
      await setDoc(doc(db, 'users', uid), profile({ name: uid }));
      await setDoc(doc(db, 'privateProfiles', uid), { email: `${uid}@rajagiri.edu.in` });
    }
    await setDoc(doc(db, 'admins', 'mod'), { role: 'moderator' });
  });
});

describe('who can get in', () => {
  test('verified Rajagiri students can read profiles', async () => {
    await assertSucceeds(getDoc(doc(as('alice'), 'users', 'bob')));
  });

  test('non-Rajagiri and unverified accounts are rejected', async () => {
    await assertFails(getDoc(doc(as('eve', { email: 'eve@gmail.com', email_verified: true }), 'users', 'bob')));
    await assertFails(getDoc(doc(as('eve', { email: 'eve@rajagiri.edu.in', email_verified: false }), 'users', 'bob')));
    await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'users', 'bob')));
  });

  test('emails stay private except to the owner and moderators', async () => {
    await assertFails(getDoc(doc(as('alice'), 'privateProfiles', 'bob')));
    await assertSucceeds(getDoc(doc(as('bob'), 'privateProfiles', 'bob')));
    await assertSucceeds(getDoc(doc(as('mod'), 'privateProfiles', 'bob')));
  });
});

describe('push tokens', () => {
  test('students manage only their own tokens and settings', async () => {
    await assertSucceeds(setDoc(doc(as('alice'), 'privateProfiles', 'alice'),
      { email: 'alice@rajagiri.edu.in', fcmTokens: ['tok1'], pushEnabled: true }, { merge: true }));
    await assertFails(setDoc(doc(as('bob'), 'privateProfiles', 'alice'),
      { email: 'bob@rajagiri.edu.in', fcmTokens: ['evil'] }, { merge: true }));
    await assertFails(setDoc(doc(as('alice'), 'privateProfiles', 'alice'),
      { email: 'alice@rajagiri.edu.in', isAdmin: true }, { merge: true }));
    await assertFails(setDoc(doc(as('alice'), 'privateProfiles', 'alice'),
      { email: 'alice@rajagiri.edu.in', fcmTokens: Array.from({ length: 11 }, (_, i) => `t${i}`) }, { merge: true }));
  });
});

describe('profiles', () => {
  test('a student can create their own 18+ profile', async () => {
    await assertSucceeds(setDoc(doc(as('dave'), 'users', 'dave'), profile()));
  });

  test('under-18 profiles and profiles for someone else are rejected', async () => {
    await assertFails(setDoc(doc(as('dave'), 'users', 'dave'), profile({ age: 17 })));
    await assertFails(setDoc(doc(as('dave'), 'users', 'erin'), profile()));
  });

  test('students cannot unsuspend themselves or fake the officer badge', async () => {
    await seed((db) => updateDoc(doc(db, 'users', 'carol'), { suspended: true }));
    await assertFails(updateDoc(doc(as('carol'), 'users', 'carol'), { suspended: false }));
    await assertFails(updateDoc(doc(as('alice'), 'users', 'alice'), { showBadges: true }));
    await assertSucceeds(updateDoc(doc(as('mod'), 'users', 'mod'), { showBadges: true }));
  });

  test('gender and show me are required; dating fields only take known values', async () => {
    const { gender, showMe, ...legacy } = profile();
    await assertFails(setDoc(doc(as('dave'), 'users', 'dave'), legacy));
    await assertFails(setDoc(doc(as('dave'), 'users', 'dave'), profile({ gender: 'BI' })));
    await assertFails(setDoc(doc(as('dave'), 'users', 'dave'), profile({ showMe: 'ANYONE' })));
    await assertFails(setDoc(doc(as('dave'), 'users', 'dave'), profile({ orientation: 'OTHER' })));
    await assertFails(setDoc(doc(as('dave'), 'users', 'dave'), profile({ pronouns: 'x'.repeat(21) })));
    await assertSucceeds(setDoc(doc(as('dave'), 'users', 'dave'),
      profile({ gender: 'TRANS_MAN', showMe: 'WOMEN', orientation: 'QUEER', lookingFor: 'RELATIONSHIP', pronouns: 'he/him' })));
    await assertSucceeds(updateDoc(doc(as('dave'), 'users', 'dave'), { showMe: 'EVERYONE', lookingFor: '' }));
    await assertFails(updateDoc(doc(as('dave'), 'users', 'dave'), { gender: '' }));
  });

  test('students can edit their own profile details but stay 18+', async () => {
    await assertSucceeds(updateDoc(doc(as('alice'), 'users', 'alice'), { bio: 'New bio', interests: ['Music'], age: 21 }));
    await assertFails(updateDoc(doc(as('alice'), 'users', 'alice'), { age: 16 }));
    await assertFails(updateDoc(doc(as('alice'), 'users', 'bob'), { bio: 'hacked' }));
  });

  test('moderators can suspend but not rewrite profiles', async () => {
    await assertSucceeds(updateDoc(doc(as('mod'), 'users', 'bob'), { suspended: true }));
    await assertFails(updateDoc(doc(as('mod'), 'users', 'bob'), { bio: 'edited by mod' }));
  });
});

describe('swipes and matches', () => {
  const like = (from, to) => ({ from, to, action: 'LIKE' });

  test('students can only swipe as themselves', async () => {
    await assertSucceeds(setDoc(doc(as('alice'), 'swipes', 'alice_bob'), like('alice', 'bob')));
    await assertFails(setDoc(doc(as('alice'), 'swipes', 'bob_alice'), like('bob', 'alice')));
  });

  test("an incoming like stays hidden until you like back", async () => {
    await seed((db) => setDoc(doc(db, 'swipes', 'bob_alice'), like('bob', 'alice')));
    await assertFails(getDoc(doc(as('alice'), 'swipes', 'bob_alice')));
    await setDoc(doc(as('alice'), 'swipes', 'alice_bob'), like('alice', 'bob'));
    await assertSucceeds(getDoc(doc(as('alice'), 'swipes', 'bob_alice')));
  });

  test('a match needs likes from both students', async () => {
    const m = { users: ['alice', 'bob'], createdAt: serverTimestamp(), lastMessageAt: serverTimestamp() };
    await setDoc(doc(as('alice'), 'swipes', 'alice_bob'), like('alice', 'bob'));
    await assertFails(setDoc(doc(as('alice'), 'matches', pair('alice', 'bob')), m));
    await seed((db) => setDoc(doc(db, 'swipes', 'bob_alice'), like('bob', 'alice')));
    await assertSucceeds(setDoc(doc(as('alice'), 'matches', pair('alice', 'bob')), m));
  });
});

describe('chat', () => {
  const matchId = pair('alice', 'bob');

  beforeEach(async () => {
    await seed((db) => setDoc(doc(db, 'matches', matchId), { users: ['alice', 'bob'] }));
  });

  const send = (db, sender, text = 'hey') => {
    const batch = writeBatch(db);
    batch.set(doc(collection(db, 'matches', matchId, 'messages')), { senderId: sender, text, createdAt: serverTimestamp() });
    batch.update(doc(db, 'matches', matchId), { lastMessage: text, lastMessageAt: serverTimestamp(), lastSenderId: sender });
    return batch.commit();
  };

  test('matched students can message and read each other', async () => {
    await assertSucceeds(send(as('alice'), 'alice'));
    await assertSucceeds(getDocs(collection(as('bob'), 'matches', matchId, 'messages')));
    await assertSucceeds(getDocs(query(collection(as('bob'), 'matches'), where('users', 'array-contains', 'bob'))));
  });

  test('outsiders cannot read or post, and nobody can post as someone else', async () => {
    await assertFails(getDocs(collection(as('carol'), 'matches', matchId, 'messages')));
    await assertFails(send(as('carol'), 'carol'));
    await assertFails(send(as('alice'), 'bob'));
  });

  test('suspended students cannot send messages', async () => {
    await seed((db) => updateDoc(doc(db, 'users', 'alice'), { suspended: true }));
    await assertFails(send(as('alice'), 'alice'));
  });

  test('account deletion: senders can delete only their own messages', async () => {
    await seed((db) => setDoc(doc(db, 'matches', matchId, 'messages', 'm1'), { senderId: 'alice', text: 'hi' }));
    await assertFails(deleteDoc(doc(as('bob'), 'matches', matchId, 'messages', 'm1')));
    await assertSucceeds(deleteDoc(doc(as('alice'), 'matches', matchId, 'messages', 'm1')));
  });

  test('either student can unmatch; moderators can read reported chats', async () => {
    await assertSucceeds(getDocs(collection(as('mod'), 'matches', matchId, 'messages')));
    await assertSucceeds(deleteDoc(doc(as('bob'), 'matches', matchId)));
  });
});

describe('safety', () => {
  test('students can block and report; only moderators read reports', async () => {
    await assertSucceeds(setDoc(doc(as('alice'), 'blocks', 'alice_bob'), { blocker: 'alice', blocked: 'bob' }));
    const report = {
      reporterId: 'alice', reporterEmail: 'alice@rajagiri.edu.in', reportedUserId: 'bob', reportedUserName: 'bob',
      reason: 'Harassment', description: 'details', transcript: '', status: 'PENDING', createdAt: serverTimestamp(),
    };
    const created = await assertSucceeds(addDoc(collection(as('alice'), 'reports'), report));
    await assertFails(getDoc(doc(as('bob'), 'reports', created.id)));
    await assertSucceeds(getDoc(doc(as('mod'), 'reports', created.id)));
    await assertFails(updateDoc(doc(as('alice'), 'reports', created.id), { status: 'DISMISSED' }));
    await assertSucceeds(updateDoc(doc(as('mod'), 'reports', created.id), { status: 'DISMISSED', adminNote: 'ok' }));
  });

  test('audit logs are moderator-only and append-only', async () => {
    const log = { adminUid: 'mod', adminEmail: 'mod@rajagiri.edu.in', action: 'X', targetUserId: 'bob', reason: 'r' };
    await assertFails(addDoc(collection(as('alice'), 'auditLogs'), { ...log, adminUid: 'alice' }));
    const created = await assertSucceeds(addDoc(collection(as('mod'), 'auditLogs'), log));
    await assertFails(deleteDoc(doc(as('mod'), 'auditLogs', created.id)));
  });

  test('students can delete their own data', async () => {
    await assertSucceeds(deleteDoc(doc(as('alice'), 'privateProfiles', 'alice')));
    await assertSucceeds(deleteDoc(doc(as('alice'), 'users', 'alice')));
    await assertFails(deleteDoc(doc(as('alice'), 'users', 'bob')));
  });
});

describe('profile photos (storage)', () => {
  const storageAs = (uid) => env.authenticatedContext(uid, campus(uid)).storage();
  const jpeg = new Uint8Array([0xff, 0xd8, 0xff, 0xd9]);

  test('students upload only their own image', async () => {
    await assertSucceeds(uploadBytes(ref(storageAs('alice'), 'profilePhotos/alice/avatar.jpg'), jpeg, { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(storageAs('alice'), 'profilePhotos/bob/avatar.jpg'), jpeg, { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(storageAs('alice'), 'profilePhotos/alice/notes.txt'), jpeg, { contentType: 'text/plain' }));
  });
});
