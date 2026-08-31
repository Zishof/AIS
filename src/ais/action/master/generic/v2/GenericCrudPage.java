package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO satu halaman hasil query pada framework generic-CRUD-v2: membungkus baris data
 * ({@link #getRows()}) beserta metadata paginasi (total baris keseluruhan, nomor halaman saat ini
 * 1-based, ukuran halaman). Dipakai sebagai bentuk respons standar seluruh endpoint listing CRUD
 * generik agar klien (grid ZK/JS) dapat menghitung navigasi halaman secara konsisten.
 */
@SuppressWarnings("rawtypes")
public class GenericCrudPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private List rows = new ArrayList();
    private long total;
    private int page = 1;
    private int pageSize = 10;

    public List getRows() { return rows; }
    /** Mengeset baris data halaman ini; {@code null} dinormalkan menjadi list kosong agar aman diiterasi pemanggil. */
    public void setRows(List rows) { this.rows = rows == null ? new ArrayList() : rows; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    /** Menghitung jumlah total halaman dari {@link #getTotal()} dibagi {@link #getPageSize()}, dibulatkan ke atas; mengembalikan 0 bila ukuran halaman kurang dari 1 (menghindari pembagian oleh nol). */
    public long getPageCount() { return pageSize < 1 ? 0 : (total + pageSize - 1) / pageSize; }
}
