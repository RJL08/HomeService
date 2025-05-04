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
    // 1) Obtener datos del mensaje
    const mensaje = event.data.data();
    if (!mensaje) return;
    const convId = event.params.convId;
    const emisor = mensaje.senderId;
    const texto = mensaje.texto || "";

    // 2) Leer la conversación para sacar participantes
    const convSnap = await db
      .collection("conversaciones")
      .doc(convId)
      .get();
    if (!convSnap.exists) return;
    const usuarios = convSnap.get("participants") || [];

    // 3) Por cada usuario (excepto emisor), leer sus tokens y enviar
    for (const uid of usuarios) {
      if (uid === emisor) continue;
      const userSnap = await db
        .collection("usuarios")
        .doc(uid)
        .get();
      const tokens = userSnap.get("fcmTokens") || [];

      for (const token of tokens) {
        const message = {
          token,
          notification: {
            title: "Nuevo mensaje",
            body: texto,
          },
          data: {
            conversationId: convId,
          },
        };
        try {
          await admin.messaging().send(message);
          console.log(`Noti OK a token ${token}`);
        } catch (err) {
          console.error(`Error token ${token}:`, err);
        }
      }
    }
  },
);


/* eslint-disable-next-line max-len */
