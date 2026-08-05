package ais.action.master.feeder.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.NilaiHurufExport;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;

public class FeederJSONExport {
	static {
		File file = new File("/opt/.g/.h/xxyxyx.txt");
		file.getParentFile().mkdirs();
		Properties properties = System.getProperties();
		try {
			properties.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			Common.tampilErrorJikaAdmin(e);
		} catch (IOException e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public Integer ta_mahasiswa_pt = null;
	public Fakultas fakultas_mahasiswa_pt = null;
	public Jurusan jurusan_mahasiswa_pt = null;

	public Integer ta_mahasiswa = null;
	public Fakultas fakultas_mahasiswa = null;
	public Jurusan jurusan_mahasiswa = null;

	public Integer ta_kelas_kuliah = null;
	public Fakultas fakultas_kelas_kuliah = null;
	public Jurusan jurusan_kelas_kuliah = null;

	public Integer ta_ajar_dosen = null;
	public Fakultas fakultas_ajar_dosen = null;
	public Jurusan jurusan_ajar_dosen = null;

	public Integer semester_kuliah_mahasiswa = null;
	public Integer ta_kuliah_mahasiswa = null;
	public Fakultas fakultas_kuliah_mahasiswa = null;
	public Jurusan jurusan_kuliah_mahasiswa = null;

	private File file;
	private Progressmeter myProgressmeter = null;
	private Progressmeter myProgressmeterChild = null;
	private Label myLabelProses = null;

	public Set<String> tables = new HashSet<String>();
	public Integer ta_kurikulum;
	public Fakultas fakultas_kurikulum;
	public Jurusan jurusan_kurikulum;
	public String matkul;
	public Fakultas fakultas_mata_kuliah_kurikulum;
	public Jurusan jurusan_mata_kuliah_kurikulum;
	public Fakultas fakultas_matkul;
	public Jurusan jurusan_matkul;

	public Integer ta_dosen_pt;
	public Fakultas fakultas_dosen_pt;
	public Jurusan jurusan_dosen_pt;

	public Integer ta_nilai = null;
	public Fakultas fakultas_nilai = null;
	public Jurusan jurusan_nilai = null;
	public String nama_nilai = "";

	public Integer ta_nilai_transfer = null;
	public Fakultas fakultas_nilai_transfer = null;
	public Jurusan jurusan_nilai_transfer = null;
	public String nama_nilai_transfer = "";

	public Integer ta_krs = null;
	public Fakultas fakultas_krs = null;
	public Jurusan jurusan_krs = null;
	public String nama_krs = "";

	public FeederJSONExport(File file, Set<String> tables) {
		this.file = file;
		this.tables = tables;
	}

	public FeederJSONExport(File file, Set<String> tables, Progressmeter myProgressmeter,
			Progressmeter myProgressmeterChild, Label myLabelProses) {
		this.file = file;
		this.tables = tables;
		this.myProgressmeter = myProgressmeter;
		this.myProgressmeterChild = myProgressmeterChild;
		this.myLabelProses = myLabelProses;
	}

	public void proses() throws Exception {
		JSONArray jSONArrayGlobal = new JSONArray();
		if (myProgressmeter != null) {
			myProgressmeter.setValue(0);
		}
		if (tables.contains("bobot_nilai")) {
			jSONArrayGlobal.put(nilaiHuruf());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(10);
		}
		if (tables.contains("mata_kuliah")) {
			jSONArrayGlobal.put(matakuliah());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(20);
		}
		if (tables.contains("kurikulum")) {
			jSONArrayGlobal.put(kurikulum());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(30);
		}
		if (tables.contains("mata_kuliah_kurikulum")) {
			jSONArrayGlobal.put(kurikulumPunyaMatakuliah());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(40);
		}
		if (tables.contains("kelas_kuliah")) {
			jSONArrayGlobal.put(kelas_kuliah());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(50);
		}
		if (tables.contains("mahasiswa")) {
			jSONArrayGlobal.put(mahasiswa());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(60);
		}
		if (tables.contains("mahasiswa_pt")) {
			jSONArrayGlobal.put(mahasiswa_pt());
		}
		if (tables.contains("dosen_pt")) {
			jSONArrayGlobal.put(dosen_pt());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(70);
		}
		if (tables.contains("ajar_dosen")) {
			jSONArrayGlobal.put(ajar_dosen());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(80);
		}
		if (tables.contains("krs")) {
			jSONArrayGlobal.put(krs());
		}
		if (tables.contains("nilai")) {
			jSONArrayGlobal.put(nilai());
		}
		if (tables.contains("nilai_transfer")) {
			jSONArrayGlobal.put(nilai_transfer());
		}
		if (myProgressmeter != null) {
			myProgressmeter.setValue(90);
		}
		if (tables.contains("kuliah_mahasiswa")) {
			jSONArrayGlobal.put(kuliah_mahasiswa());
		}

		file.getParentFile().mkdirs();
		file.createNewFile();
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(jSONArrayGlobal.toString());
		fileWriter.flush();
		fileWriter.close();

		if (myProgressmeter != null) {
			myProgressmeter.setValue(100);
		}
	}

	public JSONObject mahasiswa_pt() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		@SuppressWarnings("unchecked")
		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(ta_mahasiswa_pt == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunangkatan", ta_mahasiswa_pt))
				.add(Restrictions.isNotNull("feeder")).add(Restrictions.ne("feeder", ""))
				.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", "")).createAlias("jurusan", "jurusan")

				.add(jurusan_mahasiswa_pt == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan", jurusan_mahasiswa_pt))
				.add(fakultas_mahasiswa_pt == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_mahasiswa_pt))

				.createAlias("jurusan.fakultas", "fakultas").createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
				.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
				.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();

		System.out.println("mahasiswas size => " + mahasiswas.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Mahasiswa mahasiswa : mahasiswas) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / mahasiswas.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data mahasiswa_pt " + index + " dari " + mahasiswas.size());
			}

			BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

			JSONObject jsonObject = FeederExporterGenerator.mahasiswa_pt(mahasiswa, biodataMahasiswa, session);
			jsonObject.put("nipd", Common.maxPanjang(mahasiswa.getNim(), 18));

			JSONObject json = new JSONObject();
			json.put("class", Mahasiswa.class.getName());
			json.put("id", mahasiswa.getId());
			json.put("keyName", "id_reg_pd");
			json.put("data", jsonObject);
			if (mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().trim().isEmpty()) {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_reg_pd", mahasiswa.getIdRegPd().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
			}
			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "mahasiswa_pt");
		object.put("data", array);
		return object;
	}

	public JSONObject mahasiswa() throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		@SuppressWarnings("unchecked")
		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(ta_mahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunangkatan", ta_mahasiswa))
				.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", "")).createAlias("jurusan", "jurusan")

				.add(jurusan_mahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan", jurusan_mahasiswa))
				.add(fakultas_mahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_mahasiswa))

				.createAlias("jurusan.fakultas", "fakultas").createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
				.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
				.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();

		System.out.println("mahasiswas size => " + mahasiswas.size() + ", jurusan_mahasiswa = " + jurusan_mahasiswa
				+ ", fakultas_mahasiswa = " + fakultas_mahasiswa);
		JSONArray array = new JSONArray();
		int index = 0;
		for (Mahasiswa mahasiswa : mahasiswas) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / mahasiswas.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data mahasiswa " + index + " dari " + mahasiswas.size());
			}

			BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

			JSONObject jsonObject = FeederExporterGenerator.mahasiswa(mahasiswa, biodataMahasiswa);
			JSONObject json = new JSONObject();
			json.put("data", jsonObject);
			json.put("class", Mahasiswa.class.getName());
			json.put("keyName", "id_pd");
			json.put("id", mahasiswa.getId());
			if (mahasiswa.getFeeder() != null && !mahasiswa.getFeeder().trim().isEmpty()) {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_pd", mahasiswa.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
			}
			array.put(json);

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "mahasiswa");
		object.put("data", array);
		return object;
	}

	public JSONObject kelas_kuliah() throws Exception {

		System.out.println(
				"jurusan_kelas_kuliah=>" + jurusan_kelas_kuliah + ", fakultas_kelas_kuliah=>" + fakultas_kelas_kuliah);

		Session session = HibernateUtil.currentNativeSession();

		Integer semesterPendek = null;
		String tahunAkademik = null;
		Integer[] semesters = null;
		if (ta_kelas_kuliah != null) {
			Integer mulai = Integer.parseInt(ta_kelas_kuliah.toString().substring(0, 4));
			tahunAkademik = mulai + "/" + (mulai + 1);
			Integer s = Integer.parseInt(ta_kelas_kuliah.toString().substring(4, 5));
			semesters = s.equals(1) ? Common.ganjil : Common.genap;

			if (s.equals(3)) {
				semesterPendek = Perkuliahan.SEMESTER_PENDEK;
			}
		}

		@SuppressWarnings("unchecked")
		List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.createAlias("jurusan", "jurusan")

				.add(jurusan_kelas_kuliah == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan", jurusan_kelas_kuliah))
				.add(fakultas_kelas_kuliah == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_kelas_kuliah))

				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAjaran", tahunAkademik))

				.add(semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK)
						? Restrictions.eq("statusSemesterPendek", semesterPendek)
						: (semesters == null ? Restrictions.sqlRestriction("true")
								: Restrictions.in("semester", semesters)))

				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

		System.out.println("perkuliahans size => " + perkuliahans.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Perkuliahan perkuliahan : perkuliahans) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / perkuliahans.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data kelas_kuliah " + index + " dari " + perkuliahans.size());
			}

			JSONObject jsonObject = FeederExporterGenerator.perkuliahan(perkuliahan);

			JSONObject json = new JSONObject();
			json.put("data", jsonObject);
			json.put("class", Perkuliahan.class.getName());
			json.put("id", perkuliahan.getId());
			json.put("keyName", "id_kls");
			if (perkuliahan.getFeeder() != null && !perkuliahan.getFeeder().trim().isEmpty()) {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_kls", perkuliahan.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
			}
			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "kelas_kuliah");
		object.put("data", array);
		return object;
	}

	public JSONObject kurikulumPunyaMatakuliah() throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		@SuppressWarnings("unchecked")
		List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
				.createCriteria(KurikulumPunyaMatakuliah.class)

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.createAlias("kurikulum", "kurikulum")

				.add(Restrictions.or(Restrictions.isNull("kurikulum.aktif"), Restrictions.eq("kurikulum.aktif", true)))

				.createAlias("kurikulum.jurusan", "jurusan").createAlias("matakuliah", "matakuliah")
				.add(Restrictions.isNotNull("kurikulum.feeder")).add(Restrictions.ne("kurikulum.feeder", ""))

				.add(fakultas_mata_kuliah_kurikulum == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_mata_kuliah_kurikulum))
				.add(jurusan_mata_kuliah_kurikulum == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kurikulum.jurusan", jurusan_mata_kuliah_kurikulum))

				.add(Restrictions.isNotNull("matakuliah.feeder")).add(Restrictions.ne("matakuliah.feeder", "")).list();

		System.out.println("kurikulumPunyaMatakuliahs size => " + kurikulumPunyaMatakuliahs.size());

		int index = 0;
		JSONArray array = new JSONArray();
		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / kurikulumPunyaMatakuliahs.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue(
						"Memproses data mata_kuliah_kurikulum " + index + " dari " + kurikulumPunyaMatakuliahs.size());
			}

			JSONObject jsonObject = FeederExporterGenerator.kurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);

			JSONObject json = new JSONObject();
			json.put("class", KurikulumPunyaMatakuliah.class.getName());
			json.put("id", kurikulumPunyaMatakuliah.getId());
			json.put("keyName", "id_kurikulum_sp;id_mk");
			json.put("data", jsonObject);
			Map<String, Object> dataKey = new HashMap<String, Object>();
			dataKey.put("id_kurikulum_sp", kurikulumPunyaMatakuliah.getKurikulum().getFeeder());
			dataKey.put("id_mk", kurikulumPunyaMatakuliah.getMatakuliah().getFeeder());
			JSONObject jsonObjectKey = new JSONObject(dataKey);
			json.put("key", jsonObjectKey);

			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "mata_kuliah_kurikulum");
		object.put("data", array);
		return object;
	}

	public JSONObject kurikulum() throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		@SuppressWarnings("unchecked")
		List<Kurikulum> kurikulums = session.createCriteria(Kurikulum.class)

				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(ta_kurikulum == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahun", ta_kurikulum))
				.createAlias("jurusan", "jurusan")

				.add(jurusan_kurikulum == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan", jurusan_kurikulum))
				.add(fakultas_kurikulum == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_kurikulum))

				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

		System.out.println("kurikulums size => " + kurikulums.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Kurikulum kurikulum : kurikulums) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / kurikulums.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data kurikulum " + index + " dari " + kurikulums.size());
			}

			JSONObject jsonObject = FeederExporterGenerator.kurikulum(kurikulum);

			JSONObject json = new JSONObject();
			json.put("class", Kurikulum.class.getName());
			json.put("id", kurikulum.getId());
			json.put("keyName", "id_kurikulum_sp");
			json.put("data", jsonObject);
			if (kurikulum.getFeeder() != null && !kurikulum.getFeeder().trim().isEmpty()) {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_kurikulum_sp", kurikulum.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
			}
			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "kurikulum");
		object.put("data", array);
		return object;
	}

	@SuppressWarnings("unchecked")
	public JSONObject nilaiHuruf() throws Exception {
		Session session = HibernateUtil.currentNativeSession();

		List<NilaiHuruf> nilaiHurufs1 = session.createCriteria(NilaiHuruf.class).add(Restrictions.ne("nilaiHuruf", ""))
				.add(Restrictions.isNull("jurusan")).list();

		List<Jurusan> jurusans = session.createCriteria(Jurusan.class).add(Restrictions.isNotNull("feeder"))
				.add(Restrictions.ne("feeder", "")).list();
		System.out.println("nilaiHurufs1 size => " + nilaiHurufs1.size() + ", jurusans = " + jurusans.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (NilaiHuruf nilaiHuruf : nilaiHurufs1) {
			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / nilaiHurufs1.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data bobot_nilai " + index + " dari " + nilaiHurufs1.size());
			}

			for (Jurusan jurusan : jurusans) {

				NilaiHurufExport nilaiHurufExport = (NilaiHurufExport) session.createCriteria(NilaiHurufExport.class)
						.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("nilaiHuruf", nilaiHuruf))
						.setMaxResults(1).uniqueResult();
				if (nilaiHurufExport == null) {
					nilaiHurufExport = new NilaiHurufExport();
					nilaiHurufExport.setJurusan(jurusan);
					nilaiHurufExport.setNilaiHuruf(nilaiHuruf);
					session.getTransaction().begin();
					session.save(nilaiHurufExport);
					session.getTransaction().commit();
				}

				JSONObject jsonObject = FeederExporterGenerator.nilaiHuruf(nilaiHurufExport);

				JSONObject json = new JSONObject();
				json.put("class", NilaiHurufExport.class.getName());
				json.put("id", nilaiHurufExport.getId());
				json.put("data", jsonObject);
				json.put("keyName", "kode_bobot_nilai");
				if (nilaiHurufExport.getFeeder() != null && !nilaiHurufExport.getFeeder().trim().isEmpty()) {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("kode_bobot_nilai", nilaiHurufExport.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					json.put("key", jsonObjectKey);
				}
				array.put(json);
			}
		}

		List<NilaiHuruf> nilaiHurufs = session.createCriteria(NilaiHuruf.class).add(Restrictions.ne("nilaiHuruf", ""))
				.createAlias("jurusan", "jurusan").add(Restrictions.isNotNull("jurusan.feeder"))
				.add(Restrictions.ne("jurusan.feeder", "")).list();

		System.out.println("nilaiHurufs size => " + nilaiHurufs.size());
		index = 0;
		for (NilaiHuruf nilaiHuruf : nilaiHurufs) {
			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / nilaiHurufs.size()));
				index++;
			}

			NilaiHurufExport nilaiHurufExport = (NilaiHurufExport) session.createCriteria(NilaiHurufExport.class)
					.add(Restrictions.eq("jurusan", nilaiHuruf.getJurusan()))
					.add(Restrictions.eq("nilaiHuruf", nilaiHuruf)).setMaxResults(1).uniqueResult();
			if (nilaiHurufExport == null) {
				nilaiHurufExport = new NilaiHurufExport();
				nilaiHurufExport.setJurusan(nilaiHuruf.getJurusan());
				nilaiHurufExport.setNilaiHuruf(nilaiHuruf);
				session.getTransaction().begin();
				session.save(nilaiHurufExport);
				session.getTransaction().commit();
			}

			JSONObject jsonObject = FeederExporterGenerator.nilaiHuruf(nilaiHurufExport);
			JSONObject json = new JSONObject();
			json.put("class", NilaiHurufExport.class.getName());
			json.put("id", nilaiHurufExport.getId());
			json.put("data", jsonObject);
			json.put("keyName", "kode_bobot_nilai");
			if (nilaiHurufExport.getFeeder() != null && !nilaiHurufExport.getFeeder().trim().isEmpty()) {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("kode_bobot_nilai", nilaiHurufExport.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
			}
			array.put(json);

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "bobot_nilai");
		object.put("data", array);
		return object;
	}

	public JSONObject matakuliah() throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		@SuppressWarnings("unchecked")
		List<Matakuliah> matakuliahs = session.createCriteria(Matakuliah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(matkul == null || matkul.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", matkul, MatchMode.ANYWHERE),
								Restrictions.ilike("nama", matkul, MatchMode.ANYWHERE)))

				.createAlias("jurusan", "jurusan")

				.add(jurusan_matkul == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan", jurusan_matkul))

				.add(fakultas_matkul == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_matkul))

				.add(Restrictions.ne("kode", "")).add(Restrictions.ne("nama", ""))
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

		System.out.println("matakuliahs size => " + matakuliahs.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Matakuliah matakuliah : matakuliahs) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / matakuliahs.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data matakuliah " + index + " dari " + matakuliahs.size());
			}

			JSONObject jsonObject = FeederExporterGenerator.matakuliah(matakuliah);

			JSONObject json = new JSONObject();
			json.put("data", jsonObject);
			json.put("class", Matakuliah.class.getName());
			json.put("id", matakuliah.getId());
			json.put("keyName", "id_mk");
			if (matakuliah.getFeeder() != null && !matakuliah.getFeeder().trim().isEmpty()) {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_mk", matakuliah.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
			}
			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "mata_kuliah");
		object.put("data", array);
		return object;
	}

	public JSONObject dosen_pt() throws Exception {

		String tahunAkademik = null;
		String semesters = null;
		if (ta_dosen_pt != null) {
			Integer mulai = Integer.parseInt(ta_dosen_pt.toString().substring(0, 4));
			tahunAkademik = mulai + "/" + (mulai + 1);
			Integer s = Integer.parseInt(ta_dosen_pt.toString().substring(4, 5));
			semesters = s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}

		Session session = HibernateUtil.currentNativeSession();
		@SuppressWarnings("unchecked")
		List<PenugasanDosenMengajar> penugasanDosenMengajars = session.createCriteria(PenugasanDosenMengajar.class)

				.createAlias("jurusan", "jurusan")

				.add(jurusan_dosen_pt == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan", jurusan_dosen_pt))
				.add(fakultas_dosen_pt == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_dosen_pt))

				.createAlias("dosen", "dosen").createAlias("dosen.perguruanTinggi", "perguruanTinggi")
				.add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semesters == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semesters))
				.add(Restrictions.isNotNull("perguruanTinggi.feeder")).add(Restrictions.isNotNull("dosen.feeder"))
				.list();

		System.out.println("penugasanDosenMengajar size => " + penugasanDosenMengajars.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (PenugasanDosenMengajar penugasanDosenMengajar : penugasanDosenMengajars) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / penugasanDosenMengajars.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data dosen_pt " + index + " dari " + penugasanDosenMengajars.size());
			}

			JSONObject jsonObject = FeederExporterGenerator.dosen_pt(penugasanDosenMengajar, session);

			JSONObject json = new JSONObject();
			json.put("class", PenugasanDosenMengajar.class.getName());
			json.put("id", penugasanDosenMengajar.getId());
			json.put("keyName", "id_reg_ptk");
			json.put("data", jsonObject);
			if (penugasanDosenMengajar.getFeeder() != null && !penugasanDosenMengajar.getFeeder().trim().isEmpty()) {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_reg_ptk", penugasanDosenMengajar.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
			}
			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "dosen_pt");
		object.put("data", array);
		return object;
	}

	public JSONObject ajar_dosen() throws Exception {

		System.out
				.println("jurusan_ajar_dosen=>" + jurusan_ajar_dosen + ", fakultas_ajar_dosen=>" + fakultas_ajar_dosen);

		Session session = HibernateUtil.currentNativeSession();
		String tahunAkademik = null;
		Integer[] semesters = null;
		if (ta_ajar_dosen != null) {
			Integer mulai = Integer.parseInt(ta_ajar_dosen.toString().substring(0, 4));
			tahunAkademik = mulai + "/" + (mulai + 1);
			Integer s = Integer.parseInt(ta_ajar_dosen.toString().substring(4, 5));
			semesters = s.equals(1) ? Common.ganjil : Common.genap;
		}

		@SuppressWarnings("unchecked")
		List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.createAlias("jurusan", "jurusan")

				.add(jurusan_ajar_dosen == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan", jurusan_ajar_dosen))
				.add(fakultas_ajar_dosen == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_ajar_dosen))

				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAjaran", tahunAkademik))
				.add(semesters == null ? Restrictions.sqlRestriction("true") : Restrictions.in("semester", semesters))
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

		System.out.println("perkuliahans size => " + perkuliahans.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Perkuliahan perkuliahan : perkuliahans) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / perkuliahans.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data ajar_dosen " + index + " dari " + perkuliahans.size());
			}

			Map<String, Dosen> dosens = perkuliahan.populateDosen();

			int i = 1;
			for (Dosen dosen : dosens.values()) {
				if (dosen != null && !dosen.getNidn().trim().isEmpty()) {

					
					PenugasanDosenMengajar penugasanDosenMengajar = Common.getPenugasanDosenMengajar(
							perkuliahan.getJurusan().getId(), perkuliahan.getProgram(), perkuliahan.getTahunAjaran(),
							perkuliahan.getGanjilGenap(), perkuliahan.getMatakuliah().getSks(), dosen);

					Integer jumlahPertemuan = ((Number) session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.rowCount()).add(Restrictions.eq("perkuliahan", perkuliahan))
							.uniqueResult()).intValue();

					JSONObject jsonObject = FeederExporterGenerator.ajar_dosen(dosen, perkuliahan, jumlahPertemuan);

					JSONObject json = new JSONObject();
					json.put("data", jsonObject);
					json.put("class", PenugasanDosenMengajar.class.getName());
					json.put("id", penugasanDosenMengajar.getId());
					json.put("keyName", "id_ajar");

					ClassMetadata metadata = HibernateUtil.getClassMetadata(Perkuliahan.class);
					String id_ajar = (String) metadata.getPropertyValue(perkuliahan, "feeder" + i, EntityMode.POJO);

					if (id_ajar != null && !id_ajar.trim().isEmpty()) {
						Map<String, Object> dataKey = new HashMap<String, Object>();
						dataKey.put("id_ajar", id_ajar.trim());
						JSONObject jsonObjectKey = new JSONObject(dataKey);
						json.put("key", jsonObjectKey);
					}
					array.put(json);

				}
				i++;
			}
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "ajar_dosen");
		object.put("data", array);
		return object;
	}

	public JSONObject nilai() throws Exception {

		System.out.println("jurusan_nilai=>" + jurusan_nilai + ", fakultas_nilai=>" + fakultas_nilai);

		Session session = HibernateUtil.currentNativeSession();

		String tahunAkademik = null;
		Integer[] semesters = null;
		if (ta_nilai != null) {
			Integer mulai = Integer.parseInt(ta_nilai.toString().substring(0, 4));
			tahunAkademik = mulai + "/" + (mulai + 1);
			Integer s = Integer.parseInt(ta_nilai.toString().substring(4, 5));
			semesters = s.equals(1) ? Common.ganjil : Common.genap;
		}

		@SuppressWarnings("unchecked")
		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
				.add(Restrictions.gt("totalNilai", 0.1))
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semesters == null ? Restrictions.sqlRestriction("true") : Restrictions.in("semester", semesters))
				.createAlias("mahasiswa", "mahasiswa")

				.add(nama_nilai.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("mahasiswa.nim", nama_nilai, MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama_nilai, MatchMode.ANYWHERE)))

				.createAlias("mahasiswa.jurusan", "jurusan")

				.add(jurusan_nilai == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.jurusan", jurusan_nilai))
				.add(fakultas_nilai == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_nilai))

				.createAlias("perkuliahan", "perkuliahan").add(Restrictions.isNotNull("mahasiswa.idRegPd"))
				.add(Restrictions.ne("mahasiswa.idRegPd", "")).add(Restrictions.isNotNull("perkuliahan.feeder"))
				.add(Restrictions.ne("perkuliahan.feeder", "")).list();

		System.out.println("detailperkuliahans size => " + detailperkuliahans.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / detailperkuliahans.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data nilai " + index + " dari " + detailperkuliahans.size());
			}

			JSONObject jsonObject = FeederExporterGenerator.nilai(detailperkuliahan);
			JSONObject json = new JSONObject();
			json.put("data", jsonObject);
			json.put("class", Detailperkuliahan.class.getName());
			json.put("id", detailperkuliahan.getId());
			json.put("keyName", "id_kls;id_reg_pd");
			Map<String, Object> dataKey = new HashMap<String, Object>();
			dataKey.put("id_kls", jsonObject.getString("id_kls"));
			dataKey.put("id_reg_pd", jsonObject.getString("id_reg_pd"));
			JSONObject jsonObjectKey = new JSONObject(dataKey);
			json.put("key", jsonObjectKey);
			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "nilai");
		object.put("data", array);
		return object;
	}

	public JSONObject nilai_transfer() throws Exception {

		System.out.println("jurusan_nilai_transfer=>" + jurusan_nilai_transfer + ", fakultas_nilai_transfer=>"
				+ fakultas_nilai_transfer);

		Session session = HibernateUtil.currentNativeSession();

		String tahunAkademik = null;
		Integer[] semesters = null;
		if (ta_nilai_transfer != null) {
			Integer mulai = Integer.parseInt(ta_nilai_transfer.toString().substring(0, 4));
			tahunAkademik = mulai + "/" + (mulai + 1);
			Integer s = Integer.parseInt(ta_nilai_transfer.toString().substring(4, 5));
			semesters = s.equals(1) ? Common.ganjil : Common.genap;
		}

		@SuppressWarnings("unchecked")
		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
				.add(Restrictions.gt("totalNilai", 0.1))
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semesters == null ? Restrictions.sqlRestriction("true") : Restrictions.in("semester", semesters))
				.createAlias("mahasiswa", "mahasiswa")

				.add(nama_nilai_transfer.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("mahasiswa.nim", nama_nilai_transfer, MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama_nilai_transfer, MatchMode.ANYWHERE)))

				.createAlias("mahasiswa.jurusan", "jurusan")

				.add(jurusan_nilai_transfer == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.jurusan", jurusan_nilai_transfer))
				.add(fakultas_nilai_transfer == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_nilai_transfer))

				.createAlias("matakuliahKonversi", "matakuliahKonversi")
				.add(Restrictions.isNotNull("mahasiswa.idRegPd")).add(Restrictions.ne("mahasiswa.idRegPd", ""))
				.add(Restrictions.isNotNull("matakuliahKonversi.feeder"))
				.add(Restrictions.ne("matakuliahKonversi.feeder", "")).list();

		System.out.println("detailperkuliahans size => " + detailperkuliahans.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / detailperkuliahans.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data nilai " + index + " dari " + detailperkuliahans.size());
			}

			JSONObject jsonObject = FeederExporterGenerator.nilai_transfer(detailperkuliahan);
			JSONObject json = new JSONObject();
			json.put("data", jsonObject);
			json.put("class", Detailperkuliahan.class.getName());
			json.put("id", detailperkuliahan.getId());
			json.put("keyName", "id_ekuivalensi");
			if (detailperkuliahan.getFeeder() != null && !detailperkuliahan.getFeeder().trim().isEmpty()) {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_ekuivalensi", detailperkuliahan.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
			}
			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "nilai_transfer");
		object.put("data", array);
		return object;
	}

	public JSONObject krs() throws Exception {

		System.out.println("jurusan_krs=>" + jurusan_krs + ", fakultas_krs=>" + fakultas_krs);

		Session session = HibernateUtil.currentNativeSession();

		String tahunAkademik = null;
		Integer[] semesters = null;
		if (ta_krs != null) {
			Integer mulai = Integer.parseInt(ta_krs.toString().substring(0, 4));
			tahunAkademik = mulai + "/" + (mulai + 1);
			Integer s = Integer.parseInt(ta_krs.toString().substring(4, 5));
			semesters = s.equals(1) ? Common.ganjil : Common.genap;
		}

		@SuppressWarnings("unchecked")
		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(semesters == null ? Restrictions.sqlRestriction("true") : Restrictions.in("semester", semesters))
				.createAlias("mahasiswa", "mahasiswa")

				.add(nama_krs.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("mahasiswa.nim", nama_krs, MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama_krs, MatchMode.ANYWHERE)))

				.createAlias("mahasiswa.jurusan", "jurusan")

				.add(jurusan_krs == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa.jurusan", jurusan_krs))
				.add(fakultas_krs == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_krs))

				.createAlias("perkuliahan", "perkuliahan").add(Restrictions.isNotNull("mahasiswa.idRegPd"))
				.add(Restrictions.ne("mahasiswa.idRegPd", "")).add(Restrictions.isNotNull("perkuliahan.feeder"))
				.add(Restrictions.ne("perkuliahan.feeder", "")).list();

		System.out.println("detailperkuliahans size => " + detailperkuliahans.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / detailperkuliahans.size()));
				index++;
			}

			if (myLabelProses != null) {
				myLabelProses.setValue("Memproses data nilai " + index + " dari " + detailperkuliahans.size());
			}

			JSONObject jsonObject = FeederExporterGenerator.krs(detailperkuliahan);
			JSONObject json = new JSONObject();
			json.put("data", jsonObject);
			json.put("class", Detailperkuliahan.class.getName());
			json.put("id", detailperkuliahan.getId());
			json.put("keyName", "id_kls;id_reg_pd");
			// Map<String, Object> dataKey = new HashMap<String, Object>();
			// dataKey.put("id_kls", jsonObject.getString("id_kls"));
			// dataKey.put("id_reg_pd", jsonObject.getString("id_reg_pd"));
			// JSONObject jsonObjectKey = new JSONObject(dataKey);
			// json.put("key", jsonObjectKey);
			array.put(json);
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "nilai");
		object.put("data", array);
		return object;
	}

	public JSONObject kuliah_mahasiswa() throws Exception {

		System.out.println("semester_kuliah_mahasiswa=>" + semester_kuliah_mahasiswa + " jurusan_kuliah_mahasiswa=>"
				+ jurusan_kuliah_mahasiswa + ", fakultas_kuliah_mahasiswa=>" + fakultas_kuliah_mahasiswa);

		Session session = HibernateUtil.currentNativeSession();
		@SuppressWarnings("unchecked")
		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(ta_kuliah_mahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunangkatan", ta_kuliah_mahasiswa))
				.add(Restrictions.isNotNull("idRegPd")).add(Restrictions.ne("idRegPd", ""))
				.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", "")).createAlias("jurusan", "jurusan")

				.add(jurusan_kuliah_mahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan", jurusan_kuliah_mahasiswa))

				.add(fakultas_kuliah_mahasiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas_kuliah_mahasiswa))

				.createAlias("jurusan.fakultas", "fakultas").createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
				.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
				.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();

		String tahunAkademik = null;
		String jenisSemester = null;

		try {
			if (semester_kuliah_mahasiswa != null) {
				Integer mulai = Integer.parseInt(semester_kuliah_mahasiswa.toString().substring(0, 4));
				tahunAkademik = mulai + "/" + (mulai + 1);
				Integer s = Integer.parseInt(semester_kuliah_mahasiswa.toString().substring(4, 5));
				jenisSemester = s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONExport.java:1135");

		}

		System.out.println("mahasiswas size => " + mahasiswas.size());
		JSONArray array = new JSONArray();
		int index = 0;
		for (Mahasiswa mahasiswa : mahasiswas) {

			if (myProgressmeterChild != null) {
				myProgressmeterChild.setValue((int) (index * 100.0 / mahasiswas.size()));
				index++;
			}

			if (tahunAkademik != null && jenisSemester != null) {
				int tahunAngkatanMhs = mahasiswa.getTahunangkatan();

				int semester = Common.getSemester(tahunAngkatanMhs, tahunAkademik, jenisSemester,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

				if (myLabelProses != null) {
					myLabelProses.setValue("Memproses data kuliah_mahasiswa semester " + semester + " " + index
							+ " dari " + mahasiswas.size());
				}
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null, true);
				JSONObject jsonObject = FeederExporterGenerator.kuliah_mahasiswa(session, mahasiswa, semester, null,
						krsMahasiswa);
				JSONObject json = new JSONObject();
				json.put("data", jsonObject);
				json.put("class", Mahasiswa.class.getName());
				json.put("id", mahasiswa.getId());
				json.put("keyName", "id_smt;id_reg_pd");

				String id_smt = jsonObject.getString("id_smt");

				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_smt", id_smt.trim());
				dataKey.put("id_reg_pd", mahasiswa.getIdRegPd().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				json.put("key", jsonObjectKey);
				array.put(json);
			} else {
				if (myLabelProses != null) {
					myLabelProses.setValue("Memproses data kuliah_mahasiswa " + index + " dari " + mahasiswas.size());
				}

				Number maxSemester = (Number) session.createCriteria(Detailperkuliahan.class)
						.setProjection(Projections.max("semester")).add(Restrictions.eq("mahasiswa", mahasiswa))
						.uniqueResult();
				Number minSemester = (Number) session.createCriteria(Detailperkuliahan.class)
						.setProjection(Projections.min("semester")).add(Restrictions.eq("mahasiswa", mahasiswa))
						.uniqueResult();

				if (maxSemester != null && minSemester != null) {

					for (int semester = minSemester.intValue(); semester <= maxSemester.intValue(); semester++) {
						KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null,
								true);
						JSONObject jsonObject = FeederExporterGenerator.kuliah_mahasiswa(session, mahasiswa, semester,
								null, krsMahasiswa);
						JSONObject json = new JSONObject();
						json.put("data", jsonObject);
						json.put("class", Mahasiswa.class.getName());
						json.put("id", mahasiswa.getId());
						json.put("keyName", "id_smt;id_reg_pd");

						String id_smt = jsonObject.getString("id_smt");

						Map<String, Object> dataKey = new HashMap<String, Object>();
						dataKey.put("id_smt", id_smt.trim());
						dataKey.put("id_reg_pd", mahasiswa.getIdRegPd().trim());
						JSONObject jsonObjectKey = new JSONObject(dataKey);
						json.put("key", jsonObjectKey);
						array.put(json);
					}
				}
			}

		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		JSONObject object = new JSONObject();
		object.put("table", "kuliah_mahasiswa");
		object.put("data", array);
		return object;

	}

	public static void main(String[] argv) throws Exception {

		Set<String> tables = new HashSet<String>();

		// tables.add("bobot_nilai");
		// tables.add("mata_kuliah");
		// tables.add("kurikulum");
		// tables.add("mata_kuliah_kurikulum");
		// tables.add("kelas_kuliah");
		// tables.add("mahasiswa");
		// tables.add("mahasiswa_pt");
		// tables.add("ajar_dosen");
		// tables.add("nilai");
		tables.add("kuliah_mahasiswa");

		File file = new File("/opt/hasil_export_feeder.json");
		FeederJSONExport feederJSONExport = new FeederJSONExport(file, tables);
		feederJSONExport.proses();
	}
}
