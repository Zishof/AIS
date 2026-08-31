package ais.action.master.sapto;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import ais.action.master.sapto.util.SaptoUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.SumberDanaPenelitianDanPengabdian;

/**
 * Jendela laporan borang akreditasi BAN-PT SAPTO butir <b>A-6.2.3</b>: daftar kegiatan
 * pengabdian kepada masyarakat dosen tetap yang disetujui dalam 3 tahun terakhir sebelum tahun
 * akademik terpilih, beserta sumber dana (bisa lebih dari satu, digabung koma) dan jumlah dana
 * (ditampilkan dalam juta rupiah). Data diambil dari
 * {@link PengajuanPenelitianDanPengabdian} berstatus {@code DISETUJUI} dengan tipe
 * {@link ConstantValues#PENGABDIAN}, difilter opsional per jurusan dosen pengaju, dijalankan
 * asinkron di thread terpisah, lalu ditampilkan lewat {@link SaptoUtil#displayWorksheet}.
 */
public class LaporanDana_A_6_2_3 extends SaptoBaseWindow {

    public static final String sheetCode = "A-6.2.3";
    private static final long serialVersionUID = 3331244819198611604L;
    private Combobox tahunAjaran;

    /** Konstruktor default: menyiapkan filter fakultas/jurusan dan tahun akademik (default tahun akademik berjalan), lalu membangun kerangka jendela laporan. */
    public LaporanDana_A_6_2_3() {
        super();
        try {
            initFakultasJurusan();
            Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
            buildBase(false);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    /** Seperti konstruktor default, dengan judul/border/closable jendela yang dapat disesuaikan. */
    public LaporanDana_A_6_2_3(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initFakultasJurusan();
        Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
        buildBase(false);
    }

    /** Kode butir borang yang dipetakan ke jendela ini: {@value #sheetCode}. */
    @Override protected String getSheetCode() { return sheetCode; }

    /** Menyusun kontrol filter pada baris toolbar: filter fakultas/jurusan bawaan {@link SaptoBaseWindow}, ditambah pemilih tahun akademik yang memicu {@link #onCetak} saat diubah. */
    @Override
    protected void buildFilters(Row row) {
        addFakultasJurusanFilter(row);

        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        tahunAjaran.setWidth("90%");
        tahunAjaran.setReadonly(true);
        row.appendChild(tahunAjaran);
        tahunAjaran.addEventListener("onChange", new EventListener() {
            @Override public void onEvent(Event e) throws Exception { onCetak(null); }
        });
    }

    /**
     * Menyusun data laporan A-6.2.3: mengambil pengajuan pengabdian yang disetujui milik dosen
     * tetap dalam 3 tahun terakhir (opsional difilter per jurusan), merangkum judul, gabungan
     * sumber dana, dan jumlah dana (dalam juta rupiah) per kegiatan, dijalankan di thread terpisah
     * agar UI tidak terblokir, lalu hasilnya ditampilkan lewat {@link SaptoUtil#displayWorksheet}.
     */
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onCetak(Event event) {
        clearContent();
        try {
            final String tahunAkademik = (String) tahunAjaran.getSelectedItem().getValue();
            final Jurusan selectedJurusan = jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
                ? null : (Jurusan) jurusan.getSelectedItem().getValue();
            final Label label = new Label(ais.common.Common.getBahasaConfig("Proses load data .."));

            new Thread(new Runnable() {
                @Override
                public void run() {
                	try {
                    int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
                    Session session = HibernateUtil.currentNativeSession();
                    List<List> datas = new ArrayList<List>();
                    for (int i = 0; i < 9; i++) datas.add(new ArrayList());

                    List<PengajuanPenelitianDanPengabdian> list = session
                        .createCriteria(PengajuanPenelitianDanPengabdian.class)
                        .createAlias("sumberDanaPenelitianDanPengabdianes", "sumberDanaPenelitianDanPengabdianes")
                        .createAlias("tbmuser", "tbmuser").createAlias("tbmuser.dosen", "dosen")
                        .add(Restrictions.eq("dosen.tetap", 1))
                        .add(selectedJurusan == null ? Restrictions.sqlRestriction("true")
                            : Restrictions.eq("dosen.jurusan", selectedJurusan))
                        .createAlias("penelitianDanPengabdian", "penelitianDanPengabdian")
                        .add(Restrictions.eq("penelitianDanPengabdian.tipePenelitianDanPengabdian", ConstantValues.PENGABDIAN))
                        .add(Restrictions.ge("penelitianDanPengabdian.tahun", tahun - 3))
                        .add(Restrictions.eq("status", PengajuanPenelitianDanPengabdian.DISETUJUI))
                        .addOrder(Order.asc("penelitianDanPengabdian.tahun")).addOrder(Order.asc("id")).list();

                    for (PengajuanPenelitianDanPengabdian p : list) {
                        List sub = new ArrayList();
                        sub.add("");
                        sub.add(p.getPenelitianDanPengabdian().getTahun().toString());
                        sub.add(p.getJudul());

                        StringBuilder sumberDana = new StringBuilder();
                        for (SumberDanaPenelitianDanPengabdian s : p.getSumberDanaPenelitianDanPengabdianes()) {
                            if (sumberDana.length() > 0) sumberDana.append(", ");
                            sumberDana.append(s.getNama());
                        }
                        sub.add(sumberDana.toString());
                        sub.add(p.getJumlahDana() / 1000000.0);
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

            SaptoUtil.displayWorksheet(label, sheetCode, contentCenter, 16);
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }
}
