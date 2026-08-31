package ais.action.master;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.JenjangDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Pendataan referensi Jenjang Pendidikan (S1, S2, D3, dll).
 * Digunakan sebagai dasar konfigurasi program studi dan jumlah semester.
 */
public class JenjangAction extends GenericCrudAction<Jenjang> {

    private static final long serialVersionUID = 3786091220301468178L;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox kode;
    private Textbox nama;
    private Intbox jumlahSemester;
    private Textbox jenjangEpsbed;
    private Textbox keterangan;
    private Textbox keteranganEn;
    private Intbox jumlahSemesterMaksimal;
    private Textbox syarat;
    private Intbox jumlahSemesterLulus;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Jenjang> getEntityClass() {
        return Jenjang.class;
    }

    @Override
    protected Jenjang createNewEntity() {
        return new Jenjang();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Jenjang";
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Jenjang.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            criteria.addOrder(Order.asc("nama"));
        }
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new JenjangRenderer();
    }

    // ======================== Form content ========================

    @Override
    protected void buildFormContent(MyWindow window, final Jenjang jenjang) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();

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

        nama = new Textbox(jenjang.getNama() == null ? "" : jenjang.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama *", nama);

        jumlahSemester = new Intbox(jenjang.getJumlahSemester());
        fb.addRow("Jumlah Semester", jumlahSemester);

        jumlahSemesterMaksimal = new Intbox(jenjang.getJumlahSemesterMaksimal());
        fb.addRow("Jumlah Semester Maksimal", jumlahSemesterMaksimal);

        jumlahSemesterLulus = new Intbox(jenjang.getJumlahSemesterLulus());
        fb.addRow("Jumlah Minimal Semester Lulus", jumlahSemesterLulus,
                "Tidak berlaku untuk mahasiswa pindahan dan alih prodi");

        kode = new Textbox(jenjang.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode", kode);

        jenjangEpsbed = new Textbox(jenjang.getJenjangEpsbed() == null ? "" : jenjang.getJenjangEpsbed());
        jenjangEpsbed.setWidth("100%");
        fb.addRow("Kode PDPT/FEEDER", jenjangEpsbed);

        syarat = new Textbox(jenjang.getSyarat());
        syarat.setWidth("100%");
        fb.addRow("Persyaratan Penerimaan", syarat);

        keterangan = new Textbox(jenjang.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(2);
        fb.addRow("Keterangan", keterangan);

        keteranganEn = new Textbox(jenjang.getKeteranganEn());
        keteranganEn.setWidth("100%");
        keteranganEn.setRows(2);
        fb.addRow("Keterangan English", keteranganEn);

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        ZkCompat.setFlex(south, true);
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

    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
            		"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaJenjang()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Jenjang",
            		"Nama Jenjang sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama jenjang yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        JenjangDao jenjangDao = DaoFactory.getInstance().getJenjangDao();
        Jenjang jenjang = currentEntity;
        if (jenjang.getId() != null) {
            jenjang = jenjangDao.load(jenjang.getId());
            currentEntity = jenjang;
        }
        jenjang.setNama(nama.getValue());
        jenjang.setJumlahSemester(jumlahSemester.getValue());
        jenjang.setJumlahSemesterMaksimal(jumlahSemesterMaksimal.getValue());
        jenjang.setJumlahSemesterLulus(jumlahSemesterLulus.getValue());
        jenjang.setKode(kode.getValue().trim());
        jenjang.setJenjangEpsbed(jenjangEpsbed.getValue());
        jenjang.setSyarat(syarat.getValue().trim());
        jenjang.setKeterangan(keterangan.getValue().trim());
        jenjang.setKeteranganEn(keteranganEn.getValue().trim());
        if (jenjang.getId() != null) {
            jenjangDao.update(jenjang);
        } else {
            jenjangDao.save(jenjang);
        }
        return true;
    }

    private boolean checkNamaJenjang() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Jenjang.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()).ignoreCase())
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /**
     * Renderer lokal untuk layar/komponen {@link JenjangAction}. Kelas ini menerjemahkan satu item data menjadi
     * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link JenjangAction} dan dapat mengakses state kelas
     * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see JenjangAction
     */
    class JenjangRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Jenjang jenjang = (Jenjang) arg1;

            RevisiHelper.createNewRevisi(Jenjang.class, jenjang, jenjang.getNama()).setParent(arg0);
            new Label(jenjang.getJumlahSemester() == null ? "" : jenjang.getJumlahSemester().toString()).setParent(arg0);
            new Label(jenjang.getJumlahSemesterMaksimal() + "").setParent(arg0);
            new Label(jenjang.getJumlahSemesterLulus() + "").setParent(arg0);
            new Label(jenjang.getKode()).setParent(arg0);
            new Label(jenjang.getJenjangEpsbed()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(jenjang.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    jenjang.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(jenjang);
                }
            });

            new Label(jenjang.getKeterangan()).setParent(arg0);
            Common.copyEditDeleteButtons(edit, delete, jenjang, JenjangAction.this).setParent(arg0);
        }
    }
}
