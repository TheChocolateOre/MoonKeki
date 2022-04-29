package moonkeki.render;

import moonkeki.app.Application;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.stream.IntStream;

public sealed class ShaderProgram implements AutoCloseable {

    private static final class Unclosable extends ShaderProgram {
        private final ShaderProgram PROGRAM;

        public Unclosable(ShaderProgram program) {
            this.PROGRAM = program;
        }

        @Override
        public void setUniformVariable(String name, boolean value) {
            PROGRAM.setUniformVariable(name, value);
        }

        @Override
        public void setUniformVariable(String name, int value) {
            PROGRAM.setUniformVariable(name, value);
        }

        @Override
        public void setUniformVariable(String name, float value) {
            PROGRAM.setUniformVariable(name, value);
        }

        @Override
        public void setUniformArray(String name, boolean[] values) {
            PROGRAM.setUniformArray(name, values);
        }

        @Override
        public void setUniformArray(String name, int[] values) {
            PROGRAM.setUniformArray(name, values);
        }

        @Override
        public void setUniformArray(String name, float[] values) {
            PROGRAM.setUniformArray(name, values);
        }

        @Override
        public void setUniformMatrix(String name, AffineTransform value) {
            PROGRAM.setUniformMatrix(name, value);
        }

        @Override
        public boolean isClosed() {
            return PROGRAM.isClosed();
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object obj) {
            return PROGRAM.equals(obj);
        }

        @Override
        public int hashCode() {
            return PROGRAM.hashCode();
        }

        @Override
        public String toString() {
            return PROGRAM.toString();
        }

        @Override
        public void use() {
            PROGRAM.use();
        }

        @Override
        public int getId() {
            return PROGRAM.getId();
        }
    }

    private static final ShaderProgram DEFAULT_CLOSABLE = new ShaderProgram(
            Shader.Vertex.DEFAULT, Shader.Fragment.DEFAULT);
    public static final ShaderProgram DEFAULT = new Unclosable(
            ShaderProgram.DEFAULT_CLOSABLE);
    private static final int UNIFORM_CACHE_SIZE = 50;
    private final int ID;
    private final Map<String, Integer> UNIFORM_LOCATIONS = new LinkedHashMap<>(
            UNIFORM_CACHE_SIZE, 0.75f, true);
    private boolean closed;

    static {
        Application.closeOnExit(ShaderProgram.DEFAULT_CLOSABLE);
    }

    //Only for wrapper ShaderProgram's'
    private ShaderProgram() {
        this.ID = -1;
    }

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

            if (hasVertex && hasFragment) {
                break;
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
        if (GL20.glGetProgrami(this.ID, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            throw new RuntimeException(GL20.glGetProgramInfoLog(this.ID));
        }//end if
        shaders.forEach(s -> GL20.glDetachShader(this.ID, s.getId()));
    }

    public void setUniformVariable(String name, boolean value) {
        this.runOnThisProgram(() -> GL20.glUniform1i(this.getUniformLocation(
                name), value ? 1 : 0));
    }

    public void setUniformVariable(String name, int value) {
        this.runOnThisProgram(() -> GL20.glUniform1i(this.getUniformLocation(
                name), value));
    }

    public void setUniformVariable(String name, float value) {
        this.runOnThisProgram(() -> GL20.glUniform1f(this.getUniformLocation(
                name), value));
    }

    public void setUniformArray(String name, boolean[] values) {
        this.setUniformArray(name, IntStream.range(0, values.length)
                .map(i -> values[i] ? 1 : 0)
                .toArray());
    }

    public void setUniformArray(String name, int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Argument array values can't " +
                    "have a length of 0.");
        }//end if

        this.runOnThisProgram(() -> GL20.glUniform1iv(this.getUniformLocation(
                name), values));
    }

    public void setUniformArray(String name, float[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("Argument array values can't " +
                    "have a length of 0.");
        }//end if

        this.runOnThisProgram(() -> GL20.glUniform1fv(this.getUniformLocation(
                name), values));
    }

    public void setUniformMatrix(String name, AffineTransform value) {
        final int UNIFORM_LOCATION = this.getUniformLocation(name);

        float[] data = new float[3 * 3];
        double[] matrix = new double[6];
        value.getMatrix(matrix);
        data[0] = (float) matrix[0];
        data[1] = (float) matrix[1];
        data[3] = (float) matrix[2];
        data[4] = (float) matrix[3];
        data[6] = (float) matrix[4];
        data[7] = (float) matrix[5];
        data[8] = 1.0f;

        this.runOnThisProgram(() -> GL20.glUniformMatrix3fv(UNIFORM_LOCATION,
                false, data));
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

        final int POSITION = GL20.glGetAttribLocation(this.ID, "position");
        GL20.glEnableVertexAttribArray(POSITION);
        GL20.glVertexAttribPointer(POSITION, 2, GL11.GL_FLOAT, false,
                4 * Float.BYTES, 0);

        final int TEX_COORD = GL20.glGetAttribLocation(this.ID, "v_texCoord");
        GL20.glEnableVertexAttribArray(TEX_COORD);
        GL20.glVertexAttribPointer(TEX_COORD, 2, GL11.GL_FLOAT, false,
                4 * Float.BYTES, 2 * Float.BYTES);
    }

    int getId() {
        this.ensureOpen();
        return this.ID;
    }

    private int getUniformLocation(String name) {
        Integer location = this.UNIFORM_LOCATIONS.get(name);
        if (location != null) {
            return location;
        }//end if

        if (this.UNIFORM_LOCATIONS.size() == ShaderProgram.UNIFORM_CACHE_SIZE) {
            Iterator<?> itr = this.UNIFORM_LOCATIONS.entrySet().iterator();
            itr.next();
            itr.remove();
        }//end if

        final int LOCATION = GL20.glGetUniformLocation(this.getId(), name);
        if (LOCATION == -1) {
            throw new IllegalArgumentException("Argument name does not " +
                    "correspond to a uniform variable in this ShaderProgram.");
        }//end if

        this.UNIFORM_LOCATIONS.put(name, LOCATION);
        return LOCATION;
    }

    private void runOnThisProgram(Runnable action) {
        final int PREV_PROGRAM = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        this.use();
        action.run();
        GL20.glUseProgram(PREV_PROGRAM);
    }

    private void ensureOpen() throws IllegalStateException {
        if (this.isClosed()) {
            throw new IllegalStateException("This Shader is closed.");
        }//end if
    }

}
