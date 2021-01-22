package render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

abstract class Canvas {

    static record Bounds(int x, int y, int width, int height) {
        Bounds {
            if (width < 0) {
                throw new IllegalArgumentException("Argument width can't be " +
                        "negative.");
            }//end if

            if (height < 0) {
                throw new IllegalArgumentException("Argument height can't be " +
                        "negative.");
            }//end if
        }
    }//end static nested record Bounds

    void setup(int framebufferId) {
        Bounds bounds = this.getBounds();
        GL11.glViewport(bounds.x, bounds.y, bounds.width, bounds.height);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(bounds.x, bounds.y, bounds.width, bounds.height);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
    }

    abstract void copyTo(Pixmap destination, int sourceFramebufferId);
    abstract Canvas.Bounds getBounds();
    abstract boolean isVoid();
    abstract int getId();

}//end class Canvas