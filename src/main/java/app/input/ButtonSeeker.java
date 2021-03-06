package app.input;

import java.util.NoSuchElementException;

public final class ButtonSeeker {

    private Button button;

    ButtonSeeker() {}

    public boolean isPresent() {
        return this.button != null;
    }

    public Button getElseThrow() {
        if (!this.isPresent()) {
            throw new NoSuchElementException("There is no Button yet in " +
                    "this ButtonIncubator.");
        }//end if

        return this.button;
    }

    void putIfAbsent(Button button) {
        if (this.isPresent()) {
            return;
        }//end if

        this.button = button;
    }

}//end static nested class ButtonSeeker
