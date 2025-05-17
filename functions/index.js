/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// functions/index.js
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.enviarNotificacionChat = onDocumentCreated(
  {
    region: "us-central1",
    document: "conversaciones/{convId}/mensajes/{msgId}",
  },
  async (event) => {
    /* ────── 1 · DATOS DEL MENSAJE ────── */
    const mensaje = event.data.data();
    if (!mensaje) return;
    const convId = event.params.convId;
    const emisor = mensaje.senderId;
    console.log("Mensaje:", mensaje);

    /* ────── 1.1 · TOKENS DEL EMISOR ────── */
    const emSnap = await db
      .collection("usuarios")
      .doc(emisor)
      .get();
    const toksEmisor = emSnap.get("fcmTokens") || [];

    /* ────── 2 · PARTICIPANTES DE LA CONV ────── */
    const convSnap = await db
      .collection("conversaciones")
      .doc(convId)
      .get();
    if (!convSnap.exists) return;
    const usuarios = convSnap.get("participants") || [];
    console.log("Participants:", usuarios, "Emisor:", emisor);

    /* ────── 3 · ENVÍO A CADA RECEPTOR ────── */
    for (const uid of usuarios) {
      if (uid === emisor) continue; // no notificar al emisor
      const userSnap = await db.collection("usuarios").doc(uid).get();
      const userTok = userSnap.get("fcmTokens") || [];

      // quita los tokens que también están en el emisor
      const tokens = userTok.filter((t) => !toksEmisor.includes(t));

      for (const token of tokens) {
        const message = {
          token,
          notification: { title: "Nuevo mensaje", body: "Tiene un mensaje nuevo" },
          data: { conversationId: convId },
        };

        try {
          await admin.messaging().send(message);
          console.log(`Noti OK → ${token}`);
        } catch (err) {
          console.error(`Error token ${token}:`, err);
          if (err.code ===
              "messaging/registration-token-not-registered") {
            await userSnap.ref.update({
              fcmTokens: admin.firestore.FieldValue.arrayRemove(token),
            });
            console.log(`Token ${token} eliminado`);
          }
        }
      }
    }
  },
);

/* eslint-disable-next-line max-len */
