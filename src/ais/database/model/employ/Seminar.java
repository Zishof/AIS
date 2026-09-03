package ais.database.model.employ;

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Entity JPA/Hibernate yang memetakan tabel {@code public.seminar}: mencatat satu baris riwayat
 * keikutsertaan seorang {@link Pegawai} dalam sebuah seminar — judul, rentang tanggal
 * pelaksanaan, lokasi, pembicara utama, peran pegawai dalam seminar ({@link #getSebagai()}), dan
 * status persetujuan.
 *
 * <p><b>Berbeda dari {@link Diklat}: kelas ini SENDIRI adalah entity riwayat personal.</b> Tidak
 * ada kelas terpisah bernama {@code RiwayatSeminarPegawai} — {@code Seminar} langsung berperan
 * sebagai baris "riwayat seminar pegawai", lengkap dengan relasi {@code @ManyToOne} wajib
 * ({@code nullable = false}) ke {@link Pegawai} dan fallback ke pengguna saat ini di
 * {@link #getPegawai()} — pola yang identik dengan klaster "riwayat pegawai" lain di paket ini
 * (mis. {@link RiwayatPelatihanPegawai}), meski nama kelasnya sendiri tidak berawalan
 * {@code Riwayat...}. Dikelola lewat
 * {@code ais.action.master.employ.helper.RiwayatSeminarPegawaiHelper}, yang memuatnya dengan
 * filter satuan kerja hierarkis/pegawai/status persetujuan dan menyediakan lampiran dokumen lewat
 * {@code ais.database.model.file.LampiranLain} (bukan {@code FotoLampiranPegawai} seperti pada
 * {@link RiwayatPelatihanPegawai}).
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see RiwayatPelatihanPegawai
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "seminar")



public class Seminar extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap dari kelas ini.
	 */
	private static final long serialVersionUID = 7230309753082900385L;

	/** Primary key baris seminar, dibangkitkan otomatis oleh database (IDENTITY). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini — field audit shadow, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini — field audit shadow, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang tercatat terakhir kali mengubah baris seminar ini. Field
	 * audit shadow murni tekstual (bukan foreign key), tidak divalidasi terhadap tabel pengguna;
	 * hanya untuk jejak audit tampilan, bukan sumber kebenaran untuk otorisasi.
	 *
	 * @return id pengguna terakhir yang mengubah, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Setter ini mengabaikan diam-diam nilai {@code null}
	 * atau string kosong/whitespace-only — tidak bisa dipakai untuk mengosongkan kembali nilai
	 * yang sudah tersimpan.
	 *
	 * @param olehId id pengguna pengubah; nilai null/kosong diabaikan tanpa efek.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Catatan/keterangan bebas mengenai baris seminar ini; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Mengisi nama pengguna pengubah terakhir; berlaku aturan pengabaian null/kosong yang sama
	 * dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; nilai null/kosong diabaikan tanpa efek.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang tercatat terakhir kali mengubah baris seminar ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA {@code @PreUpdate}, dipanggil otomatis oleh Hibernate sebelum
	 * statement UPDATE dieksekusi, mendelegasikan pencatatan stempel waktu ke
	 * {@code AuditTimestampInterceptor.ubah(this)}. Field {@link #tanggal_dirubah} juga
	 * diinisialisasi eager saat konstruksi objek lewat {@code WaktuUtil.getDate()}, sehingga ada
	 * dua jalur penulisan: nilai awal saat objek dibuat, dan nilai yang ditimpa otomatis tepat
	 * sebelum UPDATE.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengatur stempel waktu perubahan terakhir secara manual, memotong jalur otomatis di
	 * {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris seminar ini.
	 *
	 * @return tanggal-waktu perubahan terakhir (tipe {@code TIMESTAMP} di database).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string default entity ini, mengembalikan field {@link #keterangan} apa
	 * adanya. Dapat mengembalikan {@code null} bila {@link #keterangan} belum diisi — pemanggil
	 * yang menggabungkan hasilnya ke {@link String} lain sebaiknya berjaga-jaga terhadap
	 * kemungkinan literal {@code "null"} yang muncul.
	 *
	 * @return isi field {@link #keterangan}, dapat berupa {@code null}.
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengembalikan primary key baris seminar ini, dibangkitkan otomatis oleh database lewat
	 * strategi {@link javax.persistence.GenerationType#IDENTITY}.
	 *
	 * @return id baris, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id baris seminar secara manual. Kolom {@code id} dipetakan {@code insertable =
	 * false} sehingga pengisian di sini tidak terbawa ke statement INSERT.
	 *
	 * @param id id baris yang ingin diset.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan/keterangan bebas mengenai baris seminar ini.
	 *
	 * @return isi keterangan, dapat berupa {@code null}.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan/keterangan bebas mengenai baris seminar ini.
	 *
	 * @param keterangan teks keterangan baru, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Pegawai pemilik baris riwayat seminar ini; lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Judul seminar yang diikuti; lihat {@link #getJudulSeminar()}. */
	private String judulSeminar;
	/** Tanggal mulai pelaksanaan seminar; lihat {@link #getTanggalMulai()}. */
	private Date tanggalMulai;
	/** Tanggal selesai pelaksanaan seminar; lihat {@link #getTanggalSelesai()}. */
	private Date tanggalSelesai;
	/** Lokasi pelaksanaan seminar; lihat {@link #getLokasi()}. */
	private String lokasi;
	/** Nama pembicara utama seminar; lihat {@link #getPembicaraUtama()}. */
	private String pembicaraUtama;
	/** Peran pegawai dalam seminar (mis. peserta, pemateri, moderator); lihat {@link #getSebagai()}. */
	private String sebagai;
	/** Flag status persetujuan baris riwayat seminar ini; default {@code false}, lihat {@link #getStatus()}. */
	private Boolean status = false;

	/**
	 * Constructor default tanpa argumen, dibutuhkan oleh spesifikasi JPA/Hibernate untuk
	 * instansiasi reflektif entity ini.
	 */
	public Seminar() {
	}

	/**
	 * Mengembalikan {@link Pegawai} pemilik baris riwayat seminar ini, dengan resolusi proxy
	 * lewat {@code check()} lalu fallback ke {@link Common#getCurrentUser()}{@code .getPegawai()}
	 * bila hasilnya masih {@code null}, dibungkus try/catch yang menelan seluruh exception ke
	 * {@code ErrorAuditUtil.record(...)} (pola audit tangkapan-kosong yang sama dipakai di seluruh
	 * klaster "riwayat pegawai", lihat {@link RiwayatPelatihanPegawai#getPegawai()}). Fallback ini
	 * berisiko keliru bila dipanggil dari konteks admin/HR yang sedang mengedit riwayat seminar
	 * pegawai lain sementara field {@link #pegawai} belum ter-set — hasil fallback akan
	 * menunjuk ke pegawai milik pengguna yang sedang login, bukan pegawai target.
	 *
	 * <p>Relasi {@code @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}},
	 * {@code fetch = FetchType.LAZY}, kolom join {@code pegawai} {@code nullable = false}.
	 *
	 * @return pegawai pemilik riwayat seminar ini, atau {@code null} bila resolusi dan fallback
	 *         keduanya gagal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/Seminar.java:120");

		}

		return pegawai;
	}

	/**
	 * Mengisi field {@link Pegawai} pemilik baris riwayat seminar ini secara langsung, tanpa
	 * validasi.
	 *
	 * @param pegawai pegawai pemilik baris riwayat seminar ini.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan judul seminar yang diikuti pada baris riwayat ini.
	 *
	 * @return judul seminar, dapat berupa {@code null}.
	 */
	@Column(name = "judul_seminar")
	public String getJudulSeminar() {
		return judulSeminar;
	}

	/**
	 * Mengisi judul seminar pada baris riwayat ini.
	 *
	 * @param judulSeminar judul seminar baru.
	 */
	public void setJudulSeminar(String judulSeminar) {
		this.judulSeminar = judulSeminar;
	}

	/**
	 * Mengembalikan tanggal mulai pelaksanaan seminar pada baris riwayat ini.
	 *
	 * @return tanggal mulai seminar.
	 */
	@Column(name = "tanggal_mulai")
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	/**
	 * Mengisi tanggal mulai pelaksanaan seminar pada baris riwayat ini.
	 *
	 * @param tanggalMulai tanggal mulai baru.
	 */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Mengembalikan tanggal selesai pelaksanaan seminar pada baris riwayat ini. Tidak ada
	 * validasi di level model yang memastikan tanggal ini tidak mendahului
	 * {@link #getTanggalMulai()} — konsistensi rentang tanggal sepenuhnya bergantung pada lapisan
	 * pemanggil (helper/action).
	 *
	 * @return tanggal selesai seminar.
	 */
	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Mengisi tanggal selesai pelaksanaan seminar pada baris riwayat ini.
	 *
	 * @param tanggalSelesai tanggal selesai baru.
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Mengembalikan lokasi pelaksanaan seminar pada baris riwayat ini.
	 *
	 * @return lokasi seminar, dapat berupa {@code null}.
	 */
	@Column(name = "lokasi")
	public String getLokasi() {
		return lokasi;
	}

	/**
	 * Mengisi lokasi pelaksanaan seminar pada baris riwayat ini.
	 *
	 * @param lokasi lokasi seminar baru.
	 */
	public void setLokasi(String lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan nama pembicara utama seminar pada baris riwayat ini. Kolom database bernama
	 * {@code pemicara_utama} (tanpa huruf "b" pada "pembicara") — typo ejaan yang sudah tertanam
	 * di skema; nama field/getter/setter Java tetap dieja benar ({@code pembicaraUtama}), hanya
	 * nilai {@code name} pada anotasi {@code @Column} yang mencerminkan nama kolom asli di
	 * database. Tidak diubah di sini karena mengubah nama kolom memerlukan migrasi skema, di luar
	 * cakupan dokumentasi.
	 *
	 * @return nama pembicara utama, dapat berupa {@code null}.
	 */
	@Column(name = "pemicara_utama")
	public String getPembicaraUtama() {
		return pembicaraUtama;
	}

	/**
	 * Mengisi nama pembicara utama seminar pada baris riwayat ini.
	 *
	 * @param pembicaraUtama nama pembicara utama baru.
	 */
	public void setPembicaraUtama(String pembicaraUtama) {
		this.pembicaraUtama = pembicaraUtama;
	}

	/**
	 * Mengembalikan peran pegawai dalam seminar ini (mis. sebagai peserta, pemateri, moderator,
	 * atau panitia) — teks bebas tanpa daftar nilai baku (bukan enum/relasi ke tabel referensi).
	 *
	 * @return peran pegawai dalam seminar, dapat berupa {@code null}.
	 */
	@Column(name = "sebagai")
	public String getSebagai() {
		return sebagai;
	}

	/**
	 * Mengisi peran pegawai dalam seminar ini.
	 *
	 * @param sebagai peran baru.
	 */
	public void setSebagai(String sebagai) {
		this.sebagai = sebagai;
	}

	/**
	 * Mengembalikan flag status persetujuan baris riwayat seminar ini, dengan null-guard yang
	 * menuliskan ulang field ke {@code false} bila bernilai {@code null} sebelum
	 * mengembalikannya — pola yang sama dipakai di seluruh klaster "riwayat pegawai" (lihat
	 * {@link RiwayatPelatihanPegawai#getStatus()}). Status {@code true} (disetujui) membuat form
	 * di {@code RiwayatSeminarPegawaiHelper} dibekukan dan tombol hapus disembunyikan.
	 *
	 * @return status persetujuan baris riwayat seminar ini, tidak pernah {@code null}.
	 */
	public Boolean getStatus() {
		if (status == null) {
			status = false;
		}
		return status;
	}

	/**
	 * Mengisi flag status persetujuan baris riwayat seminar ini secara langsung, tanpa
	 * null-guard.
	 *
	 * @param status status baru.
	 */
	public void setStatus(Boolean status) {
		this.status = status;
	}

}
