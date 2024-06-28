package org.example;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class TrackingNumberGeneratorTest {

    @Test
    public void testConcurrency() throws InterruptedException {
        TrackingNumberGenerator generator = new TrackingNumberGenerator(1, 1);
        int numThreads = 10;
        int numTrackingNumbersPerThread = 10000;
        Set<String> generatedNumbers = new HashSet<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < numTrackingNumbersPerThread; j++) {
                    String trackingNumber = generator.generateTrackingNumber("USA", "NYC");
                    synchronized (generatedNumbers) {
                        assertFalse(generatedNumbers.contains(trackingNumber));
                        generatedNumbers.add(trackingNumber);
                    }
                }
            });
        }

        executorService.shutdown();
        assertTrue(executorService.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(numThreads * numTrackingNumbersPerThread, generatedNumbers.size());
    }


    @Test
    public void testInvalidIds() {
        assertThrows(IllegalArgumentException.class, () -> new TrackingNumberGenerator(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new TrackingNumberGenerator(1, -1));
    }

    @Test
    public void testClockBackwards() {
        TrackingNumberGenerator generator = new TrackingNumberGenerator(1, 1);
        generator.generateTrackingNumber("USA", "NYC");
        // Simulate a backward clock movement scenario
        generator.lastTimestamp = System.currentTimeMillis() + 1000; // Set last timestamp in the future
        assertThrows(RuntimeException.class, () -> generator.generateTrackingNumber("USA", "NYC"));
    }

    @Test
    public void testGenerateTrackingNumber() {
        TrackingNumberGenerator generator = new TrackingNumberGenerator(1, 1);

        String trackingNumber = generator.generateTrackingNumber("United Kingdom", "LDN");
        assertNotNull(trackingNumber);
        assertTrue(trackingNumber.matches("[A-Z]{2}-LDN-\\d{6}-[A-Z0-9]{5}"));
    }

}
