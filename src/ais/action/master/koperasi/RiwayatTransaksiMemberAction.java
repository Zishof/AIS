package ais.action.master.koperasi;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SQLQuery;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vlayout;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Riwayat Transaksi Member — konversi dari {@code modul/kantin/member/_riwayar_transaksi_member}.
 *
 * <p>Menampilkan daftar transaksi anggota yang sedang login: ringkasan (jumlah transaksi, total
 * belanja, diskon, cashback), tabel riwayat dengan tombol Detail (rincian item), dan tombol Unduh
 * Excel. Otomatis hanya menampilkan transaksi milik member yang login.</p>
 */
public class RiwayatTransaksiMemberAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;
    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private Div host;
    private AnggotaKoperasi member;
    private Long memberId;
    private List<Object[]> cache = new ArrayList<Object[]>();

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
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/RiwayatTransaksiMemberAction.java:68");
        }
        memberId = member == null ? null : member.getId();

        DashboardUiKit.attachIntro(comp, "Riwayat Transaksi Saya",
                "Daftar belanja Anda di kantin: kapan, di tenant mana, berapa total, diskon, dan cashback-nya. "
                        + "Klik Detail untuk melihat rincian barang.");

        if (host == null) {
            return;
        }
        if (member == null) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='padding:24px;text-align:center;color:#64748b;'>Halaman ini khusus untuk Anggota Koperasi yang login.</div>"));
            return;
        }
        buildList();
    }

    public void onRefresh(Event event) throws Exception {
        buildList();
    }

    public void onDownload(Event event) throws Exception {
        if (cache == null || cache.isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, belum ada transaksi yang dapat diunduh. Langkah yang dapat dilakukan: (1) periksa filter tanggal dan pastikan periode yang dipilih memiliki data transaksi; (2) coba perluas rentang tanggal pencarian; (3) ulangi proses unduh.", "Informasi",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }
        try {
            XSSFWorkbook wb = new XSSFWorkbook();
            XSSFSheet sheet = wb.createSheet("RIWAYAT");
            sheet.setDefaultColumnWidth(20);
            int ri = 0;
            sheet.createRow(ri++).createCell(0).setCellValue("Riwayat Transaksi - " + safe(member.getNama()));
            XSSFRow head = sheet.createRow(ri++);
            String[] headers = { "No.", "Tanggal", "Tenant", "Diskon", "Cashback", "Total" };
            for (int i = 0; i < headers.length; i++) {
                head.createCell(i).setCellValue(headers[i]);
            }
            int no = 1;
            for (Object[] r : cache) {
                XSSFRow row = sheet.createRow(ri++);
                row.createCell(0).setCellValue(no++);
                row.createCell(1).setCellValue(str(r[1]));
                row.createCell(2).setCellValue(str(r[2]));
                row.createCell(3).setCellValue(num(r[3]));
                row.createCell(4).setCellValue(num(r[4]));
                row.createCell(5).setCellValue(num(r[5]));
            }
            Common.setStyled(sheet);
            String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/riwayat_transaksi_"
                    + URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
                    + ".xlsx");
            File file = new File(filename);
            file.createNewFile();
            FileOutputStream out = new FileOutputStream(filename);
            try {
                wb.write(out);
            } finally {
                out.close();
            }
            Filedownload.save(file, XLSX_MIME);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            MyMessageboxConfig.show("Mohon maaf, gagal menyiapkan berkas Excel. Langkah yang dapat dilakukan: (1) coba muat ulang halaman dan ulangi ekspor; (2) pastikan koneksi jaringan stabil; (3) hubungi Administrator jika masalah berlanjut.", "Informasi",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
        }
    }

    // ======================== Build ========================

    private void buildList() {
        host.getChildren().clear();
        cache = rows("SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'dd-MM-yyyy HH24:MI'), COALESCE(b.nama,'-'), "
                + "COALESCE(a.total_diskon,0), COALESCE(a.totalcashback,0), COALESCE(a.total_biaya,0) "
                + "FROM koperasi.pembelian_anggota_koperasi a LEFT JOIN koperasi.toko b ON b.id = a.toko "
                + "WHERE a.anggota_koperasi = " + memberId + " ORDER BY a.tanggal_pembayaran DESC, a.id DESC LIMIT 300");

        double totBelanja = 0, totDiskon = 0, totCashback = 0;
        for (Object[] r : cache) {
            totDiskon += num(r[3]);
            totCashback += num(r[4]);
            totBelanja += num(r[5]);
        }
        List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
        kartu.add(new DashboardUiKit.Stat("Jumlah Transaksi", DashboardUiKit.money(cache.size()),
                "kali belanja", DashboardUiKit.PRIMARY));
        kartu.add(new DashboardUiKit.Stat("Total Belanja", "Rp " + DashboardUiKit.money(totBelanja),
                "akumulasi", DashboardUiKit.GOOD));
        kartu.add(new DashboardUiKit.Stat("Total Diskon", "Rp " + DashboardUiKit.money(totDiskon),
                "potongan diterima", DashboardUiKit.WARN));
        kartu.add(new DashboardUiKit.Stat("Total Cashback", "Rp " + DashboardUiKit.money(totCashback),
                "saldo balik", DashboardUiKit.ACCENT));
        host.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Ringkasan dan riwayat belanja Anda. Klik Detail pada sebuah baris untuk melihat barang yang dibeli.")));
        host.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));

        if (cache.isEmpty()) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='font-size:12px;color:#64748b;padding:14px;'>Belum ada transaksi.</div>"));
            return;
        }

        Grid grid = new Grid();
        grid.setSclass("dgrid");
        grid.setWidth("100%");
        grid.setStyle("border:0;background:transparent;");
        if (cache.size() > 25) {
            grid.setMold("paging");
            grid.setPageSize(25);
        }
        Columns columns = new Columns();
        columns.setParent(grid);
        columns.setSizable(true);
        addCol(columns, "No.", "48px");
        addCol(columns, "Tanggal", "150px");
        addCol(columns, "Tenant", null);
        addCol(columns, "Diskon", "110px");
        addCol(columns, "Cashback", "110px");
        addCol(columns, "Total", "120px");
        addCol(columns, "", "80px");

        Rows rows = new Rows();
        rows.setParent(grid);
        int no = 1;
        for (Object[] r : cache) {
            final Long trxId = ((Number) r[0]).longValue();
            Row row = new Row();
            row.setValign("middle");
            row.setParent(rows);
            new Label(String.valueOf(no++)).setParent(row);
            new Label(str(r[1])).setParent(row);
            Label tn = new Label(str(r[2]));
            tn.setStyle("font-weight:700;");
            tn.setParent(row);
            Label ld = new Label("Rp " + DashboardUiKit.money(num(r[3])));
            ld.setStyle("color:#dc2626;");
            ld.setParent(row);
            Label lc = new Label("Rp " + DashboardUiKit.money(num(r[4])));
            lc.setStyle("color:#0891b2;");
            lc.setParent(row);
            Label lt = new Label("Rp " + DashboardUiKit.money(num(r[5])));
            lt.setStyle("font-weight:800;color:#16a34a;");
            lt.setParent(row);
            Button det = new Button("Detail");
            det.setTooltiptext("Lihat rincian barang");
            det.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    bukaDetail(trxId);
                }
            });
            det.setParent(row);
        }
        grid.setParent(host);
    }

    private void bukaDetail(Long trxId) {
        final MyWindow w = new MyWindow("Rincian Transaksi", "normal", true);
        w.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        w.setWidth("440px");

        Vlayout box = new Vlayout();
        box.setStyle("padding:10px;");
        box.setParent(w);

        Grid g = new Grid();
        g.setSclass("dgrid");
        g.setWidth("100%");
        g.setStyle("border:0;background:transparent;");
        Columns c = new Columns();
        c.setParent(g);
        addCol(c, "Produk", null);
        addCol(c, "Qty", "60px");
        addCol(c, "Harga", "100px");
        addCol(c, "Subtotal", "110px");
        Rows gr = new Rows();
        gr.setParent(g);
        for (Object[] r : rows("SELECT COALESCE(c.nama,''), COALESCE(a.hargajual,0), COALESCE(a.qty,0), "
                + "COALESCE(a.diskon,0) FROM koperasi.pembelian a LEFT JOIN koperasi.produk c ON c.id = a.produk "
                + "WHERE a.pembelian_anggota_koperasi = " + trxId)) {
            double sub = num(r[1]) * num(r[2]) - num(r[3]);
            Row row = new Row();
            row.setParent(gr);
            new Label(str(r[0])).setParent(row);
            new Label(DashboardUiKit.money(num(r[2]))).setParent(row);
            new Label("Rp " + DashboardUiKit.money(num(r[1]))).setParent(row);
            new Label("Rp " + DashboardUiKit.money(sub)).setParent(row);
        }
        g.setParent(box);

        Button tutup = new Button("Tutup");
        tutup.setStyle("margin-top:10px;");
        tutup.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                w.detach();
            }
        });
        tutup.setParent(box);

        w.doOverlapped();
    }

    // ======================== Util ========================

    private void addCol(Columns columns, String label, String width) {
        Column c = new Column();
        c.setLabel(label);
        if (width != null) {
            c.setWidth(width);
        }
        c.setParent(columns);
    }

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
