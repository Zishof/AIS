package ais.action.master.asset;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.asset.helper.AmbilDataPermintaanPengadaanMasterAssetBanyak;
import ais.action.master.asset.helper.PerjanjianKerjasamaMasterAssetDetailAction;
import ais.action.master.asset.helper.PerjanjianKerjasamaMasterAssetHelper;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jabatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.database.model.asset.JenisPerjanjianKerjasamaAsset;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAssetDetail;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>Untuk apa</h3>
 * Controller utama untuk modul Perjanjian Kerjasama Asset pada sistem manajemen aset institusi.
 * Kelas ini mengelola seluruh siklus hidup perjanjian kerjasama antara institusi dengan penyedia
 * barang/jasa, mencakup berbagai jenis kerjasama seperti BOT (Build-Operate-Transfer), penggunaan
 * bersama, sewa operasional, dan perjanjian pengadaan barang/jasa lainnya. Modul ini menyediakan
 * antarmuka CRUD lengkap (Create, Read, Update, Delete) untuk data perjanjian, termasuk definisi
 * klausul/syarat, alur persetujuan (approval workflow), pemantauan status pelunasan, dan integrasi
 * dengan modul permintaan pengadaan serta pemesanan pengadaan asset.
 *
 * <h3>Cara kerja</h3>
 * Kelas ini mewarisi {@link GenericAutowireComposer} dari framework ZKoss dan mengimplementasikan
 * antarmuka {@link FormSop} untuk mendukung integrasi alur SOP (Standard Operating Procedure).
 * Komponen ZUL di-autowire secara otomatis berdasarkan nama atribut. Alur kerja utama:
 * <ol>
 *   <li>Inisialisasi ({@code doAfterCompose}): memverifikasi sesi pengguna, menyiapkan filter
 *       pencarian, memeriksa hak akses (baca/tulis/hapus/setujui/tolak), lalu memuat data awal.</li>
 *   <li>Pencarian ({@code onSearchDefault}): membangun {@link Criteria} Hibernate dengan filter
 *       multi-dimensi (kode, keterangan, penyedia, lokasi, status lunas, status persetujuan,
 *       satuan kerja) dan menampilkan hasilnya via {@link MyGrid} dengan paginasi.</li>
 *   <li>Tambah/Ubah ({@code onAdd}/{@code init}): membuka popup {@link MyWindow} berisi formulir
 *       lengkap yang dibangun oleh {@code form()}, mencakup data penyedia, jenis kerjasama,
 *       rentang tanggal pekerjaan, daftar item barang/jasa, PPN, dokumen lampiran, dan tautan
 *       ke permintaan pengadaan yang mendasari.</li>
 *   <li>Simpan ({@code onSave}): memvalidasi input, memperbarui/menyimpan entitas utama dan
 *       detail baris, menyinkronkan lampiran via {@link StreamingHibernateUtil}, dan secara
 *       opsional langsung menyetujui perjanjian jika checkbox "Setujui" dicentang.</li>
 *   <li>Persetujuan/Pembatalan: tombol per-baris di renderer mengubah kolom {@code disetujuiOleh}
 *       dan {@code tanggalPersetujuan} pada entitas, kemudian memperbarui visibilitas tombol
 *       aksi secara langsung tanpa reload grid penuh.</li>
 *   <li>Cetak ({@code cetak}): mengumpulkan parameter laporan via {@code parameter()}, termasuk
 *       data SOP, jabatan penandatangan, dan daftar item detail, lalu menghasilkan PDF melalui
 *       {@link Report}.</li>
 *   <li>Pemesanan: setelah perjanjian disetujui, tombol "Pemesanan" muncul untuk membuat
 *       {@link PemesananPengadaanMasterAsset} secara langsung dari data perjanjian ini.</li>
 * </ol>
 *
 * <h3>Threading</h3>
 * Kelas ini mengikuti model threading ZKoss: setiap request HTTP diproses dalam satu thread
 * ZK desktop. Event listener dikeksekusi pada thread ZK yang sama secara sinkron. Operasi
 * database menggunakan {@code HibernateUtil.currentSession()} yang terikat ke thread saat ini
 * (thread-local session). Operasi berat seperti pembuatan PDF dan update status persetujuan
 * dibungkus dalam {@code Common.createDefaultTimer()} agar tidak memblokir event ZK utama.
 * Lampiran file disimpan ke sesi Hibernate terpisah ({@link StreamingHibernateUtil}) untuk
 * menghindari konflik dengan sesi utama.
 *
 * <h3>Pemeliharaan</h3>
 * <ul>
 *   <li>Tambah kolom baru: tambahkan field di entitas {@link PerjanjianKerjasamaMasterAsset},
 *       tambahkan baris form di {@code form()}, tambahkan setter di {@code onSave()}, dan
 *       tambahkan kolom di renderer {@code PerjanjianKerjasamaMasterAssetRenderer}.</li>
 *   <li>Jenis kerjasama baru: cukup tambahkan data di tabel {@code jenis_perjanjian_kerjasama_asset};
 *       kelas ini membaca secara dinamis via {@code Common.insertRadio()}.</li>
 *   <li>Hak akses: dikontrol via {@link CommonPrivilages} dengan konstanta CREATE/UPDATE/DELETE/
 *       APPROVE/REJECT. Perubahan modul keamanan hanya perlu dilakukan di layer {@code CommonPrivilages}.</li>
 *   <li>Template cetak: file JRXML ada di {@code asset/perjanjian_kerjasama}; parameter
 *       dibangun oleh method {@code parameter()} yang bersifat statis dan dapat diuji mandiri.</li>
 * </ul>
 *
 * @author Tim Pengembang AIS
 * @version 2.0
 * @see PerjanjianKerjasamaMasterAsset
 * @see PerjanjianKerjasamaMasterAssetDetail
 * @see PerjanjianKerjasamaMasterAssetHelper
 * @see FormSop
 */
public class PerjanjianKerjasamaMasterAssetAction extends GenericAutowireComposer implements FormSop {

	/**
	 * Nomor versi serial untuk serialisasi kelas ini sesuai kontrak {@link java.io.Serializable}.
	 * Nilai ini tidak boleh diubah kecuali ada perubahan struktur kelas yang tidak kompatibel
	 * dengan versi yang tersimpan sebelumnya.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchketerangan;
	private AmbilDataPenyediaAssetBanbox searchPenyedia;
	private Combobox searchlokasi;
	private Textbox searchcatatan;

	private Label kode;
	private MyTextbox kodeInvoice;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;
	private AmbilDataPenyediaAssetBanbox penyediaAsset;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset;
	private MyToolbarbuttonConfig add;
	private Combobox pemilikAsset;
	private Combobox lokasi;
	private AmbilDataRuangBanbox ruang;
	private MyCheckboxConfig ppn;
	private MyDoublebox persenPpn;
	private Tbmuser tbmuser;
	private boolean persetujuan = false;

	private DisposisiSop disposisiSop = null;

	private MyCheckboxConfig lunasSaja;
	private MyCheckboxConfig blmLunasSaja;

	private MyCheckboxConfig blmDisetujui;
	private MyCheckboxConfig disetujui;

	private boolean tampilkanRuanganDamPemilikAset = Common.bolehKonfigurasi("tampilkanRuanganDamPemilikAset", Konfigurasi.TIDAK_AKTIF);

	private Tabpanel tabJenisKerjasama;

	/**
	 * <b>Tujuan:</b> Menampilkan konten tab "Jenis Kerjasama" secara lazy-load ketika tab tersebut
	 * pertama kali diklik pengguna. Lazy-loading dilakukan untuk menghemat sumber daya server
	 * karena konten sub-modul jenis kerjasama tidak selalu dibutuhkan setiap kali halaman dimuat.
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah {@code tabJenisKerjasama} sudah memiliki komponen anak.
	 * Jika belum ada (ukuran children == 0), maka membuat {@link MyInclude} yang menunjuk ke
	 * halaman ZUL {@code /pages/master/asset/jenis_perjanjian_kerjasama_asset.zul}. Include diatur
	 * agar mengisi seluruh lebar dan tinggi tab panel. Jika sudah ada konten, method tidak
	 * melakukan apa-apa (idempoten) sehingga aman dipanggil berkali-kali.
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK yang dipicu saat tab diklik, biasanya event {@code onSelect} dari
	 *              komponen {@link org.zkoss.zul.Tab}. Nilai event tidak digunakan secara langsung.
	 *
	 * <b>Return:</b> Tidak ada nilai kembalian (void).
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Jika ZUL yang dituju tidak
	 * ditemukan, ZKoss akan melempar {@link org.zkoss.zk.ui.UiException} yang akan ditampilkan
	 * sebagai error di antarmuka pengguna.
	 *
	 * <b>Pemeliharaan:</b> Jika lokasi file ZUL jenis kerjasama dipindah, perbarui path string
	 * di sini. Jika sub-modul jenis kerjasama dihapus, hapus tab dan method ini sekaligus.
	 */
	public void onTampilJenisKerjasama(Event event) {
		if (tabJenisKerjasama.getChildren().size() == 0) {
			MyInclude include = new MyInclude("/pages/master/asset/jenis_perjanjian_kerjasama_asset.zul");
			include.setHeight("100%");
			include.setWidth("100%");
			tabJenisKerjasama.appendChild(include);
		}
	}

	/**
	 * <b>Tujuan:</b> Konstruktor default yang digunakan oleh framework ZKoss ketika membuat
	 * instance controller ini secara otomatis melalui mekanisme autowire saat ZUL dimuat.
	 * Konstruktor ini menginisialisasi referensi pengguna yang sedang login untuk digunakan
	 * di seluruh siklus hidup controller.
	 *
	 * <b>Cara kerja:</b> Memanggil {@link Common#getCurrentUser()} untuk mengambil objek
	 * {@link Tbmuser} dari sesi HTTP aktif dan menyimpannya ke field {@code tbmuser}. Referensi
	 * ini digunakan kemudian untuk mencatat siapa yang menyetujui perjanjian ({@code disetujuiOleh})
	 * dan untuk audit trail pencatatan pembuat data.
	 *
	 * <b>Pemeliharaan:</b> Konstruktor ini harus tetap tanpa parameter agar kompatibel dengan
	 * mekanisme instantiasi ZKoss. Jangan menambahkan logika inisialisasi berat di sini;
	 * gunakan {@code doAfterCompose()} untuk inisialisasi yang memerlukan akses ke komponen ZUL.
	 */
	public PerjanjianKerjasamaMasterAssetAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * <b>Tujuan:</b> Konstruktor dengan parameter yang memungkinkan kelas ini diinstansiasi
	 * dalam mode persetujuan (approval mode). Dalam mode ini, formulir menampilkan data secara
	 * read-only (Label) dan menyembunyikan komponen input yang dapat diubah, sehingga pengguna
	 * hanya dapat menyetujui atau menolak tanpa mengubah isi perjanjian.
	 *
	 * <b>Cara kerja:</b> Mengatur flag {@code persetujuan} ke nilai parameter yang diberikan,
	 * lalu memanggil {@link Common#getCurrentUser()} untuk mendapatkan pengguna aktif. Flag ini
	 * dibaca oleh method {@code form()} untuk memutuskan apakah menampilkan komponen input
	 * ({@link MyTextbox}, {@link MyDatebox}, dll.) atau label read-only ({@link Label}, {@link Html}).
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} jika controller digunakan dalam konteks persetujuan SOP
	 *                    (form hanya baca + tombol setujui/tolak); {@code false} untuk mode
	 *                    edit penuh.
	 *
	 * <b>Pemeliharaan:</b> Konstruktor ini dipanggil oleh framework SOP secara programatik.
	 * Jika ada logika mode persetujuan baru yang perlu diinisialisasi, tambahkan di sini
	 * atau di {@code doAfterCompose()} dengan guard {@code if (persetujuan)}.
	 */
	public PerjanjianKerjasamaMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	/**
	 * <b>Tujuan:</b> Hook lifecycle ZKoss yang dipanggil sebelum proses komposisi komponen ZUL
	 * dimulai. Digunakan untuk memverifikasi hak keamanan pengguna sebelum halaman dirender,
	 * sehingga pengguna yang tidak berhak tidak akan pernah melihat konten halaman ini bahkan
	 * sebentar pun.
	 *
	 * <b>Cara kerja:</b> Memanggil {@link Common#doCheckSecurity()} yang akan memeriksa apakah
	 * pengguna yang sedang login memiliki akses ke URL/modul saat ini. Jika tidak memiliki akses,
	 * {@code doCheckSecurity()} akan melakukan redirect ke halaman error atau login sehingga
	 * eksekusi tidak berlanjut ke {@code doAfterCompose}. Jika akses valid, memanggil
	 * implementasi super untuk melanjutkan proses komposisi normal ZKoss.
	 *
	 * <b>Parameter:</b>
	 * @param page     Halaman ZK saat ini yang sedang dikomposisi.
	 * @param parent   Komponen induk dalam hierarki komponen ZK.
	 * @param compInfo Metadata informasi komponen dari definisi ZUL.
	 * @return {@link org.zkoss.zk.ui.metainfo.ComponentInfo} dari implementasi super, digunakan
	 *         oleh framework ZKoss untuk melanjutkan proses komposisi.
	 *
	 * <b>Penanganan error:</b> Exception yang dilempar oleh {@code doCheckSecurity()} atau
	 * {@code super.doBeforeCompose()} akan ditangani oleh framework ZKoss dan menyebabkan
	 * halaman gagal dimuat dengan pesan error yang sesuai.
	 *
	 * <b>Pemeliharaan:</b> Method ini tidak perlu dimodifikasi. Logika keamanan ada di
	 * {@code Common.doCheckSecurity()}. Override ini hanya ada untuk memastikan pemeriksaan
	 * keamanan dilakukan sebelum ZUL selesai dirender.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Hook lifecycle ZKoss yang dipanggil setelah seluruh komponen ZUL selesai
	 * dikomposisi dan di-wire ke field controller. Method ini merupakan titik inisialisasi utama
	 * untuk menyiapkan seluruh logika UI: verifikasi sesi, konfigurasi komponen pencarian,
	 * penentuan hak akses, inisialisasi paginasi, dan pemuatan data awal.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memvalidasi sesi: jika {@code usersTemp} tidak ada di sesi atau pengguna tidak
	 *       memiliki hak READ, sesi dibersihkan dan pengguna dialihkan ke halaman logoff.</li>
	 *   <li>Mengatur event listener pada {@code searchparent} (satuan kerja) dan
	 *       {@code searchPenyedia} agar setiap perubahan seleksi langsung memicu pencarian ulang
	 *       via {@code onSearchDefault()}.</li>
	 *   <li>Mengisi combobox {@code searchlokasi} dengan semua lokasi aktif, lalu memulihkan
	 *       pilihan lokasi dari atribut sesi jika ada (misalnya dari navigasi balik), kemudian
	 *       mengunci lokasi jika dikonfigurasi demikian via {@link LokasiAction#kunciLokasi}.</li>
	 *   <li>Mengatur visibilitas tombol "Tambah" berdasarkan hak CREATE, dan menyimpan flag
	 *       edit/delete/approve/reject dari {@link CommonPrivilages}.</li>
	 *   <li>Menginisialisasi paginasi dengan event listener yang memuat ulang data saat halaman
	 *       berubah, dan membuat timer default untuk memuat data awal saat halaman pertama tampil.</li>
	 *   <li>Memanggil {@link FilterLanjutHelper#setup} untuk mengaktifkan fitur filter lanjutan.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param comp Komponen root ZUL yang baru selesai dikomposisi, berisi semua komponen anak
	 *             yang sudah di-wire ke field controller ini.
	 * @throws Exception Jika terjadi kesalahan saat inisialisasi komponen, misalnya kesalahan
	 *                   query database untuk mengisi combobox lokasi.
	 *
	 * <b>Penanganan error:</b> Jika sesi tidak valid, method keluar lebih awal dengan redirect
	 * ke logoff. Exception lain dari inisialisasi komponen akan muncul sebagai error ZKoss.
	 *
	 * <b>Pemeliharaan:</b> Tambahkan inisialisasi komponen baru setelah baris {@code FilterLanjutHelper.setup}.
	 * Jangan mengubah urutan pemeriksaan sesi di awal method karena ini adalah garis pertahanan
	 * keamanan pertama setelah {@code doBeforeCompose}.
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

		searchPenyedia.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.insertComboDanSemua(searchlokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (session.getAttribute("Lokasi") != null) {
			Common.selectComboItem(searchlokasi, session.getAttribute("Lokasi"));
			searchlokasi.setDisabled(true);
			session.removeAttribute("Lokasi");
		}
		LokasiAction.kunciLokasi(searchlokasi);

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
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * <b>Untuk apa:</b> Kelas renderer baris grid untuk menampilkan setiap entitas
	 * {@link PerjanjianKerjasamaMasterAsset} sebagai satu baris dalam {@link MyGrid} daftar
	 * perjanjian kerjasama. Setiap baris menampilkan informasi lengkap perjanjian beserta
	 * tombol-tombol aksi kontekstual yang visibilitasnya bergantung pada hak akses pengguna
	 * dan status persetujuan perjanjian tersebut.
	 *
	 * <b>Cara kerja:</b> Mewarisi {@code ais.ui.util.MyRowRenderer} dan mengoverride method
	 * {@code render()} yang dipanggil oleh ZKoss untuk setiap item dalam model data grid.
	 * Setiap pemanggilan {@code render()} menghasilkan satu baris ({@link Row}) dengan kolom-kolom
	 * berikut: detail item ({@link PerjanjianKerjasamaMasterAssetDetailAction}), kode + revisi,
	 * jenis kerjasama, kode invoice, nomor perjanjian, penyedia, pemilik aset, lokasi, ruang,
	 * jenis kerjasama (ulang), uang muka (DP), status lunas, pembuat, penyetuju, rentang tanggal,
	 * PPN, keterangan+SOP, status aktif, dan toolbar aksi.
	 *
	 * <b>Threading:</b> Dipanggil pada thread ZK event yang sama dengan thread request.
	 * Tidak ada akses database tambahan di luar yang sudah dimuat oleh {@code onSearchDefault()}.
	 *
	 * <b>Pemeliharaan:</b> Setiap kolom baru yang ditambahkan di sini harus juga ditambahkan
	 * definisi {@code <column>} di file ZUL terkait agar jumlah kolom header dan sel data sesuai.
	 * Visibilitas tombol aksi dikontrol oleh flag {@code edit}, {@code delete}, {@code approve},
	 * {@code reject} dari outer class.
	 */
	class PerjanjianKerjasamaMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data perjanjian kerjasama ke dalam komponen
		 * {@link Row} ZKoss, menampilkan semua kolom informasi dan tombol aksi yang relevan
		 * untuk pengguna saat ini.
		 *
		 * <b>Cara kerja:</b> Melakukan cast {@code arg1} ke {@link PerjanjianKerjasamaMasterAsset},
		 * lalu membuat dan melampirkan komponen ZK ke {@code arg0} (baris grid) secara berurutan
		 * sesuai susunan kolom yang didefinisikan di ZUL. Tombol-tombol aksi (cetak, setujui,
		 * batalkan persetujuan, ubah, hapus, batalkan pemesanan, buat pemesanan) dibuat dengan
		 * event listener yang menutup (closure) referensi ke entitas saat ini.
		 * Status aktif dapat diubah langsung via checkbox inline jika pengguna memiliki hak
		 * edit dan perjanjian belum disetujui; jika sudah disetujui, status aktif ditampilkan
		 * sebagai label.
		 *
		 * <b>Parameter:</b>
		 * @param arg0 Baris grid ({@link Row}) yang akan diisi dengan komponen-komponen sel data.
		 *             Sudah diatur {@code valign="top"} agar konten multi-baris terlihat rapi.
		 * @param arg1 Objek data dari model list, harus berupa instance
		 *             {@link PerjanjianKerjasamaMasterAsset} yang valid.
		 * @throws Exception Jika terjadi kesalahan saat membuat komponen ZK atau mengakses
		 *                   properti entitas (misalnya properti lazy yang sudah detached).
		 *
		 * <b>Return:</b> Void; hasil render langsung tersimpan dalam hierarki komponen ZK
		 * melalui {@code setParent()} pada setiap komponen yang dibuat.
		 *
		 * <b>Pemeliharaan:</b> Urutan komponen yang di-append ke {@code arg0} harus selalu
		 * sinkron dengan urutan tag {@code <column>} di file ZUL. Jika ada kolom yang
		 * ditambah/dihapus, perbarui keduanya secara bersamaan.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset = (PerjanjianKerjasamaMasterAsset) arg1;

			(new PerjanjianKerjasamaMasterAssetDetailAction(perjanjianKerjasamaMasterAsset)).setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PerjanjianKerjasamaMasterAsset.class, perjanjianKerjasamaMasterAsset,
					perjanjianKerjasamaMasterAsset.getKode())).setParent(arg0);

			a.appendChild(new Label(perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset() == null ? ""
					: perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset().getNama()));

			a.appendChild(new Label(perjanjianKerjasamaMasterAsset.getKodeInvoice()));

			a.appendChild(new Label(perjanjianKerjasamaMasterAsset.getNomorPerjanjianKerjasama()));

			new Label(perjanjianKerjasamaMasterAsset.getPenyedia() == null ? ""
					: perjanjianKerjasamaMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(perjanjianKerjasamaMasterAsset.getPemilikAsset() == null ? ""
					: perjanjianKerjasamaMasterAsset.getPemilikAsset().getNama()).setParent(arg0);

			new Label(perjanjianKerjasamaMasterAsset.getLokasi() == null ? ""
					: (perjanjianKerjasamaMasterAsset.getLokasi().getNama())).setParent(arg0);

			new Label(perjanjianKerjasamaMasterAsset.getRuang() == null ? ""
					: perjanjianKerjasamaMasterAsset.getRuang().getNama()).setParent(arg0);

			new Label(perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset() == null ? ""
					: perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(perjanjianKerjasamaMasterAsset.getDp())).setParent(arg0);

			new Label(perjanjianKerjasamaMasterAsset.getLunas() ? "Ya" : "Tidak").setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(perjanjianKerjasamaMasterAsset.getDibuatOleh() == null ? ""
					: perjanjianKerjasamaMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(perjanjianKerjasamaMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(perjanjianKerjasamaMasterAsset.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null ? ""
					: perjanjianKerjasamaMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(perjanjianKerjasamaMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(perjanjianKerjasamaMasterAsset.getTanggalPersetujuan()))).setParent(a);

			new Label((perjanjianKerjasamaMasterAsset.getPengirimanMulai() == null ? ""
					: Common.dateFormat1.get().format(perjanjianKerjasamaMasterAsset.getPengirimanMulai()))
					+ " sd "
					+ (perjanjianKerjasamaMasterAsset.getPengirimanPalingLambat() == null ? ""
							: Common.dateFormat1.get().format(perjanjianKerjasamaMasterAsset.getPengirimanPalingLambat())))
					.setParent(arg0);

			new Label(!perjanjianKerjasamaMasterAsset.getPpn() ? ""
					: Common.numberFormat.get().format(perjanjianKerjasamaMasterAsset.getPersenPpn()) + "%").setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(perjanjianKerjasamaMasterAsset.getKeterangan())).setParent(vbox1);
			if (perjanjianKerjasamaMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A(
						"SOP " + Common.simpleString(perjanjianKerjasamaMasterAsset.getDisposisiSop().getKeterangan())
								+ " (" + perjanjianKerjasamaMasterAsset.getDisposisiSop().getSop().getNama() + ")"))
						.setParent(vbox1);
				aa.setStyle("font-size:9px;");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(perjanjianKerjasamaMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			if (perjanjianKerjasamaMasterAsset.getDisposisiSop() != null
					&& !perjanjianKerjasamaMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(perjanjianKerjasamaMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						perjanjianKerjasamaMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(perjanjianKerjasamaMasterAsset);
					}
				});
			} else {
				new Label(perjanjianKerjasamaMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Kerjasama Pengadaan Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					cetak(perjanjianKerjasamaMasterAsset);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Kerjasama Pengadaan Asset ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();
										Integer countMasterAssetjumlah = ((Number) session
												.createCriteria(PerjanjianKerjasamaMasterAssetDetail.class)
												.setProjection(Projections.count("id"))
												.add(Restrictions.eq("perjanjianKerjasamaMasterAsset",
														perjanjianKerjasamaMasterAsset))
												.add(Restrictions.lt("jumlah", 1.0)).uniqueResult()).intValue();

										if (!countMasterAssetjumlah.equals(0)) {
											MyMessageboxConfig.show("Mohon maaf, terdapat baris item kerjasama yang belum memiliki jumlah yang valid. Langkah yang dapat dilakukan: (1) Periksa setiap baris pada daftar barang/jasa kerjasama; (2) Isi jumlah pada baris yang masih kosong atau bernilai 0; (3) ulangi proses persetujuan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											return;
										}

										perjanjianKerjasamaMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										perjanjianKerjasamaMasterAsset
												.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, perjanjianKerjasamaMasterAsset);

										disetujuiTanggal.setValue(
												perjanjianKerjasamaMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(perjanjianKerjasamaMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null
												? ""
												: perjanjianKerjasamaMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(
												edit && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);

										cetak(perjanjianKerjasamaMasterAsset);
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event event) throws Exception {

												onSearchDefault(event);
											}
										});
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

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Kerjasama Barang/Jasa ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										perjanjianKerjasamaMasterAsset.setDisetujuiOleh(null);
										perjanjianKerjasamaMasterAsset.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, perjanjianKerjasamaMasterAsset);

										disetujuiTanggal.setValue(
												perjanjianKerjasamaMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(perjanjianKerjasamaMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null
												? ""
												: perjanjianKerjasamaMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(
												edit && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event event) throws Exception {

												onSearchDefault(event);
											}
										});
									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(perjanjianKerjasamaMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && perjanjianKerjasamaMasterAsset.getDisetujuiOleh() == null);
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
													perjanjianKerjasamaMasterAsset.getDisposisiSop())) {
												List<PerjanjianKerjasamaMasterAssetDetail> perjanjianKerjasamaMasterAssetDetails = session
														.createCriteria(PerjanjianKerjasamaMasterAssetDetail.class)
														.add(Restrictions.eq("perjanjianKerjasamaMasterAsset",
																perjanjianKerjasamaMasterAsset))
														.list();
												for (PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail : perjanjianKerjasamaMasterAssetDetails) {
													session.delete(perjanjianKerjasamaMasterAssetDetail);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, perjanjianKerjasamaMasterAsset);
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

			if (perjanjianKerjasamaMasterAsset.getPemesananPengadaanMasterAsset() != null
					&& perjanjianKerjasamaMasterAsset.getPemesananPengadaanMasterAsset().getPembelianLangsung()
					&& perjanjianKerjasamaMasterAsset.getPemesananPengadaanMasterAsset().getPostingHistory() == null
					&& perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null && edit) {
				button = new MyToolbarbuttonConfig("Batalkan Pemesanan", "/img/svg/trash.svg");
				button.setTooltiptext("Batalkan Pemesanan");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						MyMessageboxConfig.show("Apakah yakin ingin membatalkan pemesanan ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();
												final PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = perjanjianKerjasamaMasterAsset
														.getPemesananPengadaanMasterAsset();
												session.refresh(pemesananPengadaanMasterAsset);

												List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
														.createCriteria(PemesananPengadaanMasterAssetDetail.class)
														.add(Restrictions.eq("pemesananPengadaanMasterAsset",
																pemesananPengadaanMasterAsset))
														.list();
												for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {
													session.delete(pemesananPengadaanMasterAssetDetail);
												}

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														Session session = HibernateUtil.currentSession();

														perjanjianKerjasamaMasterAsset
																.setPemesananPengadaanMasterAsset(null);
														Common.refreshUpdate(session, perjanjianKerjasamaMasterAsset);
														session.flush();

														Common.refreshDelete(session, pemesananPengadaanMasterAsset);
														session.flush();

														onSearchDefault(event);
													}
												});
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
				aksiButtons.add(button);
			}

			if (perjanjianKerjasamaMasterAsset.getPemesananPengadaanMasterAsset() == null
					&& perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null && edit
					&& perjanjianKerjasamaMasterAsset.getPermintaanPengadaanMasterAssets() != null
					&& !perjanjianKerjasamaMasterAsset.getPermintaanPengadaanMasterAssets().trim().isEmpty()) {
				button = new MyToolbarbuttonConfig("Pemesanan", "/img/svg/cash.svg");
				button.setTooltiptext("Ubah Data");
				button.addEventListener("onClick", new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = new PemesananPengadaanMasterAsset();
						pemesananPengadaanMasterAsset.setPemilikAsset(perjanjianKerjasamaMasterAsset.getPemilikAsset());

						String s = "";
						String a = "";

						List<Long> ids = new ArrayList<Long>();
						for (String iddata : perjanjianKerjasamaMasterAsset.getPermintaanPengadaanMasterAssets()
								.split(",")) {
							try {
								if (!iddata.trim().isEmpty()) {
									ids.add(Long.parseLong(iddata.trim()));
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PerjanjianKerjasamaMasterAssetAction.java:907");
								// TODO: handle exception
							}
						}

						Session session = HibernateUtil.currentSession();
						List<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssetDetails = session
								.createCriteria(PermintaanPengadaanMasterAssetDetail.class).addOrder(Order.desc("id"))
								.add(ids.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", ids))
								.list();

						SatuanKerja satuanKerja = null;
						Workspace workspace = null;
						for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : permintaanPengadaanMasterAssetDetails) {

							satuanKerja = permintaanPengadaanMasterAssetDetail.getSatuanKerja();
							workspace = permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
									.getWorkspace();

							s += s.isEmpty() ? permintaanPengadaanMasterAssetDetail.getId().toString()
									: "," + permintaanPengadaanMasterAssetDetail.getId();

							if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
									.getWorkspace() != null) {
								a += a.isEmpty()
										? permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
												.getWorkspace().getId().toString()
										: "," + permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
												.getWorkspace().getId();
							}
						}

						pemesananPengadaanMasterAsset.setPermintaanPengadaanMasterAssets(
								perjanjianKerjasamaMasterAsset.getPermintaanPengadaanMasterAssets());
						pemesananPengadaanMasterAsset.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAsset);
						pemesananPengadaanMasterAsset.setSatuanKerja(satuanKerja);
						pemesananPengadaanMasterAsset.setPermintaanPengadaanMasterAssets(s);
						pemesananPengadaanMasterAsset.setAngarans(a);
						pemesananPengadaanMasterAsset.setDp(pemesananPengadaanMasterAsset.getNilai());
						pemesananPengadaanMasterAsset.setRuang(pemesananPengadaanMasterAsset.getRuang());
						pemesananPengadaanMasterAsset.setLokasi(pemesananPengadaanMasterAsset.getLokasi());
						pemesananPengadaanMasterAsset.setWorkspace(workspace);

						pemesananPengadaanMasterAsset.setPembelianLangsung(true);
						pemesananPengadaanMasterAsset.setKeterangan("Pemesanan dari Perjanjian Kerjasama");

						PemesananPengadaanMasterAssetAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pemesananPengadaanMasterAsset, true);
					}

				});
				aksiButtons.add(button);
			}

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar halaman, memulai proses
	 * pembuatan perjanjian kerjasama baru dengan membuka formulir input dalam mode tambah.
	 *
	 * <b>Cara kerja:</b> Membuat instance {@link PerjanjianKerjasamaMasterAsset} baru (kosong,
	 * tanpa ID), kemudian memanggil {@code init()} untuk menyiapkan formulir dan mengisi field
	 * controller dengan nilai-nilai default. Setelah {@code init()} selesai membangun konten
	 * jendela, {@code addWindow} ditampilkan sebagai modal dialog agar pengguna fokus pada
	 * formulir sebelum melanjutkan aktivitas lain.
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK yang dipicu oleh klik tombol "Tambah" (onClick). Nilai event
	 *              tidak digunakan secara langsung dalam method ini.
	 * @throws Exception Jika terjadi kesalahan selama inisialisasi formulir di {@code init()},
	 *                   misalnya kegagalan query database untuk mengisi combobox.
	 *
	 * <b>Return:</b> Void.
	 *
	 * <b>Pemeliharaan:</b> Jika ada nilai default yang perlu diisi saat membuat perjanjian baru
	 * (misalnya default penyedia atau lokasi), tambahkan setter pada objek baru sebelum
	 * memanggil {@code init()}.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PerjanjianKerjasamaMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan:</b> Menyiapkan dan membangun konten {@code addWindow} (jendela modal) untuk
	 * operasi tambah atau ubah perjanjian kerjasama. Method ini merupakan titik inisialisasi
	 * sentral yang menghubungkan antarmuka formulir dengan logika bisnis simpan dan batal.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyimpan referensi entitas ke field instance {@code this.perjanjianKerjasamaMasterAsset}.</li>
	 *   <li>Mengatur judul jendela: "Tambah Perjanjian Kerjasama" jika ID null (mode tambah),
	 *       atau "Ubah Perjanjian Kerjasama" jika ID sudah ada (mode edit).</li>
	 *   <li>Membersihkan konten jendela sebelumnya dengan {@code Common.clear(addWindow)}.</li>
	 *   <li>Membangun layout {@link Borderlayout}: area Center berisi formulir yang dihasilkan
	 *       oleh {@code form()}, area South berisi toolbar dengan tombol "Batal" dan "Simpan".</li>
	 *   <li>Tombol "Batal" menyembunyikan jendela tanpa menyimpan perubahan.</li>
	 *   <li>Tombol "Simpan" memanggil {@code onSave()}, dan jika berhasil (return {@code true}),
	 *       menutup jendela, memperbarui paginasi, dan memanggil {@code onSearchDefault()} untuk
	 *       menyegarkan daftar.</li>
	 *   <li>Me-reset {@code disposisiSop} ke null sebelum membangun formulir karena entitas
	 *       baru tidak memiliki SOP yang terkait.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param perjanjianKerjasamaMasterAsset Entitas yang akan diedit. Jika {@code getId() == null},
	 *        formulir berada dalam mode tambah; jika {@code getId() != null}, dalam mode ubah
	 *        dengan data yang sudah ada dipra-isi ke komponen formulir.
	 * @throws Exception Jika terjadi kesalahan saat membangun formulir, termasuk kegagalan
	 *                   query database atau kesalahan inisialisasi komponen ZK.
	 *
	 * <b>Return:</b> Void. Hasil berupa modifikasi langsung pada konten {@code addWindow}.
	 *
	 * <b>Pemeliharaan:</b> Jika perlu menambahkan tombol aksi baru di toolbar (misalnya "Pratinjau"),
	 * tambahkan di bagian South setelah tombol "Batal" dan sebelum tombol "Simpan".
	 */
	private void init(final PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset) throws Exception {

		this.perjanjianKerjasamaMasterAsset = perjanjianKerjasamaMasterAsset;
		addWindow.setTitle(perjanjianKerjasamaMasterAsset.getId() == null ? "Tambah Perjanjian Kerjasama" : "Ubah Perjanjian Kerjasama");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(perjanjianKerjasamaMasterAsset, disposisiSop, save, null));

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
		borderlayout.setParent(addWindow);

	}

	/**
	 * <b>Tujuan:</b> Memvalidasi seluruh input formulir perjanjian kerjasama dan menyimpan
	 * data ke database jika semua validasi lulus. Method ini menangani baik operasi INSERT
	 * (perjanjian baru) maupun UPDATE (perjanjian yang sudah ada), termasuk menyimpan detail
	 * baris item, memperbarui data permintaan pengadaan terkait, dan mengunggah lampiran file.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li><b>Validasi:</b> Memeriksa kode kerjasama tidak kosong, penyedia dipilih, jenis
	 *       kerjasama dipilih, tanggal mulai dan selesai pekerjaan diisi, serta semua baris
	 *       detail memiliki data barang. Jika ada yang gagal, menampilkan pesan peringatan
	 *       dan mengembalikan {@code false}.</li>
	 *   <li><b>Load entitas:</b> Jika mode edit, me-reload entitas dari database menggunakan
	 *       {@code session.load()} untuk mendapatkan proxy yang terikat ke sesi aktif.</li>
	 *   <li><b>Mapping field:</b> Menyalin semua nilai dari komponen UI ke field entitas:
	 *       nomor perjanjian, tanggal pembuatan, kode, keterangan, penyedia, lokasi, pemilik
	 *       aset, ruang, PPN/persentase PPN, jenis kerjasama, tanggal pekerjaan, DP, catatan
	 *       kesepakatan, kode invoice, formula dokumen, dan ID permintaan pengadaan terkait.</li>
	 *   <li><b>Simpan entitas utama:</b> INSERT jika baru (menghasilkan kode baru), UPDATE
	 *       jika sudah ada.</li>
	 *   <li><b>Simpan detail baris:</b> Iterasi semua baris di {@code gridMasterAsset},
	 *       simpan/update setiap {@link PerjanjianKerjasamaMasterAssetDetail} dan sinkronkan
	 *       kembali ke {@link PermintaanPengadaanMasterAssetDetail} terkait.</li>
	 *   <li><b>Sinkronisasi permintaan pengadaan:</b> Untuk setiap detail permintaan pengadaan
	 *       yang dipilih, cek apakah sudah ada entri detail kerjasama; jika belum, buat baru.</li>
	 *   <li><b>Lampiran:</b> Update field {@code ref} pada setiap {@link LampiranLain} baru
	 *       ke ID perjanjian yang baru disimpan, menggunakan sesi streaming terpisah.</li>
	 *   <li><b>Persetujuan opsional:</b> Menggunakan timer default untuk memeriksa checkbox
	 *       {@code setujui}; jika dicentang, langsung mengisi {@code disetujuiOleh} dan
	 *       {@code tanggalPersetujuan}, lalu mencetak PDF perjanjian.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK dari klik tombol "Simpan". Tidak digunakan langsung dalam method ini.
	 * @return {@code true} jika data berhasil disimpan dan jendela boleh ditutup;
	 *         {@code false} jika validasi gagal dan jendela harus tetap terbuka.
	 * @throws Exception Jika terjadi kesalahan database yang tidak tertangkap selama operasi
	 *                   simpan (misalnya constraint violation, timeout koneksi).
	 *
	 * <b>Penanganan error:</b> Validasi input ditangani dengan pesan kotak dialog. Kesalahan
	 * lampiran file ditangani dengan try-catch dan rollback pada sesi streaming. Kesalahan
	 * database lain akan muncul sebagai exception yang ditampilkan oleh framework ZKoss.
	 *
	 * <b>Pemeliharaan:</b> Setiap field baru di entitas harus ditambahkan: (1) validasi jika
	 * wajib isi, (2) mapping dari komponen UI ke entitas sebelum blok simpan.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Kerjasama belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Kerjasama atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (penyediaAsset.getAttribute("penyediaAsset") == null) {
			MyMessageboxConfig.show("Mohon maaf, Penyedia belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Penyedia dan cari penyedia dari daftar; (2) Jika penyedia belum terdaftar, daftarkan terlebih dahulu melalui menu Data Penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if ((jenisPerjanjianKerjasamaAsset.getSelectedItem() == null ? null
				: jenisPerjanjianKerjasamaAsset.getSelectedItem().getAttribute("value")) == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Kerjasama belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis kerjasama dari dropdown yang tersedia; (2) Jika jenis belum ada, tambahkan melalui menu Jenis Perjanjian Kerjasama; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (pengirimanMulai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Pekerjaan Mulai belum diisi. Langkah yang dapat dilakukan: (1) Klik field Pekerjaan Mulai dan pilih tanggal dari kalender; (2) Pastikan tanggal mulai tidak melebihi tanggal selesai; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (pengirimanPalingLambat.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Pekerjaan Sampai belum diisi. Langkah yang dapat dilakukan: (1) Klik field Pekerjaan Sampai dan pilih tanggal dari kalender; (2) Pastikan tanggal selesai tidak lebih awal dari tanggal mulai; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = (PerjanjianKerjasamaMasterAssetDetail) row
					.getAttribute("perjanjianKerjasamaMasterAssetDetail");
			if (perjanjianKerjasamaMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar kerjasama belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih barang/jasa dari daftar master aset; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();

		if (perjanjianKerjasamaMasterAsset.getId() != null) {
			perjanjianKerjasamaMasterAsset = (PerjanjianKerjasamaMasterAsset) session
					.load(PerjanjianKerjasamaMasterAsset.class, perjanjianKerjasamaMasterAsset.getId());

		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			perjanjianKerjasamaMasterAsset.setDisposisiSop(disposisiSop);
		}

		perjanjianKerjasamaMasterAsset.setNomorPerjanjianKerjasama(nomorPerjanjianKerjasama.getValue());
		perjanjianKerjasamaMasterAsset.setPengirimanMulai(pengirimanMulai.getValue());
		perjanjianKerjasamaMasterAsset.setKode(kode.getValue());
		perjanjianKerjasamaMasterAsset.setKeterangan(keterangan.getValue());
		perjanjianKerjasamaMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());

		perjanjianKerjasamaMasterAsset.setPenyedia((PenyediaAsset) penyediaAsset.getAttribute("penyediaAsset"));

		perjanjianKerjasamaMasterAsset.setLokasi(
				(Lokasi) (lokasi.getSelectedItem() == null || lokasi.getSelectedItem().getValue() == null ? null
						: lokasi.getSelectedItem().getValue()));
		perjanjianKerjasamaMasterAsset.setPemilikAsset((PemilikAsset) (pemilikAsset.getSelectedItem() == null
				|| pemilikAsset.getSelectedItem().getValue() == null ? null
						: pemilikAsset.getSelectedItem().getValue()));
		perjanjianKerjasamaMasterAsset.setRuang((Ruang) ruang.getAttribute("ruang"));

		perjanjianKerjasamaMasterAsset.setPpn(ppn.isChecked());
		perjanjianKerjasamaMasterAsset.setPersenPpn(persenPpn.getValue());
		perjanjianKerjasamaMasterAsset.setJenisPerjanjianKerjasamaAsset(
				(JenisPerjanjianKerjasamaAsset) (jenisPerjanjianKerjasamaAsset.getSelectedItem() == null ? null
						: jenisPerjanjianKerjasamaAsset.getSelectedItem().getAttribute("value")));

		perjanjianKerjasamaMasterAsset.setPengirimanPalingLambat(pengirimanPalingLambat.getValue());

		perjanjianKerjasamaMasterAsset.setDp(dp.getValue());
		perjanjianKerjasamaMasterAsset.setCatatanKesepakatan(catatanKesepakatan.getValue());

		perjanjianKerjasamaMasterAsset.setKodeInvoice(kodeInvoice.getValue().trim());

		perjanjianKerjasamaMasterAsset.setFormula(array.toString());

		String s = "";
		String a = "";
		for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets) {
			s += s.isEmpty() ? permintaanPengadaanMasterAssetDetail.getId().toString()
					: "," + permintaanPengadaanMasterAssetDetail.getId();

			if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace() != null) {
				a += a.isEmpty()
						? permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace()
								.getId().toString()
						: "," + permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset().getWorkspace()
								.getId();
			}
		}

		perjanjianKerjasamaMasterAsset.setPermintaanPengadaanMasterAssets(s);
		perjanjianKerjasamaMasterAsset.setAngarans(a);

		if (perjanjianKerjasamaMasterAsset.getId() != null) {
			session.update(perjanjianKerjasamaMasterAsset);
		} else {
			perjanjianKerjasamaMasterAsset.setDibuatOleh(Common.getCurrentUser());
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			perjanjianKerjasamaMasterAsset.setKode(kode.getValue());
			session.save(perjanjianKerjasamaMasterAsset);

		}

		for (Row row : rowsMasterAsset) {
			PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = (PerjanjianKerjasamaMasterAssetDetail) row
					.getAttribute("perjanjianKerjasamaMasterAssetDetail");
			perjanjianKerjasamaMasterAssetDetail.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAsset);
			session.saveOrUpdate(perjanjianKerjasamaMasterAssetDetail);
			session.flush();

			PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail = perjanjianKerjasamaMasterAssetDetail
					.getPermintaanPengadaanMasterAssetDetail();
			if (permintaanPengadaanMasterAssetDetail != null) {
				session.refresh(permintaanPengadaanMasterAssetDetail);
				permintaanPengadaanMasterAssetDetail
						.setPerjanjianKerjasamaMasterAssetDetail(perjanjianKerjasamaMasterAssetDetail);
				session.saveOrUpdate(permintaanPengadaanMasterAssetDetail);
				session.flush();
			}
		}

		for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets) {
			PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = (PerjanjianKerjasamaMasterAssetDetail) session
					.createCriteria(PerjanjianKerjasamaMasterAssetDetail.class)
					.add(Restrictions.eq("perjanjianKerjasamaMasterAsset", perjanjianKerjasamaMasterAsset))
					.add(Restrictions.eq("permintaanPengadaanMasterAssetDetail", permintaanPengadaanMasterAssetDetail))
					.uniqueResult();
			if (perjanjianKerjasamaMasterAssetDetail == null) {
				perjanjianKerjasamaMasterAssetDetail = new PerjanjianKerjasamaMasterAssetDetail();
				perjanjianKerjasamaMasterAssetDetail
						.setMasterAsset(permintaanPengadaanMasterAssetDetail.getMasterAsset());
				perjanjianKerjasamaMasterAssetDetail
						.setPermintaanPengadaanMasterAssetDetail(permintaanPengadaanMasterAssetDetail);
				perjanjianKerjasamaMasterAssetDetail.setJumlah(permintaanPengadaanMasterAssetDetail.getJumlah());
				perjanjianKerjasamaMasterAssetDetail
						.setKeterangan(permintaanPengadaanMasterAssetDetail.getKeterangan());
				perjanjianKerjasamaMasterAssetDetail.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAsset);

				session.save(perjanjianKerjasamaMasterAssetDetail);
				session.flush();
			}

		}

		if (lampiranLains != null && !lampiranLains.isEmpty()) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();
				for (LampiranLain lampiranLain : lampiranLains) {
					session.refresh(lampiranLain);
					lampiranLain.setRef(perjanjianKerjasamaMasterAsset.getId());

					session.getTransaction().begin();
					session.update(lampiranLain);
					session.getTransaction().commit();
				}
				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {

				if (setujui.isChecked()) {
					Session session = HibernateUtil.currentSession();
					perjanjianKerjasamaMasterAsset.setDisetujuiOleh(tbmuser);
					perjanjianKerjasamaMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
					Common.refreshUpdate(session, perjanjianKerjasamaMasterAsset);
				} else {
					Session session = HibernateUtil.currentSession();
					perjanjianKerjasamaMasterAsset.setDisetujuiOleh(null);
					perjanjianKerjasamaMasterAsset.setTanggalPersetujuan(null);
					Common.refreshUpdate(session, perjanjianKerjasamaMasterAsset);
				}

				cetak(perjanjianKerjasamaMasterAsset);
			}
		});

		return true;
	}

	/**
	 * <b>Tujuan:</b> Mengumpulkan dan membangun seluruh parameter yang dibutuhkan oleh template
	 * laporan JasperReports/JRXML untuk mencetak dokumen perjanjian kerjasama dalam format PDF.
	 * Method ini bersifat statis sehingga dapat dipanggil tanpa instance controller aktif,
	 * misalnya dari proses batch atau modul laporan lain.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Me-refresh entitas dari database untuk memastikan data terkini sebelum dicetak.</li>
	 *   <li>Membuat {@link Map} parameter acak (via {@code HashMapGenerator.getRand()}) dan
	 *       mengisi parameter {@code id} serta semua properti entitas via {@code Common.insertProperty()}
	 *       dengan prefix {@code "data"}.</li>
	 *   <li>Menambahkan data bank penyedia: mem-parse JSON array dari {@code penyedia.getBank()}
	 *       dan mengisi parameter dengan pola {@code "bank_N.key"} untuk setiap entri bank.</li>
	 *   <li>Menambahkan data dokumen penyedia: untuk setiap {@link DokumenPenyediaAsset} aktif,
	 *       mencari {@link PenyediaAssetPunyaDokumen} terkait dan mengisi parameter
	 *       {@code "dokumen.NamaDokumen"}.</li>
	 *   <li>Menambahkan data SOP: jika perjanjian memiliki {@code disposisiSop}, mengambil
	 *       semua {@link DisposisiAlurSop} dan mengisi parameter per alur SOP mencakup: aktor,
	 *       nama pengaju, satuan kerja pengaju, catatan, tanggal, dan jabatan (fungsional,
	 *       struktural, dan umum) dari data kepegawaian pengaju.</li>
	 *   <li>Mengisi daftar detail item sebagai {@code List<Map>} dengan key {@code "maps"},
	 *       di mana setiap item berisi: kelompok aset, jenis aset, tipe, spesifikasi, PPN,
	 *       harga potongan, harga beli, jumlah, nama, kode, ISBN, penyedia, status persetujuan,
	 *       dan tanggal persetujuan.</li>
	 *   <li>Menghapus parameter yang mengandung referensi objek {@code disposisiSop} untuk
	 *       mencegah serialisasi objek kompleks ke JasperReports.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param perjanjianKerjasamaMasterAsset Entitas perjanjian kerjasama yang akan dicetak.
	 *        Boleh memiliki ID null tetapi dalam praktiknya harus sudah tersimpan agar
	 *        detail baris dapat dimuat dari database.
	 * @return {@link Map} berisi semua parameter siap pakai untuk template laporan JasperReports.
	 *         Kunci parameter menggunakan konvensi: {@code "data.namaGetter"} untuk properti
	 *         entitas, {@code "bank_N.key"} untuk data bank, {@code "dokumen.nama"} untuk
	 *         dokumen penyedia, {@code "aktor_sop_ID"} dst. untuk data SOP, dan {@code "maps"}
	 *         untuk daftar detail item.
	 *
	 * <b>Penanganan error:</b> Parsing JSON bank penyedia dibungkus try-catch silent (diabaikan
	 * jika gagal). Lookup database dalam method ini menggunakan sesi Hibernate saat ini dan
	 * tidak menangani exception secara eksplisit.
	 *
	 * <b>Pemeliharaan:</b> Jika template laporan ditambah parameter baru, tambahkan entri
	 * {@code parameters.put("key", value)} di method ini. Pastikan kunci konsisten antara
	 * kode Java dan file JRXML.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset) {
		if (perjanjianKerjasamaMasterAsset != null && perjanjianKerjasamaMasterAsset.getId() != null) {
			HibernateUtil.currentSession().refresh(perjanjianKerjasamaMasterAsset);
		}
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", perjanjianKerjasamaMasterAsset.getId());

		DisposisiAlurSop.parameterMap(perjanjianKerjasamaMasterAsset.getDisposisiSop(), parameters);

		Common.insertProperty(PerjanjianKerjasamaMasterAsset.class, perjanjianKerjasamaMasterAsset, parameters, "data");

		if (perjanjianKerjasamaMasterAsset.getPenyedia() != null) {

			try {
				JSONArray array = new JSONArray(perjanjianKerjasamaMasterAsset.getPenyedia().getBank());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Iterator<String> iter = jsonObject.keys();
					while (iter.hasNext()) {
						String key = iter.next();
						parameters.put("bank_" + i + "." + key, jsonObject.get(key));
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PerjanjianKerjasamaMasterAssetAction.java:1397");
				// TODO: handle exception
			}

			Map<Long, DokumenPenyediaAsset> map = ConstantValues.ambilBerdasarClass(DokumenPenyediaAsset.class);
			List<DokumenPenyediaAsset> dokumenPenyediaAssets = new ArrayList<DokumenPenyediaAsset>();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : map.values()) {
				dokumenPenyediaAssets.add(dokumenPenyediaAsset);
			}
			PenyediaAsset penyediaAsset = perjanjianKerjasamaMasterAsset.getPenyedia();
			Session session = HibernateUtil.currentSession();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : dokumenPenyediaAssets) {

				PenyediaAssetPunyaDokumen temp = (PenyediaAssetPunyaDokumen) (penyediaAsset == null
						|| penyediaAsset.getId() == null
								? new PenyediaAssetPunyaDokumen()
								: session.createCriteria(PenyediaAssetPunyaDokumen.class)
										.add(Restrictions.eq("dokumenPenyediaAsset", dokumenPenyediaAsset))
										.add(Restrictions.eq("penyediaAsset", penyediaAsset)).setMaxResults(1)
										.uniqueResult());

				parameters.put("dokumen." + dokumenPenyediaAsset.getNama(), temp == null ? "" : temp.getKeterangan());
			}

		}

		Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();

		Session session = HibernateUtil.currentSession();
		Date sekarang = WaktuUtil.getDate();
		if (perjanjianKerjasamaMasterAsset.getDisposisiSop() != null) {
			List<DisposisiAlurSop> disposisiAlurSops = session.createCriteria(DisposisiAlurSop.class)
					.add(Restrictions.isNotNull("alurSop")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("disposisiSop", perjanjianKerjasamaMasterAsset.getDisposisiSop())).list();

			for (DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {
				parameters.put("aktor_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getAlurSop().getAktor());
				parameters.put("oleh_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getDiajukanOleh() == null ? ""
								: disposisiAlurSop.getDiajukanOleh().getUserNama());

				parameters.put("satuan_kerja_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getDiajukanOleh() == null
								|| disposisiAlurSop.getDiajukanOleh().ambilSatuanKerja() == null ? ""
										: disposisiAlurSop.getDiajukanOleh().ambilSatuanKerja().getNama());

				parameters.put("catatan_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getKeterangan());
				parameters.put("tanggal_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat6.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("tgl_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat2.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("waktu_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat51.get().format(disposisiAlurSop.getWaktu()));

				if (disposisiAlurSop.getDiajukanOleh() != null
						&& disposisiAlurSop.getDiajukanOleh().getPegawai() != null) {
					Pegawai pegawai = disposisiAlurSop.getDiajukanOleh().getPegawai();
					List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkat(sekarang, pangkats);
					JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
					JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
					Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

					parameters.put("jabatanFungsional_sop_" + disposisiAlurSop.getAlurSop().getId(),
							jabatanFungsional == null ? "" : jabatanFungsional.getNama());
					parameters.put("jabatanStruktural_sop_" + disposisiAlurSop.getAlurSop().getId(),
							jabatanStruktural == null ? "" : jabatanStruktural.getNama());
					parameters.put("jabatan_sop_" + disposisiAlurSop.getAlurSop().getId(),
							jabatan == null ? "" : jabatan.getNama());
				}
			}
		}

		List<PerjanjianKerjasamaMasterAssetDetail> perjanjianKerjasamaMasterAssetDetails = session
				.createCriteria(PerjanjianKerjasamaMasterAssetDetail.class).createAlias("masterAsset", "masterAsset")
				.addOrder(Order.asc("masterAsset.jenisAsset")).addOrder(Order.asc("masterAsset.kelompokAsset"))
				.add(Restrictions.eq("perjanjianKerjasamaMasterAsset", perjanjianKerjasamaMasterAsset)).list();
		List<Map> maps = new ArrayList<Map>();
		for (PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail : perjanjianKerjasamaMasterAssetDetails) {
			Map map = new HashMap();
			Common.insertProperty(PerjanjianKerjasamaMasterAssetDetail.class, perjanjianKerjasamaMasterAssetDetail, map,
					"data");

			map.put("kelompok_asset",
					perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getKelompokAsset() == null ? ""
							: perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getKelompokAsset().getNama());

			map.put("jenis_asset", perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getJenisAsset() == null ? ""
					: perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getJenisAsset().getNama());

			map.put("tipe_asset", perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getTipe());

			map.put("spesifikasi", perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getSpesifikasi());

			map.put("ppn",
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getPpn()
							? perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getPersenPpn()
							: 0.0);
			map.put("hargapotongan", perjanjianKerjasamaMasterAssetDetail.getHargaPotongan());
			map.put("hargabeli", perjanjianKerjasamaMasterAssetDetail.getHargaBeli());
			map.put("jumlah", perjanjianKerjasamaMasterAssetDetail.getJumlah());
			map.put("nama", perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getNama());
			map.put("kode", perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getKode());
			map.put("isbn", perjanjianKerjasamaMasterAssetDetail.getMasterAsset().getKode());

			map.put("penyedia",
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getPenyedia() == null ? ""
							: perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getPenyedia()
									.getNama());

			map.put("jenis_perjanjian_kerjasama_asset", perjanjianKerjasamaMasterAssetDetail
					.getPerjanjianKerjasamaMasterAsset().getJenisPerjanjianKerjasamaAsset());

			String status = "";
			if (perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() == null) {
				status = "Belum disetujui";
			} else {
				status = "Disetujui oleh "
						+ perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh()
								.getUserNama()
						+ " pada "
						+ (perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset()
								.getTanggalPersetujuan() == null ? ""
										: Common.dateFormat51.get().format(perjanjianKerjasamaMasterAssetDetail
												.getPerjanjianKerjasamaMasterAsset().getTanggalPersetujuan()));
			}

			map.put("status_persetujuan", status);

			map.put("perpustakaan",
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getKeterangan());

			map.put("pengirimanPalingLambat", perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset()
					.getPengirimanPalingLambat());

			map.put("tanggal_persetujuan",
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getTanggalPersetujuan());
			map.put("disetujui_oleh",
					perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset().getDisetujuiOleh() == null
							? ""
							: perjanjianKerjasamaMasterAssetDetail.getPerjanjianKerjasamaMasterAsset()
									.getDisetujuiOleh().getUserNama());

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
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@link FormSop#cetakData(GeneralValueObject)}
	 * untuk menghasilkan file laporan PDF perjanjian kerjasama yang dapat disimpan atau
	 * dikirim sebagai lampiran email. Method ini digunakan oleh framework SOP ketika perlu
	 * menghasilkan dokumen fisik dari perjanjian kerjasama.
	 *
	 * <b>Cara kerja:</b> Melakukan cast {@code generalValueObject} ke
	 * {@link PerjanjianKerjasamaMasterAsset}, membangun parameter laporan melalui {@code parameter()},
	 * lalu memanggil {@link Report#generateFileReport} dengan template {@code "asset/perjanjian_kerjasama"}
	 * untuk menghasilkan file PDF. Tanggal pembuatan perjanjian digunakan sebagai parameter
	 * tanggal laporan. Daftar maps tambahan disetel null karena parameter sudah mencakup semua
	 * data yang diperlukan.
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject Entitas perjanjian kerjasama yang dibungkus sebagai
	 *        {@link GeneralValueObject}. Harus dapat di-cast ke {@link PerjanjianKerjasamaMasterAsset}.
	 * @return {@link File} objek yang menunjuk ke file PDF sementara yang dihasilkan di
	 *         direktori temporary server. Caller bertanggung jawab menghapus file ini setelah
	 *         digunakan.
	 * @throws Exception Jika gagal men-generate laporan, misalnya template JRXML tidak ditemukan,
	 *                   atau terjadi kesalahan rendering JasperReports.
	 *
	 * <b>Pemeliharaan:</b> Nama template {@code "asset/perjanjian_kerjasama"} mengacu ke file
	 * di direktori laporan yang dikonfigurasi di sistem. Jika template dipindah atau diganti
	 * nama, perbarui string ini.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset = (PerjanjianKerjasamaMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(perjanjianKerjasamaMasterAsset),
				"asset/perjanjian_kerjasama", perjanjianKerjasamaMasterAsset.getTanggalPembuatan(), maps,
				Common.locale);
		return file;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan dan menampilkan laporan PDF perjanjian kerjasama langsung
	 * di browser pengguna melalui mekanisme popup ZKoss. Method statis ini dipanggil setelah
	 * operasi persetujuan atau simpan berhasil agar pengguna dapat langsung mencetak dokumen.
	 *
	 * <b>Cara kerja:</b> Membangun parameter laporan menggunakan method {@code parameter()},
	 * kemudian memanggil {@link Report#generatePDFReport} yang akan menghasilkan file PDF
	 * dan membukanya di browser pengguna melalui respons streaming ZKoss. Template yang
	 * digunakan adalah {@code "asset/perjanjian_kerjasama"} dari direktori laporan terkonfigurasi.
	 * Tanggal pembuatan perjanjian digunakan sebagai konteks tanggal laporan (untuk header/footer).
	 *
	 * <b>Parameter:</b>
	 * @param perjanjianKerjasamaMasterAsset Entitas perjanjian kerjasama yang akan dicetak.
	 *        Harus sudah tersimpan di database (ID tidak null) agar data detail dapat dimuat.
	 * @throws Exception Jika terjadi kesalahan saat membangun parameter atau saat rendering
	 *                   template JasperReports, termasuk kegagalan akses file template atau
	 *                   kesalahan query data dari database.
	 *
	 * <b>Return:</b> Void. Output berupa file PDF yang dikirim langsung ke browser.
	 *
	 * <b>Pemeliharaan:</b> Method ini bersifat statis agar dapat dipanggil dari class lain
	 * (misalnya dari action pemesanan) tanpa perlu instance controller. Pertahankan sifat
	 * statisnya. Jika format output perlu diubah (misalnya ke Excel), ganti {@code Report.PDF}
	 * dengan konstanta format yang sesuai.
	 */
	public static void cetak(PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(perjanjianKerjasamaMasterAsset), "asset/perjanjian_kerjasama",
				perjanjianKerjasamaMasterAsset.getTanggalPembuatan());
	}

	private Checkbox searchaktif;

	/**
	 * <b>Tujuan:</b> Membangun objek {@link Criteria} Hibernate yang mengenkapsulasi semua
	 * kondisi filter pencarian perjanjian kerjasama berdasarkan input pengguna di panel filter.
	 * Method ini digunakan baik untuk menghitung total data (tanpa order) maupun untuk mengambil
	 * data paginasi (dengan order).
	 *
	 * <b>Cara kerja:</b> Membangun Criteria terhadap {@link PerjanjianKerjasamaMasterAsset}
	 * dengan menggabungkan kondisi-kondisi berikut secara AND:
	 * <ul>
	 *   <li><b>Filter aktif:</b> Jika checkbox {@code searchaktif} dicentang, hanya tampilkan
	 *       data dengan {@code aktif = true} atau {@code aktif IS NULL}; jika tidak dicentang,
	 *       tampilkan semua (termasuk tidak aktif).</li>
	 *   <li><b>Filter status persetujuan:</b> Kombinasi checkbox {@code blmDisetujui} dan
	 *       {@code disetujui} mengontrol filter pada kolom {@code disetujuiOleh}: kedua dicentang
	 *       = semua, keduanya tidak = tidak ada hasil, hanya blm = IS NULL, hanya sudah = IS NOT NULL.</li>
	 *   <li><b>Filter lunas:</b> Checkbox {@code lunasSaja} menambahkan filter {@code lunas = true};
	 *       checkbox {@code blmLunasSaja} menambahkan filter {@code lunas = false}.</li>
	 *   <li><b>Filter satuan kerja:</b> Mengambil set satuan kerja yang diperbolehkan dari
	 *       {@code SekolahUtil.ambilSatuanKerjas()} dan memfilter berdasarkan hierarki pohon
	 *       satuan kerja jika {@code searchparent} dipilih.</li>
	 *   <li><b>Filter penyedia:</b> Exact match pada relasi {@code penyedia} jika dipilih.</li>
	 *   <li><b>Filter lokasi:</b> Exact match pada relasi {@code lokasi} jika dipilih.</li>
	 *   <li><b>Filter catatan kesepakatan:</b> ILIKE (case-insensitive ANYWHERE) pada kolom
	 *       {@code catatanKesepakatan} jika diisi.</li>
	 *   <li><b>Filter kode:</b> ILIKE ANYWHERE pada kolom {@code kode} jika diisi.</li>
	 *   <li><b>Filter keterangan:</b> ILIKE ANYWHERE pada kolom {@code keterangan} jika diisi.</li>
	 *   <li><b>Urutan:</b> Jika parameter {@code order} true, menambahkan ORDER BY {@code id DESC}.</li>
	 * </ul>
	 *
	 * <b>Parameter:</b>
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC (digunakan saat mengambil
	 *              data untuk ditampilkan); {@code false} untuk query count tanpa ordering
	 *              (digunakan saat menghitung total baris untuk paginasi).
	 * @return {@link Criteria} yang sudah dikonfigurasi, siap untuk dipaginasi atau di-count.
	 *
	 * <b>Pemeliharaan:</b> Setiap filter baru yang ditambahkan ke panel pencarian harus juga
	 * ditambahkan sebagai kondisi {@code .add()} di method ini. Gunakan pola
	 * {@code kondisi ? Restrictions.xxx() : Restrictions.sqlRestriction("1=1")} untuk filter
	 * opsional agar tidak mempengaruhi hasil jika filter tidak diisi.
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
		Criteria criteria = session.createCriteria(PerjanjianKerjasamaMasterAsset.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(blmDisetujui == null || disetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() && disetujui.isChecked() ? Restrictions.sqlRestriction("true")
						: !blmDisetujui.isChecked() && !disetujui.isChecked() ? Restrictions.sqlRestriction("false")
								: blmDisetujui.isChecked() ? Restrictions.isNull("disetujuiOleh")
										: Restrictions.isNotNull("disetujuiOleh"))

				.add(lunasSaja != null && lunasSaja.isChecked() ? Restrictions.eq("lunas", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(blmLunasSaja != null && blmLunasSaja.isChecked() ? Restrictions.eq("lunas", false)
						: Restrictions.sqlRestriction("1=1"))

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add((searchPenyedia == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchPenyedia.getAttribute("penyediaAsset") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("penyedia", searchPenyedia.getAttribute("penyediaAsset"))))

				.add(searchlokasi.getSelectedItem() == null || searchlokasi.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("lokasi", searchlokasi.getSelectedItem().getValue()))

				.add(searchcatatan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("catatanKesepakatan", searchcatatan.getValue().trim(), MatchMode.ANYWHERE))

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
	 * <b>Tujuan:</b> Menjalankan pencarian data perjanjian kerjasama berdasarkan semua filter
	 * yang aktif saat ini dan menampilkan hasilnya di grid dengan paginasi. Method ini adalah
	 * handler utama untuk semua aksi yang memerlukan refresh daftar: perubahan filter, navigasi
	 * halaman, setelah simpan, setelah hapus, dan setelah timer inisialisasi.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memperbarui informasi paginasi (total baris) dengan memanggil {@code Common.initPaging()}
	 *       menggunakan Criteria tanpa order (query COUNT).</li>
	 *   <li>Mengambil data perjanjian dengan Criteria berurutan, dibatasi oleh
	 *       {@code Common.ROWS_COUNT_ON_PAGE} baris, dimulai dari offset halaman aktif saat ini.</li>
	 *   <li>Membungkus hasil dalam {@link SimpleListModel} dan mengatur renderer
	 *       {@link PerjanjianKerjasamaMasterAssetRenderer} ke grid.</li>
	 *   <li>Memanggil {@code grid.setModelCheckMobile()} untuk mengatur model sekaligus
	 *       menangani perbedaan tampilan mobile/desktop.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK pemicu, bisa berupa event paginasi, event perubahan filter,
	 *              atau event timer. Nilai event tidak digunakan secara langsung; yang
	 *              penting adalah state komponen filter UI yang dibaca oleh {@code initCriteria()}.
	 *
	 * <b>Return:</b> Void. Hasil ditampilkan langsung ke grid ZKoss.
	 *
	 * <b>Pemeliharaan:</b> Jika perlu menambahkan kolom baru ke grid, tidak perlu mengubah
	 * method ini; cukup tambahkan kolom di renderer dan di ZUL. Jika ukuran halaman perlu
	 * diubah, ubah konstanta {@code Common.ROWS_COUNT_ON_PAGE}.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PerjanjianKerjasamaMasterAsset> perjanjianKerjasamaMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(perjanjianKerjasamaMasterAsset);
		grid.setRowRenderer(new PerjanjianKerjasamaMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	private MyGrid gridMasterAsset;
	private MyCheckboxConfig setujui;
	private Radiogroup jenisPerjanjianKerjasamaAsset;
	private MyDatebox pengirimanPalingLambat;
	private MyDoublebox dp;
	private MyTextbox catatanKesepakatan;
	private MyDatebox pengirimanMulai;
	private Textbox nomorPerjanjianKerjasama;
	private JSONArray array;
	private Row rowFormula;
	private Vbox permintaanPengadaanMasterAsset;
	private HashSet<PermintaanPengadaanMasterAssetDetail> permintaanPengadaanMasterAssets;
	private PerjanjianKerjasamaMasterAssetHelper perjanjianKerjasamaMasterAssetHelper;

	/**
	 * <b>Tujuan:</b> Membangun dan mengembalikan komponen formulir lengkap untuk input atau
	 * tampilan data perjanjian kerjasama. Method ini mengimplementasikan kontrak
	 * {@link FormSop#form()} dan mendukung dua mode: mode edit (komponen input interaktif)
	 * dan mode persetujuan (komponen label read-only).
	 *
	 * <b>Cara kerja:</b> Membuat {@link MyGrid} dua kolom (30% label, 70% input) dengan
	 * {@link Rows} berisi baris-baris formulir menggunakan {@link MyFormRow}. Setiap baris
	 * formulir memiliki label di kiri dan komponen input/label di kanan. Komponen yang
	 * dibangun mencakup:
	 * <ul>
	 *   <li>Penyedia ({@link AmbilDataPenyediaAssetBanbox} atau {@link Label}).</li>
	 *   <li>Kode kerjasama (auto-generate, ditampilkan sebagai {@link Label}).</li>
	 *   <li>Nomor perjanjian (input teks atau label).</li>
	 *   <li>Tanggal pembuatan ({@link MyDatebox} atau label).</li>
	 *   <li>Rentang tanggal pekerjaan: mulai dan selesai ({@link MyDatebox} atau label).</li>
	 *   <li>Jenis kerjasama ({@link Radiogroup} dinamis dari database atau label).</li>
	 *   <li>Kode anggaran: jika ada permintaan pengadaan terkait, tampilkan daftar workspace.</li>
	 *   <li>Permintaan pengadaan: tombol ambil data + sub-grid daftar permintaan terpilih
	 *       dengan opsi hapus per item dan kalkulasi DP otomatis.</li>
	 *   <li>Nilai pekerjaan/DP ({@link MyDoublebox} atau label).</li>
	 *   <li>Nomor tagihan pekerjaan ({@link MyTextbox} atau label).</li>
	 *   <li>Pemilik aset ({@link Combobox} atau label), tampil jika konfigurasi aktif.</li>
	 *   <li>Lokasi ({@link Combobox} atau label).</li>
	 *   <li>Ruang ({@link AmbilDataRuangBanbox} atau label), tampil jika konfigurasi aktif.</li>
	 *   <li>Checkbox PPN dan input persentase PPN (dinonaktifkan jika PPN tidak dicentang).</li>
	 *   <li>Keterangan kerjasama dan catatan kesepakatan ({@link MyTextbox} multiline atau HTML).</li>
	 *   <li>Dokumen kerjasama: area upload/download lampiran dengan tombol tambah dokumen.</li>
	 *   <li>Grid detail item ({@link PerjanjianKerjasamaMasterAssetHelper}).</li>
	 *   <li>Checkbox "Setujui Perjanjian Kerjasama" (hanya tampil di mode persetujuan).</li>
	 * </ul>
	 * Event listener pada {@code jenisPerjanjianKerjasamaAsset} dan {@code tanggalPembuatan}
	 * memicu regenerasi kode saat jenis atau tanggal berubah (hanya untuk perjanjian baru).
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject Entitas perjanjian kerjasama. Harus dapat di-cast ke
	 *        {@link PerjanjianKerjasamaMasterAsset}.
	 * @param disposisiSop Disposisi SOP aktif yang terkait dengan perjanjian ini, atau
	 *        {@code null} jika tidak ada alur SOP.
	 * @param save Tombol simpan yang akan diaktifkan/dikonfigurasi oleh method ini.
	 * @param setujuiData Event listener opsional yang dipanggil saat status persetujuan berubah
	 *        (digunakan oleh framework SOP untuk memperbarui tampilan alur). Boleh {@code null}.
	 * @return {@link MyGrid} berisi seluruh komponen formulir yang siap dilampirkan ke
	 *         komponen induk (biasanya area Center dari Borderlayout).
	 * @throws Exception Jika terjadi kesalahan saat membangun komponen, termasuk kegagalan
	 *                   query database untuk mengisi combobox/radiogroup.
	 *
	 * <b>Pemeliharaan:</b> Setiap field baru di entitas harus ditambahkan sebagai baris baru
	 * di formulir ini. Pastikan flag {@code persetujuan} selalu diperiksa untuk menentukan
	 * apakah menampilkan komponen input atau label read-only.
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}

		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		this.perjanjianKerjasamaMasterAsset = (PerjanjianKerjasamaMasterAsset) generalValueObject;

		setujui = new MyCheckboxConfig("Setujui Perjanjian Kerjasama");
		setujui.setChecked(perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null);

		if (setujuiData != null) {
			setujui.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, perjanjianKerjasamaMasterAsset.getDisetujuiOleh() != null));
				}
			});
		}

		MyGrid grid = new MyGrid();
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia *"));

		penyediaAsset = new AmbilDataPenyediaAssetBanbox();
		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getPenyedia() == null ? ""
					: perjanjianKerjasamaMasterAsset.getPenyedia().getNama()));
		} else {
			row.appendChild(penyediaAsset);
		}

		penyediaAsset.setAttribute("penyediaAsset", perjanjianKerjasamaMasterAsset.getPenyedia());
		penyediaAsset.setValue(perjanjianKerjasamaMasterAsset.getPenyedia() == null ? ""
				: perjanjianKerjasamaMasterAsset.getPenyedia().getNama());
		penyediaAsset.setReadonly(true);
		penyediaAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kerjasama *"));

		tanggalPembuatan = new MyDatebox(
				perjanjianKerjasamaMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: perjanjianKerjasamaMasterAsset.getTanggalPembuatan());
		if (perjanjianKerjasamaMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			perjanjianKerjasamaMasterAsset.setKode(noAgenda);
		}

		kode = new Label(perjanjianKerjasamaMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Perjanjian"));
		nomorPerjanjianKerjasama = new Textbox(perjanjianKerjasamaMasterAsset.getNomorPerjanjianKerjasama());
		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getNomorPerjanjianKerjasama()));
		} else {
			row.appendChild(nomorPerjanjianKerjasama);
		}
		nomorPerjanjianKerjasama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pembuatan *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat6.get().format(tanggalPembuatan.getValue())));
		} else {
			row.appendChild(tanggalPembuatan);
		}
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");
		tanggalPembuatan.setReadonly(true);

		pengirimanMulai = new MyDatebox(perjanjianKerjasamaMasterAsset.getPengirimanMulai());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Mulai *"));
		if (persetujuan) {
			row.appendChild(new Label(
					pengirimanMulai.getValue() == null ? "" : Common.dateFormat1.get().format(pengirimanMulai.getValue())));
		} else {
			row.appendChild(pengirimanMulai);
		}
		pengirimanMulai.setFormat(Common.dateFormat1.get().toPattern());
		pengirimanMulai.setWidth("90%");
		pengirimanMulai.setReadonly(true);

		pengirimanPalingLambat = new MyDatebox(perjanjianKerjasamaMasterAsset.getPengirimanPalingLambat());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pekerjaan Selesai *"));
		if (persetujuan) {
			row.appendChild(new Label(pengirimanPalingLambat.getValue() == null ? ""
					: Common.dateFormat1.get().format(pengirimanPalingLambat.getValue())));
		} else {
			row.appendChild(pengirimanPalingLambat);
		}
		pengirimanPalingLambat.setFormat(Common.dateFormat1.get().toPattern());
		pengirimanPalingLambat.setWidth("90%");
		pengirimanPalingLambat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kerjasama *"));
		jenisPerjanjianKerjasamaAsset = new Radiogroup();
		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset() == null ? ""
					: perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset().getNama()));
		} else {
			row.appendChild(jenisPerjanjianKerjasamaAsset);
		}

		Common.insertRadio(jenisPerjanjianKerjasamaAsset, "nama", JenisPerjanjianKerjasamaAsset.class,
				Restrictions.eq("aktif", true));
		Common.selectRadioItem(jenisPerjanjianKerjasamaAsset,
				perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset());
		jenisPerjanjianKerjasamaAsset.setWidth("90%");

		EventListener jEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisPerjanjianKerjasamaAsset j = (JenisPerjanjianKerjasamaAsset) (jenisPerjanjianKerjasamaAsset
						.getSelectedItem() == null ? null
								: jenisPerjanjianKerjasamaAsset.getSelectedItem().getAttribute("value"));

				if (j != null && perjanjianKerjasamaMasterAsset.getId() == null) {
					perjanjianKerjasamaMasterAsset.setJenisPerjanjianKerjasamaAsset(j);
					String noAgenda = generateCode(false);
					kode.setValue(noAgenda);
				}
			}
		};

		jenisPerjanjianKerjasamaAsset.addEventListener("onClick", jEventListener);
		tanggalPembuatan.addEventListener("onChange", jEventListener);

		List<PermintaanPengadaanMasterAssetDetail> dataPermintaanPengadaanMasterAssetDetail = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
		if (!perjanjianKerjasamaMasterAsset.getPermintaanPengadaanMasterAssets().isEmpty()) {

			List<Long> data = new ArrayList<Long>();
			for (String s : perjanjianKerjasamaMasterAsset.getPermintaanPengadaanMasterAssets().split(",")) {
				try {
					data.add(Long.parseLong(s.trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PerjanjianKerjasamaMasterAssetAction.java:2013");
					// TODO: handle exception
				}
			}
			dataPermintaanPengadaanMasterAssetDetail = data.isEmpty()
					? new ArrayList<PermintaanPengadaanMasterAssetDetail>()
					: HibernateUtil.currentSession().createCriteria(PermintaanPengadaanMasterAssetDetail.class)
							.add(Restrictions.in("id", data)).list();

			if (dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {
				dataPermintaanPengadaanMasterAssetDetail = HibernateUtil.currentSession()
						.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
						.add(Restrictions.eq("perjanjianKerjasamaMasterAsset", perjanjianKerjasamaMasterAsset)).list();
			}

			if (!dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {

				Set<Workspace> workspaces = new HashSet<Workspace>();
				for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : dataPermintaanPengadaanMasterAssetDetail) {
					if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
							.getWorkspace() != null) {
						workspaces.add(permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
								.getWorkspace());
					}
				}

				if (!workspaces.isEmpty()) {
					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Kode Anggaran"));
					Vbox unit = new Vbox();
					row.appendChild(unit);

					for (Workspace workspace : workspaces) {
						RevisiHelper.createNewRevisi(Workspace.class, workspace, workspace.toString()).setParent(unit);
					}
				}
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Permintaan Pengadaan *")));
		row.appendChild(permintaanPengadaanMasterAsset = new Vbox());
		permintaanPengadaanMasterAsset.setWidth("90%");
		PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets = new HashSet<PermintaanPengadaanMasterAssetDetail>();

		if (!perjanjianKerjasamaMasterAsset.getPermintaanPengadaanMasterAssets().isEmpty()) {
			PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets
					.addAll(dataPermintaanPengadaanMasterAssetDetail);
		}

		MyToolbarbuttonConfig button;
		permintaanPengadaanMasterAsset.appendChild(
				button = new MyToolbarbuttonConfig("Ambil Data Permintaan Barang/Jasa", "/img/svg/addthis.svg"));
		button.setVisible(!persetujuan);
		final MyGrid subPermintaanPengadaanMasterAsset = new MyGrid();
		permintaanPengadaanMasterAsset.appendChild(subPermintaanPengadaanMasterAsset);

		row = new MyFormRow();
		row.setParent(rows);
		dp = new MyDoublebox(perjanjianKerjasamaMasterAsset.getDp());
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Pekerjaan *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.numberFormat.get().format(perjanjianKerjasamaMasterAsset.getDp())));
		} else {
			row.appendChild(dp);
		}

		row = new MyFormRow();
		row.setParent(rows);
		kodeInvoice = new MyTextbox(perjanjianKerjasamaMasterAsset.getKodeInvoice());
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Tagihan Pekerjaan"));
		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getKodeInvoice()));
		} else {
			row.appendChild(kodeInvoice);
		}

		pemilikAsset = new Combobox();
		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pemilik"));
		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getPemilikAsset() == null ? ""
					: perjanjianKerjasamaMasterAsset.getPemilikAsset().getNama()));
		} else {
			row.appendChild(pemilikAsset);
		}

		Common.insertComboDanSemua(pemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(pemilikAsset, perjanjianKerjasamaMasterAsset.getPemilikAsset());
		pemilikAsset.setWidth("90%");

		lokasi = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));

		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getLokasi() == null ? ""
					: perjanjianKerjasamaMasterAsset.getLokasi().getNama()));
		} else {
			row.appendChild(lokasi);
		}

		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, perjanjianKerjasamaMasterAsset.getLokasi());
		lokasi.setWidth("90%");

		LokasiAction.kunciLokasi(lokasi);

		ruang = new AmbilDataRuangBanbox();
		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		ruang = new AmbilDataRuangBanbox();
		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getRuang() == null ? ""
					: perjanjianKerjasamaMasterAsset.getRuang().getNama()));
		} else {
			row.appendChild(ruang);
		}

		ruang.setValue(perjanjianKerjasamaMasterAsset.getRuang() == null ? ""
				: (perjanjianKerjasamaMasterAsset.getRuang().getKodeRuangan()));
		ruang.setAttribute("ruang", perjanjianKerjasamaMasterAsset.getRuang());
		ruang.setWidth("90%");

		ppn = new MyCheckboxConfig("PPN");
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));

		if (persetujuan) {
			row.appendChild(new Label(perjanjianKerjasamaMasterAsset.getPpn() ? "Terdapat PPN" : "Tidak Terdapat PPN"));
		} else {
			row.appendChild(ppn);
		}

		ppn.setChecked(perjanjianKerjasamaMasterAsset.getPpn());

		persenPpn = new MyDoublebox(perjanjianKerjasamaMasterAsset.getPersenPpn());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("PPN (%)"));
		if (persetujuan) {
			row.appendChild(new Label(Common.numberFormat.get().format(perjanjianKerjasamaMasterAsset.getPersenPpn()) + "%"));
		} else {
			row.appendChild(persenPpn);
		}

		persenPpn.setDisabled(!perjanjianKerjasamaMasterAsset.getPpn());

		ppn.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				persenPpn.setDisabled(!ppn.isChecked());
			}
		});

		keterangan = new MyTextbox(perjanjianKerjasamaMasterAsset.getKeterangan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Kerjasama"));
		row.appendChild(keterangan = new MyTextbox(perjanjianKerjasamaMasterAsset.getKeterangan()));

		if (persetujuan) {
			Html label = new Html(perjanjianKerjasamaMasterAsset.getKeterangan());
			row.appendChild(label);
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Kerjasama"));
		array = new JSONArray(perjanjianKerjasamaMasterAsset.getFormula());
		rowFormula = Common.tampilanScroll1(row);
		reloadFormula(rowFormula, array);

		keterangan = new MyTextbox(perjanjianKerjasamaMasterAsset.getKeterangan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan Kesepakatan"));
		catatanKesepakatan = new MyTextbox(perjanjianKerjasamaMasterAsset.getCatatanKesepakatan());

		if (persetujuan) {
			Html label = new Html(perjanjianKerjasamaMasterAsset.getCatatanKesepakatan());
			row.appendChild(label);
		} else {
			row.appendChild(catatanKesepakatan);
		}

		catatanKesepakatan.setWidth("90%");
		catatanKesepakatan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(((perjanjianKerjasamaMasterAssetHelper = new PerjanjianKerjasamaMasterAssetHelper(
				gridMasterAsset = new MyGrid()))).initDetail(perjanjianKerjasamaMasterAsset));

		row = new MyFormRow();
		row.setVisible(persetujuan);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Perjanjian Kerjasama"));
		row.appendChild(setujui);

		class PermintaanBarangEventListener implements EventListener {

			private PermintaanBarangEventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(subPermintaanPengadaanMasterAsset);

				if (PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets != null) {

					Map<Long, PermintaanPengadaanMasterAsset> lists = new HashMap<Long, PermintaanPengadaanMasterAsset>();
					for (PermintaanPengadaanMasterAssetDetail assetDetail : PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets) {
						lists.put(assetDetail.getPermintaanPengadaanMasterAsset().getId(),
								assetDetail.getPermintaanPengadaanMasterAsset());
					}

					Columns columns = new Columns();
					columns.setParent(subPermintaanPengadaanMasterAsset);

					MyColumnConfig column = new MyColumnConfig("Data Permintaan Pembelian");
					column.setParent(columns);

					if (!persetujuan) {
						column = new MyColumnConfig("Batal");
						column.setParent(columns);
						column.setWidth("15%");
					}

					Rows rows = new Rows();
					rows.setParent(subPermintaanPengadaanMasterAsset);
					for (final PermintaanPengadaanMasterAsset k : lists.values()) {

						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setParent(rows);

						A a = new A(k.getKode() + "-" + k.getKeterangan()
								+ (k.getDisetujuiOleh() == null ? ""
										: " (" + k.getDisetujuiOleh().getUserNama() + " "
												+ Common.dateFormat51.get().format(k.getTanggalPersetujuan()) + ")"));
						a.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								PermintaanPengadaanMasterAssetAction.onAddExternal(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

									}
								}, k);

							}
						});
						a.setStyle("font-size:10px;");
						a.setParent(row);

						if (!persetujuan) {
							MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
							button.setTooltiptext("Hapus Data");
							button.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
											MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
											MyMessageboxConfig.QUESTION, new EventListener() {

												@Override
												public void onEvent(Event event) throws Exception {
													int i = Integer.parseInt(event.getData().toString());
													if (i == MyMessageboxConfig.OK) {
														try {

															List<PermintaanPengadaanMasterAssetDetail> assetDetails = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
															for (PermintaanPengadaanMasterAssetDetail assetDetail : PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets) {
																if (!assetDetail.getPermintaanPengadaanMasterAsset()
																		.getId().equals(k.getId())) {
																	assetDetails.add(assetDetail);
																}
															}

															PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets
																	.clear();
															PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets
																	.addAll(assetDetails);

															Common.createDefaultTimer(getThis());

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
							button.setParent(row);
						}
					}
				}

				Double n = 0.0;

				for (PermintaanPengadaanMasterAssetDetail assetDetail : PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets) {
					Double k = assetDetail.getJumlah() * assetDetail.getHargaBeli();
					n += k;
				}

				dp.setValue(n);
				perjanjianKerjasamaMasterAsset.setDp(n);
			}

		}

		final PermintaanBarangEventListener permintaanBarangEventListener = new PermintaanBarangEventListener();

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<PermintaanPengadaanMasterAsset> permintaanPengadaanMasterAssets = new ArrayList<PermintaanPengadaanMasterAsset>();

						AmbilDataPermintaanPengadaanMasterAssetBanyak ambilPermintaanPengadaanMasterAsset = new AmbilDataPermintaanPengadaanMasterAssetBanyak(
								false, permintaanPengadaanMasterAssets, null);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
								.appendChild(ambilPermintaanPengadaanMasterAsset);
						ambilPermintaanPengadaanMasterAsset.setWidth("90%");
						ambilPermintaanPengadaanMasterAsset.setHeight("90%");

						ambilPermintaanPengadaanMasterAsset.setEventListener(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets
										.addAll((List<PermintaanPengadaanMasterAssetDetail>) arg0.getData());

								for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : PerjanjianKerjasamaMasterAssetAction.this.permintaanPengadaanMasterAssets) {
									PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail = new PerjanjianKerjasamaMasterAssetDetail();
									perjanjianKerjasamaMasterAssetDetail
											.setMasterAsset(permintaanPengadaanMasterAssetDetail.getMasterAsset());
									perjanjianKerjasamaMasterAssetDetail.setPermintaanPengadaanMasterAssetDetail(
											permintaanPengadaanMasterAssetDetail);
									perjanjianKerjasamaMasterAssetDetail
											.setJumlah(permintaanPengadaanMasterAssetDetail.getJumlah());
									perjanjianKerjasamaMasterAssetDetail
											.setKeterangan(permintaanPengadaanMasterAssetDetail.getKeterangan());
									perjanjianKerjasamaMasterAssetDetail
											.setPerjanjianKerjasamaMasterAsset(perjanjianKerjasamaMasterAsset);

									if (perjanjianKerjasamaMasterAsset.getId() != null) {
										Session session = HibernateUtil.currentSession();
										session.save(perjanjianKerjasamaMasterAssetDetail);
									}

									Rows rows = gridMasterAsset.getRows() == null ? new Rows()
											: gridMasterAsset.getRows();
									rows.setParent(gridMasterAsset);
									MyFormRow row = new MyFormRow();
									row.setValign("top");
									row.setParent(rows);
									perjanjianKerjasamaMasterAssetHelper.initRow(row,
											perjanjianKerjasamaMasterAssetDetail);
								}
								perjanjianKerjasamaMasterAssetHelper.eventListenerHitungUlang.onEvent(arg0);

								permintaanBarangEventListener.onEvent(arg0);
							}
						});

						ambilPermintaanPengadaanMasterAsset.onModal();
					}
				});
			}
		});

		Common.createDefaultTimer(permintaanBarangEventListener);

		jEventListener.onEvent(null);

		return grid;
	}

	/**
	 * <b>Tujuan:</b> Memuat ulang tampilan daftar dokumen kerjasama (formula) ke dalam baris
	 * formulir yang diberikan. Method ini dipanggil setiap kali daftar dokumen berubah (tambah
	 * atau hapus item) untuk menyegarkan tampilan grid dokumen secara dinamis.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membersihkan semua komponen anak dari {@code rowU} menggunakan {@code Common.clear()}.</li>
	 *   <li>Membuat grid baru dengan dua kolom: "Dokumen" (90%) dan kolom aksi (sisanya).</li>
	 *   <li>Mengiterasi setiap elemen {@link JSONObject} dalam {@code array}. Elemen yang
	 *       tidak memiliki key "key" (sudah dihapus) dilewati.</li>
	 *   <li>Untuk setiap entri valid, membuat baris dengan:
	 *     <ul>
	 *       <li>Input teks nama dokumen ({@link MyTextbox}) dengan event onChange yang
	 *           memperbarui field "nama" di JSONObject terkait.</li>
	 *       <li>Jika file sudah diunggah (ada {@link LampiranLain}): tautan nama file
	 *           dengan event klik yang membuka tampilan file via {@code Common.display()}.</li>
	 *       <li>Jika ada link eksternal (bukan file lokal): tautan URL yang membuka popup
	 *           browser via {@code Clients.evalJavaScript()}.</li>
	 *       <li>Jika belum ada file: komponen upload/download dari
	 *           {@code LampiranLain.createDownloadUploadFileLain()} yang menangani unggah
	 *           baru dan memperbarui JSONObject dengan link, nama file, dan ID file.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Tombol hapus per baris: mengisi slot JSONObject dengan JSONObject kosong (bukan
	 *       menghapus dari array untuk mempertahankan indeks), lalu memanggil
	 *       {@code reloadDataFormula()} kembali untuk menyegarkan tampilan.</li>
	 *   <li>Tombol hapus disembunyikan jika formulir dalam mode persetujuan atau checkbox
	 *       "Setujui" sudah dicentang.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param rowU  Baris formulir ({@link Row}) yang akan diisi dengan grid daftar dokumen.
	 *              Konten baris ini akan dihapus dan diganti setiap kali method ini dipanggil.
	 * @param array Array JSON yang berisi daftar entri dokumen. Setiap entri adalah
	 *              {@link JSONObject} dengan field: "key" (Long, pengidentifikasi unik),
	 *              "nama" (String, deskripsi dokumen), "nama_file" (String), "link" (String URL),
	 *              dan "id_file" (Long, ID LampiranLain jika sudah diunggah).
	 * @throws Exception Jika terjadi kesalahan saat membuat komponen ZK atau mengakses
	 *                   database untuk mencari {@link LampiranLain}.
	 *
	 * <b>Pemeliharaan:</b> Pola "hapus" dengan mengisi JSONObject kosong (bukan remove)
	 * disengaja untuk mempertahankan indeks array. Jangan mengubah ke {@code array.remove()}
	 * kecuali semua referensi indeks juga diperbarui.
	 */
	public void reloadDataFormula(final Row rowU, final JSONArray array) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Dokumen");
		column.setParent(columns);
		column.setWidth("90%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			Long key;
			if (jsonObject.isNull("key")) {
				continue;
			} else {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			String nama = "";

			if (!jsonObject.isNull("nama")) {
				nama = jsonObject.get("nama") + "";
			}

			String nama_file = "";

			if (!jsonObject.isNull("nama_file")) {
				nama_file = jsonObject.get("nama_file") + "";
			}

			String link = "";

			if (!jsonObject.isNull("link")) {
				link = jsonObject.get("link") + "";
			}

			Long id_file = null;

			if (!jsonObject.isNull("id_file")) {
				id_file = Long.parseLong(jsonObject.get("id_file") + "");
			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			Vbox vbox = new Vbox();
			vbox.setWidth("95%");
			vbox.setParent(row);

			final MyTextbox targetText = new MyTextbox(nama);

			targetText.setParent(vbox);
			targetText.setWidth("95%");

			final LampiranLain lampiranLain = id_file != null ? LampiranLain.ambil(true, id_file, "id")
					: LampiranLain.ambil(perjanjianKerjasamaMasterAsset.getId(), key.toString());

			if (lampiranLain != null) {

				A a = new A(lampiranLain.getNama());
				a.setParent(vbox);
				a.setWidth("95%");

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.display(lampiranLain);
					}
				});

			}

			else if (!nama_file.isEmpty() && !link.isEmpty()) {

				A a = new A(nama_file);
				a.setParent(vbox);
				a.setWidth("95%");
				final String url = link;
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.evalJavaScript("popupCenter({url: '" + url + "', title: 'Data', w: 1200, h: 600});");
					}
				});

			} else {

				Vbox myvbox = new Vbox();
				myvbox.setParent(vbox);
				myvbox.setWidth("95%");

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);
				LampiranLain.createDownloadUploadFileLain(hbox, perjanjianKerjasamaMasterAsset.getId(), key.toString(),
						"Dokumen Kerjasama", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								LampiranLain lampiranLain = (LampiranLain) arg0.getData();
								lampiranLains.add(lampiranLain);
								jsonObject.put("link", lampiranLain.createLinkUri(false));
								jsonObject.put("nama_file", lampiranLain.getNama());
								jsonObject.put("id_file", lampiranLain.getId());
							}
						}, null, false, false, false, !(persetujuan || setujui.isChecked()));

			}

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					jsonObject.put("nama", targetText.getValue());

				}
			};

			targetText.setRows(2);

			targetText.addEventListener("onChange", eventListener);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											array.put(index, new JSONObject());

											reloadDataFormula(rowU, array);

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
			if (persetujuan || setujui.isChecked()) {
				new Label().setParent(row);
			} else {
				button.setParent(row);
			}

		}
	}

	private List<LampiranLain> lampiranLains = new ArrayList<LampiranLain>();

	/**
	 * <b>Tujuan:</b> Menginisialisasi dan merender area dokumen kerjasama di formulir, termasuk
	 * tombol "Tambah Dokumen" dan wadah (container row) untuk daftar dokumen. Method ini dipanggil
	 * sekali saat formulir dibangun, berbeda dengan {@code reloadDataFormula()} yang dipanggil
	 * setiap kali daftar dokumen berubah.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat {@link MyFormRow} baru ({@code rowU}) sebagai wadah untuk daftar dokumen.</li>
	 *   <li>Membuat tombol "Tambah Dokumen" yang ketika diklik: membuat {@link JSONObject} baru
	 *       dengan "nama" kosong dan "key" unik (nilai random positif), menambahkannya ke
	 *       {@code array}, lalu memanggil {@code reloadDataFormula()} untuk menampilkan
	 *       entri baru tersebut.</li>
	 *   <li>Tombol "Tambah Dokumen" disembunyikan jika formulir dalam mode persetujuan
	 *       ({@code persetujuan == true}) atau checkbox "Setujui" sudah dicentang.</li>
	 *   <li>Melampirkan tombol ke {@code rowFormula} dan {@code rowU} ke parent dari
	 *       {@code rowFormula} (bukan ke {@code rowFormula} itu sendiri) untuk layout yang benar.</li>
	 *   <li>Memanggil {@code reloadDataFormula(rowU, array)} untuk menampilkan dokumen
	 *       yang sudah ada.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param rowFormula Baris formulir khusus yang sudah dikonfigurasi oleh
	 *        {@code Common.tampilanScroll1()} sebagai scroll container untuk area dokumen.
	 *        Tombol "Tambah Dokumen" dilampirkan ke baris ini.
	 * @param array Array JSON yang berisi daftar dokumen saat ini. Bisa kosong untuk
	 *              perjanjian baru, atau berisi data yang sudah ada untuk mode edit.
	 * @throws Exception Jika terjadi kesalahan saat membuat komponen ZK atau memanggil
	 *                   {@code reloadDataFormula()}.
	 *
	 * <b>Pemeliharaan:</b> Method ini memiliki ketergantungan pada {@code setujui} yang harus
	 * sudah diinisialisasi sebelum method ini dipanggil (diinisialisasi di awal {@code form()}).
	 * Pastikan urutan inisialisasi di {@code form()} tidak mengubah hal ini.
	 */
	public void reloadFormula(final Row rowFormula, final JSONArray array) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Dokumen", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(!persetujuan && !setujui.isChecked());
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("nama", "");
				Long key = Math.abs(Common.randLong());
				jsonObject.put("key", key);
				array.put(jsonObject);

				reloadDataFormula(rowU, array);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array);

	}

	/**
	 * <b>Tujuan:</b> Mengembalikan nama istilah atau label modul ini dalam bahasa Indonesia
	 * untuk digunakan dalam teks SOP, notifikasi, judul popup persetujuan, dan pesan audit.
	 * Mengimplementasikan kontrak {@link FormSop#istilah()}.
	 *
	 * <b>Cara kerja:</b> Mengembalikan string literal tetap yang merepresentasikan nama
	 * dokumen bisnis yang dikelola oleh controller ini. String ini digunakan oleh framework
	 * SOP untuk menampilkan nama modul dalam alur disposisi.
	 *
	 * <b>Return:</b> String {@code "Perjanjian Kerjasama Pengadaan Barang/Jasa"} sebagai
	 *         nama resmi modul dalam konteks SOP dan antarmuka pengguna.
	 *
	 * @return Nama istilah modul perjanjian kerjasama dalam bahasa Indonesia.
	 * @throws Exception Tidak melempar exception dalam implementasi ini, tetapi dideklarasikan
	 *                   sesuai kontrak antarmuka {@link FormSop}.
	 *
	 * <b>Pemeliharaan:</b> Jika nama modul perlu diubah (misalnya penambahan tipe baru),
	 * perbarui string di sini. Perubahan akan otomatis berlaku di semua tempat yang
	 * memanggil method ini melalui antarmuka {@link FormSop}.
	 */
	@Override
	public String istilah() throws Exception {
		return "Perjanjian Kerjasama Pengadaan Barang/Jasa";
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan entitas {@link PerjanjianKerjasamaMasterAsset} yang sedang
	 * aktif dalam konteks formulir saat ini, sebagai implementasi kontrak {@link FormSop#ambil()}.
	 * Digunakan oleh framework SOP untuk mengambil data yang sedang diproses dalam alur
	 * disposisi.
	 *
	 * <b>Cara kerja:</b> Mengembalikan langsung referensi field instance
	 * {@code perjanjianKerjasamaMasterAsset} yang diatur oleh {@code init()} atau {@code form()}.
	 * Referensi ini menunjuk ke entitas yang sedang ditampilkan/diedit dalam formulir aktif.
	 *
	 * <b>Return:</b> Entitas {@link PerjanjianKerjasamaMasterAsset} aktif yang sedang diproses,
	 *         yang juga mengimplementasikan {@link DataSop}. Bisa {@code null} jika formulir
	 *         belum diinisialisasi.
	 *
	 * @return Objek {@link DataSop} yang merupakan entitas perjanjian kerjasama aktif.
	 * @throws Exception Tidak melempar exception dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b> Field {@code perjanjianKerjasamaMasterAsset} harus selalu diperbarui
	 * sebelum framework SOP memanggil method ini. Pastikan {@code init()} dan {@code form()}
	 * selalu mengatur field ini dengan benar.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return perjanjianKerjasamaMasterAsset;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan kelas Java dari entitas yang dikelola controller ini,
	 * sebagai implementasi kontrak {@link FormSop#ambilClass()}. Digunakan oleh framework
	 * SOP untuk operasi generik berbasis refleksi seperti pembuatan query Hibernate,
	 * pencatatan audit, dan pemetaan laporan.
	 *
	 * <b>Cara kerja:</b> Mengembalikan literal kelas {@code PerjanjianKerjasamaMasterAsset.class}
	 * secara langsung. Tidak ada logika dinamis; ini adalah delegasi statis ke class literal.
	 *
	 * <b>Return:</b> {@code Class} object untuk {@link PerjanjianKerjasamaMasterAsset}.
	 *
	 * @return Kelas entitas {@link PerjanjianKerjasamaMasterAsset} yang dikelola controller ini.
	 * @throws Exception Tidak melempar exception dalam implementasi ini.
	 *
	 * <b>Pemeliharaan:</b> Jika entitas diganti nama atau dipindah package, perbarui literal
	 * kelas di sini. Anotasi {@code @SuppressWarnings("rawtypes")} diperlukan karena
	 * kontrak antarmuka menggunakan raw type {@code Class} (kompatibilitas Java 1.5+).
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PerjanjianKerjasamaMasterAsset.class;
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan kode perjanjian kerjasama baru yang unik sesuai format
	 * penomoran surat yang dikonfigurasi melalui {@link NomorSuratAlurPengadaan}. Kode ini
	 * berfungsi sebagai nomor referensi dokumen resmi perjanjian kerjasama.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika konfigurasi nomor surat tidak tersedia ({@code PERJANJIAN_KERJASAMA_DATA} null
	 *       atau {@code getNomorSurat()} null), menggunakan barcode random sebagai fallback
	 *       via {@code Common.getGeneratedBarCode()}.</li>
	 *   <li>Menentukan indeks urutan: jika {@code gunakanIndexUrut} aktif, menggunakan
	 *       indeks tersimpan di konfigurasi; jika tidak, menghitung dari database via
	 *       {@code getindex()} (jumlah baris + 1 berdasarkan filter tahun/bulan/reset).</li>
	 *   <li>Jika parameter {@code tambah} adalah {@code true}, menginkrementasi indeks
	 *       tersimpan via {@code NomorSurat.tambahIndexNomorSurat()} untuk mencegah
	 *       duplikasi pada pemanggilan berikutnya.</li>
	 *   <li>Memformat kode menggunakan {@code nomorSurat.format(index, tanggal)} dengan
	 *       tanggal dari komponen {@code tanggalPembuatan} (atau tanggal saat ini sebagai
	 *       fallback jika komponen belum diinisialisasi).</li>
	 *   <li>Mengganti placeholder "JENIS PO" dalam format dengan kode jenis kerjasama
	 *       jika entitas sudah memiliki jenis kerjasama yang dipilih.</li>
	 *   <li>Memastikan keunikan kode dengan {@code KodeUnikUtil.pastikanUnik()} yang
	 *       menambahkan sufiks numerik (-2, -3, dst.) jika kode sudah digunakan.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param tambah {@code true} untuk menginkrementasi counter nomor surat (dipanggil saat
	 *               menyimpan perjanjian baru); {@code false} hanya untuk preview/generate
	 *               tanpa mengubah counter (dipanggil saat formulir dibuka atau jenis/tanggal
	 *               berubah).
	 * @return String kode perjanjian kerjasama yang sudah diformat dan dijamin unik,
	 *         misalnya {@code "PKS/BOT/001/VI/2026"}.
	 *
	 * <b>Penanganan error:</b> Jika konfigurasi nomor surat tidak ada, fallback ke barcode
	 * random yang dijamin unik secara statistik. Tidak ada penanganan error eksplisit untuk
	 * kegagalan database.
	 *
	 * <b>Pemeliharaan:</b> Format kode dikonfigurasi di tabel {@code nomor_surat} via UI admin.
	 * Placeholder "JENIS PO" adalah konvensi kode yang dapat diubah di konfigurasi format
	 * nomor surat tanpa perlu mengubah kode Java.
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA_DATA == null
				|| NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PERJANJIAN_KERJASAMA_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());

		if (perjanjianKerjasamaMasterAsset != null
				&& perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset() != null) {
			noAgenda = noAgenda.replaceAll("JENIS PO",
					perjanjianKerjasamaMasterAsset.getJenisPerjanjianKerjasamaAsset().getKode());
		}

		return ais.action.master.KodeUnikUtil.pastikanUnik(PerjanjianKerjasamaMasterAsset.class, noAgenda);
	}

	/**
	 * <b>Tujuan:</b> Menghitung indeks urutan berikutnya untuk penomoran kode perjanjian
	 * kerjasama berdasarkan jumlah data yang sudah ada, dengan mempertimbangkan aturan
	 * reset urutan (per tahun, per bulan, atau dari tanggal tertentu) yang dikonfigurasi
	 * pada objek {@link NomorSurat}.
	 *
	 * <b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code nomorSurat} null, mengembalikan 0 sebagai nilai aman.</li>
	 *   <li>Mendapatkan tahun dan bulan saat ini dari {@link WaktuUtil#getCalendar()}.</li>
	 *   <li>Membangun query Criteria terhadap {@link PerjanjianKerjasamaMasterAsset} dengan
	 *       join ke {@code nomorSuratAlurPengadaan} dan {@code nomorSurat}.</li>
	 *   <li>Menerapkan filter berdasarkan konfigurasi {@code nomorSurat}:
	 *     <ul>
	 *       <li>Jika {@code urutBerdasarkanNomor}: filter by nomor surat spesifik.</li>
	 *       <li>Jika {@code urutBerdasarkanKelompok} dengan kelompok tersedia: filter by
	 *           kelompok nomor surat.</li>
	 *       <li>Jika {@code resetUrutanTiapTahun}: filter tahun saat ini.</li>
	 *       <li>Jika {@code resetUrutanTiapBulan}: filter tahun dan bulan saat ini.</li>
	 *       <li>Jika {@code resetTiap} tidak null dan sudah lewat: filter tanggal >= resetTiap.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Menggunakan {@code Projections.rowCount()} untuk menghitung jumlah data
	 *       yang memenuhi kondisi.</li>
	 *   <li>Mengembalikan jumlah tersebut + 1 sebagai indeks berikutnya.</li>
	 * </ol>
	 *
	 * <b>Parameter:</b>
	 * @param nomorSurat Konfigurasi nomor surat yang mendefinisikan aturan penomoran
	 *                   (format, aturan reset, urutan berdasarkan apa). Boleh {@code null}
	 *                   yang akan menyebabkan method mengembalikan 0.
	 * @return Indeks urutan berikutnya (Long), dimulai dari 1 jika belum ada data.
	 *         Nilai 0 dikembalikan hanya jika {@code nomorSurat} null.
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan error eksplisit. Jika query database
	 * gagal, exception akan merambat ke pemanggil ({@code generateCode()}).
	 *
	 * <b>Pemeliharaan:</b> Perhatikan bahwa method ini menghitung berdasarkan {@code rowCount}
	 * (jumlah baris aktif) bukan dari indeks tersimpan. Ini berarti jika ada data yang dihapus,
	 * indeks bisa kembali ke nilai yang pernah dipakai. Untuk keunikan absolut, gunakan
	 * kombinasi dengan {@code KodeUnikUtil.pastikanUnik()} yang sudah diterapkan di
	 * {@code generateCode()}.
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PerjanjianKerjasamaMasterAsset.class)
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
								? Restrictions.ge("tanggal", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * <b>Tujuan:</b> Mengatur flag mode persetujuan dari luar kelas, memungkinkan framework
	 * SOP atau komponen lain untuk mengubah mode tampilan controller dari mode edit menjadi
	 * mode persetujuan (atau sebaliknya) setelah instance sudah dibuat. Mengimplementasikan
	 * kontrak {@link FormSop#setPersetujuan(boolean)}.
	 *
	 * <b>Cara kerja:</b> Mengatur field {@code persetujuan} ke nilai parameter yang diberikan.
	 * Flag ini kemudian dibaca oleh method {@code form()} setiap kali formulir dibangun
	 * untuk menentukan apakah menampilkan komponen input interaktif atau label read-only.
	 * Perubahan ini hanya berlaku efektif jika {@code form()} dipanggil ulang setelahnya.
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan di mana semua
	 *                    field input diganti dengan label read-only dan tombol "Setujui"
	 *                    ditampilkan; {@code false} untuk mode edit normal.
	 *
	 * <b>Return:</b> Void.
	 *
	 * <b>Pemeliharaan:</b> Method ini adalah setter sederhana. Jika ada logika tambahan yang
	 * perlu dijalankan saat mode berubah (misalnya menyegarkan tampilan), tambahkan di sini
	 * atau override di subclass. Saat ini tidak ada subclass yang diketahui.
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
