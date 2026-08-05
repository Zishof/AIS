package ais.action.master.bkd.helper;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Label;

import ais.action.master.bkd.PenilaianAsesorAction;
import ais.common.Common;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.Asesor;
import ais.database.model.AsesorPegawai;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;
import ais.database.model.Tbmuser;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.penelitiandanpengabdian.TingkatArtikel;

public class BkdArtikelHelper {
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {

		Criterion criterion = pegawai == null ? Restrictions.isNotNull("tbmuser.pegawai")
				: Restrictions.eq("tbmuser.pegawai", pegawai);

		List<Artikel> artikels = session.createCriteria(Artikel.class).createAlias("tbmuser", "tbmuser").add(criterion)
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semester == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("semester", semester))
				.add(Restrictions.or(Restrictions.gt("articleId", 0), Restrictions.eq("status", Artikel.DISETUJUI)))
				.list();

		int rowCount = artikels.size();
		int i = 0;
		for (Artikel artikel : artikels) {
			label.setValue("Memproses data artikel \"" + artikel + "\" ("
					+ Common.numberFormat.get().format(i * 100.0 / rowCount) + "%)");
			i++;

			List<Pegawai> pegawais = new ArrayList<Pegawai>();
			for (String username : StringUtils.split(artikel.getAnggota(), ",")) {
				Tbmuser tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("userId", username)).uniqueResult();
				if (tbmuser != null && tbmuser.ambilPegawai() != null) {
					pegawais.add(tbmuser.ambilPegawai());
				}
			}
			BkdArtikelHelper.populate(session, artikel.getTbmuser().getPegawai(), tahunAkademik, semester, artikel,
					"ketua", pegawais.size());

			for (Pegawai p : pegawais) {
				BkdArtikelHelper.populate(session, p, tahunAkademik, semester, artikel, "anggota", pegawais.size());
			}

		}

	}

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Artikel artikel, String jenisAnggotaPublikasi, Integer jumlahAnggota) {

		if (pegawai == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)) )
				.add(Restrictions.eq("pegawai", pegawai)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("pegawai => " + pegawai + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			for (TingkatArtikel tingkatArtikel : artikel.getTingkatArtikeles()) {

				String key = "pengaturan_beban_sks_artikel";
				String newKey = key + "_" + artikel.getTahapanPenyusunanArtikel().getId() + "_"
						+ tingkatArtikel.getId();

				ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

				double sks = 0.0;
				try {
					sks = Double.parseDouble(konfigurasi.getNilai());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}

				String pengaturan_pembagian_beban_sks_artikel = Common
						.getParameterUmum("pengaturan_pembagian_beban_sks_artikel", "Dinilai sama").getNilai();

				if (jenisAnggotaPublikasi.equalsIgnoreCase("anggota") && jumlahAnggota > 0) {
					if (pengaturan_pembagian_beban_sks_artikel.equalsIgnoreCase("50%")) {
						sks = sks / 2.0;
					} else if (pengaturan_pembagian_beban_sks_artikel.equalsIgnoreCase("Dibagi rata")) {
						sks = sks / (jumlahAnggota.doubleValue() + 1); // ditambah
																		// satu
																		// sebagai
																		// ketua
					} else if (pengaturan_pembagian_beban_sks_artikel.equalsIgnoreCase(
							"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%")) {
						if (jumlahAnggota > 1) {
							sks = (sks * 0.6) / jumlahAnggota.doubleValue();
						} else if (jumlahAnggota == 1) {
							sks = (sks * 0.4) / jumlahAnggota.doubleValue();
						}
					}
				} else if (jenisAnggotaPublikasi.equalsIgnoreCase("ketua") && jumlahAnggota > 0) {
					if (pengaturan_pembagian_beban_sks_artikel.equalsIgnoreCase("50%")) {
						sks = sks / 2.0;
					} else if (pengaturan_pembagian_beban_sks_artikel.equalsIgnoreCase("Dibagi rata")) {
						sks = sks / (jumlahAnggota.doubleValue() + 1); // ditambah
																		// satu
																		// sebagai
																		// ketua
					} else if (pengaturan_pembagian_beban_sks_artikel.equalsIgnoreCase(
							"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%")) {
						if (jumlahAnggota > 1) {
							sks = (sks * 0.4) / jumlahAnggota.doubleValue();
						} else if (jumlahAnggota == 1) {
							sks = (sks * 0.6) / jumlahAnggota.doubleValue();
						}
					}
				}

				String keterangan = pegawai.getNama() + " telah mem-publikasi artikel dengan judul \""
						+ artikel.getJudul() + "\" "
						+ (jumlahAnggota > 0 ? ("sebagai " + jenisAnggotaPublikasi + " dengan jumlah anggota "
								+ jumlahAnggota + " orang") : "")
						+ " " + artikel.getTahapanPenyusunanArtikel().getNama() + "  " + tingkatArtikel.getNama()
						+ " . SKS yang diporeh adalah " + Common.numberFormat.get().format(sks) + ", cara penghitungan "
						+ pengaturan_pembagian_beban_sks_artikel;

				AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session
						.createCriteria(AsesemenPenilaian.class).add(Restrictions.eq("pegawai", pegawai))
						.add(Restrictions.eq("artikel", artikel)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
						.add(Restrictions.eq("semester", semester)).setMaxResults(1).uniqueResult();
				if (asesemenPenilaian == null) {
					asesemenPenilaian = new AsesemenPenilaian();
				}

				asesemenPenilaian.setSpesifikasi(PenilaianAsesor.ARTIKEL);
				asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENELITIAN);
				asesemenPenilaian.setKeterangan(keterangan);
				asesemenPenilaian.setArtikel(artikel);
				asesemenPenilaian.setTahunAkademik(tahunAkademik);
				asesemenPenilaian.setSemester(semester);
				asesemenPenilaian.setPegawai(pegawai);
				asesemenPenilaian.setSks(sks);

				session.getTransaction().begin();
				session.saveOrUpdate(asesemenPenilaian);
				session.getTransaction().commit();

				PenilaianAsesorAction.checkPenilaian(session, asesors, asesemenPenilaian);
			}
		}
	}
}
