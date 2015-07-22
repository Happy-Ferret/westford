package org.westmalle.wayland.x11;

import org.freedesktop.wayland.server.WlPointerResource;
import org.freedesktop.wayland.shared.WlPointerButtonState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.westmalle.wayland.core.Compositor;
import org.westmalle.wayland.core.PointerDevice;
import org.westmalle.wayland.nativ.Libxcb;
import org.westmalle.wayland.nativ.xcb_button_press_event_t;
import org.westmalle.wayland.nativ.xcb_button_release_event_t;
import org.westmalle.wayland.nativ.xcb_motion_notify_event_t;
import org.westmalle.wayland.protocol.WlPointer;
import org.westmalle.wayland.protocol.WlSeat;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.westmalle.wayland.nativ.Input.BTN_LEFT;
import static org.westmalle.wayland.nativ.Input.BTN_MIDDLE;
import static org.westmalle.wayland.nativ.Input.BTN_RIGHT;

@RunWith(MockitoJUnitRunner.class)
public class X11SeatTest {

    @Mock
    private Libxcb      libxcb;
    @Mock
    private X11Output   x11Output;
    @Mock
    private Compositor  compositor;
    @Mock
    private WlSeat      wlSeat;
    @InjectMocks
    private X11Seat     x11Seat;


    @Test
    public void testHandleButtonPressLeft() throws Exception {
        testHandleButtonPress((byte) 1,
                              BTN_LEFT);
    }

    private void testHandleButtonPress(final byte xEventDetail,
                                       final int waylandEventDetail) {
        //given
        final WlPointer wlPointer = mock(WlPointer.class);
        when(this.wlSeat.getOptionalWlPointer()).thenReturn(Optional.of(wlPointer));
        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final WlPointerResource      wlPointerResource  = mock(WlPointerResource.class);
        final Set<WlPointerResource> wlPointerResources = Collections.singleton(wlPointerResource);
        when(wlPointer.getResources()).thenReturn(wlPointerResources);
        final int compositorTime = 9876;
        when(this.compositor.getTime()).thenReturn(compositorTime);
        final xcb_button_press_event_t event = mock(xcb_button_press_event_t.class);
        event.time = 123;
        event.detail = xEventDetail;
        //when
        this.x11Seat.handle(event);
        //then
        final ArgumentCaptor<Runnable> runnableArgument = ArgumentCaptor.forClass(Runnable.class);
        //and when
        runnableArgument.getValue()
                        .run();
        //then
        verify(pointerDevice).button(wlPointerResources,
                                     compositorTime,
                                     waylandEventDetail,
                                     WlPointerButtonState.PRESSED);
    }

    @Test
    public void testHandleButtonPressMiddle() throws Exception {
        testHandleButtonPress((byte) 2,
                              BTN_MIDDLE);
    }

    @Test
    public void testHandleButtonPressRight() throws Exception {
        testHandleButtonPress((byte) 3,
                              BTN_RIGHT);
    }

    @Test
    public void testHandleButtonReleaseLeft() throws Exception {
        testHandleButtonRelease((byte) 1,
                                BTN_LEFT);
    }

    private void testHandleButtonRelease(final byte xEventDetail,
                                         final int waylandEventDetail) {
        //given
        final WlPointer wlPointer = mock(WlPointer.class);
        when(this.wlSeat.getOptionalWlPointer()).thenReturn(Optional.of(wlPointer));
        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final WlPointerResource      wlPointerResource  = mock(WlPointerResource.class);
        final Set<WlPointerResource> wlPointerResources = Collections.singleton(wlPointerResource);
        when(wlPointer.getResources()).thenReturn(wlPointerResources);
        final int compositorTime = 9876;
        when(this.compositor.getTime()).thenReturn(compositorTime);
        final xcb_button_release_event_t event = mock(xcb_button_release_event_t.class);
        event.time = 123;
        event.detail = xEventDetail;
        //when
        this.x11Seat.handle(event);
        //then
        final ArgumentCaptor<Runnable> runnableArgument = ArgumentCaptor.forClass(Runnable.class);
        //and when
        runnableArgument.getValue()
                        .run();
        //then
        verify(pointerDevice).button(wlPointerResources,
                                     compositorTime,
                                     waylandEventDetail,
                                     WlPointerButtonState.RELEASED);
    }

    @Test
    public void testHandleButtonReleaseMiddle() throws Exception {
        testHandleButtonRelease((byte) 2,
                                BTN_MIDDLE);
    }

    @Test
    public void testHandleButtonReleaseRight() throws Exception {
        testHandleButtonRelease((byte) 3,
                                BTN_RIGHT);
    }

    @Test
    public void testHandleMotion() throws Exception {
        //given
        final WlPointer wlPointer = mock(WlPointer.class);
        when(this.wlSeat.getOptionalWlPointer()).thenReturn(Optional.of(wlPointer));
        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);
        final WlPointerResource      wlPointerResource  = mock(WlPointerResource.class);
        final Set<WlPointerResource> wlPointerResources = Collections.singleton(wlPointerResource);
        when(wlPointer.getResources()).thenReturn(wlPointerResources);
        final int compositorTime = 9876;
        when(this.compositor.getTime()).thenReturn(compositorTime);
        final xcb_motion_notify_event_t event = mock(xcb_motion_notify_event_t.class);
        final int                       x     = 80;
        event.event_x = x;
        final int y = -120;
        event.event_y = y;
        //when
        this.x11Seat.handle(event);
        //then
        final ArgumentCaptor<Runnable> runnableArgument = ArgumentCaptor.forClass(Runnable.class);
        //and when
        runnableArgument.getValue()
                        .run();
        //then
        verify(pointerDevice).motion(wlPointerResources,
                                     compositorTime,
                                     x,
                                     y);
    }
}