module JavaFxSkeletonApp {
    requires javax.inject;
    requires cdi.api;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    exports eu.rapasoft;
    exports eu.rapasoft.controller;
    opens eu.rapasoft to weld.core.impl;
    opens eu.rapasoft.cdi to weld.core.impl;
}