package ais.action.master.koperasi;

import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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

import ais.action.master.asset.LokasiAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KoperasiAction extends GenericAutowireComposer
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

	private Koperasi koperasi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private Combobox fakultas;
	private Combobox jurusan;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private boolean pt;
	private boolean ya;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox lokasi;

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

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

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

		String[] contents = new String[] { "id", "kode", "nama", "fakultas", "jurusan", "yayasan", "sekolah",
				"satuanKerja", "keterangan", "aktif", "lokasi" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Koperasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Koperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Koperasi koperasi = (Koperasi) arg1;
			new Label(koperasi.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Koperasi.class, koperasi, koperasi.getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			vbox.appendChild(new Label(koperasi.getSatuanKerja() == null ? "" : koperasi.getSatuanKerja().getNama()));
			Hbox hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(koperasi.getFakultas() == null ? "" : koperasi.getFakultas().getNama()).setParent(hbox);
			new Label(koperasi.getJurusan() == null ? "" : koperasi.getJurusan().getNama()).setParent(hbox);

			hbox = new Hbox();
			hbox.setParent(vbox);

			new Label(koperasi.getYayasan() == null ? "" : koperasi.getYayasan().getNama()).setParent(hbox);
			new Label(koperasi.getSekolah() == null ? "" : koperasi.getSekolah().getNama()).setParent(hbox);

			new Label(koperasi.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(koperasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					koperasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(koperasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, koperasi, KoperasiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Koperasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		koperasi = (Koperasi) obj;
		init(koperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Koperasi koperasi) throws Exception {
		this.koperasi = koperasi;

		Tbmuser tbmuser = Common.getCurrentUser();
		if (koperasi.getFakultas() == null && tbmuser.ambilFakultas() != null) {
			koperasi.setFakultas(tbmuser.ambilFakultas());
		}

		if (koperasi.getSatuanKerja() == null && tbmuser.ambilSatuanKerja() != null) {
			koperasi.setSatuanKerja(tbmuser.ambilSatuanKerja());
		}

		if (koperasi.getJurusan() == null && tbmuser.ambilJurusan() != null) {
			koperasi.setJurusan(tbmuser.ambilJurusan());
		}

		if (koperasi.getYayasan() == null && tbmuser.ambilYayasan() != null) {
			koperasi.setYayasan(tbmuser.ambilYayasan());
		}

		if (koperasi.getSekolah() == null && tbmuser.ambilSekolah() != null) {
			koperasi.setSekolah(tbmuser.ambilSekolah());
		}

		addWindow.setTitle(koperasi.getId() == null ? "Tambah Koperasi" : "Ubah Koperasi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Koperasi"));
		row.appendChild(kode = new Textbox(koperasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Koperasi *"));
		row.appendChild(nama = new Textbox(koperasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(koperasi.getSatuanKerja() == null ? "" : koperasi.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", koperasi.getSatuanKerja());
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setVisible(pt);
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new MyLabelConfig("Fakultas"));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, koperasi.getFakultas());
		fakultas.setWidth("90%");

		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)), Restrictions.eq(
						"fakultas", koperasi.getFakultas() == null ? tbmuser.ambilFakultas() : koperasi.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setVisible(pt);
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new MyLabelConfig("Jurusan"));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, koperasi.getJurusan());

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				koperasi == null || koperasi.getYayasan() == null ? tbmuser.ambilYayasan() : koperasi.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				koperasi == null || koperasi.getSekolah() == null ? tbmuser.ambilSekolah() : koperasi.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));

		lokasi = new Combobox();

		row.appendChild(lokasi);

		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, koperasi.getLokasi());
		lokasi.setWidth("90%");

		LokasiAction.kunciLokasi(lokasi);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(koperasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, nama koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Koperasi dengan nama lengkap; (2) gunakan nama yang unik dan belum terpakai; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaKoperasi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, nama koperasi sudah ada di database. Langkah yang dapat dilakukan: (1) gunakan nama koperasi lain yang belum terdaftar; (2) cari koperasi yang sudah ada di daftar; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (koperasi.getId() != null) {
			koperasi = (Koperasi) session.load(Koperasi.class, koperasi.getId());

		}

		koperasi.setKode(kode.getValue());
		koperasi.setNama(nama.getValue());
		koperasi.setKeterangan(keterangan.getValue());

		koperasi.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		koperasi.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		koperasi.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		koperasi.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		koperasi.setLokasi((Lokasi) (lokasi.getSelectedItem() == null ? null : lokasi.getSelectedItem().getValue()));

		koperasi.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		Common.refreshSaveOrUpdate(session, koperasi);

		int jmlProduk = ((Number) session.createCriteria(ProdukKoperasi.class)
				.add(Restrictions.eq("tipeProdukKoperasi", ConstantValues.SIMPANAN))
				.setProjection(Projections.rowCount()).setMaxResults(1).uniqueResult()).intValue();

		if (jmlProduk == 0) {
			ProdukKoperasi produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.SIMPANAN);
			produkKoperasi.setNama("Simpanan Wajib");
			produkKoperasi.setSetoran(0.0);
			produkKoperasi.setKode("001");
			session.save(produkKoperasi);

			produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.SIMPANAN);
			produkKoperasi.setNama("Simpanan Pokok");
			produkKoperasi.setSetoran(10000.0);
			produkKoperasi.setKode("002");
			session.save(produkKoperasi);

			produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.SIMPANAN);
			produkKoperasi.setNama("Simpanan Sukarela");
			produkKoperasi.setSetoran(0.0);
			produkKoperasi.setKode("003");
			session.save(produkKoperasi);
			session.flush();
		}

		jmlProduk = ((Number) session.createCriteria(ProdukKoperasi.class)
				.add(Restrictions.eq("tipeProdukKoperasi", ConstantValues.PINJAMAN))
				.setProjection(Projections.rowCount()).setMaxResults(1).uniqueResult()).intValue();

		if (jmlProduk == 0) {

			String formula = "[{\"nama\":\"\",\"harga\":5000000,\"jumlah\":5000000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.REALISASI.getId() + ",\"qty\":1,\"boleh\":true,\"key\":"
					+ Math.abs(Common.randLong())
					+ "},{\"nama\":\"\",\"harga\":50000,\"jumlah\":50000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
					+ Math.abs(Common.randLong()) + "}]";

			ProdukKoperasi produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.PINJAMAN);
			produkKoperasi.setNama("Pinjaman Mikro (1.000.000 – 5.000.000)");
			produkKoperasi.setFormula(formula);
			produkKoperasi.setJangkaWaktuBulan(3.0);
			produkKoperasi.setSetoran(5050000.0);
			produkKoperasi.setBunga(1.0);
			produkKoperasi.setNilaiMinimal(1000000.0);
			produkKoperasi.setNilaiMaksimal(5050000.0);
			produkKoperasi.setKode("011");
			session.save(produkKoperasi);

			formula = "[{\"nama\":\"\",\"harga\":10000000,\"jumlah\": 10000000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.REALISASI.getId() + ",\"qty\":1,\"boleh\":true,\"key\":"
					+ Math.abs(Common.randLong())
					+ "},{\"nama\":\"\",\"harga\":75000,\"jumlah\":75000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
					+ Math.abs(Common.randLong()) + "}]";

			produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setJangkaWaktuBulan(12.0);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.PINJAMAN);
			produkKoperasi.setNama("Pinjaman 1 – 10.000.000");
			produkKoperasi.setSetoran(10075000.0);
			produkKoperasi.setKode("012");
			produkKoperasi.setBunga(1.5);
			produkKoperasi.setNilaiMinimal(1.0);
			produkKoperasi.setNilaiMaksimal(10075000.0);
			session.save(produkKoperasi);

			formula = "[{\"nama\":\"\",\"harga\":25000000,\"jumlah\":25000000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.REALISASI.getId() + ",\"qty\":1,\"boleh\":true,\"key\":"
					+ Math.abs(Common.randLong())
					+ "},{\"nama\":\"\",\"harga\":100000,\"jumlah\":100000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
					+ Math.abs(Common.randLong()) + "}]";

			produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.PINJAMAN);
			produkKoperasi.setNama("Pinjaman 10.000.001 – 25.000.000");
			produkKoperasi.setSetoran(25100000.0);
			produkKoperasi.setJangkaWaktuBulan(24.0);
			produkKoperasi.setKode("013");
			produkKoperasi.setBunga(1.5);
			produkKoperasi.setNilaiMinimal(10000001.0);
			produkKoperasi.setNilaiMaksimal(25100000.0);
			session.save(produkKoperasi);

			formula = "[{\"nama\":\"\",\"harga\":50000000,\"jumlah\":50000000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.REALISASI.getId() + ",\"qty\":1,\"boleh\":true,\"key\":"
					+ Math.abs(Common.randLong())
					+ "},{\"nama\":\"\",\"harga\":125000,\"jumlah\":125000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
					+ Math.abs(Common.randLong()) + "}]";

			produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.PINJAMAN);
			produkKoperasi.setNama("Pinjaman 25.000.001 – 50.000.000");
			produkKoperasi.setJangkaWaktuBulan(36.0);
			produkKoperasi.setSetoran(50125000.0);
			produkKoperasi.setKode("014");
			produkKoperasi.setBunga(1.5);
			produkKoperasi.setNilaiMinimal(25000001.0);
			produkKoperasi.setNilaiMaksimal(50125000.0);
			session.save(produkKoperasi);

			formula = "[{\"nama\":\"\",\"harga\":100000000,\"jumlah\":100000000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.REALISASI.getId() + ",\"qty\":1,\"boleh\":true,\"key\":"
					+ Math.abs(Common.randLong())
					+ "},{\"nama\":\"\",\"harga\":150000,\"jumlah\":150000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
					+ Math.abs(Common.randLong()) + "}]";

			produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.PINJAMAN);
			produkKoperasi.setNama("Pinjaman 50.000.001 – 100.000.000");
			produkKoperasi.setJangkaWaktuBulan(48.0);
			produkKoperasi.setSetoran(100150000.0);
			produkKoperasi.setKode("015");
			produkKoperasi.setBunga(1.5);
			produkKoperasi.setNilaiMinimal(50000001.0);
			produkKoperasi.setNilaiMaksimal(100150000.0);
										    
			session.save(produkKoperasi);

			formula = "[{\"nama\":\"\",\"harga\":150000000,\"jumlah\":150000000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.REALISASI.getId() + ",\"qty\":1,\"boleh\":true,\"key\":"
					+ Math.abs(Common.randLong())
					+ "},{\"nama\":\"\",\"harga\":175000,\"jumlah\":175000,\"jenisTransaksiKoperasi\":"
					+ ConstantValues.BIAYA_ADMIN.getId() + ",\"qty\":1,\"boleh\":false,\"key\":"
					+ Math.abs(Common.randLong()) + "}]";

			produkKoperasi = new ProdukKoperasi();
			produkKoperasi.setKoperasi(koperasi);
			produkKoperasi.setTipeProdukKoperasi(ConstantValues.PINJAMAN);
			produkKoperasi.setNama("Pinjaman 100.000.001 – 150.000.000");
			produkKoperasi.setJangkaWaktuBulan(60.0);
			produkKoperasi.setSetoran(150175000.0);
			produkKoperasi.setKode("016");
			produkKoperasi.setBunga(1.5);
			produkKoperasi.setNilaiMinimal(100000001.0);
			produkKoperasi.setNilaiMaksimal(150175000.0);
			                                
			session.save(produkKoperasi);

			session.flush();
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Koperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Koperasi> koperasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(koperasi);
		grid.setRowRenderer(new KoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKoperasi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Koperasi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.koperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.koperasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
