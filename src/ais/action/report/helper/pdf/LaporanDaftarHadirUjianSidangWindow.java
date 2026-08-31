package ais.action.report.helper.pdf;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

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

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Staff;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan daftar hadir ujian sidang window. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox
 * tahunAkademikUjianAkhirSemester}, {@code Combobox genapGanjilUjianAkhirSemester}, {@code Combobox reportType},
 * {@code MyDatebox tanggal}, {@code Combobox fakultas}, {@code Combobox jurusan}, {@code SimpleDateFormat
 * dateFormat}; inisialisasi/lifecycle ({@code initDaftarHadirUjianSidang()}, {@code init()}); operasi domain
 * lain ({@code onDaftarHadirUjianSidang()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanDaftarHadirUjianSidangWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 61315375857390154L;
	private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox genapGanjilUjianAkhirSemester;
	private Combobox reportType;
	private MyDatebox tanggal;

	// tambahan wildan
	private Combobox fakultas;
	private Combobox jurusan;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	public LaporanDaftarHadirUjianSidangWindow() {
		super();
		try {
			initDaftarHadirUjianSidang();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Hadir Ujian Sidang Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarHadirUjianSidangWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initDaftarHadirUjianSidang();
		init();
	}

	private void initDaftarHadirUjianSidang() throws Exception {
		tahunAkademikUjianAkhirSemester = new Combobox();
		tahunAkademikUjianAkhirSemester = Common.generateTahunAjaran(tahunAkademikUjianAkhirSemester);

		fakultas = new Combobox();
		jurusan = new Combobox();

		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
			}

		}

		fakultas.addEventListener("onChange", new SearchFakultasEventListener());

		tanggal = new MyDatebox();
		tanggal.setValue(ais.ui.util.WaktuUtil.getDate());

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
		// setTitle("Laporan Daftar Hadir Ujian Sidang Skripsi");
		// setWidth("500px");
		// setHeight("260px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(tanggal);
		tanggal.setWidth("90%");

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
				onDaftarHadirUjianSidang(event);
			}
		});
		print.setParent(toolbar);

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
			Common.clear(jurusan);
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			fakultas.setDisabled(true);
		} else {
			fakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());
			jurusan.setDisabled(true);
		} else {
			jurusan.setDisabled(false);
		}

	}

	@SuppressWarnings("unchecked")
	public void onDaftarHadirUjianSidang(Event event) throws Exception {
		if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Pilih salah satu fakultas", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Pilih salah satu Jurusan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
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
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Tanggal atau genap", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		// if (konsentrasi.getSelectedItem() == null) {
		// MyMessageboxConfig.show("Pilih konsentrasi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return;
		// }
		try {

			Fakultas fakultasObj = (Fakultas) fakultas.getSelectedItem().getValue();
			String genapGanjil = (String) genapGanjilUjianAkhirSemester.getSelectedItem().getValue();

			String tahunAkademik = (String) tahunAkademikUjianAkhirSemester.getSelectedItem().getValue();

			Staff staffPudek1 = (Staff) HibernateUtil.currentSession().createCriteria(Staff.class)
					.add(Restrictions.eq("staff", "pudek 1")).setMaxResults(1).uniqueResult();

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

			System.out.println("Tanggal = " + tanggal.getValue());

			@SuppressWarnings("rawtypes")
			final Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("jenis_semester", genapGanjil == null ? "" : genapGanjil);
			parameters.put("tahun_ajaran", tahunAkademik == null ? "" : tahunAkademik);
			parameters.put("fakultas", fakultasObj.getNama());
			parameters.put("tanggal", tanggal.getValue() == null ? "" : format.format(tanggal.getValue()));
			Calendar calendar = Calendar.getInstance(Common.locale);
			calendar.setTime(tanggal.getValue());
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

			int value = dayOfWeek - 2;
			value = (value < 0 ? 7 + value : value);
			parameters.put("hari_dan_tanggal", Common.haris[value] + ", " + dateFormat.format(tanggal.getValue()));
			parameters.put("tanggal_dibuat", tanggal.getValue() == null ? "" : dateFormat.format(tanggal.getValue()));
			parameters.put("pudek1", staffPudek1 == null ? "" : staffPudek1.getNama());

			Report.generatePDFReport(
					reportType == null || reportType.getSelectedItem() == null ? Report.PDF
							: reportType.getSelectedItem().getValue().toString(),
					parameters, "Daftar_Hadir_Ujian_Sidang", ais.ui.util.WaktuUtil.getDate());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Daftar Hadir Ujian Sidang Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
