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

/**
 * Kredensial Google Drive per pengguna (refresh token) yang DIBAGI ANTAR NODE.
 *
 * <p>Dulu refresh token hanya tersimpan di {@code FileDataStoreFactory} pada DISK LOKAL tiap node
 * ({@code lokasiFileTemproraryTemp/<user>_<clientid>}). Pada deployment multi-node (load-balanced),
 * node yang tidak pernah melakukan pertukaran code tidak punya kredensial di disknya, sehingga upload
 * di node itu meminta otorisasi ulang dan gagal. Entity ini memindahkan refresh token ke DB (tabel
 * {@code gdrive_credential}) sehingga SEMUA node bisa memakainya.</p>
 *
 * <p><b>Sengaja TIDAK {@code @Audited}</b>: riwayat kredensial tidak diperlukan, dan menghindari
 * keharusan menyinkronkan tabel {@code new_audit.gdrive_credential__audit} saat {@code hbm2ddl=update}
 * menambah tabel/kolom (lihat catatan Envers di hibernate.cfg.xml). Tabel dibuat otomatis oleh
 * Hibernate ({@code hbm2ddl.auto=update}).</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "public", name = "gdrive_credential")

public class GDriveCredential extends GeneralValueObject {

	private static final long serialVersionUID = 7451093820164732895L;
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return id + "-" + nama;
	}

	private String nama;
	private String refreshToken;
	private Boolean butuhOtorisasiUlang;

	public GDriveCredential() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "refresh_token", nullable = true, length = 4000)
	public String getRefreshToken() {
		return this.refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	/**
	 * {@code true} bila kredensial GDrive user ini diketahui SUDAH TIDAK BERLAKU (refresh token
	 * ditolak Google dengan {@code invalid_grant}, atau panggilan Drive API ditolak 401) dan BELUM
	 * dihubungkan ulang oleh user ("Hubungkan ke Drive"). Sengaja {@code Boolean} (bukan
	 * {@code boolean} primitif) + {@code nullable = true} agar {@code hbm2ddl.auto=update} bisa
	 * menambah kolom ini pada tabel yang SUDAH berisi baris lama tanpa gagal (ALTER ... NOT NULL
	 * pada tabel non-kosong akan abort); baris lama otomatis terbaca {@code null} = dianggap belum
	 * butuh otorisasi ulang. Dipakai untuk menghentikan percobaan backup terjadwal berulang
	 * memakai kredensial yang sudah pasti mati.
	 */
	@Column(name = "butuh_otorisasi_ulang", nullable = true)
	public Boolean getButuhOtorisasiUlang() {
		return this.butuhOtorisasiUlang;
	}

	public void setButuhOtorisasiUlang(Boolean butuhOtorisasiUlang) {
		this.butuhOtorisasiUlang = butuhOtorisasiUlang;
	}

}
