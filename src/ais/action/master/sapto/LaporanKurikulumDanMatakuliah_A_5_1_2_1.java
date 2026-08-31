package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;

/**
 * Laporan borang akreditasi BAN-PT butir A-5.1.2.1 pada paket sapto: menampilkan jumlah SKS
 * wajib lulus dan SKS pilihan lulus dari kurikulum aktif ({@link JenjangProgramStudi} terbaru
 * berdasarkan id) suatu program studi (jurusan). Kelas ini adalah window ZK yang dibangun di
 * atas {@link SaptoBaseWindow}, mengikuti konvensi kode sheet {@link #sheetCode}.
 *
 * <p>
 * Berbeda dari beberapa laporan sapto lain yang tanpa filter, kelas ini menampilkan filter
 * fakultas/jurusan ({@link #buildFilters(Row)} memanggil {@code addFakultasJurusanFilter}) —
 * jurusan yang dipilih pengguna menentukan {@link JenjangProgramStudi} mana yang dibaca.
 * </p>
 */
public class LaporanKurikulumDanMatakuliah_A_5_1_2_1 extends SaptoBaseWindow {

    public static final String sheetCode = "A-5.1.2.1";
    private static final long serialVersionUID = 3331244819198611604L;
    /** Membangun window laporan dan menyiapkan pilihan fakultas/jurusan pada filter (dipanggil dari kode yang membuat instance tanpa parameter tambahan). */
    public LaporanKurikulumDanMatakuliah_A_5_1_2_1() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Membangun window laporan dengan judul, tipe border, dan status closable yang dapat diatur eksplisit, sekaligus menyiapkan pilihan fakultas/jurusan. */
    public LaporanKurikulumDanMatakuliah_A_5_1_2_1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    @Override protected String getSheetCode() { return sheetCode; }

    /** Menambahkan filter pilihan fakultas/jurusan ke baris {@code row} pada panel filter laporan. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /**
     * Mengambil kurikulum ({@link JenjangProgramStudi}) terbaru dari jurusan yang dipilih
     * pengguna, lalu menampilkan SKS wajib lulus dan SKS pilihan lulus sebagai worksheet
     * {@link #sheetCode}. Bila belum ada jurusan yang dipilih, worksheet ditampilkan kosong.
     * Pengambilan data kurikulum dijalankan di thread terpisah agar UI tidak terblokir.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null
                || jurusan.getSelectedItem().getValue() == null ? null
                : (Jurusan) jurusan.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            if (selectedJurusan != null) {
                final JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) HibernateUtil.currentSession()
                    .createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", selectedJurusan))
                    .addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        List<List> datas = new ArrayList<List>();
                        for (int i = 0; i < 10; i++) datas.add(new ArrayList());
                        List sub1 = new ArrayList();
                        sub1.add(""); sub1.add(""); sub1.add("");
                        sub1.add(jenjangProgramStudi == null ? 0 : jenjangProgramStudi.getSksWajibLulus());
                        datas.add(sub1);
                        List sub2 = new ArrayList();
                        sub2.add(""); sub2.add(""); sub2.add("");
                        sub2.add(jenjangProgramStudi == null ? 0 : jenjangProgramStudi.getSksPilihanLulus());
                        datas.add(sub2);
                        label.setAttribute("datas", datas);
                        label.setValue("");
                    }
                }).start();
            } else {
                label.setValue("");
            }

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 10);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
