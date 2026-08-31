package ais.action.master.sop;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Checkbox;
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

import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.JenisSop;
import ais.database.model.sop.Sop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk sop. Tipe ini merupakan titik masuk UI yang menghubungkan event layar
 * dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code Textbox nama},
 * {@code Textbox keterangan}, {@code boolean edit}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
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
public class SopAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Sop sop;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private Combobox jenisSop;
	private Textbox versi;
	private MyDatebox tanggalTerbit;
	protected LampiranLain lainMahasiswa;
	private Combobox jurusan;
	private Combobox fakultas;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private Hbox hbFakultasLabel;
	private Hbox hbFakultas;

	private Hbox hbYayasanLabel;
	private Hbox hbYayasan;
	protected LampiranLain lampiran;
//	private MyCheckboxConfig untukUjiCoba;

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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (searchyayasan != null) {
			searchyayasan.getParent().setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(
					Common.bolehKonfigurasi("user_fakultas"));
		}

		if (hbFakultas != null) {
			hbFakultas.setVisible(
					Common.bolehKonfigurasi("user_fakultas"));
		}

		if (hbYayasanLabel != null) {
			hbYayasanLabel.setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		if (hbYayasan != null) {
			hbYayasan.setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		String[] contents = new String[] { "id", "kode", "nama", "versi", "jenisSop", "tanggalTerbit", "keterangan",
				"aktif", "jurusan", "fakultas", "yayasan", "sekolah", "satuanKerja" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Sop.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Sop.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Renderer lokal untuk layar/komponen {@link SopAction}. Kelas ini menerjemahkan satu item data menjadi baris
	 * atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link SopAction} dan dapat mengakses state kelas
	 * induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see SopAction
	 */
	class SopRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Sop sop = (Sop) arg1;
			new Label(sop.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(Sop.class, sop, sop.getNama())).setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, sop.getId(), Sop.class.getName(), "Lampiran", false, null,
					null, false, false, false, false);

			new Label(sop.getVersi()).setParent(arg0);
			Label l;
			(l = new Label(sop.getJenisSop().getNama())).setParent(arg0);
			l.setStyle("background-color:" + sop.getJenisSop().getWarna() + ";color:" + sop.getJenisSop().getWarnatext()
					+ ";");
			new Label(Common.dateFormat2.get().format(sop.getTanggalTerbit())).setParent(arg0);

			new Label(sop.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(sop.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					sop.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(sop);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, sop, SopAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Sop());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		sop = (Sop) obj;
		init(sop);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Sop sop) throws Exception {
		this.sop = sop;
		addWindow.setTitle(sop.getId() == null ? "Tambah SOP" : "Ubah SOP");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode SOP *"));
		row.appendChild(kode = new Textbox(sop.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama SOP *"));
		row.appendChild(nama = new Textbox(sop.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis SOP *"));
		row.appendChild(jenisSop = new Combobox());
		Common.insertCombo(jenisSop, "nama", JenisSop.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(true, jenisSop, sop.getJenisSop());
		jenisSop.setWidth("90%");
		jenisSop.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(
				Common.bolehKonfigurasi("integrasi_rab", Konfigurasi.TIDAK_AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja
				.setValue(sop.getSatuanKerja() == null
						? (Common.getCurrentUser().ambilSatuanKerja() == null ? ""
								: Common.getCurrentUser().ambilSatuanKerja().toString())
						: sop.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja",
				sop.getSatuanKerja() == null ? Common.getCurrentUser().ambilSatuanKerja() : sop.getSatuanKerja());
		satuanKerja.setWidth("90%");

//		Tbmuser sop1 = Common.getCurrentUser();

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("user_fakultas"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		Common.selectComboItem(fakultas, sop.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("user_jurusan"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, sop.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(
				Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan, sop.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(
				Common.bolehKonfigurasi("user_sekolah", Konfigurasi.TIDAK_AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah, sop.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		fakultas.setDisabled(false);
		jurusan.setDisabled(false);
		yayasan.setDisabled(false);
		sekolah.setDisabled(false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Versi SOP *"));
		row.appendChild(versi = new Textbox(sop.getVersi()));
		versi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Terbit *"));
		row.appendChild(tanggalTerbit = new MyDatebox(sop.getTanggalTerbit()));
		tanggalTerbit.setWidth("90%");

//		row = new MyFormRow();
////		row.setParent(rows);
//		row.appendChild(new ais.ui.util.MyLabelConfig(""));
//		row.appendChild(untukUjiCoba = new MyCheckboxConfig(
//				"SOP ini masih digunakan untuk uji coba (semua admin boleh uji coba disposisi)"));
//		untukUjiCoba.setChecked(sop.getUntukUjiCoba());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(sop.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiran = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("File format disposisi (jrxml atau jasper)"));
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		LampiranLain.createDownloadUploadFileLain(hbox, sop.getId(), LampiranLain.FILE_JRXML_LAYOUT_DISPOSISI_ALUR_SOP,
				"File format disposisi jrxml", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran SOP"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, sop.getId(), Sop.class.getName(), "Lampiran SOP", false,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file lampiran SOP lebih dari satu file, zip dulu semua file tersebut");

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

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode SOP; (2) isikan kode yang unik dan sesuai ketentuan; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Nama SOP; (2) isikan nama SOP yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (versi.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Versi SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Versi SOP; (2) isikan nomor versi SOP yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisSop.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis SOP belum dipilih. Langkah yang dapat dilakukan: (1) klik pilihan Jenis SOP; (2) pilih jenis SOP yang sesuai dari daftar yang tersedia; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (tanggalTerbit.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Terbit SOP belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Tanggal Terbit SOP; (2) pilih tanggal terbit yang sesuai; (3) ulangi proses menyimpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		Sop copyDari = null;
		try {
			copyDari = (Sop) sop.getCopyDari();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/SopAction.java:475");
			// TODO: handle exception
		}

		Session session = HibernateUtil.currentSession();
		try {
			if (sop.getId() != null) {
				sop = (Sop) session.load(Sop.class, sop.getId());
			}
		} catch (Exception e) {
			sop = new Sop();
		}

		sop.setKode(kode.getValue());
		sop.setNama(nama.getValue());
		sop.setVersi(versi.getValue());
		sop.setJenisSop((JenisSop) jenisSop.getSelectedItem().getValue());
		sop.setTanggalTerbit(tanggalTerbit.getValue());
		sop.setKeterangan(keterangan.getValue());

		sop.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		sop.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		sop.setYayasan((Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		sop.setSekolah((Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));
		sop.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
//		sop.setUntukUjiCoba(untukUjiCoba.isChecked());

		Common.refreshSaveOrUpdate(session, sop);

		if (copyDari != null && copyDari.getId() != null) {
			try {
				List<AlurSop> alurSops = session.createCriteria(AlurSop.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("sop", copyDari)).addOrder(Order.asc("id")).list();

				for (AlurSop alurSop : alurSops) {
					AlurSop alurSopLama = (AlurSop) session.createCriteria(AlurSop.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("sop", sop)).add(Restrictions.eq("kode", alurSop.getKode()))
							.setMaxResults(1).uniqueResult();
					if (alurSopLama == null) {
						alurSopLama = (AlurSop) alurSop.clone();
						alurSopLama.setId(null);
						alurSopLama.setSop(sop);
						session.save(alurSopLama);
						session.flush();
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(sop.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(sop.getId());

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
		Criteria criteria = session.createCriteria(Sop.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sekolah"),
										CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("yayasan"),
										CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Sop> sop = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(sop);
		grid.setRowRenderer(new SopRenderer());
		grid.setModelCheckMobile(strset);

	}

}
