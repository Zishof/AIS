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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
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
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AngketPenilaianUmum;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk angket penilaian umum. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Textbox searchnama}, {@code Combobox
 * searchjurusan}, {@code Combobox searchprogram}, {@code Combobox searchfakultas}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); validasi/perhitungan ({@code checkNamaAngket()}); mutasi data
 * ({@code onSave()}); operasi domain lain ({@code onAngketAngketUmum()}, {@code onAdd()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class AngketPenilaianUmumAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Row hbFakultasLabel;
	private Row hbYayasan;

	private Textbox kode;
	private Textbox isi;
	private Textbox keterangan;
	private Textbox petunjuk;
	private Intbox jumlahPilihan;
	private Combobox jurusan;
	private Combobox fakultas;
	private Combobox program;

	private boolean edit = false;
	private boolean delete = false;

	private AngketPenilaianUmum angketPenilaianUmum;
	private MyToolbarbuttonConfig add;

	private Tabpanel grupAngketUmum;
	private boolean pt = false;
	private boolean ya = false;
	private Combobox yayasan;
	private Combobox sekolah;

	public void onAngketAngketUmum(Event event) {
		if (grupAngketUmum != null && grupAngketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(grupAngketUmum);
			MyInclude iframe = new MyInclude("/pages/master/angket_penilaian_umum.zul");
			iframe.setParent(window);
		}
	}

	public static String[] contents = new String[] { "id", "kode", "petunjuk", "isi", "jumlahPilihan", "fakultas",
			"jurusan", "program", "aktif", "keterangan" };

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

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(AngketPenilaianUmum.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			AngketPenilaianUmum angket = new AngketPenilaianUmum();
			angket.setKode("001.000");
			angket.setIsi("EVALUASI PENILAIAN UMUM");
			Common.refreshSaveOrUpdate(session, angket);
		}

		Common.initPrograms(searchprogram);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AngketPenilaianUmum.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, AngketPenilaianUmum.class, contents);
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link AngketPenilaianUmumAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AngketPenilaianUmumAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AngketPenilaianUmumAction
	 */
	class AngketPenilaianUmumRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AngketPenilaianUmum angketPenilaianUmum = (AngketPenilaianUmum) arg1;

			new Label(angketPenilaianUmum.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AngketPenilaianUmum.class, angketPenilaianUmum, angketPenilaianUmum.getIsi())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(angketPenilaianUmum.getJumlahPilihan())).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(angketPenilaianUmum.getFakultas() == null ? "" : angketPenilaianUmum.getFakultas().getNama())
					.setParent(vbox);
			new Label(angketPenilaianUmum.getYayasan() == null ? "" : angketPenilaianUmum.getYayasan().getNama())
					.setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);

			new Label(angketPenilaianUmum.getJurusan() == null ? "" : angketPenilaianUmum.getJurusan().getNama())
					.setParent(vbox);
			new Label(angketPenilaianUmum.getSekolah() == null ? "" : angketPenilaianUmum.getSekolah().getNama())
					.setParent(vbox);

			new Label(angketPenilaianUmum.getProgram() == null || angketPenilaianUmum.getProgram().trim().isEmpty() ? ""
					: angketPenilaianUmum.getProgram()).setParent(arg0);

			final MyCheckboxConfig tampilKeterangan = new MyCheckboxConfig("Tampil Keterangan");
			tampilKeterangan.setDisabled(!edit);
			tampilKeterangan.setChecked(angketPenilaianUmum.getTampilKeterangan());
			tampilKeterangan.setParent(arg0);
			tampilKeterangan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					angketPenilaianUmum.setTampilKeterangan(tampilKeterangan.isChecked());
					Common.refreshSaveOrUpdate(angketPenilaianUmum);
				}
			});

			new Label(angketPenilaianUmum.getKeterangan()).setParent(arg0);

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
					init(angketPenilaianUmum);
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

											Common.refreshDelete(angketPenilaianUmum);

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

	public void onAdd(Event event) throws Exception {
		init(new AngketPenilaianUmum());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AngketPenilaianUmum angketPenilaianUmum) {
		this.angketPenilaianUmum = angketPenilaianUmum;
		addWindow.setTitle(angketPenilaianUmum.getId() == null ? "Tambah Angket Penilaian Umum" : "Ubah Angket Penilaian Umum");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Angket"));
		row.appendChild(kode = new Textbox(angketPenilaianUmum.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Angket"));
		row.appendChild(isi = new Textbox(angketPenilaianUmum.getIsi() == null ? "" : angketPenilaianUmum.getIsi()));
		isi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Petunjuk"));
		row.appendChild(petunjuk = new Textbox(angketPenilaianUmum.getPetunjuk()));
		petunjuk.setWidth("90%");
		petunjuk.setRows(7);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pilihan"));
		row.appendChild(jumlahPilihan = new Intbox(angketPenilaianUmum.getJumlahPilihan()));
		jumlahPilihan.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.ambilJurusan() != null) {
			angketPenilaianUmum.setJurusan(tbmuser.ambilJurusan());
		}
		if (tbmuser != null && tbmuser.ambilFakultas() != null) {
			angketPenilaianUmum.setFakultas(tbmuser.ambilFakultas());
		}
		if (tbmuser != null && tbmuser.ambilFakultas() != null) {
			angketPenilaianUmum.setProgram(tbmuser.ambilProgram() == null ? "" : tbmuser.ambilProgram().getNama());
		}

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, angketPenilaianUmum.getFakultas() == null ? tbmuser.ambilFakultas()
				: angketPenilaianUmum.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				angketPenilaianUmum.getJurusan() == null ? tbmuser.ambilJurusan() : angketPenilaianUmum.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				angketPenilaianUmum.getYayasan() == null ? tbmuser.ambilYayasan() : angketPenilaianUmum.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				angketPenilaianUmum.getSekolah() == null ? tbmuser.ambilSekolah() : angketPenilaianUmum.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		program = Common.initPrograms(program);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, angketPenilaianUmum.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				angketPenilaianUmum.getKeterangan() == null ? "" : angketPenilaianUmum.getKeterangan()));
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
		if (kode.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Angket",
					"Kolom Kode Angket belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode Angket.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (isi.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Angket",
					"Kolom Nama Angket belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Angket.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaAngket();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode Angket",
					"Kode Angket sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Kode Angket yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (angketPenilaianUmum.getId() != null) {
			angketPenilaianUmum = (AngketPenilaianUmum) session.load(AngketPenilaianUmum.class,
					angketPenilaianUmum.getId());

		}

		angketPenilaianUmum.setKode(kode.getValue());
		angketPenilaianUmum.setIsi(isi.getValue());
		angketPenilaianUmum.setKeterangan(keterangan.getValue());
		angketPenilaianUmum.setPetunjuk(petunjuk.getValue());
		angketPenilaianUmum.setJumlahPilihan(jumlahPilihan.getValue());

		angketPenilaianUmum.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		angketPenilaianUmum.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		angketPenilaianUmum.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		angketPenilaianUmum.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		angketPenilaianUmum.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, angketPenilaianUmum);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AngketPenilaianUmum.class);

		if (order)
			criteria.addOrder(Order.asc("kode"));

		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.isNull("jurusan"),
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("program"),
										Restrictions.eq("program", searchprogram.getSelectedItem().getValue())))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("isi", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AngketPenilaianUmum> angketPenilaianUmum = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(angketPenilaianUmum);
		grid.setRowRenderer(new AngketPenilaianUmumRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaAngket() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(AngketPenilaianUmum.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.angketPenilaianUmum.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.angketPenilaianUmum.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}



}
