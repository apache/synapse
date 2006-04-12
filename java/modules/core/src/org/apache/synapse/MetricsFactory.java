package org.apache.synapse;

import org.apache.axis2.addressing.EndpointReference;

public interface MetricsFactory {
	Metrics getMetrics(String URI);
	Metrics getMetrics(EndpointReference epr);
}
