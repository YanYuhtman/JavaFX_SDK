module com.ileveli.javafx_sdk {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires io.github.oshai.kotlinlogging;
    requires kotlinx.coroutines.core;
    requires kotlinx.serialization.json;

    opens com.ileveli.javafx_sdk to javafx.fxml;
    exports com.ileveli.javafx_sdk;
}