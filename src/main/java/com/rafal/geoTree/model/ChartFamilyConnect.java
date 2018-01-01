package com.rafal.geoTree.model;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Rafal on 30.12.2017.
 */
public class ChartFamilyConnect {
    public static class ResultLength {
        private Double min;
        private Double max;

        public Double getMin() {
            return min;
        }

        public void setMin(Double min) {
            this.min = min;
        }

        public Double getMax() {
            return max;
        }

        public void setMax(Double max) {
            this.max = max;
        }
    }

        XYChart.Data<Number,String> mianPerson;
    List<XYChart.Data<Number,String>> listSpose;
    List<XYChart.Data<Number,String>> listChild;

    public ChartFamilyConnect(){
        listChild = new ArrayList<>();
        listSpose = new ArrayList<>();
    }

    public void setMainPerson(XYChart.Data<Number,String> mainPerson){
        this.mianPerson = mainPerson;
    }

    public void addChild(XYChart.Data<Number,String> child){
        listChild.add(child);
    }

    public void addSpose(XYChart.Data<Number,String> spose){
        listSpose.add(spose);
    }

    public XYChart.Data<Number, String> getMianPerson() {
        return mianPerson;
    }

    public List<XYChart.Data<Number, String>> getListSpose() {
        return listSpose;
    }

    public List<XYChart.Data<Number, String>> getListChild() {
        return listChild;
    }

    public ResultLength getLengthBetweenUpperAndLower(){
        Double yMax = mianPerson.getNode().getLayoutY();
        Double yMin = yMax;

        List<XYChart.Data<Number,String>> presonList = new ArrayList<>(listSpose);
        presonList.addAll(listChild);
        for(XYChart.Data<Number,String> person : presonList){
            if(yMin > person.getNode().getLayoutY())
                yMin = person.getNode().getLayoutY();

            if(yMax < person.getNode().getLayoutY())
                yMax = person.getNode().getLayoutY();
        }

        ResultLength result = new ResultLength();
        result.setMin(yMin);
        result.setMax(yMax);

        return result;
    }

    public boolean fixAndValidate(CategoryAxis categoryAxis){
        if(mianPerson == null) return false;
        //sprawdzenie czy imie głównej osoby znajduje się na osi kategorii
        if(!categoryAxis.getCategories().contains(mianPerson.getYValue()))
            return false;

        //sprawdzenie czy imiona dzieci znajduja się na osi kategorii
        //jesli nie usuń osobę z rodziny
        List<XYChart.Data<Number,String>> toRemowe = new ArrayList<>();
        for(XYChart.Data<Number,String> item : listChild)
        {
            if(!categoryAxis.getCategories().contains(item.getYValue()))
                toRemowe.add(item);
        }
        listChild.removeAll(toRemowe);
        toRemowe.clear();
        //sprawdzenie czy imiona współmałżonków znajduja się na osi kategorii
        //jesli nie usuń osobę z rodziny
        for(XYChart.Data<Number,String> item : listSpose)
        {
            if(!categoryAxis.getCategories().contains(item.getYValue()))
                toRemowe.add(item);
        }
        listSpose.removeAll(toRemowe);
        //sprawdzenie czy obiekt jest poprawny
        if(listChild.isEmpty() && listSpose.isEmpty()) return false;

        return true;
    }
}
