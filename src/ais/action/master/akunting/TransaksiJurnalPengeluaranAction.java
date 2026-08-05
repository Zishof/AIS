package ais.action.master.akunting;

import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataAkunKreditBanbox;
import ais.action.master.akunting.helper.TransaksiJurnalPengeluaranHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>TransaksiJurnalPengeluaranAction — Controller Jurnal Kas Keluar</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK GenericAutowireComposer yang mengelola halaman daftar
 * transaksi jurnal kas keluar (pengeluaran) dalam modul akuntansi. Halaman ini
 * menampilkan semua entitas {@link Transaksi} bertipe {@code JURNAL_KAS_KELUAR}
 * pada sisi kredit ({@code merupakanDebet=false}), yaitu transaksi yang mencatat
 * aliran uang keluar dari kas organisasi. Pengguna dapat melihat, menambah,
 * mengubah, dan menghapus jurnal pengeluaran melalui antarmuka ini.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Siklus hidup halaman mengikuti pola standar ZK GenericAutowireComposer:
 * <ol>
 *   <li>{@code doBeforeCompose} — pemeriksaan keamanan awal sebelum komponen ZUL dibuat.</li>
 *   <li>{@code doAfterCompose} — inisialisasi komponen: mengambil pengguna saat ini,
 *       mendaftarkan listener pada filter akun kredit dan satuan kerja, menginisialisasi
 *       paging, mengatur tombol tambah dan hak akses, memanggil {@code onSearchDefault},
 *       mendaftarkan tombol ekspor Excel dengan criteria kustom, dan mendaftarkan
 *       tombol upload data dengan handler yang mengelola pembuatan {@link GrupTransaksi}
 *       baru secara otomatis jika belum ada.</li>
 *   <li>Inner class {@code TransaksiRenderer} — merender setiap baris grid dengan
 *       informasi grup transaksi (kode, jenis, satuan kerja, tanggal), bukti lampiran,
 *       nama akun, nama pegawai, deskripsi lengkap, dan tombol Ubah/Hapus.</li>
 *   <li>{@code onAdd} — membuka form tambah transaksi pengeluaran baru via
 *       {@link TransaksiJurnalPengeluaranHelper}.</li>
 *   <li>{@code initCriteria} — membangun Hibernate Criteria untuk query transaksi
 *       dengan berbagai filter (akun, kode, keterangan, satuan kerja, jenis jurnal).</li>
 *   <li>{@code onSearchDefault} — menjalankan query dan memperbarui grid.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Semua operasi UI berjalan di thread ZK event-dispatcher. Handler upload data
 * menerima callback yang berisi session Hibernate (dari thread upload) dan
 * dapat membuat GrupTransaksi baru menggunakan session tersebut secara langsung
 * dengan transaksi eksplisit. Penghapusan data menggunakan timer ZK default
 * untuk memanggil {@code onSearchDefault} setelah operasi selesai.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * - Filter akun menggunakan {@code AmbilDataAkunKreditBanbox} (bukan Debet)
 *   karena transaksi pengeluaran dicatat di sisi kredit akun kas.<br>
 * - Penghapusan hanya diizinkan jika {@code postingHistory} null (jurnal belum
 *   diposting); perubahan pada logika ini harus dikoordinasikan dengan
 *   modul posting jurnal.<br>
 * - Handler upload data sangat komplek; perubahan pada format file Excel impor
 *   harus diikuti dengan perubahan pada logika parsing di handler ini.<br>
 * - Array {@code contents} yang dipakai untuk cetak dan upload harus konsisten
 *   dengan properti entitas {@link Transaksi}.</p>
 */
public class TransaksiJurnalPengeluaranAction extends GenericAutowireComposer implements DataSearchDefault {

	/**
	 * Serial version UID untuk kompatibilitas serialisasi ZK session.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;

	/** Filter akun kredit menggunakan komponen pencarian akun khusus sisi kredit. */
	private AmbilDataAkunKreditBanbox searchnama;
	/** Filter satuan kerja menggunakan komponen pencarian hierarki satuan kerja. */
	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;
	private Textbox searchkode;
	private Textbox searchketerangan;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig add;
	/** Pengguna yang sedang login; dipakai untuk pembuatan GrupTransaksi baru saat upload. */
	private Tbmuser tbmuser;
	private Paging paging;

	/**
	 * <b>Tujuan:</b> Melakukan pemeriksaan keamanan sebelum halaman ZUL dikompilasi.
	 * Dipanggil oleh framework ZK sebagai langkah paling awal dalam siklus hidup
	 * komposisi halaman, sebelum komponen apapun diinstansiasi atau di-autowire.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang memverifikasi bahwa pengguna
	 * saat ini terautentikasi dan memiliki hak akses ke halaman jurnal pengeluaran.
	 * Jika tidak berhak, pengguna diarahkan ke logoff. Kemudian memanggil
	 * implementasi super untuk melanjutkan proses komposisi halaman.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super dikembalikan ke framework ZK. Pemeriksaan keamanan
	 * yang gagal menghentikan seluruh proses komposisi halaman.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan keamanan ini. Logika yang memerlukan komponen
	 * ZUL harus diletakkan di {@link #doAfterCompose(Component)}.</p>
	 *
	 * @param page     halaman ZK yang sedang dikompilasi
	 * @param parent   komponen induk dari composer ini
	 * @param compInfo metadata komponen dari ZUL
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
	 * jurnal pengeluaran setelah framework ZK selesai meng-autowire komponen ZUL.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah inisialisasi yang dilakukan:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Memeriksa sesi pengguna; jika tidak valid atau tidak punya hak READ,
	 *       arahkan ke logoff dan keluar.</li>
	 *   <li>Mendapatkan {@code tbmuser} yang diperlukan saat upload data untuk
	 *       mengisi field {@code satuanKerja} dan {@code pegawai} pada GrupTransaksi baru.</li>
	 *   <li>Mendaftarkan listener pada {@code searchnama} (filter akun) agar pencarian
	 *       otomatis dilakukan saat akun dipilih.</li>
	 *   <li>Menginisialisasi paging dengan listener standar.</li>
	 *   <li>Mengatur visibilitas tombol "Tambah" dan flag edit/delete sesuai hak akses.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk data awal.</li>
	 *   <li>Mendaftarkan listener pada {@code searchsatuanKerja} (filter satuan kerja).</li>
	 *   <li>Mendaftarkan tombol ekspor Excel dengan DataCriteria kustom (anonymous class)
	 *       yang membangun criteria khusus untuk ekspor (berbeda sedikit dengan criteria
	 *       pencarian utama karena menggunakan {@code createCriteria} bukan {@code createAlias}).</li>
	 *   <li>Mendaftarkan tombol upload dengan handler (anonymous EventListener) yang
	 *       memproses setiap baris data Excel: membuat GrupTransaksi baru jika kode
	 *       belum ada, mencegah duplikasi transaksi, dan menetapkan jenis jurnal
	 *       {@code JURNAL_KAS_KELUAR}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super atau inisialisasi komponen diteruskan ke framework ZK.
	 * Handler upload tidak melempar exception untuk data tidak valid; sebagai gantinya,
	 * menambahkan {@code false} ke {@code apakahSimpan} untuk memberi tahu framework
	 * bahwa baris tersebut tidak perlu disimpan.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Handler upload sangat bergantung pada struktur Map {@code datum} yang berisi
	 * data mentah dari file Excel. Key "grupTransaksi" digunakan untuk menentukan
	 * apakah perlu membuat GrupTransaksi baru. Perubahan pada template Excel impor
	 * harus diikuti dengan perubahan pada key yang dibaca di sini.</p>
	 *
	 * @param comp komponen root ZUL yang telah selesai di-autowire
	 * @throws Exception jika terjadi error saat inisialisasi komponen
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();

		searchnama.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);

		searchsatuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "grupTransaksi", "tanggalTransaksi", "tanggalDimasukkan", "akun",
				"debet", "kredit", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				Akun akun = (Akun) searchnama.getAttribute("akun");

				Criteria criteria = session.createCriteria(Transaksi.class)
						.add(akun == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("akun", akun))
						.createCriteria("grupTransaksi")
						.add((searchsatuanKerja == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsatuanKerja.getAttribute("satuanKerja") == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("satuanKerja", searchsatuanKerja.getAttribute("satuanKerja"))))

						.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_KAS_KELUAR))
						.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("deskripsi", searchketerangan.getValue(), MatchMode.ANYWHERE));

				if (order)
					criteria.addOrder(Order.desc("id"));
				return criteria;
			}
		}, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Transaksi.class, new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] data = (Object[]) arg0.getData();
				Transaksi transaksi = (Transaksi) data[0];
				Session session = (Session) data[1];
				Map datum = (Map) data[2];
				List apakahSimpan = (List) data[3];
				if (transaksi.getAkun() == null || transaksi.getTanggalTransaksi() == null) {
					apakahSimpan.add(false);
					return;
				}

				GrupTransaksi grupTransaksi = transaksi.getGrupTransaksi();
				if (grupTransaksi == null && datum.get("grupTransaksi") != null
						&& !datum.get("grupTransaksi").toString().trim().isEmpty()
						&& (datum.get("grupTransaksi") instanceof String)) {
					grupTransaksi = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
							.add(Restrictions.eq("kode", datum.get("grupTransaksi").toString().trim())).setMaxResults(1)
							.uniqueResult();
					if (grupTransaksi == null) {
						grupTransaksi = new GrupTransaksi();
						grupTransaksi.setSatuanKerja(tbmuser.ambilSatuanKerja());
						grupTransaksi.setTotalKredit(0.0);
						grupTransaksi.setTotalDebet(0.0);
						grupTransaksi.setTbmuser(tbmuser);
						grupTransaksi.setTanggalTransaksi(transaksi.getTanggalTransaksi());
						grupTransaksi.setJenisJurnal(Transaksi.JURNAL_KAS_KELUAR);
						grupTransaksi.setPegawai(tbmuser.ambilPegawai());
						grupTransaksi.setParentCode(datum.get("grupTransaksi").toString().trim());
						grupTransaksi.setKode(datum.get("grupTransaksi").toString().trim());
						grupTransaksi.setKeterangan(transaksi.getKeterangan());

						session.getTransaction().begin();
						session.save(grupTransaksi);
						session.getTransaction().commit();
					}
					transaksi.setGrupTransaksi(grupTransaksi);
				}
				Long idTrx = (Long) session.createCriteria(Transaksi.class)
						.add(Restrictions.eq("grupTransaksi", grupTransaksi))
						.add(Restrictions.eq("keterangan", transaksi.getKeterangan()))
						.add(Restrictions.eq("akun", transaksi.getAkun())).setProjection(Projections.property("id"))
						.setMaxResults(1).uniqueResult();
				transaksi.setId(idTrx);
				transaksi.setJenisJurnal(Transaksi.JURNAL_KAS_KELUAR);
				transaksi.setSimpan(true);
				transaksi.setParentCode(datum.get("grupTransaksi").toString().trim());
				transaksi.setKode(datum.get("grupTransaksi").toString().trim());
				transaksi.setMerupakanDebet(transaksi.getDebet() > 0.1);
			}
		}, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>TransaksiRenderer — Renderer Baris Grid Jurnal Pengeluaran</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class ini merender setiap baris pada grid daftar jurnal pengeluaran.
	 * Menampilkan informasi komprehensif tentang setiap transaksi pengeluaran
	 * termasuk kode jurnal, jenis transaksi, satuan kerja, tanggal, bukti lampiran,
	 * nama akun, nama pegawai, deskripsi detail (via {@code populateDeskripsiLengkap}),
	 * dan tombol Ubah/Hapus. Penghapusan dilakukan pada level {@link GrupTransaksi}
	 * (bukan level {@link Transaksi} individual) karena satu grup bisa memiliki
	 * banyak transaksi.</p>
	 *
	 * <p><b>Cara kerja render:</b><br>
	 * Metode utama {@code render} hanya memeriksa null dan mendelegasikan ke
	 * {@code initNotEdit} yang melakukan rendering sesungguhnya. Arsitektur ini
	 * mewarisi pola dari versi lama yang memiliki mode edit inline, yang kini
	 * sudah dihapus; naming {@code initNotEdit} merupakan sisa arsitektur lama.</p>
	 *
	 * <p><b>Threading:</b><br>
	 * Listener event berjalan di thread ZK event-dispatcher. Penghapusan GrupTransaksi
	 * menggunakan timer default agar grid direfresh setelah operasi selesai.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika diperlukan kolom baru di grid, tambahkan di metode {@code initNotEdit}
	 * dan di file ZUL secara bersamaan. Tombol hapus dinonaktifkan jika
	 * {@code postingHistory} tidak null (jurnal sudah diposting).</p>
	 */
	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Melakukan rendering lengkap satu baris grid jurnal pengeluaran,
		 * menampilkan semua kolom data transaksi beserta kontrol interaktif Ubah/Hapus.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Membersihkan isi baris ({@code Common.clear}) terlebih dahulu untuk menghindari
		 * duplikasi komponen saat re-render. Kemudian menampilkan:
		 * <ul>
		 *   <li>Kode grup transaksi dalam Vbox dengan widget revisi, jenis transaksi,
		 *       dan nama satuan kerja.</li>
		 *   <li>Tanggal transaksi dan widget upload/download bukti transaksi.</li>
		 *   <li>Nama akun kredit dari transaksi.</li>
		 *   <li>Nama pegawai yang membuat transaksi.</li>
		 *   <li>Grid deskripsi lengkap yang dihasilkan oleh
		 *       {@code grupTransaksi.populateDeskripsiLengkap()}.</li>
		 *   <li>Tombol "Ubah" yang membuka {@link TransaksiJurnalPengeluaranHelper}
		 *       dengan data transaksi yang ada; helper ini menampilkan modal edit.</li>
		 *   <li>Tombol "Hapus" yang menampilkan dialog konfirmasi dan menghapus
		 *       seluruh {@link GrupTransaksi} (beserta semua transaksinya) jika dikonfirmasi,
		 *       dengan kondisi bahwa GrupTransaksi belum diposting ({@code postingHistory==null}).</li>
		 * </ul>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception saat penghapusan ditangkap dan ditampilkan sebagai dialog error
		 * ke pengguna. Error juga ditampilkan ke admin melalui
		 * {@code Common.tampilErrorJikaAdmin}.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Nama metode {@code initNotEdit} adalah warisan arsitektur lama. Jika perlu
		 * mode edit inline di masa depan, buat metode terpisah.</p>
		 *
		 * @param arg0 baris ZK yang akan diisi komponen UI
		 * @param arg1 objek data, dicast ke {@link Transaksi}
		 * @throws Exception jika terjadi error saat membuat komponen ZK
		 */
		private void initNotEdit(final Row arg0, final Object arg1) throws Exception {
			Common.clear(arg0);
			final Transaksi transaksi = (Transaksi) arg1;

			final GrupTransaksi grupTransaksi = transaksi.getGrupTransaksi();

			Vbox a = RevisiHelper.createNewRevisi(GrupTransaksi.class, grupTransaksi, grupTransaksi.getKode());
			a.setParent(arg0);
			new Label(grupTransaksi.getJenisTransaksi() == null ? "" : grupTransaksi.getJenisTransaksi().getNama())
					.setParent(a);
			new Label(grupTransaksi.getSatuanKerja() == null ? "" : grupTransaksi.getSatuanKerja().getNama())
					.setParent(a);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(grupTransaksi.getTanggalTransaksi() == null ? ""
					: Common.dateFormat3.get().format(grupTransaksi.getTanggalTransaksi())).setParent(vbox);

			Vbox myvbox = new Vbox();
			myvbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, grupTransaksi.getId(), "Bukti Transaksi Jurnal Umum",
					"Bukti Transaksi", true, null, null, false, false, false, false);

			new Label(transaksi.getAkun() == null ? "" : transaksi.getAkun().getNama()).setParent(arg0);

			new Label(grupTransaksi.getPegawai() == null ? "" : grupTransaksi.getPegawai().getNama()).setParent(arg0);

			Object[] o = grupTransaksi.populateDeskripsiLengkap();
			Grid grid = (Grid) o[1];
			grid.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TransaksiJurnalPengeluaranHelper addWindow = new TransaksiJurnalPengeluaranHelper(transaksi);
					addWindow.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					});
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && grupTransaksi.getPostingHistory() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(grupTransaksi);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
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
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

		/**
		 * <b>Tujuan:</b> Entry point rendering baris yang dipanggil oleh framework ZK
		 * untuk setiap item dalam model daftar grid jurnal pengeluaran.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Melakukan pemeriksaan null pada objek transaksi; jika null, baris di-detach
		 * dari DOM untuk mencegah tampilan baris kosong. Jika valid, mendelegasikan
		 * rendering ke {@code initNotEdit} yang membangun semua komponen UI.
		 * Pemisahan ini memungkinkan penggantian mode render di masa depan tanpa
		 * mengubah metode utama.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception diteruskan ke framework ZK. Null check pada transaksi
		 * mencegah NullPointerException yang bisa terjadi jika model berisi
		 * elemen null.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika diperlukan penanganan khusus untuk jenis transaksi tertentu,
		 * tambahkan logika kondisional di sini sebelum mendelegasikan ke
		 * {@code initNotEdit}.</p>
		 *
		 * @param arg0 baris ZK yang akan dirender
		 * @param arg1 objek data dari model daftar, dicast ke {@link Transaksi}
		 * @throws Exception jika terjadi error saat rendering
		 */
		@Override
		public void render(final Row arg0, final Object arg1) throws Exception {
			final Transaksi transaksi = (Transaksi) arg1;
			if (transaksi == null) {
				arg0.detach();
				return;
			}
			initNotEdit(arg0, arg1);
		}

	}

	/**
	 * <b>Tujuan:</b> Menangani aksi pengguna ketika menekan tombol "Tambah"
	 * pada toolbar halaman, membuka form untuk menambahkan transaksi jurnal
	 * pengeluaran baru.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@link TransaksiJurnalPengeluaranHelper} dengan objek
	 * {@link Transaksi} baru (kosong) sebagai parameter. Helper ini merupakan
	 * komponen MyWindow yang memiliki form input lengkap untuk jurnal pengeluaran
	 * termasuk pemilihan grup transaksi, akun, dan nominal. Setelah pengguna
	 * menyimpan, callback EventListener memanggil {@code onSearchDefault} untuk
	 * menyegarkan tampilan grid.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke framework ZK. Helper sendiri menangani validasi
	 * input di dalam form-nya.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Logika form sepenuhnya ada di kelas {@link TransaksiJurnalPengeluaranHelper}.
	 * Perubahan pada form input sebaiknya dilakukan di sana, bukan di sini.</p>
	 *
	 * @param event event ZK dari klik tombol "Tambah"
	 * @throws Exception jika terjadi error saat membuat atau menampilkan helper
	 */
	public void onAdd(Event event) throws Exception {
		TransaksiJurnalPengeluaranHelper transaksiKasMasukHelper = new TransaksiJurnalPengeluaranHelper(
				new Transaksi());
		transaksiKasMasukHelper.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		transaksiKasMasukHelper.setVisible(true);
		transaksiKasMasukHelper.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun objek Hibernate {@link Criteria} untuk query data
	 * transaksi jurnal pengeluaran sesuai filter yang aktif di toolbar pencarian.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun criteria pada entitas {@link Transaksi} dengan kondisi-kondisi:
	 * <ul>
	 *   <li><b>Filter akun:</b> Jika akun dipilih di {@code searchnama}, menambahkan
	 *       filter {@code Restrictions.eq("akun", akun)}; jika tidak, semua akun.</li>
	 *   <li><b>Filter kode:</b> ILIKE pada field {@code kode} jika tidak kosong.</li>
	 *   <li><b>Filter keterangan:</b> ILIKE pada field {@code keterangan} jika tidak kosong.</li>
	 *   <li><b>Alias grupTransaksi:</b> Menggunakan {@code createAlias} untuk join
	 *       dengan tabel GrupTransaksi tanpa membuat subquery terpisah.</li>
	 *   <li><b>Filter satuan kerja:</b> Filter pada {@code grupTransaksi.satuanKerja}
	 *       melalui alias yang sudah dibuat.</li>
	 *   <li><b>Filter sisi kredit:</b> {@code merupakanDebet=false} untuk memastikan
	 *       hanya transaksi kredit yang ditampilkan (sisi pengeluaran kas).</li>
	 *   <li><b>Filter jenis jurnal:</b> {@code jenisJurnal=JURNAL_KAS_KELUAR}.</li>
	 *   <li><b>Pengurutan:</b> Jika {@code order=true}, diurutkan berdasarkan
	 *       {@code tanggalTransaksi} descending.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Null-safe pada komponen filter menggunakan {@code sqlRestriction("1=1")}
	 * sebagai fallback. Exception dari Hibernate diteruskan ke pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Penggunaan {@code createAlias} dan {@code createCriteria} tidak bisa
	 * dikombinasikan untuk alias yang sama dalam satu Criteria. Jika ada join
	 * tambahan, gunakan alias yang berbeda.</p>
	 *
	 * @param order {@code true} untuk menyertakan ORDER BY, {@code false} untuk COUNT
	 * @return objek {@link Criteria} yang telah dikonfigurasi dengan semua filter aktif
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Akun akun = (Akun) searchnama.getAttribute("akun");
		Criteria criteria = session.createCriteria(Transaksi.class)
				.add(akun == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("akun", akun))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE))

				.createAlias("grupTransaksi", "grupTransaksi")

				.add((searchsatuanKerja == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsatuanKerja.getAttribute("satuanKerja") == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("grupTransaksi.satuanKerja", searchsatuanKerja.getAttribute("satuanKerja"))))

				.add(Restrictions.eq("merupakanDebet", false))
				.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_KAS_KELUAR));

		if (order)
			criteria.addOrder(Order.desc("tanggalTransaksi"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian data transaksi jurnal pengeluaran
	 * berdasarkan filter aktif dan memperbarui tampilan grid dengan hasil yang
	 * ditemukan, disertai pembaruan komponen paging.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan dua panggilan {@link #initCriteria(boolean)}: pertama untuk
	 * menghitung total record dan memperbarui paging (dengan {@code order=false}),
	 * kemudian untuk mengambil data halaman aktif (dengan {@code order=true})
	 * menggunakan {@code setMaxResults} dan {@code setFirstResult}. Hasil query
	 * dibungkus dalam {@code SimpleListModel} dan diset ke grid dengan
	 * {@code TransaksiRenderer} baru.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke framework ZK. Jika paging null,
	 * data selalu dimulai dari baris pertama (indeks 0).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pastikan metode ini tetap idempoten. Dipanggil dari banyak tempat:
	 * timer, paging listener, filter listener, dan callback setelah operasi CRUD.</p>
	 *
	 * @param event event ZK pemicu pencarian (bisa null jika dipanggil programatik)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Transaksi> transaksi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transaksi);
		grid.setRowRenderer(new TransaksiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
