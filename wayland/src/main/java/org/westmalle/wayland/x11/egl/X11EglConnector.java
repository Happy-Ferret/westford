//Copyright 2016 Erik De Rijcke
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
package org.westmalle.wayland.x11.egl;


import com.google.auto.factory.AutoFactory;
import org.westmalle.wayland.core.EglConnector;
import org.westmalle.wayland.protocol.WlOutput;
import org.westmalle.wayland.x11.X11Connector;

import javax.annotation.Nonnull;
import java.util.Optional;

@AutoFactory(allowSubclasses = true,
             className = "X11EglConnectorFactory")
public class X11EglConnector implements EglConnector {

    @Nonnull
    private final X11Connector x11Connector;
    private final long         eglSurface;


    X11EglConnector(@Nonnull final X11Connector x11Connector,
                    final long eglSurface) {
        this.x11Connector = x11Connector;
        this.eglSurface = eglSurface;
    }

    @Override
    public long getEglSurface() {
        return this.eglSurface;
    }


    @Nonnull
    @Override
    public Optional<WlOutput> getWlOutput() {
        return this.x11Connector.getWlOutput();
    }

    @Nonnull
    public X11Connector getX11Connector() {
        return this.x11Connector;
    }
}