package ais.action.master.akunting;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataDaftarPengajuanTransferBanyak;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.CaraPembayaranTransfer;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.ProsesTransfer;
import ais.database.model.akunting.Transitori;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
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
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>ProsesTransferAction — Pengelola Proses Transfer Dana</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini merupakan controller untuk modul "Proses Transfer" dalam
 * sistem akuntansi keuangan eCampus. Modul ini mengelola alur kerja transfer bank dari tahap
 * pengajuan hingga realisasi: staf keuangan membuat entri {@link ais.database.model.akunting.ProsesTransfer}
 * yang berisi kumpulan {@link ais.database.model.akunting.DaftarPengajuanTransfer} yang
 * perlu dibayarkan, kemudian penyetuju memberikan persetujuan, dan akhirnya petugas bank
 * menandai transfer sebagai "terealisasi".</p>
 *
 * <p><b>Cara kerja:</b> Kelas mengimplementasikan empat antarmuka sekaligus: {@code DataCriteria}
 * untuk query Hibernate, {@code DataSearchDefault} untuk refresh grid, {@code DataInitDefault}
 * untuk inisialisasi form dari luar, dan {@code FormSop} untuk integrasi alur persetujuan SOP.
 * Pada saat {@link #doAfterCompose(Component)}, komponen ZK di-wire otomatis, tanggal filter
 * diinisialisasi (6 bulan ke belakang), izin CRUD/APPROVE/REJECT dibaca, dan grid diisi
 * pertama kali. Renderer baris ({@code ProsesTransferRenderer}) merender setiap
 * {@code ProsesTransfer} dengan tombol aksi persetujuan, pembatalan, dan realisasi.</p>
 *
 * <p><b>Alur pengajuan transfer:</b>
 * <ol>
 *   <li>Staf membuka formulir baru via {@link #onAdd(Event)} — memilih DaftarPengajuanTransfer
 *       yang akan diproses dengan centang checkbox, sistem menghitung total nilai otomatis.</li>
 *   <li>Setelah disimpan ({@link #onSave(Event)}), sistem membuat kode transfer otomatis,
 *       mengaitkan semua DaftarPengajuanTransfer yang dipilih ke ProsesTransfer, dan
 *       langsung mencetak dokumen pengajuan.</li>
 *   <li>Penyetuju (role APPROVE) mengklik tombol "Persetujuan" di baris grid — mengisi
 *       {@code disetujuiOleh} dan {@code tanggalPersetujuan}.</li>
 *   <li>Setelah disetujui, petugas bank membuka detail dan mengklik "Realisasikan" —
 *       mengisi {@code realisasikanOleh} dan {@code tanggalRealisasikan}.</li>
 * </ol>
 * </p>
 *
 * <p><b>Panel detail (East):</b> Sisi kanan form menampilkan daftar {@code DaftarPengajuanTransfer}
 * dengan filter multi-kategori (Uang Muka, LPJ, Kas Besar, Kas Kecil, Pengadaan, Termin, DP,
 * Diskon, Pajak). PPh tagihan ditampilkan sebagai baris tersendiri dengan nilai netto di baris
 * utama. Nilai total dihitung realtime di footer grid.</p>
 *
 * <p><b>Threading:</b> Tidak ada thread latar — semua operasi berjalan di UI thread ZK
 * dengan sesi Hibernate saat ini. Operasi simpan menggunakan {@code HibernateUtil.currentSession()}
 * dan {@code session.flush()} untuk memastikan data tersimpan sebelum query berikutnya.</p>
 *
 * <p><b>Pemeliharaan:</b> Kelas menggunakan pola {@code FormSop} — metode {@link #form}
 * membangun UI form yang dipakai baik di dalam modal window ({@link #init(ProsesTransfer)})
 * maupun dalam alur SOP eksternal. Pastikan field seperti {@code nama}, {@code keterangan},
 * {@code caraBayarByr}, {@code tanggalPembuatan}, {@code setujuiOleh}, {@code tanggalRealisasikan}
 * selalu tersedia setelah {@code form()} dipanggil karena diakses di {@link #onSave(Event)}.</p>
 *
 * @see ais.database.model.akunting.ProsesTransfer
 * @see ais.database.model.akunting.DaftarPengajuanTransfer
 * @see ais.action.master.akunting.helper.AmbilDataDaftarPengajuanTransferBanyak
 */
public class ProsesTransferAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {
	private static final long serialVersionUID = -5779730267402400328L;
	private static final int PAGE_SIZE = 10;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Checkbox searchaktif;
	private MyDatebox start;
	private MyDatebox end;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private boolean viewOnly = false;

	private ProsesTransfer prosesTransfer;
	private MyToolbarbuttonConfig add;

	private East east;
	private Combobox caraBayarByr;

	private boolean persetujuan = false;
	private boolean approve = false;
	private boolean reject = false;
	private EventListener eventListener = null;

	/**
	 * Konstruktor default untuk mode pengajuan biasa (bukan mode persetujuan).
	 *
	 * <p><b>Tujuan:</b> Membuat instance {@code ProsesTransferAction} dengan konfigurasi
	 * standar di mana {@code persetujuan=false}. Mode ini digunakan pada halaman "Proses
	 * Transfer" yang diakses oleh staf keuangan untuk membuat dan mengelola pengajuan
	 * transfer baru.</p>
	 *
	 * <p><b>Cara kerja:</b> Konstruktor kosong — semua inisialisasi dilakukan di
	 * {@link #doAfterCompose(Component)}. Field {@code persetujuan} secara default
	 * bernilai {@code false} sesuai deklarasi field di kelas.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Konstruktor no-arg wajib ada untuk instansiasi reflektif
	 * oleh framework ZK ketika halaman ZUL dimuat dengan atribut {@code use="..."}.</p>
	 */
	public ProsesTransferAction() {

	}

	/**
	 * Konstruktor untuk mode persetujuan dari modul SOP atau panel eksternal.
	 *
	 * <p><b>Tujuan:</b> Membuat instance dengan flag {@code persetujuan=true} yang mengubah
	 * perilaku UI: tombol Tambah disembunyikan, dan form menampilkan kontrol persetujuan
	 * ({@code setujuiOleh} checkbox). Digunakan ketika controller diinstansiasi secara
	 * programatik dari modul SOP ({@code FormSop}) atau dari panel manajemen persetujuan.</p>
	 *
	 * <p><b>Cara kerja:</b> Menyimpan {@code persetujuan} ke field instance. Nilai ini
	 * kemudian digunakan di {@link #form} untuk menentukan komponen mana yang editable
	 * dan di renderer untuk menentukan tombol mana yang ditampilkan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika menambah mode baru (mis. mode view-only saja), pertimbangkan
	 * menambahkan konstruktor atau setter terpisah daripada overloading flag {@code persetujuan}.</p>
	 *
	 * @param persetujuan {@code true} untuk mode persetujuan; {@code false} untuk mode
	 *                    pengajuan biasa
	 */
	public ProsesTransferAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Pemeriksaan keamanan sebelum halaman ZK dikomposi.
	 *
	 * <p><b>Tujuan:</b> Memvalidasi sesi pengguna sebelum komponen ZK dibuat. Dipanggil
	 * otomatis oleh framework ZK sebelum {@link #doAfterCompose(Component)}.</p>
	 *
	 * <p><b>Cara kerja:</b> Mendelegasikan ke {@code Common.doCheckSecurity()} yang
	 * memeriksa atribut sesi dan mengalihkan ke halaman login jika tidak valid. Kemudian
	 * memanggil implementasi superclass untuk melanjutkan proses komposisi normal ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Override ini wajib ada untuk memastikan keamanan halaman.
	 * Jangan hapus tanpa mengganti mekanisme autentikasi alternatif.</p>
	 *
	 * @param page     halaman ZK saat ini
	 * @param parent   komponen induk
	 * @param compInfo metadata komponen
	 * @return informasi komponen dari superclass
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi controller setelah komponen ZK selesai dikomposi.
	 *
	 * <p><b>Tujuan:</b> Metode lifecycle utama ZK yang mengatur seluruh inisialisasi:
	 * bahasa, date range default, izin CRUD/APPROVE/REJECT, toolbar cetak/upload,
	 * dan pemanggilan pencarian awal. Metode ini mempersiapkan halaman untuk digunakan
	 * oleh pengguna segera setelah semua komponen ZUL ter-wire.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Inisialisasi bahasa via {@code Common.initLaguage()}.</li>
	 *   <li>Set tanggal filter: {@code start} = 6 bulan lalu, {@code end} = besok
	 *       (keduanya readonly — pengguna memilih via datepicker).</li>
	 *   <li>Visibilitas tombol Tambah: hanya terlihat jika pengguna punya hak CREATE.</li>
	 *   <li>Baca izin: {@code approve}, {@code reject}, {@code edit}, {@code delete}
	 *       dari {@code CommonPrivilages}.</li>
	 *   <li>Panggil {@link #onSearchDefault(Event)} untuk load data awal grid.</li>
	 *   <li>Inisialisasi paging dengan handler event untuk reload saat halaman berubah.</li>
	 *   <li>Pasang tombol Cetak (cetak data ProsesTransfer ke format konfigurasi).</li>
	 *   <li>Pasang tombol Upload data Excel (hanya jika punya hak CREATE+UPDATE+DELETE).</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error eksplisit — exception dari
	 * inisialisasi komponen diteruskan ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan inisialisasi penting — {@code onSearchDefault} harus
	 * dipanggil setelah paging diinisialisasi agar navigasi halaman berfungsi dengan benar.
	 * Konstanta {@code PAGE_SIZE=10} menentukan ukuran halaman grid.</p>
	 *
	 * @param comp komponen root halaman ZK
	 * @throws Exception jika inisialisasi komponen atau Hibernate gagal
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
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		if (paging != null) {
			paging.setPageSize(PAGE_SIZE);
		}

		String[] contents = new String[] { "id", "kode", "tanggalPembuatan", "waktuTransfer", "nama", "keterangan",
				"bankSumber", "noRekSumber", "waktuTransfer", "realisasikanOleh", "tanggalRealisasikan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ProsesTransfer.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ProsesTransfer.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class ProsesTransferRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final ProsesTransfer prosesTransfer = (ProsesTransfer) arg1;

			if (prosesTransfer.getKode() == null || prosesTransfer.getKode().trim().isEmpty()) {
				String noAgenda = generateCode(true);
				prosesTransfer.setKode(noAgenda);

				Common.refreshUpdate(prosesTransfer);
			}

			Vbox aaa = new Vbox();
			aaa.setParent(arg0);
			new Label(text(prosesTransfer.getKode())).setParent(aaa);
			new Label(formatTanggal(prosesTransfer.getTanggalPembuatan())).setParent(aaa);

			aaa = new Vbox();
			aaa.setParent(arg0);

			if (prosesTransfer.getDisetujuiOleh() != null) {
				new Label(prosesTransfer.getDisetujuiOleh().getUserNama()).setParent(aaa);
			}

			new Label(formatTanggal(prosesTransfer.getTanggalPersetujuan() != null ? prosesTransfer.getTanggalPersetujuan()
					: prosesTransfer.getTanggalPembuatan())).setParent(aaa);

			aaa = new Vbox();
			aaa.setParent(arg0);

			if (prosesTransfer.getRealisasikanOleh() != null) {
				new Label(prosesTransfer.getRealisasikanOleh().getUserNama()).setParent(aaa);
			}

			new Label(formatTanggal(prosesTransfer.getTanggalRealisasikan())).setParent(aaa);

			RevisiHelper.createNewRevisi(ProsesTransfer.class, prosesTransfer, text(prosesTransfer.getNama()))
					.setParent(arg0);

			new Label(prosesTransfer.getCaraPembayaranTransfer() == null ? ""
					: text(prosesTransfer.getCaraPembayaranTransfer().getNama())).setParent(arg0);

			new Label(formatNilai(nilai(prosesTransfer.getNilai()))).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(text(prosesTransfer.getKeterangan())).setParent(a);

			if (prosesTransfer.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + text(prosesTransfer.getDisposisiSop().getKeterangan())
						+ (prosesTransfer.getDisposisiSop().getSop() == null ? ""
								: " (" + text(prosesTransfer.getDisposisiSop().getSop().getNama()) + ")"));
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(prosesTransfer.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit && prosesTransfer.getDisetujuiOleh() == null);
			checkbox.setChecked(isTrue(prosesTransfer.getAktif()));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					prosesTransfer.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(prosesTransfer);
				}
			});

			Hbox aa;
			(aa = Common.copyEditDeleteButtons(edit, false, prosesTransfer.getDisetujuiOleh() == null && delete,
					prosesTransfer, ProsesTransferAction.this)).setParent(arg0);
			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			aa.appendChild(disetujui);
			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			aa.appendChild(dibatalkan);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(prosesTransfer);
				}

			});
			button.setParent(aa);

			disetujui.setVisible(approve && prosesTransfer.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && prosesTransfer.getDisetujuiOleh() != null);

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

										prosesTransfer.setDisetujuiOleh(Common.getCurrentUser());
										prosesTransfer.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, prosesTransfer);
										session.flush();

										checkbox.setDisabled(!edit && prosesTransfer.getDisetujuiOleh() == null);

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

										prosesTransfer.setDisetujuiOleh(null);
										prosesTransfer.setTanggalPersetujuan(null);
										Common.refreshUpdate(session, prosesTransfer);
										session.flush();

										// ② BEBASKAN pengajuan yang menempel agar tidak "nyangkut" selamanya di
										// status "Sudah diajukan": kembalikan prosesTransfer=null (→ "Belum
										// diproses") sehingga bisa diproses ulang di Proses Transfer lain.
										// HANYA bila BELUM direalisasi (dana belum cair) — bila sudah cair,
										// jangan dilepas.
										if (prosesTransfer.getRealisasikanOleh() == null) {
											try {
												java.util.List<DaftarPengajuanTransfer> nempel = session
														.createCriteria(DaftarPengajuanTransfer.class)
														.add(Restrictions.eq("prosesTransfer", prosesTransfer)).list();
												for (DaftarPengajuanTransfer d : nempel) {
													d.setProsesTransfer(null);
													d.setTransfer(null);
													d.setTransitori(null);
													Common.refreshUpdate(session, d);
												}
												session.flush();
											} catch (Exception exFree) {
												Common.tampilErrorJikaAdmin(exFree);
											}
										}

										checkbox.setDisabled(!edit && prosesTransfer.getDisetujuiOleh() == null);

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

			if (prosesTransfer.getDisetujuiOleh() != null) {
				MyToolbarbuttonConfig disetujuia = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
				aa.appendChild(disetujuia);
				disetujuia.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(prosesTransfer);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				disetujuia.setParent(aa);

			}
		}

	}

	/**
	 * Membuka formulir tambah proses transfer baru.
	 *
	 * <p><b>Tujuan:</b> Event handler untuk tombol "Tambah" di toolbar yang membuka modal
	 * window untuk pembuatan {@link ais.database.model.akunting.ProsesTransfer} baru.
	 * Sebelum membuka form, metode mereset flag {@code viewOnly=false} dan
	 * {@code persetujuan=false} agar form dalam mode edit penuh.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat instance {@code ProsesTransfer} baru (entitas kosong),
	 * memanggil {@link #init(ProsesTransfer)} untuk membangun UI form, lalu menampilkan
	 * {@code addWindow} sebagai modal. Form menampilkan daftar {@code DaftarPengajuanTransfer}
	 * yang tersedia (belum dikaitkan ke ProsesTransfer) dengan checkbox pemilihan dan
	 * kalkulasi total nilai realtime.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tombol Tambah hanya terlihat jika pengguna memiliki hak
	 * {@code CREATE}. Jika halaman dalam mode persetujuan (parameter URL {@code persetujuan}),
	 * tombol ini tersembunyi di {@code doAfterCompose}.</p>
	 *
	 * @param event event ZK dari klik tombol Tambah
	 * @throws Exception jika pembuatan form atau modal window gagal
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		persetujuan = false;
		init(new ProsesTransfer());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Inisialisasi form dari entitas {@code GeneralValueObject} (implementasi DataInitDefault).
	 *
	 * <p><b>Tujuan:</b> Mengimplementasikan antarmuka {@code DataInitDefault} yang memungkinkan
	 * modul lain (seperti panel revisi atau grid data master) untuk membuka form edit
	 * {@code ProsesTransfer} secara programatik dari luar kelas ini, hanya dengan memanggil
	 * metode {@code init} pada instance controller.</p>
	 *
	 * <p><b>Cara kerja:</b> Melakukan cast {@code GeneralValueObject} ke {@code ProsesTransfer},
	 * menyimpannya ke field instance {@code prosesTransfer}, memanggil
	 * {@link #init(ProsesTransfer)} untuk membangun UI form, lalu menampilkan
	 * {@code addWindow} sebagai modal. Metode ini digunakan oleh framework
	 * {@code Common.copyEditDeleteButtons} yang memanggil {@code init(obj)} pada controller
	 * saat tombol Edit diklik.</p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error — exception dari
	 * {@link #init(ProsesTransfer)} diteruskan ke pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pastikan {@code addWindow} sudah diinisialisasi sebelum
	 * metode ini dipanggil. Untuk penggunaan eksternal (dari modul lain), gunakan
	 * {@link #onAddExternal(EventListener, ProsesTransfer)} yang membuat instance baru
	 * lengkap dengan window sendiri.</p>
	 *
	 * @param obj objek {@code ProsesTransfer} yang akan diedit; harus bisa di-cast ke
	 *            {@code ProsesTransfer}
	 * @throws Exception jika cast gagal atau inisialisasi form gagal
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		prosesTransfer = (ProsesTransfer) obj;
		init(prosesTransfer);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membuka form detail proses transfer dari modul eksternal (tanpa hak edit).
	 *
	 * <p><b>Tujuan:</b> Factory method statis yang membuat instance {@code ProsesTransferAction}
	 * baru secara programatik, lengkap dengan window modal sendiri, untuk menampilkan
	 * detail {@code ProsesTransfer} dalam mode view-only dari modul lain seperti
	 * {@code PertangungjawabanKasBesarAction} atau {@code KasKecilAction}.</p>
	 *
	 * <p><b>Cara kerja:</b> Mendelegasikan ke {@link #onAddExternal(EventListener, ProsesTransfer, boolean)}
	 * dengan parameter {@code edit=false}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Gunakan overload ini ketika hanya ingin menampilkan
	 * informasi transfer tanpa kemampuan edit. Untuk mode edit, gunakan overload dengan
	 * parameter {@code edit=true}.</p>
	 *
	 * @param eventListener listener yang dipanggil setelah window ditutup untuk
	 *                      memperbarui grid pemanggil; boleh null jika tidak diperlukan
	 * @param prosesTransfer entitas ProsesTransfer yang akan ditampilkan
	 * @throws Exception jika inisialisasi window atau form gagal
	 */
	public static void onAddExternal(EventListener eventListener, ProsesTransfer prosesTransfer) throws Exception {
		onAddExternal(eventListener, prosesTransfer, false);
	}

	/**
	 * Membuka form detail proses transfer dari modul eksternal dengan kontrol mode edit.
	 *
	 * <p><b>Tujuan:</b> Factory method statis yang membuat instance {@code ProsesTransferAction}
	 * baru, mengatur semua properti yang diperlukan (window, flags, eventListener), membangun
	 * UI form, dan menampilkan window modal. Digunakan ketika modul lain perlu menampilkan
	 * atau mengedit detail ProsesTransfer tanpa navigasi ke halaman terpisah.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat instance baru {@code ProsesTransferAction} dengan konstruktor no-arg.</li>
	 *   <li>Menyetel field instance: {@code eventListener}, {@code edit}, {@code viewOnly=true},
	 *       {@code persetujuan=true}.</li>
	 *   <li>Membuat {@code MyWindow} baru dan menambahkannya ke root halaman saat ini.</li>
	 *   <li>Mengatur dimensi window (95% tinggi, 90% lebar).</li>
	 *   <li>Memanggil {@link #init(ProsesTransfer)} untuk membangun UI form.</li>
	 *   <li>Menampilkan window sebagai modal yang dapat ditutup.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari inisialisasi diteruskan ke pemanggil.
	 * Pastikan pemanggil menangani exception atau membiarkannya diteruskan ke ZK framework.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini membuat instance dan window baru setiap dipanggil —
	 * tidak ada caching. Jika terlalu banyak window dibuka bersamaan, pertimbangkan
	 * mekanisme singleton per entitas.</p>
	 *
	 * @param eventListener listener untuk memperbarui grid pemanggil setelah window ditutup;
	 *                      boleh null
	 * @param prosesTransfer entitas ProsesTransfer yang akan ditampilkan/diedit
	 * @param edit           {@code true} jika pengguna boleh mengedit; {@code false} untuk view-only
	 * @throws Exception jika inisialisasi window, form, atau komponen ZK gagal
	 */
	public static void onAddExternal(EventListener eventListener, ProsesTransfer prosesTransfer, boolean edit)
			throws Exception {
		ProsesTransferAction prosesTransferAction = new ProsesTransferAction();
		prosesTransferAction.eventListener = eventListener;
		prosesTransferAction.edit = edit;
		prosesTransferAction.viewOnly = true;
		prosesTransferAction.persetujuan = true;
		prosesTransferAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(prosesTransferAction.addWindow);
		prosesTransferAction.addWindow.setHeight("95%");
		prosesTransferAction.addWindow.setWidth("90%");

		prosesTransferAction.init(prosesTransfer);

		prosesTransferAction.addWindow.setVisible(true);
		prosesTransferAction.addWindow.setClosable(true);
		prosesTransferAction.addWindow.onModal();

	}

	private void init(final ProsesTransfer prosesTransfer) throws Exception {
		this.prosesTransfer = prosesTransfer;
		addWindow.setTitle(prosesTransfer.getId() == null ? "Tambah Proses Transfer" : "Ubah Proses Transfer");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(prosesTransfer, null, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		if (prosesTransfer.getId() == null) {

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

			if (prosesTransfer.getDisetujuiOleh() != null) {

				if (prosesTransfer.getRealisasikanOleh() == null) {
					save = new MyToolbarbuttonConfig("Realisasikan", "/img/svg/check-square.svg");
					save.setTooltiptext("Simpan");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Session session = HibernateUtil.currentSession();
							session.refresh(prosesTransfer);
							prosesTransfer.setRealisasikanOleh(Common.getCurrentUser());
							if (tanggalRealisasikan != null) {
								prosesTransfer.setTanggalRealisasikan(tanggalRealisasikan.getValue());
							}
							Common.refreshSaveOrUpdate(session, prosesTransfer);
							session.flush();

							// (D.1 realisasi) Setelah transfer/DPC direalisasikan, beri tahu SEMUA pihak
							// yang terlibat: peserta disposisi + penyetuju + pembuat (+ pelaksana). Penerima
							// dikumpulkan selagi sesi aktif, lalu notifikasi diterbitkan di latar.
							try {
								final org.json.JSONArray uids = new org.json.JSONArray();
								Set<String> emailSet = new java.util.LinkedHashSet<String>();
								kumpulkanPenerimaRealisasi(session, prosesTransfer, uids, emailSet);
								StringBuilder eb = new StringBuilder();
								for (String em : emailSet) {
									if (eb.length() > 0) {
										eb.append(",");
									}
									eb.append(em);
								}
								final String emails = eb.toString();
								final ProsesTransfer ptFinal = prosesTransfer;
								final String realisator = ptFinal.getRealisasikanOleh() == null ? ""
										: ptFinal.getRealisasikanOleh().getUserNama();
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event ev) throws Exception {
										try {
											ais.common.CommonNotifikasi.transferTerealisasi(uids, emails,
													ptFinal.getKode(), ptFinal.getNama(), realisator, ptFinal,
													HAL_ZK_PROSES_TRANSFER, null);
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/akunting/ProsesTransferAction.java:777");
										}
									}
								});
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

							onSearchDefault(null);
							addWindow.setVisible(false);
						}
					});
					save.setParent(toolbar);
				} else {
					Tbmuser tbmuser = Common.getCurrentUser();
					if (prosesTransfer.getRealisasikanOleh() != null && tbmuser != null && tbmuser.getUserId() != null
							&& prosesTransfer.getRealisasikanOleh().getUserId() != null
							&& prosesTransfer.getRealisasikanOleh().getUserId().equals(tbmuser.getUserId())) {
						save = new MyToolbarbuttonConfig("Batalkan Realisasikan", "/img/svg/cancel_presentation.svg");

						save.setTooltiptext("Simpan");
						save.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {

								Session session = HibernateUtil.currentSession();
								session.refresh(prosesTransfer);
								prosesTransfer.setRealisasikanOleh(null);
								prosesTransfer.setTanggalRealisasikan(null);
								Common.refreshSaveOrUpdate(session, prosesTransfer);
								session.flush();

								onSearchDefault(null);
								addWindow.setVisible(false);
							}
						});
						save.setParent(toolbar);
					}
				}

			}

		}
		borderlayout.setParent(addWindow);

		east = new East();
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setParent(borderlayout);
		initDetail(east, prosesTransfer, null);

	}

	/** Halaman ZKoss tujuan klik notifikasi realisasi transfer/DPC. */
	private static final String HAL_ZK_PROSES_TRANSFER = "/pages/master/akunting/proses_transfer.zul";

	/**
	 * Kumpulkan penerima notifikasi realisasi: seluruh peserta alur disposisi (yang
	 * mengajukan tiap tahap + aktor yang dituju), penyetuju, pembuat, dan pelaksana
	 * realisasi. Mengisi {@code uids} (user id, ter-dedup) dan {@code emails} (alamat
	 * email valid). Dipanggil selagi sesi Hibernate masih aktif.
	 *
	 * @param session sesi Hibernate aktif
	 * @param pt      proses transfer yang baru direalisasikan
	 * @param uids    keluaran: daftar user id penerima (JSON array)
	 * @param emails  keluaran: himpunan email penerima
	 */
	@SuppressWarnings("unchecked")
	private void kumpulkanPenerimaRealisasi(Session session, ProsesTransfer pt, org.json.JSONArray uids,
			Set<String> emails) {
		java.util.Set<String> set = new java.util.LinkedHashSet<String>();
		try {
			// Pembuat
			if (pt.getOlehId() != null && !pt.getOlehId().trim().isEmpty()) {
				set.add(pt.getOlehId().trim());
			}
			// Penyetuju
			if (pt.getDisetujuiOleh() != null && pt.getDisetujuiOleh().getUserId() != null) {
				set.add(pt.getDisetujuiOleh().getUserId());
				if (pt.getDisetujuiOleh().getEmail() != null) {
					emails.add(pt.getDisetujuiOleh().getEmail());
				}
			}
			// Pelaksana realisasi
			if (pt.getRealisasikanOleh() != null && pt.getRealisasikanOleh().getUserId() != null) {
				set.add(pt.getRealisasikanOleh().getUserId());
				if (pt.getRealisasikanOleh().getEmail() != null) {
					emails.add(pt.getRealisasikanOleh().getEmail());
				}
			}
			// Peserta alur disposisi
			if (pt.getDisposisiSop() != null) {
				List<DisposisiAlurSop> alurs = session.createCriteria(DisposisiAlurSop.class)
						.add(Restrictions.eq("disposisiSop", pt.getDisposisiSop())).list();
				if (alurs != null) {
					for (DisposisiAlurSop d : alurs) {
						if (d == null) {
							continue;
						}
						if (d.getDiajukanOleh() != null && d.getDiajukanOleh().getUserId() != null) {
							set.add(d.getDiajukanOleh().getUserId());
							if (d.getDiajukanOleh().getEmail() != null) {
								emails.add(d.getDiajukanOleh().getEmail());
							}
						}
						try {
							if (d.getAlurSop() != null && d.getAlurSop().getAktorSop() != null
									&& d.getAlurSop().getAktorSop().getUsernamePengguna() != null) {
								for (String u : d.getAlurSop().getAktorSop().getUsernamePengguna().split(",")) {
									if (u != null && !u.trim().isEmpty()) {
										set.add(u.trim());
									}
								}
							}
						} catch (Exception eAktor) { ais.common.ErrorAuditUtil.record(eAktor, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:890");
						}
					}
				}
			}
			// Lengkapi email untuk user id yang belum punya email (mis. aktor disposisi).
			if (!set.isEmpty()) {
				List<Tbmuser> us = session.createCriteria(Tbmuser.class)
						.add(Restrictions.in("userId", new java.util.ArrayList<String>(set))).list();
				if (us != null) {
					for (Tbmuser u : us) {
						if (u != null && u.getEmail() != null && ais.common.Common.isValidEmailAddress(u.getEmail())) {
							emails.add(u.getEmail());
						}
					}
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		for (String u : set) {
			uids.put(u);
		}
	}

	private Map<Long, Double> longs;
	private Footer footerNilai;
	private double totalNilaiTampil;

	@SuppressWarnings("unchecked")
	private void initDetail(Component east, final ProsesTransfer prosesTransfer, final DisposisiSop disposisiSop)
			throws Exception {
		Common.clear(east);
		if (east instanceof East) {
			((East) east).setWidth("72%");
		}
		if (east instanceof East) {
			((East) east).setStyle("background:#f8fafc; border-left:1px solid #e5e7eb; overflow:auto;");
		}
		longs = new HashMap<Long, Double>();

		final MyCheckboxConfig uangMuka = new MyCheckboxConfig("Uang Muka");
		final MyCheckboxConfig lpj = new MyCheckboxConfig("LPJ");
		final MyCheckboxConfig kasBesar = new MyCheckboxConfig("Kas Besar");
		final MyCheckboxConfig kasKecil = new MyCheckboxConfig("Kas Kecil");
		final MyCheckboxConfig pengadaan = new MyCheckboxConfig("Pengadaan");
		final MyCheckboxConfig termin = new MyCheckboxConfig("Termin");
		final MyCheckboxConfig dp = new MyCheckboxConfig("DP");
		final MyCheckboxConfig diskon = new MyCheckboxConfig("Diskon");
		final MyCheckboxConfig pajak = new MyCheckboxConfig("Pajak");
		final AmbilDataSatuanKerjaBanbox satker = new AmbilDataSatuanKerjaBanbox();
		final Textbox cari = new Textbox();
		cari.setCols(10);
		final Rows rows = new Rows();

		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload(rows, disposisiSop, cari.getValue().trim(), (SatuanKerja) satker.getAttribute("satuanKerja"),
						uangMuka.isChecked(), lpj.isChecked(), kasBesar.isChecked(), kasKecil.isChecked(),
						pengadaan.isChecked(), termin.isChecked(), dp.isChecked(), diskon.isChecked(),
						pajak.isChecked(), this);
			}
		};

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setStyle("background:#f8fafc; border:0; min-height:520px;");
		borderlayout.setParent(east);

		if (prosesTransfer.getId() == null) {
			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("auto");
			toolbar.setStyle(
					"border:0; background:#ffffff; padding:10px; display:flex; flex-wrap:wrap; align-items:center; gap:8px; border-bottom:1px solid #e5e7eb;");
			toolbar.setParent(north);

			toolbar.appendChild(new Html(
					"<div style='font-size:12.5px; color:#334155; margin-right:6px; line-height:1.55;'>"
							+ "<b>Daftar transfer</b><br/>Centang kebutuhan yang akan diproses. Total nilai dihitung otomatis agar jumlah transfer mudah diperiksa sebelum disimpan.</div>"));
			toolbar.appendChild(new MyLabelConfig("Cari"));
			toolbar.appendChild(cari);

			toolbar.appendChild(new MyLabelConfig("Unit"));
			satker.setCols(10);
			toolbar.appendChild(satker);

			satker.setEventListener(eventListener);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.setTooltiptext("Cari");
			button.addEventListener("onClick", eventListener);
			button.setParent(toolbar);

			// Tombol "Singkronkan": periksa ulang & sinkronkan semua tabel referensi ke Daftar Transfer
			// (multi-thread + bar progres). Setelah selesai, daftar di panel ini di-refresh (eventListener).
			// Catatan: build server -source 1.6 → variabel yg dipakai inner class WAJIB final eksplisit.
			final EventListener listenerCari = eventListener;
			final Toolbar toolbarSingkron = toolbar;
			MyToolbarbuttonConfig singkron = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/refresh-cw.svg");
			singkron.setTooltiptext("Periksa ulang & sinkronkan semua data referensi ke Daftar Transfer");
			singkron.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event evSingkron) throws Exception {
					ais.action.master.akunting.helper.SinkronDaftarPengajuanTransferHelper.sinkronkan(toolbarSingkron,
							listenerCari);
				}
			});
			singkron.setParent(toolbar);

			uangMuka.setChecked(true);
			lpj.setChecked(true);
			kasBesar.setChecked(true);
			kasKecil.setChecked(true);
			pengadaan.setChecked(true);
			termin.setChecked(true);
			dp.setChecked(true);
			diskon.setChecked(true);
			pajak.setChecked(true);

			uangMuka.addEventListener("onClick", eventListener);
			lpj.addEventListener("onClick", eventListener);
			kasBesar.addEventListener("onClick", eventListener);
			kasKecil.addEventListener("onClick", eventListener);
			pengadaan.addEventListener("onClick", eventListener);
			termin.addEventListener("onClick", eventListener);
			dp.addEventListener("onClick", eventListener);
			diskon.addEventListener("onClick", eventListener);
			pajak.addEventListener("onClick", eventListener);

			uangMuka.setParent(toolbar);
			lpj.setParent(toolbar);
			kasBesar.setParent(toolbar);
			kasKecil.setParent(toolbar);
			pengadaan.setParent(toolbar);
			termin.setParent(toolbar);
			dp.setParent(toolbar);
			diskon.setParent(toolbar);
			pajak.setParent(toolbar);
		}

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("border:0; background:#ffffff; overflow:auto; padding:8px;");

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle(
				"border:1px solid #e5e7eb; border-radius:14px; overflow:hidden; background:#ffffff; min-height:420px;");
		grid.setMold("paging");
		grid.setPageSize(PAGE_SIZE);
		grid.setPagingPosition("top");
		grid.getPagingChild().setMold("os");

		Columns columns = new Columns();
		columns.setParent(grid);

		if (prosesTransfer.getId() != null) {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("26%");
			column.setAttribute("janganDisabled", true);
			Vbox vbox = new Vbox();
			vbox.setParent(column);

			Hbox hbox = new Hbox();
			hbox.setParent(vbox);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama daftar transfer")));

			if (!viewOnly && !persetujuan && prosesTransfer.getId() != null
					&& prosesTransfer.getDisetujuiOleh() == null) {
				MyToolbarbuttonConfig copy = new MyToolbarbuttonConfig("Ambil", "/img/svg/addthis.svg");
				copy.setTooltiptext("Ambil Data");
				copy.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								AmbilDataDaftarPengajuanTransferBanyak ambilDataDaftarPengajuanTransferBanyak = new AmbilDataDaftarPengajuanTransferBanyak(
										daftarPengajuanTransfersData);
								ambilDataDaftarPengajuanTransferBanyak
										.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								ambilDataDaftarPengajuanTransferBanyak.setHeight("95%");
								ambilDataDaftarPengajuanTransferBanyak.setWidth("700px");

								ambilDataDaftarPengajuanTransferBanyak.setEventListener(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										List<DaftarPengajuanTransfer> daftarPengajuanTransfers = (List<DaftarPengajuanTransfer>) arg0
												.getData();
										Session session = HibernateUtil.currentSession();
										for (DaftarPengajuanTransfer daftarPengajuanTransfer : daftarPengajuanTransfers) {
											daftarPengajuanTransfer.setProsesTransfer(prosesTransfer);
											Common.refreshUpdate(session, daftarPengajuanTransfer);
										}
										session.flush();

										Common.createDefaultTimer(eventListener);

									}
								});

								ambilDataDaftarPengajuanTransferBanyak.onModal();

							}
						});

					}
				});
				copy.setParent(hbox);
			}

			hbox = new Hbox();
			hbox.setParent(vbox);
			cari.setCols(10);
			hbox.appendChild(cari);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.setTooltiptext("Cari");
			button.addEventListener("onClick", eventListener);
			button.setParent(hbox);
			button.setAttribute("janganDisabled", true);
			cari.setAttribute("janganDisabled", true);
		} else {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("26%");

			final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Nama daftar transfer");
			column.appendChild(checkboxConfig);

			checkboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					longs.clear();

					if (checkboxConfig.isChecked()) {
						List<Object[]> longss = HibernateUtil.currentSession()
								.createCriteria(DaftarPengajuanTransfer.class)
								.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
										Restrictions.eq("disposisiSop.aktif", true)))
								.setProjection(Projections.projectionList().add(Projections.property("id"))
										.add(Projections.property("nominal")).add(Projections.property("aktif"))
										// +FK termin & pajak: agar cek "termin belum disetujui" hanya me-load
										// baris yang MUNGKIN termin/pajak (bukan seluruh kandidat).
										.add(Projections.property("pembayaranTerminMasterAssetDetail"))
										.add(Projections.property("pajak")))
								.add(Restrictions.isNull("prosesTransfer")).list();

						for (Object[] a : longss) {
							Long idKand = Long.parseLong(a[0].toString());
							// "Pilih semua" JANGAN ikut memilih termin BELUM DISETUJUI (yang disembunyikan
							// dari tampilan). Hanya me-load bila baris punya FK termin/pajak.
							if (a.length > 4 && (a[3] != null || a[4] != null)) {
								try {
									DaftarPengajuanTransfer dCek = (DaftarPengajuanTransfer) HibernateUtil
											.currentSession().get(DaftarPengajuanTransfer.class, idKand);
									if (dCek != null && ais.action.master.akunting.helper.DaftarPengajuanTransferSearchHelper
											.terminBelumDisetujui(HibernateUtil.currentSession(), dCek)) {
										continue;
									}
								} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1169");
								}
							}
							longs.put(idKand, Double.valueOf(a[1] == null ? 0.0 : Double.parseDouble(a[1].toString())));
						}
					}

					reload(rows, disposisiSop, cari.getValue().trim(), (SatuanKerja) satker.getAttribute("satuanKerja"),
							uangMuka.isChecked(), lpj.isChecked(), kasBesar.isChecked(), kasKecil.isChecked(),
							pengadaan.isChecked(), termin.isChecked(), dp.isChecked(), diskon.isChecked(),
							pajak.isChecked(), eventListener);

					eventListenerHitung.onEvent(null);
				}
			});
		}

		MyColumnConfig column = new MyColumnConfig("No. Dokumen");
		column.setWidth("13%");
		column.setParent(columns);

		column = new MyColumnConfig("Bank/Atas Nama/No.Rekening");
		column.setWidth("18%");
		column.setParent(columns);

		column = new MyColumnConfig("Nilai");
		column.setAlign("right");
		column.setWidth("14%");
		column.setParent(columns);

		column = new MyColumnConfig("SOP");
		column.setParent(columns);

		column = new MyColumnConfig("Aksi");
		column.setWidth("12%");
		column.setParent(columns);

		rows.setParent(grid);

		if (prosesTransfer.getId() == null) {
			reload(rows, disposisiSop, cari.getValue().trim(), (SatuanKerja) satker.getAttribute("satuanKerja"),
					uangMuka.isChecked(), lpj.isChecked(), kasBesar.isChecked(), kasKecil.isChecked(),
					pengadaan.isChecked(), termin.isChecked(), dp.isChecked(), diskon.isChecked(), pajak.isChecked(),
					eventListener);
		} else {
			reload(rows, disposisiSop, "", null, true, true, true, true, true, true, true, true, true, eventListener);
		}

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
	private DisposisiSop disposisiSop = null;
	private MyDatebox tanggalPembuatan;
	private MyCheckboxConfig setujuiOleh;
	private MyDatebox tanggalRealisasikan;

	private List<DaftarPengajuanTransfer> daftarPengajuanTransfersData = new ArrayList<DaftarPengajuanTransfer>();


	private static String text(String value) {
		return value == null ? "" : value;
	}

	private static boolean isTrue(Boolean value) {
		return Boolean.TRUE.equals(value);
	}

	private static double nilai(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	private static String formatNilai(double value) {
		return Common.numberFormat.get().format(value);
	}

	private static String formatTanggal(Date value) {
		return value == null ? "" : Common.dateFormat.get().format(value);
	}

	private void updateFooterNilai(double nilaiDipilih) {
		if (footerNilai == null) {
			return;
		}

		if (prosesTransfer != null && prosesTransfer.getId() == null) {
			footerNilai.setLabel("Dipilih: " + formatNilai(nilaiDipilih) + "    |    Tampil: "
					+ formatNilai(totalNilaiTampil));
		} else {
			footerNilai.setLabel("Total: " + formatNilai(nilaiDipilih));
		}
		footerNilai.setStyle(styleFooterTotalNilai());
	}

	private String styleFooterTotalNilai() {
		return "text-align:right; font-weight:700; color:#0f172a; background:#ecfdf5; "
				+ "border-top:1px solid #bbf7d0; padding:10px 14px; min-height:34px; "
				+ "line-height:20px; white-space:nowrap; overflow:visible;";
	}


	/**
	 * Menghitung total PPh (Pajak Penghasilan) dari sebuah tagihan pengadaan.
	 *
	 * <p><b>Tujuan:</b> Menghitung nilai PPh yang harus dipotong dari tagihan vendor
	 * ({@link DaftarPengajuanTransfer} yang berasal dari {@code saldoAwalMasterAsset})
	 * sebelum transfer dilakukan. Nilai PPh ini ditampilkan sebagai baris tersendiri di
	 * bawah tagihan utama dalam panel detail, sehingga pejabat yang mengesahkan dapat
	 * melihat nilai netto (Nominal - PPh) yang benar-benar akan ditransfer ke vendor.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code daftarPengajuanTransfer} null atau tidak memiliki
	 *       {@code saldoAwalMasterAsset}, kembalikan 0.0.</li>
	 *   <li>Query semua {@code SaldoAwalMasterAssetDetail} milik saldo awal tersebut
	 *       dari database menggunakan sesi Hibernate saat ini.</li>
	 *   <li>Untuk setiap detail yang memiliki {@code jenisPajakBarang} dengan
	 *       {@code akunDanaTitipan} (indikator bahwa PPh harus dipotong dan disetor
	 *       ke kas negara), hitung: {@code dpp = jumlah * harga} (tanpa potongan)
	 *       dan {@code pph = persenPph/100 * dpp}.</li>
	 *   <li>Kembalikan total akumulasi PPh dari semua detail.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penting:</b> Rumus ini sengaja dibuat identik dengan yang dipakai
	 * {@code PostingProsesTransferAction} saat memposting jurnal. Nilai netto yang
	 * ditampilkan di form = nilai yang benar-benar didebet ke Hutang Vendor di jurnal.
	 * Jangan menggunakan {@code detail.hitungPph()} (yang mengurangi potongan) agar
	 * tidak berbeda dengan buku besar.</p>
	 *
	 * <p><b>Penanganan error:</b> Seluruh metode dibungkus dalam try-catch yang
	 * mengembalikan 0.0 jika terjadi error apapun (mis. entitas tidak ditemukan,
	 * koneksi database gagal). Ini memastikan UI tidak rusak karena kegagalan
	 * perhitungan pajak.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika rumus PPh berubah di {@code PostingProsesTransferAction},
	 * perbarui juga metode ini agar nilai yang ditampilkan tetap konsisten dengan
	 * yang diposting ke buku besar.</p>
	 *
	 * @param daftarPengajuanTransfer tagihan yang akan dihitung PPh-nya; boleh null
	 * @return total nilai PPh dalam satuan rupiah (double), atau 0.0 jika tidak ada
	 *         PPh atau terjadi error
	 */
	/**
	 * Hitung total PPh sebuah tagihan ({@link DaftarPengajuanTransfer} dari
	 * {@code saldoAwalMasterAsset}). 0 bila bukan tagihan / tidak ada PPh.
	 *
	 * <p><b>PENTING:</b> rumus ini SENGAJA dibuat IDENTIK dengan yang dipakai
	 * {@code PostingProsesTransferAction} saat memposting jurnal, supaya nilai NETTO yang
	 * ditampilkan di form = nilai yang benar-benar didebet ke Hutang Vendor di jurnal
	 * ({@code nominal - totalPajak}), dan baris PPh = yang didebet ke akun titipan pajak.
	 * Maka: HANYA detail yang punya {@code jenisPajakBarang.getAkunDanaTitipan()}, dan DPP =
	 * {@code jumlah * harga} (TANPA potongan), {@code pph = persenPph/100 * dpp}. Jangan
	 * memakai {@code detail.hitungPph()} (yang mengurangi potongan) agar tidak beda dgn buku.</p>
	 */
	/**
	 * Mencocokkan kata kunci pencarian dengan teks yang DITAMPILKAN pada baris: No. Dokumen
	 * (kode DPT + kode Pajak induk), nama/keterangan, serta NAMA VENDOR / atas nama rekening
	 * (dan bank/no.rek). Pencocokan case-insensitive "mengandung". Tiap akses getter (banyak
	 * yang computed/lazy) dibungkus try-catch agar satu nilai bermasalah tidak menggagalkan filter.
	 */
	/**
	 * Ambil PembayaranTerminMasterAssetDetail dari sebuah DPT: baris tagihan vendor termin (relasi
	 * langsung) atau baris PPh/Pajak termin (Opsi B, via pajak.keyData = id termin-detail).
	 * Dipakai untuk menampilkan/mencari kode pembayaran termin & kode PO. Null bila bukan termin.
	 */
	private static ais.database.model.asset.PembayaranTerminMasterAssetDetail terminDetailDpt(DaftarPengajuanTransfer d) {
		if (d == null) {
			return null;
		}
		try {
			if (d.getPembayaranTerminMasterAssetDetail() != null) {
				return d.getPembayaranTerminMasterAssetDetail();
			}
			if (d.getPajak() != null && d.getPajak().getKeyData() != null
					&& d.getPajak().getSaldoAwalMasterAssetDetail() == null && d.getPajak().getSaldoAwal() == null
					&& d.getPajak().getPertangungjawaban() == null
					&& d.getPajak().getPertangungjawabanKasBesar() == null) {
				return (ais.database.model.asset.PembayaranTerminMasterAssetDetail) HibernateUtil.currentSession()
					.get(ais.database.model.asset.PembayaranTerminMasterAssetDetail.class, d.getPajak().getKeyData());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1379");
		}
		return null;
	}

	private static boolean cocokKataKunci(DaftarPengajuanTransfer d, String kataKunci) {
		if (d == null || kataKunci == null || kataKunci.trim().isEmpty()) {
			return true;
		}
		String q = kataKunci.trim().toLowerCase();
		StringBuilder hay = new StringBuilder();
		try { if (d.getNama() != null) hay.append(d.getNama()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1390");}
		try { if (d.getKode() != null) hay.append(d.getKode()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1391");}
		try { if (d.getKeterangan() != null) hay.append(d.getKeterangan()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1392");}
		// No. Dokumen sumber untuk baris Pajak (mis. 0225/INV-VDR/YTB/V/2026).
		try { if (d.getPajak() != null && d.getPajak().getKode() != null) hay.append(d.getPajak().getKode()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1394");}
		// Nama vendor / atas nama rekening + bank + no. rekening.
		try { if (d.getAtasNamaSumber() != null) hay.append(d.getAtasNamaSumber()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1396");}
		try { if (d.getBankSumber() != null && d.getBankSumber().getNama() != null) hay.append(d.getBankSumber().getNama()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1397");}
		try { if (d.getNoRekSumber() != null) hay.append(d.getNoRekSumber()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1398");}
		// No. dokumen sumber pengadaan (SaldoAwalMasterAsset) bila ada.
		try { if (d.getSaldoAwalMasterAsset() != null && d.getSaldoAwalMasterAsset().getKode() != null) hay.append(d.getSaldoAwalMasterAsset().getKode()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1400");}
		try { if (d.getSaldoAwalMasterAsset() != null && d.getSaldoAwalMasterAsset().getPenyedia() != null && d.getSaldoAwalMasterAsset().getPenyedia().getNama() != null) hay.append(d.getSaldoAwalMasterAsset().getPenyedia().getNama()).append(' '); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1401");}
		// Kode pembayaran termin + kode PO (baris termin: vendor langsung / pajak via keyData).
		try {
			ais.database.model.asset.PembayaranTerminMasterAssetDetail det = terminDetailDpt(d);
			if (det != null) {
				if (det.getPembayaranTerminMasterAsset() != null && det.getPembayaranTerminMasterAsset().getKode() != null) {
					hay.append(det.getPembayaranTerminMasterAsset().getKode()).append(' ');
				}
				if (det.getPemesananPengadaanMasterAsset() != null && det.getPemesananPengadaanMasterAsset().getKode() != null) {
					hay.append(det.getPemesananPengadaanMasterAsset().getKode()).append(' ');
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1413");
		}
		return hay.toString().toLowerCase().contains(q);
	}

	@SuppressWarnings("unchecked")
	private void reload(final Rows rows, DisposisiSop disposisiSop, String cari, SatuanKerja satker, boolean uangMuka,
			boolean lpj, boolean kasBesar, boolean kasKecil, boolean pengadaan, boolean termin, boolean dp,
			boolean diskon, boolean pajak, final EventListener eventListener

	) {
		Common.clear(rows);
		totalNilaiTampil = 0.0;
		if (longs == null) {
			longs = new HashMap<Long, Double>();
		}
		String kataKunci = cari == null ? "" : cari.trim();

		Criterion criterion = uangMuka ? Restrictions.isNotNull("uangMuka") : Restrictions.isNull("uangMuka");

		criterion = lpj ? Restrictions.or(criterion, Restrictions.isNotNull("pertangungjawaban"))
				: Restrictions.and(criterion, Restrictions.isNull("pertangungjawaban"));

		criterion = kasBesar ? Restrictions.or(criterion, Restrictions.isNotNull("kasBesar"))
				: Restrictions.and(criterion, Restrictions.isNull("kasBesar"));

		criterion = kasKecil
				? Restrictions.or(criterion,
						Restrictions.or(Restrictions.isNotNull("jenisKasKecil"),
								Restrictions.isNotNull("penggantianKasKecil")))

				: Restrictions.and(criterion, Restrictions.and(Restrictions.isNull("jenisKasKecil"),
						Restrictions.isNull("penggantianKasKecil")));

		criterion = pengadaan
				? Restrictions.or(criterion,
						Restrictions.or(Restrictions.isNotNull("saldoAwalMasterAsset"),
								Restrictions.isNotNull("pembayaranPengadaanMasterAssetDetail")))
				: Restrictions.and(criterion, Restrictions.and(Restrictions.isNull("saldoAwalMasterAsset"),
						Restrictions.isNull("pembayaranPengadaanMasterAssetDetail")));

		criterion = termin ? Restrictions.or(criterion, Restrictions.isNotNull("pembayaranTerminMasterAssetDetail"))
				: Restrictions.and(criterion, Restrictions.isNull("pembayaranTerminMasterAssetDetail"));

		criterion = dp ? Restrictions.or(criterion, Restrictions.isNotNull("pembayaranDpMasterAssetDetail"))
				: Restrictions.and(criterion, Restrictions.isNull("pembayaranDpMasterAssetDetail"));

		criterion = diskon ? Restrictions.or(criterion, Restrictions.isNotNull("diskonTagihan"))
				: Restrictions.and(criterion, Restrictions.isNull("diskonTagihan"));

		criterion = pajak ? Restrictions.or(criterion, Restrictions.isNotNull("pajak"))
				: Restrictions.and(criterion, Restrictions.isNull("pajak"));

		Criteria criteria = HibernateUtil.currentSession().createCriteria(DaftarPengajuanTransfer.class)
				.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
						.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));

		// Pasang alias LEFT JOIN (vendor penyedia + pajak breakdown) agar pencarian bisa
		// menembus sampai kode INDUK termin/PO & nama vendor — SAMA seperti tab "Daftar
		// Transfer". Tanpa ini, baris pengajuan pembayaran TERMIN tak ketemu di sini karena
		// kode induknya ada di entitas lain (bukan kolom nama/kode pada DPT).
		ais.action.master.akunting.helper.DaftarPengajuanTransferSearchHelper.pasangAlias(criteria);

		daftarPengajuanTransfersData =

				criteria.add(prosesTransfer.getId() != null ? Restrictions.sqlRestriction("true") : criterion)

						// Pencarian teks di SQL (LEFT JOIN): kode/judul DPT, nama vendor, kode pajak,
						// serta kode INDUK termin (Termin/PO/BAST) untuk baris termin — persis logika
						// pencarian tab "Daftar Transfer".
						.add(kataKunci.length() == 0 ? Restrictions.sqlRestriction("true")
								: ais.action.master.akunting.helper.DaftarPengajuanTransferSearchHelper
										.filterKodeJudul(HibernateUtil.currentSession(), kataKunci))

						.add(prosesTransfer.getId() != null ? Restrictions.eq("prosesTransfer", prosesTransfer)
								: Restrictions.isNull("prosesTransfer"))

						.addOrder(Order.desc("id")).list();

		for (final DaftarPengajuanTransfer daftarPengajuanTransfer : daftarPengajuanTransfersData) {
			// Kandidat "Tambah Proses Transfer": TERMIN yang BELUM DISETUJUI JANGAN ditarik ke DPC —
			// hanya termin yang sudah disetujui. Hanya berlaku pada daftar KANDIDAT (proses transfer
			// BARU, id==null); saat mengedit proses transfer yang sudah ada, item yang sudah menempel
			// tetap ditampilkan. Menangkap juga data LAMA yang DPT-nya terlanjur dibuat sebelum disetujui.
			if (prosesTransfer.getId() == null
					&& ais.action.master.akunting.helper.DaftarPengajuanTransferSearchHelper
							.terminBelumDisetujui(HibernateUtil.currentSession(), daftarPengajuanTransfer)) {
				continue;
			}
			SatuanKerja satuanKerja = daftarPengajuanTransfer.ambilSatuanKerja();
			if (prosesTransfer.getId() != null || satker == null || satker.getId() == null
					|| (satuanKerja != null && satker.getId().equals(satuanKerja.getId()))) {

				final Long iddata = daftarPengajuanTransfer.getId();
				// Nominal sudah NETTO di model (vendor pengadaan = bruto - PPh). PPh sendiri
				// kini menjadi baris DaftarPengajuanTransfer PAJAK terpisah (filter "Pajak"),
				// jadi tidak ada lagi sub-baris PPh inline di sini.
				final Double n = Double.valueOf(nilai(daftarPengajuanTransfer.getNominal()));
				totalNilaiTampil += nilai(n);

				if (prosesTransfer.getId() != null && iddata != null) {
					longs.put(iddata, n);
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				if (prosesTransfer.getId() != null) {
					final MyCheckboxConfig c;

					Vbox vbox = new Vbox();

					try {
						RevisiHelper.createNewRevisi(DaftarPengajuanTransfer.class, daftarPengajuanTransfer,
								daftarPengajuanTransfer.getNama()).setParent(vbox);
					} catch (Exception e) {

						ais.common.Common.tampilErrorJikaAdmin(e);
					}

					row.appendChild(vbox);

					final MyCheckboxConfig transitori = new MyCheckboxConfig("Transitori", "/img/svg/check2.svg");
					transitori.setChecked(isTrue(daftarPengajuanTransfer.getTransitori()));

					final Hbox hbox = new Hbox();

					EventListener transitoriEventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							daftarPengajuanTransfer.setTransitori(transitori.isChecked());
							Common.refreshUpdate(daftarPengajuanTransfer);

							Session session = HibernateUtil.currentSession();
							Transitori tr = (Transitori) session.createCriteria(Transitori.class)
									.add(Restrictions.eq("daftarPengajuanTransfer", daftarPengajuanTransfer))
									.setMaxResults(1).uniqueResult();
							if (transitori.isChecked()) {
								if (tr == null) {
									tr = new Transitori();
									tr.setDaftarPengajuanTransfer(daftarPengajuanTransfer);
									tr.setNama(daftarPengajuanTransfer.getNama());
									tr.setKode(daftarPengajuanTransfer.getKode());
									session.save(tr);
									session.flush();

									if (daftarPengajuanTransfer != null
											&& daftarPengajuanTransfer.getTransitoriData() == null) {
										daftarPengajuanTransfer.setTransitoriData(tr);
										Common.refreshUpdate(session, daftarPengajuanTransfer);
										session.flush();
									}
								}
							} else {
								if (tr != null) {

									if (daftarPengajuanTransfer != null
											&& daftarPengajuanTransfer.getTransitoriData() != null) {
										daftarPengajuanTransfer.setTransitoriData(null);
										Common.refreshUpdate(session, daftarPengajuanTransfer);
										session.flush();
									}

									session.delete(tr);
									session.flush();
								}
							}

							hbox.setVisible(!isTrue(daftarPengajuanTransfer.getTransitori()));
						}
					};

					hbox.setVisible(!isTrue(daftarPengajuanTransfer.getTransitori()));
					transitori.addEventListener("onClick", transitoriEventListener);

					vbox.appendChild(hbox);
					c = new MyCheckboxConfig("Transfer", "/img/svg/check2.svg");

					if ((prosesTransfer.getDisetujuiOleh() != null && prosesTransfer.getRealisasikanOleh() == null)) {
						vbox.appendChild(transitori);
						hbox.appendChild(c);
					}

					else if ((prosesTransfer.getDisetujuiOleh() != null)) {
						hbox.appendChild(new MyLabelKecil("Transfer"));

						if (isTrue(daftarPengajuanTransfer.getTransitori())) {
							vbox.appendChild(new MyLabelKecil("Transitori"));
						}
					}

					else if ((persetujuan)) {
						if (isTrue(daftarPengajuanTransfer.getTransitori())) {
							vbox.appendChild(new MyLabelKecil("Transitori"));
						}
					} else if (disposisiSop == null) {

						vbox.appendChild(transitori);

						hbox.appendChild(c);

					}

					c.setChecked(isTrue(daftarPengajuanTransfer.getTransfer()));

					EventListener eventListenerEventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							daftarPengajuanTransfer.setTransfer(c.isChecked());
							Common.refreshUpdate(daftarPengajuanTransfer);

							transitori.setVisible(!isTrue(daftarPengajuanTransfer.getTransfer()));
						}
					};

					c.addEventListener("onClick", eventListenerEventListener);

					transitori.setVisible(!isTrue(daftarPengajuanTransfer.getTransfer()));

				} else {
					final Checkbox c;
					row.appendChild(c = new Checkbox(daftarPengajuanTransfer.getNama()));

					c.setChecked(longs.keySet().contains(daftarPengajuanTransfer.getId()));

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

				Vbox vbox = new Vbox();
				row.appendChild(vbox);

				vbox.appendChild(new MyLabelKecil(text(daftarPengajuanTransfer.getKode())));
				// Untuk PPh/Pajak: tampilkan kode dokumen induk. Untuk pajak TERMIN, samakan dengan KODE
				// PEMBAYARAN TAGIHAN TERMIN (mis. 124/TRM/YTB/VI/2026) — BUKAN kode BAST; pajak non-termin
				// tetap pakai Pajak.getKode().
				if (daftarPengajuanTransfer.getPajak() != null) {
					String kodePengajuan = null;
					try {
						ais.database.model.asset.PembayaranTerminMasterAssetDetail detNoDok = terminDetailDpt(daftarPengajuanTransfer);
						if (detNoDok != null && detNoDok.getPembayaranTerminMasterAsset() != null) {
							kodePengajuan = detNoDok.getPembayaranTerminMasterAsset().getKode();
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferAction.java:1670");
					}
					if (kodePengajuan == null || kodePengajuan.trim().isEmpty()) {
						kodePengajuan = daftarPengajuanTransfer.getPajak().getKode();
					}
					if (kodePengajuan != null && !kodePengajuan.isEmpty()) {
						MyLabelKecil lblKodePajak = new MyLabelKecil(kodePengajuan);
						lblKodePajak.setStyle("font-size:10px;color:#b45309;font-weight:bold;");
						vbox.appendChild(lblKodePajak);
					}
				}

				vbox = new Vbox();
				row.appendChild(vbox);

				// Kolom "Atas Nama/No.Rek": tampilkan nama bank + atas nama pemilik rekening + nomor
				// rekening tujuan transfer (sumber data sama dengan kolom Akun, mis. untuk PPh = akun pajak).
				String namaBank = daftarPengajuanTransfer.getBankSumber() == null ? ""
						: text(daftarPengajuanTransfer.getBankSumber().getNama());
				String atasNama = text(daftarPengajuanTransfer.getAtasNamaSumber());
				String noRek = daftarPengajuanTransfer.getNoRekSumber() == null ? ""
						: daftarPengajuanTransfer.getNoRekSumber();

				boolean adaInfoBank = namaBank.trim().length() > 0 || atasNama.trim().length() > 0
						|| noRek.trim().length() > 0;

				if (!adaInfoBank) {
					// Tidak ada rekening pada akun terkait → beri petunjuk agar diisi di master Akun.
					MyLabelKecil hint = new MyLabelKecil("Rekening belum diatur pada akun");
					hint.setStyle("font-size:10px; color:#9ca3af; font-style:italic;");
					vbox.appendChild(hint);
				} else {
					if (namaBank.trim().length() > 0) {
						MyLabelKecil labelBank = new MyLabelKecil(namaBank);
						labelBank.setStyle("font-size:10px; font-weight:bold;");
						vbox.appendChild(labelBank);
					}
					if (atasNama.trim().length() > 0) {
						vbox.appendChild(new MyLabelKecil("a.n. " + atasNama));
					}
					if (noRek.trim().length() > 0) {
						vbox.appendChild(new MyLabelKecil("No. Rek: " + noRek));
					}
				}

				row.appendChild(
						new MyLabelKecil(formatNilai(nilai(daftarPengajuanTransfer.getNominal()))));

				Vbox vbox2 = new Vbox();
				vbox2.setParent(row);
				if (daftarPengajuanTransfer.getDisposisiSop() != null) {
					A aa;
					(aa = new A()).setParent(vbox2);
					aa.setStyle("font-size:9px;");
					UIClassHelper.applyReadMore(aa, "SOP " + text(daftarPengajuanTransfer.getDisposisiSop().getKeterangan())
							+ (daftarPengajuanTransfer.getDisposisiSop().getSop() == null ? ""
									: " (" + text(daftarPengajuanTransfer.getDisposisiSop().getSop().getNama()) + ")"));
					aa.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanAlurSopAction.prosess(daftarPengajuanTransfer.getDisposisiSop().getId(), null, null,
									true, arg0.getTarget());
						}
					});

				} else {
					new MyLabelKecil().setParent(vbox2);
				}

				Hbox actionBox = new Hbox();
				actionBox.setStyle("gap:4px; align-items:center; justify-content:center;");
				actionBox.setParent(row);

				boolean bolehBatal = !viewOnly && !persetujuan && prosesTransfer.getId() != null
						&& prosesTransfer.getDisetujuiOleh() == null;
				if (bolehBatal) {
					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batal", "/img/svg/trash.svg");
					button.setTooltiptext("Batalkan item ini dari daftar proses transfer");
					button.setStyle(
							"font-size:11px; font-weight:bold; color:#b91c1c; background:#fee2e2; border-radius:8px; padding:4px 8px;");
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							MyMessageboxConfig.show("Apakah yakin ingin membatalkan data ini ?", "Question",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {
													daftarPengajuanTransfer.setProsesTransfer(null);
													daftarPengajuanTransfer.setTransfer(null);
													daftarPengajuanTransfer.setTransitori(null);

													Common.refreshUpdate(daftarPengajuanTransfer);

													longs.remove(iddata);

													Common.createDefaultTimer(eventListener);

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
					button.setParent(actionBox);
				} else {
					new MyLabelKecil(prosesTransfer.getId() == null ? "Pilih/lepaskan checklist" : "-")
							.setParent(actionBox);
				}

			}
		}

		try {
			eventListenerHitung.onEvent(null);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Menyimpan pengajuan proses transfer baru beserta daftar transfer yang dipilih.
	 *
	 * <p><b>Tujuan:</b> Memvalidasi dan menyimpan data {@code ProsesTransfer} ke database,
	 * mengaitkan semua {@code DaftarPengajuanTransfer} yang dipilih pengguna (dari map
	 * {@code longs}), menghitung total nilai, dan memicu cetak dokumen pengajuan secara
	 * otomatis setelah penyimpanan berhasil.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li><b>Validasi</b>: periksa judul ({@code nama}) tidak kosong dan setidaknya
	 *       satu DaftarPengajuanTransfer dipilih ({@code longs} tidak kosong).</li>
	 *   <li><b>Load entitas</b>: jika {@code prosesTransfer.getId()} tidak null (mode edit),
	 *       load ulang dari session untuk memastikan state terkini.</li>
	 *   <li><b>Set field</b>: nama, keterangan, cara transfer, tanggal pembuatan,
	 *       tanggal realisasi, disposisiSop, dan generate kode jika baru.</li>
	 *   <li><b>Status persetujuan</b>: jika checkbox {@code setujuiOleh} dicentang,
	 *       set {@code disetujuiOleh} dan {@code tanggalPersetujuan} ke user/waktu saat ini.</li>
	 *   <li><b>Simpan ProsesTransfer</b> via {@code Common.refreshSaveOrUpdate} dan flush.</li>
	 *   <li><b>Kaitkan DaftarPengajuanTransfer</b>: query berdasarkan id dalam {@code longs},
	 *       set {@code prosesTransfer} pada masing-masing, dan akumulasi nilai total.</li>
	 *   <li><b>Update nilai total</b> pada ProsesTransfer dan simpan ulang.</li>
	 *   <li><b>Notifikasi eventListener</b> (jika ada) dan trigger cetak via timer 2.5 detik.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Validasi dilakukan di awal dan mengembalikan {@code false}
	 * jika gagal (menampilkan pesan error ke pengguna via {@code MyMessageboxConfig.show}).
	 * Exception dari Hibernate diteruskan ke pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode ini hanya digunakan untuk ProsesTransfer BARU (dari
	 * form add). Untuk realisasi (mode edit), tombol "Realisasikan" di {@link #init(ProsesTransfer)}
	 * menangani secara terpisah tanpa memanggil onSave. Pastikan field {@code longs}
	 * terisi dengan benar sebelum memanggil metode ini.</p>
	 *
	 * @param event event ZK dari klik tombol Simpan; tidak digunakan langsung
	 * @return {@code true} jika penyimpanan berhasil; {@code false} jika validasi gagal
	 * @throws Exception jika operasi Hibernate gagal
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Proses Transfer belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Proses dengan nama proses transfer yang sesuai; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (longs.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Daftar Transfer belum dipilih. Langkah yang dapat dilakukan: (1) Centang minimal satu transaksi transfer dari daftar yang tersedia; (2) Pastikan transaksi transfer sudah ada dan berstatus siap diproses; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (prosesTransfer.getId() != null) {
			prosesTransfer = (ProsesTransfer) session.load(ProsesTransfer.class, prosesTransfer.getId());

		}

		prosesTransfer.setNama(nama.getValue());
		prosesTransfer.setKeterangan(keterangan.getValue());
		prosesTransfer.setCaraPembayaranTransfer((CaraPembayaranTransfer) (caraBayarByr.getSelectedItem() == null ? null
				: caraBayarByr.getSelectedItem().getValue()));
		prosesTransfer.setTanggalPembuatan(tanggalPembuatan.getValue());

		prosesTransfer.setTanggalRealisasikan(tanggalRealisasikan.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			prosesTransfer.setDisposisiSop(disposisiSop);
		}

		if (prosesTransfer.getId() == null) {
			String noAgenda = generateCode(true);
			prosesTransfer.setKode(noAgenda);
		}

		if (setujuiOleh.isChecked()) {
			prosesTransfer.setDisetujuiOleh(Common.getCurrentUser());
			prosesTransfer.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		} else {
			prosesTransfer.setDisetujuiOleh(null);
			prosesTransfer.setTanggalPersetujuan(null);
		}

		Common.refreshSaveOrUpdate(session, prosesTransfer);
		session.flush();

		List<DaftarPengajuanTransfer> daftarPengajuanTransfers = HibernateUtil.currentSession()
				.createCriteria(DaftarPengajuanTransfer.class)
				.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
						Restrictions.eq("disposisiSop.aktif", true)))
				.add(longs.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longs.keySet()))
				.list();

		Double nilaiTotal = 0.0;
		for (DaftarPengajuanTransfer daftarPengajuanTransfer : daftarPengajuanTransfers) {
			daftarPengajuanTransfer.setProsesTransfer(prosesTransfer);

			Common.refreshUpdate(session, daftarPengajuanTransfer);

			nilaiTotal += nilai(daftarPengajuanTransfer.getNominal());

		}

		prosesTransfer.setNilai(nilaiTotal);

		Common.refreshUpdate(session, prosesTransfer);

		session.flush();

		if (eventListener != null) {
			Common.createDefaultTimer(eventListener);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				cetak(prosesTransfer);
			}
		}, "Proses cetak", false, 2500);

		return true;
	}

	/**
	 * Membangun Hibernate {@code Criteria} untuk query daftar {@code ProsesTransfer}.
	 *
	 * <p><b>Tujuan:</b> Mengkonstruksi query database secara dinamis berdasarkan filter
	 * yang diisi pengguna di toolbar pencarian: rentang tanggal, status aktif, dan kata
	 * kunci (nama atau kode). Diimplementasikan dari antarmuka {@code DataCriteria} dan
	 * digunakan oleh {@link #onSearchDefault(Event)} dan komponen ekspor.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat {@code Criteria} pada kelas {@code ProsesTransfer}
	 * dengan klausa:
	 * <ul>
	 *   <li>Filter rentang tanggal: {@code date(tanggal_pembuatan) BETWEEN start AND end}
	 *       (menggunakan SQL native untuk perbandingan tanggal saja tanpa waktu).</li>
	 *   <li>Filter aktif: jika checkbox {@code searchaktif} dicentang atau null, filter
	 *       hanya record aktif (aktif IS NULL OR aktif=true). Jika tidak dicentang,
	 *       tampilkan semua termasuk yang tidak aktif.</li>
	 *   <li>Kata kunci: pencarian partial case-insensitive pada field {@code nama}
	 *       atau {@code kode}.</li>
	 *   <li>Jika {@code order=true}, tambahkan ORDER BY id DESC.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error eksplisit — jika {@code start}
	 * atau {@code end} null (field ZUL tidak ter-wire), digunakan
	 * {@code sqlRestriction("1=1")} sebagai fallback agar query tetap valid.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Saat menambah filter baru, tambahkan klausa di sini dan
	 * field UI yang sesuai di ZUL. Perhatikan bahwa {@code searchnama} digunakan untuk
	 * mencari di dua field sekaligus (nama DAN kode) menggunakan {@code Restrictions.or}.</p>
	 *
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC (untuk tampilan grid);
	 *              {@code false} untuk query COUNT tanpa ordering (untuk paging)
	 * @return {@code Criteria} Hibernate yang siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ProsesTransfer.class)

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * Menjalankan pencarian ulang dan memperbarui tampilan grid proses transfer.
	 *
	 * <p><b>Tujuan:</b> Metode pencarian default yang diimplementasikan dari antarmuka
	 * {@code DataSearchDefault}. Dipanggil oleh: event paging, perubahan filter, setelah
	 * operasi persetujuan/pembatalan, setelah realisasi, dan saat inisialisasi awal.
	 * Memperbarui konten grid dengan data terbaru sesuai filter aktif.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Guard check: jika {@code searchnama} null (komponen belum ter-wire), return
	 *       dini untuk menghindari NullPointerException.</li>
	 *   <li>Reset flag {@code persetujuan=false} agar tampilan grid kembali ke mode
	 *       normal setelah operasi persetujuan selesai.</li>
	 *   <li>Set ukuran halaman pada paging component.</li>
	 *   <li>Inisialisasi paging dengan total count dari {@code initCriteria(false)}.</li>
	 *   <li>Query data dengan {@code initCriteria(true)} dibatasi {@code PAGE_SIZE} record
	 *       dan offset sesuai halaman aktif.</li>
	 *   <li>Set model dan renderer pada grid untuk merender data baru.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error eksplisit — exception
	 * diteruskan ke framework ZK yang akan menampilkan pesan error default.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Konstanta {@code PAGE_SIZE=10} menentukan ukuran halaman.
	 * Jika perlu ukuran halaman yang berbeda per konteks, pertimbangkan membuat field
	 * configurable atau menggunakan {@code Common.ROWS_COUNT_ON_PAGE}.</p>
	 *
	 * @param event event ZK yang memicu pencarian; boleh null jika dipanggil langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		persetujuan = false;
		if (paging != null) {
			paging.setPageSize(PAGE_SIZE);
		}
		Common.initPaging(initCriteria(false), paging);

		List<ProsesTransfer> prosesTransfer = initCriteria(true).setMaxResults(PAGE_SIZE)
				.setFirstResult(PAGE_SIZE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(prosesTransfer);
		grid.setRowRenderer(new ProsesTransferRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun komponen form UI untuk entitas {@code ProsesTransfer} (implementasi FormSop).
	 *
	 * <p><b>Tujuan:</b> Membangun dan mengembalikan {@code MyGrid} yang berisi seluruh
	 * komponen form untuk entri/edit {@code ProsesTransfer}: kode (auto-generate), tanggal
	 * pengajuan, judul transfer, cara transfer, status persetujuan, keterangan, dan
	 * informasi realisasi. Metode ini diimplementasikan dari antarmuka {@code FormSop}
	 * sehingga dapat dipanggil dari alur SOP eksternal maupun dari modal window internal.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Set field instance: {@code prosesTransfer} dan {@code disposisiSop}.</li>
	 *   <li>Bangun grid dua kolom (label 30% | input 70%).</li>
	 *   <li>Baris Kode: tampilkan kode auto-generate sebagai Label (tidak bisa diubah).</li>
	 *   <li>Baris Tanggal Pengajuan: {@code MyDatebox} editable jika belum disetujui,
	 *       Label jika sudah disetujui.</li>
	 *   <li>Baris Judul Transfer: {@code Textbox} editable jika belum disetujui,
	 *       Label jika sudah disetujui. Auto-save onChange jika mode edit.</li>
	 *   <li>Baris Cara Transfer: {@code Combobox} dari tabel {@code CaraPembayaranTransfer}
	 *       yang sesuai dengan satuan kerja saat ini. Default ke cara pembayaran default.</li>
	 *   <li>Baris Status Persetujuan: checkbox {@code setujuiOleh} hanya terlihat dalam
	 *       mode persetujuan dan belum ada disposisiSop.</li>
	 *   <li>Baris Keterangan: {@code Textbox} 3 baris, auto-save onChange jika mode edit.</li>
	 *   <li>Baris Realisasi: tampilkan siapa yang merealisasikan dan kapan (jika sudah).</li>
	 *   <li>Jika {@code disposisiSop} ada: tambahkan panel embedded
	 *       {@code initDetail} (daftar transfer) dalam groupbox.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Auto-save:</b> Untuk entitas yang sudah tersimpan (id tidak null), perubahan
	 * field langsung disimpan ke database via {@code Common.refreshUpdate} tanpa menunggu
	 * tombol Simpan ditekan. Ini memastikan data tidak hilang jika window ditutup paksa.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Field-field yang diisi di sini ({@code nama}, {@code keterangan},
	 * {@code caraBayarByr}, {@code tanggalPembuatan}, {@code setujuiOleh}, {@code tanggalRealisasikan})
	 * harus tersedia saat {@link #onSave(Event)} dipanggil. Pastikan semua baris form
	 * menggunakan guard null sebelum mengakses field karena form juga dipakai dari SOP
	 * di mana beberapa komponen mungkin tidak ada.</p>
	 *
	 * @param generalValueObject entitas {@code ProsesTransfer} yang akan ditampilkan/diedit;
	 *                           tidak boleh null
	 * @param disposisiSop       informasi disposisi SOP jika form dipanggil dari alur SOP;
	 *                           boleh null untuk penggunaan non-SOP
	 * @param save               tombol simpan yang labelnya akan diubah berdasarkan konteks
	 * @param setujui            event listener yang dipicu saat status persetujuan berubah;
	 *                           boleh null untuk penggunaan non-SOP
	 * @return {@code MyGrid} yang berisi seluruh komponen form, siap ditambahkan ke window
	 * @throws Exception jika pembuatan komponen ZK atau query Hibernate gagal
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujui) throws Exception {

		this.prosesTransfer = (ProsesTransfer) generalValueObject;
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

		if (prosesTransfer.getKode() == null || prosesTransfer.getKode().trim().isEmpty()) {
			String noAgenda = generateCode(true);
			prosesTransfer.setKode(noAgenda);
		}

		EventListener eventListenerSImpan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (prosesTransfer.getId() != null) {
					prosesTransfer.setNama(nama.getValue());
					prosesTransfer.setKeterangan(keterangan.getValue());
					prosesTransfer.setCaraPembayaranTransfer(
							(CaraPembayaranTransfer) (caraBayarByr.getSelectedItem() == null ? null
									: caraBayarByr.getSelectedItem().getValue()));
					prosesTransfer.setTanggalPembuatan(tanggalPembuatan.getValue());

					prosesTransfer.setTanggalRealisasikan(tanggalRealisasikan.getValue());

					Common.refreshUpdate(prosesTransfer);
				}

			}
		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode *"));
		row.appendChild(new Label(prosesTransfer.getKode()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan *"));
		tanggalPembuatan = new MyDatebox(prosesTransfer.getTanggalPembuatan());

		if (prosesTransfer.getDisetujuiOleh() != null) {
			row.appendChild(new Label(formatTanggal(prosesTransfer.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setReadonly(true);

		tanggalPembuatan.addEventListener("onChange", eventListenerSImpan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Transfer *"));
		nama = new Textbox(text(prosesTransfer.getNama()));
		if (prosesTransfer.getDisetujuiOleh() != null) {
			row.appendChild(new Label(text(prosesTransfer.getNama())));
		} else {
			row.appendChild(nama);
		}
		nama.setWidth("90%");
		nama.setRows(2);

		nama.addEventListener("onChange", eventListenerSImpan);

		SatuanKerja satuanKerja = Common.getSatuanKerja();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Transfer *"));

		CaraPembayaranTransfer caraPembayaranGajiDefault = (CaraPembayaranTransfer) HibernateUtil.currentSession()
				.createCriteria(CaraPembayaranTransfer.class).add(Restrictions.eq("defaultPembayaran", true))
				.add(Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))))
				.setMaxResults(1).uniqueResult();

		caraBayarByr = new Combobox();
		if (prosesTransfer.getDisetujuiOleh() != null) {
			row.appendChild(new Label(prosesTransfer.getCaraPembayaranTransfer() == null
					? (caraPembayaranGajiDefault == null ? "" : caraPembayaranGajiDefault.getNama())
					: prosesTransfer.getCaraPembayaranTransfer().getNama()));
		} else {
			row.appendChild(caraBayarByr);
		}
		Common.insertCombo(caraBayarByr, "nama", "keterangan", CaraPembayaranTransfer.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

		Common.selectComboItem(true, caraBayarByr,
				prosesTransfer.getCaraPembayaranTransfer() == null ? caraPembayaranGajiDefault
						: prosesTransfer.getCaraPembayaranTransfer());
		caraBayarByr.setReadonly(true);

		caraBayarByr.addEventListener("onChange", eventListenerSImpan);

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(setujuiOleh = new MyCheckboxConfig("Setujui"));
		setujuiOleh.setChecked(prosesTransfer.getDisetujuiOleh() != null);

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
					setujui.onEvent(new Event("", null, prosesTransfer.getDisetujuiOleh() != null));
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));

		keterangan = new Textbox(text(prosesTransfer.getKeterangan()));
		if (prosesTransfer.getDisetujuiOleh() != null || viewOnly) {
			row.appendChild(new Label(prosesTransfer.getKeterangan()));
		} else {
			row.appendChild(keterangan);

			if (prosesTransfer.getId() != null) {
				EventListener eventListenerEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						prosesTransfer.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(prosesTransfer);

					}
				};
				keterangan.addEventListener("onChange", eventListenerEventListener);
			}

		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		keterangan.addEventListener("onChange", eventListenerSImpan);

		if (prosesTransfer.getRealisasikanOleh() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telah direalisasikan oleh"));
			row.appendChild(new Label(prosesTransfer.getRealisasikanOleh().getUserNama()));
		}
		tanggalRealisasikan = new MyDatebox(prosesTransfer.getTanggalRealisasikan());
		if (prosesTransfer.getRealisasikanOleh() == null && prosesTransfer.getDisetujuiOleh() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Realisasi *"));

			row.appendChild(tanggalRealisasikan);

			tanggalRealisasikan.setFormat(Common.dateFormat.get().toPattern());
			tanggalRealisasikan.setReadonly(true);

		} else if (prosesTransfer.getTanggalRealisasikan() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telah direalisasikan tanggal"));
			row.appendChild(new Label(Common.dateFormat61.get().format(prosesTransfer.getTanggalRealisasikan())));
		}

		if (disposisiSop != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.setParent(row);
			groupboxStyled.setHeight("550px");
			groupboxStyled.appendChild(new MyCaptionStyled("Daftar Transfer"));

			initDetail(groupboxStyled, prosesTransfer, disposisiSop);
		}

		return grid;
	}

	/**
	 * Mengembalikan istilah/label untuk modul ini dalam alur SOP.
	 *
	 * <p><b>Tujuan:</b> Menyediakan nama yang ditampilkan pada judul panel SOP, log
	 * disposisi, dan notifikasi yang mengidentifikasi jenis entitas yang sedang diproses
	 * dalam alur SOP (Standard Operating Procedure).</p>
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan string literal "Proses Pengajuan Transfer" yang
	 * digunakan oleh framework SOP untuk menampilkan judul yang sesuai pada antarmuka
	 * persetujuan dan riwayat disposisi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Ubah string ini jika nama modul berubah. Pastikan konsisten
	 * dengan label yang ditampilkan di menu navigasi dan judul halaman.</p>
	 *
	 * @return string "Proses Pengajuan Transfer" sebagai label modul dalam konteks SOP
	 * @throws Exception tidak melempar exception (signature wajib karena implementasi interface)
	 */
	@Override
	public String istilah() throws Exception {
		return "Proses Pengajuan Transfer";
	}

	/**
	 * Mengembalikan entitas {@code ProsesTransfer} saat ini sebagai {@code DataSop}.
	 *
	 * <p><b>Tujuan:</b> Mengimplementasikan metode {@code FormSop.ambil()} yang memungkinkan
	 * framework SOP mengakses entitas yang sedang diproses tanpa mengetahui tipe spesifiknya.
	 * Digunakan saat framework perlu menyimpan referensi disposisi ke entitas.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan field instance {@code prosesTransfer} yang
	 * merupakan entitas {@code ProsesTransfer} yang saat ini ditampilkan atau diedit.
	 * {@code ProsesTransfer} mengimplementasikan antarmuka {@code DataSop}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pastikan {@code prosesTransfer} selalu diinisialisasi
	 * sebelum metode ini dipanggil (mis. via {@link #init(ProsesTransfer)} atau
	 * {@link #form}).</p>
	 *
	 * @return entitas {@code ProsesTransfer} yang sedang aktif; bisa null jika belum
	 *         diinisialisasi
	 * @throws Exception tidak melempar exception (signature wajib karena implementasi interface)
	 */
	@Override
	public DataSop ambil() throws Exception {
		return prosesTransfer;
	}

	/**
	 * Mengembalikan kelas Java dari entitas yang dikelola oleh controller ini.
	 *
	 * <p><b>Tujuan:</b> Mengimplementasikan metode {@code FormSop.ambilClass()} yang
	 * diperlukan framework SOP dan ekspor data untuk mengetahui tipe entitas secara
	 * reflektif tanpa membuat instance.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan {@code ProsesTransfer.class} sebagai literal
	 * kelas Java. Digunakan antara lain oleh {@code Common.insertProperty} untuk
	 * introspeksi getter dan oleh framework SOP untuk pengecekan tipe.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali entitas utama yang dikelola
	 * kelas ini berubah.</p>
	 *
	 * @return {@code ProsesTransfer.class}
	 * @throws Exception tidak melempar exception (signature wajib karena implementasi interface)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return ProsesTransfer.class;
	}

	/**
	 * Menyetel flag mode persetujuan pada controller.
	 *
	 * <p><b>Tujuan:</b> Mengimplementasikan metode {@code FormSop.setPersetujuan()} yang
	 * memungkinkan framework SOP mengubah mode controller dari mode pengajuan ke mode
	 * persetujuan (atau sebaliknya) secara programatik setelah konstruksi.</p>
	 *
	 * <p><b>Cara kerja:</b> Menyimpan nilai {@code persetujuan} ke field instance.
	 * Nilai ini akan digunakan oleh {@link #form} untuk menentukan komponen mana yang
	 * editable dan oleh renderer untuk menentukan tombol mana yang ditampilkan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pemanggil bertanggung jawab untuk membangun ulang UI
	 * (mis. memanggil {@code form()} atau {@code init()}) setelah memanggil metode ini
	 * agar perubahan flag tercermin di tampilan.</p>
	 *
	 * @param persetujuan {@code true} untuk mode persetujuan; {@code false} untuk mode
	 *                    pengajuan biasa
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Men-generate kode unik untuk {@code ProsesTransfer} baru berdasarkan format nomor surat.
	 *
	 * <p><b>Tujuan:</b> Menghasilkan kode identifikasi unik untuk setiap entitas
	 * {@code ProsesTransfer} menggunakan format nomor surat yang dikonfigurasi di
	 * {@code NomorSuratAlurKeuangan.DPC}. Kode ini berfungsi sebagai nomor referensi
	 * resmi dokumen pengajuan transfer yang dapat digunakan untuk pelaporan dan audit.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code NomorSuratAlurKeuangan.DPC} null atau belum dikonfigurasi,
	 *       gunakan {@code Common.getGeneratedBarCode()} sebagai fallback (kode acak).</li>
	 *   <li>Tentukan index berikutnya: jika konfigurasi menggunakan index urut manual
	 *       ({@code getGunakanIndexUrut()}), ambil dari {@code getNomorIndex()};
	 *       jika tidak, hitung dari database via {@link #getindex(NomorSurat)}.</li>
	 *   <li>Jika {@code tambah=true}, increment index di database via
	 *       {@code NomorSurat.tambahIndexNomorSurat()}.</li>
	 *   <li>Format nomor via {@code NomorSurat.format(index, tanggal)}.</li>
	 *   <li>Pastikan kode unik via {@code KodeUnikUtil.pastikanUnik()} yang menambahkan
	 *       suffix -2, -3, dst jika kode sudah ada.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Konfigurasi format nomor surat diatur di menu administrasi
	 * {@code NomorSuratAlurKeuangan} dengan kunci DPC. Jika format perlu diubah, konfigurasikan
	 * di sana tanpa mengubah kode Java ini.</p>
	 *
	 * @param tambah {@code true} untuk increment counter di database (saat membuat entitas baru);
	 *               {@code false} untuk hanya membaca nomor berikutnya tanpa increment
	 *               (untuk preview kode sebelum simpan)
	 * @return string kode yang diformat sesuai template nomor surat, dijamin unik
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurKeuangan.DPC == null || NomorSuratAlurKeuangan.DPC.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurKeuangan.DPC.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurKeuangan.DPC.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurKeuangan.DPC.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurKeuangan.DPC.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurKeuangan.DPC.getNomorSurat().format(index, WaktuUtil.getDate());
		return ais.action.master.KodeUnikUtil.pastikanUnik(ProsesTransfer.class, noAgenda);
	}

	/**
	 * Menghitung index urutan berikutnya untuk nomor surat berdasarkan data di database.
	 *
	 * <p><b>Tujuan:</b> Menentukan nomor urutan (index) berikutnya untuk kode
	 * {@code ProsesTransfer} dengan menghitung jumlah record yang sudah ada di database
	 * sesuai aturan penomoran yang dikonfigurasi (reset per tahun, per bulan, atau per tanggal
	 * reset tertentu, serta filter berdasarkan nomor surat atau kelompok nomor surat).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code nomorSurat} null, kembalikan 0.</li>
	 *   <li>Bangun query {@code Criteria} pada {@code ProsesTransfer} dengan join ke
	 *       {@code nomorSuratAlurKeuangan} dan {@code nomorSurat}.</li>
	 *   <li>Filter berdasarkan aturan penomoran:
	 *       <ul>
	 *         <li>Jika {@code urutBerdasarkanNomor}: filter berdasarkan nomor surat yang sama.</li>
	 *         <li>Jika {@code urutBerdasarkanKelompok}: filter berdasarkan kelompok nomor surat.</li>
	 *         <li>Lainnya: tidak ada filter tambahan (gunakan semua record).</li>
	 *       </ul>
	 *   </li>
	 *   <li>Filter reset: tambahkan filter tahun jika {@code resetUrutanTiapTahun},
	 *       filter tahun+bulan jika {@code resetUrutanTiapBulan}, atau filter
	 *       {@code tanggalPembuatan >= resetTiap} jika ada tanggal reset.</li>
	 *   <li>Hitung COUNT menggunakan {@code Projections.rowCount()}.</li>
	 *   <li>Kembalikan count + 1 sebagai index berikutnya.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Jika query mengembalikan null (tidak ada record),
	 * index dimulai dari 0 dan dikembalikan sebagai 1 (++index).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Logika ini identik di semua Action yang menggunakan
	 * {@code NomorSuratAlurKeuangan} — pertimbangkan mengekstrak ke utility class
	 * jika terlalu banyak duplikasi.</p>
	 *
	 * @param nomorSurat konfigurasi nomor surat yang menentukan aturan penomoran;
	 *                   boleh null (akan mengembalikan 0)
	 * @return index urutan berikutnya (count existing + 1), minimal 1
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}

		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(ProsesTransfer.class)
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
						.equals(Common.dateFormat8.get().format(sekarang))
						|| nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("waktu", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * Membangun map parameter untuk laporan cetak PDF {@code ProsesTransfer}.
	 *
	 * <p><b>Tujuan:</b> Mengumpulkan semua data yang diperlukan oleh template laporan
	 * JasperReports ({@code akunting/pengajuan_cheque}) ke dalam sebuah {@code Map}
	 * yang dilewatkan ke mesin laporan. Metode ini menjadi jembatan antara entitas
	 * Hibernate dan template laporan statis.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Refresh entitas dari database jika sudah tersimpan (untuk memastikan data terkini).</li>
	 *   <li>Buat map acak via {@code HashMapGenerator.getRand()} untuk menghindari konflik nama.</li>
	 *   <li>Tambahkan properti ProsesTransfer via {@code Common.insertProperty} (introspeksi getter).</li>
	 *   <li>Tambahkan parameter disposisi SOP via {@code DisposisiAlurSop.parameterMap}.</li>
	 *   <li>Query semua {@code DaftarPengajuanTransfer} milik ProsesTransfer ini,
	 *       diurutkan berdasarkan kode dan nama.</li>
	 *   <li>Untuk setiap DaftarPengajuanTransfer, buat sub-map dengan properti transfer
	 *       ditambah field status persetujuan yang sudah diformat.</li>
	 *   <li>Hitung total nominal semua transfer.</li>
	 *   <li>Hapus kunci yang mengandung "disposisiSop" dari map (untuk menghindari
	 *       konflik serialisasi).</li>
	 *   <li>Tambahkan list sub-map ({@code maps}) dan total ({@code totalSemua}) ke map utama.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Tidak ada penanganan error — exception diteruskan ke pemanggil.
	 * Error dalam query DaftarPengajuanTransfer akan menghentikan proses cetak.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Pastikan key dalam map sesuai dengan variabel parameter
	 * yang digunakan di template JasperReports. Perubahan nama field di template
	 * membutuhkan perubahan key yang sesuai di sini.</p>
	 *
	 * @param prosesTransfer entitas ProsesTransfer yang akan dicetak; tidak boleh null
	 * @return {@code Map} yang berisi semua parameter laporan, termasuk list transfer
	 *         dan total nilai
	 * @throws Exception jika refresh database atau query DaftarPengajuanTransfer gagal
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(ProsesTransfer prosesTransfer) throws Exception {
		if (prosesTransfer != null && prosesTransfer.getId() != null) {
			HibernateUtil.currentSession().refresh(prosesTransfer);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", prosesTransfer.getId());

		Common.insertProperty(ProsesTransfer.class, prosesTransfer, parameters, "data");

		DisposisiAlurSop.parameterMap(prosesTransfer.getDisposisiSop(), parameters);

		Session session = HibernateUtil.currentSession();
		List<DaftarPengajuanTransfer> daftarPengajuanTransfers = session.createCriteria(DaftarPengajuanTransfer.class)
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.add(Restrictions.eq("prosesTransfer", prosesTransfer)).list();
		List<Map> maps = new ArrayList<Map>();
		Double totalSemua = 0.0;
		for (DaftarPengajuanTransfer daftarPengajuanTransfer : daftarPengajuanTransfers) {
			Map map = new HashMap();
			Common.insertProperty(DaftarPengajuanTransfer.class, daftarPengajuanTransfer, map, "data", 2,
					"prosesTransfer");

			String status = "";
			if (daftarPengajuanTransfer.getProsesTransfer().getDisetujuiOleh() == null) {
				status = "Belum disetujui";
			} else {
				status = "Disetujui oleh "
						+ daftarPengajuanTransfer.getProsesTransfer().getDisetujuiOleh().getUserNama() + " pada "
						+ (daftarPengajuanTransfer.getProsesTransfer().getTanggalPersetujuan() == null ? ""
								: Common.dateFormat51.get()
										.format(daftarPengajuanTransfer.getProsesTransfer().getTanggalPersetujuan()));
			}

			map.put("status_persetujuan", status);

			map.put("perpustakaan", daftarPengajuanTransfer.getProsesTransfer().getKeterangan());

			map.put("tanggal_persetujuan", daftarPengajuanTransfer.getProsesTransfer().getTanggalPersetujuan());
			map.put("disetujui_oleh", daftarPengajuanTransfer.getProsesTransfer().getDisetujuiOleh() == null ? ""
					: daftarPengajuanTransfer.getProsesTransfer().getDisetujuiOleh().getUserNama());

			totalSemua += nilai(daftarPengajuanTransfer.getNominal());
			maps.add(map);
		}

		parameters.put("maps", maps);
		parameters.put("totalSemua", totalSemua);

		for (Object o : parameters.keySet()) {
			if (o.toString().contains("disposisiSop")) {
				parameters.put(o.toString(), null);
			}
		}
		return parameters;
	}

	/**
	 * Menghasilkan file PDF laporan untuk satu entitas {@code ProsesTransfer}.
	 *
	 * <p><b>Tujuan:</b> Mengimplementasikan metode {@code DataCriteria.cetakData()} yang
	 * digunakan oleh framework ekspor data ({@code Common.cetakData}) untuk menghasilkan
	 * file PDF yang dapat diunduh. File ini berisi dokumen pengajuan transfer resmi yang
	 * bisa dicetak dan ditandatangani.</p>
	 *
	 * <p><b>Cara kerja:</b> Melakukan cast {@code GeneralValueObject} ke {@code ProsesTransfer},
	 * lalu memanggil {@code Report.generateFileReport} dengan parameter dari
	 * {@link #parameter(ProsesTransfer)}, menggunakan template laporan
	 * {@code akunting/pengajuan_cheque}, format PDF, dan tanggal dokumen dari
	 * {@code tanggalPembuatan}.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari pembuatan laporan diteruskan ke pemanggil.
	 * Framework ekspor akan menampilkan pesan error jika file tidak bisa dibuat.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Template laporan berada di folder report dengan nama
	 * {@code akunting/pengajuan_cheque.jrxml}. Jika template berubah, pastikan key
	 * parameter di {@link #parameter(ProsesTransfer)} masih sesuai.</p>
	 *
	 * @param generalValueObject entitas {@code ProsesTransfer} yang akan dicetak;
	 *                           harus bisa di-cast ke {@code ProsesTransfer}
	 * @return {@code File} PDF yang berisi laporan pengajuan transfer; tidak null
	 * @throws Exception jika pembuatan laporan atau parameter gagal
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		ProsesTransfer prosesTransfer = (ProsesTransfer) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(prosesTransfer), "akunting/pengajuan_cheque",
				prosesTransfer.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * Mencetak dan menampilkan laporan PDF pengajuan transfer langsung di browser.
	 *
	 * <p><b>Tujuan:</b> Metode statis yang dapat dipanggil dari mana saja untuk menampilkan
	 * laporan pengajuan transfer sebagai PDF yang dibuka langsung di jendela browser pengguna
	 * (bukan diunduh sebagai file). Metode ini dipanggil otomatis setelah penyimpanan berhasil
	 * via timer 2.5 detik, dan juga dapat dipanggil manual dari tombol cetak di grid.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Report.generatePDFReport} yang menghasilkan
	 * PDF dari template {@code akunting/pengajuan_cheque} dengan parameter dari
	 * {@link #parameter(ProsesTransfer)}, lalu menampilkannya di browser atau membuka
	 * tab baru. Tanggal dokumen diambil dari {@code prosesTransfer.getTanggalPembuatan()}.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception diteruskan ke pemanggil. Karena metode ini
	 * sering dipanggil dari timer (EventListener), pastikan pemanggil menangkap exception
	 * dengan tepat agar UI tidak rusak.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Metode statis ini memudahkan pemanggilan dari kelas lain
	 * tanpa perlu instance controller. Pastikan template JasperReports tersedia di
	 * classpath saat runtime.</p>
	 *
	 * @param prosesTransfer entitas {@code ProsesTransfer} yang akan dicetak laporannya;
	 *                       tidak boleh null (akan melempar NullPointerException)
	 * @throws Exception jika pembuatan parameter atau rendering PDF gagal
	 */
	@SuppressWarnings({})
	public static void cetak(ProsesTransfer prosesTransfer) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(prosesTransfer), "akunting/pengajuan_cheque",
				prosesTransfer.getTanggalPembuatan());
	}
}
