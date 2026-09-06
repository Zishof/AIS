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
 * Entity referensi <b>daftar agama</b> (tabel {@code public.agama}) — dipakai sebagai lookup pilihan
 * agama pada form biodata mahasiswa/siswa/pegawai/calon mahasiswa. Data master statis, jarang
 * berubah.
 *
 * <p>Dicatat sebagai salah satu bukti nyata backlog dokumentasi berkas lepas
 * {@code ais/database/model/} — sebelum batch ini, class sesederhana ini (144 baris) hanya punya 2
 * Javadoc bawaan generator hbm2java.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "agama")
public class Agama extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
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
	 * @return representasi ringkas "{id}-{nama}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;

	private String nama;
	private String keterangan;
	private Boolean aktif;
	private Long feeder;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public Agama() {
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
	 * @return kode agama; bila kolom {@code kode} belum diisi, jatuh balik ke representasi string
	 *         {@link #getFeeder()} (kode integrasi feeder PDDikti/EMIS).
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? getFeeder() + "" : kode.trim();
	}

	/**
	 * @param kode kode agama.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama agama, di-trim saat dibaca.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama agama.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan tambahan tentang baris ini.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan tambahan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return kode numerik untuk pelaporan feeder (mis. PDDikti/EMIS); default {@code 1} bila belum
	 *         diisi.
	 */
	public Long getFeeder() {
		if (feeder == null) {
			feeder = 1L;
		}
		return feeder;
	}

	/**
	 * @param feeder kode numerik pelaporan feeder.
	 */
	public void setFeeder(Long feeder) {
		this.feeder = feeder;
	}

	/**
	 * @return {@code true} bila baris ini aktif/boleh dipilih; default {@code true} ({@code null}
	 *         dianggap aktif).
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
