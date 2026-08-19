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

import ais.action.master.akunting.helper.AmbilDataAkunDebetBanbox;
import ais.action.master.akunting.helper.TransaksiJurnalPenerimaanHelper;
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
 * <h3>TransaksiJurnalPenerimaanAction — Controller Jurnal Kas Masuk</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini adalah ZK GenericAutowireComposer yang mengelola halaman daftar
 * transaksi jurnal kas masuk (penerimaan) dalam modul akuntansi. Halaman ini
 * menampilkan semua entitas {@link Transaksi} bertipe {@code JURNAL_KAS_MASUK}
 * pada sisi debet ({@code merupakanDebet=true}), yaitu transaksi yang mencatat
 * aliran uang masuk ke kas organisasi. Pengguna dapat melihat, menambah,
 * mengubah, dan menghapus jurnal penerimaan melalui antarmuka ini.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Siklus hidup halaman mengikuti pola standar ZK GenericAutowireComposer.
 * Kelas ini adalah pasangan simetris dari {@link TransaksiJurnalPengeluaranAction},
 * dengan perbedaan utama:
 * <ul>
 *   <li>Menggunakan {@code AmbilDataAkunDebetBanbox} (bukan KreditBanbox) karena
 *       penerimaan kas dicatat di sisi debet.</li>
 *   <li>Filter {@code merupakanDebet=true} dan {@code jenisJurnal=JURNAL_KAS_MASUK}.</li>
 *   <li>Menggunakan {@link TransaksiJurnalPenerimaanHelper} sebagai form input.</li>
 * </ul>
 * Selain perbedaan di atas, seluruh alur kerja identik dengan
 * {@code TransaksiJurnalPengeluaranAction}:
 * <ol>
 *   <li>{@code doBeforeCompose} — pemeriksaan keamanan awal.</li>
 *   <li>{@code doAfterCompose} — inisialisasi semua komponen, listener, paging,
 *       tombol ekspor, dan tombol upload.</li>
 *   <li>Inner class {@code TransaksiRenderer} — merender baris grid dengan
 *       informasi grup transaksi, bukti lampiran, nama akun, pegawai, deskripsi,
 *       dan tombol Ubah/Hapus.</li>
 *   <li>{@code onAdd} — membuka form tambah transaksi penerimaan baru.</li>
 *   <li>{@code initCriteria} — membangun Hibernate Criteria dengan semua filter aktif.</li>
 *   <li>{@code onSearchDefault} — menjalankan query dan memperbarui grid.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Semua operasi UI berjalan di thread ZK event-dispatcher. Handler upload data
 * beroperasi dengan session Hibernate yang diberikan oleh framework upload,
 * menggunakan transaksi eksplisit untuk pembuatan GrupTransaksi baru.
 * Penghapusan menggunakan timer default ZK untuk refresh grid setelah operasi.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * - Sinkronkan perubahan dengan {@link TransaksiJurnalPengeluaranAction} jika
 *   ada perbaikan pada logika yang sama (misalnya handler upload, renderer).<br>
 * - Filter menggunakan {@code AmbilDataAkunDebetBanbox}; pertimbangkan untuk
 *   membuat base class jika ada banyak duplikasi kode antara kedua action ini.<br>
 * - Tombol hapus hanya aktif jika GrupTransaksi belum diposting
 *   ({@code postingHistory==null}), konsisten dengan aturan bisnis akuntansi.<br>
 * - Array {@code contents} menentukan kolom ekspor dan upload; perbarui saat
 *   ada perubahan skema entitas {@link Transaksi}.</p>
 */
public class TransaksiJurnalPenerimaanAction extends GenericAutowireComposer implements DataSearchDefault {

	/**
	 * Serial version UID untuk kompatibilitas serialisasi ZK session.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;
	private Paging paging;

	/** Filter akun debet menggunakan komponen pencarian akun khusus sisi debet. */
	private AmbilDataAkunDebetBanbox searchnama;
	/** Filter satuan kerja menggunakan komponen pencarian hierarki satuan kerja. */
	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;
	private Textbox searchkode;
	private Textbox searchketerangan;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig add;
	/** Pengguna yang sedang login; dipakai untuk pembuatan GrupTransaksi baru saat upload. */
	private Tbmuser tbmuser;

	/**
	 * <b>Tujuan:</b> Melakukan pemeriksaan keamanan sebelum halaman ZUL dikompilasi.
	 * Dipanggil oleh framework ZK sebagai langkah paling awal dalam siklus hidup
	 * komposisi halaman, sebelum komponen apapun diinstansiasi atau di-autowire.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang memverifikasi bahwa pengguna
	 * saat ini terautentikasi dan memiliki hak akses ke halaman jurnal penerimaan.
	 * Jika tidak berhak, pengguna diarahkan ke logoff sebelum halaman dimuat.
	 * Memanggil implementasi super untuk melanjutkan proses komposisi halaman.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari super dikembalikan ke framework ZK untuk ditangani sesuai
	 * konfigurasi error handler aplikasi.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan hapus pemanggilan keamanan. Semua logika yang memerlukan komponen
	 * ZUL yang sudah di-autowire harus diletakkan di {@link #doAfterCompose(Component)}.</p>
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
	 * jurnal penerimaan setelah framework ZK selesai meng-autowire komponen ZUL.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah inisialisasi yang dilakukan secara berurutan:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan autowiring.</li>
	 *   <li>Memeriksa sesi dan hak akses READ; jika tidak valid, arahkan ke logoff.</li>
	 *   <li>Mendapatkan {@code tbmuser} yang diperlukan saat upload untuk mengisi
	 *       atribut GrupTransaksi baru (satuan kerja, pegawai).</li>
	 *   <li>Mendaftarkan listener pada {@code searchnama} (filter akun debet) agar
	 *       pencarian diperbarui otomatis saat akun dipilih.</li>
	 *   <li>Menginisialisasi paging dengan listener standar.</li>
	 *   <li>Mengatur visibilitas tombol "Tambah" dan flag {@code edit/delete}.</li>
	 *   <li>Memanggil {@code onSearchDefault(null)} untuk data awal.</li>
	 *   <li>Mendaftarkan listener pada {@code searchsatuanKerja}.</li>
	 *   <li>Mendaftarkan tombol ekspor Excel dengan DataCriteria kustom yang menggunakan
	 *       {@code createCriteria("grupTransaksi")} (bukan alias) untuk keperluan join.</li>
	 *   <li>Mendaftarkan tombol upload dengan handler pembuatan GrupTransaksi otomatis:
	 *       mencari GrupTransaksi berdasarkan kode, membuat baru jika belum ada,
	 *       menetapkan jenis jurnal {@code JURNAL_KAS_MASUK} dan sisi debet.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Handler upload tidak melempar exception untuk baris data tidak valid;
	 * menambahkan {@code false} ke {@code apakahSimpan} sebagai sinyal ke framework.
	 * Exception lain diteruskan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Kriteria untuk ekspor berbeda dari criteria utama: menggunakan
	 * {@code createCriteria("grupTransaksi")} bukan {@code createAlias}.
	 * Ini adalah perbedaan yang disengaja karena ekspor memerlukan join yang
	 * berbeda. Jangan menyeragamkan keduanya tanpa pengujian menyeluruh.</p>
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

						.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE))

						.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE))

						.createCriteria("grupTransaksi")

						.add((searchsatuanKerja == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsatuanKerja.getAttribute("satuanKerja") == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("satuanKerja", searchsatuanKerja.getAttribute("satuanKerja"))))

						.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_KAS_MASUK));

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
						grupTransaksi.setJenisJurnal(Transaksi.JURNAL_KAS_MASUK);
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
				transaksi.setJenisJurnal(Transaksi.JURNAL_KAS_MASUK);
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
	 * <h3>TransaksiRenderer — Renderer Baris Grid Jurnal Penerimaan</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Inner class ini merender setiap baris pada grid daftar jurnal penerimaan kas.
	 * Secara fungsional identik dengan renderer pada
	 * {@link TransaksiJurnalPengeluaranAction}, namun tombol Ubah membuka
	 * {@link TransaksiJurnalPenerimaanHelper} (bukan helper pengeluaran).
	 * Menampilkan: kode jurnal, jenis transaksi, satuan kerja, tanggal,
	 * bukti lampiran, nama akun debet, nama pegawai, deskripsi lengkap,
	 * dan tombol Ubah/Hapus.</p>
	 *
	 * <p><b>Cara kerja render:</b><br>
	 * Metode utama {@code render} memeriksa null dan mendelegasikan ke
	 * {@code initNotEdit}. Arsitektur ini mewarisi pola dari versi lama.
	 * Penghapusan dilakukan pada level {@link GrupTransaksi}; hanya diizinkan
	 * jika jurnal belum diposting ({@code postingHistory==null}).</p>
	 *
	 * <p><b>Threading:</b><br>
	 * Listener event berjalan di thread ZK event-dispatcher. Timer default
	 * digunakan untuk refresh grid setelah penghapusan.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Sinkronkan perubahan dengan renderer pada {@code TransaksiJurnalPengeluaranAction}
	 * jika ada perbaikan yang berlaku untuk keduanya.</p>
	 */
	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Melakukan rendering lengkap satu baris grid jurnal penerimaan,
		 * menampilkan semua kolom data beserta kontrol interaktif Ubah/Hapus.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Membersihkan isi baris terlebih dahulu, kemudian menampilkan kolom-kolom:
		 * kode grup transaksi (dengan revisi history, jenis transaksi, satuan kerja),
		 * tanggal dan widget bukti transaksi, nama akun debet, nama pegawai,
		 * grid deskripsi lengkap, serta tombol Ubah (membuka
		 * {@link TransaksiJurnalPenerimaanHelper}) dan Hapus (dengan konfirmasi
		 * dan penghapusan GrupTransaksi beserta semua transasinya).
		 * Tombol hapus disembunyikan jika jurnal sudah diposting.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception saat penghapusan ditangkap dan ditampilkan sebagai dialog error.
		 * Error juga dikonsolekan melalui {@code Common.tampilErrorJikaAdmin}.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika ada kolom baru yang ditambahkan di ZUL, tambahkan juga komponen
		 * terkait di metode ini pada posisi yang tepat sesuai urutan kolom.</p>
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

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TransaksiJurnalPenerimaanHelper addWindow = new TransaksiJurnalPenerimaanHelper(transaksi);
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
			aksiButtons.add(button);

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
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

		/**
		 * <b>Tujuan:</b> Entry point rendering baris yang dipanggil oleh framework ZK
		 * untuk setiap item dalam model daftar grid jurnal penerimaan.
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Melakukan pemeriksaan null pada objek transaksi; jika null, baris di-detach
		 * dari DOM. Jika valid, mendelegasikan rendering ke {@code initNotEdit}
		 * yang membangun semua komponen UI untuk baris tersebut.</p>
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Exception diteruskan ke framework ZK. Null check mencegah NPE dari model
		 * yang berisi elemen null.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Jika ada penanganan khusus per jenis transaksi, tambahkan kondisional
		 * di sini sebelum mendelegasikan ke {@code initNotEdit}.</p>
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
	 * penerimaan baru.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance {@link TransaksiJurnalPenerimaanHelper} dengan objek
	 * {@link Transaksi} baru sebagai parameter. Helper ini merupakan komponen
	 * MyWindow dengan form input lengkap untuk jurnal penerimaan. Setelah
	 * pengguna menyimpan data di dalam helper, callback EventListener memanggil
	 * {@code onSearchDefault} untuk menyegarkan tampilan grid dengan data terbaru.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception diteruskan ke framework ZK. Helper menangani validasi inputnya
	 * sendiri secara internal.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Semua logika form ada di kelas {@link TransaksiJurnalPenerimaanHelper}.
	 * Jika perlu nilai default tertentu pada transaksi baru (misalnya tanggal
	 * hari ini), set pada objek {@code Transaksi} baru sebelum memanggil helper.</p>
	 *
	 * @param event event ZK dari klik tombol "Tambah"
	 * @throws Exception jika terjadi error saat membuat atau menampilkan helper
	 */
	public void onAdd(Event event) throws Exception {
		TransaksiJurnalPenerimaanHelper transaksiJurnalPenerimaanHelper = new TransaksiJurnalPenerimaanHelper(
				new Transaksi());
		transaksiJurnalPenerimaanHelper.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		transaksiJurnalPenerimaanHelper.setVisible(true);
		transaksiJurnalPenerimaanHelper.onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun objek Hibernate {@link Criteria} untuk query data
	 * transaksi jurnal penerimaan sesuai filter yang aktif di toolbar pencarian.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun criteria pada entitas {@link Transaksi} dengan kondisi-kondisi:
	 * <ul>
	 *   <li><b>Filter akun:</b> Jika akun debet dipilih, menambahkan filter
	 *       {@code eq("akun", akun)}; jika tidak, semua akun ditampilkan.</li>
	 *   <li><b>Filter kode:</b> ILIKE pada field {@code kode} jika tidak kosong.</li>
	 *   <li><b>Filter keterangan:</b> ILIKE pada field {@code keterangan} jika tidak kosong.</li>
	 *   <li><b>Alias grupTransaksi:</b> Join melalui {@code createAlias} untuk
	 *       mengakses field-field GrupTransaksi dalam kondisi filter.</li>
	 *   <li><b>Filter satuan kerja:</b> Filter pada alias {@code grupTransaksi.satuanKerja}.</li>
	 *   <li><b>Filter sisi debet:</b> {@code merupakanDebet=true} — hanya transaksi
	 *       debet yang ditampilkan (sisi penerimaan kas masuk).</li>
	 *   <li><b>Filter jenis jurnal:</b> {@code jenisJurnal=JURNAL_KAS_MASUK}.</li>
	 *   <li><b>Pengurutan:</b> Jika {@code order=true}, berdasarkan {@code tanggalTransaksi}
	 *       descending.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Null-safe pada komponen filter menggunakan {@code sqlRestriction("true")} atau
	 * {@code sqlRestriction("1=1")} sebagai fallback. Exception dari Hibernate
	 * diteruskan ke pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Perbedaan kritis dengan versi pengeluaran: {@code merupakanDebet=true}
	 * dan {@code jenisJurnal=JURNAL_KAS_MASUK}. Jangan ubah nilai ini tanpa
	 * memahami dampaknya pada logika akuntansi debet-kredit.</p>
	 *
	 * @param order {@code true} untuk menyertakan ORDER BY (pengambilan data),
	 *              {@code false} untuk query COUNT (paging)
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

				.add(Restrictions.eq("merupakanDebet", true))
				.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_KAS_MASUK));

		if (order)
			criteria.addOrder(Order.desc("tanggalTransaksi"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Menjalankan pencarian data transaksi jurnal penerimaan
	 * berdasarkan filter aktif dan memperbarui tampilan grid dengan hasil yang
	 * ditemukan, disertai pembaruan komponen paging.
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Melakukan dua panggilan {@link #initCriteria(boolean)}: pertama dengan
	 * {@code order=false} untuk menghitung total record dan memperbarui paging,
	 * kemudian dengan {@code order=true} untuk mengambil data halaman aktif
	 * menggunakan {@code setMaxResults} dan {@code setFirstResult}. Hasilnya
	 * dibungkus dalam {@code SimpleListModel} dan diset ke grid bersama
	 * {@code TransaksiRenderer} baru.</p>
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari Hibernate diteruskan ke framework ZK. Jika paging null,
	 * data dimulai dari indeks 0 (baris pertama).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini dipanggil dari berbagai titik (timer, paging, filter, callback
	 * CRUD). Pastikan tetap idempoten dan tidak memiliki efek samping.</p>
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
