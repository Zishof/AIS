package ais.common.inventory.outbound;

/** Adapter persistence untuk status picking, packing, custody, POD, dan receipt. */
public interface OutboundWorkflowPort {
	OutboundWorkflowResult apply(OutboundWorkflowCommand command);
}
