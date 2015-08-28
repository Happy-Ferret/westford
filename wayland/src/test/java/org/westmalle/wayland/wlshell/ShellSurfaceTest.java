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
package org.westmalle.wayland.wlshell;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;
import org.freedesktop.wayland.server.Display;
import org.freedesktop.wayland.server.EventLoop;
import org.freedesktop.wayland.server.EventSource;
import org.freedesktop.wayland.server.WlKeyboardResource;
import org.freedesktop.wayland.server.WlPointerResource;
import org.freedesktop.wayland.server.WlShellSurfaceResource;
import org.freedesktop.wayland.server.WlSurfaceResource;
import org.freedesktop.wayland.shared.WlShellSurfaceResize;
import org.freedesktop.wayland.shared.WlShellSurfaceTransient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.westmalle.wayland.core.Compositor;
import org.westmalle.wayland.core.KeyboardDevice;
import org.westmalle.wayland.core.Point;
import org.westmalle.wayland.core.PointerDevice;
import org.westmalle.wayland.core.PointerGrabMotion;
import org.westmalle.wayland.core.Rectangle;
import org.westmalle.wayland.core.Surface;
import org.westmalle.wayland.core.calc.Mat4;
import org.westmalle.wayland.core.calc.Vec4;
import org.westmalle.wayland.core.events.KeyboardFocusGained;
import org.westmalle.wayland.core.events.Motion;
import org.westmalle.wayland.protocol.WlCompositor;
import org.westmalle.wayland.protocol.WlKeyboard;
import org.westmalle.wayland.protocol.WlPointer;
import org.westmalle.wayland.protocol.WlSurface;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShellSurfaceTest {

    @Mock
    private EventSource  eventSource;
    @Mock
    private EventLoop    eventLoop;
    @Mock
    private Display      display;
    @Mock
    private WlCompositor wlCompositor;

    @Before
    public void setUp() {
        when(this.display.getEventLoop()).thenReturn(this.eventLoop);
        when(this.eventLoop.addTimer(any())).thenReturn(this.eventSource);
    }

    @Test
    public void testMove() throws Exception {
        //given
        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPosition = Point.create(100,
                                                   100);
        when(pointerDevice.getPosition()).thenReturn(pointerPosition);

        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        final Surface           surface           = mock(Surface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        when(wlSurface.getSurface()).thenReturn(surface);
        final Point surfacePosition = Point.create(75,
                                                   75);
        when(surface.global(eq(Point.create(0,
                                            0)))).thenReturn(surfacePosition);

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);
        //when
        shellSurface.move(wlSurfaceResource,
                          wlPointerResource,
                          serial);
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(98765,
                                               Point.create(110,
                                                            110)));
        //then
        verify(surface).setPosition(Point.create(85,
                                                 85));
    }

    @Test
    public void testResizeRight() throws Exception {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);

        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPositionStart = mock(Point.class);
        when(pointerDevice.getPosition()).thenReturn(pointerPositionStart);
        final Point pointerPositionMotion = Point.create(1200,
                                                         900);
        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        when(surface.local(pointerPositionStart)).thenReturn(Point.create(80,
                                                                          80));
        final Mat4 inverseTransform = mock(Mat4.class);
        when(surface.getInverseTransform()).thenReturn(inverseTransform);
        when(inverseTransform.multiply(pointerPositionMotion.toVec4())).thenReturn(Vec4.create(180,
                                                                                               180,
                                                                                               0,
                                                                                               1));

        when(surface.getSize()).thenReturn(Rectangle.create(0,
                                                            0,
                                                            100,
                                                            100));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);

        //when
        shellSurface.resize(wlShellSurfaceResource,
                            wlSurfaceResource,
                            wlPointerResource,
                            serial,
                            WlShellSurfaceResize.RIGHT.getValue());
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionArgumentCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionArgumentCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionArgumentCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(456767,
                                               pointerPositionMotion));
        //then
        verify(wlShellSurfaceResource).configure(WlShellSurfaceResize.RIGHT.getValue(),
                                                 200,
                                                 100);
    }

    @Test
    public void testResizeBottomRight() throws Exception {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);

        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPositionStart = mock(Point.class);
        when(pointerDevice.getPosition()).thenReturn(pointerPositionStart);
        final Point pointerPositionMotion = Point.create(1200,
                                                         900);
        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        when(surface.local(pointerPositionStart)).thenReturn(Point.create(80,
                                                                          80));
        final Mat4 inverseTransform = mock(Mat4.class);
        when(surface.getInverseTransform()).thenReturn(inverseTransform);
        when(inverseTransform.multiply(pointerPositionMotion.toVec4())).thenReturn(Vec4.create(180,
                                                                                               180,
                                                                                               0,
                                                                                               1));

        when(surface.getSize()).thenReturn(Rectangle.create(0,
                                                            0,
                                                            100,
                                                            100));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);

        //when
        shellSurface.resize(wlShellSurfaceResource,
                            wlSurfaceResource,
                            wlPointerResource,
                            serial,
                            WlShellSurfaceResize.BOTTOM_RIGHT.getValue());
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionArgumentCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionArgumentCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionArgumentCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(456767,
                                               pointerPositionMotion));
        //then
        verify(wlShellSurfaceResource).configure(WlShellSurfaceResize.BOTTOM_RIGHT.getValue(),
                                                 200,
                                                 200);
    }

    @Test
    public void testResizeTop() throws Exception {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);

        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPositionStart = mock(Point.class);
        when(pointerDevice.getPosition()).thenReturn(pointerPositionStart);
        final Point pointerPositionMotion = Point.create(1200,
                                                         900);
        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        when(surface.local(pointerPositionStart)).thenReturn(Point.create(80,
                                                                          20));
        final Mat4 inverseTransform = mock(Mat4.class);
        when(surface.getInverseTransform()).thenReturn(inverseTransform);
        when(inverseTransform.multiply(pointerPositionMotion.toVec4())).thenReturn(Vec4.create(180,
                                                                                               -80,
                                                                                               0,
                                                                                               1));

        when(surface.getSize()).thenReturn(Rectangle.create(0,
                                                            0,
                                                            100,
                                                            100));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);

        //when
        shellSurface.resize(wlShellSurfaceResource,
                            wlSurfaceResource,
                            wlPointerResource,
                            serial,
                            WlShellSurfaceResize.TOP.getValue());
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionArgumentCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionArgumentCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionArgumentCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(456767,
                                               pointerPositionMotion));
        //then
        verify(wlShellSurfaceResource).configure(WlShellSurfaceResize.TOP.getValue(),
                                                 100,
                                                 200);
    }

    @Test
    public void testResizeTopRight() throws Exception {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);

        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPositionStart = mock(Point.class);
        when(pointerDevice.getPosition()).thenReturn(pointerPositionStart);
        final Point pointerPositionMotion = Point.create(1200,
                                                         900);
        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        when(surface.local(pointerPositionStart)).thenReturn(Point.create(80,
                                                                          20));
        final Mat4 inverseTransform = mock(Mat4.class);
        when(surface.getInverseTransform()).thenReturn(inverseTransform);
        when(inverseTransform.multiply(pointerPositionMotion.toVec4())).thenReturn(Vec4.create(180,
                                                                                               -80,
                                                                                               0,
                                                                                               1));

        when(surface.getSize()).thenReturn(Rectangle.create(0,
                                                            0,
                                                            100,
                                                            100));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);

        //when
        shellSurface.resize(wlShellSurfaceResource,
                            wlSurfaceResource,
                            wlPointerResource,
                            serial,
                            WlShellSurfaceResize.TOP_RIGHT.getValue());
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionArgumentCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionArgumentCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionArgumentCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(456767,
                                               pointerPositionMotion));
        //then
        verify(wlShellSurfaceResource).configure(WlShellSurfaceResize.TOP_RIGHT.getValue(),
                                                 200,
                                                 200);
    }

    @Test
    public void testResizeLeft() throws Exception {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);

        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPositionStart = mock(Point.class);
        when(pointerDevice.getPosition()).thenReturn(pointerPositionStart);
        final Point pointerPositionMotion = Point.create(1200,
                                                         900);
        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        when(surface.local(pointerPositionStart)).thenReturn(Point.create(20,
                                                                          20));
        final Mat4 inverseTransform = mock(Mat4.class);
        when(surface.getInverseTransform()).thenReturn(inverseTransform);
        when(inverseTransform.multiply(pointerPositionMotion.toVec4())).thenReturn(Vec4.create(-80,
                                                                                               -80,
                                                                                               0,
                                                                                               1));

        when(surface.getSize()).thenReturn(Rectangle.create(0,
                                                            0,
                                                            100,
                                                            100));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);

        //when
        shellSurface.resize(wlShellSurfaceResource,
                            wlSurfaceResource,
                            wlPointerResource,
                            serial,
                            WlShellSurfaceResize.LEFT.getValue());
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionArgumentCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionArgumentCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionArgumentCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(456767,
                                               pointerPositionMotion));
        //then
        verify(wlShellSurfaceResource).configure(WlShellSurfaceResize.LEFT.getValue(),
                                                 200,
                                                 100);
    }

    @Test
    public void testResizeTopLeft() throws Exception {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);

        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPositionStart = mock(Point.class);
        when(pointerDevice.getPosition()).thenReturn(pointerPositionStart);
        final Point pointerPositionMotion = Point.create(1200,
                                                         900);

        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        when(surface.local(pointerPositionStart)).thenReturn(Point.create(20,
                                                                          20));
        final Mat4 inverseTransform = mock(Mat4.class);
        when(surface.getInverseTransform()).thenReturn(inverseTransform);
        when(inverseTransform.multiply(pointerPositionMotion.toVec4())).thenReturn(Vec4.create(-80,
                                                                                               -80,
                                                                                               0,
                                                                                               1));
        when(surface.getSize()).thenReturn(Rectangle.create(0,
                                                            0,
                                                            100,
                                                            100));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);

        //when
        shellSurface.resize(wlShellSurfaceResource,
                            wlSurfaceResource,
                            wlPointerResource,
                            serial,
                            WlShellSurfaceResize.TOP_LEFT.getValue());
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionArgumentCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionArgumentCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionArgumentCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(456767,
                                               pointerPositionMotion));
        //then
        verify(wlShellSurfaceResource).configure(WlShellSurfaceResize.TOP_LEFT.getValue(),
                                                 200,
                                                 200);
    }

    @Test
    public void testResizeBottom() throws Exception {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);

        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPositionStart = mock(Point.class);
        when(pointerDevice.getPosition()).thenReturn(pointerPositionStart);
        final Point pointerPositionMotion = Point.create(1200,
                                                         900);
        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        when(surface.local(pointerPositionStart)).thenReturn(Point.create(20,
                                                                          80));
        final Mat4 inverseTransform = mock(Mat4.class);
        when(surface.getInverseTransform()).thenReturn(inverseTransform);
        when(inverseTransform.multiply(pointerPositionMotion.toVec4())).thenReturn(Vec4.create(-80,
                                                                                               180,
                                                                                               0,
                                                                                               1));

        when(surface.getSize()).thenReturn(Rectangle.create(0,
                                                            0,
                                                            100,
                                                            100));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);

        //when
        shellSurface.resize(wlShellSurfaceResource,
                            wlSurfaceResource,
                            wlPointerResource,
                            serial,
                            WlShellSurfaceResize.BOTTOM.getValue());
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionArgumentCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionArgumentCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionArgumentCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(456767,
                                               pointerPositionMotion));
        //then
        verify(wlShellSurfaceResource).configure(WlShellSurfaceResize.BOTTOM.getValue(),
                                                 100,
                                                 200);
    }

    @Test
    public void testResizeBottomLeft() throws Exception {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);

        final WlPointer         wlPointer         = mock(WlPointer.class);
        final WlPointerResource wlPointerResource = mock(WlPointerResource.class);
        when(wlPointerResource.getImplementation()).thenReturn(wlPointer);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final Point pointerPositionStart = mock(Point.class);
        when(pointerDevice.getPosition()).thenReturn(pointerPositionStart);
        final Point pointerPositionMotion = Point.create(1200,
                                                         900);
        final int serial = 12345;

        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        when(surface.local(pointerPositionStart)).thenReturn(Point.create(20,
                                                                          80));
        final Mat4 inverseTransform = mock(Mat4.class);
        when(surface.getInverseTransform()).thenReturn(inverseTransform);
        when(inverseTransform.multiply(pointerPositionMotion.toVec4())).thenReturn(Vec4.create(-80,
                                                                                               180,
                                                                                               0,
                                                                                               1));

        when(surface.getSize()).thenReturn(Rectangle.create(0,
                                                            0,
                                                            100,
                                                            100));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           0);

        //when
        shellSurface.resize(wlShellSurfaceResource,
                            wlSurfaceResource,
                            wlPointerResource,
                            serial,
                            WlShellSurfaceResize.BOTTOM_LEFT.getValue());
        //then
        final ArgumentCaptor<PointerGrabMotion> pointerGrabMotionArgumentCaptor = ArgumentCaptor.forClass(PointerGrabMotion.class);
        verify(pointerDevice).grabMotion(eq(wlSurfaceResource),
                                         eq(serial),
                                         pointerGrabMotionArgumentCaptor.capture());
        //and when
        final PointerGrabMotion pointerGrabMotion = pointerGrabMotionArgumentCaptor.getValue();
        pointerGrabMotion.motion(Motion.create(456767,
                                               pointerPositionMotion));
        //then
        verify(wlShellSurfaceResource).configure(WlShellSurfaceResize.BOTTOM_LEFT.getValue(),
                                                 200,
                                                 200);
    }

    @Test
    public void testPong() {
        //given
        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);
        final int                    pingSerial             = 12345;

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           12345);

        //when
        shellSurface.pong(wlShellSurfaceResource,
                          pingSerial);

        //then
        verify(wlShellSurfaceResource).ping(pingSerial);
        verify(this.eventSource).updateTimer(anyInt());
    }

    @Test
    public void testPongTimeout() {
        //given
        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           12345);
        final ArgumentCaptor<EventLoop.TimerEventHandler> timerEventHandlerArgumentCaptor = ArgumentCaptor.forClass(EventLoop.TimerEventHandler.class);
        verify(this.eventLoop).addTimer(timerEventHandlerArgumentCaptor.capture());
        final EventLoop.TimerEventHandler timerEventHandler = timerEventHandlerArgumentCaptor.getValue();

        final WlShellSurfaceResource wlShellSurfaceResource = mock(WlShellSurfaceResource.class);
        final int                    pingSerial             = 12345;
        shellSurface.pong(wlShellSurfaceResource,
                          pingSerial);

        //when
        timerEventHandler.handle();

        //then
        assertThat(shellSurface.isActive()).isFalse();
    }

    @Test
    public void testToFront() throws Exception {
        //given
        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           12345);

        final Compositor compositor = mock(Compositor.class);
        when(this.wlCompositor.getCompositor()).thenReturn(compositor);
        final LinkedList<WlSurfaceResource> surfacesStack = new LinkedList<>();
        when(compositor.getSurfacesStack()).thenReturn(surfacesStack);

        final WlSurfaceResource wlSurfaceResource0 = mock(WlSurfaceResource.class);
        final WlSurfaceResource wlSurfaceResource1 = mock(WlSurfaceResource.class);
        final WlSurfaceResource wlSurfaceResource2 = mock(WlSurfaceResource.class);

        surfacesStack.add(wlSurfaceResource1);
        surfacesStack.add(wlSurfaceResource0);
        surfacesStack.add(wlSurfaceResource2);

        //when
        shellSurface.toFront(wlSurfaceResource0);

        //then
        assertThat(surfacesStack.getLast()).isSameAs(wlSurfaceResource0);
    }

    @Test
    public void testSetTransient() throws Exception {
        //given
        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        final WlSurfaceResource parentWlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         parentWlSurface         = mock(WlSurface.class);
        when(parentWlSurfaceResource.getImplementation()).thenReturn(parentWlSurface);
        final Surface parentSurface = mock(Surface.class);
        when(parentWlSurface.getSurface()).thenReturn(parentSurface);

        final int localX = 75;
        final int localY = 120;

        final int globalX = 100;
        final int globalY = 150;

        when(parentSurface.global(Point.create(localX,
                                               localY))).thenReturn(Point.create(globalX,
                                                                                 globalY));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           12345);

        //when
        shellSurface.setTransient(wlSurfaceResource,
                                  parentWlSurfaceResource,
                                  localX,
                                  localY,
                                  EnumSet.noneOf(WlShellSurfaceTransient.class));

        //then
        verify(surface).setPosition(Point.create(globalX,
                                                 globalY));
    }

    @Test
    public void testSetTransientInactive() throws Exception {
        //given
        final WlSurfaceResource wlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         wlSurface         = mock(WlSurface.class);
        when(wlSurfaceResource.getImplementation()).thenReturn(wlSurface);
        final Surface surface = mock(Surface.class);
        when(wlSurface.getSurface()).thenReturn(surface);

        final WlKeyboardResource      wlKeyboardResource0 = mock(WlKeyboardResource.class);
        final WlKeyboardResource      wlKeyboardResource1 = mock(WlKeyboardResource.class);
        final Set<WlKeyboardResource> keyboardFocuses     = new HashSet<>();
        keyboardFocuses.add(wlKeyboardResource0);
        keyboardFocuses.add(wlKeyboardResource1);
        when(surface.getKeyboardFocuses()).thenReturn(keyboardFocuses);

        final WlKeyboard wlKeyboard = mock(WlKeyboard.class);
        when(wlKeyboardResource0.getImplementation()).thenReturn(wlKeyboard);
        when(wlKeyboardResource1.getImplementation()).thenReturn(wlKeyboard);

        final KeyboardDevice keyboardDevice = mock(KeyboardDevice.class);
        when(wlKeyboard.getKeyboardDevice()).thenReturn(keyboardDevice);

        final WlSurfaceResource parentWlSurfaceResource = mock(WlSurfaceResource.class);
        final WlSurface         parentWlSurface         = mock(WlSurface.class);
        when(parentWlSurfaceResource.getImplementation()).thenReturn(parentWlSurface);
        final Surface parentSurface = mock(Surface.class);
        when(parentWlSurface.getSurface()).thenReturn(parentSurface);

        final int localX = 75;
        final int localY = 120;

        final int globalX = 100;
        final int globalY = 150;

        when(parentSurface.global(Point.create(localX,
                                               localY))).thenReturn(Point.create(globalX,
                                                                                 globalY));

        final ShellSurface shellSurface = new ShellSurface(this.display,
                                                           this.wlCompositor,
                                                           12345);

        //when
        shellSurface.setTransient(wlSurfaceResource,
                                  parentWlSurfaceResource,
                                  localX,
                                  localY,
                                  EnumSet.of(WlShellSurfaceTransient.INACTIVE));

        //then
        final ArgumentCaptor<Object> listenerArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(surface).register(listenerArgumentCaptor.capture());
        verify(surface).setPosition(Point.create(globalX,
                                                 globalY));
        verify(wlKeyboardResource0).leave(anyInt(),
                                          eq(wlSurfaceResource));
        verify(wlKeyboardResource1).leave(anyInt(),
                                          eq(wlSurfaceResource));
        assertThat(keyboardFocuses).isEmpty();

        //and when
        final WlKeyboardResource wlKeyboardResource2 = mock(WlKeyboardResource.class);
        when(wlKeyboardResource2.getImplementation()).thenReturn(wlKeyboard);

        keyboardFocuses.add(wlKeyboardResource2);

        final Object listener = listenerArgumentCaptor.getValue();
        final Bus    bus      = new Bus(ThreadEnforcer.ANY);
        bus.register(listener);
        bus.post(KeyboardFocusGained.create(Collections.singleton(wlKeyboardResource2)));

        //then
        assertThat(keyboardFocuses).isEmpty();
    }
}