package de.theknut.xposedgelsettings.hooks.appdrawer.tabsandfolders;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.theknut.xposedgelsettings.hooks.Common;
import de.theknut.xposedgelsettings.hooks.ObfuscationHelper.Fields;
import de.theknut.xposedgelsettings.hooks.ObfuscationHelper.Methods;
import de.theknut.xposedgelsettings.hooks.Utils;
import de.theknut.xposedgelsettings.hooks.appdrawer.tabsandfolders.TabHelperLegacy.ContentType;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

/**
 * Created by Alexander Schulz on 21.08.2014.
 */
public class Tab extends AppDrawerItem {

    public static int DEFAULT_COLOR = Color.parseColor("#F4F4F4");
    public static final String KEY_PREFIX= "tab";
    public static final int APPS_ID = 0xABB5;
    public static final int WIDGETS_ID = 0xBEEF;

    private ContentType contentType = ContentType.User;
    private int color = DEFAULT_COLOR;
    private boolean initialized = false;

    public Tab(String tabCfg) {
        this(tabCfg, true);
    }

    public Tab(String tabCfg, boolean initData) {
        String[] settings = tabCfg.split("\\|");

        for (String setting : settings) {
            if (setting.startsWith("idx=")) {
                this.idx = Integer.parseInt(setting.split("=")[1]);
            } else if (setting.startsWith("id=")) {
                this.id = Long.parseLong(setting.split("=")[1]);
            } else if (setting.startsWith("title=")) {
                this.title = setting.split("=")[1];
            } else if (setting.startsWith("contenttype=")) {
                this.contentType = ContentType.valueOf(setting.split("=")[1]);
            } else if (setting.startsWith("hide=")) {
                this.hideFromAppsPage = Boolean.parseBoolean(setting.split("=")[1]);
            } else if (setting.startsWith("sort=")) {
                this.sortType = SortType.valueOf(setting.split("=")[1]);
            } else if (setting.startsWith("color=")) {
                this.color = Integer.parseInt(setting.split("=")[1]);
            }
        }

        if (initData) initData();
    }

    public Tab(Intent intent, boolean initData) {
        this.id = intent.getLongExtra("itemid", -1);
        this.idx = intent.getIntExtra("index", -1);
        this.title = intent.getStringExtra("name");
        this.contentType = ContentType.valueOf(intent.getStringExtra("contenttype"));
        this.hideFromAppsPage = intent.getBooleanExtra("hide", false);
        this.color = intent.getIntExtra("color", DEFAULT_COLOR);

        if (initData) initData();
    }

    public Tab(Intent intent, boolean initData, boolean add) {
        this.id = intent.getLongExtra("itemid", -1);
        this.idx = intent.getIntExtra("index", -1);
        this.title = intent.getStringExtra("name");
        this.contentType = ContentType.valueOf(intent.getStringExtra("contenttype"));
        this.hideFromAppsPage = intent.getBooleanExtra("hide", false);
        this.color = intent.getIntExtra("color", DEFAULT_COLOR);

        if (initData) initData(add);
    }

    public void initData() {
        initData(false);
    }

    public void initData(final boolean add) {
        initialized = false;
        final Tab tab = this;
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                if (isUserTab()) {
                    parseData();
                } else if (isGoogleTab()) {
                    data = getGoogleApps();
                } else if (isXposedTab()) {
                    data = getXposedModules();
                } else if (isIconPacksTab()) {
                    data = getIconPacks();
                    sort(data);
                } else if (isNewUpdatedTab()) {
                    data = getNewUpdatedApps();
                    setSortType(SortType.LastUpdate);
                } else if (isNewAppsTab()) {
                    data = getNewApps();
                    setSortType(SortType.LastInstall);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                ArrayList allApps = (ArrayList) getObjectField(Common.APP_DRAWER_INSTANCE, Fields.acpvAllApps);
                callMethod(Common.APP_DRAWER_INSTANCE, Methods.acpvSetApps, allApps);

                if (add) {
                    TabHelperNew.getInstance().addTab(tab);
                }
                else if (TabHelper.getInstance().getCurrentTabData().equals(this)) {
                    TabHelper.getInstance().invalidate();
                }
                initialized = true;
            }
        }.execute();
    }

    public ContentType getContentType() {
        return contentType;
    }

    public int getPrimaryColor() {
        if (Common.IS_PRE_GNL_4) {
            return this.color;
        }

        return this.color == Color.WHITE ? Color.parseColor("#F4F4F4") : this.color;
    }

    // http://stackoverflow.com/a/4928826/809277
    public int getSecondaryColor() {
        float[] hsv = new float[3];
        Color.colorToHSV(getPrimaryColor(), hsv);
        hsv[2] *= 0.9f;
        return Color.HSVToColor(hsv);
    }

    public int getContrastColor() {
        return Utils.getContrastColor(getPrimaryColor());
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isUserTab() {
        return contentType.equals(ContentType.User);
    }

    public boolean isWidgetsTab() {
        return contentType.equals(ContentType.Widgets);
    }

    public boolean isXposedTab() {
        return contentType.equals(ContentType.Xposed);
    }

    public boolean isGoogleTab() {
        return contentType.equals(ContentType.Google);
    }

    public boolean isNewUpdatedTab() {
        return contentType.equals(ContentType.NewUpdated);
    }

    public boolean isNewAppsTab() {
        return contentType.equals(ContentType.NewApps);
    }

    private boolean isIconPacksTab() {
        return this.contentType.equals(ContentType.IconPacks);
    }

    public boolean isAppsTab() {
        return getId() == APPS_ID;
    }

    public boolean isCustomTab() {
        return isUserTab() || isDynamicTab();
    }

    public boolean isDynamicTab() {
        return isGoogleTab() || isXposedTab() || isNewUpdatedTab() || isNewAppsTab() || isIconPacksTab();
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    private void parseData() {
        parseData(KEY_PREFIX);
    }

    public ArrayList getRawData() {
        return getRawData(KEY_PREFIX);
    }

    @Override
    public void setSortType(SortType type) {
        super.setSortType(type);
        if (isCustomTab()) sort(getData());
    }

    private ArrayList getXposedModules() {
        final PackageManager pm = Common.LAUNCHER_CONTEXT.getPackageManager();
        this.rawData = new ArrayList();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        ArrayList modules = new ArrayList();

        for (ApplicationInfo app : apps) {
            if (app.metaData != null && app.metaData.getBoolean("xposedmodule", false)) {
                Intent i = pm.getLaunchIntentForPackage(app.packageName);
                if (i != null) {
                    modules.add(Utils.createAppInfo(i.getComponent()));
                    rawData.add(i.getComponent().flattenToString());
                }
            }
        }

        Intent i = pm.getLaunchIntentForPackage("de.robv.android.xposed.installer");
        if (i != null) {
            modules.add(Utils.createAppInfo(i.getComponent()));
            rawData.add(i.getComponent().flattenToString());
        }

        sort(modules);
        return modules;
    }

    private ArrayList getGoogleApps() {
        ArrayList<String> google = new ArrayList<String> (Arrays.asList("com.android.vending", "com.quickoffice.android", "com.android.chrome", "com.chrome.beta", "com.google.earth", "com.google.chromeremotedesktop"));
        ArrayList<String> exclude = new ArrayList<String> (Arrays.asList("com.google.android.diskusage"));
        ArrayList modules = new ArrayList();
        this.rawData = new ArrayList();

        for (ResolveInfo app : Utils.getAllApps()) {
            String pkg = app.activityInfo.packageName;
            if (pkg.contains("com.google.android.") || google.contains(pkg) && !exclude.contains(pkg)) {
                ComponentName cmp = new ComponentName(pkg, app.activityInfo.name);
                modules.add(Utils.createAppInfo(cmp));
                rawData.add(cmp.flattenToString());
            }
        }

        sort(modules);
        return modules;
    }

    private ArrayList getNewUpdatedApps() {
        ArrayList apps = new ArrayList();
        this.rawData = new ArrayList();

        for (ResolveInfo app : Utils.getAllApps()) {
            ComponentName cmp = new ComponentName(app.activityInfo.packageName, app.activityInfo.name);
            apps.add(Utils.createAppInfo(cmp));
            rawData.add(cmp.flattenToString());
        }

        return apps;
    }

    private ArrayList getNewApps() {
        ArrayList apps = new ArrayList();
        this.rawData = new ArrayList();

        for (ResolveInfo app : Utils.getAllApps()) {
            ComponentName cmp = new ComponentName(app.activityInfo.packageName, app.activityInfo.name);
            apps.add(Utils.createAppInfo(cmp));
            rawData.add(cmp.flattenToString());
        }

        return apps;
    }

    private ArrayList getIconPacks() {
        PackageManager packageManager = Common.LAUNCHER_CONTEXT.getPackageManager();
        List<String> packages = new ArrayList<String>();
        ArrayList apps = new ArrayList();
        this.rawData = new ArrayList();
        Intent i = new Intent();

        String[] sIconPackCategories = new String[] {
                "com.fede.launcher.THEME_ICONPACK",
                "com.anddoes.launcher.THEME",
                "com.teslacoilsw.launcher.THEME"
        };
        String[] sIconPackActions = new String[] {
                "org.adw.launcher.THEMES",
                "com.gau.go.launcherex.theme"
        };

        for (String action : sIconPackActions) {
            i.setAction(action);
            for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                if (!packages.contains(r.activityInfo.packageName))
                    packages.add(r.activityInfo.packageName);
            }
        }

        i = new Intent(Intent.ACTION_MAIN);
        for (String category : sIconPackCategories) {
            i.addCategory(category);
            for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
                if (!packages.contains(r.activityInfo.packageName))
                    packages.add(r.activityInfo.packageName);
            }
            i.removeCategory(category);
        }

        for (String pkg : packages) {
            Intent li = packageManager.getLaunchIntentForPackage(pkg);
            if (li != null) {
                apps.add(Utils.createAppInfo(li.getComponent()));
                rawData.add(li.getComponent().flattenToString());
            }
        }

        return apps;
    }

    public int getPageCount() {
        if ((isAppsTab() || isUserTab()) && FolderHelper.getInstance().hasFolder()) {
            int mCellCountX = getIntField(Common.APP_DRAWER_INSTANCE, Fields.acpvCellCountX);
            int mCellCountY = getIntField(Common.APP_DRAWER_INSTANCE, Fields.acpvCellCountY);
            int itemCnt = FolderHelper.getInstance().getFoldersForTab(getId()).size();
            itemCnt += isAppsTab() ? FolderHelper.getInstance().getAllApps().size() : getData().size();
            return (int) Math.ceil((float) itemCnt / (mCellCountX * mCellCountY));
        } else if (isCustomTab() && getData() != null) {
            int mCellCountX = getIntField(Common.APP_DRAWER_INSTANCE, Fields.acpvCellCountX);
            int mCellCountY = getIntField(Common.APP_DRAWER_INSTANCE, Fields.acpvCellCountY);
            return (int) Math.ceil((float) getData().size() / (mCellCountX * mCellCountY));
        } else if (isNewAppsTab() || isNewUpdatedTab()) {
            return 1;
        }
        return 3;
    }

    public void update() {
        initData();
    }

    @Override
    public String toString() {
        return super.toString() + "|"
                + "contenttype=" + getContentType() + "|"
                + "color=" + getPrimaryColor();
    }
}