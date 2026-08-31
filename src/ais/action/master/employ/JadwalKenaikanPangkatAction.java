package ais.action.master.employ;

import java.util.Calendar;
import java.util.Date;
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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.AmbilDataGolonganBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.KenaikanPangkatDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Jabatan;
import ais.database.model.Pegawai;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Peraturan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jadwal kenaikan pangkat. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataPegawaiBanbox ambilDataPegawaiBanbox}, {@code Textbox
 * searchpegawai}, {@code Combobox searchstatus}, {@code Textbox namaPejabat}, {@code Textbox
 * nomorSuratkeputusan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code
 * init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
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
public class JadwalKenaikanPangkatAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox();
	// private AmbilDataPegawaiBanbox searchpegawai;
	private Textbox searchpegawai;
	private Combobox searchstatus;

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

	private boolean edit = false;
	// private boolean delete = false;

	private KenaikanPangkat kenaikanPangkat;
	private MyToolbarbuttonConfig add;

	private Pegawai pegawai;
	private MyCheckboxConfig status;
	private MyDatebox mulaiDatebox;
	private MyDatebox sampaiDatebox;

	Calendar searchbawah;
	Calendar searchAtas;
	private AmbilDataGolonganBanbox golongan;
	protected LampiranLain lainMahasiswa;

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

		searchbawah = ais.ui.util.WaktuUtil.getCalendar();
		searchbawah.set(Calendar.MONTH, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) - 1);
		searchAtas = ais.ui.util.WaktuUtil.getCalendar();
		if (mulaiDatebox != null) { mulaiDatebox.setValue(searchbawah.getTime()); }
		if (sampaiDatebox != null) { sampaiDatebox.setValue(searchAtas.getTime()); }

		// searchpegawai.setEventListener(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// onSearchDefault(null);
		// }
		// });

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

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

			new Label(jabatan).setParent(arg0);

			String gaji = (kenaikanPangkat.getGolongan() == null ? "" : kenaikanPangkat.getGolongan().toString());
			if (kenaikanPangkat.getGajiPokok() != null) {
				gaji = kenaikanPangkat.getGajiPokok().toString();
			}

			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + gaji + "</font>").setParent(arg0);

			new Label((kenaikanPangkat.getMulai() == null ? "" : Common.dateFormat1.get().format(kenaikanPangkat.getMulai()))
					+ " s.d " + (kenaikanPangkat.getSampai() == null ? ""
							: Common.dateFormat1.get().format(kenaikanPangkat.getSampai())))
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/info.gif");
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
			button.setVisible(false);
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
											KenaikanPangkatDao kenaikanPangkatDao = DaoFactory.getInstance()
													.getKenaikanPangkatDao();
											// peraturanDao.beginTransaction();
											kenaikanPangkatDao.delete((kenaikanPangkat));
											// peraturanDao.commitTransaction();
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
		addWindow.setTitle("Informasi Kenaikan Pangkat");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		final MyGrid grid = new MyGrid();
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
		Boolean disabled = true;

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				kenaikanPangkat.getPegawai().getCode() + " - " + kenaikanPangkat.getPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(
				kenaikanJabatan = new MyCheckboxConfig("Merupakan perubahan jabatan fungsional atau struktural"));
		kenaikanJabatan.setChecked(kenaikanPangkat.getKenaikanJabatan());
		kenaikanJabatan.setDisabled(disabled);

		final MyFormRow jenisjabatanrow = new MyFormRow();
		jenisjabatanrow.setVisible(false);
		jenisjabatanrow.setParent(rows);
		jenisjabatanrow.appendChild(new MyLabelConfig("Jenis Jabatan"));
		jenis = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
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
		jenisjabatanrow.appendChild(jenis);
		jenis.setWidth("90%");
		jenis.setDisabled(disabled);
		jenis.setReadonly(true);

		final MyFormRow jabatanrow = new MyFormRow();
		jabatanrow.setVisible(false);
		jabatanrow.setParent(rows);
		jabatanrow.appendChild(new MyLabelConfig("Jabatan"));
		Common.insertCombo(jabatan = new Combobox(), "nama", Jabatan.class);
		Common.selectComboItem(jabatan, kenaikanPangkat.getJabatan());
		jabatanrow.appendChild(jabatan);
		jabatan.setWidth("90%");
		jabatan.setDisabled(disabled);
		jabatan.setReadonly(true);

		final MyFormRow jabatanfungsionalrow = new MyFormRow();
		jabatanfungsionalrow.setVisible(false);
		jabatanfungsionalrow.setParent(rows);
		jabatanfungsionalrow.appendChild(new MyLabelConfig("Jabatan Fungsional"));
		Common.insertComboDanSemua(jabatanFungsional = new Combobox(), new String[] { "kode", "nama" }, "keterangan",
				JabatanFungsional.class, "=Jabatan Fungsional=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jabatanFungsional, kenaikanPangkat.getJabatanFungsional());
		jabatanfungsionalrow.appendChild(jabatanFungsional);
		jabatanFungsional.setWidth("90%");
		jabatanFungsional.setDisabled(disabled);
		jabatanFungsional.setReadonly(true);

		final MyFormRow jabatanstrukturalrow = new MyFormRow();
		jabatanstrukturalrow.setVisible(false);
		jabatanstrukturalrow.setParent(rows);
		jabatanstrukturalrow.appendChild(new MyLabelConfig("Jabatan Struktural"));
		Common.insertComboDanSemua(jabatanStruktural = new Combobox(), new String[] { "kode", "nama" }, "keterangan",
				JabatanStruktural.class, "=Jabatan Struktural=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jabatanStruktural, kenaikanPangkat.getJabatanStruktural());
		jabatanstrukturalrow.appendChild(jabatanStruktural);
		jabatanStruktural.setWidth("90%");
		jabatanStruktural.setDisabled(disabled);
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
		row.appendChild(noSuratUsul = new Textbox(
				kenaikanPangkat.getNoSuratUsul() == null ? "" : kenaikanPangkat.getNoSuratUsul()));
		noSuratUsul.setWidth("90%");
		noSuratUsul.setDisabled(disabled);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat Usul"));
		// row.appendChild(tanggalSuratUsul = new MyDatebox(kenaikanPangkat
		// .getTanggalSuratUsul() == null ? ais.ui.util.WaktuUtil.getDate() :
		// kenaikanPangkat
		// .getTanggalSuratUsul()));
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.dateFormat2.get()
				.format(kenaikanPangkat.getTanggalSuratUsul() == null ? ais.ui.util.WaktuUtil.getDate()
						: kenaikanPangkat.getTanggalSuratUsul())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peraturan"));
		row.appendChild(peraturan = new Combobox());
		Common.insertComboDanSemua(peraturan, new String[] { "nama", "kode" }, "keterangan", Peraturan.class,
				"== Tanpa Peraturan ==", Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(peraturan, kenaikanPangkat.getPeraturan());
		peraturan.setWidth("90%");
		peraturan.setDisabled(disabled);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Golongan"));
		row.appendChild(golongan = new AmbilDataGolonganBanbox());
		golongan.setValue(kenaikanPangkat.getGolongan()==null?"":kenaikanPangkat.getGolongan().getNama()); 
		golongan.setAttribute("golongan", kenaikanPangkat.getGolongan());
		golongan.setWidth("90%");
		golongan.setReadonly(true);


		EventListener jenisjabatanEventListener = new EventListener() {

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
		kenaikanJabatan.setDisabled(disabled);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Surat Keputusan"));
		row.appendChild(nomorSuratkeputusan = new Textbox(kenaikanPangkat.getNomorSuratkeputusan()));
		nomorSuratkeputusan.setWidth("90%");
		nomorSuratkeputusan.setDisabled(disabled);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat Keputusan"));
		row.appendChild(tanggalSuratkeputusan = new MyDatebox(kenaikanPangkat.getTanggalSuratkeputusan()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(menjabat = new MyCheckboxConfig("Jabatan atau golongan ini sedang aktif / dijabat"));
		menjabat.setChecked(kenaikanPangkat.getMenjabat());
		menjabat.setDisabled(disabled);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai menjabat"));
		row.appendChild(mulai = new MyDatebox(kenaikanPangkat.getMulai()));
		mulai.setDisabled(disabled);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai menjabat"));
		row.appendChild(sampai = new MyDatebox(kenaikanPangkat.getSampai()));
		sampai.setDisabled(disabled);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pejabat"));
		row.appendChild(namaPejabat = new Textbox(kenaikanPangkat.getNamaPejabat()));
		namaPejabat.setWidth("90%");
		namaPejabat.setDisabled(disabled);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kenaikanPangkat.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setDisabled(disabled);

		row = new MyFormRow();
		row.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));
		row.appendChild(status = new MyCheckboxConfig());
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
		status.setDisabled(disabled);

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
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran dokumen lebih dari satu file, zip dulu semua file tersebut");

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
		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian atau tombol cari pegawai; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

//		if (peraturan.getSelectedItem() == null) {
//			MyMessageboxConfig.show("Peraturan harus dipilih", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}
//
//		if (mulai.getValue() == null) {
//			MyMessageboxConfig.show("Mulai menjabat harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}

		KenaikanPangkatDao kenaikanPangkatDao = DaoFactory.getInstance().getKenaikanPangkatDao();
		if (kenaikanPangkat.getId() != null) {
			kenaikanPangkat = kenaikanPangkatDao.load(kenaikanPangkat.getId());
		}

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

kenaikanPangkat.setGolongan(
				(Golongan) (golongan.getAttribute("golongan")));

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

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				Pegawai pegawai = kenaikanPangkat.getPegawai();
				session.refresh(pegawai);
				Common.refreshUpdate(session, pegawai, true);
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Date valueMulai = mulaiDatebox.getValue();
		Calendar vMulai = ais.ui.util.WaktuUtil.getCalendar();
		vMulai.setTime(valueMulai);
		Calendar bawah = ais.ui.util.WaktuUtil.getCalendar();
		bawah.setTime(valueMulai);
		bawah.set(Calendar.YEAR, vMulai.get(Calendar.YEAR) - 4);

		Date valueSampai = sampaiDatebox.getValue();
		Calendar vSampai = ais.ui.util.WaktuUtil.getCalendar();
		vSampai.setTime(valueSampai);
		Calendar atas = ais.ui.util.WaktuUtil.getCalendar();
		atas.setTime(valueSampai);
		atas.set(Calendar.YEAR, vSampai.get(Calendar.YEAR) - 4);
		System.out.println(Common.dateFormat2.get().format(bawah.getTime()) + "dari");
		System.out.println(Common.dateFormat2.get().format(atas.getTime()) + "sampai");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KenaikanPangkat.class);
		if (order)
			criteria
					// .addOrder(Order.desc("tanggalSuratkeputusan"))
					// .addOrder(Order.desc("tanggalSuratUsul"))
					.addOrder(Order.desc("mulai")).addOrder(Order.asc("pegawai"));
		// criteria.add(searchstatus.getSelectedItem() == null ||
		// searchstatus.getSelectedItem().getValue() == null ? Restrictions
		// .sqlRestriction("1=1") : Restrictions.eq("status", searchstatus
		// .getSelectedItem().getValue()));
		// criteria.add(Restrictions.isNull("jabatanFungsional"));
		// criteria.add(Restrictions.isNull("jabatanStruktural"));
		criteria.add(Restrictions.eq("kenaikanPangkatGolongan", true));
		criteria.add(Restrictions.between("tmt", bawah.getTime(), atas.getTime()));
		criteria.createCriteria("pegawai")
				.add(Restrictions.ilike("nama", searchpegawai.getValue().trim(), MatchMode.ANYWHERE));

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

}