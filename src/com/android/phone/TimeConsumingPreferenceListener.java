package com.android.phone;
import android.preference.Preference;
import com.android.internal.telephony.CommandException;

public interface  TimeConsumingPreferenceListener {
    public void onStarted(Preference preference, boolean reading);
    public void onUpdate(TimeConsumingPreferenceListener tcp, boolean flag);
    public void onFinished(Preference preference, boolean reading);
    public void onError(Preference preference, int error);
    public void onException(Preference preference, CommandException exception);
}
