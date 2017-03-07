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

    public static final String MyPREFS = "ProfileData";
    public static final String Name = "nameKey";
    public static final String Gender = "genderKey";
    public static final String Age = "ageKey";
    public static final String Missing = "Missing";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        final EditText name_editText,gender_editText,age_editText;
        Button edit_button;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        name_editText=(EditText)findViewById(R.id.name_editText);
        gender_editText=(EditText)findViewById(R.id.gender_editText);
        age_editText=(EditText)findViewById(R.id.age_editText);
        edit_button=(Button)findViewById(R.id.edit_button);

        // Restore preferences
        final SharedPreferences settings = getSharedPreferences(MyPREFS, MODE_PRIVATE);

        String n = settings.getString(Name, Missing);
        String g = settings.getString(Gender, Missing);
        String a = settings.getString(Age, Missing);

        if (!n.equals(Missing)){
            name_editText.setText(n);
        }
        else{
            name_editText.setHint(R.string.profile_name_hint);
        }
        if (!g.equals(Missing)){
            gender_editText.setText(g);
        }
        else{
            gender_editText.setHint(R.string.profile_gender_hint);
        }
        if (!a.equals(Missing)){
            age_editText.setText(a);
        }
        else {
            age_editText.setHint(R.string.profile_age_hint);
        }

        edit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String n  = name_editText.getText().toString();
                String g  = gender_editText.getText().toString();
                String a  = age_editText.getText().toString();

                SharedPreferences.Editor editor = settings.edit();

                editor.putString(Name, n);
                editor.putString(Gender, g);
                editor.putString(Age, a);
                editor.apply();
                Toast.makeText(ProfileActivity.this,"Thanks",Toast.LENGTH_LONG).show();
            }
        });

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
        SharedPreferences settings = getSharedPreferences(MyPREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        // Commit the edits!
        editor.apply();
    }
}
