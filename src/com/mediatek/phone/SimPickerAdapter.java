package com.mediatek.phone;

import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.mediatek.telephony.TelephonyManagerEx;
import com.android.phone.PhoneApp;
import com.android.phone.PhoneInterfaceManager;
import com.android.phone.R;

public class SimPickerAdapter extends BaseAdapter {

    public static final int ITEM_TYPE_UNKNOWN  = -1;
    public static final int ITEM_TYPE_SIM      =  0;
    public static final int ITEM_TYPE_INTERNET =  1;
    public static final int ITEM_TYPE_TEXT     =  2;
    public static final int ITEM_TYPE_ACCOUNT  =  3;
    private static final int NUMBER_LENGTH_MINUS = 4;
    private static final int VIEW_TYPE_COUNT = 4;

    Context mContext;
    long mSuggestedSimId;
    List<ItemHolder> mItems;
    
    public SimPickerAdapter(Context context, List<ItemHolder> items, long suggestedSimId) {
        mContext = context;
        mSuggestedSimId = suggestedSimId;
        mItems = items;
    }

    public int getCount() {
        // TODO Auto-generated method stub
        return mItems.size();
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
    
    @Override
    public int getItemViewType(int position) {
        // TODO Auto-generated method stub
        ItemHolder itemHolder = mItems.get(position);
        return itemHolder.mType;
    }

    public Object getItem(int position) {
        ItemHolder itemHolder = mItems.get(position);
        if (itemHolder.mType == ITEM_TYPE_SIM) {
            return Integer.valueOf(((SIMInfo)itemHolder.mData).mSlot);
        } else if (itemHolder.mType == ITEM_TYPE_INTERNET) {
            return Integer.valueOf((int)Settings.System.VOICE_CALL_SIM_SETTING_INTERNET);
        } else if (itemHolder.mType == ITEM_TYPE_TEXT || itemHolder.mType == ITEM_TYPE_ACCOUNT) {
            return itemHolder.mData;
        } else {
            return null;
        }
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;
        int viewType = getItemViewType(position);
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            holder = new ViewHolder();

            if (viewType == ITEM_TYPE_SIM) {
                view = inflater.inflate(R.layout.sim_picker_item, null);
                holder.mSimSignal = (TextView)view.findViewById(R.id.simSignal);
                holder.mSimStatus = (ImageView)view.findViewById(R.id.simStatus);
                holder.mShortPhoneNumber = (TextView)view.findViewById(R.id.shortPhoneNumber);
                holder.mDisplayName = (TextView)view.findViewById(R.id.displayName);
                holder.mPhoneNumber = (TextView)view.findViewById(R.id.phoneNumber);
                holder.mSimIcon = view.findViewById(R.id.simIcon);
                holder.mSuggested = (TextView)view.findViewById(R.id.suggested);
            } else if (viewType == ITEM_TYPE_INTERNET) {
                view = inflater.inflate(R.layout.sim_picker_item_internet, null);
                holder.mInternetIcon = (ImageView)view.findViewById(R.id.internetIcon);
            } else if (viewType == ITEM_TYPE_TEXT || viewType == ITEM_TYPE_ACCOUNT) {
                view = inflater.inflate(R.layout.sim_picker_item_text, null);
                holder.mText = (TextView)view.findViewById(R.id.text);
            }
            view.setTag(holder);
        }

        holder = (ViewHolder)view.getTag();

        if (viewType == ITEM_TYPE_SIM) {
            SIMInfo simInfo = (SIMInfo)mItems.get(position).mData;
            holder.mDisplayName.setText(simInfo.mDisplayName);
            holder.mSimIcon.setBackgroundResource(Telephony.SIMBackgroundRes[simInfo.mColor]);

            if (simInfo.mSimId == mSuggestedSimId) {
                holder.mSuggested.setVisibility(View.VISIBLE);
            } else {
                holder.mSuggested.setVisibility(View.GONE);
            }

            String shortNumber = "";
            PhoneInterfaceManager telephony = PhoneApp.getInstance().phoneMgr;
            if (!TextUtils.isEmpty(simInfo.mNumber)) {
                switch (simInfo.mDispalyNumberFormat) {
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST:
                        if (simInfo.mNumber.length() <= NUMBER_LENGTH_MINUS) {
                            shortNumber = simInfo.mNumber;
                        } else {
                            shortNumber = simInfo.mNumber.substring(0, NUMBER_LENGTH_MINUS);
                        }
                        break;
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST:
                        if (simInfo.mNumber.length() <= 4) {
                            shortNumber = simInfo.mNumber;
                        } else {
                            shortNumber = simInfo.mNumber.substring(simInfo.mNumber.length()
                                    - NUMBER_LENGTH_MINUS, simInfo.mNumber.length());
                        }
                        break;
                    case 0://android.provider.Telephony.SimInfo.DISPLAY_NUMBER_NONE:
                        shortNumber = "";
                        break;
                    default:
                        break;
                }
                holder.mPhoneNumber.setText(simInfo.mNumber);
                holder.mPhoneNumber.setVisibility(View.VISIBLE);
            } else {
                holder.mPhoneNumber.setVisibility(View.GONE);
            }
            holder.mShortPhoneNumber.setText(shortNumber);
            holder.mSimSignal.setVisibility(View.INVISIBLE);
            holder.mSimStatus.setImageResource(getSimStatusIcon(simInfo.mSlot));
        } else if (viewType == ITEM_TYPE_INTERNET) {
            holder.mInternetIcon
                    .setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
        } else if (viewType == ITEM_TYPE_TEXT) {
            String text = (String)mItems.get(position).mData;
            holder.mText.setText(text);
        } else if (viewType == ITEM_TYPE_ACCOUNT) {
            Account account = (Account)mItems.get(position).mData;
            holder.mText.setText((String)account.name);
        }

        return view;
    }

    protected int getSimStatusIcon(int slot) {

        TelephonyManagerEx telephonyManager = TelephonyManagerEx.getDefault();
        int state = telephonyManager.getSimIndicatorStateGemini(slot);
        int resourceId = 0;
        switch (state) {
            case com.android.internal.telephony.Phone.SIM_INDICATOR_LOCKED:
                resourceId = com.mediatek.internal.R.drawable.sim_locked;
                break;
            case com.android.internal.telephony.Phone.SIM_INDICATOR_RADIOOFF:
                resourceId = com.mediatek.internal.R.drawable.sim_radio_off;
                break;
            case com.android.internal.telephony.Phone.SIM_INDICATOR_ROAMING:
                resourceId = com.mediatek.internal.R.drawable.sim_roaming;
                break;
            case com.android.internal.telephony.Phone.SIM_INDICATOR_SEARCHING:
                resourceId = com.mediatek.internal.R.drawable.sim_searching;
                break;
            case com.android.internal.telephony.Phone.SIM_INDICATOR_INVALID:
                resourceId = com.mediatek.internal.R.drawable.sim_invalid;
                break;
            case com.android.internal.telephony.Phone.SIM_INDICATOR_CONNECTED:
                resourceId = com.mediatek.internal.R.drawable.sim_connected;
                break;
            case com.android.internal.telephony.Phone.SIM_INDICATOR_ROAMINGCONNECTED:
                resourceId = com.mediatek.internal.R.drawable.sim_roaming_connected;
                break;
            default:
                break;
        }
        return resourceId;
    }

    private class ViewHolder {
        View      mSimIcon;
        ImageView mSimStatus;
        TextView  mSimSignal;
        TextView  mShortPhoneNumber;
        TextView  mDisplayName;
        TextView  mPhoneNumber;
        TextView  mSuggested;
        TextView  mText;
        ImageView mInternetIcon;
        RadioButton mRadioButton;
    }

    public static class ItemHolder {
        public Object mData;
        public int mType;

        public ItemHolder(Object data, int type) {
            this.mData = data;
            this.mType = type;
        }
    }
}
