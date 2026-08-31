package ais.action.report.helper.pdf;
import ais.common.PesanFormalHelper;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
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
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
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
 * Penyusun/penyaji laporan untuk laporan jadwal uas window. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox
 * tahunAkademikUjianAkhirSemester}, {@code Combobox genapGanjilUjianAkhirSemester}, {@code Combobox
 * waktuUjianAkhirSemester}, {@code Combobox fakultasUjianAkhirSemester}, {@code Combobox
 * jurusanUjianAkhirSemester}, {@code MyDatebox dibuatTanggalUjianAkhirSemester}, {@code Combobox reportType},
 * {@code SimpleDateFormat dateFormat}; inisialisasi/lifecycle ({@code initJadwalUAS()}, {@code init()}); operasi
 * domain lain ({@code onLaporanJadwalUas()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanJadwalUasWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 986145944972683669L;
	private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox genapGanjilUjianAkhirSemester;
	private Combobox waktuUjianAkhirSemester;
	private Combobox fakultasUjianAkhirSemester;
	private Combobox jurusanUjianAkhirSemester;
	private MyDatebox dibuatTanggalUjianAkhirSemester;
	private Combobox reportType;

	public LaporanJadwalUasWindow() {
		super();
		try {
			initJadwalUAS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Jadwal Uas Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanJadwalUasWindow(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);
		initJadwalUAS();
		init();
	}

	private void initJadwalUAS() throws Exception {
		tahunAkademikUjianAkhirSemester = new Combobox();
		tahunAkademikUjianAkhirSemester = Common
				.generateTahunAjaran(tahunAkademikUjianAkhirSemester);

		dibuatTanggalUjianAkhirSemester = new MyDatebox();
		dibuatTanggalUjianAkhirSemester.setValue(ais.ui.util.WaktuUtil.getDate());

		genapGanjilUjianAkhirSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		genapGanjilUjianAkhirSemester.appendChild(comboitem);

		waktuUjianAkhirSemester = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SEMUA");
		comboitem.setValue("SEMUA");
		waktuUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("PAGI");
		comboitem.setValue("PAGI");
		waktuUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SIANG");
		comboitem.setValue("SIANG");
		waktuUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SORE");
		comboitem.setValue("SORE");
		waktuUjianAkhirSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("MALAM");
		comboitem.setValue("MALAM");
		waktuUjianAkhirSemester.appendChild(comboitem);

		fakultasUjianAkhirSemester = new Combobox();
		jurusanUjianAkhirSemester = new Combobox();
		Common.insertCombo(fakultasUjianAkhirSemester, new String[]{"nama", "kode"}, Fakultas.class, Restrictions.eq("aktif", true));
		class KurikulumFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusanUjianAkhirSemester);

				jurusanUjianAkhirSemester.setSelectedItem(null);
				if (fakultasUjianAkhirSemester.getSelectedItem() == null) {
					return;
				}
				Common.insertCombo(jurusanUjianAkhirSemester, "nama",
						Jurusan.class, CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultasUjianAkhirSemester, false));
			}

		}
		KurikulumFakultasEventListener kurikulumFakultasEventListener = new KurikulumFakultasEventListener();
		fakultasUjianAkhirSemester.addEventListener("onChange",
				kurikulumFakultasEventListener);
		fakultasUjianAkhirSemester.setSelectedIndex(0);
		kurikulumFakultasEventListener.onEvent(null);
	}

	@SuppressWarnings("deprecation")
	private void init() {

		// setClosable(true);
		// setTitle("Laporan Jadwal UAS");
		// setWidth("500px");
		// setHeight("260px");
		// setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu"));
		row.appendChild(waktuUjianAkhirSemester);
		waktuUjianAkhirSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultasUjianAkhirSemester);
		fakultasUjianAkhirSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusanUjianAkhirSemester = new Combobox());
		jurusanUjianAkhirSemester.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(dibuatTanggalUjianAkhirSemester);
		dibuatTanggalUjianAkhirSemester.setWidth("90%");

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
				onLaporanJadwalUas(event);
			}
		});
		print.setParent(toolbar);

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(fakultasUjianAkhirSemester,
					tbmuser.ambilFakultas());
			Common.clear(jurusanUjianAkhirSemester);
			Common.insertCombo(jurusanUjianAkhirSemester, "nama",
					Jurusan.class,
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			fakultasUjianAkhirSemester.setDisabled(true);
		} else {
			fakultasUjianAkhirSemester.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(jurusanUjianAkhirSemester,
					tbmuser.ambilJurusan());
			jurusanUjianAkhirSemester.setDisabled(true);
		} else {
			jurusanUjianAkhirSemester.setDisabled(false);
		}

	}

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy",
			Common.locale);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onLaporanJadwalUas(Event event) throws Exception {
		try {
			Session session = HibernateUtil.currentSession();

			if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
				MyMessageboxConfig.show("Pilih salah satu tahun akademik",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			if (waktuUjianAkhirSemester.getSelectedItem() == null) {
				MyMessageboxConfig.show("Pilih salah satu waktu ujian", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}
			if (genapGanjilUjianAkhirSemester.getSelectedItem() == null) {
				MyMessageboxConfig.show("Pilih salah satu semester genap atau ganjil",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			if (fakultasUjianAkhirSemester.getSelectedItem() == null) {
				MyMessageboxConfig.show("Pilih salah satu fakultas", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}
			if (jurusanUjianAkhirSemester.getSelectedItem() == null) {
				MyMessageboxConfig.show("Pilih salah satu Jurusan", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}
			if (dibuatTanggalUjianAkhirSemester.getValue() == null) {
				MyMessageboxConfig.show("Pilih tanggal dibuatnya jadwal UAS",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return;
			}

			String waktu = (String) waktuUjianAkhirSemester.getSelectedItem()
					.getValue();

			String genapGanjil = (String) genapGanjilUjianAkhirSemester
					.getSelectedItem().getValue();

			String sql = "((this_.waktu_mulai+0) >= (case '"
					+ waktu
					+ "'  when 'SEMUA' then -1 when 'PAGI' then 5 when 'SORE' then 13 when 'MALAM' then 18 end) "
					+ "and (this_.waktu_mulai+0) < (case '"
					+ waktu
					+ "' when 'SEMUA' then 100 when 'PAGI' then 13 when 'SORE' then 18 when 'MALAM' then 24 end))";

			List<Pertemuan> pertemuans = session
					.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("statusPertemuan.id", 4L))
					.createCriteria("perkuliahan", "perk")
					.addOrder(Order.asc("waktuMulai"))
					.add(Restrictions
							.sqlRestriction("(perk1_.semester % 2) = (case '"
									+ genapGanjil + "' when '"
									+ Perkuliahan.GANJIL
									+ "' then 1 when '"
									+ Perkuliahan.GENAP
									+ "' then 0 end)"))
					.add(Restrictions.eq("tahunAjaran",
							tahunAkademikUjianAkhirSemester.getSelectedItem()
									.getValue()))
					.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusanUjianAkhirSemester, false))
					.add(Restrictions.sqlRestriction(sql)).list();

			List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
			for (Pertemuan pertemuan : pertemuans) {
				Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(pertemuan.getTanggal());
				int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
				int value = dayOfWeek - 2;
				value = (value < 0 ? 7 + value : value);
				map.put("hari_dan_tanggal", Common.haris[value] + ", "
						+ dateFormat.format(pertemuan.getTanggal()));
				map.put("jam",
						pertemuan.getWaktuMulai() + "-"
								+ pertemuan.getWaktuSelesai());
				map.put("nama", pertemuan.getPerkuliahan().getMatakuliah()
						.getNama());
				map.put("semester", pertemuan.getPerkuliahan().getSemester());
				maps.add(map);
			}

			Fakultas fakultas = (Fakultas) fakultasUjianAkhirSemester
					.getSelectedItem().getValue();
			Jurusan jurusan = (Jurusan) jurusanUjianAkhirSemester
					.getSelectedItem().getValue();

			Staff staffPudek1 = (Staff) session.createCriteria(Staff.class)
					.add(Restrictions.eq("staff", "pudek 1")).setMaxResults(1)
					.uniqueResult();
			Staff staffDekan = (Staff) session.createCriteria(Staff.class)
					.add(Restrictions.eq("staff", "dekan")).setMaxResults(1)
					.uniqueResult();

			final Map parameters = ais.common.HashMapGenerator.getRand();
			parameters.put("pudek_1", staffPudek1.getNama());
			parameters.put("dekan", staffDekan.getNama());
			parameters.put("fakultas", fakultas.getNama());
			parameters.put("jurusan", jurusan.getNama());
			parameters.put("genapGanjil", genapGanjil);
			parameters.put("waktu", waktu);
			parameters.put("tanggal", dateFormat
					.format(dibuatTanggalUjianAkhirSemester.getValue()));

			parameters.put("tahunAkademik", tahunAkademikUjianAkhirSemester
					.getSelectedItem().getValue());

			Report.generatePDFReport(
					reportType == null || reportType.getSelectedItem() == null ? Report.PDF
							: reportType.getSelectedItem().getValue()
									.toString(), parameters,
					"Jadwal_Ujian_Akhir_Semester", ais.ui.util.WaktuUtil.getDate(), maps
						);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Jadwal Uas Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
