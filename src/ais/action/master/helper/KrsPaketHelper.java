package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.SyaratUjianAction;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Komentar;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.SyaratUjian;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK untuk halaman KRS (Kartu Rencana Studi) berbasis "paket perkuliahan" — mode
 * pengisian KRS di mana mahasiswa mengambil satu paket matakuliah sekaligus (via
 * {@code AmbilDataPaketPerkuliahanHelper}), bukan memilih matakuliah satu per satu. Menampilkan
 * ringkasan status (dosen pembimbing akademik, IPS/IPK, jumlah SKS diambil/kumulatif/maksimal
 * boleh diambil, tahun akademik, kelas, semester, status buka-tutup periode KRS), grid daftar
 * {@link Detailperkuliahan} yang sudah diambil (dengan resolusi ekivalensi matakuliah otomatis via
 * {@link Common#getMatakuliahApakahEkivalen}), informasi jam bentrok, dan grid komentar dosen PA.
 *
 * <p>
 * Tombol "Ambil Paket Perkuliahan" menjalankan rangkaian validasi berlapis sebelum membuka helper
 * pengambilan paket: syarat ujian yang menyaratkan KRS, keharusan sudah punya dosen PA, keharusan
 * status pembayaran semester berjalan/sebelumnya memenuhi batas minimal, status kemahasiswaan
 * aktif, kelas sudah ditetapkan, dan tidak ada {@link BlokirMahasiswa} aktif untuk KRS — masing-
 * masing dapat diaktifkan/dinonaktifkan lewat {@link Konfigurasi} terkait. Toolbar lain menyediakan
 * komentar, cetak KRS/Kartu UTS/Kartu UAS/Ket. Aktif, dan Refresh.
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} ({@link #loadData(Object)}) sehingga dapat menjadi
 * callback penyegar bagi {@code AmbilDataPaketPerkuliahanHelper} setelah paket diambil.
 * </p>
 */
public class KrsPaketHelper implements DataLoader {

	/** Grid daftar {@link Detailperkuliahan} yang sudah diambil mahasiswa; dirender ulang oleh {@link #loadData(Object)}. */
	private MyGrid grid;
	/** Grid daftar {@link Komentar} dosen PA terkait KRS ini; dirender ulang oleh {@link #loadDataKomentar()}. */
	private MyGrid gridKomentar;
	/** Panel informasi jam bentrok (termasuk bentrok jadwal paralel), dibangun ulang setiap {@link #loadData(Object)}. */
	private MyDiv jamBentrok = new MyDiv();
	/** Mahasiswa pemilik KRS yang sedang ditampilkan/dikelola, diset di awal {@link #display}. */
	private Mahasiswa mahasiswa;
	/** Semester KRS yang sedang ditampilkan, diset di awal {@link #display}. */
	private Integer semester;
	/** Dosen Pembimbing Akademik mahasiswa untuk KRS ini, diambil dari {@code KrsMahasiswa#getDosenPa()} pada {@link #display}. */
	private Dosen dosenPembimbingAkademik;

	/** Daftar id {@link Detailperkuliahan} yang sudah diambil mahasiswa untuk KRS ini, dimuat oleh {@link #loadData(Object)}. */
	private List<Long> detailperkuliahans;

	/** Tombol "Ambil Paket Perkuliahan"; visibilitas/status enable-nya mengikuti periode KRS dan tahapan pembayaran aktif. */
	private MyButtonConfig buttonPerkuliahan;
	/** Label ringkasan status persetujuan (belum/sebagian/semua disetujui), diperbarui oleh {@link #loadStatus()}. */
	private Label statusPersetujuan;
	// private Html keterangan;
	/** Komponen {@link Html} tujuan penulisan keterangan/analisis KRS (lihat {@code KrsMahasiswaAnalisisPopupHelper}), diset di {@link #display}. */
	private Html keteranganParent;
	/** Label total SKS yang sudah diambil pada KRS ini, diperbarui oleh {@link #loadStatus()}. */
	private Label jumlahKRS;
	/** Label batas maksimal SKS yang boleh diambil (dihitung dari IPK via {@code Common#getMinDanMaxIPK}), diisi sekali di {@link #display}. */
	private Label jumlahMaxSks;

	/** {@link Konfigurasi} status buka/tutup periode KRS (KRS/KRS_SP) untuk tahun ajaran+semester berjalan; menentukan visibilitas {@link #buttonPerkuliahan}. */
	private Konfigurasi konfigurasi;
	/** Tahun ajaran KRS yang sedang ditampilkan, diset di awal {@link #display}. */
	private String tahunAjaran;
	/** Penanda semester pendek (bukan {@code null}) atau KRS reguler ({@code null}); diset lewat konstruktor atau {@link #setSemesterPendek(Integer)}. */
	private Integer semesterPendek = null;

	/** Tahapan pembayaran/KRS aktif (bila fitur tahapan diaktifkan); {@code -1} berarti mode ringkasan tanpa grid/toolbar. */
	private Integer tahapan;

	/**
	 * @param semesterPendek status semester pendek yang berlaku untuk KRS ini ({@code null} untuk
	 *                       KRS semester reguler)
	 */
	public KrsPaketHelper(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	/**
	 * Perender baris grid: menampilkan kode/nama/SKS matakuliah (dengan info matakuliah asli bila
	 * berbeda karena ekivalensi), dosen dan jadwal hari/jam/ruangan perkuliahan (didelegasikan ke
	 * {@link ais.action.master.helper.PerkuliahanUIHelper}), semester pengambilan (dengan penanda
	 * "Mengulang"/"Menabung" bila berbeda dari semester perkuliahan), kelas, status persetujuan, dan
	 * (bila belum disetujui) tombol hapus dengan pengecekan opsional "tidak boleh hapus bila nilai
	 * sudah tidak nol" dan penghapusan komentar terkait.
	 */
	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/** Bila {@code true}, cache resolusi ekivalensi matakuliah ({@code Common#getMatakuliahApakahEkivalen}) dipaksa dibangun ulang saat render. */
		private boolean refresh;

		/** @param refresh bila {@code true}, cache resolusi ekivalensi matakuliah dipaksa dibangun ulang */
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

												session.delete(detailperkuliahan);
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
	 * Memuat ulang grid matakuliah KRS dari {@link Common#getDetailperkuliahans}, memperbarui
	 * status persetujuan/jumlah SKS ({@link #loadStatus()}), dan membangun ulang informasi jam
	 * bentrok (termasuk bentrok dengan jadwal paralel). Bila konfigurasi
	 * {@code saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok} aktif (dan bukan tahap -1),
	 * validasi jam bentrok juga dijalankan.
	 *
	 * @param value bila berupa {@link Boolean} {@code true}, paksa segarkan cache resolusi
	 *              ekivalensi matakuliah; selain itu dianggap {@code false}
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

		if (tahapan != null && tahapan.equals(-1)) {

		} else {
			if (Common.bolehKonfigurasi("saat_pengambilan_krs_tidak_diperbolehkan_ada_jam_bentrok", Konfigurasi.TIDAK_AKTIF)) {
				try {
					Common.checkJamBentrok(detailperkuliahansbaru);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

	/**
	 * Membangun dan menampilkan seluruh UI KRS paket ke dalam {@code component}: ringkasan status
	 * mahasiswa (dosen PA, IPS/IPK, SKS, tahun akademik, kelas, semester, status buka-tutup KRS),
	 * toolbar aksi (Ambil Paket Perkuliahan — dengan rangkaian validasi berlapis, lihat javadoc
	 * kelas; Komentar; Cetak KRS/UTS/UAS/Ket. Aktif; Refresh), grid matakuliah, panel jam bentrok,
	 * dan grid komentar. Konfigurasi periode KRS dibaca berbasis kalender akademik atau langsung
	 * dari {@link Konfigurasi} tergantung {@code input_krs_harus_berdasarkan_kalender_akademik}.
	 *
	 * @param editable      tidak dipakai langsung dalam badan method; diteruskan untuk kompatibilitas signature
	 * @param mahasiswa     mahasiswa pemilik KRS
	 * @param tahunAjaran   tahun ajaran KRS
	 * @param semester      semester KRS
	 * @param tahapan       tahapan pembayaran/KRS (bila fitur tahapan aktif); {@code -1} menyembunyikan grid/toolbar (mode ringkasan saja)
	 * @param component     komponen ZK tujuan tampilan (dibersihkan lebih dulu)
	 * @param window        tidak dipakai langsung dalam badan method; diteruskan untuk kompatibilitas signature
	 * @param ket            komponen {@link Html} tujuan penulisan keterangan status KRS oleh {@link #loadStatus()}
	 * @param komentarshtml tidak dipakai langsung dalam badan method; diteruskan untuk kompatibilitas signature
	 * @param keDatabase    diteruskan ke {@link Common#singkronkanKrsMahasiswa} untuk menentukan apakah sinkronisasi KRS ditulis ke database
	 */
	public void display(final Boolean editable, final Mahasiswa mahasiswa, final String tahunAjaran,
			final Integer semester, final Integer tahapan, final Component component, final MyWindow window,
			final Html ket, final Html komentarshtml, boolean keDatabase) {
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

		Double[] batas = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
		Integer maxsks = batas[0].intValue();

		MyGroupboxStyled groupbox = new MyGroupboxStyled();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("98%");
		groupbox.setParent(component);
		groupbox.appendChild(new Caption(mahasiswa.getNim() + " " + mahasiswa.getNama() + " smt " + semester));

		Row rowUtama = Common.tampilanScroll1(groupbox);

		rowUtama.getGrid().setVisible(semester > 0);
		if (tahapan != null && tahapan.equals(-1)) {
			rowUtama.getGrid().setVisible(false);
		}
		rowUtama.getGrid().setHeight("100%");
		rowUtama.getGrid().setWidth("100%");

		rowUtama.appendChild(new MyLabelConfig("Dosen Pembimbing Akademik"));

		Label dosenPembimbing = new Label(dosenPembimbingAkademik == null ? "Belum memiliki dosen pembimbing akademik"
				: dosenPembimbingAkademik.getNama());
		dosenPembimbing.setParent(rowUtama);

		Row rowUtama1;
		if (Common.isMobile()) {
			rowUtama1 = new Row();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		} else {
			rowUtama1 = rowUtama;
		}

		rowUtama1.appendChild(new MyLabelConfig("IPS"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getIps())));

		rowUtama1 = new Row();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("Status Persetujuan"));
		statusPersetujuan = new Label(ais.common.Common.getBahasaConfig("Belum disetujui"));
		statusPersetujuan.setParent(rowUtama1);

		if (Common.isMobile()) {
			rowUtama1 = new Row();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("IPK"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getIpk())));

		rowUtama1 = new Row();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("SKS yang telah diambil"));

		jumlahKRS = new Label(ais.common.Common.getBahasaConfig("0 SKS"));
		jumlahKRS.setParent(rowUtama1);

		if (Common.isMobile()) {
			rowUtama1 = new Row();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("SKS Kumulatif"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getSksk())));

		rowUtama1 = new Row();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());
		rowUtama1.appendChild(new MyLabelConfig("SKS yang boleh diambil"));

		jumlahMaxSks = new Label(Common.numberFormat.get().format(maxsks) + " SKS");
		jumlahMaxSks.setParent(rowUtama1);

		if (Common.isMobile()) {
			rowUtama1 = new Row();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("Tahun Akademik"));
		rowUtama1.appendChild(new Label(krsMahasiswa.getTahunAkademik()));

		rowUtama1 = new Row();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());
		rowUtama1.appendChild(new MyLabelConfig("Kelas"));

		Label k;
		k = new Label((krsMahasiswa.getKelas() == null ? "" : krsMahasiswa.getKelas()));
		rowUtama1.appendChild(k);

		if (Common.isMobile()) {
			rowUtama1 = new Row();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("Semester"));
		rowUtama1.appendChild(new Label(krsMahasiswa.getSemester() + " / "
				+ (krsMahasiswa.getSemesterPendek() == null
						? (krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)
						: Common.getBahasaConfig("Semester Pendek"))));

		rowUtama1 = new Row();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());
		rowUtama1.appendChild(new MyLabelConfig("Status pengambilan KRS"));

		Label waktuisiKrs = new Label(this.konfigurasi != null && this.konfigurasi.getNilai() != null
				&& this.konfigurasi.getNilai().equals(Konfigurasi.AKTIF)
						? "Saat ini anda bisa memperbaiki " + Common.getBahasa("label_krs")
						: this.konfigurasi != null && this.konfigurasi.getNilai() != null
								&& this.konfigurasi.getNilai().equals(Konfigurasi.AKTIF)
										? "Saat ini anda bisa mengambil dan mengubah " + Common.getBahasa("label_krs")
										: "Saat ini anda tidak bisa mengambil dan mengubah KRS. Waktu pengambilan KRS sudah selesai atau belum berlangsung");
		waktuisiKrs.setParent(rowUtama1);

		if (Common.isMobile()) {
			rowUtama1 = new Row();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}
		rowUtama1.appendChild(new MyLabelConfig("Keterangan"));
		Html keteranganKrs = new Html();
		ais.ui.util.KrsMahasiswaAnalisisPopupHelper.pasang(keteranganKrs, mahasiswa, krsMahasiswa, false);
		rowUtama1.appendChild(keteranganKrs);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.setVisible(semester > 0);
		if (tahapan != null && tahapan.equals(-1)) {
			toolbar.setVisible(false);
		}

		buttonPerkuliahan = new MyButtonConfig("Ambil Paket Perkuliahan", "/img/svg/edit-box-line.svg");
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

			private AmbilDataPaketPerkuliahanHelper ambilDataPerkuliahanHelper = new AmbilDataPaketPerkuliahanHelper(
					semesterPendek);

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<String> warnings = new ArrayList<String>();
				if (mahasiswa != null) {
					List<SyaratUjian> syaratUjians = ConstantValues
							.simpleList(
									HibernateUtil.currentSession().createCriteria(SyaratUjian.class)
											.add(Restrictions.eq("krs", true)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
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

				if (!(konfigurasi != null && konfigurasi.getNilai() != null
						&& konfigurasi.getNilai().equals(Konfigurasi.AKTIF))
						&& !Common.checkApakahMahasiswaBolehAmbilKrsLewatPengecualian(mahasiswa, tahunAjaran,
								semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)) {
					MyMessageboxConfig.show(
					"Mohon maaf, waktu pengambilan paket perkuliahan untuk semester ini sudah selesai atau belum berlangsung sehingga Bapak/Ibu belum dapat mengambil KRS. Langkah yang dapat dilakukan: (1) tunggu hingga periode pengambilan KRS berikutnya dibuka; (2) apabila memerlukan pengecualian, hubungi bagian Akademik atau Admin Fakultas/Prodi.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
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

				konfigurasi = Common.getKonfigurasi("status_mahasiswa_harus_aktif_sebelum_isi_krs", Konfigurasi.AKTIF);

				if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
					HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa);

					StatusMahasiswa statusMahasiswa = historyStatusMahasiswa.ambilStatusMahasiswa(semester);
					if (!statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId())) {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, status kemahasiswaan Anda saat ini adalah \"{V1}\" sehingga Anda belum dapat mengambil KRS. Langkah yang dapat dilakukan: (1) pastikan status Anda telah Aktif pada semester berjalan; (2) hubungi Admin atau bagian Akademik untuk memperbarui status Anda; (3) setelah status Aktif, ambil kembali KRS ini.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								(statusMahasiswa.getNama()));
						return;
					}
				}
				// ayu

				String kelas = krsMahasiswa.getKelas();

				if (kelas == null) {
					MyMessageboxConfig.show(
								"Mohon maaf, Bapak/Ibu belum memiliki kelas sehingga belum dapat mengambil KRS. Langkah yang dapat dilakukan: (1) hubungi bagian Akademik untuk penetapan kelas Anda; (2) setelah kelas ditetapkan, ambil kembali KRS ini.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semesterPendek == null) {
					if (!Common.checkStatusPembayaranMahasiswaSebelumnya(semester, tahapan, mahasiswa)) {
						Double harusLunas = 90.0;
						try {
							harusLunas = Double.parseDouble(Common.getKonfigurasi(
									"batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs", "90")
									.getNilai().trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsPaketHelper.java:532");

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
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("krs", true)).list();
				if (!alasans.isEmpty()) {

					String alas = "";
					for (String s : alasans) {
						alas += alas.isEmpty() ? s : "\n\n" + s;
					}

					try {
						MyMessageboxConfig.show(alas, "Informasi KRS", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsPaketHelper.java:560");
					}

					return;
				}

				ambilDataPerkuliahanHelper.display(mahasiswa, tahunAjaran, semester, KrsPaketHelper.this);
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

		MyButtonConfig buttonAktif = new MyButtonConfig("Ket.Aktif", "/img/print.png");
		toolbar.appendChild(buttonAktif);
		buttonAktif.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				String jenisSemester = semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
				CommonReportHelper.prosesCetakKetAktif(mahasiswa, tahunAjaran, jenisSemester);
			}

		});

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
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hari/Waktu/Ruang");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		gridKomentar = new MyGrid();
		gridKomentar.setMold("paging");
		gridKomentar.setPageSize(20);
		gridKomentar.setParent(groupbox);

		jamBentrok.setVisible(semester > 0);
		gridKomentar.setVisible(semester > 0);

		if (tahapan != null && tahapan.equals(-1)) {
			gridKomentar.setVisible(false);
			jamBentrok.setVisible(false);
		}

		Columns columns2 = new Columns();
		columns2.setMenupopup("auto");
		columns2.setParent(gridKomentar);

		MyColumnConfig column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Komentar");
		column2.setWidth("70%");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Oleh");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("Tanggal");

		column2 = new MyColumnConfig();
		column2.setParent(columns2);
		column2.setLabel("");
		column2.setWidth("10%");

		loadDataKomentar();

		jamBentrok.setParent(groupbox);

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

	/**
	 * Menghitung ulang total SKS dan status persetujuan (sebagian/semua/belum disetujui) dari
	 * {@link #detailperkuliahans} saat ini, memperbarui label {@link #jumlahKRS}/
	 * {@link #statusPersetujuan}, dan menulis ulang keterangan status pengambilan KRS ke
	 * {@link #keteranganParent}.
	 */
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
		ais.ui.util.KrsMahasiswaAnalisisPopupHelper.pasang(
				keteranganParent, mahasiswa, krsMahasiswa, false);
	}

	/** Memuat ulang grid komentar dosen PA terkait KRS mahasiswa/semester/tahapan/tahun ajaran saat ini. */
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

	/** @return status semester pendek yang berlaku untuk KRS ini ({@code null} untuk KRS semester reguler) */
	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	/** @param semesterPendek status semester pendek baru untuk KRS ini ({@code null} untuk KRS semester reguler) */
	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

}
