package ais.action.master.kpi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.kpi.helper.AmbilDataFormatKpiDetailBanbox;
import ais.action.master.kpi.helper.AmbilDataKpiBanyak;
import ais.action.master.kpi.helper.ItemKpiTreeModel;
import ais.action.master.kpi.helper.KpiUtil;
import ais.action.report.kpi.LaporanItemKpi;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.Kpi;
import ais.database.model.kpi.MasaKpi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class ItemKpiTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;

	private MyLabelConfig searchLabelFormatKpi;

	private AmbilDataFormatKpiDetailBanbox searchFormatKpi;
	private Tree tree;

	private Vbox kpi;
	private MyIntbox nomorUrut;
//	private AmbilDataItemKpiBanbox parent;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ItemKpi itemKpi;
	private boolean add = false;
	private ItemKpiTreeModel itemKpiTreeModel;

	private TreeMap<Long, Treecell> treecellMap = new TreeMap<Long, Treecell>();

	private MyToolbarbuttonConfig addNew;
	private MyToolbarbuttonConfig upload;
	private Date sekarang;
	private JSONArray array;
	private Row rowFormula;

	private List<Kpi> kpis = null;
//	private FormatKpi formatKpi;
	private FormatKpiDetail formatKpiDetail = null;

	private MyLabelBold totalTarget;
	private boolean adminLainBoleh;
	private MyToolbarbuttonConfig buttonHapus;
	private MyToolbarbuttonConfig cetakToolbarbutton;
//	private MyToolbarbuttonConfig upload;
	private MyCheckboxConfig tampilkanFormula;
	private Tbmuser tbmuser = null;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();
		adminLainBoleh = tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null && tbmuser.getSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null;

		sekarang = WaktuUtil.getDate();

		searchFormatKpi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				formatKpiDetail = (FormatKpiDetail) searchFormatKpi.getAttribute("formatKpiDetail");
				if (formatKpiDetail == null) {
					MyMessageboxConfig.show("Mohon maaf, Format KPI belum diisi. Langkah yang dapat dilakukan: (1) pilih Format KPI terlebih dahulu; (2) ulangi proses.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}

				onReloadTree(arg0, false);
			}
		});

		if (searchFormatKpi != null) { searchFormatKpi.setReadonly(true); }
		if (searchFormatKpi != null) { searchFormatKpi.setCols(8); }

		if (searchFormatKpi.getAttribute("formatKpi") != null) {
			searchFormatKpi.setVisible(false);
			searchLabelFormatKpi.setVisible(false);
		}

		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (addNew != null) { addNew.setVisible(add); }

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hitung Ulang", "/img/options.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						onReloadTree(arg0, true);
					}
				});

			}

		});
		Common.appendKeToolbar(button, addNew, comp);

		buttonHapus = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");

		if (buttonHapus != null) { buttonHapus.setTooltiptext("Hapus Data"); }
		if (buttonHapus != null) { buttonHapus.setVisible(delete); }
		buttonHapus.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus seluruh item KPI ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										formatKpiDetail = (FormatKpiDetail) searchFormatKpi
												.getAttribute("formatKpiDetail");
										if (formatKpiDetail == null) {
											MyMessageboxConfig.show("Mohon maaf, Format KPI belum diisi. Langkah yang dapat dilakukan: (1) pilih Format KPI terlebih dahulu; (2) ulangi penghapusan.", "Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.INFORMATION);
											return;
										}

										if (formatKpiDetail != null) {

											List<ItemKpi> itemKpis = HibernateUtil.currentSession()
													.createCriteria(ItemKpi.class)
													.add(Restrictions.eq("formatKpi", formatKpiDetail.getFormatKpi()))
													.list();
											for (ItemKpi itemKpi : itemKpis) {
												Common.refreshDelete(itemKpi);
											}

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													onReloadTree(arg0, false);
												}
											});
										}
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
		if (buttonHapus != null) { buttonHapus.setParent(addNew.getParent()); }

		String[] contents = new String[] { "id", "kode", "nama", "val", "formatKpi", "parent", "nomorUrut", "kpi",
				"formula", "target", "valtampil", "deep", "jmlDipakai", "keterangan", "aktif" };
		cetakToolbarbutton = Common.cetakData(ItemKpi.class, new DataCriteria() {

			@Override
			public Object initCriteria(boolean order) {
				formatKpiDetail = (FormatKpiDetail) searchFormatKpi.getAttribute("formatKpiDetail");

				return HibernateUtil.currentSession().createCriteria(ItemKpi.class)
						.add(Restrictions.eq("formatKpi",
								formatKpiDetail == null ? null : formatKpiDetail.getFormatKpi()))
						.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("id"));
			}
		}, contents);
		Common.appendKeToolbar(cetakToolbarbutton, addNew, comp);

		upload = Common.uploadData(new DataSearchDefault() {

			@Override
			public void onSearchDefault(Event event) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						onReloadTree(arg0, true);
					}
				});

			}
		}, ItemKpi.class, contents);
		if (upload != null) { upload.setVisible(Common.getApakahAdmin()); }
		Common.appendKeToolbar(upload, addNew, comp);

		tampilkanFormula = new MyCheckboxConfig("Formula");
		if (tampilkanFormula != null) { tampilkanFormula.setVisible(Common.getApakahAdmin()); }
		Common.appendKeToolbar(tampilkanFormula, addNew, comp);
		tampilkanFormula.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(null, false);
			}
		});

		onReloadTree(null, false);
		tampilkanKunci();

	}

	private Toolbarbutton bukaKunciDetail;
	private Toolbarbutton kunciDetail;
	private ItemKpi induk;

	public void onCetak(Event event) throws Exception {
		formatKpiDetail = (FormatKpiDetail) searchFormatKpi.getAttribute("formatKpiDetail");
		if (formatKpiDetail == null) {
			MyMessageboxConfig.show("Mohon maaf, Format KPI belum diisi. Langkah yang dapat dilakukan: (1) pilih Format KPI terlebih dahulu; (2) ulangi proses cetak.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		LaporanItemKpi laporanItemKpi = new LaporanItemKpi(formatKpiDetail);
		laporanItemKpi.setTitle("Cetak Item KPI");
		page.getFirstRoot().appendChild(laporanItemKpi);
		laporanItemKpi.setHeight("95%");
		laporanItemKpi.setWidth("90%");
		laporanItemKpi.setClosable(true);
		laporanItemKpi.onModal();
	}

	private void tampilkanKunci() {

		kunciDetail.setTooltiptext("Klik untuk meng-kunci format KPI ini");

		kunciDetail.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin mengunci Format KPI ini? Setelah dikunci, Format KPI tidak dapat diubah sampai kunci dibuka kembali.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Tbmuser tbmuser = Common.getCurrentUser();
									formatKpiDetail.setKunci(tbmuser);
									Common.refreshUpdate(formatKpiDetail);

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											onReloadTree(arg0, true);
										}
									});

								}

							}
						});
			}
		});

		bukaKunciDetail.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membuka kunci Format KPI ini? Setelah kunci dibuka, Format KPI dapat diubah kembali.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									formatKpiDetail.setKunci(null);
									Common.refreshUpdate(formatKpiDetail);

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											onReloadTree(arg0, true);
										}
									});

								}

							}
						});
			}
		});

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("KPI");
		treecol.setParent(treecols);

		treecol = new Treecol("Target");
		treecol.setWidth("8%");
		treecol.setParent(treecols);

		treecol = new Treecol("Satuan");
		treecol.setWidth("8%");
		treecol.setParent(treecols);

		if (tampilkanFormula.isChecked()) {
			treecol = new Treecol("Formula Rumus");
			treecol.setWidth("15%");
			treecol.setParent(treecols);

			treecol = new Treecol("Formula Hitungan");
			treecol.setWidth("15%");
			treecol.setParent(treecols);
		}

		treecol = new Treecol("Poin");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		treecol = new Treecol("Hitungan");
		treecol.setWidth("8%");
		treecol.setParent(treecols);

		treecol = new Treecol("Urut");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		if (searchFormatKpi.isVisible()) {
			treecol = new Treecol("");
			treecol.setWidth(adminLainBoleh ? "30px" : "0px");
			treecol.setParent(treecols);

			treecol = new Treecol("");
			treecol.setWidth("15%");
			treecol.setParent(treecols);
		}

		treecols.setParent(tree);
	}

	public void onAdd(Event event) throws Exception {

		ItemKpi myitemKpi = new ItemKpi(formatKpiDetail);
		myitemKpi.setParent(null);

		init(myitemKpi, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Timer timer = new Timer(500);
				timer.setParent(page.getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						timer.detach();
						onReloadTree(arg0, true);
					}
				});
				timer.start();
			}
		});
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(ItemKpi itemKpi, final EventListener eventListener) throws Exception {

		this.itemKpi = itemKpi;
		addWindow.setTitle(itemKpi.getId() == null ? "Tambah Item KPI" : "Ubah Item KPI");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setWidth("40%");

		column = new Column();
		column.setParent(columns);
		column.setWidth("60%");

		final Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("KPI")));
		row.appendChild(kpi = new Vbox());
		kpi.setWidth("90%");
		ItemKpiTreeAction.this.kpis = null;
		if (itemKpi.getId() == null) {

			MyToolbarbuttonConfig button;
			kpi.appendChild(button = new MyToolbarbuttonConfig("Ambil Data KPI", "/img/svg/addthis.svg"));

			final Vbox subKpi = new Vbox();
			kpi.appendChild(subKpi);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					formatKpiDetail = (FormatKpiDetail) searchFormatKpi.getAttribute("formatKpiDetail");
					if (formatKpiDetail == null) {
						MyMessageboxConfig.show("Mohon maaf, Format KPI belum diisi. Langkah yang dapat dilakukan: (1) pilih Format KPI terlebih dahulu; (2) ulangi proses.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					List<Kpi> kpis = new ArrayList<Kpi>();
					for (Object o : ConstantValues.ambilBerdasarClass(ItemKpi.class).values()) {
						ItemKpi itemKpi = (ItemKpi) o;
						if (itemKpi != null && itemKpi.getFormatKpi() != null
								&& itemKpi.getFormatKpi().getId().equals(formatKpiDetail.getFormatKpi().getId())) {
							kpis.add(itemKpi.getKpi());
						}
					}
					AmbilDataKpiBanyak ambilKpi = new AmbilDataKpiBanyak(kpis);

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilKpi);
					ambilKpi.setWidth("90%");
					ambilKpi.setHeight("90%");

					ambilKpi.setEventListener(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.clear(subKpi);
							ItemKpiTreeAction.this.kpis = (List<Kpi>) arg0.getData();

							if (ItemKpiTreeAction.this.kpis != null) {
								for (Kpi k : ItemKpiTreeAction.this.kpis) {
									subKpi.appendChild(new Label(k.getKode() + "-" + k.getNama()));
								}
							}

						}
					});

					ambilKpi.onModal();
				}
			});

		} else if (itemKpi.getKpi() != null) {
			kpi.appendChild(new Label(itemKpi.getKpi().getKode() + " - " + itemKpi.getKpi().getNama()));
		}

		row = new MyFormRow();
		row.setVisible(itemKpi.getKpi() != null);
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nomor Urut")));
		row.appendChild(nomorUrut = new MyIntbox(itemKpi.getNomorUrut()));
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(itemKpi.getKpi() != null);
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new Textbox(itemKpi.getKeterangan() == null ? "" : itemKpi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		induk = itemKpi.getParent();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("KPI Induk")));
		row.appendChild(new Label(induk == null ? "Tidak ada induk" : induk.getKode() + "-" + induk.getNama()));

		if (itemKpi.getKpi() != null) {

			row = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Formula"));

			row = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);
			array = new JSONArray(itemKpi.getFormula());
			rowFormula = Common.tampilanScroll1(row);
			reloadFormula(rowFormula, array);
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					// onReloadTree(null);
					eventListener.onEvent(new Event("", null, ItemKpiTreeAction.this.itemKpi));
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	@SuppressWarnings("rawtypes")
	public static void reloadDataFormula(final Row rowU, final JSONArray array, final boolean refresh)
			throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Tanggal Efektif");
		column.setParent(columns);
		column.setWidth("15%");

		column = new MyColumnConfig("Formula");
		column.setParent(columns);
		column.setWidth("55%");

		column = new MyColumnConfig("Nilai");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

		Map itemKps = ConstantValues.ambilBerdasarClass(ItemKpi.class);

		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);

			if (!jsonObject.isNull("tgl")) {

				Date tgl = new Date();

				String target = "";

				if (!jsonObject.isNull("tgl")) {
					tgl = Common.dateFormat1.get().parse(jsonObject.get("tgl").toString());
				}

				if (!jsonObject.isNull("target")) {
					target = jsonObject.get("target") + "";
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final Label nilai = new Label(
						Common.numberFormat.get().format(KpiUtil.ambilPoint(jsonObject, itemKps, refresh)));

				final MyTextbox targetText = new MyTextbox(target);
				final MyDatebox datebox = new MyDatebox(tgl);
				datebox.setWidth("90%");
				row.appendChild(datebox);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						jsonObject.put("tgl",
								datebox.getValue() == null ? "" : Common.dateFormat1.get().format(datebox.getValue()));

						String target = targetText.getValue() == null ? "" : targetText.getValue();
						jsonObject.put("target", target);

						nilai.setValue(Common.numberFormat.get().format(KpiUtil.ambilPoint(jsonObject,
								ConstantValues.ambilBerdasarClass(ItemKpi.class), refresh)));

					}
				};

				targetText.setWidth("90%");
				targetText.setRows(2);
				row.appendChild(targetText);

				datebox.addEventListener("onChange", eventListener);
				targetText.addEventListener("onChange", eventListener);

				nilai.setParent(row);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, array, false);

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
				button.setParent(row);
			}
		}
	}

	public static void reloadFormula(final Row rowFormula, final JSONArray array) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Formula", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("tgl", Common.dateFormat1.get().format(new Date()));
				jsonObject.put("target", "");
				array.put(jsonObject);

				reloadDataFormula(rowU, array, false);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array, false);

	}

	public boolean onSave(Event event) throws Exception {

		if (nomorUrut.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, nomor urut belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nomor Urut dengan angka; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		formatKpiDetail = (FormatKpiDetail) searchFormatKpi.getAttribute("formatKpiDetail");
		if (formatKpiDetail == null) {
			MyMessageboxConfig.show("Mohon maaf, Format KPI belum diisi. Langkah yang dapat dilakukan: (1) pilih Format KPI terlebih dahulu; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (itemKpi.getId() != null) {
			itemKpi = (ItemKpi) session.load(ItemKpi.class, itemKpi.getId());

		}

		if (ItemKpiTreeAction.this.kpis != null) {
			int urut = 1;
			Collections.sort(ItemKpiTreeAction.this.kpis);
			for (Kpi k : ItemKpiTreeAction.this.kpis) {
				ItemKpi itemKpi = new ItemKpi(formatKpiDetail);
				itemKpi.setNomorUrut(urut);
				itemKpi.setKpi(k);
				itemKpi.setFormatKpi(formatKpiDetail.getFormatKpi());
				itemKpi.setParent(induk);
				itemKpi.setKeterangan(keterangan.getValue());

				Common.refreshSaveOrUpdate(session, itemKpi);
				urut++;
			}
		} else {

			itemKpi.setFormatKpi(formatKpiDetail.getFormatKpi());
			itemKpi.setNomorUrut(nomorUrut.getValue());
			itemKpi.setParent(induk);
			itemKpi.setKeterangan(keterangan.getValue());

			if (array != null) {
				itemKpi.setFormula(array.toString());
			}
			Common.refreshSaveOrUpdate(session, itemKpi);

		}

		return true;
	}

	public void onReloadTree(Event event, final boolean hitungUlang) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
//				upload.setVisible(false);
				Common.clear(tree);

				formatKpiDetail = (FormatKpiDetail) searchFormatKpi.getAttribute("formatKpiDetail");
				if (formatKpiDetail == null) {
					return;
				}

				Pegawai pegawai = formatKpiDetail.getPegawai();
				if (pegawai == null) {
					return;
				}

				Sessions.getCurrent(true).setAttribute("nama_crit_" + ItemKpi.class.getSimpleName(), "formatKpi");
				Sessions.getCurrent(true).setAttribute("nilai_crit_" + ItemKpi.class.getSimpleName(),
						formatKpiDetail.getFormatKpi());

				Tbmuser tbmuser = Common.getCurrentUser();

				bukaKunciDetail.setVisible(formatKpiDetail.getKunci() != null);
				if (formatKpiDetail.getKunci() != null) {
					bukaKunciDetail.setTooltiptext("Dikunci oleh " + formatKpiDetail.getKunci().getUserId());
				}
				bukaKunciDetail.setVisible(formatKpiDetail.getKunci() != null);
				bukaKunciDetail.setDisabled(tbmuser == null || formatKpiDetail.getKunci() == null
						|| !formatKpiDetail.getKunci().getUserId().equals(tbmuser.getUserId()));
				kunciDetail.setVisible(formatKpiDetail.getKunci() == null);

//				upload.setVisible(formatKpi.getKunci() == null);

				buttonHapus.setVisible(formatKpiDetail.getKunci() == null);

				bukaKunciDetail
						.setLabel(formatKpiDetail.getKunci() == null ? "" : formatKpiDetail.getKunci().getUserNama());
				kunciDetail.setLabel(
						formatKpiDetail.getKunci() == null ? "Kunci" : formatKpiDetail.getKunci().getUserNama());

				totalTarget.setValue(Common.numberFormat.get().format(formatKpiDetail.getNilai()));

				if (tbmuser != null && tbmuser.getPegawai() != null
						&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
					bukaKunciDetail.setVisible(false);
					kunciDetail.setVisible(false);
				}

				initTree();

				addNew.setVisible(add);
				itemKpiTreeModel = new ItemKpiTreeModel(true, formatKpiDetail);
				tree.setModel(itemKpiTreeModel);
				tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

					@Override
					public void render(final Treeitem treeitem, Object arg1) throws Exception {
						final ItemKpi itemKpi = (ItemKpi) arg1;

						try {
							Common.clear(treeitem);
							final Treerow treerow = new Treerow();
							treerow.setParent(treeitem);

							hasSomeChilds(hitungUlang, treerow, itemKpi, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadTreeitem(hitungUlang, treeitem, true, false);
								}
							});

							Treecell arg0 = new Treecell();
							arg0.setParent(treerow);
							if (formatKpiDetail.getKunci() == null && searchFormatKpi.isVisible()) {
								Hbox toolbar = new Hbox();

								Toolbarbutton button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
								button.setTooltiptext("Refresh");
								// button.setVisible(hasChild);
								button.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										reloadTreeitem(true, treeitem, true, true);
									}
								});
								button.setParent(toolbar);

								button = new MyToolbarbuttonConfig("", "/img/svg/addthis.svg");
								button.setTooltiptext("Tambah Data");
								button.setVisible(add);
								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										ItemKpi myitemKpi = (ItemKpi) itemKpi.clone();
										myitemKpi.setParent(itemKpi);
										myitemKpi.setId(null);
										myitemKpi.setKpi(null);
										init(myitemKpi, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												render(treeitem, itemKpi);
												reloadTreeitem(true, treeitem, true, true, new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														final Timer timer = new Timer(300);
														timer.setParent(page.getFirstRoot());
														timer.addEventListener("onTimer", new EventListener() {

															@SuppressWarnings({})
															@Override
															public void onEvent(Event arg0) throws Exception {
																System.out.println("======= open tree item =======");

																try {
																	Treeitem myTreeitem = (Treeitem) treecellMap
																			.get(itemKpi.getId()).getParent()
																			.getParent();

																	render(myTreeitem, itemKpi);

																	reloadTreeitem(hitungUlang, myTreeitem, true, false,
																			new EventListener() {

																				@Override
																				public void onEvent(Event arg0)
																						throws Exception {

																					final Timer timer = new Timer(300);
																					timer.setParent(
																							page.getFirstRoot());
																					timer.addEventListener("onTimer",
																							new EventListener() {

																								@Override
																								public void onEvent(
																										Event arg0)
																										throws Exception {

																									System.out.println(
																											"========================= RELOAD TOTAL ===========================");

																									Treeitem myTreeitem = (Treeitem) treecellMap
																											.get(itemKpi
																													.getId())
																											.getParent()
																											.getParent();
																									reloadTreeitem(
																											hitungUlang,
																											myTreeitem,
																											true,
																											false);

																									timer.detach();
																								}

																							});
																					timer.start();

																				}
																			});

																} catch (Exception e) {
																	// TODO
																	// Auto-generated
																	// catch
																	// block
																	Common.tampilErrorJikaAdmin(e);
																}

																timer.detach();
															}
														});

														timer.start();

													}
												});
											}
										});

										addWindow.setVisible(true);
										addWindow.onModal();
									}

								});
								button.setParent(toolbar);

								button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
								button.setTooltiptext("Copy Data");
								button.setVisible(edit);
								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {

										ItemKpi myitemKpi = (ItemKpi) itemKpi.clone();
										myitemKpi.setParent(itemKpi.getParent());
										myitemKpi.setId(null);
										init(myitemKpi, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												reloadTreeitem(true, treeitem, true, true);
											}
										});
										addWindow.setVisible(true);
										addWindow.onModal();
									}

								});
								button.setParent(toolbar);

								button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
								button.setTooltiptext("Rubah Data");
								button.setVisible(edit);
								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										init(itemKpi, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												reloadTreeitem(true, treeitem, false, true);

											}
										});
										addWindow.setVisible(true);
										addWindow.onModal();
									}

								});
								button.setParent(toolbar);

								button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
								button.setTooltiptext("Hapus Data");
								button.setVisible(delete);
								button.addEventListener("onClick", new EventListener() {
									@Override
									public void onEvent(Event event) throws Exception {
										MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
												MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
												new EventListener() {

													@Override
													public void onEvent(Event event) throws Exception {
														int i = Integer.parseInt(event.getData().toString());
														if (i == MyMessageboxConfig.OK) {
															try {

																itemKpiTreeModel.deleteChilds(itemKpi);

																Common.refreshDelete((itemKpi));

																reloadTreeitem(true, treeitem, true, true);
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
								toolbar.setParent(arg0);
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
			}
		});
	}

	private void reloadTreeitem(boolean hitungUlang, Treeitem treeitem, Boolean reloadTotal, Boolean loadParent) {
		reloadTreeitem(hitungUlang, treeitem, reloadTotal, loadParent, null);
	}

	private void reloadTreeitem(final boolean hitungUlang, final Treeitem treeitem, final Boolean reloadTotal,
			final Boolean loadParent, final EventListener eventListener) {
		final Treeitem treeitemParent = loadParent ? treeitem.getParentItem() : treeitem;
		if (treeitemParent == null) {
			try {
				onReloadTree(null, hitungUlang);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			treeitemParent.unload();
			final Timer timer = new Timer(300);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					treeitemParent.setOpen(true);
					treeitem.setOpen(true);
					if (reloadTotal) {
						reloadTotal(hitungUlang);
					}
					if (eventListener != null) {
						eventListener.onEvent(null);
					}
					timer.detach();
				}

			});

			timer.start();
		}
	}

	private void reloadTotal(boolean hitungUlang) throws Exception {
		// TODO Auto-generated method stub
		for (Long itemKpiid : treecellMap.keySet()) {
			Treecell treecell = treecellMap.get(itemKpiid);
			ItemKpi itemKpi = (ItemKpi) treecell.getAttribute("itemKpi");
			String target = KpiUtil.ambilTarget(itemKpi.getFormula(), sekarang);
			Double hasil = hitungUlang ? itemKpi.getTarget()
					: itemKpiTreeModel.hitungItemKpi(itemKpi, target, hitungUlang, null);
			treecell.setLabel(Common.numberFormat.get().format(hasil));

			if (itemKpi.getTarget().intValue() != hasil.intValue()) {
				Session session = HibernateUtil.currentSession();
				session.refresh(itemKpi);
				itemKpi.setTarget(hasil);
				Common.refreshUpdate(session, itemKpi);
				session.flush();
			}

		}

		for (Long itemKpiid : treecellMap.keySet()) {
			Treecell treecell = treecellMap.get(itemKpiid);
			ItemKpi itemKpi = (ItemKpi) treecell.getAttribute("itemKpi");

			if (itemKpi.getKpi().getNilaifinal() && formatKpiDetail != null && itemKpi.getTarget() != null
					&& formatKpiDetail.getNilai().intValue() != itemKpi.getTarget().intValue()) {
				Session session = HibernateUtil.currentSession();
				session.refresh(formatKpiDetail);
				formatKpiDetail.setNilai(itemKpi.getTarget());
				Common.refreshUpdate(session, formatKpiDetail);
				session.flush();
			}

			if (itemKpi.getKpi().getNilaifinal()) {
				totalTarget.setValue(Common.numberFormat.get().format(formatKpiDetail.getNilai()));
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void openChilds(final Treeitem treeitemParent, int max, int index) {
		if (max > index) {
			treeitemParent.setOpen(true);
			List<Treeitem> treeitems = treeitemParent.getChildren();
			for (Object object : treeitems) {

				if (object instanceof Treechildren) {
					Treechildren treechildren = (Treechildren) object;
					List<Treeitem> mytreeitems = treechildren.getChildren();
					for (Treeitem treeitem : mytreeitems) {
						openChilds(treeitem, max, (++index));
					}

				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void closeChilds(final Treeitem treeitemParent) {
		treeitemParent.setOpen(false);
		List<Treeitem> treeitems = treeitemParent.getChildren();
		for (Object object : treeitems) {

			if (object instanceof Treechildren) {
				Treechildren treechildren = (Treechildren) object;
				List<Treeitem> mytreeitems = treechildren.getChildren();
				for (Treeitem treeitem : mytreeitems) {
					closeChilds(treeitem);
				}

			}
		}
	}

	private void hasSomeChilds(boolean hitungUlang, Treerow treerow, final ItemKpi itemKpi,
			final EventListener eventListener) throws Exception {

		String bg = "color:" + itemKpi.getKpi().getColor() + ";";
		if (!itemKpi.getKpi().getTanpaWarnaBackgrond()) {
			bg = bg + "background-color:" + itemKpi.getKpi().getBackground() + ";";
		}

		String style = "font-size:x-small;text-align: left;" + bg;
		String style1 = "font-size:x-small;text-align: right;" + bg;

		if (itemKpi.getKpi().getFontBold() && itemKpi.getKpi().getFontBesar()) {
			style = "font-size:small;text-align: left; font-weight: bold;color:" + itemKpi.getKpi().getColor() + ";"
					+ bg;
			style1 = "font-size:small;text-align: right; font-weight: bold;color:" + itemKpi.getKpi().getColor() + ";"
					+ bg;
		} else if (itemKpi.getKpi().getFontBesar()) {
			style = "font-size:small;text-align: left;color:" + itemKpi.getKpi().getColor() + ";" + bg;
			style1 = "font-size:small;text-align: right;color:" + itemKpi.getKpi().getColor() + ";" + bg;
		} else if (itemKpi.getKpi().getFontBold()) {
			style = "font-size:x-small;text-align: left; font-weight: bold;color:" + itemKpi.getKpi().getColor() + ";"
					+ bg;
			style1 = "font-size:x-small;text-align: right; font-weight: bold;color:" + itemKpi.getKpi().getColor() + ";"
					+ bg;
		}

		Treecell treecell = new Treecell(
				itemKpi.getKode() + "-" + itemKpi.getNama() + (itemKpi.getKpi().getNilaifinal() ? "(Final)" : ""));
		treecell.setTooltiptext(itemKpi.toString());
		treecell.setStyle(style);
		treecell.setParent(treerow);
		ParameterTambahan parameterTambahan = itemKpi.getKpi().getSatuanKpi() == null ? null
				: itemKpi.getKpi().getSatuanKpi().getParameterTambahan();

		Session session = HibernateUtil.currentSession();
		MasaKpi masaKpi = (MasaKpi) ConstantValues.simpleObject(session.createCriteria(MasaKpi.class)
				.add(Restrictions.sqlRestriction("(date('" + Common.databaseDateFormat.get().format(sekarang)
						+ "') between this_.mulaitarget and this_.sampaitarget) or (this_.mulaitarget is null and this_.sampaitarget is null) or (this_.mulaitarget <= date('"
						+ Common.databaseDateFormat.get().format(sekarang)
						+ "') and this_.sampaitarget is null)  or (this_.sampaitarget >= date('"
						+ Common.databaseDateFormat.get().format(sekarang) + "') and this_.mulaitarget is null)"))
				.addOrder(Order.desc("id")).setMaxResults(1), MasaKpi.class);

		boolean editUpload = edit && formatKpiDetail.getKunci() == null && masaKpi != null;

		if (formatKpiDetail.getKunci() == null && tbmuser != null && tbmuser.getUserId() != null) {

//			System.out.println("formatKpiDetail.getFormatKpi().getUsernamePenggunaTarget() -> "
//					+ formatKpiDetail.getFormatKpi().getUsernamePenggunaTarget() + ", tbmuser.getUserId() -> "
//					+ tbmuser.getUserId());
			
			Tbmrole tbmrole = tbmuser.hakAkses();

			if (formatKpiDetail.getFormatKpi() != null && (formatKpiDetail.getFormatKpi().getUsernamePenggunaTarget()
					.toLowerCase().trim().contains("," + tbmuser.getUserId().trim().toLowerCase() + ",")  || (tbmrole != null &&  formatKpiDetail.getFormatKpi().getJenisPengguna()
					.toLowerCase().trim().contains("," + tbmrole.getRoleId().trim().toLowerCase() + ",")))  ) {
				editUpload = true;
			}

		}

		if (editUpload) {

			treecell = new Treecell();
			treecell.setStyle(style);
			treecell.setParent(treerow);

			if (parameterTambahan != null) {
				Component component = ParameterTambahan.ambilComponent(itemKpi.getVal(), parameterTambahan,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ParameterTambahan parameterTambahan = itemKpi.getKpi().getSatuanKpi() == null ? null
										: itemKpi.getKpi().getSatuanKpi().getParameterTambahan();
								String val = ParameterTambahan.ambilValComponent(arg0.getTarget(), parameterTambahan);
								try {
									val = val.split(":")[1];
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/ItemKpiTreeAction.java:1315");
									// TODO: handle exception
								}

								itemKpi.setVal(val);

								try {
									if (arg0.getTarget() instanceof Textbox) {
										itemKpi.setValtampil(((Textbox) arg0.getTarget()).getValue());
									} else if (arg0.getTarget() instanceof Doublebox) {
										itemKpi.setValtampil(
												Common.numberFormat.get().format(((Doublebox) arg0.getTarget()).getValue()));
									} else if (arg0.getTarget() instanceof Intbox) {
										itemKpi.setValtampil(
												Common.numberFormat.get().format(((Intbox) arg0.getTarget()).getValue()));
									} else if (arg0.getTarget() instanceof Longbox) {
										itemKpi.setValtampil(
												Common.numberFormat.get().format(((Longbox) arg0.getTarget()).getValue()));
									} else if (arg0.getTarget() instanceof Intbox) {
										itemKpi.setValtampil(
												Common.numberFormat.get().format(((Intbox) arg0.getTarget()).getValue()));
									} else if (arg0.getTarget() instanceof Datebox) {
										itemKpi.setValtampil(
												Common.dateFormat.get().format(((Datebox) arg0.getTarget()).getValue()));
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/ItemKpiTreeAction.java:1340");
									// TODO: handle exception
								}

								Common.refreshUpdate(itemKpi);
								eventListener.onEvent(arg0);
							}
						});

				treecell.appendChild(component);
			}
		} else {
			treecell = new Treecell(parameterTambahan != null ? itemKpi.getValtampil() : "");
			treecell.setStyle(style1);
			treecell.setParent(treerow);
		}

		treecell = new Treecell(itemKpi.getKpi() == null || itemKpi.getKpi().getSatuanKpi() == null ? ""
				: itemKpi.getKpi().getSatuanKpi().getKode());
		treecell.setStyle(style);
		treecell.setParent(treerow);

		List<String> penghitungan = new ArrayList<String>();
		String target = KpiUtil.ambilTarget(itemKpi.getFormula(), sekarang);
		Double hasil = hitungUlang ? itemKpi.getTarget()
				: itemKpiTreeModel.hitungItemKpi(itemKpi, target, hitungUlang, penghitungan);

		if (tampilkanFormula.isChecked()) {
			treecell = new Treecell(target);
			treecell.setTooltiptext(target);
			treecell.setStyle(style);
			treecell.setParent(treerow);

			treecell = new Treecell();
			treecell.setStyle("font-size:x-small;text-align: left;");
			treecell.setParent(treerow);

			Vbox vbox = new Vbox();
			for (String ss : penghitungan) {
				vbox.appendChild(new MyLabelAgakKecil(ss));
			}
			treecell.appendChild(vbox);
		}

		Double point = KpiUtil.ambilPoint(itemKpi.getKpi().getFormula(), sekarang, hitungUlang);
		treecell = new Treecell(Common.numberFormat.get().format(point));
		treecell.setTooltiptext(Common.numberFormat.get().format(point));
		treecell.setStyle(style1);
		treecell.setParent(treerow);

		Treecell treecellHasil = new Treecell(Common.numberFormat.get().format(hasil));
		treecellHasil.setAttribute("itemKpi", itemKpi);
		treecellHasil.setTooltiptext(Common.numberFormat.get().format(hasil));
		treecellHasil.setStyle(style1);
		treecellHasil.setParent(treerow);

		if (itemKpi.getTarget().intValue() != hasil.intValue()) {
			itemKpi.setTarget(hasil);
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, itemKpi);
					session.flush();
				}
			});
		}

		if (itemKpi.getKpi().getNilaifinal() && formatKpiDetail != null && hasil != null
				&& formatKpiDetail.getNilai().intValue() != hasil.intValue()) {

			session.refresh(formatKpiDetail);
			formatKpiDetail.setNilai(hasil);
			Common.refreshUpdate(session, formatKpiDetail);
			session.flush();
			
		}

		if (itemKpi.getKpi().getNilaifinal()) {
			totalTarget.setValue(Common.numberFormat.get().format(formatKpiDetail.getNilai()));
		}
		
		
		

		treecell = new Treecell(itemKpi.getNomorUrut().toString());
		treecell.setTooltiptext(itemKpi.getNomorUrut().toString());
		treecell.setStyle(style);
		treecell.setParent(treerow);

		if (searchFormatKpi.isVisible()) {
			treecell = new Treecell();
			treecell.setParent(treerow);
			RevisiHelper.createNewRevisi(ItemKpi.class, itemKpi, "H").setParent(treecell);
		}

		treecellMap.put(itemKpi.getId(), treecellHasil);

	}

}
