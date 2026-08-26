package ais.action.master.helper.profile;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Box;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.BukuBahanAjarAction;
import ais.action.master.DosenAction;
import ais.action.master.PengumumanAkademisAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.DetailArtikelHelper;
import ais.action.master.helper.DosenPunyaKegiatanKedosenanHelper;
import ais.action.master.helper.DosenPunyaOrganisasiDosenHelper;
import ais.action.master.penelitiandanpengabdian.helper.PengajuanPenelitianDanPengabdianHelper;
import ais.action.report.format1.akademik.LaporanAngketDosenPerDosenWindow;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.Dosen;
import ais.database.model.FormulirKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisFormulirKegiatan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.OrganisasiDosenPunyaDosen;
import ais.database.model.PenghargaanDosen;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiDosen;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.PengajuanPenelitianDanPengabdian;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>ProfileDosen — Panel Profil Lengkap Dosen</h3>
 *
 * <p><b>Untuk apa:</b> Menyajikan dashboard profil satu halaman bagi dosen yang mencakup
 * semua aspek aktivitas akademik dan keprofesian: perkuliahan aktif (dengan materi/tugas/ujian),
 * KKN, PKL, bimbingan akademik (PA), bimbingan TA/skripsi, sidang, kegiatan kedosenan,
 * organisasi, prestasi, karya, penelitian, pengabdian, publikasi, dan buku.</p>
 *
 * <p><b>Cara kerja:</b> Konstruktor menerima entitas {@link Dosen} dan menyimpannya.
 * Kemudian {@link #init(Component, boolean)} dipanggil dari controller profil untuk
 * membangun seluruh komponen ZK secara progresif ke dalam {@code parent}. Setiap seksi
 * dibangun dengan pola Group + Row ZK.</p>
 *
 * <p><b>Paging:</b> Setiap seksi perkuliahan/KKN/PKL/PA/TA/skripsi mendukung paging
 * dengan {@code jumlahDataDalamSatuHalamanElearning = 3}. Variabel paging per-seksi
 * (mis. {@code pagingPerkuliahan}) dilacak dalam instance. Tombol "Sebelumnya/Selanjutnya"
 * memanggil re-render seksi terkait saja (bukan refresh seluruh halaman).</p>
 *
 * <p><b>Seksi yang ditampilkan (berurutan):</b>
 * <ol>
 *   <li>Toolbar: Biodata (DRH), Indeks Prestasi Dosen, Refresh</li>
 *   <li>Kartu identitas dosen (foto, nama, NIP, jabatan, golongan, unit kerja)</li>
 *   <li>Panel Perkuliahan dengan search + paging (incl. materi/tugas/ujian jika aktif)</li>
 *   <li>Panel KKN (jika konfigurasi aktif)</li>
 *   <li>Panel PKL</li>
 *   <li>Panel Pembimbing Akademik (KRS mahasiswa sebagai PA)</li>
 *   <li>Panel FormulirKegiatan per jenis (group project, pra-sidang, dll.)</li>
 *   <li>Panel Bimbingan TA/Skripsi</li>
 *   <li>Panel Sidang TA/Skripsi</li>
 *   <li>Panel Kegiatan, Organisasi, Prestasi, Karya Dosen</li>
 *   <li>Panel Penelitian, Pengabdian, Publikasi (Artikel), Buku Bahan Ajar</li>
 * </ol>
 * </p>
 *
 * <p><b>Konfigurasi yang diperiksa:</b>
 * <ul>
 *   <li>{@code Konfigurasi.AKTIFKAN_KKN} untuk menampilkan panel KKN</li>
 *   <li>{@code tampilMateri} untuk menampilkan materi, tugas, ujian per kelas</li>
 * </ul>
 * </p>
 *
 * <p><b>Threading:</b> Harus dipanggil dari ZK event thread karena memodifikasi
 * pohon komponen ZK. Semua event listener (onChange, onClick) berjalan di event thread.</p>
 *
 * <p><b>Pemeliharaan:</b> Java 1.7 dan ZKoss 5.5. Method {@code init} menggunakan
 * {@code @SuppressWarnings("deprecation", "unchecked")} untuk API ZK dan Hibernate lama.
 * Penambahan seksi baru harus mengikuti pola Group + MyRowStyled + kolspan-2 yang sudah ada.</p>
 */
public class ProfileDosen {

	private Dosen dosen;

	String tahunAkademik = null;
	String jenisSemester = null;
	String hr = null;
	String keyword = "";

	String kelas = "";

	private int jumlahDataDalamSatuHalamanElearning = 3;
	private int pagingPerkuliahan = 1;
	private int pagingKkn = 1;
	private int pagingPkl = 1;
	private int pagingKrsMahasiswa = 1;
	private int pagingFormulirKegiatan = 1;
	private int pagingMahasiswaRequestTugasAkhir = 1;
	private int pagingSkripsi = 1;

	/**
	 * Membuat instance panel profil dosen dengan entitas dosen yang ditentukan.
	 *
	 * <p>Menyimpan referensi entitas {@link Dosen} untuk diakses selama {@link #init}.
	 * Tidak ada query database pada tahap ini — semua query dilakukan di {@link #init}.</p>
	 *
	 * @param dosen entitas dosen yang profilnya akan ditampilkan; boleh null
	 *              (beberapa seksi mungkin kosong, namun tidak akan menyebabkan exception)
	 * @throws Exception diwarisi dari {@code super()} (tidak ada exception nyata di sini)
	 */
	public ProfileDosen(Dosen dosen) throws Exception {
		super();
		this.dosen = dosen;
	}

	/**
	 * Membangun seluruh panel profil dosen ke dalam komponen ZK yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Titik masuk utama untuk merender profil dosen secara lengkap.
	 * Setiap seksi dibangun secara berurutan ke dalam grid ZK di dalam {@code contentParent}.</p>
	 *
	 * <p><b>Urutan render:</b>
	 * <ol>
	 *   <li>Mempersiapkan {@code contentParent} via
	 *       {@link ProfileUiHelper#prepareContentParent(Component)},
	 *       memuat banner PT dari media konfigurasi, dan membangun Grid 2-kolom.</li>
	 *   <li>Toolbar aksi: tombol Biodata (cetak DRH {@code DosenAction.onCetakBiodata}),
	 *       Indeks Prestasi Dosen ({@code LaporanAngketDosenPerDosenWindow}), Refresh
	 *       (re-inisialisasi semua koleksi paging).</li>
	 *   <li>Kartu identitas dosen (foto, sapaan, NIP/NIDN, jabatan, dll.) via
	 *       {@link ProfileUiHelper#mulaiKartuIdentitas}.</li>
	 *   <li>Checkbox "Tampilkan Materi" per kelas (hanya jika {@code tampilMateri=true}).</li>
	 *   <li>Panel Perkuliahan: search textbox + event listener paging +
	 *       {@code TampilanELearningAction.PERKULIAHAN} via
	 *       {@code ProfileUtil.ambilPerkuliahanDanParalel}.</li>
	 *   <li>Panel KKN (jika {@code Konfigurasi.AKTIFKAN_KKN} aktif): paging + search.</li>
	 *   <li>Panel PKL: paging + search.</li>
	 *   <li>Panel Pembimbing Akademik: filter {@code dosenPa.id == dosen.id}, paging.</li>
	 *   <li>Panel FormulirKegiatan per {@code JenisFormulirKegiatan} (group project, dll.)
	 *       + satu group tanpa jenis.</li>
	 *   <li>Panel Bimbingan TA/Skripsi ({@code MahasiswaRequestTugasAkhir}): paging + search.</li>
	 *   <li>Panel Sidang TA/Skripsi ({@code Skripsi}): paging + search.</li>
	 *   <li>Panel Kegiatan Dosen: {@code GeneralValueObject.ambilData(KegiatanKedosenanPunyaDosen)},
	 *       tombol Ajukan Baru.</li>
	 *   <li>Panel Organisasi, Prestasi, Karya Dosen: pola sama.</li>
	 *   <li>Panel Penelitian, Pengabdian: {@code PengajuanPenelitianDanPengabdian}
	 *       dengan jenis PENELITIAN/PENGABDIAN.</li>
	 *   <li>Panel Publikasi ({@code Artikel}), Buku ({@code BukuBahanAjar}):
	 *       tombol Pengajuan + {@code MyInclude} iframe untuk form.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Setiap seksi dibungkus try-catch; kegagalan satu seksi
	 * tidak menghentikan render seksi lainnya. Error ditampilkan via
	 * {@code Common.tampilErrorJikaAdmin(e)} atau {@code MyMessageboxConfig.errorBox}.</p>
	 *
	 * @param parent       komponen ZK tujuan; jika null method langsung return
	 * @param tampilMateri {@code true} untuk menampilkan materi, tugas, dan ujian
	 *                     per perkuliahan; {@code false} untuk ringkasan saja
	 * @throws Exception bila terjadi error fatal dalam penyusunan komponen ZK
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	public void init(final Component parent, final boolean tampilMateri) throws Exception {

		if (parent == null) {
			return;
		}
		String waktu = ProfileUiHelper.waktuSapaan();

		Component contentParent = ProfileUiHelper.prepareContentParent(parent);

		String background_PerguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia(
				(javax.servlet.http.HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest(),
				"banner_perguruanTinggi_");
		if (background_PerguruanTinggi == null || background_PerguruanTinggi.trim().isEmpty()) {
			background_PerguruanTinggi = ais.common.Common.getRequestHostWithProtocol() + "/img/header.jpg";
		}

		Div groupboxStyled = new Div();
		groupboxStyled.setStyle("padding-left: 7px;");
		groupboxStyled.setParent(contentParent);

		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setParent(groupboxStyled);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("80px");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (parent instanceof LayoutRegion) {
			((LayoutRegion) parent).setTitle("Hai, Selamat " + waktu);
			((LayoutRegion) parent).setCollapsible(true);
			((LayoutRegion) parent).setSplittable(true);
		} else {

			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Profil Dosen", "Jadwal mengajar, bimbingan, kegiatan, penelitian, pengabdian, publikasi, dan bahan ajar dosen tersaji ringkas dalam satu tempat.");
		}

		/* Kartu identitas satu baris: foto + sapaan + nama + kontak */
		Component fotoDosen = null;
		try {
			fotoDosen = CommonMedia.tampilkanGambarKecil(dosen);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileDosen.java:258");
		}
		org.zkoss.zul.Div infoKartu = ProfileUiHelper.mulaiKartuIdentitas(rows, 2, fotoDosen, "Hai, Selamat " + waktu);
		infoKartu.appendChild(new MyLabelBoldAja(dosen.getNama()));
		infoKartu.appendChild(new MyLabelBoldAja(dosen.getNidn()));
		infoKartu.appendChild(new MyLabelBoldAja(dosen.getMycode()));
		dosen.tampilkanHp(infoKartu);
		dosen.tampilkanEmail(infoKartu);

		Row row;
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Box hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Biodata", "/img/online-icon_access.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				DosenAction.cetakDRHDosen(dosen);
			}
		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Indeks Prestasi Dosen", "/img/Diploma-Certificate-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				LaporanAngketDosenPerDosenWindow laporanTranskipAkademik = new LaporanAngketDosenPerDosenWindow();
				laporanTranskipAkademik.setTitle("Indeks Prestasi Dosen");
				laporanTranskipAkademik.setClosable(true);
				laporanTranskipAkademik.setBorder("none");
				laporanTranskipAkademik.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				laporanTranskipAkademik.setHeight("95%");
				laporanTranskipAkademik.setWidth("90%");
				laporanTranskipAkademik.onModal();
			}
		});
		button.setParent(hbox);

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = null;
						try {
							session = HibernateUtil.getSessionFactory().openSession();

							dosen.reInitPerkuliahan(session);
							dosen.reInitSkripsi(session);
							dosen.reInitBimbingan(session);
							dosen.reInitKkn(session);
							dosen.reInitPkl(session);
							dosen.reInitKrs(session);
							dosen.reInitFormulirKegiatanPeserta(session);
							dosen.reInitKonsultasi(session);

							dosen.reInitArtikel(session);
							dosen.reInitKegiatanKedosenanPunyaDosen(session);
							dosen.reInitOrganisasiDosenPunyaDosen(session);
							dosen.reInitPrestasiDosen(session);
							dosen.reInitPenghargaanDosen(session);
							dosen.reInitPengajuanPenelitianDanPengabdian(session);
							dosen.reInitBukuBahanAjar(session);

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileDosen.java:334");
						} finally {
							ProfileUiHelper.closeOpenSession(session);
						}

						init(parent, tampilMateri);
					}
				});
			}
		});
		button.setParent(hbox);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyCheckboxConfig materiPerkuliahan = new MyCheckboxConfig("Tampilkan juga materi, tugas, dan ujian");
		materiPerkuliahan.setChecked(tampilMateri);
		materiPerkuliahan.setStyle("font-size:9px");
		materiPerkuliahan.setParent(row);
		materiPerkuliahan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				init(parent, materiPerkuliahan.isChecked());
			}
		});

		rows.appendChild(new ais.ui.util.MyGroupConfig("Perkuliahan"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Perkuliahan", "Jadwal mengajar dosen ditampilkan agar kelas, materi, tugas, ujian, dan aktivitas e-Learning mudah dibuka.");

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		Hbox cariHbox = new Hbox();
		cariHbox.setParent(row);
		cariHbox.appendChild(new Space());
		cariHbox.appendChild(new Space());
		final Textbox cariPerkuliahan = new Textbox();
		cariPerkuliahan.setCols(20);
		cariPerkuliahan.setParent(cariHbox);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Rows rowsPerkuliahan = (Rows) Common.tampilanScroll1(row).getParent();

		final EventListener rowEventPerkuliahan = new EventListener() {

			public EventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
						cariPerkuliahan.getValue().trim(), kelas, false, null, true, false, false,

						true, true, true, true, true,

						true, true, true, true,

						TampilanELearningAction.PERKULIAHAN,

						(pagingPerkuliahan - 1) * jumlahDataDalamSatuHalamanElearning,
						jumlahDataDalamSatuHalamanElearning, true);
				List<Perkuliahan> perkuliahans = (List<Perkuliahan>) objects[0];

				if (perkuliahans == null || perkuliahans.isEmpty()) {
					Row row = new MyRowStyled();
					row.setParent(rowsPerkuliahan);
					Label a;
					row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Tidak ada jadwal perkuliahan")));
					a.setStyle("font-size:11px;font-weight: bolder;color:red;");
				} else {
					for (final Perkuliahan perkuliahan : perkuliahans) {
						Row row = new MyRowStyled();
						row.setParent(rowsPerkuliahan);
						Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(perkuliahan.infoSimple() + " "
								+ perkuliahan.getTahunAjaran() + "/" + perkuliahan.getGanjilGenap(),
								"/img/Healthcare-Groups-icon.png");
						row.appendChild(toolbarbuttonData);
						toolbarbuttonData.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								TampilanELearningAction.prosess(perkuliahan, true);
							}
						});

						if (tampilMateri) {
							ProfileUtil.tampilkanMateri(rowsPerkuliahan, perkuliahan.ambilPertemuan());
						}
					}
				}

				if (perkuliahans != null && !perkuliahans.isEmpty()) {
					Row row = new MyRowStyled();
					rowsPerkuliahan.appendChild(row);

					MyToolbarbuttonConfig a = new MyToolbarbuttonConfig("Tampilkan perkuliahan selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							arg0.getTarget().getParent().setVisible(false);

							try {
								pagingPerkuliahan++;
								getThis().onEvent(null);
							} catch (Exception e) {
								MyMessageboxConfig.show("Tidak ada perkuliahan selanjutnya yang bisa ditampilkan",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
				}

				perkuliahans = null;
			}
		};

		button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPerkuliahan = 1;
				Common.clear(rowsPerkuliahan);
				rowEventPerkuliahan.onEvent(arg0);
			}
		});
		button.setParent(cariHbox);

		cariPerkuliahan.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPerkuliahan = 1;
				Common.clear(rowsPerkuliahan);
				rowEventPerkuliahan.onEvent(arg0);
			}
		});

		rowEventPerkuliahan.onEvent(null);

		boolean mobile = Common.isMobile();
		if (mobile && !PengumumanAkademisAction.isKehadiranHomeDitampilkan()) {
			Tbmuser tbmuser = Common.getCurrentUser();
			String pengumuman = PengumumanAkademisAction.tampilkanKehadiranDosen(tbmuser, mobile);
			if (!pengumuman.isEmpty()) {
				row = new MyRowStyled();
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				row.appendChild(new Html(pengumuman));
			}
		}

		if (Common.bolehKonfigurasi("tampilkan_kkn_di_dashboard_samping")) {

			rows.appendChild(new ais.ui.util.MyGroupConfig(Common.getBahasaConfig("KKN")));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, Common.getBahasaConfig("KKN"), "Kelompok atau mahasiswa KKN bimbingan dosen ditampilkan agar proses pembimbingan mudah dipantau.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			cariHbox = new Hbox();
			cariHbox.setParent(row);
			cariHbox.appendChild(new Space());
			cariHbox.appendChild(new Space());
			final Textbox cariKkn = new Textbox();
			cariKkn.setCols(20);
			cariKkn.setParent(cariHbox);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			final Rows rowsKkn = (Rows) Common.tampilanScroll1(row).getParent();

			final EventListener rowEventKkn = new EventListener() {

				public EventListener getThis() {
					return this;
				}

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentSession();
					Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
							cariKkn.getValue().trim(), kelas, false, null, true, false, false,

							true, true, true, true, true,

							true, true, true, true,

							TampilanELearningAction.KKN,

							(pagingKkn - 1) * jumlahDataDalamSatuHalamanElearning, jumlahDataDalamSatuHalamanElearning,
							true);
					List<KelompokKkn> kkns = (List<KelompokKkn>) objects[0];

					if (kkns == null || kkns.isEmpty()) {
						Row row = new MyRowStyled();
						row.setParent(rowsKkn);
						Label a;
						row.appendChild(
								a = new Label("Tidak ada jadwal sebagai pembimbing " + Common.getBahasaConfig("KKN")));
						a.setStyle("font-size:11px;font-weight: bolder;color:red;");
					} else {
						for (final KelompokKkn kkn : kkns) {
							if (kkn.getKkn() != null) {
								Row row = new MyRowStyled();
								row.setParent(rowsKkn);
								Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(kkn.infoSimple() + " "
										+ kkn.getKkn().getTahunAkademik() + "/" + kkn.getKkn().getSemester(),
										"/img/Healthcare-Groups-icon.png");
								row.appendChild(toolbarbuttonData);
								toolbarbuttonData.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										TampilanELearningAction.prosess(kkn, true);
									}
								});

								if (tampilMateri) {
									ProfileUtil.tampilkanMateri(rowsKkn, kkn.ambilPertemuan());
								}
							}
						}
					}

					if (kkns != null && !kkns.isEmpty()) {
						Row row = new MyRowStyled();
						rowsKkn.appendChild(row);

						Toolbarbutton a = new MyToolbarbuttonConfig(
								"Tampilkan kelompok " + Common.getBahasaConfig("KKN") + " selanjutnya..",
								"/img/Button-Next-icon.png");
						a.setStyle("font-size:11px;");
						row.appendChild(a);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								arg0.getTarget().getParent().setVisible(false);

								try {
									pagingKkn++;
									getThis().onEvent(null);
								} catch (Exception e) {
									MyMessageboxConfig.show(
											"Tidak ada kelompok " + Common.getBahasaConfig("KKN")
													+ " selanjutnya yang bisa ditampilkan",
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								}
							}
						});
					}

					kkns = null;
				}
			};

			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingKkn = 1;
					Common.clear(rowsKkn);
					rowEventKkn.onEvent(arg0);
				}
			});
			button.setParent(cariHbox);

			cariKkn.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingKkn = 1;
					Common.clear(rowsKkn);
					rowEventKkn.onEvent(arg0);
				}
			});

			rowEventKkn.onEvent(null);

		}

		rows.appendChild(new ais.ui.util.MyGroupConfig(Common.getBahasaConfig("PKL")));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, Common.getBahasaConfig("PKL"), "Data PKL yang berkaitan dengan dosen ditampilkan agar pembimbingan dan aktivitas lapangan mudah dipantau.");

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		cariHbox = new Hbox();
		cariHbox.setParent(row);
		cariHbox.appendChild(new Space());
		cariHbox.appendChild(new Space());
		final Textbox cariPkl = new Textbox();
		cariPkl.setCols(20);
		cariPkl.setParent(cariHbox);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Rows rowsPkl = (Rows) Common.tampilanScroll1(row).getParent();

		final EventListener rowEventPkl = new EventListener() {

			public EventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
						cariPkl.getValue().trim(), kelas, false, null, true, false, false,

						true, true, true, true, true,

						true, true, true, true,

						TampilanELearningAction.PKL,

						(pagingPkl - 1) * jumlahDataDalamSatuHalamanElearning, jumlahDataDalamSatuHalamanElearning,
						true);
				List<KelompokPkl> pkls = (List<KelompokPkl>) objects[0];

				if (pkls == null || pkls.isEmpty()) {
					Row row = new MyRowStyled();
					row.setParent(rowsPkl);
					Label a;
					row.appendChild(
							a = new Label("Tidak ada jadwal sebagai pembimbing " + Common.getBahasaConfig("PKL")));
					a.setStyle("font-size:11px;font-weight: bolder;color:red;");
				} else {
					for (final KelompokPkl pkl : pkls) {
						Row row = new MyRowStyled();
						row.setParent(rowsPkl);
						Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(pkl.infoSimple() + " "
								+ pkl.getPkl().getTahunAkademik() + "/" + pkl.getPkl().getSemester(),
								"/img/Healthcare-Groups-icon.png");
						row.appendChild(toolbarbuttonData);
						toolbarbuttonData.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								TampilanELearningAction.prosess(pkl, true);
							}
						});

						if (tampilMateri) {
							ProfileUtil.tampilkanMateri(rowsPkl, pkl.ambilPertemuan());
						}
					}
				}

				if (pkls != null && !pkls.isEmpty()) {
					Row row = new MyRowStyled();
					rowsPkl.appendChild(row);

					Toolbarbutton a = new MyToolbarbuttonConfig(
							"Tampilkan kelompok " + Common.getBahasaConfig("PKL") + " selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							arg0.getTarget().getParent().setVisible(false);

							try {
								pagingPkl++;
								getThis().onEvent(null);
							} catch (Exception e) {
								MyMessageboxConfig.show(
										"Tidak ada kelompok " + Common.getBahasaConfig("PKL")
												+ " selanjutnya yang bisa ditampilkan",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
				}

				pkls = null;
			}
		};

		button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPkl = 1;
				Common.clear(rowsPkl);
				rowEventPkl.onEvent(arg0);
			}
		});
		button.setParent(cariHbox);

		cariPkl.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingPkl = 1;
				Common.clear(rowsPkl);
				rowEventPkl.onEvent(arg0);
			}
		});

		rowEventPkl.onEvent(null);

		rows.appendChild(new ais.ui.util.MyGroupConfig("Persetujuan KRS - "
				+ Common.getBahasaConfig("Pembimbing Akademik")));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Persetujuan KRS",
				"Menampilkan mahasiswa bimbingan yang masih mempunyai mata kuliah KRS belum disetujui. Klik nama mahasiswa untuk memeriksa dan memproses persetujuan.");

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		cariHbox = new Hbox();
		cariHbox.setParent(row);
		cariHbox.appendChild(new Space());
		cariHbox.appendChild(new Space());
		final Textbox cariKrsMahasiswa = new Textbox();
		cariKrsMahasiswa.setCols(20);
		cariKrsMahasiswa.setParent(cariHbox);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Rows rowsKrsMahasiswa = (Rows) Common.tampilanScroll1(row).getParent();

		final EventListener rowEventKrsMahasiswa = new EventListener() {

			public EventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				String tahunAkademik = Common.getCurrentTahunAkademik();
				String jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

				Session session = HibernateUtil.currentSession();
				Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
						cariKrsMahasiswa.getValue().trim(), kelas, false, null, true, false, false,

						true, true, true, true, true,

						true, true, false, true,

						TampilanELearningAction.KRS,

						(pagingKrsMahasiswa - 1) * jumlahDataDalamSatuHalamanElearning,
						jumlahDataDalamSatuHalamanElearning, false);
				List<KrsMahasiswa> krsMahasiswas = (List<KrsMahasiswa>) objects[0];

				if (krsMahasiswas == null || krsMahasiswas.isEmpty()) {
					Row row = new MyRowStyled();
					row.setParent(rowsKrsMahasiswa);
					Label a;
					row.appendChild(a = new Label("Tidak ada KRS mahasiswa bimbingan yang menunggu persetujuan"));
					a.setStyle("font-size:11px;font-weight: bolder;color:red;");
				} else {
					for (final KrsMahasiswa krsMahasiswa : krsMahasiswas) {

						if (dosen != null && krsMahasiswa.getDosenPa() != null
								&& krsMahasiswa.getDosenPa().getId().equals(dosen.getId())) {

							Row row = new MyRowStyled();
							row.setParent(rowsKrsMahasiswa);
							Toolbarbutton toolbarbuttonData = new ais.ui.util.MyToolbarbuttonConfig(
									krsMahasiswa.infoSimple().replaceAll("<br>", "\n"),
									"/img/svg/user-follow-line.svg");
							row.appendChild(toolbarbuttonData);
							toolbarbuttonData.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									TampilanELearningAction.prosess(krsMahasiswa, true);
								}
							});

							if (tampilMateri) {
								ProfileUtil.tampilkanMateri(rowsKrsMahasiswa, krsMahasiswa.ambilPertemuan());
							}
						}
					}
				}

				if (krsMahasiswas != null && !krsMahasiswas.isEmpty()) {
					Row row = new MyRowStyled();
					rowsKrsMahasiswa.appendChild(row);

					Toolbarbutton a = new MyToolbarbuttonConfig("Tampilkan bimbingan akademik selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							arg0.getTarget().getParent().setVisible(false);

							try {
								pagingKrsMahasiswa++;
								getThis().onEvent(null);
							} catch (Exception e) {
								MyMessageboxConfig.show(
										"Tidak ada bimbingan akademik selanjutnya yang bisa ditampilkan", "Informasi",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
				}

				krsMahasiswas = null;
			}
		};

		button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				dosen.reInitKrs(session);

				pagingKrsMahasiswa = 1;
				Common.clear(rowsKrsMahasiswa);
				rowEventKrsMahasiswa.onEvent(arg0);
			}
		});
		button.setParent(cariHbox);

		cariKrsMahasiswa.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingKrsMahasiswa = 1;
				Common.clear(rowsKrsMahasiswa);
				rowEventKrsMahasiswa.onEvent(arg0);
			}
		});

		rowEventKrsMahasiswa.onEvent(null);

		List<JenisFormulirKegiatan> jenisFormulirKegiatans = new ArrayList<JenisFormulirKegiatan>();
		int adakosong = 0;
		Session sessionFormulir = null;
		try {
			sessionFormulir = HibernateUtil.getSessionFactory().openSession();
			jenisFormulirKegiatans = ConstantValues.simpleList(sessionFormulir.createCriteria(FormulirKegiatan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNotNull("jenisFormulirKegiatan"))
					.setProjection(Projections.groupProperty("jenisFormulirKegiatan.id"))
					.addOrder(Order.asc("jenisFormulirKegiatan.id")), JenisFormulirKegiatan.class, false);
			adakosong = ((Number) sessionFormulir.createCriteria(FormulirKegiatan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.isNull("jenisFormulirKegiatan")).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileDosen.java:918");
		} finally {
			ProfileUiHelper.closeOpenSession(sessionFormulir);
		}

		for (final JenisFormulirKegiatan jenisFormulirKegiatan : jenisFormulirKegiatans) {
			rows.appendChild(new ais.ui.util.MyGroupConfig(jenisFormulirKegiatan.getNama()));

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			cariHbox = new Hbox();
			cariHbox.setParent(row);
			cariHbox.appendChild(new Space());
			cariHbox.appendChild(new Space());
			final Textbox cariFormulirKegiatan = new Textbox();
			cariFormulirKegiatan.setCols(20);
			cariFormulirKegiatan.setParent(cariHbox);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			final Rows rowsFormulirKegiatan = (Rows) Common.tampilanScroll1(row).getParent();

			final EventListener rowEventFormulirKegiatan = new EventListener() {

				public EventListener getThis() {
					return this;
				}

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentSession();
					Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
							cariFormulirKegiatan.getValue().trim(), kelas, false, null, true, false, false,

							true, true, true, true, true,

							true, true, true, true,

							TampilanELearningAction.KEGIATAN,

							(pagingFormulirKegiatan - 1) * jumlahDataDalamSatuHalamanElearning,
							jumlahDataDalamSatuHalamanElearning, true, jenisFormulirKegiatan);
					List<FormulirKegiatan> formulirKegiatans = (List<FormulirKegiatan>) objects[0];

					if (formulirKegiatans == null || formulirKegiatans.isEmpty()) {
						Row row = new MyRowStyled();
						row.setParent(rowsFormulirKegiatan);
						Label a;
						row.appendChild(a = new Label("Tidak ada jadwal Kegiatan / Seminar"));
						a.setStyle("font-size:11px;font-weight: bolder;color:red;");
					} else {
						for (final FormulirKegiatan formulirKegiatan : formulirKegiatans) {
							Row row = new MyRowStyled();
							row.setParent(rowsFormulirKegiatan);
							Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(formulirKegiatan.infoSimple()
									+ " " + formulirKegiatan.getTahunAkademik() + "/" + formulirKegiatan.getSemester(),
									"/img/vendor.png");
							row.appendChild(toolbarbuttonData);
							toolbarbuttonData.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									TampilanELearningAction.prosess(formulirKegiatan, true);
								}
							});

							if (tampilMateri) {
								ProfileUtil.tampilkanMateri(rowsFormulirKegiatan, formulirKegiatan.ambilPertemuan());
							}
						}
					}

					if (formulirKegiatans != null && !formulirKegiatans.isEmpty()) {
						Row row = new MyRowStyled();
						rowsFormulirKegiatan.appendChild(row);

						Toolbarbutton a = new MyToolbarbuttonConfig("Tampilkan data selanjutnya..",
								"/img/Button-Next-icon.png");
						a.setStyle("font-size:11px;");
						row.appendChild(a);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								arg0.getTarget().getParent().setVisible(false);

								try {
									pagingFormulirKegiatan++;
									getThis().onEvent(null);
								} catch (Exception e) {
									MyMessageboxConfig.show(
											"Tidak ada " + jenisFormulirKegiatan.getNama()
													+ " selanjutnya yang bisa ditampilkan",
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								}
							}
						});
					}

					formulirKegiatans = null;
				}
			};

			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingFormulirKegiatan = 1;
					Common.clear(rowsFormulirKegiatan);
					rowEventFormulirKegiatan.onEvent(arg0);
				}
			});
			button.setParent(cariHbox);

			cariFormulirKegiatan.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingFormulirKegiatan = 1;
					Common.clear(rowsFormulirKegiatan);
					rowEventFormulirKegiatan.onEvent(arg0);
				}
			});

			rowEventFormulirKegiatan.onEvent(null);
		}

		if (adakosong > 0) {
			rows.appendChild(new ais.ui.util.MyGroupConfig(Common.getBahasaConfig("Kegiatan / Seminar")));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, Common.getBahasaConfig("Kegiatan / Seminar"), "Kegiatan atau seminar dosen ditampilkan agar riwayat aktivitas akademik dan non-akademik mudah terlihat.");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			cariHbox = new Hbox();
			cariHbox.setParent(row);
			cariHbox.appendChild(new Space());
			cariHbox.appendChild(new Space());
			final Textbox cariFormulirKegiatan = new Textbox();
			cariFormulirKegiatan.setCols(20);
			cariFormulirKegiatan.setParent(cariHbox);

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			final Rows rowsFormulirKegiatan = (Rows) Common.tampilanScroll1(row).getParent();

			final EventListener rowEventFormulirKegiatan = new EventListener() {

				public EventListener getThis() {
					return this;
				}

				@Override
				public void onEvent(Event arg0) throws Exception {

					Session session = HibernateUtil.currentSession();
					Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
							cariFormulirKegiatan.getValue().trim(), kelas, false, null, true, false, false,

							true, true, true, true, true,

							true, true, true, true,

							TampilanELearningAction.KEGIATAN,

							(pagingFormulirKegiatan - 1) * jumlahDataDalamSatuHalamanElearning,
							jumlahDataDalamSatuHalamanElearning, true);
					List<FormulirKegiatan> formulirKegiatans = (List<FormulirKegiatan>) objects[0];

					if (formulirKegiatans == null || formulirKegiatans.isEmpty()) {
						Row row = new MyRowStyled();
						row.setParent(rowsFormulirKegiatan);
						Label a;
						row.appendChild(a = new Label("Tidak ada jadwal Kegiatan / Seminar"));
						a.setStyle("font-size:11px;font-weight: bolder;color:red;");
					} else {
						for (final FormulirKegiatan formulirKegiatan : formulirKegiatans) {
							Row row = new MyRowStyled();
							row.setParent(rowsFormulirKegiatan);
							Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(formulirKegiatan.infoSimple()
									+ " " + formulirKegiatan.getTahunAkademik() + "/" + formulirKegiatan.getSemester(),
									"/img/vendor.png");
							row.appendChild(toolbarbuttonData);
							toolbarbuttonData.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									TampilanELearningAction.prosess(formulirKegiatan, true);
								}
							});

							if (tampilMateri) {
								ProfileUtil.tampilkanMateri(rowsFormulirKegiatan, formulirKegiatan.ambilPertemuan());
							}
						}
					}

					if (formulirKegiatans != null && !formulirKegiatans.isEmpty()) {
						Row row = new MyRowStyled();
						rowsFormulirKegiatan.appendChild(row);

						Toolbarbutton a = new MyToolbarbuttonConfig("Tampilkan kegiatan / seminar selanjutnya..",
								"/img/Button-Next-icon.png");
						a.setStyle("font-size:11px;");
						row.appendChild(a);
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								arg0.getTarget().getParent().setVisible(false);

								try {
									pagingFormulirKegiatan++;
									getThis().onEvent(null);
								} catch (Exception e) {
									MyMessageboxConfig.show(
											"Tidak ada kegiatan / seminar selanjutnya yang bisa ditampilkan",
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								}
							}
						});
					}

					formulirKegiatans = null;
				}
			};

			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingFormulirKegiatan = 1;
					Common.clear(rowsFormulirKegiatan);
					rowEventFormulirKegiatan.onEvent(arg0);
				}
			});
			button.setParent(cariHbox);

			cariFormulirKegiatan.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pagingFormulirKegiatan = 1;
					Common.clear(rowsFormulirKegiatan);
					rowEventFormulirKegiatan.onEvent(arg0);
				}
			});

			rowEventFormulirKegiatan.onEvent(null);
		}

		rows.appendChild(new ais.ui.util.MyGroupConfig("Bimbingan TA/Skripsi"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Bimbingan TA/Skripsi", "Mahasiswa tugas akhir atau skripsi bimbingan dosen ditampilkan agar proses bimbingan mudah dipantau.");

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		cariHbox = new Hbox();
		cariHbox.setParent(row);
		cariHbox.appendChild(new Space());
		cariHbox.appendChild(new Space());
		final Textbox cariMahasiswaRequestTugasAkhir = new Textbox();
		cariMahasiswaRequestTugasAkhir.setCols(20);
		cariMahasiswaRequestTugasAkhir.setParent(cariHbox);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Rows rowsMahasiswaRequestTugasAkhir = (Rows) Common.tampilanScroll1(row).getParent();

		final EventListener rowEventMahasiswaRequestTugasAkhir = new EventListener() {

			public EventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
						cariMahasiswaRequestTugasAkhir.getValue().trim(), kelas, false, null, true, false, false,

						true, true, true, true, true,

						true, true, true, true,

						TampilanELearningAction.BIMBINGAN,

						(pagingMahasiswaRequestTugasAkhir - 1) * jumlahDataDalamSatuHalamanElearning,
						jumlahDataDalamSatuHalamanElearning, true);
				List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = (List<MahasiswaRequestTugasAkhir>) objects[0];

				if (mahasiswaRequestTugasAkhirs == null || mahasiswaRequestTugasAkhirs.isEmpty()) {
					Row row = new MyRowStyled();
					row.setParent(rowsMahasiswaRequestTugasAkhir);
					Label a;
					row.appendChild(a = new Label("Tidak ada jadwal sebagai pembimbing TA/Skripsi"));
					a.setStyle("font-size:11px;font-weight: bolder;color:red;");
				} else {
					for (final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
						Row row = new MyRowStyled();
						row.setParent(rowsMahasiswaRequestTugasAkhir);
						Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(
								mahasiswaRequestTugasAkhir.infoSimple(), "/img/vendor.png");
						row.appendChild(toolbarbuttonData);
						toolbarbuttonData.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								TampilanELearningAction.prosess(mahasiswaRequestTugasAkhir, true);
							}
						});

						if (tampilMateri) {
							ProfileUtil.tampilkanMateri(rowsMahasiswaRequestTugasAkhir,
									mahasiswaRequestTugasAkhir.ambilPertemuan());
						}
					}
				}

				if (mahasiswaRequestTugasAkhirs != null && !mahasiswaRequestTugasAkhirs.isEmpty()) {
					Row row = new MyRowStyled();
					rowsMahasiswaRequestTugasAkhir.appendChild(row);

					Toolbarbutton a = new MyToolbarbuttonConfig("Tampilkan bimbingan TA/Skripsi selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							arg0.getTarget().getParent().setVisible(false);

							try {
								pagingMahasiswaRequestTugasAkhir++;
								getThis().onEvent(null);
							} catch (Exception e) {
								MyMessageboxConfig.show(
										"Tidak ada bimbingan TA/Skripsi selanjutnya yang bisa ditampilkan", "Informasi",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
				}

				mahasiswaRequestTugasAkhirs = null;
			}
		};

		button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingMahasiswaRequestTugasAkhir = 1;
				Common.clear(rowsMahasiswaRequestTugasAkhir);
				rowEventMahasiswaRequestTugasAkhir.onEvent(arg0);
			}
		});
		button.setParent(cariHbox);

		cariMahasiswaRequestTugasAkhir.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingMahasiswaRequestTugasAkhir = 1;
				Common.clear(rowsMahasiswaRequestTugasAkhir);
				rowEventMahasiswaRequestTugasAkhir.onEvent(arg0);
			}
		});

		rowEventMahasiswaRequestTugasAkhir.onEvent(null);

		rows.appendChild(new ais.ui.util.MyGroupConfig("Sidang TA/Skripsi"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Sidang TA/Skripsi", "Jadwal atau data sidang tugas akhir/skripsi membantu dosen melihat perannya sebagai penguji atau pembimbing.");

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		cariHbox = new Hbox();
		cariHbox.setParent(row);
		cariHbox.appendChild(new Space());
		cariHbox.appendChild(new Space());
		final Textbox cariSkripsi = new Textbox();
		cariSkripsi.setCols(20);
		cariSkripsi.setParent(cariHbox);

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		final Rows rowsSkripsi = (Rows) Common.tampilanScroll1(row).getParent();

		final EventListener rowEventSkripsi = new EventListener() {

			public EventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();
				Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
						cariSkripsi.getValue().trim(), kelas, false, null, true, false, false,

						true, true, true, true, true,

						true, true, true, true,

						TampilanELearningAction.SKRIPSI,

						(pagingSkripsi - 1) * jumlahDataDalamSatuHalamanElearning, jumlahDataDalamSatuHalamanElearning,
						true);
				List<Skripsi> skripsis = (List<Skripsi>) objects[0];

				if (skripsis == null || skripsis.isEmpty()) {
					Row row = new MyRowStyled();
					row.setParent(rowsSkripsi);
					Label a;
					row.appendChild(a = new Label("Tidak ada jadwal sebagai penguji sidang TA/Skripsi"));
					a.setStyle("font-size:11px;font-weight: bolder;color:red;");
				} else {
					for (final Skripsi skripsi : skripsis) {
						Row row = new MyRowStyled();
						row.setParent(rowsSkripsi);
						Toolbarbutton toolbarbuttonData = new MyToolbarbuttonConfig(skripsi.infoSimple(),
								"/img/vendor.png");
						row.appendChild(toolbarbuttonData);
						toolbarbuttonData.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								TampilanELearningAction.prosess(skripsi, true);
							}
						});

						if (tampilMateri) {
							ProfileUtil.tampilkanMateri(rowsSkripsi, skripsi.ambilPertemuan());
						}
					}
				}

				if (skripsis != null && !skripsis.isEmpty()) {
					Row row = new MyRowStyled();
					rowsSkripsi.appendChild(row);

					Toolbarbutton a = new MyToolbarbuttonConfig("Tampilkan penguji sidang TA/Skripsi selanjutnya..",
							"/img/Button-Next-icon.png");
					a.setStyle("font-size:11px;");
					row.appendChild(a);
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							arg0.getTarget().getParent().setVisible(false);

							try {
								pagingSkripsi++;
								getThis().onEvent(null);
							} catch (Exception e) {
								MyMessageboxConfig.show(
										"Tidak ada penguji sidang TA/Skripsi selanjutnya yang bisa ditampilkan",
										"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					});
				}

				skripsis = null;
			}
		};

		button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingSkripsi = 1;
				Common.clear(rowsSkripsi);
				rowEventSkripsi.onEvent(arg0);
			}
		});
		button.setParent(cariHbox);

		cariSkripsi.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pagingSkripsi = 1;
				Common.clear(rowsSkripsi);
				rowEventSkripsi.onEvent(arg0);
			}
		});

		rowEventSkripsi.onEvent(null);

		Toolbarbutton toolbarbuttonData;

		rows.appendChild(new ais.ui.util.MyGroupConfig("Kegiatan Dosen"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Kegiatan Dosen", "Aktivitas dosen di luar jadwal mengajar ditampilkan agar rekam jejak kegiatan terdokumentasi dengan baik.");

		List<Long> kegiatanKedosenans = dosen.ambilKegiatanKedosenanPunyaDosen();
		if (kegiatanKedosenans == null || kegiatanKedosenans.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan kegiatan dosen")));
			a.setStyle("font-size:11px;font-weight: bolder;color:red;");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			toolbarbuttonData = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Resume-icon.png");
			row.appendChild(toolbarbuttonData);
			toolbarbuttonData.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyWindow laporan = new MyWindow();
					laporan.addEventListener(Events.ON_CLOSE, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, tampilMateri);
						}
					});

					laporan.setTitle("Kegiatan Dosen");
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					DosenPunyaKegiatanKedosenanHelper detailperkuliahanHelper = new DosenPunyaKegiatanKedosenanHelper();
					detailperkuliahanHelper.display(dosen, laporan);

					laporan.onModal();
				}
			});

		} else {
			for (final Long id : kegiatanKedosenans) {
				KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) GeneralValueObject
						.ambilData(KegiatanKedosenanPunyaDosen.class, id.toString());
				if (kegiatanKedosenanPunyaDosen != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					toolbarbuttonData = new MyToolbarbuttonConfig(
							kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getNama() + " ("
									+ kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getTahunAkademik() + "/"
									+ kegiatanKedosenanPunyaDosen.getKegiatanKedosenan().getJenisSemester() + ")",
							"/img/Resume-icon.png");

					row.appendChild(toolbarbuttonData);

					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					Hbox hboxData = new Hbox();
					row.appendChild(hboxData);
					hboxData.appendChild(new Space());
					hboxData.appendChild(new Space());

					hboxData.appendChild(
							new Image(kegiatanKedosenanPunyaDosen.getPersetujuan() ? "/img/svg/check2-circle.svg"
									: "/img/cancel.gif"));
					hboxData.appendChild(new MyLabelKecil(
							kegiatanKedosenanPunyaDosen.getPersetujuan() ? "Disetujui" : "Belum Disetujui"));

					toolbarbuttonData.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) GeneralValueObject
									.ambilData(KegiatanKedosenanPunyaDosen.class, id.toString());

							MyWindow laporan = new MyWindow();
							laporan.addEventListener(Events.ON_CLOSE, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									init(parent, tampilMateri);
								}
							});
							laporan.setTitle("Kegiatan Dosen");
							laporan.setClosable(true);
							laporan.setHeight("95%");
							laporan.setWidth("90%");
							laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

							DosenPunyaKegiatanKedosenanHelper detailperkuliahanHelper = new DosenPunyaKegiatanKedosenanHelper();
							detailperkuliahanHelper.display(dosen, laporan, kegiatanKedosenanPunyaDosen);

							laporan.onModal();
						}
					});
				}
			}
		}
		kegiatanKedosenans = null;

		rows.appendChild(new ais.ui.util.MyGroupConfig("Organisasi Dosen"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Organisasi Dosen", "Riwayat organisasi dosen membantu melihat pengalaman kelembagaan dan peran dosen di organisasi.");

		List<Long> organisasiDosen = dosen.ambilOrganisasiDosenPunyaDosen();
		if (organisasiDosen == null || organisasiDosen.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan organisasi dosen")));
			a.setStyle("font-size:11px;font-weight: bolder;color:red;");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			toolbarbuttonData = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Resume-icon.png");
			row.appendChild(toolbarbuttonData);
			toolbarbuttonData.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyWindow laporan = new MyWindow();
					laporan.addEventListener(Events.ON_CLOSE, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, tampilMateri);
						}
					});
					laporan.setTitle("Organisasi Dosen");
					laporan.setClosable(true);
					laporan.setHeight("95%");
					laporan.setWidth("90%");
					laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

					DosenPunyaOrganisasiDosenHelper detailperkuliahanHelper = new DosenPunyaOrganisasiDosenHelper();
					detailperkuliahanHelper.display(dosen, laporan);

					laporan.onModal();
				}
			});

		} else {
			for (final Long id : organisasiDosen) {
				OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) GeneralValueObject
						.ambilData(OrganisasiDosenPunyaDosen.class, id.toString());
				if (organisasiDosenPunyaDosen != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					toolbarbuttonData = new MyToolbarbuttonConfig(
							organisasiDosenPunyaDosen.getOrganisasiDosen().getNama(), "/img/Resume-icon.png");

					row.appendChild(toolbarbuttonData);

					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					Hbox hboxData = new Hbox();
					row.appendChild(hboxData);
					hboxData.appendChild(new Space());
					hboxData.appendChild(new Space());
					hboxData.appendChild(
							new Image(organisasiDosenPunyaDosen.getPersetujuan() ? "/img/svg/check2-circle.svg"
									: "/img/cancel.gif"));
					hboxData.appendChild(new MyLabelKecil(
							organisasiDosenPunyaDosen.getPersetujuan() ? "Disetujui" : "Belum Disetujui"));

					toolbarbuttonData.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) GeneralValueObject
									.ambilData(OrganisasiDosenPunyaDosen.class, id.toString());

							MyWindow laporan = new MyWindow();
							laporan.addEventListener(Events.ON_CLOSE, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									init(parent, tampilMateri);
								}
							});
							laporan.setTitle("Organisasi Dosen");
							laporan.setClosable(true);
							laporan.setHeight("95%");
							laporan.setWidth("90%");
							laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

							DosenPunyaOrganisasiDosenHelper detailperkuliahanHelper = new DosenPunyaOrganisasiDosenHelper();
							detailperkuliahanHelper.display(dosen, laporan, organisasiDosenPunyaDosen);

							laporan.onModal();
						}
					});
				}
			}
		}
		organisasiDosen = null;

		rows.appendChild(new ais.ui.util.MyGroupConfig("Prestasi Dosen"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Prestasi Dosen", "Prestasi dosen yang sudah dicatat ditampilkan agar capaian lebih mudah ditemukan dan dilaporkan.");

		List<Long> prestasiDosens = dosen.ambilPrestasiDosen();
		if (prestasiDosens == null || prestasiDosens.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan prestasi dosen")));
			a.setStyle("font-size:11px;font-weight: bolder;color:red;");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			toolbarbuttonData = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Resume-icon.png");
			row.appendChild(toolbarbuttonData);
			toolbarbuttonData.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					String url = "/pages/master/prestasi_dosen.zul?dosen=" + dosen.getId();
					Common.displayWindow(url, false, "95%", "95%", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, tampilMateri);
						}
					}, "Prestasi Dosen");
				}
			});

		} else {
			for (final Long id : prestasiDosens) {
				PrestasiDosen prestasiDosen = (PrestasiDosen) GeneralValueObject.ambilData(PrestasiDosen.class,
						id.toString());
				if (prestasiDosen != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					toolbarbuttonData = new MyToolbarbuttonConfig(prestasiDosen.getNama(), "/img/Resume-icon.png");

					row.appendChild(toolbarbuttonData);

					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					Hbox hboxData = new Hbox();
					row.appendChild(hboxData);
					hboxData.appendChild(new Space());
					hboxData.appendChild(new Space());

					hboxData.appendChild(new Image(
							prestasiDosen.getStatus().equals(PrestasiDosen.DISETUJUI) ? "/img/svg/check2-circle.svg"
									: "/img/cancel.gif"));
					hboxData.appendChild(new MyLabelKecil(prestasiDosen.getStatus()));

					toolbarbuttonData.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							String url = "/pages/master/prestasi_dosen.zul?dosen=" + dosen.getId() + "&prestasi=" + id;
							Common.displayWindow(url, false, "95%", "95%", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									init(parent, tampilMateri);
								}
							}, "Prestasi Dosen");

						}
					});
				}
			}
		}
		prestasiDosens = null;

		rows.appendChild(new ais.ui.util.MyGroupConfig("Karya Dosen"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Karya Dosen", "Karya dan penghargaan dosen menjadi dokumentasi kontribusi akademik maupun profesional.");

		List<Long> penghargaanDosens = dosen.ambilPenghargaanDosen();
		if (penghargaanDosens == null || penghargaanDosens.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan karya dosen")));
			a.setStyle("font-size:11px;font-weight: bolder;color:red;");

			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			toolbarbuttonData = new MyToolbarbuttonConfig("Ajukan Baru", "/img/Resume-icon.png");
			row.appendChild(toolbarbuttonData);
			toolbarbuttonData.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					String url = "/pages/master/penghargaan_dosen.zul?dosen=" + dosen.getId();
					Common.displayWindow(url, false, "95%", "95%", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							init(parent, tampilMateri);
						}
					}, "Penghargaan Dosen");
				}
			});

		} else {
			for (final Long id : penghargaanDosens) {
				PenghargaanDosen penghargaanDosen = (PenghargaanDosen) GeneralValueObject
						.ambilData(PenghargaanDosen.class, id.toString());
				if (penghargaanDosen != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					toolbarbuttonData = new MyToolbarbuttonConfig(penghargaanDosen.getNama(), "/img/Resume-icon.png");

					row.appendChild(toolbarbuttonData);

					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");
					Hbox hboxData = new Hbox();
					row.appendChild(hboxData);
					hboxData.appendChild(new Space());
					hboxData.appendChild(new Space());

					hboxData.appendChild(new Image(penghargaanDosen.getStatus().equals(PenghargaanDosen.DISETUJUI)
							? "/img/svg/check2-circle.svg"
							: "/img/cancel.gif"));
					hboxData.appendChild(new MyLabelKecil(penghargaanDosen.getStatus()));

					toolbarbuttonData.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							String url = "/pages/master/penghargaan_dosen.zul?dosen=" + dosen.getId() + "&penghargaan="
									+ id;
							Common.displayWindow(url, false, "95%", "95%", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									init(parent, tampilMateri);
								}
							}, "Karya Dosen");

						}
					});
				}
			}
		}
		penghargaanDosens = null;

		rows.appendChild(new ais.ui.util.MyGroupConfig("Penelitian Dosen"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Penelitian Dosen", "Riwayat penelitian dan status pengajuan dosen ditampilkan agar lebih mudah dipantau.");

		List<Long> pengajuanPenelitianDanPengabdians = dosen
				.ambilPengajuanPenelitianDanPengabdian(ConstantValues.PENELITIAN);
		if (pengajuanPenelitianDanPengabdians == null || pengajuanPenelitianDanPengabdians.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan penelitian")));
			a.setStyle("font-size:11px;font-weight: bolder;color:red;");

		} else {
			for (final Long id : pengajuanPenelitianDanPengabdians) {
				PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = (PengajuanPenelitianDanPengabdian) GeneralValueObject
						.ambilData(PengajuanPenelitianDanPengabdian.class, id.toString());
				if (pengajuanPenelitianDanPengabdian != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					try {
						toolbarbuttonData = new MyToolbarbuttonConfig(pengajuanPenelitianDanPengabdian.getJudul() + " ("
								+ pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getJudul() + "/"
								+ pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getTahunAkademik() + "/"
								+ pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getSemester() + ")",
								"/img/Resume-icon.png");

						row.appendChild(toolbarbuttonData);

						row = new MyRowStyled();
						row.setParent(rows);
						ais.ui.util.ZkCompat.setSpans(row, "2");
						Hbox hboxData = new Hbox();
						row.appendChild(hboxData);
						hboxData.appendChild(new Space());
						hboxData.appendChild(new Space());

						hboxData.appendChild(new Image(pengajuanPenelitianDanPengabdian.getStatus()
								.equals(PengajuanPenelitianDanPengabdian.DISETUJUI) ? "/img/svg/check2-circle.svg"
										: "/img/cancel.gif"));
						hboxData.appendChild(new MyLabelKecil(pengajuanPenelitianDanPengabdian.getStatus()));

						toolbarbuttonData.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = (PengajuanPenelitianDanPengabdian) GeneralValueObject
										.ambilData(PengajuanPenelitianDanPengabdian.class, id.toString());
								PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper(
										PengumumanAkademis.UNTUK_DOSEN);

								MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
								pengajuanPenelitianDanPengabdianHelper.form(pengajuanPenelitianDanPengabdian,
										pengajuanPenelitianDanPengabdian.getDisposisiSop(), save, null);

							}
						});
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileDosen.java:1866");
					}
				}
			}

		}
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		toolbarbuttonData = new MyToolbarbuttonConfig("Pengajuan Penelitian", "/img/add_item.png");
		row.appendChild(toolbarbuttonData);
		toolbarbuttonData.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper(
						PengumumanAkademis.UNTUK_DOSEN);
				MyWindow addWindowPengajuan = new MyWindow();
				addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				addWindowPengajuan.setHeight("98%");
				addWindowPengajuan.setWidth("98%");
				addWindowPengajuan.setTitle("Penelitian Dosen");
				addWindowPengajuan.setClosable(true);
				addWindowPengajuan.setBorder("none");
				addWindowPengajuan.addEventListener(Events.ON_CLOSE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						init(parent, tampilMateri);
					}
				});
				pengajuanPenelitianDanPengabdianHelper.displayPengajuan(false, tbmuser.getUserId(),
						PengumumanAkademis.UNTUK_DOSEN, null, addWindowPengajuan, null, ConstantValues.PENELITIAN,
						"100%");
				addWindowPengajuan.onModal();
			}
		});
		pengajuanPenelitianDanPengabdians = null;

		rows.appendChild(new ais.ui.util.MyGroupConfig("Pengabdian Dosen"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Pengabdian Dosen", "Kegiatan pengabdian kepada masyarakat ditampilkan sebagai bagian dari rekam jejak tridarma dosen.");

		pengajuanPenelitianDanPengabdians = dosen.ambilPengajuanPenelitianDanPengabdian(ConstantValues.PENGABDIAN);
		if (pengajuanPenelitianDanPengabdians == null || pengajuanPenelitianDanPengabdians.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan pengabdian")));
			a.setStyle("font-size:11px;font-weight: bolder;color:red;");

		} else {
			for (final Long id : pengajuanPenelitianDanPengabdians) {
				PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = (PengajuanPenelitianDanPengabdian) GeneralValueObject
						.ambilData(PengajuanPenelitianDanPengabdian.class, id.toString());
				if (pengajuanPenelitianDanPengabdian != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					try {
						toolbarbuttonData = new MyToolbarbuttonConfig(pengajuanPenelitianDanPengabdian.getJudul() + " ("
								+ pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getJudul() + "/"
								+ pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getTahunAkademik() + "/"
								+ pengajuanPenelitianDanPengabdian.getPenelitianDanPengabdian().getSemester() + ")",
								"/img/Resume-icon.png");

						row.appendChild(toolbarbuttonData);

						row = new MyRowStyled();
						row.setParent(rows);
						ais.ui.util.ZkCompat.setSpans(row, "2");
						Hbox hboxData = new Hbox();
						row.appendChild(hboxData);
						hboxData.appendChild(new Space());
						hboxData.appendChild(new Space());

						hboxData.appendChild(new Image(pengajuanPenelitianDanPengabdian.getStatus()
								.equals(PengajuanPenelitianDanPengabdian.DISETUJUI) ? "/img/svg/check2-circle.svg"
										: "/img/cancel.gif"));
						hboxData.appendChild(new MyLabelKecil(pengajuanPenelitianDanPengabdian.getStatus()));

						toolbarbuttonData.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian = (PengajuanPenelitianDanPengabdian) GeneralValueObject
										.ambilData(PengajuanPenelitianDanPengabdian.class, id.toString());
								PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper(
										PengumumanAkademis.UNTUK_DOSEN);

								MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
								pengajuanPenelitianDanPengabdianHelper.form(pengajuanPenelitianDanPengabdian,
										pengajuanPenelitianDanPengabdian.getDisposisiSop(), save, null);

							}
						});
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileDosen.java:1966");
					}
				}
			}
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		toolbarbuttonData = new MyToolbarbuttonConfig("Pengajuan Pengabdian", "/img/add_item.png");
		row.appendChild(toolbarbuttonData);
		toolbarbuttonData.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				PengajuanPenelitianDanPengabdianHelper pengajuanPenelitianDanPengabdianHelper = new PengajuanPenelitianDanPengabdianHelper(
						PengumumanAkademis.UNTUK_DOSEN);
				MyWindow addWindowPengajuan = new MyWindow();
				addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				addWindowPengajuan.setHeight("98%");
				addWindowPengajuan.setWidth("98%");
				addWindowPengajuan.setTitle("Pengabdian Dosen");
				addWindowPengajuan.setClosable(true);
				addWindowPengajuan.setBorder("none");

				addWindowPengajuan.addEventListener(Events.ON_CLOSE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						init(parent, tampilMateri);
					}
				});

				pengajuanPenelitianDanPengabdianHelper.displayPengajuan(false, tbmuser.getUserId(),
						PengumumanAkademis.UNTUK_DOSEN, null, addWindowPengajuan, null, ConstantValues.PENGABDIAN,
						"100%");
				addWindowPengajuan.onModal();
			}
		});

		pengajuanPenelitianDanPengabdians = null;

		rows.appendChild(new ais.ui.util.MyGroupConfig("Publikasi Dosen"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Publikasi Dosen", "Artikel atau publikasi dosen ditampilkan agar keluaran ilmiah mudah dilihat dan dilaporkan.");

		List<Long> artikels = dosen.ambilArtikel();
		if (artikels == null || artikels.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan publikasi")));
			a.setStyle("font-size:11px;font-weight: bolder;color:red;");

		} else {
			for (final Long id : artikels) {
				Artikel artikel = (Artikel) GeneralValueObject.ambilData(Artikel.class, id.toString());
				if (artikel != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					try {
						toolbarbuttonData = new MyToolbarbuttonConfig(
								artikel.getJudul() + " ("
										+ (artikel.getJurnalPenelitian() == null ? ""
												: artikel.getJurnalPenelitian().getJudul() + "/")
										+ artikel.getTahunAkademik() + "/" + artikel.getSemester() + ")",
								"/img/Resume-icon.png");

						row.appendChild(toolbarbuttonData);

						row = new MyRowStyled();
						row.setParent(rows);
						ais.ui.util.ZkCompat.setSpans(row, "2");
						Hbox hboxData = new Hbox();
						row.appendChild(hboxData);
						hboxData.appendChild(new Space());
						hboxData.appendChild(new Space());

						hboxData.appendChild(
								new Image(artikel.getStatus().equals(Artikel.DISETUJUI) ? "/img/svg/check2-circle.svg"
										: "/img/cancel.gif"));
						hboxData.appendChild(new MyLabelKecil(artikel.getStatus()));

						toolbarbuttonData.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Artikel artikel = (Artikel) GeneralValueObject.ambilData(Artikel.class, id.toString());
								DetailArtikelHelper detailArtikelHelper = new DetailArtikelHelper(dosen);
								detailArtikelHelper.diperuntukkanPengajuan = PengumumanAkademis.UNTUK_DOSEN;
								MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
								detailArtikelHelper.form(artikel, artikel.getDisposisiSop(), save, null);
							}
						});
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileDosen.java:2065");
					}
				}
			}
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		toolbarbuttonData = new MyToolbarbuttonConfig("Pengajuan Publikasi", "/img/add_item.png");
		row.appendChild(toolbarbuttonData);
		toolbarbuttonData.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				DetailArtikelHelper detailArtikelHelper = new DetailArtikelHelper(dosen);
				MyWindow addWindowPengajuan = new MyWindow();
				addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				addWindowPengajuan.setHeight("98%");
				addWindowPengajuan.setWidth("98%");
				addWindowPengajuan.setTitle("Publikasi Dosen");
				addWindowPengajuan.setClosable(true);
				addWindowPengajuan.setBorder("none");

				addWindowPengajuan.addEventListener(Events.ON_CLOSE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						init(parent, tampilMateri);
					}
				});

				detailArtikelHelper.displayPengajuan(false, tbmuser.getUserId(), PengumumanAkademis.UNTUK_DOSEN, null,
						addWindowPengajuan, addWindowPengajuan, "99%");
				addWindowPengajuan.onModal();
			}
		});

		artikels = null;

		rows.appendChild(new ais.ui.util.MyGroupConfig("Buku Dosen"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Buku Dosen", "Buku atau bahan ajar dosen menjadi dokumentasi materi pembelajaran dan karya akademik.");

		List<Long> bukuBahanAjars = dosen.ambilBukuBahanAjar();
		if (bukuBahanAjars == null || bukuBahanAjars.isEmpty()) {
			row = new MyRowStyled();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			Label a;
			row.appendChild(a = new Label(ais.common.Common.getBahasaConfig("Anda belum mengajukan buku")));
			a.setStyle("font-size:11px;font-weight: bolder;color:red;");

		} else {
			for (final Long id : bukuBahanAjars) {
				BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) GeneralValueObject.ambilData(BukuBahanAjar.class,
						id.toString());
				if (bukuBahanAjar != null) {
					row = new MyRowStyled();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					try {
						toolbarbuttonData = new MyToolbarbuttonConfig(
								bukuBahanAjar.getNama() + " ("
										+ (bukuBahanAjar.getTahapanPenyusunanBuku() == null ? ""
												: bukuBahanAjar.getTahapanPenyusunanBuku().getNama() + "/")
										+ bukuBahanAjar.getTahunAkademik() + "/" + bukuBahanAjar.getSemester() + ")",
								"/img/Resume-icon.png");

						row.appendChild(toolbarbuttonData);

						row = new MyRowStyled();
						row.setParent(rows);
						ais.ui.util.ZkCompat.setSpans(row, "2");
						Hbox hboxData = new Hbox();
						row.appendChild(hboxData);
						hboxData.appendChild(new Space());
						hboxData.appendChild(new Space());

						toolbarbuttonData.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) GeneralValueObject
										.ambilData(BukuBahanAjar.class, id.toString());
								BukuBahanAjarAction.onAddExternal(arg0, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										init(parent, tampilMateri);
									}
								}, bukuBahanAjar);
							}
						});
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/profile/ProfileDosen.java:2162");
					}
				}
			}
		}

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");

		toolbarbuttonData = new MyToolbarbuttonConfig("Pengajuan Buku", "/img/add_item.png");
		row.appendChild(toolbarbuttonData);
		toolbarbuttonData.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyWindow addWindowPengajuan = new MyWindow();
				addWindowPengajuan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				addWindowPengajuan.setHeight("98%");
				addWindowPengajuan.setWidth("98%");
				addWindowPengajuan.setTitle("Buku Dosen");
				addWindowPengajuan.setClosable(true);
				addWindowPengajuan.setBorder("none");

				addWindowPengajuan.addEventListener(Events.ON_CLOSE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						init(parent, tampilMateri);
					}
				});

				MyInclude iframe = new MyInclude("/pages/master/buku_bahan_ajar.zul?dosen=" + dosen.getId());
				iframe.setParent(addWindowPengajuan);
				addWindowPengajuan.onModal();
			}
		});

		bukuBahanAjars = null;

		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
		row = new MyRowStyled();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Space());
	}

}
