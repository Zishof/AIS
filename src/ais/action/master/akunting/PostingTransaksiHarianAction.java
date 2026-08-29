package ais.action.master.akunting;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.Transaksi;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>PostingTransaksiHarianAction — Proses Posting Jurnal Akuntansi Harian</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Controller ZK berbasis {@link GenericAutowireComposer} yang menangani proses posting
 * jurnal akuntansi harian. Posting jurnal adalah proses formal pemindahan transaksi
 * dari status "belum diposting" ke status "sudah diposting" dalam sistem buku besar
 * akuntansi. Setelah diposting, transaksi dikaitkan dengan sebuah {@link PostingHistory}
 * yang mencatat kapan, siapa, dan apa keterangan posting tersebut. Halaman ini
 * menampilkan daftar {@link Transaksi} (jurnal kas masuk, kas keluar, dan jurnal
 * umum) yang perlu atau sudah diposting, memungkinkan bagian akuntansi melakukan
 * posting massal (semua sekaligus) atau posting selektif (hanya baris yang dipilih).
 * Halaman ini juga dapat digunakan untuk menampilkan riwayat transaksi dari satu
 * {@link PostingHistory} tertentu jika parameter {@code postingHistory} diberikan.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Parameter URL {@code postingHistory} (opsional) menentukan mode tampilan: jika
 * ada, hanya menampilkan transaksi dari history tersebut; jika tidak, menampilkan
 * semua transaksi yang belum diposting. Tombol "Posting Semua" memproses seluruh
 * transaksi yang belum diposting dalam satu operasi batch. Tombol "Posting Terpilih"
 * memproses hanya baris yang checkbox-nya dicentang. Kedua operasi membuka window
 * konfirmasi modal yang meminta tanggal posting dan keterangan sebelum dieksekusi.
 * Pemuatan data menggunakan mekanisme progres ({@code loadDataDenganProgressPosting})
 * dengan anti-racing: jika ada permintaan reload baru saat proses sedang berjalan,
 * reload ditunda hingga proses selesai untuk menghindari kondisi balapan UI.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh operasi berjalan di event thread ZK. Mekanisme anti-racing pada
 * {@code loadDataDenganProgressPosting} menggunakan flag boolean
 * {@code postingJurnalLoadingAktif} dan {@code postingJurnalReloadTertunda}.
 * Flag ini aman diakses dari event thread ZK karena ZK menjamin serialisasi
 * event per sesi. Sesi Hibernate yang digunakan adalah sesi terkelola.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika ada jenis jurnal baru selain kas masuk, kas keluar, dan jurnal umum,
 * tambahkan ke array di {@code initCriteria} pada baris Restrictions.in("jenisJurnal",...).
 * Mekanisme progress ({@link ais.ui.util.PostingJurnalLoadingUtil}) menampilkan
 * feedback visual kepada pengguna selama proses loading — pastikan pesan yang
 * digunakan tetap deskriptif dan informatif jika ada perubahan alur loading.</p>
 *
 * @see Transaksi
 * @see PostingHistory
 * @see GrupTransaksi
 */
public class PostingTransaksiHarianAction extends GenericAutowireComposer {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas mekanisme serialisasi Java.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Grid utama penampil daftar {@link Transaksi} jurnal. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman daftar transaksi. */
	private Paging paging;

	/** Field teks pencarian berdasarkan kode transaksi. */
	private Textbox searchnama;

	/** Field teks pencarian berdasarkan keterangan transaksi. */
	private Textbox searchketerangan;

	/** Apakah pengguna memiliki hak UPDATE (untuk menampilkan tombol posting). */
	private boolean edit = false;

	/** Apakah pengguna memiliki hak DELETE (saat ini tidak digunakan aktif). */
	@SuppressWarnings("unused")
	private boolean delete = false;

	/**
	 * History posting yang sedang ditampilkan (null = tampilkan semua yang belum diposting).
	 * Diinisialisasi dari parameter URL {@code postingHistory} jika ada.
	 */
	private PostingHistory postingHistory = null;

	/** Tombol untuk mengirim/posting; hanya tampil jika pengguna punya hak UPDATE
	 *  dan tidak sedang melihat riwayat posting tertentu. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Dipanggil ZK sebelum komponen halaman dibangun. Memverifikasi hak akses
	 * keamanan melalui {@link Common#doCheckSecurity()}.
	 *
	 * <p><b>Tujuan:</b> Gerbang keamanan awal yang memastikan hanya pengguna dengan
	 * sesi valid yang dapat mengakses halaman posting transaksi harian ini.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} untuk
	 * validasi sesi dan modul, kemudian mendelegasikan ke super untuk melanjutkan
	 * proses komposisi komponen ZK.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika sesi tidak valid, proses dihentikan dan
	 * pengguna diarahkan ke halaman logoff.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada kebutuhan validasi
	 * keamanan tambahan sebelum komponen dibangun.</p>
	 *
	 * @param page halaman ZK yang sedang diproses
	 * @param parent komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen dari ZUL
	 * @return ComponentInfo dari implementasi super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil ZK setelah seluruh komponen halaman selesai di-autowire. Menginisialisasi
	 * halaman posting transaksi harian: paging, parameter URL, hak akses, visibilitas
	 * tombol, dan pemuatan data awal.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan halaman posting jurnal secara lengkap setelah
	 * seluruh komponen ZUL tersedia. Membaca parameter URL untuk menentukan mode
	 * tampilan (semua belum diposting vs. riwayat posting tertentu).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil super.doAfterCompose(comp).</li>
	 *   <li>Menginisialisasi paging dengan listener yang memanggil
	 *       {@code loadDataDenganProgressPosting} saat halaman diganti.</li>
	 *   <li>Memeriksa parameter URL {@code postingHistory}: jika ada, memuat
	 *       objek {@link PostingHistory} dari database berdasarkan ID tersebut.
	 *       Ini mengaktifkan mode "lihat riwayat" yang menampilkan transaksi
	 *       dari posting history tertentu.</li>
	 *   <li>Menyimpan flag hak akses edit dan delete.</li>
	 *   <li>Mengatur visibilitas tombol sent (posting): hanya tampil jika ada
	 *       hak edit DAN bukan dalam mode lihat riwayat (postingHistory == null).</li>
	 *   <li>Memanggil {@code loadDataDenganProgressPosting} untuk memuat data awal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception disebarkan ke ZK. Parsing ID posting
	 * history menggunakan Long.parseLong yang akan melempar NumberFormatException
	 * jika parameter tidak valid.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada parameter URL baru yang perlu dibaca untuk
	 * mengubah perilaku halaman, tambahkan pengecekan {@code execution.getParameter}
	 * di sini setelah blok postingHistory.</p>
	 *
	 * @param comp komponen root halaman ZK
	 * @throws Exception jika terjadi kesalahan saat inisialisasi atau membaca parameter
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});

		if (execution.getParameter("postingHistory") != null) {
			postingHistory = (PostingHistory) HibernateUtil.currentSession().createCriteria(PostingHistory.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("postingHistory")))).uniqueResult();
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (sent != null) { sent.setVisible(edit && postingHistory == null); }

		loadDataDenganProgressPosting(null);
	}

	/**
	 * Inner class renderer yang mengisi setiap baris grid dengan data dari satu
	 * objek {@link Transaksi}. Menampilkan informasi jurnal dan status postingnya.
	 *
	 * <p><b>Tujuan:</b> Menampilkan data transaksi jurnal dengan kolom: Kode Grup,
	 * Kode Akun (dengan link revisi), Nama Akun, Tanggal, Debet, Kredit, Keterangan,
	 * dan Status Posting. Memungkinkan pengguna mengidentifikasi transaksi mana yang
	 * sudah dan belum diposting sebelum melakukan aksi posting.</p>
	 *
	 * <p><b>Cara kerja:</b> ZK memanggil {@code render} untuk setiap elemen model.
	 * Menyimpan referensi transaksi sebagai atribut baris ({@code arg0.setAttribute})
	 * agar dapat diakses oleh {@code onPosting} saat iterasi baris grid dilakukan.
	 * Status posting ditampilkan: "Belum diposting" (via konfigurasi bahasa) jika
	 * postingHistory null, atau representasi string PostingHistory jika sudah diposting.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pastikan urutan appendChild sesuai urutan kolom ZUL.
	 * Atribut "transaksi" pada setiap baris diakses dari luar kelas (di {@code onPosting}),
	 * jangan mengubah nama atribut ini.</p>
	 */
	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi satu baris grid dengan data dari satu objek {@link Transaksi}.
		 *
		 * <p><b>Tujuan:</b> Merender baris transaksi jurnal dengan informasi yang
		 * cukup untuk bagian akuntansi memverifikasi sebelum melakukan posting.
		 * Termasuk kode grup transaksi, kode dan nama akun buku besar, tanggal,
		 * jumlah debet/kredit, keterangan, dan status posting.</p>
		 *
		 * <p><b>Cara kerja:</b>
		 * <ol>
		 *   <li>Mengcast {@code arg1} ke {@link Transaksi}.</li>
		 *   <li>Menyimpan referensi transaksi sebagai atribut baris untuk diakses
		 *       kemudian oleh logika posting selektif.</li>
		 *   <li>Menambahkan Label kode grup transaksi (kosong jika null).</li>
		 *   <li>Menambahkan link revisi untuk kode akun menggunakan RevisiHelper.</li>
		 *   <li>Menambahkan Label nama akun.</li>
		 *   <li>Menambahkan Label tanggal transaksi yang diformat dengan dateFormat3.</li>
		 *   <li>Menambahkan Label debet dan kredit yang diformat sebagai angka.</li>
		 *   <li>Menambahkan Label keterangan transaksi.</li>
		 *   <li>Menambahkan Label status posting: string dari bahasa config jika belum
		 *       diposting, atau {@code PostingHistory.toString()} jika sudah.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b> akun transaksi BISA null pada transaksi lama/data
		 * tak lengkap — {@code transaksi.getAkun()} dicek null sebelum memanggil
		 * {@code getKode()} (lihat createNewRevisi di bawah) agar tidak NullPointerException.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Urutan appendChild harus sesuai dengan definisi kolom
		 * di ZUL posting_transaksi_harian.zul.</p>
		 *
		 * @param arg0 baris grid ZK yang akan diisi
		 * @param arg1 objek data yang akan dicast ke {@link Transaksi}
		 * @throws Exception jika terjadi kesalahan saat rendering
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Transaksi transaksi = (Transaksi) arg1;
			arg0.setAttribute("transaksi", transaksi);

			new Label(transaksi.getGrupTransaksi() == null ? "" : transaksi.getGrupTransaksi().getKode())
					.setParent(arg0);

			RevisiHelper.createNewRevisi(Transaksi.class, transaksi,
					transaksi.getAkun() == null ? "" : transaksi.getAkun().getKode()).setParent(arg0);

			new Label(transaksi.getAkun() == null ? "" : transaksi.getAkun().getNama()).setParent(arg0);

			new Label(transaksi.getTanggalTransaksi() == null ? ""
					: Common.dateFormat3.get().format(transaksi.getTanggalTransaksi())).setParent(arg0);

			new Label(transaksi.getDebet() == null ? "" : Common.numberFormat.get().format(transaksi.getDebet()))
					.setParent(arg0);

			new Label(transaksi.getKredit() == null ? "" : Common.numberFormat.get().format(transaksi.getKredit()))
					.setParent(arg0);

			new Label(transaksi.getKeterangan()).setParent(arg0);

			new Label(transaksi.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: transaksi.getPostingHistory().toString()).setParent(arg0);

		}

	}

	/**
	 * Menangani klik tombol "Posting Semua" yang memproses seluruh transaksi yang
	 * belum diposting dalam satu operasi batch, setelah konfirmasi pengguna.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan bagian akuntansi mem-posting semua jurnal
	 * yang belum diposting sekaligus dengan satu kali konfirmasi, menghemat waktu
	 * dibandingkan memilih satu per satu. Digunakan pada tutup buku harian atau
	 * saat ada banyak transaksi yang perlu diposting bersamaan.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah grid tidak null dan memiliki baris. Jika tidak, return.</li>
	 *   <li>Membangun window konfirmasi modal programatis dengan input: Tanggal/Waktu
	 *       (default hari ini), penampil nama pengguna aktif, dan Keterangan (wajib).</li>
	 *   <li>Tombol Simpan menampilkan dialog konfirmasi "Apakah yakin...".</li>
	 *   <li>Jika dikonfirmasi (OK):
	 *     <ul>
	 *       <li>Membuat objek {@link PostingHistory} dengan jenis JENIS_UMUM, mencatat
	 *           pengguna, tanggal, dan keterangan, lalu menyimpannya.</li>
	 *       <li>Mengambil semua {@link Transaksi} yang postingHistory-nya masih null.</li>
	 *       <li>Untuk setiap transaksi: mengisi postingHistory, status SELESAI, dan
	 *           tanggal posting. Jika ada grup transaksi, refresh dan update juga.</li>
	 *       <li>Menampilkan pesan sukses, menutup window konfirmasi, dan merefresh grid.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari penyimpanan batch disebarkan ke ZK.
	 * Tidak ada rollback parsial; jika ada error di tengah loop, transaksi yang
	 * sudah diposting tetap tersimpan (sesi Hibernate flush per operasi).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika perlu menambahkan validasi tambahan (misalnya
	 * memastikan semua transaksi seimbang sebelum diposting), tambahkan sebelum
	 * blok pembuatan PostingHistory di dalam listener konfirmasi.</p>
	 *
	 * @param event event klik ZK dari tombol Posting Semua
	 * @throws Exception jika terjadi kesalahan saat membangun window atau penyimpanan
	 */
	public void onPostingSemua(Event event) throws Exception {
		if (grid == null || grid.getRows() == null) {
			return;
		}

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Jurnal Umum");
		addWindow.setWidth("300px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(center);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Posting menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses posting. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (keterangan.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Keterangan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Keterangan dengan deskripsi posting yang jelas; (2) Pastikan keterangan tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses posting. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin mem-posting semua data ?", "Question",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();

									PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_UMUM);
									postingHistory.setTbmuser(Common.getCurrentUser());
									postingHistory.setTanggal(tgl);
									postingHistory.setKeterangan(keterangan.getValue().trim());
									session.save(postingHistory);

									List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
											.add(Restrictions.isNull("postingHistory")).list();

									for (Transaksi transaksi : transaksis) {
										GrupTransaksi grupTransaksi = transaksi.getGrupTransaksi();
										transaksi.setPostingHistory(postingHistory);

										transaksi.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
										transaksi.setTanggalPosting(ais.ui.util.WaktuUtil.getDate());

										if (grupTransaksi != null) {
											session.refresh(grupTransaksi);
											grupTransaksi.setPostingHistory(postingHistory);
											session.update(grupTransaksi);
										}

										session.update(transaksi);

									}

									MyMessageboxConfig.show("Posting jurnal umum berhasil dilakukan", "Informasi",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

									addWindow.detach();

									onSearchDefaultTanpaProgress(event);

								}

							}
						});
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	/**
	 * Menangani klik tombol "Posting Terpilih" yang memproses hanya transaksi yang
	 * checkbox-nya dicentang oleh pengguna di grid, setelah konfirmasi.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan bagian akuntansi memilih secara selektif
	 * transaksi mana yang akan diposting, berguna ketika tidak semua transaksi
	 * dalam daftar siap untuk diposting (misalnya ada yang masih memerlukan
	 * verifikasi tambahan).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memeriksa apakah grid tidak null dan memiliki baris. Jika tidak, return.</li>
	 *   <li>Membangun window konfirmasi modal yang identik dengan {@code onPostingSemua}
	 *       (tanggal, pengguna, keterangan).</li>
	 *   <li>Jika dikonfirmasi (OK):
	 *     <ul>
	 *       <li>Membuat PostingHistory baru.</li>
	 *       <li>Mengiterasi semua baris grid, mengambil checkbox dari atribut baris,
	 *           dan hanya memproses baris yang checkbox-nya dicentang DAN tidak disabled.</li>
	 *       <li>Untuk setiap baris terpilih: mengambil transaksi dari atribut baris
	 *           dan mengupdate postingHistory, statusPosting, dan tanggalPosting.</li>
	 *       <li>Menampilkan pesan sukses dan merefresh grid.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Error ditangkap, ditampilkan via {@link Common#tampilErrorJikaAdmin}, dan
	 *       pesan error kustom ditampilkan ke pengguna.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari iterasi baris atau penyimpanan
	 * ditangkap dan ditampilkan sebagai pesan error yang informatif. Grid direfresh
	 * setelah error untuk menampilkan status terkini.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Checkbox di baris grid adalah {@link MyCheckboxConfig}
	 * yang disimpan sebagai atribut "checkbox" pada setiap baris oleh renderer.
	 * Jika renderer berubah untuk tidak menyimpan checkbox sebagai atribut, logika
	 * di sini perlu disesuaikan.</p>
	 *
	 * @param event event klik ZK dari tombol Posting Terpilih
	 * @throws Exception jika terjadi kesalahan saat membangun window konfirmasi
	 */
	@SuppressWarnings("unchecked")
	public void onPosting(Event event) throws Exception {
		if (grid == null || grid.getRows() == null) {
			return;
		}

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Jurnal Umum");
		addWindow.setWidth("300px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(center);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Posting menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses posting. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				if (keterangan.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Keterangan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Keterangan dengan deskripsi posting yang jelas; (2) Pastikan keterangan tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses posting. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show("Apakah yakin ingin mem-posting data yang anda pilih ?", "Question",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										List<Row> rows = grid.getRows().getChildren();
										Session session = HibernateUtil.currentSession();

										PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_UMUM);
										postingHistory.setTbmuser(Common.getCurrentUser());
										postingHistory.setTanggal(tgl);
										postingHistory.setKeterangan(keterangan.getValue().trim());
										session.save(postingHistory);

										for (Row row : rows) {
											MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
											if (checkbox.isDisabled() || !checkbox.isChecked())
												continue;
											Transaksi transaksi = (Transaksi) row.getAttribute("transaksi");

											transaksi.setPostingHistory(checkbox.isChecked() ? postingHistory : null);

											transaksi.setStatusPosting(
													checkbox.isChecked() ? Transaksi.STATUS_POSTING_SELESAI
															: Transaksi.STATUS_PEMERIKSAAN_BELUM);
											transaksi.setTanggalPosting(
													checkbox.isChecked() ? ais.ui.util.WaktuUtil.getDate() : null);

											session.update(transaksi);

										}

										MyMessageboxConfig.show("Posting jurnal umum berhasil dilakukan", "Informasi",
												MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

										onSearchDefaultTanpaProgress(event);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig
												.show("Data ini tidak dapat diposting. Error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});

			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	/**
	 * Membangun objek Hibernate Criteria untuk query daftar {@link Transaksi} jurnal
	 * berdasarkan filter aktif, dengan opsi pengurutan.
	 *
	 * <p><b>Tujuan:</b> Memusatkan logika query pencarian transaksi jurnal di satu
	 * tempat untuk digunakan ganda: paging (tanpa order) dan pengambilan data (dengan
	 * order). Mode tampilan (semua belum diposting vs. riwayat tertentu) ditentukan
	 * oleh field {@code postingHistory}.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membangun Criteria pada entitas {@link Transaksi}.</li>
	 *   <li>Filter mode: jika {@code postingHistory} null, menampilkan transaksi
	 *       dengan jenisJurnal dalam (kas_masuk, kas_keluar, jurnal_umum). Jika
	 *       tidak null, menampilkan transaksi dengan postingHistory tertentu.</li>
	 *   <li>Filter kode: ILIKE pada field kode jika searchnama tidak kosong.</li>
	 *   <li>Filter keterangan: ILIKE pada field keterangan jika searchketerangan
	 *       tidak kosong.</li>
	 *   <li>Jika {@code order} true, menambahkan ORDER BY statusPosting ASC,
	 *       id ASC (transaksi belum diposting muncul di atas).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada jenis jurnal baru, tambahkan ke array
	 * Transaksi.JURNAL_* pada filter Restrictions.in. Urutan sort (statusPosting
	 * ASC) menempatkan transaksi belum diposting di atas transaksi yang sudah.</p>
	 *
	 * @param order jika true, tambahkan ORDER BY statusPosting ASC, id ASC
	 * @return objek {@link Criteria} yang siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session
				.createCriteria(Transaksi.class).add(
						postingHistory == null
								? Restrictions.in("jenisJurnal",
										new String[] { Transaksi.JURNAL_KAS_MASUK, Transaksi.JURNAL_KAS_KELUAR,
												Transaksi.JURNAL_UMUM })
								: Restrictions.eq("postingHistory", postingHistory))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));

		if (order)
			criteria.addOrder(Order.asc("statusPosting")).addOrder(Order.asc("id"));
		return criteria;
	}

	/**
	 * Memuat data transaksi jurnal ke grid tanpa efek visual progres, langsung
	 * mengambil dari database dan memperbarui model grid.
	 *
	 * <p><b>Tujuan:</b> Metode internal yang melakukan pekerjaan aktual pengambilan
	 * data dan pembaruan grid, tanpa melibatkan mekanisme progres/loading UI.
	 * Dipanggil dari {@code loadDataDenganProgressPosting} setelah progress dimulai,
	 * dan juga langsung setelah operasi posting selesai untuk refresh cepat.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menginisialisasi paging via {@link Common#initPaging(Criteria, Paging)}.</li>
	 *   <li>Mengambil data halaman aktif dengan limit dan offset paging.</li>
	 *   <li>Membuat SimpleListModel dan menyetel renderer ke grid.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada kebutuhan post-processing
	 * data setelah query. Nama "TanpaProgress" menunjukkan bahwa metode ini tidak
	 * mengelola UI loading indicator.</p>
	 *
	 * @param event event ZK yang memicu pemuatan data; bisa null
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Transaksi> transaksi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transaksi);
		grid.setRowRenderer(new TransaksiRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Handler publik untuk event pencarian yang mendelegasikan ke
	 * {@code loadDataDenganProgressPosting} dengan mekanisme anti-racing dan
	 * feedback visual progres kepada pengguna.
	 *
	 * <p><b>Tujuan:</b> Menyediakan entry point publik yang dipanggil oleh ZK
	 * saat event pencarian dibutuhkan, sambil mendelegasikan ke implementasi
	 * dengan mekanisme progres yang lebih baik untuk pengalaman pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b> Hanya mendelegasikan ke
	 * {@code loadDataDenganProgressPosting(event)} tanpa logika tambahan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika perlu menambahkan logika pre-search (misalnya
	 * validasi filter), tambahkan sebelum pemanggilan loadDataDenganProgressPosting.</p>
	 *
	 * @param event event ZK yang memicu pencarian; bisa null
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag yang menandakan apakah proses loading data posting jurnal sedang berjalan.
	 * Digunakan untuk mekanisme anti-racing agar tidak ada dua proses loading yang
	 * berjalan bersamaan dalam satu sesi.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandakan ada permintaan reload yang tertunda karena loading sedang
	 * aktif. Saat loading selesai, jika flag ini true, loading akan dilakukan ulang.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * Memuat data transaksi jurnal ke grid dengan mekanisme anti-racing dan tampilan
	 * progres visual kepada pengguna. Mencegah loading ganda yang dapat menyebabkan
	 * kondisi balapan pada UI ZK.
	 *
	 * <p><b>Tujuan:</b> Menyediakan pengalaman pengguna yang lebih baik saat
	 * memuat data posting jurnal yang mungkin memerlukan waktu, sambil memastikan
	 * tidak ada dua proses loading yang berjalan bersamaan (yang dapat menyebabkan
	 * ketidakkonsistenan UI atau error Hibernate). Jika ada permintaan baru saat
	 * loading sedang berjalan, permintaan tersebut dijadwalkan untuk dieksekusi
	 * setelah loading saat ini selesai.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} true (sedang loading):
	 *     <ul>
	 *       <li>Set {@code postingJurnalReloadTertunda = true}.</li>
	 *       <li>Tampilkan pesan loading ke pengguna dan return.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Set {@code postingJurnalLoadingAktif = true} dan
	 *       {@code postingJurnalReloadTertunda = false}.</li>
	 *   <li>Tampilkan progress awal (7%).</li>
	 *   <li>Buat timer default yang di dalam handler-nya:
	 *     <ul>
	 *       <li>Update progress ke 48% dengan pesan "Mengambil data".</li>
	 *       <li>Panggil {@code onSearchDefaultTanpaProgress} untuk memuat data aktual.</li>
	 *       <li>Update progress ke 92% dengan pesan "Merapikan tampilan".</li>
	 *       <li>Di blok finally: reset flag, cek apakah ada reload tertunda.</li>
	 *       <li>Jika ada reload tertunda: jadwalkan ulang melalui timer baru.</li>
	 *       <li>Jika tidak: set progress selesai (100%).</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari {@code onSearchDefaultTanpaProgress}
	 * akan keluar dari blok try dan masuk ke finally, memastikan flag selalu
	 * direset meskipun ada error. Exception akhirnya disebarkan ke ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Nilai persentase progress (7, 48, 92, 96, 100) bersifat
	 * estimasi visual dan tidak mencerminkan kemajuan aktual. Jika proses loading
	 * bertambah langkah, sesuaikan nilai persentase agar lebih representatif.
	 * Flag {@code postingJurnalLoadingAktif} aman dari race condition karena ZK
	 * menjamin serialisasi event per sesi.</p>
	 *
	 * @param event event ZK yang memicu pemuatan data; bisa null jika dipanggil programatis
	 */
	private void loadDataDenganProgressPosting(final org.zkoss.zk.ui.event.Event event) {
		if (postingJurnalLoadingAktif) {
			postingJurnalReloadTertunda = true;
			ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Ulang Data Posting Jurnal",
					"Permintaan reload baru diterima. Data akan dimuat ulang setelah proses yang berjalan selesai.", 12);
			return;
		}
		postingJurnalLoadingAktif = true;
		postingJurnalReloadTertunda = false;
		ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Data Posting Jurnal",
				"Menyiapkan filter dan tabel data jurnal.", 7);
		Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event timerEvent) throws Exception {
				try {
					ais.ui.util.PostingJurnalLoadingUtil.update("Mengambil Data Posting Jurnal",
							"Mencari data sesuai tanggal, status posting, dan filter halaman.", 48);
					onSearchDefaultTanpaProgress(event);
					ais.ui.util.PostingJurnalLoadingUtil.update("Merapikan Tampilan",
							"Menyusun tabel, paging, status posting, dan preview jurnal.", 92);
				} finally {
					boolean reloadLagi = postingJurnalReloadTertunda;
					postingJurnalReloadTertunda = false;
					postingJurnalLoadingAktif = false;
					if (reloadLagi) {
						ais.ui.util.PostingJurnalLoadingUtil.update("Memuat Ulang Data Posting Jurnal",
								"Filter atau halaman berubah saat data sedang diproses. Data akan dimuat ulang sekarang.", 96);
						Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ulangEvent) throws Exception {
								loadDataDenganProgressPosting(event);
							}
						});
					} else {
						ais.ui.util.PostingJurnalLoadingUtil.complete("Data Posting Jurnal Siap",
								"Tabel sudah selesai dimuat dan siap digunakan.", 100);
					}
				}
			}
		});
	}


	// ================================================================= jalur API dasbor draft jurnal

	/**
	 * Kriteria dokumen jurnal umum yang SAMA dengan baris "Jurnal Umum" di dasbor draft jurnal
	 * ({@code DraftJurnalRingkasanUtil.kriteriaJurnalUmum}): jenis jurnal "Umum", tanggal transaksi
	 * pada rentang, belum closing. Disamakan supaya jumlah yang diproses tidak pernah berselisih
	 * dengan angka yang barusan dilihat pengguna di dasbor.
	 */
	private static Criteria kriteriaJurnalUmumStatic(Session session, Date mulai, Date sampai) {
		Criteria kriteria = session.createCriteria(GrupTransaksi.class)
				.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_UMUM))
				.add(Restrictions.isNull("closing"));
		if (mulai != null && sampai != null) {
			kriteria.add(Restrictions.sqlRestriction(ais.action.master.helper.PostingJurnalHelper
					.dateSql("this_.tanggal_transaksi", mulai, sampai)));
		}
		return kriteria;
	}

	/**
	 * Posting SEMUA jurnal umum draf pada rentang tanggal -- jalur API dasbor Draft Jurnal POS.
	 *
	 * <p>Dokumen jurnal umum BERBEDA dengan modul posting lain: jurnalnya (GrupTransaksi beserta
	 * baris Transaksi-nya) sudah ada sejak diketik manual, sehingga memposting hanya MENCAP dokumen
	 * dengan {@link PostingHistory}, tidak membuat jurnal baru. Kontrak per dokumen mengikuti
	 * {@code NewUiJournalService.post}: barisnya harus ada dan balance, dan bila konfigurasi bukti
	 * transaksi aktif, dokumen tanpa lampiran dilewati (tercatat sebagai "dilewati" di respons
	 * dasbor). Dokumen yang riwayat posting-nya sengaja dinonaktifkan ({@code posting=false}) TIDAK
	 * disentuh -- menyalakannya kembali adalah keputusan eksplisit di layar Jurnal Umum, bukan efek
	 * samping posting massal.</p>
	 *
	 * <p>Satu {@link PostingHistory} dipakai bersama seluruh dokumen yang berhasil -- sama dengan
	 * tombol "Posting Semua" ZK di kelas ini -- tetapi dengan {@code posting=true} supaya dasbor
	 * menghitungnya terposting ({@code PostingJurnalHelper.terapkanStatusPostingHistory}); tombol ZK
	 * lama lupa menyetel bendera itu sehingga hasil postingnya justru tetap terhitung draf.</p>
	 *
	 * <p>Transaksi dibuka sendiri (currentNativeSession + begin/commit per dokumen): dipanggil dari
	 * API tidak ada kerangka ZK yang meng-commit sesi berjalan, dan kegagalan satu dokumen tidak
	 * boleh membatalkan dokumen lain yang sudah sah tercap.</p>
	 */
	public static int postingSemua(Date mulai, Date sampai, ais.database.model.Tbmuser oleh,
			Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<?> ids = kriteriaJurnalUmumStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory"))
					.setProjection(org.hibernate.criterion.Projections.id()).list();
			if (ids.isEmpty()) {
				return 0;
			}

			boolean wajibBukti = Common.bolehKonfigurasi("file_bukti_transaksi_jurnal_wajib_diupload",
					ais.database.model.Konfigurasi.TIDAK_AKTIF);
			Date waktuPosting = tglPosting == null ? new Date() : tglPosting;

			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_UMUM);
			postingHistory.setTbmuser(oleh);
			postingHistory.setTanggal(waktuPosting);
			postingHistory.setTanggalPosting(waktuPosting);
			postingHistory.setPosting(true);
			postingHistory.setKeterangan("Posting massal jurnal umum dari dasbor draft jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			for (Object idObj : ids) {
				Long id = ((Number) idObj).longValue();
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					GrupTransaksi grup = (GrupTransaksi) session.get(GrupTransaksi.class, id);
					// Diperiksa ulang di dalam transaksi: sesi lain dapat memposting atau
					// meng-closing dokumen yang sama di sela pemilihan id dan pengecapannya.
					if (grup == null || grup.getPostingHistory() != null || grup.getClosing() != null) {
						session.getTransaction().rollback();
						continue;
					}
					if (wajibBukti && ais.database.model.file.LampiranLain.ambil(id,
							ais.common.newui.akunting.NewUiJournalService.ATTACHMENT_TYPE) == null) {
						session.getTransaction().rollback();
						continue;
					}
					List<?> barisJurnal = session.createCriteria(Transaksi.class)
							.add(Restrictions.eq("grupTransaksi.id", id)).list();
					double debet = 0, kredit = 0;
					for (Object o : barisJurnal) {
						Transaksi t = (Transaksi) o;
						debet += t.getDebet() == null ? 0 : t.getDebet().doubleValue();
						kredit += t.getKredit() == null ? 0 : t.getKredit().doubleValue();
					}
					if (barisJurnal.isEmpty() || debet < 0.005d || Math.abs(debet - kredit) >= 0.005d) {
						// Jurnal kosong/berat sebelah tidak boleh masuk buku besar; dibiarkan draf
						// supaya tetap terlihat di rincian dasbor (bendera jurnalSeimbang).
						session.getTransaction().rollback();
						continue;
					}
					grup.setTotalDebet(debet);
					grup.setTotalKredit(kredit);
					grup.setPostingHistory(postingHistory);
					session.update(grup);
					for (Object o : barisJurnal) {
						Transaksi t = (Transaksi) o;
						t.setPostingHistory(postingHistory);
						t.setStatusPosting(Transaksi.STATUS_POSTING_SELESAI);
						t.setTanggalPosting(waktuPosting);
						session.update(t);
					}
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingTransaksiHarianAction jalur API");
				}
			}

			if (n == 0) {
				// Tidak satu dokumen pun tercap: riwayat kosong tidak ditinggalkan di riwayat posting.
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.delete(postingHistory);
					session.getTransaction().commit();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingTransaksiHarianAction jalur API");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil posting
			}
		}
		return n;
	}

	/**
	 * Membatalkan posting SEMUA jurnal umum terposting pada rentang -- kebalikan {@link #postingSemua}.
	 *
	 * <p>Berbeda dengan modul yang jurnalnya turunan (mis. kas kecil), di sini GrupTransaksi ADALAH
	 * dokumen sumbernya: pembatalan hanya melepas cap {@link PostingHistory} dan mengembalikan status
	 * baris -- tidak ada satu baris jurnal pun yang dihapus. Riwayat posting-nya sendiri baru dihapus
	 * bila tidak lagi dirujuk dokumen lain: posting massal (tombol ZK maupun jalur ini) memakai SATU
	 * riwayat untuk banyak dokumen, sehingga menghapusnya membabi buta seperti
	 * {@code NewUiJournalService.unpost} justru gagal FK persis saat riwayatnya dipakai bersama.
	 * Yang dipilih hanya dokumen yang dasbor hitung terposting ({@code posting=true}); jurnal yang
	 * sudah closing tidak disentuh, sama dengan penjaga di jalur per dokumen.</p>
	 */
	public static int batalkanPostingSemua(Date mulai, Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			Criteria kriteria = kriteriaJurnalUmumStatic(session, mulai, sampai);
			ais.action.master.helper.PostingJurnalHelper.terapkanStatusPostingHistory(kriteria, true);
			List<?> ids = kriteria.setProjection(org.hibernate.criterion.Projections.id()).list();
			for (Object idObj : ids) {
				Long id = ((Number) idObj).longValue();
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					GrupTransaksi grup = (GrupTransaksi) session.get(GrupTransaksi.class, id);
					if (grup == null || grup.getClosing() != null || grup.getPostingHistory() == null) {
						session.getTransaction().rollback();
						continue;
					}
					PostingHistory riwayat = grup.getPostingHistory();
					grup.setPostingHistory(null);
					session.update(grup);
					List<?> barisJurnal = session.createCriteria(Transaksi.class)
							.add(Restrictions.eq("grupTransaksi.id", id)).list();
					for (Object o : barisJurnal) {
						Transaksi t = (Transaksi) o;
						t.setPostingHistory(null);
						t.setStatusPosting(Transaksi.STATUS_POSTING_BELUM);
						t.setTanggalPosting(null);
						session.update(t);
					}
					session.flush();
					Number sisaGrup = (Number) session.createCriteria(GrupTransaksi.class)
							.add(Restrictions.eq("postingHistory", riwayat))
							.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
					Number sisaBaris = (Number) session.createCriteria(Transaksi.class)
							.add(Restrictions.eq("postingHistory", riwayat))
							.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult();
					if ((sisaGrup == null ? 0 : sisaGrup.intValue())
							+ (sisaBaris == null ? 0 : sisaBaris.intValue()) == 0) {
						session.delete(riwayat);
					}
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingTransaksiHarianAction jalur API");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil pembatalan
			}
		}
		return n;
	}

}
