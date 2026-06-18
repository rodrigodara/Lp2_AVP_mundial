package com.aluguer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.aluguer.model.User;

public class SessionManager {

    private static SessionManager instance;
    private User utilizadorAtual;

    // 👇 NOVO: listeners para UI reagir a mudanças
    private final List<Consumer<User>> listeners = new ArrayList<>();

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
        notificar();
    }

    /** Limpa a sessão — chamar no logout. */
    public void terminarSessao() {
        this.utilizadorAtual = null;
        notificar();
    }

    /** Devolve o utilizador atual */
    public User getUtilizador() {
        return utilizadorAtual;
    }

    public boolean estaAutenticado() {
        return utilizadorAtual != null;
    }

    // =========================
    // 🔔 NOVO SISTEMA DE REACT
    // =========================

    public void addListener(Consumer<User> listener) {
        listeners.add(listener);
    }

    private void notificar() {
        for (Consumer<User> l : listeners) {
            l.accept(utilizadorAtual);
        }
    }
}