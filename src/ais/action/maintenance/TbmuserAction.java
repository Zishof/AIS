package ais.action.maintenance;


import ais.common.CommonSearchFilterHelper;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.BiodataCalonMahasiswaAction;
import ais.action.master.pmb.CetakRegistrasiAction;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.DesEncrypter;
import ais.common.ErrorAuditUtil;
import ais.common.MD5;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.OjsHibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KelompokParameterTambahanCalonMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.LogLogin;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanPaket;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Program;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.file.FotoAdmin;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Toko;
import ais.database.model.ojs.Journals;
import ais.database.model.ojs.Roles;
import ais.database.model.ojs.Users;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TbmuserAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7070962748923355504L;
	private MyWindow addWindow;
	private MyGrid grid;

	private Textbox searchnama;
//	private Textbox searchemail;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private MyCheckboxConfig searchFacebook;
	private MyCheckboxConfig searchGoogle;
	private MyCheckboxConfig searchTwitter;
	private MyCheckboxConfig searchLinkedin;
	private Checkbox searchaktif;

	private Textbox userNama;
	private Textbox userid;
	private Textbox userPassword;
	private Combobox userRole;
	private MyCheckboxConfig userShow;
	private AmbilDataPegawaiBanbox pegawai;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataPenyediaAssetBanbox penyedia;
	private Combobox jurusan;
	private Combobox fakultas;
	private Combobox program;
	private Combobox searchrole;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Textbox email;
	private Textbox hp;

	private boolean edit = false;
	private boolean delete = false;

	private Tbmuser tbmuser1;
	private MyToolbarbuttonConfig add;

	private DesEncrypter desEncrypter = new DesEncrypter(Common.DES_PASS_PHRASE);
	private MyCheckboxConfig aktif;
	private EventListener eventListener;

	private MyToolbarbuttonConfig cancel;
	private MyToolbarbuttonConfig save;

	private Paging paging;
	private Tbmuser tbmuser;
	private Row rowPegawai;
	private boolean tampilSederhana;

	private Row rowMediaSosial;
	private Combobox bahasa;
	private Textbox usernameOjs;
	private Combobox yayasan;
	private Combobox sekolah;

	private Hbox hbFakultasLabel;
	private Hbox hbFakultas;

	private Hbox hbYayasanLabel;
	private Hbox hbYayasan;
	protected FotoAdmin fileFoto;
	private Sekolah sekolah1 = null;
	private boolean pt;
	private boolean ya;
	private MyCheckboxConfig memilikiHakAksesTambahan;
	private Combobox userRole2;
	private Combobox userRole3;
	private Combobox userRole4;
	private Combobox userRole5;
	private Label satkerLabel;
	private Pegawai pegawaiData = null;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private Row rowPenyedia;
	private Combobox perguruanTinggi;
	private Combobox toko;
	/** Checkbox "Supervisor" -- hanya relevan/terlihat saat {@link #toko} terisi (pengguna ini pedagang/penjual kantin). Lihat javadoc di titik pembuatannya utk detail. */
	private Checkbox supervisor;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings("unchecked")
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		tampilSederhana = Common.bolehKonfigurasi("tampil_pengguna_sederhana", Konfigurasi.TIDAK_AKTIF);

		if (rowMediaSosial != null) { rowMediaSosial.setVisible(!tampilSederhana); }

		List<Tbmrole> tbmroles = new ArrayList<Tbmrole>();
		Map<Serializable, Tbmrole> map = ConstantValues.ambilBerdasarClass(Tbmrole.class);
		for (Tbmrole tbmrole : map.values()) {
			if (tbmrole != null && tbmrole.getRoleName() != null && tbmrole.getAktif()
					&& !tbmrole.getRoleName().equalsIgnoreCase("Siswa")
					&& !tbmrole.getRoleName().equalsIgnoreCase("Peserta")
					&& !tbmrole.getRoleName().equalsIgnoreCase("Ortu")
					&& !tbmrole.getRoleName().equalsIgnoreCase("Mahasiswa")) {
				tbmroles.add(tbmrole);
			}
		}

		Collections.sort(tbmroles);

		tbmroles.add(null);
		Common.insertComboItems(searchrole, "roleName", tbmroles);
		Common.selectComboItem(searchrole, null);
		if (searchrole != null) { searchrole.setReadonly(true); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// Sembunyikan tombol "Upload Foto (NIM)" bila hak akses (baca+ubah+hapus) tak lengkap.
		ais.common.helper.UploadFotoMassalHelper.terapkanGateTombolUpload(self);

		sekolah1 = SekolahUtil.getSekolah();

		tbmuser = Common.getCurrentUser();
		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1);
		}
		if (hbFakultas != null) {
			hbFakultas.setVisible(pt);
		}
		if (hbYayasanLabel != null) {
			hbYayasanLabel.setVisible(ya);
		}

		if (hbYayasan != null) {
			hbYayasan.setVisible(ya);
		}

		if (sekolah1 != null && sekolah1.getId() != null) {
			if (hbFakultasLabel != null) {
				hbFakultasLabel.setVisible(false);
			}
			if (hbFakultas != null) {
				hbFakultas.setVisible(false);
			}
			if (hbYayasanLabel != null) {
				hbYayasanLabel.setVisible(true);
			}

			if (hbYayasan != null) {
				hbYayasan.setVisible(true);
			}
		}

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (tampilSederhana) {

			Session session = HibernateUtil.currentSession();
			List<KelompokParameterTambahanCalonMahasiswa> kelompokParameterTambahanCalonMahasiswas = session
					.createCriteria(ParameterTambahanPaket.class).createAlias("parameterTambahan", "parameterTambahan")
					.createAlias("kelompokParameterTambahanCalonMahasiswa", "kelompokParameterTambahanCalonMahasiswa")
					.add(Restrictions.eq("parameterTambahan.aktif", true))
					.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
					.setProjection(Projections.groupProperty("kelompokParameterTambahanCalonMahasiswa")).list();
			Collections.sort(kelompokParameterTambahanCalonMahasiswas);

			List<String> columnHeadersAdding = new ArrayList<String>();
			for (KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa : kelompokParameterTambahanCalonMahasiswas) {
				List<ParameterTambahan> parameterTambahans = session.createCriteria(ParameterTambahanPaket.class)
						.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa",
								kelompokParameterTambahanCalonMahasiswa))
						.createAlias("parameterTambahan", "parameterTambahan")
						.createAlias("kelompokParameterTambahanCalonMahasiswa",
								"kelompokParameterTambahanCalonMahasiswa")
						.add(Restrictions.eq("parameterTambahan.aktif", true))
						.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
						.setProjection(Projections.groupProperty("parameterTambahan")).list();
				Collections.sort(parameterTambahans);
				for (final ParameterTambahan parameterTambahan : parameterTambahans) {
					columnHeadersAdding.add(parameterTambahan.getLabelInputan());
				}
			}

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					Tbmuser tbmuser = (Tbmuser) objects[0];
					// Long id = (Long) objects[1];
					XSSFRow row = (XSSFRow) objects[2];

					if (tbmuser.getBiodataCalonMahasiswa() != null) {
						BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null
								: tbmuser.getBiodataCalonMahasiswa();
						int i = 3;

						Session session = HibernateUtil.currentNativeSession();
						List<KelompokParameterTambahanCalonMahasiswa> kelompokParameterTambahanCalonMahasiswas = session
								.createCriteria(ParameterTambahanPaket.class)
								.createAlias("parameterTambahan", "parameterTambahan")
								.createAlias("kelompokParameterTambahanCalonMahasiswa",
										"kelompokParameterTambahanCalonMahasiswa")
								.add(Restrictions.eq("parameterTambahan.aktif", true))
								.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
								.setProjection(Projections.groupProperty("kelompokParameterTambahanCalonMahasiswa"))
								.list();
						Collections.sort(kelompokParameterTambahanCalonMahasiswas);

						for (KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa : kelompokParameterTambahanCalonMahasiswas) {
							List<ParameterTambahan> parameterTambahans = session
									.createCriteria(ParameterTambahanPaket.class)
									.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa",
											kelompokParameterTambahanCalonMahasiswa))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanCalonMahasiswa",
											"kelompokParameterTambahanCalonMahasiswa")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanCalonMahasiswa.aktif", true))
									.setProjection(Projections.groupProperty("parameterTambahan")).list();
							Collections.sort(parameterTambahans);
							for (final ParameterTambahan parameterTambahan : parameterTambahans) {
								final String jenis = kelompokParameterTambahanCalonMahasiswa.getId() + "->"
										+ parameterTambahan.getId();
								String val = "";
								String[] spl = biodataCalonMahasiswa.getParameterTambahanInds().split("\n");
								for (String d : spl) {
									String[] value = d.split("<=>");
									if (value[0].trim().equalsIgnoreCase(jenis)) {
										val = value.length > 1 ? value[1].trim() : "";
									}
								}

								row.createCell(++i).setCellValue(val);
							}
						}
						HibernateUtil.closeSession();
					}

				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Tbmuser.class, this, "Download",
					"/img/print.png", columnHeadersAdding, dataAdding, "id", "userNama", "userRole", "email",
					"usernameOjs");
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		} else {

			final String[] columns = new String[] { "id", "userNama", "userRole", "dosen", "pegawai", "email", "hp",
					"fakultas", "jurusan", "program", "facebookId", "googleId", "twitterId", "linkedinId",
					"usernameOjs", "guru", "siswa", "sekolah", "yayasan", "token", "memilikiHakAksesTambahan",
					"userRole2", "userRole3", "userRole4", "userRole5", "ubahPasword" };

			List<String> columnHeadersAdding = new ArrayList<String>();
			columnHeadersAdding.add("Jumlah Login");
			columnHeadersAdding.add("Sejarah Login");

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					Tbmuser tbmuser = (Tbmuser) objects[0];

					XSSFRow row = (XSSFRow) objects[2];

					Session session = HibernateUtil.currentNativeSession();

					Object[] logLogins = (Object[]) session.createCriteria(LogLogin.class)
							.add(Restrictions.eq("tbmuser", tbmuser)).add(Restrictions.isNotNull("login"))
							.setProjection(Projections.projectionList().add(Projections.max("login"))
									.add(Projections.rowCount()))
							.uniqueResult();

					Date maxLogin = (Date) (logLogins == null || logLogins.length == 0 || logLogins[0] == null ? null
							: logLogins[0]);
					Integer countLogin = ((Number) (logLogins == null || logLogins.length == 0 || logLogins[1] == null
							? 0
							: logLogins[1])).intValue();

					row.createCell(columns.length + 0).setCellValue(countLogin);

					row.createCell(columns.length + 1)
							.setCellValue(maxLogin == null ? "" : Common.dateFormat3.get().format(maxLogin));

					HibernateUtil.closeSession();
				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Tbmuser.class, this,
					"Download Pengguna", "/img/print.png", columnHeadersAdding, dataAdding, false, null,
					"DATA TAMBAHAN", columns);
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			if (Common.getApakahAdmin()) {
				MyToolbarbuttonConfig upload = Common.uploadData(this, Tbmuser.class, columns);
				upload.setVisible((add != null && add.isVisible()) && edit && delete);
				Common.appendKeToolbar(upload, add, comp);
			}

		}

		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Export ke OJS", "/img/corner.gif");
		Common.appendKeToolbar(exportKeOjs, add, comp);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = initCriteria(true).list();

						Session ojSession = OjsHibernateUtil.getInstance().currentSession();
						List<Journals> journals = ojSession.createCriteria(Journals.class).list();
						for (Tbmuser tbmuser : tbmusers) {
							TbmuserAction.updateUser(ojSession, tbmuser, journals);
						}
						OjsHibernateUtil.getInstance().closeSession();

						MyMessageboxConfig.show("Export data ke OJS berhasil dilakukan", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

					}
				});
			}
		});

		MyToolbarbuttonConfig addPeserta = new MyToolbarbuttonConfig("Tambah Peserta", "/img/absensi_pmb.png");
		Common.appendKeToolbar(addPeserta, add, comp);
		addPeserta.setVisible(Common.bolehKonfigurasi("tambah_peserta_pmb_di_menu_pengguna", Konfigurasi.TIDAK_AKTIF));
		addPeserta.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						final BiodataCalonMahasiswaAction biodataCalonMahasiswaAction = new BiodataCalonMahasiswaAction(
								"Calon Mahasiswa", "none", true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												onSearchDefault(arg0);
											}
										});
									}
								});

						page.getFirstRoot().appendChild(biodataCalonMahasiswaAction);
						biodataCalonMahasiswaAction.setWidth("900px");
						biodataCalonMahasiswaAction.setHeight("95%");
						biodataCalonMahasiswaAction.onModal();
					}
				});
			}
		});
	}

	public static void updateUser(Session ojSession, Tbmuser tbmuser, List<Journals> journals) {
		if (tbmuser.getUsernameOjs() != null && !tbmuser.getUsernameOjs().trim().isEmpty()
				&& !tbmuser.getUserNama().trim().isEmpty()) {
			Users users = (Users) ojSession.createCriteria(Users.class)
					.add(Restrictions.eq("username", tbmuser.getUsernameOjs())).setMaxResults(1).uniqueResult();
			if (users == null) {
				String email = tbmuser.getEmail().split(",")[0].trim();
				users = new Users();
				users.setFirstName(Common.maxPanjang(tbmuser.getUserNama(), 40));
				users.setUsername(tbmuser.getUsernameOjs().trim());

				int count = ((Number) ojSession.createCriteria(Users.class).setProjection(Projections.rowCount())
						.add(Restrictions.eq("email", email)).uniqueResult()).intValue();
				if (count > 0) {
					users.setEmail((++Common.increments) + "_" + email);
				} else {
					users.setEmail(email);
				}
			}

			try {
				String password = Common.desEncrypter.get().decrypt(tbmuser.getUserPassword());
				password = MD5.crypt(users.getUsername() + password);
				users.setPassword(password);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			ojSession.getTransaction().begin();
			ojSession.saveOrUpdate(users);
			ojSession.getTransaction().commit();

			for (Journals journal : journals) {
				if (tbmuser.getDosen() != null) {
					int size = ((Number) ojSession.createCriteria(Roles.class).setProjection(Projections.rowCount())
							.add(Restrictions.sqlRestriction("user_id=" + users.getUserId()))
							.add(Restrictions.sqlRestriction("journal_id=" + journal.getJournalId()))
							.add(Restrictions.sqlRestriction("role_id=" + Roles.ROLE_ID_AUTHOR)).uniqueResult())
							.intValue();
					if (size == 0) {
						String sql = "INSERT INTO roles(journal_id, user_id, role_id) VALUES (" + journal.getJournalId()
								+ ", " + users.getUserId() + ", " + Roles.ROLE_ID_AUTHOR + ");";
						ojSession.createSQLQuery(sql).executeUpdate();
					}

					size = ((Number) ojSession.createCriteria(Roles.class).setProjection(Projections.rowCount())
							.add(Restrictions.sqlRestriction("user_id=" + users.getUserId()))
							.add(Restrictions.sqlRestriction("journal_id=" + journal.getJournalId()))
							.add(Restrictions.sqlRestriction("role_id=" + Roles.ROLE_ID_READER)).uniqueResult())
							.intValue();

					if (size == 0) {
						String sql = "INSERT INTO roles(journal_id, user_id, role_id) VALUES (" + journal.getJournalId()
								+ ", " + users.getUserId() + ", " + Roles.ROLE_ID_READER + ");";
						ojSession.createSQLQuery(sql).executeUpdate();
					}

					size = ((Number) ojSession.createCriteria(Roles.class).setProjection(Projections.rowCount())
							.add(Restrictions.sqlRestriction("user_id=" + users.getUserId()))
							.add(Restrictions.sqlRestriction("journal_id=" + journal.getJournalId()))
							.add(Restrictions.sqlRestriction("role_id=" + Roles.ROLE_ID_REVIEWER)).uniqueResult())
							.intValue();

					if (size == 0) {
						String sql = "INSERT INTO roles(journal_id, user_id, role_id) VALUES (" + journal.getJournalId()
								+ ", " + users.getUserId() + ", " + Roles.ROLE_ID_REVIEWER + ");";
						ojSession.createSQLQuery(sql).executeUpdate();
					}
				} else {
					int size = ((Number) ojSession.createCriteria(Roles.class).setProjection(Projections.rowCount())
							.add(Restrictions.sqlRestriction("user_id=" + users.getUserId()))
							.add(Restrictions.sqlRestriction("journal_id=" + journal.getJournalId()))
							.add(Restrictions.sqlRestriction("role_id=" + Roles.ROLE_ID_EDITOR)).uniqueResult())
							.intValue();
					if (size == 0) {
						String sql = "INSERT INTO roles(journal_id, user_id, role_id) VALUES (" + journal.getJournalId()
								+ ", " + users.getUserId() + ", " + Roles.ROLE_ID_EDITOR + ");";
						ojSession.createSQLQuery(sql).executeUpdate();
					}

					size = ((Number) ojSession.createCriteria(Roles.class).setProjection(Projections.rowCount())
							.add(Restrictions.sqlRestriction("user_id=" + users.getUserId()))
							.add(Restrictions.sqlRestriction("journal_id=" + journal.getJournalId()))
							.add(Restrictions.sqlRestriction("role_id=" + Roles.ROLE_ID_LAYOUT_EDITOR)).uniqueResult())
							.intValue();
					if (size == 0) {
						String sql = "INSERT INTO roles(journal_id, user_id, role_id) VALUES (" + journal.getJournalId()
								+ ", " + users.getUserId() + ", " + Roles.ROLE_ID_LAYOUT_EDITOR + ");";
						ojSession.createSQLQuery(sql).executeUpdate();
					}

					size = ((Number) ojSession.createCriteria(Roles.class).setProjection(Projections.rowCount())
							.add(Restrictions.sqlRestriction("user_id=" + users.getUserId()))
							.add(Restrictions.sqlRestriction("journal_id=" + journal.getJournalId()))
							.add(Restrictions.sqlRestriction("role_id=" + Roles.ROLE_ID_SECTION_EDITOR)).uniqueResult())
							.intValue();
					if (size == 0) {
						String sql = "INSERT INTO roles(journal_id, user_id, role_id) VALUES (" + journal.getJournalId()
								+ ", " + users.getUserId() + ", " + Roles.ROLE_ID_SECTION_EDITOR + ");";
						ojSession.createSQLQuery(sql).executeUpdate();
					}
				}
			}

		}
	}

	public static void tampilkanSocialMediaProfile(Vbox vbox, String social) {
		List<String> emailda = new ArrayList<String>();
		for (String profile : StringUtils.split(social, ";")) {
			String[] data = StringUtils.split(profile, "||");
			try {

				String property = StringUtils.split(data[0], ":")[0];
				String linkProfile = data[1];

				String email = "";
				for (String d : data) {
					if (d != null && !d.isEmpty() && StringUtils.contains(d, "@") && StringUtils.contains(d, ".")) {
						email = d;
						break;
					}
				}

				String pro = property.replaceAll("Id", "") + " " + email;
				if (!emailda.contains(pro)) {
					emailda.add(pro);
					A link = new A("Profil " + pro);
					link.setStyle("font-size:9px;");
					link.setHref(linkProfile);
					link.setTarget("_blank");
					vbox.appendChild(link);
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
		emailda = null;
	}

	class UserRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Tbmuser tbmuser = (Tbmuser) arg1;

			CommonMedia.tampilkanGambarKecil(tbmuser).setParent(arg0);

			Vbox vbox = RevisiHelper.createNewRevisi(Tbmuser.class, tbmuser, tbmuser.getUserId());
			vbox.setParent(arg0);

//			TbmuserAction.tampilkanSocialMediaProfile(vbox, tbmuser.getSocialMediaProfile());
			vbox.setParent(arg0);

			new Label(tbmuser.getUserNama()).setParent(arg0);
			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			tbmuser.tampilkanHp(vbox2);
			tbmuser.tampilkanEmail(vbox2);

			vbox2 = new Vbox();
			vbox2.setParent(arg0);

			new Label(tbmuser.getUserRole() == null ? "" : tbmuser.getUserRole().getRoleName()).setParent(vbox2);
			if (tbmuser.getUserRole2() != null) {
				new Label(tbmuser.getUserRole2().getRoleName()).setParent(vbox2);
			}
			if (tbmuser.getUserRole3() != null) {
				new Label(tbmuser.getUserRole3().getRoleName()).setParent(vbox2);
			}
			if (tbmuser.getUserRole4() != null) {
				new Label(tbmuser.getUserRole4().getRoleName()).setParent(vbox2);
			}
			if (tbmuser.getUserRole5() != null) {
				new Label(tbmuser.getUserRole5().getRoleName()).setParent(vbox2);
			}

			if (sekolah1 == null || sekolah1.getId() == null) {
				new Label(tbmuser == null || tbmuser.getFakultas() == null ? "" : tbmuser.getFakultas().getNama())
						.setParent(arg0);
				new Label(tbmuser == null || tbmuser.getJurusan() == null ? "" : tbmuser.getJurusan().getNama())
						.setParent(arg0);
			} else {
				new Label(tbmuser == null || tbmuser.getYayasan() == null ? "" : tbmuser.getYayasan().getNama())
						.setParent(arg0);
				new Label(tbmuser == null || tbmuser.getSekolah() == null ? "" : tbmuser.getSekolah().getNama())
						.setParent(arg0);
			}

			if (tbmuser.getDosen() != null) {
				new Label(tbmuser.getDosen() == null ? "" : tbmuser.getDosen().getNama()).setParent(arg0);
			} else if (tbmuser.getGuru() != null) {
				new Label(tbmuser.getGuru() == null ? "" : tbmuser.getGuru().getNama()).setParent(arg0);
			} else if (tbmuser.getPegawai() != null) {
				new Label(tbmuser.getPegawai() == null ? "" : tbmuser.getPegawai().getNama()).setParent(arg0);
			} else {
				new Label("-").setParent(arg0);
			}

			new Label(tbmuser.getSatuanKerja() == null ? "" : tbmuser.getSatuanKerja().getNama()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(tbmuser.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			if (ConstantValues.pegawai_non_aktif_otomatis_tidak_bisa_login) {
				if (tbmuser.getPegawai() != null && !tbmuser.getPegawai().getAktif()) {
					checkbox.setDisabled(true);
				}
			}

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tbmuser.setAktif(checkbox.isChecked());
					Session session = HibernateUtil.currentSession();
					Common.refreshSaveOrUpdate(session, tbmuser);
				}
			});

			// Kolom aksi rapi: semua tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (tbmuser.getBiodataCalonMahasiswa() != null) {
						CetakRegistrasiAction.onEdit(tbmuser.getBiodataCalonMahasiswa(), TbmuserAction.this);
					} else {
						init(tbmuser);
						addWindow.setVisible(true);
						addWindow.onModal();
					}
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
											Session session = HibernateUtil.currentSession();
											GeneralValueObject.ubahDataHistory(tbmuser, CommonPrivilages.DELETE);
											Common.refreshDelete(session, tbmuser);
											ais.common.newui.NewUiCacheInvalidator.invalidateUser(tbmuser.getUserId());

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("penghapusan data pengguna (Tbmuser)", e,
													new String[] {
															"Periksa apakah akun pengguna ini masih terkait dengan data transaksi/relasi lain (misalnya riwayat login, pengajuan, atau data histori lain) sehingga tidak dapat dihapus.",
															"Nonaktifkan akun ini saja (jika tersedia opsi nonaktif) sebagai alternatif penghapusan permanen.",
															"Hubungi Administrator Sistem apabila data ini memang harus dihapus, agar relasi data terkait ditangani terlebih dahulu." });
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			Vbox aksiBox = ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

			if (ConstantValues.aktifkan_akun_demo) {
				if (tbmuser.getUserId() != null && tbmuser.getUserId().trim().equalsIgnoreCase("demo")) {
					aksiBox.setVisible(false);
				}
			}
		}

	}

	public static void onAddExternal(Event event, EventListener eventListener, Tbmuser tbmuser) throws Exception {
		onAddExternal(event, eventListener, tbmuser, null);
	}

	public static void onAddExternal(Event event, EventListener eventListener, Tbmuser tbmuser, Pegawai pegawai)
			throws Exception {

		if (tbmuser != null && tbmuser.getUserId() != null) {
			try {
				HibernateUtil.currentSession().refresh(tbmuser);
			} catch (org.hibernate.UnresolvableObjectException e) {
				/* Baris pengguna sudah dihapus admin lain sementara objek lama
				 * masih dipegang session (klik "Profil" setelah akun dihapus).
				 * Form tetap dibuka dengan data terakhir yang dipegang; pesan
				 * jelas lebih baik daripada error 500. */
				MyMessageboxConfig.show(
						"Data pengguna ini sudah tidak ditemukan di database (kemungkinan baru saja dihapus). "
								+ "Silakan muat ulang halaman; bila masih muncul, hubungi admin.",
						"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return;
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
				PesanFormalHelper.tampilkanGagalException("pemuatan data profil pengguna (Tbmuser)", e,
						new String[] {
								"Muat ulang halaman ini, kemungkinan data pengguna berubah pada saat yang bersamaan.",
								"Pastikan koneksi ke database/server dalam keadaan stabil, lalu coba buka kembali profil pengguna ini." });
			}
		}

		TbmuserAction tbmuserAction = new TbmuserAction();
		tbmuserAction.pegawaiData = pegawai;
		tbmuserAction.eventListener = eventListener;
		tbmuserAction.addWindow = new MyWindow();
		tbmuserAction.tampilSederhana = Common.bolehKonfigurasi("tampil_pengguna_sederhana", Konfigurasi.TIDAK_AKTIF);

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(tbmuserAction.addWindow);
		tbmuserAction.addWindow.setHeight("95%");
		tbmuserAction.addWindow.setWidth("850px");

		tbmuserAction.init(tbmuser);

		tbmuserAction.addWindow.setVisible(true);
		tbmuserAction.addWindow.onModal();

//		tbmuserAction.userRole.setDisabled(true);
		tbmuserAction.pegawai.setDisabled(true);
		tbmuserAction.fakultas.setDisabled(true);
		tbmuserAction.jurusan.setDisabled(true);
		tbmuserAction.program.setDisabled(true);
		tbmuserAction.userShow.setDisabled(true);
//		tbmuserAction.aktif.setDisabled(true);

	}

	/**
	 * Unggah FOTO MASSAL pengguna: pilih banyak berkas foto (nama berkas = User ID) ATAU satu berkas
	 * ZIP; setiap foto langsung dipasang ke pengguna (FotoAdmin) yang User ID-nya cocok, lalu ringkasan
	 * ditampilkan.
	 */
	/** Unduh SEMUA foto pengguna sebagai satu ZIP (baca BLOB paralel maks 50 thread). Tombol "Download Foto Massal". */
	public void onDownloadFotoMassal(Event event) throws Exception {
		ais.common.helper.DownloadFotoMassalHelper.downloadFotoTbmuserMassal();
	}

	public void onUploadFotoMassal(Event event) throws Exception {
		if (!ais.common.helper.UploadFotoMassalHelper.bolehUploadMassal()) {
			ais.ui.util.MyMessageboxConfig.show(
					"Anda tidak memiliki hak akses (baca, ubah, dan hapus) untuk mengunggah foto.", "Akses Ditolak",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
			return;
		}
		org.zkoss.zk.ui.event.ForwardEvent forwardEvent = (org.zkoss.zk.ui.event.ForwardEvent) event;
		org.zkoss.zk.ui.event.UploadEvent uploadEvent = (org.zkoss.zk.ui.event.UploadEvent) forwardEvent.getOrigin();
		org.zkoss.util.media.Media[] medias = uploadEvent.getMedias();
		java.util.List<org.zkoss.util.media.Media> daftar = new java.util.ArrayList<org.zkoss.util.media.Media>();
		if (medias != null) {
			for (org.zkoss.util.media.Media m : medias) {
				if (m != null) {
					daftar.add(m);
				}
			}
		} else if (uploadEvent.getMedia() != null) {
			daftar.add(uploadEvent.getMedia());
		}
		if (daftar.isEmpty()) {
			ais.ui.util.MyMessageboxConfig.show("Mohon maaf, tidak ada berkas foto yang dipilih. Langkah yang dapat dilakukan: (1) klik tombol upload dan pilih file foto dari komputer Anda; (2) pastikan format file adalah JPG, PNG, atau JPEG; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi",
					ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
			return;
		}
		int[] hasil = ais.common.helper.UploadFotoMassalHelper.uploadFotoTbmuserByNim(daftar);
		ais.ui.util.MyMessageboxConfig.show(ais.common.helper.UploadFotoMassalHelper.ringkasan(hasil),
				"Upload Foto Massal", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
	}

	public void onAdd(Event event) throws Exception {
		init(new Tbmuser());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(final Tbmuser tbmuser) throws Exception {

		this.tbmuser = tbmuser;
		addWindow.setTitle(tbmuser.getId() == null ? "Tambah Pengguna" : "Ubah Pengguna");
		Common.clear(addWindow);
		tbmuser1 = Common.getCurrentUser();

		if (tbmuser.getSatuanKerja() == null && tbmuser1.ambilSatuanKerja() != null) {
			tbmuser.setSatuanKerja(tbmuser1.ambilSatuanKerja());
		}

		if (tbmuser.getJurusan() == null && tbmuser1.ambilJurusan() != null) {
			tbmuser.setJurusan(tbmuser1.ambilJurusan());
		}

		if (tbmuser.getYayasan() == null && tbmuser1.ambilYayasan() != null) {
			tbmuser.setYayasan(tbmuser1.ambilYayasan());
		}

		if (tbmuser.getSekolah() == null && tbmuser1.ambilSekolah() != null) {
			tbmuser.setSekolah(tbmuser1.ambilSekolah());
		}

		Borderlayout borderlayoutUtama = new ais.ui.util.MyBorderlayout();
		addWindow.appendChild(borderlayoutUtama);

		Center centerUtama = new Center();
		centerUtama.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(centerUtama);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabData = new MyTabConfig("Data Pengguna");
		tabData.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		tabpanel.appendChild(borderlayout);

		West west = new West();
		west.setStyle("border:0px;");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("270px");
		west.setParent(borderlayout);
		fileFoto = null;
		Common.createDownloadUploadFoto(west, tbmuser, FotoAdmin.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fileFoto = (FotoAdmin) arg0.getData();
			}
		}, true);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("ID Pengguna"));
		row.appendChild(userid = new Textbox(tbmuser.getUserId()));
		userid.setWidth("90%");
		userid.setDisabled(tbmuser.getUserId() != null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Lengkap"));
		row.appendChild(userNama = new Textbox(tbmuser.getUserNama() == null ? "" : tbmuser.getUserNama()));
		userNama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kata Sandi"));
		userPassword = new Textbox(tbmuser.getUserPassword() == null ? "" : tbmuser.getUserPassword());

		if (tbmuser.getIs_encripted() != null && tbmuser.getIs_encripted()) {
			try {
				userPassword.setValue(
						tbmuser.getUserPassword() == null ? "" : desEncrypter.decrypt(tbmuser.getUserPassword()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		userPassword.setType("password");
		userPassword.setWidth("90%");
		userPassword.setDisabled(tbmuser.getUserId() != null && !Common.getApakahAdmin());
		if (Common.getApakahAdmin()) {
			Hbox passwordBox = new Hbox();
			passwordBox.setWidth("90%");
			passwordBox.setAlign("center");
			passwordBox.setSpacing("4px");
			userPassword.setWidth("100%");
			userPassword.setHflex("1");
			userPassword.setParent(passwordBox);

			final org.zkoss.zul.Toolbarbutton lihatPassword = new org.zkoss.zul.Toolbarbutton();
			lihatPassword.setImage("/img/svg/eye.svg");
			lihatPassword.setTooltiptext("Tampilkan kata sandi");
			lihatPassword.setWidth("30px");
			lihatPassword.setStyle("padding:2px;min-width:30px");
			lihatPassword.setAttribute("passwordTerlihat", Boolean.FALSE);
			lihatPassword.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					boolean terlihat = Boolean.TRUE.equals(lihatPassword.getAttribute("passwordTerlihat"));
					userPassword.setType(terlihat ? "password" : "text");
					lihatPassword.setAttribute("passwordTerlihat", Boolean.valueOf(!terlihat));
					lihatPassword.setTooltiptext(terlihat ? "Tampilkan kata sandi" : "Sembunyikan kata sandi");
				}
			});
			lihatPassword.setParent(passwordBox);
			passwordBox.setParent(row);
		} else {
			userPassword.setParent(row);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new Textbox(tbmuser.getEmail()));
		// email.setConstraint("/.+@.+\\.[a-z]+/: Format email harus sesuai");
		email.setWidth("90%");

		Common.initKeterangan(rows,
				"Jika email lebih dari satu, gunakan tanda koma (,) sebagai pemisah, misal-nya :  anda@mail.com,anda1@oke.com,anda3@mail.com. Sedangkan untuk email utama, tempatkan di urutan paling depan.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp./HP"));
		row.appendChild(hp = new Textbox(tbmuser.getHp()));
		hp.setWidth("90%");

		Criterion criterion = Restrictions.and(
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.and(Restrictions.not(Restrictions.ilike("roleName", "Ortu", MatchMode.EXACT)),
						Restrictions.and(Restrictions.not(Restrictions.ilike("roleName", "Siswa", MatchMode.EXACT)),
								Restrictions.and(
										Restrictions.not(Restrictions.ilike("roleName", "Peserta", MatchMode.EXACT)),
										Restrictions
												.not(Restrictions.ilike("roleName", "Mahasiswa", MatchMode.EXACT))))));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Pengguna / Hak Akses *"));
		row.appendChild(userRole = new Combobox());
		Common.insertComboDanSemua(userRole, new String[] { "roleName" }, "roleName", Tbmrole.class,
				"== Pilih salah satu grup pengguna ==", criterion);
		Common.selectComboItem(true, userRole, tbmuser.getUserRole());

		userRole.setWidth("90%");
		userRole.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampil"));
		row.appendChild(userShow = new MyCheckboxConfig());
		userShow.setChecked(true);
		userShow.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (userShow.isChecked()) {
					userShow.setAttribute("myValue", 1);
				} else {
					userShow.setAttribute("myValue", 0);
				}
			}
		});

		row = new MyFormRow();
		row.setVisible(Common.getApakahAdmin());
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(memilikiHakAksesTambahan = new MyCheckboxConfig("Pengguna memiliki hak akses lebih dari satu"));
		memilikiHakAksesTambahan.setChecked(tbmuser.getMemilikiHakAksesTambahan());


		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hak Akses II"));
		row.appendChild(userRole2 = new Combobox());
		Common.insertComboDanSemua(userRole2, new String[] { "roleName" }, "roleName", Tbmrole.class,
				"== Tidak ada hak akses tambahan ==", criterion);
		Common.selectComboItem(true, userRole2, tbmuser.getUserRole2());
		userRole2.setWidth("90%");
		userRole2.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hak Akses III"));
		row.appendChild(userRole3 = new Combobox());
		Common.insertComboDanSemua(userRole3, new String[] { "roleName" }, "roleName", Tbmrole.class,
				"== Tidak ada hak akses tambahan ==", criterion);
		Common.selectComboItem(true, userRole3, tbmuser.getUserRole3());
		userRole3.setWidth("90%");
		userRole3.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hak Akses IV"));
		row.appendChild(userRole4 = new Combobox());
		Common.insertComboDanSemua(userRole4, new String[] { "roleName" }, "roleName", Tbmrole.class,
				"== Tidak ada hak akses tambahan ==", criterion);
		Common.selectComboItem(true, userRole4, tbmuser.getUserRole4());
		userRole4.setWidth("90%");
		userRole4.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Hak Akses V"));
		row.appendChild(userRole5 = new Combobox());
		Common.insertComboDanSemua(userRole5, new String[] { "roleName" }, "roleName", Tbmrole.class,
				"== Tidak ada hak akses tambahan ==", criterion);
		Common.selectComboItem(true, userRole5, tbmuser.getUserRole5());
		userRole5.setWidth("90%");
		userRole5.setReadonly(true);

		EventListener eventListenerRole = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				userRole2.getParent().setVisible(memilikiHakAksesTambahan.isChecked());
				userRole3.getParent().setVisible(memilikiHakAksesTambahan.isChecked());
				userRole4.getParent().setVisible(memilikiHakAksesTambahan.isChecked());
				userRole5.getParent().setVisible(memilikiHakAksesTambahan.isChecked());

				if (!Common.getApakahAdmin()) {
					userRole2.setDisabled(true);
					userRole3.setDisabled(true);
					userRole4.setDisabled(true);
					userRole5.setDisabled(true);
				}
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(aktif = new MyCheckboxConfig("Pengguna ini aktif (bisa login)"));
		aktif.setChecked(tbmuser.getAktif());

		rowPegawai = new MyFormRow();
		rowPegawai.setStyle("border:0px;background: transparent;");
		rowPegawai.setParent(rows);
		rowPegawai.appendChild(new Label("Pegawai/Dosen/Guru"));
		rowPegawai.appendChild(pegawai = new AmbilDataPegawaiBanbox(false));
		pegawai.setValue(tbmuser.getPegawai() == null ? "" : tbmuser.getPegawai().getNama());
		pegawai.setAttribute("myValue", tbmuser.getPegawai());
		pegawai.setAttribute("pegawai", tbmuser.getPegawai());
		pegawai.setWidth("90%");
		pegawai.setReadonly(true);

		rowPenyedia = new MyFormRow();
		rowPenyedia.setVisible(false);
		rowPenyedia.setStyle("border:0px;background: transparent;");
		rowPenyedia.setParent(rows);
		rowPenyedia.appendChild(new Label(ais.common.Common.getBahasaConfig("Penyedia (Vendor)")));
		rowPenyedia.appendChild(penyedia = new AmbilDataPenyediaAssetBanbox());
		penyedia.setValue(tbmuser.getPenyediaAsset() == null ? "" : tbmuser.getPenyediaAsset().getNama());
		penyedia.setAttribute("myValue", tbmuser.getPenyediaAsset());
		penyedia.setAttribute("penyediaAsset", tbmuser.getPenyediaAsset());
		penyedia.setWidth("90%");
		penyedia.setReadonly(true);

		EventListener userRoleEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				rowPenyedia.setVisible(false);
				Tbmrole tbmrole = (Tbmrole) (userRole.getSelectedItem() == null ? null
						: userRole.getSelectedItem().getValue());
				if (tbmrole != null && tbmrole.getRoleName() != null) {
					rowPenyedia.setVisible(tbmrole.getRoleName().toLowerCase().trim().contains("penyedia"));
				}

				if (!rowPenyedia.isVisible()) {
					penyedia.setAttribute("myValue", null);
					penyedia.setAttribute("penyediaAsset", null);
				} else {
					// email.setDisabled(true);
					// email.getParent().setVisible(false);
					// rowketeranganDosen.setVisible(false);
				}

				if (pegawaiData != null) {
					rowPegawai.setVisible(true);
					pegawai.setAttribute("myValue", pegawaiData);
					pegawai.setAttribute("pegawai", pegawaiData);
					pegawai.setValue(pegawaiData.getNama());
					pegawai.setDisabled(true);
				} else {

					rowPegawai.setVisible(false);

					if (tbmrole != null && tbmrole.getRoleName() != null) {

						rowPegawai.setVisible(tbmrole.getRoleName().toLowerCase().trim().contains("pegawai")
								|| Common.bolehKonfigurasi("integrasi_kepegawaian", Konfigurasi.TIDAK_AKTIF));

						if (!rowPegawai.isVisible()) {
							pegawai.setAttribute("myValue", null);
							pegawai.setAttribute("pegawai", null);
						} else {
							// email.setDisabled(true);
							// email.getParent().setVisible(false);
							// rowketeranganDosen.setVisible(false);
						}
					}
				}
			}
		};

		if (pegawaiData != null) {
			pegawai.setAttribute("myValue", pegawaiData);
			pegawai.setAttribute("pegawai", pegawaiData);
			pegawai.setValue(pegawaiData.getNama());
			pegawai.setDisabled(true);
		}

		userRoleEventListener.onEvent(null);
		userRole.addEventListener("onChange", userRoleEventListener);

		memilikiHakAksesTambahan.addEventListener("onClick", eventListenerRole);
		memilikiHakAksesTambahan.addEventListener("onClick", userRoleEventListener);
		eventListenerRole.onEvent(null);

		satuanKerja = new AmbilDataSatuanKerjaBanbox(true, false);
		satuanKerja
				.setValue(tbmuser.getSatuanKerja() == null
						? (Common.getCurrentUser().getSatuanKerja() == null ? ""
								: Common.getCurrentUser().getSatuanKerja().getNama())
						: tbmuser.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja",
				tbmuser.getSatuanKerja() == null ? tbmuser1.getSatuanKerja() : tbmuser.getSatuanKerja());

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja);
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(
				satkerLabel = new Label(tbmuser.getSatuanKerja() == null ? "" : tbmuser.getSatuanKerja().getNama()));

		perguruanTinggi = new Combobox();
		PerguruanTinggi pta = PerguruanTinggiUtil.getPerguruanTinggi();

		Common.insertComboDanSemua(perguruanTinggi, new String[] { "nama" }, "kode", PerguruanTinggi.class,
				"== Tidak DItentukan ==",
				pta == null || pta.getId() == null ? Restrictions.eq("aktif", true) : Restrictions.idEq(pta.getId()));
		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		Common.selectComboItem(true, perguruanTinggi, tbmuser.getPerguruanTinggi());
		row.appendChild(new ais.ui.util.MyLabelConfig("Perguruan Tinggi"));
		row.appendChild(perguruanTinggi);
		perguruanTinggi.setWidth("90%");
		perguruanTinggi.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		Common.selectComboItem(fakultas,
				tbmuser == null || tbmuser.getFakultas() == null ? tbmuser1.ambilFakultas() : tbmuser.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				tbmuser == null || tbmuser.getJurusan() == null ? tbmuser1.ambilJurusan() : tbmuser.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));

		program = new Combobox();
		Common.insertComboDanSemua(program, "namaBaru", Program.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(program, tbmuser.getProgram() == null ? tbmuser1.ambilProgram() : tbmuser.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		EventListener eventListenerPegawai = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				satkerLabel.getParent().setVisible(false);
				satuanKerja.getParent().setVisible(false);

				Pegawai p = (Pegawai) pegawai.getAttribute("pegawai");
				if (p != null && p.getSatuanKerja() != null) {
					satkerLabel.getParent().setVisible(true);
					satkerLabel.setValue(p.getSatuanKerja().getNama());
				} else {
					satuanKerja.getParent().setVisible(true);
				}

				fakultas.getParent().setVisible(true);
				jurusan.getParent().setVisible(true);
				program.getParent().setVisible(true);

				Tbmrole tbmrole = (Tbmrole) (userRole.getSelectedItem() == null ? null
						: userRole.getSelectedItem().getValue());

				Tbmrole tbmrole2 = (Tbmrole) (userRole2.getSelectedItem() == null ? null
						: userRole2.getSelectedItem().getValue());
				Tbmrole tbmrole3 = (Tbmrole) (userRole3.getSelectedItem() == null ? null
						: userRole3.getSelectedItem().getValue());
				Tbmrole tbmrole4 = (Tbmrole) (userRole4.getSelectedItem() == null ? null
						: userRole4.getSelectedItem().getValue());
				Tbmrole tbmrole5 = (Tbmrole) (userRole5.getSelectedItem() == null ? null
						: userRole5.getSelectedItem().getValue());

				if (tbmrole != null && tbmrole.getRoleId() != null
						&& tbmrole.getRoleId().equalsIgnoreCase(Tbmrole.DOSEN) && tbmrole2 == null && tbmrole3 == null
						&& tbmrole4 == null && tbmrole5 == null) {
					fakultas.getParent().setVisible(false);
					jurusan.getParent().setVisible(false);
					program.getParent().setVisible(false);
				}
			}
		};

		eventListenerPegawai.onEvent(null);
		pegawai.setEventListener(eventListenerPegawai);

		userRole.addEventListener("onChange", eventListenerPegawai);
		userRole2.addEventListener("onChange", eventListenerPegawai);
		userRole3.addEventListener("onChange", eventListenerPegawai);
		userRole4.addEventListener("onChange", eventListenerPegawai);
		userRole5.addEventListener("onChange", eventListenerPegawai);

		boolean tampilRiwayatMediaSosial = Common.bolehKonfigurasi("tampilRiwayatMediaSosial");
		if (tampilRiwayatMediaSosial) {
			if (tbmuser != null && tbmuser.getUserId() != null) {
				MyTabConfig tabSosial = new MyTabConfig("Media Sosial");
				tabSosial.setParent(tabs);
				Tabpanel tabpanelSosial = new ais.ui.util.MyTabpanel();
				tabpanelSosial.setParent(tabpanels);
				Common.displaySocialMedia(tabSosial, tabpanelSosial, tbmuser);
			}
		}

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				tbmuser == null || tbmuser.getYayasan() == null ? tbmuser1.ambilYayasan() : tbmuser.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				tbmuser == null || tbmuser.getSekolah() == null ? tbmuser1.ambilSekolah() : tbmuser.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		if (sekolah1 != null && sekolah1.getId() != null) {
			fakultas.getParent().setVisible(false);
			jurusan.getParent().setVisible(false);
			program.getParent().setVisible(false);

			sekolah.getParent().setVisible(true);
			yayasan.getParent().setVisible(true);
		}

		row = new MyFormRow();
		row.setVisible(ConstantValues.penggunaanLabelBahasa);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
		row.appendChild(bahasa = new Combobox());
		Comboitem comboitem = new Comboitem();
		comboitem.setLabel(Tbmuser.INDONESIA);
		comboitem.setValue(Tbmuser.INDONESIA);
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.ENGLISH);
		comboitem.setValue(Tbmuser.ENGLISH);
		bahasa.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Tbmuser.ARAB);
		comboitem.setValue(Tbmuser.ARAB);
		bahasa.appendChild(comboitem);
		bahasa.setWidth("90%");
		bahasa.setReadonly(true);

		Common.selectComboItem(bahasa, tbmuser.getBahasa());

		if (Common.bolehKonfigurasi("tampilkan_link_login_oleh_admin_di_data_pengguna")) {

			String adminLain = Common.getKonfigurasi("admin_yg_boleh_buka_link_login_pengguna", "").getNilai();
			if (tbmuser.getUserId() != null && !tbmuser.getUserId().isEmpty()
					&& (Common.getApakahAdmin() || Common.getApakahAdmin(adminLain))) {

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Link")));

				final A a = new A("Tampilkan Link");
				a.setHref("");
				row.appendChild(a);

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String userId = tbmuser.getUserId();
						if (userId == null) {
							MyMessageboxConfig.show("Mohon maaf, data atau akun pengguna ini tidak ditemukan di sistem. Langkah yang dapat dilakukan: (1) periksa kembali apakah akun pengguna sudah dibuat dan disimpan; (2) coba segarkan halaman dan cari ulang data pengguna; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						String code = Common.getRequestHostWithProtocol() + "/m?q=" + URLEncoder.encode(
								Common.desEncrypter.get().encrypt(userId + "-user-abcdefghijklmnopqrstuvwxyz"), "UTF-8");
						a.setLabel(code);
						a.setHref(Common.getRequestHostWithProtocol() + "/logoff?param="
								+ URLEncoder.encode(code, "UTF-8"));
					}
				});

				Common.initKeterangan(rows, "Link ini bisa digunakan untuk login tanpa menggunakan password");
			}
		}
		boolean terhubungKeOjs = Common.bolehKonfigurasi("terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF);

		row = new MyFormRow();
		row.setVisible(terhubungKeOjs);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("OJS Username"));
		row.appendChild(usernameOjs = new Textbox(tbmuser.getUsernameOjs()));
		usernameOjs.setWidth("90%");

		toko = new Combobox();
		Common.insertComboDanSemua(toko, new String[] { "nama" }, "kode", Toko.class, "== Tidak Ditentukan ==",
				Restrictions.eq("aktif", true));
		row = new MyFormRow();
		row.setParent(rows);
		Common.selectComboItem(true, toko, tbmuser.getPedagang() == null ? null : tbmuser.getPedagang().getToko());
		row.appendChild(new ais.ui.util.MyLabelConfig("Toko / Penjual (jika penjual di kantin)"));
		row.appendChild(toko);
		toko.setWidth("90%");
		toko.setReadonly(true);

		/**
		 * Checkbox "Supervisor" -- HANYA relevan bila {@link #toko} terisi (pengguna ini pedagang/
		 * penjual kantin, lihat blok simpan {@code Pedagang} di bawah). Supervisor pedagang boleh:
		 * kelola Katalog Barang (tambah/ubah produk) &amp; kelola akun kasir toko ini lewat aplikasi
		 * POS Desktop/Android -- flag disimpan di {@link Pedagang#getSupervisor()} (SUDAH ada &amp;
		 * dipakai {@code KantinHelper.pedagangUbah}/{@code tambahAkunKasir}, sebelumnya HANYA bisa
		 * diatur lewat layar Konfigurasi Desktop terpisah -- sekarang bisa langsung di sini juga saat
		 * admin membuat/mengedit akun pedagang, tanpa langkah tambahan).
		 */
		final Row rowSupervisor = new MyFormRow();
		rowSupervisor.setParent(rows);
		rowSupervisor.appendChild(new ais.ui.util.MyLabelConfig("Supervisor"));
		// FIX layout: Grid form ini hanya didefinisikan 2 kolom (label 40% + isian) --
		// menambah children ke-3 langsung ke Row (checkbox + label deskripsi terpisah)
		// membuat rendering tidak selaras kolom (area kosong besar, checkbox turun ke
		// bawah). Gabungkan checkbox+deskripsi jadi SATU sel ke-2 lewat Hbox, sama
		// seperti pola baris lain di form ini yang selalu 2 children per Row.
		Hbox hboxSupervisor = new Hbox();
		hboxSupervisor.setSpacing("6px");
		hboxSupervisor.setAlign("center");
		rowSupervisor.appendChild(hboxSupervisor);
		supervisor = new Checkbox();
		supervisor.setChecked(tbmuser.getPedagang() != null && Boolean.TRUE.equals(tbmuser.getPedagang().getSupervisor()));
		supervisor.setParent(hboxSupervisor);
		Label supervisorKeterangan = new Label(
				"Boleh kelola Katalog Barang (tambah/ubah produk) dan akun kasir toko ini lewat aplikasi POS Desktop/Android.");
		supervisorKeterangan.setParent(hboxSupervisor);
		final EventListener tampilSupervisorJikaTokoDipilih = new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				boolean tokoDipilih = toko.getSelectedItem() != null && toko.getSelectedItem().getValue() instanceof Toko;
				rowSupervisor.setVisible(tokoDipilih);
				if (!tokoDipilih) {
					supervisor.setChecked(false);
				}
			}
		};
		tampilSupervisorJikaTokoDipilih.onEvent(null);
		toko.addEventListener("onSelect", tampilSupervisorJikaTokoDipilih);
		toko.addEventListener("onChange", tampilSupervisorJikaTokoDipilih);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayoutUtama);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (TbmuserAction.this.eventListener != null) {
						TbmuserAction.this.eventListener.onEvent(event);
					}
				}
			}
		});
		save.setParent(toolbar);

	}

	public boolean onSave(Event event) throws Exception {

		if (userid.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, ID Pengguna belum diisi. Langkah yang dapat dilakukan: (1) isi kolom ID Pengguna dengan username unik; (2) pastikan tidak ada spasi di awal atau akhir; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (userNama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Lengkap pengguna belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Lengkap dengan nama pengguna yang benar; (2) pastikan tidak ada karakter yang tidak valid; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (userPassword.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Password belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Password dengan kata sandi yang kuat; (2) pastikan minimal 6 karakter; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (userRole.getSelectedItem() == null || userRole.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Grup Pengguna belum dipilih. Langkah yang dapat dilakukan: (1) klik dropdown Grup Pengguna dan pilih salah satu grup; (2) pastikan daftar grup telah tersedia di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (email != null && !email.getValue().trim().isEmpty()
				&& !Common.isValidEmailAddress(email.getValue().trim())) {
			MyMessageboxConfig.show("Mohon maaf, format email yang dimasukkan tidak valid. Langkah yang dapat dilakukan: (1) periksa kembali alamat email (contoh: nama@domain.com); (2) pastikan tidak ada spasi atau karakter tidak valid; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		try {
			boolean i = Common.checkUsername(userid.getValue(), tbmuser.getUserId(), null);
			if (i) {
				MyMessageboxConfig.show("Mohon maaf, Username yang dimasukkan sudah digunakan oleh pengguna lain. Langkah yang dapat dilakukan: (1) ganti ID Pengguna dengan nama unik yang berbeda; (2) tambahkan angka atau karakter tambahan agar unik; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/TbmuserAction.java:1547");
			// TODO: handle exception
		}

		Tbmrole tbmrole1 = (Tbmrole) userRole.getSelectedItem().getValue();
		Tbmrole tbmrole2 = (Tbmrole) (userRole2.getSelectedItem() == null ? null
				: userRole2.getSelectedItem().getValue());
		Tbmrole tbmrole3 = (Tbmrole) (userRole3.getSelectedItem() == null ? null
				: userRole3.getSelectedItem().getValue());
		Tbmrole tbmrole4 = (Tbmrole) (userRole4.getSelectedItem() == null ? null
				: userRole4.getSelectedItem().getValue());

		Tbmrole tbmrole5 = (Tbmrole) (userRole5.getSelectedItem() == null ? null
				: userRole5.getSelectedItem().getValue());

		if (Common.bolehKonfigurasi("hanya_admin_yg_boleh_ubah_admin")) {

			Tbmrole tbmrole = tbmuser1 == null ? null : tbmuser1.hakAkses();

			if (!(Common.getApakahAdmin() || (tbmrole != null && tbmrole.getRoleId() != null
					&& Common.getKonfigurasi("admin_yg_boleh_ubah_dan_tambah_admin", "").getNilai().toLowerCase().trim()
							.contains(tbmrole.getRoleId().toLowerCase().trim())))
					&& (

					(tbmrole1 != null && !tbmrole1.getRoleId().equalsIgnoreCase("Dosen")
							&& !tbmrole1.getRoleName().equalsIgnoreCase("Pegawai")
							&& !tbmrole1.getRoleId().equalsIgnoreCase("Guru"))

							|| (tbmrole2 != null && !tbmrole2.getRoleId().equalsIgnoreCase("Dosen")
									&& !tbmrole2.getRoleName().equalsIgnoreCase("Pegawai")
									&& !tbmrole2.getRoleId().equalsIgnoreCase("Guru"))
							|| (tbmrole3 != null && !tbmrole3.getRoleId().equalsIgnoreCase("Dosen")
									&& !tbmrole3.getRoleName().equalsIgnoreCase("Pegawai")
									&& !tbmrole3.getRoleId().equalsIgnoreCase("Guru"))
							|| (tbmrole4 != null && !tbmrole4.getRoleId().equalsIgnoreCase("Dosen")
									&& !tbmrole4.getRoleName().equalsIgnoreCase("Pegawai")
									&& !tbmrole4.getRoleId().equalsIgnoreCase("Guru"))
							|| (tbmrole5 != null && !tbmrole5.getRoleId().equalsIgnoreCase("Dosen")
									&& !tbmrole5.getRoleName().equalsIgnoreCase("Pegawai")
									&& !tbmrole5.getRoleId().equalsIgnoreCase("Guru")))

			) {
				MyMessageboxConfig.show("Mohon maaf, akun yang Anda gunakan tidak memiliki izin untuk membuat jenis pengguna selain Dosen atau Guru. Langkah yang dapat dilakukan: (1) ganti Grup Pengguna menjadi Dosen atau Guru; (2) minta Administrator untuk membuat akun dengan jenis lain; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

//		if (pegawai.getAttribute("pegawai") == null
//				&& ((tbmrole1 != null && !tbmrole1.getRoleId().equalsIgnoreCase("Dosen")
//						&& !tbmrole1.getRoleName().equalsIgnoreCase("Pegawai")
//						&& !tbmrole1.getRoleId().equalsIgnoreCase("Guru"))
//
//						|| (tbmrole2 != null && !tbmrole2.getRoleId().equalsIgnoreCase("Dosen")
//								&& !tbmrole2.getRoleName().equalsIgnoreCase("Pegawai")
//								&& !tbmrole2.getRoleId().equalsIgnoreCase("Guru"))
//						|| (tbmrole3 != null && !tbmrole3.getRoleId().equalsIgnoreCase("Dosen")
//								&& !tbmrole3.getRoleName().equalsIgnoreCase("Pegawai")
//								&& !tbmrole3.getRoleId().equalsIgnoreCase("Guru"))
//						|| (tbmrole4 != null && !tbmrole4.getRoleId().equalsIgnoreCase("Dosen")
//								&& !tbmrole4.getRoleName().equalsIgnoreCase("Pegawai")
//								&& !tbmrole4.getRoleId().equalsIgnoreCase("Guru"))
//						|| (tbmrole5 != null && !tbmrole5.getRoleId().equalsIgnoreCase("Dosen")
//								&& !tbmrole5.getRoleName().equalsIgnoreCase("Pegawai")
//								&& !tbmrole5.getRoleId().equalsIgnoreCase("Guru")))) {
//			MyMessageboxConfig.show(
//					"Untuk hak akses guru atau dosen atau pegawai harus diisi data salah satu pegawai-nya",
//					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		if (rowPenyedia != null && rowPenyedia.isVisible()) {
			if (penyedia != null && penyedia.getAttribute("penyediaAsset") == null) {
				MyMessageboxConfig.show("Mohon maaf, Penyedia belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Penyedia dengan memilih atau memasukkan data penyedia; (2) pastikan data penyedia telah terdaftar di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		Session sessionData = HibernateUtil.currentSession();
		if (tbmuser.getUserId() != null) {
			tbmuser = (Tbmuser) sessionData.load(Tbmuser.class, tbmuser.getUserId());
		} else {
			tbmuser = new Tbmuser();
		}

		tbmuser.setUserNama(userNama.getValue());
		tbmuser.setUserPassword(desEncrypter.encrypt(userPassword.getValue().trim()));
		tbmuser.setIs_encripted(true);
		tbmuser.setPegawai((Pegawai) pegawai.getAttribute("pegawai"));
		tbmuser.setUserRole(tbmrole1);
		tbmuser.setUserShow(userShow.isChecked() ? 1 : 0);

		tbmuser.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		tbmuser.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		tbmuser.setProgram(
				(Program) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));

		tbmuser.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		tbmuser.setEmail(email.getValue().trim());
		tbmuser.setUserShow(1);
		tbmuser.setAktif(aktif.isChecked());
		tbmuser.setMahasiswa(null);
		tbmuser.setBahasa((String) bahasa.getSelectedItem().getValue());
		tbmuser.setUsernameOjs(usernameOjs.getValue());
		tbmuser.setYayasan((Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		tbmuser.setSekolah((Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		tbmuser.setPerguruanTinggi((PerguruanTinggi) (perguruanTinggi.getSelectedItem() == null ? null
				: perguruanTinggi.getSelectedItem().getValue()));

		tbmuser.setHp(hp.getValue());

		tbmuser.setMemilikiHakAksesTambahan(memilikiHakAksesTambahan.isChecked());

		tbmuser.setUserRole2(tbmrole2);
		tbmuser.setUserRole3(tbmrole3);
		tbmuser.setUserRole4(tbmrole4);
		tbmuser.setUserRole5(tbmrole5);
		if (penyedia != null) {
			tbmuser.setPenyediaAsset((PenyediaAsset) penyedia.getAttribute("penyediaAsset"));
		}
		if (pegawaiData != null) {
			tbmuser.setPegawai(pegawaiData);
		}

		GeneralValueObject.ubahDataHistory(tbmuser);

		if (tbmuser.getUserId() != null) {
			Common.refreshSaveOrUpdate(sessionData, tbmuser);
		} else {
			tbmuser.setUserId(userid.getValue().trim());
			sessionData.save(tbmuser);
		}

		Toko dataToko = (Toko) (toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue());
		if (dataToko != null) {
			try {
				sessionData.flush();
				Pedagang pedagang = tbmuser.getPedagang();
				if (pedagang == null) {
					pedagang = (Pedagang) sessionData.createCriteria(Pedagang.class)
							.add(Restrictions.eq("tbmuser", tbmuser))
							.add(Restrictions.eq("toko", dataToko)).setMaxResults(1).uniqueResult();
				}
				boolean pedagangBaru = pedagang == null;
				if (pedagangBaru) pedagang = new Pedagang();
				pedagang.setTbmuser(tbmuser);
				pedagang.setToko(dataToko);
				pedagang.setSupervisor(Boolean.valueOf(supervisor != null && supervisor.isChecked()));
				if (pedagangBaru) sessionData.save(pedagang); else sessionData.saveOrUpdate(pedagang);
				sessionData.flush();

				tbmuser.setPedagang(pedagang);
				sessionData.flush();
			} catch (Exception e) {
				try {
					if (sessionData.getTransaction() != null && sessionData.getTransaction().isActive()) {
						sessionData.getTransaction().rollback();
					}
				} catch (Exception rollbackError) {
					ErrorAuditUtil.record(rollbackError,
							"Gagal rollback penyimpanan Toko / Penjual pada TbmuserAction.onSave");
				}
				Common.tampilErrorJikaAdmin(e);
				PesanFormalHelper.tampilkanGagalException("penyimpanan Toko / Penjual pada akun pengguna", e,
						new String[] { "Pastikan toko masih aktif dan belum dihapus.",
								"Periksa tabel audit Pedagang bila struktur database baru saja diperbarui.",
								"Data tidak dinyatakan berhasil agar transaksi database dapat dibatalkan dengan aman." });
				return false;
			}
		} else if (dataToko == null && tbmuser.getPedagang() != null) {
			tbmuser.setPedagang(null);
			Common.refreshUpdate(tbmuser);
		}

		if (fileFoto != null && fileFoto.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(fileFoto);
				fileFoto.setTbmuser(tbmuser.getUserId());

				session.getTransaction().begin();
				session.update(fileFoto);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
				PesanFormalHelper.tampilkanGagalException("penyimpanan foto pengguna (Tbmuser)", e,
						new String[] {
								"Periksa kembali berkas foto yang diunggah (format dan ukuran file) lalu coba simpan kembali.",
								"Data pengguna lainnya tetap tersimpan; hanya foto yang gagal diperbarui.",
								"Ulangi proses simpan beberapa saat lagi, kemungkinan kendala penyimpanan berkas bersifat sementara." });
			}

		}

		Tbmuser.getUserRoleYgDipakai.put(tbmuser.getUserId(), tbmuser.getUserRole());

		Common.saveOrUpdateUserAccess(tbmuser, null, tbmuser.getUserId(), userPassword.getValue().trim(),
				tbmuser.getEmail());
		ais.common.newui.NewUiCacheInvalidator.invalidateUser(tbmuser.getUserId());

		return true;
	}

	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Tbmuser.class)

				.add(Restrictions.isNull("orangTua"))

				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								parent == null ? Restrictions.isNull("satuanKerja")
										: Restrictions.sqlRestriction("false"),
								Restrictions.in("satuanKerja", satuanKerjas)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchFacebook.isChecked()
						? Restrictions.and(Restrictions.isNotNull("facebookId"), Restrictions.ne("facebookId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchGoogle.isChecked()
						? Restrictions.and(Restrictions.isNotNull("googleId"), Restrictions.ne("googleId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchTwitter.isChecked()
						? Restrictions.and(Restrictions.isNotNull("twitterId"), Restrictions.ne("twitterId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchLinkedin.isChecked()
						? Restrictions.and(Restrictions.isNotNull("linkedinId"), Restrictions.ne("linkedinId", ""))
						: Restrictions.sqlRestriction("true"))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("email", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("userNama", searchnama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("userId", searchnama.getValue().trim(),
												MatchMode.ANYWHERE))))

				.add(searchrole.getSelectedItem() == null || searchrole.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						:

						Restrictions.or(Restrictions.eq("userRole", searchrole.getSelectedItem().getValue()),
								Restrictions.or(Restrictions.eq("userRole5", searchrole.getSelectedItem().getValue()),
										Restrictions.or(
												Restrictions.eq("userRole4", searchrole.getSelectedItem().getValue()),
												Restrictions.or(
														Restrictions.eq("userRole3",
																searchrole.getSelectedItem().getValue()),
														Restrictions.eq("userRole2",
																searchrole.getSelectedItem().getValue()))))

						)

				)

				.createAlias("userRole", "userRole")

				.add(Restrictions.and(Restrictions.ne("userRole.roleId", Tbmrole.ORANG_TUA),
						Restrictions.and(Restrictions.ne("userRole.roleId", Tbmrole.SISWA),
								Restrictions.and(Restrictions.ne("userRole.roleId", Tbmrole.PESERTA_KURSUS),
										Restrictions.ne("userRole.roleId", Tbmrole.MAHASISWA)))))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("yayasan"),
										CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)));

		if (order)
			criteria.addOrder(Order.asc("userNama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<Tbmuser> tbmuser = ConstantValues
				.simpleList(
						initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE).setFirstResult(
								Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())),
						Tbmuser.class);

		ListModel strset = new SimpleListModel(tbmuser);
		grid.setRowRenderer(new UserRenderer());
		grid.setModelCheckMobile(strset);

	}
}
