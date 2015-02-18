package org.westmalle.wayland.output;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;

import static com.google.common.truth.Truth.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class RegionTest {

    @InjectMocks
    private Region region;

    @Test
    public void testAdd() throws Exception {
        //given
        RectangleImmutable rect0 = new Rectangle(0,
                                                 0,
                                                 100,
                                                 100);
        RectangleImmutable rect1 = new Rectangle(50,
                                                 50,
                                                 100,
                                                 100);
        //when
        region.add(rect0);
        region.add(rect1);
        //then
        final List<RectangleImmutable> rectangleImmutables = region.asList();
        assertThat(rectangleImmutables).hasSize(3);
        assertThat(rectangleImmutables.get(0)).isEqualTo(new Rectangle(0,
                                                                       0,
                                                                       100,
                                                                       50));
        assertThat(rectangleImmutables.get(1)).isEqualTo(new Rectangle(0,
                                                                       50,
                                                                       150,
                                                                       50));
        assertThat(rectangleImmutables.get(2)).isEqualTo(new Rectangle(50,
                                                                       100,
                                                                       100,
                                                                       50));
    }

    @Test
    public void testSubtract() throws Exception {
        //given
        RectangleImmutable rect0 = new Rectangle(0,
                                                 0,
                                                 100,
                                                 100);
        RectangleImmutable rect1 = new Rectangle(50,
                                                 50,
                                                 100,
                                                 100);
        //when
        region.add(rect0);
        region.subtract(rect1);
        //then
        final List<RectangleImmutable> rectangleImmutables = region.asList();
        assertThat(rectangleImmutables).hasSize(2);
        assertThat(rectangleImmutables.get(0)).isEqualTo(new Rectangle(100,
                                                                       50,
                                                                       50,
                                                                       50));
        assertThat(rectangleImmutables.get(1)).isEqualTo(new Rectangle(50,
                                                                       100,
                                                                       100,
                                                                       50));
    }
}