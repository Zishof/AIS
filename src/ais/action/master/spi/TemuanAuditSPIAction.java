package ais.action.master.spi;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.spi.ChecklistAuditSPI;
import ais.database.model.spi.PenugasanAuditSPI;
import ais.database.model.spi.TemuanAuditSPI;
import ais.common.ConstantValues;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * <h2>TemuanAuditSPIAction &mdash; Pengendali Formulir Pengisian Satu Temuan (5-Unsur)</h2>
 *
 * <p>
 * Pengendali ZK yang membangun formulir popup untuk mengisi &mdash; atau melihat, bila mode
 * baca-saja &mdash; satu {@link TemuanAuditSPI}: hasil pemeriksaan satu langkah uji
 * ({@link ChecklistAuditSPI}) pada satu penugasan ({@link PenugasanAuditSPI}). Ditampilkan sebagai
 * jendela popup terpisah (bukan baris inline dalam grid checklist besar) karena struktur 5-unsur
 * temuan (Kriteria/Kondisi/Sebab/Akibat/Rekomendasi, lihat javadoc {@link TemuanAuditSPI}) berupa
 * lima ruas teks yang masing-masing bisa berisi beberapa paragraf &mdash; mustahil ditampilkan
 * dengan nyaman sebagai kotak-kotak sempit di dalam satu baris tabel. Dipanggil dari tombol "Isi
 * Temuan" pada grid checklist di {@link PenugasanAuditSPIAction#tampilRinci}.
 * </p>
 *
 * <h3>Penugasan langsung, bukan lewat pola composer {@code apply=}</h3>
 * <p>
 * Sama seperti {@link TindakLanjutAuditSPIAction}, kelas ini tidak berdiri sebagai halaman ZUL
 * tersendiri &mdash; jendela popupnya dirangkai murni lewat kode Java
 * ({@link #openForChecklist(ChecklistAuditSPI, PenugasanAuditSPI, boolean, Component)}) dan
 * ditempelkan langsung ke root halaman yang memanggilnya. Setelah temuan tersimpan (memiliki id),
 * panel tindak lanjut ({@link TindakLanjutAuditSPIAction#buildInlinePanel(TemuanAuditSPI)}) otomatis
 * disisipkan di bagian bawah jendela yang sama, sehingga auditor bisa langsung memantau realisasi
 * tindak lanjut tanpa berpindah layar.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class TemuanAuditSPIAction extends BaseSPIAction {

    private static final long serialVersionUID = 1L;

    /**
     * Membuka (atau membuat baru bila belum ada) formulir temuan untuk satu pasangan
     * checklist &times; penugasan, sebagai jendela popup modal.
     *
     * @param editable false untuk mode baca-saja (mis. setelah penugasan disetujui/ditutup)
     */
    @SuppressWarnings("unchecked")
    public static void openForChecklist(final ChecklistAuditSPI checklist, final PenugasanAuditSPI penugasan,
            final boolean editable, Component anchor) throws Exception {

        Session session = HibernateUtil.currentSession();
        TemuanAuditSPI temuanExisting = (TemuanAuditSPI) ConstantValues.simpleObject(
                session.createCriteria(TemuanAuditSPI.class)
                        .add(Restrictions.eq("checklistAuditSPI", checklist))
                        .add(Restrictions.eq("penugasanAuditSPI", penugasan))
                        .setMaxResults(1),
                TemuanAuditSPI.class);

        final TemuanAuditSPI temuan = temuanExisting != null ? temuanExisting
                : new TemuanAuditSPI(checklist, penugasan);

        MyWindow win = new MyWindow();
        win.setTitle("Temuan Audit — " + abbreviate(temuan.getChecklistSnapshot(), 60));
        win.setWidth("720px");
        win.setHeight("90%");
        win.setClosable(true);
        win.setBorder("normal");
        win.setMode("popup");
        win.setPosition("center,center");
        win.setParent(anchor.getPage().getFirstRoot());

        Borderlayout bl = new ais.ui.util.MyBorderlayout();
        bl.setParent(win);

        Center center = new Center();
        center.setParent(bl);
        ZkCompat.setFlex(center, true);

        buildForm(temuan, editable, center, win);

        win.setVisible(true);
        win.onModal();
    }

    @SuppressWarnings("unchecked")
    private static void buildForm(final TemuanAuditSPI temuan, final boolean editable,
            final Component container, final MyWindow win) {
        Common.clear(container);

        Grid grid = new Grid();
        grid.setWidth("100%");
        grid.setParent(container);
        Columns cols = new Columns();
        cols.setParent(grid);
        MyColumnConfig labelCol = new MyColumnConfig();
        labelCol.setWidth("25%");
        labelCol.setParent(cols);
        new Column().setParent(cols);

        Rows rows = new Rows();
        rows.setParent(grid);

        addReadonlyRow(rows, "Kriteria Audit", temuan.getKriteriaSnapshot());
        addReadonlyRow(rows, "Langkah Uji/Checklist", temuan.getChecklistSnapshot());

        final Textbox kondisi = addTextRow(rows, "Kondisi (fakta yang ditemukan) *", temuan.getKondisi(), editable);
        final Textbox sebab = addTextRow(rows, "Sebab (akar masalah)", temuan.getSebab(), editable);
        final Textbox akibat = addTextRow(rows, "Akibat (dampak/risiko)", temuan.getAkibat(), editable);
        final Textbox rekomendasi = addTextRow(rows, "Rekomendasi Auditor", temuan.getRekomendasi(), editable);

        MyFormRow klasRow = new MyFormRow();
        klasRow.setValign("top");
        klasRow.setParent(rows);
        klasRow.appendChild(new MyLabelConfig("Klasifikasi Temuan *"));
        final Combobox klasifikasi = new Combobox();
        klasifikasi.setWidth("90%");
        klasifikasi.setReadonly(true);
        klasRow.appendChild(klasifikasi);
        for (java.util.Map.Entry<String, String> e : TemuanAuditSPI.KLASIFIKASI_DATA.entrySet()) {
            Comboitem ci = new Comboitem(e.getValue());
            ci.setValue(e.getKey());
            klasifikasi.appendChild(ci);
        }
        Common.selectComboItem(klasifikasi, temuan.getKlasifikasi());
        klasifikasi.setDisabled(!editable);

        if (editable) {
            South south = new South();
            ZkCompat.setFlex(south, true);
            south.setParent(win);
            Toolbar toolbar = new Toolbar();
            toolbar.setParent(south);

            MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan Temuan", "/img/save.gif");
            save.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    if (kondisi.getValue().trim().isEmpty() || klasifikasi.getSelectedItem() == null) {
                        MyMessageboxConfig.show("Mohon maaf, Kondisi Temuan atau Klasifikasi Temuan belum diisi."
                                + " Langkah yang dapat dilakukan:"
                                + " (1) isi kolom Kondisi dengan uraian kondisi yang ditemukan saat audit;"
                                + " (2) pilih Klasifikasi Temuan dari daftar yang tersedia;"
                                + " (3) klik tombol Simpan Temuan kembali."
                                + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                                MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    temuan.setKondisi(kondisi.getValue().trim());
                    temuan.setSebab(sebab.getValue().trim());
                    temuan.setAkibat(akibat.getValue().trim());
                    temuan.setRekomendasi(rekomendasi.getValue().trim());
                    temuan.setKlasifikasi((String) klasifikasi.getSelectedItem().getValue());
                    Common.refreshSaveOrUpdate(temuan);

                    // Sisipkan panel tindak lanjut di bawah formulir begitu temuan tersimpan
                    if (temuan.getId() != null) {
                        TindakLanjutAuditSPIAction.buildInlinePanel(temuan).setParent(container);
                    }
                    MyMessageboxConfig.show("Temuan berhasil disimpan", "Info",
                            MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                }
            });
            save.setParent(toolbar);
        }

        // Bila temuan sudah pernah tersimpan, langsung tampilkan panel tindak lanjut di bawahnya
        if (temuan.getId() != null) {
            try {
                TindakLanjutAuditSPIAction.buildInlinePanel(temuan).setParent(container);
            } catch (Exception ignored) {
                ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) TemuanAuditSPIAction.buildForm:inlinePanel");
            }
        }
    }

    private static void addReadonlyRow(Rows rows, String label, String value) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new MyLabelConfig(label));
        org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(value == null ? "-" : value);
        lbl.setStyle("color:#475569;");
        row.appendChild(lbl);
    }

    private static Textbox addTextRow(Rows rows, String label, String value, boolean editable) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new MyLabelConfig(label));
        Textbox tb = new Textbox(value == null ? "" : value);
        tb.setWidth("90%");
        tb.setRows(3);
        tb.setDisabled(!editable);
        row.appendChild(tb);
        return tb;
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // =====================================================================
    // Standalone-controller contract (kept for BaseSPIAction consistency;
    // this class is normally used via the static entry point above)
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();
        initPagingListener();
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        // no-op: temuan diisi lewat openForChecklist(...)
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(TemuanAuditSPI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) c.addOrder(Order.asc("id"));
        return c;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        if (paging == null || grid == null) return;
        Common.initPaging(initCriteria(false), paging);
        List<TemuanAuditSPI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * paging.getActivePage())
                .list();
        refreshGridData(data, new ais.ui.util.MyRowRenderer() {
            @Override
            public void render(Row row, Object obj) throws Exception {
                TemuanAuditSPI item = (TemuanAuditSPI) obj;
                row.setValign("top");
                new org.zkoss.zul.Label(item.getChecklistSnapshot()).setParent(row);
                new org.zkoss.zul.Label(item.getKlasifikasiLabel()).setParent(row);
            }
        });
    }
}
