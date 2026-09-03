package ais.database.model.employ;

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
 * Entity JPA/Hibernate yang memetakan tabel {@code employ.diklat}: KATALOG/MASTER DATA kegiatan
 * diklat yang dikelola admin lewat {@code ais.action.master.employ.DiklatAction} — nama kegiatan,
 * jenis ({@link JenisDiklat}), rentang tanggal pelaksanaan, penyelenggara, dan informasi
 * sertifikat (nomor serta tahun).
 *
 * <p><b>PENTING — bukan riwayat personal pegawai:</b> kelas ini TIDAK punya field/relasi ke
 * {@link ais.database.model.Pegawai} sama sekali, berbeda dari klaster "riwayat pegawai" di paket
 * ini (mis. {@link RiwayatPelatihanPegawai}) yang selalu punya relasi {@code @ManyToOne} wajib ke
 * {@code Pegawai}. Riwayat pelatihan personal yang benar-benar diikuti seorang pegawai dicatat
 * lewat {@link RiwayatPelatihanPegawai} (field {@code nama}/{@code jenisPelatihan} miliknya
 * sendiri, merujuk {@link JenisPelatihan} — BUKAN {@link JenisDiklat}); kelas {@code Diklat} ini
 * dan {@code RiwayatPelatihanPegawai} TIDAK saling merujuk satu sama lain dalam kode — dua jalur
 * data yang independen meski namanya mirip secara semantik ("diklat" vs "pelatihan"). Berdasarkan
 * pencarian referensi di seluruh kode, hanya {@code DiklatAction} (layar CRUD admin dengan
 * validasi nama unik lewat {@code checkNamaDiklat()}) dan {@code DiklatDaoImpl}/{@code DiklatDao}
 * (DAO generik tanpa logika tambahan) yang memakai kelas ini secara langsung — tidak ada entity
 * lain yang punya relasi {@code @ManyToOne}/{@code @JoinColumn} ke tabel {@code employ.diklat};
 * kelas ini berfungsi murni sebagai daftar referensi/katalog yang berdiri sendiri, bukan sebagai
 * target foreign-key dari entity lain.
 *
 * <p>Field {@link #noSertifikat} dan {@link #tahunSertifikat} pada level katalog ini agak tidak
 * lazim untuk sebuah "master data kegiatan" (nomor sertifikat biasanya bersifat per-peserta, bukan
 * per-kegiatan) — kemungkinan sisa desain awal atau dipakai untuk mencatat sertifikat
 * institusional/akreditasi kegiatan itu sendiri, bukan sertifikat perorangan. Tidak ada bukti di
 * kode yang menjelaskan pemakaian pastinya lebih lanjut.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see JenisDiklat
 * @see RiwayatPelatihanPegawai
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "diklat")



public class Diklat extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap dari kelas ini.
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Primary key baris diklat, dibangkitkan otomatis oleh database (IDENTITY). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini — field audit shadow, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini — field audit shadow, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang tercatat terakhir kali mengubah baris diklat ini. Field
	 * audit shadow murni tekstual (bukan foreign key), tidak divalidasi terhadap tabel pengguna;
	 * hanya untuk jejak audit tampilan, bukan sumber kebenaran untuk otorisasi.
	 *
	 * @return id pengguna terakhir yang mengubah, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {return olehId;}

	/**
	 * Mengisi id pengguna pengubah terakhir. Setter ini mengabaikan diam-diam nilai {@code null}
	 * atau string kosong/whitespace-only — tidak bisa dipakai untuk mengosongkan kembali nilai
	 * yang sudah tersimpan.
	 *
	 * @param olehId id pengguna pengubah; nilai null/kosong diabaikan tanpa efek.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/** Catatan/keterangan bebas mengenai baris diklat ini; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Nama kegiatan diklat; lihat {@link #getNama()}. */
	private String nama;
	/** Jenis/kategori diklat; lihat {@link #getJenisDiklat()}. */
	private JenisDiklat jenisDiklat;
	/** Tanggal mulai pelaksanaan diklat; lihat {@link #getTanggalMulai()}. */
	private Date tanggalMulai;
	/** Tanggal selesai pelaksanaan diklat; lihat {@link #getTanggalSelesai()}. */
	private Date tanggalSelesai;
	/** Nomor sertifikat terkait kegiatan diklat ini; lihat {@link #getNoSertifikat()}. */
	private String noSertifikat;
	/** Tahun penerbitan sertifikat terkait kegiatan diklat ini; lihat {@link #getTahunSertifikat()}. */
	private String tahunSertifikat;
	/** Nama penyelenggara kegiatan diklat; lihat {@link #getPenyelenggara()}. */
	private String penyelenggara;

	/**
	 * Hook siklus hidup JPA {@code @PreUpdate}, dipanggil otomatis oleh Hibernate sebelum
	 * statement UPDATE dieksekusi, mendelegasikan pencatatan stempel waktu ke
	 * {@code AuditTimestampInterceptor.ubah(this)}. Field {@link #tanggal_dirubah} juga
	 * diinisialisasi eager saat konstruksi objek lewat {@code WaktuUtil.getDate()}, sehingga ada
	 * dua jalur penulisan: nilai awal saat objek dibuat, dan nilai yang ditimpa otomatis tepat
	 * sebelum UPDATE — {@link #setTanggal_dirubah(Date)} dapat menimpa keduanya secara manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengembalikan primary key baris diklat ini, dibangkitkan otomatis oleh database lewat
	 * strategi {@link javax.persistence.GenerationType#IDENTITY}.
	 *
	 * @return id baris, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id baris diklat secara manual. Kolom {@code id} dipetakan {@code insertable =
	 * false} sehingga pengisian di sini tidak terbawa ke statement INSERT.
	 *
	 * @param id id baris yang ingin diset.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; berlaku aturan pengabaian null/kosong yang sama
	 * dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; nilai null/kosong diabaikan tanpa efek.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang tercatat terakhir kali mengubah baris diklat ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}


	/**
	 * Mengatur stempel waktu perubahan terakhir secara manual, memotong jalur otomatis di
	 * {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris diklat ini.
	 *
	 * @return tanggal-waktu perubahan terakhir (tipe {@code TIMESTAMP} di database).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string default entity ini, mengembalikan field {@link #keterangan} apa
	 * adanya. Dapat mengembalikan {@code null} bila {@link #keterangan} belum diisi — pemanggil
	 * yang menggabungkan hasilnya ke {@link String} lain (mis. lewat konkatenasi {@code +})
	 * sebaiknya berjaga-jaga terhadap kemungkinan literal {@code "null"} yang muncul.
	 *
	 * @return isi field {@link #keterangan}, dapat berupa {@code null}.
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengembalikan catatan/keterangan bebas mengenai kegiatan diklat ini.
	 *
	 * @return isi keterangan, dapat berupa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan/keterangan bebas mengenai kegiatan diklat ini.
	 *
	 * @param keterangan teks keterangan baru, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}


	/**
	 * Mengembalikan nama kegiatan diklat ini, dipetakan {@code nullable = false} di level skema
	 * (tanpa validasi non-null eksplisit di setter — pengecekan wajib-isi dilakukan di lapisan UI
	 * {@code DiklatAction.onSave(Event)}). Nama juga divalidasi unik oleh pemanggil lewat
	 * {@code DiklatAction.checkNamaDiklat()} sebelum disimpan.
	 *
	 * @return nama kegiatan diklat.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	/**
	 * Mengisi nama kegiatan diklat ini.
	 *
	 * @param nama nama kegiatan diklat baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan jenis/kategori diklat ({@link JenisDiklat}) pada baris ini. Relasi dipetakan
	 * {@code @ManyToOne(cascade = {PERSIST, MERGE})} TANPA {@code fetch} eksplisit (default JPA
	 * EAGER), dikombinasikan dengan {@code @Fetch(FetchMode.SELECT)} dari Hibernate yang memaksa
	 * pemuatan lewat query {@code SELECT} terpisah alih-alih di-JOIN ke query utama. Kolom join
	 * {@code jenis_diklat} bersifat {@code nullable = true}, sehingga baris diklat boleh tidak
	 * memiliki jenis yang terklasifikasi.
	 *
	 * @return jenis diklat pada baris ini, dapat berupa {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_diklat", nullable = true)
	public JenisDiklat getJenisDiklat() {
		return jenisDiklat;
	}

	/**
	 * Mengisi jenis/kategori diklat pada baris ini.
	 *
	 * @param jenisDiklat jenis diklat baru, boleh {@code null}.
	 */
	public void setJenisDiklat(JenisDiklat jenisDiklat) {
		this.jenisDiklat = jenisDiklat;
	}

	/**
	 * Mengembalikan tanggal mulai pelaksanaan kegiatan diklat ini.
	 *
	 * @return tanggal mulai diklat.
	 */
	@Column(name = "tanggal_mulai")
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	/**
	 * Mengisi tanggal mulai pelaksanaan kegiatan diklat ini.
	 *
	 * @param tanggalMulai tanggal mulai baru.
	 */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Mengembalikan tanggal selesai pelaksanaan kegiatan diklat ini. Tidak ada validasi yang
	 * memastikan tanggal ini tidak mendahului {@link #getTanggalMulai()} — konsistensi rentang
	 * tanggal sepenuhnya bergantung pada lapisan pemanggil (action).
	 *
	 * @return tanggal selesai diklat.
	 */
	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Mengisi tanggal selesai pelaksanaan kegiatan diklat ini.
	 *
	 * @param tanggalSelesai tanggal selesai baru.
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Mengembalikan nomor sertifikat terkait kegiatan diklat ini. Lihat catatan pada javadoc
	 * kelas mengenai kejanggalan menaruh nomor sertifikat pada level katalog kegiatan
	 * (bukan pada baris riwayat personal per peserta).
	 *
	 * @return nomor sertifikat, dapat berupa {@code null}.
	 */
	@Column(name = "no_sertifikat")
	public String getNoSertifikat() {
		return noSertifikat;
	}

	/**
	 * Mengisi nomor sertifikat terkait kegiatan diklat ini.
	 *
	 * @param noSertifikat nomor sertifikat baru, boleh {@code null}.
	 */
	public void setNoSertifikat(String noSertifikat) {
		this.noSertifikat = noSertifikat;
	}

	/**
	 * Mengembalikan tahun penerbitan sertifikat terkait kegiatan diklat ini. Dipetakan sebagai
	 * {@code String} (bukan tipe numerik/tanggal), sehingga tidak ada validasi format tahun di
	 * level model.
	 *
	 * @return tahun sertifikat, dapat berupa {@code null}.
	 */
	@Column(name = "tahun_sertifikat")
	public String getTahunSertifikat() {
		return tahunSertifikat;
	}

	/**
	 * Mengisi tahun penerbitan sertifikat terkait kegiatan diklat ini.
	 *
	 * @param tahunSertifikat tahun sertifikat baru, boleh {@code null}.
	 */
	public void setTahunSertifikat(String tahunSertifikat) {
		this.tahunSertifikat = tahunSertifikat;
	}

	/**
	 * Mengembalikan nama penyelenggara kegiatan diklat ini.
	 *
	 * @return nama penyelenggara, dapat berupa {@code null}.
	 */
	@Column(name = "penyelenggara")
	public String getPenyelenggara() {
		return penyelenggara;
	}

	/**
	 * Mengisi nama penyelenggara kegiatan diklat ini.
	 *
	 * @param penyelenggara nama penyelenggara baru, boleh {@code null}.
	 */
	public void setPenyelenggara(String penyelenggara) {
		this.penyelenggara = penyelenggara;
	}

}
