package org.westmalle.wayland.egl;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.google.common.collect.Maps;
import com.sun.jna.Memory;
import org.freedesktop.wayland.server.ShmBuffer;
import org.freedesktop.wayland.server.WlBufferResource;
import org.freedesktop.wayland.server.WlSurfaceResource;
import org.freedesktop.wayland.shared.WlShmFormat;
import org.westmalle.wayland.nativ.Libgles2;
import org.westmalle.wayland.output.*;
import org.westmalle.wayland.output.calc.Mat4;
import org.westmalle.wayland.protocol.WlOutput;
import org.westmalle.wayland.protocol.WlSurface;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.westmalle.wayland.nativ.Libgles2.*;

@AutoFactory(className = "EglRenderEngineFactory")
public class EglRenderEngine implements RenderEngine {

    @Nonnull
    private static final String SURFACE_V          =
            "uniform mat4 mu_projection;\n" +
            "\n" +
            "attribute vec2 va_position;\n" +
            "attribute vec2 va_texcoord;\n" +
            "\n" +
            "varying vec2 vv_texcoord;\n" +
            "\n" +
            "void main(){\n" +
            "    vv_texcoord = va_texcoord;\n" +
            "    gl_Position = vec4(va_position, 0.0, 1.0) * mu_projection;\n" +
            "}";
    @Nonnull
    private static final String SURFACE_ARGB8888_F =
            "varying vec2 vv_texcoord;\n" +
            "uniform sampler2D tex;\n" +
            "\n" +
            "void main(){\n" +
            "    gl_FragColor = texture2D(tex, vv_texcoord);\n" +
            "}";
    @Nonnull
    private static final String SURFACE_XRGB8888_F =
            "varying vec2 vv_texcoord;\n" +
            "uniform sampler2D tex;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor.rgb = texture2D(tex, vv_texcoord).rgb;\n" +
            "    gl_FragColor.a = 1.;\n" +
            "}";

    @Nonnull
    private final Map<WlSurfaceResource, Gles2SurfaceData> cachedSurfaceData = new WeakHashMap<>();
    @Nonnull
    private final Map<Gles2BufferFormat, Integer>          shaderPrograms    = Maps.newHashMap();

    @Nonnull
    private final ExecutorService renderThread;
    @Nonnull
    private final Libgles2        libgles2;

    private Memory bufferData;
    private Memory elementBuffer;
    private Memory vertexBuffer;
    private Mat4   projection;

    EglRenderEngine(@Provided @Nonnull final Libgles2 libgles2) {
        this.renderThread = Executors.newSingleThreadExecutor(r -> new Thread(r,
                                                                              "GL Render Engine"));
        this.libgles2 = libgles2;
    }

    @Override
    public void begin(@Nonnull final WlOutput wlOutput) {
        this.renderThread.submit(() -> doBegin(wlOutput));
    }

    private void doBegin(@Nonnull final WlOutput wlOutput) {
        final Output       output       = wlOutput.getOutput();
        final OutputMode   mode         = output.getMode();
        final HasEglOutput hasEglOutput = (HasEglOutput) output.getImplementation();
        final EglOutput    eglOutput    = hasEglOutput.getEglOutput();
        eglOutput.begin();

        final int surfaceWidth  = mode.getWidth();
        final int surfaceHeight = mode.getHeight();
        //@formatter:off
        this.projection = Mat4.create(2.0f / surfaceWidth, 0,                     0, -1,
                                      0,                   2.0f / -surfaceHeight, 0,  1,
                                      0,                   0,                     1,  0,
                                      0,                   0,                     0,  1);
        //@formatter:on
        this.libgles2.glViewport(0,
                                 0,
                                 surfaceWidth,
                                 surfaceHeight);
        this.libgles2.glClear(GL_COLOR_BUFFER_BIT);
        //define triangles to be drawn.
        //make element buffer active
        this.libgles2.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,
                                   getElementBuffer().getInt(0));

        this.libgles2.glBufferData(GL_ELEMENT_ARRAY_BUFFER,
                                   getBufferData().size(),
                                   getBufferData(),
                                   GL_DYNAMIC_DRAW);
        //make vertexBuffer active
        this.libgles2.glBindBuffer(GL_ARRAY_BUFFER,
                                   getVertexBuffer().getInt(0));
    }

    @Nonnull
    private Memory getBufferData() {
        if (this.bufferData == null) {
            final int[] elements = new int[]{0, 1, 2,
                                             2, 3, 0
            };
            this.bufferData = new Memory(Integer.BYTES * elements.length);
            this.bufferData.write(0,
                                  elements,
                                  0,
                                  elements.length);
        }
        return this.bufferData;
    }

    @Nonnull
    private Memory getElementBuffer() {
        if (this.elementBuffer == null) {
            final Memory elementBuffer = new Memory(Integer.BYTES);
            this.libgles2.glGenBuffers(1,
                                       elementBuffer);
            this.elementBuffer = elementBuffer;
        }
        return this.elementBuffer;
    }

    @Nonnull
    private Memory getVertexBuffer() {
        if (this.vertexBuffer == null) {
            final Memory vertexBuffer = new Memory(Integer.BYTES);
            this.libgles2.glGenBuffers(1,
                                       vertexBuffer);
            this.vertexBuffer = vertexBuffer;
        }
        return this.vertexBuffer;
    }

    @Override
    public void draw(@Nonnull final WlSurfaceResource surfaceResource,
                     @Nonnull final WlBufferResource wlBufferResource) {
        this.renderThread.submit(() -> doDraw(surfaceResource,
                                              wlBufferResource));
    }

    private void doDraw(@Nonnull final WlSurfaceResource surfaceResource,
                        @Nonnull final WlBufferResource wlBufferResource) {
        final ShmBuffer buffer = ShmBuffer.get(wlBufferResource);
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer resource is not an ShmBuffer.");
        }

        final WlSurface implementation = (WlSurface) surfaceResource.getImplementation();
        final Surface   surface        = implementation.getSurface();
        final Point     position       = surface.getPosition();
        final float[] vertices = {
                position.getX(), position.getY(), 0f, 0f,
                position.getX() + buffer.getWidth(), position.getY(), 1f, 0f,
                position.getX() + buffer.getWidth(), position.getY() + buffer.getHeight(), 1f, 1f,
                position.getX(), position.getY() + buffer.getHeight(), 0f, 1f
        };

        buffer.beginAccess();
        querySurfaceData(surfaceResource,
                         buffer).makeActive(this.libgles2,
                                            buffer);
        buffer.endAccess();
        surface.firePaintCallbacks((int) NANOSECONDS.toMillis(System.nanoTime()));

        final int shaderProgram = queryShaderProgram(queryBufferFormat(buffer));
        configureShaders(shaderProgram,
                         this.projection,
                         vertices);
        this.libgles2.glUseProgram(shaderProgram);
        this.libgles2.glDrawElements(GL_TRIANGLES,
                                     6,
                                     GL_UNSIGNED_INT,
                                     null);
    }

    private Gles2SurfaceData querySurfaceData(final WlSurfaceResource surfaceResource,
                                              final ShmBuffer buffer) {
        Gles2SurfaceData surfaceData = this.cachedSurfaceData.get(surfaceResource);
        if (surfaceData == null) {
            surfaceData = Gles2SurfaceData.create(this.libgles2);
            surfaceData.init(this.libgles2,
                             buffer);
            this.cachedSurfaceData.put(surfaceResource,
                                       surfaceData);
        }
        else {
            final int surfaceDataWidth = surfaceData.getWidth();
            final int surfaceDataHeight = surfaceData.getHeight();
            final int bufferWidth = buffer.getWidth();
            final int bufferHeight = buffer.getHeight();
            if (surfaceDataWidth != bufferWidth || surfaceDataHeight != bufferHeight) {
                surfaceData.destroy(this.libgles2);
                surfaceData = Gles2SurfaceData.create(this.libgles2);
                surfaceData.init(this.libgles2,
                                 buffer);
                this.cachedSurfaceData.put(surfaceResource,
                                           surfaceData);
            }
        }
        return surfaceData;
    }

    private Gles2BufferFormat queryBufferFormat(final ShmBuffer buffer) {
        final Gles2BufferFormat format;
        final int               bufferFormat = buffer.getFormat();
        if (bufferFormat == WlShmFormat.ARGB8888.getValue()) {
            format = Gles2BufferFormat.SHM_ARGB8888;
        }
        else if (bufferFormat == WlShmFormat.XRGB8888.getValue()) {
            format = Gles2BufferFormat.SHM_XRGB8888;
        }
        else {
            throw new UnsupportedOperationException("Format " + buffer.getFormat() + " not supported.");
        }
        return format;
    }

    private int queryShaderProgram(final Gles2BufferFormat bufferFormat) {
        Integer shaderProgram = this.shaderPrograms.get(bufferFormat);
        if (shaderProgram == null) {
            shaderProgram = createShaderProgram(bufferFormat);
            this.shaderPrograms.put(bufferFormat,
                                    shaderProgram);
        }
        return shaderProgram;
    }

    private int createShaderProgram(final Gles2BufferFormat bufferFormat) {
        final int vertexShader = this.libgles2.glCreateShader(GL_VERTEX_SHADER);
        compileShader(vertexShader,
                      SURFACE_V);

        final int fragmentShader;
        if (bufferFormat == Gles2BufferFormat.SHM_ARGB8888) {
            fragmentShader = this.libgles2.glCreateShader(GL_FRAGMENT_SHADER);
            compileShader(fragmentShader,
                          SURFACE_ARGB8888_F);
        }
        else if (bufferFormat == Gles2BufferFormat.SHM_XRGB8888) {
            fragmentShader = this.libgles2.glCreateShader(GL_FRAGMENT_SHADER);
            compileShader(fragmentShader,
                          SURFACE_XRGB8888_F);
        }
        else {
            throw new UnsupportedOperationException("Buffer format " + bufferFormat + " is not supported");
        }

        final int shaderProgram = this.libgles2.glCreateProgram();
        this.libgles2.glAttachShader(shaderProgram,
                                     vertexShader);
        this.libgles2.glAttachShader(shaderProgram,
                                     fragmentShader);
        this.libgles2.glLinkProgram(shaderProgram);
        return shaderProgram;
    }

    private void compileShader(final int shaderHandle,
                               final String shaderSource) {
        final String[] lines   = new String[]{shaderSource};
        final int[]    lengths = new int[]{lines[0].length()};
        this.libgles2.glShaderSource(shaderHandle,
                                     lines.length,
                                     lines,
                                     lengths,
                                     0);
        this.libgles2.glCompileShader(shaderHandle);

        final Memory vstatus = new Memory(Integer.BYTES);
        this.libgles2.glGetShaderiv(shaderHandle,
                                    GL_COMPILE_STATUS,
                                    vstatus);
        if (vstatus.getInt(0) != Libgles2.GL_TRUE) {
            //failure!
            //get log length
            final int[] logLength = new int[1];
            this.libgles2.glGetShaderiv(shaderHandle,
                                        Libgles2.GL_INFO_LOG_LENGTH,
                                        logLength,
                                        0);
            //get log
            if (logLength[0] == 0) {
                logLength[0] = 1024;
            }
            final byte[] log = new byte[logLength[0]];
            this.libgles2.glGetShaderInfoLog(shaderHandle,
                                             logLength[0],
                                             null,
                                             0,
                                             log,
                                             0);
            System.err.println("Error compiling the vertex shader: " + new String(log));
            System.exit(1);
        }
    }

    private void configureShaders(final Integer program,
                                  final Mat4 projection,
                                  final float[] vertices) {
        final int uniTrans = this.libgles2.glGetUniformLocation(program,
                                                                "mu_projection");
        this.libgles2.glUniformMatrix4fv(uniTrans,
                                         1,
                                         false,
                                         projection.toBuffer());

        this.libgles2.glBufferData(GL_ARRAY_BUFFER,
                                   vertices.length * 4,
                                   Buffers.newDirectFloatBuffer(vertices),
                                   GL_DYNAMIC_DRAW);

        final int posAttrib = this.libgles2.glGetAttribLocation(program,
                                                                "va_position");
        final int texAttrib = this.libgles2.glGetAttribLocation(program,
                                                                "va_texcoord");
        this.libgles2.glEnableVertexAttribArray(posAttrib);
        this.libgles2.glVertexAttribPointer(posAttrib,
                                            2,
                                            GL_FLOAT,
                                            false,
                                            4 * 4,
                                            0);
        this.libgles2.glEnableVertexAttribArray(texAttrib);
        this.libgles2.glVertexAttribPointer(texAttrib,
                                            2,
                                            GL_FLOAT,
                                            false,
                                            4 * 4,
                                            2 * 4);
    }

    @Nonnull
    @Override
    public Future<?> end(@Nonnull final WlOutput wlOutput) {
        return this.renderThread.submit(() -> doEnd(wlOutput));
    }

    private void doEnd(@Nonnull final WlOutput wlOutput) {
        final HasEglOutput hasEglOutput = (HasEglOutput) wlOutput.getOutput()
                                                                 .getImplementation();
        hasEglOutput.getEglOutput()
                    .end();
    }
}
