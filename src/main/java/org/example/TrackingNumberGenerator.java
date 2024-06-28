package org.example;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TrackingNumberGenerator {
    private final long datacenterId;
    private final long workerId;
    private long sequence = 0L;
    public long lastTimestamp = -1L;

    private final long datacenterIdBits = 5L;
    private final long workerIdBits = 5L;
    private final long sequenceBits = 12L;

    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxSequence = -1L ^ (-1L << sequenceBits);

    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    private final long epoch = 1288834974657L;

    private final Lock lock = new ReentrantLock();

    private static final Map<String, String> countryCodeMap = new HashMap<>();
    private static final Random random = new Random();

    static {
        countryCodeMap.put("USA", "US");
        countryCodeMap.put("Canada", "CA");
        countryCodeMap.put("United Kingdom", "UK");
        // Add more countries as needed
    }

    public TrackingNumberGenerator(long datacenterId, long workerId) {
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("Datacenter ID can't be greater than %d or less than 0", maxDatacenterId));
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("Worker ID can't be greater than %d or less than 0", maxWorkerId));
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private String getCountryCode(String country) {
        return countryCodeMap.getOrDefault(country, "XX");
    }

    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }

    public String generateTrackingNumber(String country, String localAddress) {
        lock.lock();
        try {
            long timestamp = timeGen();

            if (timestamp < lastTimestamp) {
                throw new RuntimeException("Clock moved backwards. Refusing to generate id");
            }

            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & maxSequence;
                if (sequence == 0) {
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            long id = ((timestamp - epoch) << timestampLeftShift) |
                    (datacenterId << datacenterIdShift) |
                    (workerId << workerIdShift) |
                    sequence;

            String countryCode = getCountryCode(country);
            String uniquePart = String.format("%06d", id % 1000000); // Adjusted unique part for longer format
            String randomPart = generateRandomString(5); // 5 random alphanumeric characters

            return countryCode + "-" + localAddress + "-" + uniquePart + "-" + randomPart;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        TrackingNumberGenerator generator = new TrackingNumberGenerator(1, 1);
        for (int i = 0; i < 10; i++) {
            System.out.println(generator.generateTrackingNumber("United Kingdom", "LDN"));
        }
    }
}
