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

	/**
	 * Menyetel pasien pendaftaran ini <b>tanpa</b> memperbarui cuplikan komunitas.
	 * <p>Gunakan {@link #setPasienKomunitas(Pasien)} bila cuplikan komunitas juga perlu diambil
	 * ulang — itulah yang dilakukan ketiga layar pendaftaran pada saat simpan, dengan memanggil
	 * setter ini lebih dulu lalu {@code setPasienKomunitas(getPasien())}.</p>
	 *
	 * @param pasien pasien yang mendaftar.
	 */
	public void setPasien(Pasien pasien) {
		this.pasien = pasien;
	}

	/**
	 * Mengembalikan pasien yang mendaftar, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p><b>Perhatikan pemetaannya:</b> kolom {@code pasien} di sini dipetakan
	 * {@code nullable = true}, berbeda dari {@link BookingRegistrasi#getPasien()} yang
	 * {@code nullable = false}. Jadi pada tingkat basis data sebuah pendaftaran <b>boleh tidak
	 * menyebut pasien</b>. Baris seperti itu praktis tidak berguna — {@link #getUmur()} akan
	 * mengembalikan 0, {@link #getJenisPasien()} jatuh ke nilai tersimpan, dan
	 * {@link #ambilAlamat()} mengembalikan string kosong — tetapi tidak dicegah, sehingga kode
	 * yang membaca pendaftaran wajib memeriksa {@code null} sebelum menelusuri data pasien.</p>
	 *
	 * <p>Nilai ini menjadi sumber beberapa nilai turunan pada entity ini: {@link #getUmur()},
	 * {@link #getJenisPasien()}, dan {@link #ambilAlamat()} semuanya membacanya.</p>
	 *
	 * @return pasien yang mendaftar, atau {@code null}.
	 * @see Pasien
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pasien", nullable = true)
	public Pasien getPasien() {
		pasien = check(pasien);
		return pasien;
	}

	/**
	 * Menyetel jenis perawatan.
	 * <p><b>Tidak ada validasi</b> terhadap ketiga konstanta {@link #RAWAT_JALAN},
	 * {@link #RAWAT_INAP}, dan {@link #RAWAT_UGD}; setter ini menerima string sembarang. Pada alur
	 * normal nilainya ditentukan oleh layar mana yang dipakai, bukan dipilih pengguna.</p>
	 *
	 * @param jenis jenis perawatan; idealnya salah satu konstanta {@code RAWAT_*}.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan jenis perawatan sebagai teks bebas: {@link #RAWAT_JALAN},
	 * {@link #RAWAT_INAP}, atau {@link #RAWAT_UGD}.
	 *
	 * <p>Nilai ini menentukan kelompok field mana yang bermakna pada baris pendaftaran ini —
	 * seluruh blok rawat inap (ruang, kamar, tempat tidur, data penjamin, sumber pasien, status
	 * pendaftaran, data pasien keluar) hanya terisi untuk {@link #RAWAT_INAP}. Karena tidak ada
	 * penjaga di tingkat entity, baris rawat jalan secara teknis tetap dapat memiliki tempat tidur
	 * atau data penjamin terisi.</p>
	 *
	 * <p>Konstanta {@code RAWAT_*} dirujuk di puluhan titik pada basis kode (layar, laporan, dan
	 * helper), sehingga nilainya berperilaku seperti enum berbasis string. Pembandingannya
	 * memakai {@code equals} yang peka huruf besar-kecil, jadi data dengan kapitalisasi berbeda
	 * tidak akan cocok.</p>
	 *
	 * <p>Getter ini tidak beranotasi {@link Column @Column}, jadi dipetakan berdasarkan konvensi.</p>
	 *
	 * @return jenis perawatan, atau {@code null} bila belum diisi.
	 */
	public String getJenis() {
		return jenis;
	}

	/**
	 * Menyetel penanda pasien baru.
	 * <p>Diisi dari sebuah checkbox pada layar pendaftaran ({@code baru.isChecked()}), sehingga
	 * pada alur normal nilainya tidak pernah {@code null}.</p>
	 *
	 * @param baru {@code true} bila pasien baru pertama kali berkunjung.
	 */
	public void setBaru(Boolean baru) {
		this.baru = baru;
	}

	/**
	 * @return penanda pasien baru; {@code false} sebagai nilai awal dari konstruktor, tetapi
	 *         <b>dapat bernilai {@code null}</b> pada baris lama yang kolomnya {@code NULL}.
	 *         <p>Berbeda dari {@link BookingRegistrasi#getBaru()} yang memiliki cabang default dan
	 *         menulis balik {@code false}, getter ini mengembalikan isi field apa adanya. Kode yang
	 *         menangani keduanya secara seragam — atau yang melakukan <i>unboxing</i> ke
	 *         {@code boolean} — harus menyadari perbedaan ini agar tidak terkena
	 *         {@link NullPointerException}.</p>
	 *         <p>Tidak beranotasi {@link Column @Column}; dipetakan berdasarkan konvensi.</p>
	 */
	public Boolean getBaru() {
		return baru;
	}

	/**
	 * Menyetel poliklinik tujuan.
	 *
	 * @param poly poliklinik tujuan; boleh {@code null}.
	 */
	public void setPoly(Poly poly) {
		this.poly = poly;
	}

	/**
	 * Mengembalikan poliklinik tujuan, setelah meresolusi proxy lazy-nya lewat {@code check(...)}.
	 *
	 * <p><b>Berisi satu cabang mati.</b> Method ini masih memuat blok
	 * {@code if (getJenis() != null && getJenis().equals(RAWAT_UGD)) { ... }} yang <b>badannya
	 * seluruhnya dikomentari</b> — semula bermaksud memaksa poli menjadi
	 * {@code ConstantValues.POLI_UGD} untuk pendaftaran UGD. Dalam bentuknya sekarang blok itu
	 * tidak melakukan apa pun selain memanggil {@link #getJenis()} dua kali. Konsekuensi praktis
	 * yang perlu diketahui: <b>pendaftaran UGD tidak dipaksa memakai poli UGD</b>; nilainya
	 * mengikuti apa pun yang disetel layar. Bila laporan mengasumsikan setiap pendaftaran UGD
	 * berpoli UGD, asumsi itu tidak dijamin oleh kode ini.</p>
	 *
	 * <p>Blok mati ini sebaiknya tidak dihapus tanpa keputusan eksplisit: kehadirannya menandai
	 * niat desain yang belum selesai, dan menghidupkannya kembali akan mengubah data poli pada
	 * setiap pembacaan pendaftaran UGD — sebuah perubahan berdampak luas karena penulisan balik
	 * pada getter ikut ter-flush.</p>
	 *
	 * <p>Kolomnya {@code poly}, {@code nullable = true}.</p>
	 *
	 * @return poliklinik tujuan, atau {@code null}.
	 * @see Poly
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "poly", nullable = true)
	public Poly getPoly() {
		if (getJenis() != null && getJenis().equals(RAWAT_UGD)) {
//			poly = ConstantValues.POLI_UGD;
		}
		poly = check(poly);
		return poly;
	}

	/**
	 * Menyetel tenaga medis penanggung jawab pendaftaran ini.
	 * <p>Kolomnya {@code nullable = false}, sehingga nilai {@code null} akan gagal saat flush.
	 * Pada alur normal nilainya diturunkan dari jadwal dokter yang dipilih petugas.</p>
	 *
	 * @param dokter tenaga medis penanggung jawab.
	 */
	public void setDokter(Dokter dokter) {
		this.dokter = dokter;
	}

	/**
	 * Merangkai <b>alamat lengkap pasien</b> menjadi satu baris teks, dari komponen alamat yang
	 * tersebar di entity {@link Pasien}.
	 *
	 * <h3>Cara kerja</h3>
	 * <p>Method ini bukan accessor JavaBean (namanya tidak berawalan {@code get}/{@code set}),
	 * sehingga Hibernate mengabaikannya sepenuhnya — ia murni method bantu tampilan. Langkahnya:</p>
	 * <ol>
	 *   <li>meresolusi proxy pasien lewat {@code pasien = check(pasien)} — perhatikan bahwa ini
	 *   dilakukan pada field secara langsung, bukan lewat {@link #getPasien()};</li>
	 *   <li>memanggil {@code pasien.getAlamatLengkap()} dan menugaskan hasilnya ke variabel lokal
	 *   {@code alamat};</li>
	 *   <li><b>lalu langsung menimpa</b> variabel itu dengan rangkaian yang disusun sendiri:
	 *   alamat + kelurahan + RT + RW + kecamatan + kota + propinsi, masing-masing dijaga terhadap
	 *   {@code null} dengan mengganti bagian yang kosong menjadi string kosong.</li>
	 * </ol>
	 *
	 * <p>Karena langkah ketiga menimpa hasil langkah kedua, <b>pemanggilan
	 * {@code getAlamatLengkap()} itu sia-sia</b> — nilainya tidak pernah dipakai. Jangan
	 * menghapusnya tanpa memeriksa apakah method tersebut memiliki efek samping di
	 * {@link Pasien} (beberapa getter di model AIS menulis balik ke field), tetapi sadari bahwa
	 * hasil yang dikembalikan method ini <i>selalu</i> berasal dari rangkaian manual, bukan dari
	 * alamat lengkap milik pasien. Kedua bentuk itu bisa berbeda bila {@code getAlamatLengkap()}
	 * memformat alamat dengan cara lain.</p>
	 *
	 * <h3>Penanganan galat: menelan seluruh exception</h3>
	 * <p>Seluruh badan method dibungkus {@code try}/{@code catch (Exception e)}. Blok
	 * {@code catch}-nya hanya mencatat kejadian lewat
	 * {@code ais.common.ErrorAuditUtil.record(...)} — sebuah pencatatan yang disisipkan penyapuan
	 * otomatis ke seluruh blok {@code catch} kosong di basis kode — lalu <b>membiarkan eksekusi
	 * berlanjut</b>. Akibatnya method ini <b>tidak pernah melempar exception</b> dan pada kondisi
	 * galat mengembalikan {@code ""} (string kosong), bukan menandakan kegagalan.</p>
	 *
	 * <p>Kondisi galat yang realistis: pasien {@code null} ({@link NullPointerException} pada
	 * {@code pasien.getAlamatLengkap()}) atau proxy lazy yang tidak dapat diresolusi
	 * ({@code LazyInitializationException}). Pemanggil karena itu tidak dapat membedakan
	 * "pasien memang tidak beralamat" dari "terjadi kesalahan saat membaca alamat" — keduanya
	 * tampak sebagai string kosong. Untuk keperluan tampilan hal ini memadai; untuk keperluan
	 * pencetakan dokumen resmi, periksa lebih dulu bahwa {@link #getPasien()} tidak {@code null}.</p>
	 *
	 * <h3>Bentuk hasil</h3>
	 * <p>Bagian-bagian dirangkai dengan spasi dan label {@code " RT "} serta {@code " RW "} untuk
	 * nomor RT/RW. Karena bagian yang kosong diganti string kosong <i>tanpa</i> menghapus spasi
	 * pemisahnya, hasilnya dapat memuat spasi ganda ketika beberapa komponen alamat kosong —
	 * rapikan dengan {@code replaceAll("\\s+", " ").trim()} bila teks ini dipakai pada dokumen
	 * cetak yang rapat.</p>
	 *
	 * @return alamat pasien dalam satu baris; string kosong bila pasien tidak ada atau terjadi
	 *         galat. Tidak pernah {@code null}, tidak pernah melempar exception.
	 * @see Pasien
	 */
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

	/**
	 * Mengembalikan tenaga medis penanggung jawab pendaftaran ini, setelah meresolusi proxy
	 * lazy-nya lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code dokter}, {@code nullable = false} — setiap pendaftaran wajib menyebut
	 * tenaga medis. Nilainya diturunkan dari {@link JadwalDokter#getDokter()} milik jadwal yang
	 * dipilih petugas, sehingga ada <b>duplikasi yang disengaja</b>: field ini menjadi cuplikan
	 * yang tetap terbaca meskipun jadwal kelak diubah atau dihapus. Bila keduanya perlu
	 * dibandingkan, perlakukan {@code getJadwalDokter().getDokter()} sebagai kondisi terkini dan
	 * field ini sebagai kondisi saat pendaftaran dibuat.</p>
	 *
	 * <p>Ingat bahwa {@link Dokter} adalah master tenaga medis umum (dokter, bidan, perawat,
	 * siswa praktek), dan penanda aktif pada master itu tidak dipakai sebagai filter di mana pun —
	 * sehingga tenaga medis yang sudah dinonaktifkan tetap dapat dipilih di sini.</p>
	 *
	 * @return tenaga medis penanggung jawab.
	 * @see Dokter
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dokter", nullable = false)
	public Dokter getDokter() {
		dokter = check(dokter);
		return dokter;
	}

	/**
	 * Menyetel nomor antrian pendaftaran.
	 * <p>Diisi layar pendaftaran dengan hasil
	 * {@code CommonPendaftaranUtil.generateNomorAntrian(pendaftaran, jadwalDokter)}. Menyetelnya
	 * manual tidak dicegah, tetapi dapat menimbulkan nomor kembar karena pembangkitan berikutnya
	 * berbasis nilai maksimum.</p>
	 *
	 * @param nomorAntrian nomor antrian pada jadwal yang dituju.
	 */
	public void setNomorAntrian(Integer nomorAntrian) {
		this.nomorAntrian = nomorAntrian;
	}

	/**
	 * @return nomor antrian pasien pada jadwal yang dituju (kolom {@code nomor_antrian}), atau
	 *         {@code null} bila belum dibangkitkan.
	 *
	 *         <h3>Dua sumber nomor yang berbeda</h3>
	 *         <p>{@code CommonPendaftaranUtil.generateNomorAntrian(Pendaftaran, JadwalDokter)}
	 *         bercabang dua:</p>
	 *         <ul>
	 *           <li><b>Bila pendaftaran berasal dari booking</b> ({@link #getBookingRegistrasi()}
	 *           tidak {@code null}), nomor <b>diwarisi langsung</b> dari
	 *           {@link BookingRegistrasi#getNomorAntrian()} tanpa perhitungan apa pun. Pasien yang
	 *           sudah membuat janji temu karena itu mempertahankan nomor antriannya.</li>
	 *           <li><b>Bila pasien datang langsung</b>, nomor dihitung dari dua query terpisah pada
	 *           {@link JadwalDokter} yang sama: nilai maksimum {@code nomorAntrian} di antara
	 *           pendaftaran dengan {@code dilayaniTanggal} hari ini, ditambah satu, lalu
	 *           <b>ditambah lagi</b> nilai maksimum {@code nomorAntrian} di antara <i>booking</i>
	 *           pada jadwal itu.</li>
	 *         </ul>
	 *
	 *         <h3>Batasan yang perlu diketahui</h3>
	 *         <p>Pertama, penjumlahan pada cabang kedua memakai rentang tanggal yang <b>tidak
	 *         sama</b>: pendaftaran disaring pada tanggal hari ini, sedangkan booking disaring
	 *         dengan {@code >=} hari ini — yaitu seluruh booking yang akan datang, bukan hanya
	 *         booking untuk hari yang sama. Akibatnya nomor antrian pasien yang datang langsung
	 *         dapat melonjak mengikuti booking untuk tanggal-tanggal berikutnya, sehingga deret
	 *         nomor pada satu hari menjadi tidak berurutan.</p>
	 *         <p>Kedua, pembangkitannya berupa baca-maksimum-lalu-tulis tanpa penguncian baris dan
	 *         tanpa batasan unik pada kolomnya, sehingga dua pendaftaran yang disimpan bersamaan
	 *         pada jadwal yang sama dapat memperoleh nomor yang sama.</p>
	 *         <p>Ketiga, karena {@link JadwalDokter} membolehkan jadwal kembar untuk kombinasi
	 *         dokter+hari+shift yang sama, dua baris jadwal yang secara nyata mewakili praktek yang
	 *         sama memiliki dua deret antrian terpisah yang keduanya dimulai dari 1.</p>
	 *         <p>Keempat, bila dua pendaftaran berhasil menebus satu booking yang sama (penjaganya
	 *         hanya di lapisan tampilan — lihat {@link #getBookingRegistrasi()}), keduanya akan
	 *         mewarisi nomor antrian yang identik lewat cabang pertama.</p>
	 */
	@Column(name = "nomor_antrian", nullable = true)
	public Integer getNomorAntrian() {
		return nomorAntrian;
	}

	/**
	 * Mengembalikan tempat tidur yang ditempati pasien rawat inap, setelah meresolusi proxy
	 * lazy-nya lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code tempat_tidur}, {@code nullable = true} — bermakna hanya untuk
	 * {@link #RAWAT_INAP}. Penempatan dan pemindahan tempat tidur dikelola
	 * {@code PindahTempatTidurRawatInapAction}, yang saat memindahkan pasien menutup pendaftaran
	 * lama dengan status {@link #PINDAH} dan membuat pendaftaran baru yang menunjuk tempat tidur
	 * baru.</p>
	 *
	 * <p><b>Tidak ada penjaga hunian ganda di tingkat entity ini.</b> Tidak ada batasan unik yang
	 * mencegah dua pendaftaran berstatus {@link #TERDAFTAR} menunjuk tempat tidur yang sama, dan
	 * tidak ada pula pemeriksaan konsistensi bahwa tempat tidur ini benar-benar berada di dalam
	 * {@link #getKamarPerawatan()} maupun {@link #getRuangPerawatan()} yang tercatat. Ketiga field
	 * itu disimpan berdampingan sebagai data mandiri, sehingga dapat saling bertentangan bila
	 * disetel lewat jalur yang berbeda.</p>
	 *
	 * @return tempat tidur yang ditempati, atau {@code null}.
	 * @see TempatTidur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tempat_tidur", nullable = true)
	public TempatTidur getTempatTidur() {
		tempatTidur = check(tempatTidur);
		return tempatTidur;
	}

	/**
	 * Menyetel tempat tidur yang ditempati pasien rawat inap.
	 * <p>Setter ini tidak memeriksa apakah tempat tidur tersebut sedang ditempati pendaftaran lain,
	 * dan tidak menyelaraskan {@link #setKamarPerawatan(Kamar)} maupun
	 * {@link #setRuangPerawatan(Ruang)} — lihat catatan pada {@link #getTempatTidur()}.</p>
	 *
	 * @param tempatTidur tempat tidur; {@code null} untuk pendaftaran non-rawat-inap.
	 */
	public void setTempatTidur(TempatTidur tempatTidur) {
		this.tempatTidur = tempatTidur;
	}

	/**
	 * @return cara atau penanggung biaya perawatan sebagai <b>teks bebas</b> (kolom
	 *         {@code biaya_perawatan}), atau {@code null}.
	 *         <p>Perhatikan tipenya {@link String}, bukan angka: field ini <b>bukan</b> nominal
	 *         biaya melainkan keterangan cara pembiayaan yang diketik petugas. Nominal biaya yang
	 *         sesungguhnya berada di {@link #getPembayaran()} dan pada baris-baris
	 *         {@code DetailTransaksiLayanan}. Jangan mencoba menjumlahkan atau mengurutkan field
	 *         ini secara numerik.</p>
	 */
	@Column(name = "biaya_perawatan", nullable = true)
	public String getBiayaPerawatan() {
		return biayaPerawatan;
	}

	/**
	 * Menyetel keterangan cara/penanggung biaya perawatan.
	 *
	 * @param biayaPerawatan teks bebas; boleh {@code null}.
	 */
	public void setBiayaPerawatan(String biayaPerawatan) {
		this.biayaPerawatan = biayaPerawatan;
	}

	/**
	 * @return nama penjamin pasien rawat inap (kolom {@code nama_penjamin}), atau {@code null}.
	 *         <p>Ini bagian dari blok data penjamin — pihak yang menjamin pembiayaan pasien —
	 *         bersama {@link #getAlamatPenjamin()}, {@link #getPekerjaanPenjamin()}, dan
	 *         {@link #getPendidikanPenjamin()}. Bedakan dari {@link #getAsuransi()} yang merupakan
	 *         relasi ke master penjamin korporat, dan dari blok {@code *Pendaftar} yang mencatat
	 *         orang yang mengantar pasien. Blok ini berisi <b>data pribadi pihak ketiga</b>,
	 *         sehingga laporan dan ekspor yang memuatnya perlu memperhatikan pembatasan akses.</p>
	 */
	@Column(name = "nama_penjamin", nullable = true)
	public String getNamaPenjamin() {
		return namaPenjamin;
	}

	/**
	 * Menyetel nama penjamin pasien rawat inap.
	 *
	 * @param namaPenjamin nama penjamin; boleh {@code null}.
	 */
	public void setNamaPenjamin(String namaPenjamin) {
		this.namaPenjamin = namaPenjamin;
	}

	/**
	 * @return alamat penjamin pasien rawat inap (kolom {@code alamat_penjamin}), atau {@code null}.
	 *         Bagian dari blok data penjamin; lihat {@link #getNamaPenjamin()}.
	 */
	@Column(name = "alamat_penjamin", nullable = true)
	public String getAlamatPenjamin() {
		return alamatPenjamin;
	}

	/**
	 * Menyetel alamat penjamin pasien rawat inap.
	 *
	 * @param alamatPenjamin alamat penjamin; boleh {@code null}.
	 */
	public void setAlamatPenjamin(String alamatPenjamin) {
		this.alamatPenjamin = alamatPenjamin;
	}

	/**
	 * @return pekerjaan penjamin pasien rawat inap (kolom {@code pekerjaan_penjamin}) sebagai teks
	 *         bebas, atau {@code null}. Bagian dari blok data penjamin; lihat
	 *         {@link #getNamaPenjamin()}.
	 *         <p>Perhatikan ketidakseragaman: pekerjaan disimpan sebagai teks bebas, sedangkan
	 *         pendidikan penjamin ({@link #getPendidikanPenjamin()}) memakai relasi ke master.</p>
	 */
	@Column(name = "pekerjaan_penjamin", nullable = true)
	public String getPekerjaanPenjamin() {
		return pekerjaanPenjamin;
	}

	/**
	 * Menyetel pekerjaan penjamin pasien rawat inap.
	 *
	 * @param pekerjaanPenjamin teks bebas; boleh {@code null}.
	 */
	public void setPekerjaanPenjamin(String pekerjaanPenjamin) {
		this.pekerjaanPenjamin = pekerjaanPenjamin;
	}

	/**
	 * Mengembalikan pendidikan terakhir penjamin, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Satu-satunya relasi pada entity ini yang menunjuk ke luar paket SIRS menuju modul
	 * kepegawaian ({@code ais.database.model.employ.Pendidikan}), yang berfungsi sebagai master
	 * jenjang pendidikan bersama. Kolomnya {@code pendidikan_penjamin}, {@code nullable = true}.
	 * Bagian dari blok data penjamin; lihat {@link #getNamaPenjamin()}.</p>
	 *
	 * @return pendidikan terakhir penjamin, atau {@code null}.
	 * @see ais.database.model.employ.Pendidikan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_penjamin", nullable = true)
	public Pendidikan getPendidikanPenjamin() {
		pendidikanPenjamin = check(pendidikanPenjamin);
		return pendidikanPenjamin;
	}

	/**
	 * Menyetel pendidikan terakhir penjamin.
	 *
	 * @param pendidikanPenjamin jenjang pendidikan dari master; boleh {@code null}.
	 */
	public void setPendidikanPenjamin(Pendidikan pendidikanPenjamin) {
		this.pendidikanPenjamin = pendidikanPenjamin;
	}

	/**
	 * @return asal pasien rawat inap (kolom {@code sumber_pasien}) sebagai teks bebas, atau
	 *         {@code null}.
	 *         <p>Pada data yang lahir dari {@code PendaftaranRawatInapAction} nilainya berupa
	 *         salah satu konstanta {@code SUMBER_PASIEN_*}: {@link #SUMBER_PASIEN_POLI},
	 *         {@link #SUMBER_PASIEN_UGD}, {@link #SUMBER_PASIEN_DARI_RS},
	 *         {@link #SUMBER_PASIEN_DARI_TAMU}, atau {@link #SUMBER_PASIEN_LUAR_DKI}. Pembatasan
	 *         itu hanya berlaku di lapisan UI — {@link #setSumberPasien(String)} menerima string
	 *         apa pun.</p>
	 *         <p>Ingat bahwa nilai {@link #SUMBER_PASIEN_UGD} identik dengan {@link #RAWAT_UGD}
	 *         meski keduanya menempati kolom berbeda.</p>
	 */
	@Column(name = "sumber_pasien", nullable = true)
	public String getSumberPasien() {
		return sumberPasien;
	}

	/**
	 * Menyetel asal pasien rawat inap. <b>Tanpa validasi</b> terhadap konstanta
	 * {@code SUMBER_PASIEN_*}.
	 *
	 * @param sumberPasien asal pasien; idealnya salah satu konstanta {@code SUMBER_PASIEN_*}.
	 */
	public void setSumberPasien(String sumberPasien) {
		this.sumberPasien = sumberPasien;
	}

	/**
	 * @return keterangan tempat pasien pernah dirawat sebelumnya (kolom
	 *         {@code pernah_dirawat_di}) sebagai teks bebas, atau {@code null}.
	 *         <p>Ini riwayat yang diketik petugas berdasarkan keterangan pasien, <b>bukan</b>
	 *         tautan ke pendaftaran sebelumnya di sistem ini. Untuk menelusuri riwayat kunjungan
	 *         internal, kueri {@code Pendaftaran} berdasarkan pasiennya; untuk menelusuri rantai
	 *         pemindahan, pakai {@link #getTransferDaripendaftaran()}.</p>
	 */
	@Column(name = "pernah_dirawat_di", nullable = true)
	public String getPernahDirawatDi() {
		return pernahDirawatDi;
	}

	/**
	 * Menyetel keterangan tempat pasien pernah dirawat sebelumnya.
	 *
	 * @param pernahDirawatDi teks bebas; boleh {@code null}.
	 */
	public void setPernahDirawatDi(String pernahDirawatDi) {
		this.pernahDirawatDi = pernahDirawatDi;
	}

	/**
	 * @return tanggal pasien pernah dirawat sebelumnya (kolom {@code tanggal_pernah_dirawat}),
	 *         atau {@code null}.
	 *         <p>Dipetakan {@link TemporalType#TIMESTAMP} sehingga menyimpan jam juga, padahal
	 *         yang bermakna hanyalah tanggalnya — komponen jam pada field ini tidak memiliki arti
	 *         dan hanya akan berisi sisa dari nilai yang disetel komponen tanggal di UI.
	 *         Pertimbangkan hal ini saat membandingkan atau mengelompokkan berdasarkan field
	 *         ini.</p>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pernah_dirawat", nullable = true)
	public Date getTanggalPernahDirawat() {
		return tanggalPernahDirawat;
	}

	/**
	 * Menyetel tanggal pasien pernah dirawat sebelumnya.
	 *
	 * @param tanggalPernahDirawat tanggal riwayat perawatan; boleh {@code null}.
	 */
	public void setTanggalPernahDirawat(Date tanggalPernahDirawat) {
		this.tanggalPernahDirawat = tanggalPernahDirawat;
	}

	/**
	 * @return nama dokter perujuk (kolom {@code nama_dokter_pengirim}) sebagai teks bebas, atau
	 *         {@code null}.
	 *         <p><b>Sengaja bukan relasi</b> ke {@link Dokter}: dokter perujuk umumnya berasal dari
	 *         luar rumah sakit sehingga tidak ada di master tenaga medis. Karena disimpan sebagai
	 *         teks, nama yang sama dapat tertulis dengan berbagai ejaan dan tidak dapat
	 *         dikelompokkan secara andal untuk laporan rujukan.</p>
	 */
	@Column(name = "nama_dokter_pengirim", nullable = true)
	public String getNamaDokterPengirim() {
		return namaDokterPengirim;
	}

	/**
	 * Menyetel nama dokter perujuk.
	 *
	 * @param namaDokterPengirim teks bebas; boleh {@code null}.
	 */
	public void setNamaDokterPengirim(String namaDokterPengirim) {
		this.namaDokterPengirim = namaDokterPengirim;
	}

	/**
	 * @return isi kolom {@code pendaftar} sebagai teks bebas, atau {@code null}.
	 *
	 *         <p><b>Waspadai kemiripan nama.</b> Field ini berdiri terpisah dari trio
	 *         {@link #getNamaPendaftar()}, {@link #getAlamatPendaftar()}, dan
	 *         {@link #getTelpPendaftar()} yang mencatat identitas pengantar pasien, dan juga
	 *         terpisah dari {@link #getTbmuser()} yang mencatat petugas rumah sakit. Ketiga
	 *         "pendaftar" itu mudah tertukar saat menulis laporan.</p>
	 *
	 *         <p>Ketiga layar pendaftaran tidak menyediakan input khusus untuk kolom ini, sehingga
	 *         pada data yang lahir dari alur normal nilainya umumnya {@code null}. Perlakukan
	 *         sebagai field peninggalan sampai ada pemakai yang jelas — dan bila memerlukan
	 *         identitas pengantar pasien, pakai {@link #getNamaPendaftar()}.</p>
	 */
	@Column(name = "pendaftar", nullable = true)
	public String getPendaftar() {
		return pendaftar;
	}

	/**
	 * Menyetel isi kolom {@code pendaftar}. Lihat catatan pada {@link #getPendaftar()} mengenai
	 * kemiripan namanya dengan field lain.
	 *
	 * @param pendaftar teks bebas; boleh {@code null}.
	 */
	public void setPendaftar(String pendaftar) {
		this.pendaftar = pendaftar;
	}

	/**
	 * @return alamat orang yang mendaftarkan/mengantar pasien (kolom {@code alamat_pendaftar}),
	 *         atau {@code null}. Bagian dari blok data pengantar bersama
	 *         {@link #getNamaPendaftar()} dan {@link #getTelpPendaftar()}; berisi <b>data pribadi
	 *         pihak ketiga</b> sehingga perlu diperhatikan pada laporan dan ekspor.
	 */
	@Column(name = "alamat_pendaftar", nullable = true)
	public String getAlamatPendaftar() {
		return alamatPendaftar;
	}

	/**
	 * Menyetel alamat orang yang mendaftarkan/mengantar pasien.
	 *
	 * @param alamatPendaftar alamat pengantar; boleh {@code null}.
	 */
	public void setAlamatPendaftar(String alamatPendaftar) {
		this.alamatPendaftar = alamatPendaftar;
	}

	/**
	 * @return nomor telepon orang yang mendaftarkan/mengantar pasien (kolom
	 *         {@code telp_pendaftar}), atau {@code null}. Dalam praktik inilah kontak darurat yang
	 *         dihubungi rumah sakit, sehingga kekosongannya berdampak operasional. Bagian dari blok
	 *         data pengantar; lihat {@link #getAlamatPendaftar()}.
	 */
	@Column(name = "telp_pendaftar", nullable = true)
	public String getTelpPendaftar() {
		return telpPendaftar;
	}

	/**
	 * Menyetel nomor telepon orang yang mendaftarkan/mengantar pasien.
	 *
	 * @param telpPendaftar nomor telepon; boleh {@code null}.
	 */
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

	/**
	 * @return waktu pasien keluar dari perawatan (kolom {@code tanggal_keluar},
	 *         {@link TemporalType#TIMESTAMP}), atau {@code null} bila pasien masih dalam
	 *         perawatan.
	 *         <p>Diisi bersamaan dengan perubahan {@link #getStatusPendaftaran()} menjadi
	 *         {@link #KELUAR} atau {@link #MENINGGAL} oleh {@code DataPasienKeluarAction}.
	 *         Perlu dicatat bahwa <b>tidak ada penjaga di tingkat entity yang menjaga kedua field
	 *         itu tetap konsisten</b>: pendaftaran berstatus {@link #TERDAFTAR} secara teknis dapat
	 *         memiliki tanggal keluar terisi, dan sebaliknya. Bila lama rawat perlu dihitung,
	 *         periksa keduanya, bukan salah satu saja.</p>
	 *         <p>Berbeda dari {@link #getTanggalPernahDirawat()}, komponen jam pada field ini
	 *         bermakna — lama rawat inap dihitung dari selisihnya terhadap
	 *         {@link #getTanggalPendaftaran()}.</p>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_keluar", nullable = true)
	public Date getTanggalKeluar() {
		return tanggalKeluar;
	}

	/**
	 * Menyetel waktu pasien keluar dari perawatan.
	 * <p>Setter ini tidak mengubah {@link #setStatusPendaftaran(String)}; keduanya harus disetel
	 * bersamaan oleh pemanggil.</p>
	 *
	 * @param tanggalKeluar waktu pasien keluar; {@code null} bila masih dirawat.
	 */
	public void setTanggalKeluar(Date tanggalKeluar) {
		this.tanggalKeluar = tanggalKeluar;
	}

	/**
	 * Mengembalikan penjamin/asuransi yang dipakai pada pendaftaran ini, setelah meresolusi proxy
	 * lazy-nya lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code asuransi}, {@code nullable = true} — {@code null} berarti pasien
	 * umum/bayar sendiri. Berbeda dari blok data penjamin bertipe teks
	 * ({@link #getNamaPenjamin()} dan kerabatnya) yang mencatat penjamin perorangan, relasi ini
	 * menunjuk master penjamin korporat.</p>
	 *
	 * <p>Nilai ini <b>tidak disalin otomatis</b> dari {@link BookingRegistrasi#getAsuransi()} saat
	 * booking ditebus; petugas menentukannya ulang pada layar pendaftaran. Bersama
	 * {@link #getKomunitass()}, field ini merupakan salah satu dari sedikit data pada pendaftaran
	 * yang benar-benar bersifat historis — tidak ditimpa oleh nilai master seperti yang terjadi
	 * pada {@link #getJenisPasien()}.</p>
	 *
	 * @return penjamin/asuransi, atau {@code null}.
	 * @see Asuransi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi", nullable = true)
	public Asuransi getAsuransi() {
		asuransi = check(asuransi);
		return asuransi;
	}

	/**
	 * Menyetel penjamin/asuransi pendaftaran ini.
	 *
	 * @param asuransi penjamin korporat; {@code null} berarti pasien umum.
	 */
	public void setAsuransi(Asuransi asuransi) {
		this.asuransi = asuransi;
	}

	/**
	 * Mengembalikan kelas perawatan <b>tujuan pemindahan</b>, setelah meresolusi proxy lazy-nya
	 * lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code pindah_ke_kelas_perawatan}, {@code nullable = true}. Jangan dikacaukan
	 * dengan {@link #getKelasPerawatan()} yang merupakan kelas perawatan <i>saat ini</i>: field ini
	 * mencatat ke kelas mana pasien akan/telah dipindahkan, dan hanya bermakna pada alur
	 * {@code PindahTempatTidurRawatInapAction} yang menutup pendaftaran ini dengan status
	 * {@link #PINDAH}.</p>
	 *
	 * <p>Perhatikan bahwa arah penelusuran rantai pemindahan berbeda antara kedua field: field ini
	 * menunjuk <i>kelas</i> tujuan (bukan pendaftaran tujuan), sedangkan
	 * {@link #getTransferDaripendaftaran()} pada pendaftaran <i>berikutnya</i> yang menunjuk
	 * mundur ke pendaftaran ini. Tidak ada relasi maju dari pendaftaran lama ke pendaftaran baru;
	 * untuk menemukannya, kueri {@code Pendaftaran} berdasarkan
	 * {@code transferDaripendaftaran}.</p>
	 *
	 * @return kelas perawatan tujuan pemindahan, atau {@code null}.
	 * @see KelasPerawatan
	 * @see #PINDAH
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pindah_ke_kelas_perawatan", nullable = true)
	public KelasPerawatan getPindahKeKelasPerawatan() {
		pindahKeKelasPerawatan = check(pindahKeKelasPerawatan);
		return pindahKeKelasPerawatan;
	}

	/**
	 * Menyetel kelas perawatan tujuan pemindahan.
	 *
	 * @param pindahKeKelasPerawatan kelas perawatan tujuan; {@code null} bila tidak ada pemindahan.
	 */
	public void setPindahKeKelasPerawatan(KelasPerawatan pindahKeKelasPerawatan) {
		this.pindahKeKelasPerawatan = pindahKeKelasPerawatan;
	}

	/**
	 * Menyetel umur pasien.
	 * <p>Nilai yang disetel di sini <b>hanya bertahan selama pendaftaran belum menunjuk pasien</b>
	 * — lihat penjelasan pada {@link #getUmur()}. Untuk pendaftaran dengan pasien terisi, setter
	 * ini praktis tanpa efek.</p>
	 *
	 * @param umur umur pasien dalam tahun.
	 */
	public void setUmur(Integer umur) {
		this.umur = umur;
	}

	/**
	 * Mengembalikan umur pasien dalam tahun, dengan <b>selalu menyalin ulang dari master
	 * pasien</b> dan menulis balik ke field.
	 *
	 * <h3>Perilaku</h3>
	 * <p>Bila {@link #getPasien()} tidak {@code null}, field {@link #umur} ditimpa dengan
	 * {@code getPasien().getUmur()} — nilai tersimpan diabaikan. Setelah itu, bila hasilnya masih
	 * {@code null}, field diisi {@code 0}. Jadi method ini tidak pernah mengembalikan
	 * {@code null}, dan {@link #setUmur(Integer)} praktis tanpa efek untuk pendaftaran yang sudah
	 * menunjuk pasien.</p>
	 *
	 * <h3>Konsekuensi: umur pada pendaftaran bukan data historis</h3>
	 * <p>Ini masalah yang sama dengan {@link #getJenisPasien()}, tetapi dampaknya lebih terasa
	 * karena umur berubah setiap tahun secara alami. {@code Pasien.getUmur()} menghitung umur
	 * <b>terkini</b> dari tanggal lahir, sehingga:</p>
	 * <ul>
	 *   <li>pendaftaran seorang bayi lima tahun lalu, bila dibaca hari ini, akan menampilkan umur
	 *   lima tahun — bukan umur pasien saat pendaftaran itu dibuat;</li>
	 *   <li>karena Hibernate mengakses entity ini lewat properti, nilai baru itu ikut
	 *   <b>ter-flush ke kolom {@code umur}</b> dan menghasilkan baris revisi Envers, sehingga
	 *   data historis yang mungkin masih tersimpan di kolom itu <i>terhapus</i> begitu barisnya
	 *   dibaca;</li>
	 *   <li>laporan epidemiologi atau statistik kunjungan per kelompok umur yang membaca field ini
	 *   akan menghasilkan angka yang bergeser seiring waktu, bukan angka pada saat kunjungan.
	 *   Untuk kebutuhan seperti itu, hitung sendiri dari
	 *   {@code getPasien().getTanggalLahir()} terhadap {@link #getTanggalPendaftaran()}.</li>
	 * </ul>
	 *
	 * <p>Nilai cadangan {@code 0} juga perlu diwaspadai: ia tidak dapat dibedakan dari bayi berusia
	 * kurang dari satu tahun. Jangan memakai {@code umur == 0} sebagai penanda "data tidak
	 * tersedia".</p>
	 *
	 * <p>Getter ini tidak beranotasi {@link Column @Column}; dipetakan berdasarkan konvensi.</p>
	 *
	 * @return umur pasien dalam tahun menurut kondisi saat pembacaan; tidak pernah {@code null}.
	 */
	public Integer getUmur() {
		if (getPasien() != null) {
			umur = getPasien().getUmur();
		}
		if (umur == null) {
			umur = 0;
		}
		return umur;
	}

	/**
	 * Menyetel nama orang yang mendaftarkan/mengantar pasien.
	 *
	 * @param namaPendaftar nama pengantar; boleh {@code null}.
	 */
	public void setNamaPendaftar(String namaPendaftar) {
		this.namaPendaftar = namaPendaftar;
	}

	/**
	 * @return nama orang yang mendaftarkan/mengantar pasien (kolom {@code nama_pendaftar}), atau
	 *         {@code null}.
	 *         <p>Bagian dari blok data pengantar bersama {@link #getAlamatPendaftar()} dan
	 *         {@link #getTelpPendaftar()}. Bedakan dari {@link #getTbmuser()} (petugas rumah sakit
	 *         yang mengetikkan pendaftaran), dari {@link #getOleh()} (jejak audit pengubah
	 *         terakhir), dan dari {@link #getPendaftar()} (kolom teks terpisah yang jarang
	 *         dipakai).</p>
	 */
	@Column(name = "nama_pendaftar", nullable = true)
	public String getNamaPendaftar() {
		return namaPendaftar;
	}

	/**
	 * Menyetel kelas perawatan pasien.
	 * <p>Menyetel {@code null} tidak akan bertahan: pembacaan berikutnya lewat
	 * {@link #getKelasPerawatan()} akan menggantinya dengan {@code ConstantValues.kelasNormal}.</p>
	 *
	 * @param kelasPerawatan kelas perawatan pasien.
	 */
	public void setKelasPerawatan(KelasPerawatan kelasPerawatan) {
		this.kelasPerawatan = kelasPerawatan;
	}

	/**
	 * Mengembalikan kelas perawatan pasien, dengan <b>mengisi default dan menulis balik</b> ke
	 * field.
	 *
	 * <p>Bila field masih {@code null}, diisi {@code ConstantValues.kelasNormal} — kelas perawatan
	 * default seluruh aplikasi — lalu hasilnya dilewatkan {@code check(...)} untuk meresolusi
	 * proxy lazy dan ditugaskan kembali ke field. Persis sama dengan
	 * {@link BookingRegistrasi#getKelasPerawatan()}.</p>
	 *
	 * <p>Karena Hibernate mengakses lewat properti, pengisian default ini ikut ter-flush: baris
	 * pendaftaran lama yang kolom {@code kelas_perawatan}-nya {@code NULL} akan ter-UPDATE menjadi
	 * kelas normal pada flush pertama setelah dibaca, tanpa perintah pengguna, dan menghasilkan
	 * baris revisi Envers. Informasi "kelas perawatan belum ditentukan" karena itu hilang setelah
	 * pembacaan pertama.</p>
	 *
	 * <p><b>Waspadai {@code ConstantValues.kelasNormal} yang dapat bernilai {@code null}</b> bila
	 * cache konstanta belum terisi saat inisialisasi aplikasi; nilai kembalian method ini karena
	 * itu tidak dijamin bukan-{@code null} meskipun ada cabang default.</p>
	 *
	 * <p>Nilai ini penting bagi perhitungan biaya: {@code RawatInapCalculationProcessor} membacanya
	 * lewat rantai {@code kunjunganDokter.getDiagnosaPenyakit().getPendaftaran().getKelasPerawatan()}
	 * untuk menentukan tarif tindakan, dengan {@code ConstantValues.kelasNormal} sebagai cadangan
	 * bila hasilnya {@code null}.</p>
	 *
	 * @return kelas perawatan pasien; biasanya tidak {@code null}, tetapi lihat peringatan di atas.
	 * @see KelasPerawatan
	 * @see BookingRegistrasi#getKelasPerawatan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_perawatan", nullable = true)
	public KelasPerawatan getKelasPerawatan() {
		if (kelasPerawatan == null) {
			kelasPerawatan = ConstantValues.kelasNormal;
		}
		kelasPerawatan = check(kelasPerawatan);
		return kelasPerawatan;
	}

	/**
	 * Menyetel ruang perawatan pasien rawat inap.
	 * <p>Tidak menyelaraskan {@link #setKamarPerawatan(Kamar)} maupun
	 * {@link #setTempatTidur(TempatTidur)}; ketiganya disimpan sebagai data mandiri.</p>
	 *
	 * @param ruangPerawatan ruang perawatan; {@code null} untuk pendaftaran non-rawat-inap.
	 */
	public void setRuangPerawatan(Ruang ruangPerawatan) {
		this.ruangPerawatan = ruangPerawatan;
	}

	/**
	 * Mengembalikan ruang perawatan pasien rawat inap, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Kolomnya {@code ruang_perawatan}, {@code nullable = true}. Bersama
	 * {@link #getKamarPerawatan()} dan {@link #getTempatTidur()}, field ini membentuk trio
	 * penempatan ruang &rarr; kamar &rarr; tempat tidur yang secara logika berjenjang tetapi
	 * <b>disimpan sebagai tiga relasi terpisah tanpa penjaga konsistensi</b>. Tidak ada apa pun
	 * pada entity ini yang memastikan tempat tidur yang tercatat memang berada di kamar yang
	 * tercatat, ataupun kamar itu berada di ruang yang tercatat — ketiganya dapat saling
	 * bertentangan bila disetel lewat jalur berbeda. Kode yang menampilkan lokasi pasien sebaiknya
	 * memilih satu tingkat sebagai sumber kebenaran (biasanya tempat tidur) dan menurunkan sisanya
	 * dari sana, bukan membaca ketiganya secara terpisah.</p>
	 *
	 * <p>Perhatikan bahwa {@link Ruang} berasal dari paket {@code ais.database.model}, bukan dari
	 * paket SIRS — ia master ruangan bersama lintas modul.</p>
	 *
	 * @return ruang perawatan, atau {@code null}.
	 * @see ais.database.model.Ruang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_perawatan", nullable = true)
	public Ruang getRuangPerawatan() {
		ruangPerawatan = check(ruangPerawatan);
		return ruangPerawatan;
	}

	/**
	 * Menyetel kamar perawatan pasien rawat inap.
	 * <p>Tidak menyelaraskan {@link #setRuangPerawatan(Ruang)} maupun
	 * {@link #setTempatTidur(TempatTidur)}.</p>
	 *
	 * @param kamarPerawatan kamar perawatan; {@code null} untuk pendaftaran non-rawat-inap.
	 */
	public void setKamarPerawatan(Kamar kamarPerawatan) {
		this.kamarPerawatan = kamarPerawatan;
	}

	/**
	 * Mengembalikan kamar perawatan pasien rawat inap, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Kolomnya {@code kamar_perawatan}, {@code nullable = true}. Tingkat tengah pada trio
	 * penempatan; lihat catatan konsistensi pada {@link #getRuangPerawatan()}.</p>
	 *
	 * @return kamar perawatan, atau {@code null}.
	 * @see Kamar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar_perawatan", nullable = true)
	public Kamar getKamarPerawatan() {
		kamarPerawatan = check(kamarPerawatan);
		return kamarPerawatan;
	}

	/**
	 * Menautkan dokumen data pasien keluar ke pendaftaran ini.
	 * <p>Dipanggil oleh {@code DataPasienKeluarAction} dan
	 * {@code PindahTempatTidurRawatInapAction}. Setter ini tidak mengubah
	 * {@link #setStatusPendaftaran(String)} maupun {@link #setTanggalKeluar(Date)}; ketiganya
	 * disetel terpisah oleh pemanggil.</p>
	 *
	 * @param dataPasienKeluar dokumen data pasien keluar; {@code null} bila belum ada.
	 */
	public void setDataPasienKeluar(DataPasienKeluar dataPasienKeluar) {
		this.dataPasienKeluar = dataPasienKeluar;
	}

	/**
	 * Mengembalikan dokumen data pasien keluar yang terkait pendaftaran ini, atau {@code null}
	 * bila pasien belum keluar.
	 *
	 * <p>Kolomnya {@code data_pasien_keluar}, {@code nullable = true}. Dokumen ini memuat rincian
	 * kepulangan (kondisi keluar, cara pulang, dan sejenisnya) yang tidak muat ditampung pada
	 * baris pendaftaran, dan berdampingan dengan {@link #getStatusPendaftaran()} yang menyimpan
	 * ringkasan statusnya ({@link #KELUAR} atau {@link #MENINGGAL}) serta
	 * {@link #getTanggalKeluar()} yang menyimpan waktunya. Ketiganya <b>tidak dijaga konsisten</b>
	 * oleh entity ini.</p>
	 *
	 * <p>Relasi tidak menyatakan {@code fetch = LAZY}, sehingga memakai default
	 * {@link FetchType#EAGER} diperkuat
	 * {@link Fetch @Fetch}{@code (}{@link FetchMode#SELECT}{@code )} — itu sebabnya getter ini
	 * tidak memanggil {@code check(...)}, dengan konsekuensi satu SELECT tambahan pada setiap
	 * pemuatan pendaftaran.</p>
	 *
	 * @return dokumen data pasien keluar, atau {@code null}.
	 * @see DataPasienKeluar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "data_pasien_keluar", nullable = true)
	public DataPasienKeluar getDataPasienKeluar() {
		return dataPasienKeluar;
	}

	/**
	 * Menyetel unit/cabang tempat pendaftaran dibuat.
	 *
	 * @param lokasi unit/cabang; boleh {@code null} pada tingkat pemetaan.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan unit/cabang tempat pendaftaran dibuat, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Lokasi berperan sebagai <b>pembatas tenant</b> pada modul SIRS: ia menjadi segmen pada
	 * pembangkitan {@link #getKode()} dan pada penomoran {@link #getIndex()} lewat
	 * {@code Common.generateMaxByLokasi(Pendaftaran.class, lokasi)}. Kolomnya sendiri dipetakan
	 * {@code nullable = true}, sehingga pendaftaran tanpa lokasi secara teknis dapat tersimpan —
	 * dan baris seperti itu akan lolos dari penyaringan berbasis lokasi di layar mana pun yang
	 * memakainya. Kode baru yang mengandalkan lokasi untuk pembatasan akses harus menangani
	 * kemungkinan {@code null} secara gagal-tertutup, bukan mengabaikannya.</p>
	 *
	 * <p>Perhatikan pula bahwa <b>tidak ada penjaga yang memastikan lokasi pendaftaran sama dengan
	 * lokasi jadwal dokter yang dipilih</b> ({@link JadwalDokter#getLokasi()}), sehingga kedua
	 * nilai dapat berbeda.</p>
	 *
	 * @return unit/cabang tempat pendaftaran dibuat, atau {@code null}.
	 * @see ais.database.model.asset.Lokasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menyetel nomor urut pendaftaran di dalam lokasinya.
	 * <p>Pada alur normal hanya dipanggil sekali saat penyimpanan pertama; mengubahnya kemudian
	 * dapat menimbulkan nomor urut kembar karena pembangkitannya berbasis nilai maksimum.</p>
	 *
	 * @param index nomor urut per lokasi.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * @return nomor urut pendaftaran di dalam lokasinya, atau {@code null} bila belum tersimpan.
	 *         <p>Diisi sekali pada penyimpanan pertama dengan
	 *         {@code Common.generateMaxByLokasi(Pendaftaran.class, lokasi) + 1}. Karena
	 *         pembangkitannya berupa baca-lalu-tulis tanpa penguncian dan tanpa batasan unik pada
	 *         kolomnya, dua pendaftaran yang disimpan bersamaan pada lokasi yang sama dapat
	 *         memperoleh nomor urut yang sama. Jangan memakainya sebagai pengenal unik — gunakan
	 *         {@link #getKode()} atau {@link #getId()}.</p>
	 *         <p>Getter ini tidak beranotasi {@link Column @Column}. Perhatikan bahwa
	 *         {@code index} adalah kata kunci pada beberapa dialek SQL, sehingga nama kolomnya
	 *         biasanya perlu dikutip oleh dialek Hibernate yang dipakai.</p>
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Menyetel shift pelayanan pendaftaran ini.
	 *
	 * @param shift shift pelayanan; boleh {@code null}.
	 */
	public void setShift(Shift shift) {
		this.shift = shift;
	}

	/**
	 * Mengembalikan shift pelayanan, setelah meresolusi proxy lazy-nya lewat {@code check(...)}.
	 *
	 * <p>Kolomnya {@code shift}, {@code nullable = true}. Seperti {@link #getDokter()} dan
	 * {@link #getPoly()}, ini cuplikan dari {@link JadwalDokter#getShift()} milik jadwal terpilih
	 * dan dapat berbeda dari nilai jadwal terkini bila jadwalnya kemudian diubah.</p>
	 *
	 * @return shift pelayanan, atau {@code null}.
	 * @see Shift
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "shift", nullable = true)
	public Shift getShift() {
		shift = check(shift);
		return shift;
	}

	/**
	 * Menautkan dokumen pembayaran ke pendaftaran ini.
	 * <p>Setter ini tidak memperbarui {@link #getLunas()} secara langsung — penanda itu dihitung
	 * ulang pada setiap pembacaan.</p>
	 *
	 * @param pembayaran dokumen pembayaran; boleh {@code null}.
	 */
	public void setPembayaran(Pembayaran pembayaran) {
		this.pembayaran = pembayaran;
	}

	/**
	 * Mengembalikan dokumen pembayaran yang terkait pendaftaran ini, atau {@code null} bila belum
	 * ada penagihan.
	 *
	 * <p>Kolomnya {@code pembayaran}, {@code nullable = true}. Relasi tidak menyatakan
	 * {@code fetch = LAZY} sehingga memakai default {@link FetchType#EAGER} diperkuat
	 * {@link Fetch @Fetch}{@code (}{@link FetchMode#SELECT}{@code )} — itu sebabnya getter ini
	 * tidak memanggil {@code check(...)}, dan itu pula yang membuat {@link #getLunas()} aman
	 * membaca field {@link #pembayaran} secara langsung.</p>
	 *
	 * @return dokumen pembayaran, atau {@code null}.
	 * @see Pembayaran
	 * @see #getLunas()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran", nullable = true)
	public Pembayaran getPembayaran() {
		return pembayaran;
	}

	/**
	 * Menyetel penanda lunas — <b>praktis tanpa efek</b>, karena {@link #getLunas()} selalu
	 * menghitung ulang dari {@link #getPembayaran()}.
	 *
	 * @param lunas nilai yang akan segera tertimpa.
	 */
	public void setLunas(Boolean lunas) {
		this.lunas = lunas;
	}

	/**
	 * Mengembalikan penanda apakah tagihan pendaftaran ini sudah lunas — <b>selalu dihitung ulang
	 * dan ditulis balik</b> ke field.
	 *
	 * <h3>Rumus</h3>
	 * <p>Lunas bila dokumen pembayaran ada <i>dan</i> total biayanya tidak melebihi jumlah
	 * pembayaran tunai ditambah non-tunai:</p>
	 * <pre>{@code
	 * lunas = pembayaran != null
	 *         && pembayaran.getTotalBiaya() <= (pembayaran.getBayarTunai() + pembayaran.getBayarNonTunai());
	 * }</pre>
	 * <p>Perhatikan operator {@code <=}, bukan {@code ==}: pembayaran berlebih (misalnya karena
	 * uang muka atau kelebihan transfer) tetap dianggap lunas. Pendaftaran <b>tanpa</b> dokumen
	 * pembayaran selalu dianggap <b>belum lunas</b>, bukan "tidak relevan" — sehingga pendaftaran
	 * yang memang tidak menimbulkan tagihan pun akan tampil sebagai "BELUM LUNAS" pada dasbor.</p>
	 *
	 * <h3>Nilai turunan yang ditulis ke kolom</h3>
	 * <p>Hasil perhitungan ditugaskan ke field {@link #lunas}, dan karena property ini tidak diberi
	 * {@link javax.persistence.Transient @Transient} ia <b>ikut dipetakan ke kolom basis data</b>.
	 * Akibatnya {@link #setLunas(Boolean)} praktis tanpa efek, dan kolom {@code lunas} berisi data
	 * turunan yang di-denormalisasi — nilainya diperbarui setiap kali baris ini dibaca lalu
	 * di-flush. Jangan memakai kolom itu sebagai sumber kebenaran pada query SQL langsung; hitung
	 * dari dokumen pembayaran.</p>
	 *
	 * <h3>Risiko {@link NullPointerException}</h3>
	 * <p>Ketiga nilai yang dibandingkan berasal dari {@link Pembayaran} dan bertipe pembungkus.
	 * Ekspresi di atas melakukan <i>unboxing</i> pada ketiganya, sehingga bila salah satu di antara
	 * total biaya, bayar tunai, atau bayar non-tunai bernilai {@code null}, method ini melempar
	 * {@link NullPointerException} — bukan mengembalikan {@code false}. Dokumen pembayaran yang
	 * baru dibuat dan belum diisi nominalnya karena itu dapat membuat grid yang menampilkan kolom
	 * status pembayaran gagal dirender. Pemanggil seperti {@code DashboardSirsKomprehensif} memang
	 * memeriksa {@code getLunas() != null} sebelum memakainya, tetapi pemeriksaan itu tidak
	 * melindungi apa pun: method ini tidak pernah mengembalikan {@code null}, ia melempar
	 * exception.</p>
	 *
	 * <p>Getter ini tidak beranotasi {@link Column @Column}; dipetakan berdasarkan konvensi.</p>
	 *
	 * @return {@code true} bila tagihan sudah tertutup; tidak pernah {@code null}.
	 * @throws NullPointerException bila dokumen pembayaran ada tetapi salah satu nominalnya
	 *                              {@code null}.
	 */
	public Boolean getLunas() {
		lunas = pembayaran != null
				&& pembayaran.getTotalBiaya() <= (pembayaran.getBayarTunai() + pembayaran.getBayarNonTunai());
		return lunas;
	}

	/**
	 * Menyetel sub-poliklinik tujuan.
	 *
	 * @param subpoly sub-poliklinik; boleh {@code null}.
	 */
	public void setSubpoly(Poly subpoly) {
		this.subpoly = subpoly;
	}

	/**
	 * Mengembalikan sub-poliklinik tujuan, setelah meresolusi proxy lazy-nya lewat
	 * {@code check(...)}.
	 *
	 * <p>Sama seperti pada {@link BookingRegistrasi#getSubpoly()}, sub-poli dipetakan ke entity
	 * yang <b>sama</b> dengan {@link #getPoly()}, yaitu {@link Poly}, hanya lewat kolom berbeda
	 * ({@code subpoly}). Hierarki poli &rarr; sub-poli karena itu tidak diwakili oleh dua tipe
	 * berbeda melainkan oleh dua kolom yang menunjuk tabel yang sama, dan <b>tidak ada penjaga</b>
	 * yang memastikan nilai {@code subpoly} benar-benar merupakan turunan dari {@code poly}.</p>
	 *
	 * @return sub-poliklinik tujuan, atau {@code null}.
	 * @see Poly
	 * @see #getPoly()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "subpoly", nullable = true)
	public Poly getSubpoly() {
		subpoly = check(subpoly);
		return subpoly;
	}

	/**
	 * Mengembalikan transaksi medis UGD yang terkait pendaftaran ini, atau {@code null}.
	 *
	 * <p>Kolomnya {@code transaksi_ugd}, {@code nullable = true}. Relasi ini adalah <b>padanan
	 * jalur UGD terhadap {@link #getBookingRegistrasi()}</b>: karena
	 * {@code PendaftaranRawatUgdAction} secara eksplisit menyetel booking ke {@code null} (gawat
	 * darurat tidak mengenal janji temu), pendaftaran UGD sebagai gantinya menautkan transaksi
	 * medis di sini lewat {@code transaksi.setPendaftaran(pendaftaran)} pada alur simpannya.</p>
	 *
	 * <p>Relasi tidak menyatakan {@code fetch = LAZY} sehingga memakai default
	 * {@link FetchType#EAGER} diperkuat
	 * {@link Fetch @Fetch}{@code (}{@link FetchMode#SELECT}{@code )} — sekali lagi satu SELECT
	 * tambahan per pemuatan pendaftaran, termasuk untuk pendaftaran rawat jalan dan rawat inap
	 * yang tidak akan pernah mengisinya.</p>
	 *
	 * @return transaksi medis UGD, atau {@code null}.
	 * @see TransaksiMedis
	 * @see #RAWAT_UGD
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_ugd", nullable = true)
	public TransaksiMedis getTransaksiUgd() {
		return transaksiUgd;
	}

	/**
	 * Menautkan transaksi medis UGD ke pendaftaran ini.
	 *
	 * @param transaksiUgd transaksi medis UGD; boleh {@code null}.
	 */
	public void setTransaksiUgd(TransaksiMedis transaksiUgd) {
		this.transaksiUgd = transaksiUgd;
	}

	/**
	 * Mengembalikan penanda apakah pendaftaran ini berisi paket tindakan — <b>selalu dihitung ulang
	 * dari isi koleksi dan ditulis balik</b> ke field.
	 *
	 * <p>Nilainya turunan sederhana {@code !pakets.isEmpty()}, sehingga
	 * {@link #setMerupakanPaket(Boolean)} praktis tanpa efek dan kolom yang dipetakannya berisi
	 * data turunan yang di-denormalisasi (property ini tidak diberi
	 * {@link javax.persistence.Transient @Transient}).</p>
	 *
	 * <p><b>Bahaya {@code LazyInitializationException}.</b> Persis seperti
	 * {@link BookingRegistrasi#getMerupakanPaket()}, method ini membaca field {@link #pakets}
	 * <i>secara langsung</i>, bukan lewat {@link #getPakets()}. Karena relasi {@code @ManyToMany}
	 * dimuat malas secara default, memanggil {@code isEmpty()} atasnya pada instance yang sudah
	 * <i>detached</i> dari {@link org.hibernate.Session} akan melempar
	 * {@code LazyInitializationException}. Pola {@code check(...)} yang melindungi getter relasi
	 * lain tidak berlaku untuk koleksi.</p>
	 *
	 * @return {@code true} bila ada minimal satu paket tindakan pada pendaftaran ini.
	 */
	public Boolean getMerupakanPaket() {
		merupakanPaket = !pakets.isEmpty();
		return merupakanPaket;
	}

	/**
	 * Menyetel penanda paket — <b>praktis tanpa efek</b>, karena {@link #getMerupakanPaket()}
	 * selalu menghitung ulang dari isi {@link #getPakets()}. Untuk mengubah nilainya, ubah isi
	 * koleksi paketnya.
	 *
	 * @param merupakanPaket nilai yang akan segera tertimpa.
	 */
	public void setMerupakanPaket(Boolean merupakanPaket) {
		this.merupakanPaket = merupakanPaket;
	}

	/**
	 * Mengembalikan booking yang ditebus menjadi pendaftaran ini, atau {@code null} bila pasien
	 * datang tanpa janji temu.
	 *
	 * <h3>Sisi maju dari relasi dua arah</h3>
	 * <p>Ini pasangan dari {@link BookingRegistrasi#getPendaftaran()}. Keduanya diikat bersamaan
	 * oleh {@code PendaftaranRawatJalanAction} dan {@code PendaftaranRawatInapAction} pada saat
	 * simpan: {@code pendaftaran.setBookingRegistrasi(booking)} lebih dulu, lalu — setelah
	 * pendaftaran tersimpan — {@code booking.setPendaftaran(pendaftaran)} diikuti
	 * {@code Common.refreshUpdate(session, booking)}. Tidak ada mekanisme di entity yang menjaga
	 * kedua sisi tetap sinkron; bila salah satu disetel tanpa yang lain, relasinya menjadi
	 * timpang.</p>
	 *
	 * <p>{@code PendaftaranRawatUgdAction} sebaliknya selalu menyetel field ini ke {@code null}
	 * dan memakai {@link #getTransaksiUgd()} sebagai gantinya.</p>
	 *
	 * <h3>Pengaruh pada nomor antrian</h3>
	 * <p>Keberadaan booking mengubah cara nomor antrian ditentukan:
	 * {@code CommonPendaftaranUtil.generateNomorAntrian(Pendaftaran, JadwalDokter)} memeriksa field
	 * ini paling awal dan, bila terisi, <b>langsung mengembalikan</b>
	 * {@code getBookingRegistrasi().getNomorAntrian()} tanpa perhitungan apa pun. Lihat
	 * {@link #getNomorAntrian()} untuk rinciannya.</p>
	 *
	 * <h3>Penjaga penebusan ganda</h3>
	 * <p>Perlindungan terhadap satu booking yang ditebus dua kali hanya hidup di lapisan tampilan:
	 * {@code AmbilDataBookingRegistrasiBanbox} menyaring dengan
	 * {@code Restrictions.isNull("pendaftaran")}, dan layar booking menyembunyikan tombol
	 * ubah/hapus untuk booking yang sudah tertaut. Kolom {@code booking_registrasi} di sini tidak
	 * dipetakan {@code unique} dan alur simpan tidak memeriksa ulang, sehingga dua pendaftaran
	 * yang disimpan bersamaan dapat sama-sama menunjuk booking yang sama — dan keduanya akan
	 * mewarisi nomor antrian yang identik.</p>
	 *
	 * <p>Relasi tidak menyatakan {@code fetch = LAZY} sehingga memakai default
	 * {@link FetchType#EAGER} diperkuat
	 * {@link Fetch @Fetch}{@code (}{@link FetchMode#SELECT}{@code )}; itu sebabnya getter ini
	 * tidak memanggil {@code check(...)}.</p>
	 *
	 * @return booking yang ditebus, atau {@code null}.
	 * @see BookingRegistrasi#getPendaftaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "booking_registrasi", nullable = true)
	public BookingRegistrasi getBookingRegistrasi() {
		return bookingRegistrasi;
	}

	/**
	 * Menautkan booking yang ditebus menjadi pendaftaran ini.
	 * <p>Setter ini <b>tidak</b> menyetel sisi kebalikannya
	 * ({@code bookingRegistrasi.setPendaftaran(this)}) dan <b>tidak</b> memeriksa apakah booking
	 * tersebut sudah ditebus pendaftaran lain — keduanya menjadi tanggung jawab pemanggil.</p>
	 *
	 * @param bookingRegistrasi booking yang ditebus; {@code null} untuk pasien tanpa janji temu
	 *                          (selalu demikian pada alur UGD).
	 */
	public void setBookingRegistrasi(BookingRegistrasi bookingRegistrasi) {
		this.bookingRegistrasi = bookingRegistrasi;
	}

	/**
	 * Mengembalikan jadwal praktek yang dituju pendaftaran ini, setelah meresolusi proxy lazy-nya
	 * lewat {@code check(...)}.
	 *
	 * <p>Dari jadwal inilah diturunkan tanggal pelayanan ({@link #getDilayaniTanggal()} membaca
	 * {@link JadwalDokter#getHari()}) dan nomor antrian
	 * ({@code CommonPendaftaranUtil.generateNomorAntrian(...)} menghitung per jadwal). Bila
	 * pendaftaran berasal dari booking, jadwal ini disalin dari
	 * {@link BookingRegistrasi#getJadwalDokter()}.</p>
	 *
	 * <p>Kolomnya {@code jadwal_dokter}, {@code nullable = true} — pendaftaran tanpa jadwal secara
	 * teknis mungkin, tetapi baris seperti itu tidak dapat menghitung tanggal pelayanan maupun
	 * nomor antrian, dan akan gagal disimpan karena {@link #getDilayaniTanggal()} memetakan kolom
	 * {@code NOT NULL} dari nilai yang {@code null}.</p>
	 *
	 * <p>Ingat karakteristik {@link JadwalDokter}: rentang berlakunya tidak pernah dipakai sebagai
	 * filter, sehingga jadwal yang sudah kedaluwarsa tetap dapat dipilih di sini; dan jadwal kembar
	 * untuk kombinasi dokter+hari+shift yang sama tidak dicegah, sehingga dua pendaftaran untuk
	 * praktek yang secara nyata sama dapat memakai deret antrian yang berbeda.</p>
	 *
	 * @return jadwal praktek yang dituju, atau {@code null}.
	 * @see JadwalDokter
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_dokter", nullable = true)
	public JadwalDokter getJadwalDokter() {
		jadwalDokter = check(jadwalDokter);
		return jadwalDokter;
	}

	/**
	 * Menyetel jadwal praktek yang dituju pendaftaran ini.
	 * <p>Menyetelnya <i>setelah</i> {@link #getDilayaniTanggal()} pernah dipanggil tidak akan
	 * mengubah tanggal pelayanan, karena method itu menyimpan hasil perhitungannya dan hanya
	 * menghitung ketika tanggal masih {@code null}. Bila jadwal diganti, setel ulang
	 * {@link #setDilayaniTanggal(Date)} ke {@code null} agar perhitungannya diulang.</p>
	 *
	 * @param jadwalDokter jadwal praktek yang dituju.
	 */
	public void setJadwalDokter(JadwalDokter jadwalDokter) {
		this.jadwalDokter = jadwalDokter;
	}

	/**
	 * Mengembalikan tanggal pasien akan dilayani, <b>menghitungnya sekali</b> dari hari pada jadwal
	 * dokter bila belum pernah dihitung.
	 *
	 * <h3>Cara kerja</h3>
	 * <p>Bila {@link #dilayaniTanggal} masih {@code null} dan pendaftaran sudah menunjuk jadwal
	 * dokter, method ini mengambil kalender aplikasi ({@code ais.ui.util.WaktuUtil.getCalendar()}),
	 * menempatkannya pada {@link #getTanggalPendaftaran()}, lalu <b>memajukan kalender satu hari
	 * demi satu hari</b> sampai nama hari kalender cocok (tanpa membedakan huruf besar-kecil)
	 * dengan {@link JadwalDokter#getHari()}. Nama hari kalender diambil dari
	 * {@code ais.common.Common.haris} memakai indeks {@code Calendar.DAY_OF_WEEK - 1}. Hasilnya
	 * disimpan ke field sehingga perhitungan tidak diulang.</p>
	 *
	 * <p>Artinya pendaftaran selalu jatuh pada <b>kemunculan pertama hari jadwal tersebut pada atau
	 * setelah tanggal pendaftaran</b> — paling jauh enam hari ke depan. Bila tanggal pendaftaran
	 * jatuh tepat pada hari jadwal, perulangan tidak berjalan sama sekali dan tanggal pelayanan
	 * sama dengan tanggal pendaftaran.</p>
	 *
	 * <h3>Bahaya: perulangan dapat berjalan tanpa henti</h3>
	 * <p>Kondisi berhenti perulangan adalah kecocokan nama hari. Bila
	 * {@link JadwalDokter#getHari()} berisi nilai yang <b>tidak ada</b> pada
	 * {@code Common.haris} — salah ketik, ejaan berbeda, string kosong, atau {@code "Jumat"}
	 * sementara daftar resminya {@code "Jum'at"} dengan tanda petik satu — kondisi itu tidak akan
	 * pernah tercapai dan perulangan berputar selamanya, menahan thread permintaan dan membebani
	 * satu inti prosesor. Bila nilainya {@code null}, yang terjadi adalah
	 * {@link NullPointerException} pada {@code hari.equalsIgnoreCase(currHari)}. Tidak ada
	 * pembatas jumlah iterasi maupun pemeriksaan nilai hari sebelum perulangan dimulai. Method
	 * kembar {@link BookingRegistrasi#getDilayaniTanggal()} memiliki bahaya yang persis sama, dan
	 * karena {@link JadwalDokter#setHari(String)} tidak memvalidasi apa pun, satu baris jadwal
	 * bermasalah cukup untuk melumpuhkan layar pendaftaran yang memakainya.</p>
	 *
	 * <h3>Catatan pemetaan</h3>
	 * <p>Dipetakan {@link TemporalType#DATE} (tanpa jam) ke kolom {@code dilayani_tanggal}
	 * berdasarkan konvensi, dan {@code @Column(nullable = false)} — padahal method ini <b>dapat
	 * mengembalikan {@code null}</b> ketika pendaftaran belum menunjuk jadwal dokter, sehingga
	 * penyimpanan pendaftaran tanpa jadwal akan gagal pada tingkat basis data alih-alih memberi
	 * pesan validasi yang informatif. Nilai ini juga menjadi acuan penyaringan pada pembangkitan
	 * nomor antrian di {@code CommonPendaftaranUtil}.</p>
	 *
	 * @return tanggal pelayanan hasil perhitungan, atau {@code null} bila pendaftaran belum
	 *         menunjuk jadwal dokter dan tanggal belum disetel manual.
	 * @see JadwalDokter#getHari()
	 * @see BookingRegistrasi#getDilayaniTanggal()
	 */
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

	/**
	 * Menyetel tanggal pelayanan secara eksplisit.
	 * <p>Menyetel nilai bukan-{@code null} <b>mematikan</b> perhitungan otomatis pada
	 * {@link #getDilayaniTanggal()}, karena perhitungan itu hanya berjalan ketika field masih
	 * {@code null} — sekaligus menghindari bahaya perulangan tanpa henti yang dijelaskan di sana.
	 * Sebaliknya, menyetelnya kembali ke {@code null} akan memicu perhitungan ulang pada pembacaan
	 * berikutnya, yang berguna setelah {@link #setJadwalDokter(JadwalDokter)} mengganti jadwal.</p>
	 *
	 * <p>Ketiga layar pendaftaran memanggil setter ini dengan tanggal yang dipilih petugas dari
	 * kalender jadwal ({@code AmbilJadwalHarian}/{@code AmbilJadwalBulanan}), sehingga pada alur
	 * normal nilai ini memang ditentukan pengguna dan perhitungan otomatis tidak pernah
	 * berjalan.</p>
	 *
	 * @param dilayaniTanggal tanggal pelayanan; {@code null} mengaktifkan kembali perhitungan
	 *                        otomatis.
	 */
	public void setDilayaniTanggal(Date dilayaniTanggal) {
		this.dilayaniTanggal = dilayaniTanggal;
	}

}
