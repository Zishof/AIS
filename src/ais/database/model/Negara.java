package ais.database.model;

// Generated Apr 12, 2010 1:48:52 AM by Hibernate Tools 3.2.4.CR1

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
 * Entity referensi <b>daftar negara</b> (tabel {@code public.negara}) — dipakai sebagai lookup
 * pilihan kewarganegaraan/negara asal pada berbagai form biodata (mahasiswa, siswa, calon
 * mahasiswa, pegawai, dsb). Data bersifat master statis yang jarang berubah.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "negara")
public class Negara extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -3088213612931036389L;
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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * @return representasi ringkas "{id}-{namaNegara}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + namaNegara;
	}

	private String namaNegara;
	private String kode;
	private Boolean aktif;

	private String nama;

	/**
	 * Alias baca-saja untuk {@link #getNamaNegara()} — kolom {@code nama_negara} sama dipetakan dua
	 * kali (getter ini {@code insertable/updatable = false}) agar kode pemanggil generik yang
	 * mengharapkan properti {@code nama} pada entity lookup tetap berfungsi tanpa perubahan.
	 *
	 * @return nama negara (identik dengan {@link #getNamaNegara()}).
	 */
	@Column(name = "nama_negara", nullable = false, insertable = false, updatable = false)
	public String getNama() {
		nama = getNamaNegara();
		return nama;
	}

	/**
	 * @param nama nilai field alias {@code nama} (tidak pernah ditulis ke kolom DB — lihat
	 *             {@link #getNama()}).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public Negara() {
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
	 * @return nama negara.
	 */
	@Column(name = "nama_negara", nullable = false, length = 100)
	public String getNamaNegara() {
		return this.namaNegara;
	}

	/**
	 * @param namaNegara nama negara.
	 */
	public void setNamaNegara(String namaNegara) {
		this.namaNegara = namaNegara;
	}

	/**
	 * @return kode negara. Kasus khusus: bila {@link #namaNegara} adalah "indonesia"
	 *         (case-insensitive), kode dipaksa/di-override menjadi {@code "ID"} terlepas dari nilai
	 *         kolom {@code kode} tersimpan — getter-mutasi yang menghitung ulang setiap dipanggil.
	 *         Selain itu, string kosong bila kode belum diisi (bukan {@code null}).
	 */
	public String getKode() {
		if (namaNegara != null && namaNegara.trim().equalsIgnoreCase("indonesia")) {
			kode = "ID";
		}
		return kode == null ? "" : kode.trim();
	}

	/**
	 * @param kode kode negara (mis. ISO 2-huruf).
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return {@code true} bila baris ini aktif/boleh dipilih; default {@code true} ({@code null}
	 *         dianggap aktif) sehingga data lama tanpa kolom ini tetap muncul di pilihan.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif/nonaktif baris ini.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
