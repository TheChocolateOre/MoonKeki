package render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public abstract sealed class Shader implements AutoCloseable {

    public static final class Vertex extends Shader {

        public static Vertex fromPath(String path) throws IOException {
            return Vertex.ofSource(Files.readString(Path.of(path)));
        }

        public static Vertex ofSource(String source) {
            return new Vertex(Shader.createShader(GL20.GL_VERTEX_SHADER,
                    source));
        }

        private Vertex(int id) {
            super(id);
        }

        @Override
        public String toString() {
            return "Vertex Shader %d".formatted(this.getId());
        }

    }//end inner class VertexShader

    public static final class Fragment extends Shader {

        public static Fragment fromPath(String path) throws IOException {
            return Fragment.ofSource(Files.readString(Path.of(path)));
        }

        public static Fragment ofSource(String source) {
            return new Fragment(Shader.createShader(GL20.GL_FRAGMENT_SHADER,
                    source));
        }

        private Fragment(int id) {
            super(id);
        }

        @Override
        public String toString() {
            return "Fragment Shader %d".formatted(this.getId());
        }

    }//end inner class FragmentShader

    private static int createShader(int type, String source) {
        final int ID = GL20.glCreateShader(type);
        GL20.glShaderSource(ID, source);
        GL20.glCompileShader(ID);

        if (GL20.glGetShaderi(ID, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            throw new IllegalArgumentException(GL20.glGetShaderInfoLog(ID));
        }//end if

        return ID;
    }

    private final int ID;
    private boolean closed;

    Shader(int id) {
        this.ID = id;
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            return;
        }//end if

        GL20.glDeleteShader(this.getId());
        this.closed = true;
    }

    @Override
    public boolean equals(Object obj) {
        this.ensureOpen();

        if (this == obj) {
            return true;
        }//end if

        if (null == obj) {
            return false;
        }//end if

        return (obj instanceof Shader s) && this.getId() == s.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.getId());
    }

    int getId() {
        this.ensureOpen();
        return this.ID;
    }

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Shader is closed.");
        }//end if
    }

}//end class Shader