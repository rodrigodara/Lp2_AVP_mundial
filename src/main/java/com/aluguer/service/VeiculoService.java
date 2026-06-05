import java.sql.Connection;
import java.util.List;

import com.aluguer.dao.VeiculoDAO;
import com.aluguer.model.Veiculo;




public class VeiculoService {

    private VeiculoDAO dao;

    public VeiculoService(Connection conn) {
        this.dao = new VeiculoDAO(conn);
    }

    public List<Veiculo> getAllVehicles() {
        return dao.listarTodos();
    }
}
