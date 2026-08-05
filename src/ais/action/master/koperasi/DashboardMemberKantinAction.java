package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.SQLQuery;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Div;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.ui.util.DashboardUiKit;

/**
 * Dashboard Member — konversi dari {@code modul/kantin/member/_dashboard_member}.
 *
 * <p>Ringkasan pribadi anggota yang sedang login: saldo, jumlah transaksi, total pengeluaran,
 * total hemat (diskon+cashback), total top-up; tren belanja bulanan; dan tenant favorit.
 * Semua grafik HTML/CSS ({@link DashboardUiKit}). Otomatis hanya data milik member yang login.</p>
 */
public class DashboardMemberKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host;
    private AnggotaKoperasi member;
    private Long memberId;

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        try {
            ais.database.model.Tbmuser u = Common.getCurrentUser();
            member = u == null ? null : u.getAnggotaKoperasi();
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardMemberKantinAction.java:47");
        }
        memberId = member == null ? null : member.getId();

        DashboardUiKit.attachIntro(comp, "Ringkasan Saya",
                "Rangkuman aktivitas Anda di kantin: saldo, total belanja, penghematan dari promo, top-up, "
                        + "tren belanja bulanan, dan tenant yang paling sering Anda kunjungi.");

        if (host == null) {
            return;
        }
        if (member == null) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='padding:24px;text-align:center;color:#64748b;'>Halaman ini khusus untuk Anggota Koperasi yang login.</div>"));
            return;
        }
        build();
    }

    public void onRefresh(Event event) throws Exception {
        build();
    }

    private void build() {
        host.getChildren().clear();
        StringBuilder sb = new StringBuilder();

        double saldo = 0;
        try {
            Double s = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(member);
            saldo = s == null ? 0 : s.doubleValue();
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/DashboardMemberKantinAction.java:78");
        }

        Object[] b = first(rows("SELECT COUNT(id), COALESCE(SUM(total_biaya),0), "
                + "COALESCE(SUM(COALESCE(total_diskon,0)+COALESCE(totalcashback,0)),0) "
                + "FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi = " + memberId));
        long jmlTrx = b == null ? 0 : (long) num(b[0]);
        double pengeluaran = b == null ? 0 : num(b[1]);
        double hemat = b == null ? 0 : num(b[2]);

        // Total top-up dari public.deposit (sumber saldo resmi: mencakup top-up manual admin
        // maupun online via VA — sama dengan DepositHelper. virtual_account_bank saja akan
        // melewatkan top-up manual sehingga member yang diisi admin tampil Rp 0).
        Object[] tp = first(rows("SELECT COALESCE(SUM(nominal),0) FROM public.deposit "
                + "WHERE anggota_koperasi = " + memberId));
        double totalTopup = tp == null ? 0 : num(tp[0]);

        List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
        kartu.add(new DashboardUiKit.Stat("Saldo Saya", "Rp " + DashboardUiKit.money(saldo),
                "saldo aktif", DashboardUiKit.GOOD));
        kartu.add(new DashboardUiKit.Stat("Jumlah Transaksi", DashboardUiKit.money(jmlTrx),
                "kali belanja", DashboardUiKit.PRIMARY));
        kartu.add(new DashboardUiKit.Stat("Total Pengeluaran", "Rp " + DashboardUiKit.money(pengeluaran),
                "akumulasi belanja", DashboardUiKit.ACCENT));
        kartu.add(new DashboardUiKit.Stat("Total Hemat", "Rp " + DashboardUiKit.money(hemat),
                "dari diskon & cashback", DashboardUiKit.WARN));
        kartu.add(new DashboardUiKit.Stat("Total Top-Up", "Rp " + DashboardUiKit.money(totalTopup),
                "saldo yang pernah diisi", DashboardUiKit.PRIMARY));
        sb.append(DashboardUiKit.descChip("Selamat datang, " + DashboardUiKit.esc(safe(member.getNama()))
                + ". Berikut ringkasan aktivitas Anda."));
        sb.append(DashboardUiKit.cards(kartu));

        // Tren belanja bulanan (6 bulan)
        List<Double> tren = new ArrayList<Double>();
        for (Object[] r : rows("SELECT TO_CHAR(tanggal_pembayaran,'Mon YYYY'), COALESCE(SUM(total_biaya),0) "
                + "FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi = " + memberId
                + " GROUP BY TO_CHAR(tanggal_pembayaran,'Mon YYYY'), TO_CHAR(tanggal_pembayaran,'YYYY-MM') "
                + "ORDER BY TO_CHAR(tanggal_pembayaran,'YYYY-MM') ASC LIMIT 6")) {
            tren.add(num(r[1]));
        }
        sb.append(DashboardUiKit.sparkline("Tren Belanja Bulanan",
                "Naik-turun pengeluaran belanja Anda tiap bulan.", tren, DashboardUiKit.PRIMARY,
                "Belum ada transaksi."));

        // Tenant favorit
        LinkedHashMap<String, Double> tenant = new LinkedHashMap<String, Double>();
        for (Object[] r : rows("SELECT COALESCE(t.nama,'Koperasi Utama'), COALESCE(SUM(a.total_biaya),0) "
                + "FROM koperasi.pembelian_anggota_koperasi a LEFT JOIN koperasi.toko t ON t.id = a.toko "
                + "WHERE a.anggota_koperasi = " + memberId + " GROUP BY t.nama ORDER BY 2 DESC LIMIT 5")) {
            tenant.put(str(r[0]), num(r[1]));
        }
        sb.append(DashboardUiKit.barList("Tenant Favorit",
                "Tempat Anda paling banyak berbelanja.", tenant, DashboardUiKit.GOOD, "", true,
                "Belum ada belanja."));

        // Peta jam belanja pribadi (hari x jam) — kapan saya paling sering belanja.
        double[][] mat = new double[7][24];
        boolean adaJam = false;
        for (Object[] r : rows("SELECT CAST(EXTRACT(DOW FROM tanggal_pembayaran) AS integer), "
                + "CAST(EXTRACT(HOUR FROM tanggal_pembayaran) AS integer), COUNT(id) "
                + "FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi = " + memberId + " GROUP BY 1, 2")) {
            int dow = (int) num(r[0]);
            int jam = (int) num(r[1]);
            if (dow >= 0 && dow < 7 && jam >= 0 && jam < 24) {
                mat[dow][jam] = num(r[2]);
                adaJam = true;
            }
        }
        if (adaJam) {
            String[] hari = { "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab" };
            String[] jamLbl = new String[24];
            for (int h = 0; h < 24; h++) {
                jamLbl[h] = String.format("%02d", h);
            }
            sb.append(DashboardUiKit.heatmap("Kapan Saya Biasa Belanja",
                    "Kotak makin pekat = hari & jam Anda paling sering berbelanja.", hari, jamLbl, mat,
                    DashboardUiKit.PRIMARY, "Belum ada transaksi."));
        }

        host.appendChild(DashboardUiKit.html(sb.toString()));
    }

    // ======================== Util ========================

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql) {
        try {
            SQLQuery q = HibernateUtil.currentSession().createSQLQuery(sql);
            return q.list();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Object[]>();
        }
    }

    private Object[] first(List<Object[]> r) {
        return r == null || r.isEmpty() ? null : r.get(0);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
