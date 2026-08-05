package ais.action.master.dashboard.keuangan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Html;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VOMahasiswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DashboardPembayaranMahasiswaPerBulan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private AmbilDataMahasiswaBanbox ambilDataMahasiswaBanbox = new AmbilDataMahasiswaBanbox();
	private Combobox semester = new Combobox();
	private Center center = new Center();

	private Combobox jenisPembayaran;

	private File file;

	private Label tahunAkademik;

	private static void closeOpenedSession(Session session) {
		ais.ui.util.DashboardModernHtmlUtil.closeOpenedSession(session);
	}

	private static void closeFileOutput(FileOutputStream out) {
		if (out != null) {
			try {
				out.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardPembayaranMahasiswaPerBulan.java:79");
			}
		}
	}

	private void tampilkanInfoAwal() {
		try {
			Common.clear(center);
			org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style='font-family:Arial,sans-serif;padding:14px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;color:#334155;line-height:1.5;'>"
					+ "<div style='font-size:16px;font-weight:800;color:#0f172a;margin-bottom:6px;'>Dashboard Pembayaran Mahasiswa per Bulan</div>"
					+ "<div style='font-size:12px;'>Pilih mahasiswa, semester, dan jenis pembayaran, lalu tekan <b>Proses</b>. Sistem menyiapkan rekap tagihan bulanan, pembayaran masuk, dan sisa kewajiban dalam file Excel.</div>"
					+ "<div style='display:flex;gap:10px;flex-wrap:wrap;margin-top:12px;'>"
					+ "<div style='flex:1;min-width:170px;background:#fff;border:1px solid #dbeafe;border-radius:10px;padding:10px;'><b>Tren bulanan</b><br/><span style='font-size:11px;color:#64748b;'>Membantu melihat bulan mana yang sudah dibayar atau belum dibayar.</span></div>"
					+ "<div style='flex:1;min-width:170px;background:#fff;border:1px solid #dcfce7;border-radius:10px;padding:10px;'><b>Item biaya</b><br/><span style='font-size:11px;color:#64748b;'>Menampilkan rincian SPP, praktikum, daftar ulang, atau item biaya lain.</span></div>"
					+ "<div style='flex:1;min-width:170px;background:#fff;border:1px solid #ffedd5;border-radius:10px;padding:10px;'><b>Sisa tagihan</b><br/><span style='font-size:11px;color:#64748b;'>Memudahkan pengecekan kewajiban yang belum selesai.</span></div>"
					+ "</div></div>");
			html.setParent(center);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardPembayaranMahasiswaPerBulan.java:96");
		}
	}

	public DashboardPembayaranMahasiswaPerBulan() {
		super();
		try {

			init();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public DashboardPembayaranMahasiswaPerBulan(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
			// initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		jenisPembayaran = Common.initJenisPembayaranMahasiswa(jenisPembayaran);
		jenisPembayaran.setReadonly(true);

		for (int i = 1; i < 32; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}
		semester.setReadonly(true);
		Common.selectComboItem(semester, 1);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(ambilDataMahasiswaBanbox);
		row.setParent(rows);
		ambilDataMahasiswaBanbox.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semester);
		row.setParent(rows);
		semester.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Label(Common.getCurrentTahunAkademik()));
		row.setParent(rows);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Mahasiswa mahasiswa = (Mahasiswa) ambilDataMahasiswaBanbox.getAttribute("mahasiswa");
				if (mahasiswa != null) {
					Common.selectComboItem(semester, mahasiswa.currentSemester());
				}
			}
		};

		ambilDataMahasiswaBanbox.setEventListener(eventListener);

		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran"));
		row.appendChild(jenisPembayaran);
		row.setParent(rows);
		jenisPembayaran.setWidth("90%");

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		tampilkanInfoAwal();

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Download Excel", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				FileInputStream input = null;
				try {
					if (file == null || !file.exists()) {
						ais.ui.util.MyMessageboxConfig.show("Mohon maaf, berkas Excel belum tersedia untuk diunduh. Langkah yang dapat dilakukan: (1) klik tombol Proses terlebih dahulu; (2) tunggu hingga pratinjau dan berkas Excel selesai dibuat; (3) setelah itu, silakan lakukan pengunduhan kembali.");
						return;
					}
					input = new FileInputStream(file);
					Filedownload.save(input, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Data Mahasiswa.xlsx");
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					try { if (input != null) input.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/dashboard/keuangan/DashboardPembayaranMahasiswaPerBulan.java:228");}
				}
			}
		});
		print.setParent(toolbar);

		if (ambilDataMahasiswaBanbox.getAttribute("mahasiswa") != null) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					eventListener.onEvent(null);
				}
			});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initSpreadsheet() throws Exception {
		Common.clear(center);
		center.appendChild(new Html(ais.ui.util.KeuanganDashboardEnhanceUtil.buildDescriptionBox(
				"Pratinjau Rekap Pembayaran",
				"Data ditampilkan dulu dalam tabel agar mudah dicek. Tombol Download Excel tetap memakai file asli yang dibuat setelah proses selesai.")));
		ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("pembayaran_mahasiswa_per_bulan",
				"Memuat Rekap Pembayaran Bulanan",
				"Memvalidasi filter mahasiswa, semester, dan jenis pembayaran.", 8);

		if (ambilDataMahasiswaBanbox.getAttribute("mahasiswa") == null
				|| jenisPembayaran.getSelectedItem() == null
				|| semester.getSelectedItem() == null) {
			ais.ui.util.KeuanganDashboardEnhanceUtil.hideFloatingProgress("pembayaran_mahasiswa_per_bulan");
			ais.ui.util.MyMessageboxConfig.show("Mohon maaf, data belum dapat diproses. Mohon Bapak/Ibu melengkapi terlebih dahulu: (1) pilih mahasiswa; (2) pilih semester; (3) pilih jenis pembayaran. Setelah seluruh isian dilengkapi, silakan ulangi proses ini.");
			return;
		}

		final Mahasiswa mahasiswaFilter = (Mahasiswa) ambilDataMahasiswaBanbox.getAttribute("mahasiswa");
		Integer tahunAngkatanMhs = mahasiswaFilter.getTahunangkatan();
		Integer semesterMulai = mahasiswaFilter.getPindahKeKampusIniMasukSemester();
		Integer tahunAkademikMulai = Common.getTahunAkademik((Integer) semester.getSelectedItem().getValue(),
				tahunAngkatanMhs, semesterMulai, mahasiswaFilter.getSemesterMulai());

		final String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);
		this.tahunAkademik.setValue(tahunAkademik);

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");
		(file = new File(filename)).createNewFile();

		Session session = null;
		FileOutputStream fileOut = null;
		XSSFWorkbook workbook = null;
		List<PreviewPembayaranRow> previewRows = new ArrayList<PreviewPembayaranRow>();
		try {
			ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("pembayaran_mahasiswa_per_bulan",
					"Mengambil Data Mahasiswa",
					"Mengambil mahasiswa aktif dan menyiapkan struktur tabel pembayaran bulanan.", 20);
			session = HibernateUtil.currentNativeSession();
			List<Mahasiswa> biodataMahasiswas = session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("id", mahasiswaFilter.getId())).list();

			workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("BAYAR");
			sheet.setDefaultColumnWidth(12);
			XSSFRow rowhead = sheet.createRow((short) 0);
			String[] headers = new String[] { "NIM", "NAMA", "BULAN", "SEMESTER", "ITEM BIAYA", "NOMINAL",
					"TELAH DIBAYAR", "NILAI DIBAYAR", "TAGIHAN", "TOTAL BELUM TERBAYAR", "TOTAL TELAH TERBAYAR" };
			for (int i = 0; i < headers.length; i++) {
				rowhead.createCell(i).setCellValue(headers[i]);
			}

			int rowIndex = 1;
			int rowIndexMhs = 1;
			Integer smt = (Integer) semester.getSelectedItem().getValue();
			int size = biodataMahasiswas == null ? 0 : biodataMahasiswas.size();
			for (Mahasiswa mahasiswa : biodataMahasiswas) {
				int percent = 30 + (size <= 0 ? 40 : (int) Math.min(40, rowIndexMhs * 40.0 / size));
				ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("pembayaran_mahasiswa_per_bulan",
						"Mengolah Pembayaran Mahasiswa",
						"Menghitung tagihan dan pembayaran " + mahasiswa.toString() + ".", percent);
				rowIndexMhs++;

				JenisKegiatan jenisKegiatan = (JenisKegiatan) jenisPembayaran.getSelectedItem().getValue();
				List<CicilanPembayaran> cicilanPembayarans = mahasiswa.ambilCicilan();
				Kegiatan kegiatan = mahasiswa.ambilKegiatans(smt, jenisKegiatan);
				Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
						: kegiatan.ambilDetailKegiatan(false);
				Collection detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan,
						false);
				int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jenisKegiatan, smt,
						detailBiayas, false, false);
				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan,
						countPengaturanBulanan > 0 ? "-1" : null, true, false);
				if (detailBiayas == null || detailBiayas.isEmpty()) {
					continue;
				}

				Double totalTagihanBelumTerbayar = 0.0;
				Double totalTagihanTelahTerbayar = 0.0;
				Double totalTagihan = 0.0;
				int firstRowForStudent = previewRows.size();
				for (Object o : detailBiayas) {
					try {
						PreviewPembayaranRow pr = null;
						if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) o;
							Double tag = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa, smt, ppb);
							if (tag == null || tag < 0.01) {
								continue;
							}
							Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, ppb, cicilanPembayarans);
							if (telahDibayar != null && telahDibayar > 0.1) {
								totalTagihanTelahTerbayar += telahDibayar;
							} else {
								totalTagihanBelumTerbayar += tag;
								totalTagihan += tag;
							}
							pr = new PreviewPembayaranRow(mahasiswa.getNim(), mahasiswa.getNama(), ppb.getNamaBulan(),
									String.valueOf(ppb.getDetailBiaya().getSemester()), ppb.getDetailBiaya().getItemBiaya().getNama(),
									tag, telahDibayar, totalTagihan);
						} else if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;
							ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
							Double tag = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, false);
							if (tag == null || tag < 0.01) {
								continue;
							}
							Double telahDibayar = VOMahasiswa.hitungTotalCicilan(kegiatan, detailBiaya, cicilanPembayarans);
							if (telahDibayar != null && telahDibayar > 0.1) {
								totalTagihanTelahTerbayar += telahDibayar;
							} else {
								totalTagihanBelumTerbayar += tag;
								totalTagihan += tag;
							}
							pr = new PreviewPembayaranRow(mahasiswa.getNim(), mahasiswa.getNama(), "-",
									String.valueOf(detailBiaya.getSemester()), itemBiaya.getNama(), tag, telahDibayar,
									totalTagihan);
						}
						if (pr != null) {
							previewRows.add(pr);
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
				for (int i = firstRowForStudent; i < previewRows.size(); i++) {
					PreviewPembayaranRow pr = previewRows.get(i);
					pr.totalBelum = totalTagihanBelumTerbayar;
					pr.totalTerbayar = totalTagihanTelahTerbayar;
				}
			}

			ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("pembayaran_mahasiswa_per_bulan",
					"Membuat Tabel Pratinjau", "Menyusun tabel ringkas sebelum file Excel diunduh.", 78);
			renderPreviewGrid(previewRows);

			for (int i = 0; i < previewRows.size(); i++) {
				PreviewPembayaranRow pr = previewRows.get(i);
				XSSFRow xr = sheet.createRow(i + 1);
				xr.createCell(0).setCellValue(pr.nim);
				xr.createCell(1).setCellValue(pr.nama);
				xr.createCell(2).setCellValue(pr.bulan);
				xr.createCell(3).setCellValue(pr.semester);
				xr.createCell(4).setCellValue(pr.itemBiaya);
				xr.createCell(5).setCellValue(Common.numberFormat.get().format(pr.nominal));
				xr.createCell(6).setCellValue(pr.telahDibayar != null && pr.telahDibayar > 0.1 ? "Sudah" : "Belum");
				xr.createCell(7).setCellValue(pr.telahDibayar == null || pr.telahDibayar < 0.01 ? ""
						: Common.numberFormat.get().format(pr.telahDibayar));
				xr.createCell(8).setCellValue(Common.numberFormat.get().format(pr.akumulasiTagihan));
				xr.createCell(9).setCellValue(Common.numberFormat.get().format(pr.totalBelum));
				xr.createCell(10).setCellValue(Common.numberFormat.get().format(pr.totalTerbayar));
			}
			Common.setStyled(sheet);
			fileOut = new FileOutputStream(filename);
			workbook.write(fileOut);
			ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("pembayaran_mahasiswa_per_bulan",
					"Rekap Pembayaran Siap", "Tabel pratinjau dan file Excel berhasil dibuat.", 100);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			closeFileOutput(fileOut);
			closeOpenedSession(session);
			ais.ui.util.KeuanganDashboardEnhanceUtil.hideFloatingProgress("pembayaran_mahasiswa_per_bulan");
		}
	}

	/**
	 * Ringkasan visual (HTML/CSS, tanpa JFreeChart) di atas tabel: kartu angka, tren tagihan vs
	 * pembayaran per bulan, dan perbandingan sudah/belum dibayar. Memakai {@link ais.ui.util.DashboardUiKit}
	 * agar tampilan konsisten dan mudah dipelihara.
	 */
	private String buildVisualPembayaran(List<PreviewPembayaranRow> rows) {
		double totalTagihan = 0;
		double totalDibayar = 0;
		int jml = 0;
		int sudah = 0;
		java.util.LinkedHashMap<String, Double> tagihanPerBulan = new java.util.LinkedHashMap<String, Double>();
		java.util.LinkedHashMap<String, Double> dibayarPerBulan = new java.util.LinkedHashMap<String, Double>();
		if (rows != null) {
			for (int i = 0; i < rows.size(); i++) {
				PreviewPembayaranRow r = rows.get(i);
				if (r == null) {
					continue;
				}
				double tag = r.nominal == null ? 0 : r.nominal.doubleValue();
				double bay = r.telahDibayar == null ? 0 : r.telahDibayar.doubleValue();
				if (bay < 0) {
					bay = 0;
				}
				totalTagihan += tag;
				totalDibayar += bay;
				jml++;
				if (bay > 0.1) {
					sudah++;
				}
				String bln = (r.bulan == null || r.bulan.trim().length() == 0 || r.bulan.trim().equals("-"))
						? "Tanpa Bulan" : r.bulan.trim();
				Double t0 = tagihanPerBulan.get(bln);
				tagihanPerBulan.put(bln, Double.valueOf((t0 == null ? 0 : t0.doubleValue()) + tag));
				Double d0 = dibayarPerBulan.get(bln);
				dibayarPerBulan.put(bln, Double.valueOf((d0 == null ? 0 : d0.doubleValue()) + bay));
			}
		}
		double belum = totalTagihan - totalDibayar;
		if (belum < 0) {
			belum = 0;
		}
		int persen = totalTagihan <= 0 ? 0 : (int) Math.round(totalDibayar * 100.0 / totalTagihan);

		java.util.List<ais.ui.util.DashboardUiKit.Stat> stats = new java.util.ArrayList<ais.ui.util.DashboardUiKit.Stat>();
		stats.add(new ais.ui.util.DashboardUiKit.Stat("Total Tagihan", ais.ui.util.DashboardUiKit.money(totalTagihan),
				"Seluruh kewajiban yang harus dibayar", ais.ui.util.DashboardUiKit.PRIMARY));
		stats.add(new ais.ui.util.DashboardUiKit.Stat("Sudah Dibayar", ais.ui.util.DashboardUiKit.money(totalDibayar),
				persen + "% dari total tagihan", ais.ui.util.DashboardUiKit.GOOD));
		stats.add(new ais.ui.util.DashboardUiKit.Stat("Belum Dibayar", ais.ui.util.DashboardUiKit.money(belum),
				"Sisa yang masih harus dilunasi", ais.ui.util.DashboardUiKit.BAD));
		stats.add(new ais.ui.util.DashboardUiKit.Stat("Item Lunas", sudah + " / " + jml,
				"Banyaknya rincian biaya yang sudah dibayar", ais.ui.util.DashboardUiKit.ACCENT));

		java.util.List<String> labels = new java.util.ArrayList<String>(tagihanPerBulan.keySet());
		java.util.List<Double> sTag = new java.util.ArrayList<Double>();
		java.util.List<Double> sBay = new java.util.ArrayList<Double>();
		for (int i = 0; i < labels.size(); i++) {
			sTag.add(tagihanPerBulan.get(labels.get(i)));
			sBay.add(dibayarPerBulan.get(labels.get(i)));
		}

		java.util.LinkedHashMap<String, Double> komposisi = new java.util.LinkedHashMap<String, Double>();
		komposisi.put("Sudah Dibayar", Double.valueOf(totalDibayar));
		komposisi.put("Belum Dibayar", Double.valueOf(belum));

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='padding:4px 2px 12px;'>");
		sb.append(ais.ui.util.DashboardUiKit.cards(stats));
		sb.append(ais.ui.util.DashboardUiKit.openGrid(320));
		sb.append(ais.ui.util.DashboardUiKit.dualLineChart("Tren Tagihan vs Pembayaran per Bulan",
				"Melihat berapa tagihan dan berapa yang sudah dibayar pada tiap bulan.", labels, sTag, "Tagihan",
				ais.ui.util.DashboardUiKit.PRIMARY, sBay, "Dibayar", ais.ui.util.DashboardUiKit.GOOD));
		sb.append(ais.ui.util.DashboardUiKit.donut("Sudah Dibayar vs Belum",
				"Seberapa besar bagian yang sudah dibayar dibanding yang masih belum.", komposisi, true,
				"Belum ada data"));
		sb.append(ais.ui.util.DashboardUiKit.closeGrid());
		sb.append("</div>");
		return sb.toString();
	}

	private void renderPreviewGrid(List<PreviewPembayaranRow> data) {
		if (center == null) {
			return;
		}
		try {
			center.getChildren().clear();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		org.zkoss.zul.Div pembungkusVisual = new org.zkoss.zul.Div();
		pembungkusVisual.setWidth("100%");
		pembungkusVisual.setStyle("height:100%;overflow:auto;box-sizing:border-box;");
		pembungkusVisual.setParent(center);

		try {
			pembungkusVisual.appendChild(new Html(buildVisualPembayaran(data)));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setSclass("dgrid");
		grid.setParent(pembungkusVisual);
		Columns cols = new Columns();
		cols.setParent(grid);
		new MyColumnConfig("NIM").setParent(cols);
		new MyColumnConfig("Nama").setParent(cols);
		new MyColumnConfig("Bulan").setParent(cols);
		new MyColumnConfig("Semester").setParent(cols);
		new MyColumnConfig("Item Biaya").setParent(cols);
		new MyColumnConfig("Nominal").setParent(cols);
		new MyColumnConfig("Status").setParent(cols);
		new MyColumnConfig("Dibayar").setParent(cols);
		new MyColumnConfig("Sisa Akumulasi").setParent(cols);
		Rows rows = new Rows();
		rows.setParent(grid);
		if (data == null || data.isEmpty()) {
			MyFormRow row = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(row, "9");
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum ada data pembayaran untuk filter ini.")));
			row.setParent(rows);
			return;
		}
		for (PreviewPembayaranRow pr : data) {
			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label(pr.nim));
			row.appendChild(new Label(pr.nama));
			row.appendChild(new Label(pr.bulan));
			row.appendChild(new Label(pr.semester));
			row.appendChild(new Label(pr.itemBiaya));
			row.appendChild(new Label(Common.numberFormat.get().format(pr.nominal)));
			row.appendChild(new Label(pr.telahDibayar != null && pr.telahDibayar > 0.1 ? "Sudah" : "Belum"));
			row.appendChild(new Label(pr.telahDibayar == null || pr.telahDibayar < 0.01 ? "-" : Common.numberFormat.get().format(pr.telahDibayar)));
			row.appendChild(new Label(Common.numberFormat.get().format(pr.akumulasiTagihan)));
		}
	}

	private static class PreviewPembayaranRow {
		private String nim;
		private String nama;
		private String bulan;
		private String semester;
		private String itemBiaya;
		private Double nominal;
		private Double telahDibayar;
		private Double akumulasiTagihan;
		private Double totalBelum = 0.0;
		private Double totalTerbayar = 0.0;

		private PreviewPembayaranRow(String nim, String nama, String bulan, String semester, String itemBiaya,
				Double nominal, Double telahDibayar, Double akumulasiTagihan) {
			this.nim = nim == null ? "" : nim;
			this.nama = nama == null ? "" : nama;
			this.bulan = bulan == null ? "" : bulan;
			this.semester = semester == null ? "" : semester;
			this.itemBiaya = itemBiaya == null ? "" : itemBiaya;
			this.nominal = nominal == null ? 0.0 : nominal;
			this.telahDibayar = telahDibayar;
			this.akumulasiTagihan = akumulasiTagihan == null ? 0.0 : akumulasiTagihan;
		}
	}

}
