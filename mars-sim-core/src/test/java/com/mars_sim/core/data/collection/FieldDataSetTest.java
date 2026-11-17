/*
 * Mars Simulation Project
 * FieldDataSetTest.java
 * @date 2026-10-17
 * @author Branislav Blicha
 */
package com.mars_sim.core.data.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.mars_sim.core.EntityEvent;
import com.mars_sim.core.EntityListener;
import com.mars_sim.core.person.ai.task.util.Worker;

class FieldDataSetTest {

    private Worker owner;
    private FieldDataSet data;

    @BeforeEach
    void setUp() {
        // Stand in for the person who collected the data, the data set only keeps a reference
        owner = mock(Worker.class);
        data = new MineralData(null, owner, DataType.GROUND_DATA, null, 40);
    }

    @Test
    void testStoresDataAndCapsQuality() {
        assertEquals(DataType.GROUND_DATA, data.getDataType());
        assertSame(owner, data.getOwner());

        data.addQuality(60);
        data.addQuality(60);
        assertEquals(100, data.getQuality(), "Quality is capped at 100");

        data.addResearchTime(1.5);
        data.addResearchTime(2.5);
        data.addWorkTime(3);
        assertEquals(4.0, data.getResearchTime(), 1e-9);
        assertEquals(3.0, data.getWorkTime(), 1e-9);
    }

    @Test
    void testListenerGetsUpdateEvent() {
        EntityListener listener = mock(EntityListener.class);
        data.addEntityListener(listener);

        data.fireUpdate("quality");

        ArgumentCaptor<EntityEvent> event = ArgumentCaptor.forClass(EntityEvent.class);
        verify(listener).entityUpdate(event.capture());
        assertEquals("quality", event.getValue().getType());
        assertSame(data, event.getValue().getSource());
        assertSame(owner, event.getValue().getTarget());
    }

    @Test
    void testFailingListenerDoesNotStopOthers() {
        EntityListener broken = mock(EntityListener.class);
        doThrow(new IllegalStateException("broken listener")).when(broken).entityUpdate(any());
        EntityListener working = mock(EntityListener.class);
        data.addEntityListener(broken);
        data.addEntityListener(working);

        data.fireUpdate("quality");

        verify(broken).entityUpdate(any());
        verify(working).entityUpdate(any());
    }

    @Test
    void testRemovedListenerIsNotNotified() {
        EntityListener listener = mock(EntityListener.class);
        data.addEntityListener(listener);
        data.removeEntityListener(listener);

        data.fireUpdate("quality");

        verifyNoInteractions(listener);
        assertTrue(data.getEntityListeners().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> data.addEntityListener(null));
    }
}
