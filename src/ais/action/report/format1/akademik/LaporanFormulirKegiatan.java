package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.JenisFormulirKegiatan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanFormulirKegiatan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;

	public LaporanFormulirKegiatan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Formulir Kegiatan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanFormulirKegiatan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private MyDatebox tanggal;

	private MyDatebox sampai;

	private Combobox jenisFormulirKegiatan;

	private String fileData;

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Catatan *"));
		row.appendChild(jenisFormulirKegiatan = new Combobox());
		jenisFormulirKegiatan.setWidth("90%");
		jenisFormulirKegiatan.setReadonly(true);

		jenisFormulirKegiatan.addEventListener("onChange", eventListener);

		Common.insertCombo(jenisFormulirKegiatan, new String[] { "nama", "kode" }, "keterangan",
				JenisFormulirKegiatan.class, Restrictions.eq("aktif", true));

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 1);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		tanggal = new MyDatebox(calendar.getTime());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(sampai);
		sampai.setWidth("90%");
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", eventListener);
		print.setParent(row);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (jenisFormulirKegiatan.getSelectedItem() == null
						|| jenisFormulirKegiatan.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Jenis Formulir Kegiatan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Formulir Kegiatan dari daftar dropdown; (2) Pastikan data jenis formulir sudah dikonfigurasi di sistem; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (jenisFormulirKegiatan.getSelectedItem() == null
						|| jenisFormulirKegiatan.getSelectedItem().getValue() == null) {
					return null;
				}

				JenisFormulirKegiatan j = (JenisFormulirKegiatan) jenisFormulirKegiatan.getSelectedItem().getValue();

				Map parameters = generateParameter(tanggal.getValue(), sampai.getValue(), null, j);
				if (fileData != null) {
					parameters.put("nama_laporan", fileData);
				}
				return parameters;
			}
		}, "Formulir_Kegiatan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}, false));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map initData(FormulirKegiatan formulirKegiatan, Map parameters) {
		Map map = new HashMap();
		Session session = HibernateUtil.currentSession();

		map.put("id", formulirKegiatan.getId());

		Common.insertProperty(FormulirKegiatan.class, formulirKegiatan, map, "");

		List<FormulirKegiatanPeserta> formulirKegiatanPesertas = session.createCriteria(FormulirKegiatanPeserta.class)
				.addOrder(Order.asc("kode")).addOrder(Order.asc("id"))
				.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).list();

		if (parameters != null) {
			List<Map> maps = new ArrayList<Map>();
			for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
				Map mapsub = new HashMap();
				Common.insertProperty(FormulirKegiatanPeserta.class, formulirKegiatanPeserta, mapsub, "", 2,
						"formulirKegiatan");
				maps.add(mapsub);
			}
			parameters.put("maps", maps);
		} else {
			for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
				Common.insertProperty(FormulirKegiatanPeserta.class, formulirKegiatanPeserta, map,
						formulirKegiatanPeserta.getKode());
			}
		}

		return map;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Date tanggal, Date sampai, FormulirKegiatan formulirKegiatana,
			JenisFormulirKegiatan j) throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRandStringSerializable();

		if (j.getId() != null) {
			try {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
				List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
						.addOrder(Order.asc("id")).add(Restrictions.eq("ref", j.getId()))
						.add(Restrictions.ilike("jenis", "Formulir_Kegiatan_", MatchMode.START)).list();
				int index = 0;
				for (LampiranLain lampiran : lampiranLains) {
					File f = lampiran.ambilFile();
					if (f != null & f.exists()) {
						parameters.put("file_" + (++index), f.getAbsolutePath());
					}
				}

				StreamingHibernateUtil.getInstance().closeSession();

			} catch (Exception e1) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/report/format1/akademik/LaporanFormulirKegiatan.java:252");
			}
		}

		parameters.put("tanggal", tanggal);
		parameters.put("sampai", sampai);

		if (sampai != null) {
			parameters.put("sampai.formated1", Common.dateFormat6.get().format(sampai));
			parameters.put("sampai.formated2", Common.dateFormat2.get().format(sampai));
			parameters.put("sampai.formated3", Common.dateFormat51.get().format(sampai));
			parameters.put("sampai.formated4", Common.timeFormat.get().format(sampai));
			parameters.put("sampai.formated5", Common.dateFormat1.get().format(sampai));
		}

		parameters.put("tanggal.formated1", Common.dateFormat6.get().format(tanggal));
		parameters.put("tanggal.formated2", Common.dateFormat2.get().format(tanggal));
		parameters.put("tanggal.formated3", Common.dateFormat51.get().format(tanggal));
		parameters.put("tanggal.formated4", Common.timeFormat.get().format(tanggal));
		parameters.put("tanggal.formated5", Common.dateFormat1.get().format(tanggal));

		parameters.put("jenisFormulirKegiatan", j.getNama());

		if (formulirKegiatana != null) {
			initData(formulirKegiatana, parameters);
		} else {
			List<Map> maps = new ArrayList<Map>();
			List<FormulirKegiatan> formulirKegiatans = HibernateUtil.currentSession()
					.createCriteria(FormulirKegiatan.class)
					.add(Restrictions
							.sqlRestriction("date(waktu) between date('" + Common.databaseDateFormat.get().format(tanggal)
									+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
					.add(Restrictions.eq("jenisFormulirKegiatan", j)).addOrder(Order.asc("waktu")).list();

			for (FormulirKegiatan formulirKegiatan : formulirKegiatans) {
				maps.add(initData(formulirKegiatan, null));
			}
			parameters.put("maps", maps);
		}

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onKHS(Event event) throws Exception {

		try {

			JenisFormulirKegiatan j = (JenisFormulirKegiatan) (jenisFormulirKegiatan.getSelectedItem() == null ? null
					: jenisFormulirKegiatan.getSelectedItem().getValue());
			if (j == null) {
				return;
			}

			LampiranLain lainMahapegawai = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORMULIR_KEGIATAN);

			if (lainMahapegawai == null) {
				MyMessageboxConfig.show("Mohon maaf, file template laporan formulir kegiatan belum diupload. Langkah yang dapat dilakukan: (1) Buka menu Konfigurasi Laporan dan upload file JRXML untuk jenis formulir kegiatan ini; (2) Pastikan file template sudah sesuai format yang didukung sistem; (3) Ulangi proses cetak setelah file diupload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			File file = Report.generateCompileFileReport(Report.PDF,
					generateParameter(tanggal.getValue(), sampai.getValue(), null, j),
					fileData = lainMahapegawai.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Formulir Kegiatan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void cetak(final FormulirKegiatan formulirKegiatan) throws Exception {

		try {

			JenisFormulirKegiatan j = formulirKegiatan.getJenisFormulirKegiatan();
			if (j == null) {

				return;
			}

			LampiranLain lainMahapegawai = LampiranLain.ambil(j.getId(),
					LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_FORMULIR_KEGIATAN);

			if (lainMahapegawai == null) {
//				MyMessageboxConfig.show("File template form formulir kegiatan belum diupload", "Peringatan",
//						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			Map parameters = generateParameter(formulirKegiatan.getTanggal_dirubah(),
					formulirKegiatan.getTanggal_dirubah(), formulirKegiatan,
					formulirKegiatan.getJenisFormulirKegiatan());

			final String fileData;
			File file = Report.generateCompileFileReport(Report.PDF, parameters,
					fileData = lainMahapegawai.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

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
						Map parameters = generateParameter(formulirKegiatan.getTanggal_dirubah(),
								formulirKegiatan.getTanggal_dirubah(), formulirKegiatan,
								formulirKegiatan.getJenisFormulirKegiatan());
						if (fileData != null) {
							parameters.put("nama_laporan", fileData);
						}
						return parameters;
					}
				}, LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORM_FORMULIR_KEGIATAN, null, null));
			}

			window.setVisible(true);
			window.onModal();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Formulir Kegiatan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}
}
