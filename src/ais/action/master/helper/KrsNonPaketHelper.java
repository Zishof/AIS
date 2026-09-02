package ais.action.master.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.SyaratUjianAction;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.Komentar;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PembatasanNilaiIPKUntukPengambilanKRS;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.SyaratUjian;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK untuk layar pengisian KRS non-paket mahasiswa: menampilkan grid mata kuliah yang sudah
 * diambil ({@link Detailperkuliahan}) beserta dosen/jadwal/status persetujuan, ringkasan status
 * persetujuan dan jumlah SKS, informasi jam bentrok (termasuk terhadap kelas paralel), serta tombol
 * aksi (Ambil Matakuliah, Komentar, cetak KRS/kartu UTS/kartu UAS/tagihan pembayaran, Refresh).
 * Setiap baris mata kuliah dapat menampilkan konversi ekivalensi (bila mata kuliah kurikulum lama
 * digantikan versi baru) dan dapat dihapus selama belum disetujui — dengan pengecualian tambahan
 * bila konfigurasi {@code batalkan_persetujuan_harus_memiliki_nilai_nol} aktif dan nilai sudah
 * terisi.
 *
 * <p>
 * Tombol "Ambil Matakuliah" ({@link #display}) melakukan serangkaian validasi berlapis sebelum
 * mengizinkan pengambilan KRS baru: syarat ujian terkait KRS, status pembayaran semester berjalan
 * dan semester sebelumnya (persentase minimum lunas), dosen pembimbing akademik wajib ada, kelas
 * wajib ada, status kemahasiswaan harus aktif, dan blokir mahasiswa (alasan ditampilkan bila ada).
 * Setiap validasi dikontrol oleh konfigurasi terpisah sehingga dapat dinonaktifkan sesuai kebijakan
 * institusi. Lolos validasi, kontrol diserahkan ke
 * {@link AmbilDataPerkuliahanNonPaketHelper#display}.
 * </p>
 *
 * <p>
 * Instance ini mengimplementasikan {@link DataLoader} agar dapat dipanggil ulang oleh komponen anak
 * (mis. dialog pengambilan mata kuliah, dialog komentar) untuk memuat ulang grid setelah perubahan
 * data.
 * </p>
 */
public class KrsNonPaketHelper implements DataLoader {

	private MyGrid grid;
	private MyGrid gridKomentar;
	private MyDiv jamBentrok = new MyDiv();
	private Mahasiswa mahasiswa;
	private Integer semester;
	private Dosen dosenPembimbingAkademik;

	private List<Long> detailperkuliahans;

	private MyButtonConfig buttonPerkuliahan;
	private Label statusPersetujuan;
	// private Html keterangan;
	private Html keteranganParent;
	private Label jumlahKRS;
	private Label jumlahMaxSks;
	private PembatasanNilaiIPKUntukPengambilanKRS pembatasanNilaiIPKUntukPengambilanKRS;

	private Konfigurasi konfigurasi;
	private String tahunAjaran;
	private Integer semesterPendek = null;
	private boolean sudahBayar = false;

	private Integer tahapan;

	/** @param semesterPendek status semester pendek (SP) yang dilayani helper ini, atau {@code null} untuk KRS reguler */
	public KrsNonPaketHelper(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	/** Row renderer grid KRS: kode/nama/SKS matakuliah (dengan info konversi ekivalensi bila berbeda), dosen, jadwal, semester (menandai "Mengulang"/"Menabung" bila berbeda dari jadwal aslinya), kelas, status persetujuan, dan tombol hapus (hanya bila belum disetujui). */
	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private boolean refresh;

		public DetailMahasiswaRenderer(boolean refresh) {
			this.refresh = refresh;
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, data.toString());
			Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
					? detailperkuliahan.getMatakuliahKonversi()
					: detailperkuliahan.getPerkuliahan().getMatakuliah();

			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
					mahasiswa == null ? null : mahasiswa.getNim(), refresh);
			matakuliah = matakuliahs[0];
			Matakuliah matakuliahAsli = matakuliahs[1];

			if (matakuliah == null) {
				row.setVisible(false);
				return;
			}

			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode()
					: (matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")")).setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama()
					: (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")")).setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "")
					: (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")).setParent(row);

			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, true);

			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(row, perkuliahan, detailperkuliahan);

			try {
				if (detailperkuliahan.getSemester().equals(perkuliahan.getSemester())) {
					new Label(detailperkuliahan.getSemester() + "").setParent(row);
				} else {
					new MyLabelKecil((detailperkuliahan.getSemester() + " / " + perkuliahan.getSemester()
							+ (detailperkuliahan.getSemester() > perkuliahan.getSemester() ? " (Mengulang)"
									: " (Menabung)")))
							.setParent(row);
				}
			} catch (Exception e) {
				new Label(detailperkuliahan.getSemester() + "").setParent(row);
			}

			new Label(detailperkuliahan.getPerkuliahan() == null ? "" : detailperkuliahan.getPerkuliahan().getKelas())
					.setParent(row);

			final Label label;
			(label = new Label(detailperkuliahan.getPersetujuan() == null
					|| detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI) ? "Belum" : "Ya"))
					.setParent(row);
			label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");

			if ((detailperkuliahan.getPersetujuan() == null
					|| detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI))) {
				Hbox toolbar = new Hbox();
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show(
								"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diperhatikan bahwa data yang telah dihapus tidak dapat dikembalikan. Silakan pilih OK untuk melanjutkan penghapusan atau Batal untuk membatalkan.",
								"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();

												if (Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol")) {
													if (detailperkuliahan.getPersetujuan() != null
															&& detailperkuliahan.getPersetujuan()
																	.equals(Detailperkuliahan.DISETUJUI)
															&& detailperkuliahan.getTotalNilai() > 1.0) {
														MyMessageboxConfig.show(
								"Mohon maaf, mata kuliah ini tidak dapat dihapus karena nilainya tidak nol (telah memiliki nilai). Langkah yang dapat dilakukan: (1) pastikan mata kuliah yang akan dihapus belum memiliki nilai; (2) apabila penghapusan tetap diperlukan, hubungi bagian Akademik atau Admin untuk dilakukan penyesuaian.",
								"Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
														return;
													}
												}

												List<Komentar> komentars = session
														.createCriteria(Komentar.class).add(Restrictions
																.eq("detailperkuliahan", detailperkuliahan.getId()))
														.list();

												for (Komentar komentar : komentars) {
													Common.refreshDelete((komentar));
												}

												Common.refreshDelete((detailperkuliahan));
												loadData(true);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
								"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain di dalam sistem. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang terkait; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala masih berlanjut, hubungi Admin untuk bantuan lebih lanjut.",
								e.getMessage()));
											}

										}

									}
								});

					}

				});
				button.setParent(toolbar);
				toolbar.setParent(row);
			} else {
				new Label("").setParent(row);
			}

		}
	}

	/**
	 * Memuat ulang daftar {@link Detailperkuliahan} KRS mahasiswa, memperbarui ringkasan status
	 * ({@link #loadStatus()}) dan panel informasi jam bentrok (termasuk terhadap kelas paralel).
	 *
	 * @param value bila berupa {@link Boolean} {@code true}, memaksa refresh cache riwayat/kuota
	 *              (diteruskan ke {@code Common.getDetailperkuliahans})
	 */
	public void loadData(Object value) {
		boolean refresh = (value != null && value instanceof Boolean) ? (Boolean) value : false;
		detailperkuliahans = Common.getDetailperkuliahans(mahasiswa, semester, null, semesterPendek, false, false,
				refresh);

		ListModel strset = new SimpleListModel(detailperkuliahans);
		grid.setRowRenderer(new DetailMahasiswaRenderer(refresh));
		grid.setModelCheckMobile(strset);

		loadStatus();
		Common.clear(jamBentrok);
		jamBentrok.appendChild(new MyCaptionStyled("Informasi Jam Bentrok"));
		List<Perkuliahan> jadwalPerkuliahanParalels = new ArrayList<Perkuliahan>();
		List<Detailperkuliahan> detailperkuliahansbaru = new ArrayList<Detailperkuliahan>();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
					detailperkuliahanid.toString());
			if (d != null && d.getPerkuliahan() != null) {
				detailperkuliahansbaru.add(d);
				List<Perkuliahan> jadwalparalels = d.getPerkuliahan().ambilParalelPerkuliahan();
				jadwalPerkuliahanParalels.addAll(jadwalparalels);
			}
		}
		jamBentrok.appendChild(Common.generateInformasiJamBentrok(detailperkuliahansbaru));
		jamBentrok.appendChild(
				Common.generateInformasiJamBentrokParalel(detailperkuliahansbaru, jadwalPerkuliahanParalels));
		jamBentrok.appendChild(Common.generateInformasiJamBentrokParalelParalel(jadwalPerkuliahanParalels));

	}

	/**
	 * Membangun seluruh UI layar KRS non-paket (info dosen PA, status persetujuan, jumlah SKS, batas
	 * SKS maksimum, toolbar aksi, grid mata kuliah, grid komentar) untuk kombinasi mahasiswa/semester/
	 * tahapan yang diberikan, lalu memuat data awal.
	 *
	 * @param editable       tidak dipakai langsung di badan method (disediakan untuk kompatibilitas
	 *                       pemanggil)
	 * @param mahasiswa      mahasiswa yang KRS-nya ditampilkan
	 * @param tahunAjaran    tahun ajaran KRS
	 * @param semester       nomor semester KRS
	 * @param tahapan        tahapan KRS (bila fitur tahapan aktif), boleh {@code null}/0
	 * @param component      container ZK yang akan diisi
	 * @param window         window pemanggil
	 * @param ket            komponen {@link Html} tujuan render keterangan status pengambilan KRS
	 * @param komentarshtml  tidak dipakai langsung di badan method (disediakan untuk kompatibilitas
	 *                       pemanggil)
	 * @param keDatabase     diteruskan ke {@code Common.singkronkanKrsMahasiswa} untuk menentukan
	 *                       apakah sinkronisasi KRS mahasiswa disimpan ke database
	 */
	public void display(final Boolean editable, final Mahasiswa mahasiswa, final String tahunAjaran,
			final Integer semester, final Integer tahapan, final Component component, final MyWindow window,
			final Html ket, final Html komentarshtml, boolean keDatabase) {

		this.sudahBayar = Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false, false);

		this.mahasiswa = mahasiswa;
		this.semester = semester;

		this.tahapan = tahapan;

		this.keteranganParent = ket;
		this.tahunAjaran = tahunAjaran;
		if (Common.bolehKonfigurasi("input_krs_harus_berdasarkan_kalender_akademik")) {
			this.konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(HibernateUtil.currentSession(),
					semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP, tahunAjaran,
					semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, mahasiswa.getSemesterMulai(),
					mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(), mahasiswa.getProgram());
		} else {
			this.konfigurasi = Common.getKonfigurasi(semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
					tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		}

		Common.clear(component);

		final KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
				keDatabase, false);

		dosenPembimbingAkademik = krsMahasiswa.getDosenPa();

		Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();
		pembatasanNilaiIPKUntukPengambilanKRS = Common.getIpkUntukPengambilanKRS(mahasiswa, semester,
				mahasiswa.getTahunangkatan(), fakultas, jurusan, mahasiswa.getProgram(), semesterPendek);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");

		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(component);

		Vbox vbox = new Vbox();
		vbox.setVisible(semester > 0);
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(groupbox);
		Label dosenPembimbing = new Label(
				dosenPembimbingAkademik == null ? "Anda belum memiliki dosen pembimbing akademik"
						: "Dosen Pembimbing Akademik : " + dosenPembimbingAkademik.getNama());
		dosenPembimbing.setStyle(
				"font-size:11px;font-weight:bold;color:" + (dosenPembimbingAkademik == null ? "red" : "blue") + ";");
		dosenPembimbing.setParent(vbox);

		statusPersetujuan = new Label(ais.common.Common.getBahasaConfig("Status: Belum disetujui"));
		statusPersetujuan.setStyle("font-size:11px;font-weight:bold;color:red;");
		statusPersetujuan.setParent(vbox);

		jumlahKRS = new Label(ais.common.Common.getBahasaConfig("Jumlah SKS : 0"));
		jumlahKRS.setStyle("font-size:11px;font-weight:bold;color:blue;");
		jumlahKRS.setParent(vbox);

		if (this.pembatasanNilaiIPKUntukPengambilanKRS != null) {
			jumlahMaxSks = new Label("Jumlah maksimal SKS yang boleh diambil : "
					+ pembatasanNilaiIPKUntukPengambilanKRS.getBatasMaksimumIPKYangBolehDiambil() + " SKS");
			jumlahMaxSks.setStyle("font-size:11px;font-weight:bold;color:blue;");
			jumlahMaxSks.setParent(vbox);
		} else {
			jumlahMaxSks = new Label("Jumlah maksimal SKS yang boleh diambil : "
					+ PembatasanNilaiIPKUntukPengambilanKRS.getDefaultPembatasanNilaiIpUntukAmbilKRS() + " SKS");
			jumlahMaxSks.setStyle("font-size:11px;font-weight:bold;color:blue;");
			jumlahMaxSks.setParent(vbox);
		}

		String kelas = krsMahasiswa.getKelas();
		Label k;
		vbox.appendChild(k = new Label("Kelas : " + (kelas == null ? "" : kelas)));
		k.setStyle("font-size:11px;font-weight:bold;color:blue;");

		Label waktuisiKrs = new Label(this.konfigurasi != null && this.konfigurasi.getNilai() != null
				&& this.konfigurasi.getNilai().equals(Konfigurasi.AKTIF)
						? "Saat ini anda bisa mengambil dan mengubah " + Common.getBahasa("label_krs")
						: "Saat ini anda tidak bisa mengambil dan mengubah KRS. Waktu pengambilan KRS sudah selesai atau belum berlangsung");
		waktuisiKrs.setStyle("font-size:11px;font-weight:bold;color:blue;");
		waktuisiKrs.setParent(vbox);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.setVisible(semester > 0);

		buttonPerkuliahan = new MyButtonConfig("Ambil Matakuliah", "/img/svg/edit-box-line.svg");
		buttonPerkuliahan.setVisible(this.konfigurasi != null && this.konfigurasi.getNilai() != null
				&& this.konfigurasi.getNilai().equals(Konfigurasi.AKTIF));
		if (ConstantValues.aktifkanTahapan && tahapan != null && !tahapan.equals(0)) {
			Integer t = mahasiswa.currentTahapan();
			if (t != null && !t.equals(0) && tahapan.equals(t)) {
				buttonPerkuliahan.setDisabled(false);
			} else {
				buttonPerkuliahan.setDisabled(true);
			}
		}
		buttonPerkuliahan.addEventListener("onClick", new EventListener() {

			private AmbilDataPerkuliahanNonPaketHelper ambilDataPerkuliahanHelper = new AmbilDataPerkuliahanNonPaketHelper(
					semesterPendek);

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<String> warnings = new ArrayList<String>();
				if (mahasiswa != null) {
					List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
							HibernateUtil.currentSession().createCriteria(SyaratUjian.class)
									.add(Restrictions.eq("krs", true)).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
							SyaratUjian.class);

					System.out.println("syaratUjians => " + syaratUjians);

					for (SyaratUjian syaratUjian : syaratUjians) {
						SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester, "Ambil KRS",
								warnings);
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

				Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs",
						Konfigurasi.AKTIF);

				if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
					if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false, false)) {
						if (semester != null && semester.intValue() >= 1) {
							MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu belum menyelesaikan pembayaran biaya perkuliahan pada semester {V1}. Langkah yang dapat dilakukan: (1) lakukan pembayaran biaya perkuliahan terlebih dahulu; (2) setelah pembayaran tercatat, ambil kembali KRS ini; (3) apabila telah membayar namun status belum berubah, hubungi bagian Keuangan untuk informasi lebih lanjut.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, semester);
							return;
						}
					}

					if (!UtsDanUasCheckerHelper.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahapan)) {
						return;
					}
				}

				Konfigurasi konfigurasiDosenPembimbingAkademik = Common
						.getKonfigurasi("dosen_pa_harus_ada_sebelum_isi_krs", Konfigurasi.AKTIF);

				if (dosenPembimbingAkademik == null
						&& konfigurasiDosenPembimbingAkademik.getNilai().equals(Konfigurasi.AKTIF)) {
					MyMessageboxConfig.show(
								"Mohon maaf, Bapak/Ibu belum memiliki Dosen Pembimbing Akademik sehingga belum dapat mengambil KRS. Langkah yang dapat dilakukan: (1) hubungi bagian Akademik atau Admin Fakultas/Prodi untuk mendaftarkan Dosen Pembimbing Akademik Anda; (2) setelah terdaftar, ambil kembali KRS ini.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (Common.bolehKonfigurasi("kelas_harus_ada_sebelum_isi_krs", Konfigurasi.TIDAK_AKTIF)) {
					String kelas = krsMahasiswa.getKelas();

					if (kelas == null || kelas.trim().isEmpty()) {
						MyMessageboxConfig.show(
								"Mohon maaf, Bapak/Ibu belum memiliki kelas sehingga belum dapat mengambil KRS. Langkah yang dapat dilakukan: (1) hubungi bagian Akademik untuk penetapan kelas Anda; (2) setelah kelas ditetapkan, ambil kembali KRS ini.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}
				}

				konfigurasi = Common.getKonfigurasi("status_mahasiswa_harus_aktif_sebelum_isi_krs", Konfigurasi.AKTIF);

				if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
					HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa);

					StatusMahasiswa statusMahasiswa = historyStatusMahasiswa.ambilStatusMahasiswa(semester);
					// Tanda seru di bawah sempat hilang, sehingga gerbang ini terbalik:
					// mahasiswa AKTIF ditolak dan yang tidak aktif diloloskan. Arah yang
					// benar ditegakkan tiga layar KRS bersaudara -- KrsPaketHelper,
					// KrsKurikulumHelper, dan KrsHelper -- yang semuanya memakai negasi,
					// dan oleh bunyi pesan di bawah yang meminta pembacanya memastikan
					// statusnya sudah Aktif. Diperbaiki 2 September 2026.
					if (!statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId())) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, status kemahasiswaan Anda saat ini adalah \"{V1}\" sehingga Anda belum dapat mengambil KRS. Langkah yang dapat dilakukan: (1) pastikan status Anda telah Aktif pada semester berjalan; (2) hubungi Admin atau bagian Akademik untuk memperbarui status Anda; (3) setelah status Aktif, ambil kembali KRS ini.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								(statusMahasiswa.getNama()));
						return;
					}
				}

				if (semesterPendek == null) {
					if (!Common.checkStatusPembayaranMahasiswaSebelumnya(semester, tahapan, mahasiswa)) {
						Double harusLunas = 90.0;
						try {
							harusLunas = Double.parseDouble(Common.getKonfigurasi(
									"batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs", "90")
									.getNilai().trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsNonPaketHelper.java:455");

						}
						MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu belum melunasi {V1}% biaya perkuliahan pada{V2}. Langkah yang dapat dilakukan: (1) lakukan pelunasan biaya perkuliahan sesuai ketentuan; (2) setelah pembayaran tercatat, ambil kembali KRS ini; (3) apabila telah membayar namun status belum berubah, hubungi bagian Keuangan untuk informasi lebih lanjut.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								harusLunas, ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan != null && tahapan > 0) ? " tahap " + (tahapan - 1) : " semester " + (semester - 1)));
						return;
					}
				}

				Session session = HibernateUtil.currentSession();

				List<String> alasans = session.createCriteria(BlokirMahasiswa.class)
						.add(Restrictions.isNotNull("keterangan")).add(Restrictions.ne("keterangan", ""))
						.setProjection(Projections.property("keterangan")).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("krs", true)).list();
				if (!alasans.isEmpty()) {

					String alas = "";
					for (String s : alasans) {
						alas += alas.isEmpty() ? s : "\n\n" + s;
					}

					try {
						MyMessageboxConfig.show(alas, "Informasi KRS", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsNonPaketHelper.java:482");
					}

					return;
				}

				ambilDataPerkuliahanHelper.display(mahasiswa, tahunAjaran, semester, tahapan, KrsNonPaketHelper.this);
			}

		});
		buttonPerkuliahan.setParent(toolbar);

		MyButtonConfig button = new MyButtonConfig("Komentar", "/img/m3.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				KomentarHelper komentarHelper = new KomentarHelper(mahasiswa, tahunAjaran, semester, tahapan,
						semesterPendek, false, dosenPembimbingAkademik);

				komentarHelper.display(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadDataKomentar();
					}
				});

			}

		});
		button.setParent(toolbar);

		button = new MyButtonConfig("Cetak " + Common.getBahasa("label_krs"), "/img/print.png");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakKRS(mahasiswa, semester, tahapan, semesterPendek, false);
			}

		});
		button.setParent(toolbar);

		button = new MyButtonConfig("Cetak Kartu UTS", "/img/print.png");
		button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uts"));
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakUTS(mahasiswa, semester, tahapan, tahunAjaran, semesterPendek, false, false);
			}

		});
		button.setParent(toolbar);

		button = new MyButtonConfig("Cetak Kartu UAS", "/img/print.png");
		button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uas"));
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakUAS(mahasiswa, semester, tahapan, tahunAjaran, semesterPendek, false, false);

			}

		});
		button.setParent(toolbar);

		button = new MyButtonConfig("Cetak Tagihan Pembayaran", "/img/print.png");
		button.setVisible(!this.sudahBayar);
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();

				JenjangProgramStudi jenjangProgramStudi = (JenjangProgramStudi) session
						.createCriteria(JenjangProgramStudi.class)
						.add(Restrictions.eq("jurusan", mahasiswa.getJurusan())).setMaxResults(1).uniqueResult();

				Map<String, Serializable> parameters = ais.common.HashMapGenerator.getRandStringSerializable();
				parameters.put("semester", semester);
				parameters.put("tahun_ajaran", tahunAjaran);
				parameters.put("program", mahasiswa.getProgram());
				parameters.put("mahasiswa", mahasiswa.getId());
				parameters.put("nama_mahasiswa", mahasiswa.getNama());
				parameters.put("nim", mahasiswa.getNim());
				parameters.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				parameters.put("nama_jurusan", mahasiswa.getJurusan().getNama());
				parameters.put("tanggal", ais.ui.util.WaktuUtil.getDate());

				if (jenjangProgramStudi != null && jenjangProgramStudi.getNmKaPS() != null
						&& !jenjangProgramStudi.getNmKaPS().trim().equals("")) {
					parameters.put("kaprodi",
							jenjangProgramStudi == null ? "(                                          )"
									: jenjangProgramStudi.getNmKaPS());
					parameters.put("nip", jenjangProgramStudi == null ? "" : jenjangProgramStudi.getNidnKaPS());
				} else {
					Jurusan jurusan = KrsNonPaketHelper.this.mahasiswa.getJurusan();
					Dosen dosen = jurusan.getKaprodi();
					parameters.put("kaprodi",
							dosen == null ? "(                                          )" : dosen.getNama());
					parameters.put("nip", dosen == null ? "" : dosen.getCode());
				}

				parameters.put("semester_pendek", semesterPendek);
				parameters.put("namamahasiswa", mahasiswa.getNama());
				parameters.put("namafakultas", mahasiswa.getJurusan().getFakultas().getNama());
				parameters.put("dosenpa",
						dosenPembimbingAkademik == null ? ""
								: dosenPembimbingAkademik == null ? "......................."
										: dosenPembimbingAkademik.getNama());
				parameters.put("nuptkosenpa",
						krsMahasiswa.getDosenPa() == null ? ""
								: krsMahasiswa.getDosenPa().getNuptk());
				parameters.put("nipdosenpa",
						krsMahasiswa.getDosenPa() == null ? "......................."
								: (krsMahasiswa.getDosenPa().getCode().isEmpty() ? krsMahasiswa.getDosenPa().getNidn()
										: krsMahasiswa.getDosenPa().getCode()));

				PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

				JenisKegiatan jenisKegiatan = pembayaranUtil
						.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
				Collection<DetailBiaya> detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
						jenisKegiatan, null, false);

				List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					Double nilai = detailBiaya.hitungTotal();
					Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
					map.put("kode", detailBiaya.getItemBiaya().getKode());
					map.put("item_biaya", detailBiaya.getKeterangan());
					map.put("biaya", nilai);
					maps.add(map);
				}

				Report.generatePDFReport("pdf", parameters, "Tagihan_Pembayaran_KRS_Non_Paket",
						ais.ui.util.WaktuUtil.getDate(), maps);
			}
		});
		button.setParent(toolbar);

		button = new MyButtonConfig("Refresh", "/img/Configure.png");

		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.setParent(groupbox);
//		grid.setOddRowSclass("non-odd");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		// column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		// column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		// column.setWidth("25%");
		column.setVisible(this.sudahBayar);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hari/Waktu/Ruang");
		column.setWidth("30%");
		column.setVisible(this.sudahBayar);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		// column.setWidth("5%");
		column.setVisible(this.sudahBayar);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");
		// column.setWidth("5%");
		column.setVisible(this.sudahBayar);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		// column.setWidth("10%");
		column.setVisible(this.sudahBayar);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		gridKomentar = new MyGrid();
		gridKomentar.setMold("paging");
		gridKomentar.setPageSize(20);
		gridKomentar.setParent(groupbox);

		Columns columns2 = new Columns();
		columns2.setMenupopup("auto");
		columns2.setParent(gridKomentar);

		MyColumnConfig column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Komentar");
		column2.setWidth("50%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Oleh");
		column2.setWidth("20%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Tanggal");
		column2.setWidth("20%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("");
		column2.setWidth("10%");

		loadDataKomentar();

		jamBentrok.setParent(groupbox);
		jamBentrok.setVisible(this.sudahBayar);

		loadData(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAjaran,
						semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)) {
					buttonPerkuliahan.setVisible(true);
				}
			}
		});

	}

	private void loadStatus() {
		boolean adaPersetujuan = false;
		boolean adaBelumPersetujuan = false;
		Integer jmlKrs = 0;
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				adaPersetujuan |= detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI);
				adaBelumPersetujuan |= detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI);
				Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
						? detailperkuliahan.getMatakuliahKonversi()
						: detailperkuliahan.getPerkuliahan().getMatakuliah();
				matakuliah = Common.getMatakuliahApakahEkivalen(matakuliah,
						mahasiswa == null ? null : mahasiswa.getNim(), false)[0];
				if (matakuliah == null) {
					continue;
				}
				jmlKrs += matakuliah.getSks();
			}
		}

		jumlahKRS.setValue("Jumlah SKS : " + jmlKrs + " SKS");

		if (adaPersetujuan && adaBelumPersetujuan) {

			statusPersetujuan.setValue("Status: Sebagian sudah disetujui");
			statusPersetujuan.setStyle("font-size:11px;font-weight:bold;color:green;");
		} else if (adaPersetujuan) {

			statusPersetujuan.setValue("Status: Sudah disetujui semua");
			statusPersetujuan.setStyle("font-size:11px;font-weight:bold;color:blue;");
		} else if (adaBelumPersetujuan) {

			statusPersetujuan.setValue("Status: Belum disetujui semua");
			statusPersetujuan.setStyle("font-size:11px;font-weight:bold;color:red;");
		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek);
		keteranganParent.setContent(mahasiswa.rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(),
				krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa, false));
	}

	/** Memuat ulang grid komentar KRS mahasiswa untuk kombinasi semester/tahapan/tahun ajaran saat ini. */
	public void loadDataKomentar() {
		List<Komentar> komentars = Common.loadKomentarData(mahasiswa, semester, tahapan, tahunAjaran, semesterPendek);
		ListModel strset = new SimpleListModel(komentars);

		gridKomentar.setRowRenderer(new Common.KomentarRenderer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataKomentar();
			}
		}));
		gridKomentar.setModelCheckMobile(strset);

		gridKomentar.renderAll();
		gridKomentar.setOddRowSclass("non-odd");

	}

	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

}
