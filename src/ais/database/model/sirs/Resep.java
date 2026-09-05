package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * Entitas <b>lembar resep</b> pada modul SIRS: kepala (header) dari satu resep obat yang
 * diterbitkan untuk satu diagnosa penyakit. Isinya sangat ringkas — hanya {@link #getKode()},
 * {@link #getDiagnosaPenyakit()}, dan {@link #getKeterangan()} — karena seluruh substansi resep
 * berada pada baris-baris {@link ResepDetail}.
 *
 * <h3>Resep, ResepDetail, dan Racikan</h3>
 * Ketiganya membentuk satu susunan tiga lapis yang mudah salah dipahami:
 * <ul>
 * <li>{@code Resep} — lembar resep (kepala). Satu baris per diagnosa.</li>
 * <li>{@link ResepDetail} — baris resep. Satu baris per obat yang diresepkan. Baris ini memiliki
 * <b>dua FK alternatif</b>: {@link ResepDetail#getItem()} untuk obat jadi yang diserahkan apa
 * adanya, dan {@link ResepDetail#getRacikan()} untuk obat yang harus diracik apoteker.</li>
 * <li>{@link Racikan} — formula racikan, entitas berdiri sendiri dengan tabelnya sendiri
 * ({@code sirs.racikan}) dan komponen-komponennya di {@link RacikanDetail}.</li>
 * </ul>
 * Jadi <b>{@code Racikan} bukan sub-tipe atau varian dari {@code Resep}</b>, dan bukan pula
 * "resep jenis racik". Ia adalah isi salah satu baris resep. Satu lembar resep dapat memuat
 * campuran baris obat jadi dan baris racikan sekaligus. Perlu diperhatikan pula bahwa
 * {@link Racikan} menyimpan FK balik {@link Racikan#getResepDetail()} yang independen dari
 * {@link ResepDetail#getRacikan()} — dua kolom terpisah tanpa {@code mappedBy}, sehingga kedua
 * sisi harus ditulis konsisten oleh kode pemanggil.
 *
 * <h3>Kaitan ke pasien bersifat tidak langsung</h3>
 * Entitas ini <b>tidak memiliki FK ke {@link Pasien} maupun ke transaksi/pendaftaran</b>.
 * Keterkaitan dengan pasien seluruhnya diperantarai {@link DiagnosaPenyakit}: satu lembar resep
 * dicari dan dibuat berdasarkan diagnosa (lihat
 * {@code ais.action.master.sirs.detail.ResepHelper}), dan diagnosa itulah yang membawa konteks
 * pasien beserta kunjungannya. Akibat langsung dari desain ini:
 * <ul>
 * <li>Kueri "seluruh resep milik pasien X" tidak dapat dilakukan langsung atas tabel ini; ia harus
 * melewati {@code sirs.diagnosa_penyakit} terlebih dahulu.</li>
 * <li>Kolom {@code diagnosa_penyakit} bersifat {@code nullable}, sehingga lembar resep tanpa
 * diagnosa dapat tersimpan tanpa ditolak basis data. Resep semacam itu menjadi <b>yatim</b>: tidak
 * dapat ditelusuri ke pasien mana pun, dan tidak akan muncul lagi pada layar resep karena layar
 * itu selalu mencari berdasarkan diagnosa.</li>
 * <li>Tidak ada penjaga yang membatasi satu diagnosa hanya boleh memiliki satu lembar resep.
 * {@code ResepHelper} memang mencari resep yang sudah ada terlebih dahulu dan hanya membuat baru
 * bila tidak ditemukan, tetapi pencarian-lalu-buat itu tidak dilindungi indeks unik pada kolom
 * {@code diagnosa_penyakit}, sehingga dua sesi yang berjalan bersamaan atas diagnosa yang sama
 * dapat menghasilkan dua lembar resep — dan lembar yang kalah beserta seluruh barisnya menjadi
 * tidak terlihat oleh layar.</li>
 * </ul>
 *
 * <h3>Pembuatan malas dan penomoran kode</h3>
 * {@code ResepHelper} membuat lembar resep secara <i>lazy</i>: objek {@link Resep} baru benar-benar
 * disimpan ketika baris obat atau racikan pertama ditambahkan, bukan saat layar dibuka. Kodenya
 * dibangkitkan {@code Common.generateCode(Resep.class, 8, "RSP")}. Karena kolom {@link #getKode()}
 * tidak memiliki indeks unik dan tidak ada penjaga tabrakan kode di lapisan model, keunikan kode
 * sepenuhnya bergantung pada pembangkit tersebut.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Field audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, dan {@link #onUpdate()} adalah infrastruktur audit
 * ({@code AuditTimestampInterceptor} + Hibernate Envers lewat {@link Audited}); keharusan teknis,
 * bukan data domain.</li>
 * <li><b>Tanpa sumbu tenant</b> — seperti seluruh modul {@code sirs}, tidak ada kolom satuan kerja
 * sehingga isolasi antar unit tidak dapat ditegakkan di lapisan model.</li>
 * <li>Tidak ada koleksi {@code ResepDetail} yang dipetakan di sini; relasi hanya satu arah dari
 * sisi anak, sehingga penghapusan lembar resep tidak meng-<i>cascade</i> ke baris-barisnya.</li>
 * </ul>
 *
 * @see ResepDetail baris resep (obat jadi atau racikan)
 * @see Racikan formula racikan yang dapat dirujuk salah satu baris resep
 * @see DiagnosaPenyakit diagnosa yang menjadi satu-satunya jalan menuju konteks pasien
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "resep")
public class Resep extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.resep}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah lembar resep ini.
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
	 * Representasi teks lembar resep untuk komponen ZK, memakai field {@link #kode} langsung.
	 *
	 * @return kode resep; dapat {@code null} bila kode belum dibangkitkan
	 */
	public String toString() {
		return kode;
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
	 * Mengembalikan nama pengguna yang terakhir mengubah lembar resep ini.
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
	 * Mengembalikan cap waktu perubahan terakhir lembar resep ini.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode resep, wajib diisi; dibangkitkan {@code Common.generateCode} dengan awalan "RSP". */
	private String kode;

	/** Diagnosa penyakit yang mendasari resep ini; satu-satunya jalan menuju konteks pasien. */
	private DiagnosaPenyakit diagnosaPenyakit;

	/** Keterangan bebas atas lembar resep. */
	private String keterangan;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public Resep() {
	}

	/**
	 * Mengembalikan kunci utama lembar resep.
	 *
	 * @return id lembar resep, atau {@code null} bila belum pernah disimpan
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
	 * @param id kunci utama lembar resep
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode resep.
	 *
	 * @return kode resep (kolom wajib, maksimal 50 karakter)
	 */
	@Column(name = "kode", nullable = false, length = 50)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Mengisi kode resep. Tidak ada penjaga tabrakan kode di lapisan model dan kolomnya tidak
	 * berindeks unik, sehingga keunikan bergantung sepenuhnya pada pembangkit
	 * {@code Common.generateCode}.
	 *
	 * @param kode kode resep
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan keterangan bebas lembar resep.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas lembar resep.
	 *
	 * @param keterangan catatan bebas atas resep
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menautkan lembar resep ini ke diagnosa penyakit yang mendasarinya. Karena diagnosa adalah
	 * satu-satunya kaitan menuju pasien, mengosongkan nilai ini menjadikan resep yatim dan tidak
	 * dapat ditelusuri lagi lewat layar resep.
	 *
	 * @param diagnosaPenyakit diagnosa yang mendasari resep, sebaiknya tidak {@code null}
	 */
	public void setDiagnosaPenyakit(DiagnosaPenyakit diagnosaPenyakit) {
		this.diagnosaPenyakit = diagnosaPenyakit;
	}

	/**
	 * Mengembalikan diagnosa penyakit yang mendasari lembar resep ini. Getter ini murni-baca
	 * (tidak memanggil {@code check(...)} seperti sebagian relasi lain di modul ini); relasi
	 * diambil dengan {@link FetchMode#SELECT}.
	 *
	 * @return diagnosa penyakit, atau {@code null} bila resep yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "diagnosa_penyakit", nullable = true)
	public DiagnosaPenyakit getDiagnosaPenyakit() {
		return diagnosaPenyakit;
	}

}
