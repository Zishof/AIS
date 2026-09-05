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

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;

/**
 * Master <b>aturan diskon</b> modul SIRS: satu baris menyatakan potongan harga yang berlaku bagi
 * kombinasi tertentu (peserta asuransi tertentu, anggota komunitas tertentu, dalam rentang tanggal
 * tertentu, untuk kuantitas dalam rentang tertentu). Sasaran diskon — item medis, tindakan, atau
 * alat medis mana yang berhak — dirinci pada baris-baris {@link DiskonDetail}.
 *
 * <h3>Nilai diskon selalu persentase, tidak pernah rupiah</h3>
 * {@link #getJumlah()} menyimpan <b>persen</b>, bukan nominal. Hal ini terlihat dari
 * {@link #toString()} yang menambahkan tanda "%", dan dipastikan oleh pemakaiannya di
 * {@code ais.action.master.sirs.util.CommonPendaftaranUtil} yang menghitung nilai diskon sebagai
 * {@code amount * (diskon.getJumlah() / 100.0)}. Tidak ada kolom untuk diskon nominal di mana pun
 * pada entitas ini; diskon rupiah tetap tidak dapat direpresentasikan.
 *
 * <h3>Kedudukan dalam urutan penetapan harga</h3>
 * Diskon adalah lapisan <b>terakhir</b>, diterapkan setelah tarif dasar dan tarif per kelas
 * perawatan selesai dijumlahkan. Hasilnya tidak mengubah angka tarif, melainkan dicatat sebagai
 * baris {@link Biaya} tersendiri bernilai negatif dengan {@link Biaya#getDiskon()} terisi. Perlu
 * dicatat dua hal penting tentang urutannya:
 * <ul>
 * <li><b>Diskon dan {@link PajakMedis} dihitung dari basis yang sama</b>, yaitu {@code amount}
 * baris transaksi sebelum penyesuaian apa pun. Pajak karena itu dikenakan atas jumlah
 * <i>sebelum</i> diskon, bukan atas dasar pengenaan setelah diskon.</li>
 * <li><b>Seluruh aturan diskon yang cocok berlaku bersama-sama secara akumulatif</b>, bukan
 * berebut menang. {@code CommonSirs.getDiskonSekarang} mengembalikan daftar, dan
 * {@code getTotalDiskonDalamPersen} menjumlahkan seluruh persentasenya. Tidak ada prioritas,
 * pengecualian saling-meniadakan, maupun batas atas total — dua aturan 60% yang sama-sama cocok
 * menghasilkan potongan 120%, yakni tagihan negatif, tanpa penolakan dari mana pun.</li>
 * </ul>
 *
 * <h3>PERINGATAN — nilai awal field membuat diskon baru mustahil terpilih</h3>
 * Penyaringan aturan diskon di {@code CommonSirs.getDiskonSekarang} memakai empat syarat atas
 * kolom-kolom entitas ini: {@code eq(aktif, true)}, {@code le(mulai, tanggal)},
 * {@code or(isNull(sampai), ge(sampai, tanggal))}, serta
 * {@code le(jumlahMinimal, jumlah)} dan {@code ge(jumlahMaksimal, jumlah)}. Dua nilai awal field
 * pada kelas ini bertabrakan dengan syarat-syarat itu:
 * <ol>
 * <li><b>{@code jumlahMaksimal} berawal {@code 0}, padahal getternya bermaksud {@code 100}.</b>
 * {@link #getJumlahMaksimal()} menyediakan nilai cadangan {@code 100} untuk keadaan {@code null} —
 * niatnya jelas: batas atas yang longgar bila operator tidak menentukan apa-apa. Namun field-nya
 * sudah diinisialisasi {@code 0} pada deklarasi, sehingga <b>nilai cadangan itu tidak pernah
 * tercapai</b> untuk objek baru: field tidak pernah {@code null}. Form
 * {@code ais.action.master.sirs.DiskonAction} mengisi kotak isian dari {@code getJumlahMaksimal()},
 * jadi yang terlihat operator adalah angka {@code 0}, dan validasi simpan tidak mewajibkan kolom
 * itu diubah. Aturan diskon yang disimpan apa adanya karena itu memiliki {@code jumlahMaksimal = 0},
 * dan syarat {@code ge(jumlahMaksimal, jumlah)} gagal untuk setiap transaksi berkuantitas satu atau
 * lebih — diskon <b>tidak akan pernah terpilih</b>, tanpa pesan kesalahan apa pun.</li>
 * <li><b>{@code sampai} berawal hari ini.</b> Field {@code sampai} diinisialisasi
 * {@code new Date()}, yaitu cap waktu saat objek dibuat. Form mengisi kotak tanggalnya dari nilai
 * itu dan validasi simpan hanya mewajibkan {@code mulai}, bukan {@code sampai}. Aturan yang
 * disimpan apa adanya karena itu berlaku hanya sampai detik pembuatannya; syarat
 * {@code ge(sampai, tanggal)} gagal untuk transaksi mana pun yang terjadi sesudahnya — termasuk
 * transaksi pada hari yang sama beberapa menit kemudian. Perhatikan bahwa kueri memang
 * mentoleransi {@code sampai} yang {@code NULL} sebagai "tanpa batas akhir", tetapi nilai awal
 * bukan {@code NULL} sehingga toleransi itu pun tidak terpakai.</li>
 * </ol>
 * Keduanya adalah kejadian berulang dari pola "diskon mustahil diberikan" yang sudah dikenal —
 * diskon yang secara sengaja dikonfigurasi diam-diam tidak pernah berlaku — meskipun mekanismenya
 * berbeda dari instance sebelumnya: di sini penyebabnya adalah nilai awal field yang mendahului
 * (dan mematikan) nilai cadangan pada getter, bukan normalisasi {@code null} pada penjaga
 * pemilih-mode. Sampai diperbaiki, setiap aturan diskon <b>wajib</b> diisi {@code jumlahMaksimal}
 * dan {@code sampai} secara eksplisit oleh operator.
 *
 * <h3>Penanganan {@code aktif} yang tidak seragam dengan tarif khusus</h3>
 * {@link #getAktif()} menormalkan {@code null} menjadi {@code true} (anggap aktif bila belum
 * ditentukan), tetapi kueri penyaring memakai {@code eq(aktif, true)} yang <b>ketat</b>: baris
 * dengan kolom {@code aktif} bernilai {@code NULL} di basis data tidak akan terpilih, berlawanan
 * dengan maksud getternya. Bandingkan dengan resolusi {@link TarifKhusus} di
 * {@code CommonTarif.getTarif} yang memakai {@code or(isNull(aktif), eq(aktif, true))} — longgar,
 * dan sesuai dengan maksud getter. Ketidakseragaman ini berarti data diskon yang dibuat di luar
 * form (mis. lewat penyemaian awal atau impor) dapat diam-diam tidak berlaku bila kolom
 * {@code aktif} dibiarkan {@code NULL}.
 *
 * <h3>Cakupan diskon tidak berlaku surut ke transaksi yang sudah menempel</h3>
 * Penyaringan tanggal, status aktif, dan rentang kuantitas hanya dilakukan pada saat aturan diskon
 * <i>dipilih</i> dan ditempelkan ke baris transaksi
 * ({@code TransaksiMedisDetail.getDiskons()}/{@code RacikanDetail.getDiskons()}). Sesudah menempel,
 * {@code CommonPendaftaranUtil} memakai {@link #getJumlah()} apa adanya tanpa memeriksa ulang
 * apakah aturan itu masih aktif atau masih dalam masa berlaku. Untuk transaksi historis sifat ini
 * benar dan diinginkan (potret nilai pada saat transaksi), tetapi berarti menonaktifkan sebuah
 * aturan diskon <b>tidak</b> mencabutnya dari transaksi berjalan yang belum diposting.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Bendera aktif dua-arah</b> — {@link #getAktif()}/{@link #setAktif(Boolean)} dapat
 * dinyalakan dan dimatikan; bukan bendera satu-arah.</li>
 * <li><b>Getter destruktif</b> — {@link #getJumlah()}, {@link #getJumlahMinimal()},
 * {@link #getJumlahMaksimal()}, {@link #getAktif()}, dan seluruh relasi menulis balik ke field.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — aturan diskon bersifat global lintas unit; pembatasan cakupan
 * hanya lewat {@link #getAsuransi()} dan {@link #getKomunitas()}.</li>
 * <li>{@code jumlah}, {@code mulai}, {@code sampai}, {@code jumlahMinimal}, {@code jumlahMaksimal},
 * dan {@code aktif} tidak diberi {@code @Column}, sehingga dipetakan ke kolom bernama sesuai nama
 * properti. {@code mulai} dan {@code sampai} juga tanpa {@code @Temporal}, sehingga tersimpan
 * sebagai stempel waktu penuh — berbeda dari {@link TarifKhusus} yang menandainya
 * {@link TemporalType#DATE}. Perbedaan itulah yang membuat nilai awal {@code sampai} pada kelas ini
 * membawa komponen jam dan langsung kedaluwarsa.</li>
 * </ul>
 *
 * @see DiskonDetail rincian item/tindakan/alat medis yang berhak atas diskon ini
 * @see PajakMedis padanan entitas ini untuk pungutan pajak medis
 * @see Biaya baris biaya negatif tempat diskon dicatat
 * @see ais.common.CommonSirs#getDiskonSekarang kueri penyaring aturan diskon yang berlaku
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "diskon")
public class Diskon extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.diskon}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah aturan diskon ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir; nilai kosong/spasi diabaikan agar jejak audit
	 * tidak terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;

	/**
	 * Representasi teks aturan diskon untuk komponen ZK, berbentuk {@code nama-nilai%}. Tanda persen
	 * pada label inilah penegasan paling langsung bahwa {@link #getJumlah()} menyimpan persentase,
	 * bukan nominal rupiah.
	 *
	 * @return label aturan diskon
	 */
	public String toString() {
		return nama + "-" + Common.numberFormat.get().format(getJumlah()) + "%";
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; nilai kosong/spasi diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah aturan diskon ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir; normalnya diisi otomatis oleh interceptor audit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir aturan diskon ini. Jangan dikacaukan dengan
	 * {@link #getMulai()}/{@link #getSampai()} yang merupakan masa berlaku domain.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode aturan diskon, wajib diisi. */
	private String kode;

	/** Nama aturan diskon (kolom bertipe {@code text}); dipakai sebagai label dan kunci pengurutan. */
	private String nama;

	/** Keterangan bebas atas aturan diskon. */
	private String keterangan;

	/** Besaran diskon dalam <b>persen</b>, bukan rupiah. */
	private Double jumlah = 0.0;

	/** Awal masa berlaku; diinisialisasi ke waktu pembuatan objek. */
	private Date mulai = new Date();

	/**
	 * Akhir masa berlaku; diinisialisasi ke waktu pembuatan objek, sehingga aturan yang disimpan
	 * tanpa penyuntingan langsung kedaluwarsa. Lihat peringatan pada dokumentasi kelas.
	 */
	private Date sampai = new Date();

	/** Akun buku besar tempat potongan diskon dibukukan; kolomnya {@code NOT NULL}. */
	private Akun akun;

	/** Kuantitas minimal transaksi agar diskon berlaku. */
	private Integer jumlahMinimal = 0;

	/**
	 * Kuantitas maksimal transaksi agar diskon berlaku. Nilai awal {@code 0} bertabrakan dengan
	 * nilai cadangan {@code 100} pada {@link #getJumlahMaksimal()} dan membuat diskon baru tidak
	 * pernah terpilih. Lihat peringatan pada dokumentasi kelas.
	 */
	private Integer jumlahMaksimal = 0;

	/** Pembatas cakupan: diskon hanya berlaku bagi peserta asuransi ini bila terisi. */
	private Asuransi asuransi;

	/** Pembatas cakupan: diskon hanya berlaku bagi anggota komunitas ini bila terisi. */
	private Komunitas komunitas;

	/** Status berlaku aturan diskon; tanpa nilai awal, sehingga {@code null} sampai diisi. */
	private Boolean aktif;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public Diskon() {
	}

	/**
	 * Mengembalikan kunci utama aturan diskon.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya untuk kerangka kerja persistensi atau saat menyalin
	 * entitas menjadi baris baru.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode aturan diskon.
	 *
	 * @return kode diskon (kolom wajib, maksimal 255 karakter)
	 */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Mengisi kode aturan diskon. Tidak ada penjaga tabrakan kode di lapisan model maupun indeks
	 * unik pada kolomnya.
	 *
	 * @param kode kode aturan diskon
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan bebas aturan diskon.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas aturan diskon.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi nama aturan diskon.
	 *
	 * @param nama nama aturan diskon
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan nama aturan diskon.
	 *
	 * @return nama aturan diskon, atau {@code null} bila tidak diisi
	 */
	@Column(name = "nama", columnDefinition = "text", nullable = true)
	public String getNama() {
		return nama;
	}

	/**
	 * Mengembalikan besaran diskon dalam <b>persen</b>, menormalkan {@code null} menjadi
	 * {@code 0.0}. <b>Getter destruktif</b>: normalisasi ditulis balik ke field.
	 *
	 * <p>
	 * Nilai ini dipakai langsung oleh {@code CommonPendaftaranUtil} sebagai
	 * {@code amount * (getJumlah() / 100.0)} dan dijumlahkan lintas aturan oleh
	 * {@code CommonSirs.getTotalDiskonDalamPersen}. Tidak ada validasi rentang di sini maupun di
	 * form: nilai di atas {@code 100} maupun negatif dapat tersimpan, dan karena beberapa aturan
	 * diskon berlaku akumulatif, total potongan pun tidak dibatasi {@code 100%}.
	 * </p>
	 *
	 * @return besaran diskon dalam persen, tidak pernah {@code null}
	 */
	public Double getJumlah() {
		if (jumlah == null) {
			jumlah = 0.0;
		}
		return jumlah;
	}

	/**
	 * Mengisi besaran diskon dalam persen.
	 *
	 * @param jumlah besaran diskon dalam persen; tidak divalidasi rentangnya
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan awal masa berlaku aturan diskon. Getter murni-baca tanpa normalisasi, sehingga
	 * dapat mengembalikan {@code null} untuk baris yang kolomnya kosong di basis data — dan baris
	 * semacam itu tidak akan terpilih oleh syarat {@code le(mulai, tanggal)} yang tidak
	 * mentoleransi {@code NULL}.
	 *
	 * @return awal masa berlaku, dapat {@code null}
	 */
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Mengisi awal masa berlaku aturan diskon. Wajib terisi — form menolak penyimpanan bila kosong.
	 *
	 * @param mulai awal masa berlaku
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan akhir masa berlaku aturan diskon. Getter murni-baca tanpa normalisasi.
	 *
	 * <p>
	 * Kueri penyaring mentoleransi nilai {@code NULL} sebagai "berlaku tanpa batas akhir"
	 * ({@code or(isNull(sampai), ge(sampai, tanggal))}), tetapi field ini diinisialisasi
	 * {@code new Date()} sehingga aturan diskon baru tidak pernah memanfaatkan toleransi tersebut:
	 * ia justru berlaku hanya sampai detik pembuatannya. Karena field ini tidak ditandai
	 * {@link TemporalType#DATE} (berbeda dari {@link TarifKhusus#getSampai()}), komponen jamnya ikut
	 * tersimpan, sehingga aturan menjadi kedaluwarsa bahkan untuk transaksi pada hari yang sama.
	 * Isilah nilai ini secara eksplisit, atau kosongkan menjadi {@code null} bila diskon dimaksudkan
	 * berlaku tanpa batas waktu.
	 * </p>
	 *
	 * @return akhir masa berlaku, dapat {@code null} yang berarti tanpa batas akhir
	 */
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Mengisi akhir masa berlaku aturan diskon. Nilai {@code null} berarti berlaku tanpa batas
	 * akhir. Form tidak mewajibkan pengisian kolom ini, sehingga nilai awal yang langsung
	 * kedaluwarsa dapat lolos tersimpan.
	 *
	 * @param sampai akhir masa berlaku, atau {@code null} untuk tanpa batas
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan akun buku besar tempat potongan diskon dibukukan. <b>Getter destruktif</b>
	 * ({@code check(...)}). Kolomnya {@code NOT NULL} dan form mewajibkan pengisiannya. Akun ini
	 * juga menjadi sumber warisan bagi {@link DiskonDetail#getAkun()} dan salah satu sumber warisan
	 * pada {@link Biaya#getAkun()}.
	 *
	 * @return akun buku besar diskon, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = false)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun buku besar tempat potongan diskon dibukukan.
	 *
	 * @param akun akun buku besar
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan kuantitas minimal agar diskon berlaku, menormalkan {@code null} menjadi
	 * {@code 0}. <b>Getter destruktif</b>. Nilai awal field ({@code 0}) sejalan dengan nilai
	 * cadangan getter, sehingga syarat {@code le(jumlahMinimal, jumlah)} terpenuhi apa adanya —
	 * berbeda dari {@link #getJumlahMaksimal()} yang bermasalah.
	 *
	 * @return kuantitas minimal, tidak pernah {@code null}
	 */
	public Integer getJumlahMinimal() {
		if (jumlahMinimal == null) {
			jumlahMinimal = 0;
		}
		return jumlahMinimal;
	}

	/**
	 * Mengisi kuantitas minimal agar diskon berlaku.
	 *
	 * @param jumlahMinimal kuantitas minimal
	 */
	public void setJumlahMinimal(Integer jumlahMinimal) {
		this.jumlahMinimal = jumlahMinimal;
	}

	/**
	 * Mengembalikan kuantitas maksimal agar diskon berlaku, menormalkan {@code null} menjadi
	 * {@code 100}. <b>Getter destruktif</b>.
	 *
	 * <p>
	 * Nilai cadangan {@code 100} di sini menyatakan maksud penulisnya dengan jelas: bila operator
	 * tidak menentukan batas atas, pakailah batas yang longgar. Maksud itu <b>tidak pernah
	 * terlaksana</b>. Field {@link #jumlahMaksimal} sudah diinisialisasi {@code 0} pada
	 * deklarasinya, sehingga tidak pernah bernilai {@code null} untuk objek yang dibuat lewat
	 * konstruktor — dan cabang nilai cadangan menjadi kode mati bagi seluruh aturan diskon baru. Ia
	 * hanya akan berjalan untuk baris lama yang kolomnya benar-benar {@code NULL} di basis data.
	 * </p>
	 *
	 * <p>
	 * Akibatnya berantai sampai ke penyaringan. {@code ais.action.master.sirs.DiskonAction} mengisi
	 * kotak isian "Jumlah maksimal mendapatkan diskon" dari getter ini, jadi yang tampil bagi
	 * operator adalah angka {@code 0}; validasi penyimpanan hanya mewajibkan nama, tanggal mulai,
	 * dan akun, sehingga angka {@code 0} itu lolos tersimpan. Kueri penyaring
	 * {@code CommonSirs.getDiskonSekarang} kemudian menerapkan {@code ge(jumlahMaksimal, jumlah)} —
	 * yang untuk {@code jumlahMaksimal = 0} gagal pada setiap transaksi berkuantitas satu atau
	 * lebih. Aturan diskon tersebut tidak pernah muncul dalam daftar diskon yang berlaku, tidak
	 * pernah menempel ke baris transaksi, dan tidak menghasilkan pesan kesalahan apa pun; dari sisi
	 * operator, diskon yang sudah dikonfigurasi sekadar "tidak jalan".
	 * </p>
	 *
	 * <p>
	 * Ini kejadian berulang dari pola "diskon mustahil diberikan" yang sudah tercatat pada modul
	 * ini, dengan mekanisme yang berbeda: pada instance sebelumnya penyebabnya adalah normalisasi
	 * {@code null} pada getter yang dipakai sebagai penjaga pemilih-mode, sedangkan di sini
	 * penyebabnya adalah nilai awal field yang mendahului dan mematikan nilai cadangan getter.
	 * Selama belum diperbaiki, setiap aturan diskon wajib diisi batas maksimalnya secara eksplisit
	 * dengan angka yang cukup besar.
	 * </p>
	 *
	 * @return kuantitas maksimal, tidak pernah {@code null}
	 */
	public Integer getJumlahMaksimal() {
		if (jumlahMaksimal == null) {
			jumlahMaksimal = 100;
		}
		return jumlahMaksimal;
	}

	/**
	 * Mengisi kuantitas maksimal agar diskon berlaku. Wajib diisi secara eksplisit dengan angka
	 * yang memadai; membiarkan nilai awal {@code 0} membuat diskon tidak pernah terpilih.
	 *
	 * @param jumlahMaksimal kuantitas maksimal
	 */
	public void setJumlahMaksimal(Integer jumlahMaksimal) {
		this.jumlahMaksimal = jumlahMaksimal;
	}

	/**
	 * Mengembalikan pembatas cakupan asuransi. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * <p>
	 * Penyaringan atas kolom ini bersifat <b>pencocokan persis, bukan penyertaan</b>:
	 * {@code CommonSirs.getDiskonSekarang} menambahkan {@code isNull(asuransi)} ketika pasien tidak
	 * memiliki asuransi, dan {@code eq(asuransi, ...)} ketika memiliki. Konsekuensinya, aturan
	 * diskon umum yang kolom asuransinya kosong <b>tidak</b> berlaku bagi pasien berasuransi, dan
	 * sebaliknya. Perilaku ini berbeda dari resolusi {@link TarifKhusus} di
	 * {@code CommonTarif.getTarif} yang melonggarkan syarat secara bertingkat hingga jatuh ke aturan
	 * umum. Diskon "untuk semua orang" karena itu perlu dibuat berulang per kombinasi cakupan.
	 * </p>
	 *
	 * @return asuransi yang dicakup, atau {@code null} bila aturan ini hanya untuk pasien tanpa asuransi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi", nullable = true)
	public Asuransi getAsuransi() {
		asuransi = check(asuransi);
		return asuransi;
	}

	/**
	 * Menetapkan pembatas cakupan asuransi aturan diskon ini.
	 *
	 * @param asuransi asuransi yang dicakup, atau {@code null}
	 */
	public void setAsuransi(Asuransi asuransi) {
		this.asuransi = asuransi;
	}

	/**
	 * Mengembalikan status berlaku aturan diskon, menormalkan {@code null} menjadi {@code true}.
	 * <b>Getter destruktif</b>. Bendera ini dua-arah — dapat dinyalakan maupun dimatikan lewat
	 * {@link #setAktif(Boolean)}.
	 *
	 * <p>
	 * Maksud "belum ditentukan berarti aktif" yang dinyatakan nilai cadangan di sini
	 * <b>tidak dihormati</b> oleh kueri penyaringnya. {@code CommonSirs.getDiskonSekarang} memakai
	 * {@code eq(aktif, true)} yang ketat, sehingga baris dengan kolom {@code aktif} bernilai
	 * {@code NULL} — misalnya hasil penyemaian awal atau impor yang tidak menyentuh kolom itu —
	 * tidak akan terpilih. Bandingkan dengan {@code CommonTarif.getTarif} untuk {@link TarifKhusus}
	 * yang memakai {@code or(isNull(aktif), eq(aktif, true))} dan justru sejalan dengan maksud
	 * getternya. Karena field {@link #aktif} juga tidak memiliki nilai awal, aturan diskon yang
	 * dibuat di luar form berisiko tersimpan dengan {@code NULL} dan diam-diam tidak berlaku.
	 * </p>
	 *
	 * @return {@code true} bila aturan diskon berlaku, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyalakan atau mematikan aturan diskon. Perlu diingat bahwa mematikan aturan hanya mencegah
	 * pemilihannya di kemudian hari; aturan yang sudah menempel pada baris transaksi tetap
	 * diperhitungkan.
	 *
	 * @param aktif {@code true} bila aturan berlaku
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan pembatas cakupan komunitas. <b>Getter destruktif</b> ({@code check(...)}).
	 * Sama seperti {@link #getAsuransi()}, penyaringannya bersifat pencocokan persis: kolom kosong
	 * hanya cocok bagi pasien tanpa komunitas, dan kolom terisi hanya cocok bila komunitas pasien
	 * termasuk di dalamnya.
	 *
	 * @return komunitas yang dicakup, atau {@code null} bila aturan ini hanya untuk pasien tanpa komunitas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "komunitas", nullable = true)
	public Komunitas getKomunitas() {
		komunitas = check(komunitas);
		return komunitas;
	}

	/**
	 * Menetapkan pembatas cakupan komunitas aturan diskon ini.
	 *
	 * @param komunitas komunitas yang dicakup, atau {@code null}
	 */
	public void setKomunitas(Komunitas komunitas) {
		this.komunitas = komunitas;
	}

}
