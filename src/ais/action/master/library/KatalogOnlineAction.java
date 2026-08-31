package ais.action.master.library;

import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.library.helper.TampilanHasilScanPerHalamanWindow;
import ais.action.master.library.modern.LibraryCatalogItemDto;
import ais.action.master.library.modern.LibraryCatalogSearchRequest;
import ais.action.master.library.modern.LibraryCatalogSearchResult;
import ais.action.master.library.modern.LibraryCatalogSearchService;
import ais.action.master.library.util.LibraryUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Item;
import ais.database.model.library.JenisItem;
import ais.database.model.library.TipeItem;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/** Modern ZK catalog backed by the same typed service as the JSP OPAC. */
public class KatalogOnlineAction extends GenericAutowireComposer {
    private static final long serialVersionUID = -5779730267402400328L;

    protected MyWindow addWindow;
    protected Paging paging;
    protected MyGrid grid;
    protected Textbox searchbahasa;
    protected Textbox searchisbn;
    protected Textbox searchissn;
    protected Textbox searchnama;
    protected Textbox searchtema;
    protected Textbox searchedisi;
    protected Textbox searchpengarang;
    protected Textbox searchcatatan;
    protected Textbox searchpenerbit;
    protected Combobox searchjenisItem;
    protected Combobox searchtipeItem;

    private final LibraryCatalogSearchService catalogService = new LibraryCatalogSearchService();

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        Common.insertCombo(searchtipeItem, "nama", TipeItem.class, "font-size:16px;");
        Common.insertCombo(searchjenisItem, "nama", JenisItem.class, "font-size:16px;");
        paging.setPageSize(Math.min(Common.ROWS_COUNT_ON_PAGE, LibraryCatalogSearchRequest.MAX_PAGE_SIZE));
        paging.addEventListener("onPaging", new EventListener() {
            public void onEvent(Event event) throws Exception { onSearchDefault(event); }
        });
        onSearchDefault(null);
    }

    /**
     * Renderer lokal untuk layar/komponen {@link KatalogOnlineAction}. Kelas ini menerjemahkan satu item data
     * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link KatalogOnlineAction} dan dapat mengakses state
     * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see KatalogOnlineAction
     */
    class ItemRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object value) throws Exception {
            row.setValign("top");
            row.setSclass("library-zk-result-row");
            final LibraryCatalogItemDto item = (LibraryCatalogItemDto) value;

            if (hasText(item.getImageUrl())) {
                Image cover = new Image(item.getImageUrl());
                cover.setWidth("112px"); cover.setHeight("152px");
                cover.setStyle("object-fit:cover;border-radius:10px;"); cover.setParent(row);
            } else {
                int hue = (int) ((item.getId() == null ? 1L : item.getId().longValue()) * 47L % 360L);
                String mark = coverMark(item.getTitle());
                new ais.ui.util.MyHtml("<div title='Sampul dibuat otomatis untuk " + html(item.getTitle())
                        + "' style='width:92px;height:132px;margin:8px;border-radius:10px;padding:12px;"
                        + "background:linear-gradient(150deg,hsl(" + hue + ",72%,88%),hsl(" + hue
                        + ",55%,58%));color:hsl(" + hue + ",70%,22%);font-weight:bold;box-shadow:0 8px 18px rgba(30,64,175,.13)'>"
                        + mark + "<div style='margin-top:55px;font-size:10px'>"
                        + html(item.getYear() == null ? "KOLEKSI" : item.getYear().toString()) + "</div></div>").setParent(row);
            }

            String metadata = "<div style='padding:6px 10px'>"
                    + "<div style='color:#0f766e;font-size:11px;font-weight:bold;text-transform:uppercase'>" + html(item.getSubject()) + "</div>"
                    + "<h3 style='color:#10233c;margin:5px 0 8px'>" + html(item.getTitle()) + "</h3>"
                    + "<div style='color:#64748b;line-height:1.7'>Penulis: " + html(orDash(item.getAuthors()))
                    + "<br/>Penerbit: " + html(orDash(item.getPublisher()))
                    + "<br/>Tahun: " + html(item.getYear() == null ? "-" : item.getYear().toString())
                    + " · Bahasa: " + html(orDash(item.getLanguage()))
                    + "<br/>Edisi: " + html(orDash(item.getEdition())) + " · Halaman: "
                    + html(item.getPages() == null || item.getPages().intValue() <= 0 ? "-" : item.getPages().toString()) + "</div>"
                    + "<div style='margin-top:8px;color:" + (item.getAvailableCount() > 0 ? "#047857" : "#b45309") + ";font-weight:bold'>● "
                    + item.getAvailableCount() + " dari " + item.getCopyCount() + " tersedia</div></div>";
            new ais.ui.util.MyHtml(metadata).setParent(row);

            String summary = "<div style='padding:8px;color:#44566c;line-height:1.65'><b>" + html(orDash(item.getItemType()))
                    + " · " + html(orDash(item.getMaterialType())) + "</b><br/><br/><b>Ringkasan</b><br/>"
                    + html(orDash(item.getSummary())) + "<br/><br/><b>Nomor panggil / ISBN</b><br/>"
                    + html(hasText(item.getCallNumber()) ? item.getCallNumber() : orDash(item.getIsbn())) + "</div>";
            new ais.ui.util.MyHtml(summary).setParent(row);

            Hbox toolbar = new Hbox();
            toolbar.setSpacing("10px");
            MyToolbarbuttonConfig google = new MyToolbarbuttonConfig("", "/img/Google-icon-big.png");
            google.setTooltiptext("Lihat isi buku melalui Google");
            google.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    LibraryUtil.laporanHTML(item.getIsbn(), item.getTitle());
                }
            });
            google.setParent(toolbar);

            MyToolbarbuttonConfig reader = new MyToolbarbuttonConfig("", "/img/Books-icon-big.png");
            reader.setTooltiptext("Lihat isi buku yang tersimpan");
            reader.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    try {
                        Session db = HibernateUtil.currentSession();
                        Item entity = (Item) db.get(Item.class, item.getId());
                        if (entity == null) return;
                        TampilanHasilScanPerHalamanWindow window = new TampilanHasilScanPerHalamanWindow("Isi Buku", "none", true);
                        page.getFirstRoot().appendChild(window);
                        window.init(entity);
                        window.onModal();
                    } catch (Exception error) {
                        Common.tampilErrorJikaAdmin(error);
                    }
                }
            });
            reader.setParent(toolbar);

            if (item.isDigital() && hasText(item.getDigitalUrl())) {
                MyToolbarbuttonConfig digital = new MyToolbarbuttonConfig("Baca Online", "/img/svg/book.svg");
                digital.setTooltiptext("Buka reader koleksi digital sesuai policy akses");
                digital.setHref(Common.ROOT + "/pustaka?s=reader&id=" + item.getId());
                digital.setTarget("_blank");
                digital.setParent(toolbar);
            }
            ais.ui.util.MenuAksiBaris.pasang(toolbar);
            toolbar.setParent(row);
        }
    }

    public void onSearchDefault(Event event) {
        if (searchnama == null) return;
        LibraryCatalogSearchRequest request = new LibraryCatalogSearchRequest();
        request.setTitle(searchnama.getValue());
        request.setIsbn(searchisbn.getValue());
        request.setIssn(searchissn.getValue());
        request.setAuthor(searchpengarang.getValue());
        request.setPublisher(searchpenerbit.getValue());
        request.setLanguage(searchbahasa.getValue());
        request.setEdition(searchedisi.getValue());
        request.setNotes(searchcatatan.getValue());
        if (searchjenisItem.getSelectedItem() != null) {
            JenisItem selected = (JenisItem) searchjenisItem.getSelectedItem().getValue();
            request.setItemTypeId(selected == null ? null : selected.getId());
        }
        if (searchtipeItem.getSelectedItem() != null) {
            TipeItem selected = (TipeItem) searchtipeItem.getSelectedItem().getValue();
            request.setMaterialTypeId(selected == null ? null : selected.getId());
        }
        request.setPage(paging == null ? 1 : paging.getActivePage() + 1);
        request.setPageSize(paging == null ? Common.ROWS_COUNT_ON_PAGE : paging.getPageSize());

        LibraryCatalogSearchResult result = catalogService.search(request);
        if (paging != null) paging.setTotalSize((int) Math.min(Integer.MAX_VALUE, result.getTotal()));
        List<LibraryCatalogItemDto> items = result.getItems();
        ListModel model = new SimpleListModel(items);
        grid.setRowRenderer(new ItemRenderer());
        grid.setModelCheckMobile(model);
    }

    private static boolean hasText(String value) { return value != null && value.trim().length() > 0; }
    private static String coverMark(String value) {
        if (!hasText(value)) return "KOLEKSI";
        String[] words = value.trim().split("\\s+"); StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length && i < 3; i++) {
            if (i > 0) result.append("<br/>");
            String word = words[i].toUpperCase(); result.append(html(word.length() > 9 ? word.substring(0, 9) : word));
        }
        return result.toString();
    }
    private static String orDash(String value) { return hasText(value) ? value : "-"; }
    private static String html(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
