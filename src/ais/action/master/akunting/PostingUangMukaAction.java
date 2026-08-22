package ais.action.master.akunting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.UangMuka;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>PostingUangMukaAction — Pengelola Posting Jurnal Uang Muka ke Buku Besar</h3>
 *
 * <p><strong>Untuk apa:</strong> Kelas ini adalah ZK GenericAutowireComposer yang
 * mengelola proses posting transaksi {@link UangMuka} (uang muka/advance payment) ke
 * jurnal umum (buku besar) dalam sistem akuntansi AIS. Uang muka adalah pembayaran di
 * muka yang dikeluarkan kepada pegawai atau pihak ketiga untuk kegiatan operasional yang
 * akan dipertanggungjawabkan kemudian. Posting menghasilkan entri jurnal akuntansi
 * (GrupTransaksi dengan detail debet/kredit) yang merepresentasikan transaksi tersebut
 * dalam buku besar.</p>
 *
 * <p><strong>Cara kerja:</strong> Alur kerja utama kelas ini adalah:
 * <ol>
 *   <li>Halaman menampilkan daftar UangMuka yang telah disetujui (disetujuiOleh tidak null)
 *       dengan daftarPengajuanTransfer terhubung ke prosesTransfer, difilter berdasarkan
 *       rentang tanggal, satuan kerja, kode/nama, dan status posting.</li>
 *   <li>Setiap baris grid menampilkan kode UangMuka, nama, nilai pengajuan, nilai dana
 *       talangan (jika ada), selisih, tanggal persetujuan, preview jurnal, status posting,
 *       dan tombol aksi.</li>
 *   <li>Tombol "Posting Semua" ({@link #onPostingSemua}) menjalankan posting massal dalam
 *       thread latar belakang menggunakan native session.</li>
 *   <li>Tombol per baris "Posting Data" dan "Batalkan Posting" untuk operasi individual.</li>
 *   <li>Tombol "Batalkan Posting Semua" ({@link #onBatalkanPostingSemua}) membatalkan semua
 *       posting dalam filter aktif.</li>
 * </ol>
 * </p>
 *
 * <p><strong>Logika Jurnal Dasar:</strong> Debet = akun dari JenisUangMuka.akun (akun uang
 * muka); Kredit = ditentukan oleh flag daftarPengajuanTransfer: jika transitori=true, kredit
 * = caraPembayaranTransfer.akunTransitori; jika transfer=true, kredit =
 * caraPembayaranTransfer.akun; selain itu kredit = null (transaksi tidak valid).</p>
 *
 * <p><strong>Logika Dana Talangan:</strong> Jika UangMuka memiliki danaTalangan yang sudah
 * disetujui (disetujuiOleh tidak null), jurnal menjadi lebih kompleks: Debet = [akunDebet
 * (nilai dari danaTalangan), akunKelebihan dari JenisUangMuka danaTalangan (selisih)];
 * Kredit = [akunKredit (nilaiPengajuan total)]. Dana talangan merepresentasikan kelebihan
 * dari talangan sebelumnya yang digunakan untuk menutup sebagian uang muka baru.</p>
 *
 * <p><strong>Threading:</strong> Proses posting massal ({@link #onPostingSemua}) menggunakan
 * thread Java terpisah dengan {@code HibernateUtil.currentNativeSession()} yang harus dikelola
 * manual (begin/commit/close). Thread ZK event (posting per baris, batalkan) menggunakan
 * {@code currentSession()} managed session.</p>
 *
 * <p><strong>Pemeliharaan:</strong> Jika logika penentuan akun kredit berubah (misalnya
 * penambahan metode pembayaran baru), perbarui blok kondisi di {@link #onPostingSemua} dan
 * {@link UangMukaRenderer}. Kedua tempat harus selalu sinkron. Jika struktur DanaTalangan
 * berubah, periksa logika nilai/selisih di kedua tempat tersebut.</p>
 *
 * @see UangMuka
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 */
public class PostingUangMukaAction extends GenericAutowireComposer {

	/** Serial version UID untuk serialisasi kelas. */
	private static final long serialVersionUID = -5779730217402400328L;

	private MyGrid grid;

	private Paging paging;

	private Textbox searchkode;
	private MyCheckboxConfig searchtampil;
	private MyCheckboxConfig searchtelahtampil;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean edit = false;

	private MyToolbarbuttonConfig sent;

	/** Flag apakah pengguna memiliki hak admin atau hak APPROVE untuk batalkan posting. */
	public boolean adminLain;

	private MyDatebox tglMulai;
	private MyDatebox tglSampai;

	private Tbmuser tbmuser;

	/**
	 * Dipanggil sebelum komponen ZK dirakit untuk memeriksa keamanan akses halaman.
	 *
	 * <p><strong>Tujuan:</strong> Memastikan hanya pengguna dengan sesi valid yang dapat
	 * mengakses halaman posting uang muka sebelum komponen UI dibangun.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@code Common.doCheckSecurity()} untuk
	 * verifikasi sesi, kemudian melanjutkan ke implementasi parent ZK.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Ditangani oleh {@code Common.doCheckSecurity()}
	 * yang akan redirect ke halaman login jika sesi tidak valid.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Boilerplate keamanan standar; tidak perlu diubah.</p>
	 *
	 * @param page     halaman ZK yang sedang dimuat
	 * @param parent   komponen induk dalam pohon ZK
	 * @param compInfo metadata komponen ZK
	 * @return ComponentInfo dari pemanggilan super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil setelah semua komponen ZK berhasil dirakit. Melakukan inisialisasi lengkap
	 * halaman posting uang muka termasuk filter tanggal default, listener satuan kerja,
	 * hak akses, parameter URL, dan pemuatan data awal.
	 *
	 * <p><strong>Tujuan:</strong> Menyiapkan semua aspek halaman agar siap digunakan,
	 * termasuk rentang tanggal default 6 bulan, listener filter satuan kerja, dan
	 * visibilitas tombol berdasarkan hak akses pengguna.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Langkah inisialisasi berurutan:
	 * <ol>
	 *   <li>Verifikasi sesi dan hak READ; logoff jika tidak valid.</li>
	 *   <li>Menyimpan pengguna saat ini ke field {@code tbmuser}.</li>
	 *   <li>Mendaftarkan listener onChange pada searchparent (filter satuan kerja).</li>
	 *   <li>Membuat SatuanKerjaTreeModel untuk navigasi hierarki satuan kerja.</li>
	 *   <li>Mengatur rentang tanggal default: 6 bulan lalu hingga sekarang.</li>
	 *   <li>Membaca parameter URL {@code mulai} dan {@code sampai} untuk override tanggal
	 *       (format yyyyMMdd); jika ada, field tanggal di-disable.</li>
	 *   <li>Mengatur flag adminLain berdasarkan hak admin atau APPROVE.</li>
	 *   <li>Mengatur visibilitas tombol sent berdasarkan hak UPDATE.</li>
	 *   <li>Memuat data awal melalui {@link #loadDataDenganProgressPosting}.</li>
	 *   <li>Mendaftarkan listener paging dan membuat timer default untuk refresh.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari parsing parameter URL ditangkap
	 * dan ditampilkan ke admin. Jika sesi tidak valid, langsung return.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada parameter URL baru, tambahkan pembacaannya
	 * di sini setelah blok pembacaan parameter mulai/sampai.</p>
	 *
	 * @param comp komponen root ZK yang telah dirakit
	 * @throws Exception jika terjadi error saat inisialisasi komponen ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglMulai != null) tglMulai.setValue(calendar.getTime());
		if (tglMulai != null) tglMulai.setReadonly(true);
		if (tglSampai != null) tglSampai.setReadonly(true);

		if (execution.getParameter("mulai") != null) {
			try {
				if (tglMulai != null) tglMulai.setValue(Common.dateFormat8.get().parse(execution.getParameter("mulai")));
				if (tglMulai != null) tglMulai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (execution.getParameter("sampai") != null) {
			try {
				if (tglSampai != null) tglSampai.setValue(Common.dateFormat8.get().parse(execution.getParameter("sampai")));
				if (tglSampai != null) tglSampai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		loadDataDenganProgressPosting(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});

	}

	/**
	 * Menangani event klik tombol "Batalkan Posting Semua" untuk membatalkan seluruh
	 * posting uang muka dalam filter aktif setelah konfirmasi pengguna.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan cara massal untuk membatalkan semua posting
	 * uang muka, misalnya ketika terjadi kesalahan penentuan akun atau perlu koreksi
	 * periode. Seluruh GrupTransaksi terkait akan dihapus dari buku besar.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menampilkan dialog konfirmasi MyMessageboxConfig.
	 * Jika pengguna mengonfirmasi (OK), mengambil semua UangMuka yang sudah diposting
	 * (postingHistory tidak null) dalam filter aktif, kemudian untuk setiap UangMuka:
	 * <ol>
	 *   <li>Menyetel postingHistory ke null.</li>
	 *   <li>Menyimpan perubahan dengan {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li>Menghapus GrupTransaksi terkait menggunakan SQL native:
	 *       {@code DELETE FROM akunting.grup_transaksi WHERE uang_muka=id AND closing IS NULL}.</li>
	 * </ol>
	 * Setelah selesai, memuat ulang grid via timer default.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari operasi database diteruskan ke
	 * framework ZK melalui signature throws di EventListener.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Filter {@code closing IS NULL} pada SQL hapus
	 * penting untuk tidak menghapus entri yang sudah di-closing. Jangan ubah kondisi ini.</p>
	 *
	 * @param event event ZK dari klik tombol Batalkan Posting Semua
	 * @throws Exception jika terjadi error saat dialog atau operasi database
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi uang muka ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<UangMuka> uangMukas = initCriteria(true).add(Restrictions.isNotNull("postingHistory"))
									.list();

							for (UangMuka uangMuka : uangMukas) {
								uangMuka.setPostingHistory(null);
								Common.refreshSaveOrUpdate(uangMuka);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where uang_muka=" + uangMuka.getId() + " and closing is null")
										.executeUpdate();
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDenganProgressPosting(null);
							}
						});
					}
				});

	}

	/**
	 * Menangani event klik tombol "Posting Semua" untuk membuka dialog konfirmasi dan
	 * menjalankan proses posting massal semua UangMuka yang belum diposting dalam filter
	 * aktif menggunakan thread latar belakang.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan staf akuntansi memposting semua transaksi
	 * uang muka yang memenuhi filter aktif dalam satu operasi massal. Dialog meminta
	 * tanggal posting, menampilkan nama poster, dan opsional keterangan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membuka popup dialog konfirmasi MyWindow dengan field
	 * tanggal posting (wajib), nama poster (read-only otomatis), keterangan (opsional).
	 * Setelah pengguna mengklik Simpan dan mengonfirmasi:
	 * <ol>
	 *   <li>Membuat PostingHistory baru dengan jenis JENIS_PERSETUJUAN_UANG_MUKA.</li>
	 *   <li>Menyimpan PostingHistory dalam native session dengan begin/commit terpisah.</li>
	 *   <li>Mengambil semua UangMuka belum diposting dalam filter aktif.</li>
	 *   <li>Untuk setiap UangMuka: menentukan akunDebet dari jenisUangMuka.akun; menentukan
	 *       akunKredit dari flag transitori/transfer pada daftarPengajuanTransfer; menentukan
	 *       akunTalangan jika ada danaTalangan yang disetujui.</li>
	 *   <li>Jika akun valid: memanggil {@code CommonAkunting.saveTransaksi}. Jika nilai
	 *       positif: Debet→Kredit; Jika negatif: Kredit→Debet (swap). Jika ada talangan:
	 *       gunakan varian array akun dengan dua debet (akunDebet+akunTalangan) dan satu kredit.</li>
	 *   <li>Menyetel postingHistory ke UangMuka dan menyimpan perubahan.</li>
	 *   <li>Label progress diperbarui dengan persentase kemajuan setiap iterasi.</li>
	 *   <li>Setelah selesai, menutup native session dan menampilkan pesan sukses.</li>
	 * </ol>
	 * Proses berjalan dalam thread Java terpisah {@code new Thread().start()} menggunakan
	 * {@code HibernateUtil.currentNativeSession()} (harus begin/commit/close manual).</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception per UangMuka ditangkap secara
	 * diam-diam (catch kosong) agar satu UangMuka gagal tidak menghentikan proses. Exception
	 * saat saveTransaksi ditampilkan ke admin. closeSession() dipanggil di akhir thread.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada jenis transfer baru (selain transitori/transfer),
	 * tambahkan kondisi penentuan akunKredit di sini dan di {@link UangMukaRenderer}. Pastikan
	 * kedua tempat selalu sinkron. Jika ada perubahan pada struktur DanaTalangan, periksa
	 * logika nilai/selisih.</p>
	 *
	 * @param event event ZK dari klik tombol Posting Semua
	 * @throws Exception jika terjadi error saat membangun dialog popup
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Uang Muka");
		addWindow.setWidth("800px");
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
				// if (keterangan.getValue().trim().equals("")) {
				// MyMessageboxConfig.show("Keterangan harus diisi",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// return;
				// }

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi uang muka ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Tbmuser tbmuser = Common.getCurrentUser();

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show("Posting transaksi uang muka berhasil dilakukan",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});

											addWindow.detach();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											Session session = HibernateUtil.currentNativeSession();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_PERSETUJUAN_UANG_MUKA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<UangMuka> uangMukas = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (UangMuka uangMuka : uangMukas) {

												SatuanKerja satuanKerja = (SatuanKerja) (uangMuka
														.getSatuanKerja() != null ? uangMuka.getSatuanKerja() : null);

												if (uangMuka != null) {

													try {
														Akun akunDebet = uangMuka.getJenisUangMuka().getAkun();
														Akun akunKredit = uangMuka.getDaftarPengajuanTransfer()
																.getTransitori()
																		? uangMuka.getDaftarPengajuanTransfer()
																				.getProsesTransfer()
																				.getCaraPembayaranTransfer()
																				.getAkunTransitori()
																		: uangMuka.getDaftarPengajuanTransfer()
																				.getTransfer()
																						? uangMuka
																								.getDaftarPengajuanTransfer()
																								.getProsesTransfer()
																								.getCaraPembayaranTransfer()
																								.getAkun()
																						: null;
														Akun akunTalangan = uangMuka.getDanaTalangan() != null
																&& uangMuka.getDanaTalangan().getDisetujuiOleh() != null
																		? uangMuka.getDanaTalangan().getJenisUangMuka()
																				.getAkunKelebihan()
																		: null;

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {
																ket = "Persetujuan uang muka \"" + uangMuka.getKode()
																		+ "\" pada penggunaan anggaran \""
																		+ uangMuka.getWorkspace().getKode() + " "
																		+ uangMuka.getWorkspace().getNama()
																		+ "\" senilai " + Common.numberFormat.get()
																				.format(uangMuka.getNilai());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get()
																			.format(rowIndex * 100.0 / uangMukas.size())
																	+ " %)");

															Double nilai = akunTalangan == null ? uangMuka.getNilai()
																	: uangMuka.getDanaTalangan().getNilai();
															Double nilaiPengajuan = uangMuka.getNilai();
															Double selisih = nilaiPengajuan - nilai;

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();

																if (akunTalangan == null) {
																	if (nilai > 0.1) {
																		CommonAkunting.saveTransaksi(akunDebet,
																				akunKredit, akunDenda, akunPiutangDenda,
																				postingHistory, apakahUangMasuk, ket,
																				uangMuka.getTanggalPersetujuan(), nilai,
																				denda, uangMuka, satuanKerja, session);
																	} else {
																		CommonAkunting.saveTransaksi(akunKredit,
																				akunDebet, akunDenda, akunPiutangDenda,
																				postingHistory, apakahUangMasuk, ket,
																				uangMuka.getTanggalPersetujuan(), nilai,
																				denda, uangMuka, satuanKerja, session);
																	}
																} else {
																	if (nilai > 0.1) {
																		CommonAkunting.saveTransaksi(
																				new Akun[] { akunDebet, akunTalangan },
																				new Akun[] { akunKredit }, akunDenda,
																				akunPiutangDenda, postingHistory,
																				apakahUangMasuk, ket,
																				uangMuka.getTanggalPersetujuan(),
																				new Double[] { nilai, selisih },
																				new Double[] { nilaiPengajuan }, denda,
																				uangMuka, satuanKerja, session);
																	} else {
																		CommonAkunting.saveTransaksi(
																				new Akun[] { akunKredit },
																				new Akun[] { akunDebet, akunTalangan },
																				akunDenda, akunPiutangDenda,
																				postingHistory, apakahUangMasuk, ket,
																				uangMuka.getTanggalPersetujuan(),
																				new Double[] { nilaiPengajuan },
																				new Double[] { nilai, selisih }, denda,
																				uangMuka, satuanKerja, session);
																	}
																}
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															uangMuka.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(uangMuka);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingUangMukaAction.java:632");
														// Exception per-baris ditangkap untuk melanjutkan proses berikutnya
													}

												}
												rowIndex++;
											}

											label.setValue("");
											HibernateUtil.closeSession();
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

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
	 * Kelas inner untuk merender setiap baris data {@link UangMuka} pada grid utama
	 * halaman posting uang muka.
	 *
	 * <p><strong>Tujuan:</strong> Mengubah setiap entitas UangMuka menjadi baris tampilan
	 * grid yang informatif dengan kolom kode+link proses transfer, nama+SOP, nilai pengajuan,
	 * nilai dana talangan, selisih, tanggal persetujuan, preview jurnal, status posting,
	 * dan tombol aksi posting/batalkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Metode {@code render} mengisi row dengan:
	 * <ul>
	 *   <li>Kolom kode: Vbox berisi link revisi kode UangMuka dan link proses transfer
	 *       (jika ada), dibuat kecil (font-size:12px).</li>
	 *   <li>Kolom nama: Vbox berisi nama UangMuka dan link SOP (jika disposisiSop ada).</li>
	 *   <li>Kolom nilai pengajuan: nilaiPengajuan (nilai total dari UangMuka).</li>
	 *   <li>Kolom dana talangan: nilai dari danaTalangan (0 jika tidak ada talangan).</li>
	 *   <li>Kolom selisih: nilaiPengajuan - nilai talangan (0 jika tidak ada talangan).</li>
	 *   <li>Kolom tanggal: tanggal persetujuan diformat.</li>
	 *   <li>Kolom jurnal: preview jurnal menggunakan {@code GrupTransaksi.tampilkanJurnal}.
	 *       Jika ada talangan: tampilkan jurnal multi-akun. Jika akun tidak valid: pesan error.</li>
	 *   <li>Kolom status: status posting (Belum diposting / nama poster + no. bukti).</li>
	 *   <li>Kolom aksi: tombol Batalkan Posting (jika sudah posting) dan Posting Data
	 *       (jika belum posting), hanya tersedia jika akun valid.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><strong>Threading:</strong> Render dipanggil pada thread ZK event; aman mengakses
	 * currentSession().</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Logika penentuan akun di metode render harus selalu
	 * sinkron dengan logika di {@link #onPostingSemua}.</p>
	 */
	class UangMukaRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data {@link UangMuka} ke dalam komponen {@link Row} ZK,
		 * lengkap dengan informasi dana talangan, preview jurnal multi-akun, dan tombol aksi.
		 *
		 * <p><strong>Tujuan:</strong> Menampilkan semua informasi relevan UangMuka dalam
		 * satu baris grid yang mudah dibaca dan actionable, termasuk preview jurnal yang
		 * menggambarkan entri buku besar yang akan terbentuk saat posting.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Menentukan akun debet, kredit, dan talangan
		 * berdasarkan jenis transfer dan keberadaan danaTalangan. Jika akun valid,
		 * menampilkan jurnal preview (single atau multi-akun untuk kasus talangan) dan
		 * tombol aksi. Tombol Posting Data saat diklik akan:
		 * <ol>
		 *   <li>Membuat PostingHistory baru dengan jenis JENIS_PERSETUJUAN_UANG_MUKA.</li>
		 *   <li>Menentukan kembali akun (akunDebet, akunKredit, akunTalangan).</li>
		 *   <li>Memanggil CommonAkunting.saveTransaksi dengan array akun yang sesuai.</li>
		 *   <li>Menyetel postingHistory ke UangMuka dan menyimpan perubahan.</li>
		 *   <li>Memuat ulang grid via loadDataDenganProgressPosting.</li>
		 * </ol>
		 * Posting per baris menggunakan managed session (currentSession()) karena berjalan
		 * pada thread ZK event.</p>
		 *
		 * <p><strong>Penanganan error:</strong> Exception saat membangun keterangan posting
		 * ditangkap dan ditampilkan ke admin. Pesan "Transaksi tidak valid." ditampilkan
		 * jika akun debet atau kredit bernilai null.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong> Logika penentuan akun harus sinkron dengan
		 * {@link #onPostingSemua}. Periksa juga kondisi selisih.intValue()==0 yang digunakan
		 * untuk memilih antara posting single-akun vs multi-akun pada kasus talangan.</p>
		 *
		 * @param arg0 komponen Row ZK yang akan diisi
		 * @param arg1 objek data yang harus berupa instansi UangMuka
		 * @throws Exception jika terjadi error saat render komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final UangMuka uangMuka = (UangMuka) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(UangMuka.class, uangMuka,
					uangMuka.getKode() == null ? "" : uangMuka.getKode())).setParent(arg0);

			if (uangMuka.getDaftarPengajuanTransfer() != null
					&& uangMuka.getDaftarPengajuanTransfer().getProsesTransfer() != null) {
				A a = new A(uangMuka.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, uangMuka.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(uangMuka.getNama()).setParent(a);
			if (uangMuka.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + uangMuka.getDisposisiSop().getKeterangan()
						+ " (" + uangMuka.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(uangMuka.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Akun akunDebet = uangMuka.getJenisUangMuka().getAkun();
			Akun akunKredit = uangMuka.getDaftarPengajuanTransfer().getTransitori()
					? uangMuka.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()
							.getAkunTransitori()
					: uangMuka.getDaftarPengajuanTransfer().getTransfer()
							? uangMuka.getDaftarPengajuanTransfer().getProsesTransfer().getCaraPembayaranTransfer()
									.getAkun()
							: null;

			Akun akunTalangan = uangMuka.getDanaTalangan() != null
					&& uangMuka.getDanaTalangan().getDisetujuiOleh() != null
							? uangMuka.getDanaTalangan().getJenisUangMuka().getAkunKelebihan()
							: null;

			Double nilai = akunTalangan == null ? uangMuka.getNilai() : uangMuka.getDanaTalangan().getNilai();
			Double nilaiPengajuan = uangMuka.getNilai();
			Double selisih = nilaiPengajuan - nilai;

			new Label(Common.numberFormat.get().format(nilaiPengajuan)).setParent(arg0);
			new Label(Common.numberFormat.get().format(akunTalangan == null ? 0.0 : nilai)).setParent(arg0);
			new Label(Common.numberFormat.get().format(akunTalangan == null ? 0.0 : selisih)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(uangMuka.getTanggalPersetujuan())).setParent(arg0);

			if (akunDebet != null && akunKredit != null) {

				if (akunTalangan == null) {

					GrupTransaksi.tampilkanJurnal(akunDebet, nilai, akunKredit, nilai).setParent(arg0);

				} else {

					List<Akun> akunsDebets = new ArrayList<Akun>();
					List<Akun> akunsKredits = new ArrayList<Akun>();

					List<Double> nilaiDebets = new ArrayList<Double>();
					List<Double> nilaiKredits = new ArrayList<Double>();

					akunsDebets.add(akunDebet);
					akunsDebets.add(akunTalangan);

					nilaiDebets.add(nilai);
					nilaiDebets.add(selisih);

					akunsKredits.add(akunKredit);
					nilaiKredits.add(nilaiPengajuan);

					GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);

				}

			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.getKode() + "-" + akunDebet.getNama() + "."
								: " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class).add(Restrictions.eq("uangMuka", uangMuka))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(uangMuka.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: uangMuka.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && uangMuka.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								uangMuka.setPostingHistory(null);
								Common.refreshSaveOrUpdate(uangMuka);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where uang_muka=" + uangMuka.getId() + " and closing is null")
										.executeUpdate();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadDataDenganProgressPosting(null);
									}
								});
							}
						});

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && uangMuka.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PERSETUJUAN_UANG_MUKA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = uangMuka.getJenisUangMuka().getAkun();
								Akun akunKredit = uangMuka.getDaftarPengajuanTransfer().getTransitori()
										? uangMuka.getDaftarPengajuanTransfer().getProsesTransfer()
												.getCaraPembayaranTransfer().getAkunTransitori()
										: uangMuka.getDaftarPengajuanTransfer().getTransfer()
												? uangMuka.getDaftarPengajuanTransfer().getProsesTransfer()
														.getCaraPembayaranTransfer().getAkun()
												: null;
								Akun akunTalangan = uangMuka.getDanaTalangan() != null
										&& uangMuka.getDanaTalangan().getDisetujuiOleh() != null
												? uangMuka.getDanaTalangan().getJenisUangMuka().getAkunKelebihan()
												: null;

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									Double nilai = akunTalangan == null ? uangMuka.getNilai()
											: uangMuka.getDanaTalangan().getNilai();
									Double nilaiPengajuan = uangMuka.getNilai();
									Double selisih = nilaiPengajuan - nilai;

									String ket = "";
									try {

										ket = "Persetujuan uang muka \"" + uangMuka.getKode()
												+ "\" pada penggunaan anggaran \"" + uangMuka.getWorkspace().getKode()
												+ " " + uangMuka.getWorkspace().getNama() + "\" senilai "
												+ Common.numberFormat.get().format(uangMuka.getNilai());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (uangMuka.getSatuanKerja() != null
											? uangMuka.getSatuanKerja()
											: tbmuser.ambilSatuanKerja());

									if (selisih.intValue() == 0) {
										if (nilai > 0.1) {
											CommonAkunting.saveTransaksi(akunDebet, akunKredit, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
													uangMuka.getTanggalPersetujuan(), nilai, denda, uangMuka,
													satuanKerja, session);
										} else {
											CommonAkunting.saveTransaksi(akunKredit, akunDebet, akunDenda,
													akunPiutangDenda, postingHistory, apakahUangMasuk, ket,
													uangMuka.getTanggalPersetujuan(), nilai, denda, uangMuka,
													satuanKerja, session);
										}
									} else {
										if (nilai > 0.1) {
											CommonAkunting.saveTransaksi(new Akun[] { akunDebet, akunTalangan },
													new Akun[] { akunKredit }, akunDenda, akunPiutangDenda,
													postingHistory, apakahUangMasuk, ket,
													uangMuka.getTanggalPersetujuan(), new Double[] { nilai, selisih },
													new Double[] { nilaiPengajuan }, denda, uangMuka, satuanKerja,
													session);
										} else {
											CommonAkunting.saveTransaksi(new Akun[] { akunKredit },
													new Akun[] { akunDebet, akunTalangan }, akunDenda, akunPiutangDenda,
													postingHistory, apakahUangMasuk, ket,
													uangMuka.getTanggalPersetujuan(), new Double[] { nilaiPengajuan },
													new Double[] { nilai, selisih }, denda, uangMuka, satuanKerja,
													session);
										}
									}

									uangMuka.setPostingHistory(postingHistory);
									session.update(uangMuka);
								}

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
				button.setParent(toolbar);
			}

		}
	}

	/**
	 * Membangun Hibernate {@link Criteria} untuk query {@link UangMuka} berdasarkan filter
	 * aktif di halaman.
	 *
	 * <p><strong>Tujuan:</strong> Menyatukan logika pembangunan query UangMuka agar dapat
	 * digunakan untuk paging, pengambilan data aktual, maupun operasi massal (batalkan
	 * semua, posting semua).</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membuat Criteria dengan join ke daftarPengajuanTransfer
	 * dan filter:
	 * <ul>
	 *   <li>prosesTransfer tidak null (hanya UangMuka yang terhubung ke proses transfer).</li>
	 *   <li>Satuan kerja sesuai hierarki filter searchparent.</li>
	 *   <li>Checkbox searchtampil (belum posting = postingHistory null).</li>
	 *   <li>Checkbox searchtelahtampil (sudah posting = postingHistory tidak null).</li>
	 *   <li>disetujuiOleh tidak null (hanya yang sudah disetujui).</li>
	 *   <li>nilai tidak nol dan tidak null.</li>
	 *   <li>Rentang tanggal persetujuan menggunakan fungsi SQL date().</li>
	 *   <li>Kode/nama/keterangan menggunakan ILIKE (case-insensitive).</li>
	 * </ul>
	 * Jika {@code order} true, menambahkan pengurutan berdasarkan ID descending.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception Hibernate diteruskan ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada filter baru, tambahkan kondisi Restrictions
	 * di sini. Filter searchtampil dan searchtelahtampil keduanya independen (tidak
	 * saling-eksklusif secara logika, meskipun UI biasanya hanya satu yang aktif).</p>
	 *
	 * @param order true untuk menambahkan pengurutan berdasarkan ID desc
	 * @return Criteria Hibernate yang sudah dikonfigurasi sesuai filter aktif
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(UangMuka.class)

				.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer")

				.add(Restrictions.isNotNull("daftarPengajuanTransfer.prosesTransfer"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.isNotNull("disetujuiOleh"))

				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
						+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and  date('"
						+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan", searchkode.getValue(), MatchMode.ANYWHERE))));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat data UangMuka ke grid tanpa tampilan progress bar secara langsung.
	 *
	 * <p><strong>Tujuan:</strong> Melakukan query dan render grid aktual sebagai metode
	 * internal yang dipanggil dari {@link #loadDataDenganProgressPosting} setelah progress
	 * bar ditampilkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menghitung total untuk paging menggunakan criteria
	 * tanpa order, kemudian mengambil data halaman aktif dengan batasan ROWS_COUNT_ON_PAGE
	 * dan offset halaman aktif. Membungkus hasil dalam SimpleListModel dan memuat ke grid
	 * dengan {@link UangMukaRenderer}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception Hibernate diteruskan ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini hanya dipanggil dari
	 * {@link #loadDataDenganProgressPosting}; tidak dimaksudkan untuk pemanggilan langsung.</p>
	 *
	 * @param event event ZK pemicu (dapat null)
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<UangMuka> uangMuka = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(uangMuka);
		grid.setRowRenderer(new UangMukaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Handler event pencarian default yang mendelegasikan ke
	 * {@link #loadDataDenganProgressPosting} untuk pembaruan grid dengan progress bar.
	 *
	 * <p><strong>Tujuan:</strong> Menjadi titik masuk terpusat untuk semua pembaruan grid,
	 * memastikan progress bar selalu ditampilkan saat data dimuat.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Hanya memanggil
	 * {@link #loadDataDenganProgressPosting(Event)} dengan event yang diterima.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jangan menaruh logika langsung di sini; selalu
	 * delegasikan ke loadDataDenganProgressPosting.</p>
	 *
	 * @param event event ZK pemicu (dapat null)
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/** Flag yang menandakan apakah proses loading data sedang berlangsung. */
	private boolean postingJurnalLoadingAktif = false;

	/** Flag yang menandakan ada permintaan reload tertunda saat loading berlangsung. */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * Memuat ulang data grid dengan menampilkan progress bar dan mengelola concurrent
	 * reload menggunakan mekanisme flag untuk mencegah race condition.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan feedback visual kepada pengguna saat data
	 * grid sedang dimuat (progress bar dengan pesan status), sekaligus mencegah multiple
	 * reload bersamaan yang dapat menyebabkan inkonsistensi tampilan grid UangMuka.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menggunakan dua flag boolean:
	 * {@code postingJurnalLoadingAktif} dan {@code postingJurnalReloadTertunda}.
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} true saat dipanggil, set
	 *       {@code postingJurnalReloadTertunda = true} dan return — tidak ada reload baru.</li>
	 *   <li>Jika tidak sedang loading, set flag aktif dan jalankan melalui timer default.</li>
	 *   <li>Dalam callback timer: tampilkan progress 48%, panggil
	 *       {@link #onSearchDefaultTanpaProgress}, tampilkan progress 92%.</li>
	 *   <li>Di blok finally: reset flag aktif. Jika ada reload tertunda, jadwalkan ulang;
	 *       jika tidak, tampilkan selesai (100%).</li>
	 * </ol>
	 * Progress ditampilkan menggunakan {@code PostingJurnalLoadingUtil} dengan nilai
	 * persentase 7%, 48%, 92%, 96%, 100%.</p>
	 *
	 * <p><strong>Threading:</strong> Semua operasi pada thread ZK event melalui timer;
	 * tidak ada thread tambahan di metode ini sendiri.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception diteruskan; blok finally memastikan
	 * flag selalu direset meskipun terjadi exception.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Mekanisme flag ini aman dalam lingkungan ZK
	 * (single-thread per desktop). Persentase progress bersifat estimasi visual.</p>
	 *
	 * @param event event ZK yang memicu reload (dapat null untuk pemanggilan programatik)
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


	// ================================================================ mesin posting massal

	/**
	 * Akun kredit diambil dari cara pembayaran pada proses transfernya: akun transitori
	 * bila pengajuannya bersifat transitori, akun biasa bila berupa transfer. Rantainya
	 * dibaca bertahap supaya dokumen yang belum lengkap menghasilkan null, bukan
	 * NullPointerException.
	 */
	private static Akun akunKreditTransfer(ais.database.model.akunting.DaftarPengajuanTransfer dpt) {
		if (dpt == null || dpt.getProsesTransfer() == null
				|| dpt.getProsesTransfer().getCaraPembayaranTransfer() == null) {
			return null;
		}
		if (Boolean.TRUE.equals(dpt.getTransitori())) {
			return dpt.getProsesTransfer().getCaraPembayaranTransfer().getAkunTransitori();
		}
		if (Boolean.TRUE.equals(dpt.getTransfer())) {
			return dpt.getProsesTransfer().getCaraPembayaranTransfer().getAkun();
		}
		return null;
	}

	/**
	 * Penyaring dokumen yang layak diposting: sudah disetujui, nilainya tidak nol,
	 * transfernya sudah diproses, dan berada dalam rentang tanggal persetujuan.
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai, java.util.Date sampai) {
		Criteria c = session.createCriteria(UangMuka.class)
				.createAlias("daftarPengajuanTransfer", "dpt")
				.add(Restrictions.isNotNull("dpt.prosesTransfer"))
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA uang muka terposting dalam rentang: hapus jurnal yang belum
	 * closing, lalu lepas penanda postingnya. Transaksinya dibuka sendiri karena dipanggil
	 * dari API, di mana tidak ada kerangka ZK yang meng-commit-kan sesi berjalan.
	 *
	 * @return jumlah dokumen yang berhasil dibatalkan postingnya.
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<UangMuka> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (UangMuka dok : daftar) {
				try {
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where uang_muka=" + dok.getId()
							+ "  and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where uang_muka="
							+ dok.getId() + " and closing is null").executeUpdate();
					dok.setPostingHistory(null);
					session.update(dok);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					Common.tampilErrorJikaAdmin(e);
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

	/**
	 * Posting SEMUA uang muka yang belum diposting dalam rentang. Pasangan akunnya sama
	 * dengan layar: debet ke akun penerima (jenis uang muka), kredit ke akun cara pembayaran transfernya.
	 *
	 * @return jumlah dokumen yang berhasil diposting.
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PERSETUJUAN_UANG_MUKA);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal uang muka dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			List<UangMuka> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).list();

			for (UangMuka dok : daftar) {
				if (dok == null) {
					continue;
				}
				try {
					Akun akunDebet = dok.getJenisUangMuka() == null ? null : dok.getJenisUangMuka().getAkun();
					Akun akunKredit = akunKreditTransfer(dok.getDaftarPengajuanTransfer());
					if (akunDebet == null || akunKredit == null) {
						continue;
					}
					Double nilai = dok.getNilai();
					if (nilai == null || nilai <= 0.1) {
						continue;
					}

					String ket = "Persetujuan uang muka \"" + dok.getKode() + "\" senilai "
							+ Common.numberFormat.get().format(nilai);

					session.getTransaction().begin();
					CommonAkunting.saveTransaksi(akunDebet, akunKredit, null, null, postingHistory, true, ket,
							dok.getTanggalPersetujuan(), nilai, 0.0, dok, dok.getSatuanKerja(), session);
					dok.setPostingHistory(postingHistory);
					session.update(dok);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
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


}
