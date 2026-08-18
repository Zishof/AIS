package ais.action.master.beasiswa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.RevisiHelper;
import ais.action.report.CommonReportHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendapatanOrangTua;
import ais.database.model.StatusMahasiswa;
import ais.database.model.beasiswa.BeasiswaPunyaPersyaratan;
import ais.database.model.beasiswa.MahasiswaBeasiswaPersyaratan;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;
import ais.database.model.beasiswa.PersyaratanBeasiswa;
import ais.database.model.file.LampiranBeasiswaMahasiswa;
import ais.database.model.pkl.PersyaratanPkl;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextboxAngka;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class BeasiswaUntukMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow daftar_beasiswa_window;
	private Paging paging;
	private MyGrid grid;

	// private MyToolbarbuttonConfig add;
	private Mahasiswa mahasiswa;

	private Toolbar toolbarData;

	// private Textbox no_SKTM;
	// private Textbox pejabat_penandatangan;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		// if (session.getAttribute("usersTemp") == null
		// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }
		//
		// add.setVisible(CommonPrivilages
		// .checkPrevilages(CommonPrivilages.CREATE));
		// add.setTooltiptext("Tambah");

		if (execution.getParameter("mahasiswa") != null) {
			mahasiswa = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa")))).uniqueResult();
		} else {
			mahasiswa = Common.getCurrentUser().getMahasiswa();
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (mahasiswa != null) {

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Beasiswa Mahasiswa", "/img/print.png");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakBeasiswaMahasiswa(mahasiswa);
				}
			});
			cetak.setParent(toolbarData);
		}
	}

	class BeasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Beasiswa beasiswa = (Beasiswa) arg1;

			new Label(beasiswa.getTahun() == null ? "" : beasiswa.getTahun().toString()).setParent(arg0);
			RevisiHelper.createNewRevisi(Beasiswa.class, beasiswa, beasiswa.getNama()).setParent(arg0);
			new Label(beasiswa.getTanggalBuka() == null ? "" : Common.dateFormat2.get().format(beasiswa.getTanggalBuka()))
					.setParent(arg0);
			new Label(beasiswa.getTanggalTutup() == null ? "" : Common.dateFormat2.get().format(beasiswa.getTanggalTutup()))
					.setParent(arg0);
			new Label(beasiswa.getInstansi()).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			final MahasiswaDaftarBeasiswa mahasiswaDaftarBeasiswa = (MahasiswaDaftarBeasiswa) session
					.createCriteria(MahasiswaDaftarBeasiswa.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.addOrder(Order.desc("id")).setMaxResults(1).add(Restrictions.eq("beasiswa", beasiswa))
					.uniqueResult();

			new Label(mahasiswaDaftarBeasiswa == null ? "TIDAK TERDAFTAR"
					: mahasiswaDaftarBeasiswa.getTerima().equals(MahasiswaDaftarBeasiswa.BELUM_DIPROSES)
							? "Belum Diproses"
							: mahasiswaDaftarBeasiswa.getTerima().equals(MahasiswaDaftarBeasiswa.DITERIMA) ? "DITERIMA"
									: "DITOLAK")
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Daftar", "/img/svg/check2-circle.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Daftar");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(beasiswa, false);
				}

			});
			button.setParent(toolbar);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			Date sekarang = calendar.getTime();
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
			button.setDisabled(
					mahasiswaDaftarBeasiswa != null || statusMahasiswa.getId().equals(ConstantValues.LULUS.getId())
							|| sekarang.after(beasiswa.getTanggalTutup()));
			button.setVisible(!button.isDisabled());

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Bukti", "/img/print.png");
			cetak.setOrient("vertical");
			cetak.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("rawtypes")
				final Map parameters = ais.common.HashMapGenerator.getRand();

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

					parameters.put("id_mahasiswa", mahasiswa.getId());
					parameters.put("id_beasiswa", beasiswa.getId());
					mahasiswa.putPhoto(parameters); 
					Report.generatePDFReport(Report.PDF, parameters, "kartu_daftar_beasiswa",
							ais.ui.util.WaktuUtil.getDate());
				}
			});
			cetak.setParent(toolbar);
			cetak.setDisabled(mahasiswaDaftarBeasiswa == null);

			button = new MyToolbarbuttonConfig(mahasiswaDaftarBeasiswa != null
					&& mahasiswaDaftarBeasiswa.getTerima().equals(MahasiswaDaftarBeasiswa.BELUM_DIPROSES) ? "Ubah"
							: "Lihat",
					"/img/absensi_pmb.png");
			button.setTooltiptext("Lihat");
			button.setOrient("vertical");
			button.setVisible(mahasiswaDaftarBeasiswa != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					init(beasiswa, !mahasiswaDaftarBeasiswa.getTerima().equals(MahasiswaDaftarBeasiswa.BELUM_DIPROSES));
				}

			});
			button.setParent(toolbar);

			toolbar.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Beasiswa.class)

				.add(Restrictions.or(Restrictions.isNull("jenjang"),
						Restrictions.eq("jenjang", mahasiswa == null ? null : mahasiswa.getJurusan().getJenjang())))

				.add(Restrictions.or(Restrictions.isNull("jurusan"),
						Restrictions.eq("jurusan", mahasiswa == null ? null : mahasiswa.getJurusan())))
				.add(Restrictions.or(Restrictions.isNull("fakultas"),
						Restrictions.eq("fakultas", mahasiswa == null ? null : mahasiswa.getJurusan().getFakultas())));

		if (order)
			criteria.addOrder(Order.desc("tanggalBuka"));
		criteria.add(Restrictions.eq("dibukaUtkMahasiswa", 1));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (grid != null) {
			Common.initPaging(initCriteria(false), paging);

			List<Beasiswa> beasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			ListModel strset = new SimpleListModel(beasiswa);
			grid.setRowRenderer(new BeasiswaRenderer());
			grid.setModelCheckMobile(strset);
		}

	}

	@SuppressWarnings("unchecked")
	public boolean daftar(Beasiswa beasiswa) throws Exception {

		if (!memenuhiSyarat) {
			MyMessageboxConfig.show(
					"Mahasiswa dengan NIM " + mahasiswa.getNim()
							+ " tidak memenuhi syarat untuk dapat mendaftar beasiswa " + beasiswa.getNama(),
					"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		MahasiswaDaftarBeasiswa mahasiswaDaftarBeasiswa = (MahasiswaDaftarBeasiswa) session
				.createCriteria(MahasiswaDaftarBeasiswa.class).add(Restrictions.eq("beasiswa", beasiswa))
				.addOrder(Order.desc("id")).setMaxResults(1).add(Restrictions.eq("mahasiswa", mahasiswa))
				.uniqueResult();

		System.out.println("mahasiswaDaftarBeasiswa = " + mahasiswaDaftarBeasiswa);

		if (mahasiswaDaftarBeasiswa == null) {
			mahasiswaDaftarBeasiswa = new MahasiswaDaftarBeasiswa();
			mahasiswaDaftarBeasiswa.setTanggalDaftar(ais.ui.util.WaktuUtil.getDate());
			mahasiswaDaftarBeasiswa.setTerima(MahasiswaDaftarBeasiswa.BELUM_DIPROSES);
		}

		mahasiswaDaftarBeasiswa.setMemenuhiSyarat(memenuhiSyarat);
		mahasiswaDaftarBeasiswa.setNama(mahasiswa + "-->" + beasiswa.getNama());
		mahasiswaDaftarBeasiswa.setMahasiswa(mahasiswa);
		mahasiswaDaftarBeasiswa.setBeasiswa(beasiswa);

		List<MahasiswaBeasiswaPersyaratan> mahasiswaBeasiswaPersyaratans = session
				.createCriteria(MahasiswaBeasiswaPersyaratan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
				.add(Restrictions.eq("beasiswa", beasiswa)).createAlias("persyaratanBeasiswa", "persyaratanBeasiswa")
				.add(Restrictions.eq("persyaratanBeasiswa.tipeDataInputan", PersyaratanBeasiswa.PILIHAN_CUSTOM)).list();
		Integer totalSkor = 0;
		for (MahasiswaBeasiswaPersyaratan mahasiswaBeasiswaPersyaratan : mahasiswaBeasiswaPersyaratans) {
			String val = mahasiswaBeasiswaPersyaratan.getNilaiString() == null ? ""
					: mahasiswaBeasiswaPersyaratan.getNilaiString().trim();
			String[] kol = StringUtils.split(val, ":");
			Integer skor = 0;
			try {
				// Guard: nilai tanpa pemisah ":" hanya menghasilkan 1 elemen → kol[1] melempar
				// ArrayIndexOutOfBoundsException. Bila format tidak sesuai, skor dianggap 0.
				if (kol != null && kol.length > 1 && kol[1] != null && ais.common.Common.isNumber(kol[1].trim())) {
					skor = Integer.parseInt(kol[1].trim());
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			totalSkor += skor;
		}
		mahasiswaDaftarBeasiswa.setTotalSkor(totalSkor);

		Common.refreshSaveOrUpdate(session, mahasiswaDaftarBeasiswa);

		MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim() + " berhasil terdaftar di beasiswa ini",
				"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		return true;

	}

	private Boolean memenuhiSyarat = true;
	private EventListener eventListener;

	public static void onAddExternal(Event event, EventListener eventListener, Beasiswa beasiswa, Mahasiswa mahasiswa)
			throws Exception {
		BeasiswaUntukMahasiswaAction beasiswaUntukMahasiswaAction = new BeasiswaUntukMahasiswaAction();
		beasiswaUntukMahasiswaAction.mahasiswa = mahasiswa;
		beasiswaUntukMahasiswaAction.eventListener = eventListener;
		beasiswaUntukMahasiswaAction.daftar_beasiswa_window = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(beasiswaUntukMahasiswaAction.daftar_beasiswa_window);
		beasiswaUntukMahasiswaAction.daftar_beasiswa_window.setHeight("95%");
		beasiswaUntukMahasiswaAction.daftar_beasiswa_window.setWidth("90%");

		beasiswaUntukMahasiswaAction.daftar_beasiswa_window.setVisible(true);
		beasiswaUntukMahasiswaAction.daftar_beasiswa_window.setClosable(true);
		beasiswaUntukMahasiswaAction.daftar_beasiswa_window.onModal();

		beasiswaUntukMahasiswaAction.init(beasiswa, false);

	}

	public static void tampilkanPersyaratan() {

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(final Beasiswa beasiswa, Boolean hanyaLihat) throws Exception {

		daftar_beasiswa_window.setTitle("Pendataan Persyaratan Beasiswa");
		memenuhiSyarat = true;

		Common.clear(daftar_beasiswa_window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setParent(borderlayout);
		west.setWidth("30%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Beasiswa"));
		row.appendChild(new ais.ui.util.MyLabelConfig(beasiswa.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Instansi"));
		row.appendChild(new ais.ui.util.MyLabelConfig(beasiswa.getInstansi()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Mahasiswa"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNim()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(
				new ais.ui.util.MyLabelConfig(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getNama()));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(center);

		North north = new North();
		north.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("320px");
		north.setAutoscroll(true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		rows = new Rows();
		rows.setParent(grid);

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Syarat Beasiswa");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Data anda");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Memenuhi syarat");
		column.setWidth("15%");

		String ta = beasiswa.getTahunAkademik();
		Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
		Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(), beasiswa.getSemester(),
				mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, currentSemester, null, null);

		if (beasiswa.getBatasanIP() > 0.01) {
			row = new MyFormRow();
			row.setStyle("border:1px;solid;background: transparent;");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Batas minimal IPK"));
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(beasiswa.getBatasanIP())));
			Double IPK = krsMahasiswa.getIpk();
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(IPK)));
			row.appendChild(new ais.ui.util.MyLabelConfig((IPK >= beasiswa.getBatasanIP()) ? "Ya" : "Tidak"));

			memenuhiSyarat = (IPK >= beasiswa.getBatasanIP()) && memenuhiSyarat;
		}

		if (beasiswa.getBatasanSks() > 0.1) {

			row = new MyFormRow();
			row.setStyle("border:1px;solid;background: transparent;");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Batas minimal SKS"));
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(beasiswa.getBatasanSks())));
			Integer sks = krsMahasiswa.getSksk();
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(sks)));
			row.appendChild(
					new ais.ui.util.MyLabelConfig((sks >= beasiswa.getBatasanSks().intValue()) ? "Ya" : "Tidak"));

			memenuhiSyarat = (sks >= beasiswa.getBatasanSks().intValue()) && memenuhiSyarat;

		}

		if (beasiswa.getBatasanSkkp() > 0.01) {
			row = new MyFormRow();
			row.setStyle("border:1px;solid;background: transparent;");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Batas minimal Angka Kredit"));
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(beasiswa.getBatasanSkkp())));
			Double angkaKredit = Common.hitungAngkaKredit(mahasiswa);
			row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(angkaKredit)));
			row.appendChild(new ais.ui.util.MyLabelConfig((angkaKredit >= beasiswa.getBatasanSkkp()) ? "Ya" : "Tidak"));

			memenuhiSyarat = (angkaKredit >= beasiswa.getBatasanSkkp()) && memenuhiSyarat;
		}

		if (beasiswa.getPenghasilanOrangTua() > 0L) {

			BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
			PendapatanOrangTua pendapatanOrtu = biodataMahasiswa == null ? null : biodataMahasiswa.getPendapatanOrtu();

			Long mulai = pendapatanOrtu == null ? 0L : pendapatanOrtu.getMulaiDari().longValue();
			Long sampai = pendapatanOrtu == null ? 0L : pendapatanOrtu.getSampai().longValue();

			row = new MyFormRow();
			row.setStyle("border:1px;solid;background: transparent;");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Penghasilan orang tua"));
			row.appendChild(
					new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(beasiswa.getPenghasilanOrangTua())));
			row.appendChild(
					new Label(Common.numberFormat.get().format(mulai) + " s.d " + Common.numberFormat.get().format(sampai)));

			row.appendChild(
					new ais.ui.util.MyLabelConfig((sampai >= beasiswa.getPenghasilanOrangTua()) ? "Ya" : "Tidak"));

			memenuhiSyarat = (sampai >= beasiswa.getPenghasilanOrangTua()) && memenuhiSyarat;
		}

		center = new Center();
		center.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setStyle("border:0px");
		grid.setWidth("100%");
		grid.setHeight("110%");

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setStyle("border:0px;background: #8dcff4;");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formulir dan Persyaratan beasiswa"));
		ais.ui.util.ZkCompat.setSpans(row, "4");

		row = new MyFormRow();
		row.setStyle("border:0px;background: #8dcff4;");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));
		ais.ui.util.ZkCompat.setSpans(row, "4");

		Session session = HibernateUtil.currentSession();
		final List<PersyaratanBeasiswa> persyaratanBeasiswas = session.createCriteria(BeasiswaPunyaPersyaratan.class)
				.createAlias("persyaratanBeasiswa", "persyaratanBeasiswa").add(Restrictions.eq("beasiswa", beasiswa))
				.setProjection(Projections.property("persyaratanBeasiswa"))
				.addOrder(Order.asc("persyaratanBeasiswa.nama")).addOrder(Order.asc("persyaratanBeasiswa.labelInputan"))
				.list();
		final List<Component> components = new ArrayList<Component>();
		Label labelLama = new Label("");
		for (final PersyaratanBeasiswa persyaratan : persyaratanBeasiswas) {
			rows.appendChild(BeasiswaUntukMahasiswaAction.tampilkanPersyaratan(persyaratan, beasiswa, mahasiswa,
					labelLama, components, true));
		}

		// vbox.appendChild(new Label(
		// "* Pastikan data anda sudah benar karena anda tidak dapat mengubah
		// data ini setelah mendaftar beasiswa! (kecuali dengan persetujuan
		// Admin)"));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Daftar dan Simpan", "/img/svg/check2-circle.svg");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (beasiswa.getHarusBayar()) {

					Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), beasiswa.getTahunAkademik(),
							beasiswa.getSemester(), mahasiswa.getPindahKeKampusIniMasukSemester(),
							mahasiswa.getSemesterMulai());

					if (!Common.checkStatusPembayaranMahasiswa(smt, mahasiswa.currentTahapan(), mahasiswa, false,
							false)) {
						MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim()
								+ " tidak diperkenankan mendaftar, karena belum membayar biaya perkuliahan semester "
								+ smt, "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						daftar_beasiswa_window.setVisible(false);
						return;
					}
				}

				if (Common.bolehKonfigurasi("jika_sudah_dapat_beasiswa_mahasiswa_tidak_boleh_mengajukan_beasiswa")) {
					int jumlah = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDaftarBeasiswa.class)
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(beasiswa.getId() != null ? Restrictions.ne("beasiswa", beasiswa)
									: Restrictions.sqlRestriction("true"))
							.setProjection(Projections.rowCount()).add(Restrictions.eq("terima", 1)).uniqueResult())
							.intValue();
					if (beasiswa.getBolehGanda() == true && jumlah > 0) {
						MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim()
								+ " tidak diperkenankan mendaftar beasiswa ini, karena beasiswa ini tidak diperbolehkan menerima lebih dari satu beasiswa, dan mahasiswa dengan NIM "
								+ mahasiswa.getNim() + " telah menerima beasiswa sebelumnya", "INFORMATION",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}

				if (Common.bolehKonfigurasi("jika_sudah_dapat_beasiswa_dalam_satu_tahun_mahasiswa_tidak_boleh_mengajukan_beasiswa")) {
					int jumlah = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDaftarBeasiswa.class)
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(beasiswa.getId() != null ? Restrictions.ne("beasiswa", beasiswa)
									: Restrictions.sqlRestriction("true"))
							.setProjection(Projections.rowCount()).add(Restrictions.eq("terima", 1)).uniqueResult())
							.intValue();
					if (beasiswa.getBolehGanda() == true && jumlah > 0) {
						MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim()
								+ " tidak diperkenankan mendaftar beasiswa ini, karena beasiswa ini tidak diperbolehkan menerima lebih dari satu beasiswa, dan mahasiswa dengan NIM "
								+ mahasiswa.getNim() + " telah menerima beasiswa sebelumnya", "INFORMATION",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}

				if (!memenuhiSyarat) {
					MyMessageboxConfig.show("Mohon maaf, data mahasiswa belum memenuhi syarat untuk mendaftar beasiswa ini. Langkah yang dapat dilakukan: (1) Periksa kembali persyaratan nilai, IPK, atau kondisi akademik yang ditentukan oleh beasiswa ini; (2) Pastikan data mahasiswa sudah lengkap dan sesuai dengan ketentuan yang berlaku; (3) Hubungi Bagian Kemahasiswaan jika merasa data sudah benar. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				for (final Component component : components) {

					if (component.getAttribute("persyaratan") != null) {
						PersyaratanBeasiswa persyaratan = (PersyaratanBeasiswa) component.getAttribute("persyaratan");
						if (!persyaratan.getHarusDiisi()) {
							continue;
						}
					}

					if (component instanceof Textbox && ((Textbox) component).getValue().trim().isEmpty()) {
						MyMessageboxConfig.show("Mohon maaf, persyaratan belum lengkap diisi. Langkah yang dapat dilakukan: (1) Isi kolom isian teks yang masih kosong dan pastikan tidak hanya berisi spasi; (2) Perhatikan semua kolom bertanda (*) yang berarti wajib diisi; (3) Klik tombol Daftar dan Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((Textbox) component).focus();
									}
								});
						return;
					} else if (component instanceof MyDatebox && ((MyDatebox) component).getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, persyaratan belum lengkap diisi. Langkah yang dapat dilakukan: (1) Isi kolom tanggal yang masih kosong dengan memilih tanggal yang sesuai; (2) Perhatikan semua kolom bertanda (*) yang berarti wajib diisi; (3) Klik tombol Daftar dan Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((MyDatebox) component).focus();
									}
								});
						return;
					} else if (component instanceof MyDoublebox && ((MyDoublebox) component).getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, persyaratan belum lengkap diisi. Langkah yang dapat dilakukan: (1) Masukkan nilai angka pada kolom angka yang masih kosong; (2) Perhatikan semua kolom bertanda (*) yang berarti wajib diisi; (3) Klik tombol Daftar dan Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((MyDoublebox) component).focus();
									}
								});
						return;
					} else if (component instanceof Combobox && ((Combobox) component).getSelectedItem() == null) {
						MyMessageboxConfig.show("Mohon maaf, persyaratan belum lengkap diisi. Langkah yang dapat dilakukan: (1) Pilih salah satu pilihan pada kolom dropdown yang belum dipilih; (2) Perhatikan semua kolom bertanda (*) yang berarti wajib diisi; (3) Klik tombol Daftar dan Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((Combobox) component).focus();
									}
								});
						return;
					}
				}

				Session session = HibernateUtil.currentSession();
				for (final PersyaratanBeasiswa persyaratan : persyaratanBeasiswas) {
					if (persyaratan.getHarusMenyertakanLampiran()) {
						MahasiswaBeasiswaPersyaratan mahasiswaBeasiswaPersyaratan = (MahasiswaBeasiswaPersyaratan) session
								.createCriteria(MahasiswaBeasiswaPersyaratan.class).addOrder(Order.desc("id"))
								.setMaxResults(1).add(Restrictions.eq("mahasiswa", mahasiswa))
								.add(Restrictions.eq("beasiswa", beasiswa))
								.add(Restrictions.eq("persyaratanBeasiswa", persyaratan)).uniqueResult();
						if (mahasiswaBeasiswaPersyaratan == null) {
							mahasiswaBeasiswaPersyaratan = new MahasiswaBeasiswaPersyaratan();
							mahasiswaBeasiswaPersyaratan.setMahasiswa(mahasiswa);
							mahasiswaBeasiswaPersyaratan.setBeasiswa(beasiswa);
							mahasiswaBeasiswaPersyaratan.setPersyaratanBeasiswa(persyaratan);
							session.save(mahasiswaBeasiswaPersyaratan);
						}

						final MahasiswaBeasiswaPersyaratan temPersyaratan = mahasiswaBeasiswaPersyaratan;

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

						int foto = ((Number) streamingSession.createCriteria(LampiranBeasiswaMahasiswa.class)
								.setProjection(Projections.rowCount())
								.add(Restrictions.eq("persyaratanBeasiswa", temPersyaratan.getId())).setMaxResults(1)
								.uniqueResult()).intValue();

						StreamingHibernateUtil.getInstance().closeSession();

						if (foto == 0) {
							MyMessageboxConfig.show("Mohon maaf, berkas \"" + persyaratan.getNama() + "\" belum diunggah. Langkah yang dapat dilakukan: (1) Cari tombol unggah (upload) di samping persyaratan \"" + persyaratan.getNama() + "\", lalu klik tombol tersebut; (2) Pilih file yang sesuai dari komputer dan tunggu hingga proses unggah selesai; (3) Klik tombol Daftar dan Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

					}
				}

				if (daftar(beasiswa)) {
					onSearchDefault(null);
					daftar_beasiswa_window.setVisible(false);

					if (eventListener != null) {
						eventListener.onEvent(null);
					}
				}
			}
		});
		save.setParent(toolbar);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal dan Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				daftar_beasiswa_window.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		borderlayout.setParent(daftar_beasiswa_window);
		daftar_beasiswa_window.setVisible(true);
		daftar_beasiswa_window.onModal();

		if (hanyaLihat) {
			Common.freeze(daftar_beasiswa_window, true);
			save.setVisible(false);
			cancel.setDisabled(false);
		}

	}

	@SuppressWarnings("deprecation")
	public static Row tampilkanPersyaratan(final PersyaratanBeasiswa persyaratan, Beasiswa beasiswa,
			Mahasiswa mahasiswa, Label labelLama, List<Component> components, boolean tampil) {
		Session session = HibernateUtil.currentSession();
		MahasiswaBeasiswaPersyaratan mahasiswaBeasiswaPersyaratan = new MahasiswaBeasiswaPersyaratan();

		if (beasiswa != null && mahasiswa != null) {
			mahasiswaBeasiswaPersyaratan = (MahasiswaBeasiswaPersyaratan) session
					.createCriteria(MahasiswaBeasiswaPersyaratan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.addOrder(Order.desc("id")).setMaxResults(1).add(Restrictions.eq("beasiswa", beasiswa))
					.add(Restrictions.eq("persyaratanBeasiswa", persyaratan)).uniqueResult();
			if (mahasiswaBeasiswaPersyaratan == null) {
				mahasiswaBeasiswaPersyaratan = new MahasiswaBeasiswaPersyaratan();
				mahasiswaBeasiswaPersyaratan.setMahasiswa(mahasiswa);
				mahasiswaBeasiswaPersyaratan.setBeasiswa(beasiswa);
				mahasiswaBeasiswaPersyaratan.setPersyaratanBeasiswa(persyaratan);
				session.save(mahasiswaBeasiswaPersyaratan);
			}
		}

		final MahasiswaBeasiswaPersyaratan temPersyaratan = mahasiswaBeasiswaPersyaratan;

		MyFormRow row = new MyFormRow();row.setValign("top");

		final Label check = new Label();
		if (labelLama == null || !labelLama.getValue().equalsIgnoreCase(persyaratan.getNama())) {
			check.setValue(persyaratan.getNama());
		}
		row.appendChild(check);

		final Component component;
		if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.TEXT)) {
			component = new Textbox(temPersyaratan == null ? null : temPersyaratan.getNilaiString());
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.TANGGAL)) {
			component = new MyDatebox(temPersyaratan == null ? null : temPersyaratan.getNilaiTanggal());

			((MyDatebox) component).focus();
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.ANGKA)) {
			component = new MyDoublebox(temPersyaratan == null ? null : temPersyaratan.getNilaiNumber());

		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.TEXT_ANGKA)) {
			component = new MyTextboxAngka(temPersyaratan == null ? null : temPersyaratan.getNilaiString());
			((MyTextboxAngka) component).setWidth("90%");
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.PILIHAN_YA_TIDAK)) {
			component = new Combobox();
			MyComboitemConfig comboitem = new MyComboitemConfig("Ya");
			comboitem.setValue(true);
			component.appendChild(comboitem);
			comboitem = new MyComboitemConfig("Tidak");
			comboitem.setValue(false);
			component.appendChild(comboitem);
			((Combobox) component).setReadonly(true);

			Common.selectComboItem(((Combobox) component),
					temPersyaratan == null ? null : temPersyaratan.getNilaiBoolean());
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.PILIHAN_CUSTOM)) {
			component = new Combobox();
			String[] ss = StringUtils.split(persyaratan.getNilaiDataInputan(), ";");
			if (ss == null) {
				ss = new String[0];
			}
			Arrays.sort(ss);
			for (String s : ss) {
				if (s == null || s.trim().length() == 0) {
					continue;
				}
				String[] kol = StringUtils.split(s, ":");
				String a = kol != null && kol.length > 0 && kol[0] != null ? kol[0].trim() : s.trim();
				Integer skor = Integer.valueOf(0);
				if (kol != null && kol.length > 1 && kol[1] != null && kol[1].trim().length() > 0) {
					try {
						skor = Integer.valueOf(Integer.parseInt(kol[1].trim()));
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
				MyComboitemConfig comboitem = new MyComboitemConfig(a);
				comboitem.setAttribute("skor", skor);
				comboitem.setValue(s);
				component.appendChild(comboitem);
			}
			((Combobox) component).setReadonly(true);

			Common.selectComboItem(((Combobox) component),
					temPersyaratan == null ? null : temPersyaratan.getNilaiString());
		} else {
			component = null;
		}

		final Label label = new Label(persyaratan.getLabelInputan() + (persyaratan.getHarusDiisi() ? " (*)" : ""));
		final Hbox hbox = new Hbox();
		hbox.setStyle("border:0px;background: transparent;");
		temPersyaratan.setStatus(true);
		hbox.setVisible(temPersyaratan.getStatus());

		if (component != null) {

			component.setAttribute("persyaratan", persyaratan);
			components.add(component);

			row.appendChild(label);
			label.setWidth("97%");

			if (persyaratan.getHarusDiisi()) {
				if (component instanceof Textbox) {
					// ((Textbox) component).setConstraint("no empty");
				} else if (component instanceof MyDatebox) {
					// ((MyDatebox) component).setConstraint("no empty");
				} else if (component instanceof MyDoublebox) {
					// ((MyDoublebox) component).setConstraint("no empty");
				} else if (component instanceof Combobox) {
					// ((Combobox) component).setConstraint("no empty");
				}
			}

			component.setVisible(tampil && (temPersyaratan == null ? true : temPersyaratan.getStatus()));
			label.setVisible(temPersyaratan.getStatus());

			row.appendChild(component);
			row.setValign("top");row.setAttribute("component", component);
			component.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					try {
						if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.TEXT)) {
							temPersyaratan.setNilaiString(((Textbox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.PILIHAN_CUSTOM)) {
							temPersyaratan
									.setNilaiString((String) (((Combobox) component).getSelectedItem() == null ? ""
											: (((Combobox) component).getSelectedItem().getValue())));
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.TANGGAL)) {
							temPersyaratan.setNilaiTanggal(((MyDatebox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.ANGKA)) {
							temPersyaratan.setNilaiNumber(((MyDoublebox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.TEXT_ANGKA)) {
							temPersyaratan.setNilaiString(((MyTextboxAngka) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanBeasiswa.PILIHAN_YA_TIDAK)) {
							Combobox cb = (Combobox) component;
							if (cb.getSelectedItem() != null) {
								temPersyaratan.setNilaiBoolean((Boolean) cb.getSelectedItem().getValue());
							}
						}
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

					Common.refreshUpdate(temPersyaratan);
				}
			});

		}

		if (persyaratan.getHarusMenyertakanLampiran()) {
			Common.createDownloadUploadFileLampiranBeasiswa(hbox, mahasiswaBeasiswaPersyaratan,
					persyaratan.getLabelInputan());
			hbox.setParent(row);
		}

		if (component == null && persyaratan.getHarusMenyertakanLampiran()) {
			ais.ui.util.ZkCompat.setSpans(row, "1,3");
		} else if (component != null && !persyaratan.getHarusMenyertakanLampiran()) {
			ais.ui.util.ZkCompat.setSpans(row, "1,2,1");
		} else if (component == null && !persyaratan.getHarusMenyertakanLampiran()) {
			/*
			 * KOLOM ISIAN KOSONG. component == null (tipeDataInputan tak dikenali / belum diatur admin)
			 * DAN tidak wajib lampiran -> sebelumnya baris hanya berisi nama persyaratan, kolom isian
			 * kosong tanpa penjelasan. Kini ditampilkan pesan jelas bahwa pengaturan tipe isian belum
			 * lengkap dan perlu dilengkapi admin.
			 */
			final Label labelInfo = new Label(
					persyaratan.getLabelInputan() + (persyaratan.getHarusDiisi() ? " (*)" : ""));
			labelInfo.setWidth("97%");
			row.appendChild(labelInfo);
			String namaAman = persyaratan.getNama() == null ? "" : persyaratan.getNama()
					.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
			row.appendChild(new ais.ui.util.MyHtml(
					"<div style='color:#b91c1c;font-size:11px;line-height:1.4;padding:5px 8px;"
							+ "border:1px solid #fecaca;border-radius:6px;background:#fef2f2;'>"
							+ "<b>Kolom isian belum dapat ditampilkan.</b><br/>"
							+ "Jenis isian untuk persyaratan \"" + namaAman + "\" belum diatur oleh admin "
							+ "(mis. berupa teks, angka, tanggal, pilihan, atau wajib melampirkan berkas). "
							+ "Mohon menghubungi admin/pengelola beasiswa agar melengkapi pengaturan "
							+ "persyaratan ini terlebih dahulu.</div>"));
			ais.ui.util.ZkCompat.setSpans(row, "1,2,1");
		}

		if (labelLama == null || !labelLama.getValue().equalsIgnoreCase(persyaratan.getNama())) {
			labelLama.setValue(persyaratan.getNama());
		}

		return row;

	}
}
