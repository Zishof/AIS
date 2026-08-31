package ais.action.master;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Penghasilan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Layar CRUD untuk {@link Penghasilan} (kelompok/rentang penghasilan referensi, mis. untuk survei
 * atau data sosial-ekonomi): nama, batas atas/bawah nilai, dan keterangan, dibangun di atas kerangka
 * generik {@link GenericCrudAction}. Nama penghasilan wajib unik ({@link #checkNamaPenghasilan}
 * menolak duplikat, mengecualikan entitas sendiri saat mode ubah). Layar juga menyediakan
 * download/upload data massal lewat {@link Common#cetakData}/{@link Common#uploadData} yang
 * ditempel setelah tombol tambah di {@link #onAfterInit}.
 */
public class PenghasilanAction extends GenericCrudAction<Penghasilan> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields
    private Textbox nama;
    private Textbox keterangan;
    private MyDoublebox batasAtas;
    private MyDoublebox batasBawah;

    // ======================== Abstract implementations ========================

    /** @return kelas entitas yang dikelola layar ini, {@link Penghasilan}. */
    @Override
    protected Class<Penghasilan> getEntityClass() { return Penghasilan.class; }

    /** @return instance {@link Penghasilan} kosong untuk form tambah baru. */
    @Override
    protected Penghasilan createNewEntity() { return new Penghasilan(); }

    /** @return judul jendela form tambah/ubah. */
    @Override
    protected String getWindowTitle() { return "Pendataan Penghasilan"; }

    /** @return nama kolom yang disertakan pada download/upload data massal. */
    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "nama", "batasAtas", "batasBawah", "keterangan", "aktif", "feeder" };
    }

    /** Menambahkan tombol cetak (download template/data) dan upload massal di sebelah tombol tambah, sesuai privilese. */
    @Override
    protected void onAfterInit(Component comp) throws Exception {
        String[] contents = getDownloadUploadContents();
        MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Penghasilan.class, this, contents);
        if (add != null) {
        add.getParent().appendChild(cetakToolbarbutton);
        }

        MyToolbarbuttonConfig upload = Common.uploadData(this, Penghasilan.class, contents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    /** Membentuk criteria pencarian {@link Penghasilan} berdasarkan filter aktif dan nama (ILIKE), diurut nama bila {@code order} true. */
    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Penghasilan.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) criteria.addOrder(Order.asc("nama"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    /** @return renderer baris grid untuk {@link Penghasilan} ({@link PenghasilanRenderer}). */
    @Override
    protected MyRowRenderer createRenderer() {
        return new PenghasilanRenderer();
    }

    // ======================== Form content ========================

    /** Membangun form tambah/ubah {@link Penghasilan}: nama (wajib), batas atas, batas bawah, dan keterangan, plus toolbar Batal/Simpan. */
    @Override
    protected void buildFormContent(MyWindow window, final Penghasilan penghasilan) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

        // ---- Center: scrollable card ----
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

        Rows rows = new Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        nama = new Textbox(penghasilan.getNama() == null ? "" : penghasilan.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Penghasilan *", nama);

        batasAtas = new MyDoublebox(penghasilan.getBatasAtas());
        batasAtas.setWidth("100%");
        fb.addRow("Batas Atas", batasAtas);

        batasBawah = new MyDoublebox(penghasilan.getBatasBawah());
        batasBawah.setWidth("100%");
        fb.addRow("Batas Bawah", batasBawah);

        keterangan = new Textbox(penghasilan.getKeterangan() == null ? "" : penghasilan.getKeterangan());
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

    /** Memvalidasi (nama wajib diisi dan harus unik) dan menyimpan {@link Penghasilan} dari nilai form saat ini. @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal. */
    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Penghasilan",
            		"Kolom Nama Penghasilan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama Penghasilan.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaPenghasilan()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Penghasilan",
            		"Nama Penghasilan sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama penghasilan yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Penghasilan entity = currentEntity;
        if (entity.getId() != null) {
            entity = (Penghasilan) session.load(Penghasilan.class, entity.getId());
            currentEntity = entity;
        }
        entity.setNama(nama.getValue());
        entity.setKeterangan(keterangan.getValue());
        entity.setBatasAtas(batasAtas.getValue());
        entity.setBatasBawah(batasBawah.getValue());
        Common.refreshSaveOrUpdate(session, entity);
        return true;
    }

    /** @return {@code true} bila sudah ada {@link Penghasilan} lain dengan nama yang sama persis (mengecualikan entitas yang sedang diedit). */
    public Boolean checkNamaPenghasilan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Penghasilan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /** Renderer baris grid {@link Penghasilan}: nama (dengan revisi), batas atas/bawah, keterangan, checkbox aktif (autosave), dan tombol ubah/hapus. */
    class PenghasilanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Penghasilan penghasilan = (Penghasilan) arg1;

            RevisiHelper.createNewRevisi(Penghasilan.class, penghasilan, penghasilan.getNama()).setParent(arg0);
            new Label(Common.numberFormat.get().format(penghasilan.getBatasAtas())).setParent(arg0);
            new Label(Common.numberFormat.get().format(penghasilan.getBatasBawah())).setParent(arg0);
            new Label(penghasilan.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(penghasilan.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    penghasilan.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(penghasilan);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, penghasilan, PenghasilanAction.this).setParent(arg0);
        }
    }
}
