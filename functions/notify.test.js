import assert from 'node:assert/strict';
import { test } from 'node:test';
import { deliverableTokens, firstName, matchPush, messagePush, otherUser, preview, staleTokens } from './notify.js';

test('first names and previews', () => {
  assert.equal(firstName('Ananya Menon'), 'Ananya');
  assert.equal(firstName('  '), 'Someone');
  assert.equal(preview('hi\n  there'), 'hi there');
  assert.equal(preview('x'.repeat(200)).length, 140);
});

test('the other match participant', () => {
  assert.equal(otherUser(['a', 'b'], 'a'), 'b');
  assert.equal(otherUser(['a', 'b'], 'b'), 'a');
  assert.equal(otherUser(undefined, 'a'), null);
});

test('tokens respect the push switch and are de-duplicated', () => {
  assert.deepEqual(deliverableTokens({ fcmTokens: ['t1', 't1', 't2'] }), ['t1', 't2']);
  assert.deepEqual(deliverableTokens({ fcmTokens: ['t1'], pushEnabled: false }), []);
  assert.deepEqual(deliverableTokens(undefined), []);
});

test('payloads are data-only and tell the app which chat to open', () => {
  const m = matchPush({ recipientId: 'a', partnerId: 'b', partnerName: 'Bob Kurian', tokens: ['t'] });
  assert.equal(m.notification, undefined);
  assert.deepEqual(m.data, {
    type: 'match', recipientId: 'a', partnerId: 'b', title: "It's a Spark! ✨", body: 'You and Bob liked each other. Say hi!',
  });
  const msg = messagePush({ recipientId: 'b', senderId: 'a', senderName: 'Ananya', text: 'hey!', tokens: ['t'] });
  assert.equal(msg.data.partnerId, 'a');
  assert.equal(msg.data.title, 'Ananya');
  assert.equal(msg.android.priority, 'high');
  for (const v of Object.values(msg.data)) assert.equal(typeof v, 'string'); // FCM data values must be strings
});

test('only permanently invalid tokens are removed', () => {
  const responses = [
    { success: true },
    { success: false, error: { code: 'messaging/registration-token-not-registered' } },
    { success: false, error: { code: 'messaging/internal-error' } },
  ];
  assert.deepEqual(staleTokens(['ok', 'dead', 'flaky'], responses), ['dead']);
});
