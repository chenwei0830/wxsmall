package com.mediatek.settings;

import com.android.phone.R;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CallRejectListAdapter extends BaseAdapter {
    private static final String TAG = "CallRejectListAdapter";
    
    private LayoutInflater mInflater;
    private ArrayList<CallRejectListItem> mDataList;
    private Context mContext;
    private CheckSelectCallBack mCheckSelectCallBack = null;
    
    public CallRejectListAdapter(Context context, ArrayList<CallRejectListItem> data) {
        mContext = context;
        mDataList = data;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ItemViewHolder holder;
        if (convertView == null) {
            holder = new ItemViewHolder();
            convertView = mInflater.inflate(R.layout.call_reject_list_item, null);

            holder.name = (TextView)convertView.findViewById(R.id.call_reject_contact_name);
            holder.checkBox = (CheckBox)convertView.findViewById(R.id.call_reject_contact_check_btn);
            holder.id = position;
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mDataList.get(holder.id).setIsChecked(isChecked);
                    mCheckSelectCallBack.setChecked(isChecked);
                }
            });
            holder.phoneNum = (TextView)convertView.findViewById(R.id.call_reject_contact_phone_num);

            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder)convertView.getTag();
            holder.id = position;
        }
        
        if (holder.name != null) {
            holder.name.setText(mDataList.get(position).getName());
        }
        if (holder.checkBox != null) {
            holder.checkBox.setChecked(mDataList.get(position).getIsChecked());
        }
        if (holder.phoneNum != null) {
            holder.phoneNum.setText(mDataList.get(position).getPhoneNum());
        }
        return convertView;
    }

    @Override
    public int getCount() {
        return mDataList != null ? mDataList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mDataList != null ? mDataList.get(position) : null;
    }
    
    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setCheckSelectCallBack(CheckSelectCallBack callBack) {
        mCheckSelectCallBack = callBack;
    }

    static class ItemViewHolder {
        TextView name;
        TextView phoneNum;
        CheckBox checkBox;
        int id;
    }

    public interface CheckSelectCallBack {
        public void setChecked(boolean isChecked);
    }
}
