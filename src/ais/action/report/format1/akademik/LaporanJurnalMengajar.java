package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Dosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyWindow;

public class LaporanJurnalMengajar extends MyWindow {

	private Center center;

	private Perkuliahan perkuliahan;

	private Toolbar toolbar;

	/**
	 *
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	public LaporanJurnalMengajar(Perkuliahan perkuliahan) {
		super();
		this.perkuliahan = perkuliahan;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Jurnal Mengajar", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "format1/jurnal_mengajar", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);

			}
		}));

		Toolbarbutton btnCetakHtml = new Toolbarbutton("Cetak (HTML)");
		btnCetakHtml.setSclass("ais-btn-sm");
		btnCetakHtml.setStyle("margin-left:6px;");
		btnCetakHtml.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				onCetakHtml();
			}
		});
		toolbar.appendChild(btnCetakHtml);

		onCetak(null);
	}

	private void onCetakHtml() {
		try {
			String html = buildJurnalHtmlPage();
			String escaped = html.replace("\\", "\\\\").replace("`", "\\`");
			org.zkoss.zk.ui.util.Clients.evalJavaScript(
				"(function(){" +
				"var w=window.open('','_blank','width=1200,height=860,scrollbars=yes,resizable=yes');" +
				"if(!w){alert('Popup diblokir browser. Izinkan popup untuk situs ini lalu coba lagi.');return;}" +
				"w.document.open('text/html','replace');" +
				"w.document.write(`" + escaped + "`);" +
				"w.document.close();" +
				"})();"
			);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Jurnal Mengajar", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings("rawtypes")
	private String buildJurnalHtmlPage() {
		// ---- data collection ----
		String namaUniversitas = "";
		String namaFakultas = "";
		String namaJurusan = "";
		if (perkuliahan.getJurusan() != null) {
			namaJurusan = perkuliahan.getJurusan().getNama() == null ? "" : perkuliahan.getJurusan().getNama();
			if (perkuliahan.getJurusan().getFakultas() != null) {
				namaFakultas = perkuliahan.getJurusan().getFakultas().getNama() == null ? ""
						: perkuliahan.getJurusan().getFakultas().getNama();
				if (perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() != null) {
					namaUniversitas = perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama() == null ? ""
							: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama();
				}
			}
		}

		String namaMK = "";
		String kodeMK = "";
		String sksMK = "";
		if (perkuliahan.getMatakuliah() != null) {
			namaMK = perkuliahan.getMatakuliah().getNama() == null ? "" : perkuliahan.getMatakuliah().getNama();
			kodeMK = perkuliahan.getMatakuliah().getKode() == null ? "" : perkuliahan.getMatakuliah().getKode();
			Object sks = perkuliahan.getMatakuliah().getSks();
			sksMK = sks == null ? "" : String.valueOf(sks);
		}

		String kelas = perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas();
		String ganjilGenap = perkuliahan.getGanjilGenap() == null ? "" : perkuliahan.getGanjilGenap();
		String tahunAjaran = perkuliahan.getTahunAjaran() == null ? "" : perkuliahan.getTahunAjaran();

		List<Dosen> dosens = perkuliahan.populateDosenBuNama();
		if (dosens == null) dosens = new ArrayList<Dosen>();
		String namaDosen = "";
		String nidnDosen = "";
		for (Dosen d : dosens) {
			String dn = d.getNama() == null ? "" : d.getNama();
			String di = d.getNidn() == null ? "" : d.getNidn();
			namaDosen += namaDosen.isEmpty() ? dn : ", " + dn;
			nidnDosen += nidnDosen.isEmpty() ? di : ", " + di;
		}

		String namaKaprodi = "";
		String nidnKaprodi = "";
		if (perkuliahan.getJurusan() != null && perkuliahan.getJurusan().getKaprodi() != null) {
			Dosen kp = perkuliahan.getJurusan().getKaprodi();
			namaKaprodi = kp.getNama() == null ? "" : kp.getNama();
			nidnKaprodi = kp.getNidn() == null ? "" : kp.getNidn();
		}

		boolean isObe = perkuliahan.getKurikulumPunyaMatakuliah() != null;

		String cplBobotStr = "";
		String teknikPerCpmkStr = "";
		String komponenPenilaianStr = "";
		if (isObe) {
			ais.database.model.KurikulumPunyaMatakuliah kpm = perkuliahan.getKurikulumPunyaMatakuliah();
			cplBobotStr = kpm.getCplBobot() == null ? "" : kpm.getCplBobot();
			teknikPerCpmkStr = kpm.getTeknikPerCpmk() == null ? "" : kpm.getTeknikPerCpmk();
			komponenPenilaianStr = kpm.getKomponenPenilaian() == null ? "" : kpm.getKomponenPenilaian();
		}

		List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();
		if (pertemuans == null) pertemuans = new ArrayList<Pertemuan>();

		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy",
				new java.util.Locale("id", "ID"));

		// ---- build HTML ----
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
		sb.append("<title>Jurnal Mengajar").append(isObe ? " OBE" : "").append("</title>");
		sb.append("<style>");
		sb.append("*{box-sizing:border-box;margin:0;padding:0;}");
		sb.append("body{font-family:Arial,sans-serif;font-size:11px;color:#1a1a1a;background:#e8eaf0;}");
		sb.append(".print-bar{position:fixed;top:0;width:100%;background:#1e3a5f;color:#fff;padding:8px 20px;display:flex;gap:8px;align-items:center;z-index:9999;box-shadow:0 2px 8px rgba(0,0,0,.4);}");
		sb.append(".pb-title{flex:1;font-weight:bold;font-size:13px;letter-spacing:.5px;}");
		sb.append(".btn-cetak{background:#f59e0b;color:#000;border:none;padding:7px 18px;border-radius:4px;cursor:pointer;font-weight:bold;font-size:12px;}");
		sb.append(".btn-tutup{background:#ef4444;color:#fff;border:none;padding:7px 18px;border-radius:4px;cursor:pointer;font-weight:bold;font-size:12px;}");
		sb.append(".page{max-width:1150px;margin:60px auto 40px;background:#fff;box-shadow:0 2px 16px rgba(0,0,0,.18);border-radius:4px;overflow:hidden;}");
		sb.append(".page-header{background:#1e3a5f;color:#fff;padding:18px 28px;text-align:center;}");
		sb.append(".page-header h2{font-size:14px;font-weight:bold;letter-spacing:1px;text-transform:uppercase;margin-bottom:2px;}");
		sb.append(".page-header p{font-size:11px;opacity:.85;}");
		sb.append(".page-body{padding:20px 28px 28px;}");
		sb.append(".section{margin-bottom:18px;}");
		sb.append(".sec-title{font-size:11px;font-weight:bold;color:#1e3a5f;text-transform:uppercase;letter-spacing:.5px;border-bottom:1.5px solid #1e3a5f;padding-bottom:4px;margin-bottom:10px;}");
		sb.append(".info-grid{display:grid;grid-template-columns:1fr 1fr;gap:4px 24px;}");
		sb.append(".info-row{display:flex;gap:4px;padding:2px 0;}");
		sb.append(".info-label{min-width:140px;font-weight:bold;color:#444;}");
		sb.append(".info-colon{margin-right:4px;color:#888;}");
		sb.append(".cpl-grid{display:flex;flex-wrap:wrap;gap:8px;margin-top:6px;}");
		sb.append(".cpl-badge{background:#1e3a5f;color:#fff;padding:5px 12px;border-radius:16px;font-size:10px;font-weight:bold;display:inline-flex;align-items:center;gap:6px;}");
		sb.append(".cpl-bobot{background:rgba(255,255,255,.25);border-radius:8px;padding:1px 6px;font-size:9px;}");
		sb.append(".grid-tbl{width:100%;border-collapse:collapse;font-size:10px;margin-top:6px;}");
		sb.append(".grid-tbl th{background:#1e3a5f;color:#fff;border:1px solid #2c5282;padding:5px 8px;text-align:left;font-size:10px;}");
		sb.append(".grid-tbl td{border:1px solid #bbc;padding:4px 8px;vertical-align:top;}");
		sb.append(".grid-tbl tr:nth-child(even) td{background:#f0f4f8;}");
		sb.append("table.jurnal{width:100%;border-collapse:collapse;font-size:10px;margin-top:6px;}");
		sb.append("table.jurnal th{background:#1e3a5f;color:#fff;border:1px solid #2c5282;padding:5px 6px;text-align:center;font-size:10px;font-weight:bold;}");
		sb.append("table.jurnal td{border:1px solid #bbc;padding:4px 6px;vertical-align:top;}");
		sb.append("table.jurnal tr:nth-child(even) td{background:#f7f9fc;}");
		sb.append(".sign-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:24px;}");
		sb.append(".sign-box{border:1px solid #bbc;border-radius:4px;padding:12px 16px;text-align:center;}");
		sb.append(".sign-ttl{font-weight:bold;color:#1e3a5f;font-size:11px;margin-bottom:2px;}");
		sb.append(".sign-sub{font-size:10px;color:#666;margin-bottom:4px;}");
		sb.append(".sign-area{height:60px;}");
		sb.append(".sign-name{font-weight:bold;border-top:1px solid #888;padding-top:4px;margin-top:4px;font-size:11px;}");
		sb.append(".sign-nidn{color:#666;font-size:10px;}");
		sb.append(".obe-badge{display:inline-block;background:#059669;color:#fff;font-size:9px;font-weight:bold;padding:2px 8px;border-radius:3px;margin-left:8px;letter-spacing:.5px;vertical-align:middle;}");
		sb.append("@media print{");
		sb.append(".print-bar{display:none!important;}");
		sb.append("body{background:#fff;}");
		sb.append(".page{max-width:100%;margin:0;box-shadow:none;border-radius:0;}");
		sb.append("table.jurnal{font-size:9px;}");
		sb.append("}");
		sb.append("</style></head><body>");

		// ---- print bar ----
		sb.append("<div class=\"print-bar\">");
		sb.append("<span class=\"pb-title\">Jurnal Mengajar");
		if (isObe) sb.append(" &mdash; <span style=\"color:#6ee7b7;\">OBE</span>");
		sb.append("</span>");
		sb.append("<button class=\"btn-cetak\" onclick=\"window.print()\">Cetak / Simpan PDF</button>");
		sb.append("<button class=\"btn-tutup\" onclick=\"window.close()\">&#x2715; Tutup</button>");
		sb.append("</div>");

		sb.append("<div class=\"page\">");

		// ---- page header ----
		sb.append("<div class=\"page-header\">");
		if (!namaUniversitas.isEmpty()) {
			sb.append("<h2>").append(escHtml(namaUniversitas)).append("</h2>");
		}
		if (!namaFakultas.isEmpty() || !namaJurusan.isEmpty()) {
			sb.append("<p>").append(escHtml(namaFakultas));
			if (!namaFakultas.isEmpty() && !namaJurusan.isEmpty()) sb.append(" &mdash; ");
			sb.append(escHtml(namaJurusan)).append("</p>");
		}
		sb.append("<div style=\"margin-top:10px;padding-top:10px;border-top:1px solid rgba(255,255,255,.3);\">");
		sb.append("<h2 style=\"font-size:15px;\">JURNAL MENGAJAR");
		if (isObe) sb.append("<span class=\"obe-badge\">OBE</span>");
		sb.append("</h2></div>");
		sb.append("</div>"); // page-header

		sb.append("<div class=\"page-body\">");

		// ---- informasi perkuliahan ----
		sb.append("<div class=\"section\">");
		sb.append("<div class=\"sec-title\">Informasi Perkuliahan</div>");
		sb.append("<div class=\"info-grid\">");
		sb.append("<div>");
		infoRow(sb, "Dosen Pengampu", namaDosen);
		infoRow(sb, "NIDN", nidnDosen);
		infoRow(sb, "Mata Kuliah", namaMK + (kodeMK.isEmpty() ? "" : " (" + kodeMK + ")"));
		infoRow(sb, "Kelas", kelas);
		sb.append("</div><div>");
		infoRow(sb, "SKS", sksMK);
		infoRow(sb, "Semester", ganjilGenap);
		infoRow(sb, "Tahun Akademik", tahunAjaran);
		infoRow(sb, "Program Studi", namaJurusan);
		sb.append("</div></div>"); // info-grid
		sb.append("</div>"); // section

		// ---- OBE-specific sections ----
		if (isObe) {
			// CPL badges
			if (!cplBobotStr.trim().isEmpty()) {
				sb.append("<div class=\"section\">");
				sb.append("<div class=\"sec-title\">Capaian Pembelajaran Lulusan (CPL) yang Dibebankan</div>");
				sb.append("<div class=\"cpl-grid\">");
				String[] cplParts = cplBobotStr.split(",");
				for (int ci = 0; ci < cplParts.length; ci++) {
					String cp = cplParts[ci].trim();
					if (cp.isEmpty()) continue;
					int col = cp.indexOf(':');
					String kode = col > 0 ? cp.substring(0, col).trim() : cp;
					String bobot = col > 0 ? cp.substring(col + 1).trim() : "";
					sb.append("<div class=\"cpl-badge\">").append(escHtml(kode));
					if (!bobot.isEmpty()) {
						sb.append("<span class=\"cpl-bobot\">").append(escHtml(bobot)).append("%</span>");
					}
					sb.append("</div>");
				}
				sb.append("</div>");
				sb.append("</div>"); // section
			}

			// CPMK & teknik penilaian — format: "CPMK-1:Kuis,UTS\nCPMK-2:Tugas,UAS"
			if (!teknikPerCpmkStr.trim().isEmpty()) {
				sb.append("<div class=\"section\">");
				sb.append("<div class=\"sec-title\">CPMK &amp; Teknik Penilaian</div>");
				sb.append("<table class=\"grid-tbl\"><thead><tr>");
				sb.append("<th style=\"width:110px;\">CPMK</th><th>Teknik Penilaian</th>");
				sb.append("</tr></thead><tbody>");
				String[] cpmkLines = teknikPerCpmkStr.split("\n");
				for (int li = 0; li < cpmkLines.length; li++) {
					String line = cpmkLines[li].trim();
					if (line.isEmpty()) continue;
					int col = line.indexOf(':');
					String kode = col > 0 ? line.substring(0, col).trim() : line;
					String teknik = col > 0 ? line.substring(col + 1).trim() : "";
					sb.append("<tr><td><strong>").append(escHtml(kode)).append("</strong></td>");
					sb.append("<td>").append(escHtml(teknik)).append("</td></tr>");
				}
				sb.append("</tbody></table></div>"); // section
			}

			// komponen penilaian — format: "Kuis:10,Tugas:10,UTS:30,UAS:40"
			if (!komponenPenilaianStr.trim().isEmpty()) {
				sb.append("<div class=\"section\">");
				sb.append("<div class=\"sec-title\">Komponen Penilaian</div>");
				sb.append("<table class=\"grid-tbl\" style=\"max-width:480px;\"><thead><tr>");
				sb.append("<th>Komponen</th><th style=\"width:90px;text-align:center;\">Bobot (%)</th>");
				sb.append("</tr></thead><tbody>");
				String[] komps = komponenPenilaianStr.split(",");
				int totalBobot = 0;
				for (int ki = 0; ki < komps.length; ki++) {
					String komp = komps[ki].trim();
					if (komp.isEmpty()) continue;
					int col = komp.indexOf(':');
					String nm = col > 0 ? komp.substring(0, col).trim() : komp;
					String bStr = col > 0 ? komp.substring(col + 1).trim() : "";
					try { totalBobot += Integer.parseInt(bStr); } catch (Exception ex2) { /* skip */ }
					sb.append("<tr><td>").append(escHtml(nm)).append("</td>");
					sb.append("<td style=\"text-align:center;\">").append(escHtml(bStr)).append("</td></tr>");
				}
				sb.append("<tr style=\"font-weight:bold;background:#e8eef4;\">");
				sb.append("<td>Total</td><td style=\"text-align:center;\">").append(totalBobot).append("</td></tr>");
				sb.append("</tbody></table></div>"); // section
			}
		}

		// ---- pertemuan table ----
		sb.append("<div class=\"section\">");
		sb.append("<div class=\"sec-title\">Rincian Pertemuan</div>");
		sb.append("<div style=\"overflow-x:auto;\"><table class=\"jurnal\"><thead><tr>");
		sb.append("<th style=\"width:28px;\">No.</th>");
		sb.append("<th style=\"width:44px;\">Pert.<br>Ke</th>");
		sb.append("<th style=\"width:130px;\">Hari / Tanggal</th>");
		sb.append("<th style=\"width:70px;\">Waktu</th>");
		if (isObe) {
			sb.append("<th>Sub-CPMK / Indikator Pencapaian</th>");
		}
		sb.append("<th>Topik / Bahan Kajian</th>");
		sb.append("<th style=\"width:100px;\">Metode Pembelajaran</th>");
		if (isObe) {
			sb.append("<th>Pengalaman Belajar</th>");
			sb.append("<th>Tugas &amp; Penilaian</th>");
		}
		sb.append("<th style=\"width:80px;\">Tanda Tangan<br>Dosen</th>");
		sb.append("</tr></thead><tbody>");

		int no = 1;
		for (int pi = 0; pi < pertemuans.size(); pi++) {
			Pertemuan p = pertemuans.get(pi);
			String tanggalStr = p.getTanggal() != null ? sdf.format(p.getTanggal()) : "";
			String wm = p.getWaktuMulai() == null ? "" : p.getWaktuMulai();
			String ws = p.getWaktuSelesai() == null ? "" : p.getWaktuSelesai();
			String waktu;
			if (wm.isEmpty() && ws.isEmpty()) waktu = "";
			else if (wm.isEmpty()) waktu = ws;
			else if (ws.isEmpty()) waktu = wm;
			else waktu = wm + " - " + ws;

			String topik = p.getTopik() == null ? "" : p.getTopik();
			String metode = p.getMetodePembelajaran() == null ? "" : p.getMetodePembelajaran();
			String pertemuanKe = p.getPertemuanKe() == null ? "" : String.valueOf(p.getPertemuanKe());

			sb.append("<tr>");
			sb.append("<td style=\"text-align:center;\">").append(no).append("</td>");
			sb.append("<td style=\"text-align:center;\">").append(escHtml(pertemuanKe)).append("</td>");
			sb.append("<td>").append(escHtml(tanggalStr)).append("</td>");
			sb.append("<td>").append(escHtml(waktu)).append("</td>");
			if (isObe) {
				String indikator = p.getIndikator() == null ? "" : p.getIndikator();
				sb.append("<td>").append(escHtml(indikator)).append("</td>");
			}
			sb.append("<td>").append(escHtml(topik)).append("</td>");
			sb.append("<td>").append(escHtml(metode)).append("</td>");
			if (isObe) {
				String pb = p.getPengalamanBelajar() == null ? "" : p.getPengalamanBelajar();
				String tp = p.getTugasDanPenilaian() == null ? "" : p.getTugasDanPenilaian();
				sb.append("<td>").append(escHtml(pb)).append("</td>");
				sb.append("<td>").append(escHtml(tp)).append("</td>");
			}
			sb.append("<td style=\"height:52px;\">&nbsp;</td>");
			sb.append("</tr>");
			no++;
		}

		if (pertemuans.isEmpty()) {
			int cs = isObe ? 9 : 6;
			sb.append("<tr><td colspan=\"").append(cs)
				.append("\" style=\"text-align:center;color:#999;padding:20px;\">Belum ada data pertemuan</td></tr>");
		}

		sb.append("</tbody></table></div>"); // overflow-x + table
		sb.append("</div>"); // section

		// ---- tanda tangan ----
		sb.append("<div class=\"sign-grid\">");
		sb.append("<div class=\"sign-box\">");
		sb.append("<div class=\"sign-ttl\">Mengetahui,</div>");
		sb.append("<div class=\"sign-sub\">Ketua Program Studi</div>");
		sb.append("<div class=\"sign-area\"></div>");
		sb.append("<div class=\"sign-name\">").append(escHtml(namaKaprodi)).append("</div>");
		if (!nidnKaprodi.isEmpty()) {
			sb.append("<div class=\"sign-nidn\">NIDN. ").append(escHtml(nidnKaprodi)).append("</div>");
		}
		sb.append("</div>");
		sb.append("<div class=\"sign-box\">");
		sb.append("<div class=\"sign-ttl\">Dosen Pengampu,</div>");
		sb.append("<div class=\"sign-sub\">&nbsp;</div>");
		sb.append("<div class=\"sign-area\"></div>");
		sb.append("<div class=\"sign-name\">").append(escHtml(namaDosen)).append("</div>");
		if (!nidnDosen.isEmpty()) {
			sb.append("<div class=\"sign-nidn\">NIDN. ").append(escHtml(nidnDosen)).append("</div>");
		}
		sb.append("</div>");
		sb.append("</div>"); // sign-grid

		sb.append("</div>"); // page-body
		sb.append("</div>"); // page
		sb.append("</body></html>");

		return sb.toString();
	}

	private static void infoRow(StringBuilder sb, String label, String value) {
		sb.append("<div class=\"info-row\">");
		sb.append("<span class=\"info-label\">").append(escHtml(label)).append("</span>");
		sb.append("<span class=\"info-colon\">:</span>");
		sb.append("<span>").append(escHtml(value)).append("</span>");
		sb.append("</div>");
	}

	private static String escHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("perkuliahan", perkuliahan == null || perkuliahan.getId() == null ? -1L : perkuliahan.getId());

		String ttd = null;
		Dosen kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
				: perkuliahan.getJurusan().getKaprodi();
		if (kaprodi != null) {
			LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
			if (lam != null) {
				File file = lam.ambilFile();

				parameters.put("ttd_kaprodi", file == null ? "" : file.getAbsolutePath());
			}
		}
		System.out.println("ttd_kaprodi => " + ttd);
		String nama_dosen = "";
		String nidn_dosen = "";
		if (perkuliahan != null) {
			int d = 1;

			List<Dosen> dosens = perkuliahan.populateDosenBuNama();

			for (Dosen dosen : dosens) {
				nama_dosen += nama_dosen.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				nidn_dosen += nidn_dosen.isEmpty() ? dosen.getNidn() : ", " + dosen.getNidn();
				LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
				if (lam != null) {
					File file = lam.ambilFile();
					parameters.put("ttd_dosen_" + d, file == null ? "" : file.getAbsolutePath());
					parameters.put("ttd_dosen_id_" + dosen.getId(), file == null ? "" : file.getAbsolutePath());

					System.out.println("ttd_dosen_" + d + " => " + ttd);
				}
				d++;

			}

			if (kaprodi != null) {
				LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						ttd = lam.ambilFile().getAbsolutePath();

						parameters.put("ttd_dosen_" + d, ttd);
					}
				}
			}

			parameters.put("nama_dosen", nama_dosen);
			parameters.put("nidn_dosen", nidn_dosen);

			List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();

			for (Pertemuan pertemuan : pertemuans) {
				nama_dosen = "";
				nidn_dosen = "";
				int size = 0;
				for (Dosen dosen : dosens) {
					Statusabsensi statusabsensi = null;
					if (pertemuan.getId() != null) {

						statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
								pertemuan.retreiveAbsensiId(dosen.getId()));

					}

					if (statusabsensi == null) {
						statusabsensi = ConstantValues.BELUM_ABSEN;
					}

					if (ConstantValues.MASUK != null && statusabsensi.getId().equals(ConstantValues.MASUK.getId())) {
						size++;
						nama_dosen += nama_dosen.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
						nidn_dosen += nidn_dosen.isEmpty() ? dosen.getNidn() : ", " + dosen.getNidn();

						LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
						if (lam != null) {
							File file = lam.ambilFile();
							parameters.put("ttd_dosen_pertemuan_" + pertemuan.getId(),
									file == null ? "" : file.getAbsolutePath());
							parameters.put("ttd_dosen_pertemuan_" + pertemuan.getId() + "_" + size,
									file == null ? "" : file.getAbsolutePath());
						}
					}
				}

				parameters.put("nama_dosen_" + pertemuan.getId(), nama_dosen);
				parameters.put("nidn_dosen_" + pertemuan.getId(), nidn_dosen);
			}
			pertemuans = null;

			List<Long> paralelId = new ArrayList<Long>();
			if (paralelId.isEmpty()) {
				paralelId.add(-1L);
			}
			paralelId.add(perkuliahan.getId());
			parameters.put("paralelId", paralelId.toArray());

			paralelId = new ArrayList<Long>(perkuliahan.ambilPertemuan().values());
			if (paralelId.isEmpty()) {
				paralelId.add(-1L);
			}
			parameters.put("pertemuans", paralelId.toArray());
		}

		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "format1/jurnal_mengajar",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Jurnal Mengajar", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
