package cn.xydzjnq.generateview.test;

import android.app.Dialog;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomDialog extends Dialog {
    public CustomDialog(@NonNull Context context) {
        super(context);
    }

    public CustomDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    protected CustomDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        setContentView(R.layout.activity_main);
    }
}
