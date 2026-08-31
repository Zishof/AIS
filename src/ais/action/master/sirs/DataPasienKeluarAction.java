package ais.action.master.sirs;

import java.util.Calendar;
import java.util.List;

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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataPembayaranBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranRawatInapBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.DataPasienKeluar;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pembayaran;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.StatusPulang;
import ais.database.model.sirs.TempatTidur;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk data pasien keluar. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tabpanel tambahData}, {@code Grid
 * grid}, {@code Paging paging}, {@code MyTextbox searchnama}, {@code MyTextbox searchkode}, {@code MyDatebox
 * tanggalPulang}, {@code Combobox statusPulang}, {@code AmbilDataPendaftaranRawatInapBanbox pendaftaran};
 * inisialisasi/lifecycle ({@code doAfterCompose()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian
 * ({@code onSearchDefault()}); mutasi data ({@code onSave()}); penghapusan/pembatalan ({@code onDelete()});
 * operasi domain lain ({@code onAdd()}, {@code addExternal()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
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
public class DataPasienKeluarAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Tabpanel tambahData;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private MyTextbox searchkode;

	private MyDatebox tanggalPulang;
	private Combobox statusPulang;
	private AmbilDataPendaftaranRawatInapBanbox pendaftaran;
	private AmbilDataPembayaranBanbox pembayaran;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Label nama;
	private Label umur;
	// private Label pkkk;
	private Label alamat;
	private Label ttl;
	private Combobox jenisPasien;
	private Label jenisKelamin;

	private DataPasienKeluar dataPasienKeluar;
	private Toolbarbutton add;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		add = new ais.ui.util.MyToolbarbuttonConfig("Buat Pasien Keluar", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new DataPasienKeluar());

			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		init(new DataPasienKeluar());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public void onDelete(DataPasienKeluar dataPasienKeluar, Session session) {

		session.createSQLQuery("update sirs.pembayaran set data_pasien_keluar = null where data_pasien_keluar = "
				+ dataPasienKeluar.getId()).executeUpdate();

		session.createSQLQuery("update sirs.pendaftaran set data_pasien_keluar = null where data_pasien_keluar = "
				+ dataPasienKeluar.getId()).executeUpdate();

		Pendaftaran pendaftaran = dataPasienKeluar.getPendaftaran();
		if (pendaftaran != null) {
			session.refresh(pendaftaran);
			pendaftaran.setStatusPendaftaran(Pendaftaran.TERDAFTAR);
			pendaftaran.setTanggalKeluar(null);
			Common.refreshUpdate(session, pendaftaran);

			TempatTidur tempatTidur = pendaftaran.getTempatTidur();
			if (tempatTidur != null) {
				tempatTidur.setTerisi(true);
				Common.refreshUpdate(session, (tempatTidur));
			}

			List<DiagnosaPenyakit> diagnosaPenyakits = session.createCriteria(DiagnosaPenyakit.class)
					.add(Restrictions.eq("pendaftaran", pendaftaran)).list();
			for (DiagnosaPenyakit diagnosaPenyakit : diagnosaPenyakits) {
				diagnosaPenyakit.setTanggalPulang(null);
				diagnosaPenyakit.setStatusPulang(null);
				Common.refreshUpdate(session, (diagnosaPenyakit));
			}
		}

		Common.refreshDelete(session, dataPasienKeluar);

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link DataPasienKeluarAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link DataPasienKeluarAction} dan dapat mengakses
	 * state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see DataPasienKeluarAction
	 */
	class DataPasienKeluarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final DataPasienKeluar dataPasienKeluar = (DataPasienKeluar) arg1;

			new Label(dataPasienKeluar.getTanggalPulang() == null ? ""
					: Common.dateFormat3.get().format(dataPasienKeluar.getTanggalPulang())).setParent(arg0);
			RevisiHelper.createNewRevisi(DataPasienKeluar.class, dataPasienKeluar,
					dataPasienKeluar.getPendaftaran().getPasien().getKode()).setParent(arg0);
			new Label(dataPasienKeluar.getPendaftaran().getPasien().getNama()).setParent(arg0);
			new Label(dataPasienKeluar.getStatusPulang() == null ? "" : dataPasienKeluar.getStatusPulang().getNama())
					.setParent(arg0);

			new Label(dataPasienKeluar.getPembayaran() == null ? "" : dataPasienKeluar.getPembayaran().getKode())
					.setParent(arg0);

			Pendaftaran myPendaftaran = dataPasienKeluar.getPendaftaran();

			String bed = myPendaftaran == null ? ""
					: (myPendaftaran.getRuangPerawatan() == null ? "" : myPendaftaran.getRuangPerawatan().getNama())
							+ " - "
							+ (myPendaftaran.getKamarPerawatan() == null ? ""
									: myPendaftaran.getKamarPerawatan().getNama())
							+ " - "
							+ (myPendaftaran.getTempatTidur() == null ? "" : myPendaftaran.getTempatTidur().getNama());

			new Label(bed).setParent(arg0);

			new Label(dataPasienKeluar.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(dataPasienKeluar);

				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											onDelete(dataPasienKeluar, session);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
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

	private EventListener perubahanPasienListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Pendaftaran pendaftaran = (Pendaftaran) DataPasienKeluarAction.this.pendaftaran.getAttribute("pendaftaran");
			if (pendaftaran == null) {
				return;
			}
			Pasien pasien = pendaftaran.getPasien();
			nama.setValue(pasien == null ? "" : pasien.getNama());

			if (pasien.getTanggalLahir() != null) {
				Calendar tahunSkr = Calendar.getInstance();
				Calendar tahunLahir = Calendar.getInstance();
				tahunLahir.setTime(pasien.getTanggalLahir());
				Integer myumur = tahunSkr.get(Calendar.YEAR) - tahunLahir.get(Calendar.YEAR);
				umur.setValue(myumur + " thn");
			} else {
				umur.setValue("");
			}

			alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());
			ttl.setValue(pasien == null ? ""
					: (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + "/"
							+ (pasien.getTanggalLahir() == null ? ""
									: Common.dateFormat2.get().format(pasien.getTanggalLahir())));

			jenisKelamin
					.setValue(pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

			Common.selectComboItem(jenisPasien, pasien.getJenisPasien());

		}
	};
	private Window addWindow;
	private EventListener eventListener;

	public void onAdd(Event event) throws Exception {
		init(new DataPasienKeluar());
	}

	public static void addExternal(DataPasienKeluar dataPasienKeluar, EventListener eventListener) throws Exception {
		DataPasienKeluarAction dataPasienKeluarAction = new DataPasienKeluarAction();
		dataPasienKeluarAction.addWindow = new Window();
		dataPasienKeluarAction.addWindow.setTitle("Pendataan Pasien Keluar");
		dataPasienKeluarAction.eventListener = eventListener;
		dataPasienKeluarAction.addWindow.setHeight("400px");
		dataPasienKeluarAction.addWindow.setWidth("650px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(dataPasienKeluarAction.addWindow);
		dataPasienKeluarAction.init(dataPasienKeluar);
		dataPasienKeluarAction.addWindow.setVisible(true);
		dataPasienKeluarAction.addWindow.onModal();
		dataPasienKeluarAction.pendaftaran.setDisabled(true);
		dataPasienKeluarAction.pembayaran.setDisabled(true);
	}

	private void init(DataPasienKeluar dataPasienKeluar) throws Exception {
		this.dataPasienKeluar = dataPasienKeluar;

		Common.clear(tambahData == null ? addWindow : tambahData);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil Pasien Rawat Inap")));
		row.appendChild(pendaftaran = new AmbilDataPendaftaranRawatInapBanbox(true));
		pendaftaran.setAttribute("pendaftaran", dataPasienKeluar.getPendaftaran());
		pendaftaran
				.setValue(dataPasienKeluar.getPendaftaran() == null ? "" : dataPasienKeluar.getPendaftaran().getKode());
		pendaftaran.setWidth("90%");

		Pasien pasien = dataPasienKeluar.getPendaftaran() == null ? null
				: dataPasienKeluar.getPendaftaran().getPasien();

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Nama Pasien/Umur"));

		Hbox myHbox = new Hbox();
		row.appendChild(myHbox);
		myHbox.appendChild(nama = new Label(pasien == null ? "" : pasien.getNama()));
		myHbox.appendChild(new Label(" / "));
		myHbox.appendChild(umur = new Label(
				dataPasienKeluar.getPendaftaran() == null || dataPasienKeluar.getPendaftaran().getUmur() == null ? ""
						: dataPasienKeluar.getPendaftaran().getUmur() + " thn"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Keluar")));
		row.appendChild(tanggalPulang = new MyDatebox(dataPasienKeluar.getTanggalPulang()));
		tanggalPulang.setFormat(Common.dateFormat3.get().toPattern());
		tanggalPulang.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Status Keluar")));
		Common.insertCombo(statusPulang = new Combobox(), "nama", StatusPulang.class);
		row.appendChild(statusPulang);
		Common.selectComboItem(statusPulang, dataPasienKeluar.getStatusPulang());
		statusPulang.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Label(pasien == null ? "" : pasien.getAlamatLengkap()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("TTL")));
		row.appendChild(ttl = new Label(pasien == null ? ""
				: (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + "/"
						+ (pasien.getTanggalLahir() == null ? ""
								: Common.dateFormat2.get().format(pasien.getTanggalLahir()))));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Label(
				pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		Common.insertCombo(jenisPasien = new Combobox(), "nama", JenisPasien.class);
		Common.selectComboItem(jenisPasien,
				dataPasienKeluar.getPendaftaran() == null ? null : dataPasienKeluar.getPendaftaran().getJenisPasien());
		row.appendChild(jenisPasien);
		jenisPasien.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nota Pembayaran")));
		row.appendChild(pembayaran = new AmbilDataPembayaranBanbox());
		pembayaran.setValue(dataPasienKeluar.getPembayaran() == null ? "" : dataPasienKeluar.getPembayaran().getKode());
		pembayaran.setAttribute("pembayaran", dataPasienKeluar.getPembayaran());

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				dataPasienKeluar.getKeterangan() == null ? "" : dataPasienKeluar.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);

		if (add != null) {
			add.setParent(toolbar);
		}
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Data Pasien Keluar", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
				}
			}
		});
		save.setParent(toolbar);

		if (tambahData != null) {
			borderlayout.setParent(tambahData);
			tambahData.getLinkedTab().setSelected(true);
		} else {
			borderlayout.setParent(addWindow);
		}

		pendaftaran.setEventListener(perubahanPasienListener);
		perubahanPasienListener.onEvent(null);
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (pendaftaran.getAttribute("pendaftaran") == null) {
			MyMessageboxConfig.show("Mohon maaf, data pasien rawat inap wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih data pasien rawat inap pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (statusPulang.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, status keluar wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih status keluar pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggalPulang.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal keluar wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal keluar pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (dataPasienKeluar.getId() != null) {
			dataPasienKeluar = (DataPasienKeluar) session.load(DataPasienKeluar.class, dataPasienKeluar.getId());

		}

		dataPasienKeluar.setStatusPulang((StatusPulang) statusPulang.getSelectedItem().getValue());
		dataPasienKeluar.setTanggalPulang(tanggalPulang.getValue());
		dataPasienKeluar.setPendaftaran((Pendaftaran) pendaftaran.getAttribute("pendaftaran"));
		dataPasienKeluar.setKeterangan(keterangan.getValue());
		dataPasienKeluar.setPembayaran((Pembayaran) pembayaran.getAttribute("pembayaran"));

		Common.refreshSaveOrUpdate(session, dataPasienKeluar);

		Pendaftaran pendaftaran = dataPasienKeluar.getPendaftaran();
		session.refresh(pendaftaran);
		pendaftaran.setDataPasienKeluar(dataPasienKeluar);

		if (dataPasienKeluar.getStatusPulang().getId().equals(ConstantValues.STATUS_MENINGGAL.getId())) {
			pendaftaran.setStatusPendaftaran(Pendaftaran.MENINGGAL);
		} else {
			pendaftaran.setStatusPendaftaran(Pendaftaran.KELUAR);
		}

		pendaftaran.setTanggalKeluar(tanggalPulang.getValue());
		session.update(pendaftaran);

		TempatTidur tempatTidur = pendaftaran.getTempatTidur();
		session.refresh(tempatTidur);
		tempatTidur.updateTerisi();
		Common.refreshUpdate(session, (tempatTidur));

		List<DiagnosaPenyakit> diagnosaPenyakits = session.createCriteria(DiagnosaPenyakit.class)
				.add(Restrictions.eq("pendaftaran", pendaftaran)).list();
		for (DiagnosaPenyakit diagnosaPenyakit : diagnosaPenyakits) {
			diagnosaPenyakit.setTanggalPulang(dataPasienKeluar.getTanggalPulang());
			diagnosaPenyakit.setStatusPulang(dataPasienKeluar.getStatusPulang());
			Common.refreshUpdate(session, (diagnosaPenyakit));
		}

		MyMessageboxConfig.show("Data pasien keluar telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (eventListener != null) {
							eventListener.onEvent(new Event("event", null, dataPasienKeluar));
							addWindow.setVisible(false);
						}

						if (tambahData != null && add != null) {
							Common.freeze(tambahData, true);
							add.setDisabled(false);
						}

					}
				});

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchkode == null || searchnama == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<DataPasienKeluar> dataPasienKeluar = initCriteria(true)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dataPasienKeluar);
		grid.setRowRenderer(new DataPasienKeluarRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DataPasienKeluar.class)
				.createAlias("pendaftaran", "pendaftaran", Criteria.INNER_JOIN)
				.createAlias("pendaftaran.pasien", "pasien", Criteria.INNER_JOIN)

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("tanggalPulang"));

		return criteria;
	}

}
