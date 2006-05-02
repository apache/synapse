package org.apache.synapse.api;

import java.util.List;

/**
 * The List mediator executes a given sequence/list of child mediators
 */
public interface ListMediator extends Mediator {

    public boolean addChild(Mediator m);

    public Mediator getChild(int pos);

    public boolean removeChild(Mediator m);

    public Mediator removeChild(int pos);

    public List getList();

    public void setList(List l); //TODO do we need this?
}
