package ais.database.model;

import java.io.Serializable;
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
// schema DIEKSPLISITKAN (P4): tanpa ini tabel mengikuti search_path koneksi, dan c3p0
// mengembalikan koneksi ke kolam beserta search_path-nya -- satu-satunya tabel di jalur
// si_* yang punya ketergantungan itu. Tetap "public" (tidak dipindah): ini tabel LEGACY
// lintas-tenant; idempotensi jalur tenant memakai <schema-tenant>.idempotency_record (v8).
@Table(schema = "public", name = "mutasi_idempoten", uniqueConstraints = @UniqueConstraint(
		name = "uk_mutasi_idempoten_kunci",
		columnNames = { "pengguna", "aksi", "client_mutation_id" }))
public class MutasiIdempoten implements Serializable {

	private static final long serialVersionUID = 1L;

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
