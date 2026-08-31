package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.Calendar;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.JabatanOrganisasiDosen;
import ais.database.model.Jurusan;
import ais.database.model.OrganisasiDosenPunyaDosen;

/**
 * Laporan borang akreditasi BAN-PT sapto butir <b>A-4.5.5</b> (keikutsertaan dosen dalam
 * organisasi keilmuan/keahlian): menampilkan daftar {@link OrganisasiDosenPunyaDosen} yang sudah
 * disetujui ({@code persetujuan=true}), difilter opsional per fakultas/jurusan, dengan penanda
 * kolom tingkat organisasi (Internasional/Nasional/Lokal) berdasarkan
 * {@link JabatanOrganisasiDosen}. Mengklik satu baris data pada lembar kerja mencetak Daftar
 * Riwayat Hidup (DRH) dosen bersangkutan lewat {@link DosenAction#cetakDRHDosen(Dosen)}.
 */
public class LaporanProfileDosen_A_4_5_5 extends SaptoBaseWindow {

    /** Kode lembar borang akreditasi yang dilaporkan kelas ini. */
    public static final String sheetCode = "A-4.5.5";
    private static final long serialVersionUID = 3331244819198611604L;

    /** Membuat jendela laporan dengan filter fakultas/jurusan awal dan langsung membangun tampilan dasar. */
    public LaporanProfileDosen_A_4_5_5() {
        super();
        try {
            initFakultasJurusan();
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /**
     * Membuat jendela laporan dengan judul, gaya border, dan status dapat-ditutup kustom.
     *
     * @param title    judul jendela
     * @param border   gaya border jendela
     * @param closable apakah jendela dapat ditutup pengguna
     */
    public LaporanProfileDosen_A_4_5_5(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        buildBase(false);
    }

    /** @return {@link #sheetCode}, kode lembar borang akreditasi yang dilaporkan kelas ini. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menyusun baris filter fakultas/jurusan. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);
    }

    /**
     * Memuat data keikutsertaan organisasi dosen (difilter jurusan bila dipilih) secara asinkron
     * di thread terpisah, lalu menampilkannya sebagai worksheet dengan klik-baris yang mencetak
     * DRH dosen terkait.
     *
     * @param event event pemicu (boleh {@code null}, tidak dipakai)
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
            final List<OrganisasiDosenPunyaDosen> items = session.createCriteria(OrganisasiDosenPunyaDosen.class)
                .createAlias("organisasiDosen","organisasiDosen")
                .createAlias("dosen","dosen")
                .addOrder(Order.asc("mulai"))
                .add(Restrictions.eq("persetujuan", true))
                .add(selectedJurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen.jurusan", selectedJurusan))
                .list();
            HibernateUtil.closeSession();

            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 9; i++) datas.add(new ArrayList());

                    int rowIndexTotal = 1;
                    for (int rowIndex = 1; rowIndex <= items.size(); rowIndex++) {
                        OrganisasiDosenPunyaDosen o = items.get(rowIndex - 1);

                        Integer tahunMulai = null, tahunSampai = null;
                        if (o.getMulai() != null) {
                            Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
                            cal.setTime(o.getMulai()); tahunMulai = cal.get(Calendar.YEAR);
                        }
                        if (o.getSampai() != null) {
                            Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
                            cal.setTime(o.getSampai()); tahunSampai = cal.get(Calendar.YEAR);
                        }

                        JabatanOrganisasiDosen jabatan = o.getJabatanOrganisasiDosen();
                        String colInt = "", colNas = "", colLoc = "";
                        if (jabatan != null && jabatan.getNama().equalsIgnoreCase("Internasional")) colInt = "V";
                        else if (jabatan != null && jabatan.getNama().equalsIgnoreCase("Nasional")) colNas = "V";
                        else colLoc = "V";

                        List sub = new ArrayList();
                        sub.add(""); sub.add(rowIndexTotal);
                        sub.add(o.getDosen().getNama());
                        sub.add(o.getOrganisasiDosen() == null ? "" : o.getOrganisasiDosen().getNama());
                        sub.add(tahunMulai); sub.add(tahunSampai);
                        sub.add(colInt); sub.add(colNas); sub.add(colLoc);
                        datas.add(sub);
                        rowIndexTotal++;
                    }
                    datas.add(new ArrayList()); datas.add(new ArrayList()); datas.add(new ArrayList());
                    label.setAttribute("datas", datas);
                    label.setValue("");
                }
            }).start();

            EventListener onCellClick = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        CellMouseEvent ev = (CellMouseEvent) arg0;
                        int y = ev.getRow() - 9;
                        Dosen dosen = items.get(y).getDosen();
                        DosenAction.cetakDRHDosen(dosen);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/LaporanProfileDosen_A_4_5_5.java:120"); /* ignore */ }
                }
            };

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16, onCellClick);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
