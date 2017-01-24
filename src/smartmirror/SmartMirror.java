/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smartmirror;

import edu.cmu.sphinx.demo.dialog.DialogDemo;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import javafx.animation.KeyValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

/**
 *
 * @author Nick, Muhaamed, Dawit, Anh, Aditya
 */
public class SmartMirror extends Application {

    public static String USED_FONT = "Proxima Nova";
    //public static String USED_FONT = "Adam";
    private final Weather weather = new Weather();
    private static final Jarvis JARVIS = new Jarvis();
    private static Thread sphinxThread;

    private final Calendar localCalendar = Calendar.getInstance(TimeZone.getDefault());
    private final String[] MONTH_STRINGS = new String[]{"January", "February", "March", "April",
        "May", "June", "July", "August", "September", "october", "November", "December"};

    private static BorderPane bpRoot;
    private static final double bpRootInsetValue = 20;
    private static boolean responseFromGoogle = false;
    private static String wrQuery = "";
    private static VBox wolframVbox, weatherVbox, leftDisplay, voiceDialogHelp;
    private static HBox helpPrompt, listeningPrompt, notListeningPrompt;
    private static Text countDown = new Text(" ");
    
    public static String command = "";
    public static int countTime;
    public static boolean newCommand = false;
    public static boolean isListening = false;
    
    public static LoadingScreen LS = new LoadingScreen();
    
    public static Scene scene;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        sphinxThread = new Thread(new DialogDemo());
        sphinxThread.start();

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        bpRoot = new BorderPane();
        bpRoot.setStyle("-fx-background-color: #000000;"); //setting bg to black
        bpRoot.setPrefSize(1200, 1920);
        bpRoot.setPadding(new Insets(bpRootInsetValue, bpRootInsetValue,
                bpRootInsetValue, bpRootInsetValue));
        scene = new Scene(bpRoot, 1200, 1920);

        //Test Buttons
        Button abort = new Button("Abort(exit)");
        abort.setOnAction((ActionEvent e) -> {
            System.exit(0);
        });
        Button loading = new Button("test(loading)");
        loading.setOnAction((ActionEvent e) -> {
            displayCenter(LS.start());
        });
        
        Button hd = new Button("tree");
        hd.setOnAction((ActionEvent e) -> {
            dismissCenter();
            hideDisplay();
            scene.setRoot(Tree.tree());
        });
        
        Button sd = new Button("show display");
        sd.setOnAction((ActionEvent e) -> {
            showDisplay();
        });

        listeningPrompt = createHelpBox("islistening");
        notListeningPrompt = createHelpBox("notlistening");
        
        helpPrompt = notListeningPrompt;
        // Current time
        Text time = new Text("--:--");
        time.setFont(Font.font(USED_FONT, 80));
        time.setFill(Color.WHITE);
        

        //Current date
        Text currentDate = new Text("dateTestText");
        currentDate.setFont(Font.font(USED_FONT, 50));
        currentDate.setFill(Color.WHITE);

        VBox tempButtons = new VBox();
        //tempButtons.getChildren().addAll(abort, hd, sd, loading);

        leftDisplay = new VBox();
        leftDisplay.getChildren().addAll(time, currentDate);
        
        //createHelpBox("isListening");

        // Start update methods
        updateTime(time);
        updateDate(currentDate);
        updateWeather(bpRoot);

        // Start sphinx listening method
        checkForSphinxCommand();

        // Current weather
        weatherVbox = weather.getWeatherVB();

        // Get help VBox
        voiceDialogHelp = VoiceDialogHelp.whatCanISay();
        voiceDialogHelp.setTranslateX(bpRoot.getWidth() / 2 - 220);

        bpRoot.setTop(tempButtons);
        bpRoot.setLeft(leftDisplay);
        bpRoot.setRight(weatherVbox);
        bpRoot.setBottom(helpPrompt);

        primaryStage.setScene(scene);
        System.out.println("testing WARNINGS!!!!!!!!!!!");
        System.out.println(" make sure to update voicedialoghelp");
        System.out.println("------If any changes are made to the dialog.gram file, make sure to "
                + "reflect that in the checkForShpinxCommand() and whatCanISay()");
        System.out.println("TODO: after 100 seconds, clear center");
        primaryStage.setFullScreen(true);
        primaryStage.show();
        
        // Need to run showHelp at start to make sure correct values are set
        showHelp();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SmartMirror.class.getName()).log(Level.SEVERE, null, ex);
        }
        hideHelp();
    }

    /**
     * This method gets the utterance from the dialogDemo and uses it to call
     * different methods, if the utterance matches the signature of a command
     *
     */
    public static void checkForSphinxCommand() {
        Timeline timeTimeline = new Timeline();
        timeTimeline.setCycleCount(Timeline.INDEFINITE);
        timeTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            // If the command is new and the mirror should current be listening, try to match the command
            if (getNewCommand() && isListening) {
                // Setting the current command to !new, b/c it has been checked
                setNewCommand(false);
                // Checks if command matches one from the grammar file
                switch (getCommand()) {
                    case "what can i say":
                        showHelp();
                        break;
                    case "go back":
                        hideHelp();
                        break;
                    // If this command is changed, make sure to update it in the DialogDemo
                    case "search wolfram alpha":
                        hideDisplay();
                        performWolframSearch();
                        break;
                    case "return home":
                        //if(!scene.getRoot().equals(bpRoot)){
//                            Tree.pane.getChildren().removeAll();
//                            Circles.pane.getChildren().removeAll();
                            Tree.stopTree();
                            Circles.stopCircle();
                            

                            scene.setRoot(bpRoot);
                        //}
                        showDisplay();
                        dismissCenter();
                        break;
                    case "show circles":
                        dismissCenter();
                        hideDisplay();
                        
                        scene.setRoot(Circles.cirlce());
                        break;
                    case "show tree":
                        dismissCenter();
                        hideDisplay();
                        
                        scene.setRoot(Tree.tree());
                        break;
                        
                    
                    case "end recognition":
                        isListening = false;
                        helpPrompt = notListeningPrompt;
                        refreshPrompt();
                        System.out.println("end recognition");
                        break;
                }
            }
            else if(getNewCommand() && !isListening){
                setNewCommand(false);
                if(getCommand().equals("begin recognition")){
                    isListening = true;
                    helpPrompt = listeningPrompt;
  
                    refreshPrompt();

                    System.out.println("started listening");
                }
            
        }
        }));
        timeTimeline.play();
    }
    
    /**
     * This method starts listening for a Wolfram query and completes the
     * necessary tasks to display it to the screen
     */
    public static void performWolframSearch() {
        //Do stuff with jarvis, sending it to wrSearch class

        displayCenter(countDown);
        countdown(6);

        Task<Void> wolframSearchTask = new Task<Void>() {
            @Override
            public Void call() {
                JARVIS.startListening();
                // While there is no response from google, wait max time of MAX_WAIT*(500) milsec
                int waitCount = 0;
                final int MAX_WAIT = 14;
                while (!responseFromGoogle && waitCount < MAX_WAIT) {
                    try {
                        Thread.sleep(500);
                        System.out.println("wait for response from google..." + "(" + waitCount + ")");
                        waitCount++;
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SmartMirror.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (waitCount != MAX_WAIT) {
                    //setting response back to false, primed for next request
                    setResponseFromGoogle(false);
                    // Gets only the first predicted interpretation
                    if(wrQuery == null){
                        System.out.println("result was null");
                    }
                    String wrQueryTrim = wrQuery.split("\"")[0];
                    wolframVbox = (WolframSearch.getResultsFor(wrQueryTrim));
                    System.out.println("wr VBox set to field");

                } else {
                    wolframVbox = WolframSearch.displayError();
                }

                return null;
            }
        };
        wolframSearchTask.setOnSucceeded((WorkerStateEvent ee) -> {
            displayCenter(wolframVbox);
            System.out.println("setOnSucceeded");
            sphinxThread = new Thread(new DialogDemo());
            sphinxThread.start();
        });

        new Thread(wolframSearchTask).start();

        

    }

    /**
     * Update time every second
     *
     * @param time Text on screen to be updated
     */
    public void updateTime(Text time) {
        Timeline timeTimeline = new Timeline();
        timeTimeline.setCycleCount(Timeline.INDEFINITE);
        timeTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {

            // Gets current time
            localCalendar.setTimeInMillis(System.currentTimeMillis());
            int hours = localCalendar.get(Calendar.HOUR_OF_DAY);
            int minutes = localCalendar.get(Calendar.MINUTE);
            //int seconds = localCalendar.get(Calendar.SECOND);

            //No military time pls
            if (hours > 12) {
                hours = hours - 12;
            }
            if (hours == 0) {
                hours = 12;
            }

            NumberFormat formatter = new DecimalFormat("00");
            String hours_str = formatter.format(hours);
            String min_str = formatter.format(minutes);
            //String sec_str = formatter.format(seconds);

            time.setText(hours_str + ":" + min_str);
        }));
        timeTimeline.play();

    }

    public void updateDate(Text date) {
        Timeline timeTimeline = new Timeline();
        timeTimeline.setCycleCount(Timeline.INDEFINITE);
        timeTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {

            // Gets current time
            localCalendar.setTimeInMillis(System.currentTimeMillis());
            int month = localCalendar.get(Calendar.MONTH);
            int numericDay = localCalendar.get(Calendar.DAY_OF_MONTH);
            int year = localCalendar.get(Calendar.YEAR);

            String month_str = MONTH_STRINGS[month];
            String stringDay = dayOfWeek(localCalendar.get(Calendar.DAY_OF_WEEK));
            date.setText(stringDay + "\n      " + month_str + " " + numericDay + ", " + year);
        }));
        timeTimeline.play();

    }
    
    public static void countdown(int seconds) {

        countDown.setFont(Font.font(USED_FONT, 50));
        countDown.setFill(Color.WHITE);
        countTime = seconds;
        
        Timeline timeTimeline = new Timeline();
        timeTimeline.setCycleCount(seconds);
        timeTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), (ActionEvent event) -> {
            countDown.setText("Time left to speak: " + (countTime-1));
            countTime--;
            System.out.println("Inside timeline");
            
        }));
        timeTimeline.play();
        
        timeTimeline.setOnFinished((ActionEvent e) ->{

            displayCenter(LS.start());
        });

    }

    /**
     * Updates weather ever 20 minutes(1200 seconds)
     *
     * @param rootP Pane to add updated weather
     */
    public void updateWeather(BorderPane rootP) {
        int updateInterval = 1200;

        Timeline weatherTimeline = new Timeline();
        weatherTimeline.setCycleCount(Timeline.INDEFINITE);
        weatherTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(updateInterval), new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                weatherVbox = weather.getWeatherVB();
                rootP.setRight(weatherVbox);
            }
        }));
        weatherTimeline.play();
    }

    /**
     * Sets responseFromGoogle, so Wolfram can grab the updated question string
     * and continue
     *
     * @param toSet Boolean to be set from Jarvis
     */
    public static void setResponseFromGoogle(boolean toSet) {
        responseFromGoogle = toSet;
    }

    /**
     * This method sets the boolean query so the Wolfram search can continue
     *
     * @param query Boolean to set
     */
    public static void setQuery(String query) {
        wrQuery = query;
    }

    /**
     * This method sets the command from DialogDemo
     *
     * @param commandToSet command to set
     */
    public static void setCommand(String commandToSet) {
        command = commandToSet;
    }

    /**
     * Returns the command
     *
     * @return command
     */
    public static String getCommand() {
        return command;
    }

    /**
     * Sets if the command is new
     *
     * @param isNewCommand is it??
     */
    public static void setNewCommand(Boolean isNewCommand) {
        newCommand = isNewCommand;
    }

    /**
     * Returns if the new command is new or not
     *
     * @return new or not
     */
    public static Boolean getNewCommand() {
        return newCommand;
    }

    /**
     * This method displays a node to the center, coming up from the bottom of
     * the screen
     *
     * @param nodeToDisplay The node that will be added to the screen
     */
    public static void displayCenter(Node nodeToDisplay) {
        bpRoot.setCenter(nodeToDisplay);

        BorderPane.setAlignment(nodeToDisplay, Pos.CENTER);
        nodeToDisplay.setTranslateY(bpRoot.getHeight());
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(nodeToDisplay.translateYProperty(), 0));

        Timeline tl = new Timeline(keyFrame);
        tl.play();
    }

    /**
     * This method gets whatever is in the center of the root BorderPane
     * (bpRoot) and dismisses it (removes from screen and then clears)
     */
    public static void dismissCenter() {
        Node node = bpRoot.getCenter();
        if (node != null) {
            KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                    new KeyValue(node.translateYProperty(), bpRoot.getHeight()));

            Timeline tl = new Timeline(keyFrame);
            tl.setOnFinished(e -> bpRoot.setCenter(null));
            tl.play();
            System.out.println("Removed bottom node(dis center)");
        } else {
            System.out.println("Nothing to remove from center(null)");
        }
    }
    
        /**
     * This method displays a node to the center, coming up from the bottom of
     * the screen
     *
     * @param nodeToDisplay The node that will be added to the screen
     */
    public static void displayBottom(Node nodeToDisplay) {
        bpRoot.setBottom(nodeToDisplay);
        Pane p = (Pane) nodeToDisplay;
        BorderPane.setAlignment(nodeToDisplay, Pos.CENTER);
        nodeToDisplay.setTranslateY(p.getHeight());
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(nodeToDisplay.translateYProperty(), 0));

        Timeline tl = new Timeline(keyFrame);
        tl.play();
    }
    
    /**
     * This method gets whatever is in the bottom of the root BorderPane
     * (bpRoot) and dismisses it (removes from screen and then clears)
     */
    public static void dismissBottom() {
        Node node = bpRoot.getBottom();
        Pane p = (Pane) node;
        if (node != null) {
            KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                    new KeyValue(node.translateYProperty(), p.getHeight()));

            Timeline tl = new Timeline(keyFrame);
            //tl.setOnFinished(e -> bpRoot.setBottom(null));
            tl.play();
            System.out.println("Removed center node(dis bott)");
        } else {
            System.out.println("Nothing to remove from bott(null)");
        }
    }

    /**
     * This method moves the weather back to the screen
     */
    public static void showWeatherDisplay() {

        double translateValue = 0;
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(weatherVbox.translateXProperty(), translateValue));

        Timeline tl = new Timeline(keyFrame);
        tl.play();
    }

    /**
     * This method hides the weather display, so the rest of the screen can be
     * used
     */
    public static void hideWeatherDisplay() {
        double translateValue = weatherVbox.getWidth() + bpRootInsetValue;
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(weatherVbox.translateXProperty(), translateValue));

        Timeline tl = new Timeline(keyFrame);
        tl.play();
    }

    /**
     * Puts the left display back on the screen
     */
    public static void showLeftDisplay() {
        double translateValue = 0;
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(leftDisplay.translateXProperty(), translateValue));

        Timeline tl = new Timeline(keyFrame);
        tl.play();
    }

    /**
     * *
     * Removes the left display, so the screen can be used for whatever
     */
    public static void hideLeftDisplay() {
        double translateValue = (-1) * (leftDisplay.getWidth() + bpRootInsetValue);
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(leftDisplay.translateXProperty(), translateValue));

        Timeline tl = new Timeline(keyFrame);
        tl.play();
    }

    /**
     * Removed the help message from the screen
     */
    public static void showHelp() {
        double translateValue = helpPrompt.getHeight();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(helpPrompt.translateYProperty(), translateValue));

        Timeline tl = new Timeline(keyFrame);
        tl.play();
        
        tl.setOnFinished((ActionEvent e) ->{
            bpRoot.setBottom(voiceDialogHelp);
            
            voiceDialogHelp.setTranslateY(voiceDialogHelp.getHeight());
        
            KeyFrame keyFrame1 = new KeyFrame(Duration.millis(500),
                    new KeyValue(voiceDialogHelp.translateYProperty(), 0));

            Timeline tl1 = new Timeline(keyFrame1);
            tl1.play();
        });
        
    }

    /**
     * Puts help message on screen
     */
    public static void hideHelp() {
        
        double translateValue = voiceDialogHelp.getHeight();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(voiceDialogHelp.translateYProperty(), translateValue));

        Timeline tl = new Timeline(keyFrame);
        tl.play();
        tl.setOnFinished((ActionEvent e) ->{
            System.out.println("finished hideHelp");
            showHelpPrompt();
        });
        
        
    }
    
    public static void showHelpPrompt(){ 
        bpRoot.setBottom(helpPrompt);
        helpPrompt.setTranslateY(helpPrompt.getHeight());
        double translateValue = 0;
        KeyFrame keyFrame = new KeyFrame(Duration.millis(500),
                new KeyValue(helpPrompt.translateYProperty(), translateValue));

        Timeline tl = new Timeline(keyFrame);
        tl.play();

    }
    
    public static void refreshPrompt(){
        bpRoot.setBottom(helpPrompt);

    }
    

    /**
     * Calls all hide methods, so the screen is clear
     */
    public static void hideDisplay() {
        dismissBottom();
        hideWeatherDisplay();
        hideLeftDisplay();
    }

    /**
     * This method calls all show methods, so the display is back on the screen
     */
    public static void showDisplay() {
        hideHelp();
        showLeftDisplay();
        showWeatherDisplay();
    }

    /**
     * Helper method that return the day of the week in string format
     *
     * @param day Integer representation of the day
     * @return String with correct day in English
     */
    public static String dayOfWeek(int day) {
        String dayInStringFormat = "";
        switch (day) {
            case 1:
                dayInStringFormat = "Sunday";
                break;
            case 2:
                dayInStringFormat = "Monday";
                break;
            case 3:
                dayInStringFormat = "Tuesday";
                break;
            case 4:
                dayInStringFormat = "Wednesday";
                break;
            case 5:
                dayInStringFormat = "Thursday";
                break;
            case 6:
                dayInStringFormat = "Friday";
                break;
            case 7:
                dayInStringFormat = "Saturday";
                break;
            default:
                System.out.println("Day Error(dayOfWeek)");
        }
        return dayInStringFormat;
    }
    
    private HBox createHelpBox(String img){
        HBox hb = new HBox();
        hb.setSpacing(20);
        
        Text hint = new Text("Hint: Say \"What can I say?\" for a list of commands");
        hint.setFont(Font.font(USED_FONT, 25));
        hint.setFill(Color.WHITE);
        
        ImageView listening = new ImageView(new Image(getClass().getResourceAsStream(
                "/resources/weather_icons/" + img + ".png"), 75, 75, true, true));
        
        hb.setAlignment(Pos.CENTER);
        
        hb.getChildren().addAll(hint, listening);
        return hb;
    }
}
