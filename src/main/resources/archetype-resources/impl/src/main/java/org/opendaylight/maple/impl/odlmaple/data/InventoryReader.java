/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.odlmaple.data;

import org.maple.core.increment.MapleDataStoreAdaptor;
import org.maple.core.increment.tracetree.TraceTree;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class InventoryReader implements MapleDataStoreAdaptor {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryReader.class);
    private DataBroker fastDataStore;
    private DataBroker db;

    public InventoryReader(DataBroker fastDataStore) {
        this.fastDataStore = fastDataStore;
    }

    public InventoryReader() {}

    private Topology readTopology(InstanceIdentifier<Topology> topoIId) {
        Topology topology = null;
        try {
            ReadOnlyTransaction rx = fastDataStore.newReadOnlyTransaction();
            topology = rx.read(LogicalDatastoreType.OPERATIONAL, topoIId).get().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return topology;
    }

    private InstanceIdentifier convertXpath2IId(String xpath) {
        if (xpath.equals("/root/network-topology/topology")) {
            return InstanceIdentifier.builder(NetworkTopology.class)
                    .child(Topology.class, new TopologyKey(new TopologyId("odlmaple.flow:1"))).build();
        } else {
            return null;
        }
    }

    @Override
    public Object readData(String xpath) {
        InstanceIdentifier iId = convertXpath2IId(xpath);
        if (xpath.equals( "/root/network-topology/topology")) {
            return readTopology(iId);
        } else {
            return null;
        }
    }

    @Override
    public void writeData(String s, Object o) {

    }

    @Override
    public void writeTraceTree(TraceTree traceTree) {

    }

}
