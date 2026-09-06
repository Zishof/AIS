package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.BorderStyle;
import org.zkoss.poi.ss.usermodel.Cell;
import org.zkoss.poi.ss.usermodel.CellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.model.impl.BookHelper;
import org.zkoss.zss.ui.Rect;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.BankSoalAction;
import ais.action.master.helper.generic.AmbilDataBankSoalBanyak;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.GeneralValueObject;
import ais.database.model.PenjelasanBankSoal;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper composer ZK yang menampilkan dan mengelola daftar {@link BankSoal} (soal ujian) yang
 * tergabung dalam satu {@link PenjelasanBankSoal} (kelompok/grup soal, mis. per bab atau per topik).
 * Menyediakan tampilan soal-jawaban langsung di grid (lewat {@link DetailUjianHelper}), pengambilan
 * soal dari bank soal lain, pembuatan soal baru, ekspor/impor massal ke/dari berkas Excel (.xlsx),
 * dan penghapusan seluruh soal dalam grup.
 *
 * <p>
 * Ekspor ({@link #doDownload}) menghasilkan spreadsheet dengan satu baris per {@link BankSoal}: teks
 * soal, jawaban benar (huruf), skor benar/salah/default, isi tiap opsi jawaban (kolom JWB_A..JWB_J),
 * penjelasan, flag tampil-penjelasan-saat-ujian, dan jenis soal. Impor ({@link #doUpload}) membaca
 * struktur yang sama untuk membuat/memperbarui {@link BankSoal} beserta {@link BankSoalDetail}
 * per opsi jawaban (dicocokkan lewat id numerik pada spreadsheet atau, bila tidak ada, lewat
 * kecocokan persis teks soal), dan menyimpulkan {@code jenisPilihanGanda} (kombinasi/benar-salah/
 * pilihan ganda biasa) dari jumlah opsi yang ditandai benar. Kolom "Soal", "Pilihan Ganda", "Upload",
 * "Hapus" dan pengambilan-soal-massal hanya tersedia untuk user non-mahasiswa.
 * </p>
 *
 * <p>
 * <b>Hubungan dengan {@link BankSoal} dan {@link BankSoalDetail}.</b> Satu {@link BankSoal}
 * memiliki sekumpulan {@link BankSoalDetail} sebagai opsi jawabannya; kepemilikan itu disimpan
 * pada kolom {@code bankSoal} milik detail. Kelas ini menyunting keduanya secara massal lewat
 * jalur impor, sehingga keterikatan opsi ke soal pemiliknya perlu diperhatikan &mdash; ini
 * keluarga masalah yang sama dengan validasi "opsi jawaban harus milik soal yang sedang
 * dijawab" pada jalur ujian formal. Pencarian opsi pada kedua {@link #doUpload} SUDAH dibatasi
 * dengan benar oleh {@code Restrictions.eq("bankSoal", newBankSoal)}, jadi tidak ada opsi milik
 * soal lain yang tersentuh. Yang perlu dicatat justru sisa-sisa opsi lama pada soal yang SAMA:
 * </p>
 * <ul>
 * <li>Impor tidak pernah MENGHAPUS opsi. Bila sebuah soal semula punya opsi A&ndash;E lalu
 * diimpor ulang dengan hanya A&ndash;C terisi, baris D dan E tetap ada beserta nilai
 * {@code betul} lamanya dan tetap menjadi opsi jawaban soal tersebut.</li>
 * <li>Pada cabang non-pilihan-ganda, opsi yang diperbarui dicari hanya dengan
 * {@code eq("bankSoal", ...)} tanpa penyaring {@code huruf}, sehingga yang tertimpa adalah
 * SATU baris sembarang. Soal pilihan ganda yang diimpor ulang sebagai esai akan menyisakan
 * opsi-opsi lamanya.</li>
 * <li>Penyimpulan {@code jenisPilihanGanda} memakai hitungan {@code count(betul = true)} atas
 * SELURUH opsi soal tersebut, jadi sisa opsi lama ikut terhitung dan dapat menghasilkan
 * klasifikasi {@link BankSoal#COMBINATION_CHOICE} yang tidak diinginkan.</li>
 * </ul>
 *
 * <p>
 * <b>Dua pasang overload dengan cakupan berbeda.</b> {@link #doDownload(Criteria)} dan
 * {@link #doUpload(Media, DataLoader)} bekerja LINTAS grup (tidak dibatasi
 * {@link PenjelasanBankSoal} mana pun), sedangkan
 * {@link #doDownload(PenjelasanBankSoal, Criteria)} dan
 * {@link #doUpload(Media, PenjelasanBankSoal, DataLoader)} dibatasi satu grup. Hanya pasangan
 * yang dibatasi grup itulah yang dipakai kelas ini (lewat {@link #initSpreadsheet()} dan
 * {@link #uploadSoal}); pasangan lintas-grup bersifat API publik yang saat dokumentasi ini
 * ditulis TIDAK dipanggil dari mana pun di basis kode. Perbedaan itu penting karena varian
 * lintas-grup mencocokkan soal berdasarkan teks soal yang sama persis di SELURUH tabel
 * {@link BankSoal}, tanpa memeriksa satuan kerja/fakultas/jurusan/dosen pemiliknya, dan
 * memindahkan soal ke grup yang namanya dibaca dari sel berkas Excel &mdash; sehingga
 * menghidupkannya kembali perlu disertai penjagaan kepemilikan lebih dulu.
 * </p>
 *
 * <p>
 * <b>Soal hasil impor tidak mewarisi konteks grup.</b> Berbeda dari tombol "Soal Baru" yang
 * menyalin {@code fakultas}, {@code jurusan}, {@code dosen}, {@code guru}, dan
 * {@code satuanKerja} dari {@link PenjelasanBankSoal}, kedua {@link #doUpload} hanya mengisi
 * {@code penjelasanBankSoal} pada {@link BankSoal} baru dan membiarkan kelima kolom itu
 * {@code null}.
 * </p>
 *
 * <p>
 * <b>Penjagaan akses.</b> Seluruh penjagaan di kelas ini bersifat tampilan
 * ({@code setVisible} berdasarkan peran {@link Tbmuser}); keempat method statis
 * {@link #doDownload}/{@link #doUpload} tidak memeriksa peran maupun kepemilikan data sama
 * sekali. Penyaringan berbasis satuan kerja dilakukan di layar induk
 * {@code PenjelasanBankSoalAction.initCriteria(boolean)}, yang memperlakukan grup dengan
 * {@code satuanKerja} bernilai {@code null} sebagai terlihat oleh semua pengguna &mdash;
 * relevan mengingat soal hasil impor memang tidak mendapat satuan kerja.
 * </p>
 */
public class DetailGrupSoalHelper implements DataLoader {

	/**
	 * Grid daftar soal (satu baris per {@link BankSoal}). Dibuat di
	 * {@link #display(PenjelasanBankSoal, Component)} sebagai {@link MyGrid} dengan mold
	 * {@code paging} berukuran 5000 dan tinggi minimum 1400px, lalu diisi ulang oleh
	 * {@link #loadData(Object)} dengan {@link DetailUjianRenderer}. Grid hanya memiliki SATU kolom
	 * berlabel kosong &mdash; seluruh tampilan soal beserta opsi jawabannya dirender ke dalam satu
	 * sel oleh {@link DetailUjianHelper#tampilSoalDanJawaban}.
	 */
	private Grid grid;

	/**
	 * Kotak kata kunci pada toolbar. Menekan Enter (event {@code onOK}) atau tombol kaca pembesar
	 * memicu {@link #loadData(Object)}.
	 *
	 * <p><b>Perlu diketahui:</b> {@link #loadData(Object)} tidak pernah membaca nilai field ini,
	 * sehingga kata kunci yang diketik tidak memengaruhi hasil &mdash; menekan tombol cari hanya
	 * memuat ulang daftar yang sama. Kotak ini terpasang di UI tetapi belum tersambung ke kriteria
	 * pencarian mana pun.</p>
	 */
	private Textbox cari;

	/**
	 * Kontrol paging eksternal, diinisialisasi lewat {@code Common.initPaging1} sehingga memakai
	 * ukuran halaman {@code Common.ROWS_COUNT_ON_PAGE_1}.
	 *
	 * <p><b>Perlu diketahui:</b> {@link #loadData(Object)} mengisi {@code setTotalSize(...)} dengan
	 * jumlah baris pada HALAMAN yang baru saja diambil, bukan jumlah seluruh soal dalam grup.
	 * Karena nilai itu tidak pernah melebihi ukuran satu halaman, syarat
	 * {@code setVisible(size > ROWS_COUNT_ON_PAGE_1)} tidak pernah terpenuhi dan kontrol paging
	 * tetap tersembunyi &mdash; soal di luar halaman pertama tidak dapat dijangkau lewat UI ini.</p>
	 */
	private Paging paging;
	/**
	 * Pengguna yang sedang login. Diisi di konstruktor dan diisi ULANG di awal
	 * {@link #display(PenjelasanBankSoal, Component)} agar tetap segar bila instance helper dipakai
	 * kembali. Menjadi dasar penjagaan tampilan: tombol "Ambil Soal", "Soal Baru", "Download",
	 * "Upload" dan "Hapus" hanya ditampilkan bila {@code tbmuser.getMahasiswa() == null}, sedangkan
	 * mode sunting pada {@link DetailUjianRenderer} memakai syarat yang lebih ketat (bukan
	 * mahasiswa, bukan siswa, dan bukan calon mahasiswa).
	 *
	 * <p>Dibaca tanpa penjagaan {@code null}: bila tidak ada pengguna login,
	 * {@link #display(PenjelasanBankSoal, Component)} akan melempar
	 * {@link NullPointerException} pada pemeriksaan {@code tbmuser.getMahasiswa()} yang pertama.</p>
	 */
	private Tbmuser tbmuser;
	/**
	 * Sisa dari rancangan penghitungan hasil pencarian. Tidak pernah dibaca maupun ditulis di
	 * kelas ini, dan tidak ada subkelas yang memakainya &mdash; nilainya tetap 0 sepanjang umur
	 * instance.
	 */
	protected int countHasil = 0;
	/**
	 * Grup soal (mis. per bab atau per topik) yang isinya sedang dikelola. Ditetapkan sekali di
	 * {@link #display(PenjelasanBankSoal, Component)} dan menjadi penyaring wajib pada
	 * {@link #loadData(Object)}, {@link #initSpreadsheet()}, {@link #uploadSoal}, tombol "Hapus",
	 * serta menjadi sumber nilai bawaan (jenis koreksi, fakultas, jurusan, dosen, guru, satuan
	 * kerja) bagi soal yang dibuat lewat tombol "Soal Baru".
	 */
	private PenjelasanBankSoal penjelasanBankSoal;

	/**
	 * Membaca pengguna yang sedang login ke {@link #tbmuser}. Seluruh state lain
	 * ({@link #grid}, {@link #paging}, {@link #cari}, {@link #penjelasanBankSoal}) baru terisi saat
	 * {@link #display(PenjelasanBankSoal, Component)} dipanggil, sehingga instance yang belum
	 * di-{@code display} tidak siap dipakai sebagai {@link DataLoader}.
	 */
	public DetailGrupSoalHelper() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Membangun UI daftar soal untuk {@code penjelasanBankSoal}: toolbar (ambil soal dari bank lain
	 * lewat {@link AmbilDataBankSoalBanyak}, buat soal baru lewat
	 * {@link BankSoalAction#onAddExternal}, download/upload Excel, hapus semua, refresh, cari) dan
	 * grid berpaging. Lalu memuat datanya.
	 *
	 * @param penjelasanBankSoal grup soal yang isinya ditampilkan/dikelola
	 * @param detail             komponen induk ZK tempat UI dibangun
	 */
	public void display(final PenjelasanBankSoal penjelasanBankSoal, Component detail) {
		this.penjelasanBankSoal = penjelasanBankSoal;
		tbmuser = Common.getCurrentUser();

		final Groupbox groupbox = new Groupbox();
		groupbox.setParent(detail);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Soal", "/img/new.gif");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Long> bankSoals = HibernateUtil.currentSession().createCriteria(BankSoal.class)
						.setProjection(Projections.property("id"))
						.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal)).list();

				AmbilDataBankSoalBanyak window = new AmbilDataBankSoalBanyak(bankSoals,
						penjelasanBankSoal.getJenisKoreksi(), null, null);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("95%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<BankSoal> bankSoals = (List<BankSoal>) arg0.getData();
						if (bankSoals != null) {

							Session session = HibernateUtil.currentSession();

							for (BankSoal bankSoal : bankSoals) {
								bankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
								Common.refreshUpdate(session, penjelasanBankSoal);
							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(true);
								}
							});

						}
					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Soal Baru", "/img/svg/addthis.svg");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				BankSoal bankSoal = new BankSoal();
				bankSoal.setJenisKoreksi(penjelasanBankSoal.getJenisKoreksi());
				bankSoal.setFakultas(penjelasanBankSoal.getFakultas());
				bankSoal.setJurusan(penjelasanBankSoal.getJurusan());
				bankSoal.setDosen(penjelasanBankSoal.getDosen());
				bankSoal.setGuru(penjelasanBankSoal.getGuru());
				bankSoal.setSatuanKerja(penjelasanBankSoal.getSatuanKerja());
				bankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
				bankSoal.setJenisKoreksi(penjelasanBankSoal.getJenisKoreksi());

				BankSoalAction.onAddExternal(event, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						BankSoal bankSoal = (BankSoal) arg0.getData();
						Session session = HibernateUtil.currentSession();

						bankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
						Common.refreshUpdate(session, bankSoal);
						session.flush();

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(true);
							}
						});

					}
				}, bankSoal, penjelasanBankSoal.getJenisKoreksi());
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Download", "/img/excel.png");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(), "/img/excel.png");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				uploadSoal(media, penjelasanBankSoal);
			}
		});

		button.setParent(toolbar);

		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setVisible(tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();
										List<BankSoal> bankSoals = session.createCriteria(BankSoal.class)
												.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal)).list();
										for (BankSoal bankSoal : bankSoals) {
											session.delete(bankSoal);
										}

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(true);
											}
										});

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

		button = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);

			}
		});

		button.setParent(toolbar);

		cari = new Textbox();
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		toolbar.appendChild(cari);
		button = new MyToolbarbuttonConfig("", "/img/search.png");
		button.setAttribute("janganDisabled", true);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		button.setParent(toolbar);

		paging = new Paging();
		Common.initPaging1(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);

			}
		});
		paging.setParent(groupbox);

		grid = new MyGrid();

		grid.setSclass("fgrid");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(5000);
		grid.setStyle("min-height:1400px");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);

		loadData(null);

	}

	/** Perender baris grid yang mendelegasikan tampilan soal+jawaban ke {@link DetailUjianHelper#tampilSoalDanJawaban}, dengan mode edit aktif untuk user non-mahasiswa/siswa/calon mahasiswa. */
	public class DetailUjianRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Callback yang dioper ke {@link DetailUjianHelper#tampilSoalDanJawaban} dan dipanggil
		 * setelah pengguna mengubah soal/jawaban dari dalam sel, untuk memuat ulang seluruh grid
		 * lewat {@link DetailGrupSoalHelper#loadData(Object)}. Satu instance dipakai bersama untuk
		 * SEMUA baris karena tidak menyimpan state per-baris.
		 */
		private EventListener ubahEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		};

		@Override
		/**
		 * Merender satu baris soal dengan mendelegasikan seluruh tampilan ke
		 * {@link DetailUjianHelper#tampilSoalDanJawaban}, termasuk teks soal, seluruh opsi jawaban,
		 * dan kontrol penyuntingannya.
		 *
		 * <p>Dua argumen terakhir menentukan mode sunting dan keduanya diberi syarat yang SAMA:
		 * pengguna bukan mahasiswa, bukan siswa, dan bukan calon mahasiswa. Syarat ini lebih ketat
		 * daripada syarat tombol toolbar di {@link DetailGrupSoalHelper#display(PenjelasanBankSoal,
		 * Component)} yang hanya memeriksa {@code getMahasiswa() == null}, sehingga siswa dan calon
		 * mahasiswa masih melihat tombol "Ambil Soal"/"Soal Baru"/"Upload"/"Hapus" walau tidak dapat
		 * menyunting isi soal langsung di grid.</p>
		 *
		 * @param arg0 baris grid ZK tujuan render
		 * @param arg1 instance {@link BankSoal} untuk baris ini
		 */
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			BankSoal bankSoal = (BankSoal) arg1;
			DetailUjianHelper.tampilSoalDanJawaban(arg0, bankSoal, null, tbmuser, true, true, true, ubahEventListener,
					tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null,
					tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null);
		}

	}

	/**
	 * Mengekspor hasil {@code criteria} (daftar {@link BankSoal}) ke spreadsheet Excel dan
	 * mengirimkannya sebagai unduhan {@code bank_soal__.xlsx} lewat {@link Filedownload#save}. Satu
	 * baris per soal: teks soal, huruf jawaban benar, skor benar/salah/default, isi opsi jawaban
	 * (kolom JWB_A..JWB_J untuk pilihan ganda, atau jawaban esai untuk jenis lain), penjelasan, flag
	 * tampil-penjelasan-saat-ujian, dan jenis soal.
	 *
	 * <p>Varian LINTAS GRUP: berkas hasil tidak memuat id {@link PenjelasanBankSoal} pada sel (0,0)
	 * dan tidak memuat kolom nomor urut, sehingga tidak dapat diunggah kembali lewat
	 * {@link #doUpload(Media, PenjelasanBankSoal, DataLoader)} dengan hasil yang setara. Kolom
	 * PENJELASAN (indeks 16) diisi hasil {@code toString()} grup soal, bukan id-nya &mdash; nilai
	 * itulah yang kelak diresolusi kembali menjadi entitas saat impor.</p>
	 *
	 * <p>Nama berkas unduhan bersifat tetap ({@code bank_soal__.xlsx}) sehingga unduhan berturutan
	 * saling menimpa di folder unduhan pengguna.</p>
	 *
	 * <p>Isi sel opsi jawaban ditulis berurutan mengikuti hasil {@code ambilBankSoalDetail(true)}
	 * dengan penambahan indeks kolom, BUKAN berdasarkan huruf opsinya. Bila sebuah soal memiliki
	 * opsi yang hurufnya tidak berurutan (mis. A, C, D karena B pernah dihapus), isinya akan
	 * bergeser ke kolom JWB_A, JWB_B, JWB_C sehingga huruf pada kolom "BENAR" tidak lagi selaras
	 * dengan posisi kolom jawabannya.</p>
	 *
	 * <p>Method ini tidak memeriksa peran maupun kepemilikan data; seluruh baris hasil
	 * {@code criteria} diekspor apa adanya. Saat dokumentasi ini ditulis, method ini belum
	 * dipanggil dari mana pun di basis kode.</p>
	 *
	 * @param criteria kriteria Hibernate untuk {@link BankSoal} yang akan diekspor
	 * @throws Exception diteruskan dari kegagalan pembangunan spreadsheet atau I/O
	 */
	@SuppressWarnings("unchecked")
	public static void doDownload(Criteria criteria) throws Exception {

		List<BankSoal> bankSoals = criteria.list();

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(20);
		spreadsheet.setMaxrows(bankSoals.size() + 5);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 5;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasaConfig("SOAL"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasaConfig("BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("NILAI SKOR BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("NILAI SKOR SALAH"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("NILAI SKOR DEFAULT"));
		for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), "JWB_" + ((char) i));
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("PENJELASAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex),
				Common.getBahasaConfig("TAMPIL PENJELASAN SAAT UJIAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("JENIS"));

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		Utils.setColumnWidth(sheet, 1, 450);
		Utils.setColumnWidth(sheet, 2, 30);
		Utils.setColumnWidth(sheet, 0, 25);
		Utils.setRowHeight(sheet, 4, 40);
		Utils.setRowHeight(sheet, 2, 1);
		try {

			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1, Common.getBahasaConfig("DAFTAR SOAL"));
			Utils.setRowHeight(sheet, 1, 130);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		rowIndex = 5;
		colIndex = 1;
		for (BankSoal bankSoal : bankSoals) {

			colIndex = 1;
			if (bankSoal == null) {
				continue;
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, bankSoal.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, bankSoal.getSoal());

			List<Long> bankSoalDetail = bankSoal == null || bankSoal.getId() == null ? new ArrayList<Long>()
					: bankSoal.ambilBankSoalDetail(true);

			if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
				String benar = "";
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						if (detail.getBetul() != null && detail.getBetul()) {
							benar += (benar.equals("") ? detail.getHuruf() : "," + detail.getHuruf());
						}
					}
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, benar);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkor());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorSalah());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorDefault());
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getJawaban() == null ? "" : detail.getJawaban());
					}
				}

			} else {
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getEssay() == null ? "" : detail.getEssay());
					}
				}
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16,
					bankSoal.getPenjelasanBankSoal() == null ? "" : bankSoal.getPenjelasanBankSoal().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, bankSoal.getTampilPenjelasanSaatUjian());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, bankSoal.getJenis());
			rowIndex++;

		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		String fileName = "bank_soal__.xlsx";
		fileName = fileName.replaceAll(" ", "_");
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				fileName);
	}

	/**
	 * Varian {@link #doDownload(Criteria)} khusus satu {@link PenjelasanBankSoal} (grup soal):
	 * struktur kolom sama, ditambah kolom nomor urut dan id grup ditulis ke sel (0,0) untuk
	 * dipakai ulang saat impor. Nama berkas unduhan menyertakan id dan nama grup soal.
	 *
	 * @param penjelasanBankSoal grup soal yang diekspor (dipakai untuk nama berkas dan kolom nomor
	 *                           urut); boleh {@code null}
	 * <p>Inilah varian yang benar-benar dipakai aplikasi, lewat {@link #initSpreadsheet()}. Berkas
	 * hasilnya dirancang untuk dapat disunting lalu diunggah kembali lewat
	 * {@link #doUpload(Media, PenjelasanBankSoal, DataLoader)}: kolom 0 memuat id
	 * {@link BankSoal} yang menjadi kunci pencocokan utama saat impor.</p>
	 *
	 * <p>Perhatikan bahwa kolom NO. (indeks 19) diisi {@code penjelasanBankSoal.getNomorUrut()},
	 * yaitu nomor urut GRUP &mdash; nilai yang sama untuk seluruh baris &mdash; bukan nomor urut
	 * masing-masing soal. Saat diunggah kembali, nilai itu ditulis ke {@code nomorUrut} setiap
	 * {@link BankSoal}, sehingga siklus ekspor-impor tanpa penyuntingan manual akan menyamakan
	 * nomor urut seluruh soal dalam grup dan menghilangkan urutan yang sebelumnya berbeda.
	 * Baris ini juga mensyaratkan {@code penjelasanBankSoal} tidak {@code null} walaupun parameter
	 * lain di method ini sudah dijaga terhadap {@code null}.</p>
	 *
	 * <p>Sama seperti overload lintas-grupnya, isi sel opsi jawaban ditulis berurutan mengikuti
	 * hasil {@code ambilBankSoalDetail(true)} dan bukan berdasarkan hurufnya, serta method ini
	 * tidak memeriksa peran maupun kepemilikan data.</p>
	 *
	 * @param criteria           kriteria Hibernate untuk {@link BankSoal} yang akan diekspor
	 * @throws Exception diteruskan dari kegagalan pembangunan spreadsheet atau I/O
	 */
	@SuppressWarnings("unchecked")
	public static void doDownload(PenjelasanBankSoal penjelasanBankSoal, Criteria criteria) throws Exception {

		List<BankSoal> bankSoals = criteria.list();

		Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
		spreadsheet.setWidth("100%");
		spreadsheet.setHeight("100%");
		spreadsheet.setSrc("../../WEB-INF/rowcolumn.xlsx");
		spreadsheet.setMaxcolumns(20);
		spreadsheet.setMaxrows(bankSoals.size() + 5);
		final String color = "#000000";

		Worksheet sheet = spreadsheet.getSelectedSheet();

		int rowIndex = 4;
		int colIndex = 5;
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 1, Common.getBahasaConfig("SOAL"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 2, Common.getBahasaConfig("BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 3, Common.getBahasaConfig("NILAI SKOR BENAR"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 4, Common.getBahasaConfig("NILAI SKOR SALAH"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 5, Common.getBahasaConfig("NILAI SKOR DEFAULT"));
		for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), "JWB_" + ((char) i));
		}
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("PENJELASAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex),
				Common.getBahasaConfig("TAMPIL PENJELASAN SAAT UJIAN"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("JENIS"));
		ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, (++colIndex), Common.getBahasaConfig("NO."));

		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(colIndex, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);
		if (penjelasanBankSoal != null) {
			ais.ui.util.EcampusUtil.setCellValue(sheet, 0, 0, penjelasanBankSoal.getId());
		}
		Utils.setColumnWidth(sheet, 1, 450);
		Utils.setColumnWidth(sheet, 2, 30);
		Utils.setColumnWidth(sheet, 0, 25);
		Utils.setRowHeight(sheet, 4, 40);
		Utils.setRowHeight(sheet, 2, 1);
		try {

			ais.ui.util.EcampusUtil.setCellValue(sheet, 1, 1, Common.getBahasaConfig("DAFTAR SOAL UJIAN"));
			Utils.setRowHeight(sheet, 1, 130);
			ais.ui.util.EcampusUtil.setBold(sheet, new Rect(0, 1, spreadsheet.getMaxcolumns() - 1, 1), true);
			Cell cell = Utils.getCell(sheet, 1, 1);
			cell.getCellStyle().setWrapText(true);
			cell.getCellStyle().setAlignment(CellStyle.ALIGN_CENTER);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		ais.ui.util.EcampusUtil.mergeCells(sheet, 1, 1, 1, spreadsheet.getMaxcolumns() - 1, false);
		ais.ui.util.EcampusUtil.setBorder(sheet, new Rect(1, rowIndex, spreadsheet.getMaxcolumns() - 1, rowIndex),
				BookHelper.BORDER_FULL, BorderStyle.THIN, color);
		ais.ui.util.EcampusUtil.setBorder(sheet,
				new Rect(1, rowIndex + 1, spreadsheet.getMaxcolumns() - 1, rowIndex + 1), BookHelper.BORDER_FULL,
				BorderStyle.THIN, color);

		rowIndex = 5;
		colIndex = 1;
		for (BankSoal bankSoal : bankSoals) {
			colIndex = 1;
			if (bankSoal == null) {
				continue;
			}
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 0, bankSoal.getId());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, colIndex, bankSoal.getSoal());

			List<Long> bankSoalDetail = bankSoal == null || bankSoal.getId() == null ? new ArrayList<Long>()
					: bankSoal.ambilBankSoalDetail(true);

			if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
				String benar = "";
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						if (detail.getBetul() != null && detail.getBetul()) {
							benar += (benar.equals("") ? detail.getHuruf() : "," + detail.getHuruf());
						}
					}
				}
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, benar);
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkor());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorSalah());
				ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex, bankSoal.getSkorDefault());
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getJawaban() == null ? "" : detail.getJawaban());
					}
				}

			} else {
				for (Long detailid : bankSoalDetail) {
					BankSoalDetail detail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							detailid.toString());
					if (detail != null) {
						ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, ++colIndex,
								detail.getEssay() == null ? "" : detail.getEssay());
					}
				}
			}

			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 16,
					bankSoal.getPenjelasanBankSoal() == null ? "" : bankSoal.getPenjelasanBankSoal().toString());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 17, bankSoal.getTampilPenjelasanSaatUjian());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 18, bankSoal.getJenis());
			ais.ui.util.EcampusUtil.setCellValue(sheet, rowIndex, 19, penjelasanBankSoal.getNomorUrut());
			rowIndex++;

		}

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		spreadsheet.getBook().write(bout);
		bout.close();
		String fileName = "template_ujian_"
				+ (penjelasanBankSoal == null ? "" : penjelasanBankSoal.getId() + "_" + penjelasanBankSoal.getNama())
				+ "_.xlsx";
		fileName = fileName.replaceAll(" ", "_");
		Filedownload.save(bout.toByteArray(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				fileName);
	}

	/**
	 * Mengimpor soal dari berkas Excel (.xlsx, format yang sama dengan hasil {@link #doDownload})
	 * TANPA membatasi pencarian pada satu grup — soal dicocokkan lintas seluruh {@link BankSoal}
	 * berdasarkan id (kolom pertama) atau, bila tidak ada, teks soal persis sama. Untuk tiap baris
	 * valid, membuat/memperbarui {@link BankSoal} dan {@link BankSoalDetail} per opsi jawaban (atau
	 * satu baris esai untuk jenis non-pilihan-ganda), lalu menyimpulkan {@code jenisPilihanGanda}.
	 * Menampilkan ringkasan jumlah baris terupload lewat {@link MyMessageboxConfig}, lalu memicu
	 * {@code dataLoader.loadData(true)}.
	 *
	 * @param media      berkas Excel yang diunggah; ditolak dengan pesan error bila bukan {@code .xlsx}
	 * @param dataLoader callback muat-ulang data pemanggil setelah impor selesai
	 * @throws Exception diteruskan dari kegagalan I/O atau parsing Excel
	 */
	public static void doUpload(Media media, final DataLoader dataLoader) throws Exception {
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
			XSSFSheet sheet = workbook.getSheetAt(0);

			List<List<String>> objects = Common.getSheetContent(sheet);

			int terupload = 0;
			Session session = HibernateUtil.currentNativeSession();
			for (List<String> strings : objects) {

				try {
					String id = strings.get(0);
					String soal = strings.get(1);

					if (soal != null) {
						soal = soal.trim();
					} else {
						soal = "";
					}

					if (soal != null && !soal.isEmpty() && !soal.equalsIgnoreCase("soal")
							&& !soal.equalsIgnoreCase("DAFTAR SOAL UJIAN")) {

						String benar = strings.get(2);
						String skor = strings.get(3);
						String skorSalah = strings.get(4);
						String benarDefault = strings.get(5);
						String[] betuls = benar.split(",");

						String penjelasan = "";
						try {
							penjelasan = strings.get(16);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:636");

						}

						PenjelasanBankSoal penjelasanBankSoal = (PenjelasanBankSoal) Common
								.getContentAsObject(penjelasan, PenjelasanBankSoal.class, null);

						Boolean tampilPenjelasanSaatUjian = false;
						try {
							tampilPenjelasanSaatUjian = Boolean.parseBoolean(strings.get(17).trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:646");

						}

						String jenis = BankSoal.PILIHAN_GANDA;
						try {
							jenis = strings.get(18);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:653");

						}

						if (id != null) {
							id = org.apache.commons.lang3.StringUtils.replace(id, ".", "");
						}

						System.out.println("id " + id + ", soal " + soal);
						System.out.println("benar " + benar + ", skor " + skor + ", skorSalah " + skorSalah
								+ ", benarDefault " + benarDefault + ", jenis = " + jenis);

						BankSoal newBankSoal = (BankSoal) session.createCriteria(BankSoal.class)
								.add(Restrictions.or(
										id == null || !Common.isNumber(id) ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("id", Long.parseLong(id.trim())),
										Restrictions.ilike("soal", soal, MatchMode.EXACT)))
								.setMaxResults(1).uniqueResult();

						if (newBankSoal == null) {
							newBankSoal = new BankSoal();
						}
						try {
							newBankSoal.setSkor(Double.parseDouble(skor.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:677");

						}
						try {
							newBankSoal.setSkorSalah(Double.parseDouble(skorSalah.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:682");

						}
						try {
							newBankSoal.setSkorDefault(Double.parseDouble(benarDefault.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:687");

						}

						newBankSoal.setJenis(jenis);
						newBankSoal.setKeterangan("");
						newBankSoal.setSoal(soal);
						newBankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
						newBankSoal.setTampilPenjelasanSaatUjian(tampilPenjelasanSaatUjian);
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();
						int jumlahJawaban = 0;
						if (newBankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
							int j = 6;
							for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
								try {
									String huruf = ((char) i) + "";
									String jawaban = strings.get(j);
									if (jawaban != null && !jawaban.trim().equals("")) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) session
												.createCriteria(BankSoalDetail.class)
												.add(Restrictions.eq("bankSoal", newBankSoal))
												.add(Restrictions.eq("huruf", huruf)).setMaxResults(1).uniqueResult();
										if (bankSoalDetail == null) {
											bankSoalDetail = new BankSoalDetail();
										}

										bankSoalDetail.setBankSoal(newBankSoal);
										boolean betul = false;
										for (String s : betuls) {
											betul |= (s != null && s.trim().equalsIgnoreCase(huruf));
										}
										bankSoalDetail.setBetul(betul);
										bankSoalDetail.setEssay("");
										bankSoalDetail.setHuruf(huruf);
										bankSoalDetail.setJawaban(jawaban);
										bankSoalDetail.setKeterangan("");

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, bankSoalDetail);
										session.getTransaction().commit();
									}
									j++;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:731");

								}
							}

						} else {
							BankSoalDetail bankSoalDetail = (BankSoalDetail) session
									.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", newBankSoal))
									.setMaxResults(1).uniqueResult();
							if (bankSoalDetail == null) {
								bankSoalDetail = new BankSoalDetail();
							}
							bankSoalDetail.setBankSoal(newBankSoal);
							bankSoalDetail.setBetul(true);
							bankSoalDetail.setEssay(benar);
							bankSoalDetail.setHuruf("");
							bankSoalDetail.setJawaban("");
							bankSoalDetail.setKeterangan("");

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, bankSoalDetail);
							session.getTransaction().commit();
						}

						Integer count = ((Number) HibernateUtil.currentSession().createCriteria(BankSoalDetail.class)
								.add(Restrictions.eq("bankSoal", newBankSoal)).add(Restrictions.eq("betul", true))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (count > 1) {
							newBankSoal.setJenisPilihanGanda(BankSoal.COMBINATION_CHOICE);
						} else if (jumlahJawaban == 2) {
							newBankSoal.setJenisPilihanGanda(BankSoal.BENAR_SALAH);
						} else {
							newBankSoal.setJenisPilihanGanda(BankSoal.MULTIPLE_COICE);
						}

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						terupload++;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailGrupSoalHelper.java:773");
				}

			}
			HibernateUtil.closeSession();

			MyMessageboxConfig.show("Upload soal telah selesai dilakukan, " + terupload + " terupload", "Pemberitahuan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(true);
						}
					});

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	/**
	 * Varian {@link #doUpload(Media, DataLoader)} yang membatasi pencarian/penautan soal pada satu
	 * {@code penjelasanBankSoal} (grup) tertentu — soal yang cocok (via id atau teks persis) dicari
	 * hanya di dalam grup ini, dan soal baru otomatis ditautkan ke grup ini. Kolom nomor urut (kolom
	 * ke-20) turut dibaca dan disimpan ke {@code nomorUrut}.
	 *
	 * @param media              berkas Excel yang diunggah
	 * @param penjelasanBankSoal grup soal tujuan impor
	 * @param dataLoader         callback muat-ulang data pemanggil setelah impor selesai
	 * @throws Exception diteruskan dari kegagalan I/O atau parsing Excel
	 */
	public static void doUpload(Media media, PenjelasanBankSoal penjelasanBankSoal, final DataLoader dataLoader)
			throws Exception {
		if (media.getName().toLowerCase().endsWith("xlsx")) {

			InputStream inputStream = media.getStreamData();
			// System.out.println("media = " + media);
			File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			// System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
			XSSFSheet sheet = workbook.getSheetAt(0);

			List<List<String>> objects = Common.getSheetContent(sheet);

			int terupload = 0;
			Session session = HibernateUtil.currentNativeSession();
			for (List<String> strings : objects) {

				try {
					String id = strings.get(0);
					String soal = strings.get(1);

					if (soal != null) {
						soal = soal.trim();
					} else {
						soal = "";
					}

					if (soal != null && !soal.isEmpty() && !soal.equalsIgnoreCase("soal")
							&& !soal.equalsIgnoreCase("DAFTAR SOAL UJIAN")) {

						String benar = strings.get(2);
						String skor = strings.get(3);
						String skorSalah = strings.get(4);
						String benarDefault = strings.get(5);
						String[] betuls = benar.split(",");

						Boolean tampilPenjelasanSaatUjian = false;
						try {
							tampilPenjelasanSaatUjian = Boolean.parseBoolean(strings.get(17).trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:844");

						}

						String jenis = BankSoal.PILIHAN_GANDA;
						try {
							jenis = strings.get(18);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:851");

						}

						String no = "0";
						try {
							no = strings.get(19);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:858");

						}

						if (id != null) {
							id = org.apache.commons.lang3.StringUtils.replace(id, ".", "");
						}

						System.out.println("id " + id + ", soal " + soal);
						System.out.println("benar " + benar + ", skor " + skor + ", skorSalah " + skorSalah
								+ ", benarDefault " + benarDefault + ", jenis = " + jenis);

						BankSoal newBankSoal = (BankSoal) session.createCriteria(BankSoal.class)
								.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal))
								.add(Restrictions.or(
										id == null || !Common.isNumber(id) ? Restrictions.sqlRestriction("false")
												: Restrictions.eq("id", Long.parseLong(id.trim())),
										Restrictions.ilike("soal", soal, MatchMode.EXACT)))
								.setMaxResults(1).uniqueResult();

						if (newBankSoal == null) {
							newBankSoal = new BankSoal();
						}
						try {
							newBankSoal.setSkor(Double.parseDouble(skor.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:883");

						}
						try {
							newBankSoal.setSkorSalah(Double.parseDouble(skorSalah.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:888");

						}
						try {
							newBankSoal.setSkorDefault(Double.parseDouble(benarDefault.trim()));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:893");

						}

						try {
							newBankSoal.setNomorUrut(Integer.parseInt(no));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:899");

						}

						newBankSoal.setJenis(jenis);
						newBankSoal.setKeterangan("");
						newBankSoal.setSoal(soal);
						newBankSoal.setPenjelasanBankSoal(penjelasanBankSoal);
						newBankSoal.setTampilPenjelasanSaatUjian(tampilPenjelasanSaatUjian);

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						int jumlahJawaban = 0;
						if (newBankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)) {
							int j = 6;
							for (int i = ((int) 'A'); i <= ((int) 'J'); i++) {
								try {
									String huruf = ((char) i) + "";
									String jawaban = strings.get(j);
									if (jawaban != null && !jawaban.trim().equals("")) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) session
												.createCriteria(BankSoalDetail.class)
												.add(Restrictions.eq("bankSoal", newBankSoal))
												.add(Restrictions.eq("huruf", huruf)).setMaxResults(1).uniqueResult();
										if (bankSoalDetail == null) {
											bankSoalDetail = new BankSoalDetail();
										}

										bankSoalDetail.setBankSoal(newBankSoal);
										boolean betul = false;
										for (String s : betuls) {
											betul |= (s != null && s.trim().equalsIgnoreCase(huruf));
										}
										bankSoalDetail.setBetul(betul);
										bankSoalDetail.setEssay("");
										bankSoalDetail.setHuruf(huruf);
										bankSoalDetail.setJawaban(jawaban);
										bankSoalDetail.setKeterangan("");

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, bankSoalDetail);
										session.getTransaction().commit();
										jumlahJawaban++;
									}
									j++;
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:946");

								}
							}

						} else {
							BankSoalDetail bankSoalDetail = (BankSoalDetail) session
									.createCriteria(BankSoalDetail.class).add(Restrictions.eq("bankSoal", newBankSoal))
									.setMaxResults(1).uniqueResult();
							if (bankSoalDetail == null) {
								bankSoalDetail = new BankSoalDetail();
							}
							bankSoalDetail.setBankSoal(newBankSoal);
							bankSoalDetail.setBetul(true);
							bankSoalDetail.setEssay(benar);
							bankSoalDetail.setHuruf("");
							bankSoalDetail.setJawaban("");
							bankSoalDetail.setKeterangan("");

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, bankSoalDetail);
							session.getTransaction().commit();
						}

						Integer count = ((Number) HibernateUtil.currentSession().createCriteria(BankSoalDetail.class)
								.add(Restrictions.eq("bankSoal", newBankSoal)).add(Restrictions.eq("betul", true))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (count > 1) {
							newBankSoal.setJenisPilihanGanda(BankSoal.COMBINATION_CHOICE);
						} else if (jumlahJawaban == 2) {
							newBankSoal.setJenisPilihanGanda(BankSoal.BENAR_SALAH);
						} else {
							newBankSoal.setJenisPilihanGanda(BankSoal.MULTIPLE_COICE);
						}

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, newBankSoal);
						session.getTransaction().commit();

						terupload++;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailGrupSoalHelper.java:988");
				}

			}
			HibernateUtil.closeSession();

			MyMessageboxConfig.show("Upload soal telah selesai dilakukan, " + terupload + " terupload", "Pemberitahuan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dataLoader.loadData(true);
						}
					});

		} else {
			MyMessageboxConfig.show(
					"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
							+ media,
					"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
		}
	}

	/** Mengekspor seluruh soal pada {@link #penjelasanBankSoal} (diurutkan nomor urut lalu id) lewat {@link #doDownload(PenjelasanBankSoal, Criteria)}. */
	private void initSpreadsheet() throws Exception {
		DetailGrupSoalHelper.doDownload(penjelasanBankSoal,
				HibernateUtil.currentSession().createCriteria(BankSoal.class)
						.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal)).addOrder(Order.asc("nomorUrut"))
						.addOrder(Order.asc("id")));
	}

	/** Mengimpor {@code media} ke {@link #penjelasanBankSoal} lewat {@link #doUpload(Media, PenjelasanBankSoal, DataLoader)}, dengan {@code this} sebagai callback muat-ulang. */
	private void uploadSoal(Media media, PenjelasanBankSoal penjelasanBankSoal) throws Exception {
		DetailGrupSoalHelper.doUpload(media, penjelasanBankSoal, this);
	}

	/**
	 * Memuat satu halaman {@link BankSoal} milik {@link #penjelasanBankSoal} (diurutkan nomor urut
	 * lalu id) ke grid, sesuai halaman aktif pada {@link #paging}.
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void loadData(Object value) {

		List<BankSoal> bankSoals = HibernateUtil.currentSession().createCriteria(BankSoal.class)
				.add(Restrictions.eq("penjelasanBankSoal", penjelasanBankSoal)).addOrder(Order.asc("nomorUrut"))
				.addOrder(Order.asc("id"))
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_1 * (paging == null ? 0 : paging.getActivePage()))
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE_1).list();

		ListModel strset = new SimpleListModel(bankSoals);
		grid.setRowRenderer(new DetailUjianRenderer());
		grid.setModel(strset);
		grid.setSclass("fgrid");
		grid.setOddRowSclass("non-odd");

		try {
			paging.setPageSize(Common.ROWS_COUNT_ON_PAGE_1);
			paging.setMold("os");
			paging.setTotalSize(bankSoals.size());
			paging.setVisible(bankSoals.size() > Common.ROWS_COUNT_ON_PAGE_1);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailGrupSoalHelper.java:1043");

		}
	}

}
