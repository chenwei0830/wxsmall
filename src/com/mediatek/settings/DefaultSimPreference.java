package com.mediatek.settings;

import java.util.HashMap;
import java.util.List;

import com.android.internal.telephony.Phone;
import com.android.phone.R;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.xlog.Xlog;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.preference.DialogPreference;
import android.provider.Telephony.SIMInfo;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView;
import android.text.TextUtils;

public class DefaultSimPreference extends DialogPreference 
            implements AdapterView.OnItemClickListener {
    
    final static String TAG = "DefaultSimPreference";
    private LayoutInflater mFlater;
    private String mValue;

    private List<SimItem> mSimItemList;
    private SelectionListAdapter mAdapter;
    private ListView mListView;
    private int mSelected = -1;
    private int mInitValue = -1;
    private Drawable mIcon;
    private Context mContext;
    
    private CellConnMgr mCellConnMgr;
    
    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;    
    private static final int PIN1_REQUEST_CODE = 302;

    public DefaultSimPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }   
    public DefaultSimPreference(Context context,AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mFlater = LayoutInflater.from(context);    

    }
    
    public void SetCellConnMgr(CellConnMgr cellConnmgr) {
        mCellConnMgr = cellConnmgr;
    }

    @Override
    public void onBindView(View view) {
        super.onBindView(view);
        TextView textSummary = (TextView) view.findViewById(android.R.id.summary);
        
        if (textSummary != null) {
            textSummary.setSingleLine();
            textSummary.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }
        Xlog.i(TAG, "summary is +"+this.getSummary());
    }


    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        Xlog.i(TAG, "onPrepareDialogBuilder");
        if (mSimItemList != null) {
            mAdapter = new SelectionListAdapter();      
            mListView = new ListView(mContext);
            mListView.setAdapter(mAdapter);
            mListView.setOnItemClickListener(this);
            mListView.setItemsCanFocus(false);
            builder.setView(mListView,0,0,0,0);
            builder.setNegativeButton(android.R.string.cancel, null);
        }
        super.onPrepareDialogBuilder(builder);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        Xlog.i(TAG, "onDialogClosed---mSelected = " + mSelected);
        Xlog.i(TAG, "onDialogClosed---mInitValue = " + mInitValue);    
        if (positiveResult && mSelected >= 0 && (mSelected != mInitValue)) {
            
            Xlog.i(TAG, "callChangeListener");
            long value;
            if (mSimItemList.get(mSelected).mSiminfo != null) {
                value = mSimItemList.get(mSelected).mSiminfo.mSimId;
            } else {
                value = android.provider.Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER;
            }
            callChangeListener(value);
            mInitValue = mSelected;
        }
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

        Xlog.i(TAG,"positon is "+position);
        
        if (v != null) {
            if (v.isEnabled() == false) {
                return;
            } else {
                SimItem simItem = mSimItemList.get(position);
                if ((simItem.mIsSim == true) &&
                        (mCellConnMgr != null) &&
                        (simItem.mState == Phone.SIM_INDICATOR_LOCKED)) {
                    mCellConnMgr.handleCellConn(simItem.mSiminfo.mSlot, PIN1_REQUEST_CODE);
                } else {
                    mSelected = position;
                    onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
                    getDialog().dismiss();  
                }
            }
        }
    }
    
    class SelectionListAdapter extends BaseAdapter {
        SelectionListAdapter(){
            
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mFlater.inflate(R.layout.preference_sim_default_select, null);
                holder = new ViewHolder();
                setViewHolderId(holder,convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            SimItem simItem = (SimItem)getItem(position);
            setNameAndNum(holder.textName,holder.textNum,simItem);
            setImageSim(holder.imageSim,simItem);
            setImageStatus(holder.imageStatus,simItem);
            setTextNumFormat(holder.textNumFormat,simItem);
            holder.ckRadioOn.setChecked(mSelected == position);
            Xlog.d(TAG,"DefaultSiminfo simItem.mState="+simItem.mState);
            if ((simItem.mState == Phone.SIM_INDICATOR_RADIOOFF)) {
                convertView.setEnabled(false);
                holder.textName.setEnabled(false);
                holder.textNum.setEnabled(false);
                holder.ckRadioOn.setEnabled(false);
            } else {
                convertView.setEnabled(true);
                holder.textName.setEnabled(true);
                holder.textNum.setEnabled(true);
                holder.ckRadioOn.setEnabled(true);
            }
            return convertView;
        }

        private void setTextNumFormat(TextView textNumFormat, SimItem simItem) {
            if (simItem.mIsSim == true) {
                if (simItem.mSiminfo.mNumber != null) {
                    switch (simItem.mSiminfo.mDispalyNumberFormat) {
                    case DISPLAY_NONE: 
                        textNumFormat.setVisibility(View.GONE);
                        break;
                    case DISPLAY_FIRST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mSiminfo.mNumber.length() >= 4) {
                            textNumFormat.setText(simItem.mSiminfo.mNumber.substring(0, 4));
                        } else {
                            textNumFormat.setText(simItem.mSiminfo.mNumber);
                        }
                        break;
                    case DISPLAY_LAST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mSiminfo.mNumber.length() >= 4) {
                            textNumFormat.setText(simItem.mSiminfo.mNumber.substring(simItem.mSiminfo.mNumber.length() - 4));
                        } else {
                            textNumFormat.setText(simItem.mSiminfo.mNumber);
                        }
                        break;
                    }           
                }
            }
        }

        private void setImageStatus(ImageView imageStatus, SimItem simItem) {
            if (simItem.mIsSim) {
                int res = getStatusResource(simItem.mState);
                if (res == -1) {
                    imageStatus.setVisibility(View.GONE);
                } else {
                    imageStatus.setVisibility(View.VISIBLE);
                    imageStatus.setImageResource(res);
                }
            }
        }

        private void setImageSim(RelativeLayout imageSim, SimItem simItem) {
            if(simItem.mIsSim == true) {
                int resColor = getSimColorResource(simItem.mSiminfo.mColor);
                if(resColor>=0) {
                    imageSim.setVisibility(View.VISIBLE);
                    imageSim.setBackgroundResource(resColor);
                }
            }else {
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
            if (simItem.mSiminfo != null) {
                if (simItem.mSiminfo.mDisplayName != null) {
                    textName.setVisibility(View.VISIBLE);
                    textName.setText(simItem.mSiminfo.mDisplayName);
                } else{
                    textName.setVisibility(View.GONE);
                }
            } else {
                textName.setVisibility(View.VISIBLE);
                textName.setText(R.string.service_3g_off);
            }
            if ((simItem.mIsSim) 
                    && ((simItem.mSiminfo.mNumber != null) 
                    && (simItem.mSiminfo.mNumber.length() != 0))) {
                textNum.setVisibility(View.VISIBLE);
                textNum.setText(simItem.mSiminfo.mNumber);
            } else {
                textNum.setVisibility(View.GONE);
            }
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
    
    public void setInitValue(int value) {
        mInitValue = value;
        mSelected = value;
    }
    
    long getValue() {
        return  mSimItemList.get(mSelected).mSiminfo.mSimId;
    }
    
    public void setInitData(List<SimItem> SimItemList) {
        Xlog.d(TAG,"setInitData()");
        mSimItemList = SimItemList;
        if(mAdapter != null){
            Xlog.d(TAG,"setInitData()+mAdapter!=null");
            mAdapter.notifyDataSetChanged();
        }
    }
    
    private void updateData() {
        int location=0;
        for (SimItem simitem: mSimItemList) {
            if (simitem.mIsSim) {
                SIMInfo siminfo = SIMInfo.getSIMInfoById(mContext, simitem.mSiminfo.mSimId);
                if(siminfo != null) {
                    SimItem simitemCopy = new SimItem(siminfo);
                    mSimItemList.set(location, simitemCopy);    
                }    
            }
            location++;
        }
    }

    private int getStatusResource(int state) {
        int iconId = 0;
        switch (state) {
        case Phone.SIM_INDICATOR_RADIOOFF:
            iconId = com.mediatek.internal.R.drawable.sim_radio_off;
            break;
        case Phone.SIM_INDICATOR_LOCKED:
            iconId = com.mediatek.internal.R.drawable.sim_locked;
            break;
        case Phone.SIM_INDICATOR_INVALID:
            iconId = com.mediatek.internal.R.drawable.sim_invalid;
            break;
        case Phone.SIM_INDICATOR_SEARCHING:
            iconId = com.mediatek.internal.R.drawable.sim_searching;
            break;
        case Phone.SIM_INDICATOR_ROAMING:
            iconId = com.mediatek.internal.R.drawable.sim_roaming;
            break;
        case Phone.SIM_INDICATOR_CONNECTED:
            iconId = com.mediatek.internal.R.drawable.sim_connected;
            break;
        case Phone.SIM_INDICATOR_ROAMINGCONNECTED:
            iconId = com.mediatek.internal.R.drawable.sim_roaming_connected;
            break;
        default:
            iconId = -1;
            break;
        }
        return iconId;
    }

    private int getSimColorResource(int color) {
        if ((color >= 0) && ( color <= 4)) {
            return Telephony.SIMBackgroundRes[color];
        } else {
            return -1;
        }
    }
}
