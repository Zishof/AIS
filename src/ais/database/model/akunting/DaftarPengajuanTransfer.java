package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.ProsesTransferAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.asset.PembayaranDpMasterAssetDetail;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.payroll.PengajuanTransaksiPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>Daftar Pengajuan Transfer</b> (DPT, di layar sering disebut <b>DPC</b> —
 * "Daftar Pengajuan Cair") pada tabel <code>akunting.daftar_pengajuan_transfer</code>.
 *
 * <h3>Peran dalam alur akunting</h3>
 * <p>Class ini adalah <b>kolam antrean pembayaran (payment pool) terpusat</b>. Segala dokumen
 * di seluruh modul yang pada akhirnya menuntut uang keluar dari kas/bank lembaga —
 * pengadaan aset, uang muka, kas besar, kas kecil, reimbursement pegawai, pajak, diskon
 * siswa, transaksi koperasi, hutang supplier toko, transaksi pegawai — <b>tidak</b>
 * dibayarkan langsung dari layarnya masing-masing. Setiap dokumen "menitipkan" satu baris
 * di tabel ini, lalu bagian keuangan memproses baris-baris itu secara kolektif lewat menu
 * <i>Pembayaran Transfer</i>. Alurnya:</p>
 * <ol>
 *   <li><b>Dokumen sumber disetujui</b> (SOP/disposisi selesai) → modul pemilik dokumen
 *   memanggil salah satu method statis <code>simpanXxx(...)</code> di class ini.</li>
 *   <li><b>Baris DPT dibuat</b> (idempoten) dan <b>ditautkan dua arah</b>: kolom relasi di
 *   baris DPT menunjuk dokumen sumber, dan kolom <code>daftar_pengajuan_transfer</code> di
 *   dokumen sumber menunjuk balik ke baris DPT. Tautan balik inilah yang dipakai sebagai
 *   penjaga idempotensi ("sudah pernah dibuat → berhenti").</li>
 *   <li><b>Petugas keuangan memilih beberapa baris</b> DPT di menu Pembayaran Transfer dan
 *   membungkusnya menjadi satu {@link ProsesTransfer} (satu batch transfer bank, satu
 *   {@code CaraPembayaranTransfer}). Lihat
 *   {@code ais.action.master.akunting.ProsesTransferAction} dan
 *   {@code ais.common.newui.akunting.NewUiTransferWorkflowService#createProcess}.</li>
 *   <li><b>ProsesTransfer direalisasikan</b> (uang benar-benar ditransfer) → jurnal
 *   diposting, {@link #getPostingHistory()} terisi, dan {@link #getTransfer()} menjadi
 *   {@code true} permanen.</li>
 * </ol>
 *
 * <h3>Bentuk data: satu baris = satu dokumen sumber, tapi 17 kemungkinan tipe</h3>
 * <p>Baris DPT <b>tidak menyimpan</b> nominal/rekening/akun/tanggal miliknya sendiri sebagai
 * data primer. Ia hanya memegang <b>satu</b> (dan hanya satu) relasi ke dokumen sumber dari
 * daftar berikut, lalu <b>menurunkan</b> seluruh atribut tampilannya dari dokumen itu:</p>
 * <ul>
 *   <li>{@link #getSaldoAwalMasterAsset()} — tagihan pengadaan aset ke penyedia/vendor</li>
 *   <li>{@link #getPembayaranPengadaanMasterAssetDetail()} — pembayaran pengadaan</li>
 *   <li>{@link #getPembayaranTerminMasterAssetDetail()} — pembayaran termin kontrak</li>
 *   <li>{@link #getPembayaranDpMasterAssetDetail()} — pembayaran DP pemesanan</li>
 *   <li>{@link #getUangMuka()} dan {@link #getPertangungjawaban()} (pengembalian sisa UM)</li>
 *   <li>{@link #getKasBesar()} dan {@link #getPertangungjawabanKasBesar()}</li>
 *   <li>{@link #getPenggantianKasKecil()} dan {@link #getJenisKasKecil()} (saldo awal KK)</li>
 *   <li>{@link #getDanaTalangan()}</li>
 *   <li>{@link #getReimbursementPegawai()}</li>
 *   <li>{@link #getPajak()} — baris PPh yang dipotong (lihat "netto vs bruto" di bawah)</li>
 *   <li>{@link #getDiskonTagihan()} — diskon siswa yang dibayar tunai (tidak memotong
 *   tagihan)</li>
 *   <li>{@link #getPengajuanTransaksiPegawai()} (payroll)</li>
 *   <li>{@link #getTransaksiKoperasi()} — khusus produk bertipe PINJAMAN</li>
 *   <li>{@link #getPembayaranHutangSupplier()} — hutang supplier modul toko/koperasi</li>
 * </ul>
 *
 * <h3>HAL PALING NON-OBVIOUS: getter di class ini bukan getter polos</h3>
 * <p>Hampir semua properti skalar/relasi "hasil turunan" di sini di-<i>resolve ulang setiap
 * kali dibaca</i> lewat rantai <code>if / else if</code> panjang yang memeriksa dokumen
 * sumber mana yang terisi, lalu <b>menulis balik hasilnya ke field</b>. Karena class ini
 * memakai <b>property access</b> (anotasi Hibernate menempel di getter), penulisan balik itu
 * ikut terbaca oleh <i>dirty checking</i> Hibernate dan <b>tersimpan permanen ke database</b>
 * pada flush berikutnya. Getter yang berperilaku begitu:</p>
 * <ul>
 *   <li>{@link #getKode()}, {@link #getNominal()}, {@link #getWaktu()},
 *   {@link #getAkun()}, {@link #getBankSumber()}, {@link #getAtasNamaSumber()},
 *   {@link #getNoRekSumber()}, {@link #getSatuanKerja()}, {@link #getDisposisiSop()},
 *   {@link #getKodeUnik()}</li>
 *   <li>tiga flag status satu-arah: {@link #getAktif()}, {@link #getTransfer()},
 *   {@link #getTransitori()}</li>
 * </ul>
 * <p>Praktisnya baris DPT berlaku sebagai <b>materialized view / cache denormalisasi</b> atas
 * dokumen sumbernya: kolomnya ada di database supaya bisa di-<i>query</i>, di-<i>sort</i>,
 * dan difilter di level SQL (mis. filter tanggal <code>date(this_.waktu) between ...</code>),
 * tetapi nilainya selalu ditimpa ulang dari sumber saat object dibaca di Java. Konsekuensi
 * yang mudah menjebak: mengubah kolom-kolom itu langsung lewat SQL/setter <b>tidak bertahan</b>,
 * karena pembacaan berikutnya akan menimpanya lagi.</p>
 *
 * <h3>Pola "flag <code>aktif</code> satu-arah" — HASIL VERIFIKASI DI FILE INI</h3>
 * <p>Dugaan dari sesi {@code PengajuanMahasiswa} <b>TERKONFIRMASI</b> untuk class ini, dengan
 * satu pengecualian penting. Rinciannya:</p>
 * <ul>
 *   <li>{@link #getAktif()} hanya pernah <b>menugaskan</b> {@code aktif = false} (dua cabang:
 *   disposisi SOP tidak aktif, atau alur SOP berakhir di titik penolakan). <b>Tidak ada satu
 *   pun cabang yang menugaskan kembali {@code true}</b> — tidak ada {@code else} pemulih.</li>
 *   <li>Kolom <code>aktif</code> benar-benar dipetakan dan dipakai di level SQL: filter
 *   standar daftar DPC adalah
 *   <code>Restrictions.or(isNull("aktif"), eq("aktif", true))</code>
 *   (lihat {@code DaftarPengajuanTransferAction} dan
 *   {@code AmbilDataDaftarPengajuanTransferBanyak}). Jadi <b>{@code NULL} berarti aktif</b>,
 *   dan begitu {@code false} tersimpan, baris hilang dari daftar di level query.</li>
 *   <li>Karena getter yang menulis {@code false} itu sekaligus getter properti Hibernate,
 *   nilai {@code false} tersebut <b>ikut ter-flush ke DB</b> — tidak sekadar in-memory.</li>
 *   <li><b>Pengecualian (beda dari class sejenis):</b> ada satu jalur pemulih di luar class
 *   ini, yaitu {@code ais.action.master.asset.helper.BreakdownTagihanVendorHelper}, yang
 *   memanggil {@code setAktif(true)}/{@code setAktif(!breakdown)} saat mode "breakdown
 *   tagihan vendor" di-<i>toggle</i>. Jalur itu hanya menyentuh baris DPT bertipe
 *   {@link Pajak}. Untuk 16 tipe dokumen sumber lainnya, <b>tidak ada</b> jalur yang
 *   mengembalikan {@code aktif} menjadi {@code true}.</li>
 * </ul>
 * <p><b>Akibat praktis yang perlu diketahui:</b> {@link #getDisposisiSop()} sendiri bersifat
 * turunan — ia mengambil ulang disposisi terkini dari dokumen sumber. Bila sebuah dokumen
 * ditolak (baris DPT dimatikan) lalu <b>diajukan ulang</b> dengan disposisi baru yang sehat,
 * {@code getAktif()} memang tidak akan menulis {@code false} lagi — tetapi nilai {@code false}
 * yang sudah tersimpan dari penolakan sebelumnya tetap dikembalikan apa adanya, sehingga baris
 * itu <b>tetap tidak muncul</b> di daftar pembayaran. Ini dicatat sebagai kuirk, bukan
 * diperbaiki di sini.</p>
 * <p>Dua flag lain berperilaku satu-arah serupa: {@link #getTransfer()} hanya bisa menyala
 * (tidak pernah dimatikan oleh getter) dan {@link #getTransitori()} hanya bisa dipadamkan.</p>
 *
 * <h3>Netto vs bruto (potongan PPh)</h3>
 * <p>Untuk pembayaran vendor pengadaan dan termin, {@link #getNominal()} mengembalikan nilai
 * <b>netto</b> = bruto dikurangi PPh yang dipotong. PPh-nya dibayarkan lewat <b>baris DPT
 * terpisah</b> bertipe {@link Pajak} (dibuat {@link #simpanPajak(Pajak)}), sehingga berlaku
 * invarian <code>netto(vendor) + Σ(baris pajak) = bruto</code>. Detail perhitungannya ada di
 * {@link #hitungTotalPphSaldoAwal(SaldoAwalMasterAsset)}, termasuk mode "breakdown" yang
 * memakai satu nilai Bukti Potong manual, bukan penjumlahan per detail PO.</p>
 *
 * <h3>Pengelompokan anggota class</h3>
 * <ol>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()} beserta {@code onUpdate()}.</li>
 *   <li><b>Utilitas session</b> — {@link #closeNativeSessionSafely(Session)}.</li>
 *   <li><b>Utilitas UI</b> — {@link #tampilStatus(DaftarPengajuanTransfer, Vbox)} merender
 *   ringkasan status SOP/transfer sebagai komponen ZK.</li>
 *   <li><b>Pabrik statis <code>simpanXxx(...)</code></b> — 14 method, satu per tipe dokumen
 *   sumber; semuanya idempoten dan mengelola transaksinya sendiri.</li>
 *   <li><b>Properti turunan</b> — getter cerdas yang menurunkan nilai dari dokumen sumber
 *   (lihat bagian "getter bukan getter polos" di atas).</li>
 *   <li><b>Relasi dokumen sumber</b> — pasangan getter/setter {@code @ManyToOne} biasa.</li>
 * </ol>
 *
 * <h3>Catatan teknis pewarisan</h3>
 * <p>Class ini {@code extends} {@link DataSop} (abstrak, mewajibkan pasangan
 * {@code getDisposisiSop}/{@code setDisposisiSop}), yang pada gilirannya
 * {@code extends} {@link ais.database.model.GeneralValueObject}.
 * <b>{@code GeneralValueObject} bukan {@code @Entity} maupun
 * {@code @MappedSuperclass}</b> — ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b>
 * memetakan properti induknya. Karena itu field {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>sengaja dideklarasikan ulang</b> di sini; itu keharusan
 * teknis, bukan duplikasi yang keliru. Method utilitas warisan yang tetap dipakai adalah
 * {@code check(...)} (resolusi proxy lazy) — lihat penjelasan lengkapnya di
 * {@link ais.database.model.GeneralValueObject#check(Object)}.</p>
 *
 * @see ProsesTransfer
 * @see DisposisiSop
 * @see ais.database.model.GeneralValueObject
 * @see DataSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "daftar_pengajuan_transfer")
public class DaftarPengajuanTransfer extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel, {@code IDENTITY}. Dideklarasikan ulang karena induk tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit). @see #setOleh(String) */
	private String oleh;
	/** ID/username pengguna terakhir yang mengubah baris ini (jejak audit). @see #setOlehId(String) */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID/username pelaku perubahan, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pelaku perubahan.
	 *
	 * <p><b>Tidak polos:</b> nilai {@code null}/kosong <b>diabaikan diam-diam</b> — field lama
	 * dipertahankan. Ini disengaja agar jejak audit yang sudah ada tidak terhapus oleh proses
	 * batch/latar yang tidak punya konteks pengguna. Konsekuensinya field ini tidak bisa
	 * "dikosongkan" lewat setter.</p>
	 *
	 * @param olehId ID/username pelaku perubahan; {@code null} atau string kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pelaku perubahan. Bersifat sama dengan
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan agar jejak audit lama
	 * tidak tertimpa.
	 *
	 * @param oleh nama pelaku perubahan; {@code null} atau string kosong diabaikan
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
	 * @return nama pelaku perubahan, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memutakhirkan stempel waktu audit tepat sebelum baris
	 * di-{@code UPDATE}, dengan mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang sama dengan
	 * method ini (pola sisipan otomatis di seluruh entity AIS) — jangan dipisah tanpa alasan
	 * agar diff antar-entity tetap seragam. Nilai awalnya waktu server saat object dibuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah waktu perubahan; biasanya dibiarkan diisi otomatis oleh
	 *                        {@code onUpdate()}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir (tidak pernah {@code null}: diinisialisasi saat object
	 *         dibuat)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log dan komponen UI ZK yang menampilkan object apa adanya.
	 *
	 * <p>Membaca field {@code nama} <b>langsung</b>, bukan lewat {@link #getNama()}, sehingga
	 * tidak ikut men-{@code trim} spasi dan tidak memicu resolusi apa pun.</p>
	 *
	 * @return string berbentuk {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode dokumen. <b>Turunan</b>: selalu ditimpa ulang dari dokumen sumber oleh
	 * {@link #getKode()}.
	 */
	private String kode;
	/** Uraian baris DPT, diisi oleh method {@code simpanXxx(...)} pembuatnya. */
	private String nama;
	/** Keterangan bebas, diisi manual dari layar. Satu-satunya field teks yang tidak diturunkan. */
	private String keterangan;
	/**
	 * Flag baris masih layak dibayar. <b>{@code null} berarti AKTIF</b> (filter daftar memakai
	 * {@code isNull OR eq true}). Lihat pembahasan "flag satu-arah" di Javadoc class dan
	 * {@link #getAktif()}.
	 */
	private Boolean aktif;
	/** Nominal yang akan ditransfer. <b>Turunan</b> — lihat {@link #getNominal()}. */
	private Double nominal;

	/**
	 * Bank rekening <b>TUJUAN</b> transfer. Nama field menyesatkan ("sumber"), lihat
	 * {@link #getBankSumber()}.
	 */
	private Bank bankSumber;
	/** Akun buku besar lawan (utang/biaya) untuk penjurnalan. <b>Turunan</b> — {@link #getAkun()}. */
	private Akun akun;
	/** Nama pemilik rekening TUJUAN. <b>Turunan</b> — {@link #getAtasNamaSumber()}. */
	private String atasNamaSumber;
	/** Nomor rekening TUJUAN. <b>Turunan</b> — {@link #getNoRekSumber()}. */
	private String noRekSumber;
	/** Batch transfer yang memuat baris ini; {@code null} selama baris masih menunggu diproses. */
	private ProsesTransfer prosesTransfer;
	/** Data transitori bila pembayaran ditempuh lewat rekening perantara, bukan transfer langsung. */
	private Transitori transitoriData;
	/**
	 * Menandai baris diselesaikan lewat transfer bank. Default {@code true} pada object baru,
	 * namun {@link #getTransfer()} mengembalikan {@code false} bila field bernilai {@code null}
	 * (baris lama hasil migrasi).
	 */
	private Boolean transfer = true;
	/** Menandai baris diselesaikan lewat mekanisme transitori. Saling eksklusif dengan {@code transfer}. */
	private Boolean transitori = false;
	// reference
	/** Dokumen sumber: pertanggungjawaban uang muka (baris ini = pengembalian sisa UM). */
	private Pertangungjawaban pertangungjawaban;
	/** Dokumen sumber: pengajuan uang muka kegiatan/kerja. */
	private UangMuka uangMuka;
	/** Dokumen sumber: reimbursement pegawai (tujuan transfer = rekening pegawai). */
	private ReimbursementPegawai reimbursementPegawai;
	/** Dokumen sumber: dana talangan atas sebuah uang muka. */
	private DanaTalangan danaTalangan;
	/** Dokumen sumber: penggantian (pengisian ulang) kas kecil. */
	private PenggantianKasKecil penggantianKasKecil;
	/** Dokumen sumber: pengajuan kas besar. */
	private KasBesar kasBesar;
	/** Dokumen sumber: pertanggungjawaban kas besar (baris ini = pengembalian sisa). */
	private PertangungjawabanKasBesar pertangungjawabanKasBesar;
	/** Dokumen sumber: detail pembayaran pengadaan aset. */
	private PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail;
	/** Dokumen sumber: tagihan pengadaan/saldo awal aset dari penyedia (vendor). */
	private SaldoAwalMasterAsset saldoAwalMasterAsset;
	/** Dokumen sumber: detail pembayaran termin perjanjian kerja sama. */
	private PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail;
	/** Dokumen sumber: detail pembayaran DP pemesanan barang/jasa. */
	private PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail;
	/** Dokumen sumber: jenis kas kecil — baris ini mewakili penyerahan SALDO AWAL kas kecil. */
	private JenisKasKecil jenisKasKecil;
	/** Dokumen sumber: pengajuan transaksi pegawai (modul payroll). */
	private PengajuanTransaksiPegawai pengajuanTransaksiPegawai;
	/** Dokumen sumber: transaksi koperasi — hanya untuk produk bertipe PINJAMAN. */
	private TransaksiKoperasi transaksiKoperasi;
	/**
	 * Disposisi SOP yang mengendalikan status persetujuan baris ini. <b>Turunan</b> dari
	 * disposisi dokumen sumber — lihat {@link #getDisposisiSop()}.
	 */
	private DisposisiSop disposisiSop;
	/** Tanggal acuan baris (dipakai filter rentang tanggal di SQL). <b>Turunan</b> — {@link #getWaktu()}. */
	private Date waktu;
	/** Satuan kerja pemilik anggaran. <b>Turunan</b> — {@link #ambilSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/**
	 * Dokumen sumber: tagihan siswa yang memuat diskon TIDAK LANGSUNG (diskon yang dibayarkan
	 * tunai, bukan memotong tagihan).
	 */
	private Tagihan diskonTagihan;
	/** Dokumen sumber: baris PPh yang dipotong dan harus disetorkan (lihat "netto vs bruto"). */
	private Pajak pajak;
	/** Jejak posting jurnal setelah transfer direalisasikan; {@code null} bila belum diposting. */
	private PostingHistory postingHistory;

	/**
	 * Menutup {@code Session} Hibernate <b>native</b> (hasil
	 * {@code HibernateUtil.currentNativeSession()}) tanpa pernah melempar exception.
	 *
	 * <p>Dipakai oleh seluruh method statis {@code simpanXxx(...)} di class ini. Berbeda dari
	 * {@code HibernateUtil.currentSession()} yang siklus hidupnya diurus filter per-request,
	 * session native dibuka dan <b>wajib ditutup sendiri</b> oleh pemanggil — kalau tidak,
	 * koneksi bocor dari pool.</p>
	 *
	 * <p>Urutan pembersihannya sengaja bertahap dan masing-masing dibungkus {@code try/catch}
	 * sendiri supaya kegagalan satu langkah tidak menggagalkan langkah berikutnya:</p>
	 * <ol>
	 *   <li><b>rollback</b> transaksi bila masih aktif — mencegah transaksi menggantung
	 *   membekukan baris (lock) sampai timeout;</li>
	 *   <li><b>clear</b> — melepas seluruh entity dari persistence context;</li>
	 *   <li><b>disconnect</b> — mengembalikan koneksi JDBC ke pool;</li>
	 *   <li><b>close</b> — hanya bila session memang masih terbuka.</li>
	 * </ol>
	 *
	 * <p><b>Jebakan yang sudah terbukti:</b> {@code currentNativeSession()} bersifat
	 * <i>thread-local</i>. Bila pemanggil masih memegang session yang sama dan hendak
	 * melanjutkan operasi setelah method {@code simpanXxx} selesai, penutupan di sini membuat
	 * operasi berikutnya gagal dengan "Session is closed" — itulah alasan adanya varian
	 * {@link #simpanPembayaranTerminMasterAssetDetail(PembayaranTerminMasterAssetDetail, Session)}
	 * yang tidak menutup session.</p>
	 *
	 * @param session session native yang hendak dibereskan; {@code null} diabaikan
	 */
	private static void closeNativeSessionSafely(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/DaftarPengajuanTransfer.java:154");
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/DaftarPengajuanTransfer.java:158");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/DaftarPengajuanTransfer.java:162");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/DaftarPengajuanTransfer.java:168");
		}
	}

	/**
	 * Merender ringkasan <b>status alur</b> sebuah baris DPT sebagai deretan komponen ZK di
	 * dalam {@code vbox1}. Dipakai kolom "Status" pada grid Pembayaran Transfer dan pada
	 * berbagai dasbor monitor akunting.
	 *
	 * <p>Keluarannya sampai tiga baris teks, dirakit dalam dua tahap:</p>
	 * <ol>
	 *   <li><b>Riwayat SOP</b> (hanya bila {@code prosesTransfer.disposisiSop} ada). Dua query
	 *   {@code DisposisiAlurSop} terurut {@code id} menurun diambil masing-masing satu baris:
	 *   <ul>
	 *     <li>langkah terakhir yang <b>sudah dikerjakan</b> ({@code diajukanOleh} tidak
	 *     {@code null}) → label "<i>&lt;nama alur&gt; &lt;waktu&gt; oleh &lt;aktor&gt;
	 *     (&lt;user&gt;)</i>";</li>
	 *     <li>langkah yang <b>masih menggantung</b> ({@code diajukanOleh} {@code null}) →
	 *     bila alurnya ditandai {@code penolakanAdaDiSini} maka dirender sebagai
	 *     "<i>Ditolak : ...</i>" berikut identitas pengaju langkah sebelumnya (user, atau
	 *     mahasiswa, atau siswa — mana yang terisi); selain itu dirender
	 *     "<i>Menunggu : &lt;aktor&gt; &lt;nama alur&gt;</i>".</li>
	 *   </ul>
	 *   </li>
	 *   <li><b>Status transfer</b>, tiga kemungkinan yang saling eksklusif:
	 *   <ul>
	 *     <li>sudah masuk batch transfer bank → tautan (<code>&lt;a&gt;</code>) "Status :
	 *     Transfer via ..." yang saat diklik membuka layar
	 *     {@code ProsesTransferAction.onAddExternal(...)} untuk batch tersebut;</li>
	 *     <li>diselesaikan lewat transitori → tautan "Status : Transitori ..." dengan aksi
	 *     klik yang sama;</li>
	 *     <li>belum tertaut batch apa pun → label statis "Status DPC : Belum diproses".</li>
	 *   </ul>
	 *   </li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menempelkan komponen baru ke {@code vbox1} (tidak pernah
	 * membersihkan isi lama — pemanggil yang bertanggung jawab), dan menjalankan dua query
	 * pada {@code HibernateUtil.currentSession()}. Karena dipanggil per baris grid, method ini
	 * berpotensi menjadi sumber N+1 query pada daftar yang panjang.</p>
	 *
	 * <p><i>Kuirk:</i> query kedua memasang {@code Restrictions.isNotNull("alurSop")}
	 * <b>dua kali</b> (sisa salin-tempel); duplikasi itu tidak mengubah hasil.</p>
	 *
	 * @param daftarPengajuanTransfer baris DPT yang statusnya dirender; {@code null} aman —
	 *                                tidak ada komponen yang ditambahkan
	 * @param vbox1                   wadah ZK tempat label/tautan status ditempelkan
	 * @see ProsesTransferAction#onAddExternal(EventListener, ProsesTransfer)
	 */
	public static void tampilStatus(final DaftarPengajuanTransfer daftarPengajuanTransfer, Vbox vbox1) {
		if (daftarPengajuanTransfer != null && daftarPengajuanTransfer.getProsesTransfer() != null
				&& daftarPengajuanTransfer.getProsesTransfer().getDisposisiSop() != null) {

			DisposisiAlurSop disposisiAlurSop = (DisposisiAlurSop) HibernateUtil.currentSession()
					.createCriteria(DisposisiAlurSop.class).add(Restrictions.isNotNull("alurSop"))
					.add(Restrictions.isNotNull("diajukanOleh")).addOrder(Order.desc("id"))
					.add(Restrictions.eq("disposisiSop", daftarPengajuanTransfer.getProsesTransfer().getDisposisiSop()))
					.setMaxResults(1).uniqueResult();
			if (disposisiAlurSop != null) {
				String ds = disposisiAlurSop.getWaktu() == null ? ""
						: " " + Common.dateFormat5.get().format(disposisiAlurSop.getWaktu());
				new MyLabelKecil(disposisiAlurSop.getAlurSop().getNama() + " " + ds + " oleh "
						+ disposisiAlurSop.getAlurSop().getAktor() + " ("
						+ disposisiAlurSop.getDiajukanOleh().getUserNama() + ") ").setParent(vbox1);
			}

			disposisiAlurSop = (DisposisiAlurSop) HibernateUtil.currentSession().createCriteria(DisposisiAlurSop.class)
					.add(Restrictions.isNotNull("alurSop")).add(Restrictions.isNotNull("alurSop"))
					.add(Restrictions.isNull("diajukanOleh")).addOrder(Order.desc("id"))
					.add(Restrictions.eq("disposisiSop", daftarPengajuanTransfer.getProsesTransfer().getDisposisiSop()))
					.setMaxResults(1).uniqueResult();
			if (disposisiAlurSop != null) {

				if (disposisiAlurSop.getAlurSop() != null && disposisiAlurSop.getAlurSop().getPenolakanAdaDiSini()
						&& disposisiAlurSop.getSebelumnya() != null
						&& disposisiAlurSop.getSebelumnya().getAlurSop() != null) {

					String infototal = "Ditolak : " + disposisiAlurSop.getSebelumnya().getAlurSop().getAktor() + " - "
							+ disposisiAlurSop.getSebelumnya().getAlurSop().getNama();
					if (disposisiAlurSop.getSebelumnya().getDiajukanOleh() != null) {
						infototal += " " + disposisiAlurSop.getSebelumnya().getDiajukanOleh().getUserNama();
					} else if (disposisiAlurSop.getSebelumnya().getMahasiswa() != null) {
						infototal += " " + disposisiAlurSop.getSebelumnya().getMahasiswa().getNama();
					} else if (disposisiAlurSop.getSebelumnya().getSiswa() != null) {
						infototal += " " + disposisiAlurSop.getSebelumnya().getSiswa().getNama();
					}

					new MyLabelKecil(infototal).setParent(vbox1);
				}

				else {

					new MyLabelKecil("Menunggu : " + disposisiAlurSop.getAlurSop().getAktor() + " "
							+ disposisiAlurSop.getAlurSop().getNama()).setParent(vbox1);
				}
			}

		}

		if (daftarPengajuanTransfer != null && daftarPengajuanTransfer.getTransfer()
				&& daftarPengajuanTransfer.getProsesTransfer() != null
				&& daftarPengajuanTransfer.getProsesTransfer().getCaraPembayaranTransfer() != null) {
			String ds = daftarPengajuanTransfer.getWaktu() == null ? ""
					: " " + Common.dateFormat5.get().format(daftarPengajuanTransfer.getWaktu());

			A a = new A("Status : Transfer via  "
					+ (daftarPengajuanTransfer.getProsesTransfer().getCaraPembayaranTransfer().getNama()) + " pada "
					+ ds + " (" + daftarPengajuanTransfer.getProsesTransfer().getNama() + " "
					+ daftarPengajuanTransfer.getProsesTransfer().getKode() + ") ");
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ProsesTransferAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, daftarPengajuanTransfer.getProsesTransfer());

				}
			});
			a.setStyle("font-size:9px;");
			a.setParent(vbox1);

		} else if (daftarPengajuanTransfer != null && daftarPengajuanTransfer.getTransitori()
				&& daftarPengajuanTransfer.getProsesTransfer() != null) {

			A a = new A("Status : Transitori  " + daftarPengajuanTransfer.getProsesTransfer().getNama() + " "
					+ daftarPengajuanTransfer.getProsesTransfer().getKode());
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ProsesTransferAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, daftarPengajuanTransfer.getProsesTransfer());

				}
			});
			a.setStyle("font-size:9px;");
			a.setParent(vbox1);

		} else if (daftarPengajuanTransfer != null && daftarPengajuanTransfer.getProsesTransfer() == null) {
			new MyLabelKecil("Status DPC : Belum diproses").setParent(vbox1);
		}

	}

	/**
	 * Mendaftarkan pembayaran <b>DP pemesanan barang/jasa</b> ke kolam antrean pembayaran.
	 *
	 * <p>Ini adalah <b>anggota pertama dari keluarga 14 method {@code simpanXxx(...)}</b> yang
	 * semuanya mengikuti resep identik berikut — penjelasan detail resep ini berlaku juga bagi
	 * saudara-saudaranya, yang Javadoc-nya hanya menyorot kekhasan masing-masing:</p>
	 * <ol>
	 *   <li><b>Gerbang idempotensi</b>: bila dokumen sumber sudah punya tautan balik
	 *   {@code getDaftarPengajuanTransfer() != null}, method berhenti tanpa efek apa pun.
	 *   Aman dipanggil berulang dari {@code onSave}, dari penjadwal latar, maupun dari helper
	 *   sinkronisasi.</li>
	 *   <li><b>Buka session native</b> {@code HibernateUtil.currentNativeSession()} —
	 *   <i>bukan</i> session request, karena method ini sering dipanggil dari luar konteks
	 *   request ZK.</li>
	 *   <li><b>Cari-atau-buat</b>: query {@code Criteria} mencari baris DPT lama yang sudah
	 *   menunjuk dokumen ini (menangani kasus tautan balik hilang tetapi barisnya masih ada);
	 *   bila tidak ada, instance baru dibuat.</li>
	 *   <li><b>Isi relasi + nama</b> yang akan tampil di daftar pembayaran. Seluruh atribut
	 *   lain (nominal, rekening, akun, waktu, satuan kerja) <b>sengaja tidak diisi</b> karena
	 *   diturunkan otomatis oleh getter — lihat Javadoc class.</li>
	 *   <li><b>Dua transaksi terpisah</b>: pertama menyimpan baris DPT (agar dapat ID), kedua
	 *   menyimpan tautan balik di dokumen sumber. Dipisah karena tautan balik butuh ID yang
	 *   baru ada setelah commit pertama.</li>
	 *   <li><b>Tutup session</b> lewat {@link #closeNativeSessionSafely(Session)}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis satu baris di {@code akunting.daftar_pengajuan_transfer}
	 * dan meng-{@code UPDATE} satu kolom di tabel dokumen sumber; menutup session native
	 * thread-local (lihat peringatan pada {@link #closeNativeSessionSafely(Session)}).</p>
	 *
	 * @param pembayaranDpMasterAssetDetail detail pembayaran DP yang hendak diantrekan;
	 *                                      diabaikan bila sudah punya baris DPT. Perhatikan
	 *                                      argumen {@code null} <b>tidak</b> aman di sini —
	 *                                      gerbang idempotensi meloloskannya dan
	 *                                      {@code setNama(...)} akan melempar
	 *                                      {@code NullPointerException}
	 */
	public static void simpanPembayaranDpMasterAssetDetail(
			PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail) {

		if (pembayaranDpMasterAssetDetail != null
				&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("pembayaranDpMasterAssetDetail", pembayaranDpMasterAssetDetail)).setMaxResults(1)
				.uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setPembayaranDpMasterAssetDetail(pembayaranDpMasterAssetDetail);
		daftarPengajuanTransfer.setNama("Pembayaran DP pemesanan barang atau jasa "
				+ pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset().getKodeInvoice());

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		pembayaranDpMasterAssetDetail.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, pembayaranDpMasterAssetDetail);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Mendaftarkan pembayaran <b>termin perjanjian kerja sama</b> ke kolam antrean pembayaran,
	 * memakai session native miliknya sendiri.
	 *
	 * <p>Method ini hanya pembungkus tipis: ia membuka
	 * {@code HibernateUtil.currentNativeSession()}, mendelegasikan seluruh pekerjaan ke
	 * {@link #simpanPembayaranTerminMasterAssetDetail(PembayaranTerminMasterAssetDetail, Session)},
	 * lalu <b>menutup session itu</b>.</p>
	 *
	 * <p><b>Pilih varian yang benar.</b> Karena session native bersifat thread-local, pemanggil
	 * yang masih akan memakai session yang sama setelah pemanggilan ini <b>wajib</b> memakai
	 * varian dua-argumen; kalau tidak, operasi berikutnya gagal dengan "Session is closed"
	 * (kasus nyata: loop {@code onSave} PembayaranTermin).</p>
	 *
	 * @param pembayaranTerminMasterAssetDetail detail termin yang hendak diantrekan
	 * @see #simpanPembayaranTerminMasterAssetDetail(PembayaranTerminMasterAssetDetail, Session)
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanPembayaranTerminMasterAssetDetail(
			PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail) {

		/* PENTING: currentNativeSession() bersifat thread-local; bila caller
		 * masih memegang session yang sama (mis. loop onSave PembayaranTermin),
		 * menutupnya di sini membuat session.getTransaction() caller berikutnya
		 * gagal "Session is closed". Caller seperti itu wajib memakai varian
		 * (detail, session) di bawah. */
		Session session = HibernateUtil.currentNativeSession();
		simpanPembayaranTerminMasterAssetDetail(pembayaranTerminMasterAssetDetail, session);

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Varian yang memakai session milik pemanggil dan TIDAK pernah
	 * menutupnya. Gunakan ini bila pemanggil masih melanjutkan operasi
	 * pada session yang sama setelah method ini selesai.
	 *
	 * <p>Di sinilah logika sesungguhnya berada: gerbang idempotensi (berhenti bila detail
	 * termin sudah punya baris DPT), cari-atau-buat baris DPT lewat {@code Criteria}, isi
	 * relasi dan nama ("Pembayaran termin perjanjian kerjasama &lt;kode invoice&gt;"), lalu
	 * simpan dalam dua transaksi terpisah — baris DPT dulu supaya mendapat ID, baru tautan
	 * balik di detail termin.</p>
	 *
	 * <p><b>Efek samping:</b> membuka dan me-{@code commit} dua transaksi pada session milik
	 * pemanggil. Bila pemanggil sudah punya transaksi aktif sendiri, transaksi itu akan
	 * ter-{@code commit} lebih awal oleh method ini — perlu diperhatikan saat memanggilnya di
	 * tengah unit kerja yang lebih besar.</p>
	 *
	 * @param pembayaranTerminMasterAssetDetail detail termin yang hendak diantrekan; diabaikan
	 *                                          bila sudah punya baris DPT
	 * @param session                           session Hibernate milik pemanggil; <b>tidak</b>
	 *                                          ditutup oleh method ini
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanPembayaranTerminMasterAssetDetail(
			PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail, Session session) {

		if (pembayaranTerminMasterAssetDetail != null
				&& pembayaranTerminMasterAssetDetail.getDaftarPengajuanTransfer() != null) {
			return;
		}

		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("pembayaranTerminMasterAssetDetail", pembayaranTerminMasterAssetDetail))
				.setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setPembayaranTerminMasterAssetDetail(pembayaranTerminMasterAssetDetail);
		daftarPengajuanTransfer.setNama("Pembayaran termin perjanjian kerjasama "
				+ pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset().getKodeInvoice());

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		pembayaranTerminMasterAssetDetail.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, pembayaranTerminMasterAssetDetail);
		session.getTransaction().commit();
	}

	/**
	 * Mendaftarkan pembayaran <b>pengadaan barang/jasa</b> ke kolam antrean pembayaran.
	 *
	 * <p>Mengikuti resep {@code simpanXxx} standar. Kekhasannya ada pada penyusunan nama:
	 * nomor dokumen diambil dari {@code penerimaanPengadaanMasterAsset.kodeTagihan}, dan bila
	 * penerimaan belum ada — kasus barang yang masuk lewat saldo awal — dipakai
	 * {@code saldoAwalMasterAsset.kode} sebagai gantinya.</p>
	 *
	 * @param pembayaranPengadaanMasterAssetDetail detail pembayaran pengadaan yang hendak
	 *                                             diantrekan; diabaikan bila sudah punya baris
	 *                                             DPT
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanPembayaranPengadaanMasterAssetDetail(
			PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail) {

		if (pembayaranPengadaanMasterAssetDetail != null
				&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("pembayaranPengadaanMasterAssetDetail", pembayaranPengadaanMasterAssetDetail))
				.setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setPembayaranPengadaanMasterAssetDetail(pembayaranPengadaanMasterAssetDetail);
		daftarPengajuanTransfer.setNama("Pembayaran pengadaan barang atau jasa "
				+ (pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() == null
						? pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset().getKode()
						: pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKodeTagihan()));

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		pembayaranPengadaanMasterAssetDetail.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, pembayaranPengadaanMasterAssetDetail);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Mendaftarkan <b>penggantian (pengisian ulang) kas kecil</b> ke kolam antrean pembayaran.
	 *
	 * <p>Mengikuti resep {@code simpanXxx} standar, dengan <b>satu gerbang tambahan</b>: bila
	 * kas kecil yang bersangkutan ditandai {@code merupakanPenutupanKasKecil}, method berhenti
	 * tanpa membuat baris DPT. Alasannya, penutupan kas kecil adalah pengembalian dana
	 * <i>ke</i> lembaga, bukan pengeluaran <i>dari</i> lembaga — jadi tidak ada yang perlu
	 * ditransfer.</p>
	 *
	 * @param penggantianKasKecil dokumen penggantian kas kecil; diabaikan bila sudah punya
	 *                            baris DPT atau bila kas kecilnya merupakan penutupan
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanPenggantianKasKecil(PenggantianKasKecil penggantianKasKecil) {

		if (penggantianKasKecil != null   && penggantianKasKecil.getDaftarPengajuanTransfer() != null) {
			return;
		}

		if (penggantianKasKecil.getKasKecil() != null
				&& penggantianKasKecil.getKasKecil().getMerupakanPenutupanKasKecil()) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("penggantianKasKecil", penggantianKasKecil)).setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setPenggantianKasKecil(penggantianKasKecil);
		daftarPengajuanTransfer.setNama("Pembayaran penggantian kas kecil " + penggantianKasKecil.getNama());

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		penggantianKasKecil.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, penggantianKasKecil);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Mendaftarkan pengajuan <b>kas besar</b> ke kolam antrean pembayaran.
	 *
	 * <p>Resep {@code simpanXxx} standar tanpa gerbang tambahan; nama baris disusun sebagai
	 * "Pembayaran kas besar &lt;nama kas besar&gt;".</p>
	 *
	 * @param kasBesar dokumen kas besar yang hendak diantrekan; diabaikan bila sudah punya
	 *                 baris DPT
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanKasBesar(KasBesar kasBesar) {

		if (kasBesar != null && kasBesar.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.eq("kasBesar", kasBesar))
				.setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setKasBesar(kasBesar);
		daftarPengajuanTransfer.setNama("Pembayaran kas besar " + kasBesar.getNama());

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		kasBesar.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, kasBesar);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Mendaftarkan <b>dana talangan</b> (pinjaman sementara atas sebuah uang muka) ke kolam
	 * antrean pembayaran. Nama baris memuat nama dana talangan dan nama workspace/kegiatan
	 * uang muka induknya bila ada.
	 *
	 * <p><b>Menyimpang dari resep standar dalam satu hal:</b> tautan balik
	 * {@code danaTalangan.setDaftarPengajuanTransfer(...)} dipasang <b>sebelum</b> baris DPT
	 * disimpan, dan setelah itu <b>hanya baris DPT</b> yang di-{@code saveOrUpdate}
	 * (satu transaksi, bukan dua). Dokumen {@code DanaTalangan} sendiri tidak pernah disimpan
	 * eksplisit di sini, jadi tautan baliknya baru bertahan bila object itu kebetulan masih
	 * <i>attached</i> pada session lain milik pemanggil dan ikut ter-flush. Bila tidak,
	 * tautan balik hilang — namun pemanggilan berikutnya masih menemukan baris DPT lama lewat
	 * langkah "cari-atau-buat", sehingga tidak menghasilkan baris ganda.</p>
	 *
	 * <p><i>Kuirk:</i> penyusunan nama memanggil {@code danaTalangan.getUangMuka()} tanpa
	 * pemeriksaan {@code null}; dana talangan tanpa uang muka induk akan memicu
	 * {@code NullPointerException}.</p>
	 *
	 * @param danaTalangan dokumen dana talangan yang hendak diantrekan; diabaikan bila sudah
	 *                     punya baris DPT
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanDanaTalangan(DanaTalangan danaTalangan) {

		if (danaTalangan != null && danaTalangan.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.eq("danaTalangan", danaTalangan))
				.setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setDanaTalangan(danaTalangan);
		daftarPengajuanTransfer.setNama("Pembayaran dana talangan " + danaTalangan.getNama() + " "
				+ (danaTalangan.getUangMuka().getWorkspace() == null ? ""
						: danaTalangan.getUangMuka().getWorkspace().getNama()));

		danaTalangan.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Mendaftarkan <b>setoran PPh</b> ke kolam antrean pembayaran. Baris hasilnya adalah
	 * pasangan dari baris pembayaran vendor: vendor menerima nominal <i>netto</i>, sedangkan
	 * pajak yang dipotong disetorkan lewat baris tersendiri ini, sehingga berlaku
	 * <code>netto(vendor) + Σ(baris pajak) = bruto</code>.
	 *
	 * <p>Resep {@code simpanXxx} standar, dengan <b>pengamanan transaksi tambahan</b>:
	 * {@code Common.refreshSaveOrUpdate} dapat melakukan <i>rollback</i> sendiri saat gagal
	 * (mis. pelanggaran constraint atau prosedur pemulihan). Bila itu terjadi, memanggil
	 * {@code commit()} tanpa syarat akan melempar "Transaction not successfully started".
	 * Karena itu kedua {@code commit()} di sini dibungkus pemeriksaan
	 * {@code getTransaction().isActive()} — pola yang <b>tidak</b> dipakai saudara-saudaranya
	 * dan sebaiknya ditiru bila kelak menemukan gejala serupa di method lain.</p>
	 *
	 * @param pajak entitas pajak (PPh) yang hendak diantrekan; diabaikan bila sudah punya
	 *              baris DPT
	 * @see #hitungTotalPphSaldoAwal(SaldoAwalMasterAsset)
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanPajak(Pajak pajak) {

		if (pajak != null && pajak.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.eq("pajak", pajak)).setMaxResults(1)
				.uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setPajak(pajak);
		daftarPengajuanTransfer.setNama("Pembayaran pajak " + pajak.getNama() + " "
				+ (pajak.getJenisPajakBarang() == null ? "" : pajak.getJenisPajakBarang().getNama()));

		// Common.refreshSaveOrUpdate bisa MELAKUKAN ROLLBACK sendiri saat gagal (mis. constraint /
		// recover). Bila begitu, memanggil commit() di sini melempar "Transaction not successfully
		// started". Karena itu commit HANYA bila transaksi masih aktif.
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		if (session.getTransaction().isActive()) {
			session.getTransaction().commit();
		}

		pajak.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, pajak);
		if (session.getTransaction().isActive()) {
			session.getTransaction().commit();
		}

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Mendaftarkan <b>tagihan pengadaan aset dari penyedia/vendor</b> ke kolam antrean
	 * pembayaran. Ini jalur DPT yang paling ramai dipakai modul aset.
	 *
	 * <p><b>Gerbang masuk lebih ketat</b> dari resep standar — semua syarat berikut wajib
	 * terpenuhi: argumen tidak {@code null}, sudah tersimpan ({@code getId() != null}), belum
	 * punya baris DPT, dan <b>sudah disetujui</b> ({@code getDisetujuiOleh() != null}). Syarat
	 * terakhir itulah yang mencegah tagihan yang masih draf ikut masuk daftar pembayaran.</p>
	 *
	 * <p><b>Langkah tambahan setelah penyimpanan</b>: bila tagihan ini berasal dari sebuah
	 * pemesanan (PO), akumulasi nilai terbayar PO tersebut dihitung ulang lewat
	 * {@code PemesananPengadaanMasterAsset#hitungDibayar(Session)} dan disimpan ke kolom
	 * {@code dibayar}. Tanpa langkah ini, progres pembayaran PO di layar pengadaan akan
	 * tertinggal.</p>
	 *
	 * <p>Perhatikan tautan balik di sini disimpan dengan {@code Common.refreshUpdate}
	 * (bukan {@code refreshSaveOrUpdate}) karena dokumen sumber dipastikan sudah ber-ID.</p>
	 *
	 * @param saldoAwalMasterAsset tagihan pengadaan yang hendak diantrekan; diabaikan bila
	 *                             {@code null}, belum tersimpan, belum disetujui, atau sudah
	 *                             punya baris DPT
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanSaldoAwalMasterAsset(SaldoAwalMasterAsset saldoAwalMasterAsset) {

		if (saldoAwalMasterAsset == null || saldoAwalMasterAsset.getId() == null
				|| saldoAwalMasterAsset.getDaftarPengajuanTransfer() != null
				|| saldoAwalMasterAsset.getDisetujuiOleh() == null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("saldoAwalMasterAsset", saldoAwalMasterAsset)).setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setSaldoAwalMasterAsset(saldoAwalMasterAsset);
		daftarPengajuanTransfer.setNama("Pembayaran tagihan " + saldoAwalMasterAsset.getKode() + " "
				+ (saldoAwalMasterAsset.getPenyedia() == null ? "" : saldoAwalMasterAsset.getPenyedia().getNama()));

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		saldoAwalMasterAsset.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshUpdate(session, saldoAwalMasterAsset);
		session.getTransaction().commit();

		PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = saldoAwalMasterAsset
				.getPenerimaanPengadaanMasterAsset() == null ? null
						: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset();
		if (pemesananPengadaanMasterAsset != null && pemesananPengadaanMasterAsset.getId() != null) {
			session.refresh(pemesananPengadaanMasterAsset);
			pemesananPengadaanMasterAsset.setDibayar(pemesananPengadaanMasterAsset.hitungDibayar(session));
			session.getTransaction().begin();
			Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
			session.getTransaction().commit();
		}

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Mendaftarkan pencairan <b>uang muka</b> kegiatan/kerja ke kolam antrean pembayaran.
	 * Nama baris memuat nama uang muka dan nama workspace/kegiatan terkait bila ada.
	 *
	 * <p>Resep {@code simpanXxx} standar (dua transaksi terpisah, tautan balik disimpan
	 * eksplisit). Pasangannya di sisi pengembalian sisa dana adalah
	 * {@link #simpanPertangungjawaban(Pertangungjawaban)}.</p>
	 *
	 * @param uangMuka dokumen uang muka yang hendak diantrekan; diabaikan bila sudah punya
	 *                 baris DPT
	 * @see #simpanPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail) untuk penjelasan
	 *      lengkap resep {@code simpanXxx}
	 */
	public static void simpanUangMuka(UangMuka uangMuka) {

		if (uangMuka != null && uangMuka.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.eq("uangMuka", uangMuka))
				.setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setUangMuka(uangMuka);
		daftarPengajuanTransfer.setNama("Pembayaran uang muka " + uangMuka.getNama() + " "
				+ (uangMuka.getWorkspace() == null ? "" : uangMuka.getWorkspace().getNama()));

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		uangMuka.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, uangMuka);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	/**
	 * Pembayaran hutang supplier TOKO (modul Inventory &amp; Sales). Ditambahkan 2026-08-20 supaya
	 * pembayaran ke pemasok toko ikut muncul di menu Pembayaran Transfer, sejajar dengan pembayaran
	 * pengadaan aset yang sudah lebih dulu tertaut. Idempoten seperti simpanXxx lainnya.
	 */
	public static void simpanPembayaranHutangSupplier(
			ais.database.model.koperasi.PembayaranHutangSupplier pembayaranHutangSupplier) {

		if (pembayaranHutangSupplier == null || pembayaranHutangSupplier.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("pembayaranHutangSupplier", pembayaranHutangSupplier)).setMaxResults(1)
				.uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();
		}
		daftarPengajuanTransfer.setPembayaranHutangSupplier(pembayaranHutangSupplier);
		daftarPengajuanTransfer.setNama("Pembayaran hutang supplier toko "
				+ (pembayaranHutangSupplier.getSupplier() == null ? ""
						: pembayaranHutangSupplier.getSupplier().getNama()));

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		pembayaranHutangSupplier.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, pembayaranHutangSupplier);
		session.getTransaction().commit();

		closeNativeSessionSafely(session);
	}

	private ais.database.model.koperasi.PembayaranHutangSupplier pembayaranHutangSupplier;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembayaran_hutang_supplier", nullable = true)
	public ais.database.model.koperasi.PembayaranHutangSupplier getPembayaranHutangSupplier() {
		pembayaranHutangSupplier = check(pembayaranHutangSupplier);
		return pembayaranHutangSupplier;
	}

	public void setPembayaranHutangSupplier(
			ais.database.model.koperasi.PembayaranHutangSupplier pembayaranHutangSupplier) {
		this.pembayaranHutangSupplier = pembayaranHutangSupplier;
	}

	/**
	 * Masukkan reimbursement yang telah DISETUJUI ke daftar DPC (transfer pool).
	 * Idempoten: hanya membuat satu baris DaftarPengajuanTransfer per dokumen dan
	 * menautkan balik lewat {@code daftar_pengajuan_transfer}. Klon
	 * {@link #simpanUangMuka} untuk pola yang identik.
	 */
	public static void simpanReimbursement(ReimbursementPegawai reimbursementPegawai) {

		if (reimbursementPegawai != null && reimbursementPegawai.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("reimbursementPegawai", reimbursementPegawai)).setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();
		}
		daftarPengajuanTransfer.setReimbursementPegawai(reimbursementPegawai);
		daftarPengajuanTransfer.setNama("Pembayaran reimbursement "
				+ (reimbursementPegawai.getNama() == null ? reimbursementPegawai.getKode()
						: reimbursementPegawai.getNama())
				+ " " + (reimbursementPegawai.getPegawai() == null ? ""
						: reimbursementPegawai.getPegawai().getNama()));

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		reimbursementPegawai.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, reimbursementPegawai);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	public static void simpanJenisKasKecil(JenisKasKecil jenisKasKecil) {

		if (jenisKasKecil != null && jenisKasKecil.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class).add(Restrictions.eq("jenisKasKecil", jenisKasKecil))
				.setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();
		}
		daftarPengajuanTransfer.setJenisKasKecil(jenisKasKecil);
		daftarPengajuanTransfer.setNama("Saldo awal " + jenisKasKecil.getNama());

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		jenisKasKecil.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, jenisKasKecil);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	public static void simpanPertangungjawaban(Pertangungjawaban pertangungjawaban) {

		// DIAKTIFKAN (dulu di-nonaktif via if(true)return). Self-gating agar aman dipanggil dari
		// mana pun: hanya buat DPT bila pertangungjawaban SUDAH DISETUJUI, ada dana yang harus
		// dibayarkan kembali (dikembalikan > 0), dan DPT belum ada (idempoten).
		if (pertangungjawaban == null || pertangungjawaban.getDaftarPengajuanTransfer() != null
				|| pertangungjawaban.getDisetujuiOleh() == null) {
			return;
		}
		if (pertangungjawaban.getDikembalikan() == null || pertangungjawaban.getDikembalikan() <= 0.1) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("pertangungjawaban", pertangungjawaban)).setMaxResults(1).uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();
		}
		daftarPengajuanTransfer.setPertangungjawaban(pertangungjawaban);
		daftarPengajuanTransfer.setNama("Pembayaran pengembalian uang muka " + pertangungjawaban.getNama() + " "
				+ (pertangungjawaban.getUangMuka().getWorkspace() == null ? ""
						: pertangungjawaban.getUangMuka().getWorkspace().getNama()));

		pertangungjawaban.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);

	}

	public static void simpanPertangungjawabanKasBesar(PertangungjawabanKasBesar pertangungjawabanKasBesar) {

		// DIAKTIFKAN (dulu di-nonaktif via if(true)return). Self-gating: hanya buat DPT bila
		// pertangungjawaban kas besar SUDAH DISETUJUI, ada dana dikembalikan > 0, dan DPT belum ada.
		if (pertangungjawabanKasBesar == null || pertangungjawabanKasBesar.getDaftarPengajuanTransfer() != null
				|| pertangungjawabanKasBesar.getDisetujuiOleh() == null) {
			return;
		}
		if (pertangungjawabanKasBesar.getDikembalikan() == null
				|| pertangungjawabanKasBesar.getDikembalikan() <= 0.1) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("pertangungjawabanKasBesar", pertangungjawabanKasBesar)).setMaxResults(1)
				.uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();
		}
		daftarPengajuanTransfer.setPertangungjawabanKasBesar(pertangungjawabanKasBesar);
		daftarPengajuanTransfer.setNama("Pembayaran pengembalian kas besar " + pertangungjawabanKasBesar.getNama() + " "
				+ (pertangungjawabanKasBesar.getKasBesar() == null ? ""
						: pertangungjawabanKasBesar.getKasBesar().getNama()));

		pertangungjawabanKasBesar.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);

	}

	public static void simpanDiskonPembayaran(Tagihan diskonTagihan) {

		try {

			if (diskonTagihan.getId() != null && diskonTagihan.getDiskonSiswa() != null
					&& !diskonTagihan.getDiskonSiswa().getMemotongTagihan()
					&& diskonTagihan.getDiskonTidakLangsung() > 0.1) {

				Session session = HibernateUtil.currentNativeSession();
				DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
						.createCriteria(DaftarPengajuanTransfer.class)
						.add(Restrictions.eq("diskonTagihan", diskonTagihan)).setMaxResults(1).uniqueResult();
				if (daftarPengajuanTransfer == null) {
					daftarPengajuanTransfer = new DaftarPengajuanTransfer();
				}
				daftarPengajuanTransfer.setDiskonTagihan(diskonTagihan);
				daftarPengajuanTransfer.setNama("Pembayaran diskon Pembayaran Siswa "
						+ (diskonTagihan.getSiswa() != null ? diskonTagihan.getSiswa().getNama()
								: (diskonTagihan.getCalonSiswa() == null ? ""
										: diskonTagihan.getCalonSiswa().getNama()))
						+ " "
						+ (diskonTagihan.getSiswa() != null ? diskonTagihan.getSiswa().getNomorInduk()
								: (diskonTagihan.getCalonSiswa() == null ? ""
										: diskonTagihan.getCalonSiswa().getNoRegistrasi()))
						+ " " + diskonTagihan.getItemBiayaSekolah().getNama());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
				session.getTransaction().commit();

				// currentNativeSession ditutup manual karena bukan currentSession request.
				closeNativeSessionSafely(session);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/DaftarPengajuanTransfer.java:749");
		}
	}

	public DaftarPengajuanTransfer() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getKode() {
		try {
			if (getSaldoAwalMasterAsset() != null) {
				kode = getSaldoAwalMasterAsset().getKode();
			} else if (getUangMuka() != null) {
				kode = getUangMuka().getKode();
			} else if (getPembayaranHutangSupplier() != null) {
				kode = getPembayaranHutangSupplier().getKodeUnik();
			} else if (getReimbursementPegawai() != null) {
				kode = getReimbursementPegawai().getKode();
			} else if (getPertangungjawaban() != null) {
				kode = getPertangungjawaban().getKode();
			} else if (getPertangungjawabanKasBesar() != null) {
				kode = getPertangungjawabanKasBesar().getKode();
			} else if (getDanaTalangan() != null) {
				kode = getDanaTalangan().getKode();
			} else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getKasKecil() != null) {
				kode = getPenggantianKasKecil().getKasKecil().getKode();
			} else if (getPembayaranPengadaanMasterAssetDetail() != null
					&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset() != null) {
				kode = getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset().getKode();
			} else if (getPembayaranTerminMasterAssetDetail() != null
					&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset() != null) {
				kode = getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getKode();
			} else if (getPembayaranDpMasterAssetDetail() != null
					&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset() != null) {
				kode = getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getKode();
			} else if (getJenisKasKecil() != null) {
				kode = "SKK-" + getJenisKasKecil().getId();
			}

			else if (getKasBesar() != null) {
				kode = getKasBesar().getKode();
			}

			else if (getDiskonTagihan() != null) {
				kode = "DISKON-" + getDiskonTagihan().getId();
			} else if (getPajak() != null) {
				kode = "PAJAK-" + getPajak().getId();
			}

			else if (getPengajuanTransaksiPegawai() != null) {
				kode = getPengajuanTransaksiPegawai().getKode();
			}

			else if (getTransaksiKoperasi() != null) {
				kode = getTransaksiKoperasi().getKode();
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/DaftarPengajuanTransfer.java:812");
			// TODO: handle exception
		}

		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Boolean getAktif() {
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

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_pengadaan_master_asset_detail", nullable = true)
	public PembayaranPengadaanMasterAssetDetail getPembayaranPengadaanMasterAssetDetail() {
		return pembayaranPengadaanMasterAssetDetail;
	}

	public void setPembayaranPengadaanMasterAssetDetail(
			PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail) {
		this.pembayaranPengadaanMasterAssetDetail = pembayaranPengadaanMasterAssetDetail;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban", nullable = true)
	public Pertangungjawaban getPertangungjawaban() {
		return pertangungjawaban;
	}

	public void setPertangungjawaban(Pertangungjawaban pertangungjawaban) {
		this.pertangungjawaban = pertangungjawaban;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "uang_muka", nullable = true)
	public UangMuka getUangMuka() {
		return uangMuka;
	}

	public void setUangMuka(UangMuka uangMuka) {
		this.uangMuka = uangMuka;
	}

	// targetAuditMode NOT_AUDITED WAJIB: ReimbursementPegawai tidak @Audited, sedangkan
	// entity ini @Audited -- tanpa anotasi ini Envers GAGAL init listeners saat build
	// SessionFactory ("An audited relation ... to a not audited entity") dan SELURUH
	// aplikasi mati di startup (terbukti di UAT 2026-08-19; pola insiden hotel.Kamar).
	@Audited(targetAuditMode = org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED)
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "reimbursement_pegawai", nullable = true)
	public ReimbursementPegawai getReimbursementPegawai() {
		return reimbursementPegawai;
	}

	public void setReimbursementPegawai(ReimbursementPegawai reimbursementPegawai) {
		this.reimbursementPegawai = reimbursementPegawai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penggantian_kas_kecil", nullable = true)
	public PenggantianKasKecil getPenggantianKasKecil() {
		return penggantianKasKecil;
	}

	public void setPenggantianKasKecil(PenggantianKasKecil penggantianKasKecil) {
		this.penggantianKasKecil = penggantianKasKecil;
	}

	private static double nilai(Double d) {
		return d == null ? 0.0 : d.doubleValue();
	}

	/**
	 * Total PPh yang dipotong untuk sebuah pengadaan (SaldoAwalMasterAsset).
	 * Dihitung dari penjumlahan {@link Pajak#getNilai()} seluruh entitas Pajak yang
	 * tertaut ke detail pengadaan ini — sehingga PERSIS sama dengan total nominal
	 * baris-baris DaftarPengajuanTransfer pajak (dibuat oleh {@link #simpanPajak}).
	 * Maka: netto(vendor) + Σ(baris pajak) = bruto.
	 */
	@SuppressWarnings("unchecked")
	private double hitungTotalPphSaldoAwal(SaldoAwalMasterAsset saldoAwal) {
		try {
			if (saldoAwal == null) {
				return 0.0;
			}
			// Mode BREAKDOWN: PPh = nilai "Bukti Potong" (satu nilai manual), BUKAN Σ Pajak
			// per detail PO. Netto vendor = bruto - Bukti Potong.
			if (Boolean.TRUE.equals(saldoAwal.getBreakdownAktif())) {
				return nilai(saldoAwal.getBreakdownBuktiPotong());
			}
			// PENTING: getNominal() ini DIPANGGIL Hibernate saat snapshot properti (auto-flush).
			// Query di sini, secara default, MEMICU autoFlush lagi → flushEntities → getNominal()
			// → query → autoFlush → ... = REKURSI TAK BERUJUNG (StackOverflowError, sebuah Error
			// sehingga catch(Exception) di bawah pun tak menangkapnya). FlushMode.MANUAL pada query
			// mencegah pre-flush itu, jadi rekursi terputus. Data Pajak yang dibaca = yang sudah
			// tersimpan (cukup untuk menghitung total PPh).
			java.util.List<Pajak> pajaks = HibernateUtil.currentSession().createCriteria(Pajak.class)
					.createAlias("saldoAwalMasterAssetDetail", "d")
					.add(Restrictions.eq("d.saldoAwal", saldoAwal))
					.setFlushMode(org.hibernate.FlushMode.MANUAL).list();
			double pph = 0.0;
			for (Pajak p : pajaks) {
				pph += nilai(p.getNilai());
			}
			return pph;
		} catch (Throwable e) {
			return 0.0;
		}
	}

	public Double getNominal() {

		if (getSaldoAwalMasterAsset() != null) {
			// Vendor pengadaan: nominal NETTO = bruto - total PPh yang dipotong.
			// PPh dibayar lewat baris DaftarPengajuanTransfer PAJAK terpisah (lihat
			// Pajak.buat/simpanPajak), sehingga netto + Σ(baris pajak) = bruto.
			double bruto = nilai(saldoAwalMasterAsset.getNilai());
			nominal = bruto - hitungTotalPphSaldoAwal(saldoAwalMasterAsset);
		}

		else if (getKasBesar() != null) {
			nominal = getKasBesar().getNilai();
		}

		else if (getUangMuka() != null && getUangMuka().getNilai() != null) {
			nominal = getUangMuka().getNilai();
		}

		else if (getPembayaranHutangSupplier() != null && getPembayaranHutangSupplier().getNominal() != null) {
			nominal = Double.valueOf(getPembayaranHutangSupplier().getNominal().doubleValue());
		}

		else if (getReimbursementPegawai() != null && getReimbursementPegawai().getNominal() != null) {
			nominal = getReimbursementPegawai().getNominal();
		}

		else if (getPertangungjawaban() != null && getPertangungjawaban().getDikembalikan() != null) {
			nominal = getPertangungjawaban().getDikembalikan();
		}

		else if (getPertangungjawabanKasBesar() != null && getPertangungjawabanKasBesar().getDikembalikan() != null) {
			nominal = getPertangungjawabanKasBesar().getDikembalikan();
		}

		else if (getDanaTalangan() != null && getDanaTalangan().getNilai() != null) {
			nominal = getDanaTalangan().getNilai();
		}

		else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getNilai() != null) {
			nominal = getPenggantianKasKecil().getNilai();
		}

		else if (getPembayaranPengadaanMasterAssetDetail() != null
				&& getPembayaranPengadaanMasterAssetDetail().getDibayar() != null) {
			nominal = getPembayaranPengadaanMasterAssetDetail().getDibayar();
		}

		else if (getPembayaranTerminMasterAssetDetail() != null
				&& getPembayaranTerminMasterAssetDetail().getDibayar() != null) {
			// Netto vendor termin = dibayar (DPP+PPN+pinalti) - PPh yang dipotong. PPh dibayar lewat
			// baris DaftarPengajuanTransfer PAJAK terpisah (Pajak.buatDariTermin/simpanPajak), sehingga
			// netto + baris PPh = dibayar. Konsisten dengan jalur SaldoAwal (bruto - PPh).
			double dibayarTermin = getPembayaranTerminMasterAssetDetail().getDibayar();
			Double pphTermin = getPembayaranTerminMasterAssetDetail().getNilaiPphTermin();
			nominal = dibayarTermin - (pphTermin == null ? 0.0 : pphTermin);
		}

		else if (getPembayaranDpMasterAssetDetail() != null
				&& getPembayaranDpMasterAssetDetail().getDibayar() != null) {
			nominal = getPembayaranDpMasterAssetDetail().getDibayar();
		}

		else if (getJenisKasKecil() != null) {
			nominal = getJenisKasKecil().getSaldoAwal();
		}

		else if (getDiskonTagihan() != null && getDiskonTagihan().getDiskonSiswa() != null) {
			nominal = getDiskonTagihan().getDiskonTidakLangsung();
		}

		else if (getPajak() != null) {
			nominal = getPajak().getNilai();
		}

		else if (getPengajuanTransaksiPegawai() != null) {
			nominal = getPengajuanTransaksiPegawai().getNilaiTransaksi();
		}

		else if (getTransaksiKoperasi() != null) {
			nominal = getTransaksiKoperasi().getNilai();
		}

		return nominal == null ? 0.0 : nominal;
	}

	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_sumber_id", nullable = true)
	public Bank getBankSumber() {
		bankSumber = check(bankSumber);

		if (getSaldoAwalMasterAsset() != null && saldoAwalMasterAsset.getPenyedia() != null) {
			bankSumber = saldoAwalMasterAsset.getPenyedia().getBankUtama();
		} else if (getUangMuka() != null && getUangMuka().getJenisUangMuka() != null
				&& getUangMuka().getJenisUangMuka().getAkun() != null) {
			bankSumber = getUangMuka().getJenisUangMuka().getAkun().getBank();
		}

		else if (getReimbursementPegawai() != null) {
			// tujuan transfer reimbursement = bank PEGAWAI penerima; fallback akun
			try {
				if (getReimbursementPegawai().getPegawai() != null
						&& getReimbursementPegawai().getPegawai().getBank() != null) {
					bankSumber = getReimbursementPegawai().getPegawai().getBank();
				} else if (getReimbursementPegawai().getAkunPembayaran() != null) {
					bankSumber = getReimbursementPegawai().getAkunPembayaran().getBank();
				} else if (getReimbursementPegawai().getAkun() != null) {
					bankSumber = getReimbursementPegawai().getAkun().getBank();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) DaftarPengajuanTransfer.getBankSumber-reimbursement");
			}
		}

		else if (getPertangungjawaban() != null && getPertangungjawaban().getUangMuka().getJenisUangMuka() != null
				&& getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun() != null) {
			bankSumber = getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun().getBank();
		}

		else if (getPertangungjawabanKasBesar() != null
				&& getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar() != null
				&& getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar().getAkun() != null) {
			bankSumber = getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar().getAkun().getBank();
		}

		else if (getDanaTalangan() != null && getDanaTalangan().getJenisUangMuka() != null
				&& getDanaTalangan().getJenisUangMuka().getAkun() != null) {
			bankSumber = getDanaTalangan().getJenisUangMuka().getAkun().getBank();
		}

		else if (getKasBesar() != null && getKasBesar().getJenisKasBesar() != null
				&& getKasBesar().getJenisKasBesar().getAkun() != null) {
			bankSumber = getKasBesar().getJenisKasBesar().getAkun().getBank();
		}

		else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getKasKecil() != null
				&& getPenggantianKasKecil().getKasKecil().getJenisKasKecil() != null
				&& getPenggantianKasKecil().getKasKecil().getJenisKasKecil().getAkun() != null) {
			bankSumber = getPenggantianKasKecil().getKasKecil().getJenisKasKecil().getAkun().getBank();
		}

		else if (getPembayaranPengadaanMasterAssetDetail() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset()
						.getPenyedia() != null) {

			bankSumber = getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset().getPenyedia()
					.getBankUtama();
		}

		else if (getPembayaranTerminMasterAssetDetail() != null
				&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset() != null
				&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getPenyedia() != null) {

			bankSumber = getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getPenyedia()
					.getBankUtama();
		}

		else if (getPembayaranDpMasterAssetDetail() != null
				&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset() != null
				&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getPenyedia() != null) {

			bankSumber = getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getPenyedia().getBankUtama();
		}

		else if (getJenisKasKecil() != null && getJenisKasKecil().getAkun() != null) {
			bankSumber = getJenisKasKecil().getAkun().getBank();
		}

		else if (getDiskonTagihan() != null && getDiskonTagihan().getItemBiayaSekolah() != null
				&& getDiskonTagihan().getItemBiayaSekolah().getAkunUtangDiskon() != null) {
			bankSumber = getDiskonTagihan().getItemBiayaSekolah().getAkunUtangDiskon().getBank();
		}

		else if (getPajak() != null && getPajak().getJenisPajakBarang() != null
				&& getPajak().getJenisPajakBarang().getAkun() != null) {
			bankSumber = getPajak().getJenisPajakBarang().getAkun().getBank();
		}

		else if (getPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai()
						.getAkun() != null) {
			bankSumber = getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai()
					.getAkun().getBank();
		}

		else if (getTransaksiKoperasi() != null && getTransaksiKoperasi().getCaraPembayaranKoperasi() != null
				&& getTransaksiKoperasi().getCaraPembayaranKoperasi().getAkun() != null) {
			bankSumber = getTransaksiKoperasi().getCaraPembayaranKoperasi().getAkun().getBank();
		}

		return bankSumber;
	}

	public void setBankSumber(Bank bankSumber) {
		this.bankSumber = bankSumber;
	}

	public String getAtasNamaSumber() {

		if (getSaldoAwalMasterAsset() != null && saldoAwalMasterAsset.getPenyedia() != null) {
			atasNamaSumber = saldoAwalMasterAsset.getPenyedia().getAtasNama();
		} else if (getUangMuka() != null && getUangMuka().getJenisUangMuka() != null
				&& getUangMuka().getJenisUangMuka().getAkun() != null) {
			atasNamaSumber = getUangMuka().getJenisUangMuka().getAkun().getAtasNama();
		}

		else if (getReimbursementPegawai() != null && getReimbursementPegawai().getPegawai() != null) {
			// tujuan transfer reimbursement = rekening PEGAWAI penerima
			atasNamaSumber = getReimbursementPegawai().getPegawai().getNama();
		}

		else if (getPertangungjawaban() != null && getPertangungjawaban().getUangMuka().getJenisUangMuka() != null
				&& getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun() != null) {
			atasNamaSumber = getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun().getAtasNama();
		}

		else if (getPertangungjawabanKasBesar() != null
				&& getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar() != null
				&& getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar().getAkun() != null) {
			atasNamaSumber = getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar().getAkun().getAtasNama();
		}

		else if (getKasBesar() != null && getKasBesar().getJenisKasBesar() != null
				&& getKasBesar().getJenisKasBesar().getAkun() != null) {
			atasNamaSumber = getKasBesar().getJenisKasBesar().getAkun().getAtasNama();
		}

		else if (getDanaTalangan() != null && getDanaTalangan().getJenisUangMuka() != null
				&& getDanaTalangan().getJenisUangMuka().getAkun() != null) {
			atasNamaSumber = getDanaTalangan().getJenisUangMuka().getAkun().getAtasNama();
		}

		else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getKasKecil() != null
				&& getPenggantianKasKecil().getKasKecil().getJenisKasKecil() != null
				&& getPenggantianKasKecil().getKasKecil().getJenisKasKecil().getAkun() != null) {
			atasNamaSumber = getPenggantianKasKecil().getKasKecil().getJenisKasKecil().getAkun().getAtasNama();
		}

		else if (getPembayaranPengadaanMasterAssetDetail() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset()
						.getPenyedia() != null) {

			atasNamaSumber = getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset().getPenyedia()
					.getAtasNama();
		}

		else if (getPembayaranTerminMasterAssetDetail() != null
				&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset() != null
				&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getPenyedia() != null) {

			atasNamaSumber = getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getPenyedia()
					.getAtasNama();
		}

		else if (getPembayaranDpMasterAssetDetail() != null
				&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset() != null
				&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getPenyedia() != null) {

			atasNamaSumber = getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getPenyedia()
					.getAtasNama();
		}

		else if (getJenisKasKecil() != null && getJenisKasKecil().getAkun() != null) {
			atasNamaSumber = getJenisKasKecil().getAkun().getAtasNama();
		}

		else if (getDiskonTagihan() != null && getDiskonTagihan().getItemBiayaSekolah() != null
				&& getDiskonTagihan().getItemBiayaSekolah().getAkunUtangDiskon() != null) {
			atasNamaSumber = getDiskonTagihan().getItemBiayaSekolah().getAkunUtangDiskon().getAtasNama();
		}

		else if (getPajak() != null && getPajak().getJenisPajakBarang() != null
				&& getPajak().getJenisPajakBarang().getAkun() != null) {
			atasNamaSumber = getPajak().getJenisPajakBarang().getAkun().getAtasNama();
		}

		else if (getPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai()
						.getAkun() != null) {
			atasNamaSumber = getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai()
					.getJenisTransaksiPegawai().getAkun().getAtasNama();
		}

		else if (getTransaksiKoperasi() != null && getTransaksiKoperasi().getCaraPembayaranKoperasi() != null
				&& getTransaksiKoperasi().getCaraPembayaranKoperasi().getAkun() != null) {
			atasNamaSumber = getTransaksiKoperasi().getCaraPembayaranKoperasi().getAkun().getAtasNama();
		}

		return atasNamaSumber;
	}

	public void setAtasNamaSumber(String atasNamaSumber) {
		this.atasNamaSumber = atasNamaSumber;
	}

	public String getNoRekSumber() {

		if (getSaldoAwalMasterAsset() != null && saldoAwalMasterAsset.getPenyedia() != null) {
			noRekSumber = saldoAwalMasterAsset.getPenyedia().getNoRek();
		} else if (getUangMuka() != null && getUangMuka().getJenisUangMuka() != null
				&& getUangMuka().getJenisUangMuka().getAkun() != null) {
			noRekSumber = getUangMuka().getJenisUangMuka().getAkun().getNoRek();
		}

		else if (getReimbursementPegawai() != null) {
			// tujuan transfer reimbursement = rekening PEGAWAI penerima (kolom
			// rekening_penerima bila diisi, fallback norek profil pegawai)
			try {
				if (getReimbursementPegawai().getRekeningPenerima() != null
						&& !getReimbursementPegawai().getRekeningPenerima().trim().isEmpty()) {
					noRekSumber = getReimbursementPegawai().getRekeningPenerima();
				} else if (getReimbursementPegawai().getPegawai() != null) {
					noRekSumber = getReimbursementPegawai().getPegawai().getNorek();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) DaftarPengajuanTransfer.getNoRekSumber-reimbursement");
			}
		}

		else if (getKasBesar() != null && getKasBesar().getJenisKasBesar() != null
				&& getKasBesar().getJenisKasBesar().getAkun() != null) {
			noRekSumber = getKasBesar().getJenisKasBesar().getAkun().getNoRek();
		}

		else if (getPertangungjawaban() != null && getPertangungjawaban().getUangMuka().getJenisUangMuka() != null
				&& getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun() != null) {
			noRekSumber = getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun().getNoRek();
		}

		else if (getPertangungjawabanKasBesar() != null
				&& getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar() != null
				&& getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar().getAkun() != null) {
			noRekSumber = getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar().getAkun().getNoRek();
		}

		else if (getDanaTalangan() != null && getDanaTalangan().getJenisUangMuka() != null
				&& getDanaTalangan().getJenisUangMuka().getAkun() != null) {
			noRekSumber = getDanaTalangan().getJenisUangMuka().getAkun().getNoRek();
		}

		else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getKasKecil() != null
				&& getPenggantianKasKecil().getKasKecil().getJenisKasKecil() != null
				&& getPenggantianKasKecil().getKasKecil().getJenisKasKecil().getAkun() != null) {
			noRekSumber = getPenggantianKasKecil().getKasKecil().getJenisKasKecil().getAkun().getNoRek();
		}

		else if (getPembayaranPengadaanMasterAssetDetail() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset()
						.getPenyedia() != null) {

			noRekSumber = getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset().getPenyedia()
					.getNoRek();
		}

		else if (getPembayaranTerminMasterAssetDetail() != null
				&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset() != null
				&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getPenyedia() != null) {

			noRekSumber = getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getPenyedia()
					.getNoRek();
		}

		else if (getPembayaranDpMasterAssetDetail() != null
				&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset() != null
				&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getPenyedia() != null) {

			noRekSumber = getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getPenyedia().getNoRek();
		}

		else if (getJenisKasKecil() != null && getJenisKasKecil().getAkun() != null) {
			noRekSumber = getJenisKasKecil().getAkun().getNoRek();
		}

		else if (getDiskonTagihan() != null && getDiskonTagihan().getItemBiayaSekolah() != null
				&& getDiskonTagihan().getItemBiayaSekolah().getAkunUtangDiskon() != null) {
			noRekSumber = getDiskonTagihan().getItemBiayaSekolah().getAkunUtangDiskon().getNoRek();
		}

		else if (getPajak() != null && getPajak().getJenisPajakBarang() != null
				&& getPajak().getJenisPajakBarang().getAkun() != null) {
			noRekSumber = getPajak().getJenisPajakBarang().getAkun().getNoRek();
		}

		else if (getPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai()
						.getAkun() != null) {
			noRekSumber = getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai()
					.getAkun().getNoRek();
		}

		else if (getTransaksiKoperasi() != null && getTransaksiKoperasi().getCaraPembayaranKoperasi() != null
				&& getTransaksiKoperasi().getCaraPembayaranKoperasi().getAkun() != null) {
			noRekSumber = getTransaksiKoperasi().getCaraPembayaranKoperasi().getAkun().getNoRek();
		}

		return noRekSumber == null || noRekSumber.isEmpty() ? null : noRekSumber.trim();
	}

	public void setNoRekSumber(String noRekSumber) {
		this.noRekSumber = noRekSumber;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "proses_transfer", nullable = true)
	public ProsesTransfer getProsesTransfer() {
		return prosesTransfer;
	}

	public void setProsesTransfer(ProsesTransfer prosesTransfer) {
		this.prosesTransfer = prosesTransfer;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dana_talangan", nullable = true)
	public DanaTalangan getDanaTalangan() {
		return danaTalangan;
	}

	public void setDanaTalangan(DanaTalangan danaTalangan) {
		this.danaTalangan = danaTalangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_termin_master_asset_detail", nullable = true)
	public PembayaranTerminMasterAssetDetail getPembayaranTerminMasterAssetDetail() {
		return pembayaranTerminMasterAssetDetail;
	}

	public void setPembayaranTerminMasterAssetDetail(
			PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail) {
		this.pembayaranTerminMasterAssetDetail = pembayaranTerminMasterAssetDetail;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_dp_master_asset_detail", nullable = true)
	public PembayaranDpMasterAssetDetail getPembayaranDpMasterAssetDetail() {
		return pembayaranDpMasterAssetDetail;
	}

	public void setPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail) {
		this.pembayaranDpMasterAssetDetail = pembayaranDpMasterAssetDetail;
	}

	private String kodeUnik = null;

	@Column(unique = true)
	public String getKodeUnik() {
		if (kodeUnik == null && getId() != null) {
			kodeUnik = Common.getGeneratedBarCode() + "_" + getId();
		}
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);

		if (getSaldoAwalMasterAsset() != null) {
			disposisiSop = saldoAwalMasterAsset.getDisposisiSop();
		} else if (getUangMuka() != null && getUangMuka().getDisposisiSop() != null) {
			disposisiSop = getUangMuka().getDisposisiSop();
		}

		else if (getReimbursementPegawai() != null && getReimbursementPegawai().getDisposisiSop() != null) {
			disposisiSop = getReimbursementPegawai().getDisposisiSop();
		}

		else if (getPertangungjawaban() != null && getPertangungjawaban().getDisposisiSop() != null) {
			disposisiSop = getPertangungjawaban().getDisposisiSop();
		}

		else if (getPertangungjawabanKasBesar() != null && getPertangungjawabanKasBesar().getDisposisiSop() != null) {
			disposisiSop = getPertangungjawabanKasBesar().getDisposisiSop();
		}

		else if (getDanaTalangan() != null && getDanaTalangan().getDisposisiSop() != null) {
			disposisiSop = getDanaTalangan().getDisposisiSop();
		}

		else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getDisposisiSop() != null) {
			disposisiSop = getPenggantianKasKecil().getDisposisiSop();
		}

		else if (getKasBesar() != null && getKasBesar().getDisposisiSop() != null) {
			disposisiSop = getKasBesar().getDisposisiSop();
		}

		else if (getPembayaranPengadaanMasterAssetDetail() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset()
						.getDisposisiSop() != null) {

			disposisiSop = getPembayaranPengadaanMasterAssetDetail().getPembayaranPengadaanMasterAsset()
					.getDisposisiSop();

		}

		else if (getPembayaranTerminMasterAssetDetail() != null
				&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset() != null
				&& getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getDisposisiSop() != null) {

			disposisiSop = getPembayaranTerminMasterAssetDetail().getPembayaranTerminMasterAsset().getDisposisiSop();

		}

		else if (getPembayaranTerminMasterAssetDetail() != null
				&& getPembayaranTerminMasterAssetDetail().getPemesananPengadaanMasterAsset() != null
				&& getPembayaranTerminMasterAssetDetail().getPemesananPengadaanMasterAsset()
						.getDisposisiSop() != null) {

			disposisiSop = getPembayaranTerminMasterAssetDetail().getPemesananPengadaanMasterAsset().getDisposisiSop();

		}

		else if (getPembayaranDpMasterAssetDetail() != null
				&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset() != null
				&& getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getDisposisiSop() != null) {
			disposisiSop = getPembayaranDpMasterAssetDetail().getPembayaranDpMasterAsset().getDisposisiSop();
		}

		else if (getPengajuanTransaksiPegawai() != null && getPengajuanTransaksiPegawai().getDisposisiSop() != null) {
			disposisiSop = getPengajuanTransaksiPegawai().getDisposisiSop();
		}

		else if (getTransaksiKoperasi() != null && getTransaksiKoperasi().getDisposisiSop() != null) {
			disposisiSop = getTransaksiKoperasi().getDisposisiSop();
		}

		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);

		if (getSaldoAwalMasterAsset() != null && saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null) {
			akun = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null
					|| saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang() == null
							? null
							: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang()
									.getAkunHutangPenyedia();
		} else if (getSaldoAwalMasterAsset() != null && saldoAwalMasterAsset.getPenyedia() != null) {
			akun = saldoAwalMasterAsset.getPenyedia().getAkunUtang();
		}

		else if (getUangMuka() != null && getUangMuka().getJenisUangMuka() != null
				&& getUangMuka().getJenisUangMuka().getAkun() != null) {
			akun = getUangMuka().getJenisUangMuka().getAkun();
		}

		else if (getReimbursementPegawai() != null) {
			// Akun reimbursement: dari anggaran/Jenis Reimbursement (getAkun sudah
			// menurunkan workspace.akun atau akun tetap jenis); fallback akunBiaya lama.
			try {
				if (getReimbursementPegawai().getAkun() != null) {
					akun = getReimbursementPegawai().getAkun();
				} else if (getReimbursementPegawai().getAkunBiaya() != null) {
					akun = getReimbursementPegawai().getAkunBiaya();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) DaftarPengajuanTransfer.getAkun-reimbursement");
			}
		}

		else if (getKasBesar() != null && getKasBesar().getJenisKasBesar() != null
				&& getKasBesar().getJenisKasBesar().getAkun() != null) {
			akun = getKasBesar().getJenisKasBesar().getAkun();
		}

		else if (getPertangungjawaban() != null && getPertangungjawaban().getUangMuka().getJenisUangMuka() != null
				&& getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun() != null) {
			akun = getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun();
		}

		else if (getPertangungjawabanKasBesar() != null
				&& getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar() != null
				&& getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar().getAkun() != null) {
			akun = getPertangungjawabanKasBesar().getKasBesar().getJenisKasBesar().getAkun();
		}

		else if (getDanaTalangan() != null && getDanaTalangan().getJenisUangMuka() != null
				&& getDanaTalangan().getJenisUangMuka().getAkun() != null) {
			akun = getDanaTalangan().getJenisUangMuka().getAkun();
		}

		else if (getPenggantianKasKecil() != null && getPenggantianKasKecil().getKasKecil() != null
				&& getPenggantianKasKecil().getKasKecil().getJenisKasKecil() != null
				&& getPenggantianKasKecil().getKasKecil().getJenisKasKecil().getAkun() != null) {
			akun = getPenggantianKasKecil().getKasKecil().getJenisKasKecil().getAkun();
		}

		else if (getPembayaranPengadaanMasterAssetDetail() != null) {

			akun = pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() == null
					|| pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
							.getJenisPenerimaanBarang() == null ? null
									: pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
											.getJenisPenerimaanBarang().getAkunHutangPenyedia();

//			if (pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null
//					&& pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
//							.getPemesananPengadaanMasterAsset() != null
//					&& pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
//							.getPemesananPengadaanMasterAsset().getByTermin()) {
//				try {
//					JSONArray array = new JSONArray(pembayaranPengadaanMasterAssetDetail
//							.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset().getFormula());
//					for (int i = 0; i < array.length(); i++) {
//
//						JSONObject jsonObject = array.getJSONObject(i);
//
//						if (!jsonObject.isNull("key") && jsonObject.get("key").toString()
//								.equalsIgnoreCase(pembayaranPengadaanMasterAssetDetail
//										.getPenerimaanPengadaanMasterAsset().getKodeTermin())) {
//
//							try {
//
//								Boolean merupakan_dp;
//								if (!jsonObject.isNull("merupakan_dp")) {
//									merupakan_dp = Boolean.parseBoolean(jsonObject.get("merupakan_dp") + "");
//								} else {
//									merupakan_dp = false;
//								}
//
//								if (merupakan_dp) {
//									akun = pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset()
//											.getPemesananPengadaanMasterAsset().getJenisPemesananPengadaanAsset()
//											.getAkunDp();
//									break;
//								}
//
//							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/DaftarPengajuanTransfer.java:1555");
//								e.printStackTrace();
//							}
//						}
//
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}

		}

		else if (getPembayaranTerminMasterAssetDetail() != null) {

			akun = pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset() == null
					|| pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getJenisPemesananPengadaanAsset() == null ? null
									: pembayaranTerminMasterAssetDetail.getPemesananPengadaanMasterAsset()
											.getJenisPemesananPengadaanAsset().getAkunUtangDp();

		}

		else if (getPembayaranDpMasterAssetDetail() != null) {
			akun = pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset() == null
					|| pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset()
							.getJenisPemesananPengadaanAsset() == null ? null
									: pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset()
											.getJenisPemesananPengadaanAsset().getAkunUtangDp();
		}

		else if (getJenisKasKecil() != null) {
			akun = getJenisKasKecil().getAkun();
		}

		else if (getDiskonTagihan() != null && getDiskonTagihan().getItemBiayaSekolah() != null
				&& getDiskonTagihan().getItemBiayaSekolah().getAkunUtangDiskon() != null) {
			akun = getDiskonTagihan().getItemBiayaSekolah().getAkunUtangDiskon();
		}

		else if (getPajak() != null && getPajak().getJenisPajakBarang() != null) {
			akun = getPajak().getJenisPajakBarang().getAkun();
		}

		else if (getPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai()
						.getAkun() != null) {
			akun = getPengajuanTransaksiPegawai().getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai()
					.getAkun();
		}

		else if (getTransaksiKoperasi() != null && getTransaksiKoperasi().getCaraPembayaranKoperasi() != null
				&& getTransaksiKoperasi().getCaraPembayaranKoperasi().getAkun() != null) {
			akun = getTransaksiKoperasi().getCaraPembayaranKoperasi().getAkun();
		}

		return akun;
	}

	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kas_kecil", nullable = true)
	public JenisKasKecil getJenisKasKecil() {
		jenisKasKecil = check(jenisKasKecil);
		return jenisKasKecil;
	}

	public void setJenisKasKecil(JenisKasKecil jenisKasKecil) {
		this.jenisKasKecil = jenisKasKecil;
	}

	public Boolean getTransfer() {

		if (getProsesTransfer() != null && getProsesTransfer().getRealisasikanOleh() != null && !getTransitori()) {
			transfer = true;
		}

		return transfer == null ? false : transfer;
	}

	public void setTransfer(Boolean transfer) {
		this.transfer = transfer;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {

		if (getSaldoAwalMasterAsset() != null) {
			waktu = getSaldoAwalMasterAsset().getTanggalPersetujuan();
		}

		else if (getUangMuka() != null) {
			waktu = getUangMuka().getTanggalPembuatan();
		}

		else if (getPertangungjawaban() != null) {
			waktu = getPertangungjawaban().getTanggalPembuatan();
		}

		else if (getPertangungjawabanKasBesar() != null) {
			waktu = getPertangungjawabanKasBesar().getTanggalPembuatan();
		}

		else if (getDanaTalangan() != null) {
			waktu = getDanaTalangan().getTanggalPembuatan();
		}

		else if (getKasBesar() != null) {
			waktu = getKasBesar().getTanggalPembuatan();
		}

		else if (getPenggantianKasKecil() != null) {
			waktu = getPenggantianKasKecil().getTanggalPembuatan();
		}

		else if (getPembayaranPengadaanMasterAssetDetail() != null) {
			waktu = getPembayaranPengadaanMasterAssetDetail().getTanggalDibayar();
		}

		else if (getPembayaranTerminMasterAssetDetail() != null) {
			waktu = getPembayaranTerminMasterAssetDetail().getTanggalDibayar();
		}

		else if (getPembayaranDpMasterAssetDetail() != null) {
			waktu = getPembayaranDpMasterAssetDetail().getTanggalDibayar();
		}

		else if (getJenisKasKecil() != null) {
			waktu = getJenisKasKecil().getTanggal();
		}

		else if (getDiskonTagihan() != null && getDiskonTagihan().getTanggalTagihan() != null) {
			waktu = getDiskonTagihan().getTanggalTagihan();
		} else if (getPajak() != null) {
			// Baris pembayaran pajak: SELALU pakai tanggal Pajak (getTanggal tak pernah null).
			// Syarat lama "getSatuanKerja() != null" membuat baris pajak TERMIN (tertaut via keyData
			// tanpa relasi saldoAwal) punya waktu=null → tersaring keluar filter tanggal
			// "date(this_.waktu) between ...". Pajak breakdown/pertanggungjawaban punya satuanKerja
			// jadi tidak terpengaruh.
			waktu = getPajak().getTanggal();
		}

		else if (getPengajuanTransaksiPegawai() != null
				&& getPengajuanTransaksiPegawai().getTanggalJatuhTempo() != null) {
			waktu = getPengajuanTransaksiPegawai().getTanggalJatuhTempo();
		}

		else if (getTransaksiKoperasi() != null) {
			waktu = getTransaksiKoperasi().getTanggalTransaksi();
		}

		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	public SatuanKerja ambilSatuanKerja() {
		SatuanKerja satuanKerja = null;

		if (getSaldoAwalMasterAsset() != null) {
			satuanKerja = getSaldoAwalMasterAsset().getSatuanKerja();
		} else if (getUangMuka() != null) {
			satuanKerja = getUangMuka().getSatuanKerja();
		}

		else if (getReimbursementPegawai() != null) {
			satuanKerja = getReimbursementPegawai().getSatuanKerja();
		}

		else if (getPertangungjawaban() != null) {
			satuanKerja = getPertangungjawaban().getSatuanKerja();
		}

		else if (getPertangungjawabanKasBesar() != null) {
			satuanKerja = getPertangungjawabanKasBesar().getSatuanKerja();
		}

		else if (getKasBesar() != null) {
			satuanKerja = getKasBesar().getSatuanKerja();
		}

		else if (getDanaTalangan() != null) {
			satuanKerja = getDanaTalangan().getSatuanKerja();
		}

		else if (getPenggantianKasKecil() != null) {
			satuanKerja = getPenggantianKasKecil().getSatuanKerja();
		}

		else if (getPembayaranPengadaanMasterAssetDetail() != null
				&& getPembayaranPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset() != null) {
			satuanKerja = getPembayaranPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset()
					.getSatuanKerja();
		}

		else if (getPembayaranTerminMasterAssetDetail() != null
				&& getPembayaranTerminMasterAssetDetail().getPemesananPengadaanMasterAsset() != null) {
			satuanKerja = getPembayaranTerminMasterAssetDetail().getPemesananPengadaanMasterAsset().getSatuanKerja();
		}

		else if (getPembayaranDpMasterAssetDetail() != null
				&& getPembayaranDpMasterAssetDetail().getPemesananPengadaanMasterAsset() != null) {
			satuanKerja = getPembayaranDpMasterAssetDetail().getPemesananPengadaanMasterAsset().getSatuanKerja();
		}

		else if (getJenisKasKecil() != null) {
			satuanKerja = getJenisKasKecil().getSatuanKerja();
		}

		else if (getDiskonTagihan() != null && getDiskonTagihan().getPengaturanBiaya() != null
				&& getDiskonTagihan().getPengaturanBiaya().getSekolah() != null) {
			satuanKerja = getDiskonTagihan().getPengaturanBiaya().getSekolah().getSatuanKerja();
		} else if (getPajak() != null && getPajak().getSatuanKerja() != null) {
			satuanKerja = getPajak().getSatuanKerja();
		}

		else if (getPengajuanTransaksiPegawai() != null && getPengajuanTransaksiPegawai().getSatuanKerja() != null) {
			satuanKerja = getPengajuanTransaksiPegawai().getSatuanKerja();
		}

		else if (getTransaksiKoperasi() != null && getTransaksiKoperasi().getSatuanKerja() != null) {
			satuanKerja = getTransaksiKoperasi().getSatuanKerja();
		}

		return satuanKerja;
	}

	public Boolean getTransitori() {
		if (transfer != null && transfer) {
			transitori = false;
		}
		return transitori == null ? false : transitori;
	}

	public void setTransitori(Boolean transitori) {
		this.transitori = transitori;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "diskon_tagihan", nullable = true)
	public Tagihan getDiskonTagihan() {
		return diskonTagihan;
	}

	public void setDiskonTagihan(Tagihan diskonTagihan) {
		this.diskonTagihan = diskonTagihan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_besar", nullable = true)
	public KasBesar getKasBesar() {
		return kasBesar;
	}

	public void setKasBesar(KasBesar kasBesar) {
		this.kasBesar = kasBesar;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transitori_data", nullable = true)
	public Transitori getTransitoriData() {
		return transitoriData;
	}

	public void setTransitoriData(Transitori transitoriData) {
		this.transitoriData = transitoriData;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban_kas_besar", nullable = true)
	public PertangungjawabanKasBesar getPertangungjawabanKasBesar() {
		return pertangungjawabanKasBesar;
	}

	public void setPertangungjawabanKasBesar(PertangungjawabanKasBesar pertangungjawabanKasBesar) {
		this.pertangungjawabanKasBesar = pertangungjawabanKasBesar;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = ambilSatuanKerja();
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pajak", nullable = true)
	public Pajak getPajak() {
		return pajak;
	}

	public void setPajak(Pajak pajak) {
		this.pajak = pajak;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal_master_asset", nullable = true)
	public SaldoAwalMasterAsset getSaldoAwalMasterAsset() {
		return saldoAwalMasterAsset;
	}

	public void setSaldoAwalMasterAsset(SaldoAwalMasterAsset saldoAwalMasterAsset) {
		this.saldoAwalMasterAsset = saldoAwalMasterAsset;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengajuan_transaksi_pegawai", nullable = true)
	public PengajuanTransaksiPegawai getPengajuanTransaksiPegawai() {
		return pengajuanTransaksiPegawai;
	}

	public void setPengajuanTransaksiPegawai(PengajuanTransaksiPegawai pengajuanTransaksiPegawai) {
		this.pengajuanTransaksiPegawai = pengajuanTransaksiPegawai;
	}

	public static void simpanPengajuanTransaksiPegawai(PengajuanTransaksiPegawai pengajuanTransaksiPegawai) {

		if (pengajuanTransaksiPegawai != null && pengajuanTransaksiPegawai.getDaftarPengajuanTransfer() != null) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
				.createCriteria(DaftarPengajuanTransfer.class)
				.add(Restrictions.eq("pengajuanTransaksiPegawai", pengajuanTransaksiPegawai)).setMaxResults(1)
				.uniqueResult();
		if (daftarPengajuanTransfer == null) {
			daftarPengajuanTransfer = new DaftarPengajuanTransfer();

		}
		daftarPengajuanTransfer.setPengajuanTransaksiPegawai(pengajuanTransaksiPegawai);
		daftarPengajuanTransfer
				.setNama("Pembayaran Pengajuan Transaksi Pegawai " + pengajuanTransaksiPegawai.getNama());

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
		session.getTransaction().commit();

		pengajuanTransaksiPegawai.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, pengajuanTransaksiPegawai);
		session.getTransaction().commit();

		// currentNativeSession ditutup manual karena bukan currentSession request.
		closeNativeSessionSafely(session);
	}

	public static void simpanTransaksiKoperasi(TransaksiKoperasi transaksiKoperasi) {

		if (transaksiKoperasi != null && transaksiKoperasi.getDaftarPengajuanTransfer() != null) {
			return;
		}

		if (transaksiKoperasi.getProdukKoperasi() != null
				&& transaksiKoperasi.getProdukKoperasi().getTipeProdukKoperasi() != null
				&& ConstantValues.PINJAMAN != null && transaksiKoperasi.getProdukKoperasi().getTipeProdukKoperasi()
						.getId().equals(ConstantValues.PINJAMAN.getId())) {

			Session session = HibernateUtil.currentNativeSession();
			DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) session
					.createCriteria(DaftarPengajuanTransfer.class)
					.add(Restrictions.eq("transaksiKoperasi", transaksiKoperasi)).setMaxResults(1).uniqueResult();
			if (daftarPengajuanTransfer == null) {
				daftarPengajuanTransfer = new DaftarPengajuanTransfer();

			}
			daftarPengajuanTransfer.setTransaksiKoperasi(transaksiKoperasi);
			daftarPengajuanTransfer.setNama("Transaksi Koperasi " + transaksiKoperasi.getNama());

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, daftarPengajuanTransfer);
			session.getTransaction().commit();

			transaksiKoperasi.setDaftarPengajuanTransfer(daftarPengajuanTransfer);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, transaksiKoperasi);
			session.getTransaction().commit();

			// currentNativeSession ditutup manual karena bukan currentSession request.
			closeNativeSessionSafely(session);
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@NotFound(action = NotFoundAction.IGNORE)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_koperasi", nullable = true)
	public TransaksiKoperasi getTransaksiKoperasi() {
		return transaksiKoperasi;
	}

	public void setTransaksiKoperasi(TransaksiKoperasi transaksiKoperasi) {
		this.transaksiKoperasi = transaksiKoperasi;
	}
}
