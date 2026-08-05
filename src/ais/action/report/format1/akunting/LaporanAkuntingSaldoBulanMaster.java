package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanAkuntingSaldoBulanMaster extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private MyDatebox tanggal1;
	private MyDatebox tanggal2;

	private Combobox grup;
	private Combobox kelompok;

	private Center center;
	private Toolbar toolbar;

	private Combobox nama;

	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;

	public Combobox jenisLaporan;

//	private MyCheckboxConfig rinci;

	// private MyDatebox tanggalSaldoAwal;

	protected EventListener eventListener2;

	private String fileData = null;

	public LaporanAkuntingSaldoBulanMaster() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Akunting Saldo Bulan Master", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAkuntingSaldoBulanMaster(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private List<JenisLaporan> jenisLaporans = null;

	@SuppressWarnings("unchecked")
	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("300px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai"));
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, 1);
		row.appendChild(tanggal1 = new MyDatebox(calendar.getTime()));
		tanggal1.setFormat(Common.dateFormat1.get().toPattern());
		tanggal1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Sampai"));
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		calendar.set(Calendar.DATE, 0);
		row.appendChild(tanggal2 = new MyDatebox(calendar.getTime()));
		tanggal2.setFormat(Common.dateFormat1.get().toPattern());
		tanggal2.setReadonly(true);

		Map<Long, JenisLaporan> maps = ConstantValues.ambilBerdasarClass(JenisLaporan.class);
		jenisLaporans = new ArrayList<JenisLaporan>();
		for (JenisLaporan jenisLaporan : maps.values()) {
			try {
				LampiranLain lainMahapegawai = LampiranLain.ambil(jenisLaporan.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI);
				if (lainMahapegawai != null && lainMahapegawai.getId() != null) {
					File file = lainMahapegawai.ambilFile();
					if (file.exists()) {
						jenisLaporans.add(jenisLaporan);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akunting/LaporanAkuntingSaldoBulanMaster.java:155");
				// TODO: handle exception
			}

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Laporan"));
		row.appendChild(jenisLaporan = new Combobox());

		if (jenisLaporans.isEmpty()) {
			Common.insertComboDanSemua(jenisLaporan, new String[] { "nama", "keterangan" }, "keterangan",
					JenisLaporan.class);
		} else {
			Common.insertComboItems(jenisLaporan, new String[] { "nama", "keterangan" }, "keterangan", jenisLaporans,
					"Pilih Jenis Laporan");
			jenisLaporan.setReadonly(true);
		}
		jenisLaporan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(jenisLaporans.isEmpty());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Laporan"));
		nama = new Combobox();
		List<String> namas = HibernateUtil.currentSession().createCriteria(MasterGrupLaporan.class)
				.setProjection(Projections.groupProperty("nama")).list();
		if (!namas.isEmpty()) {
			for (String n : namas) {
				MyComboitemConfig comboitem = new MyComboitemConfig(n);
				comboitem.setValue(n);
				nama.appendChild(comboitem);
			}
		} else {
			MyComboitemConfig comboitem = new MyComboitemConfig(MasterGrupLaporan.AKTIVA);
			comboitem.setValue(MasterGrupLaporan.AKTIVA);
			nama.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.KEWAJIBAN);
			comboitem.setValue(MasterGrupLaporan.KEWAJIBAN);
			nama.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.PENDAPATAN);
			comboitem.setValue(MasterGrupLaporan.PENDAPATAN);
			nama.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.OPERASIONAL);
			comboitem.setValue(MasterGrupLaporan.OPERASIONAL);
			nama.appendChild(comboitem);
			comboitem = new MyComboitemConfig(MasterGrupLaporan.INVESTASI);
			comboitem.setValue(MasterGrupLaporan.INVESTASI);
			nama.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		nama.appendChild(comboitem);
		nama.setSelectedItem(comboitem);
		nama.setReadonly(true);
		row.appendChild(nama);
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(jenisLaporans.isEmpty());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Laporan"));
		row.appendChild(grup = new Combobox());
		grup.setWidth("90%");
		grup.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(jenisLaporans.isEmpty());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Laporan"));
		row.appendChild(kelompok = new Combobox());
		kelompok.setWidth("90%");

		eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisLaporan laporan = (JenisLaporan) (jenisLaporan.getSelectedItem() == null ? null
						: jenisLaporan.getSelectedItem().getValue());
				MasterGrupLaporan masterGrupLaporan = (MasterGrupLaporan) (grup.getSelectedItem() == null ? null
						: grup.getSelectedItem().getValue());
				Common.insertComboDanSemua(kelompok, new String[] { "jenisLaporan", "masterGrupLaporan", "id" },
						"keterangan", KelompokLaporan.class,
						Restrictions.and(
								masterGrupLaporan == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("masterGrupLaporan", masterGrupLaporan),
								laporan == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("jenisLaporan", laporan)));
				kelompok.setSelectedItem(null);

			}
		};

		grup.addEventListener("onChange", eventListener2);
		jenisLaporan.addEventListener("onChange", eventListener2);

		EventListener eventListenerLagi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				JenisLaporan laporan = (JenisLaporan) (jenisLaporan.getSelectedItem() == null ? null
						: jenisLaporan.getSelectedItem().getValue());
				String n = (String) (nama.getSelectedItem() != null && nama.getSelectedItem().getValue() != null
						? nama.getSelectedItem().getValue()
						: null);

				List<MasterGrupLaporan> masterGrupLaporans = HibernateUtil.currentSession()
						.createCriteria(KelompokLaporan.class).add(Restrictions.isNotNull("masterGrupLaporan"))
						.setProjection(Projections.groupProperty("masterGrupLaporan"))
						.add(laporan == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisLaporan", laporan))
						.createAlias("masterGrupLaporan", "masterGrupLaporan1")
						.add(n == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("masterGrupLaporan1.nama", n))
						.list();

				Collections.sort(masterGrupLaporans);

				System.out.println("masterGrupLaporans -> " + masterGrupLaporans.size());
				Common.clear(grup);
				for (MasterGrupLaporan grupLaporan : masterGrupLaporans) {
					Comboitem comboitem = new Comboitem(grupLaporan.getNama() + " - " + grupLaporan.getKeterangan());
					comboitem.setValue(grupLaporan);
					grup.appendChild(comboitem);
				}
				Comboitem comboitem = new Comboitem("Semua");
				comboitem.setValue(null);
				grup.appendChild(comboitem);
				Common.selectComboItem(grup, null);

			}
		};

		nama.addEventListener("onChange", eventListenerLagi);
		jenisLaporan.addEventListener("onChange", eventListenerLagi);

		if (jenisLaporans.isEmpty()) {
			eventListenerLagi.onEvent(null);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchsatuanKerja = new AmbilDataSatuanKerjaBanbox());
		searchsatuanKerja.setWidth("90%");
		searchsatuanKerja.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Button tampilkan;
		row.appendChild(tampilkan = new Button("Tampilkan"));
		tampilkan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				onReport(event);

			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (tanggal1.getValue() == null) {
					MyMessageboxConfig.show("Pilih tanggal mulai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (tanggal2.getValue() == null) {
					MyMessageboxConfig.show("Pilih tanggal sampai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();

				if (fileData != null) {
					parameters.put("nama_laporan", fileData);
				}

				return parameters;
			}
		}, "akunting/laporan_keuangan_mutasi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		JenisLaporan laporan = (JenisLaporan) (jenisLaporan.getSelectedItem() == null ? null
				: jenisLaporan.getSelectedItem().getValue());
		if (!jenisLaporans.isEmpty() && laporan == null) {
			return null;
		}

		if (tanggal1.getValue() == null) {
			MyMessageboxConfig.show("Pilih tanggal mulai", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (tanggal2.getValue() == null) {
			MyMessageboxConfig.show("Pilih tanggal sampai", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		MasterGrupLaporan masterGrupLaporan = (MasterGrupLaporan) (grup.getSelectedItem() == null ? null
				: grup.getSelectedItem().getValue());
		KelompokLaporan kelompokLaporan = (KelompokLaporan) (kelompok.getSelectedItem() == null ? null
				: kelompok.getSelectedItem().getValue());

		Map parameters = ais.common.HashMapGenerator.getRand();

		Calendar tahunSebelumnya = Calendar.getInstance();
		tahunSebelumnya.setTime(tanggal1.getValue());
		tahunSebelumnya.set(Calendar.YEAR, tahunSebelumnya.get(Calendar.YEAR) - 1);
		parameters.put("tanggal0", tahunSebelumnya.getTime());

		Calendar tahunSetelahnya = Calendar.getInstance();
		tahunSetelahnya.setTime(tanggal2.getValue());
		tahunSetelahnya.set(Calendar.YEAR, tahunSetelahnya.get(Calendar.YEAR) + 1);
		parameters.put("tanggal3", tahunSetelahnya.getTime());

		Calendar tanggalSaldoAwal = Calendar.getInstance();
		tanggalSaldoAwal.setTime(tanggal1.getValue());
		tanggalSaldoAwal.set(Calendar.DATE, tanggalSaldoAwal.get(Calendar.DATE) - 1);

		parameters.put("tanggal1", tanggal1.getValue());
		parameters.put("tanggal2", tanggal2.getValue());

		parameters.put("tanggal1_1", tanggalSaldoAwal.getTime());

		parameters.put("tanggalSaldoAwalType", tanggalSaldoAwal.getTime());
		parameters.put("tanggalSaldoAwal", Common.databaseDateFormat.get().format(tanggalSaldoAwal.getTime()));

		tanggalSaldoAwal = Calendar.getInstance();
		tanggalSaldoAwal.setTime(tanggal2.getValue());
		tanggalSaldoAwal.set(Calendar.DATE, tanggalSaldoAwal.get(Calendar.DATE) - 1);
		parameters.put("tanggal2_1", tanggalSaldoAwal.getTime());

		String nama = (String) (this.nama.getSelectedItem() == null ? null : this.nama.getSelectedItem().getValue());
		parameters.put("nama", nama == null ? "-1" : nama);
		parameters.put("grup", masterGrupLaporan == null || masterGrupLaporan.getId() == null ? -1L : masterGrupLaporan.getId());
		parameters.put("kelompok", kelompokLaporan == null || kelompokLaporan.getId() == null ? -1L : kelompokLaporan.getId());
		SatuanKerja satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
		parameters.put("satuan_kerja", satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());

		parameters.put("jenis_laporan", laporan == null || laporan.getId() == null ? -1L : laporan.getId());

		String namaFile = "akunting/laporan_keuangan_mutasi";
		if (Common.bolehKonfigurasi("laporan_saldo_harus_berdasarkan_jenis_laporan", Konfigurasi.TIDAK_AKTIF) && laporan != null) {
			parameters.put("nama_laporan", namaFile + "_" + laporan.getId());
		}

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onReport(Event event) {

		try {

			JenisLaporan laporan = (JenisLaporan) (jenisLaporan.getSelectedItem() == null ? null
					: jenisLaporan.getSelectedItem().getValue());
			if (!jenisLaporans.isEmpty() && laporan == null) {
				return;
			}

			if (laporan != null) {

				LampiranLain lainMahapegawai = LampiranLain.ambil(laporan.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI);

				if (lainMahapegawai != null) {
					File file = Report.generateCompileFileReport(Report.PDF, generateParameter(),
							fileData = lainMahapegawai.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
					CommonReport.tampilkanReportPDF(center, file);
					return;
				}

			}

			String f = "akunting/laporan_keuangan_mutasi";

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), f, ais.ui.util.WaktuUtil.getDate(),
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Akunting Saldo Bulan Master", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
