package com;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 *
 */
public class Ranker {
    /**
     * rank all the documents according to the given query and corpus information
     *
     * @param query          - the query we search by
     * @param d_path         - the postings directory path
     * @param documents      - the documents file (as map)
     * @param dictionary     - the dictionary (as map)
     * @param numOfDoc       - number of docs in corpus
     * @param sumOfDocLength - the sum of all the document's length
     * @return (document : rank) map, for all the documents
     */
    public static Map<String, Double> rank(cQuery query, String d_path, HashMap<String, String> documents, HashMap<String, String> dictionary, int numOfDoc, long sumOfDocLength) {
        ExecutorService reader_pool = Executors.newCachedThreadPool();//Todo change to limit (8) threads?
        HashMap<String, Double> documentsRank = new HashMap<>();
        HashMap<String, String[]> termToDocTf = new HashMap<>();
        HashMap<Character, HashSet<String>> querytermOfChar = new HashMap<>();
        for (String queryTerm : query.terms.keySet()) {
            if (queryTerm.equals(""))
                continue;//divide the term in the query to set of each char to make the search in one time
            Character firstChar = (Character.isLetter(queryTerm.charAt(0)) ? queryTerm.charAt(0) : '_');
            HashSet<String> setOfTerms = querytermOfChar.getOrDefault(firstChar, new LinkedHashSet<>());
            setOfTerms.add(queryTerm.toLowerCase());
            setOfTerms.add(queryTerm.toUpperCase());
            querytermOfChar.put(firstChar, setOfTerms);
        }
        List<Future<Map<String, String[]>>> futuresTerms = new LinkedList<>();
        List<Future<Map<String, String[]>>> futuresCities = new LinkedList<>();

        for (Character ch : querytermOfChar.keySet()) {//start to search for lines of each term in the query
            Future<Map<String, String[]>> future = reader_pool.submit(new ReadThread(querytermOfChar.get(ch), ch, d_path));
            futuresTerms.add(future);
        }

        HashSet<String> documentsWithCities = new LinkedHashSet<>();
        HashMap<Character, HashSet<String>> citytermOfChar = new HashMap<>();

        for (String city : query.cities) {//divide the cities to set of every char to make the search in one time
            Character firstChar = (Character.isLetter(city.charAt(0)) ? Character.toLowerCase(city.charAt(0)) : '_');
            HashSet<String> setOfCities = citytermOfChar.getOrDefault(firstChar, new LinkedHashSet<>());
            setOfCities.add(city);
            setOfCities.add(city.toLowerCase());
            citytermOfChar.put(firstChar, setOfCities);
        }
        for (Character ch : citytermOfChar.keySet()) {//start the search of every set of cities.
            Future<Map<String, String[]>> future = reader_pool.submit(new ReadThread(citytermOfChar.get(ch), ch, d_path));
            futuresCities.add(future);
        }

        for (Future<Map<String, String[]>> future : futuresTerms) {//collect the line of the terms
            try {
                termToDocTf.putAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        for (Future<Map<String, String[]>> future : futuresCities) {//collect the lines of the cities
            Map<String, String[]> citiesOfChar = null;
            try {
                citiesOfChar = future.get();
                for (Map.Entry<String, String[]> entry : citiesOfChar.entrySet()) {
                    for (int i = 1; i < entry.getValue().length; i++) {
                        documentsWithCities.add(entry.getValue()[i].split(";")[0]);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        reader_pool.shutdown();
        double avdl = (double) sumOfDocLength / numOfDoc;
        double logMplus1 = Math.log(numOfDoc + 1);
        final double b = 0.4;
        final double k = 1.2;
        final double TITLE = 5;
        for (String queryTerm : query.terms.keySet()) {
            String[] docTF = null;
            String oldQueryTerm = queryTerm;
            if (!dictionary.containsKey(queryTerm.toLowerCase()))
                if (!dictionary.containsKey(queryTerm.toUpperCase()))
                    continue;
                else {
                    docTF = termToDocTf.get(queryTerm.toUpperCase());
                    queryTerm = queryTerm.toUpperCase();
                }
            else {
                docTF = termToDocTf.get(queryTerm.toLowerCase());
                queryTerm = queryTerm.toLowerCase();
            }
//            String[] docTF = termToDocTf.get(queryTerm);
            for (int i = 1; i < docTF.length; i++) {
                String docID = docTF[i].split(";")[0];
                String[] dataOfDoc = documents.get(docID).split(";");
                if (!(query.cities.isEmpty() || (query.cities.contains(dataOfDoc[4]) || documentsWithCities.contains(docID))))
                    continue;
                if (!(query.languages.isEmpty() || query.languages.contains(dataOfDoc[5])))
                    continue;
                String docTitle = "";
                docTitle = dataOfDoc[6];
                if (docTitle.equals(" "))
                    docTitle = "";
                int tf = Integer.parseInt(docTF[i].split(";")[1]);
                int docLenth = Integer.parseInt(dataOfDoc[2]);
                double numerator = query.terms.get(oldQueryTerm) * ((docTitle.contains(queryTerm.toUpperCase()) || docTitle.contains(queryTerm.toLowerCase())) ? TITLE : 1) * (k + 1) * tf * (logMplus1 - Math.log(Integer.parseInt(dictionary.get(queryTerm))));
                double denominator = tf + k * (1 - b + b * (docLenth / avdl));
                double bm25TodocAndTerm = numerator / denominator;
                documentsRank.put(docID, documentsRank.getOrDefault(docID, 0.0) + bm25TodocAndTerm);
            }
        }
        return documentsRank;
    }

}

class ReadThread implements Callable<Map<String, String[]>> {
    private HashSet<String> terms;
    private char firstChar;
    String path;

    /**
     * c'tor
     *
     * @param terms     - terms should retrieve
     * @param firstChar - the terms first letter
     * @param path      - the path to the postings directory
     */
    ReadThread(HashSet<String> terms, char firstChar, String path) {
        this.terms = terms;
        this.firstChar = firstChar;
        this.path = path;
    }

    /**
     * @return word:(the word fitted line from the postings file)
     */
    @Override
    public Map<String, String[]> call() {
        Map<String, String[]> linesOfTerms = new HashMap<>();
        File file = new File(path + "\\" + firstChar);
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String st;
            while ((st = bufferedReader.readLine()) != null && !terms.isEmpty()) {
                String term = st.substring(0, st.indexOf("~"));
                if (terms.contains(term)) {
                    linesOfTerms.put(term, st.split("\\|"));
                    terms.remove(term.toLowerCase());
                    terms.remove(term.toUpperCase());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return linesOfTerms;
    }
}