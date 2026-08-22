package ais.action.master.helper.generic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.KrsUtilHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.PaketPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GenerateKRSPaketMahasiswaOtomatisWindow extends MyWindow {

	/**
	 *
	 */
	private static final long serialVersionUID = 7041626862427552460L;
	private final PaketPerkuliahan paketPerkuliahan;
	private Intbox nimMulai;
	private Intbox nimSampai;
	private Textbox kelas;
	private Intbox semester;

	public GenerateKRSPaketMahasiswaOtomatisWindow(PaketPerkuliahan paketPerkuliahan) {
		super();
		this.paketPerkuliahan = paketPerkuliahan;
		display();
	}

	private void display() {

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(this);

		setSizable(true);
		Common.clear(this);
		setPosition("center");
		setHeight("530px");
		setWidth("550px");
		setTitle("Generate KRS Paket Mahasiswa Secara Otomatis");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(
				new ais.ui.util.MyLabelConfig(paketPerkuliahan.getKurikulum().getJurusan().getFakultas().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(new ais.ui.util.MyLabelConfig(paketPerkuliahan.getKurikulum().getJurusan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket Perkuliahan"));
		row.appendChild(new ais.ui.util.MyLabelConfig(paketPerkuliahan.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Mulai"));
		row.appendChild(nimMulai = new Intbox(0));
		// nimMulai.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Sampai"));
		row.appendChild(nimSampai = new Intbox(0));
		// nimSampai.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new Textbox(""));
		// kelas.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semester = new Intbox(1));
		// semester.setConstraint("no empty");

		South south = new South();
		south.setParent(borderlayout);

		Toolbar hbox = new Toolbar();
		hbox.setHeight("30px");
		hbox.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				GenerateKRSPaketMahasiswaOtomatisWindow.this.detach();
			}
		});
		cancel.setParent(hbox);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Generate", "/img/save.gif");
		save.setTooltiptext("Generate");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onGenerate(event)) {
					GenerateKRSPaketMahasiswaOtomatisWindow.this.detach();
				}
			}
		});
		save.setParent(hbox);
	}

	private boolean onGenerate(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("jurusan", paketPerkuliahan.getKurikulum().getJurusan()))
				.add(Restrictions.eq("semester", semester.getValue()))
				.add(Restrictions.eq("program",
						paketPerkuliahan.getKurikulum().getProgram() == null ? "Reguler"
								: paketPerkuliahan.getKurikulum().getProgram().getNama()))
				.add(Restrictions.eq("tahunAjaran", paketPerkuliahan.getTahunAkademik()))
				.add(Restrictions.ilike("kelas", kelas.getValue().trim())).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (count == 0) {
			MyMessageboxConfig.show("Jadwal perkuliahan tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		@SuppressWarnings("unchecked")
		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.sqlRestriction("to_number(nim,'99999999999999999999999') between "
						+ nimMulai.getValue() + " and " + nimSampai.getValue() + ""))
				.add(Restrictions.between("tahunangkatan", paketPerkuliahan.getAngkatanMulai(),
						paketPerkuliahan.getAngkatanSampai()))
				.list();

		if (mahasiswas.size() == 0) {
			MyMessageboxConfig.show("Data mahasiswa tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		for (Mahasiswa mahasiswa : mahasiswas) {
			System.out.println("processing mahasiswa = " + mahasiswa);
			save(mahasiswa);
		}

		return true;
	}

	@SuppressWarnings({ "unchecked" })
	public boolean save(Mahasiswa mahasiswa) throws Exception {

		if (semester.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, semester belum dipilih. Langkah yang dapat dilakukan: (1) pilih terlebih dahulu semester yang dituju pada kolom Semester; (2) pastikan semester sesuai dengan paket perkuliahan; (3) ulangi proses setelah semester dipilih.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (semester.getValue() > paketPerkuliahan.getMaxSmt() || semester.getValue() < paketPerkuliahan.getMinSmt()) {
			MyMessageboxConfig.showFormat(
					"Mohon maaf, semester {V1} tidak diizinkan untuk mengikuti paket perkuliahan \"{V2}\". Langkah yang dapat dilakukan: (1) periksa rentang semester yang diizinkan pada paket perkuliahan tersebut; (2) pilih semester yang sesuai; (3) sesuaikan pengaturan batas semester paket bila diperlukan.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
					semester, paketPerkuliahan.getNama());
			return false;
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		Session session = HibernateUtil.currentSession();

		String sql = "delete from detailperkuliahan where mahasiswa = " + mahasiswa.getId() + " and semester = "
				+ semester.getValue();
		session.createSQLQuery(sql).executeUpdate();

		List<Perkuliahan> selectedPerkuliahans = new ArrayList<Perkuliahan>();
		List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
				.createCriteria(KurikulumPunyaMatakuliah.class)
				.add(Restrictions.eq("kurikulum", paketPerkuliahan.getKurikulum()))
				.add(Restrictions.eq("semester", semester.getValue())).list();

		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {

			List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("matakuliah", kurikulumPunyaMatakuliah.getMatakuliah()))
					.add(Restrictions.isNull("statusSemesterPendek"))

					.createAlias("jurusan", "jurusan")

					.add(Restrictions.eq("jurusan", mahasiswa.getJurusan()))

					.add(Restrictions.eq("program", mahasiswa.getProgram()))

					.add(Restrictions.eq("semester", semester.getValue()))

					.add(Restrictions.eq("tahunAjaran", paketPerkuliahan.getTahunAkademik())).add(Restrictions
							.or(Restrictions.eq("merupakan_paralel", false), Restrictions.isNull("merupakan_paralel")))
					.setMaxResults(Common.MAX_RESULT).list();

			for (Perkuliahan perkuliahan : perkuliahans) {
				Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, false);
				if (jumlahUdahMasuk < perkuliahan.getKapasitasKelas()
						&& !Common.checkJamBentrok(selectedPerkuliahans, perkuliahan)) {
					selectedPerkuliahans.add(perkuliahan);
					break;
				}
			}
			perkuliahans = null;
		}

		Set<Long> matakuliahs = new HashSet<Long>();
		String peringatanKapasitasRuangan = "";
		for (final Perkuliahan perkuliahan : selectedPerkuliahans) {
			if (matakuliahs.contains(perkuliahan.getMatakuliah().getId())) {
				continue;
			}
			matakuliahs.add(perkuliahan.getMatakuliah().getId());

			Long id;
			try {
				id = (Long) (session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.isNull("ikutiPerkuliahan")).setProjection(Projections.property("id"))
						.add(Restrictions.eq("perkuliahan", perkuliahan)).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("semester", semester.getValue()))

						.createCriteria("perkuliahan", Criteria.LEFT_JOIN)
						.add(Restrictions.isNull("statusSemesterPendek")).uniqueResult());
			} catch (Exception e) {
				continue;
			}

			if (!Common.checkMatakuliahPrasyarat(perkuliahan.getMatakuliah(), mahasiswa, semester.getValue())) {
				continue;
			}

			Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser,
					GenerateKRSPaketMahasiswaOtomatisWindow.class);
			if (id != null) {
				detailperkuliahan = (Detailperkuliahan) session.load(Detailperkuliahan.class, id);
			} else {

				Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, false);

				jumlahUdahMasuk++;
				if (jumlahUdahMasuk > (perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas()
						: perkuliahan.getKapasitasKelas())) {
					peringatanKapasitasRuangan += Common.pesan(
							"Mohon maaf, kapasitas kelas untuk perkuliahan ini telah penuh. Kapasitas maksimal kelas tersebut adalah {V1} peserta, sedangkan penambahan Anda akan menjadikan jumlah peserta menjadi {V2}. Langkah yang dapat dilakukan: (1) pilih jadwal perkuliahan atau kelas lain yang masih tersedia; (2) hubungi bagian akademik untuk penambahan kuota kelas bila diperlukan.\n",
							(perkuliahan.getKapasitasKelas() == null ? Ruang.getDefaultKapasitas() : perkuliahan.getKapasitasKelas()), jumlahUdahMasuk);
					continue;
				}
			}

			detailperkuliahan.setPaketPerkuliahan(paketPerkuliahan);
			detailperkuliahan.setNilaiHuruf("");
			detailperkuliahan.setTotalNilai(0.0);
			detailperkuliahan.setMahasiswa(mahasiswa);
			detailperkuliahan.setPerkuliahan(perkuliahan);
			detailperkuliahan.setSemester(semester.getValue());
			if (detailperkuliahan.getId() == null) {
				KrsUtilHelper.simpanKrsJikaBelumAda(session, detailperkuliahan);
			} else {
				session.update(detailperkuliahan);
			}

		}
		if (!peringatanKapasitasRuangan.trim().equals("")) {
			MyMessageboxConfig.show(peringatanKapasitasRuangan, "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
		}
		return true;
	}

}
