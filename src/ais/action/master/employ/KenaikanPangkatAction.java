package ais.action.master.employ;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.employ.helper.AmbilDataGajiPokokBanbox;
import ais.action.master.employ.helper.AmbilDataInsentifBanbox;
import ais.action.master.helper.AmbilDataGolonganBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.KenaikanPangkatDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jabatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Peraturan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyCombobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Controller/action ZK untuk kenaikan pangkat. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataPegawaiBanbox ambilDataPegawaiBanbox}, {@code
 * AmbilDataPegawaiBanbox searchpegawai}, {@code Combobox searchstatus}, {@code Combobox searchJenisPerubahan},
 * {@code Textbox namaPejabat}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()},
 * {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code reloadUser()},
 * {@code ambil()}, {@code ambilClass()}); mutasi data ({@code onSave()}, {@code setPersetujuan()});
 * pelaporan/ekspor ({@code cetakData()}); operasi domain lain ({@code onAdd()}, {@code
 * autoIsiGajiPokokDariMasaKerja()}, {@code form()}, {@code istilah()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
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
public class KenaikanPangkatAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox;
	private AmbilDataPegawaiBanbox searchpegawai;
	private Combobox searchstatus;
	private Combobox searchJenisPerubahan;

	private Textbox namaPejabat;
	private Textbox nomorSuratkeputusan;
	private MyDatebox tanggalSuratkeputusan;

	private MyDatebox mulai;
	private MyDatebox sampai;

	private MyDatebox tanggalSuratUsul;
	private Textbox noSuratUsul;
	private Textbox keterangan;
	private Combobox peraturan;

	private MyCheckboxConfig kenaikanJabatan;
	private Combobox jenis;
	private Combobox jabatan;
	private Combobox jabatanFungsional;
	private Combobox jabatanStruktural;
	private MyCheckboxConfig menjabat;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private boolean edit = false;
	private boolean delete = false;

	private DisposisiSop disposisiSop = null;
	private KenaikanPangkat kenaikanPangkat;
	private MyToolbarbuttonConfig add;

	private Pegawai pegawai;
	private MyCheckboxConfig status;
	private AmbilDataGolonganBanbox golongan;
	protected LampiranLain lainMahasiswa;
	private AmbilDataGajiPokokBanbox gajiPokok;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataInsentifBanbox insentif;
	private boolean persetujuan = false;
	private MyCheckboxConfig nonAktifkanJabatanSebelumnya;
	private MyCombobox jenisPerubahan;
	private JSONObject jsonDataPengguna;
	private MyCheckboxConfig nonAktifkanPengguna;
	private MyCheckboxConfig aktifkanPengguna;
	private Row rownonAktifkanPengguna;
	private Row rowaktifkanPengguna;
	private Row rowmenjabat;
	private MyCheckboxConfig terdapatKenaikanGajiBerkala;
	private Row rowKenaikan;
	private Decimalbox kenaikanBerkalaBulan;
	private MyCheckboxConfig gajiLangsungDitentukanDisini;
	private MyCheckboxConfig gajiPokokOtomatisMasaKerja;
	/** Gaji pokok manual sebelum ditimpa nilai otomatis — untuk dikembalikan saat cekbox dilepas. */
	private GajiPokok gajiPokokAsli;
	private MyDoublebox nilaiGaji;
	private Row rownilaiGaji;
	private boolean menggunakanLangsungGajiPokok;
	private Row rowterdapatKenaikanGajiBerkala;
	private Row rownilaiInsentif;
	private MyDoublebox nilaiInsentif;

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

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		searchpegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		if (session.getAttribute("pegawai") == null) {
			pegawai = (Pegawai) session.getAttribute("pegawai");
		}

		if (this.pegawai != null) {
			searchpegawai.setAttribute("pegawai", pegawai);
			searchpegawai.setValue(pegawai.toString());
			searchpegawai.setDisabled(true);
		}

		MyComboitemConfig comboitem = new MyComboitemConfig("Disetujui");
		if (comboitem != null) { comboitem.setValue(true); }
		searchstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Belum Disetujui");
		if (comboitem != null) { comboitem.setValue(false); }
		searchstatus.appendChild(comboitem);
		searchstatus.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		comboitem = new MyComboitemConfig(KenaikanPangkat.UBAH_JABATAN_DAN_GOLONGAN);
		if (comboitem != null) { comboitem.setValue(KenaikanPangkat.UBAH_JABATAN_DAN_GOLONGAN); }
		searchJenisPerubahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig(KenaikanPangkat.UBAH_JABATAN);
		if (comboitem != null) { comboitem.setValue(KenaikanPangkat.UBAH_JABATAN); }
		searchJenisPerubahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig(KenaikanPangkat.UBAH_GOLONGAN);
		if (comboitem != null) { comboitem.setValue(KenaikanPangkat.UBAH_GOLONGAN); }
		searchJenisPerubahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig(KenaikanPangkat.UBAH_PENGUNDURAN_DIRI);
		if (comboitem != null) { comboitem.setValue(KenaikanPangkat.UBAH_PENGUNDURAN_DIRI); }
		searchJenisPerubahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchJenisPerubahan.appendChild(comboitem);
		if (searchJenisPerubahan != null) { searchJenisPerubahan.setSelectedItem(comboitem); }
		if (searchJenisPerubahan != null) { searchJenisPerubahan.setReadonly(true); }
		searchJenisPerubahan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "pegawai", "jenisKenaikanPangkat", "noSuratUsul", "tanggalSuratUsul",
				"golongan", "gajiPokok", "insentif", "namaPejabat", "nomorSuratkeputusan", "tanggalSuratkeputusan",
				"mulai", "sampai", "peraturan", "tmt", "kenaikanJabatan", "jenis", "jabatanFungsional",
				"jabatanStruktural", "jabatan", "menjabat", "status", "kenaikanPangkatGolongan",
				"kenaikanPangkatFungsional", "nonAktifkanJabatanSebelumnya", "jenisPerubahan",
				"gajiLangsungDitentukanDisini", "nilaiGaji", "nilaiInsentif" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KenaikanPangkat.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KenaikanPangkatAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KenaikanPangkatAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KenaikanPangkatAction
	 */
	class KenaikanPangkatRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KenaikanPangkat kenaikanPangkat = (KenaikanPangkat) arg1;

			if (kenaikanPangkat.getMenjabat()) {
				arg0.setStyle("background-color: rgba(144,238,144,0.4);");
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(KenaikanPangkat.class, kenaikanPangkat,
					kenaikanPangkat.getPegawai().getNama())).setParent(arg0);
			Hbox hbox = new Hbox();
			hbox.setParent(a);
			LampiranLain.createDownloadUploadFileLain(hbox, kenaikanPangkat.getId(), KenaikanPangkat.class.getName(),
					"Dokumen", false, null, null, false, false, false, false);

			String jabatan = "";
			if (kenaikanPangkat.getJabatanFungsional() != null) {
				jabatan = kenaikanPangkat.getJabatanFungsional().getNama();
			} else if (kenaikanPangkat.getJabatanStruktural() != null) {
				jabatan = kenaikanPangkat.getJabatanStruktural().getNama();
			} else if (kenaikanPangkat.getJabatan() != null) {
				jabatan = kenaikanPangkat.getJabatan().getNama();
			}

			hbox = new Hbox();
			hbox.setParent(arg0);

			new Label(kenaikanPangkat.getJenisPerubahan()).setParent(hbox);
			new Label(jabatan).setParent(hbox);

			hbox = new Hbox();
			hbox.setParent(arg0);
			new Label(kenaikanPangkat.getNoSuratUsul()).setParent(hbox);
			new Label(kenaikanPangkat.getTanggalSuratUsul() == null ? ""
					: Common.dateFormat2.get().format(kenaikanPangkat.getTanggalSuratUsul())).setParent(hbox);

			String gaji = (kenaikanPangkat.getGolongan() == null ? "" : kenaikanPangkat.getGolongan().toString());
			if (kenaikanPangkat.getGajiPokok() != null) {
				gaji = kenaikanPangkat.getGajiPokok().toString();
			}

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + gaji + "</font>").setParent(arg0);

			gaji = (kenaikanPangkat.getInsentif() == null ? "" : kenaikanPangkat.getInsentif().toString());
			if (kenaikanPangkat.getInsentif() != null) {
				gaji = kenaikanPangkat.getInsentif().toString();
			}

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + gaji + "</font>").setParent(arg0);

			new Label(kenaikanPangkat.getPeraturan() == null ? "" : kenaikanPangkat.getPeraturan().getNama())
					.setParent(arg0);

			hbox = new Hbox();
			hbox.setParent(arg0);
			new Label(kenaikanPangkat.getNomorSuratkeputusan()).setParent(hbox);
			new Label(kenaikanPangkat.getTanggalSuratkeputusan() == null ? ""
					: Common.dateFormat1.get().format(kenaikanPangkat.getTanggalSuratkeputusan())).setParent(hbox);

			new Label((kenaikanPangkat.getMulai() == null ? "" : Common.dateFormat1.get().format(kenaikanPangkat.getMulai()))
					+ " s.d " + (kenaikanPangkat.getSampai() == null ? ""
							: Common.dateFormat1.get().format(kenaikanPangkat.getSampai())))
					.setParent(arg0);

			new Label(kenaikanPangkat.getNamaPejabat()).setParent(arg0);

			new Label(kenaikanPangkat.getMenjabat() ? "Ya" : "Tidak").setParent(arg0);

			if (kenaikanPangkat.getSampai() != null
					&& !Common.dateFormat83.get().format(kenaikanPangkat.getSampai())
							.equals(Common.dateFormat83.get().format(WaktuUtil.getDate()))
					&& kenaikanPangkat.getSampai().before(WaktuUtil.getDate())) {
				new ais.ui.util.MyHtml("<font style=\"font-size: x-small;color:green;\">terlewat</font>")
						.setParent(arg0);
			} else {
				new ais.ui.util.MyHtml(
						kenaikanPangkat.getStatus() ? "<font style=\"font-size: x-small;color:blue;\">disetujui</font>"
								: "<font style=\"font-size: x-small;color:red;\">belum disetujui</font>")
						.setParent(arg0);
			}

			a = new Vbox();
			a.setParent(arg0);
			if (kenaikanPangkat.getDisposisiSop() != null) {
				new Label(Common.simpleString(kenaikanPangkat.getKeterangan())).setParent(a);
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + kenaikanPangkat.getDisposisiSop().getKeterangan() + " ("
						+ kenaikanPangkat.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(kenaikanPangkat.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			} else {
				new Label(kenaikanPangkat.getKeterangan()).setParent(a);
			}

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kenaikanPangkat);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

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
											Common.refreshDeleteFlush(kenaikanPangkat);
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
			button.setParent(toolbar);

		}
	}

	public void onAdd(Event event) throws Exception {
		init(new KenaikanPangkat());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KenaikanPangkat kenaikanPangkat) throws Exception {
		this.kenaikanPangkat = kenaikanPangkat;
		addWindow.setTitle(kenaikanPangkat.getId() == null ? "Tambah Kenaikan / Penurunan Jabatan dan Golongan" : "Ubah Kenaikan / Penurunan Jabatan dan Golongan");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(kenaikanPangkat, disposisiSop, save, null));

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

	/**
	 * Mengisi field <b>Gaji Pokok</b> secara OTOMATIS mengikuti <b>masa kerja</b> pegawai (dari data
	 * kepegawaian) dan <b>golongan / penggajian berdasarkan</b> yang dipilih, dengan nilai nominal
	 * sesuai tabel master Gaji Pokok ({@link ais.action.master.employ.helper.MasaKerjaUtil#cariGajiPokok}).
	 *
	 * <p>Dilewati bila form memakai mode "Gaji Langsung Ditentukan Disini" (nominal manual) atau bila
	 * pegawai/golongan belum dipilih. Tidak menimpa bila tidak ada baris tabel yang cocok.</p>
	 */
	private void autoIsiGajiPokokDariMasaKerja() {
		try {
			if (gajiPokok == null) {
				return;
			}
			// Hanya auto-isi bila cekbox "Penggajian Otomatis Berdasarkan Masa Kerja" DIPILIH.
			// Default tidak dipilih → proses penggajian tetap berjalan manual seperti sekarang.
			if (gajiPokokOtomatisMasaKerja == null || !gajiPokokOtomatisMasaKerja.isChecked()) {
				return;
			}
			// Mode "gaji langsung ditentukan disini": nominal diisi manual → jangan ditimpa.
			if (gajiLangsungDitentukanDisini != null && gajiLangsungDitentukanDisini.isChecked()) {
				return;
			}
			Pegawai peg = ambilDataPegawaiBanbox == null ? null
					: (Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai");
			ais.database.model.employ.Golongan gol = golongan == null ? null
					: (ais.database.model.employ.Golongan) golongan.getAttribute("golongan");
			if (peg == null || gol == null) {
				return;
			}
			ais.database.model.employ.GajiPokok gp = ais.action.master.employ.helper.MasaKerjaUtil
					.cariGajiPokok(peg, gol, ais.ui.util.WaktuUtil.getDate());
			if (gp != null) {
				gajiPokok.setAttribute("gajiPokok", gp);
				gajiPokok.setValue(gp.toString());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (terdapatKenaikanGajiBerkala.isChecked() && kenaikanBerkalaBulan.getValue() == null) {
			MyMessageboxConfig.show("Jika terdapat kenaikan gaji berkali, masukkan bulan", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		KenaikanPangkatDao kenaikanPangkatDao = DaoFactory.getInstance().getKenaikanPangkatDao();
		if (kenaikanPangkat.getId() != null) {
			kenaikanPangkat = kenaikanPangkatDao.load(kenaikanPangkat.getId());
		}
		kenaikanPangkat.setTerdapatKenaikanGajiBerkala(terdapatKenaikanGajiBerkala.isChecked());
		kenaikanPangkat.setKenaikanBerkalaBulan(
				kenaikanBerkalaBulan.getValue() == null ? null : kenaikanBerkalaBulan.getValue().intValue());

		kenaikanPangkat.setMulai(mulai.getValue());
		kenaikanPangkat.setSampai(sampai.getValue());

		kenaikanPangkat.setStatus(status.isChecked());
		kenaikanPangkat.setMenjabat(menjabat.isChecked());
		kenaikanPangkat
				.setJenis((String) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue()));
		kenaikanPangkat.setJabatanFungsional((JabatanFungsional) (kenaikanPangkat.getJenis() != null
				&& kenaikanPangkat.getJenis().equals(Pegawai.JENIS_FUNGSIONAL)
						? jabatanFungsional.getSelectedItem() == null ? null
								: jabatanFungsional.getSelectedItem().getValue()
						: null));
		kenaikanPangkat.setJabatanStruktural((JabatanStruktural) (kenaikanPangkat.getJenis() != null
				&& kenaikanPangkat.getJenis().equals(Pegawai.JENIS_STRUKTURAL)
						? jabatanStruktural.getSelectedItem() == null ? null
								: jabatanStruktural.getSelectedItem().getValue()
						: null));

		kenaikanPangkat.setJabatan((Jabatan) (kenaikanPangkat.getJenis() != null
				&& !kenaikanPangkat.getJenis().equals(Pegawai.JENIS_STRUKTURAL)
				&& !kenaikanPangkat.getJenis().equals(Pegawai.JENIS_FUNGSIONAL)
						? jabatan.getSelectedItem() == null ? null : jabatan.getSelectedItem().getValue()
						: null));

		kenaikanPangkat.setKenaikanJabatan(kenaikanJabatan.isChecked());

		kenaikanPangkat.setPeraturan(
				(Peraturan) (peraturan.getSelectedItem() == null ? null : peraturan.getSelectedItem().getValue()));
		kenaikanPangkat.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		kenaikanPangkat.setNomorSuratkeputusan(nomorSuratkeputusan.getValue());
		kenaikanPangkat.setTanggalSuratkeputusan(tanggalSuratkeputusan.getValue());
		kenaikanPangkat.setNamaPejabat(namaPejabat.getValue());
		kenaikanPangkat.setKeterangan(keterangan.getValue());
		kenaikanPangkat.setTanggalSuratUsul(tanggalSuratUsul.getValue());
		kenaikanPangkat.setNoSuratUsul(noSuratUsul.getValue());
		kenaikanPangkat.setGolongan((Golongan) (golongan.getAttribute("golongan")));
		kenaikanPangkat.setGajiPokok((GajiPokok) gajiPokok.getAttribute("gajiPokok"));

		kenaikanPangkat.setInsentif((Insentif) insentif.getAttribute("insentif"));
		kenaikanPangkat.setJsonDataPengguna(jsonDataPengguna == null ? null : jsonDataPengguna.toString());
		kenaikanPangkat.setNonAktifkanJabatanSebelumnya(nonAktifkanJabatanSebelumnya.isChecked());
		kenaikanPangkat.setNonAktifkanPengguna(nonAktifkanPengguna.isChecked());
		kenaikanPangkat.setAktifkanPengguna(aktifkanPengguna.isChecked());
		kenaikanPangkat.setJenisPerubahan((String) (jenisPerubahan.getSelectedItem() == null ? null
				: jenisPerubahan.getSelectedItem().getValue()));

		kenaikanPangkat.setGajiLangsungDitentukanDisini(gajiLangsungDitentukanDisini.isChecked());
		kenaikanPangkat.setGajiPokokOtomatisMasaKerja(
				gajiPokokOtomatisMasaKerja != null && gajiPokokOtomatisMasaKerja.isChecked());
		kenaikanPangkat.setNilaiGaji(nilaiGaji.getValue());
		kenaikanPangkat.setNilaiInsentif(nilaiInsentif.getValue());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			kenaikanPangkat.setDisposisiSop(disposisiSop);
		}

		if (kenaikanPangkat.getId() != null) {
			kenaikanPangkatDao.update(kenaikanPangkat);
		} else {
			kenaikanPangkatDao.save(kenaikanPangkat);
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(kenaikanPangkat.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (nonAktifkanJabatanSebelumnya.isChecked() && kenaikanPangkat.getStatus()) {
			Session session = HibernateUtil.currentSession();
			List<KenaikanPangkat> kenaikanJabatans = session.createCriteria(KenaikanPangkat.class)
					.add(Restrictions.eq("pegawai", kenaikanPangkat.getPegawai()))
					.add(Restrictions.lt("id", kenaikanPangkat.getId())).list();
			for (KenaikanPangkat kenaikanPangkat : kenaikanJabatans) {
				kenaikanPangkat.setMenjabat(false);
				if (kenaikanPangkat.getSampai() == null) {
					kenaikanPangkat.setSampai(WaktuUtil.kemarin());
				}
				Common.refreshUpdate(session, kenaikanPangkat);
			}

		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kenaikanPangkat.getStatus() && kenaikanPangkat.getMenjabat()) {
					Session session = HibernateUtil.currentSession();
					List<Tbmuser> tbmusers = ConstantValues.simpleList(session.createCriteria(Tbmuser.class)
							.add(Restrictions.eq("pegawai", kenaikanPangkat.getPegawai())), Tbmuser.class);
					for (Tbmuser tbmuser : tbmusers) {
						JSONObject jsonObject = jsonDataPengguna.isNull(tbmuser.getUserId()) ? null
								: jsonDataPengguna.getJSONObject(tbmuser.getUserId());
						if (jsonObject == null) {
							Tbmrole tbmrole1 = tbmuser.getUserRole();
							Tbmrole tbmrole2 = tbmuser.getUserRole2();
							Tbmrole tbmrole3 = tbmuser.getUserRole3();
							Tbmrole tbmrole4 = tbmuser.getUserRole4();

							Tbmrole tbmrole5 = tbmuser.getUserRole5();
							jsonObject = new JSONObject();
							jsonObject.put("tbmrole1", tbmrole1 == null ? "" : tbmrole1.getRoleId());
							jsonObject.put("tbmrole2", tbmrole2 == null ? "" : tbmrole2.getRoleId());
							jsonObject.put("tbmrole3", tbmrole3 == null ? "" : tbmrole3.getRoleId());
							jsonObject.put("tbmrole4", tbmrole4 == null ? "" : tbmrole4.getRoleId());
							jsonObject.put("tbmrole5", tbmrole5 == null ? "" : tbmrole5.getRoleId());
						}

						Tbmrole tbmrole1 = (Tbmrole) (jsonObject.isNull("tbmrole1")
								|| jsonObject.getString("tbmrole1").trim().isEmpty() ? null
										: ConstantValues.ambil(Tbmrole.class.getName(),
												jsonObject.getString("tbmrole1")));

						Tbmrole tbmrole2 = (Tbmrole) (jsonObject.isNull("tbmrole2")
								|| jsonObject.getString("tbmrole2").trim().isEmpty() ? null
										: ConstantValues.ambil(Tbmrole.class.getName(),
												jsonObject.getString("tbmrole2")));

						Tbmrole tbmrole3 = (Tbmrole) (jsonObject.isNull("tbmrole3")
								|| jsonObject.getString("tbmrole3").trim().isEmpty() ? null
										: ConstantValues.ambil(Tbmrole.class.getName(),
												jsonObject.getString("tbmrole3")));

						Tbmrole tbmrole4 = (Tbmrole) (jsonObject.isNull("tbmrole4")
								|| jsonObject.getString("tbmrole4").trim().isEmpty() ? null
										: ConstantValues.ambil(Tbmrole.class.getName(),
												jsonObject.getString("tbmrole4")));

						Tbmrole tbmrole5 = (Tbmrole) (jsonObject.isNull("tbmrole5")
								|| jsonObject.getString("tbmrole5").trim().isEmpty() ? null
										: ConstantValues.ambil(Tbmrole.class.getName(),
												jsonObject.getString("tbmrole5")));
						tbmuser.setUserRole(tbmrole1);
						tbmuser.setUserRole2(tbmrole2);
						tbmuser.setUserRole3(tbmrole3);
						tbmuser.setUserRole4(tbmrole4);
						tbmuser.setUserRole5(tbmrole5);

						Common.refreshUpdate(session, tbmuser);
					}
				}

				if (kenaikanPangkat.getStatus() && kenaikanPangkat.getNonAktifkanPengguna()) {
					Session session = HibernateUtil.currentSession();
					Pegawai pegawai = kenaikanPangkat.getPegawai();
					session.refresh(pegawai);
					pegawai.setAktif(false);
					Common.refreshUpdate(session, pegawai);

					List<Tbmuser> tbmusers = ConstantValues.simpleList(
							session.createCriteria(Tbmuser.class).add(Restrictions.eq("pegawai", pegawai)),
							Tbmuser.class);
					for (Tbmuser tbmuser : tbmusers) {
						tbmuser.setAktif(false);
						Common.refreshUpdate(session, tbmuser);
					}
					session.flush();
				}

				if (kenaikanPangkat.getStatus() && kenaikanPangkat.getAktifkanPengguna()) {
					Session session = HibernateUtil.currentSession();
					Pegawai pegawai = kenaikanPangkat.getPegawai();
					session.refresh(pegawai);
					pegawai.setAktif(true);
					Common.refreshUpdate(session, pegawai);
					List<Tbmuser> tbmusers = ConstantValues.simpleList(
							session.createCriteria(Tbmuser.class).add(Restrictions.eq("pegawai", pegawai)),
							Tbmuser.class);
					for (Tbmuser tbmuser : tbmusers) {
						tbmuser.setAktif(true);
						Common.refreshUpdate(session, tbmuser);
					}
					session.flush();
				}
			}
		}, "", false, 4000);

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
		Criteria criteria = session.createCriteria(KenaikanPangkat.class).createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

		;

		if (order)
			criteria.addOrder(Order.desc("tanggalSuratkeputusan")).addOrder(Order.desc("tanggalSuratUsul"))
					.addOrder(Order.asc("pegawai"));

		criteria.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null
				? (searchpegawai.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchpegawai.getValue().trim(), MatchMode.ANYWHERE))
				: Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai"))))
				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))

				.add(searchJenisPerubahan.getSelectedItem() == null
						|| searchJenisPerubahan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisPerubahan", searchJenisPerubahan.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KenaikanPangkat> kenaikanPangkat = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kenaikanPangkat);
		grid.setRowRenderer(new KenaikanPangkatRenderer());
		grid.setModelCheckMobile(strset);

	}

	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {
		this.disposisiSop = disposisiSop;
		this.kenaikanPangkat = (KenaikanPangkat) generalValueObject;
		final MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox(false, true);
		if (persetujuan) {
			row.appendChild(
					new Label(kenaikanPangkat.getPegawai() == null ? "" : kenaikanPangkat.getPegawai().getNama()));
		} else {
			row.appendChild(ambilDataPegawaiBanbox);
		}

		ambilDataPegawaiBanbox
				.setValue(kenaikanPangkat.getPegawai() == null ? "" : kenaikanPangkat.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", kenaikanPangkat.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		if (this.pegawai != null) {
			ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
			ambilDataPegawaiBanbox.setValue(pegawai.toString());
			ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
		}

		jenisPerubahan = new MyCombobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(KenaikanPangkat.UBAH_JABATAN_DAN_GOLONGAN);
		comboitem.setValue(KenaikanPangkat.UBAH_JABATAN_DAN_GOLONGAN);
		jenisPerubahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig(KenaikanPangkat.UBAH_JABATAN);
		comboitem.setValue(KenaikanPangkat.UBAH_JABATAN);
		jenisPerubahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig(KenaikanPangkat.UBAH_GOLONGAN);
		comboitem.setValue(KenaikanPangkat.UBAH_GOLONGAN);
		jenisPerubahan.appendChild(comboitem);
		comboitem = new MyComboitemConfig(KenaikanPangkat.UBAH_PENGUNDURAN_DIRI);
		comboitem.setValue(KenaikanPangkat.UBAH_PENGUNDURAN_DIRI);
		jenisPerubahan.appendChild(comboitem);
		jenisPerubahan.setSelectedItem(comboitem);
		jenisPerubahan.setReadonly(true);

		Common.selectComboItem(jenisPerubahan, kenaikanPangkat.getJenisPerubahan());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Perubahan Jabatan atau Golongan *"));
		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getJenisPerubahan()));
		} else {
			row.appendChild(jenisPerubahan);
		}

		final MyFormRow rowkenaikanJabatan = new MyFormRow();
		rowkenaikanJabatan.setParent(rows);
		rowkenaikanJabatan.appendChild(new ais.ui.util.MyLabelConfig(""));
		kenaikanJabatan = new MyCheckboxConfig("Merupakan perubahan jabatan fungsional atau struktural");

		if (persetujuan) {
			rowkenaikanJabatan.appendChild(new Label("Merupakan perubahan jabatan fungsional atau struktural ? "
					+ (kenaikanPangkat.getKenaikanJabatan() ? "Ya" : "Tidak")));
		} else {
			rowkenaikanJabatan.appendChild(kenaikanJabatan);
		}

		kenaikanJabatan.setChecked(kenaikanPangkat.getKenaikanJabatan());

		final MyFormRow jenisjabatanrow = new MyFormRow();
		jenisjabatanrow.setVisible(false);
		jenisjabatanrow.setParent(rows);
		jenisjabatanrow.appendChild(new MyLabelConfig("Jenis Jabatan"));
		jenis = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Pegawai.JENIS_STRUKTURAL);
		comboitem.setValue(Pegawai.JENIS_STRUKTURAL);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Pegawai.JENIS_FUNGSIONAL);
		comboitem.setValue(Pegawai.JENIS_FUNGSIONAL);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Pegawai.JENIS_HONORER);
		comboitem.setValue(Pegawai.JENIS_HONORER);
		jenis.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Pegawai.JENIS_OUTSOURCHING);
		comboitem.setValue(Pegawai.JENIS_OUTSOURCHING);
		jenis.appendChild(comboitem);
		Common.selectComboItem(jenis, kenaikanPangkat.getJenis());

		if (persetujuan) {
			jenisjabatanrow.appendChild(new Label(kenaikanPangkat.getJenis()));
		} else {
			jenisjabatanrow.appendChild(jenis);
		}

		jenis.setWidth("90%");
		jenis.setReadonly(true);

		final MyFormRow jabatanrow = new MyFormRow();
		jabatanrow.setVisible(false);
		jabatanrow.setParent(rows);
		jabatanrow.appendChild(new MyLabelConfig("Jabatan"));
		Common.insertCombo(jabatan = new Combobox(), "nama", Jabatan.class);
		Common.selectComboItem(jabatan, kenaikanPangkat.getJabatan());

		if (persetujuan) {
			jabatanrow.appendChild(
					new Label(kenaikanPangkat.getJabatan() == null ? "" : kenaikanPangkat.getJabatan().getNama()));
		} else {
			jabatanrow.appendChild(jabatan);
		}

		jabatan.setWidth("90%");
		jabatan.setReadonly(true);

		final MyFormRow jabatanfungsionalrow = new MyFormRow();
		jabatanfungsionalrow.setVisible(false);
		jabatanfungsionalrow.setParent(rows);
		jabatanfungsionalrow.appendChild(new MyLabelConfig("Jabatan Fungsional"));
		Common.insertComboDanSemua(jabatanFungsional = new Combobox(), new String[] { "kode", "nama" }, "keterangan",
				JabatanFungsional.class, "=Jabatan Fungsional=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jabatanFungsional, kenaikanPangkat.getJabatanFungsional());

		if (persetujuan) {
			jabatanfungsionalrow.appendChild(new Label(kenaikanPangkat.getJabatanFungsional() == null ? ""
					: kenaikanPangkat.getJabatanFungsional().getNama()));
		} else {
			jabatanfungsionalrow.appendChild(jabatanFungsional);
		}

		jabatanFungsional.setWidth("90%");
		jabatanFungsional.setReadonly(true);

		final MyFormRow jabatanstrukturalrow = new MyFormRow();
		jabatanstrukturalrow.setVisible(false);
		jabatanstrukturalrow.setParent(rows);
		jabatanstrukturalrow.appendChild(new MyLabelConfig("Jabatan Struktural"));
		Common.insertComboDanSemua(jabatanStruktural = new Combobox(), new String[] { "kode", "nama" }, "keterangan",
				JabatanStruktural.class, "=Jabatan Struktural=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jabatanStruktural, kenaikanPangkat.getJabatanStruktural());

		if (persetujuan) {
			jabatanfungsionalrow.appendChild(new Label(kenaikanPangkat.getJabatanStruktural() == null ? ""
					: kenaikanPangkat.getJabatanStruktural().getNama()));
		} else {
			jabatanstrukturalrow.appendChild(jabatanStruktural);
		}

		jabatanStruktural.setWidth("90%");
		jabatanStruktural.setReadonly(true);

		final EventListener jabatanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jabatanrow.setVisible(false);
				jabatanfungsionalrow.setVisible(false);
				jabatanstrukturalrow.setVisible(false);
				String myjenis = (String) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue());

				if (myjenis != null) {
					if (myjenis.equals(Pegawai.JENIS_FUNGSIONAL)) {
						jabatanfungsionalrow.setVisible(true);
					} else if (myjenis.equals(Pegawai.JENIS_STRUKTURAL)) {
						jabatanstrukturalrow.setVisible(true);
					} else {
						jabatanrow.setVisible(true);
					}
				}
			}
		};

		jenis.addEventListener("onChange", jabatanEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Surat Usul"));

		noSuratUsul = new Textbox(kenaikanPangkat.getNoSuratUsul() == null ? "" : kenaikanPangkat.getNoSuratUsul());

		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getNoSuratUsul()));
		} else {
			row.appendChild(noSuratUsul);
		}

		noSuratUsul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat Usul"));

		tanggalSuratUsul = new MyDatebox(kenaikanPangkat.getTanggalSuratUsul() == null ? ais.ui.util.WaktuUtil.getDate()
				: kenaikanPangkat.getTanggalSuratUsul());

		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat1.get().format(tanggalSuratUsul.getValue())));
		} else {
			row.appendChild(tanggalSuratUsul);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peraturan"));
		peraturan = new Combobox();

		if (persetujuan) {
			row.appendChild(
					new Label(kenaikanPangkat.getPeraturan() == null ? "" : kenaikanPangkat.getPeraturan().getNama()));
		} else {
			row.appendChild(peraturan);
		}

		Common.insertComboDanSemua(peraturan, new String[] { "nama", "kode" }, "keterangan", Peraturan.class,
				"== Tanpa Peraturan ==", Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(peraturan, kenaikanPangkat.getPeraturan());
		peraturan.setWidth("90%");

		final MyFormRow rowFile = new MyFormRow();

		rowFile.setParent(rows);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowFile);
				rowFile.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Dokumen Peraturan"));
				rowFile.setVisible(false);
				Peraturan jp = (Peraturan) (peraturan.getSelectedItem() == null ? null
						: peraturan.getSelectedItem().getValue());
				if (jp != null) {

					FileFotoLain fileFotoLain = FileFotoLain.ambil(false, jp.getId(), Peraturan.class.getName(),
							LampiranLain.class);

					rowFile.setVisible(fileFotoLain != null);
					Vbox myvbox = new Vbox();
					myvbox.setParent(rowFile);

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, jp.getId(), Peraturan.class.getName(),
							"Peraturan Dokumen", false, null, null, false, false, false, false);
				}
			}
		};
		peraturan.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gaji Langsung Ditentukan Disini"));
		gajiLangsungDitentukanDisini = new MyCheckboxConfig();

		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getGajiLangsungDitentukanDisini() ? "Ya" : "Tidak"));
		} else {
			row.appendChild(gajiLangsungDitentukanDisini);
		}
		gajiLangsungDitentukanDisini.setChecked(kenaikanPangkat.getGajiLangsungDitentukanDisini());

		// Cekbox: "Penggajian Otomatis Berdasarkan Masa Kerja".
		// - DIPILIH  : Gaji Pokok diisi/diikuti otomatis dari TAHUN MASA KERJA pegawai + golongan
		//              (nilai nominal sesuai tabel master Gaji Pokok).
		// - TIDAK DIPILIH (default): penggajian TETAP mengikuti proses yang berjalan saat ini (manual).
		MyFormRow rowGajiOtomatis = new MyFormRow();
		rowGajiOtomatis.setParent(rows);
		rowGajiOtomatis.appendChild(new ais.ui.util.MyLabelConfig("Penggajian Otomatis Berdasarkan Masa Kerja"));
		gajiPokokOtomatisMasaKerja = new MyCheckboxConfig();
		if (persetujuan) {
			rowGajiOtomatis
					.appendChild(new Label(kenaikanPangkat.getGajiPokokOtomatisMasaKerja() ? "Ya" : "Tidak"));
		} else {
			rowGajiOtomatis.appendChild(gajiPokokOtomatisMasaKerja);
		}
		gajiPokokOtomatisMasaKerja.setChecked(kenaikanPangkat.getGajiPokokOtomatisMasaKerja());

		rownilaiGaji = new MyFormRow();
		rownilaiGaji.setParent(rows);
		rownilaiGaji.appendChild(new ais.ui.util.MyLabelConfig("Nilai Gaji"));

		nilaiGaji = new MyDoublebox(kenaikanPangkat.getNilaiGaji());

		if (persetujuan) {
			rownilaiGaji.appendChild(new Label(Common.numberFormat.get().format(kenaikanPangkat.getNilaiGaji())));
		} else {
			rownilaiGaji.appendChild(nilaiGaji);
		}

		rownilaiInsentif = new MyFormRow();
		rownilaiInsentif.setParent(rows);
		rownilaiInsentif.appendChild(new ais.ui.util.MyLabelConfig("Nilai Insentif"));

		nilaiInsentif = new MyDoublebox(kenaikanPangkat.getNilaiInsentif());

		if (persetujuan) {
			rownilaiInsentif.appendChild(new Label(Common.numberFormat.get().format(kenaikanPangkat.getNilaiInsentif())));
		} else {
			rownilaiInsentif.appendChild(nilaiInsentif);
		}

		menggunakanLangsungGajiPokok = Common.bolehKonfigurasi("kenaikan_pangkat_menggunakan_langsung_gaji_pokok", Konfigurasi.TIDAK_AKTIF);

		final MyFormRow rowgolongan = new MyFormRow();
		rowgolongan.setVisible(!menggunakanLangsungGajiPokok);
		rowgolongan.setParent(rows);
		rowgolongan.appendChild(new ais.ui.util.MyLabelConfig("Golongan"));
		golongan = new AmbilDataGolonganBanbox();

		if (persetujuan) {
			rowgolongan.appendChild(
					new Label(kenaikanPangkat.getGolongan() == null ? "" : kenaikanPangkat.getGolongan().getNama()));
		} else {
			rowgolongan.appendChild(golongan);
		}

		golongan.setValue(kenaikanPangkat.getGolongan() == null ? "" : kenaikanPangkat.getGolongan().getNama());
		golongan.setAttribute("golongan", kenaikanPangkat.getGolongan());
		golongan.setWidth("90%");
		golongan.setReadonly(true);

		final MyFormRow rowgajiPokok = new MyFormRow();
		rowgajiPokok.setVisible(menggunakanLangsungGajiPokok);
		rowgajiPokok.setParent(rows);
		rowgajiPokok.appendChild(new ais.ui.util.MyLabelConfig("Gaji Pokok"));
		gajiPokok = new AmbilDataGajiPokokBanbox();

		if (persetujuan) {
			rowgajiPokok.appendChild(
					new Label(kenaikanPangkat.getGajiPokok() == null ? "" : kenaikanPangkat.getGajiPokok().getNama()));
		} else {
			rowgajiPokok.appendChild(gajiPokok);
		}

		gajiPokok.setValue(kenaikanPangkat.getGajiPokok() == null ? "" : kenaikanPangkat.getGajiPokok().toString());
		gajiPokok.setAttribute("gajiPokok", kenaikanPangkat.getGajiPokok());
		gajiPokok.setWidth("90%");
		gajiPokok.setReadonly(true);

		// Simpan nilai gaji pokok manual saat ini sebagai baseline untuk dikembalikan bila cekbox
		// "Penggajian Otomatis Berdasarkan Masa Kerja" dilepas (sebelum ditimpa nilai otomatis).
		gajiPokokAsli = kenaikanPangkat.getGajiPokok();

		// === Auto-isi GAJI POKOK mengikuti MASA KERJA pegawai (sesuai tabel master Gaji Pokok) ===
		// Saat Pegawai atau Golongan (Penggajian Berdasarkan) dipilih, Gaji Pokok dihitung dari masa
		// kerja pegawai (data kepegawaian) + golongan via MasaKerjaUtil.cariGajiPokok lalu diisi otomatis.
		final EventListener autoGajiPokokListener = new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				autoIsiGajiPokokDariMasaKerja();
			}
		};
		try {
			golongan.setEventListener(autoGajiPokokListener);
		} catch (Exception eAbai) { ais.common.ErrorAuditUtil.record(eAbai, "auto-audit(empty-catch) src/ais/action/master/employ/KenaikanPangkatAction.java:1177");
		}
		try {
			ambilDataPegawaiBanbox.setEventListener(autoGajiPokokListener);
		} catch (Exception eAbai) { ais.common.ErrorAuditUtil.record(eAbai, "auto-audit(empty-catch) src/ais/action/master/employ/KenaikanPangkatAction.java:1181");
		}
		// Saat cekbox "Penggajian Otomatis Berdasarkan Masa Kerja" di-klik: bila DIPILIH, langsung
		// hitung & isi Gaji Pokok dari masa kerja dan matikan pemilihan manual; bila TIDAK, aktifkan
		// kembali pemilihan manual (proses lama).
		if (gajiPokokOtomatisMasaKerja != null) {
			gajiPokokOtomatisMasaKerja.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					boolean auto = gajiPokokOtomatisMasaKerja.isChecked();
					if (auto) {
						// DIPILIH: simpan dulu nilai manual sekarang (agar bisa dikembalikan), matikan
						// pemilihan manual, lalu isi Gaji Pokok otomatis dari masa kerja.
						try {
							gajiPokokAsli = (GajiPokok) gajiPokok.getAttribute("gajiPokok");
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/employ/KenaikanPangkatAction.java:1196");
						}
						try {
							if (gajiPokok != null) {
								gajiPokok.setDisabled(true);
							}
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/employ/KenaikanPangkatAction.java:1202");
						}
						autoIsiGajiPokokDariMasaKerja();
					} else {
						// TIDAK DIPILIH: KEMBALIKAN Gaji Pokok ke nilai manual sebelumnya (proses lama)
						// dan aktifkan kembali pemilihan manual. Memperbaiki bug nilai otomatis tetap
						// tertinggal saat cekbox dilepas.
						try {
							if (gajiPokok != null) {
								gajiPokok.setDisabled(false);
								gajiPokok.setAttribute("gajiPokok", gajiPokokAsli);
								gajiPokok.setValue(gajiPokokAsli == null ? "" : gajiPokokAsli.toString());
							}
						} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/employ/KenaikanPangkatAction.java:1215");
						}
					}
				}
			});
		}
		// Keadaan awal saat form dibuka: hormati cekbox tersimpan. Bila otomatis aktif, isi/ikuti
		// masa kerja & matikan pemilihan manual; method autoIsi sudah ter-gate oleh cekbox.
		try {
			if (gajiPokok != null && gajiPokokOtomatisMasaKerja != null) {
				gajiPokok.setDisabled(gajiPokokOtomatisMasaKerja.isChecked());
			}
		} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/employ/KenaikanPangkatAction.java:1227");
		}
		autoIsiGajiPokokDariMasaKerja();

		final MyFormRow rowInsentif = new MyFormRow();
		rowInsentif.setVisible(menggunakanLangsungGajiPokok);
		rowInsentif.setParent(rows);
		rowInsentif.appendChild(new ais.ui.util.MyLabelConfig("Insentif"));
		insentif = new AmbilDataInsentifBanbox();

		if (persetujuan) {
			rowInsentif.appendChild(
					new Label(kenaikanPangkat.getInsentif() == null ? "" : kenaikanPangkat.getInsentif().getNama()));
		} else {
			rowInsentif.appendChild(insentif);
		}

		insentif.setValue(kenaikanPangkat.getInsentif() == null ? "" : kenaikanPangkat.getInsentif().toString());
		insentif.setAttribute("insentif", kenaikanPangkat.getInsentif());
		insentif.setWidth("90%");
		insentif.setReadonly(true);

		final EventListener jenisjabatanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				jabatanrow.setVisible(false);
				jabatanfungsionalrow.setVisible(false);
				jabatanstrukturalrow.setVisible(false);
				jenisjabatanrow.setVisible(kenaikanJabatan.isChecked());
				if (kenaikanJabatan.isChecked()) {
					jabatanEventListener.onEvent(arg0);
				}
			}
		};

		kenaikanJabatan.addEventListener("onCheck", jenisjabatanEventListener);
		jenisjabatanEventListener.onEvent(null);

		rowterdapatKenaikanGajiBerkala = new MyFormRow();
		rowterdapatKenaikanGajiBerkala.setParent(rows);
		rowterdapatKenaikanGajiBerkala.appendChild(new ais.ui.util.MyLabelConfig("Terdapat kenaikan gaji berkala"));
		terdapatKenaikanGajiBerkala = new MyCheckboxConfig();

		if (persetujuan) {
			rowterdapatKenaikanGajiBerkala
					.appendChild(new Label(kenaikanPangkat.getTerdapatKenaikanGajiBerkala() ? "Ya" : "Tidak"));
		} else {
			rowterdapatKenaikanGajiBerkala.appendChild(terdapatKenaikanGajiBerkala);
		}
		terdapatKenaikanGajiBerkala.setChecked(kenaikanPangkat.getTerdapatKenaikanGajiBerkala());

		rowKenaikan = new MyFormRow();
		rowKenaikan.setParent(rows);
		rowKenaikan.appendChild(new ais.ui.util.MyLabelConfig("Kenaikan Berkala di Bulan"));
		kenaikanBerkalaBulan = new Decimalbox(kenaikanPangkat.getKenaikanBerkalaBulan() == null ? null
				: new BigDecimal(kenaikanPangkat.getKenaikanBerkalaBulan()));
		if (persetujuan) {
			rowKenaikan.appendChild(new Label(kenaikanPangkat.getKenaikanBerkalaBulan() == null ? ""
					: Common.numberFormat.get().format(kenaikanPangkat.getKenaikanBerkalaBulan())));
		} else {
			rowKenaikan.appendChild(kenaikanBerkalaBulan);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Surat Keputusan"));
		nomorSuratkeputusan = new Textbox(kenaikanPangkat.getNomorSuratkeputusan());

		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getNomorSuratkeputusan()));
		} else {
			row.appendChild(nomorSuratkeputusan);
		}

		nomorSuratkeputusan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat Keputusan"));
		tanggalSuratkeputusan = new MyDatebox(kenaikanPangkat.getTanggalSuratkeputusan());
		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getTanggalSuratkeputusan() == null ? ""
					: Common.dateFormat1.get().format(kenaikanPangkat.getTanggalSuratkeputusan())));
		} else {
			row.appendChild(tanggalSuratkeputusan);
		}

		rowmenjabat = new MyFormRow();
		rowmenjabat.setParent(rows);
		rowmenjabat.appendChild(new ais.ui.util.MyLabelConfig(""));
		menjabat = new MyCheckboxConfig("Jabatan atau golongan ini sedang aktif / dijabat");

		if (persetujuan) {
			rowmenjabat.appendChild(new Label("Jabatan atau golongan ini sedang aktif / dijabat ? "
					+ (kenaikanPangkat.getMenjabat() ? "Ya" : "Tidak")));
		} else {
			rowmenjabat.appendChild(menjabat);
		}

		menjabat.setChecked(kenaikanPangkat.getMenjabat());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terhitung Mulai"));
		mulai = new MyDatebox(kenaikanPangkat.getMulai());
		if (persetujuan) {
			row.appendChild(new Label(
					kenaikanPangkat.getMulai() == null ? "" : Common.dateFormat1.get().format(kenaikanPangkat.getMulai())));
		} else {
			row.appendChild(mulai);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Terhitung Sampai"));
		sampai = new MyDatebox(kenaikanPangkat.getSampai());
		if (persetujuan) {
			row.appendChild(new Label(
					kenaikanPangkat.getSampai() == null ? "" : Common.dateFormat1.get().format(kenaikanPangkat.getSampai())));
		} else {
			row.appendChild(sampai);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pejabat"));
		namaPejabat = new Textbox(kenaikanPangkat.getNamaPejabat());
		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getNamaPejabat()));
		} else {
			row.appendChild(namaPejabat);
		}
		namaPejabat.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(kenaikanPangkat.getKeterangan());
		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Non Aktifkan Jabatan Sebelumnya"));
		nonAktifkanJabatanSebelumnya = new MyCheckboxConfig();

		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getNonAktifkanJabatanSebelumnya() ? "Ya" : "Tidak"));
		} else {
			row.appendChild(nonAktifkanJabatanSebelumnya);
		}
		nonAktifkanJabatanSebelumnya.setChecked(kenaikanPangkat.getNonAktifkanJabatanSebelumnya());

		rownonAktifkanPengguna = new MyFormRow();
		rownonAktifkanPengguna.setParent(rows);
		rownonAktifkanPengguna.appendChild(new ais.ui.util.MyLabelConfig("Non Aktifkan Pengguna"));
		nonAktifkanPengguna = new MyCheckboxConfig();

		if (persetujuan) {
			rownonAktifkanPengguna.appendChild(new Label(kenaikanPangkat.getNonAktifkanPengguna() ? "Ya" : "Tidak"));
		} else {
			rownonAktifkanPengguna.appendChild(nonAktifkanPengguna);
		}
		nonAktifkanPengguna.setChecked(kenaikanPangkat.getNonAktifkanPengguna());

		rowaktifkanPengguna = new MyFormRow();
		rowaktifkanPengguna.setParent(rows);
		rowaktifkanPengguna.appendChild(new ais.ui.util.MyLabelConfig("Aktifkan Pengguna"));
		aktifkanPengguna = new MyCheckboxConfig();

		if (persetujuan) {
			rowaktifkanPengguna.appendChild(new Label(kenaikanPangkat.getAktifkanPengguna() ? "Ya" : "Tidak"));
		} else {
			rowaktifkanPengguna.appendChild(aktifkanPengguna);
		}
		aktifkanPengguna.setChecked(kenaikanPangkat.getAktifkanPengguna());

		final EventListener a = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rownonAktifkanPengguna.setVisible(true);
				rowaktifkanPengguna.setVisible(true);

				if (nonAktifkanPengguna.isChecked()) {
					aktifkanPengguna.setChecked(false);
					rowaktifkanPengguna.setVisible(false);
				}
				if (aktifkanPengguna.isChecked()) {
					nonAktifkanPengguna.setChecked(false);
					rownonAktifkanPengguna.setVisible(false);
				}
			}
		};
		nonAktifkanPengguna.addEventListener("onClick", a);
		aktifkanPengguna.addEventListener("onClick", a);
		a.onEvent(null);

		jsonDataPengguna = new JSONObject(kenaikanPangkat.getJsonDataPengguna());
		final MyFormRow rowUser = new MyFormRow();
		rowUser.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowUser, "2");
		EventListener eventListener2 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai pegawai = (Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai");
				Common.clear(rowUser);
				if (pegawai != null) {
					KenaikanPangkatAction.reloadUser(rowUser, pegawai, persetujuan, jsonDataPengguna);
				}
			}
		};

		ambilDataPegawaiBanbox.setEventListener(eventListener2);
		eventListener2.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		status = new MyCheckboxConfig();

		if (persetujuan) {
			row.appendChild(new Label(kenaikanPangkat.getStatus() ? "Ya" : "Tidak"));
		} else {
			row.appendChild(status);
		}

		row.setVisible(disposisiSop == null && CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.setChecked(kenaikanPangkat.getStatus());
		status.setDisabled(!CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		status.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.freeze(grid, status.isChecked());
				status.setDisabled(false);
				if (pegawai != null) {
					ambilDataPegawaiBanbox.setValue(pegawai.toString());
					ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
					ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
				}
			}
		});

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Dokumen"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kenaikanPangkat.getId(), KenaikanPangkat.class.getName(),
				"Dokumen", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, !persetujuan, null);
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran dokumen lebih dari satu file, zip dulu semua file tersebut");

		EventListener jenisPerubahanEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				rowkenaikanJabatan.setVisible(true);
				jabatanrow.setVisible(true);
				jabatanfungsionalrow.setVisible(true);
				jabatanstrukturalrow.setVisible(true);
				rowmenjabat.setVisible(true);
				rowgolongan.setVisible(true);
				rowgajiPokok.setVisible(true);
				nonAktifkanPengguna.setDisabled(false);
				nonAktifkanJabatanSebelumnya.setDisabled(false);

				String s = (String) (jenisPerubahan.getSelectedItem() == null
						|| jenisPerubahan.getSelectedItem().getValue() == null
								? KenaikanPangkat.UBAH_JABATAN_DAN_GOLONGAN
								: jenisPerubahan.getSelectedItem().getValue());
				if (s.equals(KenaikanPangkat.UBAH_PENGUNDURAN_DIRI)) {
					rowkenaikanJabatan.setVisible(false);
					jabatanrow.setVisible(false);
					jabatanfungsionalrow.setVisible(false);
					jabatanstrukturalrow.setVisible(false);
					rowgolongan.setVisible(false);
//					rowgajiPokok.setVisible(false);
					jenisjabatanEventListener.onEvent(null);
					rowmenjabat.setVisible(false);

					nonAktifkanPengguna.setChecked(true);
					nonAktifkanPengguna.setDisabled(true);
					a.onEvent(arg0);

					nonAktifkanJabatanSebelumnya.setChecked(true);
					nonAktifkanJabatanSebelumnya.setDisabled(true);

				} else if (s.equals(KenaikanPangkat.UBAH_GOLONGAN)) {
					rowkenaikanJabatan.setVisible(false);
					jabatanrow.setVisible(false);
					jabatanfungsionalrow.setVisible(false);
					jabatanstrukturalrow.setVisible(false);
				} else if (s.equals(KenaikanPangkat.UBAH_JABATAN)) {
					rowgolongan.setVisible(false);
//					rowgajiPokok.setVisible(false);
					jenisjabatanEventListener.onEvent(null);
				} else if (s.equals(KenaikanPangkat.UBAH_JABATAN_DAN_GOLONGAN)) {
					jenisjabatanEventListener.onEvent(null);
				}

				rownilaiGaji.setVisible(gajiLangsungDitentukanDisini.isChecked());
				rownilaiInsentif.setVisible(gajiLangsungDitentukanDisini.isChecked());
				rowgajiPokok.setVisible(!gajiLangsungDitentukanDisini.isChecked());
				rowInsentif.setVisible(!gajiLangsungDitentukanDisini.isChecked());
				rowterdapatKenaikanGajiBerkala.setVisible(!gajiLangsungDitentukanDisini.isChecked());
				rowKenaikan.setVisible(
						!gajiLangsungDitentukanDisini.isChecked() && terdapatKenaikanGajiBerkala.isChecked());
			}

		};

		jenisPerubahan.addEventListener("onChange", jenisPerubahanEventListener);
		jenisPerubahanEventListener.onEvent(null);
		gajiLangsungDitentukanDisini.addEventListener("onClick", jenisPerubahanEventListener);
		terdapatKenaikanGajiBerkala.addEventListener("onClick", jenisPerubahanEventListener);

		return grid;
	}

	@SuppressWarnings("unchecked")
	public static void reloadUser(final Row rowsParent, Pegawai pegawai, boolean persetujuan,
			final JSONObject jsonDataPengguna) throws Exception {
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(rowsParent);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("");
		column.setParent(columns);
		column.setWidth("100px");

		column = new MyColumnConfig("Nama Pengguna");
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig("Hak Akses I");
		column.setParent(columns);

		column = new MyColumnConfig("Hak Akses II");
		column.setParent(columns);

		column = new MyColumnConfig("Hak Akses III");
		column.setParent(columns);

		column = new MyColumnConfig("Hak Akses IV");
		column.setParent(columns);

		column = new MyColumnConfig("Hak Akses V");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Session session = HibernateUtil.currentSession();
		List<Tbmuser> tbmusers = ConstantValues.simpleList(session.createCriteria(Tbmuser.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("pegawai", pegawai)), Tbmuser.class);
		for (final Tbmuser tbmuser : tbmusers) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			CommonMedia.tampilkanGambarKecil(tbmuser).setParent(row);

			new Label(tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")").setParent(row);

			JSONObject jsonObject = jsonDataPengguna.isNull(tbmuser.getUserId()) ? null
					: jsonDataPengguna.getJSONObject(tbmuser.getUserId());
			if (jsonObject == null) {
				Tbmrole tbmrole1 = tbmuser.getUserRole();
				Tbmrole tbmrole2 = tbmuser.getUserRole2();
				Tbmrole tbmrole3 = tbmuser.getUserRole3();
				Tbmrole tbmrole4 = tbmuser.getUserRole4();

				Tbmrole tbmrole5 = tbmuser.getUserRole5();
				jsonObject = new JSONObject();
				jsonObject.put("tbmrole1", tbmrole1 == null ? "" : tbmrole1.getRoleId());
				jsonObject.put("tbmrole2", tbmrole2 == null ? "" : tbmrole2.getRoleId());
				jsonObject.put("tbmrole3", tbmrole3 == null ? "" : tbmrole3.getRoleId());
				jsonObject.put("tbmrole4", tbmrole4 == null ? "" : tbmrole4.getRoleId());
				jsonObject.put("tbmrole5", tbmrole5 == null ? "" : tbmrole5.getRoleId());

				jsonDataPengguna.put(tbmuser.getUserId(), jsonObject);
			}

			Tbmrole tbmrole1 = (Tbmrole) (jsonObject.isNull("tbmrole1")
					|| jsonObject.getString("tbmrole1").trim().isEmpty() ? null
							: ConstantValues.ambil(Tbmrole.class.getName(), jsonObject.getString("tbmrole1")));

			Tbmrole tbmrole2 = (Tbmrole) (jsonObject.isNull("tbmrole2")
					|| jsonObject.getString("tbmrole2").trim().isEmpty() ? null
							: ConstantValues.ambil(Tbmrole.class.getName(), jsonObject.getString("tbmrole2")));

			Tbmrole tbmrole3 = (Tbmrole) (jsonObject.isNull("tbmrole3")
					|| jsonObject.getString("tbmrole3").trim().isEmpty() ? null
							: ConstantValues.ambil(Tbmrole.class.getName(), jsonObject.getString("tbmrole3")));

			Tbmrole tbmrole4 = (Tbmrole) (jsonObject.isNull("tbmrole4")
					|| jsonObject.getString("tbmrole4").trim().isEmpty() ? null
							: ConstantValues.ambil(Tbmrole.class.getName(), jsonObject.getString("tbmrole4")));

			Tbmrole tbmrole5 = (Tbmrole) (jsonObject.isNull("tbmrole5")
					|| jsonObject.getString("tbmrole5").trim().isEmpty() ? null
							: ConstantValues.ambil(Tbmrole.class.getName(), jsonObject.getString("tbmrole5")));

			if (persetujuan) {
				new Label(tbmrole1 == null ? "" : tbmrole1.getRoleName()).setParent(row);
				new Label(tbmrole2 == null ? "" : tbmrole2.getRoleName()).setParent(row);
				new Label(tbmrole3 == null ? "" : tbmrole3.getRoleName()).setParent(row);
				new Label(tbmrole4 == null ? "" : tbmrole4.getRoleName()).setParent(row);
				new Label(tbmrole5 == null ? "" : tbmrole5.getRoleName()).setParent(row);
			} else {

				Criterion criterion = Restrictions.and(
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.and(Restrictions.not(Restrictions.ilike("roleName", "Ortu", MatchMode.EXACT)),
								Restrictions.and(
										Restrictions.not(Restrictions.ilike("roleName", "Siswa", MatchMode.EXACT)),
										Restrictions.and(
												Restrictions.not(
														Restrictions.ilike("roleName", "Peserta", MatchMode.EXACT)),
												Restrictions.not(Restrictions.ilike("roleName", "Mahasiswa",
														MatchMode.EXACT))))));

				final Combobox userRole = new Combobox();
				Common.insertComboDanSemua(userRole, new String[] { "roleName" }, "roleName", Tbmrole.class,
						"== Tanpa hak akses I ==", criterion);
				Common.selectComboItem(true, userRole, tbmrole1);
				userRole.setWidth("95%");
				userRole.setParent(row);

				final Combobox userRole2 = new Combobox();
				Common.insertComboDanSemua(userRole2, new String[] { "roleName" }, "roleName", Tbmrole.class,
						"== Tanpa hak akses II ==", criterion);
				Common.selectComboItem(true, userRole2, tbmrole2);
				userRole2.setWidth("95%");
				userRole2.setParent(row);

				final Combobox userRole3 = new Combobox();
				Common.insertComboDanSemua(userRole3, new String[] { "roleName" }, "roleName", Tbmrole.class,
						"== Tanpa hak akses II ==", criterion);
				Common.selectComboItem(true, userRole3, tbmrole3);
				userRole3.setWidth("95%");
				userRole3.setParent(row);

				final Combobox userRole4 = new Combobox();
				Common.insertComboDanSemua(userRole4, new String[] { "roleName" }, "roleName", Tbmrole.class,
						"== Tanpa hak akses II ==", criterion);
				Common.selectComboItem(true, userRole4, tbmrole4);
				userRole4.setWidth("95%");
				userRole4.setParent(row);

				final Combobox userRole5 = new Combobox();
				Common.insertComboDanSemua(userRole5, new String[] { "roleName" }, "roleName", Tbmrole.class,
						"== Tanpa hak akses II ==", criterion);
				Common.selectComboItem(true, userRole5, tbmrole5);
				userRole5.setWidth("95%");
				userRole5.setParent(row);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Tbmrole tbmrole1 = (Tbmrole) userRole.getSelectedItem().getValue();
						Tbmrole tbmrole2 = (Tbmrole) (userRole2.getSelectedItem() == null ? null
								: userRole2.getSelectedItem().getValue());
						Tbmrole tbmrole3 = (Tbmrole) (userRole3.getSelectedItem() == null ? null
								: userRole3.getSelectedItem().getValue());
						Tbmrole tbmrole4 = (Tbmrole) (userRole4.getSelectedItem() == null ? null
								: userRole4.getSelectedItem().getValue());

						Tbmrole tbmrole5 = (Tbmrole) (userRole5.getSelectedItem() == null ? null
								: userRole5.getSelectedItem().getValue());

						JSONObject jsonObject = jsonDataPengguna.isNull(tbmuser.getUserId()) ? new JSONObject()
								: jsonDataPengguna.getJSONObject(tbmuser.getUserId());
						jsonObject.put("tbmrole1", tbmrole1 == null ? "" : tbmrole1.getRoleId());
						jsonObject.put("tbmrole2", tbmrole2 == null ? "" : tbmrole2.getRoleId());
						jsonObject.put("tbmrole3", tbmrole3 == null ? "" : tbmrole3.getRoleId());
						jsonObject.put("tbmrole4", tbmrole4 == null ? "" : tbmrole4.getRoleId());
						jsonObject.put("tbmrole5", tbmrole5 == null ? "" : tbmrole5.getRoleId());

						jsonDataPengguna.put(tbmuser.getUserId(), jsonObject);

					}
				};

				userRole.addEventListener("onChange", eventListener);
				userRole2.addEventListener("onChange", eventListener);
				userRole3.addEventListener("onChange", eventListener);
				userRole4.addEventListener("onChange", eventListener);
				userRole5.addEventListener("onChange", eventListener);
			}
		}
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Kenaikan/Penurunan Jabatan";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return kenaikanPangkat;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return KenaikanPangkat.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}