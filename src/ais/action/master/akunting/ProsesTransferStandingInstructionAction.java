package ais.action.master.akunting;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.East;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.ProsesTransferStandingInstruction;
import ais.database.model.akunting.StandingInstruction;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
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
 * <h3>ProsesTransferStandingInstructionAction — Pemrosesan Transfer Standing Instruction</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini adalah Action ZK yang mengelola siklus hidup dokumen
 * Proses Transfer Standing Instruction (SI) — yaitu instruksi transfer berulang/terjadwal
 * yang umumnya dikaitkan dengan pembayaran gaji pegawai ke berbagai bank. Modul ini
 * memungkinkan staf keuangan membuat batch transfer SI dari daftar {@link StandingInstruction}
 * yang aktif, mendapatkan persetujuan dari pejabat berwenang, lalu merealisasikannya.</p>
 *
 * <p><b>Cara kerja:</b> Setiap {@link StandingInstruction} menyimpan informasi routing
 * bank dalam kolom {@code transferVia} sebagai JSON object, di mana kunci adalah ID bank
 * dan nilai adalah objek berisi nilai transfer dan referensi ke
 * {@link ProsesTransferStandingInstruction} yang telah memilihnya ({@code "si": "id"}).
 * Ketika pengguna membuat proses baru, ia memilih SI yang belum diproses (transferVia
 * belum memiliki referensi "si"), memasukkannya ke dalam batch, lalu menyimpan.
 * Sistem akan memperbarui field {@code transferVia} di setiap SI yang dipilih dengan
 * menambahkan ID proses baru.</p>
 *
 * <p><b>Alur persetujuan:</b>
 * <ol>
 *   <li>Pembuatan: staf membuat entri baru, memilih SI, dan menyimpan.</li>
 *   <li>Persetujuan: pejabat mengklik tombol centang di baris grid; sistem mengisi
 *       {@code disetujuiOleh} dan {@code tanggalPersetujuan}.</li>
 *   <li>Pembatalan persetujuan: tombol warning di baris; sistem menghapus {@code disetujuiOleh}.</li>
 *   <li>Realisasi: setelah disetujui, tombol "Realisasikan" tersedia di form detail;
 *       mengisi {@code realisasikanOleh} dan {@code tanggalRealisasikan}.</li>
 * </ol></p>
 *
 * <p><b>Panel detail (East):</b> Jendela form memiliki panel East yang menampilkan
 * daftar SI yang terkait. Untuk entri baru, panel menampilkan daftar SI yang belum
 * diproses lengkap dengan checkbox untuk memilih. Untuk entri yang sudah ada, panel
 * menampilkan SI yang sudah terpilih dalam mode read-only. Total nilai ditampilkan
 * di footer dan diperbarui secara real-time saat checkbox diubah.</p>
 *
 * <p><b>Threading:</b> Seluruh operasi UI dan DB berjalan di thread ZK event-dispatcher.
 * Operasi cetak dijadwalkan async via {@code Common.createDefaultTimer()} dengan delay
 * 2,5 detik agar tidak memblokir UI.</p>
 *
 * <p><b>Pemeliharaan:</b> Jika format JSON {@code transferVia} di {@link StandingInstruction}
 * berubah, pastikan logika parsing di {@code reload()} dan {@code onSave()} diperbarui
 * secara konsisten. Perhatikan bahwa field {@code prosesStanding} di SI menggunakan
 * format string CSV {@code ",id1,id2,"} untuk menyimpan relasi — ini adalah desain
 * lama yang tidak menggunakan tabel relasi.</p>
 *
 * @author AIS
 * @see ProsesTransferStandingInstruction
 * @see StandingInstruction
 */
public class ProsesTransferStandingInstructionAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * ID serialisasi versi kelas untuk kompatibilitas {@code Serializable}.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
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

	private ProsesTransferStandingInstruction prosesTransferStandingInstruction;
	private MyToolbarbuttonConfig add;

	private East east;

	private boolean persetujuan = false;
	private boolean approve = false;
	private boolean reject = false;
	private EventListener eventListener = null;

	/**
	 * Konstruktor default — dipakai saat ZK meng-autowire kelas ini dari file ZUL.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi action dalam mode pengajuan normal. Tidak ada
	 * inisialisasi tambahan yang diperlukan karena semua field diinisialisasi melalui
	 * autowire ZK dan di {@code doAfterCompose()}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada dependensi yang perlu diinjeksi sebelum composing,
	 * tambahkan di sini.</p>
	 */
	public ProsesTransferStandingInstructionAction() {

	}

	/**
	 * Konstruktor mode persetujuan — dipakai saat action dibuat secara programatis
	 * dari alur SOP atau menu persetujuan.
	 *
	 * <p><b>Tujuan:</b> Mengaktifkan mode persetujuan di mana form ditampilkan untuk
	 * pejabat yang akan menyetujui atau menolak proses transfer yang diajukan.</p>
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan
	 */
	public ProsesTransferStandingInstructionAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Hook ZK pra-composing untuk memeriksa keamanan akses sebelum halaman dirender.
	 *
	 * <p><b>Tujuan:</b> Memastikan hanya pengguna dengan sesi valid yang dapat mengakses
	 * modul ini. Dipanggil oleh framework ZK sebelum komponen ZUL di-compose.</p>
	 *
	 * @param page     halaman ZK yang sedang di-compose
	 * @param parent   komponen induk
	 * @param compInfo metadata komponen
	 * @return {@code ComponentInfo} dari superclass
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Hook ZK pasca-composing yang menginisialisasi seluruh komponen UI setelah semua
	 * elemen ZUL berhasil di-wire ke field Java.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan halaman daftar proses transfer SI sehingga siap
	 * digunakan pengguna — termasuk filter tanggal, hak akses tombol, paging, dan
	 * ekspor data.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose()} untuk autowire komponen ZUL.</li>
	 *   <li>Menginisialisasi locale dengan {@code Common.initLaguage()}.</li>
	 *   <li>Menyetel filter tanggal: start = 6 bulan lalu, end = besok, keduanya
	 *       read-only.</li>
	 *   <li>Menyetel visibilitas tombol Add dan hak APPROVE/REJECT/UPDATE/DELETE.</li>
	 *   <li>Memanggil {@code onSearchDefault()} segera untuk mengisi grid pertama kali.</li>
	 *   <li>Menginisialisasi paging dengan listener refresh.</li>
	 *   <li>Menambahkan tombol ekspor cetak dan upload ke toolbar.</li>
	 * </ol></p>
	 *
	 * @param comp komponen root ZK hasil composing
	 * @throws Exception jika terjadi kesalahan saat inisialisasi
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

		String[] contents = new String[] { "id", "kode", "tanggalPembuatan", "waktuTransfer", "nama", "keterangan",
				"bankSumber", "noRekSumber", "waktuTransfer", "realisasikanOleh", "tanggalRealisasikan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ProsesTransferStandingInstruction.class, this,
				contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ProsesTransferStandingInstruction.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer baris grid untuk menampilkan setiap {@link ProsesTransferStandingInstruction}
	 * dalam tabel daftar utama.
	 *
	 * <p><b>Untuk apa:</b> Mengubah objek domain menjadi baris visual di grid dengan
	 * informasi kode, tanggal, penyetuju, realisator, nama, nilai, keterangan, SOP,
	 * checkbox aktif, dan tombol aksi (approve/cancel/view/print).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada kolom baru di grid ZUL, tambahkan sel baru di
	 * {@code render()} dengan urutan yang sesuai kolom header.</p>
	 */
	class ProsesTransferStandingInstructionRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris {@link ProsesTransferStandingInstruction} ke dalam komponen
		 * ZK di dalam {@link Row} yang disediakan framework.
		 *
		 * <p><b>Tujuan:</b> Mengubah objek domain menjadi representasi visual yang
		 * informatif, termasuk status persetujuan interaktif (tombol centang/batal),
		 * tombol realisasi/view, dan tautan SOP.</p>
		 *
		 * <p><b>Cara kerja langkah demi langkah:</b>
		 * <ol>
		 *   <li>Menghasilkan kode jika masih kosong (fallback untuk data lama) dan
		 *       langsung menyimpannya via {@code Common.refreshUpdate()}.</li>
		 *   <li>Menampilkan Vbox berisi kode dan tanggal pembuatan di sel pertama.</li>
		 *   <li>Menampilkan penyetuju dan tanggal persetujuan di sel kedua.</li>
		 *   <li>Menampilkan realisator dan tanggal realisasi di sel ketiga.</li>
		 *   <li>Menampilkan revisi nama via {@code RevisiHelper.createNewRevisi()}.</li>
		 *   <li>Menampilkan nilai dalam format angka.</li>
		 *   <li>Menampilkan keterangan dan link SOP jika ada.</li>
		 *   <li>Menampilkan checkbox Aktif yang bisa diubah langsung di grid.</li>
		 *   <li>Menampilkan tombol aksi: Ubah/Copy/Hapus via {@code copyEditDeleteButtons},
		 *       tombol persetujuan (centang, hanya jika belum disetujui dan punya hak APPROVE),
		 *       tombol pembatalan (hanya jika sudah disetujui dan punya hak REJECT),
		 *       tombol lihat detail (jika sudah disetujui), dan tombol cetak.</li>
		 *   <li>Tombol persetujuan menampilkan konfirmasi via MessageBox sebelum mengisi
		 *       {@code disetujuiOleh} dan {@code tanggalPersetujuan}.</li>
		 *   <li>Tombol pembatalan menampilkan konfirmasi sebelum menghapus {@code disetujuiOleh}.</li>
		 * </ol></p>
		 *
		 * @param arg0 baris ZK yang harus diisi komponen anak
		 * @param arg1 objek data yang akan di-cast ke {@link ProsesTransferStandingInstruction}
		 * @throws Exception jika terjadi kesalahan saat merender atau mengakses DB
		 *
		 * <p><b>Penanganan error:</b> Error pada level baris tidak ditangkap di sini;
		 * error DB akan naik ke framework ZK. Pastikan data konsisten sebelum render.</p>
		 *
		 * <p><b>Pemeliharaan:</b> Tombol approve/reject menggunakan hak APPROVE/REJECT
		 * yang dikonfigurasi di modul hak akses. Jika alur persetujuan berubah (misalnya
		 * perlu dua tingkat persetujuan), logika tombol perlu dimodifikasi.</p>
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final ProsesTransferStandingInstruction prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) arg1;

			if (prosesTransferStandingInstruction.getKode() == null
					|| prosesTransferStandingInstruction.getKode().trim().isEmpty()) {
				String noAgenda = generateCode(true);
				prosesTransferStandingInstruction.setKode(noAgenda);

				Common.refreshUpdate(prosesTransferStandingInstruction);
			}

			Vbox aaa = new Vbox();
			aaa.setParent(arg0);
			new Label(prosesTransferStandingInstruction.getKode()).setParent(aaa);
			new Label(Common.dateFormat.get().format(prosesTransferStandingInstruction.getTanggalPembuatan()))
					.setParent(aaa);

			aaa = new Vbox();
			aaa.setParent(arg0);

			if (prosesTransferStandingInstruction.getDisetujuiOleh() != null) {
				new Label(prosesTransferStandingInstruction.getDisetujuiOleh().getUserNama()).setParent(aaa);
			}

			new Label(Common.dateFormat.get()
					.format(prosesTransferStandingInstruction.getTanggalPersetujuan() != null
							? prosesTransferStandingInstruction.getTanggalPersetujuan()
							: prosesTransferStandingInstruction.getTanggalPembuatan()))
					.setParent(aaa);

			aaa = new Vbox();
			aaa.setParent(arg0);

			if (prosesTransferStandingInstruction.getRealisasikanOleh() != null) {
				new Label(prosesTransferStandingInstruction.getRealisasikanOleh().getUserNama()).setParent(aaa);
			}

			new Label(prosesTransferStandingInstruction.getTanggalRealisasikan() == null ? ""
					: Common.dateFormat.get().format(prosesTransferStandingInstruction.getTanggalRealisasikan()))
					.setParent(aaa);

			RevisiHelper.createNewRevisi(ProsesTransferStandingInstruction.class, prosesTransferStandingInstruction,
					prosesTransferStandingInstruction.getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(prosesTransferStandingInstruction.getNilai())).setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(prosesTransferStandingInstruction.getKeterangan()).setParent(a);

			if (prosesTransferStandingInstruction.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa,
						"SOP " + prosesTransferStandingInstruction.getDisposisiSop().getKeterangan() + " ("
								+ prosesTransferStandingInstruction.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(prosesTransferStandingInstruction.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit && prosesTransferStandingInstruction.getDisetujuiOleh() == null);
			checkbox.setChecked(prosesTransferStandingInstruction.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					prosesTransferStandingInstruction.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(prosesTransferStandingInstruction);
				}
			});

			Hbox aa;
			(aa = Common.copyEditDeleteButtons(edit, false,
					prosesTransferStandingInstruction.getDisetujuiOleh() == null && delete,
					prosesTransferStandingInstruction, ProsesTransferStandingInstructionAction.this)).setParent(arg0);
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

					cetak(prosesTransferStandingInstruction);
				}

			});
			button.setParent(aa);

			disetujui.setVisible(approve && prosesTransferStandingInstruction.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && prosesTransferStandingInstruction.getDisetujuiOleh() != null);

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

										prosesTransferStandingInstruction.setDisetujuiOleh(Common.getCurrentUser());
										prosesTransferStandingInstruction
												.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, prosesTransferStandingInstruction);
										session.flush();

										checkbox.setDisabled(
												!edit && prosesTransferStandingInstruction.getDisetujuiOleh() == null);

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

										prosesTransferStandingInstruction.setDisetujuiOleh(null);
										prosesTransferStandingInstruction.setTanggalPersetujuan(null);
										Common.refreshUpdate(session, prosesTransferStandingInstruction);
										session.flush();

										checkbox.setDisabled(
												!edit && prosesTransferStandingInstruction.getDisetujuiOleh() == null);

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

			if (prosesTransferStandingInstruction.getDisetujuiOleh() != null) {
				MyToolbarbuttonConfig disetujuia = new MyToolbarbuttonConfig("", "/img/svg/eye.svg");
				aa.appendChild(disetujuia);
				disetujuia.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(prosesTransferStandingInstruction);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				disetujuia.setParent(aa);

			}
		}

	}

	/**
	 * Handler event ZK untuk tombol "Tambah" — membuka form kosong untuk membuat
	 * proses transfer standing instruction baru.
	 *
	 * <p><b>Tujuan:</b> Entry point utama bagi staf keuangan untuk memulai proses
	 * batch transfer baru dari daftar standing instruction yang aktif.</p>
	 *
	 * <p><b>Cara kerja:</b> Menyetel {@code viewOnly = false} dan {@code persetujuan
	 * = false} untuk memastikan form terbuka dalam mode edit penuh, lalu memanggil
	 * {@code init()} dengan entitas baru untuk membangun konten window.</p>
	 *
	 * @param event event ZK yang memicu handler ini
	 * @throws Exception jika terjadi kesalahan saat membangun form
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		persetujuan = false;
		init(new ProsesTransferStandingInstruction());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Implementasi {@code DataInitDefault.init()} — membuka form edit/lihat untuk
	 * objek yang diberikan dari luar kelas ini.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan sistem SOP dan modul lain membuka form secara
	 * programatis melalui antarmuka generik {@code DataInitDefault}.</p>
	 *
	 * @param obj objek domain; akan di-cast ke {@link ProsesTransferStandingInstruction}
	 * @throws Exception jika cast gagal atau terjadi kesalahan saat membangun form
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) obj;
		init(prosesTransferStandingInstruction);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Membuka window modal proses transfer SI dari konteks eksternal (misalnya dari
	 * link di baris grid PenggantianKasKecil atau modul lain) tanpa memerlukan
	 * instance action yang sudah terikat ke ZUL.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan modul lain (misalnya renderer baris penggantian
	 * kas kecil) untuk membuka detail proses transfer secara modal tanpa navigasi
	 * halaman, hanya dengan menyediakan event listener callback dan objek yang ingin
	 * ditampilkan.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat instance baru {@code ProsesTransferStandingInstructionAction}.</li>
	 *   <li>Menyetel {@code eventListener}, {@code viewOnly = true}, dan
	 *       {@code persetujuan = true} pada instance baru.</li>
	 *   <li>Membuat {@link MyWindow} baru dan menempelkannya ke root halaman aktif.</li>
	 *   <li>Memanggil {@code init()} untuk membangun konten window.</li>
	 *   <li>Menampilkan window sebagai modal yang dapat ditutup.</li>
	 * </ol></p>
	 *
	 * @param eventListener listener yang dipanggil kembali setelah aksi di window
	 *                      (misalnya untuk refresh grid pemanggil)
	 * @param prosesTransferStandingInstruction objek yang akan ditampilkan
	 * @throws Exception jika terjadi kesalahan saat membuat atau menampilkan window
	 *
	 * <p><b>Thread safety:</b> Harus dipanggil dari thread ZK event-dispatcher.</p>
	 */
	public static void onAddExternal(EventListener eventListener,
			ProsesTransferStandingInstruction prosesTransferStandingInstruction) throws Exception {
		ProsesTransferStandingInstructionAction prosesTransferStandingInstructionAction = new ProsesTransferStandingInstructionAction();
		prosesTransferStandingInstructionAction.eventListener = eventListener;
		prosesTransferStandingInstructionAction.viewOnly = true;
		prosesTransferStandingInstructionAction.persetujuan = true;
		prosesTransferStandingInstructionAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(prosesTransferStandingInstructionAction.addWindow);
		prosesTransferStandingInstructionAction.addWindow.setHeight("95%");
		prosesTransferStandingInstructionAction.addWindow.setWidth("90%");

		prosesTransferStandingInstructionAction.init(prosesTransferStandingInstruction);

		prosesTransferStandingInstructionAction.addWindow.setVisible(true);
		prosesTransferStandingInstructionAction.addWindow.setClosable(true);
		prosesTransferStandingInstructionAction.addWindow.onModal();

	}

	/**
	 * Membangun konten window modal untuk form proses transfer standing instruction,
	 * termasuk layout, form detail, dan tombol aksi yang sesuai dengan status entitas.
	 *
	 * <p><b>Tujuan:</b> Method internal yang dipanggil oleh {@code onAdd()},
	 * {@code init(GeneralValueObject)}, dan {@code onAddExternal()} untuk menyiapkan
	 * window modal dengan konten yang tepat berdasarkan status entitas (baru/sudah ada,
	 * sudah disetujui/belum, sudah direalisasikan/belum).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyetel judul window sesuai mode (Tambah atau Ubah).</li>
	 *   <li>Membersihkan konten window lama.</li>
	 *   <li>Membangun {@code Borderlayout}: Center berisi form kiri, East berisi
	 *       panel detail SI (70% lebar) via {@code initDetail()}.</li>
	 *   <li>South berisi toolbar dengan tombol yang berbeda berdasarkan status:
	 *       <ul>
	 *         <li><b>Entitas baru:</b> Batal + Simpan/Ajukan.</li>
	 *         <li><b>Entitas ada, belum disetujui:</b> Selesai saja (tidak bisa edit).</li>
	 *         <li><b>Entitas ada, disetujui, belum direalisasikan:</b> Selesai + Realisasikan
	 *             (mengisi realisasikanOleh dan tanggalRealisasikan).</li>
	 *         <li><b>Entitas ada, disetujui, sudah direalisasikan, oleh user ini:</b>
	 *             Selesai + Batalkan Realisasikan.</li>
	 *       </ul>
	 *   </li>
	 * </ol></p>
	 *
	 * @param prosesTransferStandingInstruction objek yang akan diedit/ditampilkan
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI
	 *
	 * <p><b>Pemeliharaan:</b> Logika tombol bersarang cukup kompleks. Jika alur status
	 * berubah (misalnya menambah status baru), tambahkan cabang {@code if-else} baru
	 * di blok "entitas ada" dengan hati-hati.</p>
	 */
	private void init(final ProsesTransferStandingInstruction prosesTransferStandingInstruction) throws Exception {
		this.prosesTransferStandingInstruction = prosesTransferStandingInstruction;
		addWindow.setTitle(prosesTransferStandingInstruction.getId() == null ? "Tambah Proses Standing Instruction" : "Ubah Proses Standing Instruction");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(prosesTransferStandingInstruction, null, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		if (prosesTransferStandingInstruction.getId() == null) {

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

			if (prosesTransferStandingInstruction.getDisetujuiOleh() != null) {

				if (prosesTransferStandingInstruction.getRealisasikanOleh() == null) {
					save = new MyToolbarbuttonConfig("Realisasikan", "/img/svg/check-square.svg");
					save.setTooltiptext("Simpan");
					save.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							Session session = HibernateUtil.currentSession();
							session.refresh(prosesTransferStandingInstruction);
							prosesTransferStandingInstruction.setRealisasikanOleh(Common.getCurrentUser());
							if (tanggalRealisasikan != null) {
								prosesTransferStandingInstruction
										.setTanggalRealisasikan(tanggalRealisasikan.getValue());
							}
							Common.refreshSaveOrUpdate(session, prosesTransferStandingInstruction);
							session.flush();

							onSearchDefault(null);
							addWindow.setVisible(false);
						}
					});
					save.setParent(toolbar);
				} else {
					Tbmuser tbmuser = Common.getCurrentUser();
					if (prosesTransferStandingInstruction.getRealisasikanOleh() != null && tbmuser != null
							&& tbmuser.getUserId() != null
							&& prosesTransferStandingInstruction.getRealisasikanOleh().getUserId() != null
							&& prosesTransferStandingInstruction.getRealisasikanOleh().getUserId()
									.equals(tbmuser.getUserId())) {
						save = new MyToolbarbuttonConfig("Batalkan Realisasikan", "/img/svg/cancel_presentation.svg");

						save.setTooltiptext("Simpan");
						save.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {

								Session session = HibernateUtil.currentSession();
								session.refresh(prosesTransferStandingInstruction);
								prosesTransferStandingInstruction.setRealisasikanOleh(null);
								prosesTransferStandingInstruction.setTanggalRealisasikan(null);
								Common.refreshSaveOrUpdate(session, prosesTransferStandingInstruction);
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
		initDetail(east, prosesTransferStandingInstruction, null);

	}

	/**
	 * Peta berisi ID {@link StandingInstruction} yang dipilih oleh pengguna (kunci)
	 * beserta nilai transfer masing-masing (nilai). Digunakan untuk menghitung total
	 * di footer dan menentukan SI yang akan diproses saat menyimpan.
	 */
	private Map<Long, Double> longs;

	/**
	 * Peta id {@link StandingInstruction} &rarr; kumpulan kunci bank ({@code idBank} sebagai
	 * String) yang benar-benar dicentang oleh pengguna pada sesi form yang sedang berjalan.
	 *
	 * <p>
	 * <b>Kenapa perlu dilacak terpisah dari {@link #longs}:</b> checkbox per-baris menulis
	 * entri {@code {"nilai":n,"si":""}} ke {@link StandingInstruction#getTransferVia()} dan
	 * langsung mempersistensikannya seketika saat dicentang ({@code Common.refreshUpdate}) --
	 * jauh sebelum {@link #onSave(Event)} dipanggil. Bila pengguna menutup window lewat
	 * tombol "Batal" tanpa menekan Simpan, entri itu tertinggal di basis data dengan
	 * {@code si} tetap kosong (tidak ada langkah pembersihan pada tombol Batal). Tanpa peta
	 * ini, {@link #onSave(Event)} akan menstempel <b>seluruh</b> entri ber-{@code si} kosong
	 * pada setiap SI yang kebetulan terpilih -- termasuk entri terbengkalai dari sesi
	 * sebelumnya yang tidak pernah dicentang pada sesi saat ini -- sehingga total batch
	 * membengkak dari nilai lama yang tidak diminta. Peta ini membatasi
	 * {@link #onSave(Event)} hanya menstempel kunci bank yang benar-benar dicentang di sesi
	 * form ini.
	 * </p>
	 */
	private Map<Long, Set<String>> bankTerpilihSesiIni;

	/**
	 * Komponen footer grid detail yang menampilkan total nilai transfer secara real-time.
	 * Diperbarui setiap kali pengguna mencentang atau mencabut centang item SI.
	 */
	private Footer footerNilai;

	/**
	 * Menginisialisasi panel East (detail SI) dalam window form proses transfer,
	 * menampilkan daftar standing instruction yang relevan beserta kontrol pemilihan.
	 *
	 * <p><b>Tujuan:</b> Membangun panel kanan window yang menampilkan daftar
	 * {@link StandingInstruction} — untuk entitas baru tampil sebagai daftar checkable,
	 * untuk entitas yang sudah ada tampil sebagai daftar read-only dari SI yang sudah
	 * terpilih.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membersihkan konten panel East dan menyetel lebarnya ke 70%.</li>
	 *   <li>Menginisialisasi map {@code longs} untuk menyimpan pilihan SI.</li>
	 *   <li>Membangun toolbar filter (hanya untuk entitas baru): field cari teks,
	 *       filter satuan kerja, tombol cari, dan checkbox filter "Gaji".</li>
	 *   <li>Membuat grid dengan kolom: Nama/Checkbox, Bank, Satuan Kerja, Nilai, SOP.</li>
	 *   <li>Menambahkan checkbox "pilih semua" di header kolom Nama yang saat dicentang
	 *       memuat semua SI aktif ke dalam {@code longs} sekaligus.</li>
	 *   <li>Memanggil {@code reload()} untuk mengisi baris data SI sesuai filter.</li>
	 *   <li>Menambahkan footer dengan total nilai yang diperbarui via
	 *       {@code eventListenerHitung}.</li>
	 * </ol></p>
	 *
	 * @param east        komponen kontainer (East panel atau Groupbox) tempat detail dirender
	 * @param prosesTransferStandingInstruction entitas aktif; jika id null maka tampil
	 *                    mode pemilihan
	 * @param disposisiSop disposisi SOP aktif atau null
	 * @throws Exception jika terjadi kesalahan saat membangun komponen atau query DB
	 *
	 * <p><b>Pemeliharaan:</b> Checkbox "pilih semua" melakukan query langsung ke DB tanpa
	 * filter satuan kerja. Jika ada kebutuhan filter tambahan untuk "pilih semua",
	 * tambahkan Restrictions di query di dalam listener checkbox tersebut.</p>
	 */
	@SuppressWarnings("unchecked")
	private void initDetail(Component east, final ProsesTransferStandingInstruction prosesTransferStandingInstruction,
			final DisposisiSop disposisiSop) throws Exception {
		Common.clear(east);
		if (east instanceof East) {
			((East) east).setWidth("70%");
		}
		longs = new HashMap<Long, Double>();
		bankTerpilihSesiIni = new HashMap<Long, Set<String>>();

		final MyCheckboxConfig gaji = new MyCheckboxConfig("Gaji");

		final AmbilDataSatuanKerjaBanbox satker = new AmbilDataSatuanKerjaBanbox();
		final Textbox cari = new Textbox();
		cari.setCols(10);
		final Rows rows = new Rows();

		final EventListener eventListener = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				reload(rows, disposisiSop, cari.getValue().trim(), (SatuanKerja) satker.getAttribute("satuanKerja"),
						gaji.isChecked(), this);
			}
		};

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(east);

		if (prosesTransferStandingInstruction.getId() == null) {
			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("40px");
			toolbar.setParent(north);

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

			gaji.setChecked(true);

			gaji.addEventListener("onClick", eventListener);

			gaji.setParent(toolbar);

		}

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setMold("paging");
		grid.setPageSize(10);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Nama daftar transfer");
		column.appendChild(checkboxConfig);

		checkboxConfig.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				longs.clear();

				if (checkboxConfig.isChecked()) {
					List<Object[]> longss = HibernateUtil.currentSession().createCriteria(StandingInstruction.class)
							.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
									Restrictions.eq("disposisiSop.aktif", true)))
							.setProjection(Projections.projectionList().add(Projections.property("id"))
									.add(Projections.property("nominal")).add(Projections.property("aktif")))
							.add(Restrictions.isNull("prosesTransferStandingInstruction")).list();

					for (Object[] a : longss) {
						longs.put(Long.parseLong(a[0].toString()), Double.parseDouble(a[1].toString()));
					}
				}

				reload(rows, disposisiSop, cari.getValue().trim(), (SatuanKerja) satker.getAttribute("satuanKerja"),
						gaji.isChecked(), eventListener);

				eventListenerHitung.onEvent(null);
			}
		});

		column = new MyColumnConfig("Bank");
		column.setParent(columns);

		column = new MyColumnConfig("Satuan Kerja");
		column.setWidth("20%");
		column.setParent(columns);

		column = new MyColumnConfig("Nilai");
		column.setAlign("right");
		column.setWidth("20%");
		column.setParent(columns);

		column = new MyColumnConfig("SOP");
		column.setWidth("15%");
		column.setParent(columns);

		rows.setParent(grid);

		if (prosesTransferStandingInstruction.getId() == null) {
			reload(rows, disposisiSop, cari.getValue().trim(), (SatuanKerja) satker.getAttribute("satuanKerja"),
					gaji.isChecked(), eventListener);
		} else {
			reload(rows, disposisiSop, "", null, true, eventListener);
		}

		Foot foot = new Foot();

		foot.setParent(grid);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footerNilai = new Footer("");
		foot.appendChild(footerNilai);

		footer = new Footer("");
		foot.appendChild(footer);

		eventListenerHitung.onEvent(null);
	}

	/**
	 * Listener yang menghitung dan memperbarui total nilai transfer di footer grid detail
	 * setiap kali komposisi pilihan SI berubah.
	 *
	 * <p><b>Tujuan:</b> Memberikan umpan balik real-time kepada pengguna mengenai total
	 * nilai yang akan ditransfer berdasarkan SI yang dipilih, tanpa perlu menekan tombol
	 * hitung manual.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengiterasi semua nilai dalam map {@code longs} dan
	 * menjumlahkannya, lalu memformat hasilnya menggunakan {@code Common.numberFormat}
	 * dan menyetelnya ke label {@code footerNilai}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Listener ini tidak menerima parameter event yang bermakna
	 * (arg0 tidak digunakan). Dipanggil dari: checkbox per-baris, checkbox pilih-semua,
	 * dan setelah {@code reload()}.</p>
	 */
	private EventListener eventListenerHitung = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Double n = 0.0;

			for (Double nn : longs.values()) {
				n += nn;
			}
			footerNilai.setLabel(Common.numberFormat.get().format(n));
		}

	};
	private DisposisiSop disposisiSop = null;
	private MyDatebox tanggalPembuatan;
	private MyCheckboxConfig setujuiOleh;
	private MyDatebox tanggalRealisasikan;

	private List<StandingInstruction> standingInstructionsData = new ArrayList<StandingInstruction>();

	/**
	 * Memuat ulang baris-baris daftar standing instruction di panel detail berdasarkan
	 * filter yang diberikan, dan merender setiap baris sebagai checkbox (mode pilih)
	 * atau label (mode lihat).
	 *
	 * <p><b>Tujuan:</b> Method inti panel detail yang mengambil data {@link StandingInstruction}
	 * dari DB sesuai filter, lalu merender setiap SI beserta informasi bank, satuan kerja,
	 * nilai, dan status SOP. Untuk entitas proses yang sedang dibuat (id null), SI
	 * ditampilkan sebagai checkbox agar pengguna bisa memilih. Untuk entitas yang sudah ada,
	 * hanya SI yang sudah terhubung ke proses ini yang ditampilkan dalam mode read-only.</p>
	 *
	 * <p><b>Cara kerja langkah demi langkah:</b>
	 * <ol>
	 *   <li>Membersihkan baris yang ada di {@code rows}.</li>
	 *   <li>Membangun criteria Hibernate untuk {@link StandingInstruction} dengan filter:
	 *       aktif = true, disposisiSop aktif, filter gaji/non-gaji, filter teks cari
	 *       (nama/kode/keterangan), dan jika proses sudah ada: filter via
	 *       {@code ILIKE prosesStanding ANYWHERE ",id,"}.</li>
	 *   <li>Untuk setiap SI, mengambil data {@link PembayaranGajiPunyaPegawai} yang terkait
	 *       untuk membangun peta nilai per bank ({@code mapsBank}).</li>
	 *   <li>Untuk setiap bank dalam peta, mengecek apakah entri {@code transferVia} JSON
	 *       sudah memiliki referensi ke proses ini atau belum.</li>
	 *   <li>Menerapkan filter satuan kerja jika diberikan.</li>
	 *   <li>Membuat baris: jika proses sudah ada, label nama; jika belum, checkbox yang
	 *       saat dicentang menambahkan ke {@code longs} dan memperbarui JSON
	 *       {@code transferVia} di SI, lalu memicu {@code eventListenerHitung}.</li>
	 *   <li>Menampilkan bank, satuan kerja, nilai, dan SOP di sel berikutnya.</li>
	 * </ol></p>
	 *
	 * @param rows          komponen {@link Rows} ZK tempat baris akan ditambahkan
	 * @param disposisiSop  disposisi SOP aktif atau null
	 * @param cari          teks pencarian untuk filter nama/kode/keterangan SI
	 * @param satker        filter satuan kerja; null berarti tampilkan semua
	 * @param gaji          {@code true} untuk menampilkan SI yang terkait pembayaran gaji;
	 *                      {@code false} untuk yang tidak terkait gaji
	 * @param eventListener listener yang dipanggil ulang saat terjadi perubahan pilihan
	 * @throws Exception jika terjadi kesalahan query DB atau parsing JSON
	 *
	 * <p><b>Penanganan error:</b> Parsing JSON {@code transferVia} di dalam try-catch
	 * agar jika satu SI memiliki data rusak, SI lain tetap dapat ditampilkan. Error
	 * diabaikan secara diam-diam di blok catch kosong — pertimbangkan logging untuk
	 * debugging.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Method ini menggunakan JOIN antara {@link StandingInstruction},
	 * {@link PembayaranGajiPunyaPegawai}, dan logika JSON yang cukup kompleks. Jika
	 * model data berubah (misalnya format {@code transferVia}), perbarui logika parsing
	 * dan update JSON di sini secara konsisten dengan {@code onSave()}.</p>
	 */
	@SuppressWarnings("unchecked")
	private void reload(final Rows rows, DisposisiSop disposisiSop, String cari, SatuanKerja satker, boolean gaji,
			final EventListener eventListener

	) throws Exception {
		Common.clear(rows);

		Criterion criterion = gaji ? Restrictions.isNotNull("pembayaranGaji") : Restrictions.isNull("pembayaranGaji");

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(StandingInstruction.class)
				.createAlias("disposisiSop", "disposisiSop", Criteria.LEFT_JOIN)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
						.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));

		standingInstructionsData =

				criteria.add(prosesTransferStandingInstruction.getId() != null ? Restrictions.sqlRestriction("true")
						: criterion)

						.add((cari.isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.ilike("keterangan", cari, MatchMode.ANYWHERE),
										Restrictions.or(Restrictions.ilike("nama", cari, MatchMode.ANYWHERE),
												Restrictions.ilike("kode", cari, MatchMode.ANYWHERE)))

						))

						.add(prosesTransferStandingInstruction.getId() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("prosesStanding",
										"," + prosesTransferStandingInstruction.getId() + ",", MatchMode.ANYWHERE))

						.addOrder(Order.desc("id")).list();

		for (final StandingInstruction standingInstruction : standingInstructionsData) {

			if (standingInstruction.getProsesStanding().trim().isEmpty()
					|| prosesTransferStandingInstruction.getId() == null
					|| (prosesTransferStandingInstruction.getId() != null && standingInstruction.getProsesStanding()
							.contains("," + prosesTransferStandingInstruction.getId() + ","))) {

				JSONObject jsonObjectTransfer = new JSONObject(standingInstruction.getTransferVia());

				Criteria criteriaD = session.createCriteria(PembayaranGajiPunyaPegawai.class)

						.add(Restrictions.eq("pembayaranGaji", standingInstruction.getPembayaranGaji()));

				List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = criteriaD.list();

				Map<Long, Double> mapsBank = new HashMap<Long, Double>();
				for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {
					Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
					Bank bank = pegawai.ambilBank(pembayaranGajiPunyaPegawai.getFormatItemGaji());
					Long idBank = bank == null || bank.getId() == null ? -1L : bank.getId();
					Double nilai = mapsBank.get(idBank);
					if (nilai == null) {
						nilai = 0.0;
					}
					nilai += pembayaranGajiPunyaPegawai.getNilai();
					mapsBank.put(idBank, nilai);
				}

				for (final Long idBank : mapsBank.keySet()) {

					JSONObject jsonObjectData = null;

					try {
						jsonObjectData = jsonObjectTransfer.isNull(idBank.toString()) ? null
								: jsonObjectTransfer.getJSONObject(idBank.toString());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/ProsesTransferStandingInstructionAction.java:1138");
						// TODO: handle exception
					}

					Long idS = jsonObjectData == null || jsonObjectData.isNull("si")
							|| jsonObjectData.get("si").toString().trim().isEmpty() ? null
									: Long.parseLong(jsonObjectData.get("si").toString().trim());

					ProsesTransferStandingInstruction sdIns = (ProsesTransferStandingInstruction) (idS == null ? null
							: session.createCriteria(ProsesTransferStandingInstruction.class)
									.add(Restrictions.idEq(idS)).uniqueResult());

					if ((sdIns != null && prosesTransferStandingInstruction.getId() != null
							&& sdIns.getId().equals(prosesTransferStandingInstruction.getId()))
							|| (prosesTransferStandingInstruction.getId() == null && sdIns == null)) {

						Bank bank = (Bank) ConstantValues.ambil(Bank.class.getName(), idBank);
						Double nilai = mapsBank.get(idBank);

						SatuanKerja satuanKerja = standingInstruction.ambilSatuanKerja();
						if (prosesTransferStandingInstruction.getId() != null || satker == null || (satuanKerja != null
								&& satker != null && satker.getId().equals(satuanKerja.getId()))) {

							final Long iddata = standingInstruction.getId();
							final Double n = nilai;

							if (prosesTransferStandingInstruction.getId() != null) {
								longs.put(iddata, n);
							}

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							if (prosesTransferStandingInstruction.getId() != null) {
								row.appendChild(new Label(standingInstruction.getNama()));
							} else {

								final Checkbox c;
								row.appendChild(c = new Checkbox(standingInstruction.getNama()));
								c.setDisabled(bank == null);
								c.setChecked(longs.keySet().contains(standingInstruction.getId())
										&& !jsonObjectTransfer.isNull(idBank.toString()));

								c.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										if (c.isChecked()) {
											longs.put(iddata, n);
										} else {
											longs.remove(iddata);
										}

										Set<String> bankTerpilih = bankTerpilihSesiIni.get(iddata);
										if (bankTerpilih == null) {
											bankTerpilih = new HashSet<String>();
											bankTerpilihSesiIni.put(iddata, bankTerpilih);
										}

										JSONObject jsonObjectTransfer = new JSONObject(
												standingInstruction.getTransferVia());

										if (c.isChecked()) {
											JSONObject jsonObjectData = new JSONObject();
											jsonObjectData.put("nilai", n);
											jsonObjectData.put("si",
													prosesTransferStandingInstruction.getId() == null ? ""
															: prosesTransferStandingInstruction.getId().toString());
											jsonObjectTransfer.put(idBank.toString(), jsonObjectData);
											bankTerpilih.add(idBank.toString());
										} else {
											jsonObjectTransfer.remove(idBank.toString());
											bankTerpilih.remove(idBank.toString());
										}

										standingInstruction.setTransferVia(jsonObjectTransfer.toString());
										Common.refreshUpdate(standingInstruction);

										eventListenerHitung.onEvent(null);
									}
								});
							}
							row.appendChild(new MyLabelKecil(bank == null ? "" : bank.getNama()));

							row.appendChild(new MyLabelKecil(satuanKerja == null ? "" : satuanKerja.getNama()));

							row.appendChild(new MyLabelKecil(Common.numberFormat.get().format(nilai)));

							if (standingInstruction.getDisposisiSop() != null) {
								A aa;
								(aa = new A()).setParent(row);
								aa.setStyle("font-size:9px;");
								UIClassHelper.applyReadMore(aa,
										"SOP " + standingInstruction.getDisposisiSop().getKeterangan() + " ("
												+ standingInstruction.getDisposisiSop().getSop().getNama() + ")");
								aa.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										TampilanAlurSopAction.prosess(standingInstruction.getDisposisiSop().getId(),
												null, null, true, arg0.getTarget());
									}
								});

							} else {
								new MyLabelKecil().setParent(row);
							}

						}
					}
				}
			}
		}
	}

	/**
	 * Memvalidasi input form dan menyimpan entitas {@link ProsesTransferStandingInstruction}
	 * ke database, lalu memperbarui referensi di setiap {@link StandingInstruction} yang
	 * terpilih dan memicu cetak laporan secara async.
	 *
	 * <p><b>Tujuan:</b> Method inti penyimpanan yang menangani validasi, persistensi
	 * entitas proses baru, serta pembaruan JSON {@code transferVia} di setiap SI
	 * terpilih untuk mencatat bahwa SI sudah masuk dalam batch proses ini.</p>
	 *
	 * <p><b>Cara kerja langkah demi langkah:</b>
	 * <ol>
	 *   <li><b>Validasi nama:</b> Jika kosong, tampilkan peringatan dan return false.</li>
	 *   <li><b>Validasi pilihan SI:</b> Jika {@code longs} kosong (tidak ada SI dipilih),
	 *       tampilkan peringatan dan return false.</li>
	 *   <li><b>Muat ulang dari DB:</b> Jika entitas sudah ada (id tidak null), muat ulang
	 *       untuk menghindari stale state.</li>
	 *   <li><b>Set metadata:</b> Nama, keterangan, tanggal pembuatan, tanggal realisasi,
	 *       disposisi SOP, kode (untuk entitas baru via {@code generateCode(true)}),
	 *       dan status persetujuan berdasarkan checkbox {@code setujuiOleh}.</li>
	 *   <li><b>Simpan ke DB:</b> via {@code Common.refreshSaveOrUpdate()} dan flush.</li>
	 *   <li><b>Update SI (hanya untuk entitas baru):</b> Mengambil semua SI yang id-nya ada
	 *       di {@code longs}, lalu untuk setiap SI, mengiterasi JSON {@code transferVia}
	 *       untuk menemukan entri yang belum memiliki referensi "si" <b>dan yang kunci
	 *       banknya tercatat di {@link #bankTerpilihSesiIni} untuk sesi form ini</b>, lalu
	 *       mengisinya dengan ID proses baru. Pembatasan ini disengaja: mencegah entri
	 *       ber-{@code si} kosong yang tertinggal dari sesi form sebelumnya (dicentang lalu
	 *       ditutup lewat tombol Batal tanpa disimpan) ikut terstempel dan menggelembungkan
	 *       total batch ini. Juga memperbarui field {@code prosesStanding} (CSV ID) dan
	 *       menghitung total nilai keseluruhan.</li>
	 *   <li><b>Update nilai total:</b> Menyimpan total nilai ke entitas proses.</li>
	 *   <li><b>Async post-save:</b> Jika ada eventListener callback (dari {@code onAddExternal}),
	 *       memicunya via timer. Memicu cetak laporan dengan delay 2,5 detik.</li>
	 * </ol></p>
	 *
	 * @param event event ZK pemicu (tidak digunakan langsung)
	 * @return {@code true} jika berhasil disimpan; {@code false} jika validasi gagal
	 * @throws Exception jika terjadi kesalahan DB yang tidak terduga
	 *
	 * <p><b>Penanganan error:</b> Validasi ditangani dengan MessageBox dan return false.
	 * Error DB tidak ditangkap secara eksplisit dan akan naik ke framework ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Blok update SI (setelah {@code if (tambah)}) hanya
	 * berjalan untuk entitas baru. Jika ada kebutuhan untuk menambah/mengurangi SI
	 * dari proses yang sudah ada, logika ini perlu diperluas dengan mekanisme diff.</p>
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Proses Transfer Standing Instruction belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Proses dengan nama yang sesuai; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (longs.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Daftar Transfer Standing Instruction belum dipilih. Langkah yang dapat dilakukan: (1) Centang minimal satu transaksi transfer dari daftar yang tersedia; (2) Pastikan transaksi standing instruction sudah ada dan siap diproses; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (prosesTransferStandingInstruction.getId() != null) {
			prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) session
					.load(ProsesTransferStandingInstruction.class, prosesTransferStandingInstruction.getId());

		}

		prosesTransferStandingInstruction.setNama(nama.getValue());
		prosesTransferStandingInstruction.setKeterangan(keterangan.getValue());

		prosesTransferStandingInstruction.setTanggalPembuatan(tanggalPembuatan.getValue());

		prosesTransferStandingInstruction.setTanggalRealisasikan(tanggalRealisasikan.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			prosesTransferStandingInstruction.setDisposisiSop(disposisiSop);
		}

		boolean tambah = false;
		if (prosesTransferStandingInstruction.getId() == null) {
			String noAgenda = generateCode(true);
			prosesTransferStandingInstruction.setKode(noAgenda);
			tambah = true;
		}

		if (setujuiOleh.isChecked()) {
			prosesTransferStandingInstruction.setDisetujuiOleh(Common.getCurrentUser());
			prosesTransferStandingInstruction.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		} else {
			prosesTransferStandingInstruction.setDisetujuiOleh(null);
			prosesTransferStandingInstruction.setTanggalPersetujuan(null);
		}

		Common.refreshSaveOrUpdate(session, prosesTransferStandingInstruction);
		session.flush();

		if (tambah) {
			List<StandingInstruction> standingInstructions = HibernateUtil.currentSession()
					.createCriteria(StandingInstruction.class)
					.add(longs.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", longs.keySet()))
					.list();

			Double nilai = 0.0;
			for (StandingInstruction standingInstruction : standingInstructions) {

				Set<String> bankTerpilih = bankTerpilihSesiIni.get(standingInstruction.getId());

				JSONObject jsonObjectTransfer = new JSONObject(standingInstruction.getTransferVia());
				Iterator<String> iterator = jsonObjectTransfer.keys();

				while (iterator.hasNext()) {
					String d = iterator.next();
					if (bankTerpilih == null || !bankTerpilih.contains(d)) {
						// Entri ber-"si" kosong yang bukan berasal dari centangan sesi form ini
						// (mis. tertinggal dari sesi sebelumnya yang dibatalkan tanpa disimpan) --
						// jangan ikut diklaim/dijumlahkan ke batch ini. Lihat javadoc
						// #bankTerpilihSesiIni.
						continue;
					}
					JSONObject jsonObjectData = jsonObjectTransfer.getJSONObject(d);
					if (jsonObjectData.isNull("si") || jsonObjectData.get("si").toString().isEmpty()) {
						jsonObjectData.put("si", prosesTransferStandingInstruction.getId().toString());
						Double n = jsonObjectData.getDouble("nilai");
						nilai += n;
					}

				}
				standingInstruction.setProsesStanding(
						standingInstruction.getProsesStanding() + "," + prosesTransferStandingInstruction.getId());
				standingInstruction.setTransferVia(jsonObjectTransfer.toString());
				Common.refreshUpdate(session, standingInstruction);

			}

			prosesTransferStandingInstruction.setNilai(nilai);
			Common.refreshUpdate(session, prosesTransferStandingInstruction);
			session.flush();
		}

		if (eventListener != null) {
			Common.createDefaultTimer(eventListener);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				cetak(prosesTransferStandingInstruction);
			}
		}, "Proses cetak", false, 2500);

		return true;
	}

	/**
	 * Membangun {@link Criteria} Hibernate untuk query {@link ProsesTransferStandingInstruction}
	 * berdasarkan filter tanggal, status aktif, dan teks pencarian.
	 *
	 * <p><b>Tujuan:</b> Menyediakan satu titik query yang dipakai bersama oleh
	 * {@code onSearchDefault()} (untuk paging dan grid) dan oleh infrastruktur ekspor.</p>
	 *
	 * <p><b>Cara kerja:</b> Membangun criteria dengan filter:
	 * <ul>
	 *   <li>Tanggal pembuatan antara start dan end (SQL date comparison).</li>
	 *   <li>Status aktif jika checkbox aktif dicentang.</li>
	 *   <li>Teks nama/kode menggunakan ILIKE ANYWHERE jika diisi.</li>
	 * </ul>
	 * Jika {@code order} true, menambahkan ORDER BY id DESC.</p>
	 *
	 * @param order {@code true} untuk menambahkan ordering; {@code false} untuk paging count
	 * @return {@link Criteria} siap eksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ProsesTransferStandingInstruction.class)

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
	 * Memuat ulang data grid proses transfer SI berdasarkan filter aktif, dengan paginasi.
	 *
	 * <p><b>Tujuan:</b> Entry point utama untuk refresh tampilan. Dipanggil saat inisialisasi,
	 * setelah simpan, setelah perubahan filter, dan setelah perubahan halaman paging.</p>
	 *
	 * <p><b>Cara kerja:</b> Guard jika {@code searchnama} null (UI belum siap) agar tidak NPE.
	 * Menyetel ulang {@code persetujuan = false} (reset mode). Menghitung total record untuk
	 * paging via {@code initCriteria(false)}, lalu mengambil halaman aktif via
	 * {@code initCriteria(true)} dengan setMaxResults dan setFirstResult, dan mengisi grid.</p>
	 *
	 * @param event event ZK pemicu; boleh null jika dipanggil programatis
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		persetujuan = false;
		Common.initPaging(initCriteria(false), paging);

		List<ProsesTransferStandingInstruction> prosesTransferStandingInstruction = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(prosesTransferStandingInstruction);
		grid.setRowRenderer(new ProsesTransferStandingInstructionRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun dan mengembalikan grid formulir proses transfer standing instruction
	 * yang dapat dilekatkan ke berbagai kontainer (window internal maupun form SOP).
	 *
	 * <p><b>Tujuan:</b> Implementasi {@code FormSop.form()} yang menghasilkan widget
	 * formulir berisi field-field entitas {@link ProsesTransferStandingInstruction}:
	 * kode, tanggal pengajuan, judul, status persetujuan, keterangan, realisasi, dan
	 * daftar SI dalam groupbox (jika dalam konteks SOP).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menghasilkan kode baru jika belum ada.</li>
	 *   <li>Membuat listener auto-save ({@code eventListenerSImpan}) yang menyimpan
	 *       perubahan ke DB secara langsung saat field diubah (hanya untuk entitas
	 *       yang sudah ada).</li>
	 *   <li>Merender baris-baris form: Kode (read-only label), Tanggal Pengajuan
	 *       (datebox atau label tergantung status persetujuan), Judul Transfer (textbox
	 *       atau label), Status Persetujuan (checkbox {@code setujuiOleh}, hanya visible
	 *       dalam mode persetujuan tertentu), Keterangan, info realisasi, dan tanggal
	 *       realisasi.</li>
	 *   <li>Mendaftarkan atribut {@code eventListenerSetuju} pada grid untuk integrasi SOP.</li>
	 *   <li>Jika dalam konteks disposisiSop, menambahkan groupbox berisi panel detail SI
	 *       via {@code initDetail()}.</li>
	 * </ol></p>
	 *
	 * @param generalValueObject objek data; di-cast ke {@link ProsesTransferStandingInstruction}
	 * @param disposisiSop       disposisi SOP aktif atau null
	 * @param save               tombol simpan yang mungkin perlu dikonfigurasi
	 * @param setujui            listener untuk checkbox persetujuan; null jika tidak diperlukan
	 * @return {@link MyGrid} berisi seluruh komponen form
	 * @throws Exception jika terjadi kesalahan saat membangun komponen
	 *
	 * <p><b>Pemeliharaan:</b> Field {@code tanggalRealisasikan} hanya tampil dalam kondisi
	 * tertentu (sudah disetujui belum direalisasikan). Jika logika status berubah,
	 * perbarui kondisi {@code if-else} untuk visibility field ini.</p>
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujui) throws Exception {

		this.prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) generalValueObject;
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

		if (prosesTransferStandingInstruction.getKode() == null
				|| prosesTransferStandingInstruction.getKode().trim().isEmpty()) {
			String noAgenda = generateCode(true);
			prosesTransferStandingInstruction.setKode(noAgenda);
		}

		EventListener eventListenerSImpan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (prosesTransferStandingInstruction.getId() != null) {
					prosesTransferStandingInstruction.setNama(nama.getValue());
					prosesTransferStandingInstruction.setKeterangan(keterangan.getValue());

					prosesTransferStandingInstruction.setTanggalPembuatan(tanggalPembuatan.getValue());

					prosesTransferStandingInstruction.setTanggalRealisasikan(tanggalRealisasikan.getValue());

					Common.refreshUpdate(prosesTransferStandingInstruction);
				}

			}
		};

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode *"));
		row.appendChild(new Label(prosesTransferStandingInstruction.getKode()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan *"));
		tanggalPembuatan = new MyDatebox(prosesTransferStandingInstruction.getTanggalPembuatan());

		if (prosesTransferStandingInstruction.getDisetujuiOleh() != null) {
			row.appendChild(
					new Label(Common.dateFormat.get().format(prosesTransferStandingInstruction.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setReadonly(true);

		tanggalPembuatan.addEventListener("onChange", eventListenerSImpan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Transfer *"));
		nama = new Textbox(prosesTransferStandingInstruction.getNama());
		if (prosesTransferStandingInstruction.getDisetujuiOleh() != null) {
			row.appendChild(new Label(prosesTransferStandingInstruction.getNama()));
		} else {
			row.appendChild(nama);
		}
		nama.setWidth("90%");
		nama.setRows(2);

		nama.addEventListener("onChange", eventListenerSImpan);

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(setujuiOleh = new MyCheckboxConfig("Setujui"));
		setujuiOleh.setChecked(prosesTransferStandingInstruction.getDisetujuiOleh() != null);

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
					setujui.onEvent(new Event("", null, prosesTransferStandingInstruction.getDisetujuiOleh() != null));
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));

		keterangan = new Textbox(prosesTransferStandingInstruction.getKeterangan());
		if (prosesTransferStandingInstruction.getDisetujuiOleh() != null || viewOnly) {
			row.appendChild(new Label(prosesTransferStandingInstruction.getKeterangan()));
		} else {
			row.appendChild(keterangan);

			if (prosesTransferStandingInstruction.getId() != null) {
				EventListener eventListenerEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						prosesTransferStandingInstruction.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(prosesTransferStandingInstruction);

					}
				};
				keterangan.addEventListener("onChange", eventListenerEventListener);
			}

		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		keterangan.addEventListener("onChange", eventListenerSImpan);

		if (prosesTransferStandingInstruction.getRealisasikanOleh() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telah direalisasikan oleh"));
			row.appendChild(new Label(prosesTransferStandingInstruction.getRealisasikanOleh().getUserNama()));
		}
		tanggalRealisasikan = new MyDatebox(prosesTransferStandingInstruction.getTanggalRealisasikan());
		if (prosesTransferStandingInstruction.getRealisasikanOleh() == null
				&& prosesTransferStandingInstruction.getDisetujuiOleh() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Realisasi *"));

			row.appendChild(tanggalRealisasikan);

			tanggalRealisasikan.setFormat(Common.dateFormat.get().toPattern());
			tanggalRealisasikan.setReadonly(true);

		} else if (prosesTransferStandingInstruction.getTanggalRealisasikan() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Telah direalisasikan tanggal"));
			row.appendChild(new Label(
					Common.dateFormat61.get().format(prosesTransferStandingInstruction.getTanggalRealisasikan())));
		}

		if (disposisiSop != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.setParent(row);
			groupboxStyled.setHeight("400px");
			groupboxStyled.appendChild(new MyCaptionStyled("Daftar Transfer"));

			initDetail(groupboxStyled, prosesTransferStandingInstruction, disposisiSop);
		}

		return grid;
	}

	/**
	 * Mengembalikan istilah domain dalam bahasa Indonesia untuk modul ini.
	 *
	 * @return {@code "Proses Pengajuan Standing Instruction"}
	 * @throws Exception tidak akan terjadi; deklarasi ada karena kontrak antarmuka
	 */
	@Override
	public String istilah() throws Exception {
		return "Proses Pengajuan Standing Instruction";
	}

	/**
	 * Mengembalikan entitas {@link ProsesTransferStandingInstruction} aktif sebagai
	 * {@link DataSop} untuk keperluan sistem alur SOP.
	 *
	 * @return entitas yang sedang aktif di form
	 * @throws Exception tidak akan terjadi; deklarasi ada karena kontrak antarmuka
	 */
	@Override
	public DataSop ambil() throws Exception {
		return prosesTransferStandingInstruction;
	}

	/**
	 * Mengembalikan kelas entitas domain yang dikelola kelas ini untuk keperluan
	 * refleksi generik di infrastruktur SOP dan ekspor.
	 *
	 * @return {@code ProsesTransferStandingInstruction.class}
	 * @throws Exception tidak akan terjadi; deklarasi ada karena kontrak antarmuka
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return ProsesTransferStandingInstruction.class;
	}

	/**
	 * Mengubah mode persetujuan action ini secara programatis dari infrastruktur SOP.
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Menghasilkan kode unik untuk entitas {@link ProsesTransferStandingInstruction}
	 * berdasarkan konfigurasi nomor surat SI yang aktif.
	 *
	 * <p><b>Tujuan:</b> Membuat nomor dokumen proses transfer SI yang mengikuti
	 * format dan aturan reset yang dikonfigurasi di {@link NomorSuratAlurKeuangan#SI}.
	 * Fallback ke barcode acak jika konfigurasi tidak ada.</p>
	 *
	 * <p><b>Cara kerja:</b> Sama dengan {@code PenggantianKasKecilAction.generateCode()} —
	 * menggunakan {@code gunakanIndexUrut} atau {@code getindex()} untuk menentukan
	 * indeks, memformat kode, menaikkan counter jika {@code tambah = true}, dan
	 * memastikan keunikan via {@code KodeUnikUtil}.</p>
	 *
	 * @param tambah {@code true} untuk menaikkan counter nomor surat
	 * @return string kode unik yang sudah diformat
	 *
	 * <p><b>Pemeliharaan:</b> Konfigurasi nomor surat ada di
	 * {@code NomorSuratAlurKeuangan.SI}. Pastikan entri ini diisi admin sebelum
	 * modul digunakan agar nomor surat memiliki format yang sesuai institusi.</p>
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurKeuangan.SI == null || NomorSuratAlurKeuangan.SI.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurKeuangan.SI.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurKeuangan.SI.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurKeuangan.SI.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurKeuangan.SI.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurKeuangan.SI.getNomorSurat().format(index, WaktuUtil.getDate());
		return ais.action.master.KodeUnikUtil.pastikanUnik(ProsesTransferStandingInstruction.class, noAgenda);
	}

	/**
	 * Menghitung indeks urutan berikutnya untuk nomor surat proses transfer SI dari
	 * jumlah record yang ada di database, dengan memperhatikan aturan reset dan kelompok.
	 *
	 * <p><b>Tujuan:</b> Alternatif dinamis dari counter tersimpan untuk menentukan nomor
	 * urut berikutnya. Menggunakan rowCount query Hibernate dengan filter yang disesuaikan
	 * dengan konfigurasi {@link NomorSurat} (reset per tahun/bulan/tanggal, kelompok).</p>
	 *
	 * <p><b>Cara kerja:</b> Sama dengan {@code PenggantianKasKecilAction.getindex()} —
	 * membangun query rowCount dengan filter kondisional berdasarkan properti
	 * {@link NomorSurat}, mengembalikan rowCount + 1 sebagai indeks berikutnya.</p>
	 *
	 * @param nomorSurat konfigurasi nomor surat; jika null return 0
	 * @return indeks urutan berikutnya, minimal 1
	 *
	 * <p><b>Pemeliharaan:</b> Perhatikan bahwa field waktu reset di sini menggunakan
	 * {@code "waktu"} (bukan {@code "tanggalPembuatan"} seperti di PenggantianKasKecil).
	 * Pastikan nama field sesuai dengan kolom di entitas {@link ProsesTransferStandingInstruction}.</p>
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}

		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(ProsesTransferStandingInstruction.class)
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
	 * Membangun peta parameter laporan untuk template JasperReports proses transfer SI,
	 * termasuk data utama entitas, daftar item SI per bank, dan informasi status.
	 *
	 * <p><b>Tujuan:</b> Menyediakan semua data yang dibutuhkan oleh template JasperReports
	 * {@code "akunting/pengajuan_si"} dalam format {@code Map} key-value. Method ini
	 * merupakan titik persiapan data tunggal yang digunakan oleh {@code cetakData()}
	 * dan {@code cetak()} agar logika pengumpulan data tidak duplikat.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Refresh entitas dari DB jika sudah ada untuk memastikan data terkini.</li>
	 *   <li>Membuat peta parameter dasar dengan ID entitas dan semua properti via
	 *       {@code Common.insertProperty()}.</li>
	 *   <li>Menambahkan parameter SOP via {@code DisposisiAlurSop.parameterMap()}.</li>
	 *   <li>Mengambil semua SI yang terkait (via {@code prosesStanding ILIKE}).</li>
	 *   <li>Untuk setiap SI, mengiterasi JSON {@code transferVia} dan untuk setiap entri
	 *       bank yang referensi "si"-nya cocok dengan ID proses ini, membuat peta item
	 *       berisi data SI, bank, nilai, dan status persetujuan.</li>
	 *   <li>Menambahkan daftar peta item ke parameter dengan kunci "maps".</li>
	 *   <li>Menghapus kunci yang mengandung "disposisiSop" dari parameter untuk menghindari
	 *       konflik serialisasi di JasperReports.</li>
	 * </ol></p>
	 *
	 * @param prosesTransferStandingInstruction entitas yang akan dibuatkan laporannya
	 * @return {@link Map} berisi semua parameter siap dipakai template JasperReports
	 * @throws Exception jika terjadi kesalahan DB atau parsing JSON
	 *
	 * <p><b>Pemeliharaan:</b> Jika template JasperReports membutuhkan field baru,
	 * tambahkan ke peta di sini. Perhatikan bahwa kunci "maps" berisi list of map
	 * yang merepresentasikan tabel detail di laporan.</p>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(ProsesTransferStandingInstruction prosesTransferStandingInstruction) throws Exception {
		if (prosesTransferStandingInstruction != null && prosesTransferStandingInstruction.getId() != null) {
			HibernateUtil.currentSession().refresh(prosesTransferStandingInstruction);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", prosesTransferStandingInstruction.getId());

		Common.insertProperty(ProsesTransferStandingInstruction.class, prosesTransferStandingInstruction, parameters,
				"data");

		DisposisiAlurSop.parameterMap(prosesTransferStandingInstruction.getDisposisiSop(), parameters);

		Session session = HibernateUtil.currentSession();
		List<StandingInstruction> standingInstructions = session.createCriteria(StandingInstruction.class)
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).add(Restrictions.ilike("prosesStanding",
						"," + prosesTransferStandingInstruction.getId() + ",", MatchMode.ANYWHERE))
				.list();
		List<Map> maps = new ArrayList<Map>();
		Double totalSemua = 0.0;
		for (StandingInstruction standingInstruction : standingInstructions) {

			JSONObject jsonObjectTransfer = new JSONObject(standingInstruction.getTransferVia());
			Iterator<String> iterator = jsonObjectTransfer.keys();

			while (iterator.hasNext()) {

				String d = iterator.next();

				JSONObject jsonObjectData = jsonObjectTransfer.getJSONObject(d);

				Long idS = jsonObjectData == null || jsonObjectData.isNull("si")
						|| jsonObjectData.get("si").toString().trim().isEmpty() ? null
								: Long.parseLong(jsonObjectData.get("si").toString().trim());

				if (idS != null && prosesTransferStandingInstruction.getId().equals(idS)) {

					Long idBank = Long.parseLong(d);
					Bank bank = (Bank) ConstantValues.ambil(Bank.class.getName(), idBank);

					Map map = new HashMap();
					Common.insertProperty(StandingInstruction.class, standingInstruction, map, "data", 2,
							"prosesTransferStandingInstruction");

					Double nilai = jsonObjectData.getDouble("nilai");
					totalSemua += nilai;

					String status = "";
					if (prosesTransferStandingInstruction.getDisetujuiOleh() == null) {
						status = "Belum disetujui";
					} else {
						status = "Disetujui oleh " + prosesTransferStandingInstruction.getDisetujuiOleh().getUserNama()
								+ " pada "
								+ (prosesTransferStandingInstruction.getTanggalPersetujuan() == null ? ""
										: Common.dateFormat51.get()
												.format(prosesTransferStandingInstruction.getTanggalPersetujuan()));
					}

					map.put("status_persetujuan", status);

					map.put("perpustakaan", prosesTransferStandingInstruction.getKeterangan());

					map.put("tanggal_persetujuan", prosesTransferStandingInstruction.getTanggalPersetujuan());
					map.put("disetujui_oleh", prosesTransferStandingInstruction.getDisetujuiOleh() == null ? ""
							: prosesTransferStandingInstruction.getDisetujuiOleh().getUserNama());

					map.put("bank", bank == null ? "" : bank.getNama());
					map.put("nilai", nilai);

					maps.add(map);
				}
			}
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
	 * Menghasilkan file PDF laporan proses transfer standing instruction untuk ekspor
	 * massal dari toolbar grid.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@code DataCriteria.cetakData()} untuk menghasilkan
	 * file PDF yang dapat diunduh via infrastruktur ekspor generik. Menggunakan template
	 * {@code "akunting/pengajuan_si"} dan parameter dari method {@code parameter()}.</p>
	 *
	 * @param generalValueObject objek yang akan dicetak; di-cast ke
	 *                           {@link ProsesTransferStandingInstruction}
	 * @return file PDF yang dihasilkan
	 * @throws Exception jika template tidak ditemukan atau terjadi kesalahan I/O
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		ProsesTransferStandingInstruction prosesTransferStandingInstruction = (ProsesTransferStandingInstruction) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(prosesTransferStandingInstruction),
				"akunting/pengajuan_si", prosesTransferStandingInstruction.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * Menampilkan laporan cetak proses transfer SI langsung di browser pengguna
	 * sebagai PDF inline (bukan unduhan).
	 *
	 * <p><b>Tujuan:</b> Memberikan pratinjau cetak interaktif kepada pengguna setelah
	 * menyimpan proses baru atau dari tombol cetak di baris grid.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@code Report.generatePDFReport()} dengan parameter
	 * dari {@code parameter()} dan template {@code "akunting/pengajuan_si"}. Laporan
	 * ditampilkan langsung di browser (bukan disimpan ke file).</p>
	 *
	 * @param prosesTransferStandingInstruction entitas yang akan dicetak
	 * @throws Exception jika template tidak ditemukan atau terjadi kesalahan rendering
	 *
	 * <p><b>Thread safety:</b> Harus dipanggil dari thread ZK event-dispatcher.</p>
	 */
	@SuppressWarnings({})
	public static void cetak(ProsesTransferStandingInstruction prosesTransferStandingInstruction) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(prosesTransferStandingInstruction), "akunting/pengajuan_si",
				prosesTransferStandingInstruction.getTanggalPembuatan());
	}
}
