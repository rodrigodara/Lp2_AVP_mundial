package com.aluguer.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utilitário de ligação à base de dados MySQL.
 *
 * Usa o padrão Singleton para garantir que só existe
 * uma ligação ativa durante toda a sessão da aplicação.
 *
 * Configuração: edita as constantes URL, USER e PASSWORD
 * com os dados do teu servidor MySQL local.
 */
public class DatabaseConnection {

    // -------------------------------------------------------------------------
    // Configuração da ligação — EDITAR CONFORME O TEU AMBIENTE
    // -------------------------------------------------------------------------
    private static final String URL      = "jdbc:mysql://localhost:3306/aluguer_veiculos"
                                         + "?useSSL=false"
                                         + "&allowPublicKeyRetrieval=true"
                                         + "&serverTimezone=Europe/Lisbon"
                                         + "&characterEncoding=utf8";
    private static final String USER     = "root";       // utilizador MySQL
    private static final String PASSWORD = "password";   // password MySQL

    // -------------------------------------------------------------------------
    // Singleton
    // -------------------------------------------------------------------------
    private static Connection instance = null;

    /** Construtor privado — impede instanciação direta */
    private DatabaseConnection() {}

    /**
     * Devolve a ligação à base de dados.
     * Cria uma nova ligação se ainda não existir ou se tiver sido fechada.
     *
     * @return Connection ativa ao MySQL
     * @throws SQLException se não conseguir ligar
     */
    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                // Carrega o driver JDBC do MySQL (necessário em Java < 9)
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DB] Ligação ao MySQL estabelecida com sucesso.");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL não encontrado. "
                    + "Verifica se o mysql-connector-j está no pom.xml", e);
            }
        }
        return instance;
    }

    /**
     * Fecha a ligação à base de dados.
     * Chama este método quando a aplicação JavaFX fechar (no stop() da Application).
     */
    public static void closeConnection() {
        if (instance != null) {
            try {
                if (!instance.isClosed()) {
                    instance.close();
                    System.out.println("[DB] Ligação ao MySQL fechada.");
                }
            } catch (SQLException e) {
                System.err.println("[DB] Erro ao fechar ligação: " + e.getMessage());
            } finally {
                instance = null;
            }
        }
    }
}
