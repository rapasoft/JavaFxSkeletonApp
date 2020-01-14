package eu.rapasoft;

import eu.rapasoft.cdi.StartupScene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;

@ApplicationScoped
public class App {

    private static final String MAIN_FXML = "/main.fxml";

    @Inject
    private FXMLLoader fxmlLoader;

    public void start(@Observes @StartupScene Stage stage) throws IOException {
        URL mainFxml = getClass().getResource(MAIN_FXML);
        Parent root = fxmlLoader.load(mainFxml.openStream());
        Scene scene = new Scene(root, 500, 500);

        stage.setTitle("HelloFX");
        stage.setScene(scene);
        stage.show();
    }

}
