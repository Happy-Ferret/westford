//Copyright 2015 Erik De Rijcke
//
//Licensed under the Apache License,Version2.0(the"License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,software
//distributed under the License is distributed on an"AS IS"BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package org.westmalle.wayland.core;

import org.freedesktop.wayland.server.DestroyListener;
import org.freedesktop.wayland.server.ShmBuffer;
import org.freedesktop.wayland.server.WlBufferResource;
import org.freedesktop.wayland.server.WlCallbackResource;
import org.freedesktop.wayland.server.WlCompositorResource;
import org.freedesktop.wayland.server.WlRegionResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.westmalle.wayland.protocol.WlCompositor;
import org.westmalle.wayland.protocol.WlRegion;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FiniteRegionFactory.class,
                 ShmBuffer.class})
public class SurfaceTest {

    @Mock
    private FiniteRegionFactory finiteRegionFactory;
    @Mock
    private Compositor          compositor;
    @InjectMocks
    private Surface             surface;

    @Before
    public void setUp() {
        mockStatic(ShmBuffer.class);
    }

    @Test
    public void testMarkDestroyed() throws Exception {
        //given
        //when
        this.surface.markDestroyed();
        //then
        assertThat(this.surface.isDestroyed()).isTrue();
    }

    @Test
    public void testMarkDamaged() throws Exception {
        //given
        final FiniteRegion region = mock(FiniteRegion.class);
        when(region.add(any())).thenReturn(region);
        when(this.finiteRegionFactory.create()).thenReturn(region);
        final Rectangle damage = Rectangle.create(100,
                                                  100,
                                                  20,
                                                  50);
        //when
        this.surface.markDamaged(damage);
        //then
        verify(region).add(damage);
    }

    @Test
    public void testAttachCommit() throws Exception {
        //given
        final WlBufferResource buffer = mock(WlBufferResource.class);
        final Integer          relX   = -10;
        final Integer          relY   = 200;

        final ShmBuffer shmBuffer = mock(ShmBuffer.class);
        when(ShmBuffer.get(buffer)).thenReturn(shmBuffer);

        final int bufferWidth = 200;
        when(shmBuffer.getWidth()).thenReturn(bufferWidth);
        final int bufferHeight = 300;
        when(shmBuffer.getHeight()).thenReturn(bufferHeight);

        //when
        this.surface.attachBuffer(buffer,
                                  relX,
                                  relY);
        this.surface.commit();
        //then
        assertThat(this.surface.getState()
                               .getBuffer()
                               .isPresent()).isTrue();
        assertThat(this.surface.getSize()).isEqualTo(Rectangle.builder()
                                                              .width(200)
                                                              .height(300)
                                                              .build());
        verify(compositor).requestRender();
    }

    @Test
    public void testAttachCommitAttachCommit() throws Exception {
        //given
        final WlBufferResource buffer = mock(WlBufferResource.class);
        final Integer          relX   = -10;
        final Integer          relY   = 200;

        final ShmBuffer shmBuffer = mock(ShmBuffer.class);
        when(ShmBuffer.get(buffer)).thenReturn(shmBuffer);

        final int bufferWidth = 200;
        when(shmBuffer.getWidth()).thenReturn(bufferWidth);
        final int bufferHeight = 300;
        when(shmBuffer.getHeight()).thenReturn(bufferHeight);

        //when
        this.surface.attachBuffer(buffer,
                                  relX,
                                  relY);
        this.surface.commit();
        this.surface.attachBuffer(buffer,
                                  relX,
                                  relY);
        this.surface.commit();
        //then
        assertThat(this.surface.getSize()).isEqualTo(Rectangle.builder()
                                                              .width(200)
                                                              .height(300)
                                                              .build());
        verify(buffer).release();
    }

    @Test
    public void testAttachAttachCommit() throws Exception {
        //given
        final WlBufferResource buffer0 = mock(WlBufferResource.class);
        final Integer          relX0   = -10;
        final Integer          relY0   = 200;

        final WlBufferResource buffer1 = mock(WlBufferResource.class);
        final Integer          relX1   = -10;
        final Integer          relY1   = 200;

        final ShmBuffer shmBuffer0 = mock(ShmBuffer.class);
        when(ShmBuffer.get(buffer0)).thenReturn(shmBuffer0);

        final int bufferWidth0 = 200;
        when(shmBuffer0.getWidth()).thenReturn(bufferWidth0);
        final int bufferHeight0 = 300;
        when(shmBuffer0.getHeight()).thenReturn(bufferHeight0);

        final ShmBuffer shmBuffer1 = mock(ShmBuffer.class);
        when(ShmBuffer.get(buffer1)).thenReturn(shmBuffer1);

        final int bufferWidth1 = 123;
        when(shmBuffer1.getWidth()).thenReturn(bufferWidth1);
        final int bufferHeight1 = 456;
        when(shmBuffer1.getHeight()).thenReturn(bufferHeight1);

        //when
        this.surface.attachBuffer(buffer0,
                                  relX0,
                                  relY0);
        this.surface.attachBuffer(buffer1,
                                  relX1,
                                  relY1);
        this.surface.commit();
        //then
        assertThat(this.surface.getState()
                               .getBuffer()
                               .get()).isSameAs(buffer1);
        assertThat(this.surface.getSize()).isEqualTo(Rectangle.builder()
                                                              .width(123)
                                                              .height(456)
                                                              .build());
    }

    @Test
    public void testAttachDestroyBuffer() {
        //given
        final WlBufferResource wlBufferResource = mock(WlBufferResource.class);
        final Integer          relX0            = -10;
        final Integer          relY0            = 200;

        //when
        this.surface.attachBuffer(wlBufferResource,
                                  relX0,
                                  relY0);
        //then
        final ArgumentCaptor<DestroyListener> listenerArgumentCaptor = ArgumentCaptor.forClass(DestroyListener.class);
        verify(wlBufferResource,
               times(1)).register(listenerArgumentCaptor.capture());
        final DestroyListener destroyListener = listenerArgumentCaptor.getValue();
        //and when
        destroyListener.handle();
        //then
        this.surface.detachBuffer();
    }

    @Test
    public void testAttachDetachCommit() throws Exception {
        //given
        final WlBufferResource buffer = mock(WlBufferResource.class);
        final Integer          relX   = -10;
        final Integer          relY   = 200;

        final ShmBuffer shmBuffer = mock(ShmBuffer.class);
        when(ShmBuffer.get(buffer)).thenReturn(shmBuffer);

        //when
        this.surface.attachBuffer(buffer,
                                  relX,
                                  relY);
        this.surface.detachBuffer();
        this.surface.commit();
        //then
        assertThat(this.surface.getState()
                               .getBuffer()
                               .isPresent()).isFalse();
        assertThat(this.surface.getSize()).isEqualTo(Rectangle.ZERO);
        verify(compositor).requestRender();
    }

    @Test
    public void testRemoveOpaqueRegion() throws Exception {
        //given
        final WlRegionResource wlRegionResource = mock(WlRegionResource.class);
        final WlRegion         wlRegion         = mock(WlRegion.class);
        when(wlRegionResource.getImplementation()).thenReturn(wlRegion);
        final Region region = mock(Region.class);
        when(wlRegion.getRegion()).thenReturn(region);
        this.surface.setOpaqueRegion(wlRegionResource);
        //when
        this.surface.removeOpaqueRegion();
        //then
        assertThat(this.surface.getState()
                               .getOpaqueRegion()
                               .isPresent()).isFalse();
    }

    @Test
    public void testRemoveInputRegion() throws Exception {
        //given
        final WlRegionResource wlRegionResource = mock(WlRegionResource.class);
        final WlRegion         wlRegion         = mock(WlRegion.class);
        when(wlRegionResource.getImplementation()).thenReturn(wlRegion);
        final Region region = mock(Region.class);
        when(wlRegion.getRegion()).thenReturn(region);

        this.surface.setInputRegion(wlRegionResource);
        //when
        this.surface.removeInputRegion();
        //then
        assertThat(this.surface.getState()
                               .getInputRegion()
                               .isPresent()).isFalse();
    }

    @Test
    public void testFirePaintCallbacks() throws Exception {
        //given
        final int serial = 548674;

        final WlCallbackResource wlCallbackResource0 = mock(WlCallbackResource.class);
        final WlCallbackResource wlCallbackResource1 = mock(WlCallbackResource.class);
        final WlCallbackResource wlCallbackResource2 = mock(WlCallbackResource.class);

        this.surface.addCallback(wlCallbackResource0);
        this.surface.addCallback(wlCallbackResource1);
        this.surface.addCallback(wlCallbackResource2);
        //when
        this.surface.firePaintCallbacks(serial);
        //then
        verify(wlCallbackResource0).done(serial);
        verify(wlCallbackResource1).done(serial);
        verify(wlCallbackResource2).done(serial);
        assertThat(this.surface.getFrameCallbacks()).isEmpty();
    }

    @Test
    public void testLocal() throws Exception {
        //given
        final Point absoluteCoordinate = Point.create(150,
                                                      150);
        final Point surfaceCoordinate = Point.create(100,
                                                     100);
        this.surface.setPosition(surfaceCoordinate);
        //when
        final Point relativeCoordinate = this.surface.local(absoluteCoordinate);
        //then
        assertThat(relativeCoordinate).isEqualTo(Point.create(50,
                                                              50));
    }

    @Test
    public void testUpdateSizeNoScaling() throws Exception {
        //given
        final ShmBuffer shmBuffer = mock(ShmBuffer.class);
        when(shmBuffer.getWidth()).thenReturn(100);
        when(shmBuffer.getHeight()).thenReturn(100);
        final WlBufferResource buffer = mock(WlBufferResource.class);
        when(ShmBuffer.get(buffer)).thenReturn(shmBuffer);

        this.surface.attachBuffer(buffer,
                                  0,
                                  0);
        //when
        this.surface.commit();

        //then
        assertThat(this.surface.getSize()).isEqualTo(Rectangle.create(0,
                                                                      0,
                                                                      100,
                                                                      100));
    }

    @Test
    public void testUpdateSizeBufferAbsent() throws Exception {
        //given
        //when
        this.surface.commit();

        //then
        assertThat(this.surface.getSize()).isEqualTo(Rectangle.ZERO);
    }

    @Test
    public void testUpdateSizeScaling() throws Exception {
        //given
        final ShmBuffer shmBuffer = mock(ShmBuffer.class);
        when(shmBuffer.getWidth()).thenReturn(100);
        when(shmBuffer.getHeight()).thenReturn(100);
        final WlBufferResource buffer = mock(WlBufferResource.class);
        when(ShmBuffer.get(buffer)).thenReturn(shmBuffer);

        this.surface.setScale(5);

        this.surface.attachBuffer(buffer,
                                  0,
                                  0);
        //when
        this.surface.commit();

        //then
        assertThat(this.surface.getSize()).isEqualTo(Rectangle.create(0,
                                                                      0,
                                                                      20,
                                                                      20));
    }
}