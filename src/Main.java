
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n==============================");
            System.out.println("       ROOMBUDDY LISTING");
            System.out.println("==============================");
            System.out.println("1. View Room Listings");
            System.out.println("2. Add Room Listing");
            System.out.println("3. Search Room Listing");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");

            choice = input.nextInt();
            input.nextLine();

            switch (choice) {
                case 1:
                    System.out.println("View Room Listings selected.");
                    break;

                case 2:
                    System.out.println("Add Room Listing selected.");
                    break;

                case 3:
                    System.out.println("Search Room Listing selected.");
                    break;

                case 4:
                    System.out.println("Thank you for using RoomBuddy!");
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
            }

        } while (choice != 4);

        input.close();
    }
}
