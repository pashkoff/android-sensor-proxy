package org.pilin.sensorTest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.pilin.sensor.SensorProtos;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SensorTestActivity extends Activity implements SensorEventListener {

	private final String TAG = "SensorTestActivity";

	private SensorManager msensorManager;

	private float[] accelData = new float[3];
	private float[] gravityData = new float[3];
	private float[] gyroData = new float[3];
	private float[] linacData = new float[3];
	private float[] rotvecData = new float[4];
	private float[] proxymData = new float[1];

	private TextView xText;
	private TextView yText;
	private TextView zText;
	private TextView socketText;
	private Button connectBtn;
	private TextView connStateLbl;

	private final int PORT = 48912;
	private String address;

	enum ConnectionState {
		DISCONNECTED, CONNECTING, CONNECTED
	}

	ConnectionState mConnectionState;

	DatagramSocket mSocket;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		msensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		accelData = new float[3];

		xText = (TextView) findViewById(R.id.xText);
		yText = (TextView) findViewById(R.id.yText);
		zText = (TextView) findViewById(R.id.zText);
		socketText = (TextView) findViewById(R.id.socketText);
		socketText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				disconnect();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		connectBtn = (Button) findViewById(R.id.connectBtn);
		connectBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				address = socketText.getText().toString();

				switch (mConnectionState) {
				case CONNECTED:
				case CONNECTING:
					disconnect();
					break;
				case DISCONNECTED:
					connect();
				}

			}
		});
		
		connStateLbl = (TextView)findViewById(R.id.conStateLbl);

		changeConnectionState(ConnectionState.DISCONNECTED);
	}

	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();

		disconnect();
	}

	// /

	private void activateSensor() {
		if (!msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)) {
			Log.w(TAG, "TYPE_ACCELEROMETER err");
		}
		if (!msensorManager.registerListener(this, msensorManager.getDefaultSensor(/*Sensor.TYPE_GRAVITY*/9), SensorManager.SENSOR_DELAY_NORMAL)) {
			Log.w(TAG, "TYPE_GRAVITY err");
		}
		if (!msensorManager.registerListener(this, msensorManager.getDefaultSensor(/*Sensor.TYPE_LINEAR_ACCELERATION*/10), SensorManager.SENSOR_DELAY_NORMAL)) {
			Log.w(TAG, "TYPE_LINEAR_ACCELERATION err");
		}
		if (!msensorManager.registerListener(this, msensorManager.getDefaultSensor(/*Sensor.TYPE_ROTATION_VECTOR*/11), SensorManager.SENSOR_DELAY_NORMAL)) {
			Log.w(TAG, "TYPE_ROTATION_VECTOR err");
		}
		if (!msensorManager.registerListener(this, msensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL)) {
			Log.w(TAG, "TYPE_PROXIMITY err");
		}
		
		List<Sensor> sensorList = msensorManager.getSensorList(Sensor.TYPE_ALL);
		for (Sensor s : sensorList)
		{
			Log.i(TAG, s.toString() + " " + s.getName() + " " + s.getType());
		}
	}

	private void deactivateSensor() {
		msensorManager.unregisterListener((SensorEventListener) this);
	}

	private void loadNewSensorData(SensorEvent event) {
		final int type = event.sensor.getType();
		
		switch (type)
		{
		case Sensor.TYPE_ACCELEROMETER:
			accelData = event.values.clone();
			break;
		case 9: // gravity
			gravityData = event.values.clone();
			break;
		case Sensor.TYPE_GYROSCOPE:
			gyroData = event.values.clone();
			break;
		case 10: // linear accel
			linacData = event.values.clone();
			break;
		case 11: // rot vec
			rotvecData = event.values.clone();
			break;
		case Sensor.TYPE_PROXIMITY:
			proxymData = event.values.clone();
			break;
		default:
			Log.v(TAG, "unknown sensor " + type);
		}
		
		
	}

	public void onSensorChanged(SensorEvent event) {
		loadNewSensorData(event);

		// Выводим результат
		xText.setText(String.valueOf(accelData[0]));
		yText.setText(String.valueOf(accelData[1]));
		zText.setText(String.valueOf(accelData[2]));

		sendSensorData(accelData);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	// /

	private void changeConnectionState(ConnectionState cs) {
		String[] conStateStrgs = getResources().getStringArray(R.array.connStates);
		connStateLbl.setText(conStateStrgs[cs.ordinal()]);

		mConnectionState = cs;

		switch (cs) {
		case CONNECTED:
			activateSensor();
			break;
		case DISCONNECTED:
			deactivateSensor();
			break;
		case CONNECTING:
			break;
		}
	}

	private void connect() {

		if (mSocket != null) {
			Log.w(TAG, "Already connected");
			return;
		}

		try {
			mSocket = new DatagramSocket();
			
			changeConnectionState(ConnectionState.CONNECTED);
			
		} catch (SocketException e) {
			Log.w(TAG, e);
			assert (mSocket == null);
			
			Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
		}
	}

	private void disconnect() {
		changeConnectionState(ConnectionState.DISCONNECTED);

		if (mSocket == null) {
			Log.w(TAG, "Already disconnected");
			return;
		}
		mSocket.close();
		mSocket = null;
	}
	
	private boolean isConnected()
	{
		return mSocket != null;
	}

	private void sendSensorData(float[] data) {
		if (!isConnected())
			return;
		
		SensorProtos.Accel acc = SensorProtos.Accel.newBuilder().setX(data[0]).setY(data[1]).setZ(data[2]).build();

		byte[] ba = acc.toByteArray();

		try {
			InetAddress addr = InetAddress.getByName(address);
			DatagramPacket dp = new DatagramPacket(ba, ba.length, addr, PORT);

			mSocket.send(dp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.w(TAG, e);
		}
	}

}