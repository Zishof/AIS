package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataArtikelBanyak;
import ais.action.master.library.helper.AmbilDataDariGoogleScholarBanyak;
import ais.action.master.penelitiandanpengabdian.ArtikelAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.ScholarArticle;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper generik "Daftar Buku Referensi/Artikel" yang dapat ditautkan ke berbagai jenis
 * entitas pemilik (owner) sekaligus — {@link Skripsi}, {@link MahasiswaRequestTugasAkhir},
 * {@link JadwalUjianPMB}, {@link KelompokKkn}, {@link KelompokPkl}, {@link Perkuliahan},
 * {@link KurikulumPunyaMatakuliah}, {@link JadwalPelajaran} — lewat baris relasi
 * {@link DataPunyaArtikel} yang memiliki satu kolom foreign key per jenis owner (hanya
 * kolom yang relevan yang diisi tidak-null pada satu baris). Menyediakan pencarian
 * (judul/kata kunci/abstrak {@link Artikel}), pengambilan artikel dari katalog internal
 * ({@link AmbilDataArtikelBanyak}) atau dari Google Scholar ({@link
 * AmbilDataDariGoogleScholarBanyak}, tombol saat ini disembunyikan/{@code setVisible(false)}
 * pada varian tab), tampilan kutipan, dan penghapusan.
 *
 * <p>
 * Method {@link #display} memiliki dua mode tata letak yang hampir identik isinya
 * (duplikasi toolbar/tombol yang disengaja): sebagai isi {@link Tabpanel} (label tab
 * diperbarui otomatis dengan jumlah data) atau sebagai isi region {@link Center} pada
 * {@link org.zkoss.zul.Borderlayout} (toolbar dipasang di {@code North}, paging di
 * {@code South} milik parent). Parameter {@code sqltambahan} pada varian
 * {@link #display(String, Component)} memungkinkan pemanggil menambahkan kondisi SQL
 * kustom (mis. untuk owner yang belum punya kolom foreign key khusus) ke kriteria
 * pencarian OR standar.
 * </p>
 */
public class DataPunyaArtikelHelper implements DataLoader {

	private Grid grid;
	private Skripsi skripsi;

	private Component component;

	private Paging paging;

	private Tbmuser tbmuser;
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private JadwalUjianPMB jadwalUjianPMB;
	private KelompokKkn kelompokKkn;
	private KelompokPkl kelompokPkl;
	private Perkuliahan perkuliahan;
	private String sqltambahan = "false";
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private JadwalPelajaran jadwalPelajaran;
	private Textbox cari;

	/** Membuat helper dan mengambil pengguna yang sedang login ke {@link #tbmuser} (dipakai untuk kontrol visibilitas tombol). */
	public DataPunyaArtikelHelper() {

		tbmuser = Common.getCurrentUser();
	}

	/** Perender baris grid: detail artikel (via {@link DetailArtikelHelper#displayRow}) plus tombol Kutipan dan Hapus. */
	class DetailSkripsiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris {@link DataPunyaArtikel}: detail {@link Artikel} terkait
		 * lewat {@link DetailArtikelHelper#displayRow}, tombol "Kutipan" (menampilkan
		 * format kutipan via {@link ArtikelAction#tampilkanKutipan}), dan tombol hapus
		 * (terlihat hanya bila ada pengguna login, dengan konfirmasi dan pesan galat
		 * ramah bila gagal karena relasi data).
		 */
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final DataPunyaArtikel dataPunyaArtikel = (DataPunyaArtikel) data;
			final Artikel artikel = dataPunyaArtikel.getArtikel();

			DetailArtikelHelper.displayRow(arg0, artikel, null, false);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kutipan", "/img/eye-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Kutipan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ArtikelAction.tampilkanKutipan(artikel);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(tbmuser != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(dataPunyaArtikel);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);

		}

	}

	/**
	 * Membangun kriteria pencarian {@link DataPunyaArtikel} yang tertaut ke SALAH SATU
	 * owner yang sedang aktif pada helper ini ({@link #jadwalPelajaran}/{@link #perkuliahan}/
	 * {@link #kurikulumPunyaMatakuliah}/{@link #kelompokPkl}/{@link #kelompokKkn}/
	 * {@link #mahasiswaRequestTugasAkhir}/{@link #skripsi}/{@link #jadwalUjianPMB}, atau
	 * {@link #sqltambahan} sebagai kondisi SQL tambahan), disaring opsional oleh kata
	 * kunci pencarian pada judul/kata kunci/abstrak artikel.
	 *
	 * @param order tambahkan pengurutan berdasarkan id menaik bila {@code true}
	 * @return kriteria Hibernate atas {@link DataPunyaArtikel}
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(DataPunyaArtikel.class).createAlias("artikel", "artikel")
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("artikel.judul", cari.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("artikel.keyword", cari.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("artikel.abstrak", cari.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(Restrictions.or(Restrictions.eq("jadwalPelajaran", jadwalPelajaran), Restrictions.or(
						Restrictions.eq("perkuliahan", perkuliahan),
						Restrictions.or(Restrictions.sqlRestriction(sqltambahan), Restrictions.or(
								Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah), Restrictions.or(
										Restrictions.eq("kelompokPkl", kelompokPkl),
										Restrictions.or(Restrictions.eq("kelompokKkn", kelompokKkn),
												Restrictions.or(
														Restrictions.or(
																Restrictions.eq("mahasiswaRequestTugasAkhir",
																		mahasiswaRequestTugasAkhir),
																Restrictions.eq("skripsi", skripsi)),
														Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB)))))))));

		if (order) {
			crit.addOrder(Order.asc("id"));
		}
		return crit;
	}

	/**
	 * Memuat ulang daftar {@link DataPunyaArtikel} sesuai {@link #initCriteria(boolean)}
	 * dengan paging standar, dan bila kontainer tampilan berupa {@link Tabpanel},
	 * memperbarui label tab dengan jumlah data ("Artikel (N)").
	 *
	 * @param value tidak digunakan (parameter kontrak {@link DataLoader})
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.initPaging(initCriteria(false), paging);

		List<DataPunyaArtikel> dataPunyaArtikel = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		if (component instanceof Tabpanel) {
			((Tabpanel) component).getLinkedTab()
					.setLabel("Artikel " + (dataPunyaArtikel.size() == 0 ? "" : "(" + dataPunyaArtikel.size() + ")"));
		}

		ListModel strset = new SimpleListModel(dataPunyaArtikel);
		grid.setRowRenderer(new DetailSkripsiRenderer());
		grid.setModel(strset);

	}

	/**
	 * Seperti {@link #display(Skripsi, MahasiswaRequestTugasAkhir, JadwalUjianPMB, KelompokKkn, KelompokPkl, Perkuliahan, KurikulumPunyaMatakuliah, Component)}
	 * memakai owner yang sudah tersimpan di field instance (biasanya semuanya {@code null}
	 * bila dipanggil sebelum owner lain diset), dengan tambahan {@code sqltambahan} —
	 * kondisi SQL kustom untuk mencakup owner yang belum punya kolom foreign key khusus
	 * pada {@link DataPunyaArtikel}.
	 *
	 * @param sqltambahan kondisi SQL tambahan yang di-OR-kan ke kriteria owner standar
	 * @param component   kontainer ZK yang akan diisi (dibersihkan lebih dulu)
	 */
	public void display(final String sqltambahan, final Component component) {
		this.sqltambahan = sqltambahan;
		display(skripsi, mahasiswaRequestTugasAkhir, jadwalUjianPMB, kelompokKkn, kelompokPkl, perkuliahan,
				kurikulumPunyaMatakuliah, component);
	}

	/** Seperti {@link #display(Skripsi, MahasiswaRequestTugasAkhir, JadwalUjianPMB, KelompokKkn, KelompokPkl, Perkuliahan, KurikulumPunyaMatakuliah, JadwalPelajaran, Component)} tanpa owner {@link JadwalPelajaran}. */
	public void display(final Skripsi skripsi, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final JadwalUjianPMB jadwalUjianPMB, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final Perkuliahan perkuliahan, final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final Component component) {
		display(skripsi, mahasiswaRequestTugasAkhir, jadwalUjianPMB, kelompokKkn, kelompokPkl, perkuliahan,
				kurikulumPunyaMatakuliah, null, component);
	}

	/**
	 * Membangun tampilan "Daftar Buku Referensi" untuk kombinasi owner yang diberikan
	 * (tepat satu owner biasanya terisi non-null; sisanya {@code null}) ke dalam
	 * {@code component}. Tata letak menyesuaikan tipe {@code component}: bila
	 * {@link Tabpanel}, dibangun sebagai groupbox dengan toolbar+grid+paging langsung
	 * di dalamnya (label tab diperbarui dengan jumlah data); bila {@link Center} pada
	 * borderlayout, toolbar dipasang ke region {@code North} dan paging ke
	 * {@code South} milik parent {@code component}; selain itu, grid dipasang langsung
	 * ke {@code component} tanpa toolbar/paging tambahan. Toolbar (bila ada) berisi
	 * "Ambil Artikel" (terlihat untuk staf non-mahasiswa, membuka
	 * {@link AmbilDataArtikelBanyak} berisi artikel yang sudah dipakai owner-owner
	 * sejenis sebagai referensi cepat), "Ambil Artikel dari Google Scholar" (membuka
	 * {@link AmbilDataDariGoogleScholarBanyak}, membuat {@link Artikel} baru dari
	 * {@link ScholarArticle} bila belum ada), dan kotak pencarian.
	 *
	 * @param skripsi                    owner skripsi, boleh {@code null}
	 * @param mahasiswaRequestTugasAkhir owner permintaan tugas akhir, boleh {@code null}
	 * @param jadwalUjianPMB             owner jadwal ujian PMB, boleh {@code null}
	 * @param kelompokKkn                owner kelompok KKN, boleh {@code null}
	 * @param kelompokPkl                owner kelompok PKL, boleh {@code null}
	 * @param perkuliahan                owner perkuliahan, boleh {@code null}
	 * @param kurikulumPunyaMatakuliah   owner mata kuliah kurikulum, boleh {@code null}
	 * @param jadwalPelajaran            owner jadwal pelajaran (sekolah), boleh {@code null}
	 * @param component                  kontainer ZK yang akan diisi (dibersihkan lebih dulu bila tidak {@code null})
	 */
	public void display(final Skripsi skripsi, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final JadwalUjianPMB jadwalUjianPMB, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final Perkuliahan perkuliahan, final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final JadwalPelajaran jadwalPelajaran, final Component component) {
		this.skripsi = skripsi;
		this.kelompokKkn = kelompokKkn;
		this.kelompokPkl = kelompokPkl;
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
		this.jadwalUjianPMB = jadwalUjianPMB;
		this.perkuliahan = perkuliahan;
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliah;
		this.jadwalPelajaran = jadwalPelajaran;
		if (component != null) {
			Common.clear(component);
		}
		this.component = component;

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		cari = new Textbox();
		if (component instanceof Tabpanel) {
			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(component);
			groupbox.appendChild(new MyCaptionStyled("Daftar Buku Referensi"));

			Toolbar toolbar = new Toolbar();

			// toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Artikel", "/img/new.gif");
			button.setVisible(tbmuser != null && Common.getCurrentUser().getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<Artikel> artikels = HibernateUtil.currentSession().createCriteria(DataPunyaArtikel.class)
							.add(Restrictions.or(Restrictions.eq("perkuliahan", perkuliahan),
									Restrictions.or(Restrictions.eq("kelompokPkl", kelompokPkl),
											Restrictions.or(Restrictions.eq("kelompokKkn", kelompokKkn),
													Restrictions.or(
															Restrictions.or(
																	Restrictions.eq("mahasiswaRequestTugasAkhir",
																			mahasiswaRequestTugasAkhir),
																	Restrictions.eq("skripsi", skripsi)),
															Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB))))))
							.setProjection(Projections.property("artikel")).list();
					AmbilDataArtikelBanyak ambilDataArtikelBanyak = new AmbilDataArtikelBanyak(artikels);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataArtikelBanyak);
					ambilDataArtikelBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Artikel> artikels = (List<Artikel>) arg0.getData();
							for (Artikel artikel : artikels) {
								DataPunyaArtikel dataPunyaArtikel = new DataPunyaArtikel();
								dataPunyaArtikel.setArtikel(artikel);
								dataPunyaArtikel.setKeterangan("");
								dataPunyaArtikel.setJadwalUjianPMB(jadwalUjianPMB);
								dataPunyaArtikel.setSkripsi(skripsi);
								dataPunyaArtikel.setKelompokKkn(kelompokKkn);
								dataPunyaArtikel.setPerkuliahan(perkuliahan);
								dataPunyaArtikel.setKelompokPkl(kelompokPkl);
								dataPunyaArtikel.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
								dataPunyaArtikel.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
								dataPunyaArtikel.setJadwalPelajaran(jadwalPelajaran);
								Common.refreshSaveOrUpdate(dataPunyaArtikel);
							}

							loadData(null);
						}
					});
					ambilDataArtikelBanyak.setWidth("97%");
					ambilDataArtikelBanyak.setHeight("97%");
					ambilDataArtikelBanyak.setVisible(true);
					ambilDataArtikelBanyak.onModal();

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Ambil Artikel dari Google Scholar",
					"/img/education-university-icon.png");

			button.setVisible(false);

			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataDariGoogleScholarBanyak ambilDataDariGoogleScholarBanyak = new AmbilDataDariGoogleScholarBanyak(
							perkuliahan != null && perkuliahan.getMatakuliah() != null
									? perkuliahan.getMatakuliah().getNama() : "");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataDariGoogleScholarBanyak);
					ambilDataDariGoogleScholarBanyak.setHeight("95%");
					ambilDataDariGoogleScholarBanyak.setWidth("90%");


					ambilDataDariGoogleScholarBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<ScholarArticle> objects = (List<ScholarArticle>) arg0.getData();
							Session session = HibernateUtil.currentSession();
							for (ScholarArticle scholarArticle : objects) {

								try {

									Artikel artikel = (Artikel) session.createCriteria(Artikel.class)
											.add(Restrictions.eq("scholarArticle", scholarArticle)).uniqueResult();
									if (artikel == null) {
										artikel = new Artikel();
										artikel.setScholarArticle(scholarArticle);
										session.save(artikel);
									}

									DataPunyaArtikel dataPunyaArtikel = new DataPunyaArtikel();
									dataPunyaArtikel.setArtikel(artikel);
									dataPunyaArtikel.setKeterangan("");
									dataPunyaArtikel.setJadwalUjianPMB(jadwalUjianPMB);
									dataPunyaArtikel.setSkripsi(skripsi);
									dataPunyaArtikel.setKelompokKkn(kelompokKkn);
									dataPunyaArtikel.setPerkuliahan(perkuliahan);
									dataPunyaArtikel.setKelompokPkl(kelompokPkl);
									dataPunyaArtikel.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
									dataPunyaArtikel.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
									dataPunyaArtikel.setJadwalPelajaran(jadwalPelajaran);
									Common.refreshSaveOrUpdate(dataPunyaArtikel);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);

								}

							}

							loadData(null);
						}
					});

					ambilDataDariGoogleScholarBanyak.onModal();

				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(new Space());
			toolbar.appendChild(new MyLabelConfig("Cari : "));
			toolbar.appendChild(cari);
			cari.setCols(15);
			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			grid = new Grid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setParent(groupbox);

			paging.setParent(groupbox);
		} else {
			grid = new Grid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setParent(component);

			if (component instanceof Center) {
				South south = new South();
				south.setParent(component.getParent());
				paging.setParent(south);

				North north = new North();
				north.setParent(component.getParent());
				ais.ui.util.ZkCompat.setFlex(north, true);
				north.setHeight("25px");

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(north);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Artikel", "/img/new.gif");
				button.setVisible(tbmuser != null && Common.getCurrentUser().getMahasiswa() == null);
				button.addEventListener("onClick", new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						List<Artikel> artikels = HibernateUtil.currentSession().createCriteria(DataPunyaArtikel.class)
								.add(Restrictions.or(Restrictions.eq("perkuliahan", perkuliahan), Restrictions.or(
										Restrictions.eq("kelompokPkl", kelompokPkl), Restrictions
												.or(Restrictions.eq("kelompokKkn", kelompokKkn), Restrictions.or(
														Restrictions.or(
																Restrictions.eq("mahasiswaRequestTugasAkhir",
																		mahasiswaRequestTugasAkhir),
																Restrictions.eq("skripsi", skripsi)),
														Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB))))))
								.setProjection(Projections.property("artikel")).list();
						AmbilDataArtikelBanyak ambilDataArtikelBanyak = new AmbilDataArtikelBanyak(artikels);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilDataArtikelBanyak);
						ambilDataArtikelBanyak.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Artikel> artikels = (List<Artikel>) arg0.getData();
								for (Artikel artikel : artikels) {
									DataPunyaArtikel dataPunyaArtikel = new DataPunyaArtikel();
									dataPunyaArtikel.setArtikel(artikel);
									dataPunyaArtikel.setKeterangan("");
									dataPunyaArtikel.setJadwalUjianPMB(jadwalUjianPMB);
									dataPunyaArtikel.setSkripsi(skripsi);
									dataPunyaArtikel.setKelompokKkn(kelompokKkn);
									dataPunyaArtikel.setPerkuliahan(perkuliahan);
									dataPunyaArtikel.setKelompokPkl(kelompokPkl);
									dataPunyaArtikel.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
									dataPunyaArtikel.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
									dataPunyaArtikel.setJadwalPelajaran(jadwalPelajaran);
									Common.refreshSaveOrUpdate(dataPunyaArtikel);
								}

								loadData(null);
							}
						});
						ambilDataArtikelBanyak.setWidth("97%");
						ambilDataArtikelBanyak.setHeight("97%");
						ambilDataArtikelBanyak.setVisible(true);
						ambilDataArtikelBanyak.onModal();

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("Ambil Artikel dari Google Scholar",
						"/img/education-university-icon.png");
				button.addEventListener("onClick", new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						AmbilDataDariGoogleScholarBanyak ambilDataDariGoogleScholarBanyak = new AmbilDataDariGoogleScholarBanyak(
								perkuliahan != null && perkuliahan.getMatakuliah() != null
										? perkuliahan.getMatakuliah().getNama() : "");
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilDataDariGoogleScholarBanyak);
						ambilDataDariGoogleScholarBanyak.setHeight("95%");
						ambilDataDariGoogleScholarBanyak.setWidth("90%");

						ambilDataDariGoogleScholarBanyak.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								List<ScholarArticle> objects = (List<ScholarArticle>) arg0.getData();
								Session session = HibernateUtil.currentSession();
								for (ScholarArticle scholarArticle : objects) {

									try {

										Artikel artikel = (Artikel) session.createCriteria(Artikel.class)
												.add(Restrictions.eq("scholarArticle", scholarArticle)).uniqueResult();
										if (artikel == null) {
											artikel = new Artikel();
											artikel.setScholarArticle(scholarArticle);
											session.save(artikel);
										}

										DataPunyaArtikel dataPunyaArtikel = new DataPunyaArtikel();
										dataPunyaArtikel.setArtikel(artikel);
										dataPunyaArtikel.setKeterangan("");
										dataPunyaArtikel.setJadwalUjianPMB(jadwalUjianPMB);
										dataPunyaArtikel.setSkripsi(skripsi);
										dataPunyaArtikel.setKelompokKkn(kelompokKkn);
										dataPunyaArtikel.setPerkuliahan(perkuliahan);
										dataPunyaArtikel.setKelompokPkl(kelompokPkl);
										dataPunyaArtikel.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
										dataPunyaArtikel.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
										dataPunyaArtikel.setJadwalPelajaran(jadwalPelajaran);
										Common.refreshSaveOrUpdate(dataPunyaArtikel);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);

									}

								}

								loadData(null);
							}
						});

						ambilDataDariGoogleScholarBanyak.onModal();

					}

				});
				button.setParent(toolbar);

				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelConfig("Cari : "));
				toolbar.appendChild(cari);
				cari.setCols(15);
				cari.addEventListener("onOK", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});
				button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						loadData(null);
					}
				});
				button.setParent(toolbar);
			}
		}

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Rincian");
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Abstrak");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Informasi");
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

}
