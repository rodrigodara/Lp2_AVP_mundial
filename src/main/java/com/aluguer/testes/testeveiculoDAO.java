package com.aluguer.testes;

import java.sql.Connection;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;
import com.aluguer.util.DatabaseConnection;

public class TesteVeiculoDAO {

    public static void main(String[] args) {

        Connection conn = DatabaseConnection.connect();
        VeiculoDAO dao = new VeiculoDAO(conn);

        Veiculo v = new Veiculo(
            "Audi", "A3", 2019, "Gasolina", 70.00, "Lisboa", 1
        );

        boolean inserido = dao.inserir(v);
        System.out.println("Inserido? " + inserido);

        dao.listarTodos().forEach(System.out::println);
    }
}
