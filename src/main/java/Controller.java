import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * QueueTimer
 * <p>
 * Created by Daroot Leafstorm on 12/26/2019.
 */
public class Controller {


    public AreaChart<Number, Number> timeChart;
    public NumberAxis xAxis;
    public NumberAxis yAxis;

    public Label queueTime;
    public Label lobbyTime;

    private static XYChart.Series queueSeries;
    private static XYChart.Series lobbySeries;

    private static int queueCount = 0;
    private static int lobbyCount = 0;
    private static ArrayList<Long> queueTimes;
    private static ArrayList<Long> lobbyTimes;

    /*public void addQueueTime(long time){
//        queueTimes.add(time);
        System.out.println("adding time to chart");
        queueSeries.getData().add(new XYChart.Data<>(queueCount, TimeUnit.MILLISECONDS.toSeconds(time)));
        queueCount++;
    }

    public void addLobbyTime(long time){
//        lobbyTimes.add(time);
        lobbySeries.getData().add(new XYChart.Data<>(lobbyCount, TimeUnit.MILLISECONDS.toSeconds(time)));
        lobbyCount++;
    }*/

    public void initialize(Scene scene){
        /*xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);

        queueSeries = new XYChart.Series();
        lobbySeries = new XYChart.Series();

        queueSeries.setName("Queue");
        lobbySeries.setName("Lobby");

        timeChart.getData().add(queueSeries);
        timeChart.getData().add(lobbySeries);*/

        scene.getWindow().setOnCloseRequest(event -> System.exit(0));

        queueTime.textProperty().bind(Main.getQueueTimeProperty());
        lobbyTime.textProperty().bind(Main.getLobbyTimeProperty());
    }
}
