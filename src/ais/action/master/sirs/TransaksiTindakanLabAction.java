package ais.action.master.sirs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataTempatTidurBanbox;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.action.master.sirs.util.CommonTarifTindakan;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.listener.TransaksiListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.TindakanDiagnosaPenyakit;
import ais.database.model.sirs.TindakanLabDetail;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.database.model.sirs.TransaksiTindakanLabDetail;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk transaksi tindakan lab. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Grid grid}, {@code Paging paging},
 * {@code Tabpanel tambahData}, {@code MyTextbox searchkode}, {@code MyTextbox searchmr}, {@code MyTextbox
 * searchnama}, {@code Combobox searchkelas}, {@code Combobox searchruang}; inisialisasi/lifecycle ({@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()});
 * validasi/perhitungan ({@code checkKodeTransaksi()}); mutasi data ({@code onSave()}, {@code onBerubah()});
 * penghapusan/pembatalan ({@code onDelete()}); pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code
 * onAdd()}, {@code createMain()}, {@code onBebas()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
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
public class TransaksiTindakanLabAction extends GenericAutowireComposer implements TransaksiListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Grid grid;
	private Paging paging;

	private Tabpanel tambahData;

	private MyTextbox searchkode;
	private MyTextbox searchmr;
	private MyTextbox searchnama;

	private Combobox searchkelas;
	private Combobox searchruang;
	private Combobox searchkamar;
	private AmbilDataTempatTidurBanbox searchbed;

	private Label kode;
	private Pasien pasien;
	private Pendaftaran pendaftaran;
	private String keterangan;
	private Date tanggalTransaksi;
	private Boolean bebas;
	private KelasPerawatan kelasPerawatan;
	private String nama;

	private boolean edit = false;
	private boolean delete = false;

	private TransaksiMedis transaksi;
	private Toolbarbutton add;
	private Toolbarbutton simpan;
	private Toolbarbutton cetak;

	private Center center = new Center();

	private String SUMBER = TransaksiMedis.SUMBER_LAB;
	private TindakanLabDetailLabAction tindakanLabDetailLabAction;

	private Lokasi myLokasi = Common.getCurrentLokasi();
	private Shift myShift;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}
		myLokasi = Common.getCurrentLokasi();

		if (execution.getParameter("sumber") != null && !execution.getParameter("sumber").trim().equalsIgnoreCase("")) {
			SUMBER = execution.getParameter("sumber").trim();
		}
		System.out.println("SUMBER => " + SUMBER + " =============================");

		Common.insertCombo(searchkelas, "nama", "keterangan", KelasPerawatan.class);
		Common.insertCombo(searchruang, "nama", "keterangan", Ruang.class);

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchkamar);
				if (searchkelas.getSelectedItem() != null && searchruang.getSelectedItem() != null) {
					Common.insertCombo(searchkamar, "nama", "keterangan", Kamar.class,
							Restrictions.and(Restrictions.eq("ruang", searchruang.getSelectedItem().getValue()),
									Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue())));
				}
			}
		};

		searchkelas.addEventListener("onChange", myEventListener);
		searchruang.addEventListener("onChange", myEventListener);

		searchkelas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KelasPerawatan mykelasPerawatan = (KelasPerawatan) (searchkelas.getSelectedItem() == null ? null
						: searchkelas.getSelectedItem().getValue());
				if (mykelasPerawatan != null) {
					searchbed.setMyKelasPerawatan(mykelasPerawatan);
				}
			}
		});

		searchruang.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Ruang myRuang = (Ruang) (searchruang.getSelectedItem() == null ? null
						: searchruang.getSelectedItem().getValue());
				if (myRuang != null) {
					searchbed.setMyRuang(myRuang);
				}
			}
		});

		searchkamar.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Kamar myKamar = (Kamar) (searchkamar.getSelectedItem() == null ? null
						: searchkamar.getSelectedItem().getValue());
				if (myKamar != null) {
					searchbed.setMyKamar(myKamar);
				}
			}
		});

		add = new ais.ui.util.MyToolbarbuttonConfig("Buat Hasil (" + SUMBER + ") Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new TransaksiMedis());

			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		init(new TransaksiMedis());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link TransaksiTindakanLabAction}. Kelas ini menerjemahkan satu item
	 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TransaksiTindakanLabAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TransaksiTindakanLabAction
	 */
	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final TransaksiMedis transaksi = (TransaksiMedis) arg1;

			if (transaksi.getValidasi() == null || !transaksi.getValidasi()) {
				arg0.setStyle("background-color:yellow;");
			} else {
				arg0.setStyle("background-color:#DBFDF3;");
			}

			Pasien pasien = transaksi.getPasien();

			new ais.action.master.sirs.detail.TransaksiTindakanDetailAction(transaksi).setParent(arg0);

			RevisiHelper.createNewRevisi(TransaksiMedis.class, transaksi, transaksi.getKode()).setParent(arg0);
			// new Label(pasien == null ? "" :
			// pasien.getKode()).setParent(arg0);
			new Label(pasien == null ? transaksi.getNama() : pasien.getNama()).setParent(arg0);
			new Label(transaksi.getTanggalTransaksi() == null ? ""
					: Common.dateFormat3.get().format(transaksi.getTanggalTransaksi())).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getAlamatLengkap()).setParent(arg0);
			new Label(transaksi.getKelasPerawatan() == null ? "" : transaksi.getKelasPerawatan().getNama())
					.setParent(arg0);

			String bed = "";
			if (transaksi.getPendaftaran() != null && transaksi.getPendaftaran().getTempatTidur() != null) {
				bed = (transaksi.getPendaftaran().getRuangPerawatan() == null ? ""
						: transaksi.getPendaftaran().getRuangPerawatan().getNama())
						+ " - "
						+ (transaksi.getPendaftaran().getKamarPerawatan() == null ? ""
								: transaksi.getPendaftaran().getKamarPerawatan().getNama())
						+ " - " + (transaksi.getPendaftaran().getTempatTidur() == null ? ""
								: transaksi.getPendaftaran().getTempatTidur().getNama());
			}

			new Label(bed).setParent(arg0);

			new Label(transaksi.getBebas() ? "Ya" : "Tidak").setParent(arg0);
			new Label(transaksi.getValidasi() == null || !transaksi.getValidasi() ? "Belum" : "Ya").setParent(arg0);
			new Label(transaksi.getLunas() ? "Ya" : "Belum").setParent(arg0);

			Hbox toolbar = new Hbox();

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Transaksi");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					onCetak(transaksi);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && (transaksi.getLunas() == null || !transaksi.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(transaksi);

				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && (transaksi.getLunas() == null || !transaksi.getLunas()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(transaksi);

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TransaksiMedis());
	}

	private TransaksiDetailAction transaksiDetailAction;

	private Borderlayout createMain(final TransaksiMedis transaksi) throws Exception {
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setStyle("border:0px;background: transparent;");
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		final Rows rows = new Rows();
		rows.setParent(grid);

		kode = new Label(transaksi.getKode());
		CommonPendaftaranUtil.initTransaksi(rows, kode, transaksi, this);

		CommonSirs.initLokasiDanShift(transaksi.getLokasi() == null ? myLokasi : transaksi.getLokasi(),
				transaksi.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];
					}
				});

		return borderlayout;
	}

	/**
	 * Tipe implementasi bersarang {@link TransaksiDetailAction} milik {@link TransaksiTindakanLabAction}. Kelas
	 * ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TransaksiTindakanLabAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Grid grid}; operasi lokal: {@code
	 * loadData()}, {@code display}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see TransaksiTindakanLabAction
	 */
	public class TransaksiDetailAction extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private Grid grid;

		public TransaksiDetailAction() throws Exception {
			super();
			display();
		}

		/**
		 * Renderer lokal untuk layar/komponen {@link TransaksiDetailAction}. Kelas ini menerjemahkan satu item data
		 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TransaksiDetailAction} dan dapat mengakses
		 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see TransaksiDetailAction
		 */
		class TransaksiDetailRenderer extends ais.ui.util.MyRowRenderer {

			public TransaksiDetailRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final TransaksiMedisDetail transaksiDetail = (TransaksiMedisDetail) data;

				final Radio radio = new Radio();
				radio.setParent(row);
				radio.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (radio.isChecked()) {
							tindakanLabDetailLabAction.loadData(transaksiDetail);
						}
					}
				});

				RevisiHelper
						.createNewRevisi(TransaksiMedisDetail.class, transaksiDetail,
								transaksiDetail.getTindakan() == null ? "" : transaksiDetail.getTindakan().getNama())
						.setParent(row);

				new Label(transaksiDetail.getTindakan() == null
						|| transaksiDetail.getTindakan().getJenisTindakan() == null ? ""
								: transaksiDetail.getTindakan().getJenisTindakan().getNama())
						.setParent(row);

				final MyDatebox myDatebox = new MyDatebox(transaksiDetail.getTanggalTindakan());
				myDatebox.setWidth("90%");
				myDatebox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						transaksiDetail.setTanggalTindakan(myDatebox.getValue());
						session.update(transaksiDetail);

					}
				});
				myDatebox.setParent(row);

			}
		}

		@SuppressWarnings("unchecked")
		public void loadData(Object value) throws Exception {
			Session session = HibernateUtil.currentSession();
			List<TransaksiMedisDetail> transaksiDetails = transaksi == null || transaksi.getId() == null
					? new ArrayList<TransaksiMedisDetail>()
					: session.createCriteria(TransaksiMedisDetail.class).addOrder(Order.desc("id"))
							.add(Restrictions.eq("transaksi", transaksi)).list();

			if (transaksiDetails.isEmpty() && value != null && value instanceof Pendaftaran) {

				if (TransaksiTindakanLabAction.this.onSave(null)) {

					Pendaftaran myPendaftaran = (Pendaftaran) value;

					Criterion cariTindakan = Restrictions.sqlRestriction("1=1");
					if (SUMBER.equals(TransaksiMedis.SUMBER_LAB)) {
						cariTindakan = Restrictions.eq("tindakan.tindakanLab", true);
					} else if (SUMBER.equals(TransaksiMedis.SUMBER_OPERASI)) {
						cariTindakan = Restrictions.eq("tindakan.tindakanOperasi", true);
					} else if (SUMBER.equals(TransaksiMedis.SUMBER_RADIOLOGI)) {
						cariTindakan = Restrictions.eq("tindakan.tindakanRadiologi", true);
					} else if (SUMBER.equals(TransaksiMedis.SUMBER_VK)) {
						cariTindakan = Restrictions.eq("tindakan.tindakanVk", true);
					} else if (SUMBER.equals(TransaksiMedis.SUMBER_RENAL_UNIT)) {
						cariTindakan = Restrictions.eq("tindakan.tindakanRenalUnit", true);
					} else if (SUMBER.equals(TransaksiMedis.SUMBER_GIZI)) {
						cariTindakan = Restrictions.eq("tindakan.tindakanGizi", true);
					}

					List<TindakanDiagnosaPenyakit> tindakanDiagnosaPenyakits = HibernateUtil.currentSession()
							.createCriteria(TindakanDiagnosaPenyakit.class)
							.createAlias("diagnosaPenyakit", "diagnosaPenyakit").createAlias("tindakan", "tindakan")
							.add(Restrictions.eq("diagnosaPenyakit.pendaftaran", myPendaftaran)).add(cariTindakan)
							.list();
					final KelasPerawatan kelasPerawatan = pendaftaran.getKelasPerawatan();

					for (TindakanDiagnosaPenyakit tindakanDiagnosaPenyakit : tindakanDiagnosaPenyakits) {
						Tindakan tindakan = tindakanDiagnosaPenyakit.getTindakan();
						if (tindakan != null && tindakan.getId() != null) {

							BiayaTindakanPerKelas hargaJualItem = CommonTarifTindakan.getBiayaTindakanPerKelas(tindakan,
									kelasPerawatan);

							TransaksiMedisDetail transaksiDetail = (TransaksiMedisDetail) session
									.createCriteria(TransaksiMedisDetail.class)
									.add(Restrictions.eq("tindakanDiagnosaPenyakit", tindakanDiagnosaPenyakit))
									.add(Restrictions.eq("transaksi", transaksi)).setMaxResults(1).uniqueResult();
							if (transaksiDetail == null) {
								transaksiDetail = new TransaksiMedisDetail();
							}
							transaksiDetail.setTindakanDiagnosaPenyakit(tindakanDiagnosaPenyakit);
							transaksiDetail
									.setAmount(hargaJualItem.getBiaya() == null ? 0.0 : hargaJualItem.getBiaya());
							transaksiDetail.setTindakan(tindakan);
							transaksiDetail.setQty(1.0);
							transaksiDetail.setKeterangan(
									"Transaksi layanan di lokasi " + (myLokasi == null ? "" : myLokasi.getNama()));
							transaksiDetail.setTransaksi(transaksi);

							session.saveOrUpdate(transaksiDetail);
							transaksiDetails.add(transaksiDetail);
						}
					}

					simpan.setDisabled(transaksiDetails.size() == 0);
					add.setDisabled(transaksiDetails.size() != 0);
				}
			}

			ListModel strset = new SimpleListModel(transaksiDetails);
			grid.setRowRenderer(new TransaksiDetailRenderer());
			grid.setModel(strset);
			grid.renderAll();

		}

		private void display() throws Exception {

			setHeight("100%");
			setWidth("100%");
			setStyle("border:0px;background: transparent;");

			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new Grid();
			grid.setMold("paging");
			grid.setPageSize(25);
			grid.setParent(center);

			Columns columns = new Columns();

			columns.setParent(grid);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("40px");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Tindakan");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Jenis");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Tgl. Pelaksanaan");

			loadData(null);
		}
	}

	/**
	 * Tipe implementasi bersarang {@link TindakanLabDetailLabAction} milik {@link TransaksiTindakanLabAction}.
	 * Kelas ini memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok
	 * anonim.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TransaksiTindakanLabAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code Grid grid}; operasi lokal: {@code
	 * loadData()}, {@code display}(). Aturan bisnis bersama tetap berada pada kelas induk atau service yang
	 * dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
	 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
	 * tambahkan perilaku lintas domain pada service bersama.</p>
	 *
	 * @see TransaksiTindakanLabAction
	 */
	public class TindakanLabDetailLabAction extends Borderlayout {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private Grid grid;

		public TindakanLabDetailLabAction() throws Exception {
			super();
			display();
		}

		/**
		 * Renderer lokal untuk layar/komponen {@link TindakanLabDetailLabAction}. Kelas ini menerjemahkan satu item
		 * data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TindakanLabDetailLabAction} dan dapat
		 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see TindakanLabDetailLabAction
		 */
		class TindakanLabDetailRenderer extends ais.ui.util.MyRowRenderer {

			public TindakanLabDetailRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final TransaksiTindakanLabDetail transaksiTindakanLabDetail = (TransaksiTindakanLabDetail) data;
				TindakanLabDetail tindakanLabDetail = transaksiTindakanLabDetail.getTindakanLabDetail();

				RevisiHelper.createNewRevisi(TindakanLabDetail.class, tindakanLabDetail, tindakanLabDetail.getNama())
						.setParent(row);

				final Textbox nilaiTextbox = new Textbox(transaksiTindakanLabDetail.getNilai());
				nilaiTextbox.setWidth("90%");
				nilaiTextbox.setParent(row);
				nilaiTextbox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						transaksiTindakanLabDetail.setNilai(nilaiTextbox.getValue().trim());
						session.update(transaksiTindakanLabDetail);
					}
				});

				new Label(tindakanLabDetail.getNormal()).setParent(row);
				new Label(tindakanLabDetail.getSatuan()).setParent(row);

			}
		}

		@SuppressWarnings("unchecked")
		public void loadData(TransaksiMedisDetail transaksiDetail) throws Exception {
			Tindakan tindakan = transaksiDetail.getTindakan();
			Session session = HibernateUtil.currentSession();
			List<TindakanLabDetail> tindakanLabDetails = session.createCriteria(TindakanLabDetail.class)
					.addOrder(Order.asc("nama")).add(Restrictions.eq("tindakan", tindakan)).list();

			List<TransaksiTindakanLabDetail> transaksiTindakanLabDetails = new ArrayList<TransaksiTindakanLabDetail>();
			for (TindakanLabDetail tindakanLabDetail : tindakanLabDetails) {
				TransaksiTindakanLabDetail transaksiTindakanLabDetail = (TransaksiTindakanLabDetail) session
						.createCriteria(TransaksiTindakanLabDetail.class)
						.add(Restrictions.eq("tindakanLabDetail", tindakanLabDetail))
						.add(Restrictions.eq("transaksiDetail", transaksiDetail)).setMaxResults(1).uniqueResult();
				if (transaksiTindakanLabDetail == null) {
					transaksiTindakanLabDetail = new TransaksiTindakanLabDetail();
					transaksiTindakanLabDetail.setTindakanLabDetail(tindakanLabDetail);
					transaksiTindakanLabDetail.setTransaksiDetail(transaksiDetail);
					session.save(transaksiTindakanLabDetail);
				}
				transaksiTindakanLabDetails.add(transaksiTindakanLabDetail);
			}

			ListModel strset = new SimpleListModel(transaksiTindakanLabDetails);
			grid.setRowRenderer(new TindakanLabDetailRenderer());
			grid.setModel(strset);
			grid.renderAll();

		}

		private void display() throws Exception {

			setHeight("100%");
			setWidth("100%");
			setStyle("border:0px;background: transparent;");

			Center center = new Center();
			center.setParent(this);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new Grid();
			grid.setMold("paging");
			grid.setPageSize(25);
			grid.setParent(center);

			Columns columns = new Columns();

			columns.setParent(grid);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Jenis");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nilai Hasil");
			column.setWidth("35%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nilai Normal");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Satuan");

		}
	}

	private void init(final TransaksiMedis transaksi) throws Exception {
		this.transaksi = transaksi;

		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();

		East east = new East();
		east.setStyle("border:0px;background: transparent;");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("70%");

		Common.clear(center);
		center.setStyle("border:0px;background: transparent;");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		center.appendChild(createMain(transaksi));
		if (transaksi.getId() != null) {
			Common.freeze(center, true);
		}

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(east);

		Borderlayout subBorderlayout = new Borderlayout();
		subBorderlayout.setParent(radiogroup);
		Center subCenter = new Center();
		subCenter.setTitle("Daftar Tindakan (" + SUMBER + ")");
		subCenter.setParent(subBorderlayout);
		ais.ui.util.ZkCompat.setFlex(subCenter, true);

		East myeast = new East();
		myeast.setStyle("border:0px;background: transparent;");
		myeast.setParent(subBorderlayout);
		myeast.setTitle("Daftar Hasil Tindakan (" + SUMBER + ")");
		ais.ui.util.ZkCompat.setFlex(myeast, true);
		myeast.setWidth("60%");
		tindakanLabDetailLabAction = new TindakanLabDetailLabAction();
		myeast.appendChild(tindakanLabDetailLabAction);

		transaksiDetailAction = new TransaksiDetailAction();
		subCenter.appendChild(transaksiDetailAction);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		add.setParent(toolbar);
		simpan = new ais.ui.util.MyToolbarbuttonConfig("Simpan Hasil (" + SUMBER + ").", "/img/save.gif");
		simpan.setTooltiptext("Simpan");
		simpan.setDisabled(false);
		add.setDisabled(false);
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					MyMessageboxConfig.show("Alhamdulillah, data transaksi telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onCetak(transaksi);
								}
							});
					add.setDisabled(false);
					simpan.setDisabled(true);
					add.setDisabled(false);
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					//
					Common.freeze(center, true);
					Common.freeze(transaksiDetailAction, true);
					Common.freeze(tindakanLabDetailLabAction, true);
				}
			}
		});
		simpan.setParent(toolbar);

		cetak = new ais.ui.util.MyToolbarbuttonConfig("Cetak Hasil (" + SUMBER + ").", "/img/print.png");
		cetak.setTooltiptext("Cetak");
		cetak.setDisabled(transaksi.getId() == null);
		cetak.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(transaksi);
			}
		});
		cetak.setParent(toolbar);

		add.setDisabled(false);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);
	}

	public void onDelete(final TransaksiMedis transaksi) throws Exception {

		MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin membatalkan transaksi ini? Perlu diketahui bahwa transaksi yang telah dibatalkan tidak dapat dikembalikan.", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Session session = HibernateUtil.currentSession();

								session.createSQLQuery(
										"delete from sirs.detail_transaksi_layanan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								session.createSQLQuery(
										"delete from sirs.detail_transaksi_pasien where racikan_detail in (select id from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ")));")
										.executeUpdate();

								String sql = "delete from sirs.racikan_detail where racikan in (select id from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
										+ transaksi.getId() + "));";
								session.createSQLQuery(sql).executeUpdate();

								session.createSQLQuery(
										"update sirs.transaksi_medis_detail set racikan = null where transaksi = "
												+ transaksi.getId() + ";")
										.executeUpdate();

								session.createSQLQuery(
										"delete from sirs.racikan where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								session.createSQLQuery(
										"delete from sirs.transaksi_medis_detail where transaksi_detail in (select id from sirs.transaksi_medis_detail where transaksi = "
												+ transaksi.getId() + ");")
										.executeUpdate();

								List<TransaksiMedisDetail> transaksiDetails = session.createCriteria(TransaksiMedisDetail.class)
										.add(Restrictions.eq("transaksi", transaksi)).list();

								for (TransaksiMedisDetail transaksiDetail : transaksiDetails) {
									Common.refreshDelete(session, transaksiDetail);
								}

								Common.refreshDelete(session, transaksi);
								init(new TransaksiMedis());

								onSearchDefault(event);
							} catch (Exception e) {
								ais.common.Common.tampilErrorJikaAdmin(e);
								MyMessageboxConfig.show(Common.pesan(
										"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
												e.getMessage()));
							}

						}

					}
				});
	}

	public void onCetak(TransaksiMedis transaksi) throws Exception {
		final Map<String, Serializable> parameters = new HashMap<String, Serializable>();
		parameters.put("id", transaksi.getId());
		parameters.put("sumber", SUMBER);
		Report.generateWindowReport(Report.PDF, parameters, "sirs/hasil_uji_laboratorium", transaksi.getTanggalTransaksi());
	}

	public boolean onSave(Event event) throws Exception {
		if (myLokasi == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Lokasi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Lokasi yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi data Shift terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) pilih Shift yang sesuai; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggalTransaksi == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi Tanggal Transaksi terlebih dahulu karena data ini wajib diisi. Langkah yang dapat dilakukan: (1) tentukan Tanggal Transaksi; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (!bebas && pendaftaran == null) {
			MyMessageboxConfig.show("Mohon maaf, untuk pasien yang bukan berstatus BEBAS, data Pendaftaran wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih data Pendaftaran pasien; (2) atau tandai transaksi sebagai pasien BEBAS bila memang sesuai; (3) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (nama == null || nama.trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi kolom Nama terlebih dahulu karena kolom ini wajib diisi. Langkah yang dapat dilakukan: (1) isikan kolom Nama; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (transaksi.getId() != null) {
			transaksi = (TransaksiMedis) session.load(TransaksiMedis.class, transaksi.getId());

		}

		if (kode.getValue().trim().equals("")) {
			kode.setValue(Common.generateCode(TransaksiMedis.class, 8));
		}

		transaksi.setJenisTransaksi(TransaksiMedis.TRX_ITEM);
		transaksi.setSumber(SUMBER);
		transaksi.setNama(nama);
		transaksi.setAlamat(pasien == null ? "" : pasien.getAlamatLengkap());
		transaksi.setKelasPerawatan((KelasPerawatan) (kelasPerawatan));

		transaksi.setUmur(pasien == null ? "" : pasien.getUmur() + " thn");
		transaksi.setTanggalTransaksi(tanggalTransaksi);

		transaksi.setPasien((Pasien) pasien);
		transaksi.setKode(kode.getValue());
		transaksi.setKeterangan(keterangan);
		transaksi.setLokasi(myLokasi);
		transaksi.setShift(myShift);
		transaksi.setPendaftaran((Pendaftaran) pendaftaran);
		transaksi.setBebas(bebas);

		transaksi.setKode(kode.getValue());
		transaksi.setLokasi(myLokasi);
		transaksi.setShift(myShift);

		if (transaksi.getId() != null) {
			Common.refreshUpdate(session, transaksi);
		} else {
			String mykode = Common.generateCode(TransaksiMedis.class, 8, "TRX", myLokasi);
			transaksi.setIndex(Common.generateMaxByLokasi(TransaksiMedis.class, myLokasi) + 1);
			kode.setValue(mykode);
			kode.setValue(transaksi.getKode());
			session.save(transaksi);
		}

		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransaksiMedis.class).add(Restrictions.eq("sumber", SUMBER))
				.createAlias("pendaftaran", "pendaftaran", Criteria.LEFT_JOIN)
				.createAlias("pasien", "pasien", Criteria.LEFT_JOIN)

				.add(searchkelas.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kelasPerawatan", searchkelas.getSelectedItem().getValue()))
				.add(searchruang.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.ruangPerawatan", searchruang.getSelectedItem().getValue()))
				.add(searchkamar.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.kamarPerawatan", searchkamar.getSelectedItem().getValue()))
				.add((searchbed == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchbed.getAttribute("tempatTidur") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pendaftaran.tempatTidur", searchbed.getAttribute("tempatTidur"))))
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))))
				.add((searchmr == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmr.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("pasien.kode", searchmr.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TransaksiMedis> transaksi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transaksi);
		grid.setRowRenderer(new TransaksiRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public Boolean checkKodeTransaksi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TransaksiMedis.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.transaksi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.transaksi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public void onBebas(Boolean checked) throws Exception {
		this.bebas = checked;

	}

	@Override
	public void onBerubah(Boolean bebas, Pendaftaran pendaftaran, Pasien pasien, String nama, Date tanggalTransaksi,
			KelasPerawatan kelasPerawatan, String keterangan) throws Exception {
		this.bebas = bebas;
		this.pendaftaran = pendaftaran;
		this.pasien = pasien;
		this.tanggalTransaksi = tanggalTransaksi;
		this.keterangan = keterangan;
		this.nama = nama;
		this.kelasPerawatan = kelasPerawatan;

		final TransaksiMedis mytransaksi = (TransaksiMedis) HibernateUtil.currentSession().createCriteria(TransaksiMedis.class)
				.add(Restrictions.eq("sumber", SUMBER)).add(Restrictions.eq("pendaftaran", pendaftaran))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

		if (mytransaksi != null && transaksi.getId() == null) {
			init(mytransaksi);
		} else {
			if (transaksi.getId() == null) {
				MyMessageboxConfig.showFormat("Mohon maaf, transaksi {V1} untuk pendaftaran ini tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan transaksi telah dibuat terlebih dahulu; (2) periksa kembali data pendaftaran pasien.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, SUMBER);
			}
		}

	}

}
