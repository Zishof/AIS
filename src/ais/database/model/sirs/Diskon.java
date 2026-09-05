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
 * <h3>Riwayat perbaikan — nilai awal field yang dulu membuat diskon baru mustahil terpilih</h3>
 * Penyaringan aturan diskon di {@code CommonSirs.getDiskonSekarang} memakai syarat atas
 * kolom-kolom entitas ini: {@code or(isNull(aktif), eq(aktif, true))}, {@code le(mulai, tanggal)},
 * {@code or(isNull(sampai), ge(sampai, tanggal))}, serta
 * {@code le(jumlahMinimal, jumlah)} dan {@code ge(jumlahMaksimal, jumlah)}. Sebelumnya, dua nilai
 * awal field pada kelas ini bertabrakan dengan syarat-syarat itu dan membuat <b>setiap</b> aturan
 * diskon yang disimpan apa adanya tidak pernah terpilih, tanpa pesan kesalahan apa pun:
 * <ol>
 * <li><b>{@code jumlahMaksimal} dulu berawal {@code 0}, padahal getternya bermaksud {@code 100}.</b>
 * {@link #getJumlahMaksimal()} menyediakan nilai cadangan {@code 100} untuk keadaan {@code null} —
 * niatnya jelas: batas atas yang longgar bila operator tidak menentukan apa-apa. Namun field-nya
 * dulu diinisialisasi {@code 0} pada deklarasi, sehingga nilai cadangan itu tidak pernah tercapai
 * untuk objek baru. Sudah diperbaiki: field ini sekarang <b>tidak diinisialisasi</b> (tetap
 * {@code null} sampai diisi), sehingga nilai cadangan {@code 100} berlaku untuk baris baru dan
 * form {@code ais.action.master.sirs.DiskonAction} menampilkan {@code 100}, bukan {@code 0}, pada
 * kotak isian "Jumlah maksimal mendapatkan diskon".</li>
 * <li><b>{@code sampai} dulu berawal hari ini.</b> Field {@code sampai} dulu diinisialisasi
 * {@code new Date()}, yaitu cap waktu saat objek dibuat, sehingga aturan yang disimpan apa adanya
 * berlaku hanya sampai detik pembuatannya. Sudah diperbaiki: field ini sekarang <b>tidak
 * diinisialisasi</b> (tetap {@code null}), sehingga toleransi kueri {@code isNull(sampai)} =
 * "tanpa batas akhir" berlaku langsung untuk baris baru, sejalan dengan {@link TarifKhusus} yang
 * juga tidak menginisialisasi {@code sampai}.</li>
 * </ol>
 * {@code ais.action.master.sirs.DiskonAction#onSave} juga kini menolak penyimpanan bila
 * {@code jumlahMaksimal < jumlahMinimal} atau {@code sampai} lebih awal dari {@code mulai}, untuk
 * mencegah kombinasi rentang yang tidak mungkin terpenuhi tersimpan tanpa peringatan.
 *
 * <h3>Penanganan {@code aktif} kini seragam dengan tarif khusus</h3>
 * {@link #getAktif()} menormalkan {@code null} menjadi {@code true} (anggap aktif bila belum
 * ditentukan). Kueri penyaring {@code CommonSirs.getDiskonSekarang} sebelumnya memakai
 * {@code eq(aktif, true)} yang ketat — baris dengan kolom {@code aktif} bernilai {@code NULL} di
 * basis data (mis. hasil penyemaian awal atau impor yang tidak menyentuh kolom itu) tidak akan
 * terpilih, berlawanan dengan maksud getternya. Sudah diperbaiki: kueri kini memakai
 * {@code or(isNull(aktif), eq(aktif, true))}, sama seperti resolusi {@link TarifKhusus} di
 * {@code CommonTarif.getTarif}, sehingga baris dengan {@code aktif} {@code NULL} ikut dianggap
 * aktif.
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
 * {@link TemporalType#DATE}. Sejak {@code sampai} tidak lagi diinisialisasi ke {@code new Date()},
 * perbedaan ini tidak lagi berakibat aturan baru langsung kedaluwarsa, tetapi kolomnya tetap
 * menyimpan komponen jam bila operator mengisi {@code sampai} lewat jalur selain form (form
 * mengisi dari {@link ais.ui.util.MyDatebox}, yang hanya membawa komponen tanggal).</li>
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
	 * Akhir masa berlaku; sengaja TIDAK diinisialisasi (tetap {@code null} sampai diisi eksplisit
	 * oleh operator), sejalan dengan toleransi {@code isNull(sampai)} = "tanpa batas akhir" pada
	 * kueri penyaring {@code CommonSirs.getDiskonSekarang}. Sebelumnya field ini diinisialisasi
	 * {@code new Date()} sehingga setiap aturan baru langsung kedaluwarsa pada detik pembuatannya;
	 * lihat riwayat perbaikan pada dokumentasi kelas.
	 */
	private Date sampai;

	/** Akun buku besar tempat potongan diskon dibukukan; kolomnya {@code NOT NULL}. */
	private Akun akun;

	/** Kuantitas minimal transaksi agar diskon berlaku. */
	private Integer jumlahMinimal = 0;

	/**
	 * Kuantitas maksimal transaksi agar diskon berlaku; sengaja TIDAK diinisialisasi, sehingga
	 * tetap {@code null} sampai diisi eksplisit oleh operator dan nilai cadangan {@code 100} pada
	 * {@link #getJumlahMaksimal()} berlaku untuk baris baru. Sebelumnya field ini diinisialisasi
	 * {@code 0}, yang mematikan nilai cadangan tersebut dan membuat setiap diskon baru tidak pernah
	 * terpilih. Lihat riwayat perbaikan pada dokumentasi kelas.
	 */
	private Integer jumlahMaksimal;

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
	 * ({@code or(isNull(sampai), ge(sampai, tanggal))}), dan field ini <b>tidak</b> diinisialisasi
	 * (tetap {@code null} sampai diisi), sehingga aturan diskon baru langsung memanfaatkan toleransi
	 * tersebut sampai operator mengisi tanggal akhirnya secara eksplisit. Karena field ini tidak
	 * ditandai {@link TemporalType#DATE} (berbeda dari {@link TarifKhusus#getSampai()}), nilai yang
	 * diisi lewat jalur selain form (yang memakai {@link ais.ui.util.MyDatebox}, tanggal saja) dapat
	 * membawa komponen jam.
	 * </p>
	 *
	 * @return akhir masa berlaku, dapat {@code null} yang berarti tanpa batas akhir
	 */
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Mengisi akhir masa berlaku aturan diskon. Nilai {@code null} berarti berlaku tanpa batas
	 * akhir. {@code ais.action.master.sirs.DiskonAction#onSave} menolak penyimpanan bila nilai ini
	 * diisi lebih awal dari {@link #getMulai()}, tetapi tetap tidak mewajibkan kolom ini terisi.
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
	 * tidak menentukan batas atas, pakailah batas yang longgar. Field {@link #jumlahMaksimal}
	 * sengaja tidak diinisialisasi pada deklarasinya (dulu diinisialisasi {@code 0}, yang mematikan
	 * cabang nilai cadangan ini untuk setiap aturan diskon baru — lihat riwayat perbaikan pada
	 * dokumentasi kelas), sehingga sekarang nilai cadangan berlaku baik untuk objek baru maupun
	 * baris lama yang kolomnya {@code NULL} di basis data.
	 * </p>
	 *
	 * <p>
	 * {@code ais.action.master.sirs.DiskonAction} mengisi kotak isian "Jumlah maksimal mendapatkan
	 * diskon" dari getter ini, sehingga operator kini melihat {@code 100} pada form tambah baru,
	 * bukan {@code 0}. Kueri penyaring {@code CommonSirs.getDiskonSekarang} menerapkan
	 * {@code ge(jumlahMaksimal, jumlah)}; dengan nilai cadangan {@code 100}, syarat itu terpenuhi
	 * apa adanya untuk kuantitas transaksi wajar tanpa operator perlu mengubah apa pun.
	 * {@code DiskonAction#onSave} juga menolak penyimpanan bila nilai ini lebih kecil dari
	 * {@link #getJumlahMinimal()}.
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
	 * Mengisi kuantitas maksimal agar diskon berlaku. {@code DiskonAction#onSave} menolak
	 * penyimpanan bila nilai ini diisi lebih kecil dari {@link #getJumlahMinimal()}.
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
	 * Maksud "belum ditentukan berarti aktif" yang dinyatakan nilai cadangan di sini kini juga
	 * dihormati oleh kueri penyaringnya. {@code CommonSirs.getDiskonSekarang} memakai
	 * {@code or(isNull(aktif), eq(aktif, true))} — sama seperti {@code CommonTarif.getTarif} untuk
	 * {@link TarifKhusus} — sehingga baris dengan kolom {@code aktif} bernilai {@code NULL} (mis.
	 * hasil penyemaian awal atau impor yang tidak menyentuh kolom itu) tetap terpilih, sejalan
	 * dengan maksud getternya.
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
