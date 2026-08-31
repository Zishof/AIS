package ais.action.master.generic.v2;

import java.io.Serializable;

/**
 * Objek data (POJO serializable) yang merepresentasikan satu kriteria pengurutan pada framework CRUD
 * generik {@code ais.action.master.generic.v2}: nama properti entitas yang diurutkan dan arahnya
 * (naik/turun, default naik). Dipakai sebagai bagian dari permintaan daftar data untuk menentukan
 * klausa {@code ORDER BY} query Hibernate yang dibangun secara dinamis.
 */
public class GenericCrudSort implements Serializable {
    private static final long serialVersionUID = 1L;
    private String property;
    private boolean ascending = true;

    public GenericCrudSort() { }
    public GenericCrudSort(String property, boolean ascending) {
        this.property = property;
        this.ascending = ascending;
    }
    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }
    public boolean isAscending() { return ascending; }
    public void setAscending(boolean ascending) { this.ascending = ascending; }
}
