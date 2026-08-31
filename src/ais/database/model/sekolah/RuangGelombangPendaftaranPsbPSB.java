package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Model data untuk ruang gelombang pendaftaran psb psb. Tipe ini membawa state yang dipertukarkan
 * oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta
 * relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code RuangPSB ruangPSB}, {@code CalonSiswa calonSiswa}, {@code
 * String kodeUnik}; pemetaan persistence: tabel {@code sekolah.ruang_gelombang_psb}; pembacaan/pencarian ({@code
 * getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getId()}, {@code getRuangPSB()}, {@code
 * getCalonSiswa()}); mutasi data ({@code setOlehId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setId()}, {@code setRuangPSB()}); operasi domain lain ({@code toString()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "ruang_gelombang_psb")
public class RuangGelombangPendaftaranPsbPSB extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8522391894818139048L;

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
		calonSiswa = getCalonSiswa();
		return calonSiswa == null ? "" : calonSiswa.getNama();
	}

	private RuangPSB ruangPSB;
	private CalonSiswa calonSiswa;

	private String kodeUnik;

	public RuangGelombangPendaftaranPsbPSB() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_psb")
	public RuangPSB getRuangPSB() {
		ruangPSB = check(ruangPSB);
		return ruangPSB;
	}

	public void setRuangPSB(RuangPSB ruangPSB) {
		this.ruangPSB = ruangPSB;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa")
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	@Column(name = "kode_unik", unique = true)
	public String getKodeUnik() {
		if (calonSiswa != null && calonSiswa.getId() != null) {
			kodeUnik = calonSiswa.getId() + "_";
		}
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

}
