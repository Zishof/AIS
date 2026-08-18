package ais.action.master.pkl;

import java.util.Collections;
import java.util.List;

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
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import ais.ui.util.MyRadioConfig;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.pkl.PersyaratanPkl;

public class PersyaratanPklAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox nama;
	private MyCheckboxConfig harusMenyertakanLampiran;

	private Combobox tipeDataInputan;
	private Textbox labelInputan;
	private MyCheckboxConfig harusDiisi;
	private Radiogroup jenisKelamin;
	private Textbox keterangan;

	private boolean edit = true;
	private boolean delete = true;

	private PersyaratanPkl persyaratanPkl;
	private Textbox nilaiDataInputan;

	// private MyToolbarbuttonConfig add;

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

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class PersyaratanPklRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PersyaratanPkl persyaratanPkl = (PersyaratanPkl) arg1;

			RevisiHelper.createNewRevisi(PersyaratanPkl.class, persyaratanPkl,
					persyaratanPkl.getNama() + (persyaratanPkl.getHarusDiisi() ? " (*)" : "")).setParent(arg0);
			new Label(persyaratanPkl.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);
			new Label(persyaratanPkl.getLabelInputan()).setParent(arg0);
			new Label(persyaratanPkl.getTipeDataInputan()).setParent(arg0);
			new Label(persyaratanPkl.getNilaiDataInputan()).setParent(arg0);
			new Label(persyaratanPkl.getJenisKelamin()).setParent(arg0);
			new Label(persyaratanPkl.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(persyaratanPkl);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {

									Common.refreshDelete(persyaratanPkl);

									onSearchDefault(event);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e); 
									MyMessageboxConfig
											.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
													+ e.getMessage());
								}

							}

						}
					});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PersyaratanPkl());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private void init(PersyaratanPkl persyaratanPkl) {
		this.persyaratanPkl = persyaratanPkl;
		addWindow.setTitle(persyaratanPkl.getId() == null ? "Tambah Persyaratan Pkl" : "Ubah Persyaratan Pkl");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Persyaratan"));
		row.appendChild(nama = new Combobox(persyaratanPkl.getNama() == null ? "" : persyaratanPkl.getNama()));
		nama.setWidth("90%");
		List<String> strings = HibernateUtil.currentSession().createCriteria(PersyaratanPkl.class)
				.setProjection(Projections.groupProperty("nama")).add(Restrictions.ne("nama", "")).list();
		Collections.sort(strings);
		for (String s : strings) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(s);
			comboitem.setValue(s);
			nama.appendChild(comboitem);
		}
		Common.selectComboItem(nama, persyaratanPkl.getNama());
		Common.initKeterangan(rows, "Masukkan nama persyaratan atau pilih salah satu");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harus Menyertakan File Lampiran"));
		row.appendChild(harusMenyertakanLampiran = new MyCheckboxConfig());
		harusMenyertakanLampiran.setChecked(persyaratanPkl.getHarusMenyertakanLampiran());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Data Inputan"));
		row.appendChild(tipeDataInputan = new Combobox());
		tipeDataInputan.setWidth("90%");
		tipeDataInputan.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(PersyaratanPkl.TIDAK_ADA);
		comboitem.setValue(PersyaratanPkl.TIDAK_ADA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanPkl.TEXT);
		comboitem.setValue(PersyaratanPkl.TEXT);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanPkl.ANGKA);
		comboitem.setValue(PersyaratanPkl.ANGKA);
		tipeDataInputan.appendChild(comboitem);
		
		comboitem = new MyComboitemConfig(PersyaratanPkl.TEXT_ANGKA);
		comboitem.setValue(PersyaratanPkl.TEXT_ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanPkl.TANGGAL);
		comboitem.setValue(PersyaratanPkl.TANGGAL);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanPkl.PILIHAN_YA_TIDAK);
		comboitem.setValue(PersyaratanPkl.PILIHAN_YA_TIDAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanPkl.PILIHAN_CUSTOM);
		comboitem.setValue(PersyaratanPkl.PILIHAN_CUSTOM);
		tipeDataInputan.appendChild(comboitem);

		Common.selectComboItem(tipeDataInputan, persyaratanPkl.getTipeDataInputan());
		if (tipeDataInputan.getSelectedItem() == null) {
			tipeDataInputan.setSelectedIndex(0);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Label inputan (jika terdapat data yang wajib diinput)"));
		row.appendChild(labelInputan = new Textbox(persyaratanPkl.getLabelInputan()));
		labelInputan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Data Inputan (jika \"Tipe Data Inputan\" berupa pilihan custom)"));
		row.appendChild(nilaiDataInputan = new Textbox(persyaratanPkl.getNilaiDataInputan()));
		nilaiDataInputan.setWidth("90%");
		nilaiDataInputan.setRows(3);

		Common.initKeterangan(rows,
				"Input nilai custom harus diberi pemisah semicolon (;) dan untuk skor dipisah dengan kolon (:), skor harus berupa angka desimal, contoh : Ya:1;Tidak:0;Belum Tau:2");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persyataran ini berlaku untuk jenis kelamin"));
		jenisKelamin = new Radiogroup();
		MyRadioConfig radio = new MyRadioConfig();
		radio.setLabel("Semua");
		radio.setValue("");
		jenisKelamin.appendChild(radio);
		if (persyaratanPkl.getJenisKelamin().equalsIgnoreCase("")) {
			radio.setSelected(true);
		}

		radio = new MyRadioConfig();
		radio.setLabel("Laki-laki");
		radio.setValue("Laki-laki");
		jenisKelamin.appendChild(radio);
		if (persyaratanPkl.getJenisKelamin().equalsIgnoreCase("Laki-laki")) {
			radio.setSelected(true);
		}

		radio = new MyRadioConfig();
		radio.setLabel("Perempuan");
		radio.setValue("Perempuan");
		jenisKelamin.appendChild(radio);
		if (persyaratanPkl.getJenisKelamin().equalsIgnoreCase("Perempuan")) {
			radio.setSelected(true);
		}
		row.appendChild(jenisKelamin);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Harus Diisi"));
		row.appendChild(harusDiisi = new MyCheckboxConfig());
		harusDiisi.setChecked(persyaratanPkl.getHarusDiisi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(persyaratanPkl.getKeterangan() == null ? "" : persyaratanPkl.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Persyaratan PKL belum diisi. Langkah yang dapat dilakukan: (1) Ketikkan atau pilih nama persyaratan pada kolom \"Nama Persyaratan\"; (2) Pastikan nama tidak hanya berisi spasi; (3) Klik Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (persyaratanPkl.getId() != null) {
			persyaratanPkl = (PersyaratanPkl) session.load(PersyaratanPkl.class, persyaratanPkl.getId());

		}

		persyaratanPkl.setJenisKelamin(
				jenisKelamin.getSelectedItem() == null ||  jenisKelamin.getSelectedItem().getValue() == null ? "" : jenisKelamin.getSelectedItem().getValue().toString());
		persyaratanPkl.setNilaiDataInputan(nilaiDataInputan.getValue());
		persyaratanPkl.setNama(nama.getValue().trim());
		persyaratanPkl.setHarusMenyertakanLampiran(harusMenyertakanLampiran.isChecked());
		persyaratanPkl.setTipeDataInputan((String) tipeDataInputan.getSelectedItem().getValue());
		persyaratanPkl.setLabelInputan(labelInputan.getValue());
		persyaratanPkl.setHarusDiisi(harusDiisi.isChecked());
		persyaratanPkl.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, persyaratanPkl);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PersyaratanPkl.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PersyaratanPkl> persyaratanPkl = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(persyaratanPkl);
		grid.setRowRenderer(new PersyaratanPklRenderer());
		grid.setModelCheckMobile(strset);

	}

	

}
