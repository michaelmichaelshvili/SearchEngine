package com;

import java.util.HashMap;
import java.util.HashSet;

public class cQuery extends cItem {
    //    Integer num;
//    String title;
    String description;
    String narrative;
    HashSet<String> cities;
    HashSet<String> languages;
    public HashMap<String, Integer> descTerms = new HashMap<>();


    public cQuery(String ID, String text, HashSet<String> cities,HashSet<String> languages) {
        super(ID, text);
        this.cities = (HashSet<String>)cities.clone();
        this.languages = (HashSet<String>)languages.clone();
    }
//    terms
}
