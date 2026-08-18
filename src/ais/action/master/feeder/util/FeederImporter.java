package ais.action.master.feeder.util;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Node;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.database.model.BiodataDosen;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GrupJurusan;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.JenisEvaluasi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KebutuhanKhusus;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.LembagaPengangkat;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.Pekerjaan;
import ais.database.model.Penghasilan;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusPegawai;
import ais.database.model.Wilayah;
import ais.database.model.employ.Golongan;

public class FeederImporter {

	private String token;

	private static Integer JUMLAH_SEKALI_AMBIL_DATA = 300;

	private FeederConnector feederConnector;

	private Progressmeter progressmeter;

	private Progressmeter progressmeterChild;

	private Label labelProses;

	public FeederImporter(FeederConnector feederConnector, String token) {
		this.token = token;
		this.feederConnector = feederConnector;
	}

	public FeederImporter(FeederConnector feederConnector, String token, Progressmeter progressmeter,
			Progressmeter progressmeterChild, Label labelProses) {
		this.token = token;
		this.feederConnector = feederConnector;
		this.progressmeter = progressmeter;
		this.progressmeterChild = progressmeterChild;
		this.labelProses = labelProses;
	}

	public void doImport() throws Exception {
		ConstantValues.initNativeSesion();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data agama..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,10);
		}
		agama();
		jenisEvaluasi();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data status awal mahasiswa..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,15);
		}

		statusAwalMahasiswa();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data pekerjaan..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,20);
		}

		pekerjaan();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data penghasilan..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,25);
		}
		penghasilan();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data jenjang..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,30);
		}
		jenjang();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data fakultas..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,35);
		}
		fakultas();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data grup jurusan..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,40);
		}
		grupJurusan();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data jurusan..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,45);
		}
		jurusan();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data perguruan tinggi..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,50);
		}
		perguruanTinggi();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data kurikulum..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,55);
		}
		kurikulum();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data ikatan Kerja Dosen..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,60);
		}
		ikatanKerjaDosen();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data status Kepegawaian..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,62);
		}
		statusKepegawaian();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data status Pegawai..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,64);
		}
		statusPegawai();

		// if (labelProses != null) {
		// NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data status Pegawai..");
		// progressmeter.setValue(65);
		// }
		// statusKepegawaian();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data golongan..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,65);
		}
		golongan();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data nilai huruf..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,70);
		}
		nilaiHuruf();

		// data-data besar

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data kebutuhan khusus..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,75);
		}
		kebutuhanKhusus();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data wilayah..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,80);
		}
		wilayah();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data matakuliah..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,85);
		}
		matakuliah();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data kurikulum punya matakuliah..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,87);
		}
		kurikulumPunyaMatakuliah();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data mahasiswa..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,90);
		}
		mahasiswa();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data dosen..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,95);
		}
		dosen();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Import data penugasan dosen mengajar..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,100);
		}
		penugasanDosenMengajar();
	}

	public void perguruanTinggi() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "satuan_pendidikan", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			PerguruanTinggi perguruanTinggi = FeederConverter.perguruanTinggi(node);
			if (perguruanTinggi.getKodeYayasan() == null || perguruanTinggi.getKodeYayasan().trim().isEmpty()) {
				continue;
			}
			System.out.println(perguruanTinggi);

			PerguruanTinggi existing = (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
					.add(Restrictions.eq("feeder", perguruanTinggi.getFeeder())).setMaxResults(1).uniqueResult();
			if (existing == null) {
				existing = (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
						.add(Restrictions.ilike("nama", perguruanTinggi.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = perguruanTinggi;
			}
			existing.setFeeder(perguruanTinggi.getFeeder());

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();

			session.getTransaction().begin();
			session.createSQLQuery(
					"update fakultas set perguruan_tinggi = " + existing.getId() + "  where perguruan_tinggi is null")
					.executeUpdate();
			session.createSQLQuery(
					"update dosen set perguruan_tinggi = " + existing.getId() + " where perguruan_tinggi is null")
					.executeUpdate();

			session.getTransaction().commit();

			session.flush();

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void pekerjaan() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "pekerjaan", "expired_date is null", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			Pekerjaan pekerjaan = FeederConverter.pekerjaan(node);
			System.out.println(pekerjaan);

			Pekerjaan existing = (Pekerjaan) session.createCriteria(Pekerjaan.class)
					.add(Restrictions.eq("feeder", pekerjaan.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = pekerjaan;
			}

			existing.setFeeder(pekerjaan.getFeeder());

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void penghasilan() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "penghasilan", "expired_date is null", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			Penghasilan penghasilan = FeederConverter.penghasilan(node);
			System.out.println(penghasilan);

			Penghasilan existing = (Penghasilan) session.createCriteria(Penghasilan.class)
					.add(Restrictions.eq("feeder", penghasilan.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = penghasilan;
			}

			existing.setFeeder(penghasilan.getFeeder());

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void agama() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "agama", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}

			Agama agama = FeederConverter.agama(node);
			System.out.println(agama);

			Agama existing = (Agama) session.createCriteria(Agama.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("feeder", agama.getFeeder())).setMaxResults(1).uniqueResult();
			if (existing == null) {
				existing = (Agama) session.createCriteria(Agama.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.ilike("nama", agama.getNama()))
						.setMaxResults(1).uniqueResult();
			}

			if (existing == null && agama.getNama().equalsIgnoreCase("Katholik")) {
				existing = (Agama) session.createCriteria(Agama.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.ilike("nama", "Katolik"))
						.setMaxResults(1).uniqueResult();
			}

			if (existing == null && agama.getNama().equalsIgnoreCase("Kristen")) {
				existing = (Agama) session.createCriteria(Agama.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.ilike("nama", "Protestan"))
						.setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = agama;
			}
			existing.setNama(agama.getNama());
			existing.setFeeder(agama.getFeeder());

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void jenisEvaluasi() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "jenis_evaluasi", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}

			JenisEvaluasi jenisEvaluasi = FeederConverter.jenisEvaluasi(node);
			System.out.println(jenisEvaluasi);

			JenisEvaluasi existing = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
					.add(Restrictions.eq("feeder", jenisEvaluasi.getFeeder())).setMaxResults(1).uniqueResult();
			if (existing == null) {
				existing = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
						.add(Restrictions.ilike("nama", jenisEvaluasi.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = jenisEvaluasi;
			}
			existing.setNama(jenisEvaluasi.getNama());
			existing.setFeeder(jenisEvaluasi.getFeeder());

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void statusAwalMahasiswa() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "jenis_pendaftaran", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			StatusAwalMahasiswa statusAwalMahasiswa = FeederConverter.statusAwalMahasiswa(node);
			System.out.println(statusAwalMahasiswa);

			StatusAwalMahasiswa existing = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
					.add(Restrictions.eq("feeder", statusAwalMahasiswa.getFeeder())).setMaxResults(1).uniqueResult();
			if (existing == null) {
				existing = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
						.add(Restrictions.ilike("nama", statusAwalMahasiswa.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null && statusAwalMahasiswa.getNama().equalsIgnoreCase("Peserta didik baru")) {
				session.createSQLQuery("update status_awal_mahasiswa set feeder = " + statusAwalMahasiswa.getFeeder()
						+ " where nama ilike '%baru%'").executeUpdate();
				continue;
			}

			if (existing == null && statusAwalMahasiswa.getNama().equalsIgnoreCase("Pindahan Alih Bentuk")) {
				existing = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
						.add(Restrictions.ilike("nama", "Alih Prodi")).setMaxResults(1).uniqueResult();
			}

			if (existing == null && statusAwalMahasiswa.getNama().equalsIgnoreCase("Lainnya")) {
				session.createSQLQuery("update status_awal_mahasiswa set feeder = " + statusAwalMahasiswa.getFeeder()
						+ " where nama in ('Transfer','PKU','Prog. Khusus')").executeUpdate();
				continue;
			}

			if (existing == null) {
				existing = statusAwalMahasiswa;
				existing.setKode(statusAwalMahasiswa.getFeeder() + "");
			}
			existing.setFeeder(statusAwalMahasiswa.getFeeder());

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	@SuppressWarnings("unchecked")
	public void wilayah() throws Exception {

		for (int i = 0; i < 1000000; i += FeederImporter.JUMLAH_SEKALI_AMBIL_DATA) {
			List<Node> result = feederConnector.getRecordset(token, "wilayah", "", "id_wil",
					FeederImporter.JUMLAH_SEKALI_AMBIL_DATA, i);
			if (result.isEmpty()) {
				break;
			}
			Session session = HibernateUtil.currentNativeSession();
			int index = 0;
			for (Node node : result) {

				if (progressmeterChild != null) {
					NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
					index++;
				}
				Wilayah wilayah = FeederConverter.wilayah(node);
				System.out.println(wilayah);
				if (wilayah.getFeeder() == null || wilayah.getFeeder().trim().isEmpty()) {
					continue;
				}

				Wilayah existing = (Wilayah) session.createCriteria(Wilayah.class)
						.add(Restrictions.sqlRestriction("trim(feeder) = '" + wilayah.getFeeder().trim() + "'"))
						.setMaxResults(1).uniqueResult();
				if (existing == null) {
					existing = (Wilayah) session.createCriteria(Wilayah.class)
							.add(Restrictions.ilike("nama", wilayah.getNama())).setMaxResults(1).uniqueResult();
				}

				if (existing == null) {
					existing = wilayah;
				}
				existing.setFeeder(wilayah.getFeeder().trim());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, existing);
				session.getTransaction().commit();
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}

		Session session = HibernateUtil.currentNativeSession();
		List<Wilayah> wilayahs = session.createCriteria(Wilayah.class).add(Restrictions.isNull("wilayahInduk"))
				.add(Restrictions.isNotNull("induk")).add(Restrictions.ne("induk", "")).list();

		for (Wilayah wilayah : wilayahs) {
			Wilayah wilayahInduk = (Wilayah) session.createCriteria(Wilayah.class)
					.add(Restrictions.eq("feeder", wilayah.getInduk())).setMaxResults(1).uniqueResult();
			wilayah.setWilayahInduk(wilayahInduk);
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, wilayah);
			session.getTransaction().commit();
		}
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		wilayahs = null;

	}

	public void kebutuhanKhusus() throws Exception {

		for (int i = 0; i < 1000000; i += FeederImporter.JUMLAH_SEKALI_AMBIL_DATA) {

			List<Node> result = feederConnector.getRecordset(token, "kebutuhan_khusus", "", "id_kk",
					FeederImporter.JUMLAH_SEKALI_AMBIL_DATA, i);
			if (result.isEmpty()) {
				break;
			}

			Session session = HibernateUtil.currentNativeSession();
			int index = 0;
			for (Node node : result) {

				if (progressmeterChild != null) {
					NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
					index++;
				}
				KebutuhanKhusus kebutuhanKhusus = FeederConverter.kebutuhanKhusus(node);
				System.out.println(kebutuhanKhusus);
				if (kebutuhanKhusus.getFeeder() == null) {
					continue;
				}

				KebutuhanKhusus existing = (KebutuhanKhusus) session.createCriteria(KebutuhanKhusus.class)
						.add(Restrictions.eq("feeder", kebutuhanKhusus.getFeeder())).setMaxResults(1).uniqueResult();

				if (existing == null) {
					existing = kebutuhanKhusus;
				} else {
					kebutuhanKhusus.setId(existing.getId());
				}

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, existing);
				session.getTransaction().commit();
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
	}

	public void jenjang() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "jenjang_pendidikan", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			Jenjang jenjang = FeederConverter.jenjang(node);
			System.out.println(jenjang + "-" + jenjang.getFeeder());
			if (jenjang.getFeeder() == null) {
				continue;
			}

			Jenjang existing = (Jenjang) session.createCriteria(Jenjang.class)
					.add(Restrictions.eq("feeder", jenjang.getFeeder())).setMaxResults(1).uniqueResult();
			if (existing == null) {
				existing = (Jenjang) session.createCriteria(Jenjang.class)
						.add(Restrictions.ilike("nama", jenjang.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null && jenjang.getNama().equalsIgnoreCase("S1")) {
				existing = (Jenjang) session.createCriteria(Jenjang.class).add(Restrictions.ilike("nama", "Strata 1"))
						.setMaxResults(1).uniqueResult();
			}

			if (existing == null && jenjang.getNama().equalsIgnoreCase("S2")) {
				existing = (Jenjang) session.createCriteria(Jenjang.class).add(Restrictions.ilike("nama", "Strata 2"))
						.setMaxResults(1).uniqueResult();
			}

			if (existing == null && jenjang.getNama().equalsIgnoreCase("S3")) {
				existing = (Jenjang) session.createCriteria(Jenjang.class).add(Restrictions.ilike("nama", "Strata 3"))
						.setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = jenjang;
			}
			existing.setNama(jenjang.getNama());
			existing.setFeeder(jenjang.getFeeder());

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void ikatanKerjaDosen() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "ikatan_kerja_dosen", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			IkatanKerjaDosen ikatanKerjaDosen = FeederConverter.ikatanKerjaDosen(node);
			System.out.println(ikatanKerjaDosen + "-" + ikatanKerjaDosen.getFeeder());
			if (ikatanKerjaDosen.getFeeder() == null) {
				continue;
			}

			IkatanKerjaDosen existing = (IkatanKerjaDosen) session.createCriteria(IkatanKerjaDosen.class)
					.add(Restrictions.eq("feeder", ikatanKerjaDosen.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = ikatanKerjaDosen;
			} else {
				ikatanKerjaDosen.setId(existing.getId());
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, ikatanKerjaDosen);
			session.getTransaction().commit();

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void statusKepegawaian() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "status_kepegawaian", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			StatusKepegawaian statusKepegawaian = FeederConverter.statusKepegawaian(node);
			System.out.println(statusKepegawaian + "-" + statusKepegawaian.getFeeder());
			if (statusKepegawaian.getFeeder() == null) {
				continue;
			}

			StatusKepegawaian existing = (StatusKepegawaian) session.createCriteria(StatusKepegawaian.class)
					.add(Restrictions.eq("feeder", statusKepegawaian.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = statusKepegawaian;
			} else {
				statusKepegawaian.setId(existing.getId());
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, statusKepegawaian);
			session.getTransaction().commit();

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void lembagaPengangkat() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "lembaga_pengangkat", "", "", 1000, 0);
		for (Node node : result) {
			LembagaPengangkat lembagaPengangkat = FeederConverter.lembagaPengangkat(node);
			System.out.println(lembagaPengangkat + "-" + lembagaPengangkat.getFeeder());
			if (lembagaPengangkat.getFeeder() == null) {
				continue;
			}

			LembagaPengangkat existing = (LembagaPengangkat) session.createCriteria(LembagaPengangkat.class)
					.add(Restrictions.eq("feeder", lembagaPengangkat.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = lembagaPengangkat;
			} else {
				lembagaPengangkat.setId(existing.getId());
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, lembagaPengangkat);
			session.getTransaction().commit();

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void statusPegawai() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "status_keaktifan_pegawai", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			StatusPegawai statusPegawai = FeederConverter.statusPegawai(node);
			System.out.println(statusPegawai + "-" + statusPegawai.getFeeder());
			if (statusPegawai.getFeeder() == null) {
				continue;
			}

			StatusPegawai existing = (StatusPegawai) session.createCriteria(StatusPegawai.class)
					.add(Restrictions.eq("feeder", statusPegawai.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = (StatusPegawai) session.createCriteria(StatusPegawai.class)
						.add(Restrictions.ilike("nama", statusPegawai.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null && statusPegawai.getNama().equalsIgnoreCase("ALMARHUM")) {
				existing = (StatusPegawai) session.createCriteria(StatusPegawai.class)
						.add(Restrictions.ilike("nama", "Meninggal")).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = statusPegawai;
			} else {
				statusPegawai.setId(existing.getId());
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, statusPegawai);
			session.getTransaction().commit();

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void golongan() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "pangkat_golongan", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			Golongan golongan = FeederConverter.golongan(node);
			System.out.println(golongan + "-" + golongan.getFeeder());
			if (golongan.getFeeder() == null) {
				continue;
			}

			Golongan existing = (Golongan) session.createCriteria(Golongan.class)
					.add(Restrictions.eq("feeder", golongan.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = (Golongan) session.createCriteria(Golongan.class)
						.add(Restrictions.ilike("nama", golongan.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = (Golongan) session.createCriteria(Golongan.class)
						.add(Restrictions.ilike("pangkat", golongan.getPangkat())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = golongan;
			} else {
				golongan.setId(existing.getId());
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, golongan);
			session.getTransaction().commit();

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void nilaiHuruf() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "bobot_nilai", "", "", 1000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			NilaiHuruf nilaiHuruf = FeederConverter.nilaiHuruf(node, session);
			System.out.println(nilaiHuruf + "-" + nilaiHuruf.getFeeder());
			if (nilaiHuruf.getFeeder() == null || nilaiHuruf.getJurusan() == null) {
				continue;
			}

			NilaiHuruf existing = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
					.add(Restrictions.eq("feeder", nilaiHuruf.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
						.add(Restrictions.ilike("nilaiHuruf", nilaiHuruf.getNilaiHuruf()))
						.add(Restrictions.eq("jurusan", nilaiHuruf.getJurusan())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				continue;
			} else {
				existing.setFeeder(nilaiHuruf.getFeeder());
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void fakultas() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "sms", "id_jns_sms=1", "id_sms", 1000, 0);

		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			Fakultas fakultas = FeederConverter.fakultas(node, session);
			System.out.println(fakultas);

			Fakultas existing = (Fakultas) session.createCriteria(Fakultas.class)
					.add(Restrictions.eq("feeder", fakultas.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = (Fakultas) session.createCriteria(Fakultas.class)
						.add(Restrictions.eq("kode", fakultas.getKode())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = (Fakultas) session.createCriteria(Fakultas.class)
						.add(Restrictions.ilike("nama", fakultas.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				System.out.println("fakultas " + fakultas + " tidak ada");
				existing = fakultas;
			} else {
				fakultas.setId(existing.getId());
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		int countFakultas = ((Number) session.createCriteria(Fakultas.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		Fakultas tempFakultas = null;
		if (countFakultas == 0) {
			tempFakultas = new Fakultas();
			tempFakultas.setNama("-");
			tempFakultas.setKode("-");
			session.getTransaction().begin();
			session.save(tempFakultas);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void grupJurusan() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "sms", "id_jns_sms=2", "id_sms", 1000, 0);

		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			GrupJurusan grupJurusan = FeederConverter.grupJurusan(node, session);
			System.out.println(grupJurusan);

			GrupJurusan existing = (GrupJurusan) session.createCriteria(GrupJurusan.class)
					.add(Restrictions.eq("feeder", grupJurusan.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = (GrupJurusan) session.createCriteria(GrupJurusan.class)
						.add(Restrictions.eq("kode", grupJurusan.getKode())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = (GrupJurusan) session.createCriteria(GrupJurusan.class)
						.add(Restrictions.ilike("nama", grupJurusan.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				System.out.println("grupJurusan " + grupJurusan + " tidak ada");
				existing = grupJurusan;
			} else {
				grupJurusan.setId(existing.getId());
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void jurusan() throws Exception {

		Session session = HibernateUtil.currentNativeSession();

		Fakultas tempFakultas = (Fakultas) session.createCriteria(Fakultas.class).add(Restrictions.eq("nama", "-"))
				.add(Restrictions.eq("kode", "-")).setMaxResults(1).uniqueResult();

		List<Node> result = feederConnector.getRecordset(token, "sms", "id_jns_sms=3", "id_sms", 1000, 0);

		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			Jurusan jurusan = FeederConverter.jurusan(node, session);
			System.out.println(jurusan + " - " + jurusan.getJenjang() + "-" + jurusan.getKodeEpsbed());

			Jurusan existing = (Jurusan) session.createCriteria(Jurusan.class)
					.add(Restrictions.eq("feeder", jurusan.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = (Jurusan) session.createCriteria(Jurusan.class)
						.add(Restrictions.eq("kodeEpsbed", jurusan.getKodeEpsbed())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = (Jurusan) session.createCriteria(Jurusan.class)
						.add(Restrictions.ilike("nama", jurusan.getNama()))
						.add(Restrictions.eq("jenjang", jurusan.getJenjang())).setMaxResults(1).uniqueResult();
			}

			if (existing == null && tempFakultas == null) {
				System.out.println("jurusan " + jurusan + " tidak ada");
				continue;
			} else if (existing != null) {
				existing = FeederUtil.copyDataJikaKosong(jurusan, existing, Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			} else {
				existing = jurusan;
			}

			if (tempFakultas != null && existing.getFakultas() == null) {
				existing = jurusan;
				existing.setFakultas(tempFakultas);
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void matakuliah() throws Exception {

		for (int i = 0; i < 1000000; i += FeederImporter.JUMLAH_SEKALI_AMBIL_DATA) {

			List<Node> result = feederConnector.getRecordset(token, "mata_kuliah", "", "id_sms",
					FeederImporter.JUMLAH_SEKALI_AMBIL_DATA, i);
			if (result.isEmpty()) {
				break;
			}
			Session session = HibernateUtil.currentNativeSession();

			int index = 0;
			for (Node node : result) {

				if (progressmeterChild != null) {
					NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
					index++;
				}
				Matakuliah matakuliah = FeederConverter.matakuliah(node, session);
				if (matakuliah.getJurusan() == null) {
					continue;
				}
				if (matakuliah.getKode() == null || matakuliah.getKode().trim().isEmpty()) {
					continue;
				}

				Matakuliah existing = null;

				if (existing == null && matakuliah.getKode() != null) {
					existing = (Matakuliah) session.createCriteria(Matakuliah.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kode", matakuliah.getKode()))
							.add(Restrictions.eq("jurusan", matakuliah.getJurusan())).setMaxResults(1).uniqueResult();
				}

				if (existing == null && matakuliah.getNama() != null) {
					existing = (Matakuliah) session.createCriteria(Matakuliah.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("nama", matakuliah.getNama()))
							.add(Restrictions.eq("jurusan", matakuliah.getJurusan())).setMaxResults(1).uniqueResult();
				}

				if (existing == null) {
					existing = matakuliah;
				} else {
					existing = FeederUtil.copyDataJikaKosong(matakuliah, existing, Matakuliah.class);
				}

				existing.setFeeder(matakuliah.getFeeder());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, existing);
				session.getTransaction().commit();

			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
	}

	public void kurikulum() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		List<Node> result = feederConnector.getRecordset(token, "kurikulum", "", "id_sms", 100000, 0);
		int index = 0;
		for (Node node : result) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
				index++;
			}
			Kurikulum kurikulum = FeederConverter.kurikulum(node, session);
			System.out.println(kurikulum + " - " + kurikulum.getJurusan() + " - " + kurikulum.getTahun());
			if (kurikulum.getJurusan() == null) {
				continue;
			}

			int count = ((Number) session.createCriteria(Kurikulum.class)
					.add(Restrictions.eq("feeder", kurikulum.getFeeder())).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			if (count > 0) {
				continue;
			}

			Kurikulum existing = null;

			if (existing == null && kurikulum.getKode() != null) {
				existing = (Kurikulum) session.createCriteria(Kurikulum.class)
						.add(Restrictions.eq("tahun", kurikulum.getTahun()))
						.add(Restrictions.eq("jurusan", kurikulum.getJurusan())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = kurikulum;
			}

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
	}

	public void kurikulumPunyaMatakuliah() throws Exception {
		for (int i = 0; i < 1000000; i += FeederImporter.JUMLAH_SEKALI_AMBIL_DATA) {
			List<Node> result = feederConnector.getRecordset(token, "mata_kuliah_kurikulum", "", "id_kurikulum,id_mk",
					FeederImporter.JUMLAH_SEKALI_AMBIL_DATA, i);
			if (result.isEmpty()) {
				break;
			}
			Session session = HibernateUtil.currentNativeSession();

			int index = 0;
			for (Node node : result) {

				if (progressmeterChild != null) {
					NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
					index++;
				}
				KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = FeederConverter.kurikulumPunyaMatakuliah(node,
						session);
				System.out.println(kurikulumPunyaMatakuliah);
				if (kurikulumPunyaMatakuliah.getKurikulum() == null
						|| kurikulumPunyaMatakuliah.getMatakuliah() == null) {
					continue;
				}

				int count = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
						.add(Restrictions.eq("feeder", kurikulumPunyaMatakuliah.getFeeder()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				if (count > 0) {
					continue;
				}

				KurikulumPunyaMatakuliah existing = kurikulumPunyaMatakuliah;

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, existing);
				session.getTransaction().commit();
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
	}

	public void mahasiswa() throws Exception {

		for (int i = 0; i < 1000000; i += FeederImporter.JUMLAH_SEKALI_AMBIL_DATA) {

			List<Node> result = feederConnector.getRecordset(token, "mahasiswa_pt", "", "nipd",
					FeederImporter.JUMLAH_SEKALI_AMBIL_DATA, i);
			if (result.isEmpty()) {
				break;
			}
			Session session = HibernateUtil.currentNativeSession();

			int index = 0;
			for (Node node : result) {

				try {
					if (progressmeterChild != null) {
						NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
						index++;
					}
					BiodataMahasiswa biodataMahasiswa = FeederConverter.mahasiswa(node, session);
					Mahasiswa mahasiswa = biodataMahasiswa.getMahasiswa();
					System.out.println(mahasiswa + " - " + mahasiswa.getNim() + " - " + mahasiswa.getJurusan() + " - "
							+ mahasiswa.getTahunangkatan());
					if (mahasiswa.getJurusan() == null) {
						continue;
					}

					Mahasiswa existing = null;

					if (existing == null && mahasiswa.getNim() != null) {
						existing = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.sqlRestriction(
								"replace(trim(nim),'.','') = replace(trim('" + mahasiswa.getNim() + "'),'.','')"))
								.setMaxResults(1).uniqueResult();
					}

					if (existing == null) {
						existing = mahasiswa;
					} else {
						existing = FeederUtil.copyDataJikaKosong(mahasiswa, existing, Mahasiswa.class);
					}
					existing.setFeeder(mahasiswa.getFeeder());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, existing);
					session.getTransaction().commit();

					BiodataMahasiswa biodataMahasiswaExisting = existing.ambilBiodata();
					FeederUtil.copyDataJikaKosong(biodataMahasiswa, biodataMahasiswaExisting, BiodataMahasiswa.class);
					biodataMahasiswaExisting.setMahasiswa(existing);

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, biodataMahasiswaExisting);
					session.getTransaction().commit();
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);

					try {
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						session = HibernateUtil.currentNativeSession();
					} catch (Exception ee) {
						Common.tampilErrorJikaAdmin(ee);
					}

				}
			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
	}

	public void dosen() throws Exception {

		for (int i = 0; i < 1000000; i += FeederImporter.JUMLAH_SEKALI_AMBIL_DATA) {

			List<Node> result = feederConnector.getRecordset(token, "dosen", "", "id_ptk",
					FeederImporter.JUMLAH_SEKALI_AMBIL_DATA, i);
			if (result.isEmpty()) {
				break;
			}
			Session session = HibernateUtil.currentNativeSession();

			int index = 0;
			for (Node node : result) {

				if (progressmeterChild != null) {
					NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
					index++;
				}
				BiodataDosen biodataDosen = FeederConverter.dosen(node, session);
				Dosen dosen = biodataDosen.getDosen();

				System.out.println(
						dosen + " - " + dosen.getNidn() + " - " + dosen.getMycode() + " - " + dosen.getIdRegPtk());
				Dosen existing = null;
				if (dosen.getNidn() != null && !dosen.getNidn().trim().isEmpty()) {
					existing = (Dosen) session.createCriteria(Dosen.class)
							.add(Restrictions.sqlRestriction(
									"replace(trim(nidn),'.','') = replace(trim('" + dosen.getNidn() + "'),'.','')"))
							.setMaxResults(1).uniqueResult();
				}

				if (existing == null && dosen.getCode() != null && !dosen.getCode().trim().isEmpty()) {
					existing = (Dosen) session.createCriteria(Dosen.class)
							.add(Restrictions.sqlRestriction(
									"replace(trim(code),'.','') = replace(trim('" + dosen.getCode() + "'),'.','')"))
							.setMaxResults(1).uniqueResult();
				}

				if (existing == null && dosen.getMycode() != null && !dosen.getMycode().trim().isEmpty()) {
					existing = (Dosen) session.createCriteria(Dosen.class)
							.add(Restrictions.sqlRestriction(
									"replace(trim(mycode),'.','') = replace(trim('" + dosen.getMycode() + "'),'.','')"))
							.setMaxResults(1).uniqueResult();
				}

				if (existing == null) {
					existing = dosen;
				} else {
					existing = FeederUtil.copyDataJikaKosong(dosen, existing, Dosen.class);
				}

				existing.setFeeder(dosen.getFeeder());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, existing);
				session.getTransaction().commit();

				BiodataDosen biodataDosenExisting = (BiodataDosen) session.createCriteria(BiodataDosen.class)
						.add(Restrictions.eq("dosen", existing)).setMaxResults(1).uniqueResult();
				if (biodataDosenExisting == null) {
					biodataDosenExisting = new BiodataDosen();
				}
				FeederUtil.copyDataJikaKosong(biodataDosen, biodataDosenExisting, BiodataDosen.class);
				biodataDosenExisting.setDosen(existing);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, biodataDosenExisting);
				session.getTransaction().commit();

			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
	}

	// public void dosen_pt() throws Exception {
	//
	// penugasanDosenMengajar();
	// }

	public void penugasanDosenMengajar() throws Exception {

		for (int i = 0; i < 1000000; i += FeederImporter.JUMLAH_SEKALI_AMBIL_DATA) {

			List<Node> result = feederConnector.getRecordset(token, "dosen_pt", "", "id_reg_ptk",
					FeederImporter.JUMLAH_SEKALI_AMBIL_DATA, i);
			if (result.isEmpty()) {
				break;
			}
			Session session = HibernateUtil.currentNativeSession();

			int index = 0;
			for (Node node : result) {

				if (progressmeterChild != null) {
					NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / result.size()));
					index++;
				}
				PenugasanDosenMengajar penugasanDosenMengajar = FeederConverter.penugasanDosenMengajar(node, session);

				if (penugasanDosenMengajar.getKode() == null || penugasanDosenMengajar.getKode().trim().isEmpty()
						|| penugasanDosenMengajar.getKode().trim().equals("-")) {
					continue;
				}

				System.out.println(penugasanDosenMengajar + " - " + penugasanDosenMengajar.getDosen());
				PenugasanDosenMengajar existing = (PenugasanDosenMengajar) session
						.createCriteria(PenugasanDosenMengajar.class)
						.add(Restrictions.eq("feeder", penugasanDosenMengajar.getFeeder())).setMaxResults(1)
						.uniqueResult();

				if (existing == null) {
					existing = penugasanDosenMengajar;
				} else {
					existing = FeederUtil.copyDataJikaKosong(penugasanDosenMengajar, existing,
							PenugasanDosenMengajar.class);
				}

				existing.setFeeder(penugasanDosenMengajar.getFeeder());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, existing);
				session.getTransaction().commit();

			}

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
		}
	}
}
