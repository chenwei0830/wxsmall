package com.mediatek.settings;
import com.android.phone.R;

/**
 * Assisted class for transfer contact data
 * @author MTK80906
 */
public class CallRejectListItem {
    private String id;
    private String name;
    private String phoneNum;
    private boolean isChecked;
    
    public CallRejectListItem() {
        name = "";
        phoneNum = "";
        id = "";
        isChecked = false;
    }
    
    public CallRejectListItem(String name, String phoneNum, String id, boolean isChecked) {
        this.name = name;
        this.phoneNum = phoneNum;
        this.id = id;
        this.isChecked = isChecked;
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public boolean getIsChecked() {
        return isChecked;
    }

    public void setIsChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }
}
