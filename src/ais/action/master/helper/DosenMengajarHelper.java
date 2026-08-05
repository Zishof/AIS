package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Vbox;

import ais.action.master.BiodataDosenAction;
import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyTabConfig;

public class DosenMengajarHelper implements DataLoader {

	private MyGrid grid;
	private Dosen dosen;
	private String tahunAjaran;
	private String jenisSemester;
	private String program;
	private Jurusan jurusan;
	private boolean ases;

	public DosenMengajarHelper() {

	}

	private static DosenMengajarDetailperkuliahanHelper detailperkuliahanHelper = new DosenMengajarDetailperkuliahanHelper(
			true);

	public static Tabbox createDetail(final Dosen dosen, final Matakuliah matakuliah, final Jenjang jenjang,
			final String tahunAkademik, final String semester, final EventListener keteranganEventListener)
			throws Exception {

		Tabbox tabbox = new Tabbox();
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
		tabSoal.setParent(tabs);

		MyTabConfig tabPengajaran = new MyTabConfig("Rincian Pengajaran");
		tabPengajaran.setParent(tabs);

		MyTabConfig tabELearning = new MyTabConfig("E-Learning Pengajaran");
		tabELearning.setParent(tabs);

		MyTabConfig tabSK = new MyTabConfig("SK Pengajaran");
		tabSK.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setStyle("min-height: 300px;");
		tabpanelUtama.setParent(tabpanels);

		PenilaianAsesorHelper.formNilai(new Pegawai(dosen), "matakuliah", matakuliah, jenjang, tahunAkademik, semester,
				"SK Mengajar", PenilaianAsesor.PENGAJARAN, keteranganEventListener).setParent(tabpanelUtama);

		final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
		jurusanTabpanel.setParent(tabpanels);
		jurusanTabpanel.setWidth("100%");

		tabPengajaran.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusanTabpanel.getChildren().isEmpty()) {

					Criterion criterion = Restrictions.eq("dosen1", dosen);
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

					List<Perkuliahan> perkuliahans = HibernateUtil.currentSession()
							.createCriteria(Detailperkuliahan.class)
							.setProjection(Projections.groupProperty("perkuliahan"))
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.createCriteria("perkuliahan").add(criterion).add(Restrictions.eq("matakuliah", matakuliah))
							.createAlias("jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
							.add(Restrictions.eq("tahunAjaran", tahunAkademik)).add(Restrictions.in("semester",
									semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
							.list();

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(jurusanTabpanel);
					borderlayout.setHeight("100%");
					borderlayout.setWidth("100%");

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
					// per kelas yang diajar dosen ini data-driven, sama seperti pola "Ke-1".."Ke-N"
					// di SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
					// Tab/Tabpanel bawaan ZK. Konten tetap eager seperti semula.
					ais.ui.util.MyButtonTabbox tabboxPengajaran = ais.ui.util.MyButtonTabbox.buat(center, "100%",
							null);
					int indexPengajaran = 1;
					for (Perkuliahan perkuliahan : perkuliahans) {
						org.zkoss.zul.Div panelPengajaran = tabboxPengajaran.tambahTab(indexPengajaran,
								perkuliahan.getHari() + " " + perkuliahan.getKelas());
						detailperkuliahanHelper.display(perkuliahan.getPerkuliahan_paralel() == null ? perkuliahan
								: perkuliahan.getPerkuliahan_paralel(), panelPengajaran);
						indexPengajaran++;
					}
					tabboxPengajaran.pilih(1);

				}
			}
		});

		final Tabpanel eLearningTabpanel = new ais.ui.util.MyTabpanel();
		eLearningTabpanel.setParent(tabpanels);
		eLearningTabpanel.setWidth("100%");

		tabELearning.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (eLearningTabpanel.getChildren().isEmpty()) {
					Tbmuser tbmuser = Common.getCurrentUser();
					AktifitasPerkuliahanHelper aktifitasPerkuliahanHelper = new AktifitasPerkuliahanHelper(
							tbmuser == null ? null : tbmuser.getMahasiswa(),
							tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(), true);

					Criterion criterion = Restrictions.eq("dosen1", dosen);
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
					criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

					List<Perkuliahan> perkuliahans = HibernateUtil.currentSession()
							.createCriteria(Detailperkuliahan.class)
							.setProjection(Projections.groupProperty("perkuliahan"))
							.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
							.createCriteria("perkuliahan").add(criterion).add(Restrictions.eq("matakuliah", matakuliah))
							.createAlias("jurusan", "jurusan").add(Restrictions.eq("jurusan.jenjang", jenjang))
							.add(Restrictions.eq("tahunAjaran", tahunAkademik)).add(Restrictions.in("semester",
									semester.equals(Perkuliahan.GANJIL) ? Common.ganjil : Common.genap))
							.list();

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(eLearningTabpanel);
					borderlayout.setHeight("100%");
					borderlayout.setWidth("100%");

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					// GANTI TAB -> BUTTON GROUP (kelas reusable ais.ui.util.MyButtonTabbox): tab
					// per kelas yang diajar dosen ini data-driven, sama seperti pola "Ke-1".."Ke-N"
					// di SetingBiayaAction yang sebelumnya bermasalah blank/scroll pakai
					// Tab/Tabpanel bawaan ZK. Konten tetap eager seperti semula.
					ais.ui.util.MyButtonTabbox tabboxELearning = ais.ui.util.MyButtonTabbox.buat(center, "100%",
							null);
					int indexELearning = 1;
					for (Perkuliahan perkuliahan : perkuliahans) {
						org.zkoss.zul.Div panelELearning = tabboxELearning.tambahTab(indexELearning,
								perkuliahan.getHari() + " " + perkuliahan.getKelas());
						int banyak = 1;
						try {
							banyak = Integer.parseInt(Common
									.getKonfigurasi("tampilan_jumlah_agenda_perkuliahan", banyak + "").getNilai());
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/DosenMengajarHelper.java:243");
						}
						aktifitasPerkuliahanHelper.initDetail(perkuliahan, panelELearning, 0, banyak);
						indexELearning++;
					}
					tabboxELearning.pilih(1);

				}
			}
		});

		final Tabpanel skTabpanel = new ais.ui.util.MyTabpanel();
		skTabpanel.setParent(tabpanels);
		skTabpanel.setWidth("100%");
		tabSK.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (skTabpanel.getChildren().isEmpty()) {
					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(skTabpanel);
					borderlayout.setStyle("border:0px;");
					borderlayout.setHeight("100%");
					borderlayout.setWidth("100%");

					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					BiodataDosenAction.reloadDosen(center, dosen, tahunAkademik, semester);
				}
			}
		});

		return tabbox;
	}

	public static void displayRow(Row row, final Perkuliahan perkuliahan, final Boolean ases, final Dosen dosen)
			throws Exception {
		int jumlahMhs = ((Number) HibernateUtil.currentSession().createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI)).uniqueResult()).intValue();

		Matakuliah matakuliah = perkuliahan.getMatakuliah();

		Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah, null, false);
		matakuliah = matakuliahs[0];
		Matakuliah matakuliahAsli = matakuliahs[1];
		if (matakuliah == null) {
			row.setVisible(false);
			return;
		}

		final Vbox vboxKeterangan = new Vbox();
		final EventListener keteranganEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vboxKeterangan);

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				List<PenilaianAsesor> asesorMemberikanPenilaians = session.createCriteria(PenilaianAsesor.class)
						.add(Restrictions.isNotNull("asesor")).createAlias("asesemenPenilaian", "asesemenPenilaian")
						.add(Restrictions.eq("asesemenPenilaian.matakuliah", perkuliahan.getMatakuliah())).list();
				for (PenilaianAsesor penilaianAsesor : asesorMemberikanPenilaians) {
					new Label(penilaianAsesor.getAsesor().getAsesorPenunjangKinerjaDosen().getNama() + " : "
							+ Common.numberFormat.get().format(penilaianAsesor.getSks()) + " sks, "
							+ (penilaianAsesor.getKeterangan())
							+ (penilaianAsesor.getAsesemenPenilaian().getPegawai() == null ? ""
									: " (" + penilaianAsesor.getAsesemenPenilaian().getPegawai().getNama() + ")"))
							.setParent(vboxKeterangan);
				}
			}
		};

		if (jumlahMhs == 0) {
			new Label().setParent(row);
		} else {
			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			final EventListener eventListener = new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						if (ases) {
							DosenMengajarHelper.createDetail(dosen, perkuliahan.getMatakuliah(),
									perkuliahan.getJurusan().getJenjang(), perkuliahan.getTahunAjaran(),
									perkuliahan.getGanjilGenap(),
									keteranganEventListener).setParent(detail);
						} else {
							detailperkuliahanHelper.display(perkuliahan.getPerkuliahan_paralel() == null ? perkuliahan
									: perkuliahan.getPerkuliahan_paralel(), detail);
						}

					}
				}
			};

			detail.addEventListener("onOpen", eventListener);

			if (ases) {
				detail.setOpen(true);
				eventListener.onEvent(null);
			}
		}

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		Vbox vbox = new Vbox();
		vbox.setParent(hbox);

		RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,
				(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode()
						: (matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")")))
				.setParent(vbox);

		new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama()
				: (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")")).setParent(vbox);

		new Label("Kelas/Smt/SKS : " + perkuliahan.getKelas() + "/"
				+ (perkuliahan.getSemester() == null ? "" : perkuliahan.getSemester().toString()) + "/"
				+ (matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "")
						: (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")))
				.setParent(vbox);
		new Label("Qty Mhs : " + Common.numberFormat.get().format(jumlahMhs)).setParent(vbox);

		vbox = new Vbox();
		vbox.setParent(hbox);

		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
		Hbox hboxLagi;
		if (Common.bolehKonfigurasi("tampilkan_rps")) {

			hboxLagi = new Hbox();
			hboxLagi.setParent(vbox);
			LampiranLain.createDownloadUploadFileLain(hboxLagi, perkuliahan.getId(), LampiranLain.SILABUS, "RPS", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false,
					mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null);
		}
		if (Common.bolehKonfigurasi("tampilkan_sap")) {

			hboxLagi = new Hbox();
			hboxLagi.setParent(vbox);
			LampiranLain.createDownloadUploadFileLain(hboxLagi, perkuliahan.getId(), LampiranLain.SAP, "SAP", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false,
					mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null);
		}
		if (Common.bolehKonfigurasi("tampilkan_absen_manual")) {
			hboxLagi = new Hbox();
			hboxLagi.setParent(vbox);
			LampiranLain.createDownloadUploadFileLain(hboxLagi, perkuliahan.getId(), "Absen Manual", "Absen Manual",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false,
					mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null);
		}

		if (Common.bolehKonfigurasi("tampilkan_soal_uts")) {
			hboxLagi = new Hbox();
			hboxLagi.setParent(vbox);
			LampiranLain.createDownloadUploadFileLain(hboxLagi, perkuliahan.getId(), "Soal UTS", "Soal UTS", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false,
					mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null);
		}

		if (Common.bolehKonfigurasi("tampilkan_soal_uas")) {
			hboxLagi = new Hbox();
			hboxLagi.setParent(vbox);
			LampiranLain.createDownloadUploadFileLain(hboxLagi, perkuliahan.getId(), "Soal UAS", "Soal UAS", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false,
					mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null);
		}

		for (String t : AktifitasPerkuliahanHelper.lampiranLain) {

			if (Common.bolehKonfigurasi("tampilkan_" + t, Konfigurasi.TIDAK_AKTIF)) {
				hboxLagi = new Hbox();
				hboxLagi.setParent(vbox);
				LampiranLain.createDownloadUploadFileLain(hboxLagi, perkuliahan.getId(), t, t, false,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false,
						mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null);
			}
		}

		String tampilkan_lampiran_lain_di_agenda = Common.getKonfigurasi("tampilkan_lampiran_lain_di_agenda", "")
				.getNilai();
		if (tampilkan_lampiran_lain_di_agenda != null && !tampilkan_lampiran_lain_di_agenda.trim().isEmpty()) {
			for (String s : tampilkan_lampiran_lain_di_agenda.split(",")) {
				hboxLagi = new Hbox();
				hboxLagi.setParent(vbox);
				LampiranLain.createDownloadUploadFileLain(hboxLagi, perkuliahan.getId(), s, s, false,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, null, false, false, false,
						mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null);
			}
		}

		ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, true);

		ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(row, perkuliahan);

		vboxKeterangan.setParent(row);
		if (jumlahMhs > 0) {
			keteranganEventListener.onEvent(null);
		}
	}

	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final Perkuliahan perkuliahan = (Perkuliahan) data;

			DosenMengajarHelper.displayRow(row, perkuliahan, ases, dosen);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Criterion criterion = Restrictions.eq("dosen1", dosen);
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

		List<Perkuliahan> perkuliahans = HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criterion)
				.add(Restrictions.eq("tahunAjaran", tahunAjaran)).add(Restrictions.eq("program", program))
				.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.sqlRestriction(
						jenisSemester.equals(Perkuliahan.GENAP) ? "this_.semester%2=0" : "this_.semester%2=1"))
				.list();

		DetailMahasiswaRenderer detailMahasiswaRenderer = new DetailMahasiswaRenderer();

		Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		Common.clear(rows);
		grid.appendChild(rows);

		int totalSks = 0;
		for (Perkuliahan perkuliahan : perkuliahans) {

			Matakuliah matakuliah = perkuliahan.getMatakuliah();

			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah, null, false);
			matakuliah = matakuliahs[0];
			if (matakuliah == null) {
				continue;
			}

			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			try {
				detailMahasiswaRenderer.render(row, perkuliahan);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

			totalSks += matakuliah.getSks();
		}

		Row row = new Row();row.setValign("top");
		row.setStyle("background-color: rgba(224, 224, 235,0.4);");
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(new MyLabelBold("Total SKS : " + Common.numberFormat.get().format(totalSks)));
		row.appendChild(new Label());
		row.appendChild(new Label());
		row.appendChild(new Label());

	}

	public void display(boolean ases, PenugasanDosenMengajar penugasanDosenMengajar, final Component component) {

		this.ases = ases;
		this.dosen = penugasanDosenMengajar.getDosen();
		this.tahunAjaran = penugasanDosenMengajar.getTahunAkademik();
		this.jenisSemester = penugasanDosenMengajar.getSemester();
		this.program = penugasanDosenMengajar.getProgram();
		this.jurusan = penugasanDosenMengajar.getJurusan();

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 1000px;");

		groupbox.setWidth("95%");
		groupbox.setParent(component);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("min-height: 1000px;");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("45px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Matakuliah");
		column.setWidth(ases ? "15%" : "65%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hari/Jam/Ruang");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Informasi");
		column.setWidth(ases ? "50%" : "0%");

		loadData(null);
	}

}
