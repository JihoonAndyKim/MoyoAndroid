/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrftoolbox.hrs;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.GraphicalView;

import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.FeaturesActivity;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.hts.HTSService;
import no.nordicsemi.android.nrftoolbox.pro.ProfileActivity;
import no.nordicsemi.android.nrftoolbox.profile.BleManager;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileActivity;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileService;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileServiceReadyActivity;

/**
 * HRSActivity is the main Heart rate activity. It implements HRSManagerCallbacks to receive callbacks from HRSManager class. The activity supports portrait and landscape orientations. The activity
 * uses external library AChartEngine to show real time graph of HR values.
 */
// TODO The HRSActivity should be rewritten to use the service approach, like other do.
public class HRSActivity extends BleProfileServiceReadyActivity<HRSService.RSCBinder> {
	@SuppressWarnings("unused")
	private final String TAG = "HRSActivity";

	private final static String GRAPH_STATUS = "graph_status";
	private final static String GRAPH_COUNTER = "graph_counter";
	private final static String HR_VALUE = "hr_value";

	private final static int MAX_HR_VALUE = 65535;
	private final static int MIN_POSITIVE_VALUE = 0;
	private final static int REFRESH_INTERVAL = 1000; // 1 second interval

	private Handler mHandler = new Handler();

	private boolean isGraphInProgress = false;
	private boolean trigger = true;

	private GraphicalView mGraphView;
	private LineGraphView mLineGraph;
	private TextView mHRSValue, mHRSPosition;

	private int mHrmValue = 0;
	private int mCounter = 0;

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_feature_hrs);
		setGUI();
	}

	private void setGUI() {
		mLineGraph = LineGraphView.getLineGraphView();
		mHRSValue = (TextView) findViewById(R.id.text_hrs_value);
		mHRSPosition = (TextView) findViewById(R.id.text_hrs_position);

		//Set this to be invisible
		mHRSPosition.setVisibility(View.INVISIBLE);
		showGraph();
	}

	private void showGraph() {
		mGraphView = mLineGraph.getView(this);
		ViewGroup layout = (ViewGroup) findViewById(R.id.graph_hrs);
		layout.addView(mGraphView);
	}

	@Override
	protected void onStart() {
		super.onStart();

		final Intent intent = getIntent();
		if (!isDeviceConnected() && intent.hasExtra(FeaturesActivity.EXTRA_ADDRESS)) {
			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(getIntent().getByteArrayExtra(FeaturesActivity.EXTRA_ADDRESS));
			onDeviceSelected(device, device.getName());

			intent.removeExtra(FeaturesActivity.EXTRA_APP);
			intent.removeExtra(FeaturesActivity.EXTRA_ADDRESS);
		}
	}

	@Override
	protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		isGraphInProgress = savedInstanceState.getBoolean(GRAPH_STATUS);
		mCounter = savedInstanceState.getInt(GRAPH_COUNTER);
		mHrmValue = savedInstanceState.getInt(HR_VALUE);

		if (isGraphInProgress)
			startShowGraph();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(GRAPH_STATUS, isGraphInProgress);
		outState.putInt(GRAPH_COUNTER, mCounter);
		outState.putInt(HR_VALUE, mHrmValue);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		stopShowGraph();
	}

	@Override
	protected int getLoggerProfileTitle() {
		return R.string.hrs_feature_title;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.hrs_about_text;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.hrs_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		return HRSManager.HR_SERVICE_UUID;
	}

	private void updateGraph(final int hrmValue) {
		mCounter++;
		mLineGraph.addValue(new Point(mCounter, hrmValue));
		mGraphView.repaint();
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return HRSService.class;
	}

	private Runnable mRepeatTask = new Runnable() {
		@Override
		public void run() {
			if (mHrmValue > 0)
				updateGraph(mHrmValue);
			if (isGraphInProgress)
				mHandler.postDelayed(mRepeatTask, REFRESH_INTERVAL);
		}
	};

	void startShowGraph() {
		isGraphInProgress = true;
		mRepeatTask.run();
	}

	void stopShowGraph() {
		isGraphInProgress = false;
		mHandler.removeCallbacks(mRepeatTask);
	}

	@Override
	protected void onServiceBinded(final HRSService.RSCBinder binder) {
		// not used
	}

	@Override
	protected void onServiceUnbinded() {
		// not used
	}


	private void setHRSValueOnView(final int value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (value >= MIN_POSITIVE_VALUE && value <= MAX_HR_VALUE) {
					mHRSValue.setText(Integer.toString(value));
				} else {
					mHRSValue.setText(R.string.not_available_value);
				}
				if (value >= 290 && trigger) {

					String phoneNo = "3039059887";
					String message = "USER IS UNDERGOING CARDIAC ARREST";
					if (phoneNo.length() > 0 && message.length() > 0) {
						sendSMS(phoneNo, message);
						Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Emergency services contacted",
								Snackbar.LENGTH_SHORT)
								.show();
					}
					else
						Toast.makeText(getBaseContext(), "Please enter both phone number and message.", Toast.LENGTH_SHORT).show();
					trigger = false;
				}
				if(value < 290) {
					trigger = true;
				}
			}
		});
	}

	private void setHRSPositionOnView(final String position) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (position != null) {
					mHRSPosition.setText(position);
				} else {
					mHRSPosition.setText(R.string.not_available);
				}
			}
		});
	}

	@Override
	public void onServicesDiscovered(final BluetoothDevice device, final boolean optionalServicesFound) {
		// this may notify user or show some views
	}

	@Override
	public void onDeviceReady(final BluetoothDevice device) {
		startShowGraph();
	}

	/*
	@Override
	public void onHRSensorPositionFound(final BluetoothDevice device, final String position) {
		setHRSPositionOnView(position);
	}

	@Override
	public void onHRValueReceived(final BluetoothDevice device, int value) {
		mHrmValue = value;
		setHRSValueOnView(mHrmValue);
	}
	*/

	@Override
	public void onDeviceDisconnected(final BluetoothDevice device) {
		Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Moyo has disconnected with the application",
				Snackbar.LENGTH_SHORT)
				.show();

		sendNotification();

		super.onDeviceDisconnected(device);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mHRSValue.setText(R.string.not_available_value);
				mHRSPosition.setText(R.string.not_available);
				stopShowGraph();
			}
		});
	}

	@Override
	protected void setDefaultUI() {
		mHRSValue.setText(R.string.not_available_value);
		mHRSPosition.setText(R.string.not_available);
		clearGraph();
	}

	private void clearGraph() {
		mLineGraph.clearGraph();
		mGraphView.repaint();
		mCounter = 0;
		mHrmValue = 0;
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (HTSService.BROADCAST_HTS_MEASUREMENT.equals(action)) {
				final int value = intent.getIntExtra(HRSService.HRS_VALUE, 0);
				final String position = intent.getStringExtra(HRSService.HRS_POSITION);
				// Update GUI
				setHRSPositionOnView(position);
				setHRSValueOnView(value);
			}
		}
	};


	public void sendSMS(String phoneNo, String msg) {
		try {
			SmsManager smsManager = SmsManager.getDefault();
			smsManager.sendTextMessage(phoneNo, null, msg, null, null);
		} catch (Exception ex) {
			Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
					Toast.LENGTH_LONG).show();
			ex.printStackTrace();
		}
	}

	private void sendNotification() {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.drawable.moyo_logo_small)
						.setContentTitle("Moyo Notification")
						.setContentText("The application has disconnected from Moyo. Please " +
								"reconnect by opening the app and connecting to the device");

		Intent resultIntent = new Intent(this, HRSActivity.class);

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(HRSActivity.class);
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
				);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, mBuilder.build());
	}
}
