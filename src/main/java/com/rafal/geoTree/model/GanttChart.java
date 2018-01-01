package com.rafal.geoTree.model;

/**
 * Created by Rafal on 29.12.2017.
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class GanttChart<X,Y> extends XYChart<X,Y> {

    public static class ExtraData {

        public long length;
        public boolean StartLine = false;
        public boolean endLine = false;
        public boolean familyConnection = false;
        public ChartFamilyConnect connect;
        public String styleClass;


        public ExtraData(long lengthMs, String styleClass) {
            super();
            this.length = lengthMs;
            this.styleClass = styleClass;
        }
        public long getLength() {
            return length;
        }
        public void setLength(long length) {
            this.length = length;
        }
        public String getStyleClass() {
            return styleClass;
        }
        public void setStyleClass(String styleClass) {
            this.styleClass = styleClass;
        }

        public void setConnect(ChartFamilyConnect connect) {
            this.connect = connect;
        }

        public void setFamilyConnection(boolean familyConnection) {
            this.familyConnection = familyConnection;
        }
    }

    private double blockHeight = 20;

    public GanttChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }

    public GanttChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<X,Y>> data) {
        super(xAxis, yAxis);
        if (!(xAxis instanceof ValueAxis && yAxis instanceof CategoryAxis)) {
            throw new IllegalArgumentException("Axis type incorrect, X and Y should both be NumberAxis");
        }
        setData(data);
    }

    private static String getStyleClass( Object obj) {
        return ((ExtraData) obj).getStyleClass();
    }

    private static double getLength( Object obj) {
        return ((ExtraData) obj).getLength();
    }

    @Override protected void layoutPlotChildren() {
        NumberAxis xAxis = (NumberAxis)this.getXAxis();
        /*xAxis.setAutoRanging(false);
        xAxis.setLowerBound(100);
        xAxis.setUpperBound(124);
        xAxis.setTickUnit(3);*/
        //xAxis.setLowerBound(100);
        for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex++) {

            Series<X,Y> series = getData().get(seriesIndex);

            Iterator<Data<X,Y>> iter = getDisplayedDataIterator(series);
            while(iter.hasNext()) {
                Data<X,Y> item = iter.next();
                double x = getXAxis().getDisplayPosition(item.getXValue());

                double y = getYAxis().getDisplayPosition(item.getYValue());
                if (Double.isNaN(x) || Double.isNaN(y)) {
                    continue;
                }
                Node block = item.getNode();
                Shape ellipse;
                if (block != null) {
                    if (block instanceof StackPane) {
                        StackPane region = (StackPane)item.getNode();
                        ellipse = createBlock((ExtraData) item.getExtraValue());

                        y -= getBlockHeight() / 2.0;

                        region.setShape(null);
                        region.setShape(ellipse);
                        region.setScaleShape(false);
                        region.setCenterShape(false);
                        region.setCacheShape(false);

                        block.setLayoutX(x);
                        block.setLayoutY(y);
                    }
                }
            }
        }
    }

    private Shape createBlock(ExtraData data){
        Shape result;
        if(data.familyConnection)
            result = createFamilyConnect(data);
        else
            result= createPersonBlock(data);

        return result;
    }

    private Shape createPersonBlock(ExtraData data){
        double xScale = ((getXAxis() instanceof NumberAxis) ? Math.abs(((NumberAxis)getXAxis()).getScale()) : 1);
        double ySclae = ((getYAxis() instanceof NumberAxis) ? Math.abs(((NumberAxis)getYAxis()).getScale()) : 1);
        double width = getLength( data) * xScale;
        double height = getBlockHeight();
        Rectangle rectangleUp= new Rectangle(width,1);
        Rectangle rectangleDown= new Rectangle(width,1);
        rectangleDown.setLayoutX(0);
        rectangleDown.setLayoutY(height);
        Shape unionShape = Shape.union(rectangleUp,rectangleDown);

        Rectangle between = new Rectangle(width-getBlockHeight()/2,getBlockHeight()/2);
        between.setLayoutX(getBlockHeight()/4);
        between.setLayoutY(getBlockHeight()/4);
        unionShape = Shape.union(unionShape,between);

        if(data.StartLine){
            Rectangle rectangleLeft= new Rectangle(1,height);
            unionShape = Shape.union(unionShape,rectangleLeft);
        }

        if(data.endLine){
            Rectangle rectangleRight= new Rectangle(1,height);
            rectangleRight.setLayoutX(width);
            rectangleRight.setLayoutY(0);
            unionShape = Shape.union(unionShape,rectangleRight);
        }

        return unionShape;
    }

    private Shape createFamilyConnect(ExtraData data){
        if(!data.connect.fixAndValidate((CategoryAxis) getYAxis())) return null;
        Double circleRadious = getBlockHeight()/4;
        //pierwsza linia pomocnicza wyznaczająca punkt 0,0 obiektu
        Rectangle firstLine= new Rectangle(1,1);
        Shape result = firstLine;
        //wyznaczenia głównej lini od głównego przodka
        ChartFamilyConnect.ResultLength mainY= data.connect.getLengthBetweenUpperAndLower();


        //główna linia nad główną osobą
        Double lengthUpLine = data.connect.getMianPerson().getNode().getLayoutY()-mainY.getMin()-getBlockHeight()/2;
        Rectangle mainUpLine= new Rectangle(1,lengthUpLine);
        mainUpLine.setLayoutY(-lengthUpLine);
        result = Shape.union(result, mainUpLine);
        //główna linia pod główną osobą
        if(mainY.getMax() != data.connect.getMianPerson().getNode().getLayoutY()) {
            Double lengthDownLine = mainY.getMax() - data.connect.getMianPerson().getNode().getLayoutY() - getBlockHeight() / 2;
            Rectangle mainDownLine = new Rectangle(1, lengthDownLine);
            mainDownLine.setLayoutY(getBlockHeight());
            result = Shape.union(result, mainDownLine);
        }

        //połączenie dzieci
        for(XYChart.Data<Number,String> child : data.connect.getListChild()){
            Double widthLine = child.getNode().getLayoutX() - data.connect.getMianPerson().getNode().getLayoutX();
            Double heigthLine = child.getNode().getLayoutY() - data.connect.getMianPerson().getNode().getLayoutY()+getBlockHeight()/2;
            Rectangle childLine= new Rectangle(Math.abs(widthLine),1);
            childLine.setLayoutX(widthLine>=0 ? 0 : widthLine);
            childLine.setLayoutY(heigthLine);
            result = Shape.union(result, childLine);
        }
        //połączenie małżonków
        for(XYChart.Data<Number,String> spose : data.connect.getListSpose()){

            Double widthLine = spose.getNode().getLayoutX() - data.connect.getMianPerson().getNode().getLayoutX();
            Double heigthLine = spose.getNode().getLayoutY() - data.connect.getMianPerson().getNode().getLayoutY()+getBlockHeight()/2;
            Circle circle = new Circle(circleRadious);
            circle.setLayoutY(heigthLine);
            circle.setLayoutX(widthLine+circleRadious);

            widthLine = widthLine+2*circleRadious;
            Rectangle childLine= new Rectangle(Math.abs(widthLine),1);
            childLine.setLayoutX(widthLine>=0 ? 0 : widthLine);
            childLine.setLayoutY(heigthLine);

            result = Shape.union(result, childLine);
            result = Shape.union(result, circle);
        }


        return result;
    }

    public double getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight( double blockHeight) {
        this.blockHeight = blockHeight;
    }

    @Override protected void dataItemAdded(Series<X,Y> series, int itemIndex, Data<X,Y> item) {
        Node block = createContainer(series, getData().indexOf(series), item, itemIndex);
        getPlotChildren().add(block);
    }

    @Override protected  void dataItemRemoved(final Data<X,Y> item, final Series<X,Y> series) {
        final Node block = item.getNode();
        getPlotChildren().remove(block);
        removeDataItemFromDisplay(series, item);
    }

    @Override protected void dataItemChanged(Data<X, Y> item) {
    }

    @Override protected  void seriesAdded(Series<X,Y> series, int seriesIndex) {
        for (int j=0; j<series.getData().size(); j++) {
            Data<X,Y> item = series.getData().get(j);
            Node container = createContainer(series, seriesIndex, item, j);
            getPlotChildren().add(container);
        }
    }

    @Override protected  void seriesRemoved(final Series<X,Y> series) {
        for (XYChart.Data<X,Y> d : series.getData()) {
            final Node container = d.getNode();
            getPlotChildren().remove(container);
        }
        removeSeriesFromDisplay(series);

    }


    private Node createContainer(Series<X, Y> series, int seriesIndex, final Data<X,Y> item, int itemIndex) {

        Node container = item.getNode();

        if (container == null) {
            container = new StackPane();
            item.setNode(container);
        }

        container.getStyleClass().add( getStyleClass( item.getExtraValue()));

        return container;
    }

}
