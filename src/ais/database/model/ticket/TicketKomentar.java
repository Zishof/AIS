package ais.database.model.ticket;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Komentar / pesan pada sebuah {@link Ticket} — thread interaksi antara developer↔pengguna maupun
 * pengguna↔pengguna. {@link #internal} = true menandai catatan yang hanya boleh dilihat pengelola
 * (developer/admin), bukan pengaju. Lampiran pesan memakai {@code LampiranLain} (jenis
 * "TICKET_KOMENTAR", ref = id komentar). Tabel {@code public.ticket_komentar} dibuat otomatis
 * ({@code hbm2ddl=update}).
 *
 * <p><b>Enforcement visibilitas.</b> Kelas entity ini hanya menyimpan flag {@link #internal};
 * penyaringan komentar internal dari pengaju non-pengelola dilakukan di layer controller
 * (mis. {@code NewUiTicketController}), yang membandingkan flag ini dengan hak kelola pengguna
 * sebelum komentar/lampirannya ditampilkan atau diunduh. Kelas ini sendiri tidak menegakkan
 * pembatasan tersebut.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ticket_komentar")
public class TicketKomentar extends GeneralValueObject {

	private static final long serialVersionUID = 3120250724003L;

	/** ID baris (primary key, auto-increment). */
	private Long id;
	/** Tiket induk tempat komentar ini diposting. */
	private Ticket ticket;
	/** Isi teks komentar/pesan. */
	private String isi;
	/** ID pengguna yang menulis komentar (lintas jenis akun, sama seperti pengaju pada {@link Ticket}). */
	private String userId;
	/** Nama penulis komentar, disimpan sebagai snapshot untuk tampilan cepat. */
	private String nama;
	/** Jenis/tipe akun penulis komentar (mis. pegawai, dosen, siswa, dst). */
	private String tipePengguna;
	/** Penanda catatan internal: {@code true} berarti hanya boleh dilihat pengelola, bukan pengaju. */
	private Boolean internal;
	/** Waktu komentar diposting; default saat instansiasi objek, sebelum benar-benar disimpan. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Nama pengguna terakhir yang mengubah baris ini (field audit bayangan dari {@code GeneralValueObject}). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (field audit bayangan dari {@code GeneralValueObject}). */
	private String olehId;
	/** Waktu perubahan terakhir; diperbarui otomatis oleh {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default (dibutuhkan Hibernate). */
	public TicketKomentar() {
	}

	/**
	 * Mengambil ID baris (primary key).
	 *
	 * @return ID komentar, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Mengatur ID baris. Biasanya tidak dipanggil manual karena kolom {@code id} auto-increment
	 * dan {@code insertable = false}.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil tiket induk tempat komentar ini diposting.
	 *
	 * @return {@link Ticket} induk, boleh {@code null} bila belum diset.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "ticket", nullable = true)
	public Ticket getTicket() {
		return ticket;
	}

	/**
	 * Mengatur tiket induk tempat komentar ini diposting.
	 *
	 * @param ticket tiket induk baru.
	 */
	public void setTicket(Ticket ticket) {
		this.ticket = ticket;
	}

	/**
	 * Mengambil isi teks komentar/pesan.
	 *
	 * @return isi komentar, boleh {@code null}.
	 */
	@Column(name = "isi", nullable = true, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	/**
	 * Mengatur isi teks komentar/pesan.
	 *
	 * @param isi isi komentar baru.
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * Mengambil ID pengguna penulis komentar.
	 *
	 * @return ID penulis, boleh {@code null}.
	 */
	@Column(name = "user_id", nullable = true, length = 128)
	public String getUserId() {
		return userId;
	}

	/**
	 * Mengatur ID pengguna penulis komentar.
	 *
	 * @param userId ID penulis baru.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Mengambil nama penulis komentar (snapshot, tidak bergantung lookup akun).
	 *
	 * @return nama penulis, boleh {@code null}.
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return nama;
	}

	/**
	 * Mengatur nama penulis komentar.
	 *
	 * @param nama nama penulis baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil jenis/tipe akun penulis komentar.
	 *
	 * @return tipe pengguna, boleh {@code null}.
	 */
	@Column(name = "tipe_pengguna", nullable = true, length = 64)
	public String getTipePengguna() {
		return tipePengguna;
	}

	/**
	 * Mengatur jenis/tipe akun penulis komentar.
	 *
	 * @param tipePengguna tipe pengguna baru.
	 */
	public void setTipePengguna(String tipePengguna) {
		this.tipePengguna = tipePengguna;
	}

	/**
	 * Mengambil penanda catatan internal. Tidak menulis balik ke field {@link #internal} — hanya
	 * mengembalikan {@code false} secara sementara bila field {@code null} (default bukan
	 * internal/terlihat semua pihak).
	 *
	 * @return {@code true} bila komentar ini hanya boleh dilihat pengelola.
	 */
	@Column(name = "internal", nullable = true)
	public Boolean getInternal() {
		return internal == null ? false : internal;
	}

	/**
	 * Mengatur penanda catatan internal.
	 *
	 * @param internal status internal baru.
	 */
	public void setInternal(Boolean internal) {
		this.internal = internal;
	}

	/**
	 * Mengambil waktu komentar diposting.
	 *
	 * @return tanggal komentar.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = true)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Mengatur waktu komentar diposting.
	 *
	 * @param tanggal tanggal komentar baru.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengambil nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, boleh {@code null} bila belum pernah diubah.
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengatur nama pengguna terakhir yang mengubah baris ini. Nilai kosong/blank diabaikan
	 * (tidak menimpa nilai lama) agar riwayat "oleh" tidak hilang akibat pemanggilan dengan nilai
	 * kosong secara tidak sengaja.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna pengubah, boleh {@code null} bila belum pernah diubah.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengatur ID pengguna terakhir yang mengubah baris ini. Nilai kosong/blank diabaikan (tidak
	 * menimpa nilai lama), sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengambil waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengatur waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah tanggal perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Callback JPA {@code @PreUpdate} — dipanggil otomatis oleh Hibernate sebelum setiap
	 * {@code UPDATE}, mendelegasikan pencatatan {@link #oleh}/{@link #olehId}/
	 * {@link #tanggal_dirubah} ke {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Representasi string ringkas komentar, dipakai untuk debugging/log.
	 *
	 * @return string berformat {@code "<id>-<nama>"} ({@code nama} kosong bila {@code null}).
	 */
	public String toString() {
		return id + "-" + (nama == null ? "" : nama);
	}
}
