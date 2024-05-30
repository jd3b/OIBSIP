import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class ATM {
    private User user;

    public ATM(User user) {
        this.user = user;
    }

    public void showMenu() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("ATM Menu:");
            System.out.println("1. Transaction History");
            System.out.println("2. Withdraw");
            System.out.println("3. Deposit");
            System.out.println("4. Transfer");
            System.out.println("5. Quit");
            System.out.print("Choose an option: ");
            int choice = sc.nextInt();


            switch (choice) {
                case 1:
                    showTransactionHistory();
                    break;
                case 2:
                    withdraw();
                    break;
                case 3:
                    deposit();
                    break;
                case 4:
                    transfer();
                    break;
                case 5:
                    System.out.println("Thank you for using the ATM");
                    return;
                default:
                    System.out.println("Invalid choice");
            }
        }
    }

    private void showTransactionHistory() {
        List<Transaction> transactions = user.getAccount().getTransactions();
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            for (Transaction t : transactions) {
                System.out.println(t);
            }
        }
    }

    private void withdraw() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter amount to withdraw: ");
        double amount = sc.nextDouble();

        if (user.getAccount().withdraw(amount)) {
            System.out.println("Withdrawal successful.");
        } else {
            System.out.println("Insufficient balance.");
        }
    }

    private void deposit() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter amount to deposit: ");
        double amount = sc.nextDouble();

        user.getAccount().deposit(amount);
        System.out.println("Deposit successful.");
    }

    private void transfer() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter recipient user ID: ");
        String recipientId = sc.next();
        System.out.print("Enter amount to transfer: ");
        double amount = sc.nextDouble();

        if (user.getAccount().transfer(amount, recipientId)) {
            System.out.println("Transfer successful.");
        } else {
            System.out.println("Transfer failed. Insufficient balance or invalid recipient.");
        }
    }
}

class User {
    private String userId;
    private String pin;
    private Account account;

    public User(String userId, String pin, double initialBalance) {
        this.userId = userId;
        this.pin = pin;
        this.account = new Account(initialBalance, userId);
    }

    public String getUserId() {
        return userId;
    }

    public String getPin() {
        return pin;
    }

    public Account getAccount() {
        return account;
    }
}

class Transaction {
    private String type;
    private double amount;
    private double balanceAfter;
    private String recipientId;

    public Transaction(String type, double amount, double balanceAfter) {
        this(type, amount, balanceAfter, null);
    }

    public Transaction(String type, double amount, double balanceAfter, String recipientId) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.recipientId = recipientId;
    }

    @Override
    public String toString() {
        if (recipientId != null) {
            return type + ": $" + amount + ", Balance: $" + balanceAfter + ", Recipient: " + recipientId;
        } else {
            return type + ": $" + amount + ", Balance: $" + balanceAfter;
        }
    }
}

class Account {
    private double balance;
    private List<Transaction> transactions;
    private String userId;

    public Account(double initialBalance, String userId) {
        this.balance = initialBalance;
        this.transactions = new ArrayList<>();
        this.userId = userId;
    }

    public double getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public boolean withdraw(double amount) {
        if (amount <= balance) {
            balance -= amount;
            transactions.add(new Transaction("Withdraw", amount, balance));
            updateDatabase("Withdraw", amount, balance, null);
            return true;
        }
        return false;
    }

    public void deposit(double amount) {
        balance += amount;
        transactions.add(new Transaction("Deposit", amount, balance));
        updateDatabase("Deposit", amount, balance, null);
    }

    public boolean transfer(double amount, String recipientId) {
        if (amount <= balance) {
            balance -= amount;
            transactions.add(new Transaction("Transfer", amount, balance, recipientId));
            updateDatabase("Transfer", amount, balance, recipientId);
            return true;
        }
        return false;
    }

    private void updateDatabase(String type, double amount, double balanceAfter, String recipientId) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/atm_system?autoReconnect=true&useSSL=false", "root", "Go4goljh@")) {
            String sql = "INSERT INTO transactions (user_id, type, amount, balance_after, recipient_id) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setString(2, type);
            pstmt.setDouble(3, amount);
            pstmt.setDouble(4, balanceAfter);
            pstmt.setString(5, recipientId);
            pstmt.executeUpdate();

            sql = "UPDATE users SET balance = ? WHERE user_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, balanceAfter);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

public class ATMSystem {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter user ID: ");
        String userId = sc.nextLine();
        System.out.print("Enter PIN: ");
        String pin = sc.nextLine();

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/atm_system?autoReconnect=true&useSSL=false", "root", "Go4goljh@")) {
            String sql = "SELECT * FROM users WHERE user_id = ? AND pin = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            pstmt.setString(2, pin);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                User user = new User(userId, pin, balance);
                ATM atm = new ATM(user);
                atm.showMenu();
            } else {
                System.out.println("Invalid user ID or PIN.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

