package com.example.homeservice.utils;

import android.text.TextUtils;

public class ValidacionUtils {

    // Valida el nombre: no vacío, máx 20 chars
    public static boolean validarNombre(String nombre) {
        if (TextUtils.isEmpty(nombre)) return false;
        return nombre.length() <= 20;
    }

    // Valida apellidos: máx 30 chars
    public static boolean validarApellidos(String apellidos) {
        return apellidos.length() <= 30;
    }

    // Valida correo con regex
    public static boolean validarCorreo(String correo) {
        if (correo == null) return false;
        // Ejemplo: .com o .es
        String regexEmail = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.(com|es)$";
        return correo.matches(regexEmail);
    }

    // Valida contraseña: 8-12 caracteres
    public static boolean validarContrasena(String contrasena) {
        if (contrasena == null) return false;
        int len = contrasena.length();
        return (len >= 8 && len <= 18);
    }

}


