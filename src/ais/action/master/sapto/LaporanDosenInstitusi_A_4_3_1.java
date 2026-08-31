package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.event.CellMouseEvent;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.ui.util.DataCriteriaWithColumn;

/**
 * Laporan borang akreditasi BAN-PT butir A-4.3.1 pada paket sapto: rekap jumlah dosen tetap
 * menurut jabatan fungsional (Profesor, Lektor Kepala, Lektor, Asisten Ahli, Tenaga Pengajar),
 * dipecah per jenjang pendidikan terakhir (S3/Sp-2, S2/Sp-1, S1/Profesi/D4). Kelas ini adalah
 * window ZK yang dibangun di atas {@link SaptoBaseWindow}, mengikuti konvensi kode sheet
 * {@link #sheetCode}. Tidak memiliki filter yang ditampilkan ke pengguna.
 *
 * <p>
 * Data dihitung lewat satu query SQL native ber-{@code UNION ALL} (3 blok, satu per kelompok
 * jenjang pendidikan) yang menjumlahkan kecocokan jabatan fungsional memakai {@code CASE WHEN},
 * dijalankan di thread terpisah. Laporan ini interaktif: mengklik satu sel data membuka daftar
 * {@link Dosen} rinci di balik angka tersebut lewat {@code Common#cetakDataCustomButton}, dengan
 * kriteria (jabatan fungsional + kelompok jenjang pendidikan) diturunkan dari posisi
 * baris/kolom sel yang diklik.
 * </p>
 */
public class LaporanDosenInstitusi_A_4_3_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-4.3.1_PT";
    private static final long serialVersionUID = 3331244819198611604L;

    /** Membangun window laporan dalam konfigurasi baku (dipanggil dari kode yang membuat instance tanpa parameter tambahan). */
    public LaporanDosenInstitusi_A_4_3_1() {
        super();
        try { buildBase(true); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun window laporan dengan judul, tipe border, dan status closable yang dapat diatur eksplisit. */
    public LaporanDosenInstitusi_A_4_3_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        buildBase(true);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    @Override protected void buildFilters(Row row) { /* no filters */ }

    /**
     * Menjalankan query rekap jumlah dosen tetap per jabatan fungsional dan kelompok jenjang
     * pendidikan, menampilkan hasilnya sebagai worksheet {@link #sheetCode} (dijalankan di
     * thread terpisah), sekaligus memasang listener klik-sel yang membuka daftar {@link Dosen}
     * rinci di balik angka yang diklik.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 11; i++) datas.add(new ArrayList());

                    String sql =
                        "select " +
                        "sum(case when c.nama ilike 'Profesor' then 1 else 0 end) as Profesor," +
                        "sum(case when c.nama ilike 'Lektor Kepala' then 1 else 0 end) as LektorKepala," +
                        "sum(case when c.nama ilike 'Lektor' then 1 else 0 end) as Lektor," +
                        "sum(case when c.nama ilike 'Asisten Ahli' then 1 else 0 end) as AsistenAhli," +
                        "sum(case when c.nama ilike 'Tenaga Pengajar%' then 1 else 0 end) as TenagaPengajar " +
                        "from dosen a " +
                        "inner join employ.pendidikan b on (a.pendidikan=b.id) " +
                        "inner join jabatan_fungsional_dosen c on (a.jabatan_fungsional_dosen=c.id) " +
                        "where a.aktif and (b.nama='S3' or b.nama='Sp-2') and a.tetap=1 " +
                        "union all " +
                        "select " +
                        "sum(case when c.nama ilike 'Profesor' then 1 else 0 end)," +
                        "sum(case when c.nama ilike 'Lektor Kepala' then 1 else 0 end)," +
                        "sum(case when c.nama ilike 'Lektor' then 1 else 0 end)," +
                        "sum(case when c.nama ilike 'Asisten Ahli' then 1 else 0 end)," +
                        "sum(case when c.nama ilike 'Tenaga Pengajar%' then 1 else 0 end) " +
                        "from dosen a " +
                        "inner join employ.pendidikan b on (a.pendidikan=b.id) " +
                        "inner join jabatan_fungsional_dosen c on (a.jabatan_fungsional_dosen=c.id) " +
                        "where a.aktif and (b.nama='S2' or b.nama='Sp-1') and a.tetap=1 " +
                        "union all " +
                        "select " +
                        "sum(case when c.nama ilike 'Profesor' then 1 else 0 end)," +
                        "sum(case when c.nama ilike 'Lektor Kepala' then 1 else 0 end)," +
                        "sum(case when c.nama ilike 'Lektor' then 1 else 0 end)," +
                        "sum(case when c.nama ilike 'Asisten Ahli' then 1 else 0 end)," +
                        "sum(case when c.nama ilike 'Tenaga Pengajar%' then 1 else 0 end) " +
                        "from dosen a " +
                        "inner join employ.pendidikan b on (a.pendidikan=b.id) " +
                        "inner join jabatan_fungsional_dosen c on (a.jabatan_fungsional_dosen=c.id) " +
                        "where a.aktif and (b.nama='S1' or b.nama='Profesi' or b.nama='D4') and a.tetap=1";

                    List<Object[]> objs = session.createSQLQuery(sql).list();
                    for (Object[] obj : objs) {
                        List sub = new ArrayList();
                        sub.add(""); sub.add(""); sub.add("");
                        for (int c = 0; c < 5; c++) {
                            sub.add(obj[c] == null ? 0.0 : Double.parseDouble(obj[c].toString().trim()));
                        }
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
                        final CellMouseEvent event = (CellMouseEvent) arg0;
                        EventListener el = (EventListener) Common.cetakDataCustomButton(
                            Dosen.class, new DataCriteriaWithColumn() {
                                @Override
                                public Object[] initCriteria(boolean order) {
                                    try {
                                        int x = event.getColumn() - 3;
                                        int y = event.getRow() - 11;
                                        String[] xx = {"Profesor","Lektor Kepala","Lektor","Asisten Ahli","Tenaga Pengajar",""};
                                        String[] jenjang = {"S3","Strata 3"};
                                        if (y == 1) jenjang = new String[]{"S2","Strata 2"};
                                        else if (y == 2) jenjang = new String[]{"Profesi","D4","S1","Strata 1"};
                                        else if (y == 3) jenjang = new String[]{};
                                        String colX = xx[x];
                                        Criteria criteria = HibernateUtil.currentSession()
                                            .createCriteria(Dosen.class)
                                            .add(Restrictions.eq("tetap", 1))
                                            .createAlias("jabatanFungsionalDosen","jabatanFungsionalDosen")
                                            .add(colX.trim().isEmpty() ? Restrictions.sqlRestriction("true")
                                                : Restrictions.ilike("jabatanFungsionalDosen.nama", colX, MatchMode.EXACT))
                                            .createAlias("pendidikan","pendidikan")
                                            .add(jenjang.length == 0 ? Restrictions.sqlRestriction("true")
                                                : Restrictions.in("pendidikan.nama", jenjang));
                                        return new Object[]{criteria, new String[]{"nidn","mycode","nama","kelamin",
                                            "tempatlahir","tanggallahir","ktp","npwp","gelarDepan","gelarBelakang",
                                            "jabatanFungsionalDosen.nama","pendidikan.nama","fakultas.nama","jurusan.nama",
                                            "sertifikasi","ikatanKerjaDosen.nama","statusKepegawaian.nama",
                                            "jenisPendidikDanTenagaKependidikan.nama","lembagaPengangkat.nama",
                                            "sumberGaji.nama","golonganPegawai.nama"}};
                                    } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
                                    return null;
                                }
                            }, null, "Download Data", "/img/print.png", null, null, false, null, "DATA TAMBAHAN",
                            new String[48]).getAttribute("eventListener");
                        el.onEvent(event);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanDosenInstitusi_A_4_3_1.java:148"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
