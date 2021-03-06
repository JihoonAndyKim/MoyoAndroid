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
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import org.achartengine.GraphicalView;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

import no.nordicsemi.android.log.Logger;
import no.nordicsemi.android.nrftoolbox.FeaturesActivity;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.hrs.HRSService;
import no.nordicsemi.android.nrftoolbox.hts.HTSService;
import no.nordicsemi.android.nrftoolbox.pro.ProfileActivity;
import no.nordicsemi.android.nrftoolbox.profile.BleManager;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileActivity;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileService;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileServiceReadyActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

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
	private double mTimeCounter = 0d;

	private static final String VALUE = "value";
	private int mValueC;
	private static final String POSITION = "position";
	private String mPosC;

	LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
	private int countOnPlot = 20;
	DataPoint[] values = new DataPoint[countOnPlot];

	private Queue<Integer> fifo = new LinkedList<Integer>();

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_feature_hrs);
//		if (!ensureBLEExists())
//			finish();
		setGUI();
	}

	@Override
	protected void onInitialize(final Bundle savedInstanceState) {
		LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, makeIntentFilter());

		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(VALUE))
				mValueC = savedInstanceState.getInt(VALUE);
				mPosC = savedInstanceState.getString(POSITION);
		}
	}


	private void setGUI() {
		//mLineGraph = LineGraphView.getLineGraphView();
		GraphView graph = (GraphView) findViewById(R.id.graph_hrs);
		GridLabelRenderer glr = graph.getGridLabelRenderer();

		glr.setPadding(64); // should allow for 3 digits to fit on screen
		glr.setGridColor(R.color.white);

		//series.setDrawBackground(true);
		series.setBackgroundColor(R.color.moyoSecondary);
		graph.addSeries(series);
		graph.getGridLabelRenderer().setVerticalAxisTitle("");

		graph.getViewport().setXAxisBoundsManual(true);
		graph.getViewport().setMinX(0);
		graph.getViewport().setMaxX(10);
		// set manual Y bounds
		graph.getViewport().setYAxisBoundsManual(true);
		graph.getViewport().setMinY(-10000);
		graph.getViewport().setMaxY(10000);

		/*
		StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
		staticLabelsFormatter.setHorizontalLabels(new String[] {"   ", "   "});
		staticLabelsFormatter.setVerticalLabels(new String[] {"   ", "   "});
		graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
		*/

		graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
		graph.getViewport().setScalableY(false);

		mHRSValue = (TextView) findViewById(R.id.text_hrs_value);
		mHRSPosition = (TextView) findViewById(R.id.text_hrs_position);

		//Set this to be invisible
		//mHRSPosition.setVisibility(View.INVISIBLE);
		//mHRSValue.setVisibility(View.INVISIBLE);
		showGraph();
	}

	private void showGraph() {
		//mGraphView = mLineGraph.getView(this);
		//ViewGroup layout = (ViewGroup) findViewById(R.id.graph_hrs);
		//layout.addView(mGraphView);
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
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

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

	private void setHRSValue(final int value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (value >= MIN_POSITIVE_VALUE && value <= MAX_HR_VALUE) {
					mHRSValue.setText(Integer.toString(value));
				} else {
					mHRSValue.setText(R.string.not_available_value);
				}

			}
		});
	}

	private void setHRSValueOnView() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//short realVal[] = new short[10];
				while(!fifo.isEmpty()) {
					//realVal = convertNumber(value);
					int firstByte = fifo.remove();
					int secondByte = fifo.remove();
					short val = twoBytesToShort((byte) secondByte, (byte) firstByte);

					mTimeCounter += 1d;
					//updateData(value);
					series.appendData(new DataPoint(mTimeCounter/30, (double) val), true, 1000);
					mHandler.postDelayed(this, 1000);
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

				if (position.equals("Danger")) {

					SharedPreferences settings = getSharedPreferences("ProfileData", MODE_PRIVATE);

					String n = settings.getString("nameKey", "Missing");
					String g = settings.getString("genderKey", "Missing");
					String a = settings.getString("ageKey", "Missing");
					String m = settings.getString("medKey", "Missing");

					String phoneNo = "3039416813";
					String message = String.format("%s is going into cardiac arrest.\nGender: %s\nAge: %s\nMedical Information:\n%s", n, g, a, m);
					if (phoneNo.length() > 0 && message.length() > 0) {
						sendSMS(phoneNo, message);
						Snackbar.make(findViewById(R.id.myCoordinatorLayout), "Emergency services contacted",
								Snackbar.LENGTH_SHORT)
								.show();
					}
					else
						Toast.makeText(getBaseContext(), "Please enter both phone number and message.", Toast.LENGTH_SHORT).show();
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
		//mLineGraph.clearGraph();
		//mGraphView.repaint();
		mCounter = 0;
		mHrmValue = 0;
	}

	public static short twoBytesToShort(byte b1, byte b2) {
		return (short) ((b1 << 8) | (b2 & 0xFF));
	}

	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();

			if (HRSService.BROADCAST_HRS_MEASUREMENT.equals(action)) {
				final byte value[] = intent.getByteArrayExtra(HRSService.HRS_VALUE);
				final String position = intent.getStringExtra(HRSService.HRS_POSITION);
				final int hrVal = intent.getIntExtra(HRSService.NEW_HRS_VALUE, 0);
				// Update GUI
				if(position != null)
					setHRSPositionOnView(position);
				if(value != null) {
					for (int i = 0; i < value.length; i++)
						fifo.add(new Integer (value[i]));
					//while(!fifo.isEmpty()){
						setHRSValueOnView();
					//}
				}
				if(hrVal > 0) {
					setHRSValue(hrVal);
				}
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

	private static IntentFilter makeIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(HRSService.BROADCAST_HRS_MEASUREMENT);
		return intentFilter;
	}

	private boolean ensureBLEExists() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
}
