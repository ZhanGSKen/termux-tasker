package cc.winboll.studio.app;

/**
 * @Author ZhanGSKen@QQ.COM
 * @Date 2024/08/08 18:21:53
 * @Describe WinBoll Tasker 日志窗口
 */
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cc.winboll.studio.app.R;
import cc.winboll.studio.libapputils.LogUtils;
import cc.winboll.studio.libapputils.views.LogView;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WinBollTaskerLogActivity extends Activity {

    public static final String TAG = "WinBollTaskerLogActivity";

    static final int MSG_RUNCOMMANDSERVICE_EXIT = 0;
    static final int MSG_UPDATE_TIME = 1;

    LogView mLogView;
    MyBindService myBindService;
    EditText mEditText;
    Button mbtnRunCommand;
    Button mbtnStopCommand;
    Intent mIntent;
    static WinBollTaskerLogActivity mWinBollTaskerLogActivity;
    ViewTreeObserver mViewTreeObserver;
    LinearLayout mLinearLayoutInput;
    float mLogViewHeight = 0;
    TextView mtvTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWinBollTaskerLogActivity = this;
        setContentView(R.layout.activity_winbolltaskerlog);
        cc.winboll.studio.libapputils.LogUtils.init(this);
        mLogView = findViewById(R.id.logview);
        mLogView.start();
        LogUtils.d(TAG, "WinBollTaskerLogActivity Start.");
        bindService();

        mEditText = findViewById(R.id.activitywinbolltaskerlogEditText1);
        focusToEditText();
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            参数说明
//            @param v 被监听的对象
//            @param actionId  动作标识符,如果值等于EditorInfo.IME_NULL，则回车键被按下。
//            @param event    如果由输入键触发，这是事件；否则，这是空的(比如非输入键触发是空的)。
//            @return 返回你的动作
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEND) {
                        handleEnterPress();
                        return true;
                    }
                    return false;
                }
            });

        mbtnRunCommand = findViewById(R.id.activitywinbolltaskerlogButton1);
        mbtnRunCommand.setOnClickListener(new Button.OnClickListener(){

                @Override
                public void onClick(View view) {
                    handleEnterPress();
                }
            });

        mbtnStopCommand = findViewById(R.id.activitywinbolltaskerlogButton2);
        mbtnStopCommand.setOnClickListener(new Button.OnClickListener(){

                @Override
                public void onClick(View view) {
                    if (myBindService.stopTermuxCommand(mIntent)) {
                        setRunCommandStatus(false);
                        LogUtils.d(TAG, "Stop Termux Command");
                    } else {
                        Toast.makeText(getApplication(), "Stop Termux Command Failed.", Toast.LENGTH_SHORT).show();
                    }
                    //focusToEditText();
                }
            });

        setRunCommandStatus(false);



        (new Thread(new Runnable(){
                @Override
                public void run() {
                    while (true) {
                        if (mLogViewHeight != mLogView.getHeight()) {
                            mLogViewHeight = mLogView.getHeight();
                            mLogView.scrollLogUp();
                            //LogUtils.d(TAG, "LogView Height Change.");
                        }
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            LogUtils.d(TAG, e, Thread.currentThread().getStackTrace());
                        }
                    }
                }
            })).start();

        // 创建一个TextView用于显示时间
        mtvTime = findViewById(R.id.activitywinbolltaskerlogTextView1);
        // 设置定时器，每秒更新一次时间

        // 启动定时任务
        (new Thread(new Runnable(){
                @Override
                public void run() {
                    while (true) {
                        Message msg = mHandler.obtainMessage(MSG_UPDATE_TIME);
                        mHandler.sendMessage(msg);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            LogUtils.d(TAG, e, Thread.currentThread().getStackTrace());
                        }
                    }
                }
            })).start();
        
    }
    
    //
    // 定义一个更新时间的方法
    //
    private void updateTime() {
        // 获取当前时间戳（毫秒）
        long currentTime = System.currentTimeMillis();

        // 格式化时间
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        String formattedTime = sdf.format(new Date(currentTime));

        // 更新TextView的内容
        mtvTime.setText(formattedTime);
    }

    private void handleEnterPress() {
        // 在这里编写你的回车事件处理逻辑

        mIntent = myBindService.runTermuxCommand(mEditText.getText().toString());
        setRunCommandStatus(true);
        //focusToEditText();
        mEditText.setText("");
    }

    void setRunCommandStatus(boolean isRunning) {
        //mEditText.setEnabled(!isRunning);
        mbtnRunCommand.setEnabled(!isRunning);
        mbtnStopCommand.setEnabled(isRunning);
    }



    void focusToEditText() {
        mEditText.postDelayed(new Runnable(){
                @Override
                public void run() {
                    mEditText.requestFocus();
                }
            }, 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unBindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_winbolltaskerlog, menu);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.item_starttermux) {
            Intent intent = new Intent(this, TermuxActivity.class);
            startActivity(intent);
            //startActivityForResult(intent, REQUEST_LOGACTIVITY);

        }
        return super.onMenuItemSelected(featureId, item);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MyBindService.LocalBinder binder = (MyBindService.LocalBinder) service;
            myBindService = binder.getService();
            //调用服务中的方法
            myBindService.testServiceInterface();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    //绑定服务
    private void bindService() {
        Intent intent = new Intent(this, MyBindService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    //解绑服务
    private void unBindService() {
        unbindService(mConnection);
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RUNCOMMANDSERVICE_EXIT : {
                        setRunCommandStatus(false);
                        break;
                    }
                case MSG_UPDATE_TIME : {
                        updateTime();
                        break;
                    }
                default : {
                        super.handleMessage(msg);
                    }
            }
        }

    };

    public static void notifyRunCommandServiceExit() {
        if (mWinBollTaskerLogActivity != null) {
            Message msg = mWinBollTaskerLogActivity.mHandler.obtainMessage(MSG_RUNCOMMANDSERVICE_EXIT);
            mWinBollTaskerLogActivity.mHandler.sendMessage(msg);
        }
    }
}
