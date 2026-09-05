package ais.database.model.sister;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/**
 * <h2>Entitas Tridharma tersinkron SISTER &mdash; Jabatan Struktural</h2>
 *
 * <p>Baris tabel {@code sister_trid_jabatan_struktural}, hasil sinkronisasi endpoint SISTER
 * (Kemdikbudristek) <b>{@code jabatan_struktural}</b> &mdash; riwayat jabatan struktural/manajerial
 * (mis. Dekan, Ketua Jurusan, Rektor) yang pernah/sedang diemban seorang dosen. Mengikuti KERANGKA
 * STRUKTUR &amp; MEKANISME SINKRONISASI yang identik dengan {@link TridPenelitianSister} (lihat javadoc
 * kelas tersebut untuk penjelasan lengkap): terdaftar di {@link SisterEntitasRegistry} pada kunci
 * {@code "jabatan_struktural"}, dirutekan &amp; di-upsert secara reflektif oleh {@link ais.common.DataSisterApi}
 * berdasarkan {@link #getKode()}, dengan {@link #getKeterangan()} menyimpan salinan JSON mentah dan
 * {@link #getAktif()} tetap flag satu-arah. Endpoint ini TIDAK BER-PAGINASI.</p>
 *
 * <p>Field bisnis endpoint ini: {@link #getJabatan() jabatan} (nama jabatan struktural),
 * {@link #getSkJabatan() skJabatan} (nomor SK pengangkatan), {@link #getTanggalMulaiJabatan() tanggalMulaiJabatan}
 * dan {@link #getTanggalSelesaiJabatan() tanggalSelesaiJabatan} (rentang masa jabatan).</p>
 *
 * @see TridPenelitianSister
 * @see ais.database.model.sister.SisterEntitasRegistry
 * @see ais.common.DataSisterApi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_trid_jabatan_struktural")
public class TridJabatanStrukturalSister extends GeneralValueObject {
	/** Versi serialisasi untuk kompatibilitas {@link java.io.Serializable}. */
	private static final long serialVersionUID = 1L;
	/** Primary key lokal (surrogate), digenerate basis data (IDENTITY); TIDAK sama dengan {@link #kode} (id sisi SISTER). */
	private Long id;
	/** Nama pengguna terakhir pengubah baris (audit); lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir pengubah baris (audit); lihat {@link #getOlehId()}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan object, diperbarui via {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Id item pada sisi SISTER (kunci upsert); lihat {@link #getKode()}. */
	private String kode;
	/** Salinan JSON mentah satu item dari respons SISTER (cadangan/audit sumber). */
	private String keterangan;
	/** Flag aktif; {@code null} diperlakukan sebagai aktif ({@code true}) oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** Id dosen pemilik pada sisi SISTER (string polos, bukan relasi JPA); lihat {@link #getIdSdm()}. */
	private String idSdm;
	/** Nama jabatan struktural/manajerial (mis. Dekan, Ketua Jurusan, Rektor). */
	private String jabatan;
	/** Nomor Surat Keputusan (SK) pengangkatan jabatan struktural. */
	private String skJabatan;
	/** Tanggal mulai masa jabatan struktural (string sesuai format SISTER, bukan {@code Date}). */
	private String tanggalMulaiJabatan;
	/** Tanggal selesai masa jabatan struktural (string sesuai format SISTER; kosong bila masih menjabat). */
	private String tanggalSelesaiJabatan;

	/** Konstruktor bawaan (dibutuhkan Hibernate &amp; refleksi upsert {@code DataSisterApi}); field diisi lewat setter. */
	public TridJabatanStrukturalSister() {}

	/**
	 * @return primary key lokal (surrogate, IDENTITY), atau {@code null} bila entity belum tersimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }

	/**
	 * Menyetel primary key lokal. Kolom dipetakan {@code insertable = false} &mdash; nilai SELALU berasal
	 * dari basis data (IDENTITY autoincrement); method ini dipakai Hibernate saat memuat entity, bukan
	 * untuk menetapkan id secara manual sebelum simpan.
	 *
	 * @param id primary key lokal
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() { return olehId; }

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong DIABAIKAN DIAM-DIAM (jejak audit
	 * yang sudah terisi tidak bisa terhapus oleh jalur simpan yang kebetulan tak membawa info pengguna) &mdash;
	 * sama seperti kontrak {@link ais.database.model.GeneralValueObject#setOlehId(String)} yang di-shadow
	 * method ini.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }

	/**
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() { return oleh; }

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi sama seperti {@link #setOlehId(String)}:
	 * nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }

	/**
	 * Hook {@code @PreUpdate}: mengisi ulang {@link #tanggal_dirubah}/{@link #oleh}/{@link #olehId} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tiap kali baris di-UPDATE.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

	/**
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi (dipanggil pula oleh {@link #onUpdate()}).
	 *
	 * @param t waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }

	/**
	 * @return id item sisi SISTER (kunci upsert), sudah di-{@code trim()}, atau {@code null} bila kosong/belum diisi
	 */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }

	/**
	 * Menyetel id item sisi SISTER (kunci upsert oleh {@link ais.common.DataSisterApi}). Tanpa validasi;
	 * normalisasi (trim/kosong&rarr;null) dilakukan pada {@link #getKode()}, bukan di sini.
	 *
	 * @param kode id item sisi SISTER
	 */
	public void setKode(String kode) { this.kode = kode; }

	/**
	 * @return salinan JSON mentah item ini (cadangan/audit sumber)
	 */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }

	/**
	 * @param k salinan JSON mentah item ini
	 */
	public void setKeterangan(String k) { this.keterangan = k; }

	/**
	 * @return flag aktif; {@code true} bila belum pernah diisi ({@code null}) &mdash; lihat catatan
	 *         {@link TridPenelitianSister} soal flag ini yang TIDAK pernah disetel oleh mesin sinkronisasi otomatis
	 */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }

	/**
	 * @param a flag aktif baru
	 */
	public void setAktif(Boolean a) { this.aktif = a; }

	/**
	 * @return id dosen pemilik pada sisi SISTER (string polos, bukan relasi JPA)
	 */
	@Column(name = "id_sdm", columnDefinition = "text") public String getIdSdm() { return idSdm; }

	/**
	 * @param v id dosen pemilik pada sisi SISTER
	 */
	public void setIdSdm(String v) { this.idSdm = v; }

	/**
	 * @return nama jabatan struktural/manajerial
	 */
	@Column(name = "jabatan", columnDefinition = "text") public String getJabatan() { return jabatan; }

	/**
	 * @param v nama jabatan struktural/manajerial
	 */
	public void setJabatan(String v) { this.jabatan = v; }

	/**
	 * @return nomor SK pengangkatan jabatan struktural
	 */
	@Column(name = "sk_jabatan", columnDefinition = "text") public String getSkJabatan() { return skJabatan; }

	/**
	 * @param v nomor SK pengangkatan jabatan struktural
	 */
	public void setSkJabatan(String v) { this.skJabatan = v; }

	/**
	 * @return tanggal mulai masa jabatan struktural (string, format sesuai SISTER)
	 */
	@Column(name = "tanggal_mulai_jabatan", columnDefinition = "text") public String getTanggalMulaiJabatan() { return tanggalMulaiJabatan; }

	/**
	 * @param v tanggal mulai masa jabatan struktural
	 */
	public void setTanggalMulaiJabatan(String v) { this.tanggalMulaiJabatan = v; }

	/**
	 * @return tanggal selesai masa jabatan struktural (string, format sesuai SISTER; kosong bila masih menjabat)
	 */
	@Column(name = "tanggal_selesai_jabatan", columnDefinition = "text") public String getTanggalSelesaiJabatan() { return tanggalSelesaiJabatan; }

	/**
	 * @param v tanggal selesai masa jabatan struktural
	 */
	public void setTanggalSelesaiJabatan(String v) { this.tanggalSelesaiJabatan = v; }

	/**
	 * @return representasi ringkas {@code id-kode}, dipakai untuk log/debug
	 */
	@Override public String toString() { return id + "-" + kode; }
}
