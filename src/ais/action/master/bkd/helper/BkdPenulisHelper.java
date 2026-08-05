package ais.action.master.bkd.helper;

import java.util.List;
import java.util.Map;

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
import ais.database.model.BukuBahanAjar;
import ais.database.model.Dosen;
import ais.database.model.ParameterUmum;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenunjangKinerjaDosen;

public class BkdPenulisHelper {
	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			Label label) {

		Criterion criterion = pegawai == null ? Restrictions.isNotNull("dosenPengarang1.pegawaiId")
				: Restrictions.eq("dosenPengarang1.pegawaiId", pegawai.getId());

		List<BukuBahanAjar> bukuBahanAjars = session.createCriteria(BukuBahanAjar.class)
				.add(Restrictions.isNotNull("tahapanPenyusunanBuku")).add(Restrictions.isNotNull("jenisPeredaranBuku"))
				.createAlias("dosenPengarang1", "dosenPengarang1").add(criterion)
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semester == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("semester", semester))
				.list();

		int rowCount = bukuBahanAjars.size();
		int i = 0;
		for (BukuBahanAjar bukuBahanAjar : bukuBahanAjars) {
			label.setValue("Memproses data buku ajar \"" + bukuBahanAjar + "\" ("
					+ Common.numberFormat.get().format(i * 100.0 / rowCount) + "%)");
			i++;

			Map<String, Dosen> pegawais = bukuBahanAjar.populateDosenAnggota();
			if (bukuBahanAjar.getDosenPengarang1().getPegawaiId() != null) {
				BkdPenulisHelper.populate(session, new Pegawai(bukuBahanAjar.getDosenPengarang1()), tahunAkademik,
						semester, bukuBahanAjar, "ketua", pegawais.size());
			}

			for (Dosen p : pegawais.values()) {
				if (p.getPegawaiId() != null) {
					BkdPenulisHelper.populate(session, new Pegawai(p), tahunAkademik, semester, bukuBahanAjar,
							"anggota", pegawais.size());
				}
			}

		}

	}

	@SuppressWarnings("unchecked")
	public static void populate(Session session, final Pegawai pegawai, String tahunAkademik, String semester,
			BukuBahanAjar bukuBahanAjar, String jenisAnggotaPublikasi, Integer jumlahAnggota) {

		if (pegawai == null) {
			return;
		}

		List<Asesor> asesors = session.createCriteria(AsesorPegawai.class).createAlias("pegawai", "pegawai")
				.createAlias("asesor", "asesor")
				.add(Restrictions.or(Restrictions.isNull("asesor.aktif"), Restrictions.eq("asesor.aktif", true)) )
				.add(Restrictions.eq("pegawai", pegawai)).setProjection(Projections.groupProperty("asesor")).list();

		System.out.println("pegawai => " + pegawai + ", asesors => " + asesors);
		if (!asesors.isEmpty()) {

			String key = "pengaturan_beban_sks_buku";
			String newKey = key + "_" + bukuBahanAjar.getTahapanPenyusunanBuku().getId() + "_"
					+ bukuBahanAjar.getJenisPeredaranBuku().getId();

			ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

			double sks = 0.0;
			try {
				sks = Double.parseDouble(konfigurasi.getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			String pengaturan_pembagian_beban_sks_buku = Common
					.getParameterUmum("pengaturan_pembagian_beban_sks_buku", "Dinilai sama").getNilai();

			if (jenisAnggotaPublikasi.equalsIgnoreCase("anggota") && jumlahAnggota > 0) {
				if (pengaturan_pembagian_beban_sks_buku.equalsIgnoreCase("50%")) {
					sks = sks / 2.0;
				} else if (pengaturan_pembagian_beban_sks_buku.equalsIgnoreCase("Dibagi rata")) {
					sks = sks / (jumlahAnggota.doubleValue() + 1); // ditambah
																	// satu
																	// sebagai
																	// ketua
				} else if (pengaturan_pembagian_beban_sks_buku.equalsIgnoreCase(
						"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%")) {
					if (jumlahAnggota > 1) {
						sks = (sks * 0.6) / jumlahAnggota.doubleValue();
					} else if (jumlahAnggota == 1) {
						sks = (sks * 0.4) / jumlahAnggota.doubleValue();
					}
				}
			} else if (jenisAnggotaPublikasi.equalsIgnoreCase("ketua") && jumlahAnggota > 0) {
				if (pengaturan_pembagian_beban_sks_buku.equalsIgnoreCase("50%")) {
					sks = sks / 2.0;
				} else if (pengaturan_pembagian_beban_sks_buku.equalsIgnoreCase("Dibagi rata")) {
					sks = sks / (jumlahAnggota.doubleValue() + 1); // ditambah
																	// satu
																	// sebagai
																	// ketua
				} else if (pengaturan_pembagian_beban_sks_buku.equalsIgnoreCase(
						"Ketua = 60% dan 1 Anggota = 40%, jika Anggota > 1, maka Ketua = 40% dan Anggota = 60%")) {
					if (jumlahAnggota > 1) {
						sks = (sks * 0.4) / jumlahAnggota.doubleValue();
					} else if (jumlahAnggota == 1) {
						sks = (sks * 0.6) / jumlahAnggota.doubleValue();
					}
				}
			}

			String keterangan = pegawai.getNama() + " telah mem-publikasi buku dengan judul \""
					+ bukuBahanAjar.getNama() + "\" "
					+ (jumlahAnggota > 0 ? ("sebagai " + jenisAnggotaPublikasi + " dengan jumlah anggota "
							+ jumlahAnggota + " orang") : "")
					+ " " + bukuBahanAjar.getTahapanPenyusunanBuku().getNama() + "  "
					+ bukuBahanAjar.getJenisPeredaranBuku().getNama() + " . SKS yang diporeh adalah "
					+ Common.numberFormat.get().format(sks) + ", cara penghitungan " + pengaturan_pembagian_beban_sks_buku;

			AsesemenPenilaian asesemenPenilaian = (AsesemenPenilaian) session.createCriteria(AsesemenPenilaian.class)
					.add(Restrictions.eq("pegawai", pegawai)).add(Restrictions.eq("bukuBahanAjar", bukuBahanAjar))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik)).add(Restrictions.eq("semester", semester))
					.setMaxResults(1).uniqueResult();
			if (asesemenPenilaian == null) {
				asesemenPenilaian = new AsesemenPenilaian();
			}

			asesemenPenilaian.setSpesifikasi(PenilaianAsesor.PENULIS_BUKU);
			asesemenPenilaian.setBidang(PenunjangKinerjaDosen.PENDIDIKAN);
			asesemenPenilaian.setKeterangan(keterangan);
			asesemenPenilaian.setBukuBahanAjar(bukuBahanAjar);
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
