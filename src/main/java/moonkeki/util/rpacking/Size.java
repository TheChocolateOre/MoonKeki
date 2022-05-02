package moonkeki.util.rpacking;

public record Size(int width, int height) {

    public Size {
        if (width < 1) {
            throw new IllegalArgumentException("The width must be " +
                    "positive.");
        }//end if

        if (height < 1) {
            throw new IllegalArgumentException("The height must be " +
                    "positive.");
        }//end if
    }

}
