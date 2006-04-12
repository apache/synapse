package org.apache.synapse.api;

import java.util.List;

public interface ListMediator extends Mediator {
	public void setList(List m);
	public List getList();
}
