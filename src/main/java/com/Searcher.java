package com;

import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This class mission is to return results(as most relevant documents) for given queries
 */
public class Searcher {

    private HashMap<String, String[]> termToCloseTerms = new HashMap<>();//a word to 10 most related words map
    private String postings_dir;
    private HashMap<String, String> documents;
    private HashMap<String, String> dictionary;
    TreeSet<String> cities = new TreeSet<>();
    TreeSet<String> languages = new TreeSet<>();
    private long sumOfDocLength = 0;
    private int numOfDoc = 0;
    private HashSet<String> stopWords = new HashSet<>();

    /**
     * c'tor
     * this function import all the resources needed to make a search into the memory:
     * * documents file
     * * semantics helper file
     * * dictionary
     * * corpus's cities
     * * corpus's languages
     * * sum of all the documents length
     * * number of documents
     * * stop words
     *
     * @param postings_dir - the stem/nostem postings file path
     */
    public Searcher(String postings_dir) {
        this.postings_dir = postings_dir;
        BufferedReader br = null;
        try {

            br = new BufferedReader(new InputStreamReader(Searcher.class.getResourceAsStream("termtoterm.properties")));
            String st;
            while ((st = br.readLine()) != null) {
                int index = st.indexOf('=');
                termToCloseTerms.put(st.substring(0, index), st.substring(index + 2, st.length() - 1).split(", "));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Close.close(br);
        }
        documents = new HashMap<>(MapSaver.loadMap(postings_dir + "\\documents"));
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String[] docInfo = entry.getValue().split(";");
            sumOfDocLength += Long.parseLong(docInfo[2]);
            if (!docInfo[4].equals(" "))
                cities.add(docInfo[4]);
            if (!docInfo[5].equals(" "))
                languages.add(docInfo[5]);
        }
        dictionary = new HashMap<>(MapSaver.loadMap(postings_dir + "\\dic"));
        numOfDoc = documents.size();
        File file = new File(postings_dir + "\\" + "stop_words.txt");
        br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String st;
            while ((st = br.readLine()) != null)
                stopWords.add(st.toLowerCase());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Close.close(br);
        }
        stopWords.remove("between");
        stopWords.remove("may");

    }

    /**
     * @param query      - a single string query
     * @param ifStem     - if the search is with stemming
     * @param ifSemantic - if the search is with semantic treatment
     * @param cities     - the cities the document should have at their "city" parameter. cities==null=>every city possible
     * @param languages  - the languages the document should have at their "language" parameter. languages==null=>every language possible
     * @return most relevant(50 or less) documents to the query with the given filters
     */
    public Map<String, List<Pair<String, String[]>>> search(String query, boolean ifStem, boolean ifSemantic, HashSet<String> cities, HashSet<String> languages) {
        cQuery cquery = new cQuery(String.valueOf((int) (Math.random() * 1000)), query, cities, languages);//Todo change ID
        cquery = (cQuery) Parse.Parser.parse(cquery, ifStem, stopWords);
        if (ifSemantic) {
            Set<String> termsCopy = new HashSet<>(cquery.terms.keySet());
            for (String s : termsCopy) {
//                if (termToCloseTerms.containsKey(s.toLowerCase())) {
//                    for (String s2 : termToCloseTerms.get(s.toLowerCase())) {
//                        if (cquery.terms.containsKey(s2.toUpperCase()))
//                            cquery.terms.put(s2.toUpperCase(), cquery.terms.get(s2.toUpperCase()) + 1);
//                        else if (cquery.terms.containsKey(s2.toLowerCase()))
//                            cquery.terms.put(s2.toLowerCase(), cquery.terms.get(s2.toLowerCase()) + 1);
//                        else
//                            cquery.terms.put(s2.toLowerCase(), cquery.terms.get(s2.toLowerCase()) + 1);
//                    }
//                }
                if (termToCloseTerms.containsKey(s.toLowerCase())) {
                    cquery.terms.put(termToCloseTerms.get(s.toLowerCase())[9], cquery.terms.getOrDefault(termToCloseTerms.get(s.toLowerCase())[9], 0) + 1);
//                        cquery.terms.put(termToCloseTerms.get(s.toLowerCase())[8], cquery.terms.getOrDefault(termToCloseTerms.get(s.toLowerCase())[8], 0) + 1);
                }
            }
        }

        Map<String, Double> rankedDocuments = Ranker.rank(cquery, postings_dir, documents, dictionary, numOfDoc, sumOfDocLength);
        // Create a list from elements of HashMap
        List<Map.Entry<String, Double>> list = new LinkedList<>(rankedDocuments.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });
        List<Pair<String, String[]>> temp = new LinkedList<>();
        best50Docs(list, temp);
        Map<String, List<Pair<String, String[]>>> map = new TreeMap<>();
        map.put(cquery.ID, temp);
        return map;
    }

    /**
     * @param path       - the path to the queries file
     * @param ifStem     - if the search is with stemming
     * @param ifSemantic - if the search is with semantic treatment
     * @param cities     - the cities the document should have at their "city" parameter. cities==null=>every city possible
     * @param languages  - the languages the document should have at their "language" parameter. languages==null=>every language possible
     * @return most relevant(50 or less) documents to each query in the queries file with the given filters
     */
    public Map<String, List<Pair<String, String[]>>> search(Path path, boolean ifStem, boolean ifSemantic, HashSet<String> cities, HashSet<String> languages) {
        Map<String, List<Pair<String, String[]>>> relevantDocToQuery = new TreeMap<>();
        Document document = null;
        try {
            document = Jsoup.parse(new String(Files.readAllBytes(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Thread> threads = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        Elements queries = document.getElementsByTag("top");
        for (Element qElement : queries) {
            String qid = qElement.getElementsByTag("num").get(0).childNode(0).toString().trim().split(":")[1];
            String qtitle = qElement.getElementsByTag("title").get(0).text();
            String qdesc = qElement.getElementsByTag("desc").get(0).childNode(0).toString().trim().split(":")[1];
            String qnarr = qElement.getElementsByTag("narr").get(0).text();
            cQuery cquery = new cQuery(qid, qtitle + " " + qdesc, cities, languages);
            cquery.description = qdesc;
            cquery.narrative = qnarr;


            cquery = (cQuery) Parse.Parser.parse(cquery, ifStem, stopWords);
            if (ifSemantic) {
                Set<String> termsCopy = new HashSet<>(cquery.terms.keySet());
                for (String s : termsCopy) {
//                    if (termToCloseTerms.containsKey(s.toLowerCase())) {
//                        for (String s2 : termToCloseTerms.get(s.toLowerCase())) {
//                            if (cquery.terms.containsKey(s2.toUpperCase()))
//                                cquery.terms.put(s2.toUpperCase(), cquery.terms.get(s2.toUpperCase()) + 1);
//                            else if (cquery.terms.containsKey(s2.toLowerCase()))
//                                cquery.terms.put(s2.toLowerCase(), cquery.terms.get(s2.toLowerCase()) + 1);
//                            else
//                                cquery.terms.put(s2.toLowerCase(), 1);
//                        }
//                    }
                    if (termToCloseTerms.containsKey(s.toLowerCase())) {
                        cquery.terms.put(termToCloseTerms.get(s.toLowerCase())[9], cquery.terms.getOrDefault(termToCloseTerms.get(s.toLowerCase())[9], 0) + 1);
//                        cquery.terms.put(termToCloseTerms.get(s.toLowerCase())[8], cquery.terms.getOrDefault(termToCloseTerms.get(s.toLowerCase())[8], 0) + 1);
                    }
                }
            }
            Map<String, Double> rankedDocuments = Ranker.rank(cquery, postings_dir, documents, dictionary, numOfDoc, sumOfDocLength);
            // Create a list from elements of HashMap
            List<Map.Entry<String, Double>> list = new LinkedList<>(rankedDocuments.entrySet());

            // Sort the list
            Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
                public int compare(Map.Entry<String, Double> o1,
                                   Map.Entry<String, Double> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });
            List<Pair<String, String[]>> temp = new LinkedList<>();
            best50Docs(list, temp);
            relevantDocToQuery.put(qid, new LinkedList<>(temp));
        }
        return relevantDocToQuery;
    }

    /**
     * take the best 50 docs from the documentToRank list(that their rank bigger then 0) and put them with their entities in temp
     *
     * @param documentToRank - documentToRank of doc:rank entries
     * @param temp           -           documentToRank of doc:(doc entities array) pairs
     */
    private void best50Docs(List<Map.Entry<String, Double>> documentToRank, List<Pair<String, String[]>> temp) {
        int i = 50;
        for (Map.Entry<String, Double> aa : documentToRank) {
            if (aa.getValue() > 0) {
                temp.add(new Pair<>(documents.get(aa.getKey()).split(";")[0], getDocumentEntities(aa.getKey())));
                i--;
            } else
                break;
            if (i == 0)
                break;
        }
    }

    /**
     * @param doc - document name
     * @return the given document most common entities
     */
    private String[] getDocumentEntities(String doc) {
        String entry = documents.get(doc);
        return entry.substring(entry.lastIndexOf(";") + 2, entry.length() - 1).split(",");

    }
}
