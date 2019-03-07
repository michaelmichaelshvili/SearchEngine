package com;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * This class responsible of save and load map in properties file
 */
public class MapSaver {

    /**
     * save the map to path
     * @param map - the map to save
     * @param path - to tis path
     */
    public static void saveMap(Map<String, Object> map, String path) {
        path = path + ".properties";
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Properties properties = new Properties();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            properties.put(entry.getKey(), entry.getValue().toString());
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(path);
            if (properties.size() != 0)
                properties.store(fout, null);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * load map from path to memory
     * @param path - file save there
     * @return the map that saved
     */
    public static Map<String, String> loadMap(String path) {
        Properties properties = new Properties();
        InputStream input = null;
        Map<String, String> mapOfProperties = null;
        try {
            input = new FileInputStream(path + ".properties");
            properties.load(input);

            Stream<Map.Entry<Object, Object>> stream = properties.entrySet().stream();
            mapOfProperties = stream.collect(Collectors.toMap(
                    e -> String.valueOf(e.getKey()),
                    e -> String.valueOf(e.getValue())));

        } catch (IOException e) {
//            e.printStackTrace();
        }finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return mapOfProperties;
    }
}
