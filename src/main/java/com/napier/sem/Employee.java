package com.napier.sem;

/**
 * Represents an employee in the employees database.
 */
public class Employee
{
    public int emp_no;
    public String first_name;
    public String last_name;
    public String title;
    public int salary;

    // Department in which this employee works
    public Department dept;

    // Employee who manages this employee
    public Employee manager;

    public Employee()
    {
    }
}
