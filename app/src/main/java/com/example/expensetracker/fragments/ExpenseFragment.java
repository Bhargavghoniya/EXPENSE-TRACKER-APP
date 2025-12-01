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

public class ExpenseFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

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

        View view = inflater.inflate(R.layout.fragment_expense, container, false);

        // Initialize UI components
        btnChooseCategory = view.findViewById(R.id.btnChooseCategory);
        editTxtAmount = view.findViewById(R.id.editTxtAmount);
        editTxtNotes = view.findViewById(R.id.editTxtNotes);
        gridViewCategory = view.findViewById(R.id.gridViewCategories);
        txtViewDate = view.findViewById(R.id.txtViewDate);
        imgViewCalendar = view.findViewById(R.id.imgViewCalendar);

        // Add category list
        addData();

        // Set current date
        String currentDate = dateFormat.format(new Date());
        txtViewDate.setText(currentDate);

        calendar = Calendar.getInstance();

        // INPUT TYPE — Default numeric keyboard
        editTxtAmount.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER |
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL |
                        android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        // Calendar click
        txtViewDate.setOnClickListener(v -> showCalendar());
        imgViewCalendar.setOnClickListener(v -> showCalendar());

        // CHOOSE CATEGORY
        btnChooseCategory.setOnClickListener(v -> {

            hideKeyboard();

            gridViewCategory.setVisibility(View.VISIBLE);
            GridViewAdapter adapter =
                    new GridViewAdapter(requireContext(),
                            R.layout.layout_categoryitem, categoryItemList);

            gridViewCategory.setAdapter(adapter);

            gridViewCategory.setOnItemClickListener((parent, itemView, i, id) -> {

                DatabaseHelper DB = new DatabaseHelper(requireContext());
                TransactionModel model = new TransactionModel();

                try {
                    double amount = Double.parseDouble(editTxtAmount.getText().toString());
                    model.setAmount(-amount); // Expense = negative
                    model.setNote(editTxtNotes.getText().toString());
                    model.setCategory(categoryItemList.get(i).getCategory());
                    model.setGroup("expense");
                    model.setDate(calendar.getTime());
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Amount cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                Boolean success = DB.addData(model);

                if (success) {
                    Toast.makeText(requireContext(), "Entry Inserted", Toast.LENGTH_SHORT).show();

                    // Go back to HomeFragment
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.main_container, new HomeFragment())
                            .commit();

                } else {
                    Toast.makeText(requireContext(), "Entry Not Inserted", Toast.LENGTH_SHORT).show();
                }

            });

        });

        return view;
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = requireActivity().getCurrentFocus();
        if (view != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        calendar.set(year, month, dayOfMonth);
        txtViewDate.setText(dateFormat.format(calendar.getTime()));
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

    private void addData() {
        categoryItemList.add(new TransactionModel("Car", R.drawable.car1));
        categoryItemList.add(new TransactionModel("Pet", R.drawable.pet1));
        categoryItemList.add(new TransactionModel("Grocery", R.drawable.grocery1));
        categoryItemList.add(new TransactionModel("Entertainment", R.drawable.entertainment));
        categoryItemList.add(new TransactionModel("Gift", R.drawable.gift));
        categoryItemList.add(new TransactionModel("Dine Out", R.drawable.food1));
        categoryItemList.add(new TransactionModel("Home", R.drawable.house));
        categoryItemList.add(new TransactionModel("Phone", R.drawable.smartphone));
        categoryItemList.add(new TransactionModel("Sports", R.drawable.sports));
        categoryItemList.add(new TransactionModel("Medical", R.drawable.medical));
        categoryItemList.add(new TransactionModel("Transportation", R.drawable.transportation));
        categoryItemList.add(new TransactionModel("Clothing", R.drawable.clothing1));
    }
}
