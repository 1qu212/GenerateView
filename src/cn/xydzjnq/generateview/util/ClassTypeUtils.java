package cn.xydzjnq.generateview.util;

import java.util.ArrayList;
import java.util.HashMap;

public class ClassTypeUtils {
    public static final HashMap<String, String> viewPaths = new HashMap<>();
    public static final ArrayList<String> activitys = new ArrayList<>();
    public static final ArrayList<String> fragments = new ArrayList<>();
    public static final ArrayList<String> recycleViewAdapters = new ArrayList<>();
    public static final ArrayList<String> adapters = new ArrayList<>();

    static {
        //view的默认包名是android.weight
        viewPaths.put("WebView", "android.webkit.WebView");
        viewPaths.put("View", "android.view.View");
        viewPaths.put("ViewStub", "android.view.ViewStub");
        viewPaths.put("SurfaceView", "android.view.SurfaceView");
        viewPaths.put("TextureView", "android.view.TextureView");

        //activitys
        activitys.add("android.app.Activity");

        //fragments
        fragments.add("android.app.Fragment");
        fragments.add("android.support.v4.app.Fragment");
        fragments.add("androidx.fragment.app.Fragment");

        //recycleViewAdapters
        recycleViewAdapters.add("android.support.v7.recyclerview.extensions.ListAdapter");
        recycleViewAdapters.add("androidx.recyclerview.widget.ListAdapter");
        recycleViewAdapters.add("android.support.v7.widget.RecyclerView.Adapter");
        recycleViewAdapters.add("androidx.recyclerview.widget.RecyclerView.Adapter");

        //adapters
        adapters.add("android.widget.BaseAdapter");
        adapters.add("android.widget.BaseExpandableListAdapter");
        adapters.add("android.widget.CursorAdapter");
        adapters.add("android.support.v4.widget.CursorAdapter");
        adapters.add("androidx.cursoradapter.widget.CursorAdapter");
    }
}
