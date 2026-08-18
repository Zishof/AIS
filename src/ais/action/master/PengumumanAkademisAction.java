package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
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
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.maintenance.MainAction;
import ais.action.maintenance.ProfileAction;
import ais.action.master.helper.DetailPengumumanAkademisHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.TampilanPengumumanPMBAction;
import ais.action.master.psb.TampilanPengumumanPSBAction;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.AIGenerator;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.calendar.CalendarUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KategoriPengumuman;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

public class PengumumanAkademisAction extends GenericAutowireComposer implements DataInitDefault {
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyWindow addWindowAttachment;
	private MyGrid grid;

	private Textbox judul;
	private MyCkEditor catatan;
	private Combobox diperuntukkan;
	private MyDatebox tanggal;
	private MyDatebox sampai;
	private MyCheckboxConfig galeryBerupaHtml;

	private Combobox jurusan;
	private Combobox fakultas;
	private Combobox program;
	private Textbox searchjudul;
	private Textbox searchisi;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchfakultas;
	private Combobox searchPerguruanTinggi;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox searchKategoriPengumuman;

	private Checkbox searchaktif;
	private Label labelFakProd;
	private Label labelYaySek;
	private Hbox fakProd;
	private Hbox yaySek;
	private Combobox searchTahunAjaran;
	private Combobox searchDiperuntukkan;
	private Combobox tahunAjaran;
	private MyCheckboxConfig aktif;
	private MyCheckboxConfig bolehDiberiKomentar;
	private MyCheckboxConfig adaVideoConference;
	private MyCheckboxConfig adaVideoConferenceGoogleMeet;
	private MyCheckboxConfig broadcastKeMahasiswaAktif;
	private MyCheckboxConfig broadcastKeDosen;
	private MyCheckboxConfig broadcastAdmin;
	private MyCheckboxConfig broadcastCalonMahasiswa;
	private MyCheckboxConfig tampilkanPengumumanLain;
	private MyCheckboxConfig tampilkanProfile;
	private MyCheckboxConfig langsungMunculDiTab;
	private MyCheckboxConfig langsungTampilBeranda;
	private PengumumanAkademis pengumumanAkademis;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private Textbox korespondensi;

	private Tabpanel pengumumanPerkuliahan;
	private Tabpanel penumumanWebsiteTab;
	private Tabpanel kategoriPengumumanTab;
	private Tabpanel teksBerjalanTab;
	private Textbox tinggiGaleriMobile;

	private DetailPengumumanAkademisHelper detailPengumumanAkademisHelper = new DetailPengumumanAkademisHelper();
	private MyCheckboxConfig broadcastKeMahasiswaAlumni;
	private MyCheckboxConfig broadcastKeMahasiswaCuti;
	private boolean tampilSederhana;

	public void onPengumumanPerkuliahan(Event event) {
		if (pengumumanPerkuliahan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(pengumumanPerkuliahan);
			MyInclude iframe = new MyInclude("/pages/master/pengumuman_perkuliahan.zul");
			iframe.setParent(window);
		}
	}

	public void onPenumumanWebsite(Event event) {
		if (penumumanWebsiteTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(penumumanWebsiteTab);
			MyInclude iframe = new MyInclude("/pages/master/penumuman_website.zul");
			iframe.setParent(window);
		}
	}

	public void onKategoriPengumuman(Event event) {
		if (kategoriPengumumanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kategoriPengumumanTab);
			MyInclude iframe = new MyInclude("/pages/master/kategori_pengumuman.zul");
			iframe.setParent(window);
		}
	}

	public void onTeksBerjalan(Event event) {
		if (teksBerjalanTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(teksBerjalanTab);
			MyInclude iframe = new MyInclude("/pages/master/text_berjalan.zul");
			iframe.setParent(window);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private void diperuntukkan(Combobox diperuntukkan) {
		MyComboitemConfig comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_UMUM);
		comboitem.setValue(PengumumanAkademis.UNTUK_UMUM);
		diperuntukkan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_ADMIN);
		comboitem.setValue(PengumumanAkademis.UNTUK_ADMIN);
		diperuntukkan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_VENDOR);
		comboitem.setValue(PengumumanAkademis.UNTUK_VENDOR);
		diperuntukkan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_KARIR);
		comboitem.setValue(PengumumanAkademis.UNTUK_KARIR);
		diperuntukkan.appendChild(comboitem);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		if (!tampilSederhana) {

			if (ya) {

				comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_SISWA);
				comboitem.setValue(PengumumanAkademis.UNTUK_SISWA);
				diperuntukkan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_GURU);
				comboitem.setValue(PengumumanAkademis.UNTUK_GURU);
				diperuntukkan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_CALON_SISWA);
				comboitem.setValue(PengumumanAkademis.UNTUK_CALON_SISWA);
				diperuntukkan.appendChild(comboitem);

			}

			if (pt) {
				comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_DOSEN);
				comboitem.setValue(PengumumanAkademis.UNTUK_DOSEN);
				diperuntukkan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_MAHASISWA);
				comboitem.setValue(PengumumanAkademis.UNTUK_MAHASISWA);
				diperuntukkan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_ALUMNI);
				comboitem.setValue(PengumumanAkademis.UNTUK_ALUMNI);
				diperuntukkan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_CALON_MAHASISWA);
				comboitem.setValue(PengumumanAkademis.UNTUK_CALON_MAHASISWA);
				diperuntukkan.appendChild(comboitem);

				comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_PERPUSTAKAAN);
				comboitem.setValue(PengumumanAkademis.UNTUK_PERPUSTAKAAN);
				diperuntukkan.appendChild(comboitem);

			}
		} else {
			comboitem = new MyComboitemConfig(PengumumanAkademis.UNTUK_PESERTA);
			comboitem.setValue(PengumumanAkademis.UNTUK_PESERTA);
			diperuntukkan.appendChild(comboitem);
		}
	}

	// private Label value_jurusan;
	// private Label value_fakultas;
	private Label value_tahun_akademik;

	private MyColumnConfig label_tahun_akademik;

	private Textbox hanyaUntuk;
	private Textbox hanyaUntukAngkatan;
	private MyCheckboxConfig tetapTampilkanPengumumanMeskipunSudahKelewat;
	private Row rowHanyaUntuk;
	private Row rowHanyaUntukKeterangan;
	private Row rowHanyaUntukKeteranganAdmin;
	private Row rowHanyaUntukAngkatan;
	private Combobox kategoriPengumuman;
	private boolean pt;
	private boolean ya;
	private Combobox yayasan;
	private Combobox sekolah;
	private MyCheckboxConfig broadcastKeSiswaAktif;
	private MyCheckboxConfig broadcastKeGuru;
	private Tbmuser tbmuser;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Common.insertComboDanSemua(searchPerguruanTinggi, "nama", PerguruanTinggi.class,
				Restrictions.eq("aktif", true));
		if (perguruanTinggi != null) {
			Common.selectComboItem(true, searchPerguruanTinggi, perguruanTinggi);
			searchPerguruanTinggi.setDisabled(true);
		}

		tbmuser = Common.getCurrentUser();

		tampilSederhana = Common.bolehKonfigurasi("tampil_pengumuman_sederhana", Konfigurasi.TIDAK_AKTIF);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		if (labelFakProd != null) { labelFakProd.setVisible(pt); }
		if (fakProd != null) { fakProd.setVisible(pt); }

		if (labelYaySek != null) { labelYaySek.setVisible(ya); }
		if (yaySek != null) { yaySek.setVisible(ya); }

		try {
			// value_jurusan.setVisible(!tampilSederhana);
			// value_fakultas.setVisible(!tampilSederhana);
			value_tahun_akademik.setVisible(!tampilSederhana);
			label_tahun_akademik.setVisible(!tampilSederhana);
			searchfakultas.setVisible(!tampilSederhana);
			searchjurusan.setVisible(!tampilSederhana);
			searchTahunAjaran.setVisible(!tampilSederhana);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		Common.insertComboDanSemua(searchKategoriPengumuman, "nama", KategoriPengumuman.class);

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		// Common.selectComboItem(searchTahunAjaran,
		// Common.getCurrentTahunAkademik());
		tahunAjaran = Common.generateTahunAjaran(tahunAjaran = new Combobox());
		diperuntukkan(searchDiperuntukkan);

		MyComboitemConfig comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua"); }
		if (comboitem != null) { comboitem.setValue(null); }
		searchDiperuntukkan.appendChild(comboitem);
		if (searchDiperuntukkan != null) { searchDiperuntukkan.setSelectedItem(comboitem); }
		if (searchDiperuntukkan != null) { searchDiperuntukkan.setReadonly(true); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.initPrograms(searchprogram);

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		// "Jalan sendiri saja" (permintaan user): muat Papan Pengumuman di LATAR tanpa overlay
		// pemblokir "Harap tunggu, sedang menyiapkan data...". createDefaultTimerNoBusy identik
		// dengan createDefaultTimer namun TIDAK memanggil showBusy — pengguna langsung bisa
		// berinteraksi sementara daftar dimuat.
		Common.createDefaultTimerNoBusy(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class PengumumanAkademisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						MyTabConfig tab2 = new MyTabConfig("Lampiran");
						tab2.setParent(tabs);

						MyTabConfig tab1 = new MyTabConfig("Diskusi");
						tab1.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
						tabpanel2.setHeight("3000px");
						tabpanel2.setParent(tabpanels);
						detailPengumumanAkademisHelper.displayAttachment(pengumumanAkademis, tabpanel2,
								addWindowAttachment);

						Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
						tabpanel1.setParent(tabpanels);
						tabpanel1.setHeight("3000px");
						detailPengumumanAkademisHelper.displayDetailPengumuman(pengumumanAkademis, tabpanel1);

					}

				}
			});

			RevisiHelper
					.createNewRevisi(PengumumanAkademis.class, pengumumanAkademis, pengumumanAkademis.getTahunAjaran())
					.setParent(arg0);

			new Label(pengumumanAkademis.getDiperuntukkan() + (pengumumanAkademis.getKategoriPengumuman() == null ? ""
					: " / " + pengumumanAkademis.getKategoriPengumuman().getNama())).setParent(arg0);

			new Label(pengumumanAkademis.getTanggal() == null ? ""
					: Common.dateFormat2.get().format(pengumumanAkademis.getTanggal())).setParent(arg0);
			new Label(pengumumanAkademis.getSampai() == null ? ""
					: Common.dateFormat2.get().format(pengumumanAkademis.getSampai())).setParent(arg0);
			new Label(pengumumanAkademis.getJudul()).setParent(arg0);
			new Label(pengumumanAkademis.getOleh()).setParent(arg0);
			new Label(((pengumumanAkademis.getFakultas() == null ? "" : pengumumanAkademis.getFakultas().getNama())
					+ (pengumumanAkademis.getJurusan() == null ? ""
							: " / " + pengumumanAkademis.getJurusan().getNama()))
					+ (pengumumanAkademis.getYayasan() == null ? "" : pengumumanAkademis.getYayasan().getNama())
					+ (pengumumanAkademis.getSekolah() == null ? ""
							: " / " + pengumumanAkademis.getSekolah().getNama()))
					.setParent(arg0);

			new Label(pengumumanAkademis.getProgram() == null || pengumumanAkademis.getProgram().trim().isEmpty()
					? "Semua"
					: pengumumanAkademis.getProgram()).setParent(arg0);

			long diff = 0L;
			if (pengumumanAkademis.getTanggal() != null && pengumumanAkademis.getSampai() != null) {
				diff = pengumumanAkademis.getSampai().getTime() - pengumumanAkademis.getTanggal().getTime();
				diff = (diff / (1000 * 60 * 60 * 24));
			}
			new Label(diff + " hari").setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(pengumumanAkademis.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					pengumumanAkademis.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(pengumumanAkademis);
				}
			});
			new Label(pengumumanAkademis.getBolehDiberiKomentar() ? "Ya" : "Tidak").setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, pengumumanAkademis, PengumumanAkademisAction.this)
					.setParent(arg0);

		}
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pengumumanAkademis = (PengumumanAkademis) obj;
		init(pengumumanAkademis);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new PengumumanAkademis());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * REF SEMENTARA untuk Galeri saat menambah pengumuman BARU (id masih {@code null}).
	 *
	 * <p>Id {@link PengumumanAkademis} di-generate DB ({@code IDENTITY}) sehingga belum tersedia sebelum
	 * disimpan. Supaya bagian <b>Galeri</b> tetap BISA DIPAKAI saat menambah data, item galeri
	 * ({@link LampiranLain}) diunggah memakai <b>ref sementara acak BERNILAI NEGATIF</b> — tidak mungkin
	 * bentrok dengan id asli yang selalu positif. Saat Simpan, seluruh item galeri yang tercatat di
	 * {@code maps} di-{@code setRef(id asli)} oleh blok timer di {@link #onSave(Event)} (memakai sesi
	 * STREAMING, karena {@link LampiranLain} berada di DB streaming) — jadi ref sementara otomatis
	 * berpindah ke pengumuman yang baru tersimpan.</p>
	 *
	 * <p>Untuk <b>Lampiran</b> tidak perlu ref sementara: relasinya FK ke entity dan
	 * {@code LampiranPengumumanAkademis.pengumumanAkademis} ber-{@code cascade=PERSIST}, sehingga
	 * menyimpan lampiran otomatis ikut menyimpan pengumuman induknya (id asli langsung terisi).</p>
	 */
	private Long refSementaraGaleri;

	/** Ref yang dipakai item Galeri saat ini: id asli bila sudah ada, selain itu ref sementara. */
	private Long refGaleri() {
		if (pengumumanAkademis != null && pengumumanAkademis.getId() != null) {
			return pengumumanAkademis.getId();
		}
		if (refSementaraGaleri == null) {
			long acak = Math.abs(java.util.UUID.randomUUID().getMostSignificantBits() % 900000000000L);
			refSementaraGaleri = Long.valueOf(-(acak + 1000000000L));
		}
		return refSementaraGaleri;
	}

	private Map<Long, LampiranLain> maps = new HashMap<Long, LampiranLain>();
	private Rows myGridGaleri;
	private Intbox slideWaktu;
	private Row rowHanyaUntukUsername;
	private Textbox tinggiGaleri;
	// private Div div;
	// private Tabpanel tabpanel2;
	private Textbox hanyaUntukUsername;
	private Row rowHanyaUntukKeteranganUsername;
	private JSONArray isiPollings;
	private Combobox perguruanTinggi;
	private Textbox judulEn;
	private MyCkEditor catatanEn;
	private Combobox induk;
	protected LampiranLain lainMahasiswa;
	private Row rowIcon;
	private Combobox posisiTombol;
	private MyTextbox labelTombol;
	private Intbox nomorUrut;
	private Textbox klassData;

	@SuppressWarnings("unchecked")
	private void init(final PengumumanAkademis pengumumanAkademis) throws Exception {
		this.pengumumanAkademis = pengumumanAkademis;
		Common.clear(addWindow);
		Borderlayout borderlayoutLampiran = new Borderlayout();
		borderlayoutLampiran.setParent(addWindow);
		Center centerLampiran = new Center();
		centerLampiran.setParent(borderlayoutLampiran);
		ais.ui.util.ZkCompat.setFlex(centerLampiran, true);

		MyGrid gridLampiran = new MyGrid();
		gridLampiran.setWidth("100%");
		gridLampiran.setParent(centerLampiran);
		gridLampiran.setWidth("100%");
		gridLampiran.setHeight("100%");
		gridLampiran.setSclass("fgrid");

		tinggiGaleriMobile = new Textbox(pengumumanAkademis.getTinggiGaleriMobile());
		tinggiGaleri = new Textbox(pengumumanAkademis.getTinggiGaleri());
		slideWaktu = new Intbox(pengumumanAkademis.getSlideWaktu());
		galeryBerupaHtml = new MyCheckboxConfig("Galeri berupa HTML");
		galeryBerupaHtml.setChecked(pengumumanAkademis.getGaleryBerupaHtml());

		Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		MyFormRow rowLampiran = new MyFormRow();
		rowLampiran.setParent(rowsLampiran);

		// === GANTI Tabbox → Grid → Rows → Row (permintaan user): 3 seksi DITUMPUK VERTIKAL — Data
		// Pengumuman (atas), lalu Lampiran Pengumuman, lalu Galeri Pengumuman (bawah). Semua dibangun
		// EAGER, jadi tidak ada lagi masalah render lazy/tinggi pada Tabbox. ===
		Grid gridStack = new Grid();
		gridStack.setWidth("100%");
		gridStack.setParent(rowLampiran);
		Columns colsStack = new Columns();
		colsStack.setParent(gridStack);
		new org.zkoss.zul.Column().setParent(colsStack);
		Rows rowsStack = new Rows();
		rowsStack.setParent(gridStack);

		final String gayaHeaderSeksi = "font-weight:700;font-size:14px;color:#1e3a8a;"
				+ "border-bottom:2px solid #e2e8f0;padding:7px 2px;margin:4px 0 8px;";

		// Seksi 1: DATA PENGUMUMAN
		Row rowSeksiData = new Row();
		rowSeksiData.setParent(rowsStack);
		Vbox vbSeksiData = new Vbox();
		vbSeksiData.setWidth("100%");
		vbSeksiData.setParent(rowSeksiData);
		new ais.ui.util.MyHtml("<div style='" + gayaHeaderSeksi + "'>Data Pengumuman</div>").setParent(vbSeksiData);
		final ais.ui.util.MyDiv tabpanel = new ais.ui.util.MyDiv();
		tabpanel.setWidth("100%");
		tabpanel.setParent(vbSeksiData);

		// Seksi 2: LAMPIRAN PENGUMUMAN
		Row rowSeksiLampiran = new Row();
		rowSeksiLampiran.setParent(rowsStack);
		Vbox vbSeksiLampiran = new Vbox();
		vbSeksiLampiran.setWidth("100%");
		vbSeksiLampiran.setParent(rowSeksiLampiran);
		new ais.ui.util.MyHtml("<div style='" + gayaHeaderSeksi + "'>Lampiran Pengumuman</div>").setParent(vbSeksiLampiran);
		final ais.ui.util.MyDiv tabpanelBiodata = new ais.ui.util.MyDiv();
		tabpanelBiodata.setWidth("100%");
		tabpanelBiodata.setParent(vbSeksiLampiran);
		final EventListener biodataEvent = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tabpanelBiodata);
				// LAMPIRAN AKTIF walau pengumuman BARU (id belum ada) — TIDAK perlu simpan/isi Judul dulu.
				// Relasi LampiranPengumumanAkademis.pengumumanAkademis ber-cascade=PERSIST, sehingga saat
				// lampiran diunggah, pengumuman induknya ikut TERSIMPAN otomatis dan id aslinya langsung
				// terisi pada objek yang sama. Klik Simpan berikutnya tinggal MEMPERBARUI record itu.
				try {
					detailPengumumanAkademisHelper.displayAttachment(
							PengumumanAkademisAction.this.pengumumanAkademis, tabpanelBiodata, addWindowAttachment);
				} catch (Exception exDisp) {
					exDisp.printStackTrace();
					ais.common.ErrorAuditUtil.record(exDisp, "auto-audit PengumumanAkademisAction.biodataEvent.display");
					new ais.ui.util.MyLabelConfig("Gagal menampilkan lampiran: " + exDisp.getMessage())
							.setParent(tabpanelBiodata);
				}
			}
		};
		// Seksi 3: GALERI PENGUMUMAN
		Row rowSeksiGaleri = new Row();
		rowSeksiGaleri.setParent(rowsStack);
		Vbox vbSeksiGaleri = new Vbox();
		vbSeksiGaleri.setWidth("100%");
		vbSeksiGaleri.setParent(rowSeksiGaleri);
		new ais.ui.util.MyHtml("<div style='" + gayaHeaderSeksi + "'>Galeri Pengumuman</div>").setParent(vbSeksiGaleri);
		final ais.ui.util.MyDiv tabpanelGalery = new ais.ui.util.MyDiv();
		tabpanelGalery.setWidth("100%");
		tabpanelGalery.setParent(vbSeksiGaleri);
		final EventListener galeryEvent = new EventListener() {

			EventListener getThis() {
				return this;
			}

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (tabpanelGalery.getChildren().isEmpty()) {
					// GALERI AKTIF walau pengumuman BARU (id belum ada) — TIDAK perlu simpan/isi Judul dulu.
					// Item galeri diunggah memakai REF SEMENTARA negatif (lihat refGaleri()); saat Simpan,
					// ref tersebut dipindahkan ke id asli oleh pindahkanRefGaleriSementara().

					maps = new HashMap<Long, LampiranLain>();

					Grid grid = new Grid();
					grid.setSclass("dgrid");
					grid.setWidth("100%");
					grid.setParent(tabpanelGalery);
					grid.setWidth("100%");
					// JANGAN paksa height:100% — saat tab dibangun DINAMIS (dipilih setelah halaman tampil),
					// tinggi tabpanel belum efektif sehingga height:100% resolve ke 0 → grid kolaps & seluruh
					// baris terklip = tab tampak KOSONG. Biarkan tinggi mengikuti konten (natural flow).

					Columns columns = new Columns();
					MyColumnConfig column = new MyColumnConfig();
					column.setWidth("15%");
					columns.appendChild(column);
					column = new MyColumnConfig();
					columns.appendChild(column);
					grid.appendChild(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Galeri Gambar"));

					Hbox myHbox = new Hbox();
					myHbox.setParent(row);
					myHbox.setHeight("30px");

					Hbox hboxGambar = new Hbox();
					hboxGambar.setParent(myHbox);
					tampilkanButton(hboxGambar);

					row = new MyFormRow();
					row.setParent(rows);
					ais.ui.util.ZkCompat.setSpans(row, "2");

					myGridGaleri = (Rows) Common.tampilanScroll1(row).getParent();

					columns = new Columns();
					columns.setParent(myGridGaleri.getGrid());

					if (pengumumanAkademis.getGaleryBerupaHtml()) {
						column = new MyColumnConfig("Isi Galieri");
						column.setWidth("90%");
						column.setParent(columns);

						column = new MyColumnConfig("");
						column.setWidth("0px");
						column.setParent(columns);
					} else {

						column = new MyColumnConfig("Foto");
						column.setWidth("60%");
						column.setParent(columns);

						column = new MyColumnConfig("Keterangan");
						column.setWidth("30%");
						column.setParent(columns);
					}

					column = new MyColumnConfig("Hapus");
					column.setWidth("10%");
					column.setParent(columns);

					row = new MyFormRow();
					row.setVisible(!tampilSederhana);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Waktu Ganti Slide (dalam detik)"));
					row.appendChild(slideWaktu);
					slideWaktu.setWidth("90%");

					row = new MyFormRow();
					row.setVisible(!tampilSederhana);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Style Galeri Desktop"));
					row.appendChild(tinggiGaleri);
					tinggiGaleri.setWidth("90%");

					row = new MyFormRow();
					row.setVisible(!tampilSederhana);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig("Style Galeri Mobile"));
					row.appendChild(tinggiGaleriMobile);
					tinggiGaleriMobile.setWidth("90%");

					row = new MyFormRow();
					row.setVisible(!tampilSederhana);
					row.setParent(rows);
					row.appendChild(new ais.ui.util.MyLabelConfig(""));
					row.appendChild(galeryBerupaHtml);
					galeryBerupaHtml.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pengumumanAkademis.setGaleryBerupaHtml(galeryBerupaHtml.isChecked());
							pengumumanAkademis.setTinggiGaleri(tinggiGaleri.getValue());
							pengumumanAkademis.setTinggiGaleriMobile(tinggiGaleriMobile.getValue());
							getThis().onEvent(arg0);
						}
					});
					// Pakai refGaleri(): id asli bila sudah ada, atau REF SEMENTARA saat pengumuman masih baru
					// — sehingga item yang baru diunggah pada mode Tambah tetap ikut tampil di daftar.
					{
						try {
							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
							List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
									.addOrder(Order.asc("id")).add(Restrictions.eq("ref", refGaleri()))
									.add(Restrictions.ilike("jenis", "Galery_Pengumuman_", MatchMode.START)).list();
							for (LampiranLain lampiran : lampiranLains) {
								maps.put(lampiran.getId(), lampiran);
							}

							StreamingHibernateUtil.getInstance().closeSession();

						} catch (Exception e1) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/PengumumanAkademisAction.java:741");
						}
					}

					reloadDataGambar(pengumumanAkademis);
				}

			}
		};

		// Data form: dulu Borderlayout+Center (butuh tinggi eksplisit dari Tabpanel). Di layout tumpuk (Div)
		// Borderlayout kolaps → form kosong. Pakai Grid langsung (natural flow) agar ter-render tanpa
		// bergantung tinggi parent.
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(tabpanel);

		Columns columns = new Columns();
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("15%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		columns.appendChild(column);
		grid.appendChild(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		Common.selectComboItem(tahunAjaran, pengumumanAkademis.getTahunAjaran());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");
		// tahunAjaran.setConstraint("no empty");
		tahunAjaran.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diperuntukkan"));
		row.appendChild(diperuntukkan = new Combobox());
		diperuntukkan(diperuntukkan);

		diperuntukkan.setWidth("90%");
		// diperuntukkan.setConstraint("no empty");
		diperuntukkan.setReadonly(true);

		Common.selectComboItem(diperuntukkan, pengumumanAkademis.getDiperuntukkan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori Pengumuman"));
		row.appendChild(kategoriPengumuman = new Combobox());
		Common.insertComboDanSemua(kategoriPengumuman, new String[] { "nama" }, "keterangan", KategoriPengumuman.class,
				"== Tanpa Kategori ==", Restrictions.sqlRestriction("true"));
		Common.selectComboItem(kategoriPengumuman, pengumumanAkademis.getKategoriPengumuman());
		kategoriPengumuman.setWidth("90%");
		kategoriPengumuman.setReadonly(true);

		rowHanyaUntuk = new MyFormRow();
		rowHanyaUntuk.setStyle("border:0px;background: transparent;");
		rowHanyaUntuk.setParent(rows);
		rowHanyaUntuk.appendChild(new ais.ui.util.MyLabelConfig("Khusus untuk grup pengguna"));
		rowHanyaUntuk.appendChild(hanyaUntuk = new Textbox(pengumumanAkademis.getHanyaUntuk()));
		hanyaUntuk.setWidth("90%");
		hanyaUntuk.setRows(2);

		rowHanyaUntukAngkatan = new MyFormRow();
		rowHanyaUntukAngkatan.setStyle("border:0px;background: transparent;");
		rowHanyaUntukAngkatan.setParent(rows);
		rowHanyaUntukAngkatan.appendChild(new ais.ui.util.MyLabelConfig("Khusus untuk tahun angkatan"));
		rowHanyaUntukAngkatan.appendChild(hanyaUntukAngkatan = new Textbox(pengumumanAkademis.getHanyaUntukAngkatan()));
		hanyaUntukAngkatan.setWidth("90%");

		rowHanyaUntukKeterangan = Common.initKeterangan(rows,
				"Jika pengumuman ini khusus suatu NIM tertentu, masukkan NIM mahasiswa, dan juga tahun angkatan yang pisah menggunakan tanda titik koma (,). Misal : 1234,1235,1236");
		rowHanyaUntukKeteranganAdmin = Common.initKeterangan(rows,
				"Jika pengumuman ini khusus untuk admin tertentu, masukkan kode grup pengguna yang pisah menggunakan tanda titik koma (,). Misal : am,admfak,admprd,pmb");

		rowHanyaUntukUsername = new MyFormRow();
		rowHanyaUntukUsername.setStyle("border:0px;background: transparent;");
		rowHanyaUntukUsername.setParent(rows);
		rowHanyaUntukUsername
				.appendChild(new ais.ui.util.MyLabelConfig("Khusus untuk User ID (username untuk login pengguna)"));
		rowHanyaUntukUsername.appendChild(hanyaUntukUsername = new Textbox(pengumumanAkademis.getHanyaUntukUsername()));
		hanyaUntukUsername.setWidth("90%");
		hanyaUntukUsername.setRows(2);

		rowHanyaUntukKeteranganUsername = Common.initKeterangan(rows,
				"Jika pengumuman ini khusus untuk admin tertentu, masukkan User ID (username untuk login pengguna) yang pisah menggunakan tanda koma (,). Misal : joni,andi,rika");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					rowHanyaUntuk.setVisible(diperuntukkan.getSelectedItem().getValue()
							.equals(PengumumanAkademis.UNTUK_ALUMNI)
							|| diperuntukkan.getSelectedItem().getValue().equals(PengumumanAkademis.UNTUK_MAHASISWA)
							|| diperuntukkan.getSelectedItem().getValue().equals(PengumumanAkademis.UNTUK_PESERTA));

					rowHanyaUntukAngkatan.setVisible(diperuntukkan.getSelectedItem().getValue()
							.equals(PengumumanAkademis.UNTUK_ALUMNI)
							|| diperuntukkan.getSelectedItem().getValue().equals(PengumumanAkademis.UNTUK_MAHASISWA));

					rowHanyaUntukKeterangan.setVisible(rowHanyaUntuk.isVisible());

					if (diperuntukkan.getSelectedItem().getValue().equals(PengumumanAkademis.UNTUK_ADMIN)) {
						rowHanyaUntuk.setVisible(true);
					}

					rowHanyaUntukUsername.setVisible(
							diperuntukkan.getSelectedItem().getValue().equals(PengumumanAkademis.UNTUK_ADMIN));

					rowHanyaUntukKeteranganAdmin.setVisible(
							diperuntukkan.getSelectedItem().getValue().equals(PengumumanAkademis.UNTUK_ADMIN));
					rowHanyaUntukKeteranganUsername.setVisible(
							diperuntukkan.getSelectedItem().getValue().equals(PengumumanAkademis.UNTUK_ADMIN));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:866");
					// TODO: handle exception
				}
			}
		};

		diperuntukkan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengumuman ini valid mulai "));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(
				tanggal = new MyDatebox(pengumumanAkademis.getTanggal() == null ? ais.ui.util.WaktuUtil.getDate()
						: pengumumanAkademis.getTanggal()));
		// tanggal.setConstraint("no empty");

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(sampai = new MyDatebox(pengumumanAkademis.getSampai() == null ? ais.ui.util.WaktuUtil.getDate()
				: pengumumanAkademis.getSampai()));
		// sampai.setConstraint("no empty");

		hbox.appendChild(tetapTampilkanPengumumanMeskipunSudahKelewat = new MyCheckboxConfig(
				"Tetap tampilkan pengumuman meskipun sudah terlewat"));
		tetapTampilkanPengumumanMeskipunSudahKelewat
				.setChecked(pengumumanAkademis.getTetapTampilkanPengumumanMeskipunSudahKelewat());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif / Komentar"));

		Hbox hboxKomentar = new Hbox();
		row.appendChild(hboxKomentar);

		hboxKomentar.appendChild(aktif = new MyCheckboxConfig("Aktif atau ditampilkan"));
		aktif.setChecked(pengumumanAkademis.getAktif());

		hboxKomentar.appendChild(langsungMunculDiTab = new MyCheckboxConfig("Langsung muncul"));
		langsungMunculDiTab.setChecked(pengumumanAkademis.getLangsungMunculDiTab());
		langsungMunculDiTab.setVisible(!tampilSederhana);

		// Atribut baru (permintaan user): bila dicentang, isi pengumuman ini LANGSUNG tampil penuh
		// di Papan Pengumuman halaman utama (Beranda) tanpa perlu diklik judulnya dulu.
		hboxKomentar.appendChild(langsungTampilBeranda = new MyCheckboxConfig("Langsung tampil di Beranda"));
		langsungTampilBeranda.setChecked(pengumumanAkademis.getLangsungTampilBeranda());
		langsungTampilBeranda.setTooltiptext(
				"Bila dicentang, isi pengumuman langsung tampil penuh di Papan Pengumuman halaman utama tanpa perlu diklik.");

		hboxKomentar.appendChild(bolehDiberiKomentar = new MyCheckboxConfig("Boleh diberi komentar"));
		bolehDiberiKomentar.setChecked(pengumumanAkademis.getBolehDiberiKomentar());
		bolehDiberiKomentar.setVisible(!tampilSederhana);

		hboxKomentar.appendChild(tampilkanPengumumanLain = new MyCheckboxConfig("Tampil pengumuman lain"));
		tampilkanPengumumanLain.setChecked(pengumumanAkademis.getTampilkanPengumumanLain());
		tampilkanPengumumanLain.setVisible(!tampilSederhana);

		hboxKomentar.appendChild(tampilkanProfile = new MyCheckboxConfig("Tampil profile"));
		tampilkanProfile.setChecked(pengumumanAkademis.getTampilkanProfile());
		tampilkanProfile.setVisible(!tampilSederhana);

		hboxKomentar.appendChild(adaVideoConference = new MyCheckboxConfig("Bisa (Jitsi)"));
		adaVideoConference.setChecked(pengumumanAkademis.getAdaVideoConference());
		adaVideoConference.setVisible(!tampilSederhana);

		hboxKomentar.appendChild(adaVideoConferenceGoogleMeet = new MyCheckboxConfig("Bisa (Google Meet)"));
		adaVideoConferenceGoogleMeet.setChecked(pengumumanAkademis.getAdaVideoConferenceGoogleMeet());
		adaVideoConferenceGoogleMeet.setVisible(!tampilSederhana);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Broadcast"));

		Hbox hboxBroadcast = new Hbox();
		row.appendChild(hboxBroadcast);

		hboxBroadcast.appendChild(broadcastKeMahasiswaAktif = new MyCheckboxConfig("ke mahasiswa"));
		broadcastKeMahasiswaAktif.setChecked(pengumumanAkademis.getBroadcastKeMahasiswaAktif());
		broadcastKeMahasiswaAktif.setVisible(pt);

		hboxBroadcast.appendChild(broadcastKeDosen = new MyCheckboxConfig("ke dosen"));
		broadcastKeDosen.setChecked(pengumumanAkademis.getBroadcastKeDosen());
		broadcastKeDosen.setVisible(pt);

		hboxBroadcast.appendChild(broadcastAdmin = new MyCheckboxConfig("ke admin"));
		broadcastAdmin.setChecked(pengumumanAkademis.getBroadcastAdmin());

		hboxBroadcast.appendChild(broadcastCalonMahasiswa = new MyCheckboxConfig("ke calon mahasiswa"));
		broadcastCalonMahasiswa.setChecked(pengumumanAkademis.getBroadcastCalonMahasiswa());
		broadcastCalonMahasiswa.setVisible(pt);

		hboxBroadcast.appendChild(broadcastKeMahasiswaAlumni = new MyCheckboxConfig("ke alumni"));
		broadcastKeMahasiswaAlumni.setChecked(pengumumanAkademis.getBroadcastKeMahasiswaAlumni());
		broadcastKeMahasiswaAlumni.setVisible(pt);

		hboxBroadcast.appendChild(broadcastKeMahasiswaCuti = new MyCheckboxConfig("ke mhs cuti"));
		broadcastKeMahasiswaCuti.setChecked(pengumumanAkademis.getBroadcastKeMahasiswaCuti());
		broadcastKeMahasiswaCuti.setVisible(pt);

		hboxBroadcast.appendChild(broadcastKeSiswaAktif = new MyCheckboxConfig("ke siswa"));
		broadcastKeSiswaAktif.setChecked(pengumumanAkademis.getBroadcastKeSiswaAktif());
		broadcastKeSiswaAktif.setVisible(ya);

		hboxBroadcast.appendChild(broadcastKeGuru = new MyCheckboxConfig("ke guru"));
		broadcastKeGuru.setChecked(pengumumanAkademis.getBroadcastKeGuru());
		broadcastKeGuru.setVisible(ya);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(judul = new Textbox(pengumumanAkademis.getJudul()));
		judul.setWidth("90%");

		// Kolom Judul/Isi bahasa Inggris DIHILANGKAN dari form — kini konten pengumuman diterjemahkan
		// otomatis mengikuti bahasa aktif (English/Arab/Mandarin) via TRANSLATER INTERNAL saat ditampilkan,
		// tanpa perlu diisi manual dan tanpa disimpan ke DB. Field judulEn/catatanEn dipertahankan di kode
		// (nilai lama tidak diubah) demi kompatibilitas, tetapi tidak lagi dapat disunting di sini.

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi"));

		catatan = new MyCkEditor();
		catatan.setValue(pengumumanAkademis.getCatatan());
		catatan.setWidth("98%");
		catatan.setHeight("200px");
		catatan.setParent(row);

		MyToolbarbutton fileupload = new MyToolbarbutton("fa-cog", "Generate Pengumuman");
		catatan.hbox.appendChild(fileupload);

		EventListener eventListenerData = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		};

		String tanyaMengajar = " apa saja";

		fileupload.addEventListener("onClick",
				AIGenerator.generateApa("Generate Pengumuman", "Pengumuman tentang apa ?", "Buatkan pengumuman tentang",
						false, "",
						Common.getKonfigurasi("llama_system_pengumuman",
								"Kamu adalah operator Sistem Informasi Akademik Perguruan Tinggi ").getNilai().trim(),
						catatan, eventListenerData, tanyaMengajar, eventListenerData));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Polling / Jejak Pendapat"));

		isiPollings = new JSONArray(pengumumanAkademis.getIsiPolling());
		row.appendChild(PengumumanAkademisAction.initIsiPolling(pengumumanAkademis, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PengumumanAkademis pengumumanAkademis = (PengumumanAkademis) arg0.getData();
				isiPollings = new JSONArray(pengumumanAkademis.getIsiPolling());
			}
		}));

		if (tbmuser != null && tbmuser.ambilJurusan() != null) {
			pengumumanAkademis.setJurusan(tbmuser.ambilJurusan());
		}
		if (tbmuser != null && tbmuser.ambilFakultas() != null) {
			pengumumanAkademis.setFakultas(tbmuser.ambilFakultas());
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
				pengumumanAkademis.getYayasan() == null ? tbmuser.ambilYayasan() : pengumumanAkademis.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		Common.pilihSekolah(sekolah,
				pengumumanAkademis.getSekolah() == null ? tbmuser.ambilSekolah() : pengumumanAkademis.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perguruan Tinggi"));

		PerguruanTinggi selected = PerguruanTinggiUtil.getPerguruanTinggi();

		perguruanTinggi = new Combobox();
		Common.insertComboDanSemua(perguruanTinggi, "nama", PerguruanTinggi.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(perguruanTinggi, pengumumanAkademis.getPerguruanTinggi() == null
				? (tbmuser.ambilFakultas() == null || tbmuser.ambilFakultas().getPerguruanTinggi() == null ? selected
						: tbmuser.ambilFakultas().getPerguruanTinggi())
				: pengumumanAkademis.getPerguruanTinggi());
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
				pengumumanAkademis.getFakultas() == null ? tbmuser.ambilFakultas() : pengumumanAkademis.getFakultas());
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
				pengumumanAkademis.getJurusan() == null ? tbmuser.ambilJurusan() : pengumumanAkademis.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = Common.initPrograms(program);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, pengumumanAkademis.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengumuman ini menginduk ke"));
		row.appendChild(induk = new Combobox());
		induk.setWidth("90%");
		Common.insertComboDanSemua(induk, new String[] { "judul" }, "judulEn", PengumumanAkademis.class,
				"=Tidak menginduk pengumuman lain=",
				initCriteria(true).add(pengumumanAkademis.getId() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ne("id", pengumumanAkademis.getId())));
		Common.selectComboItem(induk, pengumumanAkademis.getInduk());

		lainMahasiswa = null;
		rowIcon = new MyFormRow();
		rowIcon.setParent(rows);
		rowIcon.appendChild(new ais.ui.util.MyLabelConfig("Gambar/Icon Pengumuman"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pengumumanAkademis.getId(), LampiranLain.ICON_PENGUMUMAN,
				"Gambar/Icon Pengumuman", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(rowIcon);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Posisi tombol ada di"));
		row.appendChild(posisiTombol = new Combobox());

		Comboitem comboitem = new MyComboitemConfig(PengumumanAkademis.ATAS);
		comboitem.setValue(PengumumanAkademis.ATAS);
		posisiTombol.appendChild(comboitem);

		comboitem = new MyComboitemConfig(PengumumanAkademis.BAWAH);
		comboitem.setValue(PengumumanAkademis.BAWAH);
		posisiTombol.appendChild(comboitem);

//		comboitem = new MyComboitemConfig(PengumumanAkademis.KANAN);
//		comboitem.setValue(PengumumanAkademis.KANAN);
//		posisiTombol.appendChild(comboitem);
//
//		comboitem = new MyComboitemConfig(PengumumanAkademis.KIRI);
//		comboitem.setValue(PengumumanAkademis.KIRI);
//		posisiTombol.appendChild(comboitem);

		Common.selectComboItem(posisiTombol, pengumumanAkademis.getPosisiTombol());
		posisiTombol.setReadonly(true);
		posisiTombol.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Label Tombol"));
		row.appendChild(labelTombol = new MyTextbox(pengumumanAkademis.getLabelTombol()));
		labelTombol.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut Tombol"));
		row.appendChild(nomorUrut = new Intbox(pengumumanAkademis.getNomorUrut()));

		EventListener eventListenerIcon = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PengumumanAkademis indk = (PengumumanAkademis) (induk.getSelectedItem() == null ? null
						: induk.getSelectedItem().getValue());
				rowIcon.setVisible(indk != null);

				posisiTombol.getParent().setVisible(indk != null);
				labelTombol.getParent().setVisible(indk != null);
				nomorUrut.getParent().setVisible(indk != null);
			}
		};

		induk.addEventListener("onChange", eventListenerIcon);
		eventListenerIcon.onEvent(null);

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koresponden"));
		row.appendChild(korespondensi = new Textbox(pengumumanAkademis.getKorespondensi()));
		korespondensi.setWidth("90%");
		korespondensi.setRows(3);

		if (korespondensi.getValue().trim().isEmpty()) {
			korespondensi.setValue(Common.getCurrentUser().getUserId());
		}

		if (!tampilSederhana) {
			Common.initKeterangan(rows,
					"Untuk memasukkan banyak Koresponden, masukkan username masing-masing pengguna dengan pemisah tanda koma (,)");

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Koresponden",
					"/img/user_male_add.png");

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tambah Koresponden"));
			row.appendChild(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
					ambil.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							// TODO Auto-generated method stub
							List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
							if (tbmusers != null && tbmusers.size() != 0) {
								for (Tbmuser tbmuser : tbmusers) {
									korespondensi.setValue(korespondensi.getValue()
											+ (korespondensi.getValue().isEmpty() ? tbmuser.getUserId()
													: "," + tbmuser.getUserId()));
								}
							}
						}
					});
					ambil.setWidth("850px");
					ambil.setHeight("97%");
					ambil.setVisible(true);
					ambil.onModal();
				}
			});
		}

		row = new MyFormRow();
		row.setVisible(!tampilSederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Class Data"));
		row.appendChild(klassData = new Textbox(pengumumanAkademis.getKlassData()));
		klassData.setWidth("90%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayoutLampiran);

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

		// Layout tumpuk: konten Lampiran & Galeri dibangun EAGER (langsung), bukan lagi lewat klik tab.
		// biodataEvent/galeryEvent sudah dijaga: bila Judul kosong & belum tersimpan, cukup tampil petunjuk
		// (tanpa popup). Bila Judul terisi / sudah tersimpan, langsung menyimpan + menampilkan isinya.
		// Muat ULANG kedua seksi saat Judul diisi (onChange) agar isinya muncul begitu data valid.
		biodataEvent.onEvent(null);
		galeryEvent.onEvent(null);
		if (judul != null) {
			judul.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					try {
						Common.clear(tabpanelBiodata);
						biodataEvent.onEvent(ev);
						Common.clear(tabpanelGalery);
						galeryEvent.onEvent(ev);
					} catch (Exception e) {
						ais.common.ErrorAuditUtil.record(e, "auto-audit PengumumanAkademisAction.judul.onChange.muatSeksi");
					}
				}
			});
		}
	}

	private void tampilkanButton(final Hbox hboxGambar) {
		Common.clear(hboxGambar);
		// refGaleri(): id asli bila pengumuman sudah tersimpan, atau REF SEMENTARA (negatif) bila masih
		// baru — sehingga tombol unggah galeri TETAP AKTIF saat menambah data.
		LampiranLain.createDownloadUploadFileLain(hboxGambar, refGaleri(),
				"Galery_Pengumuman_" + Common.getGeneratedBarCode(), "Galeri Gambar", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();
						maps.put(lainMahasiswaCover.getId(), lainMahasiswaCover);
						reloadDataGambar(pengumumanAkademis);

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tampilkanButton(hboxGambar);
							}
						});
					}
				});
	}

	private void reloadDataGambar(final PengumumanAkademis pengumumanAkademis) throws Exception {
		Common.clear(myGridGaleri);

		for (final LampiranLain lampiranLain : maps.values()) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(myGridGaleri);

			if (pengumumanAkademis.getGaleryBerupaHtml()) {
				MyCkEditor ckEditor = new MyCkEditor();
				ckEditor.setValue(lampiranLain.getDeskripsi());
				ckEditor.setWidth("95%");
				ckEditor.setParent(row);
				ckEditor.setHeight("100px");

				new Label().setParent(row);
			} else {
				String link = FileFotoLain.ambilLinkLampiranLain(lampiranLain, false, false, LampiranLain.class);

				Image image = new Image(link);
				image.setWidth("95%");
				image.setParent(row);

				final Textbox textbox = new Textbox(lampiranLain.getDeskripsi());
				textbox.setWidth("90%");
				textbox.setRows(7);
				textbox.setParent(row);

				textbox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();

							session.refresh(lampiranLain);
							lampiranLain.setDeskripsi(textbox.getValue());

							session.getTransaction().begin();
							session.update(lampiranLain);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
			}

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
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

											LampiranLain d = maps.remove(lampiranLain.getId());
											System.out.println("d = > " + d);

											try {
												Session session = StreamingHibernateUtil.getInstance().currentSession();

												session.getTransaction().begin();
												session.delete(lampiranLain);
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
											} catch (Exception e) {
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												Common.tampilErrorJikaAdmin(e);
											}

											reloadDataGambar(pengumumanAkademis);
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
			button.setParent(row);
		}
	}

	@SuppressWarnings("unchecked")
	public static void tampilkanPolling(final PengumumanAkademis pengumumanAkademis, final Component vbox) {
		try {
			Common.clear(vbox);
			Tbmuser tbmuser = Common.getCurrentUser();
			JSONArray isiPollings = new JSONArray(pengumumanAkademis.getIsiPolling());
			if (isiPollings.length() > 0 && tbmuser != null && tbmuser.getUserId() != null) {
				JSONObject jawabanPolling = new JSONObject(pengumumanAkademis.getJawabanPolling());

				Iterator<String> jumlahPemilih = jawabanPolling.keys();
				Map<Long, Integer> jumlahs = new HashMap<Long, Integer>();
				int total = 0;
				while (jumlahPemilih.hasNext()) {
					total++;
					try {
						String key = jumlahPemilih.next();
						Long refJawabanKey = ais.common.CommonJSONUtil.ambilLong(jawabanPolling, key);
						if (jumlahs.containsKey(refJawabanKey)) {
							jumlahs.put(refJawabanKey, jumlahs.get(refJawabanKey) + 1);
						} else {
							jumlahs.put(refJawabanKey, 1);
						}

					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}

				Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
				groupbox.setParent(vbox);
				groupbox.appendChild(new MyCaptionStyled(
						"POLLING / JEJAK PENDAPAT, TOTAL PEMILIH : " + Common.numberFormat.get().format(total)));

				Radiogroup radiogroup = new Radiogroup();
				radiogroup.setParent(groupbox);

				for (int i = 0; i < isiPollings.length(); i++) {

					final JSONObject jsonObject = isiPollings.getJSONObject(i);
					boolean terjawab = !jawabanPolling.isNull(tbmuser.getUserId());
					final Component vboxPolling;
					if (terjawab) {
						vboxPolling = new ais.ui.util.MyGroupboxStyled();
						Long ref = ais.common.CommonJSONUtil.ambilLong(jsonObject, "ref");
						int jumlahpemilih = jumlahs.containsKey(ref) ? jumlahs.get(ref) : 0;
						double persen = (jumlahpemilih * 100.0) / total;
						vboxPolling.appendChild(new MyCaptionStyled(jsonObject.getString("judul") + ", total pemilih : "
								+ Common.numberFormat.get().format(jumlahpemilih) + " (" + Common.numberFormat.get().format(persen)
								+ "%)"));

						Long refJawaban = ais.common.CommonJSONUtil.ambilLong(jawabanPolling, tbmuser.getUserId());

						if (refJawaban.equals(ref)) {
							((Groupbox) vboxPolling).setStyle("background:#e6fffe;");
							vboxPolling.appendChild(new MyLabelBold("Pilihan Anda"));
						}

						Progressmeter progressmeter = new Progressmeter((int) persen);
						vboxPolling.appendChild(progressmeter);

					} else {
						vboxPolling = new Vbox();
						final Radio radio;
						vboxPolling.appendChild(radio = new Radio(jsonObject.getString("judul")));
						radio.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Tbmuser tbmuser = Common.getCurrentUser();
								JSONObject jawabanPolling = new JSONObject(pengumumanAkademis.getJawabanPolling());
								jawabanPolling.put(tbmuser.getUserId(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject, "ref"));
								pengumumanAkademis.setJawabanPolling(jawabanPolling.toString());
								Common.refreshUpdate(pengumumanAkademis);
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										tampilkanPolling(pengumumanAkademis, vbox);
									}
								});
							}
						});
					}

					vboxPolling.appendChild(new ais.ui.util.MyHtml(jsonObject.getString("isi")));
					radiogroup.appendChild(vboxPolling);

				}
			} else {
				vbox.setVisible(false);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static Map<String, Map<Long, Pertemuan>> pertemuansHarian = new HashMap<String, Map<Long, Pertemuan>>();

	/** Escape ringan untuk teks header slide kehadiran. */
	private static String khEsc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/**
	 * Bungkus daftar "slide" kartu kehadiran (Dosen/Guru) menjadi tampilan modern:
	 * header gradient + ringkasan (Total Jadwal / Sudah Absen / Belum), animasi fade,
	 * tombol Sebelumnya/Berikutnya + Jeda, progress-bar auto-advance, pause saat hover.
	 * HTML/CSS/JS murni (tanpa pustaka), aman dipakai di komponen ZK Html.
	 */
	private static String bungkusSlideKehadiranKeren(long randId, String judul, String tanggal,
			java.util.List<String> slides, int total, int hadir, boolean mobile) {
		if (slides == null || slides.isEmpty()) {
			return "";
		}
		int interval = 10;
		try {
			interval = Integer.parseInt(Common.getKonfigurasi("interval_slide_kehadiran_detik", "10").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:1537");
		}
		if (interval < 2) {
			interval = 2;
		}
		int belum = total - hadir;
		if (belum < 0) {
			belum = 0;
		}
		String r = String.valueOf(randId);
		int n = slides.size();
		StringBuilder sb = new StringBuilder();

		sb.append("<style>");
		sb.append("@keyframes khFade{from{opacity:0;transform:translateY(14px) scale(.985)}to{opacity:1;transform:none}}");
		sb.append("@keyframes khPop{0%{transform:scale(.6);opacity:0}60%{transform:scale(1.08)}100%{transform:scale(1);opacity:1}}");
		sb.append(".kh-wrap{max-width:1180px;margin:0 auto;padding:6px 10px 18px;font-family:Segoe UI,Roboto,Arial,sans-serif;}");
		sb.append(".kh-head{position:relative;border-radius:18px;padding:16px 18px;color:#fff;overflow:hidden;"
				+ "background:linear-gradient(135deg,rgba(0,0,0,.28),rgba(0,0,0,0) 55%),linear-gradient(135deg,var(--ais-theme-primary,#1d4ed8),var(--ais-theme-accent,#06b6d4));"
				+ "box-shadow:0 14px 30px rgba(15,23,42,.22);}");
		sb.append(".kh-title{font-size:20px;font-weight:900;letter-spacing:.2px;}");
		sb.append(".kh-date{font-size:12.5px;opacity:.95;margin-top:2px;}");
		sb.append(".kh-stats{display:flex;flex-wrap:wrap;gap:10px;margin-top:12px;}");
		sb.append(".kh-stat{flex:1 1 110px;min-width:104px;background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.28);border-radius:14px;padding:10px 12px;animation:khPop .5s both;}");
		sb.append(".kh-stat .n{font-size:26px;font-weight:900;line-height:1;}");
		sb.append(".kh-stat .l{font-size:11px;opacity:.95;margin-top:4px;text-transform:uppercase;letter-spacing:.04em;}");
		sb.append(".kh-stat.hadir{background:rgba(22,163,74,.30);border-color:rgba(187,247,208,.6);}");
		sb.append(".kh-stat.belum{background:rgba(220,38,38,.26);border-color:rgba(254,202,202,.6);}");
		sb.append(".kh-bar{height:6px;border-radius:999px;background:rgba(255,255,255,.25);margin-top:12px;overflow:hidden;}");
		sb.append(".kh-bar > i{display:block;height:100%;width:0;background:#fff;border-radius:999px;}");
		sb.append(".kh-stage{position:relative;margin-top:14px;min-height:50px;}");
		sb.append(".kh-slide{display:none;animation:khFade .55s ease both;}");
		sb.append(".kh-slide.active{display:block;}");
		sb.append(".kh-slide table{width:100%;border-collapse:separate;border-spacing:14px;}");
		sb.append(".kh-slide td{vertical-align:top;text-align:center;padding:14px 12px;border-radius:16px;background:#fff;"
				+ "border:1px solid #e6ebf2;box-shadow:0 6px 16px rgba(15,23,42,.08);transition:transform .25s,box-shadow .25s;}");
		sb.append(".kh-slide td:hover{transform:translateY(-4px);box-shadow:0 14px 26px rgba(15,23,42,.16);}");
		sb.append(".kh-slide .gambar_profile{border-radius:50%;height:96px !important;width:96px !important;object-fit:cover;"
				+ "border:3px solid var(--ais-theme-primary,#2563eb);box-shadow:0 4px 10px rgba(15,23,42,.18);margin-bottom:6px;}");
		sb.append(".kh-slide td strong{color:#0f172a;font-size:14px;}");
		sb.append(".kh-slide td table td{background:transparent;border:none;box-shadow:none;padding:1px 0;font-size:12px;color:#475569;}");
		sb.append(".kh-slide td table td:hover{transform:none;box-shadow:none;}");
		sb.append(".kh-ctrl{display:flex;align-items:center;justify-content:center;gap:10px;margin-top:14px;flex-wrap:wrap;}");
		sb.append(".kh-btn{cursor:pointer;border:none;border-radius:10px;padding:8px 14px;font-size:13px;font-weight:700;color:#fff;"
				+ "background:var(--ais-theme-primary,#2563eb);box-shadow:0 4px 10px rgba(37,99,235,.3);transition:transform .15s,filter .15s;}");
		sb.append(".kh-btn:hover{filter:brightness(1.08);transform:translateY(-1px);}");
		sb.append(".kh-btn:active{transform:translateY(1px);}");
		sb.append(".kh-counter{font-size:13px;font-weight:800;color:#334155;min-width:64px;text-align:center;}");
		sb.append(".kh-dots{display:flex;justify-content:center;gap:7px;margin-top:10px;flex-wrap:wrap;}");
		sb.append(".kh-dot{width:9px;height:9px;border-radius:50%;background:#cbd5e1;cursor:pointer;transition:all .25s;}");
		sb.append(".kh-dot.active{background:var(--ais-theme-primary,#2563eb);transform:scale(1.35);}");
		sb.append(".kh-slide td:hover{outline:2px solid var(--ais-theme-primary,#2563eb);}");
		sb.append(".kh-modal{display:none;position:fixed;top:0;left:0;right:0;bottom:0;z-index:2147483647;"
				+ "background:rgba(15,23,42,.55);align-items:center;justify-content:center;padding:4px;}");
		sb.append(".kh-modal.open{display:flex;}");
		sb.append(".kh-modal-box{background:#fff;border-radius:8px;width:99vw;max-width:99vw;height:99vh;"
				+ "max-height:99vh;display:flex;flex-direction:column;overflow:hidden;"
				+ "box-shadow:0 24px 60px rgba(0,0,0,.45);animation:khPop .35s both;}");
		sb.append(".kh-modal-bar{display:flex;align-items:center;justify-content:space-between;gap:10px;"
				+ "padding:11px 15px;color:#fff;font-weight:800;font-size:15px;"
				+ "background:linear-gradient(135deg,var(--ais-theme-primary,#1d4ed8),var(--ais-theme-accent,#06b6d4));}");
		sb.append(".kh-modal-x{cursor:pointer;border:none;border-radius:9px;padding:7px 13px;font-size:13px;"
				+ "font-weight:800;background:rgba(255,255,255,.22);color:#fff;}");
		sb.append(".kh-modal-x:hover{background:rgba(255,255,255,.4);}");
		sb.append(".kh-modal-frame{display:block;flex:1 1 auto;min-height:0;width:100%;height:100%;border:none;"
				+ "background:#fff;overflow:auto;-webkit-overflow-scrolling:touch;touch-action:pan-y;}");
		sb.append("</style>");

		sb.append("<div class=\"kh-wrap\" id=\"khwrap").append(r).append("\">");
		sb.append("<div class=\"kh-head\">");
		sb.append("<div class=\"kh-title\">").append(khEsc(judul)).append("</div>");
		sb.append("<div class=\"kh-date\">").append(khEsc(tanggal)).append("</div>");
		sb.append("<div class=\"kh-stats\">");
		sb.append("<div class=\"kh-stat\"><div class=\"n\">").append(total).append("</div><div class=\"l\">Total Jadwal</div></div>");
		sb.append("<div class=\"kh-stat hadir\"><div class=\"n\">").append(hadir).append("</div><div class=\"l\">Sudah Absen</div></div>");
		sb.append("<div class=\"kh-stat belum\"><div class=\"n\">").append(belum).append("</div><div class=\"l\">Belum Absen</div></div>");
		sb.append("</div>");
		if (n > 1) {
			sb.append("<div class=\"kh-bar\"><i id=\"khbar").append(r).append("\"></i></div>");
		}
		sb.append("</div>");

		sb.append("<div class=\"kh-stage\" id=\"khstage").append(r).append("\">");
		for (int s = 0; s < n; s++) {
			sb.append("<div class=\"kh-slide").append(s == 0 ? " active" : "").append("\">").append(slides.get(s))
					.append("</div>");
		}
		sb.append("</div>");

		sb.append("<div class=\"kh-ctrl\">");
		sb.append("<button type=\"button\" class=\"kh-btn\" onclick=\"khGo").append(r)
				.append("(-1)\">&#8249; Sebelumnya</button>");
		sb.append("<span class=\"kh-counter\" id=\"khctr").append(r).append("\">1 / ").append(n).append("</span>");
		sb.append("<button type=\"button\" class=\"kh-btn\" onclick=\"khGo").append(r)
				.append("(1)\">Berikutnya &#8250;</button>");
		sb.append("<button type=\"button\" class=\"kh-btn\" id=\"khplay").append(r).append("\" onclick=\"khToggle")
				.append(r).append("()\">&#10073;&#10073; Jeda</button>");
		sb.append("</div>");

		sb.append("<div class=\"kh-dots\" id=\"khdots").append(r).append("\">");
		for (int s = 0; s < n; s++) {
			sb.append("<span class=\"kh-dot").append(s == 0 ? " active" : "").append("\" onclick=\"khSet").append(r)
					.append("(").append(s).append(")\"></span>");
		}
		sb.append("</div>");
		sb.append("</div>");

		// Overlay modal + iframe untuk menampilkan 1 panel detail pertemuan saat kartu di-klik.
		String base = "";
		try {
			base = ais.common.Common.getRequestHostWithProtocol();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:1647");
		}
		if (base == null) {
			base = "";
		}

		// ================================================================
		// PENTING: <script> di dalam Html ZK tidak dieksekusi browser
		// (konten Html di-set via innerHTML). Gunakan Clients.evalJavaScript
		// dengan setTimeout agar DOM sudah ada saat JS berjalan.
		// Modal dibuat di document.body (bukan di dalam Html ZK) agar
		// tidak terhalang stacking context header/navigasi ZK.
		// ================================================================
		int intervalMs = interval * 1000;
		StringBuilder jsKh = new StringBuilder();
		jsKh.append("setTimeout(function(){(function(){");
		jsKh.append("var stage=document.getElementById('khstage").append(r).append("');if(!stage)return;");
		jsKh.append("var idx=0,playing=true,t=null,p=null,start=0;");
		jsKh.append("var slides=stage.getElementsByClassName('kh-slide');");
		jsKh.append("var dots=document.getElementById('khdots").append(r).append("').getElementsByClassName('kh-dot');");
		jsKh.append("var ctr=document.getElementById('khctr").append(r).append("');");
		jsKh.append("var bar=document.getElementById('khbar").append(r).append("');");
		jsKh.append("var btn=document.getElementById('khplay").append(r).append("');");
		jsKh.append("var n=slides.length;var DUR=").append(intervalMs).append(";");
		jsKh.append("function render(){for(var k=0;k<n;k++){slides[k].className='kh-slide'+(k===idx?' active':'');if(dots[k])dots[k].className='kh-dot'+(k===idx?' active':'');}if(ctr)ctr.innerHTML=(idx+1)+' / '+n;}");
		jsKh.append("function tick(){if(!bar)return;var pct=Math.min(100,(Date.now()-start)*100/DUR);bar.style.width=pct+'%';}");
		jsKh.append("function schedule(){clearTimeout(t);clearInterval(p);start=Date.now();if(bar)bar.style.width='0%';p=setInterval(tick,80);t=setTimeout(function(){go(1);},DUR);}");
		jsKh.append("function go(d){idx=(idx+d+n)%n;render();if(playing){schedule();}else if(bar){bar.style.width='0%';}}");
		jsKh.append("window.khGo").append(r).append("=function(d){go(d);};");
		jsKh.append("window.khSet").append(r).append("=function(k){idx=((k%n)+n)%n;render();if(playing){schedule();}};");
		jsKh.append("window.khToggle").append(r).append("=function(){playing=!playing;if(btn){btn.innerHTML=playing?'\\u23F8\\u23F8 Jeda':'\\u25B6 Putar';}if(playing){schedule();}else{clearTimeout(t);clearInterval(p);if(bar)bar.style.width='0%';}};");
		jsKh.append("var wrap=document.getElementById('khwrap").append(r).append("');");
		jsKh.append("if(wrap){wrap.onmouseenter=function(){clearTimeout(t);clearInterval(p);};wrap.onmouseleave=function(){if(playing){schedule();}};}");
		// Buat modal overlay di document.body agar terlepas dari stacking context ZK
		jsKh.append("if(!document.getElementById('khmodal").append(r).append("')){");
		jsKh.append("var khm=document.createElement('div');");
		jsKh.append("khm.id='khmodal").append(r).append("';khm.className='kh-modal';");
		jsKh.append("khm.innerHTML='<div class=\"kh-modal-box\"><div class=\"kh-modal-bar\"><span>\\uD83D\\uDCC5 Detail Pertemuan</span>"
				+ "<button type=\"button\" class=\"kh-modal-x\" id=\"khcls").append(r).append("\">\\u2715 Tutup</button></div>"
				+ "<iframe id=\"khframe").append(r).append("\" class=\"kh-modal-frame\" frameborder=\"0\" scrolling=\"yes\"></iframe></div>';");
		jsKh.append("khm.onclick=function(ev){var e=ev||window.event;if((e.target||e.srcElement)===khm){window.khClose").append(r).append("&&window.khClose").append(r).append("();}};");
		jsKh.append("document.body.appendChild(khm);");
		jsKh.append("var xb=document.getElementById('khcls").append(r).append("');");
		jsKh.append("if(xb)xb.onclick=function(){window.khClose").append(r).append("&&window.khClose").append(r).append("();};");
		jsKh.append("}");
		// Fungsi show/close modal
		jsKh.append("var kbseUrl='").append(base).append("/pages/master/tampilan_satu_pertemuan.zul?id=';");
		jsKh.append("window.khShow").append(r).append("=function(pid){var m=document.getElementById('khmodal").append(r)
				.append("');var f=document.getElementById('khframe").append(r)
				.append("');if(!m||!f)return;f.src=kbseUrl+encodeURIComponent(pid);m.className='kh-modal open';};");
		jsKh.append("window.khClose").append(r).append("=function(){var m=document.getElementById('khmodal").append(r)
				.append("');var f=document.getElementById('khframe").append(r)
				.append("');if(m)m.className='kh-modal';if(f)f.src='about:blank';};");
		jsKh.append("render();if(n>1){schedule();}");
		jsKh.append("})();},0);");

		try {
			Clients.evalJavaScript(jsKh.toString());
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:1705");
			// Tidak ada ZK Desktop aktif (mis. dipanggil dari thread latar) — JS tidak dieksekusi
		}

		return sb.toString();
	}

	/**
	 * Penanda per-thread: kehadiran dosen/guru SUDAH ditampilkan di panel Home (kolom
	 * kiri). Saat aktif, panel profil — yang pada tampilan MOBILE juga merender kehadiran
	 * — TIDAK menampilkannya lagi, sehingga tidak tampil DUA KALI. Di luar Home (profil
	 * berdiri sendiri) penanda kosong → kehadiran tetap tampil normal.
	 */
	private static final ThreadLocal<Boolean> KEHADIRAN_HOME_DITAMPILKAN = new ThreadLocal<Boolean>();

	public static void tandaiKehadiranHomeDitampilkan(boolean nilai) {
		if (nilai) {
			KEHADIRAN_HOME_DITAMPILKAN.set(Boolean.TRUE);
		} else {
			KEHADIRAN_HOME_DITAMPILKAN.remove();
		}
	}

	public static boolean isKehadiranHomeDitampilkan() {
		return Boolean.TRUE.equals(KEHADIRAN_HOME_DITAMPILKAN.get());
	}

	public static String tampilkanKehadiranDosen(Tbmuser tbmuser, boolean mobile) {
		return tampilkanKehadiranDosen(tbmuser, tbmuser == null ? null : tbmuser.ambilJurusan(),
				tbmuser == null ? null : tbmuser.ambilFakultas(), mobile, 1);
	}

	@SuppressWarnings("unchecked")
	public static String tampilkanKehadiranDosen(Tbmuser tbmuser, Jurusan jurusan, Fakultas fakultas, boolean mobile,
			int baris) {
		String pengumuman = "";
		if (tbmuser == null || (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getElearning())) {
			String sekarang = Common.dateFormat8.get().format(WaktuUtil.getDate());
			try {

				int jml = 5;

				try {
					jml = Integer.parseInt(Common.getKonfigurasi("jml_tampil_kehadiran_dalam_satu_baris_dekstop", "5")
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:1750");
					// TODO: handle exception
				}

				if (mobile) {
					jml = 2;

					try {
						jml = Integer.parseInt(Common
								.getKonfigurasi("jml_tampil_kehadiran_dalam_satu_baris_mobile", "2").getNilai().trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:1760");
						// TODO: handle exception
					}
				}

				String cssKehadiran = Common.getKonfigurasi("css_tampilan_utama_hadir",
						"vertical-align: top;width:250px;\r\n" + "    text-align: center;\r\n"
								+ "    padding-top: 10px;\r\n" + "    padding-bottom: 10px;\r\n"
								+ "    border-radius: 5px;\r\n"
								+ "    box-shadow: rgba(0, 0, 0, 0.16) 0px 3px 6px, rgba(0, 0, 0, 0.23) 0px 3px 6px;\r\n"
								+ "")
						.getNilai();
				String cellSpacing = Common
						.getKonfigurasi("css_cell_spacing_tampilan_kehadiran", "cellpadding=\"10\" cellspacing=\"15\"")
						.getNilai();

				int i = 1;

				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();

				Long kodeProdi = tbmuser == null || tbmuser.ambilJurusan() == null ? null
						: tbmuser.ambilJurusan().getId();
				Long kodeFakultas = tbmuser == null || tbmuser.ambilFakultas() == null ? null
						: tbmuser.ambilFakultas().getId();

				if (fakultas != null) {
					kodeFakultas = fakultas.getId();
				}

				if (jurusan != null) {
					kodeProdi = jurusan.getId();
				}
				Map<Long, Pertemuan> pertemuans = pertemuansHarian.get(sekarang);
				if (pertemuans == null) {
					Session session = HibernateUtil.currentNativeSession();
					List<Pertemuan> pertemuansData = session.createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(Order.asc("waktuMulai")).add(Restrictions.eq("tanggal", WaktuUtil.getDate()))
							.add(Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"),
									Restrictions.isNotNull("perkuliahan")))
							.list();
					pertemuans = new HashMap<Long, Pertemuan>();
					for (Pertemuan pertemuan : pertemuansData) {
						pertemuans.put(pertemuan.getId(), pertemuan);
					}
					pertemuansData = null;
					pertemuansHarian.put(sekarang, pertemuans);

					// session.disconnect();
					ais.common.Common.closeOpenedSession(session);
				}

				List<Pertemuan> hariIni = new ArrayList<Pertemuan>();
				List<Long> perkuliahans = null;
				if (mahasiswa != null) {
					perkuliahans = mahasiswa.ambilPerkuliahanDanParalel();
					kodeProdi = null;
					kodeFakultas = null;
				} else if (dosen != null) {
					perkuliahans = dosen.ambilPerkuliahan(HibernateUtil.currentSession());
					kodeProdi = null;
					kodeFakultas = null;
				}

				for (Pertemuan pertemuan : pertemuans.values()) {
					if (pertemuan.getPerkuliahan() != null && pertemuan.getTanggal() != null
							&& pertemuan.getPerkuliahan().getJumlahDosen() > 0
							&& sekarang.equals(Common.dateFormat8.get().format(pertemuan.getTanggal()))) {
						if (perkuliahans == null || (perkuliahans != null && pertemuan.getPerkuliahan() != null
								&& perkuliahans.contains(pertemuan.getPerkuliahan().getId()))) {

							if (kodeProdi == null
									|| (kodeProdi != null && pertemuan.getPerkuliahan().getJurusan() != null
											&& pertemuan.getPerkuliahan().getJurusan().getId() != null
											&& pertemuan.getPerkuliahan().getJurusan().getId().equals(kodeProdi))) {

								if (kodeFakultas == null
										|| (kodeFakultas != null && pertemuan.getPerkuliahan().getJurusan() != null
												&& pertemuan.getPerkuliahan().getJurusan().getFakultas() != null
												&& pertemuan.getPerkuliahan().getJurusan().getFakultas().getId() != null
												&& pertemuan.getPerkuliahan().getJurusan().getFakultas().getId()
														.equals(kodeFakultas))) {

									if (hariIni.size() > 100 && baris == 1) {
										break;
									}

									hariIni.add(pertemuan);
								}
							}
						}
					}
				}

				//System.out.println("Pertemuan hari ini -> " + hariIni.size() + " total " + pertemuans.size());

				if (hariIni != null && !hariIni.isEmpty()) {

					Collections.sort(hariIni);

					Long randId = Common.randLong();
					java.util.List<String> slidesKh = new java.util.ArrayList<String>();
					int totalKartuKh = 0;
					int totalHadirKh = 0;

					i = 1;
					int j = 1;
					String tds = "";
					String tr = "";
					for (Pertemuan pertemuan : hariIni) {

						List<Dosen> dosens = pertemuan.ambilDosen();
						for (Dosen idDosen : dosens) {

							if (i == 500) {
								break;
							}

							String link = CommonMedia.getUrlFotoPengguna(new Tbmuser(idDosen), 152, 114);

							Statusabsensi statusabsensi = null;
							if (pertemuan.getId() != null) {

								statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
										pertemuan.retreiveAbsensiId(idDosen.getId()));

							}

							if (statusabsensi == null) {
								statusabsensi = ConstantValues.BELUM_ABSEN;
							}

							// Hitung total kartu & yang sudah absen (status bukan BELUM_ABSEN).
							totalKartuKh++;
							if (statusabsensi != null && statusabsensi != ConstantValues.BELUM_ABSEN) {
								totalHadirKh++;
							}

							String tableData = "<table   style='width:99%;'>";

							tableData += "<tr><td><strong>" + idDosen.getNama() + "</strong></td></tr>";
							tableData += "<tr><td>Kehadiran: " + (statusabsensi == null ? "" : statusabsensi.getNama())
									+ "</td></tr>";
							tableData += "<tr><td>" + pertemuan.getStatusPertemuan().getNama() + "</td></tr>";
							tableData += "<tr><td>" + pertemuan.getPerkuliahan().getMatakuliah().getNama() + " "
									+ pertemuan.getPerkuliahan().getSemester() + " "
									+ pertemuan.getPerkuliahan().getKelas() + "</td></tr>";

							if (pertemuan.getRuang() != null) {
								tableData += "<tr><td>" + pertemuan.getRuang().getNama() + "</td></tr>";
							}

							if (idDosen.getNidn() != null && !idDosen.getNidn().isEmpty()) {
								tableData += "<tr><td>" + idDosen.getNidn() + "</td></tr>";
							}

							if (pertemuan.getWaktuMulai() != null || pertemuan.getWaktuSelesai() != null) {
								tableData += "<tr><td>"
										+ (pertemuan.getWaktuMulai() == null ? "" : pertemuan.getWaktuMulai()) + " sd "
										+ (pertemuan.getWaktuSelesai() == null ? "" : pertemuan.getWaktuSelesai())
										+ "</td></tr>";
							}

							if (mahasiswa != null) {
								statusabsensi = null;
								if (pertemuan.getId() != null) {

									statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
											pertemuan.retreiveAbsensiId(mahasiswa.getId()));

								}

								if (statusabsensi == null) {
									statusabsensi = ConstantValues.BELUM_ABSEN;
								}

								tableData += "<tr><td><strong>Kehadiran " + mahasiswa.getNama() + " : "
										+ statusabsensi.getNama() + "</strong></td></tr>";
							}

							tableData += "</table>";

							// Klik kartu jadwal => buka popup detail 1 panel pertemuan (seperti e-Learning).
							String klikDetailPertemuan = pertemuan.getId() == null ? ""
									: (" onclick=\"if(window.khShow" + randId + ")khShow" + randId + "("
											+ pertemuan.getId()
											+ ");\" title=\"Klik untuk melihat detail pertemuan\"");

							String td = "<td style='" + cssKehadiran + ";cursor:pointer;'" + klikDetailPertemuan + ">"
									+ "<img src=\"" + link
									+ "\" style=\"height:100px;width: 85px !important;\" class=\"gambar_profile\" /><br>"
									+ tableData + "\n" + "</td>";

							tds += td;

							if (i % jml == 0) {

								tr += "<tr>" + tds + "  </tr>";

								if (i % (jml * baris) == 0) {

									String table = "<table " + cellSpacing + " style='width:99%;'>" + tr + "</table>";

									slidesKh.add(table);
									tr = "";
									j++;
								}

								tds = "";
							}

							i++;
						}
					}

					if (!tds.isEmpty()) {

						tr += "<tr>" + tds + "  </tr>";

						String table = "<table " + cellSpacing + " style='width:99%;'>" + tr + "</table>";

						slidesKh.add(table);
					}

					String tglDosen = Common.dateFormat4.get().format(WaktuUtil.getDate());
					pengumuman += bungkusSlideKehadiranKeren(randId,
							"Informasi Kehadiran Dosen Harian " + tglDosen, tglDosen, slidesKh, totalKartuKh,
							totalHadirKh, mobile);

				} else if (baris > 1) {
					pengumuman = "<strong><font style='color:red'>Tidak ada jadwal dosen untuk hari ini</font></strong>";
				}
				hariIni = null;
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		}

		return pengumuman;
	}

	public static String tampilkanKehadiranGuru(Tbmuser tbmuser, boolean mobile) {
		return tampilkanKehadiranGuru(tbmuser, mobile, 1);
	}

	@SuppressWarnings("unchecked")
	public static String tampilkanKehadiranGuru(Tbmuser tbmuser, boolean mobile, int baris) {
		String pengumuman = "";
		if (tbmuser == null || (tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getElearning())) {
			String sekarang = Common.dateFormat8.get().format(WaktuUtil.getDate());
			try {

				int jml = 5;

				try {
					jml = Integer.parseInt(Common.getKonfigurasi("jml_tampil_kehadiran_dalam_satu_baris_dekstop", "5")
							.getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:2019");
					// TODO: handle exception
				}

				if (mobile) {
					jml = 2;

					try {
						jml = Integer.parseInt(Common
								.getKonfigurasi("jml_tampil_kehadiran_dalam_satu_baris_mobile", "2").getNilai().trim());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:2029");
						// TODO: handle exception
					}
				}
				int i = 1;

				String cssKehadiran = Common.getKonfigurasi("css_tampilan_utama_kehadiran",
						"vertical-align: top;width:250px;\r\n" + "    text-align: center;\r\n"
								+ "    padding-top: 10px;\r\n" + "    padding-bottom: 10px;\r\n"
								+ "    border-radius: 5px;\r\n"
								+ "    box-shadow: rgba(0, 0, 0, 0.16) 0px 3px 6px, rgba(0, 0, 0, 0.23) 0px 3px 6px;\r\n"
								+ "")
						.getNilai();
				String cellSpacing = Common
						.getKonfigurasi("css_cell_spacing_tampilan_kehadiran", "cellpadding=\"10\" cellspacing=\"15\"")
						.getNilai();

				Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
//			Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();

				Map<Long, Pertemuan> pertemuans = pertemuansHarian.get(sekarang);
				if (pertemuans == null) {
					List<Pertemuan> pertemuansData = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.addOrder(Order.asc("waktuMulai")).add(Restrictions.eq("tanggal", WaktuUtil.getDate()))
							.add(Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"),
									Restrictions.isNotNull("perkuliahan")))
							.list();
					pertemuans = new HashMap<Long, Pertemuan>();
					for (Pertemuan pertemuan : pertemuansData) {
						pertemuans.put(pertemuan.getId(), pertemuan);
					}
					pertemuansData = null;
					pertemuansHarian.put(sekarang, pertemuans);
				}

				List<Pertemuan> hariIni = new ArrayList<Pertemuan>();
				List<Long> perkuliahans = null;
//			if (siswa != null) {
//				perkuliahans = siswa.ambilJadwalPelajaranDanParalel();
//			} else if (guru != null) {
//				perkuliahans = guru.ambilJadwalPelajaran(HibernateUtil.currentSession());
//			}

				Yayasan yayasan = tbmuser.ambilYayasan();
				Sekolah sekolah = tbmuser.ambilSekolah();

				try {
					Sekolah sekolahData = SekolahUtil.getSekolah();
					if (sekolahData != null && sekolahData.getId() != null) {
						sekolah = sekolahData;
					}

					Yayasan yayasanData = SekolahUtil.getYayasan();
					if (yayasanData != null && yayasanData.getId() != null) {
						yayasan = yayasanData;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/PengumumanAkademisAction.java:2086");
					// TODO: handle exception
				}

				for (Pertemuan pertemuan : pertemuans.values()) {
					if (pertemuan.getJadwalPelajaran() != null && pertemuan.getTanggal() != null
							&& sekarang.equals(Common.dateFormat8.get().format(pertemuan.getTanggal()))) {
						if (perkuliahans == null || (perkuliahans != null && pertemuan.getJadwalPelajaran() != null
								&& perkuliahans.contains(pertemuan.getJadwalPelajaran().getId()))) {

							if (yayasan == null || yayasan.getId() == null || (yayasan != null
									&& yayasan.getId() != null && pertemuan.getJadwalPelajaran().getYayasan() != null
									&& yayasan.getId().equals(pertemuan.getJadwalPelajaran().getYayasan().getId()))) {

								if (sekolah == null || sekolah.getId() == null
										|| (sekolah != null && sekolah.getId() != null
												&& pertemuan.getJadwalPelajaran().getSekolah() != null
												&& sekolah.getId()
														.equals(pertemuan.getJadwalPelajaran().getSekolah().getId()))) {
									if (hariIni.size() > 100 && baris == 1) {
										break;
									}
									hariIni.add(pertemuan);
								}
							}
						}
					}
				}

				//System.out.println("Pertemuan hari ini -> " + hariIni.size() + " total " + pertemuans.size());

				if (hariIni != null && !hariIni.isEmpty()) {

					Collections.sort(hariIni);

					Long randId = Common.randLong();
					java.util.List<String> slidesKh = new java.util.ArrayList<String>();
					int totalKartuKh = 0;
					int totalHadirKh = 0;

					i = 1;
					int j = 1;
					String tds = "";
					String tr = "";
					for (Pertemuan pertemuan : hariIni) {

						List<Guru> gurus = pertemuan.ambilGuru();
						for (Guru idGuru : gurus) {

							if (i == 500) {
								break;
							}

							String link = CommonMedia.getUrlFotoPengguna(new Tbmuser(idGuru), 152, 114);

							Statusabsensi statusabsensi = null;
							if (pertemuan.getId() != null) {

								statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
										pertemuan.retreiveAbsensiId(idGuru.getId()));

							}

							if (statusabsensi == null) {
								statusabsensi = ConstantValues.BELUM_ABSEN;
							}

							// Hitung total kartu & yang sudah absen (status bukan BELUM_ABSEN).
							totalKartuKh++;
							if (statusabsensi != null && statusabsensi != ConstantValues.BELUM_ABSEN) {
								totalHadirKh++;
							}

							String tableData = "<table  style='width:99%;'>";

							tableData += "<tr><td><strong>" + idGuru.getNama() + "</strong></td></tr>";
							tableData += "<tr><td>" + statusabsensi.getNama() + "</td></tr>";
							tableData += "<tr><td>" + pertemuan.getStatusPertemuan().getNama() + "</td></tr>";
							tableData += "<tr><td>" + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + " "
									+ pertemuan.getJadwalPelajaran().ambilNama() + "</td></tr>";

							if (pertemuan.getRuang() != null) {
								tableData += "<tr><td>" + pertemuan.getRuang().getNama() + "</td></tr>";
							}

							if (idGuru.getNuptk() != null && !idGuru.getNuptk().isEmpty()) {
								tableData += "<tr><td>" + idGuru.getNuptk() + "</td></tr>";
							}

							if (pertemuan.getWaktuMulai() != null || pertemuan.getWaktuSelesai() != null) {
								tableData += "<tr><td>"
										+ (pertemuan.getWaktuMulai() == null ? "" : pertemuan.getWaktuMulai()) + " sd "
										+ (pertemuan.getWaktuSelesai() == null ? "" : pertemuan.getWaktuSelesai())
										+ "</td></tr>";
							}

							if (siswa != null) {
								statusabsensi = null;
								if (pertemuan.getId() != null) {

									statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
											pertemuan.retreiveAbsensiId(siswa.getId()));

								}

								if (statusabsensi == null) {
									statusabsensi = ConstantValues.BELUM_ABSEN;
								}

								tableData += "<tr><td><strong>Kehadiran " + siswa.getNama() + " : "
										+ statusabsensi.getNama() + "</strong></td></tr>";
							}

							tableData += "</table>";

							// Klik kartu jadwal => buka popup detail 1 panel pertemuan (seperti e-Learning).
							String klikDetailPertemuan = pertemuan.getId() == null ? ""
									: (" onclick=\"if(window.khShow" + randId + ")khShow" + randId + "("
											+ pertemuan.getId()
											+ ");\" title=\"Klik untuk melihat detail pertemuan\"");

							String td = "<td style='" + cssKehadiran + ";cursor:pointer;'" + klikDetailPertemuan + ">"
									+ "<img src=\"" + link
									+ "\" style=\"height:100px;width: 85px !important;\" class=\"gambar_profile\" /><br>"
									+ tableData + "\n" + "</td>";

							tds += td;

							if (i % jml == 0) {

								tr += "<tr>" + tds + "  </tr>";

								if (i % (jml * baris) == 0) {

									String table = "<table " + cellSpacing + " style='width:99%;'>" + tr + "</table>";

									slidesKh.add(table);
									tr = "";
									j++;
								}

								tds = "";
							}

							i++;
						}
					}

					if (!tds.isEmpty()) {

						tr += "<tr>" + tds + "  </tr>";

						String table = "<table " + cellSpacing + " style='width:99%;'>" + tr + "</table>";

						slidesKh.add(table);
					}

					String tglGuru = Common.dateFormat4.get().format(WaktuUtil.getDate());
					pengumuman += bungkusSlideKehadiranKeren(randId,
							"Informasi Kehadiran Guru Harian " + tglGuru, tglGuru, slidesKh, totalKartuKh, totalHadirKh,
							mobile);

				} else if (baris > 1) {
					pengumuman = "<strong><font style='color:red'>Tidak ada jadwal guru mengajar untuk hari ini</font></strong>";
				}
				hariIni = null;
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}
		return pengumuman;
	}

	public static void tampilPengumuanLangsungTampil(List<PengumumanAkademis> listPengumumanAkademisLangsung, Tabs tabs,
			Tabpanels tabpanels) {
		System.out.println("listPengumumanAkademisLangsung -> " + listPengumumanAkademisLangsung.size());
		if (!listPengumumanAkademisLangsung.isEmpty()) {
			PerguruanTinggi selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
			Tbmuser tbmuser = Common.getCurrentUser();
			Sekolah sekolah = SekolahUtil.getSekolah();
			for (PengumumanAkademis akademis : listPengumumanAkademisLangsung) {

				MyTabConfig tabPengumumanData = new MyTabConfig(akademis.getJudul(),
						"/img/svg/information-circle-outline.svg");
				tabPengumumanData.setParent(tabs);
				tabPengumumanData.setClosable(false);

				Tabpanel tabpanelPengumumanData = new ais.ui.util.MyTabpanel();
				tabpanelPengumumanData.setParent(tabpanels);

				Borderlayout borderlayoutPengumumanData = new Borderlayout();
				borderlayoutPengumumanData.setParent(tabpanelPengumumanData);

				Center centerData = new Center();
				ais.ui.util.ZkCompat.setFlex(centerData, true);
				centerData.setParent(borderlayoutPengumumanData);

				TampilanPengumumanAkademisAction.tampil(centerData, akademis, sekolah, tbmuser, selectedPerguruanTinggi,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						}, false, false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

							}
						});
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void tampilPengumuman(Rows rows, PengumumanAkademis pengumumanAkademis, Sekolah sekolah,
			Tbmuser tbmuser, PerguruanTinggi selectedPerguruanTinggi, boolean tampilPengumumanLain, boolean awal,
			EventListener eventListenerSub) throws Exception {
		boolean mobile = Common.isMobile();

		Map<Long, PengumumanAkademis> top = new HashMap<Long, PengumumanAkademis>();
		Map<Long, PengumumanAkademis> bottom = new HashMap<Long, PengumumanAkademis>();
		if (pengumumanAkademis != null) {
			Map<Long, PengumumanAkademis> mapPengumuman = ConstantValues.ambilBerdasarClass(PengumumanAkademis.class);
			for (PengumumanAkademis p : mapPengumuman.values()) {
				if (p.getInduk() != null && p.getPosisiTombol().equals(PengumumanAkademis.ATAS)
						&& p.getInduk().getId().equals(pengumumanAkademis.getId())) {
					top.put(p.getId(), p);
				}
				if (p.getInduk() != null && p.getPosisiTombol().equals(PengumumanAkademis.BAWAH)
						&& p.getInduk().getId().equals(pengumumanAkademis.getId())) {
					bottom.put(p.getId(), p);
				}
			}
		}

		if (!top.isEmpty()) {

			List<PengumumanAkademis> pengumumanAkademisSub = new ArrayList<PengumumanAkademis>(top.values());
			Collections.sort(pengumumanAkademisSub);
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			Hbox hbox = new Hbox();
			row.appendChild(hbox);
			for (PengumumanAkademis akademis : pengumumanAkademisSub) {
				String img = null;
				LampiranLain lampiranLain = LampiranLain.ambil(akademis.getId(), LampiranLain.ICON_PENGUMUMAN);
				if (lampiranLain != null && lampiranLain.getId() != null) {
					try {
						img = lampiranLain.createLinkUri();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
				Toolbarbutton toolbarbutton = img == null || img.isEmpty()
						? new MyToolbarbuttonConfig(akademis.getLabelTombol())
						: new MyToolbarbuttonConfig(akademis.getLabelTombol(), img);
				toolbarbutton.setParent(hbox);
				toolbarbutton.setAttribute("akademis", akademis);
				if (eventListenerSub != null) {
					toolbarbutton.addEventListener("onClick", eventListenerSub);
				}
			}
		}

		String pengumuman = "";
		if (Common.bolehKonfigurasi("tampilkan_info_kehadiran_pengajar_di_halaman_utama")) {

			if (awal) {
				if (!mobile
						&& !pengumumanAkademis.getDiperuntukkan().equalsIgnoreCase(PengumumanAkademis.UNTUK_CALON_SISWA)
						&& !pengumumanAkademis.getDiperuntukkan()
								.equalsIgnoreCase(PengumumanAkademis.UNTUK_CALON_MAHASISWA)
						&& !pengumumanAkademis.getDiperuntukkan().equalsIgnoreCase(PengumumanAkademis.UNTUK_ALUMNI)) {
					pengumuman = sekolah == null || sekolah.getId() == null ? tampilkanKehadiranDosen(tbmuser,
							tbmuser.ambilJurusan(), tbmuser.ambilFakultas(), mobile, 1)
							: tampilkanKehadiranGuru(tbmuser, mobile, 1);
				}
			}
		}

		if (!PengumumanAkademis.galeries.containsKey(pengumumanAkademis.getId())) {
			PengumumanAkademis.reloadGaleries(pengumumanAkademis);
		}

		Map<Long, LampiranLain> lampiranLains = PengumumanAkademis.galeries.get(pengumumanAkademis.getId());

		if (lampiranLains != null && !lampiranLains.isEmpty()) {

			if (lampiranLains.size() == 1) {
				LampiranLain lampiranLain = lampiranLains.values().iterator().next();
				String link = lampiranLain.createLinkUri();
				pengumuman += "<img src=\"" + link + "\" style=\""
						+ (mobile ? pengumumanAkademis.getTinggiGaleriMobile() : pengumumanAkademis.getTinggiGaleri())
						+ "\" />\n";
			} else if (!lampiranLains.isEmpty()) {
				pengumuman += "<div class=\"slideshow-container\" style=\"background-color: rgba(255,255,255,0.4);"
						+ (mobile ? pengumumanAkademis.getTinggiGaleriMobile() : pengumumanAkademis.getTinggiGaleri())
						+ "\">\n";
				int i = 1;
				for (Long id : lampiranLains.keySet()) {
					LampiranLain lampiranLain = lampiranLains.get(id);
					String link = lampiranLain.createLinkUri();
					String text = lampiranLain.getDeskripsi();
					if (text == null) {
						text = "";
					}

					pengumuman += "\n<div name=\"mySlides" + pengumumanAkademis.getId()
							+ "\" class=\"mySlides fade\">\n" + "<div class=\"numbertext\">\n" + i + " / "
							+ lampiranLains.size() + "</div>\n"
							+ (pengumumanAkademis.getGaleryBerupaHtml() ? text
									: "<img src=\"" + link + "\" style=\"width:100%;\" />\n" + "<div class=\"text\">\n"
											+ text + "</div>\n")
							+ "</div>";
					i++;

				}
				pengumuman += "</div><br>\n";
				pengumuman += "<div style=\"text-align:center\">\n";

				for (@SuppressWarnings("unused")
				Long id : lampiranLains.keySet()) {
					pengumuman += "<span name=\"dot" + pengumumanAkademis.getId() + "\"  class=\"dot\"></span> \n";
				}
				pengumuman += "</div><br>\n";
			}

			pengumuman += "<script>\n" + "var slideIndex" + pengumumanAkademis.getId() + " = 0;\n" + "showSlides"
					+ pengumumanAkademis.getId() + "();\n" +

					"function showSlides" + pengumumanAkademis.getId() + "() {\n" + "  var i"
					+ pengumumanAkademis.getId() + ";\n" + "  var slides = document.getElementsByName(\"mySlides"
					+ pengumumanAkademis.getId() + "\");\n" + "  var dots = document.getElementsByName(\"dot"
					+ pengumumanAkademis.getId() + "\");\n" + "  for (i" + pengumumanAkademis.getId() + " = 0; i"
					+ pengumumanAkademis.getId() + " < slides.length; i" + pengumumanAkademis.getId() + "++) {\n"
					+ "    slides[i" + pengumumanAkademis.getId() + "].style.display = \"none\";  \n" + "  }\n"
					+ "  slideIndex" + pengumumanAkademis.getId() + "++;\n" + "  if (slideIndex"
					+ pengumumanAkademis.getId() + " > slides.length) {slideIndex" + pengumumanAkademis.getId()
					+ " = 1}    \n" + "  for (i" + pengumumanAkademis.getId() + " = 0; i" + pengumumanAkademis.getId()
					+ " < dots.length; i" + pengumumanAkademis.getId() + "++) {\n" + "    dots[i"
					+ pengumumanAkademis.getId() + "].className = dots[i" + pengumumanAkademis.getId()
					+ "].className.replace(\" active\", \"\");\n" + "  }\n" + "  slides[slideIndex"
					+ pengumumanAkademis.getId() + "-1].style.display = \"block\";  \n" + "  dots[slideIndex"
					+ pengumumanAkademis.getId() + "-1].className += \" active\";\n" + "  setTimeout(showSlides"
					+ pengumumanAkademis.getId() + ", " + (pengumumanAkademis.getSlideWaktu() * 1000) + "); \n" + "}\n"
					+ "</script>";

		}

		if (pengumumanAkademis.getKlassData() != null) {
			try {
				Object o = Class.forName(pengumumanAkademis.getKlassData()).newInstance();
				if (o instanceof Window) {
					Window window = (Window) o;
					window.setWidth("100%");
					window.setBorder("none");
					window.setStyle("min-height:1200px");
					if (tbmuser != null && tbmuser.getUserId() != null) {
						Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
						if (desktopHeight != null) {
							window.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
						}
					}
					MyFormRow row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(window);
				} else if (o instanceof MyPortallayout) {
					MyPortallayout window = (MyPortallayout) o;
					window.setWidth("100%");
					window.setStyle("min-height:1200px");
					if (tbmuser != null && tbmuser.getUserId() != null) {
						Integer desktopHeight = MainAction.desktopHeights.get(tbmuser.getUserId());
						if (desktopHeight != null) {
							window.setStyle("min-height:" + (desktopHeight * 0.9) + "px");
						}
					}
					MyFormRow row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(window);
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Html(pengumuman));

//		System.out.println("tbmuser -> " + tbmuser + " "
//				+ (tbmuser == null || tbmuser.getOrangTua() == null ? "" : tbmuser.getOrangTua().getNamaAyah()));

		if (tbmuser != null && tbmuser.getOrangTua() != null) {

			Group group = new ais.ui.util.MyGroupConfig("Data Anak");
			group.setParent(rows);

			row = new MyFormRow();
			row.setParent(rows);

			Hbox hb = new Hbox();
			row.appendChild(hb);

			try {
				JSONObject o = new JSONObject(tbmuser.getOrangTua().getAnak());
				Iterator<String> keys = o.keys();
				while (keys.hasNext()) {
					String key = keys.next();
					if (key.startsWith("siswa")) {
						final Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(o, key));
						if (siswa != null) {
							Vbox vbox = new Vbox();
							vbox.setPack("center");
							vbox.setParent(hb);
							CommonMedia.tampilkanGambarKecil(siswa).setParent(vbox);

							new MyLabelBold(siswa.getNama() + " ("
									+ (siswa.getKelas() == null ? "tanpa kelas" : siswa.getKelas().getNama()) + ")")
									.setParent(vbox);

							String bodyLoginTombol = "Masuk Sebagai Anak Anda";

							MyToolbarbutton formulir = new MyToolbarbutton("fa-sign-in",
									Common.getBahasaConfig(bodyLoginTombol));
							formulir.getLabelC().setStyle("font-size:15px");
							vbox.appendChild(formulir);
							formulir.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									String code = siswa.urlLogin();
									Clients.confirmClose(null);
									ExecutionsCtrl.sendRedirect(Common.getRequestHostWithProtocol() + "/logoff?param="
											+ URLEncoder.encode(code, "UTF-8"));
								}
							});
						}
					} else if (key.startsWith("mahasiswa")) {
						final Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(o, key));
						if (mahasiswa != null) {
							Vbox vbox = new Vbox();
							vbox.setParent(hb);
							CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(vbox);
							new MyLabelBold(mahasiswa.getNama() + " (" + (mahasiswa.getNim()) + ")").setParent(vbox);

							String bodyLoginTombol = "Masuk Sebagai Anak Anda";

							MyToolbarbutton formulir = new MyToolbarbutton("fa-sign-in",
									Common.getBahasaConfig(bodyLoginTombol));
							formulir.getLabelC().setStyle("font-size:15px");
							vbox.appendChild(formulir);
							formulir.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									String code = mahasiswa.urlLogin();
									Clients.confirmClose(null);
									ExecutionsCtrl.sendRedirect(Common.getRequestHostWithProtocol() + "/logoff?param="
											+ URLEncoder.encode(code, "UTF-8"));
								}
							});
						}
					}
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		}

		if (!bottom.isEmpty()) {

			List<PengumumanAkademis> pengumumanAkademisSub = new ArrayList<PengumumanAkademis>(bottom.values());
			Collections.sort(pengumumanAkademisSub);
			row = new MyFormRow();
			row.setParent(rows);
			Hbox hbox = new Hbox();
			row.appendChild(hbox);
			for (PengumumanAkademis akademis : pengumumanAkademisSub) {
				String img = null;
				LampiranLain lampiranLain = LampiranLain.ambil(akademis.getId(), LampiranLain.ICON_PENGUMUMAN);
				if (lampiranLain != null && lampiranLain.getId() != null) {
					try {
						img = lampiranLain.createLinkUri();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}
				Toolbarbutton toolbarbutton = img == null || img.isEmpty()
						? new MyToolbarbuttonConfig(akademis.getLabelTombol())
						: new MyToolbarbuttonConfig(akademis.getLabelTombol(), img);
				toolbarbutton.setParent(hbox);
				toolbarbutton.setAttribute("akademis", akademis);
				if (eventListenerSub != null) {
					toolbarbutton.addEventListener("onClick", eventListenerSub);
				}
			}
		}

		if ((!pengumumanAkademis.getDiperuntukkan().equalsIgnoreCase(PengumumanAkademis.UNTUK_CALON_SISWA)
				&& !pengumumanAkademis.getDiperuntukkan().equalsIgnoreCase(PengumumanAkademis.UNTUK_CALON_MAHASISWA)
				&& pengumumanAkademis.getTampilkanPengumumanLain()) || tampilPengumumanLain) {

			Session session = HibernateUtil.currentNativeSession();
			Criteria criteria;
			if (pengumumanAkademis.getDiperuntukkan().equalsIgnoreCase(PengumumanAkademis.UNTUK_CALON_SISWA)) {

				Sekolah selectedSekolah = SekolahUtil.getSekolah();
				Yayasan selectedYayasan = SekolahUtil.getYayasan();

				criteria = TampilanPengumumanPSBAction.initCriteriaStatic(true, selectedSekolah, selectedYayasan);
			} else if (pengumumanAkademis.getDiperuntukkan()
					.equalsIgnoreCase(PengumumanAkademis.UNTUK_CALON_MAHASISWA)) {
				criteria = TampilanPengumumanPMBAction.initCriteriaStatic(true, selectedPerguruanTinggi);
			} else {
				criteria = TampilanPengumumanAkademisAction.initCriteriaStatic(true, tbmuser, selectedPerguruanTinggi,
						null, session);
			}

			List<PengumumanAkademis> pengumumanAkademisLain = ConstantValues.simpleList(criteria,
					PengumumanAkademis.class);
			// session.disconnect();
			ais.common.Common.closeOpenedSession(session);

			tampilPengumumanLain(rows, pengumumanAkademis.getId(), pengumumanAkademisLain);

		}

		if (!pengumumanAkademis.getDiperuntukkan().equalsIgnoreCase(PengumumanAkademis.UNTUK_CALON_SISWA)
				&& !pengumumanAkademis.getDiperuntukkan().equalsIgnoreCase(PengumumanAkademis.UNTUK_CALON_MAHASISWA)
				&& pengumumanAkademis.getTampilkanProfile()) {
			MyFormRow rowLagi = new MyFormRow();
			rowLagi.setParent(rows);
			try {
				ProfileAction.initProfile(tbmuser, rowLagi, null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		String currentLang = null;
		try {
			currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (currentLang == null) {
			currentLang = Tbmuser.INDONESIA;
		}

		// Judul & isi: bila bahasa aktif BUKAN Indonesia, terjemahkan otomatis dari teks Indonesia via
		// TRANSLATER INTERNAL (mengikuti English/Arab/Mandarin) — TANPA disimpan ke DB. Kolom Inggris lama
		// tidak lagi dipakai/diwajibkan.
		boolean _isIndoDetil = currentLang.equals(Tbmuser.INDONESIA);
		String _judulTampil = _isIndoDetil ? pengumumanAkademis.getJudul()
				: ais.common.Common.terjemahDinamis(pengumumanAkademis.getJudul());
		String _isiTampil = pengumumanAkademis.getCatatan() == null ? "" : pengumumanAkademis.getCatatan();
		if (!_isIndoDetil) {
			_isiTampil = ais.common.Common.terjemahDinamisHtml(_isiTampil);
		}
		if (_isiTampil == null) {
			_isiTampil = "";
		}

		if (pengumumanAkademis.getKategoriPengumuman() == null
				|| !pengumumanAkademis.getKategoriPengumuman().getMerupakanPengumumanUtama()) {
			Group group = new ais.ui.util.MyGroupConfig(_judulTampil);
			group.setParent(rows);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Html(
				"<p align='justify'>" + _isiTampil.replaceAll("560px", "260px") + "</p>"));

	}

	public static void tampilPengumumanLain(Rows rowsa, Long idPengumuan,
			List<PengumumanAkademis> pengumumanAkademisLain) {

		if (!pengumumanAkademisLain.isEmpty()) {

			String currentLang = null;
			try {
				currentLang = (String) Sessions.getCurrent(true).getAttribute("current_lang");
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			if (currentLang == null) {
				currentLang = Tbmuser.INDONESIA;
			}

			MyFormRow rowa = new MyFormRow();

			rowa.setParent(rowsa);

			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setSclass("fgrid");
			grid.setStyle("background-color: rgba(255,255,255,0.4);");
			grid.setParent(rowa);
			grid.setMold("paging");
			grid.setPageSize(25);
			Rows rows = new Rows();
			rows.setParent(grid);

			boolean ada = false;
			KategoriPengumuman kategoriPengumuman = new KategoriPengumuman();
			kategoriPengumuman.setId(-1L);
			for (PengumumanAkademis pengumumanAkademis : pengumumanAkademisLain) {

				if (idPengumuan != null && idPengumuan.equals(pengumumanAkademis.getId())) {
					continue;
				}

				final Long p = pengumumanAkademis.getId();
				try {
					KategoriPengumuman kategoriPengumumanTemporari = (KategoriPengumuman) pengumumanAkademis
							.getKategoriPengumuman();
					if (kategoriPengumumanTemporari != null && (kategoriPengumuman == null
							|| !kategoriPengumuman.getId().equals(kategoriPengumumanTemporari.getId()))) {
						kategoriPengumuman = kategoriPengumumanTemporari;

						if (currentLang.equals(Tbmuser.INDONESIA)) {
							Group group = new ais.ui.util.MyGroupConfig(kategoriPengumuman.getNama());
							group.setParent(rows);
						} else if (currentLang.equals(Tbmuser.ENGLISH)) {
							Group group = new ais.ui.util.MyGroupConfig(kategoriPengumuman.getNamaEn());
							group.setParent(rows);
						}

					} else if (kategoriPengumumanTemporari == null && kategoriPengumuman != null) {
						kategoriPengumuman = null;
						Group group = new ais.ui.util.MyGroupConfig("Pengumuman dan Informasi");
						group.setParent(rows);

					}
				} catch (Exception e) {
					kategoriPengumuman = null;
					Group group = new ais.ui.util.MyGroupConfig("Pengumuman dan Informasi");
					group.setParent(rows);
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");

				row.setParent(rows);

				String text = currentLang.equals(Tbmuser.INDONESIA) ? pengumumanAkademis.getJudul()
						: ais.common.Common.terjemahDinamis(pengumumanAkademis.getJudul());
				if (text == null) {
					text = pengumumanAkademis.getJudul() == null ? "" : pengumumanAkademis.getJudul();
				}

				text = text.length() > 255 ? text.substring(0, 254) + ".." : text;

				Toolbarbutton toolbarbutton = new ais.ui.util.MyToolbarbuttonConfig(text);
				toolbarbutton.setStyle("font-size: 11px;");

				row.appendChild(toolbarbutton);

				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanPengumumanAkademisAction.prosess(p, null, null, true, null);
					}
				});

				ada = true;
			}

			if (!ada) {
				rowa.setVisible(false);
			}
		}

	}

	public boolean onSave(Event event) throws Exception {
		if (judul.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Judul",
					"Kolom Judul belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Judul.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		// if (catatan.getValue().trim().equals("")) {
		// MyMessageboxConfig.show("Catatan harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		Session session = HibernateUtil.currentSession();
		if (pengumumanAkademis.getId() != null) {
			pengumumanAkademis = (PengumumanAkademis) session.load(PengumumanAkademis.class,
					pengumumanAkademis.getId());
		}

		// Field Inggris tak lagi disunting di form (translater internal menangani tampilan) — pertahankan
		// nilai lama bila komponen tidak dibangun agar tak menimpanya dengan null.
		if (judulEn != null) {
			pengumumanAkademis.setJudulEn(judulEn.getValue());
		}
		if (catatanEn != null) {
			pengumumanAkademis.setCatatanEn(catatanEn.getValue());
		}

		pengumumanAkademis.setHanyaUntukAngkatan("," + hanyaUntukAngkatan.getValue().trim() + ",");
		pengumumanAkademis.setHanyaUntuk("," + hanyaUntuk.getValue().trim() + ",");
		pengumumanAkademis.setTetapTampilkanPengumumanMeskipunSudahKelewat(
				tetapTampilkanPengumumanMeskipunSudahKelewat.isChecked());
		pengumumanAkademis.setDiperuntukkan(
				(String) (diperuntukkan.getSelectedItem() == null ? null : diperuntukkan.getSelectedItem().getValue()));
		pengumumanAkademis.setSampai(sampai.getValue());
		pengumumanAkademis.setTanggal(tanggal.getValue());
		pengumumanAkademis.setJudul(judul.getValue());
		pengumumanAkademis.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		pengumumanAkademis.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		if (tinggiGaleri != null) {
			pengumumanAkademis.setTinggiGaleri(tinggiGaleri.getValue().trim());
		}
		if (tinggiGaleriMobile != null) {
			pengumumanAkademis.setTinggiGaleriMobile(tinggiGaleriMobile.getValue());
		}

		if (galeryBerupaHtml != null) {
			pengumumanAkademis.setGaleryBerupaHtml(galeryBerupaHtml.isChecked());
		}

		String myoleh = pengumumanAkademis.getOleh();
		if (myoleh == null || myoleh.trim().isEmpty()) {
			if (tbmuser != null) {
				if (tbmuser.getMahasiswa() != null) {
					myoleh = tbmuser.getMahasiswa().getNim() + " - " + tbmuser.getMahasiswa().getNama()
							+ " (Mahasiswa)";
				} else if (tbmuser.getMahasiswa() != null) {
					myoleh = tbmuser.ambilDosen().getNama() + " (Dosen)";
				} else {
					myoleh = tbmuser.getUserId() + " (" + tbmuser.hakAkses().getRoleName() + ")";
				}
			}
		}

		pengumumanAkademis.setOleh(myoleh);
		pengumumanAkademis.setCatatan(catatan.getValue());
		pengumumanAkademis.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
		pengumumanAkademis.setAktif(aktif.isChecked());
		pengumumanAkademis.setBolehDiberiKomentar(bolehDiberiKomentar.isChecked());
		pengumumanAkademis.setAdaVideoConference(adaVideoConference.isChecked());
		pengumumanAkademis.setTampilkanPengumumanLain(tampilkanPengumumanLain.isChecked());

		pengumumanAkademis.setTampilkanProfile(tampilkanProfile.isChecked());

		pengumumanAkademis.setKorespondensi(
				korespondensi.getValue().trim().isEmpty() ? tbmuser.getUserId() : korespondensi.getValue().trim());

		pengumumanAkademis.setBroadcastAdmin(broadcastAdmin.isChecked());
		pengumumanAkademis.setBroadcastKeDosen(broadcastKeDosen.isChecked());
		pengumumanAkademis.setBroadcastKeMahasiswaAktif(broadcastKeMahasiswaAktif.isChecked());
		pengumumanAkademis.setBroadcastCalonMahasiswa(broadcastCalonMahasiswa.isChecked());
		pengumumanAkademis.setBroadcastKeMahasiswaAlumni(broadcastKeMahasiswaAlumni.isChecked());
		pengumumanAkademis.setBroadcastKeMahasiswaCuti(broadcastKeMahasiswaCuti.isChecked());

		pengumumanAkademis
				.setKategoriPengumuman((KategoriPengumuman) (kategoriPengumuman.getSelectedItem() == null ? null
						: kategoriPengumuman.getSelectedItem().getValue()));

		pengumumanAkademis.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));
		pengumumanAkademis.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		pengumumanAkademis.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
		pengumumanAkademis.setBroadcastKeGuru(broadcastKeGuru.isChecked());
		pengumumanAkademis.setBroadcastKeSiswaAktif(broadcastKeSiswaAktif.isChecked());

		pengumumanAkademis.setHanyaUntukUsername("," + hanyaUntukUsername.getValue() + ",");
		pengumumanAkademis.setAdaVideoConferenceGoogleMeet(adaVideoConferenceGoogleMeet.isChecked());
		if (slideWaktu != null) {
			pengumumanAkademis.setSlideWaktu(slideWaktu.getValue());
		}

		pengumumanAkademis.setIsiPolling(isiPollings == null ? null : isiPollings.toString());

		pengumumanAkademis.setPerguruanTinggi((PerguruanTinggi) (perguruanTinggi.getSelectedItem() == null ? null
				: perguruanTinggi.getSelectedItem().getValue()));
		pengumumanAkademis.setPosisiTombol(
				(String) (posisiTombol.getSelectedItem() == null ? null : posisiTombol.getSelectedItem().getValue()));

		pengumumanAkademis.setLabelTombol(labelTombol.getValue());

		pengumumanAkademis.setNomorUrut(nomorUrut.getValue());
		pengumumanAkademis.setLangsungMunculDiTab(langsungMunculDiTab.isChecked());
		if (langsungTampilBeranda != null) {
			pengumumanAkademis.setLangsungTampilBeranda(langsungTampilBeranda.isChecked());
		}
		pengumumanAkademis.setInduk(
				(PengumumanAkademis) (induk.getSelectedItem() == null ? null : induk.getSelectedItem().getValue()));

		pengumumanAkademis.setKlassData(klassData.getValue());

		Common.refreshSaveOrUpdate(session, pengumumanAkademis);
		session.flush();

		// CATATAN: item Galeri yang diunggah memakai REF SEMENTARA (mode Tambah) TIDAK perlu dipindahkan
		// di sini — blok timer di bawah SUDAH melakukannya: setiap LampiranLain di `maps` di-set
		// setRef(pengumumanAkademis.getId()) memakai sesi STREAMING (LampiranLain berada di DB streaming,
		// bukan sesi utama). Setelah pengumuman tersimpan, id asli sudah tersedia untuk dipakai di sana.
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();

					for (LampiranLain lampiranLain : maps.values()) {

						if (lampiranLain.getId() != null) {
							session.refresh(lampiranLain);
							lampiranLain.setRef(pengumumanAkademis.getId());

							session.getTransaction().begin();
							session.update(lampiranLain);
							session.getTransaction().commit();
						}
					}

					StreamingHibernateUtil.getInstance().closeSession();
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

				ais.action.master.helper.BroadcastHelper.kirimEmailKeKorespondensi(pengumumanAkademis);
				ais.action.master.helper.BroadcastHelper.broadcastEmail(pengumumanAkademis);

				PengumumanAkademis.reloadGaleries(pengumumanAkademis);
			}
		});

		try {
			if (pengumumanAkademis.getAdaVideoConferenceGoogleMeet()) {

				CalendarUtil calendarUtil = new CalendarUtil(tbmuser);
				calendarUtil.proses(pengumumanAkademis, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

					}
				});

			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(pengumumanAkademis.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengumumanAkademis.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("judul", searchjudul.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchisi.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("catatan", searchisi.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchPerguruanTinggi.getSelectedItem() == null
						|| searchPerguruanTinggi.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("perguruanTinggi"),
										Restrictions.eq("perguruanTinggi",
												searchPerguruanTinggi.getSelectedItem().getValue())))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchKategoriPengumuman.getSelectedItem() == null
						|| searchKategoriPengumuman.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kategoriPengumuman",
										searchKategoriPengumuman.getSelectedItem().getValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchDiperuntukkan.getSelectedItem() == null
						|| searchDiperuntukkan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("diperuntukkan", searchDiperuntukkan.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PengumumanAkademis> pengumumanAkademis = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pengumumanAkademis);
		grid.setRowRenderer(new PengumumanAkademisRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static Grid initIsiPolling(final PengumumanAkademis pengumumanAkademis, final EventListener eventListener)
			throws Exception {

		Grid subGrid = new Grid();

		Columns subcolumns = new Columns();
		subcolumns.setParent(subGrid);

		MyColumnConfig subcolumnRef = new MyColumnConfig();
		subcolumnRef.setParent(subcolumns);
		subcolumnRef.setWidth("90%");

		MyColumnConfig subcolumn = new MyColumnConfig("Hapus");
		subcolumn.setParent(subcolumns);

		final Rows subrowsRefs = new Rows();
		subrowsRefs.setParent(subGrid);
		JSONArray isiPollings = new JSONArray(pengumumanAkademis.getIsiPolling());
		for (int i = 0; i < isiPollings.length(); i++) {
			JSONObject jsonObject = isiPollings.getJSONObject(i);
			addIsiPolling(jsonObject, pengumumanAkademis, subrowsRefs, eventListener);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Polling", "/img/add_item.png");
		button.setTooltiptext("Tambah Isi Polling");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow addWindow = new MyWindow("Tambah Polling", "none", true);
				addWindow.setHeight("90%");
				addWindow.setWidth("900px");
				addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

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
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				final Long ref = Common.randLong();

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Judul *"));
				final Textbox nama;
				row.appendChild(nama = new Textbox());
				nama.setWidth("90%");
				nama.setRows(2);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Isi / Keterangan *"));
				final MyCkEditor pengarang;
				row.appendChild(pengarang = new MyCkEditor());
				pengarang.setWidth("90%");
				pengarang.setHeight("500px");

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
						addWindow.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (nama.getValue().trim().isEmpty()) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Judul polling",
									"Kolom Judul polling belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Judul polling.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}
						if (pengarang.getValue().trim().isEmpty()) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Isi polling",
									"Kolom Isi polling belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Isi polling.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}

						addWindow.detach();

						JSONObject jsonObject = new JSONObject();

						jsonObject.put("ref", ref);
						jsonObject.put("judul", nama.getValue().trim());
						jsonObject.put("isi", pengarang.getValue().trim());

						JSONArray jsonArray = new JSONArray(pengumumanAkademis.getIsiPolling());
						jsonArray.put(jsonObject);
						pengumumanAkademis.setIsiPolling(jsonArray.toString());

						if (pengumumanAkademis.getId() != null) {
							Common.refreshUpdate(pengumumanAkademis);
						}

						eventListener.onEvent(new Event("", null, pengumumanAkademis));

						addIsiPolling(jsonObject, pengumumanAkademis, subrowsRefs, eventListener);
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(addWindow);
				addWindow.onModal();
			}
		});
		button.setParent(subcolumnRef);

		return subGrid;
	}

	private static void addIsiPolling(final JSONObject jsonObject, final PengumumanAkademis pengumumanAkademis,
			Rows subrowsRefs, final EventListener eventListener) throws Exception {
		final Long ref = ais.common.CommonJSONUtil.ambilLong(jsonObject, "ref");
		final MyFormRow subrow = new MyFormRow();
		subrow.setParent(subrowsRefs);
		subrow.setValign("top");
		subrow.setAttribute("o", jsonObject.toString());

		Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
		vbox.appendChild(new MyCaptionStyled(jsonObject.getString("judul")));
		vbox.appendChild(new ais.ui.util.MyHtml(jsonObject.getString("isi")));
		subrow.appendChild(vbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
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

									JSONArray jsonArrayCopy = new JSONArray();
									JSONArray jsonArray = new JSONArray(pengumumanAkademis.getIsiPolling());

									for (int ii = 0; ii < jsonArray.length(); ii++) {
										JSONObject o = jsonArray.getJSONObject(ii);
										Long refO = ais.common.CommonJSONUtil.ambilLong(o, "ref");
										if (!refO.equals(ref)) {
											jsonArrayCopy.put(o);
										}
									}

									pengumumanAkademis.setIsiPolling(jsonArrayCopy.toString());

									if (pengumumanAkademis.getId() != null) {
										Common.refreshUpdate(pengumumanAkademis);
									}

									eventListener.onEvent(new Event("", null, pengumumanAkademis));
									subrow.detach();
								}

							}
						});

			}
		});
		button.setParent(subrow);
	}
}
