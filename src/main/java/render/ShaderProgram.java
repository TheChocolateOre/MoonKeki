package render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.stream.IntStream;

public final class ShaderProgram implements AutoCloseable {

    private static final int UNIFORM_CACHE_SIZE = 50;
    private final int ID;
    private boolean closed;
    private Map<String, Integer> uniformLocations = new LinkedHashMap<>(
            UNIFORM_CACHE_SIZE, 0.75f, true);

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
        shaders.forEach(s -> GL20.glDetachShader(this.ID, s.getId()));

        //TODO Setup the attributes properly
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
    }

    int getId() {
        this.ensureOpen();
        return this.ID;
    }

    private int getUniformLocation(String name) {
        Integer location = this.uniformLocations.get(name);
        if (location != null) {
            return location;
        }//end if

        if (this.uniformLocations.size() == ShaderProgram.UNIFORM_CACHE_SIZE) {
            Iterator<?> itr = this.uniformLocations.entrySet().iterator();
            itr.next();
            itr.remove();
        }//end if

        final int LOCATION = GL20.glGetUniformLocation(this.getId(), name);
        if (LOCATION == -1) {
            throw new IllegalArgumentException("Argument name does not " +
                    "correspond to a uniform variable in this ShaderProgram.");
        }//end if

        this.uniformLocations.put(name, LOCATION);
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

}//end class ShaderProgram