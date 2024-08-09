package cc.winboll.studio.app;

/**
 * @Author ZhanGSKen@QQ.COM
 * @Date 2024/08/08 18:50:01
 * @Describe WinBoll Tasker Service
 */
import android.app.Service;
import android.content.Intent;
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE;
import android.os.Binder;
import android.os.IBinder;
import cc.winboll.studio.libapputils.LogUtils;

public class MyBindService extends Service implements ServiceInterface {

    public static final String TAG = "MyBindService";

    private final IBinder mBinder = new LocalBinder();
    
    Intent mIntent;
    
    public MyBindService() {
        LogUtils.d(TAG, "MyBindService");
    }

    @Override
    public void onCreate() {
        cc.winboll.studio.libapputils.LogUtils.init(this);
        LogUtils.d(TAG, "onCreate: ");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.d(TAG, "onBind: ");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        LogUtils.d(TAG, "onRebind: ");
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d(TAG, "onStartCommand: ");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.d(TAG, "onUnbind: ");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(TAG, "onDestroy: ");
    }

    //必须继承binder，才能作为中间人对象返回
    public class LocalBinder extends Binder {
        public MyBindService getService() {
            return MyBindService.this;
        }
    }

    @Override
    public void testServiceInterface() {
        LogUtils.d(TAG, "testServiceInterface: ");
        (new Thread(new Runnable(){
                @Override
                public void run() {
                    for (int i = 0; i < 0; i++) {
                        LogUtils.d(TAG, "testServiceInterface: " + Integer.toString(i));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            LogUtils.d(TAG, e, Thread.currentThread().getStackTrace());
                        }
                    }
                }
            })).start();
    }

    @Override
    public Intent runTermuxCommand(String szCmd) {
        LogUtils.d(TAG, "runTermuxCommand: ");
       
        // 普通调用
        String szTaskLogPath ="/storage/emulated/0/Android/data/com.termux.tasker/cache/LogUtils/log.txt";
        Intent mIntent = new Intent();
        mIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
        mIntent.setAction("com.termux.RUN_COMMAND");
        mIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        mIntent.putExtra("com.termux.RUN_COMMAND_RUNNER", "app-shell");
        
        // Tasker 前台台模式，显示真实命令 szCmd + " &>>" + szTaskLogPath。
        mIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", szCmd + " &>>" + szTaskLogPath});
        // Tasker 后台模式，隐藏真实命令显示 szCmd。
        //mIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", szCmd + " &>>" + szTaskLogPath + " &"});
        
        //mIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", szCmd + " | tee " + szTaskLogPath});
        //mIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", szCmd + " | tee " + szTaskLogPath + " &"});
        mIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        mIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", "false");
        mIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(mIntent);
       
        
        // 调用用 TermuxService
        /*String szTaskLogPath ="/storage/emulated/0/Android/data/com.termux.tasker/cache/LogUtils/log.txt";
        Intent mIntent = new Intent();
        
        //mIntent.setClassName(getPackageName(), "cc.winboll.studio.app.TermuxService");
        mIntent.setClassName(getApplicationContext(), "cc.winboll.studio.app.TermuxService");
        mIntent.setAction(TERMUX_SERVICE.ACTION_SERVICE_EXECUTE);
        mIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        mIntent.putExtra("com.termux.RUN_COMMAND_RUNNER", "app-shell");
        mIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", szCmd + " &>>" + szTaskLogPath});
        mIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        mIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", "false");
        mIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(mIntent);*/
        
        // 调用cc.winboll.studio.app.RunCommandService
        /*String szTaskLogPath ="/storage/emulated/0/Android/data/com.termux.tasker/cache/LogUtils/log.txt";
        Intent mIntent = new Intent();
        mIntent.setClassName(getPackageName(), "cc.winboll.studio.app.RunCommandService");
        mIntent.setAction("com.termux.RUN_COMMAND");
        mIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        mIntent.putExtra("com.termux.RUN_COMMAND_RUNNER", "app-shell");
        mIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", szCmd + " &>>" + szTaskLogPath});
        mIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        mIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", "false");
        mIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(mIntent);*/
        
        return mIntent;
    }
    
    @Override
    public boolean stopTermuxCommand(Intent runCommandIntent) {
        return stopService(runCommandIntent);
    }
}
