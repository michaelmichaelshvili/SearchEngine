package com;

import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class has the responsibility to get the corpus and splite the file into documents.
 */
public class ReadFile {
    /**
     * list of files
     */
    File[] files_list = null;
    Parse parser;
    /**
     * pool of thread that rin read function
     */
    private ExecutorService pool;
    /**
     * use to sync the read file
     */
    private Object syncObject;
    /**
     * nunber if reader that done
     */
    private static AtomicInteger count = new AtomicInteger(0);

    /**
     * c'tor
     *
     * @param corpusPath    - the path of corpus with files
     * @param stopWordsPath - the path for stop words
     * @param postingOut    - the path to directory we wrute the posting files.
     * @param ifStem        - if to do stem
     */
    public ReadFile(String corpusPath, String stopWordsPath, String postingOut, boolean ifStem) {
        File corpus = new File(corpusPath);
        files_list = corpus.listFiles();
        parser = new Parse(corpusPath, stopWordsPath, postingOut, ifStem);
        pool = Executors.newFixedThreadPool(8);
    }

    /**
     * This function  read the file  by send each file to thread
     */
    public void readFiles() {
        syncObject = new Object();
        count.addAndGet(files_list.length);
        for (File file : this.files_list) {
            pool.execute(new Reader(file));
        }
        synchronized (syncObject) {
            try {
                syncObject.wait();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {//when all threads are done
                parser.indexer.writeRestRecords();//write the rest of documents
                parser.indexer.sortFiles();
                pool.shutdown();
                parser.parsers_pool.shutdown();
            }
        }
    }

    /**
     * This class is thread that get file, split hum to documents by JSOUP and send them to parse thread
     * All the jsoup code will esplain in the report.
     */
    class Reader implements Runnable {

        File file;

        public Reader(File file) {
            this.file = file;
        }

        @Override
        public void run() {
            read(file);
        }

        public void read(File file) {
            Document document = null;
            try {
                document = Jsoup.parse(new String(Files.readAllBytes(file.listFiles()[0].toPath())));
            } catch (IOException e) {
                e.printStackTrace();
            }
            Elements docElements = document.getElementsByTag("DOC");//split by DOC tag
            document = null;
            cDocument[] docToParse = new cDocument[docElements.size()];
            int placeInDoc = 0;
            for (Element element : docElements) {
                Elements IDElement = element.getElementsByTag("DOCNO");
                Elements TitleElement = element.getElementsByTag("TI");

                Elements TextElement = element.getElementsByTag("TEXT");
                Elements fElements = element.getElementsByTag("F");
                String city = "";
                String language = "";
                for (Element fElement : fElements) {
                    if (fElement.attr("P").equals("104")) {//city
                        city = fElement.text();
                        if (city.length() > 0 && Character.isLetter(city.charAt(0)))
                            city = city.split(" ")[0].toUpperCase();
                        else
                            city = "";
                    } else if (fElement.attr("P").equals("105")) {//language
                        language = fElement.text().split(" ")[0];
                        if (!(!language.equals("") && !Character.isDigit(language.charAt(0)))) {
                            language = "";
                        }
                    }
                }
                String ID = IDElement.text();
                String title = TitleElement.text();
                String text = TextElement.text();
                cDocument cDoc = new cDocument(ID, title, text);
                cDoc.city = city;
                cDoc.language = language;
                docToParse[placeInDoc++] = cDoc;
            }
            docElements.clear();
            docElements = null;
            parser.parse(docToParse);
            docToParse = null;
            count.getAndDecrement();
            if (count.get() == 0) {
                synchronized (syncObject) {
                    syncObject.notify();
                }
            }

        }
    }
}
