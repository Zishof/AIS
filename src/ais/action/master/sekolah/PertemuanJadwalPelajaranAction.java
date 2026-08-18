package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.dashboard.sekolah.DashboardRekapPertemuanJadwalPelajaran;
import ais.action.master.sekolah.helper.AktifitasPembelajaranHelper;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.DataPunyaArtikel;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.library.Item;
import ais.database.model.penelitiandanpengabdian.Artikel;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPelajaranPunyaItem;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.MatapelajaranPunyaBukuBahanAjar;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class PertemuanJadwalPelajaranAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	protected Combobox searchhari;
	protected Textbox searchnamaruang;
	protected Textbox searchketerangan;
	protected Textbox searchsiswa;

	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchta;
	private Combobox searchsmt;

	private Combobox searchkelas;
	private AmbilDataGuruBanbox searchguru;

	private Tbmuser tbmuser;

	protected Tabpanel rekapitulasiPertemuanJadwalPelajaran;

	public void onRekapPertemuanJadwalPelajaran(Event event) {

		if (rekapitulasiPertemuanJadwalPelajaran.getChildren().size() == 0) {
			DashboardRekapPertemuanJadwalPelajaran laporan = new DashboardRekapPertemuanJadwalPelajaran();
			ais.ui.util.BaseDasbordPortal.mountWrapped(laporan, rekapitulasiPertemuanJadwalPelajaran,
				"Rekap Pertemuan", "Ringkasan pertemuan yang sudah terlaksana vs target per mata pelajaran.");
		}
	}

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
		Common.generateTahunAjaran(searchta);

		Comboitem comboitem = new Comboitem(JadwalPelajaran.GANJIL);
		if (comboitem != null) { comboitem.setValue(1); }
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(JadwalPelajaran.GENAP);
		if (comboitem != null) { comboitem.setValue(2); }
		searchsmt.appendChild(comboitem);
		if (searchsmt != null) { searchsmt.setCols(2); }

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		if (searchsmt != null) { searchsmt.setReadonly(true); }

		for (String h : Common.haris) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			searchhari.appendChild(comboitem);
		}
		comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchhari.appendChild(comboitem);
		if (searchhari != null) { searchhari.setReadonly(true); }
		if (searchhari != null) { searchhari.setSelectedItem(comboitem); }

		tbmuser = Common.getCurrentUser();
		aktifitasPembelajaranHelper = new AktifitasPembelajaranHelper(tbmuser.getSiswa(), tbmuser.getCalonSiswa());
		EventListener kelasEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (searchta == null || searchta.getSelectedItem() == null) {
					return;
				}
				String ta = (String) searchta.getSelectedItem().getValue();
				Sekolah s = tbmuser == null ? null : tbmuser.ambilSekolah();
				System.out.println("s => " + s);
				Common.insertComboDanSemua(searchkelas, new String[] { "nama", "tahunAjaran", "ruang" }, "keterangan",
						KelasSiswa.class,
						Restrictions.and(Restrictions.eq("tahunAjaran", ta), Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"),
										s == null ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.selectComboItem(searchkelas, null);
			}
		};

		kelasEvent.onEvent(null);
		searchta.addEventListener("onChange", kelasEvent);

		searchguru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	        FilterLanjutHelper.setup(comp);
}

	protected AktifitasPembelajaranHelper aktifitasPembelajaranHelper;

	class JadwalPelajaranRenderer extends ais.ui.util.MyRowRenderer {

		private boolean mobile = Common.isMobile();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detail.getChildren().size() == 0) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						int banyak = 1;
						try {
							banyak = Integer.parseInt(Common
									.getKonfigurasi("tampilan_jumlah_agenda_jadwal_pelajaran", banyak + "").getNilai());
						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
						aktifitasPembelajaranHelper.initDetail(jadwalPelajaran, groupbox, 0, banyak);
						detail.appendChild(groupbox);
					}
				}
			});

			Common.getDeskripsiJadwalPelajaranHbox(jadwalPelajaran, true, !mobile, arg0);
		}

	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalPelajaran.class)

				.createAlias("matapelajaran", "matapelajaran");

		if (!searchnamaruang.getValue().trim().isEmpty()) {
			criteria.createAlias("ruang", "ruang")
					.add(Restrictions.ilike("ruang.nama", searchnamaruang.getValue().trim(), MatchMode.ANYWHERE));
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {

			List<Long> kelas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id"))
					.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa())).list();

			if (!kelas.isEmpty()) {
				criteria.add(Restrictions.in("kelas.id", kelas));
			}

		}

		if (!searchsiswa.getValue().trim().isEmpty()) {
			List<Long> kelas = session.createCriteria(KelasSiswaPunyaSiswa.class)
					.setProjection(Projections.property("kelasSiswa.id")).createAlias("siswa", "siswa")
					.add(Restrictions.or(
							Restrictions.ilike("siswa.nama", searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("siswa.nomorInduk", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE))))
					.list();

			if (!kelas.isEmpty()) {
				criteria.add(Restrictions.in("kelas.id", kelas));
			}
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria

				.add(searchhari.getSelectedItem() == null || searchhari.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						:

						Restrictions.or(
								Restrictions
										.or(Restrictions.eq("hari", searchhari.getSelectedItem().getValue()),
												Restrictions
														.or(Restrictions.eq("hari5",
																searchhari.getSelectedItem().getValue()),
																Restrictions.or(
																		Restrictions.eq("hari4",
																				searchhari.getSelectedItem()
																						.getValue()),
																		Restrictions.or(
																				Restrictions.eq("hari3",
																						searchhari.getSelectedItem()
																								.getValue()),
																				Restrictions.eq("hari2",
																						searchhari.getSelectedItem()
																								.getValue()))))),

								Restrictions.or(Restrictions.eq("hari6", searchhari.getSelectedItem().getValue()),
										Restrictions.or(
												Restrictions.eq("hari7", searchhari.getSelectedItem().getValue()),
												Restrictions.or(
														Restrictions.eq("hari8",
																searchhari.getSelectedItem().getValue()),
														Restrictions.or(
																Restrictions.eq("hari9",
																		searchhari.getSelectedItem().getValue()),
																Restrictions.eq("hari10",
																		searchhari.getSelectedItem().getValue()))))))

				)

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("matapelajaran.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchguru == null || searchguru.getAttribute("guru") == null) ? Restrictions.sqlRestriction("1=1") :

						Restrictions.or(Restrictions.eq("guru10", searchguru.getAttribute("guru")), Restrictions.or(
								Restrictions.eq("guru9", searchguru.getAttribute("guru")),
								Restrictions.or(Restrictions.eq("guru8", searchguru.getAttribute("guru")), Restrictions
										.or(Restrictions.eq("guru7", searchguru.getAttribute("guru")), Restrictions.or(
												Restrictions.eq("guru6", searchguru.getAttribute("guru")),
												Restrictions.or(
														Restrictions.eq("guru5", searchguru.getAttribute("guru")),
														Restrictions.or(
																Restrictions.eq("guru4",
																		searchguru.getAttribute("guru")),
																Restrictions.or(
																		Restrictions.eq("guru3",
																				searchguru.getAttribute("guru")),
																		Restrictions.or(
																				Restrictions.eq("guru",
																						searchguru
																								.getAttribute("guru")),
																				Restrictions.eq("guru2", searchguru
																						.getAttribute("guru")))))))))))

				)

				.add(searchkelas.getSelectedItem() == null || searchkelas.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kelas", searchkelas.getSelectedItem().getValue()))

				.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
						|| searchsmt.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JadwalPelajaran> jadwalPelajaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalPelajaran);
		grid.setRowRenderer(new JadwalPelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("unchecked")
	public static String generateiIntroductoryText(JadwalPelajaran jadwalPelajaran, TreeMap<String, Long> pertemuans) {
		String introductoryText = "<h3>1. Identitas Matapelajaran</h3><table style='border: 0px solid black;width: 100%;'><tr><td style='width: 20%;'>"
				+ (jadwalPelajaran.getSekolah() == null ? ""
						: "Yayasan" + "</td><td>" + jadwalPelajaran.getSekolah().getYayasan().getNama())
				+ "</td></tr>";

		introductoryText += (jadwalPelajaran.getSekolah() == null ? ""
				: "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Sekolah") + "</td><td>"
						+ jadwalPelajaran.getSekolah().getNama() + "</td></tr>");

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Kode Matapelajaran") + "</td><td>"
				+ jadwalPelajaran.getMatapelajaran().getKode() + "</td></tr>";

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Nama Matapelajaran") + "</td><td>"
				+ jadwalPelajaran.getMatapelajaran().getNama() + "</td></tr>";

		introductoryText += "<tr><td style='width: 20%;'>" + Common.getBahasaConfig("Semester") + "</td><td>"
				+ jadwalPelajaran.getSemester() + "</td></tr>";

		String pengampu = "";
		for (Guru d : jadwalPelajaran.populateGuru().values()) {
			String m = d.getNama();
			pengampu += pengampu.isEmpty() ? m : ",<br></br>" + m;
		}

		introductoryText += "<tr><td style='width: 20%;vertical-align: top;'>"
				+ Common.getBahasaConfig("Pengampu JadwalPelajaran") + "</td><td>" + pengampu + "</td></tr>";
		Session session = HibernateUtil.currentSession();
		List<Object[]> objects = session.createCriteria(KelasSiswaPunyaSiswa.class)
				.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas())).createAlias("siswa", "siswa")
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.projectionList().add(Projections.property("siswa.nomorInduk"))
						.add(Projections.property("siswa.nama")).add(Projections.property("siswa.id")))
				.addOrder(Order.asc("nomorUrut"))	.addOrder(Order.asc("siswa.nama")).list();
		String peserta = "";
		for (Object[] o : objects) {
			String m = o[0] + " - " + o[1];
			peserta += peserta.isEmpty() ? m : ",<br></br>" + m;
		}

		if (!jadwalPelajaran.getDeskripsiPembelajaran().isEmpty()
				&& jadwalPelajaran.getMatapelajaran().getDeskripsiPembelajaran().isEmpty()) {

		}

		introductoryText += "<tr><td style='width: 20%;vertical-align: top;'>"
				+ Common.getBahasaConfig("Peserta Pembelajaran") + "</td><td>" + peserta + "</td></tr></table>";

		introductoryText += "<h3>2.	" + Common.getBahasaConfig("Deskripsi Pembelajaran") + "</h3><p>"
				+ jadwalPelajaran.getDeskripsiPembelajaran().replaceAll("\n", "<br></br>") + "</p>";

		introductoryText += "<h3>3.	" + Common.getBahasaConfig("Capaian / Kompetensi") + "</h3><p>"
				+ jadwalPelajaran.getCapaianPembelajaranProdi().replaceAll("\n", "<br></br>") + "</p>";

		introductoryText += "<h3>4.	" + Common.getBahasaConfig("Rencana Pembelajaran") + "</h3>";

		introductoryText += "<table style='border: 1px solid black;width: 100%;'>";
		introductoryText += "<tr style='border: 1px solid #ddd;'>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Minggu ke") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>"
				+ Common.getBahasaConfig("Indikator Capaian Pembelajaran") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Bahan Kajian") + "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Metode Pembelajaran")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Pengalaman Belajar")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Waktu Pembelajaran")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Tugas dan Penilaian")
				+ "</th>";
		introductoryText += "<th style='border: 1px solid #ddd;'>" + Common.getBahasaConfig("Sumber Belajar") + "</th>";
		introductoryText += "</tr>";
		int i = 1;
		for (Long pertemuanid : pertemuans.values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null) {
				introductoryText += "<tr style='border: 1px solid #ddd;hover {background-color: rgba(169,169,169,0.4);}'>";
				introductoryText += "<td style='border: 1px solid #ddd;text-align:center;'>" + i + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getIndikator().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getTopik().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getMetodePembelajaran().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getPengalamanBelajar().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getWaktupembelajaran().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getTugasDanPenilaian().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "<td style='border: 1px solid #ddd;'>"
						+ pertemuan.getBukuRujukan1().replaceAll("\n", "<br></br>") + "</td>";
				introductoryText += "</tr>";
				i++;
			}
		}

		introductoryText += "</table>";

		introductoryText += "<h3>5.	" + Common.getBahasaConfig("Daftar Rujukan") + "</h3><ol>";

		List<Item> items = session.createCriteria(JadwalPelajaranPunyaItem.class)
				.setProjection(Projections.groupProperty("item"))
				.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();
		for (Item itemData : items) {
			// CSLItemData item = ItemAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getNama() + " - " + itemData.getPengarangs() + " - "
						+ (itemData.getPenerbit() == null ? "" : itemData.getPenerbit().getNama()) + "-"
						+ (itemData.getTahun() == null || itemData.getTahun().equals(0) ? "" : itemData.getTahun())
						+ "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		List<BukuBahanAjar> bukuBahanAjars = session.createCriteria(MatapelajaranPunyaBukuBahanAjar.class)
				.setProjection(Projections.groupProperty("bukuBahanAjar"))
				.add(Restrictions.eq("matapelajaran", jadwalPelajaran.getMatapelajaran())).list();
		for (BukuBahanAjar itemData : bukuBahanAjars) {
			// CSLItemData item =
			// BukuBahanAjarAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getNama() + " - " + itemData.getPengarang1() + " - "
						+ (itemData.getPenerbit()) + "-"
						+ (itemData.getTahun() == null || itemData.getTahun().equals(0) ? "" : itemData.getTahun())
						+ "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		List<Artikel> artikels = session.createCriteria(DataPunyaArtikel.class)
				.setProjection(Projections.groupProperty("artikel"))
				.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran)).list();
		for (Artikel itemData : artikels) {
			// CSLItemData item =
			// ArtikelAction.generateCSLItemData(itemData);
			try {
				// String bibl =
				// CSL.makeAdhocBibliography("chicago-author-date",
				// item).makeString();
				introductoryText += "<li>" + itemData.getJudul() + "</li>";
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}

		introductoryText += "</ol><br></br><br></br><br></br><br></br><br></br><br></br>";

		return introductoryText;
	}

	public void onCopyJadwalPelajaran(Event event) throws Exception {
		final MyWindow window = new MyWindow("Copy Jadwal Pelajaran", "normal", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("300px");
		window.setWidth("400px");

		final Combobox tahunAkademikDari = Common.generateTahunAjaran(null);
		if (searchta.getSelectedItem() != null) {
			Common.selectComboItem(tahunAkademikDari, searchta.getSelectedItem().getValue());
		}
		final Combobox tahunAkademikKe = Common.generateTahunAjaran(null);

		final Combobox jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		jenisSemester.appendChild(comboitem);

		final Combobox jenisSemester1 = new Combobox();
		comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		jenisSemester1.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		jenisSemester1.appendChild(comboitem);

		if (searchsmt.getSelectedItem() != null) {
			Common.selectComboItem(jenisSemester, searchsmt.getSelectedItem().getValue());
			Common.selectComboItem(jenisSemester1, searchsmt.getSelectedItem().getValue());
		}

		final Combobox yayasan = new Combobox();
		final Combobox sekolah = new Combobox();

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		// grid.setOddRowSclass("non-odd");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran Dari"));
		row.appendChild(tahunAkademikDari);
		tahunAkademikDari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester Dari"));
		row.appendChild(jenisSemester1);
		jenisSemester1.setWidth("90%");
		jenisSemester1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran Ke"));
		row.appendChild(tahunAkademikKe);
		tahunAkademikKe.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester Ke"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Copy Jadwal Pelajaran", "/img/svg/edit-copy.svg");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String tahunAkademik1 = (String) (tahunAkademikDari.getSelectedItem() == null ? null
						: tahunAkademikDari.getSelectedItem().getValue());
				final String tahunAkademik2 = (String) (tahunAkademikKe.getSelectedItem() == null ? null
						: tahunAkademikKe.getSelectedItem().getValue());

				final String semester1 = (String) (jenisSemester1.getSelectedItem() == null ? null
						: jenisSemester1.getSelectedItem().getValue());

				final String semester2 = (String) (jenisSemester.getSelectedItem() == null ? null
						: jenisSemester.getSelectedItem().getValue());

				final Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				final Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());

				if (tahunAkademik1 == null) {
					MyMessageboxConfig.show("Tahun Ajaran Dari harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (tahunAkademik2 == null) {
					MyMessageboxConfig.show("Tahun Ajaran Ke harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (tahunAkademik1.equals(tahunAkademik2) && semester1.equals(semester2)) {
					MyMessageboxConfig.show("Tahun Ajaran dan semester gak boleh sama", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (mySekolah == null) {
					MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", 1, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semester1 == null) {
					MyMessageboxConfig.show("Jenis Semester dari harus diisi", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semester2 == null) {
					MyMessageboxConfig.show("Jenis Semester ke harus diisi", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				final List<JadwalPelajaran> jadwalPelajarans = ConstantValues
						.simpleList(session.createCriteria(JadwalPelajaran.class)

								.add(myYayasan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("yayasan", myYayasan))

								.add(mySekolah == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("sekolah", mySekolah))

								.add(Restrictions.eq("tahunAjaran", tahunAkademik1))
								.add(Restrictions.sqlRestriction(
										semester1.equals(JadwalPelajaran.GANJIL) ? "this_.semester % 2 = 1"
												: "this_.semester % 2 = 0")),
								JadwalPelajaran.class);

				System.out.println("jadwalPelajarans -> " + jadwalPelajarans.size());

				MyMessageboxConfig.show(
						"Apakah yakin ingin melanjutkan men-copy " + jadwalPelajarans.size()
								+ " jadwal pelajaran dari tahun ajaran " + tahunAkademik1 + " semester " + semester1
								+ " ke tahun ajaran " + tahunAkademik2 + " di semester " + semester2 + " ?",
						"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									String warning = "";
									Session session = HibernateUtil.currentSession();
									for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {

										Integer ada = ((Number) session.createCriteria(JadwalPelajaran.class)

												.add(myYayasan == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("yayasan", myYayasan))
												.add(mySekolah == null ? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("sekolah", mySekolah))
												.add(Restrictions.eq("tahunAjaran", tahunAkademik2))
												.add(Restrictions
														.sqlRestriction(semester2.equals(JadwalPelajaran.GANJIL)
																? "this_.semester % 2 = 1"
																: "this_.semester % 2 = 0"))

												.add(jadwalPelajaran.getJamPelajaran() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran",
																jadwalPelajaran.getJamPelajaran()))

												.add(jadwalPelajaran.getJamPelajaran2() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran2",
																jadwalPelajaran.getJamPelajaran2()))

												.add(jadwalPelajaran.getJamPelajaran3() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran3",
																jadwalPelajaran.getJamPelajaran3()))

												.add(jadwalPelajaran.getJamPelajaran4() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran4",
																jadwalPelajaran.getJamPelajaran4()))

												.add(jadwalPelajaran.getJamPelajaran5() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran5",
																jadwalPelajaran.getJamPelajaran5()))

												.add(jadwalPelajaran.getJamPelajaran6() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran6",
																jadwalPelajaran.getJamPelajaran6()))

												.add(jadwalPelajaran.getJamPelajaran7() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran7",
																jadwalPelajaran.getJamPelajaran7()))

												.add(jadwalPelajaran.getJamPelajaran8() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran8",
																jadwalPelajaran.getJamPelajaran8()))

												.add(jadwalPelajaran.getJamPelajaran9() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran9",
																jadwalPelajaran.getJamPelajaran9()))

												.add(jadwalPelajaran.getJamPelajaran10() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran10",
																jadwalPelajaran.getJamPelajaran10()))

												.add(jadwalPelajaran.getJamPelajaran11() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran11",
																jadwalPelajaran.getJamPelajaran11()))

												.add(jadwalPelajaran.getJamPelajaran12() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("jamPelajaran12",
																jadwalPelajaran.getJamPelajaran12()))

												.add(jadwalPelajaran.getGuru() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru", jadwalPelajaran.getGuru()))

												.add(jadwalPelajaran.getGuru2() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru2", jadwalPelajaran.getGuru2()))

												.add(jadwalPelajaran.getGuru3() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru3", jadwalPelajaran.getGuru3()))

												.add(jadwalPelajaran.getGuru4() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru4", jadwalPelajaran.getGuru4()))

												.add(jadwalPelajaran.getGuru5() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru5", jadwalPelajaran.getGuru5()))

												.add(jadwalPelajaran.getGuru6() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru6", jadwalPelajaran.getGuru6()))

												.add(jadwalPelajaran.getGuru7() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru7", jadwalPelajaran.getGuru7()))

												.add(jadwalPelajaran.getGuru8() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru8", jadwalPelajaran.getGuru8()))

												.add(jadwalPelajaran.getGuru9() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru9", jadwalPelajaran.getGuru9()))

												.add(jadwalPelajaran.getGuru10() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru10", jadwalPelajaran.getGuru10()))

												.add(jadwalPelajaran.getGuru11() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru11", jadwalPelajaran.getGuru11()))

												.add(jadwalPelajaran.getGuru12() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("guru12", jadwalPelajaran.getGuru12()))

												.add(jadwalPelajaran.getHari() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari", jadwalPelajaran.getHari()))

												.add(jadwalPelajaran.getHari2() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari2", jadwalPelajaran.getHari2()))

												.add(jadwalPelajaran.getHari3() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari3", jadwalPelajaran.getHari3()))

												.add(jadwalPelajaran.getHari4() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari4", jadwalPelajaran.getHari4()))

												.add(jadwalPelajaran.getHari5() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari5", jadwalPelajaran.getHari5()))

												.add(jadwalPelajaran.getHari6() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari6", jadwalPelajaran.getHari6()))

												.add(jadwalPelajaran.getHari7() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari7", jadwalPelajaran.getHari7()))

												.add(jadwalPelajaran.getHari8() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari8", jadwalPelajaran.getHari8()))

												.add(jadwalPelajaran.getHari9() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari9", jadwalPelajaran.getHari9()))

												.add(jadwalPelajaran.getHari10() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari10", jadwalPelajaran.getHari10()))

												.add(jadwalPelajaran.getHari11() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari11", jadwalPelajaran.getHari11()))

												.add(jadwalPelajaran.getHari12() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("hari12", jadwalPelajaran.getHari12()))

												.add(jadwalPelajaran.getProgram() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("program", jadwalPelajaran.getProgram()))

												.add(jadwalPelajaran.getMatapelajaran() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.eq("matapelajaran",
																jadwalPelajaran.getMatapelajaran()))

												.createAlias("kelas", "kelas")

												.add(jadwalPelajaran.getKelas() == null
														? Restrictions.sqlRestriction("1=1")
														: Restrictions.ilike("kelas.nama",
																jadwalPelajaran.getKelas().getNama(), MatchMode.EXACT))

												.setProjection(Projections.rowCount()).uniqueResult()).intValue();

										System.out.println("jadwalPelajaran -> " + jadwalPelajaran + ", ada " + ada);

										if (ada.equals(0)) {

											KelasSiswa kelasSiswa = (KelasSiswa) session
													.createCriteria(KelasSiswa.class)
													.add(Restrictions.eq("sekolah", mySekolah))
													.add(Restrictions.eq("tahunAjaran", tahunAkademik2))
													.add(Restrictions.ilike("nama",
															jadwalPelajaran.getKelas().getNama(), MatchMode.EXACT))
													.setMaxResults(1).uniqueResult();

											if (kelasSiswa != null) {
												JadwalPelajaran copyJadwalPelajaran = (JadwalPelajaran) jadwalPelajaran
														.clone();

												copyJadwalPelajaran.setId(null);
												copyJadwalPelajaran.setTahunAjaran(tahunAkademik2);
												copyJadwalPelajaran
														.setSemester(semester2.equals(Perkuliahan.GANJIL) ? 1 : 2);
												copyJadwalPelajaran.setKelas(kelasSiswa);

												session.save(copyJadwalPelajaran);
											} else {
												warning += "\n\nKelas " + jadwalPelajaran.getKelas().getNama()
														+ " di tahun ajaran " + tahunAkademik2 + " tidak ditemukan";
											}

										}

									}

									MyMessageboxConfig.show("Copy jadwal pelajaran telah selesai dilakukan" + warning,
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													window.detach();
													onSearchDefault(arg0);
												}
											});

								}

							}
						});

			}
		});

		save.setParent(toolbar);

		window.onModal();
	}

	public void onDeleteJadwalPelajaran(Event event) throws Exception {
		final MyWindow window = new MyWindow("Hapus Jadwal Pelajaran", "normal", true);
		page.getFirstRoot().appendChild(window);
		window.setHeight("300px");
		window.setWidth("850px");

		final Combobox tahunAkademikDari = Common.generateTahunAjaran(null);
		if (searchta.getSelectedItem() != null) {
			Common.selectComboItem(tahunAkademikDari, searchta.getSelectedItem().getValue());
		}

		final Combobox jenisSemester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(JadwalPelajaran.GANJIL);
		comboitem.setValue(JadwalPelajaran.GANJIL);
		jenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig(JadwalPelajaran.GENAP);
		comboitem.setValue(JadwalPelajaran.GENAP);
		jenisSemester.appendChild(comboitem);

		final Combobox yayasan = new Combobox();
		final Combobox sekolah = new Combobox();

		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		// grid.setOddRowSclass("non-odd");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademikDari);
		tahunAkademikDari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setWidth("90%");

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Hapus Jadwal Pelajaran", "/img/svg/trash.svg");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {

				final String tahunAkademik1 = (String) (tahunAkademikDari.getSelectedItem() == null ? null
						: tahunAkademikDari.getSelectedItem().getValue());

				final String semester = (String) (jenisSemester.getSelectedItem() == null ? null
						: jenisSemester.getSelectedItem().getValue());

				final Sekolah mySekolah = (Sekolah) (sekolah.getSelectedItem() == null
						|| sekolah.getSelectedItem().getValue() == null ? null : sekolah.getSelectedItem().getValue());
				final Yayasan myYayasan = (Yayasan) (yayasan.getSelectedItem() == null
						|| yayasan.getSelectedItem().getValue() == null ? null : yayasan.getSelectedItem().getValue());

				if (tahunAkademik1 == null) {
					MyMessageboxConfig.show("Tahun Akademik Dari harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (semester == null) {
					MyMessageboxConfig.show("Jenis Semester harus diisi", "Peringatan", 1,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				@SuppressWarnings("unchecked")
				final List<JadwalPelajaran> jadwalPelajarans = ConstantValues
						.simpleList(session.createCriteria(JadwalPelajaran.class)

								.add(myYayasan == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("yayasan", myYayasan))

								.add(mySekolah == null ? Restrictions.sqlRestriction("1=1")
										: Restrictions.eq("sekolah", mySekolah))
								.add(Restrictions.eq("tahunAjaran", tahunAkademik1))
								.add(Restrictions.sqlRestriction(
										semester.equals(JadwalPelajaran.GANJIL) ? "this_.semester % 2 = 1"
												: "this_.semester % 2 = 0")),
								JadwalPelajaran.class);

				// Htm peringatan = "";

				MyMessageboxConfig.show(
						"Apakah yakin ingin menghapus jadwal pelajaran ini ?\nCatatan : Jadwal Pelajaran yang sudah dibuatkan agenda pertemuan dan terdapat data mahasiswa-nya tidak dapat dihapus",
						"Question", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();
									for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {

										Integer count1 = jadwalPelajaran.ambilJumlahPertemuan();

										if (count1.equals(0)) {
											session.delete(jadwalPelajaran);
										}

									}

									MyMessageboxConfig.show("Hapus jadwal pelajaran berhasil dilakukan", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													window.detach();
													onSearchDefault(arg0);

												}
											});

									window.detach();

								}

							}
						});

			}
		});

		save.setParent(toolbar);

		window.onModal();
	}
}
