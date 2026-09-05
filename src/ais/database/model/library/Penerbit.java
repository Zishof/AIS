package ais.database.model.library;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;



/**
 * Entitas master <b>penerbit</b> (tabel {@code library.penerbit}) — data induk perusahaan/lembaga
 * penerbit buku dan bahan pustaka pada modul perpustakaan. Data yang disimpan hanya identitas dan
 * kontak dasar ({@link #getNama()}, {@link #getAlamat()}, {@link #getKodePos()}, {@link #getTelp()},
 * {@link #getFax()}, {@link #getKontak()}, {@link #getEmail()}, {@link #getKeterangan()}), tanpa
 * kolom status/aktif — berbeda dari {@link Pengarang} yang punya bidang {@code aktif}.
 *
 * <h2>Relasi ke pemeriksa/reviewer</h2>
 * <p>Penerbit dapat memiliki daftar pemeriksa (user yang ditugaskan meninjau) lewat
 * {@link PenerbitPunyaPemeriksa}, masing-masing dapat dikaitkan ke satu {@code DomainPenelitian}
 * (domain/bidang penelitian) dan punya status aktif per baris. Ini <b>berbeda</b> dari
 * {@link ItemPunyaPemeriksa}, yang menautkan pemeriksa langsung ke {@link Item} (bukan lewat
 * penerbit) untuk alur review karya ilmiah/koleksi per item. Penerbit sendiri <b>tidak</b> tertaut
 * langsung ke {@link Item} — tidak ada bidang penerbit pada {@link ItemPunyaTerbit} (entitas
 * "terbitan" milik item ternyata murni berisi konten+rentang tanggal per satuan kerja/perpustakaan,
 * tidak menunjuk penerbit manapun); keterkaitan penerbit dengan sebuah item, bila ada, dikelola di
 * luar berkas entitas ini.</p>
 *
 * <h2>Auto-isi satuan kerja (baris baru saja)</h2>
 * <p>{@link #getSatuanKerja()} mengisi otomatis {@link #satuanKerja} hanya untuk baris yang
 * <b>belum pernah disimpan</b> ({@link #id} masih {@code null}) dan bidangnya masih kosong: diambil
 * dari satuan kerja pengguna aktif ({@link Tbmuser#ambilSatuanKerja()}), atau bila itu kosong, dari
 * satuan kerja {@link Perpustakaan} yang sedang aktif di sesi ({@code Common.getCurrentPerpustakaan()}).
 * Baris yang sudah tersimpan tidak pernah disentuh ulang oleh getter ini. Pola ini konsisten dengan
 * mekanisme auto-seed satuan kerja yang berulang di entitas master modul perpustakaan lain.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see PenerbitPunyaPemeriksa
 * @see ItemPunyaPemeriksa
 * @see ItemPunyaTerbit
 * @see SatuanKerja
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "penerbit")



public class Penerbit extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah data penerbit ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah data penerbit ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan, agar bidang audit bayangan ini tidak pernah
	 * ditimpa kosong oleh jalur salin/klon objek.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah data penerbit ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah data penerbit ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Interceptor-lah
	 * yang mengisi {@link #oleh}, {@link #olehId}, dan {@link #getTanggal_dirubah()} dari konteks
	 * pengguna aktif. Method sengaja {@code protected} dan tidak boleh dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor},
	 * bukan oleh form.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris penerbit ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: nama penerbit apa adanya (bidang mentah, bukan hasil
	 * {@link #getNama()} yang dipangkas), dipakai label bawaan komponen ZK dan penelusuran log.
	 *
	 * @return nama penerbit apa adanya; dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Nama perusahaan/lembaga penerbit. */
	private String nama;
	/** Alamat jalan penerbit. */
	private String alamat;
	/** Kode pos alamat penerbit. */
	private String kodePos;
	/** Nomor telepon penerbit. */
	private String telp;
	/** Nomor faks penerbit. */
	private String fax;
	/** Nama orang yang dapat dihubungi di pihak penerbit. */
	private String kontak;
	/** Alamat surel penerbit. */
	private String email;
	/** Catatan bebas tentang penerbit. */
	private String keterangan;

	/** Satuan kerja pemilik data penerbit ini; diisi otomatis untuk baris baru oleh {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public Penerbit() {
	}

	/**
	 * Konstruktor pintasan untuk membuat referensi ringan ke penerbit yang sudah dikenal id-nya
	 * (mis. untuk dipakai sebagai kriteria pencarian), tanpa memuat bidang lain.
	 *
	 * @param id kunci utama penerbit yang sudah ada
	 */
	public Penerbit(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci utama baris penerbit ini.
	 *
	 * @return id penerbit, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris penerbit ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama penerbit setelah dipangkas spasi tepinya.
	 *
	 * @return nama penerbit yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama penerbit.
	 *
	 * @param nama nama penerbit
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas tentang penerbit.
	 *
	 * @return catatan bebas; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang penerbit.
	 *
	 * @param keterangan teks catatan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan alamat jalan penerbit.
	 *
	 * @return alamat; boleh {@code null}
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menyetel alamat jalan penerbit.
	 *
	 * @param alamat alamat; boleh {@code null}
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan kode pos alamat penerbit.
	 *
	 * @return kode pos; boleh {@code null}
	 */
	public String getKodePos() {
		return kodePos;
	}

	/**
	 * Menyetel kode pos alamat penerbit.
	 *
	 * @param kodePos kode pos; boleh {@code null}
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Mengembalikan nomor telepon penerbit.
	 *
	 * @return nomor telepon; boleh {@code null}
	 */
	public String getTelp() {
		return telp;
	}

	/**
	 * Menyetel nomor telepon penerbit.
	 *
	 * @param telp nomor telepon; boleh {@code null}
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Mengembalikan nomor faks penerbit.
	 *
	 * @return nomor faks; boleh {@code null}
	 */
	public String getFax() {
		return fax;
	}

	/**
	 * Menyetel nomor faks penerbit.
	 *
	 * @param fax nomor faks; boleh {@code null}
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}

	/**
	 * Mengembalikan nama orang yang dapat dihubungi di pihak penerbit.
	 *
	 * @return nama kontak; boleh {@code null}
	 */
	public String getKontak() {
		return kontak;
	}

	/**
	 * Menyetel nama orang yang dapat dihubungi di pihak penerbit.
	 *
	 * @param kontak nama kontak; boleh {@code null}
	 */
	public void setKontak(String kontak) {
		this.kontak = kontak;
	}

	/**
	 * Mengembalikan alamat surel penerbit.
	 *
	 * @return alamat surel; boleh {@code null}
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Menyetel alamat surel penerbit.
	 *
	 * @param email alamat surel; boleh {@code null}
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Mengembalikan satuan kerja pemilik data penerbit ini, mengisi otomatis bila kosong <b>hanya
	 * untuk baris yang belum pernah disimpan</b> (id masih {@code null}): diambil lebih dulu dari
	 * satuan kerja pengguna aktif ({@code Common.getCurrentUser().ambilSatuanKerja()}), atau bila
	 * itu {@code null}, dari satuan kerja {@link Perpustakaan} yang sedang aktif di sesi
	 * ({@code Common.getCurrentPerpustakaan()}). Dimuat dengan {@link FetchMode#SELECT}. Baris yang
	 * sudah tersimpan (id tidak {@code null}) tidak pernah diisi ulang lewat jalur ini, apa pun nilai
	 * bidangnya.
	 *
	 * @return satuan kerja pemilik data penerbit; dapat tetap {@code null} bila baris baru dan tidak
	 *         ada pengguna aktif maupun perpustakaan aktif yang dapat dijadikan sumber
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (this.satuanKerja == null && this.id == null) {
			Tbmuser pengguna = Common.getCurrentUser();
			SatuanKerja satuanKerjaBaru = pengguna == null ? null : pengguna.ambilSatuanKerja();
			Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
			if (satuanKerjaBaru == null && currentPerpustakaan != null) {
				satuanKerjaBaru = currentPerpustakaan.getSatuanKerja();
			}
			this.satuanKerja = satuanKerjaBaru;
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik data penerbit ini secara eksplisit, melewati auto-isi
	 * {@link #getSatuanKerja()}.
	 *
	 * @param satuanKerja satuan kerja baru; boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
