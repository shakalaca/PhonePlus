package com.corner23.android.phoneplus;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhonePlus extends Service implements SensorEventListener {
	private final static String TAG = "RhonePlus";
	
    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private Sensor mProximitySensor;
    
    private final static int MAX_VOLUME = 7;
    private final static int MIN_VOLUME = 1;
    private AudioManager mAudioManager;
    private int nRingVolumeOrigin = -1;
    private int nIncallVolumeOrigin = -1;
    
    private TelephonyManager mTelephonyManager;
	private boolean bPrepareToAnswer = false;
    private boolean bRinging = false;
	private boolean bInCall = false;
	private boolean bInBag = false;
	private boolean bInDark = false;
	private boolean bOobab = false;		// out of bag at beginning
	private boolean bOutgoingCall = false;
	
	private SharedPreferences settings;
	private boolean bPocketMode = false;
	private boolean bPoliteMode = false;
	
//	private int nUnSilentCallCount = 0;
//	private long nStartTime = 0;
//	private boolean bUnSilentTimer = false;
	
	private final static int WAVE_INTERVAL = 200;
	private long lLastWaveTime = 0;
	private int nWaveCount = 0;
	private boolean bInsilent = false;
	
	private boolean bRegOutgoingBR = false;

	final Handler mHandler = new Handler(); 
	
	private void setRingVolume(int volume) {
		if (mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			return;
		}
		
		mAudioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
		Log.d(TAG, "Volume set to : " + volume);
	}

	private void setIncallVolume(int volume) {
		mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0);
	}
	
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        private Sensor mAccelerometer;

    	private void readPreferences() {
			bPocketMode = settings.getBoolean(Preference.PREF_POCKET_MODE, false);
			bPoliteMode = settings.getBoolean(Preference.PREF_POLITE_MODE, false);
			
			nRingVolumeOrigin = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
			nIncallVolumeOrigin = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
    	}
    	
        private void registerAccSensorListener() {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (mAccelerometer != null) {
            	mSensorManager.registerListener(PhonePlus.this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
        
        private void unregisterAccSensorListener() {
    		if (mAccelerometer != null) {
    			mSensorManager.unregisterListener(PhonePlus.this, mAccelerometer);
    			mAccelerometer = null;
    		}
    	}
        
        private void initState() {
			bRinging = false;
			bOobab = false;
			bPrepareToAnswer = false;
			bInsilent = false;
			bOutgoingCall = false;
			setRingVolume(nRingVolumeOrigin);
        }
    	
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
			case TelephonyManager.CALL_STATE_RINGING:
				Log.d(TAG, "CALL_STATE_RINGING");
				readPreferences();
				bRinging = true;
				bInCall = false;
				bInsilent = false;
				Log.d(TAG, "dark:" + bInDark + ", bag;" + bInBag);
				bOobab = !bInDark && !bInBag;
				if (bPocketMode && !bOobab) {
					setRingVolume(MAX_VOLUME);
				}				
				
				if (bPoliteMode) {
					registerAccSensorListener();
				}
				
//				if (nUnSilentCallCount == 3) {
//					mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
//					mAudioManager.setStreamVolume(AudioManager.STREAM_RING, MAX_VOLUME, 0);
//					nUnSilentCallCount = 0;
//					bUnSilentTimer = false;
//				}
//				
//				if (true) {
//					Time time = new Time();
//					time.setToNow();
//					nStartTime = time.toMillis(true);
//					bUnSilentTimer = true;
//					Log.d(TAG, "unsilent start");
//				} else {
//					nUnSilentCallCount = 0;
//					bUnSilentTimer = false;
//					Log.d(TAG, "unsilent stop");
//				}
				break;
				
			case TelephonyManager.CALL_STATE_OFFHOOK:
//				Log.d(TAG, "CALL_STATE_OFFHOOK");
				if (!bOutgoingCall) {
					if (bPoliteMode) {
						unregisterAccSensorListener();
					}
					bInCall = true;
					initState();
				}
				break;
				
			case TelephonyManager.CALL_STATE_IDLE:
//				Log.d(TAG, "CALL_STATE_IDLE");
			default:
				if (bPoliteMode) {
					unregisterAccSensorListener();
				}
				bInCall = false;
				initState();

//				if (bUnSilentTimer) {
//					Time time = new Time();
//					time.setToNow();
//					long delta = time.toMillis(true) - nStartTime;
//					if (delta < 1000 * 5) {
//						nUnSilentCallCount++;
//					}
//					
//					Log.d(TAG, "count:" + nUnSilentCallCount);
//				}
				break;
			}
			super.onCallStateChanged(state, incomingNumber);
		}
    };

    private final BroadcastReceiver mOutgoingCallBroadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				Log.d(TAG, "ACTION_NEW_OUTGOING_CALL");
				bOutgoingCall = true;
			}
		}
    };
    	
    @Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if (settings == null) {
			settings = getSharedPreferences(Preference.SHARED_PREFS_NAME, 0);
		}
		
		if (mAudioManager == null) {
			mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);		
			nRingVolumeOrigin = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
		}
		
		if (mSensorManager == null) {
			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
			
	        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);        
	        if (mLightSensor != null) {
	        	mSensorManager.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
	        }
	        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
	        if (mProximitySensor != null) {
	        	mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
	        }
		}
		
		if (mTelephonyManager == null) {
			mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}
		
		if (!bRegOutgoingBR) {
			IntentFilter filter = new IntentFilter();  
			filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
			registerReceiver(mOutgoingCallBroadcastReceiver, filter);
			bRegOutgoingBR = true;
		}
	}
    
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mLightSensor != null) {
			mSensorManager.unregisterListener(this, mLightSensor);
			mLightSensor = null;
		}
		if (mProximitySensor != null) {
			mSensorManager.unregisterListener(this, mProximitySensor);
			mProximitySensor = null;
		}
		mSensorManager = null;

		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		mTelephonyManager = null;
		
		mAudioManager = null;
		
		if (bRegOutgoingBR) {
			unregisterReceiver(mOutgoingCallBroadcastReceiver);
			bRegOutgoingBR = false;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
//		int sensorType = sensor.getType();
//		
//		switch (sensorType) {
//		case Sensor.TYPE_LIGHT:
//			Log.d(TAG, "LIGHT SENSOR");
//			break;
//			
//		case Sensor.TYPE_PROXIMITY:
//			Log.d(TAG, "PROXIMITY SENSOR");
//			break;
//			
//		case Sensor.TYPE_ACCELEROMETER:
//			Log.d(TAG, "ACCELEROMETER SENSOR");
//			break;
//			
//		default:
//			Log.d(TAG, "OTHER SENSOR: " + sensorType);
//		}
//		
//		Log.d(TAG, "ACCURACY:" + accuracy);
	}

	private void processLightSensorEvent(float light) {
		Log.d(TAG, "LIGHT SENSOR:" + light);
		if (light < 10.0) {
			bInDark = true;
		} else {
			bInDark = false;
		}
	}
	
	private void processProximitySensorEvent(float proximity) {
		Log.d(TAG, "PROXIMITY SENSOR:" + proximity);
		if (proximity == 0.0) {
			bInBag = true;
		} else {
			bInBag = false;
		}
	}
	
	private void processAccelerometerSensorEvent(float x, float y, float z) {
		Log.d(TAG, "ACCELEROMETER SENSOR:" + x + "/" + y + "/" + z);
		if (y > 4.0) {
			bPrepareToAnswer = true;
		} 
	}
	
	private void postSensorEvent() {
		if (bRinging) {			
			if (bInsilent) {							// user wave to silent
				mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				// setRingVolume(0);
			} else if (!bInDark && !bInBag) {			// phone is ringing, and is out of bag
				// if user rise the phone, lower the volume
				if (bPrepareToAnswer && bPoliteMode) {
					setRingVolume(MIN_VOLUME);
				} else {
					setRingVolume(nRingVolumeOrigin);
				}
			}
		} else if (bInCall) {
			// if phone close to face, use origin volume
			if (bInBag) {
				setIncallVolume(nIncallVolumeOrigin);				
			} else {
				nIncallVolumeOrigin = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
				setIncallVolume(MAX_VOLUME);
			}
		}
	}
	
	private void waveToSilentCheck() {
		int nWaveTimes = 3;
		
		if (bRinging && bOobab && !bInsilent) {
			long currTime = System.currentTimeMillis();
			if (lLastWaveTime != 0) {
				if (currTime - lLastWaveTime < WAVE_INTERVAL) {
					if (++nWaveCount == (nWaveTimes * 2 - 1)) {
						nWaveCount = 0;
						bInsilent = true;
					}
				} else {
					nWaveCount = 0;
				}
			}
			lLastWaveTime = currTime;
//			Log.d(TAG, "wave count: " + nWaveCount);
		}
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		int sensorType = event.sensor.getType();
		
		switch (sensorType) {
		case Sensor.TYPE_LIGHT:
			processLightSensorEvent(event.values[0]);
			break;
			
		case Sensor.TYPE_PROXIMITY:
			processProximitySensorEvent(event.values[0]);
			waveToSilentCheck();
			break;
			
		case Sensor.TYPE_ACCELEROMETER:
			processAccelerometerSensorEvent(event.values[0], event.values[1], event.values[2]);
			break;
			
		default:
//			Log.d(TAG, "OTHER SENSOR: " + sensorType);
		}
		
		postSensorEvent();
	}
}
