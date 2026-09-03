package ais.database.model.akunting;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Penanda anti-posting-ganda untuk {@link ais.action.servlet.api.JurnalPenyesuaianHelper} &mdash;
 * satu baris = satu (template, periode) yang <b>sudah</b> diposting.
 *
 * <h2>Kenapa entity ini ada</h2>
 * <p>Sebelumnya satu-satunya penjaga adalah penanda TEKS {@code [PENYESUAIAN <id> <periode>]}
 * yang ditempel pada {@link PostingHistory#getKeterangan()} lalu dicari lewat {@code LIKE} sebelum
 * memposting (lihat Javadoc {@link TemplateJurnalPenyesuaian} butir 5 untuk riwayat lengkap
 * celahnya). Penjaga teks itu punya beberapa kelemahan struktural yang tidak bisa ditutup tanpa
 * kunci basis data sungguhan: rentan TOCTOU pada permintaan bersamaan, penandanya hilang bila
 * template dihapus lalu dibuat ulang (id baru), dan bisa terpotong diam-diam bila nama template
 * panjang mendorong keterangan melewati batas kolom {@code varchar(255)}.</p>
 * <p>Tabel ini menutup ketiganya sekaligus dengan {@code UNIQUE (template_id, periode)} yang
 * ditegakkan basis data &mdash; bukan query {@code SELECT} lalu {@code INSERT} yang bisa diselip
 * permintaan lain di antaranya. {@code JurnalPenyesuaianHelper.jalankan(...)} menulis baris ini
 * TEPAT SEBELUM membuat {@link PostingHistory} pada transaksi yang sama; pelanggaran unique
 * constraint (dua permintaan bersamaan menembak (template, periode) yang sama) ditangkap sebagai
 * {@code org.hibernate.exception.ConstraintViolationException} dan diperlakukan sebagai "sudah
 * diposting" &mdash; pola yang sama dipakai {@code KantinHelper} untuk idempotensi buka sesi kas.</p>
 *
 * <h2>Kompatibilitas mundur</h2>
 * <p>Periode yang sudah diposting SEBELUM tabel ini ada tidak punya baris di sini. Karena itu
 * {@code JurnalPenyesuaianHelper.sudahDiposting(...)} tetap memeriksa penanda teks lama sebagai
 * fallback -- tabel ini adalah sumber kebenaran untuk posting BARU, bukan pengganti retroaktif
 * tanpa migrasi data historis (migrasi tersebut belum dijalankan; perlu kredensial basis data
 * produksi untuk mem-back-fill dari {@code posting_history.keterangan} lama).</p>
 *
 * <h2>{@code templateId} sengaja BUKAN relasi JPA</h2>
 * <p>Kolom ini kolom biasa ({@code Long}), bukan {@code @ManyToOne} ke
 * {@link TemplateJurnalPenyesuaian}. Menghapus template TIDAK menghapus jurnal yang sudah
 * terbentuk (baris {@link PostingHistory}/{@link Transaksi} tetap ada -- lihat Javadoc
 * {@link TemplateJurnalPenyesuaian} butir 4), sehingga baris penanda ini pun harus tetap bertahan
 * setelah template-nya dihapus; relasi JPA dengan cascade delete akan menghapus jejak itu dan
 * membuka kembali celah "hapus lalu buat ulang template = id baru = penanda hilang". Sebagai
 * gantinya {@code JurnalPenyesuaianHelper.hapus(...)} menolak menghapus template yang sudah punya
 * baris penanda sama sekali -- lihat Javadoc method itu.</p>
 *
 * @see ais.action.servlet.api.JurnalPenyesuaianHelper
 * @see TemplateJurnalPenyesuaian
 * @see PostingHistory
 */
@Entity
@Table(schema = "akunting", name = "penanda_jurnal_penyesuaian",
		uniqueConstraints = @UniqueConstraint(name = "uq_penanda_jurnal_penyesuaian_template_periode",
				columnNames = { "template_id", "periode" }))
public class PenandaJurnalPenyesuaian implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Kunci utama, dibangkitkan basis data. */
	private Long id;
	/** Id {@link TemplateJurnalPenyesuaian}; kolom biasa, BUKAN relasi JPA -- lihat Javadoc kelas. */
	private Long templateId;
	/** Periode berformat {@code yyyy-MM}, sudah divalidasi ketat oleh pemanggil sebelum ditulis. */
	private String periode;
	/** Id {@link PostingHistory} yang terbentuk dari penanda ini; untuk telusur, boleh {@code null}. */
	private Long postingHistoryId;
	/** Id pengguna yang memposting; jejak audit ringan, boleh {@code null}. */
	private String olehId;
	/** Waktu baris ini ditulis. */
	private Date dibuatPada = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib Hibernate. */
	public PenandaJurnalPenyesuaian() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "template_id", nullable = false)
	public Long getTemplateId() {
		return templateId;
	}

	public void setTemplateId(Long templateId) {
		this.templateId = templateId;
	}

	@Column(name = "periode", nullable = false, length = 7)
	public String getPeriode() {
		return periode;
	}

	public void setPeriode(String periode) {
		this.periode = periode;
	}

	@Column(name = "posting_history_id", nullable = true)
	public Long getPostingHistoryId() {
		return postingHistoryId;
	}

	public void setPostingHistoryId(Long postingHistoryId) {
		this.postingHistoryId = postingHistoryId;
	}

	@Column(name = "oleh_id", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	@Column(name = "dibuat_pada", nullable = false)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}
}
