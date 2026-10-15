package com.sqljudge.common.persistence;

import java.security.SecureRandom;
import java.util.UUID;

public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;
    private static final long VERSION_BITS = 0x7000L;
    private static final int RAND_A_BOUND = 1 << 12;
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;
    private static final long VARIANT_BITS = 0x8000_0000_0000_0000L;

    private UuidV7() {}

    public static UUID generate() {
        return generate(System.currentTimeMillis());
    }

    public static UUID generate(long epochMillis) {
        long mostSignificantBits =
                ((epochMillis & TIMESTAMP_MASK) << 16)
                        | VERSION_BITS
                        | RANDOM.nextInt(RAND_A_BOUND);
        long leastSignificantBits = (RANDOM.nextLong() & RAND_B_MASK) | VARIANT_BITS;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    public static long timestampMillis(UUID uuid) {
        return uuid.getMostSignificantBits() >>> 16;
    }
}
