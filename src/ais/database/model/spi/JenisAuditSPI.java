package ais.database.model.spi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h2>JenisAuditSPI &mdash; Kategori Besar Audit Internal</h2>
 *
 * <p>
 * Kelas ini merepresentasikan level PALING ATAS dari hierarki data master checklist audit milik
 * modul Satuan Pengawasan Internal (SPI): sebuah kategori besar yang menjadi payung bagi seluruh
 * kriteria dan langkah uji di bawahnya. Contoh isi baris pada tabel ini: "Audit Keuangan", "Audit
 * Pengadaan Barang/Jasa", "Audit Sumber Daya Manusia", "Audit Aset/Barang Milik", "Audit
 * Operasional/Akademik", dan "Audit Teknologi Informasi". Kategori-kategori ini sengaja dibuat
 * bebas/dinamis (bukan enum tetap di kode) agar staf SPI bisa menambah jenis audit baru sendiri
 * tanpa perlu perubahan program, sesuai kebutuhan lembaga yang bisa berubah dari waktu ke waktu.
 * </p>
 *
 * <h3>Kedudukan dalam hierarki data master</h3>
 * <p>
 * Struktur data master checklist audit SPI dibuat 3 tingkat (BUKAN 5 tingkat seperti modul SPMI
 * yang meniru struktur instrumen akreditasi BAN-PT yang memang berlapis banyak) karena checklist
 * audit internal pada praktiknya cukup direpresentasikan dengan: kategori besar &rarr; standar/
 * kriteria acuan &rarr; langkah uji konkret yang dicentang di lapangan. Tiga tingkat itu adalah:
 * </p>
 * <ol>
 *   <li>{@link JenisAuditSPI} (kelas ini) &mdash; kategori besar, level 1.</li>
 *   <li>{@link KriteriaAuditSPI} &mdash; standar/aturan acuan di bawah satu jenis audit, level 2.
 *       Satu jenis audit bisa memiliki banyak kriteria.</li>
 *   <li>{@link ChecklistAuditSPI} &mdash; langkah uji/pertanyaan konkret di bawah satu kriteria,
 *       level 3 (paling bawah/daun). Inilah yang benar-benar diperiksa satu per satu oleh auditor
 *       saat pelaksanaan audit, dan menghasilkan satu baris temuan per checklist per penugasan.</li>
 * </ol>
 *
 * <h3>Mengapa perlu tabel tersendiri (bukan enum/konstanta kode)</h3>
 * <p>
 * Jenis-jenis audit yang relevan bagi satu lembaga bisa berbeda dari lembaga lain, dan bisa
 * berkembang seiring waktu (mis. penambahan "Audit Keberlanjutan/ESG" di kemudian hari). Dengan
 * menyimpannya sebagai data (bukan kode terkompilasi), staf SPI dapat menambah/menonaktifkan
 * kategori lewat layar Setup SPI tanpa melibatkan pengembang, sekaligus tetap tercatat riwayat
 * perubahannya lewat anotasi {@code @Audited} (Hibernate Envers) yang dipasang di kelas ini.
 * </p>
 *
 * <h3>Pola implementasi</h3>
 * <p>
 * Struktur field, anotasi, dan konvensi penamaan kolom kelas ini mengikuti pola baku yang dipakai
 * seluruh entity "data master sederhana" di aplikasi ini (id auto-increment, jejak audit
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang diisi otomatis oleh
 * {@code AuditTimestampInterceptor} lewat callback JPA {@code @PreUpdate}, field {@code aktif}
 * bertipe {@code Boolean} yang default-nya dianggap true bila null demi kompatibilitas data lama).
 * Pola ini SENGAJA disamakan dengan kelas-kelas sejenis lain di aplikasi (mis.
 * {@code ais.database.model.spmi.JenisSPMI}) supaya staf pengembang yang sudah familiar dengan
 * satu modul dapat langsung memahami modul lain tanpa belajar konvensi baru.
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_audit_spi")
public class JenisAuditSPI extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil ID pengguna (bukan nama) yang terakhir mengubah baris ini. Field ini adalah
	 * SHADOW dari jejak audit yang sesungguhnya sudah lengkap tercatat oleh Hibernate Envers
	 * (anotasi {@code @Audited} pada kelas ini) &mdash; keberadaannya BUKAN duplikasi yang keliru,
	 * melainkan KEHARUSAN TEKNIS: Envers menyimpan riwayat revisi lengkap di tabel `_aud` yang
	 * mahal/berat untuk sekadar menampilkan "terakhir diubah oleh siapa" pada satu baris di layar
	 * daftar, sehingga pasangan {@code oleh}/{@code olehId} ini menyediakan cara murah &amp; cepat
	 * membaca informasi tersebut tanpa query terpisah ke tabel Envers.
	 *
	 * @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi (mis. data lama sebelum field ini ditambahkan).
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna yang mengubah baris ini. SENGAJA mengabaikan nilai kosong/blank (tidak
	 * menimpa nilai yang sudah ada) agar proses simpan yang tidak membawa konteks pengguna (mis.
	 * batch/import) tidak menghapus jejak siapa yang terakhir benar-benar mengubah data secara
	 * manual.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna yang mengubah baris ini. Sama seperti {@link #setOlehId(String)},
	 * nilai kosong/blank sengaja diabaikan supaya tidak menimpa jejak yang sudah tercatat.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama (bukan ID) pengguna yang terakhir mengubah baris ini. Lihat
	 * {@link #getOlehId()} untuk penjelasan lengkap kenapa pasangan field ini sengaja ada
	 * berdampingan dengan riwayat Envers.
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang dipanggil otomatis oleh Hibernate tepat sebelum
	 * setiap operasi UPDATE dieksekusi ke database. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menyegarkan
	 * {@link #getTanggal_dirubah()} ke waktu saat ini &mdash; sehingga kode aplikasi TIDAK PERLU
	 * mengingat untuk memanggil {@code setTanggal_dirubah(new Date())} secara manual pada setiap
	 * titik yang mengubah entity ini; mekanisme ini berjalan otomatis di level persistence.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi manual waktu terakhir baris ini diubah. Dalam praktiknya field ini disegarkan
	 * otomatis lewat {@link #onUpdate()} pada setiap UPDATE, sehingga setter ini terutama dipakai
	 * saat entity pertama kali dibuat (nilai default konstruksi objek, lihat inisialisasi field)
	 * atau saat memuat ulang data historis.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil waktu terakhir baris ini diubah, diinisialisasi sejak objek dibuat ke waktu saat
	 * itu ({@link ais.ui.util.WaktuUtil#getDate()}) dan disegarkan otomatis oleh {@link #onUpdate()}
	 * setiap kali terjadi UPDATE ke database.
	 *
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini untuk keperluan log/debug &mdash; format
	 * {@code "<id>-<nama>"}, mis. "3-Audit Keuangan".
	 *
	 * @return string gabungan ID dan nama jenis audit.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public JenisAuditSPI() {
	}

	/**
	 * ID primer baris ini, di-generate otomatis oleh database (strategi {@code IDENTITY}) saat
	 * baris pertama kali disimpan. Anotasi {@code insertable = false} berarti kolom ini TIDAK
	 * pernah dikirim eksplisit dalam perintah INSERT &mdash; nilainya sepenuhnya diserahkan ke
	 * mekanisme auto-increment database, lalu dibaca kembali oleh Hibernate setelah insert sukses.
	 *
	 * @return ID unik baris ini, atau {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris ini secara manual. Jarang dipakai langsung oleh kode aplikasi karena ID
	 * biasanya diisi otomatis oleh Hibernate setelah INSERT (lihat {@link #getId()}); berguna
	 * terutama saat membangun objek referensi ringan (mis. hanya untuk keperluan {@code JoinColumn}
	 * pada relasi tanpa memuat seluruh baris dari database).
	 *
	 * @param id ID baris yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode singkat/mnemonic opsional untuk jenis audit ini (mis. "AK" untuk Audit Keuangan),
	 * dipakai sebagai identitas ringkas pada laporan/penomoran dokumen tanpa harus mengulang nama
	 * lengkap. Boleh kosong karena tidak semua lembaga membutuhkan pengkodean formal.
	 *
	 * @return kode singkat jenis audit, atau {@code null} bila belum diisi.
	 */
	@Column(name = "kode", nullable = true, columnDefinition = "text")
	public String getKode() {
		return kode;
	}

	/**
	 * Mengisi kode singkat jenis audit ini.
	 *
	 * @param kode kode singkat/mnemonic baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama kategori jenis audit ini (mis. "Audit Keuangan", "Audit Pengadaan Barang/Jasa") &mdash;
	 * lihat javadoc kelas untuk daftar contoh lengkap. Nilai yang dikembalikan sudah di-{@code trim()}
	 * agar spasi tak sengaja di awal/akhir input pengguna tidak lolos ke tampilan/laporan.
	 *
	 * @return nama jenis audit yang sudah dipangkas spasinya, atau {@code null} bila belum diisi
	 *         (meski kolom database bersifat {@code nullable = false} pada baris yang sudah tersimpan).
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama kategori jenis audit ini.
	 *
	 * @param nama nama jenis audit baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tambahan mengenai jenis audit ini, mis. penjelasan ruang lingkup atau
	 * catatan kapan kategori ini mulai dipakai.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk jenis audit ini.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif/nonaktif baris ini. SENGAJA memperlakukan nilai {@code null} sebagai
	 * {@code true} (aktif) &mdash; konvensi yang dipakai konsisten di seluruh entity "data master
	 * sederhana" pada aplikasi ini agar baris data LAMA yang dibuat sebelum kolom {@code aktif}
	 * ditambahkan ke skema tidak tiba-tiba dianggap nonaktif/hilang dari daftar pilihan begitu
	 * kolom ini diperkenalkan.
	 *
	 * @return {@code true} bila jenis audit ini aktif/boleh dipakai (termasuk saat nilai tersimpan
	 *         {@code null}), {@code false} bila sudah dinonaktifkan secara eksplisit.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif jenis audit ini. Menonaktifkan (bukan menghapus) baris ini
	 * adalah cara yang dianjurkan untuk "membuang" kategori yang sudah tidak relevan lagi, karena
	 * baris yang benar-benar dihapus akan merusak integritas referensial baris-baris
	 * {@link KriteriaAuditSPI}, {@link RencanaAuditTahunanSPI}, maupun {@link PenugasanAuditSPI}
	 * yang sudah pernah mengacu ke sini.
	 *
	 * @param aktif status baru; {@code null} akan diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
