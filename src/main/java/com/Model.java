package com;

import javafx.util.Pair;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class is from MVC architecture.
 */
public class Model {

    /**
     * the main object
     */
    ReadFile readFile;
    Searcher searcher;

    /**
     * start the indexing transaction
     *
     * @param corpusPath    - corpus path
     * @param stopWordsPath - stop-words file's path
     * @param postingsPath  - output path for the postings file
     * @param ifStem        - if indexing should be with/without stemming
     */
    public void startIndexing(String corpusPath, String stopWordsPath, String postingsPath, boolean ifStem) {
        readFile = new ReadFile(corpusPath, stopWordsPath, postingsPath, ifStem);
        readFile.readFiles();
    }

    /**
     * reset all memory and file
     */
    public void reset() {
        readFile.parser.indexer.reset();
    }

    /**
     * @return the computed dictionary
     */
    public Map<String, String> getDictionary() {
        return new TreeMap<>(MapSaver.loadMap(readFile.parser.indexer.d_path + "\\dicTF"));
    }

    /**
     * initiate searcher
     *
     * @param postings_dir - postings directory path
     */
    public void initSearch(String postings_dir) {
        searcher = new Searcher(postings_dir);
    }

    /**
     * @param query      - a single string query
     * @param ifStem     - if the search is with stemming
     * @param ifSemantic - if the search is with semantic treatment
     * @param cities     - the cities the document should have at their "city" parameter. cities==null=>every city possible
     * @param languages  - the languages the document should have at their "language" parameter. languages==null=>every language possible
     * @return most relevant(50 or less) documents to the query with the given filters
     */
    public Map<String, List<Pair<String, String[]>>> searchByQuery(String query, boolean ifStem, boolean ifSemantic, HashSet<String> cities, HashSet<String> languages) {
        Map<String, List<Pair<String, String[]>>> ans = searcher.search(query, ifStem, ifSemantic, cities, languages);
        return ans;
    }

    /**
     * @param path       - the path to the queries file
     * @param ifStem     - if the search is with stemming
     * @param ifSemantic - if the search is with semantic treatment
     * @param cities     - the cities the document should have at their "city" parameter. cities==null=>every city possible
     * @param languages  - the languages the document should have at their "language" parameter. languages==null=>every language possible
     * @return most relevant(50 or less) documents to each query in the queries file with the given filters
     */
    public Map<String, List<Pair<String, String[]>>> searchByQuery_File(Path path, boolean ifStem, boolean ifSemantic, HashSet<String> cities, HashSet<String> languages) {
        Map<String, List<Pair<String, String[]>>> ans = searcher.search(path, ifStem, ifSemantic, cities, languages);
        return ans;
    }

    /**
     * this function writes the results of the retrieval to the output file in the required format for the TRECEVAL program
     *
     * @param ans  - retrieval results
     * @param file - output file
     */
    public void saveQueryOutput(Map<String, List<Pair<String, String[]>>> ans, File file) {
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter br = null;
        try {
            br = new BufferedWriter(new FileWriter(file, true));
            for (Map.Entry<String, List<Pair<String, String[]>>> entry : ans.entrySet()) {
                for (Pair<String, String[]> doc : entry.getValue()) {
                    try {
                        br.write(entry.getKey() + " " + "0 " + doc.getKey() + " 1 42.38 mt\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            br.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Close.close(br);
        }


    }
}
