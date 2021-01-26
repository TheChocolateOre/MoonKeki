package render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public abstract sealed class Canvas permits Canvas.ScreenRegion, Pixmap {

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

    public static abstract non-sealed class ScreenRegion extends Canvas {
        @Override
        void setup(int framebufferId) {
            super.setup(0);
        }

        @Override
        void copyTo(Pixmap destination, int sourceFramebufferId) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            Canvas.Bounds bounds = this.getBounds();
            destination.bind();
            GL20.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
                    destination.getXOffset(), destination.getYOffset(),
                    bounds.x(), bounds.y(), bounds.width(), bounds.height());
        }

        @Override
        boolean isVoid() {
            Canvas.Bounds bounds = this.getBounds();
            return bounds.width == 0 || bounds.height == 0;
        }

        @Override
        protected abstract Canvas.Bounds getBounds();
    }//end static nested class ScreenRegion

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

}//end class Canvas