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

	/**
	 * Mengembalikan primary key baris idempotensi.
	 *
	 * @return id baris; {@code null} selama objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel primary key baris idempotensi. Hanya relevan bagi Hibernate saat mengisi objek
	 * dari hasil query, karena kolom dipetakan {@code insertable = false}.
	 *
	 * @param id nilai primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** UserId pemilik mutasi; kunci dibatasi per pengguna agar id milik
	 * pengguna lain tidak dapat dipakai membaca respons tersimpan. */
	@Column(name = "pengguna", nullable = false, length = 150)
	public String getPengguna() {
		return pengguna;
	}

	/**
	 * Menyetel userId pemilik mutasi.
	 *
	 * @param pengguna userId pemilik mutasi
	 */
	public void setPengguna(String pengguna) {
		this.pengguna = pengguna;
	}

	/**
	 * Mengembalikan nama aksi/operasi bisnis yang diidentifikasi bersama {@link #getPengguna()}
	 * dan {@link #getClientMutationId()} sebagai kunci unik idempotensi ({@code
	 * uk_mutasi_idempoten_kunci}) -- aksi yang berbeda dengan {@code clientMutationId} yang sama
	 * dianggap dua mutasi berbeda, bukan retry dari satu mutasi yang sama.
	 *
	 * @return nama aksi; boleh {@code null} bila belum diisi
	 */
	@Column(name = "aksi", nullable = false, length = 100)
	public String getAksi() {
		return aksi;
	}

	/**
	 * Menyetel nama aksi/operasi bisnis.
	 *
	 * @param aksi nama aksi baru
	 */
	public void setAksi(String aksi) {
		this.aksi = aksi;
	}

	/**
	 * Mengembalikan id mutasi yang dibuat KLIEN (bukan server), stabil lintas percobaan
	 * <i>retry</i>. Bersama {@link #getPengguna()} dan {@link #getAksi()} membentuk kunci unik
	 * yang membuat retry dengan id yang sama mengembalikan {@link #getRespons()} lama alih-alih
	 * menjalankan ulang operasi bisnisnya.
	 *
	 * @return id mutasi klien; boleh {@code null} bila belum diisi
	 */
	@Column(name = "client_mutation_id", nullable = false, length = 150)
	public String getClientMutationId() {
		return clientMutationId;
	}

	/**
	 * Menyetel id mutasi klien.
	 *
	 * @param clientMutationId id mutasi klien baru
	 */
	public void setClientMutationId(String clientMutationId) {
		this.clientMutationId = clientMutationId;
	}

	/** Respons JSON yang dikirim saat eksekusi pertama sukses. */
	@Column(name = "respons", columnDefinition = "TEXT")
	public String getRespons() {
		return respons;
	}

	/**
	 * Menyetel respons JSON yang akan dikembalikan pada retry berikutnya.
	 *
	 * @param respons respons JSON baru; boleh {@code null}
	 */
	public void setRespons(String respons) {
		this.respons = respons;
	}

	/**
	 * Mengembalikan waktu baris idempotensi dicatat (presisi TIMESTAMP).
	 *
	 * @return waktu pencatatan; terisi waktu server saat objek dibuat bila tidak disetel
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menyetel waktu pencatatan.
	 *
	 * @param tanggal waktu baru
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}
}
