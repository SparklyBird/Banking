import org.junit.Before;
import org.junit.Test;
import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import static org.junit.Assert.*;
// Cool thing that creates a simulated version of a class, object, or method,
// and it mimics the behavior of real objects, but allows you to control its behavior during testing
import static org.mockito.Mockito.*;

public class BankingAppTest {

    private BankingApp bankingApp;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @Before
    public void setUp() {
        bankingApp = new BankingApp();

        // Mock the database connection
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        // Set the mocked connection to the BankingApp
        bankingApp.conn = mockConnection;
    }

    @Test
    public void testRegisterSuccess() throws SQLException {
        String username = "TestUser";
        String password = "TestPassword";

        // Mock the database interaction for registration
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        bankingApp.usernameField = new JTextField(username);
        bankingApp.passwordField = new JPasswordField(password);
        bankingApp.register();

        // Verify that the registration SQL was executed
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    public void testRegisterFailure() throws SQLException {
        String username = "TestUser";
        String password = "TestPassword";

        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Registration failed"));
        bankingApp.usernameField = new JTextField(username);
        bankingApp.passwordField = new JPasswordField(password);
        bankingApp.register();

        // Verify that no update was made due to failure
        verify(mockPreparedStatement, times(0)).executeUpdate();
    }

    @Test
    public void testLoginSuccess() throws SQLException {
        String username = "TestUser";
        String password = "TestPassword";

        // Mock the database interaction for login
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getDouble("balance")).thenReturn(1000.0);
        bankingApp.usernameField = new JTextField(username);
        bankingApp.passwordField = new JPasswordField(password);
        bankingApp.login();

        // Verify that the login SQL was executed
        verify(mockPreparedStatement, times(1)).executeQuery();

        // Check if the user is successfully logged in
        assertEquals("TestUser", bankingApp.loggedInUser);
    }

    @Test
    public void testLoginFailure() throws SQLException {
        String username = "TestUser";
        String password = "WrongPassword";

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // No user found
        bankingApp.usernameField = new JTextField(username);
        bankingApp.passwordField = new JPasswordField(password);
        bankingApp.login();

        // Verify that the login SQL was executed
        verify(mockPreparedStatement, times(1)).executeQuery();

        // Check that the login failed (no user logged in)
        assertNull(bankingApp.loggedInUser);
    }

    @Test
    public void testDepositSuccess() throws SQLException {
        String username = "TestUser";
        double initialBalance = 1000.0;
        double depositAmount = 500.0;

        // Mock the database interaction for updating balance
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        bankingApp.loggedInUser = username;
        bankingApp.balanceField = new JTextField(String.valueOf(initialBalance));
        bankingApp.amountField = new JTextField(String.valueOf(depositAmount));
        bankingApp.deposit();

        // Verify that the balance was updated in the database
        verify(mockPreparedStatement, times(1)).executeUpdate();

        // Check that the new balance is updated correctly
        assertEquals("1,500.00", bankingApp.balanceField.getText());
    }

    @Test
    public void testWithdrawSuccess() throws SQLException {
        String username = "TestUser";
        double initialBalance = 1000.0;
        double withdrawAmount = 500.0;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        bankingApp.loggedInUser = username;
        bankingApp.balanceField = new JTextField(String.valueOf(initialBalance));
        bankingApp.amountField = new JTextField(String.valueOf(withdrawAmount));
        bankingApp.withdraw();

        // Verify that the balance was updated in the database
        verify(mockPreparedStatement, times(1)).executeUpdate();

        // Check that the new balance is updated correctly
        assertEquals("500.00", bankingApp.balanceField.getText());
    }

    @Test
    public void testWithdrawInsufficientFunds() {
        String username = "TestUser";
        double initialBalance = 100.0;
        double withdrawAmount = 500.0;

        bankingApp.loggedInUser = username;
        bankingApp.balanceField = new JTextField(String.valueOf(initialBalance));
        bankingApp.amountField = new JTextField(String.valueOf(withdrawAmount));
        bankingApp.withdraw();

        // Ensure that withdrawal did not occur due to insufficient funds
        assertEquals("100.00", bankingApp.balanceField.getText());
    }
}
