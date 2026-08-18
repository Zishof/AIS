package ais.action.master.resources.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.sun.jersey.api.NotFoundException;

import ais.action.master.helper.KrsUtilHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Komentar;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MatakuliahPrasyarat;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;

public class KrsResourceHelper {

	@SuppressWarnings("unchecked")
	public static boolean hapusKrs(Mahasiswa mahasiswa, Integer semester, String krs) {
		Session session = HibernateUtil.currentSession();

		// HibernateUtil.session = session;

		String[] myStrings = krs.split(",");
		List<Perkuliahan> perkuliahans = new ArrayList<Perkuliahan>();
		for (String myId : myStrings) {
			Perkuliahan myPerkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(myId.trim()))).uniqueResult();
			if (myPerkuliahan == null) {
				continue;
			}
			perkuliahans.add(myPerkuliahan);
		}

		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.isNull("ikutiPerkuliahan")).add(perkuliahans.size() == 0
						? Restrictions.sqlRestriction("1!=1") : Restrictions.in("perkuliahan", perkuliahans))
				.list();

		session.getTransaction().begin();

		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {

			if (detailperkuliahan.getPersetujuan() == null
					|| !detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
				throw new NotFoundException("Matakuliah " + detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
						+ " sudah disetujui. Anda tidak bisa menghapusnya");
			}

			if (Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol")) {
				if (detailperkuliahan.getPersetujuan() != null
						&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& detailperkuliahan.getTotalNilai() > 1.0) {
					throw new NotFoundException("Jika nilai tidak nol, anda tidak bisa menghapus matakuliah "
							+ detailperkuliahan.getPerkuliahan().getMatakuliah().getNama() + "");

				}
			}

			List<Komentar> komentars = session.createCriteria(Komentar.class)
					.add(Restrictions.eq("detailperkuliahan", detailperkuliahan.getId())).list();

			for (Komentar komentar : komentars) {
				Common.refreshDelete(session, (komentar));
			}

			Common.refreshDelete(session, (detailperkuliahan));
		}

		session.getTransaction().commit();

		//
		// HibernateUtil.closeSession();

		HibernateUtil.closeSession();
		return true;
	}

	public static boolean checkAmbil(Mahasiswa mahasiswa, Integer semester, String krs) {

		if (mahasiswa.getDosen() == null) {
			throw new NotFoundException(
					"Anda belum mempunyai dosen pembimbing akademik, sehingga tidak bisa mengambil KRS. Harap segera menghubungi bagian Akademik atau Admin Fakultas atau Prodi untuk mendaftarkan Dosen Pembimbing Akademik Anda");
		}

		if (!Common.checkStatusPembayaranMahasiswa(semester, 0, mahasiswa, false, false)) {
			throw new NotFoundException("Anda belum membayar biaya perkuliahan di semester " + semester
					+ ". Ambillah KRS yang baru saja anda lakukan pembayaran. Harap hubungi bagian keuangan untuk informasi lebih lanjut");
		}
		// if
		// (!mahasiswa.getStatus().getId().equals(ConstantValues.AKTIF.getId()))
		// {
		// throw new NotFoundException("Status anda sedang " +
		// mahasiswa.getStatus().getNama()
		// + ", anda tidak bisa mengambil KRS. Hubungi admin untuk informasi
		// lebih lanjut");
		// }

		return true;
	}

	public static boolean ambilKrs(Mahasiswa mahasiswa, Integer semester, String krs, Tbmuser tbmuser) {

		if (!checkAmbil(mahasiswa, semester, krs)) {
			return false;
		}

		Session session = HibernateUtil.currentSession();
		// HibernateUtil.session = session;
		try {
			String[] myStrings = krs.split(",");
			List<Perkuliahan> perkuliahans = new ArrayList<Perkuliahan>();
			for (String myId : myStrings) {
				Perkuliahan myPerkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(Long.parseLong(myId.trim()))).uniqueResult();
				if (myPerkuliahan == null) {
					continue;
				}
				perkuliahans.add(myPerkuliahan);
			}

			Integer jmlSks = hitungSksYangTelahDiambil(mahasiswa, semester, perkuliahans);

			if (Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, semester, jmlSks, null)) {
				throw new NotFoundException(
						"SKS yang anda ambil melebihi batas maksimal. SKS yang anda ambil adalah " + jmlSks + " SKS");
			}

			session.getTransaction().begin();

			String peringatanKapasitasRuangan = "";
			Set<Long> matakuliahs = new HashSet<Long>();
			for (Perkuliahan perkuliahan : perkuliahans) {
				if (matakuliahs.contains(perkuliahan.getMatakuliah().getId())) {
					continue;
				}
				matakuliahs.add(perkuliahan.getMatakuliah().getId());
				Long id = null;
				try {
					id = (Long) (session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.property("id"))
							.add(Restrictions.eq("perkuliahan", perkuliahan))
							.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("semester", semester))
							.uniqueResult());
				} catch (Exception e) {
					continue;
				}

				String sql = "select " + "count(aa.id) as jml " + "from detailperkuliahan aa "
						+ "left join perkuliahan bb on (aa.perkuliahan = bb.id) " + "where (bb.matakuliah in " + "( "
						+ "select " + "b.matakuliah_prasyarat " + "from perkuliahan a "
						+ "inner join matakuliah_prasyarat b on (b.matakuliah = a.matakuliah) "
						+ "where a.matakuliah = " + perkuliahan.getMatakuliah().getId() + " " + ")  "
						+ " or aa.matakuliah_konversi in " + "( " + "select " + "b.matakuliah_prasyarat "
						+ "from perkuliahan a " + "inner join matakuliah_prasyarat b on (b.matakuliah = a.matakuliah) "
						+ "where a.matakuliah = " + perkuliahan.getMatakuliah().getId() + " " + ")) and aa.mahasiswa = "
						+ mahasiswa.getId();

				System.out.println("sql = " + sql);

				Integer myCount = ((Number) session.createCriteria(MatakuliahPrasyarat.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				if (!myCount.equals(0)) {
					Integer count = ((Number) session.createSQLQuery(sql).uniqueResult()).intValue();

					if (count.equals(0)) {
						throw new NotFoundException(
								"Mahsiswa dengan NIM '" + mahasiswa.getNim() + "' dan nama '" + mahasiswa.getNama()
										+ "' belum boleh mengambil matakuliah '" + perkuliahan.getMatakuliah().getNama()
										+ "', karena belum mengambil salah satu dari matakuliah prasyarat");
					}
				}

				Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser, KrsResourceHelper.class);
				if (id != null) {
					detailperkuliahan = (Detailperkuliahan) session.load(Detailperkuliahan.class, id);
				} else {

					Session mySession = HibernateUtil.currentNativeSession();
					Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(mySession, perkuliahan, false);

					HibernateUtil.closeSession();

					jumlahUdahMasuk++;
					if (jumlahUdahMasuk > (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
							: perkuliahan.getKapasitasKelas())) {
						peringatanKapasitasRuangan += "Kapasitas kelas sudah penuh. Maksimal kapasitas kelas tesebut adalah "
								+ (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
										: perkuliahan.getKapasitasKelas())
								+ ", sedangkan anda mencoba masuk ke perkuliahan ini menjadi berjumlah "
								+ jumlahUdahMasuk + ". Pilihlah jadwal perkuliahan lainnya.\n";
						continue;
					}
				}

				detailperkuliahan.setNilaiHuruf("");
				detailperkuliahan.setTotalNilai(0.0);
				detailperkuliahan.setMahasiswa(mahasiswa);
				detailperkuliahan.setPerkuliahan(perkuliahan);
				detailperkuliahan.setSemester(semester);
				session.saveOrUpdate(detailperkuliahan);

			}

			session.getTransaction().commit();
			//
			// HibernateUtil.closeSession();

			HibernateUtil.closeSession();

			if (!peringatanKapasitasRuangan.trim().equals("")) {
				throw new NotFoundException(peringatanKapasitasRuangan);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan pada saat pengambilan KRS. " + e.getMessage());

		}

		return true;
	}

	@SuppressWarnings({ })
	private static Integer hitungSksYangTelahDiambil(Mahasiswa mahasiswa, Integer semester,
			List<Perkuliahan> perkuliahans) {
		Map<Long, Perkuliahan> map = new java.util.HashMap<Long, Perkuliahan>();

		for (Perkuliahan perkuliahan : perkuliahans) {
			map.put(perkuliahan.getMatakuliah().getId(), perkuliahan);
		}

		Integer jumlah = KrsUtilHelper.hitungSksYangTelahDiambil(map, mahasiswa, null, semester, null);
		return jumlah;
	}

}
