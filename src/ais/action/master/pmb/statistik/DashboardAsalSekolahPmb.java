package ais.action.master.pmb.statistik;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;

import ais.database.model.Jenjang;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyHtml;

/**
 * <h1>DashboardAsalSekolahPmb — Profil Asal Sekolah Calon Mahasiswa</h1>
 *
 * <p>Menjawab pertanyaan: <em>"Dari sekolah mana saja mahasiswa kita datang?"</em>
 * Informasi ini sangat berguna untuk:</p>
 * <ul>
 *   <li><b>Perencanaan promosi</b> — sekolah yang sudah banyak mengirimkan lulusan
 *       perlu dipertahankan relasi kedekatannya; sekolah yang belum perlu dikunjungi.</li>
 *   <li><b>Analisis jenis sekolah</b> — apakah lebih banyak dari SMA, SMK, atau MA?
 *       Berguna untuk penyesuaian kurikulum bridging dan fasilitas pendukung.</li>
 *   <li><b>Peta sebaran geografis sekolah</b> — propinsi asal sekolah menunjukkan
 *       radius rekrutmen yang efektif saat ini.</li>
 *   <li><b>Angkatan kelulusan</b> — distribusi tahun kelulusan menunjukkan apakah
 *       banyak calon yang merupakan lulusan baru atau sudah gap year beberapa tahun.</li>
 * </ul>
 *
 * <p>Data diambil dari field {@code namaSekolahAsal} (entity master), {@code jenisSekolah},
 * {@code propinsiSekolah}, dan {@code tahunKelulusan} di {@code BiodataCalonMahasiswa}.</p>
 *
 * @see DashboardPmbBase
 */
public class DashboardAsalSekolahPmb extends DashboardPmbBase {

	// ═══════════════════════════════════════════════════════════════
	// Inner data classes
	// ═══════════════════════════════════════════════════════════════

	private static final class NamaData {
		String nama;
		int    jumlah;
		NamaData(String n, int j) { nama = n; jumlah = j; }
	}

	// ═══════════════════════════════════════════════════════════════
	// Konstanta
	// ═══════════════════════════════════════════════════════════════

	private static final int MAX_SEKOLAH    = 10;
	private static final int MAX_PROPINSI   = 8;
	private static final int MAX_ABBREV     = 22;

	// ═══════════════════════════════════════════════════════════════
	// Konstruktor
	// ═══════════════════════════════════════════════════════════════

	public DashboardAsalSekolahPmb(PerguruanTinggi pt) {
		super(pt);
	}

	// ═══════════════════════════════════════════════════════════════
	// doRefresh
	// ═══════════════════════════════════════════════════════════════

	@Override
	protected void doRefresh(Session session, String ta, String sem) {
		Jenjang jenjang = getSelectedJenjang();
		List<NamaData> sekolahList   = queryTopSekolah(session, ta, sem, jenjang);
		List<NamaData> jenisSekolah  = queryJenisSekolah(session, ta, sem, jenjang);
		List<NamaData> propinsiList  = queryPropinsiSekolah(session, ta, sem, jenjang);
		List<NamaData> lulusanList   = queryTahunLulusan(session, ta, sem, jenjang);

		// Hitung total sekolah unik
		int totalSekolah = countSekolahUnik(session, ta, sem, jenjang);

		StringBuilder sb = new StringBuilder(5120);

		// Header
		sb.append("<div style=\"margin-bottom:14px;\">")
		  .append("<div style=\"font-size:18px;font-weight:700;color:#1e3a5f;margin-bottom:3px;\">")
		  .append("Profil Asal Sekolah &#8212; ").append(escHtml(ta));
		if (jenjang != null) {
			sb.append(" &#8212; ").append(escHtml(jenjang.getNama()));
		}
		sb.append("</div>")
		  .append("<div style=\"font-size:12px;color:#9ca3af;\">")
		  .append("Dari mana calon mahasiswa berasal: sekolah, jenis sekolah, daerah asal sekolah, dan tahun kelulusan.")
		  .append("</div></div>");

		// KPI
		String topSekolah  = sekolahList.isEmpty() ? "-" : sekolahList.get(0).nama;
		String topJenis    = jenisSekolah.isEmpty() ? "-" : jenisSekolah.get(0).nama;
		String topPropinsi = propinsiList.isEmpty() ? "-" : propinsiList.get(0).nama;
		sb.append(HtmlChartHelper.kpiCards(
				new String[] { "Sekolah Unik", "Sekolah Terbanyak", "Jenis Terbanyak", "Propinsi Terbanyak" },
				new String[] { fmtAngka(totalSekolah), abbrev(topSekolah, 18),
						abbrev(topJenis, 18), abbrev(topPropinsi, 18) },
				new String[] { "data tercatat", "", "", "" },
				new String[] { "", "", "", "" },
				new boolean[] { true, true, true, true },
				new String[] { "#1877f2", "#059669", "#7c3aed", "#f59e0b" }));

		// Top sekolah asal (full width)
		if (!sekolahList.isEmpty()) {
			int n = sekolahList.size();
			String[] lbl = new String[n];
			double[] val = new double[n];
			for (int i = 0; i < n; i++) {
				lbl[i] = sekolahList.get(i).nama;
				val[i] = sekolahList.get(i).jumlah;
			}
			sb.append(fullWidth(HtmlChartHelper.barHorizontal(
					"10 Sekolah Asal Terbanyak",
					"Sekolah yang paling banyak mengirimkan lulusan untuk mendaftar ke kampus kita. "
					+ "Hubungan dengan sekolah-sekolah ini perlu terus dijaga dan diperkuat.",
					lbl, val, "#2563eb")));
		}

		// Jenis sekolah + propinsi sekolah
		String donutJenis;
		if (!jenisSekolah.isEmpty()) {
			int n = jenisSekolah.size();
			String[] lbl = new String[n];
			double[] val = new double[n];
			String[] colors = { "#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6" };
			for (int i = 0; i < n; i++) {
				lbl[i] = jenisSekolah.get(i).nama;
				val[i] = jenisSekolah.get(i).jumlah;
			}
			String[] usedColors = new String[n];
			for (int i = 0; i < n; i++) { usedColors[i] = colors[i % colors.length]; }
			donutJenis = HtmlChartHelper.donut(
					"Komposisi Jenis Sekolah",
					"Perbandingan jumlah calon dari SMA, SMK, MA, atau jenis sekolah lainnya. "
					+ "Berguna untuk menyesuaikan program bridging di awal kuliah.",
					lbl, val, usedColors, "");
		} else {
			donutJenis = emptyCard("Komposisi Jenis Sekolah", "Belum ada data jenis sekolah.");
		}

		String barPropinsi;
		if (!propinsiList.isEmpty()) {
			int n = propinsiList.size();
			String[] lbl = new String[n];
			double[] val = new double[n];
			for (int i = 0; i < n; i++) {
				lbl[i] = propinsiList.get(i).nama;
				val[i] = propinsiList.get(i).jumlah;
			}
			barPropinsi = HtmlChartHelper.barHorizontal(
					"8 Propinsi Asal Sekolah Terbanyak",
					"Dari propinsi mana saja sekolah asal calon mahasiswa berasal. "
					+ "Propinsi yang belum banyak terwakili adalah potensi pasar yang bisa dikembangkan.",
					lbl, val, "#059669");
		} else {
			barPropinsi = emptyCard("Propinsi Asal Sekolah", "Belum ada data propinsi sekolah.");
		}
		sb.append(grid2col(donutJenis, barPropinsi));

		// Tahun kelulusan
		if (!lulusanList.isEmpty()) {
			int n = lulusanList.size();
			String[] cats  = new String[n];
			double[][] vals = new double[n][1];
			for (int i = 0; i < n; i++) {
				cats[i]    = lulusanList.get(i).nama;
				vals[i][0] = lulusanList.get(i).jumlah;
			}
			sb.append(fullWidth(HtmlChartHelper.barVerticalGrouped(
					"Distribusi Tahun Kelulusan",
					"Tahun berapa calon mahasiswa lulus dari sekolah menengah. "
					+ "Batang paling tinggi menunjukkan angkatan yang paling banyak mendaftar. "
					+ "Banyaknya pendaftar dari tahun lama bisa berarti ada segmen lulusan yang belum berkuliah.",
					cats,
					new String[] { "Jumlah" },
					vals,
					new String[] { "#6366f1" })));
		}

		new MyHtml(sb.toString()).setParent(contentHolder);
	}

	// ═══════════════════════════════════════════════════════════════
	// Query methods
	// ═══════════════════════════════════════════════════════════════

	/** Jumlah sekolah unik berdasarkan namaSekolahAsal atau asalSma string. */
	@SuppressWarnings("unchecked")
	private int countSekolahUnik(Session session, String ta, String sem, Jenjang jenjang) {
		try {
			String hql = "SELECT COUNT(DISTINCT bcm.namaSekolahAsal) "
					+ "FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause(jenjang)
					+ "AND bcm.namaSekolahAsal IS NOT NULL";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta);
			applySemParam(q, sem);
			applyJenjangParam(q, jenjang);
			Number n = (Number) q.uniqueResult();
			if (n != null && n.intValue() > 0) { return n.intValue(); }

			// Fallback ke string asalSma
			String hql2 = "SELECT COUNT(DISTINCT bcm.asalSma) "
					+ "FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause(jenjang)
					+ "AND bcm.asalSma IS NOT NULL AND bcm.asalSma != ''";
			org.hibernate.Query q2 = session.createQuery(hql2).setParameter("ta", ta);
			applySemParam(q2, sem);
			applyJenjangParam(q2, jenjang);
			Number n2 = (Number) q2.uniqueResult();
			return n2 == null ? 0 : n2.intValue();
		} catch (Exception e) {
			logErr("countSekolahUnik ta=" + ta, e);
			return 0;
		}
	}

	/** Top N sekolah berdasarkan namaSekolahAsal entity, fallback ke asalSma string. */
	@SuppressWarnings("unchecked")
	private List<NamaData> queryTopSekolah(Session session, String ta, String sem, Jenjang jenjang) {
		List<NamaData> result = new ArrayList<NamaData>();
		try {
			// Coba join entity NamaSekolahAsal
			String hql = "SELECT nsa.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.namaSekolahAsal nsa "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause(jenjang)
					+ "GROUP BY nsa.id, nsa.nama "
					+ "ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta)
					.setMaxResults(MAX_SEKOLAH);
			applySemParam(q, sem);
			applyJenjangParam(q, jenjang);
			for (Object obj : q.list()) {
				Object[] r = (Object[]) obj;
				result.add(new NamaData(r[0] == null ? "-" : r[0].toString(),
						r[1] == null ? 0 : ((Number) r[1]).intValue()));
			}
			if (!result.isEmpty()) { return result; }

			// Fallback: string asalSma
			String hql2 = "SELECT bcm.asalSma, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause(jenjang)
					+ "AND bcm.asalSma IS NOT NULL AND bcm.asalSma != '' "
					+ "GROUP BY bcm.asalSma "
					+ "ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query q2 = session.createQuery(hql2).setParameter("ta", ta)
					.setMaxResults(MAX_SEKOLAH);
			applySemParam(q2, sem);
			applyJenjangParam(q2, jenjang);
			for (Object obj : q2.list()) {
				Object[] r = (Object[]) obj;
				result.add(new NamaData(r[0] == null ? "-" : r[0].toString(),
						r[1] == null ? 0 : ((Number) r[1]).intValue()));
			}
		} catch (Exception e) {
			logErr("queryTopSekolah ta=" + ta, e);
		}
		return result;
	}

	/** Distribusi berdasarkan jenis sekolah (SMA / SMK / MA / dll). */
	@SuppressWarnings("unchecked")
	private List<NamaData> queryJenisSekolah(Session session, String ta, String sem, Jenjang jenjang) {
		List<NamaData> result = new ArrayList<NamaData>();
		try {
			String hql = "SELECT j.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.jenisSekolah j "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause(jenjang)
					+ "GROUP BY j.id, j.nama "
					+ "ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta);
			applySemParam(q, sem);
			applyJenjangParam(q, jenjang);
			for (Object obj : q.list()) {
				Object[] r = (Object[]) obj;
				result.add(new NamaData(r[0] == null ? "-" : r[0].toString(),
						r[1] == null ? 0 : ((Number) r[1]).intValue()));
			}
		} catch (Exception e) {
			logErr("queryJenisSekolah ta=" + ta, e);
		}
		return result;
	}

	/** Top N propinsi dari propinsiSekolah (bukan propinsi calon — ini propinsi sekolahnya). */
	@SuppressWarnings("unchecked")
	private List<NamaData> queryPropinsiSekolah(Session session, String ta, String sem, Jenjang jenjang) {
		List<NamaData> result = new ArrayList<NamaData>();
		try {
			String hql = "SELECT ps.nama, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm JOIN bcm.propinsiSekolah ps "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause(jenjang)
					+ "GROUP BY ps.id, ps.nama "
					+ "ORDER BY COUNT(bcm) DESC";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta)
					.setMaxResults(MAX_PROPINSI);
			applySemParam(q, sem);
			applyJenjangParam(q, jenjang);
			for (Object obj : q.list()) {
				Object[] r = (Object[]) obj;
				result.add(new NamaData(r[0] == null ? "-" : r[0].toString(),
						r[1] == null ? 0 : ((Number) r[1]).intValue()));
			}
		} catch (Exception e) {
			logErr("queryPropinsiSekolah ta=" + ta, e);
		}
		return result;
	}

	/** Distribusi tahun kelulusan SMA. */
	@SuppressWarnings("unchecked")
	private List<NamaData> queryTahunLulusan(Session session, String ta, String sem, Jenjang jenjang) {
		List<NamaData> result = new ArrayList<NamaData>();
		try {
			String hql = "SELECT bcm.tahunKelulusan, COUNT(bcm) "
					+ "FROM BiodataCalonMahasiswa bcm "
					+ "WHERE bcm.tahunAkademik = :ta " + semClause(sem) + jenjangClause(jenjang)
					+ "AND bcm.tahunKelulusan IS NOT NULL AND bcm.tahunKelulusan != '' "
					+ "GROUP BY bcm.tahunKelulusan "
					+ "ORDER BY bcm.tahunKelulusan";
			org.hibernate.Query q = session.createQuery(hql).setParameter("ta", ta)
					.setMaxResults(10);
			applySemParam(q, sem);
			applyJenjangParam(q, jenjang);
			for (Object obj : q.list()) {
				Object[] r = (Object[]) obj;
				result.add(new NamaData(r[0] == null ? "-" : r[0].toString(),
						r[1] == null ? 0 : ((Number) r[1]).intValue()));
			}
		} catch (Exception e) {
			logErr("queryTahunLulusan ta=" + ta, e);
		}
		return result;
	}

}
