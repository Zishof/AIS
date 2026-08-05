package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Kamus terjemahan hasil <b>pembelajaran AI</b> (Ollama) yang bersifat <b>persisten</b>.
 *
 * <p>Ketika mesin AI menerjemahkan sebuah segmen teks yang belum ada di kamus internal,
 * hasilnya disimpan di tabel ini (kunci = pasangan {@code sumber_hash} + {@code lang})
 * sehingga (1) tidak perlu memanggil AI lagi untuk teks yang sama, (2) hasilnya tetap ada
 * setelah aplikasi di-restart, dan (3) kamus terjemahan tumbuh seiring pemakaian. Dipakai
 * oleh {@code TerjemahAiHelper}.</p>
 *
 * <p>Tabel dibuat otomatis oleh Hibernate ({@code hbm2ddl.auto=update}). {@code sumber_hash}
 * (MD5 heksадesimal) dipakai sebagai kunci unik agar aman untuk teks panjang tanpa melampaui
 * batas ukuran indeks basis data.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "kamus_terjemah",
		uniqueConstraints = @UniqueConstraint(columnNames = { "sumber_hash", "lang" }))
public class KamusTerjemah extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String lang;
	private String sumberHash;
	private String sumber;
	private String hasil;
	private Date waktu;

	public KamusTerjemah() {
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
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

	/** Kode bahasa tujuan yang dinormalisasi (english / arab / mandarin). */
	@Column(name = "lang", nullable = false, length = 16)
	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	/** MD5 heksadesimal dari teks sumber (dinormalisasi) — kunci unik bersama {@code lang}. */
	@Column(name = "sumber_hash", nullable = false, length = 40)
	public String getSumberHash() {
		return sumberHash;
	}

	public void setSumberHash(String sumberHash) {
		this.sumberHash = sumberHash;
	}

	@Column(name = "sumber", columnDefinition = "text")
	public String getSumber() {
		return sumber;
	}

	public void setSumber(String sumber) {
		this.sumber = sumber;
	}

	@Column(name = "hasil", columnDefinition = "text")
	public String getHasil() {
		return hasil;
	}

	public void setHasil(String hasil) {
		this.hasil = hasil;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	public String toString() {
		return id + "-" + lang + "-" + sumberHash;
	}
}
