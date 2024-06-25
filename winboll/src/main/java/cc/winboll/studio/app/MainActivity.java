package cc.winboll.studio.app;

/**
 * @Author ZhanGSKen@QQ.COM
 * @Date 2024/06/22 15:26:03
 * @Describe 应用主窗口
 */
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.termux.terminal.TerminalSessionClient;
import com.termux.terminal.TerminalSession;
import com.termux.shared.activity.ActivityUtils;

public class MainActivity extends Activity {

    public static final String TAG = "MainActivity";

    Intent mIntent;

    public volatile boolean mIsHandling;
    public volatile boolean mIsAddNewLog;

    LinearLayout mllLogView;
    ScrollView mScrollView;
    TextView mTextView;
    CheckBox mSelectableCheckBox;
    LogViewThread mLogViewThread;
    LogViewHandler mLogViewHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogUtils.init(this);
        initView();


        Button btnRun = findViewById(R.id.activitymainButton1);
        btnRun.setOnClickListener(new Button.OnClickListener(){

                @Override
                public void onClick(View view) {
                    EditText et = findViewById(R.id.activitymainEditText1);

                    run(et.getText().toString());
                }
            });
        Button btnStop = findViewById(R.id.activitymainButton2);
        btnStop.setOnClickListener(new Button.OnClickListener(){

                @Override
                public void onClick(View view) {
                    stop();
                }
            });
        mLogViewThread = new LogViewThread(this);
        mLogViewThread.start();
        updateLogView();
        Toast.makeText(getApplication(), "onCreate end.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(R.id.item_termux == item.getItemId()) {
            openTermux();
        }
        return super.onOptionsItemSelected(item);
    }
    
    void openTermux() {
        Toast.makeText(getApplication(), "openTermux", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, TermuxActivity.class);
        ActivityUtils.startActivity(this, intent);
        
    }

    void initView() {
        mLogViewHandler = new LogViewHandler();
        // 加载视图布局
        //addView(inflate(mContext, R.layout.view_log, null));
        mllLogView = findViewById(R.id.activitymainLinearLayout1);
        // 初始化日志子控件视图
        //
        mScrollView = findViewById(R.id.viewlogScrollViewLog);
        mTextView = findViewById(R.id.viewlogTextViewLog);

        (findViewById(R.id.viewlogButtonClean)).setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    LogUtils.cleanLog();
                    LogUtils.d(TAG, "Log is cleaned.");
                }
            });
        (findViewById(R.id.viewlogButtonCopy)).setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {

                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText(getPackageName(), LogUtils.loadLog()));
                    LogUtils.d(TAG, "Log is copied.");
                }
            });
        mSelectableCheckBox = findViewById(R.id.viewlogCheckBoxSelectable);
        mSelectableCheckBox.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    if (mSelectableCheckBox.isChecked()) {
                        mllLogView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
                    } else {
                        mllLogView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                    }
                }
            });
        // 设置滚动时不聚焦日志
        mllLogView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    public void updateLogView() {
        if (mLogViewHandler.isHandling() == true) {
            // 正在处理日志显示，
            // 就先设置一个新日志标志位
            // 以便日志显示完后，再次显示新日志内容
            mLogViewHandler.setIsAddNewLog(true);
        } else {
            //LogUtils.d(TAG, "LogListener showLog(String path)");
            Message message = mLogViewHandler.obtainMessage(LogViewHandler.MSG_LOGVIEW_UPDATE);
            mLogViewHandler.sendMessage(message);
            mLogViewHandler.setIsAddNewLog(false);
        }
    }

    public void run(String szCmd) {
        Toast.makeText(getApplication(), "run", Toast.LENGTH_SHORT).show();
        mIntent = new Intent();
        mIntent.setClassName("com.termux", "com.termux.app.RunCommandService");
        mIntent.setAction("com.termux.RUN_COMMAND");
        mIntent.putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash");
        mIntent.putExtra("com.termux.RUN_COMMAND_RUNNER", "app-shell");
        mIntent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", szCmd + " &>>" + LogUtils._mszLogPath});
        mIntent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home");
        mIntent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", "false");
        mIntent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        startService(mIntent);
    }

    public void stop() {
        Toast.makeText(getApplication(), "stop", Toast.LENGTH_SHORT).show();
        stopService(mIntent);
    }

    class LogViewHandler extends Handler {

        final static int MSG_LOGVIEW_UPDATE = 0;
        volatile boolean isHandling;
        volatile boolean isAddNewLog;

        public LogViewHandler() {
            setIsHandling(false);
            setIsAddNewLog(false);
        }

        public void setIsHandling(boolean isHandling) {
            this.isHandling = isHandling;
        }

        public boolean isHandling() {
            return isHandling;
        }

        public void setIsAddNewLog(boolean isAddNewLog) {
            this.isAddNewLog = isAddNewLog;
        }

        public boolean isAddNewLog() {
            return isAddNewLog;
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOGVIEW_UPDATE:{
                        if (isHandling() == false) {
                            setIsHandling(true);
                            showAndScrollLogView();
                        }
                        break;
                    }
                default:
                    break;
            }
            super.handleMessage(msg);
        }

        void showAndScrollLogView() {
            mTextView.setText(LogUtils.loadLog());
            mScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        // 日志显示结束
                        setIsHandling(false);
                        // 检查是否添加了新日志
                        if (isAddNewLog()) {
                            // 有新日志添加，先更改新日志标志
                            setIsAddNewLog(false);
                            // 再次发送显示日志的显示
                            Message message = obtainMessage(MSG_LOGVIEW_UPDATE);
                            sendMessage(message);
                        }
                    }
                });

        }
    }



    public class LogViewThread extends Thread {


        public static final String TAG = "LogViewThread";

        // 线程退出标志
        volatile boolean isExist = false;
        // 应用日志文件监听实例
        LogListener mLogListener;
        // 日志视图弱引用
        WeakReference<MainActivity> rmainActivity;

        //
        // 构造函数
        // @logView : 日志显示输出视图类
        public LogViewThread(MainActivity mainActivity) {
            rmainActivity = new WeakReference<MainActivity>(mainActivity);

        }

        public void setIsExist(boolean isExist) {
            this.isExist = isExist;
        }

        public boolean isExist() {
            return isExist;
        }

        @Override
        public void run() {
            String szLogDir = LogUtils.getLogDir().getPath();
            mLogListener = new LogListener(szLogDir);
            mLogListener.startWatching();
            while (isExist() == false) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }
            mLogListener.stopWatching();
        }


        //
        // 日志文件监听类
        //
        class LogListener extends FileObserver {
            public LogListener(String path) {
                super(path);
            }

            @Override
            public void onEvent(int event, String path) {
                int e = event & FileObserver.ALL_EVENTS;
                switch (e) {
                    case FileObserver.CLOSE_WRITE:{
                            if (rmainActivity.get() != null) {
                                rmainActivity.get().updateLogView();
                            }
                            break;
                        }
                    case FileObserver.DELETE:{
                            if (rmainActivity.get() != null) {
                                rmainActivity.get().updateLogView();
                            }
                            break;
                        }
                }
            }
        }
    }

    public static class LogUtils {

        public static final String TAG = "LogUtils";

        // 日志显示时间格式
        static SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("[yyyyMMdd_HHmmSS]", Locale.getDefault());
        // 应用日志文件夹
        static File _mfLogDir;
        // 应用日志文件
        static File _mfLogFile;
        // 是否在调试状态
        static boolean _mIsDebug;
        public static String _mszLogDir = "/sdcard/Tasker/";
        public static String _mszLogPath = "/sdcard/Tasker/tasker.log";


        public static void setIsDebug(boolean isDebug) {
            _mIsDebug = isDebug;
        }

        public static boolean isDebug() {
            return _mIsDebug;
        }

        //
        // 初始化函数
        //
        public static void init(Context context) {
            setIsDebug(true);
            // 初始化日志读写文件路径
            //_mfLogDir = new File(context.getExternalCacheDir(), TAG);
            _mfLogDir = new File(_mszLogDir);
            if (!_mfLogDir.exists()) {
                _mfLogDir.mkdirs();
            }
            //_mfLogFile = new File(_mfLogDir, "log.txt");
            _mfLogFile = new File(_mszLogPath);
        }

        //
        // 获取应用日志文件夹
        //
        public static File getLogDir() {
            return _mfLogDir;
        }

        //
        // 调试日志写入函数
        //
        public static void d(String szTAG, String szMessage) {
            if (isDebug()) {
                saveLogDebug(szTAG, szMessage);
            }
        }

        //
        // 调试日志写入函数
        // 包含线程调试堆栈信息
        //
        public static void d(String szTAG, String szMessage, StackTraceElement[] listStackTrace) {
            if (isDebug()) {
                StringBuilder sbMessage = new StringBuilder(szMessage);
                sbMessage.append(" \nAt ");
                sbMessage.append(listStackTrace[2].getMethodName());
                sbMessage.append(" (");
                sbMessage.append(listStackTrace[2].getFileName());
                sbMessage.append(":");
                sbMessage.append(listStackTrace[2].getLineNumber());
                sbMessage.append(")");
                saveLogDebug(szTAG, sbMessage.toString());
            }
        }

        //
        // 调试日志写入函数
        // 包含异常信息和线程调试堆栈信息
        //
        public static void d(String szTAG, Exception e, StackTraceElement[] listStackTrace) {
            if (isDebug()) {
                StringBuilder sbMessage = new StringBuilder(e.getClass().toGenericString());
                sbMessage.append(" : ");
                sbMessage.append(e.getMessage());
                sbMessage.append(" \nAt ");
                sbMessage.append(listStackTrace[2].getMethodName());
                sbMessage.append(" (");
                sbMessage.append(listStackTrace[2].getFileName());
                sbMessage.append(":");
                sbMessage.append(listStackTrace[2].getLineNumber());
                sbMessage.append(")");
                saveLogDebug(szTAG, sbMessage.toString());
            }
        }

        //
        // 应用信息日志写入函数
        //
        public static void i(String szTAG, String szMessage) {
            saveLogInfo(szMessage);
        }

        //
        // 日志文件保存函数
        //
        static void saveLogInfo(String szMessage) {
            try {

                BufferedWriter out = null;
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_mfLogFile, true), "UTF-8"));
                out.write(mSimpleDateFormat.format(System.currentTimeMillis()) + ": " + szMessage + "\n");
                out.close();

            } catch (IOException e) {
                LogUtils.d(TAG, "IOException : " + e.getMessage());
            }
        }

        //
        // 日志文件保存函数
        //
        static void saveLogDebug(String szTAG, String szMessage) {
            try {
                BufferedWriter out = null;
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_mfLogFile, true), "UTF-8"));
                out.write(mSimpleDateFormat.format(System.currentTimeMillis()) + "[" + szTAG + "]: " + szMessage + "\n");
                out.close();
            } catch (IOException e) {
                LogUtils.d(TAG, "IOException : " + e.getMessage());
            }
        }

        //
        // 历史日志加载函数
        //
        public static String loadLog() {
            if (_mfLogFile.exists()) {
                StringBuffer sb = new StringBuffer();
                try {
                    BufferedReader in = null;
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(_mfLogFile), "UTF-8"));
                    String line = "";
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                } catch (IOException e) {
                    LogUtils.d(TAG, "IOException : " + e.getMessage());
                } 
                return sb.toString();
            }
            return "";
        }

        //
        // 清理日志函数
        //
        public static void cleanLog() {
            if (_mfLogFile.exists()) {
                _mfLogFile.delete();
            }
        }
    }
}
