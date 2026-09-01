package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

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
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataMahasiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KelompokStatusMahasiswa;
import ais.database.model.Mahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Baris detail ZK ({@code org.zkoss.zul.Detail}, lewat superclass {@link MyDetail}) yang dipasang
 * pada grid master {@link KelompokStatusMahasiswa} — pengelompokan mahasiswa berdasarkan status
 * (mis. kategori status keaktifan/administratif tertentu di luar status keluar formal yang punya
 * kelas sendiri, {@code KelompokStatusKeluarMahasiswa}). Sama seperti
 * {@code KelompokMahasiswaDetailAction}, keanggotaan disimpan langsung sebagai FK
 * {@code kelompokStatusMahasiswa} pada baris {@link Mahasiswa} (bukan tabel pivot terpisah) — satu
 * mahasiswa hanya bisa berada di SATU {@code KelompokStatusMahasiswa} pada satu waktu (assign baru
 * menimpa assign lama). Saat baris kelompok pada grid induk di-expand ({@code onOpen}, lihat
 * konstruktor), kelas ini merender grid anggota berisi seluruh {@code Mahasiswa} yang FK-nya
 * menunjuk ke entity induk ini. Strukturnya SAMA PERSIS dengan
 * {@code KelompokMahasiswaDetailAction} (nama field, urutan method, kolom grid identik) —
 * satu-satunya perbedaan substansial adalah entity {@code KelompokStatusMahasiswa} vs
 * {@code KelompokMahasiswa} dan sedikit variasi di {@link #uploadDataMahasiswa} (di sini memakai
 * {@code HibernateUtil.currentNativeSession()} alih-alih {@code openSession()} eksplisit — lihat
 * catatan pada method tersebut).
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code KelompokStatusMahasiswa
 * kelompokStatusMahasiswa}, {@code MyGrid grid}, {@code Textbox pencarian}; inisialisasi/lifecycle ({@code
 * initCriteria()}); pembacaan/pencarian ({@code loadData()}, {@code uploadDataMahasiswa()}); operasi domain lain
 * ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping konkret:</b> tombol "Ambil Data Mahasiswa" membuka picker massal
 * {@code AmbilDataMahasiswaBanyak}, diberi daftar anggota kelompok ini SAAT INI (hasil
 * {@code initCriteria(false).list()}) agar picker menandai baris tersebut sebagai sudah
 * terpilih/terkunci; memilih baris baru langsung meng-assign {@code kelompokStatusMahasiswa} pada
 * mahasiswa terpilih dan menyimpannya lewat {@code Common.refreshUpdate}. Upload Excel
 * ({@link #uploadDataMahasiswa}) berjalan di background thread dengan transaksi manual per baris +
 * rollback-on-failure. Tombol "Hapus Semua" melepas ({@code null}-kan) field
 * {@code kelompokStatusMahasiswa} pada hingga 5000 mahasiswa yang cocok filter aktif saat ini —
 * bukan hapus baris {@code Mahasiswa}. Pemanggil baru sebaiknya menggunakan method yang sudah ada
 * atau service bersama, bukan membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class KelompokStatusMahasiswaDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private KelompokStatusMahasiswa kelompokStatusMahasiswa;
	private MyGrid grid;

	private Textbox pencarian;

	/**
	 * Membuat detail row untuk satu {@code kelompokStatusMahasiswa} tertentu.
	 *
	 * <p>Menyimpan referensi entity induk dan mendaftarkan listener {@code onOpen} yang
	 * membersihkan anak komponen lalu memanggil {@link #display()} — grid anggota kelompok baru
	 * dibangun saat detail benar-benar terbuka ({@code isOpen()}), bukan saat konstruktor
	 * dipanggil.</p>
	 *
	 * @param kelompokStatusMahasiswa entity kelompok status mahasiswa induk yang anggotanya
	 *                                 ditampilkan
	 */
	public KelompokStatusMahasiswaDetailAction(KelompokStatusMahasiswa kelompokStatusMahasiswa) {
		super();
		this.kelompokStatusMahasiswa = kelompokStatusMahasiswa;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KelompokStatusMahasiswaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer baris grid anggota kelompok untuk {@link KelompokStatusMahasiswaDetailAction}. Setiap
	 * baris mewakili satu {@link Mahasiswa} yang FK {@code kelompokStatusMahasiswa}-nya menunjuk ke
	 * entity induk: foto kecil, NIM, tautan riwayat revisi Envers ({@code
	 * RevisiHelper.createNewRevisi}), tahun angkatan, nama jurusan/prodi, dan tombol hapus (hanya
	 * tampil bila mahasiswa memang masih anggota kelompok ini).
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KelompokStatusMahasiswaDetailAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p><b>Efek samping:</b> tombol hapus baris tidak menghapus entity {@code Mahasiswa}, melainkan
	 * meng-null-kan FK {@code kelompokStatusMahasiswa} lalu menyimpannya
	 * ({@code Common.refreshSaveOrUpdate}) — efeknya baris tersebut hilang dari grid ini pada
	 * refresh berikutnya.</p>
	 *
	 * @see KelompokStatusMahasiswaDetailAction
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public MahasiswaRenderer() {

		}

		/**
		 * Merender satu baris grid untuk {@code mahasiswa}: foto, NIM, tautan riwayat revisi, tahun
		 * angkatan, nama jurusan, dan tombol hapus (melepas keanggotaan kelompok, bukan menghapus
		 * data mahasiswa).
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

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setVisible(mahasiswa.getKelompokStatusMahasiswa() != null);
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
											mahasiswa.setKelompokStatusMahasiswa(null);
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
			button.setParent(arg0);
		}
	}

	/**
	 * Memuat ulang (maks. 500 baris) daftar {@link Mahasiswa} yang menjadi anggota
	 * {@code kelompokStatusMahasiswa} ini, sesuai filter {@link #initCriteria(boolean)}
	 * (mahasiswa aktif, cocok kata kunci {@code pencarian} bila diisi), lalu menampilkannya ke grid.
	 *
	 * @param value tidak dipakai — signature mengikuti kontrak umum handler event grid AIS
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<Mahasiswa> mahasiswas = ConstantValues.simpleList(initCriteria(true).setMaxResults(500), Mahasiswa.class);

		ListModel strset = new SimpleListModel(mahasiswas);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun seluruh UI panel detail: caption "Daftar mahasiswa yang masuk kelompok &lt;nama&gt;",
	 * toolbar (tombol "Ambil Data Mahasiswa" untuk assign massal lewat
	 * {@code AmbilDataMahasiswaBanyak}, textbox pencarian nama/NIM, tombol refresh, tombol
	 * cetak/export lewat {@code Common.cetakData}, tombol upload Excel kustom lewat
	 * {@link #uploadDataMahasiswa}, tombol "Hapus Semua"), definisi kolom grid, lalu memanggil
	 * {@link #loadData(Object)} untuk memuat baris pertama kali. Dipanggil sekali per pembukaan
	 * detail (lihat listener {@code onOpen} di konstruktor).
	 */
	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang masuk kelompok " + kelompokStatusMahasiswa.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Mahasiswa", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Mahasiswa> mahasiswas = initCriteria(false).list();

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
										mahasiswa.setKelompokStatusMahasiswa(kelompokStatusMahasiswa);
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

		String[] contents = new String[] { "nim", "nama", "jurusan.nama", "tahunangkatan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
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
											mahasiswa.setKelompokStatusMahasiswa(null);
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
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Prodi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	/**
	 * Implementasi kontrak {@link DataCriteria}: membangun {@link Criteria} Hibernate atas
	 * {@link Mahasiswa} yang (1) aktif ({@code aktif} null atau {@code true}), (2) bila textbox
	 * {@code pencarian} diisi, namanya atau NIM-nya cocok {@code ilike} di mana saja
	 * ({@link MatchMode#ANYWHERE}), dan (3) FK {@code kelompokStatusMahasiswa}-nya sama dengan
	 * entity induk ini, diurutkan menurun berdasarkan id. Dipakai bersama oleh
	 * {@link #loadData(Object)}, tombol "Ambil Data Mahasiswa", "Hapus Semua", dan tombol
	 * cetak/export toolbar ({@code Common.cetakData(this, contents)}).
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

				.addOrder(Order.desc("id")).add(Restrictions.eq("kelompokStatusMahasiswa", kelompokStatusMahasiswa));
	}

	/**
	 * Meng-assign {@code kelompokStatusMahasiswa} ini ke sekumpulan {@link Mahasiswa} berdasarkan
	 * berkas Excel (.xlsx) hasil upload, dijalankan di background {@link Thread} agar UI tidak
	 * terkunci.
	 *
	 * <p>Kolom 0 tiap baris (mulai baris ke-2, indeks 1) dibaca sebagai NIM/NPM. Mahasiswa dicari
	 * lewat {@code Common.getSheetContentAsObject} (match otomatis by NIM ke entity), fallback
	 * {@code ConstantValues.ambilByNim} bila kosong. Bila ditemukan, FK
	 * {@code kelompokStatusMahasiswa} di-set dan disimpan dalam transaksi manual per baris;
	 * kegagalan satu baris di-rollback secara eksplisit agar TIDAK menyisakan transaksi aktif yang
	 * akan membuat {@code begin()} pada baris berikutnya gagal dengan "Transaction already active"
	 * (lihat komentar inline) — tanpa rollback ini, satu baris NIM tidak valid akan menggagalkan
	 * SEMUA baris sesudahnya secara diam-diam.</p>
	 *
	 * <p><b>Kuirk dicatat apa adanya (bukan diperbaiki di sini):</b> berbeda dari
	 * {@code KelompokMahasiswaDetailAction}/{@code KelompokStatusKeluarMahasiswaDetailAction} yang
	 * memakai {@code HibernateUtil.openSession()} eksplisit di background thread, method ini memakai
	 * {@code HibernateUtil.currentNativeSession()}; struktur try/finally-nya juga berlapis dengan
	 * dua pemanggilan {@code HibernateUtil.closeSession()} (satu di akhir blok try, satu lagi di
	 * blok {@code finally} terluar) — redundan namun tidak berbahaya karena {@code closeSession()}
	 * aman dipanggil berulang pada ThreadLocal yang sudah kosong.</p>
	 *
	 * <p>Progres ditampilkan lewat {@link Label} yang di-poll oleh {@link Timer} setiap 200ms;
	 * hasil akhir (berhasil/gagal/dilewati per baris) dirangkum lewat {@code ais.common.LaporanUpload}
	 * dan diserahkan ke {@code eventListener} saat proses selesai (label kosong menandakan thread
	 * sudah beres).</p>
	 *
	 * @param file          berkas .xlsx sementara hasil upload (sudah divalidasi ekstensinya oleh
	 *                      pemanggil di {@link #display()})
	 * @param eventListener dipanggil ({@code laporan.selesaikan(eventListener)}) setelah seluruh
	 *                      baris diproses dan laporan siap ditampilkan/diunduh
	 * @throws Exception diteruskan dari inisialisasi awal (parsing workbook terjadi di thread
	 *                    terpisah dan errornya ditangani/dicatat di sana, bukan dilempar ke sini)
	 */
	public void uploadDataMahasiswa(final File file, final EventListener eventListener) throws Exception {

		// Laporan hasil per baris. Menggantikan Label "peringatan" yang disiapkan untuk
		// menampung keterangan baris bermasalah tetapi TIDAK PERNAH diisi, sehingga baris
		// yang tak cocok hilang tanpa kabar sementara notifikasi tetap berbunyi berhasil.
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Mahasiswa Kelompok Status");
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
				try {

				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					Session session = HibernateUtil.currentNativeSession();

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

							if (mahasiswa != null && mahasiswa.getId() != null) {

								mahasiswa.setKelompokStatusMahasiswa(kelompokStatusMahasiswa);

								session.getTransaction().begin();
								try {
									Common.refreshUpdate(session, mahasiswa);
									session.getTransaction().commit();
								} catch (Exception eSimpan) {
									// WAJIB rollback: tanpa ini transaksi tetap AKTIF sehingga begin() pada baris
									// berikutnya melempar "Transaction already active" -- satu baris bermasalah
									// membuat SELURUH baris sesudahnya ikut gagal tanpa jejak.
									try {
										session.getTransaction().rollback();
									} catch (Exception eRoll) {
										ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload");
									}
									throw eSimpan;
								}

								laporan.catatBerhasil(i, mahasiswa.getNim(), mahasiswa.getNama());

								label.setValue("Upload data \"" + mahasiswa.getNim() + " - " + mahasiswa.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else if (nimBaris == null || nimBaris.trim().isEmpty()) {
								laporan.catatDilewati(i, "", "Kolom NIM/NPM kosong");
							} else {
								laporan.catatDilewati(i, nimBaris,
										"NIM/NPM tidak ditemukan pada data mahasiswa -- periksa penulisannya, "
											+ "atau mahasiswa memang belum terdaftar");
							}

						} catch (Exception e) {
							laporan.catatGagal(i, nimBaris, e);
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/KelompokStatusMahasiswaDetailAction.java:452");
				}

				HibernateUtil.closeSession();

				label.setValue("");
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}
}
