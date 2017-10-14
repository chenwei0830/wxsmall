/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.phone.vt;

import android.app.ActivityManagerNative;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.sip.SipPhone;
import com.android.phone.Constants;
import com.android.phone.PhoneApp;
import com.android.phone.PhoneUtils;
import com.android.phone.R;

import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.settings.VTAdvancedSetting;
import com.mediatek.settings.VTSettingUtils;
import com.mediatek.vt.VTManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public final class VTCallUtils {

    private static final String LOG_TAG = "VTCallUtils";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    /**
     * Video Call will control some resource, such as Camera, Media.
     * So Phone App will broadcast Intent to other APPs before acquire and after release the resource.
     * Intent action:
     * Before - "android.phone.extra.VT_CALL_START"
     * After - "android.phone.extra.VT_CALL_END"
     */
    public static final String VT_CALL_START = "android.phone.extra.VT_CALL_START";
    public static final String VT_CALL_END = "android.phone.extra.VT_CALL_END";

    // "chmod" is a command to change file permission, 6 is for User, 4 is for Group
    private static final String CHANGE_FILE_PERMISSION = "chmod 640 ";

    private static final int BITMAP_COMPRESS_QUALITY = 100;

    private VTCallUtils() {
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    /**
     * for InCallScreen to update VT UI
     * In old code, there are more than 2 modes.
     * But change to 2 mode OPEN/CLOSE.
     * In fact, it can be handled by InCallScreen.mVTInCallCanvas's visible/invisible.
     * But we also use VTScreenMode, because if need more than 2 modes in future, we can add it easily. 
     */
    public static enum VTScreenMode {
        VT_SCREEN_CLOSE,
        VT_SCREEN_OPEN
    }

    /**
     * Show video call incoming GUI
     */
    public static void showVTIncomingCallUi() {
        if (DBG) {
            log("showVTIncomingCallUi()...");
        }

        VTSettingUtils.getInstance().updateVTEngineerModeValues();

        PhoneApp app = PhoneApp.getInstance();

        try {
            ActivityManagerNative.getDefault().closeSystemDialogs("call");
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        app.preventScreenOn(true);
        app.requestWakeState(PhoneApp.WakeState.FULL);

        if (DBG) {
            log("- updating notification from showVTIncomingCall()...");
        }
        // incoming call use voice call GUI, so use "true" as parameter
        app.displayCallScreen(true);
    }

    /**
     * Check video call used files and copy them to special path
     */
    public static void checkVTFile() {
        if (DBG) {
            log("start checkVTFile() ! ");
        }
        if (!(new File(VTAdvancedSetting.getPicPathDefault()).exists())) {
            if (DBG) {
                log("checkVTFile() : the default pic file not exists , create it ! ");
            }

            try {
                Bitmap btp1 =
                    BitmapFactory.decodeResource(PhoneApp.getInstance().getResources(),
                                                 R.drawable.vt_incall_pic_qcif);
                VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathDefault(), btp1);
                btp1.recycle();
                if (DBG) {
                    log(" - Bitmap.isRecycled() : " + btp1.isRecycled());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!(new File(VTAdvancedSetting.getPicPathUserselect()).exists())) {
            if (DBG) {
                log("checkVTFile() : the default user select pic file not exists , create it ! ");
            }
            try {
                Bitmap btp2 = BitmapFactory.decodeResource(PhoneApp.getInstance().getResources(),
                                                           R.drawable.vt_incall_pic_qcif);
                VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathUserselect() , btp2);
                btp2.recycle();
                if (DBG) {
                    log(" - Bitmap.isRecycled() : " + btp2.isRecycled());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!(new File(VTAdvancedSetting.getPicPathDefault2()).exists())) {
            if (DBG) {
                log("checkVTFile() : the default pic2 file not exists , create it ! ");
            }
            try {
                Bitmap btp3 = BitmapFactory.decodeResource(PhoneApp.getInstance().getResources(),
                                                           R.drawable.vt_incall_pic_qcif);
                VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathDefault2(), btp3);
                btp3.recycle();
                if (DBG) {
                    log(" - Bitmap.isRecycled() : " + btp3.isRecycled());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!(new File(VTAdvancedSetting.getPicPathUserselect2()).exists())) {
            if (DBG) {
                log("checkVTFile() : the default user select pic2 file not exists , create it ! ");
            }
            try {
                Bitmap btp4 = BitmapFactory.decodeResource(PhoneApp.getInstance().getResources(),
                                                           R.drawable.vt_incall_pic_qcif);
                VTCallUtils.saveMyBitmap(VTAdvancedSetting.getPicPathUserselect2(), btp4);
                btp4.recycle();
                if (DBG) {
                    log(" - Bitmap.isRecycled() : " + btp4.isRecycled());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (DBG) {
            log("end checkVTFile() ! ");
        }
    }

    /**
     * Create, compress and change contribute of specified bitmap file
     * @param bitName          file name
     * @param bitmap           Bitmap object to save
     * @throws IOException     file operation exception
     */
    public static void saveMyBitmap(String bitName, Bitmap bitmap) throws IOException {
        if (DBG) {
            log("saveMyBitmap()...");
        }

        File file = new File(bitName);
        file.createNewFile();
        FileOutputStream fOut = null;

        try {
            fOut = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        bitmap.compress(Bitmap.CompressFormat.PNG, BITMAP_COMPRESS_QUALITY, fOut);
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (DBG) {
                log("Change file visit right for mediaserver process");
            }
            // Mediaserver process can only visit the file with group permission,
            // So we change here, or else, hide me function will not work
            String command = CHANGE_FILE_PERMISSION + file.getAbsolutePath();
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
            if (DBG) {
                log("exception happens when change file permission");
            }
        }

        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * VT needs special timing method for different number:
     * In MT call, the timing method is the same as voice call:
     * starting timing when the call state turn to "ACTIVE".
     * In MO call, because of the multimedia ringtone, the timing method is different 
     * which we starting timing when receive the Message - VTManager.VT_MSG_START_COUNTER.
     * Because when the multimedia ringtone is playing, the call state is already "ACTIVE",
     * but then we are connecting with the multimedia ringtone server but not the number we dialing.
     * So we must wait to start timing when connected with the number we dialing.
     * The Message VTManager.VT_MSG_START_COUNTER is to tell us that we have connected with the number we dialing.
     * But it is not to follow this method for all numbers in MO call.
     * Some numbers don't need timing - vtNumbers_none [].
     * Some numbers need timing with the voice call method - vtNumbers_default [].
     * You can UPDATE the numbers in them here.
     * 
     */

    private static String[] sNumbersNone = {"12531", "+8612531"};
    private static String[] sNumbersDefault = {"12535", "13800100011", "+8612535", "+8613800100011"};

    public static enum VTTimingMode {
        VT_TIMING_NONE, /*VT_TIMING_SPECIAL,*/ VT_TIMING_DEFAULT
    }

    /**
     * Check video call time mode according to phone number
     * @param number    phone number
     * @return          video call time mode
     */
    public static VTTimingMode checkVTTimingMode(String number) {
        if (DBG) {
            log("checkVTTimingMode - number:" + number);
        }

        ArrayList<String> arrayListNone = new ArrayList<String>(Arrays.asList(sNumbersNone));
        ArrayList<String> arrayListDefault = new ArrayList<String>(Arrays.asList(sNumbersDefault));

        if (arrayListNone.indexOf(number) >= 0) {
            if (DBG) {
                log("checkVTTimingMode - return:" + VTTimingMode.VT_TIMING_NONE);
            }
            return VTTimingMode.VT_TIMING_NONE;
        }

        if (arrayListDefault.indexOf(number) >= 0) {
            if (DBG) {
                log("checkVTTimingMode - return:"
                        + VTTimingMode.VT_TIMING_DEFAULT);
            }
            return VTTimingMode.VT_TIMING_DEFAULT;
        }

        return VTTimingMode.VT_TIMING_DEFAULT;
    }

    /**
     * Place video call
     * @param phone         Phone object
     * @param number        phone number
     * @param contactRef    contact reference
     * @param simId         sim id
     * @return              result of place video call
     */
    public static int placeVTCall(Phone phone, String number, Uri contactRef, int simId) {
        int status = Constants.CALL_STATUS_DIALED;
        try {
            if (DBG) {
                log("placeVTCall: '" + number + "'..." + "simId : " + simId);
            }

            if (Phone.State.IDLE != PhoneApp.getInstance().mCM.getState()) {
                return Constants.CALL_STATUS_FAILED;
            }
            if (PhoneNumberUtils.isIdleSsString(number)) {
                if (DBG) {
                    log("the number for VT call is idle ss string");
                }
                return Constants.CALL_STATUS_FAILED;
            }
            //In current stage, video call doesn't support uri number
            if (PhoneNumberUtils.isUriNumber(number) || phone instanceof SipPhone) {
                if (DBG) {
                    log("the number for VT call is idle uri string");
                }
                return Constants.CALL_STATUS_FAILED;
            }

            VTInCallScreenFlags.getInstance().reset();
            checkVTFile();
            VTSettingUtils.getInstance().updateVTSettingState();
            VTSettingUtils.getInstance().updateVTEngineerModeValues();

            if (DBG) {
                log("==> placeVTCall(): simId: " + simId);
            }
            int nCSNetType = 0; // so,nCSNetType: 1-GSM, 2-GPRS
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                if (0 == simId) {
                    nCSNetType = SystemProperties.getInt(
                            TelephonyProperties.PROPERTY_CS_NETWORK_TYPE, -1);
                } else if (1 == simId) {
                    nCSNetType = SystemProperties.getInt(
                            TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_2, -1);
                }
            } else {
                nCSNetType = SystemProperties.getInt(
                            TelephonyProperties.PROPERTY_CS_NETWORK_TYPE, -1);
            }
            if (DBG) {
                log("==> placeVTCall(): nCSNetType: " + nCSNetType);
            }
                        
            if ((1 == nCSNetType) || (2 == nCSNetType)) {
                status = Constants.CALL_STATUS_DROP_VOICECALL;
            }

            if (!VTInCallScreenFlags.getInstance().mVTInControlRes) {
                PhoneApp.getInstance().sendBroadcast(new Intent(VTCallUtils.VT_CALL_START));
                VTInCallScreenFlags.getInstance().mVTInControlRes = true;
            }
            VTInCallScreenFlags.getInstance().mVTIsMT = false;
            VTInCallScreenFlags.getInstance().mVTPeerBigger = VTSettingUtils.getInstance().mPeerBigger;
            if (VDBG) {
                log("- set VTManager open ! ");
            }
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                VTManager.getInstance().setVTOpen(PhoneApp.getInstance().getBaseContext(), 
                                                  PhoneApp.getInstance().mCMGemini);
            } else {
                VTManager.getInstance().setVTOpen(PhoneApp.getInstance().getBaseContext(), 
                                                  PhoneApp.getInstance().mCM);
            }
            if (VDBG) {
                log("- finish set VTManager open ! ");
            }

            if (VTInCallScreenFlags.getInstance().mVTSurfaceChangedH 
                    && VTInCallScreenFlags.getInstance().mVTSurfaceChangedL) {
                if (VDBG) {
                    log("- set VTManager ready ! ");
                }
                VTManager.getInstance().setVTReady(); 
                if (VDBG) {
                    log("- finish set VTManager ready ! ");
                }
            } else {
                VTInCallScreenFlags.getInstance().mVTSettingReady = true;
            }

            PhoneUtils.placeCallRegister(phone);
            Connection cn;
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                    cn = PhoneApp.getInstance().mCMGemini.vtDialGemini(phone, number, simId);
                } else {
                    cn = PhoneApp.getInstance().mCMGemini.vtDialGemini(phone, number, Phone.GEMINI_SIM_1);
                }
            } else {
                cn = PhoneApp.getInstance().mCM.vtDial(phone, number);
            }
            if (DBG) {
                log("vtDial() returned: " + cn);
            }
            if (cn == null) {
                if (phone.getPhoneType() == Phone.PHONE_TYPE_GSM) {
                    // On GSM phones, null is returned for MMI codes
                    if (DBG) {
                        log("dialed MMI code: " + number);
                    }
                    status = Constants.CALL_STATUS_DIALED_MMI;
                    PhoneUtils.setMMICommandToService(number);
                } else {
                    status = Constants.CALL_STATUS_FAILED;
                }
            } else {
                PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_OFFHOOK);

                // phone.dial() succeeded: we're now in a normal phone call.
                // attach the URI to the CallerInfo Object if it is there,
                // otherwise just attach the Uri Reference.
                // if the uri does not have a "content" scheme, then we treat
                // it as if it does NOT have a unique reference.
                String content = phone.getContext().getContentResolver().SCHEME_CONTENT;
                if ((contactRef != null) && (contactRef.getScheme().equals(content))) {
                    Object userDataObject = cn.getUserData();
                    if (userDataObject == null) {
                        cn.setUserData(contactRef);
                    } else {
                        // TODO: This branch is dead code, we have
                        // just created the connection 'cn' which has
                        // no user data (null) by default.
                        if (userDataObject instanceof CallerInfo) {
                            ((CallerInfo) userDataObject).contactRefUri = contactRef;
                        } else {
                            ((PhoneUtils.CallerInfoToken) userDataObject).currentInfo.contactRefUri =
                                contactRef;
                        }
                    }
                }
            }
        } catch (CallStateException ex) {
            Log.w(LOG_TAG, "Exception from vtDial()", ex);
            status = Constants.CALL_STATUS_FAILED;
        }

        return status;
    }

    /**
     * The function to judge whether the call is video call
     * @param call      Call object
     * @return true     yes
     *         false    no
     */
    public static boolean isVideoCall(Call call) {
        if (null == call) {
            return false;
        }
        if (null == call.getLatestConnection()) {
            return false;
        }
        return call.getLatestConnection().isVideo();
    }

}
