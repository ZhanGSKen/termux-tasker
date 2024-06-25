package cc.winboll.studio.app.terminal.io;

/**
 * @Author ZhanGSKen@QQ.COM
 * @Date 2024/06/25 13:43:46
 * @Describe KeyboardShortcut
 */
public class KeyboardShortcut {

    public final int codePoint;
    public final int shortcutAction;

    public KeyboardShortcut(int codePoint, int shortcutAction) {
        this.codePoint = codePoint;
        this.shortcutAction = shortcutAction;
    }

}
