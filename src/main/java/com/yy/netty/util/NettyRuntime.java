package com.yy.netty.util;

import com.yy.netty.util.internal.ObjectUtil;
import com.yy.netty.util.internal.SystemPropertyUtil;

import java.util.Locale;

public class NettyRuntime {

    private NettyRuntime() {

    }

    static class AvailableProcessorsHolder {

        private int availableProcessors;

        /**
         * Set the number of available processors.
         *
         * @param availableProcessors the number of available processors
         * @throws IllegalArgumentException if the specified number of available processors is non-positive
         * @throws IllegalStateException    if the number of available processors is already configured
         */
        synchronized void setAvailableProcessors(final int availableProcessors) {
            ObjectUtil.checkPositive(availableProcessors, "availableProcessors");
            if (this.availableProcessors != 0) {
                final String message = String.format(
                        Locale.ROOT,
                        "availableProcessors is already set to [%d], rejecting [%d]",
                        this.availableProcessors,
                        availableProcessors
                );
                throw new IllegalStateException(message);
            }
            this.availableProcessors = availableProcessors;
        }

        /**
         * Get the configured number of available processors. The default is {@link Runtime#availableProcessors()}.
         * This can be overridden by setting the system property "io.netty.availableProcessors" or by invoking
         * {@link #setAvailableProcessors(int)} before any calls to this method.
         *
         * @return the configured number of available processors
         */
        //@SuppressForbidden(reason = "to obtain default number of available processors")
        synchronized int availableProcessors() {
            if (availableProcessors == 0) {
                final int availableProcessors = SystemPropertyUtil.getInt(
                        "io.netty.availableProcessors",
                        Runtime.getRuntime().availableProcessors()
                );
                setAvailableProcessors(availableProcessors);
            }
            return availableProcessors;
        }

    }

    private static final AvailableProcessorsHolder holder = new AvailableProcessorsHolder();


    /**
     * Get the configured number of available processors. The default is {@link Runtime#availableProcessors()}. This
     * can be overridden by setting the system property "io.netty.availableProcessors" or by invoking
     * {@link #setAvailableProcessors(int)} before any calls to this method.
     *
     * @return the configured number of available processors
     */
    public static int availableProcessors() {
        return holder.availableProcessors();
    }

    /**
     * Set the number of available processors.
     *
     * @param availableProcessors the number of available processors
     * @throws IllegalArgumentException if the specified number of available processors is non-positive
     * @throws IllegalStateException    if the number of available processors is already configured
     */
    @SuppressWarnings("unused,WeakerAccess")
    public static void setAvailableProcessors(final int availableProcessors) {
        holder.setAvailableProcessors(availableProcessors);
    }

}
