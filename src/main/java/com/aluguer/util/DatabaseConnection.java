package com.aluguer.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // -----------------------------
    // CONFIGURAÇÃO MYSQL (XAMPP)
    // -----------------------------
  private static final String URL =
        "jdbc:mysql://localhost:3306/aluguer_veiculos"
        + "?useSSL=false"
        + "&serverTimezone=UTC";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    // -----------------------------
    // SINGLETON CONNECTION
    // -----------------------------
    private static Connection instance = null;

    private DatabaseConnection() {
        // impede criação de objetos
    }

public static Connection getConnection() throws SQLException {
    try {
        if (instance == null || instance.isClosed()) {
            instance = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[DB] Ligação ao MySQL estabelecida com sucesso.");
        }
    } catch (SQLException e) {
        throw new SQLException("Erro ao conectar à base de dados: " + e.getMessage(), e);
    }
    return instance;
}

    public static void closeConnection() {
        if (instance != null) {
            try {
                if (!instance.isClosed()) {
                    instance.close();
                    System.out.println("[DB] Ligação fechada.");
                }
            } catch (SQLException e) {
                System.err.println("[DB] Erro ao fechar ligação: " + e.getMessage());
            } finally {
                instance = null;
            }
        }
    }
}