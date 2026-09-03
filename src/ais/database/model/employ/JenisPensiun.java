package ais.database.model.employ;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;



/**
 * Model data untuk jenis pensiun. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code String nama}, {@code Date tanggal_dirubah}; pemetaan
 * persistence: tabel {@code employ.jenis_pensiun}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()},
 * {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getKeterangan()}, {@code getNama()}); mutasi data
 * ({@code setOlehId()}, {@code onUpdate()}, {@code setId()}, {@code setOleh()}, {@code setTanggal_dirubah()},
 * {@code setKeterangan()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * <h2>Peran dalam klaster karir kepegawaian</h2>
 * <p>Tabel master/referensi murni yang mengelompokkan berkas {@link Pensiun} menurut dasar
 * berakhirnya masa dinas — misalnya "Batas Usia Pensiun", "Atas Permintaan Sendiri", "Meninggal
 * Dunia", atau "Tidak Cakap Jasmani/Rohani". Sama seperti {@link JenisKenaikanPangkat}, isinya
 * seluruhnya dimasukkan operator melalui {@code ais.action.master.employ.JenisPensiunAction} dan
 * <b>tidak</b> di-seed konstanta statis seperti {@link TipeMasaKerja}/{@link TipePegawai}. Karena itu
 * tidak ada kode Java yang mencocokkan nama jenis tertentu; jenis pensiun berfungsi sebagai label
 * pengelompokan dan bahan pelaporan, bukan pemicu perhitungan hak pensiun.</p>
 *
 * <p>Dua relasi masuk menunjuk entity ini, dengan kewajiban yang berbeda:</p>
 * <ul>
 * <li>{@link Pensiun#getJenisPensiun()} — kolom {@code jenisPensiun}, {@code nullable = false}: setiap
 * berkas pensiun <b>wajib</b> menyebut jenisnya.</li>
 * <li>{@link MutasiPindah#getJenisPensiun()} — kolom {@code jenisPensiun}, {@code nullable = true}:
 * field sisa dari penyalinan struktur {@link Pensiun} saat {@link MutasiPindah} dibuat. Pada berkas
 * mutasi antar unit kerja field ini normalnya dibiarkan kosong.</li>
 * </ul>
 *
 * <p><b>Catatan tenancy:</b> tidak ada kolom satuan kerja/tenant pada tabel ini, sehingga daftar jenis
 * pensiun berlaku global untuk seluruh instansi dalam satu basis data. Penyaringan per satuan kerja,
 * bila dibutuhkan, harus dilakukan di lapisan Action/DAO.</p>
 *
 * @see GeneralValueObject
 * @see Pensiun
 * @see MutasiPindah
 * @see JenisKenaikanPangkat
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "jenis_pensiun")



public class JenisPensiun extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai ini dipakai bersama oleh banyak entity di paket
	 * {@code employ} karena berkas-berkas tersebut disalin dari template yang sama; angkanya tidak
	 * memiliki makna bisnis dan tidak boleh diubah tanpa alasan, sebab perubahan akan mematahkan
	 * deserialisasi state ZK/HTTP session yang tersimpan dari versi aplikasi sebelumnya.
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Kunci utama baris {@code employ.jenis_pensiun}; diisi database (IDENTITY). */
	private Long id;
	/** Nama/identitas petugas terakhir yang menyimpan baris ini -- jejak audit tampilan. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini -- jejak audit yang dapat ditelusuri balik. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir menyimpan baris ini. Dipakai untuk menelusuri balik siapa yang
	 * memasukkan atau mengubah jenis pensiun, terpisah dari {@link #getOleh()} yang menyimpan nama
	 * tampilan.
	 *
	 * <p>Nilainya dapat {@code null} untuk baris warisan (dibuat sebelum kolom audit ini
	 * diperkenalkan) maupun untuk baris yang disimpan proses batch tanpa konteks pengguna. Pemanggil
	 * wajib menangani {@code null}.</p>
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila tidak tercatat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Mengabaikan (tidak menimpa nilai lama) bila masukan
	 * {@code null}/kosong-setelah-trim -- pola pengaman umum di entity domain kepegawaian agar jejak
	 * audit "olehId" tidak pernah ditimpa kosong oleh pemanggil yang lalai mengisinya.
	 *
	 * <p><b>Konsekuensi yang harus disadari:</b> setter ini bersifat <i>satu arah</i>. Sekali terisi,
	 * nilai lama tidak dapat dikosongkan lewat setter ini; pengosongan hanya mungkin lewat UPDATE SQL
	 * langsung. Ini disengaja untuk melindungi jejak audit, bukan cacat.</p>
	 *
	 * @param olehId id pengguna penyimpan; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Keterangan bebas untuk jenis pensiun ini; juga menjadi label {@link #toString()}. */
	private String keterangan;
	/** Nama jenis pensiun (mis. "Batas Usia Pensiun"); wajib terisi di database. */
	private String nama;

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu. Deklarasi one-liner mengikuti gaya
	 * berkas hbm2java asli (tidak dirapikan agar diff minimal terhadap riwayat SVN).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Kunci utama baris {@code employ.jenis_pensiun}.
	 *
	 * <p>Dihasilkan database dengan strategi {@code IDENTITY} dan dipetakan
	 * {@code insertable = false}, sehingga nilai yang diisi manual pada objek baru <b>diabaikan</b>
	 * saat {@code INSERT}. Objek yang belum tersimpan mengembalikan {@code null}; kondisi ini yang
	 * dipakai lapisan Action untuk membedakan "baris baru" dari "baris hasil muat".</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter {@link #getId()}. Umumnya hanya dipanggil Hibernate saat memuat/menyimpan baris, atau
	 * oleh kode yang sengaja membentuk referensi ringan ke baris yang sudah ada. Menyetel id pada
	 * entity yang sudah terkelola session dapat membuat Hibernate memperlakukannya sebagai entity
	 * lain; hindari di luar konteks tersebut.
	 *
	 * @param id nilai kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Setter {@link #getOleh()} -- pola pengaman sama dengan {@link #setOlehId(String)}: masukan
	 * {@code null}/kosong-setelah-trim diabaikan sehingga nama penyimpan lama tidak tertimpa kosong.
	 *
	 * @param oleh nama penyimpan; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama/identitas petugas yang terakhir menyimpan baris jenis pensiun ini -- jejak audit untuk
	 * ditampilkan di layar. Untuk penelusuran teknis gunakan {@link #getOlehId()}.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila tidak tercatat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Setter {@link #getTanggal_dirubah()}. Normalnya <b>tidak perlu dipanggil manual</b> karena
	 * {@link #onUpdate()} sudah menyetelnya otomatis sebelum tiap {@code UPDATE}. Pemanggilan manual
	 * hanya masuk akal pada migrasi/impor data yang ingin mempertahankan cap waktu asli.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris ini. Diinisialisasi ke waktu pembuatan objek melalui
	 * {@code WaktuUtil.getDate()} (bukan {@code new Date()}, agar mengikuti sumber waktu tunggal
	 * aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}.
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibentuk di JVM
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Label teks entity ini.
	 *
	 * <p><b>Perhatikan:</b> yang dikembalikan adalah {@link #getKeterangan() keterangan}, <b>bukan</b>
	 * {@link #getNama() nama}, padahal {@code keterangan} boleh {@code null} sedangkan {@code nama}
	 * wajib terisi. Akibatnya komponen ZK yang menampilkan objek ini apa adanya akan memperlihatkan
	 * baris kosong bila operator tidak mengisi keterangan. Perilaku ini identik dengan
	 * {@link JenisKenaikanPangkat}; jangan diubah tanpa memeriksa layar yang mengandalkannya.</p>
	 *
	 * @return keterangan baris ini, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Keterangan bebas mengenai jenis pensiun ini, misalnya rujukan peraturan atau syarat yang
	 * berlaku. Boleh {@code null}. Perhatikan bahwa nilai ini juga dipakai sebagai hasil
	 * {@link #toString()}.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Setter {@link #getKeterangan()}. Menerima {@code null} apa adanya (tanpa pengaman kosong,
	 * berbeda dengan setter kolom audit) sehingga keterangan memang dapat dikosongkan kembali.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama jenis pensiun, misalnya "Batas Usia Pensiun" atau "Atas Permintaan Sendiri". Kolom
	 * {@code nama} dipetakan {@code nullable = false}, jadi baris tanpa nama ditolak database saat
	 * {@code INSERT}.
	 *
	 * <p>Getter ini mengembalikan nilai apa adanya tanpa {@code trim()} defensif (berbeda dengan
	 * {@link TipeMasaKerja#getNama()}), sehingga spasi di ujung yang terbawa dari formulir ikut
	 * tersimpan dan ikut terbandingkan saat pencarian.</p>
	 *
	 * @return nama jenis pensiun; {@code null} hanya untuk objek yang belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	/**
	 * Setter {@link #getNama()}. Nilai wajib diisi sebelum penyimpanan karena kolomnya
	 * {@code nullable = false}; validasi pengisian dilakukan lapisan Action, bukan di sini.
	 *
	 * @param nama nama jenis pensiun
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

}
