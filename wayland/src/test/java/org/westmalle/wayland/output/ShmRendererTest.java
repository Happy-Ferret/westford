package org.westmalle.wayland.output;

import com.google.common.util.concurrent.ListenableFuture;
import org.freedesktop.wayland.server.ShmBuffer;
import org.freedesktop.wayland.server.WlBufferResource;
import org.freedesktop.wayland.server.WlSurfaceResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.westmalle.wayland.protocol.WlSurface;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
                        //following classes have static methods, so we have to powermock them:
                        ShmBuffer.class
                })
public class ShmRendererTest {

    @Mock
    private ShmRenderEngine shmRenderEngine;
    @InjectMocks
    private ShmRenderer     shmRenderer;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(ShmBuffer.class);
    }

    @Test
    public void testRender() throws Exception {
        //given
        final WlSurfaceResource surfaceResource = mock(WlSurfaceResource.class);

        final WlSurface wlSurface = mock(WlSurface.class);
        when(surfaceResource.getImplementation()).thenReturn(wlSurface);

        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        final SurfaceState surfaceState = mock(SurfaceState.class);
        when(surface.getState()).thenReturn(surfaceState);

        final WlBufferResource wlBufferResource = mock(WlBufferResource.class);
        when(surfaceState.getBuffer()).thenReturn(Optional.of(wlBufferResource));

        final ShmBuffer shmBuffer = mock(ShmBuffer.class);
        when(ShmBuffer.get(wlBufferResource)).thenReturn(shmBuffer);

        ListenableFuture listenableFuture = mock(ListenableFuture.class);
        when(this.shmRenderEngine.draw(any(),
                                       any())).thenReturn(listenableFuture);
        //when
        this.shmRenderer.render(surfaceResource);
        //then
        verify(this.shmRenderEngine,
               times(1)).draw(surfaceResource,
                              shmBuffer);
    }

    @Test
    public void testBeginRender() throws Exception {
        //given
        ListenableFuture listenableFuture = mock(ListenableFuture.class);
        when(this.shmRenderEngine.begin()).thenReturn(listenableFuture);
        //when
        this.shmRenderer.beginRender();
        //then
        verify(this.shmRenderEngine,
               times(1)).begin();
    }

    @Test
    public void testEndRender() throws Exception {
        //given
        ListenableFuture listenableFuture = mock(ListenableFuture.class);
        when(this.shmRenderEngine.end()).thenReturn(listenableFuture);
        //when
        this.shmRenderer.endRender();
        //then
        verify(this.shmRenderEngine,
               times(1)).end();
    }
}