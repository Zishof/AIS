package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.DosenAction;
import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Jurusan;
import ais.database.model.RiwayatPendidikanDosen;

/**
 * Laporan SAPTO/borang akreditasi BAN-PT sheet dasar "DOSEN": rekap profil pendidikan seluruh
 * dosen tetap aktif (memiliki NIDN), difilter opsional per fakultas/jurusan. Untuk setiap dosen
 * ditampilkan nama, NIDN, tanggal lahir, jabatan fungsional, serta riwayat pendidikan S1/S2/S3
 * masing-masing (gelar akademik, nama sekolah/kampus asal, bidang ilmu) yang diambil dari
 * {@link RiwayatPendidikanDosen}. Data dimuat asinkron di thread terpisah lalu dirender lewat
 * {@link SaptoUtil#displayWorksheet}; mengklik baris dosen memicu cetak Daftar Riwayat Hidup (DRH)
 * dosen tersebut lewat {@link DosenAction#cetakDRHDosen}. Berbeda dari kelas {@code Laporan*_A_X_Y}
 * lain di paket ini, sheet ini tidak terikat satu butir borang tertentu (kode sheet {@code "DOSEN"}
 * dipakai sebagai sumber profil dasar yang dirujuk laporan-laporan A.4.x lain).
 */
public class LaporanProfileDosen extends SaptoBaseWindow {

    public static final String sheetCode = "DOSEN";
    private static final long serialVersionUID = 3331244819198611604L;
    /** Membangun jendela laporan dengan filter fakultas/jurusan siap pakai. */
    public LaporanProfileDosen() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun jendela laporan dengan judul/border/closable kustom. */
    public LaporanProfileDosen(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    /** @return kode sheet borang {@code "DOSEN"}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Membangun baris filter fakultas/jurusan lewat {@link #addFakultasJurusanFilter}. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /**
     * Menghitung dan menampilkan rekap profil pendidikan dosen aktif ber-NIDN, difilter jurusan
     * bila dipilih. Pengambilan riwayat pendidikan S1/S2/S3 tiap dosen dijalankan asinkron di
     * thread terpisah; hasil dirender lewat {@link SaptoUtil#displayWorksheet}. Mengklik baris
     * dosen mencetak DRH dosen tersebut.
     *
     * @param event event pemicu (perubahan filter), boleh {@code null}
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null
                : (Jurusan) jurusan.getSelectedItem().getValue();

            Session session = HibernateUtil.currentNativeSession();
            final List<Dosen> dosens = session.createCriteria(Dosen.class)
                .addOrder(Order.asc("nama"))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("nidn")).add(Restrictions.ne("nidn", "")).list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 6; i++) datas.add(new ArrayList());

                    Session session = HibernateUtil.currentNativeSession();
                    for (int rowIndex = 1; rowIndex <= dosens.size(); rowIndex++) {
                        Dosen dosen = dosens.get(rowIndex - 1);
                        RiwayatPendidikanDosen s1 = (RiwayatPendidikanDosen) session
                            .createCriteria(RiwayatPendidikanDosen.class).add(Restrictions.eq("dosen", dosen))
                            .add(Restrictions.eq("jenjangPendidikan", ConstantValues.s1)).setMaxResults(1).uniqueResult();
                        RiwayatPendidikanDosen s2 = (RiwayatPendidikanDosen) session
                            .createCriteria(RiwayatPendidikanDosen.class).add(Restrictions.eq("dosen", dosen))
                            .add(Restrictions.eq("jenjangPendidikan", ConstantValues.s2)).setMaxResults(1).uniqueResult();
                        RiwayatPendidikanDosen s3 = (RiwayatPendidikanDosen) session
                            .createCriteria(RiwayatPendidikanDosen.class).add(Restrictions.eq("dosen", dosen))
                            .add(Restrictions.eq("jenjangPendidikan", ConstantValues.s3)).setMaxResults(1).uniqueResult();

                        List sub = new ArrayList();
                        sub.add(rowIndex);
                        sub.add(dosen.getNama());
                        sub.add(dosen.getNidn());
                        sub.add(dosen.getTanggallahir() == null ? "" : Common.dateFormat112.get().format(dosen.getTanggallahir()));
                        sub.add(dosen.getJabatanFungsionalDosen() == null ? "" : dosen.getJabatanFungsionalDosen().getNama());
                        sub.add(s1 == null ? "" : s1.getGelarAkademik());
                        sub.add(s1 == null ? "" : s1.getNamaSekolah());
                        sub.add(s1 == null ? "" : s1.getBidangIlmu());
                        sub.add(s2 == null ? "" : s2.getGelarAkademik());
                        sub.add(s2 == null ? "" : s2.getNamaSekolah());
                        sub.add(s2 == null ? "" : s2.getBidangIlmu());
                        sub.add(s3 == null ? "" : s3.getGelarAkademik());
                        sub.add(s3 == null ? "" : s3.getNamaSekolah());
                        sub.add(s3 == null ? "" : s3.getBidangIlmu());
                        datas.add(sub);
                    }
                    HibernateUtil.closeSession();
                    datas.add(new ArrayList()); datas.add(new ArrayList()); datas.add(new ArrayList());
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
                        int y = ev.getRow() - 6;
                        DosenAction.cetakDRHDosen(dosens.get(y));
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileDosen.java:122"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 15, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
