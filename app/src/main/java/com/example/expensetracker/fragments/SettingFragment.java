package com.example.expensetracker.fragments;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.Login_Register.login;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.TransactionModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingFragment extends Fragment {

    TextView txtUsername,topusername, txtVersion, txtContact, btnEditProfile;
    Switch switchFingerprint, switchDarkMode, switchNotifications;
    Button btnExportPdf, btnClearExpenses, btnLogout;

    DatabaseHelper db;
    SharedPreferences spUser;      // "user"
    SharedPreferences spSettings;  // "settings"

    String currentUsername = "";

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        db = new DatabaseHelper(requireContext());
        spUser = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        spSettings = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        currentUsername = spUser.getString("username", "");

        // ---- FIND VIEWS ----
        txtUsername = view.findViewById(R.id.txtUsername);
        topusername = view.findViewById(R.id.topuser);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        switchFingerprint = view.findViewById(R.id.switchFingerprint);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        btnExportPdf = view.findViewById(R.id.btnExportPdf);
        btnClearExpenses = view.findViewById(R.id.btnClearExpenses);
        btnLogout = view.findViewById(R.id.btnLogout);
        txtVersion = view.findViewById(R.id.txtVersion);
        txtContact = view.findViewById(R.id.txtContact);

        TextView txtTotalIncome = view.findViewById(R.id.txtTotalIncome);
        TextView txtTotalExpense = view.findViewById(R.id.txtTotalExpense);
        TextView txtTotalEntries = view.findViewById(R.id.txtTotalEntries);

        double income = db.getTotalIncome();
        double expense = db.getTotalExpense();
        int entries = db.getTotalEntries();

        txtTotalIncome.setText("Total Income: ₹" + income);
        txtTotalExpense.setText("Total Expense: ₹" + expense);
        txtTotalEntries.setText("Total Entries: " + entries);

        // ---- SET INITIAL VALUES ----
        txtUsername.setText(currentUsername.isEmpty() ? "Guest" : currentUsername);
        topusername.setText("Welcome, " + (currentUsername.isEmpty() ? "Guest" : currentUsername));
        topusername.setTypeface(Typeface.DEFAULT_BOLD);
        txtVersion.setText("Version " + getAppVersion());
        txtContact.setText("Developer: Bhargav Ghoniya");

        switchFingerprint.setChecked(spSettings.getBoolean("fingerprint_enabled", false));
        switchDarkMode.setChecked(spSettings.getBoolean("dark_mode", false));
        switchNotifications.setChecked(spSettings.getBoolean("notifications_enabled", true));

        // Apply current dark mode
        AppCompatDelegate.setDefaultNightMode(
                switchDarkMode.isChecked() ?
                        AppCompatDelegate.MODE_NIGHT_YES :
                        AppCompatDelegate.MODE_NIGHT_NO
        );

        // ---- CLICK HANDLERS ----

        // Edit profile (for now just info)
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());



        // Change PIN dialog
        View rowChangePin = view.findViewById(R.id.rowChangePin);
        rowChangePin.setOnClickListener(v -> showChangePinDialog());

        // Fingerprint toggle
        switchFingerprint.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spSettings.edit().putBoolean("fingerprint_enabled", isChecked).apply();
            Toast.makeText(requireContext(),
                    isChecked ? "Fingerprint unlock enabled" : "Fingerprint unlock disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spSettings.edit().putBoolean("dark_mode", isChecked).apply();
            restartActivityForTheme();  // your existing method
        });



        // Notifications toggle
        // Load saved state
        boolean isNoti = spSettings.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(isNoti);

// Handle toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spSettings.edit().putBoolean("notifications_enabled", isChecked).apply();

            Toast.makeText(requireContext(),
                    isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT).show();
        });

// Create notification channel (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "expense_channel",
                    "Expense Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager =
                    requireContext().getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }


        // Export PDF
        btnExportPdf.setOnClickListener(v -> exportPdfForUser());

        // Clear expenses
        btnClearExpenses.setOnClickListener(v -> confirmClearExpenses());

        // Logout
        btnLogout.setOnClickListener(v -> logoutUser());

        return view;
    }
    private void restartActivityForTheme() {
        requireActivity().runOnUiThread(() -> {
            Intent i = new Intent(requireContext(), MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            requireActivity().finish();
        });
    }

    // ------------------------ CHANGE PIN ------------------------
    private void showEditProfileDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        EditText edtNewUsername = view.findViewById(R.id.edtNewUsername);

        SharedPreferences sp = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        String oldUsername = sp.getString("username", "");

        edtNewUsername.setText(oldUsername);

        builder.setView(view);

        builder.setPositiveButton("Save", (dialog, which) -> {

            String newUsername = edtNewUsername.getText().toString().trim();

            if (newUsername.isEmpty()) {
                Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseHelper db = new DatabaseHelper(requireContext());

            // Check if username already taken
            if (db.isUserExists(newUsername)) {
                Toast.makeText(requireContext(), "Username already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Update USER_TABLE
            db.updateUsername(oldUsername, newUsername);

            // 2. Update EXPENSE_TABLE for all old entries
            db.updateExpenseUsername(oldUsername, newUsername);

            // 3. Update SharedPreferences
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("username", newUsername);
            editor.apply();

            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();

            // refresh UI
            txtUsername.setText(newUsername);
            topusername.setText(newUsername);
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void showChangePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change PIN");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_pin, null);
        EditText edtOld = dialogView.findViewById(R.id.edtOldPin);
        EditText edtNew = dialogView.findViewById(R.id.edtNewPin);
        EditText edtConfirm = dialogView.findViewById(R.id.edtConfirmPin);

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (d, which) -> {

            String oldPin = edtOld.getText().toString().trim();
            String newPin = edtNew.getText().toString().trim();
            String conPin = edtConfirm.getText().toString().trim();

            String savedPin = spUser.getString("password", "");

            if (oldPin.isEmpty() || newPin.isEmpty() || conPin.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!oldPin.equals(savedPin)) {
                Toast.makeText(requireContext(), "Old PIN incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPin.equals(conPin)) {
                Toast.makeText(requireContext(), "New PINs not matching", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update in DB + SharedPreferences
            boolean ok = db.updateUserPassword(currentUsername, newPin);
            if (ok) {
                spUser.edit().putString("password", newPin).apply();
                Toast.makeText(requireContext(), "PIN updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to update PIN", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ------------------------ EXPORT PDF ------------------------

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void exportPdfForUser() {

        if (currentUsername.isEmpty()) {
            Toast.makeText(requireContext(), "No logged in user", Toast.LENGTH_SHORT).show();
            return;
        }

        List<TransactionModel> list = db.getAllExpensesForCurrentUser();
        if (list == null || list.isEmpty()) {
            Toast.makeText(requireContext(), "No expenses to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            int pageWidth = 595;
            int pageHeight = 842;

            PdfDocument pdf = new PdfDocument();
            Paint paint = new Paint();
            Paint linePaint = new Paint();
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(1);

            int x = 40;
            int y = 70;
            int tableLeft = 30;
            int tableRight = pageWidth - 30;

            // ---------- PAGE 1 ----------
            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // TITLE
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(20);
            canvas.drawText("Expense Report", pageWidth / 2f - 70, y, paint);
            y += 30;

            // Sub text
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setTextSize(12);
            canvas.drawText("User: " + currentUsername, x, y, paint);
            y += 15;
            canvas.drawText("Generated: " + new SimpleDateFormat("dd MMM yyyy").format(new Date()), x, y, paint);
            y += 25;

            // TABLE HEADER
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(12);

            int rowHeight = 25;
            int currentY = y;

            // Draw header background
            Paint headerBg = new Paint();
            headerBg.setColor(Color.LTGRAY);
            canvas.drawRect(tableLeft, currentY - 18, tableRight, currentY + 8, headerBg);

            // Header text
            canvas.drawText("DATE", tableLeft + 10, currentY, paint);
            canvas.drawText("CATEGORY", tableLeft + 110, currentY, paint);
            canvas.drawText("NOTE", tableLeft + 250, currentY, paint);
            canvas.drawText("AMOUNT", tableRight - 80, currentY, paint);

            currentY += rowHeight;

            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setTextSize(11);

            boolean alt = false;

            for (TransactionModel t : list) {

                if (currentY > pageHeight - 60) {
                    pdf.finishPage(page);

                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pdf.getPages().size() + 1).create();
                    page = pdf.startPage(pageInfo);
                    canvas = page.getCanvas();
                    currentY = 70;
                }

                // Alternate row background
                if (alt) {
                    Paint bg = new Paint();
                    bg.setColor(Color.parseColor("#F5F5F5"));
                    canvas.drawRect(tableLeft, currentY - 18, tableRight, currentY + 8, bg);
                }
                alt = !alt;

                String date = new SimpleDateFormat("dd/MM/yyyy").format(t.getDate());

                canvas.drawText(date, tableLeft + 10, currentY, paint);
                canvas.drawText(trimText(t.getCategory(), 12), tableLeft + 110, currentY, paint);
                canvas.drawText(trimText(t.getNote(), 20), tableLeft + 250, currentY, paint);

                String amt = String.format(Locale.getDefault(), "%.2f", t.getAmount());
                canvas.drawText(amt, tableRight - 80, currentY, paint);

                currentY += rowHeight;
            }

            // ---------- SUMMARY ----------
            currentY += 20;
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(13);

            double totalIncome = db.getTotalIncome();
            double totalExpense = db.getTotalExpense();

            canvas.drawText("Total Income:  " + totalIncome, x, currentY, paint);
            currentY += 18;
            canvas.drawText("Total Expense: " + totalExpense, x, currentY, paint);

            pdf.finishPage(page);

            // SAVE
            String fileName = "Expense_Report_" + currentUsername + "_" + System.currentTimeMillis() + ".pdf";
            savePdfToDownloads(pdf, fileName);
            pdf.close();
            Toast.makeText(requireContext(), "Saved to Downloads folder", Toast.LENGTH_LONG).show();


        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "PDF export failed", Toast.LENGTH_SHORT).show();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void savePdfToDownloads(PdfDocument pdf, String fileName) throws IOException {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = requireContext().getContentResolver()
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

        OutputStream out = requireContext().getContentResolver().openOutputStream(uri);
        pdf.writeTo(out);
        out.close();
    }

    // Utility to limit long text
    private String trimText(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 2) + "…" : s;
    }


    private String safeLen(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    // ------------------------ CLEAR EXPENSES ------------------------

    private void confirmClearExpenses() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Expenses?")
                .setMessage("This will delete all expenses of user: " + currentUsername)
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteAllExpensesForCurrentUser();
                    Toast.makeText(requireContext(), "Expenses cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------ LOGOUT ------------------------

    private void logoutUser() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout?")
                .setMessage("Do you really want to logout?")
                .setPositiveButton("Yes", (d, w) -> {
                    // Keep username, clear password, mark logged_in false
                    SharedPreferences.Editor ed = spUser.edit();
                    ed.remove("password");
                    ed.putBoolean("logged_in", false);
                    ed.apply();

                    Intent i = new Intent(requireContext(), login.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getAppVersion() {
        try {
            return requireContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }
}
