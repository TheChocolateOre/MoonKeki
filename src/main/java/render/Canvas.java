package render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public abstract sealed class Canvas permits Canvas.ScreenRegion, Pixmap {

    public static abstract non-sealed class ScreenRegion extends Canvas {
        @Override
        void setup(int framebufferId) {
            super.setup(0);
        }

        @Override
        void copyTo(Pixmap destination, int sourceFramebufferId) {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            destination.bind();
            GL20.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
                    destination.getXOffset(), destination.getYOffset(),
                    this.getXOffset(), this.getYOffset(), this.getWidth(),
                    this.getHeight());
        }

        @Override
        protected abstract int getXOffset();
        @Override
        protected abstract int getYOffset();
        @Override
        protected abstract int getWidth();
        @Override
        protected abstract int getHeight();
    }//end static nested class ScreenRegion

    void setup(int framebufferId) {
        GL11.glViewport(this.getXOffset(), this.getYOffset(), this.getWidth(),
                this.getHeight());
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(this.getXOffset(), this.getYOffset(), this.getWidth(),
                this.getHeight());
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
    }

    boolean isVoid() {
        return this.getWidth() == 0 || this.getHeight() == 0;
    }

    abstract int getXOffset();
    abstract int getYOffset();
    abstract int getWidth();
    abstract int getHeight();
    abstract void copyTo(Pixmap destination, int sourceFramebufferId);

}//end class Canvas