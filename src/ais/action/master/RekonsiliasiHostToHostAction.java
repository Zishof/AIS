package ais.action.master;

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
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
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

import ais.action.master.helper.DefaultJenisParsingReconsile;
import ais.action.master.helper.JenisParsingReconsile;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.JenisRekonsiliasiHostToHost;
import ais.database.model.Konfigurasi;
import ais.database.model.RekonsiliasiHostToHost;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk rekonsiliasi host to host. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchkode}, {@code Textbox searchnama}, {@code Textbox
 * searchketerangan}, {@code Combobox searchstatus}, {@code MyDatebox searchmulai}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onBelumRekonsiliasi()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
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
public class RekonsiliasiHostToHostAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchkode;
	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchstatus;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

	private Combobox jenisRekonsiliasiHostToHost;
	private LampiranLain lainMahasiswa = null;

	private RekonsiliasiHostToHost rekonsiliasiHostToHost;

	private MyToolbarbuttonConfig find;

	private Tabpanel belumRekonsiliasi;

	public void onBelumRekonsiliasi(Event event) {
		if (belumRekonsiliasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(belumRekonsiliasi);
			MyInclude iframe = new MyInclude("/pages/master/log_host_to_host_yang_belum_reconsile.zul");
			iframe.setParent(window);
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

		MyComboitemConfig comboitem = new MyComboitemConfig(RekonsiliasiHostToHost.SUKSES);
		if (comboitem != null) { comboitem.setValue(RekonsiliasiHostToHost.SUKSES); }
		searchstatus.appendChild(comboitem);

		comboitem = new MyComboitemConfig(RekonsiliasiHostToHost.GAGAL);
		if (comboitem != null) { comboitem.setValue(RekonsiliasiHostToHost.GAGAL); }
		searchstatus.appendChild(comboitem);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "kode", "nama", "lampiranId",
				"jenisRekonsiliasiHostToHost", "waktu", "nilai", "status");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link RekonsiliasiHostToHostAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link RekonsiliasiHostToHostAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see RekonsiliasiHostToHostAction
	 */
	class RekonsiliasiHostToHostRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final RekonsiliasiHostToHost rekonsiliasiHostToHost = (RekonsiliasiHostToHost) arg1;

			new Label(rekonsiliasiHostToHost.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(RekonsiliasiHostToHost.class, rekonsiliasiHostToHost,
					rekonsiliasiHostToHost.getNama()).setParent(arg0);
			new Label(rekonsiliasiHostToHost.getJenisRekonsiliasiHostToHost() == null ? ""
					: rekonsiliasiHostToHost.getJenisRekonsiliasiHostToHost().getNama()).setParent(arg0);

			new Label(rekonsiliasiHostToHost.getWaktu() == null ? ""
					: Common.dateFormat3.get().format(rekonsiliasiHostToHost.getWaktu())).setParent(arg0);

			new Label(Common.numberFormat.get().format(rekonsiliasiHostToHost.getNilai())).setParent(arg0);

			if (rekonsiliasiHostToHost.getStatus() != null
					&& rekonsiliasiHostToHost.getStatus().equals(RekonsiliasiHostToHost.SUKSES)) {
				@SuppressWarnings("unchecked")
				List<CicilanPembayaran> cicilanPembayarans = HibernateUtil.currentSession()
						.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("rekonsiliasiHostToHost", rekonsiliasiHostToHost)).list();

				new Label(cicilanPembayarans.toString()).setParent(arg0);
			} else {
				@SuppressWarnings("unchecked")
				List<CicilanPembayaranGagal> cicilanPembayaranGagals = HibernateUtil.currentSession()
						.createCriteria(CicilanPembayaranGagal.class)
						.add(Restrictions.eq("rekonsiliasiHostToHost", rekonsiliasiHostToHost)).list();

				new Label(cicilanPembayaranGagals.toString()).setParent(arg0);
			}

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LampiranLain lainMahasiswa = LampiranLain.ambil(rekonsiliasiHostToHost.getId(),
							LampiranLain.REKONSILIASI_HOST_TO_HOST);

					if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
						Filedownload.save(lainMahasiswa.ambilFile(), lainMahasiswa.getKeterangan());
					}

				}
			};

			A a;
			(a = new A(rekonsiliasiHostToHost.getKeterangan() + " " + (rekonsiliasiHostToHost.getLogHostToHost() == null
					? "" : rekonsiliasiHostToHost.getLogHostToHost().getNama()))).setParent(arg0);
			a.addEventListener("onClick", eventListener);

			new Label(rekonsiliasiHostToHost.getStatus()).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new RekonsiliasiHostToHost());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(RekonsiliasiHostToHost rekonsiliasiHostToHost) {
		this.rekonsiliasiHostToHost = rekonsiliasiHostToHost;
		addWindow.setTitle(rekonsiliasiHostToHost.getId() == null ? "Tambah Rekonsiliasi Host To Host" : "Ubah Rekonsiliasi Host To Host");
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Rekonsiliasi"));
		row.appendChild(jenisRekonsiliasiHostToHost = new Combobox());
		Common.insertCombo(jenisRekonsiliasiHostToHost, "nama", "namaKelas", JenisRekonsiliasiHostToHost.class);
		Common.selectComboItem(jenisRekonsiliasiHostToHost, rekonsiliasiHostToHost.getJenisRekonsiliasiHostToHost());
		jenisRekonsiliasiHostToHost.setWidth("90%");
		jenisRekonsiliasiHostToHost.setReadonly(true);

		Konfigurasi kelas = Common.getKonfigurasi(
				"default_class_yang_digunakan_untuk_memproses_reconsile_pembayaran_host_to_host",
				DefaultJenisParsingReconsile.class.getName());
		boolean ada = false;
		for (Object o : jenisRekonsiliasiHostToHost.getChildren()) {
			Comboitem comboitem = (Comboitem) o;
			JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost = (JenisRekonsiliasiHostToHost) comboitem
					.getValue();
			if (jenisRekonsiliasiHostToHost.getNamaKelas().equalsIgnoreCase(kelas.getNilai())) {
				ada = true;
				this.jenisRekonsiliasiHostToHost.setSelectedItem(comboitem);
				break;
			}
		}

		if (!ada) {
			@SuppressWarnings("rawtypes")
			Class clazz;
			try {
				clazz = Class.forName(kelas.getNilai());
				JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost = new JenisRekonsiliasiHostToHost();
				jenisRekonsiliasiHostToHost.setNama(clazz.getSimpleName());
				jenisRekonsiliasiHostToHost.setNamaKelas(clazz.getName());
				Common.refreshSaveOrUpdate(jenisRekonsiliasiHostToHost);
				MyComboitemConfig comboitem = new MyComboitemConfig(jenisRekonsiliasiHostToHost.getNama());
				comboitem.setDescription(clazz.getName());
				comboitem.setValue(jenisRekonsiliasiHostToHost);
				this.jenisRekonsiliasiHostToHost.appendChild(comboitem);
				this.jenisRekonsiliasiHostToHost.setSelectedItem(comboitem);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File Lampiran Rekonsiliasi"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, Common.randLong(), LampiranLain.REKONSILIASI_HOST_TO_HOST,
				"Lampiran Rekonsiliasi", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

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
		if (rekonsiliasiHostToHost != null && rekonsiliasiHostToHost.getId() != null) {
			LampiranLain lainMahasiswa = LampiranLain.ambil(rekonsiliasiHostToHost.getId(),
					LampiranLain.REKONSILIASI_HOST_TO_HOST);

			if (lainMahasiswa == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data File lampiran",
						"Kolom File lampiran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu File lampiran.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return false;
			}
		} else {
			if (lainMahasiswa == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data File lampiran",
						"Kolom File lampiran belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu File lampiran.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return false;
			}
		}

		final JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost = (JenisRekonsiliasiHostToHost) (this.jenisRekonsiliasiHostToHost
				.getSelectedItem() == null ? null : this.jenisRekonsiliasiHostToHost.getSelectedItem().getValue());
		if (jenisRekonsiliasiHostToHost == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Rekonsiliasi",
					"Kolom Jenis Rekonsiliasi belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Rekonsiliasi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				JenisParsingReconsile jenisParsingReconsile = (JenisParsingReconsile) Class
						.forName(jenisRekonsiliasiHostToHost.getNamaKelas()).newInstance();
				jenisParsingReconsile.parsing(lainMahasiswa, jenisRekonsiliasiHostToHost);
				onSearchDefault(arg0);
			}
		}, "Sedang memproses data yang telah Anda upload..");

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RekonsiliasiHostToHost.class);

		if (order)
			criteria.addOrder(Order.desc("waktu"));
		criteria

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.waktu) >= date('"
								+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "')")))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.waktu) <= date('"
								+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))

				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))
				.add(searchkode.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchketerangan.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<RekonsiliasiHostToHost> rekonsiliasiHostToHost = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(rekonsiliasiHostToHost);
		grid.setRowRenderer(new RekonsiliasiHostToHostRenderer());
		grid.setModelCheckMobile(strset);

	}

}
