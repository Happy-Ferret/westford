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
package org.westmalle.wayland.protocol;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import org.freedesktop.wayland.server.Client;
import org.freedesktop.wayland.server.WlSubsurfaceRequests;
import org.freedesktop.wayland.server.WlSubsurfaceResource;
import org.freedesktop.wayland.server.WlSurfaceResource;
import org.freedesktop.wayland.shared.WlSubsurfaceError;
import org.westmalle.wayland.core.Point;
import org.westmalle.wayland.core.Scene;
import org.westmalle.wayland.core.Subsurface;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@AutoFactory(className = "WlSubsurfaceFactory")
public class WlSubsurface implements WlSubsurfaceRequests,
                                     ProtocolObject<WlSubsurfaceResource> {

    private final Set<WlSubsurfaceResource> resources = Collections.newSetFromMap(new WeakHashMap<>());

    @Nonnull
    private final Subsurface subsurface;
    @Nonnull
    private final Scene scene;

    WlSubsurface(@Nonnull @Provided final Scene scene,
                 @Nonnull final Subsurface subsurface) {
        this.subsurface = subsurface;
        this.scene = scene;
    }

    @Nonnull
    @Override
    public WlSubsurfaceResource create(@Nonnull final Client client,
                                       @Nonnegative final int version,
                                       final int id) {
        return new WlSubsurfaceResource(client,
                                        version,
                                        id,
                                        this);
    }

    @Nonnull
    @Override
    public Set<WlSubsurfaceResource> getResources() {
        return this.resources;
    }

    @Override
    public void destroy(final WlSubsurfaceResource resource) {
        resource.destroy();
    }

    @Override
    public void setPosition(final WlSubsurfaceResource wlSubsurfaceResource,
                            final int x,
                            final int y) {
        getSubsurface().setPosition(Point.create(x,
                                                 y));
    }

    @Override
    public void placeAbove(final WlSubsurfaceResource requester,
                           @Nonnull final WlSurfaceResource sibling) {
        //TODO unit test
        if (isValid(sibling)) {
            getSubsurface().above(sibling);
        }
        else {
            requester.postError(WlSubsurfaceError.BAD_SURFACE.value,
                                "placeAbove request failed. wl_surface is not a sibling or the parent");
        }
    }

    private boolean isValid(final WlSurfaceResource sibling) {
        final Subsurface subsurface = getSubsurface();
        if (subsurface.isInert()) {
            /*
             * we return true here as a the docs say that a subsurface with a destroyed parent should become inert
             * ie we don't care what the sibling argument is, as the request will be ignored anyway.
             */
            return true;
        }

        return this.scene.getSubsurfaceStack(subsurface.getParentWlSurfaceResource())
                         .contains(sibling);
    }

    @Override
    public void placeBelow(final WlSubsurfaceResource requester,
                           @Nonnull final WlSurfaceResource sibling) {
        //TODO unit test
        if (isValid(sibling)) {
            getSubsurface().below(sibling);
        }
        else {
            requester.postError(WlSubsurfaceError.BAD_SURFACE.value,
                                "placeBelow request failed. wl_surface is not a sibling or the parent");
        }
    }

    @Override
    public void setSync(final WlSubsurfaceResource requester) {
        getSubsurface().setSync(true);
    }

    @Override
    public void setDesync(final WlSubsurfaceResource requester) {
        getSubsurface().setSync(false);
    }

    @Nonnull
    public Subsurface getSubsurface() {
        return this.subsurface;
    }
}
