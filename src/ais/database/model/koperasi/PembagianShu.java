package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — fitur pembagian SHU.

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

/**
 * <h2>PembagianShu — Kepala Pembagian Sisa Hasil Usaha (SHU) per Tahun Buku</h2>
 *
 * <p>
 * Entity ini menyimpan <b>keputusan dan total pembagian SHU</b> untuk satu tahun buku koperasi.
 * Berbeda dengan entity simpan pinjam lain yang merupakan konsolidasi dari engine existing, fitur
 * pembagian SHU memang <b>belum ada</b> di sistem (yang tersedia hanya penanda
 * {@code ProdukKoperasi.hitungShu}), sehingga tabel <code>koperasi.pembagian_shu</code> ini dibuat
 * sebagai kebutuhan baru yang sah — bukan duplikasi.
 * </p>
 *
 * <p>
 * Nilai {@link #getTotalShu()} bersumber dari <b>keputusan Rapat Anggota Tahunan (RAT)</b> dan
 * dimasukkan oleh admin/pengurus — bukan dihitung otomatis oleh sistem — karena SHU adalah hasil
 * akhir laporan keuangan yang disahkan RAT. Persentase alokasi ke tiap pos juga ditetapkan RAT dan
 * disimpan di sini. Rincian bagian tiap anggota berada pada entity anak {@link ShuAnggota}, yang
 * dihitung proporsional oleh sistem: <em>jasa modal</em> sebanding simpanan anggota dan <em>jasa
 * usaha</em> sebanding partisipasi (jasa/bunga) anggota.
 * </p>
 *
 * <h3>Pos alokasi sesuai UU Perkoperasian &amp; SOM USPK</h3>
 * <ul>
 * <li>{@link #getPersenCadangan()} — dana cadangan (penguatan modal).</li>
 * <li>{@link #getPersenJasaModal()} — balas jasa modal (sebanding simpanan).</li>
 * <li>{@link #getPersenJasaUsaha()} — balas jasa usaha (sebanding partisipasi).</li>
 * <li>{@link #getPersenPendidikan()} — dana pendidikan &amp; pelatihan.</li>
 * <li>{@link #getPersenPengurus()} — insentif pengurus/pengawas/pengelola.</li>
 * <li>{@link #getPersenSosial()} — dana sosial.</li>
 * <li>{@link #getPersenLain()} — pos lain-lain.</li>
 * </ul>
 * Total seluruh persentase idealnya 100% dan divalidasi di lapisan Action.
 *
 * <h3>Status</h3>
 * <ul>
 * <li>{@link #STATUS_DRAFT} — kebijakan disusun, belum disahkan RAT.</li>
 * <li>{@link #STATUS_DISAHKAN} — disahkan RAT, siap dibagikan.</li>
 * <li>{@link #STATUS_DIBAGIKAN} — bagian anggota telah dibukukan/dibayarkan.</li>
 * </ul>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Mengikuti pola rumah AIS: {@code IDENTITY}, relasi lazy dengan {@code check(...)}, hook audit
 * {@code @PreUpdate}, {@code @Audited} (SHU adalah keputusan penting yang harus dapat ditelusuri),
 * getter numerik null-safe, kompatibel Java 1.7. Terdaftar di {@code hibernate.cfg.xml} sehingga
 * {@code hbm2ddl=update} membuat tabel schema {@code koperasi} otomatis. Kombinasi koperasi+tahun
 * sebaiknya unik (dijaga di Action).
 * </p>
 *
 * @see ShuAnggota
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pembagian_shu")
public class PembagianShu extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 7742100014412002001L;

	/** Nilai {@link #status}: kebijakan pembagian SHU sedang disusun, belum disahkan RAT. */
	public static final String STATUS_DRAFT = "DRAFT";
	/** Nilai {@link #status}: kebijakan pembagian telah disahkan Rapat Anggota Tahunan, siap dibagikan. */
	public static final String STATUS_DISAHKAN = "DISAHKAN";
	/**
	 * Nilai {@link #status}: bagian SHU seluruh anggota telah dihitung dan dibukukan ke
	 * {@link ShuAnggota}. <b>Catatan penting:</b> diset otomatis oleh
	 * {@code PembagianShuHelper.hitungDanSimpan} begitu perhitungan selesai — bukan hanya begitu
	 * benar-benar dicairkan ke rekening/saldo anggota. Lihat catatan arsitektur lebih lengkap pada
	 * {@link ShuAnggota#getJasaModal()} mengenai belum adanya mekanisme pencairan riil yang tertaut
	 * ke status ini.
	 */
	public static final String STATUS_DIBAGIKAN = "DIBAGIKAN";

	/** Primary key, IDENTITY dari kolom {@code id}. */
	private Long id;
	/** Nama pengguna (username) pembuat/pengubah terakhir baris ini, untuk audit ringan. */
	private String oleh;
	/** Id pengguna pembuat/pengubah terakhir baris ini, pasangan {@link #oleh}. */
	private String olehId;

	/** Koperasi pemilik kepala pembagian SHU tahun buku ini. */
	private Koperasi koperasi;
	/** Tahun buku yang dibagikan SHU-nya. */
	private Integer tahun = 0;
	/** Total SHU tahun buku ini (rupiah), hasil keputusan RAT — dimasukkan manual, bukan dihitung otomatis oleh sistem. */
	private Double totalShu = 0.0;
	/** Persentase alokasi ke dana cadangan (penguatan modal), sesuai keputusan RAT. */
	private Double persenCadangan = 0.0;
	/** Persentase alokasi ke balas jasa modal (sebanding simpanan anggota), sesuai keputusan RAT. */
	private Double persenJasaModal = 0.0;
	/** Persentase alokasi ke balas jasa usaha (sebanding partisipasi anggota), sesuai keputusan RAT. */
	private Double persenJasaUsaha = 0.0;
	/** Persentase alokasi ke dana pendidikan &amp; pelatihan, sesuai keputusan RAT. */
	private Double persenPendidikan = 0.0;
	/** Persentase alokasi ke insentif pengurus/pengawas/pengelola, sesuai keputusan RAT. */
	private Double persenPengurus = 0.0;
	/** Persentase alokasi ke dana sosial, sesuai keputusan RAT. */
	private Double persenSosial = 0.0;
	/** Persentase alokasi ke pos lain-lain, sesuai keputusan RAT. */
	private Double persenLain = 0.0;
	/** Tanggal Rapat Anggota Tahunan yang mengesahkan pembagian SHU ini; dasar tanggal jurnal pembagian. */
	private Date tanggalRat;
	/** {@link #STATUS_DRAFT}, {@link #STATUS_DISAHKAN}, atau {@link #STATUS_DIBAGIKAN}; default {@link #STATUS_DRAFT}. */
	private String status = STATUS_DRAFT;
	/** Catatan bebas mengenai kepala pembagian SHU tahun buku ini. */
	private String keterangan;

	/** Konstruktor kosong, dipakai Hibernate dan saat membangun kepala pembagian SHU baru sebelum diisi. */
	public PembagianShu() {
	}

	/**
	 * Konstruktor pintasan untuk merujuk sebuah kepala pembagian SHU yang sudah ada hanya lewat id-nya.
	 *
	 * @param id primary key kepala pembagian SHU yang sudah ada
	 */
	public PembagianShu(Long id) {
		this.id = id;
	}

	/** @return primary key kepala pembagian SHU ini, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; kolom {@code insertable = false} sehingga id sesungguhnya berasal dari IDENTITY database. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return id pengguna pembuat/pengubah terakhir baris ini, atau {@code null} bila belum pernah di-set. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Set id pengguna pembuat/pengubah. Nilai kosong/hanya-spasi diabaikan (tidak menimpa nilai
	 * lama) agar audit tidak pernah kehilangan jejak pengguna karena panggilan kosong yang tidak
	 * disengaja.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Set nama pengguna pembuat/pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * kosong/hanya-spasi diabaikan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna pembuat/pengubah terakhir baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Hibernate yang dipanggil otomatis sebelum setiap {@code UPDATE}. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} (dan field audit sejenis) tanpa perlu campur tangan kode pemanggil.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu terakhir baris ini diubah; default saat objek dibuat, diperbarui oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang hendak diset secara manual. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu terakhir baris ini diubah. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return koperasi pemilik kepala pembagian SHU ini, dimuat lazy lewat {@code check(...)}.
	 *         Catatan: relasi ini {@code nullable = true}; penyaringan menurut koperasi berjalan
	 *         (bila ada) dilakukan di lapisan Action/query pemanggil, bukan dipaksa di sini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		koperasi = check(koperasi);
		return koperasi;
	}

	/**
	 * Set koperasi pemilik. Ditolak diam-diam (field tetap {@code null}) bila {@code koperasi}
	 * bernilai {@code null} atau belum memiliki id (belum tersimpan) — mencegah kepala pembagian
	 * SHU tertaut ke koperasi yang belum valid di database.
	 *
	 * @param koperasi koperasi pemilik; diabaikan (diset {@code null}) bila belum memiliki id
	 */
	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi == null || koperasi.getId() == null ? null : koperasi;
	}

	/** @return tahun buku yang dibagikan SHU-nya; tidak pernah {@code null} ({@code 0} sebagai fallback). */
	@Column(name = "tahun")
	public Integer getTahun() {
		return tahun == null ? 0 : tahun;
	}

	/** @param tahun tahun buku yang dibagikan SHU-nya. Kombinasi koperasi+tahun sebaiknya unik (dijaga di Action). */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * @return total SHU tahun buku ini (rupiah); tidak pernah {@code null} ({@code 0.0} sebagai
	 *         fallback). Nilai ini adalah <b>input manual</b> hasil keputusan RAT — dimasukkan
	 *         admin/pengurus lewat {@code PembagianShuHelper.Parameter#totalShu}, bukan dihitung
	 *         otomatis dari laba operasional atau dari {@link ModalPenyertaanKoperasi} mana pun;
	 *         entity ini tidak memiliki relasi ke laporan laba-rugi. Nilai inilah basis
	 *         {@link #getNominalJasaModal()} dan {@link #getNominalJasaUsaha()}.
	 */
	@Column(name = "total_shu")
	public Double getTotalShu() {
		return totalShu == null ? 0.0 : totalShu;
	}

	/** @param totalShu total SHU tahun buku ini (rupiah), hasil keputusan RAT. */
	public void setTotalShu(Double totalShu) {
		this.totalShu = totalShu;
	}

	/** @return persentase alokasi ke dana cadangan; tidak pernah {@code null} ({@code 0.0} sebagai fallback). */
	@Column(name = "persen_cadangan")
	public Double getPersenCadangan() {
		return persenCadangan == null ? 0.0 : persenCadangan;
	}

	/** @param persenCadangan persentase alokasi ke dana cadangan, sesuai keputusan RAT. */
	public void setPersenCadangan(Double persenCadangan) {
		this.persenCadangan = persenCadangan;
	}

	/**
	 * @return persentase alokasi ke balas jasa modal; tidak pernah {@code null} ({@code 0.0}
	 *         sebagai fallback). Dasar {@link #getNominalJasaModal()}, yang selanjutnya menjadi
	 *         basis rumus {@code jasaModal} per anggota di {@link ShuAnggota#getJasaModal()}.
	 */
	@Column(name = "persen_jasa_modal")
	public Double getPersenJasaModal() {
		return persenJasaModal == null ? 0.0 : persenJasaModal;
	}

	/** @param persenJasaModal persentase alokasi ke balas jasa modal, sesuai keputusan RAT. */
	public void setPersenJasaModal(Double persenJasaModal) {
		this.persenJasaModal = persenJasaModal;
	}

	/**
	 * @return persentase alokasi ke balas jasa usaha; tidak pernah {@code null} ({@code 0.0}
	 *         sebagai fallback). Dasar {@link #getNominalJasaUsaha()}, yang selanjutnya menjadi
	 *         basis rumus {@code jasaUsaha} per anggota di {@link ShuAnggota#getJasaUsaha()}.
	 */
	@Column(name = "persen_jasa_usaha")
	public Double getPersenJasaUsaha() {
		return persenJasaUsaha == null ? 0.0 : persenJasaUsaha;
	}

	/** @param persenJasaUsaha persentase alokasi ke balas jasa usaha, sesuai keputusan RAT. */
	public void setPersenJasaUsaha(Double persenJasaUsaha) {
		this.persenJasaUsaha = persenJasaUsaha;
	}

	/** @return persentase alokasi ke dana pendidikan &amp; pelatihan; tidak pernah {@code null} ({@code 0.0} sebagai fallback). */
	@Column(name = "persen_pendidikan")
	public Double getPersenPendidikan() {
		return persenPendidikan == null ? 0.0 : persenPendidikan;
	}

	/** @param persenPendidikan persentase alokasi ke dana pendidikan &amp; pelatihan, sesuai keputusan RAT. */
	public void setPersenPendidikan(Double persenPendidikan) {
		this.persenPendidikan = persenPendidikan;
	}

	/** @return persentase alokasi ke insentif pengurus/pengawas/pengelola; tidak pernah {@code null} ({@code 0.0} sebagai fallback). */
	@Column(name = "persen_pengurus")
	public Double getPersenPengurus() {
		return persenPengurus == null ? 0.0 : persenPengurus;
	}

	/** @param persenPengurus persentase alokasi ke insentif pengurus/pengawas/pengelola, sesuai keputusan RAT. */
	public void setPersenPengurus(Double persenPengurus) {
		this.persenPengurus = persenPengurus;
	}

	/** @return persentase alokasi ke dana sosial; tidak pernah {@code null} ({@code 0.0} sebagai fallback). */
	@Column(name = "persen_sosial")
	public Double getPersenSosial() {
		return persenSosial == null ? 0.0 : persenSosial;
	}

	/** @param persenSosial persentase alokasi ke dana sosial, sesuai keputusan RAT. */
	public void setPersenSosial(Double persenSosial) {
		this.persenSosial = persenSosial;
	}

	/** @return persentase alokasi ke pos lain-lain; tidak pernah {@code null} ({@code 0.0} sebagai fallback). */
	@Column(name = "persen_lain")
	public Double getPersenLain() {
		return persenLain == null ? 0.0 : persenLain;
	}

	/** @param persenLain persentase alokasi ke pos lain-lain, sesuai keputusan RAT. */
	public void setPersenLain(Double persenLain) {
		this.persenLain = persenLain;
	}

	/** @return tanggal Rapat Anggota Tahunan yang mengesahkan pembagian SHU ini, atau {@code null} bila belum diisi. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_rat")
	public Date getTanggalRat() {
		return tanggalRat;
	}

	/** @param tanggalRat tanggal Rapat Anggota Tahunan yang mengesahkan pembagian SHU ini; dasar tanggal jurnal pembagian. */
	public void setTanggalRat(Date tanggalRat) {
		this.tanggalRat = tanggalRat;
	}

	/**
	 * @return status kepala pembagian SHU: {@link #STATUS_DRAFT} (default bila kosong),
	 *         {@link #STATUS_DISAHKAN}, atau {@link #STATUS_DIBAGIKAN}. Tidak pernah
	 *         {@code null}/kosong. Lihat catatan penting pada {@link #STATUS_DIBAGIKAN} mengenai
	 *         makna sesungguhnya nilai tersebut.
	 */
	@Column(name = "status", length = 20)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_DRAFT : status;
	}

	/** @param status {@link #STATUS_DRAFT}, {@link #STATUS_DISAHKAN}, atau {@link #STATUS_DIBAGIKAN}. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return catatan bebas mengenai kepala pembagian SHU tahun buku ini, atau {@code null} bila belum diisi. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan bebas mengenai kepala pembagian SHU tahun buku ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * <h3>Total seluruh persentase alokasi</h3>
	 *
	 * <p>
	 * Menjumlahkan ketujuh pos alokasi ({@link #getPersenCadangan()}, {@link #getPersenJasaModal()},
	 * {@link #getPersenJasaUsaha()}, {@link #getPersenPendidikan()}, {@link #getPersenPengurus()},
	 * {@link #getPersenSosial()}, {@link #getPersenLain()}). Idealnya bernilai tepat {@code 100},
	 * sesuai prinsip bahwa seluruh SHU harus habis dialokasikan ke pos-pos yang disahkan RAT — tidak
	 * boleh ada sisa yang tidak jelas peruntukannya maupun alokasi yang melebihi 100% (berarti
	 * membagikan lebih dari SHU yang tersedia).
	 * </p>
	 *
	 * <p>
	 * Method ini murni turunan ({@code @Transient}, tidak dipersist) dan <b>tidak menegakkan</b>
	 * aturan 100% itu sendiri — ia hanya menghitung dan mengembalikan jumlahnya apa adanya, termasuk
	 * bila hasilnya kurang atau lebih dari 100. Penegakan aturan (validasi keseimbangan) dilakukan di
	 * dua tempat lain yang saling independen: lapisan Action pembagian SHU ({@code PembagianShuAction})
	 * saat kebijakan disusun/disahkan, dan mesin posting jurnal ({@code PostingDanaAnggotaUtil}) yang
	 * — sesuai riwayat commit r78651 — secara eksplisit melewati (skip) dokumen dengan total persentase
	 * di luar toleransi 0,01 dari 100, alih-alih mengarang kebijakan pembagian yang tidak pernah
	 * diputuskan RAT. Artinya entity ini sendiri dapat saja tersimpan di database dengan
	 * {@code getTotalPersen() != 100} (mis. draft yang belum lengkap diisi) tanpa exception — barulah
	 * saat hendak diposting jurnalnya, ketidaksesuaian itu dicegat.
	 * </p>
	 *
	 * @return jumlah seluruh persentase alokasi; idealnya {@code 100.0}, tetapi dapat menyimpang bila
	 *         data belum lengkap/valid — pemanggil yang memerlukan validasi ketat harus memeriksa
	 *         sendiri toleransinya, bukan mengandalkan method ini melempar exception
	 */
	@javax.persistence.Transient
	public double getTotalPersen() {
		return getPersenCadangan() + getPersenJasaModal() + getPersenJasaUsaha() + getPersenPendidikan()
				+ getPersenPengurus() + getPersenSosial() + getPersenLain();
	}

	/**
	 * <h3>Nominal SHU untuk pos jasa modal (rupiah)</h3>
	 *
	 * <p>
	 * Rumus: <code>nominalJasaModal = totalShu &times; persenJasaModal / 100</code>. Ini adalah
	 * <b>total</b> yang dialokasikan untuk seluruh anggota pada pos jasa modal — bukan bagian satu
	 * anggota. Nilai ini kemudian menjadi basis pembagian proporsional per anggota di
	 * {@link ShuAnggota#getJasaModal()}, dengan rumus <code>jasaModal(anggota) = simpanan(anggota) /
	 * totalSimpananSeluruhAnggota &times; nominalJasaModal</code> yang dihitung dan disimpan oleh
	 * {@code PembagianShuHelper.hitungDanSimpan}. Method ini sendiri murni turunan
	 * ({@code @Transient}, tidak dipersist), selalu dihitung ulang dari {@link #totalShu} dan
	 * {@link #persenJasaModal} yang berlaku saat dipanggil — sehingga bila {@link #totalShu} atau
	 * {@link #persenJasaModal} diubah setelah SHU per anggota sudah dihitung dan disimpan
	 * ({@link #STATUS_DIBAGIKAN}), angka yang dikembalikan method ini akan berubah mengikuti nilai
	 * baru, sementara baris {@link ShuAnggota} yang sudah tersimpan TIDAK ikut berubah otomatis
	 * (baru sinkron kembali setelah {@code hitungDanSimpan} dijalankan ulang, yang menghapus dan
	 * membuat ulang seluruh rincian). Pemanggil yang menampilkan nilai ini berdampingan dengan data
	 * {@link ShuAnggota} lama perlu waspada terhadap potensi selisih sesaat ini.
	 * </p>
	 *
	 * @return total nominal SHU untuk pos jasa modal (rupiah); {@code 0.0} bila {@link #totalShu}
	 *         atau {@link #persenJasaModal} belum diisi
	 */
	@javax.persistence.Transient
	public double getNominalJasaModal() {
		return getTotalShu() * getPersenJasaModal() / 100.0;
	}

	/**
	 * <h3>Nominal SHU untuk pos jasa usaha (rupiah)</h3>
	 *
	 * <p>
	 * Rumus: <code>nominalJasaUsaha = totalShu &times; persenJasaUsaha / 100</code>. Pasangan
	 * {@link #getNominalJasaModal()} untuk pos jasa usaha: total yang dialokasikan untuk seluruh
	 * anggota, menjadi basis pembagian proporsional per anggota di
	 * {@link ShuAnggota#getJasaUsaha()} dengan rumus <code>jasaUsaha(anggota) =
	 * partisipasi(anggota) / totalPartisipasiSeluruhAnggota &times; nominalJasaUsaha</code>. Sama
	 * seperti {@link #getNominalJasaModal()}, method ini murni turunan ({@code @Transient}) yang
	 * selalu mengikuti nilai {@link #totalShu} dan {@link #persenJasaUsaha} terkini — catatan
	 * mengenai potensi selisih sesaat terhadap baris {@link ShuAnggota} yang sudah tersimpan berlaku
	 * sama persis di sini.
	 * </p>
	 *
	 * @return total nominal SHU untuk pos jasa usaha (rupiah); {@code 0.0} bila {@link #totalShu}
	 *         atau {@link #persenJasaUsaha} belum diisi
	 */
	@javax.persistence.Transient
	public double getNominalJasaUsaha() {
		return getTotalShu() * getPersenJasaUsaha() / 100.0;
	}

	/** @return representasi ringkas "SHU tahun - total" untuk debug/log/tampilan sederhana. */
	@Override
	public String toString() {
		return "SHU " + getTahun() + " - " + getTotalShu();
	}

	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Riwayat posting jurnal pembagian SHU (dok 61 butir B): terisi begitu mesin
	 * {@code PostingDanaAnggotaUtil} memecah SHU ke pos-pos pembagiannya.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
		return postingHistory;
	}

	/** @param postingHistory cap posting jurnal pembagian SHU; diisi mesin posting begitu jurnal dibuat, {@code null} bila belum diposting. */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
