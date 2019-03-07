package com;

//import org.json.JSONArray;
//import org.json.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.*;

public class IndexerTest {

    @org.junit.Test
    public void intToBytes() {
        JsonElement json = null;
        JsonParser parser = new JsonParser();
        try {
            json = parser.parse(new FileReader(new File(ClassLoader.getSystemResource("sample/json.json").getPath())));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
//        Object object = null;
//        try {
//            object = parser
//                    .parse(new FileReader(ClassLoader.getSystemResource("json.json").getFile()));
//        } catch (IOException | ParseException e) {
//            e.printStackTrace();
//        }
//        JSONObject json = (JSONObject)object;
//        JSONArray jsonArray = (JSONArray) json.get("capital");
        System.out.println();


        /*String path = "https://restcountries.eu/rest/v2/capital/";
        URL url = null;
        String ans = "";
        try {
            url = new URL(path +"" );
            URLConnection con = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String data = bufferedReader.readLine();
            String state = data.substring(data.indexOf("{\"name\":") + 9, data.indexOf("topLevelDomain") - 3);//In this place we have the data of state.
            String currency = data.substring(data.indexOf("\"currencies\":[{\"code\":") + 23, data.indexOf("\"currencies\":[{\"code\":") + 26);//In this place we have the data of currency.
            String population = data.substring(data.indexOf("\"population\":") + 13, data.indexOf(",\"latlng\""));//In this place we have the data of population.
            population = Parse.parseNumber(population);// We need to return the population according to parse rule.
            ans = new StringBuilder("[").append(state).append(",").append(currency).append(",").append(population).append("]").toString();
        } catch (MalformedURLException e) {
            return "[*,*,*]";
        } catch (FileNotFoundException e) {
            return "[*,*,*]";
        } catch (IOException e) {
            return "[*,*,*]";
        }
        return ans;*/
    }
}