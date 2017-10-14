package com.mediatek.phone;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.sip.SipManager;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;

import com.mediatek.phone.SimPickerAdapter.ItemHolder;

public final class SimPickerDialog {

    public static final int DEFAULT_SIM_NOT_SET = -5;

    private SimPickerDialog() {
    }

    public static AlertDialog create(Context context, String title,
            DialogInterface.OnClickListener listener) {
        return create(context, title, DEFAULT_SIM_NOT_SET,
                createItemHolder(context, true), listener);
    }

    public static AlertDialog create(Context context, String title,
            long suggestedSimId, DialogInterface.OnClickListener listener) {
        return create(context, title, suggestedSimId, createItemHolder(context, true), listener);
    }

    protected static AlertDialog create(Context context,
            String title, long suggestedSimId, List<ItemHolder> items,
            DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        SimPickerAdapter simAdapter = new SimPickerAdapter(context, items, suggestedSimId);
        builder.setSingleChoiceItems(simAdapter, -1, listener)
               .setTitle(title);
        return builder.create();
    }

    protected static List<ItemHolder> createItemHolder(Context context, boolean internet) {
        return createItemHolder(context, null, internet, null);
    }

    protected static List<ItemHolder> createItemHolder(Context context,
            String phone, boolean internet, ArrayList<Account> accounts) {

        List<SIMInfo> simInfos = SIMInfo.getInsertedSIMList(context);
        ArrayList<ItemHolder> itemHolders = new ArrayList<ItemHolder>();
        ItemHolder temp = null;

        if (!TextUtils.isEmpty(phone)) {
            temp = new ItemHolder(phone, SimPickerAdapter.ITEM_TYPE_TEXT);
            itemHolders.add(temp);
        }

        int index = 0;
        for (SIMInfo simInfo : simInfos) {
            temp = new ItemHolder(simInfo, SimPickerAdapter.ITEM_TYPE_SIM);
            if (index == 0) {
                itemHolders.add(temp);
            } else {
                int lastPos = itemHolders.size() - 1;
                SIMInfo temInfo = (SIMInfo)itemHolders.get(lastPos).mData;
                if (simInfo.mSlot < temInfo.mSlot) {
                    itemHolders.add(lastPos, temp);
                } else {
                    itemHolders.add(temp);
                }
            }
            index++;
        }

        int enabled = Settings.System.getInt(context.getContentResolver(),
                Settings.System.ENABLE_INTERNET_CALL, 0);
        if (internet && SipManager.isVoipSupported(context) && enabled == 1) {
            temp = new ItemHolder("Internet"/*context.getResources().getText(R.string.internet)*/,
                    SimPickerAdapter.ITEM_TYPE_INTERNET);
            itemHolders.add(temp);
        }
        
        if (accounts != null) {
            for (Account account : accounts) {
                temp = new ItemHolder(account, SimPickerAdapter.ITEM_TYPE_ACCOUNT);
                itemHolders.add(temp);
            }
        }
        return itemHolders;
    }

}
