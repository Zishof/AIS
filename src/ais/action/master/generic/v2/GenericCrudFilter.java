package ais.action.master.generic.v2;

import java.io.Serializable;

public class GenericCrudFilter implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String EQ = "EQ";
    public static final String NE = "NE";
    public static final String CONTAINS = "CONTAINS";
    public static final String STARTS_WITH = "STARTS_WITH";
    public static final String GT = "GT";
    public static final String GTE = "GTE";
    public static final String LT = "LT";
    public static final String LTE = "LTE";
    public static final String IS_NULL = "IS_NULL";
    public static final String IS_NOT_NULL = "IS_NOT_NULL";
    public static final String IN = "IN";

    private String property;
    private String operator = EQ;
    private Object value;
    private Object secondValue;

    public GenericCrudFilter() { }
    public GenericCrudFilter(String property, String operator, Object value) {
        this.property = property;
        this.operator = operator;
        this.value = value;
    }
    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public Object getSecondValue() { return secondValue; }
    public void setSecondValue(Object value) { secondValue = value; }
}
