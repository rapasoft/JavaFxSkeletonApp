package eu.rapasoft.controller;

import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import io.reactivex.schedulers.Schedulers;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.stream.IntStream;

public class MainController {
    public static final String BLOCKING = "Blocking";
    public static final String NON_BLOCKING_OLD = "Non-Blocking JavaFx";
    public static final String NON_BLOCKING = "Non-Blocking RxJavaFx";
    private static final int NUMBER_OF_TASKS = 10;
    @FXML
    public ComboBox<String> selection;
    @FXML
    public ListView<String> listOfTasks;

    @FXML
    public void initialize() {
        selection.setItems(FXCollections.observableArrayList("-", BLOCKING, NON_BLOCKING_OLD, NON_BLOCKING));
        selection.valueProperty().addListener((observable, oldValue, value) -> {
            ObservableList<String> observableList = FXCollections.observableList(new ArrayList<>());
            listOfTasks.setItems(observableList);

            switch (value) {
                case BLOCKING:
                    runTasksJavaFx(observableList);
                    break;
                case NON_BLOCKING_OLD:
                    runTasksLaterJavaFx(observableList);
                    break;
                case NON_BLOCKING:
                    runTasksRxJavaFx(observableList);
                    break;
                default:
                    break;
            }
        });
    }

    private void runTasksJavaFx(ObservableList<String> observableList) {
        IntStream.range(1, NUMBER_OF_TASKS)
                .mapToObj(this::runTask)
                .map(result -> result.time > 500 ? new Result(result.name + " (slow)", result.time) : result)
                .forEach(result -> observableList.add(result.toString()));
    }

    private void runTasksLaterJavaFx(ObservableList<String> observableList) {
        IntStream.range(1, NUMBER_OF_TASKS)
                .forEach(i -> Platform.runLater(() -> {
                    Result result = runTask(i);
                    if (result.time > 500) {
                        result = new Result(result.name + " (slow)", result.time);
                    }
                    observableList.add(result.toString());
                }));
    }

    private void runTasksRxJavaFx(ObservableList<String> observableList) {
        Observable.range(1, NUMBER_OF_TASKS)
                .subscribeOn(Schedulers.computation())
                .map(this::runTask)
                .map(result -> result.time > 500 ? new Result(result.name + " (slow)", result.time) : result)
                .observeOn(JavaFxScheduler.platform())
                .forEach(result -> observableList.add(result.toString()));
    }

    private Result runTask(Integer i) {
        long currentTime = System.currentTimeMillis();

        String name = "Task" + i;
        long sleepDuration = (long) (Math.random() * 1000);

        try {
            Thread.sleep(sleepDuration);
            return new Result(name, sleepDuration);
        } catch (Exception e) {
            return new Result("-", 0);
        } finally {
            System.out.println(name + " took " + (System.currentTimeMillis() - currentTime) + " ms");
        }
    }

    static class Result {
        public final String name;
        public final long time;

        public Result(String name, long time) {
            this.name = name;
            this.time = time;
        }

        @Override
        public String toString() {
            return name + " (" + time + " ms)";
        }
    }

}
