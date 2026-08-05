package ais.action.master.bkd;


import ais.common.CommonSearchFilterHelper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Vbox;

import ais.action.master.MahasiswaRequestTugasAkhirAction;
import ais.action.master.bkd.helper.PenilaianAsesorHelper;
import ais.action.master.helper.AktifitasTugasAkhirHelper;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyTabConfig;

public class BimbinganSkripsiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;

	protected AmbilDataDosenBanbox searchdosen;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	protected Combobox searchprogram;
	protected Combobox searchfakultas;
	protected Combobox searchjurusan;

	private Dosen dosenTerpilih;

	private boolean ases;
	private static AktifitasTugasAkhirHelper aktifitasTugasAkhirHelper = new AktifitasTugasAkhirHelper();

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

		if (execution.getParameter("dosen") != null) {
			dosenTerpilih = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen").trim()))).uniqueResult();
		}

		if (execution.getParameter("ases") != null) {
			ases = Boolean.parseBoolean(execution.getParameter("ases"));
		}

		if (dosenTerpilih != null) {
			searchdosen.setAttribute("dosen", dosenTerpilih);
			searchdosen.setAttribute("myValue", dosenTerpilih);
			searchdosen.setValue(dosenTerpilih.getNama());
			searchdosen.setDisabled(true);
		}

		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		Common.initPrograms(searchprogram);

		Tbmuser tbmuser = Common.getCurrentUser();
		// System.out.println(users);
		if (tbmuser != null && tbmuser.ambilDosen() != null && tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			// Common.selectComboItem(searchdosen, dosen);
			searchdosen.setValue(dosen.getNama());
			searchdosen.setAttribute("myValue", dosen);
			searchdosen.setAttribute("dosen", dosen);
			searchdosen.setDisabled(true);
		}

		searchdosen.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);

			}
		});

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	public static void displayRow(Center center, final PenilaianAsesor penilaianAsesor) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
		tabSoal.setParent(tabs);

		final MyTabConfig tabPengajaran = new MyTabConfig("Rincian Bimbingan");
		tabPengajaran.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanelUtama.setStyle("min-height: 300px;");
		tabpanelUtama.setParent(tabpanels);

		PenilaianAsesorHelper.formNilai(penilaianAsesor.getAsesemenPenilaian().getPegawai(),
				penilaianAsesor.getAsesemenPenilaian().getJenjang(),
				penilaianAsesor.getAsesemenPenilaian().getTahunAkademik(),
				penilaianAsesor.getAsesemenPenilaian().getSemester(), "SK",
				penilaianAsesor.getAsesemenPenilaian().getSpesifikasi(), new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

					}
				}).setParent(tabpanelUtama);

		final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
		jurusanTabpanel.setParent(tabpanels);
		jurusanTabpanel.setWidth("100%");

		tabPengajaran.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Dosen dosen = penilaianAsesor.getAsesemenPenilaian().getPegawai().getDosen();
				if (dosen != null && jurusanTabpanel.getChildren().isEmpty()) {
					Criterion criterion = Restrictions.sqlRestriction("false");
					if (penilaianAsesor.getAsesemenPenilaian().getSpesifikasi()
							.endsWith(PenilaianAsesor.PEMBIMBING_TA)) {
						criterion = Restrictions.eq("dosen1", dosen);
						criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosen));
						criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
					} else if (penilaianAsesor.getAsesemenPenilaian().getSpesifikasi()
							.endsWith(PenilaianAsesor.PENGUJI_PROPOSAL_TA)) {
						criterion = Restrictions.eq("dosen4", dosen);
						criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
					}

					Session session = HibernateUtil.currentSession();

					List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = session
							.createCriteria(MahasiswaRequestTugasAkhir.class)
							.add(Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
									Restrictions.or(Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
											Restrictions.or(
													Restrictions.eq("status",
															MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
													Restrictions.eq("status",
															MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
							.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
							.add(Restrictions
									.eq("jurusan.jenjang", penilaianAsesor.getAsesemenPenilaian().getJenjang()))
							.add(criterion)
							.add(Restrictions.eq("tahunAkademik",
									penilaianAsesor.getAsesemenPenilaian().getTahunAkademik()))
							.add(Restrictions.in("semester",
									penilaianAsesor.getAsesemenPenilaian().getSemester().equals(Perkuliahan.GANJIL)
											? Common.ganjil
											: Common.genap))
							.list();

					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(jurusanTabpanel);
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					MyGrid grid = new MyGrid();
					grid.setMold("paging");
					grid.setPageSize(10);grid.getPagingChild().setMold("os");
					grid.setParent(center);
					Rows rows = new Rows();
					rows.setParent(grid);
					for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
						Row row = new Row();row.setValign("top");
						row.setParent(rows);

						BimbinganSkripsiAction.displayRow(row, mahasiswaRequestTugasAkhir,
								penilaianAsesor.getAsesemenPenilaian().getPegawai(), false);
					}

				}
			}
		});
	}

	public static void displayRow(Row arg0, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final Pegawai pegawai, final Boolean ases) throws Exception {
		final Vbox vboxKeterangan = new Vbox();
		final EventListener keteranganEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(vboxKeterangan);

				MahasiswaRequestTugasAkhirAction.tampilkanInfoDosen(mahasiswaRequestTugasAkhir, true)
						.setParent(vboxKeterangan);

			}
		};

		final MyDetail detail = new MyDetail();
		detail.setParent(arg0);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				Common.clear(detail);
				if (detail.isOpen()) {

					Set<Long> treeMap = new HashSet<Long>();
					if (mahasiswaRequestTugasAkhir.getDosen1() != null) {
						treeMap.add(mahasiswaRequestTugasAkhir.getDosen1().getId());
					}
					if (mahasiswaRequestTugasAkhir.getDosen2() != null) {
						treeMap.add(mahasiswaRequestTugasAkhir.getDosen2().getId());
					}
					if (mahasiswaRequestTugasAkhir.getDosen3() != null) {
						treeMap.add(mahasiswaRequestTugasAkhir.getDosen3().getId());
					}
					if (mahasiswaRequestTugasAkhir.getDosen4() != null) {
						treeMap.add(mahasiswaRequestTugasAkhir.getDosen4().getId());
					}
					if (mahasiswaRequestTugasAkhir.getDosen5() != null) {
						treeMap.add(mahasiswaRequestTugasAkhir.getDosen5().getId());
					}

					if (ases && pegawai != null && pegawai.getDosen() != null
							&& treeMap.contains(pegawai.getDosen().getId())
							&& (mahasiswaRequestTugasAkhir.getStatus()
									.equals(MahasiswaRequestTugasAkhir.MENGULANG_STATUS)
									|| mahasiswaRequestTugasAkhir.getStatus()
											.equals(MahasiswaRequestTugasAkhir.LULUS_STATUS)
									|| mahasiswaRequestTugasAkhir.getStatus()
											.equals(MahasiswaRequestTugasAkhir.SEMINAR_STATUS)
									|| mahasiswaRequestTugasAkhir.getStatus()
											.equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS))) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						final MyTabConfig tabSoal = new MyTabConfig("Penilaian Asesor");
						tabSoal.setParent(tabs);

						final MyTabConfig tabPengajaran = new MyTabConfig("Rincian Bimbingan");
						tabPengajaran.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setStyle("min-height: 300px;");
						tabpanelUtama.setParent(tabpanels);

						PenilaianAsesorHelper
								.formNilai(pegawai, mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getJenjang(),
										mahasiswaRequestTugasAkhir.getTahunAkademik(),
										mahasiswaRequestTugasAkhir.getSemester() % 2 == 0 ? Perkuliahan.GENAP
												: Perkuliahan.GANJIL,
										"SK pembimbing", PenilaianAsesor.PEMBIMBING_TA, keteranganEventListener)
								.setParent(tabpanelUtama);

						final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
						jurusanTabpanel.setParent(tabpanels);
						jurusanTabpanel.setWidth("100%");

						tabPengajaran.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (jurusanTabpanel.getChildren().isEmpty()) {

									ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
									groupbox.setStyle("min-height: 700px;");
									aktifitasTugasAkhirHelper.initDetail(mahasiswaRequestTugasAkhir, groupbox);
									jurusanTabpanel.appendChild(groupbox);

								}
							}
						});
					} else {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						aktifitasTugasAkhirHelper.initDetail(mahasiswaRequestTugasAkhir, groupbox);
						detail.appendChild(groupbox);
					}

				}
			}
		};

		detail.addEventListener("onOpen", eventListener);

		if (ases && (mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS))) {
			detail.setOpen(true);
			eventListener.onEvent(null);
		}

		MahasiswaRequestTugasAkhirAction.tampilkanInfoMahasiswa(mahasiswaRequestTugasAkhir, null).setParent(arg0);

		Vbox vbox = new Vbox();
		vbox.setParent(arg0);
		new Label(mahasiswaRequestTugasAkhir.getStatus()).setParent(vbox);
		if (mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.AKTIF_STATUS)
				|| mahasiswaRequestTugasAkhir.getStatus().equals(MahasiswaRequestTugasAkhir.LULUS_STATUS)) {
			MahasiswaRequestTugasAkhirAction.tombolCetakPengantar(mahasiswaRequestTugasAkhir).setParent(vbox);
		}

		vboxKeterangan.setParent(arg0);
		keteranganEventListener.onEvent(null);
	}

	class MahasiswaRequestTugasAkhirRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) arg1;
			Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");
			BimbinganSkripsiAction.displayRow(arg0, mahasiswaRequestTugasAkhir,
					dosen == null || dosen.getPegawaiId() == null ? null : new Pegawai(dosen), ases);
		}

	}

	public Criteria initCriteria(boolean order) {

		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");

		Criterion criterion = Restrictions.sqlRestriction("1=1");

		if (dosen != null) {
			criterion = Restrictions.eq("dosen1", dosen);
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosen));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
			criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MahasiswaRequestTugasAkhir.class).add(criterion);

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.createAlias("mahasiswa", "mahasiswa")
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.program", searchprogram.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisSemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction(
								searchJenisSemester.getSelectedItem().getValue().equals(Perkuliahan.GENAP)
										? "this_.semester % 2 = 0"
										: "this_.semester % 2 = 1"))

				.createCriteria("mahasiswa.jurusan", Criteria.LEFT_JOIN)
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhir = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(mahasiswaRequestTugasAkhir);
		grid.setRowRenderer(new MahasiswaRequestTugasAkhirRenderer());
		grid.setModelCheckMobile(strset);

	}

}
