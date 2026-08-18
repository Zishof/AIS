package ais.action.report.format1.sirs.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.DiagnosaPenyakitPunyaPemeriksaan;
import ais.database.model.sirs.Pemeriksaan;

@SuppressWarnings("rawtypes")
public class PemeriksaanReportHelper {

	private DiagnosaPenyakit diagnosaPenyakit;

	private List hasil = new ArrayList();
	private String jenis;

	public PemeriksaanReportHelper(DiagnosaPenyakit diagnosaPenyakit, String jenis) {
		this.diagnosaPenyakit = diagnosaPenyakit;
		this.jenis = jenis;
		createHeader();
	}

	public List getHasil() {
		System.out.println("hasil = " + hasil);
		return hasil;
	}

	@SuppressWarnings("unchecked")
	private Set<Long> populateRootParents() {
		Set<Long> longs = new HashSet();

		Session session = HibernateUtil.currentSession();
		List<Pemeriksaan> pemeriksaans = session.createCriteria(DiagnosaPenyakitPunyaPemeriksaan.class)
				.setProjection(Projections.property("pemeriksaan"))
				.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit)).createAlias("pemeriksaan", "pemeriksaan")
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("pemeriksaan.jenis", jenis)).list();

		for (Pemeriksaan pemeriksaan : pemeriksaans) {
			Pemeriksaan temp = pemeriksaan.getParent();
			if (temp == null) {
				longs.add(pemeriksaan.getId());
			}
			while (temp != null) {
				if (temp.getParent() == null) {
					longs.add(temp.getId());
				}
				temp = temp.getParent();
			}
		}

		return longs;
	}

	@SuppressWarnings("unchecked")
	private void createHeader() {
		Session session = HibernateUtil.currentSession();
		Set<Long> parents = populateRootParents();
		System.out.println("parents = " + parents);
		List<Pemeriksaan> pemeriksaans = parents.isEmpty() ? new ArrayList<Pemeriksaan>()
				: session.createCriteria(Pemeriksaan.class).add(Restrictions.isNull("parent"))
						.add(Restrictions.in("id", parents)).addOrder(Order.asc("nama")).list();

		for (Pemeriksaan pemeriksaan : pemeriksaans) {
			Map map = new HashMap();
			map.put("nama", pemeriksaan.getNama());
			map.put("nilai", "");
			map.put("satuan", pemeriksaan.getSatuan());
			hasil.add(map);
			createSub(pemeriksaan);
		}
	}

	@SuppressWarnings("unchecked")
	private void createSub(Pemeriksaan parent) {
		Session session = HibernateUtil.currentSession();
		List<Pemeriksaan> pemeriksaans = session.createCriteria(Pemeriksaan.class)
				.add(parent == null ? Restrictions.isNull("parent") : Restrictions.eq("parent", parent))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("jenis", jenis)).addOrder(Order.asc("nama"))
				.list();

		for (Pemeriksaan pemeriksaan : pemeriksaans) {

			String tambahanDepan = "    ";
			Pemeriksaan temPemeriksaan = pemeriksaan.getParent();
			while (temPemeriksaan != null) {
				tambahanDepan += "    ";
				temPemeriksaan = temPemeriksaan.getParent();
			}

			int count = ((Number) session.createCriteria(Pemeriksaan.class).add(Restrictions.eq("parent", pemeriksaan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count != 0) {
				Map map = new HashMap();
				map.put("nama", tambahanDepan + pemeriksaan.getNama());
				map.put("nilai", "");
				map.put("satuan", pemeriksaan.getSatuan());
				hasil.add(map);
				createSub(pemeriksaan);
			} else {
				DiagnosaPenyakitPunyaPemeriksaan diagnosaPenyakitPunyaPemeriksaan = (DiagnosaPenyakitPunyaPemeriksaan) session
						.createCriteria(DiagnosaPenyakitPunyaPemeriksaan.class)
						.add(Restrictions.eq("diagnosaPenyakit", diagnosaPenyakit))
						.add(Restrictions.eq("pemeriksaan", pemeriksaan)).setMaxResults(1).uniqueResult();
				if (diagnosaPenyakitPunyaPemeriksaan != null) {
					Map map = new HashMap();
					map.put("nama", tambahanDepan + pemeriksaan.getNama());
					String nilai = diagnosaPenyakitPunyaPemeriksaan.getNama() + " "
							+ diagnosaPenyakitPunyaPemeriksaan.getPilihanGanda().replaceAll("\\|", ", ")
							+ diagnosaPenyakitPunyaPemeriksaan.getKeterangan();
					map.put("nilai", nilai.trim());
					map.put("satuan", pemeriksaan.getSatuan());
					hasil.add(map);
				} else {
					Map map = new HashMap();
					map.put("nama", tambahanDepan + pemeriksaan.getNama());
					map.put("nilai", "");
					map.put("satuan", pemeriksaan.getSatuan());
					hasil.add(map);
				}
			}
		}
	}

}
