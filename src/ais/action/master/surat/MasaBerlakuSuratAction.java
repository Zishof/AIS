package ais.action.master.surat;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.ui.util.FormBuilder;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.surat.MasaBerlakuSurat;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD modul surat untuk {@link MasaBerlakuSurat} (referensi masa berlaku dokumen/surat
 * keluar, mis. "1 bulan"/"1 tahun"): kode, nama, rentang tanggal contoh mulai/sampai, dan
 * keterangan, dibangun di atas kerangka generik {@link GenericCrudAction}. Layar juga menyediakan
 * download/upload data massal lewat {@link Common#cetakData}/{@link Common#uploadData}.
 */
public class MasaBerlakuSuratAction extends GenericCrudAction<MasaBerlakuSurat> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox kode;
    private Textbox nama;
    private MyDatebox mulai;
    private MyDatebox sampai;
    private Textbox keterangan;

    // ======================== Abstract implementations ========================

    /** @return kelas entitas yang dikelola layar ini, {@link MasaBerlakuSurat}. */
    @Override
    protected Class<MasaBerlakuSurat> getEntityClass() { return MasaBerlakuSurat.class; }

    /** @return instance {@link MasaBerlakuSurat} kosong untuk form tambah baru. */
    @Override
    protected MasaBerlakuSurat createNewEntity() { return new MasaBerlakuSurat(); }

    /** @return judul jendela form tambah/ubah. */
    @Override
    protected String getWindowTitle() { return "Pendataan Masa Berlaku Surat"; }

    /** @return nama kolom yang disertakan pada download/upload data massal. */
    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "mulai", "sampai", "keterangan", "aktif" };
    }

    /** Menambahkan tombol cetak (download template/data) dan upload massal di sebelah tombol tambah, sesuai privilese. */
    @Override
    protected void onAfterInit(Component comp) throws Exception {
        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(MasaBerlakuSurat.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, MasaBerlakuSurat.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    /** Membentuk criteria pencarian {@link MasaBerlakuSurat} berdasarkan filter aktif dan nama (ILIKE), diurut nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(MasaBerlakuSurat.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** @return renderer baris grid untuk {@link MasaBerlakuSurat} ({@link MasaBerlakuSuratRenderer}). */
    @Override
    protected MyRowRenderer createRenderer() {
        return new MasaBerlakuSuratRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah {@link MasaBerlakuSurat}: kode, nama (wajib), tanggal mulai/sampai, dan keterangan, plus toolbar Batal/Simpan. */
    @Override
    protected void buildFormContent(MyWindow window, final MasaBerlakuSurat masaBerlakuSurat) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // Center with card
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        org.zkoss.zul.Grid formGrid = new org.zkoss.zul.Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new Textbox(masaBerlakuSurat.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Masa Berlaku", kode);

        nama = new Textbox(masaBerlakuSurat.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Masa Berlaku *", nama);

        mulai = new MyDatebox(masaBerlakuSurat.getMulai());
        fb.addRow("Mulai Masa Berlaku", mulai);

        sampai = new MyDatebox(masaBerlakuSurat.getSampai());
        fb.addRow("Sampai Masa Berlaku", sampai);

        keterangan = new Textbox(masaBerlakuSurat.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan);

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        south.setParent(borderlayout);

        org.zkoss.zul.Toolbar toolbar = new org.zkoss.zul.Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan");
        save.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        save.setParent(toolbar);
        borderlayout.setParent(window);
    }

    // ======================== Save logic ========================

    /** Memvalidasi (nama wajib diisi) dan menyimpan {@link MasaBerlakuSurat} dari nilai form saat ini. @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal. */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Nama Masa Berlaku Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama Masa Berlaku; (2) isikan nama masa berlaku surat secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        MasaBerlakuSurat entity = currentEntity;
        if (entity.getId() != null) {
            entity = (MasaBerlakuSurat) session.load(MasaBerlakuSurat.class, entity.getId());
            currentEntity = entity;
        }
        entity.setKode(kode.getValue());
        entity.setNama(nama.getValue());
        entity.setMulai(mulai.getValue());
        entity.setSampai(sampai.getValue());
        entity.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid {@link MasaBerlakuSurat}: kode, nama (dengan revisi), tanggal mulai/sampai, keterangan, checkbox aktif (autosave), dan tombol ubah/hapus. */
    class MasaBerlakuSuratRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final MasaBerlakuSurat masaBerlakuSurat = (MasaBerlakuSurat) arg1;

            new Label(masaBerlakuSurat.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(MasaBerlakuSurat.class, masaBerlakuSurat, masaBerlakuSurat.getNama()).setParent(arg0);
            new Label(masaBerlakuSurat.getMulai() == null ? ""
                    : Common.dateFormat2.get().format(masaBerlakuSurat.getMulai())).setParent(arg0);
            new Label(masaBerlakuSurat.getSampai() == null ? ""
                    : Common.dateFormat2.get().format(masaBerlakuSurat.getSampai())).setParent(arg0);
            new Label(masaBerlakuSurat.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(masaBerlakuSurat.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    masaBerlakuSurat.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(masaBerlakuSurat);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, masaBerlakuSurat, MasaBerlakuSuratAction.this).setParent(arg0);
        }
    }
}
