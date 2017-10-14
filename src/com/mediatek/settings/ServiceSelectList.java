package com.mediatek.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sip.Dialog;

import com.mediatek.xlog.Xlog;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.preference.DialogPreference;
import android.provider.Telephony.SIMInfo;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.phone.PhoneInterfaceManager;
import com.android.internal.telephony.gemini.GeminiPhone;

import com.mediatek.telephony.TelephonyManagerEx;
import com.android.phone.Utils;
import com.android.phone.PhoneApp;
import com.android.phone.R;

public class ServiceSelectList extends DialogPreference 
            implements AdapterView.OnItemClickListener, DialogInterface.OnClickListener {
    
    final static String TAG = "Settings/ServiceSelectList";
    private LayoutInflater mFlater;
    private String mValue;

    //private List<SimItem> mSimItemList;
    private SelectionListAdapter mAdapter;
    private ListView mListView;
    private int mSelected = -1;
    private int mSwitchTo = -1;
    private int mInitValue = -1;    
    private Drawable mIcon;
    private Context mContext;
    private PhoneInterfaceManager phoneMgr = null;
    private TelephonyManagerEx mTelephonyManagerEx;
    
    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;    
    
    private static final int PIN1_REQUEST_CODE = 302;
    
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private AlertDialog mAlertDialog = null;

    public ServiceSelectList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }   
    public ServiceSelectList(Context context,AttributeSet attrs, int defStyle) {
       super(context, attrs);
       
        mContext = context;
        mFlater = LayoutInflater.from(context);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.ListPreference, 0, 0);
        mEntries = a.getTextArray(com.android.internal.R.styleable.ListPreference_entries);
        mEntryValues = a.getTextArray(com.android.internal.R.styleable.ListPreference_entryValues);
        a.recycle(); 
        
        phoneMgr = PhoneApp.getInstance().phoneMgr;
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
    }
    
    @Override
    public void onBindView(View view) {
        super.onBindView(view);
    }

    
    @Override
     protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        mAdapter = new SelectionListAdapter(this.getContext());
        mListView = new ListView(mContext);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        this.mSelected = mAdapter.getHas3GService();

        mListView.setItemsCanFocus(false);
        mListView.setCacheColorHint(0);

        builder.setInverseBackgroundForced(true);
        builder.setView(mListView,0,0,0,0);

        builder.setNegativeButton(R.string.cancel, this);
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        Xlog.i(TAG, "onDialogClosed : mSelected = " + mSelected);
        Xlog.i(TAG, "onDialogClosed : mInitValue = " + mInitValue);

        if (positiveResult) {
            Xlog.i(TAG, "callChangeListener");
            callChangeListener(this.mAdapter.mSimItemList.get(mSelected).mSimID);
            mInitValue = mSelected;
        }

        this.dismissSelf();
    }
    
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            onDialogClosed(true);
            //this.handleSwitch(mAdapter.mSimItemList.get(mSwitchTo).mSlot);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            /*AlertDialog slectDialog = (AlertDialog)this.getDialog();
            if (slectDialog != null) {
                slectDialog.dismiss();
            }*/
            onDialogClosed(false);
        }
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Xlog.i(TAG, "onclick");
        Xlog.i(TAG,"positon is "+position);
        Xlog.i(TAG,"current select is " + mSelected);
        
        if (v.isEnabled() == false) {
            return ;
        } else if (position == mSelected) {
            dismissSelf();
            return ;
        } else {
            SimItem simItem = mAdapter.mSimItemList.get(position);
            if (simItem.mSimID == SimItem.DESCRIPTION_LIST_ITEM_SIMID) {
                return ;
            } else {
                mSelected = position;
                Xlog.i(TAG,"Switch to " + mSelected);
                int msgId = simItem.mSimID == SimItem.OFF_LIST_ITEM_SIMID ? R.string.confirm_3g_switch_to_off : R.string.confirm_3g_switch;
                AlertDialog newDialog = new AlertDialog.Builder(mContext)
                .setTitle(android.R.string.dialog_alert_title)
                .setPositiveButton(R.string.buttonTxtContinue, this)
                .setNegativeButton(R.string.cancel, this)
                .setCancelable(true)
                .setMessage(msgId)
                .create();
                
                newDialog.show();
                mAlertDialog = newDialog;
                this.onDialogClosed(false);
            }
        }
    }

    void dismissDialogs() {
        Xlog.d(TAG, "disable the 3G switch.");     

        android.app.Dialog dialog = this.getDialog();
        if (dialog != null) {
            dialog.dismiss();
        }
	if(mAlertDialog != null && mAlertDialog.isShowing()){
            mAlertDialog.dismiss();
	}
    }

    class SelectionListAdapter extends BaseAdapter {
        
        List<SimItem> mSimItemList;
        
        public SelectionListAdapter(List<SimItem> simItemList) {
            mSimItemList = simItemList;
        }
        
        public SelectionListAdapter(Context ctx) {
            mSimItemList = new ArrayList<SimItem>();
            List<SIMInfo> list = SIMInfo.getInsertedSIMList(ctx);
            for (SIMInfo info : list) {
                mSimItemList.add(new SimItem(info));
            }
            String offText = ctx.getResources().getString(R.string.service_3g_off);
            mSimItemList.add(new SimItem(offText, 0, SimItem.OFF_LIST_ITEM_SIMID));
            //mSimItemList.add(new SimItem(ctx.getResources().getString(R.string.modem_switching_tip), 0, SimItem.DESCRIPTION_LIST_ITEM_SIMID));
        }
        
        public int getCount() {
            return mSimItemList.size();
        }

        public Object getItem(int position) {
            return mSimItemList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public int getHas3GService() {
            int index = -1;
            for (int i = 0; i < mSimItemList.size(); ++i) {
                /*SimItem item = mSimItemList.get(i);
                if (item.mIsSim && item.has3GCapability) {
                    index = i;
                    break;
                } else if (!item.mIsSim && item.mSimID == SimItem.OFF_LIST_ITEM_SIMID) {
                    index = i;
                }*/
                SimItem item = mSimItemList.get(i);
                if (item.has3GCapability) {
                    index = i;
                    break;
                }
            }
            return index;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if(convertView==null) {
                convertView=mFlater.inflate(R.layout.preference_sim_list, null);
                holder=new ViewHolder();
                setViewHolderId(holder,convertView);
                convertView.setTag(holder);
            }
            else {
                holder=(ViewHolder)convertView.getTag();
            }
            SimItem simItem = (SimItem)getItem(position);
            setNameAndNum(holder.textName,holder.textNum,simItem);
            setImageSim(holder.imageSim,simItem);
            setImageStatus(holder.imageStatus,simItem);
            setTextNumFormat(holder.textNumFormat,simItem);
            holder.ckRadioOn.setChecked(mSelected == position);
            return convertView;
          }
        private void setTextNumFormat(TextView textNumFormat, SimItem simItem) {
            if(simItem.mIsSim == true) {
                if (simItem.mNumber!=null) {
                    switch (simItem.mDispalyNumberFormat) {
                    case DISPLAY_NONE: 
                        textNumFormat.setVisibility(View.GONE);
                        break;
                    case DISPLAY_FIRST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mNumber.length()>=4) {
                            textNumFormat.setText(simItem.mNumber.substring(0, 4));
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                        }
                        break;
                    case DISPLAY_LAST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mNumber.length()>=4) {
                            textNumFormat.setText(simItem.mNumber.substring(simItem.mNumber.length()-4));
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                        }
                        break;
                    }           
                }
            }
            
        }
        private void setImageStatus(ImageView imageStatus, SimItem simItem) {
            if(simItem.mIsSim==true){
		int status = mTelephonyManagerEx.getSimIndicatorStateGemini(simItem.mSlot);
		int res = Utils.getStatusResource(status); 
                if(res == -1) {
                    imageStatus.setVisibility(View.GONE);
                }
                else {
                    imageStatus.setVisibility(View.VISIBLE);
                    imageStatus.setImageResource(res);
                }
            }
                
        }
        private void setImageSim(RelativeLayout imageSim, SimItem simItem) {
            if(simItem.mIsSim == true) {
                int resColor = Utils.getSimColorResource(simItem.mColor);
                if(resColor>=0) {
                    imageSim.setVisibility(View.VISIBLE);
                    imageSim.setBackgroundResource(resColor);
                }
            }
            else if (simItem.mColor == 8){
                imageSim.setVisibility(View.VISIBLE);
                imageSim.setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
            }
            else {
                imageSim.setVisibility(View.GONE);
            }
        }

        private void setViewHolderId(ViewHolder holder, View convertView) {
            holder.textName=(TextView)convertView.findViewById(R.id.simNameSel);
            holder.textNum=(TextView)convertView.findViewById(R.id.simNumSel);
            holder.imageStatus=(ImageView)convertView.findViewById(R.id.simStatusSel);
            holder.textNumFormat=(TextView)convertView.findViewById(R.id.simNumFormatSel);
            holder.ckRadioOn=(RadioButton)convertView.findViewById(R.id.Enable_select);
            holder.imageSim=(RelativeLayout)convertView.findViewById(R.id.simIconSel);
        }

        private void setNameAndNum(TextView textName,TextView textNum, SimItem simItem) {
            if(simItem.mName != null) {
                textName.setVisibility(View.VISIBLE);
                textName.setText(simItem.mName);
            }
            else
                textName.setVisibility(View.GONE);
            
            if((simItem.mIsSim == true) &&((simItem.mNumber != null)&&(simItem.mNumber.length() != 0))) {
                textNum.setVisibility(View.VISIBLE);
                textNum.setText(simItem.mNumber);
            }
            else
                textNum.setVisibility(View.GONE);
        }
        class ViewHolder{
            TextView textName;
            TextView textNum;
            RelativeLayout imageSim;
            ImageView imageStatus;
            TextView textNumFormat;
            RadioButton ckRadioOn;
            
        }
    }
    
    void SetRadioCheched(int index) {
        int listSize = mListView.getCount();
        
        for (int k=0; k<listSize; k++) {
            
            View ItemView = mListView.getChildAt(k);
            RadioButton btn = (RadioButton)ItemView.findViewById(R.id.Enable_select);
            if(btn!=null){
                btn.setChecked((k == index)?true:false);
            }
        }
    }
    
    void setInitValue(int value) {
        mInitValue = value;
        mSelected = value;
    }
    
    class SimItem {
        public final static long DESCRIPTION_LIST_ITEM_SIMID = -2;
        public final static long OFF_LIST_ITEM_SIMID = -1; 
        public boolean has3GCapability = false;
        
        public boolean mIsSim = true;
        public String mName = null;
        public String mNumber = null;
        public int mDispalyNumberFormat = 0;
        public int mColor = -1;
        public int mSlot = -1;
        public long mSimID = -1;
        public int mState = Phone.SIM_INDICATOR_NORMAL;
        
        //Constructor for not real sim
        public SimItem (String name, int color,long simID) {
            mName = name;
            mColor = color;
            mIsSim = false;
            mSimID = simID;
            if (phoneMgr != null) {
                has3GCapability = mSlot == phoneMgr.get3GCapabilitySIM();
            }
        }
        //constructor for sim
        public SimItem (SIMInfo siminfo) {
            mIsSim = true;
            mName = siminfo.mDisplayName;
            mNumber = siminfo.mNumber;
            mDispalyNumberFormat = siminfo.mDispalyNumberFormat;
            mColor = siminfo.mColor;
            mSlot = siminfo.mSlot;
            mSimID = siminfo.mSimId;
            if (phoneMgr != null) {
                has3GCapability = mSlot == phoneMgr.get3GCapabilitySIM();
            }
        }
    }
    
    void dismissSelf() {
        Xlog.d(TAG, "Dismiss the select list.");     
        AlertDialog dialog = (AlertDialog)this.getDialog();
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
