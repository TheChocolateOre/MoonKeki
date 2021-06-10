package app.input;

import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Mouse {

    public static final class Button extends app.input.Button {
        public static final Mouse.Button LEFT = new Button(
                GLFW.GLFW_MOUSE_BUTTON_LEFT);
        public static final Mouse.Button MIDDLE = new Button(
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        public static final Mouse.Button RIGHT = new Button(
                GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        public static final Mouse.Button N4 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_4);
        public static final Mouse.Button N5 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_5);
        public static final Mouse.Button N6 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_6);
        public static final Mouse.Button N7 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_7);
        public static final Mouse.Button N8 = new Button(
                GLFW.GLFW_MOUSE_BUTTON_8);

        private Button(final int id) {
            super(id);
        }

        @Override
        public boolean isPressed() {
            return GLFW.glfwGetMouseButton(GLFW.glfwGetCurrentContext(),
                    this.ID) == GLFW.GLFW_PRESS;
        }

        private static Optional<Mouse.Button> fromId(final int id) {
            return Optional.ofNullable(switch (id) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> Mouse.Button.LEFT;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> Mouse.Button.MIDDLE;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> Mouse.Button.RIGHT;
                case GLFW.GLFW_MOUSE_BUTTON_4 -> Mouse.Button.N4;
                case GLFW.GLFW_MOUSE_BUTTON_5 -> Mouse.Button.N5;
                case GLFW.GLFW_MOUSE_BUTTON_6 -> Mouse.Button.N6;
                case GLFW.GLFW_MOUSE_BUTTON_7 -> Mouse.Button.N7;
                case GLFW.GLFW_MOUSE_BUTTON_8 -> Mouse.Button.N8;
                default -> null;
            });
        }
    }//end static nested class Button
    
    private static class PositionEvent extends Event {
    
        final BiPredicate<Double, Double> HOVER_PREDICATE;
        
        PositionEvent(boolean isHappening, BiPredicate<Double, Double> 
                      hoverPredicate) {
            super(isHappening);
            this.HOVER_PREDICATE = hoverPredicate;
        }
        
    }//end static nested class PositionEvent
    
    public record Position(double x, double y) {}

    //id -> button (only for non-named buttons, if exist)
    private static final Map<Integer, Mouse.Button> ID_TO_BUTTON =
            new ConcurrentHashMap<>();

    private static final ReadWriteLock PRESSED_EVENTS_LOCK =
            new ReentrantReadWriteLock(true);
    private static final ReadWriteLock RELEASED_EVENTS_LOCK =
            new ReentrantReadWriteLock(true);
    private static final ReadWriteLock POSITION_EVENTS_LOCK =
            new ReentrantReadWriteLock(true);
    private static final Map<Mouse.Button, Set<Event>> PRESSED_EVENTS =
            new HashMap<>();
    private static final Map<Mouse.Button, Set<Event>> RELEASED_EVENTS =
            new HashMap<>();
    private static final Collection<PositionEvent> POSITION_EVENTS =
            new LinkedHashSet<>();

    private static final Queue<ButtonSeeker> pressedButtonSeekers =
            new ConcurrentLinkedQueue<>();
    private static final Queue<ButtonSeeker> releasedButtonSeekers =
            new ConcurrentLinkedQueue<>();

    static {
        GLFW.glfwSetMouseButtonCallback(GLFW.glfwGetCurrentContext(), (long
                window, int button, int action, int mods) -> {
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
            final Mouse.Button BUTTON = Mouse.Button.fromId(button)
                    .orElseGet(() -> Mouse.getButton(button));

            //We process the ButtonSeeker's'
            ButtonSeeker buttonSeeker = PRESSED ?
                    Mouse.pressedButtonSeekers.poll() :
                    Mouse.releasedButtonSeekers.poll();
            if (buttonSeeker != null) {
                buttonSeeker.putIfAbsent(BUTTON);
            }//end if
            
            //We process the Event's'
            final ReadWriteLock LOCK;
            final Collection<Event> EVENTS;
            final ReadWriteLock OTHER_LOCK;
            final Collection<Event> OTHER_EVENTS;
            if (PRESSED) {
                LOCK = Mouse.PRESSED_EVENTS_LOCK;
                EVENTS = Mouse.PRESSED_EVENTS.getOrDefault(BUTTON,
                        Collections.emptySet());
                OTHER_LOCK = Mouse.RELEASED_EVENTS_LOCK;
                OTHER_EVENTS = Mouse.RELEASED_EVENTS.getOrDefault(BUTTON,
                        Collections.emptySet());
            } else {
                LOCK = Mouse.RELEASED_EVENTS_LOCK;
                EVENTS = Mouse.RELEASED_EVENTS.getOrDefault(BUTTON,
                        Collections.emptySet());
                OTHER_LOCK = Mouse.PRESSED_EVENTS_LOCK;
                OTHER_EVENTS = Mouse.PRESSED_EVENTS.getOrDefault(BUTTON,
                        Collections.emptySet());
            }//end if

            LOCK.readLock().lock();
            try {
                EVENTS.forEach(Event::start);
            } finally {
                LOCK.readLock().unlock();
            }//end try

            OTHER_LOCK.readLock().lock();
            try {
                OTHER_EVENTS.forEach(Event::stop);
            } finally {
                OTHER_LOCK.readLock().unlock();
            }//end try
        });
        
        GLFW.glfwSetCursorPosCallback(GLFW.glfwGetCurrentContext(), (long 
                window, double x, double y) -> {
            POSITION_EVENTS_LOCK.readLock().lock();
            try {
                Mouse.POSITION_EVENTS.forEach(e -> {
                    if (e.POSITION_PREDICATE.test(x, y)) {
                        e.start();
                    } else {
                        e.stop();
                    }//end if
                });
            } finally {
                POSITION_EVENTS_LOCK.readLock().unlock();
            }//end try
        });
    }//end static initializer
    
    public static Event.Consumer buttonEvent(Mouse.Button button, 
                                             boolean pressed) {
        final ReadWriteLock LOCK;
        final Map<Mouse.Button, Set<Event>> EVENTS;
        if (pressed) {
            LOCK = Mouse.PRESSED_EVENTS_LOCK;
            EVENTS = Mouse.PRESSED_EVENTS;
        } else {
            LOCK = Mouse.RELEASED_EVENTS_LOCK;
            EVENTS = Mouse.RELEASED_EVENTS;
        }//end if
        final Event EVENT = new Event(button.isPressed() == pressed) {
            @Override
            public void close() {
                super.close();
                LOCK.writeLock().lock();
                try {
                    EVENTS.getOrDefault(button, Collections.emptySet())
                          .remove(this);
                } finally {
                    LOCK.writeLock().unlock();
                }//end try
            }
        };

        LOCK.writeLock().lock();
        try {
            EVENTS.computeIfAbsent(button, k -> new LinkedHashSet<>())
                  .add(EVENT);
        } finally {
            LOCK.writeLock().unlock();
        }//end try

        return EVENT.asConsumer();
    }
    
    public static Event.Consumer positionEvent(BiPredicate<Double, Double> 
                                               hoverPredicate) {
        final AffineTransform TRANSFORM = Mouse.getTransform();
        final BiPredicate<Double, Double> TRASNFORMED_PREDICATE = (x, y) -> {
            Point2D p = new Point2D.Double();
            TRANSFORM.transform(new Point2D.Double(x, y), p);
            return hoverPredicate.test(p.getX(), p.getY());
        };
        Mouse.Position mousePosition = Mouse.getMousePosition();
        final Event POSITION_EVENT = new PositionEvent(TRASNFORMED_PREDICATE.test(
                mousePosition.x, mousePosition.y), TRASNFORMED_PREDICATE) {
            @Override
            public void close() {
                super.close();
                Mouse.POSITION_EVENTS_LOCK.writeLock().lock();
                try {
                    Mouse.POSITION_EVENTS.remove(this);
                } finally {
                    Mouse.POSITION_EVENTS_LOCK.writeLock().unlock();
                }//end try
            }
        };

        Mouse.POSITION_EVENTS_LOCK.writeLock().lock();
        try {
            Mouse.POSITION_EVENTS.add(POSITION_EVENT);
        } finally {
            Mouse.POSITION_EVENTS_LOCK.writeLock().unlock();
        }//end try

        return EVENT.asConsumer();
    }
    
    public static Mouse.Position getMousePosition() {
        final double[] xCache = new double[1];
        final double[] yCache = new double[1];
        GLFW.glfwGetCursorPos(GLFW.glfwGetCurrentContext(), xCache, yCache);
        return new Mouse.Position(xCache[0], yCache[0]);
    }

    public static ButtonSeeker seekNextButton(final boolean pressed) {
        ButtonSeeker buttonSeeker = new ButtonSeeker();
        if (pressed) {
            Mouse.pressedButtonSeekers.add(buttonSeeker);
        } else {
            Mouse.releasedButtonSeekers.add(buttonSeeker);
        }//end if

        return buttonSeeker;
    }

    //trust-based, no argument validation
    private static Mouse.Button getButton(int id) {
        return Mouse.ID_TO_BUTTON.computeIfAbsent(id, Button::new);
    }
    
    //screen coordinates -> framebuffer
    private static AffineTransform getTransform() {
        int[] winWidthCache = new int[1];
        int[] winHeightCache = new int[1];
        int[] fbWidthCache = new int[1];
        int[] fbHeightCache = new int[1];
        GLFW.glfwGetWindowSize(GLFW.glfwGetCurrentContext(), winWidthCache, winHeightCache);
        GLFW.glfwGetFramebufferSize(GLFW.glfwGetCurrentContext(), fbWidthCache, fbHeightCache);
        
        AffineTransform transform = AffineTransform.getScaleInstance((double) fbWidthCache[0] / 
                winWidthCache[0], (double) fbHeightCache[0] / winHeightCache[0]);
        transform.translate(0, winHeightCache[0]);
        transform.scale(1.0, -1.0);
        
        return transform;
    }

    private Mouse() {
        throw new UnsupportedOperationException("You shall not pass");
    }

}//end Mouse class
