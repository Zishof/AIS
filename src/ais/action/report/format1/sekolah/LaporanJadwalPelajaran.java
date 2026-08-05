package ais.action.report.format1.sekolah;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konstanta;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JamPelajaran;
import ais.database.model.sekolah.JenisLaporanJadwalSekolah;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KelompokJamPelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyWindow;

public class LaporanJadwalPelajaran extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;

	private Paging paging = new Paging();
	private Textbox cari;

	private MyGrid grid;

	Map<Long, KelasSiswa> map = new java.util.HashMap<Long, KelasSiswa>();

	private MyDatebox tanggal;

	private Combobox sekolah;

	private Combobox tahunAkademik;

	private Combobox searchsmt;

	private Combobox jenisLaporanJadwalSekolah;

	public LaporanJadwalPelajaran() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Jadwal Pelajaran", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

		Common.initPaging25(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
		Tbmuser tbmuser = Common.getCurrentUser();

		Tabpanel tabpanel1 = null;
		if (tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null) {

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(Common.tampilanScrollTabbox(this));
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			MyTabConfig tab1 = new MyTabConfig("Jadwal Pelajaran");
			tab1.setParent(tabs);

			MyTabConfig tab51 = new MyTabConfig("Jenis Jadwal Pelajaran");
			tab51.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			tabpanel1 = new ais.ui.util.MyTabpanel();
			tabpanel1.setParent(tabpanels);

			final Tabpanel tabpanel51 = new ais.ui.util.MyTabpanel();
			tabpanel51.setParent(tabpanels);
			tab51.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (tabpanel51.getChildren().size() == 0) {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("100%");
						window.setWidth("100%");
						window.setParent(tabpanel51);
						MyInclude iframe = new MyInclude("/pages/master/sekolah/jenis_laporan_jadwal_sekolah.zul");
						iframe.setParent(window);
					}
				}
			});

		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanel1 == null ? this : tabpanel1);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		Borderlayout borderlayout1 = new ais.ui.util.MyBorderlayout();
		borderlayout1.setParent(west);

		North north = new North();
		north.setParent(borderlayout1);
		north.setHeight("160px");
		north.setBorder("none");

		MyGrid mygrid = new MyGrid();// grid.setOddRowSclass("non-odd");
		mygrid.setWidth("100%");
		mygrid.setParent(north);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");
		mygrid.setSclass("fgrid");

		Columns columns = new Columns();
		columns.setParent(mygrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("60px");
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tgl : "));
		tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
		tanggal.setFormat(Common.dateFormat1.get().toPattern());
		tanggal.setReadonly(true);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		hbox.appendChild(tanggal);

		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan Jadwal");
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Kelas : "));

		hbox = new Hbox();
		hbox.setParent(row);

		hbox.appendChild(sekolah = new Combobox());
		sekolah.setWidth("60px");
		Common.insertComboDanSemua(sekolah, "nama", Sekolah.class, Restrictions.sqlRestriction("true"));

		Sekolah s = SekolahUtil.getSekolah();
		if (s != null && s.getId() != null) {
			Common.selectComboItem(true, sekolah, s);
			sekolah.setDisabled(true);
		}

		cari = new Textbox();
		cari.setParent(hbox);
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		button = new MyButtonConfig("Cari");
		button.setParent(hbox);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("TA : "));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");

		tahunAkademik.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Smt : "));
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(1);
		searchsmt = new Combobox();
		searchsmt.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(2);
		searchsmt.appendChild(comboitem);
		searchsmt.setCols(2);

		Common.selectComboItem(searchsmt, Common.isNowSemensterGanjil() ? 1 : 2);
		searchsmt.setReadonly(true);
		row.appendChild(searchsmt);
		searchsmt.setWidth("90%");

		searchsmt.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Laporan *"));
		row.appendChild(jenisLaporanJadwalSekolah = new Combobox());
		jenisLaporanJadwalSekolah.setWidth("90%");
		jenisLaporanJadwalSekolah.setReadonly(true);

		EventListener eventListenerJenis = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertCombo(jenisLaporanJadwalSekolah, new String[] { "nama", "kode" }, "keterangan",
						JenisLaporanJadwalSekolah.class,
						Restrictions.and(Restrictions.eq("sekolah", s), Restrictions.eq("aktif", true)));
				jenisLaporanJadwalSekolah.setReadonly(true);
				if (jenisLaporanJadwalSekolah.getChildren().size() > 0) {
					jenisLaporanJadwalSekolah.setSelectedIndex(0);
				}

				if (jenisLaporanJadwalSekolah.getChildren().size() == 1) {
					jenisLaporanJadwalSekolah.setDisabled(true);
				}

			}

		};
		sekolah.addEventListener("onChange", eventListenerJenis);

		Common.createDefaultTimer(eventListenerJenis);

		Center center1 = new Center();
		center1.setParent(borderlayout1);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		South south1 = new South();
		south1.setParent(borderlayout1);
		south1.setHeight("40px");

		Vbox vbox = new Vbox();
		vbox.setParent(south1);

		paging.setParent(vbox);
		paging.setHeight("30px");

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center1);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);
		column = new MyColumnConfig();
		column.setWidth("45px");
		column.setParent(columns);

		column = new MyColumnConfig("Kelas");
		column.setParent(columns);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		String namaFile = "sekolah/report";

		north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				return generateParameter();
			}
		}, namaFile, null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}, false));

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	@SuppressWarnings("unchecked")
	protected void onSearchDefault(Object object) {
		Common.initPaging25(initCriteria(false), paging);
		List<KelasSiswa> kelasSiswaPunyaSiswas = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_25)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE_25 * (paging == null ? 0 : paging.getActivePage())),
				KelasSiswa.class);

		List<KelasSiswa> siswas = new ArrayList<KelasSiswa>();
		siswas.addAll(map.values());
		siswas.addAll(kelasSiswaPunyaSiswas);
		ListModel strset = new SimpleListModel(siswas);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);

		if (idsSiswa != null && !idsSiswa.isEmpty()) {
			onReport(null);
		}
	}

	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KelasSiswa kelasSiswa = (KelasSiswa) arg1;

			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setChecked(map.keySet().contains(kelasSiswa.getId())
					|| (idsSiswa != null && idsSiswa.contains(kelasSiswa.getId())));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						map.put(kelasSiswa.getId(), kelasSiswa);
					} else {
						map.remove(kelasSiswa.getId());
					}
				}
			});

			RevisiHelper.createNewRevisi(KelasSiswa.class, kelasSiswa, kelasSiswa.getNama()).setParent(arg0);

		}

	}

	private List<Long> idsSiswa = null;

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		idsSiswa = null;
		Session session = HibernateUtil.currentSession();

		List<Long> ids = new ArrayList<Long>();
		Guru guru = null;
		Tbmuser tbmuser = Common.getCurrentUser();
		guru = tbmuser.ambilGuru();
		if (guru != null) {
			ids = session.createCriteria(JadwalPelajaran.class).add(Restrictions.isNotNull("kelas"))
					.setProjection(Projections.groupProperty("kelas.id"))

					.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))

					.add(searchsmt.getSelectedItem() == null || searchsmt.getSelectedItem().getValue() == null
							? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("semester", searchsmt.getSelectedItem().getValue()))

					.add(guru == null ? Restrictions.sqlRestriction("1=1") :

							Restrictions.or(Restrictions.eq("guru12", guru), Restrictions.or(
									Restrictions.eq("guru11", guru),
									Restrictions.or(Restrictions.eq("guru10", guru), Restrictions.or(
											Restrictions.eq("guru9", guru),
											Restrictions.or(Restrictions.eq("guru8", guru), Restrictions.or(
													Restrictions.eq("guru7", guru),
													Restrictions.or(Restrictions.eq("guru6", guru), Restrictions.or(
															Restrictions.eq("guru5", guru),
															Restrictions.or(Restrictions.eq("guru4", guru), Restrictions
																	.or(Restrictions.eq("guru3", guru), Restrictions.or(
																			Restrictions.eq("guru", guru),
																			Restrictions.eq("guru2", guru))))))))))))

					).list();

		}

		if (tbmuser.getSiswa() != null) {
			idsSiswa = session.createCriteria(KelasSiswaPunyaSiswa.class).add(Restrictions.isNotNull("kelasSiswa"))
					.add(Restrictions.eq("siswa", tbmuser.getSiswa()))
					.setProjection(Projections.groupProperty("kelasSiswa.id")).list();
		}

		Criteria criteria = session.createCriteria(KelasSiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))

				.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false));

		criteria.add(Restrictions.or(
				guru != null ? Restrictions.eq("guruPembina", guru) : Restrictions.sqlRestriction("false"),
				guru != null && ids.isEmpty() ? Restrictions.sqlRestriction("false")
						: ids.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", ids)))

				.add(tbmuser.getSiswa() != null
						? idsSiswa.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", idsSiswa)
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));

		Criterion criterion = Restrictions.ilike("nama", cari.getValue().trim(), MatchMode.ANYWHERE);
		criterion = Restrictions.or(criterion,
				Restrictions.ilike("keterangan", cari.getValue().trim(), MatchMode.ANYWHERE));

		criteria.add(map.isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.not(Restrictions.in("id", map.keySet())))
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : criterion);
		return criteria;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Session session = HibernateUtil.currentSession();

		Integer smt = (Integer) searchsmt.getSelectedItem().getValue();

		List<KelompokJamPelajaran> kelompokJamPelajarans = session.createCriteria(KelompokJamPelajaran.class)
				.addOrder(Order.asc("nama")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: CommonSearchFilterHelper.eqSelectedWithId("sekolah", sekolah, false))
				.list();

		Map parameters = ais.common.HashMapGenerator.getRand();

		// EKSPOR DINAMIS: jrxml terupload per jenis dipakai utk SEMUA format cetak (PDF/XLS/DOCX/PPTX).
		try {
			JenisLaporanJadwalSekolah jDinamis = (JenisLaporanJadwalSekolah) (jenisLaporanJadwalSekolah
					.getSelectedItem() == null ? null : jenisLaporanJadwalSekolah.getSelectedItem().getValue());
			if (jDinamis != null && jDinamis.getId() != null) {
				LampiranLain layoutDinamis = LampiranLain.ambil(jDinamis.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_JADWAL);
				if (layoutDinamis != null && layoutDinamis.ambilFile() != null
						&& layoutDinamis.ambilFile().exists()) {
					parameters.put("nama_laporan", layoutDinamis.ambilFile().getAbsolutePath());
				}
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/report/format1/sekolah/LaporanJadwalPelajaran.java:556");
		}

		for (Object o : ConstantValues.ambilBerdasarClass(Konstanta.class).values()) {
			Konstanta konstanta = (Konstanta) o;
			if (konstanta != null) {
				parameters.put(konstanta.getKode(), konstanta.getKeterangan());
			}
		}

		parameters.put("smt_angka", smt);
		parameters.put("smt", smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		parameters.put("ta", tahunAkademik.getSelectedItem().getValue());

		TreeMap<String, List<Map>> treeMap = new TreeMap<String, List<Map>>();

		for (KelasSiswa kelasSiswa : map.values()) {

			for (KelompokJamPelajaran kelompokJamPelajaran : kelompokJamPelajarans) {

				Criterion criterion = Restrictions.eq("jamPelajaran.kelompokJamPelajaran", kelompokJamPelajaran);
				criterion = Restrictions.or(Restrictions.eq("jamPelajaran2.kelompokJamPelajaran", kelompokJamPelajaran),
						criterion);
				criterion = Restrictions.or(Restrictions.eq("jamPelajaran3.kelompokJamPelajaran", kelompokJamPelajaran),
						criterion);
				criterion = Restrictions.or(Restrictions.eq("jamPelajaran4.kelompokJamPelajaran", kelompokJamPelajaran),
						criterion);
				criterion = Restrictions.or(Restrictions.eq("jamPelajaran5.kelompokJamPelajaran", kelompokJamPelajaran),
						criterion);

				criterion = Restrictions.or(Restrictions.eq("jamPelajaran6.kelompokJamPelajaran", kelompokJamPelajaran),
						criterion);
				criterion = Restrictions.or(Restrictions.eq("jamPelajaran7.kelompokJamPelajaran", kelompokJamPelajaran),
						criterion);
				criterion = Restrictions.or(Restrictions.eq("jamPelajaran8.kelompokJamPelajaran", kelompokJamPelajaran),
						criterion);
				criterion = Restrictions.or(Restrictions.eq("jamPelajaran9.kelompokJamPelajaran", kelompokJamPelajaran),
						criterion);
				criterion = Restrictions
						.or(Restrictions.eq("jamPelajaran10.kelompokJamPelajaran", kelompokJamPelajaran), criterion);

				List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(
						session.createCriteria(JadwalPelajaran.class)
								.createAlias("jamPelajaran", "jamPelajaran", Criteria.LEFT_JOIN)
								.createAlias("jamPelajaran2", "jamPelajaran2", Criteria.LEFT_JOIN)
								.createAlias("jamPelajaran3", "jamPelajaran3", Criteria.LEFT_JOIN)
								.createAlias("jamPelajaran4", "jamPelajaran4", Criteria.LEFT_JOIN)
								.createAlias("jamPelajaran5", "jamPelajaran5", Criteria.LEFT_JOIN)

								.createAlias("jamPelajaran6", "jamPelajaran6", Criteria.LEFT_JOIN)
								.createAlias("jamPelajaran7", "jamPelajaran7", Criteria.LEFT_JOIN)
								.createAlias("jamPelajaran8", "jamPelajaran8", Criteria.LEFT_JOIN)
								.createAlias("jamPelajaran9", "jamPelajaran9", Criteria.LEFT_JOIN)
								.createAlias("jamPelajaran10", "jamPelajaran10", Criteria.LEFT_JOIN)

								.add(criterion).add(Restrictions.isNotNull("matapelajaran"))
								.add(Restrictions.eq("semester", smt)).add(Restrictions.eq("kelas", kelasSiswa)),
						JadwalPelajaran.class);

				System.out.println("jadwalPelajarans -> " + jadwalPelajarans.size());

				for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {

					if (jadwalPelajaran.getHari() != null && jadwalPelajaran.getJamPelajaran() != null
							&& jadwalPelajaran.getJamPelajaran().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();
						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari());

						mapsData.put("guru", jadwalPelajaran.getGuru());

						if (jadwalPelajaran.getGuru() != null) {
							String urlFotoGuru = CommonMedia.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}

					}
					if (jadwalPelajaran.getHari2() != null && jadwalPelajaran.getJamPelajaran2() != null
							&& jadwalPelajaran.getJamPelajaran2().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran2().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran2().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);

						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran2().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran2());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari2());
						mapsData.put("guru", jadwalPelajaran.getGuru2());

						if (jadwalPelajaran.getGuru2() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru2()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}

					}
					if (jadwalPelajaran.getHari3() != null && jadwalPelajaran.getJamPelajaran3() != null
							&& jadwalPelajaran.getJamPelajaran3().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran3().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran3().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran3().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran3());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari3());
						mapsData.put("guru", jadwalPelajaran.getGuru3());

						if (jadwalPelajaran.getGuru3() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru3()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}

					}
					if (jadwalPelajaran.getHari4() != null && jadwalPelajaran.getJamPelajaran4() != null
							&& jadwalPelajaran.getJamPelajaran4().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran4().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran4().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran4().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran4());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari4());
						mapsData.put("guru", jadwalPelajaran.getGuru4());

						if (jadwalPelajaran.getGuru4() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru4()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}

					}
					if (jadwalPelajaran.getHari5() != null && jadwalPelajaran.getJamPelajaran5() != null
							&& jadwalPelajaran.getJamPelajaran5().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran5().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran5().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran5().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran5());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari5());
						mapsData.put("guru", jadwalPelajaran.getGuru5());

						if (jadwalPelajaran.getGuru5() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru5()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}
					}

					if (jadwalPelajaran.getHari6() != null && jadwalPelajaran.getJamPelajaran6() != null
							&& jadwalPelajaran.getJamPelajaran6().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran6().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran6().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran6().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran6());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari6());
						mapsData.put("guru", jadwalPelajaran.getGuru6());

						if (jadwalPelajaran.getGuru6() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru6()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}
					}

					if (jadwalPelajaran.getHari7() != null && jadwalPelajaran.getJamPelajaran7() != null
							&& jadwalPelajaran.getJamPelajaran7().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran7().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran7().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran7().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran7());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari7());
						mapsData.put("guru", jadwalPelajaran.getGuru7());

						if (jadwalPelajaran.getGuru7() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru7()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}
					}

					if (jadwalPelajaran.getHari8() != null && jadwalPelajaran.getJamPelajaran8() != null
							&& jadwalPelajaran.getJamPelajaran8().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran8().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran8().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran8().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran8());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari8());
						mapsData.put("guru", jadwalPelajaran.getGuru8());

						if (jadwalPelajaran.getGuru8() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru8()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}
					}

					if (jadwalPelajaran.getHari9() != null && jadwalPelajaran.getJamPelajaran9() != null
							&& jadwalPelajaran.getJamPelajaran9().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran9().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran9().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran9().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran9());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari9());
						mapsData.put("guru", jadwalPelajaran.getGuru9());

						if (jadwalPelajaran.getGuru9() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru9()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}
					}

					if (jadwalPelajaran.getHari10() != null && jadwalPelajaran.getJamPelajaran10() != null
							&& jadwalPelajaran.getJamPelajaran10().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran10().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran10().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran10().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran10());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari10());
						mapsData.put("guru", jadwalPelajaran.getGuru10());

						if (jadwalPelajaran.getGuru10() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru10()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}
					}

					if (jadwalPelajaran.getHari11() != null && jadwalPelajaran.getJamPelajaran11() != null
							&& jadwalPelajaran.getJamPelajaran11().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran11().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran11().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran11().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran11());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari11());
						mapsData.put("guru", jadwalPelajaran.getGuru11());

						if (jadwalPelajaran.getGuru11() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru11()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}
					}

					if (jadwalPelajaran.getHari12() != null && jadwalPelajaran.getJamPelajaran12() != null
							&& jadwalPelajaran.getJamPelajaran12().getKelompokJamPelajaran() != null) {

						String key = (jadwalPelajaran.getJamPelajaran12().getKelompokJamPelajaran().getNama() + "_"
								+ jadwalPelajaran.getJamPelajaran12().getKelompokJamPelajaran().getId()) + "_"
								+ kelasSiswa.getId();

						List<Map> mapsList = treeMap.get(key);
						if (mapsList == null) {
							mapsList = new ArrayList<Map>();
							treeMap.put(key, mapsList);
						}
						Map mapsData = new HashMap();
						mapsList.add(mapsData);
						Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
						mapsData.put("semester_tingkat", semester_tingkat);
						mapsData.put("tingkat", kelasSiswa.getTingkat());
						mapsData.put("kelasSiswa", kelasSiswa);
						mapsData.put("kelompokJamPelajaran",
								jadwalPelajaran.getJamPelajaran12().getKelompokJamPelajaran());
						mapsData.put("jamPelajaran", jadwalPelajaran.getJamPelajaran12());
						mapsData.put("jadwalPelajaran", jadwalPelajaran);
						mapsData.put("hari", jadwalPelajaran.getHari12());
						mapsData.put("guru", jadwalPelajaran.getGuru12());

						if (jadwalPelajaran.getGuru12() != null) {
							String urlFotoGuru = CommonMedia
									.getUrlFotoPengguna(new Tbmuser(jadwalPelajaran.getGuru12()));
							mapsData.put("urlFotoGuru", urlFotoGuru);
						}
					}
				}

			}

		}

		List list = new ArrayList();
		Map<Long, Map> listMaps = new HashMap<Long, Map>();
		for (String key : treeMap.keySet()) {
			List<Map> mapsList = treeMap.get(key);

			for (Map mapdata : mapsList) {

				KelompokJamPelajaran kelompokJamPelajaran = (KelompokJamPelajaran) mapdata.get("kelompokJamPelajaran");
				KelasSiswa kelasSiswa = (KelasSiswa) mapdata.get("kelasSiswa");
				Map parametersMap = listMaps.get(kelompokJamPelajaran.getId());
				if (parametersMap == null) {
					parametersMap = new HashMap();

					Common.insertProperty(KelasSiswa.class, kelasSiswa, parametersMap, "kelas");
					for (String hari : Common.haris) {
						parametersMap.put(hari.toLowerCase() + ".matapelajaran.kode", "");
						parametersMap.put(hari.toLowerCase() + ".matapelajaran.nama", "");
						parametersMap.put(hari.toLowerCase() + ".matapelajaran.kkm", 0);
						parametersMap.put(hari.toLowerCase() + ".waktuMulai", "");
						parametersMap.put(hari.toLowerCase() + ".waktuSelesai", "");
						parametersMap.put(hari.toLowerCase() + ".ruang.kode", "");
						parametersMap.put(hari.toLowerCase() + ".ruang.nama", "");
						parametersMap.put(hari.toLowerCase() + ".guru.kode", "");
						parametersMap.put(hari.toLowerCase() + ".guru.nama", "");
					}

					listMaps.put(kelompokJamPelajaran.getId(), parametersMap);
					list.add(parametersMap);
				}
				parametersMap.put("kelompokJamPelajaran",
						kelompokJamPelajaran == null || kelompokJamPelajaran.getId() == null ? -1L : kelompokJamPelajaran.getId());
				parametersMap.put("kelompokJamPelajaran.nama",
						kelompokJamPelajaran == null ? "" : kelompokJamPelajaran.getNama());
				parametersMap.put("kelompokJamPelajaran.keterangan",
						kelompokJamPelajaran == null ? "" : kelompokJamPelajaran.getKeterangan());
				parametersMap.put("kelas", kelasSiswa.getId());

				Integer semester_tingkat = ((kelasSiswa.getTingkat() - 1) * 2) + smt;
				parametersMap.put("semester_tingkat", semester_tingkat);
				parametersMap.put("tingkat", kelasSiswa.getTingkat());

				String hari = (String) mapdata.get("hari");
				JamPelajaran jamPelajaran = (JamPelajaran) mapdata.get("jamPelajaran");
				JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) mapdata.get("jadwalPelajaran");
				Guru guru = (Guru) mapdata.get("guru");

				if (jadwalPelajaran.getMasaJadwalPelajaran() != null) {
					parameters.put("masa_mulai", jadwalPelajaran.getMasaJadwalPelajaran().getMulai());
					parameters.put("masa_sampai", jadwalPelajaran.getMasaJadwalPelajaran().getSampai());

					parameters.put("masa_mulai.formated1",
							Common.dateFormat6.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getMulai()));
					parameters.put("masa_mulai.formated2",
							Common.dateFormat2.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getMulai()));
					parameters.put("masa_mulai.formated3",
							Common.dateFormat51.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getMulai()));
					parameters.put("masa_mulai.formated4",
							Common.timeFormat.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getMulai()));
					parameters.put("masa_mulai.formated5",
							Common.dateFormat1.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getMulai()));

					parameters.put("masa_sampai.formated1",
							Common.dateFormat6.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getSampai()));
					parameters.put("masa_sampai.formated2",
							Common.dateFormat2.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getSampai()));
					parameters.put("masa_sampai.formated3",
							Common.dateFormat51.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getSampai()));
					parameters.put("masa_sampai.formated4",
							Common.timeFormat.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getSampai()));
					parameters.put("masa_sampai.formated5",
							Common.dateFormat1.get().format(jadwalPelajaran.getMasaJadwalPelajaran().getSampai()));
				}

				masukkanData(parametersMap, hari, jamPelajaran, jadwalPelajaran, guru);
			}

		}

		parameters.put("tanggal", tanggal.getValue());
		parameters.put("maps", list);

		try {
			Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
			Yayasan y = s == null ? null : s.getYayasan();

			Integer thn = Integer.parseInt(tahunAkademik.getSelectedItem().getValue().toString().split("/")[0]);

			Map<String, Double[]> maps = NilaiHurufSekolah.getNilaiHurufSekolah(thn, s, y,
					tahunAkademik.getSelectedItem().getValue().toString(), null);

			String nilaiHuruf = "";
			for (String key : maps.keySet()) {
				Double[] nilais = maps.get(key);
				String d = key + ":" + nilais[0] + ":" + nilais[1];
				nilaiHuruf += nilaiHuruf.isEmpty() ? d : ";" + d;
				parameters.put("nilai_huruf_" + key, nilais[0] + ";" + nilais[1]);
			}

			parameters.put("nilaiHuruf", nilaiHuruf);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sekolah/LaporanJadwalPelajaran.java:1105");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Jadwal Pelajaran", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void masukkanData(Map parametersMap, String hari, JamPelajaran jamPelajaran, JadwalPelajaran jadwalPelajaran,
			Guru guru) throws Exception {
		parametersMap.put(hari.toLowerCase() + ".matapelajaran.kode", jadwalPelajaran.getMatapelajaran().getKode());
		parametersMap.put(hari.toLowerCase() + ".matapelajaran.nama", jadwalPelajaran.getMatapelajaran().getNama());
		parametersMap.put(hari.toLowerCase() + ".matapelajaran.kkm", jadwalPelajaran.getMatapelajaran().getKkm());
		parametersMap.put(hari.toLowerCase() + ".waktuMulai", jamPelajaran.getMulaiS());
		parametersMap.put(hari.toLowerCase() + ".waktuSelesai", jamPelajaran.getSampaiS());
		parametersMap.put(hari.toLowerCase() + ".namaJadwal", jamPelajaran.getNama());
		parametersMap.put(hari.toLowerCase() + ".ruang.kode",
				jadwalPelajaran.getRuang() == null ? "" : jadwalPelajaran.getRuang().getKode());
		parametersMap.put(hari.toLowerCase() + ".ruang.nama",
				jadwalPelajaran.getRuang() == null ? "" : jadwalPelajaran.getRuang().getNama());
		parametersMap.put(hari.toLowerCase() + ".guru.kode", guru == null ? "" : guru.getKode());
		parametersMap.put(hari.toLowerCase() + ".guru.nama", guru == null ? "" : guru.getNama());

		if (guru != null) {
			String urlFotoGuru = CommonMedia.getUrlFotoPengguna(new Tbmuser(guru));
			parametersMap.put(hari.toLowerCase() + ".guru.foto", urlFotoGuru);
		}

	}

	@SuppressWarnings({})
	public void onReport(Event event) {
		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					JenisLaporanJadwalSekolah j = (JenisLaporanJadwalSekolah) (jenisLaporanJadwalSekolah
							.getSelectedItem() == null ? null : jenisLaporanJadwalSekolah.getSelectedItem().getValue());
					if (j != null) {

						LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(),
								LampiranLain.FILE_JRXML_LAYOUT_JENIS_JADWAL);

						if (lainMahasiswa == null) {
							MyMessageboxConfig.show("File laporan jadwal pelajaran belum diupload", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						File file = Report.generateCompileFileReport(Report.PDF, generateParameter(),
								lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
						CommonReport.tampilkanReportPDF(center, file);

					} else {

						MyMessageboxConfig.show("File laporan jadwal pelajaran belum diupload", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Jadwal Pelajaran", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

	}

}
