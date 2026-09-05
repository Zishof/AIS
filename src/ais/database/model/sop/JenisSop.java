package ais.database.model.sop;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.GrupChecklistPenilaianUmum;

/**
 * Entity master kategori/jenis SOP (mis. "Administrasi, logistik, SDM, dan keuangan") yang
 * menjadi FK wajib dari {@link Sop} lewat {@link Sop#getJenisSop()}. Satu {@code JenisSop} dapat
 * dipakai oleh banyak {@link Sop} berbeda; ia menyediakan pengelompokan/kategori serta atribut
 * tampilan (warna) dan wewenang tingkat-kategori yang secara default DIWARISKAN oleh seluruh
 * {@code Sop} anggotanya (lihat {@link Sop#getDiperuntukkan()}/{@link Sop#getJenisPengguna()}/
 * {@link Sop#getUsernamePengguna()} yang membaca ulang nilai dari sini).
 *
 * <p><b>Delegasi opsional ke {@link AktorSop}.</b> {@link #aktorSop} adalah FK opsional ke satu
 * {@link AktorSop} yang, bila diisi, dimaksudkan menjadi sumber kebenaran wewenang untuk seluruh
 * kategori ini — MENGGANTIKAN field lokal {@link #jenisPengguna}/{@link #usernamePengguna} milik
 * {@code JenisSop} sendiri. Konsep ini BERBEDA dari {@link AktorSop} yang dilekatkan ke satu
 * {@link AlurSop} (satu langkah alur SOP tertentu) — {@code aktorSop} di sini beroperasi pada
 * tingkat KATEGORI (siapa yang boleh memakai/melihat jenis SOP ini sama sekali), bukan pada
 * tingkat langkah persetujuan individual.</p>
 *
 * <p><b>Bug salin-tempel pada {@link #getJenisPengguna()} (FIXED):</b> sebelum diperbaiki, saat
 * {@link #aktorSop} terisi, method tersebut secara keliru menulis hasil delegasi ke field
 * {@link #usernamePengguna} (bukan {@link #jenisPengguna}), tampak sebagai kesalahan
 * salin-tempel dari {@link #getUsernamePengguna()} yang lupa mengganti nama variabel target.
 * Sudah ditambal agar menimpa {@link #jenisPengguna}, menyerupai pola delegasi yang benar pada
 * {@link #getUsernamePengguna()}.</p>
 *
 * @see Sop
 * @see AktorSop
 * @see AlurSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_sop")

public class JenisSop extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi untuk kontrak {@link java.io.Serializable} yang diwarisi dari
	 * {@link GeneralValueObject}. Dipertahankan konstan agar instance yang pernah diserialisasi
	 * tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** ID baris (primary key), digenerate database ({@code IDENTITY}); lihat {@link #getId()}. */
	private Long id;
	/**
	 * Field audit "shadow" berisi label penyunting terakhir, berpasangan dengan {@link #olehId}.
	 * Diisi mandiri oleh pemanggil dan hanya menerima nilai non-kosong — lihat
	 * {@link #setOleh(String)}. Pola pasangan nama/ID ini berulang di banyak entity AIS sebagai
	 * keharusan teknis, bukan bug.
	 */
	private String oleh;
	/**
	 * Field audit "shadow" berisi identitas mentah penyunting terakhir, berpasangan dengan
	 * {@link #oleh}. Lihat javadoc {@link #oleh}.
	 */
	private String olehId;

	/**
	 * @return identitas mentah penyunting terakhir sebagaimana tersimpan, tanpa transformasi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi {@link #olehId}. Nilai {@code null} atau string kosong/hanya-spasi DIABAIKAN
	 * secara diam-diam agar baris audit yang sudah terisi tidak tertimpa kosong.
	 *
	 * @param olehId identitas penyunting; diabaikan bila null/kosong/hanya-spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi {@link #oleh}. Sama seperti {@link #setOlehId(String)}, nilai null/kosong/
	 * hanya-spasi diabaikan secara diam-diam.
	 *
	 * @param oleh label penyunting; diabaikan bila null/kosong/hanya-spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return label penyunting terakhir sebagaimana tersimpan, tanpa transformasi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence tepat sebelum baris ini
	 * di-{@code UPDATE}. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang memperbarui
	 * {@link #tanggal_dirubah} secara konsisten lintas entity. Tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Timestamp perubahan terakhir baris ini, diinisialisasi ke waktu pembuatan instance dan
	 * diperbarui lewat {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah timestamp perubahan baru; dipakai terutama oleh kode migrasi/impor
	 *                         karena alur normal memperbarui field ini lewat {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp perubahan terakhir baris ini, dipetakan sebagai kolom {@code TIMESTAMP}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas untuk log/debug: {@code id} digabung {@link #nama} dengan
	 *         pemisah {@code "-"}. Tidak dimaksudkan untuk ditampilkan ke pengguna akhir.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat kategori SOP; lihat {@link #getKode()}. */
	private String kode;

	/** Nama tampil kategori SOP; kolom wajib di skema. */
	private String nama;
	/** Deskripsi/keterangan bebas teks tentang kategori ini. */
	private String keterangan;
	/** Warna latar untuk tampilan kategori (format {@code #RRGGBB}); lihat {@link #getWarna()}. */
	private String warna;
	/** Warna teks untuk tampilan kategori (format {@code #RRGGBB}); lihat {@link #getWarnatext()}. */
	private String warnatext;
	/**
	 * Target audiens kategori (mis. konstanta {@code GrupChecklistPenilaianUmum.UNTUK_UMUM});
	 * diwariskan oleh seluruh {@link Sop} anggota lewat {@link Sop#getDiperuntukkan()}.
	 */
	private String diperuntukkan;
	/**
	 * Daftar role/jenis pengguna berpisah-koma yang berwenang atas kategori ini bila
	 * {@link #aktorSop} tidak diisi (atau ditimpa oleh delegasi {@link AktorSop#getJenisPengguna()}
	 * bila terisi); lihat {@link #getJenisPengguna()}.
	 */
	private String jenisPengguna;
	/**
	 * Daftar username berpisah-koma yang berwenang atas kategori ini bila {@link #aktorSop}
	 * tidak diisi (atau ditimpa oleh delegasi {@link AktorSop#getUsernamePengguna()} bila
	 * terisi); lihat {@link #getUsernamePengguna()}.
	 */
	private String usernamePengguna;
	/** Flag aktif/nonaktif; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;
	/**
	 * FK opsional ke {@link AktorSop} yang, bila diisi, menjadi sumber kebenaran wewenang untuk
	 * SELURUH kategori ini — menggantikan {@link #jenisPengguna}/{@link #usernamePengguna}
	 * lokal. Lihat catatan delegasi pada javadoc kelas serta {@link #getJenisPengguna()}.
	 */
	private AktorSop aktorSop;

	/** Konstruktor default kosong, dipakai Hibernate serta kode aplikasi yang membangun kategori baru sebelum mengisi field satu per satu. */
	public JenisSop() {
	}

	/**
	 * @return ID baris (primary key), digenerate database dan tidak pernah di-{@code INSERT}
	 *         manual ({@code insertable = false}).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id ID baris; lazimnya hanya diisi ulang oleh Hibernate saat memuat entity, bukan
	 *           oleh kode aplikasi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return {@link #kode} yang sudah di-{@code trim()}, atau string kosong (bukan
	 *         {@code null}) bila field belum diisi.
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * @param kode kode singkat kategori baru; disimpan apa adanya (trimming terjadi saat dibaca).
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return {@link #nama} yang sudah di-{@code trim()}, atau {@code null} bila field memang
	 *         belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama tampil kategori baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return deskripsi/keterangan kategori apa adanya (tanpa trimming atau default).
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan deskripsi/keterangan kategori baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return status aktif kategori ini; {@code true} bila field belum pernah diisi (default
	 *         "aktif" agar kategori lama yang dibuat sebelum kolom ini ada tetap berlaku).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif/nonaktif baru untuk kategori ini.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan warna latar tampilan kategori ini. Bila field belum pernah diisi (null atau
	 * string kosong), method ini menetapkan default {@code "#FFFFFF"} (putih) dan MENULIS BALIK
	 * default tersebut ke field {@link #warna} sebagai efek samping (pola "getter yang menulis"
	 * — panggilan berikutnya pada instance yang sama tidak lagi melalui cabang default karena
	 * field sudah terisi).
	 *
	 * @return warna latar dalam format {@code #RRGGBB}, tidak pernah {@code null}/kosong.
	 */
	public String getWarna() {
		if (warna == null || warna.isEmpty()) {
			warna = "#FFFFFF";
		}
		return warna;
	}

	/**
	 * @param warna warna latar baru (format {@code #RRGGBB}) untuk tampilan kategori ini.
	 */
	public void setWarna(String warna) {
		this.warna = warna;
	}

	/**
	 * @return target audiens kategori ini: {@link #diperuntukkan} apa adanya bila terisi, atau
	 *         {@code GrupChecklistPenilaianUmum.UNTUK_UMUM} sebagai default bila field null/
	 *         kosong. Berbeda dari {@link #getWarna()}, default di sini TIDAK ditulis balik ke
	 *         field.
	 */
	@Column(name = "diperuntukkan", nullable = false)
	public String getDiperuntukkan() {
		return diperuntukkan == null || diperuntukkan.isEmpty() ? GrupChecklistPenilaianUmum.UNTUK_UMUM : diperuntukkan;
	}

	/**
	 * @param diperuntukkan target audiens baru untuk kategori ini.
	 */
	public void setDiperuntukkan(String diperuntukkan) {
		this.diperuntukkan = diperuntukkan;
	}

	/**
	 * Mengembalikan daftar role/jenis pengguna yang berwenang atas kategori ini.
	 *
	 * <p><b>Bug salin-tempel (FIXED):</b> sebelum diperbaiki, saat {@link #getAktorSop()} tidak
	 * {@code null}, cabang ini keliru menulis hasil delegasi ke field {@link #usernamePengguna}
	 * (bukan {@link #jenisPengguna}), tampak sebagai salin-tempel dari
	 * {@link #getUsernamePengguna()} yang lupa mengganti nama variabel target — akibatnya method
	 * ini tidak pernah benar-benar mengembalikan nilai dari {@link #aktorSop} dan sebagai efek
	 * samping mencemari {@link #usernamePengguna}. Sudah ditambal agar menimpa
	 * {@link #jenisPengguna}, menyerupai pola delegasi yang benar pada
	 * {@link #getUsernamePengguna()}.</p>
	 * <p>Cabang {@code else} (saat {@link #aktorSop} null) menormalkan {@link #jenisPengguna}
	 * menjadi bentuk terbungkus-koma lalu menyingkat kembali ke string kosong bila hasilnya hanya
	 * berisi kombinasi koma (mengcover 1-4 koma berturutan sebagai kasus khusus, alih-alih pola
	 * regex umum) — logika ini pada dasarnya menduplikasi (secara manual, tidak lewat helper
	 * bersama) fungsi yang di {@link AktorSop} sudah dirapikan menjadi
	 * {@code AktorSop.formatCommaSeparated}.</p>
	 *
	 * @return nilai jenis-pengguna kategori ini: diturunkan dari {@link #aktorSop} bila terisi,
	 *         atau field lokal ternormalisasi bila {@link #aktorSop} null.
	 */
	@Column(name = "jenis_pengguna", nullable = true, columnDefinition = "text")
	public String getJenisPengguna() {
		if (getAktorSop() != null) {
			jenisPengguna = getAktorSop().getJenisPengguna();
		} else {
			jenisPengguna = (jenisPengguna == null || jenisPengguna.trim().equalsIgnoreCase(",") ? ""
					: "," + jenisPengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
					.replaceAll(",,", ",");

			if (jenisPengguna.equals(",")) {
				jenisPengguna = "";
			} else if (jenisPengguna.equals(",,")) {
				jenisPengguna = "";
			} else if (jenisPengguna.equals(",,,")) {
				jenisPengguna = "";
			} else if (jenisPengguna.equals(",,,,")) {
				jenisPengguna = "";
			}
		}
		return jenisPengguna == null ? "" : jenisPengguna.trim();
	}

	/**
	 * @param jenisPengguna nilai lokal jenis-pengguna baru. Efektif hanya dipakai selama
	 *                       {@link #aktorSop} null; ditimpa oleh delegasi pada
	 *                       {@link #getJenisPengguna()} bila {@link #aktorSop} terisi.
	 */
	public void setJenisPengguna(String jenisPengguna) {
		this.jenisPengguna = jenisPengguna;
	}

	/**
	 * Mengembalikan daftar username yang berwenang atas kategori ini. Bila {@link #getAktorSop()}
	 * tidak {@code null}, field {@link #usernamePengguna} ditimpa oleh
	 * {@code getAktorSop().getUsernamePengguna()}, pola delegasi yang sama dengan
	 * {@link #getJenisPengguna()}. Cabang {@code else} menormalkan {@link #usernamePengguna}
	 * lokal ke bentuk terbungkus-koma dengan logika manual yang sama seperti pada
	 * {@link #getJenisPengguna()}.
	 *
	 * @return username pengguna yang diturunkan dari {@link #aktorSop} (kasus umum, benar), atau
	 *         field lokal ternormalisasi bila {@link #aktorSop} null.
	 */
	@Column(name = "username_pengguna", nullable = true, columnDefinition = "text")
	public String getUsernamePengguna() {
		if (getAktorSop() != null) {
			usernamePengguna = getAktorSop().getUsernamePengguna();
		} else {
			usernamePengguna = (usernamePengguna == null || usernamePengguna.trim().equalsIgnoreCase(",") ? ""
					: "," + usernamePengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
					.replaceAll(",,", ",");

			if (usernamePengguna.equals(",")) {
				usernamePengguna = "";
			} else if (usernamePengguna.equals(",,")) {
				usernamePengguna = "";
			} else if (usernamePengguna.equals(",,,")) {
				usernamePengguna = "";
			} else if (usernamePengguna.equals(",,,,")) {
				usernamePengguna = "";
			}
		}
		return usernamePengguna == null ? "" : usernamePengguna.trim();
	}

	/**
	 * @param usernamePengguna nilai lokal username-pengguna baru. Efektif ditimpa kembali oleh
	 *                          {@link #getUsernamePengguna()} selama {@link #aktorSop} terisi.
	 */
	public void setUsernamePengguna(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}

	/**
	 * Mengembalikan warna teks tampilan kategori ini. Mengikuti pola "getter yang menulis" yang
	 * sama dengan {@link #getWarna()}: bila field belum diisi, ditetapkan default
	 * {@code "#000000"} (hitam) dan ditulis balik ke {@link #warnatext}.
	 *
	 * @return warna teks dalam format {@code #RRGGBB}, tidak pernah {@code null}/kosong.
	 */
	public String getWarnatext() {
		if (warnatext == null || warnatext.isEmpty()) {
			warnatext = "#000000";
		}
		return warnatext;
	}

	/**
	 * @param warnatext warna teks baru (format {@code #RRGGBB}) untuk tampilan kategori ini.
	 */
	public void setWarnatext(String warnatext) {
		this.warnatext = warnatext;
	}

	/**
	 * @return {@link AktorSop} pemilik delegasi wewenang kategori ini (bila diisi), dijamin
	 *         bukan proxy Hibernate basi berkat {@link #check(Object)}; {@code null} bila
	 *         kategori ini memakai {@link #jenisPengguna}/{@link #usernamePengguna} lokalnya
	 *         sendiri alih-alih delegasi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "aktor_sop", nullable = true)
	public AktorSop getAktorSop() {
		aktorSop = check(aktorSop);
		return aktorSop;
	}

	/**
	 * @param aktorSop aktor delegasi wewenang baru untuk kategori ini. Mengisi field ini
	 *                  mengaktifkan jalur delegasi pada {@link #getJenisPengguna()} dan
	 *                  {@link #getUsernamePengguna()}.
	 */
	public void setAktorSop(AktorSop aktorSop) {
		this.aktorSop = aktorSop;
	}
}
