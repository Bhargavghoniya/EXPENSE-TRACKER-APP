package com.example.expensetracker.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.GridViewAdapter;
import com.example.expensetracker.R;
import com.example.expensetracker.TransactionModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddIncomeFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    List<TransactionModel> categoryItemList = new ArrayList<>();
    Button btnChooseCategory;
    EditText editTxtAmount, editTxtNotes;
    GridView gridViewCategory;
    TextView txtViewDate;
    Calendar calendar;
    ImageView imgViewCalendar;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_income, container, false);

        // UI components
        btnChooseCategory = view.findViewById(R.id.btnChooseCategory);
        editTxtAmount = view.findViewById(R.id.editTxtAmount);
        editTxtNotes = view.findViewById(R.id.editTxtNotes);
        gridViewCategory = view.findViewById(R.id.gridViewCategories);
        txtViewDate = view.findViewById(R.id.txtViewDate);
        imgViewCalendar = view.findViewById(R.id.imgViewCalendar);

        // Add category icons
        addData();

        // Date setup
        String today = dateFormat.format(new Date());
        txtViewDate.setText(today);
        calendar = Calendar.getInstance();

        // Disable autofill
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            editTxtAmount.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            editTxtAmount.setAutofillHints(new String[]{""});
        }

        // Date click
        txtViewDate.setOnClickListener(v -> {
            showCalendar();
            hideKeyboard(v);
        });

        imgViewCalendar.setOnClickListener(v -> {
            showCalendar();
            hideKeyboard(v);
        });

        // Category selection
        btnChooseCategory.setOnClickListener(v -> {

            hideKeyboard(v);
            gridViewCategory.setVisibility(View.VISIBLE);

            GridViewAdapter adapter = new GridViewAdapter(requireContext(),
                    R.layout.layout_categoryitem, categoryItemList);
            gridViewCategory.setAdapter(adapter);

            gridViewCategory.setOnItemClickListener((adapterView, itemView, pos, id) -> {

                DatabaseHelper DB = new DatabaseHelper(requireContext());
                TransactionModel model = new TransactionModel();

                try {
                    model.setAmount(Double.parseDouble(editTxtAmount.getText().toString()));
                    model.setNote(editTxtNotes.getText().toString());
                    model.setCategory(categoryItemList.get(pos).getCategory());
                    model.setGroup("income");
                    model.setDate(calendar.getTime());
                } catch (Exception ex) {
                    Toast.makeText(requireContext(), "Enter valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean inserted = DB.addData(model);

                if (inserted) {
                    Toast.makeText(requireContext(), "Income Added", Toast.LENGTH_SHORT).show();

                    // Redirect to HomeFragment
                    requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_container, new HomeFragment())
                            .commit();

                } else {
                    Toast.makeText(requireContext(), "Failed to Insert", Toast.LENGTH_SHORT).show();
                }
            });
        });

        return view;
    }

    // Hide default keyboard
    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager)
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void addData() {
        categoryItemList.add(new TransactionModel("Deposits", R.drawable.deposit1));
        categoryItemList.add(new TransactionModel("Salary", R.drawable.salary));
    }

    public void showCalendar() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int y, int m, int d) {
        calendar.set(y, m, d);
        txtViewDate.setText(dateFormat.format(calendar.getTime()));
    }
}
