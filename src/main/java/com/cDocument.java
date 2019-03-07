package com;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class represent document. here we collect all the data on File
 */
public class cDocument extends cItem {
    //    String ID;//DOCNO the id of doc
    String title;
    //    public String text;
    int max_tf;//the terms that appear the most in the file
    int docLenth;
    String city;
    String language;
    //    public HashMap<String, Integer> terms = new HashMap<>();
    LinkedHashSet cityPosition = new LinkedHashSet();


    cDocument(String ID, String title, String text) {
        super(ID, text);
        this.title = title;
    }

    /**
     * to do stemming to all of the term
     * @param stemmer - the algorithm to stemming
     */

}
