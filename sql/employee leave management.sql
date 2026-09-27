CREATE DATABASE IF NOT EXISTS testdb;
USE testdb;
SELECT DATABASE();

CREATE TABLE IF NOT EXISTS employees (
    employee_id INT PRIMARY KEY AUTO_INCREMENT,
    employee_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    department VARCHAR(50),
    joining_date DATE
);

CREATE TABLE IF NOT EXISTS leave_balance (
    balance_id INT PRIMARY KEY AUTO_INCREMENT,
    employee_id INT NOT NULL,
    total_leaves INT DEFAULT 20,
    used_leaves INT DEFAULT 0,
    remaining_leaves INT DEFAULT 20,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
);

CREATE TABLE IF NOT EXISTS leave_requests (
    leave_id INT PRIMARY KEY AUTO_INCREMENT,
    employee_id INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    leave_days INT NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
);
SHOW TABLES;