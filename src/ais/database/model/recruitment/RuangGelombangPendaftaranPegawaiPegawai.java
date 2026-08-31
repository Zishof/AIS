package ais.database.model.recruitment;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;

/**
 * Model data untuk ruang gelombang pendaftaran pegawai pegawai. Tipe ini membawa state yang
 * dipertukarkan oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh
 * field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code RuangPegawai ruangPegawai}, {@code CalonPegawai
 * calonPegawai}, {@code String kodeUnik}; pemetaan persistence: tabel {@code public.ruang_gelombang_pegawai};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getId()},
 * {@code getRuangPegawai()}, {@code getCalonPegawai()}); mutasi data ({@code setOlehId()}, {@code setOleh()},
 * {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setId()}, {@code setRuangPegawai()}); operasi domain
 * lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
@Table(schema = "public", name = "ruang_gelombang_pegawai")



public class RuangGelombangPendaftaranPegawaiPegawai extends GeneralValueObject {

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
		return calonPegawai.getNama();
	}

	private RuangPegawai ruangPegawai;
	private CalonPegawai calonPegawai;

	private String kodeUnik;

	public RuangGelombangPendaftaranPegawaiPegawai() {
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

	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "ruang_pegawai")
	public RuangPegawai getRuangPegawai() {
		return ruangPegawai;
	}

	public void setRuangPegawai(RuangPegawai ruangPegawai) {
		this.ruangPegawai = ruangPegawai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "calon_pegawai")
	public CalonPegawai getCalonPegawai() {
		return calonPegawai;
	}

	public void setCalonPegawai(CalonPegawai calonPegawai) {
		this.calonPegawai = calonPegawai;
	}

	@Column(name = "kode_unik", unique = true)
	public String getKodeUnik() {
		if (calonPegawai != null && calonPegawai.getId() != null) {
			kodeUnik = calonPegawai.getId() + "_";
		}
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

}
