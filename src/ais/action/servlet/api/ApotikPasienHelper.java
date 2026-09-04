package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlergiPasien;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Pasien;

/**
 * Profil pasien Apotik berbasis model SIRS existing. Helper ini read-only dan
 * sengaja tidak membuat salinan tabel pasien/alergi/diagnosis.
 */
public final class ApotikPasienHelper {

	private ApotikPasienHelper() {
	}

	private static String str(Object nilai) {
		return nilai == null ? "" : nilai.toString();
	}

	private static Long optLong(JSONObject request, String kunci) {
		if (request == null || request.isNull(kunci)) return null;
		try {
			return Long.valueOf(str(request.get(kunci)).trim());
		} catch (Exception ignored) {
			return null;
		}
	}

	private static String tanggal(java.util.Date nilai) {
		return nilai == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(nilai);
	}

	private static JSONObject ringkas(Pasien pasien) throws Exception {
		JSONObject j = new JSONObject();
		j.put("id", pasien.getId());
		j.put("kode", str(pasien.getKode()));
		j.put("nama", str(pasien.getNama()));
		j.put("jenisKelamin", str(pasien.getJenisKelamin()));
		j.put("tanggalLahir", tanggal(pasien.getTanggalLahir()));
		j.put("noHp", str(pasien.getNoHp()));
		j.put("noTelp", str(pasien.getNoTelp()));
		j.put("aktif", pasien.getAktif() == null || Boolean.TRUE.equals(pasien.getAktif()));
		return j;
	}

	@SuppressWarnings("unchecked")
	public static void cari(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request == null ? "" : request.optString("keyword", "").trim();
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria daftar = session.createCriteria(Pasien.class);
			org.hibernate.Criteria jumlah = session.createCriteria(Pasien.class);
			if (!keyword.isEmpty()) {
				org.hibernate.criterion.Criterion filter = Restrictions.disjunction()
						.add(Restrictions.ilike("kode", "%" + keyword + "%"))
						.add(Restrictions.ilike("nama", "%" + keyword + "%"))
						.add(Restrictions.ilike("noHp", "%" + keyword + "%"))
						.add(Restrictions.ilike("noTelp", "%" + keyword + "%"));
				daftar.add(filter);
				jumlah.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", "%" + keyword + "%"))
						.add(Restrictions.ilike("nama", "%" + keyword + "%"))
						.add(Restrictions.ilike("noHp", "%" + keyword + "%"))
						.add(Restrictions.ilike("noTelp", "%" + keyword + "%")));
			}
			long total = ((Number) jumlah.setProjection(Projections.rowCount())
					.uniqueResult()).longValue();
			daftar.addOrder(Order.asc("nama"));
			daftar.setFirstResult((page - 1) * size).setMaxResults(size);
			List<Pasien> pasien = daftar.list();
			JSONArray data = new JSONArray();
			for (Pasien p : pasien) data.put(ringkas(p));
			hasil.put("status", "00");
			hasil.put("data", data);
			hasil.put("page", page);
			hasil.put("pageSize", size);
			hasil.put("total", total);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	@SuppressWarnings("unchecked")
	public static void detail(JSONObject request, JSONObject hasil) throws Exception {
		Long id = optLong(request, "id");
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "ID pasien wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Pasien pasien = (Pasien) session.get(Pasien.class, id);
			if (pasien == null) {
				hasil.put("status", "91");
				hasil.put("description", "Pasien tidak ditemukan.");
				return;
			}
			JSONObject data = ringkas(pasien);
			data.put("tempatLahir", str(pasien.getTempatLahir()));
			data.put("alamat", str(pasien.getAlamat()));
			data.put("kewarganegaraan", str(pasien.getKewarganegaraan()));
			List<AlergiPasien> alergi = session.createCriteria(AlergiPasien.class)
					.add(Restrictions.eq("pasien", pasien))
					.addOrder(Order.desc("tanggalCatat")).setMaxResults(50).list();
			JSONArray alergiJson = new JSONArray();
			for (AlergiPasien a : alergi) {
				alergiJson.put(new JSONObject()
						.put("id", a.getId())
						.put("substansi", str(a.getSubstansi()))
						.put("kategori", str(a.getKategori()))
						.put("reaksi", str(a.getReaksi()))
						.put("keparahan", str(a.getKeparahan()))
						.put("statusKlinis", str(a.getStatusKlinis()))
						.put("tanggalCatat", tanggal(a.getTanggalCatat()))
						.put("itemId", a.getItemMedis() == null
								? JSONObject.NULL : a.getItemMedis().getId()));
			}
			List<DiagnosaPenyakit> diagnosa = session.createCriteria(DiagnosaPenyakit.class)
					.add(Restrictions.eq("pasien", pasien))
					.addOrder(Order.desc("tanggal")).setMaxResults(20).list();
			JSONArray diagnosaJson = new JSONArray();
			for (DiagnosaPenyakit d : diagnosa) {
				diagnosaJson.put(new JSONObject()
						.put("id", d.getId())
						.put("kode", str(d.getKode()))
						.put("tanggal", tanggal(d.getTanggal()))
						.put("keluhan", str(d.getKeluhanPasien()))
						.put("kesimpulan", str(d.getKesimpulanPemeriksaan())));
			}
			data.put("alergi", alergiJson);
			data.put("diagnosa", diagnosaJson);
			hasil.put("status", "00");
			hasil.put("data", data);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
