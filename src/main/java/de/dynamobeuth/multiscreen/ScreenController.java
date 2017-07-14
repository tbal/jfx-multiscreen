package de.dynamobeuth.multiscreen;

abstract public class ScreenController {

    private ScreenManager screenManager;

    private MultiScreenApplication application;

    public void setApplication(MultiScreenApplication application) {
        this.application = application;
    }

    public MultiScreenApplication getApplication() {
        return application;
    }

    public void setScreenManager(ScreenManager manager) {
        this.screenManager = manager;
    }

    protected ScreenManager getScreenManager() {
        return screenManager;
    }

    /**
     * Will be called once after initialize() and after application and screenManager were injected
     */
    protected void prepare() {
    }

    /**
     * Will be called once when the screen is shown for the first time
     * and just before the screen is added to its root and before possible screen transition animations
     */
    protected void onBeforeFirstShow() {
    }

    /**
     * Will be called every time the screen is to be shown
     * and just before the screen is added to its root and before possible screen transition animations
     */
    protected void onBeforeShow() {
    }

    /**
     * Will be called once when the screen is shown for the first time
     * and when possible screen transition animations are just finished
     */
    protected void onFirstShow() {
    }

    /**
     * Will be called every time the screen is shown
     * and when possible screen transition animations are just finished
     */
    protected void onShow() {
    }
}
