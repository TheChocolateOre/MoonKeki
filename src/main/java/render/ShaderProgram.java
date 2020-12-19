package render;

import org.lwjgl.opengl.GL20;

import java.util.Collection;
import java.util.Collections;

class ShaderProgram implements AutoCloseable {

    private final int ID;
    private boolean closed;
    private boolean linked;

    //The returned ShaderProgram is linked, the shaders are detached
    static ShaderProgram of(Collection<? extends Shader> shaders) {
        ShaderProgram program = new ShaderProgram(shaders).link();
        shaders.forEach(program::detach);
        return program;
    }

    //Creates a non-linked ShaderProgram
    ShaderProgram() {
        this(Collections.emptyList());
    }

    //Creates a non-linked ShaderProgram, the shaders are attached
    ShaderProgram(Collection<? extends Shader> shaders) {
        this.ID = GL20.glCreateProgram();
        shaders.forEach(s -> GL20.glAttachShader(this.ID, s.getId()));
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

    ShaderProgram attach(Shader shader) {
        GL20.glAttachShader(this.getId(), shader.getId());
        this.linked = false;
        return this;
    }

    ShaderProgram detach(Shader shader) {
        GL20.glDetachShader(this.getId(), shader.getId());
        return this;
    }

    ShaderProgram link() {
        GL20.glLinkProgram(this.getId());
        this.linked = true;
        return this;
    }

    void use() {
        GL20.glUseProgram(this.getId());
    }

    int getId() {
        this.ensureOpen();
        return this.ID;
    }

    boolean isLinked() {
        return this.linked;
    }

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Shader is closed.");
        }//end if
    }

}//end class ShaderProgram