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
 * Laporan borang akreditasi BAN-PT butir A-4.4.1 (profil dosen tetap): mendaftar dosen aktif
 * dengan ikatan kerja tetap dan NIDN terisi (opsional difilter jurusan), diurutkan menurut nama.
 * Untuk setiap dosen, riwayat pendidikan S1/S2/S3 ({@link RiwayatPendidikanDosen}) diambil terpisah
 * dan ditampilkan sebagai kolom gelar/nama sekolah/bidang ilmu per jenjang. Mengikuti kerangka
 * kerja laporan sapto ({@link SaptoBaseWindow}); subkelas ini menentukan {@code sheetCode}
 * ({@code "A-4.4.1"}), filter fakultas/jurusan, dan pengisian data di {@link #onCetak}. Baris pada
 * worksheet dapat diklik untuk mencetak DRH dosen bersangkutan lewat
 * {@link DosenAction#cetakDRHDosen(Dosen)}.
 */
public class LaporanProfileDosen_A_4_4_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.4.1";
    private static final long serialVersionUID = 3331244819198611604L;
    /** Membuat jendela laporan, menginisialisasi filter fakultas/jurusan, dan membangun tata letak dasar. */
    public LaporanProfileDosen_A_4_4_1() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membuat jendela laporan dengan judul/border/closable kustom; setup sama seperti konstruktor default. */
    public LaporanProfileDosen_A_4_4_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter fakultas/jurusan bawaan {@link SaptoBaseWindow} ke baris filter. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /** Mengambil daftar dosen tetap aktif ber-NIDN (difilter jurusan bila dipilih) beserta riwayat pendidikan S1/S2/S3 masing-masing (di thread terpisah) dan menampilkannya sebagai worksheet A-4.4.1 dengan klik baris untuk cetak DRH dosen. */
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
                .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                .addOrder(Order.asc("nama"))
                .createAlias("ikatanKerjaDosen","ikatanKerjaDosen")
                .add(Restrictions.or(Restrictions.isNull("ikatanKerjaDosen.tetap"), Restrictions.eq("ikatanKerjaDosen.tetap", true)))
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", selectedJurusan))
                .add(Restrictions.isNotNull("nidn")).add(Restrictions.ne("nidn", "")).list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 12; i++) datas.add(new ArrayList());

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
                        sub.add(""); sub.add(rowIndex);
                        sub.add(dosen.getNama()); sub.add(dosen.getNidn());
                        sub.add(dosen.getTanggallahir() == null ? "" : Common.dateFormat112.get().format(dosen.getTanggallahir()));
                        sub.add(dosen.getJabatanFungsionalDosen() == null ? "" : dosen.getJabatanFungsionalDosen().getNama());
                        sub.add(dosen.getSertifikasi() ? "Ya" : "Tidak");
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
                        int y = ev.getRow() - 12;
                        DosenAction.cetakDRHDosen(dosens.get(y));
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileDosen_A_4_4_1.java:124"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
