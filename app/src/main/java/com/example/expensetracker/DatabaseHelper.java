package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String EXPENSE_TABLE = "EXPENSE_TABLE";
    public static final String COLUMN_ID = "COLUMN_ID";
    public static final String COLUMN_CATEGORY = "COLUMN_CATEGORY";
    public static final String COLUMN_NOTE = "COLUMN_NOTE";
    public static final String COLUMN_AMOUNT = "COLUMN_AMOUNT";
    public static final String COLUMN_DATE = "COLUMN_DATE";
    public static final String COLUMN_GROUP = "COLUMN_GROUP";
    public static final String COLUMN_USERNAME = "COLUMN_USERNAME";  // 🔥 NEW

    public static final String USER_TABLE = "USER_TABLE";
    public static final String USER_ID = "ID";
    public static final String USERNAME = "USERNAME";
    public static final String PASSWORD = "PASSWORD";

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Context context;

    public DatabaseHelper(@Nullable Context context) {
        super(context, "data.db", null, 3);  // 🔥 bump version
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // 🔥 NEW EXPENSE TABLE WITH USERNAME
        db.execSQL("CREATE TABLE " + EXPENSE_TABLE + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_NOTE + " TEXT, " +
                COLUMN_GROUP + " TEXT, " +
                COLUMN_AMOUNT + " REAL, " +
                COLUMN_USERNAME + " TEXT)");

        // USER TABLE
        db.execSQL("CREATE TABLE " + USER_TABLE + "(" +
                USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                USERNAME + " TEXT UNIQUE, " +
                PASSWORD + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // User table already handled
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + USER_TABLE + "(" +
                    USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    USERNAME + " TEXT UNIQUE, " +
                    PASSWORD + " TEXT)");
        }

        // 🔥 DROP and RECREATE expense table because app uninstall clean
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS " + EXPENSE_TABLE);
            onCreate(db);
        }
    }

    // ------------------ AUTH ------------------

    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(USERNAME, username);
        cv.put(PASSWORD, password);
        return db.insert(USER_TABLE, null, cv) != -1;
    }

    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + USER_TABLE + " WHERE " + USERNAME + "=? AND " + PASSWORD + "=?",
                new String[]{username, password});

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public boolean isUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + USER_TABLE + " WHERE " + USERNAME + "=?",
                new String[]{username});

        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // ------------------ EXPENSE INSERT ------------------
    private String getCurrentUser() {
        SharedPreferences sp = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        return sp.getString("username", "");
    }

    public boolean updateUserPassword(String username, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(PASSWORD, newPassword);
        return db.update(USER_TABLE, cv, USERNAME + "=?", new String[]{username}) > 0;
    }
    public boolean addData(TransactionModel model) {
        try {
            SharedPreferences sp = context.getSharedPreferences("user", Context.MODE_PRIVATE);
            String loggedUser = sp.getString("username", "unknown");

            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();

            cv.put(COLUMN_DATE, dateFormat.format(model.getDate()));
            cv.put(COLUMN_CATEGORY, model.getCategory());
            cv.put(COLUMN_AMOUNT, model.getAmount());
            cv.put(COLUMN_NOTE, model.getNote());
            cv.put(COLUMN_GROUP, model.getGroup());
            cv.put(COLUMN_USERNAME, loggedUser);   // 🔥 assign user

            return db.insert(EXPENSE_TABLE, null, cv) > 0;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // ------------------ GET LOGGED USER ------------------

    private String getLoggedUser() {
        SharedPreferences sp = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        return sp.getString("username", "");
    }

    // ------------------ SEARCH ------------------

    public List<TransactionModel> getSearchedData(String item) {

        List<TransactionModel> returnList = new ArrayList<>();
        String user = getLoggedUser();

        String query = "SELECT * FROM " + EXPENSE_TABLE +
                " WHERE " + COLUMN_USERNAME + "=? AND (" +
                " UPPER(" + COLUMN_CATEGORY + ")=UPPER(?) OR " +
                " UPPER(" + COLUMN_NOTE + ") LIKE UPPER(?) )";

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query,
                new String[]{user, item, "%" + item + "%"}
        );

        if (cursor.moveToFirst()) {
            do {
                returnList.add(getRecordFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        return returnList;
    }

    // ------------------ DAILY FILTER ------------------

    public ArrayList<TransactionModel> getDataByDate(Date today) {

        ArrayList<TransactionModel> returnList = new ArrayList<>();
        String user = getLoggedUser();

        String query = "SELECT * FROM " + EXPENSE_TABLE +
                " WHERE " + COLUMN_DATE + "=? AND " +
                COLUMN_USERNAME + "=?" +
                " ORDER BY " + COLUMN_DATE;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query,
                new String[]{dateFormat.format(today), user});

        if (cursor.moveToFirst()) {
            do returnList.add(getRecordFromCursor(cursor));
            while (cursor.moveToNext());
        }

        return returnList;
    }

    // ------------------ YEAR FILTER ------------------

    public ArrayList<TransactionModel> getDataByYear(String year) {

        ArrayList<TransactionModel> list = new ArrayList<>();
        String user = getLoggedUser();

        String query = "SELECT * FROM " + EXPENSE_TABLE +
                " WHERE " + COLUMN_DATE + " LIKE ? AND " +
                COLUMN_USERNAME + "=? ORDER BY " + COLUMN_DATE;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query,
                new String[]{year + "%", user});

        if (cursor.moveToFirst()) {
            do list.add(getRecordFromCursor(cursor));
            while (cursor.moveToNext());
        }

        return list;
    }

    // ------------------ MONTH FILTER ------------------

    public ArrayList<TransactionModel> getDataByMonth(String month) {

        ArrayList<TransactionModel> list = new ArrayList<>();
        String user = getLoggedUser();

        String query = "SELECT * FROM " + EXPENSE_TABLE +
                " WHERE " + COLUMN_DATE + " LIKE ? AND " +
                COLUMN_USERNAME + "=? ORDER BY " + COLUMN_DATE;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query,
                new String[]{"_____" + month + "%", user});

        if (cursor.moveToFirst()) {
            do list.add(getRecordFromCursor(cursor));
            while (cursor.moveToNext());
        }

        return list;
    }

    // ------------------ MONTH + YEAR FILTER ------------------

    public List<TransactionModel> getDataByMonthYear(String month, String year) {

        List<TransactionModel> list = new ArrayList<>();
        String user = getLoggedUser();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + EXPENSE_TABLE +
                        " WHERE strftime('%m'," + COLUMN_DATE + ")=? AND " +
                        " strftime('%Y'," + COLUMN_DATE + ")=? AND " +
                        COLUMN_USERNAME + "=? ORDER BY " + COLUMN_DATE,
                new String[]{month, year, user}
        );

        if (cursor.moveToFirst()) {
            do list.add(getRecordFromCursor(cursor));
            while (cursor.moveToNext());
        }

        return list;
    }

    // ------------------ PIE CHART ------------------

    public ArrayList<TransactionModel> Pie(Date today) {

        ArrayList<TransactionModel> list = new ArrayList<>();
        String user = getLoggedUser();

        String query =
                "SELECT " + COLUMN_CATEGORY + ", SUM(abs(" + COLUMN_AMOUNT + "))" +
                        " FROM " + EXPENSE_TABLE +
                        " WHERE " + COLUMN_DATE + "=? AND " +
                        COLUMN_USERNAME + "=? AND " +
                        COLUMN_CATEGORY + " NOT LIKE '%salary%' AND " +
                        COLUMN_CATEGORY + " NOT LIKE '%deposit%' " +
                        " GROUP BY " + COLUMN_CATEGORY;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(query,
                new String[]{dateFormat.format(today), user});

        if (cursor.moveToFirst()) {
            do {
                list.add(new TransactionModel(
                        cursor.getString(0),
                        cursor.getDouble(1)
                ));
            } while (cursor.moveToNext());
        }

        return list;
    }

    // ------------------ UPDATE ------------------

    public boolean updateData(TransactionModel m) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_CATEGORY, m.getCategory());
        cv.put(COLUMN_NOTE, m.getNote());
        cv.put(COLUMN_GROUP, m.getGroup());
        cv.put(COLUMN_AMOUNT, m.getAmount());
        cv.put(COLUMN_DATE, new SimpleDateFormat("yyyy-MM-dd").format(m.getDate()));
        // keep same user

        return db.update(EXPENSE_TABLE, cv, COLUMN_ID + "=? AND " + COLUMN_USERNAME + "=?",
                new String[]{String.valueOf(m.getId()), getCurrentUser()}) > 0;
    }

    // ------------------ DELETE ------------------

    public boolean deleteData(TransactionModel transactionModel) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(EXPENSE_TABLE,
                COLUMN_ID + "=? AND " + COLUMN_USERNAME + "=?",
                new String[]{String.valueOf(transactionModel.getId()), getCurrentUser()});
        return rows > 0;
    }

    // ------------------ CURSOR → MODEL ------------------

    private TransactionModel getRecordFromCursor(Cursor cursor) {

        try {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String dateStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
            String cat = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
            String note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE));
            String group = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUP));
            double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AMOUNT));

            Date date = dateFormat.parse(dateStr);

            return new TransactionModel(id, date, cat, note, group, amount);

        } catch (ParseException e) {
            e.printStackTrace();
            Toast.makeText(context, "date parse error", Toast.LENGTH_SHORT).show();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // All expenses for current user (for PDF)
    public List<TransactionModel> getAllExpensesForCurrentUser() {
        List<TransactionModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + EXPENSE_TABLE +
                        " WHERE " + COLUMN_USERNAME + " = ? " +
                        " ORDER BY " + COLUMN_DATE + " ASC",
                new String[]{getCurrentUser()}
        );

        if (cursor.moveToFirst()) {
            do {
                list.add(getRecordFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }



    public void deleteAllExpensesForCurrentUser() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(EXPENSE_TABLE, COLUMN_USERNAME + "=?", new String[]{getCurrentUser()});
    }

    public double getTotalIncome() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + EXPENSE_TABLE +
                        " WHERE " + COLUMN_AMOUNT + " > 0 AND " + COLUMN_USERNAME + "=?",
                new String[]{getCurrentUser()}
        );
        if (c.moveToFirst()) {
            double val = c.getDouble(0);
            c.close();
            return val;
        }
        c.close();
        return 0;
    }

    public double getTotalExpense() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COLUMN_AMOUNT + ") FROM " + EXPENSE_TABLE +
                        " WHERE " + COLUMN_AMOUNT + " < 0 AND " + COLUMN_USERNAME + "=?",
                new String[]{getCurrentUser()}
        );
        if (c.moveToFirst()) {
            double val = c.getDouble(0);
            c.close();
            return Math.abs(val);
        }
        c.close();
        return 0;
    }

    public int getTotalEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + EXPENSE_TABLE +
                        " WHERE " + COLUMN_USERNAME + "=?",
                new String[]{getCurrentUser()}
        );
        if (c.moveToFirst()) {
            int val = c.getInt(0);
            c.close();
            return val;
        }
        c.close();
        return 0;
    }
    public boolean updateUsername(String oldUsername, String newUsername) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(USERNAME, newUsername);
        return db.update(USER_TABLE, cv, USERNAME + "=?", new String[]{oldUsername}) > 0;
    }

    public void updateExpenseUsername(String oldUsername, String newUsername) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USERNAME, newUsername);
        db.update(EXPENSE_TABLE, cv, COLUMN_USERNAME + "=?", new String[]{oldUsername});
    }

}
