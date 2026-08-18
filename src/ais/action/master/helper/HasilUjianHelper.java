package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.dashboard.admin.RekapHasilTugasMahasiswa;
import ais.action.master.dashboard.admin.RekapHasilUjianMahasiswa;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Ujian;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class HasilUjianHelper implements DataLoader, DataCriteria {

	private MyGrid grid;

	private Mahasiswa mahasiswa = null;

	private BiodataCalonMahasiswa biodataCalonMahasiswa = null;

	private Pertemuan pertemuan;

	public HasilUjianHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa, Pertemuan pertemuan) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		this.pertemuan = pertemuan;
	}

	public static void reinitUjian(Ujian ujian, Pertemuan pertemuan) {
		if (ujian != null && ujian.getDiperuntukkan() == null && pertemuan != null) {
			ujian.setDiperuntukkan(pertemuan.untuk());

			Session session = HibernateUtil.currentSession();
			Ujian u = (Ujian) session.createCriteria(Ujian.class).add(Restrictions.idEq(ujian.getId())).uniqueResult();
			u.setDiperuntukkan(pertemuan.untuk());
			Common.refreshUpdate(session, u);
			session.flush();

		}

		if (ujian != null && (ujian.getJurusan() == null || ujian.getMatakuliah() == null) && pertemuan != null
				&& pertemuan.getPerkuliahan() != null) {

			Session session = HibernateUtil.currentSession();

			Ujian u = (Ujian) session.createCriteria(Ujian.class).add(Restrictions.idEq(ujian.getId())).uniqueResult();
			u.setJurusan(pertemuan.getPerkuliahan().getJurusan());
			u.setFakultas(pertemuan.getPerkuliahan().getJurusan().getFakultas());
			u.setMatakuliah(pertemuan.getPerkuliahan().getMatakuliah());
			u.setDosen(pertemuan.getPerkuliahan().getDosen1());
			Common.refreshUpdate(session, u);
			session.flush();

			ujian.setJurusan(pertemuan.getPerkuliahan().getJurusan());
			ujian.setFakultas(pertemuan.getPerkuliahan().getJurusan().getFakultas());
			ujian.setMatakuliah(pertemuan.getPerkuliahan().getMatakuliah());
			ujian.setDosen(pertemuan.getPerkuliahan().getDosen1());

		} else

		if (ujian != null && (ujian.getSekolah() == null || ujian.getMatapelajaran() == null) && pertemuan != null
				&& pertemuan.getJadwalPelajaran() != null) {

			Session session = HibernateUtil.currentSession();

			Ujian u = (Ujian) session.createCriteria(Ujian.class).add(Restrictions.idEq(ujian.getId())).uniqueResult();
			u.setSekolah(pertemuan.getJadwalPelajaran().getSekolah());
			u.setYayasan(pertemuan.getJadwalPelajaran().getYayasan());
			u.setMatapelajaran(pertemuan.getJadwalPelajaran().getMatapelajaran());
			u.setGuru(pertemuan.getJadwalPelajaran().getGuru());
			Common.refreshUpdate(session, u);
			session.flush();

			ujian.setSekolah(pertemuan.getJadwalPelajaran().getSekolah());
			ujian.setYayasan(pertemuan.getJadwalPelajaran().getYayasan());
			ujian.setMatapelajaran(pertemuan.getJadwalPelajaran().getMatapelajaran());
			ujian.setGuru(pertemuan.getJadwalPelajaran().getGuru());

		} else if (ujian != null && ujian.getSekolah() == null && pertemuan != null
				&& pertemuan.getJadwalPertemuanPSB() != null) {
			Session session = HibernateUtil.currentSession();
			Ujian u = (Ujian) session.createCriteria(Ujian.class).add(Restrictions.idEq(ujian.getId())).uniqueResult();

			u.setSekolah(pertemuan.getJadwalPertemuanPSB().getGelombangPendaftaranPsb().getSekolah());
			u.setYayasan(pertemuan.getJadwalPertemuanPSB().getGelombangPendaftaranPsb().getYayasan());
			Common.refreshUpdate(session, u);
			session.flush();

			ujian.setSekolah(pertemuan.getJadwalPertemuanPSB().getGelombangPendaftaranPsb().getSekolah());
			ujian.setYayasan(pertemuan.getJadwalPertemuanPSB().getGelombangPendaftaranPsb().getYayasan());

		} else if (ujian != null && ujian.getSekolah() == null && pertemuan != null
				&& pertemuan.getJadwalUjianPSB() != null) {

			Session session = HibernateUtil.currentSession();
			Ujian u = (Ujian) session.createCriteria(Ujian.class).add(Restrictions.idEq(ujian.getId())).uniqueResult();

			u.setSekolah(pertemuan.getJadwalUjianPSB().getGelombangPendaftaranPsb().getSekolah());
			u.setYayasan(pertemuan.getJadwalUjianPSB().getGelombangPendaftaranPsb().getYayasan());
			Common.refreshUpdate(session, u);
			session.flush();

			ujian.setSekolah(pertemuan.getJadwalUjianPSB().getGelombangPendaftaranPsb().getSekolah());
			ujian.setYayasan(pertemuan.getJadwalUjianPSB().getGelombangPendaftaranPsb().getYayasan());

		}
	}

	class DetailPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		private DetailUjianHelper detailUjianHelper = new DetailUjianHelper();

		@SuppressWarnings("deprecation")
		@Override
		public void render(final Row arg0, final Object data) throws Exception {

			final HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) data;
			final PertemuanPunyaUjian pertemuanPunyaUjian = hasilUjianMahasiswa.getPertemuanPunyaUjian();
			final Ujian ujian = pertemuanPunyaUjian.getUjian();

			HasilUjianHelper.reinitUjian(ujian, pertemuan);

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			Tbmuser tbmuser = Common.getCurrentUser();

			if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null && tbmuser.getSiswa()==null
					|| tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null) {

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						Common.clear(detail);
						if (detail.isOpen()) {
							if (event != null && event.getData() != null
									&& event.getData() instanceof HasilUjianMahasiswa) {
								Common.clear(arg0);
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										render(arg0, data);
									}
								});
							} else {
								detailUjianHelper.display(ujian, detail, pertemuanPunyaUjian.getPertemuan(),
										pertemuanPunyaUjian, false, false);
							}
						}

					}
				};

				detail.setAttribute("eventListener", eventListener);
				detail.addEventListener("onOpen", eventListener);
			} else {

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						Common.clear(detail);
						if (detail.isOpen()) {

							if (event != null && event.getData() != null
									&& event.getData() instanceof HasilUjianMahasiswa) {
								Common.clear(arg0);
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										render(arg0, data);
									}
								});
							} else {
								HasilUjianMahasiswaHelper hasilUjianMahasiswaHelper = new HasilUjianMahasiswaHelper(
										mahasiswa, biodataCalonMahasiswa, pertemuan);
								hasilUjianMahasiswaHelper.tampilRow(detail, hasilUjianMahasiswa);
							}
						}

					}
				};
				detail.setAttribute("eventListener", eventListener);
				detail.addEventListener("onOpen", eventListener);
			}

			new Label(ujian.getNama()).setParent(arg0);
			new Label(Common.getBahasaConfig(ujian.getJenis()) + " / " + Common.getBahasaConfig(ujian.getLevel())
					+ " / " + Common.numberFormat.get().format(ujian.getNilaiLulus())).setParent(arg0);
			new Label(hasilUjianMahasiswa == null ? ""
					: Common.numberFormat.get().format(hasilUjianMahasiswa.getJawabanBenar())).setParent(arg0);

			new Label((hasilUjianMahasiswa == null || hasilUjianMahasiswa.getNilai() == null ? ""
					: Common.numberFormat.get().format(hasilUjianMahasiswa.getNilai()))
					+ ((ujian.getJenis().equalsIgnoreCase(BankSoal.ESAY)
							|| ujian.getJenis().equalsIgnoreCase(BankSoal.JAWABAN_SINGKAT))
									? ""
									: " / " + (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getNilai() == null
											? ""
											: (hasilUjianMahasiswa.getLulus() ? Common.getBahasaConfig("Lulus")
													: Common.getBahasaConfig("Tidak Lulus")))))
					.setParent(arg0);

			if (mahasiswa != null || biodataCalonMahasiswa != null
					|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
					|| tbmuser.getCalonSiswa() != null) {

				new Label((pertemuanPunyaUjian.getJmlDitampilkan() == null ? ""
						: Common.numberFormat.get().format(pertemuanPunyaUjian.getJmlDitampilkan()))
						+ (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getJumlahSoal() == null ? ""
								: " / " + Common.numberFormat.get().format(hasilUjianMahasiswa.getJumlahSoal())))
						.setParent(arg0);

				new Label(
						pertemuanPunyaUjian.getDibatasiWaktu() != null && pertemuanPunyaUjian.getDibatasiWaktu() ? "Ya"
								: "Tidak")
						.setParent(arg0);

				new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
						|| pertemuanPunyaUjian.getLama() == null ? ""
								: Common.timeFormat1.get().format(pertemuanPunyaUjian.getLama()))
						.setParent(arg0);

				new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
						|| pertemuanPunyaUjian.getMulaiUjian() == null
								? ""
								: Common.dateFormat3.get().format(pertemuanPunyaUjian.getMulaiUjian()).endsWith("00:00:00")
										? Common.dateFormat1.get().format(pertemuanPunyaUjian.getMulaiUjian())
										: Common.dateFormat3.get().format(pertemuanPunyaUjian.getMulaiUjian()))
						.setParent(arg0);

				new Label(pertemuanPunyaUjian.getDibatasiWaktu() == null || !pertemuanPunyaUjian.getDibatasiWaktu()
						|| pertemuanPunyaUjian.getSampaiUjian() == null
								? ""
								: Common.dateFormat3.get().format(pertemuanPunyaUjian.getSampaiUjian()).endsWith("00:00:00")
										? Common.dateFormat1.get().format(pertemuanPunyaUjian.getSampaiUjian())
										: Common.dateFormat3.get().format(pertemuanPunyaUjian.getSampaiUjian()))
						.setParent(arg0);

				new Label(pertemuanPunyaUjian.getFormatNilai() == null
						|| pertemuanPunyaUjian.getFormatNilai().getStatusPertemuan() == null ? ""
								: pertemuanPunyaUjian.getFormatNilai().getNama())
						.setParent(arg0);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				};

				PertemuanPunyaUjianHelper.tampilBolekIkutUjianAtauTidak(arg0, pertemuanPunyaUjian, mahasiswa,
						biodataCalonMahasiswa, hasilUjianMahasiswa, eventListener, null);

				if (mahasiswa != null || biodataCalonMahasiswa != null
						|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || tbmuser.getSiswa() != null
						|| tbmuser.getCalonSiswa() != null) {
					Long id = mahasiswa != null ? mahasiswa.getId()
							: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
									: tbmuser.getSiswa() != null ? tbmuser.getSiswa().getId()
											: tbmuser.getCalonSiswa() != null ? tbmuser.getCalonSiswa().getId() : null;
					if ((ujian != null && !ujian.getAktif())
							|| pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + id + ",")) {
						Common.freeze(arg0, true);

						Common.clear(arg0);
						ais.ui.util.ZkCompat.setSpans(arg0, "10");
						Label lbl = new Label("Anda tidak diizinkan ikut ujian \"" + ujian.getNama() + "\"");
						arg0.appendChild(lbl);
						lbl.setStyle("font-size:9px;color:red;");

					}
				}

			}
		}
	}

	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<HasilUjianMahasiswa> hasilUjianMahasiswas = mahasiswa != null
				? mahasiswa.ambilHasilUjianMahasiswa(session,
						value != null && value instanceof Boolean ? (Boolean) value : false)
				: biodataCalonMahasiswa != null
						? biodataCalonMahasiswa.ambilHasilUjianMahasiswa(session,
								value != null && value instanceof Boolean ? (Boolean) value : false)
						: new ArrayList<HasilUjianMahasiswa>();

		ListModel strset = new SimpleListModel(hasilUjianMahasiswas);
		grid.setRowRenderer(new DetailPertemuanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display(final Component component) {
		Common.clear(component);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(Common.tampilanScroll(component));
		borderlayout.setHeight("1700px");

		North north = new North();
		north.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(north);

		if (biodataCalonMahasiswa != null) {
			String[] cols = { "pertemuanPunyaUjian", "biodataCalonMahasiswa", "lamaPengerjaan", "sisaWaktuPengerjaan",
					"jumlahSoal", "jawabanBenar", "totalNilai", "nilai", "lulus" };
			MyToolbarbuttonConfig button = Common.cetakData(this, cols);
			toolbar.appendChild(button);
		} else if (mahasiswa != null) {
			String[] cols = { "pertemuanPunyaUjian", "mahasiswa", "lamaPengerjaan", "sisaWaktuPengerjaan", "jumlahSoal",
					"jawabanBenar", "totalNilai", "nilai", "lulus" };
			MyToolbarbuttonConfig button = Common.cetakData(this, cols);
			toolbar.appendChild(button);
		}

		MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Ujian",
				"/img/svg/edit-box-line.svg");
		buttonFormatNilai.setParent(toolbar);
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				RekapHasilUjianMahasiswa addWindow = new RekapHasilUjianMahasiswa(false, mahasiswa,
						biodataCalonMahasiswa, pertemuan == null ? null : pertemuan.ambilVOPembelajaran());
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Hasil Ujian");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}

		});

		buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Tugas", "/img/svg/edit-box-line.svg");
		buttonFormatNilai.setParent(toolbar);
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				RekapHasilTugasMahasiswa addWindow = new RekapHasilTugasMahasiswa(true, mahasiswa,
						biodataCalonMahasiswa, null);
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Hasil Tugas");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}

		});

		buttonFormatNilai = new MyToolbarbuttonConfig("Refresh", "/img/svg/refresh-cw.svg");
		buttonFormatNilai.setParent(toolbar);
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				loadData(true);
			}

		});
		Tbmuser tbmuser = Common.getCurrentUser();

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setSclass("fgrid");
		// Tampilkan SEMUA peserta dalam satu halaman (page size besar).
		grid.setPageSize(1000);
		grid.setParent(center);
		// Paging tampil di ATAS dan BAWAH tabel (konsisten dgn Hasil Ujian lain).
		grid.setPagingPosition("both");
		try {
			org.zkoss.zul.Paging pg = grid.getPagingChild();
			if (pg != null) {
				pg.setMold("os");
				pg.setDetailed(true);
			}
		} catch (Exception ignorePg) { ais.common.ErrorAuditUtil.record(ignorePg, "auto-audit(empty-catch) src/ais/action/master/helper/HasilUjianHelper.java:424");
		}

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ujian");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skor");
		column.setWidth(mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null ? "5%"
				: "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth(mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null ? "5%"
				: "0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null
				? "Jml Soal / Maks. Skor"
				: "Jml Soal");
		column.setWidth(mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null ? "10%"
				: "7%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dibatasi Wkt");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Lama");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dimulai");
		column.setWidth("14%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("14%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai masuk ke");
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("8%");

		loadData(null);

	}

	@Override
	public Criteria initCriteria(boolean order) {
		// TODO Auto-generated method stub
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
				.createAlias("pertemuanPunyaUjian", "pertemuanPunyaUjian")
				.add(Restrictions.or(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa),
						Restrictions.eq("mahasiswa", mahasiswa)))
				.addOrder(Order.asc("pertemuanPunyaUjian.mulaiUjian"));
	}

}
