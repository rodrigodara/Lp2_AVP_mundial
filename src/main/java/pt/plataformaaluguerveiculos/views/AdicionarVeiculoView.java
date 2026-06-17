package pt.plataformaaluguerveiculos.views;

import com.aluguer.model.Veiculo;
import com.aluguer.service.VeiculoService;
import com.aluguer.util.SessionManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class AdicionarVeiculoView {

    private final VBox root;

    public AdicionarVeiculoView() {
        root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titulo = new Label("Adicionar Veículo");
        titulo.getStyleClass().add("dashboard-titulo");

        Label subtitulo = new Label("Preenche os dados do teu veículo");
        subtitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        // ============================
        // FORMULÁRIO
        // ============================
        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(500);

        TextField tfMarca = new TextField();
        tfMarca.setPromptText("Ex: Toyota");

        TextField tfModelo = new TextField();
        tfModelo.setPromptText("Ex: Yaris");

        TextField tfAno = new TextField();
        tfAno.setPromptText("Ex: 2020");

        ComboBox<String> cbCombustivel = new ComboBox<>();
        cbCombustivel.getItems().addAll("Gasolina", "Diesel", "Elétrico", "Híbrido", "GPL");
        cbCombustivel.setPromptText("Seleciona o combustível");
        cbCombustivel.setMaxWidth(Double.MAX_VALUE);

        TextField tfPreco = new TextField();
        tfPreco.setPromptText("Ex: 35.00");

        TextField tfLocalizacao = new TextField();
        tfLocalizacao.setPromptText("Ex: Lisboa");

        form.add(new Label("Marca:"),        0, 0); form.add(tfMarca,        1, 0);
        form.add(new Label("Modelo:"),       0, 1); form.add(tfModelo,       1, 1);
        form.add(new Label("Ano:"),          0, 2); form.add(tfAno,          1, 2);
        form.add(new Label("Combustível:"),  0, 3); form.add(cbCombustivel,  1, 3);
        form.add(new Label("Preço/Dia (€):"),0, 4); form.add(tfPreco,        1, 4);
        form.add(new Label("Localização:"),  0, 5); form.add(tfLocalizacao,  1, 5);

        // Estilo dos labels
        form.getChildren().stream()
            .filter(n -> n instanceof Label)
            .forEach(n -> n.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;"));

        // Estilo dos campos
        String fieldStyle = "-fx-pref-width: 280px; -fx-font-size: 13px;";
        tfMarca.setStyle(fieldStyle);
        tfModelo.setStyle(fieldStyle);
        tfAno.setStyle(fieldStyle);
        tfPreco.setStyle(fieldStyle);
        tfLocalizacao.setStyle(fieldStyle);
        cbCombustivel.setStyle(fieldStyle);

        // ============================
        // BOTÕES
        // ============================
        Button btnGuardar = new Button("Guardar Veículo");
        btnGuardar.getStyleClass().add("btn-primario");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");

        btnCancelar.setOnAction(e ->
            NavigationManager.getInstance().navegarParaMeusVeiculos()
        );

        btnGuardar.setOnAction(e -> {
            String marca       = tfMarca.getText().trim();
            String modelo      = tfModelo.getText().trim();
            String anoStr      = tfAno.getText().trim();
            String combustivel = cbCombustivel.getValue();
            String precoStr    = tfPreco.getText().trim().replace(",", ".");
            String localizacao = tfLocalizacao.getText().trim();

            // Validação
            if (marca.isEmpty() || modelo.isEmpty() || anoStr.isEmpty()
                    || combustivel == null || precoStr.isEmpty() || localizacao.isEmpty()) {
                mostrarErro("Preenche todos os campos.");
                return;
            }

            int ano;
            double preco;
            try {
                ano = Integer.parseInt(anoStr);
                if (ano < 1900 || ano > 2100) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                mostrarErro("Ano inválido.");
                return;
            }
            try {
                preco = Double.parseDouble(precoStr);
                if (preco <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                mostrarErro("Preço inválido. Usa um valor positivo.");
                return;
            }

            int proprietarioId = SessionManager.getInstance().getUtilizador().getId();
            Veiculo v = new Veiculo(marca, modelo, ano, combustivel, preco, localizacao, proprietarioId, "disponivel");

            try {
                VeiculoService service = new VeiculoService();
                boolean ok = service.inserir(v);
                if (ok) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION,
                        "Veículo adicionado com sucesso!", ButtonType.OK);
                    alert.setTitle("Sucesso");
                    alert.setHeaderText(null);
                    alert.showAndWait();
                    NavigationManager.getInstance().navegarParaMeusVeiculos();
                } else {
                    mostrarErro("Não foi possível adicionar o veículo. Tenta novamente.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                mostrarErro("Erro ao guardar veículo: " + ex.getMessage());
            }
        });

        HBox acoes = new HBox(15, btnGuardar, btnCancelar);
        acoes.setAlignment(Pos.CENTER);

        root.getChildren().addAll(titulo, subtitulo, form, acoes);
    }

    private void mostrarErro(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public VBox getRoot() {
        return root;
    }
}
