import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

public class ExpenseTracker {

    static final String DATA_DIR      = "data";
    static final String EXPENSES_FILE = DATA_DIR + "/expenses.csv";
    static final String BUDGETS_FILE  = DATA_DIR + "/budgets.csv";
    static final String ALERTS_FILE   = DATA_DIR + "/alerts.csv";

    static final String R = "\033[0m",  BOLD = "\033[1m";
    static final String G = "\033[32m", Y = "\033[33m";
    static final String RED = "\033[31m", C = "\033[36m", B = "\033[34m";

    static final Scanner sc = new Scanner(System.in);
    static int nextExpenseId = 1, nextBudgetId = 1, nextAlertId = 1;

    public static void main(String[] args) {
        initFiles();
        computeNextIds();
        clearScreen();
        banner();
        showUnreadCount();

        boolean run = true;
        while (run) {
            mainMenu();
            int c = readInt("Enter choice: ");
            switch (c) {
                case 1 -> addExpense();
                case 2 -> viewExpenses();
                case 3 -> monthlySummary();
                case 4 -> manageBudgets();
                case 5 -> viewAlerts();
                case 6 -> deleteExpense();
                case 7 -> { System.out.println(G + "\nGoodbye! Spend wisely!" + R); run = false; }
                default -> System.out.println(RED + "Invalid choice." + R);
            }
        }
        sc.close();
    }

    // ── FILE INIT ───────────────────────────────────────────────────────────
    static void initFiles() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            if (!Files.exists(Paths.get(EXPENSES_FILE)))
                Files.writeString(Paths.get(EXPENSES_FILE), "id,category,amount,description,date\n");
            if (!Files.exists(Paths.get(BUDGETS_FILE)))
                Files.writeString(Paths.get(BUDGETS_FILE), "id,category,limit,month,year\n");
            if (!Files.exists(Paths.get(ALERTS_FILE)))
                Files.writeString(Paths.get(ALERTS_FILE), "id,category,message,type,datetime,read\n");
        } catch (IOException e) { System.err.println("Init error: " + e.getMessage()); }
    }

    static void computeNextIds() {
        nextExpenseId = maxId(EXPENSES_FILE) + 1;
        nextBudgetId  = maxId(BUDGETS_FILE)  + 1;
        nextAlertId   = maxId(ALERTS_FILE)   + 1;
    }

    static int maxId(String file) {
        try {
            return Files.readAllLines(Paths.get(file)).stream().skip(1)
                .mapToInt(l -> { try { return Integer.parseInt(l.split(",")[0].trim()); } catch (Exception e) { return 0; } })
                .max().orElse(0);
        } catch (IOException e) { return 0; }
    }

    // ── ADD EXPENSE ─────────────────────────────────────────────────────────
    static void addExpense() {
        System.out.println("\n" + BOLD + G + "ADD NEW EXPENSE" + R);
        System.out.println("Categories: Food | Transport | Shopping | Bills | Health | Entertainment | Education | Other");
        String cat = readString("Category: ").trim();
        if (cat.isEmpty()) cat = "Other";
        double amt = readDouble("Amount (Rs): ");
        if (amt <= 0) { System.out.println(RED + "Amount must be positive!" + R); return; }
        String desc = readString("Description (optional): ").trim().replace(",", ";");
        System.out.print("Date (YYYY-MM-DD, Enter=today): ");
        String ds = sc.nextLine().trim();
        String date = ds.isEmpty() ? LocalDate.now().toString() : ds;

        appendLine(EXPENSES_FILE, nextExpenseId + "," + cat + "," + amt + "," + desc + "," + date + "\n");
        System.out.println(G + "\nExpense added! ID=" + nextExpenseId + R);
        nextExpenseId++;
        checkBudgetAlert(cat, date);
    }

    // ── VIEW EXPENSES ────────────────────────────────────────────────────────
    static void viewExpenses() {
        System.out.println("\n" + BOLD + C + "ALL EXPENSES" + R);
        List<String[]> rows = readCsv(EXPENSES_FILE);
        if (rows.isEmpty()) { System.out.println(Y + "No expenses yet." + R); return; }
        System.out.printf("%-5s %-14s %-10s %-28s %-12s%n", "ID","Category","Amount","Description","Date");
        System.out.println("-".repeat(72));
        double total = 0;
        for (String[] r : rows) {
            System.out.printf("%-5s %-14s " + G + "Rs %-7s" + R + " %-28s %-12s%n",
                r[0], r[1], r[2], trunc(r.length>3?r[3]:"",26), r.length>4?r[4]:"");
            try { total += Double.parseDouble(r[2].trim()); } catch (Exception ignored) {}
        }
        System.out.println("-".repeat(72));
        System.out.printf(BOLD + "Total: " + G + "Rs %.2f" + R + "%n", total);
    }

    // ── DELETE EXPENSE ───────────────────────────────────────────────────────
    static void deleteExpense() {
        viewExpenses();
        int id = readInt("Enter Expense ID to delete (0=cancel): ");
        if (id == 0) return;
        try {
            List<String> lines = Files.readAllLines(Paths.get(EXPENSES_FILE));
            List<String> updated = new ArrayList<>();
            updated.add(lines.get(0));
            boolean found = false;
            for (int i = 1; i < lines.size(); i++) {
                if (!lines.get(i).split(",")[0].trim().equals(String.valueOf(id))) updated.add(lines.get(i));
                else found = true;
            }
            if (found) { Files.write(Paths.get(EXPENSES_FILE), updated); System.out.println(G + "Deleted ID " + id + R); }
            else System.out.println(RED + "ID not found." + R);
        } catch (IOException e) { System.out.println(RED + "Error." + R); }
    }

    // ── MONTHLY SUMMARY ──────────────────────────────────────────────────────
    static void monthlySummary() {
        int month = LocalDate.now().getMonthValue(), year = LocalDate.now().getYear();
        System.out.println("\n" + BOLD + C + "MONTHLY SUMMARY - " + Month.of(month) + " " + year + R);

        Map<String, Double> catTotal = new LinkedHashMap<>();
        double grandTotal = 0;
        for (String[] r : readCsv(EXPENSES_FILE)) {
            try {
                LocalDate d = LocalDate.parse(r[r.length-1].trim());
                if (d.getMonthValue() == month && d.getYear() == year) {
                    double amt = Double.parseDouble(r[2].trim());
                    catTotal.merge(r[1].trim(), amt, Double::sum);
                    grandTotal += amt;
                }
            } catch (Exception ignored) {}
        }

        if (catTotal.isEmpty()) { System.out.println(Y + "No expenses this month." + R); return; }

        List<String[]> budgets = readCsv(BUDGETS_FILE);
        System.out.printf("%-18s %-12s %-12s %-14s%n","Category","Spent","Budget","Status");
        System.out.println("-".repeat(58));

        for (Map.Entry<String,Double> e : catTotal.entrySet()) {
            String cat = e.getKey(); double spent = e.getValue();
            String budgetStr = "Not set", status = "";
            for (String[] b : budgets) {
                if (b.length >= 5 && b[1].trim().equalsIgnoreCase(cat) &&
                    b[3].trim().equals(String.valueOf(month)) && b[4].trim().equals(String.valueOf(year))) {
                    double lim = Double.parseDouble(b[2].trim());
                    budgetStr = String.format("Rs %.2f", lim);
                    double pct = (spent/lim)*100;
                    if (pct >= 100) status = RED + "OVER BUDGET!" + R;
                    else if (pct >= 80) status = Y + String.format("WARNING %.0f%%", pct) + R;
                    else status = G + String.format("OK %.0f%%", pct) + R;
                    break;
                }
            }
            System.out.printf("%-18s " + G + "Rs %-9.2f" + R + " %-12s %s%n", cat, spent, budgetStr, status);
        }
        System.out.println("-".repeat(58));
        System.out.printf(BOLD + "Total this month: " + G + "Rs %.2f" + R + "%n", grandTotal);
    }

    // ── BUDGET MANAGEMENT ────────────────────────────────────────────────────
    static void manageBudgets() {
        System.out.println("\n" + BOLD + Y + "BUDGET MANAGEMENT" + R);
        System.out.println("1. Set/Update Budget  2. View Budgets  3. Delete Budget  4. Back");
        int c = readInt("Choice: ");
        switch (c) {
            case 1 -> setBudget();
            case 2 -> viewBudgets();
            case 3 -> deleteBudget();
        }
    }

    static void setBudget() {
        String cat = readString("Category: ").trim();
        double lim = readDouble("Monthly Limit (Rs): ");
        int month = LocalDate.now().getMonthValue(), year = LocalDate.now().getYear();
        try {
            List<String> lines = Files.readAllLines(Paths.get(BUDGETS_FILE));
            List<String> updated = new ArrayList<>(); updated.add(lines.get(0));
            for (int i = 1; i < lines.size(); i++) {
                String[] p = lines.get(i).split(",");
                if (!(p.length>=5 && p[1].trim().equalsIgnoreCase(cat) && p[3].trim().equals(String.valueOf(month)) && p[4].trim().equals(String.valueOf(year))))
                    updated.add(lines.get(i));
            }
            Files.write(Paths.get(BUDGETS_FILE), updated);
        } catch (IOException ignored) {}
        appendLine(BUDGETS_FILE, nextBudgetId + "," + cat + "," + lim + "," + month + "," + year + "\n");
        nextBudgetId++;
        System.out.println(G + "Budget set: " + cat + " -> Rs " + lim + "/month" + R);
    }

    static void viewBudgets() {
        int month = LocalDate.now().getMonthValue(), year = LocalDate.now().getYear();
        List<String[]> rows = readCsv(BUDGETS_FILE).stream()
            .filter(r -> r.length>=5 && r[3].trim().equals(String.valueOf(month)) && r[4].trim().equals(String.valueOf(year)))
            .collect(Collectors.toList());
        if (rows.isEmpty()) { System.out.println(Y + "No budgets this month." + R); return; }
        System.out.printf("%-5s %-18s %-12s%n","ID","Category","Limit");
        System.out.println("-".repeat(38));
        rows.forEach(r -> System.out.printf("%-5s %-18s " + Y + "Rs %s" + R + "%n", r[0], r[1], r[2]));
    }

    static void deleteBudget() {
        viewBudgets();
        int id = readInt("Budget ID to delete: ");
        try {
            List<String> lines = Files.readAllLines(Paths.get(BUDGETS_FILE));
            List<String> updated = new ArrayList<>(); updated.add(lines.get(0));
            for (int i = 1; i < lines.size(); i++)
                if (!lines.get(i).split(",")[0].trim().equals(String.valueOf(id))) updated.add(lines.get(i));
            Files.write(Paths.get(BUDGETS_FILE), updated);
            System.out.println(G + "Deleted." + R);
        } catch (IOException e) { System.out.println(RED + "Error." + R); }
    }

    // ── ALERTS ──────────────────────────────────────────────────────────────
    static void checkBudgetAlert(String cat, String date) {
        try {
            LocalDate d = LocalDate.parse(date);
            int month = d.getMonthValue(), year = d.getYear();
            double spent = readCsv(EXPENSES_FILE).stream()
                .filter(r -> r.length>=5 && r[1].trim().equalsIgnoreCase(cat))
                .filter(r -> { try { LocalDate ed = LocalDate.parse(r[4].trim()); return ed.getMonthValue()==month && ed.getYear()==year; } catch(Exception e){return false;} })
                .mapToDouble(r -> { try { return Double.parseDouble(r[2].trim()); } catch(Exception e){return 0;} }).sum();

            for (String[] b : readCsv(BUDGETS_FILE)) {
                if (b.length>=5 && b[1].trim().equalsIgnoreCase(cat) &&
                    b[3].trim().equals(String.valueOf(month)) && b[4].trim().equals(String.valueOf(year))) {
                    double lim = Double.parseDouble(b[2].trim());
                    double pct = (spent/lim)*100;
                    String msg = "", type = "";
                    if (pct >= 100) {
                        msg = String.format("BUDGET EXCEEDED! [%s] Spent Rs %.2f of Rs %.2f (%.1f%%)", cat, spent, lim, pct);
                        type = "EXCEEDED";
                        System.out.println("\n" + RED + BOLD + "!!! " + msg + " !!!" + R);
                    } else if (pct >= 80) {
                        msg = String.format("WARNING! [%s] %.1f%% used (Rs %.2f / Rs %.2f)", cat, pct, spent, lim);
                        type = "WARNING";
                        System.out.println("\n" + Y + BOLD + ">> " + msg + R);
                    }
                    if (!msg.isEmpty()) {
                        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        appendLine(ALERTS_FILE, nextAlertId + "," + cat + ",\"" + msg + "\"," + type + "," + now + ",0\n");
                        nextAlertId++;
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    static void viewAlerts() {
        System.out.println("\n" + BOLD + RED + "ALERTS" + R);
        try {
            List<String> lines = Files.readAllLines(Paths.get(ALERTS_FILE));
            if (lines.size() <= 1) { System.out.println(G + "No alerts! You're within budget." + R); return; }
            for (int i = lines.size()-1; i >= 1; i--) {
                String line = lines.get(i);
                String[] p = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (p.length < 6) continue;
                String type = p[3].trim();
                String color = type.equals("EXCEEDED") ? RED : type.equals("WARNING") ? Y : C;
                String readStatus = p[5].trim().equals("0") ? BOLD+G+" [NEW]"+R : " [read]";
                System.out.println(color + "-".repeat(55) + R);
                System.out.println(color + p[2].replace("\"","") + R + readStatus);
                if (p.length > 4) System.out.println(C + p[4] + R);
            }
            List<String> updated = new ArrayList<>(); updated.add(lines.get(0));
            for (int i=1;i<lines.size();i++) updated.add(lines.get(i).replaceAll(",0$",",1"));
            Files.write(Paths.get(ALERTS_FILE), updated);
            System.out.println(C + "\nAll alerts marked as read." + R);
        } catch (IOException e) { System.out.println(RED + "Error reading alerts." + R); }
    }

    static void showUnreadCount() {
        try {
            long count = Files.readAllLines(Paths.get(ALERTS_FILE)).stream().skip(1).filter(l -> l.endsWith(",0")).count();
            if (count > 0) System.out.println(RED + BOLD + "You have " + count + " unread alert(s)!" + R);
        } catch (IOException ignored) {}
    }

    // ── HELPERS ─────────────────────────────────────────────────────────────
    static List<String[]> readCsv(String file) {
        try {
            return Files.readAllLines(Paths.get(file)).stream().skip(1)
                .filter(l -> !l.isBlank()).map(l -> l.split(",")).collect(Collectors.toList());
        } catch (IOException e) { return new ArrayList<>(); }
    }

    static void appendLine(String file, String line) {
        try { Files.writeString(Paths.get(file), line, StandardOpenOption.APPEND); }
        catch (IOException e) { System.err.println("Write error: " + e.getMessage()); }
    }

    static void mainMenu() {
        long unread = 0;
        try { unread = Files.readAllLines(Paths.get(ALERTS_FILE)).stream().skip(1).filter(l->l.endsWith(",0")).count(); }
        catch (IOException ignored) {}
        String badge = unread > 0 ? RED + " [" + unread + " ALERT!]" + R : "";
        System.out.println("\n" + BOLD + B + "MAIN MENU" + R + badge);
        System.out.println("  1.  Add Expense");
        System.out.println("  2.  View All Expenses");
        System.out.println("  3.  Monthly Summary");
        System.out.println("  4.  Manage Budgets");
        System.out.println("  5.  View Alerts" + badge);
        System.out.println("  6.  Delete Expense");
        System.out.println("  7.  Exit");
        System.out.println(B + "-".repeat(17) + R);
    }

    static void banner() {
        System.out.println(C + BOLD);
        System.out.println("==========================================");
        System.out.println("   EXPENSE TRACKER WITH ALERTS");
        System.out.println("   Java + File Persistence (CSV/Data)");
        System.out.println("==========================================" + R);
    }

    static int readInt(String p) {
        while(true) { System.out.print(p);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (Exception e) { System.out.println(RED+"Enter a number."+R); } }
    }
    static double readDouble(String p) {
        while(true) { System.out.print(p);
            try { return Double.parseDouble(sc.nextLine().trim()); }
            catch (Exception e) { System.out.println(RED+"Enter a valid amount."+R); } }
    }
    static String readString(String p) { System.out.print(p); return sc.nextLine(); }
    static String trunc(String s, int max) { if(s==null||s.isEmpty())return""; return s.length()>max?s.substring(0,max-1)+"..":s; }
    static void clearScreen() { System.out.print("\033[H\033[2J"); System.out.flush(); }
}
