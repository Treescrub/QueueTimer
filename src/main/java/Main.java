import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QueueTimer
 * <p>
 * Created by Daroot Leafstorm on 12/26/2019.
 */
public class Main extends Application {

     /*
        TODO: check SWF lobby
        TODO: add icons
     */

    /*
        Start queue = [2019.12.26-21.23.24:956][419]GameFlow: Starting Quickmatch: GameSession
        Cancel queue = [2019.12.26-23.50.27:083][918]GameFlow: Canceling Matchmaking
        Join lobby = [2019.12.26-21.23.40:442][349]GameFlow: Joining sesion : GameSession
        Leave lobby = [2019.12.27-18.15.25:146][978]GameFlow: Verbose: vvv OnLeavingOnlineMultiplayer vvv
        Game start = [2019.12.27-19.09.26:919][532]GameFlow: UDBDGameInstance::StartOfferingSequence

        Part of queue start
            GameFlow: Canceling Find Sessions, then Quickmatch Find Sessions
     */

    private static final SimpleDateFormat logFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss:SSS");
    private static final SimpleDateFormat displayFormat = new SimpleDateFormat("mm:ss");

    private static final String LOG_PREFIX = "^\\[(.{23})]\\[.{3}]G.{9}([\\w\\s:]+)$";
    private static final Pattern LOG_PREFIX_PATTERN = Pattern.compile(LOG_PREFIX);
    private static final Pattern START_QUEUE = Pattern.compile("^St.{19}GameSession$");
    private static final Pattern CANCEL_QUEUE = Pattern.compile("^Can.{11}hmaking$");
    private static final Pattern JOIN_LOBBY = Pattern.compile("^J.{27}$");
    private static final Pattern LEAVE_LOBBY = Pattern.compile("^Verbose: v.{33}$");
    private static final Pattern START_GAME = Pattern.compile("^.{18}StartOfferingSequence$");

    private static Date queueStart = null;
    private static Date lobbyStart = null;

    private static Controller controller;

    private static long queueTime;
    private static long lobbyTime;

    private static StringProperty queueTimeProperty;
    private static StringProperty lobbyTimeProperty;

    static StringProperty getQueueTimeProperty() {
        return queueTimeProperty;
    }

    static StringProperty getLobbyTimeProperty() {
        return lobbyTimeProperty;
    }

    public static void main(String[] args) {
        launch(args);
    }

    private static void readLog(File file, long skip) {
        long startTime = System.nanoTime();
        try {
            long test = System.nanoTime();
            BufferedReader in = new BufferedReader(new FileReader(file));
            System.out.println("BR read: " + (System.nanoTime() - test) / 1000000.0 + "ms");
            System.out.println("skipped: " + in.skip(skip));

            long iterationTime = System.nanoTime();
            long iterationTotal = 0;

            long iterations = 0;

            String line;
            while ((line = in.readLine()) != null) {

                iterationTotal += System.nanoTime() - iterationTime;
                iterationTime = System.nanoTime();
                iterations++;

                Matcher prefix = LOG_PREFIX_PATTERN.matcher(line);
                if(!prefix.matches()){
                    continue;
                }

                line = prefix.group(2);

                Matcher startQueue = START_QUEUE.matcher(line);
                if (startQueue.matches()) {
                    System.out.println("startQueue: " + line);
                    queueStart = logFormat.parse(prefix.group(1));
                    lobbyStart = null;
                    continue;
                }

                Matcher cancelQueue = CANCEL_QUEUE.matcher(line);
                if (cancelQueue.matches()) {
                    if(queueStart != null) {
                        System.out.println("cancelQueue: " + line);
                        long time = logFormat.parse(prefix.group(1)).getTime() - queueStart.getTime();
                        queueTime += time;
                        //Platform.runLater(() -> controller.addQueueTime(time));
                        queueStart = null;
                    }
                    continue;
                }

                Matcher joinLobby = JOIN_LOBBY.matcher(line);
                if (joinLobby.matches()) {
                    System.out.println("joinLobby: " + line);
                    long time = logFormat.parse(prefix.group(1)).getTime() - queueStart.getTime();
                    queueTime += time;
//                    Platform.runLater(() -> controller.addQueueTime(time));
                    lobbyStart = logFormat.parse(prefix.group(1));
                    queueStart = null;
                    continue;
                }

                Matcher leaveLobby = LEAVE_LOBBY.matcher(line);
                if (leaveLobby.matches()) {
                    if(lobbyStart != null) {
                        System.out.println("leaveLobby: " + line);
                        long time = logFormat.parse(prefix.group(1)).getTime() - lobbyStart.getTime();
                        lobbyTime += time;
//                        Platform.runLater(() -> controller.addLobbyTime(time));
                        lobbyStart = null;
                    }
                    continue;
                }

                Matcher startGame = START_GAME.matcher(line);
                if(startGame.matches()){
                    if(lobbyStart != null){
                        System.out.println("startGame: " + line);
                        long time = logFormat.parse(prefix.group(1)).getTime() - lobbyStart.getTime();
                        lobbyTime += time;
                        lobbyStart = null;
                    }
                }
            }
            in.close();

            System.out.println("iterations: " + iterations);
            System.out.println("average iteration time: " + (iterationTotal / iterations / 1000000.0) + "ms");
            System.out.println("total iteration time: " + iterationTotal / 1000000.0 + "ms");

            System.out.println("queue time: " + TimeUnit.MILLISECONDS.toSeconds(queueTime));
            System.out.println("lobby time: " + TimeUnit.MILLISECONDS.toSeconds(lobbyTime));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        System.out.println("runtime: " + (System.nanoTime() - startTime) / 1000000.0 + "ms");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        queueTimeProperty = new SimpleStringProperty("");
        lobbyTimeProperty = new SimpleStringProperty("");

        Thread propertyUpdater = new Thread(() -> {
            while(true){
                if(queueStart != null) {
                    String time = displayFormat.format(queueTime + Calendar.getInstance().getTime().getTime() - queueStart.getTime());
                    Platform.runLater(() -> queueTimeProperty.setValue(time));
                } else {
                    String time = displayFormat.format(queueTime);
                    Platform.runLater(() -> queueTimeProperty.setValue(time));
                }
                if(lobbyStart != null) {
                    String time = displayFormat.format(lobbyTime + Calendar.getInstance().getTime().getTime() - lobbyStart.getTime());
                    Platform.runLater(() -> lobbyTimeProperty.setValue(time));
                } else {
                    String time = displayFormat.format(lobbyTime);
                    Platform.runLater(() -> lobbyTimeProperty.setValue(time));
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        propertyUpdater.start();

        Thread processCheck = new Thread(() -> {
            while(true){
                try {
                    String line;
                    StringBuilder pidInfo = new StringBuilder();

                    Process p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");

                    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

                    while ((line = input.readLine()) != null) {
//                        System.out.println(line);
                        pidInfo.append(line);
                    }

                    input.close();

                    if (!pidInfo.toString().contains("DeadByDaylight-Win")) {
                        queueStart = null;
                        lobbyStart = null;
                    }

                    Thread.sleep(500);
                } catch (IOException | InterruptedException e){
                    e.printStackTrace();
                }
            }
        });
        processCheck.start();

        Thread thread = new Thread(() -> {
            File log = new File(System.getProperty("user.home") + "\\AppData\\Local\\DeadByDaylight\\Saved\\Logs\\DeadByDaylight.log");
//            File log = new File(System.getProperty("user.home") + "\\AppData\\Local\\DeadByDaylight\\Saved\\Logs\\DeadByDaylight-backup-2019.12.27-01.20.56.log");

            long fileLength = log.length();
            readLog(log, 0);

            while (true) {
                if(!log.exists() || !log.canRead()){
                    continue;
                }
                if (fileLength < log.length()) {
                    System.out.println("file size increased, reading new data");
                    readLog(log, fileLength);
                }
                if(log.length() < fileLength){ // game was restarted or file was changed
                    System.out.println("file size decreased, restarting at beginning of log");
                    queueStart = null;
                    lobbyStart = null;
                    readLog(log, 0);
                }
                fileLength = log.length();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();


        FXMLLoader loader = new FXMLLoader(getClass().getResource("application.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Queue Timer");
        Scene scene = new Scene(root);
//        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("DeadByDaylight.ico")));
        primaryStage.setScene(scene);
        primaryStage.show();

        controller = loader.getController();
        controller.initialize(root.getScene());
    }
}
