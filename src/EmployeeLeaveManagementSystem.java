import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
public class EmployeeLeaveManagementSystem {

    static final String URL = "jdbc:mysql://localhost:3306/testdb";
    static final String USERNAME = "root";
    static final String PASSWORD = "root";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try (Connection con = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {

            System.out.println("======================================");
            System.out.println(" Employee Leave Management System");
            System.out.println("======================================");

            while (true) {

                System.out.println("\n1. Register Employee");
                System.out.println("2. Apply Leave");
                System.out.println("3. Approve Leave");
                System.out.println("4. Reject Leave");
                System.out.println("5. View Leave Balance");
                System.out.println("6. View Leave Requests");
                System.out.println("7. View Employees");
                System.out.println("8. Exit");

                System.out.print("Enter your choice: ");
                int choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {

                    case 1:
                        registerEmployee(con);
                        break;

                    case 2:
                        applyLeave(con);
                        break;

                    case 3:
                        updateLeaveStatus(con, "APPROVED");
                        break;

                    case 4:
                        updateLeaveStatus(con, "REJECTED");
                        break;

                    case 5:
                        viewLeaveBalance(con);
                        break;

                    case 6:
                        viewLeaveRequests(con);
                        break;

                    case 7:
                        viewEmployees(con);
                        break;

                    case 8:
                        System.out.println("Thank you!");
                        return;

                    default:
                        System.out.println("Invalid choice!");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 1. Register Employee
    static void registerEmployee(Connection con) {

        String sql = "INSERT INTO employees "
                   + "(employee_name, email, department, joining_date) "
                   + "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql,
                java.sql.Statement.RETURN_GENERATED_KEYS)) {

            System.out.print("Enter employee name: ");
            String name = sc.nextLine();

            System.out.print("Enter email: ");
            String email = sc.nextLine();

            System.out.print("Enter department: ");
            String department = sc.nextLine();

            System.out.print("Enter joining date (YYYY-MM-DD): ");
            LocalDate joiningDate = LocalDate.parse(sc.nextLine());

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, department);
            ps.setDate(4, java.sql.Date.valueOf(joiningDate));

            int rows = ps.executeUpdate();

            if (rows > 0) {

                ResultSet rs = ps.getGeneratedKeys();

                if (rs.next()) {

                    int employeeId = rs.getInt(1);

                    String balanceSql =
                            "INSERT INTO leave_balance "
                            + "(employee_id, total_leaves, used_leaves, remaining_leaves) "
                            + "VALUES (?, 20, 0, 20)";

                    try (PreparedStatement balancePs =
                                 con.prepareStatement(balanceSql)) {

                        balancePs.setInt(1, employeeId);
                        balancePs.executeUpdate();
                    }

                    System.out.println("Employee registered successfully!");
                    System.out.println("Employee ID: " + employeeId);
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 2. Apply Leave
    static void applyLeave(Connection con) {

        String sql = "INSERT INTO leave_requests "
                   + "(employee_id, start_date, end_date, leave_days, reason, status) "
                   + "VALUES (?, ?, ?, ?, ?, 'PENDING')";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            System.out.print("Enter employee ID: ");
            int employeeId = sc.nextInt();
            sc.nextLine();

            System.out.print("Enter start date (YYYY-MM-DD): ");
            LocalDate startDate = LocalDate.parse(sc.nextLine());

            System.out.print("Enter end date (YYYY-MM-DD): ");
            LocalDate endDate = LocalDate.parse(sc.nextLine());

            if (endDate.isBefore(startDate)) {
                System.out.println("End date cannot be before start date.");
                return;
            }

            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

            System.out.print("Enter reason: ");
            String reason = sc.nextLine();

            ps.setInt(1, employeeId);
            ps.setDate(2, java.sql.Date.valueOf(startDate));
            ps.setDate(3, java.sql.Date.valueOf(endDate));
            ps.setInt(4, (int) days);
            ps.setString(5, reason);

            ps.executeUpdate();

            System.out.println("Leave applied successfully!");
            System.out.println("Leave days: " + days);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 3 & 4. Approve / Reject Leave
    static void updateLeaveStatus(Connection con, String newStatus) {

        System.out.print("Enter leave ID: ");
        int leaveId = sc.nextInt();
        sc.nextLine();

        try {

            // Start transaction
            con.setAutoCommit(false);

            String checkSql =
                    "SELECT employee_id, leave_days, status "
                    + "FROM leave_requests WHERE leave_id = ?";

            int employeeId;
            int leaveDays;
            String currentStatus;

            try (PreparedStatement ps = con.prepareStatement(checkSql)) {

                ps.setInt(1, leaveId);

                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    System.out.println("Leave request not found.");
                    con.rollback();
                    return;
                }

                employeeId = rs.getInt("employee_id");
                leaveDays = rs.getInt("leave_days");
                currentStatus = rs.getString("status");
            }

            if (!currentStatus.equals("PENDING")) {
                System.out.println("This leave is already " + currentStatus);
                con.rollback();
                return;
            }

            // If approving, check leave balance
            if (newStatus.equals("APPROVED")) {

                String balanceSql =
                        "SELECT remaining_leaves FROM leave_balance "
                        + "WHERE employee_id = ?";

                int remainingLeaves;

                try (PreparedStatement ps =
                             con.prepareStatement(balanceSql)) {

                    ps.setInt(1, employeeId);

                    ResultSet rs = ps.executeQuery();

                    if (!rs.next()) {
                        System.out.println("Leave balance not found.");
                        con.rollback();
                        return;
                    }

                    remainingLeaves =
                            rs.getInt("remaining_leaves");
                }

                if (remainingLeaves < leaveDays) {
                    System.out.println("Insufficient leave balance.");
                    con.rollback();
                    return;
                }

                // Update leave balance
                String updateBalance =
                        "UPDATE leave_balance "
                        + "SET used_leaves = used_leaves + ?, "
                        + "remaining_leaves = remaining_leaves - ? "
                        + "WHERE employee_id = ?";

                try (PreparedStatement ps =
                             con.prepareStatement(updateBalance)) {

                    ps.setInt(1, leaveDays);
                    ps.setInt(2, leaveDays);
                    ps.setInt(3, employeeId);

                    ps.executeUpdate();
                }
            }

            // Update leave request
            String updateLeave =
                    "UPDATE leave_requests "
                    + "SET status = ? WHERE leave_id = ?";

            try (PreparedStatement ps =
                         con.prepareStatement(updateLeave)) {

                ps.setString(1, newStatus);
                ps.setInt(2, leaveId);

                ps.executeUpdate();
            }

            // Commit transaction
            con.commit();

            System.out.println(
                    "Leave request " + newStatus + " successfully!");

        } catch (Exception e) {

            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            System.out.println(
                    "Transaction failed. Changes rolled back.");
            System.out.println("Error: " + e.getMessage());

        } finally {

            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 5. View Leave Balance
    static void viewLeaveBalance(Connection con) {

        String sql =
                "SELECT e.employee_id, e.employee_name, "
                + "lb.total_leaves, lb.used_leaves, "
                + "lb.remaining_leaves "
                + "FROM employees e "
                + "JOIN leave_balance lb "
                + "ON e.employee_id = lb.employee_id "
                + "WHERE e.employee_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            System.out.print("Enter employee ID: ");
            int employeeId = sc.nextInt();
            sc.nextLine();

            ps.setInt(1, employeeId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                System.out.println("\nEmployee ID: "
                        + rs.getInt("employee_id"));

                System.out.println("Employee Name: "
                        + rs.getString("employee_name"));

                System.out.println("Total Leaves: "
                        + rs.getInt("total_leaves"));

                System.out.println("Used Leaves: "
                        + rs.getInt("used_leaves"));

                System.out.println("Remaining Leaves: "
                        + rs.getInt("remaining_leaves"));

            } else {
                System.out.println("Employee not found.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 6. View Leave Requests
    static void viewLeaveRequests(Connection con) {

        String sql =
                "SELECT lr.leave_id, e.employee_name, "
                + "lr.start_date, lr.end_date, "
                + "lr.leave_days, lr.reason, lr.status "
                + "FROM leave_requests lr "
                + "JOIN employees e "
                + "ON lr.employee_id = e.employee_id "
                + "ORDER BY lr.leave_id";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== Leave Requests ==========");

            while (rs.next()) {

                System.out.println(
                        "Leave ID: " + rs.getInt("leave_id"));

                System.out.println(
                        "Employee: " + rs.getString("employee_name"));

                System.out.println(
                        "Start Date: " + rs.getDate("start_date"));

                System.out.println(
                        "End Date: " + rs.getDate("end_date"));

                System.out.println(
                        "Leave Days: " + rs.getInt("leave_days"));

                System.out.println(
                        "Reason: " + rs.getString("reason"));

                System.out.println(
                        "Status: " + rs.getString("status"));

                System.out.println("-----------------------------------");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 7. View Employees
    static void viewEmployees(Connection con) {

        String sql = "SELECT * FROM employees";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n========== Employees ==========");

            while (rs.next()) {

                System.out.println(
                        "ID: " + rs.getInt("employee_id"));

                System.out.println(
                        "Name: " + rs.getString("employee_name"));

                System.out.println(
                        "Email: " + rs.getString("email"));

                System.out.println(
                        "Department: " + rs.getString("department"));

                System.out.println(
                        "Joining Date: " + rs.getDate("joining_date"));

                System.out.println("-----------------------------------");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

