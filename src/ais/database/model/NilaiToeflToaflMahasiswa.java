package ais.database.model;

// Generated Apr 23, 20010 12:45:00 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import ais.common.LogicalUtil;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.nilai_toefl_toafl_mahasiswa} — nilai
 * <b>tes bahasa</b> mahasiswa dari salah satu dari DUA tes yang BERBEDA (bukan salah eja):
 * {@link #TOEFL} (<i>Test of English as a Foreign Language</i>, tes bahasa Inggris) atau {@link
 * #TOAFL} (<i>Test of Arabic as a Foreign Language</i>, tes bahasa Arab) — jenis tesnya
 * ditentukan oleh relasi {@link #getJenisToefl()}, bukan oleh dua konstanta {@link #TOEFL}/
 * {@link #TOAFL} itu sendiri (keduanya tidak tampak dirujuk langsung dari field/getter lain di
 * kelas ini; kemungkinan dipakai sebagai nilai pembanding oleh pemanggil luar).
 *
 * <p>Menyimpan hingga 4 sub-skor ({@link #getSkor1()}..{@link #getSkor4()}, mis. listening/
 * structure/reading/writing) dan nilai total yang DIHITUNG lewat rumus dinamis milik jenis
 * tesnya ({@link JenisToefl#getRumus()}, dievaluasi dengan pustaka exp4j) — lihat {@link
 * #getTotal()} untuk detail evaluasi dan fallback-nya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "nilai_toefl_toafl_mahasiswa")

public class NilaiToeflToaflMahasiswa extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 1995121656114539247L;
	/** Primary key baris {@code nilai_toefl_toafl_mahasiswa}, kolom {@code id} (identity, auto-generate). */
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

	/** Konstanta nama tes bahasa Inggris (<i>Test of English as a Foreign Language</i>). */
	public static final String TOEFL = "TOEFL";
	/** Konstanta nama tes bahasa Arab (<i>Test of Arabic as a Foreign Language</i>) — tes yang berbeda dari {@link #TOEFL}, bukan salah eja. */
	public static final String TOAFL = "TOAFL";

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
	 * Representasi ringkas untuk log/debug: identitas mahasiswa terkait.
	 *
	 * @return string representasi {@code mahasiswa} (via {@code toString()}-nya), atau
	 *         {@code "null"} bila tidak ada mahasiswa terkait
	 */
	public String toString() {
		return mahasiswa + "";
	}

	/** Mahasiswa pemilik nilai tes ini. */
	private Mahasiswa mahasiswa;
	/** Tanggal pelaksanaan tes; lazy default ke waktu sekarang bila belum diisi, lihat {@link #getTanggalTest()}. */
	private Date tanggalTest;
	/** Tanggal masa berlaku hasil tes ini berakhir. */
	private Date masaBerlaku;
	/** Nama jenis tes sebagai string (disalin dari {@link #jenisToefl} bila relasi itu terisi); lihat {@link #getJenisTest()}. */
	private String jenisTest;
	/** Jenis tes terstruktur (menentukan {@link #TOEFL}/{@link #TOAFL} beserta rumus perhitungan total). */
	private JenisToefl jenisToefl;
	/** Sub-skor ke-1 (mis. listening); default {@code 0.0} bila kosong. */
	private Double skor1;
	/** Sub-skor ke-2 (mis. structure); default {@code 0.0} bila kosong. */
	private Double skor2;
	/** Sub-skor ke-3 (mis. reading); default {@code 0.0} bila kosong. */
	private Double skor3;
	/** Sub-skor ke-4 (mis. writing); default {@code 0.0} bila kosong. */
	private Double skor4;
	/** Nilai total hasil evaluasi rumus {@link JenisToefl#getRumus()}; lihat {@link #getTotal()}. */
	private Double total;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public NilaiToeflToaflMahasiswa() {
	}

	/**
	 * @return primary key baris {@code nilai_toefl_toafl_mahasiswa}; {@code null} sebelum baris
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
	 * @return mahasiswa pemilik nilai tes ini; boleh {@code null}. Tidak memakai {@code check()}
	 *         untuk resolusi proxy lazy (berbeda dari beberapa entity lain di cluster ini yang
	 *         konsisten memakainya).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return this.mahasiswa;
	}

	/**
	 * @param mahasiswa mahasiswa pemilik baru; {@code null} untuk melepas tautan.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return tanggal pelaksanaan tes; waktu sekarang (dihitung sesaat, TIDAK ditulis balik ke
	 *         field) bila belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTest() {
		return tanggalTest == null ? ais.ui.util.WaktuUtil.getDate() : tanggalTest;
	}

	/**
	 * @param tanggalTest tanggal pelaksanaan tes baru.
	 */
	public void setTanggalTest(Date tanggalTest) {
		this.tanggalTest = tanggalTest;
	}

	/**
	 * @return sub-skor ke-1; {@code 0.0} bila belum diisi.
	 */
	@Column(name = "skor1")
	public Double getSkor1() {
		return skor1 == null ? 0.0 : skor1;
	}

	/**
	 * @param skor1 sub-skor ke-1 baru.
	 */
	public void setSkor1(Double skor1) {
		this.skor1 = skor1;
	}

	/**
	 * @return sub-skor ke-2; {@code 0.0} bila belum diisi.
	 */
	@Column(name = "skor2")
	public Double getSkor2() {
		return skor2 == null ? 0.0 : skor2;
	}

	/**
	 * @param skor2 sub-skor ke-2 baru.
	 */
	public void setSkor2(Double skor2) {
		this.skor2 = skor2;
	}

	/**
	 * @return sub-skor ke-3; {@code 0.0} bila belum diisi.
	 */
	@Column(name = "skor3")
	public Double getSkor3() {
		return skor3 == null ? 0.0 : skor3;
	}

	/**
	 * @param skor3 sub-skor ke-3 baru.
	 */
	public void setSkor3(Double skor3) {
		this.skor3 = skor3;
	}

	/**
	 * @return sub-skor ke-4; {@code 0.0} bila belum diisi.
	 */
	@Column(name = "skor4")
	public Double getSkor4() {
		return skor4 == null ? 0.0 : skor4;
	}

	/**
	 * @param skor4 sub-skor ke-4 baru.
	 */
	public void setSkor4(Double skor4) {
		this.skor4 = skor4;
	}

	/**
	 * Menghitung nilai total dari keempat sub-skor.
	 *
	 * <p><b>Jalur rumus dinamis:</b> bila {@link #getJenisToefl()} tidak {@code null} dan
	 * rumusnya ({@link JenisToefl#getRumus()}) tidak kosong, rumus itu di-parse dan dievaluasi
	 * dengan pustaka exp4j, dengan variabel {@code skor1}..{@code skor4} diisi dari getter
	 * masing-masing. Sebelum evaluasi, ekspresi divalidasi lewat {@code e.validate()} — hasil
	 * validasi hanya DICATAT ke {@code System.out}/{@code ErrorAuditUtil} (bukan menghentikan
	 * evaluasi), sehingga rumus yang tidak valid tetap dicoba dievaluasi apa adanya.</p>
	 * <p><b>Fallback rata-rata sederhana:</b> bila {@link #getJenisToefl()} {@code null}, rumus
	 * kosong, ATAU evaluasi rumus melempar exception apa pun, nilai total dihitung sebagai
	 * rata-rata sederhana keempat sub-skor ({@code (skor1+skor2+skor3+skor4)/4.0}).</p>
	 * <p><b>Efek samping:</b> field {@link #total} SELALU ditulis ulang pada setiap pemanggilan
	 * (baik dari hasil evaluasi rumus maupun fallback rata-rata) — nilai yang pernah diset
	 * manual lewat {@link #setTotal(Double)} akan tertimpa begitu getter ini dipanggil sekali
	 * saja. Beberapa jalur mencetak ke {@code System.out} (bukan logger terstruktur).</p>
	 *
	 * @return nilai total hasil rumus dinamis atau rata-rata sederhana (lihat di atas)
	 */
	public Double getTotal() {
		if (jenisToefl != null && !jenisToefl.getRumus().isEmpty()) {
			try {
				Set<String> strings = new HashSet<String>();
				strings.add("skor1");
				strings.add("skor2");
				strings.add("skor3");
				strings.add("skor4");
				Expression e = new ExpressionBuilder(jenisToefl.getRumus()).variables(strings)
						.functions(LogicalUtil.ALL_FUNCTION).operator(LogicalUtil.ALL_OPERATOR).build();
				e.setVariable("skor1", getSkor1());
				e.setVariable("skor2", getSkor2());
				e.setVariable("skor3", getSkor3());
				e.setVariable("skor4", getSkor4());

				try {
					boolean valid = e.validate().isValid();
					System.out.println(" valid => " + valid);
					if (!valid) {
						List<String> d = e.validate().getErrors();
						if (!d.isEmpty()) {
							String ds = "";
							for (String dd : d) {
								ds += ds.isEmpty() ? dd : ".\n" + dd;
							}
							System.out.println("error = " + ds);
						}
					}
				} catch (Exception ee) {
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/database/model/NilaiToeflToaflMahasiswa.java:196");
				}

				total = e.evaluate();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/NilaiToeflToaflMahasiswa.java:201");
				total = (getSkor1() + getSkor2() + getSkor3() + getSkor4()) / 4.0;
			}
		} else {
			total = (getSkor1() + getSkor2() + getSkor3() + getSkor4()) / 4.0;
		}
		return total;
	}

	/**
	 * @param total nilai total baru; bisa tetap ditimpa oleh hasil evaluasi rumus/rata-rata
	 *              saat dibaca via {@link #getTotal()} — lihat javadoc getter tersebut.
	 */
	public void setTotal(Double total) {
		this.total = total;
	}

	/**
	 * @return tanggal masa berlaku hasil tes ini berakhir; boleh {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMasaBerlaku() {
		return masaBerlaku;
	}

	/**
	 * @param masaBerlaku tanggal masa berlaku baru.
	 */
	public void setMasaBerlaku(Date masaBerlaku) {
		this.masaBerlaku = masaBerlaku;
	}

	/**
	 * @return jenis tes terstruktur (menentukan apakah ini {@link #TOEFL} atau {@link #TOAFL}
	 *         beserta rumus perhitungan totalnya); boleh {@code null}. Tidak memakai {@code
	 *         check()} untuk resolusi proxy lazy.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_toefl", nullable = true)
	public JenisToefl getJenisToefl() {
		return jenisToefl;
	}

	/**
	 * @param jenisToefl jenis tes baru; {@code null} untuk melepas tautan (jalur rumus dinamis
	 *                   di {@link #getTotal()} akan jatuh ke fallback rata-rata sederhana).
	 */
	public void setJenisToefl(JenisToefl jenisToefl) {
		this.jenisToefl = jenisToefl;
	}

	/**
	 * Nama jenis tes sebagai string, untuk kemudahan tampilan/pencarian tanpa join.
	 *
	 * <p><b>Getter yang menulis balik (disinkronkan dari relasi):</b> bila {@link
	 * #getJenisToefl()} (field mentah {@link #jenisToefl}, bukan getter) tidak {@code null},
	 * field {@link #jenisTest} DITIMPA dengan {@code jenisToefl.getNama()} setiap kali getter
	 * ini dipanggil — nilai yang pernah diset manual lewat {@link #setJenisTest(String)}
	 * tertimpa selama relasi {@link #jenisToefl} terisi.</p>
	 *
	 * @return nama jenis tes efektif (dari relasi bila tersedia, atau field lokal bila tidak
	 *         ada relasi); boleh {@code null} bila keduanya kosong.
	 */
	@Column(name = "jenistest")
	public String getJenisTest() {
		if (jenisToefl != null) {
			jenisTest = jenisToefl.getNama();
		}
		return jenisTest;
	}

	/**
	 * @param jenisTest nama jenis tes baru untuk field lokal (bisa tetap ditimpa oleh nama dari
	 *                  relasi {@link #jenisToefl} saat dibaca via {@link #getJenisTest()}).
	 */
	public void setJenisTest(String jenisTest) {
		this.jenisTest = jenisTest;
	}

}
