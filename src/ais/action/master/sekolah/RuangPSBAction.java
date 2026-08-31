package ais.action.master.sekolah;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.psb.RuangPsbCalonSiswaDetailAction;
import ais.action.master.sekolah.psb.CommonReportPsb;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Gedung;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.database.model.sekolah.RuangPSB;
import ais.database.model.sekolah.UjianPSB;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk ruang psb. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox nama}, {@code Textbox
 * searchkodeRuangan}, {@code Textbox kodeRuangan}, {@code Textbox searchkapasitasruangan};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code getDataAlbumPSBAdmin()});
 * validasi/perhitungan ({@code cekRuanganIsi()}); mutasi data ({@code onSave()}); pelaporan/ekspor ({@code
 * onCetakAbsensi()}, {@code onCetakBau()}, {@code onCetakAlbum()}); operasi domain lain ({@code onAdd()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class RuangPSBAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Textbox searchnama;
	private Textbox nama;
	private Textbox searchkodeRuangan;
	private Textbox kodeRuangan;
	private Textbox searchkapasitasruangan;

	private Decimalbox kapasitasRuangan;
	private Combobox searchgedung;
	private Combobox searchUjianPSB;
	private Combobox gedung;
	private Combobox gelombangPendaftaranPsb;
	private Combobox searchgelombangPendaftaranPsb;
	private MyToolbarbuttonConfig add;
	private RuangPSB ruangPSB;
	private Combobox ujianPSB;

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
		Common.insertCombo(gedung = new Combobox(), "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchgedung, "nama", Gedung.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(gelombangPendaftaranPsb = new Combobox(), "nama", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchgelombangPendaftaranPsb, "nama", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		if (!searchgelombangPendaftaranPsb.getChildren().isEmpty()) {
			searchgelombangPendaftaranPsb.setSelectedIndex(0);
		}
		if (searchgelombangPendaftaranPsb != null) { searchgelombangPendaftaranPsb.setReadonly(true); }

		if (execution.getParameter("gelombangPendaftaranPsb") != null) {
			GelombangPendaftaranPsb gel = (GelombangPendaftaranPsb) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPsb.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPsb"))))
					.uniqueResult();
			if (gel != null) {
				Common.selectComboItem(true, searchgelombangPendaftaranPsb, gel);
				searchgelombangPendaftaranPsb.setDisabled(true);
			}
		}

		Common.insertCombo(searchUjianPSB, "nama", "gelombangPendaftaranPsb", UjianPSB.class);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Perbaiki Urutan Nomor Ujian di Ruang Ujian",
				"/img/svg/check2-circle.svg");
		if (button != null) { button.setParent(add.getParent()); }
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<RuangPSB> ruangPSBs = initCriteria(true).list();
						Session session = HibernateUtil.currentSession();

						RuangPSB ruangPSBData = !ruangPSBs.isEmpty() ? ruangPSBs.get(0) : null;

						if (ruangPSBData != null) {
							List<CalonSiswa> calonSiswasData = ConstantValues
									.simpleList(
											session.createCriteria(CalonSiswa.class)
													.add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
													.add(Restrictions.eq("gelombangPendaftaranPsb",
															ruangPSBData.getGelombangPendaftaranPsb())),
											CalonSiswa.class);

							System.out.println("calonSiswasData -> " + calonSiswasData.size());

							for (CalonSiswa calonSiswa : calonSiswasData) {
								RuangGelombangPendaftaranPsbPSB gelombangPendaftaranPsbPSB = (RuangGelombangPendaftaranPsbPSB) session
										.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
										.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();
								if (gelombangPendaftaranPsbPSB == null) {
									gelombangPendaftaranPsbPSB = new RuangGelombangPendaftaranPsbPSB();
								}
								gelombangPendaftaranPsbPSB.setCalonSiswa(calonSiswa);
								gelombangPendaftaranPsbPSB.setRuangPSB(ruangPSBData);
								Common.refreshUpdate(session, gelombangPendaftaranPsbPSB, false);
							}
							session.flush();

							for (RuangPSB ruangPSB : ruangPSBs) {
								int count = ((Number) session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
										.add(Restrictions.eq("ruangPSB", ruangPSB))
										.createAlias("calonSiswa", "calonSiswa")
										.add(Restrictions.ne("calonSiswa.nomorInduk", ""))
										.add(Restrictions.isNotNull("calonSiswa.nomorInduk"))
										.setProjection(Projections.rowCount()).uniqueResult()).intValue();
								if (ruangPSB.getKapasitasRuangan().intValue() != count) {
									List<RuangPSB> ruangIniDanSelanjutnya = initCriteria(false)
											.addOrder(Order.asc("id")).add(Restrictions.ge("id", ruangPSB.getId()))
											.list();
									if (!ruangIniDanSelanjutnya.isEmpty()) {
										List<CalonSiswa> calonSiswas = session
												.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
												.createAlias("calonSiswa", "calonSiswa")
												.add(Restrictions.ne("calonSiswa.nomorInduk", ""))
												.add(Restrictions.isNotNull("calonSiswa.nomorInduk"))
												.setProjection(Projections.property("calonSiswa"))
												.add(Restrictions.in("ruangPSB", ruangIniDanSelanjutnya))
												.addOrder(Order.asc("calonSiswa.nomorInduk")).list();
										int jumlahTotal = 0;
										for (RuangPSB psb : ruangIniDanSelanjutnya) {
											for (int i = 0; i < psb.getKapasitasRuangan(); i++) {
												if (jumlahTotal < calonSiswas.size()) {
													CalonSiswa calonSiswa = calonSiswas.get(jumlahTotal);
													RuangGelombangPendaftaranPsbPSB ruangGelombangPendaftaranPsbPSB = (RuangGelombangPendaftaranPsbPSB) session
															.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
															.add(Restrictions.eq("calonSiswa", calonSiswa))
															.setMaxResults(1).uniqueResult();
													if (ruangGelombangPendaftaranPsbPSB != null) {
														ruangGelombangPendaftaranPsbPSB.setRuangPSB(psb);
														Common.refreshSaveOrUpdate(session,
																ruangGelombangPendaftaranPsbPSB);
													}
												}
												jumlahTotal++;
											}
										}
									}
									break;
								}
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
			}
		});
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link RuangPSBAction}. Kelas ini menerjemahkan satu item data menjadi
	 * baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link RuangPSBAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see RuangPSBAction
	 */
	class RuangPSBRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final RuangPSB ruangPSB = (RuangPSB) arg1;

			new RuangPsbCalonSiswaDetailAction(ruangPSB).setParent(arg0);

			Integer isi = cekRuanganIsi(ruangPSB);

			if (ruangPSB.getPenuh().equals(0) && isi.equals(ruangPSB.getKapasitasRuangan())) {
				ruangPSB.setPenuh(1);
				Common.refreshUpdate(ruangPSB);
			}

			RevisiHelper.createNewRevisi(RuangPSB.class, ruangPSB, ruangPSB.getNama()).setParent(arg0);

			new Label(ruangPSB.getKodeRuangan()).setParent(arg0);
			new Label(ruangPSB.getGedung() == null ? "" : ruangPSB.getGedung().getNama()).setParent(arg0);
			new Label(
					ruangPSB.getKapasitasRuangan() == null ? "" : ruangPSB.getKapasitasRuangan().toString() + "/" + isi)
					.setParent(arg0);
			new Label(ruangPSB.getGelombangPendaftaranPsb() == null ? ""
					: ruangPSB.getGelombangPendaftaranPsb().getNama()).setParent(arg0);
			new Label(ruangPSB.getUjianPSB() == null ? "" : ruangPSB.getUjianPSB().getNama()).setParent(arg0);
			new Label(ruangPSB.getTahun() == null ? "" : ruangPSB.getTahun().toString()).setParent(arg0);
			new Label(ruangPSB.getTahunAkademik()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Penuh");
			checkbox.setChecked(ruangPSB.getPenuh().equals(1));
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					ruangPSB.setPenuh(checkbox.isChecked() ? 1 : 0);
					Common.refreshSaveOrUpdate(ruangPSB);
				}
			});

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Ubah Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(ruangPSB);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE));
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
											Common.refreshDelete(ruangPSB);
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

			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Absensi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPsb.onCetakAbsensiPSB(ruangPSB);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Verifikasi", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Verifikasi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					CommonReportPsb.onCetakVerifikasiPSB(ruangPSB);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Berita Acara", "/img/album.png");
			button.setOrient("vertical");
			button.setTooltiptext("Berita Acara Ujian");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					onCetakBau(ruangPSB);
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Cover Album", "/img/album_pmb.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cover Album");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub
					onCetakAbsensi(ruangPSB);
				}
			});

			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Album Absensi", "/img/absensi_pmb.png");
			button.setOrient("vertical");
			button.setTooltiptext("Album Absensi");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onCetakAlbum(ruangPSB);
				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new RuangPSB());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(RuangPSB ruangPSB) {
		this.ruangPSB = ruangPSB;
		addWindow.setTitle(ruangPSB.getId() == null ? "Tambah Ruang PSB" : "Ubah Ruang PSB");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang untuk ujian"));
		row.appendChild(ujianPSB = new Combobox());
		Common.insertCombo(ujianPSB, "nama", "gelombangPendaftaranPsb", UjianPSB.class,
				searchgelombangPendaftaranPsb.getSelectedItem() == null
						|| searchgelombangPendaftaranPsb.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("gelombangPendaftaranPsb",
										searchgelombangPendaftaranPsb.getSelectedItem().getValue()));
		Common.selectComboItem(ujianPSB, ruangPSB.getUjianPSB());
		ujianPSB.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Ruangan"));
		row.appendChild(kodeRuangan = new Textbox(ruangPSB.getKodeRuangan() == null ? "" : ruangPSB.getKodeRuangan()));
		kodeRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Ruangan"));
		row.appendChild(nama = new Textbox(ruangPSB.getNama() == null ? "" : ruangPSB.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gedung"));
		Common.selectComboItem(gedung, ruangPSB.getGedung());
		row.appendChild(gedung);
		gedung.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kapasitas Ruangan"));
		row.appendChild(kapasitasRuangan = new Decimalbox(
				new BigDecimal(ruangPSB.getKapasitasRuangan() == null ? 30 : ruangPSB.getKapasitasRuangan())));
		kapasitasRuangan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang"));
		gelombangPendaftaranPsb.setDisabled(false);
		Common.selectComboItem(gelombangPendaftaranPsb,
				ruangPSB.getGelombangPendaftaranPsb() == null ? null : ruangPSB.getGelombangPendaftaranPsb());
		row.appendChild(gelombangPendaftaranPsb);
		gelombangPendaftaranPsb.setWidth("90%");
		gelombangPendaftaranPsb.setReadonly(true);

		if (ruangPSB.getId() != null) {
			if (cekRuanganIsi(ruangPSB) > 0) {
				gelombangPendaftaranPsb.setDisabled(true);
			} else {
				gelombangPendaftaranPsb.setDisabled(false);
			}
		}

		if (searchgelombangPendaftaranPsb.getSelectedItem() != null
				&& searchgelombangPendaftaranPsb.getSelectedItem().getValue() != null) {
			Common.selectComboItem(gelombangPendaftaranPsb, searchgelombangPendaftaranPsb.getSelectedItem().getValue());
			gelombangPendaftaranPsb.setDisabled(searchgelombangPendaftaranPsb.isDisabled());

		}

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

		if (ujianPSB.getSelectedItem() == null) {
			MyMessageboxConfig.show("Ujian harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (kodeRuangan.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Ruangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (gedung.getSelectedItem() == null) {
			MyMessageboxConfig.show("Gedung harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kapasitasRuangan.getValue() == null) {
			MyMessageboxConfig.show("Kapasitas Ruangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (ruangPSB.getId() != null) {
			ruangPSB = (RuangPSB) session.load(RuangPSB.class, ruangPSB.getId());
		}
		ruangPSB.setUjianPSB((UjianPSB) ujianPSB.getSelectedItem().getValue());
		ruangPSB.setNama(nama.getValue());
		ruangPSB.setKodeRuangan(kodeRuangan.getValue());
		ruangPSB.setGedung((Gedung) (gedung.getSelectedItem() == null ? null : gedung.getSelectedItem().getValue()));
		ruangPSB.setKapasitasRuangan(
				kapasitasRuangan.getValue() == null ? null : Integer.parseInt(kapasitasRuangan.getValue().toString()));
		ruangPSB.setGelombangPendaftaranPsb(
				(GelombangPendaftaranPsb) (gelombangPendaftaranPsb.getSelectedItem() == null ? null
						: gelombangPendaftaranPsb.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, ruangPSB);
		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RuangPSB.class);
		if (order)
			criteria.addOrder(Order.asc("id"));
		criteria

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("kodeRuangan", searchkodeRuangan.getValue(), MatchMode.ANYWHERE))
				.add(searchkapasitasruangan.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kapasitasRuangan",
								Integer.parseInt(searchkapasitasruangan.getValue().toString())))
				.add(searchgelombangPendaftaranPsb.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gelombangPendaftaranPsb",
								searchgelombangPendaftaranPsb.getSelectedItem().getValue()))
				.add(searchgedung.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("gedung", searchgedung.getSelectedItem().getValue()))
				.add(searchUjianPSB.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ujianPSB", searchUjianPSB.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging(initCriteria(false), paging);

		List<RuangPSB> ruangPSB = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(ruangPSB);
		grid.setRowRenderer(new RuangPSBRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Integer cekRuanganIsi(RuangPSB ruangPSB) {
		Integer count = 0;
		Session session = HibernateUtil.currentSession();
		session.refresh(ruangPSB);
		// List<RuangGelombangPendaftaranPsbPSB>
		// ruangGelombangPendaftaranPsbPSBs = session.createCriteria(
		// RuangGelombangPendaftaranPsbPSB.class).add(Restrictions.eq("ruangPSB",
		// ruangPSB))
		// .list();

		count = ((Number) session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
				.add(Restrictions.eq("ruangPSB", ruangPSB)).createAlias("calonSiswa", "calonSiswa")
				.add(Restrictions.ne("calonSiswa.nomorInduk", "")).add(Restrictions.isNotNull("calonSiswa.nomorInduk"))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		// System.out.println(ruangPSB.getNama() + " ruang bawah");
		// isi = ruangGelombangPendaftaranPsbPSBs.size();
		System.out.println("Jumlah isi ruang : " + count);
		return count;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakAbsensi(RuangPSB ruang) throws Exception {

		this.ruangPSB = ruang;
		final Map parameters = ais.common.HashMapGenerator.getRand();

		System.out.println("ruang cetak absensi " + ruang.getId());
		parameters.put("ruang", ruang.getId());
		parameters.put("sekolah_id", ruang.getGelombangPendaftaranPsb().getSekolah().getId());
		Report.generatePDFReport(Report.PDF, parameters, "Coverspsbi", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakBau(RuangPSB ruang) throws Exception {

		this.ruangPSB = ruang;
		final Map parameters = ais.common.HashMapGenerator.getRand();

		System.out.println("ruang cetak Bau " + ruang.getId());
		parameters.put("ruang", ruang.getId());
		parameters.put("sekolah_id", ruang.getGelombangPendaftaranPsb().getSekolah().getId());
		Report.generatePDFReport(Report.PDF, parameters, "BeritaAcaraUjianPSB", ais.ui.util.WaktuUtil.getDate());

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakAlbum(RuangPSB ruang) throws Exception {

		this.ruangPSB = ruang;
		// final Map<String, Long> parameters = new HashMap<String, Long>();
		final Map parameters = ais.common.HashMapGenerator.getRand();
		List<Map<String, Object>> maps = getDataAlbumPSBAdmin(ruang);
		parameters.put("ujian", ruang.getUjianPSB() == null ? -1L : ruang.getUjianPSB().getId());
		parameters.put("ruang", ruang.getId());
		parameters.put("tahunakademik", ruang.getTahunAkademik());
		parameters.put("gelombang_pendaftaran",
				ruang.getUjianPSB() == null || ruang.getUjianPSB().getGelombangPendaftaranPsb() == null ? ""
						: ruang.getUjianPSB().getGelombangPendaftaranPsb().getNama());
		parameters.put("ket_ruang",
				ruang.getNama() + " ( " + (ruangPSB.getGedung() == null ? "" : ruang.getGedung().getNama()) + " )");
		parameters.put("sekolah_id", ruang.getGelombangPendaftaranPsb().getSekolah().getId());
		System.out.println("Cetak Album PSB gelombangPendaftaranPsb " + ruang.getGelombangPendaftaranPsb().getNama()
				+ " ruang " + ruang.getNama());

		parameters.put("gelombangPendaftaranPsb", ruang.getGelombangPendaftaranPsb().getNama());
		Report.generatePDFReport("pdf", parameters, "AlbumPSBHari", ais.ui.util.WaktuUtil.getDate(), maps);

	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getDataAlbumPSBAdmin(RuangPSB ruang) throws Exception {
		this.ruangPSB = ruang;
		Session session = HibernateUtil.currentSession();
		List<RuangGelombangPendaftaranPsbPSB> listPendaftaranWisuda = session
				.createCriteria(RuangGelombangPendaftaranPsbPSB.class).createAlias("calonSiswa", "calonSiswa")
				.add(Restrictions.ne("calonSiswa.nomorInduk", "")).add(Restrictions.isNotNull("calonSiswa.nomorInduk"))
				.addOrder(Order.asc("id")).add(Restrictions.eq("ruangPSB", ruang)).list();

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		Iterator<?> itr = listPendaftaranWisuda.iterator();

		try {

			while (itr.hasNext()) {
				RuangGelombangPendaftaranPsbPSB beanPendaftaranWisuda = (RuangGelombangPendaftaranPsbPSB) itr.next();
				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("nama", beanPendaftaranWisuda.getCalonSiswa().getNama().toUpperCase());
				map.put("no_ujian", beanPendaftaranWisuda.getCalonSiswa().getNomorInduk());
				map.put("ttl", beanPendaftaranWisuda.getCalonSiswa().getTempatLahir().toUpperCase() + " / "
						+ Common.dateFormat2.get().format(beanPendaftaranWisuda.getCalonSiswa().getTanggalLahir()));
				map.put("kelamin", beanPendaftaranWisuda.getCalonSiswa().getJenisKelamin());

				map.put("gelombang_pendaftaran",
						beanPendaftaranWisuda.getCalonSiswa().getGelombangPendaftaranPsb().getNama());
				map.put("alamat", beanPendaftaranWisuda.getCalonSiswa().getAlamatSiswa());
				map.put("prodi_1", beanPendaftaranWisuda.getCalonSiswa().getSekolah().getNama());
				map.put("prodi_2", beanPendaftaranWisuda.getCalonSiswa().getYayasan().getNama());

				beanPendaftaranWisuda.getCalonSiswa().putPhoto(map);

				maps.add(map);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return maps;
	}

}
