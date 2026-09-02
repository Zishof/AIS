package ais.database.model.akunting;

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

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Entity <b>pengajuan penggantian (reimbursement) dana kas kecil</b>, dipetakan ke tabel
 * {@code akunting.penggantian_kas_kecil}.
 *
 * <h2>Posisi dalam alur akunting kas kecil</h2>
 *
 * <p>Kas kecil bekerja dengan sistem dana tetap (<i>imprest</i>): satuan kerja memegang saldo
 * kas kecil, memakainya untuk pengeluaran kecil, lalu <b>mengajukan penggantian</b> agar saldo
 * kembali ke jumlah semula. Baris {@code KasKecil} mencatat <i>penggunaan</i>-nya; baris
 * {@code PenggantianKasKecil} ini mencatat <i>permintaan penggantian atas penggunaan itu</i>.
 * Rantai lengkapnya:</p>
 *
 * <ol>
 *   <li>{@link KasKecil} — pengajuan/penggunaan dana kas kecil oleh satuan kerja.</li>
 *   <li><b>{@code PenggantianKasKecil}</b> (kelas ini) — permintaan penggantian, menunjuk satu
 *       {@link KasKecil} lewat {@link #getKasKecil()} (kolom {@code kas_kecil}).</li>
 *   <li>{@link DisposisiSop} — alur persetujuan berjenjang; dari sinilah status, penyetuju, dan
 *       tanggal persetujuan sebenarnya <b>diturunkan</b> (lihat bagian berikutnya).</li>
 *   <li>{@link DaftarPengajuanTransfer} — begitu disetujui, dokumen ini dijadwalkan sebagai
 *       transfer bank oleh {@link DaftarPengajuanTransfer#simpanPenggantianKasKecil}
 *       (dipanggil dari {@code PenggantianKasKecilAction}). Relasi dua arah:
 *       {@link #getDaftarPengajuanTransfer()} di sini, {@code getPenggantianKasKecil()} di sana.</li>
 *   <li>{@link PostingHistory} — setelah transfer terealisasi, dokumen diposting menjadi jurnal
 *       oleh {@code PostingPenggantianKasKecilAction}
 *       (jenis {@link PostingHistory#JENIS_PENGGANTIAN_KAS_KECIL}). Kolom
 *       {@code posting_history} yang terisi = sudah diposting.</li>
 * </ol>
 *
 * <p>Relasi ke {@link KasKecil} bersifat <b>dua arah dan simetris</b>: kelas ini memegang kolom
 * {@code kas_kecil}, sementara {@link KasKecil} memegang kolom {@code penggantian_kas_kecil}.
 * Keduanya dipetakan {@code @ManyToOne} (bukan {@code @OneToOne}) sehingga secara skema tidak
 * ada jaminan pasangannya unik; secara pemakaian di kode, satu penggantian melayani <b>satu</b>
 * pengajuan kas kecil. {@link KasKecil} bahkan <i>meminjam</i> penyetuju, tanggal persetujuan,
 * dan disposisi dari penggantian ini bila miliknya sendiri kosong.</p>
 *
 * <h2>Rantai pewarisan &amp; pengulangan field yang DISENGAJA</h2>
 *
 * <p>{@code PenggantianKasKecil} → {@link DataSop} → {@link ais.database.model.GeneralValueObject}.
 * {@link DataSop} hanya menambahkan kontrak abstrak {@code getDisposisiSop()}/
 * {@code setDisposisiSop(...)} supaya semua dokumen ber-alur SOP bisa diperlakukan seragam oleh
 * mesin disposisi.</p>
 *
 * <p><b>PENTING — jangan "dibersihkan":</b>
 * {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa. Hibernate karena itu <b>tidak</b>
 * memetakan properti milik induk. Deklarasi ulang {@link #id}, {@link #oleh}, {@link #olehId},
 * dan {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi yang keliru</b>, melainkan
 * keharusan teknis agar kolom-kolom audit dasar ikut terpetakan. Menghapusnya akan menghilangkan
 * kolom-kolom itu dari tabel. Penjelasan lengkap pola ini ada di
 * {@link ais.database.model.GeneralValueObject}.</p>
 *
 * <h2>Getter di kelas ini BUKAN getter polos — banyak yang MENULIS</h2>
 *
 * <p>Ini karakter paling penting dan paling mudah membuat salah paham. Hibernate memakai
 * <b>akses properti</b> (anotasi ada di getter), sehingga getter-getter di bawah ikut dipanggil
 * saat <i>dirty checking</i> dan <i>flush</i>. Nilai yang mereka hitung karena itu <b>ikut
 * tersimpan permanen ke database</b> — dan karena kelas ini {@code @Audited}, ikut pula tercatat
 * di tabel revisi Envers. Daftar getter yang menulis balik ke field:</p>
 *
 * <ul>
 *   <li>{@link #getAktif()} — menulis {@link #aktif} dan {@link #disposisiSop}; secara tidak
 *       langsung juga {@link #status} dan {@link #disetujuiOleh} lewat {@link #getStatus()}.</li>
 *   <li>{@link #getStatus()} — menulis {@link #status} (bisa {@code Disetujui} atau
 *       {@code Ditolak}) dan {@link #disposisiSop}.</li>
 *   <li>{@link #getNilai()} dan {@link #getSaldo()} — <b>menyalin ulang</b> dari
 *       {@link KasKecil#getNilai()}/{@link KasKecil#getSaldo()}. Kolom {@code nilai} dan
 *       {@code saldo} di tabel ini praktis hanya cermin nilai kas kecil, bukan data mandiri.</li>
 *   <li>{@link #getSatuanKerja()} — menyalin ulang dari satuan kerja kas kecil.</li>
 *   <li>{@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 *       {@link #getTanggalPembuatan()} — diturunkan dari simpul-simpul {@link DisposisiSop}.</li>
 *   <li>{@link #getTahun()} dan {@link #getBulan()} — mengisi <b>tahun/bulan HARI INI</b> bila
 *       masih kosong (lihat peringatan di method-nya).</li>
 *   <li>{@link #getNomorSuratAlurKeuangan()} — mengisi default statis bila kosong.</li>
 *   <li>{@link #getKodeUnik()} — <b>menyusun ulang</b> nilai kolom {@code unique} setiap kali
 *       dipanggil.</li>
 *   <li>{@link #getTanggalTransaksi()} — menurunkan tanggal dari jalur transfer.</li>
 * </ul>
 *
 * <p><b>Getter yang menutup sesi Hibernate:</b> tidak ada satu pun di kelas ini. Kelas ini tidak
 * pernah membuka {@code Session} sendiri. Yang ada hanya pemakaian
 * {@link ais.database.model.GeneralValueObject#check(Object)} pada getter relasi; {@code check()}
 * itulah yang — sebagai upaya terakhir — bisa membuka dan menutup sesi baru untuk memuat ulang
 * entity yang proxy-nya sudah terlepas. Konsekuensi biaya dan jebakannya dijelaskan di
 * {@link ais.database.model.GeneralValueObject}.</p>
 *
 * <h2>VERIFIKASI pola "flag {@code aktif} satu-arah"</h2>
 *
 * <p>Beberapa entity akunting sejenis diduga memakai pola "{@code aktif} sekali dipaksa
 * {@code false} tidak pernah kembali {@code true}". <b>Hasil pembacaan langsung kode kelas ini:
 * pola tersebut ADA, tetapi TIDAK MUTLAK</b> — {@link #getAktif()} di sini punya jalur pemulihan
 * yang tidak dimiliki {@code PengajuanMahasiswa}:</p>
 *
 * <ol>
 *   <li>Cabang <b>pertama</b> justru menaikkan kembali: bila {@link #getStatus()} bernilai
 *       {@link #DISETUJU}, {@code aktif} di-set {@code true}. Jadi selama dokumen masih berstatus
 *       disetujui, nilai {@code false} yang tersimpan di kolom akan <b>dipulihkan</b> menjadi
 *       {@code true} pada pembacaan berikutnya.</li>
 *   <li>Dua cabang <b>berikutnya</b> memaksa {@code false}: (a) bila disposisinya sendiri sudah
 *       tidak aktif, dan (b) bila alur berhenti di simpul yang ditandai sebagai titik penolakan
 *       ({@code getPenolakanAdaDiSini()}). Karena keduanya dievaluasi <i>setelah</i> cabang
 *       pertama, {@code false} selalu menang dalam satu pemanggilan.</li>
 * </ol>
 *
 * <p><b>Kesimpulan:</b> sifat "satu arah" hanya berlaku untuk dokumen yang <b>belum</b> berstatus
 * {@link #DISETUJU}. Untuk dokumen semacam itu, begitu {@code aktif} pernah dipaksa {@code false}
 * (mis. disposisi sempat dinonaktifkan), tidak ada satu pun cabang di method ini yang bisa
 * mengembalikannya ke {@code true} — bahkan setelah penyebabnya hilang — karena nilai {@code false}
 * sudah ikut tersimpan ke kolom oleh flush Hibernate, dan {@code aktif == null ? true} tidak lagi
 * berlaku (field-nya bukan {@code null} lagi, tapi {@code Boolean.FALSE}). Satu-satunya jalan
 * kembali adalah setter manual {@link #setAktif(Boolean)} dari layar
 * {@code PenggantianKasKecilAction} — dan layar itu pun hanya menampilkan checkbox yang bisa
 * diubah ketika dokumen <i>belum</i> disetujui dan disposisinya masih aktif. Praktisnya: untuk
 * dokumen yang alurnya berhenti di penolakan, {@code aktif} bersifat permanen {@code false}.</p>
 *
 * <h2>Status persetujuan: tiga sumber yang mudah tertukar</h2>
 *
 * <p>Kolom {@code status} <b>bukan</b> sumber kebenaran. {@link #getStatus()} menurunkan nilainya
 * dari {@link #getDisetujuiOleh()} — yang sendirinya diturunkan dari
 * {@link DisposisiSop#getDisposisiSetuju()}. Akibatnya {@link #setStatus(String)} yang
 * membersihkan penyetuju saat dokumen ditolak <b>tidak awet</b>: pembacaan berikutnya akan
 * menghitung ulang dari disposisi. Perlakukan {@link DisposisiSop} sebagai satu-satunya sumber
 * kebenaran; ketiga konstanta {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK} hanyalah
 * label tampilan/filter.</p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 *
 * <ul>
 *   <li><b>Audit dasar (deklarasi ulang wajib):</b> {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas dokumen:</b> {@link #getKode()}, {@link #getKodeUnik()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #getTahun()}, {@link #getBulan()},
 *       {@link #getNomorSuratAlurKeuangan()}.</li>
 *   <li><b>Nilai uang (turunan dari kas kecil):</b> {@link #getNilai()}, {@link #getSaldo()}.</li>
 *   <li><b>Alur persetujuan:</b> {@link #getDisposisiSop()}, {@link #getStatus()},
 *       {@link #getAktif()}, {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 *       {@link #getTanggalPembuatan()}, {@link #getTanggalPersetujuan()},
 *       {@link #getTanggalPersetujuanManual()}.</li>
 *   <li><b>Relasi hilir:</b> {@link #getKasKecil()}, {@link #getSatuanKerja()},
 *       {@link #getDaftarPengajuanTransfer()}, {@link #getPostingHistory()},
 *       {@link #getTanggalTransaksi()}.</li>
 *   <li><b>Tidak ada method statis/utilitas query</b> di kelas ini. Semua pencarian dan
 *       kriteria posting berada di lapisan action ({@code PenggantianKasKecilAction},
 *       {@code PersetujuanPenggantianKasKecilAction}, {@code PostingPenggantianKasKecilAction},
 *       {@code PenggantianKasKecilApiHelper}).</li>
 * </ul>
 *
 * <h2>Catatan lain yang tidak terlihat dari nama method</h2>
 *
 * <ul>
 *   <li>{@link #toString()} mengembalikan <b>field</b> {@link #nama} apa adanya, bukan
 *       {@link #getNama()}. Untuk baris yang kolom {@code nama}-nya kosong, {@code toString()}
 *       bisa mengembalikan {@code null} sementara {@link #getNama()} mengembalikan nama kas
 *       kecilnya. Komponen ZK yang memakai {@code toString()} bisa menampilkan kosong.</li>
 *   <li>{@link #setOleh(String)}, {@link #setOlehId(String)}, dan
 *       {@link #setDisposisiSop(DisposisiSop)} <b>diam-diam mengabaikan</b> nilai kosong/null —
 *       artinya relasi/atribut yang sudah terisi <b>tidak bisa dikosongkan lagi</b> lewat setter.</li>
 *   <li>Kelas ini {@code @Audited} dan {@code dynamicInsert/dynamicUpdate}. Kombinasi getter
 *       yang menghitung nilai baru setiap pemanggilan (mis. {@link #getTanggalPembuatan()} yang
 *       mengembalikan {@code new Date()} saat kosong) dengan Envers berpotensi menghasilkan baris
 *       revisi audit "palsu" pada setiap flush.</li>
 * </ul>
 *
 * @see KasKecil
 * @see DaftarPengajuanTransfer
 * @see PostingHistory
 * @see DisposisiSop
 * @see DataSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "penggantian_kas_kecil")
public class PenggantianKasKecil extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama tabel, kolom {@code id} (IDENTITY). Dideklarasikan ulang karena
	 * {@link ais.database.model.GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini (kolom audit {@code oleh}). */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini (kolom audit {@code oleh_id}). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> —
	 * nilai lama dipertahankan. Jadi kolom ini tidak bisa dikosongkan kembali lewat setter,
	 * dan pemuatan baris yang kolomnya {@code NULL} tidak akan menghapus nilai yang sudah ada
	 * pada instance yang dipakai ulang.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai kosong/{@code null} diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-{@code UPDATE},
	 * lalu meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} untuk mengisi jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) dari konteks pengguna yang aktif.
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja dibiarkan menempel di baris yang sama
	 * seperti pada kode aslinya (hasil generator) supaya diff tetap minimal; nilai awalnya adalah
	 * waktu server saat instance dibuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Umumnya diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance baru karena
	 *         field-nya diinisialisasi dengan waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Label status awal: dokumen masih menunggu keputusan. Juga menjadi nilai balik default
	 * {@link #getStatus()} bila kolom {@code status} kosong.
	 */
	public static final String PENGAJUAN = "Pengajuan";
	/**
	 * Label status disetujui. Perhatikan ejaannya <b>tanpa huruf "i" di akhir nama konstanta</b>
	 * ({@code DISETUJU}) walau isinya {@code "Disetujui"} — mudah salah ketik saat mencari.
	 */
	public static final String DISETUJU = "Disetujui";
	/** Label status ditolak; disetel {@link #getStatus()} bila alur SOP berhenti di simpul penolakan. */
	public static final String DITOLAK = "Ditolak";

	/**
	 * Representasi teks dokumen, dipakai komponen ZK (combobox, listbox, label otomatis).
	 *
	 * <p><b>Kuirk:</b> membaca <b>field</b> {@link #nama} langsung, bukan {@link #getNama()}.
	 * Untuk baris yang kolom {@code nama}-nya {@code NULL}, method ini mengembalikan {@code null}
	 * (bukan nama kas kecil seperti {@link #getNama()}), sehingga tampilan bisa kosong.</p>
	 *
	 * @return isi kolom {@code nama} apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Kode/nomor agenda dokumen penggantian (kolom {@code kode}). */
	private String kode;
	/** Nama/judul dokumen; bila kosong, {@link #getNama()} memakai nama {@link KasKecil}-nya. */
	private String nama;
	/** Keterangan bebas dari pengaju (kolom {@code keterangan}). */
	private String keterangan;
	/** Pengajuan kas kecil yang hendak diganti dananya (kolom {@code kas_kecil}). */
	private KasKecil kasKecil;
	/** Nominal penggantian; disalin ulang dari {@link KasKecil#getNilai()} oleh {@link #getNilai()}. */
	private Double nilai;
	/** Saldo kas kecil terkait; disalin ulang dari {@link KasKecil#getSaldo()} oleh {@link #getSaldo()}. */
	private Double saldo;
	/** Flag aktif; lihat pembahasan pola "satu arah" pada Javadoc kelas dan {@link #getAktif()}. */
	private Boolean aktif;
	/** Pembuat dokumen; ditimpa dari simpul awal disposisi oleh {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Penyetuju dokumen; ditimpa dari simpul persetujuan disposisi oleh {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Waktu persetujuan; ditimpa dari disposisi oleh {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Waktu pembuatan; ditimpa dari disposisi oleh {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Label status; nilai sebenarnya dihitung ulang {@link #getStatus()} dari disposisi. */
	private String status;
	/** Satuan kerja pemilik dana; disalin ulang dari kas kecil oleh {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/** Simpul alur SOP yang sedang berjalan untuk dokumen ini (kolom {@code disposisi_sop}). */
	private DisposisiSop disposisiSop;
	/** Jejak posting jurnal; terisi berarti dokumen sudah diposting (kolom {@code posting_history}). */
	private PostingHistory postingHistory;
	/** Bulan periode dokumen (1-12); diisi bulan berjalan oleh {@link #getBulan()} bila kosong. */
	private Integer bulan;
	/** Konfigurasi penomoran surat alur keuangan untuk dokumen penggantian kas kecil. */
	private NomorSuratAlurKeuangan nomorSuratAlurKeuangan;
	/** Tahun periode dokumen; diisi tahun berjalan oleh {@link #getTahun()} bila kosong. */
	private Integer tahun;
	/** Baris antrian transfer bank yang dibuat dari dokumen ini (kolom {@code daftar_pengajuan_transfer}). */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;
	/** Tanggal transaksi untuk keperluan jurnal; diturunkan {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi;

	/**
	 * Konstruktor kosong wajib untuk Hibernate dan untuk pembuatan dokumen baru dari layar
	 * {@code PenggantianKasKecilAction} ({@code tambahBaru()} memanggil
	 * {@code init(new PenggantianKasKecil())}).
	 */
	public PenggantianKasKecil() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * (strategi {@code IDENTITY}).</p>
	 *
	 * @return id baris, atau {@code null} untuk dokumen yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya dipanggil Hibernate saat memuat/menyimpan baris.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode/nomor agenda dokumen, sudah dipangkas spasi.
	 *
	 * @return kode yang sudah di-{@code trim()}, atau {@code null} bila kosong/hanya spasi
	 */
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menyetel kode/nomor agenda dokumen (apa adanya, tanpa pemangkasan).
	 *
	 * @param kode kode dokumen
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama/judul dokumen dengan mekanisme cadangan berlapis.
	 *
	 * <p>Urutan: bila kolom {@code nama} terisi, kembalikan hasil {@code trim()}-nya; bila
	 * kosong, pakai nama {@link KasKecil} yang ditunjuk; bila kas kecilnya pun tidak ada,
	 * kembalikan string kosong.</p>
	 *
	 * <p>Berbeda dengan kebanyakan getter di kelas ini, method ini <b>tidak</b> menulis balik ke
	 * field — nilai cadangan hanya dikembalikan, kolom {@code nama} tetap {@code NULL} di
	 * database. Karena itu {@link #toString()} (yang membaca field langsung) dan method ini bisa
	 * memberi hasil berbeda untuk baris yang sama.</p>
	 *
	 * @return nama dokumen, nama kas kecil, atau string kosong — tidak pernah {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? (getKasKecil() == null ? "" : getKasKecil().getNama()) : this.nama.trim();
	}

	/**
	 * Menyetel nama/judul dokumen.
	 *
	 * @param nama nama dokumen
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas yang diisi pengaju.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas dokumen.
	 *
	 * @param keterangan keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menghitung ulang dan mengembalikan flag "aktif" dokumen — <b>bukan getter polos</b>.
	 *
	 * <p>Dipanggil dari renderer grid {@code PenggantianKasKecilAction} (untuk menampilkan
	 * checkbox atau label "Ya"/"Tidak") <i>dan</i> oleh Hibernate saat flush, sehingga nilai
	 * hasil perhitungan di sini <b>ikut tersimpan permanen</b> ke kolom {@code aktif} dan
	 * tercatat di audit Envers.</p>
	 *
	 * <p>Urutan evaluasinya:</p>
	 * <ol>
	 *   <li>Bila {@link #getStatus()} sama dengan {@link #DISETUJU} → {@code aktif = true}.
	 *       Ini satu-satunya jalur yang bisa <b>memulihkan</b> nilai {@code false} yang sudah
	 *       tersimpan. Perhatikan bahwa {@link #getStatus()} sendiri bukan getter polos: ia ikut
	 *       menulis {@link #status} dan (lewat {@link #getDisetujuiOleh()}) {@link #disetujuiOleh}.</li>
	 *   <li>Menyimpan hasil {@link #getDisposisiSop()} ke field {@link #disposisiSop} (resolusi
	 *       proxy lazy sekaligus efek samping penulisan field).</li>
	 *   <li>Bila disposisi ada dan {@link DisposisiSop#getAktif()} bernilai {@code false} →
	 *       {@code aktif = false}.</li>
	 *   <li>Bila alur berhenti di simpul akhir yang ditandai sebagai titik penolakan
	 *       ({@code disposisiEnd.getAlurSop().getPenolakanAdaDiSini()}) → {@code aktif = false}.</li>
	 * </ol>
	 *
	 * <p><b>Sifat "satu arah" (hasil verifikasi):</b> karena kedua cabang pemaksa {@code false}
	 * dievaluasi setelah cabang pemulih, {@code false} selalu menang dalam satu pemanggilan. Untuk
	 * dokumen yang <b>belum</b> berstatus {@link #DISETUJU}, nilai {@code false} yang sudah
	 * tersimpan tidak akan pernah kembali {@code true} lewat method ini — fallback
	 * {@code aktif == null ? true} hanya menolong selama kolomnya masih benar-benar {@code NULL}.
	 * Pemulihan hanya mungkin lewat {@link #setAktif(Boolean)} dari layar, atau bila dokumen
	 * akhirnya disetujui.</p>
	 *
	 * <p><b>Risiko {@code NullPointerException}:</b> baris 1 memanggil {@code getStatus().equals(...)}
	 * — aman karena {@link #getStatus()} tidak pernah mengembalikan {@code null}. Namun
	 * {@code disposisiSop.getAktif()} dan {@code getPenolakanAdaDiSini()} di-unbox ke
	 * {@code boolean}; keduanya bisa {@code null} secara tipe walau implementasi saat ini
	 * mengembalikan default non-{@code null}.</p>
	 *
	 * @return {@code true} bila dokumen masih dianggap aktif/berlaku; {@code false} bila
	 *         disposisinya nonaktif atau alurnya berakhir di penolakan. Tidak pernah {@code null}.
	 */
	public Boolean getAktif() {

		if (getStatus().equals(PenggantianKasKecil.DISETUJU)) {
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
	 * Menyetel flag aktif secara manual.
	 *
	 * <p>Satu-satunya jalan mengembalikan dokumen ke {@code true} setelah
	 * {@link #getAktif()} memaksanya {@code false}. Dipakai renderer grid
	 * {@code PenggantianKasKecilAction} lewat checkbox — dan checkbox itu hanya bisa diubah bila
	 * dokumen belum disetujui serta disposisinya masih aktif. Nilai yang disetel di sini tetap
	 * bisa ditimpa lagi oleh {@link #getAktif()} pada pembacaan berikutnya.</p>
	 *
	 * @param aktif flag aktif baru; {@code null} berarti "kembali ke default {@code true}"
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nominal penggantian — <b>selalu disalin ulang dari kas kecil</b>.
	 *
	 * <p>Bila {@link #getKasKecil()} ada, field {@link #nilai} <b>ditimpa</b> dengan
	 * {@link KasKecil#getNilai()}. Karena getter ini juga dipanggil Hibernate saat flush, kolom
	 * {@code nilai} pada tabel ini praktis hanya cermin nilai kas kecil dan tidak dapat
	 * dipertahankan berbeda: menyetelnya lewat {@link #setNilai(Double)} akan tergerus pada
	 * pembacaan berikutnya selama relasi kas kecil masih terpasang.</p>
	 *
	 * <p>Nilai ini yang dipakai {@link DaftarPengajuanTransfer#getNominal()} sebagai nominal
	 * transfer, dan disaring kriteria posting ({@code nilai} harus bukan nol) sebelum dijurnal.</p>
	 *
	 * @return nominal penggantian; {@code 0.0} bila belum ada nilai — tidak pernah {@code null}
	 */
	public Double getNilai() {
		if (getKasKecil() != null) {
			nilai = getKasKecil().getNilai();
		}
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel nominal penggantian.
	 *
	 * <p>Lihat peringatan di {@link #getNilai()}: nilai manual akan tertimpa selama relasi
	 * {@link #getKasKecil()} tidak {@code null}.</p>
	 *
	 * @param nilai nominal penggantian
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Menyetel pembuat dokumen.
	 *
	 * <p>Lihat {@link #getDibuatOleh()}: nilai ini akan tertimpa oleh pengaju pada simpul awal
	 * disposisi bila disposisinya sudah terbentuk.</p>
	 *
	 * @param dibuatOleh pengguna pembuat dokumen
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pembuat dokumen, <b>diturunkan dari alur SOP bila tersedia</b>.
	 *
	 * <p>Dua langkah: (1) resolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} — hasilnya ditulis balik ke
	 * field; (2) bila disposisi punya simpul awal dengan pengaju, field {@link #dibuatOleh}
	 * <b>ditimpa</b> dengan pengaju tersebut. Jadi kolom {@code dibuat_oleh} pada akhirnya
	 * mengikuti alur SOP, bukan isian form.</p>
	 *
	 * <p><b>Catatan kinerja/konsistensi:</b> {@link #getDisposisiSop()} dipanggil tiga kali
	 * berturut-turut di sini, dan setiap pemanggilan menjalankan {@code check()} lagi. Kelas
	 * {@link DisposisiSop} sendiri mendokumentasikan bahwa pemanggilan berulang semacam ini
	 * pernah memberi hasil tidak konsisten (non-{@code null} lalu {@code null}) saat dijalankan
	 * dari thread latar dengan siklus-hidup session sendiri, sehingga berisiko NPE. Pola aman
	 * yang sudah diterapkan di {@code DisposisiSop} adalah mengambil sekali ke variabel lokal.</p>
	 *
	 * @return pengguna pembuat dokumen, boleh {@code null}
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
	 * <p>Juga dipanggil {@link #setStatus(String)} dengan argumen {@code null} saat status
	 * disetel {@link #DITOLAK}. Lihat {@link #getDisetujuiOleh()}: pengosongan itu tidak awet
	 * bila disposisi masih memiliki simpul persetujuan.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju, boleh {@code null}
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan penyetuju dokumen, <b>disinkronkan penuh dengan alur SOP</b>.
	 *
	 * <p>Method ini adalah sumber kebenaran de-facto untuk "sudah disetujui atau belum":
	 * {@link #getStatus()} menyimpulkan {@link #DISETUJU} semata-mata dari hasil {@code != null}
	 * di sini, dan kriteria posting ({@code PostingPenggantianKasKecilAction}) menyaring dengan
	 * {@code Restrictions.isNotNull("disetujuiOleh")}.</p>
	 *
	 * <p>Alurnya: resolusi proxy lazy → bila simpul persetujuan disposisi punya pengaju, field
	 * ditimpa dengan pengaju itu → bila simpul persetujuan tidak ada (atau pengajunya kosong)
	 * sementara disposisinya sendiri ada, field <b>dikosongkan</b> menjadi {@code null}. Cabang
	 * terakhir inilah yang membatalkan efek {@link #setDisetujuiOleh(Tbmuser)} manual maupun
	 * pengosongan dari {@link #setStatus(String)}.</p>
	 *
	 * <p>Karena hasilnya ditulis balik ke field dan Hibernate memakai akses properti, perubahan
	 * ini ikut tersimpan ke kolom {@code disetujui_oleh}.</p>
	 *
	 * @return pengguna penyetuju bila alur sudah melewati simpul persetujuan; {@code null} bila
	 *         belum disetujui
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
		return disetujuiOleh;
	}

	/**
	 * Menyetel waktu persetujuan.
	 *
	 * <p>Dipanggil juga oleh {@link #setStatus(String)} dengan {@code null} saat status
	 * {@link #DITOLAK}. Lihat {@link #getTanggalPersetujuan()} — pengosongan bisa dihitung
	 * ulang dari disposisi.</p>
	 *
	 * @param tanggalPersetujuan waktu persetujuan, boleh {@code null}
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan waktu persetujuan dokumen, diturunkan dari alur SOP dengan kemungkinan
	 * penimpaan manual.
	 *
	 * <p>Tiga lapis:</p>
	 * <ol>
	 *   <li><b>Di dalam {@code try}</b> — bila simpul persetujuan disposisi punya pengaju,
	 *       ambil {@code waktu} simpul itu; bila simpul persetujuan tidak ada/pengajunya kosong,
	 *       kosongkan. Blok ini dibungkus {@code try/catch} karena
	 *       {@link #getDisposisiSop()} dapat mengembalikan instance canonical/berbagi
	 *       ({@code AuditTimestampInterceptor}) yang proxy-nya terikat ke {@code Session} lain
	 *       yang sudah tertutup; komentar aslinya dipertahankan di badan method. Bila terjadi
	 *       {@code LazyInitializationException}, kejadiannya dicatat
	 *       {@code ErrorAuditUtil.record(...)} (penanda {@code auto-audit(empty-catch)}) dan
	 *       nilai cadangan dipertahankan alih-alih membuat getter ini crash.</li>
	 *   <li><b>Di luar {@code try}</b> — {@code disetujuiOleh = check(disetujuiOleh)} menulis
	 *       langsung ke field {@link #disetujuiOleh} <i>tanpa</i> melewati
	 *       {@link #getDisetujuiOleh()}. Ini efek samping lintas-properti: getter tanggal
	 *       memodifikasi properti penyetuju.</li>
	 *   <li><b>Penimpaan manual</b> — bila {@link #getTanggalPersetujuanManual()} terisi dan
	 *       penyetujunya ada, tanggal manual itu yang menang.</li>
	 * </ol>
	 *
	 * <p>Nilai ini dipakai kriteria posting sebagai filter rentang tanggal
	 * ({@code date(this_.tanggal_persetujuan) between ...}) dan dipinjam
	 * {@link KasKecil#getTanggalPersetujuan()} bila kas kecil belum punya tanggalnya sendiri.</p>
	 *
	 * @return waktu persetujuan, atau {@code null} bila dokumen belum disetujui
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
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/PenggantianKasKecil.java:getTanggalPersetujuan-lazy");
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
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
	 * Mengembalikan waktu pembuatan dokumen, diturunkan dari simpul awal alur SOP.
	 *
	 * <p>Bila disposisi punya simpul awal dengan pengaju, field {@link #tanggalPembuatan}
	 * <b>ditimpa</b> dengan {@code waktu} simpul itu. Bila hasil akhirnya masih {@code null},
	 * method mengembalikan {@code new Date()}.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> nilai cadangan {@code new Date()} tidak ditulis ke
	 * field, tetapi karena Hibernate membaca properti lewat getter, nilai "sekarang" itulah yang
	 * ikut dibandingkan saat <i>dirty checking</i> dan disimpan saat flush. Untuk baris yang
	 * kolomnya {@code NULL} dan tanpa disposisi, setiap pemanggilan menghasilkan waktu berbeda
	 * sehingga entity bisa selalu terlihat "kotor" — pada entity {@code @Audited} seperti ini,
	 * itu berpotensi menimbulkan baris revisi Envers yang tidak mencerminkan perubahan nyata.</p>
	 *
	 * @return waktu pembuatan dokumen; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
		}

		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Menghitung ulang dan mengembalikan label status dokumen — <b>bukan getter polos</b>.
	 *
	 * <p>Dipakai luas: renderer grid, penyaring combobox status di
	 * {@code PenggantianKasKecilAction}, penentu boleh/tidaknya dokumen diedit atau dihapus, dan
	 * cabang pertama {@link #getAktif()}.</p>
	 *
	 * <p>Urutannya:</p>
	 * <ol>
	 *   <li>Bila {@link #getDisetujuiOleh()} tidak {@code null} → {@link #DISETUJU}.</li>
	 *   <li>Sebaliknya, bila kolom {@code status} masih menyimpan {@link #DISETUJU} padahal
	 *       penyetujunya sudah hilang → <b>diturunkan kembali</b> ke {@link #PENGAJUAN}. Inilah
	 *       yang membuat persetujuan bisa "batal sendiri" ketika disposisi diubah.</li>
	 *   <li>Menyimpan hasil {@link #getDisposisiSop()} ke field {@link #disposisiSop}.</li>
	 *   <li>Bila alur berhenti di simpul yang ditandai titik penolakan → {@link #DITOLAK}
	 *       (menimpa kedua hasil sebelumnya).</li>
	 * </ol>
	 *
	 * <p>Nilai hasil perhitungan ditulis ke field, jadi ikut tersimpan ke kolom {@code status}
	 * saat flush. Kolom itu karena itu bukan sumber kebenaran, melainkan cache dari keadaan
	 * {@link DisposisiSop}.</p>
	 *
	 * @return salah satu dari {@link #PENGAJUAN}, {@link #DISETUJU}, atau {@link #DITOLAK};
	 *         tidak pernah {@code null} maupun string kosong
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
	 * Menyetel label status dokumen, dengan pembersihan otomatis saat penolakan.
	 *
	 * <p>Bila {@code status} yang disetel adalah {@link #DITOLAK}, method ini juga mengosongkan
	 * penyetuju dan tanggal persetujuan lewat {@link #setDisetujuiOleh(Tbmuser)} dan
	 * {@link #setTanggalPersetujuan(Date)}.</p>
	 *
	 * <p><b>Efek pembersihan itu tidak awet.</b> {@link #getDisetujuiOleh()} dan
	 * {@link #getTanggalPersetujuan()} menghitung ulang nilainya dari {@link DisposisiSop} pada
	 * pembacaan berikutnya; selama simpul persetujuan disposisi masih ada beserta pengajunya,
	 * kedua field akan terisi lagi dan {@link #getStatus()} pun kembali melaporkan
	 * {@link #DISETUJU}. Menolak dokumen secara benar harus dilakukan lewat alur SOP, bukan
	 * lewat setter ini.</p>
	 *
	 * @param status label status baru; perlakuan khusus hanya untuk {@link #DITOLAK}
	 */
	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	/**
	 * Mengembalikan pengajuan kas kecil yang dananya hendak diganti (kolom {@code kas_kecil}).
	 *
	 * <p>Ini relasi inti kelas ini: {@link #getNama()}, {@link #getNilai()}, {@link #getSaldo()},
	 * dan {@link #getSatuanKerja()} semuanya mengambil datanya dari sini. Kriteria posting pun
	 * mensyaratkan relasi ini tidak {@code null}.</p>
	 *
	 * <p>Berbeda dengan getter relasi lain di kelas ini, method ini <b>tidak</b> memanggil
	 * {@code check()} — dipetakan dengan {@code @Fetch(FetchMode.SELECT)} tanpa
	 * {@code fetch = LAZY} eksplisit, jadi diandalkan sudah termuat. Bila instance ini diakses
	 * di luar session aslinya, memanggil method pada hasilnya bisa memicu
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return pengajuan kas kecil terkait, boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_kecil", nullable = true)
	public KasKecil getKasKecil() {
		return kasKecil;
	}

	/**
	 * Menyetel pengajuan kas kecil yang dananya diganti.
	 *
	 * @param kasKecil pengajuan kas kecil terkait
	 */
	public void setKasKecil(KasKecil kasKecil) {
		this.kasKecil = kasKecil;
	}

	/**
	 * Mengembalikan satuan kerja pemilik dana — <b>disalin ulang dari kas kecil</b>.
	 *
	 * <p>Bila {@link #getKasKecil()} ada, field {@link #satuanKerja} ditimpa dengan satuan kerja
	 * kas kecil tersebut; bila tidak, hanya proxy lazy field sendiri yang di-resolusi lewat
	 * {@code check()}. Nilai hasil ikut tersimpan ke kolom {@code satuan_kerja} saat flush.</p>
	 *
	 * <p>Kolom ini dipakai penyaringan hak akses per satuan kerja pada layar posting
	 * ({@code Restrictions.in("satuanKerja", satuanKerjas)} dengan {@code NULL} berarti
	 * "berlaku untuk semua").</p>
	 *
	 * @return satuan kerja pemilik dana, boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getKasKecil() != null) {
			satuanKerja = getKasKecil().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik dana.
	 *
	 * <p>Akan tertimpa oleh {@link #getSatuanKerja()} selama relasi kas kecil terpasang.</p>
	 *
	 * @param satuanKerja satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kode gabungan unik dokumen; selalu disusun ulang oleh {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Tanggal persetujuan isian operator, menimpa tanggal dari disposisi bila diisi. */
	private Date tanggalPersetujuanManual;

	/**
	 * Menyusun ulang dan mengembalikan kode unik dokumen — <b>dihitung setiap pemanggilan</b>.
	 *
	 * <p>Formatnya {@code <kode> + "_" + <id disposisi>}, atau {@code <kode> + "_" + <id baris>}
	 * bila dokumen belum punya disposisi. Field {@link #kodeUnik} ditimpa dengan hasilnya,
	 * sehingga nilai baru ikut tersimpan ke kolom saat flush.</p>
	 *
	 * <p><b>Kuirk yang berisiko:</b></p>
	 * <ul>
	 *   <li>Kolomnya {@code @Column(unique = true)} tetapi nilainya <b>berubah</b> begitu
	 *       disposisi terpasang (dari {@code "..._<id>"} menjadi {@code "..._<idDisposisi>"}).
	 *       Nilai kolom yang seharusnya stabil karena itu bisa berpindah, dan tabrakan constraint
	 *       unik mungkin terjadi bila dua dokumen menghasilkan gabungan yang sama.</li>
	 *   <li>{@link #getKode()} bisa mengembalikan {@code null}; konkatenasi string tetap berjalan
	 *       dan menghasilkan teks yang diawali {@code "null"} — bukan {@code NullPointerException},
	 *       tapi juga bukan kode yang bermakna.</li>
	 *   <li>Untuk dokumen baru yang belum disimpan ({@link #getId()} masih {@code null}) dan belum
	 *       berdisposisi, hasilnya berakhiran {@code "_null"} — dan semua dokumen baru semacam itu
	 *       menghasilkan nilai yang sama.</li>
	 * </ul>
	 *
	 * @return kode unik gabungan; tidak pernah {@code null} tapi bisa berisi teks {@code "null"}
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kode unik dokumen.
	 *
	 * <p>Praktis tidak berguna: {@link #getKodeUnik()} selalu menghitung ulang nilainya, sehingga
	 * apa pun yang disetel di sini akan tergantikan pada pembacaan berikutnya. Method ini ada
	 * terutama agar Hibernate dapat memuat nilai kolom.</p>
	 *
	 * @param kodeUnik kode unik
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan simpul alur SOP yang sedang berjalan untuk dokumen ini.
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop#getDisposisiSop()} — inilah yang membuat
	 * dokumen ini bisa diproses seragam oleh mesin disposisi
	 * ({@code TampilanAlurSopAction}, {@code Sop}).</p>
	 *
	 * <p>Memanggil {@link ais.database.model.GeneralValueObject#check(Object)} untuk meresolusi
	 * proxy lazy dan <b>menulis hasilnya balik ke field</b>. Karena {@code check()} bisa berujung
	 * membaca ulang entity lewat session baru, method ini tidak murah bila dipanggil berulang —
	 * dan di kelas ini ia memang dipanggil berkali-kali di dalam satu method (lihat
	 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()}).</p>
	 *
	 * @return disposisi SOP dokumen, atau {@code null} bila alur belum dimulai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP dokumen — <b>tidak bisa dipakai untuk mengosongkan relasi</b>.
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop#setDisposisiSop(DisposisiSop)}. Penjaga di
	 * awal method langsung {@code return} bila argumennya {@code null} atau belum punya id, jadi
	 * memanggil {@code setDisposisiSop(null)} adalah <b>no-op senyap</b>. Konsekuensinya, disposisi
	 * yang sudah terpasang pada sebuah instance tidak dapat dilepas lewat setter ini — termasuk
	 * saat Hibernate memuat ulang baris yang kolom {@code disposisi_sop}-nya sudah {@code NULL}
	 * ke instance yang dipakai ulang.</p>
	 *
	 * <p><b>Kode mati:</b> ekspresi ternary di baris berikutnya menguji lagi
	 * {@code disposisiSop == null || disposisiSop.getId() == null} — kondisi yang mustahil benar
	 * karena sudah disaring penjaga di atas. Ternary itu karena itu selalu bernilai
	 * {@code disposisiSop}, sehingga badan method setara dengan penugasan biasa. Bentuk
	 * berbelitnya dipertahankan apa adanya (tidak disederhanakan) agar diff tetap murni Javadoc.</p>
	 *
	 * @param disposisiSop disposisi SOP baru; diabaikan bila {@code null} atau belum tersimpan
	 *                     (id masih {@code null})
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Mengembalikan saldo kas kecil terkait — <b>selalu disalin ulang dari kas kecil</b>.
	 *
	 * <p>Sama seperti {@link #getNilai()}: bila {@link #getKasKecil()} ada, field {@link #saldo}
	 * ditimpa dengan {@link KasKecil#getSaldo()}, dan nilai itu ikut tersimpan ke kolom
	 * {@code saldo} saat flush. Kolom ini karena itu hanya cermin, bukan data mandiri.</p>
	 *
	 * @return saldo kas kecil; {@code 0.0} bila belum ada nilai — tidak pernah {@code null}
	 */
	public Double getSaldo() {
		if (getKasKecil() != null) {
			saldo = getKasKecil().getSaldo();
		}
		return saldo == null ? 0.0 : saldo;
	}

	/**
	 * Menyetel saldo kas kecil terkait.
	 *
	 * <p>Akan tertimpa oleh {@link #getSaldo()} selama relasi kas kecil terpasang.</p>
	 *
	 * @param saldo saldo kas kecil
	 */
	public void setSaldo(Double saldo) {
		this.saldo = saldo;
	}

	/**
	 * Mengembalikan jejak posting jurnal dokumen ini (kolom {@code posting_history}).
	 *
	 * <p>Terisi berarti dokumen <b>sudah diposting</b> menjadi jurnal; {@code null} berarti
	 * belum. Layar {@code PostingPenggantianKasKecilAction} memakai persis ini sebagai filter
	 * "belum tampil"/"telah tampil" ({@code Restrictions.isNull/isNotNull("postingHistory")}),
	 * dan pembatalan posting mengosongkannya kembali.</p>
	 *
	 * @return jejak posting, atau {@code null} bila dokumen belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel jejak posting jurnal dokumen ini.
	 *
	 * @param postingHistory jejak posting; {@code null} untuk menandai dokumen belum/batal
	 *                       diposting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan tahun periode dokumen, <b>mengisi tahun berjalan bila masih kosong</b>.
	 *
	 * <p>Bersama {@link #getBulan()}, kolom ini dipakai sebagai penanda/penyaring periode
	 * penomoran surat.</p>
	 *
	 * <p><b>Peringatan integritas data:</b> pengisian default memakai
	 * {@code WaktuUtil.getCalendar()} — yaitu <b>tahun saat getter dipanggil</b>, bukan tahun
	 * dokumen dibuat. Karena hasilnya ditulis ke field dan Hibernate membaca properti lewat
	 * getter, membuka dokumen lama yang kolom {@code tahun}-nya {@code NULL} akan menstempelnya
	 * dengan tahun sekarang dan menyimpannya. Nilai yang tampak "sudah ada" belum tentu
	 * mencerminkan periode dokumen sebenarnya.</p>
	 *
	 * @return tahun periode dokumen; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun periode dokumen.
	 *
	 * @param tahun tahun periode
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan konfigurasi penomoran surat alur keuangan untuk dokumen penggantian kas
	 * kecil, <b>mengisi default statis bila masih kosong</b>.
	 *
	 * <p>Bila field kosong, diisi konstanta bersama
	 * {@link NomorSuratAlurKeuangan#PENGGANTIAN_KAS_KECIL_DATA}; bila sudah terisi, hanya
	 * di-resolusi proxy lazy-nya lewat {@code check()}. Kedua cabang menulis balik ke field,
	 * sehingga default itu ikut tersimpan ke kolom {@code nomor_surat_alur_keuangan}.</p>
	 *
	 * <p><b>Catatan:</b> {@link NomorSuratAlurKeuangan#PENGGANTIAN_KAS_KECIL_DATA} adalah field
	 * statis yang dimuat sekali dari database saat inisialisasi. Objek statis itu berasal dari
	 * session yang mungkin sudah lama tertutup, jadi ia adalah instance <i>detached</i> yang
	 * dibagi ke seluruh aplikasi — perlakukan sebagai hanya-baca.</p>
	 *
	 * @return konfigurasi penomoran surat; bisa {@code null} bila data master belum tersedia
	 *         saat inisialisasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_keuangan", nullable = true)
	public NomorSuratAlurKeuangan getNomorSuratAlurKeuangan() {
		if (nomorSuratAlurKeuangan == null) {
			nomorSuratAlurKeuangan = NomorSuratAlurKeuangan.PENGGANTIAN_KAS_KECIL_DATA;
		} else {
			nomorSuratAlurKeuangan = check(nomorSuratAlurKeuangan);
		}
		return nomorSuratAlurKeuangan;
	}

	/**
	 * Menyetel konfigurasi penomoran surat alur keuangan.
	 *
	 * @param nomorSuratAlurKeuangan konfigurasi penomoran surat
	 */
	public void setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan nomorSuratAlurKeuangan) {
		this.nomorSuratAlurKeuangan = nomorSuratAlurKeuangan;
	}

	/**
	 * Mengembalikan bulan periode dokumen (1-12), <b>mengisi bulan berjalan bila masih kosong</b>.
	 *
	 * <p>Perhatikan {@code + 1}: {@link Calendar#MONTH} berbasis nol, jadi nilai yang disimpan
	 * sudah dalam konvensi manusia (Januari = 1).</p>
	 *
	 * <p><b>Peringatan integritas data sama seperti {@link #getTahun()}:</b> dokumen lama yang
	 * kolom {@code bulan}-nya {@code NULL} akan distempel bulan sekarang begitu dibaca, dan
	 * nilai itu ikut tersimpan.</p>
	 *
	 * @return bulan periode dokumen dalam rentang 1-12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan periode dokumen.
	 *
	 * @param bulan bulan periode (konvensi 1-12, bukan berbasis nol)
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan baris antrian transfer bank yang dibuat dari dokumen ini.
	 *
	 * <p>Diisi {@link DaftarPengajuanTransfer#simpanPenggantianKasKecil} setelah dokumen
	 * disetujui — method itu mencari baris transfer yang sudah menunjuk dokumen ini, membuat baru
	 * bila belum ada, lalu menyimpannya dua arah. Relasi ini <b>tidak dibuat</b> untuk kas kecil
	 * yang bersifat penutupan ({@code getMerupakanPenutupanKasKecil()}).</p>
	 *
	 * <p>Kriteria posting mensyaratkan {@code daftarPengajuanTransfer.prosesTransfer} tidak
	 * {@code null} — artinya dokumen hanya boleh dijurnal setelah transfernya benar-benar
	 * diproses.</p>
	 *
	 * @return baris antrian transfer, atau {@code null} bila belum dijadwalkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menyetel baris antrian transfer bank untuk dokumen ini.
	 *
	 * @param daftarPengajuanTransfer baris antrian transfer
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Menurunkan dan mengembalikan tanggal transaksi yang dipakai sebagai tanggal jurnal.
	 *
	 * <p>Tiga jalur, dievaluasi berurutan:</p>
	 * <ol>
	 *   <li><b>Jalur transitori</b> — bila transfernya ditandai transitori dan proses
	 *       transitorinya ada, pakai tanggal pembuatan proses transitori itu.</li>
	 *   <li><b>Jalur transfer biasa</b> — bila ada proses transfer, pakai tanggal realisasinya;
	 *       bila belum direalisasikan, pakai tanggal pembuatan proses transfer.</li>
	 *   <li><b>Cadangan</b> — pakai {@link #getTanggalPembuatan()} dokumen ini (yang sendirinya
	 *       bisa mengembalikan waktu sekarang bila kosong).</li>
	 * </ol>
	 *
	 * <p>Hasilnya ditulis balik ke field {@link #tanggalTransaksi} sehingga ikut tersimpan ke
	 * kolom {@code tanggal_transaksi}. Perhatikan bahwa cabang pertama memakai {@code getter}
	 * sekali lalu <b>field</b> {@code daftarPengajuanTransfer} langsung untuk pemanggilan
	 * berikutnya — aman karena getternya memang tidak melakukan resolusi apa pun.</p>
	 *
	 * <p><b>Risiko unboxing:</b> {@code daftarPengajuanTransfer.getTransitori()} di-unbox ke
	 * {@code boolean}; bila suatu saat method itu bisa mengembalikan {@code null}, baris ini akan
	 * melempar {@code NullPointerException}.</p>
	 *
	 * @return tanggal transaksi untuk jurnal; tidak pernah {@code null} karena jalur cadangan
	 *         selalu memberi nilai
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
	 * Menyetel tanggal transaksi.
	 *
	 * <p>Akan selalu tertimpa oleh {@link #getTanggalTransaksi()} pada pembacaan berikutnya —
	 * setter ini praktis hanya melayani pemuatan oleh Hibernate.</p>
	 *
	 * @param tanggalTransaksi tanggal transaksi
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}


	/**
	 * Mengembalikan tanggal persetujuan yang diisi operator secara manual.
	 *
	 * <p>Getter polos (tanpa efek samping). Nilainya dipakai {@link #getTanggalPersetujuan()}
	 * sebagai <b>penimpa terakhir</b>: bila terisi dan penyetujunya ada, tanggal manual inilah
	 * yang menang atas tanggal yang diturunkan dari disposisi. Berguna untuk membetulkan tanggal
	 * dokumen yang persetujuan formalnya terjadi di luar sistem.</p>
	 *
	 * @return tanggal persetujuan manual, atau {@code null} bila tidak diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/**
	 * Menyetel tanggal persetujuan manual (penimpa tanggal dari disposisi).
	 *
	 * @param tanggalPersetujuanManual tanggal persetujuan manual; {@code null} untuk kembali
	 *                                 mengikuti tanggal dari alur SOP
	 */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}
}
