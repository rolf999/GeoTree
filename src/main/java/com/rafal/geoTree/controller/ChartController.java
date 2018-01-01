package com.rafal.geoTree.controller;

import com.rafal.geoTree.model.AgeAnalyzer;
import com.rafal.geoTree.model.ChartFamilyConnect;
import com.rafal.geoTree.model.TreeStructure;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import com.rafal.geoTree.model.GanttChart;
import com.rafal.geoTree.model.GanttChart.ExtraData;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/**
 * Created by Rafal on 29.12.2017.
 */
public class ChartController {
    private int maxLevel = 200;
    private TreeItem<String> tree;
    private Stage stage;
    private TreeStructure treeStructure;
    private GanttChart<Number,String> chart;

    private int averageLifeLength = 2;

    //listy potrzebne do generowania wykresu
    private List<String> names = new ArrayList<>();
    private List<XYChart.Series> listOfSeries = new ArrayList<>();
    private List<ChartFamilyConnect> listOfFamiyConnection = new ArrayList<>();
    private AgeAnalyzer ageAnalyzer = new AgeAnalyzer();

    @FXML
    VBox vBox;
    ScrollPane scroll = new ScrollPane();

    //konstruktor wywolywany po wstrzyknieciu wszsytkich obiektow widoku
    public void init() {
        ChangeListener<Number> stageSizeListener = (observable, oldValue, newValue) -> {
            if(chart != null) {
                vBox.setMinSize(stage.getWidth() - 30, stage.getHeight() - 80);
                vBox.setMaxSize(stage.getWidth() - 30, stage.getHeight() - 80);
                scroll.setMinSize(stage.getWidth()-20, stage.getHeight()-80);
                chart.setMinSize(stage.getWidth()-60, chart.getHeight());
                autoRacing((NumberAxis)chart.getXAxis(),chart.getWidth());
            }
        };
        stage.widthProperty().addListener(stageSizeListener);
        stage.heightProperty().addListener(stageSizeListener);

        generateChart();
    }

    private void autoRacing(NumberAxis axis, Double chartWidth){
        double range = axis.getUpperBound()-axis.getLowerBound();
        int numbersOfInterwal = (int)(0.01*chartWidth);
        int tick = (int)range/numbersOfInterwal;
        tick = tick - tick%5;
        axis.setTickUnit(tick);
        axis.setMinorTickCount(3);

    }

    public void onClickEksportuj(){
        if(chart == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Eksport");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG", "*.png")
        );
        File file = fileChooser.showSaveDialog(stage);
        //Zabepieczenie przed anulowaniem dialogu
        if(file == null) return;

        String extension = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(".") + 1);
        BufferedImage image = SwingFXUtils.fromFXImage(chart.snapshot(new SnapshotParameters(), null),null);
        try {
            if ("jpg".equals(extension)) {
                BufferedImage imageRGB = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.OPAQUE);
                Graphics2D graphics = imageRGB.createGraphics();
                graphics.drawImage(image, 0, 0, null);
                ImageIO.write(imageRGB, extension, file);
            } else if ("png".equals(extension)) {
                ImageIO.write(image, extension, file);
            }
        } catch (IOException e) {
            showErrorAlert("Eksport zakończony niepowodzeniem");
        }
    }

    public TreeItem<String> getTree() {
        return tree;
    }

    public void setTree(TreeItem<String> tree) {
        this.tree = tree;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public TreeStructure getTreeStructure() {
        return treeStructure;
    }

    public void setTreeStructure(TreeStructure treeStructure) {
        this.treeStructure = treeStructure;
    }

    private void showErrorAlert(String message){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.setTitle("Błąd!");
        alert.showAndWait();
    }

    private void fillChart(TreeItem<String> person, ChartFamilyConnect family,int level){
        if(level > maxLevel) return;
        for(TreeItem<String> personInFamily: person.getChildren()){
            boolean isSpose = false;
            String name = personInFamily.getValue();
            if(personInFamily.getValue().startsWith("(m)")){
                name = name.replace("(m) ", "");
                isSpose = true;
            }
            if(!names.contains(name))
                names.add(name);
            AgeAnalyzer.AgeResult ageResult = ageAnalyzer.analyze(treeStructure.getIndividualByName(name),1);
            XYChart.Data<Number,String> personInChart = new XYChart.Data(getBirthDate(ageResult), name, createExtraData(ageResult));
            XYChart.Series series = new XYChart.Series();
            series.getData().add(personInChart);
            listOfSeries.add(series);
            if(isSpose)
                family.addSpose(personInChart);
            else{
                if(!personInFamily.getChildren().isEmpty()){
                    ChartFamilyConnect childFamily = new ChartFamilyConnect();
                    childFamily.setMainPerson(personInChart);
                    listOfFamiyConnection.add(childFamily);
                    fillChart(personInFamily, childFamily, ++level);
                    --level;
                }
                family.addChild(personInChart);
            }
        }
    }


    public void generateChart(){
        int level = 100;
        names.clear();
        listOfSeries.clear();
        listOfFamiyConnection.clear();

        if(tree == null) return;
        AgeAnalyzer.AgeResult age = ageAnalyzer.analyze(treeStructure.getIndividualByName(tree.getValue()),1);
        ChartFamilyConnect connect = null;
        if(!names.contains(tree.getValue()))
            names.add(tree.getValue());
        AgeAnalyzer.AgeResult ageResult = ageAnalyzer.analyze(treeStructure.getIndividualByName(tree.getValue()),1);
        XYChart.Data<Number,String> person = new XYChart.Data(getBirthDate(ageResult), tree.getValue(), createExtraData(ageResult));
        XYChart.Series series = new XYChart.Series();
        series.getData().add(person);
        listOfSeries.add(series);
        if(!tree.getChildren().isEmpty()){
            connect = new ChartFamilyConnect();
            connect.setMainPerson(person);
            listOfFamiyConnection.add(connect);
        }
        fillChart(tree, connect, ++level);




        final NumberAxis xAxis = new NumberAxis();
        final CategoryAxis yAxis = new CategoryAxis();
        chart = new GanttChart<Number,String>(xAxis,yAxis);
        xAxis.setLabel("");
        xAxis.setTickLabelFill(Color.CHOCOLATE);
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(this.ageAnalyzer.getLowerBirth()-30);
        xAxis.setUpperBound(this.ageAnalyzer.getUpperDead()+30);

        yAxis.setLabel("");
        yAxis.setTickLabelFill(Color.CHOCOLATE);
        yAxis.setTickLabelGap(10);
        yAxis.setCategories(FXCollections.<String>observableArrayList(names));

        //dodanie połączeni rodziny
        if(!listOfSeries.isEmpty()){
            XYChart.Series lastSeries = listOfSeries.get(listOfSeries.size()-1);
            for(ChartFamilyConnect con : listOfFamiyConnection){
                ExtraData extraData = new ExtraData(0,"status-black");
                extraData.setConnect(con);
                extraData.setFamilyConnection(true);
                person = new XYChart.Data(con.getMianPerson().getXValue(), con.getMianPerson().getYValue(), extraData);
                lastSeries.getData().add(person);
            }
        }

        //dodawnie wszystkich osob do wykresu
        for(XYChart.Series item : listOfSeries)
            chart.getData().add(item);
        chart.getStylesheets().add(getClass().getResource("/ganttchart.css").toExternalForm());
        chart.setMinSize(stage.getWidth(), 40*names.size());
        scroll.setContent(chart);
        scroll.setMinSize(stage.getWidth(), stage.getHeight());
        //scroll.setMinSize(stage.getWidth(),stage.getHeight());
        vBox.getChildren().add(scroll);
    }

    private ExtraData createExtraData(AgeAnalyzer.AgeResult ageResult){
        String style = ageResult.getBirth() == 0 ? "status-red" : "status-green";
        ExtraData extraData = new ExtraData(ageResult.getAge(), style);
        extraData.endLine = ageResult.isDeadKnow();
        extraData.StartLine = ageResult.isBirthKnow();
        return  extraData;
    }

    private int getBirthDate(AgeAnalyzer.AgeResult age){
        int birthYear = age.getBirth();
        if(birthYear == 0)
            birthYear = ageAnalyzer.getUpperDead()-ageAnalyzer.getAverageLifeAge();
        return  birthYear;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }
}
