package ais.database.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Catatan idempotensi mutasi offline-first (Tahap C).
 *
 * Klien mobile mengantre mutasi non-kritis dengan {@code clientMutationId}
 * yang stabil; ketika respons server hilang di jaringan, retry membawa id
 * yang sama. Baris pada tabel ini membuat retry mengembalikan respons lama
 * alih-alih menjalankan operasi bisnis dua kali.
 *
 * Tabel dibuat otomatis oleh {@code hbm2ddl.auto=update} saat aplikasi
 * dinyalakan; tidak ada migrasi manual.
 */
@Entity
@Table(name = "mutasi_idempoten", uniqueConstraints = @UniqueConstraint(
		name = "uk_mutasi_idempoten_kunci",
		columnNames = { "pengguna", "aksi", "client_mutation_id" }))
public class MutasiIdempoten {

	private Long id;
	private String pengguna;
	private String aksi;
	private String clientMutationId;
	private String respons;
	private Date tanggal = new Date();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** UserId pemilik mutasi; kunci dibatasi per pengguna agar id milik
	 * pengguna lain tidak dapat dipakai membaca respons tersimpan. */
	@Column(name = "pengguna", nullable = false, length = 150)
	public String getPengguna() {
		return pengguna;
	}

	public void setPengguna(String pengguna) {
		this.pengguna = pengguna;
	}

	@Column(name = "aksi", nullable = false, length = 100)
	public String getAksi() {
		return aksi;
	}

	public void setAksi(String aksi) {
		this.aksi = aksi;
	}

	@Column(name = "client_mutation_id", nullable = false, length = 150)
	public String getClientMutationId() {
		return clientMutationId;
	}

	public void setClientMutationId(String clientMutationId) {
		this.clientMutationId = clientMutationId;
	}

	/** Respons JSON yang dikirim saat eksekusi pertama sukses. */
	@Column(name = "respons", columnDefinition = "TEXT")
	public String getRespons() {
		return respons;
	}

	public void setRespons(String respons) {
		this.respons = respons;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}
}
