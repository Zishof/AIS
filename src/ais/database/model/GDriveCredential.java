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
 *
 * <h3>Catatan keamanan — PERLUASAN task_f597932c (bukan temuan baru)</h3>
 * <p>Sama seperti {@link GDriveCode} (kode OAuth mentah) yang sudah tercatat pada task_f597932c:
 * entity ini menyimpan {@link #getRefreshToken()} — hasil AKHIR pertukaran kode OAuth — juga dalam
 * bentuk MENTAH (tanpa enkripsi), di kolom {@code refresh_token} (panjang 4000). Dibaca/ditulis oleh
 * {@code ais.common.gdrive.GDriveUtilPerPengguna} (mis. {@code simpanRefreshToken}, dan pembacaan
 * kembali saat melakukan panggilan Drive API), sehingga entity ini AKTIF DIPAKAI, bukan yatim. Token
 * refresh OAuth Google berumur panjang (tidak kedaluwarsa otomatis sampai dicabut/invalid_grant),
 * sehingga jendela paparan data-at-rest lebih luas dibanding kode OAuth sekali-pakai pada
 * {@link GDriveCode}. Ini memperluas cakupan task_f597932c (mekanisme penyimpanan sama: kolom teks
 * polos di tabel Postgres, tanpa lapisan enkripsi aplikasi), BUKAN kerentanan terpisah.</p>
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

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas "{id}-{nama}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String nama;
	private String refreshToken;
	private Boolean butuhOtorisasiUlang;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public GDriveCredential() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return username pengguna pemilik kredensial ini (kunci pencarian upsert di
	 *         {@code GDriveUtilPerPengguna}), di-trim saat dibaca.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama username pengguna pemilik kredensial ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return refresh token OAuth Google Drive MENTAH (tanpa enkripsi) milik pengguna
	 *         {@link #getNama()} — lihat catatan keamanan pada Javadoc kelas (perluasan
	 *         task_f597932c; entity ini AKTIF dipakai, bukan yatim).
	 */
	@Column(name = "refresh_token", nullable = true, length = 4000)
	public String getRefreshToken() {
		return this.refreshToken;
	}

	/**
	 * @param refreshToken refresh token OAuth mentah untuk disimpan.
	 */
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

	/**
	 * @param butuhOtorisasiUlang {@code true} untuk menandai kredensial ini butuh otorisasi ulang
	 *                            (di-set oleh {@code GDriveUtilPerPengguna} saat token ditolak
	 *                            Google), atau {@code false}/{@code null} setelah user berhasil
	 *                            terhubung ulang.
	 */
	public void setButuhOtorisasiUlang(Boolean butuhOtorisasiUlang) {
		this.butuhOtorisasiUlang = butuhOtorisasiUlang;
	}

}
