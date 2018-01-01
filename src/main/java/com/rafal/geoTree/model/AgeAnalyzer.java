package com.rafal.geoTree.model;

import org.gedcom4j.model.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Rafal on 01.01.2018.
 */
public class AgeAnalyzer {
    public static class AgeResult{
        boolean deadKnow;
        boolean birthKnow;
        int birth;
        int dead;
        int age;

        public boolean isDeadKnow() {
            return deadKnow;
        }

        public boolean isBirthKnow() {
            return birthKnow;
        }

        public int getBirth() {
            return birth;
        }

        public int getDead() {
            return dead;
        }

        public int getAge() {
            return age;
        }
    }
    private int infinity = 100000;
    private int averageFirstChildAge = 30;
    private int averageLifeAge = 90;
    private int maxLevelOfSearach = 100;
    private Integer lowerBirth = infinity;
    private Integer upperDead = 0;


    public AgeResult analyze(Individual individual, int level){
        AgeResult result = new AgeResult();
        result.birth = getBirtyYear(individual);
        result.dead = getDeadYear(individual);
        if(result.birth == 0)
            result.birthKnow = false;
        else
            result.birthKnow = true;
        if(result.dead == 0)
            result.deadKnow = false;
        else
            result.deadKnow = true;

        //oszacuj date smierci na podstawie daty urodzenia
        if(result.dead == 0 && result.birth != 0) {
            result.dead = result.birth +averageLifeAge;
        }
        //oszacuj date urodznia na podstawie daty smierci
        if(result.birth == 0 && result.dead != 0) {
            result.birth = result.dead -averageLifeAge;
        }
        //oszacuj daty na podstawie dzieci
        if(result.birth == 0 && result.dead == 0) {
            probablyAge(individual, result, level);
        }
        //jeśli poprzednie oszacowanie zakończone niepowodzeniem, spróbuj oszacować w inny sposób
        if(result.birth == 0 && result.dead == 0){
            probablyAgeSceondChance(individual, result);
        }

        if(result.birth>result.dead) {
            result.deadKnow = false;
            result.dead = result.birth + averageLifeAge;
        }

        result.age = Math.abs(result.dead-result.birth);

        if(result.birth!=0) {
            if (lowerBirth > result.getBirth())
                lowerBirth = result.getBirth();
            if (upperDead < result.getDead())
                upperDead = result.getDead();
        }
        return result;
    }

    public int getDateFromString(String string){
        if(string == null) return  0;
        Pattern p = Pattern.compile("-?\\d+");
        Matcher m = p.matcher(string);
        while (m.find()) {
            int date = Integer.parseInt(m.group());
            if(date >= 100)
                return date;
        }
        return  0;
    }

    private int getBirtyYear(Individual individual){
        if(individual == null) return 0;
        for(IndividualEvent event : individual.events){
            if(IndividualEventType.BIRTH.equals(event.type))
                return getDateFromString(event.date.value);
        }
        return 0;
    }

    private int getDeadYear(Individual individual){
        if(individual == null) return 0;
        for(IndividualEvent event : individual.events){
            if(IndividualEventType.DEATH.equals(event.type) && event.date != null)
                return getDateFromString(event.date.value);
        }
        return 0;
    }

    private void probablyAge(Individual individual, AgeResult actualData, int level){
        if(level>maxLevelOfSearach) return;
        int birthAge = infinity;
        int deadAge = infinity;
        for(FamilySpouse familySpouse : individual.familiesWhereSpouse){
            for(Individual child : familySpouse.family.children){
                AgeResult childAge = analyze(child, level+1);
                if(childAge.birth != 0){
                    int newAge = childAge.birth - averageFirstChildAge;
                    if(birthAge > newAge)
                        birthAge = newAge;
                }

                if(childAge.dead != 0){
                    int newAge = childAge.dead - averageLifeAge;
                    if(deadAge > newAge)
                        deadAge = newAge;
                }
            }
            //jesli nie okreslono na podstawie dzieci, okresl na podstawie wspolmalzonkow
            if(birthAge == infinity && deadAge == infinity){
                //pobraie współmałżonka
                Individual spose = individual.equals(familySpouse.family.husband) ? familySpouse.family.wife : familySpouse.family.husband;
                int sposeBirth = getBirtyYear(spose);
                int sposeDead = getDeadYear(spose);
                if(sposeBirth != 0) {
                    birthAge = sposeBirth;
                    deadAge = birthAge + averageLifeAge;
                    //data urodzenia ma pierwszenstwoa nad datą smierci
                    continue;
                }
                if(sposeDead != 0){
                    birthAge = sposeDead - averageLifeAge;
                    deadAge = sposeDead;
                }
            }

        }
        //sprawdzenie czy poszukiwania sie udały, jeśli nie to zakoncz
        checktResult(birthAge,deadAge, actualData);
    }

    //ustalenie daty urodzenia na podstawie okresu życia rodzinców i rodzeństwa
    public void probablyAgeSceondChance(Individual individual, AgeResult actualData){
        int birthAge = infinity;
        int deadAge = infinity;
        for(FamilyChild familyChild : individual.familiesWhereChild){

            if(familyChild.family.husband != null) {
                int dadBirth = getBirtyYear(familyChild.family.husband);
                if (dadBirth != 0) {
                    birthAge = dadBirth + averageFirstChildAge;
                    deadAge = birthAge + averageLifeAge;
                    //data urodzenia ma pierwszenstwoa nad datą smierci
                    break;
                }
            }
            if(familyChild.family.wife != null){
                int momBirth = getBirtyYear(familyChild.family.wife);
                if(momBirth != 0) {
                    birthAge = momBirth+averageFirstChildAge;
                    deadAge = birthAge + averageLifeAge;
                    break;
                }
            }
            for(Individual siblings : familyChild.family.children){
                //wykluzenie samego siebie
                if(individual.equals(siblings)) continue;
                int siblingsBirth = getBirtyYear(familyChild.family.wife);
                if(siblingsBirth != 0) {
                    birthAge = siblingsBirth;
                    deadAge = birthAge + averageLifeAge;
                    break;
                }
            }
        }
        //sprawdzenie czy poszukiwania sie udały, jeśli nie to zakoncz
        checktResult(birthAge,deadAge, actualData);
    }

    private void checktResult(int birthAge, int deadAge, AgeResult actualData){
        if (birthAge == infinity && deadAge == infinity)
            return;
        else if(birthAge != infinity && deadAge != infinity){
            actualData.birth = birthAge;
            actualData.dead = birthAge+averageLifeAge;
        }else if(birthAge != infinity && deadAge == infinity){
            actualData.birth = birthAge;
            actualData.dead = birthAge+averageLifeAge;
        }else if(birthAge == infinity && deadAge != infinity){
            actualData.birth = deadAge-averageLifeAge;
            actualData.dead = deadAge;
        }
    }



    public int getAverageLifeAge() {
        return averageLifeAge;
    }

    public Integer getLowerBirth() {
        return lowerBirth;
    }

    public Integer getUpperDead() {
        return upperDead;
    }

    public boolean isValid(){
        if(lowerBirth > upperDead)
            return false;
        else
            return true;
    }
}
