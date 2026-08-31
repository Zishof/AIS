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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk penugasan dosen mengajar. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Paging paging}, {@code MyGrid grid},
 * {@code Textbox searchnama}, {@code Textbox searchdosen}, {@code Combobox searchTahunAjaran}, {@code Combobox
 * searchJenisSemester}, {@code boolean edit}, {@code boolean delete}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
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
public class PenugasanDosenMengajarAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchdosen;
	protected Combobox searchTahunAjaran;
	protected Combobox searchJenisSemester;

	private boolean edit = false;
	private boolean delete = false;

	private MyToolbarbuttonConfig add;

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
		if (searchTahunAjaran != null) { searchTahunAjaran.setReadonly(true); }
		if (searchJenisSemester != null) { searchJenisSemester.setReadonly(true); }

		Common.generateTahunAjaran(searchTahunAjaran);

		Comboitem comboitem = new org.zkoss.zul.Comboitem();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GANJIL); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		searchJenisSemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel(Perkuliahan.GENAP); }
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		searchJenisSemester.appendChild(comboitem);

		Common.selectComboItem(searchJenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		if (add != null) { add.setVisible(false); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "dosen", "jurusan", "program", "tahunAkademik", "semester", "kode",
				"tanggalSuratTugas", "tmtSuratTugas", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PenugasanDosenMengajar.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PenugasanDosenMengajar.class, contents);
		if (upload != null) { upload.setVisible(edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		MyToolbarbuttonConfig singkronDenganMhs = new MyToolbarbuttonConfig("Generate No. SK Berdasarkan Jadwal",
				"/img/svg/check2.svg");
		singkronDenganMhs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(null);
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {

						Session session = HibernateUtil.currentNativeSession();
						// Thread latar TIDAK lewat FilterJSP -> WAJIB tutup native session sendiri (cegah bocor c3p0).
						try {
							List<Perkuliahan> longs = ConstantValues
									.simpleList(
											session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("tahunAjaran",
															searchTahunAjaran.getSelectedItem().getValue()))

													.add(Restrictions.eq("ganjilGenap",
															searchJenisSemester.getSelectedItem().getValue())),
											Perkuliahan.class);

							int size = longs.size();
							int index = 0;
							for (Perkuliahan perkuliahan : longs) {
								index++;
								try {

									label.setValue("Singkronkan penugasan " + perkuliahan.infoSimple() + " ("
											+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

									List<Dosen> dosens = perkuliahan.populateDosenBuNama();
									for (Dosen dosen : dosens) {
									
										Common.getPenugasanDosenMengajar(perkuliahan.getJurusan().getId(),
												perkuliahan.getProgram(), perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap(),
												perkuliahan.getMatakuliah().getSks(), dosen);

									}

								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}

							}
							label.setValue("");
						} finally {
							try { session.clear(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/PenugasanDosenMengajarAction.java:171");}
							try { session.disconnect(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/PenugasanDosenMengajarAction.java:172");}
							try { session.close(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/PenugasanDosenMengajarAction.java:173");}
						}
					}
				}).start();

			}
		});
		Common.appendKeToolbar(singkronDenganMhs, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	class PenugasanDosenMengajarRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PenugasanDosenMengajar penugasanDosenMengajar = (PenugasanDosenMengajar) arg1;
			new Label(penugasanDosenMengajar.getDosen() == null ? "" : penugasanDosenMengajar.getDosen().getNama())
					.setParent(arg0);
			RevisiHelper.createNewRevisi(PenugasanDosenMengajar.class, penugasanDosenMengajar,
					penugasanDosenMengajar.getNama()).setParent(arg0);
			new Label(penugasanDosenMengajar.getJurusan() == null ? "" : penugasanDosenMengajar.getJurusan().getNama())
					.setParent(arg0);
			new Label(penugasanDosenMengajar.getProgram()).setParent(arg0);
			final MyTextbox kode = new MyTextbox(penugasanDosenMengajar.getKode());
			final MyDatebox tanggalSuratTugas = new MyDatebox(penugasanDosenMengajar.getTanggalSuratTugas());
			final MyDatebox tmtSuratTugas = new MyDatebox(penugasanDosenMengajar.getTmtSuratTugas());
			final MyTextbox keterangan = new MyTextbox(penugasanDosenMengajar.getKeterangan());

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					penugasanDosenMengajar.setKode(kode.getValue());
					penugasanDosenMengajar.setTmtSuratTugas(tmtSuratTugas.getValue());
					penugasanDosenMengajar.setTanggalSuratTugas(tanggalSuratTugas.getValue());
					penugasanDosenMengajar.setKeterangan(keterangan.getValue());
					
					Common.refreshUpdate(penugasanDosenMengajar); 
				}
			};

			kode.setWidth("95%");
			kode.setParent(arg0);

			tanggalSuratTugas.setWidth("95%");
			tanggalSuratTugas.setParent(arg0);

			tmtSuratTugas.setWidth("95%");
			tmtSuratTugas.setParent(arg0);

			keterangan.setWidth("95%");
			keterangan.setParent(arg0);

			kode.addEventListener("onChange", eventListener);
			tanggalSuratTugas.addEventListener("onChange", eventListener);
			tmtSuratTugas.addEventListener("onChange", eventListener);
			keterangan.addEventListener("onChange", eventListener);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenugasanDosenMengajar.class).createAlias("dosen", "dosen");

		if (order)
			criteria.addOrder(Order.desc("dosen.nama")).addOrder(Order.desc("tahunAkademik"))
					.addOrder(Order.asc("semester"));
		criteria

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(searchJenisSemester.getSelectedItem() == null
						|| searchJenisSemester.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("semester", searchJenisSemester.getSelectedItem().getValue()))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchdosen.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("dosen.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("dosen.nidn", searchdosen.getValue().trim(), MatchMode.ANYWHERE))

				)

		;
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenugasanDosenMengajar> penugasanDosenMengajar = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(penugasanDosenMengajar);
		grid.setRowRenderer(new PenugasanDosenMengajarRenderer());
		grid.setModelCheckMobile(strset);

	}

}
