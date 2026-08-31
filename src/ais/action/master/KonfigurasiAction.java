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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.MemoryCacheUtil;
import ais.common.MemoryDbUtil;
import ais.database.dao.DaoFactory;
import ais.database.dao.KonfigurasiDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk konfigurasi. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchnilai}, {@code Combobox
 * searchta}, {@code Textbox searchinfo}, {@code Textbox nilai}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}, {@code tampilKunci()}); mutasi data ({@code onSave()}); operasi domain lain
 * ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class KonfigurasiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchnilai;
	private Combobox searchta;
	private Textbox searchinfo;

	private Textbox nilai;
	private Combobox nama;
	private Textbox keterangan;
	private Combobox tahunAkademik;
	private Textbox info1;
	private Textbox info2;
	private Textbox info3;
	private Textbox info4;
	private Textbox info5;

	private boolean edit = false;
	private boolean delete = false;

	private Konfigurasi konfigurasi;
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

		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		nama = Common.createComboKonfigurasi(nama);

		searchta = Common.generateTahunAjaranDanSemua(searchta);
		Common.selectComboItem(searchta, null);

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

	/**
	 * Renderer lokal untuk layar/komponen {@link KonfigurasiAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KonfigurasiAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Tbmuser tbmuser}; operasi lokal:
	 * {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KonfigurasiAction
	 */
	class KonfigurasiRenderer extends ais.ui.util.MyRowRenderer {

		private Tbmuser tbmuser = Common.getCurrentUser();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Konfigurasi konfigurasi = (Konfigurasi) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Konfigurasi.class, konfigurasi, konfigurasi.getNama())).setParent(arg0);

			new MyLabelKecil(konfigurasi.getNilai()).setParent(a);

			new Label(konfigurasi.getTahunAkademik()).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new Label(konfigurasi.getInfo1()).setParent(a);
			new Label(konfigurasi.getInfo2()).setParent(a);
			new Label(konfigurasi.getInfo3()).setParent(a);
			new Label(konfigurasi.getInfo4()).setParent(a);
			new Label(konfigurasi.getInfo5()).setParent(a);

			new Label(konfigurasi.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			tampilKunci(toolbar, konfigurasi, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					onSearchDefault(event);
				}

			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit && konfigurasi.getDikunci() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(konfigurasi);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && konfigurasi.getDikunci() == null);
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
											Common.refreshDelete(konfigurasi);
											try {
												MemoryDbUtil.getKonfigurasi().remove(konfigurasi.getNama());
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/KonfigurasiAction.java:186");
												// TODO: handle exception
											}
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
		init(new Konfigurasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Konfigurasi konfigurasi) {
		this.konfigurasi = konfigurasi;
		addWindow.setTitle(konfigurasi.getId() == null ? "Tambah Konfigurasi" : "Ubah Konfigurasi");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Konfigurasi"));
		row.appendChild(nama);
		Common.selectComboItem(nama, konfigurasi.getNama());

		if (nama.getSelectedItem() == null) {
			nama.setValue(konfigurasi.getNama());
		}

		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Konfigurasi"));
		row.appendChild(nilai = new Textbox(konfigurasi.getNilai()));
		nilai.setWidth("90%");
		nilai.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		Common.selectComboItem(tahunAkademik, konfigurasi.getTahunAkademik());
		if (konfigurasi.getTahunAkademik() == null) {
			tahunAkademik.setSelectedItem(null);
		} else if (konfigurasi.getId() != null) {
			tahunAkademik.setDisabled(true);
		}
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Konfigurasi 1"));
		row.appendChild(info1 = new Textbox(konfigurasi.getInfo1() == null ? "" : konfigurasi.getInfo1()));
		info1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Konfigurasi 2"));
		row.appendChild(info2 = new Textbox(konfigurasi.getInfo2() == null ? "" : konfigurasi.getInfo2()));
		info2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Konfigurasi 3"));
		row.appendChild(info3 = new Textbox(konfigurasi.getInfo3() == null ? "" : konfigurasi.getInfo3()));
		info3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Konfigurasi 4"));
		row.appendChild(info4 = new Textbox(konfigurasi.getInfo4() == null ? "" : konfigurasi.getInfo4()));
		info4.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Konfigurasi 5"));
		row.appendChild(info5 = new Textbox(konfigurasi.getInfo5() == null ? "" : konfigurasi.getInfo5()));
		info5.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(konfigurasi.getKeterangan() == null ? "" : konfigurasi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(4);

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
		if (nama.getSelectedItem() == null && nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Konfigurasi",
					"Kolom Jenis Konfigurasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Konfigurasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

//		if (nilai.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Nilai Konfigurasi harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		KonfigurasiDao konfigurasiDao = DaoFactory.getInstance().getKonfigurasiDao();
		if (konfigurasi.getId() != null) {
			konfigurasi = konfigurasiDao.load(konfigurasi.getId());

		}

		konfigurasi.setNama((String) (nama.getSelectedItem() == null ? "" : nama.getSelectedItem().getValue()));

		if (konfigurasi.getNama().trim().equals("")) {
			konfigurasi.setNama(nama.getValue().trim());
		}

		konfigurasi.setKeterangan(keterangan.getValue());
		konfigurasi.setInfo1(info1.getValue().trim());
		konfigurasi.setInfo2(info2.getValue().trim());
		konfigurasi.setInfo3(info3.getValue().trim());
		konfigurasi.setInfo4(info4.getValue().trim());
		konfigurasi.setInfo5(info5.getValue().trim());
		konfigurasi.setNilai((String) nilai.getValue().trim());
		konfigurasi.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? null
						: tahunAkademik.getSelectedItem().getValue()));

		// konfigurasiDao.beginTransaction();
		if (konfigurasi.getId() != null) {
			konfigurasiDao.update(konfigurasi);
		} else {
			konfigurasiDao.save(konfigurasi);
		}
		// konfigurasiDao.commitTransaction();
		MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

		if (MemoryCacheUtil.lokasi_file_temporary_data != null
				&& !MemoryCacheUtil.lokasi_file_temporary_data.trim().isEmpty()) {
			ConstantValues.lokasiFileTemproraryTemp = MemoryCacheUtil.lokasi_file_temporary_data;
		}

		else if (konfigurasi.getNama() != null && konfigurasi.getNama().equals("lokasi_file_temporary_data")) {
			ConstantValues.lokasiFileTemproraryTemp = Common
					.getKonfigurasi("lokasi_file_temporary_data", konfigurasi.getNama()).getNilai();
			ConstantValues.reloadTemp();
//			ConstantValues.panjangLokasiFileTemprorary = ConstantValues.lokasiFileTemprorary.length();
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Konfigurasi.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnilai.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nilai", searchnilai.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchinfo.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("info5", searchinfo.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("info4", searchinfo.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("info3", searchinfo.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("info1", searchinfo.getValue().trim(),
																MatchMode.ANYWHERE),
														Restrictions.ilike("info2", searchinfo.getValue().trim(),
																MatchMode.ANYWHERE)))))

				)

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", searchta.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Konfigurasi> konfigurasi = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Konfigurasi.class);

		ListModel strset = new SimpleListModel(konfigurasi);
		grid.setRowRenderer(new KonfigurasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static void tampilKunci(Component toolbar, final Konfigurasi konfigurasi, Tbmuser tbmuser,
			final EventListener eventListener) {
		final MyToolbarbuttonConfig bukaKunci = new MyToolbarbuttonConfig("Buka", "/img/svg/unlock.svg");
		final MyToolbarbuttonConfig kunci = new MyToolbarbuttonConfig("Kunci", "/img/Lock-Lock-icon.png");

		bukaKunci.setStyle("font-size:11px;");
		kunci.setStyle("font-size:11px;");

		if (tbmuser.getSiswa() == null && konfigurasi != null) {

			toolbar.appendChild(bukaKunci);
			toolbar.appendChild(kunci);

			kunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengunci konfigurasi ini ?.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										konfigurasi.setDikunci(Common.getCurrentUser());
										Common.refreshUpdate(konfigurasi);

										kunci.setVisible(konfigurasi.getDikunci() == null);
										bukaKunci.setVisible(konfigurasi.getDikunci() != null);
										if (konfigurasi.getDikunci() != null) {
											bukaKunci.setLabel(
													"Buka Kunci (" + konfigurasi.getDikunci().getUserNama() + ")");
										}
										MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
										Common.createDefaultTimer(eventListener);
									}

								}
							});
				}
			});

			kunci.setVisible(konfigurasi.getDikunci() == null);
			kunci.setDisabled(!Common.getApakahAdminBolehKunci());

			kunci.setParent(toolbar);
//			kunci.setOrient("vertical");

			bukaKunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membuka kunci konfigurasi ini ?.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										konfigurasi.setDikunci(null);
										Common.refreshUpdate(konfigurasi);

										kunci.setVisible(konfigurasi.getDikunci() == null);
										bukaKunci.setVisible(konfigurasi.getDikunci() != null);
										MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);
										Common.createDefaultTimer(eventListener);
									}

								}
							});
				}
			});
			bukaKunci.setVisible(konfigurasi.getDikunci() != null);
			if (konfigurasi.getDikunci() != null) {
				bukaKunci.setLabel("Buka Kunci (" + konfigurasi.getDikunci().getUserNama() + ")");
			}
			bukaKunci.setDisabled((konfigurasi.getDikunci() != null && Common.getCurrentUser().getUserId() != null
					&& !konfigurasi.getDikunci().getUserId().equals(Common.getCurrentUser().getUserId()))

					|| !Common.getApakahAdminBolehKunci());

			bukaKunci.setParent(toolbar);

			Konfigurasi konfigurasiKunci = Common.getKonfigurasi("kunci_nilai_untuk_admin", Konfigurasi.TIDAK_AKTIF);

			if (konfigurasiKunci.getNilai().equals(Konfigurasi.AKTIF)) {
				if (Common.getCurrentUser().getRoot() != null && Common.getCurrentUser().getRoot()
						&& Common.getCurrentUser() != null && Common.getCurrentUser().hakAkses() != null
						&& Common.getCurrentUser().hakAkses() != null && Common.getCurrentUser() != null
						&& Common.getCurrentUser().hakAkses() != null
						&& Common.getCurrentUser().hakAkses().getRoleId().equals(Tbmrole.ADMINISTRATOR)) {
					bukaKunci.setDisabled(false);
				}
			}

		}
	}
}
