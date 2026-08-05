package ais.common;

import org.hibernate.Criteria;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.South;

/**
 * Helper terpusat untuk konfigurasi paging ZK.
 *
 * Common tetap menyimpan semua signature lama, sementara implementasi teknis
 * dipindahkan ke class ini agar maintenance lebih mudah dan pola paging tidak
 * berulang di banyak method.
 */
public final class CommonPagingHelper {

	private CommonPagingHelper() {
	}

	public static void initPaging(Paging paging, EventListener eventListener) {
		initPaging(null, paging, eventListener);
	}

	public static void initPaging(Criteria criteria, Paging paging) {
		initPaging(criteria, paging, null);
	}

	public static void initPaging(Criteria criteria, final Paging paging, final EventListener eventListener) {
		initPaging(criteria, paging, eventListener, Projections.rowCount());
	}

	public static void initPaging(Criteria criteria, final Paging paging, final EventListener eventListener,
			Projection projection) {
		configure(criteria, paging, eventListener, projection, Common.ROWS_COUNT_ON_PAGE, true, SCROLL_PARENT_PARENT);
	}

	public static void initPaging5(Paging paging, EventListener eventListener) {
		initPaging5(null, paging, eventListener);
	}

	public static void initPaging5(Criteria criteria, Paging paging) {
		initPaging5(criteria, paging, null);
	}

	public static void initPaging5(Criteria criteria, Paging paging, EventListener eventListener) {
		configure(criteria, paging, eventListener, Projections.rowCount(), Common.ROWS_COUNT_ON_PAGE_5, false,
				SCROLL_EVENT_PARENT_PARENT);
	}

	public static void initPagingCustom(Paging paging, EventListener eventListener, int displayPerPage) {
		initPagingCustom(null, paging, eventListener, displayPerPage);
	}

	public static void initPagingCustom(Criteria criteria, Paging paging, int displayPerPage) {
		initPagingCustom(criteria, paging, null, displayPerPage);
	}

	public static void initPagingCustom(Criteria criteria, Paging paging, EventListener eventListener,
			int displayPerPage) {
		configure(criteria, paging, eventListener, Projections.rowCount(), displayPerPage, false,
				SCROLL_EVENT_PARENT_PARENT);
	}

	public static void initPaging1(Paging paging, EventListener eventListener) {
		initPaging1(null, paging, eventListener);
	}

	public static void initPaging1(Criteria criteria, Paging paging) {
		initPaging1(criteria, paging, null);
	}

	public static void initPaging1(Criteria criteria, Paging paging, EventListener eventListener) {
		configure(criteria, paging, eventListener, Projections.rowCount(), Common.ROWS_COUNT_ON_PAGE_1, false,
				SCROLL_EVENT_TARGET);
	}

	public static void initPaging15(Paging paging, EventListener eventListener) {
		initPaging15(null, paging, eventListener);
	}

	public static void initPaging15(Criteria criteria, Paging paging) {
		initPaging15(criteria, paging, null);
	}

	public static void initPaging15(Criteria criteria, Paging paging, EventListener eventListener) {
		configure(criteria, paging, eventListener, Projections.rowCount(), Common.ROWS_COUNT_ON_PAGE_15, false,
				SCROLL_EVENT_PARENT_PARENT);
	}

	public static void initPaging10(Paging paging, EventListener eventListener) {
		initPaging10(null, paging, eventListener);
	}

	public static void initPaging10(Criteria criteria, Paging paging) {
		initPaging10(criteria, paging, null);
	}

	public static void initPaging10(Criteria criteria, Paging paging, EventListener eventListener) {
		configure(criteria, paging, eventListener, Projections.rowCount(), Common.ROWS_COUNT_ON_PAGE_10, false,
				SCROLL_EVENT_PARENT_PARENT);
	}

	public static void initPaging100(Paging paging, EventListener eventListener) {
		initPaging100(null, paging, eventListener);
	}

	public static void initPaging100(Criteria criteria, Paging paging) {
		initPaging100(criteria, paging, null);
	}

	public static void initPaging100(Criteria criteria, Paging paging, EventListener eventListener) {
		configure(criteria, paging, eventListener, Projections.rowCount(), Common.ROWS_COUNT_ON_PAGE_100, false,
				SCROLL_EVENT_PARENT_PARENT);
	}

	public static void initPaging50(Paging paging, EventListener eventListener) {
		initPaging50(null, paging, eventListener);
	}

	public static void initPaging50(Criteria criteria, Paging paging) {
		initPaging50(criteria, paging, null);
	}

	public static void initPaging50(Criteria criteria, Paging paging, EventListener eventListener) {
		configure(criteria, paging, eventListener, Projections.rowCount(), Common.ROWS_COUNT_ON_PAGE_50, false,
				SCROLL_EVENT_PARENT_PARENT);
	}

	public static void initPaging25(Paging paging, EventListener eventListener) {
		initPaging25(null, paging, eventListener);
	}

	public static void initPaging25(Criteria criteria, Paging paging) {
		initPaging25(criteria, paging, null);
	}

	public static void initPaging25(Criteria criteria, Paging paging, EventListener eventListener) {
		initPaging25(criteria, paging, eventListener, Projections.rowCount());
	}

	public static void initPaging25(Criteria criteria, Paging paging, EventListener eventListener,
			Projection projection) {
		configure(criteria, paging, eventListener, projection, Common.ROWS_COUNT_ON_PAGE_25, false,
				SCROLL_EVENT_PARENT_PARENT);
	}

	private static final int SCROLL_PARENT_PARENT = 1;
	private static final int SCROLL_EVENT_PARENT_PARENT = 2;
	private static final int SCROLL_EVENT_TARGET = 3;

	private static void configure(Criteria criteria, final Paging paging, final EventListener eventListener,
			Projection projection, final int pageSize, boolean alwaysDetailed, final int scrollMode) {
		if (paging == null) {
			return;
		}

		if (eventListener == null) {
			setupPagingData(criteria, paging, projection, pageSize, alwaysDetailed);
			return;
		}

		paging.setDetailed(alwaysDetailed || !Common.isMobile());
		paging.addEventListener("onPaging", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					eventListener.onEvent(event);
				} finally {
					scrollIntoView(paging, event, scrollMode);
				}
			}
		});
	}

	private static void setupPagingData(Criteria criteria, Paging paging, Projection projection, int pageSize,
			boolean alwaysDetailed) {
		paging.setDetailed(alwaysDetailed || !Common.isMobile());
		paging.setPageSize(pageSize);
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");

		int size = pageSize;
		if (criteria != null && projection != null) {
			try {
				size = ((Number) criteria.setProjection(projection).uniqueResult()).intValue();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		paging.setTotalSize(size);
		paging.setVisible(size > pageSize);
		updateParentHeight(paging, size > pageSize);
	}

	private static void updateParentHeight(Paging paging, boolean visible) {
		try {
			Component parent = paging.getParent();
			if (parent instanceof South) {
				((South) parent).setHeight(visible ? "30px" : "0px");
			}
			if (parent instanceof Row) {
				((Row) parent).setHeight(visible ? "30px" : "0px");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPagingHelper.java:211");
		}
	}

	private static void scrollIntoView(Paging paging, Event event, int scrollMode) {
		try {
			// Gulir kontainer kembali ke posisi paling atas setelah paging berganti
			// halaman, sehingga header & filter card tetap terlihat.
			// Utamakan .ais-crud-portal (semua halaman CRUD portal baru); bila tidak ada,
			// fallback ke window.scrollTo agar header halaman lain juga tetap terlihat.
			Clients.evalJavaScript(
					"(function(){"
					+ "var p=document.querySelector('.ais-crud-portal');"
					+ "if(p){p.scrollTop=0;return;}"
					+ "window.scrollTo(0,0);"
					+ "})()");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonPagingHelper.java:227");
		}
	}
}
