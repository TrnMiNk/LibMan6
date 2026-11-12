package model6;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public class ReturnDetail6 {
    private long id;
    private int returnedQuantity;
    private int damagedQuantity;
    private String conditionNote;
    private BigDecimal fineAmount;
    private BorrowDetail6 borrowDetail;

    public ReturnDetail6() {}

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public int getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(int value) { returnedQuantity = value; }
    public int getDamagedQuantity() { return damagedQuantity; }
    public void setDamagedQuantity(int value) { damagedQuantity = value; }
    public String getConditionNote() { return conditionNote; }
    public void setConditionNote(String value) { conditionNote = value; }
    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal value) { fineAmount = value; }
    public BorrowDetail6 getBorrowDetail() { return borrowDetail; }
    public void setBorrowDetail(BorrowDetail6 value) { borrowDetail = value; }
}
