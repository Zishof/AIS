package ais.database.model.kursus;

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
import ais.database.model.GeneralValueObject;

/**
 * Review/rating (1-5) yang diberikan seorang peserta untuk sebuah ProdukKursus.
 *
 * <h3>Kepemilikan ulasan -- identitas pengulas TIDAK dapat dipalsukan lewat jalur self-service</h3>
 * <p>
 * Entity ini murni model data (tidak ada method simpan di kelas ini). Ada dua jalur penulisan:
 * </p>
 * <ol>
 * <li><b>Self-service peserta</b> -- aksi {@code simpan_ulasan} di
 * {@code WEB-INF/baru/modul/kursus/_kursus_service.jsp}. {@link #getPesertaKursus()} DITETAPKAN
 * DARI SESI LOGIN (lewat {@code ensurePesertaKursus(db, tbmuser)}), BUKAN dari parameter request
 * yang dikirim client -- peserta tidak bisa menulis ulasan atas nama peserta lain. Endpoint ini
 * juga mensyaratkan enrollment berstatus {@code PesertaPunyaProdukKursus.TERBELI} untuk
 * {@code produkKursusId} yang direview (menolak dengan pesan "Anda hanya dapat memberi ulasan pada
 * kursus yang sudah dibeli" jika tidak ada), dan mem-VALIDASI UNIK (produkKursus, pesertaKursus) --
 * pemanggilan ulang meng-UPDATE ulasan yang sama alih-alih membuat baris dobel.</li>
 * <li><b>Moderasi admin</b> -- {@code ais.action.master.kursus.UlasanKursusAction}, dilindungi
 * {@code Common.doCheckSecurity()}/hak privilese CRUD standar; hanya dipakai untuk
 * sembunyikan/hapus (mengubah {@link #getAktif()}), bukan untuk peserta menulis ulasan.</li>
 * </ol>
 * <p>
 * {@link #oleh}/{@link #olehId} adalah field audit shadow interceptor (siapa yang TERAKHIR MENGUBAH
 * baris lewat Hibernate) dan BUKAN penanda kepemilikan ulasan -- jangan ditukar dengan
 * {@link #getPesertaKursus()} yang merupakan identitas pengulas sesungguhnya.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ulasan_kursus")
public class UlasanKursus extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;

	/** Field audit shadow: nama pengubah terakhir. BUKAN penanda kepemilikan ulasan -- lihat javadoc kelas. */
	private String oleh;

	/** Field audit shadow: id pengubah terakhir. BUKAN penanda kepemilikan ulasan -- lihat javadoc kelas. */
	private String olehId;

	/** @return id pengguna yang terakhir mengubah baris ini (audit shadow), diisi otomatis oleh interceptor. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengubah terakhir. Nilai kosong/hanya-spasi diabaikan (dead assignment) agar
	 * baris audit sebelumnya tidak tertimpa nilai kosong saat interceptor dipanggil ulang.
	 *
	 * @param olehId id pengguna pengubah; {@code null} atau string kosong tidak melakukan apa-apa.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengubah terakhir. Nilai kosong/hanya-spasi diabaikan (dead assignment),
	 * simetris dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null} atau string kosong tidak melakukan apa-apa.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang terakhir mengubah baris ini (audit shadow), diisi otomatis oleh interceptor. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} (dan field audit terkait)
	 * lewat {@code AuditTimestampInterceptor} setiap kali baris ini di-UPDATE.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu perubahan terakhir; default saat instance dibuat, ditimpa {@link #onUpdate()} saat UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir untuk ditimpa langsung (jarang dipanggil manual). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas "{@link #getKode() kode} - {@link #getNama() nama}". */
	public String toString() {
		return kode + " - " + nama;
	}

	/** Kode unik entity (kolom bawaan {@code GeneralValueObject}); lihat {@link #getKode()}. Tidak dipakai sebagai kunci verifikasi publik seperti pada {@code SertifikatKursus}. */
	private String kode;

	/** Label tampilan gabungan nama peserta + nama kursus; lihat {@link #getNama()}. */
	private String nama;
	private String keterangan;

	/** Kursus yang direview. */
	private ProdukKursus produkKursus;

	/**
	 * Peserta pemberi ulasan -- identitas pengulas sesungguhnya. Ditetapkan dari sesi login peserta
	 * (bukan dari parameter request) oleh jalur self-service {@code simpan_ulasan}; lihat javadoc kelas.
	 */
	private PesertaKursus pesertaKursus;
	private Integer rating;
	private String komentar;
	private Date tanggal;

	/** Flag tampil/sembunyi untuk moderasi admin; default {@code true} bila belum diisi -- lihat {@link #getAktif()}. */
	private Boolean aktif;

	public UlasanKursus() {
	}

	/** @return id baris, auto-generated (identity) oleh database. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; normalnya tidak diisi manual karena kolom bertanda {@code insertable = false}. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return keterangan bebas, boleh kosong. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return kode unik entity ini (kolom bawaan {@code GeneralValueObject}). Diisi lazy pada
	 *         pemanggilan pertama lewat {@code BarcodeCommon.generateCode()} jika belum ada. Tidak
	 *         ada endpoint publik yang mencari {@link UlasanKursus} lewat kode ini (berbeda dari
	 *         {@code SertifikatKursus.getKode()} yang dipakai servlet verifikasi publik).
	 */
	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	/** @param kode kode unik untuk ditetapkan langsung (jarang dipakai manual). */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return label tampilan "{nama peserta} - {nama kursus}", dihitung ulang setiap pemanggilan dari
	 *         {@link #getProdukKursus()}/{@link #getPesertaKursus()} selama keduanya tidak null;
	 *         jika salah satu null, mengembalikan nilai {@link #nama} yang tersimpan sebelumnya
	 *         (bisa {@code null}) tanpa menghitung ulang.
	 */
	public String getNama() {
		if (getProdukKursus() != null && getPesertaKursus() != null) {
			nama = pesertaKursus.getNama() + " - " + produkKursus.getNama();
		}
		return nama;
	}

	/** @param nama nama tampilan untuk ditimpa langsung (akan dihitung ulang oleh {@link #getNama()} bila relasi tersedia). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return kursus yang direview. Direfresh lewat {@link GeneralValueObject#check(Object)} agar
	 *         konsisten dengan identity map session, sesuai pola entity lain di paket ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_kursus", nullable = false)
	public ProdukKursus getProdukKursus() {
		produkKursus = check(produkKursus);
		return produkKursus;
	}

	/** @param produkKursus kursus yang direview; wajib diisi (kolom {@code nullable = false}). */
	public void setProdukKursus(ProdukKursus produkKursus) {
		this.produkKursus = produkKursus;
	}

	/**
	 * @return peserta pemberi ulasan (identitas pengulas sesungguhnya -- lihat javadoc kelas soal
	 *         bagaimana field ini diisi dari sesi login, bukan parameter request). Direfresh lewat
	 *         {@link GeneralValueObject#check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_kursus", nullable = false)
	public PesertaKursus getPesertaKursus() {
		pesertaKursus = check(pesertaKursus);
		return pesertaKursus;
	}

	/** @param pesertaKursus peserta pemberi ulasan; wajib diisi (kolom {@code nullable = false}). */
	public void setPesertaKursus(PesertaKursus pesertaKursus) {
		this.pesertaKursus = pesertaKursus;
	}

	/** @return rating 1-5; default {@code 5} bila belum diisi. */
	public Integer getRating() {
		return rating == null ? 5 : rating;
	}

	/** @param rating rating 1-5 untuk ditetapkan langsung (validasi rentang dilakukan pemanggil, mis. {@code simpan_ulasan}). */
	public void setRating(Integer rating) {
		this.rating = rating;
	}

	/** @return komentar teks bebas peserta, boleh kosong/null. */
	@Column(name = "komentar", nullable = true, columnDefinition = "text")
	public String getKomentar() {
		return komentar;
	}

	/** @param komentar komentar teks bebas. */
	public void setKomentar(String komentar) {
		this.komentar = komentar;
	}

	/** @return waktu ulasan diberikan/diubah; default "sekarang" (bukan tersimpan) bila field belum pernah diisi. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	/** @param tanggal waktu ulasan diberikan/diubah. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * @return {@code true} bila ulasan ditampilkan ke publik, {@code false} bila disembunyikan
	 *         admin. Default {@code true} bila field belum pernah diisi -- pola nullable-default
	 *         yang NORMAL/tidak terbalik (bandingkan dengan {@code BeritaKursus#getAktif()} yang
	 *         memakai pola identik, keduanya konsisten dengan makna namanya, BUKAN kasus seperti
	 *         {@code InformasiPerpustakaan#getAktif()} yang logikanya terbalik).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif {@code true} untuk tampil publik, {@code false} untuk disembunyikan admin. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
