package ais.database.model.file;

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
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
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Foto profil {@code AnggotaKoperasi} MANDIRI — member POS yang tidak
 * ditautkan ke siswa/mahasiswa/pengguna (31-08, laporan galat
 * {@code anggota_foto_upload}). Pola persis {@link FotoSiswa}: satu tabel
 * foto profil per jenis subjek, dibaca/ditulis lewat mesin
 * {@code ProfileImageUtil}/{@code FileFotoLain} yang sama sehingga tidak ada
 * silo media khusus aplikasi desktop. Member yang PUNYA tautan sivitas tetap
 * memakai tabel foto entitas tautannya (perilaku lama tidak berubah).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "foto_anggota_koperasi")
public class FotoAnggotaKoperasi extends FileFotoLain {

	private static final long serialVersionUID = 1L;

	public static String DEFAULT_JENIS = "foto anggota koperasi";

	private Long id;
	private String oleh;
	private String olehId;
	private String nama;
	private String keterangan;
	private Long anggotaKoperasi;
	private Blob foto;
	private FotoAnggotaKoperasi copyDari;
	private String link;
	private String jenis;
	private String lokasiSimpan;
	private String gdrive;
	private String gdriveUsername;

	public FotoAnggotaKoperasi() {
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return nama;
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

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		}
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@NotAudited
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/** Id {@code koperasi.anggota_koperasi} pemilik foto. */
	@Column(name = "anggota_koperasi", nullable = true)
	public Long getAnggotaKoperasi() {
		return anggotaKoperasi;
	}

	public void setAnggotaKoperasi(Long anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public FotoAnggotaKoperasi getCopyDari() {
		return copyDari;
	}

	public void setCopyDari(FotoAnggotaKoperasi copyDari) {
		this.copyDari = copyDari;
	}

	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	public String getGdrive() {
		if (copyDari != null) {
			gdrive = copyDari.gdrive;
		}
		String s = gdrive == null || gdrive.trim().isEmpty() ? retreive("gdrive") : gdrive;
		return s != null && !s.trim().isEmpty() ? s : gdrive;
	}

	public void setGdrive(String gdrive) {
		if (gdrive != null && !gdrive.trim().isEmpty()) {
			put(gdrive, "gdrive");
		}
		this.gdrive = gdrive;
	}

	public String getGdriveUsername() {
		if (copyDari != null) {
			gdriveUsername = copyDari.gdriveUsername;
		}
		return gdriveUsername;
	}

	public void setGdriveUsername(String gdriveUsername) {
		this.gdriveUsername = gdriveUsername;
	}

	@Override
	public String ambilJenis() {
		return jenis;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		return this.getClass();
	}

	@Override
	@Column(name = "jenis", length = 30)
	public String getJenis() {
		jenis = DEFAULT_JENIS;
		return jenis;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	public String getLink() {
		return link == null ? "" : link.trim();
	}

	public void setLink(String link) {
		this.link = link;
	}

	@Override
	public Long ambilRef() {
		return anggotaKoperasi;
	}

	@Override
	public String ambilLink() {
		return getLink();
	}

	private String url;

	@Transient
	public String getUrl() {
		try {
			if (url == null && getId() != null) {
				url = createLinkUri();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/database/model/file/FotoAnggotaKoperasi.java:getUrl");
		}
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
