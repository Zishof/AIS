package ais.database.model.penelitiandanpengabdian;

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




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;



import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;

/**
 * Model entitas <b>satu baris anggota tim</b> pada satu pengajuan proposal penelitian/pengabdian
 * ({@link PengajuanPenelitianDanPengabdian}). Anggota dapat berupa dosen/pegawai ({@link Tbmuser})
 * atau mahasiswa ({@link Mahasiswa}) — persis satu dari keduanya yang diisi untuk satu baris,
 * dipilih lewat kombinasi getter/setter {@link #getTbmuser()}/{@link #getMahasiswa()}.
 *
 * <p>
 * Tabel ini adalah representasi <b>terstruktur</b> (satu baris per anggota, dapat direlasikan ke
 * {@link Tbmuser}/{@link Mahasiswa} asli) dari daftar anggota, berbeda dengan field teks bebas
 * {@link PengajuanPenelitianDanPengabdian#getAnggota()} pada entitas induk yang hanya menyimpan
 * daftar username/NIM sebagai satu string dipisah koma. Baris di tabel ini dibangun ulang (hapus
 * lalu insert) oleh {@code PengajuanPenelitianDanPengabdianHelper.onSave} berdasarkan isi field teks
 * tersebut setiap kali proposal disimpan.
 * </p>
 *
 * <p>
 * <b>Perhatian — getter destruktif:</b> {@link #getTbmuser()} akan menulis ulang (menghapus) field
 * {@code tbmuser} ke {@code null} sebagai efek samping bila {@code mahasiswa} sudah terisi, meskipun
 * dipanggil hanya untuk membaca nilai. Lihat javadoc method tersebut.
 * </p>
 *
 * @see PengajuanPenelitianDanPengabdian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "penelitiandanpengabdian", name = "anggota_pengajuan_penelitian_dan_pengabdian")



public class AnggotaPengajuanPenelitianDanPengabdian extends GeneralValueObject {

	/**
	 * Versi kelas untuk kebutuhan serialisasi ({@link java.io.Serializable}). Nilai ini disalin
	 * (copy-paste) dari entitas lain dalam paket yang sama saat dibuat oleh hbm2java dan tidak
	 * pernah diubah sejak — tidak merefleksikan riwayat perubahan struktur kelas ini secara individual.
	 */
	private static final long serialVersionUID = 2463812577548439808L;
	/** Primary key baris anggota, auto-increment ({@code IDENTITY}) pada kolom {@code id}. */
	private Long id;
	/** Field audit legacy: nama pengguna yang melakukan perubahan terakhir (bebas format, isi manual). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Field audit legacy: id/username pengguna yang melakukan perubahan terakhir. Lihat {@link #getOlehId()}. */
	private String olehId;

	/** @return id/username pengguna yang tercatat melakukan perubahan terakhir pada baris ini (field audit legacy, tidak dipetakan sebagai kolom entitas — bukan {@code @Column}). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pencatat perubahan terakhir. Nilai {@code null} atau string kosong/spasi
	 * diabaikan (tidak menimpa nilai yang sudah tersimpan) — dipakai agar pemanggil yang tidak
	 * membawa konteks pengguna tidak menghapus jejak audit yang sudah ada.
	 *
	 * @param olehId id/username pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pencatat perubahan terakhir. Nilai {@code null} atau string kosong/spasi
	 * diabaikan, dengan alasan yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang tercatat melakukan perubahan terakhir pada baris ini (field audit legacy, tidak dipetakan sebagai kolom entitas). */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence sesaat sebelum
	 * setiap {@code UPDATE} baris ini dieksekusi, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} ke waktu saat ini. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengatur cap waktu perubahan terakhir secara manual. Dalam alur normal field ini
	 * diperbarui otomatis lewat {@link #onUpdate()}; setter ini terutama dipakai saat memuat ulang
	 * data dari sumber lain (mis. import) yang perlu mempertahankan cap waktu aslinya.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return cap waktu perubahan terakhir baris ini; diinisialisasi ke waktu pembuatan objek dan diperbarui otomatis oleh {@link #onUpdate()} pada setiap {@code UPDATE}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi teks ringkas baris ini untuk keperluan log/debug: gabungan {@code tbmuser}, {@code mahasiswa}, dan {@code pengajuanPenelitianDanPengabdian} (memakai {@code toString()} masing-masing objek, termasuk saat {@code null}). */
	public String toString() {
		return tbmuser + "_" + mahasiswa + "_" + pengajuanPenelitianDanPengabdian;
	}

	/** Dosen/pegawai anggota tim, bila anggota ini seorang {@link Tbmuser} (bukan mahasiswa). Lihat {@link #getTbmuser()} untuk perilaku getter yang saling meniadakan dengan {@link #mahasiswa}. */
	private Tbmuser tbmuser;
	/** Mahasiswa anggota tim, bila anggota ini seorang {@link Mahasiswa} (bukan dosen/pegawai). Mengisi field ini membuat {@link #getTbmuser()} mengosongkan {@link #tbmuser} pada pemanggilan berikutnya. */
	private Mahasiswa mahasiswa;
	/** Catatan/keterangan bebas untuk baris anggota ini (mis. peran dalam tim). Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Proposal pengajuan penelitian/pengabdian induk yang memiliki baris anggota ini (wajib, FK {@code NOT NULL}). */
	private PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian;

	/** Konstruktor default (wajib untuk entitas Hibernate/JPA); seluruh field diisi lewat setter. */
	public AnggotaPengajuanPenelitianDanPengabdian() {
	}

	/** @return primary key baris anggota ini, atau {@code null} bila belum tersimpan (belum di-{@code INSERT}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengatur id baris ini secara manual. Karena kolom {@code id} dipetakan {@code insertable = false}
	 * (nilai dihasilkan basis data lewat {@code IDENTITY}), pengaturan manual di sini hanya berguna
	 * untuk menandai objek yang mewakili baris yang sudah ada (mis. hasil query manual), bukan untuk
	 * memaksa id tertentu saat {@code INSERT}.
	 *
	 * @param id primary key yang ingin diasosiasikan ke objek ini
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return catatan/keterangan baris anggota ini, apa adanya (bisa {@code null}, tidak di-trim seperti pada beberapa entitas lain di paket ini). */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan baru untuk baris anggota ini; boleh {@code null}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * <b>Getter destruktif:</b> selain mengembalikan nilai, method ini <b>menulis ulang state</b> —
	 * bila {@link #mahasiswa} sudah terisi (bukan {@code null}), field {@link #tbmuser} langsung
	 * di-{@code null}-kan sebelum dikembalikan, sebagai penegakan aturan "anggota adalah dosen ATAU
	 * mahasiswa, tidak pernah keduanya". Efek ini permanen pada instance yang sedang dipegang: bila
	 * instance ini kemudian di-{@code save}/{@code update}, {@code tbmuser} akan ikut tersimpan
	 * {@code null} walau sebelumnya terisi. Nilai hasil juga melewati {@code check()} (helper
	 * {@link GeneralValueObject}) untuk menahan referensi ke baris {@link Tbmuser} yang sudah tidak
	 * ada/tidak valid.
	 *
	 * @return dosen/pegawai anggota tim, atau {@code null} bila anggota ini mahasiswa atau tidak diisi
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
	 * Mengatur dosen/pegawai anggota tim secara langsung, tanpa mengosongkan {@link #mahasiswa}.
	 * Pemanggil bertanggung jawab memastikan hanya satu dari {@code tbmuser}/{@code mahasiswa} yang
	 * bermakna untuk baris ini — pengosongan silang hanya terjadi lewat {@link #getTbmuser()}, bukan
	 * di setter ini.
	 *
	 * @param tbmuser dosen/pegawai yang menjadi anggota tim; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * @return mahasiswa anggota tim, atau {@code null} bila anggota ini dosen/pegawai atau tidak
	 *         diisi. Berbeda dengan {@link #getTbmuser()}, getter ini <b>tidak</b> memiliki efek
	 *         samping (tidak mengosongkan {@link #tbmuser} maupun memanggil {@code check()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Mengatur mahasiswa anggota tim. Mengisi field ini dengan nilai bukan-{@code null} akan membuat
	 * pemanggilan {@link #getTbmuser()} berikutnya mengosongkan {@link #tbmuser} (lihat javadoc
	 * method tersebut) — namun setter ini sendiri tidak langsung mengosongkan apa pun.
	 *
	 * @param mahasiswa mahasiswa yang menjadi anggota tim; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return proposal pengajuan penelitian/pengabdian induk yang memiliki baris anggota ini (FK wajib, tidak pernah {@code null} pada baris yang sudah tersimpan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengajuan_penelitian_dan_pengabdian", nullable = false)
	public PengajuanPenelitianDanPengabdian getPengajuanPenelitianDanPengabdian() {
		return pengajuanPenelitianDanPengabdian;
	}

	/** @param pengajuanPenelitianDanPengabdian proposal pengajuan induk yang memiliki baris anggota ini. */
	public void setPengajuanPenelitianDanPengabdian(PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian) {
		this.pengajuanPenelitianDanPengabdian = pengajuanPenelitianDanPengabdian;
	}

}
