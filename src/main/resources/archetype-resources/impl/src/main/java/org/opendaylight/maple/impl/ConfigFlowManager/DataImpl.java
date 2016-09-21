/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.ConfigFlowManager;

import org.maple.core.increment.MapleCore;
import org.maple.core.increment.MapleDataStoreAdaptor;
import org.maple.core.increment.app.MapleApp;
import org.maple.core.increment.app.systemApps.Host2PortEntry;
import org.maple.core.increment.tracetree.Port;
import org.maple.core.increment.tracetree.TraceTree;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.maple.increment.rev160920.HostTable;
import org.opendaylight.yang.gen.v1.urn.maple.increment.rev160920.HostTableBuilder;
import org.opendaylight.yang.gen.v1.urn.maple.increment.rev160920.host.table.HostItem;
import org.opendaylight.yang.gen.v1.urn.maple.increment.rev160920.host.table.HostItemBuilder;
import org.opendaylight.yang.gen.v1.urn.maple.increment.rev160920.host.table.HostItemKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.maple.tracetree.rev151114.MapleTracetreeData;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class DataImpl implements MapleDataStoreAdaptor{
    private static final Logger LOG = LoggerFactory.getLogger(DataImpl.class);
    DataBroker dataBroker;
    TraceTreeWriter ttw;

    public DataImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.ttw = new TraceTreeWriter(this.dataBroker);
    }

    @Override
    public Object readData(String s) {
        InstanceIdentifier iId = convertXpath2IId(s);
        if (s.equals( "/root/network-topology/topology")) {
            return readTopology(iId);
        } else if (s.equals( "/root/host-table")) {
        	HostTable hostTable = (HostTable)readHostTable(iId);
        	Map<Integer, Port> hostTableInMemory = new HashMap<Integer, Port>();
        	for (HostItem hostItem: hostTable.getHostItem()) {
        		Port port = new Port(hostItem.getTpId());
        		hostTableInMemory.put(hostItem.getHostId(), port);
        	}
            return hostTableInMemory;
        }
        else {
            return null;
        }
    }

    @Override
    public void writeData(String s, Object o) {
        if (s.equals("/root/trace-tree")) {
            TraceTree tracetree = (TraceTree) o;
            writeTraceTree(tracetree);
        } else if (s.equals("/root/host-table")) {
        	Host2PortEntry host2PortEntry = (Host2PortEntry) o;
        	writeHostTable(host2PortEntry);
        }
    }

   private void writeHostTable(Host2PortEntry h) {
        //HostTable hostTable = new HostTableBuilder().setHostItem(hostItemList).build();

	    HostItem hostItem = new HostItemBuilder().setHostId(h.hostIP).setTpId(h.port).build();
	    
        InstanceIdentifier<HostItem> hostItemIId = InstanceIdentifier.builder(HostTable.class)
        		.child(HostItem.class, new HostItemKey(h.hostIP)).build();
        WriteTransaction wx = dataBroker.newWriteOnlyTransaction();
        wx.put(LogicalDatastoreType.CONFIGURATION, hostItemIId, hostItem);
        try {
            wx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            LOG.error("Transaction failed: {}", e.toString());
        }

    }

    @Override
    public void writeTraceTree(TraceTree traceTree) {
    	ttw.writeToDataStore(traceTree);
    }




    private Object readTopology(InstanceIdentifier<Topology> topoIId) {
        
        Optional<?> dataFuture = null;
		try {
			ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
			dataFuture = rx.read(LogicalDatastoreType.OPERATIONAL, topoIId).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        return dataFuture.get();
    }

    private Object readHostTable(InstanceIdentifier<HostTable> hostTableIId) {

        Optional<?> dataFuture = null;
        try {
            ReadOnlyTransaction rx = dataBroker.newReadOnlyTransaction();
            dataFuture = rx.read(LogicalDatastoreType.CONFIGURATION, hostTableIId).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return dataFuture.get();
    }

    private InstanceIdentifier convertXpath2IId(String xpath) {
        if (xpath.equals("/root/network-topology/topology")) {
            return InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId("flow:1"))).build();
        } else if (xpath.equals("/root/host-table")) {
            return InstanceIdentifier.builder(HostTable.class).build();
        } else {
            return null;
        }
    }
}
