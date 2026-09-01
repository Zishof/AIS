package ais.action.master.helper;
import ais.common.PesanFormalHelper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.MasaPerkuliahanAction;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Implementasi pola "Bandbox picker" AIS untuk entity {@link ais.database.model.MasaPerkuliahan}
 * — lihat {@link ais.ui.util.GetEventListener} untuk arsitektur kerangka umum
 * (constructor/display/onSearchDefault/renderer/callback).
 * <p>
 * {@code MasaPerkuliahan} adalah master data periode akademik (mis. rentang tanggal "Semester
 * Ganjil 2024/2025"), opsional dikaitkan ke {@code Jurusan}/{@code Fakultas}/program tertentu
 * (kosong berarti "Semua"), dan bisa ditandai {@code defaultData} sebagai periode aktif sistem
 * saat ini. KHAS kelas ini: constructor LANGSUNG mem-preselect {@link MasaPerkuliahan} yang
 * {@code defaultData == true} (query terpisah dari popup, dieksekusi sebelum popup pernah dibuka)
 * — Bandbox sudah terisi nilai default begitu instance dibuat, sebelum pengguna membuka apa pun.
 * Popup pencarian menyediakan field {@code nama}, Combobox fakultas/tahun akademik/program/prodi,
 * dan checkbox {@code tampilkanHanyaYgAktif} (default tercentang). Filter jurusan/program/fakultas
 * SELALU digabung {@code Restrictions.or(isNull(...), ...)} agar entri "Semua" tetap tampil.
 * Constructor menerima {@link Jurusan} induk opsional yang mengunci Combobox fakultas dan prodi
 * (sama seperti pola {@link AmbilDataJamPerkuliahanBanbox}). Sama seperti
 * {@link AmbilDataJamPerkuliahanBanbox}, popup ini juga menyediakan aksi CRUD langsung (tombol
 * Tambah/Ubah/Hapus, checkbox Aktif/Default) — tapi di sini akses diatur dua lapis: konfigurasi
 * {@code masa_perkuliahan_hanya_boleh_diubah_oleh_admin} (bila aktif, hanya admin yang bisa ubah)
 * DAN jenis pengguna (mahasiswa/siswa/dosen selalu TIDAK bisa ubah, terlepas dari privilese lain).
 * Toggle checkbox "Default" pada satu baris otomatis meng-unset flag default pada SEMUA baris lain
 * lewat SQL langsung. Pemilihan bersifat TUNGGAL (Radiogroup).
 * </p>
 *
 * @see Bandbox
 */
public class AmbilDataMasaPerkuliahanBanbox extends Bandbox implements GetEventListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 6452451056684904810L;
	private MyGrid grid;

	private EventListener eventListener;
	private Jurusan jurusan;

	private Tbmuser tbmuser = Common.getCurrentUser();
	private boolean masaPerkuliahanHanyaBolehDiubahOlheAdmin = false;

	/**
	 * Konstruktor default, mendelegasikan ke {@link #AmbilDataMasaPerkuliahanBanbox(Jurusan)}
	 * dengan {@code jurusan = null} (tidak dikunci ke prodi tertentu).
	 */
	public AmbilDataMasaPerkuliahanBanbox() {
		this(null);
	}

	/**
	 * Konstruktor dengan filter opsional dari entity induk {@link Jurusan}: bila diisi, Combobox
	 * fakultas dan prodi di popup pencarian dikunci ke fakultas/prodi jurusan tersebut. Membaca
	 * konfigurasi {@code masa_perkuliahan_hanya_boleh_diubah_oleh_admin} untuk menentukan hak edit
	 * di {@link #display()}/{@link MasaPerkuliahanRenderer}, memasang listener {@code onOpen}
	 * standar, lalu — KHAS constructor ini — LANGSUNG mengeksekusi query terpisah untuk mencari
	 * {@link MasaPerkuliahan} dengan {@code defaultData == true} dan mem-preselect-nya ke Bandbox
	 * (tanpa menunggu popup dibuka).
	 *
	 * @param jurusan prodi induk untuk mengunci pencarian, atau {@code null} untuk pencarian bebas
	 */
	public AmbilDataMasaPerkuliahanBanbox(Jurusan jurusan) {
		super();
		masaPerkuliahanHanyaBolehDiubahOlheAdmin = Common.bolehKonfigurasi("masa_perkuliahan_hanya_boleh_diubah_oleh_admin", Konfigurasi.TIDAK_AKTIF);

		this.jurusan = jurusan;
		setReadonly(true);
		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}
			}
		});

		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) HibernateUtil.currentSession()
				.createCriteria(MasaPerkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("defaultData", true)).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		if (masaPerkuliahan != null) {
			AmbilDataMasaPerkuliahanBanbox.this.setOpen(false);
			AmbilDataMasaPerkuliahanBanbox.this.setAttribute("masaPerkuliahan", masaPerkuliahan);
			AmbilDataMasaPerkuliahanBanbox.this.setAttribute("myValue", masaPerkuliahan);
			AmbilDataMasaPerkuliahanBanbox.this.setValue(masaPerkuliahan.getNama());
		}
	}

	/**
	 * Mengganti prodi induk yang mengunci pencarian setelah instance dibuat: mengosongkan nilai
	 * dan atribut Bandbox saat ini, memberi tahu {@link #eventListener} tentang pengosongan
	 * tersebut, mereset Combobox fakultas/prodi, lalu membangun ulang popup lewat
	 * {@link #display()} dengan {@link Jurusan} baru. Bukan bagian kerangka standar — method
	 * spesifik entity ini untuk kasus form yang mengganti prodi pilihan setelah Bandbox terpasang.
	 *
	 * @param jurusan prodi induk baru untuk mengunci pencarian
	 * @throws Exception diteruskan dari {@link EventListener#onEvent(Event)} milik
	 *                    {@link #eventListener}
	 */
	public void setJurusan(Jurusan jurusan) throws Exception {

		setValue("");
		setAttribute("masaPerkuliahan", null);
		setAttribute("myValue", null);
		eventListener.onEvent(null);
		Common.clear(this);
		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		this.jurusan = jurusan;
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		display();
	}

	/** Kriteria pencarian: nama masa perkuliahan (ilike, substring). */
	private Textbox nama;

	/** Kriteria pencarian: fakultas (termasuk cakupan "Semua"). */
	private Combobox searchfakultas = new Combobox();
	/** Kriteria pencarian: prodi (termasuk cakupan "Semua"). */
	private Combobox searchjurusan = new Combobox();
	/** Kriteria pencarian: program/jenjang (termasuk cakupan "Semua"). */
	private Combobox searchprogram = new Combobox();
	private Jurusan selectedJurusan;
	private Fakultas selectedFakultas;
	/** Kriteria pencarian: tahun akademik. */
	private Combobox searchtahunakademik;
	/** Kriteria pencarian: hanya tampilkan baris aktif (default tercentang). */
	private MyCheckboxConfig tampilkanHanyaYgAktif;

	/**
	 * Renderer baris grid hasil pencarian {@link MasaPerkuliahan}: rentang tanggal mulai/sampai,
	 * tahun akademik, program/jurusan/fakultas (tampil "Semua" bila kosong), dan radio button
	 * pilihan mengikuti kerangka standar (lihat {@link ais.ui.util.GetEventListener}). KHUSUS
	 * entity ini, bila pengguna diizinkan mengubah (lihat aturan dua lapis di Javadoc class), baris
	 * juga menampilkan checkbox Aktif dan Default (toggle "Default" pada satu baris meng-unset
	 * default di SEMUA baris lain lewat SQL langsung) serta tombol Ubah/Hapus (memanggil
	 * {@link MasaPerkuliahanAction#onAddExternal}/{@code onDelete}); bila tidak diizinkan, hanya
	 * label status Aktif/Tidak Aktif yang tampil. Penghapusan yang gagal karena constraint FK
	 * ditangani dengan pesan ramah lewat {@link ais.common.PesanFormalHelper}.
	 *
	 * @see AmbilDataMasaPerkuliahanBanbox
	 */
	class MasaPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) arg1;
			MyRadioConfig checkbox = new MyRadioConfig(masaPerkuliahan.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataMasaPerkuliahanBanbox.this.setOpen(false);
					AmbilDataMasaPerkuliahanBanbox.this.setAttribute("masaPerkuliahan", masaPerkuliahan);
					AmbilDataMasaPerkuliahanBanbox.this.setAttribute("myValue", masaPerkuliahan);
					AmbilDataMasaPerkuliahanBanbox.this.setValue(masaPerkuliahan.getNama());
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new MyLabelAgakKecil(
					masaPerkuliahan.getMulai() == null ? "" : Common.dateFormat1.get().format(masaPerkuliahan.getMulai()))
					.setParent(vbox);
			new MyLabelAgakKecil(
					masaPerkuliahan.getSampai() == null ? "" : Common.dateFormat1.get().format(masaPerkuliahan.getSampai()))
					.setParent(vbox);
			new Label(masaPerkuliahan.getTahunAkademik()).setParent(vbox);
			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(masaPerkuliahan.getProgram() == null ? "Semua" : masaPerkuliahan.getProgram()).setParent(vbox);
			new Label(masaPerkuliahan.getJurusan() == null ? "Semua" : masaPerkuliahan.getJurusan().getNama())
					.setParent(vbox);
			new Label(masaPerkuliahan.getFakultas() == null ? "Semua" : masaPerkuliahan.getFakultas().getNama())
					.setParent(vbox);

			if (masaPerkuliahanHanyaBolehDiubahOlheAdmin && Common.getApakahAdmin()) {
				new Label(masaPerkuliahan.getAktif() ? "Aktif" : "Tidak Aktif").setParent(arg0);
				new Label().setParent(arg0);
			} else {

				vbox = new Vbox();
				vbox.setParent(arg0);
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(masaPerkuliahan.getAktif());
				aktif.setParent(vbox);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						masaPerkuliahan.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(masaPerkuliahan);
					}
				});
				final MyCheckboxConfig defaultData = new MyCheckboxConfig("Default");
				defaultData.setChecked(masaPerkuliahan.getDefaultData());
				defaultData.setParent(vbox);
				defaultData.setVisible(Common.getApakahAdmin());
				defaultData.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						masaPerkuliahan.setDefaultData(defaultData.isChecked());
						Common.refreshSaveOrUpdate(masaPerkuliahan);

						HibernateUtil.currentSession().createSQLQuery(
								"update masa_perkuliahan set default_data=false where id != " + masaPerkuliahan.getId())
								.executeUpdate();
						onSearchDefault(arg0);
					}
				});

				Hbox toolbar = new Hbox();

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
				button.setTooltiptext("Ubah Data");
				button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.ambilDosen() == null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MasaPerkuliahanAction.onAddExternal(event, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) arg0.getData();
								AmbilDataMasaPerkuliahanBanbox.this.setOpen(false);
								AmbilDataMasaPerkuliahanBanbox.this.setAttribute("masaPerkuliahan", masaPerkuliahan);
								AmbilDataMasaPerkuliahanBanbox.this.setAttribute("myValue", masaPerkuliahan);
								AmbilDataMasaPerkuliahanBanbox.this.setValue(masaPerkuliahan.getNama());
								if (eventListener != null) {
									eventListener.onEvent(arg0);
								}

							}
						}, masaPerkuliahan);
					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.ambilDosen() == null);
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

												MasaPerkuliahan currentMasaPerkuliahan = (MasaPerkuliahan) getAttribute(
														"masaPerkuliahan");

												if (currentMasaPerkuliahan != null && masaPerkuliahan.getId()
														.equals(currentMasaPerkuliahan.getId())) {

													AmbilDataMasaPerkuliahanBanbox.this.setAttribute("masaPerkuliahan",
															null);
													AmbilDataMasaPerkuliahanBanbox.this.setAttribute("myValue", null);
													AmbilDataMasaPerkuliahanBanbox.this.setValue("");

													if (eventListener != null) {
														eventListener.onEvent(event);
													}

												}

												MasaPerkuliahanAction.onDelete(masaPerkuliahan);

												onSearchDefault(event);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
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

	}

	/**
	 * Membangun popup pencarian {@link MasaPerkuliahan} sekali (dipanggil lazy dari listener
	 * {@code onOpen} atau ulang dari {@link #setJurusan(Jurusan)}): form dengan field nama,
	 * fakultas, tahun akademik, program, prodi, dan checkbox "Hanya yg aktif", tombol Cari, dan
	 * grid hasil dibungkus {@link org.zkoss.zul.Radiogroup} (pilih tunggal). Combobox
	 * fakultas/prodi dikunci bila {@link #jurusan} sudah ditentukan lewat constructor. Bila
	 * pengguna diizinkan mengubah (lihat aturan dua lapis di Javadoc class), toolbar juga
	 * menampilkan tombol Tambah Masa Perkuliahan. Mengikuti kerangka {@code display()} standar —
	 * lihat {@link ais.ui.util.GetEventListener}. Memanggil {@link #onSearchDefault(Event)} di
	 * akhir agar grid terisi saat popup pertama dibuka.
	 */
	public void display() {
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		setReadonly(true);

		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("750px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		MyPanel panel = new MyPanel();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Masa Perkuliahan");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");
		Common.selectComboItem(searchfakultas, jurusan == null ? null : jurusan.getFakultas());
		searchfakultas.setDisabled(jurusan != null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(searchtahunakademik = new Combobox());
		searchtahunakademik.setWidth("90%");
		Common.generateTahunAjaranDanSemua(searchtahunakademik);

		Common.selectComboItem(searchtahunakademik, null);

		row = new MyFormRow();
		searchprogram = new Combobox();
		Common.initPrograms(searchprogram);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");
		Common.insertCombo(searchjurusan, "nama", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", jurusan == null ? null : jurusan.getFakultas()));
		Common.selectComboItem(searchjurusan, jurusan);
		searchjurusan.setDisabled(jurusan != null);

		if (selectedFakultas != null) {
			Common.selectComboItem(this.searchfakultas, selectedFakultas);
		}

		if (selectedJurusan != null) {
			Common.selectComboItem(this.searchjurusan, selectedJurusan);
		}

		row.appendChild(tampilkanHanyaYgAktif = new MyCheckboxConfig("Hanya yg aktif"));
		tampilkanHanyaYgAktif.setChecked(true);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		if (masaPerkuliahanHanyaBolehDiubahOlheAdmin && Common.getApakahAdmin()) {

		} else {
			button = new MyToolbarbuttonConfig("Tambah Masa Perkuliahan", "/img/new.gif");
			button.setTooltiptext("Tambah Masa Perkuliahan");
			button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.ambilDosen() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MasaPerkuliahan masaPerkuliahan = new MasaPerkuliahan();
					masaPerkuliahan.setJurusan(jurusan);
					masaPerkuliahan.setFakultas(jurusan == null ? null : jurusan.getFakultas());

					MasaPerkuliahanAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) arg0.getData();
							AmbilDataMasaPerkuliahanBanbox.this.setOpen(false);
							AmbilDataMasaPerkuliahanBanbox.this.setAttribute("masaPerkuliahan", masaPerkuliahan);
							AmbilDataMasaPerkuliahanBanbox.this.setAttribute("myValue", masaPerkuliahan);
							AmbilDataMasaPerkuliahanBanbox.this.setValue(masaPerkuliahan.getNama());
							if (eventListener != null) {
								eventListener.onEvent(arg0);
							}
							onSearchDefault(arg0);
						}
					}, masaPerkuliahan);
				}

			});
			button.setParent(toolbar);
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Masa");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program" + "/" + Common.getBahasaConfig("Jurusan") + "/" + "Fakultas");
		column.setWidth("22%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth(tbmuser.getMahasiswa() == null || tbmuser.ambilDosen() == null ? "10%" : "0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ubah");
		column.setWidth("8%");

		onSearchDefault(null);

	}

	/**
	 * Mengeksekusi pencarian {@link MasaPerkuliahan} dengan filter {@code aktif} (hanya bila
	 * checkbox {@link #tampilkanHanyaYgAktif} tercentang), tahun akademik (eq bila dipilih),
	 * {@code nama} (ilike substring), dan jurusan/program/fakultas — SEMUA digabung
	 * {@code Restrictions.or(isNull(...), ...)} sehingga masa perkuliahan berlaku "Semua" selalu
	 * ikut tampil terlepas dari kombinasi filter. Diurutkan menurun berdasar tanggal mulai lalu
	 * sampai, dibatasi {@link ais.common.Common#MAX_RESULT}, lalu memasang
	 * {@link MasaPerkuliahanRenderer} dan model hasil ke {@link #grid}. Mengikuti kerangka
	 * {@code onSearchDefault} standar — lihat {@link ais.ui.util.GetEventListener}.
	 *
	 * @param event event pemicu (klik tombol Cari, atau efek samping dari aksi tambah/hapus
	 *              inline); boleh {@code null} saat dipanggil dari {@link #display()}
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(MasaPerkuliahan.class)

				.add(!tampilkanHanyaYgAktif.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("aktif", true))

				.add(searchtahunakademik.getSelectedItem() == null
						|| searchtahunakademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchtahunakademik.getSelectedItem().getValue()));

		criteria.addOrder(Order.desc("mulai")).addOrder(Order.desc("sampai"))
				.add(Restrictions.ilike("nama", nama.getText().trim(), MatchMode.ANYWHERE))

				.add(Restrictions.or(Restrictions.isNull("jurusan"),
						CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(Restrictions.or(Restrictions.isNull("program"),
						searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("program", searchprogram.getSelectedItem().getValue())));

		if (searchfakultas.getSelectedItem() != null) {
			criteria.add(Restrictions.or(Restrictions.isNull("fakultas"),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));
		}
		List<MasaPerkuliahan> masaPerkuliahan = criteria.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(masaPerkuliahan);
		grid.setRowRenderer(new MasaPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** {@inheritDoc} Implementasi setter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	/** {@inheritDoc} Implementasi getter polos standar — lihat {@link ais.ui.util.GetEventListener}. */
	public EventListener getEventListener() {
		return eventListener;
	}

	/**
	 * Menetapkan prodi yang akan diprapilih (bukan dikunci) pada Combobox prodi saat popup
	 * berikutnya dibangun, dan membersihkan state Bandbox (nilai/atribut) via
	 * {@link Common#clear(org.zkoss.zul.Bandbox)}. Berbeda dari {@link #setJurusan(Jurusan)}: ini
	 * hanya mengubah default pilihan combo, bukan mengunci/membatasi hasil pencarian.
	 *
	 * @param jurusan prodi yang akan diprapilih
	 */
	public void setJurusanSelected(Jurusan jurusan) {
		Common.clear(this);
		this.selectedJurusan = jurusan;
	}

	/**
	 * Menetapkan fakultas yang akan diprapilih pada Combobox fakultas saat popup berikutnya
	 * dibangun, dan membersihkan state Bandbox (nilai/atribut). Lihat catatan
	 * {@link #setJurusanSelected(Jurusan)} — hanya mengubah default pilihan, bukan mengunci hasil.
	 *
	 * @param fakultas fakultas yang akan diprapilih
	 */
	public void setFakultasSelected(Fakultas fakultas) {
		Common.clear(this);
		this.selectedFakultas = fakultas;
	}
}
