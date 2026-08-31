package ais.action.master.employ;

import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.RiwayatStatusKepegawaianDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.RiwayatStatusKepegawaian;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk riwayat status kepegawaian. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code AmbilDataPegawaiBanbox ambilDataPegawaiBanbox}, {@code
 * AmbilDataPegawaiBanbox searchpegawai}, {@code AmbilDataSatuanKerjaBanbox searchparent}, {@code Radiogroup
 * statusKepegawaian}, {@code MyRadioConfig cpns}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class RiwayatStatusKepegawaianAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox();
	private AmbilDataPegawaiBanbox searchpegawai;

	private AmbilDataSatuanKerjaBanbox searchparent;

	// private Textbox searchnama;
	// private Combobox pegawai;
	private Radiogroup statusKepegawaian;
	private MyRadioConfig cpns;
	private MyRadioConfig pns;
	private Textbox jenisPegawai;
	private Textbox noSK;
	private MyDatebox tanggalSK;
	private MyDatebox tmt;
	private Combobox golongan;
	private Combobox jabatanStruktural;
	private Combobox jabatanFungsional;

	private boolean edit = false;
	private boolean delete = false;

	private RiwayatStatusKepegawaian riwayatStatusKepegawaian;
	private MyToolbarbuttonConfig add;
	protected LampiranLain lainMahasiswa;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		Common.insertCombo(golongan, "nama", Golongan.class, Restrictions.eq("aktif", true));
		Common.insertCombo(jabatanStruktural, "nama", JabatanStruktural.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		// Common.insertCombo(jabatanFungsional, "nama",
		// JabatanFungsional.class);
		// Common.insertCombo(pegawai, "nama", Pegawai.class);
		statusKepegawaian = new Radiogroup();
		cpns = new MyRadioConfig(RiwayatStatusKepegawaian.CPNS);
		if (cpns != null) { cpns.setValue(RiwayatStatusKepegawaian.CPNS); }
		if (cpns != null) { cpns.setAttribute("status", RiwayatStatusKepegawaian.CPNS); }
		statusKepegawaian.appendChild(cpns);
		pns = new MyRadioConfig(RiwayatStatusKepegawaian.PNS);
		if (pns != null) { pns.setValue(RiwayatStatusKepegawaian.PNS); }
		if (pns != null) { pns.setAttribute("status", RiwayatStatusKepegawaian.PNS); }
		statusKepegawaian.appendChild(pns);

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
	}

	/**
	 * Tipe implementasi bersarang {@link RiwayatStatusKepegawaianRender} milik {@link
	 * RiwayatStatusKepegawaianAction}. Kelas ini memberi nama pada state atau perilaku lokal agar tanggung
	 * jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link RiwayatStatusKepegawaianAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see RiwayatStatusKepegawaianAction
	 */
	class RiwayatStatusKepegawaianRender extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final RiwayatStatusKepegawaian riwayatStatusKepegawaian = (RiwayatStatusKepegawaian) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(RiwayatStatusKepegawaian.class, riwayatStatusKepegawaian,
					riwayatStatusKepegawaian.getPegawai().getNama() == null ? "-"
							: riwayatStatusKepegawaian.getPegawai().getNama()))
					.setParent(arg0);
			Hbox hbox = new Hbox();
			hbox.setParent(a);
			LampiranLain.createDownloadUploadFileLain(hbox, riwayatStatusKepegawaian.getId(),
					RiwayatStatusKepegawaian.class.getName(), "Dokumen", false, null, null, false, false, false, false);

			new Label(riwayatStatusKepegawaian.getStatusKepegawaian() == null ? ""
					: riwayatStatusKepegawaian.getStatusKepegawaian()).setParent(arg0);
			new Label(riwayatStatusKepegawaian.getJenisPegawai() == null ? ""
					: riwayatStatusKepegawaian.getJenisPegawai()).setParent(arg0);
			new Label(riwayatStatusKepegawaian.getNoSK() == null ? "" : riwayatStatusKepegawaian.getNoSK())
					.setParent(arg0);
			new Label(riwayatStatusKepegawaian.getTanggalSK() == null ? ""
					: Common.dateFormat1.get().format(riwayatStatusKepegawaian.getTanggalSK())).setParent(arg0);
			new Label(riwayatStatusKepegawaian.getGolongan().getNama() == null ? ""
					: riwayatStatusKepegawaian.getGolongan().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(riwayatStatusKepegawaian);
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
											RiwayatStatusKepegawaianDao riwayatStatusKepegawaianDao = DaoFactory
													.getInstance().getRiwayatStatusKepegawaianDao();

											riwayatStatusKepegawaianDao.delete((riwayatStatusKepegawaian));
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new RiwayatStatusKepegawaian());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(RiwayatStatusKepegawaian riwayatStatusKepegawaian) throws Exception {
		this.riwayatStatusKepegawaian = riwayatStatusKepegawaian;
		addWindow.setTitle("Riwayat Status Kepegawaian");
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
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(ambilDataPegawaiBanbox);
		ambilDataPegawaiBanbox.setValue(riwayatStatusKepegawaian.getPegawai() == null ? ""
				: riwayatStatusKepegawaian.getPegawai().getCode() + " - "
						+ riwayatStatusKepegawaian.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", riwayatStatusKepegawaian.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No SK"));
		row.appendChild(noSK = new Textbox(
				riwayatStatusKepegawaian.getNoSK() == null ? "" : riwayatStatusKepegawaian.getNoSK()));
		noSK.setWidth("90%");
		noSK.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal SK"));
		row.appendChild(tanggalSK = new MyDatebox(
				riwayatStatusKepegawaian.getTanggalSK() == null ? ais.ui.util.WaktuUtil.getDate()
						: riwayatStatusKepegawaian.getTanggalSK()));
		tanggalSK.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TMT *"));
		row.appendChild(tmt = new MyDatebox(riwayatStatusKepegawaian.getTmt() == null ? ais.ui.util.WaktuUtil.getDate()
				: riwayatStatusKepegawaian.getTmt()));
		tmt.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Kepegawaian"));
		row.appendChild(statusKepegawaian);
		statusKepegawaian.setSelectedItem(riwayatStatusKepegawaian.getStatusKepegawaian() == null ? null
				: riwayatStatusKepegawaian.getStatusKepegawaian().equals(RiwayatStatusKepegawaian.CPNS) ? cpns : pns);
		statusKepegawaian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pegawai *"));
		row.appendChild(jenisPegawai = new Textbox(
				riwayatStatusKepegawaian.getJenisPegawai() == null ? "" : riwayatStatusKepegawaian.getJenisPegawai()));
		jenisPegawai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Golongan *"));
		row.appendChild(golongan = new Combobox());
		Common.insertCombo(golongan, "nama", Golongan.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(golongan,
				riwayatStatusKepegawaian.getGolongan() == null ? "" : riwayatStatusKepegawaian.getGolongan());
		golongan.setWidth("90%");

		final MyFormRow jabatanfungsionalrow = new MyFormRow();
		jabatanfungsionalrow.setVisible(false);
		jabatanfungsionalrow.setParent(rows);
		jabatanfungsionalrow.appendChild(new MyLabelConfig("Jabatan Fungsional"));
		Common.insertComboDanSemua(jabatanFungsional = new Combobox(), new String[] { "kode", "nama" }, "keterangan",
				JabatanFungsional.class, "=Jabatan Fungsional=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jabatanFungsional, riwayatStatusKepegawaian.getJabatanFungsional());
		jabatanfungsionalrow.appendChild(jabatanFungsional);
		jabatanFungsional.setWidth("90%");
		jabatanFungsional.setReadonly(true);

		final MyFormRow jabatanstrukturalrow = new MyFormRow();
		jabatanstrukturalrow.setVisible(false);
		jabatanstrukturalrow.setParent(rows);
		jabatanstrukturalrow.appendChild(new MyLabelConfig("Jabatan Struktural"));
		Common.insertComboDanSemua(jabatanStruktural = new Combobox(), new String[] { "kode", "nama" }, "keterangan",
				JabatanStruktural.class, "=Jabatan Struktural=",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jabatanStruktural, riwayatStatusKepegawaian.getJabatanStruktural());
		jabatanstrukturalrow.appendChild(jabatanStruktural);
		jabatanStruktural.setWidth("90%");
		jabatanStruktural.setReadonly(true);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Dokumen"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, riwayatStatusKepegawaian.getId(),
				RiwayatStatusKepegawaian.class.getName(), "Dokumen", false, new EventListener() {

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
		// if (pegawai.getSelectedItem() == null) {
		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Pegawai Dipilih", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		if (golongan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Golongan belum dipilih. Langkah yang dapat dilakukan: (1) pilih Golongan dari dropdown; (2) pastikan data golongan sudah tersedia di master; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}

		// if (jabatanFungsional.getSelectedItem() == null) {
		// MyMessageboxConfig.show("jabatanFungsional Harus Dipilih",
		// MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
		// return false;
		// }
		//
		// if (jabatanStruktural.getSelectedItem() == null) {
		// MyMessageboxConfig.show("jabatanStruktural Harus Dipilih",
		// MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
		// return false;
		// }
//		if (statusKepegawaian.getSelectedItem().getValue() == null) {
//			MyMessageboxConfig.show("statusKepegawaian Harus Diisi", MyMessageboxConfig.INFORMATION,
//					MyMessageboxConfig.OK, "");
//			return false;
//		}
		if (jenisPegawai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) pilih Jenis Pegawai dari dropdown; (2) pastikan data jenis pegawai sudah tersedia; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK,
					"");
			return false;
		}
//		if (noSK.getValue() == null) {
//			MyMessageboxConfig.show("noSK Harus Diisi", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
//			return false;
//		}
//		if (tanggalSK.getValue() == null) {
//			MyMessageboxConfig.show("tanggalSK Harus Diisi", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
//			return false;
//		}
		if (tmt.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Mulai Tugas (TMT) belum diisi. Langkah yang dapat dilakukan: (1) pilih Tanggal Mulai Tugas menggunakan datepicker; (2) pastikan tanggal sudah benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", MyMessageboxConfig.INFORMATION, MyMessageboxConfig.OK, "");
			return false;
		}

		// boolean i = checkNamaDiklat();
		// if (i) {
		// MyMessageboxConfig.show("Nama Diklat sudah ada di database", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		RiwayatStatusKepegawaianDao riwayatStatusKepegawaianDao = DaoFactory.getInstance()
				.getRiwayatStatusKepegawaianDao();
		if (riwayatStatusKepegawaian.getId() != null) {
			riwayatStatusKepegawaian = riwayatStatusKepegawaianDao.load(riwayatStatusKepegawaian.getId());

		}

		// riwayatStatusKepegawaian.setPegawai((Pegawai) (pegawai
		// .getSelectedItem() == null ? null : pegawai.getSelectedItem()
		// .getValue()));
		riwayatStatusKepegawaian.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		riwayatStatusKepegawaian.setGolongan(
				(Golongan) (golongan.getSelectedItem() == null ? null : golongan.getSelectedItem().getValue()));
		riwayatStatusKepegawaian
				.setJabatanFungsional((JabatanFungsional) (jabatanFungsional.getSelectedItem() == null ? null
						: jabatanFungsional.getSelectedItem().getValue()));
		riwayatStatusKepegawaian
				.setJabatanStruktural((JabatanStruktural) (jabatanStruktural.getSelectedItem() == null ? null
						: jabatanStruktural.getSelectedItem().getValue()));
		riwayatStatusKepegawaian.setTanggalSK(tanggalSK.getValue());
		riwayatStatusKepegawaian.setTmt(tmt.getValue());
		riwayatStatusKepegawaian.setNoSK(noSK.getValue());
		riwayatStatusKepegawaian.setJenisPegawai(jenisPegawai.getValue());
		riwayatStatusKepegawaian.setStatusKepegawaian(
				statusKepegawaian.getSelectedItem() == null || statusKepegawaian.getSelectedItem().getValue() == null ? null : statusKepegawaian.getSelectedItem().getValue().toString());
		// riwayatStatusKepegawaian.setNama("nama");

		//
		if (riwayatStatusKepegawaian.getId() != null) {
			riwayatStatusKepegawaianDao.update(riwayatStatusKepegawaian);
		} else {
			riwayatStatusKepegawaianDao.save(riwayatStatusKepegawaian);
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				Session session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(riwayatStatusKepegawaian.getId());

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

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RiwayatStatusKepegawaian.class)
				
				
				.createAlias("pegawai", "pegawai")
				.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

		;
		if (order)
			criteria.addOrder(Order.asc("pegawai"));

		criteria.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai"))));
		// criteria.createCriteria("pegawai");
		// criteria.add(Restrictions.ilike("nama", searchnama.getValue(),
		// MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<RiwayatStatusKepegawaian> riwayatStatusKepegawaian = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(riwayatStatusKepegawaian);
		grid.setRowRenderer(new RiwayatStatusKepegawaianRender());
		grid.setModelCheckMobile(strset);

	}

	// public Boolean checkNamaDiklat() {
	//
	// Integer kotaCount = null;
	// Session session = HibernateUtil.currentSession();
	// kotaCount = ((Number) session
	// .createCriteria(Diklat.class)
	// .setProjection(Projections.rowCount())
	// .add(Restrictions.eq("nama", jenisPegawai.getValue().trim()))
	// .add(this.riwayatStatusKepegawaian.getId() == null ? Restrictions
	// .sqlRestriction("1=1") : Restrictions.ne("id",
	// this.riwayatStatusKepegawaian.getId())).uniqueResult())
	// .intValue();
	//
	// return !kotaCount.equals(0);
	// }

}