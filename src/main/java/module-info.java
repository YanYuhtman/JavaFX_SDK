module com.ileveli.javafx_sdk {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires io.github.oshai.kotlinlogging;


    opens com.ileveli.javafx_sdk to javafx.fxml;
    exports com.ileveli.javafx_sdk;
}