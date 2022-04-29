package moonkeki.render;

import moonkeki.app.Application;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract sealed class Shader implements AutoCloseable {

    public static sealed class Vertex extends Shader {

        private static final class Unclosable extends Vertex {
            private final Vertex VERTEX;

            public Unclosable(Vertex vertex) {
                this.VERTEX = vertex;
            }

            @Override
            public String toString() {
                return VERTEX.toString();
            }

            @Override
            public boolean isClosed() {
                return VERTEX.isClosed();
            }

            @Override
            public void close() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean equals(Object obj) {
                return VERTEX.equals(obj);
            }

            @Override
            public int hashCode() {
                return VERTEX.hashCode();
            }

            @Override
            public int getId() {
                return VERTEX.getId();
            }
        }

        private static final Vertex DEFAULT_CLOSABLE = Vertex.ofSource(
                """
                #version 330 core

                in vec2 position;
                in vec2 v_texCoord;
                out vec2 texCoord;
                uniform mat3 transformMatrix;

                void main() {
                    texCoord = v_texCoord;
                    gl_Position = vec4(transformMatrix * vec3(position, 1.0f), 
                            1.0f);
                }
                """
        );
        public static final Vertex DEFAULT = new Unclosable(
                Vertex.DEFAULT_CLOSABLE);

        static {
            Application.closeOnExit(Shader.Vertex.DEFAULT_CLOSABLE);
        }

        public static Vertex fromPath(String path) throws IOException {
            return Vertex.ofSource(Files.readString(Path.of(path)));
        }

        public static Vertex ofSource(String source) {
            return new Vertex(Shader.createShader(GL20.GL_VERTEX_SHADER,
                    source));
        }

        //Only for wrapper ShaderProgram's'
        private Vertex() {}

        private Vertex(int id) {
            super(id);
        }

        @Override
        public String toString() {
            return "Vertex Shader %d".formatted(this.getId());
        }

    }

    public static sealed class Fragment extends Shader {

        private static final class Unclosable extends Fragment {
            private final Fragment FRAGMENT;

            public Unclosable(Fragment vertex) {
                this.FRAGMENT = vertex;
            }

            @Override
            public String toString() {
                return FRAGMENT.toString();
            }

            @Override
            public boolean isClosed() {
                return FRAGMENT.isClosed();
            }

            @Override
            public void close() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean equals(Object obj) {
                return FRAGMENT.equals(obj);
            }

            @Override
            public int hashCode() {
                return FRAGMENT.hashCode();
            }

            @Override
            public int getId() {
                return FRAGMENT.getId();
            }
        }

        private static final Fragment DEFAULT_CLOSABLE = Fragment.ofSource(
                """
                #version 330 core

                in vec2 texCoord;
                out vec4 fragColor;
                uniform sampler2D sampler;
                
                void main() {
                    fragColor = texture(sampler, texCoord);
                }
                """
        );
        public static final Fragment DEFAULT = new Unclosable(
                Fragment.DEFAULT_CLOSABLE);

        static {
            Application.closeOnExit(Shader.Fragment.DEFAULT_CLOSABLE);
        }

        public static Fragment fromPath(String path) throws IOException {
            return Fragment.ofSource(Files.readString(Path.of(path)));
        }

        public static Fragment ofSource(String source) {
            return new Fragment(Shader.createShader(GL20.GL_FRAGMENT_SHADER,
                    source));
        }

        //Only for wrapper Shader's'
        private Fragment() {}

        private Fragment(int id) {
            super(id);
        }

        @Override
        public String toString() {
            return "Fragment Shader %d".formatted(this.getId());
        }

    }

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

    //Only for wrapper Shader's'
    private Shader() {
        this.ID = -1;
    }

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

}
