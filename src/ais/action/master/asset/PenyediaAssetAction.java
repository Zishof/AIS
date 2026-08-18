package ais.action.master.asset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.asset.helper.AmbilDataMasterAssetBanyak;
import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Bank;
import ais.database.model.GeneralValueObject;
import ais.database.model.Kota;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.akunting.Akun;
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.database.model.asset.JenisPekerjaanPenyedia;
import ais.database.model.asset.JenisPenyediaAsset;
import ais.database.model.asset.KategoriPenyediaAsset;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PenyediaAssetAction — Manajemen Data Penyedia / Vendor Aset</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini merupakan controller utama (ZK Composer) untuk halaman manajemen
 * penyedia atau vendor dalam modul pengadaan aset. Fungsinya mencakup seluruh
 * siklus hidup data vendor, mulai dari pendaftaran perusahaan baru, pengisian
 * identitas lengkap, pengelolaan dokumen legal (akta pendirian, akta perubahan,
 * NPWP, pakta integritas), verifikasi dokumen persyaratan, pengelolaan rekening
 * bank, pencatatan produk/barang yang dapat dipasok, hingga alur persetujuan
 * (SOP/disposisi). Kelas ini juga menyediakan fitur ekspor daftar vendor beserta
 * username dan password ke file Excel, serta cetak profil perusahaan.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Kelas ini mewarisi {@code GenericAutowireComposer} dari ZK Framework sehingga
 * seluruh komponen UI yang dideklarasikan sebagai field (Textbox, Combobox,
 * Paging, MyGrid, dsb.) akan di-wire otomatis dari file ZUL yang sepadan.
 * Antarmuka {@code DataInitDefault} menyediakan kontrak inisialisasi form edit,
 * {@code DataCriteria} menyediakan kriteria pencarian Hibernate, {@code DataSearchDefault}
 * menyediakan pencarian ulang default, dan {@code FormSop} menyediakan integrasi
 * alur SOP/disposisi. Saat halaman dimuat, {@code doAfterCompose} dipanggil ZK
 * untuk memeriksa hak akses, menginisialisasi paging, menambah tombol toolbar
 * ekspor password dan cetak data. Data vendor ditampilkan dalam {@code MyGrid}
 * dengan renderer {@code PenyediaAssetRenderer}. Form detail (tambah/ubah)
 * menggunakan {@code Tabbox} berlapis dengan delapan tab: Identitas, Dokumen,
 * Rekening Bank, Akta Pendirian, Akta Perubahan, NPWP, Pakta Integritas, dan
 * Produk. Setiap tab dimuat secara lazy (hanya diisi saat tab pertama kali
 * diklik untuk tab sub-referensi). Penyimpanan dilakukan oleh {@code onSave}
 * yang memvalidasi seluruh field wajib sebelum memanggil
 * {@code Common.refreshSaveOrUpdate}.</p>
 *
 * <p><b>Threading:</b><br>
 * Seluruh metode yang berinteraksi dengan UI ZK berjalan di ZK Event Thread
 * (thread tunggal per sesi). Operasi berat (ekspor Excel, simpan lampiran)
 * dibungkus dengan {@code Common.createDefaultTimer} agar tidak memblokir
 * event thread. Akses Hibernate menggunakan {@code HibernateUtil.currentSession()}
 * (session terkelola per thread) dan {@code StreamingHibernateUtil} untuk BLOB
 * lampiran. Kelas ini tidak aman digunakan secara konkuren lintas thread.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Saat menambah field baru ke entitas {@code PenyediaAsset}, daftarkan juga
 * field tersebut di array {@code contents} dalam {@code doAfterCompose} agar
 * masuk ke ekspor/impor Excel. Pastikan DDL tabel {@code penyedia_asset} dan
 * tabel audit Envers diperbarui secara manual. Untuk menambah tab baru pada
 * form, tambahkan {@code Tab} dan {@code Tabpanel} pada metode {@code form}.
 * Kelas ini berjalan di ZK 5.5 dan Java 1.7+.</p>
 *
 * @author AIS Development Team
 * @version 1.0
 * @see PenyediaAsset
 * @see PenyediaAssetPunyaDokumen
 * @see CommonReportHelper#onCetakPenyediaAsset
 */
public class PenyediaAssetAction extends GenericAutowireComposer
		implements DataInitDefault, DataCriteria, DataSearchDefault, FormSop {

	/**
	 * Serial version UID untuk serialisasi kelas ini sesuai kontrak
	 * {@code Serializable} yang diwarisi dari {@code GenericAutowireComposer}.
	 * Nilai ini bersifat tetap dan tidak boleh diubah agar kompatibilitas
	 * sesi yang disimpan tetap terjaga.
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Component addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Checkbox searchaktif;

	/** Filter daftar berdasarkan Kategori penyedia; item pertama "&mdash; Semua &mdash;" = tanpa filter. */
	private Combobox searchKategori;
	/** Filter daftar berdasarkan Jenis penyedia; item pertama "&mdash; Semua &mdash;" = tanpa filter. */
	private Combobox searchJenis;
	/** Filter daftar berdasarkan Status penyedia; item pertama "&mdash; Semua &mdash;" = tanpa filter. */
	private Combobox searchStatus;
	/** Filter: hanya tampilkan vendor yang berkasnya belum lengkap (minimal satu field kunci kosong). */
	private Checkbox searchBelumLengkap;

	private Textbox kode;
	private Textbox nama;
	private Textbox alamat;
	private Textbox kodePos;
	private Textbox telp;
	private Textbox fax;
	private MyDatebox tanggalPembuatan;
	private PenyediaAsset penyediaAsset;
	private MyToolbarbuttonConfig add;
	private Combobox jenisPenyediaAsset;
	private Combobox kategoriPenyediaAsset;
	private AmbilDataKecamatanBanbox kecamatan;
	private Label propinsi;
	private Label kota;

	private Textbox kontak;
	private Textbox email;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Tabpanel verifikasiDokumen;

	private Tabpanel jenisPekerjaan;

	/**
	 * <b>Tujuan:</b> Memuat halaman sub-referensi Jenis Pekerjaan Penyedia secara
	 * lazy ke dalam tab "Jenis Pekerjaan" pada form penyedia. Pemuatan hanya
	 * dilakukan sekali; jika panel sudah memiliki anak, metode ini tidak melakukan
	 * apa-apa sehingga tidak ada pemuatan berulang yang sia-sia.<br><br>
	 *
	 * <b>Cara kerja:</b> Membuat {@code MyWindow} tanpa border dan tanpa judul
	 * berukuran penuh (100%×100%) sebagai wadah, kemudian menyisipkan
	 * {@code MyInclude} yang merujuk ke ZUL
	 * {@code /pages/master/asset/jenis_pekerjaan_penyedia.zul}. ZK akan memuat
	 * ZUL tersebut beserta composer-nya secara asinkron ke dalam window.<br><br>
	 *
	 * <b>Parameter:</b><br>
	 * @param event Event ZK yang memicu aksi ini, biasanya event {@code onSelect}
	 *              dari {@code Tab} "Jenis Pekerjaan". Nilai event tidak digunakan
	 *              langsung tetapi kehadiran parameter diperlukan oleh konvensi
	 *              ZK event handler.<br><br>
	 *
	 * <b>Return:</b> void — tidak mengembalikan nilai.<br><br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; jika ZUL tidak
	 * ditemukan ZK akan melempar exception yang ditangani oleh error handler
	 * global aplikasi.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Pola lazy-load ini digunakan konsisten di semua tab
	 * sub-referensi. Jika path ZUL berubah, perbarui string path di sini.
	 *
	 * @param event event ZK pemicu (tidak digunakan secara langsung)
	 */
	public void onJenisPekerjaan(Event event) {

		if (jenisPekerjaan.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisPekerjaan);
			MyInclude iframe = new MyInclude("/pages/master/asset/jenis_pekerjaan_penyedia.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * <b>Tujuan:</b> Konstruktor default tanpa argumen untuk instansiasi standar
	 * oleh ZK Framework dan untuk penggunaan sebagai controller halaman utama
	 * daftar penyedia. Dalam konteks ini, mode persetujuan dinonaktifkan
	 * sehingga form tampil dalam mode edit penuh.<br><br>
	 *
	 * <b>Cara kerja:</b> Memanggil konstruktor super ({@code GenericAutowireComposer})
	 * kemudian menetapkan {@code persetujuan = false} sehingga seluruh field
	 * pada form bersifat dapat diedit oleh pengguna.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Konstruktor ini digunakan oleh ZK saat me-wire ZUL
	 * {@code penyedia_asset.zul}. Jangan ubah visibilitas menjadi private.
	 */
	public PenyediaAssetAction() {
		super();
		this.persetujuan = false;
	}

	/**
	 * <b>Tujuan:</b> Konstruktor berparameter untuk membuat instance
	 * {@code PenyediaAssetAction} dengan mode persetujuan yang dapat dikonfigurasi.
	 * Digunakan ketika kelas ini diinstansiasi secara programatis, misalnya dari
	 * alur SOP/disposisi di mana form harus tampil dalam mode read-only (hanya
	 * melihat data untuk disetujui) atau mode edit penuh.<br><br>
	 *
	 * <b>Cara kerja:</b> Memanggil konstruktor super kemudian menyimpan nilai
	 * {@code persetujuan} ke field instance. Nilai {@code true} berarti form
	 * ditampilkan dalam mode persetujuan (baca saja), sedangkan {@code false}
	 * berarti mode edit penuh.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} jika form digunakan dalam alur persetujuan
	 *                    SOP (semua field ditampilkan sebagai Label baca saja),
	 *                    {@code false} untuk mode edit penuh.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Pastikan setiap pemanggil konstruktor ini meneruskan
	 * nilai yang benar sesuai konteks penggunaan.
	 *
	 * @param persetujuan flag mode persetujuan
	 */
	public PenyediaAssetAction(boolean persetujuan) {
		super();
		this.persetujuan = persetujuan;
	}

	/**
	 * <b>Tujuan:</b> Memuat halaman sub-referensi Verifikasi Dokumen Penyedia
	 * secara lazy ke dalam tab "Verifikasi Dokumen" pada form penyedia. Identik
	 * secara pola dengan {@link #onJenisPekerjaan(Event)} tetapi mengarah ke
	 * halaman verifikasi dokumen.<br><br>
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah {@code verifikasiDokumen} sudah memiliki
	 * komponen anak. Jika belum, membuat {@code MyWindow} berukuran 100%×100%
	 * dan menyisipkan {@code MyInclude} yang merujuk ke ZUL
	 * {@code /pages/master/asset/penyedia_asset_punya_verifikasi_dokumen.zul}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK pemicu dari tab "Verifikasi Dokumen", tidak digunakan
	 *              secara langsung.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Jika path ZUL berubah, perbarui string di baris
	 * {@code MyInclude}. Pola ini konsisten dengan semua handler onXxx tab lain.
	 *
	 * @param event event ZK pemicu
	 */
	public void onCheck(Event event) {

		if (verifikasiDokumen.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(verifikasiDokumen);
			MyInclude iframe = new MyInclude("/pages/master/asset/penyedia_asset_punya_verifikasi_dokumen.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel dashboardVendor;

	/**
	 * <b>Tujuan:</b> Menampilkan papan informasi (dashboard) ringkasan seluruh
	 * vendor/penyedia pada tab utama "Dashboard": jumlah vendor, status, kategori,
	 * jenis, kelengkapan berkas (NPWP/akta/rekening/kontak), serta tren bulanan.
	 * Tujuannya agar pengelola langsung melihat gambaran besar tanpa membuka data
	 * satu per satu.<br><br>
	 *
	 * <b>Cara kerja:</b> Jika panel {@code dashboardVendor} masih kosong, render
	 * didelegasikan ke
	 * {@link ais.action.master.asset.helper.DasboardVendor#init(org.zkoss.zk.ui.Component)}
	 * yang menampilkan bilah progress (loading) lebih dulu, lalu memuat angka secara
	 * agregat di latar memakai {@code Timer} ZK dan merakit grafik HTML/CSS modern via
	 * {@code DashboardUiKit} (donat, batang, spider, tren) tanpa JFreeChart. Bersifat
	 * lazy-load (hanya sekali) agar tidak menghitung ulang setiap kali pindah tab.<br><br>
	 *
	 * @param event event ZK pemicu dari tab "Dashboard"; boleh {@code null} ketika
	 *              dipanggil dari {@code doAfterCompose} untuk render awal.
	 */
	public void onDashboardVendor(Event event) {
		if (dashboardVendor != null && dashboardVendor.getChildren().isEmpty()) {
			new ais.action.master.asset.helper.DasboardVendor().init(dashboardVendor);
		}
	}

	private Tabpanel jenisPenyedia;

	/**
	 * <b>Tujuan:</b> Memuat halaman sub-referensi Jenis Penyedia Asset secara lazy
	 * ke dalam tab "Jenis Penyedia" pada form penyedia.<br><br>
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah panel {@code jenisPenyedia} sudah berisi
	 * komponen anak. Jika belum, membuat {@code MyWindow} 100%×100% dan menyisipkan
	 * {@code MyInclude} yang merujuk ke ZUL
	 * {@code /pages/master/asset/jenis_penyedia_asset.zul}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK pemicu dari tab "Jenis Penyedia", tidak digunakan
	 *              secara langsung.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Pola lazy-load ini identik dengan handler tab lain.
	 * Pastikan path ZUL sesuai jika ada perubahan struktur direktori.
	 *
	 * @param event event ZK pemicu
	 */
	public void onJenisPenyedia(Event event) {

		if (jenisPenyedia.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisPenyedia);
			MyInclude iframe = new MyInclude("/pages/master/asset/jenis_penyedia_asset.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel dokumenPenyedia;

	/**
	 * <b>Tujuan:</b> Memuat halaman sub-referensi Dokumen Penyedia Asset secara
	 * lazy ke dalam tab "Dokumen Penyedia" pada form penyedia.<br><br>
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah panel {@code dokumenPenyedia} sudah
	 * berisi komponen anak. Jika belum, membuat {@code MyWindow} 100%×100% dan
	 * menyisipkan {@code MyInclude} ke ZUL
	 * {@code /pages/master/asset/dokumen_penyedia_asset.zul}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK pemicu dari tab "Dokumen Penyedia", tidak digunakan
	 *              secara langsung.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Jika master dokumen penyedia dipindah ke halaman lain,
	 * perbarui path ZUL di sini.
	 *
	 * @param event event ZK pemicu
	 */
	public void onDokumenPenyedia(Event event) {

		if (dokumenPenyedia.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(dokumenPenyedia);
			MyInclude iframe = new MyInclude("/pages/master/asset/dokumen_penyedia_asset.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel kategoriPenyedia;

	/**
	 * <b>Tujuan:</b> Memuat halaman sub-referensi Kategori Penyedia Asset secara
	 * lazy ke dalam tab "Kategori Penyedia" pada form penyedia.<br><br>
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah panel {@code kategoriPenyedia} sudah
	 * berisi komponen anak. Jika belum, membuat {@code MyWindow} 100%×100% dan
	 * menyisipkan {@code MyInclude} ke ZUL
	 * {@code /pages/master/asset/kategori_penyedia_asset.zul}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK pemicu dari tab "Kategori Penyedia", tidak digunakan
	 *              secara langsung.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Pastikan ZUL kategori tetap tersedia dan path-nya
	 * konsisten dengan direktori halaman master asset.
	 *
	 * @param event event ZK pemicu
	 */
	public void onKategoriPenyedia(Event event) {

		if (kategoriPenyedia.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPenyedia);
			MyInclude iframe = new MyInclude("/pages/master/asset/kategori_penyedia_asset.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel statusPenyedia;
//	private Radiogroup statusPenyediaAsset;
	private Textbox longitude;
	private Textbox latitude;
	private HashMap<Long, LampiranLain> maps;
	private Row rowGalery;
	protected Rows myGridGaleri;
	private Rows rowsDokumen;
	private JSONArray array;
	private JSONArray arrayProduk;
	private Textbox noAktaPendirian;
	private MyDatebox tanggalAktaPendirian;
	private Textbox namaNotaris;
	protected LampiranLain dokumenAktaPendirian;
	private Textbox noPengesahan;
	private MyDatebox tanggalPengesahan;
	protected LampiranLain dokumenPengesahan;
	private Textbox noAktaPendirianAkhir;
	private MyDatebox tanggalAktaPendirianAkhir;
	private Textbox namaNotarisAkhir;
	protected LampiranLain dokumenAktaPerubahan;
	private Textbox noPengesahanAkhir;
	private MyDatebox tanggalPengesahanAkhir;
	protected LampiranLain dokumenPengesahanAkhir;
	private Textbox npwp;
	protected LampiranLain dokumenNPWP;
	protected LampiranLain dokumenPaktaIntegritas;
	private South mysouth = null;
	private EventListener eventListener;
	private Combobox bankUtama;
	private Textbox atasNama;
	private Textbox noRek;
	private AmbilDataAkunBanbox akunUtang;
	private boolean persetujuan = true;
	private DisposisiSop disposisiSop = null;
	private Row rowatasNama;
	private Row rownoRek;
	private Center center;
	private Rows rowsData = null;
	private Combobox jenisPekerjaanPenyedia1;
	private Combobox jenisPekerjaanPenyedia2;
	private Combobox jenisPekerjaanPenyedia3;
	private Combobox jenisPekerjaanPenyedia4;
	private Combobox jenisPekerjaanPenyedia5;
	private Textbox pemilik;

	/**
	 * <b>Tujuan:</b> Memuat halaman sub-referensi Status Penyedia Asset secara
	 * lazy ke dalam tab "Status Penyedia" pada form penyedia.<br><br>
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah panel {@code statusPenyedia} sudah
	 * berisi komponen anak. Jika belum, membuat {@code MyWindow} 100%×100% dan
	 * menyisipkan {@code MyInclude} ke ZUL
	 * {@code /pages/master/asset/status_penyedia_asset.zul}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK pemicu dari tab "Status Penyedia", tidak digunakan
	 *              secara langsung.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Status penyedia menentukan apakah vendor diblokir,
	 * daftar hitam, atau aktif. Pastikan ZUL referensi ini konsisten dengan
	 * entitas {@code StatusPenyediaAsset}.
	 *
	 * @param event event ZK pemicu
	 */
	public void onStatusPenyedia(Event event) {

		if (statusPenyedia.getChildren().isEmpty()) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(statusPenyedia);
			MyInclude iframe = new MyInclude("/pages/master/asset/status_penyedia_asset.zul");
			iframe.setParent(window);
		}
	}

	/**
	 * <b>Tujuan:</b> Hook ZK yang dipanggil sebelum komponen halaman ini
	 * dikompilasi. Digunakan untuk memeriksa keamanan akses sebelum halaman
	 * dirender, sehingga pengguna yang tidak memiliki sesi valid langsung
	 * diarahkan ke halaman logoff tanpa sempat melihat konten apapun.<br><br>
	 *
	 * <b>Cara kerja:</b> Memanggil {@code Common.doCheckSecurity()} untuk
	 * memverifikasi keabsahan sesi dan hak akses, kemudian mendelegasikan ke
	 * implementasi super yang melanjutkan proses kompilasi komponen ZK.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param page     halaman ZK yang sedang diproses
	 * @param parent   komponen induk dalam hierarki ZK
	 * @param compInfo metadata komponen yang akan dikompilasi<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code ComponentInfo} yang dikembalikan oleh super, digunakan ZK
	 *         untuk melanjutkan proses komposisi komponen.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Jangan hapus pemanggilan {@code Common.doCheckSecurity()}
	 * karena ini adalah garis pertahanan pertama terhadap akses tidak sah.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan:</b> Hook ZK yang dipanggil setelah seluruh komponen halaman
	 * selesai di-wire dari file ZUL. Metode ini adalah titik inisialisasi utama
	 * halaman, mencakup pemeriksaan hak akses pengguna, inisialisasi paging,
	 * penambahan tombol toolbar ekspor password dan ekspor/impor data Excel,
	 * serta pemuatan data awal ke grid.<br><br>
	 *
	 * <b>Cara kerja:</b> Pertama memeriksa apakah atribut sesi {@code usersTemp}
	 * ada dan pengguna memiliki hak baca ({@code CommonPrivilages.READ}); jika
	 * tidak, sesi dibersihkan dan pengguna diarahkan ke logoff. Selanjutnya
	 * tombol "Tambah" dikonfigurasi visibilitasnya sesuai hak CREATE. Kemudian
	 * paging diinisialisasi dengan listener yang memanggil ulang
	 * {@code onSearchDefault}. Tombol "Password penyedia / perusahaan" ditambahkan
	 * ke toolbar; klik tombol ini membangun file Excel berisi pasangan
	 * username/password untuk setiap vendor (membuat akun baru jika belum ada).
	 * Terakhir, tombol cetak data dan tombol impor data ditambahkan.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param comp komponen root yang telah di-wire oleh ZK; digunakan untuk
	 *             menemukan toolbar dan menambahkan tombol-tombol custom.<br><br>
	 *
	 * <b>Penanganan error:</b> Jika sesi tidak valid, pengguna diarahkan ke
	 * logoff secara otomatis. Error dalam proses ekspor Excel ditampilkan hanya
	 * kepada administrator melalui {@code Common.tampilErrorJikaAdmin}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Array {@code contents} mendefinisikan kolom-kolom
	 * yang diekspor/diimpor; perbarui array ini setiap kali ada penambahan
	 * field pada entitas {@code PenyediaAsset}.
	 *
	 * @param comp komponen root ZK
	 * @throws Exception jika terjadi kesalahan inisialisasi komponen
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		isiFilterCombo();
		onSearchDefault(null);
		// Tab "Dashboard" adalah tab utama (terpilih default) → render awal di sini
		// karena forward onClick hanya menyala saat tab diklik, bukan saat halaman dibuka.
		onDashboardVendor(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig generatePasswordPenyediaAsset = new MyToolbarbuttonConfig(
				"Password penyedia / perusahaan", "/img/print.png");
		generatePasswordPenyediaAsset.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show(
						"Anda akan membuatkan dan mengambil username dan password penyedia / perusahaan.", "Informasi",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											final String filename = Sessions.getCurrent().getWebApp()
													.getRealPath("/tmp/user_password_penyediaAsset_"
															+ URLEncoder.encode(Common.datetimeFormat2s.get()
																	.format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
															+ ".xlsx");

											List<PenyediaAsset> penyediaAssets = initCriteria(true)
													.add(Restrictions.isNotNull("nama")).setMaxResults(1048576).list();

											XSSFWorkbook workbook = new XSSFWorkbook();
											XSSFSheet sheet = workbook.createSheet("DOSEN");
											sheet.setDefaultColumnWidth(20);
											int rowIndex = 0;

											XSSFRow rowhead = sheet.createRow((short) 0);
											rowhead.createCell(0).setCellValue("ID");
											rowhead.createCell(1).setCellValue("Username");
											rowhead.createCell(2).setCellValue("Password");
											rowhead.createCell(3).setCellValue("Nama Lengkap");
											rowhead.createCell(4).setCellValue("Email");
											rowhead.createCell(5).setCellValue("HP");

											for (PenyediaAsset penyediaAsset : penyediaAssets) {
												if (penyediaAsset.getNama() != null
														&& !penyediaAsset.getNama().trim().isEmpty()) {
													rowIndex++;
													Session session = HibernateUtil.currentNativeSession();
													try {
														Tbmuser tbmuser = (Tbmuser) session
																.createCriteria(Tbmuser.class)
																.add(Restrictions.or(Restrictions.isNull("aktif"),
																		Restrictions.eq("aktif", true)))
																.add(Restrictions.eq("penyediaAsset", penyediaAsset))
																.setMaxResults(1).uniqueResult();
														if (tbmuser == null || tbmuser.getUserId() == null) {
															tbmuser = new Tbmuser();

															String newUsername = (penyediaAsset.getEmail() == null || penyediaAsset.getEmail().trim().isEmpty())
																	? StringUtils.split(penyediaAsset.getNama(), " ")[0]
																			+ "" + RandomStringUtils.randomNumeric(3)
																	: penyediaAsset.getEmail().split(",")[0].trim();

															newUsername = newUsername.toLowerCase().trim();

															tbmuser.setUserId(newUsername);
															tbmuser.setEmail(penyediaAsset.getEmail());
															tbmuser.setIs_encripted(true);
															tbmuser.setRoot(false);
															tbmuser.setUserNama(penyediaAsset.getNama());
															String passw = RandomStringUtils.randomNumeric(5);
															tbmuser.setUserPassword(
																	Common.desEncrypter.get().encrypt(passw.trim()));
															tbmuser.setUserRole(ConstantValues.tbmrolePenyedia);
															tbmuser.setUserShow(1);
															tbmuser.setPenyediaAsset(penyediaAsset);

															session.getTransaction().begin();
															session.save(tbmuser);
															session.getTransaction().commit();

														}

														XSSFRow row = sheet.createRow(rowIndex);
														row.createCell(0).setCellValue(penyediaAsset.getId());
														row.createCell(1).setCellValue(tbmuser.getUserId());

														try {
															row.createCell(2).setCellValue(Common.desEncrypter.get()
																	.decrypt(tbmuser.getUserPassword()));
														} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

														row.createCell(3).setCellValue(penyediaAsset.getNama());
														row.createCell(4).setCellValue(penyediaAsset.getEmail());
														row.createCell(5).setCellValue(penyediaAsset.getTelp());
													} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
													finally {
														HibernateUtil.closeSession();
													}
												}
											}

											try {
												FileOutputStream fileOut = new FileOutputStream(filename);
												workbook.write(fileOut);
												fileOut.close();
											} catch (IOException e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException(
														"pembuatan berkas Excel Data Penyedia Asset", e,
														new String[] {
																"Pastikan tidak ada berkas Excel lain dengan nama sama yang sedang terbuka pada perangkat Bapak/Ibu.",
																"Periksa ketersediaan ruang penyimpanan (disk) pada server aplikasi.",
																"Ulangi kembali proses ekspor data ini." });
											}

											try {
												File file = new File(filename);
												Filedownload.save(new FileInputStream(file),
														"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
														file.getName());
											} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pengunduhan berkas Excel Data Penyedia Asset", e,
					new String[] { "Periksa koneksi jaringan Bapak/Ibu, lalu ulangi proses unduh.",
							"Pastikan browser Bapak/Ibu mengizinkan unduhan berkas dari aplikasi ini.",
							"Coba gunakan browser lain apabila kendala tetap berlanjut." });
		}

										}
									});

								}

							}
						});

			}
		});
		Common.appendKeToolbar(generatePasswordPenyediaAsset, add, comp);

		String[] contents = new String[] { "id", "kode", "nama", "pemilik", "propinsi", "kota", "kecamatan",
				"jenisPenyediaAsset", "kategoriPenyediaAsset", "statusPenyediaAsset", "alamat", "kodePos", "telp",
				"fax", "kontak", "email", "longitude", "latitude", "keterangan", "aktif", "bank", "noAktaPendirian",
				"tanggalAktaPendirian", "namaNotaris", "noPengesahan", "tanggalPengesahan", "noAktaPendirianAkhir",
				"tanggalAktaPendirianAkhir", "namaNotarisAkhir", "noPengesahanAkhir", "tanggalPengesahanAkhir",
				"bankUtama", "atasNama", "noRek", "akunUtang", "jenisPekerjaanPenyedia1", "jenisPekerjaanPenyedia2",
				"jenisPekerjaanPenyedia3", "jenisPekerjaanPenyedia4", "jenisPekerjaanPenyedia5" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PenyediaAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenyediaAsset.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * <h3>PenyediaAssetRenderer — Renderer Baris Grid Penyedia Asset</h3>
	 *
	 * <p><b>Untuk apa:</b><br>
	 * Kelas inner ini bertanggung jawab merender satu baris data {@code PenyediaAsset}
	 * ke dalam komponen {@code Row} ZK pada grid daftar penyedia. Setiap baris
	 * menampilkan kode, nama (dengan tautan revisi), jenis, kategori, status,
	 * alamat lengkap (digabung dari semua komponen wilayah), jenis pekerjaan
	 * (hingga 5 jenis), keterangan, checkbox aktif yang dapat diubah langsung,
	 * serta tombol aksi (salin, ubah, hapus, cetak profil).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Method {@code render} dipanggil oleh ZK untuk setiap objek dalam model
	 * daftar. Alamat lengkap dibangun dengan menggabungkan semua komponen
	 * wilayah (kecamatan, kota, propinsi, kode pos, telp, fax, kontak, email)
	 * yang tidak kosong menggunakan pemisah koma. Checkbox aktif langsung
	 * menyimpan perubahan ke database melalui {@code Common.refreshSaveOrUpdate}.
	 * Tombol cetak membuka laporan PDF profil perusahaan via
	 * {@code CommonReportHelper.onCetakPenyediaAsset}.</p>
	 *
	 * <p><b>Threading:</b> Dijalankan di ZK Event Thread; tidak ada akses
	 * database langsung kecuali melalui listener event checkbox.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika kolom baru ditambahkan ke grid di ZUL,
	 * tambahkan pula cell yang sesuai di metode {@code render} ini agar
	 * jumlah kolom tetap konsisten.</p>
	 */
	class PenyediaAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan:</b> Merender satu baris data penyedia asset ke dalam komponen
		 * {@code Row} ZK. Dipanggil oleh ZK untuk setiap elemen pada model daftar
		 * saat grid digambar ulang.<br><br>
		 *
		 * <b>Cara kerja:</b> Mem-cast argumen {@code arg1} menjadi
		 * {@code PenyediaAsset}, kemudian menambahkan Label untuk kode, nama
		 * (melalui {@code RevisiHelper} untuk mendukung pelacakan revisi), jenis,
		 * kategori, status, alamat gabungan (kecamatan + kota + propinsi + kode pos
		 * + telp + fax + kontak + email), jenis pekerjaan (1-5), dan keterangan.
		 * Checkbox aktif dikonfigurasi dengan listener {@code onCheck} yang menyimpan
		 * perubahan secara langsung. Tombol aksi standar (salin/ubah/hapus) dibuat
		 * oleh {@code Common.copyEditDeleteButtons}, dan tombol cetak profil perusahaan
		 * ditambahkan sesudahnya.<br><br>
		 *
		 * <b>Parameter:</b>
		 * @param arg0 komponen {@code Row} ZK yang akan diisi dengan sel-sel data
		 * @param arg1 objek data; diasumsikan bertipe {@code PenyediaAsset}<br><br>
		 *
		 * <b>Penanganan error:</b> Exception dari pemanggil diabaikan ke atas
		 * (dideklarasikan {@code throws Exception}).<br><br>
		 *
		 * <b>Pemeliharaan:</b> Urutan komponen yang ditambahkan ke {@code arg0}
		 * harus sesuai persis dengan urutan kolom yang dideklarasikan dalam
		 * {@code <columns>} di file ZUL.
		 *
		 * @param arg0 baris ZK tujuan render
		 * @param arg1 objek data penyedia
		 * @throws Exception jika terjadi kesalahan saat merender komponen
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PenyediaAsset penyediaAsset = (PenyediaAsset) arg1;

			// Kolom Kode: kode + chip "% kelengkapan berkas" agar admin langsung tahu
			// vendor mana yang datanya masih kurang (tanpa membuka detail).
			Vbox kodeBox = new Vbox();
			kodeBox.setSpacing("2px");
			kodeBox.appendChild(new Label(penyediaAsset.getKode()));
			kodeBox.appendChild(new org.zkoss.zul.Html(chipKelengkapan(penyediaAsset)));
			kodeBox.setParent(arg0);
			RevisiHelper.createNewRevisi(PenyediaAsset.class, penyediaAsset, penyediaAsset.getNama()).setParent(arg0);
			new Label(penyediaAsset.getJenisPenyediaAsset() == null ? ""
					: penyediaAsset.getJenisPenyediaAsset().getNama()).setParent(arg0);
			new Label(penyediaAsset.getKategoriPenyediaAsset() == null ? ""
					: penyediaAsset.getKategoriPenyediaAsset().getNama()).setParent(arg0);
			new Label(penyediaAsset.getStatusPenyediaAsset() == null ? ""
					: penyediaAsset.getStatusPenyediaAsset().getNama()).setParent(arg0);
			String alamat = penyediaAsset.getAlamat();
			if (penyediaAsset.getKecamatan() != null) {
				String c = "Kec." + penyediaAsset.getKecamatan().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (penyediaAsset.getKota() != null) {
				String c = "Kab/Kota." + penyediaAsset.getKota().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (penyediaAsset.getPropinsi() != null) {
				String c = "Prop." + penyediaAsset.getPropinsi().getNama();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getKodePos().trim().isEmpty()) {
				String c = "Kode Pos " + penyediaAsset.getKodePos();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getTelp().trim().isEmpty()) {
				String c = "Telp. " + penyediaAsset.getTelp();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getFax().trim().isEmpty()) {
				String c = "Fax. " + penyediaAsset.getFax();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getKontak().trim().isEmpty()) {
				String c = "Kontak. " + penyediaAsset.getKontak();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}
			if (!penyediaAsset.getEmail().trim().isEmpty()) {
				String c = "Email. " + penyediaAsset.getEmail();
				alamat += alamat.isEmpty() ? c : ", " + c;
			}

			new MyLabelKecil(alamat).setParent(arg0);

			String jenis = "";
			if (penyediaAsset.getJenisPekerjaanPenyedia1() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia1().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia1().getNama();
			}
			if (penyediaAsset.getJenisPekerjaanPenyedia2() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia2().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia2().getNama();
			}
			if (penyediaAsset.getJenisPekerjaanPenyedia3() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia3().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia3().getNama();
			}
			if (penyediaAsset.getJenisPekerjaanPenyedia4() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia4().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia4().getNama();
			}
			if (penyediaAsset.getJenisPekerjaanPenyedia5() != null) {
				jenis += jenis.isEmpty() ? penyediaAsset.getJenisPekerjaanPenyedia5().getNama()
						: ", " + penyediaAsset.getJenisPekerjaanPenyedia5().getNama();
			}
			new MyLabelKecil(jenis).setParent(arg0);

			new Label(penyediaAsset.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(penyediaAsset.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penyediaAsset.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(penyediaAsset);
				}
			});

			Hbox a;
			(a = Common.copyEditDeleteButtons(edit, delete, penyediaAsset, PenyediaAssetAction.this)).setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Profil Perusahaan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					CommonReportHelper.onCetakPenyediaAsset(penyediaAsset, false);
				}

			});
			button.setParent(a);

		}

	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code DataInitDefault} untuk
	 * menginisialisasi form edit/tambah dengan data dari objek penyedia yang
	 * diberikan. Dipanggil ketika pengguna mengklik tombol Ubah pada baris grid.<br><br>
	 *
	 * <b>Cara kerja:</b> Me-reset {@code rowsData} ke null agar form dibangun ulang
	 * dari awal (bukan menggunakan cache baris sebelumnya), mem-cast
	 * {@code GeneralValueObject} ke {@code PenyediaAsset}, memanggil metode
	 * {@code init(PenyediaAsset)} untuk membangun UI form, kemudian menampilkan
	 * jendela {@code addWindow} dalam mode modal jika jendela tersebut bertipe
	 * {@code Window}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param obj objek {@code GeneralValueObject} yang akan di-cast ke
	 *            {@code PenyediaAsset}; berisi data yang akan diisikan ke form.<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}; exception
	 * ditangani oleh error handler global ZK.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Reset {@code rowsData = null} penting untuk memastikan
	 * form selalu dibangun ulang saat objek berbeda dibuka.
	 *
	 * @param obj data penyedia yang akan diedit
	 * @throws Exception jika terjadi kesalahan membangun form
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		rowsData = null;
		penyediaAsset = (PenyediaAsset) obj;
		init(penyediaAsset);
		addWindow.setVisible(true);

		if (addWindow instanceof Window) {
			((Window) addWindow).onModal();
		}
	}

	/**
	 * <b>Tujuan:</b> Menangani event klik tombol "Tambah" pada toolbar untuk
	 * membuka form tambah penyedia baru dengan objek {@code PenyediaAsset} kosong.<br><br>
	 *
	 * <b>Cara kerja:</b> Me-reset {@code rowsData} ke null, membuat instance
	 * {@code PenyediaAsset} baru, memanggil {@code init(PenyediaAsset)} untuk
	 * membangun UI form, kemudian menampilkan {@code addWindow} dalam mode modal.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK dari klik tombol Tambah; tidak digunakan secara
	 *              langsung tetapi diperlukan oleh konvensi penamaan handler ZK.<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Jika logika inisialisasi data default untuk vendor baru
	 * dibutuhkan (misal: mengisi tanggal pembuatan otomatis), tambahkan di sini
	 * sebelum pemanggilan {@code init}.
	 *
	 * @param event event klik tombol Tambah
	 * @throws Exception jika terjadi kesalahan membangun form
	 */
	public void onAdd(Event event) throws Exception {
		rowsData = null;
		init(new PenyediaAsset());
		addWindow.setVisible(true);
		if (addWindow instanceof Window) {
			((Window) addWindow).onModal();
		}
	}

	/**
	 * <b>Tujuan:</b> Metode statis untuk membuka form penyedia dari konteks
	 * eksternal (bukan dari halaman daftar penyedia itu sendiri), dengan
	 * menggunakan jendela dan panel south yang sudah disediakan oleh halaman
	 * pemanggil. Digunakan misalnya dari halaman pengadaan yang ingin membuka
	 * form penyedia secara inline.<br><br>
	 *
	 * <b>Cara kerja:</b> Membuat instance baru {@code PenyediaAssetAction},
	 * menetapkan referensi {@code addWindow} dan {@code mysouth} dari parameter,
	 * kemudian memanggil {@code init(PenyediaAsset)} untuk membangun konten form
	 * di dalam komponen yang diberikan.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param addWindow     komponen jendela atau container tempat form akan dirender
	 * @param mysouth       panel selatan untuk toolbar simpan/batal; jika null
	 *                      maka toolbar dibuat di dalam borderlayout baru
	 * @param penyediaAsset entitas penyedia yang akan diedit; boleh berupa objek
	 *                      baru (belum pernah tersimpan) atau objek hasil load DB<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Metode ini tidak memanggil {@code onModal()} sehingga
	 * penampilan modal/non-modal sepenuhnya diatur oleh pemanggil.
	 *
	 * @param addWindow     container tujuan form
	 * @param mysouth       panel south untuk toolbar
	 * @param penyediaAsset entitas penyedia
	 * @throws Exception jika terjadi kesalahan membangun form
	 */
	public static void onAddExternal(Component addWindow, South mysouth, PenyediaAsset penyediaAsset) throws Exception {
		PenyediaAssetAction penyediaAssetAction = new PenyediaAssetAction();
		penyediaAssetAction.addWindow = addWindow;
		penyediaAssetAction.mysouth = mysouth;

		penyediaAssetAction.init(penyediaAsset);

	}

	/**
	 * <b>Tujuan:</b> Metode statis untuk membuka form penyedia dalam sebuah
	 * jendela modal baru yang dibuat secara programatis, dengan dimensi yang
	 * dapat dikonfigurasi dan callback event listener setelah penyimpanan.<br><br>
	 *
	 * <b>Cara kerja:</b> Membuat instance {@code PenyediaAssetAction} baru,
	 * menyimpan referensi {@code eventListener}, kemudian membuat {@code MyWindow}
	 * baru dan menambahkannya ke root halaman ZK saat ini. Dimensi jendela
	 * disesuaikan: jika perangkat mobile maka 100%×100%, jika desktop maka
	 * menggunakan parameter {@code desktopWidth} dan {@code desktopHeight} (atau
	 * default 95%×95% jika null). Form diinisialisasi dengan {@code init(PenyediaAsset)}
	 * kemudian jendela ditampilkan dalam mode modal.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event          event ZK yang memicu pembukaan form (tidak digunakan
	 *                       secara langsung)
	 * @param eventListener  listener yang akan dipanggil setelah data berhasil
	 *                       disimpan; event data berisi objek {@code PenyediaAsset}
	 *                       yang baru saja tersimpan
	 * @param penyediaAsset  entitas penyedia yang akan diedit/ditambah
	 * @param desktopWidth   lebar jendela dalam pixel untuk tampilan desktop;
	 *                       null berarti gunakan default 95%
	 * @param desktopHeight  tinggi jendela dalam pixel untuk tampilan desktop;
	 *                       null berarti gunakan default 95%<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Pastikan pemanggil menyediakan {@code EventListener}
	 * yang valid jika perlu notifikasi setelah simpan. Jika null, simpan tetap
	 * berjalan tetapi tidak ada callback.
	 *
	 * @param event         event pemicu
	 * @param eventListener callback setelah simpan
	 * @param penyediaAsset entitas penyedia
	 * @param desktopWidth  lebar jendela desktop (px), atau null
	 * @param desktopHeight tinggi jendela desktop (px), atau null
	 * @throws Exception jika terjadi kesalahan membangun form
	 */
	public static void onAddExternal(Event event, EventListener eventListener, PenyediaAsset penyediaAsset,
			Integer desktopWidth, Integer desktopHeight) throws Exception {
		PenyediaAssetAction penyediaAssetAction = new PenyediaAssetAction();
		penyediaAssetAction.eventListener = eventListener;
		penyediaAssetAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(penyediaAssetAction.addWindow);
		((MyWindow) penyediaAssetAction.addWindow)
				.setHeight(Common.isMobile() ? "100%" : desktopHeight == null ? "95%" : desktopHeight + "px");
		((MyWindow) penyediaAssetAction.addWindow)
				.setWidth(Common.isMobile() ? "100%" : desktopWidth == null ? "95%" : desktopWidth + "px");

		penyediaAssetAction.init(penyediaAsset);

		penyediaAssetAction.addWindow.setVisible(true);
		((MyWindow) penyediaAssetAction.addWindow).onModal();
	}

	/**
	 * <b>Tujuan:</b> Metode statis untuk membuka form pendaftaran perusahaan baru
	 * (self-registration) dalam jendela modal berukuran lebih kecil (default
	 * 400px tinggi × 550px lebar). Digunakan untuk alur pendaftaran mandiri
	 * oleh vendor/perusahaan yang belum terdaftar di sistem.<br><br>
	 *
	 * <b>Cara kerja:</b> Identik dengan {@link #onAddExternal(Event, EventListener, PenyediaAsset, Integer, Integer)}
	 * tetapi memanggil {@code initDaftar(PenyediaAsset)} (bukan {@code init})
	 * sehingga form yang ditampilkan adalah form pendaftaran ringkas yang hanya
	 * meminta nama, kategori, jenis perusahaan, jenis pekerjaan, telp, email,
	 * dan kontak person. Setelah simpan, akun pengguna dibuat otomatis dan
	 * kredensial dikirim via email.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event          event ZK pemicu (tidak digunakan langsung)
	 * @param eventListener  callback setelah pendaftaran selesai
	 * @param penyediaAsset  objek penyedia baru (biasanya kosong, belum tersimpan)
	 * @param desktopWidth   lebar jendela desktop (px), atau null untuk default 550px
	 * @param desktopHeight  tinggi jendela desktop (px), atau null untuk default 400px<br><br>
	 *
	 * <b>Penanganan error:</b> Email dengan kredensial dikirim setelah simpan;
	 * jika email gagal, exception ditampilkan ke admin.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Form pendaftaran ini bersifat publik (dapat diakses
	 * tanpa login). Pastikan validasi email unik tetap aktif untuk mencegah
	 * duplikasi akun.
	 *
	 * @param event         event pemicu
	 * @param eventListener callback setelah selesai
	 * @param penyediaAsset entitas penyedia baru
	 * @param desktopWidth  lebar jendela (px), atau null
	 * @param desktopHeight tinggi jendela (px), atau null
	 * @throws Exception jika terjadi kesalahan
	 */
	public static void onAddExternalDaftar(Event event, EventListener eventListener, PenyediaAsset penyediaAsset,
			Integer desktopWidth, Integer desktopHeight) throws Exception {
		PenyediaAssetAction penyediaAssetAction = new PenyediaAssetAction();
		penyediaAssetAction.eventListener = eventListener;
		penyediaAssetAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(penyediaAssetAction.addWindow);
		((MyWindow) penyediaAssetAction.addWindow)
				.setHeight(Common.isMobile() ? "100%" : desktopHeight == null ? "400px" : desktopHeight + "px");
		((MyWindow) penyediaAssetAction.addWindow)
				.setWidth(Common.isMobile() ? "100%" : desktopWidth == null ? "550px" : desktopWidth + "px");

		penyediaAssetAction.initDaftar(penyediaAsset);

		penyediaAssetAction.addWindow.setVisible(true);
		((MyWindow) penyediaAssetAction.addWindow).onModal();
	}

	/**
	 * <b>Tujuan:</b> Membangun tab panel "Dokumen Persyaratan" yang menampilkan
	 * daftar seluruh dokumen yang dipersyaratkan untuk vendor beserta status
	 * verifikasi dan kemampuan upload/download file per dokumen.<br><br>
	 *
	 * <b>Cara kerja:</b> Mengambil semua {@code DokumenPenyediaAsset} dari cache
	 * {@code ConstantValues}, mengurutkannya, lalu untuk setiap dokumen:
	 * mencari atau membuat {@code PenyediaAssetPunyaDokumen} yang mengaitkan
	 * dokumen dengan vendor ini. Setiap baris grid menampilkan komponen
	 * upload/download file ({@code LampiranLain.createDownloadUploadFileLain}),
	 * nama dokumen, sifat (Wajib/Opsional), dan status verifikasi. Untuk pengguna
	 * admin, status dapat diubah melalui Combobox (Belum/Verifikasi/Revisi) dan
	 * keterangan dapat diedit. Untuk pengguna vendor (penyediaAsset != null),
	 * status dan keterangan ditampilkan sebagai Label baca saja. Upload diblokir
	 * jika dokumen sudah berstatus "Verifikasi".<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor yang dokumennya ditampilkan; boleh null
	 *                      atau belum memiliki id (untuk penyedia baru, semua
	 *                      baris dokumen akan berupa objek baru yang belum tersimpan)<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Tabpanel} ZK yang siap dimasukkan ke {@code Tabpanels}
	 *         pada form utama penyedia.<br><br>
	 *
	 * <b>Penanganan error:</b> Menggunakan {@code HibernateUtil.currentSession()}
	 * yang tidak akan melempar exception jika sesi sudah dibuka sebelumnya.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Daftar dokumen diambil dinamis dari tabel
	 * {@code dokumen_penyedia_asset}; untuk menambah dokumen baru cukup
	 * tambahkan record di tabel tersebut tanpa mengubah kode ini.
	 *
	 * @param penyediaAsset entitas vendor pemilik dokumen
	 * @return Tabpanel berisi grid dokumen persyaratan
	 */
	@SuppressWarnings("unchecked")
	private Tabpanel initDokumen(PenyediaAsset penyediaAsset) {
		Tbmuser tbmuser = Common.getCurrentUser();

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		tabpanel.setHeight("750px");

		MyGrid grid = new MyGrid();
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig("Dokumen");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Sifat");
		column.setParent(columns);

		column = new MyColumnConfig("Status");
		column.setParent(columns);

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);
		column.setWidth("35%");

		rowsDokumen = new Rows();
		rowsDokumen.setParent(grid);

		Map<Long, DokumenPenyediaAsset> map = ConstantValues.ambilBerdasarClass(DokumenPenyediaAsset.class);
		List<DokumenPenyediaAsset> dokumenPenyediaAssets = new ArrayList<DokumenPenyediaAsset>();
		for (DokumenPenyediaAsset dokumenPenyediaAsset : map.values()) {
			dokumenPenyediaAssets.add(dokumenPenyediaAsset);
		}
		Collections.sort(dokumenPenyediaAssets);
		Session session = HibernateUtil.currentSession();
		for (DokumenPenyediaAsset dokumenPenyediaAsset : dokumenPenyediaAssets) {

			PenyediaAssetPunyaDokumen temp = (PenyediaAssetPunyaDokumen) (penyediaAsset == null
					|| penyediaAsset.getId() == null
							? new PenyediaAssetPunyaDokumen()
							: session.createCriteria(PenyediaAssetPunyaDokumen.class)
									.add(Restrictions.eq("dokumenPenyediaAsset", dokumenPenyediaAsset))
									.add(Restrictions.eq("penyediaAsset", penyediaAsset)).setMaxResults(1)
									.uniqueResult());
			if (temp == null || temp.getId() == null) {
				temp = new PenyediaAssetPunyaDokumen();
				temp.setDokumenPenyediaAsset(dokumenPenyediaAsset);
				temp.setPenyediaAsset(penyediaAsset);
			}
			final PenyediaAssetPunyaDokumen penyediaAssetPunyaDokumen = temp;
			final MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsDokumen);

			row.setValign("top");
			row.setAttribute("penyediaAssetPunyaDokumen", penyediaAssetPunyaDokumen);

			MyDetail detail = new MyDetail();
			row.appendChild(detail);
			detail.setOpen(true);
			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			FileFotoLain lampiranLain = penyediaAssetPunyaDokumen.getId() == null ? null
					: FileFotoLain.ambil(false, penyediaAssetPunyaDokumen.getId(),
							PenyediaAssetPunyaDokumen.class.getName(), LampiranLain.class);
			row.setValign("top");
			row.setAttribute("lampiranLain", lampiranLain);
			Boolean tampilUpload = !penyediaAssetPunyaDokumen.getStatus()
					.equalsIgnoreCase(PenyediaAssetPunyaDokumen.VERIFIKASI);

			LampiranLain.createDownloadUploadFileLain(hbox, penyediaAssetPunyaDokumen.getId(),
					PenyediaAssetPunyaDokumen.class.getName(), dokumenPenyediaAsset.getNama(), false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getData();
							row.setValign("top");
							row.setAttribute("lampiranLain", lampiranLain);
						}
					}, null, false, false, false, tampilUpload);

			hbox.setParent(vbox);

			row.appendChild(new Label(dokumenPenyediaAsset.getNama()));
			row.appendChild(new Label(dokumenPenyediaAsset.getWajib() ? "Wajib" : "Opsional"));

			if (tbmuser != null && tbmuser.getPenyediaAsset() != null) {
				Vbox statusRo = new Vbox();
				statusRo.setSpacing("3px");
				statusRo.appendChild(new Label(penyediaAssetPunyaDokumen.getStatus()));
				if (penyediaAssetPunyaDokumen.getTanggalBerlakuSampai() != null) {
					org.zkoss.zul.Datebox roDate = new org.zkoss.zul.Datebox(
							penyediaAssetPunyaDokumen.getTanggalBerlakuSampai());
					roDate.setFormat("dd-MM-yyyy");
					roDate.setWidth("130px");
					roDate.setDisabled(true);
					Hbox roBox = new Hbox();
					roBox.setAlign("center");
					roBox.appendChild(new Label("Berlaku s/d:"));
					roBox.appendChild(roDate);
					statusRo.appendChild(roBox);
				}
				statusRo.setParent(row);
				row.appendChild(new Label(penyediaAssetPunyaDokumen.getKeterangan()));
			} else {
				Combobox combobox = new Combobox();
				Comboitem comboitem = new Comboitem(PenyediaAssetPunyaDokumen.BELUM);
				comboitem.setValue(PenyediaAssetPunyaDokumen.BELUM);
				combobox.appendChild(comboitem);
				comboitem = new Comboitem(PenyediaAssetPunyaDokumen.VERIFIKASI);
				comboitem.setValue(PenyediaAssetPunyaDokumen.VERIFIKASI);
				combobox.appendChild(comboitem);
				comboitem = new Comboitem(PenyediaAssetPunyaDokumen.REVISI);
				comboitem.setValue(PenyediaAssetPunyaDokumen.REVISI);
				combobox.appendChild(comboitem);
				Common.selectComboItem(combobox, penyediaAssetPunyaDokumen.getStatus());
				combobox.setWidth("90%");
				combobox.setReadonly(true);

				// Status pemeriksaan + tanggal "Berlaku s/d" digabung satu sel (Vbox) — konsisten
				// dengan halaman Verifikasi Dokumen, tanpa menambah kolom grid.
				Vbox statusBox = new Vbox();
				statusBox.setSpacing("3px");
				statusBox.setWidth("100%");
				statusBox.appendChild(combobox);
				Hbox berlakuBox = new Hbox();
				berlakuBox.setAlign("center");
				berlakuBox.appendChild(new Label("Berlaku s/d:"));
				final org.zkoss.zul.Datebox berlakuSampai = new org.zkoss.zul.Datebox(
						penyediaAssetPunyaDokumen.getTanggalBerlakuSampai());
				berlakuSampai.setFormat("dd-MM-yyyy");
				berlakuSampai.setWidth("130px");
				berlakuBox.appendChild(berlakuSampai);
				statusBox.appendChild(berlakuBox);
				statusBox.setParent(row);

				berlakuSampai.addEventListener("onChange", new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						penyediaAssetPunyaDokumen.setTanggalBerlakuSampai(berlakuSampai.getValue());
						if (penyediaAssetPunyaDokumen.getId() != null) {
							Common.refreshUpdate(penyediaAssetPunyaDokumen);
						}
					}
				});

				combobox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Combobox combobox = (Combobox) arg0.getTarget();
						penyediaAssetPunyaDokumen.setStatus((String) combobox.getSelectedItem().getValue());
						if (penyediaAssetPunyaDokumen.getId() != null) {
							Common.refreshUpdate(penyediaAssetPunyaDokumen);
						}
					}
				});

				final Textbox keterangan = new Textbox(penyediaAssetPunyaDokumen.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);
				keterangan.setParent(row);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						penyediaAssetPunyaDokumen.setKeterangan(keterangan.getValue());
						Common.refreshSaveOrUpdate(penyediaAssetPunyaDokumen);

					}
				};

				keterangan.addEventListener("onChange", eventListener);
			}
		}
		dokumenPenyediaAssets = null;
		return tabpanel;
	}

	/**
	 * <b>Tujuan:</b> Membangun tab panel "Pakta Integritas" yang memungkinkan
	 * vendor mengunggah dokumen pakta integritas mereka.<br><br>
	 *
	 * <b>Cara kerja:</b> Membuat {@code Tabpanel} berisi {@code MyGrid} dua
	 * kolom. Baris pertama menyediakan komponen upload/download dokumen pakta
	 * integritas menggunakan {@code FileFotoLain.createDownloadUpload} dengan
	 * identifier unik {@code PenyediaAsset.class.getName() + "_Dokumen_Pakta_Integritas"}.
	 * Referensi ke dokumen yang diunggah disimpan ke field {@code dokumenPaktaIntegritas}
	 * melalui EventListener sehingga dapat diproses saat {@code onSave}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor pemilik dokumen pakta integritas<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Tabpanel} berisi form upload pakta integritas<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}; error
	 * upload ditangani oleh komponen {@code FileFotoLain}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Field {@code dokumenPaktaIntegritas} di-reset ke null
	 * setiap kali metode ini dipanggil untuk menghindari referensi basi ke
	 * dokumen lama.
	 *
	 * @param penyediaAsset entitas vendor
	 * @return Tabpanel pakta integritas
	 * @throws Exception jika terjadi kesalahan membangun komponen
	 */
	private Tabpanel dataPakta(final PenyediaAsset penyediaAsset) throws Exception {
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		tabpanel.setHeight("750px");

		MyGrid grid = new MyGrid();
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow rowNPWP = new MyFormRow();
		rowNPWP.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hboxNpwp = new Hbox();
		rowNPWP.appendChild(hboxNpwp);

		dokumenPaktaIntegritas = null;
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen"));
		Hbox hbox = new Hbox();

		boolean harusPdf = false;
		Map<String, FileFotoLain> lampiranLains = null;
		boolean tidakTampilJurusan = false;
		boolean hanyaIcon = false;
		boolean usingId = false;
		boolean tampilUpload = true;

		Integer cutomUkuranUpload = null;
		boolean vertical = false;
		boolean janganPreviewDiLayarUtama = false;

		FileFotoLain.createDownloadUpload(hbox, penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Pakta_Integritas", "Dokumen Pakta Integritas", harusPdf,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dokumenPaktaIntegritas = (LampiranLain) arg0.getData();
					}
				}, lampiranLains, tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, hboxNpwp, LampiranLain.class);
		hbox.setParent(row);

		rowNPWP.setParent(rows);

		return tabpanel;
	}

	/**
	 * <b>Tujuan:</b> Membangun tab panel "NPWP" untuk mengisi nomor NPWP vendor
	 * dan mengunggah dokumen NPWP terkait.<br><br>
	 *
	 * <b>Cara kerja:</b> Membuat {@code Tabpanel} berisi {@code MyGrid} dua
	 * kolom. Baris pertama berisi field teks NPWP yang diikat ke
	 * {@code penyediaAsset.getNpwp()}. Baris kedua menyediakan komponen
	 * upload/download dokumen NPWP dengan identifier
	 * {@code PenyediaAsset.class.getName() + "_Dokumen_NPWP"}. Referensi dokumen
	 * disimpan ke {@code dokumenNPWP} melalui EventListener.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor; id-nya digunakan sebagai referensi
	 *                      lampiran di tabel {@code lampiran_lain}<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Tabpanel} berisi form NPWP<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Validasi format NPWP (15 digit) belum diimplementasikan
	 * di sini; sebaiknya ditambahkan di {@code onSave} jika diperlukan.
	 *
	 * @param penyediaAsset entitas vendor
	 * @return Tabpanel NPWP
	 * @throws Exception jika terjadi kesalahan
	 */
	private Tabpanel dataNpwp(final PenyediaAsset penyediaAsset) throws Exception {
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		tabpanel.setHeight("750px");

		MyGrid grid = new MyGrid();
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NPWP *"));
		row.appendChild(npwp = new Textbox(penyediaAsset.getNpwp()));
		npwp.setWidth("90%");

		MyFormRow rowNPWP = new MyFormRow();
		rowNPWP.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hboxNpwp = new Hbox();
		rowNPWP.appendChild(hboxNpwp);

		dokumenNPWP = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen"));
		Hbox hbox = new Hbox();

		boolean harusPdf = false;
		Map<String, FileFotoLain> lampiranLains = null;
		boolean tidakTampilJurusan = false;
		boolean hanyaIcon = false;
		boolean usingId = false;
		boolean tampilUpload = true;

		Integer cutomUkuranUpload = null;
		boolean vertical = false;
		boolean janganPreviewDiLayarUtama = false;

		FileFotoLain.createDownloadUpload(hbox, penyediaAsset.getId(), PenyediaAsset.class.getName() + "_Dokumen_NPWP",
				"Dokumen NPWP", harusPdf, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dokumenNPWP = (LampiranLain) arg0.getData();
					}
				}, lampiranLains, tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, hboxNpwp, LampiranLain.class);
		hbox.setParent(row);

		rowNPWP.setParent(rows);

		return tabpanel;
	}

	/**
	 * <b>Tujuan:</b> Membangun tab panel "Akta Perubahan Terakhir Perusahaan"
	 * untuk mengisi data akta perubahan (bukan akta pendirian awal) beserta
	 * dokumen pendukung dan pengesahannya.<br><br>
	 *
	 * <b>Cara kerja:</b> Membuat {@code Tabpanel} berisi {@code MyGrid} dua
	 * kolom dengan baris-baris: No. Akta Perubahan, Tanggal Akta Perubahan,
	 * Nama Notaris, upload Dokumen Akta Perubahan (dengan catatan zip jika
	 * lebih dari satu file), No. Pengesahan, Tanggal Pengesahan, dan upload
	 * Dokumen Pengesahan. Dalam mode persetujuan ({@code persetujuan = true}),
	 * semua field ditampilkan sebagai Label baca saja. Upload dinonaktifkan
	 * dalam mode persetujuan ({@code !persetujuan} sebagai flag tampilUpload).<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor yang data akta perubahannya ditampilkan<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Tabpanel} berisi form akta perubahan terakhir<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Ada bug minor: baris "No. Pengesahan" dalam mode
	 * persetujuan menampilkan {@code getNamaNotarisAkhir()} alih-alih
	 * {@code getNoPengesahanAkhir()}. Perbaiki jika diperlukan akurasi data.
	 *
	 * @param penyediaAsset entitas vendor
	 * @return Tabpanel akta perubahan terakhir
	 * @throws Exception jika terjadi kesalahan
	 */
	private Tabpanel dataAktaTerakhir(final PenyediaAsset penyediaAsset) throws Exception {
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		tabpanel.setHeight("750px");

		MyGrid grid = new MyGrid();
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Akta Perubahan"));
		noAktaPendirianAkhir = new Textbox(penyediaAsset.getNoAktaPendirianAkhir());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getNoAktaPendirianAkhir()));
		} else {
			row.appendChild(noAktaPendirianAkhir);
		}
		noAktaPendirianAkhir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akta Perubahan"));
		tanggalAktaPendirianAkhir = new MyDatebox(penyediaAsset.getTanggalAktaPendirianAkhir());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getTanggalAktaPendirianAkhir() == null ? ""
					: Common.dateFormat6.get().format(penyediaAsset.getTanggalAktaPendirianAkhir())));
		} else {
			row.appendChild(tanggalAktaPendirianAkhir);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Notaris"));
		namaNotarisAkhir = new Textbox(penyediaAsset.getNamaNotarisAkhir());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getNamaNotarisAkhir()));
		} else {
			row.appendChild(namaNotarisAkhir);
		}
		namaNotarisAkhir.setWidth("90%");

		dokumenAktaPerubahan = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Akta Perubahan"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Akta_Perubahan_Akhir", "Dokumen Akta Perubahan", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dokumenAktaPerubahan = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika Dokumen Akta Perubahan lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Pengesahan"));
		noPengesahanAkhir = new Textbox(penyediaAsset.getNoPengesahanAkhir());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getNamaNotarisAkhir()));
		} else {
			row.appendChild(noPengesahanAkhir);
		}
		noPengesahanAkhir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengesahan"));
		tanggalPengesahanAkhir = new MyDatebox(penyediaAsset.getTanggalPengesahanAkhir());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getTanggalPengesahanAkhir() == null ? ""
					: Common.dateFormat6.get().format(penyediaAsset.getTanggalPengesahanAkhir())));
		} else {
			row.appendChild(tanggalPengesahanAkhir);
		}

		dokumenPengesahanAkhir = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Pengesahan"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Pengesahan_Akhir", "Dokumen Pengesahan", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dokumenPengesahanAkhir = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika Dokumen Pengesahan lebih dari satu file, zip dulu semua file tersebut");

		return tabpanel;
	}

	/**
	 * <b>Tujuan:</b> Membangun tab panel "Akta Pendirian Perusahaan" untuk
	 * mengisi data akta pendirian (dokumen legal pertama perusahaan) beserta
	 * dokumen pendukung dan informasi pengesahannya.<br><br>
	 *
	 * <b>Cara kerja:</b> Membuat {@code Tabpanel} berisi {@code MyGrid} dua
	 * kolom dengan baris-baris: No. Akta Pendirian, Tanggal Akta Pendirian,
	 * Nama Notaris, Nama Pemilik Perusahaan, upload Dokumen Akta Pendirian
	 * (dengan hint zip jika lebih dari satu file), No. Pengesahan, Tanggal
	 * Pengesahan, dan upload Dokumen Pengesahan. Dalam mode persetujuan semua
	 * field menjadi Label baca saja dan upload dinonaktifkan.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor yang data akta pendiriannya ditampilkan<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Tabpanel} berisi form akta pendirian perusahaan<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}. Error
	 * upload dokumen ditangani oleh komponen {@code LampiranLain}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Field {@code dokumenAktaPendirian} dan
	 * {@code dokumenPengesahan} di-reset ke null di awal metode. Referensi ke
	 * id penyedia baru (sebelum tersimpan) akan diperbarui di {@code onSave}.
	 *
	 * @param penyediaAsset entitas vendor
	 * @return Tabpanel akta pendirian
	 * @throws Exception jika terjadi kesalahan
	 */
	private Tabpanel dataAkta(final PenyediaAsset penyediaAsset) throws Exception {
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		tabpanel.setHeight("750px");

		MyGrid grid = new MyGrid();
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Akta Pendirian"));
		noAktaPendirian = new Textbox(penyediaAsset.getNoAktaPendirian());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getNoAktaPendirian()));
		} else {
			row.appendChild(noAktaPendirian);
		}
		noAktaPendirian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Akta Pendirian"));
		tanggalAktaPendirian = new MyDatebox(penyediaAsset.getTanggalAktaPendirian());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getTanggalAktaPendirian() == null ? ""
					: Common.dateFormat6.get().format(penyediaAsset.getTanggalAktaPendirian())));
		} else {
			row.appendChild(tanggalAktaPendirian);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Notaris"));
		namaNotaris = new Textbox(penyediaAsset.getNamaNotaris());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getNamaNotaris()));
		} else {
			row.appendChild(namaNotaris);
		}
		namaNotaris.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pemilik"));
		pemilik = new Textbox(penyediaAsset.getPemilik());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getPemilik()));
		} else {
			row.appendChild(pemilik);
		}
		pemilik.setWidth("90%");

		dokumenAktaPendirian = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Akta Pendirian"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Akta_Pendirian", "Dokumen Akta Pendirian", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dokumenAktaPendirian = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika Dokumen Akta Pendirian lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Pengesahan"));
		noPengesahan = new Textbox(penyediaAsset.getNoPengesahan());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getNoPengesahan()));
		} else {
			row.appendChild(noPengesahan);
		}
		noPengesahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengesahan"));
		tanggalPengesahan = new MyDatebox(penyediaAsset.getTanggalPengesahan());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getTanggalPengesahan() == null ? ""
					: Common.dateFormat6.get().format(penyediaAsset.getTanggalPengesahan())));
		} else {
			row.appendChild(tanggalPengesahan);
		}

		dokumenPengesahan = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Pengesahan"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, penyediaAsset.getId(),
				PenyediaAsset.class.getName() + "_Dokumen_Pengesahan", "Dokumen Pengesahan", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dokumenPengesahan = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan);
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika Dokumen Pengesahan lebih dari satu file, zip dulu semua file tersebut");

		return tabpanel;
	}

	/**
	 * <b>Tujuan:</b> Membangun tab panel "Rekening Bank" untuk mengelola daftar
	 * rekening bank vendor (dapat lebih dari satu rekening dalam JSON Array) dan
	 * menentukan rekening utama beserta akun utang pada sistem akuntansi.<br><br>
	 *
	 * <b>Cara kerja:</b> Mem-parse field {@code penyediaAsset.getBank()} dari
	 * JSON Array ke {@code array}. Memanggil {@code reloadBank} untuk menampilkan
	 * grid rekening yang dapat diedit. Di bawah grid rekening ditambahkan
	 * Combobox Bank Utama (dari tabel Bank aktif), field Atas Nama, dan No.
	 * Rekening Utama. Field atas nama dan no rekening disembunyikan jika Bank
	 * Utama belum dipilih (melalui EventListener onChange). Dalam mode
	 * persetujuan semua field menjadi Label baca saja.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor yang data rekeningnya ditampilkan<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Tabpanel} berisi form rekening bank<br><br>
	 *
	 * <b>Penanganan error:</b> JSON parse dibungkus try-catch; jika field bank
	 * tidak valid atau null, {@code array} diinisialisasi sebagai JSONArray kosong.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Anotasi {@code @SuppressWarnings("deprecation")}
	 * disebabkan penggunaan API ZK yang sudah deprecated. Jika ZK diupgrade,
	 * periksa dan ganti API yang deprecated tersebut.
	 *
	 * @param penyediaAsset entitas vendor
	 * @return Tabpanel rekening bank
	 * @throws Exception jika terjadi kesalahan
	 */
	@SuppressWarnings("deprecation")
	private Tabpanel dataRekening(final PenyediaAsset penyediaAsset) throws Exception {
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		tabpanel.setHeight("750px");

		MyGrid grid = new MyGrid();
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		try {
			array = new JSONArray(penyediaAsset.getBank());
		} catch (Exception e) {
			array = new JSONArray();
		}
		Row rowBank = Common.tampilanScroll1(row);
		reloadBank(rowBank, array, persetujuan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bank Utama"));
		bankUtama = new Combobox();

		if (persetujuan) {
			row.appendChild(
					new Label(penyediaAsset.getBankUtama() == null ? "" : penyediaAsset.getBankUtama().getNama()));
		} else {
			row.appendChild(bankUtama);
		}

		Common.insertComboDanSemua(bankUtama, new String[] { "nama" }, "keterangan", Bank.class, "=Pilih=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, bankUtama, penyediaAsset.getBankUtama());
		bankUtama.setWidth("90%");

		rowatasNama = new MyFormRow();
		rowatasNama.setParent(rows);
		rowatasNama.appendChild(new ais.ui.util.MyLabelConfig("Bank Utama Atas Nama"));
		atasNama = new Textbox(penyediaAsset.getAtasNama());
		if (persetujuan) {
			rowatasNama.appendChild(new Label(penyediaAsset.getAtasNama()));
		} else {
			rowatasNama.appendChild(atasNama);
		}
		atasNama.setWidth("90%");

		rownoRek = new MyFormRow();
		rownoRek.setParent(rows);
		rownoRek.appendChild(new ais.ui.util.MyLabelConfig("No. Rekening Utama"));
		noRek = new Textbox(penyediaAsset.getNoRek());
		if (persetujuan) {
			rownoRek.appendChild(new Label(penyediaAsset.getNoRek()));
		} else {
			rownoRek.appendChild(noRek);
		}
		noRek.setWidth("90%");

		EventListener eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Bank bankData = (Bank) (bankUtama.getSelectedItem() == null ? null
						: bankUtama.getSelectedItem().getValue());

				rowatasNama.setVisible(bankData != null);
				rownoRek.setVisible(bankData != null);

			}
		};

		bankUtama.addEventListener("onChange", eventListener2);
		eventListener2.onEvent(null);

		return tabpanel;
	}

	/**
	 * <b>Tujuan:</b> Membangun tab panel "Produk" untuk mengelola daftar produk
	 * atau barang yang dapat dipasok oleh vendor ini. Setiap produk direpresentasikan
	 * sebagai referensi ke entitas {@code MasterAsset}.<br><br>
	 *
	 * <b>Cara kerja:</b> Mem-parse field {@code penyediaAsset.getFormula()} dari
	 * JSON Array ke {@code arrayProduk}. Memanggil {@code reloadProduk} yang
	 * menambahkan tombol "Tambah Produk" (jika tidak dalam mode persetujuan)
	 * dan merender grid produk dengan kolom Kode, Nama Produk, dan Jenis.
	 * Penambahan produk dilakukan melalui dialog {@code AmbilDataMasterAssetBanyak}
	 * yang mendukung pemilihan banyak produk sekaligus.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor yang daftar produknya ditampilkan<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Tabpanel} berisi grid daftar produk vendor<br><br>
	 *
	 * <b>Penanganan error:</b> JSON parse dibungkus try-catch; jika field formula
	 * tidak valid, {@code arrayProduk} diinisialisasi sebagai JSONArray kosong.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Daftar produk disimpan sebagai JSON Array di kolom
	 * {@code formula} tabel penyedia_asset. Jika volume produk per vendor besar,
	 * pertimbangkan untuk memindahkan relasi ini ke tabel terpisah.
	 *
	 * @param penyediaAsset entitas vendor
	 * @return Tabpanel produk vendor
	 * @throws Exception jika terjadi kesalahan
	 */
	@SuppressWarnings("deprecation")
	private Tabpanel dataProduk(final PenyediaAsset penyediaAsset) throws Exception {
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		tabpanel.setHeight("750px");

		MyGrid grid = new MyGrid();
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		try {
			arrayProduk = new JSONArray(penyediaAsset.getFormula());
		} catch (Exception e) {
			arrayProduk = new JSONArray();
		}
		Row rowBank = Common.tampilanScroll1(row);
		reloadProduk(rowBank, arrayProduk, persetujuan);

		return tabpanel;
	}

	/**
	 * <b>Tujuan:</b> Memuat ulang (refresh) tampilan grid daftar produk vendor
	 * pada baris container yang diberikan, berdasarkan data dari JSONArray produk
	 * terkini. Dipanggil setelah penambahan atau penghapusan produk.<br><br>
	 *
	 * <b>Cara kerja:</b> Membersihkan seluruh konten {@code rowU} dengan
	 * {@code Common.clear}, kemudian membangun ulang {@code Grid} produk dengan
	 * kolom Kode, Nama Produk, dan Jenis. Untuk setiap entri JSONObject dalam
	 * {@code array}: mengambil id {@code MasterAsset} dari field "masterAsset",
	 * lookup entitas dari cache {@code ConstantValues}, lalu merender Label kode,
	 * nama, dan jenis. Jika tidak dalam mode persetujuan, tombol hapus (ikon
	 * tempat sampah) ditambahkan dengan konfirmasi dialog sebelum menghapus entry
	 * dari array (dengan cara mengisi JSONObject kosong di indeks tersebut) dan
	 * memanggil ulang metode ini secara rekursif.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param rowU        baris container ZK tempat grid produk akan dirender ulang
	 * @param array       JSONArray berisi objek produk dengan field "masterAsset"
	 *                    (id {@code MasterAsset}); entri kosong (JSONObject kosong)
	 *                    dianggap terhapus dan dilewati
	 * @param persetujuan {@code true} untuk mode baca saja (tanpa tombol hapus),
	 *                    {@code false} untuk mode edit dengan tombol hapus<br><br>
	 *
	 * <b>Return:</b> void<br><br>
	 *
	 * <b>Penanganan error:</b> Error saat menghapus ditampilkan ke admin melalui
	 * {@code Common.tampilErrorJikaAdmin} dan ditampilkan dalam dialog pesan.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Entri yang dihapus tidak benar-benar dibuang dari array
	 * tetapi diganti JSONObject kosong; ini mempertahankan indeks untuk konsistensi
	 * referensi selama sesi berlangsung.
	 *
	 * @param rowU        container baris ZK
	 * @param array       data produk dalam JSONArray
	 * @param persetujuan flag mode baca saja
	 * @throws Exception jika terjadi kesalahan
	 */
	public static void reloadDataProduk(final Row rowU, final JSONArray array, final boolean persetujuan)
			throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);

		column = new MyColumnConfig("Nama Produk");
		column.setParent(columns);

		column = new MyColumnConfig("Jenis");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("8%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			Long masterAssetId = jsonObject.isNull("masterAsset") ? null : ais.common.CommonJSONUtil.ambilLong(jsonObject,"masterAsset");
			if (masterAssetId != null) {

				MasterAsset masterAsset = (MasterAsset) ConstantValues.ambil(MasterAsset.class.getName(),
						masterAssetId);
				if (masterAsset != null) {
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);

					row.appendChild(new Label(masterAsset.getKode()));

					row.appendChild(new Label(masterAsset.getNama()));

					row.appendChild(new Label(
							masterAsset.getJenisAsset() == null ? "" : masterAsset.getJenisAsset().getNama()));

					if (persetujuan) {
						new Label().setParent(row);
					} else {

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

														reloadDataProduk(rowU, array, persetujuan);

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
		}
	}

	/**
	 * <b>Tujuan:</b> Menyiapkan area tampilan daftar rekening bank dengan
	 * menambahkan tombol "Tambah Bank" (jika dalam mode edit) dan memuat
	 * ulang grid data rekening. Berfungsi sebagai entry point untuk inisialisasi
	 * awal maupun refresh daftar rekening.<br><br>
	 *
	 * <b>Cara kerja:</b> Jika tidak dalam mode persetujuan, membuat dan menambahkan
	 * tombol "Tambah Bank" ke {@code rowBank}. Klik tombol ini akan menambahkan
	 * JSONObject baru dengan tanggal hari ini dan field target kosong ke
	 * {@code array}, lalu memanggil {@code reloadDataBank} untuk memuat ulang
	 * grid. Kemudian membuat baris form baru ({@code rowU}) di bawah {@code rowBank}
	 * dalam parent yang sama dan memanggil {@code reloadDataBank} untuk pengisian
	 * awal.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param rowBank     baris ZK tempat tombol "Tambah Bank" ditempatkan;
	 *                    grid data rekening ditempatkan di bawahnya (sibling)
	 * @param array       JSONArray berisi objek rekening bank yang sudah ada;
	 *                    array ini di-mutasi saat tombol Tambah diklik
	 * @param persetujuan {@code true} untuk mode baca saja (tanpa tombol Tambah),
	 *                    {@code false} untuk mode edit<br><br>
	 *
	 * <b>Return:</b> void<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Struktur dua baris (tombol di rowBank, data di rowU)
	 * membutuhkan bahwa {@code rowBank} dan {@code rowU} berbagi parent yang sama;
	 * pastikan pemanggil menyediakan parent container yang tepat.
	 *
	 * @param rowBank     baris ZK untuk tombol tambah
	 * @param array       JSONArray data rekening bank
	 * @param persetujuan flag mode baca saja
	 * @throws Exception jika terjadi kesalahan
	 */
	public static void reloadBank(final Row rowBank, final JSONArray array, final boolean persetujuan)
			throws Exception {
		final MyFormRow rowU = new MyFormRow();

		if (!persetujuan) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Bank", "/img/svg/addthis.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("tgl", Common.dateFormat1.get().format(new Date()));
					jsonObject.put("target", "");
					array.put(jsonObject);

					reloadDataBank(rowU, array, persetujuan);
				}
			});
			button.setParent(rowBank);
		}
		rowU.setParent(rowBank.getParent());

		reloadDataBank(rowU, array, persetujuan);

	}

	/**
	 * <b>Tujuan:</b> Memuat ulang (refresh) tampilan grid daftar rekening bank
	 * vendor pada baris container yang diberikan, berdasarkan data dari JSONArray
	 * rekening terkini. Dipanggil setelah penambahan atau penghapusan rekening.<br><br>
	 *
	 * <b>Cara kerja:</b> Membersihkan seluruh konten {@code rowU} dengan
	 * {@code Common.clear}, kemudian membangun ulang {@code Grid} dengan kolom:
	 * Jenis Rekening, Bank, Nomor Rekening, Nama Pemilik Rekening, Kantor Cabang
	 * Bank, dan kolom aksi. Untuk setiap entri dalam array, membuat baris form
	 * dengan field teks yang dapat diedit (atau Label baca saja jika persetujuan).
	 * Perubahan pada setiap field secara otomatis diperbarui ke JSONObject yang
	 * sesuai melalui EventListener {@code onChange}. Tombol hapus (dengan
	 * konfirmasi dialog) mengisi JSONObject kosong di indeks tersebut dan
	 * memanggil ulang metode ini.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param rowU        baris container ZK tempat grid rekening dirender ulang
	 * @param array       JSONArray berisi objek rekening dengan field: jenisRekening,
	 *                    bank, nomorRekening, namaPemilikRekening, kantorCabangBank
	 * @param persetujuan {@code true} untuk mode baca saja, {@code false} untuk
	 *                    mode edit dengan kemampuan mengubah dan menghapus<br><br>
	 *
	 * <b>Return:</b> void<br><br>
	 *
	 * <b>Penanganan error:</b> Error hapus ditampilkan dalam dialog pesan dan
	 * ke admin via {@code Common.tampilErrorJikaAdmin}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Perubahan pada struktur JSON rekening (penambahan
	 * field baru) memerlukan pembaruan di metode ini untuk membaca dan menulis
	 * field baru tersebut.
	 *
	 * @param rowU        container baris ZK
	 * @param array       data rekening dalam JSONArray
	 * @param persetujuan flag mode baca saja
	 * @throws Exception jika terjadi kesalahan
	 */
	public static void reloadDataBank(final Row rowU, final JSONArray array, final boolean persetujuan)
			throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Jenis Rekening");
		column.setParent(columns);

		column = new MyColumnConfig("Bank");
		column.setParent(columns);

		column = new MyColumnConfig("Nomor Rekening");
		column.setParent(columns);

		column = new MyColumnConfig("Nama Pemilik Rekening");
		column.setParent(columns);

		column = new MyColumnConfig("Kantor Cabang Bank");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setWidth("8%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			final MyTextbox jenisRekening = new MyTextbox(
					jsonObject.isNull("jenisRekening") ? "" : jsonObject.getString("jenisRekening"));
			jenisRekening.setWidth("90%");

			if (persetujuan) {
				row.appendChild(new Label(jenisRekening.getValue()));
			} else {
				row.appendChild(jenisRekening);
			}

			final MyTextbox bank = new MyTextbox(jsonObject.isNull("bank") ? "" : jsonObject.getString("bank"));
			bank.setWidth("90%");

			if (persetujuan) {
				row.appendChild(new Label(bank.getValue()));
			} else {
				row.appendChild(bank);
			}

			final MyTextbox nomorRekening = new MyTextbox(
					jsonObject.isNull("nomorRekening") ? "" : jsonObject.getString("nomorRekening"));
			nomorRekening.setWidth("90%");
			if (persetujuan) {
				row.appendChild(new Label(nomorRekening.getValue()));
			} else {
				row.appendChild(nomorRekening);
			}

			final MyTextbox namaPemilikRekening = new MyTextbox(
					jsonObject.isNull("namaPemilikRekening") ? "" : jsonObject.getString("namaPemilikRekening"));
			namaPemilikRekening.setWidth("90%");
			if (persetujuan) {
				row.appendChild(new Label(namaPemilikRekening.getValue()));
			} else {
				row.appendChild(namaPemilikRekening);
			}

			final MyTextbox kantorCabangBank = new MyTextbox(
					jsonObject.isNull("kantorCabangBank") ? "" : jsonObject.getString("kantorCabangBank"));
			kantorCabangBank.setWidth("90%");
			if (persetujuan) {
				row.appendChild(new Label(kantorCabangBank.getValue()));
			} else {
				row.appendChild(kantorCabangBank);
			}

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					jsonObject.put("jenisRekening", jenisRekening.getValue().trim());
					jsonObject.put("bank", bank.getValue().trim());
					jsonObject.put("nomorRekening", nomorRekening.getValue().trim());
					jsonObject.put("namaPemilikRekening", namaPemilikRekening.getValue().trim());
					jsonObject.put("kantorCabangBank", kantorCabangBank.getValue().trim());

				}
			};

			jenisRekening.addEventListener("onChange", eventListener);
			bank.addEventListener("onChange", eventListener);
			nomorRekening.addEventListener("onChange", eventListener);
			namaPemilikRekening.addEventListener("onChange", eventListener);
			kantorCabangBank.addEventListener("onChange", eventListener);

			if (persetujuan) {
				new Label().setParent(row);
			} else {

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

												reloadDataBank(rowU, array, persetujuan);

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

	/**
	 * <b>Tujuan:</b> Menyiapkan area tampilan daftar produk vendor dengan
	 * menambahkan tombol "Tambah Produk" (jika dalam mode edit) dan memuat
	 * ulang grid data produk. Berfungsi sebagai entry point untuk inisialisasi
	 * awal maupun refresh daftar produk vendor.<br><br>
	 *
	 * <b>Cara kerja:</b> Jika tidak dalam mode persetujuan, menambahkan tombol
	 * "Tambah Produk". Klik tombol ini membuka dialog {@code AmbilDataMasterAssetBanyak}
	 * untuk memilih banyak produk sekaligus (dengan mengecualikan produk yang
	 * sudah ada di daftar). Setiap produk yang dipilih ditambahkan ke
	 * {@code array} sebagai JSONObject dengan field "masterAsset" (id). Kemudian
	 * membuat baris form baru ({@code rowU}) di bawah {@code rowBank} dan
	 * memanggil {@code reloadDataProduk} untuk pengisian awal atau refresh.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param rowBank     baris ZK tempat tombol "Tambah Produk" ditempatkan
	 * @param array       JSONArray berisi objek produk yang sudah ada; di-mutasi
	 *                    saat produk baru dipilih dari dialog
	 * @param persetujuan {@code true} untuk mode baca saja (tanpa tombol Tambah),
	 *                    {@code false} untuk mode edit<br><br>
	 *
	 * <b>Return:</b> void<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Dialog {@code AmbilDataMasterAssetBanyak} mendukung
	 * filter dan seleksi banyak produk; ukurannya dikonfigurasi 850px lebar
	 * dan 97% tinggi.
	 *
	 * @param rowBank     baris ZK untuk tombol tambah
	 * @param array       JSONArray data produk
	 * @param persetujuan flag mode baca saja
	 * @throws Exception jika terjadi kesalahan
	 */
	public static void reloadProduk(final Row rowBank, final JSONArray array, final boolean persetujuan)
			throws Exception {
		final MyFormRow rowU = new MyFormRow();

		if (!persetujuan) {
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Produk", "/img/svg/addthis.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<Long> masterAssetsIds = new ArrayList<Long>();
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonObject = array.getJSONObject(i);
						Long masterAsset = jsonObject.isNull("masterAsset") ? null : ais.common.CommonJSONUtil.ambilLong(jsonObject,"masterAsset");
						if (masterAsset != null) {
							masterAssetsIds.add(masterAsset);
						}
					}

					List<MasterAsset> masterAssets = ConstantValues.ambilBanyak(MasterAsset.class.getName(),
							masterAssetsIds);

					AmbilDataMasterAssetBanyak ambilDataMasterAssetBanyak = new AmbilDataMasterAssetBanyak(masterAssets,
							null);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataMasterAssetBanyak);
					ambilDataMasterAssetBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<MasterAsset> masterAssets = (List<MasterAsset>) arg0.getData();

							for (MasterAsset masterAsset : masterAssets) {
								JSONObject jsonObject = new JSONObject();
								jsonObject.put("masterAsset", masterAsset.getId());
								array.put(jsonObject);
							}

							reloadDataProduk(rowU, array, persetujuan);
						}
					});
					ambilDataMasterAssetBanyak.setWidth("850px");
					ambilDataMasterAssetBanyak.setHeight("97%");
					ambilDataMasterAssetBanyak.setVisible(true);
					ambilDataMasterAssetBanyak.onModal();

				}
			});
			button.setParent(rowBank);
		}
		rowU.setParent(rowBank.getParent());

		reloadDataProduk(rowU, array, persetujuan);

	}

	/**
	 * <b>Tujuan:</b> Membangun tab panel "Identitas Perusahaan" yang merupakan
	 * tab utama dan terlengkap pada form penyedia, berisi seluruh data identitas
	 * vendor termasuk kode, nama, kategori, jenis, jenis pekerjaan (5 slot),
	 * akun utang, alamat, wilayah, kontak, email, tanggal pengajuan, keterangan,
	 * dan galeri foto/logo perusahaan.<br><br>
	 *
	 * <b>Cara kerja:</b> Membangun {@code Tabpanel} berisi {@code MyGrid} dua
	 * kolom. Kode perusahaan di-generate otomatis jika kosong. Jika pengguna
	 * login sebagai vendor penyedia ({@code tbmuser.getPenyediaAsset() != null}),
	 * field kode dikunci. Jika ada {@code disposisiSop}, field nama menggunakan
	 * {@code AmbilDataPenyediaAssetBanbox} (dengan autocomplete lookup vendor
	 * yang sudah terdaftar) sehingga vendor dapat dipilih dari daftar yang ada.
	 * Galeri foto dimuat dari {@code StreamingHibernateUtil} berdasarkan prefix
	 * jenis "Galery_PenyediaAsset_" dan ditampilkan sebagai grid gambar yang
	 * dapat diunggah/dihapus. Dalam mode persetujuan semua field menjadi Label
	 * baca saja.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor yang identitasnya ditampilkan; id null
	 *                      berarti form untuk vendor baru<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Tabpanel} berisi form identitas perusahaan<br><br>
	 *
	 * <b>Penanganan error:</b> Galeri foto menggunakan try-catch dengan rollback
	 * {@code StreamingHibernateUtil} jika query BLOB gagal.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Baris longitude dan latitude sengaja disembunyikan
	 * ({@code setVisible(false)}); aktifkan jika fitur peta diperlukan di masa
	 * mendatang.
	 *
	 * @param penyediaAsset entitas vendor
	 * @return Tabpanel identitas perusahaan
	 * @throws Exception jika terjadi kesalahan
	 */
	private Tabpanel mainData(final PenyediaAsset penyediaAsset) throws Exception {
		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();

		tabpanel.setHeight("750px");

		MyGrid grid = new MyGrid();
		grid.setParent(tabpanel);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (penyediaAsset.getKode() == null || penyediaAsset.getKode().trim().isEmpty()) {
			String noAgenda = generateCode(false);
			penyediaAsset.setKode(noAgenda);
		}

		Rows rows = new Rows();
		rows.setParent(grid);
		kode = new Textbox(penyediaAsset.getKode());
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Perusahaan"));
		if (persetujuan || (tbmuser != null && tbmuser.getPenyediaAsset() != null && penyediaAsset != null
				&& tbmuser.getPenyediaAsset().getId().equals(penyediaAsset.getId()))) {
			row.appendChild(new Label(penyediaAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Perusahaan *"));
		if (disposisiSop == null) {
			nama = new Textbox(penyediaAsset.getNama() == null ? "" : penyediaAsset.getNama());
		} else {
			nama = new AmbilDataPenyediaAssetBanbox();
			nama.setValue(penyediaAsset.getNama() == null ? "" : penyediaAsset.getNama());
			if (penyediaAsset != null && penyediaAsset.getId() != null) {
				nama.setDisabled(true);
			}
			((AmbilDataPenyediaAssetBanbox) nama).setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					PenyediaAsset penyediaAsset = (PenyediaAsset) nama.getAttribute("penyediaAsset");
					if (penyediaAsset != null) {
						form(penyediaAsset, disposisiSop, null, null);
					}
				}
			});
		}
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getNama()));
		} else {
			row.appendChild(nama);
		}

		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori Perusahaan *"));
		kategoriPenyediaAsset = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getKategoriPenyediaAsset() == null ? ""
					: penyediaAsset.getKategoriPenyediaAsset().getNama()));
		} else {
			row.appendChild(kategoriPenyediaAsset);
		}

		Common.insertCombo(kategoriPenyediaAsset, new String[] { "nama" }, "keterangan", KategoriPenyediaAsset.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(kategoriPenyediaAsset, penyediaAsset.getKategoriPenyediaAsset());
		kategoriPenyediaAsset.setWidth("90%");
		kategoriPenyediaAsset.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Perusahaan *"));
		jenisPenyediaAsset = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getJenisPenyediaAsset() == null ? ""
					: penyediaAsset.getJenisPenyediaAsset().getNama()));
		} else {
			row.appendChild(jenisPenyediaAsset);
		}

		Common.insertCombo(jenisPenyediaAsset, new String[] { "nama" }, "keterangan", JenisPenyediaAsset.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPenyediaAsset, penyediaAsset.getJenisPenyediaAsset());
		jenisPenyediaAsset.setWidth("90%");
		jenisPenyediaAsset.setReadonly(true);

//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig("Status Perusahaan *"));
//		statusPenyediaAsset = new Radiogroup();
//
//		if (persetujuan) {
//			row.appendChild(new Label(penyediaAsset.getStatusPenyediaAsset() == null ? ""
//					: penyediaAsset.getStatusPenyediaAsset().getNama()));
//		} else {
//			row.appendChild(statusPenyediaAsset);
//		}
//
//		Common.insertRadio(statusPenyediaAsset, new String[] { "nama" }, "keterangan", StatusPenyediaAsset.class,
//				Restrictions.eq("aktif", true));
//		Common.selectRadioItem(statusPenyediaAsset, penyediaAsset.getStatusPenyediaAsset());
//		statusPenyediaAsset.setOrient("vertical");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan Utama *"));
		jenisPekerjaanPenyedia1 = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getJenisPekerjaanPenyedia1() == null ? ""
					: penyediaAsset.getJenisPekerjaanPenyedia1().getNama()));
		} else {
			row.appendChild(jenisPekerjaanPenyedia1);
		}

		Common.insertCombo(jenisPekerjaanPenyedia1, new String[] { "nama" }, "keterangan", JenisPekerjaanPenyedia.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia1, penyediaAsset.getJenisPekerjaanPenyedia1());
		jenisPekerjaanPenyedia1.setWidth("90%");
		jenisPekerjaanPenyedia1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan II"));
		jenisPekerjaanPenyedia2 = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getJenisPekerjaanPenyedia2() == null ? ""
					: penyediaAsset.getJenisPekerjaanPenyedia2().getNama()));
		} else {
			row.appendChild(jenisPekerjaanPenyedia2);
		}

		Common.insertComboDanSemua(jenisPekerjaanPenyedia2, new String[] { "nama" }, "keterangan",
				JenisPekerjaanPenyedia.class, "Tidak ada", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia2, penyediaAsset.getJenisPekerjaanPenyedia2());
		jenisPekerjaanPenyedia2.setWidth("90%");
		jenisPekerjaanPenyedia2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan III"));
		jenisPekerjaanPenyedia3 = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getJenisPekerjaanPenyedia3() == null ? ""
					: penyediaAsset.getJenisPekerjaanPenyedia3().getNama()));
		} else {
			row.appendChild(jenisPekerjaanPenyedia3);
		}

		Common.insertComboDanSemua(jenisPekerjaanPenyedia3, new String[] { "nama" }, "keterangan",
				JenisPekerjaanPenyedia.class, "Tidak ada", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia3, penyediaAsset.getJenisPekerjaanPenyedia3());
		jenisPekerjaanPenyedia3.setWidth("90%");
		jenisPekerjaanPenyedia3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan IV"));
		jenisPekerjaanPenyedia4 = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getJenisPekerjaanPenyedia4() == null ? ""
					: penyediaAsset.getJenisPekerjaanPenyedia4().getNama()));
		} else {
			row.appendChild(jenisPekerjaanPenyedia4);
		}

		Common.insertComboDanSemua(jenisPekerjaanPenyedia4, new String[] { "nama" }, "keterangan",
				JenisPekerjaanPenyedia.class, "Tidak ada", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia4, penyediaAsset.getJenisPekerjaanPenyedia4());
		jenisPekerjaanPenyedia4.setWidth("90%");
		jenisPekerjaanPenyedia4.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan V"));
		jenisPekerjaanPenyedia5 = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getJenisPekerjaanPenyedia5() == null ? ""
					: penyediaAsset.getJenisPekerjaanPenyedia5().getNama()));
		} else {
			row.appendChild(jenisPekerjaanPenyedia5);
		}

		Common.insertComboDanSemua(jenisPekerjaanPenyedia5, new String[] { "nama" }, "keterangan",
				JenisPekerjaanPenyedia.class, "Tidak ada", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia5, penyediaAsset.getJenisPekerjaanPenyedia5());
		jenisPekerjaanPenyedia5.setWidth("90%");
		jenisPekerjaanPenyedia5.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getPenyediaAsset() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Utang"));
		akunUtang = new AmbilDataAkunBanbox(false);

		if (persetujuan) {
			row.appendChild(
					new Label(penyediaAsset.getAkunUtang() == null ? "" : penyediaAsset.getAkunUtang().getNama()));
		} else {
			row.appendChild(akunUtang);
		}

		akunUtang.setAttribute("akun", penyediaAsset.getAkunUtang());
		akunUtang.setValue(penyediaAsset.getAkunUtang() == null ? ""
				: penyediaAsset.getAkunUtang().getKode() + "-" + penyediaAsset.getAkunUtang().getNama());
		akunUtang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		alamat = new Textbox(penyediaAsset.getAlamat());

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getAlamat()));
		} else {
			row.appendChild(alamat);
		}

		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos"));
		kodePos = new Textbox(penyediaAsset.getKodePos());

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getKodePos()));
		} else {
			row.appendChild(kodePos);
		}

		kodePos.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp."));
		telp = new Textbox(penyediaAsset.getTelp());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getTelp()));
		} else {
			row.appendChild(telp);
		}
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fax."));
		fax = new Textbox(penyediaAsset.getFax());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getFax()));
		} else {
			row.appendChild(fax);
		}
		fax.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kecamatan"));
		kecamatan = new AmbilDataKecamatanBanbox();
		if (persetujuan) {
			row.appendChild(
					new Label(penyediaAsset.getKecamatan() == null ? "" : penyediaAsset.getKecamatan().getNama()));
		} else {
			row.appendChild(kecamatan);
		}
		kecamatan.setValue(penyediaAsset.getKecamatan() == null ? "" : penyediaAsset.getKecamatan().getNama());
		kecamatan.setAttribute("wilayah", penyediaAsset.getKecamatan());
		kecamatan.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi"));
//		Common.selectComboItem(propinsi, penyediaAsset.getPropinsi());
		propinsi.setAttribute("wilayah", penyediaAsset.getPropinsi());
		row.appendChild(propinsi);
		propinsi.setWidth("90%");

		Common.createFieldKota(rows, "Kota/Kabupaten", kota, propinsi, penyediaAsset.getKota(), true);

		Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsi, kota, kecamatan);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Longitude"));
		longitude = new Textbox(penyediaAsset.getLongitude());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getLongitude()));
		} else {
			row.appendChild(longitude);
		}
		longitude.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Latitude"));
		latitude = new Textbox(penyediaAsset.getLatitude());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getLatitude()));
		} else {
			row.appendChild(latitude);
		}
		latitude.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kontak"));
		kontak = new Textbox(penyediaAsset.getKontak());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getKontak()));
		} else {
			row.appendChild(kontak);
		}
		kontak.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		email = new Textbox(penyediaAsset.getEmail());
		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getEmail()));
		} else {
			row.appendChild(email);
		}
		email.setWidth("90%");

		tanggalPembuatan = new MyDatebox(penyediaAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
				: penyediaAsset.getTanggalPembuatan());
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan *"));
		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat1.get().format(penyediaAsset.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(penyediaAsset.getKeterangan() == null ? "" : penyediaAsset.getKeterangan());

		if (persetujuan) {
			row.appendChild(new Label(penyediaAsset.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(3);

		rowGalery = new MyFormRow();
		rowGalery.setParent(rows);
		EventListener galeryEvent = new EventListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowGalery);
				maps = new HashMap<Long, LampiranLain>();

				rowGalery.appendChild(new ais.ui.util.MyLabelConfig("Foto / Logo Perusahaan"));

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				grid.setParent(rowGalery);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();

				MyColumnConfig column = new MyColumnConfig();
				columns.appendChild(column);
				grid.appendChild(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Hbox myHbox = new Hbox();
				myHbox.setParent(row);
				myHbox.setHeight("30px");

				Hbox hboxGambar = new Hbox();
				hboxGambar.setParent(myHbox);
				tampilkanButton(hboxGambar, this);

				row = new MyFormRow();
				row.setParent(rows);

				myGridGaleri = (Rows) Common.tampilanScroll1(row).getParent();

				columns = new Columns();
				columns.setParent(myGridGaleri.getGrid());

				column = new MyColumnConfig("Foto");
				column.setWidth("90%");
				column.setParent(columns);

				column = new MyColumnConfig("Keterangan");
				column.setParent(columns);
				column.setWidth("0px");

				column = new MyColumnConfig("Hapus");
				column.setWidth("10%");
				column.setParent(columns);

				if (penyediaAsset.getId() != null) {
					try {
						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
								.addOrder(Order.asc("id")).add(Restrictions.eq("ref", penyediaAsset.getId()))
								.add(Restrictions.ilike("jenis", "Galery_PenyediaAsset_", MatchMode.START)).list();
						for (LampiranLain lampiran : lampiranLains) {
							maps.put(lampiran.getId(), lampiran);
						}

						StreamingHibernateUtil.getInstance().closeSession();

					} catch (Exception e1) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/asset/PenyediaAssetAction.java:3042");
					}
				}

				reloadDataGambar(penyediaAsset);
			}

		};

		galeryEvent.onEvent(null);

		return tabpanel;
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi form pendaftaran mandiri perusahaan (self-
	 * registration) yang lebih ringkas dibandingkan form edit lengkap. Digunakan
	 * untuk alur di mana vendor/perusahaan mendaftarkan dirinya ke sistem untuk
	 * pertama kali, tanpa perlu login terlebih dahulu.<br><br>
	 *
	 * <b>Cara kerja:</b> Mengatur judul jendela menjadi "Pendaftaran Perusahaan",
	 * membangun {@code Borderlayout} dengan {@code Center} (berisi grid form) dan
	 * {@code South} (berisi toolbar tombol "Daftarkan Perusahaan"). Grid form
	 * berisi field: Nama Perusahaan, Kategori Perusahaan, Jenis Perusahaan,
	 * Jenis Pekerjaan I-V, Telp/HP, Email, dan Kontak Person. Saat tombol
	 * "Daftarkan Perusahaan" diklik: memvalidasi field wajib, memeriksa keunikan
	 * email (di tabel PenyediaAsset dan Tbmuser), menyimpan entitas penyedia,
	 * membuat akun Tbmuser baru dengan username=email dan password acak 5 digit,
	 * mengirim email kredensial, lalu menampilkan pesan sukses dan menutup jendela.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas penyedia baru yang akan didaftarkan; biasanya
	 *                      objek kosong {@code new PenyediaAsset()}<br><br>
	 *
	 * <b>Penanganan error:</b> Validasi email duplikat dilakukan dengan query
	 * Hibernate sebelum simpan. Sesi Hibernate dibuka secara native
	 * ({@code currentNativeSession}) dan ditutup di blok finally.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Role akun baru ditentukan oleh
	 * {@code ConstantValues.tbmrolePenyedia}; pastikan role ini ada di database.
	 * Email dikirim menggunakan konfigurasi {@code default_email} dari tabel
	 * konfigurasi sistem.
	 *
	 * @param penyediaAsset entitas penyedia baru
	 * @throws Exception jika terjadi kesalahan inisialisasi
	 */
	private void initDaftar(final PenyediaAsset penyediaAsset) throws Exception {
		this.penyediaAsset = penyediaAsset;

		if (addWindow instanceof Window) {
			((Window) addWindow).setTitle("Pendaftaran Perusahaan");
		}

		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		borderlayout.setParent(addWindow);

		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Perusahaan *"));
		row.appendChild(nama = new Textbox(penyediaAsset.getNama() == null ? "" : penyediaAsset.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori Perusahaan *"));
		row.appendChild(kategoriPenyediaAsset = new Combobox());
		Common.insertCombo(kategoriPenyediaAsset, new String[] { "nama" }, "keterangan", KategoriPenyediaAsset.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(kategoriPenyediaAsset, penyediaAsset.getKategoriPenyediaAsset());
		kategoriPenyediaAsset.setWidth("90%");
		kategoriPenyediaAsset.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Perusahaan *"));
		row.appendChild(jenisPenyediaAsset = new Combobox());
		Common.insertCombo(jenisPenyediaAsset, new String[] { "nama" }, "keterangan", JenisPenyediaAsset.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPenyediaAsset, penyediaAsset.getJenisPenyediaAsset());
		jenisPenyediaAsset.setWidth("90%");
		jenisPenyediaAsset.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan Utama *"));
		jenisPekerjaanPenyedia1 = new Combobox();

		row.appendChild(jenisPekerjaanPenyedia1);

		Common.insertCombo(jenisPekerjaanPenyedia1, new String[] { "nama" }, "keterangan", JenisPekerjaanPenyedia.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia1, penyediaAsset.getJenisPekerjaanPenyedia1());
		jenisPekerjaanPenyedia1.setWidth("90%");
		jenisPekerjaanPenyedia1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan II"));
		jenisPekerjaanPenyedia2 = new Combobox();

		row.appendChild(jenisPekerjaanPenyedia2);

		Common.insertComboDanSemua(jenisPekerjaanPenyedia2, new String[] { "nama" }, "keterangan",
				JenisPekerjaanPenyedia.class, "Tidak ada", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia2, penyediaAsset.getJenisPekerjaanPenyedia2());
		jenisPekerjaanPenyedia2.setWidth("90%");
		jenisPekerjaanPenyedia2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan III"));
		jenisPekerjaanPenyedia3 = new Combobox();

		row.appendChild(jenisPekerjaanPenyedia3);

		Common.insertComboDanSemua(jenisPekerjaanPenyedia3, new String[] { "nama" }, "keterangan",
				JenisPekerjaanPenyedia.class, "Tidak ada", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia3, penyediaAsset.getJenisPekerjaanPenyedia3());
		jenisPekerjaanPenyedia3.setWidth("90%");
		jenisPekerjaanPenyedia3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan IV"));
		jenisPekerjaanPenyedia4 = new Combobox();

		row.appendChild(jenisPekerjaanPenyedia4);

		Common.insertComboDanSemua(jenisPekerjaanPenyedia4, new String[] { "nama" }, "keterangan",
				JenisPekerjaanPenyedia.class, "Tidak ada", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia4, penyediaAsset.getJenisPekerjaanPenyedia4());
		jenisPekerjaanPenyedia4.setWidth("90%");
		jenisPekerjaanPenyedia4.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pekerjaan V"));
		jenisPekerjaanPenyedia5 = new Combobox();

		row.appendChild(jenisPekerjaanPenyedia5);

		Common.insertComboDanSemua(jenisPekerjaanPenyedia5, new String[] { "nama" }, "keterangan",
				JenisPekerjaanPenyedia.class, "Tidak ada", Restrictions.eq("aktif", true));
		Common.selectComboItem(jenisPekerjaanPenyedia5, penyediaAsset.getJenisPekerjaanPenyedia5());
		jenisPekerjaanPenyedia5.setWidth("90%");
		jenisPekerjaanPenyedia5.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp/HP *"));
		row.appendChild(telp = new Textbox(penyediaAsset.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email *"));
		row.appendChild(email = new Textbox(penyediaAsset.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kontak Person *"));
		row.appendChild(kontak = new Textbox(penyediaAsset.getKontak()));
		kontak.setWidth("90%");

		South south = new South();
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Daftarkan Perusahaan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (nama.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Nama Perusahaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama Perusahaan dengan nama resmi perusahaan; (2) Nama wajib diisi untuk keperluan identifikasi penyedia; (3) ulangi proses pendaftaran ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (kategoriPenyediaAsset.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Kategori Perusahaan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih kategori perusahaan dari dropdown yang tersedia; (2) Jika kategori belum ada, hubungi Administrator untuk menambahkan; (3) ulangi proses pendaftaran ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (jenisPenyediaAsset.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Jenis Perusahaan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis perusahaan dari dropdown yang tersedia; (2) Jika jenis belum ada, hubungi Administrator untuk menambahkan; (3) ulangi proses pendaftaran ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (jenisPekerjaanPenyedia1.getSelectedItem() == null) {
					MyMessageboxConfig.show("Mohon maaf, Jenis Pekerjaan Utama belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis pekerjaan utama dari dropdown; (2) Jenis pekerjaan utama menentukan bidang kompetensi perusahaan; (3) ulangi proses pendaftaran ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				if (email.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Email Perusahaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Email dengan alamat email aktif perusahaan; (2) Email digunakan untuk pengiriman informasi akun login; (3) ulangi proses pendaftaran ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (telp.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Telp/HP Perusahaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Telp/HP dengan nomor telepon aktif perusahaan; (2) Nomor telepon digunakan untuk verifikasi dan komunikasi; (3) ulangi proses pendaftaran ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (kontak.getValue().trim().equals("")) {
					MyMessageboxConfig.show("Mohon maaf, Kontak Person Perusahaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kontak dengan nama penanggung jawab/PIC perusahaan; (2) Informasi kontak diperlukan untuk keperluan komunikasi; (3) ulangi proses pendaftaran ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = null;
				String newUsername = "";
				String passw = "";
				Tbmuser tbmuser = null;
				try {
					session = HibernateUtil.currentNativeSession();

				int count = ((Number) session.createCriteria(PenyediaAsset.class)
						.add(Restrictions.eq("email", email.getValue().trim())).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();
				if (count > 0) {
					MyMessageboxConfig.show("Email yang Anda masukkan telah terdaftar", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				count = ((Number) session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("userId", email.getValue().trim())).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();
				if (count > 0) {
					MyMessageboxConfig.show("Email yang Anda masukkan telah terdaftar", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				penyediaAsset.setJenisPekerjaanPenyedia1(
						(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia1.getSelectedItem() == null ? null
								: jenisPekerjaanPenyedia1.getSelectedItem().getValue()));
				penyediaAsset.setJenisPekerjaanPenyedia2(
						(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia2.getSelectedItem() == null ? null
								: jenisPekerjaanPenyedia2.getSelectedItem().getValue()));
				penyediaAsset.setJenisPekerjaanPenyedia3(
						(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia3.getSelectedItem() == null ? null
								: jenisPekerjaanPenyedia3.getSelectedItem().getValue()));
				penyediaAsset.setJenisPekerjaanPenyedia4(
						(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia4.getSelectedItem() == null ? null
								: jenisPekerjaanPenyedia4.getSelectedItem().getValue()));
				penyediaAsset.setJenisPekerjaanPenyedia5(
						(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia5.getSelectedItem() == null ? null
								: jenisPekerjaanPenyedia5.getSelectedItem().getValue()));
				penyediaAsset.setNama(nama.getValue());
				penyediaAsset.setEmail(email.getValue());
				penyediaAsset.setKontak(kontak.getValue());
				penyediaAsset.setTelp(telp.getValue());
				penyediaAsset.setKategoriPenyediaAsset(
						(KategoriPenyediaAsset) kategoriPenyediaAsset.getSelectedItem().getValue());
				penyediaAsset
						.setJenisPenyediaAsset((JenisPenyediaAsset) jenisPenyediaAsset.getSelectedItem().getValue());

				// Pendaftar via web = CALON vendor: disetujuiOleh sengaja DIBIARKAN null (belum
				// disahkan) → muncul di tab "Calon Vendor". Tanggal daftar dicatat untuk kolom
				// "Tgl Daftar" pada tab tersebut.
				penyediaAsset.setTanggalPembuatan(ais.ui.util.WaktuUtil.getDate());

				session.getTransaction().begin();
				session.save(penyediaAsset);
				session.getTransaction().commit();

				tbmuser = new Tbmuser();

				newUsername = (penyediaAsset.getEmail() == null || penyediaAsset.getEmail().trim().isEmpty())
						? StringUtils.split(penyediaAsset.getNama(), " ")[0] + "" + RandomStringUtils.randomNumeric(3)
						: penyediaAsset.getEmail().split(",")[0].trim();

				newUsername = newUsername.toLowerCase().trim();

				tbmuser.setUserId(newUsername);
				tbmuser.setEmail(penyediaAsset.getEmail());
				tbmuser.setIs_encripted(true);
				tbmuser.setRoot(false);
				tbmuser.setUserNama(penyediaAsset.getNama());
				passw = RandomStringUtils.randomNumeric(5);
				tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passw.trim()));
				tbmuser.setUserRole(ConstantValues.tbmrolePenyedia);
				tbmuser.setUserShow(1);
				tbmuser.setPenyediaAsset(penyediaAsset);

				session.getTransaction().begin();
				session.save(tbmuser);
				session.getTransaction().commit();

				} finally {
					HibernateUtil.closeSession();
				}

				String subject = "Username dan Password Perusahaan";
				String body = "Username Perusahaan Anda adalah " + newUsername + " dan password " + passw
						+ "<br><br>Terima Kasih";
				String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

				JSONArray userIds = new JSONArray();
				userIds.put(newUsername);
				MailSender.sendMail(userIds, subject, body, sender, penyediaAsset.getEmail(), penyediaAsset);

				MyMessageboxConfig.show("Perusahaan Anda berhasil terdaftar, informasi login telah terkirm ke email",
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								addWindow.detach();
							}
						});
			}
		});
		save.setParent(toolbar);
	}

	/**
	 * <b>Tujuan:</b> Menginisialisasi form edit/tambah penyedia lengkap (dengan
	 * delapan tab) ke dalam {@code addWindow}. Merupakan metode init utama yang
	 * digunakan oleh {@link #init(GeneralValueObject)}, {@link #onAdd(Event)},
	 * dan metode-metode {@code onAddExternal}.<br><br>
	 *
	 * <b>Cara kerja:</b> Menyimpan referensi penyediaAsset, mengatur judul jendela,
	 * membersihkan konten addWindow lama ({@code Common.clear}), membangun
	 * {@code Borderlayout} baru dengan {@code Center} (berisi hasil {@code form()})
	 * dan {@code South} (dari {@code mysouth} yang ada atau baru). Memanggil
	 * {@code tampilSimpanData} untuk menambahkan tombol Simpan dan Batal ke
	 * toolbar. {@code disposisiSop} di-reset ke null sebelum memanggil
	 * {@code form()}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas penyedia yang akan diinisialisasi ke form;
	 *                      bisa baru (id null) atau existing (id tidak null)<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Jika {@code mysouth} tidak null (mode inline di halaman
	 * lain), toolbar simpan dimasukkan ke south yang sudah ada; jika null,
	 * south baru dibuat di dalam borderlayout.
	 *
	 * @param penyediaAsset entitas penyedia
	 * @throws Exception jika terjadi kesalahan
	 */
	private void init(PenyediaAsset penyediaAsset) throws Exception {
		this.penyediaAsset = penyediaAsset;

		if (addWindow instanceof Window) {
			((Window) addWindow).setTitle("Pendataan Penyedia / Perusahaan");
		}

		Common.clear(addWindow);

		Borderlayout borderlayout = new Borderlayout();
		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(penyediaAsset, disposisiSop, save, null));

		borderlayout.setParent(addWindow);

		if (mysouth != null) {
			tampilSimpanData(mysouth, save);
		} else {
			South south = new South();
			south.setParent(borderlayout);
			tampilSimpanData(south, save);
		}
	}

	/**
	 * <b>Tujuan:</b> Menambahkan toolbar Simpan (dan Batal jika diperlukan) ke
	 * komponen {@code south} yang diberikan. Menangani dua konteks berbeda:
	 * (1) form dalam {@code Window} modal dengan tombol Batal yang menutup jendela,
	 * dan (2) form inline (embedded di halaman lain) dengan tombol Simpan terpusat
	 * yang mengirim callback event listener.<br><br>
	 *
	 * <b>Cara kerja:</b> Jika {@code addWindow} bertipe {@code Window}, membuat
	 * toolbar dengan tombol "Batal" (menutup window dengan {@code setVisible(false)})
	 * dan tombol "Simpan" yang memanggil {@code onSave}, lalu memanggil
	 * {@code onSearchDefault} untuk memuat ulang daftar jika simpan berhasil.
	 * Jika {@code addWindow} bukan Window (mode inline), toolbar di-align tengah,
	 * tombol Simpan memanggil {@code onSave} dan jika berhasil memanggil
	 * {@code eventListener.onEvent} dengan data penyedia yang tersimpan serta
	 * menampilkan pesan sukses.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param south komponen {@code South} dari borderlayout tempat toolbar dimasukkan
	 * @param save  tombol simpan ({@code MyToolbarbuttonConfig}) yang akan
	 *              dikonfigurasi dengan listener dan ditempatkan di toolbar<br><br>
	 *
	 * <b>Return:</b> void<br><br>
	 *
	 * <b>Pemeliharaan:</b> Pastikan {@code eventListener} tidak null sebelum
	 * dipanggil jika mode inline digunakan; saat ini tidak ada null-check eksplisit
	 * untuk {@code eventListener}.
	 *
	 * @param south panel south untuk toolbar
	 * @param save  tombol simpan yang akan dikonfigurasi
	 */
	private void tampilSimpanData(Component south, MyToolbarbuttonConfig save) {
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		if (addWindow != null && addWindow instanceof Window) {
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
		} else {
			toolbar.setAlign("center");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (onSave(event)) {

						if (PenyediaAssetAction.this.eventListener != null) {
							PenyediaAssetAction.this.eventListener
									.onEvent(new Event("", event.getTarget(), PenyediaAssetAction.this.penyediaAsset));
						}

						MyMessageboxConfig.show("Data berhasil tersimpan", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
					}
				}
			});
			save.setParent(toolbar);
		}
	}

	/**
	 * <b>Tujuan:</b> Menampilkan (atau memperbarui) tombol upload foto/gambar baru
	 * ke galeri perusahaan di dalam {@code Hbox} yang diberikan. Dipanggil saat
	 * inisialisasi galeri dan setelah upload gambar berhasil untuk me-reset
	 * tombol upload agar siap menerima file berikutnya.<br><br>
	 *
	 * <b>Cara kerja:</b> Membersihkan konten {@code hboxGambar} dengan
	 * {@code Common.clear}, kemudian memanggil
	 * {@code LampiranLain.createDownloadUploadFileLain} dengan identifier unik
	 * berbasis barcode acak ({@code "Galery_PenyediaAsset_" + Common.getGeneratedBarCode()}).
	 * EventListener upload menyimpan lampiran ke {@code maps}, memanggil
	 * {@code reloadDataGambar}, dan menjadwalkan ulang tombol upload via
	 * {@code Common.createDefaultTimer(eventListener)}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param hboxGambar    container Hbox tempat tombol upload ditempatkan;
	 *                      dibersihkan sebelum tombol baru ditambahkan
	 * @param eventListener listener yang digunakan untuk memanggil ulang metode
	 *                      ini setelah upload selesai (self-refreshing pattern)<br><br>
	 *
	 * <b>Return:</b> void<br><br>
	 *
	 * <b>Pemeliharaan:</b> Setiap upload menggunakan identifier barcode baru
	 * (unik) sehingga multiple upload dapat dilakukan pada satu sesi.
	 *
	 * @param hboxGambar    container tombol upload
	 * @param eventListener listener refresh
	 */
	private void tampilkanButton(Hbox hboxGambar, final EventListener eventListener) {
		Common.clear(hboxGambar);
		LampiranLain.createDownloadUploadFileLain(hboxGambar, penyediaAsset.getId(),
				"Galery_PenyediaAsset_" + Common.getGeneratedBarCode(), "Galeri Gambar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();
						maps.put(lainMahasiswaCover.getId(), lainMahasiswaCover);
						reloadDataGambar(penyediaAsset);

						Common.createDefaultTimer(eventListener);
					}
				});
	}

	/**
	 * <b>Tujuan:</b> Memuat ulang tampilan galeri foto/logo perusahaan pada grid
	 * galeri ({@code myGridGaleri}), menampilkan semua gambar yang ada di {@code maps}
	 * beserta kemampuan mengedit deskripsi dan menghapus tiap gambar.<br><br>
	 *
	 * <b>Cara kerja:</b> Membersihkan seluruh baris {@code myGridGaleri} dengan
	 * {@code Common.clear}, kemudian untuk setiap {@code LampiranLain} dalam
	 * {@code maps}: membuat baris dengan Image (tautan dari
	 * {@code FileFotoLain.ambilLinkLampiranLain}), Textbox deskripsi yang
	 * menyimpan perubahan ke database via {@code StreamingHibernateUtil} saat
	 * {@code onChange}, dan tombol hapus yang menghapus dari {@code maps} dan
	 * menghapus entitas dari database via {@code StreamingHibernateUtil}. Dalam
	 * mode persetujuan, deskripsi ditampilkan sebagai Label baca saja dan tidak
	 * ada tombol hapus.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param penyediaAsset entitas vendor pemilik galeri; digunakan sebagai
	 *                      referensi saat menghapus gambar dan memanggil
	 *                      {@code PenyediaAsset.reloadGaleries}<br><br>
	 *
	 * <b>Return:</b> void<br><br>
	 *
	 * <b>Penanganan error:</b> Operasi database pada {@code StreamingHibernateUtil}
	 * dibungkus try-catch dengan rollback eksplisit.<br><br>
	 *
	 * <b>Pemeliharaan:</b> {@code maps} adalah HashMap yang dibagikan antara
	 * {@code tampilkanButton} dan metode ini; pastikan tidak ada modifikasi
	 * konkuren dari thread lain.
	 *
	 * @param penyediaAsset entitas vendor pemilik galeri
	 * @throws Exception jika terjadi kesalahan merender komponen
	 */
	private void reloadDataGambar(final PenyediaAsset penyediaAsset) throws Exception {
		Common.clear(myGridGaleri);

		for (final LampiranLain lampiranLain : maps.values()) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(myGridGaleri);

			String link = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class);

			Image image = new Image(link);
			image.setStyle("max-width: 256px !important;min-width: 60px !important;min-height: 300px !important;");
			image.setSclass("gambar_profile");
			image.setWidth("95%");
			image.setParent(row);

			final Textbox textbox = new Textbox(lampiranLain.getDeskripsi());
			textbox.setWidth("90%");
			textbox.setRows(12);

			if (persetujuan) {
				row.appendChild(new Label(lampiranLain.getDeskripsi()));
			} else {
				row.appendChild(textbox);
			}

			textbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lampiranLain);
						lampiranLain.setDeskripsi(textbox.getValue());

						session.getTransaction().begin();
						session.update(lampiranLain);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("penyimpanan deskripsi lampiran galeri Penyedia Asset", e,
								new String[] { "Ulangi kembali penyimpanan deskripsi lampiran ini.",
										"Pastikan lampiran yang dimaksud belum dihapus oleh pengguna lain secara bersamaan." });
					}

				}
			});

			if (persetujuan) {
				new Label().setParent(row);
			} else {

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

												LampiranLain d = maps.remove(lampiranLain.getId());
												System.out.println("d = > " + d);

												try {
													Session session = StreamingHibernateUtil.getInstance()
															.currentSession();

													session.getTransaction().begin();
													session.delete(lampiranLain);
													session.getTransaction().commit();

													StreamingHibernateUtil.getInstance().closeSession();
												} catch (Exception e) {
													StreamingHibernateUtil.getInstance().rollbackTransaction();
													Common.tampilErrorJikaAdmin(e);
													PesanFormalHelper.tampilkanGagalException(
															"penghapusan lampiran galeri Penyedia Asset", e,
															new String[] { "Ulangi kembali penghapusan lampiran ini.",
																	"Muat ulang halaman apabila lampiran tetap tampil setelah dihapus." });
												}

												reloadDataGambar(penyediaAsset);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException(
														"penghapusan lampiran galeri Penyedia Asset " + penyediaAsset.getNama(),
														"Data lampiran ini kemungkinan masih berelasi/dipakai oleh data lain di sistem "
																+ "sehingga tidak dapat dihapus secara langsung.",
														e,
														new String[] {
																"Periksa apakah lampiran ini masih dirujuk oleh data Penyedia Asset atau transaksi lain.",
																"Hubungi Administrator Sistem apabila lampiran memang perlu dihapus paksa." });
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

	/**
	 * <b>Tujuan:</b> Memeriksa apakah kode perusahaan yang dimasukkan sudah
	 * digunakan oleh entitas {@code PenyediaAsset} lain di database, untuk
	 * mencegah duplikasi kode.<br><br>
	 *
	 * <b>Cara kerja:</b> Menggunakan {@code HibernateUtil.currentSession()} untuk
	 * membuat Criteria ke tabel {@code PenyediaAsset} dengan filter
	 * {@code eq("kode", kode.getValue().trim())}. Jika penyedia yang sedang
	 * diedit sudah memiliki id (bukan baru), menambahkan filter
	 * {@code ne("id", penyediaAsset.getId())} untuk mengecualikan dirinya sendiri
	 * dari pengecekan. Mengembalikan true jika ada record duplikat (count > 0).<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code true} jika kode sudah digunakan oleh penyedia lain
	 *         (artinya simpan harus dibatalkan), {@code false} jika kode bebas.<br><br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; exception dari
	 * Hibernate dibiarkan melempar ke atas.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Nama metode "checkNamaAgama" tidak mencerminkan
	 * fungsinya yang sebenarnya (memeriksa kode, bukan nama atau agama).
	 * Pertimbangkan untuk merename menjadi {@code checkKodeSudahAda} di
	 * refactoring mendatang.
	 *
	 * @return true jika kode sudah dipakai penyedia lain
	 */
	public Boolean checkNamaAgama() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PenyediaAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.penyediaAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.penyediaAsset.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	/**
	 * <b>Tujuan:</b> Mencegah munculnya data vendor ganda dengan memeriksa apakah NPWP
	 * yang diisi sudah dipakai penyedia/perusahaan lain. Satu badan usaha hanya boleh
	 * memiliki satu NPWP, sehingga duplikasi NPWP hampir selalu berarti data dobel yang
	 * harus dicegah sejak awal demi kebersihan master vendor dan akurasi pelaporan.<br><br>
	 *
	 * <b>Cara kerja:</b> Menjalankan query COUNT pada {@code PenyediaAsset} dengan NPWP
	 * sama persis (setelah trim), mengecualikan record yang sedang diedit (jika ID tidak
	 * null) via {@code Restrictions.ne("id", ...)} agar operasi ubah tidak mendeteksi
	 * dirinya sendiri sebagai duplikat. Jika NPWP kosong, dianggap tidak duplikat
	 * (validasi wajib-isi NPWP sudah ditangani terpisah di {@code onSave}).<br><br>
	 *
	 * @return {@code true} bila NPWP sudah dipakai vendor lain; {@code false} bila unik
	 *         atau kosong.
	 */
	public boolean checkNpwpSudahAda() {
		String npwpVal = npwp == null ? "" : npwp.getValue().trim();
		if (npwpVal.isEmpty()) {
			return false;
		}
		Session session = HibernateUtil.currentSession();
		Number jml = (Number) session.createCriteria(PenyediaAsset.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("npwp", npwpVal))
				.add(this.penyediaAsset.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.penyediaAsset.getId()))
				.uniqueResult();
		return jml != null && jml.intValue() > 0;
	}

	/**
	 * Menghasilkan "chip" HTML kecil berisi persen kelengkapan berkas vendor (NPWP,
	 * akta pendirian, rekening/bank, email, telepon, alamat) dengan warna lampu lalu
	 * lintas &mdash; hijau (&ge;80%), kuning (&ge;50%), merah (&lt;50%) &mdash; supaya
	 * admin bisa melihat vendor mana yang datanya masih kurang langsung dari daftar
	 * tanpa membuka detail. Memakai 6 kolom kunci yang sama dengan panel kelengkapan
	 * pada dashboard agar penilaian konsisten.
	 *
	 * @param p entitas vendor yang dinilai
	 * @return potongan HTML {@code <span>} berwarna, mis. {@code "83% lengkap"}
	 */
	private String chipKelengkapan(PenyediaAsset p) {
		int terisi = 0;
		terisi += adaIsi(p.getNpwp()) ? 1 : 0;
		terisi += adaIsi(p.getNoAktaPendirian()) ? 1 : 0;
		terisi += adaIsi(p.getBank()) ? 1 : 0;
		terisi += adaIsi(p.getEmail()) ? 1 : 0;
		terisi += adaIsi(p.getTelp()) ? 1 : 0;
		terisi += adaIsi(p.getAlamat()) ? 1 : 0;
		int persen = (int) Math.round(terisi * 100.0 / 6.0);
		String warna = persen >= 80 ? "#16a34a" : (persen >= 50 ? "#d97706" : "#dc2626");
		String bg = persen >= 80 ? "#dcfce7" : (persen >= 50 ? "#fef3c7" : "#fee2e2");
		return "<span title='Kelengkapan berkas vendor' style='display:inline-block;padding:1px 7px;"
				+ "border-radius:999px;font-size:10px;font-weight:800;color:" + warna + ";background:" + bg + ";'>"
				+ persen + "% lengkap</span>";
	}

	/** True bila string tidak null dan tidak kosong setelah di-trim. */
	private static boolean adaIsi(String s) {
		return s != null && !s.trim().isEmpty();
	}

	/**
	 * Mengisi tiga combobox filter daftar (Kategori, Jenis, Status) dengan data referensi yang
	 * aktif, masing-masing diberi item pertama "&mdash; Semua &mdash;" (nilai {@code null}) sebagai
	 * default agar tidak menyaring apa pun. Dipanggil sekali dari {@code doAfterCompose} sebelum
	 * pencarian awal.
	 */
	private void isiFilterCombo() {
		isiSatuFilterCombo(searchKategori, KategoriPenyediaAsset.class);
		isiSatuFilterCombo(searchJenis, JenisPenyediaAsset.class);
		isiSatuFilterCombo(searchStatus, ais.database.model.asset.StatusPenyediaAsset.class);
	}

	/**
	 * Mengisi satu combobox filter dari entitas referensi (label = {@code nama}) yang berstatus
	 * aktif, lalu menyisipkan item "&mdash; Semua &mdash;" di awal dan memilihnya sebagai default.
	 *
	 * @param combo combobox target; diabaikan bila {@code null} (mis. dipanggil di konteks non-daftar)
	 * @param clazz kelas entitas referensi (Kategori/Jenis/Status PenyediaAsset)
	 */
	private void isiSatuFilterCombo(Combobox combo, Class<?> clazz) {
		if (combo == null) {
			return;
		}
		Common.insertCombo(combo, new String[] { "nama" }, "keterangan", clazz, Restrictions.eq("aktif", true));
		Comboitem semua = new Comboitem("— Semua —");
		semua.setValue(null);
		combo.insertBefore(semua, combo.getFirstChild());
		combo.setSelectedItem(semua);
	}

	/**
	 * <b>Tujuan:</b> Memvalidasi semua field wajib, mengumpulkan nilai dari
	 * seluruh komponen UI, dan menyimpan (atau memperbarui) entitas
	 * {@code PenyediaAsset} ke database beserta seluruh dokumen terkait
	 * (lampiran, galeri, dokumen persyaratan). Ini adalah metode simpan utama
	 * yang mengorkestrasi penyimpanan kompleks multi-tab.<br><br>
	 *
	 * <b>Cara kerja:</b> Pertama memvalidasi: nama, NPWP, jenis pekerjaan utama,
	 * kategori, jenis perusahaan tidak boleh kosong, dan kode tidak boleh duplikat
	 * ({@code checkNamaAgama}). Jika lolos validasi, memuat ulang entitas dari
	 * database jika sudah ada (untuk menghindari stale object), kemudian mengisi
	 * semua field dari komponen UI (kecamatan, propinsi, kota, nama, keterangan,
	 * alamat, email, fax, kode, kodePos, kontak, telp, kategori, jenis perusahaan,
	 * akta pendirian, NPWP, bank utama, rekening, akun utang, tanggal, dll.).
	 * Jika kode masih kosong, di-generate ulang dengan {@code generateCode(true)}.
	 * Memanggil {@code Common.refreshSaveOrUpdate} untuk menyimpan entitas.
	 * Setelah simpan, menjalankan timer async untuk: memperbarui sesi HTTP jika
	 * yang login adalah vendor penyedia tersebut, menyimpan semua
	 * {@code PenyediaAssetPunyaDokumen}, memperbarui referensi lampiran (NPWP,
	 * akta, pengesahan, galeri) ke id penyedia yang baru tersimpan, dan memanggil
	 * {@code PenyediaAsset.reloadGaleries}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK dari klik tombol Simpan; tidak digunakan secara
	 *              langsung dalam implementasi.<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code true} jika penyimpanan berhasil dan form boleh ditutup,
	 *         {@code false} jika validasi gagal (form tetap terbuka dengan
	 *         pesan error yang sesuai).<br><br>
	 *
	 * <b>Penanganan error:</b> Validasi gagal menampilkan dialog informasi.
	 * Error pada pembaruan lampiran ditangani dengan rollback
	 * {@code StreamingHibernateUtil} dan ditampilkan ke admin.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Jika menambah field baru ke form, tambahkan pula
	 * baris {@code penyediaAsset.setXxx()} di blok pengisian field. Pastikan
	 * urutan validasi konsisten dengan field yang ditampilkan di form.
	 *
	 * @param event event ZK pemicu simpan
	 * @return true jika simpan berhasil
	 * @throws Exception jika terjadi kesalahan saat menyimpan
	 */
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Penyedia/Perusahaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Nama dengan nama resmi penyedia/perusahaan; (2) Nama wajib diisi untuk identifikasi penyedia dalam sistem; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (npwp.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, NPWP Penyedia/Perusahaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field NPWP dengan nomor NPWP yang valid (format: XX.XXX.XXX.X-XXX.XXX); (2) NPWP wajib untuk keperluan perpajakan dan verifikasi identitas; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisPekerjaanPenyedia1.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Pekerjaan Utama belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis pekerjaan utama dari dropdown; (2) Jenis pekerjaan menentukan bidang kompetensi penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kategoriPenyediaAsset.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kategori Penyedia/Perusahaan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih kategori penyedia dari dropdown yang tersedia; (2) Jika kategori belum ada, tambahkan melalui menu Kategori Penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisPenyediaAsset.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Penyedia/Perusahaan belum dipilih. Langkah yang dapat dilakukan: (1) Pilih jenis penyedia dari dropdown yang tersedia; (2) Jika jenis belum ada, tambahkan melalui menu Jenis Penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
//		if (statusPenyediaAsset.getSelectedItem() == null) {
//			MyMessageboxConfig.show("Jenis Penyedia / Perusahaan harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		if (!kode.getValue().trim().isEmpty()) {
			boolean i = checkNamaAgama();
			if (i) {
				MyMessageboxConfig.show("Kode sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		// Cegah vendor ganda berdasarkan NPWP (satu badan usaha = satu NPWP).
		if (checkNpwpSudahAda()) {
			MyMessageboxConfig.show("NPWP sudah terdaftar pada penyedia / perusahaan lain", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		// Data vendor berubah → batalkan cache dashboard agar ringkasan ikut ter-update.
		try {
			ais.common.DashboardCacheUtil
					.invalidateL3(ais.action.master.asset.helper.DasboardVendor.kunciCache());
		} catch (Exception abaikanCache) { ais.common.ErrorAuditUtil.record(abaikanCache, "auto-audit(empty-catch) src/ais/action/master/asset/PenyediaAssetAction.java:3929");
		}

		Session session = HibernateUtil.currentSession();
		if (penyediaAsset.getId() != null) {
			penyediaAsset = (PenyediaAsset) session.load(PenyediaAsset.class, penyediaAsset.getId());
		}

		penyediaAsset.setPemilik(pemilik.getValue().trim());

		penyediaAsset.setJenisPekerjaanPenyedia1(
				(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia1.getSelectedItem() == null ? null
						: jenisPekerjaanPenyedia1.getSelectedItem().getValue()));
		penyediaAsset.setJenisPekerjaanPenyedia2(
				(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia2.getSelectedItem() == null ? null
						: jenisPekerjaanPenyedia2.getSelectedItem().getValue()));
		penyediaAsset.setJenisPekerjaanPenyedia3(
				(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia3.getSelectedItem() == null ? null
						: jenisPekerjaanPenyedia3.getSelectedItem().getValue()));
		penyediaAsset.setJenisPekerjaanPenyedia4(
				(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia4.getSelectedItem() == null ? null
						: jenisPekerjaanPenyedia4.getSelectedItem().getValue()));
		penyediaAsset.setJenisPekerjaanPenyedia5(
				(JenisPekerjaanPenyedia) (jenisPekerjaanPenyedia5.getSelectedItem() == null ? null
						: jenisPekerjaanPenyedia5.getSelectedItem().getValue()));

		penyediaAsset.setKecamatan((Wilayah) kecamatan.getAttribute("wilayah"));
		penyediaAsset.setPropinsi((Propinsi) (propinsi.getAttribute("wilayah")));
		penyediaAsset.setKota((Kota) (kota.getAttribute("wilayah")));
		penyediaAsset.setNama(nama.getValue());
		penyediaAsset.setKeterangan(keterangan.getValue());
		penyediaAsset.setAlamat(alamat.getValue());
		penyediaAsset.setEmail(email.getValue());
		penyediaAsset.setFax(fax.getValue());
		penyediaAsset.setKode(kode.getValue().trim());
		penyediaAsset.setKodePos(kodePos.getValue());
		penyediaAsset.setKontak(kontak.getValue());
		penyediaAsset.setTelp(telp.getValue());
		penyediaAsset
				.setKategoriPenyediaAsset((KategoriPenyediaAsset) kategoriPenyediaAsset.getSelectedItem().getValue());
		penyediaAsset.setJenisPenyediaAsset((JenisPenyediaAsset) jenisPenyediaAsset.getSelectedItem().getValue());

//		penyediaAsset.setStatusPenyediaAsset(
//				(StatusPenyediaAsset) statusPenyediaAsset.getSelectedItem().getAttribute("value"));

		penyediaAsset.setNoAktaPendirian(noAktaPendirian.getValue().trim());
		penyediaAsset.setTanggalAktaPendirian(tanggalAktaPendirian.getValue());
		penyediaAsset.setNamaNotaris(namaNotaris.getValue().trim());
		penyediaAsset.setNoPengesahan(noPengesahan.getValue().trim());
		penyediaAsset.setTanggalPengesahan(tanggalPengesahan.getValue());

		penyediaAsset.setNoAktaPendirianAkhir(noAktaPendirianAkhir.getValue().trim());
		penyediaAsset.setTanggalAktaPendirianAkhir(tanggalAktaPendirianAkhir.getValue());
		penyediaAsset.setNamaNotarisAkhir(namaNotarisAkhir.getValue().trim());
		penyediaAsset.setNoPengesahanAkhir(noPengesahanAkhir.getValue().trim());
		penyediaAsset.setTanggalPengesahanAkhir(tanggalPengesahanAkhir.getValue());
		penyediaAsset.setNpwp(npwp.getValue().trim());

		penyediaAsset.setBankUtama(
				(Bank) (bankUtama.getSelectedItem() == null ? null : bankUtama.getSelectedItem().getValue()));
		penyediaAsset.setAtasNama(atasNama.getValue());
		penyediaAsset.setNoRek(noRek.getValue());
		penyediaAsset.setAkunUtang((Akun) akunUtang.getAttribute("akun"));

		penyediaAsset.setTanggalPembuatan(tanggalPembuatan.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			penyediaAsset.setDisposisiSop(disposisiSop);
		}

		if (array != null) {
			penyediaAsset.setBank(array.toString());
		}

		if (kode.getValue().trim().isEmpty()) {
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			penyediaAsset.setKode(kode.getValue());
		}

		// Vendor yang DITAMBAH langsung oleh admin (record baru, di luar pendaftaran mandiri web
		// & di luar alur disposisi SOP) langsung disahkan sebagai vendor resmi agar tampil di tab
		// "Penyedia" — bukan "Calon Vendor". Pendaftar via web (initDaftar) TIDAK melewati onSave
		// ini, sehingga disetujuiOleh tetap null → tampil di tab "Calon Vendor".
		if (penyediaAsset.getId() == null && disposisiSop == null && penyediaAsset.getDisetujuiOleh() == null
				&& Common.getCurrentUser() != null) {
			penyediaAsset.setDisetujuiOleh(Common.getCurrentUser());
			penyediaAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		}

		Common.refreshSaveOrUpdate(session, penyediaAsset);

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getPenyediaAsset() != null
						&& tbmuser.getPenyediaAsset().getId().equals(penyediaAsset.getId())) {
					tbmuser.setPenyediaAsset(penyediaAsset);

					HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
					HttpSession session1 = request.getSession(true);
					session1.setAttribute("PenyediaAsset", tbmuser.getPenyediaAsset());
					session1.setAttribute("mytbmuser", tbmuser);
					session1.setAttribute("usersTemp", tbmuser);
					session1.setAttribute("user", tbmuser);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Executions.getCurrent().sendRedirect("");
						}
					});

				}

				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					List<Row> rows = rowsDokumen.getChildren();
					for (Row row : rows) {
						PenyediaAssetPunyaDokumen penyediaAssetPunyaDokumen = (PenyediaAssetPunyaDokumen) row
								.getAttribute("penyediaAssetPunyaDokumen");
						if (penyediaAssetPunyaDokumen != null) {
							penyediaAssetPunyaDokumen.setPenyediaAsset(penyediaAsset);
							Common.refreshSaveOrUpdate(penyediaAssetPunyaDokumen);

							LampiranLain lampiranLain = (LampiranLain) row.getAttribute("lampiranLain");
							if (lampiranLain != null && lampiranLain.getId() != null) {

								session.refresh(lampiranLain);
								lampiranLain.setRef(penyediaAssetPunyaDokumen.getId());
								session.getTransaction().begin();
								session.update(lampiranLain);
								session.getTransaction().commit();
							}
						}
					}

					if (dokumenPaktaIntegritas != null && dokumenPaktaIntegritas.getId() != null) {
						session.refresh(dokumenPaktaIntegritas);
						dokumenPaktaIntegritas.setRef(penyediaAsset.getId());

						session.getTransaction().begin();
						session.update(dokumenPaktaIntegritas);
						session.getTransaction().commit();
					}

					if (dokumenNPWP != null && dokumenNPWP.getId() != null) {
						session.refresh(dokumenNPWP);
						dokumenNPWP.setRef(penyediaAsset.getId());

						session.getTransaction().begin();
						session.update(dokumenNPWP);
						session.getTransaction().commit();
					}

					if (dokumenAktaPendirian != null && dokumenAktaPendirian.getId() != null) {
						session.refresh(dokumenAktaPendirian);
						dokumenAktaPendirian.setRef(penyediaAsset.getId());

						session.getTransaction().begin();
						session.update(dokumenAktaPendirian);
						session.getTransaction().commit();
					}

					if (dokumenPengesahan != null && dokumenPengesahan.getId() != null) {
						session.refresh(dokumenPengesahan);
						dokumenPengesahan.setRef(penyediaAsset.getId());

						session.getTransaction().begin();
						session.update(dokumenPengesahan);
						session.getTransaction().commit();
					}

					if (dokumenAktaPerubahan != null && dokumenAktaPerubahan.getId() != null) {
						session.refresh(dokumenAktaPerubahan);
						dokumenAktaPerubahan.setRef(penyediaAsset.getId());

						session.getTransaction().begin();
						session.update(dokumenAktaPerubahan);
						session.getTransaction().commit();
					}

					if (dokumenPengesahanAkhir != null && dokumenPengesahanAkhir.getId() != null) {
						session.refresh(dokumenPengesahanAkhir);
						dokumenPengesahanAkhir.setRef(penyediaAsset.getId());

						session.getTransaction().begin();
						session.update(dokumenPengesahanAkhir);
						session.getTransaction().commit();
					}

					for (LampiranLain lampiranLain : maps.values()) {

						if (lampiranLain.getId() != null) {
							session.refresh(lampiranLain);
							lampiranLain.setRef(penyediaAsset.getId());

							session.getTransaction().begin();
							session.update(lampiranLain);
							session.getTransaction().commit();
						}
					}

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException(
							"pengaitan berkas dokumen/lampiran ke data Penyedia Asset " + penyediaAsset.getNama(), e,
							new String[] { "Ulangi kembali proses simpan data Penyedia Asset ini.",
									"Periksa kembali berkas dokumen (pengesahan, akta perubahan, lampiran lain) yang diunggah, kemungkinan berkas rusak atau terlalu besar." });
				}

				PenyediaAsset.reloadGaleries(penyediaAsset);
			}
		});

		return true;
	}

	// ════════════════════════════════════════════════════════════════════════════════
	// TAB "CALON VENDOR" — pendaftar (perusahaan) via web dashboard vendor yang BELUM
	// disahkan (disetujuiOleh == null). Berisi: daftar pendaftar, tombol buat & kirim akun
	// (user/password) ke email + unduh Excel, verifikasi kelengkapan berkas, dan tombol
	// "Terima jadi Vendor" yang menyahkan pendaftar → otomatis pindah ke tab "Penyedia".
	// Lihat juga onSearchDefault (filter disetujuiOleh IS NOT NULL) & onSave (auto-sah admin).
	// ════════════════════════════════════════════════════════════════════════════════
	private Tabpanel calonVendor;

	/**
	 * Handler tab "Calon Vendor" (forward onClick dari penyedia_aset.zul). Memuat ulang
	 * daftar calon vendor setiap kali tab dibuka agar status terbaru langsung tampak.
	 *
	 * @param event event ZK pemicu (boleh null)
	 * @throws Exception bila terjadi kesalahan saat merender
	 */
	public void onCalonVendor(Event event) throws Exception {
		if (calonVendor == null) {
			return;
		}
		renderCalonVendor();
	}

	/**
	 * Merender isi panel "Calon Vendor": kartu toolbar (Segarkan, Unduh Akun Excel) + grid
	 * daftar pendaftar yang belum disahkan (disetujuiOleh == null), masing-masing dengan
	 * tombol aksi: Akun &amp; Kirim, Verifikasi, dan Terima jadi Vendor.
	 *
	 * @throws Exception bila terjadi kesalahan saat membangun komponen / query
	 */
	@SuppressWarnings("unchecked")
	private void renderCalonVendor() throws Exception {
		Common.clear(calonVendor);

		org.zkoss.zul.Div portal = new org.zkoss.zul.Div();
		portal.setSclass("ais-crud-portal");
		portal.setParent(calonVendor);

		// ── KARTU FILTER + TOOLBAR ──
		org.zkoss.zul.Div filterCard = new org.zkoss.zul.Div();
		filterCard.setSclass("ais-crud-filter-card");
		filterCard.setParent(portal);

		org.zkoss.zul.Div filterHead = new org.zkoss.zul.Div();
		filterHead.setSclass("ais-crud-filter-head");
		filterHead.setParent(filterCard);
		Label judul = new Label(ais.common.Common.getBahasaConfig("Calon Vendor — pendaftar via web yang menunggu persetujuan"));
		judul.setSclass("ais-crud-filter-title");
		judul.setParent(filterHead);
		org.zkoss.zul.Div headAksi = new org.zkoss.zul.Div();
		headAksi.setSclass("ais-crud-filter-actions");
		headAksi.setParent(filterHead);

		// Input filter (final agar dapat diakses listener & helper).
		final Textbox fNama = new Textbox();
		fNama.setWidth("95%");
		final org.zkoss.zul.Datebox fTgl = new org.zkoss.zul.Datebox();
		fTgl.setWidth("95%");
		fTgl.setFormat("dd-MM-yyyy");
		final Combobox fKategori = new Combobox();
		fKategori.setWidth("95%");
		fKategori.setReadonly(true);
		Common.insertCombo(fKategori, new String[] { "nama" }, "keterangan", KategoriPenyediaAsset.class,
				Restrictions.eq("aktif", true));
		final Combobox fJenis = new Combobox();
		fJenis.setWidth("95%");
		fJenis.setReadonly(true);
		Common.insertCombo(fJenis, new String[] { "nama" }, "keterangan", JenisPenyediaAsset.class,
				Restrictions.eq("aktif", true));

		// Wadah grid (dideklarasi dulu agar bisa dirujuk listener toolbar).
		org.zkoss.zul.Div dataCard = new org.zkoss.zul.Div();
		dataCard.setSclass("ais-crud-data-card");
		final org.zkoss.zul.Div gridHolder = new org.zkoss.zul.Div();

		MyToolbarbuttonConfig btnCari = new MyToolbarbuttonConfig("Cari", "/img/search.gif");
		btnCari.setTooltiptext("Cari calon vendor sesuai filter");
		btnCari.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				muatGridCalon(gridHolder, fNama, fTgl, fKategori, fJenis);
			}
		});
		btnCari.setParent(headAksi);

		MyToolbarbuttonConfig btnSegarkan = new MyToolbarbuttonConfig("Segarkan", "/img/refresh.png");
		btnSegarkan.setTooltiptext("Bersihkan filter & muat ulang");
		btnSegarkan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				fNama.setValue("");
				fTgl.setValue(null);
				fKategori.setSelectedItem(null);
				fJenis.setSelectedItem(null);
				muatGridCalon(gridHolder, fNama, fTgl, fKategori, fJenis);
			}
		});
		btnSegarkan.setParent(headAksi);

		MyToolbarbuttonConfig btnCreate = new MyToolbarbuttonConfig("Create User & Password", "/img/mail.png");
		btnCreate.setTooltiptext(
				"Buat user & password lalu kirim ke email masing-masing calon vendor (sesuai daftar/filter)");
		btnCreate.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				buatKirimAkunMassal(fNama, fTgl, fKategori, fJenis, gridHolder);
			}
		});
		btnCreate.setParent(headAksi);

		MyToolbarbuttonConfig btnExcel = new MyToolbarbuttonConfig("Unduh User & Password (Excel)", "/img/print.png");
		btnExcel.setTooltiptext("Buat (bila belum ada) lalu unduh user & password ke Excel (sesuai daftar/filter)");
		btnExcel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				unduhAkunCalonExcel(fNama, fTgl, fKategori, fJenis);
			}
		});
		btnExcel.setParent(headAksi);

		// ── BODY FILTER (grid 2 kolom: Nama|Tgl Daftar, Kategori|Jenis) ──
		org.zkoss.zul.Div filterBody = new org.zkoss.zul.Div();
		filterBody.setSclass("ais-crud-filter-body");
		filterBody.setParent(filterCard);
		MyGrid fgrid = new MyGrid();
		fgrid.setFixedLayout(true);
		fgrid.setSclass("ais-crud-filter-grid");
		fgrid.setParent(filterBody);
		Columns fcols = new Columns();
		fcols.setParent(fgrid);
		tambahKolomLebar(fcols, "150px");
		tambahKolomLebar(fcols, null);
		tambahKolomLebar(fcols, "150px");
		tambahKolomLebar(fcols, null);
		Rows frows = new Rows();
		frows.setParent(fgrid);
		Row fr1 = new Row();
		fr1.setParent(frows);
		fr1.appendChild(new ais.ui.util.MyLabelConfig("Nama Perusahaan"));
		fr1.appendChild(fNama);
		fr1.appendChild(new ais.ui.util.MyLabelConfig("Tgl Daftar"));
		fr1.appendChild(fTgl);
		Row fr2 = new Row();
		fr2.setParent(frows);
		fr2.appendChild(new ais.ui.util.MyLabelConfig("Kategori"));
		fr2.appendChild(fKategori);
		fr2.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		fr2.appendChild(fJenis);

		// Enter di Nama atau pilih Kategori/Jenis → langsung cari.
		EventListener cariListener = new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				muatGridCalon(gridHolder, fNama, fTgl, fKategori, fJenis);
			}
		};
		fNama.addEventListener("onOK", cariListener);
		fKategori.addEventListener("onSelect", cariListener);
		fJenis.addEventListener("onSelect", cariListener);

		// ── KARTU DATA ──
		dataCard.setParent(portal);
		gridHolder.setParent(dataCard);

		muatGridCalon(gridHolder, fNama, fTgl, fKategori, fJenis);
	}

	/** Membangun Criteria calon vendor (disetujuiOleh == null) + filter Nama/Tgl Daftar/Kategori/Jenis. */
	private Criteria kriteriaCalon(Textbox fNama, org.zkoss.zul.Datebox fTgl, Combobox fKategori, Combobox fJenis) {
		Criteria c = HibernateUtil.currentSession().createCriteria(PenyediaAsset.class)
				.add(Restrictions.isNull("disetujuiOleh"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (fNama != null && fNama.getValue() != null && !fNama.getValue().trim().isEmpty()) {
			c.add(Restrictions.ilike("nama", fNama.getValue().trim(), MatchMode.ANYWHERE));
		}
		if (fTgl != null && fTgl.getValue() != null) {
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTime(fTgl.getValue());
			cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
			cal.set(java.util.Calendar.MINUTE, 0);
			cal.set(java.util.Calendar.SECOND, 0);
			cal.set(java.util.Calendar.MILLISECOND, 0);
			java.util.Date dari = cal.getTime();
			cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
			java.util.Date sampai = cal.getTime();
			c.add(Restrictions.ge("tanggalPembuatan", dari)).add(Restrictions.lt("tanggalPembuatan", sampai));
		}
		if (fKategori != null && fKategori.getSelectedItem() != null
				&& fKategori.getSelectedItem().getValue() != null) {
			c.add(Restrictions.eq("kategoriPenyediaAsset", fKategori.getSelectedItem().getValue()));
		}
		if (fJenis != null && fJenis.getSelectedItem() != null && fJenis.getSelectedItem().getValue() != null) {
			c.add(Restrictions.eq("jenisPenyediaAsset", fJenis.getSelectedItem().getValue()));
		}
		return c;
	}

	/** Memuat ulang HANYA grid calon vendor (nilai filter dipertahankan). */
	@SuppressWarnings("unchecked")
	private void muatGridCalon(final Component gridHolder, final Textbox fNama, final org.zkoss.zul.Datebox fTgl,
			final Combobox fKategori, final Combobox fJenis) throws Exception {
		Common.clear(gridHolder);

		MyGrid grid = new MyGrid();
		grid.setSclass("dgrid");
		grid.setFixedLayout(true);
		grid.setParent(gridHolder);

		Columns columns = new Columns();
		columns.setParent(grid);
		tambahKolomCalon(columns, "Kode", "7%");
		tambahKolomCalon(columns, "Nama Perusahaan", "16%");
		tambahKolomCalon(columns, "Kategori", "9%");
		tambahKolomCalon(columns, "Jenis", "9%");
		tambahKolomCalon(columns, "Email", "13%");
		tambahKolomCalon(columns, "Telp / HP", "8%");
		tambahKolomCalon(columns, "Tgl Daftar", "8%");
		tambahKolomCalon(columns, "Kelengkapan", "8%");
		tambahKolomCalon(columns, "Aksi", "");

		Rows rows = new Rows();
		rows.setParent(grid);

		final EventListener refreshGrid = new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				muatGridCalon(gridHolder, fNama, fTgl, fKategori, fJenis);
			}
		};

		List<PenyediaAsset> calonList = kriteriaCalon(fNama, fTgl, fKategori, fJenis).addOrder(Order.desc("id"))
				.setMaxResults(5000).list();

		java.text.SimpleDateFormat fmtTgl = new java.text.SimpleDateFormat("dd-MM-yyyy");

		for (int i = 0; i < calonList.size(); i++) {
			final PenyediaAsset p = calonList.get(i);
			Row row = new Row();
			row.setValign("middle");
			row.setParent(rows);

			row.appendChild(new Label(p.getKode() == null ? "" : p.getKode()));
			row.appendChild(new Label(p.getNama() == null ? "" : p.getNama()));
			row.appendChild(new Label(p.getKategoriPenyediaAsset() == null ? ""
					: p.getKategoriPenyediaAsset().getNama()));
			row.appendChild(new Label(p.getJenisPenyediaAsset() == null ? ""
					: p.getJenisPenyediaAsset().getNama()));
			row.appendChild(new Label(p.getEmail() == null ? "" : p.getEmail()));
			row.appendChild(new Label(p.getTelp() == null ? "" : p.getTelp()));
			row.appendChild(new Label(p.getTanggalPembuatan() == null ? "-" : fmtTgl.format(p.getTanggalPembuatan())));
			row.appendChild(new org.zkoss.zul.Html(chipKelengkapan(p)));

			org.zkoss.zul.Hbox aksi = new org.zkoss.zul.Hbox();
			aksi.setSpacing("4px");

			MyToolbarbuttonConfig btnAkun = new MyToolbarbuttonConfig("Akun & Kirim", "/img/mail.png");
			btnAkun.setTooltiptext("Buat user & password lalu kirim ke email pendaftar");
			btnAkun.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					aksiBuatKirimAkun(p);
				}
			});
			aksi.appendChild(btnAkun);

			MyToolbarbuttonConfig btnVerif = new MyToolbarbuttonConfig("Verifikasi", "/img/search.gif");
			btnVerif.setTooltiptext("Periksa & lengkapi berkas yang dikirim pendaftar");
			btnVerif.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					PenyediaAssetAction.onAddExternal(null, refreshGrid, p, null, null);
				}
			});
			aksi.appendChild(btnVerif);

			MyToolbarbuttonConfig btnTerima = new MyToolbarbuttonConfig("Terima jadi Vendor", "/img/ok.png");
			btnTerima.setTooltiptext("Sahkan sebagai vendor resmi → pindah ke tab Penyedia");
			btnTerima.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event e) throws Exception {
					aksiTerimaVendor(p, refreshGrid);
				}
			});
			aksi.appendChild(btnTerima);

			row.appendChild(aksi);
		}

		if (calonList.isEmpty()) {
			Row row = new Row();
			row.setParent(rows);
			org.zkoss.zul.Cell cell = new org.zkoss.zul.Cell();
			cell.setColspan(9);
			cell.setStyle("padding:14px;color:#64748b;");
			cell.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak ada calon vendor yang cocok dengan filter.")));
			cell.setParent(row);
		}
	}

	/**
	 * Tombol "Create User &amp; Password": membuat akun (bila belum ada) untuk SELURUH calon
	 * vendor pada daftar/filter saat ini lalu mengirim user &amp; password ke email masing-masing
	 * (sesuai email yang terdaftar). Calon tanpa email dilewati.
	 */
	private void buatKirimAkunMassal(final Textbox fNama, final org.zkoss.zul.Datebox fTgl, final Combobox fKategori,
			final Combobox fJenis, final Component gridHolder) throws Exception {
		final List<PenyediaAsset> calonList = kriteriaCalon(fNama, fTgl, fKategori, fJenis)
				.add(Restrictions.isNotNull("nama")).addOrder(Order.asc("nama")).setMaxResults(100000).list();
		if (calonList.isEmpty()) {
			MyMessageboxConfig.show("Tidak ada calon vendor pada daftar/filter saat ini.", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		MyMessageboxConfig.show(
				"Buat user & password untuk " + calonList.size()
						+ " calon vendor lalu kirim ke email masing-masing? (yang belum punya email akan dilewati)",
				"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
							return;
						}
						int diproses = 0;
						int terkirim = 0;
						int tanpaEmail = 0;
						for (int i = 0; i < calonList.size(); i++) {
							PenyediaAsset p = calonList.get(i);
							if (p.getNama() == null || p.getNama().trim().isEmpty()) {
								continue;
							}
							boolean adaEmail = p.getEmail() != null && !p.getEmail().trim().isEmpty();
							try {
								buatAtauAmbilUserPenyedia(p, adaEmail);
								diproses++;
								if (adaEmail) {
									terkirim++;
								} else {
									tanpaEmail++;
								}
							} catch (Exception ex) {
								Common.tampilErrorJikaAdmin(ex);
							}
						}
						muatGridCalon(gridHolder, fNama, fTgl, fKategori, fJenis);
						MyMessageboxConfig.show(
								"Selesai. Akun diproses: " + diproses + ", email terkirim: " + terkirim
										+ ", tanpa email (tidak dikirim): " + tanpaEmail + ".",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}
				});
	}

	/** Menambah satu kolom grid (lebar saja, tanpa label) — dipakai grid filter. */
	private void tambahKolomLebar(Columns columns, String width) {
		MyColumnConfig c = new MyColumnConfig();
		if (width != null) {
			c.setWidth(width);
		}
		c.setParent(columns);
	}

	/** Menambahkan satu kolom grid pada tab Calon Vendor. */
	private void tambahKolomCalon(Columns columns, String label, String width) {
		MyColumnConfig c = new MyColumnConfig();
		c.setLabel(label);
		if (width != null && !width.isEmpty()) {
			c.setWidth(width);
		}
		c.setParent(columns);
	}

	/**
	 * Aksi tombol "Akun &amp; Kirim": membuat (bila belum ada) user &amp; password untuk
	 * pendaftar, mengirimkannya ke email pendaftar, lalu menampilkan ringkasan ke admin.
	 *
	 * @param p calon vendor (pendaftar)
	 * @throws Exception bila gagal membuat/mengirim akun
	 */
	private void aksiBuatKirimAkun(final PenyediaAsset p) throws Exception {
		if (p.getEmail() == null || p.getEmail().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Pendaftar belum memiliki email. Lengkapi email lebih dulu melalui tombol Verifikasi.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		String[] akun = buatAtauAmbilUserPenyedia(p, true);
		MyMessageboxConfig.show("Akun untuk \"" + p.getNama() + "\" siap dan telah dikirim ke email " + p.getEmail()
				+ ".\n\nUsername : " + akun[0] + "\nPassword : " + (akun[1] == null ? "(tersimpan)" : akun[1]),
				"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
	}

	/**
	 * Aksi tombol "Terima jadi Vendor": mengesahkan pendaftar sebagai vendor resmi (mengisi
	 * disetujuiOleh + tanggalPersetujuan) sehingga otomatis pindah dari tab "Calon Vendor" ke
	 * tab "Penyedia". Sekaligus memastikan akun login telah dibuat.
	 *
	 * @param p calon vendor (pendaftar)
	 * @throws Exception bila gagal menyimpan
	 */
	private void aksiTerimaVendor(final PenyediaAsset p, final EventListener refreshGrid) throws Exception {
		MyMessageboxConfig.show(
				"Sahkan \"" + p.getNama() + "\" sebagai vendor resmi? Data akan pindah ke tab \"Penyedia\".",
				"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {
					@Override
					public void onEvent(Event ev) throws Exception {
						if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
							return;
						}
						Session session = HibernateUtil.currentNativeSession();
						try {
							PenyediaAsset db = (PenyediaAsset) session.get(PenyediaAsset.class, p.getId());
							if (db != null) {
								db.setDisetujuiOleh(Common.getCurrentUser());
								db.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
								if (db.getAktif() == null) {
									db.setAktif(true);
								}
								session.getTransaction().begin();
								session.update(db);
								session.getTransaction().commit();
							}
						} finally {
							HibernateUtil.closeSession();
						}

						// Pastikan akun login sudah ada agar vendor dapat langsung login.
						try {
							buatAtauAmbilUserPenyedia(p, false);
						} catch (Exception ex) {
							Common.tampilErrorJikaAdmin(ex);
						}

						if (refreshGrid != null) {
							refreshGrid.onEvent(ev);
						} else {
							renderCalonVendor();
						}
						if (searchnama != null) {
							onSearchDefault(null);
						}
						MyMessageboxConfig.show("\"" + p.getNama() + "\" kini terdaftar sebagai vendor resmi.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}
				});
	}

	/**
	 * Membuat (bila belum ada) atau mengambil akun login (Tbmuser ber-role penyedia) untuk
	 * sebuah pendaftar. Bila akun lama tak terbaca passwordnya, password di-reset agar bisa
	 * dikirim ulang. Mengembalikan {username, password}.
	 *
	 * @param penyediaAsset pendaftar/penyedia
	 * @param kirimEmail    bila true, kirim user/password ke email penyedia
	 * @return array {username, password}
	 * @throws Exception bila gagal membuat/mengirim akun
	 */
	private String[] buatAtauAmbilUserPenyedia(final PenyediaAsset penyediaAsset, final boolean kirimEmail)
			throws Exception {
		String username = null;
		String password = null;
		Session session = HibernateUtil.currentNativeSession();
		try {
			Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("penyediaAsset", penyediaAsset)).setMaxResults(1).uniqueResult();

			if (tbmuser == null || tbmuser.getUserId() == null) {
				tbmuser = new Tbmuser();
				String newUsername = (penyediaAsset.getEmail() == null || penyediaAsset.getEmail().trim().isEmpty())
						? StringUtils.split(penyediaAsset.getNama() == null ? "vendor" : penyediaAsset.getNama(),
								" ")[0] + "" + RandomStringUtils.randomNumeric(3)
						: penyediaAsset.getEmail().split(",")[0].trim();
				newUsername = newUsername.toLowerCase().trim();
				password = RandomStringUtils.randomNumeric(5);
				tbmuser.setUserId(newUsername);
				tbmuser.setEmail(penyediaAsset.getEmail());
				tbmuser.setIs_encripted(true);
				tbmuser.setRoot(false);
				tbmuser.setUserNama(penyediaAsset.getNama());
				tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(password.trim()));
				tbmuser.setUserRole(ConstantValues.tbmrolePenyedia);
				tbmuser.setUserShow(1);
				tbmuser.setPenyediaAsset(penyediaAsset);
				session.getTransaction().begin();
				session.save(tbmuser);
				session.getTransaction().commit();
				username = newUsername;
			} else {
				username = tbmuser.getUserId();
				try {
					password = Common.desEncrypter.get().decrypt(tbmuser.getUserPassword());
				} catch (Exception eDec) {
					password = null;
				}
				if (password == null || password.trim().isEmpty()) {
					password = RandomStringUtils.randomNumeric(5);
					tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(password.trim()));
					tbmuser.setIs_encripted(true);
					session.getTransaction().begin();
					session.update(tbmuser);
					session.getTransaction().commit();
				}
			}

			if (kirimEmail && penyediaAsset.getEmail() != null && !penyediaAsset.getEmail().trim().isEmpty()) {
				String subject = "Username dan Password Perusahaan";
				String body = "Username Perusahaan Anda adalah " + username + " dan password " + password
						+ "<br><br>Terima Kasih";
				String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
				JSONArray userIds = new JSONArray();
				userIds.put(username);
				MailSender.sendMail(userIds, subject, body, sender, penyediaAsset.getEmail(), penyediaAsset);
			}
		} finally {
			HibernateUtil.closeSession();
		}
		return new String[] { username, password };
	}

	/**
	 * Mengekspor seluruh user &amp; password calon vendor (disetujuiOleh == null) ke berkas
	 * Excel (.xlsx) dan mengunduhnya. Akun yang belum ada akan dibuat lebih dulu.
	 *
	 * @throws Exception bila gagal membangun/menulis berkas
	 */
	@SuppressWarnings("unchecked")
	private void unduhAkunCalonExcel(final Textbox fNama, final org.zkoss.zul.Datebox fTgl,
			final Combobox fKategori, final Combobox fJenis) throws Exception {
		List<PenyediaAsset> calonList = kriteriaCalon(fNama, fTgl, fKategori, fJenis)
				.add(Restrictions.isNotNull("nama")).addOrder(Order.asc("nama")).setMaxResults(1048576).list();

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("CALON_VENDOR");
		sheet.setDefaultColumnWidth(22);
		XSSFRow head = sheet.createRow(0);
		head.createCell(0).setCellValue("ID");
		head.createCell(1).setCellValue("Username");
		head.createCell(2).setCellValue("Password");
		head.createCell(3).setCellValue("Nama Perusahaan");
		head.createCell(4).setCellValue("Email");
		head.createCell(5).setCellValue("HP");

		int rowIndex = 0;
		for (int i = 0; i < calonList.size(); i++) {
			PenyediaAsset p = calonList.get(i);
			if (p.getNama() == null || p.getNama().trim().isEmpty()) {
				continue;
			}
			rowIndex++;
			String[] akun = buatAtauAmbilUserPenyedia(p, false);
			XSSFRow r = sheet.createRow(rowIndex);
			r.createCell(0).setCellValue(p.getId() == null ? 0 : p.getId());
			r.createCell(1).setCellValue(akun[0] == null ? "" : akun[0]);
			r.createCell(2).setCellValue(akun[1] == null ? "" : akun[1]);
			r.createCell(3).setCellValue(p.getNama());
			r.createCell(4).setCellValue(p.getEmail() == null ? "" : p.getEmail());
			r.createCell(5).setCellValue(p.getTelp() == null ? "" : p.getTelp());
		}

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/user_password_calon_vendor_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
				+ ".xlsx");
		FileOutputStream fileOut = new FileOutputStream(filename);
		workbook.write(fileOut);
		fileOut.close();

		File file = new File(filename);
		Filedownload.save(new FileInputStream(file),
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file.getName());
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code DataCriteria} untuk
	 * membangun objek {@code Criteria} Hibernate yang digunakan dalam pencarian
	 * dan pagination daftar penyedia. Menggabungkan filter dari komponen UI
	 * pencarian (nama, status aktif) dan pembatasan akses berdasarkan pengguna
	 * yang sedang login.<br><br>
	 *
	 * <b>Cara kerja:</b> Mengambil pengguna saat ini dari sesi; jika pengguna
	 * adalah vendor ({@code tbmuser.getPenyediaAsset() != null}), criteria dibatasi
	 * hanya ke id penyedia tersebut. Jika checkbox {@code searchaktif} dicentang
	 * (atau null), menambahkan filter aktif=true atau null. Jika ada input di
	 * {@code searchnama}, menambahkan filter ilike (case-insensitive) dengan
	 * mode ANYWHERE. Jika parameter {@code order} true, menambahkan urutan
	 * ascending berdasarkan nama.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param order {@code true} untuk menambahkan ORDER BY nama ASC pada criteria;
	 *              {@code false} untuk criteria tanpa ordering (digunakan untuk
	 *              menghitung total data pagination)<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Criteria} Hibernate yang siap dijalankan dengan {@code list()}
	 *         atau digunakan untuk {@code Projections.rowCount()}<br><br>
	 *
	 * <b>Pemeliharaan:</b> Perubahan pada field pencarian di ZUL harus diikuti
	 * dengan perubahan pada criteria di sini. Untuk filter tambahan, tambahkan
	 * restriction baru setelah filter yang ada.
	 *
	 * @param order apakah criteria perlu diurutkan
	 * @return criteria Hibernate untuk pencarian penyedia
	 */
	public Criteria initCriteria(boolean order) {

		Tbmuser tbmuser = Common.getCurrentUser();
		PenyediaAsset penyediaAssetData = tbmuser == null ? null : tbmuser.getPenyediaAsset();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenyediaAsset.class)
				.add(penyediaAssetData == null || penyediaAssetData.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("id", penyediaAssetData.getId()))
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		// Satu kotak pencarian mencakup Nama / Kode / NPWP sekaligus (lebih praktis bagi admin).
		String kataKunci = searchnama == null ? "" : searchnama.getValue().trim();
		criteria.add(kataKunci.isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.disjunction()
						.add(Restrictions.ilike("nama", kataKunci, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kode", kataKunci, MatchMode.ANYWHERE))
						.add(Restrictions.ilike("npwp", kataKunci, MatchMode.ANYWHERE)));

		// Filter dropdown Kategori / Jenis / Status (diabaikan bila "— Semua —" terpilih / null).
		Object katVal = (searchKategori == null || searchKategori.getSelectedItem() == null) ? null
				: searchKategori.getSelectedItem().getValue();
		if (katVal != null) {
			criteria.add(Restrictions.eq("kategoriPenyediaAsset", katVal));
		}
		Object jenVal = (searchJenis == null || searchJenis.getSelectedItem() == null) ? null
				: searchJenis.getSelectedItem().getValue();
		if (jenVal != null) {
			criteria.add(Restrictions.eq("jenisPenyediaAsset", jenVal));
		}
		Object stsVal = (searchStatus == null || searchStatus.getSelectedItem() == null) ? null
				: searchStatus.getSelectedItem().getValue();
		if (stsVal != null) {
			criteria.add(Restrictions.eq("statusPenyediaAsset", stsVal));
		}

		// Filter "hanya yang belum lengkap": minimal satu dari 6 berkas kunci masih kosong.
		if (searchBelumLengkap != null && searchBelumLengkap.isChecked()) {
			criteria.add(Restrictions.disjunction()
					.add(Restrictions.or(Restrictions.isNull("npwp"), Restrictions.eq("npwp", "")))
					.add(Restrictions.or(Restrictions.isNull("noAktaPendirian"), Restrictions.eq("noAktaPendirian", "")))
					.add(Restrictions.or(Restrictions.isNull("bank"), Restrictions.eq("bank", "")))
					.add(Restrictions.or(Restrictions.isNull("email"), Restrictions.eq("email", "")))
					.add(Restrictions.or(Restrictions.isNull("telp"), Restrictions.eq("telp", "")))
					.add(Restrictions.or(Restrictions.isNull("alamat"), Restrictions.eq("alamat", ""))));
		}

		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code DataSearchDefault} untuk
	 * melakukan pencarian ulang dan memuat ulang data penyedia ke dalam grid,
	 * sesuai dengan filter yang aktif di komponen UI pencarian. Dipanggil saat
	 * halaman pertama dimuat, saat pengguna mengubah filter, dan setelah operasi
	 * simpan/hapus.<br><br>
	 *
	 * <b>Cara kerja:</b> Memeriksa null-safety pada {@code searchnama}; jika null
	 * maka halaman ini tidak dalam konteks daftar (misalnya mode inline) sehingga
	 * metode langsung return. Memanggil {@code Common.initPaging} dengan criteria
	 * tanpa order untuk menghitung total data. Kemudian mengambil daftar penyedia
	 * dengan limit {@code Common.ROWS_COUNT_ON_PAGE} dan offset berdasarkan
	 * halaman aktif paging. Membuat {@code SimpleListModel} dan mengatur renderer
	 * {@code PenyediaAssetRenderer} ke grid.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param event Event ZK yang memicu pencarian; boleh null (saat dipanggil
	 *              programatis dari kode lain seperti {@code doAfterCompose}).<br><br>
	 *
	 * <b>Return:</b> void<br><br>
	 *
	 * <b>Pemeliharaan:</b> Ukuran halaman dikontrol oleh konstanta global
	 * {@code Common.ROWS_COUNT_ON_PAGE}. Ubah nilai ini di Common jika perlu
	 * menyesuaikan jumlah baris per halaman.
	 *
	 * @param event event pemicu pencarian (boleh null)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		// Tab "Penyedia" hanya menampilkan VENDOR RESMI (sudah disetujui). Pendaftar via web
		// yang belum disahkan (disetujuiOleh == null) tampil di tab "Calon Vendor", bukan di sini.
		Common.initPaging(initCriteria(false).add(Restrictions.isNotNull("disetujuiOleh")), paging);

		List<PenyediaAsset> penyediaAsset = initCriteria(true).add(Restrictions.isNotNull("disetujuiOleh"))
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penyediaAsset);
		grid.setRowRenderer(new PenyediaAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code FormSop} untuk membangun
	 * form lengkap (delapan tab) penyedia asset yang dapat digunakan dalam alur
	 * SOP/disposisi maupun mode edit standar. Form ini merupakan komponen UI
	 * utama yang digunakan oleh semua konteks tampilan form penyedia.<br><br>
	 *
	 * <b>Cara kerja:</b> Menginisialisasi Label propinsi dan kota sebagai field
	 * instance, menyimpan referensi penyediaAsset dan disposisiSop. Jika
	 * {@code rowsData} null (pertama kali), membuat {@code MyGrid} baru dengan
	 * satu kolom; jika sudah ada, membersihkan isinya. Membuat satu baris form
	 * berisi {@code Tabbox} dengan delapan tab dan panel yang dibangun oleh
	 * metode-metode privat: mainData, initDokumen, dataRekening, dataAkta,
	 * dataAktaTerakhir, dataNpwp, dataPakta, dan dataProduk. Mengembalikan
	 * {@code MyGrid} yang berisi tabbox tersebut.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject objek penyedia yang akan diedit; di-cast ke
	 *                           {@code PenyediaAsset}
	 * @param disposisiSop       konteks disposisi SOP jika form digunakan dalam
	 *                           alur persetujuan; null untuk mode normal
	 * @param save               tombol simpan yang disediakan oleh pemanggil;
	 *                           tidak digunakan langsung oleh metode ini
	 * @param setujui            tombol setuju yang disediakan oleh pemanggil;
	 *                           tidak digunakan langsung oleh metode ini<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code MyGrid} yang berisi tabbox dengan semua tab form penyedia;
	 *         siap untuk dimasukkan ke dalam container UI<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}. Error
	 * dari setiap metode pembangun tab akan melempar ke pemanggil.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Untuk menambah tab baru, tambahkan {@code Tab} di
	 * {@code Tabs} dan tambahkan panel baru di {@code Tabpanels} dengan memanggil
	 * metode builder yang sesuai.
	 *
	 * @param generalValueObject entitas penyedia
	 * @param disposisiSop       konteks SOP (boleh null)
	 * @param save               tombol simpan (tidak digunakan langsung)
	 * @param setujui            tombol setuju (tidak digunakan langsung)
	 * @return MyGrid berisi form penyedia berlapis tab
	 * @throws Exception jika terjadi kesalahan membangun form
	 */
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {
		propinsi = new Label();
		kota = new Label();
		this.penyediaAsset = (PenyediaAsset) generalValueObject;
		this.disposisiSop = disposisiSop;

		MyGrid gridForm = new MyGrid();

		if (rowsData == null) {

			gridForm.setWidth("100%");
			gridForm.setHeight("100%");
			gridForm.setStyle("min-height:200px;border:0px;background: transparent;");

			Columns columns = new Columns();
			columns.setParent(gridForm);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);

			rowsData = new Rows();
			rowsData.setParent(gridForm);

		} else {
			Common.clear(rowsData);
		}

		MyFormRow rowData = new MyFormRow();
		rowData.setValign("top");
		rowData.setParent(rowsData);

		final Tabbox tabbox = new Tabbox();
		// AKAR layar kosong di tab selain yang pertama: tiap tabpanel berisi grid ber-setHeight("100%").
		// Saat tabpanel BELUM terpilih ia ter-render display:none → tinggi grid dihitung 0, dan ZK TIDAK
		// menghitung ulang saat tab dibuka → konten "ada" tapi tinggi 0 = LAYAR KOSONG. Dua lapis fix:
		//  (1) beri Tabbox ukuran pasti (width/height) sebagai wadah,
		//  (2) saat tab dipilih, INVALIDATE panel terpilih agar grid di dalamnya dihitung ulang &
		//      tampil penuh. Lihat juga [[fix-tab-obe-perkuliahan-kosong]].
		tabbox.setWidth("100%");
		tabbox.setHeight("790px");
		tabbox.setStyle("overflow:auto;");
		tabbox.setParent(rowData);
		tabbox.addEventListener(org.zkoss.zk.ui.event.Events.ON_SELECT, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Tabpanel terpilih = tabbox.getSelectedPanel();
				if (terpilih != null) {
					terpilih.invalidate();
				}
			}
		});

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);
		Tab tab = new Tab("Identitas Perusahaan");
		tab.setParent(tabs);

		tab = new Tab("Dokumen Persyaratan");
		tab.setParent(tabs);

		tab = new Tab("Rekening Bank");
		tab.setParent(tabs);

		tab = new Tab("Akta Pendirian Perusahaan");
		tab.setParent(tabs);

		tab = new Tab("Akta Perubahan Terakhir Perusahaan");
		tab.setParent(tabs);

		tab = new Tab("NPWP");
		tab.setParent(tabs);

		tab = new Tab("Pakta Integritas");
		tab.setParent(tabs);

		tab = new Tab("Produk");
		tab.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		tabpanels.appendChild(mainData(penyediaAsset));

		tabpanels.appendChild(initDokumen(penyediaAsset));

		tabpanels.appendChild(dataRekening(penyediaAsset));

		tabpanels.appendChild(dataAkta(penyediaAsset));

		tabpanels.appendChild(dataAktaTerakhir(penyediaAsset));

		tabpanels.appendChild(dataNpwp(penyediaAsset));

		tabpanels.appendChild(dataPakta(penyediaAsset));

		tabpanels.appendChild(dataProduk(penyediaAsset));
		return gridForm;
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code FormSop} untuk menyediakan
	 * nama/istilah modul ini yang ditampilkan dalam alur SOP dan disposisi.<br><br>
	 *
	 * <b>Cara kerja:</b> Mengembalikan string literal yang merepresentasikan nama
	 * modul ini dalam konteks SOP: "Pengajuan Penyedia (Vendor)".<br><br>
	 *
	 * <b>Return:</b>
	 * @return string "Pengajuan Penyedia (Vendor)" yang digunakan sebagai label
	 *         modul dalam alur SOP/disposisi<br><br>
	 *
	 * <b>Pemeliharaan:</b> Jika nama modul perlu disesuaikan atau perlu dukungan
	 * multi-bahasa, ubah string yang dikembalikan di sini.
	 *
	 * @return istilah nama modul untuk alur SOP
	 * @throws Exception tidak dilempar dalam implementasi ini
	 */
	@Override
	public String istilah() throws Exception {
		return "Pengajuan Penyedia (Vendor)";
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code FormSop} untuk menyediakan
	 * referensi ke entitas {@code DataSop} yang sedang diproses dalam alur SOP.
	 * Digunakan oleh framework SOP untuk mendapatkan objek yang akan
	 * dikaitkan dengan disposisi.<br><br>
	 *
	 * <b>Cara kerja:</b> Mengembalikan field instance {@code penyediaAsset} yang
	 * sudah diset saat form diinisialisasi. {@code PenyediaAsset} mengimplementasikan
	 * {@code DataSop} sehingga dapat dikembalikan langsung.<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code PenyediaAsset} yang sedang aktif di form, atau null jika
	 *         form belum diinisialisasi<br><br>
	 *
	 * <b>Pemeliharaan:</b> Pastikan {@code PenyediaAsset} selalu mengimplementasikan
	 * {@code DataSop}; jika interface berubah, perbarui entitas tersebut.
	 *
	 * @return entitas penyedia aktif sebagai DataSop
	 * @throws Exception tidak dilempar dalam implementasi ini
	 */
	@Override
	public DataSop ambil() throws Exception {
		return penyediaAsset;
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code FormSop} untuk menyediakan
	 * kelas entitas yang dikelola oleh form ini, digunakan oleh framework SOP
	 * untuk keperluan refleksi dan mapping.<br><br>
	 *
	 * <b>Cara kerja:</b> Mengembalikan {@code PenyediaAsset.class} secara langsung.<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code Class} dari {@code PenyediaAsset}<br><br>
	 *
	 * <b>Pemeliharaan:</b> Tidak perlu diubah kecuali jika entitas utama
	 * modul ini berubah.
	 *
	 * @return kelas PenyediaAsset
	 * @throws Exception tidak dilempar dalam implementasi ini
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PenyediaAsset.class;
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code FormSop} untuk menetapkan
	 * mode persetujuan form. Dipanggil oleh framework SOP saat form digunakan
	 * dalam konteks persetujuan/review disposisi.<br><br>
	 *
	 * <b>Cara kerja:</b> Menetapkan nilai {@code persetujuan} ke field instance.
	 * Nilai true akan membuat semua field pada form menjadi read-only (Label)
	 * saat form dibangun ulang. Metode ini harus dipanggil sebelum {@code form()}
	 * agar mode persetujuan diterapkan dengan benar.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param persetujuan {@code true} untuk mode persetujuan/baca saja,
	 *                    {@code false} untuk mode edit penuh<br><br>
	 *
	 * <b>Pemeliharaan:</b> Perubahan mode persetujuan tidak otomatis me-refresh
	 * form yang sudah dirender; pemanggil harus memanggil ulang {@code form()}
	 * setelah memanggil metode ini.
	 *
	 * @param persetujuan flag mode persetujuan
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * <b>Tujuan:</b> Mengimplementasikan kontrak {@code DataInitDefault} untuk
	 * menghasilkan file cetak (PDF) profil perusahaan dari entitas penyedia yang
	 * diberikan. Digunakan oleh mekanisme ekspor data terpusat.<br><br>
	 *
	 * <b>Cara kerja:</b> Mendelegasikan seluruh proses cetak ke
	 * {@code CommonReportHelper.onCetakPenyediaAsset(penyediaAsset, false)}.
	 * Parameter kedua {@code false} menandakan bahwa laporan tidak perlu langsung
	 * diunduh (diserahkan ke pemanggil). Mengembalikan {@code File} yang berisi
	 * laporan PDF.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param generalValueObject entitas yang akan dicetak; di-cast ke
	 *                           {@code PenyediaAsset}<br><br>
	 *
	 * <b>Return:</b>
	 * @return {@code File} yang berisi laporan PDF profil perusahaan<br><br>
	 *
	 * <b>Penanganan error:</b> Dideklarasikan {@code throws Exception}; error
	 * JasperReports ditangani oleh {@code CommonReportHelper}.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Template laporan dikelola di dalam
	 * {@code CommonReportHelper}; tidak perlu mengubah metode ini jika hanya
	 * mengubah tampilan laporan.
	 *
	 * @param generalValueObject entitas penyedia
	 * @return file PDF profil perusahaan
	 * @throws Exception jika terjadi kesalahan cetak laporan
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PenyediaAsset penyediaAsset = (PenyediaAsset) generalValueObject;
		return CommonReportHelper.onCetakPenyediaAsset(penyediaAsset, false);
	}

	/**
	 * <b>Tujuan:</b> Menghasilkan kode perusahaan (nomor pengajuan penyedia)
	 * berdasarkan konfigurasi {@code NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA}.
	 * Kode bersifat unik dan mengikuti format nomor surat yang dikonfigurasi
	 * administrator sistem.<br><br>
	 *
	 * <b>Cara kerja:</b> Memeriksa apakah konfigurasi nomor surat pengajuan
	 * penyedia tersedia. Jika tidak, mengembalikan kode barcode acak sebagai
	 * fallback. Jika tersedia, menentukan indeks berikutnya: jika
	 * {@code gunakanIndexUrut} aktif, menggunakan {@code nomorIndex} langsung;
	 * jika tidak, memanggil {@code getindex(NomorSurat)} untuk menghitung jumlah
	 * record yang ada. Jika parameter {@code tambah} true, menambahkan indeks
	 * nomor surat dengan {@code NomorSurat.tambahIndexNomorSurat} (hanya dipanggil
	 * saat simpan definitif, bukan saat preview). Memformat kode menggunakan
	 * tanggal dari {@code tanggalPembuatan} dan memastikan keunikan melalui
	 * {@code KodeUnikUtil.pastikanUnik}.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param tambah {@code true} jika indeks nomor surat perlu di-increment
	 *               (dipanggil saat simpan); {@code false} jika hanya preview
	 *               kode tanpa mengubah counter (dipanggil saat inisialisasi
	 *               form untuk menampilkan kode default)<br><br>
	 *
	 * <b>Return:</b>
	 * @return String kode perusahaan yang unik dan terformat sesuai konfigurasi
	 *         nomor surat, atau barcode acak jika konfigurasi belum diatur<br><br>
	 *
	 * <b>Penanganan error:</b> Tidak ada penanganan eksplisit; jika format gagal
	 * exception melempar ke atas.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Pemanggilan {@code KodeUnikUtil.pastikanUnik} memastikan
	 * tidak ada duplikasi kode meskipun ada kondisi race (misalnya dua pengguna
	 * membuat penyedia baru secara bersamaan).
	 *
	 * @param tambah apakah counter nomor surat perlu di-increment
	 * @return kode perusahaan yang dihasilkan
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA == null
				|| NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA.getNomorSurat());

		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA.getNomorSurat());
		}

		String noAgenda = NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return ais.action.master.KodeUnikUtil.pastikanUnik(PenyediaAsset.class, noAgenda);
	}

	/**
	 * <b>Tujuan:</b> Menghitung indeks urutan berikutnya untuk generasi kode
	 * perusahaan berdasarkan aturan pengurutan yang dikonfigurasi pada entitas
	 * {@code NomorSurat}: bisa berdasarkan nomor surat tertentu, kelompok
	 * nomor surat, atau semua, dengan opsi reset per tahun, per bulan, atau
	 * per tanggal tertentu.<br><br>
	 *
	 * <b>Cara kerja:</b> Jika {@code nomorSurat} null, mengembalikan 0. Membuat
	 * Criteria ke {@code PenyediaAsset} dengan berbagai restriction bersyarat:
	 * (1) filter berdasarkan nomor surat atau kelompok nomor surat jika
	 * {@code urutBerdasarkanNomor} atau {@code urutBerdasarkanKelompok} aktif;
	 * (2) filter tahun jika {@code resetUrutanTiapTahun} aktif;
	 * (3) filter tahun+bulan jika {@code resetUrutanTiapBulan} aktif;
	 * (4) filter tanggal pembuatan &gt;= {@code resetTiap} jika tanggal reset
	 * sudah lewat. Menggunakan {@code Projections.rowCount()} untuk menghitung
	 * jumlah record, kemudian mengembalikan count+1 sebagai indeks berikutnya.<br><br>
	 *
	 * <b>Parameter:</b>
	 * @param nomorSurat konfigurasi nomor surat yang menentukan aturan pengurutan
	 *                   dan reset; jika null mengembalikan 0<br><br>
	 *
	 * <b>Return:</b>
	 * @return indeks urutan berikutnya (mulai dari 1); dijamin tidak null dan
	 *         minimal bernilai 1<br><br>
	 *
	 * <b>Penanganan error:</b> Jika query mengembalikan null, indeks diset ke 0
	 * sebelum di-increment menjadi 1.<br><br>
	 *
	 * <b>Pemeliharaan:</b> Metode ini menggunakan LEFT JOIN ke
	 * {@code nomorSuratAlurPengadaan} dan {@code nomorSurat}; pastikan relasi
	 * ini terdefinisi dengan benar di mapping Hibernate entitas
	 * {@code PenyediaAsset}.
	 *
	 * @param nomorSurat konfigurasi aturan penomoran
	 * @return indeks urutan berikutnya
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PenyediaAsset.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
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
}
