package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataKelasBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.Perkuliahan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan khs per prodi dan angkatan. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox fakultas}, {@code Combobox
 * jurusan}, {@code Combobox semester}, {@code AmbilDataKelasBanbox kelas}, {@code Textbox nims}, {@code
 * MyCheckboxConfig hitungUlang}, {@code MyCheckboxConfig remedial}, {@code Intbox angkatan};
 * inisialisasi/lifecycle ({@code initKHS()}, {@code init()}); operasi domain lain ({@code generateParameter()},
 * {@code onKHS()}, {@code isiParameterTtdDosenPa()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanKHSPerProdiDanAngkatan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox semester;
	private AmbilDataKelasBanbox kelas;
	private Textbox nims;
	private MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK");
	private MyCheckboxConfig remedial = new MyCheckboxConfig("Remedial");
	private Intbox angkatan;

	private Center center;
	private MyCheckboxConfig semesterPendek;
	private Toolbar toolbar;

	private MyDatebox tanggal;

	public LaporanKHSPerProdiDanAngkatan() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan KHS Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKHSPerProdiDanAngkatan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		semester = new Combobox();
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}
		Common.selectComboItem(semester, 1);

		angkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

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
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas" + " *"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasaConfig("Jurusan") + " *"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		// jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		row.appendChild(semester);
		semester.setWidth("90%");
		// semester.addEventListener("onChange", eventListener);
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan *"));
		row.appendChild(angkatan);
		angkatan.setWidth("90%");
		// angkatan.addEventListener("onChange", eventListener);
		// angkatan.addEventListener(Events.ON_OK, eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new AmbilDataKelasBanbox());
		kelas.setWidth("90%");
		kelas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nims = new Textbox());
		nims.setWidth("90%");
		nims.setRows(5);

		Common.initKeterangan(rows,
				"Jika KHS ini khusus nim tertentu, masukkan nim yang pisah menggunakan tanda koma (,). Misal : 1209902323,1209902324,1209902325");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungUlang);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(remedial);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Fakultas belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Fakultas dari daftar dropdown; (2) Pastikan data Fakultas tersedia di sistem; (3) Ulangi proses cetak laporan KHS. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"), "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Semester belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Semester (Ganjil/Genap) dari daftar dropdown; (2) Pastikan data Semester tersedia; (3) Ulangi proses cetak laporan KHS. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (angkatan.getValue() == null) {
					MyMessageboxConfig.show("Mohon maaf, Angkatan belum diisi. Langkah yang dapat dilakukan: (1) Masukkan atau pilih Tahun Angkatan mahasiswa; (2) Pastikan angkatan yang dimaksud terdaftar di sistem; (3) Ulangi proses cetak laporan KHS. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Kartu_Hasil_Studi_Per_Prodi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		// onKHS(null);
		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Pilih " + "Fakultas", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Pilih semester", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (angkatan.getValue() == null) {
			MyMessageboxConfig.show("Pilih angkatan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		List<String> nims = new ArrayList<String>();
		for (String nim : this.nims.getValue().trim().split(",")) {
			if (!nim.trim().isEmpty()) {
				nims.add(nim.trim());
			}
		}

		String kel = kelas.getValue().trim();

		List<Mahasiswa> mahasiswas = ConstantValues
				.simpleList(HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("jurusan", jurusan))
						.add(nims.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("nim", nims))
						.add(Restrictions.eq("tahunangkatan", angkatan.getValue()))
						.add(kel != null && !kel.trim().isEmpty()
								? Restrictions.ilike("kelas", kel.trim(), MatchMode.EXACT)
								: Restrictions.sqlRestriction("true")),
						Mahasiswa.class);

		if (nims.isEmpty()) {
			nims.add("-1");
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("nims", nims.toArray());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("semester", semester.getSelectedItem().getValue());
		parameters.put("tahunangkatan", angkatan.getValue() == null ? -1 : angkatan.getValue());
		parameters.put("tanggal", tanggal.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : tanggal.getValue());
		parameters.put("kaprodi", "(                                          )");
		parameters.put("nip", "");
		parameters.put("kelas", kelas.getValue().trim());

		String subReport = Common.ambilREAL_PATH_REPORT() + "/"
				+ Common.getKonfigurasi("Report_Kartu_Hasil_Studi", "").getInfo1();

		File jasper = new File(subReport);
		File fileJasper = CommonReport.generateFileJasper(jasper.getName(), "Kartu_Hasil_Studi");

		subReport = fileJasper.getAbsolutePath();
		System.out.println("subReport = " + subReport);
		parameters.put("SUBREPORT_DIR_KHS", subReport);

		for (Mahasiswa mahasiswa : mahasiswas) {

			try {
				Fakultas fakultas = mahasiswa.getJurusan().getFakultas();

				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
						(Integer) semester.getSelectedItem().getValue(), null,
						semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null, hitungUlang.isChecked());

				PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = Common
						.getIpkUntukPengambilanKRS(mahasiswa, (Integer) semester.getSelectedItem().getValue(),
								mahasiswa.getTahunangkatan(), fakultas, jurusan, mahasiswa.getProgram(),
								krsMahasiswa.getSemesterPendek());

				PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRSBerikutNya = Common
						.getIpkUntukPengambilanKRS(mahasiswa, ((Integer) semester.getSelectedItem().getValue()) + 1,
								mahasiswa.getTahunangkatan(), fakultas, jurusan, mahasiswa.getProgram(),
								krsMahasiswa.getSemesterPendek());

				parameters.put("dosenpa_" + mahasiswa.getId(),
						krsMahasiswa == null ? ""
								: krsMahasiswa.getDosenPa() == null ? "......................."
										: krsMahasiswa.getDosenPa().getNama());
				parameters.put("nipdosenpa_" + mahasiswa.getId(),
						krsMahasiswa.getDosenPa() == null ? "......................."
								: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
										: krsMahasiswa.getDosenPa().getCode()));

				Double ipmhs = krsMahasiswa.getIps();
				Double ipkmhs = krsMahasiswa.getIpk();

				Integer sksmhss = krsMahasiswa.getSksYangDiambil();
				Integer sksmhs = krsMahasiswa.getSksk();

				System.out.println(mahasiswa + " ipkmhs => " + ipkmhs);
				System.out.println(mahasiswa + " ipmhs => " + ipmhs);

				parameters.put("max_sks_" + mahasiswa.getId(), pembatasanNilaiIPKUntukPengambilanKRS == null ? null
						: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

				parameters.put("max_sks_next_" + mahasiswa.getId(),
						pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
								: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya
										.getBatasMaksimumIPKYangBolehDiambil());

				parameters.put("max_sks_berikut_" + mahasiswa.getId(),
						pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
								: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya
										.getBatasMaksimumIPKYangBolehDiambil());

				if ((Integer) semester.getSelectedItem().getValue() > 1) {
					Double iplast = Common.ipTerakhir(mahasiswa, (Integer) semester.getSelectedItem().getValue());
					parameters.put("ip_sebelumnya_" + mahasiswa.getId(), iplast);
				}

				Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "_" + mahasiswa.getId());
				Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs_" + mahasiswa.getId(), 1,
						"mahasiswa");

				if (krsMahasiswa.getDosenPa() != null) {
					Common.insertProperty(Dosen.class, krsMahasiswa.getDosenPa(), parameters,
							"dosen_pa_" + mahasiswa.getId(), 1);
				}

				parameters.put("ipk_" + mahasiswa.getId(), ipkmhs);
				parameters.put("ips_" + mahasiswa.getId(), ipmhs);
				parameters.put("sksk_" + mahasiswa.getId(), sksmhs);
				parameters.put("sks_" + mahasiswa.getId(), sksmhss);

				parameters.put("ipk_" + mahasiswa.getNim(), ipkmhs);
				parameters.put("ips_" + mahasiswa.getNim(), ipmhs);
				parameters.put("sksk_" + mahasiswa.getNim(), sksmhs);
				parameters.put("sks_" + mahasiswa.getNim(), sksmhss);

				parameters.put("dekan_" + mahasiswa.getNim(),
						mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
								: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
				parameters.put("nip_dekan_" + mahasiswa.getNim(),
						mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
								: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
				parameters.put("nidn_dekan_" + mahasiswa.getNim(),
						mahasiswa.getJurusan().getFakultas().getDekan() == null ? "....................."
								: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

				parameters.put("pudek1_" + mahasiswa.getNim(),
						mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
								: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
				parameters.put("nip_pudek1_" + mahasiswa.getNim(),
						mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
								: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
				parameters.put("nidn_pudek1_" + mahasiswa.getNim(),
						mahasiswa.getJurusan().getFakultas().getPudek1() == null ? "....................."
								: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

				Map parametersData = new HashMap();
				LaporanKHS.initParametermapKhs(parametersData,
						semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null, remedial.isChecked(),
						mahasiswa, krsMahasiswa.getSemester(), null, krsMahasiswa, false, hitungUlang.isChecked());
				parameters.put("data_" + mahasiswa.getNim(), parametersData.get("maps"));

				parameters.put("remedial_" + mahasiswa.getNim(), remedial.isChecked());

				if (Common.bolehKonfigurasi("tampilkan_total_mutu_di_krs", Konfigurasi.TIDAK_AKTIF)) {
					parameters.put("total_nilai_mutu_" + mahasiswa.getNim(), mahasiswa.hitungMutuSampaiSemester(
							(Integer) semester.getSelectedItem().getValue(), null, null, true));

					parameters.put("total_nilai_ip_" + mahasiswa.getNim(), mahasiswa.hitungNilaiIpSampaiSemester(
							(Integer) semester.getSelectedItem().getValue(), null, null, true));
				}

				parameters.put("ip_kumulatif_" + mahasiswa.getNim(), ipkmhs);

				isiParameterTtdDosenPa(parameters, mahasiswa, krsMahasiswa);

				Integer semester = (Integer) this.semester.getSelectedItem().getValue();
				if (hitungUlang.isChecked()) {
					mahasiswa.reInitDetailperkuliahan(HibernateUtil.currentSession());
				}
				krsMahasiswa = semester <= 1 ? null
						: Common.singkronkanKrsMahasiswa(mahasiswa, semester - 1, null,
								semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null,
								hitungUlang.isChecked());

				ipmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIps();
				ipkmhs = krsMahasiswa == null ? 0.0 : krsMahasiswa.getIpk();

				sksmhss = krsMahasiswa == null ? 0 : krsMahasiswa.getSksYangDiambil();
				sksmhs = krsMahasiswa == null ? 0 : krsMahasiswa.getSksk();

				parameters.put("ipk_1", ipkmhs);
				parameters.put("ips_1", ipmhs);
				parameters.put("sksk_1", sksmhs);
				parameters.put("sks_1", sksmhss);

				parameters.put("ip_kumulatif_1", ipkmhs);

				parameters.put("ip_semester_1", ipmhs);

				parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null);

				LampiranLain lampiranLain = LampiranLain.ambil(mahasiswa.getId(), LampiranLain.TTD_MAHASISWA);
				if (lampiranLain != null && lampiranLain.ambilFile() != null) {
					parameters.put("ttd_mahasiswa_" + mahasiswa.getId(), lampiranLain.ambilFile().getAbsolutePath());
				}

				if (Common.bolehKonfigurasi("tampilkan_total_nilai_di_krs", Konfigurasi.TIDAK_AKTIF)) {
					parameters.put("total_nilai", mahasiswa.hitungNilaiSampaiSemester(semester, null, null, true));
					if (semester > 1) {
						parameters.put("total_nilai_1",
								mahasiswa.hitungNilaiSampaiSemester(semester - 1, null, null, true));
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanKHSPerProdiDanAngkatan.java:505");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan KHS Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
		}

		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
							"Kartu_Hasil_Studi_Per_Prodi", ais.ui.util.WaktuUtil.getDate(), toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan KHS Per Prodi Dan Angkatan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

	}

	private void isiParameterTtdDosenPa(Map parameters, Mahasiswa mahasiswa, KrsMahasiswa krsMahasiswa) {
		if (mahasiswa == null) {
			return;
		}

		Dosen pa = krsMahasiswa == null ? mahasiswa.getDosenPa() : krsMahasiswa.getDosenPa();
		if (pa == null) {
			return;
		}

		LampiranLain lampiranLain = LampiranLain.ambil(pa.getId(), LampiranLain.TTD_DOSEN);
		if (lampiranLain == null || lampiranLain.ambilFile() == null) {
			return;
		}

		String path = lampiranLain.ambilFile().getAbsolutePath();
		parameters.put("krs.dosenPa.ttd.dosen_" + mahasiswa.getId(), path);
		parameters.put("krs_" + mahasiswa.getId() + ".dosenPa.ttd.dosen", path);
		parameters.put("dosen_pa_" + mahasiswa.getId() + ".ttd.dosen", path);
		parameters.put("ttd_dosen_pa_" + mahasiswa.getId(), path);
		if (mahasiswa.getNim() != null && !mahasiswa.getNim().trim().isEmpty()) {
			parameters.put("krs.dosenPa.ttd.dosen_" + mahasiswa.getNim(), path);
			parameters.put("krs_" + mahasiswa.getNim() + ".dosenPa.ttd.dosen", path);
			parameters.put("dosen_pa_" + mahasiswa.getNim() + ".ttd.dosen", path);
			parameters.put("ttd_dosen_pa_" + mahasiswa.getNim(), path);
		}
	}

}
