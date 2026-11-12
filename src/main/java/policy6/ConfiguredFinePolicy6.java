package policy6;

import model6.BorrowDetail6;
import model6.ReturnDetail6;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

/** Công thức demo đề xuất; đơn giá phải được cấu hình tường minh. */
public class ConfiguredFinePolicy6 implements FinePolicy6 {
    private final Properties properties = new Properties();
    public static final BigDecimal MAX = new BigDecimal("9999999999999.99");

    public ConfiguredFinePolicy6() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("fine-policy.properties")) {
            if (input != null) properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Không đọc được cấu hình đơn giá phạt.", exception);
        }
    }

    public ConfiguredFinePolicy6(Properties values) { properties.putAll(values); }

    private BigDecimal rate(String key) {
        String value = properties.getProperty(key, "").strip();
        if (value.isEmpty()) throw new IllegalArgumentException("Chưa cấu hình đơn giá " + key + " trong fine-policy.properties cho khoản phạt phát sinh.");
        try { return checkMoney(new BigDecimal(value)); }
        catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("Đơn giá " + key + " phải là số không âm, tối đa 2 chữ số thập phân và trong DECIMAL(15,2).");
        }
    }

    public static BigDecimal checkMoney(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX) > 0) {
            throw new IllegalArgumentException("Tiền phạt phải không âm và nằm trong giới hạn DECIMAL(15,2).");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    @Override
    public BigDecimal calculateFine(BorrowDetail6 borrowed, ReturnDetail6 returned, LocalDate returnDate) {
        long days = Math.max(0, ChronoUnit.DAYS.between(borrowed.getDueDate(), returnDate));
        BigDecimal fine = new BigDecimal("0.00");
        if (days > 0) fine = fine.add(rate("dailyRate").multiply(BigDecimal.valueOf(days)).multiply(BigDecimal.valueOf(returned.getReturnedQuantity())));
        if (returned.getDamagedQuantity() > 0) fine = fine.add(rate("damageRate").multiply(BigDecimal.valueOf(returned.getDamagedQuantity())));
        return checkMoney(fine);
    }
}
