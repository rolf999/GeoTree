package com.rafal.geoTree;

import com.rafal.geoTree.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Parent root = (Parent)loader.load();
        MainController controller = (MainController)loader.getController();
        controller.setStage(primaryStage);
        primaryStage.setTitle("GeoTree");
        primaryStage.setScene(new Scene(root, 841, 486));
        primaryStage.setResizable(false);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
