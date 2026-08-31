package ais.action.master;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PenumumanWebsite;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK (databind lama, non-MVVM) untuk kelola pengumuman/berita di website kampus
 * ({@link PenumumanWebsite}): daftar dengan pencarian judul/isi (dibatasi ke perguruan tinggi
 * pemanggil bila konteks multi-PT aktif), tambah/ubah lewat jendela modal, serta pencatatan
 * riwayat revisi lewat {@link RevisiHelper#createNewRevisi}. Akses diverifikasi lewat
 * {@link Common#doCheckSecurity()} sebelum halaman disusun dan {@link CommonPrivilages#READ}
 * setelahnya; sesi tanpa {@code usersTemp} atau tanpa hak baca dipaksa logoff.
 */
public class PenumumanWebsiteAction extends GenericAutowireComposer {
	private static final long serialVersionUID = 1L;

	private MyGrid grid;
	private Paging paging;
	private Textbox searchjudul;
	private Textbox searchisi;
	private MyWindow addWindow;
	private Textbox judul;
	private Textbox ringkasan;
	private Textbox kategori;
	private Textbox isi;
	private MyDatebox tanggal;
	private Checkbox aktif;
	private PenumumanWebsite penumumanWebsite;

	/** Memverifikasi keamanan sesi lewat {@link Common#doCheckSecurity()} sebelum halaman disusun. */
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Memvalidasi sesi user dan hak baca ({@link CommonPrivilages#READ}) setelah komponen
	 * tersusun; memaksa logoff bila salah satu tidak terpenuhi. Bila lolos, memuat daftar
	 * pengumuman awal lewat {@link #onSearchDefault(Event)}.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		onSearchDefault(null);
	}

	/**
	 * Memuat ulang grid daftar pengumuman (maksimal 200 baris, terbaru dahulu) sesuai isian
	 * filter judul/isi saat ini, dibatasi ke perguruan tinggi pemanggil bila konteks multi-PT
	 * aktif. Setiap baris diberi tombol "Ubah" yang membuka {@link #openForm(PenumumanWebsite)}.
	 *
	 * @param event event pemicu (boleh {@code null}, tidak dipakai)
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session hs = HibernateUtil.currentSession();
		try {
			org.hibernate.Criteria criteria = hs.createCriteria(PenumumanWebsite.class).addOrder(Order.desc("tanggal"))
					.addOrder(Order.desc("id"));
			String sj = searchjudul == null ? "" : searchjudul.getValue();
			String si = searchisi == null ? "" : searchisi.getValue();
			if (sj != null && sj.trim().length() > 0) {
				criteria.add(Restrictions.ilike("judul", sj.trim(), MatchMode.ANYWHERE));
			}
			if (si != null && si.trim().length() > 0) {
				criteria.add(Restrictions.ilike("isi", si.trim(), MatchMode.ANYWHERE));
			}
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
			if (pt != null && pt.getId() != null) {
				criteria.add(Restrictions.eq("perguruanTinggi", pt));
			}
			List<PenumumanWebsite> list = criteria.setMaxResults(200).list();
			grid.getRows().getChildren().clear();
			for (final PenumumanWebsite data : list) {
				Row row = new Row();
				row.setParent(grid.getRows());
				row.appendChild(new Label(data.getTanggal() == null ? "" : Common.dateFormat.get().format(data.getTanggal())));
				row.appendChild(new Label(data.getKategori()));
				row.appendChild(new Label(data.getJudul()));
				row.appendChild(new Label(data.getRingkasan()));
				row.appendChild(new Label(data.getAktif() ? "Aktif" : "Tidak Aktif"));
				MyToolbarbuttonConfig edit = new MyToolbarbuttonConfig("Ubah", "/img/edit.gif");
				edit.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
					public void onEvent(Event event) throws Exception {
						openForm(data);
					}
				});
				row.appendChild(edit);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/** Membuka jendela isi data untuk membuat pengumuman baru (record kosong). */
	public void onAdd(Event event) throws Exception {
		openForm(new PenumumanWebsite());
	}

	/** Mengisi field formulir dari {@code data} dan menampilkan jendela modal isi/edit pengumuman. */
	private void openForm(PenumumanWebsite data) throws Exception {
		penumumanWebsite = data;
		judul.setValue(data.getJudul());
		ringkasan.setValue(data.getRingkasan());
		kategori.setValue(data.getKategori());
		isi.setValue(data.getIsi());
		tanggal.setValue(data.getTanggal());
		aktif.setChecked(data.getAktif());
		addWindow.setVisible(true);
		try {
			addWindow.doModal();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Memvalidasi judul wajib diisi, lalu menyimpan (buat baru atau perbarui)
	 * {@link PenumumanWebsite} dalam transaksi Hibernate: mencatat perguruan tinggi pemanggil dan
	 * identitas user yang menyimpan, mencatat riwayat revisi lewat {@link RevisiHelper}, menutup
	 * jendela modal, dan memuat ulang daftar. Transaksi di-rollback dan kesalahan ditampilkan bila
	 * penyimpanan gagal.
	 *
	 * @param event event pemicu (tidak dipakai)
	 */
	public void onSave(Event event) throws Exception {
		if (judul.getValue() == null || judul.getValue().trim().length() == 0) {
			try {
				MyMessageboxConfig.show("Judul wajib diisi.");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return;
		}
		Session hs = HibernateUtil.currentSession();
		try {
			hs.getTransaction().begin();
			if (penumumanWebsite == null) {
				penumumanWebsite = new PenumumanWebsite();
			}
			penumumanWebsite.setJudul(judul.getValue());
			penumumanWebsite.setRingkasan(ringkasan.getValue());
			penumumanWebsite.setKategori(kategori.getValue());
			penumumanWebsite.setIsi(isi.getValue());
			penumumanWebsite.setTanggal(tanggal.getValue());
			penumumanWebsite.setAktif(aktif.isChecked());
			penumumanWebsite.setPerguruanTinggi(PerguruanTinggiUtil.getPerguruanTinggi());
			Tbmuser user = Common.getCurrentUser();
			if (user != null) {
				penumumanWebsite.setOleh(user.getUserNama());
				penumumanWebsite.setOlehId(user.getUserId());
			}
			Common.refreshUpdate(hs, penumumanWebsite);
			hs.getTransaction().commit();
			RevisiHelper.createNewRevisi(PenumumanWebsite.class, penumumanWebsite, "Berita Website Kampus");
			addWindow.setVisible(false);
			onSearchDefault(null);
		} catch (Exception e) {
			HibernateUtil.rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		} finally {
			HibernateUtil.closeSession();
		}
	}
}
