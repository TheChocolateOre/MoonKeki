package app.input;

import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Keyboard {

    public enum Key {
        SPACE(GLFW.GLFW_KEY_SPACE),
        APOSTROPHE(GLFW.GLFW_KEY_APOSTROPHE),
        COMMA(GLFW.GLFW_KEY_COMMA),
        MINUS(GLFW.GLFW_KEY_MINUS),
        PERIOD(GLFW.GLFW_KEY_PERIOD),
        SLASH(GLFW.GLFW_KEY_SLASH),
        N0(GLFW.GLFW_KEY_0),
        N1(GLFW.GLFW_KEY_1),
        N2(GLFW.GLFW_KEY_2),
        N3(GLFW.GLFW_KEY_3),
        N4(GLFW.GLFW_KEY_4),
        N5(GLFW.GLFW_KEY_5),
        N6(GLFW.GLFW_KEY_6),
        N7(GLFW.GLFW_KEY_7),
        N8(GLFW.GLFW_KEY_8),
        N9(GLFW.GLFW_KEY_9),
        SEMICOLON(GLFW.GLFW_KEY_SEMICOLON),
        EQUAL(GLFW.GLFW_KEY_EQUAL),
        A(GLFW.GLFW_KEY_A),
        B(GLFW.GLFW_KEY_B),
        C(GLFW.GLFW_KEY_C),
        D(GLFW.GLFW_KEY_D),
        E(GLFW.GLFW_KEY_E),
        F(GLFW.GLFW_KEY_F),
        G(GLFW.GLFW_KEY_G),
        H(GLFW.GLFW_KEY_H),
        I(GLFW.GLFW_KEY_I),
        J(GLFW.GLFW_KEY_J),
        K(GLFW.GLFW_KEY_K),
        L(GLFW.GLFW_KEY_L),
        M(GLFW.GLFW_KEY_M),
        N(GLFW.GLFW_KEY_N),
        O(GLFW.GLFW_KEY_O),
        P(GLFW.GLFW_KEY_P),
        Q(GLFW.GLFW_KEY_Q),
        R(GLFW.GLFW_KEY_R),
        S(GLFW.GLFW_KEY_S),
        T(GLFW.GLFW_KEY_T),
        U(GLFW.GLFW_KEY_U),
        V(GLFW.GLFW_KEY_V),
        W(GLFW.GLFW_KEY_W),
        X(GLFW.GLFW_KEY_X),
        Y(GLFW.GLFW_KEY_Y),
        Z(GLFW.GLFW_KEY_Z),
        LEFT_BRACKET(GLFW.GLFW_KEY_LEFT_BRACKET),
        BACKSLASH(GLFW.GLFW_KEY_BACKSLASH),
        RIGHT_BRACKET(GLFW.GLFW_KEY_RIGHT_BRACKET),
        GRAVE_ACCENT(GLFW.GLFW_KEY_GRAVE_ACCENT),
        ESCAPE(GLFW.GLFW_KEY_ESCAPE),
        ENTER(GLFW.GLFW_KEY_ENTER),
        TAB(GLFW.GLFW_KEY_TAB),
        BACKSPACE(GLFW.GLFW_KEY_BACKSPACE),
        INSERT(GLFW.GLFW_KEY_INSERT),
        DELETE(GLFW.GLFW_KEY_DELETE),
        RIGHT(GLFW.GLFW_KEY_RIGHT),
        LEFT(GLFW.GLFW_KEY_LEFT),
        DOWN(GLFW.GLFW_KEY_DOWN),
        UP(GLFW.GLFW_KEY_UP),
        PAGE_UP(GLFW.GLFW_KEY_PAGE_UP),
        PAGE_DOWN(GLFW.GLFW_KEY_PAGE_DOWN),
        HOME(GLFW.GLFW_KEY_HOME),
        END(GLFW.GLFW_KEY_END),
        CAPS_LOCK(GLFW.GLFW_KEY_CAPS_LOCK),
        SCROLL_LOCK(GLFW.GLFW_KEY_SCROLL_LOCK),
        NUM_LOCK(GLFW.GLFW_KEY_NUM_LOCK),
        PRINT_SCREEN(GLFW.GLFW_KEY_PRINT_SCREEN),
        PAUSE(GLFW.GLFW_KEY_PAUSE),
        F1(GLFW.GLFW_KEY_F1),
        F2(GLFW.GLFW_KEY_F2),
        F3(GLFW.GLFW_KEY_F3),
        F4(GLFW.GLFW_KEY_F4),
        F5(GLFW.GLFW_KEY_F5),
        F6(GLFW.GLFW_KEY_F6),
        F7(GLFW.GLFW_KEY_F7),
        F8(GLFW.GLFW_KEY_F8),
        F9(GLFW.GLFW_KEY_F9),
        F10(GLFW.GLFW_KEY_F10),
        F11(GLFW.GLFW_KEY_F11),
        F12(GLFW.GLFW_KEY_F12),
        F13(GLFW.GLFW_KEY_F13),
        F14(GLFW.GLFW_KEY_F14),
        F15(GLFW.GLFW_KEY_F15),
        F16(GLFW.GLFW_KEY_F16),
        F17(GLFW.GLFW_KEY_F17),
        F18(GLFW.GLFW_KEY_F18),
        F19(GLFW.GLFW_KEY_F19),
        F20(GLFW.GLFW_KEY_F20),
        F21(GLFW.GLFW_KEY_F21),
        F22(GLFW.GLFW_KEY_F22),
        F23(GLFW.GLFW_KEY_F23),
        F24(GLFW.GLFW_KEY_F24),
        F25(GLFW.GLFW_KEY_F25),
        NP0(GLFW.GLFW_KEY_KP_0),
        NP1(GLFW.GLFW_KEY_KP_1),
        NP2(GLFW.GLFW_KEY_KP_2),
        NP3(GLFW.GLFW_KEY_KP_3),
        NP4(GLFW.GLFW_KEY_KP_4),
        NP5(GLFW.GLFW_KEY_KP_5),
        NP6(GLFW.GLFW_KEY_KP_6),
        NP7(GLFW.GLFW_KEY_KP_7),
        NP8(GLFW.GLFW_KEY_KP_8),
        NP9(GLFW.GLFW_KEY_KP_9),
        NP_DECIMAL(GLFW.GLFW_KEY_KP_DECIMAL),
        NP_DIVIDE(GLFW.GLFW_KEY_KP_DIVIDE),
        NP_MULTIPLY(GLFW.GLFW_KEY_KP_MULTIPLY),
        NP_SUBTRACT(GLFW.GLFW_KEY_KP_SUBTRACT),
        NP_ADD(GLFW.GLFW_KEY_KP_ADD),
        NP_ENTER(GLFW.GLFW_KEY_KP_ENTER),
        NP_EQUAL(GLFW.GLFW_KEY_KP_EQUAL),
        LEFT_SHIFT(GLFW.GLFW_KEY_LEFT_SHIFT),
        LEFT_CONTROL(GLFW.GLFW_KEY_LEFT_CONTROL),
        LEFT_ALT(GLFW.GLFW_KEY_LEFT_ALT),
        LEFT_SUPER(GLFW.GLFW_KEY_LEFT_SUPER),
        RIGHT_SHIFT(GLFW.GLFW_KEY_RIGHT_SHIFT),
        RIGHT_CONTROL(GLFW.GLFW_KEY_RIGHT_CONTROL),
        RIGHT_ALT(GLFW.GLFW_KEY_RIGHT_ALT),
        RIGHT_SUPER(GLFW.GLFW_KEY_RIGHT_SUPER),
        MENU(GLFW.GLFW_KEY_MENU);

        private static Optional<Key> fromId(final int id) {
            return Optional.ofNullable(switch (id) {
                case GLFW.GLFW_KEY_SPACE -> Key.SPACE;
                case GLFW.GLFW_KEY_APOSTROPHE -> Key.APOSTROPHE;
                case GLFW.GLFW_KEY_COMMA -> Key.COMMA;
                case GLFW.GLFW_KEY_MINUS -> Key.MINUS;
                case GLFW.GLFW_KEY_PERIOD -> Key.PERIOD;
                case GLFW.GLFW_KEY_SLASH -> Key.SLASH;
                case GLFW.GLFW_KEY_0 -> Key.N0;
                case GLFW.GLFW_KEY_1 -> Key.N1;
                case GLFW.GLFW_KEY_2 -> Key.N2;
                case GLFW.GLFW_KEY_3 -> Key.N3;
                case GLFW.GLFW_KEY_4 -> Key.N4;
                case GLFW.GLFW_KEY_5 -> Key.N5;
                case GLFW.GLFW_KEY_6 -> Key.N6;
                case GLFW.GLFW_KEY_7 -> Key.N7;
                case GLFW.GLFW_KEY_8 -> Key.N8;
                case GLFW.GLFW_KEY_9 -> Key.N9;
                case GLFW.GLFW_KEY_SEMICOLON -> Key.SEMICOLON;
                case GLFW.GLFW_KEY_EQUAL -> Key.EQUAL;
                case GLFW.GLFW_KEY_A -> Key.A;
                case GLFW.GLFW_KEY_B -> Key.B;
                case GLFW.GLFW_KEY_C -> Key.C;
                case GLFW.GLFW_KEY_D -> Key.D;
                case GLFW.GLFW_KEY_E -> Key.E;
                case GLFW.GLFW_KEY_F -> Key.F;
                case GLFW.GLFW_KEY_G -> Key.G;
                case GLFW.GLFW_KEY_H -> Key.H;
                case GLFW.GLFW_KEY_I -> Key.I;
                case GLFW.GLFW_KEY_J -> Key.J;
                case GLFW.GLFW_KEY_K -> Key.K;
                case GLFW.GLFW_KEY_L -> Key.L;
                case GLFW.GLFW_KEY_M -> Key.M;
                case GLFW.GLFW_KEY_N -> Key.N;
                case GLFW.GLFW_KEY_O -> Key.O;
                case GLFW.GLFW_KEY_P -> Key.P;
                case GLFW.GLFW_KEY_Q -> Key.Q;
                case GLFW.GLFW_KEY_R -> Key.R;
                case GLFW.GLFW_KEY_S -> Key.S;
                case GLFW.GLFW_KEY_T -> Key.T;
                case GLFW.GLFW_KEY_U -> Key.U;
                case GLFW.GLFW_KEY_V -> Key.V;
                case GLFW.GLFW_KEY_W -> Key.W;
                case GLFW.GLFW_KEY_X -> Key.X;
                case GLFW.GLFW_KEY_Y -> Key.Y;
                case GLFW.GLFW_KEY_Z -> Key.Z;
                case GLFW.GLFW_KEY_LEFT_BRACKET -> Key.LEFT_BRACKET;
                case GLFW.GLFW_KEY_BACKSLASH -> Key.BACKSLASH;
                case GLFW.GLFW_KEY_RIGHT_BRACKET -> Key.RIGHT_BRACKET;
                case GLFW.GLFW_KEY_GRAVE_ACCENT -> Key.GRAVE_ACCENT;
                case GLFW.GLFW_KEY_ESCAPE -> Key.ESCAPE;
                case GLFW.GLFW_KEY_ENTER -> Key.ENTER;
                case GLFW.GLFW_KEY_TAB -> Key.TAB;
                case GLFW.GLFW_KEY_BACKSPACE -> Key.BACKSPACE;
                case GLFW.GLFW_KEY_INSERT -> Key.INSERT;
                case GLFW.GLFW_KEY_DELETE -> Key.DELETE;
                case GLFW.GLFW_KEY_RIGHT -> Key.RIGHT;
                case GLFW.GLFW_KEY_LEFT -> Key.LEFT;
                case GLFW.GLFW_KEY_DOWN -> Key.DOWN;
                case GLFW.GLFW_KEY_UP -> Key.UP;
                case GLFW.GLFW_KEY_PAGE_UP -> Key.PAGE_UP;
                case GLFW.GLFW_KEY_PAGE_DOWN -> Key.PAGE_DOWN;
                case GLFW.GLFW_KEY_HOME -> Key.HOME;
                case GLFW.GLFW_KEY_END -> Key.END;
                case GLFW.GLFW_KEY_CAPS_LOCK -> Key.CAPS_LOCK;
                case GLFW.GLFW_KEY_SCROLL_LOCK -> Key.SCROLL_LOCK;
                case GLFW.GLFW_KEY_NUM_LOCK -> Key.NUM_LOCK;
                case GLFW.GLFW_KEY_PRINT_SCREEN -> Key.PRINT_SCREEN;
                case GLFW.GLFW_KEY_PAUSE -> Key.PAUSE;
                case GLFW.GLFW_KEY_F1 -> Key.F1;
                case GLFW.GLFW_KEY_F2 -> Key.F2;
                case GLFW.GLFW_KEY_F3 -> Key.F3;
                case GLFW.GLFW_KEY_F4 -> Key.F4;
                case GLFW.GLFW_KEY_F5 -> Key.F5;
                case GLFW.GLFW_KEY_F6 -> Key.F6;
                case GLFW.GLFW_KEY_F7 -> Key.F7;
                case GLFW.GLFW_KEY_F8 -> Key.F8;
                case GLFW.GLFW_KEY_F9 -> Key.F9;
                case GLFW.GLFW_KEY_F10 -> Key.F10;
                case GLFW.GLFW_KEY_F11 -> Key.F11;
                case GLFW.GLFW_KEY_F12 -> Key.F12;
                case GLFW.GLFW_KEY_F13 -> Key.F13;
                case GLFW.GLFW_KEY_F14 -> Key.F14;
                case GLFW.GLFW_KEY_F15 -> Key.F15;
                case GLFW.GLFW_KEY_F16 -> Key.F16;
                case GLFW.GLFW_KEY_F17 -> Key.F17;
                case GLFW.GLFW_KEY_F18 -> Key.F18;
                case GLFW.GLFW_KEY_F19 -> Key.F19;
                case GLFW.GLFW_KEY_F20 -> Key.F20;
                case GLFW.GLFW_KEY_F21 -> Key.F21;
                case GLFW.GLFW_KEY_F22 -> Key.F22;
                case GLFW.GLFW_KEY_F23 -> Key.F23;
                case GLFW.GLFW_KEY_F24 -> Key.F24;
                case GLFW.GLFW_KEY_F25 -> Key.F25;
                case GLFW.GLFW_KEY_KP_0 -> Key.NP0;
                case GLFW.GLFW_KEY_KP_1 -> Key.NP1;
                case GLFW.GLFW_KEY_KP_2 -> Key.NP2;
                case GLFW.GLFW_KEY_KP_3 -> Key.NP3;
                case GLFW.GLFW_KEY_KP_4 -> Key.NP4;
                case GLFW.GLFW_KEY_KP_5 -> Key.NP5;
                case GLFW.GLFW_KEY_KP_6 -> Key.NP6;
                case GLFW.GLFW_KEY_KP_7 -> Key.NP7;
                case GLFW.GLFW_KEY_KP_8 -> Key.NP8;
                case GLFW.GLFW_KEY_KP_9 -> Key.NP9;
                case GLFW.GLFW_KEY_KP_DECIMAL -> Key.NP_DECIMAL;
                case GLFW.GLFW_KEY_KP_DIVIDE -> Key.NP_DIVIDE;
                case GLFW.GLFW_KEY_KP_MULTIPLY -> Key.NP_MULTIPLY;
                case GLFW.GLFW_KEY_KP_SUBTRACT -> Key.NP_SUBTRACT;
                case GLFW.GLFW_KEY_KP_ADD -> Key.NP_ADD;
                case GLFW.GLFW_KEY_KP_ENTER -> Key.NP_ENTER;
                case GLFW.GLFW_KEY_KP_EQUAL -> Key.NP_EQUAL;
                case GLFW.GLFW_KEY_LEFT_SHIFT -> Key.LEFT_SHIFT;
                case GLFW.GLFW_KEY_LEFT_CONTROL -> Key.LEFT_CONTROL;
                case GLFW.GLFW_KEY_LEFT_ALT -> Key.LEFT_ALT;
                case GLFW.GLFW_KEY_LEFT_SUPER -> Key.LEFT_SUPER;
                case GLFW.GLFW_KEY_RIGHT_SHIFT -> Key.RIGHT_SHIFT;
                case GLFW.GLFW_KEY_RIGHT_CONTROL -> Key.RIGHT_CONTROL;
                case GLFW.GLFW_KEY_RIGHT_ALT -> Key.RIGHT_ALT;
                case GLFW.GLFW_KEY_RIGHT_SUPER -> Key.RIGHT_SUPER;
                case GLFW.GLFW_KEY_MENU -> Key.MENU;
                default -> null;
            });
        }

        final int ID;
        final Keyboard.Button BUTTON;

        Key(final int id) {
            final int SCANCODE = GLFW.glfwGetKeyScancode(id);
            this.ID = id;
            this.BUTTON = (SCANCODE != -1) ? new Button(id, SCANCODE,
                    GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), id) ==
                    GLFW.GLFW_PRESS) : null;
        }

        public boolean exists() {
            return this.BUTTON != null;
        }

        public Optional<Button> asButton() {
            return Optional.ofNullable(this.BUTTON);
        }
    }//end nested enum Key

    static final class Button extends app.input.Button {
        private final int SCANCODE;

        private final ReadWriteLock STATE_LOCK = new ReentrantReadWriteLock(
                true);
        private boolean pressed; //Relevant if id is not relevant

        private Button(int id, int scancode, boolean pressed) {
            super(id);
            if (-1 == scancode) {
                throw new IllegalArgumentException("Argument scancode can't " +
                        "be -1.");
            }//end if

            this.SCANCODE = scancode;
            this.pressed = pressed;
        }

        @Override
        public boolean isPressed() {
            if (this.hasId()) {
                return GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), this.ID) ==
                        GLFW.GLFW_PRESS;
            }//end if

            this.STATE_LOCK.readLock().lock();
            try {
                return this.pressed;
            } finally {
                this.STATE_LOCK.readLock().unlock();
            }//end try
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }//end if

            if (null == o) {
                return false;
            }//end if

            if (!super.equals(o)) {
                return false;
            }//end if

            return (o instanceof Button b) && this.SCANCODE == b.SCANCODE;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.SCANCODE);
        }

        private boolean hasId() {
            return this.ID != GLFW.GLFW_KEY_UNKNOWN;
        }

        private void setState(boolean pressed) {
            this.STATE_LOCK.writeLock().lock();
            try {
                this.pressed = pressed;
            } finally {
                this.STATE_LOCK.writeLock().unlock();
            }//end try
        }
    }//end static nested class Button

    //scancode -> button
    private static final Map<Integer, Keyboard.Button> SCANCODE_TO_BUTTON =
            new ConcurrentHashMap<>();

    private static final ReadWriteLock BUTTON_EVENTS_LOCK =
            new ReentrantReadWriteLock(true);
    private static final Map<Keyboard.Button, Set<ButtonEvent>> BUTTON_EVENTS =
            new HashMap<>();

    private static final Queue<ButtonSeeker> pressedButtonSeekers =
            new ConcurrentLinkedQueue<>();
    private static final Queue<ButtonSeeker> releasedButtonSeekers =
            new ConcurrentLinkedQueue<>();

    static {
        GLFW.glfwSetKeyCallback(GLFW.glfwGetCurrentContext(), (long window, int
                key, int scancode, int action, int mods) -> {
            //We get the type of the event (released | pressed)
            final boolean PRESSED;
            if (GLFW.GLFW_RELEASE == action) {
                PRESSED = false;
            } else if (GLFW.GLFW_PRESS == action) {
                PRESSED = true;
            } else {
                return;
            }//end if

            //We get the Button associated with the current callback
            final Keyboard.Button BUTTON;
            Optional<Key> keyOptional = Key.fromId(key);
            Key k;
            if (keyOptional.isPresent() && (k = keyOptional.get()).exists()) {
                BUTTON = k.BUTTON;
            } else {//non-named button
                BUTTON = Keyboard.getButton(key, scancode, PRESSED);
                BUTTON.setState(PRESSED);
            }//end if

            //We process the ButtonSeeker's'
            ButtonSeeker buttonSeeker = PRESSED ?
                    Keyboard.pressedButtonSeekers.poll() :
                    Keyboard.releasedButtonSeekers.poll();
            if (buttonSeeker != null) {
                buttonSeeker.putIfAbsent(BUTTON);
            }//end if

            //We process the ButtonEvent's'
            var buttonSnapshot = new Button.Snapshot(BUTTON, PRESSED);
            Keyboard.BUTTON_EVENTS_LOCK.readLock().lock();
            try {
                Keyboard.BUTTON_EVENTS.getOrDefault(BUTTON,
                        Collections.emptySet()).forEach(e -> e.update(
                        buttonSnapshot));
            } finally {
                Keyboard.BUTTON_EVENTS_LOCK.readLock().unlock();
            }//end try
        });
    }//end static initializer

    public static ButtonSeeker seekNextButton(final boolean pressed) {
        ButtonSeeker buttonSeeker = new ButtonSeeker();
        if (pressed) {
            Keyboard.pressedButtonSeekers.add(buttonSeeker);
        } else {
            Keyboard.releasedButtonSeekers.add(buttonSeeker);
        }//end if

        return buttonSeeker;
    }

    static void addButtonEvent(Keyboard.Button button, ButtonEvent event) {
        Keyboard.BUTTON_EVENTS_LOCK.writeLock().lock();
        try {
            Keyboard.BUTTON_EVENTS.computeIfAbsent(button, k ->
                    new LinkedHashSet<>()).add(event);
        } finally {
            Keyboard.BUTTON_EVENTS_LOCK.writeLock().unlock();
        }//end try
    }

    static void removeButtonEvent(Keyboard.Button button, ButtonEvent event) {
        Keyboard.BUTTON_EVENTS_LOCK.writeLock().lock();
        try {
            Keyboard.BUTTON_EVENTS.getOrDefault(button, Collections.emptySet())
                                  .remove(event);
        } finally {
            Keyboard.BUTTON_EVENTS_LOCK.writeLock().unlock();
        }//end try
    }

    //trust-based, no argument validation
    private static Button getButton(int id, int scancode, boolean pressed) {
        return Keyboard.SCANCODE_TO_BUTTON.computeIfAbsent(scancode, k ->
                new Button(id, k, pressed));
    }

    private Keyboard() {
        throw new UnsupportedOperationException("You shall not pass");
    }

}//end class Keyboard
