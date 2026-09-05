package ais.database.model.library;

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




import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;



/**
 * Entitas <b>terbitan</b> (tabel {@code library.item_punya_terbit}) yang menyimpan satu entri
 * konten kaya (rich text) terkait sebuah {@link Item} pustaka, berlaku pada rentang tanggal
 * {@link #getMulai()}–{@link #getSampai()} dan diskop ke satu {@link SatuanKerja}+
 * {@link Perpustakaan} tertentu. <b>Meski namanya "terbit", entitas ini tidak menunjuk
 * {@link Penerbit} manapun</b> — tidak ada bidang penerbit, tahun, edisi, atau cetakan di sini;
 * "terbit" pada nama kelas ini berarti "konten yang diterbitkan/dipublikasikan" untuk item
 * tersebut (mis. pengumuman, catatan penerbitan, atau ringkasan tambahan), bukan data bibliografi
 * penerbit. Dikelola sepenuhnya lewat
 * {@code ais.action.master.library.helper.ItemPunyaTerbitHelper}: satu {@link Item} dapat memiliki
 * berapa pun baris terbitan (satu per kombinasi satuan kerja/perpustakaan/rentang tanggal), dengan
 * konten default diisi dari abstrak (ID lalu EN) atau catatan item bila kolom {@link #content}
 * belum diisi manual. Baris langsung tersimpan ke basis data bila {@link Item} induk sudah
 * persisten; bila item masih baru, baris hanya ditahan di grid UI sampai proses simpan item induk.
 *
 * <p>Komentar atas satu baris terbitan ditampung terpisah lewat {@link ItemPunyaTerbitKomentar}
 * (relasi banyak-ke-satu ke kelas ini).</p>
 *
 * <h2>Auto-isi satuan kerja dan perpustakaan (baris baru saja)</h2>
 * <p>Seperti {@link Penerbit#getSatuanKerja()}, {@link #getSatuanKerja()} mengisi otomatis
 * {@link #satuanKerja} hanya untuk baris yang belum pernah disimpan dan bidangnya masih kosong,
 * dari satuan kerja pengguna aktif atau (bila kosong) satuan kerja {@link Perpustakaan} yang
 * sedang aktif di sesi; kegagalan resolusi ditelan diam-diam lewat blok {@code try/catch} kosong
 * yang direkam ke {@code ErrorAuditUtil} (jejak audit pasif, bukan penanganan error aktif).
 * {@link #getPerpustakaan()} punya perilaku berbeda dan lebih agresif: ia mengisi
 * {@link #perpustakaan} dari perpustakaan aktif di sesi <b>kapan pun bidangnya masih kosong,
 * termasuk pada baris yang sudah tersimpan</b> ({@link #id} tidak {@code null}), dan bila begitu
 * langsung memicu {@code UPDATE} ke basis data lewat {@code Common.refreshUpdate(session, this)} —
 * getter ini punya efek samping penulisan basis data, bukan sekadar pembacaan.</p>
 *
 * <h2>Bidang {@code aktif} — kondisi hampir selalu tak terpenuhi</h2>
 * <p>{@link #getAktif()} dihitung ulang setiap dipanggil dari perbandingan
 * {@code sampai.before(sekarang) && mulai.after(sekarang)} — yakni <b>tanggal selesai sudah lewat
 * DAN tanggal mulai belum tiba</b>. Untuk data wajar di mana {@link #mulai} mendahului
 * {@link #sampai}, kombinasi ini nyaris mustahil terpenuhi kecuali {@code mulai} berada di masa
 * depan sekaligus {@code sampai} sudah di masa lalu (data dengan {@code sampai} sebelum
 * {@code mulai}) — sehingga {@link #getAktif()} pada praktiknya hampir selalu mengembalikan
 * {@code false} walau baris terbitan sedang berada dalam rentang tanggal berlaku
 * ({@code mulai} sudah lewat, {@code sampai} belum tiba). Diperlakukan sebagai perilaku aktual
 * kode saat ini, bukan diperbaiki di sini; bidang ini juga tidak dipetakan ke kolom basis data
 * ({@code @Column} tidak ada) sehingga nilainya tidak pernah dipersisten.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see Item
 * @see Penerbit
 * @see ItemPunyaTerbitKomentar
 * @see Perpustakaan
 * @see SatuanKerja
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "item_punya_terbit")



public class ItemPunyaTerbit extends GeneralValueObject {

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
	 * Mengembalikan id pengguna yang terakhir mengubah baris terbitan ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris terbitan ini.
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
	 * Menyetel nama pengguna yang terakhir mengubah baris terbitan ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris terbitan ini.
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
	 * Mengembalikan stempel waktu perubahan terakhir baris terbitan ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Satuan kerja pemilik baris terbitan ini; diisi otomatis untuk baris baru oleh {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Perpustakaan pemilik baris terbitan ini; diisi otomatis (dan dapat memicu update basis data) oleh {@link #getPerpustakaan()}. */
	private Perpustakaan perpustakaan;
	/** Item pustaka yang terbitan ini terkait. */
	private Item item;
	/** Tanggal mulai berlaku terbitan; bawaan waktu pembuatan objek. */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal berakhir berlaku terbitan; boleh kosong (tanpa batas akhir). */
	private Date sampai;
	/** Konten kaya (HTML) terbitan; wajib diisi (lihat {@code @Column(nullable = false)} pada {@link #getContent()}). */
	private String content;
	/** Status aktif hasil hitung {@link #getAktif()}; lihat catatan pada javadoc kelas soal logika perbandingannya. */
	private Boolean aktif;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public ItemPunyaTerbit() {
	}

	/**
	 * Konstruktor pintasan untuk membuat referensi ringan ke baris terbitan yang sudah dikenal
	 * id-nya, tanpa memuat bidang lain.
	 *
	 * @param id kunci utama baris terbitan yang sudah ada
	 */
	public ItemPunyaTerbit(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci utama baris terbitan ini.
	 *
	 * @return id baris terbitan, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris terbitan ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan satuan kerja pemilik baris terbitan ini, mengisi otomatis bila kosong <b>hanya
	 * untuk baris yang belum pernah disimpan</b> (id masih {@code null}): diambil dari satuan kerja
	 * pengguna aktif, atau bila kosong, dari satuan kerja {@link Perpustakaan} yang sedang aktif di
	 * sesi. Kegagalan pada blok resolusi ini (mis. tidak ada pengguna aktif yang menyebabkan
	 * {@code NullPointerException} pada {@code Common.getCurrentUser().ambilSatuanKerja()}) ditelan
	 * oleh {@code catch (Exception e)} yang hanya merekam ke {@code ErrorAuditUtil} — bidang tetap
	 * {@code null} tanpa melempar apa pun ke pemanggil. Dimuat dengan {@link FetchMode#SELECT}.
	 *
	 * @return satuan kerja pemilik baris terbitan; dapat tetap {@code null} bila baris baru dan
	 *         resolusi gagal atau tidak ada sumber yang tersedia
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (this.satuanKerja == null && this.id == null) {
			try {
				SatuanKerja satuanKerja = Common.getCurrentUser().ambilSatuanKerja();
				Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
				if (satuanKerja == null && currentPerpustakaan != null) {
					satuanKerja = currentPerpustakaan.getSatuanKerja();
				}
				this.satuanKerja = satuanKerja;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/library/ItemPunyaTerbit.java:124");
			}
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik baris terbitan ini secara eksplisit, melewati auto-isi
	 * {@link #getSatuanKerja()}.
	 *
	 * @param satuanKerja satuan kerja baru; boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan item pustaka yang terbitan ini terkait. Proxy lazy diresolusi lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return item terkait, atau {@code null} bila belum disetel
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel item pustaka yang terbitan ini terkait.
	 *
	 * @param item item terkait; boleh {@code null}
	 */
	public void setItem(Item item) {
		this.item = item;
	}

	/**
	 * Mengembalikan status aktif baris terbitan, dihitung ulang setiap dipanggil dari
	 * {@link #mulai}/{@link #sampai} terhadap waktu sekarang: {@code true} hanya bila
	 * {@code sampai} sudah lewat DAN {@code mulai} belum tiba (lihat penjelasan lengkap pada
	 * javadoc kelas soal mengapa kombinasi ini nyaris tidak pernah terpenuhi untuk data wajar).
	 * Nilai balik <b>tidak dipetakan ke kolom basis data manapun</b> — bidang {@link #aktif} hanya
	 * hidup di memori dan ditimpa ulang setiap panggilan ini.
	 *
	 * @return {@code true} bila {@code sampai} sudah lewat dan {@code mulai} belum tiba (kondisi
	 *         yang pada praktiknya nyaris selalu {@code false} untuk rentang tanggal yang wajar);
	 *         {@code false} pada kasus lain, termasuk bila {@link #mulai}/{@link #sampai} kosong
	 */
	public Boolean getAktif() {
		if (mulai != null && sampai != null && sampai.before(ais.ui.util.WaktuUtil.getDate())
				&& mulai.after(ais.ui.util.WaktuUtil.getDate())) {
			aktif = true;
		} else {
			aktif = false;
		}
		return aktif;
	}

	/**
	 * Menyetel bidang {@code aktif} secara manual. Karena tidak dipetakan ke kolom basis data dan
	 * {@link #getAktif()} selalu menghitung ulang nilainya sendiri, nilai yang disetel di sini akan
	 * langsung ditimpa pada pemanggilan {@link #getAktif()} berikutnya.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan perpustakaan pemilik baris terbitan ini. Berbeda dari
	 * {@link #getSatuanKerja()}, auto-isi di sini berlaku <b>kapan pun bidangnya masih kosong,
	 * termasuk pada baris yang sudah tersimpan</b>: diambil dari perpustakaan yang sedang aktif di
	 * sesi ({@code Common.getCurrentPerpustakaan()}), dan bila baris ini sudah punya id, perubahan
	 * tersebut <b>langsung dipersisten</b> lewat {@code Common.refreshUpdate(session, this)} — efek
	 * samping penulisan basis data dari sebuah getter. Proxy lazy hasil akhirnya diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * @return perpustakaan pemilik baris terbitan; dapat tetap {@code null} bila tidak ada
	 *         perpustakaan aktif di sesi saat pertama kali dipanggil
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perpustakaan", nullable = true)
	public Perpustakaan getPerpustakaan() {
		if (perpustakaan == null && Common.getCurrentPerpustakaan() != null) {
			perpustakaan = Common.getCurrentPerpustakaan();
			setPerpustakaan(perpustakaan);
			if (id != null) {
				Session session = HibernateUtil.currentSession();
				Common.refreshUpdate(session, this);
			}
		}
		perpustakaan = check(perpustakaan);
		return perpustakaan;
	}

	/**
	 * Menyetel perpustakaan pemilik baris terbitan ini.
	 *
	 * @param perpustakaan perpustakaan terkait; boleh {@code null}
	 */
	public void setPerpustakaan(Perpustakaan perpustakaan) {
		this.perpustakaan = perpustakaan;
	}

	/**
	 * Mengembalikan tanggal mulai berlaku terbitan ini.
	 *
	 * @return tanggal mulai; wajib diisi menurut {@code @Column(nullable = false)}, namun bawaan
	 *         objek baru sudah terisi waktu pembuatan sehingga jarang {@code null} dalam praktik
	 */
	@Column(name = "mulai", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Menyetel tanggal mulai berlaku terbitan ini.
	 *
	 * @param mulai tanggal mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan tanggal berakhir berlaku terbitan ini.
	 *
	 * @return tanggal berakhir; boleh {@code null} (tanpa batas akhir)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Menyetel tanggal berakhir berlaku terbitan ini.
	 *
	 * @param sampai tanggal berakhir baru; boleh {@code null}
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan konten kaya (HTML) terbitan ini, diisi lewat editor {@code MyCkEditor} pada
	 * layar terkait (default diambil dari abstrak/catatan item bila kosong saat pembuatan).
	 *
	 * @return konten terbitan; wajib diisi menurut {@code @Column(nullable = false)}
	 */
	@Column(name = "content", nullable = false, columnDefinition = "text")
	public String getContent() {
		return content;
	}

	/**
	 * Menyetel konten kaya (HTML) terbitan ini.
	 *
	 * @param content konten baru
	 */
	public void setContent(String content) {
		this.content = content;
	}

}
