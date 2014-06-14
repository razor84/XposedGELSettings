package de.theknut.xposedgelsettings.ui;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import de.theknut.xposedgelsettings.R;
import de.theknut.xposedgelsettings.hooks.Common;

@SuppressLint("WorldReadableFiles")
public class ChooseAppList extends ListActivity {

    String prefKey;
    Intent intent;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getListView().setCacheColorHint(CommonUI.UIColor);
        getListView().setBackgroundColor(CommonUI.UIColor);
        getActionBar().setBackgroundDrawable(new ColorDrawable(CommonUI.UIColor));

        // retrieve the preference key so that we can save a app linked with the gesture
        intent = getIntent();
        prefKey = intent.getStringExtra("prefKey");

        PackageManager pm = getPackageManager();

        // load all apps which are listed in the app drawer
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        // sort them
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));

        AppArrayAdapter adapter = new AppArrayAdapter(this, getPackageManager(), apps);
        setListAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED, intent);
        ChooseAppList.this.finish();
    }

    public class AppArrayAdapter extends ArrayAdapter<ResolveInfo> {
        private Context context;
        private List<ResolveInfo> values;
        private PackageManager pm;

        public AppArrayAdapter(Context context, PackageManager pm, List<ResolveInfo> values) {
            super(context, R.layout.row, values);
            this.context = context;
            this.values = values;
            this.pm = pm;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.row, parent, false);

            ResolveInfo item = values.get(position);

            // setup app icon to row
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            imageView.setImageDrawable(item.loadIcon(pm));

            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.checkbox);
            checkBox.setVisibility(View.GONE);

            // setup app label to row
            TextView textView = (TextView) rowView.findViewById(R.id.name);
            textView.setText(item.loadLabel(pm));

            if (prefKey != null) {
                rowView.setTag(item.activityInfo.packageName);
                rowView.setOnClickListener(new OnClickListener() {

                    @SuppressWarnings("deprecation")
                    @Override
                    public void onClick(View v) {

                        SharedPreferences prefs = getSharedPreferences(Common.PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.remove(prefKey + "_launch");
                        editor.apply();
                        editor.putString(prefKey + "_launch", (String) v.getTag());
                        editor.apply();
                        setResult(RESULT_OK, intent);
                        ChooseAppList.this.finish();
                    }
                });
            } else {
                rowView.setTag(new ComponentName(item.activityInfo.packageName, item.activityInfo.name));
                rowView.setOnClickListener(new OnClickListener() {

                    @SuppressWarnings("deprecation")
                    @Override
                    public void onClick(View v) {

                        Intent i = new Intent(ChooseAppList.this, FragmentSelectiveIcon.class);
                        i.putExtra("app", ((ComponentName) v.getTag()).flattenToString());
                        startActivity(i);
                    }
                });
            }

            // add the row to the listview
            return rowView;
        }
    }
}