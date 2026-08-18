
package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.SyaratUjianAction;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.Perkuliahan;
import ais.database.model.Staff;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanKRS extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = -397946194166101691L;
	// private Combobox tahunAkademikUjianAkhirSemester;
	private Combobox semesterAbsensiUjian;
	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK");
	private MyCheckboxConfig remedial = new MyCheckboxConfig("Remedial");
	private Center center;
	private Toolbar toolbar;

	public LaporanKRS() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan KRS", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKRS(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		// tahunAkademikUjianAkhirSemester = new Combobox();
		semesterAbsensiUjian = new Combobox();
		// if (tahunAkademikUjianAkhirSemester != null) {
		// tahunAkademikUjianAkhirSemester = Common
		// .generateTahunAjaran(tahunAkademikUjianAkhirSemester);
		for (int i = 1; i <= 21; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semesterAbsensiUjian.appendChild(comboitem);
		}
		Common.selectComboItem(semesterAbsensiUjian, 1);
		// }
	}

	private MyDatebox tanggal;
	private MyCheckboxConfig semesterPendek;
	private MyDatebox tanggalDicetak;

	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Mahasiswa mhs = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				Integer smt = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();
				if (mhs != null) {

					Date dicetak = tanggalDicetak.getValue();
					try {

						final Integer tahunAngkatanMhs = mhs.getTahunangkatan();
						String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

						Integer tahunAkademikMulai = Common.getTahunAkademik(smt, tahunAngkatanMhs,
								mhs.getSemesterMulai());

						Konfigurasi konfigurasi = Common.getKonfigurasi("tanggal_ditetapkan_khs_"
								+ mhs.getJurusan().getId() + "_" + tahunAkademikMulai + "_" + semesterMulai,
								Common.dateFormat1.get().format(dicetak));

						if (event.getTarget() == tanggalDicetak) {
							konfigurasi.setNilai(Common.dateFormat1.get().format(tanggalDicetak.getValue()));
							Common.refreshSaveOrUpdate(konfigurasi);
						}

						dicetak = Common.dateFormat1.get().parse(konfigurasi.getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanKRS.java:138");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan KRS", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});

					}

					tanggalDicetak.setValue(dicetak);

					onKHS(event);
				}

			}
		};

		Tbmuser tbmuser = Common.getCurrentUser();
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensiUjian);
		semesterAbsensiUjian.setWidth("90%");
		semesterAbsensiUjian.addEventListener("onChange", eventListener);
		semesterAbsensiUjian.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (Common.getCurrentUser() != null && Common.getCurrentUser().getMahasiswa() != null) {
			Mahasiswa mahasiswa = Common.getCurrentUser().getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			bandboxMahasiswa.setDisabled(true);
			// Mahasiswa sudah pasti (pengguna = mahasiswa) → Semester default = semester berjalannya.
			Common.selectComboItem(semesterAbsensiUjian, mahasiswa.currentSemester());
		}

		// Saat mahasiswa DIPILIH lewat banbox, Semester otomatis diisi = semester berjalan mahasiswa
		// tersebut (mahasiswa.currentSemester()); setelah itu tetap boleh diganti manual. Delegasikan
		// ke eventListener bersama agar perilaku lama (generate/preview) tetap sama.
		bandboxMahasiswa.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Mahasiswa mhsTerpilih = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				if (mhsTerpilih != null) {
					Common.selectComboItem(semesterAbsensiUjian, mhsTerpilih.currentSemester());
				}
				eventListener.onEvent(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungUlang);
		hitungUlang.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));
		semesterPendek.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(remedial);
		remedial.addEventListener("onClick", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Ditetapkan"));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggal);
		tanggal.setWidth("90%");
		tanggal.addEventListener("onChange", eventListener);
		tanggal.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null);
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Cetak"));
		tanggalDicetak = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		row.appendChild(tanggalDicetak);
		tanggalDicetak.setWidth("90%");
		tanggalDicetak.addEventListener("onChange", eventListener);
		tanggalDicetak.setDisabled(tbmuser == null || tbmuser.getMahasiswa() != null);
		tanggalDicetak.setReadonly(true);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				// if (tahunAkademikUjianAkhirSemester.getSelectedItem()
				// ==
				// null) {
				// MyMessageboxConfig.show("Pilih salah satu tahun akademik",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.INFORMATION);
				// return null;
				// }
				if (semesterAbsensiUjian.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
					MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "Cetak_KRS_Mahasiswa", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	@SuppressWarnings({ "rawtypes" })
	public Map generateParameter() throws Exception {
		// if (tahunAkademikUjianAkhirSemester.getSelectedItem() == null) {
		// // MyMessageboxConfig.show("Pilih salah satu tahun akademik",
		// "Peringatan",
		// // 1,
		// // MyMessageboxConfig.INFORMATION);
		// return null;
		// }
		if (semesterAbsensiUjian.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih ganjil atau genap", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (bandboxMahasiswa.getAttribute("mahasiswa") == null) {
			// MyMessageboxConfig.show("Pilih Mahasiswa", "Peringatan",
			// MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");

		int semester = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();

		return LaporanKRS.generateParameter(mahasiswa, semester, hitungUlang.isChecked(),
				semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null, remedial.isChecked(),
				tanggal.getValue(), tanggalDicetak.getValue(), false, false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map generateParameter(Mahasiswa mahasiswa, int semester, boolean hitungUang, Integer sp,
			boolean remedial, Date tanggal, Date tanggalDicetak, boolean uts, boolean uas) throws Exception {

		Integer tahap = mahasiswa.currentTahapan(semester);

		Session session = HibernateUtil.currentSession();
		Staff staffDekan = (Staff) session.createCriteria(Staff.class).add(Restrictions.eq("staff", "prodi"))
				.setMaxResults(1).uniqueResult();

		JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) session
				.createCriteria(JenjangProgramStudi.class).add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))
				.setMaxResults(1).uniqueResult();

		Common.reloadNilai(mahasiswa, semester, false);

		if (hitungUang) {
			mahasiswa.reInitDetailperkuliahan(session);
		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, sp, hitungUang);

		Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();
		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS = Common.getIpkUntukPengambilanKRS(
				mahasiswa, semester, mahasiswa.getTahunangkatan(), fakultas, jurusan, mahasiswa.getProgram(),
				krsMahasiswa.getSemesterPendek());

		PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRSBerikutNya = Common
				.getIpkUntukPengambilanKRS(mahasiswa, semester + 1, mahasiswa.getTahunangkatan(), fakultas, jurusan,
						mahasiswa.getProgram(), krsMahasiswa.getSemesterPendek());

		Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
		parameters.put("max_sks", pembatasanNilaiIPKUntukPengambilanKRS == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("max_sks_next", pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya.getBatasMaksimumIPKYangBolehDiambil());

		parameters.put("max_sks_berikut", pembatasanNilaiIPKUntukPengambilanKRSBerikutNya == null ? null
				: pembatasanNilaiIPKUntukPengambilanKRSBerikutNya.getBatasMaksimumIPKYangBolehDiambil());
		parameters.put("remedial", remedial);
		parameters.put("semester", semester);
		// parameters.put("tahun_ajaran", tahunAkademik);
		parameters.put("pembantu_dekan", staffDekan == null ? "" : staffDekan.getNama());
		parameters.put("mahasiswa", mahasiswa.getId());
		// parameters.put("nip", staffDekan.getNip());
		parameters.put("tanggal", tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal);
		parameters.put("tanggal_dicetak", tanggalDicetak == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDicetak);
		parameters.put("nuptkosenpa", krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNuptk());
		parameters.put("dosenpa", krsMahasiswa == null ? ""
				: krsMahasiswa.getDosenPa() == null ? "......................." : krsMahasiswa.getDosenPa().getNama());
		parameters.put("nipdosenpa",
				krsMahasiswa.getDosenPa() == null ? "......................."
						: (krsMahasiswa.getDosenPa().getCode() == null
								|| krsMahasiswa.getDosenPa().getCode().isEmpty()
										? krsMahasiswa.getDosenPa().getNidn()
										: krsMahasiswa.getDosenPa().getCode()));

		Double ipmhs = krsMahasiswa.getIps();
		Double ipkmhs = krsMahasiswa.getIpk();

		Integer sksmhss = krsMahasiswa.getSksYangDiambil();
		Integer sksmhs = krsMahasiswa.getSksk();

		if (semester > 1) {
			Double iplast = Common.ipTerakhir(mahasiswa, semester);
			parameters.put("ip_sebelumnya", iplast);
		}
		parameters.put("ipk", ipkmhs);
		parameters.put("ips", ipmhs);
		parameters.put("sksk", sksmhs);
		parameters.put("sks", sksmhss);

		parameters.put("ip_kumulatif", ipkmhs);

		parameters.put("ip_semester", ipmhs);

		krsMahasiswa = semester <= 1 ? null
				: Common.singkronkanKrsMahasiswa(mahasiswa, semester - 1, null, sp, hitungUang);

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

		if (Common.bolehKonfigurasi("tampilkan_total_nilai_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai", mahasiswa.hitungNilaiSampaiSemester(semester, null, null, true));
			if (semester > 1) {
				parameters.put("total_nilai_1", mahasiswa.hitungNilaiSampaiSemester(semester - 1, null, null, true));
			}
		}

		if (Common.bolehKonfigurasi("tampilkan_total_mutu_di_krs", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("total_nilai_mutu", mahasiswa.hitungMutuSampaiSemester(semester, null, null, true));
			parameters.put("total_nilai_ip", mahasiswa.hitungNilaiIpSampaiSemester(semester, null, null, true));
		}

		if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
				&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
			parameters.put("kaprodi", jenjangProgramStudi == null ? "(                                          )"
					: jenjangProgramStudi.getNmKaPS());
			parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
		} else {
			Dosen dosen = jurusan.getKaprodi();
			parameters.put("kaprodi", dosen == null ? "(                                          )" : dosen.getNama());
			parameters.put("nip", dosen == null ? "" : dosen.getCode());
		}

		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, mahasiswa.getTahunangkatan(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

		String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

		parameters.put("bar", "2-" + tahunAkademik + "-" + semester + "-" + mahasiswa.getId());

		if (Common.bolehKonfigurasi("khs_dipisahkan_tiap_jenjang", Konfigurasi.TIDAK_AKTIF)) {
			parameters.put("nama_laporan", "Kartu_Hasil_Studi_"
					+ (mahasiswa.getJenjang() == null ? "" : mahasiswa.getJenjang().getId()));

		}

		parameters.put("semester_pendek", sp);

		try {
			FileFotoLain lampiranLain = FileFotoLain.ambil(false, mahasiswa.getId(), LampiranLain.TTD_MAHASISWA,
					LampiranLain.class);
			File file = lampiranLain == null ? null : lampiranLain.ambilFile();
			if (file != null && file.exists()) {
				parameters.put("ttd_mahasiswa", file.getAbsolutePath());
			}
			file = null;
			lampiranLain = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanKRS.java:478");
//			e.printStackTrace();
		}

		Map p1 = CommonReportHelper.generateParameterKrs(mahasiswa, semester, tahap, sp, remedial, false, false);
		parameters.putAll(p1);
		return parameters;
	}

	@SuppressWarnings({ })
	public void onKHS(Event event) throws Exception {
		List<String> warnings = new ArrayList<String>();
		Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
		int semester = (Integer) semesterAbsensiUjian.getSelectedItem().getValue();
		boolean remedial = this.remedial.isChecked();
		Integer semesterPendek = this.semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : null;
		Integer tahapan = null;
		if (mahasiswa != null) {

			tahapan = mahasiswa.currentTahapan(semester);

			List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
					HibernateUtil.currentSession().createCriteria(SyaratUjian.class).add(Restrictions.eq("krs", true))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
					SyaratUjian.class);

			System.out.println("syaratUjians => " + syaratUjians);

			for (SyaratUjian syaratUjian : syaratUjians) {
				SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester, "Cetak KRS", warnings);
			}
		}
		if (!warnings.isEmpty()) {

			String w = "";
			for (String wa : warnings) {
				w += w.isEmpty() ? wa : "\n\n" + wa;
			}

			MyMessageboxConfig.show(w, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		if (Common.bolehKonfigurasi("saat_cetak_krs_harus_telah_disetujui", Konfigurasi.TIDAK_AKTIF)) {

			List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
			int jml = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)) {
						if ((remedial && detailperkuliahan.getPerkuliahan().getMerupakanRemedial())
								|| (!remedial && !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {

							if ((ConstantValues.aktifkanTahapan && tahapan != null && tahapan > 0)
									? detailperkuliahan.getTahap().equals(tahapan)
									: detailperkuliahan.getSemester().equals(semester)) {

								if ((semesterPendek == null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
										|| (semesterPendek != null
												&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
												&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
														.equals(semesterPendek))) {
									jml++;
								}

							}

						}
					}
				}
			}
			detailperkuliahans = null;

			if (jml > 0) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, Bapak/Ibu belum dapat mencetak Kartu Rencana Studi (KRS) karena masih terdapat {V1} perkuliahan yang belum disetujui. Langkah yang dapat dilakukan: (1) hubungi Dosen Pembimbing Akademik untuk memperoleh persetujuan perkuliahan; (2) pastikan seluruh perkuliahan telah disetujui; (3) ulangi proses pencetakan KRS.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, jml);
				return;
			}
		}
		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Cetak_KRS_Mahasiswa",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan KRS", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
