import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;

public class App {
    private static final Scanner scanner = new Scanner(System.in);
    private static final File dataFile = new File("LoginData.txt");

    public static void main(String[] args) {
        // Ensure the data file exists immediately
        ensureFileExists();

        boolean isProgramRunning = true;
        boolean isLoggedIn = false;

        while (isProgramRunning) {
            if (!isLoggedIn) {
                System.out.println("\n--- Welcome to Login Credential Saver ---");
                System.out.println("Type: \"Signup\", \"Login\", or \"Exit\"");
                System.out.print("> ");
                String input = scanner.nextLine().trim().toLowerCase();

                switch (input) {
                    case "signup":
                        Signup sup = new Signup();
                        sup.handleSignup(scanner, dataFile);
                        break;

                    case "login":
                        Login log = new Login();
                        isLoggedIn = log.handleLogin(scanner, dataFile);
                        break;

                    case "exit":
                        System.out.println("Goodbye!");
                        isProgramRunning = false;
                        break;

                    default:
                        System.out.println("Invalid input. Please try again.");
                }
            } else {
                System.out.println("\n[Dashboard] Welcome back! You are authorized.");
                System.out.println("Type \"Logout\" to return to menu or \"Exit\" to quit.");
                System.out.print("> ");
                String choice = scanner.nextLine().toLowerCase();

                if (choice.equals("logout")) {
                    isLoggedIn = false;
                } else if (choice.equals("exit")) {
                    isProgramRunning = false;
                }
            }
        }
    }

    private static void ensureFileExists() {
        try {
            if (dataFile.createNewFile()) {
                System.out.println("System initialized: New database created.");
            }
        } catch (Exception e) {
            System.out.println("Fatal Error: Could not initialize data file.");
        }
    }
}

class Login {
    public boolean handleLogin(Scanner scanner, File dataFile) {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        // Try-with-resources ensures the file is closed even if an error occurs
        try (Scanner fileScanner = new Scanner(dataFile)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] parts = line.split(",");

                if (parts.length == 3) {
                    String storedUser = parts[1];
                    String storedPass = parts[2];

                    if (storedUser.equals(username) && storedPass.equals(password)) {
                        System.out.println("Login successful!");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading login data: " + e.getMessage());
        }

        System.out.println("Invalid username or password.");
        return false;
    }
}

class Signup {
    public void handleSignup(Scanner scanner, File dataFile) {
        System.out.println("--- Signup Page ---");
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        try (FileWriter writer = new FileWriter(dataFile, true)) {
            writer.write(name + "," + username + "," + password + "\n");
            System.out.println("Account created successfully!");
        } catch (Exception e) {
            System.out.println("Error saving user: " + e.getMessage());
        }
    }
}
