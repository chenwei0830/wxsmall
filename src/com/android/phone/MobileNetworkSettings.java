/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

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

package com.android.phone;

import java.util.ArrayList;
import java.util.List;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ThrottleManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.provider.Telephony.SIMInfo;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.settings.SimItem;
import com.mediatek.common.featureoption.FeatureOption;

import android.content.BroadcastReceiver;
import android.telephony.TelephonyManager;
import android.content.Context;

import com.mediatek.settings.DefaultSimPreference;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.CellConnService.CellConnMgr;

import com.mediatek.phone.ext.SettingsExtension;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.settings.MultipleSimActivity;
import com.mediatek.settings.PreCheckForRunning;
import com.mediatek.settings.CallSettings;
/**
 * "Mobile network settings" screen.  This preference screen lets you
 * enable/disable mobile data, and control data roaming and other
 * network-specific mobile data features.  It's used on non-voice-capable
 * tablets as well as regular phone devices.
 *
 * Note that this PreferenceActivity is part of the phone app, even though
 * you reach it from the "Wireless & Networks" section of the main
 * Settings app.  It's not part of the "Call settings" hierarchy that's
 * available from the Phone app (see CallFeaturesSetting for that.)
 */
public class MobileNetworkSettings extends PreferenceActivity
        implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener{
    // debug data
    private static final String LOG_TAG = "NetworkSettings";
    private static final boolean DBG = true;
    public static final int REQUEST_CODE_EXIT_ECM = 17;

    //String keys for preference lookup
    private static final String BUTTON_DATA_ENABLED_KEY = "button_data_enabled_key";
    private static final String BUTTON_DATA_USAGE_KEY = "button_data_usage_key";
    private static final String BUTTON_PREFERED_NETWORK_MODE = "preferred_network_mode_key";
    private static final String BUTTON_ROAMING_KEY = "button_roaming_key";
    private static final String BUTTON_CDMA_LTE_DATA_SERVICE_KEY = "cdma_lte_data_service_key";

    ///M: add for data connection under gemini
    private static final String KEY_DATA_CONN = "data_connection_setting";
    static final int preferredNetworkMode = Phone.PREFERRED_NT_MODE;

    //Information about logical "up" Activity
    private static final String UP_ACTIVITY_PACKAGE = "com.android.settings";
    private static final String UP_ACTIVITY_CLASS =
            "com.android.settings.Settings$WirelessSettingsActivity";

    //UI objects
    private CheckBoxPreference mButtonDataRoam;
    private CheckBoxPreference mButtonDataEnabled;
    private Preference mLteDataServicePref;
    ///M: add for data conn feature @{
    private DefaultSimPreference mDataConnPref = null;
    ///@}
    private Preference mButtonDataUsage;
    private DataUsageListener mDataUsageListener;
    private static final String iface = "rmnet0"; //TODO: this will go away
    private Phone mPhone;
    private MyHandler mHandler;
    private boolean mOkClicked;
    private SettingsExtension mExtension;
    //GsmUmts options and Cdma options
    GsmUmtsOptions mGsmUmtsOptions;
    CdmaOptions mCdmaOptions;

    private Preference mClickedPreference;
    /// M: add for gemini support @{
    private static final String BUTTON_GSM_UMTS_OPTIONS = "gsm_umts_options_key";
    private static final String BUTTON_CDMA_OPTIONS = "cdma_options_key";
    private static final String BUTTON_APN = "button_apn_key";
    private static final String BUTTON_CARRIER_SEL = "button_carrier_sel_key";
    private static final String BUTTON_3G_SERVICE = "button_3g_service_key";
    private static final String BUTTON_PLMN_LIST = "button_plmn_key";
    private static final String BUTTON_2G_ONLY = "button_prefer_2g_key";
    private static final String BUTTON_NETWORK_MODE_EX_KEY = "button_network_mode_ex_key";
    private static final String BUTTON_NETWORK_MODE_KEY = "gsm_umts_preferred_network_mode_key";
    private static final String DATA_PREFER_KEY = "data_prefer_key";

    public static final int PCH_DATA_PREFER = 0;
    public static final int PCH_CALL_PREFER = 1;
    private int mSimId;
    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SIM_CARD_SIGNAL = 2;
    private static final int PIN1_REQUEST_CODE = 302;

    private static final int DATA_ENABLE_ALERT_DIALOG = 100;
    private static final int DATA_DISABLE_ALERT_DIALOG = 200;
    private static final int ROAMING_DIALOG = 300;
    private static final int PROGRESS_DIALOG = 400;
    private static final int MOBILE_DATA_PREF_DIALOG = 500;

    private Preference mButtonPreferredNetworkModeEx;
    private ListPreference mButtonPreferredNetworkMode;
    private Preference mPreference3GSwitch;
    private Preference mPLMNPreference;
    private CheckBoxPreference mButtonPreferredGSMOnly;
    private GeminiPhone mGeminiPhone;
    private PreferenceScreen mApnPref;
    private PreferenceScreen mCarrierSelPref;
    private PreCheckForRunning mPreCheckForRunning;
    private CheckBoxPreference mMobileDataPref;

    private static final int MODEM_MASK_GPRS = 0x01;
    private static final int MODEM_MASK_EDGE = 0x02;
    private static final int MODEM_MASK_WCDMA = 0x04;
    private static final int MODEM_MASK_TDSCDMA = 0x08;
    private static final int MODEM_MASK_HSDPA = 0x10;
    private static final int MODEM_MASK_HSUPA = 0x20;
    private CellConnMgr mCellConnMgr;
    private TelephonyManager mTelephonyManager;

    private ITelephony iTelephony;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            log("onCallStateChanged ans state is "+state);
            switch(state){
            case TelephonyManager.CALL_STATE_IDLE:
                setScreenEnabled();
                break;
            default:
                break;
            }
        }
    };

    private boolean mAirplaneModeEnabled = false;
    private int mDualSimMode = -1;
    private IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); //Added by vend_am00015 2010-06-07
            if(action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                setScreenEnabled();
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED) && isChangeData) {
                Log.d(LOG_TAG, "catch data change!");
                Phone.DataState state = getMobileDataState(intent);
                String apnTypeList = intent.getStringExtra(Phone.DATA_APN_TYPE_KEY);
                Log.d(LOG_TAG, "apnTypeList="+apnTypeList);
                Log.d(LOG_TAG,"state="+state);
                if ((Phone.APN_TYPE_DEFAULT.equals(apnTypeList) && state == Phone.DataState.CONNECTED) 
                    || (state == Phone.DataState.DISCONNECTED)) {
                    mH.removeMessages(DATA_STATE_CHANGE_TIMEOUT);
                    removeDialog(PROGRESS_DIALOG);
                    isChangeData = false;
                    setDataConnPref();
                }
            }else if(action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)){
                mDualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1);
                setScreenEnabled();
            }else if(TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY.equals(action)){
                setNetworkOperator();
            }else if(action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)){
                Log.d(LOG_TAG,"indicator state changed");
                setDataConnPref();
            } else if (PhoneApp.NETWORK_MODE_CHANGE_RESPONSE.equals(action)) {
                if (!intent.getBooleanExtra(PhoneApp.NETWORK_MODE_CHANGE_RESPONSE, true)) {
                        Log.d(LOG_TAG,"network mode change failed! restore the old value.");
                        int oldMode = intent.getIntExtra(PhoneApp.OLD_NETWORK_MODE, 0);
                        Log.d(LOG_TAG,"oldMode = " + oldMode);
                        android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                                oldMode);
                    }
            }
        }
    };
    public static int DATA_STATE_CHANGE_TIMEOUT = 2001;
    private boolean isChangeData = false;
    Handler mH = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == DATA_STATE_CHANGE_TIMEOUT) {
                removeDialog(PROGRESS_DIALOG);
                isChangeData = false;
                setDataConnPref();
            }
        }
    };
    /// @}
    //This is a method implemented for DialogInterface.OnClickListener.
    //  Used to dismiss the dialogs when they come up.
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mPhone.setDataRoamingEnabled(true);
            mOkClicked = true;
        } else {
            // Reset the toggle
            mButtonDataRoam.setChecked(false);
        }
    }

    public void onDismiss(DialogInterface dialog) {
        // Assuming that onClick gets called first
        if (!mOkClicked) {
            mButtonDataRoam.setChecked(false);
        }
    }

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        /** TODO: Refactor and get rid of the if's using subclasses */
        /// M: add for plmn prefer feature for CTA test & gemini rat mode @{       
        if (preference == mPLMNPreference) {
            if (CallSettings.isMultipleSim()) {
                Intent intent = new Intent(this, MultipleSimActivity.class);
                intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
                intent.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
                intent.putExtra(MultipleSimActivity.targetClassKey, "com.mediatek.settings.PLMNListPreference");
                mPreCheckForRunning.checkToRun(intent, mSimId, 302);
                return true;
            }else {
                return false;
            }
        } else if (preference == mButtonPreferredNetworkModeEx) {
            CharSequence[] entries;
            CharSequence[] entriesValue; 
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.intentKey, "ListPreference");
            if((getBaseBand(SIM_CARD_1) & MODEM_MASK_TDSCDMA) != 0){
                entries = getResources().getStringArray(R.array.gsm_umts_network_preferences_choices_cmcc);
                entriesValue = getResources().getStringArray(R.array.gsm_umts_network_preferences_values_cmcc);
            }else{
                entries = getResources().getStringArray(R.array.gsm_umts_network_preferences_choices);
                entriesValue = getResources().getStringArray(R.array.gsm_umts_network_preferences_values);
            }
            intent.putExtra(MultipleSimActivity.initArray, entries);
            intent.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            intent.putExtra(MultipleSimActivity.LIST_TITLE, getResources().getString(R.string.gsm_umts_network_preferences_title));
            intent.putExtra(MultipleSimActivity.initFeatureName, "NETWORK_MODE");

            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, SIM_CARD_1);
            long[] simIds = new long[1];
            simIds[0] = info != null ? info.mSimId : 0;

            intent.putExtra(MultipleSimActivity.initSimId, simIds);
            intent.putExtra(MultipleSimActivity.initBaseKey, "preferred_network_mode_key@");
            intent.putExtra(MultipleSimActivity.initArrayValue, entriesValue);
            mPreCheckForRunning.checkToRun(intent, mSimId, 302);
            return true;
        }
        /// @}       
        
        if (mGsmUmtsOptions != null &&
                mGsmUmtsOptions.preferenceTreeClick(preference)) {
            return true;
        } else if (mCdmaOptions != null &&
                   mCdmaOptions.preferenceTreeClick(preference)) {
            if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {

                mClickedPreference = preference;

                // In ECM mode launch ECM app dialog
                startActivityForResult(
                    new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                    REQUEST_CODE_EXIT_ECM);
            }
            return true;
        } else if (preference == mButtonPreferredNetworkMode) {
            //displays the value taken from the Settings.System
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
                    getContentResolver(), android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            return true;
        } else if (preference == mButtonDataRoam) {
            if (DBG) log("onPreferenceTreeClick: preference == mButtonDataRoam.");

            //normally called on the toggle click
            if (mButtonDataRoam.isChecked()) {
                // First confirm with a warning dialog about charges
                mOkClicked = false;
                showDialog(ROAMING_DIALOG);
            } else {
                mPhone.setDataRoamingEnabled(false);
            }
            return true;
        } else if (preference == mButtonDataEnabled) {
            if (DBG)log("onPreferenceTreeClick: preference == mButtonDataEnabled.");
            if (!mExtension.dataEnableReminder(mButtonDataEnabled, this)) {
                Log.d(LOG_TAG,"onPreferenceTreeClick: preference == mButtonDataEnabled.");
                if (mButtonDataEnabled.isChecked() && isSimLocked()) {
                    mCellConnMgr.handleCellConn(0, PIN1_REQUEST_CODE);    
                    Log.d(LOG_TAG,"Data enable check change request pin single card");
                    mButtonDataEnabled.setChecked(false);
                } else {
                    isChangeData = true;
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                    if(networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()){
                    }else{
                        showDialog(PROGRESS_DIALOG);
                    }
                    cm.setMobileDataEnabled(mButtonDataEnabled.isChecked());
                    mH.sendMessageDelayed(mH.obtainMessage(DATA_STATE_CHANGE_TIMEOUT), 30000);
                    if (mButtonDataEnabled.isChecked()&&
                        isNeedtoShowRoamingMsg()){
                        mExtension.showWarningDlg(this,R.string.data_conn_under_roaming_hint);
                    }
                }
            }
            return true;
        } else if (preference == mLteDataServicePref) {
            String tmpl = android.provider.Settings.Secure.getString(getContentResolver(),
                        android.provider.Settings.Secure.SETUP_PREPAID_DATA_SERVICE_URL);
            if (!TextUtils.isEmpty(tmpl)) {
                String imsi = mTelephonyManager.getSubscriberId();
                if (imsi == null) {
                    imsi = "";
                }
                final String url = TextUtils.isEmpty(tmpl) ? null
                        : TextUtils.expandTemplate(tmpl, imsi).toString();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } else {
                android.util.Log.e(LOG_TAG, "Missing SETUP_PREPAID_DATA_SERVICE_URL");
            }
            return true;
        } else if (preference == mApnPref && CallSettings.isMultipleSim()) {
            //// M: for gemini support @{
            Intent it = new Intent();
            it.setAction("android.intent.action.MAIN");
            it.setClassName("com.android.phone", "com.mediatek.settings.MultipleSimActivity");
            it.putExtra(Phone.GEMINI_SIM_ID_KEY, mSimId);
            it.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            it.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            it.putExtra(MultipleSimActivity.targetClassKey, "com.android.settings.ApnSettings");
            mPreCheckForRunning.checkToRun(it, mSimId, 302);
            return true;
        } else if (preference == mCarrierSelPref && CallSettings.isMultipleSim()) {
            Intent it = new Intent();
            it.setAction("android.intent.action.MAIN");
            it.setClassName("com.android.phone", "com.mediatek.settings.MultipleSimActivity");
            it.putExtra(MultipleSimActivity.initTitleName, preference.getTitle());
            it.putExtra(MultipleSimActivity.intentKey, "PreferenceScreen");
            it.putExtra(MultipleSimActivity.initFeatureName, "NETWORK_SEARCH");
            it.putExtra(MultipleSimActivity.targetClassKey, "com.android.phone.NetworkSetting");
            mPreCheckForRunning.checkToRun(it, mSimId, 302);
            return true;
            //// @}
        } else if (preference == mMobileDataPref) {
            if(mMobileDataPref.isChecked()){
                showDialog(MOBILE_DATA_PREF_DIALOG);
            } else {
                try {
                    Settings.System.putInt(getContentResolver(), Settings.System.GPRS_TRANSFER_SETTING, 1);
                    if(FeatureOption.MTK_GEMINI_SUPPORT){
                        if(iTelephony != null){
                             iTelephony.setGprsTransferTypeGemini(PCH_CALL_PREFER, Phone.GEMINI_SIM_1);
                             iTelephony.setGprsTransferTypeGemini(PCH_CALL_PREFER, Phone.GEMINI_SIM_2);
                        }
                    } else {
                        if(iTelephony != null){
                            iTelephony.setGprsTransferType(PCH_CALL_PREFER);
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            return true;
        } else if (preference == mDataConnPref && CallSettings.isMultipleSim()){
            Log.d(LOG_TAG,"mDataConnPref is clicked");
            return true;

        } else {
            // if the button is anything but the simple toggle preference,
            // we'll need to disable all preferences to reject all click
            // events until the sub-activity's UI comes up.
            preferenceScreen.setEnabled(false);
            // Let the intents be launched by the Preference manager
            return false;
        }
    }

    //// M: is sim locked or not
    private boolean isSimLocked() {
        TelephonyManagerEx telephonyMEx = TelephonyManagerEx.getDefault();
        return telephonyMEx.getSimIndicatorState() ==  Phone.SIM_INDICATOR_LOCKED;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.network_setting);

        mExtension = ExtensionManager.getInstance().getSettingsExtension();

        mPhone = PhoneApp.getPhone();
        /// M: for gemini phone
        if (CallSettings.isMultipleSim()){
            mGeminiPhone = (GeminiPhone)mPhone;
        }
        mHandler = new MyHandler();
        /// M: for receivers sim lock gemini phone @{
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY);
        if(FeatureOption.MTK_GEMINI_SUPPORT){
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        ///M: add to receiver indicator intents@{
        if (isDataConnAvailable()){
            mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        }
        mIntentFilter.addAction(PhoneApp.NETWORK_MODE_CHANGE_RESPONSE); 
        ///@}
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mPreCheckForRunning = new PreCheckForRunning(this);
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);
        if (list.size() == 1) {
            mPreCheckForRunning.byPass = false;
            mSimId = list.get(0).mSlot;
        }else{
            mPreCheckForRunning.byPass = true;
        }
        /// @}
        //get UI object references
        PreferenceScreen prefSet = getPreferenceScreen();
        //M: add data connection for gemini sim project
        mDataConnPref = (DefaultSimPreference) prefSet.findPreference(KEY_DATA_CONN);
        mDataConnPref.setOnPreferenceChangeListener(this);
        if (!isDataConnAvailable()){
            prefSet.removePreference(mDataConnPref);
        }
        //
        mButtonDataEnabled = (CheckBoxPreference) prefSet.findPreference(BUTTON_DATA_ENABLED_KEY);
        mButtonDataRoam = (CheckBoxPreference) prefSet.findPreference(BUTTON_ROAMING_KEY);
        mButtonPreferredNetworkMode = (ListPreference) prefSet.findPreference(
                BUTTON_PREFERED_NETWORK_MODE);
        mButtonDataUsage = prefSet.findPreference(BUTTON_DATA_USAGE_KEY);
        /// M: support gemini phone, 3G switch, PLMN prefer @{     
        if (CallSettings.isMultipleSim()) {
            prefSet.removePreference(mButtonDataEnabled);
            prefSet.removePreference(mButtonDataRoam);
        }
        mPreference3GSwitch = prefSet.findPreference(BUTTON_3G_SERVICE);
        mPLMNPreference = prefSet.findPreference(BUTTON_PLMN_LIST);
        /// @}   
        mLteDataServicePref = prefSet.findPreference(BUTTON_CDMA_LTE_DATA_SERVICE_KEY);
        mButtonPreferredNetworkModeEx = prefSet.findPreference(BUTTON_NETWORK_MODE_EX_KEY);

        boolean isLteOnCdma = mPhone.getLteOnCdmaMode() == Phone.LTE_ON_CDMA_TRUE;
        if (getResources().getBoolean(R.bool.world_phone)) {
            // set the listener for the mButtonPreferredNetworkMode list preference so we can issue
            // change Preferred Network Mode.
            mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);

            //Get the networkMode from Settings.System and displays it
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
                    getContentResolver(),android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
            mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));
            mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
            mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet);
        } else {
            if (!isLteOnCdma) {
                prefSet.removePreference(mButtonPreferredNetworkMode);
            }
            int phoneType = mPhone.getPhoneType();
            if (phoneType == Phone.PHONE_TYPE_CDMA) {
                mCdmaOptions = new CdmaOptions(this, prefSet, mPhone);
                if (isLteOnCdma) {
                    mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
                    mButtonPreferredNetworkMode.setEntries(
                            R.array.preferred_network_mode_choices_lte);
                    mButtonPreferredNetworkMode.setEntryValues(
                            R.array.preferred_network_mode_values_lte);
                    int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                            mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                            preferredNetworkMode);
                    mButtonPreferredNetworkMode.setValue(
                            Integer.toString(settingsNetworkMode));
                }
                /// M: support for cdma @{
                if(!PhoneUtils.isSupportFeature("3G_SWITCH")){
                    if(mPreference3GSwitch != null){
                        prefSet.removePreference(mPreference3GSwitch);
                        mPreference3GSwitch = null;
                    }
                }
                prefSet.removePreference(mButtonPreferredNetworkModeEx);
                mApnPref = (PreferenceScreen) prefSet.findPreference(BUTTON_APN);
                mCarrierSelPref = (PreferenceScreen) prefSet.findPreference(BUTTON_CARRIER_SEL);
                /// @}
            } else if (phoneType == Phone.PHONE_TYPE_GSM) {
                mGsmUmtsOptions = new GsmUmtsOptions(this, prefSet);
                /// M: support for operators @{
                mApnPref = (PreferenceScreen) prefSet.findPreference(BUTTON_APN);
                mButtonPreferredGSMOnly = (CheckBoxPreference) prefSet.findPreference(BUTTON_2G_ONLY);
                mButtonPreferredNetworkMode = (ListPreference)prefSet.findPreference(BUTTON_NETWORK_MODE_KEY);

                /// M: add mobile data prefer preference @{
                if(iTelephony == null){
                    iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                }
                mMobileDataPref = (CheckBoxPreference)prefSet.findPreference(DATA_PREFER_KEY);
                //mMobileDataPref.setOnPreferenceClickListener(this);
                int pchFlag = Settings.System.getInt(this.getContentResolver(), Settings.System.GPRS_TRANSFER_SETTING,
                                Settings.System.GPRS_TRANSFER_SETTING_DEFAULT);
                mMobileDataPref.setChecked(pchFlag == 0 ? true : false);
                /// @}

                //Get the networkMode from Settings.System and displays it
                int settingsNetworkMode = android.provider.Settings.Secure.getInt(mPhone.getContext().
                    getContentResolver(),android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                    preferredNetworkMode);
                if (settingsNetworkMode > 2) {
                    settingsNetworkMode = preferredNetworkMode;
                    android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        settingsNetworkMode);
                }
                mButtonPreferredNetworkMode.setValue(Integer.toString(settingsNetworkMode));

                if(!PhoneUtils.isSupportFeature("3G_SWITCH")){
                    prefSet.removePreference(mPreference3GSwitch);
                    if(getBaseBand(0) > MODEM_MASK_EDGE){
                        if(CallSettings.isMultipleSim()){
                            prefSet.removePreference(mButtonPreferredNetworkMode);
                        }else{
                            prefSet.removePreference(mButtonPreferredNetworkModeEx);
                        }
                    }
                }else{
                    prefSet.removePreference(mButtonPreferredNetworkModeEx);
                    prefSet.removePreference(mButtonPreferredNetworkMode);
                }

                mExtension.customizeFeatureForOperator(prefSet, mButtonPreferredNetworkModeEx, 
                    mButtonPreferredNetworkMode, mPreference3GSwitch, mButtonPreferredGSMOnly);

                if (mButtonPreferredNetworkMode != null)
                {
                    mButtonPreferredNetworkMode.setOnPreferenceChangeListener(this);
                    if((getBaseBand(SIM_CARD_1) & MODEM_MASK_TDSCDMA) != 0){
                        mButtonPreferredNetworkMode.setEntries(
                            getResources().getStringArray(R.array.gsm_umts_network_preferences_choices_cmcc));    
                        mButtonPreferredNetworkMode.setEntryValues(
                            getResources().getStringArray(R.array.gsm_umts_network_preferences_values_cmcc));    
                    }
                }
                mCarrierSelPref = (PreferenceScreen) prefSet.findPreference(BUTTON_CARRIER_SEL);
                /// @}
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }

        final boolean missingDataServiceUrl = TextUtils.isEmpty(
                android.provider.Settings.Secure.getString(getContentResolver(),
                        android.provider.Settings.Secure.SETUP_PREPAID_DATA_SERVICE_URL));
        if (!isLteOnCdma || missingDataServiceUrl) {
            prefSet.removePreference(mLteDataServicePref);
        } else {
            android.util.Log.d(LOG_TAG, "keep ltePref");
        }

        ThrottleManager tm = (ThrottleManager) getSystemService(Context.THROTTLE_SERVICE);
        mDataUsageListener = new DataUsageListener(this, mButtonDataUsage, prefSet);
        /// M: register receivers
        registerReceiver(mReceiver, mIntentFilter);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mExtension.removeNMMode(prefSet, mButtonPreferredNetworkMode, 
                mButtonPreferredGSMOnly, mButtonPreferredNetworkModeEx);
        setNetworkOperator();
        /// M: add unlock sim card receiver
        mCellConnMgr = new CellConnMgr();
        mCellConnMgr.register(this);
    }

    private void setNetworkOperator(){
        boolean isShowPlmn = false;
        if (CallSettings.isMultipleSim()){
            List<SIMInfo> sims = SIMInfo.getInsertedSIMList(this);
            for(SIMInfo sim : sims){
                isShowPlmn |= mGeminiPhone.isCspPlmnEnabled(sim.mSlot);
            }
        }else{
            isShowPlmn = mPhone.isCspPlmnEnabled();
        }
        mExtension.removeNMOp(getPreferenceScreen(), mCarrierSelPref, isShowPlmn);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // upon resumption from the sub-activity, make sure we re-enable the
        // preferences.
        mAirplaneModeEnabled = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, -1)==1;
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mButtonDataEnabled.setChecked(cm.getMobileDataEnabled());

        // Set UI state in onResume because a user could go home, launch some
        // app to change this setting's backend, and re-launch this settings app
        // and the UI state would be inconsistent with actual state
        mButtonDataRoam.setChecked(mPhone.getDataRoamingEnabled());

        if (getPreferenceScreen().findPreference(BUTTON_PREFERED_NETWORK_MODE) != null)  {
            mPhone.getPreferredNetworkType(mHandler.obtainMessage(
                    MyHandler.MESSAGE_GET_PREFERRED_NETWORK_TYPE));
        }
        mDataUsageListener.resume();
       
        //if the phone not idle state or airplane mode, then disable the preferenceScreen
        /// M: support for gemini phone
        if(CallSettings.isMultipleSim()) {
            mDualSimMode = android.provider.Settings.System.getInt(getContentResolver(), 
                    android.provider.Settings.System.DUAL_SIM_MODE_SETTING, -1);
            Log.d(LOG_TAG, "Settings.onResume(), mDualSimMode="+mDualSimMode);
        }
        
        /// M: set RAT mode when on resume 
        if (mButtonPreferredNetworkMode != null)
        {
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, preferredNetworkMode);
            Log.d(LOG_TAG, "mButtonPreferredNetworkMode != null and the settingsNetworkMode = " + settingsNetworkMode);
            UpdatePreferredNetworkModeSummary(settingsNetworkMode);
        }
        
        ///M: add for data connection gemini and op01 only
        setDataConnPref();
        //Please make sure this is the last line!!
        setScreenEnabled();
    }
    private void setDataConnPref(){
        Log.d(LOG_TAG,"setDataConnPref");
        if (isDataConnAvailable() &&
            mDataConnPref != null){
            mDataConnPref.SetCellConnMgr(mCellConnMgr);
            Log.d(LOG_TAG,"setDataConnPref---2");
            long dataconnectionID = android.provider.Settings.System.getLong(getContentResolver(), 
                                    android.provider.Settings.System.GPRS_CONNECTION_SIM_SETTING,
                                    android.provider.Settings.System.DEFAULT_SIM_NOT_SET);
            List<SimItem> mSimItemListGprs = new ArrayList<SimItem>();
            List<SIMInfo> simList = SIMInfo.getInsertedSIMList(this);
            if (simList.size() > 1){
                //check whether need to order the sim card list
                SIMInfo siminfo1 = simList.get(Phone.GEMINI_SIM_1);
                SIMInfo siminfo2 = simList.get(Phone.GEMINI_SIM_2);
                if(siminfo1.mSlot > siminfo2.mSlot){
                 simList.clear();
                 simList.add(siminfo2);
                 simList.add(siminfo1);
                }
            }
            mSimItemListGprs.clear();
            SimItem simitem;
            int state;
            int k = 0;
            TelephonyManagerEx mTelephonyManagerEx = TelephonyManagerEx.getDefault();
            for (SIMInfo siminfo: simList) {
                if (siminfo != null) {
                    simitem = new SimItem(siminfo);
                    state = mTelephonyManagerEx.getSimIndicatorStateGemini(siminfo.mSlot);
                    simitem.mState = state;
                    Log.d(LOG_TAG, "state="+simitem.mState);
                    if (siminfo.mSimId == dataconnectionID) {
                        mDataConnPref.setInitValue(k);
                        mDataConnPref.setSummary(siminfo.mDisplayName);
                    }
                    mSimItemListGprs.add(simitem);
                }
                k++;
            }
            if(dataconnectionID == android.provider.Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                mDataConnPref.setInitValue(simList.size());
                mDataConnPref.setSummary(R.string.service_3g_off);
            }
            simitem = new SimItem (null);
            mSimItemListGprs.add(simitem);  
            Log.d(LOG_TAG,"mSimItemListGprs="+mSimItemListGprs.size());
            mDataConnPref.setInitData(mSimItemListGprs);   
        }
    }

    /// M: show dialogs
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        if (id == PROGRESS_DIALOG) {
            dialog = new ProgressDialog(this);
            ((ProgressDialog)dialog).setMessage(getResources().getString(R.string.updating_settings));
            dialog.setCancelable(false);
        } else if(id == DATA_ENABLE_ALERT_DIALOG 
                || id == DATA_DISABLE_ALERT_DIALOG){
            int message = (id == DATA_ENABLE_ALERT_DIALOG?
                    R.string.networksettings_tips_data_enabled
                    :R.string.networksettings_tips_data_disabled);
            dialog = new AlertDialog.Builder(this)
                    .setMessage(getText(message))
                    .setTitle(com.android.internal.R.string.dialog_alert_title)
                    .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                    .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            isChangeData = true;
                            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                            showDialog(PROGRESS_DIALOG);
                            boolean tempCheckedStatus = mButtonDataEnabled.isChecked();
                            cm.setMobileDataEnabled(!tempCheckedStatus);
                            mButtonDataEnabled.setChecked(!tempCheckedStatus);
                            mH.sendMessageDelayed(mH.obtainMessage(DATA_STATE_CHANGE_TIMEOUT), 30000);
                        }
                    })
                    .setNegativeButton(com.android.internal.R.string.no, null)
                    .create();
        } else if(id == ROAMING_DIALOG){
            dialog = new AlertDialog.Builder(this).setMessage(getText(R.string.roaming_warning))
                    .setTitle(com.android.internal.R.string.dialog_alert_title)
                    .setIconAttribute(com.android.internal.R.attr.alertDialogIcon)
                    .setPositiveButton(com.android.internal.R.string.yes, this)
                    .setNegativeButton(com.android.internal.R.string.no, this)
                    .create();
            dialog.setOnDismissListener(this);
        } else if (id == MOBILE_DATA_PREF_DIALOG){
            dialog = new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.pch_data_prefer_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            try{
                                Settings.System.putInt(getContentResolver(), Settings.System.GPRS_TRANSFER_SETTING,0);
                                if(FeatureOption.MTK_GEMINI_SUPPORT){
                                    if(iTelephony != null){
                                        iTelephony.setGprsTransferTypeGemini(PCH_DATA_PREFER, Phone.GEMINI_SIM_1);
                                        iTelephony.setGprsTransferTypeGemini(PCH_DATA_PREFER, Phone.GEMINI_SIM_2);
                                    }
                                } else {
                                   if(iTelephony != null){
                                        iTelephony.setGprsTransferType(PCH_DATA_PREFER);
                                   }
                                }
                            }catch(RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){
                        public void onClick(DialogInterface dialog, int which) {
                            mMobileDataPref.setChecked(false);
                        }
                    })
                    .create();
        }
        return dialog;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataUsageListener.pause();
    }
    /// M: add for support receiver & check sim lock
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mCellConnMgr.unregister();
        if (mPreCheckForRunning != null) {
            mPreCheckForRunning.deRegister();
        }
    }
    
    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes specifically on CLIR.
     *
     * @param preference is the preference to be changed, should be mButtonCLIR.
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mButtonPreferredNetworkMode) {
            //NOTE onPreferenceChange seems to be called even if there is no change
            //Check if the button value is changed from the System.Setting
            mButtonPreferredNetworkMode.setValue((String) objValue);
            int buttonNetworkMode;
            buttonNetworkMode = Integer.valueOf((String) objValue).intValue();
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, preferredNetworkMode);
            if (buttonNetworkMode != settingsNetworkMode) {
                /// M: when wait for network switch done show dialog    
                showDialog(PROGRESS_DIALOG);
                int modemNetworkMode;
                switch(buttonNetworkMode) {
                    case Phone.NT_MODE_GLOBAL:
                        modemNetworkMode = Phone.NT_MODE_GLOBAL;
                        break;
                    case Phone.NT_MODE_EVDO_NO_CDMA:
                        modemNetworkMode = Phone.NT_MODE_EVDO_NO_CDMA;
                        break;
                    case Phone.NT_MODE_CDMA_NO_EVDO:
                        modemNetworkMode = Phone.NT_MODE_CDMA_NO_EVDO;
                        break;
                    case Phone.NT_MODE_CDMA:
                        modemNetworkMode = Phone.NT_MODE_CDMA;
                        break;
                    case Phone.NT_MODE_GSM_UMTS:
                        modemNetworkMode = Phone.NT_MODE_GSM_UMTS;
                        break;
                    case Phone.NT_MODE_WCDMA_ONLY:
                        modemNetworkMode = Phone.NT_MODE_WCDMA_ONLY;
                        break;
                    case Phone.NT_MODE_GSM_ONLY:
                        modemNetworkMode = Phone.NT_MODE_GSM_ONLY;
                        break;
                    case Phone.NT_MODE_WCDMA_PREF:
                        modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                        break;
                    default:
                        modemNetworkMode = Phone.PREFERRED_NT_MODE;
                }

                // If button has no valid selection && setting is LTE ONLY
                // mode, let the setting stay in LTE ONLY mode. UI is not
                // supported but LTE ONLY mode could be used in testing.
                if ((modemNetworkMode == Phone.PREFERRED_NT_MODE) &&
                    (settingsNetworkMode == Phone.NT_MODE_LTE_ONLY)) {
                    return true;
                }

                UpdatePreferredNetworkModeSummary(buttonNetworkMode);

                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        buttonNetworkMode );
                /// M: support gemini Set the modem network mode
                if (CallSettings.isMultipleSim()) {
                    mGeminiPhone.setPreferredNetworkTypeGemini(modemNetworkMode, mHandler
                            .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), mSimId);
                } else {
                    mPhone.setPreferredNetworkType(modemNetworkMode, mHandler
                            .obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
                }
            }
        }else if (preference == mDataConnPref){
            Long simid = (Long)objValue;
            Log.d(LOG_TAG,"under click simid="+simid);
            switchGprsDefautlSIM(simid);
        }
        return true;
    }
    /**
     * switch data connection default SIM
     * @param value: sim id of the new default SIM
     */
    private void switchGprsDefautlSIM(long simid) {
        if(simid <0) {
            Log.d(LOG_TAG,"value="+simid+" is an exceptions");
            return;
        }
        long GprsValue = android.provider.Settings.System.getLong(getContentResolver(),
                         android.provider.Settings.System.GPRS_CONNECTION_SIM_SETTING,
                         android.provider.Settings.System.DEFAULT_SIM_NOT_SET);
        Log.d(LOG_TAG,"Current GprsValue="+GprsValue+" and target value="+simid);
        if(simid == GprsValue) {
            return;
        }        
        Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
        intent.putExtra("simid", simid);
        sendBroadcast(intent);
        showDialog(PROGRESS_DIALOG);
        mH.sendMessageDelayed(mH.obtainMessage(DATA_STATE_CHANGE_TIMEOUT), 30000);
        isChangeData = true;
    }
    private class MyHandler extends Handler {

        private static final int MESSAGE_GET_PREFERRED_NETWORK_TYPE = 0;
        private static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PREFERRED_NETWORK_TYPE:
                    handleGetPreferredNetworkTypeResponse(msg);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    handleSetPreferredNetworkTypeResponse(msg);
                    break;
            }
        }

        private void handleGetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int modemNetworkMode = ((int[])ar.result)[0];

                if (DBG) {
                    log ("handleGetPreferredNetworkTypeResponse: modemNetworkMode = " +
                            modemNetworkMode);
                }

                int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                        mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode);

                if (DBG) {
                    log("handleGetPreferredNetworkTypeReponse: settingsNetworkMode = " +
                            settingsNetworkMode);
                }

                //check that modemNetworkMode is from an accepted value
                if (modemNetworkMode == Phone.NT_MODE_WCDMA_PREF ||
                        modemNetworkMode == Phone.NT_MODE_GSM_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_WCDMA_ONLY ||
                        modemNetworkMode == Phone.NT_MODE_GSM_UMTS ||
                        modemNetworkMode == Phone.NT_MODE_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_CDMA_NO_EVDO ||
                        modemNetworkMode == Phone.NT_MODE_EVDO_NO_CDMA ||
                        modemNetworkMode == Phone.NT_MODE_GLOBAL ) {
                    if (DBG) {
                        log("handleGetPreferredNetworkTypeResponse: if 1: modemNetworkMode = " +
                                modemNetworkMode);
                    }

                    //check changes in modemNetworkMode and updates settingsNetworkMode
                    if (modemNetworkMode != settingsNetworkMode) {
                        if (DBG) {
                            log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                    "modemNetworkMode != settingsNetworkMode");
                        }

                        settingsNetworkMode = modemNetworkMode;

                        if (DBG) { log("handleGetPreferredNetworkTypeResponse: if 2: " +
                                "settingsNetworkMode = " + settingsNetworkMode);
                        }

                        //changes the Settings.System accordingly to modemNetworkMode
                        android.provider.Settings.Secure.putInt(
                                mPhone.getContext().getContentResolver(),
                                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                                settingsNetworkMode );
                    }
                    /// M: add for wcdma prefer feature
                    if(modemNetworkMode == Phone.NT_MODE_GSM_UMTS){
                        modemNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                        settingsNetworkMode = Phone.NT_MODE_WCDMA_PREF;
                    }

                    UpdatePreferredNetworkModeSummary(modemNetworkMode);
                    // changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    mButtonPreferredNetworkMode.setValue(Integer.toString(modemNetworkMode));
                } else if (modemNetworkMode == Phone.NT_MODE_LTE_ONLY) {
                    // LTE Only mode not yet supported on UI, but could be used for testing
                    if (DBG) log("handleGetPreferredNetworkTypeResponse: lte only: no action");
                } else {
                    if (DBG) log("handleGetPreferredNetworkTypeResponse: else: reset to default");
                    resetNetworkModeToDefault();
                }
            }
        }

        private void handleSetPreferredNetworkTypeResponse(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
             /// M: when set network mode show wait dialog
            removeDialog(PROGRESS_DIALOG);
            if (ar.exception == null) {
                int networkMode = Integer.valueOf(
                        mButtonPreferredNetworkMode.getValue()).intValue();
                android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        networkMode );
            } else {
                 /// M: support gemini phone
                if (CallSettings.isMultipleSim()) {
                    mGeminiPhone.getPreferredNetworkTypeGemini(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE), mSimId);
                } else {
                    mPhone.getPreferredNetworkType(obtainMessage(MESSAGE_GET_PREFERRED_NETWORK_TYPE));
                }
            }
        }

        private void resetNetworkModeToDefault() {
            //set the mButtonPreferredNetworkMode
            mButtonPreferredNetworkMode.setValue(Integer.toString(preferredNetworkMode));
            //set the Settings.System
            android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                        preferredNetworkMode );
            /// M: support gemini Set the Modem
            if (CallSettings.isMultipleSim()) {
                mGeminiPhone.setPreferredNetworkTypeGemini(preferredNetworkMode,
                        obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE), mSimId);
            } else {
                mPhone.setPreferredNetworkType(preferredNetworkMode,
                        obtainMessage(MyHandler.MESSAGE_SET_PREFERRED_NETWORK_TYPE));
            }
        }
    }
    ///M: add for AT&T
    private boolean isNeedtoShowRoamingMsg() {
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        boolean isInRoaming = telMgr.isNetworkRoaming();
        boolean isRoamingEnabled = mPhone.getDataRoamingEnabled();
        Log.d(LOG_TAG,"***isInRoaming="+isInRoaming+" isRoamingEnabled="+isRoamingEnabled);
        if (isInRoaming && !isRoamingEnabled){
            return true;
        }else{
            return false;
        }
    }
    ///
    private void UpdatePreferredNetworkModeSummary(int NetworkMode) {
        switch(NetworkMode) {
        case Phone.NT_MODE_WCDMA_PREF:
            /// M: support td phone @{
            if((getBaseBand(SIM_CARD_1) & MODEM_MASK_TDSCDMA) != 0){
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_scdma_perf_summary);
            }else{
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_perf_summary);
            }
            /// @}
            break;
        case Phone.NT_MODE_GSM_ONLY:
            mButtonPreferredNetworkMode.setSummary(
                    R.string.preferred_network_mode_gsm_only_summary);
            break;
        case Phone.NT_MODE_WCDMA_ONLY:
            /// M: support td phone @{
            if((getBaseBand(SIM_CARD_1) & MODEM_MASK_TDSCDMA) != 0){
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_scdma_only_summary);
            }else{
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_wcdma_only_summary);
            }
            /// @}
            break;
        case Phone.NT_MODE_GSM_UMTS:
            mButtonPreferredNetworkMode.setSummary(
		    R.string.preferred_network_mode_gsm_wcdma_summary);
            break;
        case Phone.NT_MODE_CDMA:
            switch (mPhone.getLteOnCdmaMode()) {
            case Phone.LTE_ON_CDMA_TRUE:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_summary);
                break;
            case Phone.LTE_ON_CDMA_FALSE:
            default:
                mButtonPreferredNetworkMode.setSummary(
                        R.string.preferred_network_mode_cdma_evdo_summary);
                break;
            }
            break;
        case Phone.NT_MODE_CDMA_NO_EVDO:
            mButtonPreferredNetworkMode.setSummary(
                    R.string.preferred_network_mode_cdma_only_summary);
            break;
        case Phone.NT_MODE_EVDO_NO_CDMA:
            mButtonPreferredNetworkMode.setSummary(
                    R.string.preferred_network_mode_evdo_only_summary);
            break;
        case Phone.NT_MODE_GLOBAL:
        default:
            mButtonPreferredNetworkMode.setSummary(
                    R.string.preferred_network_mode_lte_cdma_summary);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
        case REQUEST_CODE_EXIT_ECM:
            Boolean isChoiceYes =
                data.getBooleanExtra(EmergencyCallbackModeExitDialog.EXTRA_EXIT_ECM_RESULT, false);
            if (isChoiceYes) {
                // If the phone exits from ECM mode, show the CDMA Options
                mCdmaOptions.showDialog(mClickedPreference);
            } else {
                // do nothing
            }
            break;

        default:
            break;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {  // See ActionBar#setDisplayHomeAsUpEnabled()
            // Commenting out "logical up" capability. This is a workaround for issue 5278083.
            //
            // Settings app may not launch this activity via UP_ACTIVITY_CLASS but the other
            // Activity that looks exactly same as UP_ACTIVITY_CLASS ("SubSettings" Activity).
            // At that moment, this Activity launches UP_ACTIVITY_CLASS on top of the Activity.
            // which confuses users.
            // TODO: introduce better mechanism for "up" capability here.
            /*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName(UP_ACTIVITY_PACKAGE, UP_ACTIVITY_CLASS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);*/
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /// M: get slot base band 
    private int getBaseBand(int slot)
    {
        int value = 0;
        String propertyKey = "gsm.baseband.capability";
        String capability = null;
        if (slot == 1) {
            propertyKey += "2";
        }
        capability = SystemProperties.get(propertyKey);
        if (capability == null || "".equals(capability)) {
            return value;
        }
        
        try {
            value = Integer.valueOf(capability, 16);
        }catch (NumberFormatException ne) {
            log("parse value of basband error");
        }
        return value;        
    }
    /// M: when receive data change broadcast get the extra    
    private static Phone.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(Phone.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(Phone.DataState.class, str);
        } else {
            return Phone.DataState.DISCONNECTED;
        }
    }
    /// M: disable screen in some case    
    private boolean isDataConnAvailable() {
        if (CallSettings.isMultipleSim()) {
            return true;
        }
        return false;
    }
    private void setScreenEnabled(){
        boolean isShouldEnabled = false;
        boolean isIdle = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);

        isShouldEnabled = isIdle && (!mAirplaneModeEnabled) && (mDualSimMode!=0);
        getPreferenceScreen().setEnabled(isShouldEnabled);

        boolean isGeminiMode = CallSettings.isMultipleSim();
        boolean isSupport3GSwitch = PhoneUtils.isSupportFeature("3G_SWITCH");
        List<SIMInfo> sims = SIMInfo.getInsertedSIMList(this);
        boolean isHasSimCard = ((sims != null) && (sims.size() > 0));
        if (mPreference3GSwitch != null) {
            mPreference3GSwitch.setEnabled(isHasSimCard && isShouldEnabled);
        }

        if(mButtonPreferredNetworkMode != null){
            boolean isNWModeEnabled = isShouldEnabled && CallSettings.isRadioOn(SIM_CARD_1);
            mButtonPreferredNetworkMode.setEnabled(isNWModeEnabled);
            if(!isNWModeEnabled){
                Dialog dialog = mButtonPreferredNetworkMode.getDialog();
                if(dialog != null && dialog.isShowing()){
                    dialog.dismiss();
                }
            }
        }

        if(mButtonPreferredNetworkModeEx != null){
            boolean isNWModeEnabled = isShouldEnabled && CallSettings.isRadioOn(SIM_CARD_1);
            mButtonPreferredNetworkModeEx.setEnabled(isNWModeEnabled);
        }
    }
}
