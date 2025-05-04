module.exports = {
  env: {
    es6: true,
    node: true,
  },
  parserOptions: {
    ecmaVersion: 2020, // Actualizado para soportar sintaxis moderna
    sourceType: "module", // Necesario para módulos de ES6
  },
  extends: [
    "eslint:recommended",
    "google",
  ],
  rules: {
    "max-len": ["error", { code: 120 }], // Aumenta límite de caracteres por línea
    "no-undef": "off", // Desactiva verificación de variables no definidas
    "require-jsdoc": "off", // Google style lo exige, pero es opcional
    "quotes": ["error", "double", { allowTemplateLiterals: true }],
    "indent": ["error", 2], // Usar 2 espacios en lugar de 4 del estilo Google
    "object-curly-spacing": ["error", "always"], // Espacios en llaves
  },
  globals: {
    // Agrega aquí las variables globales de Firebase
    admin: "readonly",
    onDocumentCreated: "readonly",
    db: "readonly",
    messaging: "readonly",
  },
};
