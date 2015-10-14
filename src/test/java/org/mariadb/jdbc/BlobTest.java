package org.mariadb.jdbc;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.sql.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BlobTest extends BaseTest {

    @BeforeClass()
    public static void initClass() throws SQLException {
        createTable("bug716378", "id int not null primary key auto_increment, test longblob, test2 blob, test3 text");
        createTable("BlobTeststreamtest2", "id int primary key not null, st varchar(20), strm text");
        createTable("BlobTeststreamtest3", "id int primary key not null, strm text");
        createTable("BlobTestclobtest", "id int not null primary key, strm text");
        createTable("BlobTestclobtest2", "strm text");
        createTable("BlobTestclobtest3", "id int not null primary key, strm text");
        createTable("BlobTestclobtest4", "id int not null primary key, strm text");
        createTable("BlobTestblobtest", "id int not null primary key, strm blob");
        createTable("BlobTestblobtest2", "id int not null primary key, strm blob");
        createTable("conj77_test", "Name VARCHAR(100) NOT NULL,Archive LONGBLOB, PRIMARY KEY (Name)", "Engine=InnoDB DEFAULT CHARSET utf8");


    }

    @Test
    public void testPosition() throws SQLException {
        byte[] blobContent = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        byte[] pattern = new byte[]{3, 4};
        Blob blob = new MySQLBlob(blobContent);
        assertEquals(3, blob.position(pattern, 1));
        pattern = new byte[]{12, 13};
        assertEquals(-1, blob.position(pattern, 1));
        pattern = new byte[]{11, 12};
        assertEquals(11, blob.position(pattern, 1));
        pattern = new byte[]{1, 2};
        assertEquals(1, blob.position(pattern, 1));

    }

    @Test(expected = SQLException.class)
    public void testBadStart() throws SQLException {
        byte[] blobContent = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        byte[] pattern = new byte[]{3, 4};
        Blob blob = new MySQLBlob(blobContent);
        blob.position(pattern, 0);
    }

    @Test(expected = SQLException.class)
    public void testBadStart2() throws SQLException {
        byte[] blobContent = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        byte[] pattern = new byte[]{3, 4};
        Blob blob = new MySQLBlob(blobContent);
        blob.position(pattern, 44);
    }

    @Test
    public void testBug716378() throws SQLException {
        Statement stmt = sharedConnection.createStatement();

        stmt.executeUpdate("insert into bug716378 values(null, 'a','b','c')");
        ResultSet rs = stmt.executeQuery("select * from bug716378");
        assertTrue(rs.next());
        byte[] arr = new byte[0];
        assertEquals(arr.getClass(), rs.getObject(2).getClass());
        assertEquals(arr.getClass(), rs.getObject(3).getClass());
        assertEquals(String.class, rs.getObject(4).getClass());
    }


    @Test
    public void testCharacterStreamWithMultibyteCharacterAndLength() throws Throwable {
        String toInsert1 = "\u00D8bbcdefgh\njklmn\"";
        String toInsert2 = "\u00D8abcdefgh\njklmn\"";
        PreparedStatement stmt = sharedConnection.prepareStatement("insert into BlobTeststreamtest2 (id, st, strm) values (?,?,?)");
        stmt.setInt(1, 2);
        stmt.setString(2, toInsert1);
        Reader reader = new StringReader(toInsert2);
        stmt.setCharacterStream(3, reader, 5);
        stmt.execute();

        ResultSet rs = sharedConnection.createStatement().executeQuery("select * from BlobTeststreamtest2");
        rs.next();
        Reader rdr = rs.getCharacterStream("strm");
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = rdr.read()) != -1) {
            sb.append((char) ch);
        }
        assertEquals(toInsert1, rs.getString(2));
        assertEquals(toInsert2.substring(0, 5), sb.toString());
    }

    @Test
    public void testCharacterStreamWithMultibyteCharacter() throws Throwable {
        PreparedStatement stmt = sharedConnection.prepareStatement("insert into BlobTeststreamtest3 (id, strm) values (?,?)");
        stmt.setInt(1, 2);
        String toInsert = "\u00D8abcdefgh\njklmn\"";
        Reader reader = new StringReader(toInsert);
        stmt.setCharacterStream(2, reader);
        stmt.execute();
        ResultSet rs = sharedConnection.createStatement().executeQuery("select * from BlobTeststreamtest3");
        rs.next();
        Reader rdr = rs.getCharacterStream("strm");
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = rdr.read()) != -1) {
            sb.append((char) ch);
        }
        assertEquals(toInsert, sb.toString());
    }

    @Test
    public void testClobWithLengthAndMultibyteCharacter() throws SQLException, IOException {
        PreparedStatement stmt = sharedConnection.prepareStatement("insert into BlobTestclobtest (id, strm) values (?,?)");
        String clob = "\u00D8clob";
        stmt.setInt(1, 1);
        stmt.setClob(2, new StringReader(clob));
        stmt.execute();
        ResultSet rs = sharedConnection.createStatement().executeQuery("select * from BlobTestclobtest");
        rs.next();
        Reader readStuff = rs.getClob("strm").getCharacterStream();
        char[] a = new char[5];
        readStuff.read(a);
        assertEquals(new String(a), clob);
    }

    @Test
    public void testClob3() throws Exception {
        PreparedStatement stmt = sharedConnection.prepareStatement("insert into BlobTestclobtest2 (strm) values (?)");
        Clob clob = sharedConnection.createClob();
        Writer writer = clob.setCharacterStream(1);
        writer.write("\u00D8hello", 0, 6);
        writer.flush();
        stmt.setClob(1, clob);
        stmt.execute();
        ResultSet rs = sharedConnection.createStatement().executeQuery("select * from BlobTestclobtest2");
        rs.next();
        Object o = rs.getObject(1);
        assertTrue(o instanceof String);
        String s = rs.getString(1);
        assertEquals("\u00D8hello", s);
    }

    @Test
    public void testBlob() throws SQLException, IOException {
        PreparedStatement stmt = sharedConnection.prepareStatement("insert into BlobTestblobtest (id, strm) values (?,?)");
        byte[] theBlob = {1, 2, 3, 4, 5, 6};
        InputStream stream = new ByteArrayInputStream(theBlob);
        stmt.setInt(1, 1);
        stmt.setBlob(2, stream);
        stmt.execute();
        ResultSet rs = sharedConnection.createStatement().executeQuery("select * from BlobTestblobtest");
        rs.next();
        InputStream readStuff = rs.getBlob("strm").getBinaryStream();
        int ch;
        int pos = 0;
        while ((ch = readStuff.read()) != -1) {
            assertEquals(theBlob[pos++], ch);
        }

        readStuff = rs.getBinaryStream("strm");

        pos = 0;
        while ((ch = readStuff.read()) != -1) {
            assertEquals(theBlob[pos++], ch);
        }
    }

    @Test
    public void testBlobWithLength() throws SQLException, IOException {
        PreparedStatement stmt = sharedConnection.prepareStatement("insert into BlobTestblobtest2 (id, strm) values (?,?)");
        byte[] theBlob = {1, 2, 3, 4, 5, 6};
        InputStream stream = new ByteArrayInputStream(theBlob);
        stmt.setInt(1, 1);
        stmt.setBlob(2, stream, 4);
        stmt.execute();
        ResultSet rs = sharedConnection.createStatement().executeQuery("select * from BlobTestblobtest2");
        rs.next();
        InputStream readStuff = rs.getBlob("strm").getBinaryStream();
        int ch;
        int pos = 0;
        while ((ch = readStuff.read()) != -1) {
            assertEquals(theBlob[pos++], ch);
        }
    }

    @Test
    public void testClobWithLength() throws SQLException, IOException {
        PreparedStatement stmt = sharedConnection.prepareStatement("insert into BlobTestclobtest3 (id, strm) values (?,?)");
        String clob = "clob";
        stmt.setInt(1, 1);
        stmt.setClob(2, new StringReader(clob));
        stmt.execute();
        ResultSet rs = sharedConnection.createStatement().executeQuery("select * from BlobTestclobtest3");
        rs.next();
        Reader readStuff = rs.getClob("strm").getCharacterStream();
        char[] a = new char[4];
        readStuff.read(a);
        Assert.assertEquals(new String(a), clob);
    }

    @Test
    public void testClob2() throws SQLException, IOException {
        PreparedStatement stmt = sharedConnection.prepareStatement("insert into BlobTestclobtest4 (id, strm) values (?,?)");
        Clob clob = sharedConnection.createClob();
        OutputStream ostream = clob.setAsciiStream(1);
        byte[] bytes = "hello".getBytes();
        ostream.write(bytes);
        stmt.setInt(1, 1);
        stmt.setClob(2, clob);
        stmt.execute();
        ResultSet rs = sharedConnection.createStatement().executeQuery("select * from BlobTestclobtest4");
        rs.next();
        Object o = rs.getObject(2);
        assertTrue(o instanceof String);
        String s = rs.getString(2);
        assertTrue(s.equals("hello"));
    }

    @Test
    public void blobSerialization() throws Exception {
        Blob b = new MySQLBlob(new byte[]{1, 2, 3});
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(b);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        MySQLBlob b2 = (MySQLBlob) ois.readObject();
        byte[] a = b2.getBytes(1, (int) b2.length());
        assertEquals(3, a.length);
        assertEquals(1, a[0]);
        assertEquals(2, a[1]);
        assertEquals(3, a[2]);


        java.sql.Clob c = new MySQLClob(new byte[]{1, 2, 3});
        baos = new ByteArrayOutputStream();
        oos = new ObjectOutputStream(baos);
        oos.writeObject(c);

        ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        MySQLClob c2 = (MySQLClob) ois.readObject();
        a = c2.getBytes(1, (int) c2.length());
        assertEquals(3, a.length);
        assertEquals(1, a[0]);
        assertEquals(2, a[1]);
        assertEquals(3, a[2]);
    }

    @Test
    public void conj73() throws Exception {
       /* CONJ-73: Assertion error: UTF8 length calculation reports invalid ut8 characters */
        java.sql.Clob c = new MySQLClob(new byte[]{(byte) 0x10, (byte) 0xD0, (byte) 0xA0, (byte) 0xe0, (byte) 0xa1, (byte) 0x8e});
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(c);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        MySQLClob c2 = (MySQLClob) ois.readObject();

        assertEquals(3, c2.length());
    }

    @Test
    public void conj77() throws Exception {
        final Statement sta1 = sharedConnection.createStatement();
        try {

            final PreparedStatement pre = sharedConnection.prepareStatement("INSERT INTO conj77_test (Name,Archive) VALUES (?,?)");
            try {
                pre.setString(1, "Empty String");
                pre.setBytes(2, "".getBytes());
                pre.addBatch();

                pre.setString(1, "Data Hello");
                pre.setBytes(2, "hello".getBytes());
                pre.addBatch();

                pre.setString(1, "Empty Data null");
                pre.setBytes(2, null);
                pre.addBatch();

                pre.executeBatch();
            } finally {
                if (pre != null)
                    pre.close();
            }
        } finally {
            if (sta1 != null)
                sta1.close();
        }
        final Statement sta2 = sharedConnection.createStatement();
        try {
            final ResultSet set = sta2.executeQuery("Select name,archive as text FROM conj77_test");
            try {
                while (set.next()) {
                    final Blob blob = set.getBlob("text");
                    if (blob != null) {
                        final ByteArrayOutputStream bout = new ByteArrayOutputStream((int) blob.length());
                        try {
                            final InputStream bin = blob.getBinaryStream();
                            try {
                                final byte[] buffer = new byte[1024 * 4];

                                for (int read = bin.read(buffer); read != -1; read = bin.read(buffer))
                                    bout.write(buffer, 0, read);
                            } finally {
                                if (bin != null)
                                    bin.close();
                            }
                        } finally {
                            if (bout != null)
                                bout.close();
                        }
                    }
                }
            } finally {
                if (set != null)
                    set.close();
            }
        } finally {
            if (sta2 != null)
                sta2.close();
        }
    }


}
