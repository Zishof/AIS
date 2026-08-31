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

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.RekeningDosenDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.Dosen;
import ais.database.model.RekeningDosen;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk rekening dosen. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchbank}, {@code AmbilDataDosenBanbox searchdosen}, {@code
 * Textbox noRekening}, {@code RekeningDosen rekeningDosen}, {@code Combobox bank}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class RekeningDosenAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchbank;
	private AmbilDataDosenBanbox searchdosen;

	private Textbox noRekening;

	private RekeningDosen rekeningDosen;

	private Combobox bank;
	private Combobox dosen;
	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		add.setVisible(CommonPrivilages
				.checkPrevilages(CommonPrivilages.CREATE));
		if (add != null) { add.setTooltiptext("Tambah"); }

		Common.insertCombo(bank = new Combobox(), "nama", Bank.class);
		Common.insertCombo(searchbank, "nama", Bank.class);
		Common.insertCombo(dosen = new Combobox(), "nama", Dosen.class);
		// Common.insertCombo(searchdosen, "nama", Dosen.class);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class RekeningDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final RekeningDosen rekeningDosen = (RekeningDosen) arg1;

			new Label(rekeningDosen.getNoRekening()).setParent(arg0);
			new Label(rekeningDosen.getDosen().getNama()).setParent(arg0);
			new Label(rekeningDosen.getBank().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(rekeningDosen);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											
											Common.refreshDelete(rekeningDosen);
											
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
		init(new RekeningDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(RekeningDosen rekeningDosen) {
		this.rekeningDosen = rekeningDosen;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Rekening"));
		row.appendChild(noRekening = new Textbox(
				rekeningDosen.getNoRekening() == null ? "" : rekeningDosen
						.getNoRekening()));
		noRekening.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen")));
		Common.selectComboItem(dosen, rekeningDosen.getDosen());
		row.appendChild(dosen);
		dosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bank"));
		Common.selectComboItem(bank, rekeningDosen.getBank());
		row.appendChild(bank);
		bank.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);
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
		if (noRekening.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data No Rekening",
					"Kolom No Rekening belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu No Rekening.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (dosen.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Dosen",
					"Kolom Dosen belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Dosen.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		RekeningDosenDao rekeningDosenDao = DaoFactory.getInstance()
				.getRekeningDosenDao();
		if (rekeningDosen.getId() != null) {
			rekeningDosen = rekeningDosenDao.load(rekeningDosen.getId());
		}
		rekeningDosen.setNoRekening(noRekening.getValue());
		rekeningDosen.setDosen((Dosen) dosen.getSelectedItem().getValue());
		rekeningDosen.setBank((Bank) bank.getSelectedItem().getValue());

		// rekeningDosenDao.beginTransaction();
		if (rekeningDosen.getId() != null) {
			rekeningDosenDao.update(rekeningDosen);
		} else {
			rekeningDosenDao.save(rekeningDosen);
		}
		// rekeningDosenDao.commitTransaction();
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RekeningDosen.class);
		if (order)
			criteria.addOrder(Order.asc("noRekening"));
		criteria.add(
				searchbank.getSelectedItem() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("bank",
						searchbank.getSelectedItem().getValue())).add((searchdosen == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (
				searchdosen.getAttribute("myValue") == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("dosen",
						searchdosen.getAttribute("myValue"))));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<RekeningDosen> rekeningDosen = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult((paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(rekeningDosen);
		grid.setRowRenderer(new RekeningDosenRenderer());
		grid.setModelCheckMobile(strset);

		

	}
}
