package com;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Wait Page controller
 */
public class WaitPage implements Initializable{
    @FXML
    ImageView waitImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        waitImage.setImage(new Image(getClass().getResourceAsStream("tenor.gif")));
    }
}
