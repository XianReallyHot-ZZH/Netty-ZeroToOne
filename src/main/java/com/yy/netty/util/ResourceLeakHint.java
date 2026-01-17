package com.yy.netty.util;

public interface ResourceLeakHint {

    /**
     * Returns a human-readable message that potentially enables easier resource leak tracking.
     */
    String toHintString();

}
