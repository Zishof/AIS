package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanIjazahAkademik;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelompokStatusKeluarMahasiswa;
import ais.database.model.Mahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Baris detail ZK ({@code org.zkoss.zul.Detail}, lewat superclass {@link MyDetail}) yang dipasang
 * pada grid master {@link KelompokStatusKeluarMahasiswa} — pengelompokan mahasiswa yang sudah
 * "keluar" secara akademik (lulus/wisuda, drop-out, dsb.), dipakai sebagai wadah kelengkapan
 * administrasi kelulusan/keluar (nomor ijazah, akta, SK, tanggal wisuda/yudisium/SK Rektor, SKPI)
 * DAN sebagai titik cetak massal dokumen (ijazah, transkrip) untuk seluruh anggotanya sekaligus.
 * Sama seperti {@code KelompokMahasiswaDetailAction}/{@code KelompokStatusMahasiswaDetailAction},
 * keanggotaan disimpan langsung sebagai FK {@code kelompokStatusKeluarMahasiswa} pada baris
 * {@link Mahasiswa} (bukan tabel pivot terpisah) — satu mahasiswa hanya bisa berada di SATU
 * kelompok status keluar pada satu waktu. Saat baris kelompok pada grid induk di-expand
 * ({@code onOpen}, lihat konstruktor), kelas ini merender grid anggota dengan kolom data akademik
 * yang bisa diedit langsung inline per baris, plus toolbar cetak dokumen massal.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code KelompokStatusKeluarMahasiswa
 * kelompokStatusKeluarMahasiswa}, {@code MyGrid grid}, {@code Textbox pencarian}, {@code List mahasiswas};
 * inisialisasi/lifecycle ({@code initCriteria()}); pembacaan/pencarian ({@code loadData()}, {@code
 * uploadDataMahasiswa()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping konkret &amp; kekhasan kelas ini (paling kompleks dari 4 kelas
 * "Kelompok*DetailAction" sekeluarga):</b></p>
 * <ul>
 * <li>Baris grid punya SEMBILAN field akademik yang diedit inline (nomor ijazah 1/2, nomor akta
 * 1/2, tanggal wisuda, tanggal SK Rektor, tanggal yudisium, nomor SK DO, nomor SKPI) — namun
 * SEMUA field berbagi satu {@code EventListener onChange} yang sama: mengubah SATU field mana pun
 * memicu simpan ULANG kesembilan field sekaligus lewat {@code Common.refreshUpdate}, bukan simpan
 * per-field. Ini pola existing yang dipertahankan, bukan bug baru yang diperbaiki di sini.</li>
 * <li>Tombol toolbar "Ijazah" dan "Transkrip" men-generate dokumen PDF untuk SELURUH mahasiswa yang
 * sedang termuat di grid (field {@code mahasiswas}, bukan hanya baris yang dipilih), menggabungkan
 * hasilnya dengan Apache PDFBox {@code PDFMergerUtility} menjadi satu berkas PDF, lalu menampilkannya
 * lewat {@code Report.tampil} — proses berjalan di background {@link Thread} dengan progress bar
 * berbasis {@link Label}.</li>
 * <li>Tombol "Ambil Data Mahasiswa" memberi picker {@code AmbilDataMahasiswaBanyak} list
 * {@code mahasiswas} yang SUDAH ter-scope ke kelompok ini (hasil {@link #loadData(Object)}
 * sebelumnya, BUKAN query ulang seperti pada {@code KelompokMahasiswaDetailAction}) sebagai
 * baris pre-checked/terkunci di picker.</li>
 * <li>{@link #uploadDataMahasiswa} membaca 9 kolom tambahan (bukan hanya NIM) dari Excel dan
 * me-reload ulang entity {@code Mahasiswa} maupun {@code KelompokStatusKeluarMahasiswa} lewat
 * {@code session.get(...)} pada sesi thread ini sebelum menyimpan — lihat komentar inline soal
 * kenapa entity harus di-attach ulang ke sesi lokal thread, bukan dipakai langsung dari cache.</li>
 * </ul>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * <p><b>Cakupan akses data mahasiswa keluar (hasil penelusuran seluruh rantai).</b> Kelas ini
 * tidak memiliki penjagaan kepemilikan data apa pun, dan rantai di atasnya juga tidak:</p>
 * <ul>
 * <li>Entitas {@link KelompokStatusKeluarMahasiswa} tidak punya kolom fakultas, jurusan,
 * sekolah, maupun satuan kerja &mdash; kelompok memang dirancang global.</li>
 * <li>{@code KelompokStatusKeluarMahasiswaAction.initCriteria(boolean)} (grid induk) menyaring
 * hanya berdasarkan nama kelompok dan kata kunci NIM/nama, tanpa penyaring kepemilikan.</li>
 * <li>{@link #initCriteria(boolean)} di kelas ini menyaring hanya berdasarkan FK kelompok dan
 * status aktif mahasiswa.</li>
 * <li>Picker {@code AmbilDataMahasiswaBanyak} pada tombol "Ambil Data Mahasiswa" mencari ke
 * SELURUH {@link Mahasiswa} aktif di basis data; kolom prodi di sana adalah penyaring teks
 * bebas yang diketik pengguna, bukan pembatas cakupan.</li>
 * </ul>
 * <p>Satu-satunya gerbang adalah privilese menu tingkat aksi
 * ({@link ais.common.CommonPrivilages} CREATE/UPDATE/DELETE) yang dibaca di kelas induk.
 * Akibatnya siapa pun yang dapat membuka menu ini dapat: melihat seluruh mahasiswa keluar
 * lintas program studi, memasukkan mahasiswa aktif mana pun ke dalam kelompok, menyunting
 * langsung sembilan kolom dokumen kelulusan (termasuk NOMOR IJAZAH dan nomor SK), menimpanya
 * secara massal lewat unggah Excel ({@link #uploadDataMahasiswa}), serta mencetak berkas PDF
 * ijazah dan transkrip untuk seluruh anggota grid sekaligus. Perlu dicatat bahwa memasukkan
 * mahasiswa ke kelompok di sini juga MENGELUARKANNYA dari kelompok status keluar sebelumnya,
 * karena keanggotaan berupa FK tunggal pada baris {@link Mahasiswa}.</p>
 * <p>Uraian ini adalah dokumentasi keadaan terkini, bukan perubahan perilaku; tidak ada
 * gerbang yang ditambahkan atau dihapus oleh commit dokumentasi ini.</p>
 *
 * @see MyDetail
 */
public class KelompokStatusKeluarMahasiswaDetailAction extends MyDetail implements DataCriteria {

	/**
	 * Versi serialisasi kelas. Wajib ada karena {@link MyDetail} (turunan komponen ZK) bersifat
	 * {@link java.io.Serializable}: ZK dapat menserialisasi pohon komponen saat sesi dipindahkan
	 * antar node atau dituliskan ke penyimpanan sesi. Nilainya tidak boleh diubah selama bentuk
	 * serial kelas ini masih kompatibel.
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	/**
	 * Kelompok status keluar yang anggotanya ditampilkan pada baris detail ini. Ditetapkan sekali
	 * lewat konstruktor dan menjadi SATU-SATUNYA pembatas pada {@link #initCriteria(boolean)}.
	 *
	 * <p>Entitas {@link KelompokStatusKeluarMahasiswa} sendiri tidak memiliki kolom
	 * fakultas/jurusan/satuan kerja, sehingga kelompok bersifat global: keanggotaannya dapat
	 * mencakup mahasiswa dari program studi mana pun. Keanggotaan disimpan sebagai FK
	 * {@code kelompokStatusKeluarMahasiswa} pada baris {@link Mahasiswa} (bukan tabel pivot),
	 * sehingga satu mahasiswa hanya dapat berada di satu kelompok status keluar pada satu waktu
	 * dan memasukkannya ke kelompok baru otomatis mengeluarkannya dari kelompok sebelumnya.</p>
	 */
	private KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa;
	/**
	 * Grid anggota kelompok (satu baris per {@link Mahasiswa}), dibuat di {@link #display()} dan
	 * diisi ulang oleh {@link #loadData(Object)} dengan {@link MahasiswaRenderer}. Memakai mold
	 * {@code paging} berukuran 50 baris di sisi klien; seluruh data (maksimum 1500 baris) tetap
	 * dimuat sekaligus ke memori, bukan per halaman basis data.
	 */
	private MyGrid grid;

	/**
	 * Kotak kata kunci pada toolbar; isinya dicocokkan {@code ilike ANYWHERE} ke nama ATAU NIM
	 * mahasiswa di {@link #initCriteria(boolean)}. Dibuat di {@link #display()}, sehingga
	 * {@link #initCriteria(boolean)} tidak boleh dipanggil sebelum {@link #display()} berjalan
	 * (pembacaan {@code getValue()} pada field yang masih {@code null} akan melempar
	 * {@link NullPointerException}).
	 *
	 * <p>Penyaring ini mempersempit tampilan saja, bukan penjagaan akses: dikosongkan berarti
	 * seluruh anggota kelompok ditampilkan.</p>
	 */
	private Textbox pencarian;

	/**
	 * Membuat detail row untuk satu {@code kelompokStatusKeluarMahasiswa} tertentu.
	 *
	 * <p>Menyimpan referensi entity induk dan mendaftarkan listener {@code onOpen} yang
	 * membersihkan anak komponen lalu memanggil {@link #display()} — grid anggota (beserta
	 * kolom kelengkapan dokumen kelulusan/keluar) baru dibangun saat detail benar-benar terbuka
	 * ({@code isOpen()}), bukan saat konstruktor dipanggil.</p>
	 *
	 * @param kelompokStatusKeluarMahasiswa entity kelompok status keluar mahasiswa induk yang
	 *                                       anggotanya ditampilkan
	 */
	public KelompokStatusKeluarMahasiswaDetailAction(KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa) {
		super();
		this.kelompokStatusKeluarMahasiswa = kelompokStatusKeluarMahasiswa;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KelompokStatusKeluarMahasiswaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer baris grid anggota kelompok untuk {@link KelompokStatusKeluarMahasiswaDetailAction}.
	 * Setiap baris mewakili satu {@link Mahasiswa} anggota kelompok status keluar ini: foto, NIM,
	 * tautan riwayat revisi Envers, angkatan, jurusan, semester lulus, DAN sembilan textbox/datebox
	 * kelengkapan dokumen kelulusan/keluar yang bisa diedit inline (nomor ijazah 1/2, nomor akta
	 * 1/2, tanggal wisuda, tanggal SK Rektor, tanggal yudisium, nomor SK DO, nomor SKPI), plus tombol
	 * cetak transkrip per-mahasiswa dan tombol hapus.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KelompokStatusKeluarMahasiswaDetailAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p><b>Efek samping:</b> kesembilan field dokumen berbagi SATU {@code EventListener onChange}
	 * yang sama — mengubah nilai satu field mana pun memicu simpan ULANG kesembilan field sekaligus
	 * ({@code Common.refreshUpdate}), bukan simpan per-field individual (perilaku existing,
	 * dipertahankan apa adanya). Tombol hapus baris meng-null-kan FK
	 * {@code kelompokStatusKeluarMahasiswa} (bukan menghapus entity {@code Mahasiswa}).</p>
	 *
	 * @see KelompokStatusKeluarMahasiswaDetailAction
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Konstruktor tanpa argumen. Renderer tidak menyimpan state sendiri &mdash; seluruh data yang
		 * dibutuhkan diambil dari instance {@link KelompokStatusKeluarMahasiswaDetailAction} induk
		 * (kelas dalam non-statis) dan dari objek {@link Mahasiswa} yang dioper per baris.
		 */
		public MahasiswaRenderer() {

		}

		/**
		 * Merender satu baris grid untuk {@code mahasiswa}: foto, NIM, tautan riwayat revisi,
		 * angkatan, jurusan, semester lulus, sembilan field dokumen kelulusan/keluar yang bisa
		 * diedit inline (auto-save gabungan on-change), tombol cetak transkrip, dan tombol hapus
		 * (melepas keanggotaan kelompok).
		 *
		 * @param arg0 baris grid ZK tujuan render
		 * @param data instance {@link Mahasiswa} untuk baris ini
		 */
		@Override
		public void render(final Row arg0, Object data) throws Exception {
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) data;

			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);
			new Label(mahasiswa.getNim()).setParent(arg0);

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNama()).setParent(arg0);

			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);

			new Label(mahasiswa.getSemesterLulus() + "").setParent(arg0);

			final MyTextbox noIjazah1 = new MyTextbox(mahasiswa.getNoIjazah1());
			noIjazah1.setWidth("95%");

			final MyTextbox noIjazah2 = new MyTextbox(mahasiswa.getNoIjazah2());
			noIjazah2.setWidth("95%");

			final MyTextbox noAkta1 = new MyTextbox(mahasiswa.getNoAkta1());
			noAkta1.setWidth("95%");

			final MyTextbox noAkta2 = new MyTextbox(mahasiswa.getNoAkta2());
			noAkta2.setWidth("95%");

			final MyDatebox tanggalWisuda = new MyDatebox(mahasiswa.getTanggalWisuda());
			tanggalWisuda.setWidth("95%");

			final MyDatebox tanggalSkRektor = new MyDatebox(mahasiswa.getTanggalSkRektor());
			tanggalSkRektor.setWidth("95%");

			final MyDatebox tanggalYudisium = new MyDatebox(mahasiswa.getTanggalYudisium());
			tanggalYudisium.setWidth("95%");

			final MyTextbox skDo = new MyTextbox(mahasiswa.getSkDo());
			skDo.setWidth("95%");

			final MyTextbox nomorSkpi = new MyTextbox(mahasiswa.getNomorSkpi());
			nomorSkpi.setWidth("95%");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					mahasiswa.setNoIjazah1(noIjazah1.getValue().trim());
					mahasiswa.setNoIjazah2(noIjazah2.getValue().trim());
					mahasiswa.setNoAkta1(noAkta1.getValue().trim());
					mahasiswa.setNoAkta2(noAkta2.getValue().trim());
					mahasiswa.setTanggalWisuda(tanggalWisuda.getValue());
					mahasiswa.setTanggalSkRektor(tanggalSkRektor.getValue());
					mahasiswa.setTanggalYudisium(tanggalYudisium.getValue());
					mahasiswa.setSkDo(skDo.getValue().trim());
					mahasiswa.setNomorSkpi(nomorSkpi.getValue().trim());
					Common.refreshUpdate(mahasiswa);
				}
			};

			noIjazah1.addEventListener("onChange", eventListener);
			noIjazah2.addEventListener("onChange", eventListener);
			noAkta1.addEventListener("onChange", eventListener);
			noAkta2.addEventListener("onChange", eventListener);

			tanggalWisuda.addEventListener("onChange", eventListener);
			tanggalYudisium.addEventListener("onChange", eventListener);

			tanggalSkRektor.addEventListener("onChange", eventListener);
			tanggalYudisium.addEventListener("onChange", eventListener);
			skDo.addEventListener("onChange", eventListener);
			nomorSkpi.addEventListener("onChange", eventListener);

			noIjazah1.setParent(arg0);
			noIjazah2.setParent(arg0);
			noAkta1.setParent(arg0);
			noAkta2.setParent(arg0);

			tanggalWisuda.setParent(arg0);
			tanggalYudisium.setParent(arg0);

			tanggalSkRektor.setParent(arg0);

			tanggalYudisium.setParent(arg0);

			skDo.setParent(arg0);

			nomorSkpi.setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Transkrip", "/img/svg/printer.svg");
			button.setVisible(mahasiswa.getKelompokStatusKeluarMahasiswa() != null);
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanTranskipAkademik laporanTranskipAkademik = new LaporanTranskipAkademik(mahasiswa);
					laporanTranskipAkademik.setTitle("Transkrip");
					laporanTranskipAkademik.setClosable(true);
					laporanTranskipAkademik.setBorder("none");
					laporanTranskipAkademik.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					laporanTranskipAkademik.setHeight("95%");
					laporanTranskipAkademik.setWidth("90%");
					laporanTranskipAkademik.onModal();

				}
			});
			button.setParent(hbox);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setVisible(mahasiswa.getKelompokStatusKeluarMahasiswa() != null);
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
											mahasiswa.setKelompokStatusKeluarMahasiswa(null);
											Common.refreshSaveOrUpdate(mahasiswa);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
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
			button.setParent(hbox);
		}
	}

	/**
	 * Daftar anggota kelompok yang sedang termuat di grid (hasil {@link #loadData(Object)} paling
	 * akhir). Selain dipakai grid, list ini JUGA dipakai langsung sebagai sumber "sudah
	 * dipilih/terkunci" bagi picker {@code AmbilDataMahasiswaBanyak} pada tombol "Ambil Data
	 * Mahasiswa", dan sebagai sumber data proses cetak massal ijazah/transkrip di toolbar
	 * {@link #display()}.
	 */
	private List<Mahasiswa> mahasiswas = null;

	/**
	 * Memuat ulang (maks. 1500 baris) daftar {@link Mahasiswa} yang menjadi anggota
	 * {@code kelompokStatusKeluarMahasiswa} ini, sesuai filter {@link #initCriteria(boolean)}
	 * (mahasiswa aktif, cocok kata kunci {@code pencarian} bila diisi), menyimpannya ke field
	 * {@link #mahasiswas}, dan menampilkannya ke grid.
	 *
	 * <p>Batas {@code setMaxResults(1500)} bersifat diam: bila kelompok memiliki lebih dari 1500
	 * anggota, kelebihannya hilang dari grid TANPA peringatan apa pun kepada pengguna. Karena
	 * field {@link #mahasiswas} yang sama dipakai sebagai sumber cetak massal ijazah/transkrip dan
	 * sebagai daftar "sudah dipilih" bagi picker {@code AmbilDataMahasiswaBanyak}, pemotongan itu
	 * ikut memengaruhi ketiga fitur tersebut &mdash; berbeda dari tombol "Hapus Semua" yang
	 * menjalankan querinya sendiri dengan batas 5000.</p>
	 *
	 * <p>Seluruh baris dimuat sekaligus ke memori; paging pada {@link #grid} hanya bekerja di sisi
	 * klien atas daftar yang sudah termuat itu.</p>
	 *
	 * @param value tidak dipakai — signature mengikuti kontrak umum handler event grid AIS
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		mahasiswas = ConstantValues.simpleList(initCriteria(true).setMaxResults(1500), Mahasiswa.class);

		ListModel strset = new SimpleListModel(mahasiswas);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun seluruh UI panel detail: caption "Daftar mahasiswa yang masuk kelompok &lt;nama&gt;",
	 * toolbar (tombol "Ambil Data Mahasiswa" assign massal, textbox pencarian, refresh, tombol
	 * "Ijazah" &amp; "Transkrip" untuk cetak PDF massal seluruh anggota grid saat ini — gabung lewat
	 * PDFBox {@code PDFMergerUtility} di background thread, tombol cetak/export data lewat
	 * {@code Common.cetakData}, tombol upload Excel kustom lewat {@link #uploadDataMahasiswa}, tombol
	 * "Hapus Semua"), definisi 15 kolom grid (identitas + 9 kolom dokumen kelulusan/keluar), lalu
	 * memanggil {@link #loadData(Object)} untuk memuat baris pertama kali. Dipanggil sekali per
	 * pembukaan detail (lihat listener {@code onOpen} di konstruktor).
	 *
	 * <p><b>Cetak massal ijazah &amp; transkrip.</b> Kedua tombol bekerja atas field
	 * {@link #mahasiswas} apa adanya &mdash; yakni seluruh anggota yang termuat di grid, BUKAN
	 * baris yang dipilih pengguna &mdash; dan tidak meminta konfirmasi lebih dulu. Tiap mahasiswa
	 * menghasilkan satu berkas PDF yang lalu digabung dengan {@code PDFMergerUtility}; kegagalan
	 * per mahasiswa hanya dicatat lalu dilewati, sehingga berkas gabungan dapat kekurangan halaman
	 * tanpa penanda yang terlihat pengguna. Tombol Transkrip lebih dulu menanyakan salah satu dari
	 * enam varian laporan. Keduanya berjalan di {@link Thread} terpisah dengan indikator kemajuan
	 * berbasis {@link Label}, dan penggabungan akhir dijalankan dari listener
	 * {@code Common.displayLoadBar}.</p>
	 *
	 * <p><b>Assign massal.</b> Tombol "Ambil Data Mahasiswa" membuka picker
	 * {@code AmbilDataMahasiswaBanyak} dengan {@link #mahasiswas} sebagai daftar yang sudah
	 * terpilih. Mahasiswa yang dipilih langsung di-set FK kelompoknya dan disimpan satu per satu
	 * tanpa konfirmasi; karena keanggotaan berupa FK tunggal pada {@link Mahasiswa}, mahasiswa
	 * yang sebelumnya berada di kelompok status keluar lain otomatis berpindah ke kelompok ini.</p>
	 *
	 * <p><b>Hapus Semua.</b> Tidak menghapus entitas {@link Mahasiswa}, melainkan meng-null-kan FK
	 * {@code kelompokStatusKeluarMahasiswa} untuk seluruh hasil {@link #initCriteria(boolean)}
	 * dengan batas 5000 baris &mdash; batas yang BERBEDA dari 1500 pada {@link #loadData(Object)},
	 * sehingga aksi ini dapat mengeluarkan mahasiswa yang tidak pernah tampil di grid. Sembilan
	 * kolom dokumen kelulusan yang sudah terisi TIDAK ikut dikosongkan.</p>
	 *
	 * <p><b>Susunan kolom.</b> Grid mendefinisikan 16 kolom dan {@link MahasiswaRenderer}
	 * menempelkan tepat 16 komponen, sehingga jumlahnya cocok. Perlu diperhatikan dua hal:</p>
	 * <ul>
	 * <li>Label kolom ke-9 dan ke-10 berbunyi "No.Transkrip" dan "No.SK", padahal yang dirender
	 * di posisi itu adalah {@code noAkta1} dan {@code noAkta2} &mdash; penamaan layar sengaja
	 * berbeda dari nama field entitas.</li>
	 * <li>Kolom "Tgl.SK" dan "Tgl.Yudisium" hanya sejajar berkat pemanggilan
	 * {@code tanggalYudisium.setParent(arg0)} yang tampak ganda di {@link MahasiswaRenderer}.
	 * Pemanggilan kedua BUKAN redundansi: di ZK, {@code setParent} pada komponen yang sudah
	 * berinduk MEMINDAHKANNYA ke urutan terakhir, sehingga {@code tanggalSkRektor} naik ke posisi
	 * "Tgl.SK" dan {@code tanggalYudisium} turun ke posisi "Tgl.Yudisium". Menghapusnya sebagai
	 * "baris kembar" akan menukar isi kedua kolom tersebut.</li>
	 * </ul>
	 *
	 * <p><b>Kecocokan ekspor&ndash;impor.</b> Daftar kolom ekspor {@code contents} berisi 13 kolom
	 * dengan indeks 0=nim, 1=nama, 2=jurusan.nama, 3=tahunangkatan, lalu 4..12 = kesembilan kolom
	 * dokumen kelulusan. Susunan ini sengaja PERSIS sama dengan yang dibaca
	 * {@link #uploadDataMahasiswa} (kolom 0 sebagai NIM dan kolom 4&ndash;12 sebagai kesembilan
	 * field dokumen), sehingga berkas hasil ekspor dapat langsung disunting lalu diunggah kembali.
	 * Mengubah isi atau urutan {@code contents} tanpa mengubah pembacaan indeks di
	 * {@link #uploadDataMahasiswa} akan mematahkan alur bolak-balik ini. Perhatikan bahwa kolom
	 * "Foto" pada grid tidak punya padanan di {@code contents}, jadi susunan kolom berkas ekspor
	 * memang berbeda satu posisi dari susunan kolom grid.</p>
	 */
	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang masuk kelompok " + kelompokStatusKeluarMahasiswa.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Mahasiswa", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataMahasiswaBanyak window = new AmbilDataMahasiswaBanyak(mahasiswas);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(final Event dataCalonMhs) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Mahasiswa> mahasiswas = (List<Mahasiswa>) dataCalonMhs.getData();

								if (mahasiswas != null) {
									Session session = HibernateUtil.currentSession();
									for (Mahasiswa mahasiswa : mahasiswas) {
										mahasiswa.setKelompokStatusKeluarMahasiswa(kelompokStatusKeluarMahasiswa);
										Common.refreshUpdate(session, mahasiswa);
									}

									loadData(null);
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

		button = new MyToolbarbuttonConfig("Ijazah", "/img/svg/file-pdf.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final PDFMergerUtility ut = new PDFMergerUtility();
				final File filePdfBaru = new File(Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ut.setDestinationStream(new FileOutputStream(filePdfBaru));
						ut.mergeDocuments();

						Report.tampil(filePdfBaru);
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("rawtypes")
					@Override
					public void run() {
						int index = 0;
						int size = mahasiswas.size();
						for (Mahasiswa mahasiswa : mahasiswas) {
							index++;
							label.setValue("Memperoses data " + mahasiswa.getNama() + " ("
									+ Common.numberFormat.get().format(((index * 1.0 )/ (size * 1.0 )) * 100.0) + "%)");
							try {
								Map parameters = LaporanIjazahAkademik.parameterIjazah(mahasiswa, null, null, false);
								File file = Report.generateFileReport(Report.PDF, parameters, "Ijazah",
										ais.ui.util.WaktuUtil.getDate(), new Toolbar());
								ut.addSource(file);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KelompokStatusKeluarMahasiswaDetailAction.java:382");
							}

						}

						label.setValue("");
					}
				}).start();

			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Transkrip", "/img/svg/journal-bookmark.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow window = new MyWindow("Pilih Jenis Transkrip", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("250px");
				window.setWidth("500px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center a = new Center();
				a.setParent(borderlayout);

				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(a);
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

				final Combobox jenis = new Combobox();
				Comboitem comboitem = new MyComboitemConfig("Transkrip Akademik");
				comboitem.setValue("Transkrip_Akademik");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("Transkrip 2 Kolom");
				comboitem.setValue("Rekaman_Nilai_2_Kolom");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("Transkrip 2 Halaman");
				comboitem.setValue("report1");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("Transkrip 4 Kolom");
				comboitem.setValue("Report6");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("Transkrip IPK");
				comboitem.setValue("Rekaman_Nilai");
				jenis.appendChild(comboitem);

				comboitem = new MyComboitemConfig("IPK berdasar Kelompok");
				comboitem.setValue("Rekaman_Nilai_Kelompok");
				jenis.appendChild(comboitem);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Jenis *"));
				row.appendChild(jenis);

				jenis.setWidth("95%");
				jenis.setSelectedIndex(0);
				jenis.setReadonly(true);

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
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Cetak Transkrip",
						"/img/svg/journal-bookmark.svg");
				save.setTooltiptext("Lanjut Cetak Transkrip");
				save.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final String namaFile = (String) jenis.getSelectedItem().getValue();
						final PDFMergerUtility ut = new PDFMergerUtility();
						final File filePdfBaru = new File(
								Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
						final Label label = Common.displayLoadBar(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ut.setDestinationStream(new FileOutputStream(filePdfBaru));
								ut.mergeDocuments();

								Report.tampil(filePdfBaru);
							}
						});

						new Thread(new Runnable() {

							@SuppressWarnings("rawtypes")
							@Override
							public void run() {
								int index = 0;
								int size = mahasiswas.size();
								for (Mahasiswa mahasiswa : mahasiswas) {
									index++;
									label.setValue("Memperoses data " + mahasiswa.getNama() + " ("
											+ Common.numberFormat.get().format(((index * 1.0 )/ (size * 1.0 )) * 100.0) + "%)");
									try {
										Map parameters = LaporanTranskipAkademik.generateParameter(mahasiswa,
												mahasiswa.currentSemester(), false, false, WaktuUtil.getDate(),
												WaktuUtil.getDate());
										File file = Report.generateFileReport(Report.PDF, parameters, namaFile,
												ais.ui.util.WaktuUtil.getDate(), new Toolbar());
										ut.addSource(file);
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KelompokStatusKeluarMahasiswaDetailAction.java:522");
									}

								}

								label.setValue("");
							}
						}).start();

					}
				});
				save.setParent(toolbar);

				window.onModal();

			}
		});
		button.setParent(toolbar);

		String[] contents = new String[] { "nim", "nama", "jurusan.nama", "tahunangkatan", "noIjazah1", "noIjazah2",
				"noAkta1", "noAkta2", "tanggalWisuda", "tanggalSkRektor", "tanggalYudisium", "skDo", "nomorSkpi" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Mahasiswa.class, this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload " + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
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
							uploadDataMahasiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							});
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

		button = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										List<Mahasiswa> mahasiswas = ConstantValues
												.simpleList(initCriteria(true).setMaxResults(5000), Mahasiswa.class);
										for (Mahasiswa mahasiswa : mahasiswas) {
											mahasiswa.setKelompokStatusKeluarMahasiswa(null);
											Common.refreshSaveOrUpdate(mahasiswa);
										}
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(null);
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
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("9%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt Lulus");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Ijazah I");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Ijazah II");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.Transkrip");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.SK");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl.Wisuda");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl.SK");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl.Yudisium");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.DO");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No.SKPI");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
	}

	/**
	 * Implementasi kontrak {@link DataCriteria}: membangun {@link Criteria} Hibernate atas
	 * {@link Mahasiswa} yang (1) aktif ({@code aktif} null atau {@code true}), (2) bila textbox
	 * {@code pencarian} diisi, namanya atau NIM-nya cocok {@code ilike} di mana saja
	 * ({@link MatchMode#ANYWHERE}), dan (3) FK {@code kelompokStatusKeluarMahasiswa}-nya sama dengan
	 * entity induk ini, diurutkan menurun berdasarkan id. Dipakai bersama oleh
	 * {@link #loadData(Object)}, tombol "Hapus Semua", dan tombol cetak/export toolbar
	 * ({@code Common.cetakData(Mahasiswa.class, this, contents)}).
	 *
	 * @param order parameter kontrak {@link DataCriteria}; TIDAK dipakai pada override ini — urutan
	 *              selalu menurun berdasarkan id terlepas dari nilai parameter ini
	 * @return criteria Hibernate siap dieksekusi ({@code .list()}/{@code .setMaxResults(...)})
	 */
	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("nama", pencarian.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nim", pencarian.getValue().trim(), MatchMode.ANYWHERE)))

				.addOrder(Order.desc("id"))
				.add(Restrictions.eq("kelompokStatusKeluarMahasiswa", kelompokStatusKeluarMahasiswa));
	}

	/**
	 * Meng-update kelengkapan dokumen kelulusan/keluar SEMBILAN kolom (nomor ijazah 1/2, nomor akta
	 * 1/2, tanggal wisuda, tanggal SK Rektor, tanggal yudisium, nomor SK DO, nomor SKPI) sekaligus
	 * meng-assign {@code kelompokStatusKeluarMahasiswa} ini berdasarkan berkas Excel (.xlsx) hasil
	 * upload, dijalankan di background {@link Thread} agar UI tidak terkunci.
	 *
	 * <p>Kolom 0 dibaca sebagai NIM/NPM, kolom 4-12 sebagai kesembilan field dokumen (lihat urutan
	 * di atas). Mahasiswa dicari lewat {@code Common.getSheetContentAsObject} (match otomatis by
	 * NIM ke entity), fallback {@code ConstantValues.ambilByNim} bila kosong. SEBELUM disimpan,
	 * baik entity {@code Mahasiswa} maupun {@code kelompokStatusKeluarMahasiswa} di-reload ulang
	 * lewat {@code session.get(...)} pada sesi milik thread ini — WAJIB, karena entity yang didapat
	 * dari cache/session lain berstatus detached terhadap sesi background thread ini sehingga update
	 * tidak akan ter-flush bila dipakai langsung (lihat komentar inline). Bila salah satu tidak
	 * ditemukan di database, baris dilewati dan dicatat ({@code laporan.catatDilewati}).</p>
	 *
	 * <p>Simpan memakai transaksi manual per baris dengan rollback eksplisit saat gagal — tanpa ini
	 * transaksi tetap aktif dan {@code begin()} pada baris berikutnya melempar "Transaction already
	 * active", membuat SEMUA baris sesudahnya ikut gagal tanpa jejak sementara notifikasi tetap
	 * berbunyi berhasil (lihat komentar inline). Sesi Hibernate dibuka manual lewat
	 * {@code HibernateUtil.openSession()} (bukan {@code currentNativeSession()}) karena
	 * {@code Common.getSheetContentAsObject}/{@code getSheetContentAsString} yang dipanggil di dalam
	 * loop menutup sesi native ThreadLocal, sehingga sesi hasil {@code currentNativeSession()} sudah
	 * tertutup pada saat {@code getTransaction().begin()} dipanggil. Kegagalan per baris dicatat
	 * lebih rinci lewat {@code laporan.catatGagalDetail} (rantai cause + titik kode masuk ke seksi
	 * "CATATAN TEKNIS TAMBAHAN" laporan) dibanding varian {@code catatGagal} pada
	 * {@code KelompokMahasiswaDetailAction}/{@code KelompokStatusMahasiswaDetailAction}.</p>
	 *
	 * <p>Progres ditampilkan lewat {@link Label} yang di-poll {@link Timer} setiap 200ms; hasil akhir
	 * dirangkum {@code ais.common.LaporanUpload} dan diserahkan ke {@code eventListener} saat selesai.
	 * Sesi thread ditutup di blok {@code finally} lewat
	 * {@code HibernateUtil.closeSessionQuietly(session)} DAN {@code HibernateUtil.closeSession()}
	 * berurutan — pembersihan ganda ini disengaja untuk menutup baik sesi eksplisit yang dibuka di
	 * atas maupun sisa ThreadLocal helper Excel.</p>
	 *
	 * @param file          berkas .xlsx sementara hasil upload (sudah divalidasi ekstensinya oleh
	 *                      pemanggil di {@link #display()})
	 * @param eventListener dipanggil ({@code laporan.selesaikan(eventListener)}) setelah seluruh
	 *                      baris diproses dan laporan siap ditampilkan/diunduh
	 * @throws Exception diteruskan dari inisialisasi awal (parsing workbook terjadi di thread
	 *                    terpisah dan errornya ditangani/dicatat di sana, bukan dilempar ke sini)
	 */
	public void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		/*
		 * LAPORAN UPLOAD. Sebelumnya baris yang NIM-nya tidak dikenali dilewati DIAM-DIAM
		 * (blok "if (mahasiswa != null ...)" tanpa else) sementara kotak "berhasil dilakukan"
		 * tetap tampil tanpa syarat -- itulah sebabnya pengguna melihat notifikasi berhasil
		 * padahal tidak satu pun baris tersimpan. Kini hasil TIAP baris dicatat, lalu di akhir
		 * proses jumlah berhasil/gagal/dilewati ditampilkan dan rincian per baris otomatis diunduh.
		 */
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Data Mahasiswa Status Keluar");
		laporan.setNamaBerkasSumber(file.getName());

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
					Clients.clearBusy();
					timer.detach();
					laporan.selesaikan(eventListener);
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				Session session = null;
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					/*
					 * WAJIB openSession(), BUKAN currentNativeSession(). Common.getSheetContentAsObject()
					 * dan getSheetContentAsString() di dalam loop menutup native session ThreadLocal
					 * (HibernateUtil.closeSession()), sehingga session hasil currentNativeSession()
					 * sudah TERTUTUP saat getTransaction().begin() dipanggil -> "Session is closed!"
					 * di SETIAP baris -> seluruh baris tercatat gagal.
					 */
					session = HibernateUtil.openSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						String nimBaris = "";
						try {

							nimBaris = Common.getSheetContentAsString(sheet, 0, i);

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
									Mahasiswa.class);
							if (mahasiswa == null) {
								mahasiswa = ConstantValues.ambilByNim(nimBaris);
							}

							String noIjazah1 = Common.getSheetContentAsString(sheet, 4, i);
							String noIjazah2 = Common.getSheetContentAsString(sheet, 5, i);
							String noAkta1 = Common.getSheetContentAsString(sheet, 6, i);
							String noAkta2 = Common.getSheetContentAsString(sheet, 7, i);
							Date tanggalWisuda = Common.getSheetContentAsDate(sheet, 8, i);
							Date tanggalSkRektor = Common.getSheetContentAsDate(sheet, 9, i);
							Date tanggalYudisium = Common.getSheetContentAsDate(sheet, 10, i);
							String skDo = Common.getSheetContentAsString(sheet, 11, i);
							String nomorSkpi = Common.getSheetContentAsString(sheet, 12, i);

							if (mahasiswa != null && mahasiswa.getId() != null) {

								// Reload ke session khusus thread ini agar entitas managed (bukan
								// detached dari cache/session lain) -> update pasti ter-flush.
								Mahasiswa mahasiswaSafe = (Mahasiswa) session.get(Mahasiswa.class, mahasiswa.getId());
								KelompokStatusKeluarMahasiswa kelompokSafe = (KelompokStatusKeluarMahasiswa) session
										.get(KelompokStatusKeluarMahasiswa.class, kelompokStatusKeluarMahasiswa.getId());
								if (mahasiswaSafe == null || kelompokSafe == null) {
									laporan.catatDilewati(i, nimBaris, "Data mahasiswa/kelompok tidak ditemukan di database");
									continue;
								}

								mahasiswaSafe.setNoAkta1(noAkta1);
								mahasiswaSafe.setNoAkta2(noAkta2);
								mahasiswaSafe.setNoIjazah1(noIjazah1);
								mahasiswaSafe.setNoIjazah2(noIjazah2);
								mahasiswaSafe.setSkDo(skDo);
								mahasiswaSafe.setNomorSkpi(nomorSkpi);
								mahasiswaSafe.setTanggalWisuda(tanggalWisuda);
								mahasiswaSafe.setTanggalSkRektor(tanggalSkRektor);
								mahasiswaSafe.setTanggalYudisium(tanggalYudisium);
								mahasiswaSafe.setKelompokStatusKeluarMahasiswa(kelompokSafe);

								session.getTransaction().begin();
								try {
									Common.refreshUpdate(session, mahasiswaSafe);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									/*
									 * WAJIB rollback. Tanpa ini transaksi tetap AKTIF, sehingga begin() pada
									 * baris berikutnya melempar "Transaction already active" -- satu baris
									 * bermasalah membuat SELURUH baris sesudahnya ikut gagal tanpa jejak,
									 * sementara notifikasi tetap berbunyi berhasil.
									 */
									try {
										session.getTransaction().rollback();
									} catch (Exception eRoll) {
										ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload "
											+ "src/ais/action/master/helper/KelompokStatusKeluarMahasiswaDetailAction.java");
									}
									throw eSimpan;
								}

								laporan.catatBerhasil(i, mahasiswaSafe.getNim(), mahasiswaSafe.getNama());

								label.setValue("Upload data \"" + mahasiswaSafe.getNim() + " - " + mahasiswaSafe.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else if (nimBaris == null || nimBaris.trim().isEmpty()) {
								laporan.catatDilewati(i, "", "Kolom NIM/NPM kosong");
							} else {
								laporan.catatDilewati(i, nimBaris,
									"NIM/NPM tidak ditemukan pada data mahasiswa -- periksa penulisannya, "
										+ "atau mahasiswa memang belum terdaftar");
							}

						} catch (Exception e) {
							// catatGagalDetail: selain baris ringkas (+penyebab akar & saran solusi),
							// rincian teknis lengkap (rantai cause + titik kode) masuk ke seksi
							// CATATAN TEKNIS TAMBAHAN di akhir berkas laporan.
							laporan.catatGagalDetail(i, nimBaris, e);
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/KelompokStatusKeluarMahasiswaDetailAction.java:836");
				} finally {
					// Tutup session khusus thread ini + bersihkan ThreadLocal sisa helper Excel.
					HibernateUtil.closeSessionQuietly(session);
					HibernateUtil.closeSession();
				}

				label.setValue("");
			}
		}).start();
	}
}
