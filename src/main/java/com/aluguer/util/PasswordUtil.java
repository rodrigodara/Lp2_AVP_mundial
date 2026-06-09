package com.aluguer.util;

import org.mindrot.jbcrypt.BCrypt;


public class PasswordUtil {

    private static final int SALT_ROUNDS = 12;

    private PasswordUtil() {}

    /**
     * Gera o hash BCrypt da password fornecida.
     *
     * @param plainPassword password em texto simples
     * @return hash BCrypt (60 caracteres)
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("A password não pode ser vazia.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(SALT_ROUNDS));
    }

    /**
     * Verifica se a password em texto simples corresponde ao hash guardado.
     *
     * @param plainPassword password introduzida pelo utilizador
     * @param hashedPassword hash guardado na base de dados
     * @return true se coincidir, false caso contrário
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) return false;
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
