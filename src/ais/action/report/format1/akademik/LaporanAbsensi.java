package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan absensi. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahunAkademik}, {@code
 * Combobox semesterAbsensi}, {@code Combobox perkuliahan}, {@code MyCheckboxConfig tampilNilai}, {@code Combobox
 * fakultas}, {@code Combobox jurusan}, {@code MyDatebox waktudicetak}, {@code Combobox program};
 * inisialisasi/lifecycle ({@code initPerkuliahan()}, {@code init()}); operasi domain lain ({@code
 * generateParameter()}, {@code onLaporanAbsensi()}); konfigurasi constructor: {@code fakultas}, {@code jurusan},
 * {@code program}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanAbsensi extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5620991583788581962L;

	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;
	private Combobox perkuliahan;
	private MyCheckboxConfig tampilNilai;
	private Combobox fakultas;
	private Combobox jurusan;
	private MyDatebox waktudicetak;
	private Combobox program;
	private Textbox kelas;
	private Combobox format;

	private Center center;

	private Toolbar toolbar;

	/**
	 * Event listener lokal milik {@link LaporanAbsensi}. Kelas ini menangani event untuk komponen induk dan
	 * meneruskan pekerjaan domain ke method/service yang sudah tersedia.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanAbsensi} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see LaporanAbsensi
	 */
	class PerkuliahanEventListener implements EventListener {
		@Override
		public void onEvent(Event event) throws Exception {
			Common.clear(perkuliahan);
			perkuliahan.setSelectedItem(null);
			if (tahunAkademik.getSelectedItem() == null)
				return;
			if (semesterAbsensi.getSelectedItem() == null)
				return;
			if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null)
				return;
			if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null)
				return;

			String myKelas = kelas.getValue();
			String myProgram = (String) (program.getSelectedItem() == null
					|| program.getSelectedItem().getValue() == null ? "" : program.getSelectedItem().getValue());

			List<Perkuliahan> items = DaoFactory.getInstance().getPerkuliahanDao().findByCriteria(
					Order.asc("waktuMulaiD"),
					myKelas.trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("kelas", myKelas, MatchMode.ANYWHERE),

					myProgram.trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ilike("program", myProgram, MatchMode.ANYWHERE),

					Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()),
					Restrictions.eq("semester", semesterAbsensi.getSelectedItem().getValue()),
					CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false));
			if (items.size() == 0)
				return;
			for (Perkuliahan o : items) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1().getNama()) + " - "
						+ o.getMatakuliah().getNama() + " (" + o.getId() + ")");
				comboitem.setValue(o);

				String deskripsi = "Smt: "
						+ (o.getSemester()
								+ (o.getKelas() == null || o.getKelas().equals("") ? "" : " " + o.getKelas()))
						+ ", Ruang: " + (o.getRuang() == null ? "" : o.getRuang().getKodeRuangan()) + ", Hari: "
						+ o.getHari() + ", Waktu: " + o.getWaktuMulai() + "-" + o.getWaktuSelesai() + ", Paralel: "
						+ (o.getMerupakan_paralel() == null || !o.getMerupakan_paralel() ? "Bukan" : "Ya");

				comboitem.setDescription(deskripsi);

				perkuliahan.appendChild(comboitem);
			}
		}
	}

	PerkuliahanEventListener perkuliahanEventListener = new PerkuliahanEventListener();

	public LaporanAbsensi() {
		super();
		try {

			fakultas = new Combobox();
			jurusan = new Combobox();
			Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

			program = Common.initPrograms(null);

			init();
			initPerkuliahan();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Absensi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAbsensi(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		fakultas = new Combobox();
		jurusan = new Combobox();

		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		program = Common.initPrograms(null);

		/**
		 * Event listener lokal milik {@link LaporanAbsensi}. Kelas ini menangani event untuk komponen induk dan
		 * meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanAbsensi} dan dapat mengakses state
		 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see LaporanAbsensi
		 */
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

		init();
		initPerkuliahan();
	}

	private void initPerkuliahan() {

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

		if (tahunAkademik != null) {
			tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
			tahunAkademik.addEventListener("onChange", perkuliahanEventListener);
			for (int i = 1; i <= 21; i++) {
				org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
				comboitem.setLabel(i + "");
				comboitem.setValue(i);
				semesterAbsensi.appendChild(comboitem);
			}
			Common.selectComboItem(semesterAbsensi, 1);
			semesterAbsensi.addEventListener("onChange", perkuliahanEventListener);
			jurusan.addEventListener("onChange", perkuliahanEventListener);
			// dosen.addEventListener("onChange", eventListener);

		}

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {
		//
		// EventListener eventListener = new EventListener() {
		//
		// @Override
		// public void onEvent(Event event) throws Exception {
		// onLaporanAbsensi(event);
		//
		// }
		// };

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		// jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		tahunAkademik.setWidth("90%");
		// tahunAkademik.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensi = new Combobox());
		semesterAbsensi.setWidth("90%");
		// semesterAbsensi.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");
		program.addEventListener("onChange", perkuliahanEventListener);
		// program.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox());
		kelas.setWidth("90%");
		kelas.addEventListener("onChange", perkuliahanEventListener);
		kelas.addEventListener("onOK", perkuliahanEventListener);
		// kelas.addEventListener("onChange", eventListener);
		// kelas.addEventListener("onOK", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Perkuliahan"));
		row.appendChild(perkuliahan = new Combobox());
		perkuliahan.setWidth("90%");
		// perkuliahan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Format"));
		row.appendChild(format = new Combobox());
		format.setWidth("90%");
		// format.addEventListener("onChange", eventListener);
		MyComboitemConfig comboitem = new MyComboitemConfig("Potrait");
		comboitem.setValue("LaporanAbsensi");
		format.appendChild(comboitem);
		format.setSelectedItem(comboitem);

		comboitem = new MyComboitemConfig("Ladscape 1");
		comboitem.setValue("LaporanAbsensiLanscape");
		format.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Ladscape 2");
		comboitem.setValue("LaporanAbsensiLanscape1");
		format.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampil Status Kehadiran"));
		row.appendChild(tampilNilai = new MyCheckboxConfig());
		// tampilNilai.addEventListener("onCheck", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dicetak pada"));
		row.appendChild(waktudicetak = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		waktudicetak.setFormat(Common.dateFormat.get().toPattern());

		waktudicetak.setWidth("90%");
		// waktudicetak.addEventListener("onChange", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (perkuliahan.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Perkuliahan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih salah satu perkuliahan dari daftar yang tersedia; (2) Pastikan data perkuliahan sudah dimuat sesuai filter semester dan dosen; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Pilih salah satu " + Common.getBahasaConfig("Jurusan"), "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "LaporanAbsensi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAbsensi(arg0);

			}
		}));

		// onLaporanAbsensi(null);

		try {
			perkuliahanEventListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Absensi", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAbsensi(arg0);
			}
		});

		MyToolbarbuttonConfig tombol = new MyToolbarbuttonConfig("UTS", "/img/print.png");
		tombol.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (perkuliahan.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Perkuliahan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih salah satu perkuliahan dari daftar yang tersedia; (2) Pastikan data perkuliahan sudah dimuat sesuai filter semester dan dosen; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				CommonReportHelper.onLaporanAbsensi((Perkuliahan) perkuliahan.getSelectedItem().getValue(), "UTS");
			}

		});
		tombol.setParent(toolbar);

		tombol = new MyToolbarbuttonConfig("UAS", "/img/print.png");
		tombol.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (perkuliahan.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Perkuliahan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih salah satu perkuliahan dari daftar yang tersedia; (2) Pastikan data perkuliahan sudah dimuat sesuai filter semester dan dosen; (3) Ulangi proses cetak laporan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				CommonReportHelper.onLaporanAbsensi((Perkuliahan) perkuliahan.getSelectedItem().getValue(), "UAS");
			}

		});
		tombol.setParent(toolbar);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (perkuliahan.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih salah satu perkuliahan",
			// "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			// MyMessageboxConfig.show("Pilih salah satu
			// "+"Jurusan",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		Perkuliahan perkuliahan = (Perkuliahan) this.perkuliahan.getSelectedItem().getValue();

		Session session = HibernateUtil.currentSession();
		JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) session
				.createCriteria(JenjangProgramStudi.class)
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false)).setMaxResults(1).uniqueResult();

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("perkuliahan", perkuliahan.getId());
		parameters.put("tampil_nilai", tampilNilai.isChecked() ? "1" : "0");

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Jurusan jurusan = perkuliahan.getJurusan();
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}

		String format = (String) (this.format.getSelectedItem() == null ? "LaporanAbsensi"
				: this.format.getSelectedItem().getValue());

		parameters.put("nama_laporan", format);
		parameters.put("waktu_dicetak", ais.ui.util.WaktuUtil.getDate());
		parameters.put("tanggal", waktudicetak.getValue() == null ? "" : dateFormat.format(waktudicetak.getValue()));

		List<Map<String, Serializable>> maps = CommonReportHelper.generateParameterMapAbsensi(perkuliahan);
		parameters.put("maps", maps);
		return parameters;
	}

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	@SuppressWarnings({})
	public void onLaporanAbsensi(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "LaporanAbsensi",
							ais.ui.util.WaktuUtil.getDate(), toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Absensi", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

	}

}
