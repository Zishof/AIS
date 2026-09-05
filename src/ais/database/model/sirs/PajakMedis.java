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
 * Master <b>aturan pajak medis</b>: satu baris menyatakan pungutan yang dikenakan atas layanan
 * medis bagi kombinasi tertentu (peserta asuransi tertentu, anggota komunitas tertentu, dalam
 * rentang tanggal tertentu). Sasarannya — item medis, tindakan, atau alat medis mana yang dikenai —
 * dirinci pada baris-baris {@link PajakDetail}. Entitas ini adalah cerminan struktural
 * {@link Diskon} dengan arah tanda yang berlawanan.
 *
 * <h3>Nilai pajak selalu persentase</h3>
 * {@link #getJumlah()} menyimpan <b>persen</b>, sebagaimana ditegaskan {@link #toString()} yang
 * menambahkan tanda "%" dan dipastikan oleh pemakaiannya di
 * {@code ais.action.master.sirs.util.CommonPendaftaranUtil} sebagai
 * {@code amount * (pajak.getJumlah() / 100.0)}. Pungutan bernilai nominal tetap — yang lazim untuk
 * sebagian jenis retribusi — tidak dapat direpresentasikan pada model ini.
 *
 * <h3>Kedudukan dalam urutan penetapan harga</h3>
 * Bersama {@link Diskon}, pajak adalah lapisan terakhir yang diterapkan setelah tarif dasar dan
 * tarif per kelas perawatan selesai dijumlahkan. Hasilnya dicatat sebagai baris {@link Biaya}
 * tersendiri (bernilai positif, dengan {@link Biaya#getPajak()} terisi), bukan dengan mengubah
 * angka tarif. Dua sifat urutannya perlu ditegaskan:
 * <ul>
 * <li><b>Pajak dan diskon dihitung dari basis yang sama</b>, yaitu {@code amount} baris transaksi
 * sebelum penyesuaian apa pun. Pajak karena itu dikenakan atas jumlah <i>sebelum</i> diskon, bukan
 * atas dasar pengenaan setelah diskon. Perbedaan ini bersifat sistematis dan membesar seiring nilai
 * transaksi, sehingga penting diketahui saat merekonsiliasi angka aplikasi dengan perhitungan
 * pajak manual.</li>
 * <li><b>Seluruh aturan pajak yang cocok berlaku akumulatif.</b>
 * {@code CommonSirs.getPajakSekarang} mengembalikan daftar dan
 * {@code getTotalPajakDalamPersen} menjumlahkan seluruh persentasenya; tidak ada prioritas
 * maupun batas atas.</li>
 * </ul>
 *
 * <h3>PERINGATAN — nilai awal {@code sampai} membuat aturan pajak baru langsung kedaluwarsa</h3>
 * Sama seperti pada {@link Diskon}, field {@link #sampai} diinisialisasi {@code new Date()} dan
 * tidak ditandai {@link TemporalType#DATE}, sehingga menyimpan stempel waktu penuh berikut jamnya.
 * Kueri penyaring {@code CommonSirs.getPajakSekarang} memakai
 * {@code or(isNull(sampai), ge(sampai, sekarang))} — jadi nilai {@code NULL} sesungguhnya
 * ditoleransi sebagai "berlaku tanpa batas akhir" — tetapi nilai awal bukan {@code NULL} melainkan
 * detik pembuatan objek, sehingga aturan yang disimpan tanpa penyuntingan kolom itu langsung
 * kedaluwarsa dan tidak pernah terpilih, bahkan untuk transaksi beberapa menit kemudian pada hari
 * yang sama.
 * <p>
 * Arah kerugiannya berlawanan dengan kasus diskon namun setara bobotnya: bila diskon yang mati
 * membuat pasien membayar lebih dari seharusnya, pajak yang mati membuat rumah sakit
 * <b>kurang memungut</b> — persoalan kepatuhan fiskal yang baru terlihat saat rekonsiliasi, karena
 * tidak ada pesan kesalahan apa pun yang muncul. Isilah {@code sampai} secara eksplisit, atau
 * kosongkan menjadi {@code null} bila pungutan dimaksudkan berlaku tanpa batas waktu.
 * </p>
 *
 * <h3>Penanganan {@code aktif} yang tidak seragam dengan tarif khusus</h3>
 * {@link #getAktif()} menormalkan {@code null} menjadi {@code true}, tetapi kueri penyaringnya
 * memakai {@code eq(aktif, true)} yang ketat — baris dengan kolom {@code aktif} bernilai
 * {@code NULL} tidak akan terpilih, berlawanan dengan maksud getternya. Bandingkan dengan resolusi
 * {@link TarifKhusus} di {@code CommonTarif.getTarif} yang memakai
 * {@code or(isNull(aktif), eq(aktif, true))}. Karena field {@link #aktif} juga tidak memiliki nilai
 * awal, aturan pajak yang dibuat di luar form berisiko tersimpan dengan {@code NULL} dan diam-diam
 * tidak berlaku.
 *
 * <h3>Perbedaan dengan {@link Diskon}</h3>
 * Selain arah tanda, entitas ini <b>tidak</b> memiliki pembatas kuantitas
 * ({@code jumlahMinimal}/{@code jumlahMaksimal}) — sehingga cacat nilai awal {@code jumlahMaksimal}
 * yang mematikan diskon tidak berlaku di sini. Pungutan pajak selalu dikenakan tanpa memandang
 * banyaknya barang atau layanan.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Bendera aktif dua-arah</b> — dapat dinyalakan dan dimatikan.</li>
 * <li><b>Getter destruktif</b> — {@link #getJumlah()}, {@link #getAktif()}, dan seluruh relasi
 * menulis balik ke field.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — aturan pajak bersifat global lintas unit; pembatasan cakupan
 * hanya lewat {@link #getAsuransi()} dan {@link #getKomunitas()}, keduanya dengan pencocokan persis
 * (kolom kosong hanya cocok bagi pasien tanpa asuransi/komunitas).</li>
 * <li>{@code jumlah}, {@code mulai}, {@code sampai}, dan {@code aktif} tidak diberi
 * {@code @Column}, sehingga dipetakan ke kolom bernama sesuai nama properti.</li>
 * </ul>
 *
 * @see PajakDetail rincian item/tindakan/alat medis yang dikenai pajak ini
 * @see Diskon cerminan entitas ini untuk potongan harga
 * @see Biaya baris biaya tempat pajak dicatat
 * @see ais.common.CommonSirs#getPajakSekarang kueri penyaring aturan pajak yang berlaku
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "pajak_medis")
public class PajakMedis extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.pajak_medis}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah aturan pajak ini.
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
	 * Representasi teks aturan pajak untuk komponen ZK, berbentuk {@code nama-nilai%}. Tanda persen
	 * pada label menegaskan bahwa {@link #getJumlah()} menyimpan persentase, bukan nominal.
	 *
	 * @return label aturan pajak
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
	 * Mengembalikan nama pengguna yang terakhir mengubah aturan pajak ini.
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
	 * Mengembalikan cap waktu perubahan terakhir aturan pajak ini. Jangan dikacaukan dengan
	 * {@link #getMulai()}/{@link #getSampai()} yang merupakan masa berlaku domain.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode aturan pajak, wajib diisi. */
	private String kode;

	/** Nama aturan pajak (kolom bertipe {@code text}); dipakai sebagai label dan kunci pengurutan. */
	private String nama;

	/** Keterangan bebas atas aturan pajak. */
	private String keterangan;

	/** Besaran pungutan dalam <b>persen</b>, bukan rupiah. */
	private Double jumlah = 0.0;

	/** Awal masa berlaku; diinisialisasi ke waktu pembuatan objek. */
	private Date mulai = new Date();

	/**
	 * Akhir masa berlaku; diinisialisasi ke waktu pembuatan objek, sehingga aturan yang disimpan
	 * tanpa penyuntingan langsung kedaluwarsa. Lihat peringatan pada dokumentasi kelas.
	 */
	private Date sampai = new Date();

	/** Akun buku besar tempat pungutan dibukukan; kolomnya {@code NOT NULL}. */
	private Akun akun;

	/** Status berlaku aturan pajak; tanpa nilai awal, sehingga {@code null} sampai diisi. */
	private Boolean aktif;

	/** Pembatas cakupan: pajak hanya berlaku bagi peserta asuransi ini bila terisi. */
	private Asuransi asuransi;

	/** Pembatas cakupan: pajak hanya berlaku bagi anggota komunitas ini bila terisi. */
	private Komunitas komunitas;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public PajakMedis() {
	}

	/**
	 * Mengembalikan kunci utama aturan pajak.
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
	 * Mengembalikan kode aturan pajak.
	 *
	 * @return kode pajak (kolom wajib, maksimal 255 karakter)
	 */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Mengisi kode aturan pajak. Tidak ada penjaga tabrakan kode di lapisan model maupun indeks
	 * unik pada kolomnya.
	 *
	 * @param kode kode aturan pajak
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan bebas aturan pajak.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas aturan pajak.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi nama aturan pajak.
	 *
	 * @param nama nama aturan pajak
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan nama aturan pajak.
	 *
	 * @return nama aturan pajak, atau {@code null} bila tidak diisi
	 */
	@Column(name = "nama", columnDefinition = "text", nullable = true)
	public String getNama() {
		return nama;
	}

	/**
	 * Mengembalikan besaran pungutan dalam <b>persen</b>, menormalkan {@code null} menjadi
	 * {@code 0.0}. <b>Getter destruktif</b>: normalisasi ditulis balik ke field.
	 *
	 * <p>
	 * Nilai ini dipakai langsung oleh {@code CommonPendaftaranUtil} sebagai
	 * {@code amount * (getJumlah() / 100.0)} dan dijumlahkan lintas aturan oleh
	 * {@code CommonSirs.getTotalPajakDalamPersen}. Tidak ada validasi rentang, dan karena beberapa
	 * aturan pajak dapat berlaku bersamaan, total pungutan pun tidak dibatasi.
	 * </p>
	 *
	 * @return besaran pungutan dalam persen, tidak pernah {@code null}
	 */
	public Double getJumlah() {
		if (jumlah == null) {
			jumlah = 0.0;
		}
		return jumlah;
	}

	/**
	 * Mengisi besaran pungutan dalam persen.
	 *
	 * @param jumlah besaran pungutan dalam persen; tidak divalidasi rentangnya
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan awal masa berlaku aturan pajak. Getter murni-baca tanpa normalisasi, sehingga
	 * dapat mengembalikan {@code null} — dan baris semacam itu tidak akan terpilih oleh syarat
	 * {@code le(mulai, sekarang)} yang tidak mentoleransi {@code NULL}.
	 *
	 * @return awal masa berlaku, dapat {@code null}
	 */
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Mengisi awal masa berlaku aturan pajak.
	 *
	 * @param mulai awal masa berlaku
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan akhir masa berlaku aturan pajak. Getter murni-baca tanpa normalisasi.
	 *
	 * <p>
	 * Kueri penyaring mentoleransi {@code NULL} sebagai "berlaku tanpa batas akhir", tetapi field
	 * ini diinisialisasi {@code new Date()} — dan tanpa {@link TemporalType#DATE} sehingga komponen
	 * jamnya ikut tersimpan. Aturan pajak baru yang disimpan tanpa menyunting kolom ini karena itu
	 * berlaku hanya sampai detik pembuatannya dan tidak pernah terpilih, dengan akibat rumah sakit
	 * kurang memungut tanpa peringatan apa pun. Lihat peringatan pada dokumentasi kelas.
	 * </p>
	 *
	 * @return akhir masa berlaku, dapat {@code null} yang berarti tanpa batas akhir
	 */
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Mengisi akhir masa berlaku aturan pajak. Nilai {@code null} berarti berlaku tanpa batas
	 * akhir.
	 *
	 * @param sampai akhir masa berlaku, atau {@code null} untuk tanpa batas
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan akun buku besar tempat pungutan dibukukan. <b>Getter destruktif</b>
	 * ({@code check(...)}). Kolomnya {@code NOT NULL}. Akun ini juga menjadi sumber warisan bagi
	 * {@link PajakDetail#getAkun()} dan salah satu sumber warisan pada {@link Biaya#getAkun()}.
	 *
	 * @return akun buku besar pajak, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = false)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun buku besar tempat pungutan dibukukan.
	 *
	 * @param akun akun buku besar
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan pembatas cakupan asuransi. <b>Getter destruktif</b> ({@code check(...)}).
	 * Penyaringannya bersifat <b>pencocokan persis</b>: aturan pajak yang kolom asuransinya kosong
	 * hanya berlaku bagi pasien tanpa asuransi, dan sebaliknya. Berbeda dari resolusi
	 * {@link TarifKhusus} yang melonggarkan syarat secara bertingkat, pungutan "untuk semua" perlu
	 * dibuat berulang per kombinasi cakupan.
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
	 * Menetapkan pembatas cakupan asuransi aturan pajak ini.
	 *
	 * @param asuransi asuransi yang dicakup, atau {@code null}
	 */
	public void setAsuransi(Asuransi asuransi) {
		this.asuransi = asuransi;
	}

	/**
	 * Mengembalikan status berlaku aturan pajak, menormalkan {@code null} menjadi {@code true}.
	 * <b>Getter destruktif</b>. Bendera ini dua-arah.
	 *
	 * <p>
	 * Maksud "belum ditentukan berarti aktif" yang dinyatakan nilai cadangan di sini tidak dihormati
	 * oleh kueri penyaringnya: {@code CommonSirs.getPajakSekarang} memakai {@code eq(aktif, true)}
	 * yang ketat, sehingga baris dengan kolom {@code aktif} bernilai {@code NULL} tidak akan
	 * terpilih. Karena field {@link #aktif} tidak memiliki nilai awal, aturan pajak yang dibuat di
	 * luar form berisiko tersimpan dengan {@code NULL} dan diam-diam tidak memungut apa pun.
	 * </p>
	 *
	 * @return {@code true} bila aturan pajak berlaku, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyalakan atau mematikan aturan pajak. Mematikan aturan hanya mencegah pemilihannya di
	 * kemudian hari; aturan yang sudah menempel pada baris transaksi tetap diperhitungkan.
	 *
	 * @param aktif {@code true} bila aturan berlaku
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan pembatas cakupan komunitas. <b>Getter destruktif</b> ({@code check(...)}).
	 * Sama seperti {@link #getAsuransi()}, penyaringannya bersifat pencocokan persis.
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
	 * Menetapkan pembatas cakupan komunitas aturan pajak ini.
	 *
	 * @param komunitas komunitas yang dicakup, atau {@code null}
	 */
	public void setKomunitas(Komunitas komunitas) {
		this.komunitas = komunitas;
	}
}
