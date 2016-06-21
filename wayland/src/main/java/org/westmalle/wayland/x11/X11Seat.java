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
package org.westmalle.wayland.x11;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import org.freedesktop.wayland.shared.WlKeyboardKeyState;
import org.freedesktop.wayland.shared.WlPointerAxis;
import org.freedesktop.wayland.shared.WlPointerButtonState;
import org.westmalle.wayland.core.PointerDevice;
import org.westmalle.wayland.nativ.libxcb.Libxcb;
import org.westmalle.wayland.protocol.WlKeyboard;
import org.westmalle.wayland.protocol.WlPointer;
import org.westmalle.wayland.protocol.WlSeat;

import javax.annotation.Nonnull;

import static org.freedesktop.wayland.shared.WlPointerAxis.HORIZONTAL_SCROLL;
import static org.freedesktop.wayland.shared.WlPointerAxis.VERTICAL_SCROLL;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_CURSOR_NONE;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_BUTTON_PRESS;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_BUTTON_RELEASE;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_ENTER_WINDOW;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_LEAVE_WINDOW;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_EVENT_MASK_POINTER_MOTION;
import static org.westmalle.wayland.nativ.libxcb.Libxcb.XCB_GRAB_MODE_ASYNC;
import static org.westmalle.wayland.nativ.linux.Input.BTN_LEFT;
import static org.westmalle.wayland.nativ.linux.Input.BTN_MIDDLE;
import static org.westmalle.wayland.nativ.linux.Input.BTN_RIGHT;
import static org.westmalle.wayland.nativ.linux.Input.BTN_SIDE;

@AutoFactory(className = "PrivateX11SeatFactory",
             allowSubclasses = true)
public class X11Seat {

    private static final float DEFAULT_AXIS_STEP_DISTANCE = 10.0f;

    @Nonnull
    private final Libxcb      libxcb;
    @Nonnull
    private final X11Platform x11Platform;
    @Nonnull
    private final WlSeat      wlSeat;

    X11Seat(@Provided @Nonnull final Libxcb libxcb,
            @Nonnull final X11Platform x11Platform,
            @Nonnull final WlSeat wlSeat) {
        this.libxcb = libxcb;
        this.x11Platform = x11Platform;
        this.wlSeat = wlSeat;
    }

    public void deliverKey(final int time,
                           final short eventDetail,
                           final boolean pressed) {
        final WlKeyboardKeyState wlKeyboardKeyState = wlKeyboardKeyState(pressed);
        final int                key                = toLinuxKey(eventDetail);
        final WlKeyboard         wlKeyboard         = this.wlSeat.getWlKeyboard();

        wlKeyboard.getKeyboardDevice()
                  .key(wlKeyboard.getResources(),
                       time,
                       key,
                       wlKeyboardKeyState);
    }

    private WlKeyboardKeyState wlKeyboardKeyState(final boolean pressed) {
        final WlKeyboardKeyState wlKeyboardKeyState;
        if (pressed) {
            wlKeyboardKeyState = WlKeyboardKeyState.PRESSED;
        }
        else {
            wlKeyboardKeyState = WlKeyboardKeyState.RELEASED;
        }
        return wlKeyboardKeyState;
    }

    private int toLinuxKey(final short eventDetail) {
        //convert from X keycodes to input.h keycodes
        return eventDetail - 8;
    }

    public void deliverButton(final int window,
                              final int buttonTime,
                              final short eventDetail,
                              final boolean pressed) {

        final WlPointerButtonState wlPointerButtonState = wlPointerButtonState(window,
                                                                               buttonTime,
                                                                               pressed);
        final int button = toLinuxButton(eventDetail);
        if (button == 0 && pressed) {
            handleScroll(buttonTime,
                         eventDetail);
        }
        else if (button != 0) {
            final WlPointer     wlPointer     = this.wlSeat.getWlPointer();
            final PointerDevice pointerDevice = wlPointer.getPointerDevice();

            pointerDevice.button(wlPointer.getResources(),
                                 buttonTime,
                                 button,
                                 wlPointerButtonState);
            pointerDevice.frame(wlPointer.getResources());
        }
    }

    private void handleScroll(final int buttonTime,
                              final short eventDetail) {

        final WlPointerAxis wlPointerAxis;
        final float         value;
        final int           discreteValue;

        if (eventDetail == 4 || eventDetail == 5) {
            wlPointerAxis = VERTICAL_SCROLL;
            value = eventDetail == 4 ? -DEFAULT_AXIS_STEP_DISTANCE : DEFAULT_AXIS_STEP_DISTANCE;
            discreteValue = eventDetail == 4 ? -1 : 1;
        }
        else {
            wlPointerAxis = HORIZONTAL_SCROLL;
            value = eventDetail == 6 ? -DEFAULT_AXIS_STEP_DISTANCE : DEFAULT_AXIS_STEP_DISTANCE;
            discreteValue = eventDetail == 6 ? -1 : 1;
        }

        final WlPointer     wlPointer     = this.wlSeat.getWlPointer();
        final PointerDevice pointerDevice = wlPointer.getPointerDevice();

        pointerDevice.axisDiscrete(wlPointer.getResources(),
                                   wlPointerAxis,
                                   buttonTime,
                                   discreteValue,
                                   value);
        pointerDevice.frame(wlPointer.getResources());
    }

    private WlPointerButtonState wlPointerButtonState(final int window,
                                                      final int buttonTime,
                                                      final boolean pressed) {
        final WlPointerButtonState wlPointerButtonState;
        if (pressed) {
            wlPointerButtonState = WlPointerButtonState.PRESSED;
            this.libxcb.xcb_grab_pointer(this.x11Platform.getXcbConnection(),
                                         (byte) 0,
                                         window,
                                         (short) (XCB_EVENT_MASK_BUTTON_PRESS |
                                                  XCB_EVENT_MASK_BUTTON_RELEASE |
                                                  XCB_EVENT_MASK_POINTER_MOTION |
                                                  XCB_EVENT_MASK_ENTER_WINDOW |
                                                  XCB_EVENT_MASK_LEAVE_WINDOW),
                                         (byte) XCB_GRAB_MODE_ASYNC,
                                         (byte) XCB_GRAB_MODE_ASYNC,
                                         window,
                                         XCB_CURSOR_NONE,
                                         buttonTime);
        }
        else {
            this.libxcb.xcb_ungrab_pointer(this.x11Platform.getXcbConnection(),
                                           buttonTime);
            wlPointerButtonState = WlPointerButtonState.RELEASED;
        }
        return wlPointerButtonState;
    }

    private int toLinuxButton(final int eventDetail) {
        final int button;
        switch (eventDetail) {
            case 1:
                button = BTN_LEFT;
                break;
            case 2:
                button = BTN_MIDDLE;
                break;
            case 3:
                button = BTN_RIGHT;
                break;
            case 4:
            case 5:
            case 6:
            case 7:
                //scroll
                button = 0;
                break;
            default:
                button = eventDetail + BTN_SIDE - 8;
        }
        return button;
    }

    public void deliverMotion(final int time,
                              final int x,
                              final int y) {
        final WlPointer     wlPointer     = this.wlSeat.getWlPointer();
        final PointerDevice pointerDevice = wlPointer.getPointerDevice();

        pointerDevice.motion(wlPointer.getResources(),
                             time,
                             x,
                             y);
        pointerDevice.frame(wlPointer.getResources());
    }

    @Nonnull
    public X11Platform getX11Platform() {
        return this.x11Platform;
    }

    @Nonnull
    public WlSeat getWlSeat() {
        return this.wlSeat;
    }
}
