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
import org.freedesktop.wayland.server.WlSurfaceResource;
import org.westmalle.wayland.protocol.WlCompositor;
import org.westmalle.wayland.protocol.WlSurface;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class SubsurfaceFactory {

    @Nonnull
    private final PrivateSubsurfaceFactory privateSubsurfaceFactory;
    @Nonnull
    private final Scene scene;

    @Inject
    SubsurfaceFactory(@Nonnull final PrivateSubsurfaceFactory privateSubsurfaceFactory,
                      @Nonnull final Scene scene) {
        this.privateSubsurfaceFactory = privateSubsurfaceFactory;
        this.scene = scene;
    }

    public Subsurface create(@Nonnull final WlSurfaceResource parentWlSurfaceResource,
                             @Nonnull final WlSurfaceResource wlSurfaceResource) {

        final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
        final Surface   surface   = wlSurface.getSurface();

        final Subsurface subsurface = this.privateSubsurfaceFactory.create(parentWlSurfaceResource,
                                                                           wlSurfaceResource,
                                                                           surface.getState());
        surface.getApplySurfaceStateSignal()
               .connect(subsurface::apply);

        final WlSurface parentWlSurface = (WlSurface) parentWlSurfaceResource.getImplementation();
        final Surface   parentSurface   = parentWlSurface.getSurface();

        parentSurface.getApplySurfaceStateSignal()
                     .connect((surfaceState) -> subsurface.onParentApply());
        parentSurface.getPositionSignal()
                     .connect(event -> subsurface.applyPosition());
        parentSurface.getRole()
                     .ifPresent(role -> {
                         if (role instanceof Subsurface) {
                             Subsurface parentSubsurface = (Subsurface) role;
                             parentSubsurface.getEffectiveSyncSignal()
                                             .connect(subsurface::updateEffectiveSync);
                         }
                     });

        this.scene.getSurfacesStack()
                  .remove(wlSurfaceResource);
        this.scene.getSubsurfaceStack(parentWlSurfaceResource)
                  .addLast(wlSurfaceResource);

        final DestroyListener destroyListener = () -> {
            this.scene.getSubsurfaceStack(parentWlSurfaceResource)
                      .remove(wlSurfaceResource);
            this.scene.getPendingSubsurfaceStack(parentWlSurfaceResource)
                      .remove(wlSurfaceResource);
        };
        wlSurfaceResource.register(destroyListener);

        parentWlSurfaceResource.register(() -> {
            /*
             * A destroyed parent will have it's stack of subsurfaces removed, so no need to remove the subsurface
             * from that stack (which is done in the subsurface destroy listener).
             */
            wlSurfaceResource.unregister(destroyListener);
            /*
             * Docs says a subsurface with a destroyed parent must become inert.
             */
            subsurface.setInert(true);
        });

        return subsurface;
    }
}
