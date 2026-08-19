package ais.action.master.asset;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.asset.helper.PeminjamanMasterAssetDetailAction;
import ais.action.master.asset.helper.PeminjamanMasterAssetHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PeminjamanMasterAsset;
import ais.database.model.asset.PeminjamanMasterAssetDetail;
import ais.database.model.asset.PengembalianMasterAsset;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PengembalianMasterAssetAction</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini adalah Action ZK (composer) yang mengelola seluruh siklus hidup
 * proses pengembalian aset yang sebelumnya dipinjam. Modul ini mencakup
 * pencatatan dokumen pengembalian ({@link PengembalianMasterAsset}), yang
 * merupakan pasangan dari dokumen peminjaman ({@link PeminjamanMasterAsset}).
 * Fungsionalitas yang disediakan meliputi: tampilan daftar pengembalian dengan
 * filter multi-kriteria, form tambah/ubah data pengembalian, persetujuan dan
 * pembatalan persetujuan pengembalian, penghapusan data, serta cetak dokumen
 * pengembalian dalam format PDF. Kelas ini juga mengimplementasikan antarmuka
 * {@link FormSop} sehingga dapat diintegrasikan ke dalam alur kerja SOP
 * (Standar Operasional Prosedur) yang memerlukan persetujuan bertahap.
 *
 * <b>Cara kerja:</b><br>
 * Composer ini di-wire ke halaman ZUL melalui mekanisme ZK. Saat halaman
 * dimuat, {@code doAfterCompose} menginisialisasi hak akses pengguna (CRUD +
 * approve/reject), menyiapkan paging, dan memuat data awal. Daftar data
 * ditampilkan dalam {@link MyGrid} menggunakan inner class
 * {@link PengembalianMasterAssetRenderer}. Setiap baris menampilkan detail
 * peminjaman terkait, informasi pembuat dan pemverifikasi, status aktif,
 * serta tombol aksi. Form tambah/ubah dibangun secara programatik melalui
 * method {@code init} yang memanggil method {@code form} (dari antarmuka
 * {@link FormSop}) untuk membangun grid form. Saat penyimpanan, method
 * {@code onSave} memvalidasi input, menyimpan entitas ke Hibernate, dan
 * memperbarui relasi peminjaman yang bersangkutan. Dokumen PDF dihasilkan
 * melalui {@code Report.generatePDFReport} menggunakan template Jasper Reports
 * {@code asset/pengembalian}.
 *
 * <b>Threading:</b><br>
 * Seluruh operasi berjalan di event-dispatch thread ZK (single-threaded per
 * sesi pengguna). Tidak ada thread terpisah yang digunakan. Operasi database
 * menggunakan sesi Hibernate yang dikelola ZK melalui
 * {@code HibernateUtil.currentSession()}. Operasi yang memerlukan delay
 * (seperti auto-approve setelah simpan) menggunakan
 * {@code Common.createDefaultTimer} untuk menjalankan kode di timer ZK
 * berikutnya, bukan di thread terpisah.
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini mengimplementasikan {@link FormSop}, sehingga perubahan pada
 * kontrak antarmuka tersebut akan mempengaruhi kelas ini. Method {@code form}
 * bersifat publik karena dipanggil oleh mekanisme SOP eksternal. Method
 * {@code generateCode} menggunakan {@link NomorSuratAlurPengadaan#PENGEMBALIAN_BARANG_DATA}
 * untuk format nomor surat; pastikan data ini dikonfigurasi di database.
 * Penghapusan data melalui {@code onDelete} (di renderer) juga menghapus
 * semua {@link PeminjamanMasterAssetDetail} yang berelasi sebelum menghapus
 * entitas utama. Mode persetujuan ({@code persetujuan = true}) mengubah
 * tampilan form menjadi read-only untuk kolom-kolom tertentu.
 */
public class PengembalianMasterAssetAction extends GenericAutowireComposer implements FormSop {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchketerangan;
	private Textbox searchNamaPeminjam;

	private AmbilDataSatuanKerjaBanbox searchparent;

	private Label kode;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PengembalianMasterAsset pengembalianMasterAsset;
	private MyToolbarbuttonConfig add;
	private Label tanggalKembali;
	private Label keperluan;
	private Label lokasiKegiatan;
	private MyGrid gridMasterAsset;

	private boolean persetujuan = false;

	private Tbmuser tbmuser;
	private MyCheckboxConfig setujui;
	private DisposisiSop disposisiSop = null;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Combobox peminjamanMasterAsset;
	private Label tanggalPinjam;
	private Label namaPeminjam;
	private Row rowData;

	private MyCheckboxConfig blmDisetujui;
	private MyCheckboxConfig disetujui;

	/**
	 * <b>Tujuan:</b><br>
	 * Konstruktor default yang digunakan oleh ZK saat men-compose halaman ZUL
	 * utama modul pengembalian aset dalam mode pengelolaan data biasa (bukan
	 * mode persetujuan SOP).
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengambil pengguna yang sedang login melalui {@code Common.getCurrentUser()}
	 * dan menyimpannya ke field {@code tbmuser}. Flag {@code persetujuan} dibiarkan
	 * false (nilai default), menandakan bahwa form akan tampil dalam mode penuh
	 * yang dapat diedit.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (konstruktor).
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Konstruktor ini dipanggil oleh ZK secara reflektif. Jangan tambahkan
	 * parameter ke konstruktor ini tanpa mempertimbangkan cara ZK menginisialisasi
	 * composer.
	 */
	public PengembalianMasterAssetAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Konstruktor yang digunakan ketika Action ini diinstansiasi secara programatik
	 * untuk mode persetujuan SOP. Mode persetujuan mengubah tampilan form menjadi
	 * sebagian read-only dan menambahkan checkbox persetujuan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menyimpan flag {@code persetujuan} ke field instance dan mengambil pengguna
	 * yang sedang login. Flag ini kemudian digunakan di method {@code form} untuk
	 * menentukan apakah field tertentu ditampilkan sebagai input atau label
	 * read-only.
	 *
	 * <b>Parameter:</b><br>
	 * @param persetujuan {@code true} jika form harus ditampilkan dalam mode
	 *                    persetujuan (sebagian field read-only, checkbox persetujuan
	 *                    ditampilkan); {@code false} untuk mode normal.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (konstruktor).
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Konstruktor ini biasanya dipanggil dari sistem SOP saat menampilkan form
	 * untuk approval. Pastikan nilai {@code tbmuser} selalu diinisialisasi di
	 * sini karena {@code doAfterCompose} mungkin tidak terpanggil dalam mode ini.
	 */
	public PengembalianMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Dipanggil oleh framework ZK sebelum komponen halaman di-compose untuk
	 * memverifikasi keamanan akses pengguna ke halaman ini.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@link Common#doCheckSecurity()} yang memeriksa apakah sesi
	 * pengguna valid dan pengguna memiliki hak akses ke modul ini. Kemudian
	 * melanjutkan ke implementasi super class untuk pemrosesan framework ZK.
	 * Jika keamanan gagal, pengguna akan diarahkan ke halaman login.
	 *
	 * <b>Parameter:</b><br>
	 * @param page     Halaman ZK yang sedang di-compose.
	 * @param parent   Komponen induk tempat composer ini di-attach.
	 * @param compInfo Metadata komponen dari definisi ZUL.
	 *
	 * <b>Return:</b><br>
	 * @return {@link org.zkoss.zk.ui.metainfo.ComponentInfo} hasil dari super class,
	 *         digunakan oleh framework untuk melanjutkan proses compose.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jangan hapus atau ubah pemanggilan {@code Common.doCheckSecurity()} karena
	 * ini adalah lapisan keamanan pertama sebelum halaman dibangun.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Metode inisialisasi utama yang dipanggil ZK setelah seluruh komponen halaman
	 * selesai di-wire. Menyiapkan hak akses pengguna, event listener filter,
	 * paging, dan memuat data grid pertama kali secara asinkron.
	 *
	 * <b>Cara kerja:</b><br>
	 * Dimulai dengan validasi sesi ({@code usersTemp}) dan hak akses READ; jika
	 * tidak valid, pengguna diarahkan ke logoff. Selanjutnya event listener
	 * untuk {@code searchparent} (filter satuan kerja) dikonfigurasi untuk
	 * memanggil {@code onSearchDefault} saat berubah. Model pohon satuan kerja
	 * diinisialisasi. Tombol tambah ditampilkan jika pengguna memiliki hak CREATE.
	 * Hak akses UPDATE, DELETE, APPROVE, dan REJECT disimpan ke field boolean
	 * masing-masing untuk digunakan saat rendering baris. Paging diinisialisasi
	 * dengan listener yang memanggil {@code onSearchDefault}. Pemuatan data
	 * pertama dilakukan melalui {@code Common.createDefaultTimer} untuk menghindari
	 * blocking saat compose.
	 *
	 * <b>Parameter:</b><br>
	 * @param comp Komponen root ZK yang telah selesai di-compose dan di-wire.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception yang tidak tertangkap akan disebarkan ke framework ZK dan
	 * ditampilkan sebagai error dialog kepada pengguna.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika ada komponen filter baru yang perlu diinisialisasi (misalnya combo
	 * jenis aset), tambahkan inisialisasinya di sini setelah baris inisialisasi
	 * yang sudah ada. Pastikan guard null untuk komponen yang mungkin tidak ada
	 * di semua varian halaman ZUL.
	 *
	 * @throws Exception jika terjadi kesalahan fatal saat proses inisialisasi.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
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
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

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
	 * <b>Tujuan:</b><br>
	 * Inner class renderer yang bertanggung jawab untuk merender setiap baris
	 * pada grid daftar pengembalian aset. Setiap baris menampilkan informasi
	 * lengkap tentang satu entri pengembalian beserta detail peminjaman terkait
	 * dan tombol-tombol aksi (cetak, setuju, batalkan, ubah, hapus).
	 *
	 * <b>Cara kerja:</b><br>
	 * Menerima objek {@link PengembalianMasterAsset} dan membangun tampilan baris
	 * dengan komponen-komponen berikut: panel detail peminjaman melalui
	 * {@link PeminjamanMasterAssetDetailAction}, kode pengembalian via
	 * {@link RevisiHelper#createNewRevisi}, nama peminjam, keperluan, lokasi
	 * kegiatan, rentang tanggal pinjam-kembali, informasi pembuat (nama dan
	 * tanggal), informasi persetujuan (nama dan tanggal), keterangan beserta
	 * link SOP jika ada, status aktif (checkbox jika belum disetujui dan punya
	 * hak edit, atau label jika sudah disetujui), dan toolbar aksi.
	 * Tombol persetujuan dan pembatalan menampilkan dialog konfirmasi sebelum
	 * mengubah state. Setelah persetujuan, dokumen PDF langsung dicetak melalui
	 * {@code cetak(pengembalianMasterAsset)}.
	 *
	 * <b>Parameter:</b><br>
	 * @param arg0 Baris ZK ({@link Row}) yang akan diisi komponen.
	 * @param arg1 Data baris berupa objek {@link PengembalianMasterAsset}.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception saat penghapusan (termasuk pelanggaran foreign key) ditangkap
	 * dan ditampilkan sebagai pesan error melalui {@link MyMessageboxConfig}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Renderer ini dipasang di {@code onSearchDefault}. Jika ada kolom baru
	 * yang perlu ditampilkan, tambahkan juga definisi {@code <column>} yang
	 * sesuai di file ZUL pasangan halaman ini.
	 */
	class PengembalianMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b><br>
		 * Merender satu baris data pengembalian aset ke dalam komponen ZK Row,
		 * meliputi seluruh informasi pengembalian, detail peminjaman terkait,
		 * status persetujuan, dan toolbar aksi lengkap.
		 *
		 * <b>Cara kerja:</b><br>
		 * Pertama membuat komponen {@link PeminjamanMasterAssetDetailAction} yang
		 * menampilkan daftar aset yang dikembalikan. Kemudian menampilkan kode
		 * pengembalian, nama peminjam, keperluan, lokasi, rentang tanggal, info
		 * pembuat, info persetujuan, dan keterangan. Status aktif ditampilkan
		 * sebagai checkbox interaktif jika pengguna punya hak edit dan pengembalian
		 * belum disetujui. Toolbar berisi tombol cetak, setuju, batalkan, ubah,
		 * dan hapus dengan visibilitas yang dikontrol oleh hak akses dan status
		 * persetujuan masing-masing.
		 *
		 * <b>Parameter:</b><br>
		 * @param arg0 Baris ZK target.
		 * @param arg1 Objek {@link PengembalianMasterAsset} yang akan dirender.
		 *
		 * <b>Return:</b><br>
		 * Tidak ada nilai balik (void).
		 *
		 * <b>Penanganan error:</b><br>
		 * Exception saat hapus data ditangkap dan ditampilkan sebagai pesan error
		 * dengan detail message exception untuk membantu diagnosis.
		 *
		 * <b>Pemeliharaan:</b><br>
		 * Label {@code disetujuiOleh} dan {@code disetujuiTanggal} adalah
		 * {@link MyLabelAgakKecil} yang di-capture ke variabel final agar dapat
		 * diperbarui setelah operasi approve/reject tanpa perlu reload seluruh grid.
		 * Ini adalah optimasi UX yang penting.
		 *
		 * @throws Exception jika terjadi kesalahan saat akses database atau rendering komponen.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PengembalianMasterAsset pengembalianMasterAsset = (PengembalianMasterAsset) arg1;

			final PeminjamanMasterAssetDetailAction detail;
			(detail = new PeminjamanMasterAssetDetailAction(pengembalianMasterAsset.getPeminjamanMasterAsset(), true))
					.setParent(arg0);

			RevisiHelper.createNewRevisi(PengembalianMasterAsset.class, pengembalianMasterAsset,
					pengembalianMasterAsset.getKode()).setParent(arg0);

			new Label(pengembalianMasterAsset.getPeminjamanMasterAsset().getNamaPeminjam()).setParent(arg0);

			new Label(pengembalianMasterAsset.getPeminjamanMasterAsset().getKeperluan()).setParent(arg0);

			new Label(pengembalianMasterAsset.getPeminjamanMasterAsset().getLokasiKegiatan()).setParent(arg0);

			new Label(Common.dateFormat1.get().format(pengembalianMasterAsset.getPeminjamanMasterAsset().getTanggalPinjam())
					+ " sd "
					+ Common.dateFormat1.get().format(pengembalianMasterAsset.getPeminjamanMasterAsset().getTanggalKembali()))
					.setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(pengembalianMasterAsset.getDibuatOleh() == null ? ""
					: pengembalianMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(pengembalianMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pengembalianMasterAsset.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(pengembalianMasterAsset.getDisetujuiOleh() == null ? ""
					: pengembalianMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(pengembalianMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pengembalianMasterAsset.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(pengembalianMasterAsset.getKeterangan())).setParent(vbox1);
			if (pengembalianMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa,
						"SOP " + pengembalianMasterAsset.getDisposisiSop().getKeterangan() + " ("
								+ pengembalianMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pengembalianMasterAsset.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			if (pengembalianMasterAsset.getDisposisiSop() != null
					&& !pengembalianMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (pengembalianMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pengembalianMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pengembalianMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pengembalianMasterAsset);
					}
				});
			} else {
				new Label(pengembalianMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pengembalian Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(pengembalianMasterAsset);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && pengembalianMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && pengembalianMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Pengembalian Barang/Jasa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();

										pengembalianMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										pengembalianMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, pengembalianMasterAsset);

										disetujuiTanggal
												.setValue(pengembalianMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pengembalianMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pengembalianMasterAsset.getDisetujuiOleh() == null ? ""
												: pengembalianMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pengembalianMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pengembalianMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pengembalianMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pengembalianMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

										cetak(pengembalianMasterAsset);

									}
								}
							});
				}

			});
			aksiButtons.add(disetujui);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Pengembalian Barang/Jasa ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pengembalianMasterAsset.setDisetujuiOleh(null);
										pengembalianMasterAsset.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, pengembalianMasterAsset);

										disetujuiTanggal
												.setValue(pengembalianMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pengembalianMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pengembalianMasterAsset.getDisetujuiOleh() == null ? ""
												: pengembalianMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pengembalianMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pengembalianMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pengembalianMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pengembalianMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && pengembalianMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pengembalianMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && pengembalianMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											if (SopUtil.hapusDisposisi(session,
													pengembalianMasterAsset.getDisposisiSop())) {
												List<PeminjamanMasterAssetDetail> peminjamanMasterAssetDetails = session
														.createCriteria(PeminjamanMasterAssetDetail.class)
														.add(Restrictions.eq("pengembalianMasterAsset",
																pengembalianMasterAsset))
														.list();
												for (PeminjamanMasterAssetDetail peminjamanMasterAssetDetail : peminjamanMasterAssetDetails) {
													session.delete(peminjamanMasterAssetDetail);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, pengembalianMasterAsset);
														session.flush();

														onSearchDefault(event);
													}
												});
											}
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

										}

									}

								}
							});

				}
			});
			aksiButtons.add(hapus);

			// Susun semua tombol: max 3 per baris, rata tengah
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Menangani event penambahan data pengembalian aset baru. Dipanggil saat
	 * pengguna menekan tombol "Tambah" pada toolbar halaman daftar.
	 *
	 * <b>Cara kerja:</b><br>
	 * Membuat instans {@link PengembalianMasterAsset} baru (tanpa ID), memanggil
	 * method {@code init} untuk membangun dan menampilkan form dalam dialog modal.
	 * Dialog {@code addWindow} kemudian ditampilkan dalam mode modal.
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu aksi ini, biasanya event onClick dari
	 *              tombol tambah di toolbar.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception akan disebarkan ke framework ZK untuk ditampilkan sebagai
	 * error dialog.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini adalah delegator tipis ke {@code init}. Jika ada logika
	 * pre-initialization yang perlu dilakukan sebelum membuka form tambah,
	 * tambahkan di sini sebelum memanggil {@code init}.
	 *
	 * @throws Exception jika terjadi kesalahan saat membangun form dialog.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PengembalianMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Menginisialisasi dan membangun form dialog untuk penambahan atau pengubahan
	 * data pengembalian aset. Method ini membangun layout dialog secara programatik
	 * dan mengonfigurasi tombol simpan dan batal.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menyimpan referensi entitas ke field instance dan menyetel judul dialog
	 * sesuai mode (tambah atau ubah). Membersihkan konten dialog sebelumnya
	 * dengan {@code Common.clear}. Membangun layout {@link Borderlayout} dengan
	 * area Center berisi form (dibangun oleh method {@code form}) dan area South
	 * berisi toolbar dengan tombol Batal dan Simpan. Tombol Batal menutup dialog
	 * ({@code setVisible(false)}). Tombol Simpan memanggil {@code onSave}, dan
	 * jika berhasil, memuat ulang grid dan menutup dialog.
	 *
	 * <b>Parameter:</b><br>
	 * @param pengembalianMasterAsset Entitas yang akan diedit. Jika ID-nya null,
	 *                                berarti mode tambah data baru; jika tidak null,
	 *                                berarti mode ubah data yang sudah ada.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception disebarkan ke pemanggil (biasanya {@code onAdd} atau event
	 * listener tombol ubah di renderer).
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Field {@code disposisiSop} direset ke null sebelum memanggil {@code form}
	 * agar form selalu dimulai tanpa disposisi SOP yang tersisa dari sesi
	 * sebelumnya. Jangan hapus baris {@code disposisiSop=null}.
	 *
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form.
	 */
	private void init(PengembalianMasterAsset pengembalianMasterAsset) throws Exception {
		this.pengembalianMasterAsset = pengembalianMasterAsset;
		addWindow.setTitle(pengembalianMasterAsset.getId() == null ? "Tambah Pengembalian Barang" : "Ubah Pengembalian Barang");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pengembalianMasterAsset, disposisiSop, save, null));

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

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

	}

	/**
	 * <b>Tujuan:</b><br>
	 * Memvalidasi input form pengembalian aset dan menyimpan data ke database.
	 * Method ini menangani baik mode tambah (INSERT) maupun mode ubah (UPDATE)
	 * untuk entitas {@link PengembalianMasterAsset}.
	 *
	 * <b>Cara kerja:</b><br>
	 * Validasi pertama memastikan kode tidak kosong dan peminjaman (combo
	 * {@code peminjamanMasterAsset}) sudah dipilih. Kemudian setiap baris di
	 * {@code gridMasterAsset} diperiksa untuk memastikan semua detail aset
	 * sudah diisi. Jika semua validasi lolos, sesi Hibernate dibuka dan entitas
	 * peminjaman dimuat ulang dari database untuk memastikan konsistensi. Jika
	 * ada disposisi SOP, entitas pengembalian dikaitkan. Semua field form
	 * disalin ke entitas. Nilai total dihitung dari jumlah {@code hargaBeli}
	 * semua detail. Untuk data baru, kode nomor surat digenerate melalui
	 * {@code generateCode(true)} yang juga menginkrementasi counter. Data
	 * peminjaman diperbarui untuk menunjuk ke entitas pengembalian ini. Semua
	 * detail aset disimpan/diperbarui. Di timer berikutnya, status persetujuan
	 * diset sesuai checkbox, dan dokumen PDF langsung dicetak.
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu penyimpanan, biasanya event onClick dari
	 *              tombol simpan di toolbar form.
	 *
	 * <b>Return:</b><br>
	 * @return {@code true} jika penyimpanan berhasil; {@code false} jika ada
	 *         validasi yang gagal (dialog peringatan ditampilkan).
	 *
	 * <b>Penanganan error:</b><br>
	 * Validasi ditangani dengan menampilkan dialog peringatan dan mengembalikan
	 * {@code false}. Operasi database yang gagal akan melempar exception yang
	 * disebarkan ke pemanggil.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Perhatikan bahwa kode nomor surat hanya digenerate ({@code generateCode(true)})
	 * untuk data baru. Untuk data ubah, kode yang sudah ada dipertahankan.
	 * Field {@code nilai} di entitas pengembalian adalah jumlah dari semua
	 * {@code hargaBeli} detail peminjaman, bukan harga total aset.
	 *
	 * @throws Exception jika terjadi kesalahan database atau kesalahan komponen ZK.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Pengembalian belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Pengembalian atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		PeminjamanMasterAsset a = (PeminjamanMasterAsset) (peminjamanMasterAsset.getSelectedItem() == null ? null
				: peminjamanMasterAsset.getSelectedItem().getValue());

		if (a == null) {
			MyMessageboxConfig.show("Mohon maaf, Data Peminjam belum dipilih. Langkah yang dapat dilakukan: (1) Pilih nomor/kode peminjaman dari dropdown Peminjam; (2) Pastikan data peminjaman sudah ada dan berstatus aktif; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = (PeminjamanMasterAssetDetail) row
					.getAttribute("peminjamanMasterAssetDetail");
			if (peminjamanMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar pengembalian belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih aset yang akan dikembalikan dari daftar; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();

		PeminjamanMasterAsset asset = (PeminjamanMasterAsset) session.createCriteria(PeminjamanMasterAsset.class)
				.add(Restrictions.idEq(a.getId())).uniqueResult();

		if (pengembalianMasterAsset.getId() != null) {
			pengembalianMasterAsset = (PengembalianMasterAsset) session.load(PengembalianMasterAsset.class,
					pengembalianMasterAsset.getId());
		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pengembalianMasterAsset.setDisposisiSop(disposisiSop);
		}

		pengembalianMasterAsset.setKode(kode.getValue());
		pengembalianMasterAsset.setKeterangan(keterangan.getValue());
		pengembalianMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());
		pengembalianMasterAsset.setPeminjamanMasterAsset(asset);

		Double jumlah = 0.0;
		for (Row row : rowsMasterAsset) {
			PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = (PeminjamanMasterAssetDetail) row
					.getAttribute("peminjamanMasterAssetDetail");
			Double n = peminjamanMasterAssetDetail.getHargaBeli();

			jumlah += n;
		}

		pengembalianMasterAsset.setNilai(jumlah);

		if (pengembalianMasterAsset.getId() != null) {
			session.update(pengembalianMasterAsset);
		} else {
			pengembalianMasterAsset.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			pengembalianMasterAsset.setKode(kode.getValue());
			session.save(pengembalianMasterAsset);
		}

		asset.setPengembalianMasterAsset(pengembalianMasterAsset);
		session.update(asset);

		for (Row row : rowsMasterAsset) {
			PeminjamanMasterAssetDetail peminjamanMasterAssetDetail = (PeminjamanMasterAssetDetail) row
					.getAttribute("peminjamanMasterAssetDetail");
			peminjamanMasterAssetDetail.setPeminjamanMasterAsset(asset);
			session.saveOrUpdate(peminjamanMasterAssetDetail);
		}

		session.flush();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (setujui.isChecked()) {
					Session session = HibernateUtil.currentSession();
					pengembalianMasterAsset.setDisetujuiOleh(tbmuser);
					pengembalianMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
					Common.refreshUpdate(session, pengembalianMasterAsset);
				} else {
					Session session = HibernateUtil.currentSession();
					pengembalianMasterAsset.setDisetujuiOleh(null);
					pengembalianMasterAsset.setTanggalPersetujuan(null);
					Common.refreshUpdate(session, pengembalianMasterAsset);
				}

				cetak(pengembalianMasterAsset);
			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Membangun peta parameter ({@link Map}) yang berisi semua data yang diperlukan
	 * untuk menghasilkan dokumen PDF laporan pengembalian aset melalui Jasper Reports.
	 *
	 * <b>Cara kerja:</b><br>
	 * Melakukan refresh entitas dari database jika memiliki ID. Membuat peta
	 * parameter dengan kunci acak (untuk menghindari caching) menggunakan
	 * {@code HashMapGenerator.getRand()}. Data utama entitas disisipkan melalui
	 * {@code Common.insertProperty} yang menggunakan reflection untuk membaca
	 * semua getter. Parameter SOP ditambahkan melalui
	 * {@code DisposisiAlurSop.parameterMap}. Kemudian semua
	 * {@link PeminjamanMasterAssetDetail} yang berelasi dengan peminjaman
	 * yang bersangkutan diambil dan diurutkan berdasarkan jenis dan kelompok
	 * aset. Untuk setiap detail, dibuat peta terpisah dengan field-field yang
	 * dibutuhkan template (nama, kode, barcode, spesifikasi, tipe, harga,
	 * status persetujuan, dll). Semua peta detail dikumpulkan dalam list
	 * {@code maps} dan disisipkan ke parameter utama. Field yang mengandung
	 * referensi {@code disposisiSop} di-null-kan untuk menghindari masalah
	 * serialisasi di Jasper Reports.
	 *
	 * <b>Parameter:</b><br>
	 * @param pengembalianMasterAsset Entitas pengembalian yang akan dicetak.
	 *
	 * <b>Return:</b><br>
	 * @return Peta parameter yang siap dikirim ke engine Jasper Reports.
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit; exception disebarkan ke pemanggil
	 * ({@code cetak} atau {@code cetakData}).
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Template Jasper Reports yang digunakan adalah {@code asset/pengembalian}.
	 * Jika ada field baru yang perlu ditambahkan ke laporan, tambahkan kunci
	 * baru ke peta di sini dan tambahkan field yang sesuai di template JRXML.
	 * Perhatikan bahwa peta detail menggunakan kunci {@code isbn} untuk kode
	 * aset (bukan ISBN sungguhan), ini adalah konvensi lama yang perlu
	 * dipertahankan untuk kompatibilitas template.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map parameter(PengembalianMasterAsset pengembalianMasterAsset) {
		if (pengembalianMasterAsset != null && pengembalianMasterAsset.getId() != null) {
			HibernateUtil.currentSession().refresh(pengembalianMasterAsset);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", pengembalianMasterAsset.getId());

		Common.insertProperty(PengembalianMasterAsset.class, pengembalianMasterAsset, parameters, "data");

		DisposisiAlurSop.parameterMap(pengembalianMasterAsset.getDisposisiSop(), parameters);
		Session session = HibernateUtil.currentSession();

		List<PeminjamanMasterAssetDetail> peminjamanMasterAssetDetails = session
				.createCriteria(PeminjamanMasterAssetDetail.class).createAlias("masterAsset", "masterAsset")
				.addOrder(Order.asc("masterAsset.jenisAsset")).addOrder(Order.asc("masterAsset.kelompokAsset"))
				.addOrder(Order.asc("masterAsset.id"))
				.add(Restrictions.eq("peminjamanMasterAsset", pengembalianMasterAsset.getPeminjamanMasterAsset()))
				.list();
		List<Map> maps = new ArrayList<Map>();
		for (PeminjamanMasterAssetDetail peminjamanMasterAssetDetail : peminjamanMasterAssetDetails) {
			Map map = new HashMap();
			Common.insertProperty(PeminjamanMasterAssetDetail.class, peminjamanMasterAssetDetail, map, "data");

			map.put("kelompok_asset", peminjamanMasterAssetDetail.getMasterAsset().getKelompokAsset() == null ? ""
					: peminjamanMasterAssetDetail.getMasterAsset().getKelompokAsset().getNama());

			map.put("jenis_asset", peminjamanMasterAssetDetail.getMasterAsset().getJenisAsset() == null ? ""
					: peminjamanMasterAssetDetail.getMasterAsset().getJenisAsset().getNama());

			map.put("tipe_asset", peminjamanMasterAssetDetail.getMasterAsset().getTipe());

			map.put("spesifikasi", peminjamanMasterAssetDetail.getMasterAsset().getSpesifikasi());

			map.put("hargabeli", peminjamanMasterAssetDetail.getHargaBeli());
			map.put("jumlah", 1.0);
			map.put("nama", peminjamanMasterAssetDetail.getMasterAsset().getNama());
			map.put("kode",
					peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getPengembalianMasterAsset().getKode());
			map.put("isbn", peminjamanMasterAssetDetail.getMasterAsset().getKode());
			map.put("barcode", peminjamanMasterAssetDetail.getAssetDetail() == null ? ""
					: peminjamanMasterAssetDetail.getAssetDetail().getBarcode());
			map.put("nama_barang", peminjamanMasterAssetDetail.getAssetDetail() == null ? ""
					: peminjamanMasterAssetDetail.getAssetDetail().getNama());
			String status = "";
			if (peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getPengembalianMasterAsset()
					.getDisetujuiOleh() == null) {
				status = "Belum disetujui";
			} else {
				status = "Disetujui oleh " + peminjamanMasterAssetDetail
						.getPeminjamanMasterAsset().getPengembalianMasterAsset().getDisetujuiOleh().getUserNama()
						+ " pada "
						+ (peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getPengembalianMasterAsset()
								.getTanggalPersetujuan() == null
										? ""
										: Common.dateFormat51.get()
												.format(peminjamanMasterAssetDetail.getPeminjamanMasterAsset()
														.getPengembalianMasterAsset().getTanggalPersetujuan()));
			}

			map.put("status_persetujuan", status);

			map.put("perpustakaan", peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getPengembalianMasterAsset()
					.getKeterangan());
			map.put("tanggal_persetujuan", peminjamanMasterAssetDetail.getPeminjamanMasterAsset()
					.getPengembalianMasterAsset().getTanggalPersetujuan());
			map.put("disetujui_oleh",
					peminjamanMasterAssetDetail.getPeminjamanMasterAsset().getPengembalianMasterAsset()
							.getDisetujuiOleh() == null ? ""
									: peminjamanMasterAssetDetail.getPeminjamanMasterAsset()
											.getPengembalianMasterAsset().getDisetujuiOleh().getUserNama());

			maps.add(map);
		}

		parameters.put("maps", maps);

		for (Object o : parameters.keySet()) {
			if (o.toString().contains("disposisiSop")) {
				parameters.put(o.toString(), null);
			}
		}
		return parameters;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Menghasilkan file PDF laporan pengembalian aset dan mengembalikannya sebagai
	 * {@link File}. Method ini diimplementasikan dari antarmuka {@link FormSop}
	 * untuk mendukung integrasi dengan sistem SOP yang mungkin memerlukan file
	 * laporan secara programatik.
	 *
	 * <b>Cara kerja:</b><br>
	 * Melakukan cast {@code generalValueObject} ke {@link PengembalianMasterAsset},
	 * membangun parameter laporan melalui method {@code parameter}, lalu memanggil
	 * {@code Report.generateFileReport} dengan template {@code asset/pengembalian}
	 * dan format {@code PDF}. List {@code maps} dibiarkan null karena data detail
	 * sudah dimasukkan ke dalam parameter map.
	 *
	 * <b>Parameter:</b><br>
	 * @param generalValueObject Objek data yang di-cast menjadi
	 *                           {@link PengembalianMasterAsset} untuk keperluan cetak.
	 *
	 * <b>Return:</b><br>
	 * @return Objek {@link File} yang berisi file PDF yang telah digenerate,
	 *         siap untuk diunduh atau ditampilkan.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari engine Jasper Reports disebarkan ke pemanggil.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Template laporan berada di path {@code asset/pengembalian} relatif terhadap
	 * direktori template Jasper yang dikonfigurasi di aplikasi. Pastikan file
	 * JRXML/Jasper template ada dan sudah dikompilasi sebelum deploy.
	 *
	 * @throws Exception jika terjadi kesalahan saat menghasilkan laporan PDF.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PengembalianMasterAsset pengembalianMasterAsset = (PengembalianMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(pengembalianMasterAsset), "asset/pengembalian",
				pengembalianMasterAsset.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Menghasilkan dan menampilkan laporan PDF pengembalian aset langsung ke
	 * browser pengguna. Method ini adalah shortcut untuk mencetak dokumen
	 * pengembalian tanpa perlu menyimpannya sebagai file terlebih dahulu.
	 *
	 * <b>Cara kerja:</b><br>
	 * Memanggil {@code Report.generatePDFReport} dengan parameter yang dibangun
	 * dari method {@code parameter(pengembalianMasterAsset)}. Laporan digenerate
	 * menggunakan template {@code asset/pengembalian} dan format {@code PDF}.
	 * Tanggal pembuatan entitas digunakan sebagai tanggal laporan.
	 *
	 * <b>Parameter:</b><br>
	 * @param pengembalianMasterAsset Entitas pengembalian yang akan dicetak
	 *                                dokumennya.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void); laporan langsung dikirim ke browser.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception disebarkan ke pemanggil. Method ini biasanya dipanggil dari
	 * timer asinkron, sehingga exception yang tidak tertangkap akan dilaporkan
	 * oleh framework ZK.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini dipanggil secara otomatis setelah approve berhasil dan setelah
	 * simpan baru/ubah. Jika perilaku ini tidak diinginkan dalam semua kasus,
	 * tambahkan parameter boolean untuk mengontrol apakah cetak otomatis
	 * dilakukan atau tidak.
	 *
	 * @throws Exception jika terjadi kesalahan saat menghasilkan atau mengirimkan
	 *                   laporan PDF.
	 */
	@SuppressWarnings({})
	private void cetak(PengembalianMasterAsset pengembalianMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(pengembalianMasterAsset), "asset/pengembalian",
				pengembalianMasterAsset.getTanggalPembuatan());
	}

	private Checkbox searchaktif;

	/**
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate yang digunakan
	 * untuk mengambil data {@link PengembalianMasterAsset} sesuai dengan filter
	 * yang aktif di halaman. Criteria ini digunakan untuk pengambilan data grid
	 * dan penghitungan total baris untuk paging.
	 *
	 * <b>Cara kerja:</b><br>
	 * Pertama memeriksa apakah {@code searchparent} tersedia; jika tidak,
	 * mengembalikan null. Mengambil satuan kerja yang dipilih dari banbox dan
	 * membangun set semua satuan kerja yang termasuk (termasuk sub-hierarki
	 * melalui {@code satuanKerjaTreeModel.getChildsSet}). Criteria utama
	 * {@link PengembalianMasterAsset} dibangun dengan filter: status aktif
	 * (checkbox {@code searchaktif}), status persetujuan ({@code blmDisetujui}
	 * dan {@code disetujui} checkbox), alias untuk join ke peminjaman, filter
	 * satuan kerja (menggunakan set hierarki), filter nama peminjam (ilike),
	 * filter kode (ilike), dan filter keterangan (ilike). Jika {@code order}
	 * true, ditambahkan ORDER BY id DESC.
	 *
	 * <b>Parameter:</b><br>
	 * @param order Jika {@code true}, menambahkan ORDER BY id DESC pada criteria.
	 *
	 * <b>Return:</b><br>
	 * @return Objek {@link Criteria} siap eksekusi, atau {@code null} jika
	 *         {@code searchparent} belum tersedia.
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit; exception dari Hibernate disebarkan
	 * ke pemanggil.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Filter status persetujuan menggunakan logika: jika kedua checkbox dicentang
	 * atau keduanya tidak dicentang, tidak ada filter (tampilkan semua / tampilkan
	 * kosong). Ini adalah perilaku yang disengaja. Satuan kerja yang null di
	 * entitas dianggap sebagai "bisa dilihat oleh semua satuan kerja".
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
		Criteria criteria = session.createCriteria(PengembalianMasterAsset.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(blmDisetujui == null || disetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() && disetujui.isChecked() ? Restrictions.sqlRestriction("true")
						: !blmDisetujui.isChecked() && !disetujui.isChecked() ? Restrictions.sqlRestriction("false")
								: blmDisetujui.isChecked() ? Restrictions.isNull("disetujuiOleh")
										: Restrictions.isNotNull("disetujuiOleh"))

				.createAlias("peminjamanMasterAsset", "peminjamanMasterAsset")

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchNamaPeminjam.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("peminjamanMasterAsset.namaPeminjam", searchNamaPeminjam.getValue().trim(),
								MatchMode.ANYWHERE))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

		;
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Event handler utama yang memuat ulang daftar pengembalian aset ke dalam
	 * grid sesuai filter yang aktif saat ini. Dipanggil saat filter berubah,
	 * tombol cari ditekan, atau setelah operasi simpan/hapus/approve selesai.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menghitung total baris untuk paging menggunakan {@code Common.initPaging}
	 * dengan criteria tanpa pengurutan. Kemudian mengambil data halaman saat ini
	 * menggunakan criteria dengan pengurutan, batas {@code Common.ROWS_COUNT_ON_PAGE}
	 * baris, dan offset berdasarkan halaman aktif di komponen paging. Model data
	 * dibungkus dalam {@link SimpleListModel} dan diset ke grid bersama renderer
	 * {@link PengembalianMasterAssetRenderer}.
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu pencarian (bisa null jika dipanggil
	 *              secara programatik setelah operasi CRUD).
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception dari Hibernate disebarkan ke framework ZK.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * {@code Common.ROWS_COUNT_ON_PAGE} menentukan jumlah baris per halaman
	 * secara global. Jika {@code paging} null (misalnya dalam mode embed tanpa
	 * komponen paging), {@code paging.getActivePage()} akan mengembalikan 0
	 * melalui guard ekspresi ternary.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PengembalianMasterAsset> pengembalianMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pengembalianMasterAsset);
		grid.setRowRenderer(new PengembalianMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b><br>
	 * Membangun dan mengembalikan grid form input untuk data pengembalian aset.
	 * Method ini merupakan implementasi dari antarmuka {@link FormSop} yang
	 * memungkinkan form ini digunakan baik dalam dialog biasa maupun dalam
	 * alur persetujuan SOP.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menginisialisasi {@code satuanKerjaTreeModel} jika null. Membangun
	 * {@link MyGrid} dengan dua kolom (label 30%, nilai 70%). Setiap baris form
	 * berisi label di kolom kiri dan input/display di kolom kanan. Field-field
	 * yang dibangun meliputi: combo peminjaman (hanya yang sudah disetujui dan
	 * belum dikembalikan), kode pengembalian (auto-generate, read-only), tanggal
	 * pembuatan (bisa diedit atau label read-only tergantung mode persetujuan),
	 * tanggal pinjam (informasi dari peminjaman, read-only), nama peminjam
	 * (informasi, read-only), keperluan (informasi, read-only), lokasi kegiatan
	 * (informasi, read-only), tanggal kembali (informasi, read-only), keterangan
	 * pengembalian (textarea atau read-only), baris data detail aset, dan
	 * checkbox status persetujuan (hanya dalam mode persetujuan).
	 * Event listener pada combo peminjaman memperbarui semua field informasi
	 * secara reaktif saat peminjaman dipilih.
	 *
	 * <b>Parameter:</b><br>
	 * @param generalValueObject Entitas {@link PengembalianMasterAsset} yang
	 *                           akan diisi ke form (bisa baru atau yang sudah ada).
	 * @param disposisiSop       Disposisi SOP yang terkait (bisa null untuk
	 *                           mode non-SOP).
	 * @param save               Tombol simpan yang akan diaktifkan setelah form
	 *                           siap (dibutuhkan oleh integrasi SOP).
	 * @param setujuiData        Event listener yang dipanggil saat status
	 *                           persetujuan berubah (bisa null).
	 *
	 * <b>Return:</b><br>
	 * @return {@link MyGrid} yang berisi form input siap ditampilkan.
	 *
	 * <b>Penanganan error:</b><br>
	 * Exception disebarkan ke pemanggil.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Logic {@code this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null
	 * || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop}
	 * memastikan disposisi yang sudah ada tidak ditimpa oleh disposisi null dari
	 * pemanggil SOP yang memberikan disposisi baru satu kali kemudian null
	 * di panggilan berikutnya. Jangan sederhanakan logika ini tanpa memahami
	 * alur SOP sepenuhnya.
	 *
	 * @throws Exception jika terjadi kesalahan saat membangun komponen ZK.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		this.pengembalianMasterAsset = (PengembalianMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		pengembalianMasterAsset.setSatuanKerja(tbmuser.ambilSatuanKerja());

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		if (searchparent != null) {

			if (parent != null) {
				satuanKerjas.clear();
				satuanKerjas.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peminjaman *"));
		peminjamanMasterAsset = new Combobox();
		Common.insertCombo(peminjamanMasterAsset,
				new String[] { "kode", "namaPeminjam", "tanggalPinjam", "tanggalKembali" }, "keperluan",
				PeminjamanMasterAsset.class,

				Restrictions.and(
						Restrictions.or(Restrictions.isNull("satuanKerja"),
								satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(
												parent == null ? Restrictions.isNull("satuanKerja")
														: Restrictions.sqlRestriction("false"),
												Restrictions.in("satuanKerja", satuanKerjas))),

						Restrictions.and(Restrictions.isNotNull("disetujuiOleh"),
								Restrictions.isNull("pengembalianMasterAsset"))));
		Common.selectComboItem(true, peminjamanMasterAsset, pengembalianMasterAsset.getPeminjamanMasterAsset());
		peminjamanMasterAsset.setReadonly(true);
		row.appendChild(peminjamanMasterAsset);
		peminjamanMasterAsset.setWidth("90%");

		if (pengembalianMasterAsset.getPeminjamanMasterAsset() != null) {
			peminjamanMasterAsset.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pengembalian *"));

		tanggalPembuatan = new MyDatebox(
				pengembalianMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: pengembalianMasterAsset.getTanggalPembuatan());

		if (pengembalianMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			pengembalianMasterAsset.setKode(noAgenda);
		}

		kode = new Label(pengembalianMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pengembalianMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat5.get().format(tanggalPembuatan.getValue())));
		} else {
			row.appendChild(tanggalPembuatan);
		}
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pinjam *"));
		row.appendChild(tanggalPinjam = new Label(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
				: Common.dateFormat1.get().format(pengembalianMasterAsset.getPeminjamanMasterAsset().getTanggalPinjam())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Peminjam *"));
		row.appendChild(namaPeminjam = new Label(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
				: pengembalianMasterAsset.getPeminjamanMasterAsset().getNamaPeminjam()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keperluan *"));
		row.appendChild(keperluan = new Label(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
				: pengembalianMasterAsset.getPeminjamanMasterAsset().getKeperluan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi Kegiatan *"));
		row.appendChild(lokasiKegiatan = new Label(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
				: pengembalianMasterAsset.getPeminjamanMasterAsset().getLokasiKegiatan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Kembali *"));

		row.appendChild(tanggalKembali = new Label(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
				: Common.dateFormat1.get().format(pengembalianMasterAsset.getPeminjamanMasterAsset().getTanggalKembali())));

		keterangan = new MyTextbox(
				pengembalianMasterAsset.getKeterangan() == null ? "" : pengembalianMasterAsset.getKeterangan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Pengembalian"));
		if (persetujuan) {
			row.appendChild(new Label(pengembalianMasterAsset.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		rowData = new MyFormRow();
		rowData.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowData, "2");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PeminjamanMasterAsset asset = (PeminjamanMasterAsset) (peminjamanMasterAsset.getSelectedItem() == null
						? null
						: peminjamanMasterAsset.getSelectedItem().getValue());

				pengembalianMasterAsset.setPeminjamanMasterAsset(asset);

				tanggalPinjam.setValue(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
						: Common.dateFormat1.get()
								.format(pengembalianMasterAsset.getPeminjamanMasterAsset().getTanggalPinjam()));

				namaPeminjam.setValue(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
						: pengembalianMasterAsset.getPeminjamanMasterAsset().getNamaPeminjam());

				keperluan.setValue(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
						: pengembalianMasterAsset.getPeminjamanMasterAsset().getKeperluan());

				lokasiKegiatan.setValue(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
						: pengembalianMasterAsset.getPeminjamanMasterAsset().getLokasiKegiatan());

				tanggalKembali.setValue(pengembalianMasterAsset.getPeminjamanMasterAsset() == null ? ""
						: Common.dateFormat1.get()
								.format(pengembalianMasterAsset.getPeminjamanMasterAsset().getTanggalKembali()));

				Common.clear(rowData);
				gridMasterAsset = new MyGrid();
				rowData.appendChild(new PeminjamanMasterAssetHelper(gridMasterAsset, true)
						.initDetail(pengembalianMasterAsset.getPeminjamanMasterAsset(), persetujuan));

			}
		};

		row = new MyFormRow();
		row.setVisible(persetujuan && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(setujui = new MyCheckboxConfig("Setujui Pengajuan Pengembalian ini"));
		setujui.setChecked(pengembalianMasterAsset.getDisetujuiOleh() != null);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox && setujui != arg0.getTarget()) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");
					if (selesai != null && selesai) {
						setujui.setChecked(true);
						setujui.setDisabled(true);
					} else {
						setujui.setChecked(false);
						setujui.setDisabled(false);
					}
				}
			}
		});

		Common.createDefaultTimer(eventListener);

		peminjamanMasterAsset.addEventListener("onChange", eventListener);

		if (setujuiData != null) {
			setujui.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, pengembalianMasterAsset.getDisetujuiOleh() != null));
				}
			});
		}

		return grid;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Mengembalikan istilah/nama modul yang digunakan dalam sistem SOP untuk
	 * mengidentifikasi jenis pengajuan ini. Digunakan oleh framework SOP untuk
	 * menampilkan label yang sesuai pada antarmuka persetujuan.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan string konstan "Permintaan Pengembalian Barang" yang
	 * merupakan nama resmi modul ini dalam konteks SOP.
	 *
	 * <b>Return:</b><br>
	 * @return String "Permintaan Pengembalian Barang" sebagai nama istilah modul.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Jika nama modul berubah (misalnya karena perubahan terminologi bisnis),
	 * perbarui string ini. Pertimbangkan untuk menggunakan konstanta statis
	 * atau properti konfigurasi bahasa untuk mendukung multi-bahasa.
	 *
	 * @throws Exception jika terjadi kesalahan yang tidak terduga.
	 */
	@Override
	public String istilah() throws Exception {
		return "Permintaan Pengembalian Barang";
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Mengembalikan entitas data pengembalian aset yang sedang aktif di form,
	 * sebagai implementasi dari antarmuka {@link FormSop}. Digunakan oleh
	 * framework SOP untuk mendapatkan referensi ke entitas yang sedang diproses.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan field {@code pengembalianMasterAsset} yang merupakan entitas
	 * yang sedang diedit atau ditampilkan di form. Field ini diset oleh method
	 * {@code init} atau {@code form} sebelum dipanggil.
	 *
	 * <b>Return:</b><br>
	 * @return Objek {@link PengembalianMasterAsset} yang sedang aktif di form,
	 *         atau null jika belum ada entitas yang diinisialisasi.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Pastikan {@code pengembalianMasterAsset} selalu diinisialisasi sebelum
	 * method ini dipanggil oleh framework SOP, karena null return dapat
	 * menyebabkan NullPointerException di sisi pemanggil.
	 *
	 * @throws Exception jika terjadi kesalahan yang tidak terduga.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return pengembalianMasterAsset;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Mengembalikan kelas Java dari entitas yang dikelola oleh Action ini,
	 * sebagai implementasi dari antarmuka {@link FormSop}. Digunakan oleh
	 * framework SOP untuk refleksi dan identifikasi tipe entitas.
	 *
	 * <b>Cara kerja:</b><br>
	 * Mengembalikan {@code PengembalianMasterAsset.class} secara langsung.
	 * Framework SOP menggunakan informasi ini untuk berbagai keperluan seperti
	 * mengambil konfigurasi SOP yang sesuai dengan tipe entitas ini.
	 *
	 * <b>Return:</b><br>
	 * @return {@code Class} dari {@link PengembalianMasterAsset}.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini tidak perlu diubah kecuali entitas utama yang dikelola
	 * oleh Action ini berubah.
	 *
	 * @throws Exception jika terjadi kesalahan yang tidak terduga.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PengembalianMasterAsset.class;
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Menghasilkan kode nomor surat untuk dokumen pengembalian aset menggunakan
	 * konfigurasi format nomor surat yang telah ditetapkan di
	 * {@link NomorSuratAlurPengadaan#PENGEMBALIAN_BARANG_DATA}.
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika konfigurasi nomor surat tidak tersedia, mengembalikan barcode acak
	 * melalui {@code Common.getGeneratedBarCode()}. Jika tersedia, mengambil
	 * indeks urutan berdasarkan konfigurasi (bisa menggunakan index urut global
	 * atau dihitung dari jumlah data yang ada). Jika {@code tambah} true,
	 * menginkrementasi counter nomor surat melalui
	 * {@code NomorSurat.tambahIndexNomorSurat}. Kode digenerate menggunakan
	 * format yang terkonfigurasi, kemudian divalidasi keunikannya melalui
	 * {@code KodeUnikUtil.pastikanUnik} untuk menghindari duplikat.
	 *
	 * <b>Parameter:</b><br>
	 * @param tambah Jika {@code true}, counter nomor surat diinkrementasi setelah
	 *               kode digenerate (digunakan saat benar-benar menyimpan data baru).
	 *               Jika {@code false}, kode digenerate sebagai preview tanpa
	 *               menginkrementasi counter (digunakan saat menampilkan form kosong).
	 *
	 * <b>Return:</b><br>
	 * @return String kode nomor surat yang unik sesuai format yang terkonfigurasi.
	 *
	 * <b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit. Jika konfigurasi nomor surat tidak
	 * lengkap, akan dikembalikan barcode acak sebagai fallback.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Konfigurasi format nomor surat ({@code PENGEMBALIAN_BARANG_DATA}) harus
	 * ada di database dan di-load saat startup aplikasi. Jika kode duplikat
	 * masih terjadi dalam kondisi konkurensi tinggi, pertimbangkan untuk
	 * menambahkan unique constraint di level database sebagai lapisan keamanan
	 * tambahan.
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA == null
				|| NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PENGEMBALIAN_BARANG_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return ais.action.master.KodeUnikUtil.pastikanUnik(PengembalianMasterAsset.class, noAgenda);
	}

	/**
	 * <b>Tujuan:</b><br>
	 * Menghitung indeks urutan berikutnya untuk nomor surat pengembalian aset
	 * berdasarkan jumlah data yang sudah ada di database, dengan mempertimbangkan
	 * konfigurasi reset urutan (per tahun, per bulan, atau per tanggal tertentu).
	 *
	 * <b>Cara kerja:</b><br>
	 * Jika {@code nomorSurat} null, mengembalikan 0. Mengambil tahun dan bulan
	 * saat ini dari {@link WaktuUtil}. Membangun criteria Hibernate untuk
	 * menghitung jumlah entitas {@link PengembalianMasterAsset} yang sudah ada,
	 * dengan filter bergantung pada konfigurasi nomor surat:
	 * - Jika urutBerdasarkanNomor: filter by nomor surat yang sama.
	 * - Jika urutBerdasarkanKelompok: filter by kelompok nomor surat yang sama.
	 * - Reset tiap tahun: filter hanya data tahun ini.
	 * - Reset tiap bulan: filter hanya data bulan ini.
	 * - Reset tiap tanggal: filter data sejak tanggal reset.
	 * Hasil count ditambah 1 untuk mendapatkan indeks berikutnya.
	 *
	 * <b>Parameter:</b><br>
	 * @param nomorSurat Konfigurasi nomor surat yang menentukan format dan aturan
	 *                   penomoran. Tidak boleh null (sudah dicek di pemanggil).
	 *
	 * <b>Return:</b><br>
	 * @return Indeks urutan berikutnya (jumlah data yang ada + 1), minimal 1.
	 *
	 * <b>Penanganan error:</b><br>
	 * Jika query mengembalikan null, indeks default adalah 1 (bukan 0) untuk
	 * memastikan nomor surat pertama dimulai dari 1, bukan 0.
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini rentan terhadap duplikat dalam kondisi konkurensi (race condition)
	 * karena tidak menggunakan locking database. Untuk lingkungan multi-pengguna
	 * dengan volume penambahan tinggi, pertimbangkan menggunakan sequence database
	 * atau mekanisme distributed lock.
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PengembalianMasterAsset.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)

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
	 * <b>Tujuan:</b><br>
	 * Menyetel flag mode persetujuan pada Action ini sebagai implementasi dari
	 * antarmuka {@link FormSop}. Digunakan oleh framework SOP untuk mengubah
	 * perilaku form setelah instansiasi.
	 *
	 * <b>Cara kerja:</b><br>
	 * Menyimpan nilai {@code persetujuan} ke field instance yang sama. Flag ini
	 * kemudian dibaca oleh method {@code form} untuk menentukan apakah field-field
	 * tertentu ditampilkan sebagai input atau label read-only, dan apakah baris
	 * persetujuan ditampilkan.
	 *
	 * <b>Parameter:</b><br>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan (form
	 *                    sebagian read-only dengan checkbox persetujuan);
	 *                    {@code false} untuk mode pengelolaan data biasa.
	 *
	 * <b>Return:</b><br>
	 * Tidak ada nilai balik (void).
	 *
	 * <b>Pemeliharaan:</b><br>
	 * Method ini harus dipanggil sebelum method {@code form} dipanggil agar
	 * perubahan mode terlihat di tampilan. Jika dipanggil setelah form sudah
	 * dibangun, perubahan tidak akan terlihat tanpa memanggil ulang {@code form}.
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
