package com.template.browse;

import com.template.model.Listing;
import com.template.model.ListingGroup;

/** How the screens ask the main window to change screens. MainController implements this. */
public interface Navigator {

    /** Show the full details page of one listing (Step 3). */
    void openListing(Listing listing);

    /** Show every listing of one row, e.g. all "Dormitories in Lapasan". */
    void showSeeAll(ListingGroup group);

    /** Back to the home screen. */
    void goHome();

    /** Back to the screen the user came from (home, "see all", or another listing). */
    void goBack();

    /** "Click to message host" on the details page. Member D builds the chat screen. */
    void openChat(Listing listing);

    /** "Schedule a viewing" on the details page. Member D builds the schedule screen. */
    void openScheduleViewing(Listing listing);

    /** Shown when an unverified user tries to message or schedule. Member A builds the ID upload screen. */
    void openIdVerification();
}
