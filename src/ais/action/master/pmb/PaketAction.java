package ais.action.master.pmb;

import java.util.List;

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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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

import ais.action.master.helper.DetailPaketJurusanPmbHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.PaketDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Paket;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk paket. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code Textbox kode}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code Textbox kelasVerifikasiRapor}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}); pembacaan/pencarian ({@code
 * onTampilkanParameter()}, {@code onSearchDefault()}); validasi/perhitungan ({@code checkNamaPaket()}); mutasi
 * data ({@code onSave()}); operasi domain lain ({@code onPendidikanOrtu()}, {@code onMatapelajaranSekolah()},
 * {@code onParameterVerifikasi()}, {@code onKartuIdentitas()}, {@code onJenisSekolah()}, {@code
 * onJurusanSekolah()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class PaketAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private Textbox kelasVerifikasiRapor;
	private Intbox jumlahProdiYgBolehDiambil;
	private MyCheckboxConfig bisaDipilihSemuaGelombang;
	private MyCheckboxConfig bisaMemilihPilihanYangSama;
	private MyCheckboxConfig biayaPendaftaranSemuaGelombangSama;
	private PerguruanTinggi selectedPerguruanTinggi;
	private boolean edit = false;
	private boolean delete = false;

	private Paket paket;
	private MyToolbarbuttonConfig add;

	private Tabpanel tampilkanPedidikanOrtu;

	public void onPendidikanOrtu(Event event) {
		if (tampilkanPedidikanOrtu.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanPedidikanOrtu);
			MyInclude iframe = new MyInclude("/pages/master/pendidikan_orangtua.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tampilkanParameter;

	public void onTampilkanParameter(Event event) {
		if (tampilkanParameter.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanParameter);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_paket.zul");
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

	private Tabpanel tampilkanParameterVerifikasi;

	public void onParameterVerifikasi(Event event) {
		if (tampilkanParameterVerifikasi.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanParameterVerifikasi);
			MyInclude iframe = new MyInclude("/pages/master/parameter_verifikasi_calon_mahasiswa.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tampilkanKartuIdentitas;

	public void onKartuIdentitas(Event event) {
		if (tampilkanKartuIdentitas.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanKartuIdentitas);
			MyInclude iframe = new MyInclude("/pages/master/jenis_kartu_identitas.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tampilkanJenisSekolah;

	public void onJenisSekolah(Event event) {
		if (tampilkanJenisSekolah.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanJenisSekolah);
			MyInclude iframe = new MyInclude("/pages/master/jenis_sekolah.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tampilkanJurusanSekolah;

	public void onJurusanSekolah(Event event) {
		if (tampilkanJurusanSekolah.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanJurusanSekolah);
			MyInclude iframe = new MyInclude("/pages/master/jurusan_sekolah.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tampilkanNamaSekolah;

	public void onNamaSekolah(Event event) {
		if (tampilkanNamaSekolah.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanNamaSekolah);
			MyInclude iframe = new MyInclude("/pages/master/nama_sekolah_asal.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tampilkanPekerjaanOrtu;

	public void onPekerjaanOrtu(Event event) {
		if (tampilkanPekerjaanOrtu.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanPekerjaanOrtu);
			MyInclude iframe = new MyInclude("/pages/master/pekerjaan_orangtua.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel tampilkanPendapatanOrtu;
	private MyCheckboxConfig wajibUploadFoto;

	public void onPendapatanOrtu(Event event) {
		if (tampilkanPendapatanOrtu.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(tampilkanPendapatanOrtu);
			MyInclude iframe = new MyInclude("/pages/master/pendapatan_orangtua.zul");
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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		selectedPerguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
	}

	class PaketRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Paket paket = (Paket) arg1;

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

						final MyTabConfig tabSoal = new MyTabConfig("Program Studi");
						tabSoal.setParent(tabs);

						MyTabConfig tabJawaban = new MyTabConfig("Jurusan Pendidikan Sebelumnya");
						tabJawaban.setParent(tabs);

						MyTabConfig tabProgram = new MyTabConfig("Program");
						tabProgram.setParent(tabs);

						MyTabConfig tabParameterTambahan = new MyTabConfig("Parameter Tambahan");
						tabParameterTambahan.setParent(tabs);

						MyTabConfig tabGelombangPendaftaran = new MyTabConfig("Gelombang Pendaftaran");
						tabGelombangPendaftaran.setParent(tabs);

						MyTabConfig tabPersyaratan = new MyTabConfig("Persyaratan pilihan dengan kombinasi paket");
						tabPersyaratan.setParent(tabs);

						MyTabConfig tabRapor = new MyTabConfig("Verifikasi Nilai Rapor");
						tabRapor.setParent(tabs);

						MyTabConfig tabParameter = new MyTabConfig("Parameter Verifikasi");
						tabParameter.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
						tabpanelUtama.setParent(tabpanels);

						DetailPaketJurusanPmbHelper detailPaketJurusanPmbHelper = new DetailPaketJurusanPmbHelper();
						detailPaketJurusanPmbHelper.displayDetailPaketJurusan(paket, tabpanelUtama, addWindow);

						final Tabpanel jurusanTabpanel = new ais.ui.util.MyTabpanel();
						jurusanTabpanel.setParent(tabpanels);
						jurusanTabpanel.setHeight("1500px");
						jurusanTabpanel.setWidth("100%");

						tabJawaban.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (jurusanTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/master/paket_registrasi_mahasiswa.zul?paket=" + paket.getId());
									iframe.setHeight("1500px");
									iframe.setWidth("100%");

									iframe.setParent(jurusanTabpanel);

								}
							}
						});

						final Tabpanel programTabpanel = new ais.ui.util.MyTabpanel();
						programTabpanel.setParent(tabpanels);
						programTabpanel.setHeight("1500px");
						programTabpanel.setWidth("100%");

						tabProgram.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (programTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/master/paket_punya_program.zul?paket=" + paket.getId());
									iframe.setHeight("1500px");
									iframe.setWidth("100%");

									iframe.setParent(programTabpanel);

								}
							}
						});

						final Tabpanel parameterTambahanTabpanel = new ais.ui.util.MyTabpanel();
						parameterTambahanTabpanel.setParent(tabpanels);
						parameterTambahanTabpanel.setHeight("1500px");
						parameterTambahanTabpanel.setWidth("100%");

						tabParameterTambahan.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (parameterTambahanTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/master/parameter_tambahan_paket.zul?paket=" + paket.getId());
									iframe.setHeight("1500px");
									iframe.setWidth("100%");

									iframe.setParent(parameterTambahanTabpanel);

								}
							}
						});

						final Tabpanel gelombangPendaftaranTabpanel = new ais.ui.util.MyTabpanel();
						gelombangPendaftaranTabpanel.setParent(tabpanels);
						gelombangPendaftaranTabpanel.setHeight("1500px");
						gelombangPendaftaranTabpanel.setWidth("100%");

						tabGelombangPendaftaran.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (gelombangPendaftaranTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/master/paket_punya_gelombang_pendaftaran.zul?paket="
													+ paket.getId());
									iframe.setHeight("1500px");
									iframe.setWidth("100%");

									iframe.setParent(gelombangPendaftaranTabpanel);

								}
							}
						});

						final Tabpanel persyaratanTabpanel = new ais.ui.util.MyTabpanel();
						persyaratanTabpanel.setParent(tabpanels);
						persyaratanTabpanel.setHeight("1500px");
						persyaratanTabpanel.setWidth("100%");

						tabPersyaratan.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (persyaratanTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/master/persyaratan_pilihan_paket.zul?paket=" + paket.getId());
									iframe.setHeight("1500px");
									iframe.setWidth("100%");

									iframe.setParent(persyaratanTabpanel);

								}
							}
						});

						final Tabpanel raporTabpanel = new ais.ui.util.MyTabpanel();
						raporTabpanel.setParent(tabpanels);
						raporTabpanel.setHeight("1500px");
						raporTabpanel.setWidth("100%");
						tabRapor.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (raporTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/master/paket_punya_matapelajaran.zul?paket=" + paket.getId());
									iframe.setHeight("1500px");
									iframe.setWidth("100%");

									iframe.setParent(raporTabpanel);

								}
							}
						});

						final Tabpanel parameterTabpanel = new ais.ui.util.MyTabpanel();
						parameterTabpanel.setParent(tabpanels);
						parameterTabpanel.setHeight("1500px");
						parameterTabpanel.setWidth("100%");

						tabParameter.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (parameterTabpanel.getChildren().isEmpty()) {

									MyInclude iframe = new MyInclude(
											"/pages/master/paket_punya_parameter_verifikasi_calon_mahasiswa.zul?paket="
													+ paket.getId());
									iframe.setHeight("1500px");
									iframe.setWidth("100%");

									iframe.setParent(parameterTabpanel);

								}
							}
						});

					}
				}
			});

			RevisiHelper.createNewRevisi(Paket.class, paket, paket.getNama()).setParent(arg0);

			new Label(paket.getKode()).setParent(arg0);
			new Label(paket.getJumlahProdiYgBolehDiambil() + "").setParent(arg0);
			new Label(paket.getBisaDipilihSemuaGelombang() ? "Ya" : "Tidak").setParent(arg0);
			new Label(paket.getBisaMemilihPilihanYangSama() ? "Ya" : "Tidak").setParent(arg0);
			new Label(paket.getKelasVerifikasiRapor()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(paket.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					paket.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(paket);
				}
			});

			new Label(paket.getKeterangan() == null ? "" : paket.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(paket);
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
											PaketDao paketDao = DaoFactory.getInstance().getPaketDao();
											// agamaDao.beginTransaction();
											paketDao.delete(paketDao.merge(paket));
											// agamaDao.commitTransaction();
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
		init(new Paket());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Paket paket) {
		this.paket = paket;
		addWindow.setTitle(paket.getId() == null ? "Tambah Paket" : "Ubah Paket");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Paket"));
		row.appendChild(nama = new Textbox(paket.getNama() == null ? "" : paket.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Paket"));
		row.appendChild(kode = new Textbox(paket.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah pilihan yang boleh diambil"));
		row.appendChild(jumlahProdiYgBolehDiambil = new Intbox(paket.getJumlahProdiYgBolehDiambil()));
		jumlahProdiYgBolehDiambil.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bisa dipilih semua gelombang pendaftaran"));
		row.appendChild(bisaDipilihSemuaGelombang = new MyCheckboxConfig());
		bisaDipilihSemuaGelombang.setChecked(paket.getBisaDipilihSemuaGelombang());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Bisa memilih pilihan " + Common.getBahasaConfig("Jurusan") + " yang sama"));
		row.appendChild(bisaMemilihPilihanYangSama = new MyCheckboxConfig());
		bisaMemilihPilihanYangSama.setChecked(paket.getBisaMemilihPilihanYangSama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semua gelombang mempunyai tagihan yang sama"));
		row.appendChild(biayaPendaftaranSemuaGelombangSama = new MyCheckboxConfig());
		biayaPendaftaranSemuaGelombangSama.setChecked(paket.getBiayaPendaftaranSemuaGelombangSama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"Mahasiswa wajib mengupload foto saat login di pendaftaran calon mahasiswa"));
		row.appendChild(wajibUploadFoto = new MyCheckboxConfig());
		wajibUploadFoto.setChecked(paket.getWajibUploadFoto());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas Verifikasi Rapor"));
		row.appendChild(kelasVerifikasiRapor = new Textbox(paket.getKelasVerifikasiRapor()));
		kelasVerifikasiRapor.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(paket.getKeterangan() == null ? "" : paket.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		if (Common.bolehKonfigurasi("keterangan_paket_digunakan_sebagai_info_kelulusan_jika_diisi", Konfigurasi.TIDAK_AKTIF)) {
			Common.initKeterangan(rows,
					"Keterangan akan tampil di informasi lanjutan apabila mahasiswa dinyatakan lulus. Jika keterangan kosong, akan tampil info default.");
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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Paket belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Paket dengan nama paket pendaftaran yang sesuai; (2) pastikan kolom tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Paket belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Paket dengan nama paket pendaftaran yang sesuai; (2) pastikan kolom tidak kosong atau hanya spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		/*
		 * if (keterangan.getValue().trim().equals("")) { MyMessageboxConfig.show(
		 * "Keterangan harus diisi", "Peringatan", MyMessageboxConfig.OK,
		 * MyMessageboxConfig.INFORMATION); return false; }
		 */

		boolean i = checkNamaPaket();
		if (i) {
			MyMessageboxConfig.show("Nama Paket sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		PaketDao paketDao = DaoFactory.getInstance().getPaketDao();
		if (paket.getId() != null) {
			paket = paketDao.load(paket.getId());

		}

		paket.setNama(nama.getValue());
		paket.setKode(kode.getValue().trim());
		paket.setKeterangan(keterangan.getValue() == null ? "" : keterangan.getValue());
		paket.setJumlahProdiYgBolehDiambil(jumlahProdiYgBolehDiambil.getValue());
		paket.setBisaDipilihSemuaGelombang(bisaDipilihSemuaGelombang.isChecked());
		paket.setBisaMemilihPilihanYangSama(bisaMemilihPilihanYangSama.isChecked());
		paket.setBiayaPendaftaranSemuaGelombangSama(biayaPendaftaranSemuaGelombangSama.isChecked());
		paket.setKelasVerifikasiRapor(kelasVerifikasiRapor.getValue());
		paket.setWajibUploadFoto(wajibUploadFoto.isChecked());
		paket.setPerguruanTinggi(selectedPerguruanTinggi);

		// agamaDao.beginTransaction();
		if (paket.getId() != null) {
			paketDao.update(paket);
		} else {
			paketDao.save(paket);
		}
		// agamaDao.commitTransaction();
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		List<Paket> paket = session.createCriteria(Paket.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.addOrder(Order.asc("id"))
				.add(selectedPerguruanTinggi == null || selectedPerguruanTinggi.getId() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("perguruanTinggi", selectedPerguruanTinggi),
								Restrictions.isNull("perguruanTinggi")))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(paket);
		grid.setRowRenderer(new PaketRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaPaket() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Paket.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.paket.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.paket.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
