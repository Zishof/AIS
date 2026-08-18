package ais.action.master.bkd;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesemenPenilaian;
import ais.database.model.AsesorPegawai;
import ais.database.model.AsesorPenunjangKinerjaDosen;
import ais.database.model.Pegawai;
import ais.database.model.PenilaianAsesor;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class PegawaiAction extends GenericAutowireComposer
		implements CommonOnSearchdefault, DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Combobox searchstatus;
	private Checkbox searchbukanDosen;
//	private AmbilDataSatuanKerjaBanbox searchparent;
	private Checkbox searchaktif;

	private boolean edit = false;
	private boolean delete = false;

//	private SatuanKerja satuanKerjaOnSession;

//	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private MyToolbarbuttonConfig add;

	protected Tab belumDinilai;
	protected Tab telahDinilai;

	protected MyGrid gridBelumDinilai;
	protected MyGrid gridTelahDinilai;

	private Textbox cari_asesi;
	private Textbox cari_asesi_telah;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onLoadData(Event event) {

		Session session = HibernateUtil.currentSession();

		Pegawai peg = tbmuser.ambilPegawai();
		AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen = (AsesorPenunjangKinerjaDosen) (searchAsesorPenunjangKinerjaDosen
				.getSelectedItem() == null ? null : searchAsesorPenunjangKinerjaDosen.getSelectedItem().getValue());

		List<Long> asessi = asesorPenunjangKinerjaDosen == null || tbmuser == null ? new ArrayList<Long>()
				: session.createCriteria(AsesorPegawai.class).createAlias("asesor", "asesor")
						.add(Restrictions.or(Restrictions.isNull("asesor.aktif"),
								Restrictions.eq("asesor.aktif", true)))
						.setProjection(Projections.groupProperty("pegawai.id"))
						.add(Restrictions.eq("asesor.asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))
						.add(Restrictions.eq("asesor.tbmuser", tbmuser)).list();

		if (peg != null) {
			asessi.add(peg.getId());
		}

		System.out.println("asessi => " + asessi);

		ais.ui.util.MyRowRenderer rowRenderer = new ais.ui.util.MyRowRenderer() {

			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				final PenilaianAsesor penilaianAsesor = (PenilaianAsesor) arg1;

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						PenilaianAsesorAction.kasihPenilaian(penilaianAsesor, new DataSearchDefault() {

							@Override
							public void onSearchDefault(Event event) {
								onLoadData(event);
							}
						});

					}
				};

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(penilaianAsesor.getAsesemenPenilaian().getPegawai().getDosen() != null
						? penilaianAsesor.getAsesemenPenilaian().getPegawai().getDosen()
						: penilaianAsesor.getAsesemenPenilaian().getPegawai()).setParent(vbox);
				A a;
				(a = new A(penilaianAsesor.getAsesemenPenilaian().getPegawai().getNama())).setParent(vbox);
				a.addEventListener("onClick", eventListener);

				vbox = new Vbox();
				vbox.setParent(arg0);
				A aa = new A(penilaianAsesor.getAsesemenPenilaian().getBidang() + " - "
						+ penilaianAsesor.getAsesemenPenilaian().getKeterangan());
				aa.setParent(vbox);
				aa.addEventListener("onClick", eventListener);

				AsesemenPenilaian asesemenPenilaian = penilaianAsesor.getAsesemenPenilaian();
				vbox.appendChild(
						new Label("Beban: " + Common.numberFormat.get().format(asesemenPenilaian.getSks()) + " sks"));
				vbox.appendChild(
						new Label("Kinerja: " + Common.numberFormat.get().format(penilaianAsesor.getSks()) + " sks"));
				vbox.appendChild(new Label("Catatan: " + penilaianAsesor.getKeterangan()));

			}
		};

		if (belumDinilai.isSelected()) {

			List d = initCriteria(false, false, false).add(Restrictions.le("sks", 0.1)).addOrder(Order.asc("id"))

					.add(cari_asesi.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									Restrictions.ilike("pegawai.nama", cari_asesi.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("pegawai.code", cari_asesi.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("pegawai.mycode", cari_asesi.getValue().trim(),
													MatchMode.ANYWHERE)))

					)

					.setMaxResults(100).list();
			ListModel strset = new SimpleListModel(d);
			gridBelumDinilai.setRowRenderer(rowRenderer);
			gridBelumDinilai.setModelCheckMobile(strset);
		} else {
			List d = initCriteria(false, false, false).add(Restrictions.gt("sks", 0.1)).addOrder(Order.asc("id"))

					.add(cari_asesi_telah.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									Restrictions.ilike("pegawai.nama", cari_asesi_telah.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("pegawai.code", cari_asesi_telah.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.ilike("pegawai.mycode", cari_asesi_telah.getValue().trim(),
													MatchMode.ANYWHERE)))

					)

					.setMaxResults(100).list();
			ListModel strset = new SimpleListModel(d);
			gridTelahDinilai.setRowRenderer(rowRenderer);
			gridTelahDinilai.setModelCheckMobile(strset);
		}
	}

	// baru
	protected Combobox searchAsesorPenunjangKinerjaDosen;
	private MyCheckboxConfig terdapatSksBeban;
	private MyCheckboxConfig searchbelum;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;
	protected Combobox searchJenisPenilaian;
	private Tbmuser tbmuser;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings({})
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();

		PenilaianAsesorAction.initJenisPenilaianAsesor(searchJenisPenilaian);
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final String jenis = (String) (searchJenisPenilaian.getSelectedItem() == null ? null
						: searchJenisPenilaian.getSelectedItem().getValue());

				AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen = (AsesorPenunjangKinerjaDosen) (searchAsesorPenunjangKinerjaDosen
						.getSelectedItem() == null ? null
								: searchAsesorPenunjangKinerjaDosen.getSelectedItem().getValue());

				if (asesorPenunjangKinerjaDosen == null) {
					MyMessageboxConfig.show("Asesor harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Tbmuser tbmuser = Common.getCurrentUser();
				PenilaianAsesorAction.prosesUlang(PegawaiAction.this, tbmuser,
						(String) searchTahunAjaran.getSelectedItem().getValue(),
						(String) searchJenisSemester.getSelectedItem().getValue(), jenis, asesorPenunjangKinerjaDosen);

			}
		});

		Common.insertComboDanSemua(searchstatus, "nama", StatusPegawai.class, Restrictions.eq("aktif", true));

		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }
		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }

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

		Common.generateTahunAjaran(searchTahunAjaran);

//		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
//
//		if (session.getAttribute("satuanKerjaOnSession") != null) {
//			satuanKerjaOnSession = (SatuanKerja) session.getAttribute("satuanKerjaOnSession");
//			session.removeAttribute("satuanKerjaOnSession");
//		}
//
//		if (satuanKerjaOnSession != null) {
//			searchparent.setAttribute("satuanKerja", satuanKerjaOnSession);
//			searchparent.setValue(satuanKerjaOnSession.toString());
//			searchparent.setDisabled(true);
//		}
//
//		if (execution.getParameter("satuan_kerja") != null) {
//			satuanKerjaOnSession = (SatuanKerja) HibernateUtil.currentSession().createCriteria(SatuanKerja.class)
//					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("satuan_kerja")))).uniqueResult();
//		}
//
//		searchparent.setEventListener(new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				onSearchDefault(arg0);
//			}
//		});

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

//		Session session = HibernateUtil.currentNativeSession();
//		List<AsesorPenunjangKinerjaDosen> asesorPenunjangKinerjaDosens = session
//				.createCriteria(AsesorPenunjangKinerjaDosen.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//				.addOrder(Order.asc("nama")).list();
//		final TreeMap<String, AsesorPenunjangKinerjaDosen> treeMap = new TreeMap<String, AsesorPenunjangKinerjaDosen>();
//		for (AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen : asesorPenunjangKinerjaDosens) {
//			int asesorCount = ((Number) session.createCriteria(Asesor.class)
//					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//					.setProjection(Projections.rowCount())
//					.add(Restrictions.eq("asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))
//					.add(Restrictions.eq("tbmuser", tbmuser)).setMaxResults(1).uniqueResult()).intValue();
//			if (asesorCount > 0) {
//				treeMap.put(asesorPenunjangKinerjaDosen.getNama(), asesorPenunjangKinerjaDosen);
//			}
//		}
//
//		if (treeMap.isEmpty()) {
		Common.insertComboDanSemua(searchAsesorPenunjangKinerjaDosen, "nama", AsesorPenunjangKinerjaDosen.class,
				Restrictions.eq("aktif", true));
//		} else {
//			Common.insertComboItems(searchAsesorPenunjangKinerjaDosen, "nama", new ArrayList(treeMap.values()));
//			searchAsesorPenunjangKinerjaDosen.setReadonly(true);
//			searchAsesorPenunjangKinerjaDosen.setSelectedIndex(0);
//		}

		MyToolbarbuttonConfig deleteData = new MyToolbarbuttonConfig("Reset semua kinerja", "/img/svg/trash.svg");
		if (deleteData != null) { deleteData.setVisible(edit && delete); }
		Common.appendKeToolbar(deleteData, add, comp);
		deleteData.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show("Apakah yakin me-reset kembali data semua kinerja ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										String ta = (String) searchTahunAjaran.getSelectedItem().getValue();
										String smt = (String) searchJenisSemester.getSelectedItem().getValue();
										Pegawai myDosen = tbmuser.ambilPegawai();
										String sql = "delete from penilaian_asesor where pilih = false and sks_kinerja<0.1 and asesemen_penilaian in (select id from asesemen_penilaian where tahunakademik='"
												+ ta + "'  and semester='" + smt + "' "
												+ (myDosen == null ? "" : " and pegawai=" + myDosen.getId()) + ")";
										System.out.println("sql -> " + sql);
										HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();

										sql = "delete from asesemen_penilaian where tahunakademik='" + ta
												+ "' and semester='" + smt + "' "
												+ (myDosen == null ? "" : " and pegawai=" + myDosen.getId())
												+ " and id not in (select asesemen_penilaian from penilaian_asesor group by asesemen_penilaian)";
										System.out.println("sql -> " + sql);
										HibernateUtil.currentSession().createSQLQuery(sql).executeUpdate();
										onSearchDefault(event);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(
												"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}

								}

							}
						});
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

	class PegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pegawai pegawai = (Pegawai) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen = (AsesorPenunjangKinerjaDosen) (searchAsesorPenunjangKinerjaDosen
								.getSelectedItem() == null ? null
										: searchAsesorPenunjangKinerjaDosen.getSelectedItem().getValue());

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("5000px");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						final MyTabConfig tabSoal = new MyTabConfig("Asesemen Beban Kinerja Dosen");
						tabSoal.setParent(tabs);

						final MyTabConfig tabSoal1 = new MyTabConfig("Pendataan Beban Kinerja Dosen");
						tabSoal1.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setStyle("min-height: 200px;");
						tabpanelUtama.setHeight("1000px");
						tabpanelUtama.setParent(tabpanels);

						MyInclude include = new MyInclude(
								"/pages/master/bkd/asesor_memberikan_penilaian_rinci.zul?pegawai=" + pegawai.getId()
										+ "&ta="
										+ URLEncoder.encode(searchTahunAjaran.getSelectedItem().getValue().toString(),
												"UTF-8")
										+ "&smt=" + searchJenisSemester.getSelectedItem().getValue()
										+ (asesorPenunjangKinerjaDosen == null ? ""
												: "&asesor=" + asesorPenunjangKinerjaDosen.getId()));
						include.setHeight("3000px");
						include.setWidth("100%");
						tabpanelUtama.appendChild(include);

						final Tabpanel tabpanelPertemuan = new ais.ui.util.MyTabpanel();
						tabpanelPertemuan.setStyle("min-height: 200px;");
						tabpanelPertemuan.setHeight("1000px");
						tabpanelPertemuan.setParent(tabpanels);

						tabSoal1.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelPertemuan.getChildren().isEmpty()) {
									MyInclude include = new MyInclude("/pages/master/bkd/kinerja.zul?pegawai="
											+ pegawai.getId() + "&ta="
											+ URLEncoder.encode(
													searchTahunAjaran.getSelectedItem().getValue().toString(), "UTF-8")
											+ "&smt=" + searchJenisSemester.getSelectedItem().getValue());
									include.setHeight("1000px");
									include.setWidth("100%");
									tabpanelPertemuan.appendChild(include);
								}
							}
						});
					}
				}
			});

			CommonMedia.tampilkanGambarKecil(pegawai).setParent(arg0);

			new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(arg0);

			Vbox vbox = RevisiHelper.createNewRevisi(Pegawai.class, pegawai,
					pegawai.getDosen() == null ? pegawai.getNama() : pegawai.getDosen().getNama());
			vbox.setParent(arg0);

			String socialMediaProfile = (String) HibernateUtil.currentSession().createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("socialMediaProfile")).add(Restrictions.eq("pegawai", pegawai))
					.setMaxResults(1).uniqueResult();
			if (socialMediaProfile != null) {
				for (String profile : StringUtils.split(socialMediaProfile, ";")) {
					String[] data = StringUtils.split(profile, "||");
					try {
						String property = StringUtils.split(data[0], ":")[0];
						String linkProfile = data[1];
						A link = new A("Profil " + property.replaceAll("Id", ""));
						link.setHref(linkProfile);
						link.setTarget("_blank");
						vbox.appendChild(link);
					} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				}

			}

			Session session = HibernateUtil.currentSession();
			Number b = (Number) session.createCriteria(PenilaianAsesor.class)
					.createAlias("asesemenPenilaian", "asesemenPenilaian")
					.add(Restrictions.eq("asesemenPenilaian.pegawai", pegawai))
					.setProjection(Projections.sum("asesemenPenilaian.sks"))

					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("asesemenPenilaian.tahunAkademik",
											searchTahunAjaran.getSelectedItem().getValue()))

					.add(searchJenisSemester.getSelectedItem() == null
							|| searchJenisSemester.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("asesemenPenilaian.semester",
											searchJenisSemester.getSelectedItem().getValue()))
					.uniqueResult();

			Number r = (Number) session.createCriteria(PenilaianAsesor.class)
					.createAlias("asesemenPenilaian", "asesemenPenilaian")
					.add(Restrictions.eq("asesemenPenilaian.pegawai", pegawai)).setProjection(Projections.sum("sks"))

					.add(searchTahunAjaran.getSelectedItem() == null
							|| searchTahunAjaran.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("asesemenPenilaian.tahunAkademik",
											searchTahunAjaran.getSelectedItem().getValue()))

					.add(searchJenisSemester.getSelectedItem() == null
							|| searchJenisSemester.getSelectedItem().getValue() == null
									? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("asesemenPenilaian.semester",
											searchJenisSemester.getSelectedItem().getValue()))
					.uniqueResult();

			new Label("Total SKS Beban " + Common.numberFormat.get().format(b == null ? 0 : b.intValue())
					+ " sks dan Realisasi " + Common.numberFormat.get().format(r == null ? 0 : r.intValue()) + " sks atau "
					+ Common.numberFormat.get().format(

							(b == null ? 0 : b.intValue()) == 0 ? 0
									: (((r == null ? 0 : r.intValue()) * 100.0) / (b == null ? 0 : b.intValue()))

					) + "%").setParent(arg0);

			vbox = new Vbox();
			vbox.setHeight("100%");
			vbox.setWidth("100%");
			vbox.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(vbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Daftar Riwayat Hidup");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event event) throws Exception {

					Map parameters = ais.common.HashMapGenerator.getRand();
					Common.insertProperty(Pegawai.class, pegawai, parameters, "", 2);
					GajiPokok gajiPokok = pegawai.ambilGajiPokok(WaktuUtil.getDate());
					if(gajiPokok != null) {
						Common.insertProperty(GajiPokok.class, gajiPokok, parameters, "gp", 1);
					}
					parameters.put("id", pegawai.getId());
					pegawai.putPhoto(parameters); 
					Report.generatePDFReport(Report.PDF, parameters, "employ/daftar_riwayat_hidup",
							ais.ui.util.WaktuUtil.getDate());
				}

			});
			button.setParent(toolbar);

		}
	}

	public Criteria initCriteria(boolean order) {
		return initCriteria(order, true, searchbelum.isChecked());
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order, boolean projection, boolean searchbelum) {

//		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
//		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
//		if (parent != null) {
//			satuanKerjas.clear(); satuanKerjas.add(parent);
//			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
//		}

		Session session = HibernateUtil.currentSession();
		Pegawai peg = tbmuser.ambilPegawai();
		AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen = (AsesorPenunjangKinerjaDosen) (searchAsesorPenunjangKinerjaDosen
				.getSelectedItem() == null ? null : searchAsesorPenunjangKinerjaDosen.getSelectedItem().getValue());

		List<Long> asessi = asesorPenunjangKinerjaDosen == null || tbmuser == null ? new ArrayList<Long>()
				: session.createCriteria(AsesorPegawai.class).createAlias("asesor", "asesor")
						.add(Restrictions.or(Restrictions.isNull("asesor.aktif"),
								Restrictions.eq("asesor.aktif", true)))
						.setProjection(Projections.groupProperty("pegawai.id"))
						.add(Restrictions.eq("asesor.asesorPenunjangKinerjaDosen", asesorPenunjangKinerjaDosen))
						.add(Restrictions.eq("asesor.tbmuser", tbmuser)).list();

		if (peg != null) {
			asessi.add(peg.getId());
		}

		System.out.println("asessi => " + asessi);

		Criteria criteria = session.createCriteria(PenilaianAsesor.class)
				.createAlias("asesemenPenilaian", "asesemenPenilaian")
				.add(terdapatSksBeban.isChecked() ? Restrictions.gt("asesemenPenilaian.sks", 0.1)
						: Restrictions.sqlRestriction("true"))
				.createAlias("asesor", "asesor");

		if (projection) {
			criteria.setProjection(Projections.groupProperty("asesemenPenilaian.pegawai"));
		}

		criteria.createAlias("asesemenPenilaian.pegawai", "pegawai")

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pegawai.statusPegawai", searchstatus.getSelectedItem().getValue()))

				.add((asessi == null || asessi.isEmpty() || Common.getApakahAdmin()
						? Restrictions.sqlRestriction("true")
						: Restrictions.in("pegawai.id", asessi)))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("asesemenPenilaian.tahunAkademik",
										searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("asesemenPenilaian.semester",
										searchJenisSemester.getSelectedItem().getValue()))

				.add(searchbelum ? Restrictions.le("sks", 0.1) : Restrictions.sqlRestriction("true"))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("pegawai.aktif"), Restrictions.eq("pegawai.aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchbukanDosen.isChecked() ? Restrictions.isNull("pegawai.dosen")
						: Restrictions.sqlRestriction("true"))

//				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
//						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))
//				.add(satuanKerjaOnSession == null ? Restrictions.sqlRestriction("1=1")
//						: Restrictions.eq("pegawai.satuanKerja", satuanKerjaOnSession))

				.add(searchJenisPenilaian.getSelectedItem() == null
						|| searchJenisPenilaian.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("asesemenPenilaian.spesifikasi",
										searchJenisPenilaian.getSelectedItem().getValue()));

		if (projection) {
			criteria.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
					: Restrictions.ilike("pegawai.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
					.add(searchcode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									Restrictions.ilike("pegawai.code", searchcode.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("pegawai.mycode", searchcode.getValue().trim(),
											MatchMode.ANYWHERE)));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging, null, Projections.countDistinct("asesemenPenilaian.pegawai"));

		List<Pegawai> pegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pegawai);
		grid.setRowRenderer(new PegawaiRenderer());
		grid.setModelCheckMobile(strset);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLoadData(arg0);
			}
		});
	}

}
