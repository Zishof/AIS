package ais.action.master.akunting;

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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.JenisKasKecil;
import ais.database.model.akunting.KasKecil;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>JenisKasKecilAction — Pengelola Master Jenis Kas Kecil (Petty Cash)</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Controller ZK berbasis {@link GenericAutowireComposer} yang menangani manajemen
 * data master {@link JenisKasKecil}. Jenis Kas Kecil (Petty Cash) adalah dana tunai
 * kecil yang dialokasikan ke unit kerja tertentu untuk keperluan operasional sehari-hari
 * yang nilainya tidak cukup besar untuk melalui proses transfer bank normal. Setiap
 * jenis kas kecil memiliki akun buku besar kas kecil yang terhubung, akun penutup
 * opsional (digunakan saat kas kecil di-reset), saldo awal, tanggal saldo awal, dan
 * satuan kerja pemilik. Controller ini memungkinkan bagian keuangan mendefinisikan
 * satu atau beberapa jenis kas kecil per satuan kerja (misalnya Kas Kecil Operasional,
 * Kas Kecil ATK, dll.) dengan saldo yang dipantau secara real-time melalui metode
 * statik {@code hitungSaldo}.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Komponen ZUL menggunakan autowiring ZK untuk menyuntikkan grid, paging, field
 * pencarian, dan popup window ke field-field kelas. Inisialisasi di {@code doAfterCompose}
 * menyiapkan filter satuan kerja, paging, dan timer refresh. Formulir tambah/ubah
 * dibangun secara programatis di metode {@code init(JenisKasKecil)} dengan baris:
 * Satuan Kerja (wajib), Kode, Nama, Tanggal Saldo Awal, Akun Kas Kecil (wajib),
 * Akun Penutup (opsional), Saldo Awal (wajib), dan Keterangan. Setelah data tersimpan,
 * metode {@code onSave} secara asinkron memastikan ada {@link DaftarPengajuanTransfer}
 * yang terhubung dengan jenis kas kecil ini (via timer), dan me-reload cache statis
 * kas kecil default via {@link JenisKasKecil#reloadDefault()}.</p>
 *
 * <p><b>Threading:</b><br>
 * Metode {@code hitungSaldo} menggunakan {@link HibernateUtil#currentNativeSession()}
 * (sesi native terpisah) dan secara eksplisit menutupnya setelah selesai — berbeda
 * dari pola sesi terkelola normal. Ini disengaja karena kalkulasi saldo mungkin
 * dipanggil dari konteks yang tidak memiliki sesi terkelola aktif.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Jika ada field baru pada {@link JenisKasKecil}, perbarui metode {@code init}
 * untuk formulir dan metode {@code onSave} untuk penyimpanan. Renderer
 * {@code JenisKasKecilRenderer} juga perlu diperbarui agar kolom baru tampil
 * di grid. Metode {@code hitungSaldo} menggunakan LEFT JOIN ke
 * {@code penggantianKasKecil} untuk mengecualikan kas kecil yang sudah diganti —
 * perubahan pada relasi ini mempengaruhi akurasi kalkulasi saldo.</p>
 *
 * @see JenisKasKecil
 * @see KasKecil
 * @see DaftarPengajuanTransfer
 */
public class JenisKasKecilAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * Nomor versi serialisasi untuk kompatibilitas mekanisme serialisasi Java.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	/** Popup window untuk formulir tambah/ubah jenis kas kecil. */
	private MyWindow addWindow;

	/** Komponen paging untuk navigasi halaman daftar jenis kas kecil. */
	private Paging paging;

	/** Grid utama penampil daftar {@link JenisKasKecil}. */
	private MyGrid grid;

	/** Field teks pencarian berdasarkan nama jenis kas kecil. */
	private Textbox serachnama;

	/** Field teks pencarian berdasarkan kode jenis kas kecil. */
	private Textbox serachkode;

	/** Checkbox filter untuk menampilkan hanya yang aktif atau semua. */
	private Checkbox searchaktif;

	/** Input nama jenis kas kecil pada formulir tambah/ubah. */
	private Textbox nama;

	/** Input kode jenis kas kecil pada formulir tambah/ubah. */
	private Textbox kode;

	/** Input keterangan/deskripsi jenis kas kecil pada formulir. */
	private Textbox keterangan;

	/** Banbox pemilih akun kas kecil utama pada formulir (wajib). */
	private AmbilDataAkunBanbox akun;

	/** Objek {@link JenisKasKecil} yang sedang aktif di formulir. */
	public JenisKasKecil jenisKasKecil;

	/** Tombol tambah data baru; visibilitasnya bergantung pada hak CREATE. */
	private MyToolbarbuttonConfig add;

	/** Apakah pengguna memiliki hak UPDATE. */
	private boolean edit;

	/** Apakah pengguna memiliki hak DELETE. */
	private boolean delete;

	/** Banbox pemilih satuan kerja pada formulir tambah/ubah. */
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	/** Model pohon satuan kerja untuk navigasi hierarki filter unit kerja. */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/** Banbox pemilih satuan kerja sebagai filter pada halaman daftar. */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/** Input saldo awal kas kecil pada formulir tambah/ubah. */
	private MyDoublebox saldoAwal;

	/** Input tanggal saldo awal kas kecil pada formulir tambah/ubah. */
	private MyDatebox tanggal;

	/** Banbox pemilih akun penutup kas kecil (opsional) pada formulir. */
	private AmbilDataAkunBanbox akunPenutupKasKecil;

	/**
	 * Dipanggil ZK sebelum komponen halaman dibangun. Memverifikasi hak akses
	 * keamanan melalui {@link Common#doCheckSecurity()}.
	 *
	 * <p><b>Tujuan:</b> Gerbang keamanan awal yang mencegah halaman dimuat jika
	 * sesi atau hak akses pengguna tidak valid untuk modul kas kecil ini.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} untuk
	 * validasi sesi dan modul, lalu mendelegasikan ke super.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika akses tidak sah, proses dihentikan dan
	 * pengguna diarahkan ke halaman logoff.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada kebutuhan keamanan
	 * khusus tambahan sebelum komponen dibangun.</p>
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
	 * halaman master jenis kas kecil: validasi sesi, bahasa, filter satuan kerja,
	 * paging, timer refresh, dan hak akses CRUD.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan halaman daftar jenis kas kecil secara lengkap
	 * agar siap digunakan pengguna untuk mengelola master petty cash organisasi.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil super dan {@link Common#initLaguage()}.</li>
	 *   <li>Validasi sesi dan hak READ; jika tidak ada, logout.</li>
	 *   <li>Mendaftarkan listener pada {@code searchparent} yang memicu pencarian
	 *       ulang saat filter satuan kerja berubah.</li>
	 *   <li>Menginisialisasi {@link SatuanKerjaTreeModel} (tanpa node root).</li>
	 *   <li>Mengatur visibilitas tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Menyimpan flag edit dan delete berdasarkan hak UPDATE/DELETE.</li>
	 *   <li>Menginisialisasi paging dengan listener onPaging → onSearchDefault.</li>
	 *   <li>Membuat timer default yang memuat data pada tick pertama.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception disebarkan ke ZK. Sesi tidak valid
	 * menyebabkan redirect ke halaman logoff.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada filter baru (misalnya filter berdasarkan
	 * rentang tanggal saldo awal), tambahkan inisialisasinya di sini.</p>
	 *
	 * @param comp komponen root halaman ZK
	 * @throws Exception jika terjadi kesalahan saat inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		}

		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	/**
	 * Menghitung saldo kas kecil yang tersedia untuk jenis kas kecil tertentu pada
	 * tanggal yang ditentukan, dengan memperhitungkan semua pengeluaran yang belum
	 * diganti (penggantianKasKecil masih null).
	 *
	 * <p><b>Tujuan:</b> Menyediakan informasi saldo kas kecil real-time yang akurat
	 * untuk digunakan saat pengguna mengajukan penggunaan kas kecil baru. Sistem
	 * perlu mengetahui apakah saldo masih mencukupi sebelum pengajuan disetujui.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengembalikan 0.0 jika parameter {@code jenisKasKecil} null.</li>
	 *   <li>Mengambil saldo awal dari entitas {@link JenisKasKecil}.</li>
	 *   <li>Membuka sesi native Hibernate untuk query aggregate.</li>
	 *   <li>Membangun filter tanggal: menambahkan 1 menit ke parameter tanggal
	 *       untuk memastikan semua transaksi pada tanggal tersebut ikut terhitung.</li>
	 *   <li>Menjumlahkan semua nilai {@link KasKecil} dengan LEFT JOIN ke
	 *       {@code penggantianKasKecil} dan filter:
	 *     <ul>
	 *       <li>penggantianKasKecil.tanggalPersetujuan IS NULL (belum diganti).</li>
	 *       <li>Bukan record dengan id bukanId (untuk validasi saat edit).</li>
	 *       <li>jenisKasKecil sesuai parameter.</li>
	 *       <li>tanggal_pengajuan sebelum atau sama dengan tanggal+1menit.</li>
	 *       <li>Status aktif (aktif null atau true).</li>
	 *     </ul>
	 *   </li>
	 *   <li>Menutup sesi native secara eksplisit dan memanggil
	 *       {@link HibernateUtil#closeSession()}.</li>
	 *   <li>Mengembalikan saldoAwal - jumlah terpakai.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke caller.
	 * Nilai null dari sum aggregate (tidak ada record) ditangani dengan default 0.0.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini menggunakan sesi native yang HARUS ditutup
	 * secara eksplisit — ini adalah pengecualian dari pola sesi terkelola. Perubahan
	 * pada relasi KasKecil.penggantianKasKecil akan mempengaruhi akurasi kalkulasi ini.</p>
	 *
	 * @param bukanId id record {@link KasKecil} yang dikecualikan dari perhitungan
	 *                (null jika tambah baru, id jika edit existing)
	 * @param jenisKasKecil jenis kas kecil yang saldonya akan dihitung
	 * @param tanggal tanggal batas akhir penghitungan pengeluaran
	 * @return saldo kas kecil tersisa dalam satuan rupiah (double)
	 */
	public static Double hitungSaldo(Long bukanId, JenisKasKecil jenisKasKecil, Date tanggal) {

		if (jenisKasKecil == null) {
			return 0.0;
		}

		Double saldoAwal = jenisKasKecil == null ? 0.0 : jenisKasKecil.getSaldoAwal();
		Session session = HibernateUtil.currentNativeSession();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal);
		calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) + 1);
		String sqlRes = "tanggal_pengajuan <= '" + Common.databaseDateFormat1.get().format(calendar.getTime()) + "'";

		Number terpakai = ((Number) session.createCriteria(KasKecil.class)
				.createAlias("penggantianKasKecil", "penggantianKasKecil", Criteria.LEFT_JOIN)
				.add(Restrictions.isNull("penggantianKasKecil.tanggalPersetujuan"))

				.add(bukanId != null ? Restrictions.ne("id", bukanId) : Restrictions.sqlRestriction("true"))
				.add(Restrictions.eq("jenisKasKecil", jenisKasKecil)).add(Restrictions.sqlRestriction(sqlRes))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.sum("nilai")).uniqueResult());

		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		Double saldo = saldoAwal - (terpakai == null ? 0.0 : terpakai.doubleValue());

		System.out.println("saldoAwal -> " + saldoAwal + ", terpakai -> " + terpakai + ", saldo -> " + saldo);
		return saldo;
	}

	/**
	 * Inner class renderer yang mengisi setiap baris grid dengan data dari satu
	 * objek {@link JenisKasKecil}, termasuk checkbox interaktif "Aktif" yang
	 * dapat diubah langsung dari grid.
	 *
	 * <p><b>Tujuan:</b> Menampilkan data jenis kas kecil secara informatif di grid
	 * dengan kolom: Kode (dengan link revisi), Nama, Keterangan, Akun Kas Kecil,
	 * Akun Penutup, Tanggal Saldo Awal, Saldo Awal, Satuan Kerja, checkbox Aktif,
	 * dan tombol Ubah/Hapus.</p>
	 *
	 * <p><b>Cara kerja:</b> ZK memanggil {@code render} untuk setiap elemen model.
	 * Akun dan akun penutup ditampilkan dalam format "kode-nama". Tanggal diformat
	 * menggunakan {@link Common#dateFormat}. Saldo diformat dengan {@link Common#numberFormat}.
	 * Checkbox "Aktif" menyimpan perubahan langsung ke database.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pastikan urutan appendChild sesuai urutan kolom ZUL.
	 * Jika ada kolom baru, tambahkan di sini dan di file ZUL secara bersamaan.</p>
	 */
	class JenisKasKecilRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi satu baris grid dengan data dari satu objek {@link JenisKasKecil}.
		 *
		 * <p><b>Tujuan:</b> Menampilkan informasi lengkap jenis kas kecil dalam
		 * baris grid yang kompak dan informatif, dengan checkbox aktif yang dapat
		 * diubah langsung tanpa membuka formulir edit.</p>
		 *
		 * <p><b>Cara kerja:</b>
		 * <ol>
		 *   <li>Menampilkan kode via RevisiHelper (dengan link audit trail).</li>
		 *   <li>Menampilkan nama dan keterangan sebagai Label.</li>
		 *   <li>Menampilkan info akun kas kecil dalam format "kode-nama" via
		 *       MyLabelAgakKecil untuk ukuran teks yang lebih kecil.</li>
		 *   <li>Menampilkan info akun penutup kas kecil (opsional, bisa kosong).</li>
		 *   <li>Menampilkan tanggal saldo awal yang diformat.</li>
		 *   <li>Menampilkan saldo awal yang diformat dengan angka.</li>
		 *   <li>Menampilkan satuan kerja dalam format "kode-nama".</li>
		 *   <li>Checkbox "Aktif": saat diubah, langsung menyimpan ke database
		 *       via {@link Common#refreshSaveOrUpdate}; dinonaktifkan (disabled) dan
		 *       listener-nya menolak penyimpanan bagi pengguna tanpa hak UPDATE
		 *       (flag {@code edit}), konsisten dengan tombol Ubah/Hapus di baris yang sama.</li>
		 *   <li>Tombol Ubah/Hapus via {@link Common#copyEditDeleteButtons}.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b> Exception disebarkan ke ZK. Nilai null
		 * pada field entitas ditangani dengan operator ternary untuk menghindari NPE.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Pastikan urutan appendChild konsisten dengan
		 * urutan kolom yang didefinisikan di file ZUL jenis_kas_kecil.zul.</p>
		 *
		 * @param arg0 baris grid ZK yang akan diisi
		 * @param arg1 objek data yang akan dicast ke {@link JenisKasKecil}
		 * @throws Exception jika terjadi kesalahan rendering atau penyimpanan
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JenisKasKecil jenisKasKecil = (JenisKasKecil) arg1;

			RevisiHelper
					.createNewRevisi(JenisKasKecil.class, jenisKasKecil,
							jenisKasKecil.getKode() == null ? "" : jenisKasKecil.getKode().trim().toString())
					.setParent(arg0);
			new Label(jenisKasKecil.getNama()).setParent(arg0);
			new Label(jenisKasKecil.getKeterangan()).setParent(arg0);

			new MyLabelAgakKecil(jenisKasKecil.getAkun() == null ? ""
					: jenisKasKecil.getAkun().getKode() + "-" + jenisKasKecil.getAkun().getNama()).setParent(arg0);

			new MyLabelAgakKecil(jenisKasKecil.getAkunPenutupKasKecil() == null ? ""
					: jenisKasKecil.getAkunPenutupKasKecil().getKode() + "-"
							+ jenisKasKecil.getAkunPenutupKasKecil().getNama())
					.setParent(arg0);

			new Label(Common.dateFormat.get().format(jenisKasKecil.getTanggal())).setParent(arg0);

			new Label(Common.numberFormat.get().format(jenisKasKecil.getSaldoAwal())).setParent(arg0);

			new Label(jenisKasKecil.getSatuanKerja() == null ? ""
					: jenisKasKecil.getSatuanKerja().getKode() + "-" + jenisKasKecil.getSatuanKerja().getNama())
					.setParent(arg0);

			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
			aktif.setDisabled(!edit);
			aktif.setChecked(jenisKasKecil.getAktif());
			aktif.setParent(arg0);
			aktif.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (!edit) {
						return;
					}
					jenisKasKecil.setAktif(aktif.isChecked());
					Common.refreshSaveOrUpdate(jenisKasKecil);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisKasKecil, JenisKasKecilAction.this).setParent(arg0);

		}

	}

	/**
	 * Implementasi antarmuka {@link DataInitDefault} yang memungkinkan controller lain
	 * membuka formulir edit jenis kas kecil secara programatis, misalnya dari
	 * tombol Ubah pada baris grid yang menggunakan pola generik AIS.
	 *
	 * <p><b>Tujuan:</b> Menyediakan titik masuk standar yang kompatibel dengan pola
	 * tombol Ubah generik ({@link Common#copyEditDeleteButtons}) di seluruh modul AIS.
	 * Dengan mengimplementasikan {@link DataInitDefault}, kelas ini dapat digunakan
	 * sebagai target dari tombol Ubah generik.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengcast {@link GeneralValueObject} ke {@link JenisKasKecil},
	 * memanggil {@code init(JenisKasKecil)} untuk membangun formulir edit, kemudian
	 * menampilkan window dalam mode modal.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception disebarkan ke ZK untuk ditampilkan
	 * sebagai error dialog.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jangan mengubah signature; ini adalah kontrak dari
	 * interface {@link DataInitDefault}.</p>
	 *
	 * @param obj objek {@link GeneralValueObject} yang dicast ke {@link JenisKasKecil}
	 * @throws Exception jika terjadi kesalahan saat membangun formulir atau modal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisKasKecil = (JenisKasKecil) obj;
		init(jenisKasKecil);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menangani klik tombol Tambah pada toolbar. Membuat objek {@link JenisKasKecil}
	 * baru dan membuka formulir dalam mode modal.
	 *
	 * <p><b>Tujuan:</b> Menyediakan titik masuk bagi pengguna yang ingin menambahkan
	 * jenis kas kecil baru ke master data keuangan organisasi.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code init(new JenisKasKecil())} untuk
	 * membangun formulir kosong, kemudian menampilkan {@code addWindow} dalam mode
	 * modal. Exception dari {@code onModal()} ditangkap dan ditampilkan sebagai
	 * info error untuk admin menggunakan {@link Common#tampilErrorJikaAdmin}.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari {@code onModal} ditangkap dan
	 * dilaporkan melalui {@link Common#tampilErrorJikaAdmin} agar tidak membuat
	 * halaman mati untuk pengguna biasa.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada nilai default yang perlu diisi saat tambah
	 * baru (misalnya satuan kerja dari konteks pengguna), lakukan setelah {@code init}.</p>
	 *
	 * @param event event klik ZK dari tombol Tambah
	 * @throws Exception jika terjadi kesalahan saat membangun formulir
	 */
	public void onAdd(Event event) throws Exception {
		init(new JenisKasKecil());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun konten formulir tambah/ubah di dalam {@code addWindow} berdasarkan
	 * objek {@link JenisKasKecil} yang diberikan. Dipanggil baik untuk mode tambah
	 * maupun mode ubah.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan formulir interaktif yang memungkinkan pengguna
	 * mengisi atau mengubah data jenis kas kecil. Formulir dibangun secara programatis
	 * menggunakan komponen ZK agar mudah dikontrol dari Java tanpa bergantung pada
	 * ZUL statis.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengatur judul window: "Tambah Jenis Kas Kecil" atau "Ubah Jenis Kas Kecil".</li>
	 *   <li>Menyimpan referensi ke field instance {@code jenisKasKecil}.</li>
	 *   <li>Membersihkan konten window sebelumnya.</li>
	 *   <li>Membangun Borderlayout: Center berisi grid formulir, South berisi toolbar.</li>
	 *   <li>Menghitung set satuan kerja yang tersedia berdasarkan perguruan tinggi
	 *       dan hierarki satuan kerja untuk filter dropdown.</li>
	 *   <li>Mencoba mengisi default satuan kerja dari konteks pengguna jika belum ada.</li>
	 *   <li>Menambahkan baris formulir: Satuan Kerja (wajib), Kode, Nama, Tanggal
	 *       Saldo Awal (wajib, readonly popup), Akun Kas Kecil (wajib), Akun Penutup
	 *       (opsional), Saldo Awal (wajib doublebox), Keterangan (textarea 3 baris).</li>
	 *   <li>Tombol Batal menutup window; Simpan memanggil {@code onSave}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception saat membangun komponen disebarkan ke ZK.
	 * Exception saat mengisi satuan kerja default ditangkap dan diabaikan (try-catch)
	 * agar tidak menghalangi pembukaan formulir.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field baru pada {@link JenisKasKecil}, tambahkan
	 * MyFormRow yang sesuai. Perhatikan bahwa akun disimpan via setAttribute/getAttribute
	 * mengikuti pola {@link AmbilDataAkunBanbox}.</p>
	 *
	 * @param jenisKasKecil objek yang akan ditampilkan/diedit; null-id = mode tambah
	 * @throws Exception jika terjadi kesalahan saat membangun komponen formulir
	 */
	private void init(JenisKasKecil jenisKasKecil) throws Exception {
		addWindow.setTitle(jenisKasKecil.getId() == null ? "Tambah Jenis Kas Kecil" : "Ubah Jenis Kas Kecil");
		this.jenisKasKecil = jenisKasKecil;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
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

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		try {
			if (jenisKasKecil.getSatuanKerja() == null) {
				jenisKasKecil.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/JenisKasKecilAction.java:582");
			// tidak perlu ditangani; satuan kerja default cukup dibiarkan null
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(jenisKasKecil.getSatuanKerja() == null ? "" : jenisKasKecil.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", jenisKasKecil.getSatuanKerja());

		row.appendChild(satuanKerja);

		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		row.appendChild(kode = new Textbox((jenisKasKecil.getKode() == null ? "" : jenisKasKecil.getKode().trim())));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(jenisKasKecil.getNama() == null ? "" : jenisKasKecil.getNama().trim()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Saldo Awal *"));
		row.appendChild(tanggal = new MyDatebox(jenisKasKecil.getTanggal()));
		tanggal.setFormat(Common.dateFormat.get().toPattern());
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Kas Kecil *"));
		row.appendChild(akun = new AmbilDataAkunBanbox(false));
		akun.setValue(jenisKasKecil.getAkun() == null ? "" : jenisKasKecil.getAkun().getNama());
		akun.setAttribute("akun", jenisKasKecil.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Penutup Kas Kecil"));
		row.appendChild(akunPenutupKasKecil = new AmbilDataAkunBanbox(false));
		akunPenutupKasKecil.setValue(
				jenisKasKecil.getAkunPenutupKasKecil() == null ? "" : jenisKasKecil.getAkunPenutupKasKecil().getNama());
		akunPenutupKasKecil.setAttribute("akun", jenisKasKecil.getAkunPenutupKasKecil());
		akunPenutupKasKecil.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saldo Awal Kas Kecil *"));
		row.appendChild(saldoAwal = new MyDoublebox(jenisKasKecil.getSaldoAwal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jenisKasKecil.getKeterangan() == null ? "" : jenisKasKecil.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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

	}

	/**
	 * Memvalidasi dan menyimpan data jenis kas kecil dari formulir ke database.
	 * Setelah berhasil menyimpan, secara asinkron memastikan ada DaftarPengajuanTransfer
	 * yang terhubung dan me-reload cache kas kecil default.
	 *
	 * <p><b>Tujuan:</b> Melakukan validasi berlapis pada input pengguna sebelum
	 * menyimpan atau memperbarui entitas {@link JenisKasKecil}. Mengembalikan
	 * {@code true} jika berhasil, {@code false} jika validasi gagal.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Validasi satuan kerja tidak boleh null.</li>
	 *   <li>Validasi nama tidak boleh kosong.</li>
	 *   <li>Validasi akun kas kecil harus dipilih.</li>
	 *   <li>Validasi saldo awal tidak boleh null.</li>
	 *   <li>Jika mode ubah, reload entitas dari sesi Hibernate.</li>
	 *   <li>Mengisi semua field entitas dari input formulir.</li>
	 *   <li>Menyimpan via {@link Common#refreshSaveOrUpdate} dan flush.</li>
	 *   <li>Membuat timer asinkron yang memastikan ada DaftarPengajuanTransfer
	 *       untuk kas kecil ini (jika belum ada, dibuat secara otomatis).</li>
	 *   <li>Memanggil {@link JenisKasKecil#reloadDefault()} untuk memperbarui
	 *       cache statis kas kecil default di seluruh aplikasi.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke ZK. Operasi
	 * pembuatan DaftarPengajuanTransfer bersifat fire-and-forget melalui timer.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field wajib baru, tambahkan validasi sebelum
	 * blok penyimpanan. Logika pembuatan otomatis DaftarPengajuanTransfer penting
	 * untuk alur transfer kas kecil — jangan dihapus.</p>
	 *
	 * @param event event ZK dari tombol Simpan (bisa null jika dipanggil programatis)
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan database saat penyimpanan
	 */
	public boolean onSave(Event event) throws Exception {
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Satuan Kerja dari field pencarian yang tersedia; (2) Pastikan data Satuan Kerja sudah tercatat di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nama dengan nama kas kecil yang sesuai; (2) Pastikan nama tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun kas melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (saldoAwal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Saldo Awal belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Saldo dengan nilai saldo awal kas kecil; (2) Pastikan saldo tidak kosong dan bernilai valid; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisKasKecil.getId() != null) {
			jenisKasKecil = (JenisKasKecil) session.load(JenisKasKecil.class, jenisKasKecil.getId());
		}
		jenisKasKecil.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		jenisKasKecil.setAkun((Akun) akun.getAttribute("akun"));
		jenisKasKecil.setAkunPenutupKasKecil((Akun) akunPenutupKasKecil.getAttribute("akun"));
		jenisKasKecil.setTanggal(tanggal.getValue());
		jenisKasKecil.setKode(kode.getValue());
		jenisKasKecil.setNama(nama.getValue());
		jenisKasKecil.setSaldoAwal(saldoAwal.getValue());
		jenisKasKecil.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisKasKecil);
		session.flush();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jenisKasKecil.getDaftarPengajuanTransfer() == null) {
					DaftarPengajuanTransfer.simpanJenisKasKecil(jenisKasKecil);
				}
			}
		});

		JenisKasKecil.reloadDefault();
		return true;
	}

	/**
	 * Membangun objek Hibernate Criteria untuk query daftar {@link JenisKasKecil}
	 * berdasarkan filter aktif, dengan opsi pengurutan berdasarkan kode.
	 *
	 * <p><b>Tujuan:</b> Memusatkan logika filter query agar dapat digunakan baik
	 * untuk paging (tanpa order) maupun pengambilan data (dengan order), menghindari
	 * duplikasi kode filter.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mendapatkan dan memproses hierarki satuan kerja dari {@code searchparent}.</li>
	 *   <li>Membangun Criteria dengan filter:
	 *     <ul>
	 *       <li>Satuan kerja: null (global) ATAU dalam set satuan kerja pengguna.</li>
	 *       <li>Status aktif: hanya aktif jika checkbox aktif dicentang.</li>
	 *       <li>Kode: ILIKE jika tidak kosong.</li>
	 *       <li>Nama: ILIKE jika tidak kosong.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Jika {@code order} true, tambahkan ORDER BY kode ASC.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke caller.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada filter baru (misalnya filter berdasarkan
	 * akun), tambahkan klausa .add() di sini. Logika OR(isNull, in()) pada
	 * satuan kerja mengakomodasi data tanpa satuan kerja dan data per unit.</p>
	 *
	 * @param order jika true, tambahkan ORDER BY kode ASC ke query
	 * @return objek {@link Criteria} yang siap dieksekusi
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
		Criteria criteria = session.createCriteria(JenisKasKecil.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.asc("kode"));
		return criteria;
	}

	/**
	 * Menjalankan pencarian data jenis kas kecil dan memperbarui grid beserta
	 * paging dengan hasil yang ditemukan sesuai filter aktif.
	 *
	 * <p><b>Tujuan:</b> Handler utama event pencarian yang memuat data dari database
	 * dan menampilkannya di grid. Dipanggil saat filter berubah, paging dinavigasi,
	 * setelah operasi simpan/hapus, atau saat timer refresh berdetak.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghitung total record untuk paging via {@link Common#initPaging(Criteria, Paging)}.</li>
	 *   <li>Mengambil data halaman aktif dengan limit dan offset paging.</li>
	 *   <li>Membuat SimpleListModel dan menyetel renderer ke grid.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception Hibernate disebarkan ke ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada kebutuhan sorting
	 * alternatif atau pengelompokan hasil query.</p>
	 *
	 * @param event event ZK yang memicu pencarian; bisa null jika dipanggil programatis
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisKasKecil> jenisKasKecil = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(jenisKasKecil);
		grid.setRowRenderer(new JenisKasKecilRenderer());
		grid.setModelCheckMobile(strset);

	}

}
