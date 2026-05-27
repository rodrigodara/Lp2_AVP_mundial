package com.aluguer.util;

import com.aluguer.model.User;

/**
 * Singleton que guarda o utilizador autenticado durante a sessão.
 * Usar SessionManager.getInstance().getUtilizador() em qualquer controller.
 */
public class SessionManager {

    private static SessionManager instance;
    private User utilizadorAtual;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /** Guarda o utilizador após login bem-sucedido. */
    public void iniciarSessao(User user) {
        this.utilizadorAtual = user;
    }

    /** Limpa a sessão — chamar no logout. */
    public void terminarSessao() {
        this.utilizadorAtual = null;
    }

    /** Devolve o utilizador atual, ou null se não há sessão. */
    public User getUtilizador() {
        return utilizadorAtual;
    }

    /** true se existe uma sessão ativa. */
    public boolean estaAutenticado() {
        return utilizadorAtual != null;
    }
}