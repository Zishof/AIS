package ais.action.master;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Pendataan referensi Agama. Digunakan pada biodata mahasiswa dan pegawai.
 * Sinkronisasi Feeder Dikti menggunakan kolom feeder (kode numerik resmi Dikti).
 *
 * Direfaktor: extends GenericCrudAction — boilerplate CRUD + Help button
 * ditangani terpusat di base class.
 *
 * Form UI menggunakan FormBuilder dengan alternating odd/even styling.
 * Design tokens terpusat di FormBuilder — ubah sekali, berlaku ke semua form.
 */
public class AgamaAction extends GenericCrudAction<Agama> {

    private static final long serialVersionUID = -5779730267402400328L;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox kode;
    private Textbox nama;
    private Textbox keterangan;
    private Longbox feeder;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<Agama> getEntityClass() {
        return Agama.class;
    }

    @Override
    protected Agama createNewEntity() {
        return new Agama();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Agama";
    }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "keterangan", "aktif", "feeder" };
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(Agama.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            criteria.addOrder(Order.asc("nama"));
        }
        criteria.add(searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new AgamaRenderer();
    }

    // ======================== Form UI ========================

    /**
     * Membangun konten form Tambah/Ubah dengan desain card bergaya modern:
     *  - Header gradient biru (judul form kontekstual)
     *  - Baris alternating odd/even (label abu-abu kiri, value putih/abu-muda kanan)
     *  - Hint text kecil di bawah input untuk panduan cepat
     *  - Toolbar terpisah di area South dengan tombol Batal dan Simpan
     *
     * Untuk mengubah warna/padding secara global, cukup edit konstanta di FormBuilder.
     */
    @Override
    protected void buildFormContent(MyWindow window, final Agama agama) throws Exception {

        // ---- Root: Borderlayout mengisi seluruh window ----
        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();

        // ---- Center: area scrollable form ----
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        ZkCompat.setFlex(center, true);
        center.setParent(borderlayout);

        // ---- Card wrapper: rounded corner + drop shadow ----
        Div cardWrap = new Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        // ---- Form grid: plain Grid (tanpa background image MyGrid) ----
        Grid formGrid = new Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        // ---- FormBuilder: alternating odd/even rows ----
        FormBuilder fb = new FormBuilder(rows);

        kode = new Textbox(agama.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Agama", kode, "Singkatan pendek, mis. ISL  KRS  KTK  HTU  BUD");

        nama = new Textbox(agama.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Agama *", nama);

        keterangan = new Textbox(agama.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan, "Catatan tambahan bila diperlukan");

        fb.addSectionHeader("Integrasi Feeder Dikti");

        feeder = new Longbox(agama.getFeeder());
        feeder.setWidth("100%");
        fb.addRow("Kode Feeder", feeder,
                "1 = Islam "
                + "2 = Kristen "
                + "3 = Katolik "
                + "4 = Hindu "
                + "5 = Buddha "
                + "6 = Konghucu");

        // ---- South: toolbar aksi ----
        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        south.setParent(borderlayout);

        Toolbar toolbar = new Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup tanpa menyimpan");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan perubahan");
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
            PesanFormalHelper.tampilkanGagal("penyimpanan data Agama",
            		"Kolom Nama Agama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama Agama.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaDuplikat()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Agama",
            		"Nama Agama sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama agama yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        Session session = HibernateUtil.currentSession();
        Agama agama = currentEntity;
        if (agama.getId() != null) {
            agama = (Agama) session.load(Agama.class, agama.getId());
            currentEntity = agama;
        }
        agama.setKode(kode.getValue());
        agama.setNama(nama.getValue());
        agama.setKeterangan(keterangan.getValue());
        agama.setFeeder(feeder.getValue());
        Common.refreshSaveOrUpdate(session, agama);
        return true;
    }

    private boolean checkNamaDuplikat() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(Agama.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("nama", nama.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /**
     * Renderer lokal untuk layar/komponen {@link AgamaAction}. Kelas ini menerjemahkan satu item data menjadi
     * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link AgamaAction} dan dapat mengakses state kelas
     * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see AgamaAction
     */
    class AgamaRenderer extends ais.ui.util.MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final Agama agama = (Agama) arg1;
            new Label(agama.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(Agama.class, agama, agama.getNama()).setParent(arg0);
            new Label(agama.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(agama.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    agama.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(agama);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, agama, AgamaAction.this).setParent(arg0);
        }
    }

    // ======================== Help content (spesifik Agama) ========================

    @Override
    protected String getHelpContent() {
        return "<div style='font-family:sans-serif;font-size:13px;line-height:1.8;color:#1e293b;'>"
                + "<h3 style='margin-top:0;color:#1d4ed8;border-bottom:2px solid #dbeafe;padding-bottom:6px;'>"
                + "&#128218; Panduan Penggunaan &mdash; Agama</h3>"

                + "<p>Modul ini mengelola <b>data referensi agama</b> yang digunakan pada biodata "
                + "mahasiswa baru, pegawai, dan sinkronisasi sistem Feeder Dikti.</p>"

                + "<h4 style='color:#0f172a;'>&#128221; Kolom Data</h4>"
                + "<table style='border-collapse:collapse;width:100%;margin-bottom:10px;'>"
                + "<tr style='background:#f1f5f9;'>"
                + "<th style='border:1px solid #cbd5e1;padding:5px 8px;text-align:left;'>Kolom</th>"
                + "<th style='border:1px solid #cbd5e1;padding:5px 8px;text-align:left;'>Keterangan</th></tr>"
                + "<tr><td style='border:1px solid #cbd5e1;padding:5px 8px;'><b>Kode Agama</b></td>"
                + "<td style='border:1px solid #cbd5e1;padding:5px 8px;'>Singkatan pendek, mis. ISL, KRS, HTU, BUD, KHG</td></tr>"
                + "<tr><td style='border:1px solid #cbd5e1;padding:5px 8px;'><b>Nama Agama *</b></td>"
                + "<td style='border:1px solid #cbd5e1;padding:5px 8px;'>Nama lengkap agama (wajib diisi, harus unik)</td></tr>"
                + "<tr><td style='border:1px solid #cbd5e1;padding:5px 8px;'>Keterangan</td>"
                + "<td style='border:1px solid #cbd5e1;padding:5px 8px;'>Catatan tambahan bila diperlukan</td></tr>"
                + "<tr><td style='border:1px solid #cbd5e1;padding:5px 8px;'><b>Kode Feeder</b></td>"
                + "<td style='border:1px solid #cbd5e1;padding:5px 8px;'>"
                + "Kode numerik resmi Dikti: 1=Islam, 2=Kristen, 3=Katolik, 4=Hindu, 5=Buddha, 6=Konghucu</td></tr>"
                + "</table>"

                + "<h4 style='color:#0f172a;'>&#128269; Pencarian &amp; Filter</h4>"
                + "<ul style='margin-top:0;padding-left:18px;'>"
                + "<li>Cari berdasarkan nama (sebagian sudah cukup, tidak sensitif huruf besar/kecil).</li>"
                + "<li>Centang <b>Tampilkan hanya yang aktif</b> untuk menyembunyikan agama yang sudah dinonaktifkan.</li>"
                + "</ul>"

                + "<div style='background:#fff7ed;border:1px solid #fed7aa;border-radius:6px;"
                + "padding:10px 14px;margin-top:10px;'>"
                + "&#9888;&#65039; <b>Perhatian Feeder:</b> Pastikan kode <b>Feeder</b> sesuai kode resmi Dikti. "
                + "Kesalahan kode menyebabkan data mahasiswa gagal tersinkronisasi ke PDDikti.</div>"
                + "</div>";
    }
}
