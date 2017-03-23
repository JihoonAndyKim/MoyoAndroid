package no.nordicsemi.android.nrftoolbox;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import no.nordicsemi.android.nrftoolbox.adapter.AppAdapter;
import no.nordicsemi.android.nrftoolbox.hrs.HRSActivity;

/**
 * Created by andy.kim on 3/23/17.
 */

public class IntroActivity extends AppIntro {

    private Intent globalIntent;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent newIntent = new Intent(IntroActivity.this, FeaturesActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        // Handle NFC message, if app was opened using NFC AAR record
        final Intent intent = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            final Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                for (int i = 0; i < rawMsgs.length; i++) {
                    final NdefMessage msg = (NdefMessage) rawMsgs[i];
                    final NdefRecord[] records = msg.getRecords();

                    for (NdefRecord record : records) {
                        if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA) {
                            switch (record.toMimeType()) {
                                case FeaturesActivity.EXTRA_APP:
                                    newIntent.putExtra(FeaturesActivity.EXTRA_APP, new String(record.getPayload()));
                                    break;
                                case FeaturesActivity.EXTRA_ADDRESS:
                                    newIntent.putExtra(FeaturesActivity.EXTRA_ADDRESS, invertEndianness(record.getPayload()));
                                    break;
                            }
                        }
                    }
                }
            }
        }
        globalIntent = newIntent;

        addSlide(AppIntroFragment.newInstance("T", "s", R.drawable.battery, getResources().getColor(R.color.moyoPrimary)));


    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        startActivity(globalIntent);
        finish();
        // Do something when users tap on Done button.
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }


    /**
     * Inverts endianness of the byte array.
     * @param bytes input byte array
     * @return byte array in opposite order
     */
    private byte[] invertEndianness(final byte[] bytes) {
        if (bytes == null)
            return null;
        final int length = bytes.length;
        final byte[] result = new byte[length];
        for (int i = 0; i < length; i++)
            result[i] = bytes[length - i - 1];
        return result;
    }
}
