package ais.action.master.surat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
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

import ais.action.master.helper.BroadcastHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataPejabatBanbox;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.surat.helper.DasboardSurat;
import ais.action.master.surat.helper.SuratMasukPunyaGambarFotoHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.AlurPersetujuanSuratKeluar;
import ais.database.model.surat.AlurPersetujuanSuratMasuk;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.OpsiSuratMasuk;
import ais.database.model.surat.OpsiSuratMasukValue;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk alur persetujuan surat masuk status. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchjenisjabatan}, {@code MyCheckboxConfig searchbelumsayaajukan}, {@code AmbilDataPejabatBanbox pejabat};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initDetail()}, {@code
 * init()}, {@code initOptional()}, {@code initKelengkapanBerkas()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}, {@code preview()},
 * {@code onPreview()}, {@code onAddExternal()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class AlurPersetujuanSuratMasukStatusAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 *  
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchjenisjabatan;
	private MyCheckboxConfig searchbelumsayaajukan;

	private AmbilDataPejabatBanbox pejabat;
	private MyCheckboxConfig disetujui;
	private Textbox keterangan;

	private boolean edit = false;
	private AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus;
	private MyToolbarbuttonConfig add;
	private MyGrid gridGambar;
	private Vbox vboxAlur;
	private HashSet<JenisJabatan> selectedJenisJabatan;
	private HashSet<JenisJabatan> removedJenisJabatan;

	private Boolean ubahLangsungA = false;
	private Rows rowsOpsiSuratMasuk;
	private JenisJabatan jenisJabatan = null;
	private MyDatebox waktuPersetujuan;
	protected LampiranLain lainMahasiswa;

	private List<JenisJabatan> jenisJabatans = null;
	private JSONObject jenisSurats;
	private boolean ubah = true;
	private MyCheckboxConfig ditolak;
	private Tbmuser tbmuser;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private AmbilDataSatuanKerjaBanbox searchparent;
	private SuratMasuk suratMasuk = null;

	private MyCheckboxConfig blmDisetujui;
	private MyCheckboxConfig telahDisetujui;
	private MyDatebox waktuDitolak;

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

		if (execution.getParameter("ubahLangsung") != null) {
			ubahLangsungA = true;
		}

		tbmuser = Common.getCurrentUser();

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Tbmuser tbmuser = Common.getCurrentUser();

		if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
				&& !tbmuser.hakAkses().getMelihatSemuaSurat()) {

		} else {
			if (searchbelumsayaajukan != null) {
				searchbelumsayaajukan.setVisible(false);
			}
		}

		List<Pejabat> pejabats = null;
		if (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getJenisJabatan() != null) {

			Comboitem comboitem = new Comboitem();
			comboitem.setLabel(tbmuser.hakAkses().getJenisJabatan().getNama());
			comboitem.setValue(tbmuser.hakAkses().getJenisJabatan());
			searchjenisjabatan.appendChild(comboitem);
			searchjenisjabatan.setSelectedItem(comboitem);
			searchjenisjabatan.setDisabled(true);
			pejabats = new ArrayList<Pejabat>();
		} else {

			pejabats = Common.getCurrentPejabat(true);
			if (pejabats != null && !pejabats.isEmpty()) {
				jenisJabatans = new ArrayList<JenisJabatan>();

				for (Pejabat pejabat : pejabats) {
					jenisJabatans.add(pejabat.getJenisJabatan());
				}
				Common.insertComboItems(searchjenisjabatan, "nama", jenisJabatans);

				Comboitem comboitem = new Comboitem();
				comboitem.setLabel("Semua");
				comboitem.setValue(null);
				searchjenisjabatan.appendChild(comboitem);
				searchjenisjabatan.setSelectedItem(comboitem);
				searchjenisjabatan.setReadonly(true);
			} else {
				Common.insertComboDanSemua(searchjenisjabatan, "nama", JenisJabatan.class,
						Restrictions.eq("aktif", true));
			}
		}

		if (!Common.getApakahAdmin() && pejabats == null) {
			return;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				searchparent.setDisabled(false);
				searchparent.setAttribute("satuanKerja", null);
				searchparent.setValue("");

				onSearchDefault(null);
			}
		});

		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		blmDisetujui = new MyCheckboxConfig("Belum Disetujui");
		if (blmDisetujui != null) { blmDisetujui.setChecked(true); }
		telahDisetujui = new MyCheckboxConfig("Telah Disetujui");
		if (telahDisetujui != null) { telahDisetujui.setChecked(true); }

		blmDisetujui.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		telahDisetujui.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Common.appendKeToolbar(blmDisetujui, add, comp);
		Common.appendKeToolbar(telahDisetujui, add, comp);

		String[] contents = new String[] { "id", "alurPersetujuanSuratMasuk", "disetujui", "ditolak", "pejabat",
				"waktuPersetujuan", "jenisJabatan", "telahDirevisi", "catatanDisposisi", "waktuDitolak",
				"suratMasuk.kode", "suratMasuk.noSurat", "suratMasuk.nama", "suratMasuk.status", "suratMasuk.sifat",
				"suratMasuk.kerahasiaan", "suratMasuk.klasifikasiSuratMasuk", "suratMasuk.alurPersetujuanSuratMasuk",
				"suratMasuk.tanggal", "suratMasuk.tanggalSurat", "keterangan", "konseptor", "siswa", "mahasiswa" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(AlurPersetujuanSuratMasukStatus.class, this,
				contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AlurPersetujuanSuratMasukStatusAction}. Kelas ini menerjemahkan
	 * satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AlurPersetujuanSuratMasukStatusAction} dan
	 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AlurPersetujuanSuratMasukStatusAction
	 */
	class AlurPersetujuanSuratMasukStatusRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) arg1;

			final SuratMasuk suratMasuk = alurPersetujuanSuratMasukStatus.getSuratMasuk();

			if (suratMasuk == null) {
				arg0.detach();
				return;
			}

			Component parent = arg0;
			if (ubahLangsungA) {
				parent = new Vbox();
				parent.setParent(arg0);
			}

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(
					suratMasuk.getTanggal() == null ? "" : Common.dateFormat6.get().format(suratMasuk.getTanggal()))
					.setParent(vbox);
			new MyLabelAgakKecil(suratMasuk.getTanggalSurat() == null ? ""
					: Common.dateFormat6.get().format(suratMasuk.getTanggalSurat())).setParent(vbox);

			RevisiHelper.createNewRevisi(AlurPersetujuanSuratMasukStatus.class, alurPersetujuanSuratMasukStatus,
					alurPersetujuanSuratMasukStatus.getSuratMasuk() == null ? ""
							: alurPersetujuanSuratMasukStatus.getSuratMasuk().getKode())
					.setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(suratMasuk.getKlasifikasiSuratMasuk() == null ? ""
					: suratMasuk.getKlasifikasiSuratMasuk().getNama()).setParent(vbox);
			new MyLabelAgakKecil(alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk() == null ? ""
					: alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk().getNama()).setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(suratMasuk.getLoker() == null ? "" : suratMasuk.getLoker().getNama()).setParent(vbox);
			new MyLabelAgakKecil(suratMasuk.getStatus()).setParent(vbox);
			new MyLabelAgakKecil(suratMasuk.getSifat()).setParent(vbox);
			new MyLabelAgakKecil(suratMasuk.getKerahasiaan()).setParent(vbox);

			Vbox myvbox = new Vbox();
			myvbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, alurPersetujuanSuratMasukStatus.getId(),
					AlurPersetujuanSuratMasukStatus.class.getName(), "tindak lanjut Disposisi", false, null, null,
					false, false, false, false);

			new Label(suratMasuk.getPerihal()).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			List<OpsiSuratMasukValue> suratMasukValues = session.createCriteria(OpsiSuratMasukValue.class)
					.add(Restrictions.eq("suratMasuk", suratMasuk)).list();
			List<String> opsiMasukNames = new ArrayList<String>();
			for (OpsiSuratMasukValue v : suratMasukValues) { opsiMasukNames.add(v.getNama()); }
			new ais.ui.util.MyHtml(DasboardSurat.buildOpsiChipsHtmlV20(opsiMasukNames)).setParent(parent);

			List<AlurPersetujuanSuratMasukStatus> alurPersetujuanSuratMasukStatuses;
			if (jenisJabatan != null) {
				alurPersetujuanSuratMasukStatuses = HibernateUtil.currentSession()
						.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
						.add(Restrictions.eq("suratMasuk", alurPersetujuanSuratMasukStatus.getSuratMasuk()))
						.addOrder(Order.asc("id")).list();
			} else {
				alurPersetujuanSuratMasukStatuses = new ArrayList<AlurPersetujuanSuratMasukStatus>();
				alurPersetujuanSuratMasukStatuses.add(alurPersetujuanSuratMasukStatus);
			}

			Vbox hbox21 = new Vbox();
			hbox21.setParent(parent);
			new ais.ui.util.MyHtml(DasboardSurat.buildAlurMasukStatusListHtmlV20(alurPersetujuanSuratMasukStatuses))
					.setParent(hbox21);

			Hbox aa = new Hbox();
			aa.setParent(hbox21);
			LampiranLain.createDownloadUploadFileLain(aa, alurPersetujuanSuratMasukStatus.getId(),
					AlurPersetujuanSuratMasukStatus.class.getName(), "tindak lanjut Disposisi", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							lainMahasiswa = (LampiranLain) arg0.getData();
						}
					});

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Catatan Disposisi", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Lihat catatan disposisi (tabel)");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ais.action.master.surat.helper.CatatanDisposisiPopupHelper.showMasuk(
							alurPersetujuanSuratMasukStatus, tbmuser, (org.zkoss.zk.ui.Component) event.getTarget());
				}
			});
			aksiButtons.add(button);

			JenisJabatan jenisJabatan = alurPersetujuanSuratMasukStatus.getJenisJabatan();

			boolean boleh = false;
			if (jenisJabatan != null) {
				List<Pejabat> jab = Common.getCurrentPejabat(false);
				if (jab != null && !jab.isEmpty()) {
					for (Pejabat pejabat : jab) {
						if (pejabat.getJenisJabatan() != null
								&& pejabat.getJenisJabatan().getId().equals(jenisJabatan.getId())) {
							boleh = true;
							break;
						}
					}
				}
			}

			if (Common.getApakahAdmin()) {
				boleh = true;
			}

			if (!alurPersetujuanSuratMasukStatus.getDisetujui()) {

				if (boleh) {
					button = new MyToolbarbuttonConfig("Tindak Lanjuti", "/img/Check-icon.png");
					button.setOrient("vertical");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(alurPersetujuanSuratMasukStatus);
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					aksiButtons.add(button);

					button = new MyToolbarbuttonConfig("Batalkan", "/img/Check-icon.png");
					button.setOrient("vertical");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {

							MyMessageboxConfig.show("Apakah yakin ingin membatalkan disposisi data ini ?", "Pertanyaan",
									MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
									new EventListener() {

										@Override
										public void onEvent(Event event) throws Exception {
											int i = Integer.parseInt(event.getData().toString());
											if (i == MyMessageboxConfig.OK) {
												try {

													Common.refreshDelete(alurPersetujuanSuratMasukStatus);

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
				}

			} else {
				if (boleh) {
					button = new MyToolbarbuttonConfig("Ubah", "/img/Check-icon.png");
					button.setOrient("vertical");
					button.setTooltiptext("Ubah Data");
					button.setVisible(edit);
					button.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							init(alurPersetujuanSuratMasukStatus);
							addWindow.setVisible(true);
							addWindow.onModal();
						}

					});
					aksiButtons.add(button);
				}

				button = new MyToolbarbuttonConfig("Lihat", "/img/eye-icon.png");
				button.setOrient("vertical");
				button.setTooltiptext("Lihat Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						preview(alurPersetujuanSuratMasukStatus);
						addWindow.setVisible(true);
						addWindow.onModal();
					}
				});
				aksiButtons.add(button);

			}

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new AlurPersetujuanSuratMasukStatus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final SuratMasuk suratMasuk, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		MyTabConfig tabGambar = new MyTabConfig("Lampiran Surat");
		tabGambar.setParent(tabs);

		MyTabConfig tabDisposisi = new MyTabConfig("Disposisi ke");
		tabDisposisi.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelGambar = new ais.ui.util.MyTabpanel();
		tabpanelGambar.setParent(tabpanels);

		Tabpanel tabpanelDisposisi = new ais.ui.util.MyTabpanel();
		tabpanelDisposisi.setParent(tabpanels);

		// edit=true: samakan dgn jalur admin (SuratMasukAction.initDetail) -> tampilkan
		// daftar lampiran surat. Jalur edit=false memicu regenerasi PDF surat keluar
		// (kompilasi belasan JRXML) secara sinkron utk surat internal tanpa scan, yang
		// membuat preview "processing terus" pada login user. Toolbar unggah tetap
		// disembunyikan utk user tanpa hak buat (lihat guard di helper).
		tabpanelGambar.appendChild(
				new SuratMasukPunyaGambarFotoHelper(gridGambar = new MyGrid()).initDetail(suratMasuk, true));

		tabpanelDisposisi.appendChild(SuratMasukAction.initJenisJabatan(suratMasuk, jenisSurats));

	}

	private void preview(AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus) throws Exception {
		this.alurPersetujuanSuratMasukStatus = alurPersetujuanSuratMasukStatus;
		addWindow.setTitle("Preview Surat Masuk");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initDetail(alurPersetujuanSuratMasukStatus.getSuratMasuk(), center);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		// Tombol CETAK di dekat "Selesai": unduh/buka berkas lampiran surat masuk yang sedang dipratinjau.
		final SuratMasuk suratMasukCetak = alurPersetujuanSuratMasukStatus.getSuratMasuk();
		MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
		cetak.setTooltiptext("Cetak / unduh berkas surat masuk");
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					if (suratMasukCetak == null || suratMasukCetak.getId() == null) {
						ais.ui.util.MyMessageboxConfig.show("Data surat masuk tidak ditemukan.", "Informasi",
								ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
						return;
					}
					// FotoGambarSuratMasuk berisi BLOB → WAJIB pakai StreamingHibernateUtil (catatan streaming DB).
					org.hibernate.Session sesiStream = ais.database.hibernate.StreamingHibernateUtil.getInstance()
							.currentSession();
					FotoGambarSuratMasuk foto = (FotoGambarSuratMasuk) sesiStream
							.createCriteria(FotoGambarSuratMasuk.class)
							.add(Restrictions.eq("suratMasuk", suratMasukCetak.getId())).addOrder(Order.desc("id"))
							.setMaxResults(1).uniqueResult();
					if (foto == null) {
						ais.ui.util.MyMessageboxConfig.show(
								"Surat masuk ini belum memiliki berkas lampiran untuk dicetak/diunduh.", "Informasi",
								ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.INFORMATION);
						return;
					}
					if (foto.getGdrive() != null && !foto.getGdrive().isEmpty()) {
						org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrent().sendRedirect(foto.downloadGDriveUrl(), "_blank");
					} else {
						Common.display(foto);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});
		cetak.setParent(toolbar);

		borderlayout.setParent(addWindow);
	}

	@SuppressWarnings("deprecation")
	private void init(final AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus) throws Exception {
		this.suratMasuk = alurPersetujuanSuratMasukStatus.getSuratMasuk();
		selectedJenisJabatan = new HashSet<JenisJabatan>();
		removedJenisJabatan = new HashSet<JenisJabatan>();
		this.alurPersetujuanSuratMasukStatus = alurPersetujuanSuratMasukStatus;
		addWindow.setTitle(alurPersetujuanSuratMasukStatus.getId() == null ? "Tambah Alur Persetujuan Surat Masuk" : "Ubah Alur Persetujuan Surat Masuk");

		try {
			jenisSurats = new JSONObject(alurPersetujuanSuratMasukStatus.getJenisSurats());
		} catch (Exception e) {
			jenisSurats = new JSONObject();
		}

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setWidth("65%");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setAutoscroll(true);

		if (alurPersetujuanSuratMasukStatus.getSuratMasuk() != null) {
			initDetail(alurPersetujuanSuratMasukStatus.getSuratMasuk(), east);
		}

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Disposisi oleh"));
		JenisJabatan jenisJabatan = alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk() == null ? null
				: alurPersetujuanSuratMasukStatus.getJenisJabatan() != null
						? alurPersetujuanSuratMasukStatus.getJenisJabatan()
						: alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk().getJenisJabatan();

		if (jenisJabatan != null) {
			jenisJabatan = alurPersetujuanSuratMasukStatus.getJenisJabatan();
		}

		row.appendChild(pejabat = new AmbilDataPejabatBanbox(jenisJabatan));
		if (alurPersetujuanSuratMasukStatus.getPejabat() != null) {
			pejabat.setAttribute("pejabat", alurPersetujuanSuratMasukStatus.getPejabat());
			pejabat.setValue(alurPersetujuanSuratMasukStatus.getPejabat() == null ? ""
					: alurPersetujuanSuratMasukStatus.getPejabat().toString());
		}
		pejabat.setWidth("90%");

		if (jenisJabatan != null) {
			Pejabat pejabatData = Common.getCurrentPejabat(jenisJabatan);
			if (pejabatData != null) {
				pejabat.setAttribute("pejabat", pejabatData);
				pejabat.setValue(pejabatData.toString());

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig(""));
				Hbox hbox = new Hbox();
				row.appendChild(hbox);
				Vbox vbox1 = new Vbox();
				vbox1.setParent(hbox);

				try {
					CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox1);
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				vbox1.appendChild(new Label(tbmuser.getUserNama()));
			}
		}

		String catatanPimpinan = SuratMasukAction.catatanPimpinanDisposisiMasuk(alurPersetujuanSuratMasukStatus);
		if (catatanPimpinan != null && !catatanPimpinan.trim().isEmpty()) {
			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Catatan / Instruksi Pimpinan"));
			MyLabelAgakKecil catatanPimpinanLabel = new MyLabelAgakKecil(catatanPimpinan);
			catatanPimpinanLabel.setStyle("white-space:pre-wrap;font-weight:600;color:#334155;");
			row.appendChild(catatanPimpinanLabel);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(disetujui = new MyCheckboxConfig(
				"Diterima oleh \"" + (jenisJabatan == null ? "" : jenisJabatan.getNama()) + "\""));
		disetujui.setChecked(alurPersetujuanSuratMasukStatus.getDisetujui());

		waktuDitolak = new MyDatebox(alurPersetujuanSuratMasukStatus.getWaktuDitolak());

		disetujui.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (pejabat.getAttribute("pejabat") == null) {
					MyMessageboxConfig.show("Mohon maaf, Pejabat belum dipilih. Langkah yang dapat dilakukan: (1) pilih pejabat yang akan menyetujui pada kolom Pejabat; (2) pastikan data pejabat tersedia di master data; (3) ulangi proses persetujuan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									disetujui.setChecked(false);
								}

							});
					return;
				}

				if (alurPersetujuanSuratMasukStatus.getId() != null) {
					alurPersetujuanSuratMasukStatus.setDisetujui(disetujui.isChecked());
					alurPersetujuanSuratMasukStatus.setPejabat((Pejabat) pejabat.getAttribute("pejabat"));

				}
				waktuPersetujuan.setDisabled(!disetujui.isChecked());
				alurPersetujuanSuratMasukStatus.setDisetujui(disetujui.isChecked());
				waktuPersetujuan.setValue(alurPersetujuanSuratMasukStatus.getWaktuPersetujuan());

				ditolak.setDisabled(disetujui.isChecked());
				ditolak.setChecked(!disetujui.isChecked());

				waktuDitolak.setDisabled(!ditolak.isChecked());
				alurPersetujuanSuratMasukStatus.setDitolak(ditolak.isChecked());
				waktuDitolak.setValue(alurPersetujuanSuratMasukStatus.getWaktuDitolak());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu"));
		waktuPersetujuan = new MyDatebox(alurPersetujuanSuratMasukStatus.getWaktuPersetujuan());
		
		
		if (waktuPersetujuan.getValue() != null && (alurPersetujuanSuratMasukStatus.getDisetujui() || alurPersetujuanSuratMasukStatus.getDitolak())) {
			row.appendChild(new Label(Common.dateFormat.get().format(waktuPersetujuan.getValue())));
		} else {
			row.appendChild(waktuPersetujuan);
		}
		
		waktuPersetujuan.setReadonly(true);
		waktuPersetujuan.setFormat(Common.dateFormat.get().toPattern());
		waktuPersetujuan.setDisabled(!disetujui.isChecked());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(ditolak = new MyCheckboxConfig(
				"Ditolak oleh \"" + (jenisJabatan == null ? "" : jenisJabatan.getNama()) + "\""));
		ditolak.setChecked(alurPersetujuanSuratMasukStatus.getDitolak());
		ditolak.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (pejabat.getAttribute("pejabat") == null) {
					MyMessageboxConfig.show("Mohon maaf, Pejabat belum dipilih. Langkah yang dapat dilakukan: (1) pilih pejabat yang akan menyetujui pada kolom Pejabat; (2) pastikan data pejabat tersedia di master data; (3) ulangi proses persetujuan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									disetujui.setChecked(false);
								}

							});
					return;
				}

				if (alurPersetujuanSuratMasukStatus.getId() != null) {
					alurPersetujuanSuratMasukStatus.setDitolak(ditolak.isChecked());
					alurPersetujuanSuratMasukStatus.setPejabat((Pejabat) pejabat.getAttribute("pejabat"));

				}

				disetujui.setDisabled(ditolak.isChecked());
				disetujui.setChecked(!ditolak.isChecked());

				waktuPersetujuan.setDisabled(!disetujui.isChecked());
				alurPersetujuanSuratMasukStatus.setDisetujui(disetujui.isChecked());
				waktuPersetujuan.setValue(alurPersetujuanSuratMasukStatus.getWaktuPersetujuan());

				waktuDitolak.setDisabled(!ditolak.isChecked());
				alurPersetujuanSuratMasukStatus.setDitolak(ditolak.isChecked());
				waktuDitolak.setValue(alurPersetujuanSuratMasukStatus.getWaktuDitolak());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal/Waktu Ditolak"));
		row.appendChild(waktuDitolak);
		waktuDitolak.setReadonly(true);
		waktuDitolak.setFormat(Common.dateFormat.get().toPattern());
		waktuDitolak.setDisabled(!ditolak.isChecked());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Uraian *"));
		row.appendChild(keterangan = new Textbox(alurPersetujuanSuratMasukStatus.getKeterangan() == null ? ""
				: alurPersetujuanSuratMasukStatus.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(15);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, alurPersetujuanSuratMasukStatus.getId(),
				AlurPersetujuanSuratMasukStatus.class.getName(), "tindak lanjut Disposisi", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran catatan lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(vboxAlur = new Vbox());

		if (alurPersetujuanSuratMasukStatus.getMasihLanjut()) {
			initKelengkapanBerkas(vboxAlur, alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk());
		}

		if (alurPersetujuanSuratMasukStatus.getSuratMasuk() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Opsi :"));
			initOptional(alurPersetujuanSuratMasukStatus.getSuratMasuk(), row);
		}

		ubah = true;
		SuratMasuk suratMasuk = alurPersetujuanSuratMasukStatus.getSuratMasuk();
		if (alurPersetujuanSuratMasukStatus.getId() != null && suratMasuk != null && suratMasuk.getId() != null) {

			Session session = HibernateUtil.currentSession();
			AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatusNext = (AlurPersetujuanSuratMasukStatus) session
					.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
					.setMaxResults(1).add(Restrictions.gt("id", alurPersetujuanSuratMasukStatus.getId()))
					.add(Restrictions.eq("suratMasuk", suratMasuk)).addOrder(Order.asc("id")).uniqueResult();

			if (alurPersetujuanSuratMasukStatus.getDisetujui() && alurPersetujuanSuratMasukStatusNext != null
					&& alurPersetujuanSuratMasukStatusNext.getDisetujui()) {
				Common.freezeGanti(grid, true);
				ubah = false;
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelStyled("Informasi Disposisi"));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		String html = SuratMasukAction.infoDisposisiBagan(suratMasuk);
		new ais.ui.util.MyHtml(html).setParent(row);

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

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		if (!ubah) {
			save.setVisible(false);
			cancel.setLabel("Tutup");
		}

	}

	@SuppressWarnings("unchecked")
	private void initOptional(final SuratMasuk suratMasuk, Row parent) {

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(parent);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		Tbmuser tbmuser = Common.getCurrentUser();

		Session session = HibernateUtil.currentSession();
		List<OpsiSuratMasuk> opsiSuratMasuks = session.createCriteria(OpsiSuratMasuk.class)
				.add(Restrictions.and(
						Restrictions.or(Restrictions.isNull("usernamePengguna"),
								Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
										MatchMode.ANYWHERE)),
						Restrictions.or(Restrictions.isNull("jenisPengguna"),
								Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
										MatchMode.ANYWHERE))))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();

		rowsOpsiSuratMasuk = new Rows();
		rowsOpsiSuratMasuk.setParent(grid);

		for (OpsiSuratMasuk opsiSuratMasuk : opsiSuratMasuks) {

			OpsiSuratMasukValue opsiSuratMasukValue = null;
			if (suratMasuk.getId() != null) {
				opsiSuratMasukValue = (OpsiSuratMasukValue) session.createCriteria(OpsiSuratMasukValue.class)
						.add(Restrictions.eq("opsiSuratMasuk", opsiSuratMasuk))
						.add(Restrictions.eq("suratMasuk", suratMasuk)).setMaxResults(1).uniqueResult();
			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rowsOpsiSuratMasuk);
			row.setValign("top");
			row.setAttribute("opsiSuratMasuk", opsiSuratMasuk);
			row.setValign("top");
			row.setAttribute("opsiSuratMasukValue", opsiSuratMasukValue);
			final MyCheckboxConfig checkbox;
			row.appendChild(checkbox = new MyCheckboxConfig(opsiSuratMasuk.getNama()));
			row.setValign("top");
			row.setAttribute("checkbox", checkbox);
			checkbox.setChecked(opsiSuratMasukValue != null);
		}

	}

	@SuppressWarnings("unchecked")
	private void initKelengkapanBerkas(Vbox vbox, AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk) {
		Common.clear(vbox);
		if (alurPersetujuanSuratMasuk == null || alurPersetujuanSuratMasuk.getId() == null) {
			return;
		}

		final MyGrid subGrid = new MyGrid();
		vbox.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Kepada Yth. :");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");
		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratMasuk.getId() != null) {
			alurPersetujuanSuratMasuk = (AlurPersetujuanSuratMasuk) session
					.createCriteria(AlurPersetujuanSuratMasuk.class)
					.add(Restrictions.idEq(alurPersetujuanSuratMasuk.getId())).uniqueResult();
		}
		selectedJenisJabatan = new HashSet<JenisJabatan>();
		removedJenisJabatan = new HashSet<JenisJabatan>();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);

		List<JenisJabatan> alurPersetujuanSuratMasuks = session.createCriteria(AlurPersetujuanSuratMasuk.class)
				.setProjection(Projections.groupProperty("jenisJabatan")).add(Restrictions.isNotNull("jenisJabatan"))
				.add(Restrictions.eq("parent", alurPersetujuanSuratMasuk)).list();
		System.out.println("alurPersetujuanSuratMasuks -> " + alurPersetujuanSuratMasuks.size());
		for (JenisJabatan jenisJabatan : alurPersetujuanSuratMasuks) {
			new Label(jenisJabatan.getNama()).setParent(vboxSkala);
		}

		TreeMap<String, JenisJabatan> data = new TreeMap<String, JenisJabatan>();
		for (JenisJabatan jenisJabatan : alurPersetujuanSuratMasuk.getJenisJabatans()) {
			data.put(jenisJabatan.getNama(), jenisJabatan);
		}

		for (final JenisJabatan jenisJabatan : data.values()) {

			AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
					.createCriteria(AlurPersetujuanSuratMasukStatus.class).add(Restrictions.isNotNull("kodeUnik"))
					.add(Restrictions.eq("alurPersetujuanSuratMasuk", alurPersetujuanSuratMasuk))
					.add(Restrictions.eq("suratMasuk",
							AlurPersetujuanSuratMasukStatusAction.this.alurPersetujuanSuratMasukStatus.getSuratMasuk()))
					.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();

			System.out.println("alurPersetujuanSuratMasukStatus => " + alurPersetujuanSuratMasukStatus);

			final Checkbox checkbox = new Checkbox(jenisJabatan.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(alurPersetujuanSuratMasukStatus != null);
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedJenisJabatan.add(jenisJabatan);
						removedJenisJabatan.remove(jenisJabatan);
					} else {
						selectedJenisJabatan.remove(jenisJabatan);
						removedJenisJabatan.add(jenisJabatan);
					}
				}
			});
		}
		data = null;
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (pejabat.getAttribute("pejabat") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pejabat belum dipilih. Langkah yang dapat dilakukan: (1) pilih pejabat yang akan menyetujui pada kolom Pejabat; (2) pastikan data pejabat tersedia di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, Uraian Disposisi belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Uraian pada formulir disposisi; (2) isi uraian atau keterangan yang diperlukan secara jelas; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		List<Row> rowsFotoGambar = null;
		if (gridGambar != null && gridGambar.getRows() != null) {
			rowsFotoGambar = gridGambar.getRows().getChildren();
			for (Row row : rowsFotoGambar) {
				FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) row
						.getAttribute("fotoGambarSuratMasuk");
				if (fotoGambarSuratMasuk.getSuratMasuk() == null) {
					MyMessageboxConfig.show("Mohon maaf, terdapat baris gambar yang belum diisi. Langkah yang dapat dilakukan: (1) periksa daftar gambar pada formulir; (2) hapus baris yang kosong atau unggah file gambar yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}
			}
		}

		Session session = HibernateUtil.currentSession();
		if (alurPersetujuanSuratMasukStatus.getId() != null) {
			alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
					.load(AlurPersetujuanSuratMasukStatus.class, alurPersetujuanSuratMasukStatus.getId());

		}

		if (alurPersetujuanSuratMasukStatus.getId() == null) {
			String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(((Pejabat) pejabat.getAttribute("pejabat")),
					suratMasuk, ((Pejabat) pejabat.getAttribute("pejabat")).getJenisJabatan(), tbmuser,
					tbmuser == null ? null : tbmuser.getMahasiswa(), tbmuser == null ? null : tbmuser.getSiswa());
			System.out.println("suratMasuk -> kodeUnik " + kodeUnik);
			if (kodeUnik != null) {
				alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
						.createCriteria(AlurPersetujuanSuratMasukStatus.class)
						.add(Restrictions.eq("kodeUnik", kodeUnik)).uniqueResult();
			}
		}

		alurPersetujuanSuratMasukStatus.setDitolak(ditolak.isChecked());
		alurPersetujuanSuratMasukStatus.setDisetujui(disetujui.isChecked());
		alurPersetujuanSuratMasukStatus.setPejabat((Pejabat) pejabat.getAttribute("pejabat"));
		alurPersetujuanSuratMasukStatus.setKeterangan(keterangan.getValue());
		alurPersetujuanSuratMasukStatus.setWaktuPersetujuan(waktuPersetujuan.getValue());
		alurPersetujuanSuratMasukStatus.setWaktuDitolak(waktuDitolak.getValue());
		alurPersetujuanSuratMasukStatus.setJenisSurats(jenisSurats.toString());

		if (suratMasuk != null)
			alurPersetujuanSuratMasukStatus.setSuratMasuk(suratMasuk);

		Common.refreshSaveOrUpdate(session, alurPersetujuanSuratMasukStatus);

		// FIX akar masalah GenericJDBCException "could not update ...AlurPersetujuanSuratMasukStatus"
		// (Caused by: "canceling statement due to statement timeout" -> "current transaction is
		// aborted"): UPDATE di atas sebelumnya TIDAK di-flush di sini, sehingga baru benar-benar
		// dieksekusi Hibernate secara LAZY saat auto-flush dipicu oleh query .uniqueResult() yang
		// jauh di bawah (mis. baris ~1114). Kalau baris alur_persetujuan_surat_masuk_status ini
		// sedang dikunci pejabat/proses lain yang bersamaan meng-update kode_unik yang sama (wajar
		// terjadi di alur persetujuan multi-pejabat), PostgreSQL membatalkan statement setelah
		// menunggu terlalu lama -> transaksi jadi "aborted" -> SEMUA query berikutnya di method ini
		// (uniqueResult, save, dst) ikut gagal mentah, bukan hanya update-nya sendiri. Flush di sini
		// membuat kegagalan lock-timeout terjadi & tertangani SEGERA di titik yang jelas, dengan
		// rollback+pesan yang jelas ke user, bukan meledak samar di logika lanjutan yang panjang.
		try {
			session.flush();
		} catch (Exception eFlush) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback,
					"auto-audit(rollback-gagal) src/ais/action/master/surat/AlurPersetujuanSuratMasukStatusAction.java onSave");
			}
			session.clear();
			ais.common.ErrorAuditUtil.record(eFlush,
					"auto-audit(lock-timeout) src/ais/action/master/surat/AlurPersetujuanSuratMasukStatusAction.java onSave");
			MyMessageboxConfig.show(
					"Mohon maaf, data persetujuan surat ini sedang diproses oleh pengguna/pejabat lain di waktu yang bersamaan sehingga penyimpanan gagal. "
							+ "Langkah yang dapat dilakukan: (1) tunggu beberapa saat; (2) muat ulang (refresh) halaman ini; (3) ulangi proses simpan. "
							+ "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (alurPersetujuanSuratMasukStatus.getDitolak()) {
			DasboardSurat.tolak(session, alurPersetujuanSuratMasukStatus);
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				Session sessions = StreamingHibernateUtil.getInstance().currentSession();

				sessions.refresh(lainMahasiswa);
				lainMahasiswa.setRef(alurPersetujuanSuratMasukStatus.getId());

				sessions.getTransaction().begin();
				sessions.update(lainMahasiswa);
				sessions.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (disetujui.isChecked()) {
			// FIX akar masalah GenericJDBCException berantai (KE-1/KE-2/KE-7): blok ini berisi
			// BANYAK save/delete/uniqueResult/list yang berbagi transaksi & session yang SAMA
			// (cascade approval ke banyak pejabat/jenis jabatan). Kalau SATU saja dari operasi ini
			// kena lock-timeout PostgreSQL ("canceling statement due to statement timeout" saat
			// baris alur_persetujuan_surat_masuk_status sedang di-update pejabat lain scr
			// bersamaan), seluruh transaksi jadi "aborted" dan SEMUA query berikutnya di blok ini
			// ikut meledak mentah di titik yang berbeda-beda (persis pola error yang dilaporkan).
			// Bungkus SELURUH blok supaya kegagalan di mana pun titiknya ditangani SATU KALI di
			// sini dengan rollback + pesan jelas, bukan meledak tak tertangani di tengah proses.
			try {
			Iterator<String> enumeration = jenisSurats.keys();
			while (enumeration.hasNext()) {
				try {
					Long idJenis = Long.parseLong(enumeration.next());
					if (idJenis != null && !idJenis.equals(-1L)) {
						Pejabat pejabat = (Pejabat) ConstantValues.ambil(Pejabat.class.getName(), idJenis);
						if (pejabat != null) {

							SuratMasuk suratMasuk = alurPersetujuanSuratMasukStatus.getSuratMasuk();

							AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
									.createCriteria(AlurPersetujuanSuratMasukStatus.class)
									.add(Restrictions.isNotNull("kodeUnik"))
									.add(Restrictions.eq("suratMasuk", suratMasuk))
									.add(Restrictions.eq("pejabat", pejabat))
									.add(Restrictions.eq("jenisJabatan", pejabat.getJenisJabatan())).setMaxResults(1)
									.uniqueResult();

							if (alurPersetujuanSuratMasukStatus == null) {
								String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(pejabat, suratMasuk,
										pejabat.getJenisJabatan(), tbmuser,
										tbmuser == null ? null : tbmuser.getMahasiswa(),
										tbmuser == null ? null : tbmuser.getSiswa());
								if (kodeUnik != null) {
									alurPersetujuanSuratMasukStatus = (AlurPersetujuanSuratMasukStatus) session
											.createCriteria(AlurPersetujuanSuratMasukStatus.class)
											.add(Restrictions.isNotNull("kodeUnik"))
											.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
								}
							}

							if (alurPersetujuanSuratMasukStatus == null && !ditolak.isChecked()
									&& disetujui.isChecked()) {
								alurPersetujuanSuratMasukStatus = new AlurPersetujuanSuratMasukStatus();

								alurPersetujuanSuratMasukStatus.setKonseptor(tbmuser);
								alurPersetujuanSuratMasukStatus
										.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
								alurPersetujuanSuratMasukStatus.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

								alurPersetujuanSuratMasukStatus
										.setAlurPersetujuanSuratMasuk(suratMasuk.getAlurPersetujuanSuratMasuk());
								alurPersetujuanSuratMasukStatus.setSuratMasuk(suratMasuk);
								alurPersetujuanSuratMasukStatus.setJenisJabatan(pejabat.getJenisJabatan());
								alurPersetujuanSuratMasukStatus.setPejabat(pejabat);
								alurPersetujuanSuratMasukStatus.setJenisSurats(jenisSurats.toString());
								session.save(alurPersetujuanSuratMasukStatus);
								session.flush();
							}

//							else if (ditolak.isChecked() && alurPersetujuanSuratMasukStatus != null) {
//								session.delete(alurPersetujuanSuratMasukStatus);
//								session.flush();
//							}

						}
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

			for (JenisJabatan jenisJabatan : selectedJenisJabatan) {
				List<Pejabat> pejabats = ConstantValues.simpleList(session.createCriteria(Pejabat.class)

						.add(Restrictions.or(
								Restrictions.or(
										Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
												MatchMode.ANYWHERE),
										Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
												MatchMode.ANYWHERE)),
								Restrictions.and(
										Restrictions.or(Restrictions.isNotNull("pegawai"),
												Restrictions.or(Restrictions.isNotNull("guru"),
														Restrictions.isNotNull("dosen"))),
										Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
												Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
														Restrictions.eq("guru", tbmuser.getGuru()))))))

						.add(Restrictions.eq("jenisJabatan", jenisJabatan))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Pejabat.class);

				if (pejabats.isEmpty()) {
					pejabats = ConstantValues
							.simpleList(
									session.createCriteria(Pejabat.class)
											.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									Pejabat.class);
				}
				for (Pejabat pejabat : pejabats) {
					AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatusLocal = (AlurPersetujuanSuratMasukStatus) session
							.createCriteria(AlurPersetujuanSuratMasukStatus.class)
							.add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.eq("suratMasuk", alurPersetujuanSuratMasukStatus.getSuratMasuk()))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions.eq("pejabat", pejabat))
							.setMaxResults(1).uniqueResult();

					if (alurPersetujuanSuratMasukStatusLocal == null) {
						String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(pejabat,
								alurPersetujuanSuratMasukStatus.getSuratMasuk(), jenisJabatan, tbmuser,
								tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getSiswa());
						if (kodeUnik != null) {
							alurPersetujuanSuratMasukStatusLocal = (AlurPersetujuanSuratMasukStatus) session
									.createCriteria(AlurPersetujuanSuratMasukStatus.class)
									.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("kodeUnik", kodeUnik))
									.setMaxResults(1).uniqueResult();
						}
					}

					if (alurPersetujuanSuratMasukStatusLocal == null && !ditolak.isChecked() && disetujui.isChecked()) {
						alurPersetujuanSuratMasukStatusLocal = new AlurPersetujuanSuratMasukStatus();

						alurPersetujuanSuratMasukStatusLocal.setKonseptor(tbmuser);
						alurPersetujuanSuratMasukStatusLocal
								.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
						alurPersetujuanSuratMasukStatusLocal.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

						alurPersetujuanSuratMasukStatusLocal
								.setSuratMasuk(alurPersetujuanSuratMasukStatus.getSuratMasuk());
						alurPersetujuanSuratMasukStatusLocal.setJenisJabatan(jenisJabatan);
						alurPersetujuanSuratMasukStatusLocal.setPejabat(pejabat);

						alurPersetujuanSuratMasukStatusLocal.setMasihLanjut(false);
						session.save(alurPersetujuanSuratMasukStatusLocal);
						session.flush();
					}

					else if (ditolak.isChecked() && alurPersetujuanSuratMasukStatusLocal != null) {
						session.delete(alurPersetujuanSuratMasukStatusLocal);
						session.flush();
					}

				}
			}

			List<AlurPersetujuanSuratMasuk> alurPersetujuanSuratMasuksNext = session
					.createCriteria(AlurPersetujuanSuratMasuk.class).add(Restrictions.isNotNull("jenisJabatan"))
					.add(Restrictions.eq("parent", alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk()))
					.list();

			for (AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk : alurPersetujuanSuratMasuksNext) {
				JenisJabatan jenisJabatan = alurPersetujuanSuratMasuk.getJenisJabatan();

				List<Pejabat> pejabats = session.createCriteria(Pejabat.class)

						.add(Restrictions.or(
								Restrictions.or(
										Restrictions.ilike("jenisPengguna", "," + tbmuser.hakAkses().getRoleId() + ",",
												MatchMode.ANYWHERE),
										Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
												MatchMode.ANYWHERE)),
								Restrictions.and(
										Restrictions.or(Restrictions.isNotNull("pegawai"),
												Restrictions.or(Restrictions.isNotNull("guru"),
														Restrictions.isNotNull("dosen"))),
										Restrictions.or(Restrictions.eq("pegawai", tbmuser.getPegawai()),
												Restrictions.or(Restrictions.eq("dosen", tbmuser.getDosen()),
														Restrictions.eq("guru", tbmuser.getGuru()))))))

						.add(Restrictions.eq("jenisJabatan", jenisJabatan))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setMaxResults(1).list();

				if (pejabats.isEmpty()) {
					pejabats = session.createCriteria(Pejabat.class).add(Restrictions.eq("jenisJabatan", jenisJabatan))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1).list();
				}
				for (Pejabat pejabat : pejabats) {
					AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatusLocal = (AlurPersetujuanSuratMasukStatus) session
							.createCriteria(AlurPersetujuanSuratMasukStatus.class)
							.add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.eq("alurPersetujuanSuratMasuk", alurPersetujuanSuratMasuk))
							.add(Restrictions.eq("suratMasuk", alurPersetujuanSuratMasukStatus.getSuratMasuk()))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).add(Restrictions.eq("pejabat", pejabat))
							.setMaxResults(1).uniqueResult();

					if (alurPersetujuanSuratMasukStatusLocal == null) {
						String kodeUnik = AlurPersetujuanSuratMasukStatus.kodeUnik(pejabat,
								alurPersetujuanSuratMasukStatus.getSuratMasuk(), jenisJabatan, tbmuser,
								tbmuser == null ? null : tbmuser.getMahasiswa(),
								tbmuser == null ? null : tbmuser.getSiswa());
						if (kodeUnik != null) {
							alurPersetujuanSuratMasukStatusLocal = (AlurPersetujuanSuratMasukStatus) session
									.createCriteria(AlurPersetujuanSuratMasukStatus.class)
									.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("kodeUnik", kodeUnik))
									.setMaxResults(1).uniqueResult();
						}
					}

					if (alurPersetujuanSuratMasukStatusLocal == null && !ditolak.isChecked() && disetujui.isChecked()) {
						alurPersetujuanSuratMasukStatusLocal = new AlurPersetujuanSuratMasukStatus();

						alurPersetujuanSuratMasukStatusLocal.setKonseptor(tbmuser);
						alurPersetujuanSuratMasukStatusLocal
								.setMahasiswa(tbmuser == null ? null : tbmuser.getMahasiswa());
						alurPersetujuanSuratMasukStatusLocal.setSiswa(tbmuser == null ? null : tbmuser.getSiswa());

						alurPersetujuanSuratMasukStatusLocal.setAlurPersetujuanSuratMasuk(alurPersetujuanSuratMasuk);
						alurPersetujuanSuratMasukStatusLocal
								.setSuratMasuk(alurPersetujuanSuratMasukStatus.getSuratMasuk());
						alurPersetujuanSuratMasukStatusLocal.setJenisJabatan(jenisJabatan);
						alurPersetujuanSuratMasukStatusLocal.setPejabat(pejabat);

						alurPersetujuanSuratMasukStatusLocal.setMasihLanjut(true);

						session.save(alurPersetujuanSuratMasukStatusLocal);
						session.flush();

					}

					else if (ditolak.isChecked() && alurPersetujuanSuratMasukStatusLocal != null) {
						session.delete(alurPersetujuanSuratMasukStatusLocal);
						session.flush();
					}

				}
			}

			if (removedJenisJabatan != null) {
				for (JenisJabatan jenisJabatan : removedJenisJabatan) {
					AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatusLocal = (AlurPersetujuanSuratMasukStatus) session
							.createCriteria(AlurPersetujuanSuratMasukStatus.class)
							.add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.eq("alurPersetujuanSuratMasuk",
									alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk()))
							.add(Restrictions.eq("suratMasuk", alurPersetujuanSuratMasukStatus.getSuratMasuk()))
							.add(Restrictions.eq("jenisJabatan", jenisJabatan)).setMaxResults(1).uniqueResult();
					if (alurPersetujuanSuratMasukStatusLocal != null) {
						session.delete(alurPersetujuanSuratMasukStatusLocal);
					}

				}
			}

			if (disetujui.isChecked()) {
				List<Row> rowsOpsi = rowsOpsiSuratMasuk.getChildren();
				for (Row row : rowsOpsi) {
					MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
					OpsiSuratMasuk opsiSuratMasuk = (OpsiSuratMasuk) row.getAttribute("opsiSuratMasuk");
					OpsiSuratMasukValue opsiSuratMasukValue = (OpsiSuratMasukValue) row
							.getAttribute("opsiSuratMasukValue");
					if (checkbox.isChecked() && opsiSuratMasukValue == null) {
						opsiSuratMasukValue = new OpsiSuratMasukValue();
						opsiSuratMasukValue.setKeterangan(opsiSuratMasuk.getNama());
						opsiSuratMasukValue.setNama(opsiSuratMasuk.getNama());
						opsiSuratMasukValue.setSuratMasuk(alurPersetujuanSuratMasukStatus.getSuratMasuk());
						opsiSuratMasukValue.setOpsiSuratMasuk(opsiSuratMasuk);
						session.save(opsiSuratMasukValue);
					}

					if (!checkbox.isChecked() && opsiSuratMasukValue != null) {
						session.delete(opsiSuratMasukValue);
					}
				}

				session.flush();

				AlurPersetujuanSuratKeluar alurPersetujuanSuratKeluar = alurPersetujuanSuratMasukStatus
						.getAlurPersetujuanSuratMasuk() == null ? null
								: alurPersetujuanSuratMasukStatus.getAlurPersetujuanSuratMasuk()
										.getAlurPersetujuanSuratKeluar();

				if (alurPersetujuanSuratKeluar != null) {

					SuratKeluar suratKeluar = (SuratKeluar) session.createCriteria(SuratKeluar.class)
							.add(Restrictions.eq("suratMasuk", alurPersetujuanSuratMasukStatus.getSuratMasuk()))
							.setMaxResults(1).uniqueResult();
					if (suratKeluar == null) {
						suratKeluar = new SuratKeluar();
						suratKeluar.setAlurPersetujuanSuratKeluar(alurPersetujuanSuratKeluar);
						suratKeluar.setFakultas(alurPersetujuanSuratMasukStatus.getSuratMasuk().getFakultas());
						suratKeluar.setJurusan(alurPersetujuanSuratMasukStatus.getSuratMasuk().getJurusan());
						suratKeluar.setSuratMasuk(alurPersetujuanSuratMasukStatus.getSuratMasuk());
						suratKeluar.setKlasifikasiSuratKeluar(alurPersetujuanSuratMasukStatus
								.getAlurPersetujuanSuratMasuk().getKlasifikasiSuratKeluar());
						suratKeluar.setSatuanKerja(alurPersetujuanSuratMasukStatus.getSuratMasuk().getSatuanKerja());
						suratKeluar.setSekolah(alurPersetujuanSuratMasukStatus.getSuratMasuk().getSekolah());
						suratKeluar.setTanggal(alurPersetujuanSuratMasukStatus.getTanggal_dirubah());
						suratKeluar.setYayasan(alurPersetujuanSuratMasukStatus.getSuratMasuk().getYayasan());
						suratKeluar.setAgenda(alurPersetujuanSuratMasukStatus.getSuratMasuk().getKode());
						suratKeluar.setKode(alurPersetujuanSuratMasukStatus.getSuratMasuk().getKode());
					}

					SuratKeluarAction.onAddExternal(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(arg0);
						}
					}, suratKeluar);

				}

			}

			if (rowsFotoGambar != null && alurPersetujuanSuratMasukStatus.getSuratMasuk() != null) {
				Session mysession = StreamingHibernateUtil.getInstance().currentSession();
				try {
					mysession.getTransaction().begin();
					for (Row row : rowsFotoGambar) {
						FotoGambarSuratMasuk fotoGambarSuratMasuk = (FotoGambarSuratMasuk) row
								.getAttribute("fotoGambarSuratMasuk");
						if (fotoGambarSuratMasuk.getId() == null || fotoGambarSuratMasuk.getSuratMasuk() == null
								|| !fotoGambarSuratMasuk.getSuratMasuk()
										.equals(alurPersetujuanSuratMasukStatus.getSuratMasuk().getId())) {
							fotoGambarSuratMasuk.setSuratMasuk(alurPersetujuanSuratMasukStatus.getSuratMasuk().getId());
							mysession.saveOrUpdate(fotoGambarSuratMasuk);
						}
					}
					mysession.getTransaction().commit();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

				StreamingHibernateUtil.getInstance().closeSession();
			}

			} catch (Exception eDb) {
				try {
					if (session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception eRollback) { ais.common.ErrorAuditUtil.record(eRollback,
						"auto-audit(rollback-gagal) src/ais/action/master/surat/AlurPersetujuanSuratMasukStatusAction.java onSave-disetujui");
				}
				session.clear();
				ais.common.ErrorAuditUtil.record(eDb,
						"auto-audit(lock-timeout) src/ais/action/master/surat/AlurPersetujuanSuratMasukStatusAction.java onSave-disetujui");
				MyMessageboxConfig.show(
						"Mohon maaf, data persetujuan surat ini sedang diproses oleh pengguna/pejabat lain di waktu yang bersamaan sehingga sebagian proses gagal. "
								+ "Langkah yang dapat dilakukan: (1) tunggu beberapa saat; (2) muat ulang (refresh) halaman ini; (3) periksa status persetujuan lalu ulangi jika perlu. "
								+ "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}

		}

		SuratMasukAction.cetakDisposisi(alurPersetujuanSuratMasukStatus, tbmuser);

		Common.createDefaultTimerNoBusy((new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				BroadcastHelper.kirimEmailSuratMasuk(alurPersetujuanSuratMasukStatus.getSuratMasuk(),
						alurPersetujuanSuratMasukStatus, tbmuser);
			}
		}));

		return true;
	}

	public Criteria initCriteria(boolean order) {

		// Null-safe untuk komponen filter yang mungkin belum ter-compose (autowire null),
		// agar initCriteria tidak melempar NullPointerException saat onSearchDefault.
		String c = searchnama == null || searchnama.getValue() == null ? "" : searchnama.getValue().trim();
		SatuanKerja parent = (SatuanKerja) (searchparent == null ? null : searchparent.getAttribute("satuanKerja"));
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			if (satuanKerjaTreeModel != null) {
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}
		}

		jenisJabatan = (JenisJabatan) (searchjenisjabatan == null || searchjenisjabatan.getSelectedItem() == null ? null
				: searchjenisjabatan.getSelectedItem().getValue());

		boolean blmDisetujuiCk = blmDisetujui != null && blmDisetujui.isChecked();
		boolean telahDisetujuiCk = telahDisetujui != null && telahDisetujui.isChecked();
		boolean belumSayaAjukanCk = searchbelumsayaajukan != null && searchbelumsayaajukan.isChecked();

		Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AlurPersetujuanSuratMasukStatus.class)
				.add(Restrictions.isNotNull("kodeUnik"));

		criteria

				.add(!blmDisetujuiCk && !telahDisetujuiCk ? Restrictions.sqlRestriction("false")
						: blmDisetujuiCk && telahDisetujuiCk ? Restrictions.sqlRestriction("true")
								: telahDisetujuiCk ? Restrictions.eq("disetujui", true)
										: blmDisetujuiCk ? Restrictions.eq("disetujui", false)
												: Restrictions.sqlRestriction("true"))

				.createAlias("suratMasuk", "suratMasuk")

				.add(Restrictions.or(Restrictions.isNull("suratMasuk.satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.in("suratMasuk.satuanKerja", satuanKerjas)))

				.add(!belumSayaAjukanCk
						? (jenisJabatan == null || (tbmrole != null && tbmrole.getMelihatSemuaSurat())
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisJabatan", jenisJabatan))
						: tbmrole != null && tbmrole.getRoleId() != null && !tbmrole.getMelihatSemuaSurat()
								? Restrictions.eq("suratMasuk.konseptor", tbmuser)
								: Restrictions.sqlRestriction("true"))

				.add(!belumSayaAjukanCk
						? (jenisJabatans == null || jenisJabatans.isEmpty()
								|| (tbmrole != null && tbmrole.getMelihatSemuaSurat())
										? Restrictions.sqlRestriction("1=1")
										: Restrictions.in("jenisJabatan", jenisJabatans))
						: tbmrole != null && tbmrole.getRoleId() != null && !tbmrole.getMelihatSemuaSurat()
								? Restrictions.eq("suratMasuk.konseptor", tbmuser)
								: Restrictions.sqlRestriction("true"))

				.add(c.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("suratMasuk.noSurat", c, MatchMode.ANYWHERE), Restrictions
								.or(Restrictions.ilike("suratMasuk.perihal", c, MatchMode.ANYWHERE), Restrictions.or(
										Restrictions.ilike("suratMasuk.ringkasan", c, MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("suratMasuk.keterangan", c, MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("suratMasuk.kode", c, MatchMode.ANYWHERE),
														Restrictions.ilike("suratMasuk.nama", c,
																MatchMode.ANYWHERE)))))))

				.add(searchkode.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("suratMasuk.kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));

		if (order)
			criteria.addOrder(Order.desc("suratMasuk.tanggal"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchjenisjabatan == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);
		List<AlurPersetujuanSuratMasukStatus> alurPersetujuanSuratMasukStatus = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(alurPersetujuanSuratMasukStatus);
		grid.setRowRenderer(new AlurPersetujuanSuratMasukStatusRenderer());
		if (ubahLangsungA) {
			grid.setModel(strset);
		} else {
			grid.setModelCheckMobile(strset);
		}

	}

	private EventListener eventListener = null;

	public static void onPreview(AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus) throws Exception {
		AlurPersetujuanSuratMasukStatusAction skripsiAction = new AlurPersetujuanSuratMasukStatusAction();
		skripsiAction.addWindow = new MyWindow();
		skripsiAction.tbmuser = Common.getCurrentUser();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.addWindow);
		skripsiAction.addWindow.setHeight("95%");
		skripsiAction.addWindow.setWidth("90%");

		skripsiAction.preview(alurPersetujuanSuratMasukStatus);

		skripsiAction.addWindow.setVisible(true);
		skripsiAction.addWindow.setClosable(true);
		skripsiAction.addWindow.onModal();
	}

	public static void onAddExternal(EventListener eventListener,
			AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus) throws Exception {
		AlurPersetujuanSuratMasukStatusAction skripsiAction = new AlurPersetujuanSuratMasukStatusAction();
		skripsiAction.tbmuser = Common.getCurrentUser();
		skripsiAction.eventListener = eventListener;
		skripsiAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(skripsiAction.addWindow);
		skripsiAction.addWindow.setHeight("95%");
		skripsiAction.addWindow.setWidth("90%");

		skripsiAction.init(alurPersetujuanSuratMasukStatus);

		skripsiAction.addWindow.setVisible(true);
		skripsiAction.addWindow.setClosable(true);
		skripsiAction.addWindow.onModal();
	}
}
