package ais.database.model.epsbed;

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



import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;

@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "epsbed", name = "epsbed_publikasi_ilmiah_dosen")



public class EpsbedPublikasiIlmiahDosen extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8445312019405120038L;

	private Long id;
	private String oleh;private String olehId;public String getOlehId() {return olehId;}public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

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
	private EpsbedJenisKaryaIlmiah kodeJenisPenelitian;
	private EpsbedMediaPublikasi kodeMediaPublikasi;
	private EpsbedPeranPenulisan kodeAuthor;
	private String kodeKegiatanMandiriKelompok;
	private Integer tahunPublikasi;
	private Integer bulanPublikasi;
	private EpsbedPembiayaanPenelitian kodePembiayaan;
	private Long jumlahBiaya;
	private String judul;
	private String url;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		return dosen;
	}

	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_jenis_penelitian", nullable = true)
	public EpsbedJenisKaryaIlmiah getKodeJenisPenelitian() {
		return kodeJenisPenelitian;
	}

	public void setKodeJenisPenelitian(EpsbedJenisKaryaIlmiah kodeJenisPenelitian) {
		this.kodeJenisPenelitian = kodeJenisPenelitian;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "media_publikasi", nullable = true)
	public EpsbedMediaPublikasi getKodeMediaPublikasi() {
		return kodeMediaPublikasi;
	}

	public void setKodeMediaPublikasi(EpsbedMediaPublikasi kodeMediaPublikasi) {
		this.kodeMediaPublikasi = kodeMediaPublikasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_author", nullable = true)
	public EpsbedPeranPenulisan getKodeAuthor() {
		return kodeAuthor;
	}

	public void setKodeAuthor(EpsbedPeranPenulisan kodeAuthor) {
		this.kodeAuthor = kodeAuthor;
	}

	@Column(name = "kode_kegiatan_mandiri_kelompok")
	public String getKodeKegiatanMandiriKelompok() {
		return kodeKegiatanMandiriKelompok;
	}

	public void setKodeKegiatanMandiriKelompok(String kodeKegiatanMandiriKelompok) {
		this.kodeKegiatanMandiriKelompok = kodeKegiatanMandiriKelompok;
	}

	@Column(name = "tahun_publikasi")
	public Integer getTahunPublikasi() {
		return tahunPublikasi;
	}

	public void setTahunPublikasi(Integer tahunPublikasi) {
		this.tahunPublikasi = tahunPublikasi;
	}

	@Column(name = "bulan_publikasi")
	public Integer getBulanPublikasi() {
		return bulanPublikasi;
	}

	public void setBulanPublikasi(Integer bulanPublikasi) {
		this.bulanPublikasi = bulanPublikasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_pembiayaan", nullable = true)
	public EpsbedPembiayaanPenelitian getKodePembiayaan() {
		return kodePembiayaan;
	}

	public void setKodePembiayaan(EpsbedPembiayaanPenelitian kodePembiayaan) {
		this.kodePembiayaan = kodePembiayaan;
	}

	@Column(name = "jumlah_biaya")
	public Long getJumlahBiaya() {
		if (jumlahBiaya == null) {
			jumlahBiaya = 0L;
		}
		return jumlahBiaya;
	}

	public void setJumlahBiaya(Long jumlahBiaya) {
		this.jumlahBiaya = jumlahBiaya;
	}

	@Column(columnDefinition = "text")
	public String getJudul() {
		return judul;
	}

	public void setJudul(String judul) {
		this.judul = judul;
	}

	@Column(columnDefinition = "text")
	public String getUrl() {
		if(url==null){
			url="";
		}
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
