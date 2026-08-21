package ais.action.master.library.modern;

import javax.servlet.http.HttpServletRequest;

/**
 * Typed and bounded request for the public library catalog.
 * Browser callers cannot supply SQL, HQL, entity names, or sort properties.
 */
public class LibraryCatalogSearchRequest {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    private String query;
    private String title;
    private String isbn;
    private String issn;
    private String author;
    private String publisher;
    private String language;
    private String edition;
    private String notes;
    private String exclude;
    private String searchField;
    private String subject;
    private String callNumber;
    private String barcode;
    private String availability;
    private Long libraryId;
    private Long itemTypeId;
    private Long materialTypeId;
    private Long foundationId;
    private Long schoolId;
    private Long facultyId;
    private Long studyProgramId;
    private Integer yearFrom;
    private Integer yearTo;
    private Integer year;
    private int page = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private String sort = "NEWEST";

    public static LibraryCatalogSearchRequest from(HttpServletRequest request) {
        LibraryCatalogSearchRequest value = new LibraryCatalogSearchRequest();
        value.query = text(request.getParameter("query"), 160);
        value.title = text(request.getParameter("title"), 160);
        value.isbn = text(request.getParameter("isbn"), 32);
        value.issn = text(request.getParameter("issn"), 32);
        value.author = text(request.getParameter("author"), 120);
        value.publisher = text(request.getParameter("publisher"), 120);
        value.language = text(request.getParameter("language"), 60);
        value.edition = text(request.getParameter("edition"), 80);
        value.notes = text(request.getParameter("notes"), 160);
        value.exclude = text(request.getParameter("exclude"), 160);
        value.searchField = allowedSearchField(request.getParameter("searchField"));
        value.subject = text(request.getParameter("subject"), 120);
        value.callNumber = text(request.getParameter("callNumber"), 80);
        value.barcode = text(request.getParameter("barcode"), 80);
        value.availability = allowedAvailability(request.getParameter("availability"));
        value.libraryId = positiveLong(request.getParameter("libraryId"));
        value.itemTypeId = positiveLong(request.getParameter("itemTypeId"));
        value.materialTypeId = positiveLong(request.getParameter("materialTypeId"));
        value.foundationId = positiveLong(request.getParameter("foundationId"));
        value.schoolId = positiveLong(request.getParameter("schoolId"));
        value.facultyId = positiveLong(request.getParameter("facultyId"));
        value.studyProgramId = positiveLong(request.getParameter("studyProgramId"));
        value.yearFrom = year(request.getParameter("yearFrom"));
        value.yearTo = year(request.getParameter("yearTo"));
        value.year = year(request.getParameter("year"));
        value.page = boundedInt(request.getParameter("page"), 1, 100000, 1);
        value.pageSize = boundedInt(request.getParameter("pageSize"), 1, MAX_PAGE_SIZE, DEFAULT_PAGE_SIZE);
        value.sort = allowedSort(request.getParameter("sort"));
        if (value.yearFrom != null && value.yearTo != null && value.yearFrom.intValue() > value.yearTo.intValue()) {
            Integer swap = value.yearFrom;
            value.yearFrom = value.yearTo;
            value.yearTo = swap;
        }
        return value;
    }

    private static String text(String raw, int maxLength) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.length() == 0) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static Long positiveLong(String raw) {
        try {
            long value = Long.parseLong(raw == null ? "" : raw.trim());
            return value > 0L ? Long.valueOf(value) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Integer year(String raw) {
        int value = boundedInt(raw, 1000, 2200, -1);
        return value < 0 ? null : Integer.valueOf(value);
    }

    private static int boundedInt(String raw, int min, int max, int fallback) {
        try {
            int value = Integer.parseInt(raw == null ? "" : raw.trim());
            return value < min || value > max ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String allowedSort(String raw) {
        if ("TITLE_ASC".equals(raw) || "AUTHOR_ASC".equals(raw) || "YEAR_DESC".equals(raw)
                || "POPULAR".equals(raw) || "NEWEST".equals(raw)) {
            return raw;
        }
        return "NEWEST";
    }

    private static String allowedSearchField(String raw) {
        if ("TITLE".equals(raw) || "AUTHOR".equals(raw) || "ISBN".equals(raw)
                || "SUBJECT".equals(raw) || "PUBLISHER".equals(raw)
                || "CALL_NUMBER".equals(raw) || "BARCODE".equals(raw)) return raw;
        return "ALL";
    }

    private static String allowedAvailability(String raw) {
        if ("AVAILABLE".equals(raw) || "LOANED".equals(raw) || "DIGITAL".equals(raw)) return raw;
        return null;
    }

    public int getOffset() { return (page - 1) * pageSize; }
    public String getQuery() { return query; }
    public String getTitle() { return title; }
    public String getIsbn() { return isbn; }
    public String getIssn() { return issn; }
    public String getAuthor() { return author; }
    public String getPublisher() { return publisher; }
    public String getLanguage() { return language; }
    public String getEdition() { return edition; }
    public String getNotes() { return notes; }
    public String getExclude() { return exclude; }
    public String getSearchField() { return searchField; }
    public String getSubject() { return subject; }
    public String getCallNumber() { return callNumber; }
    public String getBarcode() { return barcode; }
    public String getAvailability() { return availability; }
    public Long getLibraryId() { return libraryId; }
    public Long getItemTypeId() { return itemTypeId; }
    public Long getMaterialTypeId() { return materialTypeId; }
    public Long getFoundationId() { return foundationId; }
    public Long getSchoolId() { return schoolId; }
    public Long getFacultyId() { return facultyId; }
    public Long getStudyProgramId() { return studyProgramId; }
    public Integer getYearFrom() { return yearFrom; }
    public Integer getYearTo() { return yearTo; }
    public Integer getYear() { return year; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public String getSort() { return sort; }
    public void setQuery(String query) { this.query = text(query, 160); }
    public void setTitle(String title) { this.title = text(title, 160); }
    public void setIsbn(String isbn) { this.isbn = text(isbn, 32); }
    public void setIssn(String issn) { this.issn = text(issn, 32); }
    public void setAuthor(String author) { this.author = text(author, 120); }
    public void setPublisher(String publisher) { this.publisher = text(publisher, 120); }
    public void setLanguage(String language) { this.language = text(language, 60); }
    public void setEdition(String edition) { this.edition = text(edition, 80); }
    public void setNotes(String notes) { this.notes = text(notes, 160); }
    public void setExclude(String exclude) { this.exclude = text(exclude, 160); }
    public void setSearchField(String searchField) { this.searchField = allowedSearchField(searchField); }
    public void setSubject(String subject) { this.subject = text(subject, 120); }
    public void setCallNumber(String callNumber) { this.callNumber = text(callNumber, 80); }
    public void setBarcode(String barcode) { this.barcode = text(barcode, 80); }
    public void setAvailability(String availability) { this.availability = allowedAvailability(availability); }
    public void setItemTypeId(Long itemTypeId) { this.itemTypeId = itemTypeId; }
    public void setMaterialTypeId(Long materialTypeId) { this.materialTypeId = materialTypeId; }
    public void setLibraryId(Long libraryId) { this.libraryId = libraryId; }
    public void setFoundationId(Long foundationId) { this.foundationId = foundationId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }
    public void setStudyProgramId(Long studyProgramId) { this.studyProgramId = studyProgramId; }
    public void setPage(int page) { this.page = page < 1 ? 1 : page; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE); }
    public void setSort(String sort) { this.sort = allowedSort(sort); }
}
