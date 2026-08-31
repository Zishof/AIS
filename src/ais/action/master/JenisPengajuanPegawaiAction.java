package ais.action.master;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.action.master.surat.helper.AmbilDataNomorSuratBanbox;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisPengajuanPegawai;
import ais.database.model.KelompokParameterTambahanPengajuanPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk jenis pengajuan pegawai. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Checkbox searchaktif}, {@code Textbox searchnama}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initKelompokParameterTambahanPengajuanPegawai()},
 * {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
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
public class JenisPengajuanPegawaiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Checkbox searchaktif;
	private Textbox searchnama;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisPengajuanPegawai jenisPengajuanPegawai;
	private MyToolbarbuttonConfig add;
	private Set<KelompokParameterTambahanPengajuanPegawai> selectedKelompokParameterTambahanPengajuanPegawai;
	protected LampiranLain lampiran;
	private AmbilDataNomorSuratBanbox nomorSurat;
	private Textbox jenisPengguna;
	private Textbox usernamePengguna;

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

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "nomorSurat", "jenisPengguna", "usernamePengguna",
				"keterangan", "dapatKonsumsi", "masukLembur", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPengajuanPegawai.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class JenisPengajuanPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisPengajuanPegawai jenisPengajuanPegawai = (JenisPengajuanPegawai) arg1;

			RevisiHelper.createNewRevisi(JenisPengajuanPegawai.class, jenisPengajuanPegawai,
					jenisPengajuanPegawai.getNama()).setParent(arg0);
			new Label(jenisPengajuanPegawai.getNomorSurat() == null ? ""
					: jenisPengajuanPegawai.getNomorSurat().getContohFormat()).setParent(arg0);

			new Label(jenisPengajuanPegawai.getJenisPengguna().isEmpty() ? "Tidak ditentukan"
					: jenisPengajuanPegawai.getJenisPengguna()).setParent(arg0);
			new Label(jenisPengajuanPegawai.getUsernamePengguna().isEmpty() ? "Tidak ditentukan"
					: jenisPengajuanPegawai.getUsernamePengguna()).setParent(arg0);

			final MyCheckboxConfig masukPresensi = new MyCheckboxConfig("Masuk Presensi");
			masukPresensi.setDisabled(!edit);
			masukPresensi.setChecked(jenisPengajuanPegawai.getMasukPresensi());
			masukPresensi.setParent(arg0);
			masukPresensi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPengajuanPegawai.setMasukPresensi(masukPresensi.isChecked());
					Common.refreshSaveOrUpdate(jenisPengajuanPegawai);
				}
			});

			final MyCheckboxConfig masukLembur = new MyCheckboxConfig("Masuk Lembur");
			masukLembur.setDisabled(!edit);
			masukLembur.setChecked(jenisPengajuanPegawai.getMasukLembur());
			masukLembur.setParent(arg0);
			masukLembur.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPengajuanPegawai.setMasukLembur(masukLembur.isChecked());
					Common.refreshSaveOrUpdate(jenisPengajuanPegawai);
				}
			});

			final MyCheckboxConfig dapatKonsumsi = new MyCheckboxConfig("Dapat Konsumsi");
			dapatKonsumsi.setDisabled(!edit);
			dapatKonsumsi.setChecked(jenisPengajuanPegawai.getDapatKonsumsi());
			dapatKonsumsi.setParent(arg0);
			dapatKonsumsi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPengajuanPegawai.setDapatKonsumsi(dapatKonsumsi.isChecked());
					Common.refreshSaveOrUpdate(jenisPengajuanPegawai);
				}
			});

			new Label(jenisPengajuanPegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisPengajuanPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisPengajuanPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisPengajuanPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisPengajuanPegawai, JenisPengajuanPegawaiAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisPengajuanPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPengajuanPegawai = (JenisPengajuanPegawai) obj;
		init(jenisPengajuanPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisPengajuanPegawai jenisPengajuanPegawai) {
		this.jenisPengajuanPegawai = jenisPengajuanPegawai;
		addWindow.setTitle(jenisPengajuanPegawai.getId() == null ? "Tambah Jenis Pengajuan Pegawai" : "Ubah Jenis Pengajuan Pegawai");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan Pegawai *"));
		row.appendChild(nama = new Textbox(jenisPengajuanPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Agenda *"));
		row.appendChild(nomorSurat = new AmbilDataNomorSuratBanbox());
		nomorSurat.setAttribute("nomorSurat", jenisPengajuanPegawai.getNomorSurat());
		nomorSurat.setValue(
				jenisPengajuanPegawai.getNomorSurat() == null ? "" : jenisPengajuanPegawai.getNomorSurat().getNama());
		nomorSurat.setWidth("90%");
		nomorSurat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis pengguna (sebagai admin)"));
		row.appendChild(jenisPengguna = new Textbox(jenisPengajuanPegawai.getJenisPengguna()));
		jenisPengguna.setWidth("90%");
		jenisPengguna.setRows(2);

		Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua aktor pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Username pengguna (sebagai admin)"));
		row.appendChild(usernamePengguna = new Textbox(jenisPengajuanPegawai.getUsernamePengguna()));
		usernamePengguna.setWidth("90%");
		usernamePengguna.setRows(2);

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Username Pengguna",
				"/img/user_male_add.png");

		final MyFormRow rowAmbilPengguna = new MyFormRow();
		rowAmbilPengguna.setParent(rows);
		rowAmbilPengguna.appendChild(new ais.ui.util.MyLabelConfig(""));
		rowAmbilPengguna.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								usernamePengguna.setValue(usernamePengguna.getValue()
										+ (usernamePengguna.getValue().isEmpty() ? tbmuser.getUserId()
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

		Common.initKeterangan(rows,
				"Jika lebih dari satu, pisahkan dengan tanda koma (,). Kosongkan apabila boleh diajukan oleh semua username pengguna");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPengajuanPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Laporan (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, jenisPengajuanPegawai.getId(),
				LampiranLain.FILE_JRXML_LAYOUT_JENIS_PENGAJUAN, "File Laporan jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		initKelompokParameterTambahanPengajuanPegawai(rows);

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
	private void initKelompokParameterTambahanPengajuanPegawai(Rows rows) {
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		final MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		Column c = new Column("Parameter Pengajuan Pegawai");
		subColumns.appendChild(c);

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		@SuppressWarnings("unchecked")
		Map<Long, KelompokParameterTambahanPengajuanPegawai> kelompokParameterTambahanPengajuanPegawais = ConstantValues
				.ambilBerdasarClass(KelompokParameterTambahanPengajuanPegawai.class);

		if (jenisPengajuanPegawai != null && jenisPengajuanPegawai.getId() != null) {
			HibernateUtil.currentSession().refresh(jenisPengajuanPegawai);
		}

		Set<Long> ids = new HashSet<Long>();
		try {
			selectedKelompokParameterTambahanPengajuanPegawai = this.jenisPengajuanPegawai
					.getKelompokParameterTambahanPengajuanPegawais();

			for (KelompokParameterTambahanPengajuanPegawai v : selectedKelompokParameterTambahanPengajuanPegawai) {
				ids.add(v.getId());
			}
		} catch (Exception e) {
			selectedKelompokParameterTambahanPengajuanPegawai = new TreeSet<KelompokParameterTambahanPengajuanPegawai>();
		}

		System.out.println("ids ->" + ids);

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final KelompokParameterTambahanPengajuanPegawai kelompokParameterTambahanPengajuanPegawai : kelompokParameterTambahanPengajuanPegawais
				.values()) {
			final Checkbox checkbox = new Checkbox(kelompokParameterTambahanPengajuanPegawai.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(ids.contains(kelompokParameterTambahanPengajuanPegawai.getId()));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedKelompokParameterTambahanPengajuanPegawai
								.add(kelompokParameterTambahanPengajuanPegawai);
					} else {

						for (KelompokParameterTambahanPengajuanPegawai a : selectedKelompokParameterTambahanPengajuanPegawai) {
							if (a.getId().equals(kelompokParameterTambahanPengajuanPegawai.getId())) {
								selectedKelompokParameterTambahanPengajuanPegawai.remove(a);
								break;
							}
						}

					}

					System.out.println("selectedKelompokParameterTambahanPengajuanPegawai => "
							+ selectedKelompokParameterTambahanPengajuanPegawai);
				}
			});
		}
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pengajuan Pegawai",
					"Kolom Nama Jenis Pengajuan Pegawai belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Jenis Pengajuan Pegawai.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nomorSurat.getAttribute("nomorSurat") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Format Nomor Agenda",
					"Kolom Format Nomor Agenda belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Format Nomor Agenda.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPengajuanPegawai.getId() != null) {
			jenisPengajuanPegawai = (JenisPengajuanPegawai) session.load(JenisPengajuanPegawai.class,
					jenisPengajuanPegawai.getId());

		}

		jenisPengajuanPegawai.setNama(nama.getValue());
		jenisPengajuanPegawai.setKeterangan(keterangan.getValue());
		jenisPengajuanPegawai
				.setKelompokParameterTambahanPengajuanPegawais(selectedKelompokParameterTambahanPengajuanPegawai);
		jenisPengajuanPegawai.setNomorSurat((NomorSurat) nomorSurat.getAttribute("nomorSurat"));

		jenisPengajuanPegawai.setJenisPengguna(jenisPengguna.getValue());
		jenisPengajuanPegawai.setUsernamePengguna(usernamePengguna.getValue());

		Common.refreshSaveOrUpdate(session, jenisPengajuanPegawai);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(jenisPengajuanPegawai.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPengajuanPegawai.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPengajuanPegawai> jenisPengajuanPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPengajuanPegawai);
		grid.setRowRenderer(new JenisPengajuanPegawaiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
