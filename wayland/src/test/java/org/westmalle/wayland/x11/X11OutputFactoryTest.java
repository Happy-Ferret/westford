package org.westmalle.wayland.x11;

import com.sun.jna.Pointer;
import org.freedesktop.wayland.server.Display;
import org.freedesktop.wayland.server.EventLoop;
import org.freedesktop.wayland.server.EventSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.westmalle.wayland.nativ.*;
import org.westmalle.wayland.output.OutputFactory;
import org.westmalle.wayland.protocol.WlOutputFactory;

import static org.mockito.Mockito.*;
import static org.westmalle.wayland.nativ.LibX11xcb.XCBOwnsEventQueue;
import static org.westmalle.wayland.nativ.Libxcb.XCB_WINDOW_CLASS_INPUT_OUTPUT;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WlOutputFactory.class,
                 OutputFactory.class,
                 X11EventBusFactory.class})
public class X11OutputFactoryTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private Display             display;
    @Mock
    private Libc                libc;
    @Mock
    private LibX11              libX11;
    @Mock
    private Libxcb              libxcb;
    @Mock
    private LibX11xcb           libX11xcb;
    @Mock
    private X11EglOutputFactory x11EglOutputFactory;
    @Mock
    private WlOutputFactory     wlOutputFactory;
    @Mock
    private OutputFactory       outputFactory;
    @Mock
    private X11EventBusFactory  x11EventBusFactory;
    @InjectMocks
    private X11OutputFactory    x11OutputFactory;

    @Test
    public void testCreateOpenDisplayFailed() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("XOpenDisplay() failed: :0");

        final String xDisplayName = ":0";
        final int    width        = 640;
        final int    height       = 480;

        when(this.libX11.XOpenDisplay(xDisplayName)).thenReturn(null);

        //when
        this.x11OutputFactory.create(xDisplayName,
                                     width,
                                     height);
        //then
        verify(this.libX11).XOpenDisplay(xDisplayName);
        //an exception is thrown
        verifyNoMoreInteractions(this.libX11);
    }

    @Test
    public void testCreateConnectionHasError() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("error occurred while connecting to X server");

        final String  xDisplayName  = ":0";
        final int     width         = 640;
        final int     height        = 480;
        final Pointer xDisplay      = mock(Pointer.class);
        final Pointer xcbConnection = mock(Pointer.class);

        when(this.libX11.XOpenDisplay(xDisplayName)).thenReturn(xDisplay);
        when(this.libX11xcb.XGetXCBConnection(xDisplay)).thenReturn(xcbConnection);
        when(this.libxcb.xcb_connection_has_error(xcbConnection)).thenReturn(1);

        //when
        this.x11OutputFactory.create(xDisplayName,
                                     width,
                                     height);
        //then
        verify(this.libX11).XOpenDisplay(xDisplayName);
        verify(this.libX11xcb).XGetXCBConnection(xDisplay);
        verify(this.libxcb).xcb_connection_has_error(xcbConnection);
        //an exception is thrown
        verifyNoMoreInteractions(this.libX11);
        verifyNoMoreInteractions(this.libX11xcb);
        verifyNoMoreInteractions(this.libxcb);
    }

    @Test
    public void testCreateWindowIdCreationError() throws Exception {
        //given
        this.exception.expect(RuntimeException.class);
        this.exception.expectMessage("failed to generate X window id");

        final String                        xDisplayName  = ":0";
        final int                           width         = 640;
        final int                           height        = 480;
        final Pointer                       xDisplay      = mock(Pointer.class);
        final Pointer                       xcbConnection = mock(Pointer.class);
        final Pointer                       setup         = mock(Pointer.class);
        final xcb_screen_t.ByReference      screen        = new xcb_screen_t.ByReference();
        final xcb_screen_iterator_t.ByValue screen_iter   = new xcb_screen_iterator_t.ByValue();
        screen_iter.data = screen;

        when(this.libX11.XOpenDisplay(xDisplayName)).thenReturn(xDisplay);
        when(this.libX11xcb.XGetXCBConnection(xDisplay)).thenReturn(xcbConnection);
        when(this.libxcb.xcb_connection_has_error(xcbConnection)).thenReturn(0);
        when(this.libxcb.xcb_get_setup(xcbConnection)).thenReturn(setup);
        when(this.libxcb.xcb_setup_roots_iterator(setup)).thenReturn(screen_iter);
        when(this.libxcb.xcb_generate_id(xcbConnection)).thenReturn(0);

        //when
        this.x11OutputFactory.create(xDisplayName,
                                     width,
                                     height);
        //then
        verify(this.libX11).XOpenDisplay(xDisplayName);
        verify(this.libX11xcb).XGetXCBConnection(xDisplay);
        verify(this.libxcb).xcb_connection_has_error(xcbConnection);
        verify(this.libxcb).xcb_get_setup(xcbConnection);
        verify(this.libxcb).xcb_setup_roots_iterator(setup);
        verify(this.libxcb).xcb_generate_id(xcbConnection);
        //an exception is thrown
        verifyNoMoreInteractions(this.libX11);
        verifyNoMoreInteractions(this.libX11xcb);
        verifyNoMoreInteractions(this.libxcb);
    }

    @Test
    public void testCreate() throws Exception {
        //given
        final EventLoop                        eventLoop     = mock(EventLoop.class);
        final EventSource                      eventSource   = mock(EventSource.class);
        final X11EventBus                      x11EventBus   = mock(X11EventBus.class);
        final String                           xDisplayName  = ":0";
        final int                              width         = 640;
        final int                              height        = 480;
        final Pointer                          xDisplay      = mock(Pointer.class);
        final Pointer                          xcbConnection = mock(Pointer.class);
        final Pointer                          setup         = mock(Pointer.class);
        final xcb_screen_t.ByReference         screen        = new xcb_screen_t.ByReference();
        final xcb_intern_atom_cookie_t.ByValue cookie        = new xcb_intern_atom_cookie_t.ByValue();
        final xcb_intern_atom_reply_t          atom_reply    = new xcb_intern_atom_reply_t();
        final int                              window        = 431;
        screen.root = 127;
        screen.root_visual = 10;
        screen.width_in_pixels = 640;
        screen.height_in_pixels = 480;
        final xcb_screen_iterator_t.ByValue screen_iter = new xcb_screen_iterator_t.ByValue();
        screen_iter.data = screen;

        when(this.libX11.XOpenDisplay(xDisplayName)).thenReturn(xDisplay);
        when(this.libX11xcb.XGetXCBConnection(xDisplay)).thenReturn(xcbConnection);
        when(this.libxcb.xcb_connection_has_error(xcbConnection)).thenReturn(0);
        when(this.libxcb.xcb_get_setup(xcbConnection)).thenReturn(setup);
        when(this.libxcb.xcb_setup_roots_iterator(setup)).thenReturn(screen_iter);
        when(this.libxcb.xcb_generate_id(xcbConnection)).thenReturn(window);
        when(this.display.getEventLoop()).thenReturn(eventLoop);
        when(this.x11EventBusFactory.create(xcbConnection)).thenReturn(x11EventBus);
        when(eventLoop.addFileDescriptor(anyInt(),
                                         anyInt(),
                                         any())).thenReturn(eventSource);
        when(this.libxcb.xcb_intern_atom(eq(xcbConnection),
                                         anyByte(),
                                         anyShort(),
                                         any())).thenReturn(cookie);
        when(this.libxcb.xcb_intern_atom_reply(xcbConnection,
                                               cookie,
                                               null)).thenReturn(atom_reply);
        //when
        this.x11OutputFactory.create(xDisplayName,
                                     width,
                                     height);
        //then
        verify(this.libX11).XOpenDisplay(xDisplayName);
        verify(this.libX11xcb).XGetXCBConnection(xDisplay);
        verify(this.libX11xcb).XSetEventQueueOwner(xDisplay,
                                                   XCBOwnsEventQueue);
        verify(this.libxcb).xcb_connection_has_error(xcbConnection);
        verify(this.libxcb).xcb_get_setup(xcbConnection);
        verify(this.libxcb).xcb_setup_roots_iterator(setup);
        verify(this.libxcb).xcb_generate_id(xcbConnection);
        verify(this.libxcb).xcb_get_file_descriptor(xcbConnection);
        verify(this.libxcb,
               atLeastOnce()).xcb_intern_atom(eq(xcbConnection),
                                              anyByte(),
                                              anyShort(),
                                              any());
        verify(this.libxcb,
               atLeastOnce()).xcb_intern_atom_reply(xcbConnection,
                                                    cookie,
                                                    null);
        verify(this.libxcb,
               atLeastOnce()).xcb_change_property(eq(xcbConnection),
                                                  anyByte(),
                                                  eq(window),
                                                  anyInt(),
                                                  anyInt(),
                                                  anyByte(),
                                                  anyInt(),
                                                  any());
        verify(this.libxcb).xcb_create_window(eq(xcbConnection),
                                              anyByte(),
                                              eq(window),
                                              eq(screen.root),
                                              anyShort(),
                                              anyShort(),
                                              eq((short) width),
                                              eq((short) height),
                                              anyShort(),
                                              eq((short) XCB_WINDOW_CLASS_INPUT_OUTPUT),
                                              eq(screen.root_visual),
                                              anyInt(),
                                              any());
        verify(this.libxcb).xcb_map_window(xcbConnection,
                                           window);
        verify(this.libxcb).xcb_flush(xcbConnection);

        verifyNoMoreInteractions(this.libX11);
        verifyNoMoreInteractions(this.libX11xcb);
        verifyNoMoreInteractions(this.libxcb);
    }
}