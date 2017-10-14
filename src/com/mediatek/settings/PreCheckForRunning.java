package com.mediatek.settings;

import com.mediatek.CellConnService.CellConnMgr;
import android.content.Context;
import android.content.Intent;
import com.mediatek.xlog.Xlog;

public class PreCheckForRunning {
    private CellConnMgr mCellConnMgr;
    private ServiceComplete mServiceComplete;
    private Context context;
    private Intent intent;
    private static final String TAG = "Settings/PreCheckForRunning";
    public boolean byPass = false;
    
    public PreCheckForRunning(Context ctx){
        context = ctx;
        mServiceComplete = new ServiceComplete();
        mCellConnMgr = new CellConnMgr(mServiceComplete);
        mCellConnMgr.register(context.getApplicationContext());
    }
    class ServiceComplete implements Runnable {
        public void run() {
            int result = mCellConnMgr.getResult();
            Xlog.d(TAG, "ServiceComplete with the result = " + CellConnMgr.resultToString(result));
            if (CellConnMgr.RESULT_OK == result || CellConnMgr.RESULT_STATE_NORMAL == result) {
                context.startActivity(intent);
            }
        }
    }
    public void checkToRun(Intent intent, int slotId, int req)
    {
        if (byPass) {
            context.startActivity(intent);
            return ;
        }
        setIntent(intent);
        int r = mCellConnMgr.handleCellConn(slotId, req);
        Xlog.d(TAG, "The result of handleCellConn = " + CellConnMgr.resultToString(r));
    }
    
    public void setIntent(Intent it){
        intent = it;
    }
    
    public void deRegister(){
        mCellConnMgr.unregister();
    }
}
