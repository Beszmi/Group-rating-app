package com.example;

import java.io.Serializable;

public final class SessionConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String[] names;
    private final int minRating;
    private final int maxRating;

    public SessionConfig(String[] names, int minRating, int maxRating) {
        this.names = names.clone();
        this.minRating = minRating;
        this.maxRating = maxRating;
    }

    public String[] getNames() {
        return names.clone();
    }

    public int getMemberCount() {
        return names.length;
    }

    public int getMinRating() {
        return minRating;
    }

    public int getMaxRating() {
        return maxRating;
    }
}
