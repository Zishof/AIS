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

/**
 * Entity Hibernate/JPA untuk tabel {@code public.kelompok_matakuliah} — <b>kelompok kurikulum
 * mata kuliah</b> menurut klasifikasi lama Kepmendiknas (MPK/MKK/MKB/MPB/MBB), dengan kode
 * yang bisa diturunkan otomatis dari {@link #getNama()} bila cocok salah satu dari lima nama
 * baku tersebut (lihat {@link #getKode()}), serta kode "feeder" untuk pelaporan eksternal
 * (mis. PDDIKTI/Feeder Dikti, lihat {@link #getFeeder()}).
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_matakuliah")
public class KelompokMatakuliah extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code kelompok_matakuliah}, kolom {@code id} (identity, auto-generate). */
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
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
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
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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

	/**
	 * Representasi ringkas untuk log/debug: nama kelompok mata kuliah.
	 *
	 * @return {@link #nama} apa adanya (tidak melalui {@link #getNama()}, sehingga tidak
	 *         di-{@code trim()})
	 */
	public String toString() {
		return nama;
	}

	/** Kode singkat kelompok (mis. MPK/MKK/MKB/MPB/MBB); diturunkan otomatis dari {@link #nama} bila kosong, lihat {@link #getKode()}. */
	private String kode;

	/**
	 * Kode singkat kelompok mata kuliah.
	 *
	 * <p><b>Getter yang menulis balik (derive-sekali):</b> bila field mentah {@link #kode}
	 * kosong/{@code null} DAN {@link #nama} cocok (case-insensitive) dengan salah satu dari
	 * lima nama baku klasifikasi Kepmendiknas ("Mata Kuliah Pengembangan Kepribadian (MPK)",
	 * dst.), kode diturunkan dan ditulis balik permanen ke field ({@code "MPK"}/{@code "MKK"}/
	 * {@code "MKB"}/{@code "MPB"}/{@code "MBB"}). Bila tidak ada yang cocok (termasuk nama bebas
	 * di luar lima itu) DAN kode masih kosong setelahnya, fallback akhir menyimpan {@code "-"}
	 * secara permanen — sehingga entity dengan nama non-baku yang belum diberi kode manual akan
	 * "terkunci" dengan kode {@code "-"} begitu getter ini dipanggil sekali.</p>
	 *
	 * @return kode kelompok; {@code "-"} bila tidak dapat diturunkan dan belum diisi manual.
	 */
	public String getKode() {
		if (nama != null && (kode == null || kode.trim().isEmpty())) {
			if (nama.trim().equalsIgnoreCase("Mata Kuliah Pengembangan Kepribadian (MPK)")) {
				kode = "MPK";
			} else if (nama.trim().equalsIgnoreCase("Mata Kuliah Keilmuan dan Ketrampilan (MKK)")) {
				kode = "MKK";
			} else if (nama.trim().equalsIgnoreCase("Mata Kuliah Keahlian dan Berkarya (MKB)")) {
				kode = "MKB";
			} else if (nama.trim().equalsIgnoreCase("Mata Kuliah Perilaku dan Berkarya (MPB)")) {
				kode = "MPB";
			} else if (nama.trim().equalsIgnoreCase("Mata Kuliah Berkehidupan Bermasyarakat (MBB)")) {
				kode = "MBB";
			}
		}

		if (kode == null) {
			kode = "-";
		}

		return kode;
	}

	/**
	 * @param kode kode singkat baru; string kosong/{@code null} akan diturunkan ulang otomatis
	 *             (atau di-fallback ke {@code "-"}) saat berikutnya dibaca via {@link
	 *             #getKode()}.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** Nama kelompok mata kuliah (bahasa Indonesia). */
	private String nama;
	/** Nama kelompok dalam bahasa Inggris; fallback ke {@link #getNama()} bila kosong, lihat {@link #getNamaen()}. */
	private String namaen;
	/** Keterangan bebas kelompok ini. */
	private String keterangan;
	/** Nomor urut tampilan kelompok ini, juga dipakai {@link #compareTo(GeneralValueObject)}; default 1 bila kosong. */
	private Integer nomorUrut;
	/** Tahun angkatan mulai berlakunya kelompok ini; default 1900 (efektif "berlaku sejak dulu"). */
	private Integer berlakuMulaiTahunAngkatan = 1900;
	/** Jenjang (mis. S1/S2) cakupan kelompok ini. */
	private Jenjang jenjang;
	/** Flag aktif kelompok ini; default {@code true} bila belum diisi. */
	private Boolean aktif;
	/** Kode "feeder" untuk pelaporan eksternal, diturunkan dari {@link #getKode()}; lihat {@link #getFeeder()}. */
	private String feeder;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public KelompokMatakuliah() {
	}

	/**
	 * @return primary key baris {@code kelompok_matakuliah}; {@code null} sebelum baris
	 *         di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama kelompok mata kuliah, di-{@code trim()}; {@code null} bila field mentah
	 *         {@code null} (meski kolomnya {@code nullable = false} di skema).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama kelompok baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return keterangan bebas kelompok ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk kelompok ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Tahun angkatan mulai berlakunya kelompok ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 1900} pada pembacaan pertama.</p>
	 *
	 * @return tahun angkatan mulai berlaku; {@code 1900} bila belum diisi.
	 */
	public Integer getBerlakuMulaiTahunAngkatan() {
		if (berlakuMulaiTahunAngkatan == null) {
			berlakuMulaiTahunAngkatan = 1900;
		}
		return berlakuMulaiTahunAngkatan;
	}

	/**
	 * @param berlakuMulaiTahunAngkatan tahun angkatan mulai berlaku baru.
	 */
	public void setBerlakuMulaiTahunAngkatan(Integer berlakuMulaiTahunAngkatan) {
		this.berlakuMulaiTahunAngkatan = berlakuMulaiTahunAngkatan;
	}

	/**
	 * Nomor urut tampilan kelompok ini, juga dipakai sebagai basis pengurutan pada {@link
	 * #compareTo(GeneralValueObject)}.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 1} pada pembacaan pertama (pengecekan
	 * {@code null} kedua pada baris {@code return} setelahnya adalah kode mati/tidak pernah
	 * tercapai, karena field sudah dijamin non-null oleh blok {@code if} di atasnya).</p>
	 *
	 * @return nomor urut; {@code 1} bila belum diisi.
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * @param nomorUrut nomor urut baru.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return jenjang (mis. S1/S2) cakupan kelompok ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang")
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * @param jenjang jenjang baru untuk kelompok ini; {@code null} untuk melepas tautan.
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Membandingkan urutan tampil dua {@link GeneralValueObject}.
	 *
	 * <p>Bila {@code arg0} juga {@link KelompokMatakuliah}, perbandingan memakai {@link
	 * #getNomorUrut()} keduanya. Selain itu, dicoba berturutan: NIM, lalu nama, lalu keterangan
	 * (perbandingan pertama yang kedua sisinya tidak {@code null} yang dipakai) — exception
	 * apa pun (mis. properti tidak didukung oleh {@code arg0}) ditelan dan dicatat ke {@link
	 * ais.common.ErrorAuditUtil}, membuat method mengembalikan {@code 0} (dianggap setara).</p>
	 *
	 * @param arg0 object pembanding
	 * @return hasil perbandingan negatif/nol/positif sesuai {@link Comparable}; {@code 0} bila
	 *         tidak ada dasar perbandingan yang valid atau terjadi error
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		if (arg0 instanceof KelompokMatakuliah) {
			KelompokMatakuliah s = (KelompokMatakuliah) arg0;
			return getNomorUrut().compareTo(s.getNomorUrut());
		} else {
			try {
				if (getNim() != null && arg0.getNim() != null) {
					return getNim().compareTo(arg0.getNim());
				} else if (getNama() != null && arg0.getNama() != null) {
					return getNama().compareTo(arg0.getNama());
				} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
					return getKeterangan().compareTo(arg0.getKeterangan());
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KelompokMatakuliah.java:190");

			}

			return 0;
		}
	}

	/**
	 * @return nama kelompok dalam bahasa Inggris, di-{@code trim()}; fallback ke {@link
	 *         #getNama()} bila field mentah {@code null} (TIDAK ditulis balik ke field).
	 */
	public String getNamaen() {
		return namaen == null ? getNama() : namaen.trim();
	}

	/**
	 * @param namaen nama bahasa Inggris baru.
	 */
	public void setNamaen(String namaen) {
		this.namaen = namaen;
	}

	/**
	 * Kode "feeder" satu huruf untuk pelaporan eksternal (mis. PDDIKTI/Feeder Dikti), diturunkan
	 * dari {@link #getKode()}.
	 *
	 * <p><b>Getter yang menulis balik</b> ke field {@link #feeder} berdasar {@link #getKode()}:
	 * MPK&rarr;A, MKK&rarr;B, MKB&rarr;C, MPB&rarr;D, MBB&rarr;E, kode yang MENGANDUNG
	 * {@code "MKU"}&rarr;F, MKDK&rarr;G. Cabang terakhir {@code MKK&rarr;H} TIDAK PERNAH
	 * tercapai — {@code "MKK"} sudah cocok pada cabang kedua ({@code &rarr;B}) sehingga
	 * rantai {@code if/else if} berhenti di situ; ini tampak sebagai artefak salin-tempel
	 * (duplikasi kondisi {@code equalsIgnoreCase("MKK")}), bukan cabang yang sengaja tidak
	 * aktif. Perhatikan pula bahwa {@code "MKU"}/{@code "MKDK"} hanya bisa dicapai bila {@link
	 * #kode} pernah diisi manual lewat {@link #setKode(String)} dengan nilai tersebut — {@link
	 * #getKode()} sendiri tidak pernah menghasilkan kode selain MPK/MKK/MKB/MPB/MBB/{@code "-"}.
	 * Dicatat apa adanya; tidak diperbaiki di sesi dokumentasi ini.</p>
	 *
	 * @return kode feeder satu huruf; {@code null} bila tidak ada yang cocok (termasuk saat
	 *         {@link #getKode()} mengembalikan {@code "-"}).
	 */
	public String getFeeder() {

		if (getKode().equalsIgnoreCase("MPK")) {
			feeder = "A";
		} else if (getKode().equalsIgnoreCase("MKK")) {
			feeder = "B";
		} else if (getKode().equalsIgnoreCase("MKB")) {
			feeder = "C";
		} else if (getKode().equalsIgnoreCase("MPB")) {
			feeder = "D";
		} else if (getKode().equalsIgnoreCase("MBB")) {
			feeder = "E";
		} else if (getKode().contains("MKU")) {
			feeder = "F";
		} else if (getKode().equalsIgnoreCase("MKDK")) {
			feeder = "G";
		} else if (getKode().equalsIgnoreCase("MKK")) {
			feeder = "H";
		}

		return feeder == null || feeder.trim().isEmpty() ? null : feeder;
	}

	/**
	 * @param feeder kode feeder baru untuk field lokal (bisa tetap ditimpa saat dibaca via
	 *               {@link #getFeeder()} — lihat javadoc getter).
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * @return status aktif kelompok ini; default {@code true} bila belum diisi.
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

}
