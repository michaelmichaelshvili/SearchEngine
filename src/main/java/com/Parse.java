package com;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * This class has the responsibility to do the parse.
 * We got array of docs from one file and pase each one in thread and wait until and.
 */
public class Parse {

    /**
     * Thread pool to threads that go the parse
     */
    ExecutorService parsers_pool = Executors.newCachedThreadPool();
    /**
     * The stopWords
     */
    static HashSet<String> stopWords = new HashSet<>();
    /**
     * if do stem to term
     */
    private boolean ifStem;

    Indexer indexer;

    /**
     * @param corpusName      - the path that all the files there are
     * @param stopWordsPath   - the path to stopwords
     * @param outputDirectory - the directory to write the posting files
     * @param ifStem          - if do stemming
     */
    public Parse(String corpusName, String stopWordsPath, String outputDirectory, boolean ifStem) {
        this.ifStem = ifStem;
        StringBuilder name = new StringBuilder(corpusName);
        indexer = new Indexer(outputDirectory, ifStem);
        //read the stopwords to hash set
        File file = new File(stopWordsPath);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String st;
            while ((st = br.readLine()) != null)
                stopWords.add(st.toLowerCase());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            copyFileUsingStream(file,new File(outputDirectory+"\\"+(ifStem ? "stem" : "nostem")+"\\"+file.getName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            Close.close(br);
        }
        stopWords.remove("between");
        stopWords.remove("may");
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    /**
     * Do parse to document by thread and send him to andex file.
     *
     * @param docs - array of docs from one file
     */
    public void parse(cDocument[] docs) {
        Future<cDocument>[] futures = new Future[docs.length];
        cDocument document;
        //send each doc to thread
        for (int i = 0; i < docs.length; i++) {
            document = docs[i];
            Future<cDocument> fpd = parsers_pool.submit(new Parser(document, ifStem));
            futures[i] = fpd;
        }

        //wait for parser and send to andex.
        for (int i = 0; i < docs.length; i++) {
            try {
                indexer.andex(futures[i].get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
//        System.out.println("DONE");
    }

    /**
     * check if string is double number
     *
     * @param str - string to check
     * @return true if string is double number
     */
    public static boolean isDoubleNumber(String str) {
        try {
            double number = Double.parseDouble(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * check if string is integer number
     *
     * @param str - string to check
     * @return true if string is integer number
     */
    public static boolean isIntegernumber(String str) {
        try {
            int number = Integer.parseInt(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * check if string represent fraction number
     *
     * @param str - string to check
     * @return true if string is fraction number
     */
    public static boolean isFraction(String str) {
        if (str.contains("/")) {
            String[] splitted = str.split("/");
            if (splitted.length == 2 && isDoubleNumber(splitted[0]) && isDoubleNumber(splitted[1]))
                return true;
        }
        return false;
    }

    /**
     * do parse according to Number rules
     *
     * @param str - string to parse
     * @return the string after parse
     */
    public static String parseNumber(String... str) {
        String ans = "";
        Double strAsDouble = Double.parseDouble(str[0]);
        int shift = 0;
        if (str.length == 1) {

            String KMB = "";
            if (Math.abs(strAsDouble) >= Math.pow(10, 9)) {
                shift = 9;
                KMB = "B";
            } else if (Math.abs(strAsDouble) >= Math.pow(10, 6)) {
                shift = 6;
                KMB = "M";
            } else if (Math.abs(strAsDouble) >= Math.pow(10, 3)) {
                shift = 3;
                KMB = "K";
            }
            strAsDouble = strAsDouble / Math.pow(10, shift);
            ans = (strAsDouble % 1 == 0.0 ? strAsDouble.intValue() : strAsDouble.toString()) + KMB;
        } else {
            if (str[1].toLowerCase().equals("trillion")) {
                shift = 12;
            } else if (str[1].toLowerCase().equals("billion")) {
                shift = 9;
            } else if (str[1].toLowerCase().equals("million")) {
                shift = 6;
            } else if (str[1].toLowerCase().equals("thousand")) {
                shift = 3;
            } else if (str[1].contains("/")) {
                return str[0] + " " + str[1];
            }
            strAsDouble = strAsDouble * Math.pow(10, shift);
            ans = parseNumber(strAsDouble.toString());
        }
        return ans;
    }

    /**
     * do parse according to precent rules
     *
     * @param str - string to parse
     * @return the string after parse
     */
    public static String parsePrecent(String... str) {
        if (str[1].equals("%"))
            return str[0] + str[1];
        return str[0] + "%";
    }

    /**
     * do parse according to price rules
     *
     * @param str - string to parse
     * @return the string after parse
     */
    public static String parsePrice(String... str) {
        int shift = 0;
        Double price = 0.0;
        if (str[0].equals("$")) {
            price = Double.parseDouble(str[1]);
            if (str.length == 3) {
                if (str[2].toLowerCase().equals("trillion")) {
                    shift = 12;
                } else if (str[2].toLowerCase().equals("billion")) {
                    shift = 9;
                } else if (str[2].toLowerCase().equals("million")) {
                    shift = 6;
                }
            }
        } else {
            price = Double.parseDouble(str[0]);
            if (str.length >= 3) {
                if (str[1].toLowerCase().equals("trillion")) {
                    shift = 12;
                } else if (str[1].toLowerCase().equals("billion") || str[1].toLowerCase().equals("bn")) {
                    shift = 9;
                } else if (str[1].toLowerCase().equals("million") || str[1].toLowerCase().equals("m")) {
                    shift = 6;
                } else if (str[1].contains("/")) {
                    return str[0] + " " + str[1] + " " + str[2];
                }
            }

        }
        if (price >= Math.pow(10, 6)) {
            shift = 6;
            price /= Math.pow(10, 6);
        }
        price = price * Math.pow(10, shift) / (shift > 0 ? Math.pow(10, 6) : 1);
        return (price % 1 == 0.0 ? Integer.toString(price.intValue()) : price.toString()) + (shift > 0 ? " M " : " ") + "Dollars";
    }

    /**
     * do parse according to date rules
     * if the length us 3 so we have year, month, day. it's our rule
     *
     * @param str - string to parse
     * @return the string after parse
     */
    public static String parseDate(String... str) {
        if (str.length == 3)//YYYY-MM-DD
        {
            return str[2] + "-" + Date.DateToDateNum.get(str[1].toUpperCase()) + "-" + str[0];
        }
        if (Date.DateToDateNum.containsKey(str[0].toUpperCase())) {
            int dayOrYear = Integer.parseInt(str[1]);
            if (dayOrYear > Date.MonthToNumberOfDays.get(str[0].toUpperCase()))//YYYY-MM
                return str[1] + "-" + Date.DateToDateNum.get(str[0].toUpperCase());
            else//MM-DD
                return Date.DateToDateNum.get(str[0].toUpperCase()) + "-" + (str[1].length() == 1 ? "0" : "") + str[1];
        }
        return Date.DateToDateNum.get(str[1].toUpperCase()) + "-" + str[0];
    }

    /**
     * remove dot and slash from unneccessey place.
     *
     * @param str - string to parse
     * @return the string after parse
     */
    public static String cleanToken(String str) {
        if (str.equals(""))
            return str;
        str = str.replaceAll("/", "");
//        if (str.charAt(0) == '/' || str.charAt(0) == '\'')
//            str = str.substring(1, str.length());
        if (str.equals(""))
            return str;
        if (str.charAt(str.length() - 1) == '.' || str.charAt(str.length() - 1) == ',' || str.charAt(str.length() - 1) == '-')
            str = str.substring(0, str.length() - 1);
        return str;
    }

    public static String parceDistance(String... str) {
        if (str[1].matches("kilometer|km|kilometers")) {
            double d = Double.parseDouble(str[0]);
            d *= 1000;
            return parseNumber(String.valueOf(d)) + " " + "meter";
        } else {
            double d = Double.parseDouble(str[0]);
            return parseNumber(String.valueOf(d)) + " " + "meter";
        }
    }

    /**
     * check if string is not a speciel word from rules
     *
     * @param term - the word to check
     * @return true if is simple word
     */
    public static boolean isSimpleTerm(String term) {
        if (term.startsWith("$") || Date.MonthToNumberOfDays.containsKey(term.toUpperCase()) || Character.isDigit(term.charAt(0)) || term.toLowerCase().equals("between") || term.toLowerCase().equals("may"))
            return false;
        return true;
    }

    /**
     * This class is thread that get doc and return the document with dictionary of terms
     */
    static class Parser implements Callable<cDocument> {
        private cDocument document;
        boolean ifstem;


        Parser(cDocument document, boolean ifstem) {
            this.document = document;
            this.ifstem = ifstem;
        }

        @Override
        public cDocument call() {
            return (cDocument) parse(document, ifstem, stopWords);
        }

        public static cItem parse(cItem item, boolean ifStem, HashSet<String> stopWords) {
            boolean isDoc = item instanceof cDocument;
            String[] tokens = item.text.replaceAll("\\.\\.+|--+", " ").replaceAll("(?<=[0-9]),(?=[0-9])", "").replaceAll("[\\.][ \n\t\"]|[\\|\"+&^:\t*!\\\\@#,=`~;)(\\?><}{_\\[\\]]", " ").replaceAll("n't|'(s|t|mon|d|ll|m|ve|re)", "").replaceAll("'", "").split("\n|\\s+");
            item.text = "";//release memory
            int tokenLength = tokens.length;
            int docLenth = 0;
            String term;
            for (int i = 0; i < tokenLength; i++) {
                term = "";
                if (tokens[i].equals("") || stopWords.contains(tokens[i].toLowerCase()))//not need to save
                    continue;
                if (isSimpleTerm(tokens[i])) {
                    if (isDoc && tokens[i].toLowerCase().equals(((cDocument) item).city.toLowerCase()))//to cities index.
                        ((cDocument) item).cityPosition.add(i);
                    term = tokens[i];
                } else if (tokens[i].startsWith("$") && isDoubleNumber(tokens[i].replace("$", ""))) {//price rule
                    try {
                        String[] splitted = tokens[i].split("((?<=\\$)|(?=\\$))|\\-");
                        if (i + 1 < tokenLength && (tokens[i + 1]).matches("miliion|billion|trillion"))
                            term = parsePrice(splitted[0], splitted[1], tokens[++i]);
                        else
                            term = parsePrice(splitted[0], splitted[1]);
                    } catch (NumberFormatException ignore) {
                    }

                } else if (tokens[i].endsWith("%"))//precent rule
                    term = parsePrecent(tokens[i].split("((?<=%)|(?=%))"));
                else if (Parse.isDoubleNumber(tokens[i]))//any case contains number
                {
                    if (i + 1 < tokenLength && tokens[i + 1].matches("Dollars"))//price
                        term = parsePrice(tokens[i], (tokens[++i]));
                    else if (i + 1 < tokenLength && tokens[i + 1].toLowerCase().matches("percent|percentage"))//precent
                        term = parsePrecent(tokens[i], tokens[++i]);
                    else if (i + 1 < tokenLength && Parse.isFraction(tokens[i + 1]) && i + 2 < tokenLength && tokens[i + 2].equals("Dollars"))//price
                        term = Parse.parsePrice(tokens[i], (tokens[++i]), (tokens[++i]));
                    else if (i + 1 < tokenLength && tokens[i + 1].matches("m|bn") && i + 2 < tokenLength && tokens[i + 2].equals("Dollars"))//price
                        term = Parse.parsePrice(tokens[i], (tokens[++i]), (tokens[++i]));
                    else if (i + 3 < tokenLength && tokens[i + 1].matches("miliion|billion|trillion") && tokens[i + 2].equals("U.S") && tokens[i + 3].equals("dollars"))
                        term = Parse.parsePrice(tokens[i], (tokens[++i]), (tokens[++i]), (tokens[++i]));
                    else if (i + 1 < tokenLength && (tokens[i + 1].matches("Thousand|Million|Billion|Trillion") || Parse.isFraction(tokens[i + 1])))
                        term = Parse.parseNumber(tokens[i], (tokens[++i]));
                    else if (i + 1 < tokenLength && Date.DateToDateNum.containsKey(tokens[i + 1].toUpperCase()))//date. current is the number
                        if (i + 2 < tokenLength && isIntegernumber(tokens[i + 2]) && Integer.parseInt(tokens[i + 2]) > 1000) {
                            term = parseDate(tokens[i + 2], tokens[i + 1], tokens[i]);
                            i += 2;
                        } else
                            term = Parse.parseDate(tokens[i], (tokens[++i]));
                    else if (i + 1 < tokenLength && tokens[i + 1].matches("meter|meters|kilometer|kilometers"))
                        term = parceDistance(tokens[i], tokens[++i]);
                    else
                        term = Parse.parseNumber(tokens[i]);
                } else if (i + 1 < tokenLength && Date.DateToDateNum.containsKey(tokens[i].toUpperCase())) {//date. cuurent is the month
                    if (i + 1 < tokenLength && Parse.isIntegernumber(tokens[i + 1]))
                        term = Parse.parseDate(tokens[i], (tokens[++i]));
                    else if (tokens[i].toLowerCase().equals("may"))
                        continue;
                } else if (tokens[i].equals("between")) {//maybe is beetween rule
                    if (tokens[i].toLowerCase().equals("between") && i + 3 < tokenLength && Parse.isDoubleNumber(tokens[i + 1]) && tokens[i + 2].toLowerCase().equals("and") && Parse.isDoubleNumber(tokens[i + 3]))
                        term = tokens[i] + " " + tokens[++i] + " " + tokens[++i] + " " + tokens[++i];
                    else
                        continue;
                } else
                    term = tokens[i];
                //put the term in dictionary acoording to case
                term = cleanToken(term);
                if (stopWords.contains(term.toLowerCase()))
                    continue;
                if (!term.equals("")) {
                    if (Character.isLowerCase(term.charAt(0))) {
                        Integer df;
                        term = term.toLowerCase();
                        if ((df = item.terms.remove(term.toUpperCase())) != null) {
                            item.terms.put(term, df);
                        }
                    } else {
                        if (item.terms.containsKey(term.toLowerCase())) {
                            term = term.toLowerCase();
                        } else
                            term = term.toUpperCase();
                    }

                    item.terms.put(term, item.terms.getOrDefault(term, 0) + 1);
                    docLenth++;
                }
            }
            //save the max_tf
            try {
                if (isDoc)
                    ((cDocument) item).max_tf = Collections.max(item.terms.values());
            } catch (Exception ignore) {//if the map empty
            }
            tokens = null;
            if (isDoc) {
                ((cDocument) item).docLenth = docLenth;
            }

            //do stemming if need
            if (ifStem)
                item.stem_dictionary(new Stemmer());
            return item;
        }

    }
}

