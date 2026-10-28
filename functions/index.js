import { initializeApp } from 'firebase-admin/app';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { logger } from 'firebase-functions';
import { setGlobalOptions } from 'firebase-functions/v2';
import { onDocumentCreated } from 'firebase-functions/v2/firestore';
import { deliverableTokens, matchPush, messagePush, otherUser, staleTokens } from './notify.js';

initializeApp();
// Same region as Firestore (asia-south1 / Mumbai) keeps latency and egress down.
setGlobalOptions({ region: 'asia-south1', maxInstances: 10 });

const db = getFirestore();

async function loadStudent(uid) {
  const [profile, priv] = await Promise.all([
    db.doc(`users/${uid}`).get(),
    db.doc(`privateProfiles/${uid}`).get(),
  ]);
  return { uid, profile: profile.data(), tokens: deliverableTokens(priv.data()) };
}

async function isBlockedEitherWay(a, b) {
  const [ab, ba] = await Promise.all([db.doc(`blocks/${a}_${b}`).get(), db.doc(`blocks/${b}_${a}`).get()]);
  return ab.exists || ba.exists;
}

async function send(message, recipientId) {
  if (message.tokens.length === 0) return;
  const result = await getMessaging().sendEachForMulticast(message);
  const dead = staleTokens(message.tokens, result.responses);
  if (dead.length > 0) {
    await db.doc(`privateProfiles/${recipientId}`).update({ fcmTokens: FieldValue.arrayRemove(...dead) });
  }
  logger.info('push sent', { type: message.data.type, success: result.successCount, failed: result.failureCount, removed: dead.length });
}

/** "It's a Spark!" to both students when a mutual match is created. */
export const notifyNewMatch = onDocumentCreated('matches/{matchId}', async (event) => {
  const users = event.data?.data()?.users;
  if (!Array.isArray(users) || users.length !== 2) return;
  const [a, b] = await Promise.all(users.map(loadStudent));
  await Promise.all([
    send(matchPush({ recipientId: a.uid, partnerId: b.uid, partnerName: b.profile?.name, tokens: a.tokens }), a.uid),
    send(matchPush({ recipientId: b.uid, partnerId: a.uid, partnerName: a.profile?.name, tokens: b.tokens }), b.uid),
  ]);
});

/** New chat message → the other student in the match. */
export const notifyNewMessage = onDocumentCreated('matches/{matchId}/messages/{messageId}', async (event) => {
  const message = event.data?.data();
  if (!message?.senderId) return;
  const match = await db.doc(`matches/${event.params.matchId}`).get();
  const recipientId = otherUser(match.data()?.users, message.senderId);
  if (!recipientId || (await isBlockedEitherWay(message.senderId, recipientId))) return;

  const [sender, recipient] = await Promise.all([loadStudent(message.senderId), loadStudent(recipientId)]);
  if (!sender.profile || sender.profile.suspended) return;
  await send(
    messagePush({
      recipientId,
      senderId: sender.uid,
      senderName: sender.profile.name,
      text: message.text,
      tokens: recipient.tokens,
    }),
    recipientId,
  );
});
