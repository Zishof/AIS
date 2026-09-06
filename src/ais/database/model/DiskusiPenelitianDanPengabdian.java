package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.StringReader;
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
import org.hibernate.envers.RelationTargetAuditMode;

import ais.common.Html2Text;
import ais.database.model.penelitiandanpengabdian.FilePengajuanPenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.PenelitianDanPengabdian;

//import org.zkforge.fckez.MyCkEditor;

/**
 * Satu baris komentar/tanggapan (tabel {@code public.diskusi_penelitian_dan_pengabdian}) pada satu
 * pengajuan {@link PenelitianDanPengabdian}, dengan dukungan SATU balasan langsung tertaut di baris
 * yang sama ({@link #getTbmuserBalasan()}/{@link #getMahasiswaBalasan()}) — berbeda dari pola diskusi
 * lain di modul ini yang tiap balasan adalah baris baru.
 *
 * <p><b>Entity INDEPENDEN, bukan bagian mekanisme {@link Diskusi}/{@link DiskusiKomentar}</b>
 * (mekanisme generik hasil hbm2java yang, berdasarkan verifikasi menyeluruh, hanya dipakai ulang oleh
 * modul jurnal — lihat Javadoc {@link Diskusi}). Sama seperti {@link DiskusiPengumumanPenelitian},
 * penulis komentar maupun balasannya di sini SELALU tercatat eksplisit lewat {@link Tbmuser}/
 * {@link Mahasiswa}, tanpa opsi anonim — sehingga tidak mewarisi persoalan anonimitas yang dicatat
 * belum ditegakkan pada {@link Diskusi}/{@link DiskusiKomentar} (task_493423ef).</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "diskusi_penelitian_dan_pengabdian")

public class DiskusiPenelitianDanPengabdian extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463822577541439808L;
	private Long id;

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
	 * @return isi komentar ({@link #getCatatan()}), dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return catatan;
	}

	private String judul;
	private String catatan;
	private String pengguna;
	private Date tanggal;
	private PenelitianDanPengabdian penelitianDanPengabdian;
	private FilePengajuanPenelitianDanPengabdian filePengajuanPengajuanPenelitianDanPengabdian;

	private Tbmuser tbmuser;
	private Mahasiswa mahasiswa;

	private Tbmuser tbmuserBalasan;
	private Mahasiswa mahasiswaBalasan;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public DiskusiPenelitianDanPengabdian() {
		// MyCkEditor
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
	 * @return isi komentar setelah tag HTML dibersihkan lewat {@link Html2Text} (efek samping:
	 *         pemanggilan pertama menormalisasi field {@link #catatan} menjadi versi plain-text-nya
	 *         — getter-mutasi). Kegagalan parsing HTML diserap diam-diam (dicatat ke
	 *         {@code ErrorAuditUtil}) dan mengembalikan nilai HTML mentah apa adanya.
	 */
	@Column(name = "catatan", nullable = false, columnDefinition = "text")
	public String getCatatan() {
		if (catatan == null) {
			catatan = "";
		}
		try {
			Html2Text parser = new Html2Text();
			parser.parse(new StringReader(catatan));
			catatan = parser.getText();
			parser = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DiskusiPenelitianDanPengabdian.java:105");
			// TODO: handle exception
		}
		return this.catatan;
	}

	/**
	 * @param catatan isi komentar (boleh mengandung markup HTML dari editor kaya-teks).
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * @param tanggal waktu komentar ditulis.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * @return waktu komentar ditulis.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * @param penelitianDanPengabdian pengajuan penelitian/pengabdian yang ditanggapi komentar ini.
	 */
	public void setPenelitianDanPengabdian(PenelitianDanPengabdian penelitianDanPengabdian) {
		this.penelitianDanPengabdian = penelitianDanPengabdian;
	}

	/**
	 * @return pengajuan penelitian/pengabdian yang ditanggapi komentar ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penelitian_dan_pengabdian", nullable = false)
	public PenelitianDanPengabdian getPenelitianDanPengabdian() {
		return penelitianDanPengabdian;
	}

	/**
	 * @param judul judul komentar ini (lihat catatan getter — nilai yang di-set di sini akan
	 *              ditimpa saat dibaca kembali).
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * @return judul komentar ini. Getter-mutasi: SELALU menimpa {@link #judul} dengan hasil
	 *         {@link #getCatatan()} (isi komentar plain-text) setiap kali dipanggil — field
	 *         {@code judul} tersimpan di DB pada dasarnya tidak pernah independen dari isi komentar.
	 */
	@Column(name = "judul", nullable = false, length = 500)
	public String getJudul() {
		judul = getCatatan();
		return judul;
	}

	/**
	 * @return nama pengguna penulis (kolom teks bebas, terpisah dari relasi {@link #getTbmuser()}/
	 *         {@link #getMahasiswa()}).
	 */
	public String getPengguna() {
		return pengguna;
	}

	/**
	 * @param pengguna nama pengguna penulis.
	 */
	public void setPengguna(String pengguna) {
		this.pengguna = pengguna;
	}

	/**
	 * @return berkas lampiran pengajuan penelitian/pengabdian terkait, atau {@code null} bila tidak
	 *         ada lampiran. Sengaja {@link RelationTargetAuditMode#NOT_AUDITED} — perubahan pada
	 *         entity berkas tujuan tidak perlu direkam sebagai revisi Envers baris diskusi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "file_pengajuan_penelitian_dan_pengabdian", nullable = true)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public FilePengajuanPenelitianDanPengabdian getFilePengajuanPengajuanPenelitianDanPengabdian() {
		return filePengajuanPengajuanPenelitianDanPengabdian;
	}

	/**
	 * @param filePengajuanPengajuanPenelitianDanPengabdian berkas lampiran pengajuan terkait.
	 */
	public void setFilePengajuanPengajuanPenelitianDanPengabdian(
			FilePengajuanPenelitianDanPengabdian filePengajuanPengajuanPenelitianDanPengabdian) {
		this.filePengajuanPengajuanPenelitianDanPengabdian = filePengajuanPengajuanPenelitianDanPengabdian;
	}

	/**
	 * @return penulis komentar sebagai {@link Tbmuser} (dosen/staf); di-null-kan otomatis bila
	 *         {@link #getMahasiswa()} terisi (satu komentar hanya boleh punya satu jenis penulis).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (mahasiswa != null) {
			tbmuser = null;
		}
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * @param tbmuser penulis komentar sebagai {@link Tbmuser}.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * @return penulis komentar sebagai {@link Mahasiswa}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * @param mahasiswa penulis komentar sebagai {@link Mahasiswa}.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return penulis balasan (tertaut langsung di baris yang sama, bukan baris baru) sebagai
	 *         {@link Tbmuser}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser_balasan", nullable = true)
	public Tbmuser getTbmuserBalasan() {
		tbmuserBalasan = check(tbmuserBalasan);
		return tbmuserBalasan;
	}

	/**
	 * @param tbmuserBalasan penulis balasan sebagai {@link Tbmuser}.
	 */
	public void setTbmuserBalasan(Tbmuser tbmuserBalasan) {
		this.tbmuserBalasan = tbmuserBalasan;
	}

	/**
	 * @return penulis balasan (tertaut langsung di baris yang sama, bukan baris baru) sebagai
	 *         {@link Mahasiswa}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa_balasan", nullable = true)
	public Mahasiswa getMahasiswaBalasan() {
		mahasiswaBalasan = check(mahasiswaBalasan);
		return mahasiswaBalasan;
	}

	/**
	 * @param mahasiswaBalasan penulis balasan sebagai {@link Mahasiswa}.
	 */
	public void setMahasiswaBalasan(Mahasiswa mahasiswaBalasan) {
		this.mahasiswaBalasan = mahasiswaBalasan;
	}

}
