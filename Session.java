package com.template.util;

import com.template.User;

// Remembers who is currently logged in.
// "static" means there is only one shared copy for the whole app.
public final class Session
{
    private Session() {}

    private static User currentUser;
    private static boolean idVerified = false;

    // Call this after a successful login
    public static void login(User user, boolean verified)
    {
        currentUser = user;
        idVerified = verified;
    }

    public static void logout()
    {
        currentUser = null;
        idVerified = false;
    }

    public static User getCurrentUser()
    {
        return currentUser;
    }

    // 0 = nobody is logged in
    public static int getUserId()
    {
        return currentUser == null ? 0 : currentUser.getUserId();
    }

    public static int getCurrentUserId()
    {
        return getUserId();
    }

    public static boolean isLoggedIn()
    {
        return currentUser != null;
    }

    public static boolean isIdVerified()
    {
        return idVerified;
    }

    // Call this right after the ID upload is approved
    public static void setIdVerified(boolean verified)
    {
        idVerified = verified;
    }
}