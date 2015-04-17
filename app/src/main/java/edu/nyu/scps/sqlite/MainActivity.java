package edu.nyu.scps.sqlite;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.os.Handler;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
    private Helper helper;  //Can't initialize these fields before onCreate.
    private SimpleCursorAdapter adapter;
    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            throw new RuntimeException();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ListView listView = (ListView)findViewById(R.id.listView);

        helper = new Helper(this);
        Cursor cursor = helper.getCursor();

        adapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2,
                cursor,
                new String[] {"name",             "_id"},
                new int[]    {android.R.id.text1, android.R.id.text2},
                0	//don't need any flags
        );

        listView.setAdapter(adapter);

        //Display a message when the table contains no rows.

        LayoutInflater inflater = getLayoutInflater();
        TextView textView = (TextView)inflater.inflate(R.layout.empty, null);
        ViewGroup viewGroup = (ViewGroup)findViewById(android.R.id.content); //Get the RelativeLayout.
        viewGroup.addView(textView);
        listView.setEmptyView(textView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor)parent.getItemAtPosition(position); //downcast
                int indexDisplayName = cursor.getColumnIndex("name");
                String name = cursor.getString(indexDisplayName);
                String s = "Deleted " + name + ", position = " + position + ", id = " + id + ".";
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();

                SQLiteDatabase database = helper.getWritableDatabase();
                database.delete("people", "_id = ?", new String[] {Long.toString(id)});
                adapter.changeCursor(helper.getCursor());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_append) {
            String name = getString("", "Enter new contact name");
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", name);
            SQLiteDatabase database = helper.getWritableDatabase();
            database.insert("people", null, contentValues);
            adapter.changeCursor(helper.getCursor());
            return true;
        }

        if (id == R.id.action_delete_all) {
            SQLiteDatabase database = helper.getWritableDatabase();
            database.delete("people", null, null);  //Delete all records!
            adapter.changeCursor(helper.getCursor());
            return true;
        }

        if(id == R.id.action_edit_entry) {
            String newId = "_id = "+ getString("ID", "Enter entry id to edit");
            String newName = getString("Name", "Enter new name");
            ContentValues contentValues = new ContentValues();
            contentValues.put("name", newName);
            SQLiteDatabase database = helper.getWritableDatabase();
            database.update("people", contentValues, newId, null);
            adapter.changeCursor(helper.getCursor());
            return true;
        }

        if(id == R.id.action_sort_alpha) {
            SQLiteDatabase database = helper.getWritableDatabase();
            adapter.changeCursor(helper.sortByName());
            return true;
        }

        if(id == R.id.action_sort_id) {
            SQLiteDatabase database = helper.getWritableDatabase();
            adapter.changeCursor(helper.sortById());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private String mResult;

    public String getString(String title, String message) {

        //A builder object can create a dialog object.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        //This inflator reads the dialog.xml and creates the objects described therein.
        //Pass null as the parent view because it's going in the dialog layout.
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog, null);
        builder.setView(view);

        //Must be final to be mentioned in the anonymous inner class.
        final EditText editText = (EditText)view.findViewById(R.id.editText);

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((keyCode == EditorInfo.IME_ACTION_DONE) ||(event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER)) {

                    Editable editable = editText.getText();
                    String string = editable.toString();
                    mResult = string;

                    Toast.makeText(MainActivity.this, mResult, Toast.LENGTH_LONG).show();

                    //Sending this message will break us out of the loop below.
                    Message message = handler.obtainMessage();
                    handler.sendMessage(message);
                }
                return false;
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        //Loop until the user presses the EditText's Done button.
        try {
            Looper.loop();
        }
        catch(RuntimeException runtimeException) {
        }

        alertDialog.dismiss();
        return mResult;
    }
}
