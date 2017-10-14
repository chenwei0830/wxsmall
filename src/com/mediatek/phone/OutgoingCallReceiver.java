package com.mediatek.phone;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;

import com.android.phone.OutgoingCallBroadcaster;
import com.android.phone.Constants;
import com.android.phone.PhoneUtils;
import com.android.phone.SipCallOptionHandler;
import com.android.phone.PhoneApp;

public class OutgoingCallReceiver extends BroadcastReceiver {

    private static final String TAG = "OutgoingCallReceiver";

    private static final String PERMISSION = android.Manifest.permission.PROCESS_OUTGOING_CALLS;

    private static final String ACTION = "com.mediatek.phone.OutgoingCallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION.equals(intent.getAction())) {
            ///Profiler.trace(Profiler.OutgoingCallReceiverEnterActionOnReceive);
            Intent broadcastIntent = new Intent(Intent.ACTION_NEW_OUTGOING_CALL);

            String number = PhoneNumberUtils.getNumberFromIntent(intent, context);
            if (number != null) {
                broadcastIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
            }

            Uri uri = intent.getData();
            broadcastIntent.putExtra(OutgoingCallBroadcaster.EXTRA_ORIGINAL_URI, uri.toString());
            broadcastIntent.putExtra(Constants.EXTRA_SLOT_ID, intent.getIntExtra(Constants.EXTRA_SLOT_ID, 0));
            broadcastIntent.putExtra(Constants.EXTRA_IS_VIDEO_CALL, intent.getBooleanExtra(Constants.EXTRA_IS_VIDEO_CALL, false));

            PhoneUtils.checkAndCopyPhoneProviderExtras(intent, broadcastIntent);
            context.sendOrderedBroadcast(broadcastIntent, PERMISSION, this,
                    null,  // scheduler
                    Activity.RESULT_OK,  // initialCode
                    number,  // initialData: initial value for the result data
                    null);  // initialExtras
            //Profiler.trace(Profiler.OutgoingCallReceiverLeaveActionOnReceive);
        } else if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {
            //Profiler.trace(Profiler.OutgoingCallReceiverEnterBroadcastOnReceive);
            String number = getResultData();

            String originalUri = intent.getStringExtra(OutgoingCallBroadcaster.EXTRA_ORIGINAL_URI);
            Uri uri = Uri.parse(originalUri);

            Intent newIntent = new Intent(Intent.ACTION_CALL, uri);
            newIntent.putExtra(Constants.EXTRA_IS_VIDEO_CALL, intent.getBooleanExtra(Constants.EXTRA_IS_VIDEO_CALL, false));
            
            //When the uri is tel:xxxxx@xxxxx, we check the extra(ALPS00248951)
            if ((PhoneNumberUtils.isUriNumber(number) && intent.getIntExtra(Constants.EXTRA_SLOT_ID, -1) == -1)
                    || Constants.SCHEME_SIP.equals(uri.getScheme())) {
                if (Constants.SCHEME_SIP.equals(uri.getScheme()) && !(PhoneNumberUtils.isUriNumber(number))) {
                    number = PhoneNumberUtils.stripSeparators(number);
                    newIntent.putExtra(OutgoingCallBroadcaster.EXTRA_ACTUAL_NUMBER_TO_DIAL, number);
                }  
                startSipCallOptionHandler(context, newIntent);
            } else {
                number = specialNumberTransfer(number);
                newIntent.putExtra(OutgoingCallBroadcaster.EXTRA_ACTUAL_NUMBER_TO_DIAL, number);
                newIntent.putExtra(Constants.EXTRA_SLOT_ID, intent.getIntExtra(Constants.EXTRA_SLOT_ID, 0));
                PhoneApp.getInstance().callController.placeCall(newIntent);
            }
            //Profiler.trace(Profiler.OutgoingCallReceiverLeaveBroadcastOnReceive);
        }
    }

    protected void startSipCallOptionHandler(Context context, Intent intent) {
        final String number = PhoneNumberUtils.getNumberFromIntent(intent, context);
        final Uri uri = Uri.fromParts("sip", number, null);
        intent.setData(uri);
        final Intent sipIntent = newSipCallOptionHandlerIntent(context, intent);
        context.startActivity(sipIntent);
    }

    private Intent newSipCallOptionHandlerIntent(Context context, Intent original) {
        Intent selectPhoneIntent = new Intent(
                OutgoingCallBroadcaster.ACTION_SIP_SELECT_PHONE, original.getData());
        selectPhoneIntent.setClass(context, SipCallOptionHandler.class);
        selectPhoneIntent.putExtra(OutgoingCallBroadcaster.EXTRA_NEW_CALL_INTENT,
                original);
        selectPhoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return selectPhoneIntent;
    }
    
    private String specialNumberTransfer(String number) {
        if (null == number) {
            return null;
        }
        number = number.replace('p', PhoneNumberUtils.PAUSE).replace('w', PhoneNumberUtils.WAIT);
        number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
        number = PhoneNumberUtils.stripSeparators(number);
        return number;
    }
}
