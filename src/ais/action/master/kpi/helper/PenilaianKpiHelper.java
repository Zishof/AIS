package ais.action.master.kpi.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Space;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataPegawaiBanyak;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.PengajuanKpi;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class PenilaianKpiHelper {

	private PengajuanKpi pengajuanKpi;
	private Paging paging;
	private MyGrid grid;
	private boolean persetujuan = false;

	public PenilaianKpiHelper(MyGrid grid, boolean persetujuan) {
		super();
		this.grid = grid;
		this.persetujuan = persetujuan;
	}

	private Date sekarang = WaktuUtil.getDate();
	@SuppressWarnings("rawtypes")
	Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();
	public void initRow(final Row row, final PenilaianKpi penilaianKpi) throws Exception {
		final Pegawai pegawai = penilaianKpi.getPegawai();
		row.setValign("top");
		row.setAttribute("penilaianKpi", penilaianKpi);

		CommonMedia.tampilkanGambarKecil(pegawai).setParent(row);
		Vbox a;
		(a = RevisiHelper.createNewRevisi(PenilaianKpi.class, penilaianKpi, pegawai.getNama())).setParent(row);

		new Label(pegawai.getMycode() == null ? "" : pegawai.getMycode()).setParent(a);

		List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkat(sekarang,pangkats);
		JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
		JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
		Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

		new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">"
				+ (jabatanFungsional == null ? "" : jabatanFungsional.getNama() + "<br>")
				+ (jabatanStruktural == null ? "" : jabatanStruktural.getNama() + "<br>")
				+ (jabatan == null ? "" : jabatan.getNama()) + "</font>").setParent(row);
		kenaikanPangkats = null;

		Vbox vbox = new Vbox();
		vbox.setParent(row);

		if (pegawai.getTanggalMulaiPengalanKerja() != null) {
			new Label("Pengalaman Kerja : " + pegawai.ambilMasaKerjaTahunPengalamanKerja() + " thn, "
					+ pegawai.ambilMasaKerjaBulanPengalamanKerja() + " bln").setParent(vbox);
		}
		if (pegawai.getTanggalmasukHonorer() != null) {
			new Label("Honor : " + pegawai.ambilMasaKerjaTahunHonorer() + " thn, "
					+ pegawai.ambilMasaKerjaBulanHonorer() + " bln").setParent(vbox);
		}
		if (pegawai.getTanggalmasukSemiTetap() != null) {
			new Label("Semi Tetap : " + pegawai.ambilMasaKerjaTahunSemiTetap() + " thn, "
					+ pegawai.ambilMasaKerjaBulanSemiTetap() + " bln").setParent(vbox);
		}
		if (pegawai.getTanggalmasuk() != null) {
			new Label("Tetap : " + pegawai.ambilMasaKerjaTahun() + " thn, " + pegawai.ambilMasaKerjaBulan() + " bln")
					.setParent(vbox);
		}

		new Label(Common.numberFormat.get().format(penilaianKpi.getNilai())).setParent(row);
		new Label(Common.numberFormat.get().format(penilaianKpi.getPersen()) + "%").setParent(row);

		final MyTextbox keterangan = new MyTextbox(
				penilaianKpi.getKeterangan() == null ? "" : penilaianKpi.getKeterangan());
		keterangan.setWidth("90%");
		if (persetujuan) {
			new MyLabelKecil(penilaianKpi.getKeterangan()).setParent(row);
		} else {
			keterangan.setParent(row);
		}
		keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				penilaianKpi.setKeterangan(keterangan.getValue());

				if (pengajuanKpi.getId() != null) {
					Common.refreshSaveOrUpdate(penilaianKpi);
				}
				row.setValign("top");
				row.setAttribute("penilaianKpi", penilaianKpi);
			}
		});

		Hbox toolbar = new Hbox();
		row.appendChild(toolbar);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat", "/img/svg/eye.svg");
		button.setVisible(!persetujuan);
		button.setTooltiptext("Hapus Data");
		button.setOrient("vertical");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				FormatKpiDetail formatKpiDetail = pegawai.ambilFormatKpiDetail(sekarang);
				if (formatKpiDetail == null) {
					MyMessageboxConfig.show("Mohon maaf, Format KPI untuk pegawai ini belum ditentukan. Langkah yang dapat dilakukan: (1) tentukan Format KPI pegawai terlebih dahulu melalui menu Format KPI; (2) ulangi proses penilaian.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Common.displayWindow("/pages/master/kpi/nilai_kpi.zul?id=" + penilaianKpi.getId(), true, "95%", "95%",
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						}, "", false);

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setOrient("vertical");
		button.setTooltiptext("Hapus Data");
		button.setVisible(pengajuanKpi.getDisetujuiOleh() == null && !persetujuan);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										penilaianKpisData.remove(penilaianKpi);

										Session session = HibernateUtil.currentSession();

										if (pengajuanKpi.getId() != null) {
											String p = pengajuanKpi.getPenilaianKpis();
											p += p.isEmpty() ? penilaianKpi.getId() + "" : "," + penilaianKpi.getId();
											pengajuanKpi.setPenilaianKpis(p);

											Common.refreshUpdate(session, pengajuanKpi);

											penilaianKpi.setPengajuanKpi(null);
											Common.refreshUpdate(session, penilaianKpi);
										}

										session.flush();

										loadData(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(MyMessageboxConfig.format(
												"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan data terkait terlebih dahulu; (2) ulangi penghapusan; (3) apabila masih gagal, mohon hubungi administrator sistem.",
												e.getMessage()));
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

	}

	private MyTextbox kode;
	private MyTextbox nama;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("pegawai.mycode", kode.getValue().trim(), MatchMode.ANYWHERE));
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("pegawai.code", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("pegawai.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenilaianKpi.class).createAlias("pegawai", "pegawai").add(critKode)
				.add(critNama)

				.add(pengajuanKpi == null || pengajuanKpi.getId() == null ? Restrictions.sqlRestriction("false")
						: Restrictions.eq("pengajuanKpi", pengajuanKpi));
		if (order)
			criteria.addOrder(Order.asc("pegawai.nama"));

		return criteria;
	}

	private List<PenilaianKpi> penilaianKpisData = new ArrayList<PenilaianKpi>();

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenilaianKpi> penilaianKpis = pengajuanKpi == null || pengajuanKpi.getId() == null ? penilaianKpisData
				: initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();

		Rows rows = grid.getRows() == null ? new Rows() : grid.getRows();
		rows.setParent(grid);
		Common.clear(rows);
		for (PenilaianKpi penilaianKpi : penilaianKpis) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			try {
				initRow(row, penilaianKpi);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/kpi/helper/PenilaianKpiHelper.java:284");
			}
		}

	}

	public Groupbox display(final PengajuanKpi pengajuanKpi, final Combobox ta,
			final AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox) {
		this.pengajuanKpi = pengajuanKpi;
		boolean persetujuan = pengajuanKpi.getDisetujuiOleh() != null;
		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();

		Tbmuser tbmuser = Common.getCurrentUser();
		Pegawai currentPegawai = tbmuser == null ? null : tbmuser.ambilPegawai();

		groupbox.appendChild(new MyCaptionStyled("Daftar Pegawai"));

		Toolbar toolbar = new Toolbar();
		ais.ui.util.MenuAksiBaris.pasang(toolbar);
		toolbar.setParent(groupbox);
		if (!persetujuan && currentPegawai == null) {
			Toolbarbutton button = new MyToolbarbuttonConfig("Ambil Data Pegawai", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja") == null) {
						MyMessageboxConfig.show("Mohon maaf, satuan kerja belum dipilih. Langkah yang dapat dilakukan: (1) pilih satuan kerja terlebih dahulu; (2) ulangi pengambilan data pegawai.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}
					if (ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, tahun akademik belum dipilih. Langkah yang dapat dilakukan: (1) pilih tahun akademik terlebih dahulu; (2) ulangi pengambilan data pegawai.", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<Pegawai> pegawais = ConstantValues.simpleList(session.createCriteria(PenilaianKpi.class)
							.setProjection(Projections.groupProperty("pegawai.id"))
							.add(Restrictions.eq("ta", ta.getSelectedItem().getValue()))
							.add(Restrictions.isNotNull("pengajuanKpi")), Pegawai.class, false);

					AmbilDataPegawaiBanyak ambilDataPegawaiBanyak = new AmbilDataPegawaiBanyak(pegawais,
							(SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja"));
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataPegawaiBanyak);
					ambilDataPegawaiBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(final Event arg0) throws Exception {

							List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();
							for (Pegawai pegawai : pegawais) {
								FormatKpiDetail formatKpiDetail = pegawai.ambilFormatKpiDetail(sekarang);
								if (formatKpiDetail == null) {
									MyMessageboxConfig.showFormat(
											"Mohon maaf, Format KPI untuk pegawai \"{V1}\" belum ditentukan. Langkah yang dapat dilakukan: (1) tentukan Format KPI pegawai tersebut terlebih dahulu melalui menu Format KPI; (2) ulangi pengambilan data pegawai.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, pegawai.getNama());
									return;
								}
							}

							final Label label = Common.displayLoadBar(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});

							new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									List<Pegawai> pegawais = (List<Pegawai>) arg0.getData();
									int size = pegawais.size();
									int index = 0;
									String p = pengajuanKpi.getPenilaianKpis();
									for (Pegawai pegawai : pegawais) {

										index++;
										label.setValue("Memproses data rencana gaji " + pegawai.getNama() + " ("
												+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

										Session session = HibernateUtil.currentNativeSession();
										PenilaianKpi penilaianKpi = (PenilaianKpi) ConstantValues.simpleObject(
												session.createCriteria(PenilaianKpi.class).addOrder(Order.asc("id"))
														.add(Restrictions.eq("ta", ta.getSelectedItem().getValue()))
														.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1),
												PenilaianKpi.class);

										if (penilaianKpi == null) {
											penilaianKpi = new PenilaianKpi();
											penilaianKpi.setPegawai(pegawai);
											penilaianKpi.setKeterangan("");
											penilaianKpi.setTa(pengajuanKpi.getTa());

											if (pengajuanKpi != null && pengajuanKpi.getId() != null) {
												penilaianKpi.setPengajuanKpi(pengajuanKpi);
											}

											session.getTransaction().begin();
											session.save(penilaianKpi);
											session.getTransaction().commit();
										} else {
											if (pengajuanKpi != null && pengajuanKpi.getId() != null) {
												penilaianKpi.setPengajuanKpi(pengajuanKpi);
												session.getTransaction().begin();
												Common.refreshUpdate(session, penilaianKpi);
												session.getTransaction().commit();
											}
										}

										p += p.isEmpty() ? penilaianKpi.getId() + "" : "," + penilaianKpi.getId();

										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
										HibernateUtil.closeSession();

										penilaianKpisData.add(penilaianKpi);

									}

									pengajuanKpi.setPenilaianKpis(p);

									if (pengajuanKpi != null && pengajuanKpi.getId() != null) {
										Session session = HibernateUtil.currentNativeSession();
										session.getTransaction().begin();
										Common.refreshUpdate(session, pengajuanKpi);
										session.getTransaction().commit();
										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
										HibernateUtil.closeSession();
									}

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					});
					ambilDataPegawaiBanyak.setWidth("90%");
					ambilDataPegawaiBanyak.setHeight("97%");
					ambilDataPegawaiBanyak.setVisible(true);
					ambilDataPegawaiBanyak.onModal();
				}

			});
			button.setParent(toolbar);
		}

		if (currentPegawai != null && (pengajuanKpi == null || pengajuanKpi.getId() == null)) {

			Session session = HibernateUtil.currentNativeSession();
			PenilaianKpi penilaianKpi = (PenilaianKpi) ConstantValues
					.simpleObject(
							session.createCriteria(PenilaianKpi.class).addOrder(Order.asc("id"))
									.add(Restrictions.eq("ta", ta.getSelectedItem().getValue()))
									.add(Restrictions.eq("pegawai", currentPegawai)).setMaxResults(1),
							PenilaianKpi.class);

			if (penilaianKpi == null) {
				penilaianKpi = new PenilaianKpi();
				penilaianKpi.setPegawai(currentPegawai);
				penilaianKpi.setKeterangan("");
				penilaianKpi.setTa(pengajuanKpi.getTa());

				if (pengajuanKpi != null && pengajuanKpi.getId() != null) {
					penilaianKpi.setPengajuanKpi(pengajuanKpi);
				}

				session.getTransaction().begin();
				session.save(penilaianKpi);
				session.getTransaction().commit();
			} else {
				if (pengajuanKpi != null && pengajuanKpi.getId() != null) {
					penilaianKpi.setPengajuanKpi(pengajuanKpi);
					session.getTransaction().begin();
					Common.refreshUpdate(session, penilaianKpi);
					session.getTransaction().commit();
				}
			}

			pengajuanKpi.setPenilaianKpis(penilaianKpi.getId() + "");

			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();

			penilaianKpisData.add(penilaianKpi);

		}

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new MyToolbarbuttonConfig("", "/img/svg/search.svg"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jabatan");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Masa Kerja");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Persen");
		column.setAlign("right");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Ket.");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("15%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);

		return groupbox;
	}

}
