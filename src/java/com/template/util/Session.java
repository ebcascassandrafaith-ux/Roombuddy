package com.template.util;

/**
 * Who is using the app right now.
 *
 * PLACEHOLDER: Member A (accounts) fills this in once login works (call Session.login(...) after a
 * successful login). Everyone else only calls the getters, so nothing else has to change.
 */
public final class Session {

    private Session() {}

    // Demo values so the details page can be tested before login exists.
    private static int userId = 2;            // 0 = nobody logged in
    private static boolean idVerified = true; // set to false to test the "Verify ID to continue" flow

    public static int getUserId() { return userId; }

    public static boolean isLoggedIn() { return userId > 0; }

    public static boolean isIdVerified() { return idVerified; }

    /** Member A calls this after a successful login. */
    public static void login(int newUserId, boolean newIdVerified) {
        userId = newUserId;
        idVerified = newIdVerified;
    }

    /** Member A calls this on logout. */
    public static void logout() {
        userId = 0;
        idVerified = false;
    }
}
