package com;

import java.util.HashMap;
import java.util.Map;

public abstract class cItem {
    String ID;//DOCNO the id of doc
    public String text;

    public HashMap<String, Integer> terms = new HashMap<>();

    public cItem(String ID, String text) {
        this.ID = ID;
        this.text = text;

    }

    void stem_dictionary(Stemmer stemmer) {
        HashMap<String, Integer> newTerms = new HashMap<>();
        try {
            for (Map.Entry<String, Integer> entry : terms.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();
                String term_s = stemmer.stemTerm(key);
                if (!newTerms.containsKey(term_s))
                    newTerms.put(term_s, value);
                else
                    newTerms.put(term_s, newTerms.get(term_s) + value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        terms = newTerms;
    }
}
