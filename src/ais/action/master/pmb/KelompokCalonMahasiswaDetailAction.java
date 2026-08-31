package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.KelompokCalonMahasiswaAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataBiodataCalonMahasiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.KelompokCalonMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk kelompok calon mahasiswa detail. Tipe ini merupakan titik masuk UI
 * yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code KelompokCalonMahasiswa
 * kelompokCalonMahasiswa}, {@code MyGrid grid}, {@code Textbox pencarian}; inisialisasi/lifecycle ({@code
 * initCriteria()}); pembacaan/pencarian ({@code loadData()}, {@code tampilkanCatatanKeanggotaan()}); mutasi data
 * ({@code prosesUlang()}); operasi domain lain ({@code display()}, {@code periksaTumpangTindihSkor()}, {@code
 * jelaskanKeanggotaanOtomatis()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class KelompokCalonMahasiswaDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private KelompokCalonMahasiswa kelompokCalonMahasiswa;
	private MyGrid grid;

	private Textbox pencarian;

	public KelompokCalonMahasiswaDetailAction(KelompokCalonMahasiswa kelompokCalonMahasiswa) {
		super();
		this.kelompokCalonMahasiswa = kelompokCalonMahasiswa;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KelompokCalonMahasiswaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KelompokCalonMahasiswaDetailAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KelompokCalonMahasiswaDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KelompokCalonMahasiswaDetailAction
	 */
	class BiodataCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public BiodataCalonMahasiswaRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			// TODO Auto-generated method stub
			final BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) data;

			CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(arg0);

			RevisiHelper.createNewRevisi(BiodataCalonMahasiswa.class, calonMahasiswa, calonMahasiswa.getNama())
					.setParent(arg0);

			new Label(calonMahasiswa.getTanggalLahir() == null
					? Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
					: Common.dateFormat2.get().format(calonMahasiswa.getTanggalLahir())).setParent(arg0);
			new Label(calonMahasiswa.getAsalSma() == null ? "" : calonMahasiswa.getAsalSma()).setParent(arg0);

			Vbox vbox = new Vbox();
			if (calonMahasiswa.getJenisSeleksi() != null)
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getJenisSeleksi().toString())));
			if (calonMahasiswa.getNoRegistrasi() != null && !calonMahasiswa.getNoRegistrasi().trim().isEmpty())
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getNoRegistrasi())));
			if (calonMahasiswa.getNoUjian() != null && !calonMahasiswa.getNoUjian().trim().isEmpty())
				vbox.appendChild(new Label("No. Ujian:" + (calonMahasiswa.getNoUjian())));
			if (calonMahasiswa.getTotalSkor() > 0)
				vbox.appendChild(new Label("Skor :" + Common.numberFormat.get().format((calonMahasiswa.getTotalSkor()))));
			vbox.appendChild(new Label("Login :" + (calonMahasiswa.getTelahLogin() ? "Ya" : "Tidak")));
			if (calonMahasiswa.getWaktuLogin() != null)
				vbox.appendChild(
						new Label("Terakhir Login :" + Common.dateFormat.get().format(calonMahasiswa.getWaktuLogin())));
			if (calonMahasiswa.getNim() != null && !calonMahasiswa.getNim().trim().isEmpty())
				vbox.appendChild(new Label("NIM :" + (calonMahasiswa.getNim())));
			if (calonMahasiswa.getMerupakanPindahan()) {
				vbox.appendChild(new Label("Pindahan dari :" + (calonMahasiswa.getPindahanDariKampus())));
				vbox.appendChild(new Label("Prodi :" + (calonMahasiswa.getPindahanDariProdi())));
				vbox.appendChild(
						new Label("Pindah di semester :" + (calonMahasiswa.getPindahDariKampusLamaDiSemester())));
				vbox.appendChild(new Label("NIM lama :" + (calonMahasiswa.getNimLamaSebelumPindah())));
				vbox.appendChild(new Label("Alasan pindah:" + (calonMahasiswa.getKeteranganPindah())));
			}

			vbox.setParent(arg0);

			vbox = new Vbox();
			if (calonMahasiswa.getProdi1() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi1())));
			}
			if (calonMahasiswa.getProdi2() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi2())));
			}
			if (calonMahasiswa.getProdi3() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi3())));
			}
			if (calonMahasiswa.getProdi4() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi4())));
			}
			if (calonMahasiswa.getProdi5() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi5())));
			}
			if (calonMahasiswa.getMundur()) {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Mengundurkan diri")));
			} else if (calonMahasiswa.getDitolak()) {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak diterima (ditolak)")));
			} else if (calonMahasiswa.getProdiLulus() != null) {
				vbox.appendChild(new Label("Lulus di prodi : " + (calonMahasiswa.getProdiLulus())));
			} else {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum lulus")));
			}
			vbox.setParent(arg0);

			new Label(calonMahasiswa.getKelompokCalonMahasiswa() == null ? "Tidak" : "Ya").setParent(arg0);

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus Manual", "/img/svg/trash.svg");
			button.setVisible(calonMahasiswa.getKelompokCalonMahasiswa() != null);
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
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
											calonMahasiswa.setKelompokCalonMahasiswa(null);
											Common.refreshSaveOrUpdate(calonMahasiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			/*
			 * Baris OTOMATIS tidak punya apa pun untuk dihapus, jadi tombol di atas
			 * disembunyikan. Tapi tombol yang hilang begitu saja membuat operator mengira
			 * sistemnya rusak ("sudah coba hapus tetapi tidak bisa"). Ganti dengan penjelas
			 * yang menyebut penyebab SPESIFIK calon ini dan langkah yang harus ditempuh.
			 */
			MyToolbarbuttonConfig penjelas = new MyToolbarbuttonConfig("Kenapa tak bisa dihapus?",
					"/img/svg/info.svg");
			penjelas.setVisible(calonMahasiswa.getKelompokCalonMahasiswa() == null);
			penjelas.setOrient("vertical");
			penjelas.setTooltiptext("Keterangan keanggotaan otomatis");
			penjelas.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(jelaskanKeanggotaanOtomatis(calonMahasiswa), "Keanggotaan Otomatis",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
			});
			aksiButtons.add(penjelas);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<BiodataCalonMahasiswa> biodataCalonMahasiswas = ConstantValues.simpleList(initCriteria(true),
				BiodataCalonMahasiswa.class);

		ListModel strset = new SimpleListModel(biodataCalonMahasiswas);
		grid.setRowRenderer(new BiodataCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	private void prosesUlang(final KelompokCalonMahasiswa kelompokCalonMahasiswa) {

		final Label label = Common.displayLoadBar(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);

			}
		});

		new Thread(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {

				Session session = HibernateUtil.currentNativeSession();
				List<KelompokCalonMahasiswa> kelompokCalonMahasiswas = ConstantValues.simpleList(session
						.createCriteria(KelompokCalonMahasiswa.class)
						.add(Restrictions.eq("gelombangPendaftaran", kelompokCalonMahasiswa.getGelombangPendaftaran()))
						.addOrder(Order.asc("skorSampai")), KelompokCalonMahasiswa.class);
				List<Long> biodataCalonMahasiswas = session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.property("id"))
						.add(Restrictions.eq("gelombangPendaftaran", kelompokCalonMahasiswa.getGelombangPendaftaran()))
						.add(Restrictions.isNull("kelompokCalonMahasiswa")).list();
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();

				int size = biodataCalonMahasiswas.size();
				int index = 0;

				for (Long biodataCalonMahasiswaid : biodataCalonMahasiswas) {
					try {
						index++;

						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) ConstantValues
								.ambil(BiodataCalonMahasiswa.class.getName(), biodataCalonMahasiswaid);
						label.setValue("Memproses data " + biodataCalonMahasiswa.getNama() + " ("
								+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

						KelompokCalonMahasiswaAction.validasiStatusAwalMahasiswa(biodataCalonMahasiswa,
								kelompokCalonMahasiswas);
						session = HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						Common.refreshUpdate(session, biodataCalonMahasiswa);
						session.getTransaction().commit();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/KelompokCalonMahasiswaDetailAction.java:253");
					}
					HibernateUtil.closeSession();
				}
				biodataCalonMahasiswas = null;
				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar calon mahasiswa yang masuk kelompok "
				+ kelompokCalonMahasiswa.getStatusAwalMahasiswa().getNama()));

		tampilkanCatatanKeanggotaan(groupbox);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Proses Ulang", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (!kelompokCalonMahasiswa.getAktifkanPenggunaanSkor()) {
					if (kelompokCalonMahasiswa.getParameterTambahanInds().trim().isEmpty()) {
						MyMessageboxConfig.show("Mohon maaf, kelompok ini belum memiliki parameter sebagai kriteria seleksi. Langkah yang dapat dilakukan: (1) buka pengaturan kelompok dan tambahkan parameter kriteria yang diperlukan; (2) pastikan minimal satu parameter telah ditambahkan dan disimpan; (3) kembali ke halaman ini dan ulangi proses. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}
				prosesUlang(kelompokCalonMahasiswa);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Data Calon Mahasiswa Manual", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<BiodataCalonMahasiswa> biodataCalonMahasiswas = initCriteria(false).list();

				AmbilDataBiodataCalonMahasiswaBanyak window = new AmbilDataBiodataCalonMahasiswaBanyak(
						biodataCalonMahasiswas);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(final Event dataCalonMhs) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<BiodataCalonMahasiswa> biodataCalonMahasiswas = (List<BiodataCalonMahasiswa>) dataCalonMhs
										.getData();

								if (biodataCalonMahasiswas != null) {
									Session session = HibernateUtil.currentSession();
									// PERBAIKAN celah "Gratis Pol jadi reguler": Kelompok Calon Mahasiswa TIDAK
									// otomatis mengubah Jenis Seleksi mahasiswa yang ditambahkan manual, padahal
									// Setting Biaya mencocokkan tagihan berdasarkan Jenis Seleksi, BUKAN Kelompok.
									// Bila kelompok ini punya "Jenis Seleksi Target" terisi, sesuaikan sekalian
									// Jenis Seleksi tiap mahasiswa yang ditambahkan supaya tagihannya ikut benar.
									ais.database.model.JenisSeleksi jenisSeleksiTarget = kelompokCalonMahasiswa
											.getJenisSeleksiTarget();
									for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {
										biodataCalonMahasiswa.setKelompokCalonMahasiswa(kelompokCalonMahasiswa);
										// Sinkronkan statusAwalMahasiswa di DB ke status kelompok tujuan.
										// Tanpa ini, kolom DB tetap lama sehingga query initCriteria
										// cabang-otomatis masih menangkap calon ini di kelompok asal
										// → data double di dua kelompok sekaligus.
										biodataCalonMahasiswa.setStatusAwalMahasiswa(
												kelompokCalonMahasiswa.getStatusAwalMahasiswa());
										if (jenisSeleksiTarget != null) {
											biodataCalonMahasiswa.setJenisSeleksiDipilih(jenisSeleksiTarget);
										}
										Common.refreshUpdate(session, biodataCalonMahasiswa);
									}

									loadData(null);

									if (jenisSeleksiTarget == null) {
										MyMessageboxConfig.show(
												"Data calon mahasiswa berhasil ditambahkan ke kelompok ini. Perlu diperhatikan: kelompok ini BELUM memiliki \"Jenis Seleksi Target\" (kosong), sehingga Jenis Seleksi mahasiswa yang ditambahkan TIDAK ikut disesuaikan otomatis. Jika kelompok ini terkait beasiswa/Setting Biaya khusus, Jenis Seleksi tiap mahasiswa perlu disesuaikan manual satu per satu (mis. lewat menu \"Verifikasi Berkas dan Kelulusan\"), atau isi \"Jenis Seleksi Target\" pada pengaturan kelompok ini lalu pakai tombol \"Sinkronkan Jenis Seleksi\" agar seluruh anggota disesuaikan sekaligus.",
												"Berhasil (dengan Catatan)", MyMessageboxConfig.OK,
												MyMessageboxConfig.EXCLAMATION);
									} else {
										MyMessageboxConfig.show(
												"Data calon mahasiswa berhasil ditambahkan ke kelompok ini, dan Jenis Seleksi mereka sudah otomatis disesuaikan ke \""
														+ jenisSeleksiTarget.getNama()
														+ "\" agar tagihan/Setting Biaya ikut sesuai.",
												"Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									}
								}
							}
						});

					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		pencarian = new Textbox();
		pencarian.setCols(8);
		pencarian.setParent(toolbar);
		pencarian.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, CetakRegistrasiAction.contents);
		toolbar.appendChild(cetakToolbarbutton);

		button = new MyToolbarbuttonConfig("Hapus Manual Semua", "/img/svg/trash.svg");
		toolbar.appendChild(button);
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										@SuppressWarnings("unchecked")
										List<BiodataCalonMahasiswa> biodataCalonMahasiswas = ConstantValues.simpleList(
												initCriteria(true)
														.add(Restrictions.isNotNull("kelompokCalonMahasiswa")),
												BiodataCalonMahasiswa.class);
										for (BiodataCalonMahasiswa calonMahasiswa : biodataCalonMahasiswas) {
											calonMahasiswa.setKelompokCalonMahasiswa(null);
											Common.refreshSaveOrUpdate(calonMahasiswa);
										}

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(null);
											}
										});
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(
												"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});

			}
		});

		// PERBAIKAN celah "Gratis Pol jadi reguler" utk anggota yg SUDAH TERLANJUR ada
		// sebelum "Jenis Seleksi Target" kelompok ini diisi (mis. 112 mahasiswa yg sudah
		// dimasukkan manual sebelum perbaikan ini) -- tombol ini menyesuaikan Jenis Seleksi
		// SELURUH anggota manual kelompok ini sekaligus ke Jenis Seleksi Target, tanpa perlu
		// membuka & menyimpan data tiap mahasiswa satu per satu.
		button = new MyToolbarbuttonConfig("Sinkronkan Jenis Seleksi", "/img/refresh.png");
		toolbar.appendChild(button);
		button.setTooltiptext(
				"Sesuaikan Jenis Seleksi seluruh anggota manual kelompok ini ke \"Jenis Seleksi Target\" kelompok (agar Setting Biaya/tagihan ikut sesuai)");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final ais.database.model.JenisSeleksi jenisSeleksiTarget = kelompokCalonMahasiswa
						.getJenisSeleksiTarget();
				if (jenisSeleksiTarget == null) {
					MyMessageboxConfig.show(
							"Kelompok ini belum memiliki \"Jenis Seleksi Target\" (kosong), sehingga tidak ada yang bisa disinkronkan. Langkah yang dapat dilakukan: (1) buka pengaturan kelompok ini (tombol edit di layar sebelumnya); (2) isi kolom \"Jenis Seleksi Target\" dengan Jenis Seleksi yang sesuai (mis. Jenis Seleksi beasiswa Gratispol); (3) simpan, lalu ulangi klik tombol \"Sinkronkan Jenis Seleksi\" ini. Jika Jenis Seleksi yang sesuai tidak tersedia di pilihan, hubungi Administrator/Pengembang Sistem untuk menambahkannya terlebih dahulu.",
							"Jenis Seleksi Target Belum Diisi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}
				MyMessageboxConfig.show(
						"Sesuaikan Jenis Seleksi SELURUH anggota manual kelompok ini menjadi \"" + jenisSeleksiTarget.getNama()
								+ "\"? Mahasiswa yang Jenis Seleksi-nya sudah sesuai akan dilewati. Setelah proses ini, disarankan membuka ulang layar tagihan tiap mahasiswa yang berubah dan menekan tombol \"Reset\" agar tagihan ikut dihitung ulang sesuai Setting Biaya yang benar.",
						"Konfirmasi Sinkronisasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
									return;
								}
								int jumlahDisesuaikan = 0;
								int jumlahDilewati = 0;
								java.util.List<String> gagal = new java.util.ArrayList<String>();
								try {
									Session session = HibernateUtil.currentSession();
									@SuppressWarnings("unchecked")
									List<BiodataCalonMahasiswa> anggota = ConstantValues.simpleList(
											session.createCriteria(BiodataCalonMahasiswa.class)
													.add(Restrictions.eq("kelompokCalonMahasiswa", kelompokCalonMahasiswa)),
											BiodataCalonMahasiswa.class);
									for (BiodataCalonMahasiswa biodataCalonMahasiswa : anggota) {
										try {
											if (jenisSeleksiTarget.equals(biodataCalonMahasiswa.getJenisSeleksiDipilih())) {
												jumlahDilewati++;
												continue;
											}
											biodataCalonMahasiswa.setJenisSeleksiDipilih(jenisSeleksiTarget);
											Common.refreshUpdate(session, biodataCalonMahasiswa);
											jumlahDisesuaikan++;
										} catch (Exception exSatu) {
											ais.common.ErrorAuditUtil.record(exSatu,
													"auto-audit(sinkron-jenis-seleksi-kelompok) src/ais/action/master/pmb/KelompokCalonMahasiswaDetailAction.java biodataCalonMahasiswaId="
															+ biodataCalonMahasiswa.getId());
											gagal.add((biodataCalonMahasiswa.getNama() == null ? "(tanpa nama)"
													: biodataCalonMahasiswa.getNama()) + " - " + exSatu.getMessage());
										}
									}
								} catch (Exception e) {
									ais.common.ErrorAuditUtil.record(e,
											"auto-audit(sinkron-jenis-seleksi-kelompok) src/ais/action/master/pmb/KelompokCalonMahasiswaDetailAction.java");
									ais.common.PesanFormalHelper.tampilkanGagalException(
											"sinkronisasi Jenis Seleksi anggota kelompok", e,
											new String[] {
													"Coba ulangi beberapa saat lagi.",
													"Jika masih gagal, hubungi Administrator/Pengembang Sistem dengan melampirkan tangkapan layar (screenshot) layar ini." });
									return;
								}

								loadData(null);

								StringBuilder pesan = new StringBuilder();
								pesan.append("Sinkronisasi selesai. Berhasil disesuaikan: ").append(jumlahDisesuaikan)
										.append(" mahasiswa. Dilewati (sudah sesuai): ").append(jumlahDilewati)
										.append(" mahasiswa.");
								if (!gagal.isEmpty()) {
									pesan.append("\n\nGAGAL untuk ").append(gagal.size())
											.append(" mahasiswa berikut (penyebab error tercatat di setiap baris):");
									for (String g : gagal) {
										pesan.append("\n- ").append(g);
									}
									pesan.append(
											"\n\nMohon periksa data mahasiswa tsb secara manual. Jika penyebabnya tidak jelas, hubungi Administrator/Pengembang Sistem dengan melampirkan tangkapan layar (screenshot) pesan ini.");
								}
								pesan.append(
										"\n\nLANGKAH SELANJUTNYA: buka ulang layar tagihan tiap mahasiswa yang berhasil disesuaikan, lalu tekan tombol \"Reset\" pada layar tagihannya agar nilai tagihan dihitung ulang sesuai Setting Biaya yang benar (perubahan Jenis Seleksi TIDAK otomatis menghitung ulang tagihan yang sudah terlanjur terbentuk).");

								MyMessageboxConfig.show(pesan.toString(),
										gagal.isEmpty() ? "Sinkronisasi Berhasil" : "Sinkronisasi Selesai (Sebagian Gagal)",
										MyMessageboxConfig.OK,
										gagal.isEmpty() ? MyMessageboxConfig.INFORMATION : MyMessageboxConfig.EXCLAMATION);
							}
						});
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal Lahir");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Asal Sekolah/Kampus");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No. Registrasi, Ujian, NIM");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan Prodi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Manual");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	/**
	 * Susun penjelasan KENAPA seorang calon menempel di kelompok ini secara otomatis,
	 * dengan menyebut aturan mana yang sedang menentukan Status Awal-nya.
	 *
	 * <p>Urutan pemeriksaan mengikuti {@code BiodataCalonMahasiswa.getStatusAwalMahasiswa()}:
	 * kelompok manual menang lebih dulu, lalu afiliasi, lalu default gelombang. Operator perlu
	 * tahu yang mana supaya tidak menebak-nebak - tiap penyebab butuh tindakan yang berbeda.</p>
	 */
	/**
	 * Cari kelompok LAIN di gelombang yang sama yang rentang skornya beririsan dgn kelompok ini.
	 *
	 * <p><b>Kenapa penting.</b> {@code validasiStatusAwalMahasiswa} menelusuri kelompok yang
	 * diurutkan {@code skorSampai} MENAIK lalu berhenti di kecocokan PERTAMA. Jadi bila dua
	 * kelompok sama-sama memuat sebuah skor, yang menang adalah yang batas atasnya lebih KECIL
	 * - bukan yang lebih tepat atau yang dibuat lebih dulu. Batasnya pun inklusif di kedua ujung
	 * ({@code skor >= mulai && skor <= sampai}), sehingga rentang bersambung seperti 0-204 dan
	 * 204-500 pun beririsan di angka 204.</p>
	 *
	 * <p>Akibatnya calon bisa jatuh ke kelompok yang tidak dikehendaki tanpa pesan apa pun.
	 * Irisan itu ditampilkan di sini supaya ketahuan sebelum jadi keluhan data.</p>
	 *
	 * @return teks peringatan siap tampil, atau {@code null} bila tidak ada irisan
	 */
	@SuppressWarnings("unchecked")
	private String periksaTumpangTindihSkor() {
		try {
			if (!kelompokCalonMahasiswa.getAktifkanPenggunaanSkor()) {
				return null;
			}
			int mulai = kelompokCalonMahasiswa.getSkorMulai();
			int sampai = kelompokCalonMahasiswa.getSkorSampai();

			List<KelompokCalonMahasiswa> lainnya = HibernateUtil.currentSession()
					.createCriteria(KelompokCalonMahasiswa.class)
					.add(Restrictions.eq("gelombangPendaftaran", kelompokCalonMahasiswa.getGelombangPendaftaran()))
					.addOrder(Order.asc("skorSampai")).list();

			StringBuilder irisan = new StringBuilder();
			for (KelompokCalonMahasiswa lain : lainnya) {
				if (lain == null || lain.getId() == null
						|| lain.getId().equals(kelompokCalonMahasiswa.getId())
						|| !lain.getAktifkanPenggunaanSkor()) {
					continue;
				}
				int mulaiLain = lain.getSkorMulai();
				int sampaiLain = lain.getSkorSampai();
				// Beririsan bila TIDAK (habis sebelum yg lain mulai) dan TIDAK (mulai setelah yg lain habis).
				if (sampai < mulaiLain || sampaiLain < mulai) {
					continue; // tidak beririsan
				}
				// Yang menang adalah batas ATAS terkecil; seri -> urutan hasil query.
				boolean kelompokIniYangMenang = sampai < sampaiLain;
				irisan.append("<br>&nbsp;&nbsp;- beririsan dengan <b>")
						.append(lain.getStatusAwalMahasiswa() == null ? "(tanpa status)"
								: lain.getStatusAwalMahasiswa().getNama())
						.append("</b> (skor ").append(mulaiLain).append(" s/d ").append(sampaiLain).append("); pada skor")
						.append(" yang dimuat keduanya, calon jatuh ke <b>")
						.append(kelompokIniYangMenang ? "kelompok ini"
								: (lain.getStatusAwalMahasiswa() == null ? "kelompok tsb"
										: lain.getStatusAwalMahasiswa().getNama()))
						.append("</b>.");
			}
			if (irisan.length() == 0) {
				return null;
			}
			return "<b>Perhatian:</b> rentang skor kelompok ini (<b>" + mulai + " s/d " + sampai + "</b>)"
					+ " tumpang tindih dengan kelompok lain di gelombang yang sama:" + irisan.toString()
					+ "<br>Penentu kemenangan adalah BATAS ATAS yang lebih kecil, bukan urutan pembuatan"
					+ " kelompok - dan batasnya inklusif, jadi rentang bersambung (mis. 0-204 dan 204-500)"
					+ " tetap beririsan di angka sambungannya. Rapikan rentangnya bila pengelompokannya"
					+ " tidak sesuai harapan.";
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "KelompokCalonMahasiswaDetailAction.periksaTumpangTindihSkor");
			return null;
		}
	}

	private String jelaskanKeanggotaanOtomatis(BiodataCalonMahasiswa calon) {
		StringBuilder pesan = new StringBuilder();
		pesan.append("\"").append(calon.getNama() == null ? "Calon ini" : calon.getNama()).append("\"")
				.append(" TIDAK disimpan sebagai anggota kelompok ini, jadi tidak ada yang bisa dihapus.")
				.append(" Dia muncul di sini karena Status Awal-nya kebetulan cocok dengan kelompok ini.");

		String penyebab = null;
		String tindakan = null;
		try {
			ais.database.model.AfiliasiCalonMahasiswa afiliasi = calon.getAfiliasiCalonMahasiswa();
			ais.database.model.GelombangPendaftaran gelombang = calon.getGelombangPendaftaran();

			if (afiliasi != null && afiliasi.getStatusAwalMahasiswa() != null) {
				penyebab = "Status Awal-nya berasal dari Afiliasi \"" + afiliasi.getNama()
						+ "\" yang disetel ke \"" + afiliasi.getStatusAwalMahasiswa().getNama() + "\".";
				tindakan = "Lepaskan calon dari afiliasi tersebut (menu Afiliasi Calon Mahasiswa),"
						+ " atau langsung pindahkan dia lewat langkah di bawah - kelompok yang ditetapkan"
						+ " manual selalu menang atas afiliasi.";
			} else if (gelombang != null && gelombang.getHarusIkutStatusAwalDefault()
					&& gelombang.getStatusAwalMahasiswaDefault() != null) {
				penyebab = "Gelombang \"" + gelombang.getNama() + "\" mencentang \"Calon mahasiswa harus"
						+ " mengikuti status awal default\" dengan default \""
						+ gelombang.getStatusAwalMahasiswaDefault().getNama() + "\", sehingga seluruh calon"
						+ " gelombang itu dikunci ke sini selama belum dikelompokkan manual.";
				tindakan = "Lepas centang tersebut pada pengaturan gelombang bila pengelompokan ini tidak"
						+ " dikehendaki, atau pindahkan calon ini saja lewat langkah di bawah.";
			} else if (kelompokCalonMahasiswa.getAktifkanPenggunaanSkor()
					&& calon.getTotalSkor() != null
					&& kelompokCalonMahasiswa.getSkorMulai() != null
					&& kelompokCalonMahasiswa.getSkorSampai() != null
					&& calon.getTotalSkor() >= kelompokCalonMahasiswa.getSkorMulai()
					&& calon.getTotalSkor() <= kelompokCalonMahasiswa.getSkorSampai()) {
				/*
				 * Penyebab paling sering, dan paling mudah terlewat: kelompok ini menyaring
				 * berdasar SKOR, dan skor calon jatuh di rentangnya. Selama dia belum masuk
				 * kelompok mana pun, tombol "Proses Ulang" akan MENGEMBALIKANNYA ke sini
				 * berulang kali (lihat KelompokCalonMahasiswaAction.validasiStatusAwalMahasiswa).
				 */
				penyebab = "Kelompok ini menyaring berdasarkan SKOR (rentang "
						+ kelompokCalonMahasiswa.getSkorMulai() + " s/d " + kelompokCalonMahasiswa.getSkorSampai()
						+ "), dan skor calon ini " + calon.getTotalSkor() + " - masuk rentang tersebut.";
				tindakan = "Selama calon ini belum masuk kelompok mana pun, tombol \"Proses Ulang\" akan"
						+ " terus mengembalikannya ke sini. Pindahkan dia lewat langkah di bawah (kelompok yang"
						+ " ditetapkan manual TIDAK ikut diproses ulang), atau perbaiki rentang skor kelompok ini.";
			} else {
				penyebab = "Status Awal yang tersimpan pada data calon ini memang sama dengan status"
						+ " kelompok ini, dan dia belum pernah dimasukkan ke kelompok mana pun."
						+ (kelompokCalonMahasiswa.getParameterTambahanInds().trim().isEmpty() ? ""
								: " Kelompok ini juga punya kriteria parameter tambahan yang bisa mencocokkannya"
										+ " lagi setiap kali \"Proses Ulang\" dijalankan.");
				tindakan = "Pindahkan lewat langkah di bawah.";
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "KelompokCalonMahasiswaDetailAction.jelaskanKeanggotaanOtomatis");
			penyebab = "Penyebab pastinya tidak dapat dibaca saat ini.";
			tindakan = "Pindahkan lewat langkah di bawah.";
		}

		pesan.append("\n\nPENYEBAB: ").append(penyebab);
		pesan.append("\n\nYANG PERLU DILAKUKAN: ").append(tindakan);
		pesan.append("\n\nCARA MEMINDAHKAN: buka kelompok TUJUAN, tekan \"Ambil Data Calon Mahasiswa Manual\",")
				.append(" lalu pilih calon ini. Status Awal-nya ikut disesuaikan ke kelompok tujuan dan dia")
				.append(" otomatis hilang dari daftar ini.");
		pesan.append("\n\nCATATAN TAGIHAN: Setting Biaya mencocokkan tagihan berdasarkan JENIS SELEKSI,")
				.append(" bukan kelompok. Setelah dipindah, pastikan kelompok tujuan punya \"Jenis Seleksi")
				.append(" Target\", tekan \"Sinkronkan Jenis Seleksi\", lalu buka layar tagihan calon ini dan")
				.append(" tekan \"Reset\" - tagihan yang sudah terlanjur terbentuk TIDAK dihitung ulang sendiri.");
		return pesan.toString();
	}

	/**
	 * Terangkan dari mana anggota kelompok ini berasal, dan kapan barisnya tidak bisa dihapus.
	 *
	 * <p>Daftar ini gabungan dua jalur (lihat {@link #initCriteria(boolean)}): jalur MANUAL
	 * (calon di-assign eksplisit ke kelompok ini) dan jalur OTOMATIS (calon yang gelombang +
	 * Status Awal-nya kebetulan cocok, dan belum masuk kelompok mana pun). Tombol "Hapus
	 * Manual" hanya berlaku untuk jalur manual - jalur otomatis tidak menyimpan apa pun yang
	 * bisa dihapus. Tanpa penjelasan ini operator mengira tombolnya rusak.</p>
	 *
	 * <p>Bila gelombang mencentang "Calon mahasiswa harus mengikuti status awal default" dan
	 * defaultnya sama dengan status kelompok ini, seluruh calon gelombang tsb DIKUNCI ke sini
	 * selama belum dipindah manual - itu ditandai terpisah supaya tidak dikira kesalahan data.</p>
	 */
	private void tampilkanCatatanKeanggotaan(ais.ui.util.MyDiv groupbox) {
		try {
			StringBuilder catatan = new StringBuilder();
			catatan.append("Baris dengan kolom <b>Manual = Tidak</b> masuk ke sini secara OTOMATIS")
					.append(" (gelombang + Status Awal-nya cocok), bukan karena disimpan di kelompok ini.")
					.append(" Baris seperti itu memang tidak punya tombol Hapus - untuk memindahkannya,")
					.append(" buka kelompok tujuan lalu pakai tombol <b>Ambil Data Calon Mahasiswa Manual</b>.");

			ais.database.model.GelombangPendaftaran gelombang = kelompokCalonMahasiswa.getGelombangPendaftaran();
			ais.database.model.StatusAwalMahasiswa statusKelompok = kelompokCalonMahasiswa.getStatusAwalMahasiswa();
			boolean dikunciGelombang = gelombang != null && gelombang.getHarusIkutStatusAwalDefault()
					&& gelombang.getStatusAwalMahasiswaDefault() != null && statusKelompok != null
					&& gelombang.getStatusAwalMahasiswaDefault().getId() != null
					&& gelombang.getStatusAwalMahasiswaDefault().getId().equals(statusKelompok.getId());

			String peringatanTumpangTindih = periksaTumpangTindihSkor();
			if (peringatanTumpangTindih != null) {
				catatan.append("<br><br>").append(peringatanTumpangTindih);
			}

			if (dikunciGelombang) {
				catatan.append("<br><br><b>Perhatian:</b> gelombang \"").append(gelombang.getNama())
						.append("\" mencentang <b>\"Calon mahasiswa harus mengikuti status awal default\"</b>")
						.append(" dengan default <b>").append(statusKelompok.getNama()).append("</b>.")
						.append(" Karena itu SELURUH calon gelombang tsb dikunci ke kelompok ini selama belum")
						.append(" dipindah manual. Bila pengelompokan ini tidak dikehendaki, buka pengaturan")
						.append(" gelombang dan lepas centang tersebut, atau ubah Status Awal Mahasiswa Default-nya.");
			}

			ais.ui.util.MyHtml html = new ais.ui.util.MyHtml("<div style=\"font-size:11px;line-height:1.5;"
					+ (dikunciGelombang || peringatanTumpangTindih != null
							? "color:#92400e;background:#fef3c7;border:1px solid #fcd34d;"
							: "color:#334155;background:#f1f5f9;border:1px solid #cbd5e1;")
					+ "border-radius:6px;padding:8px 10px;margin:6px 0;\">" + catatan.toString() + "</div>");
			html.setParent(groupbox);
		} catch (Exception e) {
			// Catatan bantu saja - jangan pernah menggagalkan render halaman.
			ais.common.ErrorAuditUtil.record(e, "KelompokCalonMahasiswaDetailAction.tampilkanCatatanKeanggotaan");
		}
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("noUjian", pencarian.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("nama", pencarian.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("noRegistrasi", pencarian.getValue().trim(),
												MatchMode.ANYWHERE))))

				.addOrder(Order.desc("id"))
				.add(Restrictions.or(
						// Cabang manual: calon yang di-assign eksplisit ke kelompok ini
						Restrictions.eq("kelompokCalonMahasiswa", kelompokCalonMahasiswa),
						// Cabang otomatis: calon yang cocok berdasar status + gelombang,
						// HANYA yang belum di-assign manual ke kelompok manapun.
						// Tanpa guard isNull, calon yang di-pindah manual ke kelompok lain
						// tetap muncul di sini karena kolom statusAwalMahasiswa di DB
						// tidak ikut berubah saat assignment manual dilakukan.
						// Hibernate 3.x: Restrictions.and() hanya terima 2 argumen → nested.
						Restrictions.and(
								Restrictions.isNull("kelompokCalonMahasiswa"),
								Restrictions.and(
										Restrictions.eq("gelombangPendaftaran",
												kelompokCalonMahasiswa.getGelombangPendaftaran()),
										Restrictions.eq("statusAwalMahasiswa",
												kelompokCalonMahasiswa.getStatusAwalMahasiswa())))));
	}

}
