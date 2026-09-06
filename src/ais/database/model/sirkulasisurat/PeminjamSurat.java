package ais.database.model.sirkulasisurat;

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

import ais.common.BarcodeCommon;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

/**
 * Entitas Hibernate (skema {@code surat}, tabel {@code Peminjam_surat}) yang menyimpan data
 * PEMINJAM pada modul sirkulasi/peminjaman fisik surat-menyurat: satu baris mewakili satu identitas
 * peminjam, yang dapat berupa {@link Mahasiswa}, {@link Dosen}, {@link Pegawai}, {@link Guru},
 * {@link Siswa}, atau {@link Tbmuser} (akun umum), tergantung siapa yang meminjam surat/dokumen.
 * Entitas ini adalah data master peminjam yang dipakai berulang oleh {@link PeminjamanSuratItem}
 * (satu peminjam bisa membuat banyak transaksi peminjaman).
 *
 * <p>Meng-extend {@link GeneralValueObject} dan memakai {@code @Audited} (Envers) — konsisten
 * dengan konvensi audit standar AIS, berbeda dari paket {@code radius} yang mengikuti skema baku
 * FreeRADIUS tanpa audit.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "Peminjam_surat")
public class PeminjamSurat extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** Field audit shadow (bukan kolom Hibernate): nama pemroses terakhir, diisi lewat {@link #setOleh(String)}. */
	private String oleh;
	/** Field audit shadow (bukan kolom Hibernate): ID pemroses terakhir, diisi lewat {@link #setOlehId(String)}. */
	private String olehId;

	/** @return ID pemroses terakhir yang mengubah baris ini (field audit shadow, lihat {@link #setOlehId(String)}). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed): bila
	 * {@code olehId} null atau hanya berisi spasi, method ini langsung {@code return} tanpa
	 * mengubah field, sehingga nilai audit sebelumnya tetap dipertahankan alih-alih ditimpa kosong.
	 *
	 * @param olehId ID pemroses yang akan diset; diabaikan bila null/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed) dengan
	 * pola yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pemroses yang akan diset; diabaikan bila null/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pemroses terakhir yang mengubah baris ini (field audit shadow, lihat {@link #setOleh(String)}). */
	public String getOleh() {
		return oleh;
	}

	/** Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini di-update. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang akan diset. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini (diperbarui otomatis lewat {@link #onUpdate()}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas: kode peminjam diikuti nama (diresolusi dari salah satu relasi identitas yang terisi). */
	public String toString() {
		mahasiswa = getMahasiswa();
		dosen = getDosen();
		guru = getGuru();
		siswa = getSiswa();
		pegawai = getPegawai();
		tbmuser = getTbmuser();
		String nama = "";
		if (mahasiswa != null) {
			nama = mahasiswa.getNim() + " - " + mahasiswa.getNama();
		} else if (dosen != null) {
			nama = dosen.getCode() + " - " + dosen.getNama();
		} else if (siswa != null) {
			nama = siswa.getNomorIndukNasional() + " - " + siswa.getNama();
		} else if (guru != null) {
			nama = guru.getKode() + " - " + guru.getNama();
		} else if (pegawai != null) {
			nama = pegawai.getCode() + " - " + pegawai.getNama();
		} else if (tbmuser != null) {
			nama = tbmuser.getUserId() + " - " + tbmuser.getUserNama();
		} else {
			nama = this.nama;
		}
		return kode + " - " + nama;
	}

	/** Kode identitas resmi peminjam (mis. nomor KTP/kartu identitas lain), diisi manual bila peminjam bukan warga internal (tanpa relasi Mahasiswa/Dosen/dst). */
	private String kodeIdentitas;
	/** Jenis identitas resmi yang dipakai (mis. "KTP", "SIM"), pasangan dari {@link #kodeIdentitas}. */
	private String jenisIdentitas;
	/** Kode unik peminjam, di-generate otomatis dari {@link BarcodeCommon#generateCode()} bila belum diisi (lihat {@link #getKode()}). */
	private String kode;
	/** Nama peminjam — diresolusi otomatis dari relasi identitas yang terisi (lihat {@link #getNama()}), atau diisi manual bila tidak ada relasi. */
	private String nama;
	/** Alamat peminjam (kolom {@code text}, tanpa batas panjang praktis). */
	private String alamat;

	/** Relasi ke {@link Mahasiswa} bila peminjam adalah mahasiswa. */
	private Mahasiswa mahasiswa;
	/** Relasi ke {@link Siswa} bila peminjam adalah siswa (modul sekolah). */
	private Siswa siswa;
	/** Relasi ke {@link Dosen} bila peminjam adalah dosen. */
	private Dosen dosen;
	/** Relasi ke {@link Guru} bila peminjam adalah guru (modul sekolah). */
	private Guru guru;
	/** Relasi ke {@link Pegawai} bila peminjam adalah pegawai. */
	private Pegawai pegawai;
	/** Relasi ke {@link Tbmuser} bila peminjam adalah pemegang akun umum (bukan salah satu peran di atas). */
	private Tbmuser tbmuser;
	/** Keterangan bebas tentang peminjam ini. */
	private String keterangan;
	/** Nomor telepon rumah/kantor peminjam. */
	private String telp;
	/** Nomor HP peminjam. */
	private String hp;
	/** Alamat email peminjam. */
	private String email;
	/** Status aktif/nonaktif peminjam; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif = true;

	/** Tanggal data peminjam ini dibuat/didaftarkan; default saat instansiasi bila belum diisi eksplisit. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Pengguna ({@link Tbmuser}) yang mendaftarkan/membuat data peminjam ini. */
	private Tbmuser dibuatOleh;

	/** Jumlah kali perpanjangan peminjaman yang sudah dipakai/diizinkan untuk peminjam ini (dipakai sebagai batas kebijakan peminjaman, konteks lengkap ada di helper terkait). */
	private Integer perpanjang;
	/** Jumlah maksimal item yang boleh dipinjam sekaligus oleh peminjam ini. */
	private Integer maksimal;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public PeminjamSurat() {
	}

	/** @return ID baris (primary key, auto-increment). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return keterangan bebas tentang peminjam ini. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan yang akan diset. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return relasi {@link Mahasiswa} bila peminjam adalah mahasiswa (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param mahasiswa relasi Mahasiswa yang akan diset. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return relasi {@link Dosen} bila peminjam adalah dosen (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/** @param dosen relasi Dosen yang akan diset. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/** @return relasi {@link Pegawai} bila peminjam adalah pegawai (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/** @param pegawai relasi Pegawai yang akan diset. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/** @return relasi {@link Tbmuser} bila peminjam adalah pemegang akun umum (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/** @param tbmuser relasi Tbmuser yang akan diset. */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/** @return kode unik peminjam; di-generate otomatis lewat {@link BarcodeCommon#generateCode()} pada pemanggilan pertama bila belum diisi. */
	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	/** @param kode kode unik peminjam yang akan diset. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Meresolusi nama peminjam dari relasi identitas yang terisi, dengan urutan prioritas:
	 * {@link Siswa} &gt; {@link Guru} &gt; {@link Mahasiswa} &gt; {@link Dosen} &gt;
	 * {@link Pegawai} &gt; {@link Tbmuser} (memakai {@code getUserId()} untuk Tbmuser). Bila tidak
	 * ada relasi yang terisi, mengembalikan nilai {@link #nama} yang diisi manual.
	 *
	 * @return nama peminjam.
	 */
	public String getNama() {

		mahasiswa = getMahasiswa();
		dosen = getDosen();
		guru = getGuru();
		siswa = getSiswa();
		pegawai = getPegawai();
		tbmuser = getTbmuser();

		if (siswa != null) {
			nama = siswa.getNama();
		} else if (guru != null) {
			nama = guru.getNama();
		} else if (mahasiswa != null) {
			nama = mahasiswa.getNama();
		} else if (dosen != null) {
			nama = dosen.getNama();
		} else if (pegawai != null) {
			nama = pegawai.getNama();
		} else if (tbmuser != null) {
			nama = tbmuser.getUserId();
		}
		return nama;
	}

	/** @param nama nama peminjam yang akan diset (dipakai hanya bila tidak ada relasi identitas yang terisi, lihat {@link #getNama()}). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return alamat peminjam. */
	@Column(name = "alamat", nullable = true, columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	/** @param alamat alamat peminjam yang akan diset. */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/** @return kode identitas resmi peminjam (mis. nomor KTP), untuk peminjam tanpa relasi identitas internal. */
	@Column(name = "kode_identitas", nullable = true)
	public String getKodeIdentitas() {
		return kodeIdentitas;
	}

	/** @param kodeIdentitas kode identitas resmi yang akan diset. */
	public void setKodeIdentitas(String kodeIdentitas) {
		this.kodeIdentitas = kodeIdentitas;
	}

	/** @return jenis identitas resmi yang dipakai (mis. "KTP"). */
	public String getJenisIdentitas() {
		return jenisIdentitas;
	}

	/** @param jenisIdentitas jenis identitas resmi yang akan diset. */
	public void setJenisIdentitas(String jenisIdentitas) {
		this.jenisIdentitas = jenisIdentitas;
	}

	/** @return nomor telepon rumah/kantor peminjam. */
	public String getTelp() {
		return telp;
	}

	/** @param telp nomor telepon yang akan diset. */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/** @return nomor HP peminjam. */
	public String getHp() {
		return hp;
	}

	/** @param hp nomor HP yang akan diset. */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/** @return alamat email peminjam. */
	@Column(name = "email_PeminjamSurat")
	public String getEmail() {
		return email;
	}

	/** @param email alamat email yang akan diset. */
	public void setEmail(String email) {
		this.email = email;
	}

	/** @return status aktif peminjam; {@code null} pada data lama diperlakukan sebagai {@code true} (aktif). */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/** @param aktif status aktif yang akan diset. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return tanggal data peminjam dibuat; bila belum pernah diisi, di-default ke tanggal saat ini pada pemanggilan pertama. */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/** @param tanggal tanggal data dibuat, akan diset. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** @return pengguna yang membuat/mendaftarkan data peminjam ini (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		return dibuatOleh;
	}

	/** @param dibuatOleh pengguna pembuat data yang akan diset. */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/** @return jumlah kali perpanjangan yang sudah dipakai/diizinkan untuk peminjam ini. */
	public Integer getPerpanjang() {
		return perpanjang;
	}

	/** @param perpanjang jumlah perpanjangan yang akan diset. */
	public void setPerpanjang(Integer perpanjang) {
		this.perpanjang = perpanjang;
	}

	/** @return jumlah maksimal item yang boleh dipinjam sekaligus oleh peminjam ini. */
	public Integer getMaksimal() {
		return maksimal;
	}

	/** @param maksimal jumlah maksimal item yang akan diset. */
	public void setMaksimal(Integer maksimal) {
		this.maksimal = maksimal;
	}

	/** @return relasi {@link Siswa} bila peminjam adalah siswa (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/** @param siswa relasi Siswa yang akan diset. */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/** @return relasi {@link Guru} bila peminjam adalah guru (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/** @param guru relasi Guru yang akan diset. */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}
}
