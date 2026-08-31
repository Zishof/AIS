package ais.action.master.kkn;

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
import ais.database.model.kkn.PersyaratanKkn;

/**
 * Controller/action ZK untuk persyaratan kkn. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
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
public class PersyaratanKknAction extends GenericAutowireComposer {

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
	private Radiogroup jenisKelamin;
	private Textbox keterangan;

	private boolean edit = true;
	private boolean delete = true;

	private PersyaratanKkn persyaratanKkn;
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

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class PersyaratanKknRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final PersyaratanKkn persyaratanKkn = (PersyaratanKkn) arg1;

			RevisiHelper.createNewRevisi(PersyaratanKkn.class, persyaratanKkn,
					persyaratanKkn.getNama() + (persyaratanKkn.getHarusDiisi() ? " (*)" : "")).setParent(arg0);
			new Label(persyaratanKkn.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);
			new Label(persyaratanKkn.getLabelInputan()).setParent(arg0);
			new Label(persyaratanKkn.getTipeDataInputan()).setParent(arg0);
			new Label(persyaratanKkn.getNilaiDataInputan()).setParent(arg0);
			new Label(persyaratanKkn.getJenisKelamin()).setParent(arg0);
			new Label(persyaratanKkn.getKeterangan()).setParent(arg0);
			
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");checkbox.setDisabled(!edit);
			checkbox.setChecked(persyaratanKkn.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					persyaratanKkn.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(persyaratanKkn);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(persyaratanKkn);
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

											Common.refreshDelete(persyaratanKkn);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Mohon maaf, data persyaratan KKN ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) pastikan tidak ada KKN atau pendaftar yang masih menggunakan persyaratan ini; (2) hapus terlebih dahulu data yang berelasi; (3) ulangi proses penghapusan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis. Detail error: "
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
		init(new PersyaratanKkn());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private void init(PersyaratanKkn persyaratanKkn) {
		this.persyaratanKkn = persyaratanKkn;
		addWindow.setTitle(persyaratanKkn.getId() == null ? "Tambah Persyaratan Kkn" : "Ubah Persyaratan Kkn");
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
		row.appendChild(nama = new Combobox(persyaratanKkn.getNama() == null ? "" : persyaratanKkn.getNama()));
		nama.setWidth("90%");
		List<String> strings = HibernateUtil.currentSession().createCriteria(PersyaratanKkn.class)
				.setProjection(Projections.groupProperty("nama")).add(Restrictions.ne("nama", "")).list();
		Collections.sort(strings);
		for (String s : strings) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(s);
			comboitem.setValue(s);
			nama.appendChild(comboitem);
		}
		Common.selectComboItem(nama, persyaratanKkn.getNama());
		Common.initKeterangan(rows, "Masukkan nama persyaratan atau pilih salah satu");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harus Menyertakan File Lampiran"));
		row.appendChild(harusMenyertakanLampiran = new MyCheckboxConfig());
		harusMenyertakanLampiran.setChecked(persyaratanKkn.getHarusMenyertakanLampiran());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Data Inputan"));
		row.appendChild(tipeDataInputan = new Combobox());
		tipeDataInputan.setWidth("90%");
		tipeDataInputan.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(PersyaratanKkn.TIDAK_ADA);
		comboitem.setValue(PersyaratanKkn.TIDAK_ADA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanKkn.TEXT);
		comboitem.setValue(PersyaratanKkn.TEXT);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanKkn.ANGKA);
		comboitem.setValue(PersyaratanKkn.ANGKA);
		tipeDataInputan.appendChild(comboitem);
		
		comboitem = new MyComboitemConfig(PersyaratanKkn.TEXT_ANGKA);
		comboitem.setValue(PersyaratanKkn.TEXT_ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanKkn.TANGGAL);
		comboitem.setValue(PersyaratanKkn.TANGGAL);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanKkn.PILIHAN_YA_TIDAK);
		comboitem.setValue(PersyaratanKkn.PILIHAN_YA_TIDAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PersyaratanKkn.PILIHAN_CUSTOM);
		comboitem.setValue(PersyaratanKkn.PILIHAN_CUSTOM);
		tipeDataInputan.appendChild(comboitem);

		Common.selectComboItem(tipeDataInputan, persyaratanKkn.getTipeDataInputan());
		if (tipeDataInputan.getSelectedItem() == null) {
			tipeDataInputan.setSelectedIndex(0);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Label inputan (jika terdapat data yang wajib diinput)"));
		row.appendChild(labelInputan = new Textbox(persyaratanKkn.getLabelInputan()));
		labelInputan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Data Inputan (jika \"Tipe Data Inputan\" berupa pilihan custom)"));
		row.appendChild(nilaiDataInputan = new Textbox(persyaratanKkn.getNilaiDataInputan()));
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
		if (persyaratanKkn.getJenisKelamin().equalsIgnoreCase("")) {
			radio.setSelected(true);
		}

		radio = new MyRadioConfig();
		radio.setLabel("Laki-laki");
		radio.setValue("Laki-laki");
		jenisKelamin.appendChild(radio);
		if (persyaratanKkn.getJenisKelamin().equalsIgnoreCase("Laki-laki")) {
			radio.setSelected(true);
		}

		radio = new MyRadioConfig();
		radio.setLabel("Perempuan");
		radio.setValue("Perempuan");
		jenisKelamin.appendChild(radio);
		if (persyaratanKkn.getJenisKelamin().equalsIgnoreCase("Perempuan")) {
			radio.setSelected(true);
		}
		row.appendChild(jenisKelamin);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Harus Diisi"));
		row.appendChild(harusDiisi = new MyCheckboxConfig());
		harusDiisi.setChecked(persyaratanKkn.getHarusDiisi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(persyaratanKkn.getKeterangan() == null ? "" : persyaratanKkn.getKeterangan()));
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
			MyMessageboxConfig.show("Mohon maaf, nama persyaratan KKN belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Persyaratan dengan nama yang sesuai; (2) pastikan kolom nama tidak dikosongkan; (3) ulangi proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (persyaratanKkn.getId() != null) {
			persyaratanKkn = (PersyaratanKkn) session.load(PersyaratanKkn.class, persyaratanKkn.getId());

		}

		persyaratanKkn.setJenisKelamin(
				jenisKelamin.getSelectedItem() == null ||  jenisKelamin.getSelectedItem().getValue() == null ? "" : jenisKelamin.getSelectedItem().getValue().toString());
		persyaratanKkn.setNilaiDataInputan(nilaiDataInputan.getValue());
		persyaratanKkn.setNama(nama.getValue().trim());
		persyaratanKkn.setHarusMenyertakanLampiran(harusMenyertakanLampiran.isChecked());
		persyaratanKkn.setTipeDataInputan((String) tipeDataInputan.getSelectedItem().getValue());
		persyaratanKkn.setLabelInputan(labelInputan.getValue());
		persyaratanKkn.setHarusDiisi(harusDiisi.isChecked());
		persyaratanKkn.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, persyaratanKkn);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PersyaratanKkn.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PersyaratanKkn> persyaratanKkn = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(persyaratanKkn);
		grid.setRowRenderer(new PersyaratanKknRenderer());
		grid.setModelCheckMobile(strset);

	}

	
}
