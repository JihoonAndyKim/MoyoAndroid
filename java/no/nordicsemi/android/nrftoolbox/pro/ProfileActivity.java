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

    public static final String PREFS_NAME = "MyPrefsFile";

    // SMS Func
    Button btnSendSMS;
    EditText txtPhoneNo;
    EditText txtMessage;
    // End SMS Func

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // SMS Func
        btnSendSMS = (Button) findViewById(R.id.btnSendSMS);
        txtPhoneNo = (EditText) findViewById(R.id.txtPhoneNo);
        txtMessage = (EditText) findViewById(R.id.txtMessage);
        btnSendSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNo = txtPhoneNo.getText().toString();
                String message = txtMessage.getText().toString();
                if (phoneNo.length()>0 && message.length()>0)
                    sendSMS(phoneNo, message);
                else
                    Toast.makeText(getBaseContext(), "Please enter both phone number and message.", Toast.LENGTH_SHORT).show();
            }
        });
        // End SMS Func

        // Restore preferences
        // SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        // boolean silent = settings.getBoolean("silentMode", false);

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
        // SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        // SharedPreferences.Editor editor = settings.edit();

        // Commit the edits!
        // editor.commit();
    }

    private void sendSMS(String phoneNumber, String message){
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, ProfileActivity.class), 0);
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, pi, null);
    }

}
