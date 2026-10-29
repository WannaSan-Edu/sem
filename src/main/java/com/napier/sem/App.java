package com.napier.sem;

import java.sql.*;
import java.util.ArrayList;

public class App
{
    private Connection con = null;

    // Connect to the MySQL employees database.
    public void connect(String location, int delay)
    {
        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver");
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Could not load SQL driver: " + e.getMessage());
            return;
        }

        int retries = 10;

        for (int i = 0; i < retries; i++)
        {
            try
            {
                if (delay > 0)
                    Thread.sleep(delay);

                con = DriverManager.getConnection(
                        "jdbc:mysql://" + location +
                                "/employees?allowPublicKeyRetrieval=true&useSSL=false",
                        "root",
                        "example"
                );

                System.out.println("Successfully connected to database.");
                return;
            }
            catch (SQLException e)
            {
                System.out.println("Database connection attempt " + (i + 1) +
                        " failed: " + e.getMessage());
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                System.out.println("Database connection interrupted.");
                return;
            }
        }

        System.out.println("Could not connect to database.");
    }

    // Close the database connection.
    public void disconnect()
    {
        if (con != null)
        {
            try
            {
                con.close();
                con = null;
                System.out.println("Disconnected from database.");
            }
            catch (SQLException e)
            {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Get an employee by ID, including their current title, salary,
    // department, and department manager where available.
    public Employee getEmployee(int id)
    {
        if (con == null)
        {
            System.out.println("Database is not connected.");
            return null;
        }

        String sql =
                "SELECT e.emp_no, e.first_name, e.last_name, " +
                        "t.title, s.salary, d.dept_no, d.dept_name, " +
                        "m.emp_no AS manager_emp_no, " +
                        "m.first_name AS manager_first_name, " +
                        "m.last_name AS manager_last_name " +
                        "FROM employees e " +
                        "LEFT JOIN titles t ON e.emp_no = t.emp_no " +
                        "AND t.to_date = '9999-01-01' " +
                        "LEFT JOIN salaries s ON e.emp_no = s.emp_no " +
                        "AND s.to_date = '9999-01-01' " +
                        "LEFT JOIN dept_emp de ON e.emp_no = de.emp_no " +
                        "AND de.to_date = '9999-01-01' " +
                        "LEFT JOIN departments d ON de.dept_no = d.dept_no " +
                        "LEFT JOIN dept_manager dm ON d.dept_no = dm.dept_no " +
                        "AND dm.to_date = '9999-01-01' " +
                        "LEFT JOIN employees m ON dm.emp_no = m.emp_no " +
                        "WHERE e.emp_no = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql))
        {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    Employee emp = new Employee();
                    emp.emp_no = rs.getInt("emp_no");
                    emp.first_name = rs.getString("first_name");
                    emp.last_name = rs.getString("last_name");
                    emp.title = rs.getString("title");
                    emp.salary = rs.getInt("salary");

                    String deptNo = rs.getString("dept_no");
                    if (deptNo != null)
                    {
                        emp.dept = new Department(
                                deptNo, rs.getString("dept_name"));

                        int managerId = rs.getInt("manager_emp_no");
                        if (!rs.wasNull())
                        {
                            Employee manager = new Employee();
                            manager.emp_no = managerId;
                            manager.first_name =
                                    rs.getString("manager_first_name");
                            manager.last_name =
                                    rs.getString("manager_last_name");
                            emp.dept.manager = manager;
                            emp.manager = manager;
                        }
                    }

                    return emp;
                }
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to get employee: " + e.getMessage());
        }

        return null;
    }

    // Display employee details safely.
    public void displayEmployee(Employee emp)
    {
        if (emp == null)
        {
            System.out.println("Employee not found.");
            return;
        }

        String departmentName =
                emp.dept != null && emp.dept.dept_name != null
                        ? emp.dept.dept_name : "Not assigned";

        String managerName =
                emp.manager != null
                        ? emp.manager.first_name + " " + emp.manager.last_name
                        : "Not assigned";

        System.out.println(
                emp.emp_no + " " + emp.first_name + " " + emp.last_name + "\n" +
                        "Title: " + (emp.title != null ? emp.title : "Not available") + "\n" +
                        "Salary: " + emp.salary + "\n" +
                        "Department: " + departmentName + "\n" +
                        "Manager: " + managerName + "\n"
        );
    }

    // Get a department by name, including its current manager.
    public Department getDepartment(String name)
    {
        if (con == null)
        {
            System.out.println("Database is not connected.");
            return null;
        }

        String sql =
                "SELECT d.dept_no, d.dept_name, " +
                        "e.emp_no AS manager_emp_no, " +
                        "e.first_name AS manager_first_name, " +
                        "e.last_name AS manager_last_name " +
                        "FROM departments d " +
                        "LEFT JOIN dept_manager dm ON d.dept_no = dm.dept_no " +
                        "AND dm.to_date = '9999-01-01' " +
                        "LEFT JOIN employees e ON dm.emp_no = e.emp_no " +
                        "WHERE d.dept_name = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql))
        {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    Department dept = new Department(
                            rs.getString("dept_no"),
                            rs.getString("dept_name")
                    );

                    int managerId = rs.getInt("manager_emp_no");
                    if (!rs.wasNull())
                    {
                        Employee manager = new Employee();
                        manager.emp_no = managerId;
                        manager.first_name =
                                rs.getString("manager_first_name");
                        manager.last_name =
                                rs.getString("manager_last_name");
                        dept.manager = manager;
                    }

                    return dept;
                }
            }
        }
        catch (SQLException e)
        {
            System.out.println("Failed to get department: " + e.getMessage());
        }

        return null;
    }

    // Get current employees and salary details for a department.
    public ArrayList<Employee> getSalariesByDepartment(Department dept)
    {
        ArrayList<Employee> employees = new ArrayList<>();

        if (con == null)
        {
            System.out.println("Database is not connected.");
            return employees;
        }

        if (dept == null || dept.dept_no == null)
        {
            System.out.println("Department not found.");
            return employees;
        }

        String sql =
                "SELECT e.emp_no, e.first_name, e.last_name, " +
                        "s.salary, t.title, d.dept_no, d.dept_name, " +
                        "m.emp_no AS manager_emp_no, " +
                        "m.first_name AS manager_first_name, " +
                        "m.last_name AS manager_last_name " +
                        "FROM employees e " +
                        "JOIN dept_emp de ON e.emp_no = de.emp_no " +
                        "JOIN departments d ON de.dept_no = d.dept_no " +
                        "JOIN salaries s ON e.emp_no = s.emp_no " +
                        "LEFT JOIN titles t ON e.emp_no = t.emp_no " +
                        "AND t.to_date = '9999-01-01' " +
                        "LEFT JOIN dept_manager dm ON d.dept_no = dm.dept_no " +
                        "AND dm.to_date = '9999-01-01' " +
                        "LEFT JOIN employees m ON dm.emp_no = m.emp_no " +
                        "WHERE d.dept_no = ? " +
                        "AND de.to_date = '9999-01-01' " +
                        "AND s.to_date = '9999-01-01' " +
                        "ORDER BY e.emp_no ASC";

        try (PreparedStatement stmt = con.prepareStatement(sql))
        {
            stmt.setString(1, dept.dept_no);

            try (ResultSet rs = stmt.executeQuery())
            {
                while (rs.next())
                {
                    Employee emp = new Employee();
                    emp.emp_no = rs.getInt("emp_no");
                    emp.first_name = rs.getString("first_name");
                    emp.last_name = rs.getString("last_name");
                    emp.salary = rs.getInt("salary");
                    emp.title = rs.getString("title");

                    emp.dept = new Department(
                            rs.getString("dept_no"),
                            rs.getString("dept_name")
                    );

                    int managerId = rs.getInt("manager_emp_no");
                    if (!rs.wasNull())
                    {
                        Employee manager = new Employee();
                        manager.emp_no = managerId;
                        manager.first_name =
                                rs.getString("manager_first_name");
                        manager.last_name =
                                rs.getString("manager_last_name");

                        emp.manager = manager;
                        emp.dept.manager = manager;
                    }

                    employees.add(emp);
                }
            }
        }
        catch (SQLException e)
        {
            System.out.println(
                    "Failed to get department salaries: " + e.getMessage());
        }

        return employees;
    }

    // Print the salary report.
    public void printSalaries(ArrayList<Employee> employees)
    {
        if (employees == null || employees.isEmpty())
        {
            System.out.println("No salary data available.");
            return;
        }

        System.out.println("\nDepartment Salary Report");
        System.out.println("================================================================");

        for (Employee emp : employees)
        {
            if (emp == null)
                continue;

            String departmentName =
                    emp.dept != null && emp.dept.dept_name != null
                            ? emp.dept.dept_name : "Not assigned";

            String managerName =
                    emp.manager != null
                            ? emp.manager.first_name + " " + emp.manager.last_name
                            : "Not assigned";

            System.out.printf(
                    "Employee ID: %d, Name: %s %s, Title: %s, " +
                            "Department: %s, Salary: %d, Manager: %s%n",
                    emp.emp_no,
                    emp.first_name,
                    emp.last_name,
                    emp.title != null ? emp.title : "Not available",
                    departmentName,
                    emp.salary,
                    managerName
            );
        }

        System.out.println("================================================================");
        System.out.println("Total employees: " + employees.size());
    }

    public static void main(String[] args)
    {
        App app = new App();

        String location = "localhost:33060";
        int delay = 3000;

        if (args.length >= 1)
            location = args[0];

        if (args.length >= 2)
        {
            try
            {
                delay = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Delay must be a valid integer.");
                return;
            }
        }

        app.connect(location, delay);

        try
        {
            Department dept = app.getDepartment("Development");
            ArrayList<Employee> employees =
                    app.getSalariesByDepartment(dept);
            app.printSalaries(employees);
        }
        finally
        {
            app.disconnect();
        }
    }
}
