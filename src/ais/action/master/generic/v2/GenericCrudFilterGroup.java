package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO grup filter pada framework generic-CRUD-v2: mewakili sekumpulan kondisi filter yang
 * digabung dengan operator AND ({@code conjunction=true}, default) atau OR ({@code false}).
 * Elemen di {@link #getFilters()} biasanya berupa definisi filter individual atau grup filter
 * bersarang lainnya, memungkinkan kombinasi filter bertingkat (mis. {@code (A AND B) OR (C AND
 * D)}) dibangun dari sisi klien layar CRUD generik. Serializable agar dapat dikirim sebagai
 * bagian payload request/response JSON.
 */
@SuppressWarnings("rawtypes")
public class GenericCrudFilterGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean conjunction = true;
    private List filters = new ArrayList();
    public boolean isConjunction() { return conjunction; }
    public void setConjunction(boolean conjunction) { this.conjunction = conjunction; }
    public List getFilters() { return filters; }
    /** Mengeset daftar filter/subgrup dalam grup ini; {@code null} dinormalkan menjadi list kosong (bukan null) agar aman diiterasi pemanggil. */
    public void setFilters(List filters) { this.filters = filters == null ? new ArrayList() : filters; }
}
