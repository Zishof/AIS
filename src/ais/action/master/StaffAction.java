package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
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

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.StaffDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jabatan;
import ais.database.model.Jurusan;
import ais.database.model.Staff;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk staff. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox nama}, {@code Textbox nip}, {@code Combobox jabatan}, {@code
 * Combobox fakultas}, {@code Combobox jurusan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code initCriteria()}, {@code init()}); pembacaan/pencarian ({@code onSearchDefault()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class StaffAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2763856241715901311L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox nama;
	private Textbox nip;
	private Combobox jabatan;
	private Combobox fakultas;
	private Combobox jurusan;
	// private Combobox jabatan;

	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private boolean edit = false;
	private boolean delete = false;

	private Staff staff;
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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		jabatan = new Combobox();
		Common.insertCombo(jabatan, "nama", Jabatan.class);

		// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		// comboitem.setLabel(Staff.KAPRODI);
		// comboitem.setValue(Staff.KAPRODI);
		// jabatan.appendChild(comboitem);

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

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Staff.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createCriteria("jurusan", CriteriaSpecification.LEFT_JOIN)
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Staff> staff = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(staff);
		grid.setRowRenderer(new StaffRenderer());
		grid.setModelCheckMobile(strset);

	}

	class StaffRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Staff staff = (Staff) arg1;

			new Label(staff.getNama() == null ? "" : staff.getNama()).setParent(arg0);
			new Label(staff.getNip() == null ? "" : staff.getNip()).setParent(arg0);
			new Label(staff.getJurusan() == null ? "" : staff.getJurusan().getNama()).setParent(arg0);
			new Label(staff.getStaff() == null ? "" : staff.getStaff()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(staff);
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

											Common.refreshDelete(staff);

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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	private void init(Staff staff) {
		this.staff = staff;
		addWindow.setTitle(staff.getId() == null ? "Tambah Staff" : "Ubah Staff");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Staff"));
		row.appendChild(nama = new Textbox(staff.getNama() == null ? "" : staff.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nip"));
		row.appendChild(nip = new Textbox(staff.getNip() == null ? "" : staff.getNip()));
		nip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan"));
		Common.selectComboItem(jabatan, staff.getStaff() == null ? null : staff.getStaff());
		row.appendChild(jabatan);
		jabatan.setWidth("90%");

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		if (Common.getCurrentUser().ambilFakultas() != null) {
			Common.selectComboItem(fakultas, Common.getCurrentUser().ambilFakultas());
			Common.insertCombo(jurusan = new Combobox(), "nama", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", Common.getCurrentUser().ambilFakultas()));
			fakultas.setDisabled(true);
		} else {
			Common.selectComboItem(fakultas, staff.getFakultas() == null ? null : staff.getFakultas());
		}

		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		// fakultas.setDisabled(false);

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, staff.getJurusan() == null ? null : staff.getJurusan());
		if (Common.getCurrentUser().ambilJurusan() != null) {
			Common.pilihJurusan(jurusan, Common.getCurrentUser().ambilJurusan());
			jurusan.setDisabled(true);
		}
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

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
		Jabatan myJabatan = (Jabatan) (jabatan.getSelectedItem() == null ? null : jabatan.getSelectedItem().getValue());

		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (myJabatan == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jabatan",
					"Kolom Jabatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jabatan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		StaffDao staffDao = DaoFactory.getInstance().getStaffDao();
		if (staff.getId() != null) {
			staff = staffDao.load(staff.getId());

		}

		staff.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		staff.setJurusan((Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
				? null : jurusan.getSelectedItem().getValue()));
		staff.setNama(nama.getValue());
		staff.setNip(nip.getValue());
		staff.setStaff(myJabatan.getNama());
		staff.setJabatan(myJabatan);

		if (staff.getId() != null) {
			staffDao.update(staff);
		} else {
			staffDao.save(staff);
		}
		return true;
	}

	public void onAdd(Event event) throws Exception {
		init(new Staff());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
