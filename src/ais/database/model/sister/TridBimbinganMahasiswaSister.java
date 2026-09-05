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
 * <h2>Entitas Tridharma tersinkron SISTER &mdash; Bimbingan Mahasiswa</h2>
 *
 * <p>Baris tabel {@code sister_trid_bimbingan_mahasiswa}, hasil sinkronisasi endpoint SISTER
 * (Kemdikbudristek) <b>{@code bimbingan_mahasiswa}</b> &mdash; riwayat pembimbingan dosen terhadap
 * mahasiswa (mis. bimbingan skripsi/tesis/disertasi), BERBEDA dari {@link TridBimbingDosenSister} yang
 * mencatat pembimbingan antar-dosen, dan dari {@link TridPengujianMahasiswaSister} yang mencatat peran
 * sebagai PENGUJI (bukan pembimbing). Mengikuti KERANGKA STRUKTUR &amp; MEKANISME SINKRONISASI yang identik
 * dengan {@link TridPenelitianSister} (lihat javadoc kelas tersebut untuk penjelasan lengkap): terdaftar
 * di {@link SisterEntitasRegistry} pada kunci {@code "bimbingan_mahasiswa"}, dirutekan &amp; di-upsert secara
 * reflektif oleh {@link ais.common.DataSisterApi} berdasarkan {@link #getKode()}, dengan
 * {@link #getKeterangan()} menyimpan salinan JSON mentah dan {@link #getAktif()} tetap flag satu-arah.
 * Endpoint ini TIDAK BER-PAGINASI.</p>
 *
 * <p>Field bisnis endpoint ini: {@link #getJudul() judul} (judul tugas akhir mahasiswa bimbingan),
 * {@link #getJenisBimbingan() jenisBimbingan} (mis. skripsi/tesis/disertasi/PKL), {@link #getProgramStudi()
 * programStudi}, {@link #getSemester() semester}.</p>
 *
 * @see TridPenelitianSister
 * @see TridBimbingDosenSister
 * @see TridPengujianMahasiswaSister
 * @see ais.database.model.sister.SisterEntitasRegistry
 * @see ais.common.DataSisterApi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_trid_bimbingan_mahasiswa")
public class TridBimbinganMahasiswaSister extends GeneralValueObject {
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
	/** Judul tugas akhir mahasiswa yang dibimbing. */
	private String judul;
	/** Jenis bimbingan (mis. skripsi, tesis, disertasi, PKL). */
	private String jenisBimbingan;
	/** Program studi mahasiswa yang dibimbing. */
	private String programStudi;
	/** Semester pelaksanaan bimbingan (string sesuai konvensi SISTER). */
	private String semester;

	/** Konstruktor bawaan (dibutuhkan Hibernate &amp; refleksi upsert {@code DataSisterApi}); field diisi lewat setter. */
	public TridBimbinganMahasiswaSister() {}

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
	 * @return judul tugas akhir mahasiswa yang dibimbing
	 */
	@Column(name = "judul", columnDefinition = "text") public String getJudul() { return judul; }

	/**
	 * @param v judul tugas akhir mahasiswa yang dibimbing
	 */
	public void setJudul(String v) { this.judul = v; }

	/**
	 * @return jenis bimbingan (mis. skripsi, tesis, disertasi, PKL)
	 */
	@Column(name = "jenis_bimbingan", columnDefinition = "text") public String getJenisBimbingan() { return jenisBimbingan; }

	/**
	 * @param v jenis bimbingan
	 */
	public void setJenisBimbingan(String v) { this.jenisBimbingan = v; }

	/**
	 * @return program studi mahasiswa yang dibimbing
	 */
	@Column(name = "program_studi", columnDefinition = "text") public String getProgramStudi() { return programStudi; }

	/**
	 * @param v program studi mahasiswa yang dibimbing
	 */
	public void setProgramStudi(String v) { this.programStudi = v; }

	/**
	 * @return semester pelaksanaan bimbingan
	 */
	@Column(name = "semester", columnDefinition = "text") public String getSemester() { return semester; }

	/**
	 * @param v semester pelaksanaan bimbingan
	 */
	public void setSemester(String v) { this.semester = v; }

	/**
	 * @return representasi ringkas {@code id-kode}, dipakai untuk log/debug
	 */
	@Override public String toString() { return id + "-" + kode; }
}
