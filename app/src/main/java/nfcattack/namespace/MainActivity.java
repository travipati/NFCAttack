package nfcattack.namespace;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    private TabHost tabHost;
    private ScrollView dataTab;
    private TextView dataView;
    private ListView savedList;

    private SavedDB db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    private void initialize() {
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("data").setContent(R.id.svData).setIndicator("Data"));
        tabHost.addTab(tabHost.newTabSpec("saved").setContent(R.id.svSaved).setIndicator("Saved"));

        dataTab = (ScrollView) findViewById(R.id.svData);
        dataView = (TextView) findViewById(R.id.tvData);

        savedList = (ListView) findViewById(R.id.lvSaved);
        db = new SavedDB(this);
        savedList.setAdapter(new CursorAdapter(this, db.getReplays(), 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                final View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.saved_field, parent, false);
                return view;
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                TextView tv = (TextView) view.findViewById(R.id.tvSaved);
//                tv.setOnLongClickListener(getSavedTextViewLongClickListener());

                int index = cursor.getColumnIndex("name");
                String name = cursor.getString(index);
                tv.setText(name);
            }
        });
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onResume() {
        super.onResume();

//        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
//        if (adapter != null) {
//            IntentFilter intentFilter[] = { new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) };
//            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
//            adapter.enableForegroundDispatch(this, pendingIntent, intentFilter, new String[][] { new String[] { "android.nfc.tech.IsoPcdA" } });
//        }

    }
}