package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.MatchMode;
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
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.akunting.KelompokLaporan;
import ais.database.model.akunting.MasterGrupLaporan;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanAkunting2Tahun extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox tahun;
	private Combobox tahun2;

	private Combobox grup;
	private Combobox kelompok;

	private Center center;
	private Toolbar toolbar;

	private Combobox nama;

	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;

	private Combobox jenisLaporan;

	private String fileData;

	private ArrayList<JenisLaporan> jenisLaporans;

	public LaporanAkunting2Tahun() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Akunting2 Tahun", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAkunting2Tahun(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun 1"));
		row.appendChild(tahun = new Combobox());
		int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = year; i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
			if (i == year) {
				tahun.setSelectedItem(comboitem);
			}
		}
		tahun.setWidth("90%");
		tahun.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun 2"));
		row.appendChild(tahun2 = new Combobox());
		for (int i = year; i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun2.appendChild(comboitem);
			if (i == year) {
				tahun2.setSelectedItem(comboitem);
			}
		}
		tahun2.setWidth("90%");
		tahun2.setReadonly(true);

		Map<Long, JenisLaporan> maps = ConstantValues.ambilBerdasarClass(JenisLaporan.class);
		jenisLaporans = new ArrayList<JenisLaporan>();
		for (JenisLaporan jenisLaporan : maps.values()) {
			try {
				LampiranLain lainMahapegawai = LampiranLain.ambil(jenisLaporan.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI + "_2");
				if (lainMahapegawai != null && lainMahapegawai.getId() != null) {
					File file = lainMahapegawai.ambilFile();
					if (file.exists()) {
						jenisLaporans.add(jenisLaporan);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akunting/LaporanAkunting2Tahun.java:159");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Grup Laporan"));
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
		Common.insertComboDanSemua(grup, new String[] { "nama", "id", "keterangan" }, "keterangan",
				MasterGrupLaporan.class);
		grup.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(jenisLaporans.isEmpty());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Laporan"));
		row.appendChild(kelompok = new Combobox());
		kelompok.setWidth("90%");

		EventListener eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisLaporan laporan = (JenisLaporan) (jenisLaporan.getSelectedItem() == null ? null
						: jenisLaporan.getSelectedItem().getValue());
				MasterGrupLaporan masterGrupLaporan = (MasterGrupLaporan) (grup.getSelectedItem() == null ? null
						: grup.getSelectedItem().getValue());
				Common.insertComboDanSemua(kelompok, new String[] { "jenisLaporan", "id" }, "keterangan",
						KelompokLaporan.class,
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

		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (nama.getSelectedItem() != null && nama.getSelectedItem().getValue() != null) {
					Common.insertComboDanSemua(grup, new String[] { "nama", "id", "keterangan" }, "keterangan",
							MasterGrupLaporan.class,
							Restrictions.ilike("nama", nama.getSelectedItem().getValue().toString(), MatchMode.EXACT));
				} else {
					Common.insertComboDanSemua(grup, new String[] { "nama", "id", "keterangan" }, "keterangan",
							MasterGrupLaporan.class);
				}
				grup.setSelectedItem(null);
				kelompok.setSelectedItem(null);

			}
		});

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

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu tahun 1", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (tahun2.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu tahun 2", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				if (fileData != null) {
					parameters.put("nama_laporan", fileData);
				}
				return parameters;
			}
		}, "akunting/laporan_keuangan_2_tahun", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		if (jenisLaporans.isEmpty()) {
			onReport(null);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		JenisLaporan laporan = (JenisLaporan) (jenisLaporan.getSelectedItem() == null ? null
				: jenisLaporan.getSelectedItem().getValue());
		if (!jenisLaporans.isEmpty() && laporan == null) {
			return null;
		}
		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun 1", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (tahun2.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun 2", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		Integer tahun1 = (Integer) this.tahun.getSelectedItem().getValue();
		Integer tahun2 = (Integer) this.tahun2.getSelectedItem().getValue();

		MasterGrupLaporan masterGrupLaporan = (MasterGrupLaporan) (grup.getSelectedItem() == null ? null
				: grup.getSelectedItem().getValue());
		KelompokLaporan kelompokLaporan = (KelompokLaporan) (kelompok.getSelectedItem() == null ? null
				: kelompok.getSelectedItem().getValue());

		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("tahun2", tahun2);
		parameters.put("tahun1", tahun1);

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, cal.getActualMaximum(Calendar.MONTH));
		cal.set(Calendar.YEAR, tahun1);
		cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));

		Calendar tanggalSaldoAwal = Calendar.getInstance();
		tanggalSaldoAwal.setTime(cal.getTime());
		tanggalSaldoAwal.set(Calendar.DATE, tanggalSaldoAwal.get(Calendar.DATE) - 1);

		parameters.put("tanggal1", cal.getTime());
		parameters.put("tgl1", cal.getTime());
		parameters.put("tgl1_format", Common.databaseDateFormat.get().format(cal.getTime()));
		parameters.put("tgl1_format_1", Common.dateFormat1.get().format(cal.getTime()));
		parameters.put("tgl1_format_2", Common.dateFormat6.get().format(cal.getTime()));
		parameters.put("tgl1_format_3", Common.dateFormat2.get().format(cal.getTime()));
		parameters.put("tanggalSaldoAwalType", tanggalSaldoAwal.getTime());
		parameters.put("tanggalSaldoAwal", Common.databaseDateFormat.get().format(tanggalSaldoAwal.getTime()));

		cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, cal.getActualMaximum(Calendar.MONTH));
		cal.set(Calendar.YEAR, tahun2);
		cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));

		parameters.put("tgl2", cal.getTime());
		parameters.put("tgl2_format", Common.databaseDateFormat.get().format(cal.getTime()));
		parameters.put("tgl2_format_1", Common.dateFormat1.get().format(cal.getTime()));
		parameters.put("tgl2_format_2", Common.dateFormat6.get().format(cal.getTime()));
		parameters.put("tgl2_format_3", Common.dateFormat2.get().format(cal.getTime()));

		parameters.put("grup", masterGrupLaporan == null || masterGrupLaporan.getId() == null ? -1L : masterGrupLaporan.getId());
		parameters.put("kelompok", kelompokLaporan == null || kelompokLaporan.getId() == null ? -1L : kelompokLaporan.getId());

		String nama = (String) (this.nama.getSelectedItem() == null ? null : this.nama.getSelectedItem().getValue());
		parameters.put("nama", nama == null ? "-1" : nama);
		SatuanKerja satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
		parameters.put("satuan_kerja", satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());

		parameters.put("jenis_laporan", laporan == null || laporan.getId() == null ? -1L : laporan.getId());

		parameters.put("tanggal2", cal.getTime());

		nama = (String) (this.nama.getSelectedItem() == null ? null : this.nama.getSelectedItem().getValue());
		parameters.put("nama", nama == null ? "-1" : nama);
		parameters.put("grup", masterGrupLaporan == null || masterGrupLaporan.getId() == null ? -1L : masterGrupLaporan.getId());
		parameters.put("kelompok", kelompokLaporan == null || kelompokLaporan.getId() == null ? -1L : kelompokLaporan.getId());
		satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
		parameters.put("satuan_kerja", satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());

		laporan = (JenisLaporan) (jenisLaporan.getSelectedItem() == null ? null
				: jenisLaporan.getSelectedItem().getValue());
		parameters.put("jenis_laporan", laporan == null || laporan.getId() == null ? -1L : laporan.getId());

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
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI + "_2");

				if (lainMahapegawai != null) {
					File file = Report.generateCompileFileReport(Report.PDF, generateParameter(),
							fileData = lainMahapegawai.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
					CommonReport.tampilkanReportPDF(center, file);
					return;
				}

			}

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/laporan_keuangan_2_tahun",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Akunting2 Tahun", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
