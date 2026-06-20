package pt.plataformaaluguerveiculos.views;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.aluguer.dao.AdminDAO;
import com.aluguer.dao.AvaliacaoDAO;
import com.aluguer.model.User;
import com.aluguer.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * AdminView — Painel de Administração.
 *
 * Separadores:
 *   1. Utilizadores  — listar, pesquisar, ver avisos, emitir aviso, bloquear/desbloquear
 *   2. Veículos      — listar, pesquisar, remover por violação
 *   3. Estatísticas  — gerais + filtros por período / marca / região
 */
public class AdminView {

    private final VBox root;
    private final AdminDAO dao = new AdminDAO();
    private final int adminId;

    // -------------------------------------------------------------------------
    // Construtor
    // -------------------------------------------------------------------------
    public AdminView() {
        User admin = SessionManager.getInstance().getUtilizador();
        this.adminId = admin != null ? admin.getId() : -1;

        root = new VBox(0);
        root.setStyle("-fx-background-color: #F8FAFC;");

        // Cabeçalho
        HBox header = criarHeader();

        // TabPane principal
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color: #F8FAFC;");

        Tab tabUtilizadores  = new Tab("👤 Utilizadores",  buildUtilizadoresPane());
        Tab tabVeiculos      = new Tab("🚗 Veículos",      buildVeiculosPane());
        Tab tabEstatisticas  = new Tab("📊 Estatísticas",  buildEstatisticasPane());

        tabs.getTabs().addAll(tabUtilizadores, tabVeiculos, tabEstatisticas);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        root.getChildren().addAll(header, tabs);
    }

    public VBox getRoot() { return root; }

    // =========================================================================
    // CABEÇALHO
    // =========================================================================
    private HBox criarHeader() {
        Label titulo = new Label("Painel de Administração");
        titulo.setStyle(
            "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;"
        );

        User admin = SessionManager.getInstance().getUtilizador();
        Label sub = new Label(admin != null ? "Sessão: " + admin.getNome() : "");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #93C5FD;");

        VBox left = new VBox(2, titulo, sub);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(left);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.setStyle("-fx-background-color: #2563EB;");
        return header;
    }

    // =========================================================================
    // 1. UTILIZADORES
    // =========================================================================
    private ScrollPane buildUtilizadoresPane() {
        VBox pane = new VBox(18);
        pane.setPadding(new Insets(24));
        pane.setStyle("-fx-background-color: #F8FAFC;");

        // ---- Barra de pesquisa ----
        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Pesquisar por nome ou email…");
        tfSearch.setPrefWidth(300);
        tfSearch.setStyle(campoCss());

        ComboBox<String> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll("Todos", "admin", "proprietario", "locatario");
        cbTipo.setValue("Todos");
        cbTipo.setStyle(campoCss() + " -fx-pref-width: 150px;");

        Button btnSearch = botao("Pesquisar", "#2563EB", "white");
        Button btnTodos  = botao("Ver Todos", "#64748B", "white");

        HBox barSearch = new HBox(10, tfSearch, cbTipo, btnSearch, btnTodos);
        barSearch.setAlignment(Pos.CENTER_LEFT);

        // ---- Tabela ----
        TableView<User> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPrefHeight(320);

        tabela.getColumns().addAll(
            colStr("ID",     u -> String.valueOf(u.getId()),         60),
            colStr("Nome",   User::getNome,                          180),
            colStr("Email",  User::getEmail,                         220),
            colStr("Perfil", User::getPerfil,                        100),
            colStr("Tipo",   u -> u.getTipo() == null ? "—" : u.getTipo(), 100),
            colStr("Ativo",  u -> u.isAtivo() ? "✅ Sim" : "❌ Não", 80)
        );

        carregarUtilizadores(tabela, null, null);

        btnSearch.setOnAction(e -> {
            String tipo = "Todos".equals(cbTipo.getValue()) ? null : cbTipo.getValue();
            carregarUtilizadores(tabela, tfSearch.getText().trim(), tipo);
        });
        btnTodos.setOnAction(e -> {
            tfSearch.clear();
            cbTipo.setValue("Todos");
            carregarUtilizadores(tabela, null, null);
        });
        tfSearch.setOnAction(e -> {
            String tipo = "Todos".equals(cbTipo.getValue()) ? null : cbTipo.getValue();
            carregarUtilizadores(tabela, tfSearch.getText().trim(), tipo);
        });
        cbTipo.setOnAction(e -> {
            String tipo = "Todos".equals(cbTipo.getValue()) ? null : cbTipo.getValue();
            carregarUtilizadores(tabela, tfSearch.getText().trim(), tipo);
        });

        // ---- Duplo-clique numa linha → ver ficha completa do utilizador ----
        tabela.setRowFactory(tv -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    mostrarDetalhesUtilizador(row.getItem());
                }
            });
            return row;
        });

        Label lblDica = new Label("💡 Duplo-clique numa linha (ou \"Ver Detalhes\") para consultar a ficha completa.");
        lblDica.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        // ---- Painel de ações ----
        Label lblAcoes = label("Ações sobre utilizador selecionado", 15, true, "#2563EB");

        Button btnDetalhes    = botao("🔍 Ver Detalhes",      "#2563EB", "white");
        Button btnTipo        = botao("🛠 Alterar Tipo",      "#6a1b9a", "white");
        Button btnBloquear    = botao("🔒 Bloquear",          "#EF4444", "white");
        Button btnDesbloquear = botao("🔓 Desbloquear",       "#22C55E", "white");
        Button btnAviso       = botao("⚠ Emitir Aviso",      "#B45309", "white");
        Button btnVerAvisos   = botao("📋 Ver Avisos",        "#1F2937", "white");

        HBox acoes = new HBox(10, btnDetalhes, btnTipo, btnBloquear, btnDesbloquear, btnAviso, btnVerAvisos);
        acoes.setAlignment(Pos.CENTER_LEFT);

        Label lblFeedback = new Label();
        lblFeedback.setFont(Font.font(13));

        btnDetalhes.setOnAction(e -> {
            User sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { aviso("Selecione um utilizador.", lblFeedback); return; }
            mostrarDetalhesUtilizador(sel);
        });

        btnTipo.setOnAction(e -> {
            User sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { aviso("Selecione um utilizador.", lblFeedback); return; }

            String tipoAtual = sel.getTipo() == null ? "locatario" : sel.getTipo();
            ChoiceDialog<String> dialog = new ChoiceDialog<>(
                tipoAtual, "proprietario", "locatario", "admin");
            dialog.setTitle("Alterar Tipo de Utilizador");
            dialog.setHeaderText("Selecione o novo tipo para " + sel.getNome() + ":");
            dialog.setContentText("Tipo:");

            dialog.showAndWait().ifPresent(novoTipo -> {
                if (novoTipo.equals(tipoAtual)) {
                    aviso("O tipo selecionado é igual ao atual — nada foi alterado.", lblFeedback);
                    return;
                }

                String mensagemConfirmacao = "admin".equals(novoTipo)
                    ? "Tornar " + sel.getNome() + " ADMINISTRADOR? O perfil será definido como ADMINISTRADOR — passará a ter acesso ao Painel de Administração e perderá o acesso ao mercado (alugar/anunciar veículos)."
                    : "Alterar o tipo de " + sel.getNome() + " para '" + novoTipo + "'? O perfil será definido como UTILIZADOR.";

                confirmar(mensagemConfirmacao, () -> {
                    try {
                        dao.atualizarTipoUtilizador(sel.getId(), novoTipo);
                        sucesso("Tipo atualizado para '" + novoTipo + "'"
                            + ("admin".equals(novoTipo) ? " — perfil definido como ADMINISTRADOR." : " — perfil definido como UTILIZADOR.")
                            , lblFeedback);
                        String tipoFiltro = "Todos".equals(cbTipo.getValue()) ? null : cbTipo.getValue();
                        carregarUtilizadores(tabela, tfSearch.getText().trim(), tipoFiltro);
                    } catch (SQLException ex) { erro(ex, lblFeedback); }
                });
            });
        });

        btnBloquear.setOnAction(e -> {
            User sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { aviso("Selecione um utilizador.", lblFeedback); return; }
            confirmar("Bloquear conta de " + sel.getNome() + "?", () -> {
                try {
                    dao.setAtivo(sel.getId(), false);
                    sucesso("Conta bloqueada.", lblFeedback);
                    String tipoFiltro = "Todos".equals(cbTipo.getValue()) ? null : cbTipo.getValue();
                    carregarUtilizadores(tabela, tfSearch.getText().trim(), tipoFiltro);
                } catch (SQLException ex) { erro(ex, lblFeedback); }
            });
        });

        btnDesbloquear.setOnAction(e -> {
            User sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { aviso("Selecione um utilizador.", lblFeedback); return; }
            confirmar("Desbloquear conta de " + sel.getNome() + "?", () -> {
                try {
                    dao.setAtivo(sel.getId(), true);
                    sucesso("Conta desbloqueada.", lblFeedback);
                    String tipoFiltro = "Todos".equals(cbTipo.getValue()) ? null : cbTipo.getValue();
                    carregarUtilizadores(tabela, tfSearch.getText().trim(), tipoFiltro);
                } catch (SQLException ex) { erro(ex, lblFeedback); }
            });
        });

        btnAviso.setOnAction(e -> {
            User sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { aviso("Selecione um utilizador.", lblFeedback); return; }
            dialogTexto("Motivo do aviso a " + sel.getNome(), motivo -> {
                try {
                    int totalAvisos = dao.contarAvisos(sel.getId()) + 1;
                    boolean banido  = dao.emitirAviso(sel.getId(), motivo);
                    if (banido) {
                        sucesso("⚠️ 3.º aviso — utilizador BANIDO automaticamente.", lblFeedback);
                    } else {
                        sucesso("Aviso " + totalAvisos + "/3 emitido com sucesso.", lblFeedback);
                    }
                    String tipoFiltro = "Todos".equals(cbTipo.getValue()) ? null : cbTipo.getValue();
                    carregarUtilizadores(tabela, tfSearch.getText().trim(), tipoFiltro);
                } catch (SQLException ex) { erro(ex, lblFeedback); }
            });
        });

        btnVerAvisos.setOnAction(e -> {
            User sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { aviso("Selecione um utilizador.", lblFeedback); return; }
            try {
                int total = dao.contarAvisos(sel.getId());
                List<String[]> avisos = dao.listarAvisos(sel.getId());
                StringBuilder sb = new StringBuilder();
                sb.append("Utilizador: ").append(sel.getNome())
                  .append("\nTotal de avisos: ").append(total).append("/3\n\n");
                if (avisos.isEmpty()) {
                    sb.append("Sem avisos registados.");
                } else {
                    for (int i = 0; i < avisos.size(); i++) {
                        sb.append("Aviso ").append(i + 1).append(":\n");
                        sb.append("  Data: ").append(avisos.get(i)[1]).append("\n");
                        sb.append("  Motivo: ").append(avisos.get(i)[0]).append("\n\n");
                    }
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Histórico de Avisos");
                alert.setHeaderText(null);
                TextArea ta = new TextArea(sb.toString());
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.setPrefSize(420, 260);
                alert.getDialogPane().setContent(ta);
                alert.showAndWait();
            } catch (SQLException ex) { erro(ex, lblFeedback); }
        });

        pane.getChildren().addAll(
            label("Gestão de Utilizadores", 18, true, "#2563EB"),
            barSearch, tabela, lblDica,
            lblAcoes, acoes, lblFeedback
        );

        ScrollPane sp = new ScrollPane(pane);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #F8FAFC; -fx-background-color: #F8FAFC;");
        return sp;
    }

    private void carregarUtilizadores(TableView<User> tabela, String termo, String tipo) {
        try {
            List<User> lista = (termo == null || termo.isEmpty())
                ? dao.listarUtilizadores(adminId)
                : dao.pesquisarUtilizadores(termo, adminId);
            // filtro de tipo local (simples, sem nova query)
            if (tipo != null && !tipo.isBlank()) {
                lista = lista.stream()
                    .filter(u -> tipo.equalsIgnoreCase(u.getTipo()))
                    .collect(java.util.stream.Collectors.toList());
            }
            tabela.setItems(FXCollections.observableArrayList(lista));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // 2. VEÍCULOS
    // =========================================================================
    private ScrollPane buildVeiculosPane() {
        VBox pane = new VBox(18);
        pane.setPadding(new Insets(24));
        pane.setStyle("-fx-background-color: #F8FAFC;");

        TextField tfSearch = new TextField();
        tfSearch.setPromptText("Pesquisar por marca, modelo, localização ou proprietário…");
        tfSearch.setPrefWidth(400);
        tfSearch.setStyle(campoCss());

        Button btnSearch = botao("Pesquisar", "#2563EB", "white");
        Button btnTodos  = botao("Ver Todos", "#64748B", "white");
        HBox barSearch   = new HBox(10, tfSearch, btnSearch, btnTodos);
        barSearch.setAlignment(Pos.CENTER_LEFT);

        // Colunas: ID, Marca, Modelo, Ano, Localização, Estado, Proprietário, Preço/dia
        TableView<ObservableList<String>> tabela = new TableView<>();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabela.setPrefHeight(330);

        String[] colunas = {"ID", "Marca", "Modelo", "Ano", "Localização", "Estado", "Proprietário", "€/dia"};
        int[]    larguras = {50, 100, 120, 60, 130, 100, 150, 70};
        for (int i = 0; i < colunas.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> tc = new TableColumn<>(colunas[i]);
            tc.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(col)));
            tc.setPrefWidth(larguras[i]);
            tabela.getColumns().add(tc);
        }

        carregarVeiculos(tabela, null);
        btnSearch.setOnAction(e -> carregarVeiculos(tabela, tfSearch.getText().trim()));
        btnTodos .setOnAction(e -> { tfSearch.clear(); carregarVeiculos(tabela, null); });
        tfSearch.setOnAction(e -> carregarVeiculos(tabela, tfSearch.getText().trim()));

        // ---- Duplo-clique numa linha → ver detalhe completo (apenas leitura) ----
        tabela.setRowFactory(tv -> {
            TableRow<ObservableList<String>> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    mostrarDetalhesVeiculo(Integer.parseInt(row.getItem().get(0)));
                }
            });
            return row;
        });

        Label lblDica = new Label("💡 Duplo-clique numa linha (ou \"Ver Detalhes\") para analisar o veículo — apenas consulta, sem edição.");
        lblDica.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        Label lblAcoes    = label("Ações sobre veículo selecionado", 15, true, "#2563EB");
        Button btnDetalhes = botao("🔍 Ver Detalhes", "#2563EB", "white");
        Button btnRemove   = botao("🗑 Remover Veículo (violação de regras)", "#EF4444", "white");
        Label  lblFb       = new Label();
        lblFb.setFont(Font.font(13));

        btnDetalhes.setOnAction(e -> {
            ObservableList<String> sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { aviso("Selecione um veículo.", lblFb); return; }
            mostrarDetalhesVeiculo(Integer.parseInt(sel.get(0)));
        });

        btnRemove.setOnAction(e -> {
            ObservableList<String> sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) { aviso("Selecione um veículo.", lblFb); return; }
            int veiculoId = Integer.parseInt(sel.get(0));
            String desc   = sel.get(1) + " " + sel.get(2) + " (prop: " + sel.get(6) + ")";
            confirmar("Remover o veículo " + desc + " por violação de regras?", () -> {
                try {
                    dao.removerVeiculo(veiculoId);
                    sucesso("Veículo removido com sucesso.", lblFb);
                    carregarVeiculos(tabela, null);
                } catch (SQLException ex) { erro(ex, lblFb); }
            });
        });

        HBox acoesVeiculo = new HBox(10, btnDetalhes, btnRemove);
        acoesVeiculo.setAlignment(Pos.CENTER_LEFT);

        pane.getChildren().addAll(
            label("Gestão de Veículos", 18, true, "#2563EB"),
            barSearch, tabela, lblDica,
            lblAcoes, acoesVeiculo, lblFb
        );

        ScrollPane sp = new ScrollPane(pane);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #F8FAFC; -fx-background-color: #F8FAFC;");
        return sp;
    }

    private void carregarVeiculos(TableView<ObservableList<String>> tabela, String termo) {
        try {
            List<Object[]> lista = (termo == null || termo.isEmpty())
                ? dao.listarVeiculosComProprietario()
                : dao.pesquisarVeiculos(termo);
            ObservableList<ObservableList<String>> dados = FXCollections.observableArrayList();
            for (Object[] row : lista) {
                ObservableList<String> r = FXCollections.observableArrayList(
                    String.valueOf(row[0]),                        // ID
                    String.valueOf(row[1]),                        // marca
                    String.valueOf(row[2]),                        // modelo
                    String.valueOf(row[3]),                        // ano
                    String.valueOf(row[6]),                        // localização
                    String.valueOf(row[7]),                        // estado
                    String.valueOf(row[8]),                        // proprietário
                    String.format("%.2f €", (Double) row[5])      // preço/dia
                );
                dados.add(r);
            }
            tabela.setItems(dados);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // FICHAS DE DETALHE (apenas consulta — sem edição)
    // =========================================================================

    /** Mostra a ficha completa de um utilizador, num diálogo só de leitura. */
    private void mostrarDetalhesUtilizador(User u) {
        int totalAvisos;
        try {
            totalAvisos = dao.contarAvisos(u.getId());
        } catch (SQLException ex) {
            totalAvisos = -1;
        }

        DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DateTimeFormatter fmtDia  = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        GridPane grid = criarGridDetalhe();
        int row = 0;
        row = addLinhaDetalhe(grid, row, "Nome",           u.getNome());
        row = addLinhaDetalhe(grid, row, "Email",          u.getEmail());
        row = addLinhaDetalhe(grid, row, "NIF",            u.getNif());
        row = addLinhaDetalhe(grid, row, "Número de Carta", u.getNumeroCarta());
        row = addLinhaDetalhe(grid, row, "Validade da Carta",
            u.getValidadeCarta() != null
                ? u.getValidadeCarta().format(fmtDia) + (u.isCartaValida() ? "" : "   ⚠️ Expirada")
                : "—");
        row = addLinhaDetalhe(grid, row, "Saldo",          String.format("%.2f €", u.getSaldo()));
        row = addLinhaDetalhe(grid, row, "Perfil",         u.getPerfil());
        row = addLinhaDetalhe(grid, row, "Tipo",           u.getTipo() == null ? "—" : u.getTipo());
        row = addLinhaDetalhe(grid, row, "Estado da Conta", u.isAtivo() ? "✅ Ativa" : "🔒 Bloqueada");
        row = addLinhaDetalhe(grid, row, "Registado em",
            u.getDataCriacao() != null ? u.getDataCriacao().format(fmtData) : "—");
        row = addLinhaDetalhe(grid, row, "Total de Avisos",
            totalAvisos >= 0 ? totalAvisos + " / 3" : "—");

        mostrarDialogoDetalhe("Ficha do Utilizador — " + u.getNome(), grid);
    }

    /**
     * Mostra a ficha completa de um veículo, num diálogo só de leitura — o Admin
     * usa esta informação apenas para avaliar conformidade, nunca para editar.
     */
    private void mostrarDetalhesVeiculo(int veiculoId) {
        Object[] d;
        try {
            d = dao.obterDetalheVeiculo(veiculoId);
        } catch (SQLException ex) {
            Alert err = new Alert(Alert.AlertType.ERROR, "Erro ao carregar o veículo: " + ex.getMessage());
            err.showAndWait();
            return;
        }
        if (d == null) return;

        int proprietarioId = (int) d[12];
        double mediaProprietario;
        try {
            mediaProprietario = new AvaliacaoDAO().mediaPorProprietario(proprietarioId);
        } catch (SQLException ex) {
            mediaProprietario = -1;
        }

        GridPane grid = criarGridDetalhe();
        int row = 0;
        row = addLinhaDetalhe(grid, row, "Marca / Modelo", d[1] + " " + d[2]);
        row = addLinhaDetalhe(grid, row, "Ano",             String.valueOf(d[3]));
        row = addLinhaDetalhe(grid, row, "Combustível",     String.valueOf(d[4]));
        row = addLinhaDetalhe(grid, row, "Preço/dia",       String.format("%.2f €", (double) d[5]));
        row = addLinhaDetalhe(grid, row, "Localização",     String.valueOf(d[6]));
        row = addLinhaDetalhe(grid, row, "Estado",          String.valueOf(d[7]));
        row = addLinhaDetalhe(grid, row, "Matrícula",       d[8] == null ? "—" : String.valueOf(d[8]));
        row = addLinhaDetalhe(grid, row, "Quilometragem",   d[9] + " km");
        row = addLinhaDetalhe(grid, row, "Proprietário",    d[10] + "  (" + d[11] + ")");
        row = addLinhaDetalhe(grid, row, "Avaliação média do proprietário",
            mediaProprietario >= 0 ? String.format("%.1f ★", mediaProprietario) : "Sem avaliações");
        row = addLinhaDetalhe(grid, row, "Total de Reservas", String.valueOf(d[13]));
        row = addLinhaDetalhe(grid, row, "Receita Total Gerada", String.format("%.2f €", (double) d[14]));

        mostrarDialogoDetalhe("Ficha do Veículo — " + d[1] + " " + d[2] + " (apenas consulta)", grid);
    }

    private GridPane criarGridDetalhe() {
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(10);
        grid.setPadding(new Insets(12, 6, 6, 6));
        return grid;
    }

    private int addLinhaDetalhe(GridPane grid, int row, String chave, String valor) {
        Label lblChave = new Label(chave);
        lblChave.setStyle("-fx-font-weight: bold; -fx-text-fill: #1F2937; -fx-font-size: 13px;");
        lblChave.setMinWidth(170);

        Label lblValor = new Label(valor == null || valor.isBlank() ? "—" : valor);
        lblValor.setStyle("-fx-text-fill: #1F2937; -fx-font-size: 13px;");
        lblValor.setWrapText(true);
        lblValor.setMaxWidth(320);

        grid.add(lblChave, 0, row);
        grid.add(lblValor, 1, row);
        return row + 1;
    }

    private void mostrarDialogoDetalhe(String titulo, GridPane grid) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 16;");
        dialog.showAndWait();
    }

    // =========================================================================
    // 3. ESTATÍSTICAS
    // =========================================================================
    private ScrollPane buildEstatisticasPane() {
        VBox pane = new VBox(22);
        pane.setPadding(new Insets(24));
        pane.setStyle("-fx-background-color: #F8FAFC;");

        // ---- Cards gerais ----
        Label lblGerais = label("Resumo Geral", 17, true, "#2563EB");
        HBox cardsBox   = criarCardsGerais();

        // ---- Filtro por período ----
        Label lblPeriodo = label("Análise por Período", 16, true, "#1F2937");

        ToggleGroup tg = new ToggleGroup();
        RadioButton rbDia  = new RadioButton("Dia");   rbDia .setToggleGroup(tg); rbDia .setSelected(true);
        RadioButton rbMes  = new RadioButton("Mês");   rbMes .setToggleGroup(tg);
        RadioButton rbAno  = new RadioButton("Ano");   rbAno .setToggleGroup(tg);
        HBox rbBox = new HBox(16, rbDia, rbMes, rbAno);
        rbBox.setAlignment(Pos.CENTER_LEFT);

        TableView<ObservableList<String>> tblPeriodo = criarTabelaStats(
            new String[]{"Período", "Reservas", "Receita (€)"});

        Button btnPeriodo = botao("Atualizar", "#2563EB", "white");
        btnPeriodo.setOnAction(e -> {
            String agrup = rbDia.isSelected() ? "DAY" : rbMes.isSelected() ? "MONTH" : "YEAR";
            try {
                preencherTabelaStats(tblPeriodo, dao.estatisticasPorPeriodo(agrup));
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
        // Carregar ao abrir
        try { preencherTabelaStats(tblPeriodo, dao.estatisticasPorPeriodo("MONTH")); }
        catch (SQLException ignored) {}

        // ---- Filtro por intervalo de datas (aplica-se a Marca + Região) ----
        Label lblFiltroData = label("Filtrar Marca / Região por Período", 16, true, "#1F2937");

        DatePicker dpInicio = new DatePicker();
        dpInicio.setPromptText("Data início");
        dpInicio.setPrefWidth(150);

        DatePicker dpFim = new DatePicker();
        dpFim.setPromptText("Data fim");
        dpFim.setPrefWidth(150);

        Button btnFiltrarData = botao("Filtrar Período", "#2563EB", "white");
        Button btnLimparData  = botao("Limpar Filtro",   "#64748B", "white");

        Label lblFiltroFeedback = new Label();
        lblFiltroFeedback.setFont(Font.font(13));

        HBox filtroDataBox = new HBox(10,
            new Label("De:"),  dpInicio,
            new Label("Até:"), dpFim,
            btnFiltrarData, btnLimparData
        );
        filtroDataBox.setAlignment(Pos.CENTER_LEFT);

        // ---- Por marca ----
        Label lblMarca = label("Análise por Marca", 16, true, "#1F2937");
        ComboBox<String> cbMarca = new ComboBox<>();
        cbMarca.getItems().add("Todas as Marcas");
        try { dao.listarMarcas().forEach(m -> cbMarca.getItems().add(m)); } catch (SQLException ignored) {}
        cbMarca.setValue("Todas as Marcas");
        cbMarca.setStyle(campoCss() + " -fx-pref-width: 200px;");
        Button btnLimparMarca = botao("✕ Limpar", "#64748B", "white");
        HBox barMarca = new HBox(10, new Label("Filtrar por marca:"), cbMarca, btnLimparMarca);
        barMarca.setAlignment(Pos.CENTER_LEFT);

        VBox grafMarcaBox = new VBox(); // contentor que vai ter o gráfico (barra ou linha)
        BarChart<String, Number> grafMarcaBarra = criarGraficoBarras("Receita (€)");
        grafMarcaBox.getChildren().add(grafMarcaBarra);

        TableView<ObservableList<String>> tblMarca = criarTabelaStats(
            new String[]{"Marca", "Reservas", "Receita (€)"});
        tblMarca.setPrefHeight(280);

        // ---- Por região ----
        Label lblRegiao = label("Análise por Região", 16, true, "#1F2937");
        ComboBox<String> cbRegiao = new ComboBox<>();
        cbRegiao.getItems().add("Todas as Regiões");
        try { dao.listarRegioes().forEach(r -> cbRegiao.getItems().add(r)); } catch (SQLException ignored) {}
        cbRegiao.setValue("Todas as Regiões");
        cbRegiao.setStyle(campoCss() + " -fx-pref-width: 200px;");
        Button btnLimparRegiao = botao("✕ Limpar", "#64748B", "white");
        HBox barRegiao = new HBox(10, new Label("Filtrar por região:"), cbRegiao, btnLimparRegiao);
        barRegiao.setAlignment(Pos.CENTER_LEFT);

        VBox grafRegiaoBox = new VBox(); // contentor que vai ter o gráfico (barra ou linha)
        BarChart<String, Number> grafRegiaoBarra = criarGraficoBarras("Receita (€)");
        grafRegiaoBox.getChildren().add(grafRegiaoBarra);

        TableView<ObservableList<String>> tblRegiao = criarTabelaStats(
            new String[]{"Região", "Reservas", "Receita (€)"});
        tblRegiao.setPrefHeight(280);

        // Carrega marca + região.
        // "Todas" → BarChart comparativo; específica → LineChart de evolução mensal.
        Runnable carregarMarcaRegiao = () -> {
            LocalDate inicio = dpInicio.getValue();
            LocalDate fim    = dpFim.getValue();

            if ((inicio != null) != (fim != null)) {
                aviso("Selecione as duas datas (início e fim) para filtrar, ou deixe ambas vazias.", lblFiltroFeedback);
                return;
            }
            if (inicio != null && fim != null && inicio.isAfter(fim)) {
                aviso("A data de início não pode ser posterior à data de fim.", lblFiltroFeedback);
                return;
            }

            String marcaSel  = "Todas as Marcas".equals(cbMarca.getValue())  ? null : cbMarca.getValue();
            String regiaoSel = "Todas as Regiões".equals(cbRegiao.getValue()) ? null : cbRegiao.getValue();

            try {
                // ---- MARCA ----
                if (marcaSel != null) {
                    lblMarca.setText("Faturamento de " + marcaSel + " por Mês");
                    List<Object[]> dados = dao.faturamentoPorDataMarca(marcaSel, inicio, fim);
                    atualizarColunaTabela(tblMarca, new String[]{"Mês", "Reservas", "Receita (€)"});
                    preencherTabelaStats(tblMarca, dados);
                    LineChart<String, Number> lineMarca = criarGraficoLinha("Receita (€)");
                    preencherGraficoLinha(lineMarca, marcaSel, dados);
                    grafMarcaBox.getChildren().setAll(lineMarca);
                } else {
                    lblMarca.setText("Análise por Marca");
                    List<Object[]> dados = dao.estatisticasPorMarca(inicio, fim, null);
                    atualizarColunaTabela(tblMarca, new String[]{"Marca", "Reservas", "Receita (€)"});
                    preencherTabelaStats(tblMarca, dados);
                    BarChart<String, Number> barMarcaNovo = criarGraficoBarras("Receita (€)");
                    preencherGrafico(barMarcaNovo, dados);
                    grafMarcaBox.getChildren().setAll(barMarcaNovo);
                }

                // ---- REGIÃO ----
                if (regiaoSel != null) {
                    lblRegiao.setText("Faturamento de " + regiaoSel + " por Mês");
                    List<Object[]> dados = dao.faturamentoPorDataRegiao(regiaoSel, inicio, fim);
                    atualizarColunaTabela(tblRegiao, new String[]{"Mês", "Reservas", "Receita (€)"});
                    preencherTabelaStats(tblRegiao, dados);
                    LineChart<String, Number> lineRegiao = criarGraficoLinha("Receita (€)");
                    preencherGraficoLinha(lineRegiao, regiaoSel, dados);
                    grafRegiaoBox.getChildren().setAll(lineRegiao);
                } else {
                    lblRegiao.setText("Análise por Região");
                    List<Object[]> dados = dao.estatisticasPorRegiao(inicio, fim, null);
                    atualizarColunaTabela(tblRegiao, new String[]{"Região", "Reservas", "Receita (€)"});
                    preencherTabelaStats(tblRegiao, dados);
                    BarChart<String, Number> barRegiaoNovo = criarGraficoBarras("Receita (€)");
                    preencherGrafico(barRegiaoNovo, dados);
                    grafRegiaoBox.getChildren().setAll(barRegiaoNovo);
                }

                if (inicio != null) {
                    sucesso("Período filtrado: " + inicio + " a " + fim + ".", lblFiltroFeedback);
                } else {
                    lblFiltroFeedback.setText("");
                }
            } catch (SQLException ex) { erro(ex, lblFiltroFeedback); }
        };

        cbMarca.setOnAction(e -> carregarMarcaRegiao.run());
        cbRegiao.setOnAction(e -> carregarMarcaRegiao.run());
        btnLimparMarca.setOnAction(e -> { cbMarca.setValue("Todas as Marcas");   carregarMarcaRegiao.run(); });
        btnLimparRegiao.setOnAction(e -> { cbRegiao.setValue("Todas as Regiões"); carregarMarcaRegiao.run(); });

        btnFiltrarData.setOnAction(e -> carregarMarcaRegiao.run());
        btnLimparData.setOnAction(e -> {
            dpInicio.setValue(null);
            dpFim.setValue(null);
            carregarMarcaRegiao.run();
        });

        // Carregar ao abrir (sem filtro)
        carregarMarcaRegiao.run();

        Separator sep1 = new Separator(); Separator sep2 = new Separator(); Separator sep3 = new Separator();

        pane.getChildren().addAll(
            label("Estatísticas", 18, true, "#2563EB"),
            sep1, lblGerais, cardsBox,
            sep2, lblPeriodo, rbBox, btnPeriodo, tblPeriodo,
            sep3,
            lblFiltroData, filtroDataBox, lblFiltroFeedback,
            new HBox(30,
                new VBox(8, lblMarca,  barMarca,  grafMarcaBox,  tblMarca),
                new VBox(8, lblRegiao, barRegiao, grafRegiaoBox, tblRegiao)
            )
        );

        ScrollPane sp = new ScrollPane(pane);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #F8FAFC; -fx-background-color: #F8FAFC;");
        return sp;
    }

    private HBox criarCardsGerais() {
        HBox box = new HBox(16);
        box.setAlignment(Pos.CENTER_LEFT);
        try {
            int[] s = dao.estatisticasGerais();
            box.getChildren().addAll(
                card("👥 Utilizadores",   String.valueOf(s[0]), "#EAF2FF", "#2563EB"),
                card("✅ Contas Ativas",  String.valueOf(s[1]), "#DCFCE7", "#22C55E"),
                card("🔒 Bloqueados",     String.valueOf(s[2]), "#FEE2E2", "#EF4444"),
                card("🚗 Veículos",       String.valueOf(s[3]), "#EAF2FF", "#2563EB"),
                card("📋 Reservas",       String.valueOf(s[4]), "#FEF3C7", "#B45309"),
                card("💰 Receita Total",  s[5] + " €",          "#f3e5f5", "#6a1b9a")
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return box;
    }

    private VBox card(String titulo, String valor, String bg, String cor) {
        Label lv = new Label(valor);
        lv.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + cor + ";");

        Label lt = new Label(titulo);
        lt.setStyle("-fx-font-size: 12px; -fx-text-fill: #1F2937;");

        VBox c = new VBox(4, lv, lt);
        c.setAlignment(Pos.CENTER_LEFT);
        c.setPadding(new Insets(16, 22, 16, 22));
        c.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10;" +
                   "-fx-border-color: #E2E8F0; -fx-border-radius: 10; -fx-border-width: 1;" +
                   "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
        c.setPrefWidth(148);
        return c;
    }

    /**
     * Cria um gráfico de barras vazio para mostrar receita por categoria
     * (marca ou região). Altura aumentada para facilitar a leitura.
     */
    private BarChart<String, Number> criarGraficoBarras(String labelEixoY) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel(labelEixoY);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        chart.setCategoryGap(12);
        return chart;
    }

    /** Preenche o gráfico de barras com a receita (índice 2 do Object[]) de cada linha. */
    private void preencherGrafico(BarChart<String, Number> chart, List<Object[]> dados) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (Object[] row : dados) {
            serie.getData().add(new XYChart.Data<>(String.valueOf(row[0]), (double) row[2]));
        }
        chart.getData().clear();
        chart.getData().add(serie);
    }

    /** Cria um LineChart vazio para evolução temporal de uma marca ou região. */
    private LineChart<String, Number> criarGraficoLinha(String labelEixoY) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel(labelEixoY);
        xAxis.setLabel("Mês");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        chart.setCreateSymbols(true);
        return chart;
    }

    /** Preenche o LineChart com dados de evolução temporal (índice 0=período, 2=receita). */
    private void preencherGraficoLinha(LineChart<String, Number> chart, String nomeSerie, List<Object[]> dados) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(nomeSerie);
        for (Object[] row : dados) {
            serie.getData().add(new XYChart.Data<>(String.valueOf(row[0]), (double) row[2]));
        }
        chart.getData().clear();
        chart.getData().add(serie);
    }

    /** Substitui os cabeçalhos das colunas de uma tabela de estatísticas sem recriar o objecto. */
    private void atualizarColunaTabela(TableView<ObservableList<String>> tabela, String[] novosCabecalhos) {
        for (int i = 0; i < Math.min(tabela.getColumns().size(), novosCabecalhos.length); i++) {
            tabela.getColumns().get(i).setText(novosCabecalhos[i]);
        }
    }

    private TableView<ObservableList<String>> criarTabelaStats(String[] colunas) {
        TableView<ObservableList<String>> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPrefHeight(220);
        for (int i = 0; i < colunas.length; i++) {
            final int col = i;
            TableColumn<ObservableList<String>, String> tc = new TableColumn<>(colunas[i]);
            tc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().get(col)));
            t.getColumns().add(tc);
        }
        return t;
    }

    private void preencherTabelaStats(TableView<ObservableList<String>> t, List<Object[]> dados) {
        ObservableList<ObservableList<String>> obs = FXCollections.observableArrayList();
        for (Object[] row : dados) {
            ObservableList<String> r = FXCollections.observableArrayList(
                String.valueOf(row[0]),
                String.valueOf(row[1]),
                String.format("%.2f", (double) row[2])
            );
            obs.add(r);
        }
        t.setItems(obs);
    }

    // =========================================================================
    // UTILITÁRIOS DE UI
    // =========================================================================
    @SuppressWarnings("unchecked")
    private <T> TableColumn<T, String> colStr(String titulo, java.util.function.Function<T, String> fn, int largura) {
        TableColumn<T, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(data -> new SimpleStringProperty(fn.apply(data.getValue())));
        col.setPrefWidth(largura);
        return col;
    }

    private Label label(String txt, int tamanho, boolean bold, String cor) {
        Label l = new Label(txt);
        l.setStyle(
            "-fx-font-size: " + tamanho + "px;" +
            (bold ? " -fx-font-weight: bold;" : "") +
            " -fx-text-fill: " + cor + ";"
        );
        return l;
    }

    private Button botao(String txt, String bg, String fg) {
        Button b = new Button(txt);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                   "; -fx-background-radius: 6; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        return b;
    }

    private String campoCss() {
        return "-fx-font-size: 13px; -fx-padding: 8; -fx-border-color: #ccc;" +
               "-fx-border-radius: 5; -fx-background-radius: 5;";
    }

    private void confirmar(String mensagem, Runnable acao) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, mensagem, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(bt -> { if (bt == ButtonType.YES) acao.run(); });
    }

    private void dialogTexto(String prompt, java.util.function.Consumer<String> callback) {
        TextInputDialog td = new TextInputDialog();
        td.setTitle(prompt);
        td.setHeaderText(null);
        td.setContentText("Motivo:");
        td.getEditor().setStyle(campoCss());
        td.showAndWait().ifPresent(texto -> {
            if (!texto.isBlank()) callback.accept(texto.trim());
        });
    }

    private void sucesso(String msg, Label lbl) {
        lbl.setText("✅ " + msg);
        lbl.setStyle("-fx-text-fill: #22C55E;");
    }

    private void aviso(String msg, Label lbl) {
        lbl.setText("⚠️ " + msg);
        lbl.setStyle("-fx-text-fill: #B45309;");
    }

    private void erro(Exception e, Label lbl) {
        lbl.setText("❌ Erro: " + e.getMessage());
        lbl.setStyle("-fx-text-fill: #EF4444;");
    }
}