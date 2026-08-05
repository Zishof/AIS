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
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
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
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MatakuliahVsKurikulumAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	protected AmbilDataKurikulumBanbox searchkurikulum;
	private Center center;

	public MatakuliahVsKurikulumAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public MatakuliahVsKurikulumAction(String title, String border, boolean closable) throws Exception {
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

		MyFormRow row = new MyFormRow();row.setValign("top");
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

		button = new MyButtonConfig("Ambil Matakuliah Baru");
		button.setParent(row);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				final Kurikulum kurikulum = (Kurikulum) searchkurikulum.getAttribute("kurikulum");
				if (kurikulum == null) {
					MyMessageboxConfig.show("Pilih " + "Kurikulum", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				List<Matakuliah> matakuliahsData = ConstantValues
						.simpleList(HibernateUtil.currentSession().createCriteria(KurikulumPunyaMatakuliah.class)
								.setProjection(Projections.groupProperty("matakuliah.id"))
								.add(Restrictions.eq("kurikulum", kurikulum)), Matakuliah.class, false);

				AmbilDataMatakuliahBanyak ambilDataMatakuliahBanyak = new AmbilDataMatakuliahBanyak(matakuliahsData);
				ambilDataMatakuliahBanyak.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
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
									.add(Restrictions.eq("kurikulum", kurikulum)).setProjection(Projections.rowCount())
									.uniqueResult()).intValue();
							if (c == 0) {
								KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();
								kurikulumPunyaMatakuliah.setAktif(true);
								kurikulumPunyaMatakuliah.setMatakuliah(matakuliah);
								kurikulumPunyaMatakuliah.setKurikulum(kurikulum);
								kurikulumPunyaMatakuliah.setSemester(1);
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

					List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues
							.simpleList(
									HibernateUtil.currentSession().createCriteria(KurikulumPunyaMatakuliah.class)
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

					MyGrid grid = new MyGrid();
					ais.ui.util.ZkCompat.setFixedLayout(grid, true);
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setHeight("100%");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig("Kode");
					column.setParent(columns);
					column.setWidth("10%");

					column = new MyColumnConfig("Matakuliah");
					column.setParent(columns);
					column.setWidth("20%");

					column = new MyColumnConfig("Semester");
					column.setParent(columns);

					column = new MyColumnConfig("Hapus");
					column.setParent(columns);
					column.setWidth("6%");

					Rows rows = new Rows();
					rows.setParent(grid);

					for (final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
						Matakuliah matakuliah = kurikulumPunyaMatakuliah.getMatakuliah();

						MyFormRow row = new MyFormRow();row.setValign("top");
						row.setParent(rows);
						row.appendChild(new Label(matakuliah.getKode()));
						row.appendChild(ObeBaseAction.ringkasanKeterangan(matakuliah.getNama()));

						Radiogroup radiogroup = new Radiogroup();
						radiogroup.setParent(row);

						for (int smt = 1; smt <= kurikulum.getJurusan().getJenjang().getJumlahSemester(); smt++) {
							final Radio checkbox = new Radio(smt + "");
							checkbox.setTooltiptext("Semester " + smt);
							checkbox.setChecked(kurikulumPunyaMatakuliah.getSemester() != null
									&& kurikulumPunyaMatakuliah.getSemester().equals(smt));
							radiogroup.appendChild(checkbox);
							final int s = smt;
							checkbox.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									kurikulumPunyaMatakuliah.setSemester(s);
									Common.refreshUpdate(kurikulumPunyaMatakuliah);
								}
							});
						}

						MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
						button.setTooltiptext("Hapus Data");
						button.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
										MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
										new EventListener() {

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
						button.setParent(row);
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

	}

}
