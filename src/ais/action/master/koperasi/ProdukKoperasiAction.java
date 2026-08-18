package ais.action.master.koperasi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.surat.helper.AmbilDataNomorSuratBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.koperasi.JenisTransaksiKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.koperasi.SyaratProdukKoperasi;
import ais.database.model.koperasi.TipeProdukKoperasi;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ProdukKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ProdukKoperasi produkKoperasi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private Combobox koperasi;
	private Koperasi myKoperasi;
	private Combobox tipeProdukKoperasi;
	private MyCheckboxConfig hitungShu;
	private Columns subColumns;
	private Column c;
	private Rows subRows;
	private Row subRow;
	private Set<SyaratProdukKoperasi> selectedSyaratProdukKoperasi;
	private Set<Long> ids;
	private Vbox vboxSkala;
	private AmbilDataNomorSuratBanbox nomorSurat;
	private double setoran;
	private Row rowDetail;
	protected JSONArray array;
	protected Row rowFormula;
	private MyDoublebox bunga;
	private MyDoublebox nilaiMaksimal;
	private MyDoublebox nilaiMinimal;
	private Combobox durasi;
	private MyDoublebox jangkaWaktuBulan;
	private Combobox penghitunganBunga;
	private Combobox metodeBunga;
	private Combobox metodeBungaSimpanan;
	private MyDoublebox bungaSimpananPersen;
	private MyLabelConfig jangkaWaktuBulanLabel;
	private MyCheckboxConfig otomatisTerbentukTransaksi;
	private MyIntbox jumlahTransaksiTerbentuk;
	private MyCheckboxConfig hanyaBolehSekaliTransaksi;

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

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "nomorSurat", "formula", "setoran",
				"penghitunganBunga", "bunga", "otomatisTerbentukTransaksi", "jumlahTransaksiTerbentuk",
				"hanyaBolehSekaliTransaksi", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ProdukKoperasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ProdukKoperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class ProdukKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ProdukKoperasi produkKoperasi = (ProdukKoperasi) arg1;
			new Label(produkKoperasi.getKoperasi().getNama()).setParent(arg0);
			new Label(produkKoperasi.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(ProdukKoperasi.class, produkKoperasi, produkKoperasi.getNama())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(produkKoperasi.getSetoran())).setParent(arg0);

			new Label(produkKoperasi.getTipeProdukKoperasi().getNama()).setParent(arg0);
			new Label(produkKoperasi.getNomorSurat() == null ? "" : produkKoperasi.getNomorSurat().getContohFormat())
					.setParent(arg0);

			new Label(produkKoperasi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(produkKoperasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					produkKoperasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(produkKoperasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, produkKoperasi, ProdukKoperasiAction.this).setParent(arg0);

		}

	}

	public static void reloadFormula(final Row rowFormula, final JSONArray array, final boolean persetujuan)
			throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Jenis Transaksi", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(!persetujuan);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("nama", "");
				jsonObject.put("qty", 1.0);
				jsonObject.put("harga", 0.0);
				jsonObject.put("jumlah", 0.0);
				Long key = Math.abs(Common.randLong());
				jsonObject.put("key", key);

				array.put(jsonObject);

				reloadDataFormula(rowU, array, persetujuan);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array, persetujuan);

	}

	public static void reloadDataFormula(final Row rowU, final JSONArray array, final boolean persetujuan)
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

		MyColumnConfig column = new MyColumnConfig("Jenis Transaksi");
		column.setParent(columns);

		column = new MyColumnConfig("Qty Default");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Nilai Default");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Total Default");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("8%");

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		final Footer footerTotal = new Footer("");
		foot.appendChild(footerTotal);

		footer = new Footer("");
		foot.appendChild(footer);

		Rows rows = new Rows();
		rows.setParent(grid);

		final EventListener hitungTotal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Double nilai = 0.0;
				for (int i = 0; i < array.length(); i++) {
					Double jumlah = 0.0;
					JSONObject jsonObject = array.getJSONObject(i);
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}
					nilai += jumlah;
				}
				footerTotal.setLabel(Common.numberFormat.get().format(nilai));
			}

		};

		hitungTotal.onEvent(null);
		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {

				JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) (jsonObject
						.isNull("jenisTransaksiKoperasi") ? null
								: ConstantValues.ambil(JenisTransaksiKoperasi.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject,"jenisTransaksiKoperasi")));

				Double qty = 1.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final Combobox jenisTranskasi = new Combobox();
				Common.insertCombo(jenisTranskasi, "nama", "keterangan", JenisTransaksiKoperasi.class,
						Restrictions.eq("aktif", true));
				Common.selectComboItem(true, jenisTranskasi, jenisTransaksiKoperasi);
				jenisTranskasi.setReadonly(true);
				jenisTranskasi.setWidth("95%");

				final Label nilai = new Label(Common.numberFormat.get().format(jumlah));

				final MyDoublebox qtyBox = new MyDoublebox(qty);
				final MyDoublebox hargaBox = new MyDoublebox(harga);

				qtyBox.setWidth("95%");
				hargaBox.setWidth("95%");

				if (persetujuan) {
					row.appendChild(new Label(jenisTransaksiKoperasi == null ? "" : jenisTransaksiKoperasi.getNama()));
					row.appendChild(new Label(Common.numberFormat.get().format(qty)));
					row.appendChild(new Label(Common.numberFormat.get().format(harga)));

				} else {
					row.appendChild(jenisTranskasi);
					row.appendChild(qtyBox);
					row.appendChild(hargaBox);

				}

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) (jenisTranskasi
								.getSelectedItem() == null ? null : jenisTranskasi.getSelectedItem().getValue());

						jsonObject.put("jenisTransaksiKoperasi",
								jenisTransaksiKoperasi == null ? null : jenisTransaksiKoperasi.getId());
						jsonObject.put("qty", qtyBox.getValue());
						jsonObject.put("harga", hargaBox.getValue());

						Double jumlah = (qtyBox.getValue() == null ? 0.0 : qtyBox.getValue())
								* (hargaBox.getValue() == null ? 0.0 : hargaBox.getValue());
						jsonObject.put("jumlah", jumlah);
						nilai.setValue(Common.numberFormat.get().format(jumlah));

						hitungTotal.onEvent(null);
					}
				};

				jenisTranskasi.addEventListener("onChange", eventListener);

				qtyBox.addEventListener("onChange", eventListener);
				hargaBox.addEventListener("onChange", eventListener);

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

												reloadDataFormula(rowU, array, persetujuan);

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

				if (persetujuan) {
					new Label().setParent(row);
				} else {
					button.setParent(row);
				}
			}
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new ProdukKoperasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		produkKoperasi = (ProdukKoperasi) obj;
		init(produkKoperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final ProdukKoperasi produkKoperasi) throws Exception {
		this.produkKoperasi = produkKoperasi;
		addWindow.setTitle(produkKoperasi.getId() == null ? "Tambah Produk Koperasi" : "Ubah Produk Koperasi");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koperasi *"));
		row.appendChild(koperasi = new Combobox());
		myKoperasi = Common.getCurrentKoperasi();
		Common.insertCombo(koperasi, "nama", Koperasi.class, Restrictions.eq("aktif", true));

		if (myKoperasi != null) {
			koperasi.setDisabled(true);
			Common.selectComboItem(true, koperasi, myKoperasi);
		}

		if (produkKoperasi.getKoperasi() != null) {
			koperasi.setDisabled(true);
			Common.selectComboItem(true, koperasi, produkKoperasi.getKoperasi());
		}
		koperasi.setWidth("90%");
		koperasi.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Produk Koperasi *"));
		row.appendChild(tipeProdukKoperasi = new Combobox());
		Common.insertCombo(tipeProdukKoperasi, "nama", "keterangan", TipeProdukKoperasi.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(true, tipeProdukKoperasi, produkKoperasi.getTipeProdukKoperasi());
		tipeProdukKoperasi.setWidth("90%");
		tipeProdukKoperasi.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Produk Koperasi"));
		row.appendChild(kode = new Textbox(produkKoperasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Produk Koperasi *"));
		row.appendChild(nama = new Textbox(produkKoperasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Agenda"));
		row.appendChild(nomorSurat = new AmbilDataNomorSuratBanbox());
		nomorSurat.setAttribute("nomorSurat", produkKoperasi.getNomorSurat());
		nomorSurat.setValue(produkKoperasi.getNomorSurat() == null ? "" : produkKoperasi.getNomorSurat().getNama());
		nomorSurat.setWidth("90%");
		nomorSurat.setReadonly(true);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hitungShu = new MyCheckboxConfig("Hitung SHU (Sisa Hasil Usaha)"));
		hitungShu.setChecked(produkKoperasi.getHitungShu());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(otomatisTerbentukTransaksi = new MyCheckboxConfig("Otomatis Terbentuk Transaksi"));
		otomatisTerbentukTransaksi.setChecked(produkKoperasi.getOtomatisTerbentukTransaksi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Transaksi Terbentuk"));
		row.appendChild(jumlahTransaksiTerbentuk = new MyIntbox(produkKoperasi.getJumlahTransaksiTerbentuk()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaBolehSekaliTransaksi = new MyCheckboxConfig("Hanya Boleh Sekali Transaksi"));
		hanyaBolehSekaliTransaksi.setChecked(produkKoperasi.getHanyaBolehSekaliTransaksi());

		setoran = 0.0;
		rowDetail = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowDetail, "2");
		rowDetail.setParent(rows);

		EventListener eventListenerDetail = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowDetail);
				array = new JSONArray(produkKoperasi.getFormula());
				rowFormula = Common.tampilanScroll1(rowDetail);

				reloadFormula(rowFormula, array, false);
			}
		};

		try {
			eventListenerDetail.onEvent(null);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		final MyCheckboxConfig formulirVerifikasi;
		row.appendChild(formulirVerifikasi = new MyCheckboxConfig("Syarat Kelengkapan Berkas"));
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		subColumns = new Columns();
		subColumns.setParent(subGrid);
		c = new Column("Verifikasi Syarat");
		subColumns.appendChild(c);

		subRows = new Rows();
		subRows.setParent(subGrid);

		subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Session session = HibernateUtil.currentSession();

		@SuppressWarnings("unchecked")
		List<SyaratProdukKoperasi> syaratProdukKoperasis = ConstantValues.simpleList(
				session.createCriteria(SyaratProdukKoperasi.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				SyaratProdukKoperasi.class);

		if (produkKoperasi.getCopyDari() != null && produkKoperasi.getCopyDari().getId() != null) {

			ProdukKoperasi gelombangPendaftaranCopy = (ProdukKoperasi) produkKoperasi.getCopyDari();
			session.refresh(gelombangPendaftaranCopy);
			selectedSyaratProdukKoperasi = new HashSet<SyaratProdukKoperasi>();
			for (SyaratProdukKoperasi kelengkapanCalonMahasiswa : gelombangPendaftaranCopy.getSyaratProdukKoperasis()) {
				selectedSyaratProdukKoperasi.add(kelengkapanCalonMahasiswa);
			}

		} else {
			if (produkKoperasi.getId() != null) {
				session.refresh(produkKoperasi);
			}
			selectedSyaratProdukKoperasi = produkKoperasi.getSyaratProdukKoperasis();

		}

		ids = new HashSet<Long>();
		for (SyaratProdukKoperasi v : selectedSyaratProdukKoperasi) {
			ids.add(v.getId());
		}

		System.out.println("ids ->" + ids);

		subGrid.setVisible(!selectedSyaratProdukKoperasi.isEmpty());
		formulirVerifikasi.setChecked(!selectedSyaratProdukKoperasi.isEmpty());

		formulirVerifikasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				subGrid.setVisible(formulirVerifikasi.isChecked());
			}
		});

		vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final SyaratProdukKoperasi syaratProdukKoperasi : syaratProdukKoperasis) {
			final Checkbox checkbox = new Checkbox(syaratProdukKoperasi.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(syaratProdukKoperasi.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedSyaratProdukKoperasi.add(syaratProdukKoperasi);
					} else {
						for (SyaratProdukKoperasi a : selectedSyaratProdukKoperasi) {
							if (a.getId().equals(syaratProdukKoperasi.getId())) {
								selectedSyaratProdukKoperasi.remove(a);
								break;
							}
						}
					}

					System.out.println("selectedSyaratProdukKoperasi => " + selectedSyaratProdukKoperasi);
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Margin (Persen)"));
		row.appendChild(bunga = new MyDoublebox(produkKoperasi.getBunga()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode Bunga Pinjaman"));
		row.appendChild(metodeBunga = new Combobox());
		Comboitem miMetode = new Comboitem("Flat (Tetap)");
		miMetode.setValue(ProdukKoperasi.METODE_FLAT);
		metodeBunga.appendChild(miMetode);
		miMetode = new Comboitem("Menurun (Efektif)");
		miMetode.setValue(ProdukKoperasi.METODE_MENURUN);
		metodeBunga.appendChild(miMetode);
		miMetode = new Comboitem("Anuitas");
		miMetode.setValue(ProdukKoperasi.METODE_ANUITAS);
		metodeBunga.appendChild(miMetode);
		Common.selectComboItem(metodeBunga, produkKoperasi.getMetodeBunga());
		metodeBunga.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengitungan Margin *"));
		row.appendChild(penghitunganBunga = new Combobox());
		Comboitem comboitem = new Comboitem(ProdukKoperasi.BULANAN);
		comboitem.setValue(ProdukKoperasi.BULANAN);
		penghitunganBunga.appendChild(comboitem);
		comboitem = new Comboitem(ProdukKoperasi.TAHUNAN);
		comboitem.setValue(ProdukKoperasi.TAHUNAN);
		penghitunganBunga.appendChild(comboitem);
		Common.selectComboItem(penghitunganBunga, produkKoperasi.getPenghitunganBunga());
		penghitunganBunga.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(jangkaWaktuBulanLabel = new ais.ui.util.MyLabelConfig("Jangka waktu angsuran (dalam bulan)"));
		row.appendChild(jangkaWaktuBulan = new MyDoublebox(produkKoperasi.getJangkaWaktuBulan()));

		EventListener penghitunganBungaEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String s = (String) (penghitunganBunga.getSelectedItem() == null ? null
						: penghitunganBunga.getSelectedItem().getValue());

				if (s.equalsIgnoreCase(ProdukKoperasi.BULANAN)) {
					jangkaWaktuBulanLabel.setValue("Jangka waktu angsuran (dalam bulan)");
				} else {
					jangkaWaktuBulanLabel.setValue("Jangka waktu angsuran (dalam tahun)");
				}

			}
		};

		penghitunganBunga.addEventListener("onChange", penghitunganBungaEventListener);

		try {
			penghitunganBungaEventListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Durasi *"));
		row.appendChild(durasi = new Combobox());
		comboitem = new Comboitem(ProdukKoperasi.HARIAN);
		comboitem.setValue(ProdukKoperasi.HARIAN);
		durasi.appendChild(comboitem);
		comboitem = new Comboitem(ProdukKoperasi.MINGGUAN);
		comboitem.setValue(ProdukKoperasi.MINGGUAN);
		durasi.appendChild(comboitem);
		comboitem = new Comboitem(ProdukKoperasi.BULANAN);
		comboitem.setValue(ProdukKoperasi.BULANAN);
		durasi.appendChild(comboitem);
		comboitem = new Comboitem(ProdukKoperasi.BULANAN);
		comboitem.setValue(ProdukKoperasi.BULANAN);
		durasi.appendChild(comboitem);
		comboitem = new Comboitem(ProdukKoperasi.TAHUNAN);
		comboitem.setValue(ProdukKoperasi.TAHUNAN);
		durasi.appendChild(comboitem);
		Common.selectComboItem(durasi, produkKoperasi.getDurasi());
		durasi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Minimal"));
		row.appendChild(nilaiMinimal = new MyDoublebox(produkKoperasi.getNilaiMinimal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Maksimal"));
		row.appendChild(nilaiMaksimal = new MyDoublebox(produkKoperasi.getNilaiMaksimal()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode Bunga Simpanan"));
		row.appendChild(metodeBungaSimpanan = new Combobox());
		Comboitem miMetodeSimp = new Comboitem("Saldo Rata-rata");
		miMetodeSimp.setValue(ProdukKoperasi.BUNGA_SIMPANAN_SALDO_RATA_RATA);
		metodeBungaSimpanan.appendChild(miMetodeSimp);
		miMetodeSimp = new Comboitem("Saldo Terendah");
		miMetodeSimp.setValue(ProdukKoperasi.BUNGA_SIMPANAN_SALDO_TERENDAH);
		metodeBungaSimpanan.appendChild(miMetodeSimp);
		miMetodeSimp = new Comboitem("Saldo Harian");
		miMetodeSimp.setValue(ProdukKoperasi.BUNGA_SIMPANAN_SALDO_HARIAN);
		metodeBungaSimpanan.appendChild(miMetodeSimp);
		Common.selectComboItem(metodeBungaSimpanan, produkKoperasi.getMetodeBungaSimpanan());
		metodeBungaSimpanan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bunga Simpanan (Persen/Tahun)"));
		row.appendChild(bungaSimpananPersen = new MyDoublebox(produkKoperasi.getBungaSimpananPersen()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(produkKoperasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		EventListener tipeEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				TipeProdukKoperasi tipeProduk = (TipeProdukKoperasi) (tipeProdukKoperasi.getSelectedItem() == null
						? null
						: tipeProdukKoperasi.getSelectedItem().getValue());

				if (tipeProduk != null && tipeProduk.getId() != null && ConstantValues.SIMPANAN != null
						&& ConstantValues.PINJAMAN != null) {
					otomatisTerbentukTransaksi.getParent()
							.setVisible(tipeProduk.getId().equals(ConstantValues.SIMPANAN.getId()));
					jumlahTransaksiTerbentuk.getParent()
							.setVisible(tipeProduk.getId().equals(ConstantValues.SIMPANAN.getId()));
					hanyaBolehSekaliTransaksi.getParent()
							.setVisible(tipeProduk.getId().equals(ConstantValues.SIMPANAN.getId()));
					metodeBungaSimpanan.getParent()
							.setVisible(tipeProduk.getId().equals(ConstantValues.SIMPANAN.getId()));
					bungaSimpananPersen.getParent()
							.setVisible(tipeProduk.getId().equals(ConstantValues.SIMPANAN.getId()));

					bunga.getParent().setVisible(tipeProduk.getId().equals(ConstantValues.PINJAMAN.getId()));
					penghitunganBunga.getParent()
							.setVisible(tipeProduk.getId().equals(ConstantValues.PINJAMAN.getId()));
					jangkaWaktuBulan.getParent().setVisible(tipeProduk.getId().equals(ConstantValues.PINJAMAN.getId()));
					durasi.getParent().setVisible(tipeProduk.getId().equals(ConstantValues.PINJAMAN.getId()));

				}

			}
		};

		tipeProdukKoperasi.addEventListener("onChange", tipeEventListener);
		tipeEventListener.onEvent(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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

	public boolean onSave(Event event) throws Exception {
		if (koperasi.getSelectedItem() == null || koperasi.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih koperasi dari daftar yang tersedia; (2) pastikan koperasi sudah terdaftar di sistem; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tipeProdukKoperasi.getSelectedItem() == null || tipeProdukKoperasi.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tipe produk koperasi belum dipilih. Langkah yang dapat dilakukan: (1) pilih tipe produk dari daftar yang tersedia; (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nama produk koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Produk dengan nama yang jelas; (2) gunakan nama yang belum terpakai; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (produkKoperasi.getId() != null) {
			produkKoperasi = (ProdukKoperasi) session.load(ProdukKoperasi.class, produkKoperasi.getId());

		}

		setoran = 0.0;
		for (int i = 0; i < array.length(); i++) {
			Double jumlah = 0.0;
			JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}
				setoran += jumlah;
			}
		}
		produkKoperasi.setSetoran(setoran);
		produkKoperasi.setKode(kode.getValue());
		produkKoperasi.setNama(nama.getValue());
		produkKoperasi.setKoperasi((Koperasi) koperasi.getSelectedItem().getValue());
		produkKoperasi.setTipeProdukKoperasi((TipeProdukKoperasi) tipeProdukKoperasi.getSelectedItem().getValue());
		produkKoperasi.setKeterangan(keterangan.getValue());
		produkKoperasi.setHitungShu(hitungShu.isChecked());
		produkKoperasi.setSyaratProdukKoperasis(selectedSyaratProdukKoperasi);
		produkKoperasi.setNomorSurat((NomorSurat) nomorSurat.getAttribute("nomorSurat"));
		produkKoperasi.setFormula(array.toString());
		produkKoperasi.setBunga(bunga.getValue());
		produkKoperasi
				.setDurasi((String) (durasi.getSelectedItem() == null ? null : durasi.getSelectedItem().getValue()));

		produkKoperasi.setPenghitunganBunga((String) (penghitunganBunga.getSelectedItem() == null ? null
				: penghitunganBunga.getSelectedItem().getValue()));

		produkKoperasi.setMetodeBunga((String) (metodeBunga == null || metodeBunga.getSelectedItem() == null
				? ProdukKoperasi.METODE_FLAT
				: metodeBunga.getSelectedItem().getValue()));

		produkKoperasi.setMetodeBungaSimpanan(
				(String) (metodeBungaSimpanan == null || metodeBungaSimpanan.getSelectedItem() == null
						? ProdukKoperasi.BUNGA_SIMPANAN_SALDO_RATA_RATA
						: metodeBungaSimpanan.getSelectedItem().getValue()));
		produkKoperasi.setBungaSimpananPersen(bungaSimpananPersen == null ? null : bungaSimpananPersen.getValue());

		produkKoperasi.setNilaiMaksimal(nilaiMaksimal.getValue());
		produkKoperasi.setNilaiMinimal(nilaiMinimal.getValue());
		produkKoperasi.setJangkaWaktuBulan(jangkaWaktuBulan.getValue());
		produkKoperasi.setJumlahTransaksiTerbentuk(jumlahTransaksiTerbentuk.getValue());
		produkKoperasi.setOtomatisTerbentukTransaksi(otomatisTerbentukTransaksi.isChecked());
		produkKoperasi.setJumlahTransaksiTerbentuk(jumlahTransaksiTerbentuk.getValue());
		produkKoperasi.setHanyaBolehSekaliTransaksi(hanyaBolehSekaliTransaksi.isChecked());

		// Peringatan lunak batas bunga SOM USPK (tidak memblokir simpan).
		String peringatanBunga = periksaBatasBunga(produkKoperasi);

		Common.refreshSaveOrUpdate(session, produkKoperasi);

		if (peringatanBunga != null) {
			MyMessageboxConfig.show(peringatanBunga, "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}

		return true;
	}

	/**
	 * Periksa apakah bunga produk melampaui batas SOM USPK 2025 (pinjaman maksimal 24%/tahun,
	 * simpanan maksimal 9%/tahun) dan kembalikan pesan peringatan bila melampaui, atau {@code null}
	 * bila aman. Bersifat <b>advisory</b> — tidak mencegah penyimpanan, karena penetapan akhir tingkat
	 * bunga merupakan kewenangan Rapat Anggota. Kolom "Margin (Persen)" bersifat per-periode, sehingga
	 * disetahunkan dengan mengalikan 12 bila penghitungannya bulanan.
	 */
	private String periksaBatasBunga(ProdukKoperasi produkKoperasi) {
		try {
			double annual = ProdukKoperasi.TAHUNAN.equalsIgnoreCase(produkKoperasi.getPenghitunganBunga())
					? produkKoperasi.getBunga()
					: produkKoperasi.getBunga() * 12.0;
			ais.database.model.koperasi.TipeProdukKoperasi tipe = produkKoperasi.getTipeProdukKoperasi();
			Long tipeId = tipe == null ? null : tipe.getId();
			if (tipeId != null && ConstantValues.PINJAMAN != null
					&& tipeId.equals(ConstantValues.PINJAMAN.getId()) && annual > 24.0) {
				return "Perhatian: bunga pinjaman setara ~" + Math.round(annual)
						+ "%/tahun, melebihi batas SOM 24%/tahun. Pastikan sesuai ketetapan Rapat Anggota.";
			}
			double annualSimpanan = produkKoperasi.getBungaSimpananPersen();
			if (tipeId != null && ConstantValues.SIMPANAN != null
					&& tipeId.equals(ConstantValues.SIMPANAN.getId()) && annualSimpanan > 9.0) {
				return "Perhatian: bunga simpanan setara ~" + Math.round(annualSimpanan)
						+ "%/tahun, melebihi batas SOM 9%/tahun. Pastikan sesuai ketetapan Rapat Anggota.";
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return null;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ProdukKoperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ProdukKoperasi> produkKoperasi = ConstantValues.simpleList(
				initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
				ProdukKoperasi.class);
		ListModel strset = new SimpleListModel(produkKoperasi);
		grid.setRowRenderer(new ProdukKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
