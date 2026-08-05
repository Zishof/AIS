package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
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
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.JudisiumDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jenjang;
import ais.database.model.Judisium;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JudusiumAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;
	private MyDoublebox nilaiMulai;
	private MyDoublebox nilaiSampai;

	private Intbox masaStudiMaksimal;
	private Combobox statusAwalMahasiswa;

	private Textbox nilaiHurufYangTidakBolehAda;

	private boolean edit = false;
	private boolean delete = false;

	private Judisium judisium;
	private MyToolbarbuttonConfig add;
	private Textbox nilaiHurufYangHarusAda;

	private Intbox minimalSksYangTelahDitempuh;
	private MyDoublebox minimalIpkYangTelahDitempuh;
	private Textbox namaen;
	private Textbox kecualiMk;
	private Combobox jenjang;
	private MyCheckboxConfig termasukMengulang;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

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
	}

	class JudisiumRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Judisium judisium = (Judisium) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Judisium.class, judisium, judisium.getNama())).setParent(arg0);
			a.appendChild(new Label(judisium.getNamaen()));
			new Label(judisium.getNilaiMulai() == null ? "" : Common.numberFormat.get().format(judisium.getNilaiMulai()))
					.setParent(arg0);
			new Label(judisium.getNilaiSampai() == null ? "" : Common.numberFormat.get().format(judisium.getNilaiSampai()))
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(judisium.getMasaStudiMaksimal()) + " smt").setParent(arg0);
			new Label(judisium.getNilaiHurufYangTidakBolehAda()).setParent(arg0);
			new Label(judisium.getNilaiHurufYangHarusAda()).setParent(arg0);
			new Label(judisium.getStatusAwalMahasiswa() == null ? "Semua" : judisium.getStatusAwalMahasiswa().getNama())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(judisium.getMinimalSksYangTelahDitempuh())).setParent(arg0);
			new Label(Common.numberFormat.get().format(judisium.getMinimalIpkYangTelahDitempuh())).setParent(arg0);
			new Label(judisium.getJenjang() == null ? "" : judisium.getJenjang().getNama()).setParent(arg0);
			new Label(judisium.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(judisium.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					judisium.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(judisium);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(judisium);
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
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(judisium);

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
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Judisium());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Judisium judisium) throws Exception {
		this.judisium = judisium;
		addWindow.setTitle(judisium.getId() == null ? "Tambah Judisium" : "Ubah Judisium");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Judisium *"));
		row.appendChild(nama = new Textbox(judisium.getNama() == null ? "" : judisium.getNama()));
		nama.setRows(2);
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Judisium English"));
		row.appendChild(namaen = new Textbox(judisium.getNamaen()));
		namaen.setRows(2);
		namaen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Mulai"));
		row.appendChild(nilaiMulai = new MyDoublebox(judisium.getNilaiMulai()));
		nilaiMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Sampai"));
		row.appendChild(nilaiSampai = new MyDoublebox(judisium.getNilaiSampai()));
		nilaiSampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Huruf yang harus ada"));
		row.appendChild(nilaiHurufYangHarusAda = new Textbox(judisium.getNilaiHurufYangHarusAda()));
		nilaiHurufYangHarusAda.setRows(2);
		nilaiHurufYangHarusAda.setWidth("90%");

		Common.initKeterangan(rows,
				"Jika nilai huruf lebih dari satu, pisahkan dengan tanda koma (,), contoh : A,B,C dan seterusnya.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Huruf tidak boleh ada"));
		row.appendChild(nilaiHurufYangTidakBolehAda = new Textbox(judisium.getNilaiHurufYangTidakBolehAda()));
		nilaiHurufYangTidakBolehAda.setRows(2);
		nilaiHurufYangTidakBolehAda.setWidth("90%");

		Common.initKeterangan(rows,
				"Jika nilai huruf lebih dari satu, pisahkan dengan tanda koma (,), contoh : A,B,C dan seterusnya.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jika mk mengulang"));
		row.appendChild(termasukMengulang = new MyCheckboxConfig(
				"Nilai Huruf tidak boleh ada juga berlaku ke nilai yg sebelumnya diulang"));
		termasukMengulang.setChecked(judisium.getTermasukMengulang());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("Pengecualian matakuliah mengulang yang tetap boleh nilai huruf tidak boleh ada"));
		row.appendChild(kecualiMk = new Textbox(judisium.getKecualiMk()));
		kecualiMk.setRows(2);
		kecualiMk.setWidth("90%");

		final Row aa = Common.initKeterangan(rows,
				"Jika pengecualian MK mengulang lebih dari satu, pisahkan dengan tanda semikolon (;), contoh : ;A;B;C; dan seterusnya.");

		EventListener eventListenerMengulang = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				kecualiMk.getParent().setVisible(termasukMengulang.isChecked());
				aa.setVisible(termasukMengulang.isChecked());

			}
		};

		termasukMengulang.addEventListener("onClick", eventListenerMengulang);
		eventListenerMengulang.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Semester / Masa Studi Maksimal"));
		row.appendChild(masaStudiMaksimal = new Intbox(judisium.getMasaStudiMaksimal()));
		masaStudiMaksimal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal mahasiswa"));
		row.appendChild(statusAwalMahasiswa = new Combobox());
		Common.insertComboDanSemua(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa, judisium.getStatusAwalMahasiswa());
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS yang telah ditempuh"));
		row.appendChild(minimalSksYangTelahDitempuh = new Intbox(judisium.getMinimalSksYangTelahDitempuh()));
		minimalSksYangTelahDitempuh.setWidth("90%");

		Common.initKeterangan(rows, "Kosongkan Minimal jumlah SKS yang telah ditempuh jika tidak ada syarat ini.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal IPK yang telah ditempuh"));
		row.appendChild(minimalIpkYangTelahDitempuh = new MyDoublebox(judisium.getMinimalIpkYangTelahDitempuh()));
		minimalIpkYangTelahDitempuh.setWidth("90%");

		Common.initKeterangan(rows, "Kosongkan Minimal nilai IPK yang telah ditempuh jika tidak ada syarat ini.");

		Common.insertComboDanSemua(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.selectComboItem(jenjang, judisium.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");
		jenjang.setReadonly(true);

		Common.initKeterangan(rows, "Kosongkan jenjang jika berlaku semua jenjang.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(judisium.getKeterangan() == null ? "" : judisium.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Judisium",
					"Kolom Nama Judisium belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Judisium.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nilaiMulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nilai Mulai Judisium",
					"Kolom Nilai Mulai Judisium belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nilai Mulai Judisium.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nilaiSampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nilai Sampai Judisium",
					"Kolom Nilai Sampai Judisium belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nilai Sampai Judisium.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		JudisiumDao judisiumDao = DaoFactory.getInstance().getJudisiumDao();
		if (judisium.getId() != null) {
			judisium = judisiumDao.load(judisium.getId());

		}

		judisium.setNilaiMulai(nilaiMulai.getValue());
		judisium.setNilaiSampai(nilaiSampai.getValue());
		judisium.setNama(nama.getValue());
		judisium.setMasaStudiMaksimal(masaStudiMaksimal.getValue());
		judisium.setNilaiHurufYangTidakBolehAda(nilaiHurufYangTidakBolehAda.getValue().trim());
		judisium.setNilaiHurufYangHarusAda(nilaiHurufYangHarusAda.getValue().trim());
		judisium.setKeterangan(keterangan.getValue());
		judisium.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
				: statusAwalMahasiswa.getSelectedItem().getValue()));

		judisium.setMinimalIpkYangTelahDitempuh(minimalIpkYangTelahDitempuh.getValue());
		judisium.setMinimalSksYangTelahDitempuh(minimalSksYangTelahDitempuh.getValue());
		judisium.setNamaen(namaen.getValue());
		judisium.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));

		judisium.setTermasukMengulang(termasukMengulang.isChecked());
		judisium.setKecualiMk(kecualiMk.getValue());

		if (judisium.getId() != null) {
			judisiumDao.update(judisium);
		} else {
			judisiumDao.save(judisium);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Judisium.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Judisium> judisium = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(judisium);
		grid.setRowRenderer(new JudisiumRenderer());
		grid.setModelCheckMobile(strset);

	}

}
