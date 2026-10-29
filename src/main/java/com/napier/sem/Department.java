package com.napier.sem;

/**
 * Represents a department in the employees database.
 */
public class Department
{
    public String dept_no;
    public String dept_name;
    public Employee manager;

    public Department()
    {
    }

    public Department(String dept_no, String dept_name)
    {
        this.dept_no = dept_no;
        this.dept_name = dept_name;
    }
}
