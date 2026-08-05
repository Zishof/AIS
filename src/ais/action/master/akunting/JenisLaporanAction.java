package ais.action.master.akunting;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.akunting.JenisLaporanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.akunting.JenisLaporan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisLaporanAction — Controller Master Jenis Laporan Akuntansi</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK GenericAutowireComposer yang mengelola halaman master
 * Jenis Laporan pada modul akuntansi. Jenis laporan adalah referensi yang
 * menentukan template laporan keuangan yang dapat dicetak atau ditampilkan
 * di dashboard, seperti laporan neraca, laba-rugi, arus kas, dan sejenisnya.
 * Setiap jenis laporan dapat dikaitkan dengan hingga tiga file template JRXML
 * (JasperReports): laporan saldo, laporan perbandingan 2 bulanan, dan laporan
 * perbandingan 2 tahunan. Pengguna dengan hak akses yang sesuai dapat
 * menambah, mengubah, dan menghapus data jenis laporan melalui antarmuka ini.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Siklus hidup halaman mengikuti pola standar ZK GenericAutowireComposer:
 * <ol>
 *   <li>{@code doBeforeCompose} — pemeriksaan keamanan awal sebelum komponen ZUL
 *       dibuat; menggunakan {@code Common.doCheckSecurity()}.</li>
 *   <li>{@code doAfterCompose} — inisialisasi komponen UI setelah autowiring selesai:
 *       memverifikasi sesi pengguna, memanggil {@code JenisLaporan.reloadDefault()}
 *       untuk menyegarkan cache statis, mengatur visibilitas tombol tambah sesuai
 *       hak akses, dan memanggil {@code onSearchDefault} untuk menampilkan daftar
 *       jenis laporan awal.</li>
 *   <li>Inner class {@code JenisLaporanRenderer} — merender setiap baris grid
 *       dengan nama, keterangan, checkbox "Tampilkan Di Dashboard", dan tombol
 *       Ubah/Hapus inline.</li>
 *   <li>{@code onAdd} — menampilkan form tambah data baru.</li>
 *   <li>Metode {@code init(JenisLaporan)} (private) — membangun form edit/tambah
 *       secara programatik dengan komponen ZK (Borderlayout, MyGrid, Textbox)
 *       dan menyertakan widget upload/download file JRXML untuk tiga slot template.</li>
 *   <li>{@code onSave} — memvalidasi input, memeriksa duplikasi nama, menyimpan
 *       data jenis laporan melalui DAO, dan memperbarui referensi lampiran file
 *       menggunakan streaming session Hibernate.</li>
 *   <li>{@code onSearchDefault} — menjalankan query Hibernate untuk mengambil
 *       semua jenis laporan (diurutkan nama, dengan filter pencarian opsional).</li>
 *   <li>{@code checkNamaJenisLaporan} — memeriksa apakah nama yang dimasukkan
 *       sudah ada di database (untuk mencegah duplikasi).</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Semua operasi berjalan di thread ZK event-dispatcher. Penyimpanan lampiran
 * file JRXML menggunakan {@code StreamingHibernateUtil} (session terpisah dari
 * session utama Hibernate), yang ditutup eksplisit setelah operasi selesai.
 * Tidak ada thread latar yang dikelola oleh kelas ini.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * - Koneksi ke streaming session (untuk lampiran) harus selalu ditutup di blok
 *   finally atau catch; pastikan {@code StreamingHibernateUtil.getInstance().closeSession()}
 *   selalu dipanggil.<br>
 * - Jika ada jenis template baru (misalnya lampiran ke-4), tambahkan field
 *   {@code lampiran3}, baris form baru di {@code init()}, dan blok penyimpanan
 *   tambahan di {@code onSave()}.<br>
 * - Konstanta tipe file ({@code LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI})
 *   harus unik per slot template agar tidak terjadi konflik referensi.</p>
 */
public class JenisLaporanAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk kompatibilitas serialisasi ZK session.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;

	private Textbox searchnama;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisLaporan jenislaporan;
	private MyToolbarbuttonConfig add;
	/** Referensi lampiran file JRXML untuk template laporan saldo. */
	protected LampiranLain lampiran;
	/** Referensi lampiran file JRXML untuk template laporan perbandingan 2 bulanan. */
	protected LampiranLain lampiran1;
	/** Referensi lampiran file JRXML untuk template laporan perbandingan 2 tahunan. */
	protected LampiranLain lampiran2;

	/**
	 * <b>Tujuan:</b> Melakukan pemeriksaan keamanan sebelum halaman ZUL dikompilasi.
	 * Dipanggil oleh framework ZK sebagai langkah pertama dalam siklus hidup komposisi
	 * halaman, sebelum komponen apapun diinstansiasi atau di-autowire.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi bahwa pengguna
	 * saat ini terautentikasi dan memiliki hak akses ke halaman ini. Jika tidak,
	 * pengguna akan diarahkan ke halaman logoff sebelum halaman selesai dimuat.
	 * Setelah pemeriksaan, memanggil implementasi induk untuk melanjutkan proses
	 * komposisi halaman secara normal.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super dikembalikan ke framework ZK. Jika pemeriksaan keamanan
	 * gagal, framework akan menghentikan proses komposisi.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan {@code Common.doCheckSecurity()}. Logika tambahan
	 * yang memerlukan komponen ZUL harus diletakkan di {@link #doAfterCompose(Component)}.</p>
	 *
	 * @param page     halaman ZK yang sedang diproses
	 * @param parent   komponen induk dari composer ini
	 * @param compInfo metadata komponen dari definisi ZUL
	 * @return {@code ComponentInfo} dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi seluruh komponen UI dan logika bisnis halaman
	 * master jenis laporan setelah framework ZK selesai meng-autowire komponen ZUL.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Urutan inisialisasi:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Memverifikasi sesi pengguna; jika tidak valid atau tidak memiliki hak READ,
	 *       mengarahkan ke logoff dan keluar dari metode.</li>
	 *   <li>Memanggil {@code JenisLaporan.reloadDefault()} untuk memastikan cache
	 *       statis jenis laporan di memori diperbarui.</li>
	 *   <li>Mengatur visibilitas tombol "Tambah" berdasarkan hak akses CREATE
	 *       (null-safe karena tombol bersifat opsional di ZUL).</li>
	 *   <li>Menetapkan flag {@code edit} dan {@code delete} berdasarkan hak akses
	 *       UPDATE dan DELETE masing-masing.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk langsung menampilkan
	 *       daftar jenis laporan saat halaman pertama dimuat.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, metode keluar lebih awal setelah memanggil
	 * {@code Common.goLogoff()}. Exception dari super diteruskan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Berbeda dengan action lain, halaman ini tidak menggunakan paging karena
	 * jumlah jenis laporan biasanya tidak terlalu banyak. Jika ke depan jumlahnya
	 * berkembang signifikan, tambahkan paging di sini dan di {@code onSearchDefault}.</p>
	 *
	 * @param comp komponen root ZUL yang telah di-autowire
	 * @throws Exception jika terjadi error saat inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		JenisLaporan.reloadDefault();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
	}

	/**
	 * <h3>JenisLaporanRenderer — Renderer Baris Grid Jenis Laporan</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class ini bertanggung jawab merender setiap baris pada grid master
	 * jenis laporan. Menampilkan nama, keterangan, checkbox untuk mengontrol
	 * apakah jenis laporan ditampilkan di dashboard, serta tombol Ubah dan Hapus
	 * yang masing-masing memanggil form edit atau konfirmasi penghapusan.</p>
	 *
	 * <p><b>Cara kerja render:</b><br>
	 * Untuk setiap objek {@link JenisLaporan}:
	 * <ol>
	 *   <li>Menampilkan nama menggunakan widget {@code RevisiHelper.createNewRevisi}
	 *       yang menyertakan tautan ke riwayat perubahan data.</li>
	 *   <li>Menampilkan keterangan sebagai Label.</li>
	 *   <li>Menampilkan checkbox "Tampilkan Di Dashboard" yang langsung menyimpan
	 *       perubahan ke database saat diubah pengguna, tanpa perlu menekan tombol
	 *       simpan terpisah.</li>
	 *   <li>Menampilkan tombol "Ubah" yang membuka form edit ({@code init + onModal}).</li>
	 *   <li>Menampilkan tombol "Hapus" yang menampilkan dialog konfirmasi sebelum
	 *       menghapus data; menangkap exception constraint violation untuk memberikan
	 *       pesan error yang informatif kepada pengguna.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Threading:</b><br>
	 * Semua listener event berjalan di thread ZK event-dispatcher. Operasi
	 * penyimpanan checkbox menggunakan {@code Common.refreshSaveOrUpdate} yang
	 * menggunakan session Hibernate terkelola.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Urutan penambahan komponen ke {@code arg0} harus sesuai dengan urutan
	 * {@code <column>} di file ZUL. Jika ada kolom baru, tambahkan di keduanya.</p>
	 */
	class JenisLaporanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris grid dengan data dari objek
		 * {@link JenisLaporan} yang diberikan, termasuk kontrol interaktif
		 * checkbox dashboard dan tombol aksi Ubah/Hapus.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Metode ini diinvoke oleh framework ZK untuk setiap item dalam model daftar.
		 * Komponen-komponen ZK dibuat secara programatik dan ditambahkan ke baris
		 * ({@code arg0}) sesuai urutan kolom. Checkbox "Tampilkan Di Dashboard"
		 * dinonaktifkan jika pengguna tidak memiliki hak UPDATE. Tombol hapus
		 * menampilkan dialog konfirmasi {@code MyMessageboxConfig} dan menangkap
		 * exception jika data berelasi dengan data lain yang mencegah penghapusan.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception saat penghapusan ditangkap dan ditampilkan sebagai pesan error
		 * kepada pengguna. Selain itu, error juga ditampilkan ke admin melalui
		 * {@code Common.tampilErrorJikaAdmin}.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika kolom baru ditambahkan di ZUL, tambahkan juga komponen baru di
		 * metode ini pada posisi yang sesuai. Pastikan pemeriksaan null dilakukan
		 * pada properti yang bisa bernilai null.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen UI
		 * @param arg1 objek data, dicast ke {@link JenisLaporan}
		 * @throws Exception jika terjadi error saat membuat atau memasang komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisLaporan jenislaporan = (JenisLaporan) arg1;

			RevisiHelper.createNewRevisi(JenisLaporan.class, jenislaporan, jenislaporan.getNama()).setParent(arg0);
			new Label(jenislaporan.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Tampilkan Di Dashboard");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenislaporan.getTampilDiDashboard());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenislaporan.setTampilDiDashboard(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenislaporan);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jenislaporan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(jenislaporan);

											onSearchDefault(event);
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
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}

	}

	/**
	 * <b>Tujuan:</b> Menangani aksi pengguna ketika menekan tombol "Tambah"
	 * pada toolbar halaman, dengan membuka form untuk menambahkan jenis laporan baru.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@link JenisLaporan} baru (kosong) dan memanggil metode
	 * {@code init(JenisLaporan)} untuk membangun form secara programatik. Setelah
	 * form siap, jendela modal ({@code addWindow}) ditampilkan kepada pengguna.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke framework ZK. Pada kondisi normal, exception tidak
	 * seharusnya terjadi di metode ini karena hanya melibatkan pembuatan objek
	 * dan pembangunan komponen UI.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika diperlukan nilai default tertentu untuk jenis laporan baru (misalnya
	 * mengisi nama dengan prefix tertentu), lakukan sebelum memanggil {@code init()}.</p>
	 *
	 * @param event event ZK yang dipicu ketika tombol "Tambah" diklik
	 * @throws Exception jika terjadi error saat membangun atau menampilkan form
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisLaporan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun dan menginisialisasi form tambah/ubah jenis laporan
	 * secara programatik dalam jendela modal {@code addWindow}, mencakup field
	 * teks dan tiga widget upload file template JRXML.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode ini:
	 * <ol>
	 *   <li>Menyimpan referensi objek {@link JenisLaporan} yang akan diedit/dibuat
	 *       ke field instance {@code jenislaporan}.</li>
	 *   <li>Mengatur judul jendela sesuai mode (Tambah/Ubah).</li>
	 *   <li>Membersihkan konten lama dari {@code addWindow} menggunakan
	 *       {@code Common.clear()}.</li>
	 *   <li>Membangun struktur layout menggunakan {@code Borderlayout}:
	 *       Center berisi grid form, South berisi toolbar Batal/Simpan.</li>
	 *   <li>Menambahkan field Nama (Textbox), Keterangan (Textbox multiline),
	 *       dan tiga baris upload/download file JRXML (laporan saldo, perbandingan
	 *       2 bulanan, perbandingan 2 tahunan) menggunakan
	 *       {@code LampiranLain.createDownloadUploadFileLain}.</li>
	 *   <li>Setiap callback upload menyimpan referensi {@code LampiranLain}
	 *       ke field {@code lampiran}, {@code lampiran1}, dan {@code lampiran2}
	 *       yang akan dipakai oleh {@code onSave}.</li>
	 *   <li>Tombol Batal menyembunyikan jendela tanpa menyimpan.</li>
	 *   <li>Tombol Simpan memanggil {@code onSave}; jika berhasil, menyegarkan
	 *       grid dan menutup jendela.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit di metode ini. Exception saat
	 * membangun komponen akan diteruskan ke atas.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Konstanta suffix {@code "_1"} dan {@code "_2"} untuk lampiran1 dan lampiran2
	 * digunakan untuk membedakan tipe file di database. Jangan ubah nilai ini
	 * tanpa memigrasikan data yang sudah ada.</p>
	 *
	 * @param jenislaporan objek {@link JenisLaporan} yang akan diedit (jika ID tidak
	 *                     null berarti mode ubah) atau objek baru (ID null berarti mode tambah)
	 */
	private void init(JenisLaporan jenislaporan) {
		this.jenislaporan = jenislaporan;
		addWindow.setTitle(jenislaporan.getId() == null ? "Tambah Jenis Laporan" : "Ubah Jenis Laporan");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Laporan"));
		row.appendChild(nama = new Textbox(jenislaporan.getNama() == null ? "" : jenislaporan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jenislaporan.getKeterangan() == null ? "" : jenislaporan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Saldo (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenislaporan.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI, "File Laporan Saldo jrxml", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		lampiran1 = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Perbandingan 2 Bulanan (jrxml atau jasper)"));
		hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenislaporan.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI + "_1",
				"File Laporan Perbandingan 2 Bulanan jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran1 = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		lampiran2 = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan Perbandingan 2 Tahunan (jrxml atau jasper)"));
		hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenislaporan.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI + "_2",
				"File Laporan Perbandingan 2 Tahunan jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran2 = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * <b>Tujuan:</b> Memvalidasi input pengguna dan menyimpan data jenis laporan
	 * (beserta referensi file lampiran JRXML) ke database.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Proses penyimpanan terdiri dari beberapa tahap:
	 * <ol>
	 *   <li><b>Validasi nama:</b> Nama harus tidak kosong; jika kosong, menampilkan
	 *       pesan peringatan dan mengembalikan {@code false}.</li>
	 *   <li><b>Pemeriksaan duplikasi:</b> Memanggil {@link #checkNamaJenisLaporan()}
	 *       untuk memastikan nama belum ada di database (kecuali untuk data yang sama
	 *       saat mode ubah).</li>
	 *   <li><b>Penyimpanan entitas utama:</b> Jika mode ubah, memuat ulang entitas
	 *       dari database via DAO untuk memastikan data segar, lalu mengupdate nama
	 *       dan keterangan. Menggunakan {@code JenisLaporanDao.save} atau {@code update}
	 *       sesuai kondisi.</li>
	 *   <li><b>Penyimpanan lampiran:</b> Menggunakan streaming session Hibernate
	 *       (terpisah dari session utama) untuk memperbarui referensi {@code ref}
	 *       (ID jenis laporan yang baru tersimpan) pada setiap lampiran JRXML.
	 *       Ini penting karena lampiran disimpan menggunakan SessionFactory streaming
	 *       yang berbeda.</li>
	 *   <li>Mengembalikan {@code true} jika berhasil sehingga pemanggil tahu
	 *       bahwa form dapat ditutup.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Blok try-catch menangkap exception dari operasi streaming session dan
	 * memanggil {@code rollbackTransaction} serta {@code tampilErrorJikaAdmin}.
	 * Jika exception terjadi, data entitas utama sudah tersimpan tetapi
	 * referensi lampiran mungkin belum terupdate — ini tidak menyebabkan
	 * inkonsistensi fatal karena referensi dapat diperbaiki dengan membuka ulang
	 * form dan menyimpan kembali.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pastikan {@code StreamingHibernateUtil.getInstance().closeSession()} selalu
	 * dipanggil setelah operasi streaming, bahkan saat ada error. Jika ada
	 * slot lampiran baru (lampiran3 dst.), tambahkan blok serupa untuk
	 * menyimpannya. Perhatikan bahwa null-check pada {@code lampiran.getId()}
	 * mencegah pemrosesan lampiran yang belum terupload.</p>
	 *
	 * @param event event ZK yang memicu simpan (biasanya klik tombol Simpan)
	 * @return {@code true} jika data berhasil disimpan dan form boleh ditutup,
	 *         {@code false} jika validasi gagal atau data duplikat
	 * @throws Exception jika terjadi error yang tidak tertangani saat penyimpanan
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Laporan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama Jenis Laporan dengan nama yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkNamaJenisLaporan();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Laporan sudah ada di database. Langkah yang dapat dilakukan: (1) Gunakan nama yang berbeda dan belum terdaftar; (2) Periksa daftar jenis laporan yang sudah ada melalui menu pencarian; (3) ulangi proses simpan dengan nama baru. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", 1,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		JenisLaporanDao jenislaporanDao = DaoFactory.getInstance().getJenisLaporanDao();
		if (jenislaporan.getId() != null) {
			jenislaporan = jenislaporanDao.load(jenislaporan.getId());

		}

		jenislaporan.setNama(nama.getValue());
		jenislaporan.setKeterangan(keterangan.getValue());

		if (jenislaporan.getId() != null) {
			jenislaporanDao.update(jenislaporan);
		} else {
			jenislaporanDao.save(jenislaporan);
		}

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenislaporan.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			if (lampiran1 != null && lampiran1.getId() != null) {
				session.refresh(lampiran1);
				lampiran1.setRef(jenislaporan.getId());

				session.getTransaction().begin();
				session.update(lampiran1);
				session.getTransaction().commit();
			}

			if (lampiran2 != null && lampiran2.getId() != null) {
				session.refresh(lampiran2);
				lampiran2.setRef(jenislaporan.getId());

				session.getTransaction().begin();
				session.update(lampiran2);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian data jenis laporan berdasarkan filter
	 * nama yang aktif dan memperbarui tampilan grid dengan hasilnya.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat Hibernate Criteria langsung pada {@link JenisLaporan} dengan:
	 * <ul>
	 *   <li>Pengurutan ascending berdasarkan nama agar daftar mudah dibaca.</li>
	 *   <li>Filter ILIKE pada field {@code nama} jika kotak pencarian tidak kosong.</li>
	 *   <li>Pembatasan jumlah hasil menggunakan {@code Common.MAX_RESULT} untuk
	 *       mencegah query yang mengembalikan terlalu banyak data.</li>
	 * </ul>
	 * Hasil query dibungkus dalam {@code SimpleListModel} dan di-set ke grid
	 * bersama dengan renderer baru {@code JenisLaporanRenderer}.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke framework ZK. Perubahan model grid
	 * bersifat aman karena ZK menangani update UI secara atomik per event.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Tidak menggunakan paging; jika data tumbuh besar, tambahkan paging dengan
	 * pola yang sama seperti action-action lain di modul ini. Saat ini
	 * {@code MAX_RESULT} berfungsi sebagai batas aman.</p>
	 *
	 * @param event event ZK pemicu pencarian (bisa null jika dipanggil programatik)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<JenisLaporan> jenislaporan = session.createCriteria(JenisLaporan.class).addOrder(Order.asc("nama"))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(jenislaporan);
		grid.setRowRenderer(new JenisLaporanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Memeriksa apakah nama jenis laporan yang dimasukkan pengguna
	 * sudah ada di database, untuk mencegah penyimpanan data duplikat.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan query COUNT pada tabel {@link JenisLaporan} dengan kondisi:
	 * <ul>
	 *   <li>Nama sama persis (case-sensitive, menggunakan {@code Restrictions.eq})
	 *       dengan nilai yang dimasukkan pengguna (setelah trim).</li>
	 *   <li>Pada mode ubah (ID tidak null): mengecualikan record dengan ID yang sama
	 *       menggunakan {@code Restrictions.ne("id", this.jenislaporan.getId())}
	 *       agar nama yang tidak berubah tidak dianggap duplikat.</li>
	 *   <li>Pada mode tambah (ID null): tidak ada pengecualian, sehingga semua
	 *       record yang cocok dianggap duplikat.</li>
	 * </ul>
	 * Mengembalikan {@code true} jika ada duplikat (count > 0), {@code false} jika aman.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika query gagal (misalnya koneksi database terputus), exception akan
	 * diteruskan ke pemanggil. Null pada hasil uniqueResult tidak mungkin terjadi
	 * karena Projections.rowCount selalu mengembalikan angka.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Perhatikan bahwa pemeriksaan ini bersifat case-sensitive di level Java
	 * (menggunakan {@code eq}). Jika database menggunakan collation case-insensitive,
	 * pertimbangkan menggunakan {@code eqProperty} atau fungsi SQL LOWER untuk
	 * konsistensi.</p>
	 *
	 * @return {@code true} jika nama sudah ada di database (duplikat),
	 *         {@code false} jika nama belum digunakan dan aman untuk disimpan
	 */
	public Boolean checkNamaJenisLaporan() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisLaporan.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenislaporan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenislaporan.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
