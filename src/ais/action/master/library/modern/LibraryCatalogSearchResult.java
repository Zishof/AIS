package ais.action.master.library.modern;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class LibraryCatalogSearchResult {
    private int page;
    private int pageSize;
    private long total;
    private List<LibraryCatalogItemDto> items = new ArrayList<LibraryCatalogItemDto>();
    private JSONObject facets = new JSONObject();

    public JSONObject toJson() throws JSONException {
        JSONArray data = new JSONArray();
        for (LibraryCatalogItemDto item : items) data.put(item.toJson());
        return new JSONObject()
                .put("ok", true)
                .put("page", page)
                .put("pageSize", pageSize)
                .put("count", total)
                .put("total", total)
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
