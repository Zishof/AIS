package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
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
import ais.action.master.helper.AmbilDataPerkuliahanBandbox;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PesanRuanganDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Gedung;
import ais.database.model.Perkuliahan;
import ais.database.model.PesanRuangan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk pesan ruangan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataRuangBanbox searchruang}, {@code Textbox searchtujuan}, {@code
 * Textbox searchnama}, {@code Textbox searchkodeRuangan}, {@code Textbox searchkapasitasruangan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); validasi/perhitungan ({@code
 * checkPemesanan()}, {@code checkPemakaianPerkuliahan()}); mutasi data ({@code onSave()}, {@code
 * saveProcess()}); operasi domain lain ({@code onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class PesanRuanganAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataRuangBanbox searchruang;
	private Textbox searchtujuan;
	private Textbox searchnama;
	private Textbox searchkodeRuangan;
	private Textbox searchkapasitasruangan;
	private Combobox searchfakultas;
	private Combobox searchgedung;
	private Combobox searchTahunAjaran;
	private Combobox searchsemester;

	private Textbox tujuan;
	private Textbox keterangan;
	private AmbilDataRuangBanbox ruang;
	private AmbilDataPerkuliahanBandbox perkuliahan;
	private AmbilDataDosenBanbox dosen;
	private Combobox tahunAkademik;
	private Combobox jenisSemester;

	private MyDatebox mulai;
	private MyDatebox sampai;

	// private boolean edit = false;
	// private boolean delete = false;

	private PesanRuangan pesanRuangan;
	private MyToolbarbuttonConfig add;

	private Ruang selectedRuang;

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");
	private EventListener eventListener;

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

		if (session.getAttribute("selectedRuang1") != null) {
			selectedRuang = (Ruang) session.getAttribute("selectedRuang1");
			session.removeAttribute("selectedRuang1");
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		if (add != null) { add.setVisible(tbmuser.getMahasiswa() == null && CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE)); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchsemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchsemester.appendChild(comboitem);

		Common.selectComboItem(searchsemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class);

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.insertCombo(searchgedung, "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (selectedRuang != null) {
			searchruang.setValue(selectedRuang.getNama());
			searchruang.setAttribute("ruang", selectedRuang);
			// searchruang.setDisabled(true);
		}

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		if (Common.getCurrentUser().ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, Common.getCurrentUser().ambilFakultas());
			searchfakultas.setDisabled(true);
		} else {
			searchfakultas.setDisabled(false);
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class PesanRuanganRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PesanRuangan pesanRuangan = (PesanRuangan) arg1;

			RevisiHelper.createNewRevisi(PesanRuangan.class, pesanRuangan, pesanRuangan.getTujuan()).setParent(arg0);

			new Label(pesanRuangan.getRuang() == null ? "" : pesanRuangan.getRuang().getNama()).setParent(arg0);

			new Label(pesanRuangan.getPerkuliahan() == null ? ""
					: Common.getDeskripsiPerkuliahan(pesanRuangan.getPerkuliahan())).setParent(arg0);

			new Label(pesanRuangan.getDosen() == null ? "" : pesanRuangan.getDosen().getNama()).setParent(arg0);

			new Label(pesanRuangan.getDipesanOleh() == null ? "" : pesanRuangan.getDipesanOleh().getUserId())
					.setParent(arg0);

			new Label(pesanRuangan.getMulai() == null ? "" : Common.dateFormat5.get().format(pesanRuangan.getMulai()))
					.setParent(arg0);

			new Label(pesanRuangan.getSampai() == null ? "" : Common.dateFormat5.get().format(pesanRuangan.getSampai()))
					.setParent(arg0);

			new Label(pesanRuangan.getTahunAkademik()).setParent(arg0);
			new Label(pesanRuangan.getJenisSemester()).setParent(arg0);
			new Label(pesanRuangan.getKeterangan()).setParent(arg0);

			Tbmuser tbmuser = Common.getCurrentUser();
			Hbox toolbar = new Hbox();
			toolbar.setVisible(tbmuser.getMahasiswa() == null);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");

			button.setVisible(Common.getApakahAdminLain(tbmuser) || (tbmuser.getUserId() != null
					&& tbmuser.getUserId().equals(pesanRuangan.getDipesanOleh().getUserId())));

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pesanRuangan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");

			button.setTooltiptext("Hapus Data");
			button.setVisible(Common.getApakahAdminLain(tbmuser) || (tbmuser.getUserId() != null
					&& tbmuser.getUserId().equals(pesanRuangan.getDipesanOleh().getUserId())));

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(pesanRuangan);

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

	public static void onAddExternal(Event event, EventListener eventListener, PesanRuangan pesanRuangan)
			throws Exception {
		PesanRuanganAction pesanRuanganAction = new PesanRuanganAction();
		pesanRuanganAction.eventListener = eventListener;
		pesanRuanganAction.addWindow = new MyWindow();
		pesanRuanganAction.selectedRuang = pesanRuangan.getRuang();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(pesanRuanganAction.addWindow);
		pesanRuanganAction.addWindow.setHeight("470px");
		pesanRuanganAction.addWindow.setWidth("850px");

		pesanRuanganAction.init(pesanRuangan);

		pesanRuanganAction.addWindow.setVisible(true);
		pesanRuanganAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new PesanRuangan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(PesanRuangan pesanRuangan) {
		this.pesanRuangan = pesanRuangan;
		addWindow.setTitle("Pesan Ruangan");

		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);

		Common.selectComboItem(jenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tujuan Pesan Ruangan"));
		row.appendChild(tujuan = new Textbox(pesanRuangan.getTujuan() == null ? "" : pesanRuangan.getTujuan()));
		tujuan.setWidth("90%");
		tujuan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang dipesan"));
		row.appendChild(ruang = new AmbilDataRuangBanbox());
		ruang.setWidth("90%");
		ruang.setValue(pesanRuangan.getRuang() == null ? "" : pesanRuangan.getRuang().getNama());
		ruang.setAttribute("ruang", pesanRuangan.getRuang());

		if (selectedRuang != null) {
			ruang.setValue(selectedRuang.getNama());
			ruang.setAttribute("ruang", selectedRuang);
			// ruang.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dipesan mulai"));
		row.appendChild(mulai = new MyDatebox(pesanRuangan.getMulai()));
		mulai.setFormat(Common.dateFormat.get().toPattern());
		mulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dipesan sampai"));
		row.appendChild(sampai = new MyDatebox(pesanRuangan.getSampai()));
		sampai.setFormat(Common.dateFormat.get().toPattern());
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perkuliahan"));
		row.appendChild(perkuliahan = new AmbilDataPerkuliahanBandbox());
		perkuliahan.setWidth("90%");
		perkuliahan.setValue(pesanRuangan.getPerkuliahan() == null ? ""
				: Common.getDeskripsiPerkuliahan(pesanRuangan.getPerkuliahan()));
		perkuliahan.setAttribute("perkuliahan", pesanRuangan.getPerkuliahan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dipesan oleh dosen"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");
		dosen.setValue(pesanRuangan.getDosen() == null
				? (Common.getCurrentUser().getDosen() == null ? null : Common.getCurrentUser().getDosen().getNama())
				: pesanRuangan.getDosen().getNama());
		dosen.setAttribute("dosen",
				pesanRuangan.getDosen() == null
						? (Common.getCurrentUser().getDosen() == null ? null : Common.getCurrentUser().getDosen())
						: pesanRuangan.getDosen());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik);
		if (pesanRuangan.getTahunAkademik() != null)
			Common.selectComboItem(tahunAkademik, pesanRuangan.getTahunAkademik());
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		if (pesanRuangan.getJenisSemester() != null)
			Common.selectComboItem(jenisSemester, pesanRuangan.getJenisSemester());
		jenisSemester.setWidth("90%");

		perkuliahan.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Perkuliahan perkuliahan = (Perkuliahan) PesanRuanganAction.this.perkuliahan.getAttribute("perkuliahan");
				dosen.setValue(
						perkuliahan.getDosen1() == null
								? (Common.getCurrentUser().getDosen() == null ? null
										: Common.getCurrentUser().getDosen().getNama())
								: perkuliahan.getDosen1().getNama());
				dosen.setAttribute("dosen", perkuliahan.getDosen1() == null
						? (Common.getCurrentUser().getDosen() == null ? null : Common.getCurrentUser().getDosen())
						: perkuliahan.getDosen1());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dipesan oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getCurrentUser().getUserId()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(pesanRuangan.getKeterangan() == null ? "" : pesanRuangan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
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

					if (eventListener != null) {
						eventListener.onEvent(new Event("", addWindow, PesanRuanganAction.this.pesanRuangan));
					}

					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (tujuan.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tujuan Pesan Ruangan",
					"Kolom Tujuan Pesan Ruangan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tujuan Pesan Ruangan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (ruang.getAttribute("ruang") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ruangan",
					"Kolom Ruangan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Ruangan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (mulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Mulai Pesan Ruangan",
					"Kolom Mulai Pesan Ruangan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Mulai Pesan Ruangan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (sampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Sampai Pesan Ruangan",
					"Kolom Sampai Pesan Ruangan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Sampai Pesan Ruangan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		PesanRuangan pesanRuangan = checkPemesanan();
		if (pesanRuangan != null) {
			MyMessageboxConfig.show(
					"Ruangan " + pesanRuangan.getRuang().getNama() + " sudah dipesan mulai dari "
							+ Common.dateFormat5.get().format(pesanRuangan.getMulai()) + " sampai "
							+ Common.dateFormat5.get().format(pesanRuangan.getSampai()) + "\nInfo detail : \n" + pesanRuangan,
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Perkuliahan myPerkuliahan = checkPemakaianPerkuliahan();
		if (myPerkuliahan != null) {
			MyMessageboxConfig.show(
					"Ruangan sudah terpakai sesuai dengan jadwal perkuliahan yang ada, yaitu:\n"
							+ Common.getDeskripsiPerkuliahan(myPerkuliahan)
							+ "\n\nApakah anda tetap ingin menggunakan ruangan ini ?",
					"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = new Integer(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								saveProcess();
								onSearchDefault(event);
							}

						}

					});
			return true;
		} else {
			return saveProcess();
		}

	}

	private boolean saveProcess() {
		PesanRuanganDao pesanRuanganDao = DaoFactory.getInstance().getPesanRuanganDao();
		if (pesanRuangan.getId() != null) {
			pesanRuangan = pesanRuanganDao.load(pesanRuangan.getId());
		}

		pesanRuangan.setTujuan(tujuan.getValue());
		pesanRuangan.setKeterangan(keterangan.getValue());
		pesanRuangan.setDipesanOleh(Common.getCurrentUser());
		pesanRuangan.setDosen((Dosen) dosen.getAttribute("dosen"));
		pesanRuangan.setMulai(mulai.getValue());
		pesanRuangan.setPerkuliahan((Perkuliahan) perkuliahan.getAttribute("perkuliahan"));
		pesanRuangan.setRuang((Ruang) ruang.getAttribute("ruang"));
		pesanRuangan.setSampai(sampai.getValue());
		pesanRuangan.setJenisSemester(
				(String) (jenisSemester.getSelectedItem() == null ? null : jenisSemester.getSelectedItem().getValue()));
		pesanRuangan.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? null
						: tahunAkademik.getSelectedItem().getValue()));

		if (pesanRuangan.getId() != null) {
			pesanRuanganDao.update(pesanRuangan);
		} else {
			pesanRuanganDao.save(pesanRuangan);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PesanRuangan.class);
		if (order)
			criteria.addOrder(Order.desc("mulai"));
		if (order)
			criteria.addOrder(Order.desc("sampai"));
		criteria.add(Restrictions.ilike("tujuan", searchtujuan.getValue(), MatchMode.ANYWHERE));
		criteria.add((searchruang == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchruang.getAttribute("ruang") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("ruang", searchruang.getAttribute("ruang"))))
				.add(searchsemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisSemester", searchsemester.getSelectedItem().getValue()))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()));

		criteria.createCriteria("ruang")
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kodeRuangan", searchkodeRuangan.getValue(), MatchMode.ANYWHERE))
				.add(searchkapasitasruangan.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kapasitasRuangan",
								Integer.parseInt(searchkapasitasruangan.getValue().toString())))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchgedung.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gedung", searchgedung.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchsemester == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<PesanRuangan> pesanRuangan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pesanRuangan);
		grid.setRowRenderer(new PesanRuanganRenderer());
		grid.setModelCheckMobile(strset);

	}

	public PesanRuangan checkPemesanan() {
		Session session = HibernateUtil.currentSession();
		PesanRuangan pesanRuangan = (PesanRuangan) session.createCriteria(PesanRuangan.class)
				.add(Restrictions.or(Restrictions.between("mulai", mulai.getValue(), sampai.getValue()),
						Restrictions.between("sampai", mulai.getValue(), sampai.getValue())))
				.setMaxResults(1).add(Restrictions.eq("ruang", ruang.getAttribute("ruang")))
				.add(this.pesanRuangan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pesanRuangan.getId()))
				.uniqueResult();

		return pesanRuangan;
	}

	public Perkuliahan checkPemakaianPerkuliahan() {

		// Perkuliahan myPerkuliahan = (Perkuliahan) this.perkuliahan
		// .getAttribute("perkuliahan");
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(mulai.getValue());
		String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];

		Double mulai;
		Double selesai;

		mulai = this.mulai.getValue() == null ? null : Double.parseDouble(dateFormat.format(this.mulai.getValue()));
		selesai = this.sampai.getValue() == null ? null : Double.parseDouble(dateFormat.format(this.sampai.getValue()));

		Session session = HibernateUtil.currentSession();

		Perkuliahan perkuliahan = (Perkuliahan) session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("ruang", ruang.getAttribute("ruang"))).add(Restrictions.eq("hari", hari))
				.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1!=1")
						: Restrictions.eq("tahunAjaran", tahunAkademik.getSelectedItem().getValue()))

				.add(jenisSemester
						.getSelectedItem() == null
								? Restrictions.sqlRestriction("1!=1")
								: Restrictions
										.sqlRestriction(
												"this_.semester "
														+ ((jenisSemester.getSelectedItem().getValue()
																.equals(Perkuliahan.GENAP) ? " % 2 = 0 " : " % 2 = 1 "))
														+ ""))
				.add(Restrictions.sqlRestriction("(to_number(waktu_mulai,'999999.99') between " + mulai + " and "
						+ selesai + "   or  to_number(waktu_selesai,'999999.99') between " + mulai + " and " + selesai
						+ ")"))
				.setMaxResults(1).uniqueResult();

		return perkuliahan;
	}

}
