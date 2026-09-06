package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.MahasiswaRegistrasiWisudaAction;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanAlbumMahasiswaWisuda;
import ais.action.report.format1.akademik.LaporanAlbumProfileWisuda;
import ais.action.report.format1.akademik.LaporanMahasiswaWisuda;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Skripsi;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Wisuda;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Helper administrasi peserta satu acara {@link Wisuda}: menampilkan & mengelola daftar
 * pendaftaran wisuda ({@link PendaftaranWisuda}) per mahasiswa — status persetujuan (keuangan,
 * administrasi, perpustakaan tingkat universitas/fakultas), nomor registrasi/kursi, ukuran toga,
 * dan status kelulusan terkini. Menyediakan pencarian (NIM/nama, fakultas/jurusan), penambahan
 * peserta dari pencarian mahasiswa, pencetakan berbagai laporan (peserta, album foto, profil,
 * bukti kartu wisuda, transkrip), ekspor Excel dengan kolom turunan (IPS/IPK/SKS/masa studi
 * dihitung saat ekspor, bukan disimpan), impor Excel massal, dan sinkronisasi status kelulusan
 * massal ke seluruh mahasiswa yang disetujui wisuda (menandai LULUS, menghitung semester lulus,
 * tahun lulus/wisuda, dan predikat kelulusan/yudisium).
 *
 * <p>
 * Mengimplementasikan {@link DataLoader} (callback penyegaran) dan {@link DataCriteria}
 * (kriteria pencarian dipakai bersama oleh paging server-side, sinkronisasi status, dan ekspor
 * Excel via {@link Common#cetakDataCustomButton}).
 * </p>
 */
public class DetailwisudaHelper implements DataLoader, DataCriteria {

	/** Grid daftar peserta wisuda; dibuat di {@link #display} dengan mold paging 50 baris dan diisi ulang tiap {@link #loadData(Object)}. */
	private MyGrid grid;
	/** Acara wisuda yang sedang dibuka. Menjadi satu-satunya penyaring kepemilikan baris di {@link #initCriteria(boolean)}. */
	private Wisuda wisuda;
	/** Paging server-side; ukuran halaman {@link Common#ROWS_COUNT_ON_PAGE}, dibuat di akhir {@link #display}. */
	private Paging paging;

	/** Kotak pencarian NIM mahasiswa (cocok sebagian/contains). */
	private Textbox nim;
	/** Kotak pencarian nama mahasiswa (cocok sebagian/contains). */
	private Textbox nama;
	/** Filter fakultas; disaring lewat alias {@code jurusan.fakultas} pada mahasiswa peserta. */
	private Combobox searchfakultas;
	/** Filter jurusan/program studi; disaring lewat {@code mahasiswa.jurusan}. */
	private Combobox searchjurusan;

	/**
	 * Hak ubah pengguna saat ini ({@link CommonPrivilages#UPDATE}), dibaca SEKALI di konstruktor.
	 * Mengendalikan visibilitas tombol baris "No." (generate no. kursi/registrasi) dan "Ubah".
	 * Karena hanya mengatur visibilitas komponen, bendera ini bukan gerbang otorisasi sisi server —
	 * pemeriksaan sebenarnya tetap milik alur tujuan masing-masing tombol.
	 */
	private boolean edit;
	/**
	 * Hak hapus pengguna saat ini ({@link CommonPrivilages#DELETE}), dibaca SEKALI di konstruktor.
	 * Mengendalikan visibilitas tombol baris "Hapus" — yang meski bernama hapus sebenarnya hanya
	 * MELEPAS relasi wisuda dari {@link PendaftaranWisuda}, bukan menghapus barisnya.
	 */
	private boolean delete;

	/** Kolom entitas yang diekspor/diimpor pada tombol "Download Peserta"/upload utama, memetakan langsung ke properti {@link PendaftaranWisuda} dan {@link Mahasiswa} terkait. */
	public static String[] contents = new String[] { "id", "mahasiswa", "mahasiswa.judulSkripsi", "mahasiswa.noAkta1",
			"mahasiswa.tanggalLulus", "tanggalDaftarWisuda", "skripsi", "wisuda", "statusPersetujuanKeuangan",
			"statusPersetujuanAdministrasi", "statusPersetujuanPerpustakaan", "statusPersetujuanPerpustakaanFakultas",
			"statusPersetujuanAdministrasiFakultas", "persetujuanWisuda", "noRegistrasiWisuda", "noKursi", "ukuranToga",
			"keterangan" };

	/** Kolom identitas dasar yang diekspor pada tombol "Download Wisudawan" (data biografis ringkas untuk keperluan cetak ijazah/undangan). */
	public static String[] contentsBaru = new String[] { "id", "mahasiswa.nim", "mahasiswa.nama",
			"mahasiswa.jurusan.fakultas.nama", "mahasiswa.jurusan.nama", "mahasiswa.kelamin", "mahasiswa.tempatlahir",
			"mahasiswa.tanggallahir" };

	/**
	 * Menambahkan tautan foto mahasiswa ke sel Excel tanpa menggagalkan ekspor bila foto tidak
	 * tersedia: foto tidak ada, URL kosong, maupun exception apa pun sama-sama berakhir sebagai
	 * sel bernilai string kosong (exception dicatat lewat {@code ErrorAuditUtil}, tidak
	 * dilempar ulang). Dipakai oleh KEDUA tombol ekspor — "Download Peserta" dan "Download
	 * Wisudawan" — pada indeks kolom terakhir masing-masing.
	 *
	 * <p>
	 * URL sengaja dibangun lewat {@code createLinkUri(false)}, yaitu rute servlet aplikasi
	 * ({@code /al}), bukan URL folder statis {@code /f}, karena sebagian instalasi menutup akses
	 * langsung ke folder media di tingkat Apache.
	 * </p>
	 *
	 * @param row           baris Excel tujuan
	 * @param cellIndex     indeks sel yang ditulis
	 * @param hyperlinkStyle gaya sel untuk tampilan tautan; hanya dipasang bila foto memang ada
	 * @param mahasiswa     mahasiswa yang fotonya dicari lewat {@link FileFotoLain#ambil}
	 */
	private static void tambahLinkFoto(XSSFRow row, int cellIndex, XSSFCellStyle hyperlinkStyle,
			Mahasiswa mahasiswa) {
		XSSFCell cell = row.createCell(cellIndex);
		try {
			FileFotoLain foto = FileFotoLain.ambil(mahasiswa.getId(), FotoMahasiswa.DEFAULT_JENIS,
					FotoMahasiswa.class);
			if (foto == null) {
				cell.setCellValue("");
				return;
			}

			// Gunakan rute servlet aplikasi (/al), bukan URL folder statis /f yang
			// dapat ditolak Apache pada instalasi yang menutup akses langsung media.
			String url = foto.createLinkUri(false);
			if (url == null || url.trim().isEmpty()) {
				cell.setCellValue("");
				return;
			}

			cell.setCellValue("Lihat Foto");
			cell.setCellStyle(hyperlinkStyle);
			XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
					.createHyperlink(Hyperlink.LINK_URL);
			link.setAddress(url);
			cell.setHyperlink(link);
		} catch (Exception e) {
			cell.setCellValue("");
			ais.common.ErrorAuditUtil.record(e,
					"Gagal menambahkan link foto pada download peserta/wisudawan");
		}
	}

	/** Menyiapkan combobox filter fakultas/jurusan dan hak edit/hapus pengguna saat ini. */
	public DetailwisudaHelper() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas = new Combobox(),
				searchjurusan = new Combobox());
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/**
	 * Merender satu baris peserta wisuda: foto, no. registrasi/kursi, status persetujuan, NIM/
	 * nama/angkatan, status akademik terkini (gabungan status keluar/status mahasiswa/status
	 * awal/semester), fakultas/jurusan, dan judul skripsi (di-cache otomatis ke
	 * {@link Mahasiswa#getJudulSkripsi()} dan ditautkan ke {@link Skripsi} bila belum ada, saat
	 * baris dirender pertama kali). Tombol aksi: Transkrip (pratinjau), No. (generate nomor
	 * kursi/registrasi, staf ber-{@link #edit}), Ubah (staf ber-{@link #edit}, mensyaratkan data
	 * skripsi sudah ada), Bukti (cetak kartu), Hapus (staf ber-{@link #delete}, hanya melepas
	 * relasi wisuda dari pendaftaran — bukan menghapus baris {@link PendaftaranWisuda}).
	 */
	class DetailWisudaRenderer extends ais.ui.util.MyRowRenderer {

		/** Renderer tanpa state sendiri; hak {@link #edit}/{@link #delete} dan konteks acara dibaca dari kelas induk. */
		public DetailWisudaRenderer() {

		}

		/**
		 * Mengisi satu baris peserta sesuai urutan kolom yang didefinisikan di {@link #display}:
		 * foto, No. Reg, No. Kursi, Persetujuan, NIM, Nama, Angkatan, Status, Fakultas, Jurusan,
		 * judul skripsi, lalu kolom aksi.
		 *
		 * <p>
		 * <b>Efek samping yang perlu diketahui:</b> render bukan operasi baca-saja. Bila
		 * {@link PendaftaranWisuda#getSkripsi()} masih kosong, method ini mencari {@link Skripsi}
		 * milik mahasiswa lewat native session tersendiri lalu MENULIS dua hal ke basis data —
		 * menautkan skripsi itu ke pendaftaran, dan menyalin judulnya ke
		 * {@link Mahasiswa#setJudulSkripsi(String)} — masing-masing dalam transaksi terpisah.
		 * Native session itu ditutup manual dan diikuti {@code HibernateUtil.closeSession()},
		 * karena penulisan terjadi di luar session milik request. Konsekuensinya, sekadar membuka
		 * atau membalik halaman daftar peserta dapat memicu penulisan dan revisi audit.
		 * </p>
		 *
		 * <p>
		 * Kolom Status merangkai status keluar, status mahasiswa terkini
		 * ({@code HistoryStatusMahasiswaUtil.currentStatus}), dan status awal; bagian semester
		 * berjalan/semester mulai (plus tahapan bila diaktifkan konfigurasi) SENGAJA dilewati untuk
		 * mahasiswa yang sudah berstatus LULUS, karena bagi mereka angka itu tidak lagi bermakna.
		 * </p>
		 *
		 * <p>
		 * Tombol aksi dikumpulkan ke satu popup kebab ({@code UIHelper.buatBarisAksi}): Transkrip
		 * (pratinjau, selalu tampil), No. dan Ubah (hanya bila {@link #edit}; Ubah menolak jalan
		 * bila data skripsi belum ada), Bukti (selalu tampil — lihat catatan efek samping pada
		 * {@link #cetakBukti(PendaftaranWisuda)}), dan Hapus (hanya bila {@link #delete}, dan hanya
		 * melepas relasi wisuda, bukan menghapus baris pendaftaran).
		 * </p>
		 *
		 * @param row  baris grid yang diisi
		 * @param data objek {@link PendaftaranWisuda} untuk baris ini
		 * @throws Exception diteruskan dari operasi komponen ZK/Hibernate
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final PendaftaranWisuda pendaftaranWisuda = (PendaftaranWisuda) data;

			CommonMedia.tampilkanGambarKecil(pendaftaranWisuda.getMahasiswa()).setParent(row);

			new Label(pendaftaranWisuda.getNoRegistrasiWisuda()).setParent(row);

			new Label(pendaftaranWisuda.getNoKursi()).setParent(row);

			new Label(
					pendaftaranWisuda.getPersetujuanWisuda() != null && pendaftaranWisuda.getPersetujuanWisuda() ? "Ya"
							: "Tidak")
					.setParent(row);

			new Label(pendaftaranWisuda.getMahasiswa().getNim()).setParent(row);
			new Label(pendaftaranWisuda.getMahasiswa().getNama()).setParent(row);
			new Label(pendaftaranWisuda.getMahasiswa().getTahunangkatan() + "").setParent(row);

			Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
			Integer tahap = (ConstantValues.aktifkanTahapan
					&& ConstantValues.getJumlahTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan()) > 2)
							? mahasiswa.currentTahapan()
							: null;
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			Html status = new ais.ui.util.MyHtml("..");
			status.setParent(row);
			status.setContent((mahasiswa.getStatusKeluar() == null ? "" : mahasiswa.getStatusKeluar().getNama() + "/")
					+ (statusMahasiswa == null ? "" : statusMahasiswa.getNama()) + "/"
					+ (mahasiswa.getStatusAwalMahasiswa() == null ? "" : mahasiswa.getStatusAwalMahasiswa().getNama())
					+ ((statusMahasiswa != null
							&& statusMahasiswa.getNama().equalsIgnoreCase(ConstantValues.LULUS.getNama()))
									? ""
									: "/" + mahasiswa.currentSemester() + "/" + mahasiswa.getSemesterMulai()
											+ ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null
													&& tahap > 0) ? "/ Thp:" + tahap : "")));

			new Label(pendaftaranWisuda.getMahasiswa().getJurusan().getFakultas().getNama()).setParent(row);

			new Label(pendaftaranWisuda.getMahasiswa().getJurusan().getNama()).setParent(row);

			Skripsi skripsi = pendaftaranWisuda.getSkripsi();
			try {
				if (skripsi == null) {
					Session session = HibernateUtil.currentNativeSession();
					skripsi = (Skripsi) session.createCriteria(Skripsi.class)
							.add(Restrictions.eq("mahasiswa", pendaftaranWisuda.getMahasiswa())).setMaxResults(1)
							.uniqueResult();
					if (skripsi != null && skripsi.getJudul() != null) {

						if (pendaftaranWisuda.getSkripsi() == null) {
							session.refresh(pendaftaranWisuda);
							pendaftaranWisuda.setSkripsi(skripsi);
							session.getTransaction().begin();
							Common.refreshUpdate(session, (pendaftaranWisuda));
							session.getTransaction().commit();
						}

						if (mahasiswa.getJudulSkripsi() == null
								|| !mahasiswa.getJudulSkripsi().equalsIgnoreCase(skripsi.getJudul())) {
							session.refresh(mahasiswa);
							mahasiswa.setJudulSkripsi(skripsi.getJudul());
							session.getTransaction().begin();
							Common.refreshUpdate(session, (mahasiswa));
							session.getTransaction().commit();
						}
					}
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailwisudaHelper.java:203");
				// TODO: handle exception
			}
			HibernateUtil.closeSession();

			if (skripsi != null && !skripsi.getJudul().isEmpty()) {
				mahasiswa.setJudulSkripsi(skripsi.getJudul());
			}
			new MyLabelKecil(mahasiswa.getJudulSkripsi()).setParent(row);

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-certificate", "Transkrip");
			aksiButtons.add(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanTranskipAkademik laporanIjazahAkademik = new LaporanTranskipAkademik(
							pendaftaranWisuda.getMahasiswa());
					laporanIjazahAkademik.setTitle("Preview Transkrip dan Ijazah");
					laporanIjazahAkademik.setClosable(true);
					laporanIjazahAkademik.setHeight("95%");
					laporanIjazahAkademik.setWidth("90%");
					laporanIjazahAkademik.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporanIjazahAkademik.onModal();
				}
			});

			MyToolbarbutton button = new MyToolbarbutton("fa-table", "No.");
			aksiButtons.add(button);
			button.setVisible(edit);
			button.setTooltiptext("Generate No Wisuda");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					GenerateNoKursiDanNoRegistrasiWindow laporanMahasiswaWisuda = new GenerateNoKursiDanNoRegistrasiWindow(
							pendaftaranWisuda.getMahasiswa());

					laporanMahasiswaWisuda.addEventListener("onClose", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(null);

						}
					});

					laporanMahasiswaWisuda.setHeight("95%");
					laporanMahasiswaWisuda.setWidth("90%");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanMahasiswaWisuda);
					laporanMahasiswaWisuda.setClosable(true);
					laporanMahasiswaWisuda.setVisible(true);
					laporanMahasiswaWisuda.onModal();
				}
			});


			button = new MyToolbarbutton("fa-pencil", "Ubah");
			button.setVisible(edit);
			aksiButtons.add(button);
			button.setTooltiptext("Ubah Status Registrasi Wisuda");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (pendaftaranWisuda.getSkripsi() == null) {
						MyMessageboxConfig.show("Mohon maaf, mahasiswa ini belum memiliki data skripsi. Langkah yang dapat dilakukan: (1) pastikan mahasiswa telah mendaftar dan memiliki data skripsi; (2) hubungi bagian akademik untuk input data skripsi mahasiswa; (3) ulangi proses ini setelah data skripsi tersedia. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.EXCLAMATION);
						return;
					}

					MahasiswaRegistrasiWisudaAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							loadData(null);
						}
					}, pendaftaranWisuda);
				}
			});

			MyToolbarbutton cetak = new MyToolbarbutton("fa-print", "Bukti");
			cetak.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					DetailwisudaHelper.cetakBukti(pendaftaranWisuda);
				}
			});
			aksiButtons.add(cetak);

			button = new MyToolbarbutton("fa-trash", "Hapus");
			button.setVisible(delete);
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

											pendaftaranWisuda.setWisuda(null);
											Common.refreshUpdate(pendaftaranWisuda);

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
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);
		}

	}

	/**
	 * Mencetak "Kartu Daftar Wisuda" (PDF) untuk satu {@code pendaftaranWisuda}. Parameter
	 * laporan mencakup yudisium hasil hitung ulang ({@link Common#hitungJudisium}), data KRS
	 * lulus tersinkron, foto kelulusan, serta seluruh properti {@link BiodataMahasiswa},
	 * {@link Mahasiswa}, dan {@link Skripsi} terkait. Dipanggil dari tombol "Bukti" pada baris
	 * grid helper ini dan dari tombol "Cetak Bukti" di {@code MahasiswaRegistrasiWisudaAction}.
	 *
	 * <p>
	 * <b>Method ini MENULIS ke basis data sebelum mencetak.</b> Sebelum menyusun laporan ia
	 * dapat menetapkan No. Kursi dari id pendaftaran yang di-padding nol hingga 8 digit lalu
	 * menyimpannya lewat {@link Common#refreshSaveOrUpdate(Object)}. Dua hal perlu diketahui
	 * pemelihara sebelum menyentuh blok tersebut:
	 * </p>
	 * <ul>
	 * <li><b>Syarat penulisannya terbalik dari maksudnya.</b> Kondisi yang dipakai adalah
	 * {@code getNoKursi() == null || !getNoKursi().isEmpty()}, sehingga nomor kursi yang SUDAH
	 * terisi justru ditulis ulang setiap kali kartu dicetak, sedangkan nomor bernilai string
	 * kosong ({@code ""}) — satu-satunya nilai yang benar-benar berarti "belum diisi" selain
	 * {@code null} — malah dilewati. Bacaan yang sejalan dengan komentar aslinya ("bila nomor
	 * kursi belum diisi") seharusnya {@code == null || isEmpty()}. Karena rumusnya idempoten
	 * (selalu id yang sama), dampaknya bukan nomor yang berubah-ubah, melainkan nomor kursi
	 * hasil penyuntingan manual atau hasil unggah Excel (kolom {@code noKursi} ada di
	 * {@link #contents}) tertimpa diam-diam, plus satu penulisan dan revisi audit pada setiap
	 * pencetakan.</li>
	 * <li><b>Tidak ada pemeriksaan persetujuan di sini.</b> Jalur resmi penomoran,
	 * {@code GenerateNoKursiDanNoRegistrasiWindow.onGenerateNoKursiWisuda}, mensyaratkan No.
	 * Registrasi sudah ada DAN kelima status persetujuan (Administrasi, Administrasi Fakultas,
	 * Keuangan, Perpustakaan, Perpustakaan Fakultas) sudah terpenuhi. Blok di method ini
	 * menetapkan No. Kursi tanpa satu pun syarat tersebut dan tanpa memeriksa hak {@link #edit},
	 * padahal tombol "Bukti" yang memanggilnya tampil untuk semua pengguna yang dapat membuka
	 * daftar peserta.</li>
	 * </ul>
	 * <p>
	 * Kedua hal di atas didokumentasikan apa adanya, BUKAN diubah di sini.
	 * </p>
	 *
	 * @param pendaftaranWisuda pendaftaran yang kartunya dicetak; mahasiswa dan skripsinya dibaca dari sini
	 * @throws Exception diteruskan dari sinkronisasi KRS, penyimpanan No. Kursi, atau {@link Report#generatePDFReport}
	 */
	@SuppressWarnings("unchecked")
	public static void cetakBukti(PendaftaranWisuda pendaftaranWisuda) throws Exception {
		if (pendaftaranWisuda.getNoKursi() == null || !pendaftaranWisuda.getNoKursi().isEmpty()) {
			String noKursi = pendaftaranWisuda.getId().toString();

			while (noKursi.length() < 8) {
				noKursi = "0" + noKursi;
			}

			pendaftaranWisuda.setNoKursi(noKursi);
			Common.refreshSaveOrUpdate(pendaftaranWisuda);
		}

		Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();

		@SuppressWarnings("rawtypes")
		Map parameters = ais.common.HashMapGenerator.getRand();

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(pendaftaranWisuda.getMahasiswa(),
				pendaftaranWisuda.getMahasiswa().getSemesterLulus() == null
						? pendaftaranWisuda.getMahasiswa().currentSemester()
						: pendaftaranWisuda.getMahasiswa().getSemesterLulus(),
				null, null, true);

		Judisium judisium = Common.hitungJudisium(pendaftaranWisuda.getMahasiswa(), krsMahasiswa);
		parameters.put("judisium", judisium == null ? "" : judisium.getNama());
		parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());

		parameters.put("id_mahasiswa", pendaftaranWisuda.getMahasiswa().getId());
		parameters.put("id_pendaftaran_wisuda", pendaftaranWisuda.getId());
		Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");

		mahasiswa.putPhotoLulus(parameters);

		BiodataMahasiswa biodataMahasiswa = pendaftaranWisuda.getMahasiswa().ambilBiodata();
		Common.insertProperty(BiodataMahasiswa.class, biodataMahasiswa, parameters, "bio");
		Common.insertProperty(Mahasiswa.class, pendaftaranWisuda.getMahasiswa(), parameters, "mhs");

		Common.insertProperty(Skripsi.class, pendaftaranWisuda.getSkripsi(), parameters, "skripsi");

		Report.generatePDFReport(Report.PDF, parameters, "kartu_daftar_wisuda", ais.ui.util.WaktuUtil.getDate());
	}

	/**
	 * Membangun {@link Criteria} pencarian {@link PendaftaranWisuda} untuk {@link #wisuda},
	 * disaring berdasarkan nama/NIM mahasiswa (contains) dan fakultas/jurusan terpilih.
	 *
	 * @param order bila {@code true}, tambahkan pengurutan berdasarkan NIM
	 * @return criteria siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PendaftaranWisuda.class).add(Restrictions.eq("wisuda", wisuda))

				.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

		;
		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));
		return criteria;
	}

	/**
	 * Memuat/menyegarkan grid dengan halaman peserta wisuda sesuai {@link #initCriteria} dan
	 * posisi {@link #paging} saat ini. Renderer baris dipasang ulang setiap pemanggilan, jadi
	 * efek samping penulisan judul skripsi pada {@code DetailWisudaRenderer.render} ikut berjalan
	 * lagi untuk setiap baris halaman yang ditampilkan.
	 *
	 * @param value tidak dipakai; parameter standar {@link DataLoader} agar helper pencarian
	 *              mahasiswa dapat memicu penyegaran tanpa mengenal tipe konkret kelas ini
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.initPaging(initCriteria(false), paging);

		List<PendaftaranWisuda> pendaftaranWisuda = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pendaftaranWisuda);
		grid.setRowRenderer(new DetailWisudaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** @return {@code this} sebagai {@link DataLoader}, diteruskan ke helper pencarian mahasiswa agar dapat memicu {@link #loadData(Object)} setelah data ditambahkan. */
	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun panel lengkap daftar peserta {@code wisuda} ke dalam {@code component}: form
	 * pencarian, toolbar aksi (Ambil Data Mahasiswa, Cari, tiga tombol cetak laporan — Peserta/
	 * Album/Profile, ekspor Excel "Download Peserta" dengan kolom IPS/IPK/SKS/masa studi
	 * turunan, upload Excel massal, "Singkronkan dengan status mahasiswa" — proses async
	 * berjalan di thread terpisah dengan progress bar yang menandai LULUS seluruh mahasiswa
	 * disetujui pada fakultas/jurusan terpilih, dan ekspor Excel kedua "Download Wisudawan"
	 * dengan kolom data ijazah/kontak/alamat lengkap) di atas grid paging 50 baris.
	 *
	 * @param wisuda    acara wisuda yang pesertanya akan ditampilkan/dikelola
	 * @param component kontainer ZK yang akan diisi (isi sebelumnya dibersihkan)
	 * @param window    jendela induk, diteruskan ke helper pencarian mahasiswa
	 */
	@SuppressWarnings("deprecation")
	public void display(final Wisuda wisuda, final Component component, final MyWindow window) {
		this.wisuda = wisuda;
		Common.clear(component);

		MyGroupboxStyled groupbox = new MyGroupboxStyled();
		groupbox.setWidth("97%");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mengikuti wisuda ini"));

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(groupbox);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(nama = new Textbox());
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
		row.appendChild(searchjurusan);

		nim.setWidth("90%");
		nama.setWidth("90%");
		searchfakultas.setWidth("90%");
		searchjurusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "8");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			private AmbilDataMahasiswaMendaftarWisudaHelper dataMahasiswaHelper = new AmbilDataMahasiswaMendaftarWisudaHelper();

			@Override
			public void onEvent(Event event) throws Exception {
				dataMahasiswaHelper.display(wisuda, getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cetak Peserta Wisuda", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				LaporanMahasiswaWisuda laporanMahasiswaWisuda = new LaporanMahasiswaWisuda(wisuda);
				laporanMahasiswaWisuda.setTitle("Peserta Wisuda");
				laporanMahasiswaWisuda.setHeight("95%");
				laporanMahasiswaWisuda.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanMahasiswaWisuda);
				laporanMahasiswaWisuda.setClosable(true);
				laporanMahasiswaWisuda.setVisible(true);
				laporanMahasiswaWisuda.onModal();
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cetak Album Wisuda", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				LaporanAlbumMahasiswaWisuda laporanAlbumMahasiswaWisuda = new LaporanAlbumMahasiswaWisuda(wisuda);
				laporanAlbumMahasiswaWisuda.setTitle("Album Wisuda");
				laporanAlbumMahasiswaWisuda.setHeight("95%");
				laporanAlbumMahasiswaWisuda.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(laporanAlbumMahasiswaWisuda);
				laporanAlbumMahasiswaWisuda.setClosable(true);
				laporanAlbumMahasiswaWisuda.setVisible(true);
				laporanAlbumMahasiswaWisuda.onModal();
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Cetak Profile Wisuda", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				LaporanAlbumProfileWisuda laporanAlbumProfileWisuda = new LaporanAlbumProfileWisuda(wisuda);
				laporanAlbumProfileWisuda.setTitle("Profile Wisuda");
				laporanAlbumProfileWisuda.setHeight("95%");
				laporanAlbumProfileWisuda.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanAlbumProfileWisuda);
				laporanAlbumProfileWisuda.setClosable(true);
				laporanAlbumProfileWisuda.setVisible(true);
				laporanAlbumProfileWisuda.onModal();
			}

		});
		button.setParent(toolbar);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Nomor KTP");
		columnHeadersAdding.add("IPS");
		columnHeadersAdding.add("IPK");
		columnHeadersAdding.add("SKS");
		columnHeadersAdding.add("SKSK");
		columnHeadersAdding.add("Masa Studi Tahun");
		columnHeadersAdding.add("Masa Studi Bulan");
		columnHeadersAdding.add("Masa Studi Hari");
		columnHeadersAdding.add("Masa Studi Deskripsi");
		columnHeadersAdding.add("Link Foto");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				PendaftaranWisuda pendaftaranWisuda = (PendaftaranWisuda) objects[0];
				Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
						mahasiswa.getSemesterLulus() == null ? mahasiswa.currentSemester()
								: mahasiswa.getSemesterLulus(),
						null, null);
				row.createCell(contents.length).setCellValue(biodataMahasiswa.getNoIdentitas());

				XSSFCell cell = row.createCell(contents.length + 1);
				cell.setCellValue(krsMahasiswa.getIps());

				cell = row.createCell(contents.length + 2);
				cell.setCellValue(krsMahasiswa.getIpk());

				cell = row.createCell(contents.length + 3);
				cell.setCellValue(krsMahasiswa.getSksYangDiambil());

				cell = row.createCell(contents.length + 4);
				cell.setCellValue(krsMahasiswa.getSksk());

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String ActualDate = Common.databaseDateFormat.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar());
				java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
				java.time.LocalDate currentdate = mahasiswa.getTanggalLulus() == null ? java.time.LocalDate.now()
						: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(mahasiswa.getTanggalLulus()));
				Period period = Period.between(dt, currentdate);
				System.out.println("Years " + period.getYears()); // Years 2
				System.out.println("Months " + period.getMonths()); // Months 1
				System.out.println("Days " + period.getDays()); // Days 11
				Jurusan jurusan = mahasiswa.getJurusan();
				int batasSemester = (jurusan != null && jurusan.getJenjang() != null
						&& jurusan.getJenjang().getJumlahSemesterMaksimal() != null
								? jurusan.getJenjang().getJumlahSemesterMaksimal()
								: 0);

				Calendar calendarMasaAwal = ais.ui.util.WaktuUtil.getCalendar();
				calendarMasaAwal.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());

				Calendar calendarMasaAkhir = ais.ui.util.WaktuUtil.getCalendar();
				calendarMasaAkhir.set(Calendar.DATE, calendarMasaAwal.get(Calendar.DATE) + (178 * batasSemester));

				ActualDate = Common.databaseDateFormat.get().format(calendarMasaAkhir.getTime());
				dt = java.time.LocalDate.parse(ActualDate, formatter);
				currentdate = java.time.LocalDate.now();

				row.createCell(contents.length + 5).setCellValue(period.getYears());

				row.createCell(contents.length + 6).setCellValue(period.getMonths());
				row.createCell(contents.length + 7).setCellValue(period.getDays());
				row.createCell(contents.length + 8).setCellValue(mahasiswa.ambilMasaStudi());
				tambahLinkFoto(row, contents.length + 9, hlink_style, mahasiswa);

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(PendaftaranWisuda.class, this,
				"Download Peserta", "/img/print.png", columnHeadersAdding, dataAdding, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
					return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
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
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														loadData(null);
													}
												});
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

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										ClassMetadata classMetadata = HibernateUtil
												.getClassMetadata(PendaftaranWisuda.class);
										Session session = HibernateUtil.currentNativeSession();

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												PendaftaranWisuda pendaftaranWisuda = id == null || id.equals(-1L)
														? null
														: (PendaftaranWisuda) session
																.createCriteria(PendaftaranWisuda.class)
																.add(Restrictions.idEq(id)).uniqueResult();

												if (pendaftaranWisuda == null) {
													pendaftaranWisuda = new PendaftaranWisuda();
												}

												Common.setObjectValues(classMetadata, pendaftaranWisuda, contents, 1,
														sheet, i);

												session.getTransaction().begin();
												session.saveOrUpdate(pendaftaranWisuda);
												session.getTransaction().commit();

												Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
												String judulskripsi = Common.getSheetContentAsString(sheet, 2, i);
												String noAkta = Common.getSheetContentAsString(sheet, 3, i);
												boolean ada = false;
												if (judulskripsi != null && !judulskripsi.trim().isEmpty()) {
													ada = true;
													mahasiswa.setJudulSkripsi(judulskripsi);
												}
												if (noAkta != null && !noAkta.trim().isEmpty()) {
													ada = true;
													mahasiswa.setNoAkta1(noAkta);
												}
												if (ada) {
													session.getTransaction().begin();
													session.update(mahasiswa);
													session.getTransaction().commit();
												}

												label.setValue("Upload data \"" + pendaftaranWisuda.getKode() + " - "
														+ pendaftaranWisuda.getNama() + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/DetailwisudaHelper.java:765");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		final MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan dengan status mahasiswa",
				"/img/excel.png");
		toolbar.appendChild(singkron);

		singkron.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final MyWindow window = new MyWindow("Pilih Fakultas dan Prodi", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("300px");
				window.setWidth("600px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				center.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);
				MyColumnConfig column = new MyColumnConfig();
				column.setWidth("30%");
				column.setParent(columns);
				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
				final Combobox fakultas;
				row.appendChild(fakultas = new Combobox());
				fakultas.setWidth("90%");
				fakultas.setReadonly(true);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
				final Combobox jurusan;
				row.appendChild(jurusan = new Combobox());
				jurusan.setWidth("90%");
				jurusan.setReadonly(true);

				Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

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
						window.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Singkronkan dengan status mahasiswa",
						"/img/save.gif");
				save.setTooltiptext("Proses");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);

								window.detach();
							}
						});

						new Thread(new Runnable() {

							@SuppressWarnings("unchecked")
							@Override
							public void run() {
								try {
								Fakultas f = (Fakultas) (fakultas.getSelectedItem() == null ? null
										: fakultas.getSelectedItem().getValue());
								Jurusan j = (Jurusan) (jurusan.getSelectedItem() == null ? null
										: jurusan.getSelectedItem().getValue());
								Session session = HibernateUtil.currentNativeSession();
								List<Mahasiswa> pendaftaranWisudas = ConstantValues
										.simpleList(
												initCriteria(true).add(Restrictions.eq("persetujuanWisuda", true))
														.add(j == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("mahasiswa.jurusan", j))
														.add(f == null ? Restrictions.sqlRestriction("true")
																: Restrictions.eq("jurusan.fakultas", f))
														.setProjection(Projections.property("mahasiswa.id")),
												Mahasiswa.class, false);
								StatusKeluar LULUS = (StatusKeluar) session.createCriteria(StatusKeluar.class)
										.add(Restrictions.ilike("nama", "Lulus", MatchMode.ANYWHERE)).setMaxResults(1)
										.uniqueResult();
								HibernateUtil.closeSession();
								int i = 1;
								for (Mahasiswa mahasiswa : pendaftaranWisudas) {
									label.setValue("Singkronkan data " + mahasiswa + " ("
											+ Common.numberFormat.get().format((i * 100.0 / pendaftaranWisudas.size()))
											+ "%)");

									if (mahasiswa != null) {

										Integer semesterLulus = Mahasiswa.hitungSmtLulus(LULUS, mahasiswa);
										mahasiswa.setSemesterLulus(semesterLulus);
										mahasiswa.setStatusKeluar(LULUS);
										try {
											if (mahasiswa.getTahunLulus() == null || mahasiswa.getTahunLulus() <= 0) {
												Calendar calendar = WaktuUtil.getCalendar();
												calendar.setTime(wisuda.getTanggal());
												mahasiswa.setTahunLulus(calendar.get(Calendar.YEAR));
											}
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailwisudaHelper.java:920");
										}

										try {
											if (mahasiswa.getTahunWisuda() == null || mahasiswa.getTahunWisuda() <= 0) {
												Calendar calendar = WaktuUtil.getCalendar();
												calendar.setTime(wisuda.getTanggal());
												mahasiswa.setTahunWisuda(calendar.get(Calendar.YEAR));
											}
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DetailwisudaHelper.java:930");
										}

										Judisium judisium = Common.hitungJudisium(mahasiswa, null);
										if (judisium != null && judisium.getId() != null) {
											mahasiswa.setPredikatKelulusan(judisium);
										}

										try {
											session = HibernateUtil.currentNativeSession();
											session.getTransaction().begin();
											Common.refreshUpdate(session, mahasiswa);
											session.getTransaction().commit();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailwisudaHelper.java:943");
											// TODO: handle exception
										}
										HibernateUtil.closeSession();

										try {
											session = HibernateUtil.currentNativeSession();
											HistoryStatusMahasiswa historyStatusMahasiswa = Common
													.currentStatus(mahasiswa);
											session.refresh(historyStatusMahasiswa);
											historyStatusMahasiswa.setStatusMahasiswa(ConstantValues.LULUS);
											session.getTransaction().begin();
											Common.refreshUpdate(session, historyStatusMahasiswa);
											session.getTransaction().commit();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailwisudaHelper.java:957");
											// TODO: handle exception
										}
										HibernateUtil.closeSession();
									}

									i++;
								}
								label.setValue("");
															} finally {
									ais.database.hibernate.HibernateUtil.closeSession();
								}
							}
						}).start();

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});

		columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("NIK");
		columnHeadersAdding.add("No.Ijazah");
		columnHeadersAdding.add("Judul Skripsi");
		columnHeadersAdding.add("IPK");
		columnHeadersAdding.add("Yudisium");
		columnHeadersAdding.add("No.Yudisium");
		columnHeadersAdding.add("Tanggal Yudisium");
		columnHeadersAdding.add("Tanggal Munaqasah");
		columnHeadersAdding.add("Nilai Skripsi");
		columnHeadersAdding.add("Nilai Huruf");
		columnHeadersAdding.add("Tahun Masuk");
		columnHeadersAdding.add("HP");
		columnHeadersAdding.add("Masa Studi");
		columnHeadersAdding.add("Ayah");
		columnHeadersAdding.add("Ibu");
		columnHeadersAdding.add("Email");
		columnHeadersAdding.add("Toga");
		columnHeadersAdding.add("Alamat");
		columnHeadersAdding.add("Link Foto");

		dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				PendaftaranWisuda pendaftaranWisuda = (PendaftaranWisuda) objects[0];
				Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];

				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa,
						mahasiswa.getSemesterLulus() == null ? mahasiswa.currentSemester()
								: mahasiswa.getSemesterLulus(),
						null, null);
				row.createCell(contentsBaru.length).setCellValue(biodataMahasiswa.getNoIdentitas());

				XSSFCell cell = row.createCell(contentsBaru.length + 1);
				cell.setCellValue(mahasiswa.getNoIjazah1());

				cell = row.createCell(contentsBaru.length + 2);
				cell.setCellValue(
						pendaftaranWisuda.getSkripsi() == null ? "" : pendaftaranWisuda.getSkripsi().getJudul());

				cell = row.createCell(contentsBaru.length + 3);
				cell.setCellValue(krsMahasiswa.getIpk());

				Judisium judisium = Common.hitungJudisium(mahasiswa, krsMahasiswa);

				cell = row.createCell(contentsBaru.length + 4);
				cell.setCellValue(judisium == null ? "" : judisium.getNama());

				cell = row.createCell(contentsBaru.length + 5);
				cell.setCellValue(mahasiswa.getNoAkta2());

				cell = row.createCell(contentsBaru.length + 6);
				cell.setCellValue(mahasiswa.getTanggalYudisium() == null ? ""
						: Common.dateFormat4.get().format(mahasiswa.getTanggalYudisium()));

				cell = row.createCell(contentsBaru.length + 7);
				cell.setCellValue(pendaftaranWisuda.getSkripsi() == null
						|| pendaftaranWisuda.getSkripsi().getTanggalSidang() == null ? ""
								: Common.dateFormat4.get().format(pendaftaranWisuda.getSkripsi().getTanggalSidang()));

				cell = row.createCell(contentsBaru.length + 8);
				cell.setCellValue(
						pendaftaranWisuda.getSkripsi() == null || pendaftaranWisuda.getSkripsi().getTotalNilai() == null
								? ""
								: Common.numberFormat.get().format(pendaftaranWisuda.getSkripsi().getTotalNilai()));

				cell = row.createCell(contentsBaru.length + 9);
				cell.setCellValue(
						pendaftaranWisuda.getSkripsi() == null ? "" : pendaftaranWisuda.getSkripsi().getNilaiHuruf());

				cell = row.createCell(contentsBaru.length + 10);
				cell.setCellValue(mahasiswa.getTahunangkatan());

				cell = row.createCell(contentsBaru.length + 11);
				cell.setCellValue(biodataMahasiswa.getHp());

				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String ActualDate = Common.databaseDateFormat.get().format(mahasiswa.getTanggalKegiatanBelajarMengajar());
				java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
				java.time.LocalDate currentdate = mahasiswa.getTanggalLulus() == null ? java.time.LocalDate.now()
						: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(mahasiswa.getTanggalLulus()));
				Period period = Period.between(dt, currentdate);
				System.out.println("Years " + period.getYears()); // Years 2
				System.out.println("Months " + period.getMonths()); // Months 1
				System.out.println("Days " + period.getDays()); // Days 11
				Jurusan jurusan = mahasiswa.getJurusan();
				int batasSemester = (jurusan != null && jurusan.getJenjang() != null
						&& jurusan.getJenjang().getJumlahSemesterMaksimal() != null
								? jurusan.getJenjang().getJumlahSemesterMaksimal()
								: 0);

				Calendar calendarMasaAwal = ais.ui.util.WaktuUtil.getCalendar();
				calendarMasaAwal.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());

				Calendar calendarMasaAkhir = ais.ui.util.WaktuUtil.getCalendar();
				calendarMasaAkhir.set(Calendar.DATE, calendarMasaAwal.get(Calendar.DATE) + (178 * batasSemester));

				ActualDate = Common.databaseDateFormat.get().format(calendarMasaAkhir.getTime());
				dt = java.time.LocalDate.parse(ActualDate, formatter);
				currentdate = java.time.LocalDate.now();

				row.createCell(contentsBaru.length + 12).setCellValue("Masa studi : " + period.getYears() + " tahun, "
						+ period.getMonths() + " bulan, " + period.getDays() + " hari. ");

				cell = row.createCell(contentsBaru.length + 13);
				cell.setCellValue(biodataMahasiswa.getNamaAyah());

				cell = row.createCell(contentsBaru.length + 14);
				cell.setCellValue(biodataMahasiswa.getNamaIbu());

				cell = row.createCell(contentsBaru.length + 15);
				cell.setCellValue(biodataMahasiswa.getEmail());

				cell = row.createCell(contentsBaru.length + 16);
				cell.setCellValue(pendaftaranWisuda.getUkuranToga() == null ? ""
						: pendaftaranWisuda.getUkuranToga().equals(1) ? "S"
								: pendaftaranWisuda.getUkuranToga().equals(2) ? "M"
										: pendaftaranWisuda.getUkuranToga().equals(3) ? "L" : "XL");

				String alamat = biodataMahasiswa.getAlamat();

				if (biodataMahasiswa.getDusun() != null && !biodataMahasiswa.getDusun().isEmpty()) {
					alamat += ", " + biodataMahasiswa.getDusun();
				}

				if (biodataMahasiswa.getKelurahan() != null && !biodataMahasiswa.getKelurahan().isEmpty()) {
					alamat += ", " + biodataMahasiswa.getKelurahan();
				}

				if (biodataMahasiswa.getKecamatan() != null) {
					alamat += ", " + biodataMahasiswa.getKecamatan().getNama();
				}

				if (biodataMahasiswa.getKota() != null) {
					alamat += ", " + biodataMahasiswa.getKota().getNama();
				}

				if (biodataMahasiswa.getPropinsi() != null) {
					alamat += ", " + biodataMahasiswa.getPropinsi().getNama();
				}

				cell = row.createCell(contentsBaru.length + 17);
				cell.setCellValue(alamat);

				tambahLinkFoto(row, contentsBaru.length + 18, hlink_style, mahasiswa);

			}
		};

		cetakToolbarbutton = Common.cetakDataCustomButton(PendaftaranWisuda.class, this, "Download Wisudawan",
				"/img/print.png", columnHeadersAdding, dataAdding, contentsBaru);
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No. Reg");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No. Kursi");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		// column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");
		// column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");
		// column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Judul " + Common.getKonfigurasi("label_skripsi", "skripsi").getNilai());
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		// borderlayout.setParent(component);

		groupbox.appendChild(paging = new Paging());
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		loadData(null);

	}

}
