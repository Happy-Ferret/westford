/*
 * Westford Wayland Compositor.
 * Copyright (C) 2016  Erik De Rijcke
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.westford.compositor.launch.dispmanx;

import org.freedesktop.wayland.server.WlKeyboardResource;
import org.westford.compositor.core.KeyboardDevice;
import org.westford.compositor.core.LifeCycle;
import org.westford.compositor.core.PointerDevice;
import org.westford.compositor.core.TouchDevice;
import org.westford.compositor.protocol.WlKeyboard;
import org.westford.compositor.protocol.WlSeat;

import java.io.IOException;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Launcher {

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(final String[] args) throws IOException {
        configureLogger();
        LOGGER.info("Starting Westford");

        new Launcher().launch(DaggerDispmanxEglCompositor.create());
    }

    private static void configureLogger() throws IOException {
        final FileHandler fileHandler = new FileHandler("westford.log");
        fileHandler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(fileHandler);

        Thread.setDefaultUncaughtExceptionHandler((thread,
                                                   throwable) -> {
            LOGGER.severe("Got uncaught exception " + throwable.getMessage());
            throwable.printStackTrace();
        });
    }

    private void launch(final DispmanxEglCompositor dispmanxEglCompositor) {

        /*
         * Keep this first as weston demo clients *really* like their globals
         * to be initialized in a certain order, else they segfault...
         */
        final LifeCycle lifeCycle = dispmanxEglCompositor.lifeCycle();

        final WlSeat wlSeat = dispmanxEglCompositor.seatFactory()
                                                   .create("seat0",
                                                           "",
                                                           "",
                                                           "",
                                                           "",
                                                           "");

        /*
         * Setup keyboard focus tracking to follow mouse pointer & touch.
         */
        final PointerDevice pointerDevice = wlSeat.getWlPointer()
                                                  .getPointerDevice();
        final TouchDevice touchDevice = wlSeat.getWlTouch()
                                              .getTouchDevice();

        final WlKeyboard              wlKeyboard          = wlSeat.getWlKeyboard();
        final KeyboardDevice          keyboardDevice      = wlKeyboard.getKeyboardDevice();
        final Set<WlKeyboardResource> wlKeyboardResources = wlKeyboard.getResources();

        pointerDevice.getPointerFocusSignal()
                     .connect(event -> keyboardDevice.setFocus(wlKeyboardResources,
                                                               pointerDevice.getFocus()));
        touchDevice.getTouchDownSignal()
                   .connect(event -> keyboardDevice.setFocus(wlKeyboardResources,
                                                             touchDevice.getGrab()));
        /*
         * Start the compositor.
         */
        lifeCycle.start();
    }
}