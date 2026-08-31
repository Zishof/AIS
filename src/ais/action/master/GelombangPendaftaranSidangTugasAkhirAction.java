package ais.action.master;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Checkbox;
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
import ais.action.report.format1.akademik.LaporanGelombangSidang;
import ais.action.report.format1.akademik.LaporanRekapitulasiGelombangSidang;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaranSidangTugasAkhir;
import ais.database.model.Jurusan;
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
 * Controller/action ZK untuk gelombang pendaftaran sidang tugas akhir. Tipe ini merupakan titik
 * masuk UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi
 * khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchketerangan}, {@code Combobox
 * searchtahunakademik}, {@code Combobox searchfakultas}, {@code Combobox searchjurusan}; inisialisasi/lifecycle
 * ({@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); penghapusan/pembatalan ({@code onDelete()}); operasi
 * domain lain ({@code onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
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
public class GelombangPendaftaranSidangTugasAkhirAction extends GenericAutowireComposer {

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
	private Intbox kuota;
	private MyDatebox sampai;
	private Combobox tahunAkademik;
	private Combobox jurusan;
	private Combobox fakultas;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir;
	private MyToolbarbuttonConfig add;
	private EventListener eventListener;
	private Tbmuser tbmuser;
	private Combobox program;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		Common.generateTahunAjaranDanSemua(searchtahunakademik);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && tbmuser.getMahasiswa() == null
				&& tbmuser.ambilDosen() == null);
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		Common.initPrograms(searchprogram);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	public static void onAddExternal(Event event, EventListener eventListener,
			GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir) throws Exception {
		GelombangPendaftaranSidangTugasAkhirAction gelombangPendaftaranSidangTugasAkhirAction = new GelombangPendaftaranSidangTugasAkhirAction();
		gelombangPendaftaranSidangTugasAkhirAction.eventListener = eventListener;
		gelombangPendaftaranSidangTugasAkhirAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(gelombangPendaftaranSidangTugasAkhirAction.addWindow);
		gelombangPendaftaranSidangTugasAkhirAction.addWindow.setHeight("350px");
		gelombangPendaftaranSidangTugasAkhirAction.addWindow.setWidth("550px");

		gelombangPendaftaranSidangTugasAkhirAction.init(gelombangPendaftaranSidangTugasAkhir);

		gelombangPendaftaranSidangTugasAkhirAction.addWindow.setVisible(true);
		gelombangPendaftaranSidangTugasAkhirAction.addWindow.onModal();
	}

	class GelombangPendaftaranSidangTugasAkhirRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir = (GelombangPendaftaranSidangTugasAkhir) arg1;

			RevisiHelper.createNewRevisi(GelombangPendaftaranSidangTugasAkhir.class,
					gelombangPendaftaranSidangTugasAkhir, gelombangPendaftaranSidangTugasAkhir.getNama())
					.setParent(arg0);

			new Label(gelombangPendaftaranSidangTugasAkhir.getMulai() == null ? ""
					: Common.dateFormat1.get().format(gelombangPendaftaranSidangTugasAkhir.getMulai())).setParent(arg0);
			new Label(gelombangPendaftaranSidangTugasAkhir.getSampai() == null ? ""
					: Common.dateFormat1.get().format(gelombangPendaftaranSidangTugasAkhir.getSampai())).setParent(arg0);
			new Label(gelombangPendaftaranSidangTugasAkhir.getTahunAkademik()).setParent(arg0);
			new Label(Common.numberFormat.get().format(gelombangPendaftaranSidangTugasAkhir.getKuota())).setParent(arg0);

			new Label(gelombangPendaftaranSidangTugasAkhir.getProgram() == null ? "Semua"
					: gelombangPendaftaranSidangTugasAkhir.getProgram()).setParent(arg0);
			new Label(gelombangPendaftaranSidangTugasAkhir.getJurusan() == null ? "Semua"
					: gelombangPendaftaranSidangTugasAkhir.getJurusan().getNama()).setParent(arg0);
			new Label(gelombangPendaftaranSidangTugasAkhir.getFakultas() == null ? "Semua"
					: gelombangPendaftaranSidangTugasAkhir.getFakultas().getNama()).setParent(arg0);
			new Label(gelombangPendaftaranSidangTugasAkhir.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(gelombangPendaftaranSidangTugasAkhir.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					gelombangPendaftaranSidangTugasAkhir.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(gelombangPendaftaranSidangTugasAkhir);
				}
			});

			final MyCheckboxConfig tetapTampilDiAdmin = new MyCheckboxConfig("Tetap Tampil di Admin");
			tetapTampilDiAdmin.setDisabled(!edit);
			tetapTampilDiAdmin.setChecked(gelombangPendaftaranSidangTugasAkhir.getTetapTampilDiAdmin());
			tetapTampilDiAdmin.setParent(arg0);
			tetapTampilDiAdmin.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					gelombangPendaftaranSidangTugasAkhir.setTetapTampilDiAdmin(tetapTampilDiAdmin.isChecked());
					Common.refreshSaveOrUpdate(gelombangPendaftaranSidangTugasAkhir);
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanGelombangSidang laporanRekapitulasiSidang = new LaporanGelombangSidang(
							gelombangPendaftaranSidangTugasAkhir);
					laporanRekapitulasiSidang.setClosable(true);
					laporanRekapitulasiSidang.setTitle("Laporan Sidang");
					laporanRekapitulasiSidang.setHeight("97%");
					laporanRekapitulasiSidang.setWidth("97%");
					laporanRekapitulasiSidang.setParent(page.getFirstRoot());
					laporanRekapitulasiSidang.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Rekap", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanRekapitulasiGelombangSidang laporanRekapitulasiSidang = new LaporanRekapitulasiGelombangSidang(
							gelombangPendaftaranSidangTugasAkhir);
					laporanRekapitulasiSidang.setClosable(true);
					laporanRekapitulasiSidang.setTitle("Laporan Jadwal Sidang");
					laporanRekapitulasiSidang.setHeight("97%");
					laporanRekapitulasiSidang.setWidth("97%");
					laporanRekapitulasiSidang.setParent(page.getFirstRoot());
					laporanRekapitulasiSidang.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(gelombangPendaftaranSidangTugasAkhir);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(
					delete && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
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
											onDelete(gelombangPendaftaranSidangTugasAkhir);
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
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public static void onDelete(GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir) {

		Common.refreshDelete(gelombangPendaftaranSidangTugasAkhir);
	}

	public void onAdd(Event event) throws Exception {
		init(new GelombangPendaftaranSidangTugasAkhir());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir) {
		Tbmuser tbmuser = Common.getCurrentUser();
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		this.gelombangPendaftaranSidangTugasAkhir = gelombangPendaftaranSidangTugasAkhir;
		addWindow.setTitle(gelombangPendaftaranSidangTugasAkhir.getId() == null ? "Tambah Jadwal Sidang" : "Ubah Jadwal Sidang");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Gelombang / Jadwal Sidang *"));
		row.appendChild(nama = new Textbox(gelombangPendaftaranSidangTugasAkhir.getNama() == null ? ""
				: gelombangPendaftaranSidangTugasAkhir.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Pendaftaran *"));
		row.appendChild(mulai = new MyDatebox(gelombangPendaftaranSidangTugasAkhir.getMulai()));
		mulai.setWidth("90%");
		mulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Pendaftaran *"));
		row.appendChild(sampai = new MyDatebox(gelombangPendaftaranSidangTugasAkhir.getSampai()));
		sampai.setWidth("90%");
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuota"));
		row.appendChild(kuota = new Intbox(gelombangPendaftaranSidangTugasAkhir.getKuota()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, gelombangPendaftaranSidangTugasAkhir.getFakultas() == null
				? tbmuser.ambilFakultas() : gelombangPendaftaranSidangTugasAkhir.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, gelombangPendaftaranSidangTugasAkhir.getJurusan() == null ? tbmuser.ambilJurusan()
				: gelombangPendaftaranSidangTugasAkhir.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = Common.initPrograms(program);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		Common.selectComboItem(program, gelombangPendaftaranSidangTugasAkhir.getProgram());
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(gelombangPendaftaranSidangTugasAkhir.getKeterangan() == null ? ""
				: gelombangPendaftaranSidangTugasAkhir.getKeterangan()));
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
						eventListener.onEvent(new Event("", addWindow,
								GelombangPendaftaranSidangTugasAkhirAction.this.gelombangPendaftaranSidangTugasAkhir));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
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

		Session session = HibernateUtil.currentSession();
		if (gelombangPendaftaranSidangTugasAkhir.getId() != null) {
			gelombangPendaftaranSidangTugasAkhir = (GelombangPendaftaranSidangTugasAkhir) session
					.load(GelombangPendaftaranSidangTugasAkhir.class, gelombangPendaftaranSidangTugasAkhir.getId());

		}
		gelombangPendaftaranSidangTugasAkhir
				.setProgram((String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						? null : program.getSelectedItem().getValue()));
		gelombangPendaftaranSidangTugasAkhir.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		gelombangPendaftaranSidangTugasAkhir.setNama(nama.getValue());
		gelombangPendaftaranSidangTugasAkhir.setKeterangan(keterangan.getValue());
		gelombangPendaftaranSidangTugasAkhir
				.setJurusan((Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? null : jurusan.getSelectedItem().getValue()));
		gelombangPendaftaranSidangTugasAkhir.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		gelombangPendaftaranSidangTugasAkhir.setMulai(mulai.getValue());
		gelombangPendaftaranSidangTugasAkhir.setSampai(sampai.getValue());

		gelombangPendaftaranSidangTugasAkhir.setKuota(kuota.getValue());

		Common.refreshSaveOrUpdate(session, gelombangPendaftaranSidangTugasAkhir);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(GelombangPendaftaranSidangTugasAkhir.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.asc("mulai"));
		if (order)
			criteria.addOrder(Order.asc("sampai"));

		criteria.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchtahunakademik.getSelectedItem() == null
						|| searchtahunakademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchtahunakademik.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

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
		List<GelombangPendaftaranSidangTugasAkhir> gelombangPendaftaranSidangTugasAkhir = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(gelombangPendaftaranSidangTugasAkhir);
		grid.setRowRenderer(new GelombangPendaftaranSidangTugasAkhirRenderer());
		grid.setModelCheckMobile(strset);

	}
}
