package model6;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public class BorrowRecord6 {
    private long id;
    private LocalDate borrowDate;
    private LibraryCard6 libraryCard;

    public BorrowRecord6() {}

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate value) { borrowDate = value; }
    public LibraryCard6 getLibraryCard() { return libraryCard; }
    public void setLibraryCard(LibraryCard6 value) { libraryCard = value; }
}
