package com.aluguer.model;

import java.time.LocalDateTime;

public class Transaction {

    public enum Tipo {
        deposito,
        levantamento
    }

    private int id;
    private int contaId;
    private double valor;
    private Tipo tipo;
    private LocalDateTime data;

    // Construtor completo (usado pelo DAO ao mapear da BD)
    public Transaction(int id, int contaId, double valor, Tipo tipo, LocalDateTime data) {
        this.id      = id;
        this.contaId = contaId;
        this.valor   = valor;
        this.tipo    = tipo;
        this.data    = data;
    }

    // Construtor sem id (para inserir na BD)
    public Transaction(int contaId, double valor, Tipo tipo) {
        this.contaId = contaId;
        this.valor   = valor;
        this.tipo    = tipo;
        this.data    = LocalDateTime.now();
    }

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------

    public int getId()             { return id; }
    public void setId(int id)      { this.id = id; }

    public int getContaId()              { return contaId; }
    public void setContaId(int contaId)  { this.contaId = contaId; }

    public double getValor()             { return valor; }
    public void setValor(double valor)   { this.valor = valor; }

    public Tipo getTipo()                { return tipo; }
    public void setTipo(Tipo tipo)       { this.tipo = tipo; }

    public LocalDateTime getData()                { return data; }
    public void setData(LocalDateTime data)       { this.data = data; }

    @Override
    public String toString() {
        return "Transaction #" + id +
                " | Conta: " + contaId +
                " | Tipo: " + tipo +
                " | Valor: " + String.format("%.2f€", valor) +
                " | Data: " + data;
    }
}
