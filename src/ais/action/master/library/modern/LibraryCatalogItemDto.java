package ais.action.master.library.modern;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Public-safe catalog projection. */
public class LibraryCatalogItemDto {
    private Long id;
    private String title;
    private String authors;
    private String publisher;
    private String isbn;
    private String issn;
    private String subject;
    private String imageUrl;
    private String summary;
    private String language;
    private String callNumber;
    private Integer year;
    private String edition;
    private Integer pages;
    private int copyCount;
    private int availableCount;
    private String itemType;
    private String materialType;
    private boolean digital;
    private String digitalUrl;
    private List<LibraryHoldingDto> holdings = new ArrayList<LibraryHoldingDto>();

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("nama", safe(title));
        json.put("pengarangs", safe(authors));
        json.put("penerbit", new JSONObject().put("nama", safe(publisher)));
        json.put("isbn", safe(isbn));
        json.put("issn", safe(issn));
        json.put("tema", safe(subject));
        json.put("kategories", safe(subject));
        json.put("imageUrl", safe(imageUrl));
        json.put("ringkasan", safe(summary));
        json.put("bahasa", safe(language));
        json.put("callNumber", safe(callNumber));
        json.put("tahun", year == null ? JSONObject.NULL : year);
        json.put("edisi", safe(edition));
        json.put("halaman", pages == null ? JSONObject.NULL : pages);
        json.put("jumlahEksemplar", copyCount);
        json.put("jumlahTersedia", availableCount);
        json.put("jenisKoleksi", safe(itemType));
        json.put("format", safe(materialType));
        json.put("digital", digital);
        json.put("digitalUrl", safe(digitalUrl));
        JSONArray holdingData = new JSONArray();
        for (LibraryHoldingDto holding : holdings) holdingData.put(holding.toJson());
        json.put("holdings", holdingData);
        return json;
    }

    private static String safe(String value) { return value == null ? "" : value; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getIssn() { return issn; }
    public void setIssn(String issn) { this.issn = issn; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCallNumber() { return callNumber; }
    public void setCallNumber(String callNumber) { this.callNumber = callNumber; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }
    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }
    public int getCopyCount() { return copyCount; }
    public void setCopyCount(int copyCount) { this.copyCount = copyCount; }
    public int getAvailableCount() { return availableCount; }
    public void setAvailableCount(int availableCount) { this.availableCount = availableCount; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public boolean isDigital() { return digital; }
    public void setDigital(boolean digital) { this.digital = digital; }
    public String getDigitalUrl() { return digitalUrl; }
    public void setDigitalUrl(String digitalUrl) { this.digitalUrl = digitalUrl; }
    public List<LibraryHoldingDto> getHoldings() { return holdings; }
    public void setHoldings(List<LibraryHoldingDto> holdings) { this.holdings = holdings == null ? new ArrayList<LibraryHoldingDto>() : holdings; }
}
