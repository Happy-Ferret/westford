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
package org.westmalle.wayland.jogl;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.NEWTEvent;
import org.freedesktop.wayland.server.WlPointerResource;
import org.freedesktop.wayland.shared.WlPointerButtonState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.westmalle.wayland.output.JobExecutor;
import org.westmalle.wayland.output.PointerDevice;
import org.westmalle.wayland.protocol.WlPointer;
import org.westmalle.wayland.protocol.WlSeat;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
                        //following classes are final, so we have to powermock them:
                        NEWTEvent.class,
                        MouseEvent.class
                })
public class JoglSeatTest {

    @Mock
    private WlSeat      wlSeat;
    @Mock
    private JobExecutor jobExecutor;
    @InjectMocks
    private JoglSeat    joglSeat;

    @Before
    public void setUp() {
        doAnswer(invocation -> {
            final Object arg0 = invocation.getArguments()[0];
            final Runnable runnable = (Runnable) arg0;
            runnable.run();
            return null;
        }).when(this.jobExecutor)
          .submit(any());
    }

    @Test
    public void testMousePressed() throws Exception {
        //given
        final MouseEvent mouseEvent = mock(MouseEvent.class);
        final long       time       = 87654;
        when(mouseEvent.getWhen()).thenReturn(time);
        final short button = 3;
        when(mouseEvent.getButton()).thenReturn(button);

        final WlPointer wlPointer = mock(WlPointer.class);
        when(this.wlSeat.getOptionalWlPointer()).thenReturn(Optional.of(wlPointer));

        final WlPointerResource      wlPointerResource  = mock(WlPointerResource.class);
        final Set<WlPointerResource> wlPointerResources = new HashSet<>();
        wlPointerResources.add(wlPointerResource);
        when(wlPointer.getResources()).thenReturn(wlPointerResources);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);

        this.wlSeat.setWlPointer(wlPointer);
        //when
        this.joglSeat.mousePressed(mouseEvent);
        //then
        verify(pointerDevice).button(wlPointerResources,
                                     (int) time,
                                     button,
                                     WlPointerButtonState.PRESSED);
    }

    @Test
    public void testMouseReleased() throws Exception {
        //given
        final MouseEvent mouseEvent = mock(MouseEvent.class);
        final long       time       = 87654;
        when(mouseEvent.getWhen()).thenReturn(time);
        final short button = 3;
        when(mouseEvent.getButton()).thenReturn(button);

        final WlPointer wlPointer = mock(WlPointer.class);
        when(this.wlSeat.getOptionalWlPointer()).thenReturn(Optional.of(wlPointer));

        final WlPointerResource      wlPointerResource  = mock(WlPointerResource.class);
        final Set<WlPointerResource> wlPointerResources = new HashSet<>();
        wlPointerResources.add(wlPointerResource);
        when(wlPointer.getResources()).thenReturn(wlPointerResources);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);

        this.wlSeat.setWlPointer(wlPointer);
        //when
        this.joglSeat.mouseReleased(mouseEvent);
        //then
        verify(pointerDevice).button(wlPointerResources,
                                     (int) time,
                                     button,
                                     WlPointerButtonState.RELEASED);
    }

    @Test
    public void testMouseMoved() throws Exception {
        //given
        final MouseEvent mouseEvent = mock(MouseEvent.class);
        final long       time       = 87654;
        when(mouseEvent.getWhen()).thenReturn(time);
        final int x = 321;
        final int y = 543;
        when(mouseEvent.getX()).thenReturn(x);
        when(mouseEvent.getY()).thenReturn(y);

        final WlPointer wlPointer = mock(WlPointer.class);
        when(this.wlSeat.getOptionalWlPointer()).thenReturn(Optional.of(wlPointer));

        final WlPointerResource      wlPointerResource  = mock(WlPointerResource.class);
        final Set<WlPointerResource> wlPointerResources = new HashSet<>();
        wlPointerResources.add(wlPointerResource);
        when(wlPointer.getResources()).thenReturn(wlPointerResources);

        final PointerDevice pointerDevice = mock(PointerDevice.class);
        when(wlPointer.getPointerDevice()).thenReturn(pointerDevice);

        this.wlSeat.setWlPointer(wlPointer);
        //when
        this.joglSeat.mouseMoved(mouseEvent);
        //then
        verify(pointerDevice).motion(wlPointerResources,
                                     (int) time,
                                     x,
                                     y);
    }
}