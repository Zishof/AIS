package ais.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.ChecklistBaruPenilaianGuruOlehSiswa;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.Siswa;

/**
 * Kelas utilitas statis (tidak dapat diinstansiasi) untuk modul penilaian guru oleh siswa pada
 * aplikasi sekolah AIS ({@code ais.database.model.sekolah}), yang menjawab pertanyaan bisnis
 * inti: "apakah seorang siswa masih memiliki kewajiban mengisi checklist penilaian guru yang
 * belum diselesaikan untuk tahun ajaran/semester tertentu?".
 *
 * <p>
 * Kelas ini menyediakan dua method statis yang saling bergantung:
 * </p>
 * <ol>
 * <li>{@link #getJadwalPelajaranSiswa(Siswa, String, String)} — menghitung daftar id jadwal
 * pelajaran ({@code sekolah.jadwal_pelajaran}) yang relevan bagi seorang siswa pada tahun
 * ajaran/semester tertentu, mencakup baik jadwal kelas reguler ({@code kelas_punya_siswa})
 * maupun jadwal kelas les ({@code kelas_les_punya_siswa}), lewat SQL native gabungan
 * (subquery {@code IN}) yang hanya menyertakan keanggotaan kelas yang aktif.</li>
 * <li>{@link #checkStatusChecklistGuru(Siswa, String, String)} — memakai hasil method pertama
 * untuk menentukan, bagi setiap kombinasi (jadwal pelajaran x guru pengampu x checklist
 * penilaian guru aktif) yang berlaku, apakah SEMUA kombinasi tersebut sudah memiliki data
 * penilaian tersimpan ({@link ChecklistBaruPenilaianGuruOlehSiswa}) dari siswa yang
 * bersangkutan. Method ini mengembalikan {@code true} bila MASIH ADA kombinasi yang belum
 * diisi (artinya siswa masih punya kewajiban mengisi checklist), dan {@code false} bila semua
 * kombinasi sudah lengkap terisi (atau bila data prasyarat, seperti jadwal pelajaran/checklist
 * aktif, tidak ditemukan sama sekali).</li>
 * </ol>
 *
 * <p>
 * Kedua method membuka sesi Hibernate sendiri secara independen (bukan sesi thread-local dari
 * {@link HibernateUtil}) lewat {@code HibernateUtil.getSessionFactory().openSession()}, dan
 * SELALU menutupnya di blok {@code finally}; kegagalan apa pun ditangani secara "lunak" lewat
 * {@link Common#tampilErrorJikaAdmin(Exception)} (menampilkan detail error hanya untuk admin)
 * dan method mengembalikan nilai default yang aman (list kosong / {@code false}) alih-alih
 * melempar exception ke pemanggil — cocok untuk dipakai langsung dalam kondisi tampilan UI
 * (mis. menentukan apakah ikon peringatan checklist ditampilkan pada dashboard siswa) tanpa
 * perlu penanganan exception tambahan di sisi pemanggil.
 * </p>
 *
 * <p>
 * Constructor kelas ini sengaja diprivatkan ({@link #ChecklistPenilaianGuruHelper()}) karena
 * seluruh anggotanya statis dan kelas ini murni berperan sebagai kumpulan fungsi utilitas,
 * tidak pernah diinstansiasi.
 * </p>
 */
public class ChecklistPenilaianGuruHelper {

	/** Constructor privat — kelas ini murni kumpulan method statis dan tidak boleh diinstansiasi. */
	private ChecklistPenilaianGuruHelper() {
	}

	/**
	 * Mengambil daftar id jadwal pelajaran ({@code sekolah.jadwal_pelajaran}) yang relevan bagi
	 * seorang siswa, mencakup jadwal dari kelas reguler ({@code kelas_punya_siswa}) maupun
	 * kelas les ({@code kelas_les_punya_siswa}) yang keanggotaannya masih aktif, difilter
	 * opsional berdasarkan tahun ajaran dan ganjil/genap semester.
	 *
	 * @param siswa       siswa yang jadwal pelajarannya ingin diambil; bila {@code null} atau
	 *                    belum memiliki id, method langsung mengembalikan list kosong
	 * @param tahunAjaran nilai tahun ajaran untuk memfilter jadwal (mis. {@code "2025/2026"}),
	 *                    boleh {@code null}/kosong untuk tidak memfilter berdasarkan tahun ajaran
	 * @param ganjilGenap penanda semester ganjil/genap (dibandingkan terhadap
	 *                    {@link Perkuliahan#GANJIL} untuk menentukan sisa bagi 2 kolom
	 *                    {@code semester}), boleh {@code null}/kosong untuk tidak memfilter
	 *                    berdasarkan semester
	 * @return daftar id jadwal pelajaran yang cocok, terurut menaik; list kosong bila siswa
	 *         tidak valid, tidak ada jadwal yang cocok, atau terjadi kegagalan query (kegagalan
	 *         ditangani lewat {@link Common#tampilErrorJikaAdmin(Exception)}, tidak dilempar ke
	 *         pemanggil)
	 */
	@SuppressWarnings("unchecked")
	public static List<Long> getJadwalPelajaranSiswa(Siswa siswa, String tahunAjaran, String ganjilGenap) {
		List<Long> result = new ArrayList<Long>();
		if (siswa == null || siswa.getId() == null) {
			return result;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			StringBuilder sql = new StringBuilder();
			sql.append("select jp.id from sekolah.jadwal_pelajaran jp ");
			sql.append(" where jp.id is not null ");
			if (tahunAjaran != null && tahunAjaran.trim().length() > 0) {
				sql.append(" and jp.tahun_ajaran = :tahunAjaran ");
			}
			if (ganjilGenap != null && ganjilGenap.trim().length() > 0) {
				sql.append(" and (jp.semester % 2) = :semesterMod ");
			}
			sql.append(" and ( ");
			sql.append(" jp.kelas_id in (select a.kelas_id from sekolah.kelas_punya_siswa a ");
			sql.append(" where a.siswa_id = :siswaId and a.kelas_id is not null and (a.aktif=true or a.aktif is null) group by a.kelas_id) ");
			sql.append(" or jp.kelas_les_siswa in (select b.kelas_id from sekolah.kelas_les_punya_siswa b ");
			sql.append(" where b.siswa_id = :siswaId and b.kelas_id is not null and (b.aktif=true or b.aktif is null) group by b.kelas_id) ");
			sql.append(" ) group by jp.id order by jp.id ");

			org.hibernate.SQLQuery query = session.createSQLQuery(sql.toString());
			query.setParameter("siswaId", siswa.getId());
			if (tahunAjaran != null && tahunAjaran.trim().length() > 0) {
				query.setParameter("tahunAjaran", tahunAjaran);
			}
			if (ganjilGenap != null && ganjilGenap.trim().length() > 0) {
				query.setParameter("semesterMod", Perkuliahan.GANJIL.equals(ganjilGenap) ? Integer.valueOf(1)
						: Integer.valueOf(0));
			}

			List<Object> rows = query.list();
			if (rows != null) {
				for (Object row : rows) {
					if (row instanceof Number) {
						result.add(Long.valueOf(((Number) row).longValue()));
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChecklistPenilaianGuruHelper.java:73");
				}
			}
		}
		return result;
	}

	/**
	 * Menentukan apakah seorang siswa MASIH memiliki checklist penilaian guru yang belum
	 * diselesaikan untuk tahun ajaran/semester tertentu.
	 *
	 * <p>
	 * Alur kerja: (1) mengambil jadwal pelajaran relevan siswa lewat
	 * {@link #getJadwalPelajaranSiswa(Siswa, String, String)}; (2) mengambil id checklist
	 * penilaian guru yang aktif dan berlaku untuk siswa ({@code untukSiswa=true} pada angket
	 * induknya); (3) membaca kolom guru pengampu ({@code guru_id} s.d. {@code guru12_id}) dari
	 * setiap jadwal pelajaran yang relevan lewat SQL native, lalu membentuk himpunan kunci
	 * WAJIB berupa kombinasi {@code "<jadwalId>_<guruId>_<checklistId>"} untuk setiap pasangan
	 * (jadwal, guru pengampu, checklist aktif); (4) membaca data penilaian yang SUDAH tersimpan
	 * dari siswa ({@link ChecklistBaruPenilaianGuruOlehSiswa}) dan membentuk himpunan kunci
	 * TERISI dengan pola yang sama (nilai checklist diambil lewat
	 * {@link ChecklistBaruPenilaianGuruOlehSiswa#ambilValue()}); (5) mengembalikan {@code true}
	 * bila himpunan TERISI TIDAK mencakup seluruh himpunan WAJIB (artinya masih ada kombinasi
	 * yang belum dinilai siswa).
	 * </p>
	 *
	 * @param siswa       siswa yang statusnya ingin diperiksa; bila {@code null} atau belum
	 *                    memiliki id, method langsung mengembalikan {@code false}
	 * @param ganjilGenap penanda semester ganjil/genap, diteruskan ke
	 *                    {@link #getJadwalPelajaranSiswa(Siswa, String, String)}
	 * @param tahunAjaran nilai tahun ajaran, diteruskan ke
	 *                    {@link #getJadwalPelajaranSiswa(Siswa, String, String)}
	 * @return {@code true} bila masih ada kombinasi jadwal-guru-checklist yang wajib diisi
	 *         namun belum dinilai oleh siswa; {@code false} bila semua sudah lengkap, bila
	 *         tidak ada jadwal/checklist/guru yang berlaku, atau bila terjadi kegagalan query
	 *         (ditangani lewat {@link Common#tampilErrorJikaAdmin(Exception)}, tidak dilempar
	 *         ke pemanggil)
	 */
	@SuppressWarnings("unchecked")
	public static boolean checkStatusChecklistGuru(Siswa siswa, String ganjilGenap, String tahunAjaran) {
		if (siswa == null || siswa.getId() == null) {
			return false;
		}

		Session session = null;
		try {
			List<Long> jadwalPelajaranIds = getJadwalPelajaranSiswa(siswa, tahunAjaran, ganjilGenap);
			if (jadwalPelajaranIds == null || jadwalPelajaranIds.isEmpty()) {
				return false;
			}

			session = HibernateUtil.getSessionFactory().openSession();
			List<Long> checklistIds = session.createCriteria(ChecklistPenilaianGuru.class)
					.createAlias("grupChecklistPenilaianGuru", "grupChecklistPenilaianGuru")
					.createAlias("grupChecklistPenilaianGuru.angketPenilaianGuru", "angketPenilaianGuru")
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianGuru.aktif", true),
							Restrictions.isNull("grupChecklistPenilaianGuru.aktif")))
					.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.untukSiswa", true),
							Restrictions.isNull("angketPenilaianGuru.untukSiswa")))
					.setProjection(Projections.groupProperty("id")).list();

			if (checklistIds == null || checklistIds.isEmpty()) {
				return false;
			}

			String guruSql = "select jp.id, jp.guru_id, jp.guru2_id, jp.guru3_id, jp.guru4_id, jp.guru5_id, "
					+ "jp.guru6_id, jp.guru7_id, jp.guru8_id, jp.guru9_id, jp.guru10_id, jp.guru11_id, jp.guru12_id "
					+ "from sekolah.jadwal_pelajaran jp where jp.id in (:jadwalPelajaranIds)";
			List<Object[]> jadwalGuruRows = session.createSQLQuery(guruSql)
					.setParameterList("jadwalPelajaranIds", jadwalPelajaranIds).list();
			if (jadwalGuruRows == null || jadwalGuruRows.isEmpty()) {
				return false;
			}

			Set<String> requiredKeys = new HashSet<String>();
			for (Object[] row : jadwalGuruRows) {
				if (row == null || row.length == 0 || !(row[0] instanceof Number)) {
					continue;
				}
				Long jadwalId = Long.valueOf(((Number) row[0]).longValue());
				Set<Long> guruIds = new HashSet<Long>();
				for (int i = 1; i < row.length; i++) {
					if (row[i] instanceof Number) {
						guruIds.add(Long.valueOf(((Number) row[i]).longValue()));
					}
				}
				for (Long guruId : guruIds) {
					for (Long checklistId : checklistIds) {
						requiredKeys.add(jadwalId + "_" + guruId + "_" + checklistId);
					}
				}
			}
			if (requiredKeys.isEmpty()) {
				return false;
			}

			List<ChecklistBaruPenilaianGuruOlehSiswa> saved = session
					.createCriteria(ChecklistBaruPenilaianGuruOlehSiswa.class)
					.createAlias("jadwalPelajaran", "jadwalPelajaran")
					.add(Restrictions.eq("siswa", siswa))
					.add(Restrictions.in("jadwalPelajaran.id", jadwalPelajaranIds)).list();
			Set<String> savedKeys = new HashSet<String>();
			if (saved != null) {
				for (ChecklistBaruPenilaianGuruOlehSiswa data : saved) {
					if (data == null || data.getJadwalPelajaran() == null || data.getGuru() == null) {
						continue;
					}
					Long jadwalId = data.getJadwalPelajaran().getId();
					Long guruId = data.getGuru().getId();
					for (Object[] nilai : data.ambilValue()) {
						if (nilai != null && nilai.length > 0 && nilai[0] instanceof Long) {
							savedKeys.add(jadwalId + "_" + guruId + "_" + nilai[0]);
						}
					}
				}
			}

			return !savedKeys.containsAll(requiredKeys);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChecklistPenilaianGuruHelper.java:168");
				}
			}
		}
	}
}
