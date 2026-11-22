import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class ExpenseTracker {
    private static final Path DATA_FILE = Path.of("expenses.csv");
    private static final Scanner INPUT = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        List<Expense> expenses = loadExpenses();
        Map<YearMonth, BigDecimal> budgets = loadBudgets();
        boolean running = true;

        while (running) {
            System.out.println("\n=== Expense Tracker ===");
            System.out.println("1. Add expense");
            System.out.println("2. List expenses");
            System.out.println("3. Filter expenses");
            System.out.println("4. Monthly report");
            System.out.println("5. Set monthly budget");
            System.out.println("6. Edit expense");
            System.out.println("7. Delete expense");
            System.out.println("0. Save and exit");
            System.out.print("Choose an option: ");

            switch (INPUT.nextLine().trim()) {
                case "1" -> addExpense(expenses);
                case "2" -> listExpenses(expenses);
                case "3" -> filterExpenses(expenses);
                case "4" -> showMonthlyReport(expenses, budgets);
                case "5" -> setMonthlyBudget(budgets);
                case "6" -> editExpense(expenses);
                case "7" -> deleteExpense(expenses);
                case "0" -> running = false;
                default -> System.out.println("Please choose one of the listed options.");
            }
        }

        saveExpenses(expenses);
        saveBudgets(budgets);
        System.out.println("Expenses and budgets saved.");
    }

    private static void addExpense(List<Expense> expenses) {
        Expense expense = readExpense(null);
        if (expense != null) {
            expenses.add(expense);
            System.out.println("Expense added.");
        }
    }

    private static Expense readExpense(Expense existing) {
        try {
            String dateHint = existing == null
                    ? "Date (YYYY-MM-DD, blank for today): "
                    : "Date (YYYY-MM-DD, blank to keep " + existing.date() + "): ";
            System.out.print(dateHint);
            String dateInput = INPUT.nextLine().trim();
            LocalDate date = dateInput.isEmpty()
                    ? (existing == null ? LocalDate.now() : existing.date())
                    : LocalDate.parse(dateInput);

            String category = readText("Category", existing == null ? null : existing.category());
            String amountHint = existing == null
                    ? "Amount: "
                    : "Amount (blank to keep " + existing.amount().toPlainString() + "): ";
            System.out.print(amountHint);
            String amountInput = INPUT.nextLine().trim();
            BigDecimal amount = amountInput.isEmpty() && existing != null
                    ? existing.amount()
                    : new BigDecimal(amountInput);
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }

            String description = readText(
                    "Description", existing == null ? null : existing.description());
            return new Expense(date, category, amount, description);
        } catch (DateTimeParseException | NumberFormatException e) {
            System.out.println("Invalid date or amount. Please try again.");
            return null;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private static String readText(String label, String existingValue) {
        String prompt = existingValue == null
                ? label + ": "
                : label + " (blank to keep \"" + existingValue + "\"): ";
        System.out.print(prompt);
        String value = INPUT.nextLine();
        return value.isBlank() && existingValue != null
                ? existingValue
                : requireText(value, label);
    }

    private static void listExpenses(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }

        printExpenses(expenses);
    }

    private static void printExpenses(List<Expense> expenses) {
        System.out.printf("%-4s %-12s %-18s %12s  %s%n",
                "#", "Date", "Category", "Amount", "Description");
        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            System.out.printf("%-4d %-12s %-18s %12s  %s%n",
                    i + 1, expense.date(), expense.category(),
                    expense.amount().toPlainString(), expense.description());
        }
    }

    private static void filterExpenses(List<Expense> expenses) {
        System.out.print("Month (YYYY-MM, blank for any): ");
        String monthInput = INPUT.nextLine().trim();
        System.out.print("Category (blank for any): ");
        String categoryInput = INPUT.nextLine().trim();

        try {
            YearMonth month = monthInput.isEmpty() ? null : YearMonth.parse(monthInput);
            List<Expense> matches = new ArrayList<>();
            for (int i = 0; i < expenses.size(); i++) {
                Expense expense = expenses.get(i);
                boolean monthMatches = month == null
                        || YearMonth.from(expense.date()).equals(month);
                boolean categoryMatches = categoryInput.isEmpty()
                        || expense.category().equalsIgnoreCase(categoryInput);
                if (monthMatches && categoryMatches) {
                    matches.add(expense);
                }
            }
            if (matches.isEmpty()) {
                System.out.println("No matching expenses.");
            } else {
                System.out.println("Matching expenses:");
                printExpenses(matches);
            }
        } catch (DateTimeParseException e) {
            System.out.println("Invalid month. Use YYYY-MM.");
        }
    }

    private static void showMonthlyReport(
            List<Expense> expenses, Map<YearMonth, BigDecimal> budgets) {
        YearMonth month = readMonth();
        if (month == null) {
            return;
        }

        List<Expense> monthlyExpenses = expenses.stream()
                .filter(expense -> YearMonth.from(expense.date()).equals(month))
                .sorted(Comparator.comparing(Expense::date))
                .toList();
        BigDecimal total = monthlyExpenses.stream()
                .map(Expense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("\nReport for " + month);
        System.out.println("Expenses: " + monthlyExpenses.size());
        System.out.println("Total: " + total.toPlainString());
        if (!monthlyExpenses.isEmpty()) {
            Map<String, BigDecimal> byCategory = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Expense expense : monthlyExpenses) {
                byCategory.merge(expense.category(), expense.amount(), BigDecimal::add);
            }
            System.out.println("By category:");
            byCategory.forEach((category, amount) ->
                    System.out.println("  " + category + ": " + amount.toPlainString()));
        }

        BigDecimal budget = budgets.get(month);
        if (budget == null) {
            System.out.println("No budget set for this month.");
        } else {
            BigDecimal remaining = budget.subtract(total);
            System.out.println("Budget: " + budget.toPlainString());
            if (remaining.signum() >= 0) {
                System.out.println("Remaining: " + remaining.toPlainString());
            } else {
                System.out.println("Over budget by: " + remaining.abs().toPlainString());
            }
        }
    }

    private static YearMonth readMonth() {
        System.out.print("Month (YYYY-MM, blank for current month): ");
        String input = INPUT.nextLine().trim();
        try {
            return input.isEmpty() ? YearMonth.now() : YearMonth.parse(input);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid month. Use YYYY-MM.");
            return null;
        }
    }

    private static void setMonthlyBudget(Map<YearMonth, BigDecimal> budgets) {
        YearMonth month = readMonth();
        if (month == null) {
            return;
        }

        System.out.print("Budget amount (enter 0 to remove this month's budget): ");
        try {
            BigDecimal amount = new BigDecimal(INPUT.nextLine().trim());
            if (amount.signum() < 0) {
                System.out.println("Budget cannot be negative.");
            } else if (amount.signum() == 0) {
                budgets.remove(month);
                System.out.println("Budget removed for " + month + ".");
            } else {
                budgets.put(month, amount);
                System.out.println("Budget saved for " + month + ".");
            }
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid amount.");
        }
    }

    private static void editExpense(List<Expense> expenses) {
        listExpenses(expenses);
        int index = readExpenseIndex(expenses);
        if (index < 0) {
            return;
        }

        Expense updated = readExpense(expenses.get(index));
        if (updated != null) {
            expenses.set(index, updated);
            System.out.println("Expense updated.");
        }
    }

    private static void deleteExpense(List<Expense> expenses) {
        listExpenses(expenses);
        int index = readExpenseIndex(expenses);
        if (index >= 0) {
            expenses.remove(index);
            System.out.println("Expense deleted.");
        }
    }

    private static int readExpenseIndex(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return -1;
        }

        System.out.print("Enter the expense number (0 to cancel): ");
        try {
            int index = Integer.parseInt(INPUT.nextLine().trim()) - 1;
            if (index == -1) {
                return -1;
            }
            if (index < 0 || index >= expenses.size()) {
                System.out.println("That number is not in the list.");
                return -1;
            }
            return index;
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid whole number.");
            return -1;
        }
    }

    private static String requireText(String value, String fieldName) {
        String text = value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return text;
    }

    private static List<Expense> loadExpenses() throws IOException {
        List<Expense> expenses = new ArrayList<>();
        if (!Files.exists(DATA_FILE)) {
            return expenses;
        }

        List<String> lines = Files.readAllLines(DATA_FILE);
        for (int i = 0; i < lines.size(); i++) {
            try {
                List<String> fields = parseCsvLine(lines.get(i));
                if (fields.size() != 4) {
                    throw new IllegalArgumentException("Expected four fields.");
                }
                expenses.add(new Expense(
                        LocalDate.parse(fields.get(0)),
                        fields.get(1),
                        new BigDecimal(fields.get(2)),
                        fields.get(3)));
            } catch (RuntimeException e) {
                throw new IOException("Invalid expense data on line " + (i + 1)
                        + " of " + DATA_FILE + ".", e);
            }
        }
        return expenses;
    }

    private static void saveExpenses(List<Expense> expenses) throws IOException {
        List<String> lines = expenses.stream()
                .map(expense -> toCsvLine(List.of(
                        expense.date().toString(),
                        expense.category(),
                        expense.amount().toPlainString(),
                        expense.description())))
                .toList();
        Files.write(DATA_FILE, lines);
    }

    private static Map<YearMonth, BigDecimal> loadBudgets() throws IOException {
        Map<YearMonth, BigDecimal> budgets = new TreeMap<>();
        Path budgetFile = Path.of("budgets.csv");
        if (!Files.exists(budgetFile)) {
            return budgets;
        }

        List<String> lines = Files.readAllLines(budgetFile);
        for (int i = 0; i < lines.size(); i++) {
            try {
                List<String> fields = parseCsvLine(lines.get(i));
                if (fields.size() != 2) {
                    throw new IllegalArgumentException("Expected two fields.");
                }
                BigDecimal amount = new BigDecimal(fields.get(1));
                if (amount.signum() <= 0) {
                    throw new IllegalArgumentException("Budget must be greater than zero.");
                }
                budgets.put(YearMonth.parse(fields.get(0)), amount);
            } catch (RuntimeException e) {
                throw new IOException("Invalid budget data on line " + (i + 1)
                        + " of " + budgetFile + ".", e);
            }
        }
        return budgets;
    }

    private static void saveBudgets(Map<YearMonth, BigDecimal> budgets) throws IOException {
        List<String> lines = budgets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toCsvLine(List.of(
                        entry.getKey().toString(),
                        entry.getValue().toPlainString())))
                .toList();
        Files.write(Path.of("budgets.csv"), lines);
    }

    private static String toCsvLine(List<String> fields) {
        return fields.stream()
                .map(field -> "\"" + field.replace("\"", "\"\"") + "\"")
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean closedQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (inQuotes) {
                if (current == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (current == '"') {
                    inQuotes = false;
                    closedQuote = true;
                } else {
                    field.append(current);
                }
            } else if (current == ',') {
                fields.add(field.toString());
                field.setLength(0);
                closedQuote = false;
            } else if (current == '"' && field.length() == 0 && !closedQuote) {
                inQuotes = true;
            } else if (closedQuote) {
                throw new IllegalArgumentException("Unexpected text after a quoted field.");
            } else {
                field.append(current);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quoted field.");
        }
        fields.add(field.toString());
        return fields;
    }

    private record Expense(LocalDate date, String category, BigDecimal amount, String description) {
        private Expense {
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }
        }
    }
}
