package model6;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public class ReturnSlip6 {
    private long id;
    private LocalDate returnDate;
    private boolean fineSettled;
    private LibraryCard6 libraryCard;
    private LibraryStaff6 staff;
    private List<ReturnDetail6> details = List.of();
    private String requestKey;

    public ReturnSlip6() {}

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate value) { returnDate = value; }
    public boolean isFineSettled() { return fineSettled; }
    public void setFineSettled(boolean value) { fineSettled = value; }
    public LibraryCard6 getLibraryCard() { return libraryCard; }
    public void setLibraryCard(LibraryCard6 value) { libraryCard = value; }
    public LibraryStaff6 getStaff() { return staff; }
    public void setStaff(LibraryStaff6 value) { staff = value; }
    public List<ReturnDetail6> getDetails() { return details; }
    public void setDetails(List<ReturnDetail6> value) { details = value; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { requestKey = value; }
    public BigDecimal getTotalFine() { return details.stream().map(ReturnDetail6::getFineAmount).reduce(new BigDecimal("0.00"), BigDecimal::add); }
}
