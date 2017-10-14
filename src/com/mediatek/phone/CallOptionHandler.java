package com.mediatek.phone;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.telephony.TelephonyManager;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.widget.ListAdapter;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.view.WindowManager;

import com.android.internal.telephony.Phone;
import com.mediatek.phone.CallOptionHelper.CallbackArgs;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.settings.Modem3GCapabilitySwitch;
import com.mediatek.settings.SipCallSetting;
import com.mediatek.CellConnService.CellConnMgr;

import com.android.phone.PhoneApp;
import com.android.phone.R;
import com.android.phone.Constants;
import com.android.phone.OutgoingCallBroadcaster;
import com.android.phone.PhoneInterfaceManager;
import com.android.phone.SipCallOptionHandler;
import com.android.phone.PhoneUtils;
import com.android.phone.PhoneUtils.VoiceMailNumberMissingException;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.IServiceStateExt;
import com.mediatek.settings.CallSettings;

public class CallOptionHandler implements CallOptionHelper.Callback,
        DialogInterface.OnDismissListener, DialogInterface.OnClickListener {

    private static final String TAG = "CallOptionHandler";

    private ProgressDialog mProgressDialog;
    protected Intent mIntent;

    protected String mNumber;

    protected Dialog[] mDialogs = new Dialog[CallOptionHelper.MAKE_CALL_REASON_MAX + 1];

    protected OnHandleCallOption mOnHandleCallOption;

    protected int mReason = CallOptionHelper.MAKE_CALL_REASON_OK;

    protected PhoneApp mApp;
    protected Context mContext;
    protected CallOptionHelper mCallOptionHelper;
    protected CellConnMgr mCellConnMgr;

    protected boolean mClicked;
    protected boolean mAssociateSimMissingClicked;
    protected CallOptionHelper.AssociateSimMissingArgs mAssociateSimMissingArgs;

    ///M: #Only emergency# text replace in case IMEI locked @{
    private IServiceStateExt mServiceStateExt;
    ///@}

    /**
     * The constructor for CallOptionhandler
     * @param context the caller's context
     */
    public CallOptionHandler(Context context) {
        mContext = context;

        mCallOptionHelper = CallOptionHelper.getInstance(mContext);
        mCallOptionHelper.setCallback(this);

        mApp = PhoneApp.getInstance();
        mCellConnMgr = mApp.cellConnMgr;
        ///M: #Only emergency# text replace in case IMEI locked @{
        mServiceStateExt = MediatekClassFactory.createInstance(IServiceStateExt.class, context);
        ///@}
    }

    /**
     * An empty method, this will be removed later
     * @param savedInstanceState the bundle for save status
     */
    public void onCreate(Bundle savedInstanceState) {
        //
    }

    /**
     * Dismiss the dialog when finish the call handle
     */
    public void onStop() {
        for (Dialog dialog : mDialogs) {
            if (dialog != null) {
                dialog.dismiss();
            }
        }
    }
    
    /**
     * Set the handler for call option handler
     * @param onHandleCallOption the handler
     */
    public void setOnHandleCallOption(OnHandleCallOption onHandleCallOption) {
        mOnHandleCallOption = onHandleCallOption;
    }

    /**
     * Dismiss the dialog when finish the handler
     * @param arg0 which dialog is dismissed
     */
    public void onDismiss(DialogInterface arg0) {
        log("onDismiss, mClicked = " + mClicked);
        if (arg0 == mDialogs[CallOptionHelper.MAKE_CALL_REASON_ASK]) {
            if (!mClicked) {
                handleCallOptionComplete();
            }
        } else if (arg0 == mDialogs[CallOptionHelper.MAKE_CALL_REASON_ASSOCIATE_MISSING]) {
            if (!mAssociateSimMissingClicked) {
                handleCallOptionComplete();
            }
        } else {
            handleCallOptionComplete();
        }
    }

    protected void handleCallOptionComplete() {
        if (mOnHandleCallOption != null) {
            mOnHandleCallOption.onHandleCallOption(false, null);
        }
    }

    protected void handleCallOptionComplete(final boolean continueOrFinish, Intent intent) {
        if (mOnHandleCallOption != null) {
            mOnHandleCallOption.onHandleCallOption(continueOrFinish, intent);
        }
    }

    /**
     * the listener for a click event for the Dialog
     * @param dialog which the button belong to
     * @param which which button is cliecked
     */
    public void onClick(DialogInterface dialog, int which) {
        //Profiler.trace(Profiler.CallOptionHandlerEnterOnClick);
        log("onClick, dialog = " + dialog + " which = " + which);
        if (dialog == mDialogs[CallOptionHelper.MAKE_CALL_REASON_ASK]) {
            final AlertDialog alert = (AlertDialog) dialog;
            final ListAdapter listAdapter = alert.getListView().getAdapter();
            final int slot = ((Integer)listAdapter.getItem(which)).intValue();

            log("onClick, slot = " + slot);
            if (slot == (int)Settings.System.VOICE_CALL_SIM_SETTING_INTERNET) {
                if (mIntent.getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)) {
                    Toast.makeText(PhoneApp.getInstance(), 
                            R.string.ip_dial_error_toast_for_sip_call_selected, Toast.LENGTH_SHORT)
                    .show();
                    handleCallOptionComplete();
                } else {
                    startSipCallOptionHandler();
                    handleCallOptionComplete();
                }
            } else {
                mIntent.putExtra(Constants.EXTRA_SLOT_ID, slot);

                if (needToCheckSIMStatus(slot)) {
                    if (slot >= 0) {
                        final int result = mCellConnMgr.handleCellConn(slot, CellConnMgr.REQUEST_TYPE_ROAMING, mRunnable);
                        log("result = " + result);
                        if (result == mCellConnMgr.RESULT_WAIT) {
                            showProgressIndication();
                        }
                    } else {
                        handleCallOptionComplete();
                    }
                } else {
                    final boolean bailout = 
                        afterCheckSIMStatus(com.mediatek.CellConnService.CellConnMgr.RESULT_STATE_NORMAL, slot);
                    if (bailout) {
                        handleCallOptionComplete();
                    }
                }
            }
            dialog.dismiss();
            mClicked = true;
        } else if (dialog == mDialogs[CallOptionHelper.MAKE_CALL_REASON_ASSOCIATE_MISSING]) {
            AlertDialog alert = (AlertDialog) dialog;
            if (mAssociateSimMissingArgs != null) {
                if (which == alert.BUTTON_POSITIVE) {
                    if (mAssociateSimMissingArgs.viaSimInfo != null) {
                        // via SIM
                        final int slot = mAssociateSimMissingArgs.viaSimInfo.mSlot;
                        mIntent.putExtra(Constants.EXTRA_SLOT_ID, slot);

                        // do not call CellConnService to avoid performance issues
                        if (needToCheckSIMStatus(slot)) {
                            if (slot >= 0) {
                                final int result = 
                                    mCellConnMgr.handleCellConn(slot, CellConnMgr.REQUEST_TYPE_ROAMING, mRunnable);
                                log("result = " + result);
                                if (result == mCellConnMgr.RESULT_WAIT) {
                                    showProgressIndication();
                                }
                            } else {
                                handleCallOptionComplete();
                            }
                        } else {
                            final boolean bailout = 
                                afterCheckSIMStatus(com.mediatek.CellConnService.CellConnMgr.RESULT_STATE_NORMAL, slot);
                            if (bailout) {
                                handleCallOptionComplete();
                            }
                        }
                    } else {
                        // via internet
                        startSipCallOptionHandler();
                        handleCallOptionComplete();
                    }
                } else if (which == alert.BUTTON_NEGATIVE) {
                    // user click 'other' button, show SIM selection dialog
                    // with default SIM suggested
                    CallbackArgs callbackArgs = mCallOptionHelper.new CallbackArgs();
                    callbackArgs.args = mAssociateSimMissingArgs.suggested;
                    callbackArgs.reason = CallOptionHelper.MAKE_CALL_REASON_ASK;
                    onMakeCall(callbackArgs);
                }
                mAssociateSimMissingClicked = true;
                mAssociateSimMissingArgs = null;
            }
            dialog.dismiss();
        }
        //Profiler.trace(Profiler.CallOptionHandlerLeaveOnClick);
    }

    void log(String msg) {
        PhoneLog.d(TAG, msg);
    }

    /**
     * The entry for making an call
     * @param intent the call intent
     */
    public void startActivity(Intent intent) {
        //Profiler.trace(Profiler.CallOptionHandlerEnterStartActivity);
        log("startActivity, intent = " + intent);
        mIntent = intent;
        try {
            mNumber = PhoneUtils.getInitialNumber(intent);
        } catch (VoiceMailNumberMissingException e) {
            log(e.getMessage());
        }
        

        int slot = intent.getIntExtra(Constants.EXTRA_SLOT_ID, -1);
        if (slot != -1) {
            if (needToCheckSIMStatus(slot)) {
                final int result = mCellConnMgr.handleCellConn(slot,
                                          CellConnMgr.REQUEST_TYPE_ROAMING, mRunnable);
                log("result = " + result);
                if (result == mCellConnMgr.RESULT_WAIT) {
                    showProgressIndication();
                }
            } else {
                handleCallOptionComplete(true, intent);
            }
            return;
        }

        mCallOptionHelper.makeCall(intent);
        //Profiler.trace(Profiler.CallOptionHandlerLeaveStartActivity);
    }

    /**
     * make the call when the pre-handle finish
     * @param args the args used to judge the call conditions
     */
    public void onMakeCall(final CallbackArgs args) {

        log("onMakeCall, reason = " + args.reason + " args = " + args.args);

        mReason = args.reason;

        switch (args.reason) {
            case CallOptionHelper.MAKE_CALL_REASON_OK: {
                int slot = -1;

                if (args.type == CallOptionHelper.DIAL_TYPE_SIP) {
                    //don't allowed ip dial for sip call
                    if (mIntent.getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)) {
                        Toast.makeText(PhoneApp.getInstance(), 
                                R.string.ip_dial_error_toast_for_sip_call_selected, Toast.LENGTH_SHORT)
                        .show();
                    } else {
                        // start sip call option handler
                        Intent selectPhoneIntent = newSipCallOptionHandlerIntent(mIntent);
                        log("startSipCallOptionHandler(): " + "calling startActivity: "
                                + selectPhoneIntent);
                        mContext.startActivity(selectPhoneIntent);
                    }
                    handleCallOptionComplete();
                    break;
                } else if (args.type == CallOptionHelper.DIAL_TYPE_VIDEO) {
                    // args.id is already slot id if video call
                    slot = (int)args.id;
                } else {
                    slot = SIMInfoWrapper.getDefault().getSimSlotById((int)args.id);

                    // if slot == -1, it's likely that no sim cards inserted
                    // using slot 0 by default
                    if (slot == -1) {
                        slot = 0;
                    }
                }
                mIntent.putExtra(Constants.EXTRA_SLOT_ID, slot);

                if (needToCheckSIMStatus(slot)) {
                    if (slot >= 0) {
                        final int result = mCellConnMgr.handleCellConn(slot, CellConnMgr.REQUEST_TYPE_ROAMING, mRunnable);
                        log("result = " + result);
                        if (result == mCellConnMgr.RESULT_WAIT) {
                            showProgressIndication();
                        }
                    } else {
                        handleCallOptionComplete();
                    }
                } else {
                    final boolean bialout = 
                        afterCheckSIMStatus(com.mediatek.CellConnService.CellConnMgr.RESULT_STATE_NORMAL, slot);
                    if (bialout) {
                        handleCallOptionComplete();
                    }
                }
            }
            break;

            case CallOptionHelper.MAKE_CALL_REASON_3G_SERVICE_OFF: {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    boolean bIsInsertSim = true;
                    if (null != PhoneApp.getInstance().phoneMgr) {
                        if (!PhoneApp.getInstance().phoneMgr.isSimInsert(Phone.GEMINI_SIM_1) && 
                                !PhoneApp.getInstance().phoneMgr.isSimInsert(Phone.GEMINI_SIM_2)) {
                            bIsInsertSim = false;
                        }
                    }
                    if (!bIsInsertSim) {
                        builder.setTitle(R.string.reminder)
                        ///M: #Only emergency# text replace in case IMEI locked @{
                        .setMessage(mServiceStateExt.isImeiLocked() ?
                                R.string.callFailed_cdma_notEmergency :
                                    R.string.callFailed_simError)
                        ///@}
                        .setNegativeButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
                    } else {
                         builder.setTitle(R.string.reminder)
                         .setMessage(R.string.turn_on_3g_service_message)
                         .setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null)
                         .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int which) {
                                  Intent intent = new Intent();
                                  intent.setClassName(Constants.PHONE_PACKAGE, Modem3GCapabilitySwitch.class.getName());
                                  mContext.startActivity(intent);
                              }
                          });
                    }
                } else {
                    if (null != PhoneApp.getInstance().phoneMgr) {
                        if (!PhoneApp.getInstance().phoneMgr.isSimInsert(Phone.GEMINI_SIM_1)) {
                            builder.setTitle(R.string.reminder)
                            ///M: #Only emergency# text replace in case IMEI locked @{
                            .setMessage(mServiceStateExt.isImeiLocked() ?
                                    R.string.callFailed_cdma_notEmergency :
                                        R.string.callFailed_simError)
                            ///@}
                            .setNegativeButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
                        }
                    } else {
                         builder.setTitle(R.string.reminder)
                         .setMessage(R.string.turn_on_3g_service_message)
                         .setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener) null)
                         .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int which) {
                                  Intent intent = new Intent();
                                  intent.setClassName(Constants.PHONE_PACKAGE, Modem3GCapabilitySwitch.class.getName());
                                  mContext.startActivity(intent);
                              }
                          });
                    }
                }
                Dialog dialog = builder.create();
                dialog.setOnDismissListener(this);
                mDialogs[args.reason] = dialog;
                dialog.show();
            }
            break;

            case CallOptionHelper.MAKE_CALL_REASON_SIP_DISABLED: {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(R.string.reminder)
                       .setMessage(R.string.enable_sip_dialog_message)
                       .setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener)null)
                       .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setClassName(Constants.PHONE_PACKAGE, SipCallSetting.class.getName());
                            mContext.startActivity(intent);
                        }
                    });
                Dialog dialog = builder.create();
                dialog.setOnDismissListener(this);
                mDialogs[args.reason] = dialog;
                dialog.show();
            }
            break;

            case CallOptionHelper.MAKE_CALL_REASON_SIP_NO_INTERNET: {

            }
            break;

            case CallOptionHelper.MAKE_CALL_REASON_SIP_START_SETTINGS: {

            }
            break;

            case CallOptionHelper.MAKE_CALL_REASON_ASK: {
                final long suggestedSim = 
                    args.args == null ? Settings.System.DEFAULT_SIM_NOT_SET : ((Long)args.args).longValue();

                AlertDialog dialog = SimPickerDialog.create(mContext, mContext.getResources()
                        .getString(R.string.sim_manage_call_via), suggestedSim, this);
                dialog.setOnDismissListener(this);
                mDialogs[args.reason] = dialog;
                mClicked = false;
                dialog.show();
            }
            break;

            case CallOptionHelper.MAKE_CALL_REASON_ASSOCIATE_MISSING: {
                Resources resources = mContext.getResources();
                CallOptionHelper.AssociateSimMissingArgs associateSimMissingArgs = 
                    (CallOptionHelper.AssociateSimMissingArgs) args.args;

                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

                final long associateSim = args.id;
                SIMInfo associateSimInfo = SIMInfoWrapper.getDefault().getSimInfoById((int)associateSim);
                String associateSimName = "";
                if (associateSimInfo != null) {
                    associateSimName = associateSimInfo.mDisplayName;
                }

                String viaSimName;
                if (associateSimMissingArgs.viaSimInfo != null) {
                    viaSimName = associateSimMissingArgs.viaSimInfo.mDisplayName;
                } else {
                    viaSimName = mContext.getResources().getString(
                            R.string.incall_call_type_label_sip);
                }

                String message = mContext.getResources().getString(
                        R.string.associate_sim_missing_message, associateSimName, viaSimName);
                builder.setTitle(args.number).setMessage(message).setPositiveButton(
                        android.R.string.yes, this);

                if (associateSimMissingArgs.type == CallOptionHelper.AssociateSimMissingArgs.ASSOCIATE_SIM_MISSING_YES_NO) {
                    builder.setNegativeButton(resources.getString(android.R.string.cancel), null);
                } else if (associateSimMissingArgs.type
                        == CallOptionHelper.AssociateSimMissingArgs.ASSOCIATE_SIM_MISSING_YES_OTHER) {
                    builder.setNegativeButton(resources.getString(R.string.associate_sim_missing_other), this);
                }

                Dialog dialog = builder.create();
                dialog.setOnDismissListener(this);
                mDialogs[args.reason] = dialog;
                dialog.show();
                mAssociateSimMissingArgs = associateSimMissingArgs;
                mAssociateSimMissingClicked = false;
            }
            break;

            case CallOptionHelper.MAKE_CALL_REASON_MISSING_VOICE_MAIL_NUMBER: {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(R.string.no_vm_number)
                       .setMessage(R.string.no_vm_number_msg)
                       .setNegativeButton(android.R.string.no, (DialogInterface.OnClickListener)null)
                       .setPositiveButton(R.string.add_vm_number_str, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setClassName(Constants.PHONE_PACKAGE, Constants.CALL_SETTINGS_CLASS_NAME);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(Phone.GEMINI_SIM_ID_KEY, (int)args.id);
                            mContext.startActivity(intent);
                        }
                    });
                Dialog dialog = builder.create();
                dialog.setOnDismissListener(this);
                mDialogs[args.reason] = dialog;
                dialog.show();
            }
            break;
            
            default:
                log("onMakeCall: no match case found!");
                break;
        }
    }

    private Intent newSipCallOptionHandlerIntent(Intent original) {
        Intent selectPhoneIntent = new Intent(
                OutgoingCallBroadcaster.ACTION_SIP_SELECT_PHONE, original.getData());
        selectPhoneIntent.setClass(mContext, SipCallOptionHandler.class);
        selectPhoneIntent.putExtra(OutgoingCallBroadcaster.EXTRA_NEW_CALL_INTENT,
                original);
        selectPhoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return selectPhoneIntent;
    }

    private String queryIPPrefix(int slot) {
        final SIMInfo simInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(slot);
        StringBuilder builder = new StringBuilder();
        builder.append("ipprefix");
        builder.append(simInfo.mSimId);
        final String key = builder.toString();
        
        final String ipPrefix = Settings.System.getString(mContext.getContentResolver(), key);
        log("queryIPPrefix, ipPrefix = " + ipPrefix);
        return ipPrefix;
    }

    /**
     * 
     * @param result
     * @param slot
     * @return true  : force OutgoingCallBroadcaster to be finished
     *         false : do not finish OutgoingCallbroadcaster
     */
    private boolean afterCheckSIMStatus(int result, int slot) {
        log("afterCheckSIMStatus, result = " + result + " slot = " + slot);

        if (result != com.mediatek.CellConnService.CellConnMgr.RESULT_STATE_NORMAL) {
            return true;
        }

        // ip dial only support voice call
        boolean noSim = SIMInfoWrapper.getDefault().getInsertedSimCount() == 0;
        if (!mIntent.getBooleanExtra(Constants.EXTRA_IS_VIDEO_CALL, false) 
                && mIntent.getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)
                && !noSim) {
            final String ipPrefix = queryIPPrefix(slot);
            if (TextUtils.isEmpty(ipPrefix)) {
                final Intent intent = new Intent("com.android.phone.MAIN");
                intent.setClassName(Constants.PHONE_PACKAGE, CallSettings.class.getName());
                final SIMInfo simInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(slot);
                intent.putExtra(Phone.GEMINI_SIM_ID_KEY, simInfo.mSimId);
                //pop toast to notify user.
                Toast.makeText(PhoneApp.getInstance(), 
                        R.string.ip_dial_error_toast_for_no_ip_prefix_number, Toast.LENGTH_SHORT)
                .show();
                
                mContext.startActivity(intent);
                return true;
            } else {
                if (mNumber.indexOf(ipPrefix) != 0) {
                    mIntent.putExtra(OutgoingCallBroadcaster.EXTRA_ACTUAL_NUMBER_TO_DIAL, ipPrefix + mNumber);
                }
            }
        }

        // a little tricky here, check the voice mail number
        if (Constants.VOICEMAIL_URI.equals(mIntent.getData().toString())) {
            TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if (TextUtils.isEmpty(telephonyManager.getVoiceMailNumberGemini(slot))) {
                CallbackArgs callbackArgs = mCallOptionHelper.new CallbackArgs();
                callbackArgs.reason = CallOptionHelper.MAKE_CALL_REASON_MISSING_VOICE_MAIL_NUMBER;
                callbackArgs.type = CallOptionHelper.DIAL_TYPE_VOICE;
                callbackArgs.id = slot;
                onMakeCall(callbackArgs);
                return false;
            } else {
                // put the voicemail number to the intent
                final String voicemailNumber = telephonyManager.getVoiceMailNumberGemini(slot);
                mIntent.putExtra(OutgoingCallBroadcaster.EXTRA_ACTUAL_NUMBER_TO_DIAL, voicemailNumber);
            }
        }

        int oldSolt = mIntent.getIntExtra(Constants.EXTRA_SLOT_ID, -1);
        log("afterCheckSIMStatus, oldSolt = " + oldSolt);
        if (oldSolt != -1 && slot != oldSolt) {
            mIntent.putExtra(Constants.EXTRA_SLOT_ID, slot);
        }

        handleCallOptionComplete(true, mIntent);
        return false;
    }

    private Runnable mRunnable = new Runnable() {
        public void run() {
            //Profiler.trace(Profiler.CallOptionHandlerEnterRun);
            final int result = mCellConnMgr.getResult();
            final int slot = mCellConnMgr.getPreferSlot();
            log("run, result = " + result + " slot = " + slot);

            final boolean bailout = afterCheckSIMStatus(result, slot);
            //Profiler.trace(Profiler.CallOptionHandlerLeaveRun);
            if (bailout) {
                handleCallOptionComplete();
            }
        }
    };

    protected void startSipCallOptionHandler() {
        final String number = PhoneNumberUtils.getNumberFromIntent(mIntent, mApp);
        final Uri uri = Uri.parse("sip:" + mNumber);
        mIntent.setData(uri);
        final Intent intent = newSipCallOptionHandlerIntent(mIntent);
        mContext.startActivity(intent);
    }

    private boolean isRoamingNeeded(int slot) {
        log("isRoamingNeeded slot = " + slot);
        if (slot == Phone.GEMINI_SIM_2) {
            log("isRoamingNeeded = " + SystemProperties.getBoolean("gsm.roaming.indicator.needed.2", false));
            return SystemProperties.getBoolean("gsm.roaming.indicator.needed.2", false);
        } else {
            log("isRoamingNeeded = " + SystemProperties.getBoolean("gsm.roaming.indicator.needed", false));
            return SystemProperties.getBoolean("gsm.roaming.indicator.needed", false);
        }
    }

    private boolean roamingRequest(int slot) {
        log("roamingRequest slot = " + slot);
        boolean bRoaming = false;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            bRoaming = TelephonyManager.getDefault().isNetworkRoamingGemini(slot);
        } else {
            bRoaming = TelephonyManager.getDefault().isNetworkRoaming();
        }

        if (bRoaming) {
            log("roamingRequest slot = " + slot + " is roaming");
        } else {
            log("roamingRequest slot = " + slot + " is not roaming");
            return false;
        }

        if (0 == Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ROAMING_REMINDER_MODE_SETTING, -1)
                && isRoamingNeeded(slot)) {
            log("roamingRequest reminder once and need to indicate");
            return true;
        }

        if (1 == Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ROAMING_REMINDER_MODE_SETTING, -1)) {
            log("roamingRequest reminder always");
            return true;
        }

        log("roamingRequest result = false");
        return false;
    }

    private boolean needToCheckSIMStatus(int slot) {
        final PhoneInterfaceManager phoneMgr = PhoneApp.getInstance().phoneMgr;
        if (slot < 0 || !phoneMgr.isSimInsert(slot)) {
            log("the sim not insert, bail out!");
            return false;
        }
        
        if (!phoneMgr.isRadioOnGemini(slot)) {
            return true;
        }
        
        return TelephonyManager.getDefault().getSimStateGemini(slot) != TelephonyManager.SIM_STATE_READY
                || roamingRequest(slot);
    }

    public interface OnHandleCallOption {
        /**
         * The calloption handler interface be called when finish the call handler
         * @param continueOrFinish indicate continue or finish
         * @param intent the call related information
         */
        void onHandleCallOption(final boolean continueOrFinish /*true for continue, false for finish*/,
                                       Intent intent);
    }

    /**
     * Show an onscreen "progress indication" with the specified title and message.
     */
    private void showProgressIndication() {
        log("showProgressIndication(searching network message )");

        // TODO: make this be a no-op if the progress indication is
        // already visible with the exact same title and message.

        dismissProgressIndication();  // Clean up any prior progress indication

        mProgressDialog = new ProgressDialog(mContext);
        //mProgressDialog.setTitle(getText(titleResId));
        mProgressDialog.setMessage(mContext.getResources().getString(R.string.sum_search_networks));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mProgressDialog.show();
    }

    /**
     * Dismiss the onscreen "progress indication" (if present).
     */
    private void dismissProgressIndication() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss(); // safe even if already dismissed
            mProgressDialog = null;
        }
    }
}
