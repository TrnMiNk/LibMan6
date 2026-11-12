package model6;

/** Danh tính trong session; không chứa mật khẩu hoặc hash. */
public class Account6 implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String username;
    private final String accountType;
    private final String displayName;

    public Account6(long id, String username, String accountType, String displayName) {
        this.id = id; this.username = username; this.accountType = accountType;
        this.displayName = displayName;
    }
    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getAccountType() { return accountType; }
    public String getDisplayName() { return displayName; }
}
