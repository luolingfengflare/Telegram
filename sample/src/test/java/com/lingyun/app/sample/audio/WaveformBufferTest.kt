package com.lingyun.app.sample.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WaveformBufferTest {

    @Test
    fun newBuffer_isAllZeros() {
        val buf = WaveformBuffer()
        assertArrayEquals(FloatArray(8) { 0f }, buf.snapshot(playing = false), 0.0001f)
    }

    @Test
    fun push_advancesRingByOne() {
        val buf = WaveformBuffer()
        buf.push(0.5f)
        val snap = buf.snapshot(playing = true)
        // The newest sample lands at index 6 (the "current amplitude" slot)
        assertEquals(0.5f, snap[6], 0.0001f)
        // The "playing" flag lands at index 7
        assertEquals(1f, snap[7], 0.0001f)
    }

    @Test
    fun push_fillsSlots0to5_withRecentHistory_inOrder() {
        val buf = WaveformBuffer()
        // Push 7 samples; the buffer should retain the last 6 in slots 0..5
        listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f).forEach { buf.push(it) }
        val snap = buf.snapshot(playing = true)
        // Slots 0..5 hold the 6 most recent BEFORE the current sample (0.2..0.7
        // since 0.1 has been pushed out), index 0 = oldest, index 5 = newest
        assertEquals(0.2f, snap[0], 0.0001f)
        assertEquals(0.3f, snap[1], 0.0001f)
        assertEquals(0.4f, snap[2], 0.0001f)
        assertEquals(0.5f, snap[3], 0.0001f)
        assertEquals(0.6f, snap[4], 0.0001f)
        assertEquals(0.7f, snap[5], 0.0001f)
        assertEquals(0.7f, snap[6], 0.0001f) // current
        assertEquals(1f, snap[7], 0.0001f)
    }

    @Test
    fun snapshot_clampsInputsTo_0_to_1() {
        val buf = WaveformBuffer()
        buf.push(-0.5f)
        buf.push(1.5f)
        val snap = buf.snapshot(playing = true)
        // Negative clamped to 0, > 1 clamped to 1
        assertEquals(0f, snap[4], 0.0001f) // -0.5 → 0
        assertEquals(1f, snap[5], 0.0001f) // 1.5  → 1
        assertEquals(1f, snap[6], 0.0001f) // current = last pushed (1.5 clamped)
    }

    @Test
    fun playingFlag_isReflectedInSlot7() {
        val buf = WaveformBuffer()
        buf.push(0.3f)
        assertEquals(1f, buf.snapshot(playing = true)[7], 0.0001f)
        assertEquals(0f, buf.snapshot(playing = false)[7], 0.0001f)
    }
}
