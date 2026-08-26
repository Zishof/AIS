package ais.common.inventory.production;

/** Port persistence status/audit produksi; implementasi wajib menjamin idempotensi. */
public interface ProductionWorkflowPort {
	ProductionWorkflowResult apply(ProductionWorkflowCommand command);
}
