package dao6;

import model6.Reader6;

public class ReaderDAO6 extends DAO6 {
    public boolean checkRegistrationInfo(Reader6 reader) {
        if (reader == null) return false;
        reader.setFullName(clean(reader.getFullName()));
        reader.setPhone(clean(reader.getPhone()));
        reader.setEmail(clean(reader.getEmail()));
        reader.setClassOrDepartment(clean(reader.getClassOrDepartment()));
        String phone = reader.getPhone();
        String email = reader.getEmail();
        return required(reader.getFullName(), 120)
                && required(reader.getClassOrDepartment(), 120)
                && (phone.isEmpty() || (phone.length() <= 30
                    && phone.matches("\\+?[0-9() .-]+")
                    && phone.replaceAll("[^0-9]", "").length() >= 7
                    && phone.replaceAll("[^0-9]", "").length() <= 15))
                && (email.isEmpty() || (email.length() <= 150
                    && email.matches("[^\\s@<>]+@[^\\s@<>.]+(?:\\.[^\\s@<>.]+)+")));
    }

    private static String clean(String value) { return value == null ? "" : value.strip(); }

    private static boolean required(String value, int max) {
        return !value.isBlank() && value.length() <= max
                && value.codePoints().noneMatch(Character::isISOControl);
    }
}
