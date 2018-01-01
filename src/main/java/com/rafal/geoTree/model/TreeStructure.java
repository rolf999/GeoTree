package com.rafal.geoTree.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.gedcom4j.model.*;
import org.gedcom4j.writer.GedcomWriter;
import org.gedcom4j.writer.GedcomWriterException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Rafal on 12.12.2017.
 */
public class TreeStructure {

    private Map<String,Individual> mapOfIndividual;
    private Map<String, Integer> lengthOfPeopleWithThisSameName;
    //private Map<Individual,String>
    private Gedcom gedcom;

    public TreeStructure(Gedcom gedcom){
        this.gedcom = gedcom;
        mapOfIndividual = new HashMap<String, Individual>();
        lengthOfPeopleWithThisSameName = new HashMap<String, Integer>();
        Map<String, Individual> individuals = gedcom.individuals;
        for(Individual individual : individuals.values()){
            String name = getNameFromIndividual(individual);

            //sprawdzenie czy imie danej osoby sie nie powtarza
            if(mapOfIndividual.containsKey(name)){
                if(lengthOfPeopleWithThisSameName.containsKey(name)) {
                    int length = lengthOfPeopleWithThisSameName.get(name);
                    lengthOfPeopleWithThisSameName.remove(name);
                    lengthOfPeopleWithThisSameName.put(name, length+1);
                } else {
                    lengthOfPeopleWithThisSameName.put(name, 2);
                }
                mapOfIndividual.put(name + " " + lengthOfPeopleWithThisSameName.get(name), individual);
                continue;
            }
            mapOfIndividual.put(name, individual);
        }
    }

    private String getNameFromIndividual(Individual individual){
        String valueInTree = "";
        if(individual.names == null || individual.names.isEmpty()) return "nieznany";
        String name = individual.names.get(0).basic;
        String[] pelneImie = name.split("/");
        for (String czesc : pelneImie){
            valueInTree += czesc + " ";
        }
        return valueInTree;
    }

    public Individual getIndividualByName(String name){
        if(!mapOfIndividual.containsKey(name))
            return null;

        return mapOfIndividual.get(name);
    }

    public String getNameByIndividual(Individual individual){
        String result = null;
        if(individual == null)
            return null;

        List<String> keys = new ArrayList<String>(mapOfIndividual.keySet());
        for(String key : keys){
            if(individual == mapOfIndividual.get(key)) {
                result = key;
                return result;
            }
        }
        //nie znaleziono
        return  null;
    }

    public TreeItem<String> findTreeStructure(){
        // znaklezienie korzeni drzewa/drzew
        TreeItem<String> roots = findRoot();

        for(TreeItem<String> root : roots.getChildren()){
            fillFamilyOfChild(root, false, 0);
        }

        return roots;
    }

    public void fillFamilyOfChild(TreeItem<String> treeIndividual, boolean withSpouse, int poziom){
        //zabezpieczenie przed zbyt glebokim wejsciem w rekurencje
        if(poziom > 100)
            return;

        Individual rootIndividual = getIndividualByName(treeIndividual.getValue());
        if(rootIndividual == null) return;
        //pobranie wszystkich małżonków
        if(withSpouse == true) {
            List<Individual> spouses = getSpouse(rootIndividual);
            for (Individual spouse : spouses) {
                TreeItem<String> spouseItem = new TreeItem<String>("");
                String nameOfSpouse = getNameByIndividual(spouse);
                spouseItem.setValue(("(m) ") + nameOfSpouse);
                treeIndividual.getChildren().add(spouseItem);
            }
        }
        //przejscie po wszystkich rodzinach korzenia i dodanie dzieci wraz z dalszą ich inicjalizacją
        for(FamilySpouse familiesWhereSpouse : rootIndividual.familiesWhereSpouse){
            List<Individual> children = familiesWhereSpouse.family.children;
            for(Individual child : familiesWhereSpouse.family.children){
                String childName = getNameByIndividual(child);
                TreeItem<String> childInTree = new TreeItem<String>(childName);
                treeIndividual.getChildren().add(childInTree);
                fillFamilyOfChild(childInTree, true, poziom+1);
            }
        }


    }

    public TreeItem<String> findRoot(){
        TreeItem<String> root = new TreeItem<String>("drzewa");
        //Znalezienie korzeni drzew genealogicznych
        for(Individual individual : mapOfIndividual.values()){
            //jeśli dana osoba ma zdefiniowanej w pliku rodziny w której jest dzieckie lub
            //nie ma rodziny w której zdefiniowany jest jako rodzic, odrzuc taką osobe
            if(!individual.familiesWhereChild.isEmpty() || individual.familiesWhereSpouse.isEmpty())
                continue;

            //pobranie małżonka/małżonkę
            List<Individual> spouses = getSpouse(individual);
            if(!spouses.isEmpty() && !spouses.get(0).familiesWhereChild.isEmpty())
                continue;

            String valueInTree = getNameByIndividual(individual);
            TreeItem<String> individualInTree = new TreeItem<String>(valueInTree);
            root.getChildren().add(individualInTree);
        }

        //za korzen moze byc uznane malzenstwo, przeszukanie wynikow w poszukiwaniu takowych i usuniecie drugiej polowki jako osobne drzewo
        List<TreeItem<String>> toRemove = new ArrayList<TreeItem<String>>();
        for(TreeItem<String> item : root.getChildren() ){
            boolean isToRemove = false;
            for(TreeItem<String> itemToRemove : toRemove ){
                if(item.equals(itemToRemove))
                    isToRemove = true;
            }
            if(isToRemove)
                continue;

            String name = item.getValue();
            Individual individual = getIndividualByName(name);
            List<Individual> sposes = getSpouse(individual);
            if(sposes.isEmpty())
                continue;

            for(Individual spose : sposes){
                String nameOfSpouse = getNameByIndividual(spose);
                TreeItem<String> spouseItem = findChildItem(root,nameOfSpouse);
                if(spouseItem == null) continue;
                    spouseItem.setValue(("(m) ")+nameOfSpouse);
                item.getChildren().add(spouseItem);
                toRemove.add(spouseItem);
            }
        }

        for(TreeItem<String> item : toRemove ){
            root.getChildren().remove(item);
        }

        return root;
    }

    public List<Individual> getSpouse(Individual individual){
        List<Individual> result = new ArrayList<Individual>();
        if(individual == null) return result;
        for(FamilySpouse family : individual.familiesWhereSpouse){
            Individual spouse = family.family.husband;
            if(individual.equals(spouse))
                spouse = family.family.wife;
            if(spouse != null)
                result.add(spouse);
        }
        return result;
    }

    private TreeItem<String> findChildItem(TreeItem<String> items, String name){
        TreeItem<String> result = null;
        if(name == null) return  null;
        for(TreeItem<String> item : items.getChildren()){
            if(name.equals(item.getValue())) {
                result = item;
                break;
            }
        }
        return result;
    }

    public ObservableList createListOfChild(Individual individual, Individual spose){
        //wypelnienie listy dzieci i związkow
        ObservableList namesOfChild =
                FXCollections.observableArrayList();

        for(FamilySpouse familySpouse : individual.familiesWhereSpouse){
            Family family = familySpouse.family;
            if(spose != null && family.husband != spose && family.wife != spose)
                continue;
            for(Individual child: family.children)
                namesOfChild.add(getNameByIndividual(child));
        }
        return namesOfChild;
    }

    public ObservableList createListOfSupose(Individual individual){
        ObservableList namesOfSpouse =
                FXCollections.observableArrayList();
        for(FamilySpouse familySpouse : individual.familiesWhereSpouse){
            Family family = familySpouse.family;

            if(family.husband == individual && family.wife !=null)
                namesOfSpouse.add(getNameByIndividual(family.wife));
            else if(family.wife == individual && family.husband !=null)
                namesOfSpouse.add(getNameByIndividual(family.husband));
        }
        return namesOfSpouse;
    }

    public String createNextFamilyId(){
        String familyId = "@F" + gedcom.families.size()+"@";
        int i = 1;
        while (true) {
            if (gedcom.families.containsKey(familyId)) {
                familyId = "@F" + (gedcom.families.size() + i) + "@";
                i++;
            }else
                break;
        }
        return familyId;
    }

    public void addFamily(Family family){
        if(family == null) return;
        family.xref = createNextFamilyId();
        gedcom.families.put(family.xref, family);

        FamilySpouse newSpouseFamily = new FamilySpouse();
        newSpouseFamily.family = family;
        if(family.wife!= null){
            family.wife.familiesWhereSpouse.add(newSpouseFamily);
        }
        if(family.husband!= null){
            family.husband.familiesWhereSpouse.add(newSpouseFamily);
        }

        FamilyChild newChildFamily = new FamilyChild();
        newChildFamily.family = family;
        for(Individual child : family.children)
            child.familiesWhereChild.add(newChildFamily);
    }

    public Family createFamily(Individual spouse1, Individual spouse2){
        Family result = new Family();
        if(spouse1.sex != null && "M".equals(spouse1.sex.value)){
            result.husband = spouse1;
            result.wife = spouse2;
        }else {
            result.husband = spouse2;
            result.wife = spouse1;
        }
        return result;
    }

    public List<String> createListOfPersonInFamily(Individual individual, boolean withChildren){
        List<String> names = new ArrayList<>();
        for(FamilySpouse familySpouse : individual.familiesWhereSpouse){
            if(familySpouse.family.wife != null && familySpouse.family.wife != individual)
                names.add(getNameByIndividual(familySpouse.family.wife));
            if(familySpouse.family.husband != null && familySpouse.family.husband != individual)
                names.add(getNameByIndividual(familySpouse.family.husband));

            if(withChildren)
            for(Individual child: familySpouse.family.children)
                names.add(getNameByIndividual(child));
        }
        return names;
    }

    public Family findFamilySpouse(Individual spouse1, Individual spouse2){
        Family findedFamily = null;
        for(FamilySpouse familySpouse: spouse1.familiesWhereSpouse){
            if(familySpouse.family.wife == spouse2 || familySpouse.family.husband == spouse2)
                findedFamily = familySpouse.family;
        }
        return findedFamily;
    }

    public Family findFamilyChild(Individual parent, Individual child){
        Family findedFamily = null;
        for(FamilySpouse familySpouse: parent.familiesWhereSpouse){
            if(familySpouse.family.children.contains(child))
                findedFamily = familySpouse.family;
        }
        return findedFamily;
    }

    public void addChildToFamily(Family family, Individual child){
        if(family == null) return;
        family.children.add(child);
        FamilyChild familyChild = new FamilyChild();
        familyChild.family = family;
        child.familiesWhereChild.add(familyChild);
    }

    public void addSpouseToFamily(Family family, Individual spouse){
        if(family == null) return;
        if(spouse.sex != null && "M".equals(spouse.sex.value)) {
            if(family.husband != null)
                family.husband = spouse;
            else
                family.wife = spouse;

        }else{
            if(family.wife != null)
                family.wife = spouse;
            else
                family.husband = spouse;
        }
        FamilySpouse familySpouse = new FamilySpouse();
        familySpouse.family = family;
        spouse.familiesWhereSpouse.add(familySpouse);
    }

    public void deleteChildFromFamily(Family family, Individual child){
        if(family == null) return;
        for(FamilyChild familyChild : child.familiesWhereChild){
            if(familyChild.family.equals(family)){
                child.familiesWhereChild.remove(familyChild);
                break;
            }
        }
        family.children.remove(child);
        deleteFamilyIfEmpty(family);
    }

    public void deleteSpouseFromFamily(Family family, Individual spouse){
        if(family == null) return;
        if(family.wife == spouse)
            family.wife = null;
        else if(family.husband == spouse)
            family.husband = null;

        for(FamilySpouse familySpouse : spouse.familiesWhereSpouse){
            if(familySpouse.family.equals(family)) {
                spouse.familiesWhereSpouse.remove(familySpouse);
                break;
            }
        }
        deleteFamilyIfEmpty(family);
    }

    public void deleteFamilyIfEmpty(Family family){
        if(family == null) return;
        // przyadek gdzy rodzina sklada się jedynie z żony
        if(family.husband == null && family.wife != null && family.children.isEmpty()) {
            for(FamilySpouse familySpouse : family.wife.familiesWhereSpouse){
                if(familySpouse.family.equals(family)) {
                    family.wife.familiesWhereSpouse.remove(familySpouse);
                    break;
                }
            }
            gedcom.families.remove(family.xref);
        } else

        // przypadek gdy rodzina sklada sie jedynie z męrza
        if(family.husband != null && family.wife == null && family.children.isEmpty()) {
            for(FamilySpouse familySpouse : family.husband.familiesWhereSpouse){
                if(familySpouse.family.equals(family)) {
                    family.husband.familiesWhereSpouse.remove(familySpouse);
                    break;
                }
            }
            gedcom.families.remove(family.xref);
        } else

        // przypadek gdzy rodzina sklada sie z jednego dziecka badz jest pusta
        if(family.husband == null && family.wife == null && family.children.size() < 2){
            for(Individual child : family.children){
                for(FamilyChild familyChild : child.familiesWhereChild){
                    if(familyChild.family.equals(family)) {
                        child.familiesWhereChild.remove(familyChild);
                        break;
                    }
                }
            }
            gedcom.families.remove(family.xref);
        }

    }

    public void deleteFamily(Family family){
        if(family == null) return;
        if(family.wife!= null){
            for(FamilySpouse familySpouse : family.wife.familiesWhereSpouse){
                if(familySpouse.family.equals(family)) {
                    family.wife.familiesWhereSpouse.remove(familySpouse);
                    break;
                }
            }
        }
        if(family.husband!= null){
            for(FamilySpouse familySpouse : family.husband.familiesWhereSpouse){
                if(familySpouse.family.equals(family)) {
                    family.husband.familiesWhereSpouse.remove(familySpouse);
                    break;
                }
            }
        }

        for(Individual child : family.children) {
            for(FamilyChild familyChild : child.familiesWhereChild){
                if(familyChild.family.equals(family)) {
                    child.familiesWhereChild.remove(familyChild);
                    break;
                }
            }
        }

        gedcom.families.remove(family.xref);
    }

    public Map<String, Individual> getMapOfIndividual() {
        return mapOfIndividual;
    }

    public boolean validate(){
        return  true;
    }
}
