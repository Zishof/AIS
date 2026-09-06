package ais.database.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.paket} — <b>paket pendaftaran PMB</b>
 * (Penerimaan Mahasiswa Baru): master yang menjadi INDUK bagi sejumlah entity anak yang
 * masing-masing menunjuk balik ke sini lewat FK {@code paket}: {@link PaketPunyaProgram}, {@link
 * PaketPunyaMatapelajaran}, {@link PaketPunyaGelombangPendaftaran}, {@link PaketJurusanPmb},
 * {@link PaketPunyaParameterVerifikasiCalonMahasiswa}, dan {@link PaketRegistrasiMahasiswa} —
 * serta dipakai langsung oleh {@link RuangPMB} (ruang ujian PMB per paket).
 *
 * <p>Berisi aturan umum satu paket: jumlah program studi yang boleh diambil calon mahasiswa
 * ({@link #getJumlahProdiYgBolehDiambil()}), apakah paket ini bisa dipilih di semua gelombang
 * pendaftaran atau harus eksplisit ({@link #getBisaDipilihSemuaGelombang()}), apakah pilihan
 * program studi yang sama diperbolehkan ({@link #getBisaMemilihPilihanYangSama()}), keseragaman
 * biaya pendaftaran antar gelombang ({@link #getBiayaPendaftaranSemuaGelombangSama()}),
 * kewajiban unggah foto, dan format kelas untuk verifikasi rapor ({@link
 * #getKelasVerifikasiRapor()}).</p>
 *
 * <p><b>Bukan entity "Paket" tunggal di codebase:</b> {@code PaketPsb} (modul sekolah), {@code
 * PaketPerkuliahan}, dan {@code sirs.PaketPerawatanDetail}/{@code PaketPerawatanDetailPasien}
 * adalah konsep "paket" yang SAMA SEKALI TERPISAH pada domain masing-masing (bukan turunan
 * atau relasi apa pun dari kelas ini) — kesamaan nama semata, bukan kesamaan hierarki.</p>
 *
 * @see PaketPunyaProgram
 * @see PaketPunyaMatapelajaran
 * @see PaketPunyaGelombangPendaftaran
 * @see PaketJurusanPmb
 * @see PaketPunyaParameterVerifikasiCalonMahasiswa
 * @see PaketRegistrasiMahasiswa
 * @see RuangPMB
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "paket")
public class Paket extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = -227313087242633498L;

	/** Primary key baris {@code paket}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu "terakhir diubah"; diinisialisasi ke waktu sekarang saat instance dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode singkat paket ini; default {@code "--"} bila belum diisi, lihat {@link #getKode()}. */
	private String kode;
	/** Nama paket pendaftaran ini. */
	private String nama;
	/** Keterangan bebas paket ini. */
	private String keterangan;
	/** Jumlah program studi yang boleh diambil calon mahasiswa pada paket ini; default 1. */
	private Integer jumlahProdiYgBolehDiambil = 1;
	/** Flag: paket ini bisa dipilih di semua gelombang pendaftaran; default {@code true} bila belum diisi. */
	private Boolean bisaDipilihSemuaGelombang;
	/** Flag: calon mahasiswa boleh memilih program studi yang sama lebih dari sekali; default {@code true} bila belum diisi. */
	private Boolean bisaMemilihPilihanYangSama;
	/** Flag: biaya pendaftaran sama untuk semua gelombang; default {@code false} bila belum diisi. */
	private Boolean biayaPendaftaranSemuaGelombangSama;
	/** Flag: paket ini mewajibkan unggah foto; default {@code true} bila belum diisi. */
	private Boolean wajibUploadFoto;
	/** Format encoded kelas/semester yang diverifikasi untuk nilai rapor (format {@code "kelas:semester;..."}); lihat {@link #getKelasVerifikasiRapor()}. */
	private String kelasVerifikasiRapor;

	/** Flag aktif paket ini; default {@code true} bila belum diisi. */
	private Boolean aktif;
	/** Perguruan tinggi pemilik paket ini; fallback ke PT tunggal sistem bila kosong, lihat {@link #getPerguruanTinggi()}. */
	private PerguruanTinggi perguruanTinggi;

	/**
	 * @return primary key baris {@code paket}; {@code null} sebelum baris di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, nullable = false, unique = true)
	public Long getId() {
		return id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama paket pendaftaran ini apa adanya (tidak di-{@code trim()}, berbeda dari
	 *         kebanyakan getter nama sejenis di cluster ini); boleh {@code null}.
	 */
	@Column(name = "nama")
	public String getNama() {
		return nama;
	}

	/**
	 * @param nama nama paket baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Representasi ringkas untuk log/debug: {@code "<id>-<nama>"}.
	 *
	 * @return string ringkas identitas paket ini
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * @param keterangan keterangan baru untuk paket ini.
	 */
	@Column(name = "keterangan")
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return keterangan bebas paket ini; boleh {@code null}.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Kode singkat paket ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code "--"} pada pembacaan pertama.</p>
	 *
	 * @return kode paket; {@code "--"} bila belum diisi.
	 */
	public String getKode() {
		if (kode == null) {
			kode = "--";
		}
		return kode;
	}

	/**
	 * @param kode kode baru untuk paket ini.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Jumlah program studi yang boleh diambil calon mahasiswa pada paket ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 1} pada pembacaan pertama.</p>
	 *
	 * @return jumlah prodi yang boleh diambil; {@code 1} bila belum diisi.
	 */
	public Integer getJumlahProdiYgBolehDiambil() {
		if (jumlahProdiYgBolehDiambil == null) {
			jumlahProdiYgBolehDiambil = 1;
		}
		return jumlahProdiYgBolehDiambil;
	}

	/**
	 * @param jumlahProdiYgBolehDiambil jumlah prodi baru yang boleh diambil.
	 */
	public void setJumlahProdiYgBolehDiambil(Integer jumlahProdiYgBolehDiambil) {
		this.jumlahProdiYgBolehDiambil = jumlahProdiYgBolehDiambil;
	}

	/**
	 * Flag "bisa dipilih semua gelombang pendaftaran".
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code true} pada pembacaan pertama.</p>
	 *
	 * @return status flag; {@code true} bila belum diisi.
	 */
	public Boolean getBisaDipilihSemuaGelombang() {
		if (bisaDipilihSemuaGelombang == null) {
			bisaDipilihSemuaGelombang = true;
		}
		return bisaDipilihSemuaGelombang;
	}

	/**
	 * @param bisaDipilihSemuaGelombang nilai flag baru.
	 */
	public void setBisaDipilihSemuaGelombang(Boolean bisaDipilihSemuaGelombang) {
		this.bisaDipilihSemuaGelombang = bisaDipilihSemuaGelombang;
	}

	/**
	 * Flag "boleh memilih program studi yang sama lebih dari sekali".
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code true} pada pembacaan pertama.</p>
	 *
	 * @return status flag; {@code true} bila belum diisi.
	 */
	public Boolean getBisaMemilihPilihanYangSama() {
		if (bisaMemilihPilihanYangSama == null) {
			bisaMemilihPilihanYangSama = true;
		}
		return bisaMemilihPilihanYangSama;
	}

	/**
	 * @param bisaMemilihPilihanYangSama nilai flag baru.
	 */
	public void setBisaMemilihPilihanYangSama(Boolean bisaMemilihPilihanYangSama) {
		this.bisaMemilihPilihanYangSama = bisaMemilihPilihanYangSama;
	}

	/**
	 * Format encoded kelas/semester yang diverifikasi untuk nilai rapor (format {@code
	 * "kelas:semester;kelas:semester;..."}, mis. {@code "10:1;10:2;11:1"}).
	 *
	 * <p><b>Default kondisional, bukan hanya null-check:</b> nilai default {@code
	 * "10:1;10:2;11:1;11:2;12:1;12:2"} (kelas 10-12 semester 1&ndash;2) dikembalikan BUKAN
	 * hanya saat field mentah {@code null}, TETAPI JUGA saat field TERISI namun tidak kosong
	 * dan TIDAK mengandung karakter {@code ':'} — yakni data lama/tidak terformat (bukan
	 * encoded {@code "kelas:semester"}) ikut dianggap "belum valid" dan diganti default,
	 * BUKAN dikembalikan apa adanya maupun ditulis balik ke field.</p>
	 *
	 * @return format encoded kelas verifikasi rapor efektif; default bila kosong atau tidak
	 *         mengandung {@code ':'}, atau nilai field di-{@code trim()} bila valid.
	 */
	public String getKelasVerifikasiRapor() {
		return kelasVerifikasiRapor == null
				|| (!kelasVerifikasiRapor.trim().isEmpty() && !StringUtils.contains(kelasVerifikasiRapor, ":"))
						? "10:1;10:2;11:1;11:2;12:1;12:2"
						: kelasVerifikasiRapor.trim();
	}

	/**
	 * @param kelasVerifikasiRapor format encoded baru (lihat {@link #getKelasVerifikasiRapor()}).
	 */
	public void setKelasVerifikasiRapor(String kelasVerifikasiRapor) {
		this.kelasVerifikasiRapor = kelasVerifikasiRapor;
	}

	/**
	 * @return status aktif paket ini; default {@code true} bila belum diisi.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return {@code true} bila biaya pendaftaran sama untuk semua gelombang; default {@code
	 *         false} bila belum diisi.
	 */
	public Boolean getBiayaPendaftaranSemuaGelombangSama() {
		return biayaPendaftaranSemuaGelombangSama == null ? false : biayaPendaftaranSemuaGelombangSama;
	}

	/**
	 * @param biayaPendaftaranSemuaGelombangSama nilai flag baru.
	 */
	public void setBiayaPendaftaranSemuaGelombangSama(Boolean biayaPendaftaranSemuaGelombangSama) {
		this.biayaPendaftaranSemuaGelombangSama = biayaPendaftaranSemuaGelombangSama;
	}

	/**
	 * Perguruan tinggi pemilik paket ini.
	 *
	 * <p><b>Getter dengan fallback fail-open ke PT tunggal sistem:</b> bila field mentah
	 * {@code null} (setelah diresolusi via {@code check()}), method mencoba mengambil
	 * perguruan tinggi tunggal sistem lewat {@code
	 * PerguruanTinggiUtil.getPerguruanTinggi()} — exception apa pun dari pemanggilan itu
	 * ditelan dan dicatat ke {@link ais.common.ErrorAuditUtil}, membuat method jatuh ke
	 * {@code null} pada baris {@code return} berikutnya (bukan melempar exception ke
	 * pemanggil). Hasil akhir juga diperiksa: perguruan tinggi tanpa ID (belum tersimpan)
	 * dianggap sama dengan {@code null}.</p>
	 *
	 * @return perguruan tinggi efektif (dari field, atau fallback PT tunggal sistem); {@code
	 *         null} bila keduanya tidak tersedia atau perguruan tinggi hasil fallback belum
	 *         ber-ID.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Paket.java:199");
		}
		return perguruanTinggi == null || perguruanTinggi.getId() == null ? null : perguruanTinggi;
	}

	/**
	 * @param perguruanTinggi perguruan tinggi baru; {@code null} untuk memakai fallback PT
	 *                        tunggal sistem saat dibaca via {@link #getPerguruanTinggi()}.
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * @return {@code true} bila paket ini mewajibkan unggah foto; default {@code true} bila
	 *         belum diisi.
	 */
	public Boolean getWajibUploadFoto() {
		return wajibUploadFoto == null ? true : wajibUploadFoto;
	}

	/**
	 * @param wajibUploadFoto nilai flag baru.
	 */
	public void setWajibUploadFoto(Boolean wajibUploadFoto) {
		this.wajibUploadFoto = wajibUploadFoto;
	}

}
