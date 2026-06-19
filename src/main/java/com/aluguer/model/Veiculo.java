package com.aluguer.model;

public class Veiculo {

    private int id;
    private String marca;
    private String modelo;
    private int ano;
    private String combustivel;
    private double precoDiario;
    private String localizacao;
    private int proprietarioId;
    private String estado;
    private String matricula;

    // Construtor completo (com ID)
    public Veiculo(int id, String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado, String matricula) {
        this.id = id;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.combustivel = combustivel;
        this.precoDiario = precoDiario;
        this.localizacao = localizacao;
        this.proprietarioId = proprietarioId;
        this.estado = estado;
        this.matricula = matricula;
    }

    // Construtor sem ID (para inserções)
    public Veiculo(String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado, String matricula) {
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.combustivel = combustivel;
        this.precoDiario = precoDiario;
        this.localizacao = localizacao;
        this.proprietarioId = proprietarioId;
        this.estado = estado;
        this.matricula = matricula;
    }

    // Construtor legado sem matrícula (para não partir código existente)
    public Veiculo(String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado) {
        this(marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId, estado, null);
    }

    public Veiculo(int id, String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado) {
        this(id, marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId, estado, null);
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public String getCombustivel() { return combustivel; }
    public void setCombustivel(String combustivel) { this.combustivel = combustivel; }

    public double getPrecoDiario() { return precoDiario; }
    public void setPrecoDiario(double precoDiario) { this.precoDiario = precoDiario; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public int getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(int proprietarioId) { this.proprietarioId = proprietarioId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    @Override
    public String toString() {
        return marca + " " + modelo + " (" + ano + ")";
    }
}