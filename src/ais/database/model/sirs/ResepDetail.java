package ais.database.model.sirs;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <b>Baris resep</b>: satu obat yang diresepkan pada satu lembar {@link Resep}. Kelas inilah yang
 * menjadi titik temu antara resep obat jadi dan resep racikan, lewat sepasang FK alternatif:
 * <ul>
 * <li>{@link #getItem()} — obat jadi ({@link ItemMedis}) yang diserahkan apa adanya kepada pasien;</li>
 * <li>{@link #getRacikan()} — formula {@link Racikan} yang harus dicampur/diracik apoteker terlebih
 * dahulu, dengan komponen-komponennya tersimpan pada {@link RacikanDetail}.</li>
 * </ul>
 * Dengan struktur ini, satu lembar resep dapat memuat campuran baris obat jadi dan baris racikan;
 * <b>racikan bukan jenis resep tersendiri, melainkan isi salah satu baris resep</b>.
 *
 * <h3>Kedua FK tidak dipaksa saling eksklusif</h3>
 * Kolom {@code item} dan {@code racikan} keduanya {@code nullable} dan tidak ada
 * <i>check constraint</i> maupun validasi di lapisan model yang memastikan tepat satu di antaranya
 * terisi. Tiga keadaan janggal karena itu dapat tersimpan tanpa penolakan:
 * <ul>
 * <li><b>Keduanya kosong</b> — baris resep tanpa obat sama sekali; baris semacam ini tetap
 * terhitung sebagai baris resep namun tidak menghasilkan apa pun saat pelayanan apotik.</li>
 * <li><b>Keduanya terisi</b> — baris menunjuk obat jadi dan formula racikan sekaligus; obat mana
 * yang sebenarnya diserahkan bergantung pada urutan pemeriksaan di kode pemanggil, bukan pada
 * model.</li>
 * <li><b>{@code resep} kosong</b> — kolom induk juga {@code nullable}, sehingga baris resep yatim
 * (tidak melekat pada lembar resep mana pun) dapat tersimpan dan tidak akan pernah muncul lagi di
 * layar resep.</li>
 * </ul>
 *
 * <h3>Pasangan FK dua arah yang independen dengan {@link Racikan}</h3>
 * {@link Racikan} juga menyimpan FK balik {@link Racikan#getResepDetail()} ke baris resep yang
 * memakainya. Kedua kolom — {@code sirs.resep_detail.racikan} dan {@code sirs.racikan.resep_detail}
 * — adalah kolom terpisah tanpa {@code mappedBy}, sehingga masing-masing dapat ditulis sendiri dan
 * berpotensi saling bertentangan (baris resep menunjuk racikan A sementara racikan A menunjuk
 * baris resep lain). Tidak ada penjaga apa pun untuk itu di lapisan model; kode pemanggil wajib
 * menulis kedua sisi secara konsisten.
 *
 * <h3>Tanggal baris versus tanggal resep</h3>
 * {@link #getTanggal()} menyimpan waktu per baris, bukan per lembar. {@link Resep} sendiri tidak
 * memiliki kolom tanggal sama sekali, sehingga tanggal efektif suatu resep hanya dapat disimpulkan
 * dari baris-barisnya. Karena field ini diinisialisasi ke waktu pembuatan objek Java dan bukan ke
 * tanggal kunjungan/diagnosa, baris yang ditambahkan belakangan ke lembar resep lama akan membawa
 * tanggal hari penambahan — bukan tanggal resep diterbitkan.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — {@link #getItem()} dan {@link #getRacikan()} memanggil
 * {@code check(...)} milik {@link GeneralValueObject}, yang memaksa materialisasi proxy malas
 * Hibernate lalu menulis hasilnya balik ke field; keduanya karena itu bukan operasi murni-baca.
 * {@link #getResep()} sebaliknya murni-baca dan memakai {@link FetchMode#SELECT}.</li>
 * <li><b>Field audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, dan {@link #onUpdate()} adalah infrastruktur audit
 * ({@code AuditTimestampInterceptor} + Hibernate Envers), keharusan teknis dan bukan cacat.</li>
 * <li><b>Tanpa sumbu tenant</b> — sesuai keterbatasan seluruh modul {@code sirs}.</li>
 * </ul>
 *
 * @see Resep lembar resep yang menjadi induk baris ini
 * @see Racikan formula racikan yang dapat dirujuk baris ini
 * @see ItemMedis obat jadi yang dapat dirujuk baris ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "resep_detail")
public class ResepDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.resep_detail}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris resep ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir; nilai kosong/spasi diabaikan agar jejak audit
	 * tidak terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;

	/**
	 * Representasi teks baris resep untuk komponen ZK, memakai field {@link #keterangan} langsung.
	 * Menghasilkan {@code null} bila keterangan belum diisi.
	 *
	 * @return keterangan baris, dapat {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; nilai kosong/spasi diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris resep ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir; normalnya diisi otomatis oleh interceptor audit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini. Jangan dikacaukan dengan
	 * {@link #getTanggal()}: yang ini cap audit teknis, yang itu tanggal domain baris resep.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Obat jadi yang diresepkan pada baris ini; alternatif dari {@link #racikan}. */
	private ItemMedis item;

	/** Formula racikan yang diresepkan pada baris ini; alternatif dari {@link #item}. */
	private Racikan racikan;

	/** Lembar resep induk baris ini; kolomnya {@code nullable} sehingga baris yatim mungkin terjadi. */
	private Resep resep;

	/** Tanggal baris resep, diinisialisasi ke waktu pembuatan objek Java. */
	private Date tanggal = new Date();

	/** Kuantitas obat yang diresepkan pada baris ini. */
	private Double jumlah = 0.0;

	/** Keterangan bebas baris, mis. aturan pakai; sekaligus label {@link #toString()}. */
	private String keterangan;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public ResepDetail() {
	}

	/**
	 * Mengembalikan kunci utama baris resep.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya untuk kerangka kerja persistensi atau saat menyalin
	 * entitas menjadi baris baru.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris resep (mis. aturan pakai).
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas baris resep.
	 *
	 * @param keterangan aturan pakai atau catatan lain
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan obat jadi yang diresepkan pada baris ini. Tidak ada penjaga yang mengosongkan
	 * {@link #setRacikan(Racikan)} secara otomatis, sehingga kedua FK dapat terisi bersamaan.
	 *
	 * @param item obat jadi, boleh {@code null} bila baris ini berisi racikan
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengembalikan obat jadi yang diresepkan pada baris ini. <b>Getter destruktif</b>: memanggil
	 * {@code check(...)} yang memaksa materialisasi proxy malas Hibernate dan menulis hasilnya
	 * balik ke field.
	 *
	 * @return obat jadi, atau {@code null} bila baris ini berisi racikan (atau tidak berisi apa pun)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menautkan baris ini ke lembar resep induknya.
	 *
	 * @param resep lembar resep induk; mengosongkannya menjadikan baris yatim
	 */
	public void setResep(Resep resep) {
		this.resep = resep;
	}

	/**
	 * Mengembalikan lembar resep induk baris ini. Getter ini murni-baca dan memakai
	 * {@link FetchMode#SELECT}.
	 *
	 * @return lembar resep induk, atau {@code null} bila baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "resep", nullable = true)
	public Resep getResep() {
		return resep;
	}

	/**
	 * Mengisi kuantitas obat yang diresepkan pada baris ini.
	 *
	 * @param jumlah kuantitas, boleh {@code null}
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan kuantitas obat pada baris ini. Berbeda dari getter numerik serupa di
	 * {@link RacikanDetail}, getter ini <b>tidak</b> menormalkan {@code null}, sehingga dapat
	 * mengembalikan {@code null} untuk baris yang dimuat dari basis data dengan kolom kosong.
	 * Pemanggil yang melakukan aritmetika langsung perlu memeriksanya sendiri.
	 *
	 * @return kuantitas obat, dapat {@code null}
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Menetapkan formula racikan yang diresepkan pada baris ini. Perlu diingat bahwa
	 * {@link Racikan#setResepDetail(ResepDetail)} adalah kolom FK terpisah yang harus ikut ditulis
	 * agar kedua arah relasi tetap konsisten.
	 *
	 * @param racikan formula racikan, boleh {@code null} bila baris ini berisi obat jadi
	 */
	public void setRacikan(Racikan racikan) {
		this.racikan = racikan;
	}

	/**
	 * Mengembalikan formula racikan yang diresepkan pada baris ini. <b>Getter destruktif</b>
	 * (lihat {@link #getItem()}).
	 *
	 * @return formula racikan, atau {@code null} bila baris ini berisi obat jadi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "racikan", nullable = true)
	public Racikan getRacikan() {
		racikan = check(racikan);
		return racikan;
	}

	/**
	 * Mengisi tanggal baris resep.
	 *
	 * @param tanggal tanggal baris resep
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan tanggal baris resep. Karena {@link Resep} tidak memiliki kolom tanggal, nilai
	 * inilah satu-satunya penanda waktu domain pada susunan resep — dan nilainya adalah waktu
	 * pembuatan objek baris, bukan tanggal resep diterbitkan.
	 *
	 * @return tanggal baris resep
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal;
	}

}
