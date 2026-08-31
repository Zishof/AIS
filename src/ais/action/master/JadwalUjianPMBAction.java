package ais.action.master;

import java.util.ArrayList;
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
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.dashboard.admin.RekapHasilTugas;
import ais.action.master.dashboard.admin.RekapHasilUjian;
import ais.action.master.helper.AktifitasJadwalUjianPMBHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.RuangPMBAction;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.Paket;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Pertemuan;
import ais.database.model.RuangPMB;
import ais.database.model.UjianPMB;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jadwal ujian pmb. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchujian}, {@code Combobox
 * searchpaket}, {@code Combobox searchTahunAjaran}, {@code Combobox ujianPMB}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()}, {@code init()});
 * pembacaan/pencarian ({@code tampilRuangan()}, {@code onSearchDefault()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class JadwalUjianPMBAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchujian;
	private Combobox searchpaket;
	private Combobox searchTahunAjaran;

	private Combobox ujianPMB;
	private Combobox paket;

	private MyDatebox waktuMulai;
	private MyDatebox waktuSampai;

	private MyCheckboxConfig berlakuUntukSemuaRuangan;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JadwalUjianPMB jadwalUjianPMB;
	private MyToolbarbuttonConfig add;
	private PerguruanTinggi selectedPerguruanTinggi;
	protected AktifitasJadwalUjianPMBHelper aktifitasJadwalUjianPMBHelper = new AktifitasJadwalUjianPMBHelper();
	private Row rowRuang;

	private Checkbox searchaktif;
	private MyCheckboxConfig pesertaUjianHarusPunyaNomorUjian;
	private MyCheckboxConfig pesertaUjianHarusTelahUjian;

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
		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }
		Common.generateTahunAjaran(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());
		String tahunAkademikPenerimaanMahasiswaBaru = Common
				.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik()).getNilai();

		Common.selectComboItem(searchTahunAjaran, tahunAkademikPenerimaanMahasiswaBaru);

		EventListener gelombangEventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				List<UjianPMB> ujianPMBs = HibernateUtil.currentSession().createCriteria(UjianPMB.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.createAlias("gelombangPendaftaran", "gelombangPendaftaran")
						.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(
										Restrictions.eq("gelombangPendaftaran.perguruanTinggi",
												selectedPerguruanTinggi),
										Restrictions.isNull("gelombangPendaftaran.perguruanTinggi")))
						.add(searchTahunAjaran.getSelectedItem() == null
								|| searchTahunAjaran.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAkademik",
												searchTahunAjaran.getSelectedItem().getValue()))
						.list();
				ujianPMBs.add(null);
				Common.insertComboItems(searchujian, "nama", "tahunAkademik", ujianPMBs);
				searchujian.setReadonly(true);
				Common.selectComboItem(searchujian, null);
			}
		};

		Common.insertComboDanSemua(searchpaket, "nama", "keterangan", Paket.class,
				Restrictions.and(Restrictions.eq("aktif", true),
						Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi"))));

		gelombangEventListener.onEvent(null);
		searchTahunAjaran.addEventListener("onChange", gelombangEventListener);

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

		MyToolbarbuttonConfig buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Ujian",
				"/img/svg/edit-box-line.svg");
		if (buttonFormatNilai != null) { buttonFormatNilai.setParent(add.getParent()); }
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Long> jadwalUjianPMB = initCriteria(false).setProjection(Projections.property("id")).list();

				List<Pertemuan> pertemuans = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(jadwalUjianPMB.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("jadwalUjianPMB.id", jadwalUjianPMB))
						.list();

				RekapHasilUjian addWindow = new RekapHasilUjian(pertemuans.toArray(new Pertemuan[] {}));
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Hasil Ujian");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}

		});

		buttonFormatNilai = new MyToolbarbuttonConfig("Rekap Hasil Tugas", "/img/svg/edit-box-line.svg");
		if (buttonFormatNilai != null) { buttonFormatNilai.setParent(add.getParent()); }
		buttonFormatNilai.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Long> jadwalUjianPMB = initCriteria(false).setProjection(Projections.property("id")).list();

				List<Pertemuan> pertemuans = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(jadwalUjianPMB.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("jadwalUjianPMB.id", jadwalUjianPMB))
						.list();

				RekapHasilTugas addWindow = new RekapHasilTugas(false, pertemuans.toArray(new Pertemuan[] {}));
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Hasil Tugas");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}

		});
	}

	class JadwalUjianPMBRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalUjianPMB jadwalUjianPMB = (JadwalUjianPMB) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detail.getChildren().size() == 0) {
						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						aktifitasJadwalUjianPMBHelper.initDetail(jadwalUjianPMB, groupbox);
						detail.appendChild(groupbox);
					}
				}
			});

			RevisiHelper.createNewRevisi(JadwalUjianPMB.class, jadwalUjianPMB, jadwalUjianPMB.getNama())
					.setParent(arg0);
			new Label(Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuMulai())).setParent(arg0);
			new Label(Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuSampai())).setParent(arg0);
			new Label(jadwalUjianPMB.getUjianPMB().toString()).setParent(arg0);
			new Label(jadwalUjianPMB.getPaket() == null ? "Semua" : jadwalUjianPMB.getPaket().getNama())
					.setParent(arg0);

			List<String> ruangPMBsTemorary = jadwalUjianPMB.getRuanganYgIkut().isEmpty() ? new ArrayList<String>()
					: HibernateUtil.currentSession().createCriteria(RuangPMB.class)
							.setProjection(Projections.property("kodeRuangan"))
							.add(Restrictions
									.sqlRestriction("this_.id in (-1" + jadwalUjianPMB.getRuanganYgIkut() + "-1)"))
							.list();
			String ruang = "<ul style='font-size:9px'>";
			for (String ruangPMB : ruangPMBsTemorary) {
				ruang += "<li>" + ruangPMB + "</li>";
			}
			new ais.ui.util.MyHtml(ruang + "</ul>").setParent(arg0);

			new Label(jadwalUjianPMB.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jadwalUjianPMB.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jadwalUjianPMB.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jadwalUjianPMB);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jadwalUjianPMB, JadwalUjianPMBAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JadwalUjianPMB());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(final JadwalUjianPMB jadwalUjianPMB) throws Exception {
		this.jadwalUjianPMB = jadwalUjianPMB;
		addWindow.setTitle(jadwalUjianPMB.getId() == null ? "Tambah Jadwal Ujian PMB" : "Ubah Jadwal Ujian PMB");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Materi Ujian *"));
		row.appendChild(nama = new Textbox(jadwalUjianPMB.getNama() == null ? "" : jadwalUjianPMB.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Mulai *"));
		row.appendChild(waktuMulai = new MyDatebox(jadwalUjianPMB.getWaktuMulai()));
		waktuMulai.setFormat(Common.dateFormat.get().toPattern());
		waktuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Sampai *"));
		row.appendChild(waktuSampai = new MyDatebox(jadwalUjianPMB.getWaktuSampai()));
		waktuSampai.setFormat(Common.dateFormat.get().toPattern());
		waktuSampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Ujian *"));
		row.appendChild(ujianPMB = new Combobox());

		List<UjianPMB> ujianPMBs = HibernateUtil.currentSession().createCriteria(UjianPMB.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
				.createAlias("gelombangPendaftaran", "gelombangPendaftaran")
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("gelombangPendaftaran.perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("gelombangPendaftaran.perguruanTinggi")))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.list();

		Common.insertComboItems(ujianPMB, "nama", "tahunAkademik", ujianPMBs);

		Common.selectComboItem(ujianPMB, jadwalUjianPMB.getUjianPMB());
		ujianPMB.setWidth("90%");
		ujianPMB.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket"));
		row.appendChild(paket = new Combobox());
		Common.insertComboDanSemua(paket, "nama", "keterangan", Paket.class,
				Restrictions.and(
						selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
										Restrictions.isNull("perguruanTinggi")),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.selectComboItem(paket, jadwalUjianPMB.getPaket());
		paket.setWidth("90%");

		Common.initKeterangan(rows, "Kosongkan pilihan paket jika jadwal ini berlaku untuk semua paket");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				pesertaUjianHarusPunyaNomorUjian = new MyCheckboxConfig("Peserta Ujian Harus Memiliki Nomor Ujian"));
		pesertaUjianHarusPunyaNomorUjian.setChecked(jadwalUjianPMB.getPesertaUjianHarusPunyaNomorUjian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				pesertaUjianHarusTelahUjian = new MyCheckboxConfig("Peserta Ujian Harus Telah Melakukan Ujian"));
		pesertaUjianHarusTelahUjian.setChecked(jadwalUjianPMB.getPesertaUjianHarusTelahUjian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(jadwalUjianPMB.getKeterangan() == null ? "" : jadwalUjianPMB.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		Common.initKeterangan(rows, "Keterangan akan muncul di kartu ujian");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				berlakuUntukSemuaRuangan = new MyCheckboxConfig("Jadwal / agenda ini berlaku untuk semua ruangan PMB"));
		berlakuUntukSemuaRuangan.setChecked(jadwalUjianPMB.getBerlakuUntukSemuaRuangan());

		rowRuang = new MyFormRow();
		rowRuang.setVisible(!berlakuUntukSemuaRuangan.isChecked());
		rowRuang.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowRuang, "2");
		rowRuang.appendChild(tampilRuangan(jadwalUjianPMB));


		berlakuUntukSemuaRuangan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalUjianPMB.setBerlakuUntukSemuaRuangan(berlakuUntukSemuaRuangan.isChecked());
				rowRuang.setVisible(!berlakuUntukSemuaRuangan.isChecked());
			}
		});

		ujianPMB.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalUjianPMB.setBerlakuUntukSemuaRuangan(berlakuUntukSemuaRuangan.isChecked());
				rowRuang.setVisible(!berlakuUntukSemuaRuangan.isChecked());
				UjianPMB u = (UjianPMB) (ujianPMB.getSelectedItem() == null ? null
						: ujianPMB.getSelectedItem().getValue());
				Common.clear(rowRuang);
				if (u != null) {
					jadwalUjianPMB.setUjianPMB(u);
					rowRuang.appendChild(tampilRuangan(jadwalUjianPMB));
				}
			}
		});

		paket.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jadwalUjianPMB.setBerlakuUntukSemuaRuangan(berlakuUntukSemuaRuangan.isChecked());
				rowRuang.setVisible(!berlakuUntukSemuaRuangan.isChecked());
				Paket u = (Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue());
				Common.clear(rowRuang);
				if (u != null) {
					jadwalUjianPMB.setPaket(u);
					rowRuang.appendChild(tampilRuangan(jadwalUjianPMB));
				}
			}
		});

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

	private String ruanganYgIkut;

	private Borderlayout tampilRuangan(final JadwalUjianPMB jadwalUjianPMB) throws Exception {
		ruanganYgIkut = jadwalUjianPMB.getRuanganYgIkut();
		Borderlayout myborderlayoutlagi = new Borderlayout();
		myborderlayoutlagi.setHeight("400px");
		North mynorthlagi = new North();
		mynorthlagi.setParent(myborderlayoutlagi);
		ais.ui.util.ZkCompat.setFlex(mynorthlagi, true);

		Hbox hbox = new Hbox();
		hbox.setParent(mynorthlagi);
		hbox.appendChild(new MyLabelBoldConfig("Cari : "));
		final Textbox cari = new Textbox("");
		cari.setParent(hbox);
		cari.setCols(20);

		Center mycenterlagi = new Center();
		mycenterlagi.setParent(myborderlayoutlagi);
		ais.ui.util.ZkCompat.setFlex(mycenterlagi, true);

		final Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setParent(mycenterlagi);
		grid.setMold("paging");
		grid.setPageSize(10);
		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);

		column = new MyColumnConfig("Nama");
		column.setParent(columns);

		column = new MyColumnConfig("Kapasitas/Terisi");
		column.setParent(columns);

		column = new MyColumnConfig("Paket");
		column.setParent(columns);

		final MyCheckboxConfig checkboxConfigAll = new MyCheckboxConfig("Ikut semua jadwal ini");

		column = new MyColumnConfig();
		column.appendChild(checkboxConfigAll);
		column.setParent(columns);

		grid.setHeight("100%");
		grid.setWidth("100%");

		grid.setRowRenderer(new ais.ui.util.MyRowRenderer() {
			@Override
			public void render(Row arg0, Object arg1) throws Exception {
				arg0.setValign("top");
				final RuangPMB ruangPMB = (arg1 instanceof RuangPMB) ? (RuangPMB) arg1 : null;

				Integer isi = RuangPMBAction.cekRuanganIsi(ruangPMB);

				RevisiHelper.createNewRevisi(RuangPMB.class, ruangPMB, ruangPMB.getNama()).setParent(arg0);

				new Label(ruangPMB.getKodeRuangan()).setParent(arg0);

				new Label(ruangPMB.getKapasitasRuangan() == null ? ""
						: ruangPMB.getKapasitasRuangan().toString() + "/" + isi).setParent(arg0);
				new Label(ruangPMB.getPaket() == null ? "Semua" : ruangPMB.getPaket().getNama()).setParent(arg0);

				Long id = ruangPMB.getId();

				final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Ikut jadwal ini");
				checkboxConfig.setDisabled(ruangPMB == null);
				checkboxConfig.setChecked(jadwalUjianPMB.getRuanganYgIkut().contains("," + id + ","));
				checkboxConfig.setParent(arg0);
				checkboxConfig.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Long id = ruangPMB.getId();
						String ids = "," + id + ",";
						String text = jadwalUjianPMB.getRuanganYgIkut();
						text = org.apache.commons.lang3.StringUtils.replace(text, ids, "");
						text = org.apache.commons.lang3.StringUtils.replace(text, id.toString(), "");

						jadwalUjianPMB.setRuanganYgIkut(text + (!checkboxConfig.isChecked() ? "" : ids));
						ruanganYgIkut = jadwalUjianPMB.getRuanganYgIkut();
					}
				});
			}
		});

		EventListener cariAkun = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<RuangPMB> ruangPMBsTemorary = HibernateUtil.currentSession().createCriteria(RuangPMB.class)
						.add(jadwalUjianPMB.getPaket() != null ? Restrictions.eq("paket", jadwalUjianPMB.getPaket())
								: Restrictions.sqlRestriction("true"))
						.add(Restrictions.eq("ujianPMB", jadwalUjianPMB.getUjianPMB())).list();
				List<RuangPMB> copy = new ArrayList<RuangPMB>();
				for (RuangPMB ruangPMB : ruangPMBsTemorary) {
					if (cari.getValue().trim().isEmpty() ||

							(ruangPMB != null &&

									((ruangPMB.getNim() != null && ruangPMB.getNim().toLowerCase()
											.contains(cari.getValue().toLowerCase().trim()))

											||

											(ruangPMB.getNama() != null && ruangPMB.getNama().toLowerCase()
													.contains(cari.getValue().toLowerCase().trim()))

									)

							)

					) {
						copy.add(ruangPMB);
					}
				}
				ListModel strset = new SimpleListModel(copy);
				grid.setModel(strset);
				ruangPMBsTemorary = null;
				copy = null;
			}
		};

		cariAkun.onEvent(null);
		cari.addEventListener("onOK", cariAkun);

		Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		toolbarbutton.setParent(hbox);
		toolbarbutton.addEventListener("onClick", cariAkun);

		checkboxConfigAll.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				List<RuangPMB> ruangPMBsTemorary = HibernateUtil.currentSession().createCriteria(RuangPMB.class)
						.add(jadwalUjianPMB.getPaket() != null ? Restrictions.eq("paket", jadwalUjianPMB.getPaket())
								: Restrictions.sqlRestriction("true"))
						.add(Restrictions.eq("ujianPMB", jadwalUjianPMB.getUjianPMB())).list();

				List<RuangPMB> copy = new ArrayList<RuangPMB>();
				for (RuangPMB ruangPMB : ruangPMBsTemorary) {
					if (cari.getValue().trim().isEmpty() ||

							(ruangPMB != null &&

									((ruangPMB.getKodeRuangan() != null && ruangPMB.getKodeRuangan().toLowerCase()
											.contains(cari.getValue().toLowerCase().trim()))

											||

											(ruangPMB.getNama() != null && ruangPMB.getNama().toLowerCase()
													.contains(cari.getValue().toLowerCase().trim()))

									)

							)

					) {

						Long id = ruangPMB.getId();
						String ids = "," + id + ",";
						String text = jadwalUjianPMB.getRuanganYgIkut();
						text = org.apache.commons.lang3.StringUtils.replace(text, ids, "");
						text = org.apache.commons.lang3.StringUtils.replace(text, id.toString(), "");

						jadwalUjianPMB.setRuanganYgIkut(text + (!checkboxConfigAll.isChecked() ? "" : ids));

						copy.add(ruangPMB);
					}
				}
				ruanganYgIkut = jadwalUjianPMB.getRuanganYgIkut();

				ListModel strset = new SimpleListModel(copy);
				grid.setModel(strset);
				ruangPMBsTemorary = null;
				copy = null;
			}
		});

		return myborderlayoutlagi;
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Materi ujian",
					"Kolom Materi ujian belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Materi ujian.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (waktuMulai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu mulai ujian",
					"Kolom Waktu mulai ujian belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu mulai ujian.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (waktuSampai.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Waktu sampai ujian",
					"Kolom Waktu sampai ujian belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Waktu sampai ujian.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (ujianPMB.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Data ujian",
					"Kolom Data ujian belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Data ujian.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jadwalUjianPMB.getId() != null) {
			jadwalUjianPMB = (JadwalUjianPMB) session.load(JadwalUjianPMB.class, jadwalUjianPMB.getId());

		}

		jadwalUjianPMB.setWaktuMulai(waktuMulai.getValue());
		jadwalUjianPMB.setWaktuSampai(waktuSampai.getValue());
		jadwalUjianPMB.setUjianPMB((UjianPMB) ujianPMB.getSelectedItem().getValue());
		jadwalUjianPMB.setNama(nama.getValue());
		jadwalUjianPMB.setKeterangan(keterangan.getValue());
		jadwalUjianPMB.setPaket((Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue()));
		jadwalUjianPMB.setBerlakuUntukSemuaRuangan(berlakuUntukSemuaRuangan.isChecked());

		// Bersihkan ruanganYgIkut: hapus ID ruang yang ujianPMB-nya berbeda dari ujian ini.
		// Terjadi saat jadwal disalin dari sesi lain atau ujianPMB diubah → grid tampil 2 ruang.
		if (!berlakuUntukSemuaRuangan.isChecked() && ruanganYgIkut != null && !ruanganYgIkut.trim().isEmpty()) {
			@SuppressWarnings("unchecked")
			List<Long> validIds = session.createCriteria(RuangPMB.class)
					.setProjection(Projections.property("id"))
					.add(Restrictions.eq("ujianPMB", jadwalUjianPMB.getUjianPMB()))
					.add(jadwalUjianPMB.getPaket() != null
							? Restrictions.eq("paket", jadwalUjianPMB.getPaket())
							: Restrictions.sqlRestriction("true"))
					.list();
			String bersih = "";
			for (Long vid : validIds) {
				String ids = "," + vid + ",";
				if (ruanganYgIkut.contains(ids)) {
					bersih += ids;
				}
			}
			ruanganYgIkut = bersih;
		}

		jadwalUjianPMB.setRuanganYgIkut(ruanganYgIkut);
		jadwalUjianPMB.setPesertaUjianHarusPunyaNomorUjian(pesertaUjianHarusPunyaNomorUjian.isChecked());
		jadwalUjianPMB.setPesertaUjianHarusTelahUjian(pesertaUjianHarusTelahUjian.isChecked());

		if (jadwalUjianPMB.getId() != null) {
			session.update(jadwalUjianPMB);
			// Propagasikan perubahan waktu ke Pertemuan yang sudah terbuat untuk jadwal ini
			session.createQuery(
					"UPDATE Pertemuan SET tanggal = :tgl, waktuMulai = :wm, waktuSelesai = :ws"
					+ " WHERE jadwalUjianPMB = :jadwal")
					.setParameter("tgl", jadwalUjianPMB.getWaktuMulai())
					.setParameter("wm", Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuMulai()))
					.setParameter("ws", Common.dateFormat3.get().format(jadwalUjianPMB.getWaktuSampai()))
					.setParameter("jadwal", jadwalUjianPMB)
					.executeUpdate();
		} else {
			session.save(jadwalUjianPMB);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalUjianPMB.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.createAlias("ujianPMB", "ujianPMB")
				.createAlias("ujianPMB.gelombangPendaftaran", "gelombangPendaftaran")
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.eq("gelombangPendaftaran.perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("gelombangPendaftaran.perguruanTinggi")));

		if (order)
			criteria.addOrder(Order.asc("waktuMulai"));

		criteria.add(searchujian.getSelectedItem() == null || searchujian.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("true")
				: Restrictions.eq("ujianPMB", searchujian.getSelectedItem().getValue()))

				.add(searchpaket.getSelectedItem() == null || searchpaket.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("paket", searchpaket.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("ujianPMB.tahunAkademik",
										searchTahunAjaran.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JadwalUjianPMB> jadwalUjianPMB = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalUjianPMB);
		grid.setRowRenderer(new JadwalUjianPMBRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jadwalUjianPMB = (JadwalUjianPMB) obj;
		init(jadwalUjianPMB);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
