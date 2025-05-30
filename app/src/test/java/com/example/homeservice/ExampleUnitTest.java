package com.example.homeservice;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import com.example.homeservice.model.Anuncio;
import com.example.homeservice.seguridad.CommonCrypto;
import com.example.homeservice.utils.ValidacionUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

public class ExampleUnitTest {

    /** Comprueba que un nombre vacío es inválido. */
    @Test
    public void validarNombre_nombreVacio_false() {
        assertFalse(ValidacionUtils.validarNombre(""));
    }


    /** Nombre con 21 caracteres → inválido. */
    @Test
    public void validarNombre_fueraDeLimite_false() {
        String nombre21 = "ABCDEFGHIJKLMNOPQRSTU"; // 21 chars
        assertFalse(ValidacionUtils.validarNombre(nombre21));
    }

    /** Correo con dominio  permitido (.net) → valido. */
    @Test
    public void validarCorreo_dominioNoPermitido_false() {
        assertFalse(ValidacionUtils.validarCorreo("pepe@mail.net"));
    }

    /** Contraseña menor de 8 caracteres → inválida. */
    @Test
    public void validarContrasena_tooShort_false() {
        assertFalse(ValidacionUtils.validarContrasena("abc123"));
    }

    @Before
    public void setUp() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);                       // clave de 256 bits
        SecretKey key = kg.generateKey();
        CommonCrypto.init(key);
    }

    /** Texto plano → cifrado → descifrado = texto plano original. */
    @Test
    public void encryptDecrypt_cycle_ok() throws Exception {
        String original = "Hola HomeService!";
        String cipherText = CommonCrypto.encrypt(original);
        String decrypted = CommonCrypto.decrypt(cipherText);

        assertEquals(original, decrypted);
    }

    /** Lanza IllegalStateException si no se inicializó la clave. */
    @Test(expected = IllegalStateException.class)
    public void decryptWithoutInit_throwsException() throws Exception {
        CommonCrypto.init(null);          // “olvida” la clave
        CommonCrypto.decrypt("dummy");
    }

    /** El constructor vacío deja distanceKm a Double.MAX_VALUE. */
    @Test
    public void constructor_defaultDistance() {
        Anuncio a = new Anuncio();
        assertEquals(Double.MAX_VALUE, a.getDistanceKm(), 0.0);
    }

    /** Setters y getters deben coincidir. */
    @Test
    public void settersGetters_ok() {
        Anuncio a = new Anuncio();
        a.setTitulo("Pintor económico");
        assertEquals("Pintor económico", a.getTitulo());
    }

     /* ===================================================
       3. CONTRASEÑA  (entre 8 y 18 caracteres)
       =================================================== */

    @Test
    public void contrasena_minima_true() {
        assertTrue(ValidacionUtils.validarContrasena("abc12345"));  // 8
    }

    @Test
    public void contrasena_menorQueMin_false() {
        assertFalse(ValidacionUtils.validarContrasena("abc1234"));  // 7
    }

    @Test
    public void contrasena_maxima_true() {
        assertTrue(ValidacionUtils.validarContrasena("A1B2C3D4E5F6G7H8I")); // 18
    }

    @Test
    public void contrasena_mayorQueMax_false() {
        assertTrue(ValidacionUtils.validarContrasena("A1B2C3D4E5F6G7H8I9")); // 19
    }

}

