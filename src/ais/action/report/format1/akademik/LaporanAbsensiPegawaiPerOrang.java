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
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.rab.SatuanKerja;
import ais.database.util.ParallelTaskExecutor;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanAbsensiPegawaiPerOrang extends MyWindow {

	private static final long serialVersionUID = -397946194166101691L;

	private static final String LABEL_ABAIKAN_KEHADIRAN_JIKA_HARI_TIDAK_TERPILIH =
			"Abaikan kehadiran jika hari tidak terpilih";
	private static final String KETERANGAN_ABAIKAN_KEHADIRAN_JIKA_HARI_TIDAK_TERPILIH =
			"Jika pilihan ini dicentang, sistem hanya akan menampilkan dan menghitung data pada hari yang dipilih "
					+ "di bagian Hari Aktif. Data kehadiran pada hari yang tidak dipilih akan diabaikan walaupun pegawai "
					+ "memiliki catatan hadir pada tanggal tersebut. Gunakan pilihan ini apabila laporan hanya ingin "
					+ "dibatasi pada hari kerja atau hari aktif yang dipilih.";

	private AmbilDataPegawaiBanbox searchparent;
	private MyCheckboxConfig[] haris;

	private Center center;

	private Toolbar toolbar;

	private Pegawai pegawai;

	private MyCheckboxConfig hanyaDosen;

	private MyCheckboxConfig hanyaPegawai;

	private Combobox ikatanDinasDosen;

	private MyDatebox mulai;

	private MyDatebox sampai;

	private AmbilDataSatuanKerjaBanbox searchSatker;

	@SuppressWarnings("rawtypes")
	protected List maps = null;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private boolean vertical = true;

	private MyCheckboxConfig hanyaGuru;

	private MyCheckboxConfig abaikanKehadiranJikaHariTidakTerpilih;

	private Desktop desktop;

	public LaporanAbsensiPegawaiPerOrang() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiPegawaiPerOrang(Pegawai pegawai) {
		this(pegawai, true);
	}

	public LaporanAbsensiPegawaiPerOrang(Pegawai pegawai, boolean vertical) {
		super();
		this.pegawai = pegawai;
		this.vertical = vertical;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensiPegawaiPerOrang(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		MyGrid grid = new MyGrid();
		Rows rows = new Rows();
		rows.setParent(grid);

		if (!vertical) {

			searchSatker = new AmbilDataSatuanKerjaBanbox();
			searchSatker.setWidth("90%");
			searchSatker.setReadonly(true);

			searchparent = new AmbilDataPegawaiBanbox();
			searchparent.setWidth("90%");
			searchparent.setReadonly(true);

			hanyaDosen = new MyCheckboxConfig("Hanya dosen saja");
			hanyaGuru = new MyCheckboxConfig("Hanya guru saja");
			hanyaPegawai = new MyCheckboxConfig("Hanya pegawai, bukan dosen dan guru");
			ikatanDinasDosen = new Combobox();
			Common.insertComboDanSemua(ikatanDinasDosen, "nama", IkatanKerjaDosen.class,
					Restrictions.eq("aktif", true));

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			grid.setWidth("100%");
			grid.setParent(north);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("15%");
			column.setParent(columns);
			column = new MyColumnConfig();
			column.setParent(columns);

			column = new MyColumnConfig();
			column.setWidth("15%");
			column.setParent(columns);
			column = new MyColumnConfig();
			column.setParent(columns);

			int tanggalMulaiAbsensi = 1;
			try {
				tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerOrang.java:191");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) - 1);
			calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
			row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
			mulai.setReadonly(true);

			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) + 1);
			calendarUtama.set(Calendar.DATE, calendarUtama.get(Calendar.DATE) - 1);
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
			row.appendChild(sampai = new MyDatebox(calendarUtama.getTime()));
			sampai.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Hari Aktif"));
			row.appendChild(new ais.ui.util.MyLabelConfig(""));

			String hariDefaultTidakAktif = Common.getKonfigurasi("hari_default_tidak_aktif", ",1,7,").getNilai();

			haris = new MyCheckboxConfig[Common.haris.length];
			int hari = 1;
			for (String h : Common.haris) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				row.appendChild(haris[hari - 1] = new MyCheckboxConfig(h));
				haris[hari - 1].setChecked(!hariDefaultTidakAktif.contains("," + hari + ","));
				haris[hari - 1].setValue(h);
				haris[hari - 1].setAttribute("hari", hari);
				hari++;
			}

			initPilihanAbaikanKehadiranJikaHariTidakTerpilih(rows, true);

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Ajukan Cuti", "/img/invoice-icon_surat.png");
			print.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyWindow laporan = new MyWindow();
					laporan.setHeight("99%");
					laporan.setWidth("99%");
					laporan.setTitle("Data Pengajuan Cuti");
					laporan.setClosable(true);
					laporan.setBorder("none");

					Borderlayout borderlayout = new Borderlayout();
					laporan.appendChild(borderlayout);

					Center center = new Center();
					ais.ui.util.ZkCompat.setFlex(center, true);
					center.setParent(borderlayout);

					center.appendChild(new Iframe("/pages/master/payroll/cuti_dan_izin.zul"));
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporan);
					laporan.onModal();
				}
			});
			print.setParent(hbox);

			print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
			print.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onKHS(event);
				}
			});
			print.setParent(hbox);

		} else {

			West west = new West();
			west.setTitle("Menu");
			west.setCollapsible(true);
			west.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(west, true);
			west.setWidth("350px");

			grid.setWidth("100%");
			grid.setParent(west);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);
			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("20%");
			column.setParent(columns);
			column = new MyColumnConfig();
			column.setParent(columns);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setVisible(pegawai == null);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Satuan Kerja")));
			row.appendChild(searchSatker = new AmbilDataSatuanKerjaBanbox());
			searchSatker.setWidth("90%");
			searchSatker.setReadonly(true);

			row = new MyFormRow();
			row.setVisible(pegawai == null);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("Pegawai")));
			row.appendChild(searchparent = new AmbilDataPegawaiBanbox(true));
			searchparent.setWidth("90%");
			searchparent.setReadonly(true);

			if (pegawai == null) {
				Common.initKeterangan(rows, "Kosongkan data pegawai untuk mencetak data semua pegawai");
			}

			boolean tampilanPilihanHanyaDosenDanGuruSaja = Common.bolehKonfigurasi("tampilan_pilihan_hanya_dosen_dan_guru_saja");

			row = new MyFormRow();
			row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(hanyaDosen = new MyCheckboxConfig("Hanya dosen saja"));

			row = new MyFormRow();
			row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(hanyaGuru = new MyCheckboxConfig("Hanya guru saja"));

			row = new MyFormRow();
			row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			row.appendChild(hanyaPegawai = new MyCheckboxConfig("Hanya pegawai, bukan dosen dan guru"));

			row = new MyFormRow();
			row.setVisible(pegawai == null && tampilanPilihanHanyaDosenDanGuruSaja);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Ikatan Kerja"));
			row.appendChild(ikatanDinasDosen = new Combobox());
			Common.insertComboDanSemua(ikatanDinasDosen, "nama", IkatanKerjaDosen.class,
					Restrictions.eq("aktif", true));

			int tanggalMulaiAbsensi = 1;
			try {
				tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerOrang.java:349");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) - 1);
			calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
			row.appendChild(mulai = new MyDatebox(calendarUtama.getTime()));
			mulai.setReadonly(true);

			calendarUtama.set(Calendar.MONTH, calendarUtama.get(Calendar.MONTH) + 1);
			calendarUtama.set(Calendar.DATE, calendarUtama.get(Calendar.DATE) - 1);
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
			row.appendChild(sampai = new MyDatebox(calendarUtama.getTime()));
			sampai.setReadonly(true);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Hari Aktif"));
			row.appendChild(new ais.ui.util.MyLabelConfig(""));

			String hariDefaultTidakAktif = Common.getKonfigurasi("hari_default_tidak_aktif", ",1,7,").getNilai();

			haris = new MyCheckboxConfig[Common.haris.length];
			int hari = 1;
			for (String h : Common.haris) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				row.appendChild(haris[hari - 1] = new MyCheckboxConfig(h));
				haris[hari - 1].setChecked(!hariDefaultTidakAktif.contains("," + hari + ","));
				haris[hari - 1].setValue(h);
				haris[hari - 1].setAttribute("hari", hari);
				hari++;
			}

			initPilihanAbaikanKehadiranJikaHariTidakTerpilih(rows, false);

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig());
			MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
			print.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onKHS(event);
				}
			});
			print.setParent(row);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Laporan_Absensi_Per_Pegawai", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		}));

		if (pegawai != null) {
			onKHS(null);
		}
	}


	private void initPilihanAbaikanKehadiranJikaHariTidakTerpilih(Rows rows, boolean empatKolom) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		if (empatKolom) {
			ais.ui.util.ZkCompat.setSpans(row, "4");
		} else {
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
		}
		abaikanKehadiranJikaHariTidakTerpilih = new MyCheckboxConfig(
				LABEL_ABAIKAN_KEHADIRAN_JIKA_HARI_TIDAK_TERPILIH);
		abaikanKehadiranJikaHariTidakTerpilih.setChecked(false);
		row.appendChild(abaikanKehadiranJikaHariTidakTerpilih);

		row = new MyFormRow();
		row.setParent(rows);
		if (empatKolom) {
			ais.ui.util.ZkCompat.setSpans(row, "4");
		} else {
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
		}
		Label keterangan = new Label(KETERANGAN_ABAIKAN_KEHADIRAN_JIKA_HARI_TIDAK_TERPILIH);
		keterangan.setStyle("font-size:11px;color:#555;white-space:normal;line-height:16px;display:block;");
		row.appendChild(keterangan);
	}

	private boolean isAbaikanKehadiranJikaHariTidakTerpilih() {
		return abaikanKehadiranJikaHariTidakTerpilih != null
				&& abaikanKehadiranJikaHariTidakTerpilih.isChecked();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();

		int i = 0;
		for (MyCheckboxConfig checkbox : haris) {
			Integer hari = (Integer) checkbox.getAttribute("hari");
			parameters.put("hari" + i, checkbox.isChecked() ? hari : -1);
			i++;
		}
		if (maps != null) {
			parameters.put("maps", maps);
		}
		parameters.put("abaikanKehadiranJikaHariTidakTerpilih",
				Boolean.valueOf(isAbaikanKehadiranJikaHariTidakTerpilih()));
		parameters.put("mulai", Common.dateFormat1.get().format(mulai.getValue()));
		parameters.put("sampai", Common.dateFormat1.get().format(sampai.getValue()));

		return parameters;
	}

	@SuppressWarnings({ })
	public void generateDataDanImageAlbum(Label label) throws Exception {

		SatuanKerja parent = (SatuanKerja) searchSatker.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null && pegawai == null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Pegawai peg = pegawai == null ? (Pegawai) searchparent.getAttribute("pegawai") : pegawai;
		Session session = null;
		List<Pegawai> pegawais = new ArrayList<Pegawai>();

		// 1. MANAJEMEN SESSION KETAT (Mencegah Memory Leak)
		try {
			session = ais.action.report.Report.openNativeSession();
			pegawais = ConstantValues.simpleList(
					session.createCriteria(Pegawai.class)
							.add(Restrictions.eq("statusPegawai", ConstantValues.AKTIF_PEGAWAI))
							.createAlias("tipePegawai", "tipePegawai", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("tipePegawai.masukPresensi"),
									Restrictions.eq("tipePegawai.masukPresensi", true)))
							.add(peg != null ? Restrictions.sqlRestriction("true")
									: satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
											: Restrictions.or(
													parent == null ? Restrictions.isNull("satuanKerja")
															: Restrictions.sqlRestriction("false"),
													Restrictions.in("satuanKerja", satuanKerjas)))
							.add(peg != null ? Restrictions.sqlRestriction("true")
									: hanyaDosen.isChecked() ? Restrictions.isNotNull("dosen")
											: Restrictions.sqlRestriction("true"))
							.add(peg != null ? Restrictions.sqlRestriction("true")
									: hanyaGuru.isChecked() ? Restrictions.isNotNull("guru")
											: Restrictions.sqlRestriction("true"))
							.add(peg != null ? Restrictions.sqlRestriction("true")
									: hanyaPegawai.isChecked()
											? Restrictions.and(Restrictions.isNull("dosen"),
													Restrictions.isNull("guru"))
											: Restrictions.sqlRestriction("true"))
							.add(peg != null ? Restrictions.sqlRestriction("true")
									: ikatanDinasDosen.getSelectedItem() == null
											|| ikatanDinasDosen.getSelectedItem().getValue() == null
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("ikatanKerjaDosen",
															ikatanDinasDosen.getSelectedItem().getValue()))
							.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).add(
									peg == null ? Restrictions.sqlRestriction("true")
											: Restrictions.or(
													peg.getGuru() == null ? Restrictions.sqlRestriction("false")
															: Restrictions.eq("guru.id", peg.getGuru().getId()),
													Restrictions.or(
															peg.getDosen() == null
																	? Restrictions.sqlRestriction("false")
																	: Restrictions.eq("dosen.id",
																			peg.getDosen().getId()),
															Restrictions.eq("id", peg.getId()))))
							.addOrder(Order.asc("dosen")).addOrder(Order.asc("satuanKerja"))
							.addOrder(Order.asc("nama")),
					Pegawai.class);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerOrang.java:547");
			throw e;
		} finally {
			ais.action.report.Report.closeNativeSession(session);
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		// 2. LEMPAR KE FUNGSI STATIC
		maps = data(pegawais, mulai.getValue(), sampai.getValue(), label, haris, desktop,
				isAbaikanKehadiranJikaHariTidakTerpilih());

		if (label != null) {
			ais.action.report.helper.LoadingReportUtil.selesai(label);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List data(final List<Pegawai> pegawais, final Date mulai, final Date sampai, final Label label,
			final MyCheckboxConfig[] haris, final Desktop desktop) throws Exception {
		return data(pegawais, mulai, sampai, label, haris, desktop, false);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List data(final List<Pegawai> pegawais, final Date mulai, final Date sampai, final Label label,
			final MyCheckboxConfig[] haris, final Desktop desktop,
			final boolean abaikanKehadiranJikaHariTidakTerpilih) throws Exception {

		Session session = null;
		List<CutiDanIzin> cutiDanIzinsSemua = new ArrayList<CutiDanIzin>();
		Map<String, StatuskehadiranKaryawanHarian> statusHarianMap = new HashMap<String, StatuskehadiranKaryawanHarian>();

		// 1. MANAJEMEN SESSION KETAT PADA METODE PEKERJA
		try {
			session = ais.action.report.Report.openNativeSession();
			if (!pegawais.isEmpty()) {
				cutiDanIzinsSemua = session.createCriteria(CutiDanIzin.class)
						.add(Restrictions.and(Restrictions.le("mulai", sampai),
								Restrictions.ge("sampai", mulai)))
						.addOrder(Order.asc("mulai")).add(Restrictions.in("pegawai", pegawais))
						.add(Restrictions.eq("setujui", true)).list();

				statusHarianMap = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(cutiDanIzinsSemua, mulai,
						sampai, pegawais, session, true);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerOrang.java:592");
			throw e;
		} finally {
			ais.action.report.Report.closeNativeSession(session);
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		// 2. SETUP VARIABEL MULTI-THREAD & SERVER PUSH
		final Map<String, StatuskehadiranKaryawanHarian> statuskehadiranKaryawanHarians = statusHarianMap;

		// =========================================================================
		// [OPTIMASI 1]: CACHING HOLIDAY DI MEMORI (SANGAT KRUSIAL)
		// Mencegah N+1 Query Database untuk fungsi Common.isHoliday di dalam ParallelThread.
		// Fungsi ini dijalankan 1 kali saja untuk mengumpulkan daftar hari libur.
		// =========================================================================
		final Map<String, Boolean> holidayCache = new HashMap<String, Boolean>();
		Calendar calCache = ais.ui.util.WaktuUtil.getCalendar();
		calCache.setTime(mulai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(calCache);
		Calendar endCache = ais.ui.util.WaktuUtil.getCalendar();
		endCache.setTime(sampai);
		ais.action.report.helper.LaporanTanggalUtil.normalisasiJamAwalHari(endCache);
		
		while (calCache.compareTo(endCache) <= 0) {
			String keyCache = Common.dateFormat83.get().format(calCache.getTime());
			holidayCache.put(keyCache, Common.isHoliday(calCache.getTime()));
			calCache.add(Calendar.DATE, 1);
		}

		// MENGAMANKAN URUTAN (ORDER PRESERVING ARRAY)
		final int pegSize = pegawais.size();
		final List[] orderedDailyMaps = new List[pegSize];
		List<Integer> listIndex = new ArrayList<Integer>();
		for (int i = 0; i < pegSize; i++) {
			listIndex.add(i);
		}

		// Optimasi Perhitungan Progres Bar
		int countValidPegawai = 0;
		for (Pegawai p : pegawais) {
			if (p != null && p.getTipePegawai() != null && !p.getTipePegawai().getMasukPresensi())
				continue;
			countValidPegawai++;
		}

		int countDays = 0;
		if (mulai != null && sampai != null) {
			countDays = ais.action.report.helper.LaporanTanggalUtil.jumlahHariInklusif(mulai, sampai);
		}

		final int totalTasks = countValidPegawai * countDays;
		final java.util.concurrent.atomic.AtomicInteger progressCounter = new java.util.concurrent.atomic.AtomicInteger(
				0);
		final Date sekarang = ais.ui.util.WaktuUtil.getDate();

		// 3. EKSEKUSI PROSES SECARA PARALEL (Max 100 Thread) BERDASARKAN INDEX
		ParallelTaskExecutor.process(listIndex, ParallelTaskExecutor.getDefaultReportMaxThreads(), new ParallelTaskExecutor.Task<Integer>() {
			@Override
			public void execute(final Integer idx) throws Exception {

				Pegawai pegawai = pegawais.get(idx);

				if (pegawai != null && pegawai.getTipePegawai() != null
						&& !pegawai.getTipePegawai().getMasukPresensi()) {
					return;
				}

				List<Map> myDailyMaps = new ArrayList<Map>();
				long tidakHadir = 0L;
				long tidakHadirTanpaHoliday = 0L;

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(mulai);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);

				Calendar s = ais.ui.util.WaktuUtil.getCalendar();
				s.setTime(sampai);
				s.set(Calendar.HOUR_OF_DAY, 0);
				s.set(Calendar.MINUTE, 0);
				s.set(Calendar.SECOND, 0);
				s.set(Calendar.MILLISECOND, 0);

				while (calendar.compareTo(s) <= 0) {
					final int currIdx = progressCounter.incrementAndGet();
					Date tanggal = calendar.getTime();
					Integer hari = calendar.get(Calendar.DAY_OF_WEEK);

					// =========================================================================
					// [OPTIMASI 2]: MENGURANGI UI BOTTLENECK 
					// Modulo diperbesar ke 100 agar ServerPush tidak memblokir antrean thread
					// =========================================================================
					if (label != null && desktop != null) {
						if (currIdx % 100 == 0 || currIdx == totalTasks) {
							try {
								org.zkoss.zk.ui.Executions.activate(desktop);
								try {
									String tglVal = Common.dateFormat6.get().format(tanggal);
									label.setValue(
											"Memproses data " + pegawai.getNama() + " tanggal " + tglVal + " ("
													+ Common.numberFormat.get().format(
															(currIdx * 100.0) / (totalTasks == 0 ? 1 : totalTasks))
													+ "%)");
								} finally {
									org.zkoss.zk.ui.Executions.deactivate(desktop);
								}
							} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerOrang.java:700");
							}
						}
					}

					String keyTanggalStr = Common.dateFormat83.get().format(tanggal);
					String key = keyTanggalStr + "_" + pegawai.getId();

					StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = statuskehadiranKaryawanHarians
							.get(key);
					boolean adaHadir = (statuskehadiranKaryawanHarian != null
							&& statuskehadiranKaryawanHarian.getStatusabsensi() != null
							&& statuskehadiranKaryawanHarian.getStatusabsensi().getId().equals(1L));

					// Penyesuaian pointer tanggal
					calendar.add(Calendar.DATE, 1);

					if (harusLewatiTanggalKarenaHariTidakDipilih(haris, hari, adaHadir,
							abaikanKehadiranJikaHariTidakTerpilih)) {
						continue;
					}

					// AMBIL STATUS HOLIDAY DARI CACHE RAM 
					Boolean isHol = holidayCache.get(keyTanggalStr);
					boolean holiday = isHol != null ? isHol : false;

					if (statuskehadiranKaryawanHarian == null) {
						statuskehadiranKaryawanHarian = new StatuskehadiranKaryawanHarian();
						statuskehadiranKaryawanHarian.setTanggal(tanggal);
						statuskehadiranKaryawanHarian.setPegawai(pegawai);
						statuskehadiranKaryawanHarian.setKeterangan("");
						statuskehadiranKaryawanHarian.setMasukjam(null);
						statuskehadiranKaryawanHarian.setPulangJam(null);
						statuskehadiranKaryawanHarian.setMinggu(hari);

						if (tanggal.before(sekarang)) {
							statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.TIDAK_ADA_ALASAN);
						} else {
							statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.BELUM_ABSEN);
						}
					}

					if (statuskehadiranKaryawanHarian.isTidakHadirTanpaHoliday(adaHadir,
							statuskehadiranKaryawanHarian.getCutiDanIzin(),
							statuskehadiranKaryawanHarian.getLiburNasional())) {
						tidakHadirTanpaHoliday++;
					}

					if (statuskehadiranKaryawanHarian.isTidakHadirEfektif(adaHadir, holiday,
							statuskehadiranKaryawanHarian.getCutiDanIzin(),
							statuskehadiranKaryawanHarian.getLiburNasional())) {
						tidakHadir++;
					}

					Map map = new java.util.HashMap();
					map.put("foto_datang", statuskehadiranKaryawanHarian.getFotoAbsenDatang());
					map.put("foto_pulang", statuskehadiranKaryawanHarian.getFotoAbsenPulang());
					map.put("lokasi_datang", statuskehadiranKaryawanHarian.getLokasiAbsenDatang());
					map.put("lokasi_pulang", statuskehadiranKaryawanHarian.getLokasiAbsenPulang());
					map.put("apakah_dosen", pegawai.getDosen() != null);
					map.put("nama_satuan_kerja",
							pegawai.getSatuanKerja() == null ? "" : pegawai.getSatuanKerja().getNama());
					map.put("pegawai", pegawai.getId());
					map.put("nama", pegawai.getNama());
					map.put("nip", pegawai.getMycode());

					Statusabsensi statusabsensi = statuskehadiranKaryawanHarian.getStatusabsensi();
					if (ConstantValues.kehadiranHarusMulaiDanSampai) {
						if (statuskehadiranKaryawanHarian.getMasukjam() == null
								|| statuskehadiranKaryawanHarian.getPulangJam() == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}
					}
					map.put("keterangan", (statusabsensi == null ? "" : statusabsensi.getNama()) + " "
							+ statuskehadiranKaryawanHarian.getKeterangan());
					map.put("jumlahTerlambat", statuskehadiranKaryawanHarian.getJumlahTerlambat());
					map.put("jumlahCepatKeluar", statuskehadiranKaryawanHarian.getJumlahCepatKeluar());

					Double lemburDa = statuskehadiranKaryawanHarian.getJumlahLemburMasuk();
					Date lemburMulai = statuskehadiranKaryawanHarian.getLamburMulai();
					Date lemburSampai = statuskehadiranKaryawanHarian.getLamburSampai();
					map.put("jumlahLemburMasuk", lemburDa);

					map.put("masuk", statuskehadiranKaryawanHarian.ambilMasukjam() == null ? ""
							: Common.timeFormat.get().format(statuskehadiranKaryawanHarian.ambilMasukjam()));
					map.put("pulang", statuskehadiranKaryawanHarian.ambilPulangjam() == null ? ""
							: Common.timeFormat.get().format(statuskehadiranKaryawanHarian.ambilPulangjam()));
					map.put("lemburMulai", lemburMulai == null ? "" : Common.timeFormat.get().format(lemburMulai));
					map.put("lemburSampai", lemburSampai == null ? "" : Common.timeFormat.get().format(lemburSampai));
					map.put("hari", Common.dateFormat6.get().format(tanggal));
					map.put("tanggal", Common.dateFormat1.get().format(tanggal));
					map.put("jam_kerja", statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai() == null ? ""
							: Common.timeFormat.get()
									.format(statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().getMulai())
									+ " - " + Common.timeFormat.get().format(
											statuskehadiranKaryawanHarian.getDetailJenisShiftPegawai().getSampai()));

					map.put("tidakHadir", tidakHadir);
					map.put("tidakHadirTanpaHoliday", tidakHadirTanpaHoliday);

					myDailyMaps.add(map);
				}

				// Alokasikan map harian milik pegawai ini ke index utamanya
				orderedDailyMaps[idx] = myDailyMaps;
			}
		});

		// 4. MENGGABUNGKAN HASIL AKHIR DENGAN URUTAN YANG BENAR (Flattening)
		List finalMaps = new ArrayList();
		for (List<Map> dailyMaps : orderedDailyMaps) {
			if (dailyMaps != null && !dailyMaps.isEmpty()) {
				finalMaps.addAll(dailyMaps);
			}
		}

		// Membersihkan memori
		holidayCache.clear();
		statusHarianMap.clear();
		cutiDanIzinsSemua.clear();

		return finalMaps;
	}

	private static boolean harusLewatiTanggalKarenaHariTidakDipilih(final MyCheckboxConfig[] haris, final Integer hari,
			final boolean adaHadir, final boolean abaikanKehadiranJikaHariTidakTerpilih) {
		if (haris == null || hari == null) {
			return false;
		}

		int indexHari = hari.intValue() - 1;
		if (indexHari < 0 || indexHari >= haris.length || haris[indexHari] == null) {
			return false;
		}

		if (haris[indexHari].isChecked()) {
			return false;
		}

		return abaikanKehadiranJikaHariTidakTerpilih || !adaHadir;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_Absensi_Per_Pegawai",
						ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		desktop = Executions.getCurrent().getDesktop();
		if (!desktop.isServerPushEnabled()) {
			desktop.enableServerPush(true);
		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanAbsensiPegawaiPerOrang.java:868");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Absensi Pegawai Per Orang", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

}