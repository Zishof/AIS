package ais.database.model.kkn;

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

import ais.database.model.Kkn;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;

/**
 * Entity <b>jawaban mahasiswa atas satu butir syarat pendaftaran KKN</b> pada tabel
 * {@code public.mahasiswa_kkn_persyaratan}. Satu baris = satu mahasiswa menjawab satu
 * {@link PersyaratanKkn} untuk satu gelaran {@link Kkn} — berbeda dari {@link MahasiswaDaftarKkn}
 * (yang merekam status seleksi keseluruhan pendaftaran), kelas ini merekam <b>rincian per
 * butir syarat</b>: nilai jawabannya (salah satu dari {@link #getNilaiString()},
 * {@link #getNilaiTanggal()}, {@link #getNilaiNumber()}, {@link #getNilaiBoolean()} — dipilih
 * sesuai {@link PersyaratanKkn#getTipeDataInputan()} syarat terkait) dan {@link #getStatus()}
 * apakah jawaban tersebut dinyatakan memenuhi syarat.
 *
 * <h3>Presisi bug default SKS/IPK pada "Syarat Lain" (dicatat, TIDAK ditambal di sini)</h3>
 * <p>Diverifikasi ulang dari sisi entity ini: {@link #getStatus()} <b>default {@code true}</b>
 * (dianggap MEMENUHI syarat) saat field {@link #status} belum pernah diisi eksplisit —
 * <i>fail-open</i>, bukan fail-closed. Dikombinasikan dengan default {@code nilaiDataInputan = ""}
 * pada {@link PersyaratanKkn#getNilaiDataInputan()} (lihat catatan di kelas tersebut), ini adalah
 * pola yang membuat syarat ambang batas (SKS/IPK minimal via "Syarat Lain") berpotensi meloloskan
 * pendaftar yang belum pernah dievaluasi sama sekali: baris {@code MahasiswaKknPersyaratan} yang
 * belum diisi status-nya akan tampak "memenuhi syarat" secara default, bukan "belum dievaluasi".
 * Ini KONFIRMASI ULANG dari sisi model, bukan temuan baru — sudah tercatat di memori proyek
 * sebelumnya dan sengaja TIDAK ditambal pada sesi dokumentasi ini.</p>
 *
 * <h3>Kembaran modul PKL</h3>
 * <p>Struktur kelas ini identik dengan {@link ais.database.model.pkl.MahasiswaPklPersyaratan} —
 * tidak ada satu pun divergensi field/method yang ditemukan antara keduanya selain penggantian
 * nama Kkn&rarr;Pkl.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "mahasiswa_kkn_persyaratan")

public class MahasiswaKknPersyaratan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris jawaban syarat ini. */
	private Long id;
	/** Nama/username pengubah terakhir; diisi lewat {@link #setOleh(String)} oleh lapisan audit. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi lewat {@link #setOlehId(String)} oleh lapisan audit. */
	private String olehId;

	/**
	 * @return id pengguna (bukan nama tampilan) yang terakhir mengubah baris ini, atau {@code null}
	 *         bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/blank
	 * diabaikan diam-diam (early return) — nilai lama tetap dipertahankan.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/blank
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama tampilan pengubah terakhir. Nilai {@code null} atau blank diabaikan diam-diam.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null}/blank
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama tampilan pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat sebelum
	 * {@code UPDATE} dikirim ke basis data, memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu saat ini pada konstruksi objek. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; biasanya diset otomatis oleh
	 *                        {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini; diperbarui otomatis oleh
	 *         {@link #onUpdate()} setiap kali baris diperbarui di basis data.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #getNama()} langsung dari field mentah — representasi teks ringkas baris ini. */
	public String toString() {
		return nama;
	}

	/** Nama tampilan baris jawaban syarat ini; boleh {@code null} (berbeda dari entity "Punya", kolom ini tidak wajib di sini). */
	private String nama;
	/** Catatan/keterangan bebas untuk baris ini; boleh {@code null}. */
	private String keterangan;
	/** Gelaran KKN terkait jawaban syarat ini. Wajib diisi (kolom {@code NOT NULL}). */
	private Kkn kkn;
	/** Mahasiswa yang menjawab syarat ini. Wajib diisi (kolom {@code NOT NULL}). */
	private Mahasiswa mahasiswa;
	/** Butir syarat katalog yang dijawab. Wajib diisi (kolom {@code NOT NULL}). */
	private PersyaratanKkn persyaratanKkn;
	/** Penanda apakah jawaban ini dinyatakan memenuhi syarat. Default {@code true} (fail-open) bila belum diisi — lihat catatan bug pada javadoc kelas. */
	private Boolean status;

	/** Nilai jawaban bertipe teks; dipakai bila {@code persyaratanKkn.tipeDataInputan} = {@link PersyaratanKkn#TEXT}/{@link PersyaratanKkn#TEXT_ANGKA}. */
	private String nilaiString;
	/** Nilai jawaban bertipe tanggal; dipakai bila {@code persyaratanKkn.tipeDataInputan} = {@link PersyaratanKkn#TANGGAL}. */
	private Date nilaiTanggal;
	/** Nilai jawaban bertipe numerik; dipakai bila {@code persyaratanKkn.tipeDataInputan} = {@link PersyaratanKkn#ANGKA}/{@link PersyaratanKkn#TEXT_ANGKA} (mis. SKS/IPK aktual mahasiswa). */
	private Double nilaiNumber;
	/** Nilai jawaban bertipe ya/tidak; dipakai bila {@code persyaratanKkn.tipeDataInputan} = {@link PersyaratanKkn#PILIHAN_YA_TIDAK}. */
	private Boolean nilaiBoolean;

	/**
	 * @return gelaran {@link Kkn} terkait jawaban syarat ini. <b>Tidak</b> memakai pembungkus
	 *         {@code check(...)} sebelum dikembalikan (berbeda dari pola di entity "Punya"
	 *         sepaket) — pemanggil yang mengakses proxy ini di luar sesi Hibernate yang masih
	 *         terbuka berisiko {@code LazyInitializationException}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kkn", nullable = false)
	public Kkn getKkn() {
		return kkn;
	}

	/** @param kkn gelaran KKN terkait jawaban syarat ini. */
	public void setKkn(Kkn kkn) {
		this.kkn = kkn;
	}

	/** Konstruktor kosong wajib bagi Hibernate (dipakai lewat refleksi saat memuat entity). */
	public MahasiswaKknPersyaratan() {
	}

	/**
	 * @return primary key baris jawaban syarat ini, di-generate basis data ({@code IDENTITY});
	 *         {@code null} sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key baris jawaban syarat ini. Kolom dipetakan {@code insertable = false}
	 *           sehingga pengisian di sini tidak berpengaruh pada {@code INSERT}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama tampilan baris ini, di-trim; {@code null} bila field {@link #nama} belum pernah diisi. Kolom ini {@code nullable = true} (berbeda dari entity "Punya" yang mewajibkannya). */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama tampilan baris ini; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan/keterangan bebas baris ini, apa adanya tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk baris ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return mahasiswa yang menjawab syarat ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/** @param mahasiswa mahasiswa yang menjawab syarat ini. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return butir syarat katalog ({@link PersyaratanKkn}) yang dijawab oleh baris ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "persyaratan_kkn", nullable = false)
	public PersyaratanKkn getPersyaratanKkn() {
		return persyaratanKkn;
	}

	/** @param persyaratanKkn butir syarat katalog yang dijawab. */
	public void setPersyaratanKkn(PersyaratanKkn persyaratanKkn) {
		this.persyaratanKkn = persyaratanKkn;
	}

	/**
	 * @return {@code true} bila jawaban syarat ini dinyatakan memenuhi syarat. Bila field
	 *         {@link #status} belum pernah diisi, method ini menuliskannya (efek samping) dengan
	 *         default {@code true} — <b>fail-open</b>: baris yang belum pernah dievaluasi tampak
	 *         seolah sudah memenuhi syarat, bukan "belum dievaluasi"/"tidak memenuhi". Lihat catatan
	 *         bug pada javadoc kelas untuk analisis dampaknya terhadap syarat SKS/IPK via
	 *         "Syarat Lain".
	 */
	@Column(name = "status")
	public Boolean getStatus() {
		if (status == null) {
			status = true;
		}
		return status;
	}

	/** @param status {@code true} bila jawaban ini dinyatakan memenuhi syarat. */
	public void setStatus(Boolean status) {
		this.status = status;
	}

	/** @return nilai jawaban bertipe teks, atau {@code null} bila syarat ini bukan bertipe teks/belum dijawab. */
	public String getNilaiString() {
		return nilaiString;
	}

	/** @param nilaiString nilai jawaban bertipe teks. */
	public void setNilaiString(String nilaiString) {
		this.nilaiString = nilaiString;
	}

	/** @return nilai jawaban bertipe tanggal, atau {@code null} bila syarat ini bukan bertipe tanggal/belum dijawab. */
	public Date getNilaiTanggal() {
		return nilaiTanggal;
	}

	/** @param nilaiTanggal nilai jawaban bertipe tanggal. */
	public void setNilaiTanggal(Date nilaiTanggal) {
		this.nilaiTanggal = nilaiTanggal;
	}

	/**
	 * @return nilai jawaban bertipe numerik (mis. SKS/IPK aktual mahasiswa untuk syarat ambang
	 *         batas), atau {@code null} bila syarat ini bukan bertipe numerik/belum dijawab. Ini
	 *         adalah field tempat nilai AKTUAL mahasiswa dibandingkan terhadap ambang batas di
	 *         {@link PersyaratanKkn#getNilaiDataInputan()} — perbandingannya sendiri dilakukan di
	 *         lapisan lain (di luar 14 file paket {@code kkn}/{@code pkl}), bukan di sini.
	 */
	public Double getNilaiNumber() {
		return nilaiNumber;
	}

	/** @param nilaiNumber nilai jawaban bertipe numerik. */
	public void setNilaiNumber(Double nilaiNumber) {
		this.nilaiNumber = nilaiNumber;
	}

	/** @return nilai jawaban bertipe ya/tidak, atau {@code null} bila syarat ini bukan bertipe pilihan ya/tidak/belum dijawab. */
	public Boolean getNilaiBoolean() {
		return nilaiBoolean;
	}

	/** @param nilaiBoolean nilai jawaban bertipe ya/tidak. */
	public void setNilaiBoolean(Boolean nilaiBoolean) {
		this.nilaiBoolean = nilaiBoolean;
	}

}
