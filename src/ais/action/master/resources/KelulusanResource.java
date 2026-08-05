package ais.action.master.resources;

import java.net.URLDecoder;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.NotFoundException;

import com.sun.jersey.spi.resource.Singleton;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;

@Path("/kelulusan")
@Singleton

public class KelulusanResource {

	@SuppressWarnings("unchecked")
	@GET
	@Path("skripsi/{username}/{password}/{program}/{ta}/{nim}/{start}/{max}")
	@Produces({ MediaType.TEXT_PLAIN })
	public String pembayaranMahasiswa(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("program") String program, @PathParam("ta") String ta, @PathParam("nim") String nim,
			@PathParam("start") String start, @PathParam("max") String max) throws Exception {

		username = URLDecoder.decode(username.replaceAll("_", ""), "UTF-8");
		password = URLDecoder.decode(password.replaceAll("_", ""), "UTF-8");
		ta = URLDecoder.decode(ta.replaceAll("_", ""), "UTF-8");
		program = URLDecoder.decode(program.replaceAll("_", "").trim(), "UTF-8");
		nim = URLDecoder.decode(nim.replaceAll("_", "").trim(), "UTF-8");

		start = URLDecoder.decode(start.replaceAll("_", "").trim(), "UTF-8");
		max = URLDecoder.decode(max.replaceAll("_", "").trim(), "UTF-8");

		if (!Common.checkLogin(username, password))
			throw new NotFoundException("fobidden access");

		String tahunAkademik = Common.getCurrentTahunAkademik();
		String semesters = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GANJIL;
		if (ta != null && ta.length() == 5) {
			Integer mulai = Integer.parseInt(ta.toString().substring(0, 4));
			tahunAkademik = mulai + "/" + (mulai + 1);
			Integer s = Integer.parseInt(ta.toString().substring(4, 5));
			semesters = s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;

		}

		System.out.println("ta => " + ta + ", program => " + program + ", nim => " + nim + ", start => " + start
				+ ", max => " + max);

		Session session = HibernateUtil.currentNativeSession();

		List<Skripsi> skripsis = session.createCriteria(Skripsi.class)

				.addOrder(Order.desc("id")).createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)

				.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.program", program))

				.add(nim == null || nim.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("mahasiswa.nim", nim, MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nim, MatchMode.ANYWHERE)))

				.add(tahunAkademik == null || tahunAkademik.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semesters == null ? Restrictions.sqlRestriction("true")
						: Restrictions
								.sqlRestriction("semester % 2 = " + (semesters.equals(Perkuliahan.GANJIL) ? "1" : "0")))

				.setFirstResult(start == null || start.trim().isEmpty() || !Common.isNumber(start) ? 0
						: Integer.parseInt(start.trim()))
				.setMaxResults(max == null || max.trim().isEmpty() || !Common.isNumber(max) ? 10
						: Integer.parseInt(max.trim()))

				.list();

		HibernateUtil.closeSession();

		JSONArray array = new JSONArray();

		for (Skripsi skripsi : skripsis) {
			JSONObject json = new JSONObject();

			Matakuliah matakuliah = skripsi.getDetailperkuliahan().getPerkuliahan() == null
					? skripsi.getDetailperkuliahan().getMatakuliahKonversi()
					: skripsi.getDetailperkuliahan().getPerkuliahan().getMatakuliah();

			json.put("id", skripsi.getId());
			json.put("gelombang", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? ""
					: skripsi.getGelombangPendaftaranSidangTugasAkhir().getNama());
			json.put("jenis", skripsi.getFormatNilaiSkripsi() == null ? "" : skripsi.getFormatNilaiSkripsi().getNama());
			json.put("matakuliah_nama", matakuliah == null ? "" : matakuliah.getNama());
			json.put("matakuliah_kode", matakuliah == null ? "" : matakuliah.getKode());
			json.put("mahasiswa_nama", skripsi.getMahasiswa() == null ? "" : skripsi.getMahasiswa().getNama());
			json.put("mahasiswa_nim", skripsi.getMahasiswa() == null ? "" : skripsi.getMahasiswa().getNim());
			json.put("semester", skripsi.getSemester());
			json.put("judul", skripsi.getJudul());
			json.put("judul_en", skripsi.getJudulen());
			json.put("abtrak", skripsi.getAbstrack());
			json.put("keyword", skripsi.getKeyword());
			json.put("nilai_angka", skripsi.getTotalNilai());
			json.put("nilai_ip", skripsi.getTotalIP());
			json.put("nilai_huruf", skripsi.getNilaiHuruf());
			json.put("keterangan", skripsi.getKeterangan());
			json.put("dosen_1_nama", skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNama());
			json.put("dosen_1_nidn", skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getNidn());

			json.put("dosen_2_nama", skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNama());
			json.put("dosen_2_nidn", skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getNidn());

			json.put("dosen_3_nama", skripsi.getPenguji1() == null ? "" : skripsi.getPenguji1().getNama());
			json.put("dosen_3_nidn", skripsi.getPenguji1() == null ? "" : skripsi.getPenguji1().getNidn());

			json.put("dosen_4_nama", skripsi.getPenguji2() == null ? "" : skripsi.getPenguji2().getNama());
			json.put("dosen_4_nidn", skripsi.getPenguji2() == null ? "" : skripsi.getPenguji2().getNidn());

			json.put("dosen_5_nama", skripsi.getPenguji3() == null ? "" : skripsi.getPenguji3().getNama());
			json.put("dosen_5_nidn", skripsi.getPenguji3() == null ? "" : skripsi.getPenguji3().getNidn());

			json.put("dosen_6_nama", skripsi.getPenguji4() == null ? "" : skripsi.getPenguji4().getNama());
			json.put("dosen_6_nidn", skripsi.getPenguji4() == null ? "" : skripsi.getPenguji4().getNidn());

			json.put("smt", skripsi.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
			json.put("ta", skripsi.getTahunAkademik());

			LampiranLain lam = LampiranLain.ambil(skripsi.getId(), LampiranLain.SKRIPSI);

			if (lam != null) {
				json.put("file_lampiran", FileFotoLain.ambilLinkLampiranLain(lam, false, false, LampiranLain.class));
			} else {
				json.put("file_lampiran", "");
			}

			lam = LampiranLain.ambil(skripsi.getId(), LampiranLain.COVER_SKRIPSI);

			if (lam != null) {
				json.put("file_cover", FileFotoLain.ambilLinkLampiranLain(lam, false, false, LampiranLain.class));
			} else {
				json.put("file_cover", "");
			}

			array.put(json);
		}

		return array.toString();
	}

}
