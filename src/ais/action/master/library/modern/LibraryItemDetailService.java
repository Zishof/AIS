package ais.action.master.library.modern;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.LampiranLain;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.Perpustakaan;

/** Read model for public holdings; keeps persistence access out of JSP rendering. */
public final class LibraryItemDetailService {
    private LibraryItemDetailService() { }

    @SuppressWarnings("unchecked")
    public static Detail find(Long itemId) {
        if (itemId == null) return null;
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            Item item = (Item) session.get(Item.class, itemId);
            if (item == null || Boolean.FALSE.equals(item.getAktif()) || !isPublic(item) || !inScope(session,itemId)) return null;
            String publisher = item.getPenerbit() == null ? null : item.getPenerbit().getNama();
            String digitalUrl = null;
            if (Boolean.TRUE.equals(item.getBolehDiDownload())) {
                LampiranLain attachment = LampiranLain.ambil(item.getId(), LampiranLain.ITEM);
                if (attachment != null) digitalUrl = LibraryDigitalUrlPolicy.safe(attachment.createLinkUri());
            }
            String ebookUrl = Boolean.TRUE.equals(item.getBolehDiDownload()) ? LibraryDigitalUrlPolicy.safe(item.getEbooksLink()) : null;
            List<Holding> holdings = loadHoldings(session, itemId);
            return new Detail(item.getId(), item.getImageUrl(), item.getNama(), item.getPengarangs(), publisher,
                    item.getKategories(), item.getDeweyDecimalClass(), item.getTema(), item.getIsbn(), item.getIssn(),
                    item.getEdisi(), item.getTahun(), item.getBahasa(), item.getCallnumber(), item.getAbstrak(),
                    digitalUrl, ebookUrl, holdings);
        } catch (Exception e) {
            ais.common.Common.tampilErrorJikaAdmin(e);
            return null;
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    @SuppressWarnings("unchecked")
    public static List<Holding> holdings(Long itemId) {
        List<Holding> result = new ArrayList<Holding>();
        if (itemId == null) return result;
        Session session = null;
        try {
            session = HibernateUtil.openSession();
            Item item = (Item) session.get(Item.class, itemId);
            if (item == null || Boolean.FALSE.equals(item.getAktif()) || !isPublic(item) || !inScope(session,itemId)) return result;
            return loadHoldings(session, itemId);
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    /**
     * DTO allowlist satu eksemplar/holding item perpustakaan yang dikembalikan oleh
     * {@link LibraryItemDetailService}. State mencakup barcode, perpustakaan pemilik, ketersediaan, jatuh tempo,
     * rak, dan panjang antrean; tipe ini tidak memuat entity Hibernate atau menjalankan query sendiri.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static} dan hanya membawa snapshot respons publik. Pembentukan status
     * ketersediaan tetap dilakukan oleh service induk agar aturan sirkulasi tidak diduplikasi.</p>
     *
     * @see LibraryItemDetailService
     */
    public static final class Holding {
        private final String barcode; private final Long libraryId; private final String libraryName;
        private final boolean available; private final String dueDate; private final String shelf; private final long queue;
        private Holding(String barcode, Long libraryId, String libraryName, boolean available, String dueDate,String shelf,long queue) {
            this.barcode = barcode; this.libraryId = libraryId; this.libraryName = libraryName;
            this.available = available; this.dueDate = dueDate;this.shelf=shelf;this.queue=queue;
        }
        public String getBarcode() { return barcode == null ? "-" : barcode; }
        public Long getLibraryId() { return libraryId; }
        public String getLibraryName() { return libraryName; }
        public boolean isAvailable() { return available; }
        public String getDueDate() { return dueDate == null ? "" : dueDate; }
        public String getShelf() { return shelf == null ? "" : shelf; }
        public long getQueue() { return queue; }
    }

    /**
     * Tipe implementasi bersarang {@link Detail} milik {@link LibraryItemDetailService}. Kelas ini memberi nama
     * pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * LibraryItemDetailService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
     * dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Long id}, {@code String imageUrl},
     * {@code String title}, {@code String authors}, {@code String publisher}, {@code String category}, {@code
     * String classification}, {@code String theme}; operasi lokal: {@code getId()}, {@code getImageUrl()}, {@code
     * getTitle()}, {@code getAuthors()}, {@code getPublisher()}, {@code getCategory()}, {@code
     * getClassification()}, {@code getTheme()}, {@code getIsbn()}, {@code getIssn}(). Aturan bisnis bersama tetap
     * berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see LibraryItemDetailService
     */
    public static final class Detail {
        private final Long id; private final String imageUrl,title,authors,publisher,category,classification,theme,isbn,issn,edition,language,callNumber,summary,digitalUrl,ebookUrl;
        private final Integer year; private final List<Holding> holdings;
        private Detail(Long id,String imageUrl,String title,String authors,String publisher,String category,String classification,String theme,String isbn,String issn,String edition,Integer year,String language,String callNumber,String summary,String digitalUrl,String ebookUrl,List<Holding> holdings){
            this.id=id;this.imageUrl=imageUrl;this.title=title;this.authors=authors;this.publisher=publisher;this.category=category;this.classification=classification;this.theme=theme;this.isbn=isbn;this.issn=issn;this.edition=edition;this.year=year;this.language=language;this.callNumber=callNumber;this.summary=summary;this.digitalUrl=digitalUrl;this.ebookUrl=ebookUrl;this.holdings=holdings;
        }
        public Long getId(){return id;} public String getImageUrl(){return imageUrl;} public String getTitle(){return title;} public String getAuthors(){return authors;} public String getPublisher(){return publisher;} public String getCategory(){return category;} public String getClassification(){return classification;} public String getTheme(){return theme;} public String getIsbn(){return isbn;} public String getIssn(){return issn;} public String getEdition(){return edition;} public Integer getYear(){return year;} public String getLanguage(){return language;} public String getCallNumber(){return callNumber;} public String getSummary(){return summary;} public String getDigitalUrl(){return digitalUrl;} public String getEbookUrl(){return ebookUrl;} public List<Holding> getHoldings(){return holdings;}
    }

    private static boolean isPublic(Item item) {
        if (item == null || item.getStatusTerbitItem() == null || item.getStatusTerbitItem().getNama() == null) return false;
        String status = item.getStatusTerbitItem().getNama().trim().toLowerCase();
        return "terbit".equals(status) || "publish".equals(status) || "published".equals(status);
    }

    @SuppressWarnings("unchecked")
    private static List<Holding> loadHoldings(Session session, Long itemId) {
        List<Holding> result = new ArrayList<Holding>();
        List<Long> allowed=LibraryScopeResolver.allowedLibraryIds(session);
        if(allowed!=null&&allowed.isEmpty())return result;
        Query query = session.createSQLQuery("select b.barcode,p.id,coalesce(p.nama,'Lokasi tidak diketahui'),"
                + "case when loan.item_punya_barcode is null then true else false end,"
                + "coalesce(to_char(loan.due_date,'DD-MM-YYYY'),''),coalesce((select max(r.nama) from library.rak_detail rd join library.rak r on r.id=rd.rak where rd.item=b.item and (r.perpustakaan=p.id or r.perpustakaan is null)),''),"
                + "(select count(h.id) from library.pesanan_anggota h where h.item=b.item and (h.perpustakaan=p.id or h.perpustakaan is null) and lower(coalesce(h.status,'')) not in ('batal','selesai','diambil') and h.kadaluarsa>=current_timestamp) from library.item_punya_barcode b "
                + "left join library.perpustakaan p on p.id=b.perpustakaan left join "
                + "(select item_punya_barcode,max(batas_waktu_pengembalian) due_date from library.peminjaman_pengadaan_item_detail "
                + "where kembali_pengadaan_item_detail is null group by item_punya_barcode) loan on loan.item_punya_barcode=b.id "
                + "where b.item=:item "+(allowed==null?"":"and p.id in (:allowedLibraries) ")+"order by p.nama,b.barcode");
        query.setLong("item", itemId.longValue());
        if(allowed!=null)query.setParameterList("allowedLibraries",allowed);
        for (Object[] row : (List<Object[]>) query.list()) {
            Long libraryId = row[1] instanceof Number ? Long.valueOf(((Number) row[1]).longValue()) : null;
            result.add(new Holding(row[0] == null ? null : String.valueOf(row[0]), libraryId,
                    row[2] == null ? null : String.valueOf(row[2]), Boolean.TRUE.equals(row[3]),
                    row[4] == null ? null : String.valueOf(row[4]),row[5] == null ? null : String.valueOf(row[5]),row[6] instanceof Number?((Number)row[6]).longValue():0L));
        }
        return result;
    }

    private static boolean inScope(Session session,Long itemId){LibraryCatalogSearchRequest request=new LibraryCatalogSearchRequest();return ((Number)new LibraryCatalogSearchService().createCriteria(session,request).add(Restrictions.eq("id",itemId)).setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult()).longValue()>0L;}
}
