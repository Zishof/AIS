package ais.action.master.kpi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyColorbox;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Html;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
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

import ais.action.master.employ.helper.GolonganUtil;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.kpi.helper.KpiUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.kpi.KategoriKpi;
import ais.database.model.kpi.Kpi;
import ais.database.model.kpi.SatuanKpi;
import ais.database.model.kpi.SkorKpi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class KpiAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Combobox searchkategori;
	private Checkbox searchaktif;

	private Checkbox fontBesar;
	private Checkbox fontBold;
	private MyColorbox color;
	private MyCheckboxConfig tanpaWarnaBackgrond;
	private MyColorbox background;
	private MyCheckboxConfig nilaifinal;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Kpi kpi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private JSONArray array;
	private Row rowFormula;

	private Combobox satuanKpi;
	private Combobox kategoriKpi;

	private Tabpanel manajemenParameter;

	public void onManajemenParameter(Event event) {
		if (manajemenParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenParameter);
			MyInclude iframe = new MyInclude("/pages/master/kpi/skor_kpi.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel konstanta;
	private Textbox valDefault;

	public void onKonstanta(Event event) {
		if (konstanta.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konstanta);
			MyInclude iframe = new MyInclude("/pages/master/konstanta.zul");
			iframe.setParent(window);
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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.insertComboDanSemua(searchkategori, new String[] { "nama", "kode" }, "keterangan", KategoriKpi.class,
				Restrictions.eq("aktif", true));

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		final List<String> columnHeadersAdding = new ArrayList<String>();
		final String[] contents = new String[] { "id", "kode", "nama", "satuanKpi", "kategoriKpi", "keterangan",
				"aktif" };

		for (Object o : ConstantValues.ambilBerdasarClass(SkorKpi.class).values()) {
			SkorKpi skorKpi = (SkorKpi) o;
			if (skorKpi.getAktif()) {
				columnHeadersAdding.add(skorKpi.getKode());
			}
		}
		Collections.sort(columnHeadersAdding);

		columnHeadersAdding.add("target");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Kpi kpi = (Kpi) objects[0];
				XSSFRow row = (XSSFRow) objects[2];

				try {
					JSONArray jsonArray = new JSONArray(kpi.getFormula());
					int index = jsonArray.length() - 1;
					JSONObject jsonObject = index < 0 ? new JSONObject() : jsonArray.getJSONObject(index);
					int i = contents.length - 1;
					for (String col : columnHeadersAdding) {
						String val = "";
						if (!jsonObject.isNull(col)) {
							val = jsonObject.get(col).toString();
						}
						row.createCell(++i).setCellValue(val);
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/KpiAction.java:202");
					// TODO: handle exception
				}

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Kpi.class, this, "Download",
				"/img/print.png", columnHeadersAdding, dataAdding, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Kpi.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] data = (Object[]) arg0.getData();
				Kpi kpi = (Kpi) data[0];
				@SuppressWarnings("rawtypes")
				Map datum = (Map) data[2];

				try {
					JSONArray jsonArray = new JSONArray(kpi.getFormula());
					int index = jsonArray.length() - 1;
					JSONObject jsonObject = index < 0 ? new JSONObject() : jsonArray.getJSONObject(index);
					for (String col : columnHeadersAdding) {

						for (Object key : datum.keySet()) {
							if (key != null && key.toString().equalsIgnoreCase(col)) {
								try {
									Double val = (Double) Double.parseDouble(datum.get(key).toString().trim());
									jsonObject.put(col, val);
								} catch (Exception e) {
									try {
										String val = datum.get(key).toString().trim();
										jsonObject.put(col, val);
									} catch (Exception ew) { ais.common.ErrorAuditUtil.record(ew, "auto-audit(empty-catch) src/ais/action/master/kpi/KpiAction.java:237");

									}
								}
								break;
							}
						}

					}

					if (index < 0) {
						jsonObject.put("tgl", Common.dateFormat1.get().format(WaktuUtil.getDate()));
						jsonArray.put(jsonObject);
					}

					String formulaBaru = jsonArray.toString();
					System.out
							.println("index -> " + index + " \nformulaBaru -> " + formulaBaru + " \ndatum -> " + datum);
					kpi.setFormula(formulaBaru);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

		}, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
//				onSearchDefault(null);
			}

		}, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class KpiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kpi kpi = (Kpi) arg1;

			String bg = "color:" + kpi.getColor() + ";";
			if (!kpi.getTanpaWarnaBackgrond()) {
				bg = bg + "background-color:" + kpi.getBackground() + ";";
			}

			String style = "font-size:x-small;" + bg;

			if (kpi.getFontBold() && kpi.getFontBesar()) {
				style = "font-size:small;text-align: left; font-weight: bold;color:" + kpi.getColor() + ";" + bg;
			} else if (kpi.getFontBesar()) {
				style = "font-size:small;text-align: left;color:" + kpi.getColor() + ";" + bg;
			} else if (kpi.getFontBold()) {
				style = "font-size:x-small;text-align: left; font-weight: bold;color:" + kpi.getColor() + ";" + bg;
			}

			Label l;
			(l = new Label(kpi.getKode())).setParent(arg0);
			l.setStyle(style);
			RevisiHelper.createNewRevisi(Kpi.class, kpi, kpi.getNama()).setParent(arg0);

			new Label(kpi.getKategoriKpi() == null ? "" : kpi.getKategoriKpi().getNama()).setParent(arg0);
			new Label(kpi.getSatuanKpi() == null ? "" : kpi.getSatuanKpi().getNama()).setParent(arg0);

			new Html(KpiUtil.ambilDeskripsi(kpi.getFormula())).setParent(arg0);

			new Label(Common.numberFormat.get().format(KpiUtil.ambilPoint(kpi.getFormula(), WaktuUtil.getDate(), false)))
					.setParent(arg0);
			new Label(kpi.getValDefault()).setParent(arg0);
			new Label(kpi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kpi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kpi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kpi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kpi, KpiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Kpi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kpi = (Kpi) obj;
		init(kpi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(Kpi kpi) throws Exception {
		this.kpi = kpi;
		addWindow.setTitle(kpi.getId() == null ? "Tambah KPI" : "Ubah KPI");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode KPI *"));
		row.appendChild(kode = new Textbox(kpi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama KPI *"));
		row.appendChild(nama = new Textbox(kpi.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan KPI"));
		row.appendChild(satuanKpi = new Combobox());
		Common.insertComboDanSemua(satuanKpi, new String[] { "nama", "kode" }, "keterangan", SatuanKpi.class,
				"==Tanpa Satuan==", Restrictions.eq("aktif", true));
		Common.selectComboItem(satuanKpi, kpi.getSatuanKpi());
		satuanKpi.setWidth("90%");
		satuanKpi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori KPI"));
		row.appendChild(kategoriKpi = new Combobox());
		Common.insertComboDanSemua(kategoriKpi, new String[] { "nama", "kode" }, "keterangan", KategoriKpi.class,
				"==Tanpa Kategori==", Restrictions.eq("aktif", true));
		Common.selectComboItem(kategoriKpi, kpi.getKategoriKpi());
		kategoriKpi.setWidth("90%");
		kategoriKpi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Default *"));
		row.appendChild(valDefault = new Textbox(kpi.getValDefault()));
		valDefault.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kpi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(nilaifinal = new MyCheckboxConfig("Merupakan Nilai Final"));
		nilaifinal.setChecked(kpi.getNilaifinal());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(fontBesar = new MyCheckboxConfig("Tulisan besar"));
		fontBesar.setChecked(kpi.getFontBesar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(fontBold = new MyCheckboxConfig("Tulisan tebal"));
		fontBold.setChecked(kpi.getFontBold());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Warna tulisan")));
		row.appendChild(color = new MyColorbox());
		color.setColor(kpi.getColor());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(tanpaWarnaBackgrond = new MyCheckboxConfig("Tulisan tanpa warna background"));
		tanpaWarnaBackgrond.setChecked(kpi.getTanpaWarnaBackgrond());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Warna background tulisan")));
		row.appendChild(background = new MyColorbox());
		background.setColor(kpi.getBackground());

		EventListener eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				background.getParent().setVisible(!tanpaWarnaBackgrond.isChecked());

			}
		};
		tanpaWarnaBackgrond.addEventListener("onClick", eventListener2);
		eventListener2.onEvent(null);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Formula"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		array = new JSONArray(kpi.getFormula());
		rowFormula = Common.tampilanScroll1(row);
		reloadFormula(rowFormula, array);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public static void reloadDataFormula(final Row rowU, final JSONArray array) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Tanggal Efektif");
		column.setParent(columns);
		column.setWidth("12%");

		List<SkorKpi> skorKpis = new ArrayList<SkorKpi>();
		for (Object o : ConstantValues.ambilBerdasarClass(SkorKpi.class).values()) {
			SkorKpi skorKpi = (SkorKpi) o;
			if (skorKpi.getAktif()) {
				skorKpis.add(skorKpi);
			}
		}
		Collections.sort(skorKpis);

		int lebar = skorKpis.size() == 0 ? 60 : 60 / skorKpis.size();

		for (SkorKpi skorKpi : skorKpis) {
			column = new MyColumnConfig(skorKpi.getKode());
			column.setTooltiptext(skorKpi.getNama());
			column.setParent(columns);
			column.setWidth(lebar + "%");
		}

		column = new MyColumnConfig("Formula");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Nilai");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		Rows rows = new Rows();
		rows.setParent(grid);

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

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				final Label nilai = new Label(Common.numberFormat.get().format(KpiUtil.ambilPoint(jsonObject, false)));

				final MyTextbox targetText = new MyTextbox(target);
				final MyDatebox datebox = new MyDatebox(tgl);
				datebox.setWidth("90%");
				row.appendChild(datebox);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (arg0 != null && arg0.getTarget() instanceof MyDoublebox
								&& arg0.getTarget().getAttribute("skorKpi") != null) {
							SkorKpi skorKpi = (SkorKpi) arg0.getTarget().getAttribute("skorKpi");
							MyDoublebox doublebox = (MyDoublebox) arg0.getTarget();
							jsonObject.put(skorKpi.getKode(), doublebox.getValue() == null ? "" : doublebox.getValue());
						}

						jsonObject.put("tgl",
								datebox.getValue() == null ? "" : Common.dateFormat1.get().format(datebox.getValue()));

						String target = targetText.getValue() == null ? "" : targetText.getValue();
						jsonObject.put("target", target);

						nilai.setValue(Common.numberFormat.get().format(KpiUtil.ambilPoint(jsonObject, false)));

					}
				};

				for (final SkorKpi skorKpi : skorKpis) {
					String val = "";
					if (!jsonObject.isNull(skorKpi.getKode())) {
						val = jsonObject.get(skorKpi.getKode()).toString();
					}
					if (skorKpi.getParameterTambahan() != null) {
						Component component = ParameterTambahan.ambilComponent(val, skorKpi.getParameterTambahan(),
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										String val = ParameterTambahan.ambilValComponent(arg0.getTarget(),
												skorKpi.getParameterTambahan());
										
										try {
											val = val.split(":")[1];
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/KpiAction.java:633");
											// TODO: handle exception
										}
										
										jsonObject.put(skorKpi.getKode(), val);
										jsonObject.put("tgl", datebox.getValue() == null ? ""
												: Common.dateFormat1.get().format(datebox.getValue()));

										String target = targetText.getValue() == null ? "" : targetText.getValue();
										jsonObject.put("target", target);

										nilai.setValue(Common.numberFormat.get().format(GolonganUtil.ambilPoint(jsonObject)));
									}
								});
						component.setAttribute("skorKpi", skorKpi);
						row.appendChild(component);
					} else {

						Double point = 0.0;
						if (!jsonObject.isNull(skorKpi.getKode())) {
							point = jsonObject.getDouble(skorKpi.getKode());
						}
						MyDoublebox doublebox = new MyDoublebox(point);
						doublebox.setAttribute("skorKpi", skorKpi);
						doublebox.setWidth("85%");
						row.appendChild(doublebox);
						doublebox.addEventListener("onChange", eventListener);
					}
				}

				targetText.setWidth("90%");
				targetText.setRows(3);
				row.appendChild(targetText);

				datebox.addEventListener("onChange", eventListener);
				targetText.addEventListener("onChange", eventListener);

				nilai.setParent(row);

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
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, array);

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

				reloadDataFormula(rowU, array);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array);

	}

	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode KPI belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode KPI dengan kode yang sesuai; (2) pastikan kolom tidak kosong; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama KPI belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama KPI dengan nama yang sesuai; (2) pastikan kolom tidak kosong; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKpi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode KPI yang dimasukkan sudah ada di database. Langkah yang dapat dilakukan: (1) gunakan kode lain yang belum terdaftar; (2) periksa daftar KPI yang sudah ada; (3) ulangi proses penyimpanan dengan kode berbeda. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kpi.getId() != null) {
			kpi = (Kpi) session.load(Kpi.class, kpi.getId());

		}

		kpi.setKode(kode.getValue());
		kpi.setNama(nama.getValue());
		kpi.setKeterangan(keterangan.getValue());
		kpi.setFormula(array.toString());
		kpi.setSatuanKpi(
				(SatuanKpi) (satuanKpi.getSelectedItem() == null ? null : satuanKpi.getSelectedItem().getValue()));
		kpi.setKategoriKpi((KategoriKpi) (kategoriKpi.getSelectedItem() == null ? null
				: kategoriKpi.getSelectedItem().getValue()));

		kpi.setNilaifinal(nilaifinal.isChecked());
		kpi.setFontBesar(fontBesar.isChecked());
		kpi.setFontBold(fontBold.isChecked());
		kpi.setTanpaWarnaBackgrond(tanpaWarnaBackgrond.isChecked());
		kpi.setBackground(background.getColor());
		kpi.setColor(color.getColor());
		kpi.setValDefault(valDefault.getValue().trim());

		Common.refreshSaveOrUpdate(session, kpi);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Kpi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkategori.getSelectedItem() == null || searchkategori.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kategoriKpi", searchkategori.getSelectedItem().getValue()))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Kpi> kpi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kpi);
		grid.setRowRenderer(new KpiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKpi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Kpi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.kpi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kpi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
