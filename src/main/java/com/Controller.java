package com;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.util.Pair;
import org.controlsfx.control.CheckListView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class Controller {

    private Model model = new Model();
    private String lastPath;
    public Map<String, String> currentDictionary;

    //fxml bonded variables
    public Button fileChooser_postings_in;
    public TextField text_postings_in;
    public TextField text_queries_path;
    public Button fileChooser_queries_file;
    public Button button_search_queries_file;
    public Button button_search_query;
    public CheckListView<String> checkListView_cities;
    public Button fileChooser_stop_words;
    public Button fileChooser_postings_out;
    public Button fileChooser_corpus;
    public Button button_reset;
    public Button button_showDictionary;
    public Button button_loadDictionary;
    public TextField text_stop_words;
    public TextField text_postings_out;
    public TextField text_corpus;
    public TextField text_query;
    public CheckBox checkBox_stemming_IN;
    public CheckBox checkBox_stemming_Q;
    public CheckBox checkBox_semantic;
    public GridPane data;
    public CheckListView<String> checkListView_languages;
    //

    @FXML
    public void initialize() {

        button_reset.setDisable(true);
        button_loadDictionary.setDisable(true);
        button_showDictionary.setDisable(true);
    }

    /**
     * This function we choose the stop words file
     */
    public void choose_stop_words_file() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(fileChooser_stop_words.getScene().getWindow());
        if (file == null)
            return;
        text_stop_words.setText(file.getPath());
    }

    /**
     * This function we choose the directory we write to ut the postings files
     */
    public void choose_postings_file_out() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File file = directoryChooser.showDialog(fileChooser_postings_out.getScene().getWindow());
        if (file == null)
            return;
        text_postings_out.setText(file.getPath());
    }

    /**
     * This function we choose the corpus directory
     */
    public void choose_corpus() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File file = directoryChooser.showDialog(fileChooser_corpus.getScene().getWindow());
        if (file == null)
            return;
        text_corpus.setText(file.getPath());
    }

    /**
     * This function start the indexing process
     */
    public void createInvertedIndex() {
        if (text_corpus.getText().equals("") || text_postings_out.getText().equals("") || text_stop_words.getText().equals("")) {//check that all of fields are not empty
            showAlert(Alert.AlertType.ERROR, "Please fill all paths");
        } else {
//            progressBar.setProgress(0);
//            progressBar.setVisible(true);
            if (!new File(text_corpus.getText()).exists()) {
                showAlert(Alert.AlertType.ERROR, "corpus text: illegal path");
                return;
            }
            if (!new File(text_stop_words.getText()).exists()) {
                showAlert(Alert.AlertType.ERROR, "stop_words text: illegal path");
                return;
            }
            if (!new File(text_postings_out.getText()).exists()) {
                showAlert(Alert.AlertType.ERROR, "postings text: illegal path");
                return;
            }
            lastPath = text_postings_out.getText();//save the last path of the last time we save the dictionary
            Stage waitStage = raiseWaitPage();
            Thread indexThread = new Thread(() -> {
                long startTime = System.nanoTime();//start to calculate how much the the process takes
                model.startIndexing(text_corpus.getText(), text_stop_words.getText(), text_postings_out.getText(), checkBox_stemming_IN.isSelected());
                long CreateIndexTime = (System.nanoTime() - startTime) / 1000000000;
                button_reset.setDisable(false);//after indexing w can reset the files
                button_loadDictionary.setDisable(false);
                button_showDictionary.setDisable(false);
                int numberOfindexDoc = model.readFile.parser.indexer.docAndexed.get();
                int uniqueTerm = model.readFile.parser.indexer.uniqueTerm.get();
                StringBuilder showText = new StringBuilder();
                showText.append("The numbers of documents indexed: ").append(numberOfindexDoc).append("\n")
                        .append("The number of unique terms: ").append(uniqueTerm).append("\n").append("The time is takes: ").append(CreateIndexTime).append(" sec");
                model.initSearch(lastPath + "\\" + (checkBox_stemming_IN.isSelected() ? "stem" : "nostem"));
                for (String checkedItem : checkListView_cities.getCheckModel().getCheckedItems()) {
                    checkListView_cities.getItems().remove(checkedItem);
                }
                checkListView_cities.getItems().clear();
                for (String checkedItem : checkListView_languages.getCheckModel().getCheckedItems()) {
                    checkListView_languages.getItems().remove(checkedItem);
                }
                checkListView_languages.getItems().clear();
                checkBox_stemming_Q.setSelected(checkBox_stemming_IN.isSelected());
                for (String city : model.searcher.cities) {//show the city
                    checkListView_cities.getItems().add(city);
                }
                for (String language : model.searcher.languages) {
                    checkListView_languages.getItems().add(language);
                }
                setDisableToFalse();
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, showText.toString()));
                Platform.runLater(waitStage::close);
            });
            indexThread.start();
        }
    }

    /**
     * generic function to show alert
     *
     * @param type - thr type of the alert
     * @param text - the context of the alert
     */
    private void showAlert(Alert.AlertType type, String text) {
        Alert alert = new Alert(type);
        alert.setContentText(text);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.showAndWait().get().getText();
    }

    /**
     * loads dictionary to memory
     */
    public void loadDictionary() {
        if (lastPath != null) {
            currentDictionary = model.getDictionary();
            showAlert(Alert.AlertType.INFORMATION, "done loading");
        } else
            showAlert(Alert.AlertType.ERROR, "You need to start indexing before you can load dictionary.");
    }

    /**
     * show the dictionay that loaded from the last path
     */
    public void showDictionary() {
        if (currentDictionary == null) {
            showAlert(Alert.AlertType.ERROR, "you need to load the dictionary before");
            return;
        }
        Stage stage = new Stage();
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("icon.png")));
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.setTitle("Dictionary");
        stage.initModality(Modality.APPLICATION_MODAL);
        ScrollPane scrollPane = new ScrollPane();
        TableView<Pair<String, String>> dictionary = new TableView<>();
        dictionary.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn term = new TableColumn<>("Term");
        TableColumn cf = new TableColumn<>("Cf");

        term.setCellValueFactory(new PropertyValueFactory<Pair<String, String>, String>("key"));
        cf.setCellValueFactory(new PropertyValueFactory<Pair<String, String>, Button>("value"));

        for (Map.Entry<String, String> entry : currentDictionary.entrySet()) {
            dictionary.getItems().add(new Pair<>(entry.getKey(), entry.getValue()));
        }

        dictionary.getColumns().addAll(term, cf);
        scrollPane.setContent(dictionary);
        dictionary.setPrefHeight(600);
        Scene scene = new Scene(scrollPane, dictionary.getMinWidth(), dictionary.getPrefHeight());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * reset the output dictionary and map
     */
    public void reset() {
        model.reset();
//        ProgressBar progressBar = new ProgressBar(0);
        button_reset.setDisable(true);
        button_loadDictionary.setDisable(true);
        button_showDictionary.setDisable(true);
        checkListView_languages.getItems().clear();
        checkListView_languages.setDisable(true);
        showAlert(Alert.AlertType.INFORMATION, "done reset");
//        progressBar.setProgress();
    }

    /**
     * choose the postings file the user want to load
     */
    public void choose_postings_file_in() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        showAlert(Alert.AlertType.WARNING, "please choose \"stem\" or \"nostem\" directory");
        File file = directoryChooser.showDialog(fileChooser_postings_in.getScene().getWindow());
        if (file == null)
            return;
        if (!file.getName().endsWith("nostem") && !file.getName().endsWith("stem"))
            showAlert(Alert.AlertType.ERROR, "chosen directory should be \"stem\" or \"nostem\" directory");
        else
            text_postings_in.setText(file.getPath());
    }

    /**
     * load part of the postings file from the path in the "text_postings_in" text field to the memory
     */
    public void load_postings_file() {
        Stage waitStage = raiseWaitPage();

        Thread initThread = new Thread(() -> {
            model.initSearch(text_postings_in.getText());
            for (String checkedItem : checkListView_cities.getCheckModel().getCheckedItems()) {
                checkListView_cities.getItems().remove(checkedItem);
            }
            checkListView_cities.getItems().clear();

            for (String checkedItem : checkListView_languages.getCheckModel().getCheckedItems()) {
                checkListView_languages.getItems().remove(checkedItem);
            }
            checkListView_languages.getItems().clear();

            for (String city : model.searcher.cities) {//show the city
                checkListView_cities.getItems().add(city);
            }
            for (String language : model.searcher.languages) {
                checkListView_languages.getItems().add(language);
            }
            checkBox_stemming_Q.setSelected(!text_postings_in.getText().endsWith("nostem"));
            setDisableToFalse();
            Platform.runLater(waitStage::close);
        });
        initThread.start();
    }

    /**
     * choose the queries file path
     */
    public void open_fileChooser_queries_file() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(fileChooser_queries_file.getScene().getWindow());
        if (file == null)
            return;
        text_queries_path.setText(file.getPath());
    }

    /**
     * @return a TableView compatible to view single query results.
     * the table columns are the document name and a button to see the document entities
     */
    private TableView<Pair<String, String[]>> getQueryTable() {
        TableView<Pair<String, String[]>> queryTable = new TableView<>();
        queryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Pair<String, String[]>, String> docColumn = new TableColumn<>("document");
        docColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getKey()));

        TableColumn<Pair<String, String[]>, String> seeMore_buttons = new TableColumn<>();//Button

        seeMore_buttons.setCellFactory(param -> new TableCell<Pair<String, String[]>, String>() {

            final Button btn = new Button("see entities");

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    btn.setOnAction(event -> {
                        Pair<String, String[]> doc = getTableView().getItems().get(getIndex());
                        Stage stage = new Stage();
                        stage.setAlwaysOnTop(false);
                        stage.setResizable(false);
                        stage.setTitle("document " + doc + " entities");
                        stage.initModality(Modality.APPLICATION_MODAL);
                        ScrollPane scrollPane = new ScrollPane();
                        TableView<String> queryTable = new TableView<>();
                        TableColumn<String, String> entities = new TableColumn<>("entity");
                        entities.setCellValueFactory(param1 -> new SimpleStringProperty(param1.getValue()));
                        queryTable.getColumns().add(entities);
                        queryTable.getItems().addAll(doc.getValue());
                        scrollPane.setContent(queryTable);
                        Scene scene = new Scene(scrollPane);
                        stage.setScene(scene);
                        stage.show();
                    });
                    setGraphic(btn);
                    setText(null);
                }
            }
        });

        queryTable.getColumns().addAll(docColumn, seeMore_buttons);
        return queryTable;
    }

    /**
     * search the query from the user input option
     */
    public void search_query() {
        searchQueryByFunction(QUERY_TYPE.string);
    }

    /**
     * search the queries from the file input option
     */
    public void search_queries_file() {
        searchQueryByFunction(QUERY_TYPE.file);
    }

    /**
     * enum to make query search generic
     */
    private enum QUERY_TYPE {
        string, file
    }

    /**
     * generic function for both query search options
     *
     * @param query_type- the type of the function to apply
     */
    private void searchQueryByFunction(QUERY_TYPE query_type) {
        ObservableList<String> cities_Chosen = checkListView_cities.getCheckModel().getCheckedItems();
        HashSet<String> cities = new HashSet<>(cities_Chosen);
        ObservableList<String> languages_Chosen = checkListView_languages.getCheckModel().getCheckedItems();
        HashSet<String> languages = new HashSet<>(languages_Chosen);

        Stage waitStage = raiseWaitPage();
        Thread searchThread = new Thread(() -> {

            TableView<Map.Entry<String, List<Pair<String, String[]>>>> tableView = new TableView<>();
            tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            TableColumn<Map.Entry<String, List<Pair<String, String[]>>>, String> queryNum = new TableColumn<>("query num");
            queryNum.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getKey()));

            TableColumn<Map.Entry<String, List<Pair<String, String[]>>>, String> seeMore_buttons = new TableColumn<>();//Button

            seeMore_buttons.setCellFactory(param -> new TableCell<Map.Entry<String, List<Pair<String, String[]>>>, String>() {

                final Button btn = new Button("see more");

                @Override
                public void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        btn.setOnAction(event -> {
                            Map.Entry<String, List<Pair<String, String[]>>> query = getTableView().getItems().get(getIndex());
                            Stage stage = new Stage();
                            stage.setAlwaysOnTop(false);
                            stage.setResizable(false);
                            stage.setTitle("query " + query.getKey());
                            stage.initModality(Modality.APPLICATION_MODAL);
                            ScrollPane scrollPane = new ScrollPane();
                            TableView<Pair<String, String[]>> queryTable = getQueryTable();
                            queryTable.getItems().addAll(query.getValue());
                            scrollPane.setContent(queryTable);
                            Scene scene = new Scene(scrollPane);
                            stage.setScene(scene);
                            stage.show();
                        });
                        setGraphic(btn);
                        setText(null);
                    }
                }
            });

            tableView.getColumns().addAll(queryNum, seeMore_buttons);
            Map<String, List<Pair<String, String[]>>> results = query_type.equals(QUERY_TYPE.file) ? model.searchByQuery_File(Paths.get(text_queries_path.getText()), checkBox_stemming_Q.isSelected(), checkBox_semantic.isSelected(), cities, languages) :
                    (query_type.equals(QUERY_TYPE.string) ? model.searchByQuery(text_query.getText(), checkBox_stemming_Q.isSelected(), checkBox_semantic.isSelected(), cities, languages) : null);
            tableView.getItems().addAll(results.entrySet());
            Platform.runLater(() -> {
                Stage stage = new Stage();

                stage.setResizable(true);
                VBox vBox = new VBox();
                Button button = new Button("save queries results");
                button.setOnAction(event -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setInitialFileName("queries_results");
                    fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("text", "*.txt"));
                    File file = fileChooser.showSaveDialog(button.getScene().getWindow());
                    if (file == null)
                        return;
                    model.saveQueryOutput(results, file);
                });
                button.setPrefWidth(vBox.getMaxWidth());
                button.setMaxWidth(vBox.getMaxWidth());
                vBox.getChildren().addAll(button, tableView);
                stage.setScene(new Scene(vBox));
                stage.setTitle(results.size() + " query's results");
                stage.initModality(Modality.APPLICATION_MODAL);
//                Platform.runLater(waitStage::close);
                waitStage.close();
                stage.show();
            });
        });
        searchThread.start();
    }

    /**
     * enables all search buttons
     */
    private void setDisableToFalse() {
        button_search_queries_file.setDisable(false);
        button_search_query.setDisable(false);
        text_query.setDisable(false);
        text_queries_path.setDisable(false);
        fileChooser_queries_file.setDisable(false);
//        checkBox_stemming_Q.setDisable(false);
        checkBox_semantic.setDisable(false);
        checkListView_cities.setDisable(false);
        checkListView_languages.setDisable(false);

    }

    /**
     * @return a new waitPage Stage
     */
    private Stage raiseWaitPage() {
        Stage waitStage = new Stage(StageStyle.UNDECORATED);
        try {
            Parent waitParent = FXMLLoader.load(this.getClass().getResource("waitPage.fxml"));
            waitStage.setScene(new Scene(waitParent));
//            waitStage.getIcons().add(new Image(this.getClass().getResourceAsStream("tenor.gif")));
            waitStage.setResizable(false);
            waitStage.initModality(Modality.APPLICATION_MODAL);
//            waitStage.setAlwaysOnTop(true);
            waitStage.setOnCloseRequest(event -> event.consume());
            waitStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return waitStage;
    }
}
