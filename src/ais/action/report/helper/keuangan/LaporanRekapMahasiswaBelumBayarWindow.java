package ais.action.report.helper.keuangan;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap mahasiswa belum bayar window. Kelas ini mengubah
 * data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * searchsemester}, {@code Combobox jenisPembayaran}, {@code Spreadsheet spreadsheet}, {@code Center center};
 * inisialisasi/lifecycle ({@code initFakultas()}, {@code init()}, {@code initSpreadsheet()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapMahasiswaBelumBayarWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Combobox jenisPembayaran = new Combobox();
	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();

//	private Combobox searchstatus = new Combobox();
	private Combobox searchwnawni = new Combobox();
	private Combobox angkatanMhsMulai = new Combobox();private Combobox angkatanMhs = new Combobox();
	private Textbox nim = new Textbox();

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private Combobox searchprogram = new Combobox();

	public LaporanRekapMahasiswaBelumBayarWindow() {
		super();
		try {

			init();
			initFakultas();
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekap Mahasiswa Belum Bayar Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapMahasiswaBelumBayarWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
		initFakultas();
		initSpreadsheet();
	}

	private void initFakultas() {

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		jenisPembayaran = Common.createComboJenisPembayaran(jenisPembayaran);

//		Common.insertCombo(searchstatus, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		MyComboitemConfig comboitem = new MyComboitemConfig(Mahasiswa.WNI);
		comboitem.setValue(Mahasiswa.WNI);
		searchwnawni.appendChild(comboitem);

		comboitem = new MyComboitemConfig(Mahasiswa.WNA);
		comboitem.setValue(Mahasiswa.WNA);
		searchwnawni.appendChild(comboitem);

		/**
		 * Event listener lokal milik {@link LaporanRekapMahasiswaBelumBayarWindow}. Kelas ini menangani event untuk
		 * komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link LaporanRekapMahasiswaBelumBayarWindow} dan
		 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see LaporanRekapMahasiswaBelumBayarWindow
		 */
		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			Common.clear(searchjurusan);
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			searchjurusan.setDisabled(true);
		} else {
			searchjurusan.setDisabled(false);
		}

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap mahasiswa yang sudah melakukan pembayaran");
		// setWidth("98%");
		// setHeight("90%");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("240px");
		north.setAutoscroll(true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
		row.appendChild(searchwnawni);
		searchwnawni.setWidth("90%");

////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
//		row.appendChild(searchstatus);
//		searchstatus.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran *"));
		row.appendChild(jenisPembayaran);
		jenisPembayaran.setWidth("90%");
		jenisPembayaran.setReadonly(true);
		jenisPembayaran.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jenisPembayaran.getSelectedItem() == null)
					return;
				if (jenisPembayaran.getValue().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {
					searchfakultas.setDisabled(true);
					searchfakultas.setSelectedItem(null);
					searchjurusan.setDisabled(true);
					searchjurusan.setSelectedItem(null);
					semesterAbsensi.setDisabled(true);
					semesterAbsensi.setSelectedItem(null);
					searchsemester.setDisabled(true);
					searchsemester.setSelectedItem(null);
//					searchstatus.setDisabled(true);
				} else if (jenisPembayaran.getValue().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
					semesterAbsensi.setDisabled(true);
					semesterAbsensi.setSelectedItem(null);
					searchsemester.setDisabled(true);
					searchsemester.setSelectedItem(null);
//					searchstatus.setDisabled(true);
				} else {
					searchfakultas.setDisabled(false);
					searchjurusan.setDisabled(false);
					semesterAbsensi.setDisabled(false);
					searchsemester.setDisabled(false);
//					searchstatus.setDisabled(false);
				}

			}
		});

		Common.initPrograms(searchprogram);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM/No.Reg/No.Ujian"));
		row.appendChild(nim);
		nim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		semesterAbsensi = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setSelectedIndex(1);
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester ke"));
		row.appendChild(searchsemester);
		searchsemester.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		row.appendChild(angkatanMhs);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		angkatanMhs.appendChild(comboitem);
		for (int i = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10; i <= ais.ui.util.WaktuUtil
				.getCalendar().get(Calendar.YEAR) + 10; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatanMhs.appendChild(comboitem);
		}
		angkatanMhs.setSelectedIndex(0);
		angkatanMhs.setWidth("90%");
		angkatanMhs.setReadonly(true);

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);
				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
				if (genap) {
					for (int i : Common.genap) {
						if (i == 0)
							continue;
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					for (int i : Common.ganjil) {
						org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				}
			}
		};

		eventListener.onEvent(null);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "8");
		row.setParent(rows);
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(row);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Proses", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				initSpreadsheet();
			}
		});
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Download", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
						"Rekap_mahasiswa_yang_belum_melakukan_pembayaran.xlsx");
			}
		});
		print.setParent(toolbar);

	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				try {
					Common.clear(center);
					String tahunAkademik = (String) (LaporanRekapMahasiswaBelumBayarWindow.this.tahunAkademik
							.getSelectedItem() == null ? null
									: LaporanRekapMahasiswaBelumBayarWindow.this.tahunAkademik.getSelectedItem()
											.getValue());
					String semester = (String) (LaporanRekapMahasiswaBelumBayarWindow.this.semesterAbsensi
							.getSelectedItem() == null ? Perkuliahan.GANJIL
									: LaporanRekapMahasiswaBelumBayarWindow.this.semesterAbsensi.getSelectedItem()
											.getValue());

					Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
							|| searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? null
									: searchfakultas.getSelectedItem().getValue());
					Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
							|| searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? null
									: searchjurusan.getSelectedItem().getValue());

					Integer semesterKe = (Integer) (searchsemester.getSelectedItem() == null ? -1
							: searchsemester.getSelectedItem().getValue());

					JenisKegiatan jenisPembayaran = (JenisKegiatan) (LaporanRekapMahasiswaBelumBayarWindow.this.jenisPembayaran
							.getSelectedItem() == null ? null
									: LaporanRekapMahasiswaBelumBayarWindow.this.jenisPembayaran.getSelectedItem()
											.getValue());
//
//					StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
//							|| searchstatus.getSelectedItem().getValue() == null ? null
//									: searchstatus.getSelectedItem().getValue());
					String wnawni = (String) (searchwnawni.getSelectedItem() == null ? null
							: searchwnawni.getSelectedItem().getValue());
					String nim = LaporanRekapMahasiswaBelumBayarWindow.this.nim.getValue().trim();

					String program = (String) (searchprogram.getSelectedItem() == null
							|| searchprogram.getSelectedItem().getValue() == null ? null
									: searchprogram.getSelectedItem().getValue());

					final Integer angkatan = (Integer) (angkatanMhs.getSelectedItem() == null ? null
							: angkatanMhs.getSelectedItem().getValue());

					if (jenisPembayaran == null || tahunAkademik == null) {
						return;
					}

					Session session = HibernateUtil.currentSession();
					List<Object[]> jurusans = new ArrayList<Object[]>();
					if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU)) {

						String sql = "select a.no_ujian, a.nama, a.tahun, a.kewarganegaraan, "
								+ "y.nama as fakultas, x.nama as jurusan " +

								"from biodata_calon_mahasiswa a "
								+ " left join kegiatan b on (b.mahasiswa=a.id and b.tahun_akademik='" + tahunAkademik
								+ "' and b.jenis_kegiatan = " + jenisPembayaran.getId()
								+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and b.semster = " + semesterKe)
								+ ((semester.equals(Perkuliahan.GENAP) ? " and b.semster % 2 = 0 "
										: "  and b.semster % 2 = 1 "))
								+ ") inner join jurusan x on (a.prodi_lulus = x.id  ) "
								+ "left join fakultas y on (x.fakultas = y.id  )  where b.id is null "
								+ (nim.trim().equals("") ? "" : "and a.no_ujian ilike '%" + nim + "%' ") + "  "
								+ (wnawni == null ? "" : "and a.kewarganegaraan = '" + wnawni + "'")
								+ (angkatan == null ? "" : " and a.tahun = " + angkatan + " ")
								+ (jurusan == null ? "" : " and a.prodi_lulus = " + jurusan.getId())
								+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
								+ (program == null ? "" : " and a.program = '" + program + "'") + " order by a.nama";

						System.out.println(sql);
						jurusans = Common.ambilSql(sql);

					} else if (jenisPembayaran.getNamaKegiatan().equals(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA)) {

						String sql = "select a.no_registrasi, a.nama, a.tahun, a.kewarganegaraan, "
								+ "'' as fakultas, '' as jurusan " +

								"from biodata_calon_mahasiswa a "
								+ " left join kegiatan b on (b.mahasiswa=a.id and b.tahun_akademik='" + tahunAkademik
								+ "' and b.jenis_kegiatan = " + jenisPembayaran.getId()
								+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and b.semster = " + semesterKe)
								+ ((semester.equals(Perkuliahan.GENAP) ? " and b.semster % 2 = 0 "
										: "  and b.semster % 2 = 1 "))
								+ ") "
								+ " left join jurusan c1 on (a.prodi_1=c1.id)\n   left join jurusan c2 on (a.prodi_2=c2.id)\n "
								+ " left join jurusan c3 on (a.prodi3=c3.id)\n   left join jurusan c4 on (a.prodi4=c4.id)\n "
								+ " left join jurusan c5 on (a.prodi5=c5.id)\n  " + "where b.id is null  "
								+ (nim.trim().equals("") ? "" : "and a.no_registrasi ilike '%" + nim + "%' ") + "  "
								+ (program == null ? "" : " and a.program = '" + program + "'")
								+ (wnawni == null ? "" : "and a.kewarganegaraan = '" + wnawni + "'")
								+ " and a.prodi_lulus is null "
								+ (angkatan == null ? "" : " and a.tahun = " + angkatan + " ")
								+ (jurusan == null ? ""
										: (" and (a.prodi_1 = " + jurusan.getId() + " or a.prodi_2 = " + jurusan.getId()
												+ " or a.prodi3 = " + jurusan.getId() + " or a.prodi4 = "
												+ jurusan.getId() + "  or a.prodi5 = " + jurusan.getId() + ")"))
								+ (fakultas == null ? ""
										: (" and (c1.fakultas = " + fakultas.getId() + " or c2.fakultas = "
												+ fakultas.getId() + " or c3.fakultas = " + fakultas.getId()
												+ " or c4.fakultas = " + fakultas.getId() + " or c5.fakultas = "
												+ fakultas.getId() + " )"))
								+ " order by a.nama";

						System.out.println(sql);
						jurusans = Common.ambilSql(sql);

					} else {

						String sql = "select a.nim, a.nama, a.tahunangkatan, a.warganegara, "
								+ "y.nama as fakultas, x.nama as jurusan " +

								"from mahasiswa a " + " left join jurusan x on (a.jurusan = x.id  )  "
								+ "left join fakultas y on (x.fakultas = y.id  )  "

								+ " where a.id not in (select b.mahasiswa from kegiatan b where b.tahun_akademik='"
								+ tahunAkademik + "' and b.jenis_kegiatan = " + jenisPembayaran.getId()
								+ (semesterKe == null || semesterKe.equals(-1) ? "" : " and b.semster = " + semesterKe)
								+ ((semester.equals(Perkuliahan.GENAP) ? " and b.semster % 2 = 0 "
										: "  and b.semster % 2 = 1 "))
								+ ") "

								+ (nim.trim().equals("") ? "" : "and a.nim ilike '%" + nim + "%' ") + " "
								+ (wnawni == null ? "" : " and a.warganegara = '" + wnawni + "'") + " "
//								+ (statusMahasiswa == null ? "" : " and a.status = " + statusMahasiswa.getId())
								+ (angkatan == null ? "" : " and a.tahunangkatan = " + angkatan + " ")
								+ (jurusan == null ? "" : " and a.jurusan = " + jurusan.getId())
								+ (fakultas == null ? "" : " and x.fakultas = " + fakultas.getId())
								+ (program == null ? "" : " and a.program = '" + program + "'") + " order by a.nama";

						System.out.println(sql);
						jurusans = Common.ambilSql(sql);
					}

					spreadsheet = new ais.ui.util.MySpreadsheet();
	Common.clear(center);spreadsheet.setParent(center);
					spreadsheet.setWidth("100%");
					spreadsheet.setHeight("100%");
					spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
					spreadsheet.setMaxcolumns(6);
					spreadsheet.setMaxrows(jurusans.size() + 4);

					Worksheet sheet = spreadsheet.getSelectedSheet();
					sheet.setDefaultColumnWidth(40);
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(0, 0, spreadsheet.getMaxcolumns() - 1, spreadsheet.getMaxrows() - 1), false);

					ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 0, "REKAPITULASI MAHASISWA YANG BELUM MELAKUKAN "
							+ (jenisPembayaran.getNamaKegiatan().toUpperCase()) + "\n "
							+ Common.getBahasaConfig("Fakultas") + " "
							+ (fakultas == null ? "SEMUA" : fakultas.getNama().toUpperCase()) + "\n"
							+ Common.getBahasaConfig("Jurusan") + " "
							+ (jurusan == null ? "SEMUA" : jurusan.getNama().toUpperCase()) + "\n TAHUN AKADEMIK "
							+ tahunAkademik + "\nPROGRAM " + (program == null ? "SEMUA" : program.toUpperCase())
							+ "\n SEMESTER " + semester.toUpperCase() + "\n ANGKATAN "
							+ (angkatan == null ? "SEMUA" : angkatan));
					final String color = "#000000";
					int rowIndex = 2;
					int colIndex = 0;
					Utils.setRowHeight(sheet, 1, 150);
					ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
					Cell cell = Utils.getCell(sheet, 1, 0);
					cell.getCellStyle().setWrapText(true);
					cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);

					ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 0, 1, spreadsheet.getMaxcolumns() - 1, false);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, "NIM/No.Reg/No.Ujian");
					Utils.setColumnWidth(sheet, 0, 130);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "Nama");
					Utils.setColumnWidth(sheet, 1, 200);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "Angkatan");
					Utils.setColumnWidth(sheet, 2, 80);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, "WNI/WNA");
					Utils.setColumnWidth(sheet, 3, 80);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, "Fakultas");
					Utils.setColumnWidth(sheet, 4, 150);
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, "Jurusan");
					Utils.setColumnWidth(sheet, 5, 150);

					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
							BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					ais.ui.util.EcampusUtil.setBold(sheet,
							new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), true);

					rowIndex = 3;
					for (Object[] kegiatan : jurusans) {

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0,
								kegiatan[0] == null ? "" : kegiatan[0].toString());
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1,
								kegiatan[1] == null ? "" : kegiatan[1].toString().toUpperCase());
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2,
								kegiatan[2] == null ? "" : kegiatan[2].toString());
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3,
								kegiatan[3] == null ? "" : kegiatan[3].toString());
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4,
								kegiatan[4] == null ? "" : kegiatan[4].toString());

						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5,
								kegiatan[5] == null ? "" : kegiatan[5].toString());

						rowIndex++;

					}

					// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
					ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Mahasiswa Belum Bayar Window", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});
	}
}
