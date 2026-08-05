package ais.action.master.obe;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AmbilDataKurikulumBanbox;
import ais.action.master.helper.generic.AmbilDataMatakuliahBanyak;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Matakuliah;
import ais.database.model.StatusMatakuliah;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MatakuliahVsKurikulumVsSemesterAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	protected AmbilDataKurikulumBanbox searchkurikulum;
	private Center center;

	public MatakuliahVsKurikulumVsSemesterAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public MatakuliahVsKurikulumVsSemesterAction(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	private Textbox nama;

	private void init() throws Exception {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North west = new North();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setHeight("40px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum" + " *"));
		row.appendChild(searchkurikulum = new AmbilDataKurikulumBanbox());
		searchkurikulum.setWidth("90%");
		searchkurikulum.setReadonly(true);
		searchkurikulum.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyButtonConfig button = new MyButtonConfig("Refresh");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);
			}
		});

	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@SuppressWarnings({ "deprecation", "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Common.clear(center);
					Kurikulum kurikulum = (Kurikulum) searchkurikulum.getAttribute("kurikulum");
					if (kurikulum == null) {
						MyMessageboxConfig.show("Pilih " + "Kurikulum", "Peringatan", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION);
						return;
					}

					Session session = HibernateUtil.currentSession();
					List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues
							.simpleList(
									session.createCriteria(KurikulumPunyaMatakuliah.class)
											.createAlias("matakuliah", "matakuliah")
											.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
													: Restrictions.or(
															Restrictions.ilike("matakuliah.kode",
																	nama.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("matakuliah.nama",
																	nama.getValue().trim(), MatchMode.ANYWHERE)))

											.add(Restrictions.eq("kurikulum", kurikulum))
											.addOrder(Order.asc("matakuliah.kode"))
											.addOrder(Order.asc("matakuliah.nama"))
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.or(Restrictions.isNull("matakuliah.aktif"),
													Restrictions.eq("matakuliah.aktif", true))),
									KurikulumPunyaMatakuliah.class);

					List<StatusMatakuliah> statusMatakuliahs = ConstantValues.simpleList(
							session.createCriteria(StatusMatakuliah.class).addOrder(Order.asc("nama")),
							StatusMatakuliah.class);

					MyGrid grid = new MyGrid();
					ais.ui.util.ZkCompat.setFixedLayout(grid, false);
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("Semester");
					column.setParent(columns);
					column.setWidth("70px");

					column = new MyColumnConfig("SKS");
					column.setParent(columns);
					column.setWidth("70px");

					for (StatusMatakuliah statusMatakuliah : statusMatakuliahs) {
						column = new MyColumnConfig(statusMatakuliah.getNama());
						column.setParent(columns);
						column.setWidth("250px");
					}

					column = new MyColumnConfig("Ambil MK");
					column.setParent(columns);
					column.setWidth("200px");

					Rows rows = new Rows();
					rows.setParent(grid);

					for (int smt = 1; smt <= kurikulum.getJurusan().getJenjang().getJumlahSemester(); smt++) {

						int jmlSks = 0;
						for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
							if (kurikulumPunyaMatakuliah != null && kurikulumPunyaMatakuliah.getMatakuliah() != null
									&& kurikulumPunyaMatakuliah.getSemester() != null
									&& kurikulumPunyaMatakuliah.getSemester().equals(smt)) {
								jmlSks += kurikulumPunyaMatakuliah.getMatakuliah().getSks();
							}
						}

						MyFormRow row = new MyFormRow();
						row.setValign("top");

						row.setParent(rows);
						row.appendChild(new Label(smt + ""));
						row.appendChild(new Label(jmlSks + ""));

						for (StatusMatakuliah statusMatakuliah : statusMatakuliahs) {

							MyGrid gridData = new MyGrid();
							ais.ui.util.ZkCompat.setFixedLayout(gridData, true);
							gridData.setWidth("100%");
							gridData.setParent(row);
							gridData.setHeight("100%");

							Columns columnsData = new Columns();
							columnsData.setParent(gridData);

							MyColumnConfig columnData = new MyColumnConfig("Kode");
							columnData.setParent(columnsData);
							columnData.setWidth("30%");

							columnData = new MyColumnConfig("Nama");
							columnData.setParent(columnsData);

							columnData = new MyColumnConfig("Hapus");
							columnData.setParent(columnsData);
							columnData.setWidth("15%");

							Rows rowsData = new Rows();
							rowsData.setParent(gridData);

							for (final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
								Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();
								if (matakuliah != null && matakuliah.getStatus() != null
										&& matakuliah.getStatus().equalsIgnoreCase(statusMatakuliah.getNama())
										&& kurikulumPunyaMatakuliah.getSemester() != null
										&& kurikulumPunyaMatakuliah.getSemester().equals(smt)) {

									MyFormRow rowData = new MyFormRow();
									rowData.setParent(rowsData);
									rowData.appendChild(new Label(matakuliah.getKode()));
									rowData.appendChild(ObeBaseAction.ringkasanKeterangan(matakuliah.getNama()));

									MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
									button.setTooltiptext("Hapus Data");
									button.addEventListener("onClick", new EventListener() {
										@Override
										public void onEvent(Event event) throws Exception {
											MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
													"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
													MyMessageboxConfig.QUESTION, new EventListener() {

														@Override
														public void onEvent(Event event) throws Exception {
															int i = Integer.parseInt(event.getData().toString());
															if (i == MyMessageboxConfig.OK) {
																try {
																	Common.refreshDelete(kurikulumPunyaMatakuliah);
																	onKHS(event);
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
									button.setParent(rowData);
								}

							}
						}

						final int smtData = smt;
						MyButtonConfig button = new MyButtonConfig("Ambil MK Baru");
						button.setParent(row);
						button.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								final Kurikulum kurikulum = (Kurikulum) searchkurikulum.getAttribute("kurikulum");
								if (kurikulum == null) {
									MyMessageboxConfig.show("Pilih " + "Kurikulum", "Peringatan", MyMessageboxConfig.OK,
											MyMessageboxConfig.INFORMATION);
									return;
								}

								List<Matakuliah> matakuliahsData = ConstantValues.simpleList(
										HibernateUtil.currentSession().createCriteria(KurikulumPunyaMatakuliah.class)
												.setProjection(Projections.groupProperty("matakuliah.id"))
												.add(Restrictions.eq("kurikulum", kurikulum)),
										Matakuliah.class, false);

								AmbilDataMatakuliahBanyak ambilDataMatakuliahBanyak = new AmbilDataMatakuliahBanyak(
										matakuliahsData);
								ambilDataMatakuliahBanyak
										.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
								ambilDataMatakuliahBanyak.setHeight("95%");
								ambilDataMatakuliahBanyak.setWidth("700px");

								ambilDataMatakuliahBanyak.setEventListener(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Session session = HibernateUtil.currentSession();
										List<Matakuliah> matakuliahes = (List<Matakuliah>) arg0.getData();
										for (Matakuliah matakuliah : matakuliahes) {
											int c = ((Number) session.createCriteria(KurikulumPunyaMatakuliah.class)
													.add(Restrictions.eq("matakuliah", matakuliah))
													.add(Restrictions.eq("kurikulum", kurikulum))
													.setProjection(Projections.rowCount()).uniqueResult()).intValue();
											if (c == 0) {
												KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
												kurikulumPunyaMatakuliah.setAktif(true);
												kurikulumPunyaMatakuliah.setMatakuliah(matakuliah);
												kurikulumPunyaMatakuliah.setKurikulum(kurikulum);
												kurikulumPunyaMatakuliah.setSemester(smtData);
												session.save(kurikulumPunyaMatakuliah);
												session.flush();
											}
										}
										onKHS(arg0);
									}
								});

								ambilDataMatakuliahBanyak.onModal();
							}
						});

					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

	}

}
