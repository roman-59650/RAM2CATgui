package fr.ulille.phlam.ram2catgui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.controlsfx.control.StatusBar;
import java.io.File;
import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private final FileChooser fileChooser = new FileChooser();
    private RAM2CATConverter converter;
    private File fileToConvert;

    @FXML
    private Button convertButton;
    @FXML
    private Button openFileButton;
    @FXML
    private Label fileLabel;
    @FXML
    private TextField temperatureField;
    @FXML
    private TextField partitionFunctionField;
    @FXML
    private CheckBox hasToSubtractElowBox;
    @FXML
    private StatusBar statusBar;
    @FXML
    private TextField molecularTagField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        molecularTagField.textProperty().addListener((ob, o, n)->{
            if (!n.trim().matches("\\d*")) molecularTagField.setText(n.replaceAll("\\D", ""));
            if (n.trim().length()>6) molecularTagField.setText(n.substring(0,6));
        });

        DoubleProperty p = new SimpleDoubleProperty(1);
        DoubleProperty t = new SimpleDoubleProperty(300);
        setAndBindNumericField(partitionFunctionField, p, 1, 1);
        setAndBindNumericField(temperatureField, t, 300, 3);

        convertButton.setOnAction(event->{
            if (fileToConvert!=null) {
                Service<Void> converterService = new Service<Void>() {
                    @Override
                    protected Task<Void> createTask() {
                        converter = new RAM2CATConverter();
                        if (fileToConvert!=null) converter.setFileToConvert(fileToConvert);
                        molecularTagField.textProperty().bindBidirectional(converter.molecularTagProperty());
                        converter.molecularTagProperty().bind(molecularTagField.textProperty());
                        converter.hasToSubtractElowProperty().bind(hasToSubtractElowBox.selectedProperty());
                        converter.conversionTemperatureProperty().bind(t);
                        converter.partitionFunctionProperty().bind(p);
                        fileLabel.textFillProperty().bindBidirectional(converter.inputFileColorProperty());
                        statusBar.progressProperty().bind(converter.progressProperty());
                        statusBar.textProperty().bind(converter.messageProperty());

                        converter.setOnCancelled(e->{
                            statusBar.progressProperty().unbind();
                            statusBar.textProperty().unbind();
                        });
                        return converter;
                    }
                };
                converterService.start();
            }
        });

        openFileButton.setOnAction(event -> {
            fileToConvert = fileChooser.showOpenDialog(Main.getPrimaryStage());
            fileLabel.setTextFill(Color.BLACK);
            if (fileToConvert!=null) {
                fileLabel.setText(fileToConvert.getName());
            }
        });
    }

    private void setAndBindNumericField(TextField textField, DoubleProperty p, double defaultValue, int decimals) {
        String format = "%."+decimals+"f";
        textField.setOnMouseClicked(e-> textField.selectAll());
        textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue& textField.getText().isEmpty()){
                textField.setText(String.format(Locale.US,format,defaultValue));
            } else if (oldValue) {
                textField.setText(String.format(Locale.US,format,p.getValue()));
            }
        });
        textField.textProperty().addListener((ob, o, n)->{
            if (n.matches("^\\d+|^\\d+.\\d*"))
                p.setValue(Double.parseDouble(n));
            else
                textField.setText(n.replaceAll("\\D",""));
        });
    }
}
