package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.payroll.util.PembayaranItemGajiPegawaiTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.employ.GajiPokok;
import ais.database.model.file.LampiranLain;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.JenisGajiPegawai;
import ais.database.model.payroll.KelompokParameterTambahanGajiPegawai;
import ais.database.model.payroll.ParameterTambahanGajiPegawai;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanSlipGajiRealPegawaiPerOrang extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private AmbilDataPegawaiBanbox searchparent;
	private Combobox bulan;

	private Center center;

	private Toolbar toolbar;

	private Combobox tahun;

	private Pegawai pegawai;

	private Integer nilaiBulan;
	private Combobox pilihFormat;
	private Integer nilaiTahun;

	private FormatItemGaji formatItemGaji = null;

	public LaporanSlipGajiRealPegawaiPerOrang() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Slip Gaji Real Pegawai Per Orang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSlipGajiRealPegawaiPerOrang(Pegawai pegawai) {
		super();
		try {
			this.pegawai = pegawai;
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Slip Gaji Real Pegawai Per Orang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSlipGajiRealPegawaiPerOrang(Pegawai pegawai, Integer bulan, Integer tahun) {
		super();
		this.pegawai = pegawai;
		this.nilaiBulan = bulan;
		this.nilaiTahun = tahun;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Slip Gaji Real Pegawai Per Orang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSlipGajiRealPegawaiPerOrang(Pegawai pegawai, FormatItemGaji formatItemGaji, Integer bulan,
			Integer tahun) {
		super();
		this.pegawai = pegawai;
		this.nilaiBulan = bulan;
		this.nilaiTahun = tahun;
		this.formatItemGaji = formatItemGaji;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Slip Gaji Real Pegawai Per Orang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSlipGajiRealPegawaiPerOrang(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {

		bulan = new Combobox();
		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);

		tahun = new Combobox();
		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, currTahun);
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Pegawai pegawai = (Pegawai) searchparent.getAttribute("pegawai");

				if (formatItemGaji != null) {
					Common.clear(pilihFormat);
					Common.selectComboItem(true, pilihFormat, formatItemGaji);
					pilihFormat.setDisabled(true);
				}

				else if (pegawai != null) {
					Common.clear(pilihFormat);
					Common.insertComboItems(pilihFormat, "nama", pegawai.ambilFormatItemGajis());
					pilihFormat.setReadonly(true);
					if (pilihFormat.getSelectedItem() == null && !pilihFormat.getChildren().isEmpty()) {
						pilihFormat.setSelectedIndex(0);
					}

					pilihFormat.setDisabled(pilihFormat.getChildren().size() == 1);

				}
				onKHS(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pegawai")));
		row.appendChild(searchparent = new AmbilDataPegawaiBanbox());
		searchparent.setWidth("90%");
		searchparent.setEventListener(eventListener);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Penggajian")));
		row.appendChild(pilihFormat = new Combobox());
		pilihFormat.setWidth("90%");
		pilihFormat.addEventListener("onChange", eventListener);
		pilihFormat.setReadonly(true);

		if (this.pegawai != null) {
			searchparent.setAttribute("pegawai", this.pegawai);
			searchparent.setValue((this.pegawai.getNama()));
			searchparent.setDisabled(true);

			if (formatItemGaji != null) {
				Common.clear(pilihFormat);
				Common.selectComboItem(true, pilihFormat, formatItemGaji);
				pilihFormat.setDisabled(true);
			}

			else {
				Common.clear(pilihFormat);
				Common.insertComboItems(pilihFormat, "nama", pegawai.ambilFormatItemGajis());
				pilihFormat.setReadonly(true);
				if (pilihFormat.getSelectedItem() == null && !pilihFormat.getChildren().isEmpty()) {
					pilihFormat.setSelectedIndex(0);
				}

				pilihFormat.setDisabled(pilihFormat.getChildren().size() == 1);
			}

		}

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Bulan")));
		row.appendChild(bulan);
		bulan.addEventListener("onChange", eventListener);
		bulan.setReadonly(true);

		if (this.nilaiBulan != null) {
			Common.selectComboItem(bulan, nilaiBulan);
			bulan.setDisabled(true);
		}

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun")));
		row.appendChild(tahun);
		tahun.addEventListener("onChange", eventListener);
		tahun.setReadonly(true);

		if (this.nilaiTahun != null) {
			Common.selectComboItem(tahun, nilaiTahun);
			tahun.setDisabled(true);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "8");

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (bulan.getSelectedItem() == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih bulan. Silakan pilih bulan terlebih dahulu sebelum menampilkan atau mencetak slip gaji.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih tahun. Silakan pilih tahun terlebih dahulu sebelum menampilkan atau mencetak slip gaji.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (searchparent.getAttribute("pegawai") == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih pegawai. Silakan pilih pegawai terlebih dahulu sebelum menampilkan atau mencetak slip gaji.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}
				FormatItemGaji formatItemGaji = (FormatItemGaji) (pilihFormat.getSelectedItem() == null ? null
						: pilihFormat.getSelectedItem().getValue());
				if (formatItemGaji == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih jenis penggajian. Silakan pilih penggajian terlebih dahulu sebelum menampilkan atau mencetak slip gaji.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "payroll/SlipGajiReal", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(null);
			}
		});

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (bulan.getSelectedItem() == null) {
			return null;
		}

		if (tahun.getSelectedItem() == null) {
			return null;
		}

		if (searchparent.getAttribute("pegawai") == null) {
			return null;
		}
		FormatItemGaji formatItemGaji = (FormatItemGaji) (pilihFormat.getSelectedItem() == null ? null
				: pilihFormat.getSelectedItem().getValue());
		if (formatItemGaji == null) {
			return null;
		}

		Integer tahun = (Integer) this.tahun.getSelectedItem().getValue();
		Integer bulan = (Integer) this.bulan.getSelectedItem().getValue();

		Pegawai pegawai = (Pegawai) searchparent.getAttribute("pegawai");

		PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai = (PembayaranGajiPunyaPegawai) HibernateUtil
				.currentSession().createCriteria(PembayaranGajiPunyaPegawai.class)
				.add(Restrictions.or(Restrictions.isNull("formatItemGaji"),
						Restrictions.eq("formatItemGaji", formatItemGaji)))
				.add(Restrictions.eq("pegawai", pegawai)).createAlias("pembayaranGaji", "pembayaranGaji")
				.add(Restrictions.eq("pembayaranGaji.bulan", bulan)).add(Restrictions.eq("pembayaranGaji.tahun", tahun))
				.setMaxResults(1).uniqueResult();

		if (pembayaranGajiPunyaPegawai == null) {

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show(
							"Mohon maaf, data slip gaji untuk pegawai, bulan, dan tahun yang dipilih belum tersedia. Langkah yang dapat dilakukan: (1) memastikan proses penggajian pada periode tersebut telah dijalankan; (2) memeriksa kembali pilihan bulan dan tahun; (3) menghubungi bagian penggajian apabila data seharusnya sudah ada.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}
			});

			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		if (formatItemGaji != null) {
			Common.insertProperty(FormatItemGaji.class, formatItemGaji, parameters, "format", 1);
		}
		Common.insertProperty(Pegawai.class, pegawai, parameters, "pegawai", 1);
		Common.insertProperty(PembayaranGaji.class, pembayaranGajiPunyaPegawai.getPembayaranGaji(), parameters,
				"pembayaranGaji", 1);

		GajiPokok gajiPokok = pegawai.ambilGajiPokok(WaktuUtil.getDate());
		if (gajiPokok != null) {
			Common.insertProperty(GajiPokok.class, gajiPokok, parameters, "gp", 1);
		}

		for (Object o : ConstantValues.ambilBerdasarClass(PenilaianKpi.class).values()) {
			PenilaianKpi penilaianKpi = (PenilaianKpi) o;
			if (penilaianKpi.getPegawai().getId().equals(pegawai.getId())) {
				Common.insertProperty(PenilaianKpi.class, penilaianKpi, parameters, "kpi_" + penilaianKpi.getTa(), 1);
			}
		}

		parameters.put("tahun", tahun == null ? -1 : tahun);
		parameters.put("bulan", bulan == null ? -1 : bulan);

		try {
			parameters.put("nama_bulan", Common.BULAN[bulan == null ? -1 : bulan - 1]);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanSlipGajiRealPegawaiPerOrang.java:400");
			// TODO: handle exception
		}

		parameters.put("nama", pegawai.getNama());
		parameters.put("nip", pegawai.getKode());
		parameters.put("kode", pegawai.getKode());
		parameters.put("norek", pegawai.getNorek() + " - " + pegawai.getDitransferKe());
		parameters.put("tanggalMasuk", pegawai.getTanggalmasuk());
		parameters.put("status", pegawai.getStatusPegawai() == null ? "" : pegawai.getStatusPegawai().getNama());
		parameters.put("departemen", pegawai.getDepartemen() == null ? "" : pegawai.getDepartemen().getNama());

		try {
			JenisGajiPegawai j = pegawai.getJenisGajiPegawai();
			if (j != null) {
				Session session = HibernateUtil.currentSession();
				session.refresh(j);

				Set<KelompokParameterTambahanGajiPegawai> kelompokParameterTambahanGajiPegawais = new TreeSet<KelompokParameterTambahanGajiPegawai>();
				for (KelompokParameterTambahanGajiPegawai kelompokParameterTambahanGajiPegawai : j
						.getKelompokParameterTambahanGajiPegawais()) {
					kelompokParameterTambahanGajiPegawais.add(kelompokParameterTambahanGajiPegawai);
				}

				for (KelompokParameterTambahanGajiPegawai kelompokParameterTambahanGajiPegawai : kelompokParameterTambahanGajiPegawais) {

					List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
							session.createCriteria(ParameterTambahanGajiPegawai.class)
									.add(Restrictions.eq("kelompokParameterTambahanGajiPegawai",
											kelompokParameterTambahanGajiPegawai))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanGajiPegawai",
											"kelompokParameterTambahanGajiPegawai")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanGajiPegawai.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);

					if (!parameterTambahans.isEmpty()) {

						for (ParameterTambahan parameterTambahan : parameterTambahans) {
							String jenis = kelompokParameterTambahanGajiPegawai.getId() + "->"
									+ parameterTambahan.getId();

							String val = "";
							String ket = "";
							String[] spl = pegawai.getParameterTambahanInds().split("\n");
							for (String d : spl) {
								String[] value = d.split("<=>");
								if (value[0].trim().equalsIgnoreCase(jenis)) {
									val = value.length > 1 ? value[1].trim() : "";
									try {
										ket = value.length > 0 ? value[value.length - 1] : "";
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanSlipGajiRealPegawaiPerOrang.java:453");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Slip Gaji Real Pegawai Per Orang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});

									}
								}
							}
							parameters.put(jenis, val);
							parameters.put(jenis + "_ket", ket);
						}
					}

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanSlipGajiRealPegawaiPerOrang.java:465");
			// TODO: handle exception
		}

		parameters.put("maps", generateDataDanImageAlbum(pembayaranGajiPunyaPegawai, parameters, bulan, tahun));
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public static List generateDataDanImageAlbum(PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai, Map maps,
			Integer bulan, Integer tahun) throws Exception {

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, tahun);
		calendar.set(Calendar.MONTH, bulan - 1);

		List list = new ArrayList();
		PembayaranItemGajiPegawaiTreeModel itemGajiPegawaiTreeModel = new PembayaranItemGajiPegawaiTreeModel(false,
				pembayaranGajiPunyaPegawai);
		itemGajiPegawaiTreeModel.populateData(list, maps, calendar.getTime());
		return list;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			if (bulan.getSelectedItem() == null) {
				return;
			}

			if (tahun.getSelectedItem() == null) {
				return;
			}

			if (searchparent.getAttribute("pegawai") == null) {
				return;
			}

			FormatItemGaji formatItemGaji = (FormatItemGaji) (pilihFormat.getSelectedItem() == null ? null
					: pilihFormat.getSelectedItem().getValue());
			if (formatItemGaji == null) {
				return;
			}

			Integer tahun = (Integer) this.tahun.getSelectedItem().getValue();
			Integer bulan = (Integer) this.bulan.getSelectedItem().getValue();

			Pegawai pegawai = (Pegawai) searchparent.getAttribute("pegawai");

			LampiranLain fileGaji = LampiranLain.ambil(pegawai.getId(), "SLIP_GAJI_" + bulan + "_" + tahun);
			System.out.println("fileGaji -> " + fileGaji);
			File fileG = fileGaji == null || fileGaji.getId() == null ? null : fileGaji.ambilFile();

			if (fileG != null) {
				toolbar.getParent().setVisible(false);

				File myFile = new File(Common.ambilREAL_PATH_REPORT() + "/" + fileG.getName());
				FileUtils.copyFile(fileG, myFile);

				CommonReport.tampilkanReportPDF(center, myFile);
			} else {
				toolbar.getParent().setVisible(true);
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "payroll/SlipGajiReal",
						ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Slip Gaji Real Pegawai Per Orang", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
