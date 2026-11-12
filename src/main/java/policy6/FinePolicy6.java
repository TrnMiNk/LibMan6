package policy6;

import model6.BorrowDetail6;
import model6.ReturnDetail6;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface FinePolicy6 {
    BigDecimal calculateFine(BorrowDetail6 borrowed, ReturnDetail6 returned, LocalDate returnDate);
}
