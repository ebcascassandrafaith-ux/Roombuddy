
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Scanner;

public class ListingSystem {

    static Scanner input = new Scanner(System.in);
    static ArrayList<Listing> listings = new ArrayList<>();
    static int nextId = 1;

    static class Listing {
        int id;
        int ownerId;
        String propertyName;
        String propertyType;
        String roomType;
        String barangay;
        String address;
        BigDecimal price;
        String description;
        String houseRules;
        String hostName;
        String status;
        boolean occupied;
        ArrayList<String> amenities = new ArrayList<>();

        Listing(int id, int ownerId, String propertyName,
                String propertyType, String roomType,
                String barangay, String address,
                BigDecimal price, String description,
                String houseRules, String hostName) {

            this.id = id;
            this.ownerId = ownerId;
            this.propertyName = propertyName;
            this.propertyType = propertyType;
            this.roomType = roomType;
            this.barangay = barangay;
            this.address = address;
            this.price = price;
            this.description = description;
            this.houseRules = houseRules;
            this.hostName = hostName;
            this.status = "pending";
            this.occupied = false;
        }
    }

    public static void main(String[] args) {

        int choice;

        do {
            System.out.println("\n================================");
            System.out.println("       ROOM BUDDY LISTINGS");
            System.out.println("================================");
            System.out.println("1. Add Listing");
            System.out.println("2. View All Listings");
            System.out.println("3. Search Listing");
            System.out.println("4. View Listing Details");
            System.out.println("5. My Listings");
            System.out.println("6. Update Availability");
            System.out.println("7. Delete Listing");
            System.out.println("8. Explore More Listings");
            System.out.println("0. Exit");
            System.out.println("================================");
            System.out.print("Enter choice: ");

            choice = readInt();

            switch (choice) {
                case 1:
                    addListing();
                    break;
                case 2:
                    viewAllListings();
                    break;
                case 3:
                    searchListing();
                    break;
                case 4:
                    viewListingDetails();
                    break;
                case 5:
                    myListings();
                    break;
                case 6:
                    updateAvailability();
                    break;
                case 7:
                    deleteListing();
                    break;
                case 8:
                    exploreMore();
                    break;
                case 0:
                    System.out.println("Thank you for using RoomBuddy!");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 0);
    }

    static void addListing() {

        System.out.println("\n--- ADD NEW LISTING ---");

        System.out.print("Owner ID: ");
        int ownerId = readInt();

        System.out.print("Host Name: ");
        String hostName = input.nextLine();

        System.out.print("Property Name: ");
        String propertyName = input.nextLine();

        System.out.print("Property Type: ");
        String propertyType = input.nextLine();

        System.out.print("Room Type: ");
        String roomType = input.nextLine();

        System.out.print("Barangay: ");
        String barangay = input.nextLine();

        System.out.print("Complete Address: ");
        String address = input.nextLine();

        System.out.print("Monthly Price: ");
        BigDecimal price = readPrice();

        System.out.print("Description: ");
        String description = input.nextLine();

        System.out.print("House Rules: ");
        String houseRules = input.nextLine();

        Listing listing = new Listing(
                nextId,
                ownerId,
                propertyName,
                propertyType,
                roomType,
                barangay,
                address,
                price,
                description,
                houseRules,
                hostName
        );

        System.out.print("How many amenities? ");
        int count = readInt();

        for (int i = 0; i < count; i++) {
            System.out.print("Enter amenity " + (i + 1) + ": ");
            listing.amenities.add(input.nextLine());
        }

        listings.add(listing);

        System.out.println("\nListing successfully added!");
        System.out.println("Listing ID: " + nextId);

        nextId++;
    }

    static void viewAllListings() {

        System.out.println("\n--- ALL LISTINGS ---");

        if (listings.isEmpty()) {
            System.out.println("No listings available.");
            return;
        }

        for (Listing listing : listings) {
            displayListing(listing);
        }
    }

    static void displayListing(Listing listing) {

        System.out.println("\n--------------------------------");
        System.out.println("Listing ID: " + listing.id);
        System.out.println("Property Name: " + listing.propertyName);
        System.out.println("Property Type: " + listing.propertyType);
        System.out.println("Room Type: " + listing.roomType);
        System.out.println("Location: " + listing.barangay);
        System.out.println("Monthly Price: PHP " + listing.price);
        System.out.println("Availability: "
                + (listing.occupied ? "OCCUPIED" : "AVAILABLE"));
        System.out.println("Status: " + listing.status);
        System.out.println("--------------------------------");
    }

    static void searchListing() {

        System.out.println("\n--- SEARCH LISTING ---");
        System.out.print("Enter Listing ID: ");

        int id = readInt();

        Listing listing = findListing(id);

        if (listing != null) {
            displayListing(listing);
        } else {
            System.out.println("Listing not found.");
        }
    }

    static void viewListingDetails() {

        System.out.println("\n--- LISTING DETAILS ---");
        System.out.print("Enter Listing ID: ");

        int id = readInt();

        Listing listing = findListing(id);

        if (listing == null) {
            System.out.println("Listing not found.");
            return;
        }

        System.out.println("\n================================");
        System.out.println("        PROPERTY DETAILS");
        System.out.println("================================");
        System.out.println("Listing ID: " + listing.id);
        System.out.println("Property Name: " + listing.propertyName);
        System.out.println("Host: " + listing.hostName);
        System.out.println("Property Type: " + listing.propertyType);
        System.out.println("Room Type: " + listing.roomType);
        System.out.println("Barangay: " + listing.barangay);
        System.out.println("Address: " + listing.address);
        System.out.println("Price: PHP " + listing.price);
        System.out.println("Availability: "
                + (listing.occupied ? "OCCUPIED" : "AVAILABLE"));
        System.out.println("Status: " + listing.status);
        System.out.println("Description: " + listing.description);
        System.out.println("House Rules: " + listing.houseRules);

        System.out.println("\nAmenities:");

        if (listing.amenities.isEmpty()) {
            System.out.println("No amenities listed.");
        } else {
            for (String amenity : listing.amenities) {
                System.out.println("- " + amenity);
            }
        }

        System.out.println("================================");
    }

    static void myListings() {

        System.out.println("\n--- MY LISTINGS ---");
        System.out.print("Enter your Owner ID: ");

        int ownerId = readInt();
        boolean found = false;

        for (Listing listing : listings) {
            if (listing.ownerId == ownerId) {
                displayListing(listing);
                found = true;
            }
        }

        if (!found) {
            System.out.println("You have not posted any listings.");
        }
    }

    static void updateAvailability() {

        System.out.println("\n--- UPDATE AVAILABILITY ---");
        System.out.print("Enter Listing ID: ");

        int id = readInt();

        Listing listing = findListing(id);

        if (listing == null) {
            System.out.println("Listing not found.");
            return;
        }

        listing.occupied = !listing.occupied;

        if (listing.occupied) {
            System.out.println("Listing marked as OCCUPIED.");
        } else {
            System.out.println("Listing marked as AVAILABLE.");
        }
    }

    static void deleteListing() {

        System.out.println("\n--- DELETE LISTING ---");
        System.out.print("Enter Listing ID: ");

        int id = readInt();

        Listing listing = findListing(id);

        if (listing == null) {
            System.out.println("Listing not found.");
            return;
        }

        System.out.print("Are you sure you want to delete this listing? (Y/N): ");
        String answer = input.nextLine();

        if (answer.equalsIgnoreCase("Y")) {
            listings.remove(listing);
            System.out.println("Listing successfully deleted.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    static void exploreMore() {

        System.out.println("\n--- EXPLORE MORE LISTINGS ---");

        boolean found = false;

        for (Listing listing : listings) {
            if (listing.status.equalsIgnoreCase("verified")
                    && !listing.occupied) {

                displayListing(listing);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No other available listings found.");
        }
    }

    static Listing findListing(int id) {

        for (Listing listing : listings) {
            if (listing.id == id) {
                return listing;
            }
        }

        return null;
    }

    static int readInt() {

        while (!input.hasNextInt()) {
            System.out.print("Please enter a valid number: ");
            input.nextLine();
        }

        int number = input.nextInt();
        input.nextLine();

        return number;
    }

    static BigDecimal readPrice() {

        while (true) {
            try {
                BigDecimal price = new BigDecimal(input.nextLine());

                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    return price;
                }

                System.out.print("Price must be greater than zero. Enter again: ");

            } catch (NumberFormatException e) {
                System.out.print("Invalid price. Enter again: ");
            }
        }
    }
}
