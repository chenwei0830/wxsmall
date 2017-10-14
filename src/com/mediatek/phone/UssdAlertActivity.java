package com.mediatek.phone;

import java.io.IOException;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.view.View;

import android.widget.EditText;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.phone.PhoneApp;
import com.android.phone.PhoneUtils;
import com.android.phone.R;

import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;

public class UssdAlertActivity extends AlertActivity implements 
        DialogInterface.OnClickListener {

    private static final String LOG_TAG = "UssdAlertActivity";
    public static final int USSD_DIALOG_REQUEST = 1;
    public static final int USSD_DIALOG_NOTIFICATION = 2;
    public static final String USSD_MESSAGE_EXTRA = "ussd_message";
    public static final String USSD_TYPE_EXTRA = "ussd_type";
    public static final String USSD_SLOT_ID = "slot_id";

    private TextView mMsg;
    private EditText mInputText;
    private String mText;
    private int mType = USSD_DIALOG_REQUEST;
    private int mSlotId;
    private Phone mphone;
    private static final String TAG = "UssdAlertActivity";
    private MediaPlayer mMediaPlayer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the "dialog"
        final AlertController.AlertParams p = mAlertParams;
        
        mphone = PhoneApp.getInstance().phone;
        Intent intent = getIntent();
        mText = intent.getStringExtra(USSD_MESSAGE_EXTRA);
        mType = intent.getIntExtra(USSD_TYPE_EXTRA, USSD_DIALOG_REQUEST);
        mSlotId = intent.getIntExtra(USSD_SLOT_ID, 0);
        //p.mIconId = android.R.drawable.ic_dialog_alert;
        //p.mTitle = getString(R.string.bt_enable_title);
        //p.mTitle = "USSD";
        p.mView = createView();
        if (mType == USSD_DIALOG_REQUEST) {
            p.mPositiveButtonText = getString(R.string.send_button);
            p.mNegativeButtonText = getString(R.string.cancel);
        } else {
            p.mPositiveButtonText = getString(R.string.ok);
        }

        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;

        playUSSDTone(PhoneApp.getInstance().getApplicationContext());
        PhoneUtils.mUssdActivity = this;
        setupAlert();
    }

    protected void onResume() {
        super.onResume();
        if (mType == USSD_DIALOG_REQUEST) {
            String text = mInputText.getText().toString();
            if (text != null && text.length() > 0) {
                mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            } else {
                mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
            mInputText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start,
                    int count, int after) {
            }
              
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            
            public void afterTextChanged(Editable s) {
                int count = s == null ? 0 : s.length();
                if (count > 0) {
                    mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    mAlert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                }
            }
          });
        }
    }
    
    private View createView() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ussd_response, null);
        mMsg = (TextView)dialogView.findViewById(R.id.msg);
        mInputText = (EditText) dialogView.findViewById(R.id.input_field);

        if (mMsg != null) {
            mMsg.setText(mText);
        }
        
        if (mType == USSD_DIALOG_NOTIFICATION) {
            mInputText.setVisibility(View.GONE);
        }
            
        return dialogView;
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mType == USSD_DIALOG_REQUEST) {
                    sendUssd();
                }
                finish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                PhoneUtils.cancelUssdDialog();
                finish();
                break;
            default:
                break;
        }
    }

    private void sendUssd() {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Log.w(LOG_TAG, "sendUssd sendUssdResponseGemini button, simId:" + mSlotId); 
            Log.w(LOG_TAG, "sendUssd USSR string :" + mInputText.getText().toString());
            ((GeminiPhone)mphone).sendUssdResponseGemini(mInputText.getText().toString(), mSlotId);
        } else {
            Log.w(LOG_TAG, "sendUssd sendUssdResponseGemini button"); 
            Log.w(LOG_TAG, "sendUssd USSR string :" + mInputText.getText().toString());
            mphone.sendUssdResponse(mInputText.getText().toString());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(TAG, "onNewIntent");
        //force to finish ourself and then start new one
        finish();
        playUSSDTone(PhoneApp.getInstance().getApplicationContext());
        startActivity(intent);
    }

    public void playUSSDTone(final Context context) {
        new Thread(new Runnable() {
            public void run() {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.reset();
                try {
                    mMediaPlayer.setDataSource(context,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    mMediaPlayer.prepare();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mMediaPlayer.start();
                setMediaListener(mMediaPlayer);
            }
        }).start();
    }

    public void setMediaListener(MediaPlayer mediaPlayer) {
       mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
           public void onCompletion(MediaPlayer mp) {
               mMediaPlayer.release();
               mMediaPlayer = null;
           }
       });
    }

    /**
     * @return the mInputText widget
     */
    public EditText getmInputTextOnlyForTest() {
        return mInputText;
    }

    /**
     * @return the mAlert widget
     */
    public AlertController getmAlertOnlyForTest() {
        return super.mAlert;
    }

    /**
     * @return the mType widget
     */
    public int getmTypeOnlyForTest() {
        return mType;
    }
}
