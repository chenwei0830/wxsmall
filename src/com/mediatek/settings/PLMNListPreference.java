package com.mediatek.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.gsm.NetworkInfoWithAcT;
import com.mediatek.xlog.Xlog;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;

import com.android.phone.TimeConsumingPreferenceActivity;
import com.android.phone.TimeConsumingPreferenceListener;
import com.android.phone.PhoneApp;
import com.android.phone.R;

public class PLMNListPreference extends TimeConsumingPreferenceActivity {
    
    private List<NetworkInfoWithAcT> mPLMNList;
    private int numbers = 0;
    private PreferenceScreen mPLMNListContainer;
    
    private static final String LOG_TAG = "Settings/PLMNListPreference";
    private static final String BUTTON_PLMN_LIST_KEY = "button_plmn_list_key";
    private static final boolean DBG = true;
    
    private int mSlotId = 0;
    private Phone mPhone = null;
    private TelephonyManager mTelephonyManager;

    private SIMCapability mCapability = new SIMCapability(0, 0, 0, 0);
    private Map<Preference, NetworkInfoWithAcT> mPreferenceMap = new LinkedHashMap<Preference, NetworkInfoWithAcT>();
    private NetworkInfoWithAcT mOldInfo;
    
    private MyHandler mHandler = new MyHandler();
    
    ArrayList<String> listPriority = new ArrayList<String>();
    ArrayList<String> listService = new ArrayList<String>();    
    
    private static final int REQUEST_ADD = 100;
    private static final int REQUEST_EDIT = 200;
    private static final int MENU_ADD = Menu.FIRST;

    
    private boolean mAirplaneModeEnabled = false;
    private int mDualSimMode = -1;
    private IntentFilter mIntentFilter;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Xlog.d(LOG_TAG, "onCallStateChanged ans state is "+state);
            switch(state){
            case TelephonyManager.CALL_STATE_IDLE:
                setScreenEnabled();
                break;
            default:
                break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); 
            if(action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                setScreenEnabled();
            } else if(action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)){
                mDualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1);
                setScreenEnabled();
            }
        }
    };

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.plmn_list);
        mPLMNListContainer = (PreferenceScreen)findPreference(BUTTON_PLMN_LIST_KEY);
        mPhone = PhoneFactory.getDefaultPhone();
        mSlotId = getIntent().getIntExtra(Phone.GEMINI_SIM_ID_KEY, 0);

        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        if(CallSettings.isMultipleSim()){
            mIntentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        registerReceiver(mReceiver, mIntentFilter);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public void onResume() {
        super.onResume();
        getSIMCapability();
        init(this, false, mSlotId);
        mAirplaneModeEnabled = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, -1)==1;
        if(CallSettings.isMultipleSim()) {
            mDualSimMode = android.provider.Settings.System.getInt(getContentResolver(), 
                    android.provider.Settings.System.DUAL_SIM_MODE_SETTING, -1);
            Xlog.d(LOG_TAG, "Settings.onResume(), mDualSimMode="+mDualSimMode);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ADD, 0, R.string.plmn_list_setting_add_plmn)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        boolean isShouldEnabled = false;
        boolean isIdle = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);
        isShouldEnabled = isIdle && (!mAirplaneModeEnabled) && (mDualSimMode != 0);
        if(menu != null){
            menu.setGroupEnabled(0, isShouldEnabled);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ADD:
            Intent intent = new Intent(this, NetworkEditor.class);
            intent.putExtra(NetworkEditor.PLMN_NAME, "");
            intent.putExtra(NetworkEditor.PLMN_CODE, "");
            intent.putExtra(NetworkEditor.PLMN_PRIORITY, 0);
            intent.putExtra(NetworkEditor.PLMN_SERVICE, 0);
            intent.putExtra(NetworkEditor.PLMN_ADD, true);
            intent.putExtra(NetworkEditor.PLMN_SIZE, mPLMNList.size());
            startActivityForResult(intent, REQUEST_ADD);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init(TimeConsumingPreferenceListener listener, boolean skipReading, int mSlotId) {
        Xlog.d(LOG_TAG, "init with simSlot = " + mSlotId);
        if(CallSettings.isMultipleSim()) {
            mDualSimMode = android.provider.Settings.System.getInt(getContentResolver(), 
                    android.provider.Settings.System.DUAL_SIM_MODE_SETTING, -1);
            Xlog.d(LOG_TAG, "Settings.onResume(), mDualSimMode="+mDualSimMode);
        }
        if (!skipReading) {
            if (CallSettings.isMultipleSim()) {
                GeminiPhone dualPhone = (GeminiPhone)mPhone;
                dualPhone.getPreferedOperatorListGemini(mSlotId, 
                        mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_LIST, 
                        mSlotId, MyHandler.MESSAGE_GET_PLMN_LIST));
            } else {
                mPhone.getPreferedOperatorList(mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_LIST, 
                        mSlotId, MyHandler.MESSAGE_GET_PLMN_LIST));
            }
            
            if (listener != null) {
                listener.onStarted(mPLMNListContainer, true);
            }
        }
    }

    public void onFinished(Preference preference, boolean reading) {
        super.onFinished(preference, reading);
        setScreenEnabled();
    }
   
    private void getSIMCapability() {
        if (CallSettings.isMultipleSim()) {
            GeminiPhone dualPhone = (GeminiPhone)mPhone;
            dualPhone.getPOLCapabilityGemini(mSlotId, 
                    mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_CAPIBILITY, 
                    mSlotId, MyHandler.MESSAGE_GET_PLMN_CAPIBILITY));
        } else {
            mPhone.getPOLCapability(mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_CAPIBILITY, 
                    mSlotId, MyHandler.MESSAGE_GET_PLMN_CAPIBILITY));
        }
    }
    
    private void refreshPreference(ArrayList<NetworkInfoWithAcT> list) {
        if (mPLMNListContainer.getPreferenceCount() != 0) {
            mPLMNListContainer.removeAll();
        }
        
        if (this.mPreferenceMap != null) {
            mPreferenceMap.clear();
        }

        if (mPLMNList != null) {
            mPLMNList.clear();
        }
        mPLMNList = list;
        if (list == null || list.size() == 0) {
            if (DBG) Xlog.d(LOG_TAG, "refreshPreference : NULL PLMN list!");
            if (list == null) mPLMNList = new ArrayList<NetworkInfoWithAcT>();
            return ;
        }
        Collections.sort(list, new NetworkCompare());
        
        for (NetworkInfoWithAcT network : list) {
            addPLMNPreference(network);
            if (DBG) {
                Xlog.d(LOG_TAG, network.toString());
            }
        }
    }
    
    class NetworkCompare implements Comparator<NetworkInfoWithAcT> {

        public int compare(NetworkInfoWithAcT object1, NetworkInfoWithAcT object2) {
            return (object1.getPriority() - object2.getPriority());
        }
    }
    
    private void addPLMNPreference(NetworkInfoWithAcT network) {
        Preference pref = new Preference(this);
        String plmnName = network.getOperatorAlphaName();
        String extendName = getNWString(network.getAccessTechnology());
        pref.setTitle(plmnName + "(" + extendName + ")");
        mPLMNListContainer.addPreference(pref);
        mPreferenceMap.put(pref, network);
    }
    
    private void extractInfoFromNetworkInfo(Intent intent, NetworkInfoWithAcT info) {
        intent.putExtra(NetworkEditor.PLMN_CODE, info.getOperatorNumeric());
        intent.putExtra(NetworkEditor.PLMN_NAME, info.getOperatorAlphaName());
        intent.putExtra(NetworkEditor.PLMN_PRIORITY, info.getPriority());
        intent.putExtra(NetworkEditor.PLMN_SERVICE, info.getAccessTechnology());
        intent.putExtra(NetworkEditor.PLMN_ADD, false);
        intent.putExtra(NetworkEditor.PLMN_SIZE, mPLMNList.size());
    }
    
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Intent intent = new Intent(this, NetworkEditor.class);
        NetworkInfoWithAcT info = this.mPreferenceMap.get(preference);
        mOldInfo = info;
        extractInfoFromNetworkInfo(intent, info);            
        startActivityForResult(intent, REQUEST_EDIT);
        return true;
    }
    
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent intent) {
        Xlog.d(LOG_TAG, "resultCode = " + resultCode);
        Xlog.d(LOG_TAG, "requestCode = " + requestCode);

        if(intent != null){
            NetworkInfoWithAcT newInfo = createNetworkInfo(intent);
            if(resultCode == NetworkEditor.RESULT_DELETE){
                handleSetPLMN(genDelete(mOldInfo));
            } else if(resultCode == NetworkEditor.RESULT_MODIFY){
                if(requestCode == REQUEST_ADD){
                    handlePLMNListAdd(newInfo);
                } else if(requestCode == REQUEST_EDIT){
                    handleSetPLMN(genModifyEx(newInfo, mOldInfo));
                }
            }
        }
    }
    
    private NetworkInfoWithAcT createNetworkInfo(Intent intent) {
        String numberName = intent.getStringExtra(NetworkEditor.PLMN_CODE);
        String operatorName = intent.getStringExtra(NetworkEditor.PLMN_NAME);
        int priority = intent.getIntExtra(NetworkEditor.PLMN_PRIORITY, 0);
        int act = intent.getIntExtra(NetworkEditor.PLMN_SERVICE, 0);
        return new NetworkInfoWithAcT(operatorName, numberName, act, priority);
    }
    
    private void handleSetPLMN(ArrayList<NetworkInfoWithAcT> list) {
        numbers = list.size();
        onStarted(this.mPLMNListContainer, false);
        for (int i = 0; i < list.size(); i++) {
            NetworkInfoWithAcT ni = list.get(i);
            if (CallSettings.isMultipleSim()) {
                GeminiPhone dualPhone = (GeminiPhone)mPhone;
                dualPhone.setPOLEntryGemini(mSlotId, ni,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PLMN_LIST, mSlotId, MyHandler.MESSAGE_SET_PLMN_LIST));
            } else {
                mPhone.setPOLEntry(ni,
                        mHandler.obtainMessage(MyHandler.MESSAGE_SET_PLMN_LIST, mSlotId, MyHandler.MESSAGE_SET_PLMN_LIST));
                if (DBG) {
                    Xlog.d(LOG_TAG, "handleSetPLMN: " + ni.toString());
                }
            }
        }
    }
    
    private void handlePLMNListAdd(NetworkInfoWithAcT newInfo) {
        Xlog.d(LOG_TAG, "handlePLMNListAdd: add new network: " + newInfo);
        dumpNetworkInfo(mPLMNList);
        ArrayList<NetworkInfoWithAcT> list = new ArrayList<NetworkInfoWithAcT>();
        for (int i = 0; i < mPLMNList.size(); i++) {
            list.add(mPLMNList.get(i));
        }
        NetworkCompare nc = new NetworkCompare();
        int pos = Collections.binarySearch(mPLMNList, newInfo, nc);
        
        int properPos = -1;
        if (pos < 0) {
            properPos = getPosition(mPLMNList, newInfo);
        }
        if (properPos == -1) {
            list.add(pos, newInfo);
        } else {
            list.add(properPos, newInfo);
        }
        adjustPriority(list);
        dumpNetworkInfo(list);
        handleSetPLMN(list);
    }
    
    private void dumpNetworkInfo(List<NetworkInfoWithAcT> list) {
        if (!DBG) return;
        
        Xlog.d(LOG_TAG, "dumpNetworkInfo : **********start*******");
        for (int i = 0; i < list.size(); i++) {
            Xlog.d(LOG_TAG, "dumpNetworkInfo : " + list.get(i).toString());
        }
        Xlog.d(LOG_TAG, "dumpNetworkInfo : ***********stop*******");
    }
    
    private ArrayList<NetworkInfoWithAcT> genModifyEx(NetworkInfoWithAcT newInfo, NetworkInfoWithAcT oldInfo) {
        Xlog.d(LOG_TAG, "genModifyEx: change : " + oldInfo.toString() + "----> " + newInfo.toString());
        dumpNetworkInfo(mPLMNList);

        NetworkCompare nc = new NetworkCompare();
        int oldPos = Collections.binarySearch(mPLMNList, oldInfo, nc);
        int newPos = Collections.binarySearch(mPLMNList, newInfo, nc);
        
        ArrayList<NetworkInfoWithAcT> list = new ArrayList<NetworkInfoWithAcT>();
        if (newInfo.getPriority() == oldInfo.getPriority()) {
            list.add(newInfo);
            dumpNetworkInfo(list);
            return list;
        }
        
        for (int i = 0; i < mPLMNList.size(); i++) {
            list.add(mPLMNList.get(i));
        }
        
        int properPos = -1;
        if (newPos < 0) {
            properPos = getPosition(mPLMNList, newInfo);
            list.add(properPos, newInfo);
            dumpNetworkInfo(list);
            return list;
        }
        
        int adjustIndex = newPos;
        if (oldPos > newPos) {
            list.remove(oldPos);
            list.add(newPos, newInfo);
        } else if (oldPos < newPos){
            list.add(newPos + 1, newInfo);
            list.remove(oldPos);
            adjustIndex -= 1;
        } else {
            list.remove(oldPos);
            list.add(oldPos, newInfo);
        }
        
        adjustPriority(list);
        dumpNetworkInfo(list);
        return list;
    }
    
    private int getPosition(List<NetworkInfoWithAcT> list, NetworkInfoWithAcT newInfo) {
        int index = -1;
        if (list == null || list.size() == 0) {
            return 0;
        }
        
        if (list.size() == 1) {
            return list.get(0).getPriority() > newInfo.getPriority() ? 0 : 1;
        }
        
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getPriority() > newInfo.getPriority()) {
                if (i == 0) {
                    index = 0;
                } else {
                    index = i -1;
                }
            }
            break;
        }
        if (index == -1) {
            index = list.size();
        }
        return index;
    }
    
    private void adjustPriority(ArrayList<NetworkInfoWithAcT> list) {
        int priority = 0;
        for (NetworkInfoWithAcT info : list) {
            info.setPriority(priority++);
        }
    }
    
    private ArrayList<NetworkInfoWithAcT> genDelete(NetworkInfoWithAcT network) {
        Xlog.d(LOG_TAG, "genDelete : " + network.toString());
        dumpNetworkInfo(mPLMNList);
        
        ArrayList<NetworkInfoWithAcT> list = new ArrayList<NetworkInfoWithAcT>();
        NetworkCompare nc = new NetworkCompare();
        int pos = Collections.binarySearch(mPLMNList, network, nc);
        
        for (int i = 0; i < mPLMNList.size(); i++) {
            list.add(mPLMNList.get(i));
        }
        
        list.remove(pos);
        network.setOperatorNumeric(null);
        list.add(network);
        
        for (int i = list.size(); i < mCapability.lastIndex + 1; i++) {
            NetworkInfoWithAcT ni = new NetworkInfoWithAcT("", null, 1, i);
            list.add(ni);
        }
        adjustPriority(list);
        dumpNetworkInfo(list);
        
        return list;
    }
   
    private class MyHandler extends Handler {
        private static final int MESSAGE_GET_PLMN_LIST = 0;
        private static final int MESSAGE_SET_PLMN_LIST = 1;
        private static final int MESSAGE_GET_PLMN_CAPIBILITY = 2;
        
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_PLMN_LIST:
                    handleGetPLMNResponse(msg);
                    break;
                case MESSAGE_SET_PLMN_LIST:
                    handleSetPLMNResponse(msg);
                    break;
                    
                case MESSAGE_GET_PLMN_CAPIBILITY:
                    handleGetPLMNCapibilityResponse(msg);
                    break;
            }
        }
        
        public void handleGetPLMNResponse(Message msg) {
            if (DBG) Xlog.d(LOG_TAG, "handleGetPLMNResponse: done");
            
            if (msg.arg2 == MyHandler.MESSAGE_GET_PLMN_LIST) {
                onFinished(mPLMNListContainer, true);
            } else {
                onFinished(mPLMNListContainer, false);
            }
            
            AsyncResult ar = (AsyncResult) msg.obj;
            boolean isUserException = false;
            if (ar.exception != null) {
                Xlog.d(LOG_TAG, "handleGetPLMNResponse with exception = " + ar.exception);
                if (mPLMNList == null) {
                    mPLMNList = new ArrayList<NetworkInfoWithAcT>();
                }
            } else {
                refreshPreference((ArrayList<NetworkInfoWithAcT>)ar.result);
            }
        }
        
        public void handleSetPLMNResponse(Message msg) {
            if (DBG) Xlog.d(LOG_TAG, "handleSetPLMNResponse: done");
            numbers --;
            
            AsyncResult ar = (AsyncResult) msg.obj;
            boolean isUserException = false;
            if (ar.exception != null) {
                Xlog.d(LOG_TAG, "handleSetPLMNResponse with exception = " + ar.exception);
            } else {
                if (DBG) Xlog.d(LOG_TAG, "handleSetPLMNResponse: with OK result!");
            }
            
            if (numbers == 0) {
                if (CallSettings.isMultipleSim()) {
                    GeminiPhone dualPhone = (GeminiPhone)mPhone;
                    dualPhone.getPreferedOperatorListGemini(mSlotId, 
                            mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_LIST, 
                            mSlotId, MyHandler.MESSAGE_SET_PLMN_LIST));
                } else {
                    mPhone.getPreferedOperatorList(mHandler.obtainMessage(MyHandler.MESSAGE_GET_PLMN_LIST, 
                            mSlotId, MyHandler.MESSAGE_SET_PLMN_LIST));
                }
            }
        }
        
        public void handleGetPLMNCapibilityResponse(Message msg) {
            if (DBG) Xlog.d(LOG_TAG, "handleGetPLMNCapibilityResponse: done");
            
            AsyncResult ar = (AsyncResult) msg.obj;
            
            if (ar.exception != null) {
                Xlog.d(LOG_TAG, "handleGetPLMNCapibilityResponse with exception = " + ar.exception);
            } else {
                mCapability.setCapability((int[])ar.result);
            }
        }
    }
    
    private class SIMCapability {
        int firstIndex;
        int lastIndex;
        int firstFormat;
        int lastFormat;
        
        public SIMCapability(int startIndex, int stopIndex, int startFormat, int stopFormat) {
            firstIndex = startIndex;
            lastIndex = stopIndex;
            firstFormat = startFormat;
            lastFormat = stopFormat;
        }
        
        public void setCapability(int r[]) {
            if (r.length < 4) {
                return;
            }
            firstIndex = r[0];
            lastIndex = r[1];
            firstFormat = r[2];
            lastFormat = r[3];
        }
    }

    private String getNWString(int rilNW) {
        int index = NetworkEditor.covertRilNW2Ap(rilNW);
        String summary = "";
        if ((NetworkEditor.getBaseBand() & NetworkEditor.MODEM_MASK_TDSCDMA) == 0) {
            //WCDMA
            summary = getResources().getStringArray(
                R.array.plmn_prefer_network_mode_choices)[index];
        } else {
            //TD-SCDMA
            summary = getResources().getStringArray(
                R.array.plmn_prefer_network_mode_td_choices)[index];
        }
        return summary;
    }

    private void setScreenEnabled(){
        boolean isShouldEnabled = false;
        boolean isIdle = (mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE);
        isShouldEnabled = isIdle && (!mAirplaneModeEnabled) && (mDualSimMode != 0);
        getPreferenceScreen().setEnabled(isShouldEnabled);
        invalidateOptionsMenu();
    }
}
