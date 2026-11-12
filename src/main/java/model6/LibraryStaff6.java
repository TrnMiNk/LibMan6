package model6;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public class LibraryStaff6 {
    private long id;
    private String fullName;
    private String role;
    private boolean active;

    public LibraryStaff6() {}

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }
    public String getRole() { return role; }
    public void setRole(String value) { role = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
}
