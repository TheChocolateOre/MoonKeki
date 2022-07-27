package moonkeki.app.input;

import moonkeki.app.events.Event;
import moonkeki.app.events.InstantEventQueue;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class Keyboard {

    public enum Key implements Keyboard.Button {
        SPACE(GLFW.GLFW_KEY_SPACE), APOSTROPHE(GLFW.GLFW_KEY_APOSTROPHE),
        COMMA(GLFW.GLFW_KEY_COMMA), MINUS(GLFW.GLFW_KEY_MINUS),
        PERIOD(GLFW.GLFW_KEY_PERIOD), SLASH(GLFW.GLFW_KEY_SLASH),

        N0(GLFW.GLFW_KEY_0), N1(GLFW.GLFW_KEY_1), N2(GLFW.GLFW_KEY_2),
        N3(GLFW.GLFW_KEY_3), N4(GLFW.GLFW_KEY_4), N5(GLFW.GLFW_KEY_5),
        N6(GLFW.GLFW_KEY_6), N7(GLFW.GLFW_KEY_7), N8(GLFW.GLFW_KEY_8),
        N9(GLFW.GLFW_KEY_9),

        SEMICOLON(GLFW.GLFW_KEY_SEMICOLON),
        EQUAL(GLFW.GLFW_KEY_EQUAL),

        A(GLFW.GLFW_KEY_A), B(GLFW.GLFW_KEY_B), C(GLFW.GLFW_KEY_C),
        D(GLFW.GLFW_KEY_D), E(GLFW.GLFW_KEY_E), F(GLFW.GLFW_KEY_F),
        G(GLFW.GLFW_KEY_G), H(GLFW.GLFW_KEY_H), I(GLFW.GLFW_KEY_I),
        J(GLFW.GLFW_KEY_J), K(GLFW.GLFW_KEY_K), L(GLFW.GLFW_KEY_L),
        M(GLFW.GLFW_KEY_M), N(GLFW.GLFW_KEY_N), O(GLFW.GLFW_KEY_O),
        P(GLFW.GLFW_KEY_P), Q(GLFW.GLFW_KEY_Q), R(GLFW.GLFW_KEY_R),
        S(GLFW.GLFW_KEY_S), T(GLFW.GLFW_KEY_T), U(GLFW.GLFW_KEY_U),
        V(GLFW.GLFW_KEY_V), W(GLFW.GLFW_KEY_W), X(GLFW.GLFW_KEY_X),
        Y(GLFW.GLFW_KEY_Y), Z(GLFW.GLFW_KEY_Z),

        LEFT_BRACKET(GLFW.GLFW_KEY_LEFT_BRACKET),
        BACKSLASH(GLFW.GLFW_KEY_BACKSLASH),
        RIGHT_BRACKET(GLFW.GLFW_KEY_RIGHT_BRACKET),
        GRAVE_ACCENT(GLFW.GLFW_KEY_GRAVE_ACCENT), ESCAPE(GLFW.GLFW_KEY_ESCAPE),
        ENTER(GLFW.GLFW_KEY_ENTER), TAB(GLFW.GLFW_KEY_TAB),
        BACKSPACE(GLFW.GLFW_KEY_BACKSPACE), INSERT(GLFW.GLFW_KEY_INSERT),
        DELETE(GLFW.GLFW_KEY_DELETE),

        RIGHT(GLFW.GLFW_KEY_RIGHT), LEFT(GLFW.GLFW_KEY_LEFT),
        DOWN(GLFW.GLFW_KEY_DOWN), UP(GLFW.GLFW_KEY_UP),

        PAGE_UP(GLFW.GLFW_KEY_PAGE_UP), PAGE_DOWN(GLFW.GLFW_KEY_PAGE_DOWN),
        HOME(GLFW.GLFW_KEY_HOME), END(GLFW.GLFW_KEY_END),
        CAPS_LOCK(GLFW.GLFW_KEY_CAPS_LOCK),
        SCROLL_LOCK(GLFW.GLFW_KEY_SCROLL_LOCK),
        NUM_LOCK(GLFW.GLFW_KEY_NUM_LOCK),
        PRINT_SCREEN(GLFW.GLFW_KEY_PRINT_SCREEN), PAUSE(GLFW.GLFW_KEY_PAUSE),

        F1(GLFW.GLFW_KEY_F1), F2(GLFW.GLFW_KEY_F2), F3(GLFW.GLFW_KEY_F3),
        F4(GLFW.GLFW_KEY_F4), F5(GLFW.GLFW_KEY_F5), F6(GLFW.GLFW_KEY_F6),
        F7(GLFW.GLFW_KEY_F7), F8(GLFW.GLFW_KEY_F8), F9(GLFW.GLFW_KEY_F9),
        F10(GLFW.GLFW_KEY_F10), F11(GLFW.GLFW_KEY_F11), F12(GLFW.GLFW_KEY_F12),
        F13(GLFW.GLFW_KEY_F13), F14(GLFW.GLFW_KEY_F14), F15(GLFW.GLFW_KEY_F15),
        F16(GLFW.GLFW_KEY_F16), F17(GLFW.GLFW_KEY_F17), F18(GLFW.GLFW_KEY_F18),
        F19(GLFW.GLFW_KEY_F19), F20(GLFW.GLFW_KEY_F20), F21(GLFW.GLFW_KEY_F21),
        F22(GLFW.GLFW_KEY_F22), F23(GLFW.GLFW_KEY_F23), F24(GLFW.GLFW_KEY_F24),
        F25(GLFW.GLFW_KEY_F25),

        NP0(GLFW.GLFW_KEY_KP_0), NP1(GLFW.GLFW_KEY_KP_1),
        NP2(GLFW.GLFW_KEY_KP_2), NP3(GLFW.GLFW_KEY_KP_3),
        NP4(GLFW.GLFW_KEY_KP_4), NP5(GLFW.GLFW_KEY_KP_5),
        NP6(GLFW.GLFW_KEY_KP_6), NP7(GLFW.GLFW_KEY_KP_7),
        NP8(GLFW.GLFW_KEY_KP_8), NP9(GLFW.GLFW_KEY_KP_9),

        NP_DECIMAL(GLFW.GLFW_KEY_KP_DECIMAL),
        NP_DIVIDE(GLFW.GLFW_KEY_KP_DIVIDE),
        NP_MULTIPLY(GLFW.GLFW_KEY_KP_MULTIPLY),
        NP_SUBTRACT(GLFW.GLFW_KEY_KP_SUBTRACT), NP_ADD(GLFW.GLFW_KEY_KP_ADD),
        NP_ENTER(GLFW.GLFW_KEY_KP_ENTER), NP_EQUAL(GLFW.GLFW_KEY_KP_EQUAL),

        LEFT_SHIFT(GLFW.GLFW_KEY_LEFT_SHIFT),
        LEFT_CONTROL(GLFW.GLFW_KEY_LEFT_CONTROL),
        LEFT_ALT(GLFW.GLFW_KEY_LEFT_ALT), LEFT_SUPER(GLFW.GLFW_KEY_LEFT_SUPER),
        RIGHT_SHIFT(GLFW.GLFW_KEY_RIGHT_SHIFT),
        RIGHT_CONTROL(GLFW.GLFW_KEY_RIGHT_CONTROL),
        RIGHT_ALT(GLFW.GLFW_KEY_RIGHT_ALT),
        RIGHT_SUPER(GLFW.GLFW_KEY_RIGHT_SUPER), MENU(GLFW.GLFW_KEY_MENU);

        private static final Key[] ID_TO_KEY;
        private static final int OFFSET;
        private static final Map<Integer, Keyboard.Button> LOCAL_ID_TO_BUTTON =
                new HashMap<>(GLFW.GLFW_KEY_LAST);

        static {
            try {
                Class.forName("moonkeki.app.input.Keyboard");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            final Key[] VALUES = Key.values();
            final IntSummaryStatistics STATS =
                    Arrays.stream(VALUES)
                          .collect(Collectors.summarizingInt(k -> k.ID));
            OFFSET = -STATS.getMin();
            ID_TO_KEY = new Key[STATS.getMax() + Key.OFFSET + 1];
            for (Key k : VALUES) {
                Key.ID_TO_KEY[k.ID + Key.OFFSET] = k;
                if (k.exists()) {
                    Key.LOCAL_ID_TO_BUTTON.put(k.LOCAL_ID, k);
                }
            }
        }

        private static Key fromId(final int id) {
            final int INDEX = Objects.checkIndex(id + Key.OFFSET,
                                                 Key.ID_TO_KEY.length);
            return Key.ID_TO_KEY[INDEX];
        }

        private final int ID;
        private final int LOCAL_ID;
        private final AbstractButton ABSTRACT_BUTTON;

        Key(final int id) {
            if (id == GLFW.GLFW_KEY_UNKNOWN) {
                throw new IllegalArgumentException("Argument id has no known " +
                        "mapping.");
            }

            this.ID = id;
            this.LOCAL_ID = GLFW.glfwGetKeyScancode(id);

            this.ABSTRACT_BUTTON = LOCAL_ID != -1 ? new AbstractButton() {
                @Override
                public Button.State getState() {
                    return Key.this.getState();
                }
            } : null;
        }

        @Override
        public Event.Hub eventHub(State triggerState) {
            return this.ABSTRACT_BUTTON != null ?
                   this.ABSTRACT_BUTTON.eventHub(triggerState) :
                   Event.Hub.EMPTY;
        }

        @Override
        public InstantEventQueue.Hub instantEventQueueHub(State triggerState) {
            return this.ABSTRACT_BUTTON != null ?
                   this.ABSTRACT_BUTTON.instantEventQueueHub(triggerState) :
                   InstantEventQueue.Hub.EMPTY;
        }

        @Override
        public State getState() {
            return (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), this.ID) ==
                    GLFW.GLFW_PRESS) ? Button.State.PRESSED :
                    Button.State.RELEASED;
        }

        @Override
        public Optional<String> getSymbol() {
            return Optional.ofNullable(GLFW.glfwGetKeyName(this.ID,
                                                           this.getLocalId()
                                                               .orElse(-1)));
        }

        @Override
        public OptionalInt getLocalId() {
            return this.LOCAL_ID != -1 ? OptionalInt.of(this.LOCAL_ID) :
                                         OptionalInt.empty();
        }

        public boolean exists() {
            return this.ABSTRACT_BUTTON != null;
        }
    }

    public interface Button extends moonkeki.app.input.Button {
        static Keyboard.Button fromLocalId(int localId) {
            final Keyboard.Button BUTTON = Key.LOCAL_ID_TO_BUTTON.get(localId);
            return BUTTON != null ?
                   BUTTON :
                   Keyboard.LOCAL_BUTTONS.computeIfAbsent(localId,
                                                          LocalButton::new);
        }

        Optional<String> getSymbol();
        OptionalInt getLocalId();
    }

    //A Button that is not in Key enum
    private static final class LocalButton extends AbstractButton implements Keyboard.Button {
        final int LOCAL_ID;
        volatile Button.State state;

        LocalButton(int localId) {
            this(localId, Button.State.RELEASED);
        }

        LocalButton(int localId, Button.State state) {
            this.LOCAL_ID = localId;
            this.state = Objects.requireNonNull(state);
        }

        @Override
        public Button.State getState() {
            return this.state;
        }

        @Override
        public Optional<String> getSymbol() {
            return Optional.ofNullable(GLFW.glfwGetKeyName(
                    GLFW.GLFW_KEY_UNKNOWN, this.LOCAL_ID));
        }

        @Override
        public OptionalInt getLocalId() {
            return OptionalInt.of(this.LOCAL_ID);
        }

        @Override
        void registerEvent(State state, Instant timestamp) {
            this.state = state;
            super.registerEvent(state, timestamp);
        }

        @Deprecated
        void setState(Button.State state) {
            this.state = Objects.requireNonNull(state);
        }
    }

    private static final Map<Integer, LocalButton> LOCAL_BUTTONS =
            new ConcurrentHashMap<>();

    static {
        GLFW.glfwSetKeyCallback(GLFW.glfwGetCurrentContext(),
                                Keyboard::processEvent);
    }

    private static void processEvent(long window, int key, int scancode,
                                     int action, int mods) {
        final Instant TIMESTAMP = Instant.now();
        final Button.State STATE;
        switch (action) {
            case GLFW.GLFW_PRESS -> STATE = Button.State.PRESSED;
            case GLFW.GLFW_RELEASE -> STATE = Button.State.RELEASED;
            default -> {return;}
        }

        final AbstractButton ABSTRACT_BUTTON = (key != GLFW.GLFW_KEY_UNKNOWN) ?
                Key.fromId(key).ABSTRACT_BUTTON :
                Keyboard.LOCAL_BUTTONS.computeIfAbsent(scancode,
                        s -> new LocalButton(s, STATE));
        if (ABSTRACT_BUTTON != null) {
            ABSTRACT_BUTTON.registerEvent(STATE, TIMESTAMP);
        }
    }

    private Keyboard() {
        throw new UnsupportedOperationException("You shall not pass.");
    }

}
