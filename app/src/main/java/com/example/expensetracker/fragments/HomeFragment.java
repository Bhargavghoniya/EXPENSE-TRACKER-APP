package com.example.expensetracker.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.CircleListView;
import com.example.expensetracker.CricleListViewAdapter;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;
import com.example.expensetracker.SearchResultActivity;
import com.example.expensetracker.TransactionModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment implements DatePickerDialog.OnDateSetListener {

    DatePickerDialog datePickerDialog;
    Calendar calendar;
    int mYear, mMonth, mDay;

    SearchView simpleSearchView;
    TextView textViewInterval, txtViewItem, categoryName;
    TextView txtExpense, txtIncome, txtBalance;   // ⬅ summary rows

    List<TransactionModel> categoryItemList = new ArrayList<>();
    List<TransactionModel> populateList = new ArrayList<>();
    DatabaseHelper databaseHelper;
    Date selectedDate;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    String selectedCat = null;

    FrameLayout relativeLayout;
    CircleListView circleListView;
    CricleListViewAdapter cricleListViewAdapter;
    ImageView categoryImg, imgViewTitle;
    TransactionModel categoryItem;
    PieChart pieChart;
    ArrayList<PieEntry> pieCategories;
    ArrayList<TransactionModel> pieList = new ArrayList<>();
    int[] colors;
    NumberFormat formatter = new DecimalFormat("#0.00");

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        imgViewTitle = view.findViewById(R.id.imgViewTitle);
        if (getActivity() != null) {
            ((androidx.appcompat.app.AppCompatActivity) getActivity())
                    .setSupportActionBar(toolbar);
            ((androidx.appcompat.app.AppCompatActivity) getActivity())
                    .getSupportActionBar().setTitle("");
        }

        // Views
        textViewInterval = view.findViewById(R.id.txtViewInterval);
        txtViewItem      = view.findViewById(R.id.txtViewItem);
        pieChart         = view.findViewById(R.id.pie);
        relativeLayout   = view.findViewById(R.id.relLayoutHome);
        circleListView   = view.findViewById(R.id.circle_list_view);

        // Summary rows (below pie)
        txtExpense = view.findViewById(R.id.txtExpense);
        txtIncome  = view.findViewById(R.id.txtIncome);
        txtBalance = view.findViewById(R.id.txtBalance);

        // Get saved date from MainActivity
        selectedDate = ((MainActivity) requireActivity()).sharedSelectedDate;
        if (selectedDate == null) {
            selectedDate = new Date();
            ((MainActivity) requireActivity()).sharedSelectedDate = selectedDate;
        }

        // Init calendar from selected date
        calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        mYear = calendar.get(Calendar.YEAR);
        mMonth = calendar.get(Calendar.MONTH);
        mDay = calendar.get(Calendar.DAY_OF_MONTH);

        updateIntervalText();

        databaseHelper = new DatabaseHelper(requireContext());
        addData();

        // Load data for selected date
        populateList = databaseHelper.getDataByDate(selectedDate);
        Log.d("HomeFragment", calendar.getTime().toString());

        initCircleList();
        pieList = databaseHelper.Pie(calendar.getTime());
        drawPie(pieList);

        // Set summary below circle
        getSummary(populateList, selectedCat);

        // Click date to change
        textViewInterval.setOnClickListener(v -> showCalendar());





        return view;
    }

    // region Circle List
    private void initCircleList() {
        cricleListViewAdapter = new CricleListViewAdapter(circleListView) {
            @Override
            public View getView(int position) {
                View view = LayoutInflater.from(requireContext())
                        .inflate(R.layout.layout_categoryitem, null);

                categoryName = view.findViewById(R.id.txtViewCategory);
                categoryImg  = view.findViewById(R.id.imgViewCategory);
                categoryItem = categoryItemList.get(position);

                categoryName.setText(categoryItem.getCategory());
                categoryImg.setImageResource(categoryItem.getPic());

                // Press on icon → show popup in center
                view.setOnTouchListener((v, motionEvent) -> {
                    for (TransactionModel record : populateList) {
                        if (record.getCategory()
                                .equals(categoryItemList.get(position).getCategory())) {

                            txtViewItem.setText(
                                    String.format("%s\n%s",
                                            record.getCategory(),
                                            formatter.format(record.getAmount()))
                            );

                            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                                txtViewItem.setVisibility(View.GONE);
                            } else {
                                txtViewItem.setVisibility(View.VISIBLE);
                                txtViewItem.bringToFront();
                            }
                        }
                    }
                    return true;
                });

                // Highlight category if it has value
                for (TransactionModel record : populateList) {
                    if (record.getCategory().equals(categoryItemList.get(position).getCategory())
                            && record.getAmount() != 0) {
                        categoryImg.setBackgroundResource(R.drawable.category_bg);
                    }
                }

                return view;
            }

            @Override
            public int getCount() {
                return categoryItemList.size();
            }
        };

        circleListView.setAdapter(cricleListViewAdapter);
        cricleListViewAdapter.setPosition(0);
    }
    // endregion

    // region Pie chart
    private void drawPie(ArrayList<TransactionModel> pieList) {
        pieCategories = new ArrayList<>();
        for (int i = 0; i < pieList.size(); i++) {
            String category = pieList.get(i).getCategory();
            int amount = (int) pieList.get(i).getAmount();
            pieCategories.add(new PieEntry(amount, category));
        }
        colors = requireContext().getResources().getIntArray(R.array.pieColors);
        PieDataSet pieDataSet = new PieDataSet(pieCategories, "Categories");
        pieDataSet.setColors(colors);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueTextSize(10f);
        pieDataSet.setHighlightEnabled(true);
        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setUsePercentValues(true);
        pieChart.clear();
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.animate();
        pieChart.getLegend().setEnabled(false);
        pieChart.setEntryLabelTypeface(
                Typeface.create("sans-serif-smallcaps", Typeface.NORMAL));
    }
    // endregion

    // region Date picker
    private void showCalendar() {
        datePickerDialog = new DatePickerDialog(
                requireContext(),
                this,
                mYear,
                mMonth,
                mDay
        );
        datePickerDialog.show();
    }

    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
        mYear = year;
        mMonth = month;
        mDay = day;

        calendar.set(Calendar.YEAR, mYear);
        calendar.set(Calendar.MONTH, mMonth);
        calendar.set(Calendar.DAY_OF_MONTH, mDay);
        selectedDate = calendar.getTime();
        ((MainActivity) requireActivity()).sharedSelectedDate = selectedDate;

        updateIntervalText();

        populateList = databaseHelper.getDataByDate(selectedDate);
        getSummary(populateList, selectedCat);

        pieList = databaseHelper.Pie(selectedDate);
        initCircleList();
        drawPie(pieList);
    }

    private void updateIntervalText() {
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMMM yyyy");
        textViewInterval.setText(fmt.format(selectedDate).toUpperCase());
    }
    // endregion

    // region Toolbar menu (Search)

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setQueryHint("Search category or notes...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                Intent i = new Intent(getActivity(), SearchResultActivity.class);
                i.putExtra("keyword", query);
                startActivity(i);

                searchView.clearFocus();
                searchMenuItem.collapseActionView();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        // ⭐ Hide logo when search opens, show when search closes
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                ImageView logo = getView().findViewById(R.id.imgViewTitle);
                logo.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                ImageView logo = getView().findViewById(R.id.imgViewTitle);
                logo.setVisibility(View.VISIBLE);
                return true;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }


    // endregion

    // region Summary (below pie)
    private void getSummary(List<TransactionModel> list, String selectedCat) {
        double expense = 0;
        double income = 0;

        for (TransactionModel record : list) {
            if ("expense".equals(record.getGroup())) {
                expense += Math.abs(record.getAmount());
            } else if ("income".equals(record.getGroup())) {
                income += record.getAmount();
            }
        }

        double balance = income - expense;

        txtExpense.setText("Expense: -" + formatter.format(expense));
        txtIncome.setText("Income: " + formatter.format(income));
        txtBalance.setText("Balance: " + formatter.format(balance));
    }
    // endregion

    // region Categories
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
        categoryItemList.add(new TransactionModel("Deposits", R.drawable.deposit1));
        categoryItemList.add(new TransactionModel("Salary", R.drawable.salary));
    }
    // endregion
}
