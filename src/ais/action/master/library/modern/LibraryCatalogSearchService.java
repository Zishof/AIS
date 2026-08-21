package ais.action.master.library.modern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.JenisItem;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TipeItem;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Server-side catalog query service shared by JSP and ZK views.
 * All user values are bound by Hibernate and every sort/filter is allow-listed.
 */
public class LibraryCatalogSearchService {

    @SuppressWarnings("unchecked")
    public LibraryCatalogSearchResult search(LibraryCatalogSearchRequest request) {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            Criteria countCriteria = createCriteria(session, request);
            Number count = (Number) countCriteria.setProjection(Projections.rowCount()).uniqueResult();

            Criteria dataCriteria = createCriteria(session, request);
            applySort(dataCriteria, request.getSort());
            List<Item> entities = dataCriteria.setFirstResult(request.getOffset())
                    .setMaxResults(request.getPageSize()).list();

            Map<Long, List<LibraryHoldingDto>> holdings = loadHoldings(session, entities);
            List<LibraryCatalogItemDto> items = new ArrayList<LibraryCatalogItemDto>();
            for (Item item : entities) items.add(toDto(item, holdings));

            LibraryCatalogSearchResult result = new LibraryCatalogSearchResult();
            result.setPage(request.getPage());
            result.setPageSize(request.getPageSize());
            result.setTotal(count == null ? 0L : count.longValue());
            result.setItems(items);
            result.setFacets(new LibraryFacetService().facets(session, request));
            return result;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    public JSONObject references() throws JSONException {
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            JSONObject result = new JSONObject();
            result.put("ok", true);
            result.put("libraries", referenceList(session, Perpustakaan.class));
            result.put("itemTypes", referenceList(session, JenisItem.class));
            result.put("materialTypes", referenceList(session, TipeItem.class));
            result.put("foundations", referenceList(session, Yayasan.class));
            result.put("schools", referenceList(session, Sekolah.class));
            result.put("faculties", referenceList(session, Fakultas.class));
            result.put("studyPrograms", referenceList(session, Jurusan.class));
            return result;
        } finally {
            HibernateUtil.closeSessionQuietly(session);
        }
    }

    Criteria createCriteria(Session session, LibraryCatalogSearchRequest request) {
        Criteria criteria = session.createCriteria(Item.class, "item");
        criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        criteria.add(Restrictions.sqlRestriction(
                "{alias}.status_terbit_item in (select id from library.status_terbit_item "
                + "where lower(trim(nama)) in ('terbit','publish','published'))"));

        if (request.getQuery() != null) {
            String value = request.getQuery();
            String field = request.getSearchField();
            if ("TITLE".equals(field)) criteria.add(Restrictions.ilike("nama", value, MatchMode.ANYWHERE));
            else if ("AUTHOR".equals(field)) criteria.add(Restrictions.ilike("pengarangs", value, MatchMode.ANYWHERE));
            else if ("ISBN".equals(field)) criteria.add(Restrictions.or(Restrictions.ilike("isbn", value, MatchMode.ANYWHERE), Restrictions.ilike("issn", value, MatchMode.ANYWHERE)));
            else if ("SUBJECT".equals(field)) criteria.add(Restrictions.or(Restrictions.ilike("kategories", value, MatchMode.ANYWHERE), Restrictions.ilike("tema", value, MatchMode.ANYWHERE)));
            else if ("PUBLISHER".equals(field)) { criteria.createAlias("penerbit", "searchPublisher", Criteria.LEFT_JOIN); criteria.add(Restrictions.ilike("searchPublisher.nama", value, MatchMode.ANYWHERE)); }
            else if ("CALL_NUMBER".equals(field)) criteria.add(Restrictions.ilike("callnumber", value, MatchMode.ANYWHERE));
            else if ("BARCODE".equals(field)) criteria.add(Restrictions.sqlRestriction("{alias}.id in (select item from library.item_punya_barcode where lower(barcode) like ?)", "%" + value.toLowerCase() + "%", Hibernate.STRING));
            else {
                Criterion keyword = Restrictions.ilike("nama", value, MatchMode.ANYWHERE);
                keyword = Restrictions.or(keyword, Restrictions.ilike("pengarangs", value, MatchMode.ANYWHERE));
                keyword = Restrictions.or(keyword, Restrictions.ilike("kategories", value, MatchMode.ANYWHERE));
                keyword = Restrictions.or(keyword, Restrictions.ilike("abstrak", value, MatchMode.ANYWHERE));
                keyword = Restrictions.or(keyword, Restrictions.ilike("kewords", value, MatchMode.ANYWHERE));
                keyword = Restrictions.or(keyword, Restrictions.ilike("isbn", value, MatchMode.ANYWHERE));
                keyword = Restrictions.or(keyword, Restrictions.ilike("issn", value, MatchMode.ANYWHERE));
                keyword = Restrictions.or(keyword, Restrictions.ilike("callnumber", value, MatchMode.ANYWHERE));
                criteria.add(keyword);
            }
        }
        if (request.getTitle() != null) criteria.add(Restrictions.ilike("nama", request.getTitle(), MatchMode.ANYWHERE));
        if (request.getIsbn() != null) {
            criteria.add(Restrictions.or(Restrictions.eq("isbn", request.getIsbn()), Restrictions.eq("isbn10", request.getIsbn())));
        }
        if (request.getIssn() != null) criteria.add(Restrictions.eq("issn", request.getIssn()));
        if (request.getAuthor() != null) criteria.add(Restrictions.ilike("pengarangs", request.getAuthor(), MatchMode.ANYWHERE));
        if (request.getLanguage() != null) criteria.add(Restrictions.ilike("bahasa", request.getLanguage(), MatchMode.ANYWHERE));
        if (request.getEdition() != null) criteria.add(Restrictions.ilike("edisi", request.getEdition(), MatchMode.ANYWHERE));
        if (request.getNotes() != null) {
            Criterion notes = Restrictions.ilike("abstrak", request.getNotes(), MatchMode.ANYWHERE);
            notes = Restrictions.or(notes, Restrictions.ilike("catatan", request.getNotes(), MatchMode.ANYWHERE));
            notes = Restrictions.or(notes, Restrictions.ilike("kewords", request.getNotes(), MatchMode.ANYWHERE));
            criteria.add(notes);
        }
        if (request.getSubject() != null) criteria.add(Restrictions.or(
                Restrictions.ilike("kategories", request.getSubject(), MatchMode.ANYWHERE),
                Restrictions.ilike("tema", request.getSubject(), MatchMode.ANYWHERE)));
        if (request.getCallNumber() != null) criteria.add(Restrictions.ilike("callnumber", request.getCallNumber(), MatchMode.ANYWHERE));
        if (request.getBarcode() != null) criteria.add(Restrictions.sqlRestriction(
                "{alias}.id in (select item from library.item_punya_barcode where lower(barcode) like ?)",
                "%" + request.getBarcode().toLowerCase() + "%", Hibernate.STRING));
        if (request.getPublisher() != null) {
            criteria.createAlias("penerbit", "publisher", Criteria.LEFT_JOIN);
            criteria.add(Restrictions.ilike("publisher.nama", request.getPublisher(), MatchMode.ANYWHERE));
        }
        if (request.getItemTypeId() != null) criteria.add(Restrictions.eq("jenisItem.id", request.getItemTypeId()));
        if (request.getMaterialTypeId() != null) criteria.add(Restrictions.eq("tipeItem.id", request.getMaterialTypeId()));
        if (request.getSchoolId() != null) criteria.add(Restrictions.eq("sekolah.id", request.getSchoolId()));
        if (request.getStudyProgramId() != null) criteria.add(Restrictions.eq("jurusan.id", request.getStudyProgramId()));
        if (request.getYearFrom() != null) criteria.add(Restrictions.ge("tahun", request.getYearFrom()));
        if (request.getYearTo() != null) criteria.add(Restrictions.le("tahun", request.getYearTo()));

        if (request.getLibraryId() != null) {
            criteria.add(Restrictions.sqlRestriction(
                    "{alias}.id in (select ipb.item from library.item_punya_barcode ipb where ipb.perpustakaan = ?)",
                    request.getLibraryId(), Hibernate.LONG));
        }
        if (request.getFoundationId() != null) {
            criteria.add(Restrictions.sqlRestriction(
                    "{alias}.sekolah in (select id from sekolah where yayasan = ?)",
                    request.getFoundationId(), Hibernate.LONG));
        }
        if (request.getFacultyId() != null) {
            criteria.add(Restrictions.sqlRestriction(
                    "{alias}.jurusan in (select id from jurusan where fakultas = ?)",
                    request.getFacultyId(), Hibernate.LONG));
        }
        if ("AVAILABLE".equals(request.getAvailability())) criteria.add(Restrictions.sqlRestriction(
                "exists (select 1 from library.item_punya_barcode b where b.item={alias}.id and not exists "
                + "(select 1 from library.peminjaman_pengadaan_item_detail d where d.item_punya_barcode=b.id and d.kembali_pengadaan_item_detail is null))"));
        else if ("LOANED".equals(request.getAvailability())) criteria.add(Restrictions.sqlRestriction(
                "exists (select 1 from library.item_punya_barcode b join library.peminjaman_pengadaan_item_detail d "
                + "on d.item_punya_barcode=b.id where b.item={alias}.id and d.kembali_pengadaan_item_detail is null)"));
        else if ("DIGITAL".equals(request.getAvailability())) criteria.add(Restrictions.or(
                Restrictions.eq("bolehDiDownload", Boolean.TRUE),
                Restrictions.or(Restrictions.isNotNull("ebooksLink"), Restrictions.isNotNull("ebooksLinkPdf"))));
        return criteria;
    }

    private void applySort(Criteria criteria, String sort) {
        if ("TITLE_ASC".equals(sort)) {
            criteria.addOrder(Order.asc("nama")).addOrder(Order.desc("id"));
        } else if ("AUTHOR_ASC".equals(sort)) {
            criteria.addOrder(Order.asc("pengarangs")).addOrder(Order.asc("nama"));
        } else if ("YEAR_DESC".equals(sort)) {
            criteria.addOrder(Order.desc("tahun")).addOrder(Order.desc("id"));
        } else if ("POPULAR".equals(sort)) {
            criteria.addOrder(Order.desc("jumlahDilihat")).addOrder(Order.desc("id"));
        } else {
            criteria.addOrder(Order.desc("id"));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<Long, List<LibraryHoldingDto>> loadHoldings(Session session, List<Item> entities) {
        Map<Long, List<LibraryHoldingDto>> result = new LinkedHashMap<Long, List<LibraryHoldingDto>>();
        if (entities == null || entities.isEmpty()) return result;
        List<Long> ids = new ArrayList<Long>();
        for (Item item : entities) ids.add(item.getId());
        Query query = session.createSQLQuery(
                "select b.item,p.id,coalesce(p.nama,'Lokasi belum ditentukan'),count(b.id),"
                + "sum(case when loan.item_punya_barcode is null then 1 else 0 end),"
                + "coalesce(to_char(max(loan.due_date),'DD-MM-YYYY'),'') "
                + "from library.item_punya_barcode b left join library.perpustakaan p on p.id=b.perpustakaan "
                + "left join (select item_punya_barcode,max(batas_waktu_pengembalian) due_date "
                + "from library.peminjaman_pengadaan_item_detail where kembali_pengadaan_item_detail is null "
                + "group by item_punya_barcode) loan on loan.item_punya_barcode=b.id "
                + "where b.item in (:ids) group by b.item,p.id,p.nama order by p.nama");
        query.setParameterList("ids", ids);
        List<Object[]> rows = query.list();
        for (Object[] row : rows) {
            if (!(row[0] instanceof Number)) continue;
            Long itemId = Long.valueOf(((Number) row[0]).longValue());
            Long libraryId = row[1] instanceof Number ? Long.valueOf(((Number) row[1]).longValue()) : null;
            int total = row[3] instanceof Number ? ((Number) row[3]).intValue() : 0;
            int available = row[4] instanceof Number ? ((Number) row[4]).intValue() : 0;
            List<LibraryHoldingDto> itemHoldings = result.get(itemId);
            if (itemHoldings == null) { itemHoldings = new ArrayList<LibraryHoldingDto>(); result.put(itemId, itemHoldings); }
            itemHoldings.add(new LibraryHoldingDto(libraryId, row[2] == null ? null : String.valueOf(row[2]),
                    total, available, row[5] == null ? null : String.valueOf(row[5]), null));
        }
        return result;
    }

    private LibraryCatalogItemDto toDto(Item item, Map<Long, List<LibraryHoldingDto>> holdingMap) {
        LibraryCatalogItemDto dto = new LibraryCatalogItemDto();
        dto.setId(item.getId());
        dto.setTitle(item.getNama());
        dto.setAuthors(item.getPengarangs());
        dto.setPublisher(item.getPenerbit() == null ? null : item.getPenerbit().getNama());
        dto.setIsbn(item.getIsbn());
        dto.setIssn(item.getIssn());
        dto.setSubject(item.getKategories() == null ? item.getTema() : item.getKategories());
        dto.setImageUrl(item.getImageUrl());
        dto.setSummary(item.getAbstrak());
        dto.setLanguage(item.getBahasa());
        dto.setCallNumber(item.getCallnumber());
        dto.setYear(item.getTahun());
        List<LibraryHoldingDto> holdings = holdingMap.get(item.getId());
        int copies = 0, available = 0;
        if (holdings != null) for (LibraryHoldingDto holding : holdings) { copies += holding.getTotal(); available += holding.getAvailable(); }
        dto.setCopyCount(copies);
        dto.setAvailableCount(available);
        dto.setHoldings(holdings);
        dto.setItemType(item.getJenisItem() == null ? null : item.getJenisItem().getNama());
        dto.setMaterialType(item.getTipeItem() == null ? null : item.getTipeItem().getNama());
        String ebook = safeUrl(item.getEbooksLink());
        if (ebook == null) ebook = safeUrl(item.getEbooksLinkPdf());
        dto.setDigital(Boolean.TRUE.equals(item.getBolehDiDownload()) || ebook != null);
        dto.setDigitalUrl(ebook);
        return dto;
    }

    private String safeUrl(String value) {
        if (value == null) return null;
        value = value.trim();
        return value.startsWith("https://") || value.startsWith("http://") || value.startsWith("/") ? value : null;
    }

    @SuppressWarnings("unchecked")
    private JSONArray referenceList(Session session, Class<?> type) throws JSONException {
        Criteria criteria = session.createCriteria(type);
        if (hasProperty(session, type, "aktif")) {
            criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
        }
        criteria.addOrder(Order.asc("nama"));
        List<Object> rows = criteria.setMaxResults(250).list();
        JSONArray result = new JSONArray();
        for (Object row : rows) {
            Long id = null;
            String name = "";
            if (row instanceof Perpustakaan) {
                id = ((Perpustakaan) row).getId(); name = ((Perpustakaan) row).getNama();
            } else if (row instanceof JenisItem) {
                id = ((JenisItem) row).getId(); name = ((JenisItem) row).getNama();
            } else if (row instanceof TipeItem) {
                id = ((TipeItem) row).getId(); name = ((TipeItem) row).getNama();
            } else if (row instanceof Yayasan) {
                id = ((Yayasan) row).getId(); name = ((Yayasan) row).getNama();
            } else if (row instanceof Sekolah) {
                id = ((Sekolah) row).getId(); name = ((Sekolah) row).getNama();
            } else if (row instanceof Fakultas) {
                id = ((Fakultas) row).getId(); name = ((Fakultas) row).getNama();
            } else if (row instanceof Jurusan) {
                id = ((Jurusan) row).getId(); name = ((Jurusan) row).getNama();
            }
            result.put(new JSONObject().put("id", id).put("nama", name == null ? "" : name));
        }
        return result;
    }

    private boolean hasProperty(Session session, Class<?> type, String property) {
        ClassMetadata metadata = session.getSessionFactory().getClassMetadata(type);
        if (metadata == null) return false;
        String[] names = metadata.getPropertyNames();
        if (names == null) return false;
        for (String name : names) if (property.equals(name)) return true;
        return false;
    }
}
