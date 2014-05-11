package de.theknut.xposedgelsettings.hooks.googlesearchbar;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import de.robv.android.xposed.XC_MethodHook;

import de.theknut.xposedgelsettings.hooks.Common;
import de.theknut.xposedgelsettings.hooks.ObfuscationHelper.Methods;

public final class OnPageBeginMovingHook extends XC_MethodHook {
	
	// http://androidxref.com/4.4.2_r1/xref/packages/apps/Launcher3/src/com/android/launcher3/PagedView.java#595
	// protected void onPageBeginMoving()
	
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		if (Common.LAUNCHER_INSTANCE == null) return;
		
		if ((Boolean) callMethod(Common.LAUNCHER_INSTANCE, Methods.launcherHasCustomContentToLeft) && !Common.IS_DRAGGING) {
			GoogleSearchBarHooks.hideSearchbar();					
		}
	}
}