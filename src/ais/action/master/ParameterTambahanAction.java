package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.reflections.Reflections;
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
import org.zkoss.zul.Comboitem;
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

import ais.action.master.helper.AmbilDataParameterTambahanBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiParameterTambahanHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.GrupParameterTambahan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk parameter tambahan. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchketerangan}, {@code Combobox
 * searchtipe}, {@code Combobox searchgrup}, {@code Checkbox searchaktif}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onRefreshGrup()}, {@code onSearchDefault()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onManajemenGrup()}, {@code onAdd()}, {@code tambahBarisSyarat()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class ParameterTambahanAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchtipe;
	private Combobox searchgrup;
	private Checkbox searchaktif;

	private MyCheckboxConfig harusMenyertakanLampiran;

	private Combobox tipeDataInputan;
	private Textbox labelInputan;
	private Textbox keterangan;

	private boolean edit = true;
	private boolean delete = true;

	private ParameterTambahan parameterTambahan;
	private Textbox nilaiDataInputan;

	private Tabpanel manajemenGrupTab;

	public void onManajemenGrup(Event event) {
		if (manajemenGrupTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(manajemenGrupTab);
			MyInclude iframe = new MyInclude("/pages/master/grup_parameter_tambahan.zul");
			iframe.setParent(window);
		}
	}

	private MyToolbarbuttonConfig find;
	private Combobox grupParameterTambahan;
	private MyCheckboxConfig tampilkanIsianKeterangan;
	private MyTextbox labelInputanKeterangan;
	private MyIntbox jumlahBaris;
	protected LampiranLain lainMahasiswa;
	private Textbox kode;
	private AmbilDataParameterTambahanBanbox parent;
	// Syarat tampil (conditional display) berbasis JSON -- dukung BANYAK syarat + logika AND/OR.
	private Combobox syaratLogika;
	private Vbox syaratContainer;
	public static String[] contents = new String[] { "id", "kode", "nama", "labelInputan", "tipeDataInputan",
			"nilaiDataInputan", "harusMenyertakanLampiran", "aktif", "nomorUrut", "wajibDiisi", "keterangan",
			"grupParameterTambahan", "tampilkanIsianKeterangan", "labelInputanKeterangan", "parent", "nilaiDefault",
			"nilaiTidakBolehDiubah", "hanyaTampilDiAdmin", "kodeAdminYgBoleh", "lampiranWajibDiisi", "jumlahBaris",
			"kondisiDataInputan", "jurusan", "fakultas", "yayasan", "sekolah", "nilaiMax", "nilaiMin" };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void onRefreshGrup(Event event) throws Exception {
		Common.insertComboDanSemua(searchgrup, "nama", GrupParameterTambahan.class);
		onSearchDefault(null);
	}

	private Row hbFakultasLabel;
	private Row hbYayasan;
	private boolean pt = false;
	private boolean ya = false;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox rowNilaiInputanComboObject;
	private MyTextbox kondisiDataInputan;
	private MyDoublebox nilaiMax;
	private MyDoublebox nilaiMin;
	private MyCheckboxConfig nilaiTidakBolehDiubah;
	private MyTextbox nilaiDefault;
	private MyIntbox jumlahText;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		ParameterTambahan.generateKuetionerTracerkemendikbud();

		pt = Common.bolehKonfigurasi("apakah_aktifkan_modul_perguruan_tinggi");
		ya = Common.bolehKonfigurasi("apakah_aktifkan_modul_sekolah", Konfigurasi.TIDAK_AKTIF);

		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null && sekolah.getId() != null) {
			pt = false;
			ya = true;
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		ParameterTambahan.initContoh();

		Common.insertComboDanSemua(searchgrup, "nama", GrupParameterTambahan.class);

		if (searchtipe != null) { searchtipe.setWidth("90%"); }
		if (searchtipe != null) { searchtipe.setReadonly(true); }
		MyComboitemConfig comboitem = new MyComboitemConfig(ParameterTambahan.TIDAK_ADA);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.TIDAK_ADA); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TEXT);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.TEXT); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.ANGKA);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.ANGKA); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TEXT_ANGKA);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.TEXT_ANGKA); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TANGGAL);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.TANGGAL); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TANGGAL_DAN_WAKTU);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.TANGGAL_DAN_WAKTU); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.WAKTU);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.WAKTU); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_YA_TIDAK);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.PILIHAN_YA_TIDAK); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_CUSTOM);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.PILIHAN_CUSTOM); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_BANYAK);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.PILIHAN_BANYAK); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_MATRIX);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.PILIHAN_MATRIX); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_MATRIX_BANYAK_NILAI);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.PILIHAN_MATRIX_BANYAK_NILAI); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_MATRIX_BANYAK_COMBO);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.PILIHAN_MATRIX_BANYAK_COMBO); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_OBJECT);
		if (comboitem != null) { comboitem.setValue(ParameterTambahan.PILIHAN_OBJECT); }
		searchtipe.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		searchtipe.appendChild(comboitem);
		if (searchtipe != null) { searchtipe.setSelectedItem(comboitem); }
		if (searchtipe != null) { searchtipe.setReadonly(true); }

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, ParameterTambahan.class, contents);
		if (upload != null) { upload.setVisible(edit && delete); }
		Common.appendKeToolbar(upload, find, comp);

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

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiParameterTambahanHelper revisiHelper = new RevisiParameterTambahanHelper(new EventListener() {

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
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(find.getParent()); }
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link ParameterTambahanAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ParameterTambahanAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see ParameterTambahanAction
	 */
	class ParameterTambahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ParameterTambahan parameterTambahan = (ParameterTambahan) arg1;
			new MyLabelAgakKecil(parameterTambahan.getLabelInputan()).setParent(arg0);

			new Label(parameterTambahan.getHarusMenyertakanLampiran() ? "Ya" : "Tidak").setParent(arg0);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(ParameterTambahan.class, parameterTambahan,
					parameterTambahan.getTipeDataInputan())).setParent(arg0);

			new MyLabelAgakKecil(parameterTambahan.getKode()).setParent(a);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, parameterTambahan.getId(),
					ParameterTambahan.class.getName(), "Lampiran Parameter", false, null, null, false, false, false,
					false);

			new MyLabelAgakKecil(parameterTambahan.getNilaiDataInputan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(parameterTambahan.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahan.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahan);
				}
			});

			final MyCheckboxConfig wajib = new MyCheckboxConfig("Isian Wajib");
			wajib.setChecked(parameterTambahan.getWajibDiisi());
			wajib.setParent(arg0);
			wajib.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahan.setWajibDiisi(wajib.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahan);
				}
			});

			if (parameterTambahan.getHarusMenyertakanLampiran()) {
				final MyCheckboxConfig lampiranWajib = new MyCheckboxConfig("Lampiran Wajib");
				lampiranWajib.setChecked(parameterTambahan.getLampiranWajibDiisi());
				lampiranWajib.setParent(arg0);
				lampiranWajib.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						parameterTambahan.setLampiranWajibDiisi(lampiranWajib.isChecked());
						Common.refreshSaveOrUpdate(parameterTambahan);
					}
				});
			} else {
				new Label().setParent(arg0);
			}

			final Intbox intbox = new Intbox(parameterTambahan.getNomorUrut());
			intbox.setWidth("90%");
			intbox.setParent(arg0);
			intbox.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahan.setNomorUrut(intbox.getValue());
					Common.refreshSaveOrUpdate(parameterTambahan);
				}
			});
			final MyTextbox kodeAdminYgBoleh = new MyTextbox(parameterTambahan.getKodeAdminYgBoleh());
			final MyLabelKecil label = new MyLabelKecil(
					"Masukkan kode admin yg boleh ubah, jika lebih dari satu pisahkan dengan tanda koma");

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			final MyCheckboxConfig hanyaTampilDiAdmin = new MyCheckboxConfig("Hanya Admin");
			hanyaTampilDiAdmin.setChecked(parameterTambahan.getHanyaTampilDiAdmin());
			hanyaTampilDiAdmin.setParent(vbox);
			hanyaTampilDiAdmin.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahan.setHanyaTampilDiAdmin(hanyaTampilDiAdmin.isChecked());
					Common.refreshSaveOrUpdate(parameterTambahan);
					label.setVisible(parameterTambahan.getHanyaTampilDiAdmin());
					kodeAdminYgBoleh.setVisible(parameterTambahan.getHanyaTampilDiAdmin());
				}
			});
			label.setParent(vbox);
			kodeAdminYgBoleh.setParent(vbox);

			kodeAdminYgBoleh.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					parameterTambahan.setKodeAdminYgBoleh(kodeAdminYgBoleh.getValue().trim());
					Common.refreshSaveOrUpdate(parameterTambahan);
				}
			});

			label.setVisible(parameterTambahan.getHanyaTampilDiAdmin());
			kodeAdminYgBoleh.setVisible(parameterTambahan.getHanyaTampilDiAdmin());

			Vbox v = new Vbox();
			v.setParent(arg0);
			new Label(parameterTambahan.getGrupParameterTambahan() == null ? ""
					: parameterTambahan.getGrupParameterTambahan().getNama()).setParent(v);

			new Label(parameterTambahan.getParent() == null ? "" : "Induk: " + parameterTambahan.getParent().getNama())
					.setParent(v);

			new MyLabelAgakKecil(parameterTambahan.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, parameterTambahan, ParameterTambahanAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ParameterTambahan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		parameterTambahan = (ParameterTambahan) obj;
		init(parameterTambahan);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menambah satu BARIS syarat tampil (Parameter Acuan + Nilai + tombol hapus) ke {@code container}.
	 * Dipakai membangun daftar syarat yang JUMLAHNYA TAK TERBATAS; dikumpulkan menjadi JSON saat simpan.
	 */
	private void tambahBarisSyarat(final Vbox container, ParameterTambahan acuan, String nilai) {
		final Hbox baris = new Hbox();
		baris.setWidth("100%");
		baris.setStyle("margin-bottom:4px;");
		// Simpan parameter acuan pada BARIS (dipilih via modal AmbilDataParameterTambahanBanyak).
		baris.setAttribute("acuan", acuan);

		// Nama parameter acuan ditampilkan read-only (bukan picker inline lagi).
		MyTextbox txtAcuan = new MyTextbox(acuan == null ? "" : (acuan.getKode() + " - " + acuan.getNama()));
		txtAcuan.setWidth("45%");
		txtAcuan.setReadonly(true);
		baris.appendChild(txtAcuan);

		MyTextbox txtNilai = new MyTextbox(nilai == null ? "" : nilai);
		txtNilai.setWidth("35%");
		baris.appendChild(txtNilai);

		MyToolbarbuttonConfig btnHapus = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		btnHapus.setTooltiptext("Hapus syarat ini");
		btnHapus.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				baris.detach();
			}
		});
		baris.appendChild(btnHapus);

		baris.setParent(container);
	}

	private void init(ParameterTambahan parameterTambahan) throws Exception {
		if (parameterTambahan.getGrupParameterTambahan() == null && searchgrup.getSelectedItem() != null
				&& searchgrup.getSelectedItem().getValue() != null) {
			parameterTambahan.setGrupParameterTambahan((GrupParameterTambahan) searchgrup.getSelectedItem().getValue());
		}

		this.parameterTambahan = parameterTambahan;
		addWindow.setTitle(parameterTambahan.getId() == null ? "Tambah Parameter" : "Ubah Parameter");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Parameter"));
		row.appendChild(kode = new Textbox(parameterTambahan.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Parameter *"));
		row.appendChild(labelInputan = new Textbox(parameterTambahan.getLabelInputan()));
		labelInputan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Menyertakan file lampiran"));
		row.appendChild(harusMenyertakanLampiran = new MyCheckboxConfig());
		harusMenyertakanLampiran.setChecked(parameterTambahan.getHarusMenyertakanLampiran());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan isian keterangan"));
		row.appendChild(tampilkanIsianKeterangan = new MyCheckboxConfig());
		tampilkanIsianKeterangan.setChecked(parameterTambahan.getTampilkanIsianKeterangan());

		final MyFormRow rowlabelInputanKeterangan = new MyFormRow();
		rowlabelInputanKeterangan.setStyle("border:0px;background: transparent;");
		rowlabelInputanKeterangan.setVisible(tampilkanIsianKeterangan.isChecked());
		rowlabelInputanKeterangan.setParent(rows);
		rowlabelInputanKeterangan.appendChild(new ais.ui.util.MyLabelConfig("Label isian keterangan"));
		rowlabelInputanKeterangan
				.appendChild(labelInputanKeterangan = new MyTextbox(parameterTambahan.getLabelInputanKeterangan()));
		labelInputanKeterangan.setWidth("90%");
		tampilkanIsianKeterangan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowlabelInputanKeterangan.setVisible(tampilkanIsianKeterangan.isChecked());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Data Inputan *"));
		row.appendChild(tipeDataInputan = new Combobox());
		tipeDataInputan.setWidth("90%");
		tipeDataInputan.setReadonly(true);
		MyComboitemConfig comboitem = new MyComboitemConfig(ParameterTambahan.TIDAK_ADA);
		comboitem.setValue(ParameterTambahan.TIDAK_ADA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TEXT);
		comboitem.setValue(ParameterTambahan.TEXT);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.ANGKA);
		comboitem.setValue(ParameterTambahan.ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TEXT_ANGKA);
		comboitem.setValue(ParameterTambahan.TEXT_ANGKA);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TANGGAL);
		comboitem.setValue(ParameterTambahan.TANGGAL);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.TANGGAL_DAN_WAKTU);
		comboitem.setValue(ParameterTambahan.TANGGAL_DAN_WAKTU);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.WAKTU);
		comboitem.setValue(ParameterTambahan.WAKTU);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_YA_TIDAK);
		comboitem.setValue(ParameterTambahan.PILIHAN_YA_TIDAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_CUSTOM);
		comboitem.setValue(ParameterTambahan.PILIHAN_CUSTOM);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_BANYAK);
		comboitem.setValue(ParameterTambahan.PILIHAN_BANYAK);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_MATRIX);
		comboitem.setValue(ParameterTambahan.PILIHAN_MATRIX);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_MATRIX_BANYAK_NILAI);
		comboitem.setValue(ParameterTambahan.PILIHAN_MATRIX_BANYAK_NILAI);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_MATRIX_BANYAK_COMBO);
		comboitem.setValue(ParameterTambahan.PILIHAN_MATRIX_BANYAK_COMBO);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_OBJECT);
		comboitem.setValue(ParameterTambahan.PILIHAN_OBJECT);
		tipeDataInputan.appendChild(comboitem);

		if (pt) {
			comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_MAHASISWA);
			comboitem.setValue(ParameterTambahan.PILIHAN_MAHASISWA);
			tipeDataInputan.appendChild(comboitem);
		}

		if (ya) {
			comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_SISWA);
			comboitem.setValue(ParameterTambahan.PILIHAN_SISWA);
			tipeDataInputan.appendChild(comboitem);

			comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_KELAS_SISWA);
			comboitem.setValue(ParameterTambahan.PILIHAN_KELAS_SISWA);
			tipeDataInputan.appendChild(comboitem);
		}

		if (pt) {
			comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_DOSEN);
			comboitem.setValue(ParameterTambahan.PILIHAN_DOSEN);
			tipeDataInputan.appendChild(comboitem);
		}

		if (ya) {
			comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_GURU);
			comboitem.setValue(ParameterTambahan.PILIHAN_GURU);
			tipeDataInputan.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_PEGAWAI);
		comboitem.setValue(ParameterTambahan.PILIHAN_PEGAWAI);
		tipeDataInputan.appendChild(comboitem);

		comboitem = new MyComboitemConfig(ParameterTambahan.PILIHAN_PENYEDIA);
		comboitem.setValue(ParameterTambahan.PILIHAN_PENYEDIA);
		tipeDataInputan.appendChild(comboitem);

		Common.selectComboItem(tipeDataInputan, parameterTambahan.getTipeDataInputan());
		if (tipeDataInputan.getSelectedItem() == null) {
			tipeDataInputan.setSelectedIndex(0);
		}
		tipeDataInputan.setReadonly(true);

		final MyFormRow rowNilaiInputan = new MyFormRow();
		rowNilaiInputan.setStyle("border:0px;background: transparent;");
		rowNilaiInputan.setParent(rows);
		rowNilaiInputan.appendChild(new ais.ui.util.MyLabelConfig("Nilai Data Inputan"));
		rowNilaiInputan.appendChild(nilaiDataInputan = new Textbox(parameterTambahan.getNilaiDataInputan()));
		nilaiDataInputan.setWidth("90%");
		nilaiDataInputan.setRows(5);

		final MyFormRow rowNilaiInputanObject = new MyFormRow();

		rowNilaiInputanObject.setStyle("border:0px;background: transparent;");
		rowNilaiInputanObject.setParent(rows);
		rowNilaiInputanObject.appendChild(new ais.ui.util.MyLabelConfig("Nilai Data Inputan"));
		rowNilaiInputanObject.appendChild(rowNilaiInputanComboObject = new Combobox());
		rowNilaiInputanComboObject.setWidth("90%");
		rowNilaiInputanComboObject.setReadonly(false);

		Reflections reflections = new Reflections("ais.database.model");
		Set<Class<? extends GeneralValueObject>> allClasses = reflections.getSubTypesOf(GeneralValueObject.class);

		TreeMap<String, String> treeMap = new TreeMap<String, String>();
		for (Class<? extends GeneralValueObject> c : allClasses) {
			treeMap.put(c.getName(), c.getSimpleName());
		}

		for (String c : treeMap.keySet()) {
			if (!StringUtils.contains(c, ".file.")) {
				Comboitem comboitemData = new Comboitem(treeMap.get(c));
				comboitemData.setDescription(c);
				comboitemData.setValue(c);
				rowNilaiInputanComboObject.appendChild(comboitemData);
			}
		}
		allClasses = null;
		treeMap = null;

		String v = (String) (tipeDataInputan.getSelectedItem() == null ? null
				: tipeDataInputan.getSelectedItem().getValue());
		if (v != null && (v.equals(ParameterTambahan.PILIHAN_OBJECT))) {
			rowNilaiInputanObject.setVisible(true);
			Common.selectComboItem(rowNilaiInputanComboObject, parameterTambahan.getNilaiDataInputan());
		} else {
			rowNilaiInputanObject.setVisible(false);
		}

		final Row rowKeteranganNilaiInputan = Common.initKeterangan(rows,
				"Input nilai custom dan banyak harus diberi pemisah semicolon (;) dan untuk skor dipisah dengan kolon (:), skor harus berupa angka desimal, contoh : Ya:1;Tidak:0;Belum Tau:2");

		final Row rowKeteranganNilaiMatrix = Common.initKeterangan(rows,
				"Input nilai matrix harus diberi pemisah semicolon (;) dan untuk skor dipisah dengan kolon (:), skor harus berupa angka desimal, kemudian untuk matrix selanjutnya harus diawalai dengan {nama_ROW} dan tanda -> lalu di akhiri dengan turun ke bawah / ENTER, contoh : F1->Ya:1;Tidak:0;Belum Tau:2<TEKAN ENTER>F2->Ya:1;Tidak:0;Belum Tau:2<TEKAN ENTER>F3->Ya:1;Tidak:0;Belum Tau:2");

		final Row rowKeteranganNilaiObject = Common.initKeterangan(rows,
				"Input nilai data mengambil dari class Data di sistem, contoh : " + Agama.class.getName() + " atau "
						+ TipeMasaKerja.class.getName() + " dan lain-lain");

		final MyFormRow rowKondisiDataInputan = new MyFormRow();
		rowKondisiDataInputan.setStyle("border:0px;background: transparent;");
		rowKondisiDataInputan.setParent(rows);
		rowKondisiDataInputan.appendChild(new ais.ui.util.MyLabelConfig("Kondisi Data Inputan / Where"));
		rowKondisiDataInputan
				.appendChild(kondisiDataInputan = new MyTextbox(parameterTambahan.getKondisiDataInputan()));

		final MyFormRow rowJumlahBaris = new MyFormRow();
		rowJumlahBaris.setStyle("border:0px;background: transparent;");
		rowJumlahBaris.setParent(rows);
		rowJumlahBaris.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Baris"));
		rowJumlahBaris.appendChild(jumlahBaris = new MyIntbox(parameterTambahan.getJumlahBaris()));

		final MyFormRow rowJumlahText = new MyFormRow();
		rowJumlahText.setStyle("border:0px;background: transparent;");
		rowJumlahText.setParent(rows);
		rowJumlahText.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Maksimal Teks"));
		rowJumlahText.appendChild(jumlahText = new MyIntbox(parameterTambahan.getJumlahText()));

		final MyFormRow rowNilaiMax = new MyFormRow();
		rowNilaiMax.setStyle("border:0px;background: transparent;");
		rowNilaiMax.setParent(rows);
		rowNilaiMax.appendChild(new ais.ui.util.MyLabelConfig("Nilai Maksimal"));
		rowNilaiMax.appendChild(nilaiMax = new MyDoublebox(parameterTambahan.getNilaiMax()));

		final MyFormRow rowNilaiMin = new MyFormRow();
		rowNilaiMin.setStyle("border:0px;background: transparent;");
		rowNilaiMin.setParent(rows);
		rowNilaiMin.appendChild(new ais.ui.util.MyLabelConfig("Nilai Minimal"));
		rowNilaiMin.appendChild(nilaiMin = new MyDoublebox(parameterTambahan.getNilaiMin()));

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String v = (String) (tipeDataInputan.getSelectedItem() == null ? null
						: tipeDataInputan.getSelectedItem().getValue());

				rowNilaiMax.setVisible(v != null && (v.equals(ParameterTambahan.ANGKA)));
				rowNilaiMin.setVisible(v != null && (v.equals(ParameterTambahan.ANGKA)));

				rowNilaiInputan.setVisible(v != null
						&& (v.equals(ParameterTambahan.PILIHAN_CUSTOM) || v.equals(ParameterTambahan.PILIHAN_MATRIX)
								|| v.equals(ParameterTambahan.PILIHAN_MATRIX_BANYAK_NILAI)
								|| v.equals(ParameterTambahan.PILIHAN_MATRIX_BANYAK_COMBO)
								|| v.equals(ParameterTambahan.PILIHAN_BANYAK)));
				rowKeteranganNilaiInputan.setVisible(v != null
						&& (v.equals(ParameterTambahan.PILIHAN_CUSTOM) || v.equals(ParameterTambahan.PILIHAN_BANYAK)));
				rowKeteranganNilaiMatrix.setVisible(v != null && (v.equals(ParameterTambahan.PILIHAN_MATRIX)
						|| v.equals(ParameterTambahan.PILIHAN_MATRIX_BANYAK_COMBO)
						|| v.equals(ParameterTambahan.PILIHAN_MATRIX_BANYAK_NILAI)));

				rowKeteranganNilaiObject.setVisible(v != null && (v.equals(ParameterTambahan.PILIHAN_OBJECT)));

				rowNilaiInputanObject.setVisible(v != null && (v.equals(ParameterTambahan.PILIHAN_OBJECT)));
				rowKondisiDataInputan.setVisible(v != null && (v.equals(ParameterTambahan.PILIHAN_OBJECT)));

				rowJumlahBaris.setVisible(v != null && v.equals(ParameterTambahan.TEXT));
				rowJumlahText.setVisible(v != null && v.equals(ParameterTambahan.TEXT));

			}
		};
		eventListener.onEvent(null);
		tipeDataInputan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(nilaiTidakBolehDiubah = new MyCheckboxConfig("Nilai tidak boleh diubah"));
		nilaiTidakBolehDiubah.setChecked(parameterTambahan.getNilaiTidakBolehDiubah());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai default"));
		row.appendChild(nilaiDefault = new MyTextbox(parameterTambahan.getNilaiDefault()));
		nilaiDefault.setWidth("90%");
		nilaiDefault.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Parameter"));
		row.appendChild(grupParameterTambahan = new Combobox());
		grupParameterTambahan.setWidth("90%");
		Common.insertComboDanSemua(grupParameterTambahan, "nama", GrupParameterTambahan.class);
		Common.selectComboItem(grupParameterTambahan, parameterTambahan.getGrupParameterTambahan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Induk Parameter"));
		row.appendChild(parent = new AmbilDataParameterTambahanBanbox());
		parent.setWidth("90%");
		parent.setAttribute("parameterTambahan", parameterTambahan.getParent());

		Common.initKeterangan(rows, "Kosongkan induk jika parameter ini tidak menginduk kepada parameter lain");

		// SYARAT TAMPIL (conditional display) berbasis JSON -- DUKUNG BANYAK syarat (parameter acuan tak
		// terbatas) + logika AND/OR. Parameter ini tampil bila syarat terpenuhi.
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Syarat Tampil (Logika)"));
		syaratLogika = new Combobox();
		syaratLogika.setReadonly(true);
		syaratLogika.setWidth("220px");
		MyComboitemConfig ciLogika = new MyComboitemConfig("Semua terpenuhi (AND)");
		ciLogika.setValue("AND");
		syaratLogika.appendChild(ciLogika);
		ciLogika = new MyComboitemConfig("Salah satu terpenuhi (OR)");
		ciLogika.setValue("OR");
		syaratLogika.appendChild(ciLogika);
		row.appendChild(syaratLogika);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		Vbox boxSyarat = new Vbox();
		boxSyarat.setWidth("100%");
		row.appendChild(boxSyarat);
		syaratContainer = new Vbox();
		syaratContainer.setWidth("100%");
		boxSyarat.appendChild(syaratContainer);
		MyToolbarbuttonConfig btnTambahSyarat = new MyToolbarbuttonConfig("Pilih Parameter Acuan", "/img/svg/plus-circle.svg");
		btnTambahSyarat.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				// Kumpulkan acuan yang SUDAH dipilih agar dikunci di modal (cegah duplikat).
				java.util.List<ParameterTambahan> sudahDipilih = new java.util.ArrayList<ParameterTambahan>();
				for (Object o : syaratContainer.getChildren()) {
					if (o instanceof Hbox) {
						Object a = ((Hbox) o).getAttribute("acuan");
						if (a instanceof ParameterTambahan) {
							sudahDipilih.add((ParameterTambahan) a);
						}
					}
				}
				// Buka modal multi-pilih standar untuk memilih parameter acuan (BANYAK sekaligus).
				final ais.action.master.helper.generic.AmbilDataParameterTambahanBanyak window =
						new ais.action.master.helper.generic.AmbilDataParameterTambahanBanyak(sudahDipilih);
				org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("90%");
				window.setEventListener(new EventListener() {
					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event ev) throws Exception {
						java.util.List<ParameterTambahan> dipilih = (java.util.List<ParameterTambahan>) ev.getData();
						if (dipilih != null) {
							for (ParameterTambahan pt : dipilih) {
								if (pt != null) {
									tambahBarisSyarat(syaratContainer, pt, "");
								}
							}
						}
					}
				});
				window.onModal();
			}
		});
		boxSyarat.appendChild(btnTambahSyarat);

		Common.initKeterangan(rows,
				"Parameter ini TAMPIL bila syarat terpenuhi (AND = semua syarat, OR = salah satu). Tiap syarat = "
						+ "jawaban 'Parameter Acuan' sama dengan 'Nilai'. Tanpa syarat = selalu tampil.");

		// Prefill baris syarat dari JSON tersimpan.
		try {
			String jsonSyarat = parameterTambahan.getSyaratTampil();
			if (jsonSyarat != null && !jsonSyarat.trim().isEmpty()) {
				org.json.JSONObject objSyarat = new org.json.JSONObject(jsonSyarat.trim());
				Common.selectComboItem(syaratLogika, objSyarat.optString("logika", "AND"));
				org.json.JSONArray arrSyarat = objSyarat.optJSONArray("syarat");
				if (arrSyarat != null) {
					for (int i = 0; i < arrSyarat.length(); i++) {
						org.json.JSONObject c = arrSyarat.optJSONObject(i);
						if (c != null) {
							long pid = c.optLong("parameterId", 0L);
							ParameterTambahan acuanPt = null;
							if (pid != 0L) {
								Object oAcuan = ais.common.ConstantValues.ambilBerdasarClass(ParameterTambahan.class)
										.get(Long.valueOf(pid));
								if (oAcuan instanceof ParameterTambahan) {
									acuanPt = (ParameterTambahan) oAcuan;
								}
							}
							if (acuanPt != null) {
								tambahBarisSyarat(syaratContainer, acuanPt, c.optString("nilai", ""));
							}
						}
					}
				}
			}
		} catch (Exception ig) {
			Common.tampilErrorJikaAdmin(ig);
		}
		if (syaratLogika.getSelectedItem() == null) {
			syaratLogika.setSelectedIndex(0);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				parameterTambahan.getKeterangan() == null ? "" : parameterTambahan.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		Tbmuser tbmuser1 = Common.getCurrentUser();

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				parameterTambahan.getFakultas() == null ? tbmuser1.ambilFakultas() : parameterTambahan.getFakultas());
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
				parameterTambahan.getJurusan() == null ? tbmuser1.ambilJurusan() : parameterTambahan.getJurusan());
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
				parameterTambahan == null || parameterTambahan.getYayasan() == null ? tbmuser1.ambilYayasan()
						: parameterTambahan.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				parameterTambahan == null || parameterTambahan.getSekolah() == null ? tbmuser1.ambilSekolah()
						: parameterTambahan.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Parameter"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, parameterTambahan.getId(), ParameterTambahan.class.getName(),
				"Lampiran Parameter", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran lebih dari satu file, zip dulu semua file tersebut");

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
		if (labelInputan.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Parameter",
					"Kolom Nama Parameter belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Parameter.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		String v = (String) (tipeDataInputan.getSelectedItem() == null ? null
				: tipeDataInputan.getSelectedItem().getValue());

		String c = (String) (rowNilaiInputanComboObject.getSelectedItem() == null ? null
				: rowNilaiInputanComboObject.getSelectedItem().getValue());

		if (v != null && (v.equals(ParameterTambahan.PILIHAN_OBJECT))) {
			if (c == null || c.isEmpty()) {
				MyMessageboxConfig.show("Untuk jenis pilihan data, nilai inputan harus dipilih.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		if (parameterTambahan.getId() != null) {
			parameterTambahan = (ParameterTambahan) session.load(ParameterTambahan.class, parameterTambahan.getId());

		}
		parameterTambahan.setKode(kode.getValue().trim());

		if (v != null && (v.equals(ParameterTambahan.PILIHAN_OBJECT))) {
			parameterTambahan.setNilaiDataInputan(c);
		} else {
			parameterTambahan.setNilaiDataInputan(nilaiDataInputan.getValue().trim());
		}

		parameterTambahan.setNama(labelInputan.getValue().trim());
		parameterTambahan.setHarusMenyertakanLampiran(harusMenyertakanLampiran.isChecked());
		parameterTambahan.setTipeDataInputan((String) tipeDataInputan.getSelectedItem().getValue());
		parameterTambahan.setLabelInputan(labelInputan.getValue().trim());
		parameterTambahan.setKeterangan(keterangan.getValue());
		parameterTambahan
				.setGrupParameterTambahan((GrupParameterTambahan) (grupParameterTambahan.getSelectedItem() == null ? ""
						: grupParameterTambahan.getSelectedItem().getValue()));
		parameterTambahan.setTampilkanIsianKeterangan(tampilkanIsianKeterangan.isChecked());
		parameterTambahan.setLabelInputanKeterangan(labelInputanKeterangan.getValue());
		parameterTambahan.setJumlahBaris(jumlahBaris.getValue());
		parameterTambahan.setJumlahText(jumlahText.getValue());
		parameterTambahan.setParent((ParameterTambahan) parent.getAttribute("parameterTambahan"));
		// Bangun JSON SYARAT TAMPIL dari baris-baris (banyak syarat).
		try {
			org.json.JSONArray arrSyarat = new org.json.JSONArray();
			if (syaratContainer != null) {
				for (Object o : syaratContainer.getChildren()) {
					if (!(o instanceof Hbox)) {
						continue;
					}
					Hbox baris = (Hbox) o;
					ParameterTambahan acuan = null;
					String nilai = "";
					Object acuanAttr = baris.getAttribute("acuan");
					if (acuanAttr instanceof ParameterTambahan) {
						acuan = (ParameterTambahan) acuanAttr;
					}
					for (Object w : baris.getChildren()) {
						if (w instanceof MyTextbox && !((MyTextbox) w).isReadonly()) {
							nilai = ((MyTextbox) w).getValue();
						}
					}
					if (acuan != null && acuan.getId() != null && nilai != null && !nilai.trim().isEmpty()) {
						org.json.JSONObject cSyarat = new org.json.JSONObject();
						cSyarat.put("parameterId", acuan.getId());
						cSyarat.put("nilai", nilai.trim());
						arrSyarat.put(cSyarat);
					}
				}
			}
			if (arrSyarat.length() == 0) {
				parameterTambahan.setSyaratTampil(null);
			} else {
				org.json.JSONObject objSyarat = new org.json.JSONObject();
				objSyarat.put("logika", syaratLogika != null && syaratLogika.getSelectedItem() != null
						? syaratLogika.getSelectedItem().getValue() : "AND");
				objSyarat.put("syarat", arrSyarat);
				parameterTambahan.setSyaratTampil(objSyarat.toString());
			}
		} catch (Exception exSyarat) {
			Common.tampilErrorJikaAdmin(exSyarat);
		}

		parameterTambahan.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		parameterTambahan.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		parameterTambahan.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		parameterTambahan.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		parameterTambahan.setKondisiDataInputan(kondisiDataInputan.getValue().trim());

		parameterTambahan.setNilaiMax(nilaiMax.getValue());
		parameterTambahan.setNilaiMin(nilaiMin.getValue());

		parameterTambahan.setNilaiDefault(nilaiDefault.getValue().trim());
		parameterTambahan.setNilaiTidakBolehDiubah(nilaiTidakBolehDiubah.isChecked());

		Common.refreshSaveOrUpdate(session, parameterTambahan);

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(parameterTambahan.getId());

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
		Criteria criteria = session.createCriteria(ParameterTambahan.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nomorUrut"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") :

				Restrictions.or(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchtipe.getSelectedItem() == null || searchtipe.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tipeDataInputan", searchtipe.getSelectedItem().getValue()))

				.add(searchgrup.getSelectedItem() == null || searchgrup.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("grupParameterTambahan", searchgrup.getSelectedItem().getValue()))

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
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ParameterTambahan> parameterTambahan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(parameterTambahan);
		grid.setRowRenderer(new ParameterTambahanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
