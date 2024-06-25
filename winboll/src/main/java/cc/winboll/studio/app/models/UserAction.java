package cc.winboll.studio.app.models;

/**
 * @Author ZhanGSKen@QQ.COM
 * @Date 2024/06/25 13:42:56
 * @Describe UserAction
 */
public enum UserAction {

    ABOUT("about"),
    REPORT_ISSUE_FROM_TRANSCRIPT("report issue from transcript");

    private final String name;

    UserAction(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
