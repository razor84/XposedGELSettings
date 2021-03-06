package de.theknut.xposedgelsettings.hooks.general;

import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;

import de.theknut.xposedgelsettings.hooks.ObfuscationHelper;
import de.theknut.xposedgelsettings.hooks.common.XGELSCallback;

public final class AddViewToCellLayoutHook extends XGELSCallback {
	
	// public boolean addViewToCellLayout(View child, int index, int childId, LayoutParams params, boolean markCells)
	// http://androidxref.com/4.4.2_r1/xref/packages/apps/Launcher3/src/com/android/launcher3/CellLayout.java#604

    @Override
    public void onBeforeHookedMethod(MethodHookParam param) {
        if (param.args[0].getClass().equals(ObfuscationHelper.Classes.LauncherAppWidgetHostView)) {
            if (DEBUG) log(param, "Make widget resizeable");

            AppWidgetHostView widget = (AppWidgetHostView) param.args[0];
            AppWidgetProviderInfo widgetProviderInfo = widget.getAppWidgetInfo();
            widgetProviderInfo.resizeMode = AppWidgetProviderInfo.RESIZE_BOTH;

            int tmp = 5;
            widgetProviderInfo.minResizeWidth = tmp;
            widgetProviderInfo.minResizeHeight = tmp;
            widgetProviderInfo.minHeight = tmp;
            widgetProviderInfo.minWidth = tmp;
        }
    }
}