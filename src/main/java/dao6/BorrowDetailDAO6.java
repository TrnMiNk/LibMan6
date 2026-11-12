package dao6;

import model6.*;
import java.sql.*;
import java.util.*;

public class BorrowDetailDAO6 extends DAO6 {
    public List<BorrowDetail6> getBorrowedItems(long cardId) throws SQLException {
        try (Connection connection = getConnection()) { return getBorrowedItems(connection, cardId); }
    }

    static List<BorrowDetail6> getBorrowedItems(Connection connection, long cardId) throws SQLException {
        String sql = "SELECT b.id,b.borrowQuantity,b.dueDate,b.borrowRecordId,br.borrowDate,d.id AS documentId,d.title,d.author,d.category,"
                + "COALESCE((SELECT SUM(rd.returnedQuantity) FROM ReturnDetail6 rd WHERE rd.borrowDetailId=b.id),0) AS returnedQuantity "
                + "FROM BorrowDetail6 b JOIN BorrowRecord6 br ON br.id=b.borrowRecordId JOIN Document6 d ON d.id=b.documentId "
                + "WHERE br.libraryCardId=? ORDER BY b.id";
        List<BorrowDetail6> items = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cardId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    BorrowDetail6 item = read(result, cardId);
                    if (item.getRemainingQuantity() > 0) items.add(item);
                }
            }
        }
        return items;
    }

    static BorrowDetail6 read(ResultSet result, long cardId) throws SQLException {
        Document6 document = new Document6();
        document.setId(result.getLong("documentId")); document.setTitle(result.getString("title"));
        document.setAuthor(result.getString("author")); document.setCategory(result.getString("category"));
        LibraryCard6 card = new LibraryCard6(); card.setId(cardId);
        BorrowRecord6 record = new BorrowRecord6(); record.setId(result.getLong("borrowRecordId"));
        record.setBorrowDate(result.getDate("borrowDate").toLocalDate()); record.setLibraryCard(card);
        BorrowDetail6 item = new BorrowDetail6(); item.setId(result.getLong("id"));
        item.setBorrowQuantity(result.getInt("borrowQuantity")); item.setReturnedQuantity(result.getInt("returnedQuantity"));
        item.setDueDate(result.getDate("dueDate").toLocalDate()); item.setBorrowRecord(record); item.setDocument(document);
        return item;
    }
}
