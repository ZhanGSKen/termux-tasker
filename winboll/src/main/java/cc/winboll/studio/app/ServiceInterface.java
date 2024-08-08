package cc.winboll.studio.app;
import android.content.Intent;

/**
 * @Author ZhanGSKen@QQ.COM
 * @Date 2024/08/08 19:28:17
 * @Describe 用途:绑定服务中需要实现的接口
 */
public interface ServiceInterface {

    public static final String TAG = "ServiceInterface";

    void testServiceInterface();
    Intent runTermuxCommand(String szCommand);
    boolean stopTermuxCommand(Intent runCommandIntent);

}
