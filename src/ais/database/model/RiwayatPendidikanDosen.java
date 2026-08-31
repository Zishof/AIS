package ais.database.model;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;

/**
 * Model data untuk riwayat pendidikan dosen. Tipe ini membawa state yang dipertukarkan oleh
 * lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi
 * yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Dosen dosen}, {@code Jenjang jenjangPendidikan}, {@code
 * String namaSekolah}, {@code Kota kota}; pemetaan persistence: tabel {@code public.riwayat_pendidikan_dosen};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getDosen()}, {@code getJenjangPendidikan()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code
 * setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setDosen()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
@Table(schema = "public", name = "riwayat_pendidikan_dosen")

public class RiwayatPendidikanDosen extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8445312019405120038L;

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
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

	private Dosen dosen;
	private Jenjang jenjangPendidikan;
	private String namaSekolah;
	private Kota kota;
	private String kotaLain;
	private Integer tahunMasuk;
	private Integer tahunKeluar;
	private Double nilaiAkhir;
	private String gelarAkademik;
	private String kodePerguruanTinggi;
	private String bidangIlmu;
	private Date tanggalIjazah;
	private Negara negara;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		return dosen;
	}

	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan", nullable = false)
	public Jenjang getJenjangPendidikan() {
		jenjangPendidikan = check(jenjangPendidikan);
		return jenjangPendidikan;
	}

	public void setJenjangPendidikan(Jenjang jenjangPendidikan) {
		this.jenjangPendidikan = jenjangPendidikan;
	}

	@Column(name = "nama_sekolah")
	public String getNamaSekolah() {
		return namaSekolah;
	}

	public void setNamaSekolah(String namaSekolah) {
		this.namaSekolah = namaSekolah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kota", nullable = true)
	public Kota getKota() {
		return kota;
	}

	public void setKota(Kota kota) {
		this.kota = kota;
	}

	@Column(name = "tahun_masuk")
	public Integer getTahunMasuk() {
		return tahunMasuk;
	}

	public void setTahunMasuk(Integer tahunMasuk) {
		this.tahunMasuk = tahunMasuk;
	}

	@Column(name = "tahun_keluar")
	public Integer getTahunKeluar() {
		return tahunKeluar;
	}

	public void setTahunKeluar(Integer tahunKeluar) {
		this.tahunKeluar = tahunKeluar;
	}

	@Column(name = "nilai_akhir")
	public Double getNilaiAkhir() {
		return nilaiAkhir;
	}

	public void setNilaiAkhir(Double nilaiAkhir) {
		this.nilaiAkhir = nilaiAkhir;
	}

	@Column(name = "gelar_akademik")
	public String getGelarAkademik() {
		return gelarAkademik;
	}

	public void setGelarAkademik(String gelarAkademik) {
		this.gelarAkademik = gelarAkademik;
	}

	@Column(name = "kode_perguruan_tinggi")
	public String getKodePerguruanTinggi() {
		return kodePerguruanTinggi;
	}

	public void setKodePerguruanTinggi(String kodePerguruanTinggi) {
		this.kodePerguruanTinggi = kodePerguruanTinggi;
	}

	@Column(name = "bidang_ilmu")
	public String getBidangIlmu() {
		return bidangIlmu;
	}

	public void setBidangIlmu(String bidangIlmu) {
		this.bidangIlmu = bidangIlmu;
	}

	@Column(name = "tanggal_ijazah")
	public Date getTanggalIjazah() {
		return tanggalIjazah;
	}

	public void setTanggalIjazah(Date tanggalIjazah) {
		this.tanggalIjazah = tanggalIjazah;
	}

	public void setKotaLain(String kotaLain) {
		this.kotaLain = kotaLain;
	}

	@Column(name = "kota_lain")
	public String getKotaLain() {
		return kotaLain;
	}

	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "negara", nullable = false)
	public Negara getNegara() {
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

}
