import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class BankingApp extends JFrame {
    Connection conn;
    JTextField usernameField;
    JTextField passwordField;
    JTextField balanceField;
    JTextField amountField;
    private JTextField recipientField;
    private JLabel statusLabel;
    String loggedInUser;
    private final DecimalFormat decimalFormat;

    public BankingApp() {
        decimalFormat = new DecimalFormat("#,###.00");
        setupDatabase();
        createLoginUI();
    }


    private void setupDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:bankingapp.db");
            Statement stmt = conn.createStatement();
            String createTableSQL = "CREATE TABLE IF NOT EXISTS accounts (username TEXT PRIMARY KEY, password TEXT, balance REAL)";
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            showMessage("Database setup error: " + e.getMessage());
        }
    }

    private void createLoginUI() {
        setTitle("Banking App - Login or Register");
        setSize(300, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(8, 2));

        panel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> login());
        panel.add(loginButton);

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(e -> register());
        panel.add(registerButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        panel.add(exitButton);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        panel.add(statusLabel);

        add(panel);
        setVisible(true);
    }

    void login() {
        String username = usernameField.getText();
        String password = new String(((JPasswordField) passwordField).getPassword());

        try {
            String query = "SELECT balance FROM accounts WHERE username=? AND password=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                loggedInUser = username;
                double balance = rs.getDouble("balance");
                showMessage("Login successful!");
                createBankingUI(balance);
            } else {
                showMessage("Invalid login credentials.");
            }
        } catch (SQLException e) {
            showMessage("Login error: " + e.getMessage());
        }
    }

    void register() {
        String username = usernameField.getText();
        String password = new String(((JPasswordField) passwordField).getPassword());

        try {
            String insertSQL = "INSERT INTO accounts (username, password, balance) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setDouble(3, 0.0);
            pstmt.executeUpdate();

            showMessage("Registration successful! Please login.");
        } catch (SQLException e) {
            showMessage("Registration error: " + e.getMessage());
        }
    }

    private void createBankingUI(double initialBalance) {
        getContentPane().removeAll();
        setTitle("Banking App - Welcome, " + loggedInUser);

        JPanel panel = new JPanel(new GridLayout(11, 2));

        panel.add(new JLabel("Balance:"));
        balanceField = new JTextField(decimalFormat.format(initialBalance));
        balanceField.setEditable(false);
        panel.add(balanceField);

        panel.add(new JLabel("Amount:"));
        amountField = new JTextField();
        panel.add(amountField);

        panel.add(new JLabel("Recipient:"));
        recipientField = new JTextField();
        panel.add(recipientField);

        JButton depositButton = new JButton("Deposit");
        depositButton.addActionListener(e -> deposit());
        panel.add(depositButton);

        JButton withdrawButton = new JButton("Withdraw");
        withdrawButton.addActionListener(e -> withdraw());
        panel.add(withdrawButton);

        JButton sendMoneyButton = new JButton("Send Money");
        sendMoneyButton.addActionListener(e -> sendMoney());
        panel.add(sendMoneyButton);

        // Add "View Accounts" button only for user "SparklyBird"
        if ("SparklyBird".equals(loggedInUser)) {
            JButton viewAccountsButton = new JButton("View Accounts");
            viewAccountsButton.addActionListener(e -> viewAccounts());
            panel.add(viewAccountsButton);
        }

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            getContentPane().removeAll();
            createLoginUI();
        });
        panel.add(logoutButton);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> System.exit(0));
        panel.add(exitButton);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        panel.add(statusLabel);

        add(panel);
        revalidate();
        repaint();
    }

    void deposit() {
        try {
            double amount = parseAmount(amountField.getText());
            if (amount > 0) {
                updateBalance(amount);
                showMessage("Deposit successful!");
            } else {
                showMessage("Invalid deposit amount.");
            }
        } catch (NumberFormatException e) {
            showMessage("Error parsing deposit amount: " + e.getMessage());
        }
    }

    void withdraw() {
        try {
            double amount = parseAmount(amountField.getText());
            double currentBalance = parseAmount(balanceField.getText());

            if (amount > 0 && currentBalance >= amount) {
                updateBalance(-amount);
                showMessage("Withdrawal successful!");
            } else {
                // Reformat the balance to ensure it's consistent with the expected format
                balanceField.setText(decimalFormat.format(currentBalance));
                showMessage("Invalid withdrawal amount or insufficient funds.");
            }
        } catch (NumberFormatException e) {
            showMessage("Error parsing withdrawal amount: " + e.getMessage());
        }
    }


    private void sendMoney() {
        try {
            double amount = parseAmount(amountField.getText());
            String recipient = recipientField.getText().trim();
            double currentBalance = parseAmount(balanceField.getText());

            if (amount > 0 && currentBalance >= amount && !recipient.isEmpty()) {
                // Check if recipient exists
                String query = "SELECT balance FROM accounts WHERE username=?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1, recipient);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    // Update balances
                    double recipientBalance = rs.getDouble("balance");

                    updateBalance(-amount);
                    updateRecipientBalance(recipient, recipientBalance + amount);

                    showMessage("Money sent successfully!");
                } else {
                    showMessage("Recipient does not exist.");
                }
            } else {
                showMessage("Invalid amount, insufficient funds, or recipient not specified.");
            }
        } catch (NumberFormatException e) {
            showMessage("Error parsing amount: " + e.getMessage());
        } catch (SQLException e) {
            showMessage("Error sending money: " + e.getMessage());
        }
    }

    private void updateBalance(double amount) {
        try {
            double newBalance = parseAmount(balanceField.getText()) + amount;
            balanceField.setText(decimalFormat.format(newBalance));

            String updateSQL = "UPDATE accounts SET balance=? WHERE username=?";
            PreparedStatement pstmt = conn.prepareStatement(updateSQL);
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, loggedInUser);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showMessage("Error updating balance: " + e.getMessage());
        }
    }


    private void updateRecipientBalance(String recipient, double newBalance) {
        try {
            String updateSQL = "UPDATE accounts SET balance=? WHERE username=?";
            PreparedStatement pstmt = conn.prepareStatement(updateSQL);
            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, recipient);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            showMessage("Error updating recipient balance: " + e.getMessage());
        }
    }

    private void viewAccounts() {
        try {
            String query = "SELECT username, balance FROM accounts";
            PreparedStatement pstmt = conn.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            List<Account> accounts = new ArrayList<>();
            while (rs.next()) {
                String username = rs.getString("username");
                double balance = rs.getDouble("balance");
                accounts.add(new Account(username, balance));
            }

            // Sort accounts by balance from highest to lowest
            accounts.sort((a1, a2) -> Double.compare(a2.getBalance(), a1.getBalance()));

            StringBuilder sb = new StringBuilder();
            sb.append("Ranking:\n");
            int rank = 1;
            for (Account account : accounts) {
                sb.append(rank++)
                        .append(". ")
                        .append(account.getUsername())
                        .append(": ")
                        .append(decimalFormat.format(account.getBalance()))
                        .append("\n");
            }

            JOptionPane.showMessageDialog(this, sb.toString(), "Accounts and Balances", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            showMessage("Error retrieving accounts: " + e.getMessage());
        }
    }

    private double parseAmount(String text) throws NumberFormatException {
        // Remove any commas and parse the number
        return Double.parseDouble(text.replace(",", ""));
    }

    private void showMessage(String message) {
        statusLabel.setText(message);
    }

    private static class Account {
        private final String username;
        private final double balance;

        public Account(String username, double balance) {
            this.username = username;
            this.balance = balance;
        }

        public String getUsername() {
            return username;
        }

        public double getBalance() {
            return balance;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BankingApp::new);
    }
}
