package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BankHost;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.bni.BniRequest;
import ais.database.model.bri.BriRequest;
import ais.database.model.bsi.BsiRequest;
import ais.ui.util.MyCheckboxConfig;

/**
 * Entity <b>bukti pembayaran siswa</b> — satu baris tabel {@code sekolah.pembayaran_siswa}
 * adalah satu <b>kuitansi/struk</b> atas uang yang benar-benar sudah diterima sekolah dari
 * seorang siswa atau calon siswa. Ini adalah <b>ujung akhir rantai billing sekolah</b> dan
 * sekaligus catatan paling kritis di seluruh modul {@code ais.database.model.sekolah}: bila
 * satu baris di sini salah, hilang, ganda, atau dipalsukan, yang salah adalah <i>uang nyata</i>
 * dan <i>jejak audit keuangan</i>, bukan sekadar tampilan layar.
 *
 * <h2>Posisi dalam rantai billing</h2>
 *
 * <p>Rantai lengkap dari katalog tarif sampai kuitansi:</p>
 *
 * <pre>
 * ItemBiayaSekolah   (katalog jenis biaya: SPP, Seragam, Uang Gedung, ...)
 *   → PengaturanBiaya  (tarif berlaku untuk angkatan/kelas/jurusan/tahun ajaran)
 *     → NominalBiaya    (materialisasi tarif menjadi kewajiban rupiah per siswa/periode)
 *       → Tagihan       (baris tagihan yang menunggu dibayar; punya diskon &amp; denda)
 *         → PembayaranSiswaDetail  (satu tagihan yang DILUNASI oleh satu kuitansi)
 *           → <b>PembayaranSiswa</b>  (KUITANSI — kelas ini; kepala/header transaksi)
 * </pre>
 *
 * <p>Jadi relasi ke tagihan bersifat <b>tak langsung</b>: kelas ini <i>tidak</i> menyimpan koleksi
 * {@code Tagihan}. Perantaranya adalah {@code PembayaranSiswaDetail}, yang menyimpan FK ke
 * {@code PembayaranSiswa} (sisi kepala) dan ke {@code Tagihan} (sisi kewajiban), sementara
 * {@code Tagihan} sendiri menyimpan FK balik {@code pembayaran_siswa_detail_id} sebagai penanda
 * “sudah lunas oleh detail ini”. Method {@link #saveDetail(Rows, Session)} pada kelas inilah
 * yang membuat baris-baris perantara tersebut. Lihat
 * {@code ais.database.model.sekolah.NominalBiaya} dan
 * {@code ais.database.model.sekolah.Tagihan} untuk sisi kewajiban rantai ini.</p>
 *
 * <h2>Subjek ganda: siswa ATAU calon siswa</h2>
 *
 * <p>Satu kuitansi selalu milik <b>salah satu</b> dari dua jenis subjek: {@code siswa}
 * (peserta didik terdaftar) atau {@code calonSiswa} (pendaftar PSB yang belum diterima). Kedua
 * kolom FK nullable, dan layar pencarian membedakannya dengan
 * {@code Restrictions.isNotNull("siswa")} vs {@code isNotNull("calonSiswa")}
 * ({@code PembayaranSiswaAction} punya mode {@code pembayaranCalonSiswa}). Kuitansi calon siswa
 * <b>bermigrasi sendiri</b> menjadi kuitansi siswa begitu pendaftar diterima — lihat efek
 * samping {@link #getSiswa()}.</p>
 *
 * <h2>Cara pembayaran &amp; akun buku besar</h2>
 *
 * <p>{@code akunPembayaranSiswa} ({@code ais.database.model.sekolah.AkunPembayaranSiswa})
 * menentukan <b>“Cara Pembayaran”</b> kuitansi ini (Tunai, Transfer, VA BNI, potong tabungan,
 * dsb.) sekaligus <b>akun buku besar</b> tujuan saat transaksi diposting ke jurnal akuntansi.
 * Kolom ini {@code nullable = false}, tetapi pemanggil sering membiarkannya kosong dan
 * mengandalkan pengisian otomatis di {@link #getAkunPembayaranSiswa()}. Flag
 * {@code AkunPembayaranSiswa.getDariTabungan()} pada master itu ikut menentukan apakah kuitansi
 * dianggap dibayar dari tabungan siswa — lihat peringatan pada {@link #getDariTabungan()}.</p>
 *
 * <h2>Kanal bank / pembayaran daring</h2>
 *
 * <p>Empat FK opsional mencatat <b>dari kanal mana</b> uang datang, dan sekaligus dipakai sebagai
 * kunci idempoten agar callback bank yang dikirim ulang tidak membuat kuitansi ganda:</p>
 *
 * <ul>
 *   <li>{@code briRequest} — {@code ais.database.model.bri.BriRequest} (H2H BRI)</li>
 *   <li>{@code bniRequest} — {@code ais.database.model.bni.BniRequest} (BNI eCollection)</li>
 *   <li>{@code bsiRequest} — {@code ais.database.model.bsi.BsiRequest} (BSI)</li>
 *   <li>{@code virtualAccountBank} — {@code ais.database.model.VirtualAccountBank}, kanal VA
 *       generik yang dipakai belasan servlet bank lain (Mandiri, BCA, BTN, BJBS, Nagari,
 *       OCBC NISP, Otto, Flip, Finpay, Esmartlink, MncBank, BMS, Maja, Bankaltimtara, …)</li>
 * </ul>
 *
 * <p>{@code bankHost} ({@code ais.database.model.BankHost}) mencatat <i>host bank</i> (alamat IP
 * pemanggil) yang memposting pembayaran, dan {@code inquiryPembayaran} menyimpan nomor referensi
 * inquiry dari bank.</p>
 *
 * <h2>Kolom uang</h2>
 *
 * <ul>
 *   <li>{@code nominal} — total rupiah kuitansi. Wajib ({@code nullable = false}).</li>
 *   <li>{@code nominalManual} — nilai yang diketik operator; bila &gt; 0,1 ia <b>menimpa</b>
 *       {@code nominal} setiap kali dibaca ({@link #getNominal()}).</li>
 *   <li>{@code dariTabungan} / {@code dariTabunganManual} — bagian pembayaran yang diambil dari
 *       tabungan/deposit siswa. {@code dariTabungan} adalah <b>sisi DEBIT saldo dompet siswa</b>:
 *       {@code ais.action.master.sekolah.util.DepositHelper} menjumlahkannya
 *       ({@code SUM(dariTabungan) WHERE dariTabungan &gt; 0.1}) sebagai pengeluaran saat menghitung
 *       saldo. Indeks basis data {@code idx_pembayaran_siswa_tabungan_*} dibuat khusus untuk
 *       query ini.</li>
 *   <li>{@code tambahanDeposit} — bagian pembayaran yang <b>disetorkan sebagai topup tabungan</b>;
 *       nilai &gt; 1,0 membuat {@link #saveOrUpdateDeposit(Session)} menerbitkan baris
 *       {@code DepositSiswa} (sisi KREDIT dompet + bahan posting jurnal).</li>
 *   <li>{@code totalDeposit} / {@code sisaDeposit} — snapshot saldo saat transaksi, dipakai
 *       laporan/struk. Keduanya setter/getter polos tanpa perhitungan.</li>
 * </ul>
 *
 * <h2>Periode</h2>
 *
 * <p>{@code bulan} (1–12), {@code tahun}, dan {@code tahunDanBulan} (bilangan gabungan
 * {@code yyyyMM}, mis. {@code 202609}) menandai <b>periode kewajiban</b> yang dilunasi — bukan
 * tanggal transaksi. Ketiganya diisi/dinormalisasi sendiri oleh getter-nya berdasarkan
 * {@code jenisBiayaSekolah.getPeriode()} (Bulanan / Tahunan / Insidentil). Method statis
 * {@link #convert(Integer, Integer)} adalah penyusun {@code yyyyMM} yang dipakai <b>puluhan</b>
 * kelas billing lain, jauh di luar kelas ini.</p>
 *
 * <h2>Siapa memvalidasi</h2>
 *
 * <p>{@code validator} (nama petugas/bank sebagai teks) dan {@code validatorUser} (FK
 * {@code ais.database.model.Tbmuser}) adalah <b>jejak “siapa menerima uang ini”</b>. Untuk
 * pembayaran kanal bank, {@code validator} diisi nilai konfigurasi
 * ({@code default_validator_bni}, {@code default_validator_bri}, …) atau nama bank. Untuk
 * pembayaran tunai, ia diisi nama kasir. <b>Kedua getter dapat memalsukan jejak ini secara
 * senyap</b> — lihat {@link #getValidator()} dan {@link #getValidatorUser()}.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ol>
 *   <li><b>Infrastruktur audit warisan</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Konstruktor</b> — {@link #PembayaranSiswa()} (wajib untuk Hibernate) dan
 *       {@link #PembayaranSiswa(Long)} (pembungkus id untuk kriteria/perbandingan).</li>
 *   <li><b>Logika bisnis nyata</b> — {@link #saveOrUpdateDeposit(Session)},
 *       {@link #chekDetail(Rows)}, {@link #saveDetail(Rows, Session)}. Ketiganya menulis ke basis
 *       data / menghitung uang dan <b>bukan</b> aksesor.</li>
 *   <li><b>Utilitas periode statis</b> — {@link #sekarang()}, {@link #convert(Integer, Integer)}.</li>
 *   <li><b>Aksesor relasi</b> — {@code bankHost}, {@code sekolah}, {@code siswa},
 *       {@code calonSiswa}, {@code yayasan}, {@code jenisBiayaSekolah},
 *       {@code akunPembayaranSiswa}, {@code briRequest}, {@code bniRequest}, {@code bsiRequest},
 *       {@code virtualAccountBank}, {@code validatorUser}.</li>
 *   <li><b>Aksesor kolom skalar</b> — {@code id}, {@code nama}, {@code bulan}, {@code tahun},
 *       {@code tahunDanBulan}, {@code tanggal}, {@code tanggalBayar}, {@code nominal},
 *       {@code nominalManual}, {@code dariTabungan}, {@code dariTabunganManual},
 *       {@code tambahanDeposit}, {@code totalDeposit}, {@code sisaDeposit},
 *       {@code inquiryPembayaran}, {@code validator}, {@code keterangan}.</li>
 * </ol>
 *
 * <h2>Hal-hal non-obvious yang WAJIB diketahui sebelum menyunting</h2>
 *
 * <h3>1. Getter di kelas ini MENULIS, bukan sekadar membaca</h3>
 *
 * <p>Empat belas getter mengubah state objek saat dipanggil. Karena kelas dipetakan dengan
 * <b>akses properti</b> (anotasi JPA menempel pada getter, lihat {@link #getId()}), Hibernate
 * memanggil getter tersebut saat <i>dirty checking</i>, sehingga <b>nilai hasil tulis-balik ikut
 * tersimpan ke basis data</b> pada flush berikutnya — walaupun tidak ada pengguna yang menyunting
 * apa pun. Sekadar <i>merender</i> kuitansi lama di grid sudah cukup untuk mengubah isinya.
 * Daftar getter tulis-balik: {@link #getNama()}, {@link #getSekolah()}, {@link #getSiswa()},
 * {@link #getYayasan()}, {@link #getBulan()}, {@link #getTahun()}, {@link #getTahunDanBulan()},
 * {@link #getTanggal()}, {@link #getTanggalBayar()}, {@link #getNominal()},
 * {@link #getTambahanDeposit()}, {@link #getAkunPembayaranSiswa()}, {@link #getValidator()},
 * {@link #getValidatorUser()}, {@link #getDariTabungan()}. Beberapa di antaranya
 * <b>menghancurkan data lama secara permanen</b>; masing-masing diberi peringatan tersendiri.</p>
 *
 * <h3>2. Setter subjek bersifat TULIS-SEKALI</h3>
 *
 * <p>{@link #setSiswa(Siswa)} dan {@link #setCalonSiswa(CalonSiswa)} <b>diam-diam mengabaikan</b>
 * argumen bila kolomnya sudah terisi (r77034, “enforce safe model mutation contracts”). Ini
 * proteksi yang benar — kuitansi tidak bisa dipindahkan ke siswa lain — tetapi juga berarti
 * kode kalibrasi/koreksi data <b>tidak akan bekerja</b> lewat setter dan gagal tanpa pesan.</p>
 *
 * <h3>3. Kolom tenant diturunkan, bukan disimpan bebas</h3>
 *
 * <p>{@link #getSekolah()} selalu menimpa {@code sekolah} dari siswa/calon siswa, dan
 * {@link #getYayasan()} selalu menimpa {@code yayasan} dari sekolah. Efeknya bagus (tenant tidak
 * bisa dipalsukan lewat form) tetapi berarti {@link #setSekolah(Sekolah)} praktis tanpa efek
 * untuk baris yang punya subjek.</p>
 *
 * <h3>4. Enam kolom dipetakan dengan nama camelCase apa adanya</h3>
 *
 * <p>{@code nama}, {@code validator}, {@code keterangan}, {@code dariTabungan},
 * {@code dariTabunganManual}, dan {@code nominalManual} tidak diberi {@code @Column}, sehingga
 * memakai nama properti apa adanya. Karena {@code MyNamingStrategy} tidak mengubah camelCase dan
 * PostgreSQL melipat identifier tanpa kutip menjadi huruf kecil, kolom fisiknya bernama
 * {@code daritabungan}, {@code daritabunganmanual}, dan {@code nominalmanual} — lihat definisi
 * indeks di {@code ais.common.InitIndex}. Jangan berasumsi {@code dari_tabungan}.</p>
 *
 * <h3>5. Anotasi &amp; audit</h3>
 *
 * <p>{@code @Audited} (Hibernate Envers) mencatat setiap revisi ke {@code new_audit.pembayaran_siswa__audit}.
 * <b>Peringatan penting:</b> jejak audit itu <b>buta</b> terhadap empat titik penghapusan yang
 * memakai SQL native langsung — tiga di servlet callback bank
 * ({@code delete from sekolah.pembayaran_siswa where bni_request_id = …}, idem BRI/BSI) dan satu
 * di tombol pembatalan unggah massal {@code PembayaranSiswaAction} yang menghapus
 * <b>seluruh instalasi</b> ({@code … where id not in (select pembayaran_siswa_id from
 * sekolah.pembayaran_siswa_detail …)}). Kuitansi topup tabungan murni tidak punya baris
 * {@code PembayaranSiswaDetail}, sehingga persis kuitansi itulah yang ikut terhapus.
 * {@code dynamicInsert}/{@code dynamicUpdate} aktif agar SQL hanya memuat kolom yang berubah.</p>
 *
 * <h3>6. Induk {@code GeneralValueObject} BUKAN kelas terpetakan</h3>
 *
 * <p>{@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa — bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti
 * apa pun miliknya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di kelas ini. Pengulangan tersebut
 * <b>bukan bug dan jangan “dibersihkan”</b>: menghapusnya menghilangkan kolomnya dari tabel.
 * Yang tetap diwarisi hanyalah <i>perilaku</i> statis, terutama {@code check(…)} untuk resolusi
 * proxy lazy yang dipakai hampir semua getter relasi di sini.</p>
 *
 * <h2>Siapa yang membuat baris di sini</h2>
 *
 * <ol>
 *   <li>{@code ais.action.master.sekolah.PembayaranSiswaAction} — kasir manual (layar
 *       {@code /pages/master/sekolah/pembayaran_siswa.zul}, menu id {@code 87657728}).</li>
 *   <li>{@code ais.common.TunaiSiswaCommon} — jalur pembayaran tunai.</li>
 *   <li>{@code ais.database.model.VirtualAccountBank#bayarSiswa} dan {@code #bayarSiswaLangsung}
 *       — kanal VA generik dan pembayaran dari saldo tabungan.</li>
 *   <li>{@code ais.action.servlet.Bniresponse} / {@code Briresponse} / {@code Bsiresponse} —
 *       callback bank H2H.</li>
 *   <li>{@code ais.action.servlet.api.TagihanSiswa} — API pembayaran/deposit untuk aplikasi
 *       seluler.</li>
 *   <li>{@code ais.action.master.sekolah.helper.PembayaranOnline} — layar pembayaran daring
 *       siswa/orang tua.</li>
 * </ol>
 *
 * <h2>CATATAN KEAMANAN — dibaca sebelum menyentuh apa pun di sini</h2>
 *
 * <p>Temuan berikut dicatat apa adanya dari pembacaan kode pemanggil; tidak satu pun disimpulkan
 * dari asumsi. Semuanya menyangkut <b>uang nyata</b>, sehingga tingkat kepentingannya sangat
 * tinggi.</p>
 *
 * <ol>
 *   <li><b>Seluruh servlet bank dapat diakses tanpa login.</b>
 *       {@code WEB-INF/applicationContext-security.xml} berakhir dengan aturan tangkap-semua
 *       {@code <intercept-url pattern="/**" access="IS_AUTHENTICATED_ANONYMOUSLY" />}, dan
 *       {@code web.xml} hanya memiliki satu {@code security-constraint} (untuk berkas
 *       {@code *.sh}/{@code *.sql}). Akibatnya {@code /Va}, {@code /MncBank}, {@code /Mandiri},
 *       {@code /BCA}, {@code /Struk}, dan kerabatnya <b>tidak digerbangi sama sekali</b> di lapis
 *       kerangka kerja. {@code /Va} memanggil {@code VirtualAccountBank.bayarSiswa(…, inquery =
 *       false, …)} yang <b>membuat baris PembayaranSiswa + PembayaranSiswaDetail + DepositSiswa
 *       dan menandai Tagihan lunas</b> — jadi entity ini memang ditulis oleh pemanggil anonim.</li>
 *   <li><b>Pemeriksaan “host bank” bukan gerbang, melainkan pendaftaran otomatis.</b>
 *       {@code PembayaranUtil.getBankHost(ip, nama)} tidak pernah menolak: bila IP pemanggil tidak
 *       dikenal <i>dan</i> konfigurasi
 *       {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} aktif, ia
 *       <b>membuat baris {@code BankHost} baru untuk IP tersebut</b>; bila tidak, ia jatuh ke baris
 *       wildcard {@code 0.0.0.0}. Nilai bawaan konfigurasi itu adalah
 *       {@code Konfigurasi.AKTIF} ({@code Common.bolehKonfigurasi(String)} satu argumen), sehingga
 *       pada instalasi bawaan <b>IP mana pun dapat mendaftarkan dirinya sendiri sebagai bank</b>.
 *       Kolom {@code bankHost} pada kuitansi ini karena itu bukan bukti identitas.</li>
 *   <li><b>{@code /Struk?id=N} membocorkan kuitansi siapa pun tanpa login.</b>
 *       {@code ais.action.servlet.Struk} mengambil id dari parameter, memuat
 *       {@code PembayaranSiswa} langsung dengan {@code Restrictions.idEq}, <b>tanpa pemeriksaan
 *       login maupun kepemilikan</b>, lalu mengirim PDF struk lengkap. Bentuk terenkripsi
 *       (awalan {@code EE}) hanya opsional — id numerik polos tetap diterima, dan id bersifat
 *       berurutan. Struk memuat nama siswa, NIS, sekolah, rincian item biaya, nominal, dan
 *       seluruh properti {@code Siswa}/{@code CalonSiswa} yang disuntikkan
 *       {@code Common.insertProperty}.</li>
 *   <li><b>Fail-open cakupan tenant di layar pengelola.</b> Pada
 *       {@code PembayaranSiswaAction.initSubCriteria(…)}, filter sekolah dan yayasan hanya aktif
 *       bila pengguna memilih nilai di combo; bawaannya {@code Restrictions.sqlRestriction("1=1")}.
 *       Akun staf dengan hak BACA menu ini melihat kuitansi <b>seluruh yayasan dan seluruh sekolah
 *       satu instalasi</b>. Pembatas anak untuk akun wali murid juga fail-open:
 *       {@code anak.isEmpty() ? sqlRestriction("true") : in("siswa.id", anak)} — wali murid yang
 *       relasi anaknya belum terisi melihat <b>semua</b> kuitansi, bukan nol.</li>
 *   <li><b>Pewarisan hak lewat menu induk (instance baru).</b>
 *       {@code PembayaranOnline} — yang <b>nol</b> {@code checkPrevilages} pada 4.366 baris —
 *       menyisipkan sepuluh layar bergerbang sebagai tab, termasuk
 *       {@code /pages/master/sekolah/pembayaran_siswa.zul}, {@code pembayaran_calon_siswa.zul},
 *       {@code bri_request.zul}, {@code bni_request.zul}, {@code bsi_request.zul},
 *       {@code virtual_account_bank.zul}, {@code tagihan.zul}, dan {@code deposit.zul}. Hak
 *       add/edit/delete yang berlaku di dalam tab adalah hak menu <i>Pembayaran Online</i>, bukan
 *       hak menu <i>Pembayaran Siswa</i>.</li>
 *   <li><b>Yang sudah benar</b> (dicatat agar tidak “diperbaiki” menjadi rusak):
 *       {@code PembayaranSiswaAction} memang memanggil {@code CommonPrivilages.checkPrevilages}
 *       untuk CREATE/UPDATE/DELETE/APPROVE; tombol Hapus baris digerbangi {@code delete}; tombol
 *       unggah massal digerbangi {@code (add.isVisible()) && edit && delete}; dan akun siswa
 *       maupun calon siswa dipaksa hanya melihat kuitansinya sendiri
 *       ({@code Restrictions.eq("siswa", tbmuser.getSiswa())}) sehingga parameter URL
 *       {@code ?siswa=} tidak dapat dipakai akun siswa untuk mengintip siswa lain.</li>
 * </ol>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.PembayaranSiswaDetail
 * @see ais.database.model.sekolah.Tagihan
 * @see ais.database.model.sekolah.NominalBiaya
 * @see ais.database.model.sekolah.AkunPembayaranSiswa
 * @see ais.database.model.sekolah.DepositSiswa
 * @see ais.database.model.VirtualAccountBank
 * @see ais.action.master.sekolah.PembayaranSiswaAction
 * @see ais.action.master.sekolah.util.PembayaranSiswaUtil
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "pembayaran_siswa", schema = "sekolah")
public class PembayaranSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas. Nilainya <b>tidak boleh diubah</b> selama struktur logis entity
	 * tetap kompatibel: objek {@code PembayaranSiswa} disimpan di sesi ZK dan dipertukarkan antar
	 * node, sehingga perubahan nilai ini membuat sesi lama gagal dideserialisasi.
	 */
	private static final long serialVersionUID = -4008239631951156828L;
	/**
	 * Kunci utama tabel {@code sekolah.pembayaran_siswa}. Dideklarasikan ulang di sini karena
	 * {@link ais.database.model.GeneralValueObject} bukan kelas terpetakan — lihat catatan pada
	 * Javadoc kelas. Dipetakan pada {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Berbeda peran dari {@code validator}: {@code oleh}
	 * menjawab “siapa terakhir menyentuh baris”, {@code validator} menjawab “siapa menerima
	 * uangnya”.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini (pendamping numerik/tekstual dari
	 * {@code oleh}), diisi otomatis oleh {@code AuditTimestampInterceptor}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>pengaman anti-penghapusan jejak</b>:
	 * argumen {@code null} atau kosong <b>diabaikan</b> sehingga nilai lama tetap bertahan.
	 * Ini sengaja — mencegah proses batch/impor yang tidak membawa identitas pengguna menghapus
	 * jejak audit yang sudah ada.
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa pesan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * argumen {@code null} atau kosong <b>diabaikan</b> agar jejak lama tidak terhapus.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa pesan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} dieksekusi,
	 * dan mendelegasikan pengisian {@code oleh}/{@code olehId}/{@code tanggal_dirubah} ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Perhatikan: kait ini <b>hanya</b> berjalan untuk {@code UPDATE} lewat Hibernate. Empat
	 * titik penghapusan SQL native pada tabel ini (lihat Javadoc kelas) melewatinya sepenuhnya,
	 * begitu pula operasi massal berbasis SQL.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir baris ini. Diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()}, bukan {@code new Date()}, agar mengikuti zona waktu dan
	 * pergeseran waktu yang dikonfigurasi aplikasi) lalu diperbarui oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir. Umumnya <b>tidak</b> dipanggil kode aplikasi:
	 * pengisiannya diserahkan ke {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir (presisi {@code TIMESTAMP})
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Label kuitansi yang sudah didenormalisasi (“NIS-Nama-JenisBiaya-yyyyMM-Rp”), dibentuk ulang
	 * setiap kali {@link #getNama()} dipanggil dan ikut tersimpan ke kolom {@code nama}.
	 */
	private String nama;

	// ---------------------------------------------------------------------------------------
	// Relasi & kolom inti kuitansi.
	//
	// Catatan: bankHost/briRequest/bniRequest/bsiRequest/virtualAccountBank hanyalah PENANDA
	// KANAL asal uang; hanya salah satu yang terisi per baris (atau tidak sama sekali untuk
	// pembayaran tunai di loket). sekolah & yayasan bukan data bebas — keduanya diturunkan
	// ulang dari siswa/calonSiswa pada setiap pembacaan getter-nya.
	// ---------------------------------------------------------------------------------------
	private BankHost bankHost;
	private JenisBiayaSekolah jenisBiayaSekolah;
	private AkunPembayaranSiswa akunPembayaranSiswa;
	private Sekolah sekolah;
	private Siswa siswa;
	private CalonSiswa calonSiswa;
	private Yayasan yayasan;
	private Integer bulan;
	private String inquiryPembayaran;
	private Double nominal;
	private Double nominalManual;
	private Double dariTabungan;
	private Double dariTabunganManual;
	private Double sisaDeposit;
	private Integer tahun;
	private Integer tahunDanBulan;
	private Date tanggal;
	private Date tanggalBayar;
	private Double tambahanDeposit;
	private Double totalDeposit;

	// ---------------------------------------------------------------------------------------
	// Kanal bank tambahan + jejak validator + keterangan bebas.
	// ---------------------------------------------------------------------------------------
	private BriRequest briRequest;
	private BniRequest bniRequest;
	private BsiRequest bsiRequest;
	private VirtualAccountBank virtualAccountBank;
	private String validator;
	private Tbmuser validatorUser;
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen. <b>Wajib ada</b> untuk Hibernate (instansiasi via refleksi) dan
	 * untuk pola “baris baru” di layar ZK. Seluruh kolom dibiarkan {@code null}; nilai
	 * bawaannya justru dihasilkan belakangan oleh getter tulis-balik (mis. {@link #getTanggal()}
	 * mengembalikan waktu sekarang bila {@code tanggal} masih {@code null}).
	 */
	public PembayaranSiswa() {
	}

	/**
	 * Konstruktor pembungkus id. Membentuk instance yang <b>hanya</b> membawa kunci utama, untuk
	 * dipakai sebagai acuan pada kriteria Hibernate atau perbandingan tanpa perlu memuat seluruh
	 * baris dari basis data.
	 *
	 * <p><b>Awas:</b> instance hasil konstruktor ini <i>bukan</i> objek terkelola — seluruh kolom
	 * lain {@code null}. Jangan menyimpannya dengan {@code session.save()}: itu akan membuat
	 * kuitansi kosong bernominal 0.</p>
	 *
	 * @param id kunci utama kuitansi yang diacu
	 */
	public PembayaranSiswa(Long id) {
		this.id = id;

	}

	/**
	 * Menerbitkan (atau memperbarui) baris {@code DepositSiswa} sebagai <b>setoran tabungan</b>
	 * yang menyertai kuitansi ini.
	 *
	 * <p>Dipanggil tepat setelah kuitansi tersimpan, dari <b>empat</b> jalur berbeda:
	 * {@code PembayaranSiswaAction} (kasir manual), {@code Bniresponse}, {@code Briresponse},
	 * {@code Bsiresponse} (callback bank), serta dua kali dari
	 * {@code VirtualAccountBank} ({@code bayarSiswa} dan {@code bayarSiswaLangsung}).</p>
	 *
	 * <p>Syarat penerbitan: {@link #getTambahanDeposit()} tidak {@code null} <b>dan</b> lebih dari
	 * 1,0 <b>dan</b> {@link #getSiswa()} tidak {@code null} (jadi calon siswa murni tidak pernah
	 * mendapat baris deposit dari sini). Baris deposit dicari lebih dulu berdasarkan FK
	 * {@code pembayaranSiswa} agar pemanggilan ulang memperbarui baris yang sama alih-alih
	 * menggandakannya — <b>ini satu-satunya pengaman idempoten</b> pada jalur ini.</p>
	 *
	 * <p>Nilai yang disalin ke {@code DepositSiswa}: akun pembayaran, yayasan, sekolah, siswa,
	 * {@code tanggalBayar}, dan {@code tanggal} (sebagai {@code waktu}); {@code inquiryPembayaran}
	 * dipatok konstan {@code "000000"}; {@code nominal} diisi {@link #getTambahanDeposit()}.</p>
	 *
	 * <p><b>PERINGATAN FINANSIAL — setoran hantu.</b> Ambang syaratnya memakai
	 * {@link #getTambahanDeposit()}, dan getter itu <b>menimpa</b> nilai 0 dengan
	 * {@link #getNominal()} (lihat peringatannya). Akibatnya pemanggil yang <i>sengaja</i>
	 * menyetel {@code tambahanDeposit} ke {@code 0.0} agar tidak ada setoran — persis yang
	 * dilakukan {@code VirtualAccountBank.bayarSiswaLangsung()}, lengkap dengan komentar
	 * “bayar via deposit: tambahanDeposit = 0 agar tidak membuat record DepositSiswa baru” —
	 * tetap menghasilkan {@code DepositSiswa} sebesar <b>seluruh nominal kuitansi</b>. Untuk
	 * pembayaran yang justru <i>memotong</i> tabungan, baris kredit palsu itu menjadi bahan
	 * posting jurnal {@code PostingDepositSiswaAction} sehingga kas/liabilitas sekolah ikut
	 * menggelembung. Perilaku ini dicatat apa adanya; memperbaikinya mengubah angka akuntansi
	 * berjalan dan harus dilakukan sebagai pekerjaan tersendiri.</p>
	 *
	 * <p><b>Efek samping:</b> menulis (menyimpan) satu baris {@code DepositSiswa} ke basis data.
	 * Method ini <b>tidak</b> membuka/menutup transaksi sendiri — pemanggil wajib menyediakan
	 * batas transaksi.</p>
	 *
	 * @param session session Hibernate aktif milik pemanggil; dipakai untuk mencari baris deposit
	 *                yang sudah ada dan untuk menyimpannya
	 */
	public void saveOrUpdateDeposit(Session session) {
		if (getTambahanDeposit() != null && getTambahanDeposit() > 1.0 && getSiswa() != null) {
			DepositSiswa depositSiswa = (DepositSiswa) session.createCriteria(DepositSiswa.class)
					.add(Restrictions.eq("pembayaranSiswa", this)).setMaxResults(1).uniqueResult();
			if (depositSiswa == null) {
				depositSiswa = new DepositSiswa();
			}
			depositSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
			depositSiswa.setPembayaranSiswa(this);
			depositSiswa.setYayasan(yayasan);
			depositSiswa.setSekolah(sekolah);
			depositSiswa.setInquiryPembayaran("000000");
			depositSiswa.setNominal(getTambahanDeposit());
			depositSiswa.setSiswa(siswa);
			depositSiswa.setTanggalBayar(tanggalBayar);
			depositSiswa.setWaktu(tanggal);
			session.save(depositSiswa);
		}
	}

	/**
	 * Menghitung <b>grand total</b> rupiah dari baris-baris tagihan yang dicentang pada grid
	 * rincian biaya, <b>tanpa menyimpan apa pun</b>. Ini adalah pasangan “hanya-hitung” dari
	 * {@link #saveDetail(Rows, Session)}: rumus keduanya sengaja identik agar angka yang
	 * dikonfirmasi ke pengguna sama persis dengan angka yang nanti disimpan.
	 *
	 * <p>Untuk setiap {@code Row} anak dari {@code rowsDetailBiaya}, method membaca dua atribut
	 * ZK yang dipasang oleh renderer layar: {@code "pilih"} (sebuah {@code MyCheckboxConfig}) dan
	 * {@code "tagihan"} (sebuah {@code Tagihan}). Baris yang tidak mempunyai keduanya dilewati.
	 * Bila checkbox tercentang, nominal baris dihitung sebagai:</p>
	 *
	 * <pre>
	 * dasar  = itemBiayaSekolah.getNilaiBiayaBisaDiubahSaatPembayaran()
	 *            ? nilai Doublebox atribut "nominal" (0.0 bila kosong)
	 *            : tagihan.getNominal()
	 * hasil  = dasar - tagihan.getDiskon() + tagihan.getDenda()
	 * </pre>
	 *
	 * <p>Jadi untuk item biaya yang ditandai “nilainya boleh diubah saat pembayaran”, angka yang
	 * dipakai adalah <b>angka yang diketik operator di layar</b>, bukan nilai tagihan — titik
	 * masuk paling langsung untuk pembayaran sebagian maupun untuk salah ketik.</p>
	 *
	 * <p><b>Ketahanan:</b> seluruh isi loop dibungkus {@code try/catch} per baris. Baris yang
	 * gagal (mis. {@code nominalBiaya} {@code null}) hanya dicatat lewat
	 * {@code ErrorAuditUtil.record} dan <b>dilewati diam-diam</b> — total yang dikembalikan bisa
	 * lebih kecil dari yang dilihat pengguna tanpa peringatan apa pun. Method juga mencetak
	 * rincian tiap item ke {@code stdout}.</p>
	 *
	 * <p>Pemanggil: {@code ais.common.TunaiSiswaCommon} memakainya untuk memutuskan apakah
	 * transaksi tunai layak diproses (total harus &gt; 0).</p>
	 *
	 * @param rowsDetailBiaya komponen {@code Rows} ZK berisi baris-baris tagihan beratribut
	 *                        {@code "pilih"}, {@code "tagihan"}, dan {@code "nominal"}
	 * @return jumlah rupiah seluruh baris tercentang setelah diskon dan denda; {@code 0.0} bila
	 *         tidak ada yang tercentang
	 */
	@SuppressWarnings("unchecked")
	public static Double chekDetail(Rows rowsDetailBiaya) {

		Double grandTotal = 0.0;
		List<Row> rows = rowsDetailBiaya.getChildren();
		for (Object o : rows) {
			try {
				if (o instanceof Row) {
					Row row = (Row) o;

					if (row.getAttribute("pilih") != null && row.getAttribute("tagihan") != null) {
						MyCheckboxConfig checkboxConfig = (MyCheckboxConfig) row.getAttribute("pilih");
						Tagihan tagihan = (Tagihan) row.getAttribute("tagihan");
//						System.out.println("chekDetail pilih -> " + checkboxConfig.isChecked() + " tagihan " + tagihan);

						if (checkboxConfig != null && checkboxConfig.isChecked()) {

							NominalBiaya nominalBiaya = tagihan.getNominalBiaya();

							Doublebox n = (Doublebox) row.getAttribute("nominal");

							Double nominal = nominalBiaya.getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran()
									? (n == null ? 0.0 : n.getValue())
									: tagihan.getNominal();

							nominal = nominal - tagihan.getDiskon();

							nominal += tagihan.getDenda();

							System.out.println("chekDetail item => " + nominalBiaya.getItemBiayaSekolah().getNama()
									+ " nominal => " + nominal);

							grandTotal += nominal;

						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/PembayaranSiswa.java:196");
			}
		}
		return grandTotal;
	}

	/**
	 * <b>Melunasi</b> tagihan-tagihan yang dicentang di grid rincian biaya: membuat satu
	 * {@code PembayaranSiswaDetail} per tagihan, mengaitkannya ke kuitansi ini, lalu memperbarui
	 * tagihan yang bersangkutan menjadi terbayar. Inilah method yang <b>benar-benar mengubah
	 * status utang siswa</b>.
	 *
	 * <p>Rumus nominal per baris identik dengan {@link #chekDetail(Rows)} (lihat penjelasan rinci
	 * di sana): nilai dasar diambil dari {@code Doublebox} layar untuk item biaya yang boleh
	 * diubah saat pembayaran, atau dari {@code tagihan.getNominal()} bila tidak, lalu dikurangi
	 * diskon dan ditambah denda.</p>
	 *
	 * <p>Untuk setiap baris tercentang, urutan kerjanya:</p>
	 * <ol>
	 *   <li>membuat {@code PembayaranSiswaDetail} baru dari {@code Tagihan} terkait, mengisi
	 *       {@code itemBiayaSekolah}, {@code nominalBiaya}, {@code nominal}, {@code nominalManual},
	 *       {@code tagihan}, dan {@code pembayaranSiswa} (= objek ini);</li>
	 *   <li>{@code session.save(…)} disusul {@code session.flush()} agar id detail langsung
	 *       tersedia;</li>
	 *   <li>{@code pembayaranSiswaDetail.updateTagihan(tagihan, nominal, session)} — di sinilah
	 *       {@code Tagihan} ditandai lunas dengan FK balik ke detail ini;</li>
	 *   <li>bila tagihan punya diskon siswa yang <i>tidak</i> memotong tagihan
	 *       ({@code diskonSiswa.getMemotongTagihan() == false}), memanggil
	 *       {@code DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan)} untuk menerbitkan
	 *       pengajuan transfer/klaim beasiswa — <b>efek samping lintas modul (akunting)</b>.</li>
	 * </ol>
	 *
	 * <p>Setelah loop selesai, {@code populatePembayaran()} dipanggil pada {@code calonSiswa}
	 * dan/atau {@code siswa} untuk menyegarkan ringkasan pembayaran yang di-cache pada entity
	 * subjek.</p>
	 *
	 * <p><b>Kuirk penting:</b> total baris dijumlahkan ke variabel lokal {@code grandTotal},
	 * namun nilai itu <b>tidak pernah dipakai</b> — {@code nominal} kuitansi harus sudah disetel
	 * pemanggil (biasanya dari {@link #chekDetail(Rows)}). Bila pemanggil menyetel nominal yang
	 * berbeda dari jumlah detailnya, tidak ada satu pun pemeriksaan di sini yang menolaknya:
	 * kepala dan rincian kuitansi bisa <b>tidak berimbang</b> secara permanen.</p>
	 *
	 * <p><b>Ketahanan:</b> seperti {@link #chekDetail(Rows)}, kegagalan per baris ditelan
	 * ({@code Common.tampilErrorJikaAdmin}) dan proses lanjut ke baris berikutnya — sebagian
	 * tagihan bisa gagal lunas tanpa pesan ke kasir, sementara uangnya sudah tercatat masuk.</p>
	 *
	 * <p><b>Efek samping:</b> menyimpan {@code PembayaranSiswaDetail}, memperbarui {@code Tagihan},
	 * dan berpotensi menerbitkan {@code DaftarPengajuanTransfer}. Tidak mengelola transaksi
	 * sendiri.</p>
	 *
	 * <p>Pemanggil: {@code PembayaranSiswaAction.save(…)} dan {@code TunaiSiswaCommon}.</p>
	 *
	 * @param rowsDetailBiaya komponen {@code Rows} ZK berisi baris tagihan beratribut
	 *                        {@code "pilih"}, {@code "tagihan"}, dan {@code "nominal"}
	 * @param session         session Hibernate aktif milik pemanggil
	 */
	@SuppressWarnings("unchecked")
	public void saveDetail(Rows rowsDetailBiaya, Session session) {

		Double grandTotal = 0.0;
		List<Row> rows = rowsDetailBiaya.getChildren();
		for (Object o : rows) {
			try {
				if (o instanceof Row) {
					Row row = (Row) o;

					if (row.getAttribute("pilih") != null && row.getAttribute("tagihan") != null) {
						MyCheckboxConfig checkboxConfig = (MyCheckboxConfig) row.getAttribute("pilih");
						Tagihan tagihan = (Tagihan) row.getAttribute("tagihan");
						System.out.println("pilih -> " + checkboxConfig.isChecked() + " tagihan " + tagihan);

						if (checkboxConfig != null && checkboxConfig.isChecked()) {

							NominalBiaya nominalBiaya = tagihan.getNominalBiaya();

							Doublebox n = (Doublebox) row.getAttribute("nominal");

							Double nominal = nominalBiaya.getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran()
									? (n == null ? 0.0 : n.getValue())
									: tagihan.getNominal();

							nominal = nominal - tagihan.getDiskon();
							nominal += tagihan.getDenda();

							System.out.println("item => " + nominalBiaya.getItemBiayaSekolah().getNama()
									+ " nominal => " + nominal + " tagihan -> " + tagihan);

							PembayaranSiswaDetail pembayaranSiswaDetail = new PembayaranSiswaDetail(tagihan);
							pembayaranSiswaDetail.setItemBiayaSekolah(nominalBiaya.getItemBiayaSekolah());
							pembayaranSiswaDetail.setNominalBiaya(nominalBiaya);
							pembayaranSiswaDetail.setNominal(nominal);
							pembayaranSiswaDetail.setNominalManual(nominal);
							pembayaranSiswaDetail.setTagihan(tagihan);
							pembayaranSiswaDetail.setPembayaranSiswa(this);
							session.save(pembayaranSiswaDetail);
							session.flush();

							System.out.println("pembayaranSiswaDetail -> " + pembayaranSiswaDetail);

							pembayaranSiswaDetail.updateTagihan(tagihan, nominal, session);

							grandTotal += pembayaranSiswaDetail.getNominal();

							if (tagihan.getDiskonSiswa() != null && !tagihan.getDiskonSiswa().getMemotongTagihan()) {
								DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
							}

						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (getCalonSiswa() != null && getCalonSiswa().getId() != null) {
			getCalonSiswa().populatePembayaran();
		}
		if (getSiswa() != null && getSiswa().getId() != null) {
			getSiswa().populatePembayaran();
		}
	}

	/**
	 * Mengembalikan kunci utama kuitansi. Anotasi JPA pada getter inilah yang menetapkan
	 * <b>akses properti</b> untuk seluruh kelas — itulah sebabnya getter tulis-balik di sini
	 * berdampak sampai ke basis data.
	 *
	 * <p>Id dihasilkan basis data ({@code IDENTITY}) dan kolomnya {@code insertable = false},
	 * jadi nilai yang disetel manual lewat {@link #setId(Long)} tidak pernah ikut pada
	 * {@code INSERT}. Karena berurutan, id ini <b>dapat ditebak</b> — relevan untuk
	 * {@code /Struk?id=N} yang disebut pada Javadoc kelas.</p>
	 *
	 * @return kunci utama; {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipakai Hibernate saat memuat baris; kode aplikasi umumnya tidak
	 * perlu memanggilnya.
	 *
	 * @param id kunci utama kuitansi
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Membentuk dan mengembalikan <b>label kuitansi</b> berformat
	 * {@code "<NIS>-<Nama>-<JenisBiaya>-<yyyyMM>-<Rp>"}, mis.
	 * {@code "12345-Budi Santoso-SPP-202609-1.500.000"}.
	 *
	 * <p><b>Getter tulis-balik.</b> Method ini menimpa field {@code nama}, {@code siswa}, dan
	 * {@code calonSiswa} setiap kali dipanggil, dan karena {@code nama} adalah kolom terpetakan
	 * (tanpa {@code @Column}, sehingga bernama {@code nama} apa adanya), label baru <b>ikut
	 * tersimpan</b> pada flush berikutnya. Label historis karena itu tidak stabil: bila nama
	 * siswa, jenis biaya, atau nominal berubah, label pada kuitansi lama ikut berubah saat
	 * kuitansi itu sekadar dirender.</p>
	 *
	 * <p>Bila kuitansi tidak punya subjek sama sekali ({@code siswa} dan {@code calonSiswa}
	 * keduanya {@code null}), nilai {@code nama} yang tersimpan dikembalikan apa adanya tanpa
	 * dibentuk ulang. {@code jenisBiayaSekolah} yang kosong dirender sebagai teks
	 * {@code "Semua"}.</p>
	 *
	 * <p>Perhatikan bahwa method ini memanggil {@link #getSiswa()}, {@link #getCalonSiswa()},
	 * {@link #getTahunDanBulan()}, dan {@link #getNominal()} — keempatnya sendiri getter
	 * tulis-balik, sehingga satu pemanggilan {@code getNama()} dapat mengubah hingga tujuh field
	 * sekaligus.</p>
	 *
	 * @return label kuitansi yang sudah dibentuk
	 */
	public String getNama() {
		siswa = getSiswa();
		calonSiswa = getCalonSiswa();

		if (siswa == null && calonSiswa == null) return nama;
		nama = (siswa == null ? (calonSiswa.getNomorInduk() + "-" + calonSiswa.getNamaSiswa())
				: (siswa.getNomorInduk() + "-" + siswa.getNamaSiswa())) + "-"
				+ (getJenisBiayaSekolah() == null ? "Semua" : getJenisBiayaSekolah().getNama()) + "-"
				+ getTahunDanBulan() + "-" + Common.numberFormat.get().format(getNominal());
		return nama;
	}

	/**
	 * Menyetel label kuitansi. Nilai apa pun yang disetel di sini akan <b>dibentuk ulang</b> oleh
	 * {@link #getNama()} pada pembacaan berikutnya selama kuitansi punya subjek.
	 *
	 * @param nama label kuitansi
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan host bank (alamat IP pemanggil H2H) yang memposting pembayaran ini, setelah
	 * proxy lazy-nya diresolusi dengan {@code check(…)}.
	 *
	 * <p><b>Bukan bukti identitas.</b> Nilainya berasal dari
	 * {@code PembayaranUtil.getBankHost(remoteAddr, nama)} yang tidak pernah menolak IP tak
	 * dikenal: bawaannya justru <i>membuat</i> baris {@code BankHost} baru untuk IP tersebut, dan
	 * bila pembuatan otomatis dimatikan ia jatuh ke baris wildcard {@code 0.0.0.0}. Lihat catatan
	 * keamanan pada Javadoc kelas.</p>
	 *
	 * @return host bank pemosting; {@code null} untuk pembayaran non-H2H (mis. tunai di loket)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_host_id", nullable = true)
	public BankHost getBankHost() {
		bankHost = check(bankHost);
		return bankHost;
	}

	/**
	 * Menyetel host bank pemosting pembayaran.
	 *
	 * @param bankHost host bank; boleh {@code null}
	 */
	public void setBankHost(BankHost bankHost) {
		this.bankHost = bankHost;
	}

	/**
	 * Mengembalikan sekolah pemilik kuitansi ini.
	 *
	 * <p><b>Getter tulis-balik — kolom tenant diturunkan, bukan disimpan bebas.</b> Sebelum
	 * mengembalikan nilai, method ini <b>selalu menimpa</b> {@code sekolah} dengan sekolah milik
	 * {@code calonSiswa} lalu (bila ada) milik {@code siswa}. Urutannya penting: {@code siswa}
	 * dievaluasi terakhir sehingga menang atas {@code calonSiswa}.</p>
	 *
	 * <p>Efeknya secara keamanan justru <b>positif</b>: kolom tenant kuitansi tidak dapat
	 * dipalsukan lewat form, karena selalu mengikuti sekolah subjeknya. Konsekuensinya,
	 * {@link #setSekolah(Sekolah)} praktis tanpa efek untuk baris yang punya subjek, dan sekolah
	 * kuitansi <b>ikut berpindah</b> bila siswa dimutasikan antar sekolah — termasuk kuitansi
	 * lama yang secara historis milik sekolah asal.</p>
	 *
	 * @return sekolah pemilik kuitansi; {@code null} hanya bila kuitansi belum punya subjek
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		calonSiswa = getCalonSiswa();
		siswa = getSiswa();
		if (calonSiswa != null) {
			sekolah = calonSiswa.getSekolah();
		}
		if (siswa != null) {
			sekolah = siswa.getSekolah();
		}
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik kuitansi, dengan normalisasi: {@code null} maupun objek
	 * “kosong” (id-nya {@code null}, mis. pilihan “Semua” di combo) disimpan sebagai {@code null}
	 * agar tidak menimbulkan pelanggaran kunci asing.
	 *
	 * <p>Nilai yang disetel di sini akan <b>ditimpa</b> oleh {@link #getSekolah()} selama kuitansi
	 * punya siswa atau calon siswa.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id disimpan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan siswa pemilik kuitansi.
	 *
	 * <p><b>Getter tulis-balik — migrasi calon siswa → siswa.</b> Bila {@code siswa} masih kosong
	 * (atau id-nya {@code null}) tetapi kuitansi punya {@code calonSiswa} yang <i>sudah</i>
	 * dikonversi menjadi siswa ({@code calonSiswa.getSiswa() != null}), field {@code siswa} diisi
	 * dari sana — <b>langsung ke field, sehingga pengaman tulis-sekali pada
	 * {@link #setSiswa(Siswa)} tidak berlaku</b>. Akibatnya kuitansi yang dulu diterbitkan atas
	 * nama pendaftar PSB otomatis “berpindah” menjadi kuitansi siswa terdaftar begitu pendaftar
	 * itu diterima, dan perpindahan itu ikut tersimpan pada flush berikutnya.</p>
	 *
	 * <p>Konsekuensi yang perlu diingat: kuitansi tersebut lalu ikut terjaring filter
	 * {@code isNotNull("siswa")} pada layar Pembayaran Siswa (bukan lagi hanya layar Pembayaran
	 * Calon Siswa), dan ikut dijumlahkan pada perhitungan saldo tabungan siswa yang bersangkutan.</p>
	 *
	 * @return siswa pemilik kuitansi; {@code null} bila kuitansi murni milik calon siswa yang
	 *         belum dikonversi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		if (siswa == null || siswa.getId() == null) {
			calonSiswa = getCalonSiswa();
			if (calonSiswa != null && calonSiswa.getSiswa() != null) {
				siswa = calonSiswa.getSiswa();
			}
		}
		return this.siswa;
	}

	/**
	 * Menyetel siswa pemilik kuitansi, dengan <b>kontrak tulis-sekali</b>: bila {@code siswa}
	 * sudah terisi dan punya id, argumen <b>diabaikan diam-diam</b> dan method langsung kembali.
	 *
	 * <p>Pengaman ini ditambahkan pada r77034 (“Generic CRUD: enforce safe model mutation
	 * contracts”) dan memang <b>benar</b> untuk data finansial — kuitansi tidak boleh dipindahkan
	 * ke siswa lain, sehingga uang milik seorang siswa tidak bisa “dipindahbukukan” lewat form
	 * generik. Sisi buruknya: kode koreksi data yang sah pun gagal tanpa pesan; koreksi semacam
	 * itu harus dilakukan di lapis basis data.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getSiswa()} <i>dapat</i> mengubah field ini walau setternya
	 * terkunci, karena getter menulis langsung ke field.</p>
	 *
	 * @param siswa siswa pemilik kuitansi; diabaikan bila kolomnya sudah terisi
	 */
	public void setSiswa(Siswa siswa) {

		if (this.siswa != null && this.siswa.getId() != null) {
			return;
		}

		this.siswa = siswa;
	}

	/**
	 * Mengembalikan yayasan pemilik kuitansi.
	 *
	 * <p><b>Getter tulis-balik.</b> Nilainya <b>selalu</b> diturunkan ulang dari
	 * {@link #getSekolah()} (yang sendirinya diturunkan dari siswa/calon siswa), sehingga rantai
	 * tenant kuitansi konsisten: siswa → sekolah → yayasan. Sama seperti {@link #getSekolah()},
	 * ini membuat kolom tenant tidak dapat dipalsukan lewat form, tetapi juga membuat
	 * {@link #setYayasan(Yayasan)} praktis tanpa efek.</p>
	 *
	 * @return yayasan pemilik kuitansi; {@code null} bila sekolahnya belum diketahui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik kuitansi, dengan normalisasi objek “kosong” ({@code null} atau
	 * id {@code null}) menjadi {@code null}. Akan ditimpa {@link #getYayasan()} pada pembacaan
	 * berikutnya bila sekolah diketahui.
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id disimpan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan <b>bulan periode kewajiban</b> yang dilunasi kuitansi ini (1–12) — bukan
	 * bulan terjadinya transaksi.
	 *
	 * <p><b>Getter tulis-balik dengan tiga tahap normalisasi:</b></p>
	 * <ol>
	 *   <li>bila {@code bulan} kosong, diisi dari bulan {@link #getTanggal()}
	 *       ({@code Calendar.MONTH + 1}, karena {@code Calendar} berbasis 0);</li>
	 *   <li>bila {@code jenisBiayaSekolah} berperiode {@code "Tahunan"} atau {@code "Insidentil"},
	 *       {@code bulan} <b>dikosongkan kembali</b> menjadi {@code null} — biaya jenis itu tidak
	 *       punya dimensi bulan;</li>
	 *   <li>bila setelah itu masih {@code null} dan {@code jenisBiayaSekolah.getUntukBulan()}
	 *       terisi, nilai bulan ditarik dari master jenis biaya.</li>
	 * </ol>
	 *
	 * <p><b>Awas — penghapusan permanen:</b> tahap (2) menimpa nilai {@code bulan} yang sudah
	 * tersimpan. Bila sebuah jenis biaya bulanan kemudian diubah menjadi “Tahunan” di master,
	 * bulan pada <b>seluruh kuitansi historis</b> yang memakai jenis biaya itu ikut dikosongkan
	 * begitu kuitansinya dirender, dan nilai aslinya tidak dapat dipulihkan (kecuali dari tabel
	 * audit Envers). Bersama {@link #getTahun()}, ini juga mengubah
	 * {@link #getTahunDanBulan()} — kunci periode yang dipakai laporan tunggakan.</p>
	 *
	 * <p><b>Kuirk:</b> tahap (2) memakai field {@code jenisBiayaSekolah} yang baru saja diisi di
	 * awal method, tetapi memanggil {@code getPeriode()} tanpa pemeriksaan {@code null} pada
	 * hasilnya — master jenis biaya tanpa periode akan melempar {@code NullPointerException}.</p>
	 *
	 * @return bulan periode 1–12; {@code null} untuk biaya tahunan/insidentil
	 */
	@Column(name = "bulan")
	public Integer getBulan() {

		jenisBiayaSekolah = getJenisBiayaSekolah();

		if (bulan == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggal());
			bulan = calendar.get(Calendar.MONTH) + 1;
		}

		if (jenisBiayaSekolah != null) {
			if (jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Tahunan")) {
				bulan = null;
			} else if (jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Insidentil")) {
				bulan = null;
			}
		}

		if (bulan == null && getJenisBiayaSekolah() != null && getJenisBiayaSekolah().getUntukBulan() != null) {
			bulan = getJenisBiayaSekolah().getUntukBulan();
		}

		return this.bulan;
	}

	/**
	 * Menyetel bulan periode kewajiban (1–12). Akan dinormalisasi ulang oleh {@link #getBulan()}.
	 *
	 * @param bulan bulan periode; {@code null} untuk biaya tahunan/insidentil
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan penanda periode <b>bulan berjalan</b> dalam format gabungan {@code yyyyMM}
	 * (mis. {@code 202609} untuk September 2026), berdasarkan waktu server
	 * ({@code WaktuUtil.getCalendar()}).
	 *
	 * <p>Dipakai kode penagihan untuk menjawab “sampai periode mana tagihan seharusnya sudah
	 * terbit/lunas” — pemanggilnya {@code ais.database.model.sekolah.KelasLesSiswaPunyaSiswa}
	 * (dua tempat).</p>
	 *
	 * <p><b>Kuirk:</b> baris {@code cal.set(Calendar.DATE, 1)} tidak berpengaruh apa pun terhadap
	 * hasil (hanya tahun dan bulan yang dibaca); ia sisa dari implementasi lama. Nilai bulan
	 * dinaikkan satu karena {@code Calendar.MONTH} berbasis 0.</p>
	 *
	 * @return periode bulan berjalan dalam bentuk {@code yyyyMM}
	 */
	public static Integer sekarang() {
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.set(Calendar.DATE, 1);
		int tahunCurrent = cal.get(Calendar.YEAR);
		int bulanCurrent = cal.get(Calendar.MONTH);
		int bulanCurrentPlus = bulanCurrent + 1;
		Integer pembayaranSekarang = PembayaranSiswa.convert(tahunCurrent, bulanCurrentPlus);
		return pembayaranSekarang;
	}

	/**
	 * Menggabungkan tahun dan bulan menjadi satu bilangan periode {@code yyyyMM}
	 * (mis. {@code 2026} + {@code 9} → {@code 202609}).
	 *
	 * <p>Ini <b>utilitas periode standar seluruh modul billing sekolah</b>: dipakai puluhan kali
	 * di {@code DetailTagihanSiswaHelper}, {@code DetailTagihanCalonSiswaHelper},
	 * {@code TagihanUtil}, {@code PembayaranOnline}, dan lain-lain, jauh melampaui kelas ini.
	 * Jangan mengubah formatnya.</p>
	 *
	 * <p>Bulan selalu diberi <i>padding</i> dua digit ({@code 9} → {@code "09"}) sehingga urutan
	 * numerik periode sama dengan urutan kronologisnya — sifat inilah yang dipakai perbandingan
	 * {@code &lt;}/{@code &gt;} pada logika tunggakan. Bila {@code b} {@code null}, hasilnya hanya
	 * tahun ({@code 2026}); bila {@code t} {@code null}, hasilnya hanya bulan berdigit dua
	 * ({@code 9}, karena {@code "09"} di-parse sebagai bilangan) — <b>bentuk kedua ini praktis
	 * tidak bermakna</b> dan menandakan data periode yang rusak.</p>
	 *
	 * @param t tahun empat digit; boleh {@code null}
	 * @param b bulan 1–12; boleh {@code null}
	 * @return periode gabungan {@code yyyyMM}; {@code null} bila {@code t} dan {@code b}
	 *         dua-duanya {@code null}
	 */
	public static Integer convert(Integer t, Integer b) {
		String bS = "";
		if (b != null) {
			bS = ("00" + b).substring(("00" + b).length() - 2);
		}
		String tS = "";
		if (t != null) {
			tS = "" + t;
		}
		String tb = tS + "" + bS;
		Integer tahunDanBulan = null;
		if (!tb.isEmpty()) {
			tahunDanBulan = Integer.parseInt(tb);
		}
		return tahunDanBulan;
	}

	/**
	 * Mengembalikan nomor referensi <i>inquiry</i> dari bank untuk transaksi ini. Aksesor polos.
	 *
	 * <p>Nilai {@code "000000"} dipakai sebagai penanda “bukan dari inquiry bank” (lihat
	 * {@link #saveOrUpdateDeposit(Session)} yang menyalinnya ke {@code DepositSiswa}).</p>
	 *
	 * @return nomor referensi inquiry bank; {@code null} untuk pembayaran non-bank
	 */
	@Column(name = "inquiry_pembayaran")
	public String getInquiryPembayaran() {
		return this.inquiryPembayaran;
	}

	/**
	 * Menyetel nomor referensi inquiry bank. Aksesor polos.
	 *
	 * @param inquiryPembayaran nomor referensi inquiry
	 */
	public void setInquiryPembayaran(String inquiryPembayaran) {
		this.inquiryPembayaran = inquiryPembayaran;
	}

	/**
	 * Mengembalikan <b>total rupiah kuitansi</b> — angka yang tercetak di struk dan yang dipakai
	 * seluruh laporan penerimaan.
	 *
	 * <p><b>Getter tulis-balik.</b> Bila {@link #getNominalManual()} terisi dan lebih dari 0,1,
	 * nilainya <b>menimpa</b> {@code nominal}. Artinya angka yang diketik operator selalu menang
	 * atas angka yang dihitung sistem, dan penimpaan itu ikut tersimpan ke kolom
	 * {@code nominal}. Melepas/mengosongkan {@code nominalManual} <b>tidak</b> memulihkan angka
	 * sistem yang sudah tertimpa.</p>
	 *
	 * <p>{@code null} dinormalisasi menjadi {@code 0.0} agar aritmetika pemanggil tidak
	 * melempar {@code NullPointerException}. Perhatikan bahwa {@code 0.0} dan “belum diisi”
	 * karena itu tidak dapat dibedakan dari luar.</p>
	 *
	 * @return total rupiah kuitansi; {@code 0.0} bila belum terisi
	 */
	@Column(name = "nominal", nullable = false, precision = 17, scale = 17)
	public Double getNominal() {
		if (getNominalManual() != null && getNominalManual() > 0.1) {
			nominal = getNominalManual();
		}
		return this.nominal == null ? 0.0 : nominal;
	}

	/**
	 * Menyetel total rupiah kuitansi. Akan ditimpa {@link #getNominal()} bila
	 * {@code nominalManual} terisi.
	 *
	 * @param nominal total rupiah kuitansi
	 */
	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	/**
	 * Mengembalikan <b>sisa saldo tabungan</b> siswa setelah transaksi ini, sebagai snapshot
	 * untuk dicetak di struk. Aksesor polos — tidak menghitung ulang saldo.
	 *
	 * <p>Saldo sesungguhnya dihitung terpisah oleh
	 * {@code ais.action.master.sekolah.util.DepositHelper}; kolom ini hanya arsip historis dan
	 * dapat berbeda dari hasil perhitungan ulang bila data setelahnya berubah.</p>
	 *
	 * @return sisa saldo tabungan saat transaksi; {@code null} bila tidak dicatat
	 */
	@Column(name = "sisa_deposit", precision = 17, scale = 17)
	public Double getSisaDeposit() {
		return this.sisaDeposit;
	}

	/**
	 * Menyetel snapshot sisa saldo tabungan setelah transaksi. Aksesor polos.
	 *
	 * @param sisaDeposit sisa saldo tabungan
	 */
	public void setSisaDeposit(Double sisaDeposit) {
		this.sisaDeposit = sisaDeposit;
	}

	/**
	 * Mengembalikan <b>tahun periode kewajiban</b> yang dilunasi kuitansi ini — bukan tahun
	 * terjadinya transaksi.
	 *
	 * <p><b>Getter tulis-balik</b>, pasangan {@link #getBulan()} dengan tiga tahap serupa:
	 * (1) bila kosong, diisi dari tahun {@link #getTanggal()}; (2) bila jenis biaya berperiode
	 * {@code "Insidentil"}, dikosongkan kembali menjadi {@code null}; (3) bila masih kosong,
	 * ditarik dari {@code jenisBiayaSekolah.getUntukTahun()}.</p>
	 *
	 * <p><b>Perbedaan halus dari {@link #getBulan()}</b> — dan kemungkinan besar tidak
	 * disengaja: di sini tahap (2) hanya mengosongkan untuk periode {@code "Insidentil"}, tidak
	 * untuk {@code "Tahunan"} (yang memang wajar, karena biaya tahunan tetap punya tahun);
	 * tetapi tahap (2) juga membaca field {@code jenisBiayaSekolah} <b>tanpa</b> memanggil
	 * {@link #getJenisBiayaSekolah()} lebih dulu, berbeda dari {@link #getBulan()}. Bila objek
	 * baru dimuat dan getternya belum pernah dipanggil, field itu masih berupa proxy yang belum
	 * diresolusi atau {@code null}, sehingga normalisasi “Insidentil” <b>terlewat</b>. Hasil
	 * {@code getTahun()} karena itu dapat berbeda tergantung urutan pemanggilan getter — dan
	 * karena nilainya tersimpan, perbedaan itu menjadi permanen.</p>
	 *
	 * @return tahun periode; {@code null} untuk biaya insidentil
	 */
	@Column(name = "tahun")
	public Integer getTahun() {
		if (tahun == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggal());
			tahun = calendar.get(Calendar.YEAR);
		}

		if (jenisBiayaSekolah != null && jenisBiayaSekolah.getPeriode().equalsIgnoreCase("Insidentil")) {
			tahun = null;
		}

		if (tahun == null && getJenisBiayaSekolah() != null && getJenisBiayaSekolah().getUntukTahun() != null) {
			tahun = getJenisBiayaSekolah().getUntukTahun();
		}

		return this.tahun;
	}

	/**
	 * Menyetel tahun periode kewajiban. Akan dinormalisasi ulang oleh {@link #getTahun()}.
	 *
	 * @param tahun tahun periode; {@code null} untuk biaya insidentil
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan <b>kunci periode gabungan</b> {@code yyyyMM} kuitansi ini.
	 *
	 * <p><b>Getter tulis-balik.</b> Nilainya selalu dihitung ulang dari {@link #getBulan()} dan
	 * {@link #getTahun()} lewat {@link #convert(Integer, Integer)}, sehingga seluruh efek samping
	 * kedua getter itu (termasuk pengosongan {@code bulan}/{@code tahun} untuk biaya
	 * tahunan/insidentil) ikut terpicu. Nilai yang disetel lewat
	 * {@link #setTahunDanBulan(Integer)} tidak pernah bertahan.</p>
	 *
	 * <p>Kolom ini yang dipakai laporan tunggakan dan pencocokan tagihan per periode.</p>
	 *
	 * @return kunci periode {@code yyyyMM}; hanya tahun untuk biaya tahunan, {@code null} untuk
	 *         biaya insidentil tanpa tahun
	 */
	@Column(name = "tahun_dan_bulan")
	public Integer getTahunDanBulan() {
		Integer b = getBulan();
		Integer t = getTahun();
		tahunDanBulan = PembayaranSiswa.convert(t, b);

		return this.tahunDanBulan;
	}

	/**
	 * Menyetel kunci periode gabungan. <b>Praktis tanpa efek</b> — {@link #getTahunDanBulan()}
	 * selalu menghitung ulang nilainya.
	 *
	 * @param tahunDanBulan kunci periode {@code yyyyMM}
	 */
	public void setTahunDanBulan(Integer tahunDanBulan) {
		this.tahunDanBulan = tahunDanBulan;
	}

	/**
	 * Mengembalikan <b>waktu transaksi</b> kuitansi (presisi {@code TIMESTAMP}).
	 *
	 * <p><b>Getter tulis-balik dengan dua koreksi senyap:</b></p>
	 * <ol>
	 *   <li><b>Kanal VA menang.</b> Bila kuitansi terhubung ke {@code virtualAccountBank} yang
	 *       punya {@code waktuBayar}, {@code tanggal} <b>ditimpa</b> dengan waktu bayar versi bank.
	 *       Waktu apa pun yang diisikan operator hilang permanen. Untuk pembayaran daring ini
	 *       masuk akal (waktu bank adalah kebenaran), tetapi perlu diketahui bahwa nilainya
	 *       berasal dari data yang dikirim pemanggil endpoint bank — lihat catatan keamanan pada
	 *       Javadoc kelas.</li>
	 *   <li><b>Tambalan tahun 1970.</b> Bila tahun {@code tanggal} bernilai 1970 (ciri khas nilai
	 *       waktu {@code 0}/gagal parse), tahunnya <b>diganti dengan tahun berjalan</b> sementara
	 *       tanggal dan bulannya dipertahankan. Ini menyembunyikan data rusak alih-alih
	 *       melaporkannya: kuitansi bertanggal salah tetap terlihat wajar, dan tanggal aslinya
	 *       tidak dapat dipulihkan.</li>
	 * </ol>
	 *
	 * <p>Bila {@code tanggal} masih {@code null}, waktu server sekarang dikembalikan <b>tanpa</b>
	 * disimpan ke field — jadi dua pemanggilan beruntun pada objek baru bisa mengembalikan waktu
	 * yang sedikit berbeda.</p>
	 *
	 * @return waktu transaksi; waktu server sekarang bila belum diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = false, length = 29)
	public Date getTanggal() {

		if (tanggal != null) {

			if (virtualAccountBank != null && virtualAccountBank.getWaktuBayar() != null) {
				tanggal = virtualAccountBank.getWaktuBayar();
			}

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal);
			if (calendar.get(Calendar.YEAR) == 1970) {
				calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
				tanggal = calendar.getTime();
			}

		}

		return this.tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menyetel waktu transaksi. Akan ditimpa {@link #getTanggal()} bila kuitansi terhubung ke VA
	 * yang punya waktu bayar, atau bila tahunnya 1970.
	 *
	 * @param tanggal waktu transaksi
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan <b>tanggal bayar</b> kuitansi (presisi {@code DATE}, tanpa jam).
	 *
	 * <p><b>Getter destruktif — kolom ini tidak dapat menyimpan nilai sendiri.</b> Baris pertama
	 * method <b>selalu</b> menimpa {@code tanggalBayar} dengan {@link #getTanggal()}, tanpa syarat
	 * apa pun. Akibatnya nilai apa pun yang disetel lewat {@link #setTanggalBayar(Date)} —
	 * termasuk tanggal setelmen bank yang sesungguhnya, yang memang disetel eksplisit oleh
	 * {@code TunaiSiswaCommon} — <b>hancur permanen</b> pada pembacaan pertama, dan penimpaan itu
	 * ikut tersimpan ke kolom {@code tanggal_bayar}.</p>
	 *
	 * <p>Ini bukan sekadar redundansi kosmetik: {@code PembayaranSiswaAction.initCriteria(…)}
	 * menyaring daftar kuitansi dengan
	 * {@code date(this_.tanggal_bayar) between …}, dan laporan penerimaan harian memakai kolom
	 * yang sama. Jadi rekapitulasi kas selalu mengikuti {@code tanggal} (waktu pencatatan/VA),
	 * bukan tanggal uang benar-benar diterima, dan selisih keduanya tidak pernah dapat dilihat
	 * lagi.</p>
	 *
	 * @return tanggal bayar, yang selalu sama dengan tanggal pada {@link #getTanggal()}
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_bayar", length = 13)
	public Date getTanggalBayar() {
		tanggalBayar = getTanggal();

		return this.tanggalBayar;
	}

	/**
	 * Menyetel tanggal bayar. <b>Tanpa efek yang bertahan</b> — {@link #getTanggalBayar()}
	 * menimpanya dengan {@link #getTanggal()} pada pembacaan berikutnya.
	 *
	 * @param tanggalBayar tanggal bayar
	 */
	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	/**
	 * Mengembalikan bagian pembayaran yang <b>disetorkan sebagai topup tabungan</b> siswa.
	 *
	 * <p><b>Getter destruktif — sumber “setoran hantu”.</b> Bila {@code tambahanDeposit}
	 * {@code null} <b>atau</b> bagian bulatnya nol ({@code intValue() == 0}, yang juga mencakup
	 * seluruh nilai antara 0 dan 1), field <b>ditimpa</b> dengan {@link #getNominal()} — yaitu
	 * <b>seluruh</b> nilai kuitansi. Karena kolom {@code tambahan_deposit} terpetakan, nilai
	 * pengganti itu ikut tersimpan.</p>
	 *
	 * <p>Dampaknya nyata, bukan teoretis: {@link #saveOrUpdateDeposit(Session)} memakai getter ini
	 * sebagai syarat penerbitan {@code DepositSiswa}, sehingga pemanggil yang <i>sengaja</i>
	 * menyetel nol — seperti {@code VirtualAccountBank.bayarSiswaLangsung()} (pembayaran tagihan
	 * yang justru <b>memotong</b> tabungan, dengan komentar eksplisit “agar tidak membuat record
	 * DepositSiswa baru”) — tetap menerbitkan setoran sebesar seluruh nominal. Baris kredit palsu
	 * itu kemudian menjadi bahan posting jurnal oleh {@code PostingDepositSiswaAction}.</p>
	 *
	 * <p>Nilai yang benar-benar nol karena itu <b>tidak dapat direpresentasikan</b> lewat objek
	 * ini; satu-satunya cara menghindarinya adalah tidak memanggil
	 * {@link #saveOrUpdateDeposit(Session)}.</p>
	 *
	 * @return besar topup tabungan; {@code 0.0} hanya bila {@link #getNominal()} juga nol
	 */
	@Column(name = "tambahan_deposit", precision = 17, scale = 17)
	public Double getTambahanDeposit() {
		if (tambahanDeposit == null || tambahanDeposit.intValue() == 0) {
			tambahanDeposit = getNominal();
		}
		return this.tambahanDeposit == null ? 0.0 : tambahanDeposit;
	}

	/**
	 * Menyetel besar topup tabungan yang menyertai pembayaran ini. <b>Nilai 0 tidak bertahan</b>
	 * — lihat peringatan pada {@link #getTambahanDeposit()}.
	 *
	 * @param tambahanDeposit besar topup tabungan
	 */
	public void setTambahanDeposit(Double tambahanDeposit) {
		this.tambahanDeposit = tambahanDeposit;
	}

	/**
	 * Mengembalikan snapshot <b>total saldo tabungan</b> siswa terkait transaksi ini, untuk
	 * dicetak di struk. Aksesor polos — tidak menghitung ulang.
	 *
	 * <p>Catatan: pada beberapa jalur (mis. {@code Bniresponse}) pengisian kolom ini sengaja
	 * dinonaktifkan (baris {@code setTotalDeposit(…)} dikomentari), sehingga kolom kerap
	 * {@code null} pada data produksi.</p>
	 *
	 * @return total saldo tabungan saat transaksi; {@code null} bila tidak dicatat
	 */
	@Column(name = "total_deposit", precision = 17, scale = 17)
	public Double getTotalDeposit() {
		return this.totalDeposit;
	}

	/**
	 * Menyetel snapshot total saldo tabungan. Aksesor polos.
	 *
	 * @param totalDeposit total saldo tabungan
	 */
	public void setTotalDeposit(Double totalDeposit) {
		this.totalDeposit = totalDeposit;
	}

	// ---------------------------------------------------------------------------------------
	// KODE MATI (dibiarkan sebagai dokumentasi sejarah): dahulu setiap kuitansi punya kolom
	// unik `kode_unik` berpola "<subjek>-<jenisBiaya>-<periode>" yang menjadi PENGAMAN
	// ANTI-GANDA di lapis basis data (constraint UNIQUE). Pengaman itu kini TIDAK ADA:
	// satu-satunya perlindungan terhadap kuitansi ganda adalah pemeriksaan idempoten di sisi
	// pemanggil (mis. `Bniresponse.isRequestSudahDiproses(...)` dan pengecekan FK
	// virtualAccountBank/bniRequest sebelum insert). Jangan mengaktifkan kembali blok ini tanpa
	// migrasi data — basis data berjalan hampir pasti sudah memuat baris yang melanggar pola
	// keunikan tersebut.
	// ---------------------------------------------------------------------------------------
	// public static String genCode(Siswa siswa, CalonSiswa calonSiswa,
	// JenisBiayaSekolah jenisBiayaSekolah,
	// Integer tahunDanBulan, Date tanggal) {
	// return (siswa != null ? "siswa_" + siswa.getId() : ("calon_siswa_" +
	// calonSiswa.getId())) + "-"
	// + jenisBiayaSekolah.getId() + "-" + (tahunDanBulan == null ?
	// tanggal.getTime() : tahunDanBulan);
	// }
	//
	// @Column(name = "kode_unik", unique = true)
	// public String getKodeUnik() {
	// this.kodeUnik = PembayaranSiswa.genCode(siswa, calonSiswa,
	// jenisBiayaSekolah, tahunDanBulan, tanggal);
	// return this.kodeUnik;
	// }
	//
	// public void setKodeUnik(String kodeUnik) {
	// this.kodeUnik = kodeUnik;
	// }

	/**
	 * Mengembalikan jenis biaya sekolah (SPP, Uang Gedung, Daftar Ulang, …) yang dilunasi
	 * kuitansi ini, setelah proxy lazy-nya diresolusi dengan {@code check(…)}.
	 *
	 * <p>Master ini menentukan <b>periode</b> kewajiban ({@code Bulanan}/{@code Tahunan}/
	 * {@code Insidentil}) dan karena itu ikut menentukan hasil {@link #getBulan()},
	 * {@link #getTahun()}, dan {@link #getTahunDanBulan()}. Kolomnya nullable: kuitansi tanpa
	 * jenis biaya berarti “membayar campuran beberapa jenis” dan dirender sebagai
	 * {@code "Semua"} oleh {@link #getNama()}.</p>
	 *
	 * @return jenis biaya sekolah; {@code null} untuk kuitansi campuran
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_biaya_id", nullable = true)
	public JenisBiayaSekolah getJenisBiayaSekolah() {
		jenisBiayaSekolah = check(jenisBiayaSekolah);
		return jenisBiayaSekolah;
	}

	/**
	 * Menyetel jenis biaya sekolah yang dilunasi kuitansi ini. Aksesor polos.
	 *
	 * @param jenisBiayaSekolah jenis biaya; {@code null} untuk kuitansi campuran
	 */
	public void setJenisBiayaSekolah(JenisBiayaSekolah jenisBiayaSekolah) {
		this.jenisBiayaSekolah = jenisBiayaSekolah;
	}

	/**
	 * Mengembalikan <b>cara pembayaran / akun buku besar</b> kuitansi ini
	 * ({@code ais.database.model.sekolah.AkunPembayaranSiswa}).
	 *
	 * <p><b>Getter tulis-balik dengan pemilihan otomatis.</b> Bila kolomnya kosong, method
	 * menelusuri seluruh {@code AkunPembayaranSiswa} dari cache {@code ConstantValues} dan
	 * memilih baris <b>pertama</b> yang ditandai {@code defaultPembayaran} <i>dan</i> milik
	 * sekolah kuitansi ini. Pilihan itu lalu tersimpan pada flush berikutnya.</p>
	 *
	 * <p><b>Tidak deterministik.</b> Penelusuran memakai {@code maps.values()} dari sebuah
	 * {@code Map} — tanpa pengurutan — dan berhenti pada kecocokan pertama. Bila sebuah sekolah
	 * punya lebih dari satu “cara pembayaran default” (tidak ada constraint yang mencegahnya),
	 * akun buku besar yang dipakai bisa <b>berbeda antar pemanggilan</b>, sehingga penerimaan yang
	 * seharusnya sejenis tersebar ke akun yang berlainan di jurnal. Ini bentuk sama dengan cacat
	 * pemilihan akun {@code setMaxResults(1)} tanpa {@code addOrder} yang tercatat pada
	 * {@code AkunPembayaranSiswa}.</p>
	 *
	 * <p>Seluruh blok dibungkus {@code try/catch} yang menelan kegagalan (hanya dicatat lewat
	 * {@code ErrorAuditUtil}), sehingga kegagalan pemilihan akun berujung pada {@code null} yang
	 * kemudian melanggar {@code nullable = false} saat penyimpanan — muncul sebagai kegagalan
	 * simpan yang tidak jelas sebabnya, jauh dari sumbernya.</p>
	 *
	 * @return cara pembayaran / akun buku besar; {@code null} bila tidak ada akun default yang
	 *         cocok untuk sekolah kuitansi ini
	 */
	@SuppressWarnings("unchecked")
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_pembayaran_siswa_id", nullable = false)
	public AkunPembayaranSiswa getAkunPembayaranSiswa() {
		akunPembayaranSiswa = check(akunPembayaranSiswa);

		try {
			if (akunPembayaranSiswa == null) {
				Sekolah sekolah = getSekolah();
				if (sekolah != null) {
					Map<Long, AkunPembayaranSiswa> maps = ConstantValues.ambilBerdasarClass(AkunPembayaranSiswa.class);
					for (AkunPembayaranSiswa a : maps.values()) {
						if (a.getDefaultPembayaran() && a.getSekolah() != null
								&& sekolah.getId().equals(a.getSekolah().getId())) {
							akunPembayaranSiswa = a;
							break;
						}
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PembayaranSiswa.java:595");
			// TODO: handle exception
		}

		return akunPembayaranSiswa;
	}

	/**
	 * Menyetel cara pembayaran / akun buku besar kuitansi ini. Aksesor polos; bila dibiarkan
	 * kosong, {@link #getAkunPembayaranSiswa()} akan memilihkannya sendiri.
	 *
	 * @param akunPembayaranSiswa cara pembayaran / akun buku besar
	 */
	public void setAkunPembayaranSiswa(AkunPembayaranSiswa akunPembayaranSiswa) {
		this.akunPembayaranSiswa = akunPembayaranSiswa;
	}

	/**
	 * Mengembalikan calon siswa (pendaftar PSB) pemilik kuitansi ini, setelah proxy lazy-nya
	 * diresolusi dengan {@code check(…)}.
	 *
	 * <p>Terisi untuk pembayaran tahap pendaftaran (formulir, uang pangkal, daftar ulang) yang
	 * dilakukan sebelum pendaftar resmi menjadi {@code Siswa}. Setelah konversi, kuitansi tetap
	 * menyimpan {@code calonSiswa} <b>dan</b> memperoleh {@code siswa} lewat efek samping
	 * {@link #getSiswa()}.</p>
	 *
	 * @return calon siswa pemilik kuitansi; {@code null} untuk pembayaran siswa terdaftar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa_id", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menyetel calon siswa pemilik kuitansi, dengan <b>kontrak tulis-sekali</b> yang sama seperti
	 * {@link #setSiswa(Siswa)}: bila {@code calonSiswa} sudah terisi dan punya id, argumen
	 * <b>diabaikan diam-diam</b>.
	 *
	 * @param calonSiswa calon siswa pemilik kuitansi; diabaikan bila kolomnya sudah terisi
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {

		if (this.calonSiswa != null && this.calonSiswa.getId() != null) {
			return;
		}

		this.calonSiswa = calonSiswa;
	}

	/**
	 * Mengembalikan permintaan pembayaran BRI H2H ({@code ais.database.model.bri.BriRequest})
	 * yang menghasilkan kuitansi ini. Aksesor polos — tidak memanggil {@code check(…)}, jadi
	 * yang dikembalikan bisa berupa proxy yang belum diresolusi.
	 *
	 * <p>{@code FetchMode.SELECT} dipakai agar relasi ini tidak ikut di-join pada setiap
	 * pemuatan kuitansi. Kolom ini juga berperan sebagai <b>kunci idempoten</b>: sebelum membuat
	 * kuitansi baru, {@code Briresponse} menghapus kuitansi lama yang bertaut ke
	 * {@code BriRequest} yang sama.</p>
	 *
	 * @return permintaan BRI terkait; {@code null} untuk kanal lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "bri_request_id")
	public BriRequest getBriRequest() {
		return briRequest;
	}

	/**
	 * Menyetel permintaan pembayaran BRI H2H yang menghasilkan kuitansi ini.
	 *
	 * @param briRequest permintaan BRI; boleh {@code null}
	 */
	public void setBriRequest(BriRequest briRequest) {
		this.briRequest = briRequest;
	}

	/**
	 * Mengembalikan permintaan pembayaran BNI eCollection
	 * ({@code ais.database.model.bni.BniRequest}) yang menghasilkan kuitansi ini. Aksesor polos.
	 *
	 * <p>Selain menandai kanal, kolom ini dipakai {@code Bniresponse.isRequestSudahDiproses(…)}
	 * sebagai bukti “pembayaran nyata sudah mendarat” ({@code COUNT} atas
	 * {@code PembayaranSiswa} dengan {@code bniRequest} yang sama) agar callback berulang tidak
	 * memproses ganda. Nilainya juga dibaca {@link #getDariTabungan()} untuk mengambil porsi
	 * pembayaran yang berasal dari deposit.</p>
	 *
	 * @return permintaan BNI terkait; {@code null} untuk kanal lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "bni_request_id")
	public BniRequest getBniRequest() {
		return bniRequest;
	}

	/**
	 * Menyetel permintaan pembayaran BNI eCollection yang menghasilkan kuitansi ini.
	 *
	 * @param bniRequest permintaan BNI; boleh {@code null}
	 */
	public void setBniRequest(BniRequest bniRequest) {
		this.bniRequest = bniRequest;
	}

	/**
	 * Mengembalikan <b>nama pihak yang memvalidasi/menerima pembayaran</b> ini — nama kasir untuk
	 * pembayaran tunai, atau nama bank/nilai konfigurasi
	 * ({@code default_validator_bni}, {@code default_validator_bri}, …) untuk pembayaran kanal
	 * bank.
	 *
	 * <p><b>PERINGATAN — getter ini dapat MEMALSUKAN jejak audit finansial.</b> Bila
	 * {@code validator} kosong, method mengisinya dengan nama <b>pengguna yang sedang login saat
	 * itu</b> ({@code Common.getCurrentUser().getUserNama()}). Karena kolom {@code validator}
	 * terpetakan dan getter dipanggil Hibernate saat <i>dirty checking</i>, sekadar
	 * <b>membuka/merender</b> kuitansi lama yang validatornya belum terisi sudah cukup untuk
	 * mencap pengguna yang sedang <i>melihat</i> sebagai pihak yang <i>menerima uang</i>. Tidak
	 * ada konfirmasi, tidak ada jejak bahwa pengisian itu otomatis, dan nilainya tidak dapat
	 * dikembalikan ke “kosong” karena setter apa pun akan tertimpa lagi oleh getter ini.</p>
	 *
	 * <p>Ini bentuk yang sama persis dengan {@code DepositSiswa.getValidator()}; keduanya
	 * memerlukan penanganan bersama, bukan tambalan setempat. Kegagalan mengambil pengguna
	 * ditelan {@code Common.tampilErrorJikaAdmin} sehingga hasilnya menjadi teks kosong.</p>
	 *
	 * @return nama validator; teks kosong (bukan {@code null}) bila tidak dapat ditentukan
	 */
	public String getValidator() {
		if (validator == null || validator.trim().isEmpty()) {
			try {
				Tbmuser tbmuser = Common.getCurrentUser();
				validator = tbmuser == null ? "" : tbmuser.getUserNama();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return validator == null ? "" : validator;
	}

	/**
	 * Menyetel nama validator/penerima pembayaran. Nilai kosong akan diisi ulang secara otomatis
	 * oleh {@link #getValidator()} — lihat peringatannya.
	 *
	 * @param validator nama validator (nama kasir atau nama bank)
	 */
	public void setValidator(String validator) {
		this.validator = validator;
	}

	/**
	 * Mengembalikan keterangan bebas kuitansi.
	 *
	 * <p>Isinya berbeda-beda menurut jalur pembuat: {@code TunaiSiswaCommon} menuliskan rangkuman
	 * tagihan yang dilunasi ({@code "<idTagihan>-<namaItem>, bulan …, tahun …, …"}), sedangkan
	 * callback bank menyalin keterangan dari permintaan bank. {@code null} dinormalisasi menjadi
	 * teks kosong agar aman dirender langsung.</p>
	 *
	 * <p>Kolomnya bertipe {@code text} pada basis data (lihat {@code cascade.sql}), jadi tidak ada
	 * batas panjang. Layar pencarian menyaringnya dengan {@code ilike} pola {@code ANYWHERE}.</p>
	 *
	 * @return keterangan kuitansi; teks kosong bila tidak diisi
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel keterangan bebas kuitansi. Aksesor polos.
	 *
	 * @param keterangan keterangan kuitansi
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan permintaan pembayaran BSI ({@code ais.database.model.bsi.BsiRequest}) yang
	 * menghasilkan kuitansi ini. Aksesor polos; berperan pula sebagai kunci idempoten bagi
	 * {@code Bsiresponse}.
	 *
	 * @return permintaan BSI terkait; {@code null} untuk kanal lain
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "bsi_request_id")
	public BsiRequest getBsiRequest() {
		return bsiRequest;
	}

	/**
	 * Menyetel permintaan pembayaran BSI yang menghasilkan kuitansi ini.
	 *
	 * @param bsiRequest permintaan BSI; boleh {@code null}
	 */
	public void setBsiRequest(BsiRequest bsiRequest) {
		this.bsiRequest = bsiRequest;
	}

	/**
	 * Mengembalikan tautan ke <b>Virtual Account</b> ({@code ais.database.model.VirtualAccountBank})
	 * yang dibayar untuk menghasilkan kuitansi ini. Aksesor polos.
	 *
	 * <p>Ini kanal <b>generik</b> yang dipakai belasan servlet bank di luar BRI/BNI/BSI (Mandiri,
	 * BCA, BTN, BJBS, Nagari, OCBC NISP, Otto, Flip, Finpay, Esmartlink, MncBank, BMS, Maja,
	 * Bankaltimtara, OnlineBmt) melalui {@code VirtualAccountBank.bayarSiswa(…)}. Nilainya juga
	 * dipakai {@link #getTanggal()} (waktu bayar bank menimpa tanggal kuitansi),
	 * {@link #getDariTabungan()} (porsi dari tabungan), dan sebagai kunci idempoten sebelum
	 * penerbitan kuitansi baru.</p>
	 *
	 * <p>Perhatikan nama kolomnya {@code virtual_account_bank} — tanpa akhiran {@code _id} seperti
	 * FK lain di kelas ini.</p>
	 *
	 * @return Virtual Account terkait; {@code null} untuk pembayaran non-VA
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "virtual_account_bank")
	public VirtualAccountBank getVirtualAccountBank() {
		return virtualAccountBank;
	}

	/**
	 * Menyetel Virtual Account yang dibayar untuk kuitansi ini.
	 *
	 * @param virtualAccountBank Virtual Account terkait; boleh {@code null}
	 */
	public void setVirtualAccountBank(VirtualAccountBank virtualAccountBank) {
		this.virtualAccountBank = virtualAccountBank;
	}

	/**
	 * Mengembalikan <b>akun pengguna</b> yang tercatat sebagai validator pembayaran ini
	 * ({@code ais.database.model.Tbmuser}) — pendamping ber-FK dari {@link #getValidator()} yang
	 * hanya berupa teks.
	 *
	 * <p><b>Getter tulis-balik dengan dua strategi:</b></p>
	 * <ol>
	 *   <li><b>Pencocokan nama.</b> Bila {@code validatorUser} belum ada tetapi
	 *       {@link #getValidator()} tidak kosong, method menelusuri seluruh {@code Tbmuser} dari
	 *       cache {@code ConstantValues} dan mengambil pengguna <b>aktif pertama</b> yang
	 *       {@code userNama}-nya sama (tanpa memperhatikan besar kecil huruf). Karena
	 *       {@code userNama} adalah nama tampilan — bukan kunci unik — dua pegawai bernama sama
	 *       akan saling tertukar, dan pencocokan berhenti pada yang pertama ditemukan tanpa
	 *       pengurutan. Untuk kanal bank, {@code validator} berisi nama bank sehingga pencocokan
	 *       ini normalnya tidak menemukan siapa pun.</li>
	 *   <li><b>Pengguna berjalan untuk baris baru.</b> Bila kuitansi belum tersimpan
	 *       ({@code getId() == null}) dan validator masih belum ketemu, pengguna yang sedang login
	 *       dipasang — <b>setelah</b> divalidasi keberadaannya di cache {@code ConstantValues}.
	 *       Validasi tersebut sengaja ditambahkan untuk mencegah pelanggaran kunci asing ketika
	 *       konteks API mengembalikan pengguna yang tidak ada di basis data (konteks Spring
	 *       Security berbeda dari sesi web biasa); komentar aslinya dipertahankan di dalam kode.</li>
	 * </ol>
	 *
	 * <p>Berbeda dari {@link #getValidator()}, strategi (2) <b>dibatasi pada baris baru</b>,
	 * sehingga getter ini tidak mencap pengguna yang sekadar melihat kuitansi lama. Namun
	 * strategi (1) tetap berlaku untuk baris lama dan dapat menautkan kuitansi ke akun pengguna
	 * yang keliru. Seluruh blok dibungkus {@code try/catch} berlapis yang menelan kegagalan ke
	 * {@code ErrorAuditUtil}.</p>
	 *
	 * @return akun pengguna validator; {@code null} bila tidak ada yang cocok
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "validator_user", nullable = true)
	public Tbmuser getValidatorUser() {

		if (validatorUser != null) {
			validatorUser = check(validatorUser);
		} else {
			try {
				if (validatorUser == null || validatorUser.getUserId() == null) {
					if (!getValidator().trim().isEmpty()) {
						for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
							try {
								Tbmuser tbmuser = (Tbmuser) o;
								if (tbmuser.getAktif()) {
									if (tbmuser.getUserNama().equalsIgnoreCase(validator)) {
										validatorUser = tbmuser;
										break;
									}
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PembayaranSiswa.java:709");
							}
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PembayaranSiswa.java:714");
			}

			try {
				if (getId() == null) {
					if (validatorUser == null || validatorUser.getUserId() == null) {
						Tbmuser current = Common.getCurrentUser();
						// Validasi user ada di ConstantValues cache sebelum set validatorUser.
						// Mencegah FK violation saat API context mengembalikan user tidak valid
						// (Spring Security context berbeda dari sesi web biasa).
						if (current != null && current.getUserId() != null) {
							for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
								try {
									Tbmuser t = (Tbmuser) o;
									if (current.getUserId().equals(t.getUserId())) {
										validatorUser = current;
										break;
									}
								} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/sekolah/PembayaranSiswa.java:732");
								}
							}
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PembayaranSiswa.java:738");
			}
		}
		return validatorUser;
	}

	/**
	 * Menyetel akun pengguna validator pembayaran. Aksesor polos.
	 *
	 * @param validatorUser akun pengguna validator; boleh {@code null}
	 */
	public void setValidatorUser(Tbmuser validatorUser) {
		this.validatorUser = validatorUser;
	}

	/**
	 * Mengembalikan besar rupiah pembayaran ini yang <b>diambil dari tabungan/deposit siswa</b>.
	 *
	 * <p>Ini <b>sisi DEBIT dompet elektronik siswa</b>:
	 * {@code ais.action.master.sekolah.util.DepositHelper} menghitung saldo dengan menjumlahkan
	 * {@code SUM(dariTabungan)} atas seluruh {@code PembayaranSiswa} milik siswa yang bersangkutan
	 * ({@code WHERE dariTabungan > 0.1}) sebagai pengeluaran. Indeks
	 * {@code idx_pembayaran_siswa_tabungan_*} di {@code ais.common.InitIndex} dibuat khusus untuk
	 * query itu.</p>
	 *
	 * <p><b>Getter tulis-balik berjenjang</b> — lima cabang, dievaluasi berurutan dan berhenti
	 * pada yang pertama cocok:</p>
	 * <ol>
	 *   <li>deposit dari {@code bniRequest} bila &gt; 0,1;</li>
	 *   <li>tabungan dari {@code virtualAccountBank} bila &gt; 0,1;</li>
	 *   <li>{@link #getDariTabunganManual()} bila &gt; 0,1 (angka yang diketik operator);</li>
	 *   <li><b>seluruh {@link #getNominal()}</b> bila cara pembayaran kuitansi ini ditandai
	 *       {@code AkunPembayaranSiswa.getDariTabungan()};</li>
	 *   <li>{@code 0.0} bila tidak satu pun terpenuhi.</li>
	 * </ol>
	 *
	 * <p><b>PERINGATAN FINANSIAL — cabang (4) berlaku surut.</b> Cabang itu tidak membaca data
	 * historis apa pun, melainkan <i>flag master</i> yang dapat diubah kapan saja dari layar
	 * “Cara Pembayaran”. Bila seorang admin menyalakan flag {@code dariTabungan} pada sebuah
	 * {@code AkunPembayaranSiswa} yang sudah lama dipakai, maka <b>setiap kuitansi historis</b>
	 * yang memakai akun itu — begitu dirender atau disentuh Hibernate — akan mencatat
	 * {@code dariTabungan} sebesar seluruh nominalnya dan menyimpannya. Saldo tabungan seluruh
	 * siswa terkait langsung tergerus sebesar total pembayaran historis mereka, tanpa satu pun
	 * transaksi penarikan yang nyata dan tanpa layar peninjauan. Melepas flag kembali <b>tidak</b>
	 * memulihkan angka yang sudah tersimpan.</p>
	 *
	 * @return besar pembayaran yang berasal dari tabungan; {@code 0.0} bila tidak ada
	 */
	public Double getDariTabungan() {

		if (getBniRequest() != null && getBniRequest().getDeposit() > 0.1) {
			dariTabungan = getBniRequest().getDeposit();
		} else if (getVirtualAccountBank() != null && getVirtualAccountBank().getTabungan() > 0.1) {
			dariTabungan = getVirtualAccountBank().getTabungan();
		} else if (getDariTabunganManual() != null && getDariTabunganManual() > 0.1) {
			dariTabungan = getDariTabunganManual();
		} else if (getAkunPembayaranSiswa() != null && getAkunPembayaranSiswa().getDariTabungan()) {
			dariTabungan = getNominal();
		} else {
			dariTabungan = 0.0;
		}
		return dariTabungan == null ? 0.0 : dariTabungan;
	}

	/**
	 * Menyetel besar pembayaran yang berasal dari tabungan. Akan dihitung ulang (dan ditimpa)
	 * oleh {@link #getDariTabungan()} pada pembacaan berikutnya.
	 *
	 * @param dariTabungan besar pembayaran dari tabungan
	 */
	public void setDariTabungan(Double dariTabungan) {
		this.dariTabungan = dariTabungan;
	}

	/**
	 * Mengembalikan besar pembayaran dari tabungan yang <b>diketik manual</b> oleh operator.
	 * Aksesor polos.
	 *
	 * <p>Nilai ini menjadi cabang ketiga pada {@link #getDariTabungan()}: ia menang atas flag
	 * cara pembayaran, tetapi kalah dari nilai yang dilaporkan kanal bank
	 * ({@code bniRequest}/{@code virtualAccountBank}).</p>
	 *
	 * @return besar pembayaran dari tabungan versi manual; {@code null} bila tidak diisi
	 */
	public Double getDariTabunganManual() {
		return dariTabunganManual;
	}

	/**
	 * Menyetel besar pembayaran dari tabungan versi manual. Aksesor polos.
	 *
	 * @param dariTabunganManual besar pembayaran dari tabungan yang diketik operator
	 */
	public void setDariTabunganManual(Double dariTabunganManual) {
		this.dariTabunganManual = dariTabunganManual;
	}

	/**
	 * Mengembalikan nominal kuitansi yang <b>diketik manual</b> oleh operator. Aksesor polos.
	 *
	 * <p>Bila nilainya &gt; 0,1, {@link #getNominal()} akan menimpa {@code nominal} dengannya —
	 * angka operator selalu menang atas angka hasil hitungan sistem, dan penimpaan itu permanen.</p>
	 *
	 * @return nominal versi manual; {@code null} bila tidak diisi
	 */
	public Double getNominalManual() {
		return nominalManual;
	}

	/**
	 * Menyetel nominal kuitansi versi manual. Nilai &gt; 0,1 akan menimpa {@code nominal} lewat
	 * {@link #getNominal()}.
	 *
	 * @param nominalManual nominal versi manual
	 */
	public void setNominalManual(Double nominalManual) {
		this.nominalManual = nominalManual;
	}

}
