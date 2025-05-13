const functions = require("firebase-functions");
const mailjet = require("node-mailjet");

exports.enviarCodigoVerificacion = functions.https.onCall(async (data, context) => {
  console.log("📦 Datos recibidos por la función:", {
  email: data?.data?.email,
  codigo: data?.data?.codigo
  });


  const { email, codigo } = data.data; 

  if (!email || !codigo) {
    throw new functions.https.HttpsError("invalid-argument", "Faltan email o código");
  }


  const mailjetClient = mailjet.apiConnect(
    "d2454fb4690a32be49f64ae7301e868a",
    "40b8ca377046ec6e81b83c748ecedebb"
  );

  try {
    // 5. Envío con más detalles de debug
    const request = await mailjetClient
      .post("send", { version: "v3.1" })
      .request({
        Messages: [{
          From: { 
            Email: "	cristiana.saldarriag@utadeo.edu.co", 
            Name: "Vademecum" 
          },
          To: [{ Email: email }],
          Subject: "Código de verificación",
          TextPart: `Tu código es: ${codigo}`,
          HTMLPart: `<h3>Tu código: <strong>${codigo}</strong></h3>`
        }]
      });

    console.log("✅ Correo enviado. Respuesta Mailjet:", request.body);
    return { 
      success: true,
      messageId: request.body.Messages[0].Status,
      email: email 
    };

  } catch (err) {
    console.error("🚨 Error completo Mailjet:", {
      message: err.message,
      stack: err.stack,
      statusCode: err.statusCode,
      response: err.response?.body
    });
    
    throw new functions.https.HttpsError(
      "internal", 
      "Error al enviar correo",
      { 
        error: err.message,
        detalles: err.response?.body 
      }
    );
  }
});

