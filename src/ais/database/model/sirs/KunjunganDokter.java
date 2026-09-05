package ais.database.model.sirs;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Catatan <b>satu kali kunjungan/visite tenaga medis</b> terhadap satu episode diagnosa pasien
 * (tabel {@code sirs.kunjungan_dokter}) pada modul SIRS AIS.
 *
 * <h2>Posisi dalam alur pelayanan</h2>
 * <p>Entity ini adalah <b>ujung terjauh</b> dari rantai pelayanan pasien SIRS. Rantai lengkapnya —
 * sebagaimana dapat diverifikasi dari relasi antar entity, bukan dari penamaannya saja — adalah:</p>
 * <pre>
 * BookingRegistrasi   (janji temu / booking sebelum datang, opsional)
 *        &darr;  booking_registrasi.pendaftaran  &harr;  pendaftaran.booking_registrasi
 * Pendaftaran         (registrasi resmi kunjungan: rawat jalan / rawat inap / UGD)
 *        &darr;  diagnosa_penyakit.pendaftaran
 * DiagnosaPenyakit    (episode diagnosa untuk pendaftaran tersebut)
 *        &darr;  kunjungan_dokter.diagnosa_penyakit
 * KunjunganDokter     (SATU kali visite oleh SATU tenaga medis)  &larr; kelas ini
 * </pre>
 * <p><b>Perhatikan:</b> {@code KunjunganDokter} <b>tidak</b> memiliki relasi langsung ke
 * {@link Pendaftaran} maupun {@link Pasien}. Keduanya hanya dapat dicapai melalui
 * {@link #getDiagnosaPenyakit()} — misalnya
 * {@code kunjunganDokter.getDiagnosaPenyakit().getPendaftaran()} dan
 * {@code kunjunganDokter.getDiagnosaPenyakit().getPasien()}, seperti yang dilakukan
 * {@code ais.action.master.sirs.util.RawatInapCalculationProcessor}. Konsekuensinya:
 * {@link #getDiagnosaPenyakit()} yang bernilai {@code null} membuat kunjungan menjadi
 * <b>yatim</b> — tidak terhubung ke pasien, pendaftaran, maupun lokasi mana pun — dan kolomnya
 * memang dipetakan {@code nullable = true}. Kode yang menelusuri rantai ini wajib memeriksa
 * {@code null} lebih dulu.</p>
 *
 * <h2>Siapa yang membuat entity ini</h2>
 * <p>Satu-satunya pembuat baris {@code KunjunganDokter} di basis kode adalah kelas bersarang
 * {@code KunjunganDokterAction} di dalam
 * {@code ais.action.master.sirs.DiagnosaPenyakitRawatInapAction} — sebuah tab pada layar diagnosa
 * penyakit rawat inap. Alurnya: petugas memilih tenaga medis, lalu sistem membuat satu baris baru
 * dengan {@link #setTindakan(Tindakan)} berisi {@code ConstantValues.KUNJUNGAN_RUTIN},
 * {@link #setWaktu(Date)} berisi waktu saat itu, {@link #setKode(String)} dari
 * {@code Common.generateCode(KunjunganDokter.class, 10)}, dan {@link #setKeterangan(String)}
 * berisi string kosong. Setelah {@code session.save(...)}, dipanggil
 * {@code RawatInapCalculationProcessor.checkKunjunganDokter(kunjunganDokter)} yang menyusun atau
 * memperbarui baris {@code DetailTransaksiLayanan} untuk menagihkan biaya kunjungan.</p>
 *
 * <p>Selain itu, entity ini dibaca oleh {@code DashboardSirsKomprehensif} (grid ringkasan
 * kunjungan tenaga medis) dan dirujuk sebagai {@code @ManyToOne} dari
 * {@link DetailTransaksiLayanan#getKunjunganDokter()}. Jadi entity ini <b>bukan</b> entity tidur —
 * ada pembuat, pembaca, dan perujuk yang nyata.</p>
 *
 * <h2>Bagaimana biaya kunjungan sebenarnya dihitung</h2>
 * <p>Penting untuk tidak salah menduga dari nama field: {@link #getBiaya()} <b>tidak</b> dipakai
 * dalam perhitungan tagihan. {@code RawatInapCalculationProcessor.checkKunjunganDokter(...)}
 * mencari {@link DetailTransaksiLayanan} yang cocok berdasarkan kombinasi
 * {@code kunjunganDokter} + {@code tindakan}; bila belum ada dibuat baru; lalu nominalnya
 * ditetapkan oleh {@code CommonPendaftaranUtil.setDetailBiaya(detail, kelasPerawatan, session)}
 * berdasarkan {@link #getTindakan()} dan kelas perawatan pada
 * {@code diagnosaPenyakit.getPendaftaran().getKelasPerawatan()} (dengan
 * {@code ConstantValues.kelasNormal} sebagai cadangan bila kelas perawatan kosong). Field
 * {@link #biaya} pada kelas ini tidak pernah dibaca maupun ditulis oleh kode mana pun — lihat
 * peringatan pada {@link #getBiaya()}.</p>
 *
 * <h2>Pemetaan, audit, dan pola AIS</h2>
 * <p>Dipetakan ke skema {@code sirs} tabel {@code kunjungan_dokter}, dengan {@code dynamicInsert}
 * dan {@code dynamicUpdate} aktif dan {@link org.hibernate.envers.Audited} sehingga setiap
 * perubahan direkam Envers. Trio field audit bayangan ({@link #oleh}, {@link #olehId},
 * {@link #tanggal_dirubah}) beserta hook {@link #onUpdate()} adalah keharusan teknis pola AIS,
 * bukan duplikasi yang perlu dibersihkan.</p>
 *
 * <p>Getter relasi mengikuti pola AIS yang biasa: relasi {@code LAZY} melewati
 * {@code check(...)} untuk meresolusi proxy sebelum dikembalikan, sedangkan relasi eager tidak.
 * Lihat catatan pada {@link #getDiagnosaPenyakit()} mengenai perbedaan ini.</p>
 *
 * @see DiagnosaPenyakit
 * @see Dokter
 * @see Tindakan
 * @see DetailTransaksiLayanan
 * @see Pendaftaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "kunjungan_dokter")
public class KunjunganDokter extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entity ini.
	 * <p>Nilainya kebetulan sama dengan {@link Dokter} dan {@link BookingRegistrasi} karena
	 * ketiganya dihasilkan dari cetakan hbm2java yang sama. Hal ini tidak menimbulkan masalah:
	 * {@code serialVersionUID} hanya relevan antar versi kelas yang sama.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama teknis (auto-increment kolom {@code id}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * ID pengguna terakhir yang mengubah baris ini — bagian dari trio field audit bayangan.
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * @return ID pengguna yang tercatat sebagai pengubah terakhir baris kunjungan ini, atau
	 *         {@code null} bila belum pernah diisi.
	 * @see #setOlehId(String)
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b>.
	 * <p>Bila {@code olehId} {@code null} atau hanya berisi spasi, method langsung kembali tanpa
	 * mengubah apa pun. Ini disengaja agar jejak pengubah terakhir tidak terhapus oleh pemanggil
	 * yang tidak memiliki konteks pengguna login (proses batch, impor, atau job terjadwal).</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null} atau string kosong diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (pendamping {@link #olehId}).
	 * Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Representasi teks kunjungan berupa <b>kode kunjungan saja</b>.
	 *
	 * <p>Berbeda dari kebanyakan entity AIS yang memakai format {@code "kode - nama"} bawaan
	 * {@link ais.database.model.GeneralValueObject#toString()}, method ini hanya mengembalikan
	 * {@link #kode} karena entity kunjungan tidak memiliki "nama" yang bermakna.</p>
	 *
	 * <p><b>Dua hal yang perlu diperhatikan.</b> Pertama, method ini membaca field {@link #kode}
	 * secara langsung, bukan lewat {@link #getKode()} — perbedaan yang tidak berpengaruh di sini
	 * karena getter tersebut sepele, tetapi menjadikannya tidak konsisten dengan pola di entity
	 * lain. Kedua, method ini dapat mengembalikan {@code null} bila kode belum diisi; ini
	 * melanggar konvensi umum {@code toString()} yang diharapkan selalu mengembalikan string, dan
	 * dapat menghasilkan {@link NullPointerException} pada pemanggil yang langsung merangkainya
	 * (mis. {@code kunjungan.toString().trim()}). Pada alur normal kode selalu diisi oleh
	 * {@code KunjunganDokterAction} sebelum penyimpanan, sehingga risiko ini hanya muncul pada
	 * instance yang belum tersimpan.</p>
	 *
	 * @return kode kunjungan, atau {@code null} bila belum diisi.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b> dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau string kosong diabaikan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang tercatat sebagai pengubah terakhir, atau {@code null}.
	 * @see #setOleh(String)
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@link javax.persistence.PreUpdate} yang mendelegasikan pengisian jejak audit ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum
	 * Hibernate menerbitkan UPDATE untuk baris ini.
	 *
	 * <p>Karena hanya {@code @PreUpdate} (tidak ada {@code @PrePersist}), pengisian otomatis
	 * terjadi pada perubahan saja; pada penyimpanan pertama nilai {@link #tanggal_dirubah}
	 * berasal dari inisialisasi {@code new Date()} pada deklarasi field.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan field {@code tanggal_dirubah} sengaja
	 * berada pada satu baris fisik — hasil alat penyapu yang menyisipkan pasangan hook+field ke
	 * ratusan entity sekaligus. Jangan memisahkannya tanpa alasan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 * <p>Pada alur normal nilai ini ditimpa otomatis oleh {@link #onUpdate()} saat flush, sehingga
	 * pemanggilan manual jarang diperlukan. Berbeda dari setter {@code oleh}/{@code olehId},
	 * setter ini menerima {@code null} apa adanya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini ({@link TemporalType#TIMESTAMP}, menyimpan
	 *         tanggal dan jam). Jangan mengacaukannya dengan {@link #getWaktu()} yang mencatat
	 *         <i>kapan kunjungan terjadi</i>; field ini murni metadata audit.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode unik kunjungan, kolom {@code kode}. Lihat {@link #getKode()}. */
	private String kode;

	/** Tenaga medis yang melakukan kunjungan. Lihat {@link #getDokter()}. */
	private Dokter dokter;

	/** Waktu kunjungan terjadi. Lihat {@link #getWaktu()}. */
	private Date waktu;

	/**
	 * Episode diagnosa yang menaungi kunjungan ini — satu-satunya jalan menuju pasien,
	 * pendaftaran, dan lokasi. Lihat {@link #getDiagnosaPenyakit()}.
	 */
	private DiagnosaPenyakit diagnosaPenyakit;

	/** Nominal biaya kunjungan — field tidur, lihat peringatan pada {@link #getBiaya()}. */
	private Double biaya;

	/**
	 * Jenis tindakan/kunjungan yang menentukan tarif penagihan. Lihat {@link #getTindakan()}.
	 */
	private Tindakan tindakan;

	/** Catatan bebas hasil kunjungan. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi,
	 * sekaligus konstruktor yang dipakai {@code KunjunganDokterAction} ketika petugas menambah
	 * satu baris kunjungan baru.
	 *
	 * <p>Seluruh field dibiarkan {@code null} kecuali {@link #tanggal_dirubah} yang diisi
	 * {@code new Date()}. Instance hasil konstruktor ini belum siap disimpan: minimal
	 * {@link #setKode(String)} harus diisi karena kolomnya {@code nullable = false}.</p>
	 */
	public KunjunganDokter() {
	}

	/**
	 * @return kunci utama teknis baris kunjungan ini, atau {@code null} bila belum tersimpan.
	 *         Dihasilkan basis data ({@link javax.persistence.GenerationType#IDENTITY}) dan
	 *         dipetakan {@code insertable = false} sehingga tidak pernah ikut pada INSERT.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama teknis.
	 * <p>Diisi Hibernate setelah INSERT; jangan mengubahnya manual pada instance terkelola karena
	 * {@link ais.database.model.GeneralValueObject#equals(Object)} dibangun di atas {@code id}.</p>
	 *
	 * @param id kunci utama teknis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode kunjungan (kolom {@code kode}, {@code nullable = false}, maksimal 100 karakter).
	 *         Nilainya dihasilkan {@code Common.generateCode(KunjunganDokter.class, 10)} oleh
	 *         {@code KunjunganDokterAction} saat baris kunjungan dibuat.
	 *         <p><b>Perhatikan:</b> berbeda dari {@link Pendaftaran#getKode()} dan
	 *         {@link BookingRegistrasi#getKode()}, kolom ini <b>tidak</b> dipetakan
	 *         {@code unique = true}. Jadi keunikan kode kunjungan tidak ditegakkan basis data dan
	 *         sepenuhnya bergantung pada kualitas {@code Common.generateCode(...)}; kode kembar
	 *         secara teknis dapat tersimpan.</p>
	 */
	@Column(name = "kode", nullable = false, length = 100)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode kunjungan.
	 * <p>Wajib diisi sebelum penyimpanan pertama karena kolomnya {@code nullable = false};
	 * kegagalannya baru muncul saat flush, bukan saat setter dipanggil.</p>
	 *
	 * @param kode kode kunjungan.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return catatan bebas hasil kunjungan (kolom {@code keterangan}), atau {@code null}.
	 *         {@code KunjunganDokterAction} mengisinya dengan string kosong saat membuat baris
	 *         baru, dan {@code RawatInapCalculationProcessor} menyisipkan nilainya ke keterangan
	 *         {@link DetailTransaksiLayanan} dengan format
	 *         {@code "Kunjungan dokter <tindakan>, Ket: <keterangan>"} — sehingga isi field ini
	 *         ikut tercetak pada rincian tagihan pasien.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas hasil kunjungan.
	 * <p>Perlu diingat bahwa isinya bukan sekadar catatan internal: nilainya disalin ke keterangan
	 * baris tagihan {@link DetailTransaksiLayanan} oleh
	 * {@code RawatInapCalculationProcessor.checkKunjunganDokter(...)}, jadi teks di sini terbaca
	 * oleh pasien pada rincian biaya.</p>
	 *
	 * @param keterangan catatan bebas; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tenaga medis yang melakukan kunjungan ini, setelah meresolusi proxy lazy-nya.
	 *
	 * <p>Relasi dipetakan {@code @ManyToOne} ke kolom {@code kunjungan_dokter.dokter} dengan
	 * {@link FetchType#LAZY}, sehingga field-nya berisi proxy Hibernate ketika entity dimuat.
	 * Pemanggilan {@code check(dokter)} yang diwarisi dari
	 * {@link ais.database.model.GeneralValueObject} meresolusi proxy tersebut menjadi instance
	 * nyata sebelum dikembalikan, sehingga getter ini tetap aman dipanggil pada entity yang sudah
	 * <i>detached</i> dari {@link org.hibernate.Session} yang memuatnya — situasi yang lazim di
	 * AIS karena entity sering hidup lintas request ZK dan cache in-memory.</p>
	 *
	 * <p><b>Ini getter yang menulis balik ke field.</b> Hasil {@code check(...)} ditugaskan kembali
	 * ke {@link #dokter}, jadi setelah pemanggilan pertama field tidak lagi berisi proxy melainkan
	 * instance kanonik. Efek sampingnya terbatas pada identitas object (bukan pada nilai kolom),
	 * sehingga tidak menimbulkan UPDATE yang tidak diinginkan.</p>
	 *
	 * <p>Kolomnya {@code nullable = true}: sebuah baris kunjungan boleh tidak menyebut tenaga
	 * medis. Pemanggil seperti {@code KunjunganDokterRenderer} pada
	 * {@code DiagnosaPenyakitRawatInapAction} harus siap menerima {@code null}.</p>
	 *
	 * @return tenaga medis pelaksana kunjungan, atau {@code null} bila tidak diisi.
	 * @see Dokter
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dokter", nullable = true)
	public Dokter getDokter() {
		dokter = check(dokter);
		return dokter;
	}

	/**
	 * Menyetel tenaga medis pelaksana kunjungan.
	 * <p>Karena relasi memakai {@code cascade = { PERSIST, MERGE }}, menyetel instance
	 * {@link Dokter} yang belum tersimpan akan ikut menyimpannya saat kunjungan ini di-persist.
	 * Pada alur normal yang disetel selalu tenaga medis yang sudah ada di master.</p>
	 *
	 * @param dokter tenaga medis pelaksana; boleh {@code null}.
	 */
	public void setDokter(Dokter dokter) {
		this.dokter = dokter;
	}

	/**
	 * @return waktu kunjungan benar-benar dilakukan, dipetakan
	 *         {@link TemporalType#TIMESTAMP} sehingga menyimpan tanggal <i>dan</i> jam — penting
	 *         karena dalam satu hari rawat inap dapat terjadi beberapa visite.
	 *         <p>Nilai ini disalin menjadi tanggal baris tagihan
	 *         ({@code detailTransaksiLayanan.setTanggal(kunjunganDokter.getWaktu())}) oleh
	 *         {@code RawatInapCalculationProcessor}, sehingga waktu di sini menentukan periode
	 *         pembebanan biaya kunjungan. Jangan dikacaukan dengan {@link #getTanggal_dirubah()}
	 *         yang merupakan metadata audit.</p>
	 *         <p>Getter ini tidak beranotasi {@link Column @Column}, sehingga dipetakan ke kolom
	 *         {@code waktu} berdasarkan konvensi penamaan properti.</p>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu;
	}

	/**
	 * Menyetel waktu kunjungan.
	 * <p>{@code KunjunganDokterAction} mengisinya dengan {@code new Date()} pada saat baris dibuat.
	 * Karena nilai ini menjadi tanggal baris tagihan, mengubahnya setelah tagihan terbentuk tidak
	 * otomatis memperbarui {@link DetailTransaksiLayanan} — perbaruan hanya terjadi bila
	 * {@code RawatInapCalculationProcessor.checkKunjunganDokter(...)} dipanggil ulang.</p>
	 *
	 * @param waktu waktu kunjungan dilakukan.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan episode diagnosa yang menaungi kunjungan ini.
	 *
	 * <p><b>Ini relasi terpenting pada kelas ini</b> — satu-satunya jalan dari sebuah kunjungan
	 * menuju pasien, pendaftaran, lokasi, dan kelas perawatan. Rantai yang dipakai
	 * {@code RawatInapCalculationProcessor} adalah:</p>
	 * <ul>
	 *   <li>{@code getDiagnosaPenyakit().getLokasi()} &rarr; lokasi baris tagihan;</li>
	 *   <li>{@code getDiagnosaPenyakit().getPasien()} &rarr; pasien yang ditagih;</li>
	 *   <li>{@code getDiagnosaPenyakit().getPendaftaran()} &rarr; registrasi kunjungan, dan dari
	 *   sana {@code getKelasPerawatan()} yang menentukan tarif.</li>
	 * </ul>
	 *
	 * <p><b>Perbedaan dari getter relasi lain di kelas ini:</b> method ini <b>tidak</b> memanggil
	 * {@code check(...)}. Itu bukan kelalaian melainkan konsekuensi pemetaannya — relasi ini tidak
	 * menyatakan {@code fetch = FetchType.LAZY}, sehingga memakai default {@code @ManyToOne} yaitu
	 * {@link FetchType#EAGER}, diperkuat {@link Fetch @Fetch}{@code (}{@link FetchMode#SELECT}{@code )}
	 * yang meminta Hibernate memuatnya lewat SELECT terpisah alih-alih JOIN. Karena sudah termuat
	 * penuh saat entity dibaca, tidak ada proxy yang perlu diresolusi. Sebaliknya, ini berarti
	 * setiap pembacaan {@code KunjunganDokter} dari basis data <b>selalu</b> menyeret satu SELECT
	 * tambahan untuk {@link DiagnosaPenyakit} — pertimbangkan hal ini pada grid yang menampilkan
	 * banyak baris kunjungan sekaligus (mis. {@code DashboardSirsKomprehensif}), karena polanya
	 * adalah N+1 query.</p>
	 *
	 * <p><b>Kunjungan yatim mungkin terjadi.</b> Kolom {@code diagnosa_penyakit} dipetakan
	 * {@code nullable = true}, sehingga baris kunjungan tanpa episode diagnosa dapat tersimpan.
	 * Baris seperti itu tidak dapat ditagihkan —
	 * {@code RawatInapCalculationProcessor.checkKunjunganDokter(...)} akan melempar
	 * {@link NullPointerException} saat menelusuri {@code getDiagnosaPenyakit().getLokasi()} bila
	 * nilainya {@code null}. Pada alur normal hal ini tidak terjadi karena satu-satunya pembuat
	 * baris kunjungan, {@code KunjunganDokterAction}, selalu memanggil
	 * {@link #setDiagnosaPenyakit(DiagnosaPenyakit)} sebelum {@code session.save(...)}. Namun
	 * jalur lain (impor data, skrip SQL, atau penghapusan episode diagnosa tanpa menghapus
	 * kunjungannya) tetap dapat menghasilkan baris yatim, jadi kode baru yang membaca entity ini
	 * wajib memeriksa {@code null}.</p>
	 *
	 * @return episode diagnosa penaung kunjungan ini, atau {@code null} bila kunjungan yatim.
	 * @see DiagnosaPenyakit
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "diagnosa_penyakit", nullable = true)
	public DiagnosaPenyakit getDiagnosaPenyakit() {
		return diagnosaPenyakit;
	}

	/**
	 * Menyetel episode diagnosa penaung kunjungan ini.
	 * <p>Wajib diisi sebelum penyimpanan agar kunjungan dapat ditagihkan dan tertaut ke pasien —
	 * lihat penjelasan lengkap pada {@link #getDiagnosaPenyakit()}. Relasi memakai
	 * {@code cascade = { PERSIST, MERGE }}, sehingga episode diagnosa yang belum tersimpan akan
	 * ikut disimpan.</p>
	 *
	 * @param diagnosaPenyakit episode diagnosa penaung; secara teknis boleh {@code null}, tetapi
	 *                         menghasilkan kunjungan yatim.
	 */
	public void setDiagnosaPenyakit(DiagnosaPenyakit diagnosaPenyakit) {
		this.diagnosaPenyakit = diagnosaPenyakit;
	}

	/**
	 * Menyetel nominal biaya kunjungan. Lihat peringatan pada {@link #getBiaya()}: field ini tidur
	 * dan tidak berpengaruh pada tagihan.
	 *
	 * @param biaya nominal biaya; boleh {@code null}.
	 */
	public void setBiaya(Double biaya) {
		this.biaya = biaya;
	}

	/**
	 * @return nominal biaya kunjungan, atau {@code null}.
	 *
	 * <p><b>Peringatan: field tidur (dormant) — jangan dipakai sebagai sumber biaya.</b>
	 * Penelusuran seluruh basis kode tidak menemukan satu pun pemanggil {@code getBiaya()} maupun
	 * {@link #setBiaya(Double)} pada entity ini, baik di layar pembuatnya
	 * ({@code DiagnosaPenyakitRawatInapAction.KunjunganDokterAction}), di prosesor
	 * perhitungannya ({@code RawatInapCalculationProcessor}), maupun di dasbornya
	 * ({@code DashboardSirsKomprehensif}). Kolomnya tetap dipetakan dan ikut dibaca/ditulis
	 * Hibernate, tetapi nilainya selalu {@code null} pada data yang dihasilkan alur normal.</p>
	 *
	 * <p>Biaya kunjungan yang sesungguhnya ditentukan di
	 * {@link DetailTransaksiLayanan}: {@code RawatInapCalculationProcessor.checkKunjunganDokter(...)}
	 * mencari/membuat baris {@code DetailTransaksiLayanan} untuk kombinasi kunjungan + tindakan,
	 * lalu menyerahkan penetapan nominalnya ke
	 * {@code CommonPendaftaranUtil.setDetailBiaya(detail, kelasPerawatan, session)} yang menghitung
	 * dari {@link #getTindakan()} dan kelas perawatan pada pendaftaran terkait. Menuliskan nilai ke
	 * field ini <b>tidak</b> akan mengubah tagihan pasien sama sekali, dan berisiko menyesatkan
	 * pembaca laporan yang mengira kolom ini otoritatif.</p>
	 */
	public Double getBiaya() {
		return biaya;
	}

	/**
	 * Mengembalikan jenis tindakan/kunjungan yang menentukan tarif, setelah meresolusi proxy
	 * lazy-nya lewat {@code check(...)} — pola yang sama dengan {@link #getDokter()}.
	 *
	 * <p><b>Peran dalam penagihan.</b> Nilai inilah, bukan {@link #getBiaya()}, yang menentukan
	 * berapa kunjungan ditagihkan. {@code RawatInapCalculationProcessor.checkKunjunganDokter(...)}
	 * memakainya untuk dua hal sekaligus: (1) sebagai bagian kunci pencocokan baris tagihan —
	 * baris {@link DetailTransaksiLayanan} dicari berdasarkan kombinasi {@code kunjunganDokter} +
	 * {@code tindakan}; dan (2) sebagai dasar penetapan nominal lewat
	 * {@code CommonPendaftaranUtil.setDetailBiaya(...)} yang mencocokkan tindakan dengan kelas
	 * perawatan pasien.</p>
	 *
	 * <p>Karena tindakan adalah bagian dari kunci pencocokan, <b>mengubah tindakan pada kunjungan
	 * yang sudah ditagihkan akan menghasilkan baris tagihan baru</b> alih-alih memperbarui baris
	 * lama — baris tagihan lama (dengan tindakan sebelumnya) tetap tertinggal dan pasien berpotensi
	 * ditagih dua kali untuk satu kunjungan yang sama. Prosesor tersebut juga langsung berhenti
	 * tanpa melakukan apa pun bila tindakan bernilai {@code null}, sehingga kunjungan tanpa
	 * tindakan tidak akan pernah menghasilkan tagihan.</p>
	 *
	 * <p>Pada alur normal {@code KunjunganDokterAction} selalu mengisinya dengan
	 * {@code ConstantValues.KUNJUNGAN_RUTIN}. Kolomnya {@code nullable = true}, jadi nilai
	 * {@code null} tetap mungkin muncul pada data dari jalur lain.</p>
	 *
	 * @return jenis tindakan/kunjungan, atau {@code null} bila tidak diisi.
	 * @see Tindakan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tindakan", nullable = true)
	public Tindakan getTindakan() {
		tindakan = check(tindakan);
		return tindakan;
	}

	/**
	 * Menyetel jenis tindakan/kunjungan.
	 * <p>Perhatikan konsekuensi penagihannya yang dijelaskan pada {@link #getTindakan()}: karena
	 * tindakan menjadi bagian kunci pencocokan baris tagihan, menggantinya setelah tagihan
	 * terbentuk dapat menyisakan baris tagihan lama. Relasi memakai
	 * {@code cascade = { PERSIST, MERGE }}.</p>
	 *
	 * @param tindakan jenis tindakan/kunjungan; boleh {@code null}, tetapi kunjungan tanpa tindakan
	 *                 tidak akan pernah ditagihkan.
	 */
	public void setTindakan(Tindakan tindakan) {
		this.tindakan = tindakan;
	}

}
