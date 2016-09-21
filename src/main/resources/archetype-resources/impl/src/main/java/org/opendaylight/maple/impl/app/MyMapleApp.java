/*
 * Copyright (c) 2016 SNLAB and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.maple.impl.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.maple.core.increment.app.*;
import org.maple.core.increment.packet.Ethernet;
import org.maple.core.increment.packet.IPv4;
import org.maple.core.increment.tracetree.Action;
import org.maple.core.increment.tracetree.MaplePacket;
import org.maple.core.increment.tracetree.Path;
import org.maple.core.increment.tracetree.Port;
import org.maple.core.increment.tracetree.RouteAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

public class MyMapleApp extends MapleAppBase {


    org.maple.core.increment.app.ShortestPath sp;

    public MyMapleApp() {
        sp = new org.maple.core.increment.app.ShortestPath();
    }

    @Override
    public void onPacket(MaplePacket pkt) {
        if (pkt.ethTypeIs(Ethernet.TYPE_IPv4)) {
            int dstIP = pkt.IPv4Dst();

            if (dstIP == IPv4.toIPv4Address("10.0.0.2") || dstIP == IPv4.toIPv4Address("10.0.0.3")) {
                int srcIP = pkt.IPv4Src();

                int selectedServerId = srcIP%2;
                if (selectedServerId == 1) { // server is 10.0.0.2
                    pkt.setIPv4Dst(IPv4.toIPv4Address("10.0.0.2"));
                    pkt.setEthDst(Ethernet.toLong(Ethernet.toMACAddress("00:00:00:00:00:02")));

                    int realDstIP = IPv4.toIPv4Address("10.0.0.2");

                    Port srcPort = this.getMapleCore().getHost2swTable().get(srcIP);
                    Port dstPort = this.getMapleCore().getHost2swTable().get(realDstIP);

                    Topology topo = (Topology)readData("/root/network-topology/topology");

                    sp.setLinks(topo.getLink());

                    pkt.setRouteAction(new Path(sp.getFormattedPath(srcPort, dstPort), dstPort.getId()));
                }else { // server is 10.0.0.3
                    pkt.setIPv4Dst(IPv4.toIPv4Address("10.0.0.3"));
                    pkt.setEthDst(Ethernet.toLong(Ethernet.toMACAddress("00:00:00:00:00:03")));

                    int realDstIP = IPv4.toIPv4Address("10.0.0.3");

                    Port srcPort = this.getMapleCore().getHost2swTable().get(srcIP);
                    Port dstPort = this.getMapleCore().getHost2swTable().get(realDstIP);

                    Topology topo = (Topology)readData("/root/network-topology/topology");

                    sp.setLinks(topo.getLink());

                    pkt.setRouteAction(new Path(sp.getFormattedPath(srcPort, dstPort), dstPort.getId()));
                }
            } else if ( pkt.IPv4SrcIs(IPv4.toIPv4Address("10.0.0.2"))
                    || pkt.IPv4SrcIs(IPv4.toIPv4Address("10.0.0.3"))) {
                int srcIP = pkt.IPv4Src();

                Port srcPort = this.getMapleCore().getHost2swTable().get(srcIP);
                Port dstPort = this.getMapleCore().getHost2swTable().get(dstIP);

                Topology topo = (Topology)readData("/root/network-topology/topology");

                sp.setLinks(topo.getLink());

                pkt.setRouteAction(new Path(sp.getFormattedPath(srcPort, dstPort), dstPort.getId()));
            } else {
                pkt.setRouteAction(RouteAction.Drop());
            }
        } else {
            this.passToNext(pkt);
        }

    }

}
