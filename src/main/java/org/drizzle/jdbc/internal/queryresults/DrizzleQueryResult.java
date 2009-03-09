package org.drizzle.jdbc.internal.queryresults;

import org.drizzle.jdbc.internal.packet.FieldPacket;
import org.drizzle.jdbc.internal.ValueObject;

import java.util.*;

/**
 * TODO: refactor, badly need to split this into two/three different classes, one for insert/update/ddl, one for selects and one for generated keys?
 *
 * User: marcuse
 * Date: Jan 23, 2009
 * Time: 8:15:55 PM
 */
public class DrizzleQueryResult implements SelectQueryResult {


    private final List<ColumnInformation> columnInformation;
    private final List<List<ValueObject>> resultSet;
    private final Map<String, Integer> columnNameMap;
    private int rowCounter;

    public DrizzleQueryResult(List<ColumnInformation> columnInformation, List<List<ValueObject>> valueObjects) {
        this.columnInformation = Collections.unmodifiableList(columnInformation);
        this.resultSet=Collections.unmodifiableList(valueObjects);
        columnNameMap=new HashMap<String,Integer>();
        rowCounter=-1;
        int i=0;
        for(ColumnInformation ci : columnInformation) {
            columnNameMap.put(ci.getName().toLowerCase(),i++);
        }
    }
    
    public boolean next() {
        rowCounter++;
        return rowCounter < resultSet.size();
    }

    public void close() {
        
    }

    public short getWarnings() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getMessage() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<ColumnInformation> getColumnInformation() {
        return columnInformation;
    }

    /**
     * gets the value at position i in the result set. i starts at zero!
     * @param i index, starts at 0
     * @return
     */
    public ValueObject getValueObject(int i) {
        return resultSet.get(rowCounter).get(i);
    }

    public ValueObject getValueObject(String column) {
        return getValueObject(columnNameMap.get(column.toLowerCase()));
    }

    public int getRows() {
        return resultSet.size();
    }


    public int getColumnId(String columnLabel) {
        return columnNameMap.get(columnLabel.toLowerCase());
    }

    public void moveRowPointerTo(int i) {
        this.rowCounter=i;
    }

    public int getRowPointer() {
        return rowCounter;
    }


    public ResultSetType getResultSetType() {
        return ResultSetType.SELECT;
    }
}