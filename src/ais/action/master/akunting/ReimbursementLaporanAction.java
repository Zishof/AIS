package ais.action.master.akunting;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.ReimbursementPegawai;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;

/** Laporan detail, rekap, aging/SLA, dan export Excel reimbursement. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ReimbursementLaporanAction extends GenericAutowireComposer {
    private static final long serialVersionUID = 1L;
    private MyDatebox start;
    private MyDatebox end;
    private Combobox status;
    private Combobox kategori;
    private Textbox pegawai;
    private Label reportInfo;
    private Grid gridDetail;
    private Grid gridStatus;
    private Grid gridKategori;
    private Grid gridAging;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        if (!isFinance()) {
            // Tanpa popup: cukup sembunyikan tab Laporan untuk non-Keuangan (pegawai tetap bisa tab Reimbursement).
            try {
                Component c = comp;
                while (c != null && !(c instanceof org.zkoss.zul.Tabpanel)) {
                    c = c.getParent();
                }
                if (c != null && ((org.zkoss.zul.Tabpanel) c).getLinkedTab() != null) {
                    ((org.zkoss.zul.Tabpanel) c).getLinkedTab().setVisible(false);
                }
            } catch (Exception ignore) { }
            comp.setVisible(false);
            return;
        }
        initFilters();
        gridDetail.setRowRenderer(new DetailRenderer());
        gridStatus.setRowRenderer(new SummaryRenderer());
        gridKategori.setRowRenderer(new SummaryRenderer());
        gridAging.setRowRenderer(new AgingRenderer());
        load();
    }

    private void initFilters() {
        Calendar cal = ais.ui.util.WaktuUtil.getCalendar(); end.setValue(cal.getTime());
        cal.set(Calendar.MONTH, Calendar.JANUARY); cal.set(Calendar.DAY_OF_MONTH, 1); start.setValue(cal.getTime());
        String[] states = { "Semua", ReimbursementPegawai.DIAJUKAN, ReimbursementPegawai.REVISI,
                ReimbursementPegawai.DITOLAK, ReimbursementPegawai.DISETUJUI, ReimbursementPegawai.LUNAS };
        for (int i = 0; i < states.length; i++) status.appendItem(states[i]).setValue(i == 0 ? "" : states[i]);
        status.setSelectedIndex(0);
        kategori.appendItem("Semua").setValue("");
        Konfigurasi cfg = Common.getKonfigurasi("kategori_reimbursement_pegawai",
                "Barang,Jasa,Perjalanan Dinas,Konsumsi,Transportasi,Lainnya");
        String value = cfg == null || cfg.getNilai() == null
                ? "Barang,Jasa,Perjalanan Dinas,Konsumsi,Transportasi,Lainnya" : cfg.getNilai();
        String[] categories = value.split(",");
        for (int i = 0; i < categories.length; i++) kategori.appendItem(categories[i].trim()).setValue(categories[i].trim());
        kategori.setSelectedIndex(0);
    }

    public void onSearch(Event event) { load(); }

    private void load() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List data = query(session, 2000);
            initialize(data);
            setModels(data);
            double total = 0; int open = 0;
            for (int i = 0; i < data.size(); i++) {
                ReimbursementPegawai d = (ReimbursementPegawai) data.get(i); total += d.getNominal();
                if (isOpen(d)) open++;
            }
            reportInfo.setValue(data.size() + " dokumen ditampilkan • Total Rp "
                    + Common.numberFormat.get().format(total) + " • " + open + " masih terbuka"
                    + (data.size() >= 2000 ? " • Tampilan dibatasi 2.000 baris; gunakan Excel untuk data lebih besar." : ""));
        } catch (Exception e) { Common.tampilErrorJikaAdmin(e); reportInfo.setValue("Laporan gagal dimuat."); }
        finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private List query(Session session, int limit) {
        Criteria c = session.createCriteria(ReimbursementPegawai.class).createAlias("pegawai", "p");
        if (start.getValue() != null) c.add(Restrictions.ge("tanggalPengajuan", startOfDay(start.getValue())));
        if (end.getValue() != null) c.add(Restrictions.lt("tanggalPengajuan", nextDay(end.getValue())));
        String state = selected(status); if (!state.isEmpty()) c.add(Restrictions.eq("status", state));
        String category = selected(kategori); if (!category.isEmpty()) c.add(Restrictions.eq("kategori", category));
        String employee = pegawai.getValue().trim();
        if (!employee.isEmpty()) c.add(Restrictions.or(Restrictions.ilike("p.nama", employee, MatchMode.ANYWHERE),
                Restrictions.ilike("p.kode", employee, MatchMode.ANYWHERE)));
        return c.addOrder(Order.desc("tanggalPengajuan")).setMaxResults(limit).list();
    }

    private void initialize(List data) {
        for (int i = 0; i < data.size(); i++) {
            ReimbursementPegawai d = (ReimbursementPegawai) data.get(i);
            Hibernate.initialize(d.getPegawai()); Hibernate.initialize(d.getAtasan());
            Hibernate.initialize(d.getAkunBiaya()); Hibernate.initialize(d.getAkunPembayaran());
        }
    }

    private void setModels(List data) {
        Map<String, Summary> statuses = new LinkedHashMap<String, Summary>();
        Map<String, Summary> categories = new TreeMap<String, Summary>();
        List aging = new ArrayList(); Date now = ais.ui.util.WaktuUtil.getDate();
        for (int i = 0; i < data.size(); i++) {
            ReimbursementPegawai d = (ReimbursementPegawai) data.get(i);
            add(statuses, d.getStatus(), d.getNominal()); add(categories, d.getKategori(), d.getNominal());
            if (isOpen(d)) aging.add(new Aging(d, days(d.getTanggalPengajuan(), now)));
        }
        gridDetail.setModel(new SimpleListModel(data));
        gridStatus.setModel(new SimpleListModel(new ArrayList(statuses.values())));
        gridKategori.setModel(new SimpleListModel(new ArrayList(categories.values())));
        gridAging.setModel(new SimpleListModel(aging));
    }

    private void add(Map<String, Summary> map, String key, double amount) {
        key = key == null || key.trim().isEmpty() ? "Tanpa keterangan" : key;
        Summary row = map.get(key); if (row == null) { row = new Summary(key); map.put(key, row); }
        row.count++; row.amount += amount;
    }

    public void onExport(Event event) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List data = query(session, 20000); initialize(data);
            byte[] excel = createWorkbook(data);
            String name = "Laporan_Reimbursement_" + Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()) + ".xlsx";
            Filedownload.save(excel, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", name);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            try { MyMessageboxConfig.show("Export gagal: " + e.getMessage(), "Laporan Reimbursement",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION); } catch (Exception ignored) { }
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private byte[] createWorkbook(List data) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        CellStyle header = wb.createCellStyle(); Font font = wb.createFont(); font.setBold(true);
        header.setFont(font); header.setFillForegroundColor((short) 44); header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        writeDetail(wb, header, data);
        writeSummary(wb, header, data, true);
        writeSummary(wb, header, data, false);
        writeAging(wb, header, data);
        ByteArrayOutputStream out = new ByteArrayOutputStream(); wb.write(out); wb.close(); return out.toByteArray();
    }

    private void writeDetail(XSSFWorkbook wb, CellStyle header, List data) {
        Sheet sheet = wb.createSheet("Detail");
        String[] heads = { "Kode", "Tanggal Pengajuan", "Tanggal Pengeluaran", "Pegawai", "Atasan", "Kategori",
                "Deskripsi", "Nominal", "Pajak %", "Status", "Tanggal Akuntansi", "Tanggal Pembayaran",
                "Metode", "Bank", "Rekening", "Akun Biaya", "Akun Bayar", "Catatan Atasan", "Catatan Pembayaran" };
        poiHeader(sheet, header, heads); int rowNum = 1;
        for (int i = 0; i < data.size(); i++) {
            ReimbursementPegawai d = (ReimbursementPegawai) data.get(i);
            org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++); int col = 0;
            cell(row,col++,d.getKode()); cell(row,col++,date(d.getTanggalPengajuan())); cell(row,col++,date(d.getTanggalPengeluaran()));
            cell(row,col++,d.getPegawai()==null?"":d.getPegawai().getNama()); cell(row,col++,d.getAtasan()==null?"":d.getAtasan().getNama());
            cell(row,col++,d.getKategori()); cell(row,col++,d.getDeskripsi()); number(row,col++,d.getNominal()); number(row,col++,d.getPajakPersen());
            cell(row,col++,d.getStatus()); cell(row,col++,date(d.getTanggalAkuntansi())); cell(row,col++,date(d.getTanggalPembayaran()));
            cell(row,col++,d.getMetodePembayaran()); cell(row,col++,d.getBankPenerima()); cell(row,col++,d.getRekeningPenerima());
            cell(row,col++,d.getAkunBiaya()==null?"":d.getAkunBiaya().toString()); cell(row,col++,d.getAkunPembayaran()==null?"":d.getAkunPembayaran().toString());
            cell(row,col++,d.getCatatanAtasan()); cell(row,col++,d.getCatatanPembayaran());
        }
        for (int i=0;i<heads.length;i++) sheet.autoSizeColumn(i);
        sheet.createFreezePane(0,1); sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0,Math.max(0,rowNum-1),0,heads.length-1));
    }

    private void writeSummary(XSSFWorkbook wb, CellStyle header, List data, boolean byStatus) {
        Sheet sheet = wb.createSheet(byStatus ? "Rekap Status" : "Rekap Kategori");
        poiHeader(sheet, header, new String[] { byStatus ? "Status" : "Kategori", "Jumlah Dokumen", "Total Nominal" });
        Map<String, Summary> map = byStatus ? new LinkedHashMap<String, Summary>() : new TreeMap<String, Summary>();
        for (int i=0;i<data.size();i++) { ReimbursementPegawai d=(ReimbursementPegawai)data.get(i); add(map,byStatus?d.getStatus():d.getKategori(),d.getNominal()); }
        int r=1; for(Summary s:map.values()){ org.apache.poi.ss.usermodel.Row row=sheet.createRow(r++); cell(row,0,s.label); number(row,1,s.count); number(row,2,s.amount); }
        sheet.autoSizeColumn(0); sheet.autoSizeColumn(1); sheet.autoSizeColumn(2); sheet.createFreezePane(0,1);
    }

    private void writeAging(XSSFWorkbook wb, CellStyle header, List data) {
        Sheet sheet=wb.createSheet("Aging SLA"); poiHeader(sheet,header,new String[]{"Kode","Pegawai","Status","Umur Hari","SLA","Nominal","Atasan"});
        int r=1; Date now=ais.ui.util.WaktuUtil.getDate();
        for(int i=0;i<data.size();i++){ ReimbursementPegawai d=(ReimbursementPegawai)data.get(i); if(!isOpen(d))continue; int age=days(d.getTanggalPengajuan(),now);
            org.apache.poi.ss.usermodel.Row row=sheet.createRow(r++); cell(row,0,d.getKode()); cell(row,1,d.getPegawai()==null?"":d.getPegawai().getNama()); cell(row,2,d.getStatus()); number(row,3,age); cell(row,4,age>7?"Lewat SLA":"Dalam SLA"); number(row,5,d.getNominal()); cell(row,6,d.getAtasan()==null?"":d.getAtasan().getNama()); }
        for(int i=0;i<7;i++)sheet.autoSizeColumn(i); sheet.createFreezePane(0,1);
    }

    private void poiHeader(Sheet sheet, CellStyle style, String[] values) { org.apache.poi.ss.usermodel.Row row=sheet.createRow(0); for(int i=0;i<values.length;i++){ org.apache.poi.ss.usermodel.Cell c=row.createCell(i); c.setCellValue(values[i]); c.setCellStyle(style); } }
    private void cell(org.apache.poi.ss.usermodel.Row row,int col,String value){ row.createCell(col).setCellValue(value==null?"":value); }
    private void number(org.apache.poi.ss.usermodel.Row row,int col,double value){ row.createCell(col).setCellValue(value); }
    private String date(Date value){ return value==null?"":Common.dateFormat4.get().format(value); }
    private String selected(Combobox box){ return box.getSelectedItem()==null||box.getSelectedItem().getValue()==null?"":String.valueOf(box.getSelectedItem().getValue()); }
    private boolean isOpen(ReimbursementPegawai d){ return ReimbursementPegawai.DIAJUKAN.equals(d.getStatus())||ReimbursementPegawai.DISETUJUI.equals(d.getStatus()); }
    private Date startOfDay(Date date){ Calendar c=ais.ui.util.WaktuUtil.getCalendar();c.setTime(date);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime(); }
    private Date nextDay(Date date){ Calendar c=ais.ui.util.WaktuUtil.getCalendar();c.setTime(startOfDay(date));c.add(Calendar.DAY_OF_MONTH,1);return c.getTime(); }
    private int days(Date from,Date to){ return from==null||to==null?0:(int)Math.max(0,(to.getTime()-from.getTime())/86400000L); }
    private boolean isFinance(){ Tbmuser u=Common.getCurrentUser();if(u==null)return false;Set roles=u.ambilRolesId();if(roles!=null){for(Object role:roles){String id=String.valueOf(role);if(Tbmrole.ADMINISTRATOR.equalsIgnoreCase(id)||Tbmrole.KEUANGAN.equalsIgnoreCase(id))return true;}}for(Tbmrole tr:new Tbmrole[]{u.getUserRole(),u.getUserRole2(),u.getUserRole3(),u.getUserRole4(),u.getUserRole5()}){if(tr!=null&&Boolean.TRUE.equals(tr.getKeuangan()))return true;}return false; }

    private static class Summary { String label; int count; double amount; Summary(String label){this.label=label;} }
    private static class Aging { ReimbursementPegawai data; int age; Aging(ReimbursementPegawai data,int age){this.data=data;this.age=age;} }
    private class SummaryRenderer implements RowRenderer { public void render(Row row,Object value){Summary s=(Summary)value;new Label(s.label).setParent(row);new Label(String.valueOf(s.count)).setParent(row);new Label("Rp "+Common.numberFormat.get().format(s.amount)).setParent(row);} }
    private class AgingRenderer implements RowRenderer { public void render(Row row,Object value){Aging a=(Aging)value;ReimbursementPegawai d=a.data;new Label(d.getKode()).setParent(row);new Label(d.getPegawai()==null?"-":d.getPegawai().getNama()).setParent(row);new Label(d.getStatus()).setParent(row);new Label(a.age+" hari").setParent(row);new Label(a.age>7?"Lewat SLA":"Dalam SLA").setParent(row);new Label("Rp "+Common.numberFormat.get().format(d.getNominal())).setParent(row);} }
    private class DetailRenderer implements RowRenderer { public void render(Row row,Object value){ReimbursementPegawai d=(ReimbursementPegawai)value;new Label(d.getKode()).setParent(row);new Label(date(d.getTanggalPengajuan())).setParent(row);new Label(d.getPegawai()==null?"-":d.getPegawai().getNama()).setParent(row);new Label(d.getKategori()).setParent(row);new Label(d.getDeskripsi()).setParent(row);new Label("Rp "+Common.numberFormat.get().format(d.getNominal())).setParent(row);new Label(d.getPajakPersen()+"%").setParent(row);new Label(d.getStatus()).setParent(row);new Label(d.getAtasan()==null?"-":d.getAtasan().getNama()).setParent(row);new Label(date(d.getTanggalAkuntansi())).setParent(row);new Label(date(d.getTanggalPembayaran())).setParent(row);new Label(d.getMetodePembayaran()==null?"-":d.getMetodePembayaran()).setParent(row);} }
}
