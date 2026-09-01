package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GrupKuesionerUmum;
import ais.database.model.GrupKuosionerUmumDetail;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Baris detail ZK ({@code org.zkoss.zul.Detail}, lewat superclass {@link MyDetail}) yang dipasang
 * pada grid master {@link GrupKuesionerUmum} — pengelompokan pengguna aplikasi ({@link Tbmuser})
 * yang akan menerima suatu kuosioner umum (mis. survei kepuasan lintas modul). Saat baris grup pada
 * grid induk di-expand oleh pengguna (event {@code onOpen} ZK, lihat konstruktor), kelas ini
 * merender panel berisi grid anggota grup: daftar {@link GrupKuosionerUmumDetail} (baris pivot
 * pengguna&harr;grup) lengkap dengan foto, id &amp; nama pengguna, keterangan bebas yang bisa diedit
 * inline, checkbox aktif, serta tombol hapus per baris. Data baru dimuat saat detail benar-benar
 * terbuka ({@code isOpen()}), bukan saat grid induk pertama kali dirender — pola lazy-load standar
 * komponen {@code Detail} ZK.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyDetail}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code GrupKuesionerUmum grupKuesionerUmum},
 * {@code MyGrid grid}, {@code boolean edit}, {@code boolean add}, {@code boolean delete}, {@code
 * MyCheckboxConfig hanyaYgAktif}; pembacaan/pencarian ({@code loadData()}, {@code onSearchDefault()}); operasi
 * domain lain ({@code display()}); konfigurasi constructor: {@code add}, {@code delete}, {@code edit}. Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping konkret:</b> textbox {@code keterangan} dan checkbox {@code aktif} pada tiap baris grid
 * MENYIMPAN LANGSUNG ke database saat berubah ({@code onChange}/{@code onCheck} masing-masing memanggil {@code
 * Common.refreshUpdate}/{@code Common.refreshSaveOrUpdate}) — tidak ada tombol "Simpan" terpisah, sel grid itu
 * sendiri adalah form auto-save. Tombol "Ambil Pengguna" membuka picker massal {@code AmbilDataTbmuserBanyak}
 * yang sudah diberi daftar pengguna aktif grup ini (query {@code groupProperty} pada {@code tbmuser}) sebagai
 * baris pre-checked/terkunci; pengguna baru yang dipilih langsung dibuatkan {@code GrupKuosionerUmumDetail} baru
 * (keterangan kosong, mengikuti nilai default aktif entity). Tombol hapus per baris memanggil {@code
 * Common.refreshDelete} dan menangkap kegagalan constraint FK dengan pesan ramah lewat {@link
 * PesanFormalHelper}, bukan stack trace mentah ke pengguna. Tombol upload Excel di toolbar memakai mekanisme
 * generik {@code Common.uploadData(this, GrupKuosionerUmumDetail.class, contents)} — BERBEDA dari
 * {@code KelompokMahasiswaDetailAction}/{@code KelompokStatusMahasiswaDetailAction}/{@code
 * KelompokStatusKeluarMahasiswaDetailAction} di paket ini yang masing-masing menulis method
 * {@code uploadDataMahasiswa} kustom sendiri dengan parsing xlsx manual; kelas ini tidak punya logic upload
 * manual sendiri.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyDetail
 */
public class GrupKuosionerUmumDetailAction extends MyDetail implements DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private GrupKuesionerUmum grupKuesionerUmum;
	private MyGrid grid;

	private boolean edit = false;
	private boolean add = false;
	private boolean delete = false;

	private MyCheckboxConfig hanyaYgAktif;

	/**
	 * Membuat detail row untuk satu {@code grupKuesionerUmum} tertentu.
	 *
	 * <p>Menghitung hak akses tombol tambah/ubah/hapus dari privilese pengguna login lewat
	 * {@link CommonPrivilages}, menyimpan referensi entity induk, dan mendaftarkan listener
	 * {@code onOpen} yang membersihkan anak komponen lalu memanggil {@link #display()} — grid
	 * anggota grup baru dibangun saat detail benar-benar terbuka ({@code isOpen()}), bukan saat
	 * konstruktor dipanggil.</p>
	 *
	 * @param grupKuesionerUmum entity grup kuosioner umum induk yang detail anggotanya ditampilkan
	 */
	public GrupKuosionerUmumDetailAction(GrupKuesionerUmum grupKuesionerUmum) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.grupKuesionerUmum = grupKuesionerUmum;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(GrupKuosionerUmumDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	/**
	 * Renderer baris grid anggota grup untuk {@link GrupKuosionerUmumDetailAction}. Setiap baris
	 * grid mewakili satu {@link GrupKuosionerUmumDetail} (pivot pengguna&harr;grup): foto kecil
	 * pengguna, id pengguna, tautan riwayat revisi Envers ({@code RevisiHelper.createNewRevisi}),
	 * textbox {@code keterangan} yang auto-save on-change, checkbox {@code aktif} yang auto-save
	 * on-check, dan tombol hapus dengan dialog konfirmasi + penanganan kegagalan FK constraint.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link GrupKuosionerUmumDetailAction} dan dapat
	 * mengakses state kelas induk (flag {@code edit}/{@code delete}). Jangan menyimpan atau membagikannya
	 * lintas desktop/session.</p>
	 * <p><b>Efek samping:</b> perubahan pada textbox/checkbox baris langsung memicu simpan ke database
	 * (bukan sekadar mengubah tampilan); jalankan pada event thread dengan konteks pengguna/session aktif.</p>
	 *
	 * @see GrupKuosionerUmumDetailAction
	 */
	class GrupKuosionerUmumDetailRenderer extends ais.ui.util.MyRowRenderer {

		public GrupKuosionerUmumDetailRenderer() {

		}

		/**
		 * Merender satu baris grid untuk {@code grupKuosionerUmumDetail}: foto, id pengguna,
		 * tautan riwayat revisi, textbox keterangan (auto-save), checkbox aktif (auto-save), dan
		 * tombol hapus (konfirmasi + penanganan FK constraint).
		 *
		 * @param row  baris grid ZK tujuan render
		 * @param data instance {@link GrupKuosionerUmumDetail} untuk baris ini
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final GrupKuosionerUmumDetail grupKuosionerUmumDetail = (GrupKuosionerUmumDetail) data;

			CommonMedia.tampilkanGambarKecil(grupKuosionerUmumDetail.getTbmuser()).setParent(row);

			new Label(grupKuosionerUmumDetail.getTbmuser() == null ? ""
					: grupKuosionerUmumDetail.getTbmuser().getUserId()).setParent(row);

			RevisiHelper.createNewRevisi(GrupKuosionerUmumDetail.class, grupKuosionerUmumDetail,
					grupKuosionerUmumDetail.getTbmuser() == null ? ""
							: grupKuosionerUmumDetail.getTbmuser().getUserNama())
					.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					grupKuosionerUmumDetail.getKeterangan() == null ? "" : grupKuosionerUmumDetail.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setDisabled(!edit);
			keterangan.setParent(row);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					grupKuosionerUmumDetail.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (grupKuosionerUmumDetail));
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(grupKuosionerUmumDetail.getAktif());
			checkbox.setParent(row);
			row.setValign("top");row.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					grupKuosionerUmumDetail.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(grupKuosionerUmumDetail);
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
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

											Common.refreshDelete(grupKuosionerUmumDetail);

											loadData(null);

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
			toolbar.setParent(row);

		}
	}

	/**
	 * Memuat ulang daftar {@link GrupKuosionerUmumDetail} milik {@code grupKuesionerUmum} ini dari
	 * database dan menampilkannya ke grid. Checkbox toolbar "Hanya yg aktif" (bila dicentang)
	 * membatasi hasil ke baris dengan {@code aktif == true} atau {@code null}; bila tidak dicentang,
	 * semua baris (termasuk yang non-aktif) ikut ditampilkan lewat filter no-op {@code
	 * Restrictions.sqlRestriction("true")}. Hasil diurutkan menurun berdasarkan id (data terbaru di
	 * atas).
	 *
	 * @param value tidak dipakai — signature mengikuti kontrak umum handler event grid AIS
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<GrupKuosionerUmumDetail> grupKuosionerUmumDetails = session.createCriteria(GrupKuosionerUmumDetail.class)
				.add(!hanyaYgAktif.isChecked() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("id")).add(Restrictions.eq("grupKuesionerUmum", grupKuesionerUmum)).list();

		ListModel strset = new SimpleListModel(grupKuosionerUmumDetails);
		grid.setRowRenderer(new GrupKuosionerUmumDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun seluruh UI panel detail: caption "Daftar &lt;nama grup&gt;", toolbar (tombol "Ambil
	 * Pengguna" untuk memilih banyak {@link Tbmuser} sekaligus lewat {@code AmbilDataTbmuserBanyak},
	 * checkbox "Hanya yg aktif", tombol cetak/export data lewat {@code Common.cetakData}, tombol
	 * upload Excel generik), definisi kolom grid, lalu memanggil {@link #loadData(Object)} untuk
	 * memuat baris pertama kali. Dipanggil sekali per pembukaan detail (lihat listener {@code onOpen}
	 * di konstruktor).
	 */
	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar " + grupKuesionerUmum.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Pengguna", "/img/add_item.png");
		button.setDisabled(!add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<Tbmuser> tbmusers = session.createCriteria(GrupKuosionerUmumDetail.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.setProjection(Projections.groupProperty("tbmuser")).add(Restrictions.isNotNull("tbmuser"))
						.add(Restrictions.eq("grupKuesionerUmum", grupKuesionerUmum)).list();

				AmbilDataTbmuserBanyak ambilDataTbmuserBanyak = new AmbilDataTbmuserBanyak(tbmusers);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataTbmuserBanyak);
				ambilDataTbmuserBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();

						for (Tbmuser tbmuser : tbmusers) {
							GrupKuosionerUmumDetail grupKuosionerUmumDetail = new GrupKuosionerUmumDetail();
							grupKuosionerUmumDetail.setTbmuser(tbmuser);
							grupKuosionerUmumDetail.setKeterangan("");
							grupKuosionerUmumDetail.setGrupKuesionerUmum(grupKuesionerUmum);
							Common.refreshSaveOrUpdate(grupKuosionerUmumDetail);
						}

						loadData(null);
					}
				});
				ambilDataTbmuserBanyak.setWidth("850px");
				ambilDataTbmuserBanyak.setHeight("97%");
				ambilDataTbmuserBanyak.setVisible(true);
				ambilDataTbmuserBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		hanyaYgAktif = new MyCheckboxConfig("Hanya yg aktif");
		hanyaYgAktif.setChecked(true);
		hanyaYgAktif.setParent(toolbar);
		hanyaYgAktif.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		String[] contents = new String[] { "id", "grupKuesionerUmum", "tbmuser", "aktif", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {

				return HibernateUtil.currentSession().createCriteria(GrupKuosionerUmumDetail.class)
						.createAlias("tbmuser", "tbmuser")
						.add(!hanyaYgAktif.isChecked() ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("grupKuosionerUmumDetail.aktif"),
										Restrictions.eq("grupKuosionerUmumDetail.aktif", true)))
						.add(Restrictions.eq("grupKuesionerUmum", grupKuesionerUmum))
						.addOrder(Order.asc("tbmuser.userNama"));
			}
		}, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, GrupKuosionerUmumDetail.class, contents);
		upload.setVisible(edit && delete);
		toolbar.appendChild(upload);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Id Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Pengguna");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aktif");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	/**
	 * Implementasi kontrak {@link DataSearchDefault}: dipicu saat pengguna memakai pencarian
	 * default dari luar (mis. tombol cari bersama pada layar induk). Cukup memuat ulang grid tanpa
	 * parameter tambahan karena pencarian di kelas ini hanya berbasis filter "Hanya yg aktif",
	 * bukan kata kunci teks bebas.
	 *
	 * @param event event pemicu pencarian (isinya tidak dipakai)
	 */
	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
