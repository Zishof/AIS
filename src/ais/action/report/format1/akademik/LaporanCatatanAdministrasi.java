package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CatatanAdministrasi;
import ais.database.model.JenisCatatanAdministrasi;
import ais.database.model.KelompokParameterTambahanCatatanAdministrasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanAdministrasi;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanCatatanAdministrasi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;

	public LaporanCatatanAdministrasi() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Catatan Administrasi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanCatatanAdministrasi(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisCatatanAdministrasi;

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisCatatanAdministrasi = new Combobox());
		jenisCatatanAdministrasi.setWidth("90%");
		jenisCatatanAdministrasi.setReadonly(true);

		Common.insertCombo(jenisCatatanAdministrasi, new String[] { "nama", "kode" }, "keterangan",
				JenisCatatanAdministrasi.class, Restrictions.eq("aktif", true));

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		jenisCatatanAdministrasi.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		tanggal = new MyDatebox(calendar.getTime());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(sampai);
		sampai.setWidth("90%");
		sampai.addEventListener("onChange", eventListener);
		sampai.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (jenisCatatanAdministrasi.getSelectedItem() == null
						|| jenisCatatanAdministrasi.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Jenis Catatan Administrasi belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Catatan Administrasi dari daftar dropdown; (2) Pastikan data jenis catatan sudah dikonfigurasi di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				JenisCatatanAdministrasi j = (JenisCatatanAdministrasi) jenisCatatanAdministrasi.getSelectedItem()
						.getValue();

				Map parameters = generateParameter(tanggal.getValue(), sampai.getValue(), null, j);
				return parameters;
			}
		}, "Catatan_Administrasi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map initData(CatatanAdministrasi catatanAdministrasi) {
		Map map = new HashMap();
		JenisCatatanAdministrasi ja = catatanAdministrasi.getJenisCatatanAdministrasi();
		Session session = HibernateUtil.currentSession();
		session.refresh(ja);

		map.put("id", catatanAdministrasi.getId());
		map.put("nama", catatanAdministrasi.getNama());
		map.put("waktu", catatanAdministrasi.getWaktu());
		map.put("keterangan", catatanAdministrasi.getKeterangan());

		for (KelompokParameterTambahanCatatanAdministrasi kelompokParameterTambahanCatatanAdministrasi : ja
				.getKelompokParameterTambahanCatatanAdministrasis()) {
			map.put("kelompok_id", kelompokParameterTambahanCatatanAdministrasi.getId());
			map.put("kelompok", kelompokParameterTambahanCatatanAdministrasi.getNama());

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanCatatanAdministrasi.class)
									.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi",
											kelompokParameterTambahanCatatanAdministrasi))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCatatanAdministrasi",
											"kelompokParameterTambahanCatatanAdministrasi")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			for (ParameterTambahan parameterTambahan : parameterTambahans) {
				String jenis = kelompokParameterTambahanCatatanAdministrasi.getId() + "->" + parameterTambahan.getId();
				String jenis_id = kelompokParameterTambahanCatatanAdministrasi.getId() + "_"
						+ parameterTambahan.getId();

				String val = "";
				String[] spl = catatanAdministrasi.getParameterTambahanInds().split("\n");
				for (String d : spl) {
					String[] value = d.split("<=>");
					if (value[0].trim().equalsIgnoreCase(jenis)) {
						val = value.length > 1 ? value[1].trim() : "";
					}
				}

				LampiranLain lampiranLain = LampiranLain.ambil(catatanAdministrasi.getId(), jenis);

				String vall = val;
				map.put("param.id." + parameterTambahan.getId(), vall);
				map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), vall);
				map.put("param.kode." + parameterTambahan.getKode(), vall);
				if (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.ANGKA)
						|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TEXT_ANGKA)) {
					try {
						Double nilai = val.trim().isEmpty() || val.trim().equalsIgnoreCase("null") ? null : Double.parseDouble(val);
						map.put("param.id." + parameterTambahan.getId(), nilai);
						map.put("param.nama." + parameterTambahan.getNama().toLowerCase(), nilai);
						map.put("param.kode." + parameterTambahan.getKode(), nilai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanCatatanAdministrasi.java:243");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Administrasi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}
				}

				map.put(jenis_id, vall);
				parameterTambahan.masukkanData(vall, jenis_id, map);

				if (lampiranLain != null) {
					map.put(jenis_id + "_url", lampiranLain.ambilFile().getAbsolutePath());
				}
			}

		}

		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Date tanggal, Date sampai, CatatanAdministrasi catatanAdministrasia,
			JenisCatatanAdministrasi j) throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();
		// EKSPOR DINAMIS: jrxml terupload per jenis dipakai utk SEMUA format cetak (PDF/XLS/DOCX/PPTX).
		if (j != null && j.getId() != null) {
			try {
				LampiranLain layoutDinamis = LampiranLain.ambil(j.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_ADMINISTRASI);
				if (layoutDinamis != null && layoutDinamis.ambilFile() != null
						&& layoutDinamis.ambilFile().exists()) {
					parameters.put("nama_laporan", layoutDinamis.ambilFile().getAbsolutePath());
				}
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanCatatanAdministrasi.java:274");
			}
		}

		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		parameters.put("jenisCatatanAdministrasi", j.getNama());

		System.out.println("parameters => " + parameters);

		List<Map> maps = new ArrayList<Map>();

		List<CatatanAdministrasi> catatanAdministrasis = HibernateUtil.currentSession()
				.createCriteria(CatatanAdministrasi.class)
				.add(Restrictions
						.sqlRestriction("date(waktu) between date('" + Common.databaseDateFormat.get().format(tanggal)
								+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
				.add(Restrictions.eq("jenisCatatanAdministrasi", j)).addOrder(Order.asc("waktu")).list();

		for (CatatanAdministrasi catatanAdministrasi : catatanAdministrasis) {
			maps.add(initData(catatanAdministrasi));
		}

		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onKHS(Event event) throws Exception {

		try {

			JenisCatatanAdministrasi j = (JenisCatatanAdministrasi) jenisCatatanAdministrasi.getSelectedItem()
					.getValue();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_ADMINISTRASI);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("Mohon maaf, file template laporan catatan administrasi belum diupload. Langkah yang dapat dilakukan: (1) Buka menu Konfigurasi Laporan dan upload file JRXML untuk jenis catatan administrasi ini; (2) Pastikan file template sudah sesuai format yang didukung sistem; (3) Ulangi proses cetak setelah file diupload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			File file = Report.generateCompileFileReport(Report.PDF,
					generateParameter(tanggal.getValue(), sampai.getValue(), null, j),
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Catatan Administrasi", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetak(final CatatanAdministrasi catatanAdministrasi) throws Exception {

		try {

			final JenisCatatanAdministrasi j = catatanAdministrasi.getJenisCatatanAdministrasi();
			if (j == null) {
				return;
			}

			LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_ADMINISTRASI);

			if (lainMahaadministrasi == null) {
				MyMessageboxConfig.show("Mohon maaf, file template form catatan administrasi belum diupload. Langkah yang dapat dilakukan: (1) Buka menu Konfigurasi Laporan dan upload file template form yang sesuai; (2) Pastikan file template sudah sesuai format yang didukung sistem; (3) Ulangi proses cetak setelah file diupload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Map parameters = generateParameter(catatanAdministrasi.getTanggal_dirubah(),
					catatanAdministrasi.getTanggal_dirubah(), catatanAdministrasi, j);

			File file = Report.generateCompileFileReport(Report.PDF, parameters,
					lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

			MyWindow window = new MyWindow("Laporan", "none", true);
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			window.setHeight("90%");
			window.setWidth("900px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(window);

			Center center = new Center();
			ais.ui.util.ZkCompat.setFlex(center, true);
			center.setParent(borderlayout);
			CommonReport.tampilkanReportPDF(center, file);

			if (parameters == null || parameters.get("tidak_tampil_pilihan_export") == null) {
				org.zkoss.zul.North north = new org.zkoss.zul.North();
				north.setParent(borderlayout);
				north.appendChild(CommonReport.exportReport(new ParameterListener() {
					@Override
					public Map generateParameters() throws Exception {
						Map parameters = generateParameter(catatanAdministrasi.getTanggal_dirubah(),
								catatanAdministrasi.getTanggal_dirubah(), catatanAdministrasi, j);
						return parameters;
					}
				}, LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_CATATAN_ADMINISTRASI, null, null));
			}

			window.setVisible(true);
			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Catatan Administrasi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}
}
