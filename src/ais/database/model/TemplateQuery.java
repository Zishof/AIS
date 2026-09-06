package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

/**
 * Entity <b>template query SQL tersimpan</b> pada tabel {@code public.template_query}. Satu
 * baris menyimpan satu potong teks SQL mentah ({@link #getQuery()}) berlabel
 * {@link #getNama()}, yang dikelola lewat layar admin {@code TemplateQueryAction} dan
 * dieksekusi lewat {@code ais.action.master.ExecuteTemplateQueryAction}.
 *
 * <p><b>Peringatan keamanan &mdash; eksekusi SQL dinamis nyaris tanpa parameterisasi.</b>
 * {@code ExecuteTemplateQueryAction.onExecute(Event)} mengambil isi kotak teks (yang pada
 * awalnya diisi dari {@link #getQuery()} lewat {@code onSearchDefault()}, tetapi bisa
 * <b>diedit bebas oleh pengguna sebelum dieksekusi</b>) dan menjalankannya langsung lewat
 * {@code session.createSQLQuery(q).list()} tanpa bind parameter apa pun. Satu-satunya
 * pertahanan adalah pemeriksaan substring case-insensitive yang menolak kata
 * "update"/"delete"/"truncate"/"drop"/"alter" di mana pun pada teks — <b>tidak
 * memblokir</b> pernyataan {@code INSERT}, {@code CREATE}, {@code GRANT}, {@code COPY ... TO
 * PROGRAM}, ataupun sub-query {@code SELECT} yang membaca tabel/skema di luar cakupan layar
 * ini. Akses ke layar ini hanya digerbangi hak {@code READ} biasa
 * ({@code CommonPrivilages.checkPrevilages(CommonPrivilages.READ)}) dan pemeriksaan sesi
 * login, <b>bukan</b> hak administratif — sehingga pengguna mana pun yang boleh mengakses
 * layar ini secara efektif dapat menjalankan pernyataan SQL sewenang-wenang (termasuk baca
 * data lintas tenant/tabel sensitif, atau — pada PostgreSQL dengan hak yang cukup — eksekusi
 * perintah OS lewat {@code COPY ... TO PROGRAM}). Kolom {@link #getQuery()} sendiri di
 * kelas ini hanyalah penyimpanan teks pasif; risikonya murni ada pada pemakainya di
 * {@code ExecuteTemplateQueryAction}.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "template_query")

public class TemplateQuery extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.template_query} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan
	 * diam-diam (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * masukan kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi
	 * ulang {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk
	 * dipanggil langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return nama template ini apa adanya — representasi teks ringkas baris ini. */
	public String toString() {
		return nama;
	}

	/** Nama/label template query; wajib diisi. */
	private String nama;
	/** Catatan/keterangan bebas tentang template ini; boleh {@code null}. */
	private String keterangan;
	/**
	 * Teks SQL mentah template ini; wajib diisi, dipetakan sebagai kolom {@code text}. Lihat
	 * peringatan keamanan pada Javadoc kelas — nilai ini dieksekusi apa adanya (setelah
	 * berpotensi diedit pengguna di layar) tanpa parameterisasi oleh
	 * {@code ExecuteTemplateQueryAction}.
	 */
	private String query;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public TemplateQuery() {
	}

	/**
	 * @return kunci utama baris ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama template, di-trim; {@code null} bila belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama template; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas template ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk template ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return teks SQL mentah template ini apa adanya, tanpa validasi maupun escaping apa
	 *         pun. Lihat peringatan keamanan pada Javadoc kelas.
	 */
	@Column(name = "query", nullable = false, columnDefinition = "text")
	public String getQuery() {
		return query;
	}

	/**
	 * @param query teks SQL mentah template ini; disimpan apa adanya tanpa validasi.
	 */
	public void setQuery(String query) {
		this.query = query;
	}
}