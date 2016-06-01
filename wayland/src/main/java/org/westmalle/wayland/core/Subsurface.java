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

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import org.freedesktop.wayland.server.WlCompositorResource;
import org.freedesktop.wayland.server.WlSurfaceResource;
import org.westmalle.wayland.core.events.Signal;
import org.westmalle.wayland.core.events.Slot;
import org.westmalle.wayland.protocol.WlCompositor;
import org.westmalle.wayland.protocol.WlSurface;

import javax.annotation.Nonnull;
import java.util.LinkedList;

@AutoFactory(allowSubclasses = true,
             className = "PrivateSubsurfaceFactory")
public class Subsurface implements Role {

    @Nonnull
    private final Scene scene;
    @Nonnull
    private final WlSurfaceResource parentWlSurfaceResource;
    @Nonnull
    private final WlSurfaceResource wlSurfaceResource;

    private final Signal<Boolean, Slot<Boolean>> effectiveSyncSignal = new Signal<>();
    private       boolean                        effectiveSync       = true;

    private boolean inert    = false;
    private boolean sync     = true;
    @Nonnull
    private Point   position = Point.ZERO;
    @Nonnull
    private SurfaceState surfaceState;
    @Nonnull
    private SurfaceState cachedSurfaceState;

    Subsurface(@Nonnull @Provided final Scene scene,
               @Nonnull final WlSurfaceResource parentWlSurfaceResource,
               @Nonnull final WlSurfaceResource wlSurfaceResource,
               @Nonnull final SurfaceState surfaceState) {
        this.scene = scene;
        this.parentWlSurfaceResource = parentWlSurfaceResource;
        this.wlSurfaceResource = wlSurfaceResource;
        this.surfaceState = surfaceState;
        this.cachedSurfaceState = surfaceState;
    }

    public void setPosition(@Nonnull final Point position) {
        if (isInert()) {
            return;
        }

        this.position = position;
    }

    @Nonnull
    public Point getPosition() {
        return this.position;
    }

    public void applyPosition() {
        if (isInert()) {
            return;
        }

        final WlSurface parentWlSurface = (WlSurface) getParentWlSurfaceResource().getImplementation();
        final WlSurface wlSurface       = (WlSurface) getWlSurfaceResource().getImplementation();

        final Surface parentSurface = parentWlSurface.getSurface();
        final Point   global        = parentSurface.global(getPosition());

        wlSurface.getSurface()
                 .setPosition(global);
    }

    @Override
    public void beforeCommit(@Nonnull final WlSurfaceResource wlSurfaceResource) {
        if (isInert()) {
            return;
        }

        if (isEffectiveSync()) {
            final WlSurface wlSurface = (WlSurface) wlSurfaceResource.getImplementation();
            final Surface surface = wlSurface.getSurface();

            //set back cached state so surface can do eg. buffer release
            surface.setState(getCachedSurfaceState());
        }
    }

    public void apply(final SurfaceState surfaceState) {
        if (isInert()) {
            return;
        }

        if (isEffectiveSync()) {
            final WlSurface wlSurface = (WlSurface) getWlSurfaceResource().getImplementation();
            final Surface surface = wlSurface.getSurface();
            final SurfaceState oldSurfaceState = getSurfaceState();
            if (!surface.getState()
                        .equals(oldSurfaceState)) {
                //replace new state with old state
                surface.apply(oldSurfaceState);
                this.cachedSurfaceState = surfaceState;
            }
        }
        else {
            //desync mode, our 'old' state is always the newest state.
            this.cachedSurfaceState = surfaceState;
            this.surfaceState = surfaceState;
        }
    }

    public void onParentApply() {
        if (isInert()) {
            return;
        }

        final SurfaceState cachedSurfaceState = getCachedSurfaceState();
        if (isEffectiveSync() &&
            !getSurfaceState().equals(cachedSurfaceState)) {
            //sync mode. update old state with cached state
            this.surfaceState = cachedSurfaceState;
            apply(cachedSurfaceState);
        }

        applyPosition();
    }

    public void setSync(final boolean sync) {
        if (isInert()) {
            return;
        }

        this.sync = sync;

        final WlSurface parentWlSurface = (WlSurface) getParentWlSurfaceResource().getImplementation();
        parentWlSurface.getSurface()
                       .getRole()
                       .ifPresent(role -> {
                           if (role instanceof Subsurface) {
                               //TODO unit test this
                               final Subsurface parentSubsurface = (Subsurface) role;
                               updateEffectiveSync(parentSubsurface.isEffectiveSync());
                           }
                           else {
                               updateEffectiveSync(false);
                           }
                       });
    }

    public boolean isEffectiveSync() {
        return this.effectiveSync;
    }

    public void updateEffectiveSync(final boolean parentEffectiveSync) {
        final boolean oldEffectiveSync = this.effectiveSync;
        this.effectiveSync = this.sync || parentEffectiveSync;

        if (oldEffectiveSync != isEffectiveSync()) {
            /*
             * If we were in sync mode and now our effective mode is desync, we have to apply our cached state
             * immediately
             */
            //TODO unit test this
            if (!isEffectiveSync()) {
                final WlSurface wlSurface = (WlSurface) getWlSurfaceResource().getImplementation();
                wlSurface.getSurface()
                         .apply(getCachedSurfaceState());
            }

            getEffectiveSyncSignal().emit(isEffectiveSync());
        }
    }

    public void above(@Nonnull final WlSurfaceResource sibling) {
        if (isInert()) {
            return;
        }

        placement(false,
                  sibling);
    }

    public void below(@Nonnull final WlSurfaceResource sibling) {
        if (isInert()) {
            return;
        }

        placement(true,
                  sibling);
    }

    private void placement(final boolean below,
                           final WlSurfaceResource sibling) {
        final LinkedList<WlSurfaceResource> pendingSubsurfaceStack = this.scene.getPendingSubsurfaceStack(getParentWlSurfaceResource());
        final int                           siblingPosition        = pendingSubsurfaceStack.indexOf(sibling);

        pendingSubsurfaceStack.remove(getWlSurfaceResource());
        pendingSubsurfaceStack.add(below ? siblingPosition : siblingPosition + 1,
                                   getWlSurfaceResource());

        //Note: committing the subsurface stack happens in WlCompositor.
    }

    @Nonnull
    public SurfaceState getSurfaceState() {
        return this.surfaceState;
    }

    @Nonnull
    public SurfaceState getCachedSurfaceState() {
        return this.cachedSurfaceState;
    }

    @Nonnull
    public WlSurfaceResource getWlSurfaceResource() {
        return this.wlSurfaceResource;
    }

    @Nonnull
    public WlSurfaceResource getParentWlSurfaceResource() {
        return this.parentWlSurfaceResource;
    }

    public boolean isInert() {
        return this.inert;
    }

    public void setInert(final boolean inert) {
        this.inert = inert;
    }

    public Signal<Boolean, Slot<Boolean>> getEffectiveSyncSignal() {
        return this.effectiveSyncSignal;
    }
}
