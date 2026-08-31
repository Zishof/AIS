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
import ais.database.model.Pegawai;



/**
 * Model data untuk daftar nilai pelaksanaan pekerjaan. Tipe ini membawa state yang dipertukarkan
 * oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta
 * relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code String nama}, {@code Date tanggal_dirubah}, {@code Pegawai
 * yangDinilai}, {@code Pegawai penilai}; pemetaan persistence: tabel {@code
 * employ.daftar_nilai_pelaksanaan_pekerjaan}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getKeterangan()}, {@code getNama()}); mutasi data ({@code
 * setOlehId()}, {@code onUpdate()}, {@code setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code
 * setKeterangan()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
@Table(schema = "employ", name = "daftar_nilai_pelaksanaan_pekerjaan")



public class DaftarNilaiPelaksanaanPekerjaan extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;private String olehId;public String getOlehId() {return olehId;}public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}
	private String keterangan;
	private String nama;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Pegawai yangDinilai;
	private Pegawai penilai;
	private Pegawai atasanPenilai;

	private Double kesetiaan;
	private Double prestasiKerja;
	private Double tanggungJawab;
	private Double ketaatan;
	private Double kejujuran;
	private Double kerjasama;
	private Double prakarsa;
	private Double kepimpinan;
	private Double jumlah;
	private Double rataRata;

	private String sebutankesetiaan;
	private String sebutanprestasiKerja;
	private String sebutantanggungJawab;
	private String sebutanketaatan;
	private String sebutankejujuran;
	private String sebutankerjasama;
	private String sebutanprakarsa;
	private String sebutankepimpinan;
	private String sebutanjumlah;
	private String sebutanrataRata;

	private String keterangankesetiaan;
	private String keteranganprestasiKerja;
	private String keterangantanggungJawab;
	private String keteranganketaatan;
	private String keterangankejujuran;
	private String keterangankerjasama;
	private String keteranganprakarsa;
	private String keterangankepimpinan;
	private String keteranganjumlah;
	private String keteranganrataRata;

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

	@Column(name = "nama", nullable = true)
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "yang_dinilai", nullable = true)
	public Pegawai getYangDinilai() {
		return yangDinilai;
	}

	public void setYangDinilai(Pegawai yangDinilai) {
		this.yangDinilai = yangDinilai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penilai", nullable = true)
	public Pegawai getPenilai() {
		return penilai;
	}

	public void setPenilai(Pegawai penilai) {
		this.penilai = penilai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "atasan_penilai", nullable = true)
	public Pegawai getAtasanPenilai() {
		return atasanPenilai;
	}

	public void setAtasanPenilai(Pegawai atasanPenilai) {
		this.atasanPenilai = atasanPenilai;
	}

	public Double getKesetiaan() {
		return kesetiaan;
	}

	public void setKesetiaan(Double kesetiaan) {
		this.kesetiaan = kesetiaan;
	}

	public Double getPrestasiKerja() {
		return prestasiKerja;
	}

	public void setPrestasiKerja(Double prestasiKerja) {
		this.prestasiKerja = prestasiKerja;
	}

	public Double getTanggungJawab() {
		return tanggungJawab;
	}

	public void setTanggungJawab(Double tanggungJawab) {
		this.tanggungJawab = tanggungJawab;
	}

	public Double getKetaatan() {
		return ketaatan;
	}

	public void setKetaatan(Double ketaatan) {
		this.ketaatan = ketaatan;
	}

	public Double getKejujuran() {
		return kejujuran;
	}

	public void setKejujuran(Double kejujuran) {
		this.kejujuran = kejujuran;
	}

	public Double getKerjasama() {
		return kerjasama;
	}

	public void setKerjasama(Double kerjasama) {
		this.kerjasama = kerjasama;
	}

	public Double getPrakarsa() {
		return prakarsa;
	}

	public void setPrakarsa(Double prakarsa) {
		this.prakarsa = prakarsa;
	}

	public Double getKepimpinan() {
		return kepimpinan;
	}

	public void setKepimpinan(Double kepimpinan) {
		this.kepimpinan = kepimpinan;
	}

	public String getSebutankesetiaan() {
		return sebutankesetiaan;
	}

	public void setSebutankesetiaan(String sebutankesetiaan) {
		this.sebutankesetiaan = sebutankesetiaan;
	}

	public String getSebutanprestasiKerja() {
		return sebutanprestasiKerja;
	}

	public void setSebutanprestasiKerja(String sebutanprestasiKerja) {
		this.sebutanprestasiKerja = sebutanprestasiKerja;
	}

	public String getSebutantanggungJawab() {
		return sebutantanggungJawab;
	}

	public void setSebutantanggungJawab(String sebutantanggungJawab) {
		this.sebutantanggungJawab = sebutantanggungJawab;
	}

	public String getSebutanketaatan() {
		return sebutanketaatan;
	}

	public void setSebutanketaatan(String sebutanketaatan) {
		this.sebutanketaatan = sebutanketaatan;
	}

	public String getSebutankejujuran() {
		return sebutankejujuran;
	}

	public void setSebutankejujuran(String sebutankejujuran) {
		this.sebutankejujuran = sebutankejujuran;
	}

	public String getSebutankerjasama() {
		return sebutankerjasama;
	}

	public void setSebutankerjasama(String sebutankerjasama) {
		this.sebutankerjasama = sebutankerjasama;
	}

	public String getSebutanprakarsa() {
		return sebutanprakarsa;
	}

	public void setSebutanprakarsa(String sebutanprakarsa) {
		this.sebutanprakarsa = sebutanprakarsa;
	}

	public String getSebutankepimpinan() {
		return sebutankepimpinan;
	}

	public void setSebutankepimpinan(String sebutankepimpinan) {
		this.sebutankepimpinan = sebutankepimpinan;
	}

	public String getKeterangankesetiaan() {
		return keterangankesetiaan;
	}

	public void setKeterangankesetiaan(String keterangankesetiaan) {
		this.keterangankesetiaan = keterangankesetiaan;
	}

	public String getKeteranganprestasiKerja() {
		return keteranganprestasiKerja;
	}

	public void setKeteranganprestasiKerja(String keteranganprestasiKerja) {
		this.keteranganprestasiKerja = keteranganprestasiKerja;
	}

	public String getKeterangantanggungJawab() {
		return keterangantanggungJawab;
	}

	public void setKeterangantanggungJawab(String keterangantanggungJawab) {
		this.keterangantanggungJawab = keterangantanggungJawab;
	}

	public String getKeteranganketaatan() {
		return keteranganketaatan;
	}

	public void setKeteranganketaatan(String keteranganketaatan) {
		this.keteranganketaatan = keteranganketaatan;
	}

	public String getKeterangankejujuran() {
		return keterangankejujuran;
	}

	public void setKeterangankejujuran(String keterangankejujuran) {
		this.keterangankejujuran = keterangankejujuran;
	}

	public String getKeterangankerjasama() {
		return keterangankerjasama;
	}

	public void setKeterangankerjasama(String keterangankerjasama) {
		this.keterangankerjasama = keterangankerjasama;
	}

	public String getKeteranganprakarsa() {
		return keteranganprakarsa;
	}

	public void setKeteranganprakarsa(String keteranganprakarsa) {
		this.keteranganprakarsa = keteranganprakarsa;
	}

	public String getKeterangankepimpinan() {
		return keterangankepimpinan;
	}

	public void setKeterangankepimpinan(String keterangankepimpinan) {
		this.keterangankepimpinan = keterangankepimpinan;
	}

	public Double getJumlah() {
		return jumlah;
	}

	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	public Double getRataRata() {
		return rataRata;
	}

	public void setRataRata(Double rataRata) {
		this.rataRata = rataRata;
	}

	public String getSebutanjumlah() {
		return sebutanjumlah;
	}

	public void setSebutanjumlah(String sebutanjumlah) {
		this.sebutanjumlah = sebutanjumlah;
	}

	public String getSebutanrataRata() {
		return sebutanrataRata;
	}

	public void setSebutanrataRata(String sebutanrataRata) {
		this.sebutanrataRata = sebutanrataRata;
	}

	public String getKeteranganjumlah() {
		return keteranganjumlah;
	}

	public void setKeteranganjumlah(String keteranganjumlah) {
		this.keteranganjumlah = keteranganjumlah;
	}

	public String getKeteranganrataRata() {
		return keteranganrataRata;
	}

	public void setKeteranganrataRata(String keteranganrataRata) {
		this.keteranganrataRata = keteranganrataRata;
	}

}
