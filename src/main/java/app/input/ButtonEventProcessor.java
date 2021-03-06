package app.input;

public class ButtonEventProcessor implements AutoCloseable {

    private final ButtonEvent BUTTON_EVENT;
    private final Runnable ACTION;

    public static ButtonEventProcessor ofButtonPress(Keyboard.Key key,
                                                     Runnable action) {
        return ButtonEventProcessor.ofButtonPress(key.asButton().orElseThrow(),
                action);
    }

    public static ButtonEventProcessor ofButtonPress(Button button,
                                                     Runnable action) {
        return new ButtonEventProcessor(ButtonEvent.of(button, true),
                action);
    }

    public static ButtonEventProcessor ofButtonRelease(Keyboard.Key key,
                                                       Runnable action) {
        return ButtonEventProcessor.ofButtonRelease(
                key.asButton().orElseThrow(), action);
    }

    public static ButtonEventProcessor ofButtonRelease(Button button,
                                                       Runnable action) {
        return new ButtonEventProcessor(ButtonEvent.of(button, false),
                action);
    }

    private ButtonEventProcessor(ButtonEvent buttonEvent, Runnable action) {
        this.BUTTON_EVENT =  buttonEvent;
        this.ACTION = action;
    }

    public void process() {
        if (this.BUTTON_EVENT.hasOccurred()) {
            this.ACTION.run();
        }//end if
    }

    public boolean isClosed() {
        return this.BUTTON_EVENT.isClosed();
    }

    @Override
    public void close() {
        this.BUTTON_EVENT.close();
    }

}//end class ButtonEventProcessor
