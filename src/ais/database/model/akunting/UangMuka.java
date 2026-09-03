package ais.database.model.akunting;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Tbmuser;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * <h2>UangMuka &mdash; dokumen pengajuan, pencairan, dan pelunasan uang muka (panjar/kasbon)</h2>
 *
 * <p>Entity ini adalah <b>dokumen induk siklus uang muka</b> pada modul Akunting AIS: sejumlah
 * dana yang dikeluarkan lembaga <i>di muka</i> kepada seorang pengguna (pegawai/staf/unit kerja)
 * untuk membiayai kegiatan yang belum terjadi, dengan kewajiban dipertanggungjawabkan kemudian.
 * Satu baris tabel <code>public.uang_muka</code> mewakili satu pengajuan &mdash; mulai dari saat
 * diketik oleh pemohon, disetujui pejabat, diantrekan untuk ditransfer, dijurnal ke buku besar,
 * sampai ditutup oleh laporan pertanggungjawaban dan (bila ada sisa) pengembalian kas.</p>
 *
 * <h3>Rantai hidup dokumen (terverifikasi dari kode, bukan dugaan)</h3>
 * <ol>
 *   <li><b>Pengajuan.</b> Layar <code>UangMukaAction</code> (menu <i>&quot;Pengajuan Uang
 *       Muka&quot;</i>, <code>/pages/master/akunting/uang_muka.zul</code>) membuat baris baru,
 *       mengisi {@link #getNama()}, {@link #getNilai()}, {@link #getMulai()}/{@link #getSampai()},
 *       sumber anggaran, dan menetapkan {@link #getKode()} dari alur Nomor Surat Keuangan
 *       ({@link #getNomorSuratAlurKeuangan()} default
 *       <code>NomorSuratAlurKeuangan.UANG_MUKA_DATA</code>). Pemohon direkam pada
 *       {@link #getDibuatOleh()}.</li>
 *   <li><b>Persetujuan.</b> Layar yang sama dijalankan dalam <i>mode persetujuan</i>
 *       (<code>PersetujuanUangMukaAction extends UangMukaAction</code>, konstruktor
 *       <code>super(true)</code>; menu <i>&quot;Persetujuan Uang Muka&quot;</i>). Pejabat memilih
 *       radio Status; bila &quot;Disetujui&quot; maka {@link #setDisetujuiOleh(Tbmuser)} diisi
 *       pengguna sesi dan {@link #setTanggalPersetujuan(Date)} diisi dari datebox
 *       {@link #getTanggalPersetujuanManual()}. Alternatifnya persetujuan datang dari alur SOP
 *       ({@link #getDisposisiSop()}), dan seluruh getter status di kelas ini akan
 *       <i>menurunkan</i> penyetuju/tanggal/status dari disposisi tersebut.</li>
 *   <li><b>Pencairan.</b> Begitu status menjadi &quot;Disetujui&quot;,
 *       <code>DaftarPengajuanTransfer.simpanUangMuka(this)</code> mengantrekan dokumen ke menu
 *       Pembayaran/Transfer; tautannya disimpan di {@link #getDaftarPengajuanTransfer()}. Dari
 *       antrean itulah {@link #getTanggalTransaksi()} mengambil tanggal realisasi kas.</li>
 *   <li><b>Penjurnalan.</b> <code>PostingUangMukaAction</code> (menu <i>&quot;Jurnal Uang
 *       Muka&quot;</i>) menyaring dokumen dengan <code>postingHistory IS NULL</code>, membuat satu
 *       {@link PostingHistory} berjenis <code>JENIS_PERSETUJUAN_UANG_MUKA</code>, lalu menerbitkan
 *       pasangan <code>GrupTransaksi</code>/<code>Transaksi</code> (jurnal) yang menunjuk balik ke
 *       dokumen ini lewat kolom <code>grup_transaksi.uang_muka</code>. Batch-nya dicap pada
 *       {@link #getPostingHistory()}; membatalkan posting mengosongkan kolom itu kembali sehingga
 *       dokumen kembali masuk antrean.</li>
 *   <li><b>Pertanggungjawaban.</b> <code>Pertangungjawaban</code> (menu <i>&quot;Pertanggungjawaban
 *       Uang Muka&quot;</i>) mencatat realisasi belanja, pajak, dan sisa yang dikembalikan.</li>
 *   <li><b>Pengembalian sisa.</b> <code>PostingPertangungjawabanPengembalianAction</code> (menu
 *       <i>&quot;Pengembalian Sisa Uang Muka&quot;</i>) menjurnal setoran sisa dana, dengan akun
 *       diambil dari {@link JenisUangMuka} milik uang muka ini: <code>getAkun()</code> sebagai
 *       akun uang muka (piutang) dan <code>getAkunKelebihan()</code> sebagai lawannya.</li>
 * </ol>
 *
 * <h3>Relasi terverifikasi</h3>
 * <ul>
 *   <li>{@link JenisUangMuka} &mdash; katalog <i>Jenis/Akun Uang Muka</i> per {@link SatuanKerja}.
 *       Bukan sekadar label: dari sinilah mesin posting mengambil akun buku besar. Bila kosong,
 *       {@link #getJenisUangMuka()} mencoba <code>JenisUangMuka.ambilDefault(satuanKerja)</code>.</li>
 *   <li>{@code Pertangungjawaban} &mdash; <b>relasi dua arah dengan DUA kolom FK</b>: entity ini
 *       menyimpan <code>uang_muka.pertangungjawaban</code>, sedangkan <code>Pertangungjawaban</code>
 *       menyimpan <code>pertangungjawaban.uang_muka</code> (<code>nullable=false</code>). Tidak ada
 *       <code>mappedBy</code>, jadi keduanya kolom independen yang bisa saling tidak konsisten.</li>
 *   <li>{@code GrupTransaksi} &mdash; jurnal; menunjuk ke sini lewat kolom <code>uang_muka</code>
 *       dan memakai kunci idempotensi <code>ambilUnik()</code> berbentuk
 *       <code>ais.database.model.akunting.UangMuka_&lt;id&gt;</code>.</li>
 *   <li>{@link PostingHistory} &mdash; kepala batch posting; dibagi bersama oleh dokumen sumber dan
 *       jurnal yang dihasilkan.</li>
 *   <li>{@link Workspace} (mata anggaran/kegiatan RAB) dan {@link SatuanKerja} &mdash; sumber dana
 *       dan unit pemilik. <code>PenggunaanAnggaran</code> memotong pagu anggaran atas nama dokumen
 *       ini (ref <code>&lt;id&gt;_UANG_MUKA</code>).</li>
 *   <li>{@link DanaTalangan} &mdash; dana talangan yang menutup sementara uang muka ini; bila dana
 *       talangan sudah disetujui, jenis uang muka <i>diambil alih</i> dari sana.</li>
 *   <li>{@link DisposisiSop} &mdash; jejak alur persetujuan SOP; menjadi sumber kebenaran status
 *       bila diisi.</li>
 *   <li>{@code DaftarPengajuanTransfer} &mdash; antrean pembayaran/pencairan.</li>
 *   <li>{@link PenerimaanPengadaanMasterAsset} &mdash; BAST penerimaan barang, untuk uang muka yang
 *       lahir dari Permintaan Pengadaan (PR).</li>
 * </ul>
 *
 * <h3>Tiga mode pengajuan</h3>
 * <ol>
 *   <li><b>Berbasis anggaran</b> (bawaan): {@link #getWorkspace()} menunjuk satu mata anggaran;
 *       {@link #getAkun()} diturunkan dari akun mata anggaran tersebut.</li>
 *   <li><b>Tanpa anggaran</b> ({@link #getTanpaAnggaran()} <code>true</code>): kolom workspace
 *       dipaksa <code>null</code> setiap kali dibaca.</li>
 *   <li><b>Dari PR</b> ({@link #getAmbilDariPr()} <code>true</code>): baris Permintaan Pengadaan
 *       dicatat sebagai <i>daftar id dipisah koma</i> pada {@link #getPermintaanPengadaanMasterAssets()}
 *       dan mata anggarannya pada {@link #getAngarans()}. Anggaran tidak dipilih langsung &mdash;
 *       {@link #getWorkspace()} merekonstruksinya dari string tersebut lewat
 *       {@link #ambilAngarans()}.</li>
 * </ol>
 *
 * <h3>Arti angka: <code>nilai</code> vs <code>saldo</code> (mudah disalahpahami)</h3>
 * <p>{@link #getNilai()} adalah <b>nominal uang muka yang diajukan</b>. {@link #getSaldo()}
 * <b>bukan</b> sisa uang muka yang belum dipertanggungjawabkan, melainkan <b>potret sisa pagu
 * anggaran</b> (<code>JenisUangMukaAction.hitungSaldo(...)</code> = pagu <code>workspace</code>
 * dikurangi seluruh <code>PenggunaanAnggaran</code>) pada saat dokumen disimpan/ditampilkan. Kolom
 * layar mengurutkannya sebagai <i>Saldo</i>, <i>Nilai</i>, <i>Sisa</i>, dengan <i>Sisa</i> dihitung
 * di layar sebagai <code>saldo - nilai</code>. Salah membaca kolom ini sebagai &quot;sisa panjar&quot;
 * akan menghasilkan kesimpulan keuangan yang keliru.</p>
 *
 * <h3>Pewarisan: <code>UangMuka extends DataSop extends GeneralValueObject</code></h3>
 * <p>{@link DataSop} hanya mewajibkan pasangan {@link #getDisposisiSop()}/{@link
 * #setDisposisiSop(DisposisiSop)} agar dokumen dapat dipasang pada mesin alur SOP.
 * {@link ais.database.model.GeneralValueObject} sendiri <b>bukan</b> <code>@Entity</code> maupun
 * <code>@MappedSuperclass</code> &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * satu pun properti induk. Karena itu field jejak audit (<code>oleh</code>, <code>olehId</code>,
 * <code>tanggal_dirubah</code>) <b>wajib</b> dideklarasikan ulang di sini; pengulangan itu
 * keharusan teknis, bukan duplikasi yang perlu &quot;dirapikan&quot;. Yang diwarisi dan tetap
 * berguna adalah utilitas <code>check(T)</code> (menormalkan proxy Hibernate/objek id-null menjadi
 * instance hidup atau <code>null</code>).</p>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Getter di kelas ini banyak yang MENULIS.</b> {@link #getAktif()}, {@link #getStatus()},
 *       {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()}, {@link #getWorkspace()},
 *       {@link #getAkun()}, {@link #getSatuanKerja()}, {@link #getMulai()}, {@link #getSelesai()},
 *       {@link #getSampai()}, {@link #getKodeUnik()}, {@link #getJenisUangMuka()},
 *       {@link #getTahun()}, {@link #getBulan()}, {@link #getTanggalTransaksi()},
 *       {@link #getAngarans()}, {@link #getPermintaanPengadaanMasterAssets()}, dan
 *       {@link #getPertangungjawaban()} semuanya menimpa field instance. Bila objek berada dalam
 *       Session Hibernate yang hidup, pembacaan biasa (render grid, cetak laporan, ekspor) dapat
 *       berubah menjadi <code>UPDATE</code> saat <i>dirty checking</i> berjalan. Jangan menambah
 *       pemanggilan getter ini di jalur baca-saja tanpa mempertimbangkan efeknya.</li>
 *   <li><b>{@link #getSelesai()} tidak pernah membaca nilai tersimpan.</b> Ia selalu menghitung
 *       ulang <code>getSampai() + N hari</code> dengan N dari konfigurasi
 *       <code>tgl_laporan_pengajuan_uang_muka</code> (default 14). Mengubah konfigurasi itu
 *       menggeser tenggat <i>seluruh</i> dokumen lama secara retroaktif. Lihat catatan pada method
 *       tersebut mengenai penegakan tenggat yang tidak ada.</li>
 *   <li><b>Status adalah nilai turunan, bukan kolom bebas.</b> {@link #getStatus()} memaksa
 *       &quot;Disetujui&quot; setiap kali {@link #getDisetujuiOleh()} tidak null, dan memaksa
 *       &quot;Ditolak&quot; bila langkah akhir SOP adalah langkah penolakan. Menulis
 *       <code>setStatus("Pengajuan")</code> pada dokumen yang penyetujunya masih terisi tidak akan
 *       bertahan sampai pembacaan berikutnya.</li>
 *   <li><b>Kolom <code>kode</code> tidak unik.</b> Yang diberi <code>unique = true</code> adalah
 *       {@link #getKodeUnik()}, yaitu <code>kode</code> digabung id dokumen/disposisi. Penomoran di
 *       layar memakai <i>hitung baris</i> (<code>rowCount()+1</code>), bukan nomor terbit
 *       tertinggi, sehingga kode dapat berulang setelah penghapusan data.</li>
 *   <li><b>Semua perubahan terekam Envers</b> (<code>@Audited</code>) ke tabel
 *       <code>uang_muka_aud</code>. Versi lama nominal/penyetuju tetap tersimpan walau dokumen
 *       diperbaiki; ini bagus untuk audit, tetapi berarti koreksi tidak pernah benar-benar
 *       menghapus jejak angka yang salah.</li>
 * </ul>
 *
 * <h3>Catatan pengendalian internal (hasil telaah kode, bukan klaim kebijakan)</h3>
 * <p>Pemisahan tugas pemohon vs penyetuju <b>tidak ditegakkan di kode</b>. Titik tulis persetujuan
 * satu-satunya di layar ZK adalah <code>UangMukaAction.onSave()</code>, yang menetapkan
 * <code>setDisetujuiOleh(pengguna sesi)</code> tanpa memeriksa peran, atasan, batas nominal,
 * maupun apakah penyetuju sama dengan {@link #getDibuatOleh()}. Konstanta hak
 * <code>CommonPrivilages.APPROVE</code>/<code>REJECT</code> ada di repo tetapi tidak pernah dipakai
 * pada jalur ini; yang berlaku hanya hak <code>UPDATE</code> atas menu yang sedang aktif. Selain
 * itu <code>UangMukaAction.doAfterCompose</code> membaca parameter URL
 * <code>?persetujuan=true</code> dan menjadikan halaman <i>Pengajuan</i> berperilaku sebagai
 * halaman <i>Persetujuan</i>, sehingga pemisahan dua menu tersebut tidak menjadi batas keamanan.
 * Kanal REST <code>UangMukaApiHelper</code> lebih ketat (hak granular
 * <code>create/update/delete/approve/reject</code> serta larangan menghapus dokumen yang sudah
 * disetujui/dijurnal/ber-LPJ), namun tetap mengambil dokumen murni berdasarkan id tanpa memeriksa
 * satuan kerja pemiliknya. Rincian per-method ada pada Javadoc masing-masing di bawah.</p>
 *
 * <p>Pada layar jurnal (<code>PostingUangMukaAction</code>) pola serupa berulang: aksi
 * &quot;Posting Semua&quot; dan &quot;Batalkan Posting Semua&quot; dipicu langsung dari tombol ZUL
 * yang tidak terikat pemeriksaan hak apa pun, dan method penanganannya tidak memeriksa ulang hak di
 * sisi server &mdash; sehingga pembatalan massal (aksi paling destruktif) justru bergerbang lebih
 * longgar daripada tombol batal per baris yang menuntut hak <code>UPDATE</code> plus status admin.
 * Penyaringan unit kerja pada seluruh kueri daftar posting bersifat aman-terbuka (baris dengan
 * <code>satuan_kerja</code> kosong selalu ikut, dan daftar unit kosong berubah menjadi kondisi
 * selalu-benar), sementara varian statis kueri tersebut &mdash; yang dipakai dasbor/API &mdash;
 * tidak menyaring unit kerja sama sekali. Konsekuensi paling berat ada pada asimetri pembatalan
 * posting yang dijelaskan pada {@link #getPostingHistory()}.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Jejak audit &amp; identitas:</b> {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}, {@link #toString()}.</li>
 *   <li><b>Identitas dokumen:</b> {@link #getKode()}, {@link #getKodeUnik()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #getNomorSuratAlurKeuangan()}, {@link #getTahun()},
 *       {@link #getBulan()}.</li>
 *   <li><b>Nominal &amp; anggaran:</b> {@link #getNilai()}, {@link #getSaldo()},
 *       {@link #getWorkspace()}, {@link #getAkun()}, {@link #getSatuanKerja()},
 *       {@link #getTanpaAnggaran()}, {@link #getAmbilDariPr()}, {@link #getAngarans()},
 *       {@link #ambilAngarans()}, {@link #getPermintaanPengadaanMasterAssets()}.</li>
 *   <li><b>Tanggal:</b> {@link #getMulai()}, {@link #getSampai()}, {@link #getSelesai()},
 *       {@link #getTanggalPembuatan()}, {@link #getTanggalPersetujuan()},
 *       {@link #getTanggalPersetujuanManual()}, {@link #getTanggalTransaksi()}.</li>
 *   <li><b>Persetujuan &amp; status:</b> {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 *       {@link #getStatus()}, {@link #getAktif()}, {@link #getDisposisiSop()}, konstanta
 *       {@link #PENGAJUAN}/{@link #DISETUJU}/{@link #DITOLAK}.</li>
 *   <li><b>Tautan hilir:</b> {@link #getPertangungjawaban()}, {@link #getPostingHistory()},
 *       {@link #getDaftarPengajuanTransfer()}, {@link #getDanaTalangan()},
 *       {@link #getJenisUangMuka()}, {@link #getPenerimaanPengadaanMasterAsset()}.</li>
 * </ol>
 *
 * @see ais.database.model.GeneralValueObject
 * @see DataSop
 * @see JenisUangMuka
 * @see DisposisiSop
 * @see PostingHistory
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "uang_muka")
public class UangMuka extends DataSop {

	/**
	 * Versi serialisasi Java. Nilainya <code>2463821577548439808L</code> &mdash; sama persis dengan
	 * puluhan entity lain di repo ini (mis. {@link JenisUangMuka} dan {@link DisposisiSop}) karena
	 * seluruh berkas awalnya lahir dari satu templat hbm2java yang sama. Jangan mengubahnya: nilai
	 * ini ikut menentukan kompatibilitas objek yang tersimpan di sesi ZK yang diserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer tabel; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyentuh baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyentuh baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id (username) pengguna yang terakhir menulis baris ini.
	 *
	 * @return id pengguna, atau <code>null</code> bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penulis terakhir.
	 *
	 * <p><b>Kuirk penting:</b> setter ini <i>menolak diam-diam</i> nilai <code>null</code> maupun
	 * string kosong &mdash; ia langsung <code>return</code> tanpa mengubah apa pun. Akibatnya nilai
	 * lama tidak pernah bisa dikosongkan lewat setter ini; jejak audit hanya bisa maju, tidak bisa
	 * dihapus. Perilaku ini disengaja agar rutin penyalinan objek yang mengoper field kosong tidak
	 * menghapus jejak yang sudah ada.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila <code>null</code> atau hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penulis terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai kosong/null diabaikan diam-diam sehingga
	 * jejak lama tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila <code>null</code> atau hanya spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir menulis baris ini.
	 *
	 * @return nama pengguna, atau <code>null</code> bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA (<code>@PreUpdate</code>) plus deklarasi field <code>tanggal_dirubah</code>
	 * &mdash; keduanya sengaja ditulis pada satu baris fisik oleh perkakas penambal audit repo ini.
	 *
	 * <p><b>Tujuan.</b> Sesaat sebelum Hibernate menerbitkan <code>UPDATE</code> untuk baris ini,
	 * <code>AuditTimestampInterceptor.ubah(this)</code> dipanggil untuk menstempel siapa dan kapan
	 * (mengisi {@link #setOleh(String)}/{@link #setOlehId(String)}/{@link #setTanggal_dirubah(Date)}
	 * dari konteks pengguna aktif bila tersedia).</p>
	 *
	 * <p><b>Efek samping.</b> Karena banyak getter di kelas ini menulis balik ke field (lihat
	 * Javadoc kelas), stempel waktu ini bisa berubah walau tidak ada perubahan bisnis yang nyata
	 * &mdash; membuka daftar uang muka saja sudah cukup untuk menggeser <code>tanggal_dirubah</code>
	 * pada sebagian baris.</p>
	 *
	 * <p><b>Kapan dipanggil.</b> Hanya oleh penyedia JPA/Hibernate; tidak pernah dipanggil kode
	 * aplikasi secara langsung.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh <code>null</code>
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Diinisialisasi ke waktu server saat objek dibuat, lalu diperbarui oleh {@link #onUpdate()}
	 * pada setiap <code>UPDATE</code>.</p>
	 *
	 * @return waktu perubahan terakhir (tidak pernah <code>null</code> untuk objek baru)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Status awal dokumen: <b>&quot;Pengajuan&quot;</b> &mdash; sudah diketik pemohon, belum
	 * disetujui siapa pun. Nilai ini juga menjadi hasil bawaan {@link #getStatus()} bila kolom
	 * status kosong. Ditulis sebagai string (bukan enum) karena dipakai langsung sebagai
	 * <code>value</code> radio ZK dan sebagai penyaring <code>Restrictions.eq("status", ...)</code>.
	 */
	public static final String PENGAJUAN = "Pengajuan";
	/**
	 * Status <b>&quot;Disetujui&quot;</b> &mdash; gerbang yang membuka seluruh proses hilir:
	 * pengantrean ke <code>DaftarPengajuanTransfer</code> (pencairan) dan kelayakan untuk dijurnal.
	 *
	 * <p><b>Perhatikan ejaan nama konstanta:</b> <code>DISETUJU</code> (tanpa &quot;i&quot;),
	 * sedangkan isinya <code>&quot;Disetujui&quot;</code>. Konstanta senama dan bernilai sama juga
	 * ada di <code>Pertangungjawaban</code>, <code>DanaTalangan</code>, <code>KasBesar</code>, dan
	 * kawan-kawan; sebagian kode akunting membandingkan status uang muka memakai konstanta milik
	 * kelas lain (mis. <code>DanaTalangan.DISETUJU</code>). Karena nilai stringnya identik,
	 * perilakunya benar &mdash; tetapi rujukan silang itu rapuh: mengubah teks salah satu konstanta
	 * akan memutus perbandingan di modul lain tanpa error kompilasi.</p>
	 */
	public static final String DISETUJU = "Disetujui";
	/**
	 * Status <b>&quot;Ditolak&quot;</b>. Selain dipilih manual, status ini juga <i>diturunkan</i>
	 * oleh {@link #getStatus()} ketika langkah akhir alur SOP ditandai sebagai langkah penolakan.
	 * {@link #setStatus(String)} memperlakukan nilai ini secara khusus dengan mengosongkan
	 * penyetuju dan tanggal persetujuan.
	 */
	public static final String DITOLAK = "Ditolak";

	/**
	 * Representasi teks dokumen, dipakai combobox/banbox ZK dan pesan log.
	 *
	 * <p>Formatnya <code>&lt;id&gt;-&lt;nama&gt;</code> dan sengaja membaca field <code>nama</code>
	 * secara langsung (bukan lewat {@link #getNama()}) sehingga aman dipanggil pada objek yang belum
	 * ter-<i>initialize</i> penuh. Untuk dokumen baru yang belum tersimpan, <code>id</code> masih
	 * <code>null</code> sehingga hasilnya berbentuk <code>&quot;null-...&quot;</code>.</p>
	 *
	 * @return teks ringkas dokumen
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nomor dokumen; lihat {@link #getKode()}. */
	private String kode;
	/** Judul/uraian singkat uang muka; lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Mata anggaran/kegiatan sumber dana; lihat {@link #getWorkspace()}. */
	private Workspace workspace;
	/** Akun buku besar sumber dana; lihat {@link #getAkun()}. */
	private Akun akun;
//	private JenisPembayaran jenisPembayaran;
	/** Nominal uang muka yang diajukan; lihat {@link #getNilai()}. */
	private Double nilai;
	/** Potret sisa pagu anggaran saat dokumen disimpan; lihat {@link #getSaldo()}. */
	private Double saldo;
	/** Penanda aktif/tidak; lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Awal periode penggunaan dana; lihat {@link #getMulai()}. */
	private Date mulai;
	/** Akhir periode penggunaan dana; lihat {@link #getSampai()}. */
	private Date sampai;
	/** Tenggat pelaporan pertanggungjawaban (terhitung); lihat {@link #getSelesai()}. */
	private Date selesai;
	/** Pemohon/pembuat dokumen; lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Penyetuju dokumen; lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Waktu persetujuan efektif; lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Waktu dokumen dibuat; lihat {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Status dokumen; lihat {@link #getStatus()}. */
	private String status;
	/** Dana talangan penutup sementara; lihat {@link #getDanaTalangan()}. */
	private DanaTalangan danaTalangan;
	/** Laporan pertanggungjawaban penutup; lihat {@link #getPertangungjawaban()}. */
	private Pertangungjawaban pertangungjawaban;
	/** Unit kerja pemilik dokumen; lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Jejak alur persetujuan SOP; lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;

	/** Jenis/akun uang muka; lihat {@link #getJenisUangMuka()}. */
	private JenisUangMuka jenisUangMuka;

	/** Batch posting jurnal; lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;
	/** Tahun buku dokumen; lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Alur penomoran surat keuangan; lihat {@link #getNomorSuratAlurKeuangan()}. */
	private NomorSuratAlurKeuangan nomorSuratAlurKeuangan;
	/** Bulan buku dokumen; lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Antrean pembayaran/pencairan; lihat {@link #getDaftarPengajuanTransfer()}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;
	/** Penanda pengajuan di luar pagu anggaran; lihat {@link #getTanpaAnggaran()}. */
	private Boolean tanpaAnggaran;
	/** Penanda pengajuan berbasis Permintaan Pengadaan; lihat {@link #getAmbilDariPr()}. */
	private Boolean ambilDariPr;
	/** Daftar id baris PR (CSV); lihat {@link #getPermintaanPengadaanMasterAssets()}. */
	private String permintaanPengadaanMasterAssets;
	/** Daftar id mata anggaran milik PR (CSV); lihat {@link #getAngarans()}. */
	private String angarans;
	/** Tanggal realisasi kas (terhitung); lihat {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA dan dipakai layar ZK saat menekan
	 * tombol &quot;Tambah&quot;.
	 *
	 * <p>Semua field dibiarkan <code>null</code>; nilai bawaan yang masuk akal disediakan oleh
	 * getter masing-masing (mis. {@link #getStatus()} mengembalikan {@link #PENGAJUAN},
	 * {@link #getTahun()}/{@link #getBulan()} mengambil periode berjalan, {@link #getMulai()}
	 * mengambil tanggal hari ini). Dengan begitu objek baru langsung dapat dirender di formulir
	 * tanpa inisialisasi tambahan.</p>
	 */
	public UangMuka() {
	}

	/**
	 * Kunci primer dokumen (kolom <code>id</code>, IDENTITY).
	 *
	 * <p>Bernilai <code>null</code> sampai baris benar-benar tersimpan. Nilai id ikut membentuk
	 * {@link #getKodeUnik()} dan kunci idempotensi jurnal
	 * (<code>GrupTransaksi.ambilUnik()</code>), sehingga id yang berubah berarti dokumen dianggap
	 * dokumen lain oleh mesin posting.</p>
	 *
	 * @return id dokumen, atau <code>null</code> untuk dokumen yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer. Hanya dipakai Hibernate dan rutin penyalinan/impor; kode bisnis
	 * tidak boleh memanggilnya pada dokumen yang sudah tersimpan.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nomor dokumen uang muka yang tampil di layar dan laporan.
	 *
	 * <p><b>Cara pembentukan.</b> Diisi oleh layar dari alur Nomor Surat Keuangan
	 * (<code>NomorSuratAlurKeuangan.UANG_MUKA_DATA</code>); bila alur belum dikonfigurasi, jalur
	 * REST menggantinya dengan barcode acak.</p>
	 *
	 * <p><b>Kasus tepi.</b> Getter menormalkan hasil: string kosong atau hanya spasi dikembalikan
	 * sebagai <code>null</code>, selain itu di-<code>trim()</code>. Jadi <code>null</code> di sini
	 * berarti &quot;belum bernomor&quot;, dan kode pemanggil tidak perlu memeriksa string kosong
	 * secara terpisah.</p>
	 *
	 * <p><b>Integritas.</b> Kolom ini <b>tidak</b> berindeks unik. Penomoran di layar memakai
	 * jumlah baris + 1, bukan nomor terbit tertinggi, sehingga penghapusan dokumen membuat nomor
	 * berputar ulang; pengaman keunikan yang benar-benar ditegakkan basis data hanya
	 * {@link #getKodeUnik()}.</p>
	 *
	 * @return nomor dokumen yang sudah di-trim, atau <code>null</code> bila belum/kosong
	 */
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menyetel nomor dokumen apa adanya (tanpa normalisasi &mdash; normalisasi terjadi di getter).
	 *
	 * @param kode nomor dokumen
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Judul/uraian singkat uang muka, mis. nama kegiatan yang dibiayai. Wajib terisi
	 * (<code>nullable = false</code>) dan divalidasi layar sebelum simpan.
	 *
	 * <p>Bila pemohon mengosongkannya, layar mengisinya otomatis dari nama mata anggaran yang
	 * dipilih. Nilai dikembalikan sudah di-<code>trim()</code>; <code>null</code> tetap
	 * <code>null</code>.</p>
	 *
	 * <p>Nilai ini juga dipakai sebagai bahan nama baris antrean transfer
	 * (<code>&quot;Pembayaran uang muka &lt;nama&gt; &lt;nama anggaran&gt;&quot;</code>) dan
	 * sebagai nama bawaan laporan pertanggungjawaban yang menutup dokumen ini.</p>
	 *
	 * @return judul dokumen yang sudah di-trim, atau <code>null</code>
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul/uraian singkat uang muka.
	 *
	 * @param nama judul dokumen
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas dari pemohon (peruntukan rinci, catatan pejabat, dsb.).
	 *
	 * <p>Dikembalikan apa adanya tanpa <code>trim()</code> maupun penyaringan HTML. Nilai ini ikut
	 * disalin ke keterangan <code>PenggunaanAnggaran</code> dan muncul di laporan cetak, jadi
	 * perlakukan sebagai teks yang berasal dari pengguna saat ditampilkan pada kanal non-ZK.</p>
	 *
	 * @return keterangan, atau <code>null</code>
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Apakah dokumen dianggap aktif (ikut diperhitungkan proses hilir dan pemotongan anggaran).
	 *
	 * <p><b>Bukan getter sederhana.</b> Method ini <i>menghitung dan menulis balik</i> field
	 * {@link #aktif} dan {@link #disposisiSop} berdasarkan tiga aturan berurutan:</p>
	 * <ol>
	 *   <li>Bila {@link #getStatus()} sudah &quot;Disetujui&quot;, dokumen dipaksa aktif. Perhatikan
	 *       bahwa {@link #getStatus()} sendiri getter berefek samping, sehingga satu pemanggilan
	 *       {@code getAktif()} merambat ke {@link #getDisetujuiOleh()} dan
	 *       {@link #getTanggalPersetujuan()}.</li>
	 *   <li>Bila jejak SOP ada dan disposisinya sudah tidak aktif, dokumen dimatikan.</li>
	 *   <li>Bila langkah akhir SOP adalah langkah penolakan
	 *       (<code>alurSop.getPenolakanAdaDiSini()</code>), dokumen dimatikan.</li>
	 * </ol>
	 *
	 * <p><b>Bawaan aman-terbuka.</b> Bila seluruh aturan tidak berlaku dan kolom masih
	 * <code>null</code>, hasilnya <code>true</code> &mdash; dokumen lama tanpa nilai eksplisit
	 * dianggap aktif. <code>PenggunaanAnggaran</code> memakai nilai ini (dikombinasikan dengan
	 * status bukan &quot;Ditolak&quot;) untuk memutuskan apakah pagu anggaran tetap terpotong.</p>
	 *
	 * <p><b>Kasus tepi.</b> Rantai <code>disposisiEnd.getAlurSop()</code> diakses berlapis dengan
	 * penjagaan null di setiap tingkat, tetapi tidak dibungkus try-catch: bila proxy disposisi
	 * terikat Session yang sudah tertutup, pemanggilan dapat melempar
	 * <code>LazyInitializationException</code> (berbeda dari {@link #getTanggalPersetujuan()} yang
	 * sengaja menelan kasus itu).</p>
	 *
	 * @return <code>true</code> bila dokumen aktif; <code>true</code> pula sebagai bawaan saat
	 *         kolom belum pernah diisi
	 */
	public Boolean getAktif() {

		if (getStatus().equals(UangMuka.DISETUJU)) {
			aktif = true;
		}
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}

		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}

		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif secara manual.
	 *
	 * <p>Nilai yang disetel di sini dapat ditimpa kembali pada pembacaan berikutnya oleh
	 * {@link #getAktif()} bila status atau jejak SOP menentukan lain.</p>
	 *
	 * @param aktif penanda aktif; <code>null</code> berarti &quot;belum ditentukan&quot; dan akan
	 *              dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mata anggaran (kegiatan RAB) yang menjadi sumber dana uang muka ini.
	 *
	 * <p><b>Getter berefek samping dan bercabang tiga.</b></p>
	 * <ol>
	 *   <li>Bila {@link #getTanpaAnggaran()} bernilai <code>true</code>, field
	 *       {@link #workspace} <b>dipaksa <code>null</code></b> &mdash; dokumen di luar pagu tidak
	 *       boleh menyeret mata anggaran apa pun, sekalipun kolomnya masih terisi di basis data.</li>
	 *   <li>Selain itu proxy Hibernate dinormalkan lewat <code>check(...)</code> yang diwarisi dari
	 *       {@link ais.database.model.GeneralValueObject}.</li>
	 *   <li>Bila {@link #getAmbilDariPr()} bernilai <code>true</code> dan {@link #getAngarans()}
	 *       tidak kosong, mata anggaran <b>direkonstruksi</b> dari daftar id pada kolom
	 *       <code>angarans</code> melalui {@link #ambilAngarans()}, lalu diambil elemen pertama
	 *       iterasi.</li>
	 * </ol>
	 *
	 * <p><b>Kasus tepi penting.</b> {@link #ambilAngarans()} mengembalikan <code>HashSet</code>,
	 * yang <i>tidak berurut</i>. Bila satu uang muka berbasis PR menyerap lebih dari satu mata
	 * anggaran, mata anggaran mana yang &quot;menang&quot; tidak dijamin stabil antar pemanggilan
	 * maupun antar JVM. Karena {@link #getAkun()} dan {@link #getSatuanKerja()} menurunkan nilainya
	 * dari sini, akun jurnal dan unit kerja dokumen bisa ikut berubah-ubah pada kasus multi-anggaran.
	 * Untuk pengajuan satu anggaran (kasus lazim) hal ini tidak terlihat.</p>
	 *
	 * <p><b>Cakupan tenant.</b> Id pada kolom <code>angarans</code> diselesaikan lewat
	 * <code>ConstantValues.ambil(...)</code>, yaitu pencarian berdasarkan id secara global tanpa
	 * penyaring satuan kerja/yayasan. Isi kolom itu dirakit layar dari baris PR yang sudah
	 * dipilih pengguna, tetapi kolomnya sendiri hanya teks biasa &mdash; tidak ada validasi ulang
	 * bahwa mata anggaran yang dirujuk memang milik unit kerja dokumen.</p>
	 *
	 * @return mata anggaran sumber dana, atau <code>null</code> untuk dokumen tanpa anggaran atau
	 *         yang daftar anggarannya kosong/tidak dapat diselesaikan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace", nullable = true)
	public Workspace getWorkspace() {
		if (getTanpaAnggaran()) {
			workspace = null;
		} else {
			workspace = check(workspace);
		}

		if (getAmbilDariPr() && !getAngarans().trim().isEmpty()) {
			Set<Workspace> workspaces = ambilAngarans();
			if (!workspaces.isEmpty()) {
				workspace = workspaces.iterator().next();
			}
			workspaces.clear();
			workspaces = null;
		}

		return workspace;
	}

	/**
	 * Menyetel mata anggaran sumber dana.
	 *
	 * <p>Nilai yang disetel di sini akan diabaikan pada pembacaan berikutnya bila dokumen bermode
	 * tanpa anggaran atau berbasis PR &mdash; lihat {@link #getWorkspace()}.</p>
	 *
	 * @param workspace mata anggaran; boleh <code>null</code>
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Nominal uang muka yang diajukan (rupiah).
	 *
	 * <p>Inilah angka yang menjadi dasar pemotongan pagu anggaran
	 * (<code>PenggunaanAnggaran</code>), nilai baris antrean transfer, dan nominal jurnal
	 * pencairan. Untuk pengajuan berbasis PR, layar menghitungnya otomatis sebagai jumlah
	 * <code>jumlah &times; hargaBeli</code> seluruh baris PR yang dipilih.</p>
	 *
	 * <p><b>Kasus tepi.</b> Getter menormalkan <code>null</code> menjadi <code>0.0</code> sehingga
	 * pemanggil dapat langsung berhitung tanpa penjagaan null. Tidak ada validasi tanda di level
	 * entity: nilai <code>0</code> maupun negatif dapat tersimpan (validasi layar hanya memastikan
	 * kotak isian tidak kosong).</p>
	 *
	 * @return nominal pengajuan; <code>0.0</code> bila belum diisi
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel nominal uang muka yang diajukan.
	 *
	 * @param nilai nominal rupiah; <code>null</code> akan dibaca sebagai <code>0.0</code>
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Awal periode penggunaan dana.
	 *
	 * <p><b>Getter berefek samping.</b> Bila kolom masih kosong, method ini <i>mengisinya</i>:
	 * pertama-tama dari tanggal mulai mata anggaran ({@link #getWorkspace()}), dan bila mata
	 * anggaran juga tidak ada, dari tanggal hari ini (<code>new Date()</code>). Nilai hasil
	 * pengisian itu bertahan di objek dan akan ikut tersimpan pada <code>flush</code> berikutnya.</p>
	 *
	 * <p><b>Perhatian.</b> Cabang terakhir memakai <code>new Date()</code> langsung, bukan
	 * <code>WaktuUtil.getDate()</code> seperti bagian lain kelas ini &mdash; jadi tidak mengikuti
	 * penyesuaian waktu terpusat bila ada. Nilai ini juga menjadi titik awal perhitungan
	 * {@link #getSampai()} dan {@link #getSelesai()}.</p>
	 *
	 * @return tanggal mulai; tidak pernah <code>null</code> setelah dipanggil sekali
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		workspace = getWorkspace();
		if (mulai == null && workspace != null) {
			mulai = workspace.getMulai();
		} else if (mulai == null) {
			mulai = new Date();
		}
		return mulai;
	}

	/**
	 * Menyetel awal periode penggunaan dana.
	 *
	 * @param mulai tanggal mulai; <code>null</code> akan diisi otomatis saat getter dipanggil
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Apakah pengajuan ini di luar pagu anggaran (tidak menunjuk mata anggaran mana pun).
	 *
	 * <p>Mode ini dipakai untuk kebutuhan mendesak yang belum punya pos anggaran. Konsekuensinya
	 * {@link #getWorkspace()} selalu <code>null</code>, sehingga {@link #getAkun()} tidak dapat
	 * menurunkan akun dari anggaran dan <code>PenggunaanAnggaran</code> tidak memotong pagu apa
	 * pun.</p>
	 *
	 * <p>Ketersediaan pilihan ini di layar dikendalikan konfigurasi
	 * <code>tampilkan_tanpa_anggaran</code>.</p>
	 *
	 * @return <code>true</code> bila tanpa anggaran; <code>false</code> sebagai bawaan
	 */
	public Boolean getTanpaAnggaran() {
		return tanpaAnggaran == null ? false : tanpaAnggaran;
	}

	/**
	 * Menyetel penanda pengajuan tanpa anggaran.
	 *
	 * @param tanpaAnggaran penanda; <code>null</code> dibaca sebagai <code>false</code>
	 */
	public void setTanpaAnggaran(Boolean tanpaAnggaran) {
		this.tanpaAnggaran = tanpaAnggaran;
	}

	/**
	 * Tenggat penyampaian laporan pertanggungjawaban atas uang muka ini.
	 *
	 * <p><b>Selalu dihitung ulang, tidak pernah dibaca dari kolom.</b> Rumusnya
	 * <code>getSampai() + N hari</code>, dengan <code>N</code> diambil dari konfigurasi
	 * <code>tgl_laporan_pengajuan_uang_muka</code> (bawaan <b>14</b> hari). Nilai lama pada kolom
	 * <code>selesai</code> ditimpa setiap kali getter dipanggil, sehingga:</p>
	 * <ul>
	 *   <li>mengubah konfigurasi jumlah hari <b>menggeser tenggat seluruh dokumen lama secara
	 *       retroaktif</b> &mdash; termasuk yang sudah lewat tenggat; dan</li>
	 *   <li>menyetel {@link #setSelesai(Date)} secara manual tidak ada gunanya, nilainya akan
	 *       tertimpa pada pembacaan berikutnya.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping tersembunyi.</b> <code>Common.getKonfigurasi(nama, default)</code> pada
	 * repo ini <i>menuliskan</i> baris konfigurasi bawaan ke basis data bila kunci tersebut belum
	 * ada. Jadi pemanggilan pertama getter ini (mis. saat me-render satu baris daftar) dapat
	 * menerbitkan <code>INSERT</code> ke tabel konfigurasi. Kegagalan parsing angka ditelan
	 * (dicatat ke <code>ErrorAuditUtil</code>) dan nilai jatuh kembali ke 14 &mdash; fail-open ke
	 * tenggat terpanjang, bukan terpendek.</p>
	 *
	 * <p><b>Penegakan.</b> Penelusuran seluruh repo menunjukkan nilai ini hanya <i>ditampilkan</i>
	 * (daftar uang muka, layar dana talangan, layar pertanggungjawaban). Tidak ada satu pun
	 * pemeriksaan yang menolak pencairan, menahan pengajuan baru, atau memberi peringatan ketika
	 * tenggat terlampaui &mdash; tenggat ini murni informatif.</p>
	 *
	 * <p><b>Kasus tepi.</b> Penjagaan <code>if (getMulai() != null)</code> praktis selalu benar
	 * karena {@link #getMulai()} tidak pernah mengembalikan <code>null</code>; kalkulasi sebenarnya
	 * bertumpu pada {@link #getSampai()}, bukan pada tanggal mulai.</p>
	 *
	 * @return tanggal tenggat pertanggungjawaban hasil perhitungan
	 */
	@Temporal(TemporalType.DATE)
	public Date getSelesai() {
		if (getMulai() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getSampai());

			int tglpengajuan = 14;
			try {
				tglpengajuan = Integer.parseInt(
						Common.getKonfigurasi("tgl_laporan_pengajuan_uang_muka", tglpengajuan + "").getNilai().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/UangMuka.java:257");
				// TODO: handle exception
			}

			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + tglpengajuan);
			selesai = calendar.getTime();
		}
		return selesai;
	}

	/**
	 * Menyetel tenggat pertanggungjawaban.
	 *
	 * <p>Praktis tanpa pengaruh: {@link #getSelesai()} selalu menghitung ulang nilainya. Setter ini
	 * hanya berguna bagi Hibernate saat memuat baris dan bagi rutin penyalinan objek.</p>
	 *
	 * @param selesai tanggal tenggat
	 */
	public void setSelesai(Date selesai) {
		this.selesai = selesai;
	}

	/**
	 * Menyetel pembuat/pemohon dokumen.
	 *
	 * @param dibuatOleh pengguna pemohon
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Pengguna yang mengajukan uang muka ini.
	 *
	 * <p>Perhatikan bahwa entity ini <b>tidak</b> menyimpan relasi ke entity Pegawai/Karyawan;
	 * &quot;penerima panjar&quot; direpresentasikan oleh akun pengguna sistem
	 * ({@link Tbmuser}) yang membuat dokumen. Untuk uang muka yang diajukan lewat alur SOP,
	 * pemohon yang sah adalah pengaju langkah awal disposisi &mdash; karena itu getter ini
	 * <b>menimpa</b> nilai kolom dengan <code>disposisiSop.getDisposisiStart().getDiajukanOleh()</code>
	 * bila tersedia.</p>
	 *
	 * <p><b>Efek samping.</b> Selain normalisasi proxy lewat <code>check(...)</code>, penimpaan di
	 * atas mengubah field instance sehingga dapat ikut tersimpan. Perlu pula diketahui bahwa
	 * <i>layar</i> daftar uang muka mengisi kolom ini dengan pengguna sesi ketika menemukan baris
	 * lama yang pembuatnya masih kosong &mdash; artinya sekadar membuka daftar dapat mengalihkan
	 * atribusi dokumen warisan kepada pembuka layar. Perilaku itu ada di
	 * <code>UangMukaAction</code>, bukan di kelas ini, tetapi konsekuensinya melekat pada properti
	 * ini.</p>
	 *
	 * @return pengguna pemohon, atau <code>null</code> bila belum pernah diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	/**
	 * Menyetel penyetuju dokumen.
	 *
	 * <p><b>Setter ini adalah tombol persetujuan yang sesungguhnya.</b> Mengisinya dengan pengguna
	 * mana pun membuat {@link #getStatus()} melaporkan &quot;Disetujui&quot;, yang selanjutnya
	 * membuka pengantrean pencairan dan kelayakan penjurnalan. Tidak ada validasi apa pun di level
	 * entity: tidak ada pemeriksaan peran, batas nominal, atasan, maupun larangan menyetujui
	 * dokumen yang dibuat sendiri. Seluruh pengendalian bertumpu pada lapisan pemanggil, dan
	 * lapisan itu (layar ZK) hanya memeriksa hak <code>UPDATE</code> atas menu yang sedang
	 * aktif.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju; <code>null</code> mengembalikan dokumen ke status
	 *                      pengajuan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Pengguna yang menyetujui uang muka ini &mdash; sekaligus penentu status dokumen.
	 *
	 * <p><b>Getter berefek samping dan berlapis.</b> Urutan kerjanya:</p>
	 * <ol>
	 *   <li>normalisasi proxy lewat <code>check(...)</code>;</li>
	 *   <li>bila alur SOP punya langkah persetujuan yang sudah diajukan seseorang, kolom
	 *       <b>ditimpa</b> dengan pengaju langkah tersebut &mdash; jejak SOP mengalahkan nilai yang
	 *       tersimpan;</li>
	 *   <li>sebaliknya, bila dokumen punya jejak SOP tetapi langkah persetujuannya belum ada,
	 *       penyetuju <b>dikosongkan</b> &mdash; dokumen ber-SOP tidak bisa &quot;disetujui
	 *       manual&quot; menembus alurnya;</li>
	 *   <li>normalisasi ulang, lalu bila {@link #getTanggalPersetujuanManual()} terisi <i>dan</i>
	 *       penyetuju ada, {@link #tanggalPersetujuan} <b>ditulis ulang</b> dengan tanggal manual
	 *       tersebut.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi langkah (4).</b> Tanggal persetujuan yang tercatat dapat sepenuhnya
	 * ditentukan pengguna lewat datebox &quot;tanggal persetujuan manual&quot; &mdash; termasuk
	 * tanggal mundur ke periode buku yang sudah lewat. Karena penulisan itu terjadi di dalam
	 * <i>getter</i>, pergeserannya bisa terjadi tanpa ada aksi simpan yang disengaja.</p>
	 *
	 * <p><b>Kasus tepi.</b> Untuk dokumen tanpa jejak SOP, langkah (2) dan (3) tidak berlaku dan
	 * nilai kolom dipakai apa adanya. Rantai <code>getDisposisiSop()</code> di sini tidak dibungkus
	 * try-catch, berbeda dari {@link #getTanggalPersetujuan()}.</p>
	 *
	 * @return pengguna penyetuju, atau <code>null</code> bila dokumen belum disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
		}

		return disetujuiOleh;
	}

	/**
	 * Menyetel waktu persetujuan efektif.
	 *
	 * <p>Nilai ini dapat ditimpa oleh {@link #getTanggalPersetujuan()} maupun
	 * {@link #getDisetujuiOleh()} pada pembacaan berikutnya bila dokumen memiliki jejak SOP atau
	 * tanggal persetujuan manual.</p>
	 *
	 * @param tanggalPersetujuan waktu persetujuan
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Waktu persetujuan efektif dokumen.
	 *
	 * <p><b>Getter berefek samping.</b> Bila alur SOP punya langkah persetujuan yang sudah diajukan
	 * seseorang, nilai ditimpa dengan waktu langkah tersebut; bila dokumen ber-SOP tetapi langkah
	 * persetujuannya belum ada, nilai dikosongkan. Seluruh blok itu dibungkus <code>try/catch</code>
	 * dengan alasan yang tercatat pada komentar di dalam kode: instance disposisi bisa berupa objek
	 * bersama (kanonik) milik interceptor audit yang proxy-nya terikat pada Session yang sudah
	 * ditutup, sehingga akses lazy melempar exception. Alih-alih membuat seluruh layar gagal,
	 * exception dicatat ke <code>ErrorAuditUtil</code> dan nilai cadangan dipertahankan.</p>
	 *
	 * <p><b>Kasus tepi.</b> Karena penurunan dari SOP dilewati saat exception terjadi, nilai yang
	 * dikembalikan pada kondisi itu bisa berupa nilai kolom yang <i>sudah usang</i> relatif terhadap
	 * jejak SOP. Penimpaan oleh tanggal persetujuan manual tidak terjadi di sini melainkan di
	 * {@link #getDisetujuiOleh()}.</p>
	 *
	 * <p><b>Bobot akuntansi.</b> Nilai inilah yang dipakai <code>PostingUangMukaAction</code>
	 * sebagai <b>tanggal transaksi jurnal</b> (bukan {@link #getTanggalTransaksi()} maupun
	 * {@link #getTanggalPembuatan()}). Karena {@link #getDisetujuiOleh()} dapat menimpanya dengan
	 * {@link #getTanggalPersetujuanManual()} yang dipilih bebas oleh pengguna, tanggal ini &mdash;
	 * dan karenanya periode buku tempat jurnal mendarat &mdash; ikut dapat ditentukan pengguna
	 * selama dokumen belum diposting. Penolakan jurnal hanya terjadi bila tanggalnya jatuh sebelum
	 * tanggal <i>closing</i> terakhir; periode terbuka mana pun setelah itu dapat dipilih.</p>
	 *
	 * @return waktu persetujuan, atau <code>null</code> bila dokumen belum disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: getDisposisiSop() bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke
			// Session lain yang sudah closed -> jangan biarkan getter ini crash, cukup
			// lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/UangMuka.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel waktu pembuatan dokumen.
	 *
	 * @param tanggalPembuatan waktu pembuatan
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Waktu dokumen dibuat.
	 *
	 * <p><b>Kasus tepi.</b> Bila kolom kosong, getter mengembalikan <code>new Date()</code> &mdash;
	 * waktu <i>saat dibaca</i>, bukan waktu pembuatan sebenarnya, dan tanpa menuliskannya ke field.
	 * Akibatnya dua pembacaan berturut-turut atas dokumen lama tanpa tanggal pembuatan dapat
	 * menghasilkan nilai berbeda, dan penyaringan rentang tanggal pada daftar (yang bekerja di
	 * kolom basis data, bukan lewat getter) tidak akan menemukan baris tersebut.</p>
	 *
	 * <p>Nilai ini juga menjadi cadangan terakhir bagi {@link #getTanggalTransaksi()} ketika dokumen
	 * belum melewati proses transfer.</p>
	 *
	 * @return waktu pembuatan; tidak pernah <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Status dokumen: {@link #PENGAJUAN}, {@link #DISETUJU}, atau {@link #DITOLAK}.
	 *
	 * <p><b>Nilai turunan, bukan kolom bebas.</b> Urutan penentuannya:</p>
	 * <ol>
	 *   <li>bila {@link #getDisetujuiOleh()} tidak <code>null</code>, status dipaksa
	 *       &quot;Disetujui&quot;;</li>
	 *   <li>sebaliknya, bila kolom status masih berbunyi &quot;Disetujui&quot; padahal penyetuju
	 *       sudah tidak ada, status dikembalikan ke &quot;Pengajuan&quot; &mdash; mekanisme
	 *       swa-koreksi bila persetujuan dicabut;</li>
	 *   <li>bila langkah akhir alur SOP adalah langkah penolakan, status dipaksa
	 *       &quot;Ditolak&quot; (mengalahkan kedua aturan di atas);</li>
	 *   <li>bila hasil akhirnya kosong, dikembalikan {@link #PENGAJUAN}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping.</b> Method ini menulis field {@link #status} dan {@link #disposisiSop},
	 * serta merambat ke {@link #getDisetujuiOleh()} yang sendirinya dapat menulis
	 * {@link #tanggalPersetujuan}. Membaca status karenanya bukan operasi murni.</p>
	 *
	 * <p><b>Kapan dipakai.</b> Menjadi gerbang di banyak tempat: pengantrean pencairan
	 * (<code>DaftarPengajuanTransfer.simpanUangMuka</code> hanya dipanggil untuk status
	 * &quot;Disetujui&quot;), penyaringan daftar, penonaktifan tombol salin/hapus pada baris yang
	 * sudah disetujui, dan penentuan apakah pemotongan pagu anggaran tetap berlaku.</p>
	 *
	 * @return salah satu dari {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK}; tidak pernah
	 *         <code>null</code> atau kosong
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		} else if (status != null && status.equals(DISETUJU)) {
			status = PENGAJUAN;
		}

		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			status = DITOLAK;
		}

		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Menyetel status dokumen.
	 *
	 * <p><b>Perilaku khusus untuk {@link #DITOLAK}:</b> selain menyimpan status, setter
	 * <i>membersihkan</i> jejak persetujuan dengan memanggil
	 * {@link #setDisetujuiOleh(Tbmuser)}<code>(null)</code> dan
	 * {@link #setTanggalPersetujuan(Date)}<code>(null)</code>. Tanpa pembersihan itu,
	 * {@link #getStatus()} akan langsung memaksa status kembali ke &quot;Disetujui&quot; pada
	 * pembacaan berikutnya karena penyetuju masih terisi.</p>
	 *
	 * <p><b>Kasus tepi yang perlu diperhatikan.</b> Untuk dokumen yang persetujuannya berasal dari
	 * alur SOP, pembersihan ini tidak bertahan: {@link #getDisetujuiOleh()} akan mengisi ulang
	 * penyetuju dari jejak SOP. Penolakan pada dokumen ber-SOP karenanya harus dilakukan lewat alur
	 * SOP (langkah penolakan), bukan lewat setter ini. Perlu dicatat pula bahwa setter ini tidak
	 * memeriksa {@link #getPostingHistory()} &mdash; secara teknis status dokumen yang jurnalnya
	 * sudah terbit masih dapat dibalik dari lapisan pemanggil, dan kanal ZK memang tidak
	 * mencegahnya (kanal REST <code>UangMukaApiHelper.ubahStatus</code> mencegahnya).</p>
	 *
	 * @param status status baru; nilai bebas, tetapi hanya tiga konstanta di kelas ini yang dikenali
	 *               logika hilir
	 */
	public void setStatus(String status) {

		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}

		this.status = status;
	}

	/**
	 * Dana talangan yang dipakai menutup sementara uang muka ini.
	 *
	 * <p>Dana talangan adalah dana pihak lain (mis. kas pribadi/unit lain) yang lebih dulu
	 * mengeluarkan uang, lalu diganti lembaga. Bila dana talangan tersebut sudah disetujui,
	 * {@link #getJenisUangMuka()} akan mengambil alih jenis/akun uang muka dari sana sehingga
	 * jurnalnya konsisten dengan dokumen talangan.</p>
	 *
	 * <p>Dipetakan <code>EAGER</code> secara efektif melalui <code>@Fetch(FetchMode.SELECT)</code>
	 * tanpa <code>fetch = LAZY</code>, berbeda dari mayoritas relasi lain di kelas ini.</p>
	 *
	 * @return dana talangan terkait, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dana_talangan", nullable = true)
	public DanaTalangan getDanaTalangan() {
		return danaTalangan;
	}

	/**
	 * Menyetel dana talangan terkait.
	 *
	 * @param danaTalangan dana talangan; boleh <code>null</code>
	 */
	public void setDanaTalangan(DanaTalangan danaTalangan) {
		this.danaTalangan = danaTalangan;
	}

	/**
	 * Unit kerja pemilik dokumen.
	 *
	 * <p><b>Getter berefek samping.</b> Bila mata anggaran ({@link #getWorkspace()}) punya satuan
	 * kerja, nilai kolom <b>ditimpa</b> dengan satuan kerja mata anggaran tersebut &mdash; sumber
	 * dana menentukan pemilik dokumen, bukan sebaliknya. Bila tidak, proxy kolom dinormalkan lewat
	 * <code>check(...)</code>.</p>
	 *
	 * <p><b>Peran dalam pengendalian akses.</b> Nilai inilah yang dipakai daftar uang muka untuk
	 * menyaring dokumen sesuai satuan kerja pengguna, dan dipakai {@link JenisUangMuka} untuk
	 * mencari jenis bawaan per unit. Perlu diketahui bahwa penyaringan di layar bersifat
	 * <i>aman-terbuka</i>: baris dengan kolom <code>satuan_kerja</code> <code>NULL</code> selalu
	 * ikut tampil bagi siapa pun, dan bila daftar satuan kerja pengguna kosong penyaring diganti
	 * kondisi selalu-benar. Karena kolom ini boleh <code>null</code> dan hanya terisi bila layar
	 * mengisinya, dokumen yang dibuat lewat jalur yang tidak menetapkannya menjadi terlihat lintas
	 * unit.</p>
	 *
	 * @return unit kerja pemilik, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {

		if (getWorkspace() != null && getWorkspace().getSatuanKerja() != null) {
			satuanKerja = getWorkspace().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menyetel unit kerja pemilik dokumen.
	 *
	 * <p>Nilai akan ditimpa pada pembacaan berikutnya bila mata anggaran dokumen punya satuan kerja
	 * sendiri &mdash; lihat {@link #getSatuanKerja()}.</p>
	 *
	 * @param satuanKerja unit kerja; boleh <code>null</code>
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kunci unik gabungan; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Tanggal persetujuan pilihan pengguna; lihat {@link #getTanggalPersetujuanManual()}. */
	private Date tanggalPersetujuanManual;
	/** BAST penerimaan barang; lihat {@link #getPenerimaanPengadaanMasterAsset()}. */
	private PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset;

	/**
	 * Kunci unik dokumen yang benar-benar ditegakkan basis data (<code>unique = true</code>).
	 *
	 * <p><b>Nilai terhitung, selalu ditulis ulang.</b> Bentuknya
	 * <code>&lt;kode&gt;_&lt;id disposisi SOP&gt;</code> bila dokumen punya jejak SOP, dan
	 * <code>&lt;kode&gt;_&lt;id dokumen&gt;</code> bila tidak. Karena nomor dokumen
	 * ({@link #getKode()}) sendiri tidak berindeks unik dan penomorannya dapat berulang setelah
	 * penghapusan data, kolom inilah satu-satunya pengaman yang mencegah dua baris benar-benar
	 * identik &mdash; tetapi ia <i>tidak</i> mencegah dua dokumen bernomor sama, karena id
	 * pembedanya selalu berbeda.</p>
	 *
	 * <p><b>Kasus tepi.</b> Untuk dokumen baru yang belum tersimpan dan belum bernomor, hasilnya
	 * berbentuk <code>&quot;null_null&quot;</code>. Bila dua dokumen semacam itu disimpan dalam satu
	 * transaksi, batasan unik dapat memicu kegagalan penyimpanan yang pesannya tidak informatif.
	 * Selain itu getter ini menulis field {@link #kodeUnik} pada setiap pembacaan sehingga ikut
	 * memicu pembaruan baris pada Session yang hidup.</p>
	 *
	 * @return kunci unik gabungan; tidak pernah <code>null</code>
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kunci unik gabungan.
	 *
	 * <p>Tanpa pengaruh praktis: {@link #getKodeUnik()} selalu menghitung ulang nilainya. Setter
	 * hanya diperlukan Hibernate saat memuat baris.</p>
	 *
	 * @param kodeUnik kunci unik
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Jejak alur persetujuan SOP untuk dokumen ini (implementasi kontrak {@link DataSop}).
	 *
	 * <p>Bila terisi, disposisi inilah sumber kebenaran bagi {@link #getDibuatOleh()},
	 * {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()}, {@link #getStatus()}, dan
	 * {@link #getAktif()} &mdash; nilai kolom pada tabel <code>uang_muka</code> hanya berlaku
	 * sebagai cadangan. Bila kosong, dokumen memakai jalur persetujuan sederhana lewat layar
	 * Persetujuan Uang Muka.</p>
	 *
	 * <p><b>Efek samping.</b> Menormalkan proxy lewat <code>check(...)</code> dan menuliskan
	 * hasilnya ke field. Getter ini dipanggil berkali-kali oleh getter lain di kelas ini, sehingga
	 * satu pembacaan status dapat memicu beberapa pemuatan lazy berturut-turut.</p>
	 *
	 * <p><b>Catatan operasional.</b> <code>DisposisiSop.hapus()</code> menghapus baris
	 * <code>uang_muka</code> yang menunjuk disposisi tersebut lewat SQL langsung
	 * (<code>delete from uang_muka where disposisi_sop=...</code>) tanpa melepas dokumen hilir
	 * terlebih dulu &mdash; membatalkan sebuah disposisi berarti membuang dokumen uang muka yang
	 * menempel padanya.</p>
	 *
	 * @return jejak disposisi SOP, atau <code>null</code> bila dokumen tidak melalui alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel jejak alur persetujuan SOP.
	 *
	 * <p><b>Setter defensif.</b> Nilai <code>null</code> maupun disposisi yang belum bersimpan
	 * (id masih <code>null</code>) <i>ditolak diam-diam</i> lewat <code>return</code> awal &mdash;
	 * tautan SOP yang sudah ada tidak boleh terhapus oleh objek kosong yang dioper rutin penyalinan
	 * atau oleh formulir yang me-reset variabelnya. Konsekuensinya <b>tautan SOP tidak dapat
	 * dilepas lewat setter ini</b>; pelepasan harus lewat SQL/level basis data.</p>
	 *
	 * <p><b>Kuirk.</b> Ekspresi ternary di baris berikutnya sudah tidak pernah bercabang: kondisi
	 * <code>disposisiSop == null || disposisiSop.getId() == null</code> pasti bernilai
	 * <code>false</code> karena kasus itu sudah dihentikan oleh penjagaan di atas. Efektifnya
	 * method ini setara dengan penugasan langsung. Bentuk ini dipertahankan apa adanya (kelas ini
	 * hanya didokumentasikan, tanpa perubahan logika).</p>
	 *
	 * @param disposisiSop jejak disposisi; <code>null</code> atau tanpa id akan diabaikan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Laporan pertanggungjawaban (LPJ) yang menutup uang muka ini.
	 *
	 * <p><b>Relasi dua arah dengan dua kolom FK.</b> Kolom <code>uang_muka.pertangungjawaban</code>
	 * di sini berdiri sendiri terhadap kolom <code>pertangungjawaban.uang_muka</code> (yang
	 * <code>nullable = false</code>) di sisi seberang &mdash; tidak ada <code>mappedBy</code> yang
	 * menyatukan keduanya. Artinya konsistensi kedua arah tidak dijamin Hibernate: sebuah LPJ bisa
	 * menunjuk uang muka ini sementara kolom di sini masih kosong, dan sebaliknya.</p>
	 *
	 * <p><b>Getter berefek samping (destruktif).</b> Bila LPJ yang tertaut ternyata sudah tidak
	 * aktif (<code>Pertangungjawaban.getAktif()</code> bernilai <code>false</code>, mis. karena
	 * disposisi SOP-nya ditolak/dinonaktifkan), field <b>dikosongkan menjadi <code>null</code></b>.
	 * Pada Session yang hidup, pengosongan itu dapat ikut tersimpan sehingga tautan LPJ hilang
	 * secara permanen dari sisi ini, walau baris LPJ-nya sendiri masih ada dan masih menunjuk balik
	 * ke dokumen ini. Sisi seberanglah yang tetap menjadi jejak yang bisa dipercaya.</p>
	 *
	 * <p><b>Pemakaian sebagai gerbang, dan apa yang TIDAK dijaga.</b> Nilai ini dipakai penyaring
	 * &quot;belum LPJ&quot; pada daftar dan sebagai larangan hapus pada kanal REST. Di luar itu
	 * urutan pertanggungjawaban tidak ditegakkan berantai: mesin posting pencairan uang muka tidak
	 * mensyaratkan adanya LPJ; penerbitan LPJ tidak mensyaratkan uang muka sudah dijurnal; dan
	 * jurnal pengembalian sisa dana di layar ZK menyaring LPJ hanya berdasarkan status posting dan
	 * tanggal &mdash; <b>tanpa</b> mensyaratkan LPJ tersebut sudah disetujui atau memang punya nilai
	 * yang dikembalikan (kanal statis/API menegakkan keduanya). Batas nilai LPJ pun dibandingkan
	 * terhadap {@link #getNilai()} penuh, bukan terhadap sisa setelah LPJ sebelumnya, sehingga
	 * beberapa LPJ berurutan atas satu dokumen masing-masing dapat mendekati nilai penuh.</p>
	 *
	 * <p><b>Tidak ada buku besar sisa panjar per dokumen.</b> Tidak satu pun properti pada entity
	 * ini berkurang ketika LPJ masuk atau ketika pengembalian dijurnal &mdash; {@link #getSaldo()}
	 * berbicara soal pagu anggaran, bukan sisa panjar. Sisa kewajiban penerima panjar hanya dapat
	 * disimpulkan dengan menjumlahkan LPJ terkait.</p>
	 *
	 * @return LPJ penutup yang masih aktif, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban", nullable = true)
	public Pertangungjawaban getPertangungjawaban() {
		if (pertangungjawaban != null && !pertangungjawaban.getAktif()) {
			pertangungjawaban = null;
		}
		return pertangungjawaban;
	}

	/**
	 * Menyetel laporan pertanggungjawaban penutup.
	 *
	 * @param pertangungjawaban LPJ; boleh <code>null</code>
	 */
	public void setPertangungjawaban(Pertangungjawaban pertangungjawaban) {
		this.pertangungjawaban = pertangungjawaban;
	}

	/**
	 * Jenis/akun uang muka &mdash; penentu akun buku besar yang dipakai saat menjurnal dokumen ini.
	 *
	 * <p><b>Mengapa penting.</b> Mesin posting mengambil <code>getAkun()</code> milik jenis ini
	 * sebagai akun uang muka (piutang kepada penerima panjar) dan
	 * <code>getAkunKelebihan()</code> sebagai lawannya saat sisa dana dikembalikan. Karena itu
	 * layar persetujuan mewajibkan jenis terisi sebelum dokumen boleh disetujui &mdash; tanpa jenis,
	 * jurnal tidak dapat dibentuk.</p>
	 *
	 * <p><b>Getter berefek samping, tiga tingkat penurunan:</b></p>
	 * <ol>
	 *   <li>bila dokumen ditalangi dana talangan yang <i>sudah disetujui</i> dan dana talangan itu
	 *       punya jenis uang muka sendiri, jenis di sini <b>ditimpa</b> dari sana &mdash; agar
	 *       jurnal talangan dan jurnal uang muka memakai akun yang sama;</li>
	 *   <li>bila tidak, proxy kolom dinormalkan lewat <code>check(...)</code>;</li>
	 *   <li>bila hasilnya masih <code>null</code> dan dokumen punya satuan kerja, dipakai jenis
	 *       bawaan unit tersebut lewat <code>JenisUangMuka.ambilDefault(satuanKerja)</code> (mencari
	 *       jenis ber-flag <code>defaultData</code> pada unit yang sama).</li>
	 * </ol>
	 *
	 * <p><b>Kasus tepi.</b> Langkah (3) hanya berjalan bila <code>satuanKerja</code> pada field
	 * sudah terisi &mdash; method ini memakai <code>check(satuanKerja)</code> langsung, bukan
	 * {@link #getSatuanKerja()}, sehingga penurunan satuan kerja dari mata anggaran tidak ikut
	 * berlaku di sini. Untuk dokumen yang satuan kerjanya baru akan diturunkan dari anggaran, jenis
	 * bawaan tidak akan ditemukan pada pemanggilan pertama. Bila unit belum punya jenis bawaan,
	 * hasilnya <code>null</code> dan dokumen tidak dapat disetujui lewat kanal REST (kanal ZK
	 * memvalidasinya di layar).</p>
	 *
	 * @return jenis/akun uang muka, atau <code>null</code> bila belum ditentukan dan tidak ada
	 *         bawaan unit
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_uang_muka", nullable = true)
	public JenisUangMuka getJenisUangMuka() {

		if (getDanaTalangan() != null && getDanaTalangan().getDisetujuiOleh() != null
				&& getDanaTalangan().getJenisUangMuka() != null) {
			jenisUangMuka = getDanaTalangan().getJenisUangMuka();
		} else {
			jenisUangMuka = check(jenisUangMuka);
		}

		satuanKerja = check(satuanKerja);
		if (jenisUangMuka == null && satuanKerja != null && satuanKerja.getId() != null) {
			jenisUangMuka = JenisUangMuka.ambilDefault(satuanKerja);
		}

		return jenisUangMuka;
	}

	/**
	 * Menyetel jenis/akun uang muka.
	 *
	 * <p>Nilai dapat ditimpa pada pembacaan berikutnya bila dokumen ditalangi dana talangan yang
	 * sudah disetujui &mdash; lihat {@link #getJenisUangMuka()}.</p>
	 *
	 * @param jenisUangMuka jenis uang muka; boleh <code>null</code>
	 */
	public void setJenisUangMuka(JenisUangMuka jenisUangMuka) {
		this.jenisUangMuka = jenisUangMuka;
	}

	/**
	 * Potret sisa pagu anggaran pada saat dokumen disimpan/ditampilkan.
	 *
	 * <p><b>Bukan sisa panjar.</b> Nilai ini dihitung
	 * <code>JenisUangMukaAction.hitungSaldo(...)</code> sebagai pagu {@link Workspace} dikurangi
	 * seluruh <code>PenggunaanAnggaran</code> yang sudah tercatat sampai tanggal tertentu, dengan
	 * dokumen ini sendiri dikecualikan dari penjumlahan. Kolom layar menampilkannya berdampingan
	 * dengan {@link #getNilai()} dan kolom hitungan <i>Sisa</i> = <code>saldo - nilai</code>.
	 * Menafsirkannya sebagai &quot;uang muka yang belum dipertanggungjawabkan&quot; adalah kekeliruan
	 * yang mudah terjadi karena namanya.</p>
	 *
	 * <p><b>Sifat data.</b> Nilai ini bersifat cache tampilan, bukan sumber kebenaran: layar
	 * menghitung ulang dan menuliskannya kembali saat baris dirender maupun saat tombol
	 * &quot;Hitung Ulang&quot; ditekan. Untuk dokumen tanpa mata anggaran (mode tanpa anggaran)
	 * hasil perhitungan selalu <code>0.0</code>. Kanal REST memakainya sebagai pembatas: pengajuan
	 * ditolak bila nominal melebihi sisa pagu.</p>
	 *
	 * @return potret sisa pagu; <code>0.0</code> bila belum pernah dihitung
	 */
	public Double getSaldo() {
		return saldo == null ? 0.0 : saldo;
	}

	/**
	 * Menyetel potret sisa pagu anggaran.
	 *
	 * @param saldo nilai sisa pagu; <code>null</code> dibaca sebagai <code>0.0</code>
	 */
	public void setSaldo(Double saldo) {
		this.saldo = saldo;
	}

	/**
	 * Akun buku besar sumber dana dokumen ini.
	 *
	 * <p><b>Getter berefek samping dan menimpa pilihan pengguna.</b> Setelah menormalkan proxy
	 * kolom lewat <code>check(...)</code>, method ini <b>menimpa</b> akun dengan
	 * <code>getWorkspace().getAkun()</code> setiap kali dokumen punya mata anggaran. Artinya akun
	 * yang dipilih manual di formulir hanya bertahan pada dokumen bermode <i>tanpa anggaran</i>
	 * (yang memang menyembunyikan mata anggaran); pada mode lain pilihan itu selalu dikalahkan akun
	 * mata anggaran.</p>
	 *
	 * <p><b>Penanganan error.</b> Seluruh blok penurunan dibungkus <code>try/catch</code> lebar yang
	 * mencatat exception ke <code>ErrorAuditUtil</code> lalu melanjutkan &mdash; melindungi jalur
	 * render dari <code>LazyInitializationException</code> saat mata anggaran tidak dapat dimuat.
	 * Bila itu terjadi, nilai yang dikembalikan adalah akun kolom (hasil <code>check</code>), yang
	 * bisa berbeda dari akun mata anggaran.</p>
	 *
	 * <p><b>Verifikasi penting soal jurnal.</b> Meski namanya menyarankan sebaliknya, <b>mesin
	 * posting uang muka tidak pernah membaca properti ini</b>. Penelusuran
	 * <code>PostingUangMukaAction</code> menunjukkan akun debet diambil dari
	 * <code>getJenisUangMuka().getAkun()</code> dan akun kredit dari cara pembayaran pada proses
	 * transfer (<code>akunTransitori</code> untuk dana singgah, <code>akun</code> untuk transfer
	 * langsung). Properti ini hanya dipakai layar dan laporan sebagai keterangan sumber dana. Jadi
	 * mengubah akun di sini tidak mengubah jurnal yang terbit.</p>
	 *
	 * @return akun buku besar sumber dana (informatif; bukan akun jurnal), atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		try {
			if (getWorkspace() != null) {
				akun = getWorkspace().getAkun();
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/UangMuka.java:481");
			// TODO: handle exception
		}
		return akun;
	}

	/**
	 * Menyetel akun buku besar sumber dana.
	 *
	 * <p>Untuk dokumen yang punya mata anggaran, nilai ini akan ditimpa pada pembacaan berikutnya
	 * &mdash; lihat {@link #getAkun()}.</p>
	 *
	 * @param akun akun buku besar; boleh <code>null</code>
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Batch penjurnalan yang sudah membukukan dokumen ini &mdash; sekaligus <b>penanda anti-jurnal
	 * ganda</b>.
	 *
	 * <p><b>Cara kerja pengaman.</b> <code>PostingUangMukaAction</code> menyaring calon dokumen
	 * dengan <code>Restrictions.isNull("postingHistory")</code>, sehingga dokumen yang kolom ini
	 * sudah terisi tidak akan ikut diproses lagi &mdash; menekan &quot;Posting Semua&quot; berkali-kali
	 * tidak menggandakan jurnal. Pengaman kedua ada di sisi jurnal: <code>GrupTransaksi</code>
	 * memakai kunci idempotensi <code>ambilUnik()</code> berbentuk
	 * <code>ais.database.model.akunting.UangMuka_&lt;id&gt;</code>.</p>
	 *
	 * <p><b>Pembatalan posting &mdash; perhatikan asimetri antar jalur.</b> Aksi
	 * &quot;batalkan&quot; mengosongkan kolom ini kembali agar dokumen layak diposting ulang; itu
	 * memang tujuannya. Namun cara membersihkan jurnalnya berbeda antar jalur:
	 * jalur statis (dipakai dasbor/API) menghapus <b>dua</b> tabel &mdash; baris
	 * <code>akunting.transaksi</code> lebih dulu, baru <code>akunting.grup_transaksi</code> &mdash;
	 * sedangkan jalur tombol di layar ZK hanya menghapus <code>akunting.grup_transaksi</code>.
	 * Karena kunci idempotensi melekat pada baris grup transaksi, membuang header tanpa membuang
	 * barisnya membuat posting ulang lolos dari pemeriksaan duplikat sekaligus meninggalkan baris
	 * transaksi yatim. Selama tabel <code>akunting.transaksi</code> tidak berkaskade hapus di level
	 * basis data, urutan &quot;batal lewat layar lalu posting ulang&quot; dapat menggandakan angka
	 * di buku besar. Pembatalan juga hanya menyaring <code>closing is null</code> &mdash; tidak ada
	 * pemeriksaan periode buku lain.</p>
	 *
	 * <p><b>Catatan.</b> Kolom ini <i>tidak</i> berbicara soal pencairan uang. Pencairan diwakili
	 * {@link #getDaftarPengajuanTransfer()}; jurnal dan kas adalah dua jalur terpisah, dan tidak ada
	 * pemeriksaan silang di level entity yang memastikan keduanya selaras.</p>
	 *
	 * @return batch posting yang membukukan dokumen ini, atau <code>null</code> bila belum dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel batch penjurnalan.
	 *
	 * <p>Dipanggil mesin posting saat membukukan dokumen, dan dipanggil dengan <code>null</code>
	 * saat posting dibatalkan sehingga dokumen kembali masuk antrean jurnal.</p>
	 *
	 * @param postingHistory batch posting; <code>null</code> berarti belum/tidak lagi dijurnal
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Tahun buku dokumen, dipakai penyaring laporan dan rekap periodik.
	 *
	 * <p><b>Getter berefek samping.</b> Bila kolom kosong, diisi tahun berjalan dari
	 * <code>WaktuUtil</code> dan nilai itu bertahan di objek. Perhatikan bahwa tahun berjalan
	 * diambil saat <i>pembacaan</i>, bukan dari {@link #getMulai()} atau
	 * {@link #getTanggalPembuatan()} &mdash; dokumen lama yang kolomnya kosong lalu dibuka pada
	 * tahun berikutnya akan tercap tahun yang salah. Layar dan kanal REST menghindari hal itu
	 * dengan mengisi tahun/bulan secara eksplisit dari tanggal mulai saat menyimpan.</p>
	 *
	 * @return tahun buku; tidak pernah <code>null</code> setelah dipanggil sekali
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun buku dokumen.
	 *
	 * @param tahun tahun buku
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Alur penomoran surat keuangan yang dipakai membentuk {@link #getKode()}.
	 *
	 * <p><b>Getter berefek samping.</b> Bila kolom kosong, diisi konstanta statis global
	 * <code>NomorSuratAlurKeuangan.UANG_MUKA_DATA</code> &mdash; alur bawaan modul uang muka.
	 * Konstanta itu dimuat sekali saat aplikasi hidup; bila belum terkonfigurasi, penomoran jatuh
	 * ke barcode acak (perilaku ada di layar/kanal REST, bukan di sini).</p>
	 *
	 * <p><b>Kasus tepi.</b> Karena yang diisikan adalah instance statis bersama, seluruh dokumen
	 * yang kolomnya kosong akan menunjuk objek yang sama. Bila objek itu berasal dari Session yang
	 * sudah ditutup, pembacaan properti alurnya dapat gagal &mdash; getter ini sendiri tidak
	 * membungkusnya dengan try-catch.</p>
	 *
	 * @return alur penomoran surat keuangan; jarang <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_keuangan", nullable = true)
	public NomorSuratAlurKeuangan getNomorSuratAlurKeuangan() {
		if (nomorSuratAlurKeuangan == null) {
			nomorSuratAlurKeuangan = NomorSuratAlurKeuangan.UANG_MUKA_DATA;
		} else {
			nomorSuratAlurKeuangan = check(nomorSuratAlurKeuangan);
		}
		return nomorSuratAlurKeuangan;
	}

	/**
	 * Menyetel alur penomoran surat keuangan.
	 *
	 * @param nomorSuratAlurKeuangan alur penomoran; <code>null</code> akan diisi bawaan saat getter
	 *                               dipanggil
	 */
	public void setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan nomorSuratAlurKeuangan) {
		this.nomorSuratAlurKeuangan = nomorSuratAlurKeuangan;
	}

	/**
	 * Bulan buku dokumen (1&ndash;12), pasangan {@link #getTahun()}.
	 *
	 * <p><b>Getter berefek samping.</b> Bila kolom kosong, diisi bulan berjalan
	 * (<code>Calendar.MONTH + 1</code> agar berbasis satu, bukan nol). Berlaku peringatan yang sama
	 * dengan {@link #getTahun()}: bulan diambil saat pembacaan, bukan dari tanggal dokumen.</p>
	 *
	 * @return bulan buku 1&ndash;12; tidak pernah <code>null</code> setelah dipanggil sekali
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan buku dokumen.
	 *
	 * @param bulan bulan buku 1&ndash;12
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Baris antrean pembayaran/pencairan (menu Pembayaran Transfer) untuk dokumen ini.
	 *
	 * <p><b>Penanda &quot;sudah diantrekan untuk dicairkan&quot;.</b> Kolom ini diisi
	 * <code>DaftarPengajuanTransfer.simpanUangMuka(this)</code>, yang <i>menolak bekerja</i> bila
	 * kolomnya sudah terisi &mdash; inilah pengaman utama terhadap pencairan ganda: satu dokumen
	 * uang muka hanya bisa punya satu baris antrean transfer. Pengaman itu berlaku di ketiga jalur
	 * yang memanggilnya (simpan di layar, render daftar, penyelaras massal, dan kanal REST
	 * pengajuan ke proses transfer), dan ketiganya juga mensyaratkan status
	 * {@link #DISETUJU}.</p>
	 *
	 * <p><b>Yang tidak dijaga.</b> Pengaman itu bersifat &quot;satu antrean per dokumen&quot;, bukan
	 * &quot;satu realisasi kas per dokumen&quot;. Berapa kali baris antrean tersebut benar-benar
	 * direalisasikan menjadi kas ditentukan modul Proses Transfer, di luar entity ini. Selain itu,
	 * bila kolom ini dikosongkan (mis. baris antrean dihapus), dokumen yang berstatus
	 * &quot;Disetujui&quot; akan diantrekan ulang secara otomatis begitu daftar uang muka
	 * di-render.</p>
	 *
	 * <p><b>Peran lain.</b> Menjadi sumber {@link #getTanggalTransaksi()}, dan menjadi larangan
	 * hapus pada kanal REST.</p>
	 *
	 * @return baris antrean transfer, atau <code>null</code> bila belum diantrekan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menyetel baris antrean pembayaran/pencairan.
	 *
	 * @param daftarPengajuanTransfer baris antrean; boleh <code>null</code>
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Akhir periode penggunaan dana.
	 *
	 * <p><b>Getter berefek samping dengan koreksi otomatis.</b> Bila tanggal akhir tersimpan
	 * mendahului {@link #getMulai()}, nilainya <b>diperbaiki paksa</b> menjadi sama dengan tanggal
	 * mulai &mdash; periode terbalik tidak pernah dikembalikan ke pemanggil. Bila kolom kosong,
	 * dikembalikan tanggal mulai (tanpa menuliskannya ke field, sehingga kolom tetap kosong di
	 * basis data).</p>
	 *
	 * <p><b>Kasus tepi.</b> Perbandingan memakai field {@link #mulai} mentah, bukan
	 * {@link #getMulai()}; pada objek yang baru dimuat dan belum pernah dibaca tanggal mulainya,
	 * <code>mulai</code> bisa masih <code>null</code> sehingga koreksi dilewati dan tanggal akhir
	 * yang lebih awal tetap lolos pada pembacaan pertama.</p>
	 *
	 * <p>Nilai ini menjadi dasar perhitungan tenggat {@link #getSelesai()}.</p>
	 *
	 * @return tanggal akhir periode; tidak pernah <code>null</code>
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		if (sampai != null && mulai != null && sampai.before(mulai)) {
			sampai = mulai;
		}
		return sampai == null ? getMulai() : sampai;
	}

	/**
	 * Menyetel akhir periode penggunaan dana.
	 *
	 * @param sampai tanggal akhir; nilai yang mendahului tanggal mulai akan dikoreksi saat dibaca
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Apakah uang muka ini lahir dari Permintaan Pengadaan (PR) barang/jasa.
	 *
	 * <p>Bila <code>true</code>, pemohon tidak memilih mata anggaran maupun nominal secara bebas:
	 * nominal dihitung dari baris PR yang dipilih, id barisnya disimpan di
	 * {@link #getPermintaanPengadaanMasterAssets()}, dan mata anggarannya di
	 * {@link #getAngarans()}. Mode ini juga mengubah cara {@link #getWorkspace()} bekerja
	 * (merekonstruksi anggaran dari string id) dan cara kolom <i>Sisa</i> ditampilkan di layar.</p>
	 *
	 * <p>Pemotongan pagu anggaran untuk mode ini sengaja dikecualikan di
	 * <code>PenggunaanAnggaran</code> karena pagunya sudah dipotong saat PR diajukan &mdash; tanpa
	 * pengecualian itu anggaran akan terpotong dua kali.</p>
	 *
	 * @return <code>true</code> bila berbasis PR; <code>false</code> sebagai bawaan
	 */
	public Boolean getAmbilDariPr() {
		return ambilDariPr == null ? false : ambilDariPr;
	}

	/**
	 * Menyetel penanda pengajuan berbasis PR.
	 *
	 * @param ambilDariPr penanda; <code>null</code> dibaca sebagai <code>false</code>
	 */
	public void setAmbilDariPr(Boolean ambilDariPr) {
		this.ambilDariPr = ambilDariPr;
	}

	/**
	 * Daftar id baris Permintaan Pengadaan yang dibiayai uang muka ini, sebagai teks dipisah koma.
	 *
	 * <p><b>Relasi &quot;banyak&quot; yang disimpan sebagai string,</b> bukan tabel penghubung.
	 * Konsekuensinya tidak ada integritas referensial: baris PR yang dihapus meninggalkan id
	 * gantung yang baru ketahuan saat penguraian gagal (dan kegagalan itu ditelan diam-diam oleh
	 * pemanggilnya).</p>
	 *
	 * <p><b>Getter berefek samping dan menormalkan.</b> Bila dokumen bukan mode PR, isi kolom
	 * <b>dikosongkan paksa</b> menjadi string kosong &mdash; mematikan mode PR sekaligus membuang
	 * jejak baris PR yang pernah dipilih. Bila mode PR aktif, nilai di-<code>trim()</code> dan
	 * bentuk sisa-hapus berupa koma saja (<code>&quot;,&quot;</code> sampai
	 * <code>&quot;,,,,&quot;</code>) dibersihkan menjadi kosong.</p>
	 *
	 * <p><b>Kuirk.</b> Pembersihan koma dilakukan dengan empat perbandingan literal berantai,
	 * bukan dengan pemeriksaan pola; daftar sisa berupa lima koma atau lebih
	 * (<code>&quot;,,,,,&quot;</code>) lolos tanpa dibersihkan dan tetap tersimpan sebagai teks yang
	 * secara semantik kosong. Pola yang sama diulang di {@link #getAngarans()}.</p>
	 *
	 * @return daftar id baris PR dipisah koma; string kosong bila tidak ada
	 */
	@Column(columnDefinition = "text")
	public String getPermintaanPengadaanMasterAssets() {

		if (!getAmbilDariPr()) {
			permintaanPengadaanMasterAssets = "";
		} else {
			permintaanPengadaanMasterAssets = (permintaanPengadaanMasterAssets == null ? ""
					: permintaanPengadaanMasterAssets.trim());

			if (permintaanPengadaanMasterAssets.equals(",")) {
				permintaanPengadaanMasterAssets = "";
			} else if (permintaanPengadaanMasterAssets.equals(",,")) {
				permintaanPengadaanMasterAssets = "";
			} else if (permintaanPengadaanMasterAssets.equals(",,,")) {
				permintaanPengadaanMasterAssets = "";
			} else if (permintaanPengadaanMasterAssets.equals(",,,,")) {
				permintaanPengadaanMasterAssets = "";
			}
		}
		return permintaanPengadaanMasterAssets;
	}

	/**
	 * Menyetel daftar id baris PR (teks dipisah koma).
	 *
	 * <p>Nilai akan dikosongkan pada pembacaan berikutnya bila dokumen bukan mode PR &mdash; lihat
	 * {@link #getPermintaanPengadaanMasterAssets()}.</p>
	 *
	 * @param permintaanPengadaanMasterAssets daftar id dipisah koma
	 */
	public void setPermintaanPengadaanMasterAssets(String permintaanPengadaanMasterAssets) {
		this.permintaanPengadaanMasterAssets = permintaanPengadaanMasterAssets;
	}

	/**
	 * Daftar id mata anggaran ({@link Workspace}) milik baris PR yang dipilih, sebagai teks dipisah
	 * koma.
	 *
	 * <p>Kolom ini adalah jejak anggaran untuk dokumen berbasis PR: {@link #getWorkspace()}
	 * merekonstruksi mata anggaran dari sini lewat {@link #ambilAngarans()}, dan
	 * {@link #getAkun()} menurunkan akun dari mata anggaran hasil rekonstruksi itu.</p>
	 *
	 * <p><b>Getter berefek samping.</b> Nilai di-<code>trim()</code> dan bentuk sisa-hapus berupa
	 * satu sampai empat koma dibersihkan menjadi string kosong; hasilnya ditulis balik ke field.
	 * Berbeda dari {@link #getPermintaanPengadaanMasterAssets()}, method ini <b>tidak</b>
	 * mengosongkan kolom pada dokumen non-PR &mdash; jejak anggaran lama bertahan walau mode PR
	 * dimatikan, dan tetap dibaca oleh {@link #getWorkspace()} bila mode PR dinyalakan lagi.</p>
	 *
	 * <p><b>Kuirk yang sama:</b> pembersihan koma memakai empat perbandingan literal, sehingga lima
	 * koma atau lebih tidak tertangani.</p>
	 *
	 * @return daftar id mata anggaran dipisah koma; string kosong bila tidak ada
	 */
	@Column(columnDefinition = "text")
	public String getAngarans() {
		angarans = (angarans == null ? "" : angarans.trim());

		if (angarans.equals(",")) {
			angarans = "";
		} else if (angarans.equals(",,")) {
			angarans = "";
		} else if (angarans.equals(",,,")) {
			angarans = "";
		} else if (angarans.equals(",,,,")) {
			angarans = "";
		}
		return angarans;
	}

	/**
	 * Menyetel daftar id mata anggaran (teks dipisah koma).
	 *
	 * @param angarans daftar id dipisah koma
	 */
	public void setAngarans(String angarans) {
		this.angarans = angarans;
	}

	/**
	 * Menguraikan {@link #getAngarans()} menjadi himpunan {@link Workspace} yang sesungguhnya.
	 *
	 * <p><b>Tujuan.</b> Menjembatani penyimpanan berbasis teks dengan objek entity, sehingga
	 * {@link #getWorkspace()} dapat menentukan mata anggaran dokumen berbasis PR dan pemanggil lain
	 * dapat menampilkan seluruh anggaran yang tersentuh.</p>
	 *
	 * <p><b>Cara kerja.</b> Memecah string dengan pemisah koma, melewati potongan kosong, lalu
	 * menyelesaikan tiap id lewat <code>ConstantValues.ambil(Workspace.class.getName(), id)</code>
	 * &mdash; pencarian cache-lalu-basis-data <b>berdasarkan id saja, tanpa penyaring satuan kerja,
	 * yayasan, maupun status aktif</b>. Hanya objek yang berhasil ditemukan dan sudah ber-id yang
	 * dimasukkan ke himpunan hasil.</p>
	 *
	 * <p><b>Penanganan error.</b> Setiap potongan diproses dalam <code>try/catch</code> tersendiri;
	 * id yang tidak berupa angka atau tidak ditemukan dilewati diam-diam (dicatat ke
	 * <code>ErrorAuditUtil</code>). Akibatnya himpunan hasil bisa lebih kecil dari daftar id tanpa
	 * ada tanda kegagalan bagi pengguna &mdash; anggaran yang hilang tidak terlihat, dokumen tetap
	 * tampil seolah wajar.</p>
	 *
	 * <p><b>Kasus tepi.</b> Hasil dibungkus ulang menjadi <code>HashSet</code> baru (salinan
	 * defensif), sehingga pemanggil boleh memodifikasinya &mdash; {@link #getWorkspace()} memang
	 * memanggil <code>clear()</code> atasnya. Karena <code>HashSet</code> tidak berurut, elemen
	 * &quot;pertama&quot; yang diambil {@link #getWorkspace()} tidak deterministik bila daftarnya
	 * lebih dari satu.</p>
	 *
	 * <p><b>Biaya.</b> Satu pencarian per id pada setiap pemanggilan, tanpa cache di level objek;
	 * dan {@link #getWorkspace()} memanggilnya setiap kali dibaca. Pada daftar uang muka berbasis PR
	 * yang panjang, ini berarti banyak pencarian berulang per render.</p>
	 *
	 * @return himpunan mata anggaran hasil penguraian; kosong (bukan <code>null</code>) bila daftar
	 *         kosong atau seluruh id gagal diselesaikan
	 */
	public Set<Workspace> ambilAngarans() {
		Set<Workspace> workspaces = new HashSet<Workspace>();
		for (String id : getAngarans().split(",")) {
			if (!id.trim().isEmpty()) {
				try {

					Workspace workspace = (Workspace) ConstantValues.ambil(Workspace.class.getName(),
							Long.parseLong(id));
					if (workspace != null && workspace.getId() != null) {
						workspaces.add(workspace);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/UangMuka.java:627");
					// TODO: handle exception
				}
			}
		}

		return new HashSet<Workspace>(workspaces);
	}

	/**
	 * Tanggal transaksi kas &mdash; tanggal yang dipakai jurnal, bukan tanggal dokumen dibuat.
	 *
	 * <p><b>Getter berefek samping, tiga cabang berurutan:</b></p>
	 * <ol>
	 *   <li>bila dokumen sudah masuk antrean transfer dan antrean itu ditandai <i>transitori</i>
	 *       (dana singgah di rekening perantara) serta proses transitorinya ada, dipakai tanggal
	 *       pembuatan proses transitori;</li>
	 *   <li>bila tidak, dan antrean punya proses transfer, dipakai tanggal realisasi transfer
	 *       &mdash; atau tanggal pembuatan proses transfer bila realisasinya belum diisi;</li>
	 *   <li>bila dokumen belum melewati proses transfer sama sekali, dipakai
	 *       {@link #getTanggalPembuatan()}.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi.</b> Selama uang belum benar-benar ditransfer, nilai ini &quot;mengambang&quot;
	 * mengikuti {@link #getTanggalPembuatan()} yang &mdash; bila kolomnya kosong &mdash; berarti
	 * tanggal hari ini. Setelah transfer terealisasi, tanggal berpindah ke tanggal realisasi. Jadi
	 * nilai yang dilaporkan properti ini dapat berbeda sebelum dan sesudah pencairan.</p>
	 *
	 * <p><b>Verifikasi.</b> Meski namanya menyarankan sebaliknya, <b>mesin posting uang muka tidak
	 * memakai properti ini sebagai tanggal jurnal</b>. <code>PostingUangMukaAction</code> memakai
	 * {@link #getTanggalPersetujuan()} sebagai tanggal transaksi jurnal. Properti ini dipakai
	 * laporan dan tampilan &mdash; berguna untuk rekonsiliasi kas, tetapi bukan penentu periode
	 * pembukuan.</p>
	 *
	 * <p><b>Kasus tepi.</b> Cabang pertama dan kedua mengakses field {@link #daftarPengajuanTransfer}
	 * langsung setelah memanggil {@link #getDaftarPengajuanTransfer()}; keduanya merujuk objek yang
	 * sama sehingga tidak ada perbedaan perilaku, tetapi pola ini membuat method rapuh bila kelak
	 * getter tersebut diberi logika penurunan.</p>
	 *
	 * @return tanggal transaksi kas; tidak pernah <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getTransitori()
				&& daftarPengajuanTransfer.getTransitoriData() != null
				&& daftarPengajuanTransfer.getTransitoriData().getProsesTransitori() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getTransitoriData().getProsesTransitori().getTanggalPembuatan();
		} else if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getProsesTransfer() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan() == null
					? daftarPengajuanTransfer.getProsesTransfer().getTanggalPembuatan()
					: daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan();
		} else {
			tanggalTransaksi = getTanggalPembuatan();
		}
		return tanggalTransaksi;
	}

	/**
	 * Menyetel tanggal transaksi kas.
	 *
	 * <p>Praktis tanpa pengaruh: {@link #getTanggalTransaksi()} selalu menghitung ulang nilainya
	 * dari status proses transfer.</p>
	 *
	 * @param tanggalTransaksi tanggal transaksi
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Tanggal persetujuan yang dipilih sendiri oleh penyetuju lewat datebox di formulir.
	 *
	 * <p><b>Mengapa perlu diperhatikan.</b> Bila kolom ini terisi dan dokumen punya penyetuju,
	 * {@link #getDisetujuiOleh()} menimpa {@link #getTanggalPersetujuan()} dengan nilai ini. Karena
	 * datebox tidak dibatasi rentang, tanggal persetujuan yang tercatat dapat <b>dimundurkan ke
	 * periode buku mana pun</b> &mdash; termasuk periode yang sudah dilaporkan. Layar hanya
	 * mengunci datebox tersebut menjadi label setelah dokumen dijurnal
	 * ({@link #getPostingHistory()} tidak <code>null</code>).</p>
	 *
	 * <p><b>Catatan lapisan pemanggil.</b> Pada layar ZK, datebox ini dipasang tanpa memandang mode
	 * (pengajuan/persetujuan/hanya-lihat) dan perubahannya langsung dituliskan ke basis data lewat
	 * penangan <code>onChange</code> &mdash; tanpa melewati tombol Simpan. Satu-satunya prasyarat
	 * adalah dokumen belum dijurnal.</p>
	 *
	 * @return tanggal persetujuan pilihan pengguna, atau <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/**
	 * Menyetel tanggal persetujuan pilihan pengguna.
	 *
	 * @param tanggalPersetujuanManual tanggal persetujuan manual; <code>null</code> berarti memakai
	 *                                 tanggal yang diturunkan sistem/SOP
	 */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}

	/**
	 * Menyetel BAST penerimaan barang hasil pengadaan yang terkait dokumen ini.
	 *
	 * @param penerimaanPengadaanMasterAsset dokumen penerimaan barang; boleh <code>null</code>
	 */
	public void setPenerimaanPengadaanMasterAsset(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
		this.penerimaanPengadaanMasterAsset = penerimaanPengadaanMasterAsset;
	}

	/**
	 * Berita Acara Serah Terima (BAST) penerimaan barang yang menutup uang muka pengadaan ini.
	 *
	 * <p>Relevan untuk uang muka bermode PR: setelah barang diterima, BAST menjadi bukti bahwa dana
	 * yang dipanjarkan sudah terwujud menjadi aset. Daftar uang muka menyediakan penyaring
	 * &quot;sudah BAST&quot; berdasarkan kolom ini, dan renderer menampilkannya sebagai tautan ke
	 * dokumen penerimaan.</p>
	 *
	 * <p>Getter ini murni &mdash; tidak menormalkan proxy dan tidak menulis balik &mdash; sehingga
	 * nilainya bisa berupa proxy Hibernate yang belum termuat. Pemanggil pada thread/Session lain
	 * perlu berhati-hati terhadap <code>LazyInitializationException</code> meski relasi ini
	 * dipetakan <code>@Fetch(FetchMode.SELECT)</code> tanpa <code>LAZY</code> eksplisit.</p>
	 *
	 * @return dokumen BAST penerimaan barang, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_master_asset", nullable = true)
	public PenerimaanPengadaanMasterAsset getPenerimaanPengadaanMasterAsset() {
		return penerimaanPengadaanMasterAsset;
	}
}
