package ais.database.model;

// Generated Dec 22, 2009 12:14:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
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
 * Entity Hibernate/JPA untuk tabel {@code public.matakuliah_ekivalen} — pemetaan
 * <b>ekivalensi mata kuliah</b>: menyatakan bahwa {@link #getMatakuliah()} setara/dapat
 * disetarakan dengan {@link #getMatakuliahEkivalen()} (wajib) dan hingga empat alternatif
 * tambahan ({@link #getMatakuliahEkivalen2()}..{@link #getMatakuliahEkivalen5()}, opsional) —
 * dipakai saat konversi transkrip mahasiswa pindahan/alih kredit, dengan opsi membatasi
 * berlaku hanya untuk NIM tertentu ({@link #getKhususUntukNim()}).
 *
 * <p>Entity ini mendukung mekanisme "persisted file location" milik Generic CRUD: {@link
 * #write()} menuliskan snapshot entity (beserta beberapa master terkait) ke berkas JSON di
 * disk dan menyimpan pathnya ke {@link #fileLocation}. Getter {@link #getFileLocation()} murni
 * membaca path tersimpan (aman dipanggil reflektif oleh Generic CRUD tanpa efek samping),
 * sedangkan {@link #getOrCreateFileLocation()} — method terpisah, ditandai {@code @Transient}
 * dan tidak cocok dengan nama field mana pun — yang boleh memicu penulisan ulang berkas bila
 * path belum ada/basi. Pemisahan dua method ini konsisten dengan pola "getter side-effect-free
 * untuk field yang dipersist" yang diterapkan pada banyak entity lain di codebase.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "matakuliah_ekivalen")
public class MatakuliahEkivalen extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 1950126270979098967L;
	/** Primary key baris {@code matakuliah_ekivalen}, kolom {@code id} (identity, auto-generate). */
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
	 * Representasi ringkas untuk log/debug: {@code "<matakuliah>_<matakuliahEkivalen>"}.
	 *
	 * @return string ringkas identitas pemetaan ekivalensi ini
	 */
	public String toString() {
		return matakuliah + "_" + matakuliahEkivalen;
	}

	/** Mata kuliah asal yang akan disetarakan. */
	private Matakuliah matakuliah;
	/** Mata kuliah ekivalen utama (wajib). */
	private Matakuliah matakuliahEkivalen;
	/** Mata kuliah ekivalen alternatif ke-2 (opsional). */
	private Matakuliah matakuliahEkivalen2;
	/** Mata kuliah ekivalen alternatif ke-3 (opsional). */
	private Matakuliah matakuliahEkivalen3;
	/** Mata kuliah ekivalen alternatif ke-4 (opsional). */
	private Matakuliah matakuliahEkivalen4;
	/** Mata kuliah ekivalen alternatif ke-5 (opsional). */
	private Matakuliah matakuliahEkivalen5;
	/** Keterangan bebas untuk pemetaan ekivalensi ini. */
	private String keterangan;
	/** Daftar NIM yang membatasi berlakunya pemetaan ini (format encoded, lihat {@link #getKhususUntukNim()}); kosong berarti berlaku untuk semua NIM. */
	private String khususUntukNim;
	/** Flag status (makna spesifik ditentukan pemanggil); default {@code true} bila belum diisi. */
	private Boolean udah;

	/** Path berkas JSON snapshot entity ini di disk, ditulis oleh {@link #write()}; lihat catatan Javadoc kelas mengenai pemisahan getter aman/tak-aman. */
	private String fileLocation;
	/** Flag aktif pemetaan ekivalensi ini; default {@code true} bila belum diisi. */
	private Boolean aktif;

	/**
	 * Menuliskan snapshot entity ini (beserta sejumlah master terkait: {@link Jurusan}, {@link
	 * Dosen}, {@link TingkatKesulitanMatakuliah}, {@link Kurikulum}, {@link MasaPerkuliahan},
	 * {@link JamPerkuliahan}, {@link JenisEvaluasi}, {@link Ruang}) ke satu berkas JSON di disk
	 * lewat mekanisme {@code write(Class...)} milik {@link GeneralValueObject}, lalu menyimpan
	 * path hasilnya ke {@link #fileLocation} lewat {@link #setFileLocation(String)}.
	 *
	 * <p><b>Efek samping:</b> operasi tulis berkas ke disk; mengubah field {@link #fileLocation}
	 * pada instance ini (perlu di-{@code save} lagi bila perubahan itu harus persisten).</p>
	 *
	 * @return berkas JSON yang baru ditulis
	 */
	public File write() {
		File f = write(Jurusan.class.getName(), Dosen.class.getName(), TingkatKesulitanMatakuliah.class.getName(),
				Kurikulum.class.getName(), MasaPerkuliahan.class.getName(), JamPerkuliahan.class.getName(),
				JenisEvaluasi.class.getName(), Ruang.class.getName());
		setFileLocation(f.getAbsolutePath());
		return f;
	}

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public MatakuliahEkivalen() {
	}

	/**
	 * @return primary key baris {@code matakuliah_ekivalen}; {@code null} sebelum baris
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
	 * @return mata kuliah asal yang akan disetarakan (proxy lazy diresolusi via {@code
	 *         check()}); kolomnya {@code nullable = false} sehingga secara skema selalu terisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah", nullable = false)
	public Matakuliah getMatakuliah() {
		matakuliah = check(matakuliah);
		return this.matakuliah;
	}

	/**
	 * @param matakuliah mata kuliah asal baru.
	 */
	public void setMatakuliah(Matakuliah matakuliah) {
		this.matakuliah = matakuliah;
	}

	/**
	 * @return mata kuliah ekivalen utama (proxy lazy diresolusi via {@code check()}); kolomnya
	 *         {@code nullable = false} sehingga secara skema selalu terisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_ekivalen", nullable = false)
	public Matakuliah getMatakuliahEkivalen() {
		matakuliahEkivalen = check(matakuliahEkivalen);
		return this.matakuliahEkivalen;
	}

	/**
	 * @param matakuliahEkivalen mata kuliah ekivalen utama baru.
	 */
	public void setMatakuliahEkivalen(Matakuliah matakuliahEkivalen) {
		this.matakuliahEkivalen = matakuliahEkivalen;
	}

	/**
	 * @return keterangan bebas pemetaan ini; boleh {@code null}.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk pemetaan ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Daftar NIM yang membatasi berlakunya pemetaan ekivalensi ini.
	 *
	 * <p><b>Getter yang menormalkan-dan-menulis-balik (idempoten):</b> setiap kali dipanggil,
	 * field {@link #khususUntukNim} dibungkus dengan koma di awal/akhir ({@code
	 * ",<isi>,"}) lalu koma ganda dikompres berulang tiga kali ({@code replaceAll(",,", ",")}
	 * dipanggil tiga kali berturutan — cukup untuk mengompres deretan hingga 2<sup>3</sup>=8
	 * koma beruntun menjadi satu), kemudian hasil yang persis berupa 1 s.d. 5 koma saja
	 * dinormalkan menjadi string kosong. Berbeda dari kebanyakan getter-menulis-balik lain di
	 * cluster ini, transformasi ini IDEMPOTEN — memanggilnya berulang pada hasil yang sudah
	 * dinormalkan tidak mengubah apa pun lagi — sehingga risikonya jauh lebih rendah daripada
	 * pola getter yang menghapus/menimpa data pada domain lain.</p>
	 *
	 * @return daftar NIM ter-encode, dibungkus koma di awal/akhir (mis. {@code ",A,B,"}) dan
	 *         di-{@code trim()}; string kosong ({@code ""}) berarti pemetaan berlaku untuk
	 *         semua NIM (tidak dibatasi).
	 */
	@Column(columnDefinition = "text", name = "khusus_untuk_nim")
	public String getKhususUntukNim() {
		khususUntukNim = (khususUntukNim == null || khususUntukNim.trim().equalsIgnoreCase(",") ? ""
				: "," + khususUntukNim.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (khususUntukNim.equals(",")) {
			khususUntukNim = "";
		} else if (khususUntukNim.equals(",,")) {
			khususUntukNim = "";
		} else if (khususUntukNim.equals(",,,")) {
			khususUntukNim = "";
		} else if (khususUntukNim.equals(",,,,")) {
			khususUntukNim = "";
		} else if (khususUntukNim.equals(",,,,,")) {
			khususUntukNim = "";
		}

		if (khususUntukNim == null) {
			khususUntukNim = "";
		}

		return khususUntukNim.trim();
	}

	/**
	 * @param khususUntukNim daftar NIM baru (format bebas; akan di-encode ulang oleh {@link
	 *                       #getKhususUntukNim()} pada pembacaan berikutnya).
	 */
	public void setKhususUntukNim(String khususUntukNim) {
		this.khususUntukNim = khususUntukNim;
	}

	/**
	 * @return status flag ini (makna spesifik ditentukan pemanggil); default {@code true} bila
	 *         belum diisi.
	 */
	public Boolean getUdah() {
		return udah == null ? true : udah;
	}

	/**
	 * @param udah status flag baru.
	 */
	public void setUdah(Boolean udah) {
		this.udah = udah;
	}

	/**
	 * @return path berkas JSON snapshot entity ini di disk apa adanya, TANPA memicu penulisan
	 *         ulang bila belum ada/basi; boleh {@code null} bila {@link #write()} belum pernah
	 *         dipanggil. Aman dipanggil reflektif oleh Generic CRUD (getter murni, sesuai
	 *         nama field {@link #fileLocation} — lihat catatan Javadoc kelas).
	 */
	public String getFileLocation() {
		return fileLocation;
	}

	/**
	 * Mengambil path berkas JSON snapshot entity ini, MENULIS ULANG berkas lewat {@link
	 * #write()} lebih dulu bila path belum ada, tidak berakhiran {@code "<id>.json"}, atau
	 * berkasnya sudah tidak ada di disk.
	 *
	 * <p>Ditandai {@code @Transient} (tidak dipersist) dan sengaja diberi nama yang TIDAK cocok
	 * dengan field {@link #fileLocation} — berbeda dari {@link #getFileLocation()} yang murni
	 * membaca, method ini boleh memicu efek samping tulis-disk sehingga tidak boleh
	 * dipanggil secara reflektif oleh Generic CRUD sekadar untuk menampilkan grid.</p>
	 *
	 * @return path berkas JSON yang terjamin ada dan sesuai ID entity ini saat method
	 *         kembali (kecuali {@link #write()} sendiri gagal)
	 */
	@javax.persistence.Transient
	public String getOrCreateFileLocation() {
		if (fileLocation == null || !fileLocation.endsWith(getId() + ".json")
				|| java.nio.file.Files.notExists(java.nio.file.Paths.get(fileLocation))) {
			write();
		}
		return fileLocation;
	}

	/**
	 * @param fileLocation path berkas JSON baru; biasanya tidak perlu diset manual karena
	 *                     dikelola oleh {@link #write()}.
	 */
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

	/**
	 * @return mata kuliah ekivalen alternatif ke-2 (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_ekivalen2", nullable = true)
	public Matakuliah getMatakuliahEkivalen2() {
		matakuliahEkivalen2 = check(matakuliahEkivalen2);
		return matakuliahEkivalen2;
	}

	/**
	 * @param matakuliahEkivalen2 mata kuliah ekivalen alternatif ke-2 baru; {@code null} untuk
	 *                            melepas tautan.
	 */
	public void setMatakuliahEkivalen2(Matakuliah matakuliahEkivalen2) {
		this.matakuliahEkivalen2 = matakuliahEkivalen2;
	}

	/**
	 * @return mata kuliah ekivalen alternatif ke-3 (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_ekivalen3", nullable = true)
	public Matakuliah getMatakuliahEkivalen3() {
		matakuliahEkivalen3 = check(matakuliahEkivalen3);
		return matakuliahEkivalen3;
	}

	/**
	 * @param matakuliahEkivalen3 mata kuliah ekivalen alternatif ke-3 baru; {@code null} untuk
	 *                            melepas tautan.
	 */
	public void setMatakuliahEkivalen3(Matakuliah matakuliahEkivalen3) {
		this.matakuliahEkivalen3 = matakuliahEkivalen3;
	}

	/**
	 * @return mata kuliah ekivalen alternatif ke-4 (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_ekivalen4", nullable = true)
	public Matakuliah getMatakuliahEkivalen4() {
		matakuliahEkivalen4 = check(matakuliahEkivalen4);
		return matakuliahEkivalen4;
	}

	/**
	 * @param matakuliahEkivalen4 mata kuliah ekivalen alternatif ke-4 baru; {@code null} untuk
	 *                            melepas tautan.
	 */
	public void setMatakuliahEkivalen4(Matakuliah matakuliahEkivalen4) {
		this.matakuliahEkivalen4 = matakuliahEkivalen4;
	}

	/**
	 * @return mata kuliah ekivalen alternatif ke-5 (proxy lazy diresolusi via {@code check()});
	 *         boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah_ekivalen5", nullable = true)
	public Matakuliah getMatakuliahEkivalen5() {
		matakuliahEkivalen5 = check(matakuliahEkivalen5);
		return matakuliahEkivalen5;
	}

	/**
	 * @param matakuliahEkivalen5 mata kuliah ekivalen alternatif ke-5 baru; {@code null} untuk
	 *                            melepas tautan.
	 */
	public void setMatakuliahEkivalen5(Matakuliah matakuliahEkivalen5) {
		this.matakuliahEkivalen5 = matakuliahEkivalen5;
	}

	/**
	 * @return status aktif pemetaan ekivalensi ini; default {@code true} bila belum diisi.
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
