package ais.action.report.helper.nilai;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonSorter;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan daftar prestasi belajar teori praktek window. Kelas ini
 * mengubah data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa
 * memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox searchfakultas}, {@code
 * Combobox searchjurusan}, {@code Combobox tahunAkademik}, {@code Combobox semesterAbsensi}, {@code Combobox
 * searchsemester}, {@code Textbox kelas}, {@code Spreadsheet spreadsheet}, {@code Center center};
 * inisialisasi/lifecycle ({@code init()}, {@code initMahasiswa()}, {@code initSpreadsheet()}); konfigurasi
 * constructor: {@code selectedMahasiswa}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanDaftarPrestasiBelajarTeoriPraktekWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	private Combobox tahunAkademik = new Combobox();
	private Combobox semesterAbsensi = new Combobox();
	private Combobox searchsemester = new Combobox();
	private Textbox kelas = new Textbox();

	private Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
	private Center center = new Center();
	private List<CommonSorter> mahasiswas = new ArrayList<CommonSorter>();
	private TreeMap<String, Matakuliah> matakuliahs = new TreeMap<String, Matakuliah>(Collections.reverseOrder());
	private List<Long> perkuliahans;

	private Perkuliahan selectedPerkuliahan = null;

	private Mahasiswa selectedMahasiswa = null;

	private MyCheckboxConfig hanya1Smt;

	private MyCheckboxConfig pisahkanTeoriDanPraktek;

	public LaporanDaftarPrestasiBelajarTeoriPraktekWindow() {
		super();
		Tbmuser tbmuser = Common.getCurrentUser();
		selectedMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		try {
			Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Prestasi Belajar Teori Praktek Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Prestasi Belajar Teori Praktek Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarPrestasiBelajarTeoriPraktekWindow(Perkuliahan perkuliahan) {
		super();
		Tbmuser tbmuser = Common.getCurrentUser();
		selectedMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		this.selectedPerkuliahan = perkuliahan;

		try {
			Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Prestasi Belajar Teori Praktek Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Prestasi Belajar Teori Praktek Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanDaftarPrestasiBelajarTeoriPraktekWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		Tbmuser tbmuser = Common.getCurrentUser();
		selectedMahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);
		init();
		try {
			initSpreadsheet();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Daftar Prestasi Belajar Teori Praktek Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings("deprecation")
	private void init() {

		setClosable(true);
		// setTitle("Daftar Prestasi Belajar");
		setWidth("100%");
		setHeight("100%");
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("240px");
		north.setAutoscroll(true);

		if (selectedPerkuliahan != null || selectedMahasiswa != null) {
			north.setVisible(false);
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas *"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi *"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		Common.generateTahunAjaran(tahunAkademik);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester *"));
		row.appendChild(semesterAbsensi);
		semesterAbsensi.setWidth("90%");
		semesterAbsensi.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		Common.selectComboItem(semesterAbsensi, Common.getSemesterString());
		semesterAbsensi.setReadonly(true);
		
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(searchsemester);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox());
		kelas.setWidth("90%");

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchsemester);
				searchsemester.setSelectedItem(null);

				if (semesterAbsensi.getSelectedItem() == null) {
					return;
				}
				if (semesterAbsensi.getSelectedItem().getValue() == null) {
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					for (int i = 1; i < 30; i++) {
						comboitem = new MyComboitemConfig();
						comboitem.setLabel(i + "");
						comboitem.setValue(i);
						searchsemester.appendChild(comboitem);
					}
				} else {
					Boolean genap = semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP);
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel("Semua");
					comboitem.setValue(null);
					searchsemester.appendChild(comboitem);
					if (genap) {
						for (int i : Common.genap) {
							if (i == 0)
								continue;
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					} else {
						for (int i : Common.ganjil) {
							comboitem = new MyComboitemConfig();
							comboitem.setLabel(i + "");
							comboitem.setValue(i);
							searchsemester.appendChild(comboitem);
						}
					}
				}

				searchsemester.setSelectedIndex(0);
				searchsemester.setReadonly(true);
			}
		};

		semesterAbsensi.addEventListener("onChange", eventListener);
		try {
			eventListener.onEvent(null);
		} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/report/helper/nilai/LaporanDaftarPrestasiBelajarTeoriPraktekWindow.java:265");
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "6");
		Hbox hbox = new Hbox();
		hanya1Smt = new MyCheckboxConfig("Tampilan MK hanya satu semester");
		hanya1Smt.setChecked(true);

		pisahkanTeoriDanPraktek = new MyCheckboxConfig("Pisahkan mk teori dan praktek");
		pisahkanTeoriDanPraktek.setChecked(false);

		row.appendChild(hbox);
		hbox.appendChild(hanya1Smt);

		EventListener tampilkan = new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(center);
				if (selectedPerkuliahan == null && selectedMahasiswa == null) {
					if (tahunAkademik.getSelectedItem() == null)
						return;
					if (semesterAbsensi.getSelectedItem() == null)
						return;
					if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null)
						return;
					if (searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null)
						return;
				}
				perkuliahans = null;

				if (selectedMahasiswa != null) {
					perkuliahans = selectedMahasiswa.ambilDetailperkuliahan();
				} else if (selectedPerkuliahan != null) {
					perkuliahans = new ArrayList<Long>();
				} else {
					perkuliahans = HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.property("id"))
							.add(kelas.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.ilike("kelas", kelas.getValue().trim(), MatchMode.ANYWHERE))
							.add(Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))

							.add(semesterAbsensi.getSelectedItem() == null
									|| semesterAbsensi.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.eq("ganjilGenap", semesterAbsensi.getSelectedItem().getValue()))

							.add(searchsemester.getSelectedItem() == null
									|| searchsemester.getSelectedItem().getValue() == null
											? Restrictions.sqlRestriction("1=1")
											: Restrictions.sqlRestriction(
													"this_.semester = " + searchsemester.getSelectedItem().getValue())

							).add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
							.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
									Restrictions.isNull("merupakan_paralel")))
							.addOrder(Order.desc("id")).list();
				}

				matakuliahs = null;
				matakuliahs = new TreeMap<String, Matakuliah>(Collections.reverseOrder());
				for (Long perkuliahanid : perkuliahans) {
					Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
							perkuliahanid);
					if (perkuliahan == null || perkuliahan.getMatakuliah() == null)
						continue;
					matakuliahs.put(perkuliahan.getMatakuliah().getKode(), perkuliahan.getMatakuliah());
				}
				initSpreadsheet();
			}
		};

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "6");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", tampilkan);
		print.setParent(toolbar);

		print = new MyToolbarbuttonConfig("Ambil File", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				spreadsheet.getBook().write(bout);
				bout.close();
				Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "daftar_prestasi_mahasiswa.xlsx");
			}
		});
		print.setParent(toolbar);

		if (selectedPerkuliahan != null || selectedMahasiswa != null) {
			try {
				tampilkan.onEvent(null);

				Common.clear(north);
				north.setVisible(true);

				toolbar = new Toolbar();
				toolbar.setParent(north);

				print = new MyToolbarbuttonConfig("Download", "/img/print.png");
				print.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						ByteArrayOutputStream bout = new ByteArrayOutputStream();
						spreadsheet.getBook().write(bout);
						bout.close();
						Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
								"daftar_prestasi_mahasiswa.xlsx");
					}
				});
				print.setParent(toolbar);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/helper/nilai/LaporanDaftarPrestasiBelajarTeoriPraktekWindow.java:392");
				PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Daftar Prestasi Belajar Teori Praktek Window", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}
	}

	private void initMahasiswa() {
		mahasiswas = null;
		mahasiswas = new ArrayList<CommonSorter>();
		String ganjilGenap = (String) semesterAbsensi.getSelectedItem().getValue();
		String ta = (String) tahunAkademik.getSelectedItem().getValue();
		if (perkuliahans == null) {
			return;
		}

		if (selectedPerkuliahan != null) {
			ganjilGenap = selectedPerkuliahan.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
			ta = selectedPerkuliahan.getTahunAjaran();
		}

		if (selectedMahasiswa != null) {
			CommonSorter commonSorter = new CommonSorter();

			Integer tahunAngkatanMhs = selectedMahasiswa.getTahunangkatan();

			Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			Integer smt = Common.getSemester(tahunAngkatanMhs, ganjilGenap,
					selectedMahasiswa.getPindahKeKampusIniMasukSemester(), tahun, selectedMahasiswa.getSemesterMulai());

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(selectedMahasiswa, smt, null, null);

			Double ipmhs = krsMahasiswa.getIps();
			commonSorter.setSerializable1(krsMahasiswa);
			commonSorter.setSerializable(selectedMahasiswa);
			commonSorter.setValue(ipmhs);
			mahasiswas.add(commonSorter);
		} else if (selectedPerkuliahan != null) {
			for (Mahasiswa mahasiswa : selectedPerkuliahan.ambilMahasiswa()) {
				CommonSorter commonSorter = new CommonSorter();

				Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
				Integer smt = Common.getSemester(tahunAngkatanMhs, ganjilGenap,
						mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null);

				Double ipmhs = krsMahasiswa.getIps();
				commonSorter.setSerializable1(krsMahasiswa);
				commonSorter.setSerializable(mahasiswa);
				commonSorter.setValue(ipmhs);
				mahasiswas.add(commonSorter);
			}
		} else {

			Collection<Long> longsMhs = selectedPerkuliahan == null ? null : selectedPerkuliahan.ambilMahasiswaId(false);
			Map<Long, Mahasiswa> mahasiswass = new HashMap<Long, Mahasiswa>();
			for (Long perkuliahanid : perkuliahans) {
				Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
						perkuliahanid);
				if (perkuliahan != null) {
					for (Long detailperkuliahanid : perkuliahan.ambilDetailperkuliahan()) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
							if (longsMhs == null || longsMhs.contains(mahasiswa.getId())) {
								mahasiswass.put(mahasiswa.getId(), mahasiswa);
							}
						}
					}
				}
			}

			for (Mahasiswa mahasiswa : mahasiswass.values()) {
				CommonSorter commonSorter = new CommonSorter();

				Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
				Integer smt = Common.getSemester(tahunAngkatanMhs, ganjilGenap,
						mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null);

				Double ipmhs = krsMahasiswa.getIps();
				commonSorter.setSerializable1(krsMahasiswa);
				commonSorter.setSerializable(mahasiswa);
				commonSorter.setValue(ipmhs);
				mahasiswas.add(commonSorter);
			}
		}
		Collections.sort(mahasiswas);

		if (selectedPerkuliahan != null) {
			Matakuliah matakuliah = selectedPerkuliahan.getMatakuliah();
			matakuliahs.put(matakuliah.getKode(), matakuliah);
			for (CommonSorter commonSorter : mahasiswas) {
				Mahasiswa mahasiswa = (Mahasiswa) commonSorter.getSerializable();
				KrsMahasiswa krsMahasiswa = (KrsMahasiswa) commonSorter.getSerializable1();
				List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
				for (Long detailperkuliahan2id : detailperkuliahans) {
					Detailperkuliahan detailperkuliahan2 = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahan2id.toString());
					if (detailperkuliahan2 != null) {
						if (krsMahasiswa.getSemester().equals(detailperkuliahan2.getSemester())) {
							Matakuliah matakuliah2 = detailperkuliahan2.getMatakuliahKonversi() != null
									? detailperkuliahan2.getMatakuliahKonversi()
									: detailperkuliahan2.getPerkuliahan() != null
											? detailperkuliahan2.getPerkuliahan().getMatakuliah()
											: null;
							if (matakuliah2 == null) {
								continue;
							}
							matakuliahs.put(matakuliah2.getKode(), matakuliah2);
						}
					}
				}
			}
		}
	}

	// private void

	private void initSpreadsheet() throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(center);
				if (perkuliahans == null) {
					return;
				}
				initMahasiswa();
				spreadsheet = new ais.ui.util.MySpreadsheet();
Common.clear(center);spreadsheet.setParent(center);
				spreadsheet.setWidth("100%");
				spreadsheet.setHeight("100%");
				spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
				spreadsheet.setMaxcolumns((matakuliahs.size() * 4) + 10);
				spreadsheet.setMaxrows(mahasiswas.size() + 8);
				final String color = "#000000";

				Worksheet sheet = spreadsheet.getSelectedSheet();

				int rowIndex = 4;
				int colIndex = 3;
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, "NIM");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, "NAMA");
				int left = 0;
				int right = 0;
				for (Matakuliah matakuliah : matakuliahs.values()) {
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex,
							matakuliah.getKode() + "\n" + matakuliah.getNama());
					ais.ui.util.EcampusUtil.setCellValue(sheet, 2, colIndex, matakuliah.getId());
					ais.ui.util.EcampusUtil.setCellValue(sheet, 6, colIndex, "S");
					ais.ui.util.EcampusUtil.setCellValue(sheet, 6, colIndex + 1, "M");
					ais.ui.util.EcampusUtil.setCellValue(sheet, 6, colIndex + 2, "B");
					ais.ui.util.EcampusUtil.setCellValue(sheet, 6, colIndex + 3, "P");
					ais.ui.util.EcampusUtil.setCellValue(sheet, 5, colIndex,
							matakuliah.getKode().toLowerCase().endsWith("p") ? "Praktek" : "Teori");
					ais.ui.util.EcampusUtil.mergeCells(sheet, 5, colIndex, 5, colIndex + 3, false);

					Utils.setColumnWidth(sheet, colIndex, 40);
					Utils.setColumnWidth(sheet, colIndex + 1, 40);
					Utils.setColumnWidth(sheet, colIndex + 2, 40);
					Utils.setColumnWidth(sheet, colIndex + 3, 40);
					Cell cell = Utils.getCell(sheet, rowIndex, colIndex);
					if (cell != null) {
						cell.getCellStyle().setWrapText(true);
						cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
						if (selectedPerkuliahan != null && selectedPerkuliahan.getMatakuliah() != null
								&& selectedPerkuliahan.getMatakuliah().getId().equals(matakuliah.getId())) {
							left = colIndex;
							right = colIndex + 2;
						}
					}
					ais.ui.util.EcampusUtil.mergeCells(sheet, rowIndex, colIndex, rowIndex, colIndex + 3, false);
					colIndex += 4;
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, "SKS MK");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1, "SKS MK x MUTU");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 2, "SMT");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 3, "SKS SMT");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 4, "SKSK");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 5, "IPS");
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 6, "IPK");

				Utils.setBackgroundColor(sheet, new Rect(left, rowIndex, right, rowIndex + mahasiswas.size() + 1),
						"#4ac6e0");
				Utils.setFontBold(sheet, new Rect(left, rowIndex, right, rowIndex + mahasiswas.size() + 1), true);

				Utils.setBackgroundColor(sheet,
						new Rect(colIndex, rowIndex, colIndex + 6, rowIndex + mahasiswas.size() + 1), "#67f754");
				Utils.setFontBold(sheet, new Rect(colIndex, rowIndex, colIndex + 6, rowIndex + mahasiswas.size() + 1),
						true);

				try {
					Utils.setColumnWidth(sheet, 2, 250);
					Utils.setColumnWidth(sheet, 1, 100);
					Utils.setColumnWidth(sheet, 0, 0);
					Utils.setRowHeight(sheet, 4, 70);
					Utils.setRowHeight(sheet, 2, 1);

					Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
							|| searchfakultas.getSelectedItem().getValue() == null
							|| searchfakultas.getSelectedItem().getValue() == null ? null
									: searchfakultas.getSelectedItem().getValue());
					Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
							|| searchjurusan.getSelectedItem().getValue() == null
							|| searchjurusan.getSelectedItem().getValue() == null ? null
									: searchjurusan.getSelectedItem().getValue());
					String tahunAkademik = (String) (LaporanDaftarPrestasiBelajarTeoriPraktekWindow.this.tahunAkademik
							.getSelectedItem() == null ? null
									: LaporanDaftarPrestasiBelajarTeoriPraktekWindow.this.tahunAkademik
											.getSelectedItem().getValue());
					String semester = (String) (LaporanDaftarPrestasiBelajarTeoriPraktekWindow.this.semesterAbsensi
							.getSelectedItem() == null ? null
									: LaporanDaftarPrestasiBelajarTeoriPraktekWindow.this.semesterAbsensi
											.getSelectedItem().getValue());
					ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1,
							"DAFTAR PRESTASI BELAJAR MAHASISWA\n " + (selectedPerkuliahan != null
									? selectedPerkuliahan.getSemester() + " " + selectedPerkuliahan.getKelas() + " "
											+ selectedPerkuliahan.getTahunAjaran() + " "
											+ selectedPerkuliahan.getProgram()
									: selectedMahasiswa != null
											? selectedMahasiswa.getNim() + " - " + selectedMahasiswa.getNama()
											: "" + Common.getBahasaConfig("Fakultas") + " "
													+ fakultas.getNama().toUpperCase() + "\n "
													+ Common.getBahasaConfig("Jurusan") + " "
													+ jurusan.getNama().toUpperCase() + "\n TAHUN AKADEMIK "
													+ tahunAkademik + "\n SEMESTER " + semester));
					Utils.setRowHeight(sheet, 1, 100);
					ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
					Cell cell = Utils.getCell(sheet, 1, 1);
					cell.getCellStyle().setWrapText(true);
					cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Daftar Prestasi Belajar Teori Praktek Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
				try {
					ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
							BorderStyle.THIN, color);
					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1),
							BookHelper.BORDER_FULL, BorderStyle.THIN, color);
					ais.ui.util.EcampusUtil.setBorder(sheet,
							new Rect(1, rowIndex + 2, spreadsheet.getMaxcolumns() - 1, rowIndex + 2),
							BookHelper.BORDER_FULL, BorderStyle.THIN, color);
				} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/report/helper/nilai/LaporanDaftarPrestasiBelajarTeoriPraktekWindow.java:643");

				}

				rowIndex = 7;
				colIndex = 1;
				for (CommonSorter commonSorter : mahasiswas) {
					Mahasiswa mahasiswa = (Mahasiswa) commonSorter.getSerializable();
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, mahasiswa.getId());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, mahasiswa.getNim());
					ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex + 1,
							mahasiswa.getNama().toUpperCase());
					rowIndex++;
				}

				try {
					ais.ui.util.EcampusUtil.mergeCells(sheet, 4, 1, 6, 1, false);
					ais.ui.util.EcampusUtil.mergeCells(sheet, 4, 2, 6, 2, false);
				} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/action/report/helper/nilai/LaporanDaftarPrestasiBelajarTeoriPraktekWindow.java:661");

				}
				int maxCol = spreadsheet.getMaxcolumns();
				int i = 7;
				int smt = 0;
				for (CommonSorter commonSorter : mahasiswas) {
					Mahasiswa mahasiswa = (Mahasiswa) commonSorter.getSerializable();
					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
					Integer sks = 0;
					Double sksMutu = 0.0;
					Double ipk = 0.0;
					Integer jml = 0;

					int y = 3;
					for (Matakuliah matakuliah : matakuliahs.values()) {

						Detailperkuliahan detailperkuliahan = null;
						for (Long detailperkuliahan2id : detailperkuliahans) {
							Detailperkuliahan detailperkuliahan2 = (Detailperkuliahan) GeneralValueObject
									.ambilData(Detailperkuliahan.class, detailperkuliahan2id.toString());
							if (detailperkuliahan2 != null) {
								Matakuliah matakuliah2 = detailperkuliahan2.getMatakuliahKonversi() != null
										? detailperkuliahan2.getMatakuliahKonversi()
										: detailperkuliahan2.getPerkuliahan() != null
												? detailperkuliahan2.getPerkuliahan().getMatakuliah()
												: null;
								if (matakuliah2 == null) {
									continue;
								}

								if (matakuliah2.getKode().equalsIgnoreCase(matakuliah.getKode())) {
									detailperkuliahan = detailperkuliahan2;
									break;
								}
							}
						}
						if (detailperkuliahan != null) {

							if (selectedPerkuliahan != null
									&& matakuliah.getId().equals(selectedPerkuliahan.getMatakuliah().getId())) {
								smt = detailperkuliahan.getSemester();
							}

							try {
								ais.ui.util.EcampusUtil.setCellValue(sheet, i, y, matakuliah.getSks());
								ais.ui.util.EcampusUtil.setCellValue(sheet, i, y + 1,
										detailperkuliahan.getNilaiHuruf());
								ais.ui.util.EcampusUtil.setCellValue(sheet, i, y + 2, detailperkuliahan.getTotalIP());
								Double ip = detailperkuliahan.getTotalIP();
								Double mxk = ip * matakuliah.getSks().doubleValue();
								ais.ui.util.EcampusUtil.setCellValue(sheet, i, y + 3, mxk);

								sks += matakuliah.getSks();
								sksMutu += mxk;

								Double val = ip * matakuliah.getSks();
								ipk += val;
								jml++;
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Daftar Prestasi Belajar Teori Praktek Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
										new String[] {
											"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
											"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
											"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
										});
							}
						} else {
							ais.ui.util.EcampusUtil.setCellValue(sheet, i, y, "X");
							ais.ui.util.EcampusUtil.setCellValue(sheet, i, y + 1, "X");
							ais.ui.util.EcampusUtil.setCellValue(sheet, i, y + 2, "X");
							ais.ui.util.EcampusUtil.setCellValue(sheet, i, y + 3, "X");
						}
						y += 4;
					}

					KrsMahasiswa krsMahasiswa = (KrsMahasiswa) (selectedPerkuliahan != null
							? Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null)
							: commonSorter.getSerializable1());

					ais.ui.util.EcampusUtil.setCellValue(sheet, i, maxCol - 7, sks);
					ais.ui.util.EcampusUtil.setCellValue(sheet, i, maxCol - 6, sksMutu);
					ais.ui.util.EcampusUtil.setCellValue(sheet, i, maxCol - 5, krsMahasiswa.getSemester());
					ais.ui.util.EcampusUtil.setCellValue(sheet, i, maxCol - 4, krsMahasiswa.getSksYangDiambil());
					ais.ui.util.EcampusUtil.setCellValue(sheet, i, maxCol - 3, krsMahasiswa.getSksk());
					ais.ui.util.EcampusUtil.setCellValue(sheet, i, maxCol - 2, krsMahasiswa.getIps());
					ais.ui.util.EcampusUtil.setCellValue(sheet, i, maxCol - 1, krsMahasiswa.getIpk());
					i++;
				}

				// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
				ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);

			}
		});

	}
}
