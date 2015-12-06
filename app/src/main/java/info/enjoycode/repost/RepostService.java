package info.enjoycode.repost;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by livin on 2015/12/6.
 */
public class RepostService extends AccessibilityService {
    private static final String TAG = "repost";
    String groupName = "胖";
    //    String groupName = "陈远明";
    boolean isFoward = false;
    boolean isDoubi = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = event.getClassName().toString();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:// 通知栏事件
                // 监听到已转的notification，打开通知
                Log.e(TAG, "onAccessibilityEvent: " + event.getText());
                if (event.getPackageName().equals("com.tencent.mm") && event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                    Notification notification = (Notification) event.getParcelableData();
                    PendingIntent pendingIntent = notification.contentIntent;
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    getPacket();//
                }
                break;
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private AccessibilityNodeInfo findInputEditTextNode() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        return findInputEditTextNode(rootNode);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private AccessibilityNodeInfo findInputEditTextNode(AccessibilityNodeInfo parentNode) {
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = parentNode.getChild(i);
            if (nodeInfo == null) {
                continue;
            }
            if (nodeInfo.getChildCount() > 0) {
                return findInputEditTextNode(nodeInfo);
            } else {
                if (nodeInfo.getClassName().equals(EditText.class.getName())) {
                    return nodeInfo;
                }
            }
        }
        return null;
    }


    @SuppressLint("NewApi")
    private void getPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        printNodeInfo(rootNode, "");
    }

    String currentGroupName;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void printNodeInfo(AccessibilityNodeInfo rootNode, String tab) {

        for (int i = 0; i < rootNode.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                continue;
            }
            if ("聊天信息".equals(nodeInfo.getContentDescription())) {
                if (rootNode.getChild(i - 1) != null && rootNode.getChild(i - 1).getText() != null) {
                    currentGroupName = rootNode.getChild(i - 1).getText().toString();
                    Log.e(TAG, "printNodeInfo: " + currentGroupName + "," + groupName);
                }
            }
            if (nodeInfo.getChildCount() > 0) {
                printNodeInfo(nodeInfo, tab + i + " ");
            } else {
                if (isFindInputEditText(tab, nodeInfo)) {
                    break;
                }
            }
        }
    }

    String latestMsg;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isFindInputEditText(CharSequence tab, final AccessibilityNodeInfo nodeInfo) {
        if (!TextUtils.isEmpty(nodeInfo.getText())) {
//            Log.e("------", tab + nodeInfo.getText().toString() + "\n" + nodeInfo.toString());
            latestMsg = nodeInfo.getText().toString();
        } else {
            if (nodeInfo.getClassName().equals(EditText.class.getName())) {
                if (!TextUtils.isEmpty(currentGroupName) && currentGroupName.contains(groupName)) {
                    Log.e(TAG, "loge: " + latestMsg);
                    if (latestMsg.contains("help") || latestMsg.contains("开启转发模式") || latestMsg.contains("关闭转发模式") || latestMsg.contains("开启逗比模式") || latestMsg.contains("关闭逗比模式")) {
                        if (latestMsg.contains("help")) {
                            sendText(nodeInfo, "tips 回复\n开启转发模式\n关闭转发模式\n开启逗比模式\n关闭逗比模式");
                        }
                        if (latestMsg.contains("开启转发模式")) {
                            setForward(nodeInfo, true);
                        }
                        if (latestMsg.contains("关闭转发模式")) {
                            setForward(nodeInfo, false);
                        }
                        if (latestMsg.contains("开启逗比模式")) {
                            setDoubi(nodeInfo, true);
                        }
                        if (latestMsg.contains("关闭逗比模式")) {
                            setDoubi(nodeInfo, false);
                        }
                    } else {
                        if (isDoubi) {
                            sendText(nodeInfo, latestMsg);
                        } else if (isFoward) {
                            sendText(nodeInfo, "已转");
                        } else {
                            Intent intent = new Intent(RepostService.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                }

                return true;
            }
//            Log.e("------", tab + nodeInfo.toString());
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void sendText(final AccessibilityNodeInfo nodeInfo, final String text) {
        if (nodeInfo == null) {
            return;
        }
        Log.e(TAG, "sendText: " + text);
        handler.post(new Runnable() {
            @Override
            public void run() {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData oldClip = clipboardManager.getPrimaryClip();
                ClipData clipData = ClipData.newPlainText("pasteLabel", text);
                clipboardManager.setPrimaryClip(clipData);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
        });
//                clipboardManager.setPrimaryClip(oldClip);


        handler.postDelayed(new Runnable() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void run() {
                AccessibilityNodeInfo parentNode = nodeInfo.getParent();
                parentNode.refresh();
                AccessibilityNodeInfo sendNode = parentNode.getChild(parentNode.getChildCount() - 1);
                while (!"发送".equals(sendNode.getText())) {
                    handler.postDelayed(this, 10);
                }
                sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);


                Intent intent = new Intent(RepostService.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        }, 10);
    }

    @Override
    public void onInterrupt() {
    }

    Handler handler = new Handler();

    public void setForward(AccessibilityNodeInfo nodeInfo, boolean forward) {
        isFoward = forward;

        sendText(nodeInfo, String.format("已%s%s模式", (forward ? "开启" : "关闭"), "转发"));
//        if (isFoward) {
//            setDoubi(nodeInfo, false);
//        }
    }

    public void setDoubi(AccessibilityNodeInfo nodeInfo, boolean doubi) {
        isDoubi = doubi;
        sendText(nodeInfo, String.format("已%s%s模式", (doubi ? "开启" : "关闭"), "逗比"));
//        if (isDoubi) {
//            setForward(nodeInfo, false);
//        }
    }

    public static Object getObjectProperty(Object object, String propertyName) {
        try {
            Field f = object.getClass().getDeclaredField(propertyName);
            f.setAccessible(true);
            return f.get(object);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
