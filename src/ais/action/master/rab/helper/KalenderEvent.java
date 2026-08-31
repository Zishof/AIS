package ais.action.master.rab.helper;

import java.sql.Time;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.impl.SimpleCalendarEvent;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Acara;
import ais.database.model.rab.AcaraPunyaJenisParameter;
import ais.database.model.rab.WorkspacePunyaJenisParameter;

/**
 * Adapter {@link SimpleCalendarEvent} (komponen kalender ZK) untuk {@link Acara} modul RAB (agenda
 * kegiatan): menyalin properti tampilan (judul, warna header/konten, isi, rentang tanggal, terkunci)
 * dari {@code Acara} saat dibuat, dan {@link #getAcara()} menulis balik nilai komponen kalender
 * (yang mungkin sudah diedit interaktif via drag/resize) ke objek {@code Acara} yang sama.
 *
 * <p>
 * {@link #getContent()} di-override untuk memperkaya isi popup event secara dinamis: bila acara
 * terkait satu {@link ais.database.model.rab.Workspace}, deskripsi workspace dan seluruh nilai
 * parameter tambahan ({@link WorkspacePunyaJenisParameter}/{@link AcaraPunyaJenisParameter},
 * diformat sesuai tipe data-nya — String/Integer/Double/Date/Time) ditambahkan sebagai baris-baris
 * tambahan setelah keterangan asli acara.
 * </p>
 */
public class KalenderEvent extends SimpleCalendarEvent {

	private Acara acara;

	/** Membuat event kalender dari {@code acara}, menyalin judul/warna/isi/rentang tanggal/status kunci ke properti {@link SimpleCalendarEvent}. */
	public KalenderEvent(Acara acara) {
		this.setAcara(acara);
		Date beginDate = acara.getPpbegin();
		Date endDate = acara.getPpend();
		String headerColor = acara.getHeadColor();
		String contentColor = acara.getCntColor();
		String content = acara.getKeterangan();
		String title = acara.getNama();
		boolean locked = acara.getPplocked();

		setHeaderColor(headerColor);
		setContentColor(contentColor);
		setContent(content);
		setTitle(title);
		setBeginDate(beginDate);
		setEndDate(endDate);
		setLocked(locked);
	}

	/** Menulis balik nilai komponen kalender saat ini (warna, judul, keterangan, rentang tanggal, status kunci) ke {@link #acara} lalu mengembalikannya. */
	public Acara getAcara() {
		acara.setCntColor(getContentColor());
		acara.setHeadColor(getHeaderColor());
		acara.setKeterangan(super.getContent());
		acara.setNama(getTitle());
		acara.setPpbegin(getBeginDate());
		acara.setPpend(getEndDate());
		acara.setPplocked(isLocked());
		return acara;
	}

	/** Mengganti {@link Acara} yang diikat oleh event kalender ini tanpa menyalin ulang properti tampilan. */
	public void setAcara(Acara acara) {
		this.acara = acara;
	}

	/**
	 * Menghasilkan isi popup event kalender: keterangan asli acara, ditambah (bila acara terikat
	 * satu {@link ais.database.model.rab.Workspace}) deskripsi workspace dan nilai setiap
	 * parameter tambahan workspace tersebut yang sudah direalisasikan pada acara ini (diformat
	 * sesuai tipe data parameter), seluruhnya dibungkus tag {@code <font>} berukuran kecil.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String getContent() {
		String originContent = super.getContent();
		if (acara != null && acara.getWorkspace() != null) {
			originContent += (originContent.trim().equals("") ? "" : "<br>")
					+ acara.getWorkspace().toString();

			Session session = HibernateUtil.currentSession();
			List<WorkspacePunyaJenisParameter> workspacePunyaJenisParameters = session
					.createCriteria(WorkspacePunyaJenisParameter.class)
					.add(Restrictions.eq("workspace", acara.getWorkspace()))
					.list();
			for (WorkspacePunyaJenisParameter workspacePunyaJenisParameter : workspacePunyaJenisParameters) {

				AcaraPunyaJenisParameter realisasiWorkspacePunyaJenisParameter = (AcaraPunyaJenisParameter) session
						.createCriteria(
								AcaraPunyaJenisParameter.class)
						.add(Restrictions.eq("workspacePunyaJenisParameter",
								workspacePunyaJenisParameter))
						.add(Restrictions.eq("acara", acara)).setMaxResults(1)
						.uniqueResult();

				String value = "-";
				if (realisasiWorkspacePunyaJenisParameter != null) {
					if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(String.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValue();
					} else if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(Integer.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValueInteger() == null ? value
								: Common.numberFormat.get()
										.format(realisasiWorkspacePunyaJenisParameter
												.getJenisParameterValueInteger());
					} else if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(Double.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValueDouble() == null ? value
								: Common.numberFormat.get()
										.format(realisasiWorkspacePunyaJenisParameter
												.getJenisParameterValueDouble());
					} else if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(Date.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValueDate() == null ? value
								: Common.dateFormat.get()
										.format(realisasiWorkspacePunyaJenisParameter
												.getJenisParameterValueDate());
					} else if (workspacePunyaJenisParameter.getJenisParameter()
							.getTypedata().equals(Time.class.getName())) {
						value = realisasiWorkspacePunyaJenisParameter
								.getJenisParameterValueTime() == null ? value
								: Common.timeFormat.get()
										.format(realisasiWorkspacePunyaJenisParameter
												.getJenisParameterValueTime());
					}
				}

				originContent += (originContent.trim().equals("") ? "" : "<br>")
						+ workspacePunyaJenisParameter.getJenisParameter()
								.getNama() + " : " + value;
			}
		}
		return "<font style=\"font-size: 8px;\">" + originContent + "</font>";
	}

	/** @return keterangan asli (tanpa pengayaan workspace/parameter tambahan dan tanpa pembungkus font) sebagaimana tersimpan di komponen kalender. */
	public String getOriginContent() {
		String originContent = super.getContent();
		return originContent;
	}

	/** Menyetel keterangan asli event (delegasi langsung ke {@link SimpleCalendarEvent#setContent}); pengayaan tambahan hanya diterapkan saat dibaca lewat {@link #getContent()}, bukan disimpan di sini. */
	@Override
	public void setContent(String content) {
		super.setContent(content);
	}

}