import app.Application;
import app.Event;
import app.Screen;
import app.input.*;
import render.Canvas;
import render.Renderer;
import render.Texture;

import java.awt.*;
import java.io.IOException;

public class FullTest {

    public static void main(String[] args) {
        Application.configuration()
                   .setWindowPosition(((monitorWidth, monitorHeight,
                                        windowWidth, windowHeight) -> new
                           Application.Builder.Position((monitorWidth -
                           windowWidth) / 2, (monitorHeight - windowHeight) /
                           2)))
                   .setWindowSize((monitorWidth, monitorHeight) -> new
                           Application.Builder.Size((int) (monitorWidth / 1.5),
                           (int) (monitorHeight / 1.5)))
                   .setWindowTitle("TestTitle")
                   .build((w, h) -> new Screen(100, 50, 800, 500,
                           new Screen.Camera.Simple(0, 0, 800, 500), 1) {
                       private Texture texture;
                       private double x;
                       private int direction = 1;
                       private boolean mirrorX;
                       private Event.Consumer buttonEvent = Event.composite()
                               .add(Keyboard.buttonEvent(Keyboard.Key.A, true))
                               .add(Keyboard.buttonEvent(Keyboard.Key.X, true))
                               .add(Keyboard.buttonEvent(Keyboard.Key.Z, true))
                               .build();

                       {
                           try {
                               texture = new Texture("C:\\Users\\" +
                                       "InfiniteMachine\\Pictures\\Temp\\shader_test.png");
                           } catch (IOException e) {
                               e.printStackTrace();
                           }//end try
                       }

                       @Override
                       protected void onUpdate(double dt) {
                           this.x += dt * 100 * this.direction;
                           if (this.x >= 500) {
                               this.direction = -1;
                           } else if (this.x <= 0) {
                               this.direction = 1;
                           }//end if

                           if (this.buttonEvent.hasOccurred()) {
                               this.mirrorX = !this.mirrorX;
                           }//end if
                       }

                       @Override
                       protected void onPause() {
                           System.out.println("Paused.");
                       }

                       @Override
                       protected void onResume() {
                           System.out.println("Resumed.");
                       }

                       @Override
                       public void draw(Renderer renderer) {
                           renderer.setCanvas(Canvas.WINDOW);
                           renderer.clearCanvas(Color.BLACK);

                           renderer.setCanvas(this);
                           renderer.clearCanvas(Color.GREEN);
                           renderer.draw(this.texture, (float) this.x, 50, 500, 400,
                                   this.mirrorX, false);
                           renderer.flush();
                       }

                       @Override
                       public void close() {
                           super.close();
                           this.texture.close();
                           //this.buttonEvent.close();
                       }

                       @Override
                       public void onWindowResize(int windowWidth, int windowHeight) {
                           this.setXOffset((windowWidth - this.getWidth()) / 2);
                           this.setYOffset((windowHeight - this.getHeight()) / 2);
                       }
                   });
    }

}
