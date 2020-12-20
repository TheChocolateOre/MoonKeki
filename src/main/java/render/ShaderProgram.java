package render;

import org.lwjgl.opengl.GL20;

import java.util.Collection;
import java.util.List;

public final class ShaderProgram implements AutoCloseable {

    private final int ID;
    private boolean closed;

    public ShaderProgram(Shader.Vertex vertexShader, Shader.Fragment
            fragmentShader) {
        this(List.of(vertexShader, fragmentShader));
    }

    //Creates a linked ShaderProgram, the shaders are detached
    public ShaderProgram(Collection<? extends Shader> shaders) {
        if (shaders.size() < 2) {
            throw new IllegalStateException("Argument Collection shaders " +
                    "must have a size of at least 2.");
        }//end if

        boolean hasVertex = false;
        boolean hasFragment = false;
        for (Shader s : shaders) {
            if (s instanceof Shader.Vertex) {
                hasVertex = true;
            } else if (s instanceof Shader.Fragment) {
                hasFragment = true;
            }//end if
        }//end for

        if (!(hasVertex & hasFragment)) {
            throw new IllegalArgumentException("Argument Collection shaders " +
                    "must contain at least 1 Shader.Vertex and 1 " +
                    "Shader.Fragment Shader.");
        }//end if

        this.ID = GL20.glCreateProgram();
        shaders.forEach(s -> GL20.glAttachShader(this.ID, s.getId()));
        GL20.glLinkProgram(this.ID);
        shaders.forEach(s -> GL20.glDetachShader(this.ID, s.getId()));
    }

    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            return;
        }//end if

        GL20.glDeleteProgram(this.getId());
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

        return (obj instanceof ShaderProgram p) && this.getId() == p.getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.getId());
    }

    @Override
    public String toString() {
        return "Shader Program %d".formatted(this.getId());
    }

    void use() {
        GL20.glUseProgram(this.getId());
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

}//end class ShaderProgram