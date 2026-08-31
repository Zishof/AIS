package ais.action.master.library.modern;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * DTO hasil pencarian katalog perpustakaan modern: satu halaman {@link LibraryCatalogItemDto}
 * beserta metadata paginasi ({@code page}/{@code pageSize}/{@code total}) dan facet pencarian
 * (kategori, penulis, dsb dalam bentuk {@link JSONObject} bebas). {@link #toJson()} menyerialisasi
 * seluruh field menjadi satu objek JSON respons API, dengan {@code total} disertakan dua kali
 * (kunci {@code "count"} dan {@code "total"}) dan daftar item disertakan dua kali (kunci
 * {@code "data"} dan {@code "items"}) demi kompatibilitas dengan beberapa varian klien front-end
 * yang membaca nama kunci berbeda.
 */
public class LibraryCatalogSearchResult {
    private int page;
    private int pageSize;
    private long total;
    private List<LibraryCatalogItemDto> items = new ArrayList<LibraryCatalogItemDto>();
    private JSONObject facets = new JSONObject();

    /**
     * Menyerialisasi hasil pencarian menjadi objek JSON respons API (page, pageSize, total/count,
     * facets, dan daftar item di bawah kunci {@code data} maupun {@code items}).
     *
     * @return objek JSON siap dikirim sebagai respons
     * @throws JSONException bila penyusunan objek JSON gagal
     */
    public JSONObject toJson() throws JSONException {
        JSONArray data = new JSONArray();
        for (LibraryCatalogItemDto item : items) data.put(item.toJson());
        // put(String, Object) dipakai untuk "total"/"count" (bukan overload put(String, long))
        // karena WEB-INF/lib memuat beberapa jar org.json berbeda (mis. json-rpc-1.0.jar
        // membawa org.json.JSONObject versi lama tanpa overload put(String, long)); boxing
        // ke Long memastikan resolusi ke overload put(String, Object) yang ada di semua versi.
        return new JSONObject()
                .put("ok", true)
                .put("page", page)
                .put("pageSize", pageSize)
                .put("count", Long.valueOf(total))
                .put("total", Long.valueOf(total))
                .put("facets", facets)
                .put("data", data)
                .put("items", data);
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public List<LibraryCatalogItemDto> getItems() { return items; }
    public void setItems(List<LibraryCatalogItemDto> items) { this.items = items; }
    public JSONObject getFacets() { return facets; }
    public void setFacets(JSONObject facets) { this.facets = facets == null ? new JSONObject() : facets; }
}
