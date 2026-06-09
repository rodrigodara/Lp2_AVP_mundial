package com.aluguer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {


    /** Chave primária gerada pelo MySQL (AUTO_INCREMENT) */
    private int id;

    /** Email único — usado no login e nas notificações */
    private String email;

    /** Nome completo do utilizador */
    private String nome;

    /** Número de Identificação Fiscal (9 dígitos) */
    private String nif;

    /** Número da carta de condução */
    private String numeroCarta;

    /** Data de validade da carta de condução */
    private LocalDate validadeCarta;

    /**
     * Password armazenada em hash BCrypt — NUNCA em texto simples.
     * O hash é gerado em ALV-26 antes de ser guardado aqui.
     */
    private String passwordHash;

    /** Saldo disponível em conta (em euros) — usado para pagar alugueres */
    private BigDecimal saldo;

    /**
     * Perfil do utilizador:
     *   "UTILIZADOR"     → acesso normal à plataforma
     *   "ADMINISTRADOR"  → acesso ao painel de administração (RF6)
     */
    private String perfil;

    /** Indica se a conta está ativa ou bloqueada pelo administrador */
    private boolean ativo;

    /** Data e hora em que a conta foi criada */
    private LocalDateTime dataCriacao;

    // -------------------------------------------------------------------------
    // Construtores
    // -------------------------------------------------------------------------

    /** Construtor vazio — necessário para instanciar a partir do ResultSet do JDBC */
    public User() {
        this.saldo  = BigDecimal.ZERO;
        this.perfil = "UTILIZADOR";
        this.ativo  = true;
    }

    /**
     * Construtor de registo — usado em ALV-27 ao criar uma nova conta.
     * O id, saldo, perfil, ativo e dataCriacao ficam com os valores por defeito.
     */
    public User(String email, String nome, String nif,
                String numeroCarta, LocalDate validadeCarta,
                String passwordHash) {
        this();
        this.email         = email;
        this.nome          = nome;
        this.nif           = nif;
        this.numeroCarta   = numeroCarta;
        this.validadeCarta = validadeCarta;
        this.passwordHash  = passwordHash;
    }

    /**
     * Construtor completo — usado ao carregar um utilizador da base de dados
     * (ex: depois de fazer login ou dentro de um DAO).
     */
    public User(int id, String email, String nome, String nif,
                String numeroCarta, LocalDate validadeCarta,
                String passwordHash, BigDecimal saldo,
                String perfil, boolean ativo, LocalDateTime dataCriacao) {
        this.id            = id;
        this.email         = email;
        this.nome          = nome;
        this.nif           = nif;
        this.numeroCarta   = numeroCarta;
        this.validadeCarta = validadeCarta;
        this.passwordHash  = passwordHash;
        this.saldo         = saldo;
        this.perfil        = perfil;
        this.ativo         = ativo;
        this.dataCriacao   = dataCriacao;
    }

    // -------------------------------------------------------------------------
    // Getters e Setters
    // -------------------------------------------------------------------------

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    public String getNumeroCarta() { return numeroCarta; }
    public void setNumeroCarta(String numeroCarta) { this.numeroCarta = numeroCarta; }

    public LocalDate getValidadeCarta() { return validadeCarta; }
    public void setValidadeCarta(LocalDate validadeCarta) { this.validadeCarta = validadeCarta; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    // -------------------------------------------------------------------------
    // Métodos de utilidade
    // -------------------------------------------------------------------------

    /** Devolve true se o utilizador é administrador */
    public boolean isAdministrador() {
        return "ADMINISTRADOR".equals(this.perfil);
    }

    /** Devolve true se a carta de condução ainda está dentro da validade */
    public boolean isCartaValida() {
        if (validadeCarta == null) return false;
        return !validadeCarta.isBefore(LocalDate.now());
    }

    /** Devolve true se o saldo é suficiente para cobrir o valor pedido */
    public boolean temSaldoSuficiente(BigDecimal valor) {
        if (valor == null || saldo == null) return false;
        return saldo.compareTo(valor) >= 0;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", nome='" + nome + '\'' +
                ", nif='" + nif + '\'' +
                ", perfil='" + perfil + '\'' +
                ", ativo=" + ativo +
                ", saldo=" + saldo +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
