package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entity <b>teks berjalan (running text/marquee) informasi</b> (tabel {@code public.text_berjalan}) —
 * satu baris adalah satu pesan pengumuman singkat yang ditampilkan berjalan pada UI portal, dengan
 * cakupan tampil yang bisa disempitkan ke satu fakultas/jurusan (modul PT), atau ke satu
 * sekolah/yayasan (modul sekolah), tergantung institusi mana yang memakainya.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "text_berjalan")
public class TextBerjalan extends GeneralValueObject {

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

	private String nama;
	private String keterangan;
	private Fakultas fakultas;
	private Jurusan jurusan;
	private PerguruanTinggi perguruanTinggi;
	private String program;
	private Sekolah sekolah;
	private Yayasan yayasan;
	private Boolean aktif;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public TextBerjalan() {
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
	 * @return isi teks berjalan (pesan yang ditampilkan), di-trim saat dibaca; string kosong bila
	 *         belum diisi (bukan {@code null}).
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? "" : this.nama.trim();
	}

	/**
	 * @param nama isi teks berjalan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan/catatan bebas tentang baris ini.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan/catatan bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @param jurusan jurusan cakupan tampil teks berjalan ini (modul PT).
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return jurusan cakupan tampil teks berjalan ini, atau {@code null} bila berlaku lintas
	 *         jurusan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param fakultas fakultas cakupan tampil teks berjalan ini (diabaikan saat dibaca kembali bila
	 *                 {@link #getJurusan()} terisi — lihat {@link #getFakultas()}).
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * @return fakultas cakupan tampil teks berjalan ini — bila {@link #getJurusan()} terisi,
	 *         fakultas selalu DITURUNKAN dari jurusan itu ({@code jurusan.getFakultas()}), menimpa
	 *         nilai kolom {@code fakultas} tersimpan (getter-mutasi, pola yang sama dipakai
	 *         {@link AbsenPiketMahasiswa#getFakultas()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		if (getJurusan() != null) {
			fakultas = getJurusan().getFakultas();
		}
		return fakultas;
	}

	/**
	 * @param yayasan yayasan cakupan tampil teks berjalan ini (diabaikan saat dibaca kembali bila
	 *                {@link #getSekolah()} terisi — lihat {@link #getYayasan()}); {@code null} bila
	 *                {@code yayasan} tanpa id (belum tersimpan).
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * @return yayasan cakupan tampil teks berjalan ini — bila {@link #getSekolah()} terisi, yayasan
	 *         selalu DITURUNKAN dari sekolah itu ({@code sekolah.getYayasan()}), menimpa nilai kolom
	 *         {@code yayasan} tersimpan (getter-mutasi).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		if (getSekolah() != null) {
			yayasan = getSekolah().getYayasan();
		}
		return yayasan;
	}

	/**
	 * @return sekolah cakupan tampil teks berjalan ini (modul sekolah), atau {@code null} bila
	 *         berlaku lintas sekolah/bukan modul sekolah.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * @param sekolah sekolah cakupan tampil teks berjalan ini; {@code null} bila {@code sekolah}
	 *                tanpa id (belum tersimpan).
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * @return nama program (label bebas) terkait teks berjalan ini, atau {@code null} bila kosong
	 *         (string kosong/whitespace dinormalisasi menjadi {@code null}).
	 */
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program;
	}

	/**
	 * @param program nama program (label bebas) terkait teks berjalan ini.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * @return perguruan tinggi pemilik teks berjalan ini; bila belum diisi, jatuh balik ke perguruan
	 *         tinggi tunggal aplikasi ({@code PerguruanTinggiUtil#getPerguruanTinggi()}), dengan
	 *         kegagalan lookup diserap diam-diam — getter-mutasi dengan fallback fail-safe.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi", nullable = true)
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/TextBerjalan.java:193");
		}
		return perguruanTinggi;
	}

	/**
	 * @param perguruanTinggi perguruan tinggi pemilik teks berjalan ini.
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * @return {@code true} bila teks berjalan ini aktif/ditampilkan; default {@code true} bila belum
	 *         diisi.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif/nonaktif teks berjalan ini.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
