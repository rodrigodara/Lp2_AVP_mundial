package pt.plataformaaluguerveiculos.views;

import java.util.List;

import com.aluguer.service.NotificacaoService;
import com.aluguer.service.NotificacaoService.Notificacao;
import com.aluguer.util.SessionManager;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class SinhoNotificacoesView {

    private final StackPane sino;
    private final Label badge;
    private final Popup popup;
    private final VBox listaPopup;
    private Timeline autoRefresh;

    public SinhoNotificacoesView() {

        Label icone = new Label("\uD83D\uDD14");
        icone.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-text-fill: white;");

        badge = new Label("0");
        badge.setStyle(
            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-background-color: #e53935; -fx-background-radius: 10;" +
            "-fx-padding: 1 5 1 5; -fx-min-width: 18; -fx-alignment: center;"
        );
        badge.setVisible(false);

        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-6, -6, 0, 0));

        sino = new StackPane(icone, badge);
        sino.setMaxSize(34, 34);
        sino.setStyle("-fx-cursor: hand;");

        listaPopup = new VBox(0);
        listaPopup.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-border-color: #d0d0d0;" +
            "-fx-border-radius: 10;" +
            "-fx-background-radius: 10;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 18, 0, 0, 6);"
        );
        listaPopup.setPrefWidth(360);
        listaPopup.setMaxHeight(460);

        popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(listaPopup);

        sino.setOnMouseClicked(e -> {
            if (popup.isShowing()) { popup.hide(); return; }
            atualizarPopup();
            Point2D pt = sino.localToScreen(0, 0);
            if (pt != null) {
                popup.show(sino, pt.getX() - 300, pt.getY() + sino.getHeight() + 6);
            }
        });

        autoRefresh = new Timeline(
            new KeyFrame(Duration.seconds(30), e -> atualizarBadge())
        );
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();

        atualizarBadge();
    }

    // ══════════════════════════════════════
    // BADGE
    // ══════════════════════════════════════

    public void atualizarBadge() {
        var user = SessionManager.getInstance().getUtilizador();
        if (user == null) { badge.setVisible(false); return; }

        int naoLidas = NotificacaoService.getInstance().contarNaoLidas(user.getId());
        if (naoLidas > 0) {
            badge.setText(naoLidas > 9 ? "9+" : String.valueOf(naoLidas));
            badge.setVisible(true);
        } else {
            badge.setVisible(false);
        }
    }

    // ══════════════════════════════════════
    // POPUP PRINCIPAL
    // ══════════════════════════════════════

    private void atualizarPopup() {
        listaPopup.getChildren().clear();

        var user = SessionManager.getInstance().getUtilizador();
        if (user == null) return;

        // cabeçalho
        Label titulo = new Label("Notificações");
        titulo.setStyle(
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;"
        );

        Button btnMarcar = new Button("Marcar como lidas");
        btnMarcar.setStyle(
            "-fx-font-size: 11px; -fx-background-color: transparent;" +
            "-fx-text-fill: #546e7a; -fx-cursor: hand; -fx-underline: true; -fx-padding: 0;"
        );
        btnMarcar.setOnAction(e -> { marcarTodasComoLidas(); atualizarPopup(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(titulo, spacer, btnMarcar);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 12, 16));
        header.setStyle(
            "-fx-border-color: transparent transparent #e8e8e8 transparent;" +
            "-fx-border-width: 0 0 1 0;"
        );
        listaPopup.getChildren().add(header);

        List<Notificacao> lista = NotificacaoService.getInstance().listar(user.getId());

        if (lista.isEmpty()) {
            VBox vazio = new VBox(8);
            vazio.setAlignment(Pos.CENTER);
            vazio.setPadding(new Insets(36));

            // sem emojis — usa texto simples
            Label iconVazio = new Label("( sem notificacoes )");
            iconVazio.setStyle("-fx-font-size: 12px; -fx-text-fill: #bdbdbd;");

            vazio.getChildren().add(iconVazio);
            listaPopup.getChildren().add(vazio);
            return;
        }

        VBox itens = new VBox(0);
        for (Notificacao n : lista) {
            itens.getChildren().add(criarItem(n));
            Separator sep = new Separator();
            sep.setStyle("-fx-background-color: #f0f0f0;");
            itens.getChildren().add(sep);
        }

        ScrollPane scroll = new ScrollPane(itens);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(340);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        listaPopup.getChildren().add(scroll);
    }

    private HBox criarItem(Notificacao n) {

        // barra colorida lateral
        Region barra = new Region();
        barra.setMinWidth(4);
        barra.setMaxWidth(4);
        barra.setStyle("-fx-background-color: " + corPorTipo(n.tipo) + "; -fx-background-radius: 2;");

        // ícone em texto simples (sem emoji)
        Label icone = new Label(iconePorTipo(n.tipo));
        icone.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-background-color: " + corPorTipo(n.tipo) + ";" +
            "-fx-background-radius: 4;" +
            "-fx-padding: 3 6 3 6;" +
            "-fx-min-width: 44;" +
            "-fx-alignment: center;"
        );

        // texto resumido
        Label msg = new Label(textoResumido(n));
        msg.setWrapText(true);
        msg.setMaxWidth(220);
        msg.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: " + (n.lida ? "#9e9e9e" : "#212121") + ";" +
            (n.lida ? "" : "-fx-font-weight: bold;")
        );

        Label data = new Label(n.dataCriacao != null && n.dataCriacao.length() > 16
                ? n.dataCriacao.substring(0, 16) : "");
        data.setStyle("-fx-font-size: 10px; -fx-text-fill: #bdbdbd;");

        VBox centro = new VBox(3, msg, data);
        centro.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(centro, Priority.ALWAYS);

        // ponto indicador de não lida
        Region dot = new Region();
        dot.setMinSize(8, 8);
        dot.setMaxSize(8, 8);
        dot.setStyle("-fx-background-color: #1a237e; -fx-background-radius: 4;");
        dot.setVisible(!n.lida);

        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(barra, icone, centro, dot);
        HBox.setMargin(icone, new Insets(0, 10, 0, 10));
        HBox.setMargin(dot, new Insets(0, 12, 0, 4));
        row.setPadding(new Insets(12, 10, 12, 0));
        row.setStyle("-fx-cursor: hand; -fx-background-color: " +
            (n.lida ? "transparent" : "#f8f9ff") + ";");

        row.setOnMouseEntered(e -> row.setStyle("-fx-cursor: hand; -fx-background-color: #eef0fb;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-cursor: hand; -fx-background-color: " +
            (n.lida ? "transparent" : "#f8f9ff") + ";"));

        row.setOnMouseClicked(e -> {
            NotificacaoService.getInstance().marcarComoLida(n.id, n.isReserva);
            atualizarBadge();
            popup.hide();
            if (n.isReserva) {
                // ACEITE -> tab 1, REJEITADO -> tab 2
                int tab = "ACEITE".equals(n.tipo) ? 1 : 2;
                NavigationManager.getInstance().navegarParaMinhasReservasEstado(tab);
            } else if ("PROPOSTA".equals(n.tipo)) {
                // Novo pedido de reserva recebido -> vai direto para
                // Pedidos Recebidos, onde o proprietário pode aceitar/rejeitar
                NavigationManager.getInstance().navegarParaPedidosRecebidos();
            } else {
                mostrarDetalhe(n);
            }
        });

        return row;
    }

    // ══════════════════════════════════════
    // POPUP DE DETALHE
    // ══════════════════════════════════════

    private void mostrarDetalhe(Notificacao n) {

        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        String cor = corPorTipo(n.tipo);

        // faixa topo
        Label lblIconeTopo = new Label(iconePorTipo(n.tipo));
        lblIconeTopo.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;" +
            "-fx-background-color: rgba(255,255,255,0.25);" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 5 12 5 12;"
        );

        Label titTopo = new Label(tituloPorTipo(n.tipo));
        titTopo.setStyle(
            "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;"
        );

        VBox topo = new VBox(8, lblIconeTopo, titTopo);
        topo.setAlignment(Pos.CENTER);
        topo.setPadding(new Insets(28, 24, 24, 24));
        topo.setStyle("-fx-background-color: " + cor + "; -fx-background-radius: 12 12 0 0;");

        // separador decorativo
        Region separador = new Region();
        separador.setPrefHeight(3);
        separador.setStyle("-fx-background-color: " + cor + "; -fx-opacity: 0.15;");

        // corpo
        String motivo = extrairMotivo(n);

        Label lblMotivoTitulo = new Label("Mensagem:");
        lblMotivoTitulo.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #9e9e9e;" +
            "-fx-padding: 0 0 4 0;"
        );

        Label lblMotivo = new Label(motivo);
        lblMotivo.setWrapText(true);
        lblMotivo.setMaxWidth(320);
        lblMotivo.setStyle(
            "-fx-font-size: 13px; -fx-text-fill: #212121; -fx-line-spacing: 3;" +
            "-fx-background-color: #f5f5f5; -fx-background-radius: 6; -fx-padding: 10 12 10 12;"
        );

        // data formatada
        String dataStr = n.dataCriacao != null && n.dataCriacao.length() > 16
                ? n.dataCriacao.substring(0, 16) : n.dataCriacao;
        Label lblData = new Label("Recebido em:  " + dataStr);
        lblData.setStyle(
            "-fx-font-size: 11px; -fx-text-fill: #9e9e9e; -fx-padding: 8 0 0 0;"
        );

        VBox corpo = new VBox(6, lblMotivoTitulo, lblMotivo, lblData);
        corpo.setPadding(new Insets(20, 24, 10, 24));

        // botão fechar
        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle(
            "-fx-background-color: " + cor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-weight: bold;" +
            "-fx-font-size: 12px;" +
            "-fx-background-radius: 6;" +
            "-fx-padding: 8 32 8 32;" +
            "-fx-cursor: hand;"
        );
        btnFechar.setOnAction(e -> dialog.close());

        HBox rodape = new HBox(btnFechar);
        rodape.setAlignment(Pos.CENTER_RIGHT);
        rodape.setPadding(new Insets(8, 24, 20, 24));

        VBox root = new VBox(topo, separador, corpo, rodape);
        root.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 20, 0, 0, 8);"
        );

        Scene scene = new Scene(root, 400, 280);
        scene.setFill(null);
        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.show();

        dialog.setOnHidden(e -> atualizarPopup());
    }

    // ══════════════════════════════════════
    // HELPERS DE TIPO
    // ══════════════════════════════════════

    private String corPorTipo(String tipo) {
        return switch (tipo) {
            case "AVISO"     -> "#e53935";
            case "PROPOSTA"  -> "#1565c0";
            case "ACEITE"    -> "#2e7d32";
            case "REJEITADO" -> "#b71c1c";
            default          -> "#37474f";
        };
    }

    private String iconePorTipo(String tipo) {
        return switch (tipo) {
            case "AVISO"     -> "AVISO";
            case "PROPOSTA"  -> "PEDIDO";
            case "ACEITE"    -> "ACEITE";
            case "REJEITADO" -> "RECUSADO";
            default          -> "INFO";
        };
    }

    private String tituloPorTipo(String tipo) {
        return switch (tipo) {
            case "AVISO"     -> "Aviso da Administracao";
            case "PROPOSTA"  -> "Novo Pedido de Reserva";
            case "ACEITE"    -> "Reserva Aceite";
            case "REJEITADO" -> "Reserva Recusada";
            default          -> "Notificacao";
        };
    }

    private String textoResumido(Notificacao n) {
        return switch (n.tipo) {
            case "ACEITE"    -> "A tua reserva do " + n.nomeVeiculo + " foi aceite";
            case "REJEITADO" -> "A tua reserva do " + n.nomeVeiculo + " foi recusada";
            default          -> n.getMensagem();
        };
    }

    private String extrairMotivo(Notificacao n) {
        return n.getMensagem();
    }

    // ══════════════════════════════════════
    // LIDAS
    // ══════════════════════════════════════

    private void marcarTodasComoLidas() {
        var user = SessionManager.getInstance().getUtilizador();
        if (user != null) {
            NotificacaoService.getInstance().marcarTodasComoLidas(user.getId());
            badge.setVisible(false);
        }
    }

    // ══════════════════════════════════════
    // GETTERS
    // ══════════════════════════════════════

    public StackPane getSino() { return sino; }

    public void mostrarPopupAncoradoA(javafx.scene.Node ancora) {
        if (popup.isShowing()) { popup.hide(); return; }
        atualizarPopup();
        Point2D pt = ancora.localToScreen(0, 0);
        if (pt != null) {
            popup.show(ancora, pt.getX() - 300, pt.getY() + ancora.getBoundsInLocal().getHeight() + 6);
        }
    }

    public void parar() {
        if (autoRefresh != null) autoRefresh.stop();
    }
}