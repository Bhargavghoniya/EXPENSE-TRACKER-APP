package com.example.expensetracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;

public class SearchListAdapter extends BaseAdapter {

    List<TransactionModel> list;
    LayoutInflater inflater;
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

    Context context;

    public interface OnActionListener {
        void onEdit(TransactionModel model);
        void onDelete(TransactionModel model);
    }

    OnActionListener listener;
    public SearchListAdapter(List<TransactionModel> list, Context context) {
        this.list = list;
        this.inflater = LayoutInflater.from(context);
    }

    public SearchListAdapter(Context ctx, List<TransactionModel> list, OnActionListener listener) {
        this.list = list;
        this.context = ctx;
        this.listener = listener;
        this.inflater = LayoutInflater.from(ctx);
    }

    @Override
    public int getCount() { return list.size(); }

    @Override
    public Object getItem(int i) { return list.get(i); }

    @Override
    public long getItemId(int i) { return i; }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {

        View view = inflater.inflate(R.layout.layout_row_balance, parent, false);

        TextView txtCategory = view.findViewById(R.id.txtCategory);
        TextView txtDate = view.findViewById(R.id.txtDate);
        TextView txtNote = view.findViewById(R.id.txtNote);
        TextView txtAmount = view.findViewById(R.id.txtAmount);

        TransactionModel item = list.get(pos);

        txtCategory.setText(item.getCategory());
        txtDate.setText(new SimpleDateFormat("yyyy-MM-dd").format(item.getDate()));
        txtNote.setText(item.getNote());
        // Set amount text
        txtAmount.setText(String.format("%.2f", item.getAmount()));

        // Color based on group
        if ("income".equalsIgnoreCase(item.getGroup())) {
            txtAmount.setTextColor(0xFF008000);   // green
        } else {
            txtAmount.setTextColor(0xFFB00000);   // red
        }

        // CLICK = EDIT
        view.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });

        // LONG PRESS = DELETE
        view.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(item);
            return true;
        });

        return view;
    }
    static class ViewHolder {
        TextView txtCategory, txtDate, txtNote, txtAmount;
    }
}
