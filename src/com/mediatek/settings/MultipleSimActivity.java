package com.mediatek.settings;

import java.io.IOException;
import java.util.ArrayList;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;

import java.util.*;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.AttributeSet;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;
import android.provider.Settings;
import android.provider.Telephony;
import android.widget.RelativeLayout;
import com.android.phone.PhoneInterfaceManager;
import com.android.phone.PhoneApp;
import com.mediatek.phone.vt.VTCallUtils;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.phone.ext.SettingsExtension;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.xlog.Xlog;
import com.android.phone.PhoneUtils;
import com.android.phone.R;

public class MultipleSimActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
    //Give the sub items type(ListPreference, CheckBoxPreference, Preference)
    private String mItemType = "PreferenceScreen";
    static public String  intentKey = "ITEM_TYPE";
    static public String SUB_TITLE_NAME = "sub_title_name";
    
    //Used for PreferenceScreen to start a activity
    static public String  targetClassKey = "TARGET_CLASS";
    
    //Used for ListPreference to initialize the list
    static public String initArray = "INIT_ARRAY";
    static public String initArrayValue = "INIT_ARRAY_VALUE";
    
    //Most time, we get the sim number by Framework's api, but we firstly check how many sim support a special feature
    //For example, although two sim inserted, but there's only one support the VT
    static public String initSimNumber = "INIT_SIM_NUMBER";
    static public String initFeatureName = "INIT_FEATURE_NAME";
    static public String initTitleName = "INIT_TITLE_NAME";
    static public String initSimId = "INIT_SIM_ID";
    static public String initBaseKey = "INIT_BASE_KEY";
    
    //VT private:
    private static final String SELECT_DEFAULT_PICTURE    = "0";
    private static final String SELECT_MY_PICTURE         = "2";
    private static final String SELECT_DEFAULT_PICTURE2    = "0";
    private static final String SELECT_MY_PICTURE2         = "1";

    private static final int PROGRESS_DIALOG = 100;
    private static final int ALERT_DIALOG = 200;
    private static final int ALERT_DIALOG_DEFAULT = 300;

    
    private int mVTWhichToSave = 0;
    
    private int mSimNumbers = 0;
    private String mTargetClass = null;
    private String mFeatureName;
    private String mTitleName;
    private String mListTitle;
    //for the key of checkbox and listpreference: mBaseKey + cardSlot || simId
    private String mBaseKey;
    private Phone mPhone = null;
    private List<SIMInfo> simList;
    private long[] simIds = null;
    private HashMap<Object, Integer> pref2CardSlot = new HashMap<Object, Integer>();
    private static String TAG = "MultipleSimActivity";
    private static final boolean DBG = true;
    
    public static final String VT_FEATURE_NAME = "VT";
    public static final String NETWORK_MODE_NAME = "NETWORK_MODE";
    public static final String LIST_TITLE = "LIST_TITLE_NAME";
    
    private ImageView mImage;
    private Bitmap mBitmap;
    private PreCheckForRunning mPreCheckForRunning;
    private TelephonyManagerEx mTelephonyManagerEx;
    private TelephonyManager mTelephonyManager;
    
    private IntentFilter mIntentFilter;
    private final MultipleSimReceiver mReceiver = new MultipleSimReceiver();
    
    PhoneInterfaceManager phoneMgr = null;
    //By default, slot1 will support 3G
    private static int m3GSupportSlot = 0;

    private SettingsExtension mExtension;

    private class MultipleSimReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (PhoneApp.NETWORK_MODE_CHANGE_RESPONSE.equals(action)) {
                removeDialog(PROGRESS_DIALOG);
                if (!intent.getBooleanExtra(PhoneApp.NETWORK_MODE_CHANGE_RESPONSE, true)) {
                    log("BroadcastReceiver: network mode change failed! restore the old value.");
                    int oldMode = intent.getIntExtra(PhoneApp.OLD_NETWORK_MODE, 0);
                    android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                            oldMode);
                    log("BroadcastReceiver  = " + oldMode);
                    if (NETWORK_MODE_NAME.equals(mFeatureName)) {
                        log("setValue  to oldMode ");
                        ((ListPreference)getPreferenceScreen().getPreference(0)).setValue(Integer.toString(oldMode));
                    }
                }else {
                    log("BroadcastReceiver: network mode change success! set to the new value.");
                    android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                            intent.getIntExtra("NEW_NETWORK_MODE", 0));
                    log("BroadcastReceiver  = " + intent.getIntExtra("NEW_NETWORK_MODE", 0));
                }
            } else if(Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)
                        || Intent.ACTION_DUAL_SIM_MODE_CHANGED.equals(action)) {
                updatePreferenceEnableState();
            } else if(TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY.equals(action)){
                if ("NETWORK_SEARCH".equals(mFeatureName)) {
                    mExtension.removeNMOpForMultiSim(mPhone, simList, mTargetClass);
                    sortSimList();
                    createSubItems();
                }
            } else if(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED.equals(action)){
                updatePreferenceList();
            }
        }
    }

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            log("onCallStateChanged ans state is "+state);
            switch(state){
            case TelephonyManager.CALL_STATE_IDLE:
                updatePreferenceEnableState();
                break;
            default:
                break;
            }
        }
    };

   
    private void log(String msg) {
        if(DBG) Xlog.d(TAG, msg);
    }
    
    @Override
    protected void onCreate(Bundle icicle){
        super.onCreate(icicle);
        mExtension = ExtensionManager.getInstance().getSettingsExtension();
        mPhone = PhoneApp.getPhone();
        simIds = getIntent().getLongArrayExtra(initSimId);
        this.addPreferencesFromResource(R.xml.multiple_sim);
        
        String itemType = getIntent().getStringExtra(intentKey);
        if (itemType != null){
            mItemType = itemType;
        }
        
        mPreCheckForRunning = new PreCheckForRunning(this);
        mTelephonyManagerEx = new TelephonyManagerEx(this);
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        
        mTargetClass = getIntent().getStringExtra(targetClassKey);
        mFeatureName = getIntent().getStringExtra(initFeatureName);
        mTitleName = getIntent().getStringExtra(initTitleName);
        mBaseKey = getIntent().getStringExtra(initBaseKey);
        mListTitle = getIntent().getStringExtra(LIST_TITLE);
            
        //If upper level tells us the exact sim numbers, then we use it firstly, else we get it
        //by framework's api
        if (simIds != null){
            mSimNumbers = simIds.length;
            simList = new ArrayList<SIMInfo>();
            for (int i = 0; i < mSimNumbers; ++i){
                simList.add(SIMInfo.getSIMInfoById(this,simIds[i]));
            }
        } else {
            simList = SIMInfo.getInsertedSIMList(this);
            mSimNumbers = simList.size();
        }
        mExtension.removeNMOpForMultiSim(mPhone, simList, mTargetClass);
        sortSimList();
        mIntentFilter =
            new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_EF_CSP_CONTENT_NOTIFY);
        if(FeatureOption.MTK_GEMINI_SUPPORT) {
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        
        phoneMgr = PhoneApp.getInstance().phoneMgr;
        if(PhoneUtils.isSupportFeature("3G_SWITCH")){
            if(phoneMgr!=null){
                m3GSupportSlot = phoneMgr.get3GCapabilitySIM();
                log("3G support slot ID: "+m3GSupportSlot);
            }else{
                log("Fail to get phone app instance");
            }
        }

        skipUsIfNeeded();
        createSubItems();
        registerReceiver(mReceiver, mIntentFilter);
    }
    
    private void sortSimList() {
        int size = simList.size();
        log("sortSimList()+simList.size()="+size);

        if(size==2) {//2 stands for two sim card
            SIMInfo temp1=simList.get(0);
            SIMInfo temp2=simList.get(1);
            if (temp1.mSlot>temp2.mSlot) {
                log("swap the position of simList");
                simList.clear();
                simList.add(temp2);
                simList.add(temp1);
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume(); 
        updatePreferenceList();
        updatePreferenceEnableState();
    }
    
    //We will consider the feature and the number inserted sim then 
    //skip us and enter the setting directly
    private void skipUsIfNeeded(){
        //For VT and Network mode we will pay special handler, so we don't skip
        if (VT_FEATURE_NAME.equals(mFeatureName) || NETWORK_MODE_NAME.equals(mFeatureName)){
            return;
        }
        
        if (mSimNumbers == 1 && mTargetClass != null){
            Intent intent = new Intent();
            int position = mTargetClass.lastIndexOf('.');
            String pkName = mTargetClass.substring(0, position);
            pkName = pkName.replace("com.mediatek.settings", "com.android.phone");
            intent.setAction(Intent.ACTION_MAIN);
            int slotId = simList.get(0).mSlot;
            intent.putExtra(Phone.GEMINI_SIM_ID_KEY, slotId);
            if(FeatureOption.EVDO_DT_SUPPORT){
                customizeForEVDO(intent, slotId, pkName, mTargetClass);
            }else{
                intent.setClassName(pkName, mTargetClass);
                checkToStart(slotId, intent);
            }
            finish();
        }
    }
    
    private void createSubItems(){
        PreferenceScreen prefSet = getPreferenceScreen();
        ArrayList<String> keys = new ArrayList<String>();
        for (int i = 0; i < prefSet.getPreferenceCount(); ++i){
            String c = prefSet.getPreference(i).getKey();
            if (!c.startsWith(mItemType)){
                keys.add(prefSet.getPreference(i).getKey());
            }
        }
        
        for (String s : keys){
            prefSet.removePreference(prefSet.findPreference(s));
        }
        
        int prefCount = prefSet.getPreferenceCount();
        
        for (int i = prefCount - 1; i > mSimNumbers - 1; --i){
            prefSet.removePreference(prefSet.getPreference(i));
        }
        
        if (mItemType.equals("PreferenceScreen")){
            initPreferenceScreen();
        } else if (mItemType.equals("CheckBoxPreference")){
            initCheckBoxPreference();
        } else if (mItemType.equals("ListPreference")){
            initListPreference();
        }
        
        if (mTitleName != null){
            this.setTitle(mTitleName);
        }
    }
    
    private void initPreferenceScreen(){
        PreferenceScreen prefSet = getPreferenceScreen();
        for (int i = 0; i < mSimNumbers; ++i){
            SimPreference p = (SimPreference)prefSet.getPreference(i);
            p.setTitle(simList.get(i).mDisplayName);
            p.setSimColor(simList.get(i).mColor);
            p.setSimSlot(i);
            p.setSimName(simList.get(i).mDisplayName);
            p.setSimNumber(simList.get(i).mNumber);
            p.setSimIconNumber(getProperOperatorNumber(simList.get(i)));
            pref2CardSlot.put(prefSet.getPreference(i), Integer.valueOf(simList.get(i).mSlot));
        }
    }
    
    private void initCheckBoxPreference(){
        PreferenceScreen prefSet = getPreferenceScreen();
        for (int i = 0; i < mSimNumbers; ++i){
            String key = null;
            CheckSimPreference p =(CheckSimPreference) prefSet.getPreference(i);
            p.setTitle(simList.get(i).mDisplayName);
            p.setSimColor(simList.get(i).mColor);
            p.setSimSlot(i);
            p.setSimName(simList.get(i).mDisplayName);
            p.setSimNumber(simList.get(i).mNumber);
            p.setSimIconNumber(getProperOperatorNumber(simList.get(i)));
            pref2CardSlot.put(prefSet.getPreference(i), Integer.valueOf(simList.get(i).mSlot));
            
            //Now we use @ as the flag to indicate if need to modify the key
            //Use this must make sure there's only one cotrol use, else will cause issue
            if (mBaseKey != null && mBaseKey.endsWith("@")){
                mBaseKey = mBaseKey.substring(0, mBaseKey.length()-1);
                key = mBaseKey;
                p.setKey(mBaseKey);
            } else if (mBaseKey != null){
                //If this used the key + simId more proper?
                key = mBaseKey + "_" + simList.get(i).mSlot;
                p.setKey(key);
            }
            
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            p.setChecked(sp.getBoolean(key, true));
            if("button_vt_auto_dropback_key".equals(key)){
                p.setChecked(sp.getBoolean(key, false));
            }
            p.setOnPreferenceChangeListener(this);
        }
    }
    
    private void initListPreference(){
        PreferenceScreen prefSet = getPreferenceScreen();
        CharSequence[] array = (String[])getIntent().getCharSequenceArrayExtra(initArray);
        CharSequence[] arrayValue = getIntent().getCharSequenceArrayExtra(initArrayValue);
        
        if (NETWORK_MODE_NAME.equals(mFeatureName)) {
            mIntentFilter.addAction(PhoneApp.NETWORK_MODE_CHANGE_RESPONSE);
        }
        
        for (int i = 0; i < mSimNumbers; ++i){
            String key = null;
            ListSimPreference listPref = (ListSimPreference)prefSet.getPreference(i);
            listPref.setTitle(simList.get(i).mDisplayName);
            listPref.setSimColor(simList.get(i).mColor);
            listPref.setSimSlot(i);
            listPref.setSimName(simList.get(i).mDisplayName);
            listPref.setSimNumber(simList.get(i).mNumber);
            listPref.setSimIconNumber(getProperOperatorNumber(simList.get(i)));
            //Now we use @ as the flag to indicate if need to modify the key
            //Use this must make sure there's only one cotrol use, else will cause issue
            if (mBaseKey != null && mBaseKey.endsWith("@")){
                mBaseKey = mBaseKey.substring(0, mBaseKey.length()-1);
                key = mBaseKey;
                listPref.setKey(mBaseKey);
            } else if(mBaseKey != null){
                //If this used the key + simId more proper?
                key = mBaseKey + "_" + simList.get(i).mSlot;
                listPref.setKey(key);
            }
            
            if (mListTitle != null) {
                listPref.setDialogTitle(mListTitle);
            }
            
            listPref.setEntries(array);
            listPref.setEntryValues(arrayValue);
            pref2CardSlot.put(prefSet.getPreference(i), Integer.valueOf(simList.get(i).mSlot));
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            listPref.setValue(sp.getString(key, null));
            listPref.setOnPreferenceChangeListener(this);

            if (NETWORK_MODE_NAME.equals(mFeatureName)) {
                int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, 0);
                listPref.setValue(Integer.toString(settingsNetworkMode));
            }
        }
    }
    
    private boolean isNeededToCheckLock() {
        if ("com.mediatek.settings.IpPrefixPreference".equals(mTargetClass)) {
            return false;
        }
        return true;
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference){
        PreferenceScreen prefSet = getPreferenceScreen();

        GeminiPhone dualPhone = null;
        if (mPhone instanceof GeminiPhone){
            dualPhone = (GeminiPhone)mPhone;
        }
         
        for (int i = 0; i < prefSet.getPreferenceCount(); i++){
            if ((preference == prefSet.getPreference(i)) && (mTargetClass != null) && (dualPhone != null)){
                int slotId = pref2CardSlot.get(preference);
                if(dualPhone.isRadioOnGemini(slotId)){
                    Intent intent = new Intent();
                    int position = mTargetClass.lastIndexOf('.');
                    String pkName = mTargetClass.substring(0, position);
                    pkName = pkName.replace("com.mediatek.settings", "com.android.phone");
                    intent.setClassName(pkName, mTargetClass);
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.putExtra(Phone.GEMINI_SIM_ID_KEY, slotId);
                    intent.putExtra(SUB_TITLE_NAME, SIMInfo.getSIMInfoBySlot(this, slotId).mDisplayName);
                    if (mFeatureName != null && mFeatureName.equals("VT")){
                        intent.putExtra("ISVT", true);
                    }
                    
                    if(FeatureOption.EVDO_DT_SUPPORT){
                        customizeForEVDO(intent, slotId, pkName, mTargetClass);
                    } else {
                        intent.setClassName(pkName, mTargetClass);
                        checkToStart(slotId, intent);
                    }
                } 
            }
        }
        return false;
    }
    
    public void checkAllowedRun(Intent intent, Preference preference) {
        int slot = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0);
        GeminiPhone dualPhone = null;
        if (mPhone instanceof GeminiPhone){
            dualPhone = (GeminiPhone)mPhone;
        }
    }

    private String getProperOperatorNumber(SIMInfo info){
        String res = null;
        int charCount = 4;
        if (info == null) return res;
        res = info.mNumber;
        switch (info.mDispalyNumberFormat){
        case SimInfo.DISPALY_NUMBER_NONE:
            res = "";
            break;
        case SimInfo.DISPLAY_NUMBER_FIRST:
            if (res != null && res.length() > 4){
                res = res.substring(0, charCount);
            }
            break;
        case SimInfo.DISPLAY_NUMBER_LAST:
            if (res != null && res.length() > 4){
                res = res.substring(res.length() - charCount, res.length());
            }
            break;
        default:
            res = "";
            break;
        }
        return res;
    }
    
    public int getNetworkMode(int buttonNetworkMode){
        int modemNetworkMode = 0;
        int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, 0);
        if (buttonNetworkMode != settingsNetworkMode) {
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
        }
        android.provider.Settings.Secure.putInt(mPhone.getContext().getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_NETWORK_MODE,
                modemNetworkMode );
        return modemNetworkMode;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO Auto-generated method stub
        if (VT_FEATURE_NAME.equals(mFeatureName)){
            VTCallUtils.checkVTFile();
            if ("button_vt_replace_expand_key".equals(preference.getKey())){
                mVTWhichToSave = 0;
                if(newValue.toString().equals(SELECT_DEFAULT_PICTURE)){
                    showDialogPic(VTAdvancedSetting.getPicPathDefault(), ALERT_DIALOG_DEFAULT);
                }else if(newValue.toString().equals(SELECT_MY_PICTURE)){
                    showDialogPic(VTAdvancedSetting.getPicPathUserselect(), ALERT_DIALOG);
                }
                
            } else if ("button_vt_replace_peer_expand_key".equals(preference.getKey())){
                mVTWhichToSave = 1;
                if(newValue.toString().equals(SELECT_DEFAULT_PICTURE2)){
                    showDialogPic(VTAdvancedSetting.getPicPathDefault2(), ALERT_DIALOG_DEFAULT);
                }else if(newValue.toString().equals(SELECT_MY_PICTURE2)){
                    showDialogPic(VTAdvancedSetting.getPicPathUserselect2(), ALERT_DIALOG);
                }
            }
            
        } else if (NETWORK_MODE_NAME.equals(mFeatureName)){
            int settingsNetworkMode = android.provider.Settings.Secure.getInt(
                    mPhone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_NETWORK_MODE, 0);
            log("Current network mode = " + settingsNetworkMode);
            int networkMode = getNetworkMode(Integer.valueOf((String) newValue).intValue());
            log("new network mode = " + networkMode);
            if (settingsNetworkMode != networkMode) {
                Intent intent = new Intent(PhoneApp.NETWORK_MODE_CHANGE, null);
                intent.putExtra(PhoneApp.OLD_NETWORK_MODE, settingsNetworkMode);
                intent.putExtra(PhoneApp.NETWORK_MODE_CHANGE, networkMode);
                intent.putExtra(Phone.GEMINI_SIM_ID_KEY, pref2CardSlot.get(preference));
                showDialog(PROGRESS_DIALOG);
                sendBroadcast(intent);
            }
        }
        return true;
    }
    
    @Override
    public Dialog onCreateDialog(int id) {
        log("--------------------[onCreateDialog]["+id+"]-----------------");
        Dialog dialog = null;
        if(mBitmap == null || mImage == null){
            return dialog;
        }

        switch(id){
        case PROGRESS_DIALOG:
            dialog = new ProgressDialog(this);
            ((ProgressDialog)dialog).setMessage(getText(R.string.updating_settings));
            ((ProgressDialog)dialog).setCancelable(false);
            ((ProgressDialog)dialog).setIndeterminate(true);
            break;
        case ALERT_DIALOG:
            dialog = new AlertDialog.Builder(this)
                .setPositiveButton(R.string.vt_change_my_pic, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                            intent.setType("image/*");
                            intent.putExtra("crop", "true");
                            intent.putExtra("aspectX", 1);
                            intent.putExtra("aspectY", 1);
                            intent.putExtra("outputX", getResources().getInteger(R.integer.qcif_x));
                            intent.putExtra("outputY", getResources().getInteger(R.integer.qcif_y));
                            intent.putExtra("return-data", true);
                            intent.putExtra("scaleUpIfNeeded", true);
                            startActivityForResult(intent, VTAdvancedSetting.REQUESTCODE_PICTRUE_PICKED_WITH_DATA);
                        } catch (ActivityNotFoundException e) {
                            log("Pictrue not found , Gallery ActivityNotFoundException !");
                        }
                    }})
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }})
                .create();
            ((AlertDialog)dialog).setView(mImage);
            dialog.setTitle(getResources().getString(R.string.vt_pic_replace_local_mypic));                
            dialog.setOnDismissListener(new OnDismissListener(){
                @Override
                public void onDismiss(DialogInterface dialog){
                    try {
                        mImage.setImageBitmap(null);
                        mBitmap.recycle();
                        removeDialog(ALERT_DIALOG);
                    } catch (Exception e){
                        log(" - Bitmap.isRecycled() : " + mBitmap.isRecycled() );
                    }
                }
            });
            break;
        case ALERT_DIALOG_DEFAULT:
            dialog = new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }})
                .create();
            ((AlertDialog)dialog).setView(mImage);
            dialog.setTitle(getResources().getString(R.string.vt_pic_replace_local_default));                
            dialog.setOnDismissListener(new OnDismissListener(){
                @Override
                public void onDismiss(DialogInterface dialog){
                    try {
                        mImage.setImageBitmap(null);
                        mBitmap.recycle();
                        removeDialog( ALERT_DIALOG_DEFAULT);
                    } catch (Exception e){
                        log(" - Bitmap.isRecycled() : " + mBitmap.isRecycled() );
                    }
                }
            });
            break;
        default:
            break;
        }
        dialog.show();
        return dialog;
    }

    private void showDialogPic(String filename, int dialog){
        mImage = new ImageView(this);
        mBitmap = BitmapFactory.decodeFile(filename);
        mImage.setImageBitmap(mBitmap);
        showDialog(dialog);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        log("onActivityResult: requestCode = " + requestCode + ", resultCode = " +resultCode);
        
        if (resultCode != RESULT_OK) return;
        
        switch (requestCode){
        case VTAdvancedSettingEx.REQUESTCODE_PICTRUE_PICKED_WITH_DATA:
            try {
                final Bitmap bitmap = data.getParcelableExtra("data");
                if (bitmap != null) {
                    if(mVTWhichToSave == 0){
                        VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathUserselect(), bitmap);
                    }else{
                        VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathUserselect2(), bitmap);
                    }
                    try {
                        bitmap.recycle();
                    } catch (Exception e){
                        log(" - Bitmap.isRecycled() : " + bitmap.isRecycled() );
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(mVTWhichToSave == 0){
                showDialogPic(VTAdvancedSetting.getPicPathUserselect(), ALERT_DIALOG);  
            }else{
                showDialogPic(VTAdvancedSetting.getPicPathUserselect2(), ALERT_DIALOG);  
            }
            break;
        }
    }
	
    protected void onDestroy() {
        super.onDestroy();
        if (mPreCheckForRunning != null) {
            mPreCheckForRunning.deRegister();
        }
        unregisterReceiver(mReceiver);
    }
    
    private void updatePreferenceEnableState(){
        PreferenceScreen prefSet = getPreferenceScreen();
        
        //For single sim or only one sim inserted, we couldn't go here
        GeminiPhone dualPhone = null;
        if (mPhone instanceof GeminiPhone){
            dualPhone = (GeminiPhone)mPhone;
        }
        boolean isIdle = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);
        for (int i = 0; i < prefSet.getPreferenceCount(); ++i){
            Preference p = prefSet.getPreference(i);
            if (dualPhone != null) {
                if(NETWORK_MODE_NAME.equals(mFeatureName)){
                    p.setEnabled(dualPhone.isRadioOnGemini(pref2CardSlot.get(p)) && isIdle);
                } else {
                    p.setEnabled(dualPhone.isRadioOnGemini(pref2CardSlot.get(p)));
                }
                if(!p.isEnabled()){
                    if(p instanceof ListPreference){
                        Dialog dialog = ((ListPreference)p).getDialog();
                        if(dialog != null && dialog.isShowing()){
                            dialog.dismiss();
                        }
                    }
                }
            }
        }
    }

    private void customizeForEVDO(Intent intent, int slotId, String pkName, String mTargetClass){
        if(slotId == 0){
            intent.setClassName(pkName, mTargetClass);
            checkToStart(slotId, intent);
            return;
        }
        if("com.android.phone.GsmUmtsCallForwardOptions".equals(mTargetClass)){
            intent.setClassName(pkName, "com.mediatek.settings.CdmaCallForwardOptions");
            checkToStart(slotId, intent);
        }else if("com.android.phone.GsmUmtsAdditionalCallOptions".equals(mTargetClass)){
            intent.setClassName(pkName, "com.mediatek.settings.CdmaCallWaitingOptions");
            checkToStart(slotId, intent);
        }else if("com.mediatek.settings.FdnSetting2".equals(mTargetClass)
            || "com.mediatek.settings.CallBarring".equals(mTargetClass)){
            Toast.makeText(this, getResources().getString(R.string.cdma_not_support), Toast.LENGTH_LONG).show();
        }else{
            intent.setClassName(pkName, mTargetClass);
            checkToStart(slotId, intent);
        }    
    }

    private void checkToStart(int slotId, Intent intent){
        if (isNeededToCheckLock()) {
            mPreCheckForRunning.checkToRun(intent, slotId, 302);
        } else {
            this.startActivity(intent);
        }
    }

    private void updatePreferenceList(){
        log("---------[update mutiple list views]---------");
        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.invalidateViews();
    }
}
