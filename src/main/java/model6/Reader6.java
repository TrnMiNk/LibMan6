package model6;

public class Reader6 {
    private long id;
    private String fullName;
    private String phone;
    private String email;
    private String classOrDepartment;

    public Reader6() {}

    public Reader6(String fullName, String phone, String email, String classOrDepartment) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.classOrDepartment = classOrDepartment;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getClassOrDepartment() { return classOrDepartment; }
    public void setClassOrDepartment(String value) { classOrDepartment = value; }
}
