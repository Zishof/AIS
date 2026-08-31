package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
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
import org.zkoss.zul.Vbox;

import ais.action.master.SyaratUjianAction;
import ais.action.master.helper.util.PenilaianUtil;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanKurikulumMahasiswa;
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
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.SyaratUjian;
import ais.database.model.file.LampiranLain;
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

/**
 * Helper composer ZK untuk halaman KRS (Kartu Rencana Studi) mode pemilihan matakuliah satu per
 * satu (via {@code AmbilDataPerkuliahanHelper}) — analog {@link KrsPaketHelper} tapi untuk mode
 * non-paket, dengan dukungan tambahan mode KRS remedial dan jendela terpisah "Perbaikan KRS"
 * (dikendalikan oleh {@link Konfigurasi} {@code KRS}/{@code KRS_SP}/{@code KRS_REMEDIAL} untuk
 * pengambilan awal dan {@code PERBAIKAN_KRS}/{@code PERBAIKAN_KRS_SP}/{@code PERBAIKAN_KRS_REMEDIAL}
 * untuk perbaikan — tombol "Ambil Perkuliahan" tampil bila salah satu dari kedua periode aktif).
 * Menampilkan ringkasan status (dosen PA, IPS/IPK, SKS, tahun akademik, kelas, semester, status
 * buka-tutup KRS), grid daftar {@link Detailperkuliahan} (dengan info kurikulum matakuliah dan
 * resolusi ekivalensi), informasi jam bentrok, dan grid komentar dosen PA.
 *
 * <p>
 * Tombol "Ambil Perkuliahan" menjalankan validasi berlapis serupa {@link KrsPaketHelper} (syarat
 * ujian, dosen PA wajib ada, kelas wajib ada bila dikonfigurasi, status pembayaran semester
 * berjalan/sebelumnya, status kemahasiswaan aktif, tidak ada {@link BlokirMahasiswa} aktif untuk
 * KRS) — dengan tambahan pengecekan berbeda saat periode "perbaikan" yang sedang aktif (mensyaratkan
 * mahasiswa sudah pernah mengambil KRS lebih dulu) dan penyesuaian gate pembayaran berbeda untuk
 * mode semester pendek ({@code mahasiswa_harus_bayar_sebelum_isi_krs_sp}). Toolbar tambahan
 * dibanding {@link KrsPaketHelper}: "Lihat Kurikulum" (buka {@code LaporanKurikulumMahasiswa}),
 * "Catatan" (cetak laporan catatan konsultasi), dan "Rekap SKS dan IPK" (unduh via
 * {@link PenilaianUtil#downloadSemuaKRS}); juga area unggah/unduh berkas KRS yang sudah
 * ditandatangani.
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} ({@link #loadData(Object)}) sehingga dapat menjadi
 * callback penyegar bagi {@code AmbilDataPerkuliahanHelper} setelah matakuliah diambil.
 * </p>
 */
public class KrsHelper implements DataLoader {

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

	private Konfigurasi konfigurasi;
	private Konfigurasi konfigurasiPerbaikan;
	private String tahunAjaran;
	private Integer semesterPendek = null;

	private Integer tahapan;
	private boolean remedial = false;

	/**
	 * @param semesterPendek status semester pendek yang berlaku ({@code null} untuk KRS reguler)
	 * @param remedial       bila {@code true}, helper beroperasi dalam mode KRS remedial
	 *                       (konfigurasi {@code KRS_REMEDIAL}/{@code PERBAIKAN_KRS_REMEDIAL})
	 */
	public KrsHelper(Integer semesterPendek, boolean remedial) {
		this.semesterPendek = semesterPendek;
		this.remedial = remedial;
	}

	/**
	 * Perender baris grid: sama seperti {@link KrsPaketHelper.DetailMahasiswaRenderer} (kode/nama/
	 * SKS matakuliah dengan info ekivalensi, dosen dan jadwal, semester dengan penanda Mengulang/
	 * Menabung, kelas, status persetujuan, tombol hapus), ditambah info kurikulum matakuliah pada
	 * label nama (mis. "(Kurikulum:2020)") dan baris disembunyikan sepenuhnya bila resolusi
	 * ekivalensi tidak menghasilkan pasangan matakuliah yang valid.
	 */
	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private boolean refresh;

		/** @param refresh bila {@code true}, cache resolusi ekivalensi matakuliah dipaksa dibangun ulang */
		public DetailMahasiswaRenderer(boolean refresh) {
			this.refresh = refresh;
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, data == null ? null : data.toString());
			if (detailperkuliahan == null) {
				row.setVisible(false);
				return;
			}
			Kurikulum kurikulum = detailperkuliahan.getPerkuliahan() == null ? null
					: detailperkuliahan.getPerkuliahan().getKurikulum();
			Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
					? detailperkuliahan.getMatakuliahKonversi()
					: detailperkuliahan.getPerkuliahan().getMatakuliah();

			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
					mahasiswa == null ? null : mahasiswa.getNim(), refresh);
			if (matakuliahs == null || matakuliahs.length < 2) {
				row.setVisible(false);
				return;
			}
			matakuliah = matakuliahs[0];
			Matakuliah matakuliahAsli = matakuliahs[1];

			if (matakuliah == null) {
				row.setVisible(false);
				return;
			}

			new Label(matakuliah.getId().equals(matakuliahAsli.getId())
					? matakuliah.getKode() + " (" + matakuliahAsli.getId() + ") "
					: matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")" + " (" + matakuliah.getId() + ") ")
					.setParent(row);
			new Label((matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama()
					: (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")"))
					+ (kurikulum == null ? "" : " (Kurikulum:" + kurikulum.getTahun() + ")")).setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "")
					: (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")).setParent(row);

			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, true);

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

			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(row, perkuliahan, detailperkuliahan);

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
												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(true);
													}
												});

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
	 * Memuat ulang grid matakuliah KRS, status persetujuan/jumlah SKS, dan informasi jam bentrok.
	 * Perilaku sama seperti {@link KrsPaketHelper#loadData(Object)}.
	 *
	 * @param value bila berupa {@link Boolean} {@code true}, paksa segarkan cache resolusi
	 *              ekivalensi matakuliah; selain itu dianggap {@code false}
	 */
	public void loadData(Object value) {
		boolean refresh = (value != null && value instanceof Boolean) ? (Boolean) value : false;
		detailperkuliahans = Common.getDetailperkuliahans(mahasiswa, semester, tahapan, null, semesterPendek, remedial,
				false, false, refresh);

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
	 * Membangun dan menampilkan seluruh UI KRS ke dalam {@code component}. Sama seperti
	 * {@link KrsPaketHelper#display}, dengan tambahan: dua konfigurasi periode dibaca sekaligus
	 * ({@link #konfigurasi} untuk pengambilan awal, {@link #konfigurasiPerbaikan} untuk perbaikan),
	 * toolbar tambahan (Lihat Kurikulum, Catatan, Rekap SKS dan IPK), dan area unggah berkas KRS
	 * yang sudah ditandatangani.
	 *
	 * @param editable      tidak dipakai langsung dalam badan method; diteruskan untuk kompatibilitas signature
	 * @param mahasiswa     mahasiswa pemilik KRS
	 * @param tahunAjaran   tahun ajaran KRS
	 * @param semester      semester KRS
	 * @param tahapan       tahapan pembayaran/KRS (bila fitur tahapan aktif); {@code -1} menyembunyikan grid/toolbar
	 * @param component     komponen ZK tujuan tampilan (dibersihkan lebih dulu)
	 * @param ket            komponen {@link Html} tujuan penulisan keterangan status KRS oleh {@link #loadStatus()}
	 * @param komentarshtml tidak dipakai langsung dalam badan method; diteruskan untuk kompatibilitas signature
	 * @param keDatabase    diteruskan ke {@link Common#singkronkanKrsMahasiswa} untuk menentukan apakah sinkronisasi KRS ditulis ke database
	 */
	public void display(final Boolean editable, final Mahasiswa mahasiswa, final String tahunAjaran,
			final Integer semester, final Integer tahapan, final Component component, final Html ket,
			final Html komentarshtml, boolean keDatabase) {
		this.mahasiswa = mahasiswa;
		this.semester = semester;

		this.tahapan = tahapan;
		this.keteranganParent = ket;
		this.tahunAjaran = tahunAjaran;
		if (Common.bolehKonfigurasi("input_krs_harus_berdasarkan_kalender_akademik")) {
			this.konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(HibernateUtil.currentSession(),
					remedial ? Konfigurasi.KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
					tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
					mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(),
					mahasiswa.getProgram());

			this.konfigurasiPerbaikan = Common.checkKonfigurasiDenganKalenderAkademik(HibernateUtil.currentSession(),
					remedial ? Konfigurasi.PERBAIKAN_KRS_REMEDIAL
							: semesterPendek == null ? Konfigurasi.PERBAIKAN_KRS : Konfigurasi.PERBAIKAN_KRS_SP,
					tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
					mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(), mahasiswa.getJurusan(),
					mahasiswa.getProgram());
		} else {
			this.konfigurasi = Common.getKonfigurasi(
					remedial ? Konfigurasi.KRS_REMEDIAL : semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
					tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
			this.konfigurasiPerbaikan = Common.getKonfigurasi(
					remedial ? Konfigurasi.PERBAIKAN_KRS_REMEDIAL
							: semesterPendek == null ? Konfigurasi.PERBAIKAN_KRS : Konfigurasi.PERBAIKAN_KRS_SP,
					tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		}

		Common.clear(component);

		final KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
				keDatabase);
		Double[] batas = Common.getMinDanMaxIPK(mahasiswa, semester, semesterPendek);
		Integer maxsks = batas[0].intValue();

		dosenPembimbingAkademik = krsMahasiswa.getDosenPa();

		MyGroupboxStyled groupbox = new MyGroupboxStyled();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("98%");
		groupbox.setParent(component);
		groupbox.appendChild(new Caption(mahasiswa.getNim() + " " + mahasiswa.getNama()));

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

		Label waktuisiKrs = new Label(this.konfigurasiPerbaikan != null && this.konfigurasiPerbaikan.getNilai() != null
				&& this.konfigurasiPerbaikan.getNilai().equals(Konfigurasi.AKTIF)
						? "Saat ini anda bisa memperbaiki " + Common.getBahasa("label_krs")
						: this.konfigurasi != null && this.konfigurasi.getNilai() != null
								&& this.konfigurasi.getNilai().equals(Konfigurasi.AKTIF)
										? "Saat ini anda bisa mengambil dan mengubah " + Common.getBahasa("label_krs")
										: "Saat ini anda tidak bisa mengambil dan mengubah KRS. Waktu pengambilan KRS sudah selesai atau belum berlangsung");
		waktuisiKrs.setParent(rowUtama1);

		String krs = mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek, krsMahasiswa, remedial);

		if (Common.isMobile()) {
			rowUtama1 = new Row();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("Keterangan"));
		rowUtama1.appendChild(new Html(krs));

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(semester > 0);
		if (tahapan != null && tahapan.equals(-1)) {
			toolbar.setVisible(false);
		}
		toolbar.setParent(groupbox);

		buttonPerkuliahan = new MyButtonConfig("Ambil Perkuliahan", "/img/svg/edit-box-line.svg");
		buttonPerkuliahan.setVisible((this.konfigurasi != null && this.konfigurasi.getNilai() != null
				&& this.konfigurasi.getNilai().equals(Konfigurasi.AKTIF))
				|| (this.konfigurasiPerbaikan != null && this.konfigurasiPerbaikan.getNilai() != null
						&& this.konfigurasiPerbaikan.getNilai().equals(Konfigurasi.AKTIF)));

		if (ConstantValues.aktifkanTahapan && tahapan != null && !tahapan.equals(0)) {
			Integer t = mahasiswa.currentTahapan();
			if (t != null && !t.equals(0) && tahapan >= t) {
				buttonPerkuliahan.setDisabled(false);
			} else {
				buttonPerkuliahan.setDisabled(true);
			}
		}

		buttonPerkuliahan.addEventListener("onClick", new EventListener() {

			private AmbilDataPerkuliahanHelper ambilDataPerkuliahanHelper = new AmbilDataPerkuliahanHelper(
					semesterPendek, remedial);

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				try {

					List<String> warnings = new ArrayList<String>();
					if (mahasiswa != null) {
						List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
								HibernateUtil.currentSession().createCriteria(SyaratUjian.class)
										.add(Restrictions.eq("krs", true)).add(Restrictions
												.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
								SyaratUjian.class);

						System.out.println("syaratUjians => " + syaratUjians);

						for (SyaratUjian syaratUjian : syaratUjians) {
							SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester,
									"Ambil KRS", warnings);
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
							&& konfigurasi.getNilai().equals(Konfigurasi.AKTIF))) {

						if ((konfigurasiPerbaikan != null && konfigurasiPerbaikan.getNilai() != null
								&& konfigurasiPerbaikan.getNilai().equals(Konfigurasi.AKTIF))) {
							if (detailperkuliahans.isEmpty()) {
								MyMessageboxConfig.show(
								"Mohon maaf, Bapak/Ibu belum pernah mengambil KRS sehingga perbaikan KRS tidak dapat dilakukan. Langkah yang dapat dilakukan: (1) pastikan Anda telah mengambil KRS pada semester berjalan; (2) hubungi bagian Akademik atau Admin Fakultas/Prodi untuk informasi lebih lanjut.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								return;
							}
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

					if (semesterPendek == null) {
						Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs",
								Konfigurasi.AKTIF);

						if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
							if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null
									|| tahapan.equals(0)) {
								if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false,
										semesterPendek != null)) {
									if (semester != null && semester.intValue() >= 1) {
										MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu belum menyelesaikan pembayaran biaya perkuliahan pada semester {V1}{V2}. Langkah yang dapat dilakukan: (1) lakukan pembayaran biaya perkuliahan terlebih dahulu; (2) setelah pembayaran tercatat, ambil kembali KRS ini; (3) apabila telah membayar namun status belum berubah, hubungi bagian Keuangan untuk informasi lebih lanjut.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								semester, (semesterPendek != null ? " semester pendek" : ""));
										return;
									}
								}
							}

							if (!UtsDanUasCheckerHelper.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahapan)) {
								return;
							}
						}
					} else {
						Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_sp",
								Konfigurasi.AKTIF);

						if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
							if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null
									|| tahapan.equals(0)) {
								if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa, false,
										semesterPendek != null)) {
									MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu belum menyelesaikan pembayaran biaya perkuliahan semester pendek pada semester {V1}. Langkah yang dapat dilakukan: (1) lakukan pembayaran biaya perkuliahan semester pendek terlebih dahulu; (2) setelah pembayaran tercatat, ambil kembali KRS semester pendek ini; (3) apabila telah membayar namun status belum berubah, hubungi bagian Keuangan untuk informasi lebih lanjut.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, semester);
									return;
								}
							}
						}
					}

					Konfigurasi konfigurasia = Common.getKonfigurasi("status_mahasiswa_harus_aktif_sebelum_isi_krs",
							Konfigurasi.AKTIF);

					if (konfigurasia.getNilai().equals(Konfigurasi.AKTIF)) {

						HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa);

						StatusMahasiswa statusMahasiswa = historyStatusMahasiswa.ambilStatusMahasiswa(semester);
						if (statusMahasiswa == null || !statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId())) {
							MyMessageboxConfig.showFormat("Mohon maaf, status kemahasiswaan Anda saat ini adalah \"{V1}\" sehingga Anda belum dapat mengambil KRS. Langkah yang dapat dilakukan: (1) pastikan status Anda telah Aktif pada semester berjalan; (2) hubungi Admin atau bagian Akademik untuk memperbarui status Anda; (3) setelah status Aktif, ambil kembali KRS ini.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								(statusMahasiswa == null ? "" : statusMahasiswa.getNama()));
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
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/KrsHelper.java:608");

							}
							MyMessageboxConfig.showFormat(
								"Mohon maaf, Bapak/Ibu belum melunasi {V1}% biaya perkuliahan pada{V2}. Langkah yang dapat dilakukan: (1) lakukan pelunasan biaya perkuliahan sesuai ketentuan; (2) setelah pembayaran tercatat, ambil kembali KRS ini; (3) apabila telah membayar namun status belum berubah, hubungi bagian Keuangan untuk informasi lebih lanjut.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
								harusLunas, ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan != null && tahapan > 0) ? " tahap " + (tahapan - 1) : " semester " + (semester - 1)));
							return;
						}

					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
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
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsHelper.java:640");
					}

					return;
				}

				ambilDataPerkuliahanHelper.display(mahasiswa, tahunAjaran, semester, tahapan, KrsHelper.this,
						detailperkuliahans);
			}

		});
		buttonPerkuliahan.setParent(toolbar);

		MyButtonConfig button = new MyButtonConfig("Komentar", "/img/m3.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				KomentarHelper komentarHelper = new KomentarHelper(mahasiswa, tahunAjaran, semester, tahapan,
						semesterPendek, remedial, dosenPembimbingAkademik);

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
				CommonReportHelper.cetakKRS(mahasiswa, semester, tahapan, semesterPendek, remedial);
			}

		});
		button.setParent(toolbar);

		button = new MyButtonConfig("Cetak Kartu UTS", "/img/print.png");
		button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uts"));
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakUTS(mahasiswa, semester, tahapan, tahunAjaran, semesterPendek, remedial, false);
			}

		});
		button.setParent(toolbar);

		button = new MyButtonConfig("Cetak Kartu UAS", "/img/print.png");
		button.setVisible(Common.bolehKonfigurasi("tampilkan_tombol_cetak_kartu_uas"));
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.cetakUAS(mahasiswa, semester, tahapan, tahunAjaran, semesterPendek, remedial, false);

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

		final MyButtonConfig kurikulum = new MyButtonConfig("Lihat Kurikulum", "/img/excel.png");
		toolbar.appendChild(kurikulum);
		kurikulum.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				LaporanKurikulumMahasiswa laporanKurikulum = new LaporanKurikulumMahasiswa(mahasiswa, semester);
				laporanKurikulum.setTitle("Riwayat Kurikulum");
				laporanKurikulum.setHeight("95%");
				laporanKurikulum.setWidth("90%");
				laporanKurikulum.setClosable(true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanKurikulum);
				laporanKurikulum.onModal();
			}
		});

		MyButtonConfig catatanMurikulum = new MyButtonConfig("Catatan", "/img/print.png");
		toolbar.appendChild(catatanMurikulum);
		catatanMurikulum.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				@SuppressWarnings("rawtypes")
				final Map parameters = ais.common.HashMapGenerator.getRand();

				parameters.put("perkuliahan", mahasiswa.getId());

				Report.generatePDFReport(Report.PDF, parameters, "catatan_konsultasi", ais.ui.util.WaktuUtil.getDate());
			}
		});

		catatanMurikulum = new MyButtonConfig("Rekap SKS dan IPK", "/img/print.png");
		toolbar.appendChild(catatanMurikulum);
		catatanMurikulum.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PenilaianUtil.downloadSemuaKRS(krsMahasiswa.getSkskS(), krsMahasiswa.getMahasiswa());
			}
		});

		Vbox myvbox = new Vbox();
		myvbox.setParent(groupbox);

		Hbox hbox = new Hbox();
		hbox.setParent(myvbox);
		LampiranLain.createDownloadUploadFileLain(hbox, krsMahasiswa.getId(), "UPLOAD_KRS_DISETUJUI",
				"KRS disetujui / di-tanda-tangani", false, null, null, false, false, false, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.setParent(groupbox);
//		grid.setOddRowSclass("non-odd");
		grid.setSclass("dgrid");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("25%");

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
		column.setLabel("Hari / Waktu / Ruang");
		column.setWidth("25%");

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
			jamBentrok.setVisible(false);
			gridKomentar.setVisible(false);
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
	 * Menghitung ulang total SKS dan status persetujuan dari {@link #detailperkuliahans} saat ini,
	 * memperbarui label {@link #jumlahKRS}/{@link #statusPersetujuan}, dan menulis ulang keterangan
	 * status pengambilan KRS ke {@link #keteranganParent}.
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

		jumlahKRS.setValue(jmlKrs + " SKS");

		if (adaPersetujuan && adaBelumPersetujuan) {

			statusPersetujuan.setValue("Sebagian sudah disetujui");
		} else if (adaPersetujuan) {

			statusPersetujuan.setValue("Sudah disetujui semua");
		} else if (adaBelumPersetujuan) {

			statusPersetujuan.setValue("Belum disetujui semua");
		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek);
		keteranganParent.setContent(mahasiswa.rubahKeteranganPengambilanKRS(krsMahasiswa.getSemester(),
				krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa, remedial));
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

	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

}
