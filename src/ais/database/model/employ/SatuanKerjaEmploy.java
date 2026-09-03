package ais.database.model.employ;

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
import ais.database.model.Pegawai;



/**
 * Model data untuk daftar <b>satuan kerja</b> khusus modul employ — daftar <b>datar</b> (tanpa
 * hierarki parent/level) yang dikelola lewat {@code SatuanKerjaAction} dan DAO
 * {@code getSatuanKerjaEmployDao()}. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar
 * spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 *
 * <p><b>Jangan tertukar dengan dua entity "satuan kerja" lain</b> (lihat catatan lengkap di
 * {@link UnitKerja}): {@code SatuanKerjaEmploy} (kelas ini) memetakan tabel {@code
 * employ.satuan_kerja_employ} dan strukturnya (nama/keterangan/jenisPimpinan/pimpinan/
 * jabatanStruktural/prioritas) hampir identik dengan field non-pohon {@link UnitKerja} — kuat
 * dugaan kelas ini adalah predecessor historis {@link UnitKerja} sebelum struktur pohon
 * (parent/level/deep) ditambahkan, dipertahankan untuk kompatibilitas data lama, <b>bukan</b>
 * alias maupun sinonim dari {@link UnitKerja} atau dari {@code ais.database.model.rab.SatuanKerja}
 * (entity satuan kerja anggaran/tenant lintas-modul di paket berbeda).</p>
 *
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String
 * oleh}, {@code String olehId}, {@code String nama}, {@code String keterangan}, {@code
 * JenisPimpinan jenisPimpinan}, {@code Pegawai pimpinan}, {@code JabatanStruktural
 * jabatanStruktural}, {@code Integer prioritas}, {@code Date tanggal_dirubah}; pemetaan
 * persistence: tabel {@code employ.satuan_kerja_employ}; pembacaan/pencarian ({@code
 * getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code
 * getNama()}, {@code getKeterangan()}, {@code getJenisPimpinan()}, {@code getPimpinan()}, {@code
 * getJabatanStruktural()}, {@code getPrioritas()}); mutasi data ({@code setOlehId()}, {@code
 * onUpdate()}, {@code setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code
 * setNama()}, {@code setKeterangan()}, {@code setJenisPimpinan()}, {@code setPimpinan()}, {@code
 * setJabatanStruktural()}, {@code setPrioritas()}); operasi domain lain ({@code toString()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value
 * object di memori. Relasi {@code @ManyToOne} dipetakan {@code @Fetch(FetchMode.SELECT)} tanpa
 * panggilan {@code check(...)} eksplisit di getter, sama seperti {@link UnitKerja}. Persistence,
 * transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see UnitKerja
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "satuan_kerja_employ")



public class SatuanKerjaEmploy extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris
	 * satuan kerja ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang diwarisi
	 * pola generiknya dari {@link GeneralValueObject}.
	 *
	 * @return id pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan secara
	 * diam-diam (tidak melempar exception, tidak mengubah state).
	 *
	 * @param olehId id pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menetapkan nama pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan
	 * secara diam-diam, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris
	 * satuan kerja ini.
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memutakhirkan
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan tanggal terakhir baris ini dirubah. Biasanya diisi otomatis oleh
	 * {@link #onUpdate()}, bukan dipanggil manual.
	 *
	 * @param tanggal_dirubah tanggal perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan tanggal terakhir baris ini dirubah. Nilai awalnya (sebelum pernah di-update)
	 * diinisialisasi ke waktu saat object dibuat, lewat {@code WaktuUtil.getDate()}.
	 *
	 * @return tanggal perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks satuan kerja ini: mengembalikan {@link #getNama()} apa adanya. Dipakai di
	 * combobox/label pemilihan satuan kerja pada UI.
	 *
	 * @return nama satuan kerja
	 */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;
	private JenisPimpinan jenisPimpinan;
	private Pegawai pimpinan;
	private JabatanStruktural jabatanStruktural;
	private Integer prioritas;

	/**
	 * Konstruktor tanpa argumen yang dipersyaratkan Hibernate/JPA untuk instansiasi entity lewat
	 * reflection. Tidak menginisialisasi field lain di luar default Java.
	 */
	public SatuanKerjaEmploy() {
	}

	/**
	 * Mengembalikan primary key baris satuan kerja ini.
	 *
	 * @return id baris, atau {@code null} bila belum persisten
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Kolom dipetakan {@code insertable = false} (nilai dihasilkan
	 * database via {@code IDENTITY}), jadi setter ini praktis hanya dipakai saat memuat ulang
	 * entity dari hasil query.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama satuan kerja, di-trim (whitespace di awal/akhir dibuang) setiap kali
	 * dibaca.
	 *
	 * @return nama satuan kerja hasil trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama satuan kerja. Nilai disimpan apa adanya; trim baru terjadi saat dibaca lewat
	 * {@link #getNama()}.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk satuan kerja ini.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jenis pimpinan yang berlaku untuk satuan kerja ini.
	 *
	 * @return jenis pimpinan, atau {@code null} bila tidak diset
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_pimpinan", nullable = true)
	public JenisPimpinan getJenisPimpinan() {
		return jenisPimpinan;
	}

	/**
	 * Menetapkan jenis pimpinan.
	 *
	 * @param jenisPimpinan jenis pimpinan baru
	 */
	public void setJenisPimpinan(JenisPimpinan jenisPimpinan) {
		this.jenisPimpinan = jenisPimpinan;
	}

	/**
	 * Mengembalikan pegawai yang menjabat sebagai pimpinan satuan kerja ini.
	 *
	 * @return pegawai pimpinan, atau {@code null} bila belum ditunjuk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pimpinan", nullable = true)
	public Pegawai getPimpinan() {
		return pimpinan;
	}

	/**
	 * Menetapkan pegawai pimpinan satuan kerja.
	 *
	 * @param pimpinan pegawai pimpinan baru
	 */
	public void setPimpinan(Pegawai pimpinan) {
		this.pimpinan = pimpinan;
	}

	/**
	 * Mengembalikan jabatan struktural yang melekat pada posisi pimpinan satuan kerja ini.
	 *
	 * @return jabatan struktural, atau {@code null} bila tidak diset
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_struktural", nullable = true)
	public JabatanStruktural getJabatanStruktural() {
		return jabatanStruktural;
	}

	/**
	 * Menetapkan jabatan struktural.
	 *
	 * @param jabatanStruktural jabatan struktural baru
	 */
	public void setJabatanStruktural(JabatanStruktural jabatanStruktural) {
		this.jabatanStruktural = jabatanStruktural;
	}

	/**
	 * Mengembalikan urutan prioritas tampil satuan kerja ini di antara satuan kerja lain.
	 *
	 * @return prioritas, atau {@code null} bila tidak diset
	 */
	@Column(name = "prioritas")
	public Integer getPrioritas() {
		return prioritas;
	}

	/**
	 * Menetapkan urutan prioritas tampil.
	 *
	 * @param prioritas prioritas baru
	 */
	public void setPrioritas(Integer prioritas) {
		this.prioritas = prioritas;
	}

}
