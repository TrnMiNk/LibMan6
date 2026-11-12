package model6;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public class BorrowDetail6 {
    private long id;
    private int borrowQuantity;
    private int returnedQuantity;
    private LocalDate dueDate;
    private BorrowRecord6 borrowRecord;
    private Document6 document;

    public BorrowDetail6() {}

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public int getBorrowQuantity() { return borrowQuantity; }
    public void setBorrowQuantity(int value) { borrowQuantity = value; }
    public int getReturnedQuantity() { return returnedQuantity; }
    public void setReturnedQuantity(int value) { returnedQuantity = value; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate value) { dueDate = value; }
    public BorrowRecord6 getBorrowRecord() { return borrowRecord; }
    public void setBorrowRecord(BorrowRecord6 value) { borrowRecord = value; }
    public Document6 getDocument() { return document; }
    public void setDocument(Document6 value) { document = value; }
    public int getRemainingQuantity() { return borrowQuantity - returnedQuantity; }
}
