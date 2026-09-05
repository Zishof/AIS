package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.employ.Pendidikan;

/**
 * <b>Registrasi kunjungan pasien</b> — dokumen induk pelayanan pada modul SIRS AIS
 * (tabel {@code sirs.pendaftaran}).
 *
 * <h2>Peran: dokumen paling sentral di klaster SIRS</h2>
 * <p>Satu baris {@code Pendaftaran} mewakili satu episode kunjungan pasien ke rumah sakit, dan
 * menjadi titik temu hampir semua data pelayanan: identitas pasien, jenis perawatan, poli dan
 * dokter yang dituju, jadwal serta nomor antrian, penjamin, kelas perawatan, penempatan tempat
 * tidur untuk rawat inap, data penjamin/pendaftar, sampai status kepulangan. Karena itu entity ini
 * adalah <b>berkas terbesar</b> di paket {@code ais.database.model.sirs}.</p>
 *
 * <h2>Rantai alur yang diverifikasi dari kode</h2>
 * <pre>
 * BookingRegistrasi   janji temu sebelum pasien datang (OPSIONAL)
 *        &harr;  relasi dua arah: booking_registrasi.pendaftaran &amp; pendaftaran.booking_registrasi
 * Pendaftaran         &larr; kelas ini: registrasi resmi saat pasien datang
 *        &darr;  diagnosa_penyakit.pendaftaran
 * DiagnosaPenyakit    episode diagnosa
 *        &darr;  kunjungan_dokter.diagnosa_penyakit
 * KunjunganDokter     satu kali visite tenaga medis
 * </pre>
 * <p>Ada tiga layar yang membuat baris {@code Pendaftaran}, masing-masing untuk satu nilai
 * {@link #getJenis()}: {@code PendaftaranRawatJalanAction} ({@link #RAWAT_JALAN}),
 * {@code PendaftaranRawatInapAction} ({@link #RAWAT_INAP}), dan
 * {@code PendaftaranRawatUgdAction} ({@link #RAWAT_UGD}). Alur UGD berbeda dari dua lainnya karena
 * secara eksplisit menyetel {@code setBookingRegistrasi(null)} — kegawatdaruratan tidak mengenal
 * janji temu — dan sebagai gantinya menautkan {@link #getTransaksiUgd()}.</p>
 *
 * <h2>Kelompok field</h2>
 * <p>Field pada entity ini dapat dibaca sebagai lima kelompok:</p>
 * <ol>
 *   <li><b>Identitas dokumen</b> — {@link #getKode()}, {@link #getIndex()},
 *   {@link #getTanggalPendaftaran()}, {@link #getTbmuser()} (petugas yang mendaftarkan),
 *   {@link #getLokasi()}, {@link #getShift()}.</li>
 *   <li><b>Pelayanan yang dituju</b> — {@link #getJenis()}, {@link #getPoly()},
 *   {@link #getSubpoly()}, {@link #getDokter()}, {@link #getJadwalDokter()},
 *   {@link #getNomorAntrian()}, {@link #getDilayaniTanggal()}, {@link #getBagian()}.</li>
 *   <li><b>Pasien dan haknya</b> — {@link #getPasien()}, {@link #getJenisPasien()},
 *   {@link #getUmur()}, {@link #getKomunitass()}, {@link #getAsuransi()},
 *   {@link #getKelasPerawatan()}, {@link #getPakets()}.</li>
 *   <li><b>Khusus rawat inap</b> — {@link #getRuangPerawatan()}, {@link #getKamarPerawatan()},
 *   {@link #getTempatTidur()}, {@link #getPindahKeKelasPerawatan()},
 *   {@link #getTransferDaripendaftaran()}, {@link #getStatusPendaftaran()},
 *   {@link #getTanggalKeluar()}, {@link #getDataPasienKeluar()}, serta blok data penjamin
 *   ({@link #getNamaPenjamin()} dan seterusnya) dan data pendaftar
 *   ({@link #getNamaPendaftar()} dan seterusnya).</li>
 *   <li><b>Keuangan</b> — {@link #getPembayaran()} dan {@link #getLunas()}.</li>
 * </ol>
 *
 * <h2>Pola arsitektur yang perlu diwaspadai di kelas ini</h2>
 * <ul>
 *   <li><b>Getter destruktif atas kolom yang dipetakan</b> — {@link #getJenisPasien()},
 *   {@link #getUmur()}, {@link #getKelasPerawatan()}, {@link #getMerupakanPaket()},
 *   {@link #getLunas()}, {@link #getDilayaniTanggal()}, dan
 *   {@link #getTanggalPendaftaran()} semuanya menulis balik nilai turunan ke field. Karena
 *   Hibernate mengakses entity ini lewat properti, penulisan itu ikut ter-flush ke basis data —
 *   membaca entity ini <b>dapat mengubah data</b>, dan karena entity ini
 *   {@link org.hibernate.envers.Audited}, ikut menghasilkan baris revisi Envers.</li>
 *   <li><b>Setter yang menjalankan query</b> — {@link #setPasienKomunitas(Pasien)} membuka
 *   session Hibernate thread-local.</li>
 *   <li><b>Field audit bayangan</b> — {@link #oleh}, {@link #olehId}, {@link #tanggal_dirubah}
 *   beserta hook {@link #onUpdate()} adalah keharusan teknis pola AIS, bukan duplikasi.</li>
 *   <li><b>Method bukan-accessor</b> — {@link #ambilAlamat()} adalah satu-satunya method non
 *   getter/setter di kelas ini; ia meratakan alamat pasien menjadi satu baris dan menelan seluruh
 *   exception.</li>
 * </ul>
 *
 * <p>Dipetakan ke skema {@code sirs} tabel {@code pendaftaran} dengan {@code dynamicInsert} dan
 * {@code dynamicUpdate} aktif, serta {@link org.hibernate.envers.Audited}.</p>
 *
 * @see BookingRegistrasi
 * @see JadwalDokter
 * @see Pasien
 * @see DataPasienKeluar
 * @see KunjunganDokter
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pendaftaran")
public class Pendaftaran extends GeneralValueObject {

	/**
	 * Status pendaftaran <b>"Masih Dalam Perawatan"</b> — nilai awal {@link #statusPendaftaran}
	 * untuk setiap pendaftaran baru.
	 *
	 * <p>Perhatikan bahwa nama konstantanya ({@code TERDAFTAR}) berbeda jauh dari nilainya
	 * ({@code "Masih Dalam Perawatan"}); saat menulis query native atau membandingkan isi kolom,
	 * yang berlaku adalah nilainya. Status ini dipulihkan kembali oleh
	 * {@code DataPasienKeluarAction} bila data pasien keluar dibatalkan, dan oleh
	 * {@code PindahTempatTidurRawatInapAction} pada pendaftaran asal setelah pemindahan
	 * dibatalkan.</p>
	 *
	 * <p>Nilai ini juga dipakai sebagai pilihan filter pada laporan rawat inap
	 * ({@code LaporanRanapPerRuangan}, {@code NewUiLaporanSirsController}).</p>
	 *
	 * @see #getStatusPendaftaran()
	 */
	public static final String TERDAFTAR = "Masih Dalam Perawatan";

	/**
	 * Status pendaftaran <b>"Pulang"</b> — pasien telah keluar dari perawatan dalam keadaan hidup.
	 * <p>Disetel oleh {@code DataPasienKeluarAction} ketika data pasien keluar disimpan dengan
	 * status pulang biasa. Perhatikan sekali lagi bahwa nama konstantanya {@code KELUAR} sementara
	 * nilainya {@code "Pulang"}.</p>
	 *
	 * @see #getStatusPendaftaran()
	 * @see #getDataPasienKeluar()
	 */
	public static final String KELUAR = "Pulang";

	/**
	 * Status pendaftaran <b>"Pindah Kelas/Bed"</b> — pendaftaran ini ditutup karena pasien
	 * dipindahkan ke kelas perawatan atau tempat tidur lain.
	 *
	 * <p>Disetel oleh {@code PindahTempatTidurRawatInapAction} pada pendaftaran <i>asal</i>;
	 * pemindahan menghasilkan pendaftaran baru yang menunjuk pendaftaran lama lewat
	 * {@link #getTransferDaripendaftaran()}. Status ini juga dipakai
	 * {@code ais.common.InitSirs} untuk menyiapkan master status pulang
	 * ({@code ConstantValues.STATUS_PINDAH}).</p>
	 *
	 * @see #getStatusPendaftaran()
	 * @see #getTransferDaripendaftaran()
	 */
	public static final String PINDAH = "Pindah Kelas/Bed";

	/**
	 * Status pendaftaran <b>"Meninggal"</b> — pasien keluar dari perawatan dalam keadaan meninggal.
	 * <p>Disetel oleh {@code DataPasienKeluarAction} dan dipakai
	 * {@code ais.common.InitSirs} untuk menyiapkan {@code ConstantValues.STATUS_MENINGGAL}.</p>
	 *
	 * @see #getStatusPendaftaran()
	 */
	public static final String MENINGGAL = "Meninggal";

	/**
	 * Jenis perawatan <b>"Rawat Jalan"</b> — pasien berobat tanpa menginap.
	 * <p>Dipakai layar {@code PendaftaranRawatJalanAction}. Untuk jenis ini, kelompok field khusus
	 * rawat inap ({@link #getRuangPerawatan()}, {@link #getKamarPerawatan()},
	 * {@link #getTempatTidur()}, data penjamin, dan seterusnya) tidak diisi.</p>
	 *
	 * @see #getJenis()
	 */
	public static final String RAWAT_JALAN = "Rawat Jalan";

	/**
	 * Jenis perawatan <b>"Rawat Inap"</b> — pasien menginap dan menempati tempat tidur.
	 * <p>Dipakai layar {@code PendaftaranRawatInapAction}. Hanya untuk jenis inilah seluruh
	 * kelompok field rawat inap bermakna, termasuk penempatan tempat tidur, data penjamin, status
	 * pendaftaran, dan data pasien keluar.</p>
	 *
	 * @see #getJenis()
	 */
	public static final String RAWAT_INAP = "Rawat Inap";

	/**
	 * Jenis perawatan <b>"UGD"</b> — pelayanan gawat darurat.
	 * <p>Dipakai layar {@code PendaftaranRawatUgdAction}, yang berbeda dari dua layar lain karena
	 * selalu menyetel {@link #setBookingRegistrasi(BookingRegistrasi)} ke {@code null} dan
	 * menautkan {@link #getTransaksiUgd()}.</p>
	 *
	 * <p><b>Perhatikan tabrakan nilai:</b> literal {@code "UGD"} di sini sama persis dengan
	 * literal {@link #SUMBER_PASIEN_UGD}, padahal keduanya menempati kolom berbeda
	 * ({@code jenis} dan {@code sumber_pasien}) dengan makna berbeda. Query yang mencari string
	 * {@code "UGD"} tanpa menyebut kolomnya berpotensi salah sasaran.</p>
	 *
	 * @see #getJenis()
	 */
	public static final String RAWAT_UGD = "UGD";

	/**
	 * Sumber pasien <b>"Poli"</b> — pasien rawat inap berasal dari poliklinik.
	 * <p>Bersama empat konstanta {@code SUMBER_PASIEN_*} lainnya, nilai ini mengisi combobox
	 * "Sumber Pasien" pada {@code PendaftaranRawatInapAction} dan disimpan apa adanya sebagai teks
	 * pada kolom {@code sumber_pasien}.</p>
	 *
	 * @see #getSumberPasien()
	 */
	public static final String SUMBER_PASIEN_POLI = "Poli";

	/**
	 * Sumber pasien <b>"UGD"</b> — pasien rawat inap masuk lewat unit gawat darurat.
	 * <p>Nilainya identik dengan {@link #RAWAT_UGD} meskipun menempati kolom yang berbeda; lihat
	 * peringatan pada konstanta tersebut.</p>
	 *
	 * @see #getSumberPasien()
	 */
	public static final String SUMBER_PASIEN_UGD = "UGD";

	/**
	 * Sumber pasien <b>"Dr. RS"</b> — rujukan dari dokter rumah sakit sendiri.
	 *
	 * @see #getSumberPasien()
	 */
	public static final String SUMBER_PASIEN_DARI_RS = "Dr. RS";

	/**
	 * Sumber pasien <b>"Dr. Tamu"</b> — rujukan dari dokter tamu/mitra.
	 *
	 * @see #getSumberPasien()
	 */
	public static final String SUMBER_PASIEN_DARI_TAMU = "Dr. Tamu";

	/**
	 * Sumber pasien <b>"Luar DKI"</b> — pasien berasal dari luar wilayah DKI.
	 * <p>Nilai ini memperlihatkan bahwa daftar sumber pasien mengandung asumsi geografis khusus
	 * (Jakarta) yang tertanam di kode, bukan konfigurasi. Instalasi di luar wilayah tersebut akan
	 * melihat pilihan ini tetap muncul.</p>
	 *
	 * @see #getSumberPasien()
	 */
	public static final String SUMBER_PASIEN_LUAR_DKI = "Luar DKI";

	/**
	 * Penanda versi serialisasi Java untuk entity ini; nilainya kebetulan sama dengan beberapa
	 * entity SIRS lain karena berasal dari cetakan hbm2java yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama teknis (auto-increment kolom {@code id}). Lihat {@link #getId()}. */
	private Long id;

	/** Nomor urut pendaftaran per lokasi. Lihat {@link #getIndex()}. */
	private Long index;

	/**
	 * ID pengguna terakhir yang mengubah pendaftaran ini — bagian dari trio field audit bayangan.
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * @return ID pengguna yang tercatat sebagai pengubah terakhir pendaftaran ini, atau
	 *         {@code null}. Jangan dikacaukan dengan {@link #getTbmuser()} yang mencatat petugas
	 *         pendaftar, maupun dengan blok {@code *Pendaftar} yang mencatat identitas orang yang
	 *         mengantar pasien.
	 * @see #setOlehId(String)
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b>.
	 * <p>{@code null} dan string berisi spasi saja diabaikan agar jejak pengubah terakhir tidak
	 * terhapus oleh pemanggil tanpa konteks pengguna login.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna terakhir yang mengubah pendaftaran ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/**
	 * Representasi teks pendaftaran dalam format {@code "<kode> <keterangan>"} — dipisah
	 * <b>spasi</b>, bukan tanda hubung seperti kebanyakan entity AIS lain.
	 *
	 * <p>Method ini membaca field {@link #kode} dan {@link #keterangan} secara langsung, bukan
	 * lewat getter-nya. Karena keduanya field skalar, tidak ada risiko
	 * {@code LazyInitializationException} — tetapi keduanya juga boleh {@code null}, sehingga hasil
	 * dapat berupa {@code "null null"} pada instance yang belum diisi. Perlu diperhatikan bahwa
	 * {@link #getKeterangan()} adalah catatan bebas yang diketik petugas, sehingga teks hasil
	 * {@code toString()} ini panjangnya tidak terkendali dan tidak cocok dipakai sebagai label
	 * ringkas; banyak layar karena itu menampilkan {@code getKode()} saja.</p>
	 *
	 * @return teks {@code "<kode> <keterangan>"}; tidak pernah {@code null}.
	 */
	public String toString() {
		return kode + " " + keterangan;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b> dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan.
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
	 * Hibernate menerbitkan UPDATE untuk baris pendaftaran ini.
	 *
	 * <p>Perlu diperhatikan khusus untuk entity ini: karena banyak getter di sini menulis balik
	 * nilai turunan ke field (lihat daftar pada Javadoc kelas), UPDATE dapat terjadi bahkan pada
	 * alur yang secara logika hanya "membaca" pendaftaran — dan setiap UPDATE seperti itu ikut
	 * memperbarui jejak audit lewat hook ini serta menambah satu baris revisi Envers.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan field {@code tanggal_dirubah} sengaja
	 * berada pada satu baris fisik — bentuk yang dihasilkan alat penyapu lintas ratusan entity.
	 * Jangan memisahkannya tanpa alasan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menyetel waktu perubahan terakhir pendaftaran ini; pada alur normal ditimpa otomatis oleh
	 * {@link #onUpdate()} saat flush.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris pendaftaran ({@link TemporalType#TIMESTAMP}). Ini
	 *         metadata audit — jangan dikacaukan dengan {@link #getTanggalPendaftaran()} (kapan
	 *         pasien mendaftar), {@link #getDilayaniTanggal()} (kapan pasien dilayani), maupun
	 *         {@link #getTanggalKeluar()} (kapan pasien pulang).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode unik pendaftaran (format {@code REG-RAJAL-...} dan sejenisnya). Lihat {@link #getKode()}. */
	private String kode;
	/** Catatan bebas petugas. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Pendaftaran asal bila pendaftaran ini hasil pemindahan. Lihat {@link #getTransferDaripendaftaran()}. */
	private Pendaftaran transferDaripendaftaran;

	/** Pasien yang mendaftar. Lihat {@link #getPasien()}. */
	private Pasien pasien;

	/** Bagian/unit organisasi terkait. Lihat {@link #getBagian()}. */
	private Bagian bagian;

	/** Petugas (pengguna aplikasi) yang melakukan pendaftaran. Lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;

	/** Jadwal praktek yang dituju. Lihat {@link #getJadwalDokter()}. */
	private JadwalDokter jadwalDokter;

	/** Waktu pasien mendaftar, diinisialisasi {@code new Date()}. Lihat {@link #getTanggalPendaftaran()}. */
	private Date tanggalPendaftaran = new Date();

	/** Penanda pasien baru. Lihat {@link #getBaru()}. */
	private Boolean baru = false;

	/**
	 * Jenis perawatan: {@link #RAWAT_JALAN}, {@link #RAWAT_INAP}, atau {@link #RAWAT_UGD}.
	 * Lihat {@link #getJenis()}.
	 */
	private String jenis;

	/**
	 * Status pendaftaran, dimulai dari {@link #TERDAFTAR}. Lihat {@link #getStatusPendaftaran()}.
	 */
	private String statusPendaftaran = TERDAFTAR;

	/** Poliklinik tujuan. Lihat {@link #getPoly()}. */
	private Poly poly;

	/** Sub-poliklinik tujuan. Lihat {@link #getSubpoly()}. */
	private Poly subpoly;

	/** Tenaga medis penanggung jawab. Lihat {@link #getDokter()}. */
	private Dokter dokter;

	/** Nomor antrian pada jadwal yang dituju. Lihat {@link #getNomorAntrian()}. */
	private Integer nomorAntrian;

	/**
	 * Penanda pendaftaran berisi paket tindakan — nilai turunan yang selalu dihitung ulang; lihat
	 * {@link #getMerupakanPaket()}.
	 */
	private Boolean merupakanPaket = false;

	// Untuk Rawat Inap

	/** Kelas perawatan pasien. Lihat {@link #getKelasPerawatan()}. */
	private KelasPerawatan kelasPerawatan;

	/** Ruang perawatan (rawat inap). Lihat {@link #getRuangPerawatan()}. */
	private Ruang ruangPerawatan;

	/** Kamar perawatan (rawat inap). Lihat {@link #getKamarPerawatan()}. */
	private Kamar kamarPerawatan;

	/** Tempat tidur yang ditempati (rawat inap). Lihat {@link #getTempatTidur()}. */
	private TempatTidur tempatTidur;

	/** Cara/penanggung biaya perawatan sebagai teks bebas. Lihat {@link #getBiayaPerawatan()}. */
	private String biayaPerawatan;

	/** Nama penjamin pasien rawat inap. Lihat {@link #getNamaPenjamin()}. */
	private String namaPenjamin;

	/** Alamat penjamin pasien rawat inap. Lihat {@link #getAlamatPenjamin()}. */
	private String alamatPenjamin;

	/** Pekerjaan penjamin pasien rawat inap. Lihat {@link #getPekerjaanPenjamin()}. */
	private String pekerjaanPenjamin;

	/** Pendidikan terakhir penjamin. Lihat {@link #getPendidikanPenjamin()}. */
	private Pendidikan pendidikanPenjamin;

	/**
	 * Asal pasien rawat inap, salah satu konstanta {@code SUMBER_PASIEN_*}.
	 * Lihat {@link #getSumberPasien()}.
	 */
	private String sumberPasien;

	/** Riwayat tempat perawatan sebelumnya. Lihat {@link #getPernahDirawatDi()}. */
	private String pernahDirawatDi;

	/** Tanggal perawatan sebelumnya. Lihat {@link #getTanggalPernahDirawat()}. */
	private Date tanggalPernahDirawat;

	/** Nama dokter perujuk, sebagai teks bebas. Lihat {@link #getNamaDokterPengirim()}. */
	private String namaDokterPengirim;

	/** Field pendaftar bertipe teks — lihat catatan pada {@link #getPendaftar()}. */
	private String pendaftar;

	/** Nama orang yang mendaftarkan/mengantar pasien. Lihat {@link #getNamaPendaftar()}. */
	private String namaPendaftar;

	/** Alamat orang yang mendaftarkan/mengantar pasien. Lihat {@link #getAlamatPendaftar()}. */
	private String alamatPendaftar;

	/** Telepon orang yang mendaftarkan/mengantar pasien. Lihat {@link #getTelpPendaftar()}. */
	private String telpPendaftar;

	// private Dokter petugas;

	/** Waktu pasien keluar dari perawatan. Lihat {@link #getTanggalKeluar()}. */
	private Date tanggalKeluar;

	/** Penjamin/asuransi yang dipakai. Lihat {@link #getAsuransi()}. */
	private Asuransi asuransi;

	/** Kelas perawatan tujuan pemindahan. Lihat {@link #getPindahKeKelasPerawatan()}. */
	private KelasPerawatan pindahKeKelasPerawatan;

	/** Dokumen data pasien keluar. Lihat {@link #getDataPasienKeluar()}. */
	private DataPasienKeluar dataPasienKeluar;

	/** Umur pasien — nilai turunan yang selalu dihitung ulang; lihat {@link #getUmur()}. */
	private Integer umur;

	/**
	 * Jenis/golongan pasien — nilai turunan dari pasien; lihat {@link #getJenisPasien()}.
	 */
	private JenisPasien jenisPasien;

	/** Unit/cabang tempat pendaftaran dibuat. Lihat {@link #getLokasi()}. */
	private Lokasi lokasi;

	/** Shift pelayanan. Lihat {@link #getShift()}. */
	private Shift shift;

	/** Dokumen pembayaran terkait. Lihat {@link #getPembayaran()}. */
	private Pembayaran pembayaran;

	/**
	 * Penanda lunas — nilai turunan yang selalu dihitung ulang dari pembayaran; lihat
	 * {@link #getLunas()}.
	 */
	private Boolean lunas;

	/** Transaksi medis UGD terkait. Lihat {@link #getTransaksiUgd()}. */
	private TransaksiMedis transaksiUgd;

	/** Booking yang ditebus menjadi pendaftaran ini. Lihat {@link #getBookingRegistrasi()}. */
	private BookingRegistrasi bookingRegistrasi;

	/** Tanggal pelayanan hasil perhitungan dari hari jadwal. Lihat {@link #getDilayaniTanggal()}. */
	private Date dilayaniTanggal;

	/**
	 * Paket tindakan pada pendaftaran ini, lewat tabel penghubung
	 * {@code sirs.pandaftaran_has_paket}. Lihat {@link #getPakets()}.
	 */
	private Set<Tindakan> pakets = new HashSet<Tindakan>();

	/**
	 * Mengembalikan daftar paket tindakan pada pendaftaran ini.
	 *
	 * <p>Dipetakan {@code @ManyToMany} ke {@link Tindakan} lewat tabel penghubung
	 * <b>{@code sirs.pandaftaran_has_paket}</b> — perhatikan <b>salah ketik yang disengaja
	 * dipertahankan</b> pada nama tabel dan nama kolomnya ({@code pandaftaran}, bukan
	 * {@code pendaftaran}). Salah ketik itu sudah menjadi nama objek basis data yang nyata;
	 * memperbaikinya di kode tanpa migrasi basis data akan langsung memutus pemetaan. Tabel
	 * penghubung komunitas ({@link #getKomunitass()}) mengandung salah ketik yang sama.</p>
	 *
	 * <p>Isi koleksi ini umumnya diwarisi dari {@link BookingRegistrasi#getPakets()} ketika
	 * pendaftaran berasal dari booking. Pada alur simpan, layar pendaftaran menyetel ulang koleksi
	 * dengan {@code new HashSet<Tindakan>()} lalu menambahkan isi final, sehingga pendaftaran dan
	 * booking tidak berakhir berbagi satu object koleksi yang sama.</p>
	 *
	 * <p>Anotasi {@code @OrderBy("nama asc")} hanya mempengaruhi ORDER BY saat pemuatan; urutan
	 * iterasi {@link HashSet} tetap tidak terjamin.</p>
	 *
	 * @return koleksi paket tindakan; tidak pernah {@code null}.
	 * @see Tindakan
	 */
	@ManyToMany(targetEntity = Tindakan.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pandaftaran_has_paket", schema = "sirs", joinColumns = @JoinColumn(name = "pandaftaran"), inverseJoinColumns = @JoinColumn(name = "paket"))
	public Set<Tindakan> getPakets() {
		return pakets;
	}

	/**
	 * Mengganti seluruh daftar paket tindakan pendaftaran ini.
	 * <p>Mengganti referensi koleksi, bukan menggabungkan isinya; koleksi kosong yang di-flush akan
	 * menghapus seluruh baris tabel penghubung untuk pendaftaran ini. Karena
	 * {@link #getMerupakanPaket()} diturunkan dari isi koleksi ini, setter ini juga mengubah nilai
	 * penanda paket pada pembacaan berikutnya.</p>
	 *
	 * @param pakets koleksi paket tindakan yang baru.
	 */
	public void setPakets(Set<Tindakan> pakets) {
		this.pakets = pakets;
	}

	/**
	 * Cuplikan keanggotaan komunitas pasien pada saat pendaftaran; diisi oleh
	 * {@link #setPasienKomunitas(Pasien)}. Lihat {@link #getKomunitass()}.
	 */
	private Set<Komunitas> komunitass = new HashSet<Komunitas>();

	/**
	 * Mengembalikan cuplikan (<i>snapshot</i>) komunitas yang diikuti pasien pada saat pendaftaran
	 * dibuat, lewat tabel penghubung {@code sirs.pandaftaran_has_komunitas} (perhatikan salah ketik
	 * {@code pandaftaran} yang sengaja dipertahankan, sama seperti pada {@link #getPakets()}).
	 *
	 * <p><b>Ini data historis.</b> Isinya ditetapkan oleh {@link #setPasienKomunitas(Pasien)}
	 * berdasarkan keanggotaan yang berlaku pada {@link #getTanggalPendaftaran()}, lalu dibekukan.
	 * Komunitas menentukan potongan/tarif, sehingga daftar ini harus mencerminkan kondisi saat
	 * transaksi terjadi dan tidak boleh berubah surut. Untuk mengetahui keanggotaan komunitas
	 * pasien yang berlaku sekarang, kueri {@code KomunitasPunyaPasien} secara langsung.</p>
	 *
	 * <p>Isi koleksi ini ditampilkan pada beberapa layar transaksi medis — misalnya
	 * {@code CommonPendaftaranUtil.initTransaksi(...)} menampilkannya sebagai label dengan
	 * membuang tanda kurung siku dari hasil {@code toString()} koleksi.</p>
	 *
	 * @return koleksi komunitas pasien pada saat pendaftaran; tidak pernah {@code null}.
	 * @see #setPasienKomunitas(Pasien)
	 * @see Komunitas
	 */
	@ManyToMany(targetEntity = Komunitas.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pandaftaran_has_komunitas", schema = "sirs", joinColumns = @JoinColumn(name = "pandaftaran"), inverseJoinColumns = @JoinColumn(name = "komunitas"))
	public Set<Komunitas> getKomunitass() {
		return komunitass;
	}

	/**
	 * Mengganti seluruh cuplikan komunitas pendaftaran ini.
	 * <p>Umumnya tidak dipanggil langsung — pengisian yang benar dilakukan lewat
	 * {@link #setPasienKomunitas(Pasien)}, yang memanggil setter ini lebih dulu dengan
	 * {@link HashSet} kosong. Menyetel koleksi kosong lalu mem-flush akan menghapus data historis
	 * potongan/tarif untuk pendaftaran ini.</p>
	 *
	 * @param komunitass koleksi komunitas yang baru.
	 */
	public void setKomunitass(Set<Komunitas> komunitass) {
		this.komunitass = komunitass;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate, sekaligus dipakai ketiga layar
	 * pendaftaran saat petugas mendaftarkan pasien baru.
	 *
	 * <p>Field yang sudah terisi sejak awal: {@link #tanggalPendaftaran} = {@code new Date()},
	 * {@link #baru} = {@code false}, {@link #statusPendaftaran} = {@link #TERDAFTAR},
	 * {@link #merupakanPaket} = {@code false}, {@link #tanggal_dirubah} = {@code new Date()},
	 * serta koleksi {@link #pakets} dan {@link #komunitass} berupa {@link HashSet} kosong. Sisanya
	 * {@code null} — termasuk {@link #kode}, {@link #tbmuser}, dan {@link #dokter} yang kolomnya
	 * {@code nullable = false} sehingga wajib diisi sebelum penyimpanan.</p>
	 */
	public Pendaftaran() {
	}

	/**
	 * @return kunci utama teknis pendaftaran ini, atau {@code null} bila belum tersimpan.
	 *         Dihasilkan basis data ({@link javax.persistence.GenerationType#IDENTITY}) dan
	 *         dipetakan {@code insertable = false}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama teknis.
	 * <p>Diisi Hibernate setelah INSERT; jangan mengubahnya pada instance terkelola karena
	 * identitas object ({@code equals}) bergantung padanya.</p>
	 *
	 * @param id kunci utama teknis.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan jenis/golongan pasien, dengan <b>selalu menyalin ulang dari master pasien</b>
	 * bila tersedia.
	 *
	 * <h3>Perilaku: nilai turunan yang menimpa nilai tersimpan</h3>
	 * <p>Method ini bercabang dua:</p>
	 * <ul>
	 *   <li>bila field {@link #pasien} tidak {@code null} <i>dan</i> pasien tersebut punya jenis
	 *   pasien, maka {@link #jenisPasien} <b>ditimpa</b> dengan
	 *   {@code pasien.getJenisPasien()} — nilai yang tersimpan pada kolom
	 *   {@code pendaftaran.jenis_pasien} diabaikan sepenuhnya;</li>
	 *   <li>bila tidak, barulah nilai tersimpan dipakai, dilewatkan {@code check(...)} untuk
	 *   meresolusi proxy lazy.</li>
	 * </ul>
	 *
	 * <p>Konsekuensi yang harus disadari: <b>kolom {@code jenis_pasien} pada pendaftaran bukan data
	 * historis</b>. Selama pasien masih punya jenis pasien di master, pendaftaran lama akan ikut
	 * berubah mengikuti master setiap kali dibaca — dan karena Hibernate mengakses lewat properti,
	 * perubahan itu ikut ter-flush ke basis data serta menghasilkan baris revisi Envers. Jadi bila
	 * jenis pasien seseorang diubah dari "Umum" menjadi "BPJS", seluruh riwayat pendaftarannya
	 * perlahan akan tercatat sebagai "BPJS" juga. Bila laporan memerlukan jenis pasien pada saat
	 * transaksi, nilai itu <b>tidak tersedia</b> dari field ini; sumber historis yang tersedia
	 * hanyalah {@link #getKomunitass()} dan {@link #getAsuransi()}.</p>
	 *
	 * <p>Perhatikan pula bahwa cabang pertama membaca field {@link #pasien} <i>secara langsung</i>,
	 * bukan lewat {@link #getPasien()}. Artinya proxy lazy pasien tidak diresolusi lebih dulu; bila
	 * pasien masih berupa proxy yang belum terinisialisasi pada instance yang sudah
	 * <i>detached</i>, pemanggilan {@code pasien.getJenisPasien()} dapat melempar
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return jenis/golongan pasien, atau {@code null}.
	 * @see JenisPasien
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pasien", nullable = true)
	public JenisPasien getJenisPasien() {
		if (pasien != null && pasien.getJenisPasien() != null) {
			jenisPasien = pasien.getJenisPasien();
		} else {
			jenisPasien = check(jenisPasien);
		}
		return jenisPasien;
	}

	/**
	 * Menyetel jenis/golongan pasien.
	 * <p>Nilai yang disetel di sini <b>hanya bertahan selama pasien tidak memiliki jenis pasien di
	 * master</b> — lihat penjelasan pada {@link #getJenisPasien()}. Untuk pasien yang jenisnya
	 * sudah terisi di master, setter ini praktis tanpa efek.</p>
	 *
	 * @param jenisPasien jenis/golongan pasien.
	 */
	public void setJenisPasien(JenisPasien jenisPasien) {
		this.jenisPasien = jenisPasien;
	}

	/**
	 * @return catatan bebas petugas mengenai pendaftaran ini (kolom {@code keterangan}), atau
	 *         {@code null}. Nilainya ikut membentuk {@link #toString()}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas petugas.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel kode pendaftaran.
	 * <p>Kolomnya {@code unique = true, nullable = false}; pelanggaran keunikan baru terdeteksi
	 * saat flush.</p>
	 *
	 * @param kode kode unik pendaftaran.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return kode unik pendaftaran (kolom {@code kode}, {@code unique = true},
	 *         {@code nullable = false}).
	 *         <p>Dibangkitkan {@code Common.generateCode(Pendaftaran.class, 10, "<awalan>",
	 *         lokasi)} dengan awalan sesuai jenis perawatan — {@code REG-RAJAL} untuk rawat jalan,
	 *         dan awalan sepadan pada layar rawat inap serta UGD — sehingga kode
	 *         <b>tersegmentasi per lokasi</b>. Layar mengisi nilai sementara dengan
	 *         {@code Common.generateCode(Pendaftaran.class, 8)} tanpa awalan pada saat form dibuka,
	 *         lalu menimpanya dengan varian berlokasi pada saat simpan.</p>
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel pendaftaran asal pemindahan.
	 * <p>Setter ini <b>tidak</b> mengubah status pendaftaran asal menjadi {@link #PINDAH}; itu
	 * dilakukan terpisah oleh {@code PindahTempatTidurRawatInapAction}. Tidak ada penjaga terhadap
	 * rantai melingkar (pendaftaran yang menunjuk dirinya sendiri atau membentuk siklus), sehingga
	 * kode yang menelusuri rantai pemindahan harus menyiapkan pembatas kedalaman.</p>
	 *
	 * @param transferDaripendaftaran pendaftaran asal; {@code null} bila bukan hasil pemindahan.
	 */
	public void setTransferDaripendaftaran(Pendaftaran transferDaripendaftaran) {
		this.transferDaripendaftaran = transferDaripendaftaran;
	}

	/**
	 * Mengembalikan pendaftaran <b>asal</b> bila pendaftaran ini lahir dari pemindahan kelas atau
	 * tempat tidur, atau {@code null} bila pendaftaran ini berdiri sendiri.
	 *
	 * <p>Ini relasi rekursif ke tipe yang sama ({@code pendaftaran.transfer_dari_pendaftaran}
	 * menunjuk {@code pendaftaran.id}). Alurnya: {@code PindahTempatTidurRawatInapAction} menutup
	 * pendaftaran lama dengan status {@link #PINDAH}, membuat pendaftaran baru untuk kelas/tempat
	 * tidur yang baru, lalu menautkan keduanya lewat relasi ini. Dengan begitu satu episode rawat
	 * inap yang mengalami beberapa kali pemindahan tersimpan sebagai <b>rantai</b> beberapa baris
	 * {@code Pendaftaran}, bukan satu baris yang terus diperbarui — sehingga biaya per kelas
	 * perawatan tetap dapat dipisahkan.</p>
	 *
	 * <p>Relasi tidak menyatakan {@code fetch = LAZY}, sehingga memakai default
	 * {@link FetchType#EAGER} diperkuat
	 * {@link Fetch @Fetch}{@code (}{@link FetchMode#SELECT}{@code )} — itu sebabnya getter ini
	 * tidak memanggil {@code check(...)}. Perlu diperhatikan bahwa pemuatan eager pada relasi
	 * rekursif berarti setiap pemuatan pendaftaran juga menyeret seluruh rantai pendahulunya lewat
	 * SELECT berantai.</p>
	 *
	 * @return pendaftaran asal pemindahan, atau {@code null}.
	 * @see #PINDAH
	 * @see #getPindahKeKelasPerawatan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transfer_dari_pendaftaran", nullable = true)
	public Pendaftaran getTransferDaripendaftaran() {
		return transferDaripendaftaran;
	}

	/**
	 * Menyetel bagian/unit organisasi terkait pendaftaran ini.
	 *
	 * @param bagian bagian/unit; boleh {@code null}.
	 */
	public void setBagian(Bagian bagian) {
		this.bagian = bagian;
	}

	/**
	 * Mengembalikan bagian/unit organisasi terkait, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Kolomnya {@code bagian}, {@code nullable = true}. Berbeda dari {@link #getPoly()} yang
	 * merupakan tujuan pelayanan medis, {@link Bagian} bersifat organisatoris. Ketiga layar
	 * pendaftaran tidak menyediakan input untuk field ini, sehingga pada data yang lahir dari alur
	 * normal nilainya umumnya {@code null}.</p>
	 *
	 * @return bagian/unit organisasi, atau {@code null}.
	 * @see Bagian
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bagian", nullable = true)
	public Bagian getBagian() {
		bagian = check(bagian);
		return bagian;
	}

	/**
	 * Menyetel petugas yang melakukan pendaftaran.
	 * <p>Ketiga layar pendaftaran mengisinya dengan {@code Common.getCurrentUser()} pada saat
	 * simpan, sehingga nilainya mengikuti pengguna yang sedang login.</p>
	 *
	 * @param tbmuser pengguna aplikasi yang mendaftarkan pasien.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan petugas (pengguna aplikasi) yang melakukan pendaftaran, setelah meresolusi
	 * proxy lazy-nya lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code tbmuser}, {@code nullable = false} — setiap pendaftaran wajib menyebut
	 * petugasnya. Bedakan tiga hal yang mudah tertukar pada entity ini:</p>
	 * <ul>
	 *   <li><b>{@code tbmuser}</b> — petugas rumah sakit yang mengetikkan pendaftaran;</li>
	 *   <li><b>{@link #getOleh()}/{@link #getOlehId()}</b> — jejak audit pengubah <i>terakhir</i>,
	 *   yang bisa berbeda dari petugas pendaftar bila dokumen kemudian diubah orang lain;</li>
	 *   <li><b>{@link #getNamaPendaftar()} dan kerabatnya</b> — identitas keluarga/pengantar pasien,
	 *   bukan pengguna aplikasi.</li>
	 * </ul>
	 *
	 * @return petugas yang melakukan pendaftaran.
	 * @see ais.database.model.Tbmuser
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = false)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menyetel waktu pendaftaran.
	 * <p><b>Urutan pemanggilan penting.</b> Nilai ini menjadi acuan dua perhitungan turunan:
	 * cuplikan komunitas pada {@link #setPasienKomunitas(Pasien)} dan tanggal pelayanan pada
	 * {@link #getDilayaniTanggal()}. Menyetelnya setelah kedua perhitungan itu berjalan tidak akan
	 * memperbarui hasilnya.</p>
	 *
	 * @param tanggalPendaftaran waktu pasien mendaftar.
	 */
	public void setTanggalPendaftaran(Date tanggalPendaftaran) {
		this.tanggalPendaftaran = tanggalPendaftaran;
	}

	/**
	 * Mengembalikan waktu pendaftaran, dengan <b>menulis balik</b> {@code new Date()} bila masih
	 * kosong.
	 *
	 * <p>Getter destruktif ringan: baris lama yang kolomnya {@code NULL} akan terisi waktu
	 * <i>pembacaan</i>, bukan waktu pendaftaran sesungguhnya, dan nilai itu ikut ter-flush.
	 * Perlakukan nilai yang muncul dengan cara demikian sebagai perkiraan.</p>
	 *
	 * <p>Getter ini tidak beranotasi {@link Temporal @Temporal}, sehingga pemetaannya mengikuti
	 * default penyedia JPA untuk properti {@link Date} — <b>komponen jam ikut tersimpan</b>. Ini
	 * relevan karena beberapa query di {@code CommonPendaftaranUtil} membandingkan tanggal
	 * pelayanan bertipe DATE dengan nilai waktu yang mengandung jam.</p>
	 *
	 * @return waktu (dan jam) pendaftaran; tidak pernah {@code null}.
	 */
	public Date getTanggalPendaftaran() {
		if (tanggalPendaftaran == null) {
			tanggalPendaftaran = new Date();
		}
		return tanggalPendaftaran;
	}

	/**
	 * Menyetel status pendaftaran.
	 * <p><b>Tidak ada validasi apa pun</b> — setter ini menerima string sembarang, bukan hanya
	 * keempat konstanta {@link #TERDAFTAR}, {@link #KELUAR}, {@link #PINDAH}, dan
	 * {@link #MENINGGAL}. Tidak ada pula mesin transisi status: perpindahan dari "Pulang" kembali
	 * ke "Masih Dalam Perawatan" dimungkinkan dan memang dipakai oleh
	 * {@code DataPasienKeluarAction} saat data pasien keluar dibatalkan. Konsistensi status
	 * sepenuhnya menjadi tanggung jawab layar pemanggil.</p>
	 *
	 * @param statusPendaftaran status pendaftaran; idealnya salah satu konstanta status pada kelas
	 *                          ini.
	 */
	public void setStatusPendaftaran(String statusPendaftaran) {
		this.statusPendaftaran = statusPendaftaran;
	}

	/**
	 * Mengembalikan status pendaftaran sebagai teks bebas, dimulai dari {@link #TERDAFTAR} pada
	 * pendaftaran baru.
	 *
	 * <p>Empat nilai yang dipakai alur aplikasi:</p>
	 * <ul>
	 *   <li>{@link #TERDAFTAR} — pasien masih dalam perawatan (nilai awal, juga dipulihkan saat
	 *   pembatalan data pasien keluar atau pembatalan pemindahan);</li>
	 *   <li>{@link #KELUAR} — pasien pulang; disetel {@code DataPasienKeluarAction};</li>
	 *   <li>{@link #MENINGGAL} — pasien meninggal; disetel {@code DataPasienKeluarAction};</li>
	 *   <li>{@link #PINDAH} — pendaftaran ditutup karena pemindahan kelas/tempat tidur; disetel
	 *   {@code PindahTempatTidurRawatInapAction} pada pendaftaran asal.</li>
	 * </ul>
	 *
	 * <p>Nilai ini menjadi filter utama laporan rawat inap ({@code LaporanRanapPerRuangan},
	 * {@code NewUiLaporanSirsController}) dan juga disetel oleh jalur non-UI
	 * {@code ApotikDemoProvisionHelper}. Karena disimpan sebagai teks bebas tanpa validasi (lihat
	 * {@link #setStatusPendaftaran(String)}), kode yang mencocokkan status sebaiknya menyiapkan
	 * cabang "tidak dikenal" alih-alih mengasumsikan hanya ada empat nilai.</p>
	 *
	 * <p>Getter ini tidak beranotasi {@link Column @Column}, jadi dipetakan berdasarkan konvensi
	 * penamaan properti.</p>
	 *
	 * @return status pendaftaran; tidak pernah {@code null} pada instance yang dibuat lewat
	 *         konstruktor, tetapi dapat {@code null} pada baris yang disetel eksplisit.
	 */
	public String getStatusPendaftaran() {
		return statusPendaftaran;
	}

	/**
	 * Menyetel pasien pendaftaran ini <b>sekaligus mengambil cuplikan keanggotaan komunitasnya
	 * dari basis data</b>.
	 *
	 * <h3>Setter yang menjalankan query</h3>
	 * <p>Selain menugaskan {@code pasien} ke field, method ini membuka
	 * {@link org.hibernate.Session} thread-local lewat {@code HibernateUtil.currentSession()} dan
	 * menjalankan criteria atas {@code KomunitasPunyaPasien} untuk menentukan komunitas mana saja
	 * yang diikuti pasien tersebut <i>pada saat pendaftaran dibuat</i>. Hasilnya dibekukan ke
	 * {@link #getKomunitass()} sehingga tersimpan ke tabel penghubung
	 * {@code sirs.pandaftaran_has_komunitas}. Alasan desainnya: komunitas menentukan potongan atau
	 * tarif khusus, dan tarif itu harus mengikuti kondisi saat transaksi terjadi — bukan berubah
	 * surut ketika pasien kelak keluar-masuk komunitas.</p>
	 *
	 * <h3>Kriteria penyaringan komunitas</h3>
	 * <p>Query menggabungkan alias {@code komunitas} lalu menerapkan empat pembatasan:</p>
	 * <ul>
	 *   <li>{@code komunitas.mulai <= tanggalPendaftaran} — komunitas sudah mulai berlaku;</li>
	 *   <li>{@code komunitas.sampai IS NULL OR komunitas.sampai >= tanggalPendaftaran} — komunitas
	 *   belum berakhir, dengan {@code null} berarti tanpa batas akhir;</li>
	 *   <li>{@code pasien = <pasien>} — hanya keanggotaan milik pasien tersebut;</li>
	 *   <li>{@code komunitas.aktif = true} — komunitas nonaktif diabaikan.</li>
	 * </ul>
	 * <p>Proyeksi {@code Projections.groupProperty("komunitas")} membuat hasilnya berupa daftar
	 * {@link Komunitas} yang sudah unik. Perlu dicatat bahwa penyaringan rentang tanggal dilakukan
	 * pada properti <b>komunitas</b> — yaitu masa berlaku komunitasnya — bukan pada masa
	 * keanggotaan pasien di komunitas itu. Pasien yang sudah lama keluar dari sebuah komunitas
	 * tetap terjaring selama komunitas tersebut masih berlaku dan aktif.</p>
	 *
	 * <h3>Urutan pemanggilan dan efek samping</h3>
	 * <ol>
	 *   <li><b>Cuplikan lama selalu dihapus lebih dulu</b> lewat
	 *   {@code setKomunitass(new HashSet<Komunitas>())}. Memanggilnya dengan {@code pasien}
	 *   bernilai {@code null} — atau ketika query tidak menemukan apa pun — akan
	 *   <b>mengosongkan</b> daftar komunitas pendaftaran ini, dan pengosongan itu tersimpan sebagai
	 *   penghapusan baris tabel penghubung pada flush berikutnya.</li>
	 *   <li><b>Acuan tanggalnya dibaca saat itu juga</b> lewat
	 *   {@link #getTanggalPendaftaran()}; bila tanggal belum disetel, getter itu mengisinya dengan
	 *   waktu sekarang. Setel tanggal pendaftaran sebelum memanggil method ini bila pendaftaran
	 *   dicatat mundur.</li>
	 *   <li><b>Membutuhkan session Hibernate yang aktif</b>, sehingga tidak dapat dipanggil dari
	 *   thread latar, proses deserialisasi, atau unit test tanpa fixture basis data — berbeda dari
	 *   setter lain di kelas ini yang murni menugaskan nilai.</li>
	 *   <li><b>Menembus setter pasangannya</b>: penugasan terakhir dilakukan langsung ke field
	 *   ({@code this.pasien = pasien}), bukan lewat {@link #setPasien(Pasien)}.</li>
	 *   <li><b>Ada method kembar di {@link BookingRegistrasi}.</b>
	 *   {@link BookingRegistrasi#setPasienKomunitas(Pasien)} berisi query identik dengan acuan
	 *   {@code tanggalBookingRegistrasi}. Saat booking ditebus menjadi pendaftaran, yang dipanggil
	 *   adalah versi ini — sehingga komunitas dihitung ulang pada tanggal pendaftaran dan hasilnya
	 *   <b>dapat berbeda</b> dari cuplikan yang tersimpan di booking. Perubahan kriteria di sini
	 *   hampir selalu harus dicerminkan di sana.</li>
	 * </ol>
	 *
	 * <p>Dipanggil oleh keempat jalur pendaftaran ({@code PendaftaranRawatJalanAction},
	 * {@code PendaftaranRawatInapAction}, {@code PendaftaranRawatUgdAction}, dan
	 * {@code CommonPendaftaranUtil}) pada saat simpan, dengan pola
	 * {@code pendaftaran.setPasienKomunitas(pendaftaran.getPasien())} — yaitu memberi ulang pasien
	 * yang sudah tersetel semata-mata untuk memicu pengambilan cuplikan komunitas.</p>
	 *
	 * <p>Anotasi {@code @SuppressWarnings("unchecked")} diperlukan karena {@code Criteria.list()}
	 * pada API Hibernate 3 mengembalikan {@link List} mentah.</p>
	 *
	 * @param pasien pasien pendaftaran; {@code null} akan mengosongkan cuplikan komunitas dan
	 *               melewati query.
	 * @see #getKomunitass()
	 * @see BookingRegistrasi#setPasienKomunitas(Pasien)
	 */
	@SuppressWarnings("unchecked")
	public void setPasienKomunitas(Pasien pasien) {
		setKomunitass(new HashSet<Komunitas>());
		if (pasien != null && getTanggalPendaftaran() != null) {
			List<Komunitas> komunitas = HibernateUtil.currentSession().createCriteria(KomunitasPunyaPasien.class)
					.createAlias("komunitas", "komunitas")
					.add(Restrictions.le("komunitas.mulai", getTanggalPendaftaran()))
					.add(Restrictions.or(Restrictions.isNull("komunitas.sampai"),
							Restrictions.ge("komunitas.sampai", getTanggalPendaftaran())))
					.add(Restrictions.eq("pasien", pasien)).add(Restrictions.eq("komunitas.aktif", true))
					.setProjection(Projections.groupProperty("komunitas")).list();
			getKomunitass().addAll(komunitas);
		}

		this.pasien = pasien;
	}

	public void setPasien(Pasien pasien) {
		this.pasien = pasien;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pasien", nullable = true)
	public Pasien getPasien() {
		pasien = check(pasien);
		return pasien;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	public String getJenis() {
		return jenis;
	}

	public void setBaru(Boolean baru) {
		this.baru = baru;
	}

	public Boolean getBaru() {
		return baru;
	}

	public void setPoly(Poly poly) {
		this.poly = poly;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "poly", nullable = true)
	public Poly getPoly() {
		if (getJenis() != null && getJenis().equals(RAWAT_UGD)) {
//			poly = ConstantValues.POLI_UGD;
		}
		poly = check(poly);
		return poly;
	}

	public void setDokter(Dokter dokter) {
		this.dokter = dokter;
	}

	public String ambilAlamat() {

		String alamat = "";
		try {
			pasien = check(pasien);

			alamat = pasien.getAlamatLengkap();

			alamat = (pasien.getAlamat() + " " + (pasien.getKelurahan() == null ? "" : pasien.getKelurahan().getNama())
					+ (pasien.getRt() == null ? "" : " RT " + pasien.getRt())
					+ (pasien.getRw() == null ? "" : " RW " + pasien.getRw()) + " "
					+ (pasien.getKecamatan() == null ? "" : pasien.getKecamatan().getNama()) + " "
					+ (pasien.getKota() == null ? "" : pasien.getKota().getNama()) + " "
					+ (pasien.getPropinsi() == null ? "" : pasien.getPropinsi().getNama()));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sirs/Pendaftaran.java:366");
			// TODO: handle exception
		}
		return alamat;

	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dokter", nullable = false)
	public Dokter getDokter() {
		dokter = check(dokter);
		return dokter;
	}

	public void setNomorAntrian(Integer nomorAntrian) {
		this.nomorAntrian = nomorAntrian;
	}

	@Column(name = "nomor_antrian", nullable = true)
	public Integer getNomorAntrian() {
		return nomorAntrian;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tempat_tidur", nullable = true)
	public TempatTidur getTempatTidur() {
		tempatTidur = check(tempatTidur);
		return tempatTidur;
	}

	public void setTempatTidur(TempatTidur tempatTidur) {
		this.tempatTidur = tempatTidur;
	}

	@Column(name = "biaya_perawatan", nullable = true)
	public String getBiayaPerawatan() {
		return biayaPerawatan;
	}

	public void setBiayaPerawatan(String biayaPerawatan) {
		this.biayaPerawatan = biayaPerawatan;
	}

	@Column(name = "nama_penjamin", nullable = true)
	public String getNamaPenjamin() {
		return namaPenjamin;
	}

	public void setNamaPenjamin(String namaPenjamin) {
		this.namaPenjamin = namaPenjamin;
	}

	@Column(name = "alamat_penjamin", nullable = true)
	public String getAlamatPenjamin() {
		return alamatPenjamin;
	}

	public void setAlamatPenjamin(String alamatPenjamin) {
		this.alamatPenjamin = alamatPenjamin;
	}

	@Column(name = "pekerjaan_penjamin", nullable = true)
	public String getPekerjaanPenjamin() {
		return pekerjaanPenjamin;
	}

	public void setPekerjaanPenjamin(String pekerjaanPenjamin) {
		this.pekerjaanPenjamin = pekerjaanPenjamin;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_penjamin", nullable = true)
	public Pendidikan getPendidikanPenjamin() {
		pendidikanPenjamin = check(pendidikanPenjamin);
		return pendidikanPenjamin;
	}

	public void setPendidikanPenjamin(Pendidikan pendidikanPenjamin) {
		this.pendidikanPenjamin = pendidikanPenjamin;
	}

	@Column(name = "sumber_pasien", nullable = true)
	public String getSumberPasien() {
		return sumberPasien;
	}

	public void setSumberPasien(String sumberPasien) {
		this.sumberPasien = sumberPasien;
	}

	@Column(name = "pernah_dirawat_di", nullable = true)
	public String getPernahDirawatDi() {
		return pernahDirawatDi;
	}

	public void setPernahDirawatDi(String pernahDirawatDi) {
		this.pernahDirawatDi = pernahDirawatDi;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pernah_dirawat", nullable = true)
	public Date getTanggalPernahDirawat() {
		return tanggalPernahDirawat;
	}

	public void setTanggalPernahDirawat(Date tanggalPernahDirawat) {
		this.tanggalPernahDirawat = tanggalPernahDirawat;
	}

	@Column(name = "nama_dokter_pengirim", nullable = true)
	public String getNamaDokterPengirim() {
		return namaDokterPengirim;
	}

	public void setNamaDokterPengirim(String namaDokterPengirim) {
		this.namaDokterPengirim = namaDokterPengirim;
	}

	@Column(name = "pendaftar", nullable = true)
	public String getPendaftar() {
		return pendaftar;
	}

	public void setPendaftar(String pendaftar) {
		this.pendaftar = pendaftar;
	}

	@Column(name = "alamat_pendaftar", nullable = true)
	public String getAlamatPendaftar() {
		return alamatPendaftar;
	}

	public void setAlamatPendaftar(String alamatPendaftar) {
		this.alamatPendaftar = alamatPendaftar;
	}

	@Column(name = "telp_pendaftar", nullable = true)
	public String getTelpPendaftar() {
		return telpPendaftar;
	}

	public void setTelpPendaftar(String telpPendaftar) {
		this.telpPendaftar = telpPendaftar;
	}

	// @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	// @Fetch(FetchMode.SELECT)@JoinColumn(name = "petugas", nullable = true)
	// public Dokter getPetugas() {
	// return petugas;
	// }
	//
	// public void setPetugas(Dokter petugas) {
	// this.petugas = petugas;
	// }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_keluar", nullable = true)
	public Date getTanggalKeluar() {
		return tanggalKeluar;
	}

	public void setTanggalKeluar(Date tanggalKeluar) {
		this.tanggalKeluar = tanggalKeluar;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi", nullable = true)
	public Asuransi getAsuransi() {
		asuransi = check(asuransi);
		return asuransi;
	}

	public void setAsuransi(Asuransi asuransi) {
		this.asuransi = asuransi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pindah_ke_kelas_perawatan", nullable = true)
	public KelasPerawatan getPindahKeKelasPerawatan() {
		pindahKeKelasPerawatan = check(pindahKeKelasPerawatan);
		return pindahKeKelasPerawatan;
	}

	public void setPindahKeKelasPerawatan(KelasPerawatan pindahKeKelasPerawatan) {
		this.pindahKeKelasPerawatan = pindahKeKelasPerawatan;
	}

	public void setUmur(Integer umur) {
		this.umur = umur;
	}

	public Integer getUmur() {
		if (getPasien() != null) {
			umur = getPasien().getUmur();
		}
		if (umur == null) {
			umur = 0;
		}
		return umur;
	}

	public void setNamaPendaftar(String namaPendaftar) {
		this.namaPendaftar = namaPendaftar;
	}

	@Column(name = "nama_pendaftar", nullable = true)
	public String getNamaPendaftar() {
		return namaPendaftar;
	}

	public void setKelasPerawatan(KelasPerawatan kelasPerawatan) {
		this.kelasPerawatan = kelasPerawatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_perawatan", nullable = true)
	public KelasPerawatan getKelasPerawatan() {
		if (kelasPerawatan == null) {
			kelasPerawatan = ConstantValues.kelasNormal;
		}
		kelasPerawatan = check(kelasPerawatan);
		return kelasPerawatan;
	}

	public void setRuangPerawatan(Ruang ruangPerawatan) {
		this.ruangPerawatan = ruangPerawatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_perawatan", nullable = true)
	public Ruang getRuangPerawatan() {
		ruangPerawatan = check(ruangPerawatan);
		return ruangPerawatan;
	}

	public void setKamarPerawatan(Kamar kamarPerawatan) {
		this.kamarPerawatan = kamarPerawatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar_perawatan", nullable = true)
	public Kamar getKamarPerawatan() {
		kamarPerawatan = check(kamarPerawatan);
		return kamarPerawatan;
	}

	public void setDataPasienKeluar(DataPasienKeluar dataPasienKeluar) {
		this.dataPasienKeluar = dataPasienKeluar;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "data_pasien_keluar", nullable = true)
	public DataPasienKeluar getDataPasienKeluar() {
		return dataPasienKeluar;
	}

	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	public void setIndex(Long index) {
		this.index = index;
	}

	public Long getIndex() {
		return index;
	}

	public void setShift(Shift shift) {
		this.shift = shift;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "shift", nullable = true)
	public Shift getShift() {
		shift = check(shift);
		return shift;
	}

	public void setPembayaran(Pembayaran pembayaran) {
		this.pembayaran = pembayaran;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran", nullable = true)
	public Pembayaran getPembayaran() {
		return pembayaran;
	}

	public void setLunas(Boolean lunas) {
		this.lunas = lunas;
	}

	public Boolean getLunas() {
		lunas = pembayaran != null
				&& pembayaran.getTotalBiaya() <= (pembayaran.getBayarTunai() + pembayaran.getBayarNonTunai());
		return lunas;
	}

	public void setSubpoly(Poly subpoly) {
		this.subpoly = subpoly;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "subpoly", nullable = true)
	public Poly getSubpoly() {
		subpoly = check(subpoly);
		return subpoly;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_ugd", nullable = true)
	public TransaksiMedis getTransaksiUgd() {
		return transaksiUgd;
	}

	public void setTransaksiUgd(TransaksiMedis transaksiUgd) {
		this.transaksiUgd = transaksiUgd;
	}

	public Boolean getMerupakanPaket() {
		merupakanPaket = !pakets.isEmpty();
		return merupakanPaket;
	}

	public void setMerupakanPaket(Boolean merupakanPaket) {
		this.merupakanPaket = merupakanPaket;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "booking_registrasi", nullable = true)
	public BookingRegistrasi getBookingRegistrasi() {
		return bookingRegistrasi;
	}

	public void setBookingRegistrasi(BookingRegistrasi bookingRegistrasi) {
		this.bookingRegistrasi = bookingRegistrasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_dokter", nullable = true)
	public JadwalDokter getJadwalDokter() {
		jadwalDokter = check(jadwalDokter);
		return jadwalDokter;
	}

	public void setJadwalDokter(JadwalDokter jadwalDokter) {
		this.jadwalDokter = jadwalDokter;
	}

	@Temporal(TemporalType.DATE)
	@Column(nullable = false)
	public Date getDilayaniTanggal() {
		if (dilayaniTanggal == null) {
			if (getJadwalDokter() != null && getTanggalPendaftaran() != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(getTanggalPendaftaran());
				dilayaniTanggal = calendar.getTime();
				String hari = getJadwalDokter().getHari();
				String currHari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
				while (!hari.equalsIgnoreCase(currHari)) {
					calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
					currHari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
					dilayaniTanggal = calendar.getTime();
				}
			}
		}
		return dilayaniTanggal;
	}

	public void setDilayaniTanggal(Date dilayaniTanggal) {
		this.dilayaniTanggal = dilayaniTanggal;
	}

}
