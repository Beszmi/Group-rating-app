package com.example;

/**
 * Per-member min/max integer percentages for a fixed total of 100.
 * Anchored at n=5: min=5, max=35 (half-width 15 from equal split 20%).
 * Tighter for more members, looser for fewer.
 */
public final class RatingBounds {
    public static final int MIN_MEMBERS = 3;
    public static final int MAX_MEMBERS = 20;

    private RatingBounds() {
    }

    public static int minRating(int memberCount) {
        return compute(memberCount)[0];
    }

    public static int maxRating(int memberCount) {
        return compute(memberCount)[1];
    }

    /**
     * @return min and max inclusive; chosen so valid integer splits summing to 100 exist.
     */
    public static int[] compute(int n) {
        if (n < MIN_MEMBERS || n > MAX_MEMBERS) {
            throw new IllegalArgumentException("memberCount must be between " + MIN_MEMBERS + " and " + MAX_MEMBERS);
        }
        double avg = 100.0 / n;
        double halfWidth;
        if (n <= 5) {
            halfWidth = 15.0 + (5 - n) * 5.0;
        } else {
            halfWidth = 15.0 * (MAX_MEMBERS - n) / (MAX_MEMBERS - 5);
        }
        int min = (int) Math.round(avg - halfWidth);
        int max = (int) Math.round(avg + halfWidth);
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        while (n * min > 100 && min > 0) {
            min--;
        }
        while (n * max < 100 && max < 100) {
            max++;
        }
        if (min < 0) {
            min = 0;
        }
        if (max > 100) {
            max = 100;
        }
        if (min > max) {
            int mid = (int) Math.round(avg);
            mid = Math.max(0, Math.min(100, mid));
            min = max = mid;
        }
        return new int[]{min, max};
    }
}
