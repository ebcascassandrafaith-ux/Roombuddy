package com.template;

/** Switches that are safe to flip while developing. */
public final class AppConfig {

    private AppConfig() {}

    /**
     * true  = screens use built-in sample listings (no database needed).
     * false = real data from the `roombuddy` database through DBConnection.
     */
    public static final boolean DEMO_MODE = true;
}