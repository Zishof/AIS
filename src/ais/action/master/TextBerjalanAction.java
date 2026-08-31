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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.TextBerjalan;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk text berjalan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchjurusan}, {@code Combobox
 * searchprogram}, {@code Combobox searchfakultas}, {@code Combobox searchPerguruanTinggi};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()});
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
public class TextBerjalanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchPerguruanTinggi;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Checkbox searchaktif;

	private Label labelFakProd;
	private Label labelYaySek;
	private Hbox fakProd;
	private Hbox yaySek;

	private MyCkEditor nama;
	private Combobox jurusan;
	private Combobox fakultas;
	private Combobox program;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private TextBerjalan textBerjalan;
	private MyToolbarbuttonConfig add;
	private Tbmuser tbmuser;
	private Combobox yayasan;
	private Combobox sekolah;
	private boolean pt;
	private boolean ya;
	private Combobox perguruanTinggi;

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

		Common.initPrograms(searchprogram);
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Common.insertComboDanSemua(searchPerguruanTinggi, "nama", PerguruanTinggi.class,
				Restrictions.eq("aktif", true));
		if (perguruanTinggi != null) {
			Common.selectComboItem(true, searchPerguruanTinggi, perguruanTinggi);
			searchPerguruanTinggi.setDisabled(true);
		}

		tbmuser = Common.getCurrentUser();

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		if (labelFakProd != null) { labelFakProd.setVisible(pt); }
		if (fakProd != null) { fakProd.setVisible(pt); }

		if (labelYaySek != null) { labelYaySek.setVisible(ya); }
		if (yaySek != null) { yaySek.setVisible(ya); }

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "fakultas", "jurusan", "perguruanTinggi", "program", "sekolah",
				"yayasan", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(TextBerjalan.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, TextBerjalan.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class TextBerjalanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TextBerjalan textBerjalan = (TextBerjalan) arg1;
			new ais.ui.util.MyHtml(
					"<marquee behavior=\"scroll\" direction=\"left\">" + textBerjalan.getNama() + "</marquee>")
					.setParent(arg0);
			new Label(((textBerjalan.getFakultas() == null ? "" : textBerjalan.getFakultas().getNama())
					+ (textBerjalan.getJurusan() == null ? "" : " / " + textBerjalan.getJurusan().getNama()))
					+ (textBerjalan.getYayasan() == null ? "" : textBerjalan.getYayasan().getNama())
					+ (textBerjalan.getSekolah() == null ? "" : " / " + textBerjalan.getSekolah().getNama())
					+ (textBerjalan.getProgram() == null || textBerjalan.getProgram().trim().isEmpty() ? ""
							: " / " + textBerjalan.getProgram()))
					.setParent(arg0);

			RevisiHelper.createNewRevisi(TextBerjalan.class, textBerjalan, textBerjalan.getKeterangan())
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(textBerjalan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					textBerjalan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(textBerjalan);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, textBerjalan, TextBerjalanAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TextBerjalan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		textBerjalan = (TextBerjalan) obj;
		init(textBerjalan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TextBerjalan textBerjalan) {
		this.textBerjalan = textBerjalan;
		addWindow.setTitle(textBerjalan.getId() == null ? "Tambah Teks Berjalan" : "Ubah Teks Berjalan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi Teks Berjalan"));
		row.appendChild(nama = new MyCkEditor());
		nama.setValue(textBerjalan.getNama());

		if (tbmuser != null && tbmuser.ambilJurusan() != null) {
			textBerjalan.setJurusan(tbmuser.ambilJurusan());
		}
		if (tbmuser != null && tbmuser.ambilFakultas() != null) {
			textBerjalan.setFakultas(tbmuser.ambilFakultas());
		}

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		Common.selectComboItem(yayasan,
				textBerjalan.getYayasan() == null ? tbmuser.ambilYayasan() : textBerjalan.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		Common.pilihSekolah(sekolah,
				textBerjalan.getSekolah() == null ? tbmuser.ambilSekolah() : textBerjalan.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perguruan Tinggi"));

		PerguruanTinggi selected = PerguruanTinggiUtil.getPerguruanTinggi();

		perguruanTinggi = new Combobox();
		Common.insertComboDanSemua(perguruanTinggi, "nama", PerguruanTinggi.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(perguruanTinggi, textBerjalan.getPerguruanTinggi() == null
				? (tbmuser.ambilFakultas() == null || tbmuser.ambilFakultas().getPerguruanTinggi() == null ? selected
						: tbmuser.ambilFakultas().getPerguruanTinggi())
				: textBerjalan.getPerguruanTinggi());
		row.appendChild(perguruanTinggi);
		perguruanTinggi.setWidth("90%");

		if ((tbmuser.ambilFakultas() == null || tbmuser.ambilFakultas().getPerguruanTinggi() == null ? selected
				: tbmuser.ambilFakultas().getPerguruanTinggi()) != null) {
			perguruanTinggi.setDisabled(true);
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				textBerjalan.getFakultas() == null ? tbmuser.ambilFakultas() : textBerjalan.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				textBerjalan.getJurusan() == null ? tbmuser.ambilJurusan() : textBerjalan.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = Common.initPrograms(program);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, textBerjalan.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(textBerjalan.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Teks Berjalan",
					"Kolom Isi Teks Berjalan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data "
							+ "Teks Berjalan dapat disimpan.",
					new String[] {
							"Isi terlebih dahulu kolom Isi Teks Berjalan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (textBerjalan.getId() != null) {
			textBerjalan = (TextBerjalan) session.load(TextBerjalan.class, textBerjalan.getId());

		}

		textBerjalan.setNama(nama.getValue());
		textBerjalan.setKeterangan(keterangan.getValue());
		textBerjalan.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));
		textBerjalan.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		textBerjalan.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
		textBerjalan.setPerguruanTinggi((PerguruanTinggi) (perguruanTinggi.getSelectedItem() == null ? null
				: perguruanTinggi.getSelectedItem().getValue()));
		textBerjalan.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		textBerjalan.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, textBerjalan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TextBerjalan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(searchPerguruanTinggi.getSelectedItem() == null
						|| searchPerguruanTinggi.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("perguruanTinggi"),
										Restrictions.eq("perguruanTinggi",
												searchPerguruanTinggi.getSelectedItem().getValue())))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("yayasan"),
								CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TextBerjalan> textBerjalan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(textBerjalan);
		grid.setRowRenderer(new TextBerjalanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
