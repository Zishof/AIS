package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
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
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.InitDataHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelompokGelombang;
import ais.database.model.sekolah.PaketPsb;
import ais.database.model.sekolah.PaketPsbPunyaGelombangPendaftaranPsb;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.StatusAwalSiswa;
import ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class GelombangPendaftaranPsbAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchta;
	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Checkbox searchaktif;
	private Textbox nama;
	private Combobox sekolah;
	private Textbox keterangan;
	private Combobox jenisBiayaSekolah;
	private boolean edit = false;
	private boolean delete = false;

	private GelombangPendaftaranPsb gelombangPendaftaranPsb;
	private MyToolbarbuttonConfig add;
	private Combobox yayasan;
	private Textbox informasi;
	private Combobox tahunAjaran;
	private MyDatebox mulai;
	private MyDatebox sampai;

	private Tabpanel kelompok;

	public void onKelompok(Event event) {
		if (kelompok.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kelompok);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kelompok_gelombang.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tabPengaturanNIS;

	public void onPengaturanNIS(Event event) {
		if (tabPengaturanNIS.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tabPengaturanNIS);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/format_nis.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel kebutuhanKhusus;

	public void onKebutuhanKhusus(Event event) {
		if (kebutuhanKhusus.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(kebutuhanKhusus);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/kebutuhan_khusus_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel galeriFoto;

	public void onGaleriFoto(Event event) {
		if (galeriFoto.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(galeriFoto);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/galeri_foto_psb.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel verifikasi;

	public void onVerifikasiKelengkapanBerkas(Event event) {
		if (verifikasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(verifikasi);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/verifikasi_kelengkapan_calon_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel verifikasiTambahan;

	public void onVerifikasiTambahan(Event event) {
		if (verifikasiTambahan.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(verifikasiTambahan);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/parameter_verifikasi_calon_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel paket;

	public void onPaket(Event event) {
		if (paket.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(paket);
			MyInclude iframe = new MyInclude("/pages/master/sekolah/paket_psb.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel konfigurasiCalonBiodataSiswa;

	public void onKonfigurasiCalonBiodataSiswa(Event event) {
		if (konfigurasiCalonBiodataSiswa.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(konfigurasiCalonBiodataSiswa);
			MyInclude iframe = new MyInclude("/pages/master/konfigurasi_biodata_calon_siswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tampilkanMatapelajaranSekolah;

	public void onMatapelajaranSekolah(Event event) {
		if (tampilkanMatapelajaranSekolah.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanMatapelajaranSekolah);
			MyInclude iframe = new MyInclude("/pages/master/matapelajaran_sekolah.zul");
			iframe.setParent(window);
		}
	}

	private Set<VerifikasiKelengkapanCalonSiswa> selectedVerifikasiKelengkapanCalonSiswa;
	private Combobox jenisBiayaSekolahLulus;
	private Textbox kelasVerifikasiRapor;
	private Combobox statusAwalSiswa;
//	private MyCheckboxConfig harusSebagaiAnakAlumni;
	private MyCheckboxConfig harusSebagaiAlumni;
	private Combobox alumniDari;
	private Combobox classUntukPendaftaran;
	private Combobox classUntukMelengkapiBerkas;
	private MyCheckboxConfig munculkanTagihanSetelahDaftar;
	private MyCheckboxConfig harusSebagaiSaudara;
//	private MyCheckboxConfig harusSebagaiSaudaraAlumni;
	protected LampiranLain kop;
	private MyCheckboxConfig sesuaiKelas;
	private MyCheckboxConfig sesuaiKelasSaatDiterima;
	private MyCheckboxConfig otomatisLoginSetelahDaftar;
	protected LampiranLain lainMahasiswa;
	private Combobox penjurusanSekolah;
	private MyCheckboxConfig otomatisDiterimaKetikaSudahBayarReg;
	private MyCheckboxConfig otomatisDapatNisKetikaSudahBayarReg;
	private MyCheckboxConfig otomatisDapatNisKetikaSudahBayarDaftarUlang;
	protected LampiranLain bg_ppdb;
	private MyCheckboxConfig hanyapaket;
	private HashMap<Long, PaketPsbPunyaGelombangPendaftaranPsb> selectedPaketPsbPunyaGelombangPendaftaranPsb;
	private Row rowJp;
	private MyCheckboxConfig dibatasiUmur;
	private Intbox umurminimal;
	private Intbox umurmaksimal;
	private MyDatebox umurDihitungTanggal;
	private MyCheckboxConfig siswaPindahanBolehMendaftar;
	private MyCheckboxConfig hanyaUntukAnakPegawai;
	private MyCheckboxConfig tampilAlur;
	private MyCheckboxConfig tampilPembayaranViaPaymentGateway;
	private MyCheckboxConfig tampilLengkapiBerkas;
	private MyCheckboxConfig tampilInformasiKelulusan;
	private MyCheckboxConfig tampilUjian;
	private MyCheckboxConfig tampilCetakNoReg;
	private MyCheckboxConfig tampilCetakBiodata;
	private MyCheckboxConfig tampilCetakKartuUjian;
	private MyCheckboxConfig tampilKeteranganDiterima;
	private MyCheckboxConfig tampilLogout;

	private MyCheckboxConfig tampilFormLampiranDiHalamanUtama;
	private MyCheckboxConfig tampilFormTambahanDiHalamanUtama;
	private Combobox kelompokGelombang;
	private MyCheckboxConfig cetakKartuUjianHarusVerifikasiBerkas;
	private Combobox jenisBiayaSekolahTerverifikasi;
	private Textbox tingkatDariAlumni;
	private MyCheckboxConfig terdapatVerifikasiDenganNikAlumni;
	private Textbox tahunAkademikAlumni;
	private Intbox kuotaDiterima;
	private MyCheckboxConfig langsungDapatNisSaatDaftar;
	private MyCheckboxConfig terdapatVerifikasiDenganNikSibling;
	private MyCheckboxConfig tampilkanQrCodeMahasiswaSetelahDapatNim;
	private Textbox kelasDariAlumni;

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

		Common.generateTahunAjaranDanSemua(searchta);
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

		String[] contents = new String[] { "id", "tahunAjaran", "statusAwalSiswa", "nama", "mulai", "sampai",
				"informasi", "sekolah", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GelombangPendaftaranPsb.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	        FilterLanjutHelper.setup(comp);
}

	class GelombangPendaftaranPsbRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GelombangPendaftaranPsb gelombangPendaftaranPsb = (GelombangPendaftaranPsb) arg1;
			gelombangPendaftaranPsb.chekKuotaPendaftar();
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

						MyTabConfig tabRuangUjian = new MyTabConfig("Data Ujian");
						tabRuangUjian.setParent(tabs);

						MyTabConfig tabRuangRuang = new MyTabConfig("Ruangan Ujian");
						tabRuangRuang.setParent(tabs);

						MyTabConfig tabJadwal = new MyTabConfig("Jadwal Ujian");
						tabJadwal.setParent(tabs);

						MyTabConfig tabJadwalPertemuan = new MyTabConfig("Jadwal Pertemuan");
						tabJadwalPertemuan.setParent(tabs);

						MyTabConfig tabPembagianKelas = new MyTabConfig("Pembagian Kelas");
						tabPembagianKelas.setParent(tabs);

						MyTabConfig tabRapor = new MyTabConfig("Verifikasi Nilai Rapor");
						tabRapor.setParent(tabs);

						MyTabConfig tabParameterTambahan = new MyTabConfig("Parameter Tambahan");
						tabParameterTambahan.setParent(tabs);

						MyTabConfig tabParameter = new MyTabConfig("Parameter Verifikasi");
						tabParameter.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel ruangUjianTabpanel = new ais.ui.util.MyTabpanel();
						ruangUjianTabpanel.setParent(tabpanels);
						ruangUjianTabpanel.setHeight("890px");
						ruangUjianTabpanel.setWidth("100%");

						MyInclude iframe = new MyInclude(
								"/pages/psb/ujian_psb.zul?gelombangPendaftaranPsb=" + gelombangPendaftaranPsb.getId());
						iframe.setHeight("890px");
						iframe.setWidth("100%");
						iframe.setParent(ruangUjianTabpanel);

						final Tabpanel ruangRuangTabpanel = new ais.ui.util.MyTabpanel();
						ruangRuangTabpanel.setParent(tabpanels);
						ruangRuangTabpanel.setHeight("890px");
						ruangRuangTabpanel.setWidth("100%");

						tabRuangRuang.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (ruangRuangTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude("/pages/psb/ruang_psb.zul?gelombangPendaftaranPsb="
											+ gelombangPendaftaranPsb.getId());
									iframe.setHeight("890px");
									iframe.setWidth("100%");
									iframe.setParent(ruangRuangTabpanel);

								}
							}
						});

						final Tabpanel ruangJadwalTabpanel = new ais.ui.util.MyTabpanel();
						ruangJadwalTabpanel.setParent(tabpanels);
						ruangJadwalTabpanel.setHeight("890px");
						ruangJadwalTabpanel.setWidth("100%");

						tabJadwal.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (ruangJadwalTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/psb/jadwal_ujian_psb.zul?gelombangPendaftaranPsb="
													+ gelombangPendaftaranPsb.getId());
									iframe.setHeight("890px");
									iframe.setWidth("100%");
									iframe.setParent(ruangJadwalTabpanel);

								}
							}
						});

						final Tabpanel ruangJadwalPertemuanTabpanel = new ais.ui.util.MyTabpanel();
						ruangJadwalPertemuanTabpanel.setParent(tabpanels);
						ruangJadwalPertemuanTabpanel.setHeight("890px");
						ruangJadwalPertemuanTabpanel.setWidth("100%");

						tabJadwalPertemuan.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (ruangJadwalPertemuanTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/psb/jadwal_pertemuan_psb.zul?gelombangPendaftaranPsb="
													+ gelombangPendaftaranPsb.getId());
									iframe.setHeight("890px");
									iframe.setWidth("100%");
									iframe.setParent(ruangJadwalPertemuanTabpanel);

								}
							}
						});

						final Tabpanel pembagianKelasTabpanel = new ais.ui.util.MyTabpanel();
						pembagianKelasTabpanel.setParent(tabpanels);
						pembagianKelasTabpanel.setHeight("890px");
						pembagianKelasTabpanel.setWidth("100%");

						tabPembagianKelas.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (pembagianKelasTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude("/pages/psb/kelas_psb.zul?gelombangPendaftaranPsb="
											+ gelombangPendaftaranPsb.getId());
									iframe.setHeight("890px");
									iframe.setWidth("100%");
									iframe.setParent(pembagianKelasTabpanel);

								}
							}
						});

						final Tabpanel raporTabpanel = new ais.ui.util.MyTabpanel();
						raporTabpanel.setParent(tabpanels);
						raporTabpanel.setHeight("500px");
						raporTabpanel.setWidth("100%");
						tabRapor.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (raporTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/master/sekolah/gelombang_pendaftaran_psb_punya_matapelajaran.zul?gelombangPendaftaranPsb="
													+ gelombangPendaftaranPsb.getId());
									iframe.setHeight("1490px");
									iframe.setWidth("100%");

									iframe.setParent(raporTabpanel);

								}
							}
						});

						final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);
						tabpanelUtama.setHeight("900px");
						tabpanelUtama.setWidth("100%");
						tabParameterTambahan.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (tabpanelUtama.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/psb/parameter_tambahan_gelombang.zul?gelombangPendaftaranPsb="
													+ gelombangPendaftaranPsb.getId());
									iframe.setHeight("900px");
									iframe.setWidth("100%");
									iframe.setParent(tabpanelUtama);

								}
							}
						});

						final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
						jurusanTabpanel.setParent(tabpanels);
						jurusanTabpanel.setHeight("890px");
						jurusanTabpanel.setWidth("100%");

						tabParameter.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (jurusanTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/psb/gelombang_punya_parameter_verifikasi_calon_siswa.zul?gelombangPendaftaranPsb="
													+ gelombangPendaftaranPsb.getId());
									iframe.setHeight("890px");
									iframe.setWidth("100%");
									iframe.setParent(jurusanTabpanel);

								}
							}
						});

					}
				}
			});

			new Label((gelombangPendaftaranPsb.getTahunAjaran() == null
					|| gelombangPendaftaranPsb.getTahunAjaran().trim().isEmpty() ? "Semua"
							: gelombangPendaftaranPsb.getTahunAjaran())
					+ (" " + (gelombangPendaftaranPsb.getStatusAwalSiswa() == null ? ""
							: gelombangPendaftaranPsb.getStatusAwalSiswa().getNama())))
					.setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(GelombangPendaftaranPsb.class, gelombangPendaftaranPsb,
					gelombangPendaftaranPsb.getNama())).setParent(arg0);

			if (gelombangPendaftaranPsb.getKelompokGelombang() != null) {
				new Label(gelombangPendaftaranPsb.getKelompokGelombang().getNama()).setParent(a);
			}

			new Label(gelombangPendaftaranPsb.getMulai() == null ? ""
					: Common.dateFormat1.get().format(gelombangPendaftaranPsb.getMulai())).setParent(arg0);
			new Label(gelombangPendaftaranPsb.getSampai() == null ? ""
					: Common.dateFormat1.get().format(gelombangPendaftaranPsb.getSampai())).setParent(arg0);

			new Label(gelombangPendaftaranPsb.getKeterangan()).setParent(arg0);

			new Label(
					gelombangPendaftaranPsb.getSekolah() == null ? "" : gelombangPendaftaranPsb.getSekolah().getNama())
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(gelombangPendaftaranPsb.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					gelombangPendaftaranPsb.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(gelombangPendaftaranPsb);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, gelombangPendaftaranPsb, GelombangPendaftaranPsbAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new GelombangPendaftaranPsb());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		gelombangPendaftaranPsb = (GelombangPendaftaranPsb) obj;
		init(gelombangPendaftaranPsb);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(final GelombangPendaftaranPsb gelombangPendaftaranPsb) throws Exception {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
		addWindow.setTitle(gelombangPendaftaranPsb.getId() == null ? "Tambah Gelombang Pendaftaran" : "Ubah Gelombang Pendaftaran");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		row.appendChild(tahunAjaran = new Combobox());
		Common.generateTahunAjaran(tahunAjaran);
		Common.selectComboItem(tahunAjaran, gelombangPendaftaranPsb.getTahunAjaran());
		tahunAjaran.setWidth("90%");
		tahunAjaran.setReadonly(true);
		int countCalon = 0;
		if (!Common.getApakahAdmin() && gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getId() != null) {
			countCalon = ((Number) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("gelombangPendaftaranPsb", gelombangPendaftaranPsb)).uniqueResult())
					.intValue();
		}

		tahunAjaran.setDisabled(countCalon > 0);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Gelombang Pendaftaran *"));
		row.appendChild(nama = new Textbox(gelombangPendaftaranPsb.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelompok Gelombang"));
		row.appendChild(kelompokGelombang = new Combobox());
		Common.insertComboDanSemua(kelompokGelombang, new String[] { "nama" }, "kode", KelompokGelombang.class,
				"Tanpa Kelompok", Restrictions.eq("aktif", true));
		Common.selectComboItem(kelompokGelombang, gelombangPendaftaranPsb.getKelompokGelombang());
		kelompokGelombang.setWidth("90%");
		kelompokGelombang.setReadonly(true);

		kop = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("KOP Formulir Pendaftaran "));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, gelombangPendaftaranPsb.getId(), LampiranLain.KOP_GELOMBANG_PSB,
				"KOP", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kop = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		bg_ppdb = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Background Gelombang (JPG) "));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, gelombangPendaftaranPsb.getId(), LampiranLain.BG_PPDB_GELOMBANG,
				"Background", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						bg_ppdb = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pendaftaran *"));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(mulai = new MyDatebox(gelombangPendaftaranPsb.getMulai()));
		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig(" s.d ")));
		hbox.appendChild(sampai = new MyDatebox(gelombangPendaftaranPsb.getSampai()));

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan *"));
		row.appendChild(yayasan);
		Common.selectComboItem(yayasan, gelombangPendaftaranPsb.getYayasan());
		yayasan.setWidth("90%");
		yayasan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah *"));
		row.appendChild(sekolah);
		Common.pilihSekolah(sekolah, gelombangPendaftaranPsb.getSekolah());
		sekolah.setWidth("90%");
		sekolah.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus untuk penjurusan"));
		row.appendChild(penjurusanSekolah = new Combobox());
		penjurusanSekolah.setWidth("90%");
		penjurusanSekolah.setReadonly(true);

		final EventListener eventListenerPenjurusan = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Sekolah s = (sekolah.getSelectedItem() == null ? null : (Sekolah) sekolah.getSelectedItem().getValue());
				System.out.println("s => " + s);

				if (s != null && s.getPenjurusanBolehDipilihSaatPsb()) {
					HibernateUtil.currentSession().refresh(s);
					Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
					for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
						if (o.getAktif() && o.getTampilkanDiPpdb()) {
							Comboitem comboitem = new Comboitem();
							comboitem.setLabel(o.getNama());
							comboitem.setDescription(o.getKeterangan());
							comboitem.setValue(o);
							penjurusanSekolah.appendChild(comboitem);
						}
					}

					Comboitem comboitem = new Comboitem();
					comboitem.setLabel("Semua");
					comboitem.setDescription("Semua Penjurusan");
					comboitem.setValue(null);
					penjurusanSekolah.appendChild(comboitem);

					penjurusanSekolah.getParent().setVisible(!selectedPenjurusanSekolah.isEmpty());
					Common.selectComboItem(penjurusanSekolah, gelombangPendaftaranPsb.getPenjurusanSekolah());
				} else {
					penjurusanSekolah.getParent().setVisible(false);
				}
			}
		};

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal *"));
		row.appendChild(statusAwalSiswa = new Combobox());
		Common.insertCombo(statusAwalSiswa, "nama", "kode", StatusAwalSiswa.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(statusAwalSiswa, gelombangPendaftaranPsb.getStatusAwalSiswa());
		statusAwalSiswa.setWidth("90%");
		statusAwalSiswa.setReadonly(true);

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(siswaPindahanBolehMendaftar = new MyCheckboxConfig("Siswa Pindahan Boleh Mendaftar"));
		siswaPindahanBolehMendaftar.setChecked(gelombangPendaftaranPsb.getSiswaPindahanBolehMendaftar());
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(sesuaiKelas = new MyCheckboxConfig(
				"Saat pertama kali daftar, calon siswa wajib membayar sesuai kelas / kelas les yang dipilih"));
		sesuaiKelas.setChecked(gelombangPendaftaranPsb.getSesuaiKelas());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(langsungDapatNisSaatDaftar = new MyCheckboxConfig(
				"Saat pertama kali daftar, calon siswa wajib otomatis diteima dan mendapatkan NIS"));
		langsungDapatNisSaatDaftar.setChecked(gelombangPendaftaranPsb.getLangsungDapatNisSaatDaftar());

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilkanQrCodeMahasiswaSetelahDapatNim = new MyCheckboxConfig(
				"Tampilkan QR-Code Siswa Setelah mendapat NIS"));
		tampilkanQrCodeMahasiswaSetelahDapatNim
				.setChecked(gelombangPendaftaranPsb.getTampilkanQrCodeMahasiswaSetelahDapatNim());
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saat pertama kali daftar, calon siswa wajib membayar"));
		row.appendChild(jenisBiayaSekolah = new Combobox());
		jenisBiayaSekolah.setWidth("90%");
		jenisBiayaSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(otomatisLoginSetelahDaftar = new MyCheckboxConfig(
				"Saat pertama kali daftar, otomatis login letelah calon siswa menyelesaikan pendaftaran"));
		otomatisLoginSetelahDaftar.setChecked(gelombangPendaftaranPsb.getOtomatisLoginSetelahDaftar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(munculkanTagihanSetelahDaftar = new MyCheckboxConfig(
				"Saat pertama kali daftar, otomatis munculkan tagihan setelah daftar"));
		munculkanTagihanSetelahDaftar.setChecked(gelombangPendaftaranPsb.getMunculkanTagihanSetelahDaftar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saat terverifikasi, calon siswa wajib membayar"));
		row.appendChild(jenisBiayaSekolahTerverifikasi = new Combobox());
		jenisBiayaSekolahTerverifikasi.setWidth("90%");
		jenisBiayaSekolahTerverifikasi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(sesuaiKelasSaatDiterima = new MyCheckboxConfig(
				"Saat dinyatakan diterima, calon siswa wajib membayar sesuai kelas / kelas les yang dipilih"));
		sesuaiKelasSaatDiterima.setChecked(gelombangPendaftaranPsb.getSesuaiKelasSaatDiterima());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(otomatisDiterimaKetikaSudahBayarReg = new MyCheckboxConfig(
				"Calon siswa otomatis dinyatakan diterima ketika membayar biaya pendaftaran"));
		otomatisDiterimaKetikaSudahBayarReg
				.setChecked(gelombangPendaftaranPsb.getOtomatisDiterimaKetikaSudahBayarReg());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(otomatisDapatNisKetikaSudahBayarReg = new MyCheckboxConfig(
				"Calon siswa otomatis mendapatkan NIS dan diterima ketika membayar biaya pendaftaran"));
		otomatisDapatNisKetikaSudahBayarReg
				.setChecked(gelombangPendaftaranPsb.getOtomatisDapatNisKetikaSudahBayarReg());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Saat dinyatakan diterima, siswa wajib membayar"));
		row.appendChild(jenisBiayaSekolahLulus = new Combobox());
		jenisBiayaSekolahLulus.setWidth("90%");
		jenisBiayaSekolahLulus.setReadonly(true);

		EventListener eventListenerKelas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jenisBiayaSekolah.getParent().setVisible(!sesuaiKelas.isChecked());
				jenisBiayaSekolahTerverifikasi.getParent().setVisible(!sesuaiKelas.isChecked());
				jenisBiayaSekolahLulus.getParent().setVisible(!sesuaiKelasSaatDiterima.isChecked());
			}
		};

		sesuaiKelas.addEventListener("onClick", eventListenerKelas);
		sesuaiKelasSaatDiterima.addEventListener("onClick", eventListenerKelas);
		eventListenerKelas.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(hanyaUntukAnakPegawai = new MyCheckboxConfig("Hanya Untuk Anak Pegawai"));
		hanyaUntukAnakPegawai.setChecked(gelombangPendaftaranPsb.getHanyaUntukAnakPegawai());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilAlur = new MyCheckboxConfig("Tampilkan alur pendaftaran"));
		tampilAlur.setChecked(gelombangPendaftaranPsb.getTampilAlur());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilPembayaranViaPaymentGateway = new MyCheckboxConfig("Tampilkan pembayaran online"));
		tampilPembayaranViaPaymentGateway.setChecked(gelombangPendaftaranPsb.getTampilPembayaranViaPaymentGateway());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilLengkapiBerkas = new MyCheckboxConfig("Tampil Lengkapi Berkas"));
		tampilLengkapiBerkas.setChecked(gelombangPendaftaranPsb.getTampilLengkapiBerkas());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilInformasiKelulusan = new MyCheckboxConfig("Tampil Informasi Kelulusan"));
		tampilInformasiKelulusan.setChecked(gelombangPendaftaranPsb.getTampilInformasiKelulusan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilUjian = new MyCheckboxConfig("Tampil Ujian"));
		tampilUjian.setChecked(gelombangPendaftaranPsb.getTampilUjian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilCetakNoReg = new MyCheckboxConfig("Tampil Cetak No. Reg"));
		tampilCetakNoReg.setChecked(gelombangPendaftaranPsb.getTampilCetakNoReg());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilCetakBiodata = new MyCheckboxConfig("Tampil Cetak Biodata"));
		tampilCetakBiodata.setChecked(gelombangPendaftaranPsb.getTampilCetakBiodata());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilCetakKartuUjian = new MyCheckboxConfig("Tampil Cetak Kartu Ujian"));
		tampilCetakKartuUjian.setChecked(gelombangPendaftaranPsb.getTampilCetakKartuUjian());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(cetakKartuUjianHarusVerifikasiBerkas = new MyCheckboxConfig(
				"Cetak Kartu Ujian Harus Verifikasi Berkas"));
		cetakKartuUjianHarusVerifikasiBerkas
				.setChecked(gelombangPendaftaranPsb.getCetakKartuUjianHarusVerifikasiBerkas());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilKeteranganDiterima = new MyCheckboxConfig("Tampil Keterangan Diterima"));
		tampilKeteranganDiterima.setChecked(gelombangPendaftaranPsb.getTampilKeteranganDiterima());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilLogout = new MyCheckboxConfig("Tampil Logout"));
		tampilLogout.setChecked(gelombangPendaftaranPsb.getTampilLogout());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				tampilFormLampiranDiHalamanUtama = new MyCheckboxConfig("Tampil Form Lampiran Di Halaman Utama"));
		tampilFormLampiranDiHalamanUtama.setChecked(gelombangPendaftaranPsb.getTampilFormLampiranDiHalamanUtama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				tampilFormTambahanDiHalamanUtama = new MyCheckboxConfig("Tampil Form Tambahan Di Halaman Utama"));
		tampilFormTambahanDiHalamanUtama.setChecked(gelombangPendaftaranPsb.getTampilFormTambahanDiHalamanUtama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(otomatisDapatNisKetikaSudahBayarDaftarUlang = new MyCheckboxConfig(
				"Calon siswa otomatis mendapatkan NIS ketika membayar daftar ulang"));
		otomatisDapatNisKetikaSudahBayarDaftarUlang
				.setChecked(gelombangPendaftaranPsb.getOtomatisDapatNisKetikaSudahBayarDaftarUlang());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi yang ditampilkan ke calon siswa"));
		row.appendChild(informasi = new Textbox(gelombangPendaftaranPsb.getInformasi()));
		informasi.setWidth("90%");
		informasi.setRows(3);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File / lampiran informasi yang ditampilkan ke calon siswa"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, gelombangPendaftaranPsb.getId(), "INFO_PPDB", "Informasi PPDB",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file lampiran informasi yang ditampilkan ke calon siswa lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				hanyapaket = new MyCheckboxConfig("Gelombang pendaftaran ini hanya berlaku untuk paket tertentu"));
		row.setParent(rows);

		selectedPaketPsbPunyaGelombangPendaftaranPsb = new HashMap<Long, PaketPsbPunyaGelombangPendaftaranPsb>();

		rowJp = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowJp, "2");
		rowJp.setParent(rows);

		final EventListener ubahJenisPenialain = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());
				Yayasan y = (Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue());

				List<PaketPsb> pakets = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(PaketPsb.class)
								.add(Restrictions.or(Restrictions.isNull("yayasan"), Restrictions.eq("yayasan", y)))
								.add(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)))
								.addOrder(Order.asc("nama"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						PaketPsb.class);

				if (gelombangPendaftaranPsb.getId() != null) {
					HibernateUtil.currentSession().refresh(gelombangPendaftaranPsb);
				}

				if (gelombangPendaftaranPsb.getId() != null) {
					Session session = HibernateUtil.currentSession();
					List<PaketPsbPunyaGelombangPendaftaranPsb> paketPsbPunyaGelombangPendaftaranPsbs = ConstantValues
							.simpleList(
									session.createCriteria(PaketPsbPunyaGelombangPendaftaranPsb.class)
											.add(Restrictions.eq("gelombangPendaftaranPsb", gelombangPendaftaranPsb)),
									PaketPsbPunyaGelombangPendaftaranPsb.class);

					selectedPaketPsbPunyaGelombangPendaftaranPsb.clear();
					for (PaketPsbPunyaGelombangPendaftaranPsb paketPsbPunyaGelombangPendaftaranPsb : paketPsbPunyaGelombangPendaftaranPsbs) {
						if (!selectedPaketPsbPunyaGelombangPendaftaranPsb
								.containsKey(paketPsbPunyaGelombangPendaftaranPsb.getPaketPsb().getId())) {
							selectedPaketPsbPunyaGelombangPendaftaranPsb.put(
									paketPsbPunyaGelombangPendaftaranPsb.getPaketPsb().getId(),
									paketPsbPunyaGelombangPendaftaranPsb);
						}
					}

				} else {
					selectedPaketPsbPunyaGelombangPendaftaranPsb.clear();
				}

				hanyapaket.setChecked(!selectedPaketPsbPunyaGelombangPendaftaranPsb.isEmpty());
				hanyapaket.setDisabled(!selectedPaketPsbPunyaGelombangPendaftaranPsb.isEmpty());

				rowJp.setVisible(hanyapaket.isChecked());
				Common.clear(rowJp);
				MyGrid vboxSkala = new MyGrid();
				vboxSkala.setParent(rowJp);

				Columns columns = new Columns();
				columns.setParent(vboxSkala);

				MyColumnConfig column = new MyColumnConfig("Pilih Paket");
				column.setParent(columns);

				Rows rowsSkala = new Rows();
				rowsSkala.setParent(vboxSkala);

				KategoriItemPenilaianSiswa kategoriItemPenilaianSiswa = new KategoriItemPenilaianSiswa();
				kategoriItemPenilaianSiswa.setId(-1L);

				for (final PaketPsb paketPsb : pakets) {

					MyFormRow rowSkala = new MyFormRow();
					rowSkala.setStyle("border:0px;background: transparent;");
					rowSkala.setParent(rowsSkala);

					PaketPsbPunyaGelombangPendaftaranPsb paketPsbPunyaGelombangPendaftaranPsbTemp = selectedPaketPsbPunyaGelombangPendaftaranPsb
							.get(paketPsb.getId());
					if (paketPsbPunyaGelombangPendaftaranPsbTemp == null) {
						paketPsbPunyaGelombangPendaftaranPsbTemp = new PaketPsbPunyaGelombangPendaftaranPsb();
					}
					paketPsbPunyaGelombangPendaftaranPsbTemp.setPaketPsb(paketPsb);
					final PaketPsbPunyaGelombangPendaftaranPsb paketPsbPunyaGelombangPendaftaranPsb = paketPsbPunyaGelombangPendaftaranPsbTemp;

					final Checkbox checkbox = new Checkbox(paketPsb.getNama());
					checkbox.setAttribute("paket", paketPsb);
					checkbox.setParent(rowSkala);
					checkbox.setChecked(selectedPaketPsbPunyaGelombangPendaftaranPsb.containsKey(paketPsb.getId()));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							if (checkbox.isChecked()) {
								selectedPaketPsbPunyaGelombangPendaftaranPsb.put(paketPsb.getId(),
										paketPsbPunyaGelombangPendaftaranPsb);
							} else {
								selectedPaketPsbPunyaGelombangPendaftaranPsb.remove(paketPsb.getId());
							}

						}
					});

				}
			}
		};

		hanyapaket.addEventListener("onClick", ubahJenisPenialain);
		hanyapaket.setChecked(!selectedPaketPsbPunyaGelombangPendaftaranPsb.isEmpty());
		rowJp.setVisible(hanyapaket.isChecked());

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue());

				Common.insertComboDanSemua(jenisBiayaSekolah, new String[] { "nama", "sekolah" }, "periode",
						JenisBiayaSekolah.class, "== Tidak Ada Kewajiban Membayar Untuk Login Calon Siswa ==",
						Restrictions.and(Restrictions.eq("gunakanCalonSiswa", true), Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.selectComboItem(true, jenisBiayaSekolah, gelombangPendaftaranPsb.getJenisBiayaSekolah());

				Common.insertComboDanSemua(jenisBiayaSekolahTerverifikasi, new String[] { "nama", "sekolah" },
						"periode", JenisBiayaSekolah.class,
						"== Tidak Ada Kewajiban Membayar Untuk Calon Siswa Terverifikasi ==",
						Restrictions.and(Restrictions.eq("gunakanCalonSiswa", true), Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

				Common.selectComboItem(true, jenisBiayaSekolahTerverifikasi,
						gelombangPendaftaranPsb.getJenisBiayaSekolahTerverifikasi());

				Common.insertComboDanSemua(jenisBiayaSekolahLulus, new String[] { "nama", "sekolah" }, "periode",
						JenisBiayaSekolah.class, "== Tidak Ada Kewajiban Membayar Untuk Daftar Ulang Calon Siswa ==",
						Restrictions.and(Restrictions.eq("gunakanCalonSiswa", true), Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));
				Common.selectComboItem(true, jenisBiayaSekolahLulus,
						gelombangPendaftaranPsb.getJenisBiayaSekolahLulus());

				eventListenerPenjurusan.onEvent(arg0);

				ubahJenisPenialain.onEvent(arg0);
			}
		};

		sekolah.addEventListener("onChange", eventListener);
		yayasan.addEventListener("onChange", ubahJenisPenialain);
		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kuota Siswa Diterima"));
		row.appendChild(kuotaDiterima = new Intbox(gelombangPendaftaranPsb.getKuotaDiterima()));

		initKelengkapanBerkas(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Verifikasi Rapor"));
		row.appendChild(kelasVerifikasiRapor = new Textbox(gelombangPendaftaranPsb.getKelasVerifikasiRapor()));
		kelasVerifikasiRapor.setWidth("90%");
		kelasVerifikasiRapor.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(harusSebagaiAlumni = new MyCheckboxConfig("Calon siswa harus sebagai alumni"));
		harusSebagaiAlumni.setChecked(gelombangPendaftaranPsb.getHarusSebagaiAlumni());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alumni dari *"));
		row.appendChild(alumniDari = new Combobox());
		Common.insertCombo(alumniDari, "nama", "alamat", Sekolah.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(alumniDari, gelombangPendaftaranPsb.getAlumniDari());
		alumniDari.setWidth("90%");
		alumniDari.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas dari alumni"));
		row.appendChild(kelasDariAlumni = new Textbox(gelombangPendaftaranPsb.getKelasDariAlumni()));
		kelasDariAlumni.setWidth("90%");

		final Row tingkatKeteraganDariAlumni = Common.initKeterangan(rows,
				"Jika tingkat lebih dari satu, pisahkan dengan tanda koma, contoh kelas dari alumni : 5,6.  Jika dikosongkan akan mengambil dari semua kelas.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat dari alumni"));
		row.appendChild(tingkatDariAlumni = new Textbox(gelombangPendaftaranPsb.getTingkatDariAlumni()));
		tingkatDariAlumni.setWidth("90%");

		final Row tingkatKeteragan = Common.initKeterangan(rows,
				"Jika tingkat lebih dari satu, pisahkan dengan tanda koma, contoh tingkat dari alumni : A,B,C  Jika dikosongkan akan mengambil dari semua tingkat.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun pelajaran ketika lulus sebagai alumni *"));
		row.appendChild(tahunAkademikAlumni = new Textbox(gelombangPendaftaranPsb.getTahunAkademikAlumni()));
		tahunAkademikAlumni.setWidth("90%");

		final Row tahunAkademikAlumniKeteragan = Common.initKeterangan(rows,
				"Jika tahun pelajaran lebih dari satu, pisahkan dengan tanda koma, contoh tingkat dari alumni : 2024/2025,2025/2026,2026/2027. Jika dikosongkan akan mengambil dari semua tahun pelajaran.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				terdapatVerifikasiDenganNikAlumni = new MyCheckboxConfig("Terdapat verifikasi dengan NIK Alumni"));
		terdapatVerifikasiDenganNikAlumni.setChecked(gelombangPendaftaranPsb.getTerdapatVerifikasiDenganNikAlumni());

		EventListener harusSebagaiAlumniEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				terdapatVerifikasiDenganNikAlumni.getParent().setVisible(harusSebagaiAlumni.isChecked());
				tingkatDariAlumni.getParent().setVisible(harusSebagaiAlumni.isChecked());
				kelasDariAlumni.getParent().setVisible(harusSebagaiAlumni.isChecked());
				alumniDari.getParent().setVisible(harusSebagaiAlumni.isChecked());
				tahunAkademikAlumni.getParent().setVisible(harusSebagaiAlumni.isChecked());
				tingkatKeteragan.setVisible(harusSebagaiAlumni.isChecked());
				tahunAkademikAlumniKeteragan.setVisible(harusSebagaiAlumni.isChecked());
				tingkatKeteraganDariAlumni.setVisible(harusSebagaiAlumni.isChecked());
			}
		};

		harusSebagaiAlumni.addEventListener("onClick", harusSebagaiAlumniEventListener);
		harusSebagaiAlumniEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				harusSebagaiSaudara = new MyCheckboxConfig("Calon siswa harus punya saudara yang sekolah di sini"));
		harusSebagaiSaudara.setChecked(gelombangPendaftaranPsb.getHarusSebagaiSaudara());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(
				terdapatVerifikasiDenganNikSibling = new MyCheckboxConfig("Terdapat verifikasi dengan NIK Alumni"));
		terdapatVerifikasiDenganNikSibling.setChecked(gelombangPendaftaranPsb.getTerdapatVerifikasiDenganNikAlumni());

		EventListener harusSebagaiSaudaraEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				terdapatVerifikasiDenganNikSibling.getParent().setVisible(harusSebagaiSaudara.isChecked());
			}
		};

		harusSebagaiSaudara.addEventListener("onClick", harusSebagaiSaudaraEventListener);
		harusSebagaiSaudaraEventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Informasi Singkat"));
		row.appendChild(keterangan = new Textbox(gelombangPendaftaranPsb.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Form Untuk Pendaftaran Di Awal"));
		classUntukPendaftaran = new Combobox();

		classUntukMelengkapiBerkas = new Combobox();

		row.appendChild(classUntukPendaftaran);
		Comboitem comboitemData = new Comboitem("Menggunakan Form PPDB Default");
		comboitemData.setValue(null);
		classUntukPendaftaran.appendChild(comboitemData);

		comboitemData = new Comboitem("Menggunakan Form Berkas PPDB Default");
		comboitemData.setValue(null);
		classUntukMelengkapiBerkas.appendChild(comboitemData);

		if (ConstantValues.treeMapFormPpdb.isEmpty()) {
			InitDataHelper.reInitClass();
		}

		for (String c : ConstantValues.treeMapFormPpdb.keySet()) {
			comboitemData = new Comboitem(ConstantValues.treeMapFormPpdb.get(c));
			comboitemData.setDescription(c);
			comboitemData.setValue(c);
			classUntukPendaftaran.appendChild(comboitemData);

			comboitemData = new Comboitem(ConstantValues.treeMapFormPpdb.get(c));
			comboitemData.setDescription(c);
			comboitemData.setValue(c);
			classUntukMelengkapiBerkas.appendChild(comboitemData);
		}

		Common.selectComboItem(classUntukPendaftaran, gelombangPendaftaranPsb.getClassUntukPendaftaran());

		classUntukPendaftaran.setReadonly(true);
		classUntukPendaftaran.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Form Untuk Melengkapi Berkas"));
		row.appendChild(classUntukMelengkapiBerkas);
		Common.selectComboItem(classUntukMelengkapiBerkas, gelombangPendaftaranPsb.getClassUntukMelengkapiBerkas());
		classUntukMelengkapiBerkas.setReadonly(true);
		classUntukMelengkapiBerkas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(dibatasiUmur = new MyCheckboxConfig("Dibatasi Umur"));
		if (gelombangPendaftaranPsb.getDibatasiUmur() == null) {
			gelombangPendaftaranPsb
					.setDibatasiUmur(Common.bolehKonfigurasi("umur_calon_mahasiswa_dibatasi", Konfigurasi.TIDAK_AKTIF));
		}
		dibatasiUmur.setChecked(gelombangPendaftaranPsb.getDibatasiUmur());

		int umur = 27;
		try {
			umur = Integer.parseInt(Common.getKonfigurasi("nilai_umur_calon_siswa_dibatasi", "27").getNilai().trim());
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		if (gelombangPendaftaranPsb.getUmurmaksimal() == null) {
			gelombangPendaftaranPsb.setUmurmaksimal(umur);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur Minimal"));
		row.appendChild(umurminimal = new Intbox(gelombangPendaftaranPsb.getUmurminimal()));
		umurminimal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur Maksimal"));
		row.appendChild(umurmaksimal = new Intbox(gelombangPendaftaranPsb.getUmurmaksimal()));
		umurmaksimal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Umur dihitung saat tanggal"));
		row.appendChild(umurDihitungTanggal = new MyDatebox(gelombangPendaftaranPsb.getUmurDihitungTanggal()));

		final Row aa = Common.initKeterangan(rows,
				"Kosongkan tanggal apabila umur dihitung saat melakukan pendaftaran");

		EventListener eventListenerUmur = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				umurminimal.getParent().setVisible(dibatasiUmur.isChecked());
				umurmaksimal.getParent().setVisible(dibatasiUmur.isChecked());
				umurDihitungTanggal.getParent().setVisible(dibatasiUmur.isChecked());
				aa.setVisible(dibatasiUmur.isChecked());
			}
		};

		eventListenerUmur.onEvent(null);
		dibatasiUmur.addEventListener("onClick", eventListenerUmur);

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

	@SuppressWarnings("deprecation")
	private void initKelengkapanBerkas(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Formulir Verifikasi Kelengkapan Berkas"));
		final MyCheckboxConfig formulirVerifikasi;
		row.appendChild(formulirVerifikasi = new MyCheckboxConfig());
		row.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Formulir Verifikasi Kelengkapan Berkas");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		final MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(subRow);

				Sekolah s = (Sekolah) (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null
						? null
						: sekolah.getSelectedItem().getValue());

				List<VerifikasiKelengkapanCalonSiswa> verifikasiKelengkapanCalonSiswas = ConstantValues
						.simpleList(
								HibernateUtil.currentSession().createCriteria(VerifikasiKelengkapanCalonSiswa.class)
										.add(Restrictions.eq("sekolah", s))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true))),
								VerifikasiKelengkapanCalonSiswa.class);

				if (GelombangPendaftaranPsbAction.this.gelombangPendaftaranPsb.getId() != null) {
					HibernateUtil.currentSession().refresh(GelombangPendaftaranPsbAction.this.gelombangPendaftaranPsb);
				}
				try {
					/*
					 * Jangan gunakan PersistentSet milik Hibernate langsung sebagai state checkbox.
					 * Refresh/load ulang entitas dapat mengembalikan isi koleksi ke snapshot DB dan
					 * membuang pilihan pengguna sebelum flush. Salin ke Set biasa seperti alur PMB.
					 */
					selectedVerifikasiKelengkapanCalonSiswa = new HashSet<VerifikasiKelengkapanCalonSiswa>();
					Set<VerifikasiKelengkapanCalonSiswa> tersimpan = GelombangPendaftaranPsbAction.this.gelombangPendaftaranPsb
							.getVerifikasiKelengkapanCalonSiswas();
					if (tersimpan != null) {
						selectedVerifikasiKelengkapanCalonSiswa.addAll(tersimpan);
					}

					subGrid.setVisible(!selectedVerifikasiKelengkapanCalonSiswa.isEmpty());
					formulirVerifikasi.setChecked(!selectedVerifikasiKelengkapanCalonSiswa.isEmpty());

				} catch (Exception e) {
					selectedVerifikasiKelengkapanCalonSiswa = new HashSet<VerifikasiKelengkapanCalonSiswa>();
					subGrid.setVisible(false);
					formulirVerifikasi.setChecked(false);
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
				for (final VerifikasiKelengkapanCalonSiswa verifikasiKelengkapanCalonSiswa : verifikasiKelengkapanCalonSiswas) {
					final Checkbox checkbox = new Checkbox(verifikasiKelengkapanCalonSiswa.getNama());
					checkbox.setParent(vboxSkala);
					boolean sudahDipilih = false;
					for (VerifikasiKelengkapanCalonSiswa pilihan : selectedVerifikasiKelengkapanCalonSiswa) {
						if (pilihan != null && pilihan.getId() != null
								&& pilihan.getId().equals(verifikasiKelengkapanCalonSiswa.getId())) {
							sudahDipilih = true;
							break;
						}
					}
					checkbox.setChecked(sudahDipilih);
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								selectedVerifikasiKelengkapanCalonSiswa.add(verifikasiKelengkapanCalonSiswa);
							} else {
								VerifikasiKelengkapanCalonSiswa pilihanDihapus = null;
								for (VerifikasiKelengkapanCalonSiswa pilihan : selectedVerifikasiKelengkapanCalonSiswa) {
									if (pilihan != null && pilihan.getId() != null
											&& pilihan.getId().equals(verifikasiKelengkapanCalonSiswa.getId())) {
										pilihanDihapus = pilihan;
										break;
									}
								}
								if (pilihanDihapus != null) {
									selectedVerifikasiKelengkapanCalonSiswa.remove(pilihanDihapus);
								}
							}
						}
					});
				}
			}
		};

		sekolah.addEventListener("onChange", eventListener);
		try {
			eventListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Gelombang harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Mulai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sampai.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Sampai harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Yayasan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Sekolah harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (harusSebagaiAlumni.isChecked() && alumniDari.getSelectedItem() == null) {
			MyMessageboxConfig.show("Alumni dari harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (gelombangPendaftaranPsb.getId() != null) {
			gelombangPendaftaranPsb = (GelombangPendaftaranPsb) session.load(GelombangPendaftaranPsb.class,
					gelombangPendaftaranPsb.getId());
		}
		gelombangPendaftaranPsb.setKelasDariAlumni(kelasDariAlumni.getValue());
		gelombangPendaftaranPsb
				.setTampilkanQrCodeMahasiswaSetelahDapatNim(tampilkanQrCodeMahasiswaSetelahDapatNim.isChecked());
		gelombangPendaftaranPsb.setLangsungDapatNisSaatDaftar(langsungDapatNisSaatDaftar.isChecked());
		gelombangPendaftaranPsb.setHarusSebagaiAlumni(harusSebagaiAlumni.isChecked());
		gelombangPendaftaranPsb.setSiswaPindahanBolehMendaftar(siswaPindahanBolehMendaftar.isChecked());
//		gelombangPendaftaranPsb.setHarusSebagaiAnakAlumni(harusSebagaiAnakAlumni.isChecked());
		gelombangPendaftaranPsb.setAlumniDari(
				(Sekolah) (alumniDari.getSelectedItem() == null ? null : alumniDari.getSelectedItem().getValue()));

		gelombangPendaftaranPsb.setNama(nama.getValue());
		gelombangPendaftaranPsb.setTahunAjaran((String) tahunAjaran.getSelectedItem().getValue());
		gelombangPendaftaranPsb.setMulai(mulai.getValue());
		gelombangPendaftaranPsb.setSampai(sampai.getValue());
		gelombangPendaftaranPsb.setSekolah((Sekolah) sekolah.getSelectedItem().getValue());
		gelombangPendaftaranPsb.setYayasan((Yayasan) yayasan.getSelectedItem().getValue());
		if (selectedVerifikasiKelengkapanCalonSiswa == null) {
			selectedVerifikasiKelengkapanCalonSiswa = new HashSet<VerifikasiKelengkapanCalonSiswa>();
		}
		gelombangPendaftaranPsb.setVerifikasiKelengkapanCalonSiswas(
				new HashSet<VerifikasiKelengkapanCalonSiswa>(selectedVerifikasiKelengkapanCalonSiswa));
		gelombangPendaftaranPsb.setKeterangan(keterangan.getValue());
		gelombangPendaftaranPsb.setInformasi(informasi.getValue());
		gelombangPendaftaranPsb.setKelasVerifikasiRapor(kelasVerifikasiRapor.getValue());

		gelombangPendaftaranPsb.setUmurDihitungTanggal(umurDihitungTanggal.getValue());

		gelombangPendaftaranPsb
				.setJenisBiayaSekolah((JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null ? null
						: jenisBiayaSekolah.getSelectedItem().getValue()));
		gelombangPendaftaranPsb
				.setJenisBiayaSekolahLulus((JenisBiayaSekolah) (jenisBiayaSekolahLulus.getSelectedItem() == null ? null
						: jenisBiayaSekolahLulus.getSelectedItem().getValue()));

		gelombangPendaftaranPsb.setJenisBiayaSekolahTerverifikasi(
				(JenisBiayaSekolah) (jenisBiayaSekolahTerverifikasi.getSelectedItem() == null ? null
						: jenisBiayaSekolahTerverifikasi.getSelectedItem().getValue()));

		gelombangPendaftaranPsb.setStatusAwalSiswa((StatusAwalSiswa) (statusAwalSiswa.getSelectedItem() == null ? null
				: statusAwalSiswa.getSelectedItem().getValue()));

		gelombangPendaftaranPsb
				.setKelompokGelombang((KelompokGelombang) (kelompokGelombang.getSelectedItem() == null ? null
						: kelompokGelombang.getSelectedItem().getValue()));

		gelombangPendaftaranPsb
				.setClassUntukMelengkapiBerkas((String) (classUntukMelengkapiBerkas.getSelectedItem() == null ? null
						: classUntukMelengkapiBerkas.getSelectedItem().getValue()));
		gelombangPendaftaranPsb
				.setClassUntukPendaftaran((String) (classUntukPendaftaran.getSelectedItem() == null ? null
						: classUntukPendaftaran.getSelectedItem().getValue()));

		gelombangPendaftaranPsb.setMunculkanTagihanSetelahDaftar(munculkanTagihanSetelahDaftar.isChecked());

		gelombangPendaftaranPsb.setSesuaiKelas(sesuaiKelas.isChecked());
		gelombangPendaftaranPsb.setSesuaiKelasSaatDiterima(sesuaiKelasSaatDiterima.isChecked());

		gelombangPendaftaranPsb.setOtomatisLoginSetelahDaftar(otomatisLoginSetelahDaftar.isChecked());

		gelombangPendaftaranPsb
				.setPenjurusanSekolah((PenjurusanSekolah) (penjurusanSekolah.getSelectedItem() == null ? null
						: penjurusanSekolah.getSelectedItem().getValue()));

		gelombangPendaftaranPsb.setOtomatisDiterimaKetikaSudahBayarReg(otomatisDiterimaKetikaSudahBayarReg.isChecked());
		gelombangPendaftaranPsb.setOtomatisDapatNisKetikaSudahBayarReg(otomatisDapatNisKetikaSudahBayarReg.isChecked());
		gelombangPendaftaranPsb.setOtomatisDapatNisKetikaSudahBayarDaftarUlang(
				otomatisDapatNisKetikaSudahBayarDaftarUlang.isChecked());

		gelombangPendaftaranPsb.setUmurminimal(umurminimal.getValue());
		gelombangPendaftaranPsb.setUmurmaksimal(umurmaksimal.getValue());
		gelombangPendaftaranPsb.setDibatasiUmur(dibatasiUmur.isChecked());
		gelombangPendaftaranPsb.setHanyaUntukAnakPegawai(hanyaUntukAnakPegawai.isChecked());

		gelombangPendaftaranPsb.setTampilAlur(tampilAlur.isChecked());
		gelombangPendaftaranPsb.setTampilPembayaranViaPaymentGateway(tampilPembayaranViaPaymentGateway.isChecked());
		gelombangPendaftaranPsb.setTampilLengkapiBerkas(tampilLengkapiBerkas.isChecked());
		gelombangPendaftaranPsb.setTampilInformasiKelulusan(tampilInformasiKelulusan.isChecked());
		gelombangPendaftaranPsb.setTampilUjian(tampilUjian.isChecked());
		gelombangPendaftaranPsb.setTampilCetakNoReg(tampilCetakNoReg.isChecked());
		gelombangPendaftaranPsb.setTampilCetakBiodata(tampilCetakBiodata.isChecked());
		gelombangPendaftaranPsb.setTampilCetakKartuUjian(tampilCetakKartuUjian.isChecked());
		gelombangPendaftaranPsb.setTampilKeteranganDiterima(tampilKeteranganDiterima.isChecked());
		gelombangPendaftaranPsb.setTampilLogout(tampilLogout.isChecked());

		gelombangPendaftaranPsb.setTampilFormLampiranDiHalamanUtama(tampilFormLampiranDiHalamanUtama.isChecked());
		gelombangPendaftaranPsb.setTampilFormTambahanDiHalamanUtama(tampilFormTambahanDiHalamanUtama.isChecked());

		gelombangPendaftaranPsb
				.setCetakKartuUjianHarusVerifikasiBerkas(cetakKartuUjianHarusVerifikasiBerkas.isChecked());
		gelombangPendaftaranPsb.setTingkatDariAlumni(tingkatDariAlumni.getValue().trim());
		gelombangPendaftaranPsb.setTerdapatVerifikasiDenganNikAlumni(terdapatVerifikasiDenganNikAlumni.isChecked());

		gelombangPendaftaranPsb.setTahunAkademikAlumni(tahunAkademikAlumni.getValue().trim());
		gelombangPendaftaranPsb.setKuotaDiterima(kuotaDiterima.getValue());

		gelombangPendaftaranPsb.setHarusSebagaiSaudara(harusSebagaiSaudara.isChecked());
		gelombangPendaftaranPsb.setTerdapatVerifikasiDenganNikSibling(terdapatVerifikasiDenganNikSibling.isChecked());

		Common.refreshSaveOrUpdate(session, gelombangPendaftaranPsb);
		// Pastikan perubahan join-table tersimpan sebelum grid di-refresh dan dialog ditutup.
		session.flush();

		try {
			session.createSQLQuery(
					"delete from sekolah.paket_psb_punya_gelombang_pendaftaran_psb where gelombang_pendaftaran_psb="
							+ gelombangPendaftaranPsb.getId())
					.executeUpdate();

			if (selectedPaketPsbPunyaGelombangPendaftaranPsb != null) {
				for (PaketPsbPunyaGelombangPendaftaranPsb paketPunyaGelombangPendaftaran : selectedPaketPsbPunyaGelombangPendaftaranPsb
						.values()) {
					paketPunyaGelombangPendaftaran.setGelombangPendaftaranPsb(gelombangPendaftaranPsb);
					session.save(paketPunyaGelombangPendaftaran);
					session.flush();
				}
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (kop != null && kop.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(kop);
				kop.setRef(gelombangPendaftaranPsb.getId());

				session.getTransaction().begin();
				session.update(kop);
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
				bg_ppdb.setRef(gelombangPendaftaranPsb.getId());

				session.getTransaction().begin();
				session.update(bg_ppdb);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(gelombangPendaftaranPsb.getId());

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
		Criteria criteria = session.createCriteria(GelombangPendaftaranPsb.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchta.getSelectedItem() == null || searchta.getSelectedItem().getValue() == null
						|| searchta.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchta.getSelectedItem().getValue()))

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

		List<GelombangPendaftaranPsb> gelombangPendaftaranPsb = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(gelombangPendaftaranPsb);
		grid.setRowRenderer(new GelombangPendaftaranPsbRenderer());
		grid.setModelCheckMobile(strset);

	}

}
