package fr.ulille.phlam.ram2catgui;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class RAM2CATConverter extends Task {

    private int skipLines;
    private double elowMin;
    private int fileFormat;

    private DoubleProperty conversionTemperature;
    private DoubleProperty partitionFunction;
    private BooleanProperty hasToSubtractElow;
    private Property<Paint> inputFileColor;
    private Property<File> fileToConvert;
    private StringProperty molecularTag;

    private class Transition{
        private static final double C = 29979.2458; // 1 cm-1 in MHz units !!!
        public int j2, ka2, kc2, m2, j1, ka1, kc1, m1, s1, s2;
        public double f2, f1, freq, smu2, elo, unc;
        public double its, li;

        public Transition(){
            j2 = 0; ka2 = 0; kc2 = 0; m2 = 0; j1 = 0; ka1 = 0; kc1 = 0; m1 = 0; s1 = 0; s2 = 0;
            f2 = 0; f1 = 0; freq = 0; smu2 = 0; elo = 0; unc = 0;
            its = 0; li = 0;
        }

        public String getConvertedString(){
            String convertedString = "";
            if (hasToSubtractElow.get()) elo = elo-elowMin;
            double fcm = freq/C;
            double T = conversionTemperature.doubleValue();
            double Qrs = partitionFunction.doubleValue();
            String tag = molecularTag.getValue();
            double its = 4.16231e-05*freq*smu2*(Math.exp(-1.438*elo/T)-Math.exp(-1.438*(elo+fcm)/T))/Qrs;
            double li = Math.log10(its);
            //TODO : take half-integer quanta into account in the QFMT field
            if (fileFormat==0){
                convertedString = String.format(Locale.US, "%13.4f%8.4f%8.4f%2d%10.4f%3d %6s1404%2d%2d%2d%2d    %2d%2d%2d%2d",
                        freq,unc,li,3,elo,Math.round(j2)*2+1,tag,j2,ka2,kc2,m2,j1,ka1,kc1,m1);
            }
            if (fileFormat==1){
                convertedString = String.format(Locale.US, "%13.4f%8.4f%8.4f%2d%10.4f%3d %6s1405%2d%2d%2d%2d%2d  %2d%2d%2d%2d%2d",
                        freq,unc,li,3,elo,Math.round(f2)*2+1,tag,j2,ka2,kc2,m2,Math.round(Math.ceil(f2)),j1,ka1,kc1,m1,Math.round(Math.ceil(f1)));
            }
            if (fileFormat==2){
                convertedString = String.format(Locale.US, "%13.4f%8.4f%8.4f%2d%10.4f%3d %6s1405%2d%2d%2d%2d%2d  %2d%2d%2d%2d%2d",
                        freq,unc,li,3,elo,Math.round(f2)*2+1,tag,j2,ka2,kc2,m2,s2,j1,ka1,kc1,m1,s2);
            }
            return convertedString;
        }
    }

    public RAM2CATConverter(){
        conversionTemperature = new SimpleDoubleProperty(300);
        partitionFunction = new SimpleDoubleProperty(1);
        hasToSubtractElow = new SimpleBooleanProperty(true);
        inputFileColor = new SimpleObjectProperty<>(Color.BLACK);
        fileToConvert = new SimpleObjectProperty<>();
        molecularTag = new SimpleStringProperty();
    }

    @Override
    protected Object call() {
        convertFile(fileToConvert.getValue());
        return null;
    }

    private Transition setTransition(String s){
        Transition transition = new Transition();
        String[] array1 = s.split("\\(|\\)");
        if (array1.length!=3) return transition;
        else {
            if (array1[1].startsWith("*")) transition.unc = 999.9999;
            else transition.unc = Double.parseDouble(array1[1]);
            String[] array2 = array1[0].split("\\s+");
            String[] array3 = array1[2].trim().split("\\s+");
            if (array2.length==13||array2.length==14){
                fileFormat=1;
                for (int i=0;i<array2.length-1;i++){
                    transition.m2=Integer.parseInt(array2[1]);
                    transition.j2=Integer.parseInt(array2[3]);
                    transition.ka2=Integer.parseInt(array2[4]);
                    transition.kc2=Integer.parseInt(array2[5]);
                    transition.m1=Integer.parseInt(array2[7]);
                    transition.j1=Integer.parseInt(array2[9]);
                    transition.ka1=Integer.parseInt(array2[10]);
                    transition.kc1=Integer.parseInt(array2[11]);
                    transition.f2 = Double.parseDouble(array2[2]);
                    transition.f1 = Double.parseDouble(array2[8]);
                    transition.freq = Double.parseDouble(array2[array2.length-1]);
                }
            }
            if (array2.length==11||array2.length==12){
                for (int i=0;i<array2.length-1;i++){
                    transition.m2=Integer.parseInt(array2[1]);
                    transition.j2=Integer.parseInt(array2[2]);
                    transition.ka2=Integer.parseInt(array2[3]);
                    transition.kc2=Integer.parseInt(array2[4]);
                    transition.m1=Integer.parseInt(array2[6]);
                    transition.j1=Integer.parseInt(array2[7]);
                    transition.ka1=Integer.parseInt(array2[8]);
                    transition.kc1=Integer.parseInt(array2[9]);
                    transition.freq = Double.parseDouble(array2[array2.length-1]);
                }
                if (array2.length==12&array2[10].trim().length()==2){
                    fileFormat = 2; // RAM36 two-top catalog format
                    switch (array2[10].trim()){
                        case "00" : transition.s2 = 0; // AA
                        case "01" : transition.s2 = 1; // EE
                        case "11" : transition.s2 = 3; // EA
                        case "12" : transition.s2 = 5; // AE
                    }
                } else {
                    fileFormat = 0;
                }
            }
            transition.elo = Double.parseDouble(array3[0]);
            transition.smu2 = Double.parseDouble(array3[1]);
        }
        return transition;
    }

    public void convertFile(File file){
        if (file==null) return;
        Path path = Paths.get(file.getAbsolutePath());
        List<String> rlist = new ArrayList();
        try {
            rlist = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * To distinguish between "catalog" and "standard" output, the first line is checked
         */
        if (rlist.get(0).startsWith("Temperature"))
            skipLines = 10;
        else if (rlist.get(0).trim().startsWith("Upper"))
            skipLines = 4;
        else {
            inputFileColor.setValue(Color.RED);
            updateProgress(0,1);
            updateMessage("Invalid input file format!");
            //this.cancel();
            Platform.runLater(()->{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("File Format Error");
                alert.setHeaderText("Format Error");
                alert.setContentText("Invalid input file format!");
                alert.show();
            });
            return;
        }
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file.getPath()+".cat");
        } catch (IOException e) {
            e.printStackTrace();
        }
        PrintWriter printWriter = new PrintWriter(fileWriter);
        inputFileColor.setValue(Color.BLUE);
        List<Transition> transitions = rlist.stream().skip(skipLines).map(this::setTransition).collect(Collectors.toList());
        elowMin = transitions.stream().map(t->t.elo).min(Double::compareTo).get();
        for (Transition t : transitions){
            updateProgress(transitions.indexOf(t),transitions.size());
            printWriter.println(t.getConvertedString());
        }
        updateProgress(0,1);
        updateMessage("Output saved to: "+file.getName()+".cat"+" ("+transitions.size()+")");
        printWriter.close();
    }

    public DoubleProperty conversionTemperatureProperty() {
        return conversionTemperature;
    }

    public DoubleProperty partitionFunctionProperty() {
        return partitionFunction;
    }

    public BooleanProperty hasToSubtractElowProperty() {
        return hasToSubtractElow;
    }

    public Property<Paint> inputFileColorProperty() {
        return inputFileColor;
    }

    public void setFileToConvert(File fileToConvert) {
        this.fileToConvert.setValue(fileToConvert);
    }

    public StringProperty molecularTagProperty() {
        return molecularTag;
    }
}
