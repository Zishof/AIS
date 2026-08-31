package ais.database.model.employ;

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
 * Model data untuk diklat. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code String nama}, {@code JenisDiklat jenisDiklat}, {@code Date
 * tanggalMulai}, {@code Date tanggalSelesai}; pemetaan persistence: tabel {@code employ.diklat};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getKeterangan()}, {@code getNama()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code
 * setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code setKeterangan()}); operasi domain lain
 * ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
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
@Table(schema = "employ", name = "diklat")



public class Diklat extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;private String olehId;public String getOlehId() {return olehId;}public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}
	private String keterangan;
	private String nama;
	private JenisDiklat jenisDiklat;
	private Date tanggalMulai;
	private Date tanggalSelesai;
	private String noSertifikat;
	private String tahunSertifikat;
	private String penyelenggara;	
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();	

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}


	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return keterangan;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}


	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_diklat", nullable = true)
	public JenisDiklat getJenisDiklat() {
		return jenisDiklat;
	}

	public void setJenisDiklat(JenisDiklat jenisDiklat) {
		this.jenisDiklat = jenisDiklat;
	}

	@Column(name = "tanggal_mulai")
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	@Column(name = "no_sertifikat")
	public String getNoSertifikat() {
		return noSertifikat;
	}

	public void setNoSertifikat(String noSertifikat) {
		this.noSertifikat = noSertifikat;
	}

	@Column(name = "tahun_sertifikat")
	public String getTahunSertifikat() {
		return tahunSertifikat;
	}

	public void setTahunSertifikat(String tahunSertifikat) {
		this.tahunSertifikat = tahunSertifikat;
	}

	@Column(name = "penyelenggara")
	public String getPenyelenggara() {
		return penyelenggara;
	}

	public void setPenyelenggara(String penyelenggara) {
		this.penyelenggara = penyelenggara;
	}

}
