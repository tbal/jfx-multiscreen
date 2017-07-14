package de.dynamobeuth.multiscreen;

import de.dynamobeuth.multiscreen.animation.ScreenTransition;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;

import javax.naming.InvalidNameException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ScreenManager extends Pane {

    private MultiScreenApplication application;

    private String skin = "default";

    private LinkedHashMap<String, Parent> screens = new LinkedHashMap<>();
    private LinkedHashMap<String, ScreenController> controllers = new LinkedHashMap<>();

    private String previousScreenName = null;
    private String currentScreenName = null;

    private SimpleBooleanProperty closeRequestActive = new SimpleBooleanProperty(false);

    private Set<String> screensShown = new HashSet<>();

    private Pane loadingIndicatorOverlay;

    private Pane shadeScreenOverlay;

    public ScreenManager(MultiScreenApplication application) {
        super();

        this.application = application;
    }

    public void initScreens() {
        for (Map.Entry<String, URL> view : getViewsByConvention().entrySet()) {
            try {
                addScreen(view.getKey(), view.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void initStylesheets() {
        initStylesheets(getSkin());
    }

    public void initStylesheets(String skin) {
        for (String screenName : screens.keySet()) {
            URL screenCss = application.getClass().getResource("skin/" + skin + "/css/" + screenName + ".css");

            if (screenCss != null) {
                addStylesheetToScreen(screenName, screenCss.toString());
            }
        }
    }

    public void addScreen(String name, URL fxmlLocation) throws IOException {
        FXMLLoader loader = new FXMLLoader(fxmlLocation);

        // load the FXML
        Parent screen = loader.load();

        // add screen to list of screens
        screens.put(name, screen);

        // load the Controller
        ScreenController controller = loader.getController();

        // inject application
        controller.setApplication(application);

        // inject the current screenManager instance
        controller.setScreenManager(this);

        // call prepare() method
        controller.prepare();

        // add controller to list of controllers
        controllers.put(name, controller);
    }

    public boolean addStylesheetToScreen(String name, String path) {
        try {
            Parent screen = getScreenByName(name);
            screen.getStylesheets().add(path);

        } catch (InvalidNameException e) {
            e.printStackTrace();

            return false;
        }

        System.out.println("Added stylesheet for screen '" + name + "': " + path);

        return true;
    }

    public boolean removeScreen(String name) {
        return screens.remove(name) == null;
    }

    public Parent getScreenByName(String name) throws InvalidNameException {
        Parent screen = screens.get(name);

        if (screen == null) {
            throw new InvalidNameException("There is no Screen with the name '" + name + "'.");
        }

        return screen;
    }

    public boolean showScreen(String name) {
        try {
            Parent screen = getScreenByName(name);

            previousScreenName = currentScreenName;
            currentScreenName = name;

            resetScreen(screen);

            if (!getChildren().isEmpty()) {
                getChildren().remove(0);
            }

            ScreenController screenController = getControllerByName(name);

            handleControllerBeforeShowMethods(name, screenController);

            getChildren().add(screen);

            handleControllerShowMethods(name, screenController);

        } catch (InvalidNameException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public boolean showScreen(String name, ScreenTransition animation) {
        try {
            Parent screen = getScreenByName(name);

            previousScreenName = currentScreenName;
            currentScreenName = name;

            resetScreen(screen);

            ScreenController screenController = getControllerByName(name);

            handleControllerBeforeShowMethods(name, screenController);

            if (!getChildren().isEmpty()) {
                animation.animate(this, getChildren().get(0), screen, e -> handleControllerShowMethods(name, screenController));
            } else {
                animation.animate(this, null, screen, e -> handleControllerShowMethods(name, screenController));
            }

        } catch (InvalidNameException e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    private void handleControllerBeforeShowMethods(String name, ScreenController screenController) {
        if (!screensShown.contains(name)) {
            screenController.onBeforeFirstShow();
        }

        screenController.onBeforeShow();
    }

    private void handleControllerShowMethods(String name, ScreenController screenController) {
        if (!screensShown.contains(name)) {
            screenController.onFirstShow();
            screensShown.add(name);
        }

        if (currentScreenNameMatches(name)) {
            screenController.onShow();
        }
    }

    public LinkedHashMap<String, Parent> getScreens() {
        return screens;
    }

    public LinkedHashMap<String, ScreenController> getControllers() {
        return controllers;
    }

    public ScreenController getControllerByName(String name) throws InvalidNameException {
        ScreenController controller = controllers.get(name);

        if (controller == null) {
            throw new InvalidNameException("There is no Controller for the Screen with the name '" + name + "'.");
        }

        return controller;
    }

    public void showLoadingIndicatorOverlay() {
        if (loadingIndicatorOverlay == null) {
            initLoadingIndicatorOverlay();
        }

        this.getChildren().add(loadingIndicatorOverlay);
    }

    public void hideLoadingIndicatorOverlay() {
        if (loadingIndicatorOverlay != null) {
            this.getChildren().remove(loadingIndicatorOverlay);
        }
    }

    protected void initLoadingIndicatorOverlay() {
        loadingIndicatorOverlay = new Pane();
        loadingIndicatorOverlay.getStyleClass().add("loading-indicator-overlay");
        loadingIndicatorOverlay.getStylesheets().add(getClass().getResource("css/styles.css").toExternalForm());
        loadingIndicatorOverlay.minWidthProperty().bind(this.widthProperty());
        loadingIndicatorOverlay.minHeightProperty().bind(this.heightProperty());
    }

    public void shadeScreen() {
        if (shadeScreenOverlay == null) {
            shadeScreenOverlay = new Pane();
            shadeScreenOverlay.setOpacity(0.2);
            shadeScreenOverlay.setStyle("-fx-background-color: black");
        }

        setEffect(new GaussianBlur(5.0D));
        getChildren().add(shadeScreenOverlay);
    }

    public void unshadeScreen() {
        setEffect(null);
        getChildren().remove(shadeScreenOverlay);
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public String getPreviousScreenName() {
        return previousScreenName;
    }

    public boolean previousScreenNameMatches(String name) {
        return previousScreenName != null && previousScreenName.equals(name);
    }

    public String getCurrentScreenName() {
        return currentScreenName;
    }

    public boolean currentScreenNameMatches(String name) {
        return currentScreenName != null && currentScreenName.equals(name);
    }

    private void resetScreen(Parent screen) {
        screen.setOpacity(1);

        screen.setScaleX(1);
        screen.setScaleY(1);
        screen.setScaleZ(1);

        screen.setTranslateX(0);
        screen.setTranslateY(0);
        screen.setTranslateZ(0);

        screen.setRotate(0);
    }

    /**
     * Finds all views which are stored with the convention view/*View.fxml and returns them as map.
     * Inspired by https://stackoverflow.com/a/20073154
     *
     * @return A map with viewName -> viewPath
     */
    private LinkedHashMap<String, URL> getViewsByConvention() {
        String appendedNamingConvention = "View.fxml";

        LinkedHashMap<String, URL> availableViewsList = new LinkedHashMap<>();
        final File jarFile = new File(application.getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

        if (jarFile.isFile()) { // Run with JAR file
            final JarFile jar;

            try {
                jar = new JarFile(jarFile);
                final Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar

                while (entries.hasMoreElements()) {
                    final String name = entries.nextElement().getName();

                    if (name.contains("/view/")) { //filter according to the path
                        Path path = Paths.get(name);
                        String viewFileName = path.getFileName().toString();

                        if (!viewFileName.endsWith(appendedNamingConvention)) {
                            continue;
                        }

                        // remove 'View.fxml' from 'ExampleView.fxml' and lowercase it
                        String viewName = viewFileName.substring(0, viewFileName.length() - appendedNamingConvention.length()).toLowerCase();

                        availableViewsList.put(viewName, application.getClass().getResource("view/" + viewFileName));
                    }
                }

                jar.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else { // Run with IDE
            final URL url = application.getClass().getResource("view/");

            if (url != null) {
                try {
                    final File apps = new File(url.toURI());

                    for (File app : apps.listFiles()) {
                        String viewName = app.getName();

                        if (!viewName.endsWith(appendedNamingConvention)) {
                            continue;
                        }

                        // remove 'View.fxml' from 'ExampleView.fxml' and lowercase it
                        viewName = viewName.substring(0, viewName.length() - appendedNamingConvention.length()).toLowerCase();

                        availableViewsList.put(viewName, app.toURI().toURL());

                    }
                } catch (URISyntaxException e) {
                    // never happens
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }

        return availableViewsList;
    }

    public boolean isCloseRequestActive() {
        return closeRequestActive.get();
    }

    public ReadOnlyBooleanProperty closeRequestActiveProperty() {
        return closeRequestActive;
    }

    protected void setCloseRequestActive(boolean closeRequestActive) {
        this.closeRequestActive.set(closeRequestActive);
    }
}
