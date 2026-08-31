package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.DosenAction;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.MengajarDiPerguruanTinggiLain;
import ais.database.model.Perkuliahan;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;

/**
 * Laporan borang akreditasi BAN-PT butir A-4.3.3 (profil beban kerja/ekivalensi mengajar dosen
 * tetap ber-NIDN): untuk setiap {@link Dosen} tetap prodi terpilih, menghitung jumlah kelas yang
 * diampu di prodi sendiri, di prodi lain dalam institusi, mengajar di perguruan tinggi lain
 * ({@link MengajarDiPerguruanTinggiLain}), SKS penelitian dan pengabdian yang disetujui
 * ({@link PengajuanPenelitianDanPengabdian#DISETUJUI}), serta ekivalensi SKS jabatan struktural
 * (di institusi sendiri dan di PT lain), difilter fakultas/jurusan, tahun akademik, dan semester.
 *
 * <p>
 * Perhitungan berat berjalan di thread terpisah (progres ditampilkan lewat {@link Label}), hasilnya
 * dirender ke worksheet {@link #sheetCode} ("A-4.3.3") lewat {@link SaptoUtil#displayWorksheet}.
 * Klik pada baris data memicu {@link DosenAction#cetakDRHDosen} untuk dosen baris tersebut (offset
 * baris tetap 11, mengikuti tata letak template worksheet).
 * </p>
 */
public class LaporanProfileDosen_A_4_3_3 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.3.3";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;
    private Combobox semester;

    /** Konstruktor default: menyiapkan filter fakultas/jurusan, tahun akademik (default periode berjalan), dan combo semester, lalu membangun kerangka jendela. */
    public LaporanProfileDosen_A_4_3_3() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildSemesterCombobox();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Konstruktor dengan judul/border/closable eksplisit; kegagalan inisialisasi dilempar ke pemanggil. */
    public LaporanProfileDosen_A_4_3_3(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildSemesterCombobox();
        buildBase(false);
    }

    /** Membangun combo semester (Genap/Ganjil/Semua, default "Semua"). */
    private void buildSemesterCombobox() {
        semester = new Combobox();
        org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem();
        ci.setLabel(Perkuliahan.GENAP); ci.setValue(Perkuliahan.GENAP);
        semester.appendChild(ci);
        ci = new ais.ui.util.MyComboitemConfig();
        ci.setLabel(Perkuliahan.GANJIL); ci.setValue(Perkuliahan.GANJIL);
        semester.appendChild(ci);
        ci = new ais.ui.util.MyComboitemConfig();
        ci.setLabel("Semua"); ci.setValue(null);
        semester.appendChild(ci);
        semester.setSelectedItem(ci);
        semester.setWidth("90%");
        semester.setReadonly(true);
    }

    /** @return kode sheet template worksheet borang, {@link #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter fakultas/jurusan standar, tahun akademik (wajib), dan semester ke baris toolbar filter. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);

        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        row.appendChild(tahunAjaran);

        row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
        row.appendChild(semester);
    }

    /**
     * Handler tombol cetak: mengambil seluruh {@link Dosen} tetap ber-NIDN pada jurusan terpilih,
     * lalu di thread terpisah menghitung baris data per dosen (lihat dokumentasi kelas untuk rincian
     * komponen yang dijumlahkan) dan menampilkannya lewat {@link SaptoUtil#displayWorksheet}. Klik
     * sel data memicu {@link DosenAction#cetakDRHDosen} untuk dosen baris yang diklik.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null
                : (Jurusan) jurusan.getSelectedItem().getValue();

            org.hibernate.Session sess = HibernateUtil.currentNativeSession();
            final List<Dosen> dosens = sess.createCriteria(Dosen.class)
                .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                .add(Restrictions.eq("tetap", 1)).addOrder(Order.asc("nama"))
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("nidn")).add(Restrictions.ne("nidn", "")).list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 11; i++) datas.add(new ArrayList());

                    org.hibernate.Session session = HibernateUtil.currentNativeSession();

                    for (int rowIndex = 1; rowIndex <= dosens.size(); rowIndex++) {
                        Dosen dosen = dosens.get(rowIndex - 1);

                        Criterion criterion = dosen == null ? Restrictions.sqlRestriction("false")
                            : Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
                        criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

                        int jumlahProdiYgSama = ((Number) session.createCriteria(Perkuliahan.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(criterion).add(Restrictions.isNull("perkuliahan_paralel"))
                            .add(tahunAjaran.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
                            .add(semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("ganjilGenap", semester.getSelectedItem().getValue()))
                            .add(Restrictions.eq("jurusan", dosen.getJurusan()))
                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();

                        int jumlahProdiYgBeda = ((Number) session.createCriteria(Perkuliahan.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(criterion).add(Restrictions.isNull("perkuliahan_paralel"))
                            .add(tahunAjaran.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
                            .add(semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("ganjilGenap", semester.getSelectedItem().getValue()))
                            .add(Restrictions.ne("jurusan", dosen.getJurusan()))
                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();

                        int jumlahPtYgBeda = ((Number) session.createCriteria(MengajarDiPerguruanTinggiLain.class)
                            .add(tahunAjaran.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("tahunAkademik", tahunAjaran.getSelectedItem().getValue()))
                            .add(semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("semester", semester.getSelectedItem().getValue()))
                            .add(Restrictions.eq("dosen", dosen))
                            .setProjection(Projections.rowCount()).uniqueResult()).intValue();

                        Number jumlahPenelitian = (Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
                            .add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI))
                            .createAlias("tbmuser","tbmuser").createAlias("penelitianDanPengabdian","penelitianDanPengabdian")
                            .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", ConstantValues.PENELITIAN))
                            .add(tahunAjaran.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("penelitianDanPengabdian.tahunAkademik", tahunAjaran.getSelectedItem().getValue()))
                            .add(semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("penelitianDanPengabdian.semester", semester.getSelectedItem().getValue()))
                            .add(Restrictions.eq("tbmuser.dosen", dosen))
                            .setProjection(Projections.sum("penelitianDanPengabdian.sks")).uniqueResult();

                        Number jumlahPengabdian = (Number) session.createCriteria(PengajuanPenelitianDanPengabdian.class)
                            .add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI))
                            .createAlias("tbmuser","tbmuser").createAlias("penelitianDanPengabdian","penelitianDanPengabdian")
                            .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", ConstantValues.PENGABDIAN))
                            .add(tahunAjaran.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("penelitianDanPengabdian.tahunAkademik", tahunAjaran.getSelectedItem().getValue()))
                            .add(semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null
                                ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("penelitianDanPengabdian.semester", semester.getSelectedItem().getValue()))
                            .add(Restrictions.eq("tbmuser.dosen", dosen))
                            .setProjection(Projections.sum("penelitianDanPengabdian.sks")).uniqueResult();

                        int jumlah = jumlahProdiYgSama + jumlahProdiYgBeda + jumlahPtYgBeda
                            + (jumlahPenelitian == null ? 0 : jumlahPenelitian.intValue())
                            + (jumlahPengabdian == null ? 0 : jumlahPengabdian.intValue())
                            + (dosen.getSpesifikasiJabatan() == null ? 0 : dosen.getSpesifikasiJabatan().getEq_sks())
                            + (dosen.getSpesifikasiJabatanPtLain() == null ? 0 : dosen.getSpesifikasiJabatanPtLain().getEq_sks());

                        List sub = new ArrayList();
                        sub.add(""); sub.add(rowIndex); sub.add(dosen.getNama());
                        sub.add(jumlahProdiYgSama); sub.add(jumlahProdiYgBeda); sub.add(jumlahPtYgBeda);
                        sub.add(jumlahPenelitian == null ? 0 : jumlahPenelitian.intValue());
                        sub.add(jumlahPengabdian == null ? 0 : jumlahPengabdian.intValue());
                        sub.add(dosen.getSpesifikasiJabatan() == null ? 0 : dosen.getSpesifikasiJabatan().getEq_sks());
                        sub.add(dosen.getSpesifikasiJabatanPtLain() == null ? 0 : dosen.getSpesifikasiJabatanPtLain().getEq_sks());
                        sub.add(jumlah);
                        datas.add(sub);
                    }
                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList()); datas.add(new ArrayList());
                    for (int i = 0; i <= 25; i++) datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                                	} finally {
                		ais.database.hibernate.HibernateUtil.closeSession();
                	}
                }
            }).start();

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        CellMouseEvent ev = (CellMouseEvent) arg0;
                        int y = ev.getRow() - 11;
                        DosenAction.cetakDRHDosen(dosens.get(y));
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileDosen_A_4_3_3.java:216"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
