package com;

import javafx.util.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Test {
    static TreeMap<String, Pair<Double[], Double>> treeMap = new TreeMap();

    public static void main(String[] args) throws IOException {
//        long start = System.nanoTime();
//        countW2Vterms();
//        System.out.println((System.nanoTime() - start) / 1000000000);
//        //145787182   155274107    146831234
//        //167235271   144065157   154268882  163886367
//        System.out.println();
        System.out.println();
    }

    public static void countW2Vterms() throws IOException {
        HashSet<String> stopWords = new HashSet<>();
        File stop = new File("C:\\Users\\micha\\OneDrive\\מסמכים\\michael\\שנה ג\\אחזור מידע\\stop_words.txt");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(stop));
            String st;
            while ((st = br.readLine()) != null) {
                stopWords.add(st.toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        String path = "C:\\Users\\micha\\OneDrive\\מסמכים\\michael\\שנה ג\\אחזור מידע\\glove.6B\\glove.6B.200d.txt";
        File file = new File(path);
        BufferedReader bf = Files.newBufferedReader(Paths.get(file.toURI()));
        String st;
        while ((st = bf.readLine()) != null) {
            String[] splitted = st.split(" ");
            String word = splitted[0].replaceAll("\\.\\.+|--+", " ").replaceAll("[\\|\"+&^:\t*!\\\\@#,=`~;)(\\?><}{_\\[\\]]", "").replaceAll("n't|'(s|t|mon|d|ll|m|ve|re)", "").replaceAll("'", "");
            if (!stopWords.contains(word) && !word.equals("") && !word.equals(" "))
                treeMap.put(word, getVectorAsDoubles(splitted));
        }
        bf.close();

        System.out.println("b");
        Iterator<Map.Entry<String, Pair<Double[], Double>>> it = treeMap.entrySet().iterator();
        Map<String, Object> termToTenFirst = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(8);

        while (it.hasNext()) {
            Map.Entry<String, Pair<Double[], Double>> en1 = it.next();
            pool.execute(() -> {
                TreeMap<Double, String> first10 = new TreeMap<>();
                Iterator<Map.Entry<String, Pair<Double[], Double>>> it2 = treeMap.entrySet().iterator();
                while (it2.hasNext()) {
                    Map.Entry<String, Pair<Double[], Double>> en2 = it2.next();
                    if (en1 == en2)
                        continue;
                    Double cosine = cosineSimilarity(treeMap.get(en1.getKey()), treeMap.get(en2.getKey()));
                    if (first10.size() == 5) {
                        if (cosine > first10.firstKey()) {
                            first10.remove(first10.firstKey());
                            first10.put(cosine, en2.getKey());
                        }
                    } else
                        first10.put(cosine, en2.getKey());

                }
                termToTenFirst.put(en1.getKey(), Arrays.toString(first10.values().toArray()));
                System.out.println(en1.getKey());
            });
        }
        pool.shutdown();
        try {
            boolean flag = false;
            while (!flag) {
                flag = pool.awaitTermination(500, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MapSaver.saveMap(termToTenFirst, "C:\\Users\\micha\\OneDrive\\מסמכים\\michael\\שנה ג\\אחזור מידע\\glove.6B\\termtoterm");
    }

    public static Pair<Double[], Double> getVectorAsDoubles(String[] vectorAsplit) {
        Double[] vectorAdouble = new Double[vectorAsplit.length - 1];
        double normA = 0.0;
        for (int i = 1; i < vectorAsplit.length; i++) {
            vectorAdouble[i - 1] = Double.parseDouble(vectorAsplit[i]);
            normA += Math.pow(vectorAdouble[i - 1], 2);
        }
        return new Pair<>(vectorAdouble, Math.sqrt(normA));
    }

    public static double cosineSimilarity(Pair<Double[], Double> vectorA, Pair<Double[], Double> vectorB) {
        double dotProduct = 0.0;
        Double[] vectorAarr = vectorA.getKey();
        Double[] vectorBarr = vectorB.getKey();
        for (int i = 0; i < vectorA.getKey().length; i++) {
            dotProduct += vectorAarr[i] * vectorBarr[i];
        }
        return dotProduct / (vectorA.getValue() * vectorB.getValue());
    }

}