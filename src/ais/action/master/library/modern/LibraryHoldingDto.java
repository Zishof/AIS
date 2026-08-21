package ais.action.master.library.modern;

import org.json.JSONException;
import org.json.JSONObject;

/** Aggregated, public-safe availability for one library branch. */
public final class LibraryHoldingDto {
    private final Long libraryId;
    private final String libraryName;
    private final int total;
    private final int available;
    private final String dueDate;
    private final String shelf;
    private final String status;

    public LibraryHoldingDto(Long libraryId, String libraryName, int total, int available,
            String dueDate, String shelf) {
        this(libraryId,libraryName,total,available,dueDate,shelf,null);
    }

    public LibraryHoldingDto(Long libraryId, String libraryName, int total, int available,
            String dueDate, String shelf, String status) {
        this.libraryId = libraryId;
        this.libraryName = libraryName == null || libraryName.trim().length() == 0
                ? "Lokasi belum ditentukan" : libraryName.trim();
        this.total = Math.max(0, total);
        this.available = Math.max(0, Math.min(available, total));
        this.dueDate = dueDate == null ? "" : dueDate;
        this.shelf = shelf == null ? "" : shelf;
        this.status = status == null || status.trim().length()==0
                ? (this.available > 0 ? "AVAILABLE" : this.total > 0 ? "LOANED" : "NO_HOLDINGS") : status;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("libraryId", libraryId).put("libraryName", libraryName)
                .put("total", total).put("available", available).put("loaned", Math.max(0, total - available))
                .put("status", status).put("dueDate", dueDate).put("shelf", shelf);
    }

    public int getTotal() { return total; }
    public int getAvailable() { return available; }
}
