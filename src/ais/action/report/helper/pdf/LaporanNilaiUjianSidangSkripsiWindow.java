package ais.action.report.helper.pdf;
import ais.common.PesanFormalHelper;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.Staff;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanNilaiUjianSidangSkripsiWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4807967045311869727L;
	private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox genapGanjilUjianAkhirSemester;
	private Combobox reportType;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	private AmbilDataMahasiswaBanbox bandboxMahasiswa;

	public LaporanNilaiUjianSidangSkripsiWindow() {
		super();
		try {
			initNilaiUjianSidangSkripsi();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Nilai Ujian Sidang Skripsi Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanNilaiUjianSidangSkripsiWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initNilaiUjianSidangSkripsi();
		init();
	}

	private void initNilaiUjianSidangSkripsi() throws Exception {
		tahunAkademikUjianAkhirSemester = new Combobox();
		tahunAkademikUjianAkhirSemester = Common.generateTahunAjaran(tahunAkademikUjianAkhirSemester);

		genapGanjilUjianAkhirSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);

	}

	@SuppressWarnings("deprecation")
	private void init() {

		/*
		 * jenisUjian = new Combobox(); org.zkoss.zul.Comboitem comboitem = new
		 * org.zkoss.zul.Comboitem(); comboitem.setLabel("UTS");
		 * comboitem.setValue("UTS"); jenisUjian.appendChild(comboitem); comboitem = new
		 * MyComboitemConfig(); comboitem.setLabel("UAS"); comboitem.setValue("UAS");
		 * jenisUjian.appendChild(comboitem);
		 */

		// setClosable(true);
		// setTitle("Laporan Penilaian Sidang Skripsi");
		// setWidth("500px");
		// setHeight("200px");
		// setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademikUjianAkhirSemester);
		tahunAkademikUjianAkhirSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(genapGanjilUjianAkhirSemester);
		genapGanjilUjianAkhirSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan"));
		row.appendChild(reportType = CommonReport.generateReportType());
		reportType.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onUjianSidangSkripsi(event);
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onUjianSidangSkripsi(Event event) throws Exception {

		if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun akademik", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (genapGanjilUjianAkhirSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		String genapGanjil = (String) genapGanjilUjianAkhirSemester.getSelectedItem().getValue();

		String tahunAkademik = (String) tahunAkademikUjianAkhirSemester.getSelectedItem().getValue();
		Session session = HibernateUtil.currentSession();
		Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.setMaxResults(1).uniqueResult();

		if (skripsi == null) {
			MyMessageboxConfig.show("Mahasiswa ini belum mengambil skripsi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);

			return;
		}

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
		Calendar calendar = Calendar.getInstance(Common.locale);
		calendar.setTime(
				skripsi.getTanggalSidang() == null ? ais.ui.util.WaktuUtil.getDate() : skripsi.getTanggalSidang());
		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

		int value = dayOfWeek - 2;
		value = (value < 0 ? 7 + value : value);
		map.put("hari", skripsi.getTanggalSidang() == null ? "" : Common.haris[value]);
		map.put("tanggal", skripsi.getTanggalSidang() == null ? "" : dateFormat.format(skripsi.getTanggalSidang()));
		map.put("nama", skripsi.getMahasiswa().getNama());
		map.put("nim", skripsi.getMahasiswa().getNim());
		map.put("pembimbing", skripsi.getPembimbing().getNama());
		map.put("judul", skripsi.getJudul());
		map.put("nilai_ketua_sidang", skripsi.getNilaiKetuaSidang());
		map.put("nilai_pembimbing", skripsi.getNilaiPembimbing());
		map.put("nilai_penguji1", skripsi.getNilaiPenguji1());
		map.put("nilai_penguji2", skripsi.getNilaiPenguji2());
		map.put("nilai_total", skripsi.getTotalNilai());
		map.put("nilai_huruf", skripsi.getNilaiHuruf());
		map.put("presentase_ketua", skripsi.getFormatNilaiSkripsi() == null ? ""
				: skripsi.getFormatNilaiSkripsi().getProsentasiNilaiKetuaSidang());
		map.put("presentase_pembimbing", skripsi.getFormatNilaiSkripsi() == null ? ""
				: skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPembimbing());
		map.put("presentase_penguji1", skripsi.getFormatNilaiSkripsi() == null ? ""
				: skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji1());
		map.put("presentase_penguji2", skripsi.getFormatNilaiSkripsi() == null ? ""
				: skripsi.getFormatNilaiSkripsi().getProsentasiNilaiPenguji2());
		maps.add(map);

		Staff staffPudek1 = (Staff) HibernateUtil.currentSession().createCriteria(Staff.class)
				.add(Restrictions.eq("staff", "pudek 1")).setMaxResults(1).uniqueResult();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("jenis_semester", genapGanjil);
		parameters.put("tahun_ajaran", tahunAkademik);
		parameters.put("tanggal_dibuat", dateFormat.format(ais.ui.util.WaktuUtil.getDate()));
		parameters.put("pudek1", staffPudek1.getNama());

		Report.generatePDFReport(
				reportType == null || reportType.getSelectedItem() == null ? Report.PDF
						: reportType.getSelectedItem().getValue().toString(),
				parameters, "FormNilaiAkhirSidang", ais.ui.util.WaktuUtil.getDate(), maps);

	}

}
