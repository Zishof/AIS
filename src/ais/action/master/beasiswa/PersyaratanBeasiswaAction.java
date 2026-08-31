package ais.action.master.beasiswa;

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
import ais.database.dao.DaoFactory;
import ais.database.dao.beasiswa.PersyaratanBeasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.beasiswa.PersyaratanBeasiswa;

/**
 * Controller/action ZK untuk persyaratan beasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox nama}, {@code MyCheckboxConfig
 * harusMenyertakanLampiran}, {@code Combobox tipeDataInputan}, {@code Textbox labelInputan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi
 * domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
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
public class PersyaratanBeasiswaAction extends GenericAutowireComposer {

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
	private Textbox keterangan;

	private boolean edit = true;
	private boolean delete = true;

	private PersyaratanBeasiswa persyaratanBeasiswa;
	private Textbox nilaiDataInputan;
	private MyCheckboxConfig harusDiisi;

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
		// if (session.getAttribute("usersTemp") == null
		// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }
		//
		// add.setVisible(CommonPrivilages
		// .checkPrevilages(CommonPrivilages.CREATE));
		// add.setTooltiptext("Tambah");

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class PersyaratanBeasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PersyaratanBeasiswa persyaratanBeasiswa = (PersyaratanBeasiswa) arg1;

			RevisiHelper
					.createNewRevisi(PersyaratanBeasiswa.class, persyaratanBeasiswa,
							persyaratanBeasiswa.getNama() + (persyaratanBeasiswa.getHarusDiisi() ? " (*)" : ""))
					.setParent(arg0);
			new Label(persyaratanBeasiswa.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);
			new Label(persyaratanBeasiswa.getLabelInputan()).setParent(arg0);
			new Label(persyaratanBeasiswa.getTipeDataInputan()).setParent(arg0);
			new Label(persyaratanBeasiswa.getNilaiDataInputan()).setParent(arg0);
			new Label(persyaratanBeasiswa.getKeterangan()).setParent(arg0);
			
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(persyaratanBeasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					persyaratanBeasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(persyaratanBeasiswa);
				}
			});


			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(persyaratanBeasiswa);
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

											Common.refreshDelete(persyaratanBeasiswa);

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

	public void onAdd(Event event) throws Exception {
		init(new PersyaratanBeasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private void init(PersyaratanBeasiswa persyaratanBeasiswa) {
		this.persyaratanBeasiswa = persyaratanBeasiswa;
		addWindow.setTitle(persyaratanBeasiswa.getId() == null ? "Tambah Persyaratan Beasiswa" : "Ubah Persyaratan Beasiswa");
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
		row.appendChild(
				nama = new Combobox(persyaratanBeasiswa.getNama() == null ? "" : persyaratanBeasiswa.getNama()));
		nama.setWidth("90%");
		List<String> strings = HibernateUtil.currentSession().createCriteria(PersyaratanBeasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.groupProperty("nama")).add(Restrictions.ne("nama", "")).list();
		Collections.sort(strings);
		for (String s : strings) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(s);
			comboitem.setValue(s);
			nama.appendChild(comboitem);
		}
		Common.selectComboItem(nama, persyaratanBeasiswa.getNama());
		Common.initKeterangan(rows, "Masukkan nama persyaratan atau pilih salah satu");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harus Menyertakan File Lampiran"));
		row.appendChild(harusMenyertakanLampiran = new MyCheckboxConfig());
		harusMenyertakanLampiran.setChecked(persyaratanBeasiswa.getHarusMenyertakanLampiran());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Data Inputan"));
		row.appendChild(tipeDataInputan = new Combobox());
		tipeDataInputan.setWidth("90%");
		tipeDataInputan.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(PersyaratanBeasiswa.TIDAK_ADA);
		comboitem.setValue(PersyaratanBeasiswa.TIDAK_ADA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanBeasiswa.TEXT);
		comboitem.setValue(PersyaratanBeasiswa.TEXT);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanBeasiswa.ANGKA);
		comboitem.setValue(PersyaratanBeasiswa.ANGKA);
		tipeDataInputan.appendChild(comboitem);
		
		comboitem = new MyComboitemConfig(PersyaratanBeasiswa.TEXT_ANGKA);
		comboitem.setValue(PersyaratanBeasiswa.TEXT_ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanBeasiswa.TANGGAL);
		comboitem.setValue(PersyaratanBeasiswa.TANGGAL);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanBeasiswa.PILIHAN_YA_TIDAK);
		comboitem.setValue(PersyaratanBeasiswa.PILIHAN_YA_TIDAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanBeasiswa.PILIHAN_CUSTOM);
		comboitem.setValue(PersyaratanBeasiswa.PILIHAN_CUSTOM);
		tipeDataInputan.appendChild(comboitem);

		Common.selectComboItem(tipeDataInputan, persyaratanBeasiswa.getTipeDataInputan());
		if (tipeDataInputan.getSelectedItem() == null) {
			tipeDataInputan.setSelectedIndex(0);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Label inputan (jika terdapat data yang wajib diinput)"));
		row.appendChild(labelInputan = new Textbox(persyaratanBeasiswa.getLabelInputan()));
		labelInputan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Data Inputan (jika \"Tipe Data Inputan\" berupa pilihan custom)"));
		row.appendChild(nilaiDataInputan = new Textbox(persyaratanBeasiswa.getNilaiDataInputan()));
		nilaiDataInputan.setWidth("90%");
		nilaiDataInputan.setRows(3);

		Common.initKeterangan(rows,
				"Input nilai custom harus diberi pemisah semicolon (;) dan untuk skor dipisah dengan kolon (:), skor harus berupa angka desimal, contoh : Ya:1;Tidak:0;Belum Tau:2");


		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Harus Diisi"));
		row.appendChild(harusDiisi = new MyCheckboxConfig());
		harusDiisi.setChecked(persyaratanBeasiswa.getHarusDiisi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				persyaratanBeasiswa.getKeterangan() == null ? "" : persyaratanBeasiswa.getKeterangan()));
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
			MyMessageboxConfig.show("Mohon maaf, Nama Persyaratan belum diisi. Langkah yang dapat dilakukan: (1) Klik kolom Nama Persyaratan dan ketikkan nama persyaratan beasiswa yang sesuai; (2) Pastikan nama tidak hanya berisi spasi; (3) Klik tombol Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		PersyaratanBeasiswaDao pesyaratanBeasiswaDao = DaoFactory.getInstance().getPersyaratanBeasiswaDao();
		if (persyaratanBeasiswa.getId() != null) {
			persyaratanBeasiswa = pesyaratanBeasiswaDao.load(persyaratanBeasiswa.getId());

		}

		persyaratanBeasiswa.setNilaiDataInputan(nilaiDataInputan.getValue());
		persyaratanBeasiswa.setNama(nama.getValue().trim());
		persyaratanBeasiswa.setHarusMenyertakanLampiran(harusMenyertakanLampiran.isChecked());
		persyaratanBeasiswa.setTipeDataInputan((String) tipeDataInputan.getSelectedItem().getValue());
		persyaratanBeasiswa.setLabelInputan(labelInputan.getValue());
		persyaratanBeasiswa.setHarusDiisi(harusDiisi.isChecked());
		persyaratanBeasiswa.setKeterangan(keterangan.getValue());

		if (persyaratanBeasiswa.getId() != null) {
			pesyaratanBeasiswaDao.update(persyaratanBeasiswa);
		} else {
			pesyaratanBeasiswaDao.save(persyaratanBeasiswa);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PersyaratanBeasiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PersyaratanBeasiswa> persyaratanBeasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(persyaratanBeasiswa);
		grid.setRowRenderer(new PersyaratanBeasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	

}
