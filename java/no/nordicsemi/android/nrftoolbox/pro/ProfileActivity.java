package no.nordicsemi.android.nrftoolbox.pro;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import no.nordicsemi.android.nrftoolbox.R;

public class ProfileActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "ProfileData";
    public static final String Name = "nameKey";
    public static final String Sex = "sexKey";
    public static final String Age = "ageKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        name_editText=(EditText)findViewById(R.id.name_editText);
        sex_editText=(EditText)findViewById(R.id.sex_editText);
        age_editText=(EditText)findViewById(R.id.age_editText);
        edit_button=(Button)findViewById(R.id.edit_button);


        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);



    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop(){
        super.onStop();

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        // Commit the edits!
        editor.commit();
    }
}
