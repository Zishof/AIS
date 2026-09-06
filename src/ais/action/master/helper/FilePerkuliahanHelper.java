package ais.action.master.helper;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import com.google.api.services.books.model.Volume;

import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.generic.AmbilDataPertemuanFileContent;
import ais.action.master.library.helper.AmbilDataDariGoogleBookBanyak;
import ais.action.master.library.helper.AmbilDataDariGoogleScholarBanyak;
import ais.action.servlet.CheckISBN;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GrupPertemuan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.ScholarArticle;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.library.Item;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper UI e-learning untuk menampilkan dan mengelola <b>materi/berkas pertemuan</b> (entity
 * {@link PertemuanFileContent}) pada satu {@link Pertemuan} (sesi kuliah), atau — bila {@code pertemuan} tidak
 * diketahui — pada kombinasi {@link KurikulumPunyaMatakuliahDetail}/{@link GrupPertemuan}/
 * {@link KurikulumPunyaMatakuliah}. Dipakai baik oleh dosen/admin (mode kelola: unggah, hapus, ubah keterangan)
 * maupun oleh mahasiswa/calon mahasiswa/siswa/peserta kursus (mode baca saja).
 *
 * <p><b>Sumber materi yang didukung</b> (dibangun lewat {@code AmbilDataPertemuanFileContent} dan dirender oleh
 * {@link #createFile}): unggah/scan foto, scan layar, paket SCORM, salin dari pertemuan lain ("Upload/Ambil
 * Materi Sebelumnya"), Google Drive, Dropbox, tautan (link) bebas, buku dari Google Books, dan artikel dari
 * Google Scholar (tombol ini disembunyikan permanen — {@code setVisible(false)}). Setiap sumber memicu
 * {@link Pertemuan#belum(String) pertemuan.belum("pertemuan_file_content")} (menandai cache pertemuan basi) lalu
 * {@link #reloadPertemuanFileContent()} untuk memuat ulang daftar dan mengirim email notifikasi
 * ({@code CommonEmail.infoAdaFilePerkuliahan}).</p>
 * <p><b>Tampilan:</b> {@link #createFile} membangun toolbar sumber materi di {@code North} dan daftar materi di
 * {@code Center} ({@link #center}). Bila hanya satu materi, kontennya langsung dirender di center; bila lebih
 * dari satu, setiap materi menjadi satu {@code Tab} (lazy-render kontennya baru dibangun saat tab pertama kali
 * diklik, lewat {@link #tampilkanKonten}), memilih materi yang cocok {@link #selectedPertemuanFileContent} atau
 * tab pertama sebagai default. {@link #tampilkanKonten} juga merender syarat/ketentuan terkait materi
 * ({@code Tugas.tampilanSyarat}/{@code tampilanSyaratReadonly}) dan tombol Hapus (soft-unlink: kolom
 * pertemuan/kurikulumpunyamatakuliah/kurikulumpunyamatakuliahdetail/gruppertemuan diarahkan ke id sentinel
 * negatif via SQL langsung dengan commit eksplisit, bukan menghapus baris — lihat komentar KE-FIX di kode
 * tentang bug rollback StreamingHibernateUtil yang pernah membuat penghapusan tidak persist).</p>
 */
public class FilePerkuliahanHelper {

	/** Mahasiswa yang sedang melihat materi (mode baca saja); {@code null} bila pemanggil adalah dosen/admin. */
	private Mahasiswa mahasiswa;
	/** Biodata calon mahasiswa yang sedang melihat materi (mode baca saja, jalur PMB); {@code null} bila bukan konteks ini. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	// private MyGrid uploadGrid;
	/** Pertemuan (sesi kuliah) yang materinya sedang ditampilkan; boleh {@code null} bila konteksnya bukan per-pertemuan. */
	private Pertemuan pertemuan;
	/** Kurikulum-punya-matakuliah terkait, dipakai sebagai filter/atribusi materi bila {@link #pertemuan} kosong. */
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	/** Detail kurikulum-punya-matakuliah (topik/pertemuan RPS) yang materinya ditampilkan. */
	private KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail;
	/** Grup pertemuan (untuk kelas dengan beberapa grup paralel) yang materinya ditampilkan. */
	private GrupPertemuan grupPertemuan;
	/** Komponen ZK {@code Center} tempat daftar/konten materi dirender ulang oleh {@link #reloadPertemuanFileContent}. */
	private Center center;
	/** Materi yang harus otomatis terpilih/terbuka saat tab dibangun (mis. dibuka dari notifikasi tertentu). */
	private PertemuanFileContent selectedPertemuanFileContent;

	/**
	 * Membuat helper untuk konteks mahasiswa/calon mahasiswa tertentu. Bila keduanya {@code null}, helper
	 * berjalan dalam mode dosen/admin (toolbar unggah materi ditampilkan, tombol Hapus aktif).
	 *
	 * @param mahasiswa mahasiswa pemilik konteks (mode baca saja); {@code null} bila bukan mahasiswa
	 * @param biodataCalonMahasiswa biodata calon mahasiswa pemilik konteks (mode baca saja, jalur PMB); {@code null} bila bukan konteks ini
	 */
	public FilePerkuliahanHelper(final Mahasiswa mahasiswa, final BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;

	}

	/**
	 * Titik masuk utama: membangun tab "Materi &amp; Berkas Pertemuan" di {@code tabpanelFilePertemuan} —
	 * header modul, toolbar sumber materi (scan foto/layar, SCORM, salin dari pertemuan lain, Google Drive,
	 * Dropbox, tautan, Google Books, tombol Refresh), lalu memuat daftar materi.
	 *
	 * <p>Bila {@code kurikulumPunyaMatakuliahDetail} diberikan, {@code kurikulumPunyaMatakuliahTemp} DIABAIKAN
	 * dan kurikulum-punya-matakuliah diambil dari {@code kurikulumPunyaMatakuliahDetail.getKurikulumPunyaMatakuliah()}
	 * — parameter tersebut jadi mengikat konteksnya sendiri. Toolbar sumber materi hanya tampil untuk
	 * dosen/admin murni (bukan mahasiswa/calon mahasiswa/peserta kursus/siswa/calon siswa) dan hanya bila
	 * minimal satu dari {@code pertemuan}/{@code kurikulumPunyaMatakuliahDetail}/{@code grupPertemuan} tidak
	 * {@code null}. State instance ({@link #pertemuan}, {@link #grupPertemuan}, {@link #kurikulumPunyaMatakuliah},
	 * {@link #kurikulumPunyaMatakuliahDetail}, {@link #selectedPertemuanFileContent}, {@link #center}) diisi di
	 * sini dan dipakai oleh method privat lain sepanjang siklus hidup helper ini.</p>
	 *
	 * @param pertemuan pertemuan (sesi kuliah) target; boleh {@code null}
	 * @param grupPertemuan grup pertemuan target (kelas paralel); boleh {@code null}
	 * @param kurikulumPunyaMatakuliahTemp kurikulum-punya-matakuliah target; diabaikan bila
	 *            {@code kurikulumPunyaMatakuliahDetail} tidak {@code null}
	 * @param kurikulumPunyaMatakuliahDetail detail topik RPS target; boleh {@code null}
	 * @param tabpanelFilePertemuan komponen ZK (biasanya {@code Tabpanel}) tempat header dan layout materi ditempelkan
	 * @param selectedPertemuanFileContent materi yang harus otomatis terpilih/terbuka saat daftar dirender
	 */
	public void createFile(final Pertemuan pertemuan, final GrupPertemuan grupPertemuan,
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahTemp,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final Component tabpanelFilePertemuan,
			PertemuanFileContent selectedPertemuanFileContent) {

		if (kurikulumPunyaMatakuliahDetail != null) {
			kurikulumPunyaMatakuliahTemp = kurikulumPunyaMatakuliahDetail.getKurikulumPunyaMatakuliah();
		}
		this.pertemuan = pertemuan;
		this.grupPertemuan = grupPertemuan;
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliahTemp;
		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;
		this.selectedPertemuanFileContent = selectedPertemuanFileContent;

		// Header modul modern (selaras modul e-learning lain) + penjelasan singkat untuk pengguna awam.
		try {
			if (tabpanelFilePertemuan != null) {
				tabpanelFilePertemuan.appendChild(ais.ui.util.DashboardUiKit.html(ais.ui.util.DashboardUiKit.headerModul(
						"materi", "Materi & Berkas Pertemuan",
						"Semua materi dan berkas yang dibagikan pada pertemuan ini. Unduh untuk belajar, atau unggah materi baru.")));
			}
		} catch (Exception abaikanHeader) { ais.common.ErrorAuditUtil.record(abaikanHeader, "auto-audit(empty-catch) src/ais/action/master/helper/FilePerkuliahanHelper.java:113");
		}

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setHeight("4000px");
		borderlayout.setParent(tabpanelFilePertemuan);

		North north = new North();
		north.setParent(borderlayout);

		final Toolbar vbox = new Toolbar();

		vbox.appendChild(AmbilDataPertemuanFileContent.createScanFoto(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("pertemuan_file_content");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadPertemuanFileContent();
								CommonEmail.infoAdaFilePerkuliahan(pertemuan, (PertemuanFileContent) arg0.getData());
							}
						});
					}
				}));

		if (!Common.isMobile())
			vbox.appendChild(AmbilDataPertemuanFileContent.createScanLayar(pertemuan, kurikulumPunyaMatakuliah,
					kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (pertemuan != null)
								pertemuan.belum("pertemuan_file_content");
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadPertemuanFileContent();
									CommonEmail.infoAdaFilePerkuliahan(pertemuan,
											(PertemuanFileContent) arg0.getData());
								}
							});
						}
					}));

		vbox.appendChild(AmbilDataPertemuanFileContent.createScorm(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("pertemuan_file_content");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadPertemuanFileContent();
								CommonEmail.infoAdaFilePerkuliahan(pertemuan, (PertemuanFileContent) arg0.getData());
							}
						});
					}
				}));

		Tbmuser tbmuser = Common.getCurrentUser();
		vbox.setVisible(tbmuser != null && mahasiswa == null && biodataCalonMahasiswa == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
				&& (pertemuan != null || kurikulumPunyaMatakuliahDetail != null || grupPertemuan != null));
		vbox.setParent(north);
		vbox.setWidth("100%");
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Upload/Ambil Materi Sebelumnya", "/img/new.gif");
		vbox.appendChild(mybutton);

		mybutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final AmbilDataPertemuanFileContent ambilDataLampiranFileLain = new AmbilDataPertemuanFileContent(
						pertemuan, grupPertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail);

				ambilDataLampiranFileLain.setHeight("95%");
				ambilDataLampiranFileLain.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataLampiranFileLain);
				ambilDataLampiranFileLain.onModal();
				ambilDataLampiranFileLain.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						try {
							if (pertemuan != null)
								pertemuan.belum("pertemuan_file_content");
							if (arg0.getName().equalsIgnoreCase("baru")) {
								PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) arg0.getData();
								reloadPertemuanFileContent();
								CommonEmail.infoAdaFilePerkuliahan(pertemuan, pertemuanFileContent);
							} else {
								PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) arg0.getData();
								if (pertemuanFileContent != null) {
									Session session = StreamingHibernateUtil.getInstance().currentSession();
									try {

										final PertemuanFileContent copy = new PertemuanFileContent();
										copy.setKurikulumPunyaMatakuliahDetail(
												kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
														: kurikulumPunyaMatakuliahDetail.getId());
										copy.setGrupPertemuan(
												grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
										copy.setKurikulumPunyaMatakuliah(
												kurikulumPunyaMatakuliah == null ? -Common.randLong()
														: kurikulumPunyaMatakuliah.getId());
										copy.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
										copy.setCopyDari(pertemuanFileContent);
										copy.setLokasiFisik(pertemuanFileContent.getLokasiFisik());

										Tbmuser tbmuser = Common.getCurrentUser();
										Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
										Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
										Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
										String olehId = Common.generateOlehId(tbmuser);
										copy.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
										copy.setOlehId(olehId);
										copy.setOleh(tbmuser == null ? "external_update"
												: mahasiswa != null ? mahasiswa.getNama()
														: dosen != null ? dosen.getNama()
																: pegawai != null ? pegawai.getNama()
																		: (tbmuser.getUserNama()));

										session.getTransaction().begin();
										session.save(copy);
										session.getTransaction().commit();

										pertemuanFileContent = copy;

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FilePerkuliahanHelper.java:253");
									}

									StreamingHibernateUtil.getInstance().closeSession();

									reloadPertemuanFileContent();
									CommonEmail.infoAdaFilePerkuliahan(pertemuan, pertemuanFileContent);
								}

							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FilePerkuliahanHelper.java:264");
						}

						ambilDataLampiranFileLain.setVisible(false);
						ambilDataLampiranFileLain.detach();
					}
				});

			}
		});

		vbox.appendChild(AmbilDataPertemuanFileContent.tampilkanTombolUploadGDrive(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("pertemuan_file_content");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadPertemuanFileContent();
								CommonEmail.infoAdaFilePerkuliahan(pertemuan, (PertemuanFileContent) arg0.getData());
							}
						});
					}
				}));

		vbox.appendChild(AmbilDataPertemuanFileContent.tampilkanTombolUploadDropbox(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("pertemuan_file_content");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadPertemuanFileContent();
								CommonEmail.infoAdaFilePerkuliahan(pertemuan, (PertemuanFileContent) arg0.getData());
							}
						});
					}
				}));

		vbox.appendChild(AmbilDataPertemuanFileContent.tampilkanTombolTambahLink(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("pertemuan_file_content");
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadPertemuanFileContent();
								CommonEmail.infoAdaFilePerkuliahan(pertemuan, (PertemuanFileContent) arg0.getData());
							}
						});
					}
				}));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Buku", "/img/Apps-Google-Play-Books-icon.png");
		button.setVisible(Common.bolehKonfigurasi("aktifkan_ambil_buku_dari_google_book"));
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataDariGoogleBookBanyak ambilDataDariGoogleBookBanyak = new AmbilDataDariGoogleBookBanyak(
						pertemuan != null && pertemuan.getPerkuliahan() != null
								&& pertemuan.getPerkuliahan().getMatakuliah() != null
										? pertemuan.getPerkuliahan().getMatakuliah().getNama()
										: "");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(ambilDataDariGoogleBookBanyak);
				ambilDataDariGoogleBookBanyak.setHeight("95%");
				ambilDataDariGoogleBookBanyak.setWidth("90%");

				ambilDataDariGoogleBookBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						List<Object> objects = (List<Object>) arg0.getData();
						for (Object object : objects) {
							// Sumber buku bisa: Item / Volume (hasil pencarian di Google Book) ATAU
							// ItemTemporary (dipilih dari tab "Daftar buku yang sebelumnya sudah diambil").
							// Sebelumnya ItemTemporary di-cast PAKSA ke Volume di LUAR try → ClassCastException
							// sehingga "Pilih Buku" GAGAL menyimpan buku dari tab tersebut. Kini ketiga tipe
							// ditangani dengan mengekstrak field yang sama (infoLain/nama/isbn/isbn10/link).
							String infoLainBuku;
							String namaBuku;
							String isbnBuku;
							String isbn10Buku;
							String linkBuku = "";
							if (object instanceof Item) {
								Item item = (Item) object;
								infoLainBuku = item.getInfoLain();
								namaBuku = item.getNama();
								isbnBuku = item.getIsbn();
								isbn10Buku = item.getIsbn10();
							} else if (object instanceof ais.database.model.library.ItemTemporary) {
								ais.database.model.library.ItemTemporary it = (ais.database.model.library.ItemTemporary) object;
								infoLainBuku = it.getInfoLain();
								namaBuku = it.getNama();
								isbnBuku = it.getIsbn();
								isbn10Buku = it.getIsbn10();
								linkBuku = it.getLink();
							} else {
								Item item = CheckISBN.simpanVolume((Volume) object, new Item());
								infoLainBuku = item.getInfoLain();
								namaBuku = item.getNama();
								isbnBuku = item.getIsbn();
								isbn10Buku = item.getIsbn10();
							}

							try {

								JSONObject jsonObject = new JSONObject(infoLainBuku).getJSONObject("volumeInfo");

								String previewLink = jsonObject.has("previewLink")
										? jsonObject.getString("previewLink")
										: linkBuku;
								if (previewLink == null || previewLink.trim().isEmpty()) {
									previewLink = linkBuku == null ? "" : linkBuku;
								}

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
								pertemuanFileContent.setLink(previewLink);
								pertemuanFileContent.setNama(namaBuku);
								pertemuanFileContent.setFileMimeType("Google Book");
								pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								pertemuanFileContent.setGrupPertemuan(
										grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
								pertemuanFileContent.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								pertemuanFileContent
										.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
								pertemuanFileContent.setGoogleBook(infoLainBuku);
								pertemuanFileContent.setType(
										(isbnBuku == null || isbnBuku.isEmpty()) ? isbn10Buku : isbnBuku);

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								pertemuanFileContent.setOlehId(olehId);
								pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								session.getTransaction().begin();
								session.save(pertemuanFileContent);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								StreamingHibernateUtil.getInstance().rollbackTransaction();
							}

						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadPertemuanFileContent();
							}
						});
					}
				});

				ambilDataDariGoogleBookBanyak.onModal();

			}

		});
		button.setParent(vbox);

		button = new MyToolbarbuttonConfig("Ambil Artikel", "/img/education-university-icon.png");
		button.setVisible(false);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataDariGoogleScholarBanyak ambilDataDariGoogleScholarBanyak = new AmbilDataDariGoogleScholarBanyak(
						pertemuan != null && pertemuan.getPerkuliahan() != null
								&& pertemuan.getPerkuliahan().getMatakuliah() != null
										? pertemuan.getPerkuliahan().getMatakuliah().getNama()
										: "");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(ambilDataDariGoogleScholarBanyak);
				ambilDataDariGoogleScholarBanyak.setHeight("95%");
				ambilDataDariGoogleScholarBanyak.setWidth("90%");

				ambilDataDariGoogleScholarBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Tbmuser tbmuser = Common.getCurrentUser();
						List<ScholarArticle> objects = (List<ScholarArticle>) arg0.getData();
						for (ScholarArticle scholarArticle : objects) {

							try {

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
								pertemuanFileContent.setLink(scholarArticle.getLink());
								pertemuanFileContent.setNama(scholarArticle.getNama());
								pertemuanFileContent.setFileMimeType("Google Scholar");
								pertemuanFileContent.setGoogleBook(scholarArticle.getHeaders());
								pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								pertemuanFileContent.setGrupPertemuan(
										grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
								pertemuanFileContent.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								pertemuanFileContent
										.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

								pertemuanFileContent.setType(scholarArticle.getId().toString());

								Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								pertemuanFileContent.setOlehId(olehId);
								pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								session.getTransaction().begin();
								session.save(pertemuanFileContent);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								StreamingHibernateUtil.getInstance().rollbackTransaction();
							}

						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								reloadPertemuanFileContent();
							}
						});
					}
				});

				ambilDataDariGoogleScholarBanyak.onModal();

			}

		});
		button.setParent(vbox);

		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		refresh.setParent(vbox);
		refresh.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (pertemuan != null)
					pertemuan.belum("pertemuan_file_content");
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						reloadPertemuanFileContent(true);
					}
				});
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		reloadPertemuanFileContent();
	}

	/** Memuat ulang daftar materi tanpa memaksa refresh cache pertemuan. Lihat {@link #reloadPertemuanFileContent(boolean)}. */
	private void reloadPertemuanFileContent() {
		reloadPertemuanFileContent(false);
	}

	/**
	 * Mengosongkan {@link #center} lalu mengisinya ulang dengan daftar materi terkini dan merender kontennya.
	 *
	 * <p>Bila {@link #pertemuan} tidak {@code null}, daftar diambil dari cache
	 * {@code pertemuan.ambilPertemuanFileContentTotal(refresh)} (parameter {@code refresh} memaksa cache
	 * dibangun ulang dari DB, dipakai oleh tombol "Refresh"). Jika tidak, daftar diquery langsung ke
	 * {@link PertemuanFileContent} dengan filter {@link #grupPertemuan}/{@link #kurikulumPunyaMatakuliahDetail}/
	 * {@link #kurikulumPunyaMatakuliah} (masing-masing opsional), diurutkan {@code id} descending.</p>
	 * <p>Bila hasilnya tepat satu materi, kontennya langsung dirender di {@link #center} lewat
	 * {@link #tampilkanKonten}. Bila lebih dari satu, dibangun {@code Tabbox} dengan satu tab per materi (ikon
	 * disesuaikan sumbernya — Google Scholar, Google Book, atau ikon file biasa via {@link FileFoto#icon});
	 * konten tab dirender lazy saat pertama diklik. Tab yang otomatis terpilih dan langsung dirender adalah
	 * yang cocok dengan {@link #selectedPertemuanFileContent}, atau tab pertama bila tidak ada yang cocok.</p>
	 *
	 * @param refresh {@code true} untuk memaksa {@code pertemuan.ambilPertemuanFileContentTotal} membaca ulang dari DB
	 */
	@SuppressWarnings("unchecked")
	private void reloadPertemuanFileContent(boolean refresh) {
		if (center != null) {
			Common.clear(center);
		}

		List<PertemuanFileContent> pertemuanFileContents = new ArrayList<PertemuanFileContent>();

		if (pertemuan != null) {
			TreeMap<Long, PertemuanFileContent> pertemuanFileContentsa = pertemuan
					.ambilPertemuanFileContentTotal(refresh);
			pertemuanFileContents = new ArrayList<PertemuanFileContent>(pertemuanFileContentsa.values());
		} else {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				pertemuanFileContents = session.createCriteria(PertemuanFileContent.class).addOrder(Order.desc("id"))

						.add(grupPertemuan != null ? Restrictions.eq("grupPertemuan", grupPertemuan.getId())
								: Restrictions.sqlRestriction("true"))

						.add(kurikulumPunyaMatakuliahDetail != null
								? Restrictions.eq("kurikulumPunyaMatakuliahDetail",
										kurikulumPunyaMatakuliahDetail.getId())
								: pertemuan == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("pertemuan", pertemuan.getId()))

						.add(kurikulumPunyaMatakuliah == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah.getId()))
						.list();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		System.out.println("pertemuanFileContents -> " + pertemuanFileContents.size());

		if (pertemuanFileContents.size() == 1) {
			tampilkanKonten(center, pertemuanFileContents.get(0));
		} else {

			Tabbox tabbox = new Tabbox();
			boolean mobile = Common.isMobile();
			if (mobile) {
				tabbox.setOrient("vertical");
			}
			tabbox.setParent(center);
			tabbox.setHeight("5000px");
			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			int index = 0;
			for (final PertemuanFileContent pertemuanFileContent : pertemuanFileContents) {

				String n = pertemuanFileContent.getNama() != null
						&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link")
								? pertemuanFileContent.getLink()
								: pertemuanFileContent.getNama();

				String na = n;
				if (!pertemuanFileContent.getKeterangan().isEmpty()) {
					na = pertemuanFileContent.getKeterangan();
				}

				if (na == null) {
					na = "";
				}

				Tab tab;
				tabs.appendChild(tab = new Tab(na.length() > 15 ? na.substring(0, 15) + ".." : na,
						pertemuanFileContent.getFileMimeType() != null
								&& pertemuanFileContent.getFileMimeType().equalsIgnoreCase("Google Scholar")
										? "/img/education-university-icon.png"
										: !pertemuanFileContent.getGoogleBook().isEmpty()
												? "/img/Apps-Google-Play-Books-icon.png"
												: FileFoto.icon(n)));
				if (mobile) {
					tab.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
				}

				final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanels.appendChild(tabpanelUtama);
				tabpanelUtama.setHeight("5000px");
				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (tabpanelUtama.getChildren().isEmpty()) {
							tampilkanKonten(tabpanelUtama, pertemuanFileContent);
						}
					}
				};

				tab.addEventListener("onClick", eventListener);

				if (selectedPertemuanFileContent != null && selectedPertemuanFileContent.getId() != null
						&& pertemuanFileContent.getId() != null
						&& selectedPertemuanFileContent.getId().equals(pertemuanFileContent.getId())) {
					try {
						tab.setSelected(true);
						eventListener.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FilePerkuliahanHelper.java:684");
					}
				}

				else if (index == 0) {
					try {
						tab.setSelected(true);
						eventListener.onEvent(null);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FilePerkuliahanHelper.java:694");
					}
				}

				index++;
			}
		}
	}

	/**
	 * Merender konten lengkap satu materi ({@code pertemuanFileContent}) ke {@code tabpanelUtama}: daftar
	 * syarat/ketentuan terkait (bila {@link #pertemuan} ada), baris keterangan (dapat diedit langsung oleh
	 * dosen/admin via textbox — perubahan langsung di-{@code UPDATE} ke kolom {@code keterangan} lewat SQL
	 * native dan judul tab ikut diperbarui), tombol buka/unduh materi, tombol Hapus (disembunyikan untuk
	 * mahasiswa/calon mahasiswa/siswa/peserta kursus), tombol "Akses Materi" ({@code TampilanELearningAction.dilihat}
	 * mencatat aktivitas belajar), dan pratinjau konten sesuai jenisnya: iframe video (materi berbasis lokasi
	 * fisik/streaming), iframe Google Scholar (kecuali dibatasi header X-Frame-Options SAMEORIGIN), embed
	 * pratinjau Google Books, tampilan Google Drive, konten via URL, atau pratinjau berkas lokal
	 * ({@code CommonMedia.preview}) — dengan opsi simpan-ke-Drive bila berkas lokal tersedia.</p>
	 *
	 * <p>Tombol Hapus TIDAK menghapus baris {@link PertemuanFileContent}, melainkan melepas relasinya
	 * (pertemuan/kurikulumpunyamatakuliah/kurikulumpunyamatakuliahdetail/gruppertemuan diarahkan ke id sentinel
	 * negatif) lewat SQL native dengan commit eksplisit di dalam {@code session.doWork(...)} — lihat komentar
	 * KE-FIX pada kode terkait bug rollback {@code StreamingHibernateUtil} yang sebelumnya membuat perubahan
	 * ini tidak persist.</p>
	 *
	 * @param tabpanelUtama komponen ZK (tab konten atau {@link #center} langsung bila hanya satu materi) tempat konten ditempelkan
	 * @param pertemuanFileContent materi yang akan dirender
	 */
	private void tampilkanKonten(final Component tabpanelUtama, final PertemuanFileContent pertemuanFileContent) {

		PertemuanFileContent.chekScrom(pertemuanFileContent);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(tabpanelUtama);

		Rows myRows = new Rows();


		Set<String> syaratAlert = new HashSet<String>();

		if (pertemuan != null) {

			MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

			Tbmuser tbmuser = Common.getCurrentUser();
			if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
					&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {
				Tugas.tampilanSyarat(pertemuan, null, null, pertemuanFileContent, null, null, myRows, syaratAlert,
						button);
			} else {
				Tugas.tampilanSyaratReadonly(pertemuan, null, null, pertemuanFileContent, null, null, myRows,
						syaratAlert, button);

				Tugas.tampilanLain(pertemuan, null, null, pertemuanFileContent, null, null, myRows, button);
			}
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (!syaratAlert.isEmpty() && (mahasiswa != null || biodataCalonMahasiswa != null
				|| (tbmuser != null && (tbmuser.getPesertaKursus() != null || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null)))) {

			Center center = new Center();
			center.setParent(borderlayout);
			center.setBorder("none");

			Row rowUtama = Common.tampilanScroll1(center);

			Grid grid = new Grid();
			grid.setSclass("fgrid");
			grid.setParent(rowUtama);
			grid.setOddRowSclass("non-odd");

			Rows rows = new Rows();

			rows.setParent(grid);

			MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

			Tugas.tampilanSyaratReadonly(pertemuan, null, null, pertemuanFileContent, null, null, rows, syaratAlert,
					button);

			Row baru = new Row();
			baru.setParent(rowUtama.getParent());

			grid = new Grid();
			grid.setSclass("fgrid");
			grid.setParent(baru);
			grid.setOddRowSclass("non-odd");

			rows = new Rows();
			rows.setParent(grid);

			button = new MyToolbarbutton("fa-refresh", "Refresh Info Lainya");

			Tugas.tampilanLain(pertemuan, null, null, pertemuanFileContent, null, null, rows, button);

		} else {
			Center center = new Center();
			center.setParent(borderlayout);
			center.setBorder("none");

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setParent(center);
			grid.setSclass("fgrid");
			grid.setOddRowSclass("non-odd");

			Rows rows = new Rows();

			rows.setParent(grid);

			if (pertemuan != null)
				pertemuan.masukkanData("bahan_perkulaiahan_" + pertemuanFileContent.getId());

			if ((tbmuser != null)
					&& (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
							&& tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null)) {

				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new MyLabelBoldAja("Keterangan materi :"));

				row = new Row();
				row.setParent(rows);

				final Textbox ketarangan = new Textbox(pertemuanFileContent.getKeterangan());
				ketarangan.setWidth("90%");
				ketarangan.setRows(2);
				ketarangan.setParent(row);
				ketarangan.setMaxlength(255);
				ketarangan.setStyle("border: 1px solid #9fb8bf;border-radius: 10px;");

				ketarangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (pertemuan != null)
							pertemuan.belum("pertemuan_file_content");
						Session session = StreamingHibernateUtil.getInstance().currentSession();
						session.createSQLQuery("update pertemuan_file_content set keterangan= :keterangan where id="
								+ pertemuanFileContent.getId()).setParameter("keterangan", ketarangan.getValue().trim())
								.executeUpdate();
						StreamingHibernateUtil.getInstance().closeSession();

						if (tabpanelUtama instanceof Tabpanel) {
							String n = ketarangan.getValue().trim();
							if (!n.isEmpty()) {
								((Tabpanel) tabpanelUtama).getLinkedTab()
										.setLabel(n.length() > 30 ? n.substring(0, 30) + "..." : n);
							}
						}
					}
				});
			} else {
				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				new Label(pertemuanFileContent.getKeterangan()).setParent(row);
			}

			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			Hbox hbox = new Hbox();
			hbox.setParent(row);

			String n = pertemuanFileContent.getNama() != null
					&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link") ? pertemuanFileContent.getLink()
							: pertemuanFileContent.getNama();

			if (n == null) {
				n = pertemuanFileContent.getNama();
			}
			n = n.length() > 50 ? n.substring(0, 50) + "..." : n;
			if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
				n = pertemuanFileContent.getNama();
			}

			Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig(n,

					pertemuanFileContent.getFileMimeType() != null
							&& pertemuanFileContent.getFileMimeType().equalsIgnoreCase("Google Scholar")
									? "/img/education-university-icon.png"
									: (!pertemuanFileContent.getGoogleBook().isEmpty()
											&& pertemuanFileContent.getFileMimeType() != null
											&& pertemuanFileContent.getFileMimeType().equalsIgnoreCase("Google Book"))
													? "/img/Apps-Google-Play-Books-icon.png"
													: pertemuanFileContent.getLokasiFisik() != null
															? "/img/svg/desktop-light.svg"
															: FileFoto.icon(pertemuanFileContent.getNama()));
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
						if (Common.isMobile()) {
							ExecutionsCtrl.getCurrent().sendRedirect(pertemuanFileContent.getLink(), "_blank");
						} else {
							Clients.evalJavaScript("popupCenter({url: '" + Common.jsEscape(pertemuanFileContent.getLink())
									+ "', title: 'Book', w: 1200, h: 600});");
						}
					} else if (pertemuanFileContent.getGdrive() != null) {
						pertemuanFileContent.tampilGDrive(null);
					} else {

						String link = pertemuanFileContent == null ? null
								: (pertemuanFileContent.getLink() == null || pertemuanFileContent.getLink().isEmpty()
										? null
										: pertemuanFileContent.getLink());

						if (pertemuanFileContent != null
								&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
							link = pertemuanFileContent.createLinkUri();
							if (link != null) {
								// link = link.replaceAll("download=false", "download=true");
							}
						}

						if (pertemuanFileContent != null && link != null && !link.trim().isEmpty()) {

							if (pertemuanFileContent.bisaPreview()) {
								Common.displayWindow(pertemuanFileContent.merupakanGambar(), link, true, "95%", "95%",
										true, pertemuanFileContent);
							} else {

								if (Common.isMobile()) {
									ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
								} else {
									Clients.evalJavaScript(
											"popupCenter({url: '" + Common.jsEscape(link) + "', title: 'data', w: 1200, h: 600});");
								}

							}
						} else {
							MyMessageboxConfig.show(
									"Mohon maaf, berkas yang Anda akses tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) pastikan berkas belum dihapus atau dipindahkan; (3) hubungi pengajar atau administrator apabila berkas seharusnya tersedia.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					}
				}

			});

			toolbarbutton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			toolbarbutton
					.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
							&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
							&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null);
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Anda yakin ingin menghapus data ini? Perlu diperhatikan, tindakan ini bersifat permanen dan data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = StreamingHibernateUtil.getInstance().currentSession();

											session.doWork(new Work() {

												@Override
												public void execute(Connection connection) throws SQLException {
													// TODO Auto-generated method
													// stub
													String sql = "update pertemuan_file_content set pertemuan=-11111111111111111,kurikulumpunyamatakuliah=-11111111111111111,kurikulumpunyamatakuliahdetail=-11111111111111111,gruppertemuan=-11111111111111111 where id="
															+ pertemuanFileContent.getId() + " ";
													Statement stmt = null;

													try {
														stmt = connection.createStatement();
														System.out.println("proses hapus -> " + sql);
														int hasil = stmt.executeUpdate(sql);
														System.out.println("hapus -> " + sql + " hasil " + hasil);
														// COMMIT agar penghapusan materi PERSIST. StreamingHibernate tidak commit di
														// jalur sukses (closeSession hanya menutup) → sebelumnya UPDATE ter-rollback
														// sehingga materi (termasuk yang DOUBLE) "tidak bisa dihapus" dosen/admin.
														if (!connection.getAutoCommit()) {
															connection.commit();
														}
													} catch (SQLException e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FilePerkuliahanHelper.java:964");
													} finally {
														if (stmt != null) {
															stmt.close();
														}
													}

												}
											});

											// pertemuanFileContent.setPertemuan(-11111111111111111L);
											// pertemuanFileContent.setKurikulumPunyaMatakuliah(-11111111111111111L);
											// pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(-11111111111111111L);
											// pertemuanFileContent.setGrupPertemuan(-11111111111111111L);
											//
											// session.getTransaction().begin();
											// Common.refreshSaveOrUpdate(session,
											// pertemuanFileContent);
											// session.getTransaction().commit();
											if (pertemuan != null)
												pertemuan.belum("pertemuan_file_content");
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													reloadPertemuanFileContent();
												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											StreamingHibernateUtil.getInstance().rollbackTransaction();
											MyMessageboxConfig.show(
													Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang terkait; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala berlanjut.",
														e.getMessage()));
										}
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}
							});

				}

			});

			TampilanELearningAction
					.dilihat(pertemuan, "bahan_perkulaiahan_" + pertemuanFileContent.getId(), "Akses Materi")
					.setParent(hbox);

			if (pertemuanFileContent.getLokasiFisik() != null) {
				row = new Row();
				row.setParent(rows);
				String contentVideo = "<iframe src=\"" + pertemuanFileContent.getLink()
						+ "\" style=\"border:none;overflow:hidden;width:100%;min-height: 530px;min-width: 560px;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"></iframe>";
				new ais.ui.util.MyHtml(contentVideo).setParent(row);
			}

			else if (pertemuanFileContent.getFileMimeType() != null
					&& pertemuanFileContent.getFileMimeType().equalsIgnoreCase("Google Scholar")
					&& !pertemuanFileContent.getGoogleBook().toLowerCase()
							.contains("X-Frame-Options=[SAMEORIGIN]".toLowerCase())) {
				row = new Row();
				row.setParent(rows);
				String contentVideo = "<iframe src=\"" + pertemuanFileContent.getLink()
						+ "\" style=\"border:none;overflow:hidden;width:100%;min-height: 430px;min-width: 560px;\" scrolling=\"yes\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"></iframe>";
				new ais.ui.util.MyHtml(contentVideo).setParent(row);
			}

			else if (!pertemuanFileContent.getGoogleBook().trim().isEmpty()
					&& pertemuanFileContent.getFileMimeType() != null
					&& pertemuanFileContent.getFileMimeType().equalsIgnoreCase("Google Book")) {
				// Tanam (embed) PRATINJAU buku Google Books LANGSUNG di tab Materi. Sebelumnya blok
				// ini dikomentari total sehingga buku TIDAK pernah tampil (hanya bisa dibuka lewat
				// jendela popup saat judul diklik). Render via UrlDisplayHelper.displayUrlContent →
				// cabang books.google → Google Books Embedded Viewer API (jsapi DefaultViewer).
				row = new Row();
				row.setParent(rows);
				String bookLink = pertemuanFileContent.getLink();
				if (bookLink != null && !bookLink.trim().isEmpty()) {
					try {
						ais.common.UrlDisplayHelper.displayUrlContent(bookLink, row);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FilePerkuliahanHelper.java:1049");
					}
				}
			} else {
				if (pertemuanFileContent.getGdrive() == null) {

					File f = pertemuanFileContent.ambilFile();
					if (f != null && f.exists()) {
						VOPembelajaran voPembelajaran = pertemuan.ambilVOPembelajaran();
						String tugasName = "Materi pertemuan ke " + pertemuan.getPertemuanKe() + " " + pertemuan.info();
						Common.simpanKeDrive(pertemuanFileContent, f, voPembelajaran.infoSimple(), tugasName,
								new EventListener() {

									@Override
									public void onEvent(final Event arg0) throws Exception {
										if (tabpanelUtama != null) {
											Common.clear(tabpanelUtama);
										}
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event a) throws Exception {
												reloadPertemuanFileContent();
											}
										});
									}
								}).setParent(hbox);
					}

				}

				row = new Row();
				row.setParent(rows);
				String link = pertemuanFileContent.getLink();

				if (pertemuanFileContent.getGdrive() != null) {
					try {
						pertemuanFileContent.tampilGDrive(row);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FilePerkuliahanHelper.java:1088");
					}
				} else if (link != null && !link.trim().isEmpty() && link.toLowerCase().startsWith("http")) {
					Common.displayUrlContent(link, row);
				}

				else {
					try {
						CommonMedia.preview(pertemuanFileContent, row);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/FilePerkuliahanHelper.java:1099");
					}
				}
			}

			// Grid syarat/persyaratan (myRows) HANYA dirender bila benar-benar ADA syarat yang
			// dapat dipilih (Checkbox item tugas/ujian/materi). Tugas.tampilanSyarat SELALU
			// menambah 3 baris pembungkus (rowTugas/rowUjian/rowMateri) ke myRows walau pertemuan
			// TIDAK punya item apa pun → grid-scroll kosong tampil sebagai deretan GARIS KOSONG.
			// Karena itu cek keberadaan Checkbox, bukan sekadar getChildren().isEmpty().
			if (myRows != null && mengandungKomponenSyarat(myRows)) {
				row = new Row();
				row.setParent(rows);
				grid = new Grid();
				grid.setParent(row);
				grid.setSclass("fgrid");
				grid.setOddRowSclass("non-odd");
				grid.appendChild(myRows);
			}
		}
	}

	/**
	 * Mengecek apakah subtree komponen {@code c} mengandung minimal satu
	 * {@link org.zkoss.zul.Checkbox}, yang menandakan ADA item syarat (tugas/ujian/materi)
	 * yang dapat dipilih oleh dosen.
	 *
	 * <p><b>Latar:</b> {@code Tugas.tampilanSyarat} SELALU menambah tiga baris pembungkus
	 * (rowTugas/rowUjian/rowMateri) ke {@code myRows} — masing-masing berisi grid-scroll yang
	 * diisi {@code ProfileUtil.tampilkanMateri}. Bila pertemuan tidak memiliki item apa pun,
	 * grid-grid tersebut KOSONG namun pembungkusnya tetap ada, sehingga tampil sebagai deretan
	 * GARIS KOSONG. Pemeriksaan {@code getChildren().isEmpty()} tidak cukup karena pembungkus
	 * selalu ada; yang menentukan ada-tidaknya isi nyata adalah keberadaan Checkbox item.</p>
	 *
	 * @param c komponen akar yang ditelusuri secara rekursif.
	 * @return {@code true} bila ada Checkbox di dalam subtree {@code c}.
	 */
	private static boolean mengandungKomponenSyarat(Component c) {
		if (c == null) {
			return false;
		}
		if (c instanceof org.zkoss.zul.Checkbox) {
			return true;
		}
		java.util.List<?> anak = c.getChildren();
		if (anak != null) {
			for (Object o : anak) {
				if (o instanceof Component && mengandungKomponenSyarat((Component) o)) {
					return true;
				}
			}
		}
		return false;
	}
}
