package model6;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public class Document6 {
    private long id;
    private String title;
    private String category;
    private String author;

    public Document6() {}

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getCategory() { return category; }
    public void setCategory(String value) { category = value; }
    public String getAuthor() { return author; }
    public void setAuthor(String value) { author = value; }
}
