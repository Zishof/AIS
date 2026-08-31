package ais.action.master.sekolah;


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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk kanal pembayaran. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Combobox searchyayasan}, {@code Combobox
 * searchsekolah}, {@code Textbox nama}, {@code Combobox sekolah}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class KanalPembayaranAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KanalPembayaran kanalPembayaran;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Textbox bniMerchantId;
	private Textbox bniPassword;
	private Textbox bniGatewayUrl;
	private MyCheckboxConfig aktfkanPembayaranViaFlip;
	private Textbox apiKeyFlip;
	private Textbox tokenFlip;
	private MyDoublebox biayaAdminFlip;
	private MyCheckboxConfig aktfkanPembayaranViaEsmartlink;
	private Textbox usernameEsmartlink;
	private Textbox passwordEsmartlink;
	private MyDoublebox biayaAdminEsmartlink;
	private MyTextbox variableBiayaAdminEsmartlink;
	private MyCheckboxConfig aktfkanPembayaranViaFinpay;
	private Textbox apiKeyFinpay;
	private Textbox tokenFinpay;
	private MyDoublebox biayaAdminFinpay;
	private Textbox bsiMerchantId;
	private Textbox bsiScretId;
	private Textbox bsiUsername;
	private Textbox bsiPassword;
	private Textbox bsiGatewayUrl;
	private AmbilDataAkunBanbox akun;

	private Hbox idYa;
	private Hbox idYaLabel;
	private boolean ya;

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

		boolean[] ptYa = Common.chekPtAtauSekolah();
		ya = ptYa[1];

		if (idYa != null) { idYa.setVisible(ya); }
		if (idYaLabel != null) { idYaLabel.setVisible(ya); }

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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

		String[] contents = new String[] { "id", "nama", "sekolah", "yayasan", "akun", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KanalPembayaran.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class KanalPembayaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KanalPembayaran kanalPembayaran = (KanalPembayaran) arg1;

			RevisiHelper.createNewRevisi(KanalPembayaran.class, kanalPembayaran, kanalPembayaran.getNama())
					.setParent(arg0);
			new Label(kanalPembayaran.getAkun() == null ? "" : kanalPembayaran.getAkun().getNama()).setParent(arg0);

			new Label(kanalPembayaran.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kanalPembayaran.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kanalPembayaran.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kanalPembayaran);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kanalPembayaran, KanalPembayaranAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KanalPembayaran());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kanalPembayaran = (KanalPembayaran) obj;
		init(kanalPembayaran);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(KanalPembayaran kanalPembayaran) {
		this.kanalPembayaran = kanalPembayaran;
		addWindow.setTitle(kanalPembayaran.getId() == null ? "Tambah Kanal Pembayaran" : "Ubah Kanal Pembayaran");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kanal Pembayaran *"));
		row.appendChild(nama = new Textbox(kanalPembayaran.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Kas/Bank *"));
		row.appendChild(akun = new AmbilDataAkunBanbox());
		akun.setValue(kanalPembayaran.getAkun() == null ? "" : kanalPembayaran.getAkun().getNama());
		akun.setAttribute("akun", kanalPembayaran.getAkun());
		akun.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, kanalPembayaran.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, kanalPembayaran.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Bank BNI"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BNI Merchant"));
		row.appendChild(bniMerchantId = new Textbox(kanalPembayaran.getBniMerchantId()));
		bniMerchantId.setWidth("90%");
		bniMerchantId.setRows(2);

		Common.initKeterangan(rows,
				"Jika tiap angkatan mempunyai kode yang beda, bisa dibuat format sbb : {ANGKATAN}:{KODE_BNI};{ANGKATAN}:{KODE_BNI} contoh : 2019:8979;2020:8977");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BNI Password"));
		row.appendChild(bniPassword = new Textbox(kanalPembayaran.getBniPassword()));
		bniPassword.setWidth("90%");
		bniPassword.setRows(2);

		Common.initKeterangan(rows,
				"Jika tiap angkatan mempunyai pasword yang beda, bisa dibuat format sbb : {ANGKATAN}:{PASSWORD_BNI};{ANGKATAN}:{PASSWORD_BNI} contoh : 2019:685dedd9f045787873794ead6276f8bf;2020:685dedd9f045787873794ead6276f4");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BNI Gateway Url"));
		row.appendChild(bniGatewayUrl = new Textbox(kanalPembayaran.getBniGatewayUrl()));
		bniGatewayUrl.setWidth("90%");
		bniGatewayUrl.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Flip"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aktfkanPembayaranViaFlip = new MyCheckboxConfig("Aktifkan Pembayaran Via Flip"));
		aktfkanPembayaranViaFlip.setChecked(kanalPembayaran.getAktfkanPembayaranViaFlip());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Flip API SECRET KEY"));
		row.appendChild(apiKeyFlip = new Textbox(kanalPembayaran.getApiKeyFlip()));
		apiKeyFlip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Flip VALIDATION TOKEN"));
		row.appendChild(tokenFlip = new Textbox(kanalPembayaran.getTokenFlip()));
		tokenFlip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Admin Flip"));
		row.appendChild(biayaAdminFlip = new MyDoublebox(kanalPembayaran.getBiayaAdminFlip()));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("E-Smartlink"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aktfkanPembayaranViaEsmartlink = new MyCheckboxConfig("Aktifkan Pembayaran Via Smartlink"));
		aktfkanPembayaranViaEsmartlink.setChecked(kanalPembayaran.getAktfkanPembayaranViaEsmartlink());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smartlink Username"));
		row.appendChild(usernameEsmartlink = new Textbox(kanalPembayaran.getUsernameEsmartlink()));
		usernameEsmartlink.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smartlink Password"));
		row.appendChild(passwordEsmartlink = new Textbox(kanalPembayaran.getPasswordEsmartlink()));
		passwordEsmartlink.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Admin Smartlink Default"));
		row.appendChild(biayaAdminEsmartlink = new MyDoublebox(kanalPembayaran.getBiayaAdminEsmartlink()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Variable Biaya Admin Smartlink"));
		row.appendChild(
				variableBiayaAdminEsmartlink = new MyTextbox(kanalPembayaran.getVariableBiayaAdminEsmartlink()));
		variableBiayaAdminEsmartlink.setWidth("90%");
		variableBiayaAdminEsmartlink.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Finpay"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aktfkanPembayaranViaFinpay = new MyCheckboxConfig("Aktifkan Pembayaran Via Finpay"));
		aktfkanPembayaranViaFinpay.setChecked(kanalPembayaran.getAktfkanPembayaranViaFinpay());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Finpay API SECRET KEY"));
		row.appendChild(apiKeyFinpay = new Textbox(kanalPembayaran.getApiKeyFinpay()));
		apiKeyFinpay.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Finpay VALIDATION TOKEN"));
		row.appendChild(tokenFinpay = new Textbox(kanalPembayaran.getTokenFinpay()));
		tokenFinpay.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Admin Finpay"));
		row.appendChild(biayaAdminFinpay = new MyDoublebox(kanalPembayaran.getBiayaAdminFinpay()));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("BSI Maja"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja ClientID"));
		row.appendChild(bsiMerchantId = new Textbox(kanalPembayaran.getBsiMerchantId()));
		bsiMerchantId.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja SecretKey"));
		row.appendChild(bsiScretId = new Textbox(kanalPembayaran.getBsiScretId()));
		bsiScretId.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja username"));
		row.appendChild(bsiUsername = new Textbox(kanalPembayaran.getBsiUsername()));
		bsiUsername.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja password"));
		row.appendChild(bsiPassword = new Textbox(kanalPembayaran.getBsiPassword()));
		bsiPassword.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja API Endpoint"));
		row.appendChild(bsiGatewayUrl = new Textbox(kanalPembayaran.getBsiGatewayUrl()));
		bsiGatewayUrl.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kanalPembayaran.getKeterangan()));
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
			MyMessageboxConfig.show("Nama Kanal Pembayaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (akun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Akun harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kanalPembayaran.getId() != null) {
			kanalPembayaran = (KanalPembayaran) session.load(KanalPembayaran.class, kanalPembayaran.getId());

		}

		kanalPembayaran.setNama(nama.getValue());
		kanalPembayaran.setAkun((Akun) akun.getAttribute("akun"));
		kanalPembayaran.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
		kanalPembayaran.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));

		kanalPembayaran.setBniGatewayUrl(bniGatewayUrl.getValue());
		kanalPembayaran.setBniMerchantId(bniMerchantId.getValue());
		kanalPembayaran.setBniPassword(bniPassword.getValue());

		kanalPembayaran.setAktfkanPembayaranViaFlip(aktfkanPembayaranViaFlip.isChecked());
		kanalPembayaran.setApiKeyFlip(apiKeyFlip.getValue().trim());
		kanalPembayaran.setTokenFlip(tokenFlip.getValue().trim());
		kanalPembayaran.setBiayaAdminFlip(biayaAdminFlip.getValue());

		kanalPembayaran.setAktfkanPembayaranViaEsmartlink(aktfkanPembayaranViaEsmartlink.isChecked());
		kanalPembayaran.setUsernameEsmartlink(usernameEsmartlink.getValue().trim());
		kanalPembayaran.setPasswordEsmartlink(passwordEsmartlink.getValue().trim());
		kanalPembayaran.setBiayaAdminEsmartlink(biayaAdminEsmartlink.getValue());
		kanalPembayaran.setVariableBiayaAdminEsmartlink(variableBiayaAdminEsmartlink.getValue());

		kanalPembayaran.setAktfkanPembayaranViaFinpay(aktfkanPembayaranViaFinpay.isChecked());
		kanalPembayaran.setApiKeyFinpay(apiKeyFinpay.getValue().trim());
		kanalPembayaran.setTokenFinpay(tokenFinpay.getValue().trim());
		kanalPembayaran.setBiayaAdminFinpay(biayaAdminFinpay.getValue());

		kanalPembayaran.setBsiMerchantId(bsiMerchantId.getValue());
		kanalPembayaran.setBsiGatewayUrl(bsiGatewayUrl.getValue());
		kanalPembayaran.setBsiPassword(bsiPassword.getValue());
		kanalPembayaran.setBsiScretId(bsiScretId.getValue());
		kanalPembayaran.setBsiUsername(bsiUsername.getValue());

		kanalPembayaran.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kanalPembayaran);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KanalPembayaran.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

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

		List<KanalPembayaran> kanalPembayaran = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kanalPembayaran);
		grid.setRowRenderer(new KanalPembayaranRenderer());
		grid.setModelCheckMobile(strset);

	}

}
