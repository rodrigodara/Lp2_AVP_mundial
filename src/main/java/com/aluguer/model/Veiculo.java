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
    private String tipoVeiculo;
    private int lugares;
    private String transmissao;
    private double consumo;
    private int quilometragem;

    // Fotos do veículo guardadas como BLOB. foto1 é a obrigatória/capa;
    // foto2-4 são opcionais. Carregadas à parte do construtor (via setters)
    // porque são dados binários potencialmente grandes.
    private byte[] foto1;
    private byte[] foto2;
    private byte[] foto3;
    private byte[] foto4;

    // Preenchido à parte (não vem diretamente da tabela veiculo) quando
    // se faz join/agregação com a tabela avaliacao. Por omissão -1
    // significa "sem avaliações" / "não calculado".
    private double avaliacaoMedia = -1;
    private int totalAvaliacoes = 0;

    // ------------------------------------------------------------
    // Construtor completo (com ID e todos os campos novos)
    // ------------------------------------------------------------
    public Veiculo(int id, String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado,
                   String matricula, String tipoVeiculo, int lugares, String transmissao, double consumo) {
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
        this.tipoVeiculo = tipoVeiculo;
        this.lugares = lugares;
        this.transmissao = transmissao;
        this.consumo = consumo;
    }

    // Construtor sem ID (para inserções) com todos os campos novos
    public Veiculo(String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado,
                   String matricula, String tipoVeiculo, int lugares, String transmissao, double consumo) {
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.combustivel = combustivel;
        this.precoDiario = precoDiario;
        this.localizacao = localizacao;
        this.proprietarioId = proprietarioId;
        this.estado = estado;
        this.matricula = matricula;
        this.tipoVeiculo = tipoVeiculo;
        this.lugares = lugares;
        this.transmissao = transmissao;
        this.consumo = consumo;
    }

    // ------------------------------------------------------------
    // Construtores legados (mantidos para não partir código existente).
    // Os campos novos ficam com os valores DEFAULT da tabela:
    // lugares=5, transmissao='Manual', consumo=5.00, tipoVeiculo=null
    // ------------------------------------------------------------
    public Veiculo(int id, String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado, String matricula) {
        this(id, marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId, estado,
                matricula, null, 5, "Manual", 5.00);
    }

    public Veiculo(String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado, String matricula) {
        this(marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId, estado,
                matricula, null, 5, "Manual", 5.00);
    }

    public Veiculo(String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado) {
        this(marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId, estado, null);
    }

    public Veiculo(int id, String marca, String modelo, int ano, String combustivel,
                   double precoDiario, String localizacao, int proprietarioId, String estado) {
        this(id, marca, modelo, ano, combustivel, precoDiario, localizacao, proprietarioId, estado, null);
    }

    // ------------------------------------------------------------
    // Getters e Setters
    // ------------------------------------------------------------
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

    public String getTipoVeiculo() { return tipoVeiculo; }
    public void setTipoVeiculo(String tipoVeiculo) { this.tipoVeiculo = tipoVeiculo; }

    public int getLugares() { return lugares; }
    public void setLugares(int lugares) { this.lugares = lugares; }

    public String getTransmissao() { return transmissao; }
    public void setTransmissao(String transmissao) { this.transmissao = transmissao; }

    public double getConsumo() { return consumo; }
    public void setConsumo(double consumo) { this.consumo = consumo; }

    public int getQuilometragem() { return quilometragem; }
    public void setQuilometragem(int quilometragem) { this.quilometragem = quilometragem; }

    public byte[] getFoto1() { return foto1; }
    public void setFoto1(byte[] foto1) { this.foto1 = foto1; }

    public byte[] getFoto2() { return foto2; }
    public void setFoto2(byte[] foto2) { this.foto2 = foto2; }

    public byte[] getFoto3() { return foto3; }
    public void setFoto3(byte[] foto3) { this.foto3 = foto3; }

    public byte[] getFoto4() { return foto4; }
    public void setFoto4(byte[] foto4) { this.foto4 = foto4; }

    /** Devolve todas as fotos não-nulas, na ordem 1-4 (para iterar facilmente nas views). */
    public java.util.List<byte[]> getFotos() {
        java.util.List<byte[]> fotos = new java.util.ArrayList<>();
        if (foto1 != null) fotos.add(foto1);
        if (foto2 != null) fotos.add(foto2);
        if (foto3 != null) fotos.add(foto3);
        if (foto4 != null) fotos.add(foto4);
        return fotos;
    }

    public double getAvaliacaoMedia() { return avaliacaoMedia; }
    public void setAvaliacaoMedia(double avaliacaoMedia) { this.avaliacaoMedia = avaliacaoMedia; }

    public int getTotalAvaliacoes() { return totalAvaliacoes; }
    public void setTotalAvaliacoes(int totalAvaliacoes) { this.totalAvaliacoes = totalAvaliacoes; }

    @Override
    public String toString() {
        return marca + " " + modelo + " (" + ano + ")";
    }
}