# Expense Tracker with Alerts
## Java Project | No Maven Required

### How to Run
1. Double-click `run.bat`
2. That's it! Auto-compiles and launches.

### Requirements
- Java 17+ only (https://adoptium.net)
- No Maven, no extra JARs needed

### Features
- Add / View / Delete expenses
- Set monthly budgets per category
- Real-time alerts at 80% (warning) and 100% (exceeded)
- Monthly summary with budget vs spent
- Data saved in `data/` folder as CSV files

### Resume Points
- Built a Java console application with full CRUD operations
- Implemented file-based data persistence using Java NIO (CSV format)
- Designed real-time budget alert engine with threshold-based notifications (80%/100%)
- Applied OOP principles and clean separation of concerns
- Used Java Streams API for data filtering and aggregation

### Project Structure
```
ExpenseTracker/
  ExpenseTracker.java   <- All logic in one file
  run.bat               <- Double-click to launch
  data/
    expenses.csv        <- Auto-created on first run
    budgets.csv
    alerts.csv
```
