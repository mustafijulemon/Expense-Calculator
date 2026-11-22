import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ExpenseTracker {
    private static final Path DATA_FILE = Path.of("expenses.csv");
    private static final Scanner INPUT = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        List<Expense> expenses = loadExpenses();
        boolean running = true;

        while (running) {
            System.out.println("\n=== Expense Tracker ===");
            System.out.println("1. Add expense");
            System.out.println("2. List expenses");
            System.out.println("3. Monthly summary");
            System.out.println("4. Delete expense");
            System.out.println("0. Save and exit");
            System.out.print("Choose an option: ");

            switch (INPUT.nextLine().trim()) {
                case "1" -> addExpense(expenses);
                case "2" -> listExpenses(expenses);
                case "3" -> showMonthlySummary(expenses);
                case "4" -> deleteExpense(expenses);
                case "0" -> running = false;
                default -> System.out.println("Please choose one of the listed options.");
            }
        }

        saveExpenses(expenses);
        System.out.println("Expenses saved to " + DATA_FILE.toAbsolutePath());
    }

    private static void addExpense(List<Expense> expenses) {
        try {
            System.out.print("Date (YYYY-MM-DD, blank for today): ");
            String dateInput = INPUT.nextLine().trim();
            LocalDate date = dateInput.isEmpty() ? LocalDate.now() : LocalDate.parse(dateInput);

            System.out.print("Category: ");
            String category = requireText(INPUT.nextLine(), "Category");

            System.out.print("Amount: ");
            BigDecimal amount = new BigDecimal(INPUT.nextLine().trim());
            if (amount.signum() <= 0) {
                System.out.println("Amount must be greater than zero.");
                return;
            }

            System.out.print("Description: ");
            String description = requireText(INPUT.nextLine(), "Description");
            expenses.add(new Expense(date, category, amount, description));
            System.out.println("Expense added.");
        } catch (DateTimeParseException | NumberFormatException e) {
            System.out.println("Invalid date or amount. Please try again.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void listExpenses(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            System.out.println("No expenses recorded.");
            return;
        }

        System.out.printf("%-4s %-12s %-18s %12s  %s%n",
                "#", "Date", "Category", "Amount", "Description");
        for (int i = 0; i < expenses.size(); i++) {
            Expense expense = expenses.get(i);
            System.out.printf("%-4d %-12s %-18s %12s  %s%n",
                    i + 1, expense.date(), expense.category(),
                    expense.amount().toPlainString(), expense.description());
        }
    }

    private static void showMonthlySummary(List<Expense> expenses) {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal total = expenses.stream()
                .filter(expense -> YearMonth.from(expense.date()).equals(currentMonth))
                .map(Expense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println(currentMonth + " total: " + total.toPlainString());
    }

    private static void deleteExpense(List<Expense> expenses) {
        listExpenses(expenses);
        if (expenses.isEmpty()) {
            return;
        }

        System.out.print("Enter the number to delete: ");
        try {
            int index = Integer.parseInt(INPUT.nextLine().trim()) - 1;
            if (index < 0 || index >= expenses.size()) {
                System.out.println("That number is not in the list.");
                return;
            }
            expenses.remove(index);
            System.out.println("Expense deleted.");
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid whole number.");
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

    private static String toCsvLine(List<String> fields) {
        return fields.stream()
                .map(field -> "\"" + field.replace("\"", "\"\"") + "\"")
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (quoted) {
                if (current == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (current == '"') {
                    quoted = false;
                } else {
                    field.append(current);
                }
            } else if (current == ',' && field.length() == 0) {
                fields.add("");
            } else if (current == ',' && i > 0 && line.charAt(i - 1) == '"') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (current == '"' && field.length() == 0) {
                quoted = true;
            } else {
                field.append(current);
            }
        }

        if (quoted) {
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
