package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.DendaPembayaranDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DendaPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DendaPembayaranAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchnama;

	private Intbox mulai;
	private Intbox sampai;
	private Combobox itemBiaya;
	private Combobox jadwalPembayaran;
	private MyDoublebox denda;
	private Combobox dihitungDariItem;

	private boolean edit = false;
	private boolean delete = false;

	private DendaPembayaran dendaPembayaran;
	private MyToolbarbuttonConfig add;

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

	class DendaPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final DendaPembayaran dendaPembayaran = (DendaPembayaran) arg1;

			new Label(dendaPembayaran.getMulai() + " hari").setParent(arg0);
			new Label(dendaPembayaran.getSampai() + " hari").setParent(arg0);

			new Label(dendaPembayaran.getJadwalPembayaran().getTahunAkademik()).setParent(arg0);
			new Label(dendaPembayaran.getJadwalPembayaran().getJenisKegiatan().getNamaKegiatan()).setParent(arg0);
			new Label(dendaPembayaran.getItemBiaya().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(dendaPembayaran.getDenda()) + " %").setParent(arg0);
			new Label(dendaPembayaran.getDihitungDariItem() == null ? "Total"
					: dendaPembayaran.getDihitungDariItem().getNama()).setParent(arg0);
			new Label(
					"Denda pembayaran untuk jadwal pembayaran dari tgl "
							+ (dendaPembayaran.getJadwalPembayaran().getStartDate() == null ? ""
									: Common.dateFormat2.get().format(dendaPembayaran.getJadwalPembayaran().getStartDate()))
							+ " s.d "
							+ (dendaPembayaran.getJadwalPembayaran().getEndDate() == null ? ""
									: Common.dateFormat2.get().format(dendaPembayaran.getJadwalPembayaran().getEndDate()))
							+ " - "
							+ ("Tahun Akademik " + dendaPembayaran.getJadwalPembayaran().getTahunAkademik()
									+ ", semester "
									+ (dendaPembayaran.getJadwalPembayaran().getGanjil() ? "Ganjil" : "Genap")))
											.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(dendaPembayaran);
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

											Common.refreshDelete(dendaPembayaran);

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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new DendaPembayaran());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private void init(DendaPembayaran dendaPembayaran) {
		this.dendaPembayaran = dendaPembayaran;
		addWindow.setTitle(dendaPembayaran.getId() == null ? "Tambah DendaPembayaran" : "Ubah DendaPembayaran");
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

		List<JadwalPembayaran> jadwalPembayarans = HibernateUtil.currentSession().createCriteria(JadwalPembayaran.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("id")).list();

		jadwalPembayaran = new Combobox();
		for (JadwalPembayaran jadwalPembayaran : jadwalPembayarans) {
			String desc = jadwalPembayaran.getJenisKegiatan().getNamaKegiatan() + " dari "
					+ (jadwalPembayaran.getStartDate() == null ? ""
							: Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()))
					+ " s.d " + (jadwalPembayaran.getEndDate() == null ? ""
							: Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()));
			desc += " - " + ("Tahun Akademik " + jadwalPembayaran.getTahunAkademik() + ", semester "
					+ (jadwalPembayaran.getGanjil() ? "Ganjil" : "Genap"));

			MyComboitemConfig comboitem = new MyComboitemConfig(desc);
			comboitem.setValue(jadwalPembayaran);
			comboitem.setDescription(jadwalPembayaran.getKeterangan());
			this.jadwalPembayaran.appendChild(comboitem);

			comboitem = new MyComboitemConfig(desc);
			comboitem.setValue(jadwalPembayaran);
			comboitem.setDescription(jadwalPembayaran.getKeterangan());
			searchnama.appendChild(comboitem);
		}

		Common.insertCombo(itemBiaya = new Combobox(), "nama", ItemBiaya.class);
		Common.selectComboItem(itemBiaya, ConstantValues.DENDA);
		itemBiaya.setDisabled(true);

		Common.insertCombo(dihitungDariItem = new Combobox(), "nama", ItemBiaya.class,
				Restrictions.ne("id", ConstantValues.DENDA.getId()));
		Common.selectComboItem(dihitungDariItem, ConstantValues.SPP);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Keterlambatan (hari)"));
		row.appendChild(mulai = new Intbox(dendaPembayaran.getMulai() == null ? 1 : dendaPembayaran.getMulai()));
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai (hari)"));
		row.appendChild(sampai = new Intbox(dendaPembayaran.getSampai() == null ? 1 : dendaPembayaran.getSampai()));
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya (Akun)"));
		row.appendChild(itemBiaya = new Combobox());
		Common.insertCombo(itemBiaya, "nama", ItemBiaya.class);
		itemBiaya.setWidth("90%");
		Common.selectComboItem(itemBiaya, ConstantValues.DENDA);
		itemBiaya.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Denda untuk Jadwal Pembayaran"));
		row.appendChild(jadwalPembayaran);
		Common.selectComboItem(jadwalPembayaran, dendaPembayaran.getJadwalPembayaran());
		jadwalPembayaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persentase Denda (%)"));
		row.appendChild(denda = new MyDoublebox(dendaPembayaran.getDenda() == null ? 0.0 : dendaPembayaran.getDenda()));
		denda.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Presentase dari"));
		row.appendChild(dihitungDariItem);
		Common.selectComboItem(dihitungDariItem, dendaPembayaran.getDihitungDariItem() == null ? ConstantValues.SPP
				: dendaPembayaran.getDihitungDariItem());
		dihitungDariItem.setWidth("90%");

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
		if (mulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Hari mulai",
					"Kolom Hari mulai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Hari mulai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (sampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Hari sampai",
					"Kolom Hari sampai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Hari sampai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (itemBiaya.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Item biaya",
					"Kolom Item biaya belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Item biaya.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (jadwalPembayaran.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jadwal Pembayaran",
					"Kolom Jadwal Pembayaran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jadwal Pembayaran.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (denda.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Denda",
					"Kolom Denda belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Denda.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (dihitungDariItem.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Presentase dari",
					"Kolom Presentase dari belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Presentase dari.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		DendaPembayaranDao dendaPembayaranDao = DaoFactory.getInstance().getDendaPembayaranDao();

		DendaPembayaran count = (DendaPembayaran) dendaPembayaranDao.getCurrentSession()
				.createCriteria(DendaPembayaran.class).add(Restrictions.le("mulai", mulai.getValue().intValue()))
				.add(Restrictions.ge("sampai", mulai.getValue().intValue()))
				.add(Restrictions.eq("jadwalPembayaran", jadwalPembayaran.getSelectedItem().getValue()))
				.add(dendaPembayaran.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", dendaPembayaran.getId()))
				.setMaxResults(1).uniqueResult();

		if (count == null) {
			count = (DendaPembayaran) dendaPembayaranDao.getCurrentSession().createCriteria(DendaPembayaran.class)
					.add(Restrictions.le("mulai", sampai.getValue().intValue()))
					.add(Restrictions.ge("sampai", sampai.getValue().intValue()))
					.add(Restrictions.eq("jadwalPembayaran", jadwalPembayaran.getSelectedItem().getValue()))
					.add(dendaPembayaran.getId() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", dendaPembayaran.getId()))
					.setMaxResults(1).uniqueResult();
		}

		if (count != null) {
			String w = "Denda pembayaran untuk jadwal pembayaran dari tgl "
					+ (count.getJadwalPembayaran().getStartDate() == null ? ""
							: Common.dateFormat2.get().format(count.getJadwalPembayaran().getStartDate()))
					+ " s.d " + (count.getJadwalPembayaran().getEndDate() == null ? ""
							: Common.dateFormat2.get().format(count.getJadwalPembayaran().getEndDate()));

			MyMessageboxConfig.show(
					w + ", mulai " + mulai.getValue() + " hari, sampai " + sampai.getValue()
							+ " hari sudah ada, ganti dengan waktu lain.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (dendaPembayaran.getId() != null) {
			dendaPembayaran = dendaPembayaranDao.load(dendaPembayaran.getId());

		}

		dendaPembayaran.setDenda(denda.getValue());
		dendaPembayaran.setItemBiaya((ItemBiaya) itemBiaya.getSelectedItem().getValue());
		dendaPembayaran.setJadwalPembayaran((JadwalPembayaran) jadwalPembayaran.getSelectedItem().getValue());
		dendaPembayaran.setMulai(mulai.getValue());
		dendaPembayaran.setSampai(sampai.getValue());
		dendaPembayaran.setDihitungDariItem((ItemBiaya) dihitungDariItem.getSelectedItem().getValue());

		if (dendaPembayaran.getId() != null) {
			dendaPembayaranDao.update(dendaPembayaran);
		} else {
			dendaPembayaranDao.save(dendaPembayaran);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DendaPembayaran.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("jadwalPembayaran", searchnama.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<DendaPembayaran> dendaPembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dendaPembayaran);
		grid.setRowRenderer(new DendaPembayaranRenderer());
		grid.setModelCheckMobile(strset);

	}
}
