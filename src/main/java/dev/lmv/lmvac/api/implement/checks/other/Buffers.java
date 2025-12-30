package dev.lmv.lmvac.api.implement.checks.other;

public class Buffers {
    public final boolean enabled;
    public final int threshold;

    public Buffers(Boolean enabled, Integer threshold) {
        this.enabled = enabled;
        this.threshold = threshold;
    }
}
