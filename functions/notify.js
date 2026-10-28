// Pure helpers for building push payloads, kept free of Firebase calls so they can be unit-tested.

export const MAX_PREVIEW = 140;

export function firstName(name) {
  const trimmed = (name || '').trim();
  return trimmed ? trimmed.split(/\s+/)[0] : 'Someone';
}

export function preview(text) {
  const clean = (text || '').replace(/\s+/g, ' ').trim();
  return clean.length > MAX_PREVIEW ? `${clean.slice(0, MAX_PREVIEW - 1)}…` : clean;
}

/** The other participant of a two-person match. */
export function otherUser(users, uid) {
  return (users || []).find((u) => u !== uid) || null;
}

/** Tokens to notify, or none when the student turned push off. */
export function deliverableTokens(privateProfile) {
  if (!privateProfile || privateProfile.pushEnabled === false) return [];
  return [...new Set(privateProfile.fcmTokens || [])].filter((t) => typeof t === 'string' && t.length > 0);
}

/**
 * FCM payload. Data-only so the app always builds the notification itself (consistent channels,
 * lock-screen privacy and tap-to-open-chat), including when it's in the foreground.
 */
export function buildPush({ type, recipientId, partnerId, title, body, tokens }) {
  return {
    tokens,
    data: { type, recipientId, partnerId, title, body },
    android: {
      priority: 'high',
      ttl: type === 'match' ? 7 * 24 * 3600 * 1000 : 24 * 3600 * 1000,
      collapseKey: `${type}:${partnerId}`,
    },
  };
}

export function matchPush({ recipientId, partnerId, partnerName, tokens }) {
  return buildPush({
    type: 'match',
    recipientId,
    partnerId,
    title: "It's a Spark! ✨",
    body: `You and ${firstName(partnerName)} liked each other. Say hi!`,
    tokens,
  });
}

export function messagePush({ recipientId, senderId, senderName, text, tokens }) {
  return buildPush({
    type: 'message',
    recipientId,
    partnerId: senderId,
    title: firstName(senderName),
    body: preview(text) || 'Sent you a message',
    tokens,
  });
}

/** Tokens FCM reported as permanently invalid, to be removed from the student's profile. */
export function staleTokens(tokens, responses) {
  const dead = new Set(['messaging/registration-token-not-registered', 'messaging/invalid-registration-token']);
  return tokens.filter((_, i) => !responses[i]?.success && dead.has(responses[i]?.error?.code));
}
