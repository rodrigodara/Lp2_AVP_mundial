package com.aluguer.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gestão de conexões à BD via HikariCP connection pool.
 *
 * PROBLEMA ANTERIOR: singleton com uma única Connection partilhada.
 * Quando qualquer DAO usava try-with-resources, fechava a conexão
 * global, deixando todos os outros DAOs sem conexão.
 *
 * SOLUÇÃO: HikariCP mantém um pool de conexões reutilizáveis.
 * Cada chamada a getConnection() devolve uma conexão do pool.
 * Quando o try-with-resources a fecha, ela volta ao pool (não fecha mesmo).
 * Assim cada DAO tem a sua própria conexão independente.
 */
public class DatabaseConnection {

    private static final String URL =
        "jdbc:mysql://localhost:3306/aluguer_veiculos"
        + "?useSSL=false"
        + "&serverTimezone=UTC";

    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);

        // Pool entre 2 e 10 conexões simultâneas
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);

        // Timeout de 30s à espera de conexão livre
        config.setConnectionTimeout(30_000);

        // Testar conexão antes de a entregar
        config.setConnectionTestQuery("SELECT 1");

        config.setPoolName("AVL-Pool");

        dataSource = new HikariDataSource(config);
        System.out.println("[DB] HikariCP pool iniciado com sucesso.");
    }

    private DatabaseConnection() {}

    /**
     * Devolve uma conexão do pool.
     * Deve ser usada dentro de try-with-resources — ao fechar,
     * a conexão volta ao pool automaticamente (não fecha mesmo).
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Encerra o pool (chamar apenas ao fechar a aplicação).
     */
    public static void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DB] Pool HikariCP encerrado.");
        }
    }
}