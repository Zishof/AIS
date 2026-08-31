package ais.action.master.koperasi;

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
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.MejaKantin;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Pendataan Meja Kantin + QR Code pemesanan.
 *
 * Konversi dari tampilan JSP {@code modul/kantin/meja.jsp} ke ZK.
 * Memakai {@link GenericCrudAction} sehingga boilerplate CRUD (paging, privilege,
 * Help, Download/Upload, popup Pencarian Lebih Lanjut, spanduk pengantar) ditangani
 * terpusat di base class — maintenance jauh lebih mudah.
 *
 * Tiap meja punya kode unik yang dirender menjadi QR Code (reuse
 * {@link BarcodeCommon#generateBarcodeKotakAImage(String)}). Member memindai QR
 * tersebut pada halaman belanja untuk menandai meja tempat pesanan diantar.
 */
public class MejaKantinAction extends GenericCrudAction<MejaKantin> {

    private static final long serialVersionUID = 1L;

    // Form fields — direset tiap buildFormContent dipanggil
    private org.zkoss.zul.Textbox kode;
    private org.zkoss.zul.Textbox nama;
    private org.zkoss.zul.Textbox keterangan;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<MejaKantin> getEntityClass() {
        return MejaKantin.class;
    }

    @Override
    protected MejaKantin createNewEntity() {
        return new MejaKantin();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Meja Kantin";
    }

    @Override
    protected String getIntroTitle() {
        return "Meja Kantin & QR Pemesanan";
    }

    @Override
    protected String getIntroDescription() {
        return "Daftar meja beserta QR Code-nya. Member cukup memindai QR yang menempel di meja "
                + "saat memesan, sehingga petugas tahu pesanan harus diantar ke meja mana.";
    }

    @Override
    protected String[] getDownloadUploadContents() {
        return new String[] { "id", "kode", "nama", "keterangan", "aktif" };
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(MejaKantin.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            criteria.addOrder(Order.asc("nama"));
        }
        // Satu kotak pencarian mencari nama meja maupun kodenya sekaligus.
        if (!searchnama.getValue().trim().isEmpty()) {
            String kata = searchnama.getValue().trim();
            criteria.add(Restrictions.or(
                    Restrictions.ilike("nama", kata, MatchMode.ANYWHERE),
                    Restrictions.ilike("kode", kata, MatchMode.ANYWHERE)));
        }
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new MejaKantinRenderer();
    }

    // ======================== Form UI ========================

    @Override
    protected void buildFormContent(MyWindow window, final MejaKantin meja) throws Exception {

        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();

        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        ZkCompat.setFlex(center, true);
        center.setParent(borderlayout);

        Div cardWrap = new Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);

        Grid formGrid = new Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new org.zkoss.zul.Textbox(meja.getKode());
        kode.setWidth("100%");
        fb.addRow("Kode Meja *", kode, "Kode unik yang dijadikan QR Code meja, mis. MJ-01");

        nama = new org.zkoss.zul.Textbox(meja.getNama());
        nama.setWidth("100%");
        fb.addRow("Nama Meja *", nama, "Nama/nomor meja yang mudah dikenali, mis. Meja 1");

        keterangan = new org.zkoss.zul.Textbox(meja.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan, "Catatan tambahan bila diperlukan, mis. lokasi/lantai");

        // Pratinjau QR hanya untuk data yang sudah punya kode (mode Ubah).
        if (meja.getKode() != null) {
            fb.addSectionHeader("QR Code Meja");
            Image qrPrev = new Image();
            try {
                qrPrev.setContent(BarcodeCommon.generateQrAImage(meja.getKode()));
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/MejaKantinAction.java:157");
                /* abaikan bila gagal membuat QR */
            }
            qrPrev.setWidth("130px");
            qrPrev.setHeight("130px");
            qrPrev.setStyle("border:1px solid #e2e8f0;border-radius:10px;padding:4px;background:#fff;");
            fb.addRow("Pratinjau QR", qrPrev, "Cetak/tempel QR ini di meja agar dapat dipindai member.");
        }

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
        if (kode.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, kode meja belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Meja dengan kode unik; (2) ulangi penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, nama meja belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Meja; (2) ulangi penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (checkKodeDuplikat()) {
            MyMessageboxConfig.show("Mohon maaf, kode meja sudah dipakai meja lain. Langkah yang dapat dilakukan: (1) gunakan kode yang unik dan belum terpakai; (2) periksa daftar meja yang ada; (3) ulangi penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        MejaKantin meja = currentEntity;
        if (meja.getId() != null) {
            meja = (MejaKantin) session.load(MejaKantin.class, meja.getId());
            currentEntity = meja;
        }
        meja.setKode(kode.getValue());
        meja.setNama(nama.getValue());
        meja.setKeterangan(keterangan.getValue());
        Common.refreshSaveOrUpdate(session, meja);
        return true;
    }

    private boolean checkKodeDuplikat() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(MejaKantin.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("kode", kode.getValue().trim()))
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    /**
     * Renderer lokal untuk layar/komponen {@link MejaKantinAction}. Kelas ini menerjemahkan satu item data menjadi
     * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
     *
     * <p><b>Scope:</b> setiap instance terikat pada instance {@link MejaKantinAction} dan dapat mengakses state
     * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
     * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
     * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
     * renderer/listener ini.</p>
     *
     * @see MejaKantinAction
     */
    class MejaKantinRenderer extends ais.ui.util.MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final MejaKantin meja = (MejaKantin) arg1;

            // Kolom QR — kode meja yang siap dipindai.
            if (meja.getKode() != null) {
                Image qr = new Image();
                try {
                    qr.setContent(BarcodeCommon.generateQrAImage(meja.getKode()));
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/koperasi/MejaKantinAction.java:258");
                    /* abaikan bila gagal */
                }
                qr.setWidth("46px");
                qr.setHeight("46px");
                qr.setStyle("border:1px solid #e2e8f0;border-radius:8px;padding:2px;background:#fff;");
                qr.setParent(arg0);
            } else {
                new Label("-").setParent(arg0);
            }

            new Label(meja.getKode()).setParent(arg0);
            RevisiHelper.createNewRevisi(MejaKantin.class, meja, meja.getNama()).setParent(arg0);
            new Label(meja.getKeterangan()).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(meja.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    meja.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(meja);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, meja, MejaKantinAction.this).setParent(arg0);
        }
    }

    // ======================== Help content (spesifik Meja Kantin) ========================

    @Override
    protected String getHelpContent() {
        return "<div style='font-family:sans-serif;font-size:13px;line-height:1.8;color:#1e293b;'>"
                + "<h3 style='margin-top:0;color:#1d4ed8;border-bottom:2px solid #dbeafe;padding-bottom:6px;'>"
                + "&#129681; Panduan Penggunaan &mdash; Meja Kantin</h3>"

                + "<p>Modul ini mengelola <b>daftar meja</b> di kantin. Setiap meja memiliki "
                + "<b>QR Code</b> yang dipindai member saat memesan, agar pesanan langsung "
                + "terhubung ke meja yang benar.</p>"

                + "<h4 style='color:#0f172a;'>&#128221; Kolom Data</h4>"
                + "<table style='border-collapse:collapse;width:100%;margin-bottom:10px;'>"
                + "<tr style='background:#f1f5f9;'>"
                + "<th style='border:1px solid #cbd5e1;padding:5px 8px;text-align:left;'>Kolom</th>"
                + "<th style='border:1px solid #cbd5e1;padding:5px 8px;text-align:left;'>Keterangan</th></tr>"
                + "<tr><td style='border:1px solid #cbd5e1;padding:5px 8px;'><b>Kode Meja *</b></td>"
                + "<td style='border:1px solid #cbd5e1;padding:5px 8px;'>Kode unik, dijadikan isi QR Code (mis. MJ-01). Wajib & tidak boleh sama.</td></tr>"
                + "<tr><td style='border:1px solid #cbd5e1;padding:5px 8px;'><b>Nama Meja *</b></td>"
                + "<td style='border:1px solid #cbd5e1;padding:5px 8px;'>Nama/nomor meja yang mudah dikenali (mis. Meja 1).</td></tr>"
                + "<tr><td style='border:1px solid #cbd5e1;padding:5px 8px;'>Keterangan</td>"
                + "<td style='border:1px solid #cbd5e1;padding:5px 8px;'>Catatan tambahan, mis. lokasi atau lantai.</td></tr>"
                + "</table>"

                + "<h4 style='color:#0f172a;'>&#128424;&#65039; Mencetak QR</h4>"
                + "<ul style='margin-top:0;padding-left:18px;'>"
                + "<li>Buka data meja (klik Edit) untuk melihat <b>Pratinjau QR</b>.</li>"
                + "<li>Cetak lalu tempel QR di meja yang bersangkutan.</li>"
                + "<li>QR pada daftar juga dapat di-zoom untuk dipindai langsung dari layar.</li>"
                + "</ul>"

                + "<div style='background:#f0f9ff;border:1px solid #bae6fd;border-radius:6px;"
                + "padding:10px 14px;margin-top:10px;'>"
                + "&#128161; <b>Tips:</b> Gunakan <b>Unduh Excel</b> untuk mencetak daftar meja, dan "
                + "<b>Unggah Excel</b> untuk menambah banyak meja sekaligus.</div>"
                + "</div>";
    }
}
