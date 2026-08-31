package ais.action.master.generic.v2;

import java.io.Serializable;

/**
 * Objek data (DTO) satu kriteria penyaringan pada framework CRUD generik ({@code generic/v2}):
 * memasangkan nama {@code property} entitas dengan {@code operator} pembanding (lihat konstanta
 * seperti {@link #EQ}, {@link #CONTAINS}, {@link #IN}, dsb.) dan nilai pembanding
 * ({@link #value}, ditambah {@link #secondValue} untuk operator rentang/antara). Digunakan oleh
 * lapisan query CRUD generik untuk membangun kriteria pencarian secara dinamis dari permintaan
 * klien, tanpa perlu kode kueri khusus per entitas.
 */
public class GenericCrudFilter implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Operator kesetaraan (sama dengan). */
    public static final String EQ = "EQ";
    /** Operator tidak sama dengan. */
    public static final String NE = "NE";
    /** Operator mengandung teks (LIKE %value%). */
    public static final String CONTAINS = "CONTAINS";
    /** Operator diawali teks (LIKE value%). */
    public static final String STARTS_WITH = "STARTS_WITH";
    /** Operator lebih besar dari. */
    public static final String GT = "GT";
    /** Operator lebih besar atau sama dengan. */
    public static final String GTE = "GTE";
    /** Operator lebih kecil dari. */
    public static final String LT = "LT";
    /** Operator lebih kecil atau sama dengan. */
    public static final String LTE = "LTE";
    /** Operator nilai kosong (NULL). */
    public static final String IS_NULL = "IS_NULL";
    /** Operator nilai tidak kosong (NOT NULL). */
    public static final String IS_NOT_NULL = "IS_NOT_NULL";
    /** Operator keanggotaan dalam himpunan nilai. */
    public static final String IN = "IN";

    private String property;
    private String operator = EQ;
    private Object value;
    private Object secondValue;

    /** Membuat filter kosong; nilai diisi lewat setter sebelum dipakai. */
    public GenericCrudFilter() { }

    /**
     * @param property nama properti entitas yang disaring
     * @param operator salah satu konstanta operator (mis. {@link #EQ}, {@link #CONTAINS})
     * @param value    nilai pembanding
     */
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
