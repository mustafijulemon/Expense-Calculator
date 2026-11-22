# Java Expense Tracker

A dependency-free command-line expense tracker. Record and edit expenses, filter them by month or category, review category totals, and compare monthly spending with a budget.

## Requirements

- Java 17 or newer

## Run

From this folder:

```text
javac ExpenseTracker.java
java ExpenseTracker
```

Expenses are saved in `expenses.csv` in the current folder. Each expense has a date, category, positive amount, and description. Dates use `YYYY-MM-DD`; leave the date blank when adding an expense to use today's date.

Monthly budgets are saved separately in `budgets.csv`. Both files are created when you exit the application. In the menu, a blank month means the current month; enter a month as `YYYY-MM`. Enter `0` as a budget to remove it. When editing an expense, leave a field blank to keep its current value.

## Features

- Add, list, edit, and delete expenses
- Filter by month, category, or both
- View monthly totals and category breakdowns
- Set and track a separate budget for each month
- Persist data locally as CSV files
