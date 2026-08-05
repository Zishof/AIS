package ais.action.master.akreditasi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;

/**
 * DKPS 2.0 Instrumen 7.2 — Keterlibatan Mahasiswa dalam Penelitian DTPS.
 *
 * <p>Instrumen ini mengukur sejauh mana mahasiswa program studi dilibatkan
 * secara aktif dalam kegiatan penelitian yang dilakukan oleh Dosen Tetap
 * Program Studi (DTPS). Keterlibatan mahasiswa dalam penelitian merupakan
 * bagian dari implementasi pembelajaran berbasis riset (research-based learning)
 * yang memperkuat kompetensi ilmiah mahasiswa sebelum lulus.</p>
 *
 * <p>Data yang ditampilkan mencakup jumlah penelitian DTPS per tahun, jumlah
 * mahasiswa yang terlibat, dan persentase penelitian yang mengikutsertakan
 * mahasiswa. Semakin tinggi persentase ini, semakin baik integrasi antara
 * penelitian dosen dan proses pembelajaran di program studi.</p>
 *
 * <p>Sumber data: tabel {@code pengajuan_penelitian_dan_pengabdian} untuk
 * jumlah penelitian, dan tabel {@code anggota_penelitian} atau relasi mahasiswa
 * dalam penelitian jika tersedia. Jika data belum tersedia di sistem, akan
 * ditampilkan template kosong sebagai panduan pengisian manual.</p>
 */
public class LaporanDkps_7_2_KeterlibatanMhsRiset extends AkreditasiBaseWindow {

    public static final String sheetCode = "DKPS-7.2";
    private static final long serialVersionUID = 1L;

    private Combobox tahunAjaran;

    public LaporanDkps_7_2_KeterlibatanMhsRiset() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
                Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }

    public LaporanDkps_7_2_KeterlibatanMhsRiset(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
            Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    @Override
    protected String getSheetCode() { return sheetCode; }

    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(final Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = getSelectedJurusan();
            final String tahunStr = tahunAjaran.getSelectedItem() == null ? null
                : (String) tahunAjaran.getSelectedItem().getValue();
            final int tahun = tahunStr != null ? Integer.parseInt(tahunStr.split("/")[0])
                : java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data keterlibatan mahasiswa dalam penelitian ..."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = null;
                    try {
                        session = HibernateUtil.currentNativeSession();
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 5; i++) datas.add(new ArrayList());

                        // DKPS-7.2: per-record — No, Nama DTPS, Judul Penelitian,
                        //            NIM+Nama Mhs, Judul Kegiatan, Tahun
                        String jurusanFilter = selectedJurusan != null
                            ? " AND d.jurusan = " + selectedJurusan.getId() : "";

                        boolean loaded = false;
                        try {
                            String sql = "SELECT d.nama_dosen AS nama_dtps,"
                                + " p.judul_penelitian,"
                                + " m.nim || ' - ' || m.nama AS nim_nama,"
                                + " COALESCE(am.judul_kegiatan, p.judul_penelitian) AS judul_kegiatan,"
                                + " EXTRACT(year FROM p.tanggal_pengajuan) AS tahun"
                                + " FROM anggota_penelitian am"
                                + " INNER JOIN pengajuan_penelitian_dan_pengabdian p ON am.penelitian=p.id"
                                + " INNER JOIN dosen d ON p.dosen_ketua=d.id"
                                + " INNER JOIN mahasiswa m ON am.mahasiswa=m.id"
                                + " WHERE p.jenis='PENELITIAN'"
                                + " AND EXTRACT(year FROM p.tanggal_pengajuan) BETWEEN " + (tahun - 2) + " AND " + tahun
                                + jurusanFilter
                                + " ORDER BY tahun DESC, d.nama_dosen";
                            List<Object[]> rows = session.createSQLQuery(sql).list();
                            int noUrut = 1;
                            for (Object[] obj : rows) {
                                List sub = new ArrayList();
                                sub.add(noUrut++);
                                sub.add(obj[0] != null ? obj[0].toString() : "");
                                sub.add(obj[1] != null ? obj[1].toString() : "");
                                sub.add(obj[2] != null ? obj[2].toString() : "");
                                sub.add(obj[3] != null ? obj[3].toString() : "");
                                sub.add(obj[4] != null ? obj[4].toString() : "");
                                datas.add(sub);
                            }
                            loaded = rows != null && !rows.isEmpty();
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_2_KeterlibatanMhsRiset.java:133");}

                        if (!loaded) {
                            List sub = new ArrayList();
                            sub.add(1); sub.add("Nama DTPS"); sub.add("Judul Penelitian sesuai Roadmap");
                            sub.add("NIM — Nama Mahasiswa"); sub.add("Judul Kegiatan Mahasiswa"); sub.add(tahun);
                            datas.add(sub);
                        }

                        datas.add(new ArrayList());
                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_7_2_KeterlibatanMhsRiset.java:146");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_2_KeterlibatanMhsRiset.java:150");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_7_2_KeterlibatanMhsRiset.java:151");}
                        }
                        HibernateUtil.closeSession();
                    }
                }
            }).start();

            display(label, 6);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
