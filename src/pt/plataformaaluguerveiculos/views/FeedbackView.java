package pt.plataformaaluguerveiculos.views;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * ALV-60 - Mensagens básicas de erro/feedback
 * Utilitário para mostrar mensagens de erro, sucesso e confirmação ao utilizador.
 */
public class FeedbackView {

    public static void mostrarErro(String titulo, String mensagem) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.getDialogPane().getStyleClass().add("feedback-error");
        alert.showAndWait();
    }

    public static void mostrarSucesso(String titulo, String mensagem) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.getDialogPane().getStyleClass().add("feedback-sucesso");
        alert.showAndWait();
    }

    public static void mostrarAviso(String titulo, String mensagem) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
