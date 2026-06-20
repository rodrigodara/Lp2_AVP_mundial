package pt.plataformaaluguerveiculos.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class AdicionarVeiculoView {

    private final VBox root;

    // Bytes das fotos selecionadas (índice 0 = foto1/capa, obrigatória)
    private final byte[][] fotosSelecionadas = new byte[4][];
    private final ImageView[] previews = new ImageView[4];

    private static final int TAMANHO_MAX_FOTO_MB = 5;

    public AdicionarVeiculoView() {
        root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30));

        Label titulo = new Label("Adicionar Veículo");
        titulo.getStyleClass().add("dashboard-titulo");

        Label subtitulo = new Label("Preenche os dados do teu veículo");
        subtitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(560);

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

        ComboBox<String> cbTipoVeiculo = new ComboBox<>();
        cbTipoVeiculo.getItems().addAll(
            "Citadino", "Hatchback", "Sedan", "SUV", "Carrinha", "Monovolume", "Descapotável", "Comercial"
        );
        cbTipoVeiculo.setPromptText("Seleciona o tipo");
        cbTipoVeiculo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Integer> cbLugares = new ComboBox<>();
        cbLugares.getItems().addAll(2, 4, 5, 7, 9);
        cbLugares.setValue(5);
        cbLugares.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbTransmissao = new ComboBox<>();
        cbTransmissao.getItems().addAll("Manual", "Automática");
        cbTransmissao.setValue("Manual");
        cbTransmissao.setMaxWidth(Double.MAX_VALUE);

        TextField tfPreco = new TextField();
        tfPreco.setPromptText("Ex: 35.00");

        TextField tfLocalizacao = new TextField();
        tfLocalizacao.setPromptText("Ex: Lisboa");

        TextField tfMatricula = new TextField();
        tfMatricula.setPromptText("Ex: AA-00-BB");

        TextField tfConsumo = new TextField();
        tfConsumo.setPromptText("Ex: 5.5 (litros/100km ou kWh/100km)");

        TextField tfQuilometragem = new TextField();
        tfQuilometragem.setPromptText("Ex: 25000");

        int row = 0;
        form.add(new Label("Marca:"),         0, row); form.add(tfMarca,        1, row++);
        form.add(new Label("Modelo:"),        0, row); form.add(tfModelo,       1, row++);
        form.add(new Label("Ano:"),           0, row); form.add(tfAno,          1, row++);
        form.add(new Label("Combustível:"),   0, row); form.add(cbCombustivel,  1, row++);
        form.add(new Label("Tipo de veículo:"), 0, row); form.add(cbTipoVeiculo, 1, row++);
        form.add(new Label("Lugares:"),       0, row); form.add(cbLugares,      1, row++);
        form.add(new Label("Transmissão:"),   0, row); form.add(cbTransmissao,  1, row++);
        form.add(new Label("Consumo:"),       0, row); form.add(tfConsumo,      1, row++);
        form.add(new Label("Quilometragem (km):"), 0, row); form.add(tfQuilometragem, 1, row++);
        form.add(new Label("Preço/Dia (€):"), 0, row); form.add(tfPreco,        1, row++);
        form.add(new Label("Localização:"),   0, row); form.add(tfLocalizacao,  1, row++);
        form.add(new Label("Matrícula:"),     0, row); form.add(tfMatricula,    1, row++);

        String fieldStyle = "-fx-pref-width: 300px; -fx-font-size: 13px;";
        tfMarca.setStyle(fieldStyle);
        tfModelo.setStyle(fieldStyle);
        tfAno.setStyle(fieldStyle);
        tfPreco.setStyle(fieldStyle);
        tfLocalizacao.setStyle(fieldStyle);
        tfMatricula.setStyle(fieldStyle);
        tfConsumo.setStyle(fieldStyle);
        tfQuilometragem.setStyle(fieldStyle);
        cbCombustivel.setStyle(fieldStyle);
        cbTipoVeiculo.setStyle(fieldStyle);
        cbLugares.setStyle(fieldStyle);
        cbTransmissao.setStyle(fieldStyle);

        form.getChildren().stream()
            .filter(n -> n instanceof Label)
            .forEach(n -> n.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;"));

        // ============================
        // FOTOS (até 4, a 1.ª obrigatória)
        // ============================
        Label lblFotos = new Label("Fotos do veículo");
        lblFotos.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1a237e;");

        Label lblFotosAjuda = new Label(
            "Adiciona até 4 fotos. A primeira foto é obrigatória e será usada como capa. (máx. "
            + TAMANHO_MAX_FOTO_MB + "MB por foto)"
        );
        lblFotosAjuda.setStyle("-fx-font-size: 12px; -fx-text-fill: #777777;");
        lblFotosAjuda.setWrapText(true);

        HBox fotosBox = new HBox(14);
        fotosBox.setAlignment(Pos.CENTER_LEFT);

        String[] legendas = {"Foto 1 (obrigatória)", "Foto 2", "Foto 3", "Foto 4"};
        for (int i = 0; i < 4; i++) {
            VBox slot = criarSlotFoto(legendas[i], i);
            fotosBox.getChildren().add(slot);
        }

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
            String tipoVeiculo = cbTipoVeiculo.getValue();
            Integer lugares    = cbLugares.getValue();
            String transmissao = cbTransmissao.getValue();
            String consumoStr  = tfConsumo.getText().trim().replace(",", ".");
            String kmStr       = tfQuilometragem.getText().trim();
            String precoStr    = tfPreco.getText().trim().replace(",", ".");
            String localizacao = tfLocalizacao.getText().trim();
            String matricula   = tfMatricula.getText().trim().toUpperCase();

            if (marca.isEmpty() || modelo.isEmpty() || anoStr.isEmpty()
                    || combustivel == null || tipoVeiculo == null || lugares == null
                    || transmissao == null || precoStr.isEmpty()
                    || localizacao.isEmpty() || matricula.isEmpty()) {
                mostrarErro("Preenche todos os campos.");
                return;
            }

            if (fotosSelecionadas[0] == null) {
                mostrarErro("A primeira foto é obrigatória.");
                return;
            }

            int ano;
            double preco;
            double consumo;
            int quilometragem;
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
            try {
                consumo = consumoStr.isEmpty() ? 5.0 : Double.parseDouble(consumoStr);
                if (consumo < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                mostrarErro("Consumo inválido. Usa um valor positivo (ou deixa em branco).");
                return;
            }
            try {
                quilometragem = kmStr.isEmpty() ? 0 : Integer.parseInt(kmStr);
                if (quilometragem < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                mostrarErro("Quilometragem inválida. Usa um valor positivo (ou deixa em branco).");
                return;
            }

            int proprietarioId = SessionManager.getInstance().getUtilizador().getId();
            Veiculo v = new Veiculo(
                marca, modelo, ano, combustivel, preco, localizacao, proprietarioId,
                "disponivel", matricula, tipoVeiculo, lugares, transmissao, consumo
            );
            v.setQuilometragem(quilometragem);
            v.setFoto1(fotosSelecionadas[0]);
            v.setFoto2(fotosSelecionadas[1]);
            v.setFoto3(fotosSelecionadas[2]);
            v.setFoto4(fotosSelecionadas[3]);

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

        root.getChildren().addAll(
            titulo, subtitulo, form,
            lblFotos, lblFotosAjuda, fotosBox,
            acoes
        );
    }

    /** Cria uma "caixa" com preview de imagem + botão para escolher ficheiro, para um dos 4 slots de foto. */
    private VBox criarSlotFoto(String legenda, int indice) {
        ImageView preview = new ImageView();
        preview.setFitWidth(110);
        preview.setFitHeight(80);
        preview.setPreserveRatio(false);
        previews[indice] = preview;

        VBox imagemBox = new VBox(preview);
        imagemBox.setAlignment(Pos.CENTER);
        imagemBox.setPrefSize(112, 82);
        imagemBox.setStyle(
            "-fx-background-color: #f0f0f0;" +
            "-fx-border-color: #cccccc;" +
            "-fx-border-style: dashed;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;"
        );

        Label lblLegenda = new Label(legenda);
        lblLegenda.setStyle("-fx-font-size: 11px; -fx-text-fill: #555555;");
        lblLegenda.setWrapText(true);
        lblLegenda.setMaxWidth(112);
        lblLegenda.setAlignment(Pos.CENTER);

        Button btnEscolher = new Button(indice == 0 ? "Escolher" : "Adicionar");
        btnEscolher.setStyle("-fx-font-size: 11px; -fx-padding: 4 10 4 10;");
        btnEscolher.getStyleClass().add("btn-secundario");

        Button btnRemover = new Button("Remover");
        btnRemover.setStyle("-fx-font-size: 11px; -fx-padding: 4 10 4 10;");
        btnRemover.getStyleClass().add("btn-secundario");
        btnRemover.setVisible(false);
        btnRemover.setManaged(false);

        btnEscolher.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Selecionar foto");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg")
            );
            File ficheiro = chooser.showOpenDialog(root.getScene() != null ? root.getScene().getWindow() : null);
            if (ficheiro == null) return;

            if (ficheiro.length() > TAMANHO_MAX_FOTO_MB * 1024L * 1024L) {
                mostrarErro("A foto excede o tamanho máximo de " + TAMANHO_MAX_FOTO_MB + "MB.");
                return;
            }

            try {
                byte[] bytes = carregarImagemComoBytes(ficheiro);
                fotosSelecionadas[indice] = bytes;
                preview.setImage(new Image(ficheiro.toURI().toString(), 110, 80, false, true));
                btnEscolher.setText("Trocar");
                btnRemover.setVisible(true);
                btnRemover.setManaged(true);
            } catch (IOException ex) {
                ex.printStackTrace();
                mostrarErro("Não foi possível carregar a imagem selecionada.");
            }
        });

        btnRemover.setOnAction(e -> {
            fotosSelecionadas[indice] = null;
            preview.setImage(null);
            btnEscolher.setText(indice == 0 ? "Escolher" : "Adicionar");
            btnRemover.setVisible(false);
            btnRemover.setManaged(false);
        });

        HBox botoesSlot = new HBox(6, btnEscolher, btnRemover);
        botoesSlot.setAlignment(Pos.CENTER);

        VBox slot = new VBox(6, imagemBox, lblLegenda, botoesSlot);
        slot.setAlignment(Pos.TOP_CENTER);
        slot.setPrefWidth(120);
        return slot;
    }

    /** Lê um ficheiro de imagem do disco e devolve os bytes originais (sem reencode). */
    private byte[] carregarImagemComoBytes(File ficheiro) throws IOException {
        return Files.readAllBytes(ficheiro.toPath());
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