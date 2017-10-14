/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.settings;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.Phone;

import java.util.ArrayList;
import java.util.List;

public class NoNetworkPopUpService extends Service {
    private static final String TAG = "Settings/NoNetworkPopUpService";
    private static final boolean DBG = true;
    private boolean mAirplaneModeEnabled = false;
    private int mDualSimMode = -1;
    private int mReminderType = 0;
    private int mSimId = 0;
    public static final String NO_SERVICE = "no_service";
    private SharedPreferences mSP; 

    private final int NETWORK_POP_UP_MSG = 0;
    private final int NETWORK_POP_UP_MSG_SIM_1 = 1;
    private final int NETWORK_POP_UP_MSG_SIM_2 = 2;

    private boolean isShouldShow = true;
    private int mDelayTime = 0;
    private final int DELAY_TIME = 2 * 60 * 1000;
    private final String DELAY_TIME_KEY = "delay_time_key";
    public static final String NO_SERVICE_KEY = "no_service_key";

    private TelephonyManager mTelephonyManager;
    private IntentFilter mIntentFilter;

    private PhoneStateListener mPhoneServiceListener = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState){
            mSimId = serviceState.getMySimId();

            log("[state = "+serviceState.getRegState()+"]");
            log("[isShouldShow = "+isShouldShow+"]");
            if(!serviceState.getIsManualSelection()){
                return;
            }
            if(serviceState.getRegState() == ServiceState.REGISTRATION_STATE_UNKNOWN
                    || serviceState.getRegState() == ServiceState.REGISTRATION_STATE_HOME_NETWORK
                    || serviceState.getRegState() == ServiceState.REGISTRATION_STATE_ROAMING
                    || serviceState.getRegState() == ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING){
                if (CallSettings.isMultipleSim()) {
                    if(mSimId == Phone.GEMINI_SIM_1){
                        mNetworkResponse.removeMessages(NETWORK_POP_UP_MSG_SIM_1);
                    } else {
                        mNetworkResponse.removeMessages(NETWORK_POP_UP_MSG_SIM_2);
                    }    
                } else {
                    mNetworkResponse.removeMessages(NETWORK_POP_UP_MSG);
                }
            } else if(serviceState.getRegState() == ServiceState.REGISTRATION_STATE_REGISTRATION_DENIED
                    || serviceState.getRegState() == ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING){
                if (CallSettings.isMultipleSim()) {
                    if(mSimId == Phone.GEMINI_SIM_1){
                        mNetworkResponse.sendEmptyMessageDelayed(NETWORK_POP_UP_MSG_SIM_1, mDelayTime);
                    } else {
                        mNetworkResponse.sendEmptyMessageDelayed(NETWORK_POP_UP_MSG_SIM_2, mDelayTime);
                    }
                }else{
                    mNetworkResponse.sendEmptyMessageDelayed(NETWORK_POP_UP_MSG, mDelayTime);
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("[action = "+action+"]");
            if(Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
            } else if(Intent.ACTION_DUAL_SIM_MODE_CHANGED.equals(action)){
                mDualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        log("[create network pop up service]");

        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        if(CallSettings.isMultipleSim()){
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }

        registerReceiver(mReceiver, mIntentFilter);
        mSP = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        if(mSP.contains(DELAY_TIME_KEY)){
            mDelayTime = mSP.getInt(DELAY_TIME_KEY, DELAY_TIME);
        } else {
            SharedPreferences.Editor editor = mSP.edit(); 
            editor.putInt(DELAY_TIME_KEY, DELAY_TIME);
            editor.commit();
            mDelayTime = DELAY_TIME;
        }
        if(mSP.contains(NO_SERVICE_KEY)){
            isShouldShow = mSP.getBoolean(NO_SERVICE_KEY, true);
        } else {
            SharedPreferences.Editor editor = mSP.edit(); 
            editor.putBoolean(NO_SERVICE_KEY, false);
            editor.commit();
            isShouldShow = true;
        }

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneServiceListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        mAirplaneModeEnabled = Settings.System.getInt(getApplicationContext().getContentResolver(), Settings.System.AIRPLANE_MODE_ON, -1) == 1;
        if(CallSettings.isMultipleSim()){
            mDualSimMode = Settings.System.getInt(getApplicationContext().getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, -1);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("[destroy network pop up service]");
        mNetworkResponse.removeMessages(NETWORK_POP_UP_MSG);
        mNetworkResponse.removeMessages(NETWORK_POP_UP_MSG_SIM_1);
        mNetworkResponse.removeMessages(NETWORK_POP_UP_MSG_SIM_2);
        unregisterReceiver(mReceiver);
    }

    private final Handler mNetworkResponse = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            isShouldShow = mSP.getBoolean(NO_SERVICE_KEY, true);
            log("[isShouldShow = "+isShouldShow+"]");
            log("[mAirplaneModeEnabled = "+mAirplaneModeEnabled+"]");
            log("[mDualSimMode = "+mDualSimMode+"]");
            log("[message id = "+msg.what+"]");

            if(!isShouldShow && !mAirplaneModeEnabled){
                switch(msg.what) {
                case NETWORK_POP_UP_MSG:
                    if(isSimReady(Phone.GEMINI_SIM_1)){
                        startNWActivity(msg.what, Phone.GEMINI_SIM_1);
                    }
                    break;
                case NETWORK_POP_UP_MSG_SIM_1:
                    if((mDualSimMode & 0x1) == 0x1 && isSimReady(Phone.GEMINI_SIM_1)){
                        startNWActivity(msg.what, Phone.GEMINI_SIM_1);
                    }
                    break;
                case NETWORK_POP_UP_MSG_SIM_2:
                    if((mDualSimMode & 0x2) == 0x2 && isSimReady(Phone.GEMINI_SIM_2)){
                        startNWActivity(msg.what, Phone.GEMINI_SIM_2);
                    }
                    break;
                default:
                    break;
                }
            }
            mNetworkResponse.sendEmptyMessageDelayed(msg.what, mDelayTime);
        }
    };

    private void startNWActivity(int msg, int simId){
        Intent it = new Intent();
        it.putExtra(NO_SERVICE, true);
        if(CallSettings.isMultipleSim()){
            it.putExtra(Phone.GEMINI_SIM_ID_KEY, simId);
        }
        it.setClassName("com.android.phone", "com.android.phone.NetworkSetting");
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(it);
    }

    private boolean isSimReady(int simId){
        boolean isReady = true;

        if(CallSettings.isMultipleSim()){
            isReady = (mTelephonyManager.getSimStateGemini(simId) == TelephonyManager.SIM_STATE_READY);
        } else {
            isReady = (mTelephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY);
        }
        return isReady;
    }

    private void log(String msg) {
        Log.d(TAG, "[NoNetworkPopUpService]"+msg);
    }
}
