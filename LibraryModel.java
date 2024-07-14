/*
 * LibraryModel.java
 * Author:
 * Created on:
 */


import javax.swing.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class LibraryModel {

    // For use in creating dialogs and making them modal
    private JFrame dialogParent;

    private Connection connection;

    public LibraryModel(JFrame parent, String userid, String password) {
        dialogParent = parent;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/zhangziha_jdbc", userid, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String bookLookup(int isbn) {
        {
            PreparedStatement statement = null;
            Map<Integer, String> data = new HashMap<>();
            try {
                statement = connection.prepareStatement("select * from book inner join book_author on book.isbn = book_author.isbn inner join author on book_author.authorid = author.authorid where book.isbn = ? order by book.isbn, " +
                        " book_author.authorseqno");
                statement.setInt(1, isbn);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    StringBuilder result = new StringBuilder();
                    if (data.containsKey(isbn)) {
                        String temp = data.get(isbn);
                        temp = temp + " " + resultSet.getString("surname") + ".";
                        data.put(isbn, temp);
                    } else {
                        result.append("   ").append(isbn).append(": ").append(resultSet.getString("title")).append("\n")
                                .append("    Edtion: ").append(resultSet.getString("edition_no"))
                                .append(" - ").append("Number of copies: ").append(resultSet.getString("numofcop"))
                                .append(" - ").append("Copies left: ").append(resultSet.getString("numleft")).append("\n")
                                .append("    Authors: ").append(resultSet.getString("surname").trim()).append(".");
                        data.put(isbn, result.toString());
                    }

                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            StringBuilder returned = new StringBuilder("Book Lookup:\n");
            Collection<String> values = data.values();
            for (String value : values) {
                returned.append(value.substring(0, value.length() - 1)).append("\n");
            }
            return returned.toString();
        }
    }

    public String showCatalogue() {
        PreparedStatement statement = null;
        Map<String, String> data = new HashMap<>();
        try {
            statement = connection.prepareStatement("select * from book inner join book_author on book.isbn = book_author.isbn inner join author on book_author.authorid = author.authorid order by book.isbn,  book_author.authorseqno");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                StringBuilder result = new StringBuilder();
                String isbn = resultSet.getString("isbn");
                if (data.containsKey(isbn)) {
                    String temp = data.get(isbn);
                    temp = temp + " " + resultSet.getString("surname") + ".";
                    data.put(isbn, temp);
                } else {
                    result.append(isbn).append(": ").append(resultSet.getString("title")).append("\n")
                            .append("    Edtion: ").append(resultSet.getString("edition_no"))
                            .append(" - ").append("Number of copies: ").append(resultSet.getString("numofcop"))
                            .append(" - ").append("Copies left: ").append(resultSet.getString("numleft")).append("\n")
                            .append("    Authors: ").append(resultSet.getString("surname").trim()).append(".");
                    data.put(isbn, result.toString());
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        StringBuilder returned = new StringBuilder();
        Collection<String> values = data.values();
        for (String value : values) {
            returned.append(value.substring(0, value.length() - 1)).append("\n");
        }
        return returned.toString();
    }

    public String showLoanedBooks() {
        StringBuilder result = new StringBuilder("Show Loaned Books:\n\n");
        try {
            PreparedStatement ps = connection.prepareStatement("select * from cust_book");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int isbn = rs.getInt("isbn");
                int customerid = rs.getInt("customerid");
                PreparedStatement ps1 = connection.prepareStatement("select * from book where isbn = ?");
                ps1.setInt(1, isbn);
                ResultSet rs1 = ps1.executeQuery();
                if (rs1.next()) {
                    result.append(isbn).append(": ").append(rs1.getString("title").trim()).append("\n");
                    result.append("    Edition: ").append(rs1.getInt("edition_no"))
                            .append(" - Number of copies: ").append(rs1.getInt("numofcop"))
                            .append(" - Copies left: ").append(rs1.getInt("numleft")).append("\n");
                }
                PreparedStatement ps2 = connection.prepareStatement("select * from book_author inner join author on book_author.authorid = author.authorid where isbn = ? order by authorseqno");
                ps2.setInt(1, isbn);
                ResultSet rs2 = ps2.executeQuery();
                List<String> authorList = new ArrayList<>();
                while (rs2.next()) {
                    authorList.add(rs2.getString("surname").trim());
                }
                String authors = "    Authors: " + String.join(", ", authorList);
                result.append(authors).append("\n");

                // borrows
                result.append("    Borrowers:").append("\n");
                PreparedStatement ps3 = connection.prepareStatement("select * from cust_book where isbn = ?");
                ps3.setInt(1, isbn);
                ResultSet rs3 = ps3.executeQuery();
                while (rs3.next()) {
                    PreparedStatement ps4 = connection.prepareStatement("select * from customer where customerid = ?");
                    ps4.setInt(1, customerid);
                    ResultSet rs4 = ps4.executeQuery();
                    if (rs4.next()) {
                        String city = rs4.getString("city");
                        if (city == null || city.trim().equals("")) {
                            city = "(no city)";
                        }
                        result.append("        ").append(customerid).append(": ").append(rs4.getString("l_name").trim())
                                .append(", ").append(rs4.getString("f_name").trim()).append(" - ")
                                .append(city.trim()).append("\n");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public String showAuthor(int authorID) {
        StringBuilder result = new StringBuilder("Show Author:\n");
        boolean first = true;
        try {
            PreparedStatement statement = connection.prepareStatement("select * from book inner join book_author on book.isbn = book_author.isbn inner join author on book_author.authorid = author.authorid " +
                    " where author.authorid = ? order by book.isbn desc");
            statement.setInt(1, authorID);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                if (first) {
                    result.append("    ").append(resultSet.getInt("authorid"))
                            .append(" - ").append(resultSet.getString("surname").trim())
                            .append(" ").append(resultSet.getString("name")).append("\n")
                            .append("    ").append("Books writters:\n")
                            .append("        ").append(resultSet.getInt("isbn"))
                            .append(" - ").append(resultSet.getString("title")).append("\n");
                    first = false;
                } else {
                    result.append("        ").append(resultSet.getInt("isbn"))
                            .append(" - ").append(resultSet.getString("title")).append("\n");
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public String showAllAuthors() {
        StringBuilder result = new StringBuilder("Show All Authors:\n");
        try {
            PreparedStatement statement = connection.prepareStatement("select * from author");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                result.append("    ").append(resultSet.getInt("authorid"))
                        .append(": ").append(resultSet.getString("surname").trim())
                        .append(", ").append(resultSet.getString("name")).append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public String showCustomer(int customerID) {
        return "Show Customer Stub";
    }

    public String showAllCustomers() {
        StringBuilder result = new StringBuilder("Show All Customers:\n");
        try {
            PreparedStatement statement = connection.prepareStatement("select * from customer");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String city = resultSet.getString("city");
                if (city == null || city.trim().equals("")) {
                    city = "(no city)";
                }
                result.append("    ").append(resultSet.getInt("customerid"))
                        .append(": ").append(resultSet.getString("l_name").trim())
                        .append(", ").append(resultSet.getString("f_name").trim())
                        .append(" - ").append(city).append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public String borrowBook(int isbn, int customerID,
                             int day, int month, int year) {
        StringBuilder builder = new StringBuilder("Borrow Book:\n");
        String sql = "select * from book where isbn = ?";
        String[] months = {"January", "February", "March", "April",
                "May", "June", "July", "August",
                "September", "October", "November", "December"};
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, isbn);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                int numleft = resultSet.getInt("numleft");
                if (numleft <= 0) {
                    builder.append("    Not enough copies of book " + isbn + " left\n");
                } else {
                    builder.append("    Book: " + isbn + " (" + resultSet.getString("title").trim() + ")\n");
                    PreparedStatement ps1 = connection.prepareStatement("select * from customer where customerid = ?");
                    ps1.setInt(1, customerID);
                    ResultSet rs1 = ps1.executeQuery();
                    if (rs1.next()) {
                        builder.append("    Loaned to: ").append(customerID).append(" (").append(rs1.getString("f_name").trim()).append(" ").append(rs1.getString("l_name").trim()).append(")\n");
                        builder.append("    Due Date: " + day + " " + months[month] + " " + year + "\n");
                    }

                    // update books left number
                    PreparedStatement ps2 = connection.prepareStatement("update book set numleft = numleft - 1 where isbn = ?");
                    ps2.setInt(1, isbn);
                    ps2.execute();

                    // update loaned table record
                    PreparedStatement ps3 = connection.prepareStatement("insert into cust_book values(?,?,?)");
                    ps3.setInt(1, isbn);
                    ps3.setDate(2, Date.valueOf(year + "-" + (month + 1) + "-" + day));
                    ps3.setInt(3, customerID);
                    ps3.execute();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public String returnBook(int isbn, int customerid) {
        StringBuilder builder = new StringBuilder("Return Book:\n");
        try {
            PreparedStatement ps = connection.prepareStatement("select * from cust_book where isbn = ? and customerid = ?");
            ps.setInt(1, isbn);
            ps.setInt(2, customerid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // update loan record
                PreparedStatement ps1 = connection.prepareStatement("delete  from cust_book where isbn = ? and customerid = ?");
                ps1.setInt(1, isbn);
                ps1.setInt(2, customerid);
                ps1.executeUpdate();

                // update books leftnum
                PreparedStatement ps2 = connection.prepareStatement("update book set numleft = numleft + 1 where isbn = ?");
                ps2.setInt(1, isbn);
                ps2.execute();
                builder.append("    Book ").append(isbn).append(" returned for customer ").append(customerid).append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public void closeDBConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String deleteCus(int customerID) {
        StringBuilder result = new StringBuilder("Delete Customer:\n");
        try {
            PreparedStatement ps = connection.prepareStatement("select * from cust_book where customerid = ?");
            ps.setInt(1, customerID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.append("     delete customer " + customerID + " failed\n");
            } else {
                PreparedStatement ps1 = connection.prepareStatement("delete from customer where customerid = ?");
                ps1.setInt(1, customerID);
                ps1.executeUpdate();
                result.append("     delete customer " + customerID + " successfully\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public String deleteAuthor(int authorID) {
        StringBuilder result = new StringBuilder("Delete Author:\n");
        try {
            // delete author
            PreparedStatement ps1 = connection.prepareStatement("delete from author where authorid = ?");
            ps1.setInt(1, authorID);
            ps1.executeUpdate();
            // update book_author
            PreparedStatement ps2 = connection.prepareStatement("update book_author set authorid = 0 where authorid = ?");
            ps2.setInt(1, authorID);
            ps2.executeUpdate();
            result.append("     delete author " + authorID + " successfully\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public String deleteBook(int isbn) {
        StringBuilder result = new StringBuilder("Delete Book:\n");
        try {
            PreparedStatement ps = connection.prepareStatement("select * from cust_book where isbn = ?");
            ps.setInt(1, isbn);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result.append("     delete book " + isbn + " failed\n");
            } else {
                // delete book
                PreparedStatement ps1 = connection.prepareStatement("delete from book where isbn = ?");
                ps1.setInt(1, isbn);
                ps1.executeUpdate();
                // update book_author
                PreparedStatement ps2 = connection.prepareStatement("update book_author set isbn = 0 where isbn = ?");
                ps2.setInt(1, isbn);
                ps2.executeUpdate();
                result.append("     delete book " + isbn + " successfully\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}