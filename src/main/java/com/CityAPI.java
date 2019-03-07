package com;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class responsible to get data about city from API https://restcountries.eu
 * We use json to do it easily and fast.
 */
public class CityAPI {


    private Map<String, Country> capitalToCountry = new HashMap<>();//Map from capital to the country record

    public CityAPI() {
        JsonReader reader;
        Gson gson = new Gson();
        try {
            InputStream is = getClass().getResourceAsStream("json.json");
            reader = new JsonReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            reader.beginArray();
            while (reader.hasNext()) {
                Country country = gson.fromJson(reader, Country.class);
                capitalToCountry.put(country.capital, country);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getCityInfo(String city) {
        String _city = city.toLowerCase();
        _city = Character.toUpperCase(_city.charAt(0)) + _city.substring(1);
        Country country;
        if ((country = capitalToCountry.get(_city)) != null)
            return country.toString();
        return ("[*,*,*]");
    }


    /**
     * This Class represent an element in the json file
     */
    class Country {

        private String capital;

        private String name;

        private Integer population;

        private List<Currency> currencies;

        public String getCapital() {
            return capital;
        }

        public String getName() {
            return name;
        }

        public Integer getPopulation() {
            return population;
        }

        public List<Currency> getCurrencies() {
            return currencies;
        }

        @Override
        public String toString() {
            return '[' + name + ',' + currencies.get(0).code + ',' + Parse.parseNumber(population.toString()) + ']';
        }

        /**
         * This class represent how Currency looks like in the json file element
         */
        class Currency {

            private String code;
            private String name;
            private String symbol;

            public String getCode() {
                return code;
            }

            public void setCode(String code) {
                this.code = code;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getSymbol() {
                return symbol;
            }

            public void setSymbol(String symbol) {
                this.symbol = symbol;
            }
        }
    }
}
