/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mapleapp.impl;

import java.util.Map;

import org.opendaylight.maple.core.increment.app.MapleAppBase;
import org.opendaylight.maple.core.increment.app.MapleUtil;
import org.opendaylight.maple.core.increment.packet.Ethernet;
import org.opendaylight.maple.core.increment.packet.IPv4;
import org.opendaylight.maple.core.increment.tracetree.MaplePacket;
import org.opendaylight.maple.core.increment.tracetree.Port;
import org.opendaylight.maple.core.increment.tracetree.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.slf4j.LoggerFactory;

public class ${appName} extends MapleAppBase {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(${appName}.class);

    private static final String TOPO_URL = "/root/network-topology/topology";
    private static final String HOST_TABLE_URL = "/root/host-table";

    private static final int H1_IP = IPv4.toIPv4Address("10.0.0.1");
    private static final int H3_IP = IPv4.toIPv4Address("10.0.0.3");

    @Override
    public void onPacket(MaplePacket pkt) {
    	LOG.info("onPacket");

        int ethType = pkt.ethType();
        // For IPv4 traffic only
        if (ethType == Ethernet.TYPE_IPv4) {
            int srcIP = pkt.IPv4Src();
            int dstIP = pkt.IPv4Dst();

            if(srcIP==H1_IP&&dstIP==H3_IP||srcIP==H3_IP&&dstIP==H1_IP) {
                Topology topo = (Topology) readData(TOPO_URL);
                Map<Integer, Port> hostTable = (Map<Integer, Port>) readData(HOST_TABLE_URL);
                Port srcPort = hostTable.get(srcIP);
                Port dstPort = hostTable.get(dstIP);
                pkt.setRoute(MapleUtil.shortestPath(topo.getLink(), srcPort, dstPort));
            }else{
                pkt.setRoute(Route.DROP);
            }

        } else {
            passToNext(pkt);
        }
    }
}
