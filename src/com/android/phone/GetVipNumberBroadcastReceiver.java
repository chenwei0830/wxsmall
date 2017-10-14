package com.android.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Phone;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;

import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetVipNumberBroadcastReceiver extends BroadcastReceiver {
    static final String LOG_TAG = "Vip-Number";
    String phoneNr;
    private static final int TEMP = 9999;
    private static boolean isRinging1 = false;
    private static int oldmode1 = TEMP;
    private static boolean isRinging2 = false;
    private static int oldmode2 = TEMP;

    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (FeatureOption.MTK_GEMINI_SUPPORT){
            telephony.listenGemini(new MyPhoneStateListener1(context),PhoneStateListener.LISTEN_CALL_STATE , Phone.GEMINI_SIM_1);
            telephony.listenGemini(new MyPhoneStateListener2(context),PhoneStateListener.LISTEN_CALL_STATE , Phone.GEMINI_SIM_2);
        }else{
            telephony.listen(new MyPhoneStateListener1(context), PhoneStateListener.LISTEN_CALL_STATE);
        }
        Bundle bundle = intent.getExtras();
        phoneNr= bundle.getString("incoming_number");
    }
    
    
    public class MyPhoneStateListener1 extends PhoneStateListener{
        
        private Context context;
        
        public  MyPhoneStateListener1(Context context){
            this.context = context;
        }
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            
            Log.v(LOG_TAG, "incomingNumber1=" + incomingNumber);
            
            AudioManager am = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (IsVipNumberAndChecked(context)) {
                        if(oldmode1 == TEMP){
                            oldmode1 = am.getRingerMode();
                        }
                        if(oldmode1 == AudioManager.RINGER_MODE_SILENT){
                            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        }
                        Log.d(LOG_TAG, "RINGING");
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if(oldmode1 != TEMP){
                        am.setRingerMode(oldmode1);
                        oldmode1 = TEMP;
                        Log.d(LOG_TAG, "IDLE");
                    }
                    
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(LOG_TAG, "OFFHOOK");
                    break;
            }
        }
    }
    
public class MyPhoneStateListener2 extends PhoneStateListener{
        
        private Context context;
        
        public  MyPhoneStateListener2(Context context){
            this.context = context;
        }
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            
            Log.v(LOG_TAG, "incomingNumber2=" + incomingNumber);
            
            AudioManager am = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (IsVipNumberAndChecked(context)) {
                        if(oldmode2 == TEMP){
                            oldmode2 = am.getRingerMode();
                        }
                        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        Log.d(LOG_TAG, "RINGING");
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if(oldmode2 != TEMP){
                        am.setRingerMode(oldmode2);
                        oldmode2 = TEMP;
                        Log.d(LOG_TAG, "IDLE");
                    }
                    
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(LOG_TAG, "OFFHOOK");
                    break;
            }
        }
    
}
    /*public class MyPhoneStateListener extends PhoneStateListener {
        private Context context;
        
        public MyPhoneStateListener() {

        }

        public MyPhoneStateListener(Context context) {
            this.context = context;
        }
        
        public void setContext(Context context){
            this.context = context;
        }

        public void onCallStateChanged(int state, String incomingNumber) {
            Log.d("zhaobo", "onCallStateChanged");
            
            super.onCallStateChanged(state, incomingNumber);
            Log.v(LOG_TAG, incomingNumber);
            AudioManager am = (AudioManager) context.getSystemService(context.AUDIO_SERVICE);
           // Log.v(LOG_TAG, "state = "+state);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (IsVipNumber(phoneNr)) {
                        if(old_ringer_mode == 9999){
                            old_ringer_mode = am.getRingerMode();
                        }
                        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                        Log.d(LOG_TAG, "RINGING");
                        
                    }
                    
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (old_ringer_mode == AudioManager.RINGER_MODE_NORMAL) {
                        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    } else if (old_ringer_mode == AudioManager.RINGER_MODE_SILENT) {
                        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    } else if (old_ringer_mode == AudioManager.RINGER_MODE_VIBRATE) {
                        am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    }
                    
                    if(old_ringer_mode != 9999){
                        am.setRingerMode(old_ringer_mode);
                        old_ringer_mode = 9999;
                        Log.d(LOG_TAG, "IDLE");
                    }
                    
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(LOG_TAG, "OFFHOOK");
                    break;
            }
        }
        
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            
        }        

        private boolean IsVipNumber(String num) {
            String[] string;
            int i;

            string = getData(context);
            for (i = 0; i < string.length; i++) {
                if (string[i].equals(phoneNr)) {
                    Log.d(LOG_TAG, "vipnumber = " + string[i] + " imcomingnumber = " + phoneNr);
                    return true;
                }
            }
            return false;
        }

    }*/
    
    private boolean IsVipNumberAndChecked(Context context) {
        String[] string;
        int i;
        String number;
        Map<Long,String> phoneMap = new HashMap<Long,String>();
        Map<Long,Integer> boolMap = new HashMap<Long,Integer>();
        
        Cursor cursor = context.getContentResolver().query(Uri.parse("content://com.android.settings.VipContentProvider"), null, null, null, null);
        while (cursor.moveToNext()) {
            Long vipId = cursor.getLong(0);
            String phoneNumber = cursor.getString(2);
            int bool = cursor.getInt(3);
            
            phoneMap.put(vipId, phoneNumber);
            boolMap.put(vipId, bool);
        }
        
        for(Map.Entry<Long, String> entry: phoneMap.entrySet()) {
            Long contact_id = entry.getKey();
            String phone = entry.getValue();
            int isChecked = boolMap.get(contact_id);
            Log.d("VIPnumber", "  contact_id = "+contact_id+" phone" + phone+"  isChecked = "+ isChecked);
            
            number = phone.replaceAll(" ", "").replace("+86", "");
            if(number.equals(phoneNr) && isChecked ==1){
                return true;
            }
        }
        return false;
    }
    
/*此方法获取settings数据库里的数据时，无法打开数据库*/ 
  /*  private VipDatabaseHelper dbHelper;
      private void getSettingVipNumber(Context context) {
        int i;
        String[] string;
        try {
            Context settingsContext = context.createPackageContext("com.android.settings",Context.CONTEXT_IGNORE_SECURITY | Context.MODE_WORLD_READABLE);
            dbHelper = new VipDatabaseHelper(settingsContext, "VIPNumberdatabase.db3" , null, 1);
            string = getData();
            for(i=0;i<string.length;i++){
                Log.d(LOG_TAG, "xxx-------number in database = "+string[i]);
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, "get database erro");
            e.printStackTrace();
            Log.d(LOG_TAG, e.getMessage());
        }
    }
    
    private String[] getData(){
        List<String> data = new ArrayList<String>();
        Cursor mCursor = dbHelper.getReadableDatabase().rawQuery(
                "select * from VIPList", null);
        while(mCursor.moveToNext()){
            data.add(mCursor.getString(1));
        }
        String [] mString = new String [data.size()] ;
        data.toArray(mString); 

        return mString;
    }*/
}