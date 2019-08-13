package cn.xydzjnq.generateview.util;

import java.util.HashMap;

public class PathUtils {
    public static final HashMap<String, String> viewPaths = new HashMap<>();

    static {
        //view的默认包名是android.weight
        viewPaths.put("WebView", "android.webkit.WebView");
        viewPaths.put("View", "android.view.View");
        viewPaths.put("ViewStub", "android.view.ViewStub");
        viewPaths.put("SurfaceView", "android.view.SurfaceView");
        viewPaths.put("TextureView", "android.view.TextureView");
    }
}
