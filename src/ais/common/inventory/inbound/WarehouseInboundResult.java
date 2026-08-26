package ais.common.inventory.inbound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ringkasan posting inbound yang aman untuk dipakai UI maupun audit. */
public final class WarehouseInboundResult {
	public static final String POSTED = "POSTED";
	public static final String ALREADY_POSTED = "ALREADY_POSTED";
	public static final String REJECTED = "REJECTED";
	public static final String FAILED = "FAILED";

	private final String status;
	private final int postedCount;
	private final int replayCount;
	private final List<String> messages;

	public WarehouseInboundResult(String status, int postedCount, int replayCount,
			List<String> messages) {
		this.status = status == null ? "" : status.trim();
		this.postedCount = postedCount;
		this.replayCount = replayCount;
		this.messages = messages == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(messages));
	}

	public String getStatus() { return status; }
	public int getPostedCount() { return postedCount; }
	public int getReplayCount() { return replayCount; }
	public List<String> getMessages() { return messages; }
	public boolean isSuccessful() { return POSTED.equals(status) || ALREADY_POSTED.equals(status); }
}
