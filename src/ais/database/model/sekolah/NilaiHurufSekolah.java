package ais.database.model.sekolah;

// Generated Dec 16, 2009 2:17:42 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashMap;
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

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;

/**
 * Master <b>rentang konversi nilai angka &rarr; huruf</b> pada modul sekolah. Satu baris entity ini
 * adalah satu pita nilai: <i>"dari {@code mulai} sampai {@code sampai}, hurufnya
 * {@code nilaiHuruf}"</i> &mdash; misalnya <i>90&ndash;100 = A</i>, <i>80&ndash;89,99 = B</i>.
 * Dipetakan ke tabel {@code sekolah.nilai_huruf_sekolah}.
 *
 * <p>Inilah tabel konversi yang sesungguhnya. Entity tetangganya,
 * {@link ais.database.model.sekolah.JenisNilaiHuruf} (tabel {@code sekolah.jenis_nilai_huruf}),
 * <b>tidak</b> menyimpan rentang apa pun &mdash; ia hanya LABEL dimensi yang memungkinkan beberapa
 * skala huruf berjalan berdampingan dalam satu sekolah (misalnya satu skala untuk ranah
 * Pengetahuan/Keterampilan, skala lain untuk ranah Sikap, skala lain lagi untuk kegiatan les).
 * Kaitannya di sini adalah kolom FK {@code jenis_nilai_huruf}
 * ({@link #getJenisNilaiHuruf()}); baris tanpa jenis dan baris ber-jenis hidup di tabel yang sama
 * dan <b>tidak pernah saling menggantikan</b> (lihat aturan pasangan ketat di bawah).
 *
 * <h3>Layar pengelolanya</h3>
 * Data ini diisi lewat {@code ais.action.master.sekolah.NilaiHurufSekolahAction} yang dirender oleh
 * {@code /pages/master/sekolah/nilai_huruf_sekolah.zul}. Label isian pada dialog tambah/ubahnya
 * memverifikasi makna tiap kolom:
 * <table border="1">
 * <caption>Pemetaan label layar &rarr; properti</caption>
 * <tr><th>Label layar</th><th>Properti</th></tr>
 * <tr><td><i>Mulai</i></td><td>{@link #getMulai()}</td></tr>
 * <tr><td><i>Sampai</i></td><td>{@link #getSampai()}</td></tr>
 * <tr><td><i>Huruf</i></td><td>{@link #getNilaiHuruf()}</td></tr>
 * <tr><td><i>Tahun Angkatan Minimal</i></td><td>{@link #getTahunAngkatan()}</td></tr>
 * <tr><td><i>Yayasan *</i></td><td>{@link #getYayasan()}</td></tr>
 * <tr><td><i>Sekolah *</i></td><td>{@link #getSekolah()}</td></tr>
 * <tr><td><i>Berlaku Mulai Tahun Ajaran</i> (opsi "Semua")</td><td>{@link #getTahunAkademik()}</td></tr>
 * <tr><td><i>Berlaku Mulai Semester</i> (opsi "Semua")</td><td>{@link #getSemester()}</td></tr>
 * <tr><td><i>Jenis Nilai Huruf</i> (opsi "=Tanpa Jenis Nilai Huruf=")</td><td>{@link #getJenisNilaiHuruf()}</td></tr>
 * <tr><td><i>Keterangan</i></td><td>{@link #getKeterangan()}</td></tr>
 * <tr><td>kolom centang <i>Lulus</i> di grid</td><td>{@link #getLulus()}</td></tr>
 * </table>
 * Label <i>"Tahun Angkatan Minimal"</i> penting: kolom itu <b>bukan</b> penyaring angkatan
 * persis, melainkan batas bawah &mdash; sesuai tahap pencarian bertingkat di bawah.
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Rentang &amp; hasil</b>: {@link #getMulai()}, {@link #getSampai()},
 * {@link #getNilaiHuruf()}, {@link #getLulus()}, {@link #getKeterangan()}.</li>
 * <li><b>Cakupan tenant</b>: {@link #getSekolah()}, {@link #getYayasan()}. Keduanya boleh
 * {@code null}; {@code null} berarti <i>berlaku lebih luas</i>, bukan "tidak diketahui".</li>
 * <li><b>Cakupan waktu</b>: {@link #getTahunAngkatan()} (angkatan minimal siswa),
 * {@link #getTahunAkademik()} + {@link #getSemester()} yang diringkas menjadi kunci numerik
 * {@link #getTa()}, serta {@link #getTanggalMulaiBerlaku()} yang <b>tidak dibaca sama sekali</b>
 * oleh mesin konversi.</li>
 * <li><b>Dimensi skala</b>: {@link #getJenisNilaiHuruf()}.</li>
 * <li><b>Mesin konversi (statis)</b>:
 * {@link #getNilaiHurufSekolah(Double, Integer, Sekolah, Yayasan, String, String, JenisNilaiHuruf)}
 * (angka &rarr; satu baris huruf) dan
 * {@link #getNilaiHurufSekolah(Integer, Sekolah, Yayasan, String, JenisNilaiHuruf)}
 * (legenda huruf &rarr; rentang, untuk dicetak di rapor).</li>
 * <li><b>Audit warisan</b>: {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h3>Mesin konversi: pencarian bertingkat atas cache statis, tanpa query</h3>
 * Kedua method statis di kelas ini <b>tidak menyentuh database sama sekali</b>. Keduanya menyapu
 * {@code ConstantValues.nilaiHurufSekolahs}, sebuah {@code List} statis app-wide yang dimuat
 * <i>seluruh isi tabel</i> ({@code createCriteria(NilaiHurufSekolah.class)} tanpa satu pun
 * {@code Restrictions}) dengan urutan {@code tahunAngkatan DESC, ta DESC, mulai DESC}. Cache itu
 * diisi sekali saat startup ({@code InitDataHelper}) dan dimuat ulang hanya oleh
 * {@code ConstantValues.realoadNilaiHurufSekolah(Session)} yang dipanggil dari
 * {@code NilaiHurufSekolahAction#onSave(Event)}. Konsekuensi praktis: baris yang diubah langsung
 * lewat SQL, lewat impor, atau lewat layar lain <b>tidak akan terlihat</b> sampai aplikasi
 * di-restart atau ada penyimpanan di layar <i>Nilai Huruf</i>.
 *
 * <p>Urutan cache itulah aturan pemenang bila rentang bertumpang tindih: pencarian selalu
 * mengambil <b>baris pertama yang cocok lalu {@code break}</b>, dan karena {@code mulai DESC}
 * berlaku, yang menang adalah pita dengan {@code mulai} terbesar &mdash; yaitu huruf yang paling
 * menguntungkan siswa. Batas rentang <b>inklusif di kedua ujung</b>, jadi pita 80&ndash;90 dan
 * 90&ndash;100 sama-sama cocok pada nilai persis 90 dan pemenangnya 90&ndash;100.
 *
 * <h3>Cakupan tenant &mdash; hal paling non-obvious pada entity ini</h3>
 * Karena cache memuat SELURUH baris seluruh instalasi (multi-yayasan, multi-sekolah), penyaring
 * tenant sepenuhnya berada di dalam kondisi {@code if} tiap tahap, bukan di query. Tahap 1 dan 1.1
 * mencocokkan {@code sekolah.id} DAN {@code yayasan.id}; tahap 2/2.1 hanya {@code yayasan.id};
 * tahap 3/3.1/4 tidak mencocokkan apa pun karena keduanya {@code null}. Artinya:
 * <ul>
 * <li>baris ber-{@code sekolah}/{@code yayasan} <b>tidak</b> bisa bocor ke tenant lain &mdash;
 * pencocokan id-nya benar;</li>
 * <li>tetapi baris dengan {@code sekolah == null} <b>dan</b> {@code yayasan == null} adalah pita
 * <b>global lintas seluruh instalasi</b>: satu baris seperti itu ikut menentukan huruf rapor
 * setiap sekolah di server yang sama, tanpa ada yang bisa melihatnya dari layar sekolahnya
 * sendiri kecuali filter Yayasan/Sekolah di layar master dibiarkan "Semua".</li>
 * </ul>
 * Layar master memang memaksa Yayasan dan Sekolah dipilih sebelum menyimpan, tetapi validasinya
 * hanya memeriksa {@code getSelectedItem().getValue() == null}, sedangkan {@link #setSekolah(Sekolah)}
 * dan {@link #setYayasan(Yayasan)} membuang objek yang {@code getId() == null} menjadi {@code null}.
 * Pada kondisi resolusi tenant gagal (objek tenant transient ber-id {@code null} &mdash; pola yang
 * sudah terdokumentasi di {@code SekolahUtil}), penyimpanan yang tampak normal di layar akan
 * menghasilkan <b>pita global</b>, bukan gagal atau tersimpan sempit. Ini jalur paling mudah
 * terjadinya "skala huruf sekolah A tiba-tiba dipakai sekolah B".
 *
 * <h3>Getter yang menulis balik</h3>
 * {@code @Id} dipasang pada getter (property access) dan {@code dynamicUpdate = true} aktif,
 * sehingga setiap nilai yang ditulis ulang oleh getter <b>ikut tersimpan</b> pada flush berikutnya.
 * Getter yang mengubah state di kelas ini:
 * <ul>
 * <li>{@link #getTahunAngkatan()} &mdash; mengisi {@code 0} bila {@code null} (mengubah "tidak
 * dibatasi" menjadi "minimal angkatan 0"; efeknya netral karena {@code >=} selalu benar).</li>
 * <li>{@link #getTanggalMulaiBerlaku()} &mdash; mengisi tanggal hari ini bila {@code null}.</li>
 * <li>{@link #getTa()} &mdash; <b>menghitung ulang lalu menimpa</b> kolom {@code ta} dari
 * {@code tahunAkademik} + {@code semester} setiap kali dibaca.</li>
 * <li>{@link #getLulus()} &mdash; menurunkan lalu menimpa {@code lulus} dari isi huruf, dan
 * memaksa {@code false} bila huruf {@code null} meskipun sebelumnya sudah di-set {@code true}.
 * Getter ini juga menimpa {@code nilaiHuruf} dengan versi ter-{@code trim()}.</li>
 * <li>{@link #getYayasan()} &mdash; <b>menimpa</b> {@code yayasan} dengan yayasan milik
 * {@link #getSekolah()} bila sekolahnya ada. Baris yang sengaja diberi yayasan berbeda dari
 * yayasan sekolahnya akan diam-diam diseragamkan saat pertama kali dirender di grid.</li>
 * <li>{@link #getSekolah()}, {@link #getJenisNilaiHuruf()} &mdash; normalisasi proxy lewat
 * {@code check(...)}, tidak merusak nilai.</li>
 * </ul>
 * Semua penulisan itu terjadi juga saat baris hanya <i>dibaca</i> &mdash; termasuk saat mesin
 * konversi menyapu cache, karena tiap tahap memanggil {@code getTa()}, {@code getSekolah()},
 * {@code getYayasan()} dan {@code getTahunAngkatan()} pada setiap kandidat.
 *
 * <h3>Catatan teknis: pengulangan field dari {@code GeneralValueObject}</h3>
 * {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti induknya. Deklarasi ulang {@code id}, {@code oleh}, {@code olehId} dan
 * {@code tanggal_dirubah} di kelas ini karena itu <b>keharusan teknis, bukan duplikasi keliru</b>.
 *
 * <h3>Pemakai terverifikasi</h3>
 * Konversi angka&rarr;huruf dipanggil dari {@code LaporanRaporSiswa},
 * {@code LaporanRekapTotalNilai}, {@code DetailPenilaianSiswaHelper},
 * {@code DetailPenilaianLesSiswaHelper}, {@code PenilaianSiswaAction},
 * {@code TampilStudiSiswaHelper}, {@code ElearningApiUtil} dan {@code NilaiSiswaApi}. Versi
 * legenda (peta huruf&rarr;rentang) dipanggil dari {@code LaporanRaporSiswa} dan
 * {@code LaporanJadwalPelajaran}.
 *
 * @see ais.database.model.sekolah.JenisNilaiHuruf
 * @see ais.database.model.GeneralValueObject
 * @see ais.common.ConstantValues#nilaiHurufSekolahs
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "nilai_huruf_sekolah")

public class NilaiHurufSekolah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja tetap agar baris yang pernah diserialisasi (state
	 * komponen ZK, cache sesi) tetap dapat dibaca setelah kelas ini diubah.
	 */
	private static final long serialVersionUID = -8007233666610291708L;
	/** Kunci utama tabel {@code sekolah.nilai_huruf_sekolah}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Kasus tepi:</b> argumen {@code null} atau yang hanya berisi spasi <b>diabaikan</b>
	 * (nilai lama dipertahankan), sehingga jejak audit tidak bisa dikosongkan secara tidak
	 * sengaja oleh pemanggil yang belum punya konteks pengguna.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Kasus tepi:</b> sama dengan {@link #setOlehId(String)} &mdash; argumen {@code null}
	 * atau kosong diabaikan tanpa error.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menstempel jejak audit lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris ini di-UPDATE.
	 *
	 * <p><b>Efek samping:</b> mengisi/menyegarkan {@code oleh}, {@code olehId} dan
	 * {@code tanggal_dirubah} dari konteks pengguna yang aktif. Jangan dipanggil manual.
	 *
	 * <p>Pada baris yang sama juga dideklarasikan field {@code tanggal_dirubah}, diinisialisasi
	 * dengan {@code WaktuUtil.getDate()} sehingga baris baru sudah bertanggal sejak dibuat di
	 * memori (bukan menunggu flush).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang dibuat di
	 *         memori karena field-nya diinisialisasi saat deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris pita nilai, dipakai untuk log/debug.
	 *
	 * <p>Formatnya {@code mulai_sampai_tahunAngkatan_nilaiHuruf}, misalnya
	 * {@code 90.0_100.0_2020_A}. Method ini membaca <b>field mentah</b>, bukan getter, sehingga
	 * tidak memicu penulisan balik maupun inisialisasi proxy &mdash; aman dipanggil pada objek
	 * detached.
	 *
	 * @return ringkasan pita nilai; elemen yang belum diisi tampil sebagai {@code null}
	 */
	public String toString() {
		return mulai + "_" + sampai + "_" + tahunAngkatan + "_" + nilaiHuruf;
	}

	/** Batas bawah pita nilai (inklusif); lihat {@link #getMulai()}. */
	private Double mulai;
	/** Batas atas pita nilai (inklusif); lihat {@link #getSampai()}. */
	private Double sampai;
	/** Angkatan minimal siswa yang boleh memakai pita ini; lihat {@link #getTahunAngkatan()}. */
	private Integer tahunAngkatan;
	/** Huruf/predikat hasil konversi; lihat {@link #getNilaiHuruf()}. */
	private String nilaiHuruf;
	/** Cakupan yayasan; {@code null} berarti berlaku lintas yayasan. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Cakupan sekolah; {@code null} berarti berlaku untuk seluruh sekolah. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Tanggal mulai berlaku administratif; tidak dibaca mesin konversi. Lihat {@link #getTanggalMulaiBerlaku()}. */
	private Date tanggalMulaiBerlaku;
	/** Tahun ajaran mulai berlaku, format {@code "2024/2025"}; lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Semester mulai berlaku ({@code Perkuliahan.GANJIL}/{@code GENAP}); lihat {@link #getSemester()}. */
	private String semester;
	/** Kunci numerik gabungan tahun ajaran + semester; lihat {@link #getTa()}. */
	private Integer ta;
	/** Penanda pita ini dianggap lulus/tuntas; lihat {@link #getLulus()}. */
	private Boolean lulus;
	/** Skala huruf pemilik pita ini; lihat {@link #getJenisNilaiHuruf()}. */
	private JenisNilaiHuruf jenisNilaiHuruf;
	/** Catatan bebas admin; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate dan dipakai layar master saat menekan
	 * tombol <i>Tambah</i> ({@code NilaiHurufSekolahAction#onAdd(Event)}). Seluruh properti masih
	 * {@code null} kecuali {@code tanggal_dirubah} yang sudah terisi waktu saat ini.
	 */
	public NilaiHurufSekolah() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} dideklarasikan {@code insertable = false} karena nilainya dibangkitkan
	 * database ({@code IDENTITY}).
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris ini. Dipakai Hibernate; jangan diisi manual pada baris baru.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan batas bawah pita nilai (label layar <i>"Mulai"</i>).
	 *
	 * <p>Batas ini <b>inklusif</b>: mesin konversi memakai {@code nilai >= mulai}. Kolomnya
	 * {@code NOT NULL}, sehingga nilai {@code null} hanya mungkin pada objek yang belum disimpan
	 * &mdash; dan pada kondisi itu mesin konversi akan melempar {@code NullPointerException} saat
	 * auto-unboxing (ditelan diam-diam per-baris, lihat mesin konversi).
	 *
	 * @return batas bawah pita
	 */
	@Column(name = "mulai", precision = 15, nullable = false)
	public Double getMulai() {
		return this.mulai;
	}

	/**
	 * Menetapkan batas bawah pita nilai.
	 *
	 * @param mulai batas bawah, inklusif
	 */
	public void setMulai(Double mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan batas atas pita nilai (label layar <i>"Sampai"</i>).
	 *
	 * <p>Batas ini juga <b>inklusif</b> ({@code nilai <= sampai}). Karena kedua ujung inklusif,
	 * pita yang disusun bersambung ({@code 80..90} lalu {@code 90..100}) <b>bertumpang tindih</b>
	 * tepat di titik sambungnya; pemenangnya ditentukan urutan cache ({@code mulai DESC}), yaitu
	 * pita yang lebih tinggi.
	 *
	 * @return batas atas pita
	 */
	@Column(name = "sampai", precision = 15, nullable = false)
	public Double getSampai() {
		return this.sampai;
	}

	/**
	 * Menetapkan batas atas pita nilai.
	 *
	 * @param sampai batas atas, inklusif
	 */
	public void setSampai(Double sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan huruf/predikat hasil konversi (label layar <i>"Huruf"</i>, kolom
	 * {@code nilai_huruf}, panjang maksimal 10 karakter).
	 *
	 * <p>Nilai dikembalikan sudah ter-{@code trim()}, tetapi <b>field-nya tidak</b> ikut diubah di
	 * sini &mdash; penulisan balik versi trim justru terjadi di {@link #getLulus()}.
	 *
	 * <p>Karena panjangnya 10, kolom ini bisa diisi predikat kata (mis. <i>"Baik"</i>,
	 * <i>"Cukup"</i>), bukan hanya huruf tunggal. Perhatikan konsekuensinya pada penurunan
	 * {@link #getLulus()}.
	 *
	 * @return huruf hasil konversi yang sudah dipangkas spasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nilai_huruf", nullable = false, length = 10)
	public String getNilaiHuruf() {
		return this.nilaiHuruf == null ? null : this.nilaiHuruf.trim();
	}

	/**
	 * Menetapkan huruf/predikat hasil konversi.
	 *
	 * <p>Nilai disimpan apa adanya (tanpa {@code trim()}); pemangkasan baru terjadi saat dibaca.
	 *
	 * @param nilaiHuruf huruf/predikat, maksimal 10 karakter
	 */
	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	/**
	 * Menetapkan angkatan minimal pemakai pita ini.
	 *
	 * @param tahunAngkatan tahun angkatan minimal (mis. {@code 2020}); {@code 0} berarti tidak
	 *                      dibatasi
	 */
	public void setTahunAngkatan(Integer tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * Mengembalikan angkatan minimal siswa yang boleh memakai pita ini (label layar
	 * <i>"Tahun Angkatan Minimal"</i>, kolom {@code tahun_angkatan}).
	 *
	 * <p>Dipakai dua cara berbeda oleh mesin konversi: tahap "persis"
	 * ({@code tahunAngkatan.equals(...)}) mencari pita yang dibuat khusus untuk satu angkatan,
	 * sedangkan tahap "minimal" ({@code tahunAngkatan >= ...}) memperlakukannya sebagai batas
	 * bawah.
	 *
	 * <p><b>EFEK SAMPING &mdash; getter menulis balik:</b> bila field masih {@code null}, getter
	 * mengisinya dengan {@code 0} dan nilai itu ikut tersimpan pada flush berikutnya (property
	 * access + {@code dynamicUpdate}). Dampaknya netral untuk pencocokan (perbandingan
	 * {@code >= 0} selalu benar), tetapi kolom yang semula NULL berubah menjadi 0 di database
	 * hanya karena barisnya pernah dirender.
	 *
	 * @return angkatan minimal; tidak pernah {@code null} (dinormalkan menjadi {@code 0})
	 */
	@Column(name = "tahun_angkatan", precision = 15, nullable = true)
	public Integer getTahunAngkatan() {
		if (tahunAngkatan == null) {
			tahunAngkatan = 0;
		}
		return tahunAngkatan;
	}

	/**
	 * Mengembalikan sekolah pemilik pita ini (label layar <i>"Sekolah *"</i>, kolom
	 * {@code sekolah_id}).
	 *
	 * <p><b>Semantik {@code null} penting:</b> {@code null} <b>bukan</b> "belum diisi" melainkan
	 * "berlaku untuk semua sekolah". Tahap 2 dan seterusnya pada mesin konversi justru
	 * mensyaratkan {@code getSekolah() == null}.
	 *
	 * <p>Relasi {@code LAZY}; getter memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk menormalkan proxy agar aman dibaca pada
	 * objek yang sudah lepas dari session &mdash; kondisi normal di sini, karena cache statis
	 * {@code ConstantValues.nilaiHurufSekolahs} berisi objek detached.
	 *
	 * @return sekolah pemilik, atau {@code null} bila pita berlaku lintas sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik pita ini.
	 *
	 * <p><b>KASUS TEPI YANG BERBAHAYA:</b> argumen yang {@code null} <i>atau</i> yang
	 * {@code getId() == null} sama-sama disimpan sebagai {@code null}. Objek {@link Sekolah}
	 * transient ber-id {@code null} adalah bentuk kegagalan resolusi tenant yang lazim di aplikasi
	 * ini; bila itu yang sampai ke sini, baris tersimpan sebagai <b>pita global lintas sekolah</b>
	 * padahal layar tampak sudah memilih sekolah. Validasi di
	 * {@code NilaiHurufSekolahAction#onSave(Event)} tidak menangkapnya karena hanya memeriksa
	 * {@code getValue() == null}, bukan {@code getValue().getId() == null}.
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek ber-id {@code null} menjadikan pita
	 *                berlaku lintas sekolah
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik pita ini (label layar <i>"Yayasan *"</i>, kolom
	 * {@code yayasan_id}).
	 *
	 * <p>Sama seperti {@link #getSekolah()}, {@code null} berarti "berlaku lintas yayasan" &mdash;
	 * kombinasi {@code sekolah == null && yayasan == null} adalah pita global seluruh instalasi.
	 *
	 * <p><b>EFEK SAMPING &mdash; getter menimpa nilai yang sudah ada:</b> bila
	 * {@link #getSekolah()} tidak {@code null}, field {@code yayasan} <b>ditimpa</b> dengan
	 * {@code sekolah.getYayasan()}, bukan sekadar diisi bila kosong. Akibatnya yayasan yang
	 * sengaja diisi berbeda dari yayasan sekolahnya akan diseragamkan diam-diam begitu baris ini
	 * dirender di grid, dipakai mesin konversi, atau dibaca laporan; karena {@code @Id} pada
	 * getter dan {@code dynamicUpdate = true}, perubahan itu ikut tersimpan. Bila sekolahnya
	 * berpindah yayasan, seluruh pita nilainya ikut berpindah tanpa layar konfirmasi.
	 *
	 * @return yayasan pemilik, atau {@code null} bila pita berlaku lintas yayasan
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
	 * Menetapkan yayasan pemilik pita ini.
	 *
	 * <p><b>KASUS TEPI:</b> identik dengan {@link #setSekolah(Sekolah)} &mdash; objek
	 * {@link Yayasan} transient ber-id {@code null} disimpan sebagai {@code null}, sehingga
	 * kegagalan resolusi tenant melebarkan cakupan pita alih-alih menggagalkan penyimpanan.
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek ber-id {@code null} menjadikan pita
	 *                berlaku lintas yayasan
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan tanggal mulai berlaku administratif pita ini (kolom {@code DATE}).
	 *
	 * <p><b>PENTING:</b> kolom ini <b>tidak dibaca oleh mesin konversi mana pun</b> dan
	 * <b>tidak ada isiannya di layar master</b>. Masa berlaku yang benar-benar dipakai runtime
	 * adalah {@link #getTa()} (turunan {@link #getTahunAkademik()} + {@link #getSemester()}).
	 * Jangan mengandalkan kolom ini untuk menjadwalkan pergantian skala.
	 *
	 * <p><b>EFEK SAMPING &mdash; getter menulis balik:</b> bila masih {@code null}, field diisi
	 * tanggal hari ini ({@code WaktuUtil.getDate()}) dan nilai itu ikut tersimpan pada flush
	 * berikutnya. Jadi tanggal yang tercatat adalah "kapan baris ini pertama kali dibaca", bukan
	 * "kapan aturan mulai berlaku".
	 *
	 * @return tanggal mulai berlaku; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulaiBerlaku() {
		if (tanggalMulaiBerlaku == null) {
			tanggalMulaiBerlaku = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalMulaiBerlaku;
	}

	/**
	 * Menetapkan tanggal mulai berlaku administratif.
	 *
	 * @param tanggalMulaiBerlaku tanggal mulai berlaku; boleh {@code null} (akan diisi otomatis
	 *                            saat dibaca)
	 */
	public void setTanggalMulaiBerlaku(Date tanggalMulaiBerlaku) {
		this.tanggalMulaiBerlaku = tanggalMulaiBerlaku;
	}

	/**
	 * Mengembalikan tahun ajaran mulai berlakunya pita ini (label layar <i>"Berlaku Mulai Tahun
	 * Ajaran"</i>), format {@code "2024/2025"}.
	 *
	 * <p>{@code null}/kosong berarti opsi <i>"Semua"</i> pada kombo: pita berlaku sejak tahun
	 * ajaran kapan pun. Nilai ini tidak dibandingkan langsung; ia diringkas dulu menjadi
	 * {@link #getTa()}.
	 *
	 * @return tahun ajaran mulai berlaku, atau {@code null} untuk "Semua"
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun ajaran mulai berlakunya pita ini.
	 *
	 * <p><b>Efek lanjutan:</b> mengubah nilai ini mengubah hasil {@link #getTa()} pada pembacaan
	 * berikutnya, dan karena {@code getTa()} menimpa kolom {@code ta}, urutan cache
	 * ({@code ta DESC}) ikut berubah setelah pemuatan ulang.
	 *
	 * @param tahunAkademik tahun ajaran format {@code "2024/2025"}; {@code null} berarti "Semua"
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan semester mulai berlakunya pita ini (label layar <i>"Berlaku Mulai
	 * Semester"</i>).
	 *
	 * <p>Nilainya {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP};
	 * {@code null}/kosong berarti opsi <i>"Semua"</i>.
	 *
	 * @return semester mulai berlaku, atau {@code null} untuk "Semua"
	 */
	public String getSemester() {
		return semester;
	}

	/**
	 * Menetapkan semester mulai berlakunya pita ini.
	 *
	 * @param semester {@code Perkuliahan.GANJIL}, {@code Perkuliahan.GENAP}, atau {@code null}
	 *                 untuk "Semua"
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Menghitung dan mengembalikan <b>kunci masa berlaku numerik</b> gabungan tahun ajaran +
	 * semester (kolom {@code ta}). Inilah satu-satunya bentuk masa berlaku yang benar-benar
	 * dibandingkan oleh mesin konversi.
	 *
	 * <h4>Rumus</h4>
	 * Digit tahun diambil dari potongan pertama {@link #getTahunAkademik()} sebelum {@code "/"}
	 * ({@code "2024/2025"} &rarr; {@code "2024"}), atau {@code "0"} bila kosong. Digit semester
	 * ditempel di belakangnya: {@code "0"} bila semester kosong ("Semua"), {@code "2"} bila
	 * {@code Perkuliahan.GENAP}, {@code "1"} untuk selain itu. Hasilnya di-parse menjadi
	 * {@code Integer}: {@code "2024" + "2"} &rarr; {@code 20242}.
	 *
	 * <h4>Konsekuensi urutan semester</h4>
	 * Karena GENAP diberi digit {@code 2} dan GANJIL {@code 1}, urutan numerik kebetulan sejalan
	 * dengan urutan kalender akademik dalam satu tahun ajaran ({@code 20241 < 20242}).
	 * Perbandingan pada mesin konversi adalah {@code baris.getTa() <= ta permintaan}, sehingga
	 * pita "berlaku mulai" semester yang lebih awal tetap terpakai di semester berikutnya.
	 *
	 * <h4>Kasus tepi</h4>
	 * <ul>
	 * <li>Tahun ajaran kosong tetapi semester terisi menghasilkan {@code 1} atau {@code 2}
	 * &mdash; nilai sangat kecil yang praktis selalu lolos perbandingan {@code <=}. Kombinasi ini
	 * karena itu setara "berlaku sejak kapan pun".</li>
	 * <li>Format tahun ajaran yang tidak numerik membuat {@code Integer.parseInt} melempar; eksepsi
	 * dicatat {@code ErrorAuditUtil} dan {@code ta} <b>mempertahankan nilai lamanya</b> (tidak
	 * di-reset), lalu dinormalkan ke {@code 0} hanya bila memang masih {@code null}.</li>
	 * <li>Method ini <b>tidak pernah mengembalikan {@code null}</b>. Karena itu cabang
	 * {@code (baris.getTa() == null || ...)} pada kedua mesin konversi adalah kode mati.</li>
	 * </ul>
	 *
	 * <p><b>EFEK SAMPING &mdash; getter menimpa nilai tersimpan:</b> hasil hitungan ditulis ke
	 * field {@code ta} setiap kali dibaca, sehingga kolom {@code ta} di database selalu
	 * diselaraskan ulang dari {@code tahunAkademik}/{@code semester} begitu baris dibaca. Nilai
	 * {@code ta} yang pernah di-set manual lewat {@link #setTa(Integer)} tidak akan bertahan.
	 * Perhatikan bahwa cache statis diurutkan berdasarkan kolom {@code ta} <i>yang tersimpan</i>,
	 * sedangkan penyaringan memakai hasil hitung ulang ini &mdash; keduanya bisa berbeda sampai
	 * pemuatan ulang berikutnya.
	 *
	 * @return kunci masa berlaku numerik; tidak pernah {@code null}
	 */
	public Integer getTa() {
		String id_smt = (getTahunAkademik() == null || getTahunAkademik().trim().isEmpty() ? "0"
				: getTahunAkademik().split("/")[0])
				+ (getSemester() == null || getSemester().trim().isEmpty() ? "0"
						: getSemester().equals(Perkuliahan.GENAP) ? "2" : "1");
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/NilaiHurufSekolah.java:210");

		}
		if (ta == null) {
			ta = 0;
		}
		return ta;
	}

	/**
	 * Menetapkan kunci masa berlaku numerik secara langsung.
	 *
	 * <p><b>Praktis tidak berguna:</b> {@link #getTa()} menghitung ulang dan menimpa nilai ini
	 * pada pembacaan pertama. Untuk mengubah masa berlaku, ubah {@link #setTahunAkademik(String)}
	 * dan {@link #setSemester(String)}.
	 *
	 * @param ta kunci masa berlaku numerik
	 */
	public void setTa(Integer ta) {
		this.ta = ta;
	}

	/**
	 * Mengembalikan penanda apakah pita ini dihitung <b>lulus/tuntas</b> (kolom centang
	 * <i>"Lulus"</i> di grid layar master).
	 *
	 * <h4>Penurunan otomatis bila belum di-set</h4>
	 * Bila field {@code lulus} masih {@code null}, nilainya diturunkan dari isi huruf: pita
	 * dianggap <b>tidak lulus</b> bila hurufnya kosong atau (setelah di-upper-case)
	 * <b>mengandung</b> huruf {@code D}, {@code E}, atau {@code T}; selain itu dianggap lulus.
	 * Bila huruf sendiri {@code null}, hasilnya dipaksa {@code false}.
	 *
	 * <h4>KASUS TEPI &mdash; penurunan ini memakai "mengandung", bukan "sama dengan"</h4>
	 * Kolom {@code nilai_huruf} panjangnya 10 karakter sehingga boleh diisi predikat kata. Semua
	 * predikat yang kebetulan memuat D/E/T akan dinilai <b>tidak lulus</b> meski maknanya baik:
	 * <i>"Baik Sekali"</i> dan <i>"Sedang"</i> (mengandung E), <i>"Amat Baik"</i> dan
	 * <i>"Tuntas"</i> (mengandung T). Untuk skala berbasis kata, kolom {@code lulus} harus diisi
	 * eksplisit lewat centang di grid, jangan dibiarkan {@code null}.
	 *
	 * <h4>Kasus tepi kedua &mdash; pemaksaan {@code false} mengalahkan nilai eksplisit</h4>
	 * Blok terakhir {@code if (nilaiHuruf == null) lulus = false;} berjalan <b>di luar</b>
	 * pengecekan {@code lulus == null}. Bila huruf {@code null} sementara admin sudah mencentang
	 * "Lulus", centang itu tetap dibalik menjadi {@code false} dan tersimpan.
	 *
	 * <p><b>EFEK SAMPING:</b> selain menimpa {@code lulus}, baris pertama method ini juga menimpa
	 * field {@code nilaiHuruf} dengan versi ter-{@code trim()} dari {@link #getNilaiHuruf()}
	 * &mdash; normalisasi yang ikut tersimpan pada flush berikutnya.
	 *
	 * <p>Dibaca oleh renderer grid layar master untuk mengisi centang, dan menjadi acuan
	 * "lulus/tidak" pada laporan yang memerlukan status kelulusan per pita.
	 *
	 * @return {@code true} bila pita dianggap lulus/tuntas; tidak pernah {@code null}
	 */
	public Boolean getLulus() {
		nilaiHuruf = getNilaiHuruf();
		if (lulus == null && nilaiHuruf != null) {
			if (nilaiHuruf.isEmpty() || nilaiHuruf.toUpperCase().contains("D") || nilaiHuruf.toUpperCase().contains("E")
					|| nilaiHuruf.toUpperCase().contains("T")) {
				lulus = false;
			} else {
				lulus = true;
			}
		} else if (lulus == null) {
			lulus = true;
		}

		if (nilaiHuruf == null) {
			lulus = false;
		}

		return lulus;
	}

	/**
	 * Menetapkan penanda lulus/tuntas pita ini.
	 *
	 * <p>Dipanggil dari event {@code onCheck} centang <i>"Lulus"</i> di grid layar master, yang
	 * langsung menyimpan perubahannya lewat {@code Common.refreshSaveOrUpdate(...)}.
	 *
	 * <p><b>PERHATIAN:</b> centang tersebut <b>tidak dijaga hak Ubah</b> (berbeda dengan tombol
	 * pensil dan tempat sampah yang memakai {@code setVisible(edit)}/{@code setVisible(delete)}),
	 * sehingga pengguna yang hanya berhak <i>membaca</i> layar itu tetap dapat membalik status
	 * lulus/tidak-lulus pita mana pun yang tampil di grid.
	 *
	 * @param lulus {@code true} bila pita dianggap lulus; {@code null} akan diturunkan ulang oleh
	 *              {@link #getLulus()}
	 */
	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	/**
	 * Mengembalikan catatan bebas admin untuk pita ini (label layar <i>"Keterangan"</i>).
	 *
	 * <p>Murni dokumentasi; tidak dibaca oleh mesin konversi maupun laporan.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan catatan bebas admin untuk pita ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * <b>MESIN KONVERSI ANGKA &rarr; HURUF.</b> Mencari satu baris pita nilai yang paling tepat
	 * untuk sebuah nilai angka, memakai pencarian bertingkat <b>tujuh tahap</b> dengan pelonggaran
	 * kriteria bertahap (<i>fallback</i> berjenjang) atas cache statis
	 * {@code ConstantValues.nilaiHurufSekolahs}.
	 *
	 * <h4>Tidak ada query di sini</h4>
	 * Method ini <b>tidak menyentuh database</b>. Seluruh tahap mengiterasi
	 * {@code ConstantValues.nilaiHurufSekolahs} &mdash; {@code List} statis app-wide berisi
	 * <i>seluruh baris tabel dari seluruh yayasan/sekolah</i>, diurutkan
	 * {@code tahunAngkatan DESC, ta DESC, mulai DESC}. Perubahan data baru terlihat setelah
	 * {@code ConstantValues.realoadNilaiHurufSekolah(Session)} dijalankan (startup, atau
	 * penyimpanan di layar <i>Nilai Huruf</i>).
	 *
	 * <h4>Normalisasi nilai masukan</h4>
	 * {@code nilai} {@code null} diubah menjadi {@code 0.0}. Sesudah itu nilai
	 * <b>diformat lalu di-parse ulang</b> dengan {@code Common.numberFormat} (locale
	 * {@code id-ID}, maksimum 3 angka di belakang koma), sehingga perbandingan batas dilakukan
	 * pada nilai yang sudah <b>dibulatkan ke 3 desimal</b>. Nilai {@code 89,9996} karena itu
	 * menjadi {@code 90,0} dan bisa naik ke pita berikutnya. Kegagalan format ditelan diam-diam
	 * (dicatat {@code ErrorAuditUtil}) dan nilai asli tetap dipakai.
	 *
	 * <h4>Kriteria yang berlaku di SEMUA tahap</h4>
	 * <ol>
	 * <li><b>Pasangan jenis ketat</b>: {@code (baris.jenis == null && parameter == null)} atau
	 * {@code (keduanya != null && id-nya sama)}. Baris tanpa jenis <b>tidak pernah</b> melayani
	 * permintaan yang menyebut sebuah {@link ais.database.model.sekolah.JenisNilaiHuruf}, dan
	 * sebaliknya. Salah mengisi salah satu sisi membuat konversi gagal total (mengembalikan
	 * {@code null}), bukan jatuh ke skala bawaan.</li>
	 * <li><b>Masa berlaku</b>: {@code baris.getTa() <= ta} dengan {@code ta} dihitung dari
	 * {@code tahunAkademik} + {@code semester} parameter memakai rumus yang sama persis dengan
	 * {@link #getTa()}. Cabang {@code baris.getTa() == null} adalah kode mati karena
	 * {@link #getTa()} tidak pernah mengembalikan {@code null}.</li>
	 * <li><b>Rentang inklusif dua ujung</b>: {@code nilai >= baris.getMulai()} dan
	 * {@code nilai <= baris.getSampai()}.</li>
	 * </ol>
	 *
	 * <h4>Tujuh tahap, dari paling spesifik ke paling longgar</h4>
	 * Tahap berikutnya hanya dijalankan bila tahap sebelumnya tidak menemukan apa pun. Setiap
	 * tahap berhenti pada kecocokan pertama ({@code break}).
	 * <table border="1">
	 * <caption>Tahap pencarian</caption>
	 * <tr><th>Tahap (penanda di kode)</th><th>Cakupan baris</th><th>Angkatan</th></tr>
	 * <tr><td>1 &mdash; "Step 1"</td><td>sekolah cocok DAN yayasan cocok</td><td>persis sama ({@code equals})</td></tr>
	 * <tr><td>2 &mdash; "Step 1.1"</td><td>sekolah cocok DAN yayasan cocok</td><td>minimal ({@code angkatan >= baris})</td></tr>
	 * <tr><td>3 &mdash; "Step 2"</td><td>baris tanpa sekolah, yayasan cocok</td><td>persis sama</td></tr>
	 * <tr><td>4 &mdash; "Step 2.1"</td><td>baris tanpa sekolah, yayasan cocok</td><td>minimal</td></tr>
	 * <tr><td>5 &mdash; "Step 3"</td><td>baris tanpa sekolah DAN tanpa yayasan (global)</td><td>persis sama</td></tr>
	 * <tr><td>6 &mdash; "Step 3.1"</td><td>baris tanpa sekolah DAN tanpa yayasan (global)</td><td>minimal</td></tr>
	 * <tr><td>7 &mdash; "Step 4"</td><td>baris tanpa sekolah DAN tanpa yayasan (global)</td><td><b>tidak diperiksa sama sekali</b></td></tr>
	 * </table>
	 * Penanda {@code Step 1..4} pada komentar kode adalah penomoran lama yang menghitung
	 * {@code x} dan {@code x.1} sebagai satu langkah; jumlah perulangan sesungguhnya <b>tujuh</b>.
	 *
	 * <h4>Kasus tepi</h4>
	 * <ul>
	 * <li><b>Rentang tumpang tindih</b>: karena urutan cache {@code mulai DESC}, pemenangnya
	 * adalah pita dengan {@code mulai} terbesar &mdash; huruf yang paling menguntungkan siswa.
	 * Pada nilai batas persis (mis. 90 untuk pita 80&ndash;90 dan 90&ndash;100), pita atas yang
	 * menang.</li>
	 * <li><b>Cakupan tenant</b>: tahap 1&ndash;4 mencocokkan id sekolah/yayasan dengan benar,
	 * sehingga pita milik satu sekolah tidak bocor ke sekolah lain. Tahap 5&ndash;7 sebaliknya
	 * <b>tidak memeriksa tenant sama sekali</b>: satu baris ber-{@code sekolah = null} dan
	 * {@code yayasan = null} di tabel ini ikut menentukan huruf rapor SETIAP sekolah pada
	 * instalasi yang sama. Karena {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}
	 * mengubah objek tenant transient ber-id {@code null} menjadi {@code null}, baris global
	 * seperti itu bisa lahir dari penyimpanan biasa yang tampak normal di layar.</li>
	 * <li><b>Parameter {@code sekolah}/{@code yayasan} {@code null}</b>: tahap 1&ndash;4 otomatis
	 * gugur (kondisinya mensyaratkan parameter tidak {@code null}), sehingga pencarian langsung
	 * jatuh ke tahap global. Pemanggil yang kehilangan konteks tenant karena itu tetap
	 * mendapat huruf &mdash; huruf dari pita global, bukan error.</li>
	 * <li><b>{@code tahunAngkatan} {@code null}</b>: memicu {@code NullPointerException} pada
	 * {@code tahunAngkatan.equals(...)}/auto-unboxing di setiap tahap kecuali tahap 7. Eksepsi
	 * ditangkap <i>per baris</i> dan hanya diteruskan ke {@code Common.tampilErrorJikaAdmin(e)},
	 * sehingga efeknya adalah tahap 1&ndash;6 gugur diam-diam dan hasilnya diambil dari tahap 7
	 * (pita global tanpa syarat angkatan).</li>
	 * <li><b>{@code mulai}/{@code sampai} {@code null}</b> pada sebuah baris cache: auto-unboxing
	 * melempar {@code NullPointerException}, baris itu dilewati diam-diam. Kolomnya
	 * {@code NOT NULL}, jadi ini hanya mungkin pada objek yang belum tersimpan.</li>
	 * <li><b>Tidak ada yang cocok</b>: mengembalikan {@code null}. Pemanggil umumnya
	 * menerjemahkannya menjadi huruf kosong di rapor, bukan error.</li>
	 * <li><b>Efek samping tak terduga</b>: karena tiap tahap memanggil {@code getTa()},
	 * {@code getSekolah()}, {@code getYayasan()} dan {@code getTahunAngkatan()} pada setiap
	 * kandidat, satu kali konversi menjalankan seluruh penulisan balik getter tersebut pada
	 * ratusan baris cache sekaligus (lihat Javadoc kelas).</li>
	 * </ul>
	 *
	 * <h4>Dipanggil dari</h4>
	 * {@code LaporanRaporSiswa} (huruf per grup penilaian dan per mata pelajaran),
	 * {@code LaporanRekapTotalNilai}, {@code DetailPenilaianSiswaHelper},
	 * {@code DetailPenilaianLesSiswaHelper}, {@code PenilaianSiswaAction},
	 * {@code TampilStudiSiswaHelper} (kolom "Predikat"), {@code ElearningApiUtil} dan
	 * {@code NilaiSiswaApi}. Pola argumennya seragam: nilai rata-rata/total, {@code siswa.getTahunMasuk()}
	 * sebagai angkatan, {@code siswa.getSekolah()}/{@code siswa.getYayasan()} sebagai cakupan,
	 * tahun ajaran kelas, semester turunan nomor semester, dan
	 * {@code grupPenilaian.getJenisNilaiHuruf()} sebagai skala.
	 *
	 * @param nilai           nilai angka yang akan dikonversi; {@code null} diperlakukan sebagai
	 *                        {@code 0.0} dan dibulatkan ke 3 desimal sebelum dibandingkan
	 * @param tahunAngkatan   tahun angkatan siswa (umumnya {@code siswa.getTahunMasuk()});
	 *                        {@code null} membuat tahap 1&ndash;6 gugur diam-diam
	 * @param sekolah         sekolah siswa; {@code null} membuat tahap 1&ndash;2 gugur
	 * @param yayasan         yayasan siswa; {@code null} membuat tahap 1&ndash;4 gugur
	 * @param tahunAkademik   tahun ajaran penilaian, format {@code "2024/2025"}; kosong dianggap
	 *                        tahun {@code 0}
	 * @param semester        semester penilaian ({@code Perkuliahan.GANJIL}/{@code GENAP});
	 *                        kosong dianggap digit {@code 0}
	 * @param jenisNilaiHuruf skala huruf yang diminta; harus <b>persis</b> sepadan dengan kolom
	 *                        {@code jenis_nilai_huruf} baris kandidat &mdash; {@code null} hanya
	 *                        cocok dengan baris yang juga tanpa jenis
	 * @return baris pita nilai yang cocok, atau {@code null} bila ketujuh tahap gagal
	 * @see #getNilaiHurufSekolah(Integer, Sekolah, Yayasan, String, JenisNilaiHuruf)
	 * @see ais.database.model.sekolah.JenisNilaiHuruf
	 */
	public static NilaiHurufSekolah getNilaiHurufSekolah(Double nilai, Integer tahunAngkatan, Sekolah sekolah,
			Yayasan yayasan, String tahunAkademik, String semester, JenisNilaiHuruf jenisNilaiHuruf) {
		nilai = nilai == null ? 0.0 : nilai;
		// // System.out.println("Hitung sebelum nilai " + nilai + "");
		try {
			nilai = nilai == null ? 0.0 : Common.numberFormat.get().parse(Common.numberFormat.get().format(nilai)).doubleValue();
		} catch (Exception e1) { ais.common.ErrorAuditUtil.record(e1, "auto-audit(empty-catch) src/ais/database/model/sekolah/NilaiHurufSekolah.java:261");
			// TODO Auto-generated catch block
			// e1.printStackTrace();
		}

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null || semester.trim().isEmpty() ? "0"
						: semester.equals(Perkuliahan.GENAP) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/NilaiHurufSekolah.java:272");

		}

		NilaiHurufSekolah huruf = null;
		try {

			// TAHAP 1 ("Step 1") - paling spesifik: sekolah cocok, yayasan cocok,
			// angkatan PERSIS sama.
			for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
				try {

					if (nilaiHurufSekolah != null

							&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
									|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
											&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
													.equals(jenisNilaiHuruf.getId())))

							&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)
							&& nilaiHurufSekolah.getSekolah() != null && sekolah != null
							&& nilaiHurufSekolah.getSekolah().getId().equals(sekolah.getId())

							&& nilaiHurufSekolah.getYayasan() != null && yayasan != null
							&& nilaiHurufSekolah.getYayasan().getId().equals(yayasan.getId())

							&& nilai >= nilaiHurufSekolah.getMulai()

							&& nilai <= nilaiHurufSekolah.getSampai()

							&& tahunAngkatan.equals(nilaiHurufSekolah.getTahunAngkatan())

					) {
						huruf = nilaiHurufSekolah;
						// System.out.println("Step 1 : nilai huruf " +
						// huruf.toString());
						break;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			// TAHAP 2 ("Step 1.1") - cakupan sama seperti tahap 1, syarat angkatan
			// dilonggarkan menjadi batas minimal.
			if (huruf == null) {
				for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
					try {
						if (nilaiHurufSekolah != null

								&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
										|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
												&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
														.equals(jenisNilaiHuruf.getId())))

								&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)
								&& nilaiHurufSekolah.getSekolah() != null && sekolah != null
								&& nilaiHurufSekolah.getSekolah().getId().equals(sekolah.getId())

								&& nilaiHurufSekolah.getYayasan() != null && yayasan != null
								&& nilaiHurufSekolah.getYayasan().getId().equals(yayasan.getId())

								&& nilai >= nilaiHurufSekolah.getMulai()

								&& nilai <= nilaiHurufSekolah.getSampai()

								&& tahunAngkatan >= nilaiHurufSekolah.getTahunAngkatan()

						) {
							huruf = nilaiHurufSekolah;
							// System.out.println("Step 1.1 : nilai huruf " +
							// huruf.toString());
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			// TAHAP 3 ("Step 2") - cakupan dilonggarkan: baris TANPA sekolah, yayasan
			// masih harus cocok; angkatan persis sama.
			if (huruf == null) {
				for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
					try {
						if (nilaiHurufSekolah != null

								&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
										|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
												&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
														.equals(jenisNilaiHuruf.getId())))

								&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

								&& nilaiHurufSekolah.getSekolah() == null

								&& nilaiHurufSekolah.getYayasan() != null && yayasan != null
								&& nilaiHurufSekolah.getYayasan().getId().equals(yayasan.getId())

								&& nilai >= nilaiHurufSekolah.getMulai()

								&& nilai <= nilaiHurufSekolah.getSampai()

								&& tahunAngkatan.equals(nilaiHurufSekolah.getTahunAngkatan())

						) {
							huruf = nilaiHurufSekolah;
							// System.out.println("Step 2 : nilai huruf " +
							// huruf.toString());
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

			}

			// TAHAP 4 ("Step 2.1") - baris TANPA sekolah, yayasan cocok, angkatan
			// sebagai batas minimal.
			if (huruf == null) {
				for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
					try {
						if (nilaiHurufSekolah != null

								&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
										|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
												&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
														.equals(jenisNilaiHuruf.getId())))

								&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

								&& nilaiHurufSekolah.getSekolah() == null

								&& nilaiHurufSekolah.getYayasan() != null && yayasan != null
								&& nilaiHurufSekolah.getYayasan().getId().equals(yayasan.getId())

								&& nilai >= nilaiHurufSekolah.getMulai()

								&& nilai <= nilaiHurufSekolah.getSampai()

								&& tahunAngkatan >= nilaiHurufSekolah.getTahunAngkatan()

						) {
							huruf = nilaiHurufSekolah;
							// System.out.println("Step 2.1 : nilai huruf " +
							// huruf.toString());
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

			}

			// TAHAP 5 ("Step 3") - baris GLOBAL (tanpa sekolah DAN tanpa yayasan):
			// tidak ada lagi penyaring tenant di sini. Angkatan persis sama.
			if (huruf == null) {
				for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
					try {
						if (nilaiHurufSekolah != null

								&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
										|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
												&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
														.equals(jenisNilaiHuruf.getId())))

								&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

								&& nilaiHurufSekolah.getSekolah() == null

								&& nilaiHurufSekolah.getYayasan() == null

								&& nilai >= nilaiHurufSekolah.getMulai()

								&& nilai <= nilaiHurufSekolah.getSampai()

								&& tahunAngkatan.equals(nilaiHurufSekolah.getTahunAngkatan())

						) {
							huruf = nilaiHurufSekolah;
							// System.out.println("Step 3 : nilai huruf " +
							// huruf.toString());
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

			}

			// TAHAP 6 ("Step 3.1") - baris GLOBAL, angkatan sebagai batas minimal.
			if (huruf == null) {
				for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
					try {
						if (nilaiHurufSekolah != null

								&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
										|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
												&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
														.equals(jenisNilaiHuruf.getId())))

								&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

								&& nilaiHurufSekolah.getSekolah() == null

								&& nilaiHurufSekolah.getYayasan() == null

								&& nilai >= nilaiHurufSekolah.getMulai()

								&& nilai <= nilaiHurufSekolah.getSampai()

								&& tahunAngkatan >= nilaiHurufSekolah.getTahunAngkatan()

						) {
							huruf = nilaiHurufSekolah;
							// System.out.println("Step 3.1 : nilai huruf " +
							// huruf.toString());
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

				}
			}

			// TAHAP 7 ("Step 4") - jaring pengaman terakhir: baris GLOBAL, syarat
			// angkatan DIHAPUS seluruhnya. Satu-satunya tahap yang tidak menyentuh
			// tahunAngkatan, sehingga tetap berjalan meski parameter itu null.
			if (huruf == null) {
				for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
					try {
						if (nilaiHurufSekolah != null

								&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
										|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
												&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
														.equals(jenisNilaiHuruf.getId())))

								&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

								&& nilaiHurufSekolah.getSekolah() == null

								&& nilaiHurufSekolah.getYayasan() == null

								&& nilai >= nilaiHurufSekolah.getMulai()

								&& nilai <= nilaiHurufSekolah.getSampai()

						) {
							huruf = nilaiHurufSekolah;
							// System.out.println("Step 4 : nilai huruf " +
							// huruf.toString());
							break;
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return huruf;
	}

	/**
	 * <b>LEGENDA RENTANG NILAI.</b> Menyusun peta {@code huruf -> {mulai, sampai}} untuk dicetak
	 * sebagai keterangan skala penilaian di kaki rapor dan laporan jadwal pelajaran.
	 *
	 * <p>Berbeda dari
	 * {@link #getNilaiHurufSekolah(Double, Integer, Sekolah, Yayasan, String, String, JenisNilaiHuruf)},
	 * method ini <b>tidak mencari satu pemenang</b>: ia menjalankan <b>tujuh sapuan berurutan</b>
	 * atas cache statis {@code ConstantValues.nilaiHurufSekolahs} dan setiap sapuan
	 * <b>menimpa</b> entri peta yang sudah ada. Karena sapuan disusun dari cakupan paling longgar
	 * ke paling sempit, hasil akhirnya adalah aturan "yang lebih spesifik menang":
	 * <ol>
	 * <li>baris global (tanpa sekolah &amp; tanpa yayasan), tanpa syarat angkatan;</li>
	 * <li>baris global, angkatan sebagai batas minimal;</li>
	 * <li>baris global, angkatan persis sama;</li>
	 * <li>baris tanpa sekolah tetapi yayasan cocok, angkatan minimal;</li>
	 * <li>baris tanpa sekolah tetapi yayasan cocok, angkatan persis sama;</li>
	 * <li>baris sekolah + yayasan cocok, angkatan minimal;</li>
	 * <li>baris sekolah + yayasan cocok, angkatan persis sama (paling spesifik, menang terakhir).</li>
	 * </ol>
	 * Seluruh sapuan juga menerapkan pasangan jenis ketat dan {@code baris.getTa() <= ta} yang
	 * sama seperti mesin konversi.
	 *
	 * <h4>Kasus tepi &mdash; kunci peta adalah HURUF, bukan id baris</h4>
	 * {@code maps.put(huruf.trim(), ...)} berarti dua baris berbeda yang memakai huruf sama (mis.
	 * dua pita "A" untuk dua angkatan) saling menimpa. Dalam satu sapuan, urutan cache
	 * ({@code tahunAngkatan DESC, ta DESC, mulai DESC}) membuat baris <b>terakhir</b> yang menang,
	 * yaitu yang {@code mulai}-nya paling kecil. Legenda yang tercetak karena itu bisa menyebut
	 * rentang yang <i>berbeda</i> dari rentang yang benar-benar dipakai mesin konversi untuk huruf
	 * tersebut. Baris dengan huruf {@code null} dilewati.
	 *
	 * <h4>Kasus tepi &mdash; semester dipaksa "0" sehingga pita bersemester tersaring keluar</h4>
	 * Kunci masa berlaku di sini dibentuk sebagai {@code tahunAkademik + "0"} (method ini memang
	 * tidak menerima parameter semester), sedangkan {@link #getTa()} tiap baris memakai digit
	 * {@code "1"} untuk GANJIL dan {@code "2"} untuk GENAP. Untuk tahun ajaran yang sama, pita
	 * ber-semester menghasilkan {@code ta} yang <b>lebih besar</b> daripada ambang
	 * {@code tahun + "0"}, sehingga <b>gagal</b> syarat {@code baris.getTa() <= ta} dan tidak
	 * pernah masuk legenda. Praktisnya legenda hanya memuat pita yang semesternya "Semua" atau
	 * pita dari tahun ajaran sebelumnya.
	 *
	 * <h4>Kasus tepi &mdash; kedua pemanggil mengirim {@code jenisNilaiHuruf} = {@code null}</h4>
	 * {@code LaporanRaporSiswa} dan {@code LaporanJadwalPelajaran} sama-sama meneruskan
	 * {@code null}. Karena pasangan jenis bersifat ketat, legenda yang dihasilkan <b>hanya</b>
	 * memuat pita tanpa {@link ais.database.model.sekolah.JenisNilaiHuruf}. Pada sekolah yang
	 * seluruh skalanya diberi jenis, peta yang dikembalikan <b>kosong</b> dan blok keterangan
	 * skala di rapor tercetak kosong &mdash; padahal huruf pada badan rapor tetap terisi karena
	 * mesin konversi dipanggil dengan jenis yang benar.
	 *
	 * <h4>Kasus tepi lain</h4>
	 * <ul>
	 * <li>{@code tahunAngkatan} {@code null} memicu {@code NullPointerException} pada sapuan
	 * 2&ndash;7; eksepsi ditangkap per baris sehingga hanya sapuan pertama yang menghasilkan isi.</li>
	 * <li>{@code sekolah}/{@code yayasan} {@code null} membuat sapuan 4&ndash;7 gugur; legenda
	 * jatuh ke pita global saja.</li>
	 * <li>Peta yang dikembalikan berupa {@link HashMap} sehingga urutan iterasinya tidak
	 * deterministik &mdash; kedua pemanggil merangkainya menjadi string {@code "A:90.0:100.0;..."}
	 * dengan urutan yang bisa berubah antar-pencetakan.</li>
	 * <li>Tidak pernah mengembalikan {@code null}; kegagalan menghasilkan peta kosong.</li>
	 * </ul>
	 *
	 * @param tahunAngkatan   tahun angkatan siswa (mis. {@code siswa.getTahunMasuk()}); pada
	 *                        laporan jadwal diambil dari potongan tahun kombo tahun ajaran
	 * @param sekolah         sekolah yang dilaporkan; {@code null} menyisakan pita global saja
	 * @param yayasan         yayasan yang dilaporkan; {@code null} menyisakan pita global saja
	 * @param tahunAkademik   tahun ajaran laporan, format {@code "2024/2025"}; kosong dianggap
	 *                        tahun {@code 0} sehingga hampir semua pita tersaring keluar
	 * @param jenisNilaiHuruf skala huruf yang diminta; kedua pemanggil saat ini selalu
	 *                        mengirim {@code null}
	 * @return peta {@code huruf -> {mulai, sampai}}; tidak pernah {@code null}, bisa kosong
	 * @see #getNilaiHurufSekolah(Double, Integer, Sekolah, Yayasan, String, String, JenisNilaiHuruf)
	 */
	public static Map<String, Double[]> getNilaiHurufSekolah(Integer tahunAngkatan, Sekolah sekolah, Yayasan yayasan,
			String tahunAkademik, JenisNilaiHuruf jenisNilaiHuruf) {
		Map<String, Double[]> maps = new HashMap<String, Double[]>();

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ "0";
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/NilaiHurufSekolah.java:539");

		}

		try {

			// SAPUAN 1 - pita GLOBAL (tanpa sekolah & tanpa yayasan), tanpa syarat
			// angkatan. Lapisan paling longgar; boleh ditimpa sapuan berikutnya.
			for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
				try {
					if (nilaiHurufSekolah != null

							&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
									|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
											&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
													.equals(jenisNilaiHuruf.getId())))

							&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

							&& nilaiHurufSekolah.getSekolah() == null

							&& nilaiHurufSekolah.getYayasan() == null

					) {
						if (nilaiHurufSekolah.getNilaiHuruf() != null) {
							maps.put(nilaiHurufSekolah.getNilaiHuruf().trim(),
									new Double[] { nilaiHurufSekolah.getMulai(), nilaiHurufSekolah.getSampai() });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			// SAPUAN 2 - pita GLOBAL dengan angkatan sebagai batas minimal.
			for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
				try {
					if (nilaiHurufSekolah != null

							&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
									|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
											&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
													.equals(jenisNilaiHuruf.getId())))

							&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

							&& nilaiHurufSekolah.getSekolah() == null

							&& nilaiHurufSekolah.getYayasan() == null

							&& tahunAngkatan >= nilaiHurufSekolah.getTahunAngkatan()

					) {
						if (nilaiHurufSekolah.getNilaiHuruf() != null) {
							maps.put(nilaiHurufSekolah.getNilaiHuruf().trim(),
									new Double[] { nilaiHurufSekolah.getMulai(), nilaiHurufSekolah.getSampai() });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

			}

			// SAPUAN 3 - pita GLOBAL dengan angkatan PERSIS sama.
			for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
				try {
					if (nilaiHurufSekolah != null

							&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
									|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
											&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
													.equals(jenisNilaiHuruf.getId())))

							&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

							&& nilaiHurufSekolah.getSekolah() == null

							&& nilaiHurufSekolah.getYayasan() == null

							&& tahunAngkatan.equals(nilaiHurufSekolah.getTahunAngkatan())

					) {
						if (nilaiHurufSekolah.getNilaiHuruf() != null) {
							maps.put(nilaiHurufSekolah.getNilaiHuruf().trim(),
									new Double[] { nilaiHurufSekolah.getMulai(), nilaiHurufSekolah.getSampai() });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			// SAPUAN 4 - pita tanpa sekolah tetapi yayasan cocok, angkatan minimal.
			for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
				try {
					if (nilaiHurufSekolah != null

							&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
									|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
											&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
													.equals(jenisNilaiHuruf.getId())))

							&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

							&& nilaiHurufSekolah.getSekolah() == null

							&& nilaiHurufSekolah.getYayasan() != null && yayasan != null
							&& nilaiHurufSekolah.getYayasan().getId().equals(yayasan.getId())

							&& tahunAngkatan >= nilaiHurufSekolah.getTahunAngkatan()

					) {
						if (nilaiHurufSekolah.getNilaiHuruf() != null) {
							maps.put(nilaiHurufSekolah.getNilaiHuruf().trim(),
									new Double[] { nilaiHurufSekolah.getMulai(), nilaiHurufSekolah.getSampai() });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			// SAPUAN 5 - pita tanpa sekolah tetapi yayasan cocok, angkatan persis sama.
			for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
				try {
					if (nilaiHurufSekolah != null

							&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
									|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
											&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
													.equals(jenisNilaiHuruf.getId())))

							&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)

							&& nilaiHurufSekolah.getSekolah() == null

							&& nilaiHurufSekolah.getYayasan() != null && yayasan != null
							&& nilaiHurufSekolah.getYayasan().getId().equals(yayasan.getId())

							&& tahunAngkatan.equals(nilaiHurufSekolah.getTahunAngkatan())

					) {
						if (nilaiHurufSekolah.getNilaiHuruf() != null) {
							maps.put(nilaiHurufSekolah.getNilaiHuruf().trim(),
									new Double[] { nilaiHurufSekolah.getMulai(), nilaiHurufSekolah.getSampai() });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			// SAPUAN 6 - pita milik sekolah + yayasan yang cocok, angkatan minimal.
			for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
				try {
					if (nilaiHurufSekolah != null

							&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
									|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
											&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
													.equals(jenisNilaiHuruf.getId())))

							&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)
							&& nilaiHurufSekolah.getSekolah() != null && sekolah != null
							&& nilaiHurufSekolah.getSekolah().getId().equals(sekolah.getId())

							&& nilaiHurufSekolah.getYayasan() != null && yayasan != null
							&& nilaiHurufSekolah.getYayasan().getId().equals(yayasan.getId())

							&& tahunAngkatan >= nilaiHurufSekolah.getTahunAngkatan()

					) {
						if (nilaiHurufSekolah.getNilaiHuruf() != null) {
							maps.put(nilaiHurufSekolah.getNilaiHuruf().trim(),
									new Double[] { nilaiHurufSekolah.getMulai(), nilaiHurufSekolah.getSampai() });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			// SAPUAN 7 - paling spesifik: pita milik sekolah + yayasan yang cocok
			// dengan angkatan PERSIS sama. Dijalankan terakhir sehingga menimpa
			// seluruh lapisan di atasnya.
			for (NilaiHurufSekolah nilaiHurufSekolah : ConstantValues.nilaiHurufSekolahs) {
				try {

					if (nilaiHurufSekolah != null

							&& ((nilaiHurufSekolah.getJenisNilaiHuruf() == null && jenisNilaiHuruf == null)
									|| (nilaiHurufSekolah.getJenisNilaiHuruf() != null && jenisNilaiHuruf != null
											&& nilaiHurufSekolah.getJenisNilaiHuruf().getId()
													.equals(jenisNilaiHuruf.getId())))

							&& (nilaiHurufSekolah.getTa() == null || nilaiHurufSekolah.getTa() <= ta)
							&& nilaiHurufSekolah.getSekolah() != null && sekolah != null
							&& nilaiHurufSekolah.getSekolah().getId().equals(sekolah.getId())

							&& nilaiHurufSekolah.getYayasan() != null && yayasan != null
							&& nilaiHurufSekolah.getYayasan().getId().equals(yayasan.getId())

							&& tahunAngkatan.equals(nilaiHurufSekolah.getTahunAngkatan())

					) {
						if (nilaiHurufSekolah.getNilaiHuruf() != null) {
							maps.put(nilaiHurufSekolah.getNilaiHuruf().trim(),
									new Double[] { nilaiHurufSekolah.getMulai(), nilaiHurufSekolah.getSampai() });
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return maps;
	}

	/**
	 * Mengembalikan skala huruf pemilik pita ini (label layar <i>"Jenis Nilai Huruf"</i>, kolom FK
	 * {@code jenis_nilai_huruf}).
	 *
	 * <p>Inilah satu-satunya kaitan entity ini dengan
	 * {@link ais.database.model.sekolah.JenisNilaiHuruf}. Nilai {@code null} berarti opsi
	 * <i>"=Tanpa Jenis Nilai Huruf="</i> pada kombo &mdash; pita milik skala bawaan, dan hanya
	 * melayani permintaan konversi yang juga tidak menyebut jenis (pasangan ketat).
	 *
	 * <p>Sisi seberangnya adalah {@code GrupPenilaian#getJenisNilaiHuruf()}: nilai itulah yang
	 * dikirim {@code LaporanRaporSiswa} sebagai parameter {@code jenisNilaiHuruf} saat mencari
	 * huruf, sehingga hanya pita dengan jenis yang sama yang boleh dipakai.
	 *
	 * <p><b>Catatan:</b> saklar {@code aktif} milik
	 * {@link ais.database.model.sekolah.JenisNilaiHuruf} tidak pernah dibaca pada jalur konversi;
	 * menonaktifkan sebuah jenis tidak menghentikan pemakaian pita-pita yang menunjuk ke sana.
	 *
	 * <p>Relasi {@code LAZY}; getter menormalkan proxy lewat {@code check(...)} agar aman dibaca
	 * pada objek detached di dalam cache statis.
	 *
	 * @return skala huruf pemilik pita, atau {@code null} bila pita memakai skala bawaan
	 * @see ais.database.model.sekolah.JenisNilaiHuruf
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_nilai_huruf")
	public JenisNilaiHuruf getJenisNilaiHuruf() {
		jenisNilaiHuruf = check(jenisNilaiHuruf);
		return jenisNilaiHuruf;
	}

	/**
	 * Menetapkan skala huruf pemilik pita ini.
	 *
	 * <p>Berbeda dari {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)}, setter ini
	 * menyimpan argumen apa adanya &mdash; objek transient ber-id {@code null} <b>tidak</b>
	 * dinormalkan menjadi {@code null} di sini, sehingga bisa menggagalkan penyimpanan pada level
	 * Hibernate alih-alih diam-diam melebarkan cakupan.
	 *
	 * <p>Diisi dari kombo <i>"Jenis Nilai Huruf"</i> di layar master, yang hanya menampilkan
	 * jenis milik sekolah terpilih dan berstatus {@code aktif = true}.
	 *
	 * @param jenisNilaiHuruf skala huruf; {@code null} berarti "=Tanpa Jenis Nilai Huruf="
	 */
	public void setJenisNilaiHuruf(JenisNilaiHuruf jenisNilaiHuruf) {
		this.jenisNilaiHuruf = jenisNilaiHuruf;
	}
}
