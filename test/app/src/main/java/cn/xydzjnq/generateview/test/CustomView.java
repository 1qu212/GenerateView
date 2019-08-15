package cn.xydzjnq.generateview.test;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class CustomView extends LinearLayout {

    public CustomView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.custom_view, this);
    }
}
