package com.example.expensetracker.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.R;
import com.example.expensetracker.SearchListAdapter;
import com.example.expensetracker.TransactionModel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BalanceFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    ListView listView;
    TextView txtDate, txtSummary;
    Button btnDay, btnMonth, btnYear;

    DatabaseHelper db;
    List<TransactionModel> filteredList;

    Date selectedDate;
    Calendar calendar = Calendar.getInstance();

    SimpleDateFormat fmt = new SimpleDateFormat("dd MMMM yyyy");

    NumberFormat formatter = new DecimalFormat("#0.00");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_balance, container, false);

        listView = view.findViewById(R.id.listViewResults);
        txtDate = view.findViewById(R.id.txtDate);
        txtSummary = view.findViewById(R.id.txtSummary);

        btnDay = view.findViewById(R.id.btnFilterDay);
        btnMonth = view.findViewById(R.id.btnFilterMonth);
        btnYear = view.findViewById(R.id.btnFilterYear);

        db = new DatabaseHelper(requireContext());

        // GET DATE FROM HOME
        if (getArguments() != null) {
            selectedDate = (Date) getArguments().getSerializable("selectedDate");
        }
        if (selectedDate == null) selectedDate = new Date();

        calendar.setTime(selectedDate);
        txtDate.setText(fmt.format(selectedDate).toUpperCase());

        loadDay(); // FIRST LOAD WITH ANIMATION

        btnDay.setOnClickListener(v -> loadDay());
        btnMonth.setOnClickListener(v -> loadMonth());
        btnYear.setOnClickListener(v -> loadYear());

        return view;
    }

    // ---------------- FILTER FUNCTIONS ----------------

    private void loadDay() {
        filteredList = db.getDataByDate(selectedDate);

        txtDate.setText(fmt.format(selectedDate).toUpperCase());
        setList(true);   // animation ON
        updateSummary(filteredList);
    }

    private void loadMonth() {

        String month = (calendar.get(Calendar.MONTH) + 1 < 10)
                ? "0" + (calendar.get(Calendar.MONTH) + 1)
                : "" + (calendar.get(Calendar.MONTH) + 1);

        String year = String.valueOf(calendar.get(Calendar.YEAR));

        filteredList = db.getDataByMonthYear(month, year);

        txtDate.setText("MONTH: " + month + " / " + year);

        setList(true);  // animation ON
        updateSummary(filteredList);
    }

    private void loadYear() {
        String year = String.valueOf(calendar.get(Calendar.YEAR));

        filteredList = db.getDataByYear(year);

        txtDate.setText("YEAR: " + year);

        setList(true);  // animation ON
        updateSummary(filteredList);
    }

    // ---------------- SUMMARY ----------------

    private void updateSummary(List<TransactionModel> list) {
        double expense = 0;
        double income = 0;

        for (TransactionModel item : list) {
            if ("expense".equals(item.getGroup())) {
                expense += Math.abs(item.getAmount());
            }
            if ("income".equals(item.getGroup())) {
                income += item.getAmount();
            }
        }

        double balance = income - expense;

        // Colors from theme
        int colorExpense = ContextCompat.getColor(requireContext(), R.color.summaryExpense);
        int colorIncome = ContextCompat.getColor(requireContext(), R.color.summaryIncome);
        int colorBalance = ContextCompat.getColor(requireContext(), R.color.summaryBalance);

        SpannableStringBuilder sb = new SpannableStringBuilder();

        // Expense
        SpannableString s1 = new SpannableString("Expense: -" + formatter.format(expense) + "\n");
        s1.setSpan(new ForegroundColorSpan(colorExpense), 0, s1.length(), 0);
        sb.append(s1);

        // Income
        SpannableString s2 = new SpannableString("Income: " + formatter.format(income) + "\n");
        s2.setSpan(new ForegroundColorSpan(colorIncome), 0, s2.length(), 0);
        sb.append(s2);

        // Balance
        SpannableString s3 = new SpannableString("Balance: " + formatter.format(balance));
        s3.setSpan(new ForegroundColorSpan(colorBalance), 0, s3.length(), 0);
        sb.append(s3);

        txtSummary.setText(sb);
    }


    // ---------------- LIST VIEW + ACTION HANDLING ----------------

    private void setList(boolean animate) {
        SearchListAdapter adapter =
                new SearchListAdapter(requireContext(), filteredList, new SearchListAdapter.OnActionListener() {
                    @Override
                    public void onEdit(TransactionModel model) {
                        showEditDialog(model);
                    }

                    @Override
                    public void onDelete(TransactionModel model) {
                        askDelete(model);
                    }
                });

        listView.setAdapter(adapter);

        if (animate) {
            listView.setLayoutAnimation(
                    AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_fall_down)
            );
        } else {
            listView.setLayoutAnimation(null);
        }
    }

    // ---------------- DELETE ENTRY ----------------

    private void askDelete(TransactionModel model) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Entry?")
                .setMessage("Category: " + model.getCategory() + "\nAmount: " + model.getAmount())
                .setPositiveButton("Delete", (d, w) -> {

                    db.deleteData(model);
                    filteredList.remove(model);

                    setList(false); // no animation (smooth)
                    updateSummary(filteredList);

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ---------------- EDIT ENTRY ----------------

    private void showEditDialog(TransactionModel model) {

        View view = getLayoutInflater().inflate(R.layout.dialog_edit, null);

        EditText edtCategory = view.findViewById(R.id.edtCategory);
        EditText edtAmount = view.findViewById(R.id.edtAmount);
        EditText edtNote = view.findViewById(R.id.edtNote);

        edtCategory.setText(model.getCategory());
        edtAmount.setText(String.valueOf(model.getAmount()));
        edtNote.setText(model.getNote());

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Edit Transaction")
                .setView(view)
                .setPositiveButton("Save", (d, w) -> {

                    model.setCategory(edtCategory.getText().toString());
                    model.setAmount(Double.parseDouble(edtAmount.getText().toString()));
                    model.setNote(edtNote.getText().toString());

                    db.updateData(model);

                    setList(false); // NO animation
                    updateSummary(filteredList);

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {}
}
