package ais.action.master.library.modern;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
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
            if (item == null || Boolean.FALSE.equals(item.getAktif())) return null;
            String publisher = item.getPenerbit() == null ? null : item.getPenerbit().getNama();
            String digitalUrl = null;
            if (Boolean.TRUE.equals(item.getBolehDiDownload())) {
                LampiranLain attachment = LampiranLain.ambil(item.getId(), LampiranLain.ITEM);
                if (attachment != null) digitalUrl = safeUrl(attachment.createLinkUri());
            }
            String ebookUrl = safeUrl(item.getEbooksLink());
            List<Holding> holdings = new ArrayList<Holding>();
            List<ItemPunyaBarcode> copies = session.createCriteria(ItemPunyaBarcode.class)
                    .add(Restrictions.eq("item", item)).list();
            for (ItemPunyaBarcode copy : copies) {
                Perpustakaan library = copy.getPerpustakaan();
                holdings.add(new Holding(copy.getBarcode(), library == null ? null : library.getId(),
                        library == null ? "Lokasi tidak diketahui" : library.getNama()));
            }
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
            if (item == null || Boolean.FALSE.equals(item.getAktif())) return result;
            List<ItemPunyaBarcode> copies = session.createCriteria(ItemPunyaBarcode.class)
                    .add(Restrictions.eq("item", item)).list();
            for (ItemPunyaBarcode copy : copies) {
                Perpustakaan library = copy.getPerpustakaan();
                result.add(new Holding(copy.getBarcode(), library == null ? null : library.getId(),
                        library == null ? "Lokasi tidak diketahui" : library.getNama()));
            }
            return result;
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    public static final class Holding {
        private final String barcode; private final Long libraryId; private final String libraryName;
        private Holding(String barcode, Long libraryId, String libraryName) {
            this.barcode = barcode; this.libraryId = libraryId; this.libraryName = libraryName;
        }
        public String getBarcode() { return barcode == null ? "-" : barcode; }
        public Long getLibraryId() { return libraryId; }
        public String getLibraryName() { return libraryName; }
    }

    public static final class Detail {
        private final Long id; private final String imageUrl,title,authors,publisher,category,classification,theme,isbn,issn,edition,language,callNumber,summary,digitalUrl,ebookUrl;
        private final Integer year; private final List<Holding> holdings;
        private Detail(Long id,String imageUrl,String title,String authors,String publisher,String category,String classification,String theme,String isbn,String issn,String edition,Integer year,String language,String callNumber,String summary,String digitalUrl,String ebookUrl,List<Holding> holdings){
            this.id=id;this.imageUrl=imageUrl;this.title=title;this.authors=authors;this.publisher=publisher;this.category=category;this.classification=classification;this.theme=theme;this.isbn=isbn;this.issn=issn;this.edition=edition;this.year=year;this.language=language;this.callNumber=callNumber;this.summary=summary;this.digitalUrl=digitalUrl;this.ebookUrl=ebookUrl;this.holdings=holdings;
        }
        public Long getId(){return id;} public String getImageUrl(){return imageUrl;} public String getTitle(){return title;} public String getAuthors(){return authors;} public String getPublisher(){return publisher;} public String getCategory(){return category;} public String getClassification(){return classification;} public String getTheme(){return theme;} public String getIsbn(){return isbn;} public String getIssn(){return issn;} public String getEdition(){return edition;} public Integer getYear(){return year;} public String getLanguage(){return language;} public String getCallNumber(){return callNumber;} public String getSummary(){return summary;} public String getDigitalUrl(){return digitalUrl;} public String getEbookUrl(){return ebookUrl;} public List<Holding> getHoldings(){return holdings;}
    }

    private static String safeUrl(String value){if(value==null)return null;value=value.trim();if(value.startsWith("https://")||value.startsWith("http://")||value.startsWith("/"))return value;return null;}
}
