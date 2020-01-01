package eu.rapasoft;

import eu.rapasoft.cdi.StartupScene;
import javafx.application.Application;
import javafx.stage.Stage;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.util.AnnotationLiteral;

public class HelloFx extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) {
        SeContainerInitializer containerInitializer = SeContainerInitializer.newInstance();
        SeContainer container = containerInitializer.initialize();

        container.getBeanManager().fireEvent(primaryStage, new AnnotationLiteral<StartupScene>() {
        });
    }
}
