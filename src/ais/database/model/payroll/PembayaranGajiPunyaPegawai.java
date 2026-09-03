package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.akunting.PostingHistory;

/**
 * Entity <b>slip gaji satu pegawai</b> untuk satu dokumen pembayaran gaji: satu baris di
 * {@code payroll.pembayaran_gaji_punya_pegawai} berarti "pegawai X ikut dibayar pada dokumen
 * penggajian Y, dengan format item gaji Z, sebesar sekian rupiah". Entity ini adalah
 * <b>baris anak</b> dari header {@link ais.database.model.payroll.PembayaranGaji} (satu dokumen
 * penggajian per satuan kerja per bulan/tahun), sekaligus <b>induk</b> dari rincian komponen
 * gaji {@link ais.database.model.payroll.PembayaranItemGajiPegawai} (tunjangan, potongan,
 * gaji pokok, dan seterusnya; satu baris per item, masing-masing membawa akun debet/kreditnya
 * sendiri). Tiga lapis itulah bentuk sesungguhnya dokumen penggajian AIS:
 * <b>PembayaranGaji &rarr; PembayaranGajiPunyaPegawai &rarr; PembayaranItemGajiPegawai</b>.
 *
 * <h3>Isi kolom yang benar-benar ada (hasil verifikasi kode, bukan dugaan)</h3>
 * Entity ini <b>tidak</b> punya kolom terpisah untuk tunjangan, potongan, PPh, atau
 * gaji pokok. Seluruh rincian itu tinggal di tabel anak {@code PembayaranItemGajiPegawai};
 * di sini hanya tersimpan <b>angka ringkasan</b> dan <b>identitas baris</b>:
 * <ul>
 *   <li>{@link #getPembayaranGaji()} &mdash; header dokumen penggajian (FK, nullable);</li>
 *   <li>{@link #getPegawai()} &mdash; pegawai penerima (FK, nullable);</li>
 *   <li>{@link #getFormatItemGaji()} &mdash; susunan/format item gaji yang dipakai; satu pegawai
 *       boleh punya lebih dari satu format (mis. gaji tetap dan honor), sehingga kunci alami
 *       baris ini sesungguhnya adalah <b>(pembayaranGaji, pegawai, formatItemGaji)</b>, bukan
 *       (pembayaranGaji, pegawai) saja &mdash; lihat pencarian di
 *       {@code BayarGajiPegawaiAction.bayar};</li>
 *   <li>{@link #getNilai()} &mdash; nominal ringkasan baris (lihat catatan penting di bawah);</li>
 *   <li>{@link #getNilaiFinal()} &mdash; nilai item yang ditandai "final" pada format gaji, yaitu
 *       <b>gaji bersih/take-home pay</b> hasil formula (ditulis oleh mesin hitung, bukan diketik
 *       operator);</li>
 *   <li>{@link #getKomponenGaji()} &mdash; snapshot JSON {@code {idItemGaji: nilai}} seluruh
 *       komponen, dipakai laporan rekap agar tidak perlu menjelajah tabel anak;</li>
 *   <li>{@link #getMulai()}/{@link #getSampai()} &mdash; periode kerja yang dibayar
 *       (diturunkan dari bulan/tahun header + konfigurasi, bukan diketik);</li>
 *   <li>{@link #getTanggalBayar()} &mdash; tanggal bayar, dipakai sebagai <b>tanggal jurnal</b>;</li>
 *   <li>{@link #getKeterangan()} &mdash; catatan bebas per pegawai;</li>
 *   <li>{@link #getPostingHistory()} &mdash; cap posting ke buku besar (lihat di bawah);</li>
 *   <li>jejak audit warisan {@code GeneralValueObject}: {@code oleh}, {@code olehId},
 *       {@code tanggal_dirubah}, ditambah {@code @Audited} (Envers) untuk seluruh entity.</li>
 * </ul>
 * <b>Tidak ada kolom satuan kerja/tenant di entity ini.</b> Cakupan tenant seluruhnya
 * dititipkan ke induk ({@code pembayaranGaji.satuanKerja}) dan ke {@code pegawai.satuanKerja}
 * &mdash; yang terakhir itulah yang dipakai sebagai satuan kerja jurnal saat posting.
 *
 * <h3>Peran dalam mesin posting: entity ini ADALAH kunci jurnal</h3>
 * Berbeda dari kebanyakan baris detail, slip gaji per pegawai <b>dijurnal sendiri-sendiri</b>.
 * {@code PostingTransaksiPenggajianAction} (menu "Posting Penggajian") memanggil
 * {@code CommonAkunting.saveTransaksi(..., reference, satuanKerja, session)} dengan
 * <b>instance entity ini sebagai {@code reference}</b> pada ketiga jalurnya (tombol per baris,
 * tombol "Posting Semua", dan jalur REST {@code postingSemua}). {@code CommonAkunting} mengenali
 * tipe itu dan mengisi kolom {@code GrupTransaksi.pembayaranGajiPunyaPegawai}; dari situ
 * {@code GrupTransaksi.ambilUnik()} membentuk kunci idempotensi
 * {@code "ais.database.model.payroll.PembayaranGajiPunyaPegawai_" + id} yang dicek sebelum
 * jurnal ditulis. Jadi <b>id baris inilah yang menjadi kunci anti-posting-ganda</b> jurnal gaji.
 * Perhatikan bahwa mesin posting yang lain, {@code PostingTransaksiPembayaranGajiAction}
 * (menu "Posting Gaji"), menjurnal pada tingkat <b>header</b> dengan {@code reference} berupa
 * {@code PembayaranGaji} &mdash; dua mesin, dua granularitas, dua kunci berbeda untuk dokumen
 * yang sama.
 *
 * <h3>Ringkasan bug tabrakan kunci {@code task_e68c78f1} dari sisi entity ini &mdash; SUDAH DIPERBAIKI</h3>
 * {@link ais.database.model.akunting.GrupTransaksi#ambilUnik()} punya dua cabang bertetangga:
 * {@code pembayaranGajiPunyaPegawai} (entity ini) dan {@code transaksiPegawai}
 * ({@link ais.database.model.payroll.TransaksiPegawai}, dokumen transaksi pegawai seperti
 * angsuran pinjaman/potongan lepas). Sebelum revisi <b>r83966</b>, cabang {@code transaksiPegawai}
 * keliru menuliskan nama kelas <b>entity ini</b>, sehingga jurnal {@code TransaksiPegawai}
 * ber-id N memakai kunci yang persis sama dengan jurnal slip gaji ber-id N. Dampaknya
 * <b>dari sisi slip gaji</b>: kunci milik slip ini bisa "dipakai duluan" oleh dokumen transaksi
 * pegawai yang kebetulan ber-id sama, sehingga percobaan memposting slip gaji akan dianggap
 * duplikat dan jurnalnya tidak pernah ditulis &mdash; gaji terbayar tanpa jurnal, atau
 * sebaliknya cap posting dokumen lain ikut tertimpa. Peluang tabrakan <b>tinggi secara
 * struktural</b>: kedua tabel adalah tabel besar yang tumbuh bersamaan (satu baris per pegawai
 * per bulan), id-nya sama-sama {@code IDENTITY} mulai dari 1, dan kuncinya berlaku
 * <b>global lintas seluruh tenant</b> karena tidak mengandung satuan kerja. Pada instalasi yang
 * mengaktifkan kedua modul, rentang id keduanya praktis dipastikan bertumpang tindih.
 * <b>Status terverifikasi pada HEAD:</b> cacat ini <b>sudah ditutup</b> &mdash; cabang
 * {@code transaksiPegawai} kini menulis {@code TransaksiPegawai.class.getName()}. Perbaikannya
 * aman tanpa migrasi karena overload {@code saveTransaksi(TransaksiPegawai, ...)} tidak melewati
 * pengecekan dedup sama sekali (arah gagalnya adalah pelanggaran unique constraint, bukan
 * kehilangan jurnal senyap), dan bentuk kunci baru pasti unik. Yang <b>belum</b> selesai dari
 * {@code task_e68c78f1} adalah kaki lainnya: 15 dari 41 kolom referensi {@code GrupTransaksi}
 * masih belum dikenali {@code ambilUnik()} sama sekali &mdash; entity ini bukan salah satunya
 * (kolomnya dikenali sejak awal).
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <ol>
 *   <li><b>Sebagian getter menulis balik ke kolom uang dan tanggal.</b> Kelas ini dipetakan
 *       <i>property access</i> (anotasi JPA ada di getter) dengan {@code dynamicUpdate = true},
 *       jadi apa pun yang ditugaskan sebuah getter ikut terbaca saat dirty-check dan
 *       <b>tersimpan permanen</b>. Yang menulis balik: {@link #getNilai()} (menimpa
 *       {@code nilai} dengan {@code nilaiFinal}), {@link #getTanggalBayar()} (menimpa tanggal
 *       bayar &mdash; dan karenanya tanggal jurnal), {@link #getMulai()}/{@link #getSampai()}
 *       (mengisi periode dari konfigurasi), {@link #getFormatItemGaji()} (mewarisi format dari
 *       master pegawai), serta {@link #toString()} yang ikut menugaskan dua field relasi.</li>
 *   <li><b>{@code nilai} bukan satu-satunya sumber nominal.</b> Angka yang dijurnal sebagai
 *       kredit ke rekening pegawai adalah {@link #getNilai()}, yang diam-diam menjadi
 *       {@code nilaiFinal} begitu item "final" pernah dihitung. Nilai per komponen yang
 *       didebet/dikredit ke akun biaya berasal dari tabel anak, bukan dari sini.</li>
 *   <li><b>Batal posting lewat layar ZK meninggalkan baris jurnal yatim.</b> Tombol
 *       "Batalkan Posting Data" per baris di {@code PostingTransaksiPenggajianAction} hanya
 *       menghapus {@code akunting.grup_transaksi}, tidak menghapus {@code akunting.transaksi}
 *       anaknya (pola {@code task_5e79a211}); jalur REST {@code batalkanPostingSemua} sudah
 *       benar (hapus baris dulu, baru grupnya). Kedua jalur sama-sama memakai
 *       {@code and closing is null}, jadi periode yang sudah tutup buku tetap terlindungi.</li>
 *   <li><b>Baris ini tidak pernah dihapus otomatis saat posting dibatalkan</b> &mdash; yang
 *       dilepas hanya {@link #getPostingHistory()}, sehingga baris siap diposting ulang.</li>
 *   <li><b>Kelayakan jurnal ditentukan induknya, bukan baris ini.</b> Baik dasbor draft jurnal
 *       ({@code DraftJurnalRingkasanUtil.kriteriaPenggajianPegawai}) maupun mesin posting
 *       menyaring {@code pembayaranGaji.disetujuiOleh IS NOT NULL}. Entity ini sendiri tidak
 *       punya kolom persetujuan.</li>
 * </ol>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; jejak audit:</b> {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *       {@link #toString()}.</li>
 *   <li><b>Relasi:</b> {@link #getPembayaranGaji()}, {@link #getPegawai()},
 *       {@link #getFormatItemGaji()}, {@link #getPostingHistory()}.</li>
 *   <li><b>Nominal:</b> {@link #getNilai()}, {@link #getNilaiFinal()},
 *       {@link #getKomponenGaji()}.</li>
 *   <li><b>Periode &amp; tanggal:</b> {@link #getTanggalBayar()}, {@link #getMulai()},
 *       {@link #getSampai()}, serta pembantu statis {@link #ambilMulai(int, int)} dan
 *       {@link #ambilSampai(int, int)}.</li>
 *   <li><b>Deskriptif:</b> {@link #getKeterangan()}.</li>
 * </ul>
 *
 * <p><b>Catatan bentuk kelas:</b> {@link ais.database.model.GeneralValueObject} <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga
 * Hibernate tidak memetakan properti induknya. Karena itu {@code id}, {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di sini;
 * pengulangan tersebut adalah keharusan teknis, bukan duplikasi yang perlu "dibersihkan".</p>
 *
 * <p><b>Terdaftar di</b> {@code hibernate.cfg.xml} sebagai kelas beranotasi.</p>
 *
 * @see ais.database.model.payroll.PembayaranGaji
 * @see ais.database.model.payroll.PembayaranItemGajiPegawai
 * @see ais.database.model.payroll.FormatItemGaji
 * @see ais.database.model.payroll.TransaksiPegawai
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.PostingHistory
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "pembayaran_gaji_punya_pegawai")
public class PembayaranGajiPunyaPegawai extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sama persis dengan puluhan entity AIS lain
	 * (warisan generator hbm2java 2010) &mdash; jangan dijadikan penanda identitas kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, {@code IDENTITY} dari basis data. Sekaligus penyusun kunci idempotensi jurnal. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> argumen {@code null} atau string kosong <b>diabaikan diam-diam</b>
	 * (nilai lama dipertahankan) &mdash; jejak audit sengaja tidak bisa dikosongkan lewat setter
	 * ini. Konsekuensinya kolom ini tidak pernah bisa "dibersihkan" dari kode aplikasi.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris slip gaji, dipakai ZK untuk label ringkas dan pesan konfirmasi.
	 *
	 * <p><b>Efek samping (tidak lazim untuk sebuah {@code toString()}):</b> method ini
	 * <b>menugaskan</b> hasil {@link #getPembayaranGaji()} dan {@link #getPegawai()} ke field
	 * masing-masing. Karena kedua getter itu melewatkan proxy lewat
	 * {@code GeneralValueObject.check(Object)}, sekadar mencetak object ini dapat memicu
	 * inisialisasi lazy dan penggantian instance proxy dengan instance kanonik. Aman secara
	 * data (nilai tidak berubah), tetapi berarti {@code toString()} <b>bukan</b> operasi murni
	 * dan tidak boleh dipanggil dari konteks yang sesinya sudah tertutup.</p>
	 *
	 * @return gabungan {@code "<header> <pegawai>"}; salah satu bagian bisa berbunyi
	 *         {@code "null"} bila relasinya kosong.
	 */
	public String toString() {
		pembayaranGaji = getPembayaranGaji();
		pegawai = getPegawai();
		return pembayaranGaji + " " + pegawai;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * argumen {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
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
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit tepat sebelum
	 * {@code UPDATE} dikirim ke basis data.
	 *
	 * <p>Dipanggil oleh provider persistence, <b>tidak pernah</b> dari kode aplikasi. Karena
	 * beberapa getter kelas ini menulis balik ke field (lihat {@link #getNilai()} dan
	 * {@link #getTanggalBayar()}), kait ini juga ikut menyala pada "perubahan" yang sebenarnya
	 * hanya lahir dari pembacaan &mdash; {@code tanggal_dirubah} karenanya bisa bergerak tanpa
	 * ada operator yang mengubah apa pun.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Header dokumen penggajian pemilik baris ini (FK {@code pembayaran_gaji}, nullable). */
	private PembayaranGaji pembayaranGaji;
	/** Pegawai penerima slip ini (FK {@code pegawai}, nullable). */
	private Pegawai pegawai;

	/** Catatan bebas per pegawai, tampil di layar detail dan di slip cetak. */
	private String keterangan;
	/** Tanggal bayar; ditulis ulang dari header setiap kali {@link #getTanggalBayar()} dipanggil. */
	private Date tanggalBayar;
	/** Nominal ringkasan baris; dapat ditimpa {@code nilaiFinal} oleh {@link #getNilai()}. */
	private Double nilai;
	/** Snapshot JSON seluruh komponen gaji, bentuk {@code {"idItemGaji": nilai}}. */
	private String komponenGaji;
	/** Cap posting ke buku besar; {@code null} berarti baris ini belum dijurnal. */
	private PostingHistory postingHistory;
	/** Akhir periode kerja yang dibayar; diisi otomatis oleh {@link #getSampai()}. */
	private Date sampai;
	/** Awal periode kerja yang dibayar; diisi otomatis oleh {@link #getMulai()}. */
	private Date mulai;
	/** Nilai item gaji bertanda "final" (gaji bersih/take-home pay) hasil formula. */
	private Double nilaiFinal;
	/** Format/susunan item gaji yang dipakai; menentukan rekening bank tujuan transfer. */
	private FormatItemGaji formatItemGaji;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Seluruh field dibiarkan
	 * kosong; pengisian dilakukan pemanggil (lihat {@code BayarGajiPegawaiAction.bayar} dan
	 * {@code PembayaranGajiPunyaPegawaiAction} "Ambil Data Pegawai").
	 */
	public PembayaranGajiPunyaPegawai() {
	}

	/**
	 * Mengembalikan kunci utama baris slip gaji.
	 *
	 * <p><b>Penting:</b> nilai ini bukan sekadar id teknis &mdash; ia menjadi bagian kunci
	 * idempotensi jurnal {@code "…PembayaranGajiPunyaPegawai_" + id} yang dibentuk
	 * {@link ais.database.model.akunting.GrupTransaksi#ambilUnik()}, dan dipakai apa adanya
	 * dalam SQL mentah pembatalan posting.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya dipakai Hibernate; kode aplikasi tidak boleh memanggilnya
	 * pada baris yang sudah tersimpan.
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan header dokumen penggajian pemilik baris ini.
	 *
	 * <p>Nilai dilewatkan {@code GeneralValueObject.check(Object)} sehingga proxy lazy
	 * diresolusi menjadi instance kanonik lebih dulu; hasil resolusi ditugaskan kembali ke
	 * field, jadi getter ini <b>mengganti instance</b> (bukan nilai) yang dipegang object ini.</p>
	 *
	 * <p>Header inilah yang memegang bulan/tahun periode, cara pembayaran gaji, satuan kerja,
	 * dan &mdash; yang paling menentukan &mdash; kolom {@code disetujuiOleh} yang menjadi
	 * syarat kelayakan penjurnalan seluruh slip di bawahnya.</p>
	 *
	 * @return header {@link ais.database.model.payroll.PembayaranGaji}, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembayaran_gaji", nullable = true)
	public PembayaranGaji getPembayaranGaji() {
		pembayaranGaji = check(pembayaranGaji);
		return pembayaranGaji;
	}

	/**
	 * Menyetel header dokumen penggajian pemilik baris ini.
	 *
	 * @param pembayaranGaji header dokumen penggajian.
	 */
	public void setPembayaranGaji(PembayaranGaji pembayaranGaji) {
		this.pembayaranGaji = pembayaranGaji;
	}

	/**
	 * Mengembalikan pegawai penerima slip ini.
	 *
	 * <p>Seperti {@link #getPembayaranGaji()}, hasil {@code check(Object)} ditugaskan kembali ke
	 * field. Dari object inilah mesin posting mengambil <b>satuan kerja jurnal</b>
	 * ({@code pegawai.getSatuanKerja()}) dan <b>rekening bank tujuan</b>
	 * ({@code pegawai.ambilBank(formatItemGaji)}), jadi relasi ini menentukan bukan hanya
	 * "siapa dibayar" tetapi juga "dibukukan ke tenant mana" dan "dikreditkan ke akun bank
	 * mana".</p>
	 *
	 * @return pegawai penerima, atau {@code null} bila baris belum lengkap.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel pegawai penerima slip ini.
	 *
	 * @param pegawai pegawai penerima.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan catatan bebas per pegawai untuk slip ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas per pegawai.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tanggal bayar slip ini &mdash; <b>dan menimpanya lebih dulu</b>.
	 *
	 * <p><b>Getter destruktif.</b> Method ini <i>selalu</i> menugaskan ulang field
	 * {@code tanggalBayar}, tanpa syarat "hanya bila masih kosong": nilainya diambil dari
	 * {@code pembayaranGaji.getTanggalTransaksi()}, atau &mdash; bila field relasi
	 * {@code pembayaranGaji} kebetulan masih {@code null} &mdash; dari <b>tanggal hari ini</b>.
	 * Karena kelas ini dipetakan property-access dengan {@code dynamicUpdate = true}, penugasan
	 * itu ikut terbaca saat dirty-check dan <b>tersimpan permanen</b> ke kolom
	 * {@code tanggal_bayar_gaji}. Membaca saja bisa memindahkan tanggal bayar historis.</p>
	 *
	 * <p><b>Mengapa ini serius:</b> kolom yang sama dipakai sebagai
	 * <b>tanggal jurnal</b> saat posting ({@code CommonAkunting.saveTransaksi(...,
	 * pembayaranGajiPunyaPegawai.getTanggalBayar(), ...)}) <b>dan</b> sebagai penyaring rentang
	 * pada dasbor draft jurnal maupun mesin posting/batal-posting massal, yang membacanya
	 * lewat SQL mentah {@code date(this_.tanggal_bayar_gaji) between …}. Tanggal yang bergeser
	 * karenanya dapat memindahkan jurnal gaji ke periode akuntansi lain, atau membuat baris
	 * hilang/muncul dari rentang posting massal.</p>
	 *
	 * <p><b>Kuirk teknis:</b> berbeda dari {@link #getMulai()}/{@link #getSampai()} yang
	 * memanggil {@link #getPembayaranGaji()} (aman terhadap proxy), method ini membaca
	 * <b>field mentah</b> {@code pembayaranGaji}. Pada object yang relasinya belum pernah
	 * disentuh, cabang fallback "hari ini" bisa menang meski headernya sebenarnya ada.</p>
	 *
	 * @return tanggal bayar hasil penugasan ulang; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_bayar_gaji")
	public Date getTanggalBayar() {
		tanggalBayar = (pembayaranGaji != null ? pembayaranGaji.getTanggalTransaksi()
				: ais.ui.util.WaktuUtil.getDate());
		return tanggalBayar;
	}

	/**
	 * Menyetel tanggal bayar.
	 *
	 * <p>Nilai yang disetel di sini <b>tidak bertahan</b> terhadap pemanggilan
	 * {@link #getTanggalBayar()} berikutnya, yang akan menimpanya kembali dari header. Setter
	 * ini praktis hanya berguna pada baris yang belum punya header (mis. saat pembuatan di
	 * {@code BayarGajiPegawaiAction}).</p>
	 *
	 * @param tanggalBayar tanggal bayar.
	 */
	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	/**
	 * Mengembalikan cap posting baris ini ke buku besar.
	 *
	 * <p>Aksesor murni (tidak menulis balik). Keberadaan cap inilah satu-satunya penanda
	 * "slip ini sudah dijurnal": mesin posting massal menyaring
	 * {@code postingHistory IS NULL} untuk memilih kandidat, dan pembatalan posting melepasnya
	 * kembali ke {@code null} setelah menghapus jurnalnya. Satu object
	 * {@link ais.database.model.akunting.PostingHistory} dipakai bersama oleh seluruh slip
	 * dalam satu batch posting.</p>
	 *
	 * @return cap posting, atau {@code null} bila belum diposting.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel cap posting baris ini.
	 *
	 * <p>Dipanggil mesin posting setelah jurnal berhasil ditulis, dan dipanggil ulang dengan
	 * {@code null} oleh tombol/endpoint pembatalan posting.</p>
	 *
	 * @param postingHistory cap posting, atau {@code null} untuk melepas cap.
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan nominal slip yang dipakai sebagai <b>nilai kredit ke rekening pegawai</b>
	 * saat penjurnalan &mdash; <b>dan menimpanya lebih dulu</b>.
	 *
	 * <p><b>Getter destruktif atas kolom uang.</b> Bila {@link #getNilaiFinal()} terisi dan
	 * bagian bulatnya bukan nol, field {@code nilai} <b>ditimpa</b> dengan nilai final itu.
	 * Sekali lagi: kelas ini property-access + {@code dynamicUpdate}, jadi penimpaan tersebut
	 * ikut ter-flush ke kolom {@code nilai} pada sesi Hibernate yang hidup. Angka gaji yang
	 * tersimpan di basis data karenanya dapat berubah <b>hanya karena baris ini dibaca</b>
	 * (mis. saat me-render daftar, mencetak laporan rekap, atau menyusun dasbor).</p>
	 *
	 * <p><b>Semantik dua angka:</b> {@code nilai} adalah total hasil penjumlahan seluruh
	 * komponen yang dihitung ulang {@code PembayaranItemGajiPegawaiTreeModel.reset()},
	 * sedangkan {@code nilaiFinal} adalah nilai satu item yang ditandai
	 * {@code itemGajiPegawai.getFinalGaji()} pada format gaji &mdash; yaitu gaji bersih yang
	 * benar-benar ditransfer. Getter ini secara sengaja mengutamakan yang kedua, sehingga
	 * pemanggil tidak perlu tahu bedanya; harganya adalah nilai kotor yang pernah tersimpan
	 * tidak dapat dipulihkan dari kolom ini.</p>
	 *
	 * <p><b>Kasus tepi:</b>
	 * <ul>
	 *   <li>{@code nilaiFinal} bernilai pecahan kecil ({@code 0 < x < 1}) &rarr;
	 *       {@code intValue() == 0} &rarr; dianggap "belum ada nilai final", {@code nilai}
	 *       lama dipertahankan;</li>
	 *   <li>{@code nilaiFinal} negatif (potongan melebihi penghasilan) &rarr;
	 *       {@code intValue() != 0} &rarr; <b>tetap dipakai</b>, sehingga nominal kredit jurnal
	 *       bisa negatif;</li>
	 *   <li>keduanya kosong &rarr; dikembalikan {@code 0.0} (bukan {@code null}), sehingga
	 *       pemanggil tidak pernah kena NPE tetapi juga tidak bisa membedakan "nol" dari
	 *       "belum dihitung".</li>
	 * </ul>
	 *
	 * <p><b>Dipanggil dari:</b> ketiga jalur {@code PostingTransaksiPenggajianAction} (nilai
	 * kredit bank/cara pembayaran), {@code PostingTransaksiPembayaranGajiAction},
	 * {@code PembayaranGajiPunyaPegawaiPerBankAction} (subtotal per bank yang menjadi nilai
	 * Standing Instruction), serta seluruh laporan rekap penggajian.</p>
	 *
	 * @return nominal slip; tidak pernah {@code null}.
	 */
	public Double getNilai() {

		if (getNilaiFinal() != null && getNilaiFinal().intValue() != 0) {
			nilai = getNilaiFinal();
		}

		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel nominal ringkasan baris.
	 *
	 * <p>Dipanggil {@code PembayaranItemGajiPegawaiTreeModel.reset()} setelah seluruh komponen
	 * dihitung ulang. Perhatikan bahwa nilai yang disetel di sini akan <b>ditimpa kembali</b>
	 * oleh {@link #getNilai()} begitu {@code nilaiFinal} terisi.</p>
	 *
	 * @param nilai nominal ringkasan.
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Object JSON kosong yang dipakai bersama sebagai nilai bawaan {@link #getKomponenGaji()}.
	 *
	 * <p><b>Kuirk:</b> field {@code static} yang bisa berubah (mutable) dipakai lintas thread.
	 * Aman <i>saat ini</i> hanya karena tidak ada satu pun kode yang memodifikasinya &mdash;
	 * ia selalu di-{@code toString()} menjadi {@code "{}"}. Jangan menambahkan kunci ke object
	 * ini.</p>
	 */
	private static JSONObject D = new JSONObject();

	/**
	 * Mengembalikan snapshot JSON seluruh komponen gaji slip ini.
	 *
	 * <p>Bentuknya {@code {"<idItemGaji>": <nilai>, …}} dan ditulis
	 * {@code PembayaranItemGajiPegawaiTreeModel} saat perhitungan gaji. Snapshot ini adalah
	 * jalan pintas pelaporan: {@code LaporanPembayaranGaji}, {@code LaporanRekapPembayaranGaji},
	 * {@code LaporanRekapPembayaranGajiSatuanKerja},
	 * {@code LaporanRekapPembayaranGajiPerSatuanKerja}, dan
	 * {@code LaporanRekapPembayaranBerdasarkanFormatNilai} membacanya langsung alih-alih
	 * menjelajah tabel anak {@code PembayaranItemGajiPegawai} per baris.</p>
	 *
	 * <p><b>Non-obvious:</b> kolomnya {@code text} dan getter ini menormalkan {@code null}
	 * maupun string kosong menjadi {@code "{}"}, sehingga pemanggil selalu bisa langsung
	 * mem-parsing hasilnya tanpa cek null. Karena ini <b>snapshot</b>, isinya bisa tertinggal
	 * dari tabel anak bila komponen diubah lewat jalur yang tidak memanggil {@code reset()};
	 * laporan rekap dan rincian slip bisa berbeda tanpa peringatan.</p>
	 *
	 * @return JSON komponen gaji; tidak pernah {@code null}, minimal {@code "{}"}.
	 */
	@Column(columnDefinition = "text")
	public String getKomponenGaji() {
		return komponenGaji == null || komponenGaji.trim().isEmpty() ? D.toString() : komponenGaji;
	}

	/**
	 * Menyetel snapshot JSON komponen gaji.
	 *
	 * @param komponenGaji JSON {@code {"<idItemGaji>": <nilai>}}; boleh {@code null}
	 *                     (dibaca kembali sebagai {@code "{}"}).
	 */
	public void setKomponenGaji(String komponenGaji) {
		this.komponenGaji = komponenGaji;
	}

	/**
	 * Menghitung <b>tanggal awal</b> periode absensi/penggajian untuk bulan dan tahun tertentu.
	 *
	 * <p>Titik awal periode tidak selalu tanggal 1: nilainya diambil dari konfigurasi global
	 * {@code tanggal_mulai_absensi} (bawaan {@code "1"}), sehingga instalasi yang menutup
	 * absensi mis. tanggal 21 akan memperoleh periode 21 bulan ini sampai 20 bulan depan.</p>
	 *
	 * <p><b>Efek samping tersembunyi:</b> {@code Common.getKonfigurasi(kunci, bawaan)} akan
	 * <b>menulis baris konfigurasi bawaan ke basis data</b> bila kuncinya belum ada &mdash;
	 * jadi pemanggilan pertama method ini dapat menyemai konfigurasi, bukan sekadar
	 * membacanya.</p>
	 *
	 * <p><b>Kasus tepi:</b> kegagalan parsing (nilai konfigurasi bukan angka) ditangkap dan
	 * dicatat ke audit error, lalu diam-diam jatuh ke tanggal 1 &mdash; periode tetap
	 * terbentuk, tidak melempar. Argumen {@code bulan} yang berada di luar 0..11 tidak ditolak:
	 * {@code Calendar} bekerja dalam mode lenient sehingga nilai negatif atau &gt; 11
	 * bergulir ke tahun sebelumnya/berikutnya &mdash; sifat inilah yang justru diandalkan
	 * {@link #getMulai()} saat menerapkan offset konfigurasi. Komponen jam/menit/detik
	 * <b>tidak</b> dinolkan (diwarisi dari waktu server saat ini); tidak berpengaruh pada
	 * kolom {@code DATE}, tetapi berpengaruh bila hasilnya dibandingkan sebagai timestamp.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@link #getMulai()}, {@code BayarGajiPegawaiAction}, dan
	 * {@code LaporanSlipGajiPegawaiPerOrang}.</p>
	 *
	 * @param tahun tahun kalender (4 digit).
	 * @param bulan indeks bulan bergaya {@link java.util.Calendar} (0 = Januari); boleh di luar
	 *              rentang, akan bergulir.
	 * @return tanggal awal periode.
	 */
	public static Date ambilMulai(int tahun, int bulan) {
		int tanggalMulaiAbsensi = 1;
		try {
			tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/PembayaranGajiPunyaPegawai.java:201");
		}
		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		calendarUtama.set(Calendar.YEAR, tahun);
		calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi);
		calendarUtama.set(Calendar.MONTH, bulan);
		return calendarUtama.getTime();
	}

	/**
	 * Menghitung <b>tanggal akhir</b> periode absensi/penggajian untuk bulan dan tahun tertentu.
	 *
	 * <p>Kembar dari {@link #ambilMulai(int, int)} dengan satu perbedaan: tanggalnya
	 * {@code tanggal_mulai_absensi - 1}. Untuk konfigurasi bawaan ({@code 1}) hasilnya adalah
	 * {@code DATE = 0}, yang dalam mode lenient {@link java.util.Calendar} berarti
	 * <b>hari terakhir bulan sebelumnya</b> &mdash; inilah sebabnya {@link #getSampai()}
	 * memanggilnya dengan indeks bulan satu lebih besar daripada {@link #getMulai()}.
	 * Kombinasi kedua kuirk itu menghasilkan periode "1 s.d. akhir bulan" yang benar, tetapi
	 * lewat jalan yang sangat tidak jelas bila dibaca sepotong.</p>
	 *
	 * <p>Efek samping penyemaian konfigurasi dan penanganan kegagalan parsing identik dengan
	 * {@link #ambilMulai(int, int)}.</p>
	 *
	 * @param tahun tahun kalender (4 digit).
	 * @param bulan indeks bulan bergaya {@link java.util.Calendar} (0 = Januari); boleh di luar
	 *              rentang, akan bergulir.
	 * @return tanggal akhir periode.
	 */
	public static Date ambilSampai(int tahun, int bulan) {
		int tanggalMulaiAbsensi = 1;
		try {
			tanggalMulaiAbsensi = Integer.parseInt(Common.getKonfigurasi("tanggal_mulai_absensi", "1").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/PembayaranGajiPunyaPegawai.java:214");
		}
		Calendar calendarUtama = ais.ui.util.WaktuUtil.getCalendar();
		calendarUtama.set(Calendar.YEAR, tahun);
		calendarUtama.set(Calendar.DATE, tanggalMulaiAbsensi - 1);
		calendarUtama.set(Calendar.MONTH, bulan);
		return calendarUtama.getTime();
	}

	/**
	 * Mengembalikan tanggal awal periode kerja yang dibayar slip ini, <b>mengisinya lebih dulu
	 * bila masih kosong</b>.
	 *
	 * <p>Berbeda dari {@link #getTanggalBayar()} dan {@link #getNilai()}, penulisan balik di
	 * sini bersifat <b>lazy-init</b> (hanya bila {@code mulai == null}) sehingga nilai yang
	 * sudah tersimpan tidak pernah ditimpa. Meski begitu efeknya tetap persisten: pada sesi
	 * Hibernate yang hidup, pembacaan pertama akan menuliskan kolom {@code mulai}.</p>
	 *
	 * <p>Nilainya dihitung {@link #ambilMulai(int, int)} dari bulan/tahun header, digeser
	 * konfigurasi global {@code plus_minus_penambahan_bulan_penggajian} (bawaan {@code "0"}).
	 * Pergeseran itu memungkinkan instalasi yang membayar gaji bulan berjalan di muka atau di
	 * belakang. Perhatikan {@code getBulan() - 1}: kolom {@code bulan} disimpan bergaya manusia
	 * (1 = Januari) sementara {@link java.util.Calendar} berbasis 0.</p>
	 *
	 * <p><b>Kasus tepi:</b> tidak ada penjaga bila konfigurasi berisi teks non-numerik &mdash;
	 * {@code Integer.parseInt} akan melempar {@code NumberFormatException} yang <b>tidak</b>
	 * ditangkap di sini dan merambat ke pemanggil (berbeda dari {@code parseInt} di dalam
	 * {@link #ambilMulai(int, int)} yang dibungkus {@code try}). Bila header belum ada, atau
	 * bulan/tahunnya {@code null}, method mengembalikan {@code null} apa adanya.</p>
	 *
	 * @return tanggal awal periode, atau {@code null} bila header/periodenya belum lengkap.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {

		if (mulai == null && getPembayaranGaji() != null && getPembayaranGaji().getTahun() != null
				&& getPembayaranGaji().getBulan() != null) {
			pembayaranGaji = getPembayaranGaji();
			mulai = PembayaranGajiPunyaPegawai.ambilMulai(pembayaranGaji.getTahun(),
					pembayaranGaji.getBulan() - 1 + (Integer.parseInt(
							Common.getKonfigurasi("plus_minus_penambahan_bulan_penggajian", "0").getNilai().trim())));
		}

		return mulai;
	}

	/**
	 * Menyetel tanggal awal periode kerja yang dibayar.
	 *
	 * @param mulai tanggal awal periode; boleh {@code null} (akan dihitung ulang oleh
	 *              {@link #getMulai()}).
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal akhir periode kerja yang dibayar slip ini, <b>mengisinya lebih dulu
	 * bila masih kosong</b>.
	 *
	 * <p>Kembar {@link #getMulai()}, dengan satu perbedaan yang mudah terlewat: indeks bulan
	 * dikirim sebagai {@code getBulan() - 0} (bukan {@code - 1}), yaitu <b>satu bulan lebih
	 * maju</b>. Dipadukan dengan {@code DATE = tanggal_mulai_absensi - 1} di
	 * {@link #ambilSampai(int, int)}, hasil akhirnya adalah hari terakhir bulan periode.
	 * Kedua "kesalahan" itu saling meniadakan; mengoreksi salah satunya saja akan menggeser
	 * seluruh periode penggajian.</p>
	 *
	 * <p>Efek persistensi, pergeseran konfigurasi, dan kasus tepi
	 * {@code NumberFormatException} identik dengan {@link #getMulai()}.</p>
	 *
	 * @return tanggal akhir periode, atau {@code null} bila header/periodenya belum lengkap.
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {

		if (sampai == null && getPembayaranGaji() != null && getPembayaranGaji().getTahun() != null
				&& getPembayaranGaji().getBulan() != null) {
			pembayaranGaji = getPembayaranGaji();
			sampai = PembayaranGajiPunyaPegawai.ambilSampai(pembayaranGaji.getTahun(),
					pembayaranGaji.getBulan() - 0 + (Integer.parseInt(
							Common.getKonfigurasi("plus_minus_penambahan_bulan_penggajian", "0").getNilai().trim())));
		}

		return sampai;
	}

	/**
	 * Menyetel tanggal akhir periode kerja yang dibayar.
	 *
	 * @param sampai tanggal akhir periode; boleh {@code null} (akan dihitung ulang oleh
	 *               {@link #getSampai()}).
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan nilai item gaji bertanda "final", yaitu <b>gaji bersih (take-home pay)</b>
	 * hasil formula.
	 *
	 * <p>Aksesor murni. Kolom ini <b>tidak diisi operator</b>: penulisnya adalah mesin
	 * perhitungan gaji &mdash; {@code PembayaranItemGajiPegawaiTreeModel} (jalur hitung-ulang
	 * massal) dan {@code BayarGajiPegawaiAction} (jalur per pegawai) &mdash; yang menyimpan ke
	 * sini hasil item yang ditandai {@code itemGajiPegawai.getFinalGaji()}.</p>
	 *
	 * <p><b>Non-obvious:</b> kedua penulis memakai penjaga yang sama, yaitu hanya menulis bila
	 * nilai lama {@code null} <b>atau</b> hasil baru {@code > 0.1} dan bagian bulatnya berbeda.
	 * Akibatnya gaji bersih yang <b>turun menjadi nol atau negatif</b> tidak pernah menimpa
	 * nilai lama yang lebih besar &mdash; angka lama bertahan dan tetap dipakai
	 * {@link #getNilai()} sebagai nominal jurnal.</p>
	 *
	 * @return gaji bersih hasil formula, atau {@code null} bila belum pernah dihitung.
	 */
	public Double getNilaiFinal() {
		return nilaiFinal;
	}

	/**
	 * Menyetel nilai item gaji bertanda "final" (gaji bersih).
	 *
	 * <p>Hanya dipanggil mesin perhitungan gaji; jangan dipanggil dari layar entri.</p>
	 *
	 * @param nilaiFinal gaji bersih hasil formula.
	 */
	public void setNilaiFinal(Double nilaiFinal) {
		this.nilaiFinal = nilaiFinal;
	}

	/**
	 * Mengembalikan format/susunan item gaji yang dipakai slip ini, <b>dengan pewarisan diam-diam
	 * dari master pegawai</b>.
	 *
	 * <p>Alurnya dua langkah: (1) proxy diresolusi lewat {@code check(Object)} dan hasilnya
	 * ditugaskan kembali ke field; (2) bila hasilnya {@code null}, field <b>diisi</b> dengan
	 * {@code pegawai.getFormatItemGaji()}. Langkah kedua adalah penulisan balik yang
	 * persisten &mdash; slip yang sengaja dibiarkan tanpa format akan <b>permanen</b>
	 * mengadopsi format master pegawai pada pembacaan pertama.</p>
	 *
	 * <p><b>Mengapa ini bukan sekadar kenyamanan tampilan:</b> format item gaji adalah argumen
	 * {@code pegawai.ambilBank(formatItemGaji)} dan {@code pegawai.ambilNoRek(formatItemGaji)},
	 * yaitu penentu <b>rekening bank tujuan transfer</b> dan <b>akun kredit jurnal</b> pada
	 * {@code PostingTransaksiPenggajianAction}, {@code PostingTransaksiPembayaranGajiAction},
	 * serta subtotal per bank di {@code PembayaranGajiPunyaPegawaiPerBankAction} yang menjadi
	 * nilai Standing Instruction. Format yang berubah karena pewarisan otomatis dapat
	 * memindahkan uang ke rekening lain dan jurnalnya ke akun lain.</p>
	 *
	 * <p><b>Kasus tepi:</b> bila pegawai juga belum punya format, hasilnya tetap {@code null};
	 * mesin posting kemudian jatuh ke akun {@code caraPembayaranGaji} milik header, dan
	 * {@code PembayaranGajiPunyaPegawaiHelper} menolak melanjutkan proses dengan pesan yang
	 * meminta operator melengkapi Format Item Gaji di menu Data Pegawai.</p>
	 *
	 * @return format item gaji, atau {@code null} bila slip maupun pegawainya belum punya.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji", nullable = true)
	public FormatItemGaji getFormatItemGaji() {
		formatItemGaji = check(formatItemGaji);

		if (formatItemGaji == null) {
			if (getPegawai() != null && getPegawai().getFormatItemGaji() != null) {
				formatItemGaji = getPegawai().getFormatItemGaji();
			}
		}

		return formatItemGaji;
	}

	/**
	 * Menyetel format/susunan item gaji yang dipakai slip ini.
	 *
	 * @param formatItemGaji format item gaji.
	 */
	public void setFormatItemGaji(FormatItemGaji formatItemGaji) {
		this.formatItemGaji = formatItemGaji;
	}
}
