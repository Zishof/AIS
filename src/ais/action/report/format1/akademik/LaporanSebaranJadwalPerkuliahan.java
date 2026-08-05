package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

public class LaporanSebaranJadwalPerkuliahan extends MyWindow {

    private static final long serialVersionUID = 7829341056823740192L;

    private Combobox cbTahunAkademik;
    private Combobox cbSemester;
    private Combobox cbFakultas;
    private Combobox cbJurusan;

    public LaporanSebaranJadwalPerkuliahan() {
        super();
        try {
            cbFakultas = new Combobox();
            cbJurusan = new Combobox();
            Common.initFakultasDanJurusanDanSemua(cbFakultas, cbJurusan, null, null);
            init();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Sebaran Jadwal Perkuliahan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
            		new String[] {
            			"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
            			"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    private void init() throws Exception {
        Borderlayout bl = new MyBorderlayout();
        bl.setParent(this);

        West west = new West();
        west.setTitle("Filter");
        west.setCollapsible(true);
        west.setParent(bl);
        ZkCompat.setFlex(west, true);
        west.setWidth("300px");

        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setHeight("100%");
        grid.setParent(west);

        Columns columns = new Columns();
        columns.setParent(grid);
        MyColumnConfig col = new MyColumnConfig();
        col.setWidth("35%");
        col.setParent(columns);
        col = new MyColumnConfig();
        col.setParent(columns);

        Rows rows = new Rows();
        rows.setParent(grid);

        MyFormRow row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new MyLabelConfig("Fakultas"));
        row.appendChild(cbFakultas);
        cbFakultas.setWidth("90%");

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new MyLabelConfig("Prodi"));
        row.appendChild(cbJurusan);
        cbJurusan.setWidth("90%");

        cbTahunAkademik = Common.generateTahunAjaran(new Combobox());
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new MyLabelConfig("Tahun Akademik"));
        row.appendChild(cbTahunAkademik);
        cbTahunAkademik.setWidth("90%");

        cbSemester = new Combobox();
        cbSemester.setReadonly(true);
        org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem();
        ci.setLabel("Semua"); ci.setValue(null);
        cbSemester.appendChild(ci);
        cbSemester.setSelectedItem(ci);
        ci = new MyComboitemConfig();
        ci.setLabel(Perkuliahan.GANJIL); ci.setValue(Perkuliahan.GANJIL);
        cbSemester.appendChild(ci);
        ci = new MyComboitemConfig();
        ci.setLabel(Perkuliahan.GENAP); ci.setValue(Perkuliahan.GENAP);
        cbSemester.appendChild(ci);

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new MyLabelConfig("Semester"));
        row.appendChild(cbSemester);
        cbSemester.setWidth("90%");

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new MyLabelConfig());
        MyToolbarbuttonConfig btnTampil = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
        btnTampil.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onTampilkan();
            }
        });
        btnTampil.setParent(row);

        Center center = new Center();
        center.setParent(bl);
        ZkCompat.setFlex(center, true);
        Html hint = new Html();
        hint.setContent("<div style='padding:40px;color:#888;text-align:center'>"
                + "Pilih filter lalu klik <b>Tampilkan</b>.</div>");
        hint.setParent(center);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void onTampilkan() throws Exception {
        if (cbTahunAkademik.getSelectedItem() == null) {
            MyMessageboxConfig.show("Pilih tahun akademik terlebih dahulu.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }

        final String tahunAkademik = (String) cbTahunAkademik.getSelectedItem().getValue();
        final String genapGanjil = cbSemester.getSelectedItem() == null ? null
                : (String) cbSemester.getSelectedItem().getValue();
        final Fakultas fakultas = cbFakultas.getSelectedItem() == null ? null
                : (Fakultas) cbFakultas.getSelectedItem().getValue();
        final Jurusan jurusan = cbJurusan.getSelectedItem() == null ? null
                : (Jurusan) cbJurusan.getSelectedItem().getValue();

        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Session session = HibernateUtil.currentSession();

                Criterion criterionSmt = genapGanjil == null ? Restrictions.sqlRestriction("true")
                        : Restrictions.sqlRestriction("(this_.semester % 2) = (case '" + genapGanjil + "' when '"
                                + Perkuliahan.GANJIL + "' then 1 when '" + Perkuliahan.GENAP + "' then 0 end)");

                List perkuliahans = session.createCriteria(Perkuliahan.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(Restrictions.eq("tahunAjaran", tahunAkademik))
                        .add(criterionSmt)
                        .add(jurusan == null ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("jurusan", jurusan))
                        .createAlias("matakuliah", "mk")
                        .add(Restrictions.or(Restrictions.isNull("mk.extraKulikuler"),
                                Restrictions.eq("mk.extraKulikuler", false)))
                        .addOrder(Order.asc("kelas"))
                        .addOrder(Order.asc("waktuMulaiD"))
                        .createCriteria("jurusan", Criteria.LEFT_JOIN)
                        .add(fakultas == null ? Restrictions.sqlRestriction("1=1")
                                : Restrictions.eq("fakultas", fakultas))
                        .list();

                String html = buildHtmlPage(perkuliahans, tahunAkademik, genapGanjil,
                        jurusan == null ? "Semua" : jurusan.getNama(),
                        fakultas == null ? "Semua" : fakultas.getNama());

                String escaped = html.replace("\\", "\\\\").replace("`", "\\`").replace("${", "\\${");
                String js = "var w=window.open('','_blank','width=1400,height=760,scrollbars=yes,toolbar=yes,menubar=yes');"
                        + "w.document.write(`" + escaped + "`);"
                        + "w.document.close();";
                Clients.evalJavaScript(js);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String buildHtmlPage(List perkuliahans, String tahunAkademik, String genapGanjil,
            String jurusanFilter, String fakultasFilter) {

        // Group: jurusanNama -> kelas -> List<Object[]> {hari,jam,kode,nama,sks,ruang,dosen}
        Map grouped = new LinkedHashMap();

        for (Object obj : perkuliahans) {
            Perkuliahan p = (Perkuliahan) obj;
            String jNama = p.getJurusan() != null && p.getJurusan().getNama() != null
                    ? p.getJurusan().getNama() : "(Tanpa Prodi)";
            String kelas = p.getKelas() != null ? p.getKelas().toUpperCase().trim() : "-";

            if (!grouped.containsKey(jNama)) {
                grouped.put(jNama, new LinkedHashMap());
            }
            Map kelasBuckets = (Map) grouped.get(jNama);
            if (!kelasBuckets.containsKey(kelas)) {
                kelasBuckets.put(kelas, new ArrayList());
            }

            String hari = p.getHari() == null ? "" : p.getHari();
            String wm = p.getWaktuMulai() == null ? "" : p.getWaktuMulai();
            String ws = p.getWaktuSelesai() == null ? "" : p.getWaktuSelesai();
            String jam = wm.isEmpty() ? "" : (ws.isEmpty() ? wm : wm + " - " + ws);
            String kode = p.getMatakuliah() != null && p.getMatakuliah().getKode() != null
                    ? p.getMatakuliah().getKode() : "";
            String nama = p.getMatakuliah() != null && p.getMatakuliah().getNama() != null
                    ? p.getMatakuliah().getNama() : "";
            String sks = p.getMatakuliah() != null && p.getMatakuliah().getSks() != null
                    ? String.valueOf(p.getMatakuliah().getSks()) : "";
            String ruang = p.getRuang() != null && p.getRuang().getKodeRuangan() != null
                    ? p.getRuang().getKodeRuangan() : "";
            String dosen = "";
            for (Dosen d : p.populateDosenBuNama()) {
                dosen += dosen.isEmpty() ? d.getNama() : ", " + d.getNama();
            }

            ((List) kelasBuckets.get(kelas)).add(new Object[]{hari, jam, kode, nama, sks, ruang, dosen});
        }

        // Sort jurusan names alphabetically, kelas alphabetically, rows by hari+jam
        List jurusanKeys = new ArrayList(grouped.keySet());
        Collections.sort(jurusanKeys);
        for (Object jKey : jurusanKeys) {
            Map kelasBuckets = (Map) grouped.get(jKey);
            List kelasKeys = new ArrayList(kelasBuckets.keySet());
            Collections.sort(kelasKeys);
            for (Object kKey : kelasKeys) {
                Collections.sort((List) kelasBuckets.get(kKey), new Comparator() {
                    @Override
                    public int compare(Object a, Object b) {
                        Object[] ra = (Object[]) a;
                        Object[] rb = (Object[]) b;
                        int ha = hariIndex((String) ra[0]);
                        int hb = hariIndex((String) rb[0]);
                        if (ha != hb) return ha - hb;
                        return ((String) ra[1]).compareTo((String) rb[1]);
                    }
                });
            }
        }

        String semLbl = genapGanjil == null ? "Ganjil &amp; Genap" : esc(genapGanjil);

        StringBuilder sb = new StringBuilder(65536);
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<title>Sebaran Jadwal Perkuliahan - ").append(esc(tahunAkademik)).append("</title>");
        sb.append("<style>");
        sb.append("*{box-sizing:border-box;margin:0;padding:0}");
        sb.append("body{font-family:Arial,sans-serif;font-size:11px;background:#f0f2f5;color:#222}");
        sb.append(".pb{position:fixed;top:0;left:0;right:0;background:#1E3A5F;color:#fff;");
        sb.append("padding:8px 16px;display:flex;align-items:center;gap:10px;z-index:9999;box-shadow:0 2px 6px rgba(0,0,0,.3)}");
        sb.append(".pb h1{font-size:13px;font-weight:bold;flex:1}");
        sb.append(".pb button{padding:5px 14px;border:none;border-radius:4px;cursor:pointer;font-weight:bold;font-size:11px}");
        sb.append(".btn-p{background:#27ae60;color:#fff}.btn-x{background:#c0392b;color:#fff}");
        sb.append(".wrap{margin-top:50px;padding:12px 16px}");
        sb.append(".rh{background:#1E3A5F;color:#fff;text-align:center;padding:10px 16px;border-radius:5px;margin-bottom:14px}");
        sb.append(".rh h2{font-size:15px;font-weight:bold;letter-spacing:.5px;text-transform:uppercase}");
        sb.append(".rh p{font-size:10px;margin-top:4px;opacity:.85}");
        sb.append(".ps{margin-bottom:18px;border-radius:5px;overflow:hidden;");
        sb.append("box-shadow:0 1px 4px rgba(0,0,0,.12);background:#fff}");
        sb.append(".ph{background:#2C5282;color:#fff;padding:6px 12px;font-weight:bold;font-size:12px}");
        sb.append(".kr{display:flex;overflow-x:auto}");
        sb.append(".kb{flex:1;min-width:360px;border-right:2px solid #CBD5E0}");
        sb.append(".kb:last-child{border-right:none}");
        sb.append(".kh{background:#3182CE;color:#fff;padding:4px 8px;text-align:center;");
        sb.append("font-weight:bold;font-size:10px;letter-spacing:.3px}");
        sb.append("table{width:100%;border-collapse:collapse;font-size:10px}");
        sb.append("th{background:#EBF8FF;color:#2C5282;padding:4px;border:1px solid #BEE3F8;");
        sb.append("font-size:9px;font-weight:bold;text-align:center;white-space:nowrap}");
        sb.append("td{padding:3px 4px;border:1px solid #E2E8F0;vertical-align:top;line-height:1.3}");
        sb.append("tr:nth-child(even) td{background:#F7FAFC}");
        sb.append(".no{text-align:center;width:22px}.sks{text-align:center;width:26px}");
        sb.append(".hari{min-width:48px}.jam{white-space:nowrap;min-width:80px}");
        sb.append(".kode{min-width:48px}.mk{min-width:100px}.ruang{min-width:46px}.dosen{min-width:90px}");
        sb.append("@media print{");
        sb.append(".pb{display:none!important}.wrap{margin-top:0}");
        sb.append("body{background:#fff}");
        sb.append(".ps{break-inside:avoid;box-shadow:none;border:1px solid #aaa;margin-bottom:10px}");
        sb.append(".kr{flex-wrap:nowrap}}");
        sb.append("</style></head><body>");

        sb.append("<div class='pb'>");
        sb.append("<h1>SEBARAN JADWAL PERKULIAHAN &mdash; ").append(esc(tahunAkademik)).append("</h1>");
        sb.append("<button class='btn-p' onclick='window.print()'>&#128438; Cetak / Simpan PDF</button>");
        sb.append("<button class='btn-x' onclick='window.close()'>&#10005; Tutup</button>");
        sb.append("</div>");

        sb.append("<div class='wrap'>");
        sb.append("<div class='rh'><h2>SEBARAN JADWAL PERKULIAHAN</h2>");
        sb.append("<p>Semester ").append(semLbl)
          .append(" &bull; Tahun Akademik ").append(esc(tahunAkademik));
        if (!"Semua".equals(jurusanFilter)) {
            sb.append(" &bull; ").append(esc(jurusanFilter));
        } else if (!"Semua".equals(fakultasFilter)) {
            sb.append(" &bull; ").append(esc(fakultasFilter));
        } else {
            sb.append(" &bull; Semua Prodi");
        }
        sb.append("</p></div>");

        if (perkuliahans.isEmpty()) {
            sb.append("<div style='text-align:center;padding:40px;color:#666;background:#fff;border-radius:5px'>")
              .append("Tidak ada data jadwal ditemukan.</div>");
        }

        for (Object jKey : jurusanKeys) {
            String jNama = (String) jKey;
            Map kelasBuckets = (Map) grouped.get(jNama);
            List kelasKeys = new ArrayList(kelasBuckets.keySet());
            Collections.sort(kelasKeys);

            sb.append("<div class='ps'>");
            sb.append("<div class='ph'>&#9654; ").append(esc(jNama)).append("</div>");
            sb.append("<div class='kr'>");

            for (Object kKey : kelasKeys) {
                List rowList = (List) kelasBuckets.get(kKey);
                sb.append("<div class='kb'>");
                sb.append("<div class='kh'>KELAS ").append(esc((String) kKey)).append("</div>");
                sb.append("<table><tr>");
                sb.append("<th class='no'>No</th><th class='hari'>Hari</th><th class='jam'>Jam</th>");
                sb.append("<th class='kode'>Kode</th><th class='mk'>Mata Kuliah</th>");
                sb.append("<th class='sks'>SKS</th><th class='ruang'>Ruang</th><th class='dosen'>Nama Dosen</th>");
                sb.append("</tr>");
                int no = 1;
                for (Object rowObj : rowList) {
                    Object[] r = (Object[]) rowObj;
                    sb.append("<tr>");
                    sb.append("<td class='no'>").append(no++).append("</td>");
                    sb.append("<td class='hari'>").append(esc((String) r[0])).append("</td>");
                    sb.append("<td class='jam'>").append(esc((String) r[1])).append("</td>");
                    sb.append("<td class='kode'>").append(esc((String) r[2])).append("</td>");
                    sb.append("<td class='mk'>").append(esc((String) r[3])).append("</td>");
                    sb.append("<td class='sks'>").append(esc((String) r[4])).append("</td>");
                    sb.append("<td class='ruang'>").append(esc((String) r[5])).append("</td>");
                    sb.append("<td class='dosen'>").append(esc((String) r[6])).append("</td>");
                    sb.append("</tr>");
                }
                sb.append("</table></div>");
            }

            sb.append("</div></div>");
        }

        sb.append("</div></body></html>");
        return sb.toString();
    }

    private static int hariIndex(String hari) {
        if (hari == null) return 99;
        String h = hari.toLowerCase();
        if (h.contains("senin")) return 1;
        if (h.contains("selasa")) return 2;
        if (h.contains("rabu")) return 3;
        if (h.contains("kamis")) return 4;
        if (h.contains("jumat") || h.contains("jum")) return 5;
        if (h.contains("sabtu")) return 6;
        if (h.contains("minggu") || h.contains("ahad")) return 7;
        return 99;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
