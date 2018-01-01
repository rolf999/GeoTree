package com.rafal.geoTree.controller;

import com.rafal.geoTree.model.TreeStructure;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.gedcom4j.model.*;
import org.gedcom4j.parser.GedcomParser;

import org.gedcom4j.parser.GedcomParserException;
import org.gedcom4j.query.Finder;
import org.gedcom4j.validate.GedcomValidator;
import org.gedcom4j.writer.GedcomWriter;
import org.gedcom4j.writer.GedcomWriterException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainController {

    private Stage stage;
    private Gedcom gedcom;
    private TreeStructure treeStructure;


    //label statusu z okna glownego
    @FXML
    Label labelStatus;

    //label z imieniem i nazwiskiem wybranej osoby
    @FXML
    Label labelName;
    //label z datą urodzenia
    @FXML
    Label birthdayDate;
    //label z datą śmierci
    @FXML
    Label deadDate;

    //drzewno rodziny w oknie glownym
    @FXML
    TreeView personTree;

    //kontener zawierający szczegółowe dane o osobie
    @FXML
    VBox personDetails;

    //lista dzieci wybranej Osoby
    @FXML
    ListView childList;
    //listawspolmalzonkow  wybranej osoby
    @FXML
    ListView supouseList;
    //przycisk pokazujacy cale potomstwo wybranej osoby
    @FXML
    Button buttonShowAll;
    //przycisk do generowania wykresu
    @FXML
    Button buttonGenerateChart;


    private Individual selectedPerson;
    private Individual selectedChild;
    private Individual selectedSpouse;
    private TreeItem<String> selectedPersonInTree;

    //konstruktor wywolywany po wstrzyknieciu wszsytkich obiektow widoku
    @FXML
    public void initialize() {
        personDetails.setVisible(false);
        //dodanie akcji po wybrani osoby z drzewa
        personTree.getSelectionModel().selectedItemProperty().addListener( new ChangeListener() {
            public void changed(ObservableValue observable, Object oldValue,
                                Object newValue) {
                selectedPersonInTree = (TreeItem<String>) newValue;
                selectPerson((TreeItem<String>) newValue);
            }

        });
        childList.getSelectionModel().selectedItemProperty().addListener( new ChangeListener() {
            public void changed(ObservableValue observable, Object oldValue,
                                Object newValue) {
                selectedChild = treeStructure.getIndividualByName((String) newValue);
            }

        });
        supouseList.getSelectionModel().selectedItemProperty().addListener( new ChangeListener() {
            public void changed(ObservableValue observable, Object oldValue,
                                Object newValue) {
                selectedSpouse = treeStructure.getIndividualByName((String) newValue);
                childList.setItems(treeStructure.createListOfChild(selectedPerson, selectedSpouse));
            }

        });
    }

    public void onClickCreateChart(){
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/chart.fxml"));
        Parent root = null;
        try {
            root = (Parent)loader.load();
        } catch (IOException e) {
            showErrorAlert("Wystapił błąd w trakcie otwierania strony");
            return;
        }
        ChartController controller = (ChartController)loader.getController();
        Stage newStage = new Stage();
        newStage.setTitle("Wykres");
        controller.setStage(newStage);
        controller.setTreeStructure(treeStructure);
        controller.setTree(this.selectedPersonInTree);
        controller.init();
        Scene scene = new Scene(root, 1000, 700);

        newStage.setScene(scene);
        newStage.show();
    }

    public void onClickRefresh(){
        if(gedcom == null) return;
        if(!treeStructure.validate()) {
            showErrorAlert("Zaburzono strukturę drzewa");
            return;
        }
            fillTreeOfPerson(personTree);
    }

    public void onClickAbout(){
        String about = "GeoTree\n" +
                        "wersja : 1.0\n" +
                        "autor  : Rafał Grzelec";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("O aplikacji");
        alert.setHeaderText(null);
        alert.setContentText(about);
        alert.showAndWait();
    }

    public void onClickAddSpouse(){
        List<String> listOfPerson = new ArrayList<>();
        List<String> nameOfFamilyPerson = treeStructure.createListOfPersonInFamily(selectedPerson, true);
        nameOfFamilyPerson.add(treeStructure.getNameByIndividual(selectedPerson));
        for(String name: treeStructure.getMapOfIndividual().keySet()) {
            if(nameOfFamilyPerson.contains(name))
                continue;
            listOfPerson.add(name);
        }
        Optional<String> result = showSelectDialog(listOfPerson, "Wybierz osobę którą chcesz dodać", listOfPerson.get(0));
        //zabezpieczenie przed anulowanie wyboru
        if(!result.isPresent()) return;

        Family newFamily = treeStructure.createFamily(selectedPerson, treeStructure.getIndividualByName(result.get()));

        treeStructure.addFamily(newFamily);
        childList.setItems(treeStructure.createListOfChild(selectedPerson, null));
        supouseList.setItems(treeStructure.createListOfSupose(selectedPerson));
    }

    public void onClickDeleteSpouse(){
        if(selectedSpouse == null){
            showInformationAlert("Nie wybrano małżonka/małżonki");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Usunąć całą rodzinę");
        ButtonType buttonTypeOne = new ButtonType("Tak");
        ButtonType buttonTypeTwo = new ButtonType("Nie");
        ButtonType buttonTypeThree = new ButtonType("Anuluj");
        alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo, buttonTypeThree);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeOne){
            treeStructure.deleteFamily(treeStructure.findFamilySpouse(selectedPerson,selectedSpouse));
        } else if (result.get() == buttonTypeTwo) {
            treeStructure.deleteSpouseFromFamily(treeStructure.findFamilySpouse(selectedPerson,selectedSpouse),selectedSpouse);
        } else if (result.get() == buttonTypeThree) {
            return;
        }

        childList.setItems(treeStructure.createListOfChild(selectedPerson, null));
        supouseList.setItems(treeStructure.createListOfSupose(selectedPerson));
    }

    public void onClickAddChild(){
        List<String> listOfPerson = new ArrayList<>();
        List<String> listOfFamilyName = treeStructure.createListOfPersonInFamily(selectedPerson, true);
        for(String name: treeStructure.getMapOfIndividual().keySet()) {
            if(listOfFamilyName.contains(name))
                continue;
            listOfPerson.add(name);
        }
        List<String> listOfSpouseName = treeStructure.createListOfPersonInFamily(selectedPerson, false);
        listOfPerson.remove(treeStructure.getNameByIndividual(selectedPerson));
        listOfSpouseName.add("");

        Family family;
        //wybranie osoby z ktora ma sie dodawane dziecko
        Optional<String> result = showSelectDialog(listOfSpouseName, "Wybierz z którego zwiazku pochodzić ma dziecko", "");
        //zabezpieczenie przed anulowanie wyboru
        if (!result.isPresent()) return;
        if(result.get().equals(""))
            family = null;
        else
            family = treeStructure.findFamilySpouse(selectedPerson, treeStructure.getIndividualByName(result.get()));

        //wybranie osoby z do dodania
        result = showSelectDialog(listOfPerson, "Wybierz osobę do dodania", listOfPerson.get(0));
        //zabezpieczenie przed anulowanie wyboru
        if (!result.isPresent()) return;
        Individual child = treeStructure.getIndividualByName(result.get());

        if(family == null)
            for(FamilySpouse familySpouse: selectedPerson.familiesWhereSpouse)
                if(familySpouse.family.wife != null || familySpouse.family.husband != null)
                    family = familySpouse.family;

        if(family != null){
            treeStructure.addChildToFamily(family, child);
        }else{
            family = new Family();
            family.children.add(child);
            if(selectedPerson.sex != null){
                if("M".equals(selectedPerson.sex.value))
                    family.husband = selectedPerson;
                else
                    family.wife = selectedPerson;
            }else{
                family.husband = selectedPerson;
            }

            treeStructure.addFamily(family);
        }

        childList.setItems(treeStructure.createListOfChild(selectedPerson, null));
        supouseList.setItems(treeStructure.createListOfSupose(selectedPerson));
    }

    public void onClickDeleteChild(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Czy na pewno chcesz usunąć daną osobę ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            treeStructure.deleteChildFromFamily(treeStructure.findFamilyChild(selectedPerson,selectedChild), selectedChild);
        } else {
            return;
        }
        childList.setItems(treeStructure.createListOfChild(selectedPerson, null));
        supouseList.setItems(treeStructure.createListOfSupose(selectedPerson));
    }



    public void onClickbuttonZamknij(){
        System.exit(0);
    }

    public void onClickShowAll(){
        if(selectedPerson == null) return;
        childList.setItems(treeStructure.createListOfChild(selectedPerson, null));
    }

    public void onClickEditBirthday(){
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Edycja");
        dialog.setHeaderText("Wprowadź nową datę urodzenia (dd-mm-yyyy)");
        dialog.setContentText("Data:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            DateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            Date date = null;
            try {
                date = format.parse(result.get());
            } catch (ParseException e) {
                showErrorAlert("Niepoprawny format daty");
                return;
            }
            format = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            changeEventDate(selectedPerson,format.format(date),IndividualEventType.BIRTH);
            birthdayDate.setText(format.format(date));
        }
    }

    public void onClickEditDead(){
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Edycja");
        dialog.setHeaderText("Wprowadź nową datę śmierci (dd-mm-yyyy)");
        dialog.setContentText("Data:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            DateFormat format = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
            Date date = null;
            try {
                date = format.parse(result.get());
            } catch (ParseException e) {
                showErrorAlert("Niepoprawny format daty");
                return;
            }
            format = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            changeEventDate(selectedPerson,format.format(date),IndividualEventType.DEATH);
            deadDate.setText(format.format(date));
        }
    }

    public void onClickButtonOtworz(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GEDCOM", "*.ged"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        File file = fileChooser.showOpenDialog(stage);
        //Zabepieczenie przed anulowaniem dialogu
        if(file == null) return;
        clear();
        if(openFile(file)){
            setStatus(file.getAbsolutePath());
        } else {
            showInformationAlert("Błąd otwarcia pliku");
            return;
        }
        treeStructure = new TreeStructure(gedcom);
        fillTreeOfPerson(personTree);
    }

    public void onClickButtonZapiszJako(){
        if(gedcom == null) {
            showInformationAlert("Brak danych do zapisu!");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Zapisz plik");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GEDCOM", "*.ged")
        );
        File file = fileChooser.showSaveDialog(stage);
        //Zabepieczenie przed anulowaniem dialogu
        if(file == null) return;

        if(saveFile(file)){
            showInformationAlert("Zapisywanie pliku zakończone powodzeniem");
        }else{
            showInformationAlert("Zapisywanie pliku zakończone niepowodzeniem");
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setStatus(String message){
        labelStatus.setText(message);
    }

    public Gedcom getGedcom() {
        return gedcom;
    }

    public void setGedcom(Gedcom gedcom) {
        this.gedcom = gedcom;
    }

    private boolean openFile(File file){
        if(file == null)
            return false;

        GedcomParser gedcomParserp = new GedcomParser();
        try {
            gedcomParserp.load(file.getAbsolutePath());
        } catch (IOException e) {
            return false;
        } catch (GedcomParserException e) {
            return false;
        }
        setGedcom(gedcomParserp.gedcom);

        return true;
    }

    private boolean saveFile(File file){
        if(file == null)
            return false;

        if(gedcom == null)
            return false;

        GedcomWriter gedcomWriter = new GedcomWriter(gedcom);
        try {
            gedcomWriter.write(file);
        } catch (IOException e) {
            return  false;
        } catch (GedcomWriterException e) {
            return  false;
        }

        return true;
    }

    private void fillTreeOfPerson(TreeView personTree){
        //Znalezienie korzeni drzew genealogicznych
        TreeItem<String> root = treeStructure.findTreeStructure();

        personTree.setRoot(root);
    }

    private void selectPerson(TreeItem<String> selectedItem){
        selectedSpouse = null;
        selectedChild = null;
        if(selectedItem == null) return;
        //ukrycie przycisku generowania wykresu dla małżonków
        if(selectedItem.getValue().startsWith(("(m)")))
            buttonGenerateChart.setVisible(false);
        else
            buttonGenerateChart.setVisible(true);

        String name = selectedItem.getValue();
        if(name.startsWith("(m)"))
            name = name.replace("(m) ", "");
        if("drzewa".equals(name) || !treeStructure.getMapOfIndividual().containsKey(name)){
            personDetails.setVisible(false);
            return;
        }
        personDetails.setVisible(true);
        selectedPerson = treeStructure.getIndividualByName(name);
        labelName.setText(name);

        if(selectedPerson.familiesWhereSpouse.size() >= 2)
            buttonShowAll.setVisible(true);
        else
            buttonShowAll.setVisible(false);

        childList.setItems(treeStructure.createListOfChild(selectedPerson, null));
        supouseList.setItems(treeStructure.createListOfSupose(selectedPerson));
        fillBirthdayAndDeadDate();

    }

    private void fillBirthdayAndDeadDate(){
        birthdayDate.setText("?");
        deadDate.setText("?");
        for(IndividualEvent event : selectedPerson.events){
            if(event.date == null) continue;
            if(IndividualEventType.BIRTH.equals(event.type)) {
                birthdayDate.setText(event.date.value);
            }
            if(IndividualEventType.DEATH.equals(event.type)) {
                deadDate.setText(event.date.value);
            }
        }
    }

    private void changeEventDate(Individual individual, String date, IndividualEventType type){
        for(IndividualEvent event : selectedPerson.events){
            if(type.equals(event.type)) {
                event.date.value = date;
            }
        }
    }


    private void showInformationAlert(String message){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Uwaga!");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private void showErrorAlert(String message){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd!");
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private Optional<String> showSelectDialog(List<String> choices, String message, String firstElement){
        ChoiceDialog<String> dialog = new ChoiceDialog<>(firstElement,choices);
        dialog.setTitle("");
        dialog.setHeaderText(message);
        dialog.setContentText("");

        return dialog.showAndWait();
    }

    private void clear(){
        selectedPerson = null;
        selectedChild = null;
        selectedSpouse = null;
        gedcom = null;
        treeStructure = null;
        personDetails.setVisible(false);
        selectedPersonInTree = null;
    }
}
