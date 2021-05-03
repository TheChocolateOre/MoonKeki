package app.input;

public abstract sealed class Button permits Keyboard.Button, Mouse.Button {

    @Deprecated
    static record Snapshot(Button button, boolean pressed) {}

    protected final int ID;

    public Button(int id) {
        this.ID = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }//end if

        if (null == o) {
            return false;
        }//end if

        return (o instanceof Button b) && this.ID == b.ID;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.ID);
    }

    public abstract boolean isPressed();

}//end class Button
