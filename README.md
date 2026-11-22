# Java Expense Tracker

A small, dependency-free command-line expense tracker. It can add, list, and delete expenses, and show the total for the current month.

## Requirements

- Java 17 or newer

## Run

From this folder:

```text
javac ExpenseTracker.java
java ExpenseTracker
```

Expenses are saved in `expenses.csv` in the current folder. Each expense has a date, category, positive amount, and description. Dates use `YYYY-MM-DD`; leave the date blank when adding an expense to use today's date.
