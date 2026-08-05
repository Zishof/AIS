package ais.action.master.spi;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.spi.ProfilRisikoSPI;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

/**
 * <h2>ProfilRisikoSPIAction &mdash; Pengendali Layar Profil Risiko Audit (Audit Universe)</h2>
 *
 * <p>
 * Pengendali ZK untuk layar tempat staf Satuan Pengawasan Internal menilai tingkat risiko setiap
 * unit kerja ({@link SatuanKerja}) pada satu tahun tertentu, menghasilkan daftar "audit universe"
 * yang menjadi dasar penyusunan {@link ais.database.model.spi.RencanaAuditTahunanSPI Rencana Audit
 * Tahunan (PKPT)} pada layar berikutnya. Lihat javadoc {@link ProfilRisikoSPI} untuk penjelasan
 * lengkap lima komponen skor risiko dan cara total/zona risiko dihitung.
 * </p>
 *
 * <h3>Pratinjau total skor langsung berubah saat mengisi formulir</h3>
 * <p>
 * Kelima kotak input skor pada formulir tambah/ubah ({@link #buildForm(ProfilRisikoSPI)}) dipasangi
 * pendengar perubahan ({@code onChange}) yang memanggil {@link #updateTotalPreview()} setiap kali
 * salah satu angka diubah &mdash; sehingga staf SPI langsung melihat total skor dan zona risiko yang
 * dihasilkan SEBELUM menekan tombol Simpan, tanpa perlu menyimpan dulu untuk mengetahui hasil
 * klasifikasinya. Ini murni bantuan visual di sisi antarmuka; perhitungan otoritatif yang
 * sesungguhnya tetap dilakukan oleh {@link ProfilRisikoSPI#getTotalSkorRisiko()} setiap kali data
 * dibaca ulang dari database (lihat javadoc kelas tersebut untuk alasan totalnya tidak disimpan).
 * </p>
 *
 * <h3>Filter zona risiko dihitung langsung di database, bukan di Java</h3>
 * <p>
 * Combobox filter zona pada kartu pencarian ({@link #searchzona}) diterjemahkan menjadi syarat SQL
 * atas jumlah kelima kolom skor ({@link #zonaRestriction()}) &mdash; BUKAN dengan mengambil semua
 * baris ke memori Java lalu menyaringnya satu per satu. Pendekatan ini penting untuk efisiensi
 * memori dan kecepatan: penyaringan tetap dikerjakan oleh database (yang memang dioptimalkan untuk
 * itu), sejalan dengan permintaan agar kode seefisien mungkin dalam penggunaan memori.
 * </p>
 *
 * <h3>Pemilih Unit Kerja memakai pencarian bertahap, bukan combobox biasa</h3>
 * <p>
 * {@link #searchsatuanKerja} dan {@link #satuanKerja} (pada formulir) memakai
 * {@link AmbilDataSatuanKerjaBanbox} &mdash; komponen pencarian pop-up yang SUDAH menjadi konvensi
 * baku di 300-an layar lain di aplikasi ini untuk memilih {@link SatuanKerja}. Versi awal layar ini
 * sempat memakai {@code Combobox} biasa yang diisi lewat {@code Common.insertComboDanSemua(...)}
 * &mdash; method itu memuat SELURUH baris {@link SatuanKerja} ke memori sekaligus, yang terbukti
 * membuat layar ini terasa sangat lambat dibuka pada instalasi dengan banyak unit kerja (dokumentasi
 * {@code Common.insertCombo}/{@code insertComboDanSemua} sendiri memperingatkan hal ini untuk
 * entitas bervolume besar). {@link AmbilDataSatuanKerjaBanbox} sebaliknya memuat data secara
 * bertahap (pohon hierarki dimuat per-level, tabel "sering dipakai" memakai paging sisi server),
 * sehingga waktu buka layar tidak lagi bergantung pada jumlah total unit kerja di database.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class ProfilRisikoSPIAction extends BaseSPIAction {

    private static final long serialVersionUID = 1L;

    // ---- Search fields ----
    private AmbilDataSatuanKerjaBanbox searchsatuanKerja;
    private MyIntbox searchtahun;
    private Combobox searchzona;

    // ---- Form fields ----
    private AmbilDataSatuanKerjaBanbox satuanKerja;
    private MyIntbox tahun;
    private MyIntbox skorMaterialitas;
    private MyIntbox skorDampakOperasional;
    private MyIntbox skorKualitasPengendalian;
    private MyIntbox skorTemuanSebelumnya;
    private MyIntbox skorLamaTidakDiaudit;
    private Textbox  catatan;
    private Label    totalPreview;

    // ---- Current entity ----
    private ProfilRisikoSPI profilRisikoSPI;

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        initPrivileges();

        searchsatuanKerja.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onSearchDefault(e);
            }
        });

        searchzona.appendChild(new Comboitem("Semua Zona"));
        for (String zona : new String[]{ProfilRisikoSPI.ZONA_TINGGI, ProfilRisikoSPI.ZONA_SEDANG, ProfilRisikoSPI.ZONA_RENDAH}) {
            Comboitem ci = new Comboitem(zona);
            ci.setValue(zona);
            searchzona.appendChild(ci);
        }
        searchzona.setSelectedIndex(0);
        searchzona.setReadonly(true);

        onSearchDefault(null);
        initPagingListener();
        appendCetakUpload(ProfilRisikoSPI.class,
                new String[]{"id", "satuanKerja", "tahun", "skorMaterialitas", "skorDampakOperasional",
                        "skorKualitasPengendalian", "skorTemuanSebelumnya", "skorLamaTidakDiaudit", "catatan", "aktif"});
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    class ProfilRisikoSPIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final ProfilRisikoSPI item = (ProfilRisikoSPI) obj;

            new Label(item.getSatuanKerja() == null ? "-" : item.getSatuanKerja().getNama()).setParent(row);
            new Label(item.getTahun() == null ? "-" : item.getTahun().toString()).setParent(row);

            Label badge = new Label(item.getTotalSkorRisiko() + " - " + item.getZonaRisiko());
            badge.setSclass("ais-badge " + zonaSclass(item.getZonaRisiko()));
            badge.setParent(row);

            final MyCheckboxConfig aktifCb = new MyCheckboxConfig("Aktif");
            aktifCb.setDisabled(!edit);
            aktifCb.setChecked(item.getAktif());
            aktifCb.setParent(row);
            row.setAttribute("checkbox", aktifCb);
            aktifCb.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    item.setAktif(aktifCb.isChecked());
                    Common.refreshSaveOrUpdate(item);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, item, ProfilRisikoSPIAction.this).setParent(row);
        }
    }

    private static String zonaSclass(String zona) {
        if (ProfilRisikoSPI.ZONA_TINGGI.equals(zona)) return "ais-badge-merah";
        if (ProfilRisikoSPI.ZONA_SEDANG.equals(zona)) return "ais-badge-kuning";
        return "ais-badge-hijau";
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        init(new ProfilRisikoSPI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        profilRisikoSPI = (ProfilRisikoSPI) obj;
        buildForm(profilRisikoSPI);
        openAddWindow();
    }

    // =====================================================================
    // Form builder
    // =====================================================================

    private void buildForm(final ProfilRisikoSPI item) {
        FormHolder fh = prepareFormWindow("Pendataan Profil Risiko Audit");
        Rows rows = fh.rows;

        Row row = addFormRow(rows, "Unit Kerja *");
        try {
            row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        satuanKerja.setWidth("90%");
        SatuanKerja satuanKerjaAwal = item.getSatuanKerja() == null
                ? (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja")
                : item.getSatuanKerja();
        if (satuanKerjaAwal != null) {
            satuanKerja.setValue(satuanKerjaAwal.getNama());
            satuanKerja.setAttribute("satuanKerja", satuanKerjaAwal);
        }

        row = addFormRow(rows, "Tahun Penilaian *");
        row.appendChild(tahun = new MyIntbox(item.getTahun()));

        final EventListener recompute = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                updateTotalPreview();
            }
        };

        row = addFormRow(rows, "Skor Materialitas (1-5) *");
        row.appendChild(skorMaterialitas = new MyIntbox(item.getSkorMaterialitas()));
        skorMaterialitas.setTooltiptext("Seberapa besar nilai anggaran/keuangan yang dikelola unit ini");
        skorMaterialitas.addEventListener("onChange", recompute);

        row = addFormRow(rows, "Skor Dampak Operasional (1-5) *");
        row.appendChild(skorDampakOperasional = new MyIntbox(item.getSkorDampakOperasional()));
        skorDampakOperasional.setTooltiptext("Seberapa besar dampak ke operasional lembaga bila unit ini bermasalah");
        skorDampakOperasional.addEventListener("onChange", recompute);

        row = addFormRow(rows, "Skor Kualitas Pengendalian (1-5) *");
        row.appendChild(skorKualitasPengendalian = new MyIntbox(item.getSkorKualitasPengendalian()));
        skorKualitasPengendalian.setTooltiptext("Semakin lemah pengendalian internal unit ini, semakin tinggi skornya");
        skorKualitasPengendalian.addEventListener("onChange", recompute);

        row = addFormRow(rows, "Skor Temuan Sebelumnya (1-5) *");
        row.appendChild(skorTemuanSebelumnya = new MyIntbox(item.getSkorTemuanSebelumnya()));
        skorTemuanSebelumnya.setTooltiptext("Seberapa banyak/berat temuan audit pada pemeriksaan sebelumnya");
        skorTemuanSebelumnya.addEventListener("onChange", recompute);

        row = addFormRow(rows, "Skor Lama Tidak Diaudit (1-5) *");
        row.appendChild(skorLamaTidakDiaudit = new MyIntbox(item.getSkorLamaTidakDiaudit()));
        skorLamaTidakDiaudit.setTooltiptext("Semakin lama sejak terakhir diaudit, semakin tinggi skornya");
        skorLamaTidakDiaudit.addEventListener("onChange", recompute);

        row = addFormRow(rows, "Total & Zona Risiko");
        row.appendChild(totalPreview = new Label());

        row = addFormRow(rows, "Catatan Penilaian");
        row.appendChild(catatan = new Textbox(item.getCatatan()));
        catatan.setWidth("90%");
        catatan.setRows(4);

        updateTotalPreview();

        finaliseFormWindow(fh, new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
    }

    /** Menghitung ulang &amp; menampilkan pratinjau total/zona risiko dari isian formulir saat ini. */
    private void updateTotalPreview() {
        int total = skorAtau1(skorMaterialitas) + skorAtau1(skorDampakOperasional) + skorAtau1(skorKualitasPengendalian)
                + skorAtau1(skorTemuanSebelumnya) + skorAtau1(skorLamaTidakDiaudit);
        String zona = total >= ProfilRisikoSPI.AMBANG_ZONA_TINGGI ? ProfilRisikoSPI.ZONA_TINGGI
                : total >= ProfilRisikoSPI.AMBANG_ZONA_SEDANG ? ProfilRisikoSPI.ZONA_SEDANG
                : ProfilRisikoSPI.ZONA_RENDAH;
        totalPreview.setValue(total + " - " + zona);
        totalPreview.setSclass("ais-badge " + zonaSclass(zona));
    }

    private static int skorAtau1(MyIntbox box) {
        try {
            Integer v = box.getValue();
            return v == null ? 1 : v;
        } catch (Exception e) {
            return 1;
        }
    }

    // =====================================================================
    // Save
    // =====================================================================

    public boolean onSave(Event event) throws Exception {
        if (satuanKerja.getAttribute("satuanKerja") == null) {
            MyMessageboxConfig.show("Mohon maaf, Unit Kerja belum dipilih."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) klik tombol pemilih Unit Kerja dan cari unit yang akan dinilai risikonya;"
                    + " (2) pastikan unit kerja sudah terdaftar di master data;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (tahun.getValue() == null || tahun.getValue() < 2000 || tahun.getValue() > 2100) {
            MyMessageboxConfig.show("Mohon maaf, Tahun Penilaian belum diisi dengan benar."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) isi kolom Tahun Penilaian dengan angka tahun yang valid (contoh: 2025);"
                    + " (2) pastikan tahun berada dalam rentang 2000 hingga 2100;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (!skorValid(skorMaterialitas) || !skorValid(skorDampakOperasional) || !skorValid(skorKualitasPengendalian)
                || !skorValid(skorTemuanSebelumnya) || !skorValid(skorLamaTidakDiaudit)) {
            MyMessageboxConfig.show("Mohon maaf, salah satu atau lebih skor risiko belum diisi dengan benar."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) pastikan semua kolom skor (Materialitas, Dampak Operasional, Kualitas Pengendalian,"
                    + " Temuan Sebelumnya, Lama Tidak Diaudit) sudah terisi;"
                    + " (2) nilai yang diperbolehkan adalah angka 1 hingga 5;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        Session session = HibernateUtil.currentSession();
        if (profilRisikoSPI.getId() != null) {
            profilRisikoSPI = (ProfilRisikoSPI) session.load(ProfilRisikoSPI.class, profilRisikoSPI.getId());
        }
        profilRisikoSPI.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
        profilRisikoSPI.setTahun(tahun.getValue());
        profilRisikoSPI.setSkorMaterialitas(skorMaterialitas.getValue());
        profilRisikoSPI.setSkorDampakOperasional(skorDampakOperasional.getValue());
        profilRisikoSPI.setSkorKualitasPengendalian(skorKualitasPengendalian.getValue());
        profilRisikoSPI.setSkorTemuanSebelumnya(skorTemuanSebelumnya.getValue());
        profilRisikoSPI.setSkorLamaTidakDiaudit(skorLamaTidakDiaudit.getValue());
        profilRisikoSPI.setCatatan(catatan.getValue());
        Common.refreshSaveOrUpdate(session, profilRisikoSPI);
        return true;
    }

    private static boolean skorValid(MyIntbox box) {
        Integer v = box.getValue();
        return v != null && v >= 1 && v <= 5;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    private Criterion zonaRestriction() {
        if (searchzona.getSelectedIndex() <= 0 || searchzona.getSelectedItem().getValue() == null) {
            return Restrictions.sqlRestriction("true");
        }
        String zona = (String) searchzona.getSelectedItem().getValue();
        String sum = "(skor_materialitas+skor_dampak_operasional+skor_kualitas_pengendalian"
                + "+skor_temuan_sebelumnya+skor_lama_tidak_diaudit)";
        if (ProfilRisikoSPI.ZONA_TINGGI.equals(zona)) {
            return Restrictions.sqlRestriction(sum + " >= " + ProfilRisikoSPI.AMBANG_ZONA_TINGGI);
        } else if (ProfilRisikoSPI.ZONA_SEDANG.equals(zona)) {
            return Restrictions.sqlRestriction(sum + " >= " + ProfilRisikoSPI.AMBANG_ZONA_SEDANG
                    + " and " + sum + " < " + ProfilRisikoSPI.AMBANG_ZONA_TINGGI);
        }
        return Restrictions.sqlRestriction(sum + " < " + ProfilRisikoSPI.AMBANG_ZONA_SEDANG);
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(ProfilRisikoSPI.class)
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchsatuanKerja.getAttribute("satuanKerja") == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("satuanKerja", searchsatuanKerja.getAttribute("satuanKerja")))
                .add(searchtahun.getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("tahun", searchtahun.getValue()))
                .add(zonaRestriction());
        if (order) criteria.addOrder(Order.desc("tahun"));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<ProfilRisikoSPI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new ProfilRisikoSPIRenderer());
    }
}
