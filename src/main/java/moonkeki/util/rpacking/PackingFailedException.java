package moonkeki.util.rpacking;

public class PackingFailedException extends Exception {

    public PackingFailedException() {
        super();
    }

    public PackingFailedException(String message) {
        super(message);
    }

    public PackingFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PackingFailedException(Throwable cause) {
        super(cause);
    }

}
