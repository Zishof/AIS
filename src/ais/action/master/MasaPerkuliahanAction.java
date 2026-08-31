package ais.action.master;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Iframe;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk masa perkuliahan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchketerangan}, {@code Combobox
 * searchtahunakademik}, {@code Combobox searchfakultas}, {@code Combobox searchjurusan}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); penghapusan/pembatalan
 * ({@code onDelete()}); operasi domain lain ({@code onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak
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
public class MasaPerkuliahanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchtahunakademik;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Checkbox searchaktif;

	private Textbox nama;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Combobox tahunAkademik;
	private Combobox jurusan;
	private Combobox fakultas;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private MasaPerkuliahan masaPerkuliahan;
	private MyToolbarbuttonConfig add;
	private EventListener eventListener;
	private Combobox program;
	private boolean masaPerkuliahanHanyaBolehDiubahOlheAdmin = false;

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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.generateTahunAjaranDanSemua(searchtahunakademik);
		Common.initPrograms(searchprogram);

		masaPerkuliahanHanyaBolehDiubahOlheAdmin = Common.bolehKonfigurasi("masa_perkuliahan_hanya_boleh_diubah_oleh_admin", Konfigurasi.TIDAK_AKTIF);

		if (masaPerkuliahanHanyaBolehDiubahOlheAdmin && !Common.getApakahAdmin()) {
			edit = false;
			delete = false;
			add.setVisible(false);
		} else {
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
			if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
			}
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

	public static void onAddExternal(Event event, EventListener eventListener, MasaPerkuliahan masaPerkuliahan)
			throws Exception {
		MasaPerkuliahanAction masaPerkuliahanAction = new MasaPerkuliahanAction();
		masaPerkuliahanAction.eventListener = eventListener;
		masaPerkuliahanAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(masaPerkuliahanAction.addWindow);
		masaPerkuliahanAction.addWindow.setHeight("350px");
		masaPerkuliahanAction.addWindow.setWidth("550px");

		masaPerkuliahanAction.init(masaPerkuliahan);

		masaPerkuliahanAction.addWindow.setVisible(true);
		masaPerkuliahanAction.addWindow.onModal();
	}

	class MasaPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (detail.isOpen()) {

						Common.clear(detail);

						Iframe iframe = new Iframe(
								"/pages/master/perkuliahan_simple.zul?masaPerkuliahan=" + masaPerkuliahan.getId());
						iframe.setHeight("490px");
						iframe.setWidth("100%");
						iframe.setScrolling("auto");
						iframe.setParent(detail);

					}
				}
			});

			RevisiHelper.createNewRevisi(MasaPerkuliahan.class, masaPerkuliahan, masaPerkuliahan.getNama())
					.setParent(arg0);

			new Label(masaPerkuliahan.getMulai() == null ? "" : Common.dateFormat4.get().format(masaPerkuliahan.getMulai()))
					.setParent(arg0);
			new Label(masaPerkuliahan.getSampai() == null ? "" : Common.dateFormat4.get().format(masaPerkuliahan.getSampai()))
					.setParent(arg0);
			new Label(masaPerkuliahan.getTahunAkademik()).setParent(arg0);
			new Label(masaPerkuliahan.getProgram() == null ? "Semua" : masaPerkuliahan.getProgram()).setParent(arg0);
			new Label(masaPerkuliahan.getJurusan() == null ? "Semua" : masaPerkuliahan.getJurusan().getNama())
					.setParent(arg0);

			int jumlahJadwal = ((Number) HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("masaPerkuliahan", masaPerkuliahan)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			new Label(Common.numberFormat.get().format(jumlahJadwal)).setParent(arg0);
			final MyCheckboxConfig tanggalMulaiHarusSesuaiJadwal = new MyCheckboxConfig("Tanggal Mulai Sesuai Jadwal");
			tanggalMulaiHarusSesuaiJadwal.setDisabled(!edit);
			tanggalMulaiHarusSesuaiJadwal.setChecked(masaPerkuliahan.getTanggalMulaiHarusSesuaiJadwal());
			tanggalMulaiHarusSesuaiJadwal.setParent(arg0);
			tanggalMulaiHarusSesuaiJadwal.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					masaPerkuliahan.setTanggalMulaiHarusSesuaiJadwal(tanggalMulaiHarusSesuaiJadwal.isChecked());
					Common.refreshSaveOrUpdate(masaPerkuliahan);
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(masaPerkuliahan.getAktif());
			checkbox.setParent(vbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					masaPerkuliahan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(masaPerkuliahan);
				}
			});
			final MyCheckboxConfig defaultData = new MyCheckboxConfig("Default");
			defaultData.setChecked(masaPerkuliahan.getDefaultData());
			defaultData.setParent(vbox);
			defaultData.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					masaPerkuliahan.setDefaultData(defaultData.isChecked());
					Common.refreshSaveOrUpdate(masaPerkuliahan);

					HibernateUtil.currentSession().createSQLQuery(
							"update masa_perkuliahan set default_data=false where id != " + masaPerkuliahan.getId())
							.executeUpdate();
					onSearchDefault(arg0);
				}
			});

			new Label(masaPerkuliahan.getKeterangan()).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(masaPerkuliahan);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

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
											onDelete(masaPerkuliahan);
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
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public static void onDelete(MasaPerkuliahan masaPerkuliahan) {

		Common.refreshDelete(masaPerkuliahan);
	}

	public void onAdd(Event event) throws Exception {
		init(new MasaPerkuliahan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(MasaPerkuliahan masaPerkuliahan) {
		Tbmuser tbmuser = Common.getCurrentUser();
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		this.masaPerkuliahan = masaPerkuliahan;
		addWindow.setTitle(masaPerkuliahan.getId() == null ? "Tambah Masa Perkuliahan" : "Ubah Masa Perkuliahan");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Masa Perkuliahan *"));
		row.appendChild(nama = new Textbox(masaPerkuliahan.getNama() == null ? "" : masaPerkuliahan.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai *"));
		row.appendChild(mulai = new MyDatebox(masaPerkuliahan.getMulai()));
		mulai.setWidth("90%");
		mulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyDatebox(masaPerkuliahan.getSampai()));
		sampai.setWidth("90%");
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas,
				masaPerkuliahan.getFakultas() == null ? tbmuser.ambilFakultas() : masaPerkuliahan.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				masaPerkuliahan.getJurusan() == null ? tbmuser.ambilJurusan() : masaPerkuliahan.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = Common.initPrograms(program);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		Common.selectComboItem(program, masaPerkuliahan.getProgram());
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				masaPerkuliahan.getKeterangan() == null ? "" : masaPerkuliahan.getKeterangan()));
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
						eventListener.onEvent(new Event("", addWindow, MasaPerkuliahanAction.this.masaPerkuliahan));
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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Masa Perkuliahan",
					"Kolom Masa Perkuliahan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Masa Perkuliahan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tahunAkademik.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik",
					"Kolom Tahun Akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun Akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (mulai.getValue() != null && sampai.getValue() != null && mulai.getValue().after(sampai.getValue())) {
			MyMessageboxConfig.show("Tanggal mulai tidak boleh lebih besar daripada tanggal sampai", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (masaPerkuliahan.getId() != null) {
			masaPerkuliahan = (MasaPerkuliahan) session.load(MasaPerkuliahan.class, masaPerkuliahan.getId());

		}
		masaPerkuliahan.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		masaPerkuliahan.setNama(nama.getValue());
		masaPerkuliahan.setKeterangan(keterangan.getValue());
		masaPerkuliahan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		masaPerkuliahan.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		masaPerkuliahan.setMulai(mulai.getValue());
		masaPerkuliahan.setSampai(sampai.getValue());
		masaPerkuliahan.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, masaPerkuliahan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MasaPerkuliahan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("mulai"));
		if (order)
			criteria.addOrder(Order.asc("sampai"));

		criteria.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchtahunakademik.getSelectedItem() == null
						|| searchtahunakademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchtahunakademik.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchketerangan == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<MasaPerkuliahan> masaPerkuliahan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(masaPerkuliahan);
		grid.setRowRenderer(new MasaPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}
}
