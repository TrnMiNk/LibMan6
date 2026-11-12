package model6;

import java.time.LocalDateTime;

public class LibraryCard6 {
    private long id;
    private String status;
    private LocalDateTime registeredAt;
    private Reader6 reader;

    public LibraryCard6() {}

    public LibraryCard6(long id, String status, LocalDateTime registeredAt, Reader6 reader) {
        this.id = id;
        this.status = status;
        this.registeredAt = registeredAt;
        this.reader = reader;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public Reader6 getReader() { return reader; }
    public void setReader(Reader6 reader) { this.reader = reader; }
}
