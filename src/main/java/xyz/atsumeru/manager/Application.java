package xyz.atsumeru.manager;

public class Application {

    // Workaround for launching JavaFX fat-jar
    public static void main(String[] args) {
        javafx.application.Application.launch(FXApplication.class, args);
        System.exit(0);
    }
}
