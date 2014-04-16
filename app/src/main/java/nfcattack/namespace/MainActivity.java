package nfcattack.namespace;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import nfcattack.namespace.utils.SavedDB;
import nfcattack.namespace.utils.TagTechnologyWrapper;


public class MainActivity extends ActionBarActivity {

    public enum State {
        IDLE, SKIM
    }

    private TabHost tabHost;
    private TextView dataView;
    private ListView savedList;

    private SavedDB db;

    private State state = State.IDLE;
    private Bundle replaySession;

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
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle t = (Bundle)v.getTag();
                        byte[][] tr = (byte[][])t.getSerializable("transactions");
                        Bundle reqs = new Bundle();
                        for (int i = 0; i < tr.length; i ++) {
                            reqs.putByteArray(String.valueOf(i), tr[i]);
                        }
                        Bundle b = new Bundle();
                        b.putBundle("requests", reqs);
                        replaySession = b;

                        state = State.SKIM;
                        tabHost.setCurrentTab(0);
                    }
                });

                int index = cursor.getColumnIndex("name");
                String name = cursor.getString(index);
                tv.setText(name);

                byte[] tBytes = cursor.getBlob(cursor.getColumnIndex("transactions"));
                byte[][] transactions = null;
                ByteArrayInputStream bais = new ByteArrayInputStream(tBytes);
                try {
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    transactions = (byte[][])ois.readObject();
                } catch (StreamCorruptedException e) {
                    System.out.println(e);
                } catch (IOException e) {
                    System.out.println(e);
                } catch (ClassNotFoundException e) {
                    System.out.println(e);
                }

                Bundle tag = new Bundle();
                tag.putSerializable("transactions", transactions);
                tag.putString("name", name);
                tv.setTag(tag);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter != null) {
            IntentFilter intentFilter[] = { new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED) };
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
            adapter.enableForegroundDispatch(this, pendingIntent, intentFilter, new String[][] { new String[] { "android.nfc.tech.IsoPcdA" } });
        }

        Intent intent = getIntent();

        try {
            Class.forName("android.nfc.tech.IsoPcdA");
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }

        Tag extraTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (extraTag != null) {
            if (state == State.SKIM) {
                doReplayPCD(extraTag, replaySession.getBundle("requests")); //, replaySession.getBundle("responses"));
            }
        }
    }

    private void doReplayPCD(Tag tag, Bundle pcdRequests) { //, Bundle tagTransactions) {
        Bundle responses = new Bundle();
        TagTechnologyWrapper tagTech = null;
        System.out.println(state.toString());

        try {
            Class[] supportedTags = new Class[] { IsoDep.class };
            String[] tech = tag.getTechList();
            for (String s: tech) {

                for(Class c: supportedTags) {
                    if (s.equals(c.getName())) {
                        try {
                            tagTech = new TagTechnologyWrapper(tag, c.getName());
                            break;
                        } catch (IllegalArgumentException e) {
                            System.out.println(e);
                        } catch (ClassNotFoundException e) {
                            System.out.println(e);
                        } catch (NoSuchMethodException e) {
                            System.out.println(e);
                        } catch (IllegalAccessException e) {
                            System.out.println(e);
                        } catch (InvocationTargetException e) {
                            System.out.println(e);
                        }
                    }
                }
            }
            if (tagTech != null) {
                tagTech.connect();
                boolean connected = tagTech.isConnected();
                System.out.println("isConnected: " + connected);
                if (!connected) return;

                responses.putByteArray(String.valueOf(0), tag.getId());
                SpannableString msg = new SpannableString(byteArrayToHexString(tag.getId()));
                dataView.append(msg);
                boolean foundCC = false;
                for(int i=0; i < pcdRequests.size(); i++) {
                    if (foundCC) {
                        dataView.append("\n");
                    }
                    byte[] tmp = pcdRequests.getByteArray(String.valueOf(i));
                    msg = new SpannableString(byteArrayToHexString(tmp));
//                    msg.setSpan(new UnderlineSpan(), 0, 4, 0);
                    dataView.append(msg);
                    byte[] reply = tagTech.transceive(tmp);

                    responses.putByteArray(String.valueOf(i+1), reply);
                    msg = new SpannableString(byteArrayToHexString(reply));
                    dataView.append(msg);

//                    if (tagTransactions != null) {
//                        if (i + 1 < tagTransactions.size() ) {
//                            if (Arrays.equals(reply, tagTransactions.getByteArray(String.valueOf(i + 1)))) {
//                                System.out.println(getString(R.string.equal));
//                            }
//                            else {
//                                System.out.println("org: " + byteArrayToHexString(tagTransactions.getByteArray(String.valueOf(i + 1))));
//                                System.out.println("new : " + byteArrayToHexString(reply));
//                            }
//                        }
//                        else {
//                            System.out.println("index to responses out of bounds");
//                            updateStatus(getString(R.string.index_out_bounds));
//                        }
//                    }
//
                    if (reply != null && reply[0] == 0x70) {
//                        updateData("\n" + TagHelper.parseCC(reply, pcdRequests.getByteArray(String.valueOf(i - 1)), mMask));
                        foundCC = true;
//                        if (i == pcdRequests.size() - 1) {
//                            System.out.println(getString(R.string.finished_reading));
//                            updateStatus(getString(R.string.finished_reading));
//                        }
                    }
//                    else if (reply != null && reply.length > 3 && reply[0] == 0x77 && reply[2] == (byte)0x9f) {
//                        updateData("\n" + TagHelper.parseCryptogram(reply, tmp)); //previous pcdRequest
//                        System.out.println(getString(R.string.finished_reading));
//                        updateStatus(getString(R.string.finished_reading));
//                    }
                }
            }
//            else {
//                System.out.println(getString(R.string.unsupported_tag));
//            }
        } catch (IllegalStateException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
        finally {
            if (tagTech != null) {
                try {
                    tagTech.close();
                } catch (IOException e) {
                    System.out.println(e);
                }
            }
        }
    }

    public static String byteArrayToHexString(byte[] b) {
        return byteArrayToHexString(b, "0x", " ", false);
    }

    public static String byteArrayToHexString(byte[] b, String hexPrefix, String hexSuffix, boolean cast) {
        if (b == null) return null;
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xff;
            if (cast && v > 0x7f) {
                sb.append("(byte)");
            }
            sb.append(hexPrefix);
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
            if (i + 1 != b.length) {
                sb.append(hexSuffix);
            }
        }
        return sb.toString();
    }
}