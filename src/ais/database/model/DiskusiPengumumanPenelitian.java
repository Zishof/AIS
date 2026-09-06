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

import ais.common.Html2Text;
import ais.database.model.penelitiandanpengabdian.PengumumanPenelitian;

//import org.zkforge.fckez.MyCkEditor;

/**
 * Satu baris komentar/tanggapan (tabel {@code public.diskusi_pengumuman_penelitian}) pada satu
 * {@link PengumumanPenelitian} (pengumuman modul penelitian &amp; pengabdian) — penulisnya bisa
 * {@link Tbmuser} (dosen/staf) atau {@link Mahasiswa}.
 *
 * <p><b>Entity INDEPENDEN, bukan bagian mekanisme {@link Diskusi}/{@link DiskusiKomentar}</b>
 * (mekanisme generik hasil hbm2java yang, berdasarkan verifikasi menyeluruh, hanya dipakai ulang oleh
 * modul jurnal — lihat Javadoc {@link Diskusi}). Class ini punya struktur sendiri (kolom
 * {@code judul}/{@code catatan} langsung pada baris, bukan lewat entity utas terpisah) dan TIDAK
 * mewarisi mekanisme anonimitas yang dicatat belum ditegakkan pada {@link Diskusi}/
 * {@link DiskusiKomentar} (task_493423ef) — di sini penulis SELALU tercatat eksplisit lewat
 * {@link #getTbmuser()}/{@link #getMahasiswa()}, tanpa opsi anonim sama sekali.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "diskusi_pengumuman_penelitian")

public class DiskusiPengumumanPenelitian extends GeneralValueObject {

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
	private Date tanggal;
	private PengumumanPenelitian pengumumanPenelitian;
	private Tbmuser tbmuser;
	private Mahasiswa mahasiswa;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public DiskusiPengumumanPenelitian() {
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
	@Column(name = "catatan", nullable = false, length = 3000)
	public String getCatatan() {
		if (catatan == null) {
			catatan = "";
		}
		try {
			Html2Text parser = new Html2Text();
			parser.parse(new StringReader(catatan));
			catatan = parser.getText();
			parser = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DiskusiPengumumanPenelitian.java:97");
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
	 * @param pengumumanPenelitian pengumuman yang ditanggapi komentar ini.
	 */
	public void setPengumumanPenelitian(PengumumanPenelitian pengumumanPenelitian) {
		this.pengumumanPenelitian = pengumumanPenelitian;
	}

	/**
	 * @return pengumuman yang ditanggapi komentar ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengumuman_penelitian", nullable = false)
	public PengumumanPenelitian getPengumumanPenelitian() {
		return pengumumanPenelitian;
	}

	/**
	 * @param judul judul singkat komentar ini.
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * @return judul singkat komentar ini.
	 */
	@Column(name = "judul", nullable = false, length = 500)
	public String getJudul() {
		return judul;
	}

}
