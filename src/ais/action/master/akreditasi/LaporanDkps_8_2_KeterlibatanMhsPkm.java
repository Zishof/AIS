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
 * DKPS 2.0 Instrumen 8.2 — Keterlibatan Mahasiswa dalam PkM DTPS.
 *
 * <p>Instrumen ini mengukur seberapa banyak mahasiswa program studi yang
 * dilibatkan dalam kegiatan Pengabdian kepada Masyarakat (PkM) yang
 * dilakukan oleh Dosen Tetap Program Studi (DTPS). Keterlibatan mahasiswa
 * dalam PkM merupakan bagian dari implementasi pembelajaran berbasis
 * pengabdian (service learning) yang memperkuat kepedulian sosial dan
 * kompetensi praktis mahasiswa.</p>
 *
 * <p>Data ditampilkan per tahun (TS-2, TS-1, TS): jumlah PkM yang
 * dilakukan, jumlah mahasiswa yang terlibat, dan persentasenya terhadap
 * total PkM. Target ideal adalah 100% kegiatan PkM melibatkan mahasiswa.</p>
 *
 * <p>Sumber data: tabel pengajuan_penelitian_dan_pengabdian (jenis='PENGABDIAN')
 * dan anggota_pkm atau anggota_penelitian yang berisi referensi mahasiswa.</p>
 */
public class LaporanDkps_8_2_KeterlibatanMhsPkm extends AkreditasiBaseWindow {

    public static final String sheetCode = "DKPS-8.2";
    private static final long serialVersionUID = 1L;

    private Combobox tahunAjaran;

    public LaporanDkps_8_2_KeterlibatanMhsPkm() {
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

    public LaporanDkps_8_2_KeterlibatanMhsPkm(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran),
            Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

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
            final Label label = new Label(ais.common.Common.getBahasaConfig("Memuat data keterlibatan mahasiswa dalam PkM ..."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Session session = null;
                    try {
                        session = HibernateUtil.currentNativeSession();
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 5; i++) datas.add(new ArrayList());

                        // DKPS-8.2: per-record — No, Nama DTPS, Judul/Tema PkM,
                        //            NIM+Nama Mhs, Judul Kegiatan, Tahun
                        String jurusanFilter = selectedJurusan != null
                            ? " AND d.jurusan = " + selectedJurusan.getId() : "";

                        boolean loaded = false;
                        try {
                            String sql = "SELECT d.nama_dosen AS nama_dtps,"
                                + " p.judul_penelitian AS judul_pkm,"
                                + " m.nim || ' - ' || m.nama AS nim_nama,"
                                + " COALESCE(am.judul_kegiatan, p.judul_penelitian) AS judul_kegiatan,"
                                + " EXTRACT(year FROM p.tanggal_pengajuan) AS tahun"
                                + " FROM anggota_penelitian am"
                                + " INNER JOIN pengajuan_penelitian_dan_pengabdian p ON am.penelitian=p.id"
                                + " INNER JOIN dosen d ON p.dosen_ketua=d.id"
                                + " INNER JOIN mahasiswa m ON am.mahasiswa=m.id"
                                + " WHERE p.jenis='PENGABDIAN'"
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
                        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_8_2_KeterlibatanMhsPkm.java:130");}

                        if (!loaded) {
                            List sub = new ArrayList();
                            sub.add(1); sub.add("Nama DTPS"); sub.add("Judul/Tema PkM sesuai Roadmap");
                            sub.add("NIM — Nama Mahasiswa"); sub.add("Judul Kegiatan Mahasiswa"); sub.add(tahun);
                            datas.add(sub);
                        }

                        datas.add(new ArrayList());
                        label.setAttribute("datas", datas);
                        label.setValue("");
                    } catch (Exception ex) {
                        ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/akreditasi/LaporanDkps_8_2_KeterlibatanMhsPkm.java:143");
                        label.setValue("");
                    } finally {
                        if (session != null) {
                            try { session.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_8_2_KeterlibatanMhsPkm.java:147");}
                            try { session.close(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/akreditasi/LaporanDkps_8_2_KeterlibatanMhsPkm.java:148");}
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
