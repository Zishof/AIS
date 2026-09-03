package ais.action.master.akunting;

import java.io.File;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akunting.LaporanPenggantianKasKecil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.PenggantianKasKecil;
import ais.database.model.akunting.UangMuka;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCheckboxConfigPilih;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>PenggantianKasKecilAction — Pengajuan dan Persetujuan Penggantian Kas Kecil</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini adalah Action ZK yang mengelola seluruh siklus hidup dokumen
 * Penggantian Kas Kecil (petty-cash replenishment). Modul ini dipakai oleh staf keuangan untuk
 * mengajukan permintaan penggantian dana kas kecil yang telah dipakai, serta oleh pejabat
 * berwenang untuk menyetujui atau menolak pengajuan tersebut. Setelah disetujui, sistem secara
 * otomatis membuat entri {@link DaftarPengajuanTransfer} agar dana dapat ditransfer kembali ke
 * rekening kas kecil yang bersangkutan.</p>
 *
 * <p><b>Cara kerja:</b> Kelas ini mengimplementasikan empat kontrak antarmuka ZK:
 * <ul>
 *   <li>{@code DataCriteria} — menyediakan {@code initCriteria()} untuk membangun query Hibernate
 *       dengan berbagai filter (tanggal, satuan kerja, status, kode, nama).</li>
 *   <li>{@code DataSearchDefault} — menyediakan {@code onSearchDefault()} yang mengisi grid
 *       utama dengan data hasil query berpaginasi.</li>
 *   <li>{@code DataInitDefault} — menyediakan {@code init(GeneralValueObject)} agar kelas ini
 *       dapat dipanggil dari luar (misalnya alur SOP) untuk membuka form edit/lihat.</li>
 *   <li>{@code FormSop} — menyediakan {@code form()} agar form ini dapat dilekatkan dalam
 *       jendela disposisi SOP tanpa harus membuka window terpisah.</li>
 * </ul>
 * Saat halaman dimuat, {@code doAfterCompose()} menginisialisasi filter tanggal (6 bulan ke
 * belakang hingga besok), mengisi combobox status, mengatur hak akses (CREATE/UPDATE/DELETE),
 * mendaftarkan listener paging, serta menambahkan tombol ekspor dan "Hitung Ulang". Tombol
 * "Hitung Ulang" melakukan rekonsiliasi massal: memeriksa setiap {@link KasKecil} dan
 * memperbaiki saldo yang tidak sinkron, lalu membuat {@link DaftarPengajuanTransfer} untuk
 * pengajuan yang sudah disetujui namun belum memiliki entri transfer.</p>
 *
 * <p><b>Mode persetujuan:</b> Konstruktor {@code PenggantianKasKecilAction(boolean persetujuan)}
 * mengaktifkan mode persetujuan. Dalam mode ini tombol "Tambah" disembunyikan, kolom "Aktif"
 * diganti menjadi checkbox yang bisa diubah pejabat, dan tombol Ubah/Hapus dinonaktifkan untuk
 * data yang sudah disetujui.</p>
 *
 * <p><b>Alur data formula:</b> Rincian pengeluaran kas kecil disimpan dalam kolom {@code formula}
 * di entitas {@link KasKecil} sebagai JSON array. Setiap elemen berisi {@code key} (ID unik baris),
 * {@code akun} (ID {@link Akun}), {@code jumlah}, dan metadata lain. Metode
 * {@code KasKecilAction.reloadFormula()} merender array ini menjadi baris-baris formulir
 * yang dapat diedit pengguna.</p>
 *
 * <p><b>Threading:</b> Seluruh operasi DB berjalan di thread ZK event-dispatcher (single-thread
 * per sesi). Beberapa operasi async (cetak, simpan DaftarPengajuanTransfer) dijadwalkan via
 * {@code Common.createDefaultTimer()} agar tidak memblokir respons UI. Tidak ada state bersama
 * antar sesi sehingga tidak diperlukan sinkronisasi.</p>
 *
 * <p><b>Pemeliharaan:</b> Jika skema {@link PenggantianKasKecil} berubah (misalnya kolom baru),
 * perbarui array {@code contents} di {@code doAfterCompose()} agar ekspor Excel/PDF ikut
 * menyertakan kolom baru. Jika format nomor surat berubah, cukup ubah konfigurasi
 * {@link NomorSuratAlurKeuangan#PENGGANTIAN_KAS_KECIL_DATA}. Pastikan {@code KodeUnikUtil}
 * tersedia agar kode tidak duplikat saat banyak pengguna menyimpan bersamaan.</p>
 *
 * @author AIS
 * @see PenggantianKasKecil
 * @see KasKecil
 * @see DaftarPengajuanTransfer
 */
public class PenggantianKasKecilAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * ID serialisasi versi kelas. Wajib ada karena kelas ini mengimplementasikan
	 * {@code Serializable} secara tidak langsung melalui {@code GenericAutowireComposer}.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachkode;
	private Textbox serachjenis;
	private Checkbox searchaktif;
	private Combobox searchstatus;
	private MyDatebox start;
	private MyDatebox end;
	private Textbox nama;
	private Label kode;
	private Textbox keterangan;
	private Combobox kasKecil;

	public PenggantianKasKecil penggantianKasKecil;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private boolean persetujuan = false;

	private Tbmuser tbmuser;

	private Radiogroup status;

	private DisposisiSop disposisiSop = null;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean setujui = false;

	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Double nilai;
	private JSONArray array;

	private Row rowFormula;
	private Row rowRinci;
	protected LampiranLain lainMahasiswa;

	private boolean viewOnly = false;

	private MyDatebox tanggalPersetujuanManual;

	private Label nilaiHarusDikembalikan;

	private MyLabelConfig penutupan;

	/**
	 * Konstruktor default — dipakai saat kelas di-wire oleh ZK melalui file ZUL biasa.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi action dalam mode pengajuan (bukan persetujuan),
	 * sehingga pengguna dapat membuat dan mengedit pengajuan penggantian kas kecil baru.
	 * Field {@code tbmuser} diisi dengan pengguna sesi aktif agar bisa dipakai sebagai
	 * {@code dibuatOleh} ketika menyimpan data baru.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Common.getCurrentUser()} untuk mendapatkan
	 * objek {@link Tbmuser} yang sedang login. Field {@code persetujuan} tetap {@code false}
	 * (nilai default field), artinya form akan ditampilkan dalam mode pengajuan penuh.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada inisialisasi tambahan yang harus dilakukan sebelum
	 * ZK autowire, tambahkan di sini; jangan di {@code doAfterCompose} karena komponen
	 * belum tersedia saat konstruktor berjalan.</p>
	 */
	public PenggantianKasKecilAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Konstruktor mode persetujuan — dipakai ketika action dibuat secara programatis
	 * oleh modul alur SOP atau menu persetujuan keuangan.
	 *
	 * <p><b>Tujuan:</b> Mengaktifkan mode persetujuan sehingga pejabat berwenang dapat
	 * melihat daftar pengajuan yang masuk, mengubah status menjadi "Disetujui" atau
	 * "Ditolak", dan mengaktifkan/menonaktifkan item tanpa bisa menambah atau menghapus
	 * pengajuan baru.</p>
	 *
	 * <p><b>Cara kerja:</b> Menyimpan nilai parameter {@code persetujuan} ke field instance,
	 * lalu mengambil pengguna aktif via {@code Common.getCurrentUser()}. Nilai {@code persetujuan}
	 * kemudian digunakan di {@code doAfterCompose()} untuk menyembunyikan tombol "Tambah",
	 * dan di {@code form()} untuk merender kolom dalam mode read-only serta menampilkan
	 * widget status persetujuan.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan;
	 *                    {@code false} setara dengan konstruktor default.
	 *
	 * <p><b>Pemeliharaan:</b> Konstruktor ini dipanggil dari kode SOP dan dari parameter
	 * URL {@code ?persetujuan=true}. Pastikan kedua jalur tetap konsisten jika logika
	 * mode persetujuan berubah.</p>
	 */
	public PenggantianKasKecilAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Hook ZK yang dipanggil sebelum komponen halaman di-compose, digunakan untuk
	 * memeriksa keamanan akses sebelum halaman dirender.
	 *
	 * <p><b>Tujuan:</b> Memastikan pengguna yang mengakses halaman ini memiliki sesi
	 * yang valid dan hak akses yang sesuai. Jika tidak, ZK akan melakukan redirect
	 * atau menampilkan pesan kesalahan sebelum komponen apapun dirender ke browser.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} yang memeriksa
	 * sesi aktif dan hak akses modul. Setelah pemeriksaan selesai, memanggil
	 * {@code super.doBeforeCompose()} agar proses composing ZK berlanjut normal.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param page     halaman ZK yang sedang di-compose
	 * @param parent   komponen induk dalam pohon komponen ZK
	 * @param compInfo metadata komponen dari file ZUL
	 * @return {@code ComponentInfo} dari superclass untuk melanjutkan proses composing
	 *
	 * <p><b>Penanganan error:</b> Jika keamanan gagal, {@code Common.doCheckSecurity()}
	 * melempar exception atau melakukan redirect; method ini tidak menangani exception
	 * secara eksplisit.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code super.doBeforeCompose()}
	 * karena ZK membutuhkannya untuk mendaftarkan variabel autowire.</p>
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Hook ZK yang dipanggil setelah semua komponen ZUL selesai di-compose dan di-wire
	 * ke field Java. Ini adalah titik inisialisasi utama halaman.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi seluruh komponen UI — filter tanggal, combobox
	 * status, hak akses tombol, paging, ekspor data, dan tombol "Hitung Ulang" —
	 * sehingga halaman siap digunakan pengguna segera setelah dimuat.</p>
	 *
	 * <p><b>Cara kerja langkah demi langkah:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} agar ZK menyelesaikan autowire
	 *       komponen ZUL ke field Java (misalnya {@code grid}, {@code paging}, dll.).</li>
	 *   <li>Memanggil {@code Common.initLaguage()} untuk menyetel locale tampilan.</li>
	 *   <li>Memeriksa sesi dan hak akses READ; jika gagal, memanggil {@code Common.goLogoff()}
	 *       dan langsung kembali.</li>
	 *   <li>Mendaftarkan {@code EventListener} pada {@code searchparent} (filter satuan kerja)
	 *       agar setiap perubahan memicu {@code onSearchDefault()}.</li>
	 *   <li>Membangun {@code SatuanKerjaTreeModel} untuk filter hierarki satuan kerja.</li>
	 *   <li>Menyetel filter tanggal: {@code start} = 6 bulan lalu, {@code end} = besok.
	 *       Keduanya read-only agar pengguna tidak mengetik langsung (harus pakai date-picker).</li>
	 *   <li>Mengisi combobox {@code searchstatus} dengan pilihan Semua / Pengajuan /
	 *       Disetujui / Ditolak, lalu memilih "Semua" sebagai default.</li>
	 *   <li>Membaca parameter URL {@code persetujuan} untuk override mode dari ZUL.</li>
	 *   <li>Menyetel visibilitas dan tooltip tombol {@code add} berdasarkan hak CREATE.</li>
	 *   <li>Membaca hak akses UPDATE dan DELETE ke field {@code edit} dan {@code delete}.</li>
	 *   <li>Menginisialisasi paging dengan listener yang memanggil ulang {@code onSearchDefault()}.</li>
	 *   <li>Membuat tombol ekspor cetak dan upload melalui {@code Common.cetakData()} dan
	 *       {@code Common.uploadData()}, lalu menambahkannya ke toolbar.</li>
	 *   <li>Jika mode persetujuan aktif, menyembunyikan tombol "Tambah"; jika tidak,
	 *       memberi label "Pengajuan Penggantian Kas Kecil Baru".</li>
	 *   <li>Membuat timer default yang memanggil {@code onSearchDefault()} untuk mengisi
	 *       grid segera setelah halaman selesai dirender.</li>
	 *   <li>Menambahkan tombol "Hitung Ulang" yang melakukan rekonsiliasi saldo dan
	 *       pembuatan {@link DaftarPengajuanTransfer} massal.</li>
	 *   <li>Memanggil {@code FilterLanjutHelper.setup(comp)} untuk mengaktifkan panel
	 *       filter lanjutan yang bisa disembunyikan/ditampilkan.</li>
	 * </ol></p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param comp komponen root ZK dari file ZUL (hasil composing)
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen
	 *
	 * <p><b>Penanganan error:</b> Jika sesi tidak valid atau hak READ tidak ada, method
	 * langsung return setelah {@code goLogoff()} sehingga tidak ada inisialisasi lebih lanjut.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada komponen ZUL baru (misalnya filter tambahan), tambahkan
	 * inisialisasinya di sini. Perhatikan bahwa komponen ZUL harus memiliki id yang sesuai
	 * dengan nama field Java agar autowire ZK bekerja.</p>
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (searchparent == null) return;
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Comboitem comboitemSemua = new Comboitem("Semua");
		if (comboitemSemua != null) { comboitemSemua.setValue(null); }
		searchstatus.appendChild(comboitemSemua);

		Comboitem comboitem = new Comboitem(PenggantianKasKecil.PENGAJUAN);
		if (comboitem != null) { comboitem.setValue(PenggantianKasKecil.PENGAJUAN); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(PenggantianKasKecil.DISETUJU);
		if (comboitem != null) { comboitem.setValue(PenggantianKasKecil.DISETUJU); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(PenggantianKasKecil.DITOLAK);
		if (comboitem != null) { comboitem.setValue(PenggantianKasKecil.DITOLAK); }
		searchstatus.appendChild(comboitem);

		if (searchstatus != null) { searchstatus.setSelectedItem(comboitemSemua); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		if (execution.getParameter("persetujuan") != null) {
			boolean persetujuanDariUrl = Boolean.parseBoolean(execution.getParameter("persetujuan"));
			// Parameter URL TIDAK BOLEH menaikkan mode dari pengajuan ke persetujuan --
			// hanya menu Persetujuan (konstruktor super(true), lihat
			// PersetujuanPenggantianKasKecilAction) atau hak APPROVE eksplisit pada menu
			// aktif yang boleh mengaktifkannya. Mencegah eskalasi via ?persetujuan=true di
			// menu Penggantian Kas Kecil biasa.
			persetujuan = persetujuanDariUrl
					? (persetujuan || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE))
					: false;
		}

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

		String[] contents = new String[] { "id", "kode", "nama", "keterangan", "kasKecil", "formula", "nilai",
				"dibuatOleh", "disetujuiOleh", "tanggalPembuatan", "tanggalPersetujuan", "status", "disposisiSop",
				"daftarPengajuanTransfer.prosesTransfer.kode", "daftarPengajuanTransfer.prosesTransfer.nama",
				"daftarPengajuanTransfer.prosesTransfer.tanggalPembuatan",
				"daftarPengajuanTransfer.prosesTransfer.disetujuiOleh",
				"daftarPengajuanTransfer.prosesTransfer.tanggalPersetujuan",
				"daftarPengajuanTransfer.prosesTransfer.realisasikanOleh",
				"daftarPengajuanTransfer.prosesTransfer.tanggalRealisasikan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PenggantianKasKecil.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenggantianKasKecil.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		// add bisa null bila pengguna tak punya hak tambah (Common.tambahData mengembalikan null).
		if (add != null) {
			if (persetujuan) {
				add.setVisible(false);
			} else {
				add.setLabel("Pengajuan Penggantian Kas Kecil Baru");
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						List<PenggantianKasKecil> penggantianKasKecils = initCriteria(false).addOrder(Order.asc("id"))
								.setMaxResults(5000).list();

						for (PenggantianKasKecil penggantianKasKecil : penggantianKasKecils) {
							KasKecil kasKecil = penggantianKasKecil.getKasKecil();
							Double saldo = JenisKasKecilAction.hitungSaldo(kasKecil.getId(),
									kasKecil.getJenisKasKecil(), kasKecil.getTanggal());

							if (saldo.intValue() != kasKecil.getSaldo().intValue()) {
								Session session = HibernateUtil.currentSession();
								session.refresh(kasKecil);
								kasKecil.setSaldo(saldo);
								Common.refreshUpdate(kasKecil);
							}

							if (penggantianKasKecil.getDisetujuiOleh() != null
									&& penggantianKasKecil.getDaftarPengajuanTransfer() == null) {
								if (penggantianKasKecil.getStatus().equals(UangMuka.DISETUJU)) {
									DaftarPengajuanTransfer.simpanPenggantianKasKecil(penggantianKasKecil);
								}
							}
						}

						onSearchDefault(null);
					}
				});

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer baris grid untuk menampilkan setiap entri {@link PenggantianKasKecil}
	 * dalam tabel utama halaman penggantian kas kecil.
	 *
	 * <p><b>Untuk apa:</b> Kelas inner ini bertanggung jawab mengubah objek domain
	 * {@link PenggantianKasKecil} menjadi baris komponen ZK yang kaya (label, link,
	 * checkbox, tombol aksi) di dalam grid utama.</p>
	 *
	 * <p><b>Cara kerja:</b> Method {@code render()} dipanggil oleh ZK untuk setiap
	 * baris model. Ia melakukan sinkronisasi saldo kas kecil, menentukan visibilitas
	 * tombol berdasarkan status dan mode persetujuan, lalu merender setiap sel
	 * dengan tipe komponen yang tepat. Jika ada DaftarPengajuanTransfer yang belum
	 * dibuat untuk pengajuan yang sudah disetujui, ia menjadwalkan pembuatannya
	 * secara async via timer.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika kolom grid di ZUL berubah urutan atau jumlahnya,
	 * pastikan urutan {@code setParent(arg0)} di method render juga diperbarui
	 * untuk menjaga keselarasan kolom.</p>
	 */
	class PenggantianKasKecilRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data {@link PenggantianKasKecil} ke dalam komponen ZK
		 * di dalam {@link Row} yang disediakan oleh framework ZK.
		 *
		 * <p><b>Tujuan:</b> Mengubah objek domain menjadi representasi visual yang
		 * informatif di grid, termasuk informasi kas kecil, nilai saldo, status
		 * persetujuan, tautan ke proses transfer, link SOP, serta tombol aksi
		 * (Ubah, Copy, Hapus, Cetak).</p>
		 *
		 * <p><b>Cara kerja langkah demi langkah:</b>
		 * <ol>
		 *   <li>Mengambil objek {@link KasKecil} terkait dari entitas penggantian.</li>
		 *   <li>Memanggil {@code JenisKasKecilAction.hitungSaldo()} untuk menghitung
		 *       saldo aktual kas kecil; jika berbeda dari yang tersimpan, memperbarui
		 *       saldo via {@code Common.refreshUpdate()}.</li>
		 *   <li>Memeriksa apakah {@code dibuatOleh} null; jika ya, mengisi dengan
		 *       pengguna aktif saat ini sebagai fallback.</li>
		 *   <li>Jika pengajuan sudah disetujui tetapi belum memiliki
		 *       {@link DaftarPengajuanTransfer}, menjadwalkan pembuatan entri transfer
		 *       via {@code Common.createDefaultTimer()}.</li>
		 *   <li>Membuat {@code Vbox} revisi via {@code RevisiHelper.createNewRevisi()},
		 *       lalu menambahkan link ke {@code ProsesTransfer} jika ada.</li>
		 *   <li>Menampilkan jenis kas kecil (kode + nama) di sel berikutnya.</li>
		 *   <li>Menambahkan widget upload/download bukti pengeluaran via
		 *       {@code LampiranLain.createDownloadUploadFileLain()}.</li>
		 *   <li>Menampilkan tanggal, pembuat, status, dan nama penyetuju.</li>
		 *   <li>Menghitung ulang nilai formula JSON dan memperbarui nilai kas kecil
		 *       jika ada ketidaksesuaian.</li>
		 *   <li>Menampilkan saldo, nilai penggantian, dan selisih saldo.</li>
		 *   <li>Menampilkan keterangan dan tautan SOP jika ada.</li>
		 *   <li>Menampilkan status transfer dari {@code DaftarPengajuanTransfer}.</li>
		 *   <li>Menampilkan checkbox "Aktif" (di mode persetujuan) atau label biasa.</li>
		 *   <li>Menambahkan tombol aksi (Ubah/Copy/Hapus dari {@code copyEditDeleteButtons}
		 *       dan tombol Cetak).</li>
		 * </ol></p>
		 *
		 * <p><b>Parameter:</b></p>
		 * @param arg0 baris ZK ({@link Row}) yang harus diisi komponen anak
		 * @param arg1 objek data — akan di-cast ke {@link PenggantianKasKecil}
		 * @throws Exception jika terjadi kesalahan saat merender atau mengakses DB
		 *
		 * <p><b>Penanganan error:</b> Error saat sinkronisasi saldo ditangkap dan
		 * ditampilkan hanya kepada admin via {@code Common.tampilErrorJikaAdmin()}.
		 * Ini memastikan pengguna biasa tetap melihat baris meskipun ada error parsial.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Jika ada kolom baru di grid ZUL, tambahkan sel
		 * baru di akhir method ini dengan urutan yang sama. Hati-hati dengan saldo
		 * yang dihitung ulang di setiap render — pertimbangkan caching jika performa
		 * menjadi masalah saat data banyak.</p>
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenggantianKasKecil penggantianKasKecil = (PenggantianKasKecil) arg1;
			KasKecil kasKecil = penggantianKasKecil.getKasKecil();
			try {

				Double saldo = JenisKasKecilAction.hitungSaldo(kasKecil.getId(), kasKecil.getJenisKasKecil(),
						kasKecil.getTanggal());

				if (saldo.intValue() != kasKecil.getSaldo().intValue()) {
					Session session = HibernateUtil.currentSession();
					session.refresh(kasKecil);
					kasKecil.setSaldo(saldo);
					Common.refreshUpdate(kasKecil);
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			if (penggantianKasKecil.getDibuatOleh() == null) {
				penggantianKasKecil.setDibuatOleh(tbmuser);
			}

			if (penggantianKasKecil.getDisetujuiOleh() != null
					&& penggantianKasKecil.getDaftarPengajuanTransfer() == null) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (penggantianKasKecil.getStatus().equals(UangMuka.DISETUJU)) {
							DaftarPengajuanTransfer.simpanPenggantianKasKecil(penggantianKasKecil);
						}

					}
				});
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PenggantianKasKecil.class, penggantianKasKecil,
					penggantianKasKecil.getKasKecil() == null ? ""
							: penggantianKasKecil.getKasKecil().getKode() + "-"
									+ penggantianKasKecil.getKasKecil().getNama()))
					.setParent(arg0);

			if (penggantianKasKecil.getDaftarPengajuanTransfer() != null
					&& penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A aaa = new A(penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, penggantianKasKecil.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(a);
			}

			new Label(penggantianKasKecil.getKasKecil().getJenisKasKecil() == null ? ""
					: penggantianKasKecil.getKasKecil().getJenisKasKecil().getKode() + "-"
							+ penggantianKasKecil.getKasKecil().getJenisKasKecil().getNama())
					.setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, kasKecil.getId(), KasKecil.class.getName(), "Bukti", false,
					null, null, false, false, false, true);

			a = new Vbox();
			a.setParent(arg0);
			new Label((penggantianKasKecil.getKasKecil().getTanggal() == null ? ""
					: Common.dateFormat3.get().format(penggantianKasKecil.getKasKecil().getTanggal()))).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(penggantianKasKecil.getDibuatOleh() == null ? ""
					: penggantianKasKecil.getDibuatOleh().getUserNama()).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(penggantianKasKecil.getStatus()).setParent(a);
			(new Label(penggantianKasKecil.getDisetujuiOleh() == null ? ""
					: penggantianKasKecil.getDisetujuiOleh().getUserNama())).setParent(a);

			if (penggantianKasKecil.getKasKecil() != null) {

				try {
					Double nilai = 0.0;
					JSONArray array = new JSONArray(penggantianKasKecil.getKasKecil().getFormula());
					for (int i = 0; i < array.length(); i++) {
						Double jumlah = 0.0;
						JSONObject jsonObject = array.getJSONObject(i);
						Long key = null;
						if (!jsonObject.isNull("key")) {
							key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
						}

						if (key != null) {
							if (!jsonObject.isNull("jumlah")) {
								jumlah = jsonObject.getDouble("jumlah");
							}
							nilai += jumlah;
						}
					}

					if (nilai.intValue() != penggantianKasKecil.getNilai().intValue()) {
						Session session = HibernateUtil.currentSession();
						KasKecil work = (KasKecil) session.createCriteria(KasKecil.class)
								.add(Restrictions.idEq(penggantianKasKecil.getKasKecil().getId())).uniqueResult();
						work.setNilai(nilai);
						session.update(work);
						session.flush();

						penggantianKasKecil.setKasKecil(work);
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}

			}

			new Label(Common.numberFormat.get().format(penggantianKasKecil.getSaldo())).setParent(arg0);
			new Label(Common.numberFormat.get().format(penggantianKasKecil.getNilai())).setParent(arg0);
			new Label(penggantianKasKecil.getSaldo() <= 0.0 ? ""
					: Common.numberFormat.get().format(penggantianKasKecil.getSaldo() - penggantianKasKecil.getNilai()))
					.setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new Label(Common.simpleString(penggantianKasKecil.getKeterangan())).setParent(vbox1);
			if (penggantianKasKecil.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + penggantianKasKecil.getDisposisiSop().getKeterangan() + " ("
						+ penggantianKasKecil.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(penggantianKasKecil.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			DaftarPengajuanTransfer daftarPengajuanTransfer = penggantianKasKecil.getDaftarPengajuanTransfer();

			DaftarPengajuanTransfer.tampilStatus(daftarPengajuanTransfer, vbox1);

			if (penggantianKasKecil.getDisposisiSop() != null && !penggantianKasKecil.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (persetujuan && !penggantianKasKecil.getStatus().equals(PenggantianKasKecil.DISETUJU)) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(penggantianKasKecil.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						penggantianKasKecil.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(penggantianKasKecil);
					}
				});
			} else {
				new Label(penggantianKasKecil.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit,
					!persetujuan && !penggantianKasKecil.getStatus().equals(PenggantianKasKecil.DISETUJU),
					delete && !persetujuan && !penggantianKasKecil.getStatus().equals(PenggantianKasKecil.DISETUJU),
					penggantianKasKecil, PenggantianKasKecilAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(penggantianKasKecil);
				}
			});
			button.setParent(hbx);
		}

	}

	/**
	 * Menghasilkan file PDF laporan penggantian kas kecil untuk keperluan ekspor massal
	 * (misalnya dari tombol ekspor di toolbar grid).
	 *
	 * <p><b>Tujuan:</b> Implementasi kontrak {@code DataCriteria.cetakData()} yang digunakan
	 * oleh mekanisme ekspor generik {@code Common.cetakData()} untuk menghasilkan file
	 * laporan dalam format PDF yang dapat diunduh pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance {@link LaporanPenggantianKasKecil} dengan
	 * data penggantian yang diberikan, mengkonfigurasi properti tampilan (judul, ukuran,
	 * visibilitas), lalu memanggil {@code Report.generateFileReport()} dengan template
	 * JasperReports {@code "akunting/penggantianKasKecil"} untuk menghasilkan file PDF
	 * yang dikembalikan sebagai {@link File}. File ini kemudian dikirim ke browser oleh
	 * infrastruktur ekspor generik.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param generalValueObject objek data yang akan dicetak; akan di-cast ke
	 *                           {@link PenggantianKasKecil}
	 * @return objek {@link File} yang merepresentasikan PDF yang dihasilkan
	 * @throws Exception jika template JasperReports tidak ditemukan, data tidak valid,
	 *                   atau terjadi kesalahan I/O saat menghasilkan file
	 *
	 * <p><b>Perbedaan dengan {@code cetak()}:</b> Method ini menghasilkan file untuk
	 * diunduh, sedangkan {@code cetak()} statik menampilkan laporan dalam window modal
	 * langsung di browser pengguna.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika nama template JasperReports berubah, perbarui string
	 * {@code "akunting/penggantianKasKecil"} di sini dan di {@code cetak()}.</p>
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PenggantianKasKecil penggantianKasKecil = (PenggantianKasKecil) generalValueObject;
		LaporanPenggantianKasKecil buktiPengeluaranKas = new LaporanPenggantianKasKecil(penggantianKasKecil);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setVisible(false);
		File file = Report.generateFileReport(Report.PDF, buktiPengeluaranKas.generateParameter(),
				"akunting/penggantianKasKecil", ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}

	/**
	 * Menampilkan laporan cetak penggantian kas kecil dalam window modal langsung di
	 * halaman yang sedang aktif, tanpa mengunduh file.
	 *
	 * <p><b>Tujuan:</b> Memberikan pratinjau cetak interaktif kepada pengguna segera
	 * setelah menyimpan pengajuan atau dari tombol cetak di baris grid, tanpa harus
	 * meninggalkan halaman saat ini.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance {@link LaporanPenggantianKasKecil} dengan
	 * data yang diberikan, mengkonfigurasi properti window (judul, ukuran, closable),
	 * menempelkan window ke root komponen halaman aktif via
	 * {@code ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()},
	 * lalu memanggil {@code onModal()} agar window tampil sebagai dialog modal.
	 * Pengguna dapat menutupnya dengan tombol silang (closable = true).</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param penggantianKasKecil objek yang akan dicetak; tidak boleh null dan harus
	 *                            memiliki relasi {@link KasKecil} yang sudah terisi
	 * @throws Exception jika template laporan tidak ditemukan atau terjadi kesalahan
	 *                   saat merender laporan
	 *
	 * <p><b>Thread safety:</b> Method ini harus dipanggil dari thread ZK event-dispatcher.
	 * Jika dipanggil dari timer atau thread lain, bungkus dengan
	 * {@code Common.createDefaultTimer()}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Method ini bersifat statik sehingga dapat dipanggil dari
	 * kelas lain (misalnya dari renderer baris). Pastikan {@code ExecutionsCtrl} tersedia
	 * di konteks pemanggil (harus ada request ZK yang aktif).</p>
	 */
	public static void cetak(PenggantianKasKecil penggantianKasKecil) throws Exception {
		LaporanPenggantianKasKecil buktiPengeluaranKas = new LaporanPenggantianKasKecil(penggantianKasKecil);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		buktiPengeluaranKas.onModal();
	}

	/**
	 * Implementasi kontrak {@code DataInitDefault} yang membuka window edit/lihat
	 * untuk objek {@link PenggantianKasKecil} yang diberikan dari luar kelas ini.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan sistem SOP dan modul lain untuk membuka form
	 * penggantian kas kecil secara programatis tanpa harus menginstansiasi kelas ini
	 * secara langsung — cukup menggunakan antarmuka {@code DataInitDefault}.</p>
	 *
	 * <p><b>Cara kerja:</b> Melakukan cast {@code obj} ke {@link PenggantianKasKecil},
	 * menyimpannya ke field instance {@code penggantianKasKecil}, lalu memanggil
	 * {@code init(PenggantianKasKecil)} (overload private) untuk membangun konten
	 * window. Setelah itu, window ditampilkan sebagai modal.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param obj objek domain yang akan dibuka dalam form; harus merupakan instance
	 *            {@link PenggantianKasKecil}
	 * @throws Exception jika cast gagal atau terjadi kesalahan saat membangun form
	 *
	 * <p><b>Pemeliharaan:</b> Jika antarmuka {@code DataInitDefault} berubah (misalnya
	 * menambah parameter), perbarui implementasi ini sesuai kontrak baru.</p>
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		penggantianKasKecil = (PenggantianKasKecil) obj;
		init(penggantianKasKecil);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Handler event ZK untuk tombol "Tambah" di toolbar — membuka form kosong untuk
	 * membuat pengajuan penggantian kas kecil baru.
	 *
	 * <p><b>Tujuan:</b> Memberikan entry point bagi pengguna untuk memulai pengajuan
	 * penggantian kas kecil baru melalui klik tombol di antarmuka. Method ini
	 * dikonvensikan oleh ZK sebagai event handler untuk event {@code onClick} pada
	 * komponen dengan id {@code add} di file ZUL.</p>
	 *
	 * <p><b>Cara kerja:</b> Menyetel flag {@code viewOnly = false} agar form dapat
	 * diedit (bukan hanya dilihat), membuat instance {@link PenggantianKasKecil} baru
	 * yang kosong, lalu memanggil {@code init()} untuk membangun konten window dan
	 * menampilkannya sebagai modal. Error saat membuka modal ditangkap dan dilaporkan
	 * ke admin via {@code Common.tampilErrorJikaAdmin()}.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event event ZK yang memicu handler ini (biasanya onClick dari toolbar button)
	 * @throws Exception jika terjadi kesalahan saat membangun form
	 *
	 * <p><b>Pemeliharaan:</b> Nama method {@code onAdd} bersifat konvensional ZK —
	 * jangan diubah kecuali id komponen di ZUL juga diubah.</p>
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		init(new PenggantianKasKecil());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun dan mengembalikan grid formulir pengajuan penggantian kas kecil yang
	 * dapat dilekatkan ke berbagai kontainer — baik window internal maupun form SOP.
	 *
	 * <p><b>Tujuan:</b> Implementasi kontrak {@code FormSop.form()} yang menghasilkan
	 * widget formulir lengkap berisi semua field input untuk entitas
	 * {@link PenggantianKasKecil}. Desain ini memungkinkan form yang sama digunakan
	 * di window pengajuan mandiri maupun di dalam alur SOP persetujuan.</p>
	 *
	 * <p><b>Cara kerja langkah demi langkah:</b>
	 * <ol>
	 *   <li>Menginisialisasi {@code satuanKerjaTreeModel} jika belum ada.</li>
	 *   <li>Menyetel {@code disposisiSop} dengan aturan: jika sudah ada dan yang baru
	 *       null/belum tersimpan, tetap pakai yang lama.</li>
	 *   <li>Menentukan nilai {@code setujui} — true jika data sudah DISETUJU dan tidak
	 *       dalam mode persetujuan (artinya form ditampilkan read-only karena sudah final).</li>
	 *   <li>Memeriksa apakah alur SOP sudah selesai untuk mengaktifkan {@code viewOnly}.</li>
	 *   <li>Merender baris-baris form: Satuan Kerja, Kas Kecil, Kode, Judul Penggantian,
	 *       Unit, Saldo, Akun, Tanggal, Nilai Kas Kecil, Rincian Formula, Diajukan Oleh,
	 *       Diajukan Tanggal, Sisa Saldo, Penutupan Kas Kecil, Status Pengajuan (radio),
	 *       Tanggal Persetujuan, dan Keterangan.</li>
	 *   <li>Setiap field menampilkan label read-only jika {@code persetujuan || setujui
	 *       || viewOnly}, atau widget input jika masih bisa diedit.</li>
	 *   <li>Mendaftarkan {@code EventListener} pada combobox kas kecil yang memuat ulang
	 *       rincian formula JSON saat kas kecil dipilih/diubah.</li>
	 *   <li>Mendaftarkan atribut {@code eventListenerSetuju} pada grid agar form SOP dapat
	 *       memicu perubahan radio status dari luar.</li>
	 *   <li>Mendaftarkan listener pada radio status yang mengubah label tombol simpan
	 *       dan visibilitas field tanggal persetujuan.</li>
	 * </ol></p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param generalValueObject objek data; akan di-cast ke {@link PenggantianKasKecil}
	 * @param disposisiSop       disposisi SOP aktif jika form dibuka dari alur SOP;
	 *                           {@code null} jika dibuka secara mandiri
	 * @param save               tombol simpan yang label-nya akan diubah secara dinamis
	 *                           berdasarkan status yang dipilih
	 * @param setujuiData        listener tambahan pada radio status (untuk integrasi SOP);
	 *                           {@code null} jika tidak diperlukan
	 * @return {@link MyGrid} berisi seluruh komponen form, siap ditambahkan ke kontainer
	 * @throws Exception jika terjadi kesalahan saat membangun komponen atau mengakses DB
	 *
	 * <p><b>Penanganan error:</b> Jika satuan kerja pengguna tidak dapat ditentukan,
	 * blok try-catch menelan exception agar form tetap dapat dibuka meskipun data
	 * satuan kerja tidak tersedia.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field baru di entitas {@link PenggantianKasKecil},
	 * tambahkan baris form baru di sini. Pastikan logika read-only konsisten dengan
	 * kondisi {@code persetujuan}, {@code setujui}, dan {@code viewOnly}.</p>
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		penggantianKasKecil = (PenggantianKasKecil) generalValueObject;

		setujui = false;
		if (!persetujuan) {
			if (penggantianKasKecil != null && penggantianKasKecil.getStatus().equals(PenggantianKasKecil.DISETUJU)) {
				setujui = true;
			} else {
				setujui = false;
			}
		}

		if (penggantianKasKecil.getDisposisiSop() != null
				&& penggantianKasKecil.getDisposisiSop().getDisposisiSetuju() != null
				&& penggantianKasKecil.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& penggantianKasKecil.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}

		kasKecil = new Combobox();

		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		try {
			if (penggantianKasKecil.getSatuanKerja() == null) {
				penggantianKasKecil.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PenggantianKasKecilAction.java:996");
			// TODO: handle exception
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(
				penggantianKasKecil.getSatuanKerja() == null ? "" : penggantianKasKecil.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", penggantianKasKecil.getSatuanKerja());

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(penggantianKasKecil.getSatuanKerja() == null ? ""
					: penggantianKasKecil.getSatuanKerja().getNama()));
		} else {
			row.appendChild(satuanKerja);
		}

		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kas Kecil *"));

		Common.insertCombo(kasKecil, new String[] { "kode", "nama", "jenisKasKecil" }, "keterangan", KasKecil.class,

				Restrictions.and(Restrictions.eq("status", KasKecil.DISETUJU),
						Restrictions.and(
								Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.or(
												satuanKerjas.isEmpty() ? Restrictions.sqlRestriction("true")
														: Restrictions.or(
																parent == null ? Restrictions.isNull("satuanKerja")
																		: Restrictions.sqlRestriction("false"),
																Restrictions.in("satuanKerja", satuanKerjas)),
												Restrictions.eq("satuanKerja", tbmuser.ambilSatuanKerja()))),

								Restrictions.and(Restrictions.isNull("penggantianKasKecil"),
										Restrictions.eq("aktif", true)))));

		Common.selectComboItem(true, kasKecil, penggantianKasKecil.getKasKecil());

		kasKecil.setReadonly(true);
		kasKecil.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(penggantianKasKecil.getKasKecil() == null ? ""
					: penggantianKasKecil.getKasKecil().getKode() + "-" + penggantianKasKecil.getKasKecil().getNama()));
		} else {
			row.appendChild(kasKecil);
		}

		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");

				if (parent != null) {

					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					if (parent != null) {
						satuanKerjas.clear();
						satuanKerjas.add(parent);
						satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
					}

					Common.insertCombo(kasKecil, new String[] { "kode", "nama", "jenisKasKecil" }, "keterangan",
							KasKecil.class,

							Restrictions
									.and(Restrictions.eq("status", KasKecil.DISETUJU),
											Restrictions.and(
													Restrictions.or(Restrictions.isNull("satuanKerja"),
															Restrictions.or(
																	satuanKerjas.isEmpty()
																			? Restrictions.sqlRestriction("true")
																			: Restrictions.in("satuanKerja",
																					satuanKerjas),
																	Restrictions.eq("satuanKerja",
																			tbmuser.ambilSatuanKerja()))),

													Restrictions.and(Restrictions.isNull("penggantianKasKecil"),
															Restrictions.eq("aktif", true)))));

					Common.selectComboItem(true, kasKecil, penggantianKasKecil.getKasKecil());

				}

			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		if (penggantianKasKecil.getKode() == null) {
			String noAgenda = generateCode(false);
			penggantianKasKecil.setKode(noAgenda);
		}

		kode = new Label(penggantianKasKecil.getKode());
		if (persetujuan) {
			row.appendChild(new Label(penggantianKasKecil.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		nama = new Textbox(penggantianKasKecil.getNama());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Penggantian *"));
		nama.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(penggantianKasKecil.getNama()));
		} else {
			row.appendChild(nama);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit/Satuan Kerja"));
		final Label unit = new Label();
		row.appendChild(unit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saldo Kas Kecil"));
		final Label saldo = new Label();
		row.appendChild(saldo);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Kas Kecil"));
		final Label akun = new Label();
		row.appendChild(akun);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kas Kecil *"));
		final Label mulai = new Label();
		row.appendChild(mulai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Kas Kecil *"));
		final Label nilaiPengajuan = new Label();
		row.appendChild(nilaiPengajuan);

		nilaiHarusDikembalikan = new Label();
		nilaiHarusDikembalikan.setValue(penggantianKasKecil.getKasKecil() == null ? ""
				: Common.numberFormat.get().format(penggantianKasKecil.getKasKecil().getSisa()));
		penutupan = new MyLabelConfig();

		rowRinci = new MyFormRow();
		rowRinci.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowRinci, "2");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				KasKecil work = (KasKecil) (kasKecil.getSelectedItem() == null ? null
						: kasKecil.getSelectedItem().getValue());

				if (work != null && kode.getValue().trim().isEmpty()) {
					kode.setValue(work.getKode());
				}
				if (work != null && nama.getValue().trim().isEmpty()) {
					nama.setValue(work.getNama());
				}

				mulai.setValue(
						work == null || work.getTanggal() == null ? "" : Common.dateFormat4.get().format(work.getTanggal()));

				unit.setValue(work == null || work.getSatuanKerja() == null ? "" : work.getSatuanKerja().getNama());

				akun.setValue(work == null || work.getJenisKasKecil().getAkun() == null ? ""
						: work.getJenisKasKecil().getAkun().getKode() + "-"
								+ work.getJenisKasKecil().getAkun().getNama());

				saldo.setValue(work == null ? "" : Common.numberFormat.get().format(work.getSaldo()));

				nilaiPengajuan.setValue(work == null ? "" : Common.numberFormat.get().format(work.getNilai()));

				Common.clear(rowRinci);
//				KasKecilAction.tampilRinci(work).setParent(rowRinci);

				array = null;

				if (work != null) {

					nilai = 0.0;
					array = new JSONArray(work.getFormula());
					rowFormula = Common.tampilanScroll1(rowRinci);
					KasKecilAction.reloadFormula(rowFormula, array, persetujuan, setujui, viewOnly, work.getTahun(),
							new MyCheckboxConfigPilih(), nilaiHarusDikembalikan, saldo, edit);

					penutupan.setValue(work.getMerupakanPenutupanKasKecil() ? "Ya" : "Tidak");

					lainMahasiswa = null;
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rowFormula.getParent());
					Hbox hbox = new Hbox();
					LampiranLain.createDownloadUploadFileLain(hbox, work.getId(), KasKecil.class.getName(),
							"Bukti Pengeluaran", false, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									lainMahasiswa = (LampiranLain) arg0.getData();
								}
							});
					hbox.setParent(row);

					Common.initKeteranganSatuKolom((Rows) rowFormula.getParent(),
							"Jika file bukti pengeluaran lebih dari satu file, zip dulu semua file tersebut");
				}
			}
		};

		kasKecil.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Oleh"));
		row.appendChild(new Label(
				penggantianKasKecil.getDibuatOleh() == null ? "" : penggantianKasKecil.getDibuatOleh().getUserNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan Tanggal"));
		row.appendChild(new Label(penggantianKasKecil.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(penggantianKasKecil.getTanggalPembuatan())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sisa Saldo Kas Kecil"));
		row.appendChild(nilaiHarusDikembalikan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Apakah penutupan kas kecil ?"));
		penutupan.setParent(row);

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
		status = new Radiogroup();
		Radio comboitem = new Radio(UangMuka.PENGAJUAN);
		comboitem.setAttribute("value", UangMuka.PENGAJUAN);
		comboitem.setValue(UangMuka.PENGAJUAN);
		comboitem.setVisible(false);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DISETUJU);
		comboitem.setAttribute("value", UangMuka.DISETUJU);
		comboitem.setValue(UangMuka.DISETUJU);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DITOLAK);
		comboitem.setAttribute("value", UangMuka.DITOLAK);
		comboitem.setValue(UangMuka.DITOLAK);
		status.appendChild(comboitem);
		status.setWidth("90%");
		Common.selectRadioItem(status, penggantianKasKecil.getStatus());
		row.appendChild(status);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("checkbox");
					if (selesai != null && selesai) {
						Common.selectRadioItem(status, UangMuka.DISETUJU);
						Common.freeze(status, true);
					} else {
						status.setSelectedItem(null);
						Common.freeze(status, false);
					}
				}
			}
		});

		if (setujuiData != null) {
			status.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, penggantianKasKecil.getStatus().equals(UangMuka.DISETUJU)));
				}
			});
		}

		if (setujui) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
			row.appendChild(new ais.ui.util.MyLabelConfig(penggantianKasKecil.getStatus()));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
		tanggalPersetujuanManual = new MyDatebox(penggantianKasKecil.getTanggalPersetujuanManual());
		if (penggantianKasKecil.getPostingHistory() == null) {
			row.appendChild(tanggalPersetujuanManual);
		} else {
			row.appendChild(new Label(Common.dateFormat1.get()
					.format(penggantianKasKecil.getTanggalPersetujuanManual() == null ? WaktuUtil.getDate()
							: penggantianKasKecil.getTanggalPersetujuanManual())));
		}
		tanggalPersetujuanManual.setReadonly(true);
		tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (penggantianKasKecil != null && penggantianKasKecil.getId() != null) {
					penggantianKasKecil.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
					Common.refreshUpdate(penggantianKasKecil);
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(
				penggantianKasKecil.getKeterangan() == null ? "" : penggantianKasKecil.getKeterangan());

		if (setujui) {
			row.appendChild(new Label(penggantianKasKecil.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		eventListener.onEvent(null);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean setujui = status.getSelectedItem() == null ? false
						: status.getSelectedItem().getValue().equals(PenggantianKasKecil.DISETUJU);
				if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
					if (tanggalPersetujuanManual.getValue() == null) {
						tanggalPersetujuanManual.setValue(WaktuUtil.getDate());
					}
					tanggalPersetujuanManual.getParent().setVisible(setujui);
				}
				if (setujui) {
					save.setLabel("Transfer Penggantian Kas Kecil");
				} else {
					save.setLabel(!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
				}
			}
		};

		status.addEventListener("onClick", s);
		Common.createDefaultTimer(s);

		return grid;
	}

	/**
	 * Membangun konten window modal untuk form pengajuan/persetujuan penggantian kas kecil
	 * dan mengkonfigurasi tombol aksi (Batal dan Simpan).
	 *
	 * <p><b>Tujuan:</b> Method internal yang digunakan oleh {@code onAdd()},
	 * {@code init(GeneralValueObject)}, dan alur SOP untuk menyiapkan window modal
	 * yang berisi form lengkap penggantian kas kecil. Method ini memisahkan logika
	 * pembuatan UI window dari logika bisnis form itu sendiri.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Mengisi {@code dibuatOleh} dan {@code tanggalPembuatan} jika belum ada.</li>
	 *   <li>Menyetel judul window sesuai mode (Pengajuan atau Persetujuan).</li>
	 *   <li>Membersihkan konten window lama dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membuat layout {@code Borderlayout} dengan Center (form) dan South (toolbar).</li>
	 *   <li>Memanggil {@code form()} untuk menghasilkan grid form dan menempelkannya
	 *       ke Center.</li>
	 *   <li>Membuat tombol Batal yang menyembunyikan window, dan tombol Simpan yang
	 *       memanggil {@code onSave()}. Jika {@code onSave()} berhasil (return true),
	 *       grid utama di-refresh dan window ditutup.</li>
	 * </ol></p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param penggantianKasKecil objek yang akan diedit atau dibuat baru; jika id null
	 *                            maka ini adalah entitas baru yang belum tersimpan
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI
	 *
	 * <p><b>Pemeliharaan:</b> Label tombol simpan awalnya diset di sini, tetapi
	 * dapat berubah secara dinamis oleh listener status radio di dalam {@code form()}.
	 * Pastikan kedua tempat konsisten jika ada perubahan label.</p>
	 */
	private void init(final PenggantianKasKecil penggantianKasKecil) throws Exception {

		if (penggantianKasKecil.getDibuatOleh() == null) {
			penggantianKasKecil.setDibuatOleh(tbmuser);
			penggantianKasKecil.setTanggalPembuatan(new Date());
		}

		addWindow.setTitle((!persetujuan ? "Pengajuan" : "Persetujuan") + " Penggantian Kas Kecil");
		this.penggantianKasKecil = penggantianKasKecil;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig(
				!persetujuan ? "Ajukan dan Cetak" : "Ubah Status Persetujuan dan Cetak", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(penggantianKasKecil, disposisiSop, save, null));

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

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
	 * Memvalidasi input form dan menyimpan entitas {@link PenggantianKasKecil} ke database,
	 * lalu memicu proses async berupa pembaruan kas kecil, upload lampiran, cetak laporan,
	 * dan pembuatan entri transfer.
	 *
	 * <p><b>Tujuan:</b> Ini adalah method inti yang menangani seluruh logika penyimpanan
	 * pengajuan penggantian kas kecil — mulai dari validasi input, kalkulasi nilai,
	 * pemeriksaan saldo, penyimpanan ke DB, hingga pemicu proses lanjutan secara async.</p>
	 *
	 * <p><b>Cara kerja langkah demi langkah:</b>
	 * <ol>
	 *   <li><b>Validasi satuan kerja:</b> Jika atribut {@code satuanKerja} kosong,
	 *       tampilkan peringatan dan return false.</li>
	 *   <li><b>Validasi kas kecil:</b> Jika tidak ada kas kecil dipilih, tampilkan
	 *       peringatan dan return false.</li>
	 *   <li><b>Validasi judul:</b> Jika field nama kosong, tampilkan peringatan dan
	 *       return false.</li>
	 *   <li><b>Validasi formula:</b> Muat ulang objek {@link KasKecil} dari DB (fresh),
	 *       lalu iterasi array JSON formula untuk memastikan setiap item memiliki akun
	 *       biaya dan nilai > 0. Jika ada yang tidak lengkap, tampilkan peringatan dan
	 *       return false.</li>
	 *   <li><b>Kalkulasi nilai:</b> Jumlahkan semua nilai dari item formula yang valid
	 *       (key tidak null) ke dalam field {@code nilai}.</li>
	 *   <li><b>Cek saldo:</b> Hitung saldo aktual via {@code JenisKasKecilAction.hitungSaldo()};
	 *       jika nilai penggantian melebihi saldo, tampilkan peringatan dan return false.</li>
	 *   <li><b>Update kas kecil:</b> Simpan nilai dan saldo baru ke entitas KasKecil di DB.</li>
	 *   <li><b>Muat ulang entitas:</b> Jika entitas sudah ada di DB (id tidak null),
	 *       muat ulang via {@code session.load()} untuk menghindari stale data.</li>
	 *   <li><b>Set metadata:</b> Mengisi {@code dibuatOleh}, {@code disposisiSop},
	 *       referensi kas kecil, kode, nama, keterangan, satuan kerja, status persetujuan
	 *       (dengan atau tanpa penyetuju dan tanggal), serta tanggal persetujuan manual.</li>
	 *   <li><b>Simpan ke DB:</b> {@code session.update()} untuk entitas lama atau
	 *       {@code session.save()} untuk entitas baru. Kode baru di-generate via
	 *       {@code generateCode(true)}.</li>
	 *   <li><b>Async post-save:</b> Melalui timer default, menjalankan:
	 *       <ul>
	 *         <li>Refresh dan update relasi {@code penggantianKasKecil} di entitas KasKecil.</li>
	 *         <li>Jika ada lampiran baru, memperbarui referensi {@code ref} di entitas
	 *             {@link LampiranLain} menggunakan sesi streaming terpisah.</li>
	 *         <li>Memicu cetak laporan via {@code cetak()} dengan delay 2,5 detik.</li>
	 *         <li>Jika status DISETUJU, membuat {@link DaftarPengajuanTransfer}
	 *             via {@code DaftarPengajuanTransfer.simpanPenggantianKasKecil()}.</li>
	 *       </ul>
	 *   </li>
	 * </ol></p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event event ZK yang memicu penyimpanan (biasanya onClick dari tombol Simpan);
	 *              tidak digunakan secara langsung di dalam method
	 * @return {@code true} jika penyimpanan berhasil; {@code false} jika ada validasi
	 *         yang gagal dan penyimpanan dibatalkan
	 * @throws Exception jika terjadi kesalahan database yang tidak terduga
	 *
	 * <p><b>Penanganan error:</b> Validasi ditangani dengan menampilkan MessageBox dan
	 * return false. Error pada upload lampiran ditangani dengan rollback sesi streaming
	 * dan pelaporan ke admin, tanpa membatalkan penyimpanan utama yang sudah berhasil.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field baru di {@link PenggantianKasKecil} yang
	 * harus disimpan, tambahkan setter-nya di bagian "Set metadata" sebelum blok
	 * {@code session.update()/save()}. Pastikan validasi baru ditambahkan di awal
	 * method sebelum operasi DB dimulai.</p>
	 */
	public boolean onSave(Event event) throws Exception {

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Satuan Kerja dari field pencarian yang tersedia; (2) Pastikan data Satuan Kerja sudah tercatat di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		KasKecil w = (KasKecil) (kasKecil.getSelectedItem() == null ? null : kasKecil.getSelectedItem().getValue());
		if (w == null || w.getId() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kas Kecil belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Kas Kecil dari dropdown yang tersedia; (2) Pastikan data kas kecil sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Pengajuan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Pengajuan dengan deskripsi singkat yang jelas; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		KasKecil work = (KasKecil) session.createCriteria(KasKecil.class).add(Restrictions.idEq(w.getId()))
				.uniqueResult();

		if (work != null && array != null) {
			work.setFormula(array.toString());
			JSONArray array = new JSONArray(work.getFormula());
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				Long key = null;
				if (!jsonObject.isNull("key")) {
					key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
				}

				if (key != null) {

					Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
							: ConstantValues.ambil(Akun.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"akun")));

					Double jumlah = 0.0;
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}

					if (akunBiaya == null) {
						MyMessageboxConfig.show("Mohon maaf, ada akun biaya pengeluaran yang belum dipilih. Langkah yang dapat dilakukan: (1) Periksa setiap baris rincian biaya dan lengkapi kolom Akun yang masih kosong; (2) Pilih akun yang sesuai melalui field pencarian akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return false;
					}

					if (jumlah.intValue() == 0) {
						MyMessageboxConfig.show("Mohon maaf, ada nilai biaya pengeluaran yang masih nol. Langkah yang dapat dilakukan: (1) Periksa setiap baris rincian biaya dan isikan nominal yang valid; (2) Pastikan semua nilai biaya lebih dari nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return false;
					}
				}
			}

			nilai = 0.0;
			for (int i = 0; i < array.length(); i++) {
				Double jumlah = 0.0;
				JSONObject jsonObject = array.getJSONObject(i);
				Long key = null;
				if (!jsonObject.isNull("key")) {
					key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
				}

				if (key != null) {
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}
					nilai += jumlah;
				}
			}

			Double saldo = JenisKasKecilAction.hitungSaldo(work.getId(), work.getJenisKasKecil(), work.getTanggal());

			if (saldo < nilai) {
				MyMessageboxConfig.show("Mohon maaf, nilai pengeluaran kas kecil melebihi saldo yang tersedia. Langkah yang dapat dilakukan: (1) Kurangi nilai pengeluaran agar tidak melebihi saldo kas kecil saat ini; (2) Lakukan pengisian ulang kas kecil terlebih dahulu jika saldo tidak mencukupi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
			work.setNilai(nilai);
			work.setSaldo(saldo);
			session.update(work);
			session.flush();
		}

		if (penggantianKasKecil.getId() != null) {
			penggantianKasKecil = (PenggantianKasKecil) session.load(PenggantianKasKecil.class,
					penggantianKasKecil.getId());
		}

		if (penggantianKasKecil.getDibuatOleh() == null) {
			penggantianKasKecil.setDibuatOleh(tbmuser);
			penggantianKasKecil.setTanggalPembuatan(new Date());
		}
		if (disposisiSop != null && disposisiSop.getId() != null) {
			penggantianKasKecil.setDisposisiSop(disposisiSop);
		}

		penggantianKasKecil.setKasKecil(work);
		penggantianKasKecil.setKode(kode.getValue());
		penggantianKasKecil.setNama(nama.getValue());
		penggantianKasKecil.setKeterangan(keterangan.getValue());

		penggantianKasKecil.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
		if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
			penggantianKasKecil.setDisetujuiOleh(tbmuser);
			penggantianKasKecil.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
		} else {
			penggantianKasKecil.setDisetujuiOleh(null);
			penggantianKasKecil.setTanggalPersetujuan(null);
		}
		penggantianKasKecil.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
		penggantianKasKecil.setStatus(sts);

		if (penggantianKasKecil.getId() != null) {
			session.update(penggantianKasKecil);
		} else {
			penggantianKasKecil.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			penggantianKasKecil.setKode(kode.getValue());
			session.save(penggantianKasKecil);
		}

		session.flush();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				KasKecil work = (KasKecil) (kasKecil.getSelectedItem() == null ? null
						: kasKecil.getSelectedItem().getValue());
				if (work != null) {
					Session session = HibernateUtil.currentSession();
					session.refresh(work);
					work.setPenggantianKasKecil(penggantianKasKecil);
					Common.refreshUpdate(session, work);
					session.flush();

					if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
						try {
							session = StreamingHibernateUtil.getInstance().currentSession();

							session.refresh(lainMahasiswa);
							lainMahasiswa.setRef(work.getId());

							session.getTransaction().begin();
							session.update(lainMahasiswa);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(PenggantianKasKecilAction.this.penggantianKasKecil);
					}
				}, "Proses cetak", false, 2500);

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (penggantianKasKecil.getStatus().equals(PenggantianKasKecil.DISETUJU)) {
							DaftarPengajuanTransfer.simpanPenggantianKasKecil(penggantianKasKecil);
						}

					}
				});

			}
		});

		return true;
	}

	/**
	 * Membangun objek {@link Criteria} Hibernate untuk query data {@link PenggantianKasKecil}
	 * berdasarkan semua filter aktif yang dipilih pengguna di panel pencarian.
	 *
	 * <p><b>Tujuan:</b> Menyediakan satu titik pembuatan kriteria query yang digunakan
	 * bersama oleh {@code onSearchDefault()} (untuk paging dan pengisian grid) dan oleh
	 * tombol ekspor. Dengan memisahkan logika query ke method ini, mudah untuk menambah
	 * filter baru tanpa mengubah kode di banyak tempat.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code searchparent} null (komponen ZUL tidak tersedia), return null
	 *       sebagai guard agar tidak NPE.</li>
	 *   <li>Mendapatkan {@link SatuanKerja} yang dipilih di filter, lalu membangun set
	 *       hierarki satuan kerja menggunakan {@code SatuanKerjaTreeModel.getChildsSet()}.</li>
	 *   <li>Membangun {@code Criteria} dengan filter:
	 *       <ul>
	 *         <li><b>Tanggal:</b> filter {@code date(tanggal_pembuatan) BETWEEN start AND end}
	 *             menggunakan SQL langsung; jika start/end null, filter diabaikan.</li>
	 *         <li><b>Kas kecil tidak null:</b> memastikan hanya record dengan kas kecil
	 *             yang valid yang ditampilkan.</li>
	 *         <li><b>Satuan kerja:</b> OR antara null satker (global) dan satker dalam
	 *             hierarki yang dipilih.</li>
	 *         <li><b>Status:</b> jika dipilih, filter exact match pada field status.</li>
	 *         <li><b>Aktif:</b> jika checkbox aktif dicentang, tampilkan hanya yang aktif;
	 *             jika tidak dicentang, tampilkan semua.</li>
	 *         <li><b>Kode:</b> ILIKE ANYWHERE jika diisi.</li>
	 *         <li><b>Nama:</b> ILIKE ANYWHERE jika diisi.</li>
	 *         <li><b>Jenis kas kecil:</b> jika diisi, join ke {@code kasKecil.jenisKasKecil}
	 *             dan filter nama/kode dengan ILIKE ANYWHERE.</li>
	 *       </ul>
	 *   </li>
	 *   <li>Jika parameter {@code order} true, tambahkan order DESC by id.</li>
	 * </ol></p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC ke criteria;
	 *              {@code false} digunakan saat menghitung total untuk paging
	 * @return {@link Criteria} yang siap dieksekusi, atau {@code null} jika komponen
	 *         UI belum diinisialisasi
	 *
	 * <p><b>Penanganan error:</b> Jika {@code start} atau {@code end} null (misalnya
	 * komponen belum di-render), filter tanggal menggunakan
	 * {@code sqlRestriction("1=1")} sebagai fallback aman.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada filter baru di ZUL, tambahkan kondisi Hibernate
	 * baru di sini. Perhatikan bahwa penggunaan {@code createAlias()} untuk join
	 * harus dilakukan secara kondisional (seperti pada filter {@code serachjenis})
	 * agar tidak menyebabkan join tidak perlu ketika filter tidak diisi.</p>
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(PenggantianKasKecil.class)

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(Restrictions.isNotNull("kasKecil"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));

		if (serachjenis != null && !serachjenis.getValue().trim().isEmpty()) {
			criteria.createAlias("kasKecil", "kasKecil").createAlias("kasKecil.jenisKasKecil", "jenisKasKecil")
					.add(serachjenis.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("jenisKasKecil.nama", serachjenis.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("jenisKasKecil.kode", serachjenis.getValue().trim(),
											MatchMode.ANYWHERE)));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	/**
	 * Memuat ulang data grid penggantian kas kecil berdasarkan filter aktif, dengan
	 * dukungan paginasi.
	 *
	 * <p><b>Tujuan:</b> Method ini adalah entry point utama untuk refresh tampilan data.
	 * Dipanggil pada inisialisasi halaman, setelah simpan, setelah perubahan filter,
	 * dan setelah perubahan halaman paging. Namanya mengikuti konvensi ZK event handler
	 * ({@code onSearchDefault}) agar dapat dipanggil langsung dari ZUL sebagai event.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code Common.initPaging(initCriteria(false), paging)} untuk
	 *       menghitung jumlah total record dan memperbarui komponen paging di UI.</li>
	 *   <li>Memanggil {@code initCriteria(true)} dengan pagination
	 *       ({@code setMaxResults} + {@code setFirstResult}) untuk mengambil halaman data
	 *       yang sedang aktif.</li>
	 *   <li>Membungkus list hasil query dalam {@code SimpleListModel} dan menyetelnya
	 *       ke grid menggunakan {@code grid.setModelCheckMobile()} yang mendukung tampilan
	 *       mobile dan desktop.</li>
	 * </ol></p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event event ZK pemicu (bisa null jika dipanggil secara programatis)
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error eksplisit; jika query gagal,
	 * exception akan naik ke framework ZK dan ditampilkan sebagai error dialog.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika renderer baris perlu diganti (misalnya karena ada
	 * kolom baru), buat subkelas {@code MyRowRenderer} baru dan daftarkan di sini
	 * via {@code grid.setRowRenderer()}.</p>
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenggantianKasKecil> penggantianKasKecil = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(penggantianKasKecil);
		grid.setRowRenderer(new PenggantianKasKecilRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mengembalikan istilah domain dalam bahasa Indonesia untuk modul ini, digunakan
	 * sebagai label generik di alur SOP dan laporan.
	 *
	 * <p><b>Tujuan:</b> Implementasi kontrak {@code FormSop.istilah()} yang menyediakan
	 * nama modul dalam format yang dapat dibaca manusia untuk ditampilkan di antarmuka
	 * alur SOP, audit trail, dan pesan sistem.</p>
	 *
	 * @return string {@code "Penggantian Kas Kecil"}
	 * @throws Exception tidak akan terjadi; deklarasi ada karena kontrak antarmuka
	 */
	@Override
	public String istilah() throws Exception {
		return "Penggantian Kas Kecil";
	}

	/**
	 * Mengembalikan entitas {@link PenggantianKasKecil} yang sedang aktif di form
	 * sebagai {@link DataSop}, diperlukan oleh mekanisme alur SOP untuk mendapatkan
	 * referensi objek domain yang sedang diproses.
	 *
	 * <p><b>Tujuan:</b> Implementasi kontrak {@code FormSop.ambil()} yang memungkinkan
	 * sistem SOP mengakses objek domain tanpa perlu mengetahui tipe spesifiknya,
	 * cukup melalui antarmuka {@code DataSop}.</p>
	 *
	 * @return objek {@link PenggantianKasKecil} yang sedang dibuka/diedit di form
	 * @throws Exception tidak akan terjadi; deklarasi ada karena kontrak antarmuka
	 */
	@Override
	public DataSop ambil() throws Exception {
		return penggantianKasKecil;
	}

	/**
	 * Mengembalikan kelas entitas domain yang dikelola oleh action ini, digunakan
	 * oleh infrastruktur generik (ekspor, SOP) untuk refleksi.
	 *
	 * <p><b>Tujuan:</b> Implementasi kontrak {@code FormSop.ambilClass()} agar sistem
	 * generik seperti ekspor data dan alur SOP dapat mengetahui tipe entitas tanpa
	 * bergantung pada implementasi konkret kelas ini.</p>
	 *
	 * @return {@code PenggantianKasKecil.class}
	 * @throws Exception tidak akan terjadi; deklarasi ada karena kontrak antarmuka
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PenggantianKasKecil.class;
	}

	/**
	 * Menghasilkan kode unik untuk entitas {@link PenggantianKasKecil} berdasarkan
	 * konfigurasi nomor surat yang aktif, dengan jaminan keunikan via {@code KodeUnikUtil}.
	 *
	 * <p><b>Tujuan:</b> Membuat nomor dokumen penggantian kas kecil yang mengikuti
	 * format, urutan, dan aturan reset yang dikonfigurasi oleh administrator di modul
	 * Nomor Surat Alur Keuangan. Jika konfigurasi tidak ada, fallback ke barcode acak.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA} atau
	 *       {@code getNomorSurat()} null, return barcode acak via
	 *       {@code Common.getGeneratedBarCode()}.</li>
	 *   <li>Menentukan indeks urutan: jika {@code gunakanIndexUrut} true, pakai
	 *       {@code nomorIndex} langsung; jika tidak, hitung via {@code getindex()}
	 *       yang menghitung rowCount dari DB dengan aturan reset.</li>
	 *   <li>Jika {@code tambah} true, menaikkan indeks nomor surat via
	 *       {@code NomorSurat.tambahIndexNomorSurat()} (side effect: mengubah DB).</li>
	 *   <li>Memformat kode menggunakan {@code NomorSurat.format(index, tanggalSekarang)}.</li>
	 *   <li>Memastikan kode unik via {@code KodeUnikUtil.pastikanUnik()} yang menambahkan
	 *       sufiks jika kode sudah ada di DB.</li>
	 * </ol></p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param tambah {@code true} untuk menaikkan counter nomor surat (saat menyimpan
	 *               entitas baru); {@code false} hanya untuk pratinjau kode tanpa
	 *               mengubah counter
	 * @return string kode unik yang sudah diformat sesuai pola nomor surat
	 *
	 * <p><b>Pemeliharaan:</b> Method ini memiliki side effect (menaikkan counter) saat
	 * {@code tambah = true}. Hati-hati memanggil dengan {@code tambah = true} lebih dari
	 * sekali untuk entitas yang sama karena akan membuat "lubang" di urutan nomor.</p>
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA == null
				|| NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA.getNomorSurat().format(index,
				WaktuUtil.getDate());
		return ais.action.master.KodeUnikUtil.pastikanUnik(PenggantianKasKecil.class, noAgenda);
	}

	/**
	 * Menghitung indeks urutan berikutnya untuk nomor surat penggantian kas kecil
	 * berdasarkan jumlah record yang sudah ada di database, dengan memperhatikan
	 * aturan reset dan pengelompokan nomor surat.
	 *
	 * <p><b>Tujuan:</b> Menentukan nomor urut berikutnya secara dinamis dari DB, sebagai
	 * alternatif dari penggunaan counter tersimpan ({@code gunakanIndexUrut}). Pendekatan
	 * ini memastikan nomor urut selalu mencerminkan jumlah dokumen yang sudah ada,
	 * bukan counter yang bisa tidak sinkron jika ada data yang dihapus.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code nomorSurat} null, return 0 sebagai fallback aman.</li>
	 *   <li>Membangun query {@code rowCount} pada {@link PenggantianKasKecil} dengan
	 *       JOIN ke {@code nomorSuratAlurKeuangan} dan {@code nomorSurat}.</li>
	 *   <li>Menerapkan filter pengelompokan: jika {@code urutBerdasarkanNomor}, filter
	 *       exact match ke nomorSurat ini; jika {@code urutBerdasarkanKelompok} dan ada
	 *       kelompok, filter ke kelompok; jika tidak, tidak ada filter tambahan.</li>
	 *   <li>Jika reset per tahun aktif, filter {@code tahun = tahunSekarang}.</li>
	 *   <li>Jika reset per bulan aktif, filter {@code tahun = tahun AND bulan = bulan}.</li>
	 *   <li>Jika ada tanggal reset dan sudah terlewati, filter {@code tanggalPembuatan
	 *       >= tanggalReset}.</li>
	 *   <li>Mengambil rowCount, menambahkan 1, dan mengembalikannya sebagai indeks berikutnya.</li>
	 * </ol></p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param nomorSurat konfigurasi nomor surat yang aktif; jika null return 0
	 * @return indeks urutan berikutnya (jumlah record + 1), minimal 1
	 *
	 * <p><b>Pemeliharaan:</b> Jika skema tabel berubah (misalnya field reset berubah
	 * nama), perbarui property path di Restrictions. Perhatikan bahwa LEFT_JOIN
	 * digunakan untuk join ke nomorSuratAlurKeuangan agar record tanpa nomor surat
	 * pun ikut dihitung dalam total.</p>
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PenggantianKasKecil.class)
				.add(Restrictions.isNotNull("kasKecil"))
				.createAlias("nomorSuratAlurKeuangan", "nomorSuratAlurKeuangan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurKeuangan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurKeuangan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * Mengubah mode persetujuan action ini secara programatis, digunakan oleh
	 * infrastruktur alur SOP untuk mengaktifkan/menonaktifkan mode tanpa perlu
	 * membuat instance baru.
	 *
	 * <p><b>Tujuan:</b> Implementasi kontrak {@code FormSop.setPersetujuan()} yang
	 * memungkinkan sistem SOP mengubah perilaku form antara mode pengajuan dan mode
	 * persetujuan setelah action sudah diinstansiasi.</p>
	 *
	 * <p><b>Cara kerja:</b> Menyetel field {@code persetujuan} ke nilai yang diberikan.
	 * Perubahan ini akan tercermin pada render berikutnya: tombol Tambah akan
	 * disembunyikan, kolom menjadi read-only, dan widget status persetujuan muncul.</p>
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan;
	 *                    {@code false} untuk kembali ke mode pengajuan normal
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada state lain yang bergantung pada mode persetujuan
	 * (misalnya visibility tombol tambahan), tambahkan logika pembaruannya di sini.</p>
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
