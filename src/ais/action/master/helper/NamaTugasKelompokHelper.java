package ais.action.master.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zul.Filedownload;
import org.json.JSONObject;
import ais.database.model.FormatNilai;
import ais.database.model.Perkuliahan;
import ais.database.model.Tugas;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.SyaratUjianAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.NamaTugasKelompokDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.NamaTugasKelompok;
import ais.database.model.NamaTugasKelompokPunyaMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Layar/komponen ZK (dipanggil dari {@code TugasKelompokAction} lewat {@link #display}) yang
 * mengelola DAFTAR KELOMPOK ({@link NamaTugasKelompok}, mis. "Kelompok 1", "Kelompok 2") di bawah
 * satu {@link TugasKelompok} (tugas kuliah berkelompok — bisa berbasis perkuliahan, KKN, atau PKL)
 * beserta ANGGOTA tiap kelompok ({@link NamaTugasKelompokPunyaMahasiswa}). Dua sisi pengguna:
 * dosen/admin ({@link #bolehKelolaKelompok()} true) bisa menambah/mengedit/menghapus kelompok,
 * mengelola anggota, serta mengunduh/mengunggah data massal via Excel; mahasiswa hanya melihat
 * daftar kelompok dan bisa "Gabung Kelompok" sendiri (bila kuota belum penuh dan belum tergabung
 * di kelompok lain pada tugas yang sama) serta mengunggah berkas tugas kelompoknya.
 *
 * <p><b>Kuis "siapa boleh kelola" ({@link #bolehKelolaKelompok()}).</b> Diperkaya menjadi
 * pemeriksaan terpusat: HANYA {@code true} bila pengguna login BUKAN mahasiswa, BUKAN siswa, BUKAN
 * calon siswa, BUKAN calon mahasiswa (biodata), dan BUKAN peserta kursus — sebelumnya hanya
 * memeriksa "bukan mahasiswa" sehingga peran pelajar lain (siswa/calon siswa/dst.) keliru masih
 * melihat kontrol kelola. Dipakai konsisten baik di modul kuliah maupun sekolah.</p>
 *
 * <p><b>Dua mode data (Excel biasa vs. OBE).</b> Bila {@link TugasKelompok} berasal dari kurikulum
 * OBE (Outcome-Based Education — dicek via {@link #ambilObeFormatNilais()}: kurikulum
 * {@code apakahObe()} DAN {@code tugasKelompok.getFormatNilais()} bukan format lama {@code JSON}
 * biasa), tombol Download/Upload Data Kelompok diganti dari format standar
 * ({@link #uploadDataNamaTugasKelompokPunyaMahasiswa}, kolom nama/NIM/nilai/keterangan/nama-mhs)
 * ke format khusus OBE ({@link #downloadDataKelompokObe}/{@link #uploadDataKelompokObe}, nilai
 * per-{@link FormatNilai} disimpan sebagai JSON di kolom {@code TugasKelompok.keteranganNilai},
 * BUKAN sebagai baris {@link NamaTugasKelompokPunyaMahasiswa} terpisah per komponen nilai).</p>
 *
 * <p><b>Bukan tanggung jawab kelas ini:</b> merender daftar ANGGOTA per kelompok secara detail
 * (didelegasikan ke {@code NamaTugasKelompokPunyaMahasiswaHelper}, satu instance BARU per baris
 * grid — lihat catatan "PERBAIKAN cross-talk antar kelompok" di
 * {@link DetailTugasKelompokRenderer#render}), maupun validasi syarat pengumpulan tugas
 * (didelegasikan ke {@code SyaratUjianAction.checkSyaratSyaratUjian}).</p>
 */
public class NamaTugasKelompokHelper implements DataLoader {

	/** Popup form tambah/edit satu {@link NamaTugasKelompok}, dibangun oleh {@link #init}. */
	private MyWindow addWindow;
	/** Grid daftar kelompok, diisi ulang oleh {@link #loadData}. */
	private MyGrid grid;
	/** Tugas kelompok induk yang daftar kelompoknya sedang dikelola; diset di {@link #display}. */
	private TugasKelompok tugasKelompok;
	// private boolean delete = false;
	// private boolean add = false;
	/** Kelompok yang sedang diedit di {@link #addWindow} (baru atau existing); diset oleh {@link #init}, dipakai {@link #onSave}. */
	private NamaTugasKelompok namaTugasKelompok;
	/** Kotak isian nama kelompok pada popup tambah/edit. */
	private Textbox nama;
	/** Selalu {@code true} pada instance ini (tidak pernah diset {@code false} di kelas ini) — menggerbangi visibilitas tombol edit bersama {@link #bolehKelolaKelompok()}. */
	private boolean edit = true;
	/** Pengguna yang sedang login, diset di {@link #display}. */
	private Tbmuser tbmuser;
	/** Mahasiswa konteks (bila layar dibuka dari sisi mahasiswa kuliah), diteruskan ke {@code NamaTugasKelompokPunyaMahasiswaHelper}. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa konteks (bila layar dibuka dari sisi biodata calon mahasiswa), diteruskan ke {@code NamaTugasKelompokPunyaMahasiswaHelper}. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;

	/**
	 * Menyimpan konteks pemilik tampilan (untuk membedakan sudut pandang mahasiswa aktif vs.
	 * calon mahasiswa saat merender status keanggotaan/tombol gabung di
	 * {@link DetailTugasKelompokRenderer}) — belum memuat data apa pun.
	 *
	 * @param mahasiswa              mahasiswa konteks, atau {@code null} bila tidak relevan
	 * @param biodataCalonMahasiswa  calon mahasiswa konteks, atau {@code null} bila tidak relevan
	 */
	public NamaTugasKelompokHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * <h3>Penentu hak kelola Daftar Kelompok (pengelola vs pelajar/peserta)</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> memutuskan apakah pengguna yang sedang membuka
	 * "Daftar Kelompok" berhak MENGELOLA (menambah kelompok, mengunggah/mengunduh data, mengubah, dan
	 * menghapus) &mdash; yaitu dosen/guru/admin &mdash; atau hanya seorang pelajar/peserta yang cukup
	 * melihat dan (untuk mahasiswa) bergabung ke kelompoknya. Sebelumnya kontrol kelola hanya
	 * disembunyikan dari mahasiswa (memeriksa "bukan mahasiswa"), sehingga <b>siswa</b>, <b>calon
	 * siswa</b>, <b>calon mahasiswa (biodata)</b>, dan <b>peserta kursus</b> keliru masih melihat
	 * tombol Tambah/Unggah/Ubah/Hapus. Dengan satu pemeriksaan terpusat ini, semua peran pelajar
	 * diperlakukan sama sehingga tampilan aman dan konsisten di modul kuliah maupun sekolah.</p>
	 *
	 * <p>Bergantung pada pengguna yang login ({@code Common.getCurrentUser()}) karena helper ini bisa
	 * dibuka baik dalam konteks kuliah maupun sekolah; sesi tanpa pengguna ({@code null}) sengaja
	 * dianggap bukan pengelola demi keamanan tampilan.</p>
	 *
	 * @return {@code true} HANYA bila pengguna login adalah pengelola (bukan mahasiswa/siswa/calon
	 *         siswa/calon mahasiswa/peserta kursus)
	 */
	private boolean bolehKelolaKelompok() {
		Tbmuser u = Common.getCurrentUser();
		return u != null && u.getMahasiswa() == null && u.getSiswa() == null && u.getCalonSiswa() == null
				&& u.getBiodataCalonMahasiswa() == null && u.getPesertaKursus() == null;
	}

	/**
	 * Renderer baris grid daftar kelompok — instance BARU dipasang tiap {@link #loadData}
	 * (bukan dipakai ulang lintas pemuatan). Per baris {@link #render} menampilkan: link "Revisi"
	 * (riwayat Envers), nama kelompok, jumlah peserta saat ini, kuota (editable inline oleh
	 * pengelola via {@link ais.ui.util.MyIntbox}, read-only sebagai angka bagi mahasiswa),
	 * kontrol upload/download berkas tugas kelompok (bila masih dalam rentang waktu tugas dan
	 * pengguna berhak — anggota kelompok terkait, atau pengelola), dan tombol aksi (Kelola
	 * Anggota/Edit/Hapus untuk pengelola; Gabung Kelompok untuk mahasiswa yang belum tergabung di
	 * kelompok manapun pada tugas ini).
	 * <p>
	 * Setiap baris membuat instance BARU {@code NamaTugasKelompokPunyaMahasiswaHelper}
	 * (bukan berbagi satu instance untuk semua baris) — dicatat eksplisit di kode sebagai
	 * "PERBAIKAN cross-talk antar kelompok": helper anggota menyimpan state per-kelompok
	 * (grid &amp; kelompok aktif) di field instance, sehingga satu instance bersama akan
	 * saling menimpa antar baris saat detail beberapa kelompok dibuka, membuat refresh anggota
	 * salah sasaran ke kelompok lain.
	 *
	 * @see NamaTugasKelompokHelper
	 */
	class DetailTugasKelompokRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris {@link NamaTugasKelompok} sesuai deskripsi lengkap di Javadoc kelas
		 * {@link DetailTugasKelompokRenderer}. Detail anggota ({@link MyDetail}) sengaja dibuat
		 * TERTUTUP secara default ({@code setOpen(false)}) — membuka detail semua kelompok
		 * sekaligus pada grid besar (10+ kelompok) bisa membuat render berat menahan event paging
		 * sehingga halaman berikutnya tampak tidak bisa dibuka; pengguna membuka anggota per
		 * kelompok lewat ikon panah Detail atau tombol "Kelola Anggota".
		 *
		 * @param row  baris grid ZK tujuan
		 * @param data instance {@link NamaTugasKelompok} yang dirender
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			Session session = ambilSessionAktif();
			final NamaTugasKelompok namaTugasKelompok = (NamaTugasKelompok) data;

			// PERBAIKAN cross-talk antar kelompok:
			// Helper anggota kelompok DIBUAT PER-BARIS (bukan satu instance dipakai bersama seluruh
			// baris seperti sebelumnya). NamaTugasKelompokPunyaMahasiswaHelper menyimpan state
			// per-kelompok di field instance (grid & namaTugasKelompok). Bila satu instance dipakai
			// semua baris, setiap display()/loadData() saling menimpa field tsb, sehingga menambah
			// atau me-refresh anggota di kelompok ke-2 dst. justru merender ke grid kelompok lain —
			// daftar nama tidak ter-update sampai halaman dibuka ulang. Instance per-baris membuat
			// setiap kelompok punya grid & konteks sendiri, jadi refresh tepat pada kelompoknya.
			final NamaTugasKelompokPunyaMahasiswaHelper namaTugasKelompokPunyaMahasiswaHelper = new NamaTugasKelompokPunyaMahasiswaHelper(
					mahasiswa, biodataCalonMahasiswa);

			final MyDetail detail = new MyDetail();
			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						namaTugasKelompokPunyaMahasiswaHelper.display(namaTugasKelompok, detail);
					}

				}
			};
			detail.addEventListener("onOpen", eventListener);

			detail.setParent(row);
			// Jangan membuka seluruh anggota untuk 10 kelompok sekaligus. Selain membuat modal
			// sangat panjang, render detail yang berat dapat menahan event paging sehingga
			// halaman 2 (Kelompok 11 dan seterusnya) terlihat tidak dapat dibuka. Pengguna
			// tetap dapat membuka anggota kelompok yang diperlukan lewat ikon panah Detail.
			detail.setOpen(false);

			boolean bukan = false;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(NamaTugasKelompok.class, namaTugasKelompok, namaTugasKelompok.getNama()))
					.setParent(row);
			Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
			boolean masihAdaWaktu = ((namaTugasKelompok.getTugasKelompok().getSelesai() == null
					|| namaTugasKelompok.getTugasKelompok().getSelesai().after(kemarin.getTime()))
					&& (namaTugasKelompok.getTugasKelompok().getMulai() == null
							|| namaTugasKelompok.getTugasKelompok().getMulai().before(kemarin.getTime())));
			boolean sudahTampil = false;
			if (masihAdaWaktu) {
				if (tbmuser.getMahasiswa() == null || (tbmuser.getMahasiswa() != null
						&& ((Number) session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
								.add(Restrictions.eq("namaTugasKelompok", namaTugasKelompok))
								.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue() != 0)) {
					Hbox hbox = new Hbox();
					List<String> warnings = new ArrayList<String>();
					if (tugasKelompok != null && mahasiswa != null
							&& tugasKelompok.getSyaratMengumpulkanTugas() != null) {
						Detailperkuliahan detailperkuliahan = mahasiswa
								.ambilDetailperkuliahan(tugasKelompok.getPerkuliahan());
						if (detailperkuliahan == null) {
							mahasiswa.reInitDetailperkuliahan(HibernateUtil.currentSession());
							detailperkuliahan = mahasiswa.ambilDetailperkuliahan(tugasKelompok.getPerkuliahan());
						}
						if (detailperkuliahan != null) {
							SyaratUjianAction.checkSyaratSyaratUjian(tugasKelompok.getSyaratMengumpulkanTugas(),
									tugasKelompok.getPerkuliahan(), mahasiswa, detailperkuliahan.getSemester(),
									tugasKelompok.getJudul(), warnings);
						}
					}
					if (!warnings.isEmpty()) {
						new MyLabelAgakKecilBold(warnings.get(0)).setParent(a);
					} else {

						if (syaratAlert != null && !syaratAlert.isEmpty()) {
							String s = "Syarat Upload Tugas Kelompok: <br>";
							for (String dd : syaratAlert) {
								s += s.isEmpty() ? dd : "<br>" + dd;
							}
							new MyHtml(s).setParent(a);
						} else {
							sudahTampil = true;
							LampiranLain.createDownloadUploadFileLain(hbox, namaTugasKelompok.getId(),
									NamaTugasKelompok.class.getName(), "Tugas", false, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

										}
									}, null, false, false, false, masihAdaWaktu, null);
							hbox.setParent(a);
						}
					}

				} else {
					bukan = true;
					new MyLabelAgakKecilBoldMerah("Anda bukan di kelompok ini").setParent(a);
				}
			} else {
				new MyLabelAgakKecilBoldMerah("Bukan waktu-nya upload tugas").setParent(a);
			}

			if (!sudahTampil && (!bukan || tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null)) {
				Hbox hbox = new Hbox();
				LampiranLain.createDownloadUploadFileLain(hbox, namaTugasKelompok.getId(),
						NamaTugasKelompok.class.getName(), "Tugas", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false, masihAdaWaktu, null);
				hbox.setParent(a);
			}

			// Session mungkin ditutup oleh helper LampiranLain di atas — ambil ulang
			session = ambilSessionAktif();
			Integer count = ((Number) session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
					.add(Restrictions.eq("namaTugasKelompok", namaTugasKelompok)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();

			new Label(Common.numberFormat.get().format(count)).setParent(row);

			if (tbmuser != null && tbmuser.getMahasiswa() != null
					&& !tugasKelompok.getMhsYgTidakIkut().contains("," + tbmuser.getMahasiswa().getId() + ",")) {
				Hbox hbox = new Hbox();
				hbox.setParent(row);
				new Label(Common.numberFormat.get().format(namaTugasKelompok.getKuota())).setParent(hbox);

				int namaTugasKelompokMhs = ((Number) (session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
						.createAlias("namaTugasKelompok", "namaTugasKelompok")
						.add(Restrictions.eq("namaTugasKelompok.tugasKelompok", tugasKelompok))
						.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa())).setProjection(Projections.rowCount())
						.setMaxResults(1).uniqueResult())).intValue();

				if (namaTugasKelompokMhs == 0) {
					MyToolbarbuttonConfig config = new MyToolbarbuttonConfig("Gabung Kelompok", "/img/group.gif");
					config.setParent(hbox);
					config.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							Session session = HibernateUtil.currentSession();
							Integer count = ((Number) session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
									.add(Restrictions.eq("namaTugasKelompok", namaTugasKelompok))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							if (count >= namaTugasKelompok.getKuota()) {
								MyMessageboxConfig.showFormat(
										"Mohon maaf, kuota untuk kelompok \"{V1}\" telah penuh sehingga Anda tidak dapat bergabung. Langkah yang dapat dilakukan: (1) pilih kelompok lain yang kuotanya masih tersedia; (2) hubungi dosen atau pengelola apabila memerlukan penyesuaian kuota.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
										namaTugasKelompok.getNama());
								return;
							}

							NamaTugasKelompok namaTugasKelompokMhs = (NamaTugasKelompok) (session
									.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
									.createAlias("namaTugasKelompok", "namaTugasKelompok")
									.add(Restrictions.eq("namaTugasKelompok.tugasKelompok", tugasKelompok))
									.add(Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()))
									.setProjection(Projections.property("namaTugasKelompok")).setMaxResults(1)
									.uniqueResult());
							if (namaTugasKelompokMhs != null) {
								MyMessageboxConfig.showFormat(
										"Anda telah tergabung di dalam kelompok \"{V1}\", sehingga tidak dapat bergabung ke kelompok lain. Apabila Anda merasa hal ini keliru, silakan hubungi dosen atau pengelola untuk dilakukan penyesuaian.",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
										namaTugasKelompokMhs.getNama());
								return;
							}

							MyMessageboxConfig.show(Common.pesan(
									"Apakah Bapak/Ibu yakin ingin bergabung ke kelompok \"{V1}\"? Setelah bergabung, Anda akan terdaftar sebagai anggota kelompok tersebut. Silakan pilih OK untuk melanjutkan atau Batal untuk membatalkan.",
									namaTugasKelompok.getNama()),
									"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {
													NamaTugasKelompokPunyaMahasiswa kelompokPunyaMahasiswa = new NamaTugasKelompokPunyaMahasiswa();
													kelompokPunyaMahasiswa.setMahasiswa(tbmuser.getMahasiswa());
													kelompokPunyaMahasiswa.setSiswa(tbmuser.getSiswa());
													kelompokPunyaMahasiswa.setNamaTugasKelompok(namaTugasKelompok);
													HibernateUtil.currentSession().save(kelompokPunyaMahasiswa);

													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															loadData(null);
														}
													});

												} catch (Exception e) {
													Common.tampilErrorJikaAdmin(e);
													PesanFormalHelper.tampilkanGagalException(
															"bergabung ke kelompok tugas \"" + namaTugasKelompok.getNama() + "\"",
															e, new String[] {
																	"Muat ulang (refresh) halaman ini lalu coba bergabung ke kelompok kembali.",
																	"Periksa apakah kuota kelompok tersebut baru saja terisi penuh oleh mahasiswa lain.",
																	"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
															});
												}

											}

										}
									});
						}
					});

				} else if (!bukan) {
					new MyLabelAgakKecilBoldBiru("Anda tergabung di kelompok ini").setParent(a);
				}
			} else {
				final MyIntbox kuota = new MyIntbox(namaTugasKelompok.getKuota());
				kuota.setWidth("90%");
				kuota.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						namaTugasKelompok.setKuota(kuota.getValue());
						Common.refreshUpdate(namaTugasKelompok);
					}
				});
				kuota.setParent(row);
			}

			Hbox toolbar = new Hbox();
			final MyToolbarbuttonConfig kelolaAnggota = new MyToolbarbuttonConfig("Kelola Anggota",
					"/img/group.gif");
			kelolaAnggota.setVisible(bolehKelolaKelompok() && edit);
			kelolaAnggota.setTooltiptext("Tambah, lihat, atau hapus anggota kelompok");
			kelolaAnggota.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (detail.isOpen()) {
						detail.setOpen(false);
						Common.clear(detail);
						kelolaAnggota.setLabel("Kelola Anggota");
					} else {
						detail.setOpen(true);
						Common.clear(detail);
						namaTugasKelompokPunyaMahasiswaHelper.display(namaTugasKelompok, detail);
						kelolaAnggota.setLabel("Tutup Anggota");
					}
				}
			});
			kelolaAnggota.setParent(toolbar);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setVisible(bolehKelolaKelompok() && edit);
			button.setTooltiptext("Edit Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onAdd(event, namaTugasKelompok);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(bolehKelolaKelompok());
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
						"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diperhatikan bahwa data yang telah dihapus tidak dapat dikembalikan. Silakan pilih OK untuk melanjutkan penghapusan atau Batal untuk membatalkan.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(namaTugasKelompok);
											loadData(null);

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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);

		}

		/**
		 * Mengembalikan session Hibernate yang dijamin masih terbuka.
		 * Beberapa helper (LampiranLain dll.) bisa menutup session thread-local
		 * tanpa sepengetahuan pemanggil; method ini mengambil ulang session baru
		 * jika session saat ini sudah tertutup.
		 */
		private Session ambilSessionAktif() {
			Session s = HibernateUtil.currentSession();
			if (s != null && s.isOpen()) {
				return s;
			}
			s = HibernateUtil.currentNativeSession();
			if (s != null && s.isOpen()) {
				return s;
			}
			// Fallback: pakai native ThreadLocal session (dikelola & DITUTUP oleh FilterJSP di akhir
			// request). JANGAN openSession() -> objeknya lepas dari ThreadLocal sehingga FilterJSP tak
			// bisa menutupnya (render() memakainya inline tanpa finally) -> koneksi BOCOR.
			return HibernateUtil.currentNativeSession();
		}

	}

	/**
	 * Implementasi kontrak {@link DataLoader}: memuat SELURUH {@link NamaTugasKelompok} milik
	 * {@link #tugasKelompok} ini (diurutkan id, tanpa paging server-side — grid ber-paging
	 * client-side lewat {@code MyGrid}), memasang {@link DetailTugasKelompokRenderer} baru, dan
	 * mengganti model grid. Mempertahankan halaman aktif ({@code paging.getActivePage()}) sebelum
	 * &amp; sesudah {@code setModel} — {@code setModel} milik ZK secara default mengembalikan
	 * paging ke halaman pertama, yang akan mengganggu alur mengedit kelompok di halaman 2+.
	 *
	 * @param value tidak dipakai (parameter kontrak {@link DataLoader})
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		// setModel() milik ZK mengembalikan paging ke halaman pertama. Simpan posisi
		// agar setelah mengedit Kelompok 11 pengguna tetap berada di halaman 2.
		int activePage = 0;
		if (grid != null && grid.getPagingChild() != null) {
			activePage = grid.getPagingChild().getActivePage();
		}
		List<NamaTugasKelompok> namaTugasKelompok = session.createCriteria(NamaTugasKelompok.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("tugasKelompok", tugasKelompok)).list();

		ListModel strset = new SimpleListModel(namaTugasKelompok);
		grid.setRowRenderer(new DetailTugasKelompokRenderer());
		grid.setModelCheckMobile(strset);
		if (grid.getPagingChild() != null && grid.getPagingChild().getPageCount() > 0) {
			int lastPage = grid.getPagingChild().getPageCount() - 1;
			grid.getPagingChild().setActivePage(Math.min(activePage, lastPage));
		}

	}

	/** Urutan kolom format Excel standar (non-OBE) untuk download/upload data kelompok: nama kelompok, NIM, nilai, keterangan, nama mahasiswa. */
	private String[] contents = new String[] { "namaTugasKelompok", "mahasiswa.nim", "nilai", "keterangan",
			"mahasiswa.nama" };
	/** Kumpulan pesan peringatan syarat pengumpulan tugas yang berlaku (diisi pemanggil {@link #display}), ditampilkan bila upload berkas belum diizinkan. */
	private Set<String> syaratAlert;

	/**
	 * Titik masuk utama: membangun UI daftar kelompok (toolbar Tambah/Download/Upload —
	 * disembunyikan bagi non-pengelola, grid berpaging) ke dalam {@code groupbox} (dibersihkan
	 * lebih dulu). Bila mahasiswa login termasuk dalam {@code tugasKelompok.getMhsYgTidakIkut()}
	 * (daftar id dipisah koma, format {@code ",id,id,"}), tampilkan pesan "tidak perlu ikut" saja
	 * tanpa grid apa pun.
	 * <p>
	 * Tombol Download/Upload otomatis memilih format standar atau OBE (lihat Javadoc kelas) sesuai
	 * hasil {@link #ambilObeFormatNilais()}. Tombol cetak standar memakai
	 * {@link DataCriteria} inline yang punya efek samping PENTING: bila belum ada SATU PUN baris
	 * {@link NamaTugasKelompokPunyaMahasiswa} untuk tugas ini, method itu MEMBUAT baris SEMENTARA
	 * (kode barcode unik) untuk seluruh mahasiswa yang berhak (dari perkuliahan/kelompok KKN/PKL
	 * terkait, maksimal {@code Common.MAX_RESULT_500}), lalu dijadwalkan DIHAPUS lagi via
	 * {@code Common.createDefaultTimer} setelah 3 detik — trik agar template Excel unduhan
	 * langsung memuat baris kosong siap isi per mahasiswa yang berhak, tanpa benar-benar
	 * menyimpan data kelompok apa pun.
	 *
	 * @param tugasKelompok tugas kelompok yang daftar kelompoknya ditampilkan/dikelola
	 * @param groupbox      container ZK tujuan (dibersihkan lalu diisi UI layar ini)
	 * @param syaratAlert   pesan peringatan syarat pengumpulan tugas yang berlaku saat ini (diteruskan ke renderer)
	 */
	public void display(final TugasKelompok tugasKelompok, final Component groupbox, Set<String> syaratAlert) {
		this.tugasKelompok = tugasKelompok;
		this.syaratAlert = syaratAlert;
		Common.clear(groupbox);
		groupbox.appendChild(new MyCaptionStyled("Daftar Kelompok \"" + tugasKelompok.getJudul() + "\""));
		tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.getMahasiswa() != null
				&& tugasKelompok.getMhsYgTidakIkut().contains("," + tbmuser.getMahasiswa().getId() + ",")) {
			groupbox.appendChild(new MyLabelBoldMerah("Anda tidak perlu ikut tugas kelompok ini"));
		} else {
			final List<FormatNilai> obeFormatNilais = ambilObeFormatNilais();

			Toolbar toolbar = new Toolbar();
			toolbar.setVisible(bolehKelolaKelompok());
			// toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Kelompok", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					onAdd(event, new NamaTugasKelompok());
				}

			});
			button.setParent(toolbar);

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(null, new DataCriteria() {

				@SuppressWarnings("unchecked")
				@Override
				public Criteria initCriteria(boolean order) {
					// TODO Auto-generated method stub

					Session session = HibernateUtil.currentSession();
					if (((Number) session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
							.createAlias("namaTugasKelompok", "namaTugasKelompok").setProjection(Projections.rowCount())
							.add(Restrictions.eq("namaTugasKelompok.tugasKelompok", tugasKelompok)).uniqueResult())
							.intValue() == 0) {

						List<Mahasiswa> myMahasiswa = new ArrayList<Mahasiswa>();

						if (tugasKelompok.getPerkuliahan() != null) {
							myMahasiswa = session.createCriteria(Detailperkuliahan.class)
									.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
									.add(Restrictions.eq("perkuliahan", tugasKelompok.getPerkuliahan()))
									.setProjection(Projections.property("mahasiswa")).createCriteria("mahasiswa")
									.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
									.setMaxResults(Common.MAX_RESULT_500).list();
						} else if (tugasKelompok.getKelompokKkn() != null) {
							myMahasiswa = session.createCriteria(MahasiswaDapatKelompokKkn.class)
									.add(Restrictions.eq("diterima", true))
									.add(Restrictions.eq("kelompokKkn", tugasKelompok.getKelompokKkn()))
									.setProjection(Projections.property("mahasiswa")).createCriteria("mahasiswa")
									.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
									.setMaxResults(Common.MAX_RESULT_500).list();
						} else if (tugasKelompok.getKelompokPkl() != null) {
							myMahasiswa = session.createCriteria(MahasiswaDapatKelompokPkl.class)
									.add(Restrictions.eq("diterima", true))
									.add(Restrictions.eq("kelompokPkl", tugasKelompok.getKelompokPkl()))
									.setProjection(Projections.property("mahasiswa")).createCriteria("mahasiswa")
									.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
									.setMaxResults(Common.MAX_RESULT_500).list();
						}

						final String kode = Common.getGeneratedBarCode();
						for (Mahasiswa mahasiswa : myMahasiswa) {
							NamaTugasKelompokPunyaMahasiswa tugasKelompokPunyaMahasiswa = new NamaTugasKelompokPunyaMahasiswa();
							tugasKelompokPunyaMahasiswa.setMahasiswa(mahasiswa);
							tugasKelompokPunyaMahasiswa.setKode(kode);
							session.save(tugasKelompokPunyaMahasiswa);
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								HibernateUtil.currentSession().createSQLQuery(
										"delete from nama_tugas_kelompok_punya_mahasiswa where kode = '" + kode + "'")
										.executeUpdate();
							}
						}, "Loading...", false, 3000);

						return session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class).addOrder(Order.asc("id"))
								.add(Restrictions.eq("kode", kode));
					} else {

						return session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class).addOrder(Order.asc("id"))
								.createAlias("namaTugasKelompok", "namaTugasKelompok")
								.add(Restrictions.eq("namaTugasKelompok.tugasKelompok", tugasKelompok));
					}

				}
			}, "Download Data Kelompok", "/img/print.png", contents);
			toolbar.appendChild(cetakToolbarbutton);
			if (!obeFormatNilais.isEmpty()) {
				cetakToolbarbutton.setVisible(false);
				MyToolbarbuttonConfig cetakObe = new MyToolbarbuttonConfig("Download Data Kelompok", "/img/print.png");
				cetakObe.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						downloadDataKelompokObe(obeFormatNilais);
					}
				});
				toolbar.appendChild(cetakObe);
			}

			MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig(
					"Upload Data Kelompok" + Common.ukuranLabelFileUpload(), "/img/excel.png");
			upload.setUpload(Common.ukuranFileUpload());
			upload.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					UploadEvent uploadEvent = (UploadEvent) event;
					Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
					if (media.getName().toLowerCase().endsWith("xlsx")) {

						InputStream inputStream = media.getStreamData();
						// System.out.println("media = " + media);
						final File file = new File(
								Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
						// System.out.println("file = " +
						// file.getAbsolutePath());
						file.getParentFile().mkdirs();
						FileOutputStream fileOutputStream = new FileOutputStream(file);
						int c;
						while ((c = inputStream.read()) != -1) {
							fileOutputStream.write(c);
						}
						fileOutputStream.close();
						inputStream.close();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								uploadDataNamaTugasKelompokPunyaMahasiswa(file, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadData(null);
										Clients.clearBusy();
									}
								}, contents);
							}
						}, "Harap tunggu.. sedang melakukan proses upload data..");

					} else {
						MyMessageboxConfig.showFormat(
								"Mohon maaf, berkas yang Anda unggah harus berformat Excel Open XML Spreadsheet (.xlsx). Berkas yang terdeteksi: {V1}. Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut; (2) pilih menu Save As lalu simpan dengan format Excel Open XML Spreadsheet (.xlsx); (3) unggah kembali berkas hasil penyimpanan tersebut.",
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
					}
				}
			});
			button.getParent().appendChild(upload);
			upload.setVisible(button.isVisible() && edit && bolehKelolaKelompok());
			if (!obeFormatNilais.isEmpty()) {
				upload.setVisible(false);
				MyToolbarbuttonConfig uploadObe = new MyToolbarbuttonConfig(
						"Upload Data Kelompok" + Common.ukuranLabelFileUpload(), "/img/excel.png");
				uploadObe.setUpload(Common.ukuranFileUpload());
				uploadObe.addEventListener("onUpload", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						UploadEvent uploadEvent = (UploadEvent) event;
						Media media = uploadEvent.getMedia();
						if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) return;
						if (media.getName().toLowerCase().endsWith("xlsx")) {
							InputStream inputStream = media.getStreamData();
							final File file = new File(
									Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
							file.getParentFile().mkdirs();
							FileOutputStream fileOutputStream = new FileOutputStream(file);
							int c;
							while ((c = inputStream.read()) != -1) {
								fileOutputStream.write(c);
							}
							fileOutputStream.close();
							inputStream.close();
							Common.createDefaultTimer(new EventListener() {
								@Override
								public void onEvent(Event arg0) throws Exception {
									uploadDataKelompokObe(file, obeFormatNilais, new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											loadData(null);
											Clients.clearBusy();
										}
									});
								}
							}, "Harap tunggu.. sedang melakukan proses upload data OBE..");
						} else {
							MyMessageboxConfig.showFormat(
									"Mohon maaf, berkas yang Anda unggah harus berformat Excel Open XML Spreadsheet (.xlsx). Berkas yang terdeteksi: {V1}. Langkah yang dapat dilakukan: (1) buka berkas Excel tersebut; (2) pilih menu Save As lalu simpan dengan format Excel Open XML Spreadsheet (.xlsx); (3) unggah kembali berkas hasil penyimpanan tersebut.",
									"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
						}
					}
				});
				button.getParent().appendChild(uploadObe);
				uploadObe.setVisible(button.isVisible() && edit && bolehKelolaKelompok());
			}

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.getPagingChild().setMold("os");
			grid.setParent(groupbox);

			Columns columns = new Columns();

			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			// Panah detail tetap terlihat sebagai alternatif tombol "Kelola Anggota".
			// Sebelumnya 0px membuat detail anggota dan kontrol tambah anggota tidak dapat dibuka.
			column.setWidth("34px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Nama Kelompok");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Peserta");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Kuota");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth(
					tbmuser != null && (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null) ? "0%" : "24%");

			loadData(null);
		}
	}

	/**
	 * Membuka popup {@link #addWindow} untuk menambah ({@code namaTugasKelompok} baru, id
	 * {@code null}) atau mengedit ({@code namaTugasKelompok} existing) satu kelompok — ukuran
	 * popup menyesuaikan {@code Common.isMobile()}. Mendelegasikan pembangunan form ke
	 * {@link #init}.
	 *
	 * @param event            event pemicu (tidak dipakai isinya, hanya untuk signature listener)
	 * @param namaTugasKelompok kelompok yang diedit, atau instance baru kosong untuk tambah
	 */
	public void onAdd(Event event, NamaTugasKelompok namaTugasKelompok) throws Exception {
		addWindow = new MyWindow();
		if (Common.isMobile()) {
			addWindow.setHeight("70%");
			addWindow.setWidth("100%");
		} else {
			addWindow.setHeight("270px");
			addWindow.setWidth("850px");
		}
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		init(namaTugasKelompok);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Sisi impor format Excel STANDAR (non-OBE): membaca baris demi baris (kolom 0=nama kelompok
	 * dengan prefix id opsional "{@code <id>-}" dari template download — dibuang bila prefix murni
	 * angka, TANPA merusak nama kelompok yang memang mengandung tanda minus; kolom 1=mahasiswa
	 * via NIM; kolom 2=nilai; kolom 3=keterangan) di {@link Thread} terpisah dengan progres
	 * ditampilkan lewat {@link Timer} polling. Untuk tiap baris valid: {@link NamaTugasKelompok}
	 * dicari berdasar nama+{@code tugasKelompok} ini, DIBUAT bila belum ada (transaksi tersendiri);
	 * lalu {@link NamaTugasKelompokPunyaMahasiswa} untuk pasangan kelompok+mahasiswa dicari/dibuat
	 * dan nilainya diupdate (transaksi tersendiri per baris — bukan satu transaksi besar, agar
	 * kegagalan satu baris tidak membatalkan baris lain).
	 * <p>
	 * Baris tanpa mahasiswa valid (NIM tak ditemukan) atau nama kelompok kosong DILEWATI (dicatat
	 * ke {@code catatanGagal}, dibatasi 1500 karakter agar pesan ringkasan tidak membengkak) —
	 * tidak menghentikan proses baris lain. Ringkasan akhir (jumlah tersimpan/dilewati + rincian
	 * kegagalan) ditampilkan lewat messagebox setelah proses selesai, lalu {@code eventListener}
	 * dipanggil sebagai callback (mis. me-refresh grid).
	 *
	 * @param file          file .xlsx yang sudah diupload &amp; disimpan sementara di server
	 * @param eventListener dipanggil setelah messagebox ringkasan ditutup pengguna
	 * @param contents      TIDAK dipakai isinya di method ini (parameter dipertahankan untuk kompatibilitas signature/pemanggil)
	 */
	public void uploadDataNamaTugasKelompokPunyaMahasiswa(final File file, final EventListener eventListener,
			final String[] contents) throws Exception {

		final Label peringatan = new Label("");

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("loading file " + file.getAbsolutePath());
					MyMessageboxConfig.showFormatCb(
							"Proses unggah data kelompok telah berhasil dilaksanakan.{V1}",
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener,
							(peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()));
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					int tersimpan = 0;
					int dilewati = 0;
					StringBuilder catatanGagal = new StringBuilder();
					for (int i = 1; i < rowCount; i++) {
						try {

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 1, i,
									Mahasiswa.class);
							String namaSel = Common.getSheetContentAsString(sheet, 0, i);
							// Buang prefix "<id>-" (format download) TANPA merusak nama kelompok yang memang ber-"-".
							String nama = namaSel;
							if (namaSel != null) {
								int posDash = namaSel.indexOf('-');
								if (posDash > 0) {
									String pre = namaSel.substring(0, posDash).trim();
									boolean semuaAngka = pre.length() > 0;
									for (int k = 0; k < pre.length(); k++) {
										if (!Character.isDigit(pre.charAt(k))) {
											semuaAngka = false;
											break;
										}
									}
									if (semuaAngka) {
										nama = namaSel.substring(posDash + 1).trim();
									}
								}
							}
							if (mahasiswa == null) {
								dilewati++;
								if (catatanGagal.length() < 1500) catatanGagal.append("\n- Baris ").append(i + 1).append(": mahasiswa (NIM kolom ke-2) tidak ditemukan.");
								continue;
							}
							if (nama == null || nama.trim().isEmpty()) {
								dilewati++;
								if (catatanGagal.length() < 1500) catatanGagal.append("\n- Baris ").append(i + 1).append(": nama kelompok (kolom ke-1) kosong.");
								continue;
							}
							if (mahasiswa != null && nama != null && !nama.trim().isEmpty()) {

								NamaTugasKelompok namaTugasKelompok = (NamaTugasKelompok) Common
										.getSheetContentAsObject(sheet, 0, i, NamaTugasKelompok.class,
												Restrictions.eq("tugasKelompok", tugasKelompok));

								if (namaTugasKelompok == null) {
									namaTugasKelompok = new NamaTugasKelompok();
									namaTugasKelompok.setNama(nama);
									namaTugasKelompok.setKeterangan(nama);
									namaTugasKelompok.setTugasKelompok(tugasKelompok);
									session.getTransaction().begin();
									session.save(namaTugasKelompok);
									session.getTransaction().commit();
								}

								NamaTugasKelompokPunyaMahasiswa namaTugasKelompokPunyaMahasiswa = (NamaTugasKelompokPunyaMahasiswa) session
										.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
										.add(Restrictions.eq("namaTugasKelompok", namaTugasKelompok))
										.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

								if (namaTugasKelompokPunyaMahasiswa == null) {
									namaTugasKelompokPunyaMahasiswa = new NamaTugasKelompokPunyaMahasiswa();
								}

								namaTugasKelompokPunyaMahasiswa.setNamaTugasKelompok(namaTugasKelompok);
								namaTugasKelompokPunyaMahasiswa.setMahasiswa(mahasiswa);
								namaTugasKelompokPunyaMahasiswa.setNilai(Common.getSheetContentAsDouble(sheet, 2, i));
								namaTugasKelompokPunyaMahasiswa
										.setKeterangan(Common.getSheetContentAsString(sheet, 3, i));

								session.getTransaction().begin();
								session.saveOrUpdate(namaTugasKelompokPunyaMahasiswa);
								session.getTransaction().commit();
								tersimpan++;

								label.setValue("Upload data \"" + namaTugasKelompokPunyaMahasiswa.getMahasiswa() + " - "
										+ namaTugasKelompokPunyaMahasiswa.getNamaTugasKelompok() + "\" ("
										+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							}
						} catch (Exception e) {
							dilewati++;
							if (catatanGagal.length() < 1500) catatanGagal.append("\n- Baris ").append(i + 1).append(": ").append(e.getMessage());
							Common.tampilErrorJikaAdmin(e);
						}

					}
					final String ringkasanUpload = tersimpan + " baris nilai berhasil disimpan"
						+ (dilewati > 0 ? ", " + dilewati + " baris dilewati." + catatanGagal.toString() : ".");
					peringatan.setValue(ringkasanUpload);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/NamaTugasKelompokHelper.java:758");
				} finally {
					// Sesi thread-native (currentNativeSession) WAJIB ditutup di blok finally agar
					// tidak bocor walau proses upload gagal di tengah jalan.
					HibernateUtil.closeSession();
				}

				label.setValue("");
			}
		}).start();
	}

	/**
	 * Membangun form popup tambah/edit satu kelompok (satu field: Nama Kelompok) ke dalam
	 * {@link #addWindow}, dengan tombol Batal (tutup tanpa simpan) dan Simpan (memanggil
	 * {@link #onSave}, lalu {@link #loadData} dan menutup popup bila berhasil).
	 *
	 * @param namaTugasKelompok kelompok yang diedit (nilainya di-set ke field {@link #namaTugasKelompok}), nama awal mengisi {@link #nama}
	 */
	private void init(NamaTugasKelompok namaTugasKelompok) {
		this.namaTugasKelompok = namaTugasKelompok;

		addWindow.setTitle("Tugas Kelompok");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok"));
		row.appendChild(nama = new Textbox(namaTugasKelompok.getNama()));
		nama.setWidth("90%");
		nama.setRows(4);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					loadData(null);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * Validasi (nama kelompok wajib diisi) dan simpan {@link #namaTugasKelompok} lewat
	 * {@link NamaTugasKelompokDao} — bila id sudah ada, entity di-{@code load} ulang dari DAO
	 * lebih dulu (memastikan versi terbaru) sebelum field-nya ditimpa dan di-{@code update};
	 * bila belum punya id, dibuat baru via {@code save}.
	 *
	 * @param event tidak dipakai isinya, hanya untuk signature listener
	 * @return {@code true} bila berhasil disimpan (popup boleh ditutup); {@code false} bila validasi nama gagal (popup tetap terbuka)
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
			"Mohon maaf, nama kelompok wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan nama kelompok pada kolom yang tersedia; (2) kemudian tekan tombol Simpan kembali.",
			"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		NamaTugasKelompokDao namaTugasKelompokDao = DaoFactory.getInstance().getNamaTugasKelompokDao();
		if (namaTugasKelompok.getId() != null) {
			namaTugasKelompok = namaTugasKelompokDao.load(namaTugasKelompok.getId());

		}

		namaTugasKelompok.setNama(nama.getValue());
		namaTugasKelompok.setTugasKelompok(tugasKelompok);

		if (namaTugasKelompok.getId() != null) {
			namaTugasKelompokDao.update(namaTugasKelompok);
		} else {
			namaTugasKelompokDao.save(namaTugasKelompok);
		}

		return true;
	}

	/**
	 * Menentukan apakah {@link #tugasKelompok} ini memakai mode penilaian OBE (Outcome-Based
	 * Education), dan bila ya, daftar {@link FormatNilai} (komponen capaian pembelajaran) yang
	 * relevan. Syarat OBE: {@code tugasKelompok.getPerkuliahan().getKurikulum().apakahObe(...)}
	 * true UNTUK tahun ajaran+ganjil/genap perkuliahan ini, DAN
	 * {@code tugasKelompok.getFormatNilais()} terisi dan BUKAN literal {@code Tugas.JSON} (penanda
	 * format lama non-OBE). Bila OBE, hasil disaring dari {@code Common.getFormatNilais(...)}
	 * hanya yang id-nya termuat dalam JSON {@code formatNilais} (kunci = id FormatNilai) DAN
	 * punya {@code getStatusPertemuan() != null}. Exception apa pun (mis. JSON tidak valid)
	 * ditangkap dan menghasilkan list kosong (dianggap non-OBE, fallback aman ke format standar).
	 *
	 * @return daftar {@link FormatNilai} berlaku bila mode OBE aktif; list kosong bila non-OBE atau {@link #tugasKelompok} belum diset
	 */
	private List<FormatNilai> ambilObeFormatNilais() {
		List<FormatNilai> result = new ArrayList<FormatNilai>();
		try {
			if (tugasKelompok == null) return result;
			Perkuliahan perkuliahan = tugasKelompok.getPerkuliahan();
			if (perkuliahan == null || perkuliahan.getKurikulum() == null
					|| !perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(),
							perkuliahan.getGanjilGenap())
					|| tugasKelompok.getFormatNilais() == null
					|| tugasKelompok.getFormatNilais().equalsIgnoreCase(Tugas.JSON)) {
				return result;
			}
			JSONObject jsonFn = new JSONObject(tugasKelompok.getFormatNilais().replace('\0', ' '));
			for (FormatNilai fn : Common.getFormatNilais(HibernateUtil.currentSession(), perkuliahan)) {
				if (fn.getStatusPertemuan() != null && !jsonFn.isNull(fn.getId().toString())) {
					result.add(fn);
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return result;
	}

	/**
	 * Sisi ekspor mode OBE: menulis satu file Excel berisi seluruh anggota
	 * {@link NamaTugasKelompokPunyaMahasiswa} tugas ini (kolom NAMATUGASKELOMPOK/NIM/NAMA/
	 * KETERANGAN + satu kolom PER {@link FormatNilai} di {@code obeFormatNilais}). Nilai tiap
	 * komponen dibaca dari JSON {@code tugasKelompok.getKeteranganNilai()} dengan kunci
	 * {@code "<idMahasiswa>_mhs_nilai_<idFormatNilai>"} (default {@code 0.0} bila belum ada) —
	 * BUKAN dari kolom {@code nilai} standar {@link NamaTugasKelompokPunyaMahasiswa}, sesuai
	 * penyimpanan khusus mode OBE yang dijelaskan di Javadoc kelas. File langsung diunduh
	 * ({@link Filedownload#save}) ke browser setelah ditulis ke direktori temp server.
	 *
	 * @param obeFormatNilais daftar komponen nilai OBE yang jadi kolom (dari {@link #ambilObeFormatNilais()})
	 */
	@SuppressWarnings("unchecked")
	private void downloadDataKelompokObe(final List<FormatNilai> obeFormatNilais) {
		try {
			Session session = HibernateUtil.currentSession();
			List<NamaTugasKelompokPunyaMahasiswa> anggotaList = session
					.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
					.createAlias("namaTugasKelompok", "ntk")
					.add(Restrictions.eq("ntk.tugasKelompok", tugasKelompok))
					.addOrder(Order.asc("ntk.id")).addOrder(Order.asc("id")).list();

			String keterNilai = tugasKelompok.getKeteranganNilai();
			JSONObject json = new JSONObject(
					keterNilai == null || keterNilai.trim().isEmpty() ? "{}" : keterNilai.replace('\0', ' '));

			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("Nilai OBE");

			XSSFRow headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("NAMATUGASKELOMPOK");
			headerRow.createCell(1).setCellValue("MAHASISWA.NIM");
			headerRow.createCell(2).setCellValue("MAHASISWA.NAMA");
			headerRow.createCell(3).setCellValue("KETERANGAN");
			for (int i = 0; i < obeFormatNilais.size(); i++) {
				headerRow.createCell(4 + i).setCellValue(obeFormatNilais.get(i).getNama());
			}

			int rowNum = 1;
			for (NamaTugasKelompokPunyaMahasiswa anggota : anggotaList) {
				Mahasiswa mhs = anggota.getMahasiswa();
				if (mhs == null) continue;
				XSSFRow row = sheet.createRow(rowNum++);
				String namaKelompok = anggota.getNamaTugasKelompok() == null ? ""
						: (anggota.getNamaTugasKelompok().getNama() == null ? ""
								: anggota.getNamaTugasKelompok().getNama());
				row.createCell(0).setCellValue(namaKelompok);
				row.createCell(1).setCellValue(mhs.getNim() == null ? "" : mhs.getNim());
				row.createCell(2).setCellValue(mhs.getNama() == null ? "" : mhs.getNama());
				row.createCell(3).setCellValue(anggota.getKeterangan() == null ? "" : anggota.getKeterangan());
				for (int i = 0; i < obeFormatNilais.size(); i++) {
					FormatNilai fn = obeFormatNilais.get(i);
					String key = mhs.getId() + "_mhs_nilai_" + fn.getId();
					row.createCell(4 + i).setCellValue(json.isNull(key) ? 0.0 : json.optDouble(key, 0.0));
				}
			}

			String namaFile = "nilai_kelompok_obe_" + System.currentTimeMillis() + ".xlsx";
			File tempFile = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + namaFile));
			tempFile.getParentFile().mkdirs();
			FileOutputStream fos = new FileOutputStream(tempFile);
			workbook.write(fos);
			fos.close();
			Filedownload.save(tempFile, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		} catch (Exception e) {
			PesanFormalHelper.tampilkanGagalException("mengunduh data kelompok OBE", e,
					new String[] { "Muat ulang halaman lalu coba kembali." });
		}
	}

	/**
	 * Sisi impor mode OBE, padanan {@link #uploadDataNamaTugasKelompokPunyaMahasiswa} untuk format
	 * khusus OBE. Kolom nilai OBE dicari FLEKSIBEL: default mengikuti urutan
	 * {@code obeFormatNilais} mulai kolom 4, tapi bila header baris pertama ditemukan cocok NAMA
	 * (case-insensitive) dengan salah satu {@link FormatNilai}, indeks kolom sebenarnya dipakai —
	 * toleran terhadap kolom yang diurutkan ulang manual oleh pengguna di Excel.
	 * <p>
	 * <b>Perbaikan yang didokumentasikan eksplisit di kode:</b> importer OBE versi sebelumnya
	 * HANYA menulis nilai ke JSON {@code keteranganNilai} tanpa pernah membuat baris
	 * {@link NamaTugasKelompokPunyaMahasiswa}, sehingga jumlah peserta kelompok tetap tampil 0
	 * meski upload "berhasil". Kini kolom 0 (nama kelompok, lewat {@link #normalisasiNamaKelompok})
	 * JUGA disinkronkan seperti importer standar: kelompok dicari by nama, anggota
	 * dicari/dibuat/DIPINDAHKAN ke kelompok baru bila mahasiswa sebelumnya ada di kelompok lain
	 * pada tugas yang sama (mencegah anggota ganda). Baris tanpa mahasiswa valid atau kelompok
	 * tak ditemukan dilewati (dicatat, dibatasi 1500 karakter). Nilai JSON per komponen diakumulasi
	 * di memori lalu ditulis SEKALI ke {@code tugasKelompok.getKeteranganNilai()} di akhir (satu
	 * transaksi terpisah dari transaksi per-baris anggota) agar tidak berulang kali menimpa kolom
	 * besar itu per baris.
	 *
	 * @param file             file .xlsx yang sudah diupload &amp; disimpan sementara di server
	 * @param obeFormatNilais  daftar komponen nilai OBE yang kolomnya dibaca (dari {@link #ambilObeFormatNilais()})
	 * @param eventListener    dipanggil setelah messagebox ringkasan ditutup pengguna
	 */
	private void uploadDataKelompokObe(final File file, final List<FormatNilai> obeFormatNilais,
			final EventListener eventListener) throws Exception {
		final Label peringatan = new Label("");
		final Label label = new Label("Proses upload nilai OBE ...");
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					MyMessageboxConfig.showFormatCb(
							"Proses unggah data kelompok OBE telah selesai.{V1}",
							"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, eventListener,
							(peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()));
					Clients.clearBusy();
					timer.detach();
				}
			}
		});
		timer.start();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);
					XSSFRow headerRow = sheet.getRow(0);

					int[] fnCols = new int[obeFormatNilais.size()];
					for (int i = 0; i < obeFormatNilais.size(); i++) {
						fnCols[i] = 4 + i;
						if (headerRow != null) {
							String fnNama = obeFormatNilais.get(i).getNama();
							for (int c = 4; c < headerRow.getLastCellNum(); c++) {
								XSSFCell hCell = headerRow.getCell(c);
								if (hCell != null && fnNama != null
										&& fnNama.equalsIgnoreCase(hCell.getStringCellValue())) {
									fnCols[i] = c;
									break;
								}
							}
						}
					}

					Session session = HibernateUtil.currentNativeSession();
					int rowCount = sheet.getLastRowNum() + 1;
					int tersimpan = 0;
					int dilewati = 0;
					StringBuilder catatanGagal = new StringBuilder();

					TugasKelompok tk = (TugasKelompok) session.get(TugasKelompok.class, tugasKelompok.getId());
					String keterNilai = tk.getKeteranganNilai();
					JSONObject json = new JSONObject(
							keterNilai == null || keterNilai.trim().isEmpty() ? "{}" : keterNilai.replace('\0', ' '));

					for (int i = 1; i < rowCount; i++) {
						try {
							Mahasiswa mahasiswaRow = (Mahasiswa) Common.getSheetContentAsObject(sheet, 1, i,
									Mahasiswa.class);
							if (mahasiswaRow == null) {
								dilewati++;
								if (catatanGagal.length() < 1500) {
									catatanGagal.append("\n- Baris ").append(i + 1)
											.append(": mahasiswa (NIM kolom ke-2) tidak ditemukan.");
								}
								continue;
							}

							// Import OBE sebelumnya hanya menulis nilai JSON dan tidak pernah membuat
							// NamaTugasKelompokPunyaMahasiswa. Akibatnya upload tampak selesai, tetapi
							// jumlah peserta tetap 0. Sinkronkan relasi kelompok dari kolom 1 seperti
							// importer non-OBE; jika mahasiswa sudah berada di kelompok lain pada tugas
							// yang sama, pindahkan relasinya agar tidak tercipta anggota ganda.
							String namaKelompok = normalisasiNamaKelompok(
									Common.getSheetContentAsString(sheet, 0, i));
							if (namaKelompok != null && !namaKelompok.isEmpty()) {
								NamaTugasKelompok kelompok = (NamaTugasKelompok) session
										.createCriteria(NamaTugasKelompok.class)
										.add(Restrictions.eq("tugasKelompok", tk))
										.add(Restrictions.ilike("nama", namaKelompok)).setMaxResults(1).uniqueResult();
								if (kelompok == null) {
									dilewati++;
									if (catatanGagal.length() < 1500) {
										catatanGagal.append("\n- Baris ").append(i + 1).append(": kelompok '")
												.append(namaKelompok).append("' tidak ditemukan.");
									}
									continue;
								}

								NamaTugasKelompokPunyaMahasiswa anggota = (NamaTugasKelompokPunyaMahasiswa) session
										.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
										.createAlias("namaTugasKelompok", "ntk")
										.add(Restrictions.eq("ntk.tugasKelompok", tk))
										.add(Restrictions.eq("mahasiswa", mahasiswaRow)).setMaxResults(1)
										.uniqueResult();
								if (anggota == null) {
									anggota = new NamaTugasKelompokPunyaMahasiswa();
									anggota.setMahasiswa(mahasiswaRow);
								}
								anggota.setNamaTugasKelompok(kelompok);
								anggota.setKeterangan(Common.getSheetContentAsString(sheet, 3, i));
								session.getTransaction().begin();
								session.saveOrUpdate(anggota);
								session.getTransaction().commit();
							}

							String keyBase = mahasiswaRow.getId() + "_mhs_nilai_";
							for (int f = 0; f < obeFormatNilais.size(); f++) {
								Double val = Common.getSheetContentAsDouble(sheet, fnCols[f], i);
								json.put(keyBase + obeFormatNilais.get(f).getId(),
										val == null ? 0.0 : val.doubleValue());
							}
							tersimpan++;
							label.setValue("Upload nilai OBE " + mahasiswaRow.getNama() + " ("
									+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
						} catch (Exception e) {
							try {
								if (session.getTransaction().isActive()) session.getTransaction().rollback();
							} catch (Exception rollbackError) {
								ais.common.ErrorAuditUtil.record(rollbackError,
										"auto-audit NamaTugasKelompokHelper.uploadDataKelompokObe.rollback");
							}
							dilewati++;
							if (catatanGagal.length() < 1500) {
								catatanGagal.append("\n- Baris ").append(i + 1).append(": ")
										.append(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
							}
						}
					}

					session.getTransaction().begin();
					tk.setKeteranganNilai(json.toString());
					session.update(tk);
					session.getTransaction().commit();

					final String ringkasan = tersimpan + " baris nilai OBE berhasil diperbarui"
							+ (dilewati > 0 ? ", " + dilewati + " baris dilewati." + catatanGagal.toString() : ".");
					peringatan.setValue(ringkasan);
				} catch (Exception e1) {
					e1.printStackTrace();
					ais.common.ErrorAuditUtil.record(e1,
							"auto-audit NamaTugasKelompokHelper.uploadDataKelompokObe");
				} finally {
					HibernateUtil.closeSession();
				}
				label.setValue("");
			}
		}).start();
	}

	/** Menghapus prefix ID hasil download (mis. "123-Kelompok 1") tanpa merusak nama ber-tanda minus. */
	private String normalisasiNamaKelompok(String nilaiCell) {
		if (nilaiCell == null) {
			return "";
		}
		String hasil = nilaiCell.trim();
		int posDash = hasil.indexOf('-');
		if (posDash > 0) {
			String prefix = hasil.substring(0, posDash).trim();
			boolean angka = !prefix.isEmpty();
			for (int i = 0; i < prefix.length(); i++) {
				if (!Character.isDigit(prefix.charAt(i))) {
					angka = false;
					break;
				}
			}
			if (angka) {
				hasil = hasil.substring(posDash + 1).trim();
			}
		}
		return hasil;
	}

}
