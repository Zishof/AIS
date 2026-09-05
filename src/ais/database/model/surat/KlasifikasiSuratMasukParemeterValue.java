package ais.database.model.surat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * NILAI ISIAN satu parameter tambahan dinamis untuk SATU {@link SuratMasuk} tertentu -- daun
 * terakhir dari rantai konfigurasi field dinamis modul surat masuk.
 *
 * <h3>Rantai relasi (SUDAH DIVERIFIKASI dari kode, bukan asumsi)</h3>
 *
 * <p>Rantainya hanya DUA tingkat, bukan tiga seperti pola parameter tambahan di modul lain:</p>
 *
 * <pre>
 * KlasifikasiSuratMasuk            (katalog / kategori surat masuk)
 *   &darr; klasifikasi_surat_masuk
 * KlasifikasiSuratMasukParemeter   (DEFINISI field: nama, key, tipe, nilai default)
 *   &darr; klasifikasi_surat_masuk_parameter          &nbsp;&nbsp;+ surat_masuk &rarr; {@link SuratMasuk}
 * KlasifikasiSuratMasukParemeterValue  (NILAI yang diketik operator untuk satu surat)
 * </pre>
 *
 * <p>PENTING: rantai ini BUKAN mekanisme generik {@code ais.database.model.ParameterTambahan} /
 * {@code KelompokParameterTambahan*} yang dipakai modul aset, penggajian, dan koperasi. Tidak ada
 * tingkat "kelompok parameter", tidak ada kolom urutan, tidak ada flag {@code aktif} pada
 * definisi parameter, dan tidak ada {@code ParameterTambahan} bersama yang dipakai lintas modul.
 * Modul surat masuk memiliki implementasi TERSENDIRI yang kebetulan mirip secara konsep saja.
 * Kembarannya untuk surat keluar adalah {@code KlasifikasiSuratKeluarParemeter} /
 * {@code KlasifikasiSuratKeluarParemeterValue} dengan bentuk yang sama.</p>
 *
 * <h3>Kolom {@code nama} menyimpan NILAI, bukan nama (salah penamaan yang harus diingat)</h3>
 *
 * <p>Nama field {@link #getNama()} menyesatkan: yang disimpan di sini adalah NILAI hasil ketikan
 * operator pada formulir surat masuk, sedangkan LABEL field dinamis dipegang oleh
 * {@link KlasifikasiSuratMasukParemeter#getNama()}. Hal ini diverifikasi pada empat cabang
 * penyimpanan di {@code ais.action.master.surat.SuratMasukAction} yang semuanya memanggil
 * {@code klasifikasiSuratMasukParemeterValue.setNama(<isi widget>)}, dan pada perakitan tampilan
 * ringkas yang menuliskan {@code paremeter.getNama() + " : " + value.getNama()}. Jadi pasangan
 * "label : nilai" berasal dari DUA entitas berbeda yang kebetulan memakai nama properti sama.</p>
 *
 * <h3>Semua tipe disimpan sebagai teks</h3>
 *
 * <p>{@link KlasifikasiSuratMasukParemeter#getTipe()} boleh bernilai {@code java.lang.String},
 * {@code java.lang.Integer}, {@code java.lang.Double}, atau {@code java.util.Date} (empat pilihan
 * yang disediakan {@code KlasifikasiSuratMasukParameterHelper}). Apa pun tipenya, nilainya tetap
 * ditulis ke kolom {@code nama} bertipe {@code varchar(255)}: angka lewat {@code Intbox}/
 * {@code Doublebox} yang di-{@code toString}, dan tanggal lewat {@code Common.dateFormat2}.
 * Pembacaan balik memakai {@code Integer.parseInt}/{@code Double.parseDouble}/{@code parse}
 * di dalam {@code try/catch} yang, bila gagal, DIAM-DIAM jatuh ke nilai bawaan (0, 0.0, atau
 * tanggal hari ini) sehingga perubahan tipe parameter setelah ada isian lama tidak menimbulkan
 * error yang terlihat, melainkan pergeseran nilai tanpa peringatan.</p>
 *
 * <h3>Tidak ada penjamin keunikan pasangan (parameter, surat)</h3>
 *
 * <p>Tidak ada {@code @UniqueConstraint} pada pasangan {@code klasifikasi_surat_masuk_parameter}
 * dan {@code surat_masuk}. Semua pembaca di {@code SuratMasukAction} memakai
 * {@code .setMaxResults(1).uniqueResult()} sehingga bila baris ganda terlanjur ada, hanya SATU
 * yang terbaca dan disunting, sisanya menjadi data hantu yang tetap ikut tercetak pada daftar
 * ringkas parameter. Perlindungan yang ada hanya bersifat prosedural: setiap listener membaca
 * dulu baris yang cocok, dan hanya membuat baris baru bila hasilnya {@code null}.</p>
 *
 * <p>Selain itu, penghapusan sebuah definisi parameter di
 * {@code KlasifikasiSuratMasukParameterHelper} memanggil {@code session.delete(...)} secara
 * telanjang tanpa lebih dulu membersihkan baris nilai anaknya; kelas induk tidak memiliki koleksi
 * ber-cascade ke sini. Akibatnya bergantung pada ada/tidaknya foreign key di basis data:
 * penghapusan ditolak, atau baris nilai menjadi yatim.</p>
 *
 * @see KlasifikasiSuratMasukParemeter definisi field dinamis (label, key, tipe, nilai bawaan)
 * @see KlasifikasiSuratMasuk katalog klasifikasi tempat definisi parameter bergantung
 * @see SuratMasuk dokumen surat masuk yang memiliki isian ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "klasifikasi_surat_masuk_paremeter_value")
public class KlasifikasiSuratMasukParemeterValue extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java; nilainya sama dengan hampir seluruh entitas hasil templat
	 * hbm2java di basis kode ini karena disalin dari templat generator yang sama, bukan karena
	 * kelas-kelas tersebut kompatibel secara biner satu sama lain.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, kolom {@code id}; dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nama tampil pengguna yang terakhir menyunting baris ini (jejak audit bayangan). */
	private String oleh;
	/** Id pengguna yang terakhir menyunting baris ini (jejak audit bayangan). */
	private String olehId;

	/**
	 * Id pengguna penyunting terakhir.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * <p>Pengabaian nilai kosong adalah KEHARUSAN TEKNIS, bukan cacat: baris nilai parameter
	 * kerap disimpan ulang oleh listener widget yang berjalan tanpa konteks pengguna aktif
	 * (mis. saat pemuatan ulang formulir). Bila setter ini menerima nilai kosong, jejak audit
	 * pengubah terakhir akan terhapus oleh proses yang bukan perbuatan manusia.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris ini, yaitu ISI NILAI parameter (field {@code nama}) apa adanya.
	 *
	 * <p>Perhatikan bahwa nilai yang dikembalikan diambil langsung dari field, BUKAN lewat
	 * {@link #getNama()}, sehingga hasilnya bisa {@code null} untuk baris yang belum pernah
	 * diisi -- berbeda dari {@link #getNama()} yang menormalkan {@code null} menjadi teks
	 * kosong. Metode ini dipakai oleh komponen ZK yang menampilkan objek secara generik.</p>
	 *
	 * @return isi nilai parameter, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna penyunting terakhir.
	 *
	 * @return nama pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA: menyerahkan pembaruan stempel waktu dan identitas pengubah
	 * ke {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate menulis perubahan
	 * baris ini ke basis data.
	 *
	 * <p>Kait ini hanya berjalan pada operasi UPDATE, bukan INSERT; nilai awal
	 * {@code tanggal_dirubah} karena itu diberikan lewat inisialisasi field pada deklarasi yang
	 * ditulis pada baris yang sama dengan metode ini (gaya penulisan hasil penyisipan otomatis
	 * di seluruh basis kode ini). Karena kelas ini juga beranotasi {@code @Audited}, Envers
	 * tetap menyimpan riwayat versi terpisah; field {@code oleh}/{@code olehId}/
	 * {@code tanggal_dirubah} adalah jejak audit BAYANGAN yang menempel pada baris hidup agar
	 * tampilan tidak perlu menyentuh tabel revisi Envers untuk sekadar menampilkan "diubah oleh
	 * siapa, kapan" -- ini keharusan teknis, bukan duplikasi yang keliru.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Biasanya tidak dipanggil kode aplikasi karena
	 * {@link #onUpdate()} sudah mengurusnya; disediakan untuk kebutuhan impor/perbaikan data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru
	 *         karena field diinisialisasi ke waktu sekarang saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * ISI NILAI parameter yang diketik operator -- BUKAN nama parameter. Label field dinamis
	 * ada di {@link KlasifikasiSuratMasukParemeter#getNama()}. Lihat javadoc kelas.
	 */
	private String nama;
	/**
	 * Catatan bebas untuk baris nilai ini. Tidak ada satu pun formulir surat masuk yang mengisi
	 * atau menampilkannya; kolom ini hanya terisi lewat mekanisme impor/CRUD generik.
	 */
	private String keterangan;
	/** DEFINISI field dinamis yang nilainya disimpan baris ini. */
	private KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter;
	/** Dokumen surat masuk pemilik nilai ini. */
	private SuratMasuk suratMasuk;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate. Baris yang dibuat lewat konstruktor ini
	 * belum sah untuk disimpan sampai {@link #setKlasifikasiSuratMasukParemeter},
	 * {@link #setSuratMasuk}, dan {@link #setNama} diisi -- kolom {@code nama} beranotasi
	 * {@code nullable = false}.
	 */
	public KlasifikasiSuratMasukParemeterValue() {
	}

	/**
	 * Kunci utama baris.
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
	 * Mengisi kunci utama. Hanya dipakai Hibernate dan proses impor; kode aplikasi tidak boleh
	 * memanggilnya untuk baris yang sudah tersimpan.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * ISI NILAI parameter tambahan untuk surat masuk ini, sudah dinormalkan.
	 *
	 * <p>Getter ini BERSIFAT DESTRUKTIF terhadap state objek: bila field masih {@code null},
	 * field diubah menjadi teks kosong lebih dulu, baru dikembalikan. Karena kelas beranotasi
	 * {@code dynamicUpdate = true}, perubahan senyap ini ikut ter-flush ke basis data pada
	 * transaksi berjalan -- sebuah baris yang semula {@code NULL} akan tersimpan sebagai
	 * string kosong hanya karena pernah dibaca. Pada praktiknya hal itu justru menguntungkan
	 * di sini, sebab kolomnya dideklarasikan {@code nullable = false}; tetapi polanya perlu
	 * disadari karena tidak dapat dibedakan dari pembacaan yang murni membaca.</p>
	 *
	 * <p>Perhatikan pula cabang terakhir {@code this.nama == null ? null : this.nama.trim()}
	 * tidak pernah bisa menghasilkan {@code null} lagi setelah inisialisasi di atas berjalan;
	 * cabang itu sisa dari templat generator dan bersifat mati secara efektif.</p>
	 *
	 * @return nilai parameter yang sudah di-{@code trim}; teks kosong bila belum diisi,
	 *         tidak pernah {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (nama == null) {
			nama = "";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi NILAI parameter. Pemanggil di {@code SuratMasukAction} selalu mengirim hasil
	 * {@code trim()} dari widget, atau hasil format {@code Common.dateFormat2} untuk parameter
	 * bertipe tanggal.
	 *
	 * @param nama nilai parameter dalam bentuk teks
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas baris ini.
	 *
	 * @return catatan bebas, atau {@code null} bila tidak ada
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas baris ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * DEFINISI field dinamis yang nilainya dipegang baris ini.
	 *
	 * <p>Berbeda dari mayoritas getter relasi di paket ini, getter ini TIDAK memanggil
	 * {@code check(...)} untuk membongkar proxy lazy -- dan memang tidak perlu: relasi ini
	 * dibiarkan pada mode pengambilan bawaan {@code @ManyToOne} (EAGER) dan dipertegas dengan
	 * {@code @Fetch(FetchMode.SELECT)}, sehingga Hibernate sudah memuatkan objek nyata lewat
	 * SELECT terpisah, bukan proxy. Konsekuensinya: setiap pembacaan daftar nilai parameter
	 * membangkitkan satu query tambahan per baris (pola N+1 yang disengaja demi kesederhanaan),
	 * dan kode pemanggil pada perakitan ringkasan surat memang mengimbanginya dengan
	 * {@code createAlias} + {@code addOrder} agar urutan tampilan mengikuti nama definisi.</p>
	 *
	 * <p>Perhatikan ketidakkonsistenan ejaan yang harus dipertahankan: nama kelas dan tabel
	 * memakai "paremeter", sedangkan kolom kunci asingnya bernama
	 * {@code klasifikasi_surat_masuk_parameter} dengan ejaan benar. Menyeragamkan salah satunya
	 * akan memutus pemetaan terhadap basis data yang sudah berjalan.</p>
	 *
	 * @return definisi parameter induk, atau {@code null} untuk baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "klasifikasi_surat_masuk_parameter", nullable = true)
	public KlasifikasiSuratMasukParemeter getKlasifikasiSuratMasukParemeter() {
		return klasifikasiSuratMasukParemeter;
	}

	/**
	 * Menautkan baris nilai ini ke definisi parameternya.
	 *
	 * @param klasifikasiSuratMasukParemeter definisi parameter induk
	 */
	public void setKlasifikasiSuratMasukParemeter(KlasifikasiSuratMasukParemeter klasifikasiSuratMasukParemeter) {
		this.klasifikasiSuratMasukParemeter = klasifikasiSuratMasukParemeter;
	}

	/**
	 * Dokumen surat masuk pemilik nilai ini.
	 *
	 * <p>Sama seperti relasi ke definisi parameter, getter ini tidak membongkar proxy karena
	 * relasinya EAGER dengan {@code @Fetch(FetchMode.SELECT)}. Pasangan
	 * (definisi parameter, surat masuk) inilah yang dipakai seluruh pembaca sebagai kunci
	 * logis baris; lihat javadoc kelas mengenai ketiadaan penjamin keunikan atas pasangan
	 * tersebut.</p>
	 *
	 * @return surat masuk pemilik nilai, atau {@code null} untuk baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_masuk", nullable = true)
	public SuratMasuk getSuratMasuk() {
		return suratMasuk;
	}

	/**
	 * Menautkan baris nilai ini ke dokumen surat masuk pemiliknya.
	 *
	 * @param suratMasuk dokumen surat masuk pemilik nilai
	 */
	public void setSuratMasuk(SuratMasuk suratMasuk) {
		this.suratMasuk = suratMasuk;
	}

}
