package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
 * Entity Hibernate/JPA untuk tabel {@code public.diskon_mahasiswa} — satu baris merupakan
 * <b>penautan diskon per-orang</b>: penunjukan eksplisit bahwa seorang {@link Mahasiswa} (atau
 * {@link BiodataCalonMahasiswa}, untuk calon mahasiswa PMB) berhak atas satu {@link
 * JenisDiskonMahasiswa} tertentu.
 *
 * <p>Perlu dibedakan dari {@link JenisDiskonMahasiswa}: entity itu adalah <b>master/definisi
 * jenis diskon</b> (nama, besaran, apakah persen atau nominal tetap, rentang tanggal/semester
 * berlaku, filter Fakultas/Jurusan/Program/Status Awal, dan sejak 19-08-2026 juga mekanisme
 * "promo global"). Entity ini ({@code DiskonMahasiswa}) adalah <b>baris pemberian</b> — bukti
 * bahwa satu mahasiswa/calon mahasiswa tertentu ditautkan ke satu jenis diskon, dengan sejumlah
 * field ({@code aktif}, {@code semesterMulai}, {@code semesterSampai}, {@code itemBiaya}..{@code
 * itemBiaya5}) yang <b>mewarisi nilai dari jenis diskonnya</b> bila field lokal kosong — lihat
 * catatan pada masing-masing getter di bawah.</p>
 *
 * <p>Dipakai sebagai slot ke-1/2/3 pada {@link DetailKegiatan#getDiskonMahasiswaData()} dan
 * kembarannya (mesin billing pusat, lihat {@code Kegiatan.java}/{@code DetailBiaya.java}/{@code
 * DetailKegiatan.java}) untuk menentukan diskon mana yang berlaku pada satu baris tagihan.</p>
 *
 * @see JenisDiskonMahasiswa
 * @see DetailKegiatan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "diskon_mahasiswa")
public class DiskonMahasiswa extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code diskon_mahasiswa}, kolom {@code id} (identity, auto-generate). */
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
	 * Representasi ringkas untuk log/debug: {@code "<id>-<nama mahasiswa>"} (nama kosong bila
	 * mahasiswa tidak ada).
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getMahasiswa()}, yang menulis balik field
	 * {@code mahasiswa} (resolusi proxy lazy via {@code check()}) — sekadar memanggil
	 * {@code toString()} bisa memicu lazy-load mahasiswa terkait.</p>
	 *
	 * @return string ringkas {@code id-nama}
	 */
	public String toString() {
		mahasiswa = getMahasiswa();
		return id + "-" + (mahasiswa != null ? mahasiswa.getNama() : "");
	}

	/** Keterangan bebas untuk baris penautan diskon ini, kolom {@code keterangan}. */
	private String keterangan;
	/** Flag aktif lokal, kolom {@code aktif}; lihat {@link #getAktif()} untuk cara field ini ditimpa. */
	private Boolean aktif;
	/** Jenis diskon yang ditautkan (FK {@code jenis_diskon_mahasiswa}, wajib diisi). */
	private JenisDiskonMahasiswa jenisDiskonMahasiswa;
	/** Mahasiswa aktif penerima diskon (FK {@code mahasiswa}), boleh kosong untuk baris calon mahasiswa. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa (PMB) penerima diskon (FK {@code biodata_calon_mahasiswa}), boleh kosong untuk baris mahasiswa aktif. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Item biaya sasaran diskon slot ke-1 (FK {@code item_biaya}, wajib); lihat {@link #getItemBiaya()}. */
	private ItemBiaya itemBiaya;
	/** Batas bawah semester berlakunya diskon ini (kolom implisit, lihat {@link #getSemesterMulai()}). */
	private Integer semesterMulai;
	/** Batas atas semester berlakunya diskon ini (kolom implisit, lihat {@link #getSemesterSampai()}). */
	private Integer semesterSampai;
	/** Item biaya sasaran diskon slot ke-2 (FK {@code item_biaya_2}, opsional). */
	private ItemBiaya itemBiaya2;
	/** Item biaya sasaran diskon slot ke-3 (FK {@code item_biaya_3}, opsional). */
	private ItemBiaya itemBiaya3;
	/** Item biaya sasaran diskon slot ke-4 (FK {@code item_biaya_4}, opsional). */
	private ItemBiaya itemBiaya4;
	/** Item biaya sasaran diskon slot ke-5 (FK {@code item_biaya_5}, opsional). */
	private ItemBiaya itemBiaya5;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public DiskonMahasiswa() {
	}

	/**
	 * @return primary key baris {@code diskon_mahasiswa}; {@code null} sebelum baris di-{@code INSERT}.
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
	 * @return keterangan bebas untuk baris penautan diskon ini; boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan baru untuk baris ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif efektif dari penautan diskon ini.
	 *
	 * <p><b>Flag aktif satu arah:</b> selama {@link #getJenisDiskonMahasiswa()} mengembalikan
	 * objek non-null (praktis selalu, karena kolomnya {@code nullable = false}), field lokal
	 * {@link #aktif} <b>ditimpa</b> oleh {@code getJenisDiskonMahasiswa().getAktif()} — nilai
	 * {@code aktif} yang tersimpan di baris {@code diskon_mahasiswa} sendiri hanya dipakai bila
	 * jenis diskonnya (secara mustahil) {@code null}. Dengan kata lain, menonaktifkan satu
	 * penautan per-orang tanpa menonaktifkan jenis diskonnya secara keseluruhan <b>tidak
	 * berpengaruh</b> lewat getter ini; hanya menonaktifkan {@link JenisDiskonMahasiswa} yang
	 * benar-benar mematikan diskon. Pola ini sejenis dengan flag aktif satu-arah yang sudah
	 * dicatat berulang pada domain lain; tidak diperbaiki di sesi dokumentasi ini.</p>
	 *
	 * @return {@code true}/{@code false} sesuai status aktif jenis diskon terkait, atau {@code
	 *         true} sebagai default bila entah bagaimana jenis diskonnya {@code null}.
	 */
	public Boolean getAktif() {
		if (getJenisDiskonMahasiswa() != null) {
			aktif = getJenisDiskonMahasiswa().getAktif();
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel field {@code aktif} lokal. Perhatikan bahwa nilai yang diset di sini biasanya
	 * tidak pernah terlihat lewat {@link #getAktif()} selama jenis diskon terkait ada — lihat
	 * javadoc getter tersebut.
	 *
	 * @param aktif nilai aktif lokal baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return jenis diskon yang ditautkan pada baris ini (proxy lazy diresolusi via {@code
	 *         check()} dan ditulis balik ke field). Kolomnya {@code nullable = false} sehingga
	 *         secara skema selalu terisi untuk baris yang tersimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_diskon_mahasiswa", nullable = false)
	public JenisDiskonMahasiswa getJenisDiskonMahasiswa() {
		jenisDiskonMahasiswa = check(jenisDiskonMahasiswa);
		return jenisDiskonMahasiswa;
	}

	/**
	 * @param jenisDiskonMahasiswa jenis diskon baru yang ditautkan.
	 */
	public void setJenisDiskonMahasiswa(JenisDiskonMahasiswa jenisDiskonMahasiswa) {
		this.jenisDiskonMahasiswa = jenisDiskonMahasiswa;
	}

	/**
	 * @return mahasiswa aktif penerima diskon ini (proxy lazy diresolusi via {@code check()});
	 *         {@code null} untuk baris yang ditautkan ke calon mahasiswa ({@link
	 *         #getBiodataCalonMahasiswa()}), bukan mahasiswa aktif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * @param mahasiswa mahasiswa penerima diskon baru; {@code null} untuk melepas tautan.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Item biaya sasaran diskon slot ke-1, dengan <b>pewarisan dari jenis diskon</b>: bila
	 * {@link #getJenisDiskonMahasiswa()} tidak {@code null} dan item biaya slot ke-1 di sana
	 * juga tidak {@code null}, nilai itu yang dipakai (menimpa field lokal); baru kalau jenis
	 * diskonnya tidak mengisi slot ini, field lokal {@link #itemBiaya} dipakai (setelah
	 * diresolusi lewat {@code check()}). Pola pewarisan-menimpa yang sama diulang identik untuk
	 * slot ke-2 s.d. ke-5 di bawah.
	 *
	 * @return item biaya efektif untuk slot ke-1 (dari jenis diskon bila diisi, dari field lokal
	 *         bila tidak); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = false)
	public ItemBiaya getItemBiaya() {
		if (getJenisDiskonMahasiswa() != null && getJenisDiskonMahasiswa().getItemBiaya() != null) {
			itemBiaya = getJenisDiskonMahasiswa().getItemBiaya();
		} else {
			itemBiaya = check(itemBiaya);
		}
		return itemBiaya;
	}

	/**
	 * @param itemBiaya item biaya slot ke-1 baru untuk field lokal (bisa tetap ditimpa oleh nilai
	 *                  jenis diskon saat dibaca via {@link #getItemBiaya()} — lihat javadoc getter).
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Item biaya sasaran diskon slot ke-2, dengan pewarisan dari jenis diskon yang sama seperti
	 * {@link #getItemBiaya()}.
	 *
	 * @return item biaya efektif slot ke-2; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_2", nullable = true)
	public ItemBiaya getItemBiaya2() {
		if (getJenisDiskonMahasiswa() != null && getJenisDiskonMahasiswa().getItemBiaya2() != null) {
			itemBiaya2 = getJenisDiskonMahasiswa().getItemBiaya2();
		} else {
			itemBiaya2 = check(itemBiaya2);
		}
		return itemBiaya2;
	}

	/**
	 * @param itemBiaya2 item biaya slot ke-2 baru untuk field lokal.
	 */
	public void setItemBiaya2(ItemBiaya itemBiaya2) {
		this.itemBiaya2 = itemBiaya2;
	}

	/**
	 * Item biaya sasaran diskon slot ke-3, dengan pewarisan dari jenis diskon yang sama seperti
	 * {@link #getItemBiaya()}.
	 *
	 * @return item biaya efektif slot ke-3; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_3", nullable = true)
	public ItemBiaya getItemBiaya3() {
		if (getJenisDiskonMahasiswa() != null && getJenisDiskonMahasiswa().getItemBiaya3() != null) {
			itemBiaya3 = getJenisDiskonMahasiswa().getItemBiaya3();
		} else {
			itemBiaya3 = check(itemBiaya3);
		}
		return itemBiaya3;
	}

	/**
	 * @param itemBiaya3 item biaya slot ke-3 baru untuk field lokal.
	 */
	public void setItemBiaya3(ItemBiaya itemBiaya3) {
		this.itemBiaya3 = itemBiaya3;
	}

	/**
	 * Item biaya sasaran diskon slot ke-4, dengan pewarisan dari jenis diskon yang sama seperti
	 * {@link #getItemBiaya()}.
	 *
	 * @return item biaya efektif slot ke-4; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_4", nullable = true)
	public ItemBiaya getItemBiaya4() {
		if (getJenisDiskonMahasiswa() != null && getJenisDiskonMahasiswa().getItemBiaya4() != null) {
			itemBiaya4 = getJenisDiskonMahasiswa().getItemBiaya4();
		} else {
			itemBiaya4 = check(itemBiaya4);
		}
		return itemBiaya4;
	}

	/**
	 * @param itemBiaya4 item biaya slot ke-4 baru untuk field lokal.
	 */
	public void setItemBiaya4(ItemBiaya itemBiaya4) {
		this.itemBiaya4 = itemBiaya4;
	}

	/**
	 * Item biaya sasaran diskon slot ke-5, dengan pewarisan dari jenis diskon yang sama seperti
	 * {@link #getItemBiaya()}.
	 *
	 * @return item biaya efektif slot ke-5; boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya_5", nullable = true)
	public ItemBiaya getItemBiaya5() {
		if (getJenisDiskonMahasiswa() != null && getJenisDiskonMahasiswa().getItemBiaya5() != null) {
			itemBiaya5 = getJenisDiskonMahasiswa().getItemBiaya5();
		} else {
			itemBiaya5 = check(itemBiaya5);
		}
		return itemBiaya5;
	}

	/**
	 * @param itemBiaya5 item biaya slot ke-5 baru untuk field lokal.
	 */
	public void setItemBiaya5(ItemBiaya itemBiaya5) {
		this.itemBiaya5 = itemBiaya5;
	}

	/**
	 * Mengumpulkan semua item biaya slot 1&ndash;5 yang efektif tidak {@code null} (mengikuti
	 * pewarisan dari jenis diskon bila berlaku) ke dalam satu daftar.
	 *
	 * @return daftar item biaya yang tercakup oleh diskon ini, urut slot 1 s.d. 5; tidak pernah
	 *         {@code null}, boleh kosong.
	 */
	public List<ItemBiaya> ambilItemBiayas() {
		List<ItemBiaya> itemBiayas = new ArrayList<ItemBiaya>();
		if (getItemBiaya() != null) {
			itemBiayas.add(itemBiaya);
		}
		if (getItemBiaya2() != null) {
			itemBiayas.add(itemBiaya2);
		}
		if (getItemBiaya3() != null) {
			itemBiayas.add(itemBiaya3);
		}
		if (getItemBiaya4() != null) {
			itemBiayas.add(itemBiaya4);
		}
		if (getItemBiaya5() != null) {
			itemBiayas.add(itemBiaya5);
		}
		return itemBiayas;
	}

	/**
	 * Sama seperti {@link #ambilItemBiayas()} tetapi mengembalikan ID-nya saja. Dipakai antara
	 * lain oleh {@link JenisDiskonMahasiswa#cocokUntukTagihanGlobal} (via jenis diskon, bukan
	 * lewat entity ini) dan oleh mesin billing untuk menguji keanggotaan {@code
	 * detailBiaya.getItemBiaya()} pada daftar item biaya diskon ini.
	 *
	 * @return daftar ID item biaya slot 1 s.d. 5 yang efektif tidak {@code null}; tidak pernah
	 *         {@code null}, boleh kosong.
	 */
	public List<Long> ambilItemBiayaIds() {
		List<Long> itemBiayas = new ArrayList<Long>();
		if (getItemBiaya() != null) {
			itemBiayas.add(itemBiaya.getId());
		}
		if (getItemBiaya2() != null) {
			itemBiayas.add(itemBiaya2.getId());
		}
		if (getItemBiaya3() != null) {
			itemBiayas.add(itemBiaya3.getId());
		}
		if (getItemBiaya4() != null) {
			itemBiayas.add(itemBiaya4.getId());
		}
		if (getItemBiaya5() != null) {
			itemBiayas.add(itemBiaya5.getId());
		}
		return itemBiayas;
	}

	/**
	 * @return calon mahasiswa (PMB) penerima diskon ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} untuk baris yang ditautkan ke mahasiswa aktif ({@link
	 *         #getMahasiswa()}), bukan calon mahasiswa. Nilai non-null di sini juga dipakai
	 *         sebagai penentu default {@link #getSemesterSampai()} (1 untuk calon mahasiswa,
	 *         8 untuk mahasiswa aktif).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/**
	 * @param biodataCalonMahasiswa calon mahasiswa penerima diskon baru; {@code null} untuk
	 *                              melepas tautan.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Batas bawah semester berlakunya diskon ini, dengan pewarisan dari jenis diskon: bila
	 * {@link #getJenisDiskonMahasiswa()} tidak {@code null} dan {@code
	 * jenisDiskonMahasiswa.getSemesterMulai()} juga tidak {@code null}, nilai itu dipakai
	 * (menimpa field lokal {@link #semesterMulai}). Bila hasil akhirnya masih {@code null},
	 * dikembalikan {@code 0} (tanpa batas bawah efektif).
	 *
	 * <p>Dipakai sebagai gerbang eligibilitas nyata di {@code DetailKegiatan.getDiskonMahasiswaData()}
	 * dkk (mesin billing pusat): baris tagihan di luar rentang {@code semesterMulai}..{@code
	 * semesterSampai} menyebabkan tautan diskon dianggap tidak berlaku untuk baris itu.</p>
	 *
	 * @return semester mulai efektif; {@code 0} bila tidak ada batas bawah.
	 * @see #getSemesterSampai()
	 */
	public Integer getSemesterMulai() {
		jenisDiskonMahasiswa = getJenisDiskonMahasiswa();
		if (jenisDiskonMahasiswa != null && jenisDiskonMahasiswa.getSemesterMulai() != null) {
			semesterMulai = jenisDiskonMahasiswa.getSemesterMulai();
		}
		return semesterMulai == null ? 0 : semesterMulai;
	}

	/**
	 * @param semesterMulai batas bawah semester lokal baru (bisa tetap ditimpa oleh nilai jenis
	 *                      diskon saat dibaca via {@link #getSemesterMulai()}).
	 */
	public void setSemesterMulai(Integer semesterMulai) {
		this.semesterMulai = semesterMulai;
	}

	/**
	 * Batas atas semester berlakunya diskon ini — dimaksudkan meniru pola pewarisan {@link
	 * #getSemesterMulai()}, tetapi kondisi pemicunya keliru.
	 *
	 * <p><b>BUG &mdash; kondisi salah-field (asimetris dengan {@link #getSemesterMulai()}):</b>
	 * baris pertama method ini menguji {@code jenisDiskonMahasiswa.getSemesterMulai() != null}
	 * (bukan {@code getSemesterSampai() != null}) sebelum menimpa {@link #semesterSampai} dengan
	 * {@code jenisDiskonMahasiswa.getSemesterSampai()}. Konsekuensinya dua arah:</p>
	 * <ul>
	 * <li>Bila jenis diskon punya {@code semesterMulai} terisi tetapi {@code semesterSampai}
	 * bernilai {@code null} (artinya "tanpa batas atas" pada tingkat jenis diskon), kondisi
	 * bernilai {@code true} sehingga {@link #semesterSampai} ditimpa menjadi {@code null} —
	 * lalu fallback akhir method ini memaksakan batas <b>1</b> (untuk baris calon mahasiswa)
	 * atau <b>8</b> (untuk baris mahasiswa aktif) yang <b>tidak pernah dimaksudkan</b> oleh
	 * konfigurasi jenis diskonnya. Ini bisa membuat diskon tampak kedaluwarsa lebih cepat dari
	 * seharusnya.</li>
	 * <li>Bila sebaliknya jenis diskon punya {@code semesterMulai} kosong tetapi {@code
	 * semesterSampai} benar-benar diisi (mis. "berlaku sampai semester 4" tanpa batas bawah),
	 * kondisi bernilai {@code false} sehingga batas atas dari jenis diskon <b>tidak pernah
	 * dipropagasi</b> ke baris ini; field lokal {@link #semesterSampai} dipakai apa adanya
	 * (biasanya {@code null} untuk baris yang tak pernah diisi manual), lalu fallback akhir
	 * memberi <b>8</b> pada baris mahasiswa aktif — melampaui batas semester 4 yang sebenarnya
	 * dikonfigurasikan. Diskon bisa terus diberikan untuk semester-semester yang seharusnya
	 * sudah tidak berhak.</li>
	 * </ul>
	 * <p>Method ini juga tidak memanggil {@link #getJenisDiskonMahasiswa()} (yang meresolusi
	 * proxy lazy via {@code check()}) seperti {@link #getSemesterMulai()}, melainkan membaca
	 * field {@link #jenisDiskonMahasiswa} mentah — hanya aman selama getter batas-bawah sudah
	 * dipanggil lebih dulu pada instance yang sama (populer terjadi karena kedua getter selalu
	 * dipanggil berpasangan pada baris kode pemanggil, tetapi tidak dijamin oleh method ini
	 * sendiri). Dicatat apa adanya; tidak diperbaiki di sesi dokumentasi ini — lihat
	 * task hasil audit untuk detail dan rencana perbaikan.</p>
	 *
	 * @return semester sampai efektif; {@code 1} (baris calon mahasiswa) atau {@code 8} (baris
	 *         mahasiswa aktif) sebagai fallback bila nilai efektifnya {@code null} — termasuk
	 *         akibat bug di atas, bukan hanya karena benar-benar tidak dikonfigurasi.
	 * @see #getSemesterMulai()
	 */
	public Integer getSemesterSampai() {

		if (jenisDiskonMahasiswa != null && jenisDiskonMahasiswa.getSemesterMulai() != null) {
			semesterSampai = jenisDiskonMahasiswa.getSemesterSampai();
		}

		return semesterSampai == null ? (getBiodataCalonMahasiswa() != null ? 1 : 8) : semesterSampai;
	}

	/**
	 * @param semesterSampai batas atas semester lokal baru (bisa tetap ditimpa/tidak-ditimpa
	 *                       secara tidak konsisten oleh nilai jenis diskon saat dibaca via
	 *                       {@link #getSemesterSampai()} — lihat javadoc getter, bagian BUG).
	 */
	public void setSemesterSampai(Integer semesterSampai) {
		this.semesterSampai = semesterSampai;
	}

}
