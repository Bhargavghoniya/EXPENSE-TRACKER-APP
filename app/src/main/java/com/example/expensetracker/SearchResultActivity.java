package com.example.expensetracker;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;

public class SearchResultActivity extends AppCompatActivity {

    ListView listViewResults;
    DatabaseHelper db;
    List<TransactionModel> resultList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbarSearch);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        listViewResults = findViewById(R.id.listViewResults);
        db = new DatabaseHelper(this);

        String query = getIntent().getStringExtra("keyword");

        if (query != null && !query.trim().isEmpty()) {
            resultList = db.getSearchedData(query.trim());
        }

        if (resultList == null || resultList.isEmpty()) {
            Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
        } else {
            SearchListAdapter adapter = new SearchListAdapter(resultList, this);
            listViewResults.setAdapter(adapter);
        }
    }
}
