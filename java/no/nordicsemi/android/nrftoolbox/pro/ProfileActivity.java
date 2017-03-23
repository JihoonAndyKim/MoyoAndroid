package no.nordicsemi.android.nrftoolbox.pro;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.method.KeyListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import no.nordicsemi.android.nrftoolbox.R;

public class ProfileActivity extends AppCompatActivity {

    public static final String MyPREFS = "ProfileData";
    public static final String Name = "nameKey";
    public static final String Gender = "genderKey";
    public static final String Age = "ageKey";
    public static final String Med = "medKey";
    public static final String Missing = "Missing";

    SharedPreferences settings;
    EditText name_editText,gender_editText,med_editText;
    boolean name_edited, gender_edited, age_edited, med_edited;
    KeyListener name_orig_kl, gender_orig_kl, med_orig_kl;
    Spinner age_spinner;
    LinearLayout profile_layout;
    List<String> ageSpinnerArray = new ArrayList<String>();

    int ageSpinnerPosition;
    String editingField;
    Object mActionMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        profile_layout = (LinearLayout) findViewById(R.id.profile_Layout);
        name_editText=(EditText)findViewById(R.id.name_editText);
        gender_editText=(EditText)findViewById(R.id.gender_editText);
        age_spinner=(Spinner)findViewById(R.id.age_spinner);
        med_editText=(EditText)findViewById(R.id.med_editText);

        ageSpinnerArray.add("Select Age");
        for(int i = 0; i <= 120; i= i+1){
            ageSpinnerArray.add(Integer.toString(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.spinner_item,ageSpinnerArray);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        age_spinner.setAdapter(adapter);

        // Restore preferences
        settings = getSharedPreferences(MyPREFS, MODE_PRIVATE);

        String n = settings.getString(Name, Missing);
        String g = settings.getString(Gender, Missing);
        String a = settings.getString(Age, Missing);
        String m = settings.getString(Med, Missing);

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
            ageSpinnerPosition = adapter.getPosition(a);
            age_spinner.setSelection(ageSpinnerPosition);
        }
        else {
            age_spinner.setSelection(0);
        }
        if (!m.equals(Missing)){
            med_editText.setText(m);
        }
        else {
            med_editText.setHint(R.string.profile_med_hint);
        }

        name_orig_kl = name_editText.getKeyListener();
        gender_orig_kl = gender_editText.getKeyListener();
        med_orig_kl = med_editText.getKeyListener();

        name_editText.setKeyListener(null);
        gender_editText.setKeyListener(null);
        med_editText.setKeyListener(null);

        name_edited = false;
        gender_edited = false;
        age_edited = false;
        med_edited = false;

        name_editText.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v){
                name_edited = true;
                editTextLongClicked(name_editText, "Editing Name");
                return true;
            }
        });
        gender_editText.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v){
                gender_edited = true;
                editTextLongClicked(gender_editText, "Editing Gender");
                return true;
            }
        });
        med_editText.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v){
                med_edited = true;
                editTextLongClicked(med_editText, "Editing Medical Info");
                return true;
            }
        });

    }

    public void editTextLongClicked(EditText et, String s){
        editingField = s;

        if(mActionMode == null){
            mActionMode = ProfileActivity.this.startSupportActionMode(mActionModeCallback);
        }
        switch (et.getId()){
            case (R.id.name_editText):
                et.setKeyListener(name_orig_kl);
                break;
            case (R.id.gender_editText):
                et.setKeyListener(gender_orig_kl);
                break;
            case (R.id.med_editText):
                et.setKeyListener(med_orig_kl);
                break;
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(et, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback(){
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu){
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode){

            SharedPreferences.Editor editor = settings.edit();

            if (name_edited){
                String n  = name_editText.getText().toString();
                editor.putString(Name, n);
                name_editText.setKeyListener(null);
                hideKeyboard(name_editText);
                name_edited = false;
            }
            else if (gender_edited){
                String g  = gender_editText.getText().toString();
                editor.putString(Gender, g);
                gender_editText.setKeyListener(null);
                hideKeyboard(gender_editText);
                gender_edited = false;
            }
            else if (age_edited){
                String a  = age_spinner.getSelectedItem().toString();
                editor.putString(Age, a);
                // make uneditable?
            }
            else if (med_edited){
                String m = med_editText.getText().toString();
                editor.putString(Med, m);
                med_editText.setKeyListener(null);
                hideKeyboard(med_editText);
                med_edited = false;
            }

            editor.apply();

            // TODO: delete this line
            Toast.makeText(ProfileActivity.this,"Saved!",Toast.LENGTH_LONG).show();

            mActionMode = null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu){
            mode.setTitle(editingField);
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.profile_menu, menu);

            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item){
            return false;
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_profile_menu, menu);
        return true;
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

    public void hideKeyboard(View view) {
        InputMethodManager imm =(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
