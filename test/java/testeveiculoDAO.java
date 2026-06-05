package test.java;

import java.sql.Connection;
import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;
import com.aluguer.util.DatabaseConnection;

public class testeveiculoDAO {

    public static void main(String[] args) {

        Connection conn = DatabaseConnection.getConnection();

        VeiculoDAO dao = new VeiculoDAO(conn);

        Veiculo v = new Veiculo(
    "Audi", "A3", 2019, "Gasolina", 70.00, "Lisboa", 1, "disponivel"
);


        boolean inserido = dao.inserir(v);
        System.out.println("Inserido? " + inserido);

        dao.listarTodos().forEach(System.out::println);
    }
}
