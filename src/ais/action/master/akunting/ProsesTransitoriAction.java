package ais.action.master.akunting;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.ProsesTransitori;
import ais.database.model.akunting.Transitori;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>ProsesTransitoriAction — Pengelola Proses Pengajuan Transitori</h3>
 *
 * <p><strong>Untuk apa:</strong> Kelas ini adalah ZK GenericAutowireComposer yang
 * mengelola data Proses Transitori dalam modul akuntansi. Proses Transitori merupakan
 * wadah pengelompokan beberapa entitas {@link Transitori} (yang mewakili item daftar
 * pengajuan transfer individual) ke dalam satu batch pengajuan yang dapat diajukan
 * untuk persetujuan. Fitur ini digunakan oleh bagian keuangan untuk menggabungkan
 * beberapa permintaan transfer ke dalam satu proses pengajuan, memudahkan persetujuan
 * massal dan penelusuran status transfer.</p>
 *
 * <p><strong>Cara kerja:</strong> Kelas ini mengimplementasikan empat antarmuka:
 * {@link DataCriteria} (untuk membangun Criteria Hibernate), {@link DataSearchDefault}
 * (untuk memuat ulang grid), {@link DataInitDefault} (untuk membuka formulir dari luar),
 * dan {@link FormSop} (agar dapat diintegrasikan dengan alur persetujuan SOP). Alur
 * utama penggunaan adalah sebagai berikut:</p>
 * <ol>
 *   <li>Halaman menampilkan daftar ProsesTransitori yang difilter berdasarkan rentang
 *       tanggal dan nama melalui {@link #initCriteria} dan {@link #onSearchDefault}.</li>
 *   <li>Pengguna dapat membuat proses baru (tombol Tambah → {@link #onAdd}), memilih
 *       daftar Transitori yang belum terasosiasi, dan menyimpan melalui {@link #onSave}.</li>
 *   <li>Setiap baris di grid memiliki tombol Persetujuan (centang hijau) untuk menyetujui
 *       proses dan tombol Pembatalan (ikon peringatan) untuk membatalkan persetujuan.</li>
 *   <li>Formulir detail menampilkan grid Transitori terkait dengan kolom bank sumber,
 *       atas nama, nomor rekening, nilai, dan status SOP. Total nilai ditampilkan di
 *       footer grid.</li>
 *   <li>Melalui antarmuka {@link FormSop}, kelas ini dapat digunakan sebagai form dalam
 *       alur SOP sehingga persetujuan dapat dilakukan dalam konteks SOP.</li>
 * </ol>
 *
 * <p><strong>Konstruktor:</strong> Kelas memiliki dua konstruktor — konstruktor default
 * (tanpa parameter) digunakan oleh ZK saat halaman dimuat, dan konstruktor dengan
 * parameter {@code boolean persetujuan} digunakan saat kelas ini diinstansiasi secara
 * programatik dalam konteks SOP.</p>
 *
 * <p><strong>Threading:</strong> Semua operasi database dilakukan pada thread ZK event
 * menggunakan {@code HibernateUtil.currentSession()} (sesi terkelola). Tidak ada thread
 * latar belakang. Pemanggilan dari konteks SOP juga dilakukan pada thread yang sama.</p>
 *
 * <p><strong>Pemeliharaan:</strong> Jika ada field baru di {@link ProsesTransitori},
 * perbarui metode {@link #form}, {@link #onSave}, dan renderer {@link ProsesTransitoriRenderer}.
 * Pastikan juga array {@code contents} di {@link #doAfterCompose} diperbarui untuk ekspor
 * Excel. Jika ada kolom baru di tampilan detail Transitori, perbarui metode {@link #reload}
 * dan tambahkan kolom di {@link #initDetail}.</p>
 *
 * @see ProsesTransitori
 * @see Transitori
 * @see FormSop
 * @see DataCriteria
 */
public class ProsesTransitoriAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/** Serial version UID untuk serialisasi kelas. */
	private static final long serialVersionUID = -5779730267402400328L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;
	private MyDatebox start;
	private MyDatebox end;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ProsesTransitori prosesTransitori;
	private MyToolbarbuttonConfig add;

	private East east;

	private boolean persetujuan = false;
	private boolean approve = false;
	private boolean reject = false;

	/**
	 * Konstruktor default yang digunakan oleh ZK framework saat halaman dimuat secara
	 * normal melalui file ZUL.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan konstruktor tanpa parameter yang diperlukan
	 * oleh ZK GenericAutowireComposer untuk instansiasi otomatis.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Tidak melakukan inisialisasi khusus; semua
	 * inisialisasi dilakukan oleh {@link #doAfterCompose}. Flag {@code persetujuan}
	 * secara default adalah {@code false}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Konstruktor ini wajib ada untuk kompatibilitas
	 * dengan ZK autowire.</p>
	 */
	public ProsesTransitoriAction() {

	}

	/**
	 * Konstruktor dengan parameter mode persetujuan, digunakan saat kelas diinstansiasi
	 * secara programatik dalam konteks alur SOP.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan penggunaan kelas ini dalam mode persetujuan
	 * SOP, di mana tampilan formulir disesuaikan untuk aksi persetujuan (form dikunci,
	 * checkbox persetujuan ditampilkan).</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menyimpan nilai parameter {@code persetujuan} ke
	 * field instance. Jika true, berbagai bagian formulir akan menyesuaikan tampilan dan
	 * perilakunya (misalnya form dikunci dengan {@code Common.freezeGanti}, checkbox
	 * persetujuan ditampilkan).</p>
	 *
	 * <p><strong>Parameter:</strong> {@code persetujuan} — true jika digunakan dalam
	 * konteks persetujuan SOP, false untuk mode standar.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Konstruktor ini digunakan oleh framework SOP
	 * saat membuat instance untuk form persetujuan.</p>
	 *
	 * @param persetujuan true jika dalam mode persetujuan SOP
	 */
	public ProsesTransitoriAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Dipanggil sebelum komponen ZK dirakit untuk melakukan pemeriksaan keamanan akses
	 * halaman.
	 *
	 * <p><strong>Tujuan:</strong> Memastikan hanya pengguna dengan sesi valid yang dapat
	 * mengakses halaman Proses Transitori sebelum komponen UI dibangun.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@code Common.doCheckSecurity()} yang
	 * akan mengarahkan pengguna ke halaman login jika sesi tidak valid, kemudian
	 * memanggil implementasi parent untuk melanjutkan proses ZK normal.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Ditangani oleh {@code Common.doCheckSecurity()}
	 * secara internal.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Boilerplate keamanan standar; tidak perlu diubah.</p>
	 *
	 * @param page      halaman ZK yang sedang dimuat
	 * @param parent    komponen induk dalam pohon ZK
	 * @param compInfo  metadata komponen ZK
	 * @return ComponentInfo dari pemanggilan super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil setelah semua komponen ZK berhasil dirakit dan dihubungkan ke field kelas
	 * ini. Melakukan inisialisasi lengkap halaman Proses Transitori.
	 *
	 * <p><strong>Tujuan:</strong> Menginisialisasi semua komponen halaman termasuk filter
	 * tanggal default, hak akses pengguna, tombol ekspor Excel, listener paging, dan
	 * memuat data awal ke grid.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Langkah inisialisasi berurutan:
	 * <ol>
	 *   <li>Verifikasi sesi dan hak READ; logoff jika tidak valid.</li>
	 *   <li>Inisialisasi bahasa dengan {@code Common.initLaguage()}.</li>
	 *   <li>Set datebox filter sebagai readonly untuk mencegah pengetikan langsung.</li>
	 *   <li>Atur rentang tanggal default: dari 6 bulan lalu hingga besok.</li>
	 *   <li>Atur visibilitas tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Simpan flag hak APPROVE, REJECT, UPDATE, DELETE.</li>
	 *   <li>Muat data awal melalui {@link #onSearchDefault}.</li>
	 *   <li>Tambahkan tombol Download Excel menggunakan {@code Common.appendDownloadButton}
	 *       dengan array field yang akan diekspor.</li>
	 *   <li>Daftarkan listener paging.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika sesi tidak valid, akan langsung return
	 * setelah logoff.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada field baru yang perlu diekspor ke Excel,
	 * tambahkan ke array {@code contents}.</p>
	 *
	 * @param comp komponen root ZK yang telah dirakit
	 * @throws Exception jika terjadi error inisialisasi komponen ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);

		String[] contents = new String[] { "id", "nama", "keterangan", "aktif", "tanggalPembuatan",
				"tanggalPersetujuan", "nilai", "kodeUnik", "disposisiSop.id", "disetujuiOleh.userNama" };
		Common.appendDownloadButton(add, ProsesTransitori.class, this, contents);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	/**
	 * Kelas inner untuk merender setiap baris data {@link ProsesTransitori} pada grid
	 * utama halaman.
	 *
	 * <p><strong>Tujuan:</strong> Mengubah setiap entitas ProsesTransitori menjadi
	 * tampilan baris grid ZK yang informatif dengan kolom: info disetujui/tanggal,
	 * nama (dengan tombol revisi), nilai, keterangan+link SOP, serta tombol aksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Metode {@code render} meng-cast objek ke
	 * {@link ProsesTransitori}, kemudian:
	 * <ul>
	 *   <li>Kolom pertama: Vbox berisi nama user yang menyetujui (jika ada) dan tanggal
	 *       pembuatan.</li>
	 *   <li>Kolom kedua: komponen RevisiHelper untuk nama proses.</li>
	 *   <li>Kolom ketiga: nilai total diformat sebagai angka.</li>
	 *   <li>Kolom keempat: Vbox berisi keterangan dan link SOP (jika ada).</li>
	 *   <li>Kolom aksi: tombol salin/ubah/hapus (hapus hanya jika belum disetujui),
	 *       tombol Persetujuan (jika hak approve dan belum disetujui), tombol Pembatalan
	 *       (jika hak reject dan sudah disetujui), dan tombol Lihat (jika sudah
	 *       disetujui).</li>
	 * </ul>
	 * Tombol Persetujuan dan Pembatalan menampilkan konfirmasi dialog sebelum mengubah
	 * status, kemudian memperbarui grid via {@code Common.createDefaultTimer}.</p>
	 *
	 * <p><strong>Threading:</strong> Semua operasi di EventListener berjalan pada thread
	 * ZK event; aman untuk mengakses currentSession().</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada kolom baru, tambahkan di sini dan
	 * sesuaikan dengan deklarasi kolom di file ZUL.</p>
	 */
	class ProsesTransitoriRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data {@link ProsesTransitori} ke dalam komponen {@link Row} ZK.
		 *
		 * <p><strong>Tujuan:</strong> Mengisi baris grid dengan semua informasi proses
		 * transitori dan tombol aksi yang sesuai dengan status persetujuan dan hak akses
		 * pengguna saat ini.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Membangun komponen-komponen ZK secara berurutan
		 * dan menambahkannya ke Row. Untuk tombol persetujuan/pembatalan, digunakan
		 * MyMessageboxConfig untuk konfirmasi sebelum aksi dilakukan. Setelah perubahan
		 * status, grid diperbarui menggunakan timer default ZK untuk memastikan pembaruan
		 * UI terjadi pada siklus event yang benar.</p>
		 *
		 * <p><strong>Penanganan error:</strong> Exception dari EventListener diteruskan
		 * ke framework ZK. Exception pada RevisiHelper ditangkap dan diganti dengan
		 * MyLabelKecil sebagai fallback.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong> Visibilitas tombol diatur berdasarkan kombinasi
		 * flag hak akses ({@code approve}, {@code reject}, {@code edit}, {@code delete}) dan
		 * status persetujuan entitas ({@code getDisetujuiOleh()}).</p>
		 *
		 * @param arg0 komponen Row ZK yang akan diisi
		 * @param arg1 objek data yang harus berupa instansi ProsesTransitori
		 * @throws Exception jika terjadi error saat render komponen ZK
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final ProsesTransitori prosesTransitori = (ProsesTransitori) arg1;

			Vbox aaa = new Vbox();
			aaa.setParent(arg0);

			if (prosesTransitori.getDisetujuiOleh() != null) {
				new Label(prosesTransitori.getDisetujuiOleh().getUserNama()).setParent(aaa);
			}

			new Label(Common.dateFormat.get().format(prosesTransitori.getTanggalPembuatan())).setParent(aaa);

			RevisiHelper.createNewRevisi(ProsesTransitori.class, prosesTransitori, text(prosesTransitori.getNama()))
					.setParent(arg0);

			new Label(formatNilai(nilai(prosesTransitori.getNilai()))).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(text(prosesTransitori.getKeterangan())).setParent(a);

			if (prosesTransitori.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + text(prosesTransitori.getDisposisiSop().getKeterangan())
						+ (prosesTransitori.getDisposisiSop().getSop() == null ? ""
								: " (" + text(prosesTransitori.getDisposisiSop().getSop().getNama()) + ")"));
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(prosesTransitori.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}


			Hbox aa;
			(aa = Common.copyEditDeleteButtons(edit, false, prosesTransitori.getDisetujuiOleh() == null && delete,
					prosesTransitori, ProsesTransitoriAction.this)).setParent(arg0);
			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			aa.appendChild(disetujui);
			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			aa.appendChild(dibatalkan);

			disetujui.setVisible(approve && prosesTransitori.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && prosesTransitori.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui proses transfer ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();

										prosesTransitori.setDisetujuiOleh(Common.getCurrentUser());
										prosesTransitori.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, prosesTransitori);
										session.flush();


										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(null);
											}
										});

									}
								}
							});
				}

			});
			disetujui.setParent(aa);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan proses transfer ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										prosesTransitori.setDisetujuiOleh(null);
										prosesTransitori.setTanggalPersetujuan(null);
										Common.refreshUpdate(session, prosesTransitori);
										session.flush();


										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(null);
											}
										});
									}
								}
							});
				}

			});

			if (prosesTransitori.getDisetujuiOleh() != null) {
				MyToolbarbuttonConfig disetujuia = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
				aa.appendChild(disetujuia);
				disetujuia.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(prosesTransitori);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				disetujuia.setParent(aa);

			}
		}

	}

	/**
	 * Menangani event klik tombol Tambah untuk membuka formulir tambah Proses Transitori
	 * baru dalam mode non-persetujuan.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan titik masuk untuk pembuatan proses transitori
	 * baru dengan membuka popup formulir kosong.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Mereset flag {@code persetujuan} ke false (mode
	 * standar), membuat instance {@link ProsesTransitori} baru, memanggil
	 * {@link #init(ProsesTransitori)} untuk membangun formulir, kemudian menampilkan
	 * popup sebagai modal dialog.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception diteruskan ke framework ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Nama metode {@code onAdd} mengikuti konvensi ZK
	 * event handler.</p>
	 *
	 * @param event event ZK dari klik tombol Tambah
	 * @throws Exception jika terjadi error saat membangun formulir
	 */
	public void onAdd(Event event) throws Exception {
		persetujuan = false;
		init(new ProsesTransitori());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Implementasi dari {@link DataInitDefault} untuk membuka formulir dengan data entitas
	 * yang sudah ada, dipanggil oleh framework saat pengguna mengklik tombol Ubah.
	 *
	 * <p><strong>Tujuan:</strong> Membuka popup formulir yang sudah terisi dengan data
	 * ProsesTransitori yang dipilih dari grid.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Melakukan casting {@code obj} ke {@link ProsesTransitori},
	 * menyimpan ke field instance, memanggil {@link #init(ProsesTransitori)}, dan
	 * menampilkan popup modal.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception diteruskan ke framework ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini merupakan kontrak antarmuka
	 * DataInitDefault.</p>
	 *
	 * @param obj objek GeneralValueObject yang harus berupa instansi ProsesTransitori
	 * @throws Exception jika terjadi error saat membangun formulir atau menampilkan modal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		prosesTransitori = (ProsesTransitori) obj;
		init(prosesTransitori);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membangun dan mengisi popup formulir tambah/ubah Proses Transitori secara programatik,
	 * termasuk area detail daftar Transitori terkait di panel East.
	 *
	 * <p><strong>Tujuan:</strong> Mempersiapkan seluruh antarmuka formulir untuk
	 * ProsesTransitori, termasuk field header (tanggal, judul, keterangan, status
	 * persetujuan) di area Center dan daftar Transitori yang terkait di area East.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membersihkan konten addWindow, membangun Borderlayout
	 * dengan Center (formulir header via {@link #form}) dan South (toolbar tombol). Untuk
	 * entitas baru, ditampilkan tombol Batal dan Simpan; untuk entitas yang sudah ada,
	 * hanya tombol Selesai. Panel East diinisialisasi via {@link #initDetail} untuk
	 * menampilkan daftar Transitori terkait (65% lebar).</p>
	 *
	 * <p><strong>Parameter:</strong> {@code prosesTransitori} — entitas yang ditampilkan.
	 * Jika ID null, mode Tambah; jika tidak null, mode Ubah atau Lihat.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception diteruskan ke framework ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika perlu menambahkan panel tambahan (misalnya
	 * log aktivitas), dapat ditambahkan sebagai panel West atau tab baru.</p>
	 *
	 * @param prosesTransitori entitas yang akan ditampilkan di formulir
	 * @throws Exception jika terjadi error saat membangun komponen ZK
	 */
	private void init(final ProsesTransitori prosesTransitori) throws Exception {
		this.prosesTransitori = prosesTransitori;
		addWindow.setTitle(prosesTransitori.getId() == null ? "Tambah Proses Transitori" : "Ubah Proses Transitori");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(prosesTransitori, null, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		if (prosesTransitori.getId() == null) {

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);

			save.setTooltiptext("Simpan / Ajukan");
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
		} else {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);

		}
		borderlayout.setParent(addWindow);

		east = new East();
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setParent(borderlayout);
		initDetail(east, prosesTransitori, null);

	}

	/** Map berisi ID Transitori yang dipilih dan nilai nominalnya untuk batch baru. */
	private Map<Long, Double> longs;
	/** Komponen Footer ZK yang menampilkan total nilai Transitori terpilih. */
	private Footer footerNilai;
	/** Total nilai semua Transitori yang ditampilkan di grid detail. */
	private double totalNilaiTampil;

	/**
	 * Menginisialisasi panel detail (East) dengan grid daftar Transitori yang terhubung
	 * atau tersedia untuk dipilih ke dalam ProsesTransitori.
	 *
	 * <p><strong>Tujuan:</strong> Menampilkan daftar Transitori yang relevan (terhubung
	 * dengan proses yang sedang dibuka, atau tersedia untuk dipilih jika proses baru)
	 * beserta informasi lengkap seperti bank sumber, atas nama, nomor rekening, nilai,
	 * dan status SOP. Total nilai ditampilkan di footer.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membersihkan konten panel East, mengatur lebar 65%.
	 * Membangun Grid ZK dengan kolom-kolom yang sesuai. Untuk proses yang sudah ada (ID
	 * tidak null), kolom pertama adalah header teks dengan field pencarian dan tombol Cari.
	 * Untuk proses baru (ID null), kolom pertama adalah checkbox "Pilih Semua" yang saat
	 * dicentang akan mengambil semua Transitori tanpa proses dan menambahkannya ke Map
	 * {@code longs}. Footer menampilkan total nilai yang dipilih dan total yang ditampilkan.
	 * Method {@link #reload} dipanggil untuk mengisi baris data awal.</p>
	 *
	 * <p><strong>Parameter:</strong></p>
	 * <ul>
	 *   <li>{@code east} — komponen induk (East atau Groupbox) tempat grid ditempatkan</li>
	 *   <li>{@code prosesTransitori} — proses yang sedang dibuka</li>
	 *   <li>{@code disposisiSop} — konteks SOP jika ada, null jika bukan dari SOP</li>
	 * </ul>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari EventListener diteruskan ke
	 * framework ZK. Exception dari {@code eventListenerHitung} ditangkap secara diam-diam
	 * di {@link #reload}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada kolom baru di Transitori atau
	 * DaftarPengajuanTransfer, tambahkan MyColumnConfig baru di sini dan perbarui
	 * {@link #reload}.</p>
	 *
	 * @param east            komponen induk tempat grid detail ditempatkan
	 * @param prosesTransitori entitas ProsesTransitori yang sedang dibuka
	 * @param disposisiSop    konteks DisposisiSop jika dari alur SOP, null jika tidak
	 * @throws Exception jika terjadi error saat membangun komponen ZK
	 */
	@SuppressWarnings("unchecked")
	private void initDetail(Component east, ProsesTransitori prosesTransitori, final DisposisiSop disposisiSop)
			throws Exception {
		Common.clear(east);
		if (east instanceof East) {
			((East) east).setWidth("65%");
		}
		longs = new HashMap<Long, Double>();

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(east);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(10);

		Columns columns = new Columns();
		columns.setParent(grid);

		final Textbox cari = new Textbox();
		cari.setCols(10);

		final Rows rows = new Rows();

		if (prosesTransitori.getId() != null) {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("35%");

			Vbox vbox = new Vbox();
			vbox.setParent(column);

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama daftar transfer")));

			hbox = new Hbox();
			hbox.setParent(vbox);
			cari.setCols(10);
			hbox.appendChild(cari);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.setTooltiptext("Cari");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					reload(rows, disposisiSop, cari);

					eventListenerHitung.onEvent(null);
				}
			});
			button.setParent(hbox);

		} else {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("35%");

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Nama daftar transfer");
			column.appendChild(checkboxConfig);

			checkboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					longs.clear();

					if (checkboxConfig.isChecked()) {
						List<Object[]> longss = HibernateUtil.currentSession().createCriteria(Transitori.class)
								.createAlias("daftarPengajuanTransfer", "daftarPengajuanTransfer", Criteria.LEFT_JOIN)
								.setProjection(Projections.projectionList().add(Projections.property("id"))
										.add(Projections.property("daftarPengajuanTransfer.nominal")))
								.add(Restrictions.isNull("prosesTransitori")).list();

						for (Object[] a : longss) {
							longs.put(Long.parseLong(a[0].toString()), Double.valueOf(a[1] == null ? 0.0 : Double.parseDouble(a[1].toString())));
						}
					}

					reload(rows, disposisiSop, cari);

					eventListenerHitung.onEvent(null);
				}
			});
		}

		MyColumnConfig column = new MyColumnConfig("No. Dokumen");
		column.setParent(columns);
		column = new MyColumnConfig("Atas Nama");
		column.setParent(columns);
		column = new MyColumnConfig("No. Rekening");
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig("Nilai");
		column.setAlign("right");
		column.setParent(columns);

		column = new MyColumnConfig("SOP");
		column.setWidth("20%");
		column.setParent(columns);

		rows.setParent(grid);

		reload(rows, disposisiSop, cari);

		Foot foot = new Foot();

		foot.setParent(grid);

		Footer footer = new Footer("Total Nilai");
		footer.setSpan(3);
		footer.setStyle(
				"font-weight:700; color:#065f46; background:#ecfdf5; border-top:1px solid #bbf7d0; padding:10px 12px; min-height:34px; line-height:20px; white-space:nowrap;");
		foot.appendChild(footer);

		footerNilai = new Footer("");
		footerNilai.setSpan(3);
		footerNilai.setStyle(styleFooterTotalNilai());
		foot.appendChild(footerNilai);

		eventListenerHitung.onEvent(null);
	}

	/**
	 * EventListener untuk menghitung ulang total nilai Transitori yang dipilih dan
	 * memperbarui tampilan footer.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan feedback real-time kepada pengguna mengenai
	 * total nilai yang akan diproses setiap kali pilihan Transitori berubah.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menjumlahkan semua nilai di Map {@code longs} dan
	 * memanggil {@link #updateFooterNilai} dengan total tersebut. Dapat dipanggil dengan
	 * event null untuk sekadar memperbarui tampilan footer.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Listener ini dipanggil dari checkbox pilih,
	 * tombol Cari, dan saat inisialisasi.</p>
	 */
	private EventListener eventListenerHitung = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			double n = 0.0;

			if (longs != null) {
				for (Double nn : longs.values()) {
					n += nilai(nn);
				}
			}
			updateFooterNilai(n);
		}

	};

	/** DisposisiSop dari konteks SOP aktif, null jika bukan dari alur SOP. */
	private DisposisiSop disposisiSop = null;
	/** Komponen datebox untuk tanggal pembuatan proses. */
	private MyDatebox tanggalPembuatan;
	/** Checkbox untuk status persetujuan dalam mode SOP. */
	private MyCheckboxConfig setujuiOleh;


	/**
	 * Mengembalikan nilai String yang null-safe, mengganti null dengan string kosong.
	 *
	 * <p><strong>Tujuan:</strong> Helper null-safe untuk string agar tidak terjadi
	 * NullPointerException saat merender teks komponen ZK Label atau A.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Jika value null, kembalikan ""; jika tidak, kembalikan
	 * value as-is.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak melempar exception; null-safe.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Helper statis private yang dapat diganti dengan
	 * library utility jika tersedia.</p>
	 *
	 * @param value nilai String yang mungkin null
	 * @return value atau "" jika null
	 */
	private static String text(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Memeriksa apakah suatu nilai Boolean bernilai true secara null-safe.
	 *
	 * <p><strong>Tujuan:</strong> Menghindari NullPointerException saat memeriksa
	 * nilai Boolean yang mungkin null dari field entitas Hibernate.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menggunakan {@code Boolean.TRUE.equals(value)}
	 * yang secara implisit aman terhadap null (mengembalikan false jika value null).</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak melempar exception; null-safe.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Helper statis private untuk konversi boolean.</p>
	 *
	 * @param value nilai Boolean yang mungkin null
	 * @return true hanya jika value adalah Boolean.TRUE
	 */
	private static boolean isTrue(Boolean value) {
		return Boolean.TRUE.equals(value);
	}

	/**
	 * Mengkonversi nilai Double ke double primitif secara null-safe dengan fallback 0.0.
	 *
	 * <p><strong>Tujuan:</strong> Menghindari NullPointerException saat melakukan operasi
	 * aritmetika pada nilai nominal yang mungkin null dari DaftarPengajuanTransfer.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Jika value null, kembalikan 0.0; jika tidak,
	 * kembalikan {@code value.doubleValue()}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak melempar exception; null-safe.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Helper statis private untuk konversi numerik.</p>
	 *
	 * @param value nilai Double yang mungkin null
	 * @return nilai double, atau 0.0 jika null
	 */
	private static double nilai(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	/**
	 * Memformat nilai numerik double menjadi string angka dengan format lokal Indonesia.
	 *
	 * <p><strong>Tujuan:</strong> Menyeragamkan format tampilan nilai uang/nominal di
	 * seluruh antarmuka proses transitori menggunakan format angka yang sudah dikonfigurasi
	 * di {@code Common.numberFormat}.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menggunakan {@code Common.numberFormat.get().format(value)}
	 * yang merupakan ThreadLocal NumberFormat, sehingga aman digunakan dalam lingkungan
	 * multi-thread ZK.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak melempar exception untuk nilai normal;
	 * format NaN atau Infinity akan mengikuti perilaku NumberFormat standar.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Format angka dikonfigurasi secara global di Common;
	 * tidak perlu diubah di sini.</p>
	 *
	 * @param value nilai double yang akan diformat
	 * @return string angka terformat dengan pemisah ribuan dan desimal
	 */
	private static String formatNilai(double value) {
		return Common.numberFormat.get().format(value);
	}

	/**
	 * Memperbarui label komponen footer yang menampilkan total nilai Transitori.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan informasi real-time kepada pengguna mengenai
	 * total nilai yang telah dipilih (untuk proses baru) atau total nilai semua Transitori
	 * terkait (untuk proses yang sudah ada).</p>
	 *
	 * <p><strong>Cara kerja:</strong> Jika proses baru (ID null), label menampilkan
	 * "Dipilih: [nilai terpilih] | Tampil: [total tampil]". Jika proses sudah ada,
	 * label menampilkan "Total: [total tampil]". Setelah memperbarui label, gaya CSS
	 * footer juga diperbarui menggunakan {@link #styleFooterTotalNilai}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika {@code footerNilai} null (misalnya
	 * karena panel belum diinisialisasi), metode langsung return.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Format label dapat disesuaikan di sini untuk
	 * menambahkan informasi tambahan seperti jumlah item.</p>
	 *
	 * @param nilaiDipilih total nilai Transitori yang dipilih (untuk proses baru)
	 */
	private void updateFooterNilai(double nilaiDipilih) {
		if (footerNilai == null) {
			return;
		}

		if (prosesTransitori != null && prosesTransitori.getId() == null) {
			footerNilai.setLabel("Dipilih: " + formatNilai(nilaiDipilih) + "    |    Tampil: "
					+ formatNilai(totalNilaiTampil));
		} else {
			footerNilai.setLabel("Total: " + formatNilai(totalNilaiTampil));
		}
		footerNilai.setStyle(styleFooterTotalNilai());
	}

	/**
	 * Mengembalikan string gaya CSS untuk komponen footer total nilai.
	 *
	 * <p><strong>Tujuan:</strong> Menyatukan definisi gaya CSS footer dalam satu metode
	 * agar konsisten dan mudah diubah tanpa perlu mencari di berbagai lokasi kode.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Mengembalikan string CSS langsung dengan properti
	 * text-align, font-weight, warna, background, border, padding, dan whitespace yang
	 * memberikan tampilan footer yang menonjol dan mudah dibaca.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Sesuaikan warna dan styling di sini jika ada
	 * perubahan tema visual aplikasi.</p>
	 *
	 * @return string CSS untuk styling footer total nilai
	 */
	private String styleFooterTotalNilai() {
		return "text-align:right; font-weight:700; color:#0f172a; background:#ecfdf5; "
				+ "border-top:1px solid #bbf7d0; padding:10px 14px; min-height:34px; "
				+ "line-height:20px; white-space:nowrap; overflow:visible;";
	}


	/**
	 * Memuat ulang isi grid detail Transitori berdasarkan kata kunci pencarian dan
	 * kondisi proses yang sedang dibuka.
	 *
	 * <p><strong>Tujuan:</strong> Menampilkan daftar Transitori yang relevan di panel
	 * detail, baik Transitori yang sudah terkait dengan proses ini (untuk proses yang
	 * ada) maupun Transitori yang belum terkait proses apapun (untuk proses baru).
	 * Setiap baris menampilkan informasi bank, atas nama, no. rekening, nilai, dan
	 * link SOP.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membersihkan baris lama dari {@code rows} dan mereset
	 * {@code totalNilaiTampil}. Query Transitori dibatasi berdasarkan kata kunci (pencarian
	 * ilike pada keterangan, nama, kode) dan kondisi prosesTransitori (terkait proses ini
	 * untuk yang ada, atau null untuk proses baru). Untuk setiap Transitori, dibuat baris
	 * dengan komponen yang berbeda tergantung mode: mode lihat (sudah disetujui atau
	 * persetujuan) menggunakan Label + RevisiHelper; mode pilih (proses baru) menggunakan
	 * Checkbox yang terhubung ke Map {@code longs}. Informasi DaftarPengajuanTransfer
	 * ditampilkan jika ada. Di akhir, {@code eventListenerHitung} dipanggil untuk
	 * memperbarui total footer.</p>
	 *
	 * <p><strong>Parameter:</strong></p>
	 * <ul>
	 *   <li>{@code rows} — komponen Rows ZK yang akan diisi baris baru</li>
	 *   <li>{@code disposisiSop} — konteks SOP jika ada, null jika tidak</li>
	 *   <li>{@code cari} — komponen Textbox filter kata kunci, dapat null</li>
	 * </ul>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari {@code eventListenerHitung}
	 * ditangkap dan ditampilkan ke admin. List Transitori di-clear setelah diproses
	 * untuk membantu GC (garbage collector).</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada kolom baru di DaftarPengajuanTransfer
	 * atau Transitori yang perlu ditampilkan, tambahkan di blok render baris di sini.</p>
	 *
	 * @param rows         komponen Rows ZK target
	 * @param disposisiSop konteks SOP aktif, atau null
	 * @param cari         komponen Textbox kata kunci pencarian, dapat null
	 */
	@SuppressWarnings("unchecked")
	private void reload(final Rows rows, DisposisiSop disposisiSop, Textbox cari) {
		Common.clear(rows);
		totalNilaiTampil = 0.0;
		if (longs == null) {
			longs = new HashMap<Long, Double>();
		}

		String kataKunci = cari == null || cari.getValue() == null ? "" : cari.getValue().trim();
		List<Transitori> transitoris = HibernateUtil.currentSession().createCriteria(Transitori.class)
				.add(kataKunci.length() == 0 ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("keterangan", kataKunci, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", kataKunci, MatchMode.ANYWHERE),
										Restrictions.ilike("kode", kataKunci, MatchMode.ANYWHERE))))
				.add(prosesTransitori.getId() != null ? Restrictions.eq("prosesTransitori", prosesTransitori)
						: Restrictions.isNull("prosesTransitori"))
				.addOrder(Order.asc("nama")).list();

		for (final Transitori transitori : transitoris) {
			final ais.database.model.akunting.DaftarPengajuanTransfer daftar = transitori.getDaftarPengajuanTransfer();
			final Long iddata = transitori.getId();
			final Double n = Double.valueOf(daftar == null ? 0.0 : nilai(daftar.getNominal()));
			totalNilaiTampil += nilai(n);

			if (prosesTransitori.getId() != null && iddata != null) {
				longs.put(iddata, n);
			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			if (prosesTransitori.getId() != null && (prosesTransitori.getDisetujuiOleh() != null || persetujuan)) {
				try {
					RevisiHelper.createNewRevisi(Transitori.class, transitori, text(transitori.getNama())).setParent(row);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					new MyLabelKecil(text(transitori.getNama())).setParent(row);
				}
			} else {
				final Checkbox c;
				row.appendChild(c = new Checkbox(text(transitori.getNama())));
				c.setChecked(longs.keySet().contains(transitori.getId()));

				c.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (c.isChecked()) {
							longs.put(iddata, n);
						} else {
							longs.remove(iddata);
						}

						eventListenerHitung.onEvent(null);
					}
				});
			}

			if (daftar == null) {
				new MyLabelKecil("Data asal tidak ditemukan").setParent(row);
				new MyLabelKecil("-").setParent(row);
				new MyLabelKecil("-").setParent(row);
				new MyLabelKecil(formatNilai(0.0)).setParent(row);
				new MyLabelKecil("-").setParent(row);
				continue;
			}

			Vbox a = new Vbox();
			a.setParent(row);
			a.appendChild(new MyLabelKecil(daftar.getBankSumber() == null ? "Belum ditentukan"
					: text(daftar.getBankSumber().getNama())));

			if (daftar.getProsesTransfer() != null) {
				A aa = new A(text(daftar.getProsesTransfer().getKode()));
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, daftar.getProsesTransfer());
					}
				});
				aa.setStyle("font-size:12px;");
				aa.setParent(a);
			}

			row.appendChild(new MyLabelKecil(text(daftar.getAtasNamaSumber())));
			row.appendChild(new MyLabelKecil(daftar.getNoRekSumber() == null ? "Belum ditentukan"
					: text(daftar.getNoRekSumber())));
			row.appendChild(new MyLabelKecil(formatNilai(nilai(daftar.getNominal()))));

			if (daftar.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(row);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + text(daftar.getDisposisiSop().getKeterangan())
						+ (daftar.getDisposisiSop().getSop() == null ? ""
								: " (" + text(daftar.getDisposisiSop().getSop().getNama()) + ")"));
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(daftar.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			} else {
				new MyLabelKecil("-").setParent(row);
			}
		}

		if (transitoris != null) {
			transitoris.clear();
		}

		try {
			eventListenerHitung.onEvent(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memvalidasi dan menyimpan data formulir Proses Transitori ke database, termasuk
	 * mengasosiasikan Transitori yang dipilih ke proses ini.
	 *
	 * <p><strong>Tujuan:</strong> Melakukan validasi input wajib, menyimpan entitas
	 * {@link ProsesTransitori} baru atau yang diubah, kemudian mengasosiasikan semua
	 * entitas {@link Transitori} yang dipilih (dari Map {@code longs}) ke proses ini
	 * dan menghitung total nilai.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Validasi: judul tidak boleh kosong dan minimal
	 * satu Transitori harus dipilih. Kemudian:
	 * <ol>
	 *   <li>Load ulang entitas dari session jika sudah ada (untuk menghindari stale data).</li>
	 *   <li>Set field-field dari komponen formulir: nama, keterangan, tanggal pembuatan.</li>
	 *   <li>Jika checkbox setujuiOleh dicentang, set disetujuiOleh dan tanggalPersetujuan.</li>
	 *   <li>Set DisposisiSop jika ada dari konteks SOP.</li>
	 *   <li>Simpan entitas ProsesTransitori dengan refreshSaveOrUpdate dan flush.</li>
	 *   <li>Query Transitori berdasarkan ID di Map {@code longs}, set prosesTransitori
	 *       ke masing-masing, update, dan hitung total nilai.</li>
	 *   <li>Update kembali ProsesTransitori dengan total nilai.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><strong>Return:</strong> {@code true} jika berhasil, {@code false} jika validasi
	 * gagal.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception database diteruskan ke framework ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada field wajib baru, tambahkan validasi.
	 * Pastikan total nilai selalu dihitung ulang dari Transitori yang terkait.</p>
	 *
	 * @param event event ZK dari klik tombol Simpan
	 * @return true jika berhasil disimpan, false jika validasi gagal
	 * @throws Exception jika terjadi error database saat penyimpanan
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Proses Transitori belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Proses dengan nama proses transitori yang sesuai; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (longs.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Daftar Transitori belum dipilih. Langkah yang dapat dilakukan: (1) Centang minimal satu transaksi dari daftar transitori yang tersedia; (2) Pastikan transaksi transitori sudah ada dan berstatus siap diproses; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (prosesTransitori.getId() != null) {
			prosesTransitori = (ProsesTransitori) session.load(ProsesTransitori.class, prosesTransitori.getId());

		}

		prosesTransitori.setNama(nama.getValue());
		prosesTransitori.setKeterangan(keterangan.getValue());
		prosesTransitori.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (setujuiOleh.isChecked()) {
			prosesTransitori.setDisetujuiOleh(Common.getCurrentUser());
			prosesTransitori.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		} else {
			prosesTransitori.setDisetujuiOleh(null);
			prosesTransitori.setTanggalPersetujuan(null);
		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			prosesTransitori.setDisposisiSop(disposisiSop);
		}

		Common.refreshSaveOrUpdate(session, prosesTransitori);
		session.flush();

		List<Transitori> transitoris = HibernateUtil.currentSession().createCriteria(Transitori.class)
				.add(longs.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longs.keySet()))
				.list();

		Double nilaiTotal = 0.0;
		for (Transitori transitori : transitoris) {
			transitori.setProsesTransitori(prosesTransitori);

			Common.refreshUpdate(session, transitori);

			nilaiTotal += transitori.getDaftarPengajuanTransfer() == null ? 0.0
					: nilai(transitori.getDaftarPengajuanTransfer().getNominal());

		}

		prosesTransitori.setNilai(nilaiTotal);

		Common.refreshUpdate(session, prosesTransitori);

		session.flush();
		return true;
	}

	/**
	 * Membangun Hibernate {@link Criteria} untuk query {@link ProsesTransitori} berdasarkan
	 * filter tanggal, status aktif, dan nama.
	 *
	 * <p><strong>Tujuan:</strong> Menyatukan logika pembangunan query pencarian Proses
	 * Transitori agar dapat digunakan untuk paging maupun pengambilan data aktual.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Filter berantai:
	 * <ol>
	 *   <li>Filter rentang tanggal menggunakan SQL date() antara {@code start} dan
	 *       {@code end} (jika keduanya tersedia).</li>
	 *   <li>Filter aktif: hanya tampilkan yang aktif (null atau true) jika checkbox
	 *       aktif dicentang.</li>
	 *   <li>Filter nama: ilike ANYWHERE jika field searchnama tidak kosong.</li>
	 * </ol>
	 * Jika {@code order} true, menambahkan pengurutan berdasarkan ID descending
	 * (terbaru di atas).</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception Hibernate diteruskan ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada filter baru (misalnya berdasarkan status
	 * persetujuan), tambahkan Restriction di sini.</p>
	 *
	 * @param order true untuk menambahkan pengurutan berdasarkan ID desc
	 * @return Criteria Hibernate yang sudah dikonfigurasi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ProsesTransitori.class)

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * Memuat ulang data grid utama Proses Transitori sesuai filter dan halaman paging aktif.
	 *
	 * <p><strong>Tujuan:</strong> Menjadi handler utama untuk pembaruan tampilan daftar
	 * ProsesTransitori, baik saat inisialisasi halaman, perubahan filter, perpindahan
	 * halaman, maupun setelah operasi simpan/persetujuan/pembatalan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memeriksa ketersediaan {@code searchnama} untuk
	 * memastikan komponen sudah siap. Mereset flag {@code persetujuan} ke false.
	 * Menghitung total untuk paging, kemudian mengambil data halaman aktif dan memuat
	 * ke grid dengan renderer {@link ProsesTransitoriRenderer}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika {@code searchnama} null (halaman belum
	 * siap), metode langsung return tanpa aksi.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Perubahan pada jumlah item per halaman dilakukan
	 * di {@code Common.ROWS_COUNT_ON_PAGE}.</p>
	 *
	 * @param event event ZK pemicu (dapat null untuk panggilan programatik)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		persetujuan = false;
		Common.initPaging(initCriteria(false), paging);

		List<ProsesTransitori> prosesTransitori = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(prosesTransitori);
		grid.setRowRenderer(new ProsesTransitoriRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Implementasi antarmuka {@link FormSop} untuk membangun formulir Proses Transitori
	 * yang dapat diintegrasikan ke dalam alur persetujuan SOP.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan formulir yang dapat disematkan dalam
	 * halaman SOP sehingga proses persetujuan Proses Transitori dapat dilakukan dalam
	 * konteks alur kerja SOP yang lebih luas.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membangun MyGrid dengan kolom dua (label kiri 30%,
	 * konten kanan) dan baris-baris formulir:
	 * <ol>
	 *   <li>Tanggal Pengajuan: jika sudah disetujui tampilkan sebagai Label, jika belum
	 *       tampilkan sebagai MyDatebox (readonly).</li>
	 *   <li>Judul Transitori: jika sudah disetujui Label, jika belum Textbox multi-baris.</li>
	 *   <li>Keterangan: jika sudah disetujui Label; jika proses sudah ada (ID tidak null)
	 *       ditambahkan listener onChange untuk auto-save keterangan.</li>
	 *   <li>Status Persetujuan: ditampilkan hanya jika dalam mode persetujuan SOP dan
	 *       tidak ada DisposisiSop aktif. Checkbox setujuiOleh terhubung ke callback
	 *       {@code setujui} jika diberikan.</li>
	 * </ol>
	 * Jika ada DisposisiSop, ditambahkan baris yang menampilkan detail Transitori
	 * terkait dalam Groupbox bertinggi 400px. Jika proses sudah disetujui atau dalam
	 * mode persetujuan, form dikunci dengan {@code Common.freezeGanti}.</p>
	 *
	 * <p><strong>Parameter:</strong></p>
	 * <ul>
	 *   <li>{@code generalValueObject} — ProsesTransitori yang akan ditampilkan</li>
	 *   <li>{@code disposisiSop} — konteks SOP aktif, null jika tidak dari SOP</li>
	 *   <li>{@code save} — tombol simpan dari caller (listener ditambahkan di sini jika
	 *       proses baru)</li>
	 *   <li>{@code setujui} — EventListener callback persetujuan dari SOP, null jika
	 *       tidak ada</li>
	 * </ul>
	 *
	 * <p><strong>Return:</strong> MyGrid yang berisi seluruh formulir, siap ditambahkan
	 * ke komponen induk oleh caller.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception diteruskan ke framework ZK.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini merupakan kontrak antarmuka FormSop.
	 * Jika ada field baru, tambahkan baris baru di sini dan perbarui {@link #onSave}.</p>
	 *
	 * @param generalValueObject ProsesTransitori yang akan ditampilkan di formulir
	 * @param disposisiSop       konteks DisposisiSop dari SOP, null jika tidak ada
	 * @param save               komponen tombol simpan yang disediakan caller
	 * @param setujui            EventListener persetujuan SOP, null jika tidak ada
	 * @return MyGrid berisi seluruh komponen formulir
	 * @throws Exception jika terjadi error saat membangun komponen ZK
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujui) throws Exception {

		this.prosesTransitori = (ProsesTransitori) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan *"));

		tanggalPembuatan = new MyDatebox(prosesTransitori.getTanggalPembuatan());

		if (prosesTransitori.getDisetujuiOleh() != null) {
			row.appendChild(new Label(Common.dateFormat.get().format(prosesTransitori.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Transitori *"));
		nama = new Textbox(text(prosesTransitori.getNama()));
		if (prosesTransitori.getDisetujuiOleh() != null) {
			row.appendChild(new Label(prosesTransitori.getNama()));
		} else {
			row.appendChild(nama);
		}
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));

		keterangan = new Textbox(text(prosesTransitori.getKeterangan()));
		if (prosesTransitori.getDisetujuiOleh() != null) {
			row.appendChild(new Label(prosesTransitori.getKeterangan()));
		} else {
			row.appendChild(keterangan);

			if (prosesTransitori.getId() != null) {
				EventListener eventListenerEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						prosesTransitori.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(prosesTransitori);

					}
				};
				keterangan.addEventListener("onChange", eventListenerEventListener);
			}
		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setVisible(persetujuan && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(setujuiOleh = new MyCheckboxConfig("Setujui"));
		setujuiOleh.setChecked(prosesTransitori.getDisetujuiOleh() != null);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox && setujuiOleh != arg0.getTarget()) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");
					if (selesai != null && selesai) {
						setujuiOleh.setChecked(true);
						setujuiOleh.setDisabled(true);
					} else {
						setujuiOleh.setChecked(false);
						setujuiOleh.setDisabled(false);
					}
				}
			}
		});

		if (setujui != null) {
			setujuiOleh.addEventListener("onClick", setujui);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujui.onEvent(new Event("", null, prosesTransitori.getDisetujuiOleh() != null));
				}
			});
		}

		if (prosesTransitori.getDisetujuiOleh() != null || persetujuan) {
			if (prosesTransitori.getId() != null) {
				Common.freezeGanti(rows, true);
			}
		}

		if (disposisiSop != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.setParent(row);
			groupboxStyled.setHeight("400px");
			groupboxStyled.appendChild(new MyCaptionStyled("Daftar Transitori"));

			initDetail(groupboxStyled, prosesTransitori, disposisiSop);
		}

		return grid;
	}

	/**
	 * Implementasi {@link FormSop#istilah()} untuk mengembalikan nama istilah domain
	 * yang digunakan dalam antarmuka SOP.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan label yang ditampilkan dalam alur SOP
	 * untuk mengidentifikasi jenis dokumen yang sedang diproses.</p>
	 *
	 * <p><strong>Return:</strong> String "Proses Pengajuan Transitori" yang merupakan
	 * nama istilah domain untuk proses ini dalam konteks SOP.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Ubah jika terminologi domain berubah.</p>
	 *
	 * @return nama istilah domain "Proses Pengajuan Transitori"
	 * @throws Exception tidak dilempar
	 */
	@Override
	public String istilah() throws Exception {
		return "Proses Pengajuan Transitori";
	}

	/**
	 * Implementasi {@link FormSop#ambil()} untuk mengembalikan entitas {@link DataSop}
	 * yang sedang dikelola oleh form ini.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan framework SOP mengambil referensi ke
	 * entitas ProsesTransitori yang sedang ditampilkan untuk keperluan pembuatan
	 * disposisi dan penelusuran alur SOP.</p>
	 *
	 * <p><strong>Return:</strong> Entitas {@link ProsesTransitori} yang sedang aktif
	 * di form ini.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini merupakan kontrak antarmuka FormSop.</p>
	 *
	 * @return entitas ProsesTransitori aktif sebagai DataSop
	 * @throws Exception tidak dilempar
	 */
	@Override
	public DataSop ambil() throws Exception {
		return prosesTransitori;
	}

	/**
	 * Implementasi {@link FormSop#ambilClass()} untuk mengembalikan kelas Java dari
	 * entitas yang dikelola form ini.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan framework SOP mengetahui tipe entitas
	 * yang dikelola untuk keperluan refleksi dan pemrosesan generik.</p>
	 *
	 * <p><strong>Return:</strong> {@code ProsesTransitori.class}.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini merupakan kontrak antarmuka FormSop.</p>
	 *
	 * @return Class ProsesTransitori
	 * @throws Exception tidak dilempar
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return ProsesTransitori.class;
	}

	/**
	 * Implementasi {@link FormSop#setPersetujuan(boolean)} untuk mengubah mode formulir
	 * ke mode persetujuan SOP.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan framework SOP mengaktifkan mode persetujuan
	 * pada form ini sehingga tampilan dan perilaku formulir disesuaikan untuk alur
	 * persetujuan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menyimpan nilai {@code persetujuan} ke field instance,
	 * yang akan digunakan oleh {@link #form} untuk mengatur visibilitas baris status
	 * persetujuan dan penguncian form.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini merupakan kontrak antarmuka FormSop.</p>
	 *
	 * @param persetujuan true untuk mengaktifkan mode persetujuan SOP
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Implementasi {@link FormSop#cetakData(GeneralValueObject)} untuk menghasilkan file
	 * cetak dari data ProsesTransitori.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan implementasi antarmuka FormSop untuk fitur
	 * cetak. Saat ini tidak ada laporan cetak khusus untuk Proses Transitori.</p>
	 *
	 * <p><strong>Return:</strong> null — fitur cetak belum diimplementasikan untuk
	 * entitas ini.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika perlu menambahkan fitur cetak, implementasikan
	 * logika pembuatan file (misal PDF/Excel) di sini dan kembalikan File hasilnya.</p>
	 *
	 * @param generalValueObject entitas ProsesTransitori yang akan dicetak
	 * @return null (belum diimplementasikan)
	 * @throws Exception tidak dilempar saat ini
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		return null;
	}
}
