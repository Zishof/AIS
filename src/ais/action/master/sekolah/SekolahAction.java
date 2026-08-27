package ais.action.master.sekolah;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.PerguruanTinggiAction;
import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Pendaftar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.JenisSekolah;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SekolahAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;
	/** Hanya untuk super admin: bila dicentang, tampilkan SEMUA sekolah (abaikan filter domain). */
	private Checkbox abaikanDomain;

	private Textbox nama;
	private Textbox alamat;
	private Textbox email;
	private Textbox fax;
	private Textbox telp;
	private Textbox wa;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Sekolah sekolah;
	private MyToolbarbuttonConfig add;
	private Textbox namaKepalaSekolah;
	private Combobox yayasan;
	private Combobox jenisSekolah;
	private Textbox nss;
	protected LampiranLain bg;
	private MyCkEditor deskripsi;
	protected LampiranLain logo;
	protected LampiranLain background;
	private Textbox domain;
	protected LampiranLain backgroundLogin;
	private Textbox motto;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private Set<PenjurusanSekolah> selectedPenjurusanSekolah;
	private MyCheckboxConfig penjurusanBolehDipilihSaatPsb;
	private MyCheckboxConfig siswaDiizinkanDiPortalYayasan;
	private Textbox bniMerchantId;
	private Textbox bniPassword;
	private Textbox bniGatewayUrl;
	private Textbox nipKepalaSekolah;
	private Combobox css;
	private Textbox npsn;
	private Textbox kecamatan;
	private Textbox kelurahan;
	private Textbox rt;
	private Textbox rw;
	private Textbox kabupatenKota;
	private Textbox propinsi;
	private Textbox dusun;
	private Textbox kodePos;
	private Textbox website;

	private Textbox labelPejabat1;
	private AmbilDataPegawaiBanbox pegawai1;
	private Textbox labelPejabat2;
	private AmbilDataPegawaiBanbox pegawai2;
	private Textbox labelPejabat3;
	private AmbilDataPegawaiBanbox pegawai3;

	private Textbox labelPejabat4;
	private AmbilDataPegawaiBanbox pegawai4;

	private Textbox labelPejabat5;
	private AmbilDataPegawaiBanbox pegawai5;
	private Combobox perguruanTinggi;
	private Textbox tanyaWhatsapp;
	private Textbox jawabWhatsappPsb;
	private MyCheckboxConfig penjurusanWajibDipilih;
	protected LampiranLain alurppdb;

	private MyCheckboxConfig aktfkanPembayaranViaFlip;
	private Textbox apiKeyFlip;
	private Textbox tokenFlip;
	private MyDoublebox biayaAdminFlip;

	private MyCheckboxConfig aktfkanPembayaranViaEsmartlink;
	private Textbox usernameEsmartlink;
	private Textbox passwordEsmartlink;
	private MyDoublebox biayaAdminEsmartlink;

	private MyCheckboxConfig aktfkanPembayaranViaFinpay;
	private Textbox apiKeyFinpay;
	private Textbox tokenFinpay;
	private MyDoublebox biayaAdminFinpay;
	protected LampiranLain bg_bawah;
	private Textbox namaWakilKepalaSekolah;
	private Textbox nipWakilKepalaSekolah;
	private MyCheckboxConfig guruHarusPakaiSatuanKerja;
	private Yayasan yayasanData = null;
	private Pendaftar pendaftar = null;
	private Textbox bsiMerchantId;
	private Textbox bsiScretId;
	private Textbox bsiUsername;
	private Textbox bsiPassword;
	private Textbox bsiGatewayUrl;
	private MyTextbox variableBiayaAdminEsmartlink;
	protected LampiranLain bg_ppdb;
	protected LampiranLain footer_ppdb;
	protected LampiranLain kop_ppdb;
	protected LampiranLain bg_utama;
	private MyCkEditor headerppdb;
	private Combobox kanalPembayaran;
	private Combobox piilhanTampilanSekolahCb;
	protected LampiranLain kopStempel;
	private Tbmuser tbmuser;
	private MyCheckboxConfig aktfkanBjbSyariah;
	private MyDoublebox biayaAdminBjbSyariah;

	public static volatile Map<String, Sekolah> sekolahByDomain = new HashMap<String, Sekolah>();

	private static final ReentrantLock REINIT_DOMAIN_LOCK = new ReentrantLock();
	private static volatile long reinitDomainTerakhir = 0L;
	/** Jeda minimum antar-rebuild peta domain (throttle anti thundering-herd query DB). */
	private static final long REINIT_DOMAIN_INTERVAL_MS = 60000L;

	/**
	 * Membangun ulang peta domain-&gt;Sekolah.
	 *
	 * <p>Dulu method ini {@code clear()} peta bersama lalu query DB &amp; isi ulang tanpa sinkronisasi.
	 * Akibatnya, ketika peta kosong (cold start, atau tepat setelah {@code clear()}), SETIAP request
	 * paralel yang memanggil {@code if (sekolahByDomain.isEmpty()) reInitByDomain()} akan membuka
	 * koneksi Hibernate sendiri-sendiri → pool c3p0 HABIS (ratusan thread antre koneksi, terlihat di
	 * snapshot performa). Sekarang: (1) <b>single-flight</b> — hanya satu thread rebuild pada satu
	 * waktu dan request lain langsung memakai snapshot lama tanpa menunggu lock; (2) <b>throttle</b>
	 * — tidak query ulang &lt; {@value #REINIT_DOMAIN_INTERVAL_MS} ms sejak
	 * build terakhir (menahan storm saat hasil query memang kosong); (3) <b>atomic-swap</b> — bangun
	 * ke peta BARU lalu tukar referensi, sehingga pembaca tak pernah melihat peta setengah-kosong.</p>
	 */
	public static void reInitByDomain() {
		if (System.currentTimeMillis() - reinitDomainTerakhir < REINIT_DOMAIN_INTERVAL_MS) {
			return;
		}
		// Request paralel tidak perlu ikut menunggu query cache domain. Satu thread memuat,
		// sedangkan thread lain tetap memakai snapshot lama yang dipublikasikan secara atomik.
		if (!REINIT_DOMAIN_LOCK.tryLock()) {
			return;
		}
		Session session = null;
		try {
			// Cek ganda di dalam lock: thread lain mungkin baru saja membangun ulang.
			if (System.currentTimeMillis() - reinitDomainTerakhir < REINIT_DOMAIN_INTERVAL_MS) {
				return;
			}
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<Sekolah> sekolahs = ConstantValues.simpleList(
						session.createCriteria(Sekolah.class).add(Restrictions.isNotNull("domain"))
								.add(Restrictions.ne("domain", ""))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Sekolah.class);
				Map<String, Sekolah> baru = new HashMap<String, Sekolah>();
				for (Sekolah sekolah : sekolahs) {
					if (sekolah != null && sekolah.getDomain() != null) {
						// MULTI-DOMAIN: satu Sekolah boleh punya beberapa domain (dipisah koma).
						for (String d : Common.pisahDomain(sekolah.getDomain())) {
							baru.put(d, sekolah);
						}
					}
				}
				sekolahByDomain = baru;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// Tandai waktu percobaan (sukses/kosong/gagal) agar tak query ulang tiap request.
				reinitDomainTerakhir = System.currentTimeMillis();
				try {
					if (session != null) session.clear();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "SekolahAction.reInitByDomain clear session");
				}
				try {
					if (session != null && session.isConnected()) session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "SekolahAction.reInitByDomain disconnect session");
				}
				try {
					if (session != null && session.isOpen()) session.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/sekolah/SekolahAction.java:245");
				}
			}
		} finally {
			REINIT_DOMAIN_LOCK.unlock();
		}
	}

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		tbmuser = Common.getCurrentUser();
		// Filter "Tampilkan semua (tanpa filter domain)" hanya tampil untuk super admin.
		if (abaikanDomain != null) {
			abaikanDomain.setVisible(Common.getApakahAdminLain(tbmuser));
		}
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (execution.getParameter("yayasan") != null) {
			yayasanData = (Yayasan) ConstantValues.ambil(Yayasan.class.getName(),
					Long.parseLong(execution.getParameter("yayasan").trim()));
		} else {
			Yayasan yayasan = SekolahUtil.getYayasan();
			if (yayasan != null && yayasan.getId() != null) {
				yayasanData = yayasan;
			}
		}

		if (execution.getParameter("pendaftar") != null) {
			pendaftar = (Pendaftar) ConstantValues.ambil(Pendaftar.class.getName(),
					Long.parseLong(execution.getParameter("pendaftar").trim()));
		}

		String[] contents = new String[] { "id", "nama", "namaKepalaSekolah", "nss", "yayasan.nama",
				"jenisSekolah.nama", "alamat", "email", "fax", "telp", "wa", "keterangan", "deskripsi", "domain",
				"satuanKerja", "labelPejabat1", "labelPejabat2", "labelPejabat3", "labelPejabat4", "labelPejabat5",
				"pegawai1", "pegawai2", "pegawai3", "pegawai4", "pegawai5", "aktif", "css" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Sekolah.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class SekolahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Sekolah sekolah = (Sekolah) arg1;

			new Label(sekolah.getNpsn()).setParent(arg0);

			Vbox a = RevisiHelper.createNewRevisi(Sekolah.class, sekolah, sekolah.getNama());
			a.setParent(arg0);
			if (sekolah.getDomain() != null && !sekolah.getDomain().isEmpty()) {
				a.appendChild(new Label(sekolah.getDomain()));
			}

			new Label(sekolah.getNamaKepalaSekolah()).setParent(arg0);
			new Label(sekolah.getAlamat()).setParent(arg0);
			new Label(sekolah.getTelp()).setParent(arg0);
			new Label(sekolah.getWa()).setParent(arg0);
			new Label(sekolah.getFax()).setParent(arg0);
			new Label(sekolah.getEmail()).setParent(arg0);
			new Label(sekolah.getYayasan() == null ? "" : sekolah.getYayasan().getNama()).setParent(arg0);
			new Label(sekolah.getJenisSekolah() == null ? "" : sekolah.getJenisSekolah().getNama()).setParent(arg0);
			new Label(sekolah.getSatuanKerja() == null ? "" : sekolah.getSatuanKerja().getNama()).setParent(arg0);
			new Label(sekolah.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(sekolah.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					sekolah.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(sekolah);
				}
			});

			Hbox toolbar;
			(toolbar = Common.copyEditDeleteButtons(edit, delete, sekolah, SekolahAction.this)).setParent(arg0);

			GeneralValueObject.tampilKunci(toolbar, sekolah, tbmuser, new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}

			}, false);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Sekolah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		sekolah = (Sekolah) obj;
		init(sekolah);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final Sekolah sekolah) throws Exception {
		// FIX NullPointerException di onSave (this.sekolah.getId()): baris ini sebelumnya menyetel
		// this.sekolah = null untuk entitas BARU (id==null, mis. alur "Tambah Sekolah" dari onAdd()),
		// padahal onSave() langsung memakai this.sekolah tanpa null-check -> NPE saat simpan sekolah
		// baru. Field this.sekolah harus selalu berisi entitas yang sedang diedit (baru maupun lama).
		this.sekolah = sekolah;
		addWindow.setTitle(sekolah.getId() == null ? "Tambah Sekolah" : "Ubah Sekolah");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		ais.ui.util.MyButtonTabbox btnTab = ais.ui.util.MyButtonTabbox.buat(center, "100%", new int[] { 0 });
		org.zkoss.zul.Div panelSekolah = btnTab.tambahTab(0, "Data Sekolah", "/img/svg/book.svg");

		Borderlayout borderlayoutSekolah = new ais.ui.util.MyBorderlayout();
		borderlayoutSekolah.setParent(panelSekolah);

		Center centerSekolah = new Center();
		centerSekolah.setParent(borderlayoutSekolah);
		ais.ui.util.ZkCompat.setFlex(centerSekolah, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(centerSekolah);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Pokok Sekolah Nasional (NPSN) "));
		row.appendChild(npsn = new Textbox(sekolah.getNpsn()));
		npsn.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIS/NSS/NDS"));
		row.appendChild(nss = new Textbox(sekolah.getNss()));
		nss.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Sekolah *"));
		row.appendChild(nama = new Textbox(sekolah.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kepala Sekolah"));
		row.appendChild(namaKepalaSekolah = new Textbox(sekolah.getNamaKepalaSekolah()));
		namaKepalaSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kepala Sekolah"));
		row.appendChild(nipKepalaSekolah = new Textbox(sekolah.getNipKepalaSekolah()));
		nipKepalaSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Wakil Kepala Sekolah"));
		row.appendChild(namaWakilKepalaSekolah = new Textbox(sekolah.getNamaWakilKepalaSekolah()));
		namaWakilKepalaSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Wakil Kepala Sekolah"));
		row.appendChild(nipWakilKepalaSekolah = new Textbox(sekolah.getNipWakilKepalaSekolah()));
		nipWakilKepalaSekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		labelPejabat1 = new Textbox(sekolah.getLabelPejabat1());
		if (sekolah.getDikunci() != null) {
			row.appendChild(new Label(sekolah.getLabelPejabat1()));
		} else {
			row.appendChild(labelPejabat1);
		}

		row.appendChild(pegawai1 = new AmbilDataPegawaiBanbox(false));
		pegawai1.setAttribute("pegawai", sekolah.getPegawai1());
		pegawai1.setValue(sekolah.getPegawai1() == null ? "" : sekolah.getPegawai1().getNama());
		pegawai1.setWidth("90%");
		pegawai1.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		labelPejabat2 = new Textbox(sekolah.getLabelPejabat2());
		if (sekolah.getDikunci() != null) {
			row.appendChild(new Label(sekolah.getLabelPejabat2()));
		} else {
			row.appendChild(labelPejabat2);
		}
		row.appendChild(pegawai2 = new AmbilDataPegawaiBanbox(false));
		pegawai2.setAttribute("pegawai", sekolah.getPegawai2());
		pegawai2.setValue(sekolah.getPegawai2() == null ? "" : sekolah.getPegawai2().getNama());
		pegawai2.setWidth("90%");
		pegawai2.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		labelPejabat3 = new Textbox(sekolah.getLabelPejabat3());
		if (sekolah.getDikunci() != null) {
			row.appendChild(new Label(sekolah.getLabelPejabat3()));
		} else {
			row.appendChild(labelPejabat3);
		}
		row.appendChild(pegawai3 = new AmbilDataPegawaiBanbox(false));
		pegawai3.setAttribute("pegawai", sekolah.getPegawai3());
		pegawai3.setValue(sekolah.getPegawai3() == null ? "" : sekolah.getPegawai3().getNama());
		pegawai3.setWidth("90%");
		pegawai3.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);

		labelPejabat4 = new Textbox(sekolah.getLabelPejabat4());
		if (sekolah.getDikunci() != null) {
			row.appendChild(new Label(sekolah.getLabelPejabat4()));
		} else {
			row.appendChild(labelPejabat4);
		}

		row.appendChild(pegawai4 = new AmbilDataPegawaiBanbox(false));
		pegawai4.setAttribute("pegawai", sekolah.getPegawai4());
		pegawai4.setValue(sekolah.getPegawai4() == null ? "" : sekolah.getPegawai4().getNama());
		pegawai4.setWidth("90%");
		pegawai4.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		labelPejabat5 = new Textbox(sekolah.getLabelPejabat5());
		if (sekolah.getDikunci() != null) {
			row.appendChild(new Label(sekolah.getLabelPejabat5()));
		} else {
			row.appendChild(labelPejabat5);
		}
		row.appendChild(pegawai5 = new AmbilDataPegawaiBanbox(false));
		pegawai5.setAttribute("pegawai", sekolah.getPegawai5());
		pegawai5.setValue(sekolah.getPegawai5() == null ? "" : sekolah.getPegawai5().getNama());
		pegawai5.setWidth("90%");
		pegawai5.setReadonly(true);

		row = new MyFormRow();

		if (yayasanData != null && yayasanData.getId() != null) {
			sekolah.setYayasan(yayasanData);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		yayasan = new Combobox();
		if (yayasanData != null) {
			row.appendChild(new Label(yayasanData.getNama()));
		} else {
			row.appendChild(yayasan);
		}

		Common.insertCombo(yayasan, "nama", Yayasan.class);
		Common.selectComboItem(yayasan, sekolah.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Sekolah *"));
		row.appendChild(jenisSekolah = new Combobox());
		Common.insertCombo(jenisSekolah, "nama", "jenjang", JenisSekolah.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisSekolah, sekolah.getJenisSekolah());
		jenisSekolah.setWidth("90%");
		jenisSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false));
		satuanKerja
				.setValue(sekolah.getSatuanKerja() == null
						? (Common.getCurrentUser().ambilSatuanKerja() == null ? ""
								: Common.getCurrentUser().ambilSatuanKerja().toString())
						: sekolah.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja",
				sekolah.getSatuanKerja() == null ? Common.getCurrentUser().ambilSatuanKerja()
						: sekolah.getSatuanKerja());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(guruHarusPakaiSatuanKerja = new MyCheckboxConfig("Guru Harus Pakai Satuan Kerja"));
		guruHarusPakaiSatuanKerja.setChecked(sekolah.getGuruHarusPakaiSatuanKerja());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				siswaDiizinkanDiPortalYayasan = new MyCheckboxConfig("Siswa Diizinkan Login Di Portal Utama/Yayasan"));
		siswaDiizinkanDiPortalYayasan.setChecked(sekolah.getSiswaDiizinkanDiPortalYayasan());

		initKelengkapanBerkas(rows, sekolah);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat Sekolah"));
		row.appendChild(alamat = new Textbox(sekolah.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(3);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Motto Sekolah"));
		row.appendChild(motto = new Textbox(sekolah.getMotto()));
		motto.setWidth("90%");
		motto.setRows(2);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email Sekolah"));
		row.appendChild(email = new Textbox(sekolah.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telpon Sekolah"));
		row.appendChild(telp = new Textbox(sekolah.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("WA Operator Sekolah"));
		row.appendChild(wa = new Textbox(sekolah.getWa()));
		wa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("WA Tanya"));
		row.appendChild(tanyaWhatsapp = new Textbox(sekolah.getTanyaWhatsapp()));
		tanyaWhatsapp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("WA Jawab"));
		row.appendChild(jawabWhatsappPsb = new Textbox(sekolah.getJawabWhatsappPsb()));
		jawabWhatsappPsb.setWidth("90%");
		jawabWhatsappPsb.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fax Sekolah"));
		row.appendChild(fax = new Textbox(sekolah.getTelp()));
		fax.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Domain Sekolah"));
		row.appendChild(domain = new Textbox(sekolah.getDomain()));
		domain.setWidth("90%");
		Common.initKeterangan(rows,
				"Bisa lebih dari satu domain, dipisah tanda koma (,). Contoh: eschool.a.sch.id, eschool.b.sch.id, eschool.c.sch.id. "
						+ "Sistem akan mencocokkan alamat yang sedang dibuka dengan salah satu domain di atas.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RT Sekolah"));
		row.appendChild(rt = new Textbox(sekolah.getRt()));
		rt.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("RW Sekolah"));
		row.appendChild(rw = new Textbox(sekolah.getRw()));
		rw.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pos Sekolah"));
		row.appendChild(kodePos = new Textbox(sekolah.getKodePos()));
		kodePos.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kampung/Dusun Sekolah"));
		row.appendChild(dusun = new Textbox(sekolah.getDusun()));
		dusun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelurahan/Desa Sekolah"));
		row.appendChild(kelurahan = new Textbox(sekolah.getKelurahan()));
		kelurahan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kecamatan Sekolah"));
		row.appendChild(kecamatan = new Textbox(sekolah.getKecamatan()));
		kecamatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kota/Kabupaten Sekolah"));
		row.appendChild(kabupatenKota = new Textbox(sekolah.getKabupatenKota()));
		kabupatenKota.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Propinsi Sekolah"));
		row.appendChild(propinsi = new Textbox(sekolah.getPropinsi()));
		propinsi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Website Sekolah"));
		row.appendChild(website = new Textbox(sekolah.getWebsite()));
		website.setWidth("90%");

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(sekolah.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tema"));
		row.appendChild(css = PerguruanTinggiAction.buatTema());
		Common.selectComboItem(css, sekolah.getCss());
		css.setWidth("90%");
		css.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilihan Tampilan UI/UX"));
		piilhanTampilanSekolahCb = new Combobox();
		piilhanTampilanSekolahCb.setId("piilhanTampilanSekolahCb");
		piilhanTampilanSekolahCb.setReadonly(true);
		piilhanTampilanSekolahCb.setWidth("90%");
		org.zkoss.zul.Comboitem ciSekolahDefault = new org.zkoss.zul.Comboitem("Ikuti Default (Konfigurasi Sistem)");
		ciSekolahDefault.setValue(Sekolah.TAMPILAN_DEFAULT);
		piilhanTampilanSekolahCb.appendChild(ciSekolahDefault);
		org.zkoss.zul.Comboitem ciSekolahKlasik = new org.zkoss.zul.Comboitem("Tampilan Klasik (ZKoss)");
		ciSekolahKlasik.setValue(Sekolah.TAMPILAN_KLASIK);
		piilhanTampilanSekolahCb.appendChild(ciSekolahKlasik);
		org.zkoss.zul.Comboitem ciSekolahBaru = new org.zkoss.zul.Comboitem("Tampilan Baru & Modern");
		ciSekolahBaru.setValue(Sekolah.TAMPILAN_BARU);
		piilhanTampilanSekolahCb.appendChild(ciSekolahBaru);
		String curTampilanSekolah = sekolah.getPiilhanTampilan();
		boolean sekolahTampilanSet = false;
		for (Object oItem : piilhanTampilanSekolahCb.getItems()) {
			org.zkoss.zul.Comboitem ci = (org.zkoss.zul.Comboitem) oItem;
			if (curTampilanSekolah != null && curTampilanSekolah.equals(ci.getValue())) {
				piilhanTampilanSekolahCb.setSelectedItem(ci);
				sekolahTampilanSet = true;
				break;
			}
		}
		if (!sekolahTampilanSet) piilhanTampilanSekolahCb.setSelectedIndex(0);
		row.appendChild(piilhanTampilanSekolahCb);

		bg = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Atas (JPG) "));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.KOP_SEKOLAH, "KOP", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		bg_bawah = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Bawah (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.KOP_BAWAH_SEKOLAH, "KOP", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_bawah = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		kopStempel = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Stempel (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.STEMPEL_SEKOLAH, "Stempel", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kopStempel = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		kop_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("HEADER PPDB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.KOP_PPDB_SEKOLAH, "HEADER", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		bg_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background PPDB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.BG_PPDB_SEKOLAH, "Background",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		bg_utama = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Utama (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.BG_SEKOLAH, "Background", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_utama = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		footer_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("FOOTER PPDB (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.FOOTER_PPDB_SEKOLAH, "FOOTER",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						footer_ppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		logo = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Logo Sekolah"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.LOGO_SEKOLAH, "Logo", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						logo = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		background = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Sekolah"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.BACKGROUND_SEKOLAH, "Background",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						background = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		backgroundLogin = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Login Sekolah"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.BACKGROUND_LOGIN_SEKOLAH,
				"Background Login", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						backgroundLogin = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		alurppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur PPDB Sekolah"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sekolah.getId(), LampiranLain.ALUR_REGISTRASI_PSB,
				"Alur PPDB Sekolah", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						alurppdb = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, sekolah.getDikunci() == null, null);
		hbox.setParent(row);

		if (Common.getApakahAdmin()) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Profile Aplikasi"));
			row.appendChild(perguruanTinggi = new Combobox());
			Common.insertCombo(perguruanTinggi, "nama", PerguruanTinggi.class, Restrictions.eq("aktif", true));
			Common.selectComboItem(perguruanTinggi, sekolah.getPerguruanTinggi());
			perguruanTinggi.setReadonly(true);
		} else {
			perguruanTinggi = null;
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi"));
		row.appendChild(deskripsi = new MyCkEditor());
		deskripsi.setValue(sekolah.getDeskripsi());
		deskripsi.setHeight("100%");
		deskripsi.setWidth("100%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Header PPDB"));
		row.appendChild(headerppdb = new MyCkEditor());
		headerppdb.setValue(sekolah.getHeaderppdb());
		headerppdb.setHeight("100%");
		headerppdb.setWidth("100%");

		org.zkoss.zul.Div panelGateway = btnTab.tambahTab(1, "Data Payment Gateway", "/img/svg/payments.svg");

		Borderlayout borderlayoutPaymentGateway = new ais.ui.util.MyBorderlayout();
		borderlayoutPaymentGateway.setParent(panelGateway);

		final Center centerPaymentGateway = new Center();

		North northPaymentGateway = new North();
		northPaymentGateway.setParent(borderlayoutPaymentGateway);
		ais.ui.util.ZkCompat.setFlex(northPaymentGateway, true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(northPaymentGateway);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kanal Pembayaran"));
		row.appendChild(kanalPembayaran = new Combobox());
		kanalPembayaran.setWidth("90%");
		kanalPembayaran.setReadonly(true);

		EventListener eventListenerKanal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				try {
					Common.insertComboDanSemua(kanalPembayaran, new String[] { "nama" }, "keterangan",
							KanalPembayaran.class, "Ikuti Kanal Pembayaran Default",
							Restrictions.and(
									Restrictions.or(Restrictions.isNull("sekolah"),
											sekolah.getId() == null ? Restrictions.sqlRestriction("false")
													: Restrictions.eq("sekolah.id", sekolah.getId())),
									Restrictions.eq("aktif", true)));
					Common.selectComboItem(kanalPembayaran, sekolah.getKanalPembayaran());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/SekolahAction.java:998");
					// TODO: handle exception
				}
			}
		};

		EventListener eventListenerdata = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					KanalPembayaran kanal = (KanalPembayaran) (kanalPembayaran.getSelectedItem() == null ? null
							: kanalPembayaran.getSelectedItem().getValue());
					((MyGrid) centerPaymentGateway.getChildren().get(0)).setVisible(kanal == null);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/SekolahAction.java:1012");
					// TODO: handle exception
				}
			}
		};

		kanalPembayaran.addEventListener("onChange", eventListenerdata);

		centerPaymentGateway.setParent(borderlayoutPaymentGateway);
		ais.ui.util.ZkCompat.setFlex(centerPaymentGateway, true);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(centerPaymentGateway);
		grid.setWidth("100%");
		grid.setHeight("100%");

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Bank BNI"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BNI Merchant"));
		row.appendChild(bniMerchantId = new Textbox(sekolah.getBniMerchantId()));
		bniMerchantId.setWidth("90%");
		bniMerchantId.setRows(2);

		Common.initKeterangan(rows,
				"Jika tiap angkatan mempunyai kode yang beda, bisa dibuat format sbb : {ANGKATAN}:{KODE_BNI};{ANGKATAN}:{KODE_BNI} contoh : 2019:8979;2020:8977");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BNI Password"));
		row.appendChild(bniPassword = new Textbox(sekolah.getBniPassword()));
		bniPassword.setWidth("90%");
		bniPassword.setRows(2);

		Common.initKeterangan(rows,
				"Jika tiap angkatan mempunyai pasword yang beda, bisa dibuat format sbb : {ANGKATAN}:{PASSWORD_BNI};{ANGKATAN}:{PASSWORD_BNI} contoh : 2019:685dedd9f045787873794ead6276f8bf;2020:685dedd9f045787873794ead6276f4");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BNI Gateway Url"));
		row.appendChild(bniGatewayUrl = new Textbox(sekolah.getBniGatewayUrl()));
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
		aktfkanPembayaranViaFlip.setChecked(sekolah.getAktfkanPembayaranViaFlip());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Flip API SECRET KEY"));
		row.appendChild(apiKeyFlip = new Textbox(sekolah.getApiKeyFlip()));
		apiKeyFlip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Flip VALIDATION TOKEN"));
		row.appendChild(tokenFlip = new Textbox(sekolah.getTokenFlip()));
		tokenFlip.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Admin Flip"));
		row.appendChild(biayaAdminFlip = new MyDoublebox(sekolah.getBiayaAdminFlip()));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("E-Smartlink"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aktfkanPembayaranViaEsmartlink = new MyCheckboxConfig("Aktifkan Pembayaran Via Smartlink"));
		aktfkanPembayaranViaEsmartlink.setChecked(sekolah.getAktfkanPembayaranViaEsmartlink());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smartlink Username"));
		row.appendChild(usernameEsmartlink = new Textbox(sekolah.getUsernameEsmartlink()));
		usernameEsmartlink.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smartlink Password"));
		row.appendChild(passwordEsmartlink = new Textbox(sekolah.getPasswordEsmartlink()));
		passwordEsmartlink.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Admin Smartlink Default"));
		row.appendChild(biayaAdminEsmartlink = new MyDoublebox(sekolah.getBiayaAdminEsmartlink()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Variable Biaya Admin Smartlink"));
		row.appendChild(variableBiayaAdminEsmartlink = new MyTextbox(sekolah.getVariableBiayaAdminEsmartlink()));
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
		aktfkanPembayaranViaFinpay.setChecked(sekolah.getAktfkanPembayaranViaFinpay());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Finpay API SECRET KEY"));
		row.appendChild(apiKeyFinpay = new Textbox(sekolah.getApiKeyFinpay()));
		apiKeyFinpay.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Finpay VALIDATION TOKEN"));
		row.appendChild(tokenFinpay = new Textbox(sekolah.getTokenFinpay()));
		tokenFinpay.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Admin Finpay"));
		row.appendChild(biayaAdminFinpay = new MyDoublebox(sekolah.getBiayaAdminFinpay()));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("BSI Maja"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja ClientID"));
		row.appendChild(bsiMerchantId = new Textbox(sekolah.getBsiMerchantId()));
		bsiMerchantId.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja SecretKey"));
		row.appendChild(bsiScretId = new Textbox(sekolah.getBsiScretId()));
		bsiScretId.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja username"));
		row.appendChild(bsiUsername = new Textbox(sekolah.getBsiUsername()));
		bsiUsername.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja password"));
		row.appendChild(bsiPassword = new Textbox(sekolah.getBsiPassword()));
		bsiPassword.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("BSI Maja API Endpoint"));
		row.appendChild(bsiGatewayUrl = new Textbox(sekolah.getBsiGatewayUrl()));
		bsiGatewayUrl.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("BJB Syariah"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(aktfkanBjbSyariah = new MyCheckboxConfig("Aktifkan Pembayaran BJB Syariah"));
		aktfkanBjbSyariah.setChecked(sekolah.getAktfkanBjbSyariah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya Admin BJB Syariah"));
		row.appendChild(biayaAdminBjbSyariah = new MyDoublebox(sekolah.getBiayaAdminBjbSyariah()));

		eventListenerKanal.onEvent(null);
		eventListenerdata.onEvent(null);

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

		if (sekolah.getDikunci() != null) {
			Common.freezeGanti(center, true);
		}

	}

	private void initKelengkapanBerkas(Rows rows, Sekolah sekolah) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penjurusan Sekolah"));
		final MyCheckboxConfig formulirVerifikasi;
		row.appendChild(formulirVerifikasi = new MyCheckboxConfig());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Nama Penjurusan Sekolah");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Session session = HibernateUtil.currentSession();
		@SuppressWarnings("unchecked")
		List<PenjurusanSekolah> penjurusanSekolahs = session.createCriteria(PenjurusanSekolah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		if (sekolah != null && sekolah.getId() != null) {
			sekolah = (Sekolah) session.createCriteria(Sekolah.class).add(Restrictions.idEq(sekolah.getId()))
					.uniqueResult();
		}
		try {
			selectedPenjurusanSekolah = sekolah.getPenjurusanSekolahs();
			subGrid.setVisible(!selectedPenjurusanSekolah.isEmpty());
			formulirVerifikasi.setChecked(!selectedPenjurusanSekolah.isEmpty());
		} catch (Exception e) {
			selectedPenjurusanSekolah = new HashSet<PenjurusanSekolah>();
			subGrid.setVisible(!selectedPenjurusanSekolah.isEmpty());
			formulirVerifikasi.setChecked(!selectedPenjurusanSekolah.isEmpty());
		}

		formulirVerifikasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				subGrid.setVisible(formulirVerifikasi.isChecked());
			}
		});

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final PenjurusanSekolah penjurusanSekolah : penjurusanSekolahs) {
			final Checkbox checkbox = new Checkbox(penjurusanSekolah.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedPenjurusanSekolah.contains(penjurusanSekolah));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedPenjurusanSekolah.add(penjurusanSekolah);
					} else {
						selectedPenjurusanSekolah.remove(penjurusanSekolah);
					}
				}
			});
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				penjurusanBolehDipilihSaatPsb = new MyCheckboxConfig("Penjurusan Sekolah Ditentukan Saat PPDB"));
		penjurusanBolehDipilihSaatPsb.setChecked(sekolah.getPenjurusanBolehDipilihSaatPsb());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(penjurusanWajibDipilih = new MyCheckboxConfig("Penjurusan Sekolah Wajib Ditentukan"));
		penjurusanWajibDipilih.setChecked(sekolah.getPenjurusanWajibDipilih());

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisSekolah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (yayasanData == null) {
			if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
				MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		// FIX ConstraintViolationException "duplicate key ... sekolah_domain_sekolah_key": kolom
		// domain_sekolah punya unique constraint di database, tapi sebelum ini tidak ada validasi di
		// sisi form -- akibatnya user baru tahu simpan gagal lewat pesan generik "Sekolah gagal di
		// simpan, click OK untuk download error" tanpa tahu domain mana yang bentrok. Cek dulu di sini
		// supaya pesannya jelas & actionable, sebelum sampai ke Hibernate/DB.
		String domainValue = domain.getValue() == null ? "" : domain.getValue().trim();
		if (!domainValue.isEmpty()) {
			Session sesiCekDomain = HibernateUtil.currentSession();
			Sekolah sekolahDomainSama = (Sekolah) sesiCekDomain.createCriteria(Sekolah.class)
					.add(Restrictions.eq("domain", domainValue))
					.add(sekolah.getId() == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.ne("id", sekolah.getId()))
					.setMaxResults(1).uniqueResult();
			if (sekolahDomainSama != null) {
				MyMessageboxConfig.show(
						"Domain \"" + domainValue + "\" sudah dipakai oleh sekolah \"" + sekolahDomainSama.getNama()
								+ "\". Setiap sekolah harus memiliki domain yang berbeda -- silakan ganti domain lalu simpan kembali.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		KanalPembayaran kanal = (KanalPembayaran) (kanalPembayaran.getSelectedItem() == null ? null
				: kanalPembayaran.getSelectedItem().getValue());

		try {
			Session session = HibernateUtil.currentSession();
			if (sekolah.getId() != null) {
				sekolah = (Sekolah) session.load(Sekolah.class, sekolah.getId());
			}
			sekolah.setKanalPembayaran(kanal);
			sekolah.setBniGatewayUrl(bniGatewayUrl.getValue());
			sekolah.setBniMerchantId(bniMerchantId.getValue());
			sekolah.setBniPassword(bniPassword.getValue());

			sekolah.setNss(nss.getValue());
			sekolah.setNpsn(npsn.getValue());
			sekolah.setNama(nama.getValue());
			sekolah.setJenisSekolah((JenisSekolah) (jenisSekolah.getSelectedItem() == null ? null
					: jenisSekolah.getSelectedItem().getValue()));

			if (yayasanData != null && yayasanData.getId() != null) {
				sekolah.setYayasan(yayasanData);
			} else {
				sekolah.setYayasan(
						(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
			}

			if (pendaftar != null && pendaftar.getId() != null) {
				sekolah.setPendaftar(pendaftar);
			}

			sekolah.setNamaKepalaSekolah(namaKepalaSekolah.getValue());
			sekolah.setNipKepalaSekolah(nipKepalaSekolah.getValue());

			sekolah.setNamaWakilKepalaSekolah(namaWakilKepalaSekolah.getValue());
			sekolah.setNipWakilKepalaSekolah(nipWakilKepalaSekolah.getValue());

			sekolah.setFax(fax.getValue());
			sekolah.setTelp(telp.getValue());
			sekolah.setAlamat(alamat.getValue());
			sekolah.setEmail(email.getValue());
			sekolah.setKeterangan(keterangan.getValue());
			sekolah.setDomain(domain.getValue().trim());
			sekolah.setDeskripsi(deskripsi.getValue());
			sekolah.setMotto(motto.getValue().trim());
			sekolah.setWa(wa.getValue().trim());
			sekolah.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
			sekolah.setPenjurusanSekolahs(selectedPenjurusanSekolah);
			sekolah.setPenjurusanBolehDipilihSaatPsb(penjurusanBolehDipilihSaatPsb.isChecked());
			sekolah.setPenjurusanWajibDipilih(penjurusanWajibDipilih.isChecked());

			sekolah.setCss((String) (css.getSelectedItem() == null ? null : css.getSelectedItem().getValue()));

			if (piilhanTampilanSekolahCb != null && piilhanTampilanSekolahCb.getSelectedItem() != null) {
				sekolah.setPiilhanTampilan((String) piilhanTampilanSekolahCb.getSelectedItem().getValue());
			}

			sekolah.setRt(rt.getValue());
			sekolah.setRw(rw.getValue());
			sekolah.setKodePos(kodePos.getValue());
			sekolah.setDusun(dusun.getValue());
			sekolah.setKelurahan(kelurahan.getValue());
			sekolah.setKecamatan(kecamatan.getValue());
			sekolah.setKabupatenKota(kabupatenKota.getValue());
			sekolah.setPropinsi(propinsi.getValue());
			sekolah.setWebsite(website.getValue());

			sekolah.setPegawai1((Pegawai) pegawai1.getAttribute("pegawai"));
			sekolah.setPegawai2((Pegawai) pegawai2.getAttribute("pegawai"));
			sekolah.setPegawai3((Pegawai) pegawai3.getAttribute("pegawai"));
			sekolah.setPegawai4((Pegawai) pegawai4.getAttribute("pegawai"));
			sekolah.setPegawai5((Pegawai) pegawai5.getAttribute("pegawai"));

			sekolah.setLabelPejabat1(labelPejabat1.getValue());
			sekolah.setLabelPejabat2(labelPejabat2.getValue());
			sekolah.setLabelPejabat3(labelPejabat3.getValue());
			sekolah.setLabelPejabat4(labelPejabat4.getValue());
			sekolah.setLabelPejabat5(labelPejabat5.getValue());

			sekolah.setJawabWhatsappPsb(jawabWhatsappPsb.getValue());
			sekolah.setTanyaWhatsapp(tanyaWhatsapp.getValue());

			sekolah.setAktfkanPembayaranViaFlip(aktfkanPembayaranViaFlip.isChecked());
			sekolah.setApiKeyFlip(apiKeyFlip.getValue().trim());
			sekolah.setTokenFlip(tokenFlip.getValue().trim());
			sekolah.setBiayaAdminFlip(biayaAdminFlip.getValue());

			sekolah.setAktfkanPembayaranViaEsmartlink(aktfkanPembayaranViaEsmartlink.isChecked());
			sekolah.setUsernameEsmartlink(usernameEsmartlink.getValue().trim());
			sekolah.setPasswordEsmartlink(passwordEsmartlink.getValue().trim());
			sekolah.setBiayaAdminEsmartlink(biayaAdminEsmartlink.getValue());
			sekolah.setVariableBiayaAdminEsmartlink(variableBiayaAdminEsmartlink.getValue());

			sekolah.setAktfkanPembayaranViaFinpay(aktfkanPembayaranViaFinpay.isChecked());
			sekolah.setApiKeyFinpay(apiKeyFinpay.getValue().trim());
			sekolah.setTokenFinpay(tokenFinpay.getValue().trim());
			sekolah.setBiayaAdminFinpay(biayaAdminFinpay.getValue());
			sekolah.setGuruHarusPakaiSatuanKerja(guruHarusPakaiSatuanKerja.isChecked());

			sekolah.setBsiMerchantId(bsiMerchantId.getValue());
			sekolah.setBsiGatewayUrl(bsiGatewayUrl.getValue());
			sekolah.setBsiPassword(bsiPassword.getValue());
			sekolah.setBsiScretId(bsiScretId.getValue());
			sekolah.setBsiUsername(bsiUsername.getValue());
			sekolah.setHeaderppdb(headerppdb.getValue());
			sekolah.setAktfkanBjbSyariah(aktfkanBjbSyariah.isChecked());
			sekolah.setSiswaDiizinkanDiPortalYayasan(siswaDiizinkanDiPortalYayasan.isChecked());
			sekolah.setBiayaAdminBjbSyariah(biayaAdminBjbSyariah.getValue());

			if (perguruanTinggi != null) {
				sekolah.setPerguruanTinggi((PerguruanTinggi) (perguruanTinggi.getSelectedItem() == null ? null
						: perguruanTinggi.getSelectedItem().getValue()));
			}

			Common.refreshSaveOrUpdate(session, sekolah);
			session.flush();

			if (bg != null && bg.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(bg);
					bg.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(bg);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (bg_bawah != null && bg_bawah.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(bg_bawah);
					bg_bawah.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(bg_bawah);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (kopStempel != null && kopStempel.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(kopStempel);
					kopStempel.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(kopStempel);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (kop_ppdb != null && kop_ppdb.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(kop_ppdb);
					kop_ppdb.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(kop_ppdb);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (bg_ppdb != null && bg_ppdb.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(bg_ppdb);
					bg_ppdb.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(bg_ppdb);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (bg_utama != null && bg_utama.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(bg_utama);
					bg_utama.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(bg_utama);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (footer_ppdb != null && footer_ppdb.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(footer_ppdb);
					footer_ppdb.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(footer_ppdb);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (logo != null && logo.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(logo);
					logo.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(logo);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();

					Common.checkLogoUpload();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (background != null && background.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(background);
					background.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(background);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();

					Common.checkLogoUpload();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (backgroundLogin != null && backgroundLogin.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(backgroundLogin);
					backgroundLogin.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(backgroundLogin);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();

					Common.checkLogoUpload();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			if (alurppdb != null && alurppdb.getId() != null) {
				try {
					session = StreamingHibernateUtil.getInstance().currentSession();

					session.refresh(alurppdb);
					alurppdb.setRef(sekolah.getId());

					session.getTransaction().begin();
					session.update(alurppdb);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();

					Common.checkLogoUpload();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SekolahAction.reInitByDomain();
				}
			});
		} catch (Exception e) {
			MyMessageboxConfig.show("Sekolah gagal di simpan, click OK untuk download error", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, Common.downloadError(e));
			return false;
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Sekolah current = SekolahUtil.getSekolah();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Sekolah.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(yayasanData != null && yayasanData.getId() != null ? Restrictions.eq("yayasan", yayasanData)
						: Restrictions.sqlRestriction("true"))

				.add(pendaftar != null && pendaftar.getId() != null ? Restrictions.eq("pendaftar", pendaftar)
						: Restrictions.sqlRestriction("true"))

				// Super admin yang mencentang "Tampilkan semua" → SEMUA sekolah, tanpa filter domain.
				.add((abaikanDomain != null && abaikanDomain.isChecked() && Common.getApakahAdminLain(tbmuser))
						|| current == null || current.getId() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("id", current.getId()));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Sekolah> sekolah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(sekolah);
		grid.setRowRenderer(new SekolahRenderer());
		grid.setModelCheckMobile(strset);

	}

}
